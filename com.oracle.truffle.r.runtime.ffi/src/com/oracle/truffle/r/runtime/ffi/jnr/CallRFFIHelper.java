/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.charset.StandardCharsets;

import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RErrorHandling;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.context.Engine.ParseException;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RAttributes;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RComplexVector;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDataFrame;
import com.oracle.truffle.r.runtime.data.RDoubleSequence;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RExpression;
import com.oracle.truffle.r.runtime.data.RExternalPtr;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RIntSequence;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RLanguage;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.data.RRaw;
import com.oracle.truffle.r.runtime.data.RRawVector;
import com.oracle.truffle.r.runtime.data.RS4Object;
import com.oracle.truffle.r.runtime.data.RShareable;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.data.RUnboundValue;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.env.REnvironment.PutException;
import com.oracle.truffle.r.runtime.gnur.SEXPTYPE;

/**
 * This class provides methods that match the functionality of the macro/function definitions in the
 * R header files, e.g. {@code Rinternals.h} that are used by C/C++ code. For ease of
 * identification, we use method names that, as far as possible, match the names in the header
 * files. These methods should never be called from normal FastR code.
 */
public class CallRFFIHelper {

    private static final class CharSXPWrapper {
        private final String contents;

        CharSXPWrapper(String contents) {
            this.contents = contents;
        }

        public String getContents() {
            return contents;
        }

        @Override
        public String toString() {
            return "CHARSXP(" + contents + ")";
        }
    }

    public static Object createCharSXP(String contents) {
        return new CharSXPWrapper(contents);
    }

    private static RuntimeException unimplemented() {
        return unimplemented("");
    }

    private static RuntimeException unimplemented(String message) {
        System.out.println(message);
        try {
            throw RInternalError.unimplemented(message);
        } catch (Error e) {
            e.printStackTrace();
            try {
                Thread.sleep(100000);
            } catch (InterruptedException e2) {
                e2.printStackTrace();
            }
            throw e;
        }
    }

    private static void guarantee(boolean condition) {
        guarantee(condition, "");
    }

    private static void guarantee(boolean condition, String message) {
        if (!condition) {
            unimplemented(message);
        }
    }

    private static <T> T guaranteeInstanceOf(Object x, Class<T> clazz) {
        if (x == null) {
            guarantee(false, "unexpected type: null instead of " + clazz.getSimpleName());
        } else if (!clazz.isInstance(x)) {
            guarantee(false, "unexpected type: " + x + " is " + x.getClass().getSimpleName() + " instead of " + clazz.getSimpleName());
        }
        return clazz.cast(x);
    }

    // Checkstyle: stop method name check

    public static RIntVector Rf_ScalarInteger(int value) {
        return RDataFactory.createIntVectorFromScalar(value);
    }

    public static RLogicalVector Rf_ScalarLogical(int value) {
        return RDataFactory.createLogicalVectorFromScalar(value != 0);
    }

    public static RDoubleVector Rf_ScalarDouble(double value) {
        return RDataFactory.createDoubleVectorFromScalar(value);
    }

    public static RStringVector Rf_ScalarString(Object value) {
        CharSXPWrapper chars = guaranteeInstanceOf(value, CharSXPWrapper.class);
        return RDataFactory.createStringVectorFromScalar(chars.getContents());
    }

    public static int Rf_asInteger(Object x) {
        if (x instanceof Integer) {
            return ((Integer) x).intValue();
        } else if (x instanceof Double) {
            return RRuntime.double2int((Double) x);
        } else if (x instanceof Byte) {
            return RRuntime.logical2int((Byte) x);
        } else {
            guaranteeInstanceOf(x, RIntVector.class);
            return ((RIntVector) x).getDataAt(0);
        }
    }

    public static double Rf_asReal(Object x) {
        if (x instanceof Double) {
            return ((Double) x).doubleValue();
        } else if (x instanceof Byte) {
            return RRuntime.logical2double((Byte) x);
        } else {
            guaranteeInstanceOf(x, RDoubleVector.class);
            return ((RDoubleVector) x).getDataAt(0);
        }
    }

    public static int Rf_asLogical(Object x) {
        if (x instanceof Byte) {
            return ((Byte) x).intValue();
        } else {
            guaranteeInstanceOf(x, RLogicalVector.class);
            return ((RLogicalVector) x).getDataAt(0);
        }
    }

