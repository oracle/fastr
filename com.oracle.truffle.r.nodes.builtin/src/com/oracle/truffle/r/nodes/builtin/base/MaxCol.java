/*
 * Copyright (C) 1994-9 W. N. Venables and B. D. Ripley
 * Copyright (c) 2007-2017, The R Core Team
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, a copy is available at
 * https://www.R-project.org/Licenses/
 */
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.abstractVectorValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.gte;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.integerValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.lte;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.matrix;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import java.util.Arrays;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetDimAttributeNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDataFactory.VectorFactory;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess.RandomIterator;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess.SequentialIterator;
import com.oracle.truffle.r.runtime.rng.RRNG;

@RBuiltin(name = "max.col", kind = INTERNAL, parameterNames = {"x", "tie"}, behavior = PURE)
public abstract class MaxCol extends RBuiltinNode.Arg2 {
    private static final int TIE_RANDOM = 1;
    private static final int TIE_LAST = 3;
    private static final double EPSILON_FACTOR = 1e-5;

    static {
        Casts casts = new Casts(MaxCol.class);
        casts.arg("x").mustBe(abstractVectorValue().and(matrix()));
        casts.arg("tie").mustBe(integerValue()).asIntegerVector().findFirst().mustBe(gte(TIE_RANDOM).and(lte(TIE_LAST)));
    }

    public static MaxCol create() {
        return MaxColNodeGen.create();
    }

    private final ValueProfile tieProfile = ValueProfile.createEqualityProfile();

    @Specialization(guards = "xAccess.supports(x)")
    RAbstractIntVector findMaxCol(RAbstractContainer x, int tieArg,
                    @Cached("x.access()") VectorAccess xAccess,
                    @Cached("create()") VectorFactory vectorFactory,
                    @Cached("create()") GetDimAttributeNode getDimNode) {
        int nrows = getDimNode.nrows(x);
        int tie = tieProfile.profile(tieArg);

        // in the first iteration we do TIE_FIRST even for TIE_RANDOM to find out the maxima for
        // epsilon calculation. TIE_RANDOM ignores NAs and Inf when calculating the maxima
        boolean tieLast = tie == TIE_LAST;
        boolean tieRandom = tie == TIE_RANDOM;
        double[] maxVals = new double[nrows];
        Arrays.fill(maxVals, -Double.MAX_VALUE);
        int[] cols = new int[nrows];
        int resultIdx = 0;
        int colIdx = 1; // R indexing
        int naCount = 0;
        try (SequentialIterator it = xAccess.access(x)) {
            while (xAccess.next(it)) {
                double value = xAccess.getDouble(it);
                // skip rows for which we already got NA
                if (!xAccess.na.check(maxVals[resultIdx])) {
                    if (xAccess.na.check(value)) {
                        maxVals[resultIdx] = RRuntime.DOUBLE_NA;
                        cols[resultIdx] = RRuntime.INT_NA;
                        naCount++;
                        if (naCount == nrows) {
                            // all NAs we're done
                            return vectorFactory.createIntVector(cols, RDataFactory.INCOMPLETE_VECTOR);
                        }
                    } else {
                        double prevValue = maxVals[resultIdx];
                        boolean isLastTie = tieLast && prevValue == value;
                        boolean ignoreInf = tieRandom && Double.isInfinite(value);
                        if (!ignoreInf && (isLastTie || prevValue < value)) {
                            maxVals[resultIdx] = value;
                            cols[resultIdx] = colIdx;
                        }
                    }
                }
                resultIdx++;
                if (resultIdx == maxVals.length) {
                    resultIdx = 0;
                    colIdx++;
                }
            }
        }

        if (tie == TIE_RANDOM) {
            // tie random:
            // 1) tie is when two elements differ by no more than epsilon, which is relative to the
            // largest element.
            // 2) tie is broken randomly using pseudo-random numbers generator
            double[] epsilons = maxVals;
            for (int i = 0; i < epsilons.length; i++) {
                epsilons[i] *= EPSILON_FACTOR;
            }
            maxColRandom(cols, x, xAccess, getDimNode.ncols(x), nrows, epsilons);
        }
        return vectorFactory.createIntVector(cols, naCount == 0);
    }

    @Specialization(replaces = "findMaxCol")
    RAbstractIntVector findMaxColGeneric(RAbstractContainer x, int tie,
                    @Cached("create()") VectorFactory vectorFactory,
                    @Cached("create()") GetDimAttributeNode getDimNode) {
        return findMaxCol(x, tie, x.slowPathAccess(), vectorFactory, getDimNode);
    }

    private static void maxColRandom(int[] cols, RAbstractContainer x, VectorAccess xAccess, int ncols, int nrows, double[] epsilons) {
        // To be really compatible, because we use random numbers, we have to do this in the same
        // order as GNU R, which is row by row
        int colIdx;
        boolean usedRandom = false;
        try (RandomIterator it = xAccess.randomAccess(x)) {
            for (int rowIdx = 0; rowIdx < nrows; rowIdx++) {
                if (xAccess.na.check(cols[rowIdx])) {
                    // this row contains NA
                    continue;
                }
                double max = xAccess.getDouble(it, rowIdx);
                cols[rowIdx] = 1; // R indexing
                int ntie = 1;
                for (colIdx = 1; colIdx < ncols; colIdx++) {
                    double value = xAccess.getDouble(it, rowIdx + colIdx * nrows);
                    if (value > max + epsilons[rowIdx]) {
                        max = value;
                        cols[rowIdx] = colIdx + 1;
                        ntie = 1;
                    } else if (value >= max - epsilons[rowIdx]) {
                        ntie++;
                        if (!usedRandom) {
                            RRNG.getRNGState();
                            usedRandom = true;
                        }
                        if (ntie * RRNG.unifRand() < 1.) {
                            cols[rowIdx] = colIdx + 1;
                            // NOTE: GNU R also does not update the actual value
                        }
                    }
                }
            }
        }
        if (usedRandom) {
            RRNG.putRNGState();
        }
    }
}
