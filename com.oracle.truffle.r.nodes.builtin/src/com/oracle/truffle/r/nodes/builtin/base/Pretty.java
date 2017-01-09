/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995, 1996  Robert Gentleman and Ross Ihaka
 * Copyright (c) 1997-2014,  The R Core Team
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.and;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.gt;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.gte0;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.isFinite;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.lte;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.size;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;

@RBuiltin(name = "pretty", kind = INTERNAL, parameterNames = {"l", "u", "n", "min.n", "shrink.sml", "hi", "eps.correct"}, behavior = PURE)
public abstract class Pretty extends RBuiltinNode {

    private static final RStringVector NAMES = RDataFactory.createStringVector(new String[]{"l", "u", "n"}, RDataFactory.COMPLETE_VECTOR);

    @Override
    public void createCasts(CastBuilder casts) {
        casts.arg("l").asDoubleVector().findFirst().mustBe(isFinite());
        casts.arg("u").asDoubleVector().findFirst().mustBe(isFinite());
        casts.arg("n").asIntegerVector().findFirst().notNA().mustBe(gte0());
        casts.arg("min.n").asIntegerVector().findFirst().notNA().mustBe(gte0());
        casts.arg("shrink.sml").asDoubleVector().findFirst().mustBe(and(isFinite(), gt(0.0)));
        casts.arg("hi").asDoubleVector().mustBe(size(2));
        casts.arg("eps.correct").defaultError(RError.Message.GENERIC, "'eps.correct' must be 0, 1, or 2").asIntegerVector().findFirst().notNA().mustBe(and(gte0(), lte(2)));
    }

    @Specialization
    protected RList pretty(double l, double u, int n, int minN, double shrinkSml, RAbstractDoubleVector hi, int epsCorrect) {
        double hi0 = hi.getDataAt(0);
        double hi1 = hi.getDataAt(1);

        if (!RRuntime.isFinite(hi0) || hi0 < 0.0) {
            throw RError.error(this, RError.Message.INVALID_ARGUMENT, "high.u.bias");
        }
        if (!RRuntime.isFinite(hi1) || hi1 < 0.0) {
            throw RError.error(this, RError.Message.INVALID_ARGUMENT, "u5.bias");
        }
        double[] lo = new double[]{l};
        double[] up = new double[]{u};
        int[] ndiv = new int[]{n};
        rPretty(lo, up, ndiv, minN, shrinkSml, hi.materialize().getDataWithoutCopying(), epsCorrect, true);
        Object[] data = new Object[3];
        data[0] = lo[0];
        data[1] = up[0];
        data[2] = ndiv[0];
        return RDataFactory.createList(data, NAMES);
    }

    double rPretty(double[] lo, double[] up, int[] ndiv, int minN,
                    double shrinkSml, double[] highUFact,
                    int epsCorrection, boolean returnBounds) {
        /*
         * From version 0.65 on, we had rounding_eps := 1e-5, before, r..eps = 0 1e-7 is consistent
         * with seq.default()
         */
        double roundingEps = 1e-7;

        double h = highUFact[0];
        double h5 = highUFact[1];

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
            RError.warning(this, RError.Message.GENERIC, "Internal(pretty()): very small range.. corrected");
            cell = 20 * Double.MIN_VALUE;
        } else if (cell * 10 > Double.MAX_VALUE) {
            RError.warning(this, RError.Message.GENERIC, "Internal(pretty()): very large range.. corrected");
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
