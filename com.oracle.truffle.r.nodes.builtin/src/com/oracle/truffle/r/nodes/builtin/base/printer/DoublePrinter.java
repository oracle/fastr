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

import static com.oracle.truffle.r.nodes.builtin.base.printer.Utils.snprintf;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.RoundingMode;
import java.text.DecimalFormat;

import com.oracle.truffle.r.nodes.builtin.base.printer.DoubleVectorPrinter.DoubleVectorMetrics;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RDouble;

//Transcribed from GnuR, src/main/printutils.c

public final class DoublePrinter extends AbstractValuePrinter<Double> {

    public static final DoublePrinter INSTANCE = new DoublePrinter();

    @Override
    protected void printValue(Double value, PrintContext printCtx) throws IOException {
        double x = value;

        PrintWriter out = printCtx.output();
        out.print("[1] ");
        DoubleVectorMetrics dm = DoubleVectorPrinter.formatDoubleVector(RDouble.valueOf(x), 0, 1, 0, printCtx.parameters());
        out.println(encodeReal(x, dm.maxWidth, dm.d, dm.e, '.', printCtx.parameters()));
    }

    private static final int DBL_DIG = 15;

    private static final double[] tbl = {
                    1e-1,
                    1e00, 1e01, 1e02, 1e03, 1e04, 1e05, 1e06, 1e07, 1e08, 1e09,
                    1e10, 1e11, 1e12, 1e13, 1e14, 1e15, 1e16, 1e17, 1e18, 1e19,
                    1e20, 1e21, 1e22
    };
    private static final int KP_MAX = 22;
    private static final int R_dec_min_exponent = -308;
    private static final int NB = 1000;

    public static class ScientificDouble {
        public final int sgn;
        public final int kpower;
        public final int nsig;
        public final boolean roundingwidens;

        public ScientificDouble(int sgn, int kpower, int nsig, boolean roundingwidens) {
            super();
            this.sgn = sgn;
            this.kpower = kpower;
            this.nsig = nsig;
            this.roundingwidens = roundingwidens;
        }

    }

    public static ScientificDouble scientific(double x, PrintParameters pp) {
        /*
         * for a number x , determine sgn = 1_{x < 0} {0/1} kpower = Exponent of 10; nsig =
         * min(R_print.digits, #{significant digits of alpha}) roundingwidens = 1 if rounding causes
         * x to increase in width, 0 otherwise
         *
         * where |x| = alpha * 10^kpower and 1 <= alpha < 10
         */
        double alpha;
        double r;
        int kp;
        int j;

        // output arguments
        int sgn;
        int kpower;
        int nsig;
        boolean roundingwidens;

        if (x == 0.0) {
            kpower = 0;
            nsig = 1;
            sgn = 0;
            roundingwidens = false;
            r = 0.0;
        } else {
            if (x < 0.0) {
                sgn = 1;
                r = -x;
            } else {
                sgn = 0;
                r = x;
            }

            if (pp.getDigits() >= DBL_DIG + 1) {
                // TODO:
                // format_via_sprintf(r, pp.getDigits(), kpower, nsig);
                roundingwidens = false;
                // return;
                throw new UnsupportedOperationException();
            }

            kp = (int) Math.floor(Math.log10(r)) - pp.getDigits() + 1; // r = |x|;
                                                                       // 10^(kp + digits - 1) <= r

            double rPrec = r;
            /* use exact scaling factor in double precision, if possible */
            if (Math.abs(kp) <= 22) {
                if (kp >= 0) {
                    rPrec /= tbl[kp + 1];
                } else {
                    rPrec *= tbl[-kp + 1];
                }
            } else if (kp <= R_dec_min_exponent) {
                /*
                 * on IEEE 1e-308 is not representable except by gradual underflow. Shifting by 303
                 * allows for any potential denormalized numbers x, and makes the reasonable
                 * assumption that R_dec_min_exponent+303 is in range. Representation of 1e+303 has
                 * low error.
                 */
                rPrec = (rPrec * 1e+303) / Math.pow(10, kp + 303);
            } else {
                rPrec /= Math.pow(10, kp);
            }
            if (rPrec < tbl[pp.getDigits()]) {
                rPrec *= 10.0;
                kp--;
            }
            /* round alpha to integer, 10^(digits-1) <= alpha <= 10^digits */
            /*
             * accuracy limited by double rounding problem, alpha already rounded to 53 bits
             */
            alpha = Math.round(rPrec);

            nsig = pp.getDigits();
            for (j = 1; j <= pp.getDigits(); j++) {
                alpha /= 10.0;
                if (alpha == Math.floor(alpha)) {
                    nsig--;
                } else {
                    break;
                }
            }
            if (nsig == 0 && pp.getDigits() > 0) {
                nsig = 1;
                kp += 1;
            }
            kpower = kp + pp.getDigits() - 1;

            /*
             * Scientific format may do more rounding than fixed format, e.g. 9996 with 3 digits is
             * 1e+04 in scientific, but 9996 in fixed. This happens when the true value r is less
             * than 10^(kpower+1) and would not round up to it in fixed format. Here rgt is the
             * decimal place that will be cut off by rounding
             */

            int rgt = pp.getDigits() - kpower;
            /* bound rgt by 0 and KP_MAX */
            rgt = rgt < 0 ? 0 : rgt > KP_MAX ? KP_MAX : rgt;
            double fuzz = 0.5 / tbl[1 + rgt];
            // kpower can be bigger than the table.
            roundingwidens = kpower > 0 && kpower <= KP_MAX && r < tbl[kpower + 1] - fuzz;

        }

        return new ScientificDouble(sgn, kpower, nsig, roundingwidens);
    }

    public static String encodeReal(double x, DoubleVectorMetrics dm, PrintParameters pp) {
        return encodeReal(x, dm.maxWidth, dm.d, dm.e, '.', pp);
    }

    public static String encodeReal(double x, int w, int d, int e, char cdec, PrintParameters pp) {
        final String buff;
        String fmt;

        /* IEEE allows signed zeros (yuck!) */
        if (x == 0.0) {
            x = 0.0;
        }
        if (!RRuntime.isFinite(x)) {
            int numBlanks = Math.min(w, (NB - 1));
            String naFmt = "%" + numBlanks + "s";
            if (RRuntime.isNA(x)) {
                buff = snprintf(NB, naFmt, pp.getNaString());
            } else if (RRuntime.isNAorNaN(x)) {
                buff = snprintf(NB, naFmt, "NaN");
            } else if (x > 0) {
                buff = snprintf(NB, naFmt, "Inf");
            } else {
                buff = snprintf(NB, naFmt, "-Inf");
            }
        } else if (e != 0) {
            if (d != 0) {
                fmt = String.format("%%#%d.%de", Math.min(w, (NB - 1)), d);
                buff = snprintf(NB, fmt, x);
            } else {
                fmt = String.format("%%%d.%de", Math.min(w, (NB - 1)), d);
                buff = snprintf(NB, fmt, x);
            }
        } else { /* e = 0 */
            StringBuilder sb = new StringBuilder("#.#");
            DecimalFormat df = new DecimalFormat(sb.toString());
            df.setRoundingMode(RoundingMode.HALF_EVEN);
            df.setDecimalSeparatorAlwaysShown(false);
            df.setMinimumFractionDigits(d);
            df.setMaximumFractionDigits(d);
            String ds = df.format(x);
            int blanks = w - ds.length();
            fmt = "%" + Utils.asBlankArg(blanks) + "s%s";
            buff = String.format(fmt, "", ds);
        }

        if (cdec != '.') {
            buff.replace('.', cdec);
        }

        return buff;
    }

}
