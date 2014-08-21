/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import java.util.*;

import com.oracle.truffle.api.CompilerDirectives.SlowPath;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin(name = "sprintf", kind = SUBSTITUTE, parameterNames = {"fmt", "..."})
// TODO INTERNAL
public abstract class Sprintf extends RBuiltinNode {

    @Override
    public RNode[] getParameterValues() {
        return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance)};
    }

    @Specialization
    protected String sprintf(String fmt, @SuppressWarnings("unused") RMissing x) {
        controlVisibility();
        return fmt;
    }

    @Specialization(guards = "fmtLengthOne")
    protected String sprintf(RAbstractStringVector fmt, RMissing x) {
        return sprintf(fmt.getDataAt(0), x);
    }

    @Specialization
    protected String sprintf(String fmt, int x) {
        controlVisibility();
        return format(fmt, x);
    }

    @Specialization(guards = "fmtLengthOne")
    protected String sprintf(RAbstractStringVector fmt, int x) {
        return sprintf(fmt.getDataAt(0), x);
    }

    @Specialization
    protected RStringVector sprintf(String fmt, RAbstractIntVector x) {
        controlVisibility();
        String[] r = new String[x.getLength()];
        for (int k = 0; k < r.length; ++k) {
            r[k] = format(fmt, x.getDataAt(k));
        }
        return RDataFactory.createStringVector(r, RDataFactory.COMPLETE_VECTOR);
    }

    @Specialization(guards = "fmtLengthOne")
    protected RStringVector sprintf(RAbstractStringVector fmt, RAbstractIntVector x) {
        return sprintf(fmt.getDataAt(0), x);
    }

    @Specialization
    protected String sprintf(String fmt, double x) {
        controlVisibility();
        char f = Character.toLowerCase(firstFormatChar(fmt));
        if (f == 'x' || f == 'd') {
            if (Math.floor(x) == x) {
                return format(fmt, (long) x);
            }
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_FORMAT_DOUBLE, fmt);
        }
        return format(fmt, x);
    }

    @Specialization(guards = "fmtLengthOne")
    protected String sprintf(RAbstractStringVector fmt, double x) {
        return sprintf(fmt.getDataAt(0), x);
    }

    @Specialization
    protected RStringVector sprintf(String fmt, RAbstractDoubleVector x) {
        controlVisibility();
        String[] r = new String[x.getLength()];
        for (int k = 0; k < r.length; ++k) {
            r[k] = sprintf(fmt, x.getDataAt(k));
        }
        return RDataFactory.createStringVector(r, RDataFactory.COMPLETE_VECTOR);
    }

    @Specialization(guards = "fmtLengthOne")
    protected RStringVector sprintf(RAbstractStringVector fmt, RAbstractDoubleVector x) {
        return sprintf(fmt.getDataAt(0), x);
    }

    @Specialization
    protected String sprintf(String fmt, String x) {
        controlVisibility();
        return format(fmt, x);
    }

    @Specialization(guards = "fmtLengthOne")
    protected String sprintf(RAbstractStringVector fmt, String x) {
        return sprintf(fmt.getDataAt(0), x);
    }

    @Specialization
    protected RStringVector sprintf(String fmt, RAbstractStringVector x) {
        controlVisibility();
        String[] r = new String[x.getLength()];
        for (int k = 0; k < r.length; ++k) {
            r[k] = format(fmt, x.getDataAt(k));
        }
        return RDataFactory.createStringVector(r, RDataFactory.COMPLETE_VECTOR);
    }

    @Specialization(guards = "fmtLengthOne")
    protected RStringVector sprintf(RAbstractStringVector fmt, RAbstractStringVector x) {
        return sprintf(fmt.getDataAt(0), x);
    }

    @Specialization
    protected String sprintf(String fmt, Object[] args) {
        controlVisibility();
        return format(fmt, args);
    }

    private static String format(String fmt, Object... args) {
        char[] conversions = new char[args.length];
        String format = processFormat(fmt, args, conversions);
        adjustValues(args, conversions);
        return stringFormat(format, args);
    }

    private static String processFormat(String fmt, Object[] args, char[] conversions) {
        int i = 0;
        char[] cs = fmt.toCharArray();
        StringBuilder sb = new StringBuilder();
        int argc = 1;

        while (i < cs.length) {
            // skip up to and including next %
            while (i < cs.length && cs[i] != '%') {
                sb.append(cs[i++]);
            }
            if (i == cs.length) {
                break;
            }
            sb.append(cs[i++]);

            FormatInfo fi = extractFormatInfo(cs, i, argc);
            argc = fi.argc;
            if (fi.conversion != '%') {
                // take care of width/precision being defined by args
                int w = 0;
                int p = 0;
                if (fi.width != 0 || fi.widthIsArg) {
                    w = fi.widthIsArg ? intValue(args[fi.width - 1]) : fi.width;
                }
                if (fi.precision != 0 || fi.precisionIsArg) {
                    p = fi.precisionIsArg ? intValue(args[fi.precision - 1]) : fi.precision;
                }
                // which argument to print
                sb.append(intString(fi.numArg)).append('$');
                // flags
                if (fi.adjustLeft) {
                    sb.append('-');
                }
                if (fi.alwaysSign) {
                    sb.append('+');
                }
                if (fi.alternate) {
                    sb.append('#');
                }
                if (fi.padZero) {
                    sb.append('0');
                }
                if (fi.spacePrefix) {
                    sb.append(' ');
                }
                // width and precision
                if (fi.width != 0 || fi.widthIsArg) {
                    sb.append(intString(w));
                }
                if (fi.precision != 0 || fi.precisionIsArg) {
                    sb.append('.').append(intString(p));
                }
            }
            sb.append(fi.conversion);
            conversions[fi.numArg - 1] = fi.conversion;
            i = fi.nextChar;
        }

        return RRuntime.toString(sb);
    }

    private static int intValue(Object o) {
        if (o instanceof Double) {
            return ((Double) o).intValue();
        } else if (o instanceof Integer) {
            return ((Integer) o).intValue();
        } else {
            throw fail("unexpected type");
        }
    }

    @SlowPath
    private static String intString(int x) {
        return Integer.toString(x);
    }

    private static char firstFormatChar(String fmt) {
        int pos = 0;
        char f;
        for (f = '\0'; f == '\0'; f = fmt.charAt(pos + 1)) {
            pos = fmt.indexOf('%', pos);
            if (pos == -1 || pos >= fmt.length() - 1) {
                return '\0';
            }
            if (fmt.charAt(pos + 1) == '%') {
                continue;
            }
            while (!Character.isLetter(fmt.charAt(pos + 1)) && pos < fmt.length() - 1) {
                ++pos;
            }
        }
        return f;
    }

    @SlowPath
    private static String stringFormat(String format, Object[] args) {
        return String.format((Locale) null, format, args);
    }

    private static void adjustValues(Object[] args, char[] conversions) {
        for (int i = 0; i < args.length; ++i) {
            if (conversions[i] == 0) {
                continue;
            }
            if (conversions[i] == 'd') {
                if (args[i] instanceof Double) {
                    args[i] = ((Double) args[i]).intValue();
                }
            }
        }
    }

    //
    // format info parsing
    //

    private static class FormatInfo {
        char conversion;
        int width;
        int precision;
        boolean adjustLeft;
        boolean alwaysSign;
        boolean spacePrefix;
        boolean padZero;
        boolean alternate;
        int numArg;
        boolean widthIsArg;
        boolean precisionIsArg;
        int nextChar;
        int argc;
    }

    //@formatter:off
    /**
     * The grammar understood by the format info extractor is as follows. Note that the
     * leading {@code %} has already been consumed in the caller and is not given in the
     * grammar.
     *
     * formatInfo        = '%'
     *                   | arg? (widthAndPrecision | '-' | '+' | ' ' | '0' | '#')* conversion
     * arg               = number '$'
     * widthAndPrecision = oneWidth
     *                   | number '.' number
     *                   | number '.' argWidth
     *                   | argWidth '.' number
     * oneWidth          = number
     *                   | argWidth
     * argWidth          = '*' arg?
     * conversion        = < one of the conversion characters, save % >
     */
    //@formatter:on
    private static FormatInfo extractFormatInfo(char[] cs, int i, int argc) {
        int j = i;
        FormatInfo fi = new FormatInfo();
        fi.argc = argc;
        char c = cs[j];
        // finished if % is the conversion
        if (c != '%') {
            // look ahead for a $ (indicates arg)
            if (isNumeric(c) && lookahead(cs, j, '$')) {
                fi.numArg = number(cs, j, fi);
                j = fi.nextChar + 1; // advance past $
                c = cs[j];
            }
            // now loop until the conversion is found
            while (!isConversion(c)) {
                switch (c) {
                    case '-':
                        fi.adjustLeft = true;
                        ++j;
                        break;
                    case '+':
                        fi.alwaysSign = true;
                        ++j;
                        break;
                    case ' ':
                        fi.spacePrefix = true;
                        ++j;
                        break;
                    case '0':
                        fi.padZero = true;
                        ++j;
                        break;
                    case '#':
                        fi.alternate = true;
                        ++j;
                        break;
                    case '*':
                        widthAndPrecision(cs, j, fi);
                        j = fi.nextChar;
                        break;
                    default:
                        // it can still be a widthAndPrecision if a number is given
                        if (isNumeric(c)) {
                            widthAndPrecision(cs, j, fi);
                            j = fi.nextChar;
                        } else {
                            throw fail("problem with format expression");
                        }
                }
                c = cs[j];
            }
        }
        fi.conversion = c;
        fi.nextChar = j + 1;
        if (fi.numArg == 0) {
            // no arg explicitly given, use args array
            fi.numArg = fi.argc++;
        }
        return fi;
    }

    private static void widthAndPrecision(char[] cs, int i, FormatInfo fi) {
        int j = i;
        oneWidth(cs, j, fi, true);
        j = fi.nextChar;
        if (cs[j] == '.') {
            oneWidth(cs, j + 1, fi, false);
        }
    }

    private static void oneWidth(char[] cs, int i, FormatInfo fi, boolean width) {
        int j = i;
        int n;
        if (isNumeric(cs[j])) {
            n = number(cs, j, fi);
            j = fi.nextChar;
        } else {
            assert cs[j] == '*';
            if (width) {
                fi.widthIsArg = true;
            } else {
                fi.precisionIsArg = true;
            }
            ++j;
            if (isNumeric(cs[j])) {
                n = number(cs, j, fi);
                j = fi.nextChar;
                assert cs[j] == '$';
                fi.nextChar = ++j;
            } else {
                n = fi.argc++;
            }
        }
        if (width) {
            fi.width = n;
        } else {
            fi.precision = n;
        }
        fi.nextChar = j;
    }

    private static boolean isConversion(char c) {
        return "aAdifeEgGosxX".indexOf(c) != -1;
    }

    private static boolean isNumeric(char c) {
        return c >= 48 && c <= 57;
    }

    private static int number(char[] cs, int i, FormatInfo fi) {
        int j = i;
        int num = cs[j++] - 48;
        while (isNumeric(cs[j])) {
            num = 10 * num + cs[j++];
        }
        fi.nextChar = j;
        return num;
    }

    private static boolean lookahead(char[] cs, int i, char c) {
        int j = i;
        while (!isConversion(cs[j])) {
            if (cs[j++] == c) {
                return true;
            }
        }
        return false;
    }

    protected boolean fmtLengthOne(RAbstractStringVector fmt) {
        return fmt.getLength() == 1;
    }

    @SlowPath
    private static IllegalStateException fail(String message) {
        throw new IllegalStateException(message);
    }

}
