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

package com.hazelcast.internal.management.events;

import com.hazelcast.internal.json.JsonObject;
import com.hazelcast.internal.util.UuidUtil;

import java.util.UUID;

import static com.hazelcast.internal.management.events.EventMetadata.EventType.WAN_CONSISTENCY_CHECK_IGNORED;

public class WanConsistencyCheckIgnoredEvent extends AbstractWanAntiEntropyEventBase {
    private final String reason;

    public WanConsistencyCheckIgnoredEvent(String wanReplicationName, String wanPublisherId, String mapName, String reason) {
        this(UuidUtil.newUnsecureUUID(), wanReplicationName, wanPublisherId, mapName, reason);
    }

    public WanConsistencyCheckIgnoredEvent(UUID uuid, String wanReplicationName, String wanPublisherId, String mapName,
                                           String reason) {
        super(uuid, wanReplicationName, wanPublisherId, mapName);
        this.reason = reason;
    }

    public static WanConsistencyCheckIgnoredEvent enterpriseOnly(String wanReplicationName, String wanPublisherId,
                                                                 String mapName) {
        return new WanConsistencyCheckIgnoredEvent(UuidUtil.newUnsecureUUID(), wanReplicationName, wanPublisherId, mapName,
                "Consistency check is supported for enterprise clusters only.");
    }

    @Override
    public EventMetadata.EventType getType() {
        return WAN_CONSISTENCY_CHECK_IGNORED;
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = super.toJson();
        json.add("reason", reason);
        return json;
    }
}
