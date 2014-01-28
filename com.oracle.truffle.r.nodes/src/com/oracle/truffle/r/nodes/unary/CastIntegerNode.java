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

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.unary.ConvertNode.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.ops.na.*;

@NodeFields({@NodeField(name = "namesPreservation", type = boolean.class), @NodeField(name = "dimensionsPreservation", type = boolean.class)})
public abstract class CastIntegerNode extends CastNode {

    private final NACheck check = NACheck.create();

    public abstract Object executeInt(VirtualFrame frame, Object o);

    public abstract Object executeIntVector(VirtualFrame frame, Object o);

    protected abstract boolean isNamesPreservation();

    protected abstract boolean isDimensionsPreservation();

    protected boolean preserveNames() {
        return isNamesPreservation();
    }

    protected boolean preserveDimensions() {
        return isDimensionsPreservation();
    }

    @Child CastIntegerNode recursiveCastInteger;

    private Object castIntegerRecursive(VirtualFrame frame, Object o) {
        if (recursiveCastInteger == null) {
            CompilerDirectives.transferToInterpreter();
            recursiveCastInteger = adoptChild(CastIntegerNodeFactory.create(null, isNamesPreservation(), isDimensionsPreservation()));
        }
        return recursiveCastInteger.executeInt(frame, o);
    }

    @Specialization
    public RNull doNull(@SuppressWarnings("unused") RNull operand) {
        return RNull.instance;
    }

    @Specialization
    public RMissing doMissing(RMissing operand) {
        return operand;
    }

    @Specialization
    public int doInt(int operand) {
        return operand;
    }

    @Specialization
    public int doDouble(double operand) {
        check.enable(operand);
        return check.convertDoubleToInt(operand);
    }

    @Specialization
    public RIntVector doIntVector(RIntVector operand) {
        return operand;
    }

    @Specialization
    public RIntSequence doIntSequence(RIntSequence operand) {
        return operand;
    }

    @Specialization
    public int doComplex(RComplex operand) {
        check.enable(operand);
        return check.convertComplexToInt(operand);
    }

    @Specialization
    public int doCharacter(String operand) {
        check.enable(operand);
        return check.convertStringToInt(operand);
    }

    @Specialization
    public int doBoolean(byte operand) {
        check.enable(operand);
        return check.convertLogicalToInt(operand);
    }

    @Specialization(order = 101, guards = "preserveDimensions")
    public RIntVector doComplexVectorDims(RComplexVector vector) {
        check.enable(vector);
        int length = vector.getLength();
        int[] result = new int[length];
        for (int i = 0; i < length; i++) {
            result[i] = check.convertComplexToInt(vector.getDataAt(i), false);
        }
        RContext.getInstance().setEvalWarning(RError.IMAGINARY_PARTS_DISCARDED_IN_COERCION);
        return RDataFactory.createIntVector(result, check.neverSeenNA(), vector.getDimensions());
    }

    @Specialization(order = 102, guards = "preserveNames")
    public RIntVector doComplexVectorNames(RComplexVector vector) {
        check.enable(vector);
        int length = vector.getLength();
        int[] result = new int[length];
        for (int i = 0; i < length; i++) {
            result[i] = check.convertComplexToInt(vector.getDataAt(i), false);
        }
        RContext.getInstance().setEvalWarning(RError.IMAGINARY_PARTS_DISCARDED_IN_COERCION);
        return RDataFactory.createIntVector(result, check.neverSeenNA(), vector.getNames());
    }

    @Specialization(order = 103)
    public RIntVector doComplexVector(RComplexVector vector) {
        check.enable(vector);
        int length = vector.getLength();
        int[] result = new int[length];
        for (int i = 0; i < length; i++) {
            result[i] = check.convertComplexToInt(vector.getDataAt(i), false);
        }
        RContext.getInstance().setEvalWarning(RError.IMAGINARY_PARTS_DISCARDED_IN_COERCION);
        return RDataFactory.createIntVector(result, check.neverSeenNA());
    }

    @Specialization(order = 104, guards = "preserveDimensions")
    public RIntVector doStringVectorDims(RStringVector vector) {
        check.enable(vector);
        int length = vector.getLength();
        int[] result = new int[length];
        for (int i = 0; i < length; i++) {
            result[i] = check.convertStringToInt(vector.getDataAt(i));
        }
        return RDataFactory.createIntVector(result, check.neverSeenNA(), vector.getDimensions());
    }

    @Specialization(order = 105, guards = "preserveNames")
    public RIntVector doStringVectorNames(RStringVector vector) {
        check.enable(vector);
        int length = vector.getLength();
        int[] result = new int[length];
        for (int i = 0; i < length; i++) {
            result[i] = check.convertStringToInt(vector.getDataAt(i));
        }
        return RDataFactory.createIntVector(result, check.neverSeenNA(), vector.getNames());
    }

