/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995, 1996  Robert Gentleman and Ross Ihaka
 * Copyright (c) 1997-2015,  The R Core Team
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */

package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.doubleValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.gte;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.gte0;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.integerValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.isFinite;
import static com.oracle.truffle.r.runtime.RError.SHOW_CALLER;
import static com.oracle.truffle.r.runtime.RError.Message.ALGORITHM_FOR_SIZE_N_DIV_2;
import static com.oracle.truffle.r.runtime.RError.Message.INVALID_ARGUMENT;
import static com.oracle.truffle.r.runtime.RError.Message.INVALID_FIRST_ARGUMENT;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.MODIFIES_STATE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.Collections.NonRecursiveHashSetDouble;
import com.oracle.truffle.r.runtime.Collections.NonRecursiveHashSetInt;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.rng.RRNG;

/**
 * Sample2 is more efficient special case implementation of {@link Sample}.
 */
@RBuiltin(name = "sample2", kind = INTERNAL, parameterNames = {"x", "size"}, behavior = MODIFIES_STATE)
public abstract class Sample2 extends RBuiltinNode {
    private static final double U = 33554432.0;
    static final double MAX_INT = Integer.MAX_VALUE;

    private final BranchProfile errorProfile = BranchProfile.create();

    static {
        Casts casts = new Casts(Sample2.class);
        casts.arg("x").defaultError(SHOW_CALLER, INVALID_FIRST_ARGUMENT).allowNull().mustBe(integerValue().or(doubleValue())).notNA(SHOW_CALLER,
                        INVALID_FIRST_ARGUMENT).asDoubleVector().findFirst().mustBe(gte(0.0)).mustBe(isFinite());
        casts.arg("size").defaultError(SHOW_CALLER, INVALID_ARGUMENT, "size").mustBe(integerValue().or(doubleValue())).asIntegerVector().findFirst().defaultError(SHOW_CALLER, INVALID_ARGUMENT,
                        "size").notNA().mustBe(gte0());
    }

    @Specialization(guards = "x > MAX_INT")
    protected RDoubleVector doLargeX(double x, int size) {
        validate(x, size);
        RRNG.getRNGState();

        double[] result = new double[size];
        NonRecursiveHashSetDouble used = new NonRecursiveHashSetDouble((int) (size * 1.2));
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < 100; j++) {
                double value = Math.floor(x * ru() + 1);
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
                int value = (int) (x * RRNG.unifRand() + 1);
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
            errorProfile.enter();
            throw RError.error(SHOW_CALLER, ALGORITHM_FOR_SIZE_N_DIV_2);
        }
    }

    private static double ru() {
        return (Math.floor(U * RRNG.unifRand()) + RRNG.unifRand()) / U;
    }
}
