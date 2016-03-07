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

import static com.oracle.truffle.r.nodes.builtin.base.printer.DoublePrinter.NB;
import static com.oracle.truffle.r.nodes.builtin.base.printer.Utils.snprintf;

import java.io.IOException;

import com.oracle.truffle.r.nodes.builtin.base.printer.DoublePrinter.ScientificDouble;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import com.oracle.truffle.r.runtime.ops.UnaryArithmetic;

//Transcribed from GnuR, src/main/printutils.c

public final class ComplexVectorPrinter extends VectorPrinter<RAbstractComplexVector> {

    public static final ComplexVectorPrinter INSTANCE = new ComplexVectorPrinter();

    @Override
    protected VectorPrinter<RAbstractComplexVector>.VectorPrintJob createJob(RAbstractComplexVector vector, int indx, boolean quote, PrintContext printCtx) {
        return new ComplexVectorPrintJob(vector, indx, quote, printCtx);
    }

    private final class ComplexVectorPrintJob extends VectorPrintJob {

        protected ComplexVectorPrintJob(RAbstractComplexVector vector, int indx, boolean quote, PrintContext printCtx) {
            super(vector, indx, quote, printCtx);
        }

        @Override
        protected String elementTypeName() {
            return "complex";
        }

        @Override
        protected FormatMetrics formatVector(int offs, int len) {
            return formatComplexVector(vector, offs, len, 0, printCtx.parameters());
        }

        @Override
        protected void printElement(int i, FormatMetrics fm) throws IOException {
            ComplexVectorMetrics cfm = (ComplexVectorMetrics) fm;
            String v = encodeComplex(vector.getDataAt(i), cfm, printCtx.parameters());
            out.print(v);
        }

        @Override
        protected void printCell(int i, FormatMetrics fm) throws IOException {
            printElement(i, fm);
        }

        @Override
        protected void printEmptyVector() throws IOException {
            out.print("complex(0)");
        }

    }

    public static final class ComplexVectorMetrics extends FormatMetrics {
        public final int wr;
        public final int dr;
        public final int er;
        public final int wi;
        public final int di;
        public final int ei;

        public ComplexVectorMetrics(int wr, int dr, int er, int wi, int di, int ei) {
            super(wr + wi + 2);
            this.wr = wr;
            this.dr = dr;
            this.er = er;
            this.wi = wi;
            this.di = di;
            this.ei = ei;
        }

    }

