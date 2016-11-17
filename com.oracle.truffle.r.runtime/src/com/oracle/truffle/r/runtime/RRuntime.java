/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.runtime;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDouble;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RInteger;
import com.oracle.truffle.r.runtime.data.RLogical;
import com.oracle.truffle.r.runtime.data.RRaw;
import com.oracle.truffle.r.runtime.data.RString;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.data.RTypedValue;
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.env.frame.FrameSlotChangeMonitor;

public class RRuntime {

    //@formatter:off
    // Parts of the welcome message originate from GNU R.
    public static final String WELCOME_MESSAGE =
        "FastR version " + RVersionNumber.FULL + "\n" +
        "Copyright (c) 2013-16, Oracle and/or its affiliates\n" +
        "Copyright (c) 1995-2016, The R Core Team\n" +
        "Copyright (c) 2016 The R Foundation for Statistical Computing\n" +
        "Copyright (c) 2012-4 Purdue University\n" +
        "Copyright (c) 1997-2002, Makoto Matsumoto and Takuji Nishimura\n" +
        "All rights reserved.\n" +
        "\n" +
        "FastR is free software and comes with ABSOLUTELY NO WARRANTY.\n" +
        "You are welcome to redistribute it under certain conditions.\n" +
        "Type 'license()' or 'licence()' for distribution details.\n" +
        "\n" +
        "R is a collaborative project with many contributors.\n" +
        "Type 'contributors()' for more information.\n" +
        "\n" +
        "Type 'q()' to quit R.";

    public static final String LICENSE =
        "This software is distributed under the terms of the GNU General Public License\n" +
        "Version 2, June 1991. The terms of the license are in a file called COPYING\n" +
        "which you should have received with this software. A copy of the license can be\n" +
        "found at http://www.gnu.org/licenses/gpl-2.0.html.\n" +
        "\n" +
        "'Share and Enjoy.'";
    //@formatter:on

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

    public static final String R_TARGET = "target";
    public static final String R_DOT_TARGET = ".target";
    public static final String R_DEFINED = "defined";
    public static final String R_DOT_DEFINED = ".defined";
    public static final String R_NEXT_METHOD = "nextMethod";
    public static final String R_DOT_NEXT_METHOD = ".nextMethod";
    public static final String R_LOAD_METHOD_NAME = "loadMethod";
    public static final String R_DOT_METHODS = ".Methods";
    public static final String R_SOURCE = "source";
    public static final String R_COMMENT = "comment";
    public static final String R_SRCREF = "srcref";
    public static final String R_WHOLE_SRCREF = "wholeSrcref";
    public static final String R_SRCFILE = "srcfile";

    public static final String NULL = "NULL";
    public static final RSymbol PSEUDO_NULL = new RSymbol("\u0001NULL\u0001");
    public static final String UNBOUND = "UNBOUND";

    @CompilationFinal private static final String[] numberStringCache = new String[4096];
    private static final int MIN_CACHED_NUMBER = -numberStringCache.length / 2;
    private static final int MAX_CACHED_NUMBER = numberStringCache.length / 2 - 1;

    static {
        for (int i = 0; i < numberStringCache.length; i++) {
            numberStringCache[i] = String.valueOf(i + MIN_CACHED_NUMBER);
        }
    }

    /**
     * Create an {@link VirtualFrame} for a non-function environment, e.g., a package frame or the
     * global environment.
     */
    public static MaterializedFrame createNonFunctionFrame(String name) {
        FrameDescriptor frameDescriptor = new FrameDescriptor();
        MaterializedFrame frame = Truffle.getRuntime().createMaterializedFrame(RArguments.createUnitialized(), frameDescriptor);
        FrameSlotChangeMonitor.initializeNonFunctionFrameDescriptor(name, frame);
        return frame;
    }

    public static RComplex createComplexNA() {
        return RDataFactory.createComplex(COMPLEX_NA_REAL_PART, COMPLEX_NA_IMAGINARY_PART);
    }

    /**
     * Since a distinguished NaN value is used for NA, checking for {@code isNaN} suffices.
     */
    public static boolean isNAorNaN(double d) {
        return Double.isNaN(d);
    }

    @TruffleBoundary
    // TODO refactor this into RType so it is complete and more efficient
    public static String classToString(Class<?> c) {
        if (c == RLogical.class) {
            return RType.Logical.getClazz();
        } else if (c == RInteger.class) {
            return RType.Integer.getClazz();
        } else if (c == RDouble.class) {
            return RType.Double.getClazz();
        } else if (c == RComplex.class) {
            return RType.Complex.getClazz();
        } else if (c == RRaw.class) {
            return RType.Raw.getClazz();
        } else if (c == RString.class) {
            return RType.Character.getClazz();
        } else if (c == RFunction.class) {
            return RType.Function.getClazz();
        } else if (c == Object.class) {
            return RType.Any.getClazz();
        } else {
            throw new RuntimeException("internal error, unknown class: " + c);
        }
    }

