/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.util.function.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.ops.na.*;

public abstract class CastDoubleNode extends CastNode {

    private final NACheck naCheck = NACheck.create();
    private final NAProfile naProfile = NAProfile.create();
    private final BranchProfile warningBranch = BranchProfile.create();

    public abstract Object executeDouble(VirtualFrame frame, int o);

    public abstract Object executeDouble(VirtualFrame frame, double o);

    public abstract Object executeDouble(VirtualFrame frame, byte o);

    public abstract Object executeDouble(VirtualFrame frame, Object o);

    @Child private CastDoubleNode recursiveCastDouble;

    private Object castDoubleRecursive(VirtualFrame frame, Object o) {
        if (recursiveCastDouble == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            recursiveCastDouble = insert(CastDoubleNodeGen.create(null, isPreserveNames(), isDimensionsPreservation(), isAttrPreservation()));
        }
        return recursiveCastDouble.executeDouble(frame, o);
    }

    @Specialization
    protected RNull doNull(@SuppressWarnings("unused") RNull operand) {
        return RNull.instance;
    }

    @Specialization
    protected double doInt(int operand) {
        naCheck.enable(operand);
        return naCheck.convertIntToDouble(operand);
    }

    @Specialization
    protected double doDouble(double operand) {
        return operand;
    }

    @Specialization
    protected double doDouble(RComplex operand) {
        naCheck.enable(operand);
        double result = naCheck.convertComplexToDouble(operand);
        if (operand.getImaginaryPart() != 0.0) {
            warningBranch.enter();
            RError.warning(RError.Message.IMAGINARY_PARTS_DISCARDED_IN_COERCION);
        }
        return result;
    }

    @Specialization
    protected double doLogical(byte operand) {
        naCheck.enable(operand);
        return naCheck.convertLogicalToDouble(operand);
    }

    @Specialization
    public double doString(String operand) {
        naCheck.enable(operand);
        double result = naCheck.convertStringToDouble(operand);
        if (isNA(result)) {
            warningBranch.enter();
            RError.warning(RError.Message.NA_INTRODUCED_COERCION);
        }
        return result;
    }

    @Specialization
    protected double doRaw(RRaw operand) {
        return RRuntime.raw2double(operand);
    }

    private RDoubleVector createResultVector(RAbstractVector operand, double[] ddata) {
        RDoubleVector ret = RDataFactory.createDoubleVector(ddata, naCheck.neverSeenNA(), getPreservedDimensions(operand), getPreservedNames(operand));
        preserveDimensionNames(operand, ret);
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(operand);
        }
        return ret;
    }

    private RDoubleVector createResultVector(RAbstractVector operand, IntToDoubleFunction elementFunction) {
        naCheck.enable(operand);
        double[] ddata = new double[operand.getLength()];
        boolean seenNA = false;
        for (int i = 0; i < operand.getLength(); i++) {
            double value = elementFunction.applyAsDouble(i);
            ddata[i] = value;
            seenNA = seenNA || naProfile.isNA(value);
        }
        RDoubleVector ret = RDataFactory.createDoubleVector(ddata, !seenNA, getPreservedDimensions(operand), getPreservedNames(operand));
        preserveDimensionNames(operand, ret);
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(operand);
        }
        return ret;
    }

    @Specialization
    protected RDoubleVector doIntVector(RAbstractIntVector operand) {
        return createResultVector(operand, index -> naCheck.convertIntToDouble(operand.getDataAt(index)));
    }

    @Specialization
    protected RDoubleVector doLogicalVectorDims(RLogicalVector operand) {
        return createResultVector(operand, index -> naCheck.convertLogicalToDouble(operand.getDataAt(index)));
    }

    @Specialization
    protected RDoubleVector doStringVector(RStringVector operand) {
        naCheck.enable(operand);
        double[] ddata = new double[operand.getLength()];
        boolean seenNA = false;
        for (int i = 0; i < operand.getLength(); i++) {
            String value = operand.getDataAt(i);
            ddata[i] = naCheck.convertStringToDouble(value);
            if (RRuntime.isNA(ddata[i])) {
                seenNA = true;
            }
        }
        if (seenNA) {
            warningBranch.enter();
            RError.warning(RError.Message.NA_INTRODUCED_COERCION);
        }
        RDoubleVector ret = RDataFactory.createDoubleVector(ddata, !seenNA, getPreservedDimensions(operand), getPreservedNames(operand));
        preserveDimensionNames(operand, ret);
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(operand);
        }
        return ret;
    }

    @Specialization
    protected RDoubleVector doComplexVector(RComplexVector operand) {
        naCheck.enable(operand);
        double[] ddata = new double[operand.getLength()];
        boolean warning = false;
        for (int i = 0; i < operand.getLength(); i++) {
            RComplex value = operand.getDataAt(i);
            ddata[i] = naCheck.convertComplexToDouble(value);
            if (value.getImaginaryPart() != 0.0) {
                warning = true;
            }
        }
        if (warning) {
            warningBranch.enter();
            RError.warning(RError.Message.IMAGINARY_PARTS_DISCARDED_IN_COERCION);
        }
        return createResultVector(operand, ddata);
    }

    @Specialization
    protected RDoubleVector doRawVector(RRawVector operand) {
        return createResultVector(operand, index -> RRuntime.raw2double(operand.getDataAt(index)));
    }

    @Specialization
    protected RDoubleVector doDoubleVector(RDoubleVector operand) {
        return operand;
    }

    @Specialization
    protected RDoubleSequence doDoubleSequence(RDoubleSequence operand) {
        return operand;
    }

    @Specialization
    protected RDoubleVector doList(VirtualFrame frame, RList list) {
        int length = list.getLength();
        double[] result = new double[length];
        boolean seenNA = false;
        for (int i = 0; i < length; i++) {
            Object entry = list.getDataAt(i);
            if (entry instanceof RList) {
                result[i] = RRuntime.DOUBLE_NA;
                seenNA = true;
            } else {
                Object castEntry = castDoubleRecursive(frame, entry);
                if (castEntry instanceof Double) {
                    double value = (Double) castEntry;
                    result[i] = value;
                    seenNA = seenNA || RRuntime.isNA(value);
                } else if (castEntry instanceof RDoubleVector) {
                    RDoubleVector doubleVector = (RDoubleVector) castEntry;
                    if (doubleVector.getLength() == 1) {
                        double value = doubleVector.getDataAt(0);
                        result[i] = value;
                        seenNA = seenNA || RRuntime.isNA(value);
                    } else if (doubleVector.getLength() == 0) {
                        result[i] = RRuntime.DOUBLE_NA;
                        seenNA = true;
                    } else {
                        throw throwCannotCoerceListError("numeric");
                    }
                } else {
                    throw throwCannotCoerceListError("numeric");
                }
            }
        }
        RDoubleVector ret = RDataFactory.createDoubleVector(result, !seenNA);
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(list);
        }
        return ret;
    }

    @Fallback
    @TruffleBoundary
    public double doOther(Object operand) {
        throw new ConversionFailedException(operand.getClass().getName());
    }

    public static CastDoubleNode create() {
        return CastDoubleNodeGen.create(null, true, true, true);
    }

}
