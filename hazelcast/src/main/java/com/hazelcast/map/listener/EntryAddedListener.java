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

package com.hazelcast.map.listener;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.spi.annotation.NamespacesSupported;

/**
 * Invoked upon addition of an entry.
 *
 * @param <K> the type of key.
 * @param <V> the type of value.
 * @since 3.5
 */
@FunctionalInterface
@NamespacesSupported
public interface EntryAddedListener<K, V> extends MapListener {

    /**
     * Invoked upon addition of an entry.
     *
     * @param event the event invoked when an entry is added
     */
    void entryAdded(EntryEvent<K, V> event);
}
