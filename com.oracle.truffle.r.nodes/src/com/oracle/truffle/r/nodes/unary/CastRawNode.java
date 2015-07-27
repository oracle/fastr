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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

public abstract class CastRawNode extends CastBaseNode {

    private final BranchProfile warningBranch = BranchProfile.create();

    public abstract Object executeRaw(int o);

    public abstract Object executeRaw(double o);

    public abstract Object executeRaw(byte o);

    public abstract Object executeRaw(Object o);

    @Specialization
    protected RNull doNull(@SuppressWarnings("unused") RNull operand) {
        return RNull.instance;
    }

    private RRaw checkOutOfRange(int operand, int intResult) {
        if (intResult != operand) {
            warningBranch.enter();
            RError.warning(getEncapsulatingSourceSection(), RError.Message.OUT_OF_RANGE);
            return RDataFactory.createRaw((byte) 0);
        }
        return RDataFactory.createRaw((byte) intResult);
    }

    @Specialization
    protected RRaw doInt(int operand) {
        int intResult = RRuntime.int2rawIntValue(operand);
        return checkOutOfRange(operand, intResult);
    }

    @Specialization
    protected RRaw doDouble(double operand) {
        int intResult = RRuntime.double2rawIntValue(operand);
        return checkOutOfRange((int) operand, intResult);
    }

    @Specialization
    protected RRaw doComplex(RComplex operand) {
        int intResult = RRuntime.complex2rawIntValue(operand);
        if (operand.getImaginaryPart() != 0) {
            warningBranch.enter();
            RError.warning(getEncapsulatingSourceSection(), RError.Message.IMAGINARY_PARTS_DISCARDED_IN_COERCION);
        }
        return checkOutOfRange((int) operand.getRealPart(), intResult);
    }

    @Specialization
    protected RRaw doRaw(RRaw operand) {
        return operand;
    }

    @Specialization
    protected RRaw doLogical(byte operand) {
        // need to convert to int so that NA-related warning is caught
        int intVal = RRuntime.logical2int(operand);
        return doInt(intVal);
    }

    @Specialization
    protected RRaw doString(String operand) {
        // need to cast to int to catch conversion warnings
        int intVal = RRuntime.string2int(operand);
        if (RRuntime.isNA(intVal)) {
            warningBranch.enter();
            RError.warning(getEncapsulatingSourceSection(), RError.Message.NA_INTRODUCED_COERCION);
        }
        return doInt(intVal);
    }

    private RRawVector createResultVector(RAbstractVector operand, byte[] bdata) {
        RRawVector ret = RDataFactory.createRawVector(bdata, getPreservedDimensions(operand), getPreservedNames(operand));
        preserveDimensionNames(operand, ret);
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(operand);
        }
        return ret;
    }

    @Specialization
    protected RRawVector doIntVector(RAbstractIntVector operand) {
        int length = operand.getLength();
        byte[] bdata = new byte[length];
        boolean warning = false;
        for (int i = 0; i < length; i++) {
            int intValue = operand.getDataAt(i);
            int intRawValue = RRuntime.int2rawIntValue(intValue);
            if (intRawValue != intValue) {
                warningBranch.enter();
                warning = true;
                intRawValue = 0;
            }
            bdata[i] = (byte) intRawValue;
        }
        if (warning) {
            RError.warning(getEncapsulatingSourceSection(), RError.Message.OUT_OF_RANGE);
        }
        return createResultVector(operand, bdata);
    }

    @Specialization
    protected RRawVector doLogicalVector(RLogicalVector operand) {
        byte[] bdata = new byte[operand.getLength()];
        boolean warning = false;
        for (int i = 0; i < operand.getLength(); i++) {
            int intVal = RRuntime.logical2int(operand.getDataAt(i));
            int intRawValue = RRuntime.int2rawIntValue(intVal);
            if (intVal != intRawValue) {
                warningBranch.enter();
                warning = true;
                intRawValue = 0;
            }
            bdata[i] = (byte) intRawValue;
        }
        if (warning) {
            RError.warning(getEncapsulatingSourceSection(), RError.Message.OUT_OF_RANGE);
        }
        return createResultVector(operand, bdata);
    }

    @Specialization
    protected RRawVector doStringVector(RStringVector operand) {
        byte[] bdata = new byte[operand.getLength()];
        boolean naCoercionWarning = false;
        boolean outOfRangeWarning = false;
        for (int i = 0; i < operand.getLength(); i++) {
            int intVal = RRuntime.string2int(operand.getDataAt(i));
            int intRawValue = RRuntime.int2rawIntValue(intVal);
            if (RRuntime.isNA(intVal)) {
                warningBranch.enter();
                naCoercionWarning = true;
            }
            if (intVal != intRawValue) {
                warningBranch.enter();
                outOfRangeWarning = true;
                intRawValue = 0;
            }
            bdata[i] = (byte) intRawValue;
        }
        if (naCoercionWarning) {
            RError.warning(getEncapsulatingSourceSection(), RError.Message.NA_INTRODUCED_COERCION);
        }
        if (outOfRangeWarning) {
            RError.warning(getEncapsulatingSourceSection(), RError.Message.OUT_OF_RANGE);
        }
        return createResultVector(operand, bdata);
    }

    @Specialization
    protected RRawVector doComplexVector(RComplexVector operand) {
        byte[] bdata = new byte[operand.getLength()];
        boolean imaginaryDiscardedWarning = false;
        boolean outOfRangeWarning = false;
        for (int i = 0; i < operand.getLength(); i++) {
            RComplex complexVal = operand.getDataAt(i);
            int intRawValue = RRuntime.complex2rawIntValue(complexVal);
            if (complexVal.getImaginaryPart() != 0.0) {
                warningBranch.enter();
                imaginaryDiscardedWarning = true;
            }
            if ((int) complexVal.getRealPart() != intRawValue) {
                warningBranch.enter();
                outOfRangeWarning = true;
                intRawValue = 0;
            }
            bdata[i] = (byte) intRawValue;
        }
        if (imaginaryDiscardedWarning) {
            RError.warning(getEncapsulatingSourceSection(), RError.Message.IMAGINARY_PARTS_DISCARDED_IN_COERCION);
        }
        if (outOfRangeWarning) {
            RError.warning(getEncapsulatingSourceSection(), RError.Message.OUT_OF_RANGE);
        }
        return createResultVector(operand, bdata);
    }

    @Specialization
    protected RRawVector doDoubleVector(RAbstractDoubleVector operand) {
        int length = operand.getLength();
        byte[] bdata = new byte[length];
        boolean warning = false;
        for (int i = 0; i < length; i++) {
            double doubleValue = operand.getDataAt(i);
            int intRawValue = RRuntime.double2rawIntValue(doubleValue);
            if (intRawValue != (int) doubleValue) {
                warningBranch.enter();
                warning = true;
                intRawValue = 0;
            }
            bdata[i] = (byte) intRawValue;
        }
        if (warning) {
            RError.warning(getEncapsulatingSourceSection(), RError.Message.OUT_OF_RANGE);
        }
        return createResultVector(operand, bdata);
    }

    @Specialization
    protected RRawVector doRawVector(RRawVector operand) {
        return operand;
    }

    @Fallback
    @TruffleBoundary
    protected int doOther(Object operand) {
        throw new ConversionFailedException(operand.getClass().getName());
    }

    public static CastRawNode create() {
        return CastRawNodeGen.create(true, true, true);
    }

    public static CastRawNode createNonPreserving() {
        return CastRawNodeGen.create(false, false, false);
    }
}
