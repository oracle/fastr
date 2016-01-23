/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.runtime.gnur;

import java.util.*;

import com.oracle.truffle.api.CompilerDirectives.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.conn.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.env.*;

// Transcribed from GnuR src/include/Rinternals.h and src/main/serialize.c

public enum SEXPTYPE {
    NILSXP(0, RNull.class), /* nil ()NULL */
    SYMSXP(1, RSymbol.class), /* symbols */
    LISTSXP(2, RPairList.class), /* lists of dotted pairs */
    CLOSXP(3, RPairList.class), /* closures */
    ENVSXP(4, REnvironment.class), /* environments */
    PROMSXP(5, RPromise.class), /* promises: [un]evaluated closure arguments */
    LANGSXP(6, RLanguage.class), /* language constructs (special lists) */
    SPECIALSXP(7), /* special forms */
    BUILTINSXP(8), /* builtin non-special forms */
    CHARSXP(9), /* "scalar" string type (GnuR internal only) */
    LGLSXP(10, RLogicalVector.class), /* logical vectors */
    INTSXP(13, new Class<?>[]{RIntVector.class, RIntSequence.class}), /* integer vectors */
    REALSXP(14, new Class<?>[]{RDoubleVector.class, RDoubleSequence.class}), /* real variables */
    CPLXSXP(15, RComplexVector.class), /* complex variables */
    STRSXP(16, new Class<?>[]{RStringVector.class, String.class}), /* string vectors */
    DOTSXP(17, RArgsValuesAndNames.class), /* dot-dot-dot object */
    ANYSXP(18), /* make "any" args work */
    VECSXP(19, RList.class), /* generic vectors */
    EXPRSXP(20, RExpression.class), /* expressions vectors */
    BCODESXP(21), /* byte code */
    EXTPTRSXP(22, RExternalPtr.class), /* external pointer */
    WEAKREFSXP(23), /* weak reference */
    RAWSXP(24, new Class<?>[]{RRawVector.class, RRaw.class}), /* raw bytes */
    S4SXP(25, RS4Object.class), /* S4 non-vector */

    NEWSXP(30), /* fresh node created in new page */
    FREESXP(31), /* node released by GC */

    FUNSXP(99, RFunction.class), /* Closure or Builtin */

    // used in RSerialize
    REFSXP(255),
    NILVALUE_SXP(254),
    GLOBALENV_SXP(253),
    UNBOUNDVALUE_SXP(252, RUnboundValue.class),
    MISSINGARG_SXP(251, RMissing.class),
    BASENAMESPACE_SXP(250),
    NAMESPACESXP(249),
    PACKAGESXP(248),
    PERSISTSXP(247),
    BCREPDEF(244),
    BCREPREF(243),
    EMPTYENV_SXP(242),
    BASEENV_SXP(241),
    ATTRLANGSXP(240),
    ATTRLISTSXP(239),

    /*
     * FastR scalar variants of GnuR vector types (other than String) These could be removed in a
     * similar way to String, but there is no pressing need.
     */
    FASTR_DOUBLE(300, Double.class),
    FASTR_INT(301, Integer.class),
    FASTR_BYTE(302, Byte.class),
    FASTR_COMPLEX(303, RComplex.class),
    // FastR special "vector" types
    FASTR_DATAFRAME(304, RDataFrame.class),
    FASTR_FACTOR(305, RFactor.class),
    // very special case
    FASTR_SOURCESECTION(306, SourceSection.class),
    FASTR_CONNECTION(307, RConnection.class),

    EMPTYARG_SXP(500, REmpty.class);

    public final int code;
    public final Class<?>[] fastRClasses;

    @CompilationFinal private static final SEXPTYPE[] NON_NULL_VALUES;

    SEXPTYPE(int code) {
        this.code = code;
        this.fastRClasses = null;
    }

    SEXPTYPE(int code, Class<?>[] fastRClasses) {
        this.code = code;
        this.fastRClasses = fastRClasses;

    }

