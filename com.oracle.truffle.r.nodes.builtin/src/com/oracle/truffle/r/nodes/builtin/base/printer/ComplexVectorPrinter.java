/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995, 1996  Robert Gentleman and Ross Ihaka
 * Copyright (c) 1997-2013,  The R Core Team
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.base.printer;

import static com.oracle.truffle.r.nodes.builtin.base.printer.DoubleVectorPrinter.NB;
import static com.oracle.truffle.r.nodes.builtin.base.printer.Utils.snprintf;

import java.io.IOException;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.nodes.builtin.base.Round;
import com.oracle.truffle.r.nodes.builtin.base.Round.RoundArithmetic;
import com.oracle.truffle.r.nodes.builtin.base.printer.DoubleVectorPrinter.ScientificDouble;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess.RandomIterator;

//Transcribed from GnuR, src/main/printutils.c

public final class ComplexVectorPrinter extends VectorPrinter<RAbstractComplexVector> {

    static final ComplexVectorPrinter INSTANCE = new ComplexVectorPrinter();

    private ComplexVectorPrinter() {
        // singleton
    }

    @Override
    protected VectorPrinter<RAbstractComplexVector>.VectorPrintJob createJob(RAbstractComplexVector vector, int indx, PrintContext printCtx) {
        return new ComplexVectorPrintJob(vector, indx, printCtx);
    }

    private final class ComplexVectorPrintJob extends VectorPrintJob {

        protected ComplexVectorPrintJob(RAbstractComplexVector vector, int indx, PrintContext printCtx) {
            super(vector, indx, printCtx);
        }

        @Override
        protected String elementTypeName() {
            return "complex";
        }

        @Override
        protected FormatMetrics formatVector(int offs, int len) {
            return formatComplexVector(iterator, access, offs, len, 0, printCtx.parameters());
        }