    public static ComplexVectorMetrics formatComplexVector(RAbstractComplexVector x, int offs, int n, int nsmall, PrintParameters pp) {

        int wr;
        int dr;
        int er;
        int wi;
        int di;
        int ei;

        /* format.info() or x[1..l] for both Re & Im */
        int left;
        int right;
        int sleft;
        int rt;
        int mnl;
        int mxl;
        int mxsl;
        int mxns;
        int wF;
        int iwF;
        int irt;
        int imnl;
        int imxl;
        int imxsl;
        int imxns;
        int neg;
        boolean naflag;
        boolean rnanflag;
        boolean rposinf;
        boolean rneginf;
        boolean inanflag;
        boolean iposinf;
        RComplex tmp;
        boolean allReZero = true;
        boolean allImZero = true;

        naflag = false;
        rnanflag = false;
        rposinf = false;
        rneginf = false;
        inanflag = false;
        iposinf = false;
        neg = 0;

        rt = mxl = mxsl = mxns = RRuntime.INT_MIN_VALUE;
        irt = imxl = imxsl = imxns = RRuntime.INT_MIN_VALUE;
        imnl = mnl = RRuntime.INT_MAX_VALUE;

        for (int i = 0; i < n; i++) {
            /* Now round */
            RComplex xi = x.getDataAt(offs + i);
            if (RRuntime.isNA(xi.getRealPart()) || RRuntime.isNA(xi.getImaginaryPart())) {
                naflag = true;
            } else {
                /* real part */

                tmp = zprecr(xi, pp.getDigits());

                if (!RRuntime.isFinite(tmp.getRealPart())) {
                    if (RRuntime.isNAorNaN(tmp.getRealPart())) {
                        rnanflag = true;
                    } else if (tmp.getRealPart() > 0) {
                        rposinf = true;
                    } else {
                        rneginf = true;
                    }
                } else {
                    if (xi.getRealPart() != 0) {
                        allReZero = false;
                    }
                    ScientificDouble sd = DoublePrinter.scientific(tmp.getRealPart(), pp);

                    left = sd.kpower + 1;
                    if (sd.roundingwidens) {
                        left--;
                    }
                    sleft = sd.sgn + ((left <= 0) ? 1 : left); /* >= 1 */
                    right = sd.nsig - left; /* #{digits} right of '.' ( > 0 often) */
                    if (sd.sgn != 0) {
                        neg = 1; /* if any < 0, need extra space for sign */
                    }

                    if (right > rt) {
                        rt = right; /* max digits to right of . */
                    }
                    if (left > mxl) {
                        mxl = left; /* max digits to left of . */
                    }
                    if (left < mnl) {
                        mnl = left; /* min digits to left of . */
                    }
                    if (sleft > mxsl) {
                        mxsl = sleft; /* max left including sign(s) */
                    }
                    if (sd.nsig > mxns) {
                        mxns = sd.nsig; /* max sig digits */
                    }

                }
                /* imaginary part */

                /* this is always unsigned */
                /* we explicitly put the sign in when we print */

                if (!RRuntime.isFinite(tmp.getImaginaryPart())) {
                    if (RRuntime.isNAorNaN(tmp.getImaginaryPart())) {
                        inanflag = true;
                    } else {
                        iposinf = true;
                    }
                } else {
                    if (xi.getImaginaryPart() != 0) {
                        allImZero = false;
                    }
                    ScientificDouble sd = DoublePrinter.scientific(tmp.getImaginaryPart(), pp);

                    left = sd.kpower + 1;
                    if (sd.roundingwidens) {
                        left--;
                    }
                    sleft = (left <= 0) ? 1 : left;
                    right = sd.nsig - left;

                    if (right > irt) {
                        irt = right;
                    }
                    if (left > imxl) {
                        imxl = left;
                    }
                    if (left < imnl) {
                        imnl = left;
                    }
                    if (sleft > imxsl) {
                        imxsl = sleft;
                    }
                    if (sd.nsig > imxns) {
                        imxns = sd.nsig;
                    }
                }
                /* done: ; */

            }
        }

        /* see comments in formatReal() for details on this */

        /* overall format for real part */

        if (pp.getDigits() == 0) {
            rt = 0;
        }
        if (mxl != RRuntime.INT_MIN_VALUE) {
            if (mxl < 0) {
                mxsl = 1 + neg;
            }
            if (rt < 0) {
                rt = 0;
            }
            wF = mxsl + rt + (rt != 0 ? 1 : 0);

            er = (mxl > 100 || mnl < -99) ? 2 : 1;
            dr = mxns - 1;
            wr = neg + (dr > 0 ? 1 : 0) + dr + 4 + er;
        } else {
            er = 0;
            wr = 0;
            dr = 0;
            wF = 0;
        }

        /* overall format for imaginary part */

        if (pp.getDigits() == 0) {
            irt = 0;
        }
        if (imxl != RRuntime.INT_MIN_VALUE) {
            if (imxl < 0) {
                imxsl = 1;
            }
            if (irt < 0) {
                irt = 0;
            }
            iwF = imxsl + irt + (irt != 0 ? 1 : 0);

            ei = (imxl > 100 || imnl < -99) ? 2 : 1;
            di = imxns - 1;
            wi = (di > 0 ? 1 : 0) + di + 4 + ei;
        } else {
            ei = 0;
            wi = 0;
            di = 0;
            iwF = 0;
        }

        /* Now make the fixed/scientific decision */
        if (allReZero) {
            er = dr = 0;
            wr = wF;
            if (iwF <= wi + pp.getScipen()) {
                ei = 0;
                if (nsmall > irt) {
                    irt = nsmall;
                    iwF = imxsl + irt + (irt != 0 ? 1 : 0);
                }
                di = irt;
                wi = iwF;
            }
        } else if (allImZero) {
            if (wF <= wr + pp.getScipen()) {
                er = 0;
                if (nsmall > rt) {
                    rt = nsmall;
                    wF = mxsl + rt + (rt != 0 ? 1 : 0);
                }
                dr = rt;
                wr = wF;
            }
            ei = di = 0;
            wi = iwF;
        } else if (wF + iwF < wr + wi + 2 * pp.getScipen()) {
            er = 0;
            if (nsmall > rt) {
                rt = nsmall;
                wF = mxsl + rt + (rt != 0 ? 1 : 0);
            }
            dr = rt;
            wr = wF;

            ei = 0;
            if (nsmall > irt) {
                irt = nsmall;
                iwF = imxsl + irt + (irt != 0 ? 1 : 0);
            }
            di = irt;
            wi = iwF;
        } /* else scientific for both */
        if (wr < 0) {
            wr = 0;
        }
        if (wi < 0) {
            wi = 0;
        }

        /* Ensure space for Inf and NaN */
        if (rnanflag && wr < 3) {
            wr = 3;
        }
        if (rposinf && wr < 3) {
            wr = 3;
        }
        if (rneginf && wr < 4) {
            wr = 4;
        }
        if (inanflag && wi < 3) {
            wi = 3;
        }
        if (iposinf && wi < 3) {
            wi = 3;
        }

        /* finally, ensure that there is space for NA */

        if (naflag && wr + wi + 2 < pp.getNaWidth()) {
            wr += (pp.getNaWidth() - (wr + wi + 2));
        }

        return new ComplexVectorMetrics(wr, dr, er, wi, di, ei);
    }