    public static Object Rf_asChar(Object x) {
        if (x instanceof CharSXPWrapper) {
            return x;
        } else if (x instanceof RSymbol) {
            return new CharSXPWrapper(((RSymbol) x).getName());
        }

        Object obj = RRuntime.asAbstractVector(x);
        if (obj instanceof RAbstractVector) {
            RAbstractVector vector = (RAbstractVector) obj;
            if (vector.getLength() > 0) {
                if (vector instanceof RAbstractStringVector) {
                    return new CharSXPWrapper(((RAbstractStringVector) vector).getDataAt(0));
                } else {
                    unimplemented("asChar type " + x.getClass());
                }
            }
        }

        return new CharSXPWrapper(RRuntime.STRING_NA);
    }

    public static Object Rf_mkCharLenCE(byte[] bytes, @SuppressWarnings("unused") int encoding) {
        // TODO: handle encoding properly
        return new CharSXPWrapper(new String(bytes, StandardCharsets.UTF_8));
    }

    public static Object Rf_cons(Object car, Object cdr) {
        return RDataFactory.createPairList(car, cdr);
    }

    public static void Rf_defineVar(Object symbolArg, Object value, Object envArg) {
        REnvironment env = (REnvironment) envArg;
        RSymbol name = (RSymbol) symbolArg;
        try {
            env.put(name.getName(), value);
        } catch (PutException ex) {
            throw RError.error(RError.SHOW_CALLER2, ex);
        }
    }

    public static Object Rf_findVar(Object symbolArg, Object envArg) {
        return findVarInFrameHelper(symbolArg, envArg, true);
    }

    public static Object Rf_findVarInFrame(Object symbolArg, Object envArg) {
        return findVarInFrameHelper(symbolArg, envArg, false);
    }

