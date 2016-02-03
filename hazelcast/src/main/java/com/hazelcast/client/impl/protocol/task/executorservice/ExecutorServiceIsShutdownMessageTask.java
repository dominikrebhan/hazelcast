/*
 * Copyright (c) 2008-2016, Hazelcast, Inc. All Rights Reserved.
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

package com.hazelcast.client.impl.protocol.task.executorservice;

import com.hazelcast.client.impl.protocol.ClientMessage;
import com.hazelcast.client.impl.protocol.codec.ExecutorServiceIsShutdownCodec;
import com.hazelcast.client.impl.protocol.task.AbstractCallableMessageTask;
import com.hazelcast.executor.impl.DistributedExecutorService;
import com.hazelcast.instance.Node;
import com.hazelcast.nio.Connection;

import java.security.Permission;

public class ExecutorServiceIsShutdownMessageTask
        extends AbstractCallableMessageTask<ExecutorServiceIsShutdownCodec.RequestParameters> {

    public ExecutorServiceIsShutdownMessageTask(ClientMessage clientMessage, Node node, Connection connection) {
        super(clientMessage, node, connection);
    }

    @Override
    protected Object call() throws Exception {
        final DistributedExecutorService service = getService(DistributedExecutorService.SERVICE_NAME);
        return service.isShutdown(parameters.name);
    }

    @Override
    protected ExecutorServiceIsShutdownCodec.RequestParameters decodeClientMessage(ClientMessage clientMessage) {
        return ExecutorServiceIsShutdownCodec.decodeRequest(clientMessage);
    }

    @Override
    protected ClientMessage encodeResponse(Object response) {
        return ExecutorServiceIsShutdownCodec.encodeResponse((Boolean) response);
    }

    @Override
    public String getServiceName() {
        return DistributedExecutorService.SERVICE_NAME;
    }

    @Override
    public Permission getRequiredPermission() {
        return null;
    }

    @Override
    public String getDistributedObjectName() {
        return null;
    }

    @Override
    public String getMethodName() {
        return "isShutdown";
    }

    @Override
    public Object[] getParameters() {
        return null;
    }
}
