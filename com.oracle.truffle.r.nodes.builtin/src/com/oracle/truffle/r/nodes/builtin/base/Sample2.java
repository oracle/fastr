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

/**
 * Sample2 is more efficient special case implementation of {@link Sample}.
 */
@RBuiltin(name = "sample2", kind = INTERNAL, parameterNames = {"x", "size"}, behavior = MODIFIES_STATE)
public abstract class Sample2 extends RBuiltinNode.Arg2 {
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
                double value = Math.floor(RRNG.unifIndex(x) + 1);
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
                int value = (int) (RRNG.unifIndex(x) + 1);
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

}
