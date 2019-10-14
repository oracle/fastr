/*
 * Copyright (c) 1995, 1996  Robert Gentleman and Ross Ihaka
 * Copyright (c) 1997-2015,  The R Core Team
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates
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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.doubleValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.gte;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.gte0;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.integerValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.isFinite;
import static com.oracle.truffle.r.runtime.RError.Message.ALGORITHM_FOR_SIZE_N_DIV_2;
import static com.oracle.truffle.r.runtime.RError.Message.INVALID_ARGUMENT;
import static com.oracle.truffle.r.runtime.RError.Message.INVALID_FIRST_ARGUMENT;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.MODIFIES_STATE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.Collections.NonRecursiveHashSetDouble;
import com.oracle.truffle.r.runtime.Collections.NonRecursiveHashSetInt;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.rng.RRNG;
import static com.oracle.truffle.r.runtime.rng.RRNG.SampleKind.ROUNDING;

/**
 * Sample2 is more efficient special case implementation of {@link Sample}.
 */
@RBuiltin(name = "sample2", kind = INTERNAL, parameterNames = {"x", "size"}, behavior = MODIFIES_STATE)
public abstract class Sample2 extends RBuiltinNode.Arg2 {
    private static final double U = 33554432.0;
    static final double MAX_INT = Integer.MAX_VALUE;

    static {
        Casts casts = new Casts(Sample2.class);
        casts.arg("x").defaultError(INVALID_FIRST_ARGUMENT).mustBe(integerValue().or(doubleValue())).mustNotBeNA(
                        INVALID_FIRST_ARGUMENT).asDoubleVector().findFirst().mustBe(gte(0.0)).mustBe(isFinite());
        casts.arg("size").defaultError(INVALID_ARGUMENT, "size").mustBe(integerValue().or(doubleValue())).asIntegerVector().findFirst().mustNotBeNA(INVALID_ARGUMENT,
                        "size").mustBe(gte0(), INVALID_ARGUMENT, "size");
    }

    @Specialization(guards = "x > MAX_INT")
    protected RDoubleVector doLargeX(double x, int size) {
        validate(x, size);
        RRNG.getRNGState();

        double[] result = new double[size];
        NonRecursiveHashSetDouble used = new NonRecursiveHashSetDouble((int) (size * 1.2));
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < 100; j++) {
                double value = Math.floor(unifIndex(x) + 1);
                if (!used.add(value)) {
                    result[i] = value;
                    break;
                }
            }
        }

        RRNG.putRNGState();
        return RDataFactory.createDoubleVector(result, true);
    }

    @Specialization(guards = "x <= MAX_INT")
    protected RIntVector doSmallX(double x, int size) {
        validate(x, size);
        RRNG.getRNGState();

        int[] result = new int[size];
        NonRecursiveHashSetInt used = new NonRecursiveHashSetInt((int) (size * 1.2));
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < 100; j++) {
                int value = (int) (unifIndex(x) + 1);
                if (!used.add(value)) {
                    result[i] = value;
                    break;
                }
            }
        }

        RRNG.putRNGState();
        return RDataFactory.createIntVector(result, true);
    }

    private void validate(double x, int size) {
        if (size > x / 2) {
            throw error(ALGORITHM_FOR_SIZE_N_DIV_2);
        }
    }

    private static double ru() {
        return (Math.floor(U * RRNG.unifRand()) + RRNG.unifRand()) / U;
    }

    private static double unifIndex0(double dn) {
        double cut = MAX_INT;

        switch (RRNG.currentKind()) {
            case KNUTH_TAOCP:
            case USER_UNIF:
            case KNUTH_TAOCP2:
                cut = 33554431.0; /* 2^25 - 1 */
                break;
            default:
                break;
        }

        double u = dn > cut ? ru() : RRNG.unifRand();
        return Math.floor(dn * u);
    }

    // generate a random non-negative integer < 2 ^ bits in 16 bit chunks
    private static double rbits(int bits) {
        int v = 0;
        for (int n = 0; n <= bits; n += 16) {
            int v1 = (int) Math.floor(RRNG.unifRand() * 65536);
            v = 65536 * v + v1;
        }
        // mask out the bits in the result that are not needed
        // return (double) (v & ((one64 << bits) - 1));
        long one64 = 1;
        return v & ((one64 << bits) - 1);
    }

    private static double unifIndex(double dn) {
        if (RRNG.currentSampleKind() == ROUNDING) {
            return unifIndex0(dn);
        }

        // rejection sampling from integers below the next larger power of two
        if (dn <= 0) {
            return 0.0;
        }
        int bits = (int) Math.ceil(log2(dn));
        double dv;
        do {
            dv = rbits(bits);
        } while (dn <= dv);
        return dv;
    }

    public static double log2(double x) {
        return Math.log(x) / Math.log(2.0);
    }

}
