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

package com.hazelcast.security.permission;

import java.security.Permission;

/**
 *
 */
public class ConfigPermission extends ClusterPermission {

    private static final String CONFIG_PERMISSION_NAME = "<config>";
    private static final String CONFIG_PERMISSION_ACTIONS = "config";

    public ConfigPermission() {
        super(CONFIG_PERMISSION_NAME);
    }

    @Override
    public boolean implies(Permission permission) {
        return getClass() == permission.getClass();
    }

    @Override
    public String getActions() {
        return CONFIG_PERMISSION_ACTIONS;
    }
}