    @TruffleBoundary
    // TODO refactor this into RType so it is complete and more efficient
    public static String classToStringCap(Class<?> c) {
        if (c == RLogical.class) {
            return "Logical";
        } else if (c == RInteger.class) {
            return "Integer";
        } else if (c == RDouble.class) {
            return "Numeric";
        } else if (c == RComplex.class) {
            return "Complex";
        } else if (c == RRaw.class) {
            return "Raw";
        } else if (c == RString.class) {
            return "Character";
        } else if (c == Object.class) {
            return "Any";
        } else {
            throw new RuntimeException("internal error, unknown class: " + c);
        }
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
        return RDataFactory.createComplex(value, 0);
    }

    public static RComplex logical2complex(byte value) {
        return isNA(value) ? createComplexNA() : logical2complexNoCheck(value);
    }

    public static String logicalToStringNoCheck(byte operand) {
        assert operand == LOGICAL_TRUE || operand == LOGICAL_FALSE;
        return operand == LOGICAL_TRUE ? STRING_TRUE : STRING_FALSE;
    }

    public static String logicalToString(byte operand) {
        return isNA(operand) ? STRING_NA : logicalToStringNoCheck(operand);
    }

    // conversions from raw

    public static byte raw2logical(RRaw value) {
        return value.getValue() == 0 ? LOGICAL_FALSE : LOGICAL_TRUE;
    }

    public static int raw2int(RRaw value) {
        return raw2int(value.getValue());
    }

    public static int raw2int(byte value) {
        return value & 0xFF;
    }

    public static double raw2double(RRaw value) {
        return int2double(value.getValue() & 0xFF);
    }

    public static RComplex raw2complex(RRaw r) {
        return int2complex(raw2int(r));
    }

    public static String rawToHexString(RRaw operand) {
        int value = raw2int(operand);
        char[] digits = new char[]{Character.forDigit((value & 0xF0) >> 4, 16), Character.forDigit(value & 0x0F, 16)};
        return new String(digits);
    }

    @TruffleBoundary
    public static String rawToString(RRaw operand) {
        return intToString(raw2int(operand));
    }

    // conversions from string

