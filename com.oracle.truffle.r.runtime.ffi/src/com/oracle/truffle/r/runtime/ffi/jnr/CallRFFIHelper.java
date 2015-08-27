/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.ffi.jnr;

import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.context.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.env.*;
import com.oracle.truffle.r.runtime.env.REnvironment.PutException;
import com.oracle.truffle.r.runtime.gnur.*;
import com.oracle.truffle.r.runtime.ops.na.*;

/**
 * This class provides methods that match the functionality of the macro/function definitions in
 * thye R header files, e.g. {@code Rinternals.h} that are used by C/C++ code. For ease of
 * identification, we use method names that, as far as possible, match the names in the hreader
 * files. These methods should never be called from normal FastR code.
 */
public class CallRFFIHelper {
    @SuppressWarnings("unused") private static final NACheck elementNACheck = NACheck.create();

    // Checkstyle: stop method name check

    static RIntVector Rf_ScalarInteger(int value) {
        return RDataFactory.createIntVectorFromScalar(value);
    }

    static RLogicalVector Rf_ScalarLogical(int value) {
        return RDataFactory.createLogicalVectorFromScalar(value != 0);
    }

    static RDoubleVector Rf_ScalarDouble(double value) {
        return RDataFactory.createDoubleVectorFromScalar(value);
    }

    static RStringVector Rf_ScalarString(String value) {
        return RDataFactory.createStringVectorFromScalar(value);
    }

    static int Rf_asInteger(Object x) {
        if (x instanceof Integer) {
            return ((Integer) x).intValue();
        } else if (x instanceof RIntVector) {
            return ((RIntVector) x).getDataAt(0);
        } else {
            throw RInternalError.unimplemented();
        }
    }

    static double Rf_asReal(Object x) {
        if (x instanceof Double) {
            return ((Double) x).doubleValue();
        } else if (x instanceof RDoubleVector) {
            return ((RDoubleVector) x).getDataAt(0);
        } else {
            throw RInternalError.unimplemented();
        }
    }

    static int Rf_asLogical(Object x) {
        if (x instanceof Byte) {
            return ((Byte) x).intValue();
        } else if (x instanceof RLogicalVector) {
            return ((RLogicalVector) x).getDataAt(0);
        } else {
            throw RInternalError.unimplemented();
        }
    }

    static String Rf_asChar(Object x) {
        if (x instanceof String) {
            return (String) x;
        } else if (x instanceof RStringVector) {
            return ((RStringVector) x).getDataAt(0);
        } else {
            throw RInternalError.unimplemented();
        }
    }

    static Object Rf_cons(Object car, Object cdr) {
        return RDataFactory.createPairList(car, cdr);
    }

    static void Rf_defineVar(Object symbolArg, Object value, Object envArg) {
        REnvironment env = (REnvironment) envArg;
        RSymbol name = (RSymbol) symbolArg;
        try {
            env.put(name.getName(), value);
        } catch (PutException ex) {
            throw RError.error(RError.NO_NODE, ex);
        }
    }

    static Object Rf_findVar(Object symbolArg, Object envArg) {
        return findVarInFrameHelper(symbolArg, envArg, true);
    }

    static Object Rf_findVarInFrame(Object symbolArg, Object envArg) {
        return findVarInFrameHelper(symbolArg, envArg, false);
    }

    private static Object findVarInFrameHelper(Object symbolArg, Object envArg, boolean inherits) {
        if (envArg == RNull.instance) {
            throw RError.error(RError.NO_NODE, RError.Message.USE_NULL_ENV_DEFUNCT);
        }
        if (!(envArg instanceof REnvironment)) {
            throw RError.error(RError.NO_NODE, RError.Message.ARG_NOT_AN_ENVIRONMENT, inherits ? "findVar" : "findVarInFrame");
        }
        RSymbol name = (RSymbol) symbolArg;
        REnvironment env = (REnvironment) envArg;
        while (env != REnvironment.emptyEnv()) {
            Object value = env.get(name.getName());
            if (value != null) {
                return value;
            }
            if (!inherits) {
                break;
            }
            env = env.getParent();
        }
        return RUnboundValue.instance;

    }

