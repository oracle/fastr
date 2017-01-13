/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (C) 1998 Ross Ihaka
 * Copyright (c) 2000-2016, The R Core Team
 * Copyright (c) 2005-2016, The R Foundation
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.runtime.nmath.distr;

import static com.oracle.truffle.r.runtime.nmath.MathConstants.DBL_EPSILON;

import com.oracle.truffle.r.runtime.nmath.DPQ;
import com.oracle.truffle.r.runtime.nmath.DPQ.EarlyReturn;
import com.oracle.truffle.r.runtime.nmath.MathFunctions.Function3_2;
import com.oracle.truffle.r.runtime.nmath.RMath;
import com.oracle.truffle.r.runtime.nmath.RMathError;
import com.oracle.truffle.r.runtime.nmath.distr.PNBinom.PNBinomFunc;

public final class QNBinom {
    private QNBinom() {
        // only static members
    }

    public static final class QNBinomFunc implements Function3_2 {
        private final Qnorm qnorm = new Qnorm();
        private final PNBinomFunc pnbinom = new PNBinomFunc();

        @Override
        public double evaluate(double pIn, double size, double prob, boolean lowerTail, boolean logP) {
            if (Double.isNaN(pIn) || Double.isNaN(size) || Double.isNaN(prob)) {
                return pIn + size + prob;
            }

            /*
             * this happens if specified via mu, size, since prob == size/(size+mu)
             */
            if (prob == 0 && size == 0) {
                return 0;
            }

            if (prob <= 0 || prob > 1 || size < 0) {
                return RMathError.defaultError();
            }

            if (prob == 1 || size == 0) {
                return 0;
            }

            try {
                DPQ.rqp01boundaries(pIn, 0, Double.POSITIVE_INFINITY, lowerTail, logP);
            } catch (EarlyReturn e) {
                return e.result;
            }

            double capQ = 1.0 / prob;
            double capP = (1.0 - prob) * capQ;
            double mu = size * capP;
            double sigma = Math.sqrt(size * capP * capQ);
            double gamma = (capQ + capP) / sigma;

            /*
             * Note : "same" code in qpois.c, qbinom.c, qnbinom.c -- GnuR fix me: This is far from
             * optimal [cancellation for p ~= 1, etc]:
             */
            double p = pIn;
            if (!lowerTail || logP) {
                p = DPQ.rdtqiv(p, lowerTail, logP); /* need check again (cancellation!): */
                if (p == DPQ.rdt0(lowerTail, logP)) {
                    return 0;
                }
                if (p == DPQ.rdt1(lowerTail, logP)) {
                    return Double.POSITIVE_INFINITY;
                }
            }
            /* GnuR fix me: temporary hack */
            if (p + 1.01 * DBL_EPSILON >= 1.) {
                return Double.POSITIVE_INFINITY;
            }

            /* y := approx.value (Cornish-Fisher expansion) : */
            double qnormZ = qnorm.evaluate(p, 0., 1., /* lowerTail */true, /* logP */false);
            double y = RMath.forceint(mu + sigma * (qnormZ + gamma * (qnormZ * qnormZ - 1) / 6));

            /* fuzz to ensure left continuity: */
            p *= 1 - 64 * DBL_EPSILON;

            QuantileSearch search = new QuantileSearch((q, lt, lp) -> pnbinom.evaluate(q, size, prob, lt, lp));

            if (y < 1e5) {
                /* If the C-F value is not too large a simple search is OK */
                return search.simpleSearch(y, p, 1);
            } else {
                /* Otherwise be a bit cleverer in the search */
                return search.iterativeSearch(y, p);
            }
        }
    }

    public static final class QNBinomMu implements Function3_2 {
        private final QPois qpois = new QPois();
        private final QNBinomFunc qnbinom = new QNBinomFunc();

        @Override
        public double evaluate(double p, double size, double mu, boolean lowerTail, boolean logP) {
            if (size == Double.POSITIVE_INFINITY) {
                // limit case: poisson
                return qpois.evaluate(p, mu, lowerTail, logP);
            }
            // GnuR fix me: implement this properly not losing acuracy for large size (prob ~= 1)
            return qnbinom.evaluate(p, size, size / (size + mu), lowerTail, logP);
        }
    }
}
