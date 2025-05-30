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

package com.hazelcast.client.impl.protocol.codec;

import com.hazelcast.client.impl.protocol.ClientMessage;
import com.hazelcast.client.impl.protocol.Generated;
import com.hazelcast.client.impl.protocol.codec.builtin.*;
import com.hazelcast.client.impl.protocol.codec.custom.*;

import javax.annotation.Nullable;

import static com.hazelcast.client.impl.protocol.ClientMessage.*;
import static com.hazelcast.client.impl.protocol.codec.builtin.FixedSizeTypesCodec.*;

/*
 * This file is auto-generated by the Hazelcast Client Protocol Code Generator.
 * To change this file, edit the templates or the protocol
 * definitions on the https://github.com/hazelcast/hazelcast-client-protocol
 * and regenerate it.
 */

/**
 * The effect of this call is equivalent to set(k, v) on this VectorCollection once for each mapping from key k to value v.
 * The behavior of this operation is undefined if the specified collection is modified while the operation is in progress.
 * Note that all keys in the request should belong to the partition ID to which this request is being sent.
 * Any key that matches to a different partition ID shall be ignored.
 * The API implementation using this request may need to send multiple of these request messages for different partitions.
 */
@SuppressWarnings("unused")
@Generated("cf1365a26235fbd4418758a030548506")
public final class VectorCollectionPutAllCodec {
    //hex: 0x240300
    public static final int REQUEST_MESSAGE_TYPE = 2360064;
    //hex: 0x240301
    public static final int RESPONSE_MESSAGE_TYPE = 2360065;
    private static final int REQUEST_INITIAL_FRAME_SIZE = PARTITION_ID_FIELD_OFFSET + INT_SIZE_IN_BYTES;
    private static final int RESPONSE_INITIAL_FRAME_SIZE = RESPONSE_BACKUP_ACKS_FIELD_OFFSET + BYTE_SIZE_IN_BYTES;

    private VectorCollectionPutAllCodec() {
    }

    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings({"URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD"})
    public static class RequestParameters {

        /**
         * Name of the Vector Collection.
         */
        public java.lang.String name;

        /**
         * Key/VectorDocument entries to be stored in this VectorCollection.
         */
        public java.util.Collection<java.util.Map.Entry<com.hazelcast.internal.serialization.Data, com.hazelcast.vector.impl.DataVectorDocument>> entries;
    }

    public static ClientMessage encodeRequest(java.lang.String name, java.util.Collection<java.util.Map.Entry<com.hazelcast.internal.serialization.Data, com.hazelcast.vector.impl.DataVectorDocument>> entries) {
        ClientMessage clientMessage = ClientMessage.createForEncode();
        clientMessage.setContainsSerializedDataInRequest(true);
        clientMessage.setRetryable(false);
        clientMessage.setOperationName("VectorCollection.PutAll");
        ClientMessage.Frame initialFrame = new ClientMessage.Frame(new byte[REQUEST_INITIAL_FRAME_SIZE], UNFRAGMENTED_MESSAGE);
        encodeInt(initialFrame.content, TYPE_FIELD_OFFSET, REQUEST_MESSAGE_TYPE);
        encodeInt(initialFrame.content, PARTITION_ID_FIELD_OFFSET, -1);
        clientMessage.add(initialFrame);
        StringCodec.encode(clientMessage, name);
        EntryListCodec.encode(clientMessage, entries, DataCodec::encode, VectorDocumentCodec::encode);
        return clientMessage;
    }

    public static VectorCollectionPutAllCodec.RequestParameters decodeRequest(ClientMessage clientMessage) {
        ClientMessage.ForwardFrameIterator iterator = clientMessage.frameIterator();
        RequestParameters request = new RequestParameters();
        //empty initial frame
        iterator.next();
        request.name = StringCodec.decode(iterator);
        request.entries = EntryListCodec.decode(iterator, DataCodec::decode, VectorDocumentCodec::decode);
        return request;
    }

    public static ClientMessage encodeResponse() {
        ClientMessage clientMessage = ClientMessage.createForEncode();
        ClientMessage.Frame initialFrame = new ClientMessage.Frame(new byte[RESPONSE_INITIAL_FRAME_SIZE], UNFRAGMENTED_MESSAGE);
        encodeInt(initialFrame.content, TYPE_FIELD_OFFSET, RESPONSE_MESSAGE_TYPE);
        clientMessage.add(initialFrame);

        return clientMessage;
    }
}
