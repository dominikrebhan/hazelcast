/*
 * Copyright (c) 2008-2025, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.map.impl.operation.steps.engine;

import com.hazelcast.core.Offloadable;
import com.hazelcast.map.impl.operation.MapOperation;
import com.hazelcast.map.impl.operation.steps.UtilSteps;
import com.hazelcast.map.impl.recordstore.CustomStepAwareStorage;
import com.hazelcast.map.impl.recordstore.RecordStore;
import com.hazelcast.map.impl.recordstore.Storage;
import com.hazelcast.memory.NativeOutOfMemoryError;
import com.hazelcast.spi.exception.DistributedObjectDestroyedException;
import com.hazelcast.spi.impl.PartitionSpecificRunnable;
import com.hazelcast.spi.impl.operationservice.impl.OperationRunnerImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.hazelcast.internal.util.ThreadUtil.isRunningOnPartitionThread;
import static com.hazelcast.map.impl.operation.ForcedEviction.runStepWithForcedEvictionStrategies;
import static com.hazelcast.map.impl.operation.steps.engine.LinkerStep.linkSteps;

/**
 * <lu>
 * <li>This is a single operation's step supplier</li>
 * <li>Supplies steps and decides next step after executed one.</li>
 * <li>Must be thread safe</li>
 * </lu>
 */
public class StepSupplier implements Supplier<Runnable>, Consumer<Step> {

    private final State state;
    private final OperationRunnerImpl operationRunner;

    private volatile Runnable currentRunnable;
    private volatile Step currentStep;
    private volatile boolean firstStep = true;

    /**
     * Only here to disable check for testing purposes.
     */
    private final boolean checkCurrentThread;

    public StepSupplier(MapOperation operation) {
        this(operation, true);
    }

    // package-private for testing purposes
    StepSupplier(MapOperation operation,
                 boolean checkCurrentThread) {
        assert operation != null;

        this.state = operation.createState();
        this.currentStep = operation.getStartingStep();
        collectCustomSteps(operation, this);
        this.operationRunner = UtilSteps.getPartitionOperationRunner(state);
        this.checkCurrentThread = checkCurrentThread;

        assert this.currentStep != null;
    }

    @Override
    public void accept(Step headStep) {
        if (headStep == null) {
            return;
        }

        this.currentStep = linkSteps(headStep, currentStep);
    }

    public static void collectCustomSteps(MapOperation operation,
                                          Consumer<Step> consumer) {
        Storage storage = operation.getRecordStore().getStorage();
        if (storage instanceof CustomStepAwareStorage awareStorage) {
            awareStorage.collectCustomSteps(consumer);
        }
    }

    public static Step injectCustomStepsToOperation(MapOperation operation,
                                                    Step injectCustomStepsBeforeThisStep) {
        List<Step> steps = new ArrayList<>();

        collectCustomSteps(operation, customStep -> {
            if (customStep == null) {
                return;
            }

            steps.add(customStep);
        });

        Step injectionStep = injectCustomStepsBeforeThisStep;
        for (int i = 0; i < steps.size(); i++) {
            injectionStep = linkSteps(steps.get(i), injectionStep);
        }
        return injectionStep;
    }

    // used only for testing
    Step getCurrentStep() {
        return currentStep;
    }

    @Override
    public Runnable get() {
        if (currentRunnable == null && currentStep != null) {
            currentRunnable = createRunnable(currentStep, state);
        }
        return currentRunnable;
    }

    private Runnable createRunnable(Step step, State state) {
        // 0. If null step return null
        if (step == null) {
            return null;
        }

        // 1. If step needs to be offloaded,
        // return step wrapped as a runnable.
        if (step.isOffloadStep(state)) {
            return new ExecutorNameAwareRunnable() {
                @Override
                public String getExecutorName() {
                    return step.getExecutorName(state);
                }

                @Override
                public void run() {
                    assert !checkCurrentThread || !isRunningOnPartitionThread();

                    runStepWithState(step, state);
                }

                @Override
                public String toString() {
                    return step.toString();
                }
            };
        }

        // 2. If step needs to be run on partition thread,
        // return step wrapped as a partition specific runnable.
        return new PartitionSpecificRunnable() {
            @Override
            public void run() {
                assert !checkCurrentThread || isRunningOnPartitionThread();
                runStepWithState(step, state);
            }

            @Override
            public int getPartitionId() {
                return state.getPartitionId();
            }

            @Override
            public String toString() {
                return step.toString();
            }
        };
    }