    @Specialization(order = 106)
    public RIntVector doStringVector(RStringVector vector) {
        check.enable(vector);
        int length = vector.getLength();
        int[] result = new int[length];
        for (int i = 0; i < length; i++) {
            result[i] = check.convertStringToInt(vector.getDataAt(i));
        }
        return RDataFactory.createIntVector(result, check.neverSeenNA());
    }

    @Specialization(order = 107, guards = "preserveDimensions")
    public RIntVector doLogicalVectorDims(RLogicalVector vector) {
        check.enable(vector);
        int length = vector.getLength();
        int[] result = new int[length];
        for (int i = 0; i < length; i++) {
            result[i] = check.convertLogicalToInt(vector.getDataAt(i));
        }
        return RDataFactory.createIntVector(result, check.neverSeenNA(), vector.getDimensions());
    }

    @Specialization(order = 108, guards = "preserveNames")
    public RIntVector doLogicalVectorNames(RLogicalVector vector) {
        check.enable(vector);
        int length = vector.getLength();
        int[] result = new int[length];
        for (int i = 0; i < length; i++) {
            result[i] = check.convertLogicalToInt(vector.getDataAt(i));
        }
        return RDataFactory.createIntVector(result, check.neverSeenNA(), vector.getNames());
    }

    @Specialization(order = 109)
    public RIntVector doLogicalVector(RLogicalVector vector) {
        check.enable(vector);
        int length = vector.getLength();
        int[] result = new int[length];
        for (int i = 0; i < length; i++) {
            result[i] = check.convertLogicalToInt(vector.getDataAt(i));
        }
        return RDataFactory.createIntVector(result, check.neverSeenNA());
    }

    @Specialization(order = 110, guards = "preserveDimensions")
    public RIntVector doDoubleVectorDims(RDoubleVector vector) {
        check.enable(vector);
        int[] result = check.convertDoubleVectorToIntData(vector);
        return RDataFactory.createIntVector(result, check.neverSeenNA(), vector.getDimensions());
    }

    @Specialization(order = 111, guards = "preserveNames")
    public RIntVector doDoubleVectorNames(RDoubleVector vector) {
        check.enable(vector);
        int[] result = check.convertDoubleVectorToIntData(vector);
        return RDataFactory.createIntVector(result, check.neverSeenNA(), vector.getNames());
    }

    @Specialization(order = 112)
    public RIntVector doDoubleVector(RDoubleVector vector) {
        check.enable(vector);
        int[] result = check.convertDoubleVectorToIntData(vector);
        return RDataFactory.createIntVector(result, check.neverSeenNA());
    }

    @Specialization(order = 113, guards = "preserveDimensions")
    public RIntVector doRawVectorDims(RRawVector vector) {
        int length = vector.getLength();
        int[] result = new int[length];
        for (int i = 0; i < length; i++) {
            result[i] = vector.getDataAt(i).getValue();
        }
        return RDataFactory.createIntVector(result, check.neverSeenNA(), vector.getDimensions());
    }

    @Specialization(order = 114, guards = "preserveNames")
    public RIntVector doRawVectorNames(RRawVector vector) {
        int length = vector.getLength();
        int[] result = new int[length];
        for (int i = 0; i < length; i++) {
            result[i] = vector.getDataAt(i).getValue();
        }
        return RDataFactory.createIntVector(result, check.neverSeenNA(), vector.getNames());
    }

    @Specialization(order = 115)
    public RIntVector doRawVector(RRawVector vector) {
        int length = vector.getLength();
        int[] result = new int[length];
        for (int i = 0; i < length; i++) {
            result[i] = vector.getDataAt(i).getValue();
        }
        return RDataFactory.createIntVector(result, check.neverSeenNA());
    }

    @Specialization
    public RIntVector doList(VirtualFrame frame, RList list) {
        int length = list.getLength();
        int[] result = new int[length];
        for (int i = 0; i < length; i++) {
            Object entry = list.getDataAt(i);
            if (entry instanceof RList) {
                result[i] = RRuntime.INT_NA;
            } else {
                Object castEntry = castIntegerRecursive(frame, entry);
                if (castEntry instanceof Integer) {
                    result[i] = (Integer) castEntry;
                } else if (castEntry instanceof RIntVector) {
                    RIntVector intVector = (RIntVector) castEntry;
                    if (intVector.getLength() == 1) {
                        result[i] = intVector.getDataAt(0);
                    } else if (intVector.getLength() == 0) {
                        result[i] = RRuntime.INT_NA;
                    } else {
                        return cannotCoerceListError();
                    }
                } else {
                    return cannotCoerceListError();
                }
            }
        }
        return RDataFactory.createIntVector(result, check.neverSeenNA());
    }

    private RIntVector cannotCoerceListError() {
        throw RError.getListCoercion(this.getSourceSection(), "integer");
    }

    @Generic
    public int doOther(Object operand) {
        CompilerDirectives.transferToInterpreter();
        throw new ConversionFailedException(operand.getClass().getName());
    }

}
