/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.asDoubleVector;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.complexValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.notEmpty;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.numericValue;
import static com.oracle.truffle.r.runtime.RDispatch.MATH_GROUP_GENERIC;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.attributes.CopyAttributesNode;
import com.oracle.truffle.r.nodes.attributes.CopyAttributesNodeGen;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RComplexVector;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RIntVector;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess.SequentialIterator;

@RBuiltin(name = "signif", kind = PRIMITIVE, parameterNames = {"x", "digits"}, dispatch = MATH_GROUP_GENERIC, behavior = PURE)
public abstract class Signif extends RBuiltinNode.Arg2 {

    @Override
    public Object[] getDefaultParameterValues() {
        return new Object[]{RMissing.instance, 6};
    }

    @Child private CopyAttributesNode copyAttributes = CopyAttributesNodeGen.create(true);

    static {
        Casts casts = new Casts(Signif.class);
        casts.arg("x").defaultError(RError.Message.NON_NUMERIC_MATH).mustBe(numericValue().or(complexValue())).mapIf(complexValue().not(),
                        asDoubleVector(true, true, true));
        // TODO: for the error messages to be consistent with GNU R we should chack for notEmpty()
        // first but it does not seem to be possible currently as numericValue() cannot be used on
        // the result of asVector()
        casts.arg("digits").defaultError(RError.Message.NON_NUMERIC_MATH).mustBe(numericValue()).asIntegerVector().mustBe(notEmpty(), RError.Message.INVALID_ARG_OF_LENGTH, "second", 0);
    }

    // TODO: consider porting signif implementation from GNU R
    @Specialization(guards = {"xAccess.supports(x)", "digitsAccess.supports(digits)"}, limit = "getVectorAccessCacheSize()")
    protected RAbstractDoubleVector signifDouble(RAbstractDoubleVector x, RIntVector digits,
                    @Cached("x.access()") VectorAccess xAccess,
                    @Cached("digits.access()") VectorAccess digitsAccess,
                    @Cached("create()") BranchProfile emptyProfile,
                    @Cached("create()") BranchProfile identityProfile,
                    @Cached("createBinaryProfile()") ConditionProfile infProfile) {
        try (SequentialIterator xIter = xAccess.access(x);
                        SequentialIterator digitsIter = digitsAccess.access(digits)) {
            int xLength = xAccess.getLength(xIter);
            if (xLength == 0) {
                emptyProfile.enter();
                return RDataFactory.createEmptyDoubleVector();
            }
            int digitsLength = digitsAccess.getLength(digitsIter); // always >0
            int resultLength = Math.max(xLength, digitsLength);
            double[] resultData = new double[resultLength];
            boolean resultComplete = true;
            for (int i = 0; i < resultLength; i++) {
                xAccess.nextWithWrap(xIter);
                digitsAccess.nextWithWrap(digitsIter);
                double res;
                if (digitsAccess.isNA(digitsIter) || xAccess.isNA(xIter)) {
                    res = RRuntime.DOUBLE_NA;
                    resultComplete = false;
                } else {
                    int digitCount = digitsAccess.getInt(digitsIter);
                    if (digitCount > 22) {
                        identityProfile.enter();
                        res = xAccess.getDouble(xIter);
                    } else {
                        if (digitCount <= 0) {
                            digitCount = 1;
                        }
                        double val = xAccess.getDouble(xIter);
                        if (infProfile.profile(Double.isNaN(val))) {
                            res = Double.NaN;
                        } else if (infProfile.profile(Double.isInfinite(val))) {
                            res = Double.POSITIVE_INFINITY;
                        } else {
                            res = bigIntegerSignif(digitCount, val);
                        }
                    }
                }
                resultData[i] = res;
            }
            RDoubleVector resultVec = RDataFactory.createDoubleVector(resultData, resultComplete);
            resultVec = (RDoubleVector) copyAttributes.execute(resultVec, x, xLength, digits, digitsLength);
            return resultVec;
        }
    }

