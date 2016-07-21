/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.util.function.IntToDoubleFunction;
import java.util.function.IntUnaryOperator;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.RBuiltinKind;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;

/**
 * Binomial coefficients (n, k) for real n and integral k (rounded with warning).
 */
@RBuiltin(name = "choose", kind = RBuiltinKind.INTERNAL, parameterNames = {"n", "k"})
public abstract class Choose extends RBuiltinNode {

    // TODO: cast of logicals to integers

    @Specialization
    protected RAbstractDoubleVector doInts(RAbstractIntVector n, RAbstractIntVector k) {
        // TODO: check overflow, return int vector if possible, otherwise specialize
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

    private static RAbstractDoubleVector choose(int nLength, IntToDoubleFunction getN, int kLength, IntUnaryOperator getK) {
        int resultLen = Math.max(nLength, kLength);
        boolean complete = true;
        double[] result = new double[resultLen];
        for (int i = 0, nIdx = 0, kIdx = 0; i < resultLen; i++) {
            result[i] = choose(getN.applyAsDouble(nIdx), getK.applyAsInt(kIdx));
            complete &= result[i] != RRuntime.DOUBLE_NA;
            nIdx = Utils.incMod(nIdx, nLength);
            kIdx = Utils.incMod(kIdx, kLength);
        }
        return RDataFactory.createDoubleVector(result, complete);
    }

    private static double choose(double n, int ka) {
        int k = ka;
        if (n == RRuntime.DOUBLE_NA || k == RRuntime.INT_NA) {
            return RRuntime.DOUBLE_NA;
        }

        // symmetry: this e.g. turns (n=3,k=3) into (n=3,k=0)
        int intN = (int) n;
        if (n == intN && intN - k < k && intN >= 0) {
            k = intN - k;
        }

        if (k < 0) {
            return 0;
        } else if (k == 0) {
            return 1;
        }

        // TODO: if n is too large (GnuR: > 30) use 'lchoose' and converts its result
        double result = n;
        for (int i = 2; i <= k; i++) {
            result *= (n - i + 1) / i;
        }
        return result;
    }
}