    private static Object findVarInFrameHelper(Object symbolArg, Object envArg, boolean inherits) {
        if (envArg == RNull.instance) {
            throw RError.error(RError.SHOW_CALLER2, RError.Message.USE_NULL_ENV_DEFUNCT);
        }
        if (!(envArg instanceof REnvironment)) {
            throw RError.error(RError.SHOW_CALLER2, RError.Message.ARG_NOT_AN_ENVIRONMENT, inherits ? "findVar" : "findVarInFrame");
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

    public static Object Rf_getAttrib(Object obj, Object name) {
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

    public static void Rf_setAttrib(Object obj, Object name, Object val) {
        if (obj instanceof RAttributable) {
            RAttributable attrObj = (RAttributable) obj;
            String nameAsString;
            if (name instanceof RSymbol) {
                nameAsString = ((RSymbol) name).getName();
            } else {
                nameAsString = RRuntime.asString(name);
                assert nameAsString != null;
            }
            nameAsString = nameAsString.intern();
            if ("class" == nameAsString) {
                attrObj.initAttributes().put(nameAsString, val);
            } else {
                attrObj.setAttr(nameAsString, val);
            }
        }
    }

    public static RStringVector getClassHr(Object v) {
        if (v instanceof RAttributable) {
            return ((RAttributable) v).getClassHierarchy();
        } else if (v instanceof Byte) {
            return RLogicalVector.implicitClassHeader;
        } else if (v instanceof String) {
            return RStringVector.implicitClassHeader;
        } else if (v instanceof Integer) {
            return RIntVector.implicitClassHeader;
        } else if (v instanceof Double) {
            return RDoubleVector.implicitClassHeader;
        } else if (v instanceof RComplex) {
            return RComplexVector.implicitClassHeader;
        } else if (v instanceof RRaw) {
            return RRawVector.implicitClassHeader;
        } else {
            guaranteeInstanceOf(v, RNull.class);
            return RNull.implicitClassHeader;
        }
    }

    public static int Rf_inherits(Object x, String clazz) {
        RStringVector hierarchy = getClassHr(x);
        for (int i = 0; i < hierarchy.getLength(); i++) {
            if (hierarchy.getDataAt(i).equals(clazz)) {
                return 1;
            }
        }
        return 0;
    }

    public static Object Rf_lengthgets(Object x, int newSize) {
        RAbstractVector vec = (RAbstractVector) RRuntime.asAbstractVector(x);
        return vec.resize(newSize);
    }

    public static int Rf_isString(Object x) {
        return RRuntime.checkType(x, RType.Character) ? 1 : 0;
    }

    public static int Rf_isNull(Object x) {
        return x == RNull.instance ? 1 : 0;
    }

    public static Object Rf_PairToVectorList(Object x) {
        if (x == RNull.instance) {
            return RDataFactory.createList();
        }
        RPairList pl = (RPairList) x;
        return pl.toRList();
    }

    public static void Rf_error(String msg) {
        throw RError.error(RError.SHOW_CALLER2, RError.Message.GENERIC, msg);
    }

    public static void Rf_warning(String msg) {
        RError.warning(RError.SHOW_CALLER2, RError.Message.GENERIC, msg);
    }

    public static void Rf_warningcall(Object call, String msg) {
        RErrorHandling.warningcallRFFI(call, msg);
    }

    public static Object Rf_allocateVector(int mode, int n) {
        SEXPTYPE type = SEXPTYPE.mapInt(mode);
        if (n < 0) {
            throw RError.error(RError.SHOW_CALLER2, RError.Message.NEGATIVE_LENGTH_VECTORS_NOT_ALLOWED);
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
            case RAWSXP:
                return RDataFactory.createRawVector(new byte[n]);
            case VECSXP:
                return RDataFactory.createList(n);
            default:
                throw unimplemented("unexpected SEXPTYPE " + type);
        }
    }

    public static Object Rf_allocateArray(int mode, Object dimsObj) {
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

    public static Object Rf_allocateMatrix(int mode, int ncol, int nrow) {
        SEXPTYPE type = SEXPTYPE.mapInt(mode);
        if (nrow < 0 || ncol < 0) {
            throw RError.error(RError.SHOW_CALLER2, RError.Message.NEGATIVE_EXTENTS_TO_MATRIX);
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
            case STRSXP:
                return RDataFactory.createStringVector(new String[nrow * ncol], RDataFactory.COMPLETE_VECTOR, dims);
            case CPLXSXP:
                return RDataFactory.createComplexVector(new double[2 * (nrow * ncol)], RDataFactory.COMPLETE_VECTOR, dims);
            default:
                throw unimplemented();
        }
    }

    public static int LENGTH(Object x) {
        if (x instanceof RAbstractContainer) {
            return ((RAbstractContainer) x).getLength();
        } else if (x == RNull.instance) {
            return 0;
        } else if (x instanceof CharSXPWrapper) {
            return ((CharSXPWrapper) x).getContents().length();
        } else if (x instanceof Integer || x instanceof Double || x instanceof Byte || x instanceof String) {
            return 1;
        } else {
            throw unimplemented("unexpected value: " + x);
        }
    }

    public static void SET_STRING_ELT(Object x, int i, Object v) {
        RStringVector vector = guaranteeInstanceOf(x, RStringVector.class);
        CharSXPWrapper element = guaranteeInstanceOf(v, CharSXPWrapper.class);
        vector.setElement(i, element.getContents());
    }

    public static void SET_VECTOR_ELT(Object x, int i, Object v) {
        RList list = guaranteeInstanceOf(x, RList.class);
        list.setElement(i, v);
    }

    public static byte[] RAW(Object x) {
        if (x instanceof RRawVector) {
            return ((RRawVector) x).getDataWithoutCopying();
        } else if (x instanceof RRaw) {
            return new byte[]{((RRaw) x).getValue()};
        } else {
            throw unimplemented();
        }
    }

    private static int toWideLogical(byte v) {
        return RRuntime.isNA(v) ? Integer.MIN_VALUE : v;
    }

    public static int[] LOGICAL(Object x) {
        if (x instanceof RLogicalVector) {
            // TODO: this should not actually copy...
            RLogicalVector vector = (RLogicalVector) x;
            int[] array = new int[vector.getLength()];
            for (int i = 0; i < vector.getLength(); i++) {
                array[i] = toWideLogical(vector.getDataAt(i));
            }
            return array;
        } else if (x instanceof Byte) {
            return new int[]{toWideLogical((Byte) x)};
        } else {
            throw unimplemented();
        }
    }

    public static int[] INTEGER(Object x) {
        if (x instanceof RIntVector) {
            return ((RIntVector) x).getDataWithoutCopying();
        } else if (x instanceof RIntSequence) {
            return ((RIntSequence) x).materialize().getDataWithoutCopying();
        } else if (x instanceof Integer) {
            return new int[]{(Integer) x};
        } else if (x instanceof RLogicalVector) {
            RLogicalVector vec = (RLogicalVector) x;
            int[] result = new int[vec.getLength()];
            for (int i = 0; i < result.length; i++) {
                result[i] = vec.getDataAt(i);
            }
            return result;
        } else {
            guaranteeInstanceOf(x, Byte.class);
            return new int[]{(Byte) x};
        }
    }

    public static double[] REAL(Object x) {
        if (x instanceof RDoubleVector) {
            return ((RDoubleVector) x).getDataWithoutCopying();
        } else if (x instanceof RDoubleSequence) {
            return ((RDoubleSequence) x).materialize().getDataWithoutCopying();
        } else {
            guaranteeInstanceOf(x, Double.class);
            return new double[]{(Double) x};
        }
    }

    public static void logObject(Object x) {
        System.out.println("object " + x);
        System.out.println("class " + x.getClass());
    }

    public static Object STRING_ELT(Object x, int i) {
        RAbstractStringVector vector = guaranteeInstanceOf(RRuntime.asAbstractVector(x), RAbstractStringVector.class);
        return new CharSXPWrapper(vector.getDataAt(i));
    }

    public static Object VECTOR_ELT(Object x, int i) {
        Object vec = x;
        if (vec instanceof RDataFrame) {
            vec = ((RDataFrame) vec).getVector();
        }
        RAbstractListVector list = guaranteeInstanceOf(RRuntime.asAbstractVector(vec), RAbstractListVector.class);
        return list.getDataAt(i);
    }

    public static int NAMED(Object x) {
        if (x instanceof RShareable) {
            return ((RShareable) x).isShared() ? 1 : 0;
        } else {
            throw unimplemented();
        }
    }

    public static int TYPEOF(Object x) {
        if (x instanceof CharSXPWrapper) {
            return SEXPTYPE.CHARSXP.code;
        } else {
            return SEXPTYPE.gnuRCodeForObject(x);
        }
    }

    public static Object Rf_duplicate(Object x) {
        guaranteeInstanceOf(x, RAbstractVector.class);
        return ((RAbstractVector) x).copy();
    }

    public static Object PRINTNAME(Object x) {
        guaranteeInstanceOf(x, RSymbol.class);
        return new CharSXPWrapper(((RSymbol) x).getName());
    }

    public static Object TAG(Object e) {
        if (e instanceof RPairList) {
            return ((RPairList) e).getTag();
        } else {
            guaranteeInstanceOf(e, RExternalPtr.class);
            // at the moment, this can only be used to null out the pointer
            return ((RExternalPtr) e).getTag();
        }
    }

    public static Object CAR(Object e) {
        guaranteeInstanceOf(e, RPairList.class);
        Object car = ((RPairList) e).car();
        return car;
    }

    public static Object CDR(Object e) {
        guaranteeInstanceOf(e, RPairList.class);
        Object cdr = ((RPairList) e).cdr();
        return cdr;
    }

    public static Object CADR(Object e) {
        guaranteeInstanceOf(e, RPairList.class);
        Object cadr = ((RPairList) e).cadr();
        return cadr;
    }

    public static Object SET_TAG(Object x, Object y) {
        if (x instanceof RPairList) {
            ((RPairList) x).setTag(y);
        } else {
            guaranteeInstanceOf(x, RExternalPtr.class);
            // at the moment, this can only be used to null out the pointer
            ((RExternalPtr) x).setTag(y);
        }
        return x; // TODO check or y?
    }

    public static Object SETCAR(Object x, Object y) {
        guaranteeInstanceOf(x, RPairList.class);
        ((RPairList) x).setCar(y);
        return x; // TODO check or y?
    }

    public static Object SETCDR(Object x, Object y) {
        guaranteeInstanceOf(x, RPairList.class);
        ((RPairList) x).setCdr(y);
        return x; // TODO check or y?
    }

    public static Object R_FindNamespace(Object name) {
        Object result = RContext.getInstance().stateREnvironment.getNamespaceRegistry().get(RRuntime.asString(name));
        return result;
    }

    public static Object Rf_eval(Object expr, Object env) {
        guarantee(env instanceof REnvironment);
        Object result;
        if (expr instanceof RPromise) {
            result = RContext.getRRuntimeASTAccess().forcePromise(expr);
        } else if (expr instanceof RExpression) {
            result = RContext.getEngine().eval((RExpression) expr, (REnvironment) env, 0);
        } else if (expr instanceof RLanguage) {
            result = RContext.getEngine().eval((RLanguage) expr, (REnvironment) env, 0);
        } else {
            // just return value
            result = expr;
        }
        return result;
    }

    public static Object Rf_findfun(Object symbolObj, Object envObj) {
        guarantee(envObj instanceof REnvironment);
        REnvironment env = (REnvironment) envObj;
        guarantee(symbolObj instanceof RSymbol);
        RSymbol symbol = (RSymbol) symbolObj;
        // Works but not remotely efficient
        Source source = Source.fromNamedText("get(\"" + symbol.getName() + "\", mode=\"function\")", "<Rf_findfun>");
        try {
            Object result = RContext.getEngine().parseAndEval(source, env.getFrame(), false);
            return result;
        } catch (ParseException ex) {
            throw RInternalError.shouldNotReachHere(ex);
        }
    }

    public static Object Rf_GetOption1(Object tag) {
        guarantee(tag instanceof RSymbol);
        Object result = RContext.getInstance().stateROptions.getValue(((RSymbol) tag).getName());
        return result;
    }

    public static void Rf_gsetVar(Object symbol, Object value, Object rho) {
        guarantee(symbol instanceof RSymbol);
        REnvironment baseEnv = RContext.getInstance().stateREnvironment.getBaseEnv();
        guarantee(rho == baseEnv);
        try {
            baseEnv.put(((RSymbol) symbol).getName(), value);
        } catch (PutException e) {
            e.printStackTrace();
        }
    }

    public static void DUPLICATE_ATTRIB(Object to, Object from) {
        if (from instanceof RAttributable) {
            guaranteeInstanceOf(to, RAttributable.class);
            RAttributes attributes = ((RAttributable) from).getAttributes();
            ((RAttributable) to).initAttributes(attributes == null ? null : attributes.copy());
        }
        // TODO: copy OBJECT? and S4 attributes
    }

    public static REnvironment Rf_createNewEnv(REnvironment parent, String name, boolean hashed, int initialSize) {
        REnvironment env = RDataFactory.createNewEnv(name, hashed, initialSize);
        RArguments.initializeEnclosingFrame(env.getFrame(), parent.getFrame());
        return env;
    }

    public static int R_computeIdentical(Object x, Object y, int flags) {
        RFunction indenticalBuiltin = RContext.lookupBuiltin("identical");
        Object res = RContext.getEngine().evalFunction(indenticalBuiltin, null, x, y, RRuntime.asLogical((!((flags & 1) == 0))), RRuntime.asLogical((!((flags & 2) == 0))),
                        RRuntime.asLogical((!((flags & 4) == 0))), RRuntime.asLogical((!((flags & 8) == 0))), RRuntime.asLogical((!((flags & 16) == 0))));
        return (int) res;
    }

    @SuppressWarnings("unused")
    public static void Rf_copyListMatrix(Object s, Object t, int byrow) {
        throw unimplemented();
    }

    @SuppressWarnings("unused")
    public static void Rf_copyMatrix(Object s, Object t, int byrow) {
        throw unimplemented();
    }

    // Checkstyle: resume method name check

    public static Object validate(Object x) {
        return x;
    }

    public static Object getGlobalEnv() {
        return RContext.getInstance().stateREnvironment.getGlobalEnv();
    }

    public static Object getBaseEnv() {
        return RContext.getInstance().stateREnvironment.getBaseEnv();
    }

    public static Object getBaseNamespace() {
        return RContext.getInstance().stateREnvironment.getBaseNamespace();
    }

    public static Object getNamespaceRegistry() {
        return RContext.getInstance().stateREnvironment.getNamespaceRegistry();
    }

    public static int isInteractive() {
        return RContext.getInstance().isInteractive() ? 1 : 0;
    }

    public static int isS4Object(Object x) {
        return x instanceof RS4Object ? 1 : 0;
    }

    public static void printf(String message) {
        RContext.getInstance().getConsoleHandler().print(message);
    }
}
