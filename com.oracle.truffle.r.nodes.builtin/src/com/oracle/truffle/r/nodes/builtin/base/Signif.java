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

import static com.oracle.truffle.r.runtime.RBuiltinKind.PRIMITIVE;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RAttributeProfiles;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

@RBuiltin(name = "signif", kind = PRIMITIVE, parameterNames = {"x", "digits"})
public abstract class Signif extends RBuiltinNode {

    @Override
    public Object[] getDefaultParameterValues() {
        return new Object[]{RMissing.instance, 6};
    }

    private final NACheck naCheck = NACheck.create();
    private final BranchProfile identity = BranchProfile.create();
    private final ConditionProfile infProfile = ConditionProfile.createBinaryProfile();
    private final RAttributeProfiles attrProfiles = RAttributeProfiles.create();

    @Override
    protected void createCasts(CastBuilder casts) {
        casts.toInteger(1);
    }

    // TODO: consider porting signif implementation from GNU R

    @Specialization(guards = "digitsVec.getLength() == 1")
    protected RAbstractDoubleVector signif(RAbstractDoubleVector x, RAbstractIntVector digitsVec) {
        controlVisibility();
        int digits = digitsVec.getDataAt(0) <= 0 ? 1 : digitsVec.getDataAt(0);
        if (digits > 22) {
            identity.enter();
            return x;
        }
        double[] data = new double[x.getLength()];
        naCheck.enable(x);
        for (int i = 0; i < x.getLength(); i++) {
            double val = x.getDataAt(i);
            double result;
            if (naCheck.check(val)) {
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
        RDoubleVector ret = RDataFactory.createDoubleVector(data, naCheck.neverSeenNA());
        ret.copyAttributesFrom(attrProfiles, x);
        return ret;
    }

    @TruffleBoundary
    private static double bigIntegerSignif(int digits, double val) {
        BigDecimal bigDecimalVal = new BigDecimal(val, new MathContext(digits, RoundingMode.HALF_UP));
        return bigDecimalVal.doubleValue();
    }

    @Specialization(guards = "digits.getLength() == 1")
    protected RAbstractIntVector roundDigits(RAbstractIntVector x, @SuppressWarnings("unused") RAbstractIntVector digits) {
        controlVisibility();
        return x;
    }

    // TODO: add support for digit vectors of length different than 1

}
