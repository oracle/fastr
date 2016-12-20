/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (C) 1998 Ross Ihaka
 * Copyright (c) 1998--2008, The R Core Team
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.library.stats;

import static com.oracle.truffle.r.library.stats.MathConstants.M_PI;
import static com.oracle.truffle.r.library.stats.TOMS708.fabs;

import com.oracle.truffle.r.library.stats.DPQ.EarlyReturn;
import com.oracle.truffle.r.library.stats.RandGenerationFunctions.RandFunction2_Double;
import com.oracle.truffle.r.library.stats.RandGenerationFunctions.RandomNumberProvider;
import com.oracle.truffle.r.library.stats.StatsFunctions.Function3_1;
import com.oracle.truffle.r.library.stats.StatsFunctions.Function3_2;

public final class Cauchy {
    private Cauchy() {
        // contains only static classes
    }

    public static final class RCauchy extends RandFunction2_Double {
        @Override
        public double execute(double location, double scale, RandomNumberProvider rand) {
            if (Double.isNaN(location) || !Double.isFinite(scale) || scale < 0) {
                return RMath.mlError();
            }
            if (scale == 0. || !Double.isFinite(location)) {
                return location;
            } else {
                return location + scale * Math.tan(M_PI * rand.unifRand());
            }
        }
    }

    public static final class DCauchy implements Function3_1 {
        @Override
        public double evaluate(double x, double location, double scale, boolean giveLog) {
            double y;
            /* NaNs propagated correctly */
            if (Double.isNaN(x) || Double.isNaN(location) || Double.isNaN(scale)) {
                return x + location + scale;
            }
            if (scale <= 0) {
                return RMath.mlError();
            }

            y = (x - location) / scale;
            return giveLog ? -Math.log(M_PI * scale * (1. + y * y)) : 1. / (M_PI * scale * (1. + y * y));
        }
    }

    public static final class PCauchy implements Function3_2 {
        @Override
        public double evaluate(double x, double location, double scale, boolean lowerTail, boolean logP) {
            if (Double.isNaN(x) || Double.isNaN(location) || Double.isNaN(scale)) {
                return x + location + scale;
            }

            if (scale <= 0) {
                return RMath.mlError();
            }

            x = (x - location) / scale;
            if (Double.isNaN(x)) {
                return RMath.mlError();
            }

            if (!Double.isFinite(x)) {
                if (x < 0) {
                    return DPQ.rdt0(lowerTail, logP);
                } else {
                    return DPQ.rdt1(lowerTail, logP);
                }
            }

            if (!lowerTail) {
                x = -x;
            }

            /*
             * for large x, the standard formula suffers from cancellation. This is from Morten
             * Welinder thanks to Ian Smith's atan(1/x) :
             */

            // GnuR has #ifdef HAVE_ATANPI where it uses atanpi function, here we only implement the
            // case when atanpi is not available for the moment
            if (fabs(x) > 1) {
                double y = Math.atan(1 / x) / M_PI;
                return (x > 0) ? DPQ.rdclog(y, logP) : DPQ.rdval(-y, logP);
            } else {
                return DPQ.rdval(0.5 + Math.atan(x) / M_PI, logP);
            }
        }
    }

    public static final class QCauchy implements Function3_2 {
        @Override
        public double evaluate(double p, double location, double scale, boolean lowerTail, boolean logP) {
            if (Double.isNaN(p) || Double.isNaN(location) || Double.isNaN(scale)) {
                return p + location + scale;
            }
            try {
                DPQ.rqp01check(p, logP);
            } catch (EarlyReturn e) {
                return e.result;
            }
            if (scale <= 0 || !Double.isFinite(scale)) {
                if (scale == 0) {
                    return location;
                }
                return RMath.mlError();
            }

            if (logP) {
                if (p > -1) {
                    /*
                     * when ep := Math.exp(p), tan(pi*ep)= -tan(pi*(-ep))= -tan(pi*(-ep)+pi) =
                     * -tan(pi*(1-ep)) = = -tan(pi*(-Math.expm1(p)) for p ~ 0, Math.exp(p) ~ 1,
                     * tan(~0) may be better than tan(~pi).
                     */
                    if (p == 0.) {
                        /* needed, since 1/tan(-0) = -Inf for some arch. */
                        return location + (lowerTail ? scale : -scale) * Double.POSITIVE_INFINITY;
                    }
                    lowerTail = !lowerTail;
                    p = -Math.expm1(p);
                } else {
                    p = Math.exp(p);
                }
            } else {
                if (p > 0.5) {
                    if (p == 1.) {
                        return location + (lowerTail ? scale : -scale) * Double.POSITIVE_INFINITY;
                    }
                    p = 1 - p;
                    lowerTail = !lowerTail;
                }
            }

            if (p == 0.5) {
                return location;
            } // avoid 1/Inf below
            if (p == 0.) {
                return location + (lowerTail ? scale : -scale) * Double.NEGATIVE_INFINITY;
            } // p = 1. is handled above
            return location + (lowerTail ? -scale : scale) / RMath.tanpi(p);
            /* -1/tan(pi * p) = -cot(pi * p) = tan(pi * (p - 1/2)) */
        }
    }
}