    static Object Rf_getAttrib(Object obj, Object name) {
        Object result = RNull.instance;
        if (obj instanceof RAttributable) {
            RAttributable attrObj = (RAttributable) obj;
            RAttributes attrs = attrObj.getAttributes();
            if (attrs != null) {
                String nameAsString = ((RSymbol) name).getName().intern();
                Object attr = attrs.get(nameAsString);
                if (attr != null) {
                    result = attr;
                }
            }
        }
        return result;
    }

    static void Rf_setAttrib(Object obj, Object name, Object val) {
        if (obj instanceof RAttributable) {
            RAttributable attrObj = (RAttributable) obj;
            RAttributes attrs = attrObj.getAttributes();
            if (attrs == null) {
                attrs = attrObj.initAttributes();
            }
            String nameAsString;
            if (name instanceof RSymbol) {
                nameAsString = ((RSymbol) name).getName();
            } else {
                nameAsString = RRuntime.asString(name);
                assert nameAsString != null;
            }
            nameAsString = nameAsString.intern();
            attrs.put(nameAsString, val);
        }
    }

    static int Rf_isString(Object x) {
        return RRuntime.asString(x) == null ? 0 : 1;
    }

    static int Rf_isNull(Object x) {
        return x == RNull.instance ? 1 : 0;
    }

    static Object Rf_PairToVectorList(Object x) {
        if (x == RNull.instance) {
            return RDataFactory.createList();
        }
        RPairList pl = (RPairList) x;
        return pl.toRList();
    }

    static void Rf_error(String msg) {
        throw RError.error(RError.NO_NODE, RError.Message.GENERIC, msg);
    }

    static void Rf_warning(String msg) {
        RError.warning(RError.NO_NODE, RError.Message.GENERIC, msg);
    }

    static void Rf_warningcall(Object call, String msg) {
        RErrorHandling.warningcallRFFI(call, msg);
    }

    static Object Rf_allocateVector(int mode, int n) {
        SEXPTYPE type = SEXPTYPE.mapInt(mode);
        if (n < 0) {
            throw RError.error(RError.NO_NODE, RError.Message.NEGATIVE_LENGTH_VECTORS_NOT_ALLOWED);
            // TODO check long vector
        }
        switch (type) {
            case INTSXP:
                return RDataFactory.createIntVector(new int[n], RDataFactory.COMPLETE_VECTOR);
            case REALSXP:
                return RDataFactory.createDoubleVector(new double[n], RDataFactory.COMPLETE_VECTOR);
            case LGLSXP:
                return RDataFactory.createLogicalVector(new byte[n], RDataFactory.COMPLETE_VECTOR);
            case STRSXP:
                return RDataFactory.createStringVector(new String[n], RDataFactory.COMPLETE_VECTOR);
            case CPLXSXP:
                return RDataFactory.createComplexVector(new double[2 * n], RDataFactory.COMPLETE_VECTOR);
            case VECSXP:
                return RDataFactory.createList(n);
            default:
                throw RInternalError.unimplemented();
        }

    }

    static Object Rf_allocateArray(int mode, Object dimsObj) {
        RIntVector dims = (RIntVector) dimsObj;
        int n = 1;
        int[] newDims = new int[dims.getLength()];
        // TODO check long vector
        for (int i = 0; i < newDims.length; i++) {
            newDims[i] = dims.getDataAt(i);
            n *= newDims[i];
        }
        RAbstractVector result = (RAbstractVector) Rf_allocateVector(mode, n);
        result.setDimensions(newDims);
        return result;

    }

    static Object Rf_allocateMatrix(int mode, int ncol, int nrow) {
        SEXPTYPE type = SEXPTYPE.mapInt(mode);
        if (nrow < 0 || ncol < 0) {
            throw RError.error(RError.NO_NODE, RError.Message.NEGATIVE_EXTENTS_TO_MATRIX);
        }
        // TODO check long vector
        int[] dims = new int[]{nrow, ncol};
        switch (type) {
            case INTSXP:
                return RDataFactory.createIntVector(new int[nrow * ncol], RDataFactory.COMPLETE_VECTOR, dims);
            case REALSXP:
                return RDataFactory.createDoubleVector(new double[nrow * ncol], RDataFactory.COMPLETE_VECTOR, dims);
            case LGLSXP:
                return RDataFactory.createLogicalVector(new byte[nrow * ncol], RDataFactory.COMPLETE_VECTOR, dims);
            case CHARSXP:
                return RDataFactory.createStringVector(new String[nrow * ncol], RDataFactory.COMPLETE_VECTOR, dims);
            case CPLXSXP:
                return RDataFactory.createComplexVector(new double[2 * (nrow * ncol)], RDataFactory.COMPLETE_VECTOR, dims);
            default:
                throw RInternalError.unimplemented();
        }
    }