    /**
     * Responsibilities of this method:
     * <lu>
     * <li>Runs passed step with passed state</li>
     * <li>Sets next step to run</li>
     * </lu>
     */
    private void runStepWithState(Step step, State state) {
        boolean runningOnPartitionThread = isRunningOnPartitionThread();
        boolean metWithPreconditions = true;
        try {
            refreshSate(state);

            int threadIndex = -1;
            // we check for error step here to handle potential
            // errors in `beforeOperation`/`afterOperation` calls.
            boolean errorStep = step == UtilSteps.HANDLE_ERROR;
            if (!errorStep) {
                threadIndex = state.getRecordStore().beforeOperation();
            }
            try {
                if (runningOnPartitionThread && state.getThrowable() == null) {
                    metWithPreconditions = metWithPreconditions();
                }

                if (metWithPreconditions) {
                    step.runStep(state);
                }
            } catch (NativeOutOfMemoryError e) {
                if (runningOnPartitionThread) {
                    rerunWithForcedEviction(() -> step.runStep(state));
                } else {
                    throw e;
                }
            } finally {
                if (!errorStep) {
                    state.getRecordStore().afterOperation(threadIndex);
                }
            }
        } catch (Throwable throwable) {
            if (runningOnPartitionThread) {
                state.getOperation().disposeDeferredBlocks();
            }
            state.setThrowable(throwable);
        } finally {
            if (metWithPreconditions) {
                currentStep = nextStep(step);
                currentRunnable = createRunnable(currentStep, state);
            } else {
                currentStep = null;
                currentRunnable = null;
            }
        }
    }

    /**
     * Refreshes this {@code StepSupplier} {@link State} by
     * resetting its record-store and operation objects.
     * <p>
     * Reasoning:
     * <p>
     * This is needed because while an offloaded operation is waiting
     * in queue, a previously queued operation can be a map#destroy
     * operation and it can remove all current IMap state. In this
     * case later operations' state in the queue become stale.
     * By refreshing the {@link State} we are fixing this issue.
     */
    private void refreshSate(State state) {
        MapOperation operation = state.getOperation();
        boolean mapExists = operation.checkMapExists();
        RecordStore recordStore = operation.getRecordStore();
        if (!mapExists || recordStore == null) {
            state.setThrowable(new DistributedObjectDestroyedException("No such map exists with name="
                    + operation.getName() + ", op=" + operation.getClass().getSimpleName()));
            return;
        }

        state.init(recordStore, operation);
    }

    private boolean metWithPreconditions() {
        assert isRunningOnPartitionThread();

        // check node and cluster health before running each step
        operationRunner.ensureNodeAndClusterHealth(state.getOperation());

        // check timeout for only first step,
        // as in regular operation-runner
        if (firstStep) {
            assert firstStep;
            firstStep = false;
            return !operationRunner.timeout(state.getOperation());
        }
        return true;
    }

    /**
     * In case of exception, sets next step as {@link UtilSteps#HANDLE_ERROR},
     * otherwise finds next step by calling {@link Step#nextStep}
     */
    private Step nextStep(Step step) {
        if (state.getThrowable() != null
                && currentStep != UtilSteps.HANDLE_ERROR) {
            return UtilSteps.HANDLE_ERROR;
        }
        return step.nextStep(state);
    }

    private void rerunWithForcedEviction(Runnable step) {
        runStepWithForcedEvictionStrategies(state.getOperation(), step);
    }

    public void handleOperationError(Throwable throwable) {
        state.setThrowable(throwable);
        currentRunnable = null;
        currentStep = UtilSteps.HANDLE_ERROR;
    }

    public MapOperation getOperation() {
        return state.getOperation();
    }

    private interface ExecutorNameAwareRunnable extends Runnable, Offloadable {

    }
}