    @Specialization(replaces = "signifDouble")
    protected RAbstractDoubleVector signifDoubleGeneric(RAbstractDoubleVector x, RIntVector digits) {
        return signifDouble(x, digits, x.slowPathAccess(), digits.slowPathAccess(),
                        BranchProfile.create(), BranchProfile.create(), ConditionProfile.createBinaryProfile());
    }

    @Specialization(guards = {"xAccess.supports(x)", "digitsAccess.supports(digits)"}, limit = "getVectorAccessCacheSize()")
    protected RAbstractComplexVector signifComplex(RAbstractComplexVector x, RIntVector digits,
                    @Cached("x.access()") VectorAccess xAccess,
                    @Cached("digits.access()") VectorAccess digitsAccess,
                    @Cached("create()") BranchProfile emptyProfile,
                    @Cached("create()") BranchProfile identityProfile,
                    @Cached("createBinaryProfile()") ConditionProfile infRProfile,
                    @Cached("createBinaryProfile()") ConditionProfile infIProfile) {
        SequentialIterator xIter = xAccess.access(x);
        SequentialIterator digitsIter = digitsAccess.access(digits);
        int xLength = xAccess.getLength(xIter);
        if (xLength == 0) {
            emptyProfile.enter();
            return RDataFactory.createEmptyComplexVector();
        }
        int digitsLength = digitsAccess.getLength(digitsIter); // always >0
        int resultLength = Math.max(xLength, digitsLength);
        double[] resultData = new double[resultLength << 1];
        boolean resultComplete = true;
        for (int i = 0; i < resultData.length;) {
            xAccess.nextWithWrap(xIter);
            digitsAccess.nextWithWrap(digitsIter);
            double resR;
            double resI;
            if (digitsAccess.isNA(digitsIter) || xAccess.isNA(xIter)) {
                resR = RRuntime.DOUBLE_NA;
                resI = RRuntime.DOUBLE_NA;
                resultComplete = false;
            } else {
                int digitCount = digitsAccess.getInt(digitsIter);
                if (digitCount > 22) {
                    identityProfile.enter();
                    resR = xAccess.getComplexR(xIter);
                    resI = xAccess.getComplexI(xIter);
                } else {
                    if (digitCount <= 0) {
                        digitCount = 1;
                    }
                    double valR = xAccess.getComplexR(xIter);
                    double valI = xAccess.getComplexI(xIter);
                    if (infRProfile.profile(Double.isInfinite(valR))) {
                        resR = Double.POSITIVE_INFINITY;
                    } else {
                        resR = bigIntegerSignif(digitCount, valR);
                    }
                    if (infIProfile.profile(Double.isInfinite(valI))) {
                        resI = Double.POSITIVE_INFINITY;
                    } else {
                        resI = bigIntegerSignif(digitCount, valI);
                    }
                }
            }
            resultData[i++] = resR;
            resultData[i++] = resI;
        }
        RComplexVector resultVec = RDataFactory.createComplexVector(resultData, resultComplete);
        resultVec = (RComplexVector) copyAttributes.execute(resultVec, x, xLength, digits, digitsLength);
        return resultVec;
    }

    @Specialization(replaces = "signifComplex")
    protected RAbstractComplexVector signifComplexGeneric(RAbstractComplexVector x, RIntVector digits) {
        return signifComplex(x, digits, x.slowPathAccess(), digits.slowPathAccess(),
                        BranchProfile.create(), BranchProfile.create(), ConditionProfile.createBinaryProfile(), ConditionProfile.createBinaryProfile());
    }

    @TruffleBoundary
    private static double bigIntegerSignif(int digits, double val) {
        if (RRuntime.isNAorNaN(val)) {
            return val;
        }
        BigDecimal bigDecimalVal = new BigDecimal(val, new MathContext(digits, RoundingMode.HALF_EVEN));
        return bigDecimalVal.doubleValue();
    }
}
