/*
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates
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
package com.oracle.truffle.r.runtime.gnur;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RComplexVector;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.REmpty;
import com.oracle.truffle.r.runtime.data.RExpression;
import com.oracle.truffle.r.runtime.data.RExternalPtr;
import com.oracle.truffle.r.runtime.data.RForeignObjectWrapper;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.data.RPromise.EagerPromise;
import com.oracle.truffle.r.runtime.data.RRawVector;
import com.oracle.truffle.r.runtime.data.RS4Object;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.data.RUnboundValue;
import com.oracle.truffle.r.runtime.data.RWeakRef;
import com.oracle.truffle.r.runtime.data.closures.RToStringVectorClosure;
import com.oracle.truffle.r.runtime.env.REnvironment;

// Transcribed from GnuR src/include/Rinternals.h and src/main/serialize.c

public enum SEXPTYPE {

    NILSXP(0, RNull.class), /* nil ()NULL */
    SYMSXP(1, RSymbol.class), /* symbols */
    LISTSXP(2), /* lists of dotted pairs */
    CLOSXP(3), /* closures */
    ENVSXP(4, REnvironment.class), /* environments */
    PROMSXP(5, RPromise.class, EagerPromise.class), /* promises: [un]evaluated closure arguments */
    LANGSXP(6), /* language constructs (special lists) */
    SPECIALSXP(7), /* special forms */
    BUILTINSXP(8), /* builtin non-special forms */
    CHARSXP(9), /* "scalar" string type (GnuR internal only) */
    /* logical vectors */
    LGLSXP(10, RLogicalVector.class, Byte.class),
    /* integer vectors */
    INTSXP(13, RIntVector.class, Integer.class),
    /* real vectors */
    REALSXP(14, RDoubleVector.class, Double.class),
    /* complex vectors */
    CPLXSXP(15, RComplexVector.class, RComplex.class),
    /* string vectors */
    STRSXP(16, RStringVector.class, String.class, RToStringVectorClosure.class),
    DOTSXP(17, RArgsValuesAndNames.class), /* dot-dot-dot object */
    ANYSXP(18), /* make "any" args work */
    VECSXP(19, RList.class), /* generic vectors */
    EXPRSXP(20, RExpression.class), /* expressions vectors */
    BCODESXP(21), /* byte code */
    EXTPTRSXP(22, RExternalPtr.class), /* external pointer */
    WEAKREFSXP(23), /* weak reference */
    RAWSXP(24, RRawVector.class), /* raw bytes */
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

    ALTREP_SXP(238),

    EMPTYARG_SXP(500, REmpty.class);

    public final int code;
    public final Class<?>[] fastRClasses;

    private static final SEXPTYPE[] codeMap = new SEXPTYPE[501];

    SEXPTYPE(int code, Class<?>... fastRClasses) {
        this.code = code;
        this.fastRClasses = fastRClasses;
    }

    static {
        for (SEXPTYPE type : SEXPTYPE.values()) {
            assert type.code >= 0 && type.code < codeMap.length;
            codeMap[type.code] = type;
        }
    }

    public static SEXPTYPE mapInt(int type) {
        return codeMap[type];
    }

    /**
     * Return the GnuR type for the FastR class. There are times when it is convenient to work with
     * ints, e.g. {@code DeParse}. N.B. This is not unique for {@link RPairList}, so the
     * {@code type} field on the {@link RPairList} has to be consulted.
     */
    public static SEXPTYPE typeForClass(Object value) {
        if (value instanceof RPairList) {
            return ((RPairList) value).isLanguage() ? LANGSXP : LISTSXP;
        }
        if (value instanceof RForeignObjectWrapper) {
            return VECSXP;
        }
        if (value instanceof RWeakRef) {
            return WEAKREFSXP;
        }
        Class<?> fastRClass = value.getClass();
        for (SEXPTYPE type : values()) {
            for (Class<?> clazz : type.fastRClasses) {
                if (fastRClass == clazz) {
                    return type;
                }
            }
        }
        // (only) promises and environments have subtypes
        if (REnvironment.class.isAssignableFrom(fastRClass)) {
            return ENVSXP;
        } else if (RPromise.class.isAssignableFrom(fastRClass)) {
            return PROMSXP;
        }
        CompilerDirectives.transferToInterpreter();
        throw RInternalError.shouldNotReachHere(fastRClass.getName());
    }

    /**
     * Accessed from FFI layer.
     */
    public static int gnuRCodeForObject(Object obj) {
        return gnuRTypeForObject(obj).code;
    }

    /**
     * Accessed from FFI layer.
     */
    public static SEXPTYPE gnuRTypeForObject(Object obj) {
        SEXPTYPE type = typeForClass(obj);
        return gnuRType(type, obj);
    }

    /**
     * Convert an {@code SEXPTYPE} that may be a {@code FASTR_XXX} variant into the appropriate GnuR
     * type.
     */
    public static SEXPTYPE gnuRType(SEXPTYPE type, Object obj) {
        switch (type) {
            case FUNSXP:
                RFunction func = (RFunction) obj;
                if (func.isBuiltin()) {
                    return SEXPTYPE.BUILTINSXP;
                } else {
                    return SEXPTYPE.CLOSXP;
                }
            case LISTSXP:
                RPairList pl = (RPairList) obj;
                if (pl.getType() == SEXPTYPE.LANGSXP) {
                    return SEXPTYPE.LANGSXP;
                } else {
                    return type;
                }
            default:
                return type;
        }
    }
}
