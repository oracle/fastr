/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995, 1996  Robert Gentleman and Ross Ihaka
 * Copyright (c) 1997-2013,  The R Core Team
 * Copyright (c) 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.base.printer;

import java.io.IOException;

import com.oracle.truffle.r.nodes.builtin.base.printer.DoublePrinter.ScientificDouble;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;

//Transcribed from GnuR, src/main/format.c

public final class DoubleVectorPrinter extends VectorPrinter<RAbstractDoubleVector> {

    public static final DoubleVectorPrinter INSTANCE = new DoubleVectorPrinter();

    @Override
    protected VectorPrinter<RAbstractDoubleVector>.VectorPrintJob createJob(RAbstractDoubleVector vector, int indx, boolean quote, PrintContext printCtx) {
        return new DoubleVectorPrintJob(vector, indx, quote, printCtx);
    }

    private final class DoubleVectorPrintJob extends VectorPrintJob {

        protected DoubleVectorPrintJob(RAbstractDoubleVector vector, int indx, boolean quote, PrintContext printCtx) {
            super(vector, indx, quote, printCtx);
        }

        @Override
        protected DoubleVectorMetrics formatVector(int offs, int len) {
            return formatDoubleVector(vector, offs, len, 0, printCtx.parameters());
        }

        @Override
        protected void printElement(int i, FormatMetrics fm) throws IOException {
            DoubleVectorMetrics dfm = (DoubleVectorMetrics) fm;
            String v = DoublePrinter.encodeReal(vector.getDataAt(i), dfm.maxWidth, dfm.d, dfm.e, '.', printCtx.parameters());
            out.print(v);
        }

        @Override
        protected void printCell(int i, FormatMetrics fm) throws IOException {
            printElement(i, fm);
        }

        @Override
        protected void printEmptyVector() throws IOException {
            out.println("numeric(0)");
        }

        @Override
        protected String elementTypeName() {
            return "double";
        }

    }

    public static class DoubleVectorMetrics extends FormatMetrics {
        public final int d;
        public final int e;

        public DoubleVectorMetrics(int w, int d, int e) {
            super(w);
            this.d = d;
            this.e = e;
        }

    }

    public static DoubleVectorMetrics formatDoubleVector(RAbstractDoubleVector x, int offs, int n, int nsmall, PrintParameters pp) {
        int left;
        int right;
        int sleft;
        int mnl;
        int mxl;
        int rgt;
        int mxsl;
        int mxns;
        int wF;
        int neg;
        int sgn;
        int kpower;
        int nsig;
        boolean roundingwidens;
        boolean naflag;
        boolean nanflag;
        boolean posinf;
        boolean neginf;

        // output arguments
        int w;
        int d;
        int e;

        nanflag = false;
        naflag = false;
        posinf = false;
        neginf = false;
        neg = 0;
        rgt = mxl = mxsl = mxns = RRuntime.INT_MIN_VALUE;
        mnl = RRuntime.INT_MAX_VALUE;

        for (int i = 0; i < n; i++) {
            double xi = x.getDataAt(i + offs);
            if (!RRuntime.isFinite(xi)) {
                if (RRuntime.isNA(xi)) {
                    naflag = true;
                } else if (RRuntime.isNAorNaN(xi)) {
                    nanflag = true;
                } else if (xi > 0) {
                    posinf = true;
                } else {
                    neginf = true;
                }
            } else {
                ScientificDouble sd = DoublePrinter.scientific(xi, pp);
                sgn = sd.sgn;
                nsig = sd.nsig;
                kpower = sd.kpower;
                roundingwidens = sd.roundingwidens;

                left = kpower + 1;
                if (roundingwidens) {
                    left--;
                }

                sleft = sgn + ((left <= 0) ? 1 : left); /* >= 1 */
                right = nsig - left; /* #{digits} right of '.' ( > 0 often) */
                if (sgn > 0) {
                    neg = 1; /* if any < 0, need extra space for sign */
                }

                /* Infinite precision "F" Format : */
                if (right > rgt) {
                    rgt = right; /* max digits to right of . */
                }
                if (left > mxl) {
                    mxl = left; /* max digits to left of . */
                }
                if (left < mnl) {
                    mnl = left; /* min digits to left of . */
                }
                if (sleft > mxsl) {
                    mxsl = sleft; /*
                                   * max left includingimport static
                                   * com.oracle.truffle.r.nodes.builtin.base.printer.Utils.*;
                                   * sign(s)
                                   */
                }
                if (nsig > mxns) {
                    mxns = nsig; /* max sig digits */
                }
            }
        }
        /*
         * F Format: use "F" format WHENEVER we use not more space than 'E' and still satisfy
         * 'R_print.digits' {but as if nsmall==0 !}
         *
         * E Format has the form [S]X[.XXX]E+XX[X]
         *
         * This is indicated by setting *e to non-zero (usually 1) If the additional exponent digit
         * is required *e is set to 2
         */

        /*-- These 'mxsl' & 'rgt' are used in F Format
         * AND in the ____ if(.) "F" else "E" ___ below: */
        if (pp.getDigits() == 0) {
            rgt = 0;
        }
        if (mxl < 0) {
            mxsl = 1 + neg; /* we use %#w.dg, so have leading zero */
        }

        /* use nsmall only *after* comparing "F" vs "E": */
        if (rgt < 0) {
            rgt = 0;
        }
        wF = mxsl + rgt + (rgt != 0 ? 1 : 0); /* width for F format */

        /*-- 'see' how "E" Exponential format would be like : */
        e = (mxl > 100 || mnl <= -99) ? 2 : 1; /* 3 digit exponent */
        if (mxns != RRuntime.INT_MIN_VALUE) {
            d = mxns - 1;
            w = neg + (d != 0 ? 1 : 1) + d + 4 + e; /* width for E format */
            if (wF <= w + pp.getScipen()) { /* Fixpoint if it needs less space */
                e = 0;
                if (nsmall > rgt) {
                    rgt = nsmall;
                    wF = mxsl + rgt + (rgt != 0 ? 1 : 0);
                }
                d = rgt;
                w = wF;
            } /* else : "E" Exponential format -- all done above */
        } else { /* when all x[i] are non-finite */
            w = 0; /* to be increased */
            d = 0;
            e = 0;
        }
        if (naflag && w < pp.getNaWidth()) {
            w = pp.getNaWidth();
        }
        if (nanflag && w < 3) {
            w = 3;
        }
        if (posinf && w < 3) {
            w = 3;
        }
        if (neginf && w < 4) {
            w = 4;
        }

        return new DoubleVectorMetrics(w, d, e);
    }

}