    static int LENGTH(Object x) {
        if (x instanceof RAbstractContainer) {
            return ((RAbstractContainer) x).getLength();
        } else if (x instanceof Integer || x instanceof Double || x instanceof Byte || x instanceof String) {
            return 1;
        } else {
            throw RInternalError.unimplemented();
        }
    }

    static void SET_STRING_ELT(Object x, int i, Object v) {
        // TODO error checks
        RStringVector xv = (RStringVector) x;
        xv.setElement(i, v);
    }

    static void SET_VECTOR_ELT(Object x, int i, Object v) {
        // TODO error checks
        RList list = (RList) x;
        list.setElement(i, v);
    }

    static byte[] RAW(Object x) {
        if (x instanceof RRawVector) {
            return ((RRawVector) x).getDataWithoutCopying();
        } else {
            throw RInternalError.unimplemented();
        }

    }

    static byte[] LOGICAL(Object x) {
        if (x instanceof RLogicalVector) {
            return ((RLogicalVector) x).getDataWithoutCopying();
        } else {
            throw RInternalError.unimplemented();
        }

    }

    static int[] INTEGER(Object x) {
        if (x instanceof RIntVector) {
            return ((RIntVector) x).getDataWithoutCopying();
        } else {
            throw RInternalError.unimplemented();
        }
    }

    static double[] REAL(Object x) {
        if (x instanceof RDoubleVector) {
            return ((RDoubleVector) x).getDataWithoutCopying();
        } else {
            throw RInternalError.unimplemented();
        }
    }

    static String STRING_ELT(Object x, int i) {
        if (x instanceof String) {
            assert i == 0;
            return (String) x;
        } else if (x instanceof RStringVector) {
            return ((RStringVector) x).getDataAt(i);
        } else {
            throw RInternalError.unimplemented();
        }
    }

    static Object VECTOR_ELT(Object x, int i) {
        if (x instanceof RList) {
            return ((RList) x).getDataAt(i);
        } else {
            throw RInternalError.unimplemented();
        }
    }

    static int NAMED(Object x) {
        if (x instanceof RShareable) {
            return ((RShareable) x).isShared() ? 1 : 0;
        } else {
            throw RInternalError.unimplemented();
        }
    }

    static Object Rf_duplicate(Object x) {
        if (x instanceof RAbstractVector) {
            return ((RAbstractVector) x).copy();
        } else {
            throw RInternalError.unimplemented();
        }
    }

    static Object CAR(Object e) {
        if (e instanceof RPairList) {
            return ((RPairList) e).car();
        } else {
            throw RInternalError.unimplemented();
        }
    }

    static Object CDR(Object e) {
        if (e instanceof RPairList) {
            return ((RPairList) e).cdr();
        } else {
            throw RInternalError.unimplemented();
        }
    }

    static Object CADR(@SuppressWarnings("unused") Object x) {
        throw RInternalError.unimplemented();
    }

    static Object SETCAR(Object x, Object y) {
        if (x instanceof RPairList) {
            ((RPairList) x).setCar(y);
            return x; // TODO check or y?
        } else {
            throw RInternalError.unimplemented();
        }
    }

    static Object SETCDR(Object x, Object y) {
        if (x instanceof RPairList) {
            ((RPairList) x).setCdr(y);
            return x; // TODO check or y?
        } else {
            throw RInternalError.unimplemented();
        }
    }

    // Checkstyle: resume method name check

    static Object validate(Object x) {
        return x;
    }

    static Object getGlobalEnv() {
        return RContext.getInstance().stateREnvironment.getGlobalEnv();
    }

    static Object getBaseEnv() {
        return RContext.getInstance().stateREnvironment.getBaseEnv();
    }

    static Object getBaseNamespace() {
        return RContext.getInstance().stateREnvironment.getBaseNamespace();
    }

    static Object getNamespaceRegistry() {
        return RContext.getInstance().stateREnvironment.getNamespaceRegistry();
    }

    static int isInteractive() {
        return RContext.getInstance().isInteractive() ? 1 : 0;
    }

    static int isS4Object(Object x) {
        return x instanceof RS4Object ? 1 : 0;
    }
}
