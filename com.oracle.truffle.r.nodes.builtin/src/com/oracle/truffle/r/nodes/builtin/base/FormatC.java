/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.doubleValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.integerValue;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.base.Round.RoundArithmetic;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

@RBuiltin(name = "formatC", kind = INTERNAL, parameterNames = {"x", "mode", "width", "digits", "format", "flag", "i.strlen"}, behavior = PURE)
public abstract class FormatC extends RBuiltinNode {

    @Child private RoundArithmetic round = new RoundArithmetic();

    static {
        Casts casts = new Casts(FormatC.class);
        casts.arg("x").mustBe(integerValue().or(doubleValue()), Message.UNSUPPORTED_TYPE);
        casts.arg("mode").asStringVector().findFirst();
        casts.arg("width").asIntegerVector().findFirst();
        casts.arg("digits").asIntegerVector().findFirst();
        casts.arg("format").asStringVector().findFirst();
        casts.arg("flag").asStringVector().findFirst();
        casts.arg("i.strlen").asIntegerVector().findFirst();
    }

    @Specialization
    @TruffleBoundary
    RAttributable formatC(RAbstractVector x, String mode, int width, int digits, String format, String flag, @SuppressWarnings("unused") int iStrlen) {
        // ignores iStrlen
        RType type = "integer".equals(mode) ? RType.Integer : RType.Double;
        String[] result = strSignif(x, type, width, digits, format, flag);
        return RDataFactory.createStringVector(result, true);
    }

    /*
     * Former src/appl/strsignif.c
     *
     * Copyright (C) Martin Maechler, 1994, 1998 Copyright (C) 2001-2013 the R Core Team
     *
     * I want you to preserve the copyright of the original author(s), and encourage you to send me
     * any improvements by e-mail. (MM).
     *
     * Originally from Bill Dunlap bill@stat.washington.edu Wed Feb 21, 1990
     *
     * Much improved by Martin Maechler, including the "fg" format.
     *
     * Patched by Friedrich.Leisch@ci.tuwien.ac.at Fri Nov 22, 1996
     *
     * Some fixes by Ross Ihaka ihaka@stat.auckland.ac.nz Sat Dec 21, 1996 Integer arguments changed
     * from "long" to "int" Bus error due to non-writable strings fixed
     *
     * BDR 2001-10-30 use R_alloc not Calloc as memory was not reclaimed on error (and there are
     * many error exits).
     *
     * type "double" or "integer" (R - numeric 'mode').
     *
     * width The total field width; width < 0 means to left justify the number in this field
     * (equivalent to flag = "-"). It is possible that the result will be longer than this, but that
     * should only happen in reasonable cases.
     *
     * digits The desired number of digits after the decimal point. digits < 0 uses the default for
     * C, namely 6 digits.
     *
     * format "d" (for integers) or "f", "e","E", "g", "G" (for 'real') "f" gives numbers in the
     * usual "xxx.xxx" format; "e" and "E" give n.ddde<nn> or n.dddE<nn> (scientific format); "g"
     * and "G" puts them into scientific format if it saves space to do so. NEW: "fg" gives numbers
     * in "xxx.xxx" format as "f", ~~ however, digits are *significant* digits and, if digits > 0,
     * no trailing zeros are produced, as in "g".
     *
     * flag Format modifier as in K&R "C", 2nd ed., p.243; e.g., "0" pads leading zeros; "-" does
     * left adjustment the other possible flags are "+", " ", and "#". New (Feb.98): if flag has
     * more than one character, all are passed..
     */

    private String[] strSignif(RAbstractVector x, RType type, int width, int digits, String format, String flag) {
        int dig = Math.abs(digits);
        boolean rmTrailing0 = digits >= 0;
        boolean doFg = "fg".equals(format); /* TRUE iff format == "fg" */

        if (width == 0) {
            throw error(Message.GENERIC, "width cannot be zero");
        }

        String[] result = new String[x.getLength()];
        if ("d".equals(format)) {
            String form = "%" + flag + width + "d";
            if (type == RType.Integer) {
                for (int i = 0; i < x.getLength(); i++) {
                    result[i] = String.format(form, x.getDataAtAsObject(i));
                }
            } else {
                throw error(Message.GENERIC, "'type' must be \"integer\" for  \"d\"-format");
            }
        } else { /* --- floating point --- */

            if (type == RType.Double) {
                if (doFg) { /* do smart "f" : */
                    for (int i = 0; i < x.getLength(); i++) {
                        double xx = ((RAbstractDoubleVector) x).getDataAt(i);
                        if (xx == 0.) {
                            result[i] = "0";
                        } else {
                            /*
                             * This was iex= (int)floor(log10(fabs(xx))) That's wrong, as xx might
                             * get rounded up, and we do need some fuzz or 99.5 is correct.
                             */
                            double xxx = Math.abs(xx);
                            int iex = (int) Math.floor(Math.log10(xxx) + 1e-12);
                            double scaledX = round.opd(xxx / Math.pow(10, iex) + 1e-12, dig - 1);
                            if (iex > 0 && scaledX >= 10) {
                                xx = scaledX * Math.pow(10, iex);
                                iex++;
                            }
                            if (iex == -4 && Math.abs(xx) < 1e-4) {
                                /* VERY rare case */
                                iex = -5;
                            }
                            if (iex < -4) {
                                /* "g" would result in 'e-' representation: */
                                String form = "%" + flag + "." + (dig - 1 + -iex) + "f";
                                String str = String.format(form, xx);
                                /* Remove trailing "0"s __ IFF flag has no '#': */
                                if (rmTrailing0) {
                                    int j = str.length();
                                    while (j > 0 && str.charAt(j - 1) == '0') {
                                        j--;
                                    }
                                    if (j != str.length()) {
                                        str = str.substring(0, j);
                                    }
                                }
                                result[i] = str;
                            } else { /* iex >= -4: NOT "e-" */
                                /* if iex >= dig, would have "e+" representation */
                                String formatString = "%" + flag + width + "." + ((iex >= dig) ? (iex + 1) : dig) + "g";
                                result[i] = trimZero(String.format(formatString, xx));
                            }
                        } /* xx != 0 */
                    } /* if(do_fg) for(i..) */
                } else {
                    String form = "%" + flag + width + "." + dig + format;
                    for (int i = 0; i < x.getLength(); i++) {
                        String str = String.format(form, x.getDataAtAsObject(i));
                        result[i] = ("g".equals(format) || "f".equals(format)) ? trimZero(str) : str;
                    }
                }
            } else {
                throw error(Message.GENERIC, "'type' must be \"real\" for this format");
            }
        }
        return result;
    }

    private static String trimZero(String str) {
        int i = str.length();
        while (i > 0 && str.charAt(i - 1) == '0') {
            i--;
        }
        if (i > 0 && str.charAt(i - 1) == '.') {
            i--;
            return str.substring(0, i);
        }
        if (i == str.length()) {
            return str;
        }
        // need to check whether we're after the decimal point:
        int j = i;
        while (j > 0 && str.charAt(j - 1) >= '0' && str.charAt(j) <= '9') {
            j--;
        }
        if (j > 0 && str.charAt(j - 1) == '.') {
            return str.substring(0, i);
        }
        return str;
    }
}
