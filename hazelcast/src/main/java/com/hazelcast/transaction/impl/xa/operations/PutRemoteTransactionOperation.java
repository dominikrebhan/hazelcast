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

package com.hazelcast.transaction.impl.xa.operations;

import com.hazelcast.internal.util.UUIDSerializationUtil;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.spi.impl.operationservice.BackupAwareOperation;
import com.hazelcast.spi.impl.NodeEngine;
import com.hazelcast.spi.impl.operationservice.Operation;
import com.hazelcast.transaction.impl.TransactionDataSerializerHook;
import com.hazelcast.transaction.impl.TransactionLogRecord;
import com.hazelcast.transaction.impl.xa.SerializableXID;
import com.hazelcast.transaction.impl.xa.XAService;
import com.hazelcast.transaction.impl.xa.XATransaction;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public class PutRemoteTransactionOperation extends AbstractXAOperation implements BackupAwareOperation {

    private final List<TransactionLogRecord> records = new LinkedList<>();

    private SerializableXID xid;
    private UUID txnId;
    private UUID txOwnerUuid;
    private long timeoutMillis;
    private long startTime;

    public PutRemoteTransactionOperation() {
    }

    public PutRemoteTransactionOperation(Collection<TransactionLogRecord> logs, UUID txnId, SerializableXID xid,
                                         UUID txOwnerUuid, long timeoutMillis, long startTime) {
        records.addAll(logs);
        this.txnId = txnId;
        this.xid = xid;
        this.txOwnerUuid = txOwnerUuid;
        this.timeoutMillis = timeoutMillis;
        this.startTime = startTime;
    }

    @Override
    public void run() throws Exception {
        XAService xaService = getService();
        NodeEngine nodeEngine = getNodeEngine();
        XATransaction transaction =
                new XATransaction(nodeEngine, records, txnId, xid, txOwnerUuid, timeoutMillis, startTime);
        xaService.putTransaction(transaction);
    }

    @Override
    public boolean shouldBackup() {
        return true;
    }

    @Override
    public int getSyncBackupCount() {
        return 0;
    }

    @Override
    public int getAsyncBackupCount() {
        return 1;
    }

    @Override
    public Operation getBackupOperation() {
        return new PutRemoteTransactionBackupOperation(records, txnId, xid, txOwnerUuid, timeoutMillis, startTime);
    }

    @Override
    protected void writeInternal(ObjectDataOutput out) throws IOException {
        UUIDSerializationUtil.writeUUID(out, txnId);
        out.writeObject(xid);
        UUIDSerializationUtil.writeUUID(out, txOwnerUuid);
        out.writeLong(timeoutMillis);
        out.writeLong(startTime);
        int len = records.size();
        out.writeInt(len);
        if (len > 0) {
            for (TransactionLogRecord record : records) {
                out.writeObject(record);
            }
        }
    }

    @Override
    protected void readInternal(ObjectDataInput in) throws IOException {
        txnId = UUIDSerializationUtil.readUUID(in);
        xid = in.readObject();
        txOwnerUuid = UUIDSerializationUtil.readUUID(in);
        timeoutMillis = in.readLong();
        startTime = in.readLong();
        int len = in.readInt();
        if (len > 0) {
            for (int i = 0; i < len; i++) {
                TransactionLogRecord record = in.readObject();
                records.add(record);
            }
        }
    }

    @Override
    public int getClassId() {
        return TransactionDataSerializerHook.PUT_REMOTE_TX;
    }
}
