/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.runtime;

import java.text.*;
import java.util.*;

import com.oracle.truffle.api.CompilerDirectives.SlowPath;
import com.oracle.truffle.api.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.runtime.data.*;

import edu.umd.cs.findbugs.annotations.*;

public class RRuntime {

    //@formatter:off
    // Parts of the welcome message originate from GNU R.
    public static final String WELCOME_MESSAGE =
        "FastR version " + RVersionNumber.FULL + "\n" +
        "Copyright (c) 2013-4, Oracle and/or its affiliates\n" +
        "Copyright (c) 1995-2012, The R Core Team\n" +
        "Copyright (c) 2003 The R Foundation\n" +
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

    public static final int TRUE = 1;
    public static final int FALSE = 0;
    public static final String STRING_NA = new String("NA");
    public static final String STRING_NaN = new String("NaN");
    public static final String STRING_TRUE = new String("TRUE");
    public static final String STRING_FALSE = new String("FALSE");
    public static final int INT_NA = Integer.MIN_VALUE;
    public static final int INT_MIN_VALUE = Integer.MIN_VALUE + 1;
    public static final int INT_MAX_VALUE = Integer.MAX_VALUE;

    // R's NA is a special instance of IEEE's NaN
    public static final long NA_LONGBITS = 0x7ff00000000007a2L;
    public static final double DOUBLE_NA = Double.longBitsToDouble(NA_LONGBITS);
    public static final double EPSILON = Math.pow(2.0, -52.0);

    public static final double COMPLEX_NA_REAL_PART = DOUBLE_NA;
    public static final double COMPLEX_NA_IMAGINARY_PART = 0.0;

    public static final byte LOGICAL_TRUE = 1;
    public static final byte LOGICAL_FALSE = 0;
    public static final byte LOGICAL_NA = -1;

    public static final String TYPE_ANY = new String("any");
    public static final String TYPE_NUMERIC = new String("numeric");
    public static final String TYPE_DOUBLE = new String("double");
    public static final String TYPE_INTEGER = new String("integer");
    public static final String TYPE_COMPLEX = new String("complex");
    public static final String TYPE_CHARACTER = new String("character");
    public static final String TYPE_LOGICAL = new String("logical");
    public static final String TYPE_RAW = new String("raw");
    public static final String TYPE_LIST = new String("list");
    public static final String TYPE_FORMULA = new String("formula");
    public static final String TYPE_FUNCTION = new String("function");
    public static final String TYPE_MATRIX = new String("matrix");
    public static final String TYPE_ARRAY = new String("array");
    public static final String TYPE_CLOSURE = new String("closure");
    public static final String TYPE_BUILTIN = new String("builtin");
    public static final String TYPE_SPECIAL = new String("special");
    public static final String TYPE_DATA_FRAME = new String("data.frame");
    public static final String TYPE_FACTOR = new String("factor");
    public static final String TYPE_SYMBOL = new String("symbol");
    // Defunct types
    public static final String REAL = new String("real");
    public static final String SINGLE = new String("single");

    public static final String TYPE_NUMERIC_CAP = new String("Numeric");
    public static final String TYPE_INTEGER_CAP = new String("Integer");
    public static final String TYPE_COMPLEX_CAP = new String("Complex");
    public static final String TYPE_CHARACTER_CAP = new String("Character");
    public static final String TYPE_LOGICAL_CAP = new String("Logical");
    public static final String TYPE_RAW_CAP = new String("Raw");

    public static final String[] STRING_ARRAY_SENTINEL = new String[0];
    public static final String DEFAULT = "default";

    public static final String NAMES_ATTR_KEY = new String("names");
    public static final String NAMES_ATTR_EMPTY_VALUE = "";

    public static final String LEVELS_ATTR_KEY = new String("levels");
    public static final String LEVELS_ATTR_EMPTY_VALUE = "";

    public static final String NA_HEADER = "<NA>";

    public static final String DIM_ATTR_KEY = new String("dim");
    public static final String DIMNAMES_ATTR_KEY = "dimnames";
    public static final String DIMNAMES_LIST_ELEMENT_NAME_PREFIX = "$dimnames";

    public static final String CLASS_ATTR_KEY = "class";
    public static final String PREVIOUS_ATTR_KEY = "previous";
    public static final String ROWNAMES_ATTR_KEY = "row.names";

    public static final String[] CLASS_INTEGER = new String[]{TYPE_INTEGER, TYPE_NUMERIC};
    public static final String[] CLASS_DOUBLE = new String[]{TYPE_DOUBLE, TYPE_NUMERIC};

    public static final String WHICH = "which";

    public static final String WHAT = "what";

    public static final int LEN_METHOD_NAME = 512;

    public static final String RDotGeneric = ".Generic";

    public static final String RDotMethod = ".Method";

    public static final String RDotClass = ".Class";

    public static final String RDotGenericCallEnv = ".GenericCallEnv";

