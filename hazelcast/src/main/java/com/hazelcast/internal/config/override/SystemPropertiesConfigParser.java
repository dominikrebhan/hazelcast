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
package com.hazelcast.internal.config.override;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.function.Consumer;

import com.hazelcast.internal.util.StringUtil;

import static java.util.stream.Collectors.toMap;

/**
 * A utility class converting raw input properties into valid config entries.
 */
class SystemPropertiesConfigParser {
    private final String prefix;
    private final String rootNode;

    SystemPropertiesConfigParser(String prefix, String rootNode) {
        this.prefix = prefix;
        this.rootNode = rootNode;
    }

    static SystemPropertiesConfigParser client() {
        return new SystemPropertiesConfigParser("hz-client.", "hazelcast-client");
    }

    static SystemPropertiesConfigParser member() {
        return new SystemPropertiesConfigParser("hz.", "hazelcast");
    }

    Map<String, String> parse(Properties properties) {
        return properties.entrySet()
                .stream()
                .mapMulti((Entry<Object, Object> e, Consumer<Entry<String, String>> consumer) -> {
                    if (e.getKey() instanceof String keyString && e.getValue() instanceof String valueString
                            && keyString.startsWith(prefix)) {
                        consumer.accept(Map.entry(keyString, valueString));
                    }
                })
                .collect(toMap(this::processKey, Entry::getValue));
    }

    private String processKey(Entry<String, ?> e) {
        // we convert keys to lowercase for processing later; we parse the keys to find the correct
        //  configuration objects to override, and all of our configuration keys are lowercase
        return StringUtil.lowerCaseInternal(e.getKey()
          .replace(" ", "")
          .replaceFirst(prefix, rootNode + "."));
    }
}
