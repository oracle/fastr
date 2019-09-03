/*
 * Copyright (c) 1999--2014, The R Core Team
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

package com.oracle.truffle.r.runtime.nmath.distr;

import static com.oracle.truffle.r.runtime.RError.Message.CALLOC_COULD_NOT_ALLOCATE;
import static com.oracle.truffle.r.runtime.nmath.MathConstants.DBL_EPSILON;
import static com.oracle.truffle.r.runtime.nmath.MathConstants.M_LN2;
import static com.oracle.truffle.r.runtime.nmath.RMath.forceint;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.nmath.DPQ;
import com.oracle.truffle.r.runtime.nmath.DPQ.EarlyReturn;
import com.oracle.truffle.r.runtime.nmath.MathFunctions.Function2_1;
import com.oracle.truffle.r.runtime.nmath.MathFunctions.Function2_2;
import com.oracle.truffle.r.runtime.nmath.RMath;
import com.oracle.truffle.r.runtime.nmath.RMathError;
import com.oracle.truffle.r.runtime.nmath.RandomFunctions.RandFunction1_Double;
import com.oracle.truffle.r.runtime.nmath.RandomFunctions.RandomNumberProvider;
import com.oracle.truffle.r.runtime.nmath.TOMS708;
import com.oracle.truffle.r.runtime.nmath.distr.SignrankFactory.RSignrankNodeGen;

public final class Signrank {
    private Signrank() {
        // only static members
    }

    /**
     * Holds cache for the dynamic algorithm that signrank uses.
     */
    public static final class SignrankData {
        private static final ThreadLocal<double[]> data = new ThreadLocal<>();

        @TruffleBoundary
        static double[] getData(int n) {
            double[] result = data.get();
            int u = n * (n + 1) / 2;
            int c = (u / 2);

            if (result != null && (result.length <= c)) {
                result = null;
            }
            if (result == null) {
                try {
                    result = new double[c + 1];
                } catch (OutOfMemoryError e) {
                    // GnuR seems to be reporting the same number regardless of 'c'
                    throw couldNotAllocateError();
                }
                data.set(result);
            }
            return result;
        }

        public static void freeData() {
            data.set(null);
        }
    }

    private static RError couldNotAllocateError() {
        throw RError.error(RError.SHOW_CALLER, CALLOC_COULD_NOT_ALLOCATE, "18446744073172680704", 8);
    }

    private static double csignrank(double[] w, int kIn, int n) {
        int u = n * (n + 1) / 2;
        int c = (u / 2);

        if (kIn < 0 || kIn > u) {
            return 0;
        }
        int k = kIn <= c ? kIn : u - kIn;

        if (n == 1) {
            return 1.;
        }
        if (w[0] == 1.) {
            return w[k];
        }

        w[0] = w[1] = 1.;
        for (int j = 2; j < n + 1; ++j) {
            int end = Math.min(j * (j + 1) / 2, c);
            for (int i = end; i >= j; --i) {
                w[i] += w[i - j];
            }
        }
        return w[k];
    }

    public static final class DSignrank implements Function2_1 {

        public static DSignrank create() {
            return new DSignrank();
        }

        public static DSignrank getUncached() {
            return new DSignrank();
        }

        @Override
        public double evaluate(double xIn, double nIn, boolean giveLog) {
            /* NaNs propagated correctly */
            if (Double.isNaN(xIn) || Double.isNaN(nIn)) {
                return xIn + nIn;
            }

            if (nIn == Double.POSITIVE_INFINITY) {
                // to have the same output as GnuR:
                throw couldNotAllocateError();
            }

            double n = RMath.forceint(nIn);
            if (n <= 0) {
                return RMathError.defaultError();
            }

            double x = RMath.forceint(xIn);
            if (TOMS708.fabs(xIn - x) > 1e-7) {
                return DPQ.rd0(giveLog);
            }
            if ((x < 0) || (x > (n * (n + 1) / 2))) {
                return DPQ.rd0(giveLog);
            }

            int nn = (int) n;
            double[] data = SignrankData.getData(nn);
            return DPQ.rdexp(Math.log(csignrank(data, (int) x, nn)) - n * M_LN2, giveLog);
        }
    }

    public static final class PSignrank implements Function2_2 {

        public static PSignrank create() {
            return new PSignrank();
        }

        public static PSignrank getUncached() {
            return new PSignrank();
        }

        @Override
        public double evaluate(double xIn, double nIn, boolean lowerTail, boolean logP) {
            if (Double.isNaN(xIn) || Double.isNaN(nIn)) {
                return (xIn + nIn);
            }
            if (!Double.isFinite(nIn)) {
                return RMathError.defaultError();
            }

            double n = RMath.forceint(nIn);
            if (n <= 0) {
                return RMathError.defaultError();
            }

            double x = RMath.forceint(xIn + 1e-7);
            if (x < 0.0) {
                return DPQ.rdt0(lowerTail, logP);
            }
            if (x >= n * (n + 1) / 2) {
                return DPQ.rdt1(lowerTail, logP);
            }

            int nn = (int) n;
            double[] data = SignrankData.getData(nn);
            double f = Math.exp(-n * M_LN2);
            double p = 0;
            if (x <= (n * (n + 1) / 4)) {
                int xUpperLimit = getUpperIntBound(x);
                for (int i = 0; i <= xUpperLimit; i++) {
                    p += csignrank(data, i, nn) * f;
                }
                return DPQ.rdtval(p, lowerTail, logP);
            } else {
                x = n * (n + 1) / 2 - x;
                int xUpperLimit = getUpperIntBound(x);
                for (int i = 0; i < xUpperLimit; i++) {
                    p += csignrank(data, i, nn) * f;
                }
                // !lowerTail, because p = 1 - p; */
                return DPQ.rdtval(p, !lowerTail, logP);
            }
        }

        /**
         * Makes sure that the value can be reached by integer counter, this is probably just really
         * defensive, since the allocation with such number is bound to fail anyway.
         */
        private static int getUpperIntBound(double x) {
            return (x + 1) > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) Math.ceil(x);
        }
    }

    public static final class QSignrank implements Function2_2 {

        public static QSignrank create() {
            return new QSignrank();
        }

        public static QSignrank getUncached() {
            return new QSignrank();
        }

        @Override
        public double evaluate(double xIn, double nIn, boolean lowerTail, boolean logP) {
            if (Double.isNaN(xIn) || Double.isNaN(nIn)) {
                return (xIn + nIn);
            }

            if (!Double.isFinite(xIn) || !Double.isFinite(nIn)) {
                return RMathError.defaultError();
            }

            try {
                DPQ.rqp01check(xIn, logP);
            } catch (EarlyReturn earlyReturn) {
                return earlyReturn.result;
            }

            double n = RMath.forceint(nIn);
            if (n <= 0) {
                return RMathError.defaultError();
            }

            if (xIn == DPQ.rdt0(lowerTail, logP)) {
                return 0;
            }
            if (xIn == DPQ.rdt1(lowerTail, logP)) {
                return n * (n + 1) / 2;
            }

            double x = !logP && lowerTail ? xIn : DPQ.rdtqiv(xIn, lowerTail, logP);
            int nn = (int) n;
            double[] data = SignrankData.getData(nn);
            double f = Math.exp(-n * M_LN2);
            double p = 0;
            int q = 0;
            if (x <= 0.5) {
                x = x - 10 * DBL_EPSILON;
                while (true) {
                    p += csignrank(data, q, nn) * f;
                    if (p >= x) {
                        break;
                    }
                    q++;
                }
            } else {
                x = 1 - x + 10 * DBL_EPSILON;
                while (true) {
                    p += csignrank(data, q, nn) * f;
                    if (p > x) {
                        q = (int) (n * (n + 1) / 2 - q);
                        break;
                    }
                    q++;
                }
            }
            return q;
        }
    }

    @GenerateUncached
    public abstract static class RSignrank extends RandFunction1_Double {
        @Specialization
        public double exec(double nIn, RandomNumberProvider rand) {
            if (Double.isNaN(nIn)) {
                return nIn;
            }
            if (Double.isInfinite(nIn)) {
                // In GnuR these "results" seem to be generated due to the behaviour of R_forceint,
                // and the "(int) n" cast, which ends up casting +/-infinity to integer...
                return nIn < 0 ? RMathError.defaultError() : 0;
            }

            double n = forceint(nIn);
            if (n < 0) {
                return RMathError.defaultError();
            }

            if (n == 0) {
                return 0;
            }
            double r = 0.0;
            int k = (int) n;
            for (int i = 0; i < k; i++) {
                r += (i + 1) * Math.floor(rand.unifRand() + 0.5);
            }
            return r;
        }

        public static RSignrank create() {
            return RSignrankNodeGen.create();
        }

        public static RSignrank getUncached() {
            return RSignrankNodeGen.getUncached();
        }
    }
}
