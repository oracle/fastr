/*
 * Copyright (c) 1995, 1996  Robert Gentleman and Ross Ihaka
 * Copyright (c) 1997-2014,  The R Core Team
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates
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
package com.oracle.truffle.r.runtime;

import com.oracle.truffle.r.runtime.nodes.RBaseNode;

/**
 * Constructs m "pretty" values which cover the given interval. This code is used in both built-in
 * {@code pretty} and the grid package.
 */
public final class PrettyIntevals {
    private PrettyIntevals() {
        // only static members
    }

    // transcribed from pretty.c

    public static double pretty(RBaseNode errorCtx, double[] lo, double[] up, int[] ndiv, int minN,
                    double shrinkSml, double highUFact0, double highUFact1,
                    int epsCorrection, boolean returnBounds) {
        /*
         * From version 0.65 on, we had rounding_eps := 1e-5, before, r..eps = 0 1e-7 is consistent
         * with seq.default()
         */
        double roundingEps = 1e-7;

        double h = highUFact0;
        double h5 = highUFact1;

        double dx;
        double cell;
        double unit;
        double base;
        double uu;
        double ns;
        double nu;
        int k;
        boolean iSmall;

        dx = up[0] - lo[0];
        /* cell := "scale" here */
        if (dx == 0 && up[0] == 0) { /* up == lo == 0 */
            cell = 1;
            iSmall = true;
        } else {
            cell = Math.max(Math.abs(lo[0]), Math.abs(up[0]));
            /* uu = upper bound on cell/unit */
            // uu = (1 + (h5 >= 1.5 * h + .5)) ? 1 / (1 + h) : 1.5 / (1 + h5);
            // How can above expression ever be zero?
            uu = 1 / (1 + h);
            /* added times 3, as several calculations here */
            iSmall = dx < cell * uu * Math.max(1, ndiv[0]) * RRuntime.EPSILON * 3;
        }

        /* OLD: cell = FLT_EPSILON+ dx / ndiv[0]; FLT_EPSILON = 1.192e-07 */
        if (iSmall) {
            if (cell > 10) {
                cell = 9 + cell / 10;
            }
            cell *= shrinkSml;
            if (minN > 1) {
                cell /= minN;
            }
        } else {
            cell = dx;
            if (ndiv[0] > 1) {
                cell /= ndiv[0];
            }
        }

        if (cell < 20 * Double.MIN_VALUE) {
            RError.warning(errorCtx, RError.Message.GENERIC, "Internal(pretty()): very small range.. corrected");
            cell = 20 * Double.MIN_VALUE;
        } else if (cell * 10 > Double.MAX_VALUE) {
            RError.warning(errorCtx, RError.Message.GENERIC, "Internal(pretty()): very large range.. corrected");
            cell = .1 * Double.MAX_VALUE;
        }
        /*
         * NB: the power can be negative and this relies on exact calculation, which glibc's exp10
         * does not achieve
         */
        base = Math.pow(10.0, Math.floor(Math.log10(cell))); /* base <= cell < 10*base */

        /*
         * unit : from { 1,2,5,10 } * base such that |u - cell| is small, favoring larger (if h > 1,
         * else smaller) u values; favor '5' more than '2' if h5 > h (default h5 = .5 + 1.5 h)
         */
        unit = base;
        if ((uu = 2 * base) - cell < h * (cell - unit)) {
            unit = uu;
            if ((uu = 5 * base) - cell < h5 * (cell - unit)) {
                unit = uu;
                if ((uu = 10 * base) - cell < h * (cell - unit)) {
                    unit = uu;
                }
            }
        }
        /*
         * Result: c := cell, u := unit, b := base c in [ 1, (2+ h) /(1+h) ] b ==> u= b c in ( (2+
         * h)/(1+h), (5+2h5)/(1+h5)] b ==> u= 2b c in ( (5+2h)/(1+h), (10+5h) /(1+h) ] b ==> u= 5b c
         * in ((10+5h)/(1+h), 10 ) b ==> u=10b
         *
         * ===> 2/5 *(2+h)/(1+h) <= c/u <= (2+h)/(1+h)
         */

        ns = Math.floor(lo[0] / unit + roundingEps);
        nu = Math.ceil(up[0] / unit - roundingEps);
        if (epsCorrection != 0 && (epsCorrection > 1 || !iSmall)) {
            if (lo[0] != 0.) {
                lo[0] *= (1 - RRuntime.EPSILON);
            } else {
                lo[0] = -Double.MIN_VALUE;
            }
            if (up[0] != 0.) {
                up[0] *= (1 + RRuntime.EPSILON);
            } else {
                up[0] = +Double.MIN_VALUE;
            }
        }

        while (ns * unit > lo[0] + roundingEps * unit) {
            ns--;
        }

        while (nu * unit < up[0] - roundingEps * unit) {
            nu++;
        }

        k = (int) (0.5 + nu - ns);
        if (k < minN) {
            /* ensure that nu - ns == min_n */
            k = minN - k;
            if (ns >= 0.) {
                nu += k / 2;
                ns -= k / 2 + k % 2; /* ==> nu-ns = old(nu-ns) + min_n -k = min_n */
            } else {
                ns -= k / 2;
                nu += k / 2 + k % 2;
            }
            ndiv[0] = minN;
        } else {
            ndiv[0] = k;
        }
        if (returnBounds) { /* if()'s to ensure that result covers original range */
            if (ns * unit < lo[0]) {
                lo[0] = ns * unit;
            }
            if (nu * unit > up[0]) {
                up[0] = nu * unit;
            }
        } else {
            lo[0] = ns;
            up[0] = nu;
        }
        return unit;
    }
}
