/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2000--2016, The R Core Team
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
/*
 *  AUTHOR
 *    Catherine Loader, catherine@research.bell-labs.com.
 *    October 23, 2000 and Feb, 2001.
 *
 *    dnbinom_mu(): Martin Maechler, June 2008
 */

package com.oracle.truffle.r.runtime.nmath.distr;

import static com.oracle.truffle.r.runtime.nmath.GammaFunctions.lgamma;
import static com.oracle.truffle.r.runtime.nmath.MathConstants.DBL_MAX;
import static com.oracle.truffle.r.runtime.nmath.distr.DPois.dpoisRaw;

import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.r.runtime.nmath.DPQ;
import com.oracle.truffle.r.runtime.nmath.DPQ.EarlyReturn;
import com.oracle.truffle.r.runtime.nmath.MathFunctions.Function3_1;
import com.oracle.truffle.r.runtime.nmath.RMath;
import com.oracle.truffle.r.runtime.nmath.RMathError;

public final class DNBinom {
    private DNBinom() {
        // only static members
    }

    public static final class DNBinomFunc implements Function3_1 {
        private final BranchProfile nanProfile = BranchProfile.create();

        @Override
        public double evaluate(double x, double sizeIn, double prob, boolean giveLog) {
            if (Double.isNaN(x) || Double.isNaN(sizeIn) || Double.isNaN(prob)) {
                nanProfile.enter();
                return x + sizeIn + prob;
            }
            if (prob <= 0 || prob > 1 || sizeIn < 0) {
                nanProfile.enter();
                return RMathError.defaultError();
            }

            try {
                DPQ.nonintCheck(x, giveLog);
            } catch (EarlyReturn e) {
                return e.result;
            }

            if (x < 0 || !Double.isFinite(x)) {
                return DPQ.rd0(giveLog);
            }
            /* limiting case as size approaches zero is point mass at zero */
            if (x == 0 && sizeIn == 0) {
                return DPQ.rd1(giveLog);
            }
            double ix = RMath.forceint(x);
            double size = Double.isFinite(sizeIn) ? sizeIn : DBL_MAX;
            double ans = Dbinom.dbinomRaw(size, ix + size, prob, 1 - prob, giveLog);
            double p = size / (size + ix);
            return giveLog ? Math.log(p) + ans : p * ans;
        }
    }

    public static final class DNBinomMu implements Function3_1 {
        @Override
        public double evaluate(double x, double size, double mu, boolean giveLog) {
            /*
             * originally, just set prob := size / (size + mu) and called dbinom_raw(), but that
             * suffers from cancellation when mu << size
             */
            if (Double.isNaN(x) || Double.isNaN(size) || Double.isNaN(mu)) {
                return x + size + mu;
            }

            if (mu < 0 || size < 0) {
                return RMathError.defaultError();
            }
            try {
                DPQ.nonintCheck(x, giveLog);
            } catch (EarlyReturn e) {
                return e.result;
            }

            if (x < 0 || !Double.isFinite(x)) {
                return DPQ.rd0(giveLog);
            }

            /*
             * limiting case as size approaches zero is point mass at zero, even if mu is kept
             * constant. limit distribution does not have mean mu, though.
             */
            if (x == 0 && size == 0) {
                return DPQ.rd1(giveLog);
            }
            x = RMath.forceint(x);
            if (!Double.isFinite(size)) {
                // limit case: Poisson
                return (dpoisRaw(x, mu, giveLog));
            }

            if (x == 0)/* be accurate, both for n << mu, and n >> mu : */ {
                double ans = size * (size < mu ? Math.log(size / (size + mu)) : RMath.log1p(-mu / (size + mu)));
                return DPQ.rdexp(ans, giveLog);
            }
            if (x < 1e-10 * size) { /* don't use dbinom_raw() but MM's formula: */
                /* GnuR fix me --- 1e-8 shows problem; rather use algdiv() from ./toms708.c */
                double p = (size < mu ? Math.log(size / (1 + size / mu)) : Math.log(mu / (1 + mu / size)));
                double ans = x * p - mu - lgamma(x + 1) + RMath.log1p(x * (x - 1) / (2 * size));
                return DPQ.rdexp(ans, giveLog);
            } else {
                /*
                 * no unnecessary cancellation inside dbinom_raw, when x_ = size and n_ = x+size are
                 * so close that n_ - x_ loses accuracy
                 */
                double p = size / (size + x);
                double ans = Dbinom.dbinomRaw(size, x + size, size / (size + mu), mu / (size + mu), giveLog);
                return ((giveLog) ? Math.log(p) + ans : p * ans);
            }
        }
    }
}
