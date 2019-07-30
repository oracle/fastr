/*
 * Copyright (C) 1998 Ross Ihaka
 * Copyright (c) 2000-2016, The R Core Team
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
package com.oracle.truffle.r.runtime.nmath.distr;

import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.nmath.DPQ;
import com.oracle.truffle.r.runtime.nmath.MathFunctions.Function3_2;
import com.oracle.truffle.r.runtime.nmath.RMathError;
import com.oracle.truffle.r.runtime.nmath.TOMS708.Bratio;

public final class PNBinom {
    private PNBinom() {
        // only static members
    }

    public static final class PNBinomFunc implements Function3_2 {

        public static PNBinomFunc create() {
            return new PNBinomFunc();
        }

        public static PNBinomFunc getUncached() {
            return new PNBinomFunc();
        }

        private final Pbeta pbeta = new Pbeta();

        @Override
        public double evaluate(double x, double size, double prob, boolean lowerTail, boolean logP) {
            if (Double.isNaN(x) || Double.isNaN(size) || Double.isNaN(prob)) {
                return x + size + prob;
            }
            if (!Double.isFinite(size) || !Double.isFinite(prob) || size < 0 || prob <= 0 || prob > 1) {
                return RMathError.defaultError();
            }

            /* limiting case: point mass at zero */
            if (size == 0) {
                return x >= 0 ? DPQ.rdt1(lowerTail, logP) : DPQ.rdt0(lowerTail, logP);
            }

            if (x < 0) {
                return DPQ.rdt0(lowerTail, logP);
            }
            if (!Double.isFinite(x)) {
                return DPQ.rdt1(lowerTail, logP);
            }
            double floorX = Math.floor(x + 1e-7);
            return pbeta.evaluate(prob, size, floorX + 1, lowerTail, logP);
        }
    }

    public static final class PNBinomMu implements Function3_2 {

        public static PNBinomMu create() {
            return new PNBinomMu();
        }

        public static PNBinomMu getUncached() {
            return new PNBinomMu();
        }

        private final PPois ppois = new PPois();

        @Override
        public double evaluate(double x, double size, double mu, boolean lowerTail, boolean logP) {
            if (Double.isNaN(x) || Double.isNaN(size) || Double.isNaN(mu)) {
                return x + size + mu;
            }
            if (!Double.isFinite(mu) || size < 0 || mu < 0) {
                return RMathError.defaultError();
            }

            /* limiting case: point mass at zero */
            if (size == 0) {
                return (x >= 0) ? DPQ.rdt1(lowerTail, logP) : DPQ.rdt0(lowerTail, logP);
            }

            if (x < 0) {
                return DPQ.rdt0(lowerTail, logP);
            }
            if (!Double.isFinite(x)) {
                return DPQ.rdt1(lowerTail, logP);
            }
            if (!Double.isFinite(size)) {
                // limit case: Poisson
                return (ppois.evaluate(x, mu, lowerTail, logP));
            }

            double floorX = Math.floor(x + 1e-7);
            /*
             * return pbeta(pr, size, x + 1, lowerTail, logP); pr = size/(size + mu), 1-pr =
             * mu/(size+mu)
             *
             * = pbeta_raw(pr, size, x + 1, lowerTail, logP) x. pin qin = bratio (pin, qin, x.,
             * 1-x., &w, &wc, &ierr, logP), and return w or wc .. = bratio (size, x+1, pr, 1-pr, &w,
             * &wc, &ierr, logP)
             */
            Bratio bratioResult = Bratio.bratio(size, floorX + 1, size / (size + mu), mu / (size + mu), logP);
            if (bratioResult.ierr != 0) {
                RMathError.warning(Message.GENERIC, Utils.stringFormat("pnbinom_mu() -> bratio() gave error code %d", bratioResult.ierr));
            }
            return lowerTail ? bratioResult.w : bratioResult.w1;
        }
    }
}
