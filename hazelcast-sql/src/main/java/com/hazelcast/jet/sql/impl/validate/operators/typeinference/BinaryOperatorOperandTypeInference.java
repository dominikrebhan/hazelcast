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

package com.hazelcast.jet.sql.impl.validate.operators.typeinference;

import com.hazelcast.jet.sql.impl.validate.HazelcastCallBinding;
import com.hazelcast.jet.sql.impl.validate.types.HazelcastTypeUtils;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.sql.SqlCallBinding;
import org.apache.calcite.sql.type.SqlOperandTypeInference;
import org.apache.calcite.sql.type.SqlTypeName;

import static com.hazelcast.jet.sql.impl.validate.types.HazelcastTypeUtils.createType;
import static com.hazelcast.jet.sql.impl.validate.types.HazelcastTypeUtils.isNullOrUnknown;
import static com.hazelcast.jet.sql.impl.validate.types.HazelcastTypeUtils.toHazelcastType;

public final class BinaryOperatorOperandTypeInference implements SqlOperandTypeInference {

    public static final BinaryOperatorOperandTypeInference INSTANCE = new BinaryOperatorOperandTypeInference();

    private BinaryOperatorOperandTypeInference() {
        // No-op.
    }

    @Override
    public void inferOperandTypes(SqlCallBinding binding, RelDataType returnType, RelDataType[] operandTypes) {
        assert operandTypes.length == 2;
        assert binding.getOperandCount() == 2;

        boolean hasParameters = HazelcastTypeUtils.hasParameters(binding);

        int knownTypeOperandIndex = -1;
        RelDataType knownType = null;

        for (int i = 0; i < binding.getOperandCount(); i++) {
            operandTypes[i] = binding.getOperandType(i);

            if (!operandTypes[i].equals(binding.getValidator().getUnknownType())) {
                if (hasParameters && toHazelcastType(operandTypes[i]).getTypeFamily().isNumericInteger()) {
                    // If we are here, the operands are a parameter and a numeric expression.
                    // We widen the type of the numeric expression to BIGINT, so that an expression `1 > ?` is resolved to
                    // `(BIGINT)1 > (BIGINT)?` rather than `(TINYINT)1 > (TINYINT)?`
                    RelDataType newOperandType = createType(
                            binding.getTypeFactory(),
                            SqlTypeName.BIGINT,
                            operandTypes[i].isNullable()
                    );

                    operandTypes[i] = newOperandType;
                }

                if (knownType == null || isNullOrUnknown(knownType.getSqlTypeName())) {
                    knownType = operandTypes[i];
                    knownTypeOperandIndex = i;
                }
            }
        }

        // If we have [UNKNOWN, UNKNOWN] or [NULL, UNKNOWN] operands, throw a signature error,
        // since we cannot deduce the return type
        if (knownType == null || isNullOrUnknown(knownType.getSqlTypeName()) && hasParameters) {
            throw new HazelcastCallBinding(binding).newValidationSignatureError();
        }

        if (SqlTypeName.INTERVAL_TYPES.contains(knownType.getSqlTypeName())
                && isNullOrUnknown(operandTypes[1 - knownTypeOperandIndex].getSqlTypeName())) {
            // If there is an interval on the one side and NULL on the other, assume that the other side is a TIMESTAMP,
            // because this is the only viable overload.
            operandTypes[1 - knownTypeOperandIndex] = createType(binding.getTypeFactory(), SqlTypeName.TIMESTAMP, true);
        } else {
            operandTypes[1 - knownTypeOperandIndex] = knownType;
        }
    }
}
