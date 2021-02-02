/*
 * Copyright (C) 1998 Ross Ihaka
 * Copyright (c) 2000-2006, The R Core Team
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates
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
package com.oracle.truffle.r.runtime.nmath.distr;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.PrimitiveValueProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.nmath.DPQ;
import com.oracle.truffle.r.runtime.nmath.DPQ.EarlyReturn;
import com.oracle.truffle.r.runtime.nmath.MathFunctions.Function3_1;
import com.oracle.truffle.r.runtime.nmath.MathFunctions.Function3_2;
import com.oracle.truffle.r.runtime.nmath.RMathError;
import com.oracle.truffle.r.runtime.nmath.RandomFunctions.RandFunction2_Double;
import com.oracle.truffle.r.runtime.nmath.RandomFunctions.RandomNumberProvider;

public final class Unif {
    private Unif() {
        // only static members
    }

    @GenerateUncached
    public abstract static class Runif extends RandFunction2_Double {
        @Specialization
        public double exec(double minIn, double maxIn, RandomNumberProvider rand,
                        @Cached() BranchProfile errorProfile,
                        @Cached("createBinaryProfile()") ConditionProfile minEqualsMaxProfile,
                        @Cached("createEqualityProfile()") PrimitiveValueProfile minValueProfile,
                        @Cached("createEqualityProfile()") PrimitiveValueProfile maxValueProfile) {
            double min = minValueProfile.profile(minIn);
            double max = maxValueProfile.profile(maxIn);
            if (!RRuntime.isFinite(min) || !RRuntime.isFinite(max) || max < min) {
                errorProfile.enter();
                return RMathError.defaultError();
            }
            if (minEqualsMaxProfile.profile(min == max)) {
                return min;
            }
            return min + rand.unifRand() * (max - min);
        }

        public static Runif create() {
            return UnifFactory.RunifNodeGen.create();
        }

        public static Runif getUncached() {
            return UnifFactory.RunifNodeGen.getUncached();
        }
    }

    public static final class PUnif implements Function3_2 {

        public static PUnif create() {
            return new PUnif();
        }

        public static PUnif getUncached() {
            return new PUnif();
        }

        @Override
        public double evaluate(double x, double min, double max, boolean lowerTail, boolean logP) {
            if (Double.isNaN(x) || Double.isNaN(min) || Double.isNaN(max)) {
                return x + min + max;
            }
            if (max < min || !Double.isFinite(min) || !Double.isFinite(max)) {
                return RMathError.defaultError();
            }
            if (x >= max) {
                return DPQ.rdt1(lowerTail, logP);
            }
            if (x <= min) {
                return DPQ.rdt0(lowerTail, logP);
            }
            if (lowerTail) {
                return DPQ.rdval((x - min) / (max - min), logP);
            } else {
                return DPQ.rdval((max - x) / (max - min), logP);
            }
        }
    }

    public static final class DUnif implements Function3_1 {

        public static DUnif create() {
            return new DUnif();
        }

        public static DUnif getUncached() {
            return new DUnif();
        }

        @Override
        public double evaluate(double x, double min, double max, boolean giveLog) {
            if (Double.isNaN(x) || Double.isNaN(min) || Double.isNaN(max)) {
                return x + min + max;
            }
            if (max <= min) {
                return RMathError.defaultError();
            }
            if (min <= x && x <= max) {
                return giveLog ? -Math.log(max - min) : 1. / (max - min);
            }
            return DPQ.rd0(giveLog);
        }
    }

    public static final class QUnif implements Function3_2 {

        public static QUnif create() {
            return new QUnif();
        }

        public static QUnif getUncached() {
            return new QUnif();
        }

        @Override
        public double evaluate(double p, double min, double max, boolean lowerTail, boolean logP) {
            if (Double.isNaN(p) || Double.isNaN(min) || Double.isNaN(max)) {
                return p + min + max;
            }

            try {
                DPQ.rqp01check(p, logP);
            } catch (EarlyReturn e) {
                return e.result;
            }

            if (max < min || !Double.isFinite(min) || !Double.isFinite(max)) {
                return RMathError.defaultError();
            }

            if (max == min) {
                return min;
            }

            return min + DPQ.rdtqiv(p, lowerTail, logP) * (max - min);
        }
    }
}
