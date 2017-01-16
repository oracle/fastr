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
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetNamesAttributeNode;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntSequence;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

@RBuiltin(name = "cummax", kind = PRIMITIVE, parameterNames = {"x"}, dispatch = MATH_GROUP_GENERIC, behavior = PURE)
public abstract class CumMax extends RBuiltinNode {

    private final NACheck na = NACheck.create();
    @Child private GetNamesAttributeNode getNamesNode = GetNamesAttributeNode.create();

    @Override
    protected void createCasts(CastBuilder casts) {
        casts.arg("x").allowNull().mustBe(complexValue().not(), RError.Message.CUMMAX_UNDEFINED_FOR_COMPLEX).mapIf(integerValue().or(logicalValue()), asIntegerVector(),
                        asDoubleVector());
    }

    @Specialization
    protected double cummax(double arg) {
        return arg;
    }

    @Specialization
    protected int cummax(int arg) {
        return arg;
    }

    @Specialization
    protected RDoubleVector cumNull(@SuppressWarnings("unused") RNull rnull) {
        return RDataFactory.createEmptyDoubleVector();
    }

    @Specialization(guards = "emptyVec.getLength()==0")
    protected RAbstractVector cumEmpty(RAbstractVector emptyVec) {
        return emptyVec;
    }

    @Specialization
    protected RAbstractIntVector cummaxIntSequence(RIntSequence v, //
                    @Cached("createBinaryProfile()") ConditionProfile negativeStrideProfile) {
        if (negativeStrideProfile.profile(v.getStride() < 0)) {
            // all numbers are smaller than the first one
            return RDataFactory.createIntSequence(v.getStart(), 0, v.getLength());
        } else {
            return v;
        }
    }

    @Specialization
    protected RDoubleVector cummax(RAbstractDoubleVector v) {
        double[] cmaxV = new double[v.getLength()];
        double max = v.getDataAt(0);
        cmaxV[0] = max;
        na.enable(v);
        int i;
        for (i = 1; i < v.getLength(); i++) {
            if (v.getDataAt(i) > max) {
                max = v.getDataAt(i);
            }
            if (na.check(v.getDataAt(i))) {
                break;
            }
            cmaxV[i] = max;
        }
        if (!na.neverSeenNA()) {
            Arrays.fill(cmaxV, i, cmaxV.length, RRuntime.DOUBLE_NA);
        }
        return RDataFactory.createDoubleVector(cmaxV, na.neverSeenNA(), getNamesNode.getNames(v));
    }

    @Specialization(contains = "cummaxIntSequence")
    protected RIntVector cummax(RAbstractIntVector v) {
        int[] cmaxV = new int[v.getLength()];
        int max = v.getDataAt(0);
        cmaxV[0] = max;
        na.enable(v);
        int i;
        for (i = 1; i < v.getLength(); i++) {
            if (v.getDataAt(i) > max) {
                max = v.getDataAt(i);
            }
            if (na.check(v.getDataAt(i))) {
                break;
            }
            cmaxV[i] = max;
        }
        if (!na.neverSeenNA()) {
            Arrays.fill(cmaxV, i, cmaxV.length, RRuntime.INT_NA);
        }
        return RDataFactory.createIntVector(cmaxV, na.neverSeenNA(), getNamesNode.getNames(v));
    }

}
