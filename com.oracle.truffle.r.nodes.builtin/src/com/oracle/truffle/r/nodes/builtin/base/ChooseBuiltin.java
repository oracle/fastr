/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.asIntegerVector;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.logicalValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.numericValue;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import java.util.function.IntToDoubleFunction;
import java.util.function.IntUnaryOperator;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.nmath.Choose;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

/**
 * Binomial coefficients (n, k) for real n and integral k (rounded with warning).
 */
@RBuiltin(name = "choose", kind = INTERNAL, parameterNames = {"n", "k"}, behavior = PURE)
public abstract class ChooseBuiltin extends RBuiltinNode {

    private final NACheck na = NACheck.create();

    static {
        Casts casts = new Casts(ChooseBuiltin.class);
        casts.arg("n").mustBe(numericValue(), RError.SHOW_CALLER, Message.NON_NUMERIC_MATH).mapIf(logicalValue(), asIntegerVector());
        casts.arg("k").mustBe(numericValue(), RError.SHOW_CALLER, Message.NON_NUMERIC_MATH).mapIf(logicalValue(), asIntegerVector());
    }

    @Specialization
    protected RAbstractDoubleVector doInts(RAbstractIntVector n, RAbstractIntVector k) {
        // Note: we may check overflow, return int vector if possible, otherwise specialize
        // Note: may be useful to specialize on small n and k and do only integer arithmetic
        return choose(n.getLength(), idx -> (double) n.getDataAt(idx), k.getLength(), idx -> k.getDataAt(idx));
    }

    @Specialization
    protected RAbstractDoubleVector doDoubleInt(RAbstractDoubleVector n, RAbstractIntVector k) {
        return choose(n.getLength(), idx -> n.getDataAt(idx), k.getLength(), idx -> k.getDataAt(idx));
    }

    // In the cases where 'k' is real vector we round values in 'k' to integers. Warning is shown
    // only when the numbers indeed loose precision and is show for each such instance. For example,
    // if k is two times shorter than n, for each non-integral value the warning will be shown
    // twice.

    @Specialization
    protected RAbstractDoubleVector doIntDouble(RAbstractIntVector n, RAbstractDoubleVector k) {
        return choose(n.getLength(), idx -> (double) n.getDataAt(idx), k.getLength(), idx -> castToInt(k, idx));
    }

    @Specialization
    protected RAbstractDoubleVector doDoubles(RAbstractDoubleVector n, RAbstractDoubleVector k) {
        return choose(n.getLength(), idx -> n.getDataAt(idx), k.getLength(), idx -> castToInt(k, idx));
    }

    private int castToInt(RAbstractDoubleVector k, int idx) {
        double data = k.getDataAt(idx);
        if (data == RRuntime.DOUBLE_NA) {
            return RRuntime.INT_NA;
        }

        double result = Math.round(data);
        if (result != data) {
            RError.warning(this, Message.CHOOSE_ROUNDING_WARNING, data, (int) result);
        }
        return (int) result;
    }

    private RAbstractDoubleVector choose(int nLength, IntToDoubleFunction getN, int kLength, IntUnaryOperator getK) {
        int resultLen = Math.max(nLength, kLength);
        double[] result = new double[resultLen];
        na.enable(true);
        for (int i = 0, nIdx = 0, kIdx = 0; i < resultLen; i++) {
            result[i] = choose(getN.applyAsDouble(nIdx), getK.applyAsInt(kIdx));
            na.check(result[i]);
            nIdx = Utils.incMod(nIdx, nLength);
            kIdx = Utils.incMod(kIdx, kLength);
        }
        return RDataFactory.createDoubleVector(result, na.neverSeenNA());
    }

    public static double choose(double n, int k) {
        if (RRuntime.isNA(n) || RRuntime.isNA(k)) {
            return RRuntime.DOUBLE_NA;
        }
        return Choose.choose(n, k);
    }
}
