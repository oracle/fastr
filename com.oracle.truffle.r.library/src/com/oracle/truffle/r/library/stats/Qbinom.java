/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (C) 1998 Ross Ihaka
 * Copyright (c) 2000--2009, The R Core Team
 * Copyright (c) 2003--2009, The R Foundation
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.library.stats;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.ops.na.NAProfile;

public abstract class Qbinom extends RExternalBuiltinNode.Arg5 {

    private static final class Search {
        private double z;

        Search(double z) {
            this.z = z;
        }

        double doSearch(double initialY, double p, double n, double pr, double incr) {
            double y = initialY;
            if (z >= p) {
                /* search to the left */
                for (;;) {
                    double newz;
                    if (y == 0 || (newz = Pbinom.pbinom(y - incr, n, pr, /* l._t. */true, /* logP */false)) < p) {
                        return y;
                    }
                    y = Math.max(0, y - incr);
                    z = newz;
                }
            } else { /* search to the right */
                for (;;) {
                    y = Math.min(y + incr, n);
                    if (y == n || (z = Pbinom.pbinom(y, n, pr, /* l._t. */true, /* logP */false)) >= p) {
                        return y;
                    }
                }
            }
        }
    }

    @TruffleBoundary
    private static double qbinom(double initialP, double n, double pr, boolean lowerTail, boolean logP) {
        double p = initialP;

        if (Double.isNaN(p) || Double.isNaN(n) || Double.isNaN(pr)) {
            return p + n + pr;
        }

        if (!Double.isFinite(n) || !Double.isFinite(pr)) {
            return Double.NaN;
        }
        /* if logP is true, p = -Inf is a legitimate value */
        if (!Double.isFinite(p) && !logP) {
            return Double.NaN;
        }

        if (n != Math.floor(n + 0.5)) {
            return Double.NaN;
        }
        if (pr < 0 || pr > 1 || n < 0) {
            return Double.NaN;
        }

        if (logP) {
            if (p > 0) {
                return Double.NaN;
            }
            if (p == 0) {
                return lowerTail ? n : (double) 0;
            }
            if (p == Double.NEGATIVE_INFINITY) {
                return lowerTail ? (double) 0 : n;
            }
        } else { /* !logP */
            if (p < 0 || p > 1) {
                return Double.NaN;
            }
            if (p == 0) {
                return lowerTail ? (double) 0 : n;
            }
            if (p == 1) {
                return lowerTail ? n : (double) 0;
            }
        }

        if (pr == 0. || n == 0) {
            return 0.;
        }

        double q = 1 - pr;
        if (q == 0.) {
            /* covers the full range of the distribution */
            return n;
        }
        double mu = n * pr;
        double sigma = Math.sqrt(n * pr * q);
        double gamma = (q - pr) / sigma;

        /*
         * Note : "same" code in qpois.c, qbinom.c, qnbinom.c -- FIXME: This is far from optimal
         * [cancellation for p ~= 1, etc]:
         */
        if (!lowerTail || logP) {
            p = DPQ.dtQIv(logP, lowerTail, p); /* need check again (cancellation!): */
            if (p == 0.) {
                return 0.;
            }
            if (p == 1.) {
                return n;
            }
        }
        /* temporary hack --- FIXME --- */
        if (p + 1.01 * RRuntime.EPSILON >= 1.) {
            return n;
        }

        /* y := approx.value (Cornish-Fisher expansion) : */

        double z = Random2.qnorm5(p, 0., 1., /* lowerTail */true, /* logP */false);
        double y = Math.floor(mu + sigma * (z + gamma * (z * z - 1) / 6) + 0.5);

        if (y > n) {
            /* way off */
            y = n;
        }

        z = Pbinom.pbinom(y, n, pr, /* lowerTail */true, /* logP */false);

        /* fuzz to ensure left continuity: */
        p *= 1 - 64 * RRuntime.EPSILON;

        Search search = new Search(z);

        if (n < 1e5) {
            return search.doSearch(y, p, n, pr, 1);
        }
        /* Otherwise be a bit cleverer in the search */
        double incr = Math.floor(n * 0.001);
        double oldincr;
        do {
            oldincr = incr;
            y = search.doSearch(y, p, n, pr, incr);
            incr = Math.max(1, Math.floor(incr / 100));
        } while (oldincr > 1 && incr > n * 1e-15);
        return y;
    }

    @Override
    protected void createCasts(CastBuilder casts) {
        casts.toDouble(0).toDouble(1).toDouble(2).firstBoolean(3).firstBoolean(4);
    }

    @Specialization
    protected Object qbinom(RAbstractDoubleVector p, RAbstractDoubleVector n, RAbstractDoubleVector pr, boolean lowerTail, boolean logP, //
                    @Cached("create()") NAProfile na) {
        int pLength = p.getLength();
        int nLength = n.getLength();
        int prLength = pr.getLength();
        if (pLength == 0 || nLength == 0 || prLength == 0) {
            return RDataFactory.createEmptyDoubleVector();
        }
        int length = Math.max(pLength, Math.max(nLength, prLength));
        double[] result = new double[length];

        boolean complete = true;
        boolean nans = false;
        for (int i = 0; i < length; i++) {
            double value = qbinom(p.getDataAt(i % pLength), n.getDataAt(i % nLength), pr.getDataAt(i % prLength), lowerTail, logP);
            if (na.isNA(value)) {
                complete = false;
            } else if (Double.isNaN(value)) {
                nans = true;
            }
            result[i] = value;
        }
        if (nans) {
            RError.warning(RError.SHOW_CALLER, RError.Message.NAN_PRODUCED);
        }
        return RDataFactory.createDoubleVector(result, complete);
    }
}