    SEXPTYPE(int code, Class<?> fastRClass) {
        this(code, new Class<?>[]{fastRClass});
    }

    private static final Map<Integer, SEXPTYPE> codeMap = new HashMap<>();

    public static SEXPTYPE mapInt(int type) {
        return codeMap.get(type);
    }

    /**
     * Return the GnuR type for the FastR class. There are times when it is convenient to work with
     * ints, e.g. {@code DeParse}. N.B. This is not unique for {@link RPairList}, so the
     * {@code type} field on the {@link RPairList} has to be consulted.
     */
    public static SEXPTYPE typeForClass(Class<?> fastRClass) {
        for (SEXPTYPE type : NON_NULL_VALUES) {
            if (type.fastRClasses.length == 1) {
                if (fastRClass == type.fastRClasses[0]) {
                    return type;
                }
            } else {
                if (fastRClass == type.fastRClasses[0] || fastRClass == type.fastRClasses[1]) {
                    return type;
                }
            }
        }
        // (only) environments and connections have subtypes
        if (REnvironment.class.isAssignableFrom(fastRClass)) {
            return ENVSXP;
        } else if (RConnection.class.isAssignableFrom(fastRClass)) {
            return FASTR_CONNECTION;
        }
        throw RInternalError.shouldNotReachHere(fastRClass.getName());
    }

    /**
     * Accessed from FFI layer.
     */
    public static int gnuRCodeForObject(Object obj) {
        SEXPTYPE type = typeForClass(obj.getClass());
        return gnuRType(type, obj).code;
    }

    /**
     * Convert an {@code SEXPTYPE} that may be a {@code FASTR_XXX} variant into the appropriate GnuR
     * type.
     */
    public static SEXPTYPE gnuRType(SEXPTYPE type, Object obj) {
        switch (type) {
            case FUNSXP: {
                RFunction func = (RFunction) obj;
                if (func.isBuiltin()) {
                    return SEXPTYPE.BUILTINSXP;
                } else {
                    return SEXPTYPE.CLOSXP;
                }
            }

            case LISTSXP: {
                RPairList pl = (RPairList) obj;
                if (pl.getType() != null && pl.getType() == SEXPTYPE.LANGSXP) {
                    return SEXPTYPE.LANGSXP;
                } else {
                    return type;
                }
            }

            case FASTR_INT:
                return SEXPTYPE.INTSXP;
            case FASTR_DOUBLE:
                return SEXPTYPE.REALSXP;
            case FASTR_BYTE:
                return SEXPTYPE.LGLSXP;
            case FASTR_COMPLEX:
                return SEXPTYPE.CPLXSXP;
            case FASTR_DATAFRAME:
            case FASTR_FACTOR:
                return SEXPTYPE.VECSXP;
            case FASTR_CONNECTION:
                return SEXPTYPE.INTSXP;
            default:
                return type;
        }
    }

    public static SEXPTYPE convertFastRScalarType(SEXPTYPE type) {
        switch (type) {
            case FASTR_DOUBLE:
                return SEXPTYPE.REALSXP;
            case FASTR_INT:
                return SEXPTYPE.INTSXP;
            case FASTR_BYTE:
                return SEXPTYPE.LGLSXP;
            case FASTR_COMPLEX:
                return SEXPTYPE.CPLXSXP;
            default:
                assert false;
                return null;
        }
    }

    static {
        int nonNullCount = 0;
        for (SEXPTYPE type : SEXPTYPE.values()) {
            SEXPTYPE.codeMap.put(type.code, type);
            if (type.fastRClasses != null) {
                nonNullCount++;
            }
        }
        NON_NULL_VALUES = new SEXPTYPE[nonNullCount];
        nonNullCount = 0;
        for (SEXPTYPE type : SEXPTYPE.values()) {
            if (type.fastRClasses != null) {
                NON_NULL_VALUES[nonNullCount++] = type;
            }
        }
    }

}