    @TruffleBoundary
    public static int string2intNoCheck(String s, boolean exceptionOnFail) {
        // FIXME use R rules
        int result;
        try {
            result = Integer.decode(s);  // decode supports hex constants
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
        // FIXME use R rules
        if ("Inf".equals(v)) {
            return Double.POSITIVE_INFINITY;
        } else if ("NaN".equals(v)) {
            return Double.NaN;
        } else if ("NA_real_".equals(v)) {
            return DOUBLE_NA;
        }
        try {
            return Double.parseDouble(v);
        } catch (NumberFormatException e) {
            if (v.startsWith("0x")) {
                try {
                    return int2double(Integer.decode(v));
                } catch (NumberFormatException ein) {
                }
            }
            if (exceptionOnFail) {
                throw e;
            }
        }
        return DOUBLE_NA;
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
            return RDataFactory.createComplex(doubleValue, 0.0);
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

                return RDataFactory.createComplex(realPart * (negativeReal ? -1 : 1), imaginaryPart * (negativeImaginary ? -1 : 1));
            } catch (NumberFormatException ex) {
                return createComplexNA();
            }
        }
    }

    @TruffleBoundary
    public static RComplex string2complex(String v) {
        return isNA(v) ? createComplexNA() : string2complexNoCheck(v);
    }

    @TruffleBoundary
    public static RRaw string2raw(String v) {
        if (v.length() == 2 && (Utils.isIsoLatinDigit(v.charAt(0)) || Utils.isRomanLetter(v.charAt(0))) && (Utils.isIsoLatinDigit(v.charAt(1)) || Utils.isRomanLetter(v.charAt(1)))) {
            return RDataFactory.createRaw(Byte.parseByte(v, 16));
        } else {
            return RDataFactory.createRaw((byte) 0);
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
        return RDataFactory.createComplex(i, 0);
    }

    public static RComplex int2complex(int i) {
        return isNA(i) ? createComplexNA() : int2complexNoCheck(i);
    }

    @TruffleBoundary
    public static String intToStringNoCheck(int operand) {
        if (operand >= MIN_CACHED_NUMBER && operand <= MAX_CACHED_NUMBER) {
            return numberStringCache[operand - MIN_CACHED_NUMBER];
        } else {
            return String.valueOf(operand);
        }
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
        return RDataFactory.createRaw((byte) int2rawIntValue(i));
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
        return RDataFactory.createComplex(d, 0);
    }

    public static RComplex double2complex(double d) {
        return isNAorNaN(d) ? createComplexNA() : double2complexNoCheck(d);
    }

    public static int double2rawIntValue(double operand) {
        return isNA(operand) ? 0 : ((int) operand) & 0xFF;
    }

    public static RRaw double2raw(double operand) {
        return RDataFactory.createRaw((byte) double2rawIntValue(operand));
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
        return RDataFactory.createRaw((byte) complex2rawIntValue(c));
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
        return isNA(value.getRealPart()) || isNA(value.getImaginaryPart());
    }

    @TruffleBoundary
    public static String quoteString(String value, boolean encodeNonASCII) {
        if (isNA(value)) {
            return STRING_NA;
        }
        StringBuilder str = new StringBuilder(value.length() + 2);
        str.append('\"');
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
                    str.append("\\\"");
                    break;
                default:
                    if (codepoint < 32 || codepoint == 0x7f) {
                        str.append("\\").append(codepoint >>> 6).append((codepoint >>> 3) & 0x7).append(codepoint & 0x7);
                    } else if (encodeNonASCII && codepoint > 0x7f && codepoint <= 0xff) {
                        str.append("\\x" + Integer.toHexString(codepoint));
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
        str.append('\"');
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
    public static byte asLogicalObject(Object objArg) {
        Object obj = asAbstractVector(objArg);
        if (obj instanceof RAbstractIntVector) {
            return int2logical(((RAbstractIntVector) obj).getDataAt(0));
        } else if (obj instanceof RAbstractDoubleVector) {
            return double2logical(((RAbstractDoubleVector) obj).getDataAt(0));
        } else if (obj instanceof RAbstractLogicalVector) {
            return ((RAbstractLogicalVector) obj).getDataAt(0);
        } else if (obj instanceof RAbstractComplexVector) {
            return complex2logical(((RAbstractComplexVector) obj).getDataAt(0));
        } else if (obj instanceof RAbstractStringVector) {
            return string2logical(((RAbstractStringVector) obj).getDataAt(0));
        } else {
            return LOGICAL_NA;
        }
    }

    /**
     * Java equivalent of GnuR asInteger for use outside Truffle boundary. TODO support for warnings
     */
    public static int asInteger(Object objArg) {
        Object obj = asAbstractVector(objArg);
        if (obj instanceof RAbstractIntVector) {
            return ((RAbstractIntVector) obj).getDataAt(0);
        } else if (obj instanceof RAbstractDoubleVector) {
            return double2int(((RAbstractDoubleVector) obj).getDataAt(0));
        } else if (obj instanceof RAbstractLogicalVector) {
            return logical2int(((RAbstractLogicalVector) obj).getDataAt(0));
        } else if (obj instanceof RAbstractComplexVector) {
            return complex2int(((RAbstractComplexVector) obj).getDataAt(0));
        } else if (obj instanceof RAbstractStringVector) {
            return string2int(((RAbstractStringVector) obj).getDataAt(0));
        } else {
            return INT_NA;
        }
    }

    /**
     * Returns {@code true} if the given object is R object and its class attribute contains given
     * class.
     */
    public static boolean hasRClass(Object obj, String rclassName) {
        return obj instanceof RAttributable && ((RAttributable) obj).hasClass(rclassName);
    }

    public static boolean checkType(Object obj, RType type) {
        if (type == RType.Any) {
            return true;
        }
        if (type == RType.Function || type == RType.Closure || type == RType.Builtin || type == RType.Special) {
            return (obj instanceof RFunction) || (obj instanceof TruffleObject && !(obj instanceof RTypedValue));
        }
        if (type == RType.Character) {
            return obj instanceof String || obj instanceof RStringVector;
        }
        if (type == RType.Logical) {
            return obj instanceof Byte;
        }
        if (type == RType.Integer || type == RType.Double) {
            return obj instanceof Integer || obj instanceof Double;
        }
        return false;
    }

    /**
     * Runtime variant of DSL support for converting scalar values to {@link RAbstractVector}.
     */
    public static Object asAbstractVector(Object obj) {
        if (obj instanceof Integer) {
            return RDataFactory.createIntVectorFromScalar((Integer) obj);
        } else if (obj instanceof Double) {
            return RDataFactory.createDoubleVectorFromScalar((Double) obj);
        } else if (obj instanceof Byte) {
            return RDataFactory.createLogicalVectorFromScalar((Byte) obj);
        } else if (obj instanceof String) {
            return RDataFactory.createStringVectorFromScalar((String) obj);
        } else if (obj instanceof RComplex) {
            RComplex complex = (RComplex) obj;
            return RDataFactory.createComplexVector(new double[]{complex.getRealPart(), complex.getImaginaryPart()}, RDataFactory.COMPLETE_VECTOR);
        } else if (obj instanceof RRaw) {
            return RDataFactory.createRawVector(new byte[]{((RRaw) obj).getValue()});
        } else {
            return obj;
        }
    }

    public static boolean isForeignObject(TruffleObject obj) {
        return !(obj instanceof RTypedValue);
    }

    public static boolean isForeignObject(Object obj) {
        return obj instanceof TruffleObject && !(obj instanceof RTypedValue);
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
            if (xa.hasDimensions()) {
                return xa.getDimensions()[0];
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
            if (xa.hasDimensions()) {
                int[] dims = xa.getDimensions();
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

}
