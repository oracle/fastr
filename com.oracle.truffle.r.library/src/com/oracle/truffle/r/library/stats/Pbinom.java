/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (C) 1998 Ross Ihaka
 * Copyright (c) 2000--2014, The R Core Team
 * Copyright (c) 2007, The R Foundation
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.library.stats;

import com.oracle.truffle.api.profiles.BranchProfile;

public final class Pbinom implements StatsFunctions.Function3_2 {

    private final BranchProfile nanProfile = BranchProfile.create();

    @Override
    public double evaluate(double initialQ, double initialSize, double prob, boolean lowerTail, boolean logP) {
        double q = initialQ;
        double size = initialSize;
        if (Double.isNaN(q) || Double.isNaN(size) || Double.isNaN(prob)) {
            nanProfile.enter();
            return q + size + prob;
        }
        if (!Double.isFinite(size) || !Double.isFinite(prob)) {
            nanProfile.enter();
            return Double.NaN;
        }

        if (DPQ.nonint(size)) {
            nanProfile.enter();
            DPQ.nointCheckWarning(size, "n");
            return DPQ.rd0(logP);
        }
        size = Math.round(size);
        /* PR#8560: n=0 is a valid value */
        if (size < 0 || prob < 0 || prob > 1) {
            nanProfile.enter();
            return Double.NaN;
        }

        if (q < 0) {
            return DPQ.rdt0(lowerTail, logP);
        }
        q = Math.floor(q + 1e-7);
        if (size <= q) {
            return DPQ.rdt1(lowerTail, logP);
        }
        return Pbeta.pbeta(prob, q + 1, size - q, !lowerTail, logP, nanProfile);
    }
}
