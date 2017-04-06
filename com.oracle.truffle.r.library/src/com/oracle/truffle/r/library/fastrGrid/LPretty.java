/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (C) 2001-3 Paul Murrell
 * Copyright (c) 1998-2013, The R Core Team
 * Copyright (c) 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.library.fastrGrid;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.numericValue;
import static com.oracle.truffle.r.runtime.nmath.TOMS708.fabs;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.PrettyIntevals;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;

public abstract class LPretty extends RExternalBuiltinNode.Arg1 {
    static {
        Casts casts = new Casts(LPretty.class);
        casts.arg(0).mustBe(numericValue()).asDoubleVector();
    }

    public static LPretty create() {
        return LPrettyNodeGen.create();
    }

    @Specialization
    protected Object doPretty(RAbstractDoubleVector scale) {
        double min = scale.getLength() > 0 ? scale.getDataAt(0) : 0;
        double max = scale.getLength() > 1 ? scale.getDataAt(1) : 0;
        boolean swap = max < min;
        if (swap) {
            min = max;
            max = scale.getDataAt(0);
        }

        if (Double.isInfinite(min) || Double.isInfinite(max)) {
            throw error(Message.GENERIC, String.format("infinite axis extents [GEPretty(%g,%g,5)]", min, max));
        }

        double[] ns = new double[]{min};
        double[] nu = new double[]{max};
        int[] ndiv = new int[]{5};
        double unit = PrettyIntevals.pretty(getErrorContext(), ns, nu, new int[]{5}, 1, 0.25, new double[]{.8, 1.7}, 2, false);

        if (nu[0] >= ns[0] + 1) {
            if (ns[0] * unit < min - 1e-7 * unit) {
                ns[0]++;
            }
            if (nu[0] > ns[0] + 1 && nu[0] * unit > max + 1e-7 * unit) {
                nu[0]--;
            }
            ndiv[0] = (int) (nu[0] - ns[0]);
        }
        min = ns[0] * unit;
        max = nu[0] * unit;

        if (swap) {
            double temp = min;
            min = max;
            max = temp;
        }
        return createAtVector(min, max, ndiv[0]);
    }

    private static RAbstractDoubleVector createAtVector(double axp0, double axp1, double axp2) {
        /*
         * Create an 'at = ...' vector for axis(.) i.e., the vector of tick mark locations, when
         * none has been specified (= default).
         *
         * axp[0:2] = (x1, x2, nInt), where x1..x2 are the extreme tick marks {unless in log case,
         * where nInt \in {1,2,3 ; -1,-2,....} and the `nint' argument is used *instead*.} The
         * resulting REAL vector must have length >= 1, ideally >= 2
         *
         * FastR Notes: we only implement case where logflag == false.
         */
        /* --- linear axis --- Only use axp[] arg. */
        int n = (int) (fabs(axp2) + 0.25); /* >= 0 */
        int dn = Math.max(1, n);
        double rng = axp1 - axp0;
        double small = fabs(rng) / (100. * dn);
        double[] at = new double[n + 1];
        for (int i = 0; i <= n; i++) {
            at[i] = axp0 + ((double) i / dn) * rng;
            if (fabs(at[i]) < small) {
                at[i] = 0;
            }
        }
        return RDataFactory.createDoubleVector(at, RDataFactory.COMPLETE_VECTOR);
    }
}
