/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.asDoubleVector;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.asIntegerVector;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.complexValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.integerValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.logicalValue;
import static com.oracle.truffle.r.runtime.RDispatch.MATH_GROUP_GENERIC;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import java.util.Arrays;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RAttributeProfiles;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntSequence;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

@RBuiltin(name = "cummin", kind = PRIMITIVE, parameterNames = {"x"}, dispatch = MATH_GROUP_GENERIC, behavior = PURE)
public abstract class CumMin extends RBuiltinNode {

    private final NACheck na = NACheck.create();
    private final RAttributeProfiles attrProfiles = RAttributeProfiles.create();

    @Override
    protected void createCasts(CastBuilder casts) {
        casts.arg("x").mustBe(complexValue().not(), RError.Message.CUMMIN_UNDEFINED_FOR_COMPLEX).mapIf(integerValue().or(logicalValue()), asIntegerVector(), asDoubleVector());
    }

    @Specialization
    protected double cummin(double arg) {
        return arg;
    }

    @Specialization
    protected int cummin(int arg) {
        return arg;
    }

    @Specialization
    protected RDoubleVector cumNull(@SuppressWarnings("unused") RNull rnull) {
        return RDataFactory.createEmptyDoubleVector();
    }

    @Specialization
    protected RAbstractIntVector cumminIntSequence(RIntSequence v, //
                    @Cached("createBinaryProfile()") ConditionProfile negativeStrideProfile) {
        if (negativeStrideProfile.profile(v.getStride() > 0)) {
            // all numbers are bigger than the first one
            return RDataFactory.createIntSequence(v.getStart(), 0, v.getLength());
        } else {
            return v;
        }
    }

    @Specialization
    protected RDoubleVector cummin(RAbstractDoubleVector v) {
        double[] cminV = new double[v.getLength()];
        double min = v.getDataAt(0);
        cminV[0] = min;
        na.enable(v);
        int i;
        for (i = 1; i < v.getLength(); i++) {
            if (v.getDataAt(i) < min) {
                min = v.getDataAt(i);
            }
            if (na.check(v.getDataAt(i))) {
                break;
            }
            cminV[i] = min;
        }
        if (!na.neverSeenNA()) {
            Arrays.fill(cminV, i, cminV.length, RRuntime.DOUBLE_NA);
        }
        return RDataFactory.createDoubleVector(cminV, na.neverSeenNA(), v.getNames(attrProfiles));
    }

    @Specialization(contains = "cumminIntSequence")
    protected RIntVector cummin(RAbstractIntVector v) {
        int[] cminV = new int[v.getLength()];
        int min = v.getDataAt(0);
        cminV[0] = min;
        na.enable(v);
        int i;
        for (i = 1; i < v.getLength(); i++) {
            if (v.getDataAt(i) < min) {
                min = v.getDataAt(i);
            }
            if (na.check(v.getDataAt(i))) {
                break;
            }
            cminV[i] = min;
        }
        if (!na.neverSeenNA()) {
            Arrays.fill(cminV, i, cminV.length, RRuntime.INT_NA);
        }
        return RDataFactory.createIntVector(cminV, na.neverSeenNA(), v.getNames(attrProfiles));
    }

}
