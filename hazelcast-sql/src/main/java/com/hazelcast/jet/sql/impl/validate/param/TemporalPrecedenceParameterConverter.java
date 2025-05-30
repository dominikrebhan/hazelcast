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

package com.hazelcast.jet.sql.impl.validate.param;

import com.hazelcast.sql.impl.type.QueryDataType;
import com.hazelcast.sql.impl.type.QueryDataTypeFamily;
import com.hazelcast.sql.impl.type.converter.Converter;
import org.apache.calcite.sql.parser.SqlParserPos;

public class TemporalPrecedenceParameterConverter extends AbstractParameterConverter {
    public TemporalPrecedenceParameterConverter(int ordinal, SqlParserPos parserPos, QueryDataType targetType) {
        super(ordinal, parserPos, targetType);
    }

    @Override
    protected boolean isValid(Object value, Converter valueConverter) {
        QueryDataTypeFamily valueTypeFamily = valueConverter.getTypeFamily();

        return valueTypeFamily.isTemporal()
                && valueConverter.canConvertTo(targetType.getTypeFamily())
                && valueTypeFamily.getPrecedence() <= targetType.getTypeFamily().getPrecedence();
    }
}
