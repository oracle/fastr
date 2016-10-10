/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.*;
import static com.oracle.truffle.r.runtime.RDispatch.MATH_GROUP_GENERIC;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.attributes.CopyAttributesNode;
import com.oracle.truffle.r.nodes.attributes.CopyAttributesNodeGen;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

@RBuiltin(name = "signif", kind = PRIMITIVE, parameterNames = {"x", "digits"}, dispatch = MATH_GROUP_GENERIC, behavior = PURE)
public abstract class Signif extends RBuiltinNode {

    @Override
    public Object[] getDefaultParameterValues() {
        return new Object[]{RMissing.instance, 6};
    }

    @Child private CopyAttributesNode copyAttributes = CopyAttributesNodeGen.create(true);
    private final NACheck naCheck = NACheck.create();
    private final BranchProfile empty = BranchProfile.create();
    private final BranchProfile identity = BranchProfile.create();
    private final ConditionProfile infProfile = ConditionProfile.createBinaryProfile();

    @Override
    protected void createCasts(CastBuilder casts) {
        casts.arg("x").defaultError(RError.Message.NON_NUMERIC_MATH).mustBe(numericValue().or(complexValue())).mapIf(complexValue().not(),
                        asDoubleVector(true, true, true));
        // TODO: for the error messages to be consistent with GNU R we should chack for notEmpty()
        // first but it does not seem to be possible currently as numericValue() cannot be used on
        // the result of asVector()
        casts.arg("digits").defaultError(RError.Message.NON_NUMERIC_MATH).mustBe(numericValue()).asIntegerVector().mustBe(notEmpty(), RError.Message.INVALID_ARG_OF_LENGTH, "second", 0);
    }

    // TODO: consider porting signif implementation from GNU R

    @Specialization
    protected RAbstractDoubleVector signif(RAbstractDoubleVector x, RAbstractIntVector digitsVec) {
        int xLength = x.getLength();
        if (x.getLength() == 0) {
            empty.enter();
            return RDataFactory.createEmptyDoubleVector();
        }
        int digitsVecLength = digitsVec.getLength();
        int maxLength = Math.max(xLength, digitsVecLength);
        double[] data = new double[maxLength];
        int xInd = 0;
        int digitsVecInd = 0;
        for (int i = 0; i < maxLength; i++) {
            int digits = digitsVec.getDataAt(digitsVecInd);
            if (digits > 22) {
                identity.enter();
                data[i] = x.getDataAt(xInd);
            } else {
                if (digits <= 0) {
                    digits = 1;
                }
                naCheck.enable(!(x.isComplete() && digitsVec.isComplete()));
                double val = x.getDataAt(xInd);
                double result;
                if (naCheck.check(val)) {
                    result = RRuntime.DOUBLE_NA;
                } else if (naCheck.check(digits)) {
                    result = RRuntime.DOUBLE_NA;
                } else {
                    if (infProfile.profile(Double.isInfinite(val))) {
                        result = Double.POSITIVE_INFINITY;
                    } else {
                        result = bigIntegerSignif(digits, val);
                    }
                }
                data[i] = result;
            }
            xInd = Utils.incMod(xInd, xLength);
            digitsVecInd = Utils.incMod(digitsVecInd, digitsVecLength);
        }
        RDoubleVector ret = RDataFactory.createDoubleVector(data, naCheck.neverSeenNA());
        ret = (RDoubleVector) copyAttributes.execute(ret, x, xLength, digitsVec, digitsVecLength);
        return ret;
    }

    @SuppressWarnings("unused")
    @Specialization
    protected RAbstractComplexVector signif(RAbstractComplexVector x, RAbstractIntVector digitsVec) {
        // TODO: implement for complex numbers but I am not sure GNU R gets it right:
        // > signif(42.1234-7.1234i, 1)
        // [1] 40-10i

        throw RInternalError.unimplemented();
    }

    @TruffleBoundary
    private static double bigIntegerSignif(int digits, double val) {
        BigDecimal bigDecimalVal = new BigDecimal(val, new MathContext(digits, RoundingMode.HALF_UP));
        return bigDecimalVal.doubleValue();
    }

}
