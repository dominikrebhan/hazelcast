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

package com.hazelcast.ringbuffer.impl;

import com.hazelcast.internal.serialization.DataSerializerHook;
import com.hazelcast.internal.serialization.impl.FactoryIdHelper;
import com.hazelcast.nio.serialization.DataSerializableFactory;
import com.hazelcast.ringbuffer.impl.operations.AddAllBackupOperation;
import com.hazelcast.ringbuffer.impl.operations.AddAllOperation;
import com.hazelcast.ringbuffer.impl.operations.AddBackupOperation;
import com.hazelcast.ringbuffer.impl.operations.AddOperation;
import com.hazelcast.ringbuffer.impl.operations.GenericOperation;
import com.hazelcast.ringbuffer.impl.operations.MergeBackupOperation;
import com.hazelcast.ringbuffer.impl.operations.MergeOperation;
import com.hazelcast.ringbuffer.impl.operations.ReadManyOperation;
import com.hazelcast.ringbuffer.impl.operations.ReadOneOperation;
import com.hazelcast.ringbuffer.impl.operations.ReplicationOperation;

import static com.hazelcast.internal.serialization.impl.FactoryIdHelper.RINGBUFFER_DS_FACTORY;
import static com.hazelcast.internal.serialization.impl.FactoryIdHelper.RINGBUFFER_DS_FACTORY_ID;

/**
 * The {@link DataSerializerHook} for the Ringbuffer.
 */
public class RingbufferDataSerializerHook implements DataSerializerHook {

    public static final int F_ID = FactoryIdHelper.getFactoryId(RINGBUFFER_DS_FACTORY, RINGBUFFER_DS_FACTORY_ID);

    public static final int GENERIC_OPERATION = 1;
    public static final int ADD_BACKUP_OPERATION = 2;
    public static final int ADD_OPERATION = 3;
    public static final int READ_ONE_OPERATION = 4;
    public static final int REPLICATION_OPERATION = 5;
    public static final int READ_MANY_OPERATION = 6;
    public static final int ADD_ALL_OPERATION = 7;
    public static final int ADD_ALL_BACKUP_OPERATION = 8;
    public static final int READ_RESULT_SET = 9;
    public static final int RINGBUFFER_CONTAINER = 10;
    public static final int MERGE_OPERATION = 11;
    public static final int MERGE_BACKUP_OPERATION = 12;

    @Override
    public int getFactoryId() {
        return F_ID;
    }

    @Override
    @SuppressWarnings({"AnonInnerLength", "CyclomaticComplexity", "ReturnCount"})
    public DataSerializableFactory createFactory() {
        return typeId -> switch (typeId) {
            case ADD_BACKUP_OPERATION -> new AddBackupOperation();
            case ADD_OPERATION -> new AddOperation();
            case READ_ONE_OPERATION -> new ReadOneOperation();
            case REPLICATION_OPERATION -> new ReplicationOperation();
            case GENERIC_OPERATION -> new GenericOperation();
            case READ_MANY_OPERATION -> new ReadManyOperation<>();
            case ADD_ALL_OPERATION -> new AddAllOperation();
            case ADD_ALL_BACKUP_OPERATION -> new AddAllBackupOperation();
            case READ_RESULT_SET -> new ReadResultSetImpl<>();
            case RINGBUFFER_CONTAINER -> new RingbufferContainer<>();
            case MERGE_OPERATION -> new MergeOperation();
            case MERGE_BACKUP_OPERATION -> new MergeBackupOperation();
            default -> null;
        };
    }
}
