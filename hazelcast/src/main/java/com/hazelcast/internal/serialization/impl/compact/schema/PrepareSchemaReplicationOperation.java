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

package com.hazelcast.internal.serialization.impl.compact.schema;

import com.hazelcast.internal.serialization.impl.compact.Schema;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;

import java.io.IOException;

/**
 * Sent from the coordinator member to participant nodes so that they
 * will mark the schema in their local as prepared, after putting it to their
 * in-memory registry and HotRestart data (if enabled).
 */
public class PrepareSchemaReplicationOperation extends AbstractSchemaReplicationOperation {

    private Schema schema;

    public PrepareSchemaReplicationOperation() {
    }

    public PrepareSchemaReplicationOperation(Schema schema, int memberListVersion) {
        super(memberListVersion);
        this.schema = schema;
    }

    @Override
    protected void runInternal() {
        MemberSchemaService service = getService();
        service.onSchemaPreparationRequest(schema);
    }

    @Override
    protected void writeInternal(ObjectDataOutput out) throws IOException {
        out.writeObject(schema);
        out.writeInt(memberListVersion);
    }

    @Override
    protected void readInternal(ObjectDataInput in) throws IOException {
        schema = in.readObject();
        memberListVersion = in.readInt();
    }

    @Override
    public int getClassId() {
        return SchemaDataSerializerHook.PREPARE_SCHEMA_REPLICATION_OPERATION;
    }
}
