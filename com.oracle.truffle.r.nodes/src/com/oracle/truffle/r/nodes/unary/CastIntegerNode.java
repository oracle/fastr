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
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.ops.na.*;

public abstract class CastIntegerNode extends CastNode {

    private final NACheck naCheck = NACheck.create();
    private final NAProfile naProfile = NAProfile.create();
    private final BranchProfile warningBranch = BranchProfile.create();

    public abstract Object executeInt(VirtualFrame frame, int o);

    public abstract Object executeInt(VirtualFrame frame, double o);

    public abstract Object executeInt(VirtualFrame frame, byte o);

    public abstract Object executeInt(VirtualFrame frame, Object o);

    @Child private CastIntegerNode recursiveCastInteger;

    private Object castIntegerRecursive(VirtualFrame frame, Object o) {
        if (recursiveCastInteger == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            recursiveCastInteger = insert(CastIntegerNodeGen.create(null, isPreserveNames(), isDimensionsPreservation(), isAttrPreservation()));
        }
        return recursiveCastInteger.executeInt(frame, o);
    }

    @Specialization
    protected RNull doNull(@SuppressWarnings("unused") RNull operand) {
        return RNull.instance;
    }

    @Specialization
    protected RMissing doMissing(RMissing operand) {
        return operand;
    }

    @Specialization
    protected int doInt(int operand) {
        return operand;
    }

    @Specialization
    protected int doDouble(double operand) {
        naCheck.enable(operand);
        return naCheck.convertDoubleToInt(operand);
    }

    @Specialization
    protected RIntVector doIntVector(RIntVector operand) {
        return operand;
    }

    @Specialization
    protected RIntSequence doIntSequence(RIntSequence operand) {
        return operand;
    }

    @Specialization
    protected RIntSequence doDoubleSequence(RDoubleSequence operand) {
        naCheck.enable(operand);
        return RDataFactory.createIntSequence(naCheck.convertDoubleToInt(operand.getStart()), naCheck.convertDoubleToInt(operand.getStride()), operand.getLength());
    }

    @Specialization
    protected int doComplex(RComplex operand) {
        naCheck.enable(operand);
        int result = naCheck.convertComplexToInt(operand);
        if (operand.getImaginaryPart() != 0.0) {
            warningBranch.enter();
            RError.warning(RError.Message.IMAGINARY_PARTS_DISCARDED_IN_COERCION);
        }
        return result;
    }

    @Specialization
    protected int doCharacter(String operand) {
        naCheck.enable(operand);
        int result = naCheck.convertStringToInt(operand);
        if (isNA(result)) {
            warningBranch.enter();
            RError.warning(RError.Message.NA_INTRODUCED_COERCION);
        }
        return result;
    }

    @Specialization
    protected int doBoolean(byte operand) {
        naCheck.enable(operand);
        return naCheck.convertLogicalToInt(operand);
    }

    @Specialization
    protected int doRaw(RRaw operand) {
        return RRuntime.raw2int(operand);
    }