    public static final String RDotGenericDefEnv = ".GenericDefEnv";

    public static final String RDOT = ".";

    public static final String USE_METHOD = "UseMethod";

    public static final String NEXT_METHOD = "NextMethod";

    public static final String SYSTEM_DATE_FORMAT = "EEE MMM dd HH:mm:ss yyyy";

    public static final String DROP_DIM_ARG_NAME = "drop";

    /**
     * Create an {@link VirtualFrame} for a non-function environment, e.g., a package frame or the
     * global environment.
     */
    public static VirtualFrame createNonFunctionFrame() {
        return Truffle.getRuntime().createVirtualFrame(RArguments.createUnitialized(), new FrameDescriptor());
    }

    /**
     * Create a {@link VirtualFrame} for {@link RFunction} {@code function}.
     */
    public static VirtualFrame createFunctionFrame(RFunction function) {
        return Truffle.getRuntime().createVirtualFrame(RArguments.create(function), new FrameDescriptor());
    }

    public static RComplex createComplexNA() {
        return RDataFactory.createComplex(COMPLEX_NA_REAL_PART, COMPLEX_NA_IMAGINARY_PART);
    }

    public static boolean isNAorNaN(double d) {
        return isNA(d) || Double.isNaN(d);
    }

    @SlowPath
    public static String classToString(Class<?> c, boolean numeric) {
        if (c == RLogical.class) {
            return TYPE_LOGICAL;
        } else if (c == RInt.class) {
            return TYPE_INTEGER;
        } else if (c == RDouble.class) {
            return numeric ? TYPE_NUMERIC : TYPE_DOUBLE;
        } else if (c == RComplex.class) {
            return TYPE_COMPLEX;
        } else if (c == RRaw.class) {
            return TYPE_RAW;
        } else if (c == RString.class) {
            return TYPE_CHARACTER;
        } else {
            throw new RuntimeException("internal error, unknown class: " + c);
        }
    }

    @SlowPath
    public static String classToString(Class<?> c) {
        return classToString(c, true);
    }

