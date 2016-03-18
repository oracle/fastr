/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.util.function.IntFunction;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RComplexVector;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RRaw;
import com.oracle.truffle.r.runtime.data.RRawVector;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.ops.na.NACheck;
import com.oracle.truffle.r.runtime.ops.na.NAProfile;

public abstract class CastComplexNode extends CastBaseNode {

    private final NACheck naCheck = NACheck.create();
    private final NAProfile naProfile = NAProfile.create();
    private final BranchProfile warningBranch = BranchProfile.create();

    public abstract Object executeComplex(int o);

    public abstract Object executeComplex(double o);

    public abstract Object executeComplex(byte o);

    public abstract Object executeComplex(Object o);

    @Specialization
    protected RNull doNull(@SuppressWarnings("unused") RNull operand) {
        return RNull.instance;
    }

    @Specialization
    protected RComplex doInt(int operand) {
        naCheck.enable(operand);
        return naCheck.convertIntToComplex(operand);
    }

    @Specialization
    protected RComplex doDouble(double operand) {
        naCheck.enable(operand);
        return naCheck.convertDoubleToComplex(operand);
    }

    @Specialization
    protected RComplex doLogical(byte operand) {
        naCheck.enable(operand);
        return naCheck.convertLogicalToComplex(operand);
    }

    @Specialization
    protected RComplex doComplex(RComplex operand) {
        return operand;
    }

    @Specialization
    protected RComplex doRaw(RRaw operand) {
        return RDataFactory.createComplex(operand.getValue(), 0);
    }

    @Specialization
    protected RComplex doCharacter(String operand, //
                    @Cached("createBinaryProfile()") ConditionProfile emptyStringProfile) {
        naCheck.enable(operand);
        if (naCheck.check(operand) || emptyStringProfile.profile(operand.isEmpty())) {
            return RComplex.createNA();
        }
        RComplex result = RRuntime.string2complexNoCheck(operand);
        if (RRuntime.isNA(result)) {
            warningBranch.enter();
            RError.warning(this, RError.Message.NA_INTRODUCED_COERCION);
        }
        return result;
    }

    private RComplexVector createResultVector(RAbstractVector operand, IntFunction<RComplex> elementFunction) {
        naCheck.enable(operand);
        double[] ddata = new double[operand.getLength() << 1];
        boolean seenNA = false;
        for (int i = 0; i < operand.getLength(); i++) {
            RComplex complexValue = elementFunction.apply(i);
            int index = i << 1;
            ddata[index] = complexValue.getRealPart();
            ddata[index + 1] = complexValue.getImaginaryPart();
            seenNA = seenNA || naProfile.isNA(complexValue);
        }
        RComplexVector ret = RDataFactory.createComplexVector(ddata, !seenNA, getPreservedDimensions(operand), getPreservedNames(operand));
        preserveDimensionNames(operand, ret);
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(operand);
        }
        return ret;
    }

    @Specialization
    protected RComplexVector doIntVector(RAbstractIntVector operand) {
        return createResultVector(operand, index -> naCheck.convertIntToComplex(operand.getDataAt(index)));
    }

    @Specialization
    protected RComplexVector doDoubleVector(RAbstractDoubleVector operand) {
        return createResultVector(operand, index -> naCheck.convertDoubleToComplex(operand.getDataAt(index)));
    }

    @Specialization
    protected RComplexVector doLogicalVector(RLogicalVector operand) {
        return createResultVector(operand, index -> naCheck.convertLogicalToComplex(operand.getDataAt(index)));
    }

    @Specialization
    protected RComplexVector doStringVector(RStringVector operand, //
                    @Cached("createBinaryProfile()") ConditionProfile emptyStringProfile) {
        naCheck.enable(operand);
        double[] ddata = new double[operand.getLength() << 1];
        boolean seenNA = false;
        boolean warning = false;
        for (int i = 0; i < operand.getLength(); i++) {
            String value = operand.getDataAt(i);
            RComplex complexValue;
            if (naCheck.check(value) || emptyStringProfile.profile(value.isEmpty())) {
                complexValue = RComplex.createNA();
                seenNA = true;
            } else {
                complexValue = RRuntime.string2complexNoCheck(value);
                if (naProfile.isNA(complexValue)) {
                    seenNA = true;
                    if (!value.isEmpty()) {
                        warningBranch.enter();
                        warning = true;
                    }
                }
            }
            int index = i << 1;
            ddata[index] = complexValue.getRealPart();
            ddata[index + 1] = complexValue.getImaginaryPart();
        }
        if (warning) {
            RError.warning(this, RError.Message.NA_INTRODUCED_COERCION);
        }
        RComplexVector ret = RDataFactory.createComplexVector(ddata, !seenNA, getPreservedDimensions(operand), getPreservedNames(operand));
        preserveDimensionNames(operand, ret);
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(operand);
        }
        return ret;
    }

    @Specialization
    protected RComplexVector doComplexVector(RComplexVector vector) {
        return vector;
    }

    @Specialization
    protected RComplexVector doRawVector(RRawVector operand) {
        return createResultVector(operand, index -> RDataFactory.createComplex(operand.getDataAt(index).getValue(), 0));
    }

    @Fallback
    @TruffleBoundary
    protected int doOther(Object operand) {
        throw new ConversionFailedException(operand.getClass().getName());
    }

    public static CastComplexNode create() {
        return CastComplexNodeGen.create(true, true, true);
    }

    public static CastComplexNode createNonPreserving() {
        return CastComplexNodeGen.create(false, false, false);
    }
}
