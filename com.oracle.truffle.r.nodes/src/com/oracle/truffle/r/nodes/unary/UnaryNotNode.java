/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.nodes.unary;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.ops.na.*;

@RBuiltin(name = "!", kind = RBuiltinKind.PRIMITIVE, parameterNames = {""})
@GenerateNodeFactory
public abstract class UnaryNotNode extends RBuiltinNode {

    private final NACheck na = NACheck.create();
    private final NAProfile naProfile = NAProfile.create();
    private final ConditionProfile zeroLengthProfile = ConditionProfile.createBinaryProfile();

    private static byte not(byte value) {
        return (value == RRuntime.LOGICAL_TRUE ? RRuntime.LOGICAL_FALSE : RRuntime.LOGICAL_TRUE);
    }

    private static byte not(int operand) {
        return RRuntime.asLogical(operand == 0);
    }

    private static byte not(double operand) {
        return RRuntime.asLogical(operand == 0);
    }

    private static byte notRaw(RRaw operand) {
        return (byte) (255 - operand.getValue());
    }

    @Specialization
    protected byte doLogical(byte operand) {
        return naProfile.isNA(operand) ? RRuntime.LOGICAL_NA : not(operand);
    }

    @Specialization
    protected byte doInt(int operand) {
        return naProfile.isNA(operand) ? RRuntime.LOGICAL_NA : not(operand);
    }

    @Specialization
    protected byte doDouble(double operand) {
        return naProfile.isNA(operand) ? RRuntime.LOGICAL_NA : not(operand);
    }

    @Specialization
    protected RRaw doRaw(RRaw operand) {
        return RDataFactory.createRaw(notRaw(operand));
    }

    @Specialization
    protected RLogicalVector doLogicalVector(RLogicalVector vector) {
        int length = vector.getLength();
        byte[] result;
        if (zeroLengthProfile.profile(length == 0)) {
            result = RDataFactory.EMPTY_LOGICAL_ARRAY;
        } else {
            na.enable(vector);
            result = new byte[length];
            for (int i = 0; i < length; ++i) {
                byte value = vector.getDataAt(i);
                result[i] = na.check(value) ? RRuntime.LOGICAL_NA : not(value);
            }
        }
        RLogicalVector resultVector = RDataFactory.createLogicalVector(result, na.neverSeenNA());
        resultVector.copyAttributesFrom(vector);
        return resultVector;
    }

    @Specialization
    protected RLogicalVector doIntVector(RAbstractIntVector vector) {
        int length = vector.getLength();
        byte[] result;
        if (zeroLengthProfile.profile(length == 0)) {
            result = RDataFactory.EMPTY_LOGICAL_ARRAY;
        } else {
            na.enable(vector);
            result = new byte[length];
            for (int i = 0; i < length; ++i) {
                int value = vector.getDataAt(i);
                result[i] = na.check(value) ? RRuntime.LOGICAL_NA : not(value);
            }
        }
        RLogicalVector resultVector = RDataFactory.createLogicalVector(result, na.neverSeenNA());
        resultVector.copyNamesDimsDimNamesFrom(vector, getSourceSection());
        return resultVector;
    }

    @Specialization
    protected RLogicalVector doDoubleVector(RAbstractDoubleVector vector) {
        int length = vector.getLength();
        byte[] result;
        if (zeroLengthProfile.profile(length == 0)) {
            result = RDataFactory.EMPTY_LOGICAL_ARRAY;
        } else {
            na.enable(vector);
            result = new byte[length];
            for (int i = 0; i < length; ++i) {
                double value = vector.getDataAt(i);
                result[i] = na.check(value) ? RRuntime.LOGICAL_NA : not(value);
            }
        }
        RLogicalVector resultVector = RDataFactory.createLogicalVector(result, na.neverSeenNA());
        resultVector.copyNamesDimsDimNamesFrom(vector, getSourceSection());
        return resultVector;
    }

    @Specialization
    protected RRawVector doRawVector(RRawVector vector) {
        int length = vector.getLength();
        byte[] result;
        if (zeroLengthProfile.profile(length == 0)) {
            result = RDataFactory.EMPTY_RAW_ARRAY;
        } else {
            result = new byte[length];
            for (int i = 0; i < length; ++i) {
                result[i] = notRaw(vector.getDataAt(i));
            }
        }
        RRawVector resultVector = RDataFactory.createRawVector(result);
        resultVector.copyAttributesFrom(vector);
        return resultVector;
    }

    @Specialization(guards = {"isZeroLength"})
    protected RLogicalVector doStringVector(@SuppressWarnings("unused") RAbstractStringVector vector) {
        return RDataFactory.createEmptyLogicalVector();
    }

    @Specialization(guards = {"isZeroLength"})
    protected RLogicalVector doComplexVector(@SuppressWarnings("unused") RAbstractComplexVector vector) {
        return RDataFactory.createEmptyLogicalVector();
    }

    @Specialization(guards = {"isZeroLength"})
    protected RLogicalVector doList(@SuppressWarnings("unused") RList list) {
        return RDataFactory.createEmptyLogicalVector();
    }

    protected boolean isZeroLength(RAbstractVector vector) {
        return vector.getLength() == 0;
    }

    @Fallback
    protected Object invalidArgType(@SuppressWarnings("unused") Object operand) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_ARG_TYPE);
    }
}
