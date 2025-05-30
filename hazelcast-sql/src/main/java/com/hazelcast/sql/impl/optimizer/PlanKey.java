/*
 * Copyright 2025 Hazelcast Inc.
 *
 * Licensed under the Hazelcast Community License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://hazelcast.com/hazelcast-community-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.sql.impl.optimizer;

import java.util.List;

public class PlanKey {

    private final List<List<String>> searchPaths;
    private final String sql;

    public PlanKey(List<List<String>> searchPaths, String sql) {
        this.searchPaths = searchPaths;
        this.sql = sql;
    }

    public List<List<String>> getSearchPaths() {
        return searchPaths;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PlanKey that = (PlanKey) o;

        return sql.equals(that.sql) && searchPaths.equals(that.searchPaths);
    }

    @Override
    public int hashCode() {
        int result = searchPaths.hashCode();
        result = 31 * result + sql.hashCode();
        return result;
    }
}