        @Override
        protected void printElement(int i, FormatMetrics fm) throws IOException {
            ComplexVectorMetrics cfm = (ComplexVectorMetrics) fm;
            String v = encodeComplex(access.getComplex(iterator, i), cfm, '.', printCtx.parameters());
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

    @TruffleBoundary
    static ComplexVectorMetrics formatComplexVector(RandomIterator iter, VectorAccess access, int offs, int n, int nsmall, PrintParameters pp) {
        return formatComplexVector(iter, access, offs, n, nsmall, pp.getDigits(), pp.getScipen(), pp.getNaWidth());
    }

    @TruffleBoundary
    static ComplexVectorMetrics formatComplexVector(RandomIterator iter, VectorAccess access, int offs, int n, int nsmall, int digits, int sciPen, int naWidth) {

        /* format.info() or x[1..l] for both Re & Im */
        int rt;
        int mnl;
        int mxl;
        int mxsl;
        int mxns;
        int irt;
        int imnl;
        int imxl;
        int imxsl;
        int imxns;
        boolean allReZero = true;
        boolean allImZero = true;
        boolean naflag = false;
        boolean rnanflag = false;
        boolean rposinf = false;
        boolean rneginf = false;
        boolean inanflag = false;
        boolean iposinf = false;

        int neg = 0;
        rt = mxl = mxsl = mxns = RRuntime.INT_MIN_VALUE;
        irt = imxl = imxsl = imxns = RRuntime.INT_MIN_VALUE;
        imnl = mnl = RRuntime.INT_MAX_VALUE;

        for (int i = 0; i < n; i++) {
            /* Now round */
            RComplex xi = access.getComplex(iter, offs + i);
            if (RRuntime.isNA(xi.getRealPart()) || RRuntime.isNA(xi.getImaginaryPart())) {
                naflag = true;
            } else {
                /* real part */

                RComplex tmp = zprecr(xi, digits);
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
                    ScientificDouble sd = DoubleVectorPrinter.scientific(tmp.getRealPart(), digits);

                    int left = sd.kpower + 1;
                    if (sd.roundingwidens) {
                        left--;
                    }
                    int sleft = sd.sgn + ((left <= 0) ? 1 : left); /* >= 1 */
                    int right = sd.nsig - left; /* #{digits} right of '.' ( > 0 often) */
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
                    ScientificDouble sd = DoubleVectorPrinter.scientific(tmp.getImaginaryPart(), digits);

                    int left = sd.kpower + 1;
                    if (sd.roundingwidens) {
                        left--;
                    }
                    int sleft = (left <= 0) ? 1 : left;
                    int right = sd.nsig - left;

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

        if (digits == 0) {
            rt = 0;
        }
        int wr;
        int dr;
        int er;
        int wi;
        int wF;
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

        if (digits == 0) {
            irt = 0;
        }
        int di;
        int ei;
        int iwF;
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
            if (iwF <= wi + sciPen) {
                ei = 0;
                if (nsmall > irt) {
                    irt = nsmall;
                    iwF = imxsl + irt + (irt != 0 ? 1 : 0);
                }
                di = irt;
                wi = iwF;
            }
        } else if (allImZero) {
            if (wF <= wr + sciPen) {
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
        } else if (wF + iwF < wr + wi + 2 * sciPen) {
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

        if (naflag && wr + wi + 2 < naWidth) {
            wr += (naWidth - (wr + wi + 2));
        }

        return new ComplexVectorMetrics(wr, dr, er, wi, di, ei);
    }

    private static final RoundArithmetic round = new Round.RoundArithmetic();

    private static final int MAX_DIGITS = 22;

    private static RComplex zprecr(RComplex x, int digits) {
        double m1 = Math.abs(x.getRealPart());
        double m2 = Math.abs(x.getImaginaryPart());
        double m = 0;
        if (Double.isFinite(m1)) {
            m = m1;
        }
        if (Double.isFinite(m2) && m2 > m) {
            m = m2;
        }
        if (m == 0.0) {
            return x;
        }
        if (!Double.isFinite(digits)) {
            if (digits > 0) {
                return x;
            } else {
                return RComplex.valueOf(0, 0);
            }
        }
        int dig = (int) Math.floor(digits + 0.5);
        if (dig > MAX_DIGITS) {
            return x;
        } else if (dig < 1) {
            dig = 1;
        }
        int mag = (int) Math.floor(Math.log10(m));
        dig = dig - mag - 1;
        if (dig > 306) {
            double pow10 = 1.0e4;
            RComplex tmp = round.opd(pow10 * x.getRealPart(), pow10 * x.getImaginaryPart(), dig - 4);
            return RComplex.valueOf(tmp.getRealPart() / pow10, tmp.getImaginaryPart() / pow10);
        } else {
            return round.opd(x.getRealPart(), x.getImaginaryPart(), dig);
        }
    }

    @TruffleBoundary
    public static String encodeComplex(RComplex x) {
        return encodeComplex(x, 15, 0, RRuntime.STRING_NA);
    }

    @TruffleBoundary
    public static String encodeComplex(RComplex x, int digits) {
        return encodeComplex(x, digits, 0, RRuntime.STRING_NA);
    }

    @TruffleBoundary
    public static String encodeComplex(RComplex x, int digits, int sciPen, String naString) {
        VectorAccess access = x.slowPathAccess();
        try (RandomIterator iter = access.randomAccess(x)) {
            ComplexVectorMetrics cvm = formatComplexVector(iter, access, 0, 1, 0, digits, sciPen, naString.length());
            return encodeComplex(x, cvm, '.', digits, naString);
        }
    }

    @TruffleBoundary
    static String encodeComplex(RComplex x, ComplexVectorMetrics cvm, char cdec, PrintParameters pp) {
        return encodeComplex(x, cvm, cdec, pp.getDigits(), pp.getNaString());
    }

    @TruffleBoundary
    static String encodeComplex(RComplex x, ComplexVectorMetrics cvm, char cdec, int digits, String naString) {
        if (x.isNA()) {
            return DoubleVectorPrinter.encodeReal(RRuntime.DOUBLE_NA, cvm.maxWidth, 0, 0, cdec, naString);
        } else {
            String s = encodeComplex(x, cvm.wr, cvm.dr, cvm.er, cvm.wi, cvm.di, cvm.ei, cdec, digits, naString);
            int g = cvm.maxWidth - cvm.wr - cvm.wi - 2;
            if (g > 0) {
                // fill the remaining space by blanks to fit the maxWidth
                String blanks = String.format("%" + g + "s", "");
                s = blanks + s;
            }
            return s;
        }
    }

    @TruffleBoundary
    private static String encodeComplex(RComplex x, int wr, int dr, int er, int wi, int di, int ei, char cdec, int digits, String naString) {
        /* IEEE allows signed zeros; strip these here */
        double xr = RRuntime.normalizeZero(x.getRealPart());
        double xi = RRuntime.normalizeZero(x.getImaginaryPart());

        if (RRuntime.isNA(xr) || RRuntime.isNA(xi)) {
            int g = Math.min(wr + wi + 2, (NB - 1));
            String fmt = "%" + Utils.asBlankArg(g) + "s";
            return Utils.snprintf(NB,
                            fmt, /* was "%*s%*s", R_print.gap, "", */
                            naString);
        } else {
            /*
             * formatComplex rounded, but this does not, and we need to keep it that way so we don't
             * get strange trailing zeros. But we do want to avoid printing small exponentials that
             * are probably garbage.
             */
            RComplex y = zprecr(x, digits);
            String re;
            if (y.getRealPart() == 0.) {
                re = DoubleVectorPrinter.encodeReal(y.getRealPart(), wr, dr, er, cdec, naString);
            } else {
                re = DoubleVectorPrinter.encodeReal(xr, wr, dr, er, cdec, naString);
            }
            boolean flagNegIm = xi < 0;
            if (flagNegIm) {
                xi = -xi;
            }
            String im;
            if (y.getImaginaryPart() == 0.) {
                im = DoubleVectorPrinter.encodeReal(y.getImaginaryPart(), wi, di, ei, cdec, naString);
            } else {
                im = DoubleVectorPrinter.encodeReal(xi, wi, di, ei, cdec, naString);
            }
            return snprintf(NB, "%s%s%si", re, flagNegIm ? "-" : "+", im);
        }
    }

    public static String[] format(RAbstractComplexVector value, boolean trim, int nsmall, int width, char decimalMark, PrintParameters pp) {
        VectorAccess access = value.slowPathAccess();
        try (RandomIterator iter = access.randomAccess(value)) {
            int length = access.getLength(iter);
            ComplexVectorMetrics dfm = formatComplexVector(iter, access, 0, length, nsmall, pp);
            int wr = Math.max(trim ? 1 : dfm.wr, width);
            int wi = Math.max(trim ? 1 : dfm.wi, width);
            ComplexVectorMetrics adjusted = new ComplexVectorMetrics(wr, dfm.dr, dfm.er, wi, dfm.di, dfm.ei);

            String[] result = new String[length];
            for (int i = 0; i < length; i++) {
                result[i] = encodeComplex(access.getComplex(iter, i), adjusted, decimalMark, pp);
            }
            return result;
        }
    }
}
