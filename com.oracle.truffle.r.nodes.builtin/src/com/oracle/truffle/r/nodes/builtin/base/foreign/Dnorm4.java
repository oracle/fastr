/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */

package com.oracle.truffle.r.nodes.builtin.base.foreign;

import com.oracle.truffle.r.library.stats.StatsFunctions.Function3_1;

/**
 * Called from 'dnorm' R wrapper.
 */
public final class Dnorm4 implements Function3_1 {

    private static final double M_LN_SQRT_2PI = 0.918938533204672741780329736406;
    private static final double M_1_SQRT_2PI = 0.398942280401432677939946059934;

    @Override
    public double evaluate(double xa, double mu, double sigma, boolean giveLog) {
        double x = xa;
        if (Double.isNaN(x) || Double.isNaN(mu) || Double.isNaN(sigma)) {
            return x + mu + sigma;
        }

        double negInfOrZero = giveLog ? Double.NEGATIVE_INFINITY : 0; // in GnuR R_D__0
        if (!Double.isFinite(sigma)) {
            return negInfOrZero;
        } else if (!Double.isFinite(x) && !Double.isFinite(mu)) {
            return Double.NaN;
        } else if (sigma <= 0) {
            if (sigma < 0) {
                // TODO: ML_ERR_return_NAN (what is it supposed to do? GnuR does not print anything)
                return Double.NaN;
            }
            return (x == mu) ? Double.POSITIVE_INFINITY : negInfOrZero;
        }

        x = (x - mu) / sigma;
        if (!Double.isFinite(x)) {
            return negInfOrZero;
        }

        if (giveLog) {
            return -(M_LN_SQRT_2PI + 0.5 * x * x + Math.log10(sigma));
        }
        return M_1_SQRT_2PI * Math.exp(-0.5 * x * x) / sigma;
    }
}
