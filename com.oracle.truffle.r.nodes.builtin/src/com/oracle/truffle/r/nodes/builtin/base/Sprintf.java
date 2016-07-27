/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import java.util.Locale;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

@RBuiltin(name = "sprintf", kind = INTERNAL, parameterNames = {"fmt", "..."}, behavior = PURE)
public abstract class Sprintf extends RBuiltinNode {

    public abstract Object executeObject(String fmt, Object args);

    @Child private Sprintf sprintfRecursive;

    @Specialization
    protected String sprintf(String fmt, @SuppressWarnings("unused") RMissing x) {
        return fmt;
    }

    @Specialization(guards = "fmtLengthOne(fmt)")
    @TruffleBoundary
    protected String sprintf(RAbstractStringVector fmt, RMissing x) {
        return sprintf(fmt.getDataAt(0), x);
    }

    @Specialization
    @TruffleBoundary
    protected String sprintf(String fmt, int x) {
        return format(fmt, x);
    }

    @Specialization(guards = "fmtLengthOne(fmt)")
    @TruffleBoundary
    protected String sprintf(RAbstractStringVector fmt, int x) {
        return sprintf(fmt.getDataAt(0), x);
    }

    @Specialization(guards = "fmtLengthOne(fmt)")
    @TruffleBoundary
    protected String sprintf(RAbstractStringVector fmt, byte x) {
        return format(fmt.getDataAt(0), x);
    }

    @Specialization
    @TruffleBoundary
    protected RStringVector sprintf(String fmt, RAbstractIntVector x) {
        String[] r = new String[x.getLength()];
        for (int k = 0; k < r.length; k++) {
            r[k] = format(fmt, x.getDataAt(k));
        }
        return RDataFactory.createStringVector(r, RDataFactory.COMPLETE_VECTOR);
    }

    @Specialization(guards = "fmtLengthOne(fmt)")
    @TruffleBoundary
    protected RStringVector sprintf(RAbstractStringVector fmt, RAbstractIntVector x) {
        return sprintf(fmt.getDataAt(0), x);
    }

    @Specialization
    @TruffleBoundary
    protected String sprintf(String fmt, double x) {
        char f = Character.toLowerCase(firstFormatChar(fmt));
        if (f == 'x' || f == 'd') {
            if (Math.floor(x) == x) {
                return format(fmt, (long) x);
            }
            throw RError.error(this, RError.Message.INVALID_FORMAT_DOUBLE, fmt);
        }
        return format(fmt, x);
    }

    @Specialization(guards = "fmtLengthOne(fmt)")
    @TruffleBoundary
    protected String sprintf(RAbstractStringVector fmt, double x) {
        return sprintf(fmt.getDataAt(0), x);
    }

    @Specialization
    @TruffleBoundary
    protected RStringVector sprintf(String fmt, RAbstractDoubleVector x) {
        String[] r = new String[x.getLength()];
        for (int k = 0; k < r.length; k++) {
            r[k] = sprintf(fmt, x.getDataAt(k));
        }
        return RDataFactory.createStringVector(r, RDataFactory.COMPLETE_VECTOR);
    }

    @Specialization(guards = "fmtLengthOne(fmt)")
    @TruffleBoundary
    protected RStringVector sprintf(RAbstractStringVector fmt, RAbstractDoubleVector x) {
        return sprintf(fmt.getDataAt(0), x);
    }

    @Specialization
    @TruffleBoundary
    protected String sprintf(String fmt, String x) {
        return format(fmt, x);
    }

    @Specialization(guards = "fmtLengthOne(fmt)")
    @TruffleBoundary
    protected String sprintf(RAbstractStringVector fmt, String x) {
        return sprintf(fmt.getDataAt(0), x);
    }

    @Specialization
    @TruffleBoundary
    protected RStringVector sprintf(String fmt, RAbstractStringVector x) {
        String[] r = new String[x.getLength()];
        for (int k = 0; k < r.length; k++) {
            r[k] = format(fmt, x.getDataAt(k));
        }
        return RDataFactory.createStringVector(r, RDataFactory.COMPLETE_VECTOR);
    }

    @Specialization(guards = "fmtLengthOne(fmt)")
    @TruffleBoundary
    protected RStringVector sprintf(RAbstractStringVector fmt, RAbstractStringVector x) {
        return sprintf(fmt.getDataAt(0), x);
    }