    private RIntVector createResultVector(RAbstractVector operand, int[] idata) {
        RIntVector ret = RDataFactory.createIntVector(idata, naCheck.neverSeenNA(), isPreserveDimensions() ? operand.getDimensions() : null, isPreserveNames() ? operand.getNames() : null);
        preserveDimensionNames(operand, ret);
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(operand);
        }
        return ret;
    }

    @FunctionalInterface
    private interface IntToIntFunction {
        int apply(int value);
    }

    private RIntVector createResultVector(RAbstractVector operand, IntToIntFunction elementFunction) {
        naCheck.enable(operand);
        int[] idata = new int[operand.getLength()];
        boolean seenNA = false;
        for (int i = 0; i < operand.getLength(); i++) {
            int value = elementFunction.apply(i);
            idata[i] = value;
            seenNA = seenNA || naProfile.isNA(value);
        }
        RIntVector ret = RDataFactory.createIntVector(idata, !seenNA, isPreserveDimensions() ? operand.getDimensions() : null, isPreserveNames() ? operand.getNames() : null);
        preserveDimensionNames(operand, ret);
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(operand);
        }
        return ret;
    }

    @Specialization
    protected RIntVector doComplexVector(RComplexVector operand) {
        naCheck.enable(operand);
        int length = operand.getLength();
        int[] idata = new int[length];
        boolean warning = false;
        for (int i = 0; i < length; i++) {
            RComplex data = operand.getDataAt(i);
            idata[i] = naCheck.convertComplexToInt(data, false);
            if (data.getImaginaryPart() != 0.0) {
                warning = true;
            }
        }
        if (warning) {
            warningBranch.enter();
            RError.warning(RError.Message.IMAGINARY_PARTS_DISCARDED_IN_COERCION);
        }
        return createResultVector(operand, idata);
    }

    @Specialization
    protected RIntVector doStringVector(RStringVector operand) {
        naCheck.enable(operand);
        int[] idata = new int[operand.getLength()];
        boolean seenNA = false;
        for (int i = 0; i < operand.getLength(); i++) {
            String value = operand.getDataAt(i);
            idata[i] = naCheck.convertStringToInt(value);
            if (RRuntime.isNA(idata[i])) {
                seenNA = true;
            }
        }
        if (seenNA) {
            warningBranch.enter();
            RError.warning(RError.Message.NA_INTRODUCED_COERCION);
        }
        RIntVector ret = RDataFactory.createIntVector(idata, !seenNA, isPreserveDimensions() ? operand.getDimensions() : null, isPreserveNames() ? operand.getNames() : null);
        preserveDimensionNames(operand, ret);
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(operand);
        }
        return ret;
    }

    @Specialization
    public RIntVector doLogicalVector(RLogicalVector operand) {
        return createResultVector(operand, index -> naCheck.convertLogicalToInt(operand.getDataAt(index)));
    }

    @Specialization
    protected RIntVector doDoubleVector(RDoubleVector operand) {
        naCheck.enable(operand);
        return createResultVector(operand, naCheck.convertDoubleVectorToIntData(operand));
    }

    @Specialization
    protected RIntVector doRawVector(RRawVector operand) {
        return createResultVector(operand, index -> RRuntime.raw2int(operand.getDataAt(index)));
    }

    @Specialization
    protected RIntVector doList(VirtualFrame frame, RList list) {
        int length = list.getLength();
        int[] result = new int[length];
        boolean seenNA = false;
        for (int i = 0; i < length; i++) {
            Object entry = list.getDataAt(i);
            if (entry instanceof RList) {
                result[i] = RRuntime.INT_NA;
                seenNA = true;
            } else {
                Object castEntry = castIntegerRecursive(frame, entry);
                if (castEntry instanceof Integer) {
                    int value = (Integer) castEntry;
                    result[i] = value;
                    seenNA = seenNA || RRuntime.isNA(value);
                } else if (castEntry instanceof RIntVector) {
                    RIntVector intVector = (RIntVector) castEntry;
                    if (intVector.getLength() == 1) {
                        int value = intVector.getDataAt(0);
                        result[i] = value;
                        seenNA = seenNA || RRuntime.isNA(value);
                    } else if (intVector.getLength() == 0) {
                        result[i] = RRuntime.INT_NA;
                        seenNA = true;
                    } else {
                        throw throwCannotCoerceListError("integer");
                    }
                } else {
                    throw throwCannotCoerceListError("integer");
                }
            }
        }
        RIntVector ret = RDataFactory.createIntVector(result, !seenNA);
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(list);
        }
        return ret;
    }

    @Specialization
    protected RIntVector doFactor(RFactor factor) {
        return factor.getVector();
    }

    @Fallback
    @TruffleBoundary
    public int doOther(Object operand) {
        throw new ConversionFailedException(operand.getClass().getName());
    }
}
