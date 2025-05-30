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

package com.hazelcast.map.impl.operation;

import com.hazelcast.internal.serialization.Data;
import com.hazelcast.map.impl.MapDataSerializerHook;
import com.hazelcast.map.impl.operation.steps.engine.Step;
import com.hazelcast.map.impl.operation.steps.RemoveOpSteps;
import com.hazelcast.map.impl.operation.steps.engine.State;

public class RemoveOperation extends BaseRemoveOperation {

    protected boolean successful;

    public RemoveOperation() {
    }

    public RemoveOperation(String name, Data dataKey) {
        super(name, dataKey);
    }

    @Override
    protected void runInternal() {
        dataOldValue = mapServiceContext.toData(recordStore.remove(dataKey,
                getCallerProvenance()));
        successful = dataOldValue != null;
    }

    @Override
    public void afterRunInternal() {
        if (successful) {
            super.afterRunInternal();
        }
    }

    @Override
    public void applyState(State state) {
        super.applyState(state);
        successful = dataOldValue != null;
    }

    @Override
    public Step getStartingStep() {
        return RemoveOpSteps.READ;
    }

    @Override
    public boolean shouldBackup() {
        return dataOldValue != null;
    }

    @Override
    public int getClassId() {
        return MapDataSerializerHook.REMOVE;
    }
}