    @SlowPath
    public static String classToStringCap(Class<?> c) {
        if (c == RLogical.class) {
            return TYPE_LOGICAL_CAP;
        } else if (c == RInt.class) {
            return TYPE_INTEGER_CAP;
        } else if (c == RDouble.class) {
            return TYPE_NUMERIC_CAP;
        } else if (c == RComplex.class) {
            return TYPE_COMPLEX_CAP;
        } else if (c == RRaw.class) {
            return TYPE_RAW_CAP;
        } else if (c == RString.class) {
            return TYPE_CHARACTER_CAP;
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
        return operand == LOGICAL_TRUE ? STRING_TRUE : operand == LOGICAL_FALSE ? STRING_FALSE : STRING_NA;
    }

    public static String logicalToString(byte operand) {
        return isNA(operand) ? STRING_NA : logicalToStringNoCheck(operand);
    }

    // conversions from raw

    public static byte raw2logical(RRaw value) {
        return value.getValue() == 0 ? LOGICAL_FALSE : LOGICAL_TRUE;
    }

    public static int raw2int(RRaw value) {
        return value.getValue() & 0xFF;
    }

    public static double raw2double(RRaw value) {
        return int2double(value.getValue() & 0xFF);
    }

    public static RComplex raw2complex(RRaw r) {
        return int2complex(raw2int(r));
    }

    @SlowPath
    public static String rawToString(RRaw operand) {
        return intToString(raw2int(operand), false);
    }

    // conversions from string

    @SlowPath
    public static int string2intNoCheck(String s) {
        // FIXME use R rules
        try {
            return Integer.decode(s);  // decode supports hex constants
        } catch (NumberFormatException e) {
            RContext.getInstance().getAssumptions().naIntroduced.invalidate();
        }
        return INT_NA;
    }

    @SlowPath
    public static int string2int(String s) {
        return isNA(s) ? INT_NA : string2intNoCheck(s);
    }

    @SlowPath
    public static double string2doubleNoCheck(String v) {
        // FIXME use R rules
        if ("Inf".equals(v)) {
            return Double.POSITIVE_INFINITY;
        } else if ("NaN".equals(v)) {
            return Double.NaN;
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
            RContext.getInstance().getAssumptions().naIntroduced.invalidate();
        }
        return DOUBLE_NA;
    }

    @SlowPath
    public static double string2double(String v) {
        if (isNA(v)) {
            return DOUBLE_NA;
        } else {
            return string2doubleNoCheck(v);
        }
    }

    public static byte string2logicalNoCheck(String s) {
        if (s.equals("TRUE") || s.equals("T")) {
            return TRUE;
        }
        if (s.equals("FALSE") || s.equals("F")) {
            return FALSE;
        }
        if (s.equals("True") || s.equals("true")) {
            return TRUE;
        }
        if (s.equals("False") || s.equals("false")) {
            return FALSE;
        }
        RContext.getInstance().getAssumptions().naIntroduced.invalidate();
        return LOGICAL_NA;
    }

    public static byte string2logical(String s) {
        return isNA(s) ? LOGICAL_NA : string2logicalNoCheck(s);
    }

    @SlowPath
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

    @SlowPath
    public static RComplex string2complex(String v) {
        return isNA(v) ? createComplexNA() : string2complexNoCheck(v);
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

    @SlowPath
    public static String intToStringNoCheck(int operand, boolean appendL) {
        return String.valueOf(operand) + (appendL ? "L" : "");
    }

    public static String intToString(int operand, boolean appendL) {
        return isNA(operand) ? STRING_NA : intToStringNoCheck(operand, appendL);
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
        return d == 0.0 ? LOGICAL_FALSE : LOGICAL_TRUE;
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

    @SlowPath
    public static String doubleToString(double operand, int digitsBehindDot) {
        return isNA(operand) ? STRING_NA : doubleToStringNoCheck(operand, digitsBehindDot);
    }

    @SlowPath
    public static String doubleToStringNoCheck(double operand, int digitsBehindDot) {
        if (doubleIsInt(operand)) {
            return String.valueOf((int) operand);
        }
        if (operand == Double.POSITIVE_INFINITY) {
            return "Inf";
        }
        if (operand == Double.NEGATIVE_INFINITY) {
            return "-Inf";
        }
        if (Double.isNaN(operand)) {
            return STRING_NaN;
        }

        /*
         * DecimalFormat format = new DecimalFormat(); format.setMaximumIntegerDigits(12);
         * format.setMaximumFractionDigits(12); format.setGroupingUsed(false); return
         * format.format(operand);
         */
        if (operand < 1000000000000L && ((long) operand) == operand) {
            return Long.toString((long) operand);
        }
        if (operand > 1000000000000L) {
            return String.format((Locale) null, "%.6e", operand);
        }
        if (digitsBehindDot == -1) {
            return Double.toString(operand);
        } else {
            StringBuilder sb = new StringBuilder("#.");
            for (int i = 0; i < digitsBehindDot; i++) {
                sb.append('#');
            }
            DecimalFormat df = new DecimalFormat(sb.toString());
            return df.format(operand);
        }
    }

    public static String doubleToStringNoCheck(double operand) {
        return doubleToStringNoCheck(operand, -1);
    }

    public static String doubleToString(double operand) {
        return isNA(operand) ? STRING_NA : doubleToStringNoCheck(operand);
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

    @SlowPath
    public static String complexToStringNoCheck(RComplex operand) {
        return doubleToString(operand.getRealPart()) + "+" + doubleToString(operand.getImaginaryPart()) + "i";
    }

    @SlowPath
    public static String complexToString(RComplex operand) {
        return isNA(operand) ? STRING_NA : complexToStringNoCheck(operand);
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

    @SlowPath
    public static String toString(Object object) {
        if (object instanceof Integer) {
            int intValue = (int) object;
            if (isNA(intValue)) {
                return STRING_NA;
            }
            return String.valueOf(intValue);
        } else if (object instanceof Double) {
            double doubleValue = (double) object;
            if (isNA(doubleValue)) {
                return STRING_NA;
            }
            return String.valueOf(doubleValue);
        } else if (object instanceof Byte) {
            byte logicalValue = (byte) object;
            if (isNA(logicalValue)) {
                return STRING_NA;
            }
            return logicalValue == LOGICAL_TRUE ? "TRUE" : "FALSE";
        }

        return object.toString();
    }

    @SlowPath
    public static String toString(StringBuilder sb) {
        return sb.toString();
    }

    @SuppressFBWarnings(value = "ES_COMPARING_PARAMETER_STRING_WITH_EQ", justification = "string NA is intended to be treated as an identity")
    public static boolean isNA(String value) {
        return value == STRING_NA;
    }

    @SuppressFBWarnings(value = "ES_COMPARING_PARAMETER_STRING_WITH_EQ", justification = "string NaN is intended to be treated as an identity")
    public static boolean isNaN(String value) {
        return value == STRING_NaN;
    }

    public static boolean isNA(byte value) {
        return value == LOGICAL_NA;
    }

    public static boolean isNA(int left) {
        return left == INT_NA;
    }

    public static boolean isNA(double left) {
        return Double.doubleToRawLongBits(left) == NA_LONGBITS;
    }

    public static boolean isNA(RComplex left) {
        return isNA(left.getRealPart());
    }

    public static boolean isComplete(String left) {
        return !isNA(left);
    }

    public static boolean isComplete(byte left) {
        return !isNA(left);
    }

    public static boolean isComplete(int left) {
        return !isNA(left);
    }

    public static boolean isComplete(double left) {
        return !isNA(left);
    }

    public static boolean isComplete(RComplex left) {
        return !isNA(left);
    }

    @SlowPath
    public static String quoteString(String data) {
        return isNA(data) ? STRING_NA : "\"" + data + "\"";
    }

}
