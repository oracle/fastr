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
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.numericValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.complexValue;
import static com.oracle.truffle.r.runtime.RDispatch.COMPLEX_GROUP_GENERIC;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

@RBuiltin(name = "Arg", kind = PRIMITIVE, parameterNames = {"z"}, dispatch = COMPLEX_GROUP_GENERIC, behavior = PURE)
public abstract class Arg extends RBuiltinNode {

    private final ConditionProfile signumProfile = ConditionProfile.createBinaryProfile();
    private final NACheck naCheck = NACheck.create();

    @Override
    protected void createCasts(CastBuilder casts) {
        casts.arg("z").mustBe(numericValue().or(complexValue()), RError.Message.NON_NUMERIC_ARGUMENT_FUNCTION);
    }

    @Specialization
    protected double arg(double z) {
        naCheck.enable(z);
        if (naCheck.check(z)) {
            return RRuntime.DOUBLE_NA;
        }
        if (signumProfile.profile(z >= 0)) {
            return 0;
        } else {
            return Math.PI;
        }
    }

    @Specialization
    protected RAbstractDoubleVector arg(RAbstractDoubleVector v) {
        double[] result = new double[v.getLength()];

        naCheck.enable(v);
        for (int i = 0; i < v.getLength(); i++) {
            double z = v.getDataAt(i);
            if (naCheck.check(z)) {
                result[i] = RRuntime.DOUBLE_NA;
            } else {
                result[i] = z >= 0 ? 0 : Math.PI;
            }
        }

        return RDataFactory.createDoubleVector(result, v.isComplete());
    }

    @Specialization
    protected double arg(int z) {
        naCheck.enable(z);
        if (naCheck.check(z)) {
            return RRuntime.DOUBLE_NA;
        }
        if (signumProfile.profile(z >= 0)) {
            return 0;
        } else {
            return Math.PI;
        }
    }

    @Specialization
    protected RAbstractDoubleVector arg(RAbstractIntVector v) {
        double[] result = new double[v.getLength()];

        naCheck.enable(v);
        for (int i = 0; i < v.getLength(); i++) {
            int z = v.getDataAt(i);
            if (naCheck.check(z)) {
                result[i] = RRuntime.DOUBLE_NA;
            } else {
                result[i] = z >= 0 ? 0 : Math.PI;
            }
        }

        return RDataFactory.createDoubleVector(result, v.isComplete());
    }

    @Specialization
    protected double arg(byte z) {
        naCheck.enable(z);
        if (naCheck.check(z)) {
            return RRuntime.DOUBLE_NA;
        }
        return 0;
    }

    @Specialization
    protected RAbstractDoubleVector arg(RAbstractLogicalVector v) {
        double[] result = new double[v.getLength()];

        naCheck.enable(v);
        for (int i = 0; i < v.getLength(); i++) {
            int z = v.getDataAt(i);
            if (naCheck.check(z)) {
                result[i] = RRuntime.DOUBLE_NA;
            } else {
                result[i] = 0;
            }
        }

        return RDataFactory.createDoubleVector(result, v.isComplete());
    }

    @Specialization
    protected RAbstractDoubleVector arg(RAbstractComplexVector v) {
        double[] result = new double[v.getLength()];

        naCheck.enable(v);
        for (int i = 0; i < v.getLength(); i++) {
            RComplex z = v.getDataAt(i);
            if (naCheck.check(z)) {
                result[i] = RRuntime.DOUBLE_NA;
            } else {
                result[i] = Math.atan2(z.getImaginaryPart(), z.getRealPart());
            }
        }

        return RDataFactory.createDoubleVector(result, v.isComplete());
    }
}
