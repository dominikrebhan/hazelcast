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

package com.hazelcast.config;

import com.hazelcast.internal.config.ConfigDataSerializerHook;

import java.util.Map;

/**
 * Configuration for the Azure Discovery Strategy.
 */
public class AzureConfig
        extends AliasedDiscoveryConfig<AzureConfig> {
    public AzureConfig() {
        super("azure");
    }

    public AzureConfig(AzureConfig azureConfig) {
        super(azureConfig);
    }

    public AzureConfig(String tag, boolean enabled, boolean usePublicIp, Map<String, String> properties) {
        super(tag, enabled, usePublicIp, properties);
    }

    @Override
    public int getClassId() {
        return ConfigDataSerializerHook.AZURE_CONFIG;
    }
}
