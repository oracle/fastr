/*
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2013, 2023, Oracle and/or its affiliates
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
package com.oracle.truffle.r.runtime;

import java.math.BigInteger;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.Locale;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.r.common.SuppressFBWarnings;
import com.oracle.truffle.r.common.RVersionNumber;
import com.oracle.truffle.r.runtime.conn.RConnection;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.CharSXPWrapper;
import com.oracle.truffle.r.runtime.data.RBaseObject;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RComplexVector;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RExpression;
import com.oracle.truffle.r.runtime.data.RExternalPtr;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RRaw;
import com.oracle.truffle.r.runtime.data.RRawVector;
import com.oracle.truffle.r.runtime.data.RS4Object;
import com.oracle.truffle.r.runtime.data.RSequence;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.data.RTruffleObject;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector.RMaterializedVector;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.env.frame.FrameSlotChangeMonitor;

public class RRuntime {

    //@formatter:off
    // Parts of the welcome message originate from GNU R.
    public static final String WELCOME_MESSAGE =
        "R version " + RVersionNumber.FULL + " (FastR)\n" +
        RVersionNumber.COPYRIGHT +
        "\n" +
        "FastR is free software and comes with ABSOLUTELY NO WARRANTY.\n" +
        "You are welcome to redistribute it under certain conditions.\n" +
        "Type 'license()' or 'licence()' for distribution details.\n" +
        "\n" +
        "R is a collaborative project with many contributors.\n" +
        "Type 'contributors()' for more information.\n" +
        "\n" +
        "Type 'q()' to quit R.";

    //@formatter:on

    // used in DSL expressions:
    public static final boolean True = true;
    public static final boolean False = false;

    public static final String R_LANGUAGE_ID = "R";
    public static final String R_APP_MIME = "application/x-r";
    public static final String R_TEXT_MIME = "text/x-r";

    public static final String STRING_NA = new String("NA");
    public static final String STRING_NaN = "NaN";
    private static final String STRING_TRUE = "TRUE";
    private static final String STRING_FALSE = "FALSE";
    public static final int INT_NA = Integer.MIN_VALUE;
    public static final int INT_MIN_VALUE = Integer.MIN_VALUE + 1;
    public static final int INT_MAX_VALUE = Integer.MAX_VALUE;

    // R's NA is a special instance of IEEE's NaN
    private static final long NA_LONGBITS = 0x7ff00000000007a2L;
    public static final double DOUBLE_NA = Double.longBitsToDouble(NA_LONGBITS);
    public static final double EPSILON = Math.pow(2.0, -52.0);

    public static final double COMPLEX_NA_REAL_PART = DOUBLE_NA;
    public static final double COMPLEX_NA_IMAGINARY_PART = DOUBLE_NA;

    public static final byte LOGICAL_TRUE = 1;
    public static final byte LOGICAL_FALSE = 0;
    public static final byte LOGICAL_NA = -1;

    public static final RComplex COMPLEX_NA = RComplex.valueOf(RRuntime.COMPLEX_NA_REAL_PART, RRuntime.COMPLEX_NA_IMAGINARY_PART);
    public static final RComplex COMPLEX_ZERO = RComplex.valueOf(0.0, 0.0);
    public static final RComplex COMPLEX_REAL_ONE = RComplex.valueOf(1.0, 0.0);

    public static final String CLASS_SYMBOL = "name";
    public static final String CLASS_LANGUAGE = "call";
    public static final String CLASS_EXPRESSION = "expression";

    public static final String DEFAULT = "default";

    public static final String GENERIC_ATTR_KEY = "generic";

    public static final String PCKG_ATTR_KEY = "package";

    public static final String NAMES_ATTR_KEY = "names";
    public static final String NAMES_ATTR_EMPTY_VALUE = "";

    public static final String LEVELS_ATTR_KEY = "levels";

    public static final String NA_HEADER = "<NA>";

    public static final String DIM_ATTR_KEY = "dim";
    public static final String DIMNAMES_ATTR_KEY = "dimnames";
    public static final String DIMNAMES_LIST_ELEMENT_NAME_PREFIX = "$dimnames";

    public static final String CLASS_ATTR_KEY = "class";
    public static final String PREVIOUS_ATTR_KEY = "previous";
    public static final String ROWNAMES_ATTR_KEY = "row.names";

    public static final String FORMULA_CLASS = "formula";
    public static final String DOT_ENVIRONMENT = ".Environment";

    public static final String DOT_DATA = ".Data";
    public static final String DOT_XDATA = ".xData";

    public static final String DOT_S3_CLASS = ".S3Class";
    public static final String CLASS_DATA_FRAME = "data.frame";
    public static final String CLASS_FACTOR = "factor";
    public static final String ORDERED_ATTR_KEY = "ordered";

    public static final String CONN_ID_ATTR_KEY = "conn_id";

    public static final String RS3MethodsTable = ".__S3MethodsTable__.";

    public static final String R_DOT_GENERIC = ".Generic";

    public static final String R_DOT_METHOD = ".Method";

    public static final String R_DOT_CLASS = ".Class";

    public static final String R_DOT_GENERIC_CALL_ENV = ".GenericCallEnv";

    public static final String R_DOT_GENERIC_DEF_ENV = ".GenericDefEnv";

    public static final String R_DOT_GROUP = ".Group";

    public static final String RDOT = ".";

    public static final String SYSTEM_DATE_FORMAT = "EEE MMM dd HH:mm:ss yyyy";

    public static final String S_VIRTUAL = "virtual";

    public static final String S_PROTOTYPE = "prototype";

    public static final String S_CLASSNAME = "className";

    public static final String DOT_ALL_MTABLE = ".AllMTable";

    public static final String DOT_SIG_LENGTH = ".SigLength";

    public static final String DOT_SIG_ARGS = ".SigArgs";

    public static final String OP_NAMESPACE_SCOPE = ":::";

    public static final String OP_NAMESPACE_SCOPE_EXPORTED = "::";

    public static final RSymbol DEFERRED_DEFAULT_MARKER = RDataFactory.createSymbolInterned("__Deferred_Default_Marker__");

    public static final String R_TARGET = "target";
    public static final String R_DOT_TARGET = ".target";
    public static final String R_DEFINED = "defined";
    public static final String R_DOT_DEFINED = ".defined";
    public static final String R_NEXT_METHOD = "nextMethod";
    public static final String R_DOT_NEXT_METHOD = ".nextMethod";
    public static final String R_LOAD_METHOD_NAME = "loadMethod";
    public static final String R_DOT_METHODS = ".Methods";
    public static final String R_SOURCE = "source";
    public static final String COMMENT_ATTR_KEY = "comment";
    public static final String TSP_ATTR_KEY = "tsp";
    public static final String R_SRCREF = "srcref";
    public static final String R_WHOLE_SRCREF = "wholeSrcref";
    public static final String R_SRCFILE = "srcfile";

    public static final String NULL = "NULL";
    public static final RSymbol PSEUDO_NULL = RDataFactory.createSymbolInterned("\u0001NULL\u0001");
    public static final String UNBOUND = "UNBOUND";

    @CompilationFinal(dimensions = 1) private static final String[] rawStringCache = new String[256];
    @CompilationFinal(dimensions = 1) private static final String[] numberStringCache = new String[4096];
    private static final int MIN_CACHED_NUMBER = -numberStringCache.length / 2;
    private static final int MAX_CACHED_NUMBER = numberStringCache.length / 2 - 1;

    static {
        for (int i = 0; i < rawStringCache.length; i++) {
            rawStringCache[i] = Utils.intern(new String(new char[]{Character.forDigit((i & 0xF0) >> 4, 16), Character.forDigit(i & 0x0F, 16)}));
        }
        for (int i = 0; i < numberStringCache.length; i++) {
            numberStringCache[i] = String.valueOf(i + MIN_CACHED_NUMBER);
        }
    }

    /**
     * Create a {@link MaterializedFrame} for functions shared between different contexts.
     */
    public static MaterializedFrame createNewFrame(FrameDescriptor frameDescriptor) {
        return Truffle.getRuntime().createMaterializedFrame(RArguments.createUnitialized(), frameDescriptor);
    }

    /**
     * Create an {@link VirtualFrame} for a non-function environment, e.g., a package frame or the
     * global environment.
     */
    public static MaterializedFrame createNonFunctionFrame(String name) {
        FrameDescriptor frameDescriptor = FrameSlotChangeMonitor.createUninitializedFrameDescriptor(name);
        MaterializedFrame frame = Truffle.getRuntime().createMaterializedFrame(RArguments.createUnitialized(), frameDescriptor);
        FrameSlotChangeMonitor.initializeNonFunctionFrameDescriptor(frameDescriptor, frame);
        assert frame.getFrameDescriptor() == frameDescriptor;
        return frame;
    }

    public static FrameDescriptor createFrameDescriptorWithMetaData(String name) {
        return FrameSlotChangeMonitor.createFunctionFrameDescriptor(name);
    }

    /**
     * Since a distinguished NaN value is used for NA, checking for {@code isNaN} suffices.
     */
    public static boolean isNAorNaN(double d) {
        return Double.isNaN(d);
    }

    public static boolean isNull(Object obj) {
        return obj == RNull.instance;
    }

    public static boolean isFinite(double d) {
        return !isNAorNaN(d) && !Double.isInfinite(d);
    }

    public static boolean doubleIsInt(double d) {
        long longValue = (long) d;
        return longValue == d && ((int) longValue & 0xffffffff) == longValue;
    }

    public static byte asLogical(boolean b) {
        return b ? LOGICAL_TRUE : LOGICAL_FALSE;
    }

    public static boolean fromLogical(byte b) {
        return b == LOGICAL_TRUE;
    }

    public static boolean fromLogical(byte b, boolean naReplacement) {
        return naReplacement ? b != LOGICAL_FALSE : b == LOGICAL_TRUE;
    }

    // conversions from logical

    public static int logical2intNoCheck(byte value) {
        return value;
    }

    public static int logical2int(byte value) {
        return isNA(value) ? INT_NA : logical2intNoCheck(value);
    }

    public static double logical2doubleNoCheck(byte value) {
        return value;
    }

    public static double logical2double(byte value) {
        return isNA(value) ? DOUBLE_NA : logical2doubleNoCheck(value);
    }

    public static RComplex logical2complexNoCheck(byte value) {
        return RComplex.valueOf(value, 0);
    }

    public static RComplex logical2complex(byte value) {
        return isNA(value) ? RRuntime.COMPLEX_NA : logical2complexNoCheck(value);
    }

    public static String logicalToStringNoCheck(byte operand) {
        assert operand == LOGICAL_TRUE || operand == LOGICAL_FALSE : "operand: " + operand;
        return operand == LOGICAL_TRUE ? STRING_TRUE : STRING_FALSE;
    }

    public static String logicalToString(byte operand) {
        return isNA(operand) ? STRING_NA : logicalToStringNoCheck(operand);
    }

    // conversions from raw

    public static byte raw2logical(byte value) {
        return value == 0 ? LOGICAL_FALSE : LOGICAL_TRUE;
    }

    public static int raw2int(byte value) {
        return value & 0xFF;
    }

    public static double raw2double(byte value) {
        return int2double(value & 0xFF);
    }

    public static RComplex raw2complex(byte r) {
        return int2complex(raw2int(r));
    }

    public static String rawToHexString(byte operand) {
        return rawStringCache[raw2int(operand)];
    }

    // conversions from string

    @TruffleBoundary
    public static int parseInt(String s) {
        int length = s.length();
        if (length == 0) {
            throw new NumberFormatException();
        }
        long value = 0;
        if (s.charAt(0) == '-') {
            if (length == 1) {
                throw new NumberFormatException();
            }
            int pos = 1;
            while (pos < length) {
                char ch = s.charAt(pos++);
                if (ch < '0' || ch > '9') {
                    throw new NumberFormatException();
                }
                value = value * 10 + (ch - '0');
                if (value > (Integer.MAX_VALUE + 1L)) {
                    return INT_NA;
                }
            }
            return (int) -value;
        } else {
            int pos = 0;
            while (pos < length) {
                char ch = s.charAt(pos++);
                if (ch < '0' || ch > '9') {
                    throw new NumberFormatException();
                }
                value = value * 10 + (ch - '0');
                if (value > Integer.MAX_VALUE) {
                    return INT_NA;
                }
            }
            return (int) value;
        }
    }

    @TruffleBoundary
    public static int parseIntWithNA(String s) {
        int length = s.length();
        if (length == 0) {
            return INT_NA;
        }
        long value = 0;
        if (s.charAt(0) == '-') {
            if (length == 1) {
                return INT_NA;
            }
            int pos = 1;
            while (pos < length) {
                char ch = s.charAt(pos++);
                if (ch < '0' || ch > '9') {
                    return INT_NA;
                }
                value = value * 10 + (ch - '0');
                if (value > (Integer.MAX_VALUE + 1L)) {
                    return INT_NA;
                }
            }
            return (int) -value;
        } else {
            int pos = 0;
            while (pos < length) {
                char ch = s.charAt(pos++);
                if (ch < '0' || ch > '9') {
                    return INT_NA;
                }
                value = value * 10 + (ch - '0');
                if (value > Integer.MAX_VALUE) {
                    return INT_NA;
                }
            }
            return (int) value;
        }
    }

    @TruffleBoundary
    public static int string2intNoCheck(String s, boolean exceptionOnFail) {
        // FIXME use R rules
        int result;
        try {
            result = Integer.decode(Utils.trimLeadingZeros(s.trim()));  // decode supports hex
                                                                        // constants
        } catch (NumberFormatException e) {
            if (exceptionOnFail) {
                throw e;
            }
            return INT_NA;
        }

        if (result == INT_NA && exceptionOnFail) {
            throw new NumberFormatException();
        }
        return result;

    }

    @TruffleBoundary
    public static int string2intNoCheck(String s) {
        return string2intNoCheck(s, false);
    }

    @TruffleBoundary
    public static int string2int(String s) {
        return isNA(s) ? INT_NA : string2intNoCheck(s);
    }

    @TruffleBoundary
    public static double string2doubleNoCheck(String v, boolean exceptionOnFail) {
        return string2doubleNoCheck(v, exceptionOnFail, false);
    }

    @TruffleBoundary
    public static double string2doubleNoCheck(String v, boolean exceptionOnFail, boolean useLocale) {
        // FIXME use R rules
        String trimmed = v.trim();
        if ("Inf".equals(trimmed) || "+Inf".equals(trimmed)) {
            return Double.POSITIVE_INFINITY;
        } else if ("-Inf".equals(trimmed)) {
            return Double.NEGATIVE_INFINITY;
        } else if ("NaN".equals(trimmed) || "+NaN".equals(trimmed) || "-NaN".equals(trimmed)) {
            return Double.NaN;
        } else if ("NA_real_".equals(trimmed)) {
            return DOUBLE_NA;
        }
        try {
            if (useLocale) {
                ParsePosition ppos = new ParsePosition(0);
                if (trimmed.startsWith("+")) {
                    trimmed = trimmed.substring(1);
                }
                Locale numLocale = RContext.getInstance().stateRLocale.getLocale(RLocale.NUMERIC);
                Number val = NumberFormat.getInstance(numLocale).parse(trimmed, ppos);
                if (ppos.getIndex() < trimmed.length()) {
                    throw new NumberFormatException("Unparseable number: \"" + trimmed + "\". Failed at index " + ppos.getErrorIndex());
                }
                return val.doubleValue();
            } else {
                return Double.parseDouble(trimmed);
            }
        } catch (NumberFormatException e) {
            if (hasHexPrefix(v)) {
                switch (v.charAt(0)) {
                    case '-':
                        return -1 * new BigInteger(v.substring(3, v.length()), 16).doubleValue();
                    case '+':
                        return new BigInteger(v.substring(3, v.length()), 16).doubleValue();
                    default:
                        assert v.charAt(0) == '0';
                        return new BigInteger(v.substring(2, v.length()), 16).doubleValue();
                }
            }
            if (exceptionOnFail) {
                throw e;
            }
        }
        return DOUBLE_NA;
    }

    public static boolean hasHexPrefix(String s) {
        return s.startsWith("0x") || s.startsWith("-0x") || s.startsWith("+0x");
    }

    @TruffleBoundary
    public static double string2doubleNoCheck(String v) {
        return string2doubleNoCheck(v, false);
    }

    @TruffleBoundary
    public static double string2double(String v) {
        if (isNA(v)) {
            return DOUBLE_NA;
        } else {
            return string2doubleNoCheck(v);
        }
    }

    public static byte string2logicalNoCheck(String s, boolean exceptionOnFail) {
        switch (s) {
            case "TRUE":
            case "T":
            case "True":
            case "true":
                return LOGICAL_TRUE;
            case "FALSE":
            case "F":
            case "False":
            case "false":
                return LOGICAL_FALSE;
            default:
                if (exceptionOnFail) {
                    throw new NumberFormatException();
                }
                return LOGICAL_NA;
        }
    }

    public static byte string2logicalNoCheck(String s) {
        return string2logicalNoCheck(s, false);
    }

    public static byte string2logical(String s) {
        return isNA(s) ? LOGICAL_NA : string2logicalNoCheck(s);
    }

    @TruffleBoundary
    public static RComplex string2complexNoCheck(String v) {
        double doubleValue = string2doubleNoCheck(v);
        if (!RRuntime.isNA(doubleValue)) {
            return RComplex.valueOf(doubleValue, 0.0);
        } else {
            try {
                int startIdx = 0;
                char firstChar = v.charAt(0);
                boolean negativeReal = firstChar == '-';
                if (firstChar == '+' || negativeReal) {
                    startIdx++;
                }

                int plusIdx = v.indexOf("+", startIdx);
                int minusIdx = v.indexOf("-", startIdx);
                int iIdx = v.indexOf("i", startIdx);
                int signIdx = getSignIdx(plusIdx, minusIdx);
                boolean negativeImaginary = minusIdx > 0;

                double realPart = Double.parseDouble(v.substring(startIdx, signIdx));
                double imaginaryPart = Double.parseDouble(v.substring(signIdx + 1, iIdx));

                return RComplex.valueOf(realPart * (negativeReal ? -1 : 1), imaginaryPart * (negativeImaginary ? -1 : 1));
            } catch (NumberFormatException ex) {
                return RRuntime.COMPLEX_NA;
            }
        }
    }

    @TruffleBoundary
    public static RComplex string2complex(String v) {
        return isNA(v) ? RRuntime.COMPLEX_NA : string2complexNoCheck(v);
    }

    @TruffleBoundary
    public static RRaw string2raw(String v) {
        if (v.length() == 2 && (Utils.isIsoLatinDigit(v.charAt(0)) || Utils.isRomanLetter(v.charAt(0))) && (Utils.isIsoLatinDigit(v.charAt(1)) || Utils.isRomanLetter(v.charAt(1)))) {
            return RRaw.valueOf(Byte.parseByte(v, 16));
        } else {
            return RRaw.valueOf((byte) 0);
        }
    }

    // conversions from int

    public static double int2doubleNoCheck(int i) {
        return i;
    }

    public static double int2double(int i) {
        return isNA(i) ? DOUBLE_NA : int2doubleNoCheck(i);
    }

    public static byte int2logicalNoCheck(int i) {
        return i == 0 ? LOGICAL_FALSE : LOGICAL_TRUE;
    }

    public static byte int2logical(int i) {
        return isNA(i) ? LOGICAL_NA : int2logicalNoCheck(i);
    }

    public static RComplex int2complexNoCheck(int i) {
        return RComplex.valueOf(i, 0);
    }

    public static RComplex int2complex(int i) {
        return isNA(i) ? RRuntime.COMPLEX_NA : int2complexNoCheck(i);
    }

    public static String intToStringNoCheck(int operand) {
        if (operand >= MIN_CACHED_NUMBER && operand <= MAX_CACHED_NUMBER) {
            return numberStringCache[operand - MIN_CACHED_NUMBER];
        } else {
            return intToStringInternal(operand);
        }
    }

    @TruffleBoundary
    private static String intToStringInternal(int operand) {
        return String.valueOf(operand);
    }

    public static boolean isCachedNumberString(int value) {
        return value >= MIN_CACHED_NUMBER && value <= MAX_CACHED_NUMBER;
    }

    public static String getCachedNumberString(int value) {
        return numberStringCache[value - MIN_CACHED_NUMBER];
    }

    public static String intToString(int operand) {
        return isNA(operand) ? STRING_NA : intToStringNoCheck(operand);
    }

    public static int int2rawIntValue(int i) {
        return isNA(i) ? 0 : i & 0xFF;
    }

    public static RRaw int2raw(int i) {
        return RRaw.valueOf((byte) int2rawIntValue(i));
    }

    // conversions from double

    public static int double2intNoCheck(double d) {
        return (int) d;
    }

    public static int double2int(double d) {
        return isNA(d) ? INT_NA : double2intNoCheck(d);
    }

    public static byte double2logicalNoCheck(double d) {
        return Double.isNaN(d) ? LOGICAL_NA : d == 0.0 ? LOGICAL_FALSE : LOGICAL_TRUE;
    }

    public static byte double2logical(double d) {
        return isNA(d) ? LOGICAL_NA : double2logicalNoCheck(d);
    }

    public static RComplex double2complexNoCheck(double d) {
        return RComplex.valueOf(d, 0);
    }

    public static RComplex double2complex(double d) {
        return isNAorNaN(d) ? RRuntime.COMPLEX_NA : double2complexNoCheck(d);
    }

    public static int double2rawIntValue(double operand) {
        return isNA(operand) ? 0 : ((int) operand) & 0xFF;
    }

    public static RRaw double2raw(double operand) {
        return RRaw.valueOf((byte) double2rawIntValue(operand));
    }

    // conversions from complex

    public static byte complex2logicalNoCheck(RComplex c) {
        return c.getRealPart() == 0.0 && c.getImaginaryPart() == 0.0 ? LOGICAL_FALSE : LOGICAL_TRUE;
    }

    public static byte complex2logical(RComplex c) {
        return isNA(c) ? LOGICAL_NA : complex2logicalNoCheck(c);
    }

    public static int complex2intNoCheck(RComplex c) {
        return double2intNoCheck(c.getRealPart());
    }

    public static int complex2int(RComplex c) {
        return isNA(c) ? LOGICAL_NA : complex2intNoCheck(c);
    }

    public static double complex2doubleNoCheck(RComplex c) {
        return double2intNoCheck(c.getRealPart());
    }

    public static double complex2double(RComplex c) {
        return isNA(c) ? LOGICAL_NA : complex2doubleNoCheck(c);
    }

    public static int complex2rawIntValue(RComplex c) {
        return isNA(c) ? 0 : ((int) c.getRealPart() & 0xFF);
    }

    public static RRaw complex2raw(RComplex c) {
        return RRaw.valueOf((byte) complex2rawIntValue(c));
    }

    private static int getSignIdx(int plusIdx, int minusIdx) throws NumberFormatException {
        if (plusIdx < 0) {
            if (minusIdx < 0) {
                throw new NumberFormatException();
            }
            return minusIdx;
        } else {
            if (minusIdx < 0) {
                return plusIdx;
            }
            throw new NumberFormatException();
        }
    }

    @TruffleBoundary
    public static String toString(Object object) {
        if (object instanceof Integer) {
            return intToString((int) object);
        } else if (object instanceof Double) {
            return Double.toString((double) object);
        } else if (object instanceof Byte) {
            return logicalToString((byte) object);
        }
        return String.valueOf(object);
    }

    @SuppressFBWarnings(value = "ES_COMPARING_PARAMETER_STRING_WITH_EQ", justification = "string NA is intended to be treated as an identity")
    public static boolean isNA(String value) {
        return value == STRING_NA;
    }

    public static boolean isNA(byte value) {
        return value == LOGICAL_NA;
    }

    public static boolean isNA(int value) {
        return value == INT_NA;
    }

    public static boolean isNA(double value) {
        return Double.doubleToRawLongBits(value) == NA_LONGBITS;
    }

    public static boolean isNA(RComplex value) {
        return isComplexNA(value.getRealPart(), value.getImaginaryPart());
    }

    public static boolean isComplexNA(double realPart, double imagPart) {
        return isNA(realPart) || isNA(imagPart);
    }

    public static boolean isNA(double real, double imag) {
        return isNA(real) || isNA(imag);
    }

    public static String escapeString(String value, boolean encodeNonASCII, boolean quote) {
        return escapeString(value, encodeNonASCII, quote, null, null);
    }

    @TruffleBoundary
    public static String escapeString(String value, boolean encodeNonASCII, boolean quote, String leftQuote, String rightQuote) {
        if (isNA(value)) {
            return STRING_NA;
        }
        StringBuilder str = new StringBuilder(value.length() + 2);
        if (quote) {
            str.append(leftQuote == null ? '\"' : leftQuote);
        }
        int offset = 0;
        while (offset < value.length()) {
            int codepoint = value.codePointAt(offset);
            switch (codepoint) {
                case '\n':
                    str.append("\\n");
                    break;
                case '\r':
                    str.append("\\r");
                    break;
                case '\t':
                    str.append("\\t");
                    break;
                case '\b':
                    str.append("\\b");
                    break;
                case 7:
                    str.append("\\a");
                    break;
                case '\f':
                    str.append("\\f");
                    break;
                case 11:
                    str.append("\\v");
                    break;
                case '\\':
                    str.append("\\\\");
                    break;
                case '"':
                    if (quote) {
                        str.append("\\\"");
                    } else {
                        str.append('\"');
                    }
                    break;
                default:
                    if (codepoint < 32 || codepoint == 0x7f) {
                        str.append("\\").append(codepoint >>> 6).append((codepoint >>> 3) & 0x7).append(codepoint & 0x7);
                    } else if (encodeNonASCII && codepoint > 0x7f && codepoint <= 0xff) {
                        str.append("\\x").append(Integer.toHexString(codepoint));
                    } else if (codepoint > 64967) { // determined by experimentation
                        if (codepoint < 0x10000) {
                            str.append("\\u").append(String.format("%04x", codepoint));
                        } else {
                            str.append("\\U").append(String.format("%08x", codepoint));
                        }
                    } else {
                        str.appendCodePoint(codepoint);
                    }
                    break;
            }
            offset += Character.charCount(codepoint);
        }
        if (quote) {
            str.append(rightQuote == null ? '\"' : rightQuote);
        }
        return str.toString();
    }

    public static FrameSlotKind getSlotKind(Object value) {
        assert value != null;
        if (value instanceof Byte) {
            return FrameSlotKind.Byte;
        } else if (value instanceof Integer) {
            return FrameSlotKind.Int;
        } else if (value instanceof Double) {
            return FrameSlotKind.Double;
        } else {
            return FrameSlotKind.Object;
        }
    }

    /**
     * Checks and converts an object into a String. TODO rename as checkString
     */
    public static String asString(Object obj) {
        if (obj instanceof String) {
            return (String) obj;
        } else if (obj instanceof RStringVector) {
            return ((RStringVector) obj).getDataAt(0);
        } else {
            return null;
        }
    }

    /**
     * Same {@call asString} but checks if vector is of length one.
     */
    public static String asStringLengthOne(Object obj) {
        if (obj instanceof String) {
            return (String) obj;
        } else if (obj instanceof RStringVector && ((RStringVector) obj).getLength() == 1) {
            return ((RStringVector) obj).getDataAt(0);
        } else {
            return null;
        }
    }

    /**
     * Java equivalent of GnuR asLogical for use outside Truffle boundary. TODO support for warnings
     */
    public static byte asLogicalObject(Object obj) {
        if (obj instanceof Integer) {
            return int2logical((int) obj);
        } else if (obj instanceof Double) {
            return double2logical((double) obj);
        } else if (obj instanceof Byte) {
            return (byte) obj;
        } else if (obj instanceof String) {
            return string2logical((String) obj);
        } else if (obj instanceof RIntVector) {
            return int2logical(((RIntVector) obj).getDataAt(0));
        } else if (obj instanceof RDoubleVector) {
            return double2logical(((RDoubleVector) obj).getDataAt(0));
        } else if (obj instanceof RLogicalVector) {
            return ((RLogicalVector) obj).getDataAt(0);
        } else if (obj instanceof RComplexVector) {
            return complex2logical(((RComplexVector) obj).getDataAt(0));
        } else if (obj instanceof RStringVector) {
            return string2logical(((RStringVector) obj).getDataAt(0));
        } else {
            return LOGICAL_NA;
        }
    }

    public static boolean isValidLogical(byte value) {
        return value == RRuntime.LOGICAL_NA || value == RRuntime.LOGICAL_FALSE || value == RRuntime.LOGICAL_TRUE;
    }

    /**
     * Java equivalent of GnuR asInteger for use outside Truffle boundary. TODO support for warnings
     */
    public static int asInteger(Object obj) {
        if (obj instanceof Integer) {
            return (int) obj;
        } else if (obj instanceof Double) {
            return double2int((double) obj);
        } else if (obj instanceof Byte) {
            return logical2int((byte) obj);
        } else if (obj instanceof String) {
            return string2int((String) obj);
        } else if (obj instanceof RIntVector) {
            return ((RIntVector) obj).getDataAt(0);
        } else if (obj instanceof RDoubleVector) {
            return double2int(((RDoubleVector) obj).getDataAt(0));
        } else if (obj instanceof RLogicalVector) {
            return logical2int(((RLogicalVector) obj).getDataAt(0));
        } else if (obj instanceof RComplexVector) {
            return complex2int(((RComplexVector) obj).getDataAt(0));
        } else if (obj instanceof RStringVector) {
            return string2int(((RStringVector) obj).getDataAt(0));
        } else {
            return INT_NA;
        }
    }

    public static boolean checkType(Object obj, RType type) {
        switch (type) {
            case Any:
                return true;
            case Null:
                return obj == RNull.instance;
            case Raw:
                return obj instanceof RRawVector;
            case Logical:
                return obj instanceof Byte || obj instanceof RLogicalVector;
            case Integer:
            case Double:
            case Numeric:
                // Note: e.g. foo <- 3.4; exists("foo", mode = "integer") really gives TRUE
                return obj instanceof Integer || obj instanceof Double ||
                                obj instanceof RIntVector || obj instanceof RDoubleVector;
            case Complex:
                return obj instanceof RComplexVector;
            case Character:
                return obj instanceof String || obj instanceof RStringVector;
            case List:
                return obj instanceof RAbstractListVector;
            case Expression:
                return obj instanceof RExpression;
            case Special:
            case Builtin:
            case Closure:
            case Function:
                return (obj instanceof RFunction) || (obj instanceof TruffleObject && !(obj instanceof RBaseObject));
            case Symbol:
                return obj instanceof RSymbol;
            case Environment:
                return obj instanceof REnvironment;
            case PairList:
                return obj instanceof RPairList && !((RPairList) obj).isLanguage();
            case Language:
                return obj instanceof RPairList && ((RPairList) obj).isLanguage();
            case S4Object:
                return obj instanceof RS4Object;
            case Connection:
                return obj instanceof RConnection;
            case Char:
                return obj instanceof CharSXPWrapper;
            case ExternalPtr:
                return obj instanceof RExternalPtr;
            default:
                return false;
        }
    }

    /**
     * Runtime variant of DSL support for converting scalar values to {@link RAbstractVector}.
     */
    public static Object asAbstractVector(Object obj) {
        if (obj instanceof Integer) {
            return RDataFactory.createIntVectorFromScalar((int) obj);
        } else if (obj instanceof Double) {
            return RDataFactory.createDoubleVectorFromScalar((double) obj);
        } else if (obj instanceof Byte) {
            return RDataFactory.createLogicalVectorFromScalar((byte) obj);
        } else if (obj instanceof String) {
            return RDataFactory.createStringVectorFromScalar((String) obj);
        } else if (obj instanceof RComplex) {
            return RDataFactory.createComplexVectorFromScalar((RComplex) obj);
        } else if (obj instanceof RRaw) {
            return RDataFactory.createRawVectorFromScalar((RRaw) obj);
        } else {
            return obj;
        }
    }

    public static boolean isForeignObject(TruffleObject obj) {
        return !(obj instanceof RTruffleObject);
    }

    public static boolean isForeignObject(Object obj) {
        return obj instanceof TruffleObject && !(obj instanceof RTruffleObject);
    }

    public static int getForeignArraySize(Object object, InteropLibrary interop) {
        assert interop.hasArrayElements(object);
        long size;
        try {
            size = interop.getArraySize(object);
        } catch (UnsupportedMessageException ex) {
            throw RInternalError.shouldNotReachHere();
        }
        return interopArrayIndexToInt(size, object);
    }

    public static int interopArrayIndexToInt(long size, Object object) throws RError {
        if (size <= Integer.MAX_VALUE) {
            return (int) size;
        }
        CompilerDirectives.transferToInterpreter();
        throw RError.error(RError.SHOW_CALLER, RError.Message.GENERIC, "the foreign array " + object + " size " + size + " does not fit into a java integer");
    }

    public static boolean isMaterializedVector(Object o) {
        if (o instanceof RMaterializedVector) {
            return true;
        }
        if (o instanceof RAbstractVector) {
            return ((RAbstractVector) o).isMaterialized();
        }
        return false;
    }

    public static boolean hasVectorData(Object o) {
        // TODO: for the time beeeing, until all vectors switch to RVectorData
        return o instanceof RIntVector || o instanceof RDoubleVector || o instanceof RRawVector || o instanceof RLogicalVector || o instanceof RComplexVector || o instanceof RStringVector;
    }

    public static boolean isSequence(Object o) {
        if (o instanceof RSequence) {
            return true;
        }
        if (o instanceof RAbstractVector) {
            return ((RAbstractVector) o).isSequence();
        }
        return false;
    }

    /**
     * Normalize -0.0 to +0.0, mainly used for printing.
     */
    public static double normalizeZero(double value) {
        return value == 0.0 ? 0.0 : value;
    }

    public static int nrows(Object x) {
        if (x instanceof RAbstractContainer) {
            RAbstractContainer xa = (RAbstractContainer) x;
            if (hasDims(xa)) {
                return getDims(xa)[0];
            } else {
                return xa.getLength();
            }
        } else {
            throw RError.error(RError.SHOW_CALLER2, RError.Message.OBJECT_NOT_MATRIX);
        }
    }

    public static int ncols(Object x) {
        if (x instanceof RAbstractContainer) {
            RAbstractContainer xa = (RAbstractContainer) x;
            if (hasDims(xa)) {
                int[] dims = getDims(xa);
                if (dims.length >= 2) {
                    return dims[1];
                } else {
                    return 1;
                }
            } else {
                return 1;
            }
        } else {
            throw RError.error(RError.SHOW_CALLER2, RError.Message.OBJECT_NOT_MATRIX);
        }
    }

    @TruffleBoundary
    private static int[] getDims(RAbstractContainer xa) {
        return xa.getDimensions();
    }

    @TruffleBoundary
    private static boolean hasDims(RAbstractContainer xa) {
        return xa.hasDimensions();
    }

    public static boolean isS4Object(Object o) {
        return o instanceof RBaseObject && ((RBaseObject) o).isS4();
    }

    public static String getRTypeName(Object arg) {
        CompilerAsserts.neverPartOfCompilation();
        return isForeignObject(arg) ? "polyglot.value" : ((RBaseObject) asAbstractVector(arg)).getRType().getName();
    }
}
