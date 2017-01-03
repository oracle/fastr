/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (C) 1998 Ross Ihaka
 * Copyright (c) 2000-2006, The R Core Team
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.runtime.nmath.distr;

import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
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

    public static final class Runif extends RandFunction2_Double {
        private final BranchProfile errorProfile = BranchProfile.create();
        private final ConditionProfile minEqualsMaxProfile = ConditionProfile.createBinaryProfile();
        private final ValueProfile minValueProfile = ValueProfile.createEqualityProfile();
        private final ValueProfile maxValueProfile = ValueProfile.createEqualityProfile();

        @Override
        public double execute(double minIn, double maxIn, RandomNumberProvider rand) {
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
    }

    public static final class PUnif implements Function3_2 {
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
