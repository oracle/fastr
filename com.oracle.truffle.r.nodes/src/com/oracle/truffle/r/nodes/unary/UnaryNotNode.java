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
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.ops.na.*;

public abstract class UnaryNotNode extends RBuiltinNode {

    private final NACheck na = NACheck.create();

    @Specialization(order = 0)
    public byte doLogical(byte operand) {
        na.enable(operand);
        if (na.check(operand)) {
            return RRuntime.LOGICAL_NA;
        }
        return not(operand);
    }

    @Specialization(order = 1)
    public byte doInt(int operand) {
        na.enable(operand);
        if (na.check(operand)) {
            return RRuntime.LOGICAL_NA;
        }
        return RRuntime.asLogical(operand == 0);
    }

    @Specialization(order = 2)
    public byte doDouble(double operand) {
        na.enable(operand);
        if (na.check(operand)) {
            return RRuntime.LOGICAL_NA;
        }
        return RRuntime.asLogical(operand == 0);
    }

    @Specialization(order = 3)
    public RRaw doRaw(RRaw operand) {
        return RDataFactory.createRaw(performRaw(operand));
    }

    @SuppressWarnings("unused")
    @Specialization(order = 10)
    public Object doNull(RNull operand) {
        throw RError.getInvalidArgType(getEncapsulatingSourceSection());
    }

    private static byte performRaw(RRaw operand) {
        return (byte) (255 - operand.getValue());
    }

    @Specialization
    public RLogicalVector performLogicalVectorNot(@SuppressWarnings("unused") RFunction operand) {
        throw RError.getInvalidArgType(getEncapsulatingSourceSection());
    }

    @Specialization(order = 20, guards = "isZeroLength")
    public RLogicalVector performLogicalVectorNot(@SuppressWarnings("unused") RAbstractVector vector) {
        return RDataFactory.createEmptyLogicalVector();
    }

    @Specialization(order = 30)
    public RLogicalVector performLogicalVectorNot(@SuppressWarnings("unused") RAbstractStringVector vector) {
        throw RError.getInvalidArgType(getEncapsulatingSourceSection());
    }

    @Specialization(order = 40)
    public RLogicalVector performLogicalVectorNot(@SuppressWarnings("unused") RList list) {
        throw RError.getInvalidArgType(getEncapsulatingSourceSection());
    }

    @Specialization(order = 50, guards = "!isZeroLength")
    public RLogicalVector doLogicalVector(RLogicalVector vector) {
        return performLogicalVectorNot(vector);
    }

    @Specialization(order = 51, guards = "!isZeroLength")
    public RLogicalVector doIntVector(RIntVector vector) {
        return performAbstractIntVectorNot(vector);
    }

    @Specialization(order = 52, guards = "!isZeroLength")
    public RLogicalVector doDoubleVector(RDoubleVector vector) {
        return performAbstractDoubleVectorNot(vector);
    }

    @Specialization(order = 53, guards = "!isZeroLength")
    public RLogicalVector doIntSequence(RIntSequence vector) {
        return performAbstractIntVectorNot(vector);
    }

    @Specialization(order = 54, guards = "!isZeroLength")
    public RLogicalVector doDoubleSequence(RDoubleSequence vector) {
        return performAbstractDoubleVectorNot(vector);
    }

    @Specialization(order = 55, guards = "!isZeroLength")
    public RRawVector doRawVector(RRawVector vector) {
        return performRawVectorNot(vector);
    }

    private RLogicalVector performLogicalVectorNot(RLogicalVector vector) {
        na.enable(vector);
        int length = vector.getLength();
        byte[] result = new byte[length];
        for (int i = 0; i < length; ++i) {
            byte value = vector.getDataAt(i);
            result[i] = doLogical(value);
        }
        RLogicalVector resultVector = RDataFactory.createLogicalVector(result, na.neverSeenNA());
        resultVector.copyNamesDimsDimNamesFrom(vector, getSourceSection());
        return resultVector;
    }

    private RLogicalVector performAbstractIntVectorNot(RAbstractIntVector vector) {
        na.enable(vector);
        int length = vector.getLength();
        byte[] result = new byte[length];
        for (int i = 0; i < length; ++i) {
            int value = vector.getDataAt(i);
            result[i] = doInt(value);
        }
        RLogicalVector resultVector = RDataFactory.createLogicalVector(result, na.neverSeenNA());
        resultVector.copyNamesDimsDimNamesFrom(vector, getSourceSection());
        return resultVector;
    }

    private RLogicalVector performAbstractDoubleVectorNot(RAbstractDoubleVector vector) {
        na.enable(vector);
        int length = vector.getLength();
        byte[] result = new byte[length];
        for (int i = 0; i < length; ++i) {
            double value = vector.getDataAt(i);
            result[i] = doDouble(value);
        }
        RLogicalVector resultVector = RDataFactory.createLogicalVector(result, na.neverSeenNA());
        resultVector.copyNamesFrom(vector);
        return resultVector;
    }

    private RRawVector performRawVectorNot(RRawVector vector) {
        na.enable(vector);
        int length = vector.getLength();
        byte[] result = new byte[length];
        for (int i = 0; i < length; ++i) {
            RRaw value = vector.getDataAt(i);
            result[i] = performRaw(value);
        }
        RRawVector resultVector = RDataFactory.createRawVector(result);
        resultVector.copyNamesDimsDimNamesFrom(vector, getSourceSection());
        return resultVector;
    }

    private static byte not(byte value) {
        return (value == RRuntime.LOGICAL_TRUE ? RRuntime.LOGICAL_FALSE : RRuntime.LOGICAL_TRUE);
    }

    protected boolean isZeroLength(RAbstractVector vector) {
        return vector.getLength() == 0;
    }
}