    private static int maxLengthAndConvertToScalar(Object[] values) {
        int length = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i] instanceof RAbstractVector) {
                int vecLength = ((RAbstractVector) values[i]).getLength();
                if (vecLength == 0) {
                    // result will be empty character vector in this case, as in:
                    // sprintf("%d %d", as.integer(c(7,42)), integer())
                    return 0;
                } else {
                    if (vecLength == 1) {
                        values[i] = ((RAbstractVector) values[i]).getDataAtAsObject(0);
                    }
                    length = Math.max(vecLength, length);
                }
            } else {
                length = Math.max(1, length);
            }
        }
        return length;
    }

    private static Object[] createSprintfArgs(Object[] values, int index, int maxLength) {
        Object[] sprintfArgs = new Object[values.length];
        for (int i = 0; i < sprintfArgs.length; i++) {
            if (values[i] instanceof RAbstractVector) {
                sprintfArgs[i] = ((RAbstractVector) values[i]).getDataAtAsObject(index % maxLength);
            } else {
                sprintfArgs[i] = values[i];
            }
        }
        return sprintfArgs;
    }

    @Specialization(guards = "!oneElement(args)")
    @TruffleBoundary
    protected RStringVector sprintf(String fmt, RArgsValuesAndNames args) {
        Object[] values = args.getArguments();
        int maxLength = maxLengthAndConvertToScalar(values);
        if (maxLength == 0) {
            if (values.length > 0) {
                return RDataFactory.createEmptyStringVector();
            } else {
                return RDataFactory.createStringVector(fmt);
            }
        } else {
            String[] r = new String[maxLength];
            for (int k = 0; k < r.length; k++) {
                Object[] sprintfArgs = createSprintfArgs(values, k, maxLength);
                r[k] = format(fmt, sprintfArgs);
            }
            return RDataFactory.createStringVector(r, RDataFactory.COMPLETE_VECTOR);

        }
    }

    @Specialization(guards = "oneElement(args)")
    protected Object sprintfOneElement(String fmt, RArgsValuesAndNames args) {
        if (sprintfRecursive == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            sprintfRecursive = insert(SprintfNodeGen.create(null));
        }
        return sprintfRecursive.executeObject(fmt, args.getArgument(0));
    }

    @Specialization(guards = {"!oneElement(args)"})
    @TruffleBoundary
    protected RStringVector sprintf(RAbstractStringVector fmt, RArgsValuesAndNames args) {
        if (fmt.getLength() == 0) {
            return RDataFactory.createEmptyStringVector();
        } else {
            String[] data = new String[fmt.getLength()];
            for (int i = 0; i < data.length; i++) {
                RStringVector formatted = sprintf(fmt.getDataAt(i), args);
                assert formatted.getLength() > 0;
                data[i] = formatted.getDataAt(args.getLength() == 0 ? 0 : i % Math.min(args.getLength(), formatted.getLength()));
            }
            return RDataFactory.createStringVector(data, RDataFactory.COMPLETE_VECTOR);
        }
    }

    @Specialization(guards = {"oneElement(args)", "fmtLengthOne(fmt)"})
    protected Object sprintfOneElement(RAbstractStringVector fmt, RArgsValuesAndNames args) {
        return sprintfOneElement(fmt.getDataAt(0), args);
    }

    @Specialization(guards = {"oneElement(args)", "!fmtLengthOne(fmt)"})
    protected Object sprintf2(RAbstractStringVector fmt, RArgsValuesAndNames args) {
        if (fmt.getLength() == 0) {
            return RDataFactory.createEmptyStringVector();
        } else {
            String[] data = new String[fmt.getLength()];
            for (int i = 0; i < data.length; i++) {
                Object formattedObj = sprintfOneElement(fmt.getDataAt(i), args);
                if (formattedObj instanceof String) {
                    data[i] = (String) formattedObj;
                } else {
                    RStringVector formatted = (RStringVector) formattedObj;
                    assert formatted.getLength() > 0;
                    data[i] = formatted.getDataAt(i % formatted.getLength());
                }
            }
            return RDataFactory.createStringVector(data, RDataFactory.COMPLETE_VECTOR);
        }
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
                conversions[fi.numArg - 1] = fi.conversion;
            }
            sb.append(fi.conversion);
            i = fi.nextChar;
        }

        return sb.toString();
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

    @TruffleBoundary
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
                pos++;
            }
        }
        return f;
    }

    @TruffleBoundary
    private static String stringFormat(String format, Object[] args) {
        return String.format((Locale) null, format, args);
    }

    private static void adjustValues(Object[] args, char[] conversions) {
        for (int i = 0; i < args.length; i++) {
            if (conversions[i] == 0) {
                continue;
            }
            if (conversions[i] == 'd') {
                if (args[i] instanceof Double) {
                    args[i] = ((Double) args[i]).intValue();
                }
            }
            if (conversions[i] == 's') {
                if (args[i] instanceof Byte) {
                    args[i] = RRuntime.logicalToString((Byte) args[i]);
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
                        j++;
                        break;
                    case '+':
                        fi.alwaysSign = true;
                        j++;
                        break;
                    case ' ':
                        fi.spacePrefix = true;
                        j++;
                        break;
                    case '0':
                        fi.padZero = true;
                        j++;
                        break;
                    case '#':
                        fi.alternate = true;
                        j++;
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
                        } else if (c == '.') {
                            // apparently precision can be specified without width as well
                            oneWidth(cs, j + 1, fi, false);
                            j = fi.nextChar;
                        } else {
                            throw fail("problem with format expression");
                        }
                }
                c = cs[j];
            }
        }
        fi.conversion = c;
        if (c == 'i') {
            // they seem to be equivalent but 'i' is not handled correctly by the java formatter
            fi.conversion = 'd';
        }
        fi.nextChar = j + 1;
        if (fi.numArg == 0 && c != '%') {
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
            j++;
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

    protected boolean oneElement(RArgsValuesAndNames args) {
        return args.getLength() == 1;
    }

    @TruffleBoundary
    private static IllegalStateException fail(String message) {
        throw new IllegalStateException(message);
    }
}