    private static UnaryArithmetic round = UnaryArithmetic.ROUND.create();

    private static RComplex zprecr(RComplex x, int digits) {
        return round.opd(x.getRealPart(), x.getImaginaryPart(), digits);
    }

    public static String encodeComplex(RComplex x, ComplexVectorMetrics cvm, PrintParameters pp) {
        if (RRuntime.isNA(x.getRealPart()) || RRuntime.isNA(x.getImaginaryPart())) {
            return DoublePrinter.encodeReal(RRuntime.DOUBLE_NA, cvm.maxWidth, 0, 0, '.', pp);
        } else {
            String s = encodeComplex(x, cvm.wr, cvm.dr, cvm.er, cvm.wi, cvm.di, cvm.ei, '.', pp);
            int g = cvm.maxWidth - cvm.wr - cvm.wi - 2;
            if (g > 0) {
                // fill the remaining space by blanks to fit the maxWidth
                String blanks = String.format("%" + g + "s", "");
                s = blanks + s;
            }
            return s;
        }
    }

    public static String encodeComplex(RComplex x, int wr, int dr, int er, int wi, int di, int ei,
                    char cdec, PrintParameters pp) {
        String buff;
        String im;
        String re;
        boolean flagNegIm = false;
        RComplex y;

        double xr = x.getRealPart();
        double xi = x.getImaginaryPart();

        /* IEEE allows signed zeros; strip these here */
        if (xr == 0.0) {
            xr = 0.0;
        }
        if (xi == 0.0) {
            xi = 0.0;
        }

        if (RRuntime.isNA(xr) || RRuntime.isNA(xi)) {
            int g = Math.min(wr + wi + 2, (NB - 1));
            String fmt = "%" + Utils.asBlankArg(g) + "s";
            buff = Utils.snprintf(NB,
                            fmt, /* was "%*s%*s", R_print.gap, "", */
                            pp.getNaString());
        } else {
            /*
             * formatComplex rounded, but this does not, and we need to keep it that way so we don't
             * get strange trailing zeros. But we do want to avoid printing small exponentials that
             * are probably garbage.
             */
            y = zprecr(x, pp.getDigits());
            if (y.getRealPart() == 0.) {
                re = DoublePrinter.encodeReal(y.getRealPart(), wr, dr, er, cdec, pp);
            } else {
                re = DoublePrinter.encodeReal(xr, wr, dr, er, cdec, pp);
            }
            flagNegIm = xi < 0;
            if (flagNegIm) {
                xi = -xi;
            }
            if (y.getImaginaryPart() == 0.) {
                im = DoublePrinter.encodeReal(y.getImaginaryPart(), wi, di, ei, cdec, pp);
            } else {
                im = DoublePrinter.encodeReal(xi, wi, di, ei, cdec, pp);
            }
            buff = snprintf(NB, "%s%s%si", re, flagNegIm ? "-" : "+", im);
        }
        return buff;
    }

}
