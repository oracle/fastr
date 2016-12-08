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
package com.oracle.truffle.r.runtime.ffi;

import java.nio.charset.StandardCharsets;
import java.util.function.Function;

import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameInstance.FrameAccess;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RCaller;
import com.oracle.truffle.r.runtime.RCleanUp;
import com.oracle.truffle.r.runtime.REnvVars;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RErrorHandling;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RSource;
import com.oracle.truffle.r.runtime.RSrcref;
import com.oracle.truffle.r.runtime.RStartParams.SA_TYPE;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.context.Engine.ParseException;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RAttributesLayout;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RComplexVector;
import com.oracle.truffle.r.runtime.data.RDataFactory;
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
import com.oracle.truffle.r.runtime.data.RVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.env.REnvironment.PutException;
import com.oracle.truffle.r.runtime.ffi.DLL.SymbolHandle;
import com.oracle.truffle.r.runtime.ffi.ParseResult.ParseStatus;
import com.oracle.truffle.r.runtime.ffi.jni.TraceUpCallsAdapter;
import com.oracle.truffle.r.runtime.gnur.SEXPTYPE;
import com.oracle.truffle.r.runtime.nodes.DuplicationHelper;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.rng.RRNG;

/**
 * This class provides a simple Java-based implementation of {@link UpCallsRFFI}, using no Truffle
 * mechanisms.
 *
 * TODO Many of the implementations here are incomplete and/or duplicate code that exists in the
 * Truffle side of the implementation, i.e., {@link RNode} subclasses. A complete refactoring that
 * accesses the Truffle implementations (possibly somewhat refactored owing to the fact that the
 * Truffle side is driven by the builtins yet these functions don't not always map 1-1 to a builtin)
 * is desirable. In some cases it may be possible to "implement" the functions in R (which is a
 * simple way to achieve the above).
 */
public class JavaUpCallsRFFI implements UpCallsRFFI {

    private TraceUpCallsAdapter tracer;

    public JavaUpCallsRFFI() {
        if (RFFIUtils.traceEnabled()) {
            tracer = new TraceUpCallsAdapter();
        }
    }

    private static RuntimeException unimplemented() {
        return unimplemented("");
    }

    private static RuntimeException unimplemented(String message) {
        System.err.println(message);
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

    @Override
    public RIntVector Rf_ScalarInteger(int value) {
        if (tracer != null) {
            tracer.Rf_ScalarInteger(value);
        }
        return RDataFactory.createIntVectorFromScalar(value);
    }

    @Override
    public RLogicalVector Rf_ScalarLogical(int value) {
        if (tracer != null) {
            tracer.Rf_ScalarLogical(value);
        }
        return RDataFactory.createLogicalVectorFromScalar(value != 0);
    }

    @Override
    public RDoubleVector Rf_ScalarDouble(double value) {
        if (tracer != null) {
            tracer.Rf_ScalarDouble(value);
        }
        return RDataFactory.createDoubleVectorFromScalar(value);
    }

    @Override
    public RStringVector Rf_ScalarString(Object value) {
        if (tracer != null) {
            tracer.Rf_ScalarString(value);
        }
        CharSXPWrapper chars = guaranteeInstanceOf(value, CharSXPWrapper.class);
        return RDataFactory.createStringVectorFromScalar(chars.getContents());
    }

    @Override
    public int Rf_asInteger(Object x) {
        if (tracer != null) {
            tracer.Rf_asInteger(x);
        }
        // TODO this is quite incomplete and really should be implemented with CastIntegerNode
        if (x instanceof Integer) {
            return ((Integer) x).intValue();
        } else if (x instanceof Double) {
            return RRuntime.double2int((Double) x);
        } else if (x instanceof Byte) {
            return RRuntime.logical2int((Byte) x);
        } else if (x instanceof RLogicalVector) {
            return RRuntime.logical2int(((RLogicalVector) x).getDataAt(0));
        } else {
            guaranteeInstanceOf(x, RIntVector.class);
            return ((RIntVector) x).getDataAt(0);
        }
    }

    @Override
    public double Rf_asReal(Object x) {
        if (tracer != null) {
            tracer.Rf_asReal(x);
        }
        if (x instanceof Double) {
            return ((Double) x).doubleValue();
        } else if (x instanceof Byte) {
            return RRuntime.logical2double((Byte) x);
        } else {
            guaranteeInstanceOf(x, RDoubleVector.class);
            return ((RDoubleVector) x).getDataAt(0);
        }
    }

    @Override
    public int Rf_asLogical(Object x) {
        if (tracer != null) {
            tracer.Rf_asLogical(x);
        }
        if (x instanceof Byte) {
            return ((Byte) x).intValue();
        } else {
            guaranteeInstanceOf(x, RLogicalVector.class);
            return ((RLogicalVector) x).getDataAt(0);
        }
    }

    @Override
    public Object Rf_asChar(Object x) {
        if (tracer != null) {
            tracer.Rf_asChar(x);
        }
        if (x instanceof CharSXPWrapper) {
            return x;
        } else if (x instanceof RSymbol) {
            return CharSXPWrapper.create(((RSymbol) x).getName());
        }

        Object obj = RRuntime.asAbstractVector(x);
        if (obj instanceof RAbstractVector) {
            RAbstractVector vector = (RAbstractVector) obj;
            if (vector.getLength() > 0) {
                if (vector instanceof RAbstractStringVector) {
                    return CharSXPWrapper.create(((RAbstractStringVector) vector).getDataAt(0));
                } else {
                    unimplemented("asChar type " + x.getClass());
                }
            }
        }

        return CharSXPWrapper.create(RRuntime.STRING_NA);
    }

    @Override
    public Object Rf_mkCharLenCE(byte[] bytes, int encoding) {
        if (tracer != null) {
            tracer.Rf_mkCharLenCE(bytes, encoding);
        }
        // TODO: handle encoding properly
        return CharSXPWrapper.create(new String(bytes, StandardCharsets.UTF_8));
    }

    @Override
    public Object Rf_cons(Object car, Object cdr) {
        if (tracer != null) {
            tracer.Rf_cons(car, cdr);
        }
        return RDataFactory.createPairList(car, cdr);
    }

    @Override
    public void Rf_defineVar(Object symbolArg, Object value, Object envArg) {
        if (tracer != null) {
            tracer.Rf_defineVar(symbolArg, value, envArg);
        }
        REnvironment env = (REnvironment) envArg;
        RSymbol name = (RSymbol) symbolArg;
        try {
            env.put(name.getName(), value);
        } catch (PutException ex) {
            throw RError.error(RError.SHOW_CALLER2, ex);
        }
    }

    @Override
    public Object R_do_MAKE_CLASS(String clazz) {
        if (tracer != null) {
            tracer.R_do_MAKE_CLASS(clazz);
        }
        String name = "getClass";
        RFunction getClass = (RFunction) RContext.getRRuntimeASTAccess().forcePromise(name, REnvironment.getRegisteredNamespace("methods").get(name));
        return RContext.getEngine().evalFunction(getClass, null, RCaller.createInvalid(null), null, clazz);
    }

    @Override
    public Object Rf_findVar(Object symbolArg, Object envArg) {
        if (tracer != null) {
            tracer.Rf_findVar(symbolArg, envArg);
        }
        return findVarInFrameHelper(envArg, symbolArg, true);
    }

    @Override
    public Object Rf_findVarInFrame(Object envArg, Object symbolArg) {
        if (tracer != null) {
            tracer.Rf_findVarInFrame(envArg, symbolArg);
        }
        return findVarInFrameHelper(envArg, symbolArg, false);
    }

    @Override
    public Object Rf_findVarInFrame3(Object envArg, Object symbolArg, int doGet) {
        if (tracer != null) {
            tracer.Rf_findVarInFrame3(envArg, symbolArg, doGet);
        }
        // GNU R has code for IS_USER_DATBASE that uses doGet
        // This is a lookup in the single environment (envArg) only, i.e. inherits=false
        return findVarInFrameHelper(envArg, symbolArg, false);
    }

    private static Object findVarInFrameHelper(Object envArg, Object symbolArg, boolean inherits) {
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
                // simgle frame lookup
                break;
            }
            env = env.getParent();
        }
        return RUnboundValue.instance;
    }

    @Override
    public Object Rf_getAttrib(Object obj, Object name) {
        if (tracer != null) {
            tracer.Rf_getAttrib(obj, name);
        }
        Object result = RNull.instance;
        if (obj instanceof RAttributable) {
            RAttributable attrObj = (RAttributable) obj;
            DynamicObject attrs = attrObj.getAttributes();
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

    @Override
    public void Rf_setAttrib(Object obj, Object name, Object val) {
        if (tracer != null) {
            tracer.Rf_setAttrib(obj, name, val);
        }
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
            if (val == RNull.instance) {
                attrObj.removeAttr(nameAsString);
            } else if ("class" == nameAsString) {
                attrObj.initAttributes().define(nameAsString, val);
            } else {
                attrObj.setAttr(nameAsString, val);
            }
        }
    }

    private static RStringVector getClassHr(Object v) {
        RStringVector result;
        if (v instanceof RAttributable) {
            result = ((RAttributable) v).getClassHierarchy();
        } else if (v instanceof Byte) {
            result = RLogicalVector.implicitClassHeader;
        } else if (v instanceof String) {
            result = RStringVector.implicitClassHeader;
        } else if (v instanceof Integer) {
            result = RIntVector.implicitClassHeader;
        } else if (v instanceof Double) {
            result = RDoubleVector.implicitClassHeader;
        } else if (v instanceof RComplex) {
            result = RComplexVector.implicitClassHeader;
        } else if (v instanceof RRaw) {
            result = RRawVector.implicitClassHeader;
        } else {
            guaranteeInstanceOf(v, RNull.class);
            result = RNull.implicitClassHeader;
        }
        return result;
    }

    @Override
    public int Rf_inherits(Object x, String clazz) {
        if (tracer != null) {
            tracer.Rf_inherits(x, clazz);
        }
        int result = 0;
        RStringVector hierarchy = getClassHr(x);
        for (int i = 0; i < hierarchy.getLength(); i++) {
            if (hierarchy.getDataAt(i).equals(clazz)) {
                result = 1;
            }
        }
        return result;
    }

    @Override
    public Object Rf_install(String name) {
        if (tracer != null) {
            tracer.Rf_install(name);
        }
        return RDataFactory.createSymbolInterned(name);
    }

    @Override
    public Object Rf_lengthgets(Object x, int newSize) {
        if (tracer != null) {
            tracer.Rf_lengthgets(x, newSize);
        }
        RAbstractVector vec = (RAbstractVector) RRuntime.asAbstractVector(x);
        return vec.resize(newSize);
    }

    @Override
    public int Rf_isString(Object x) {
        if (tracer != null) {
            tracer.Rf_isString(x);
        }
        return RRuntime.checkType(x, RType.Character) ? 1 : 0;
    }

    @Override
    public int Rf_isNull(Object x) {
        if (tracer != null) {
            tracer.Rf_isNull(x);
        }
        return x == RNull.instance ? 1 : 0;
    }

    @Override
    public Object Rf_PairToVectorList(Object x) {
        if (tracer != null) {
            tracer.Rf_PairToVectorList(x);
        }
        if (x == RNull.instance) {
            return RDataFactory.createList();
        }
        RPairList pl = (RPairList) x;
        return pl.toRList();
    }

    @Override
    public void Rf_error(String msg) {
        if (tracer != null) {
            tracer.Rf_error(msg);
        }
        throw RError.error(RError.SHOW_CALLER2, RError.Message.GENERIC, msg);
    }

    @Override
    public void Rf_warning(String msg) {
        if (tracer != null) {
            tracer.Rf_warning(msg);
        }
        RError.warning(RError.SHOW_CALLER2, RError.Message.GENERIC, msg);
    }

    @Override
    public void Rf_warningcall(Object call, String msg) {
        if (tracer != null) {
            tracer.Rf_warningcall(call, msg);
        }
        RErrorHandling.warningcallRFFI(call, msg);
    }

    @Override
    public Object Rf_allocateVector(int mode, int n) {
        if (tracer != null) {
            tracer.Rf_allocateVector(mode, n);
        }
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
            case LANGSXP:
                return RDataFactory.createLangPairList(n);
            default:
                throw unimplemented("unexpected SEXPTYPE " + type);
        }
    }

    @Override
    public Object Rf_allocateArray(int mode, Object dimsObj) {
        if (tracer != null) {
            tracer.Rf_allocateArray(mode, dimsObj);
        }
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

    @Override
    public Object Rf_allocateMatrix(int mode, int nrow, int ncol) {
        if (tracer != null) {
            tracer.Rf_allocateMatrix(mode, ncol, nrow);
        }
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

    @Override
    public int Rf_nrows(Object x) {
        if (tracer != null) {
            tracer.Rf_nrows(x);
        }
        return RRuntime.nrows(x);
    }

    @Override
    public int Rf_ncols(Object x) {
        if (tracer != null) {
            tracer.Rf_ncols(x);
        }
        return RRuntime.ncols(x);
    }

    @Override
    public int LENGTH(Object x) {
        if (tracer != null) {
            tracer.LENGTH(x);
        }
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

    @Override
    public void SET_STRING_ELT(Object x, int i, Object v) {
        if (tracer != null) {
            tracer.SET_STRING_ELT(x, i, v);
        }
        RStringVector vector = guaranteeInstanceOf(x, RStringVector.class);
        CharSXPWrapper element = guaranteeInstanceOf(v, CharSXPWrapper.class);
        vector.setElement(i, element.getContents());
    }

    @Override
    public void SET_VECTOR_ELT(Object x, int i, Object v) {
        if (tracer != null) {
            tracer.SET_VECTOR_ELT(x, i, v);
        }
        RList list = guaranteeInstanceOf(x, RList.class);
        list.setElement(i, v);
    }

    @Override
    public Object RAW(Object x) {
        if (tracer != null) {
            tracer.RAW(x);
        }
        if (x instanceof RRawVector) {
            return ((RRawVector) x).getDataWithoutCopying();
        } else if (x instanceof RRaw) {
            return new byte[]{((RRaw) x).getValue()};
        } else {
            throw unimplemented();
        }
    }

    @Override
    public Object LOGICAL(Object x) {
        if (tracer != null) {
            tracer.LOGICAL(x);
        }
        if (x instanceof RLogicalVector) {
            return ((RLogicalVector) x).getDataWithoutCopying();
        } else if (x instanceof Byte) {
            return new byte[]{(Byte) x};
        } else {
            throw unimplemented();
        }
    }

    @Override
    public Object INTEGER(Object x) {
        if (tracer != null) {
            tracer.INTEGER(x);
        }
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

    @Override
    public Object REAL(Object x) {
        if (tracer != null) {
            tracer.REAL(x);
        }
        if (x instanceof RDoubleVector) {
            return ((RDoubleVector) x).getDataWithoutCopying();
        } else if (x instanceof RDoubleSequence) {
            return ((RDoubleSequence) x).materialize().getDataWithoutCopying();
        } else {
            guaranteeInstanceOf(x, Double.class);
            return new double[]{(Double) x};
        }
    }

    @Override
    public Object STRING_ELT(Object x, int i) {
        if (tracer != null) {
            tracer.STRING_ELT(x, i);
        }
        RAbstractStringVector vector = guaranteeInstanceOf(RRuntime.asAbstractVector(x), RAbstractStringVector.class);
        return CharSXPWrapper.create(vector.getDataAt(i));
    }

    @Override
    public Object VECTOR_ELT(Object x, int i) {
        if (tracer != null) {
            tracer.VECTOR_ELT(x, i);
        }
        Object vec = x;
        if (vec instanceof RExpression) {
            return ((RExpression) vec).getDataAt(i);
        }
        RAbstractListVector list = guaranteeInstanceOf(RRuntime.asAbstractVector(vec), RAbstractListVector.class);
        return list.getDataAt(i);
    }

    @Override
    public int NAMED(Object x) {
        if (tracer != null) {
            tracer.NAMED(x);
        }
        if (x instanceof RShareable) {
            return ((RShareable) x).isShared() ? 1 : 0;
        } else {
            throw unimplemented();
        }
    }

    @Override
    public Object SET_TYPEOF_FASTR(Object x, int v) {
        if (tracer != null) {
            tracer.SET_TYPEOF_FASTR(x, v);
        }
        int code = SEXPTYPE.gnuRCodeForObject(x);
        if (code == SEXPTYPE.LISTSXP.code && v == SEXPTYPE.LANGSXP.code) {
            return RLanguage.fromList(x, RLanguage.RepType.CALL);
        } else {
            throw unimplemented();
        }
    }

    @Override
    public int TYPEOF(Object x) {
        if (tracer != null) {
            tracer.TYPEOF(x);
        }
        if (x instanceof CharSXPWrapper) {
            return SEXPTYPE.CHARSXP.code;
        } else {
            return SEXPTYPE.gnuRCodeForObject(x);
        }
    }

    @Override
    public int OBJECT(Object x) {
        if (tracer != null) {
            tracer.OBJECT(x);
        }
        if (x instanceof RAttributable) {
            return ((RAttributable) x).getAttr(RRuntime.CLASS_ATTR_KEY) == null ? 0 : 1;
        } else {
            return 0;
        }
    }

    @Override
    public Object Rf_duplicate(Object x, int deep) {
        if (tracer != null) {
            tracer.Rf_duplicate(x, deep);
        }
        guarantee(x != null, "unexpected type: null instead of " + x.getClass().getSimpleName());
        guarantee(x instanceof RShareable || x instanceof RIntSequence || x instanceof RExternalPtr,
                        "unexpected type: " + x + " is " + x.getClass().getSimpleName() + " instead of RShareable or RExternalPtr");
        if (x instanceof RShareable) {
            return deep == 1 ? ((RShareable) x).deepCopy() : ((RShareable) x).copy();
        } else if (x instanceof RIntSequence) {
            return ((RIntSequence) x).materialize();
        } else {
            return ((RExternalPtr) x).copy();
        }
    }

    @Override
    public int Rf_anyDuplicated(Object x, int fromLast) {
        if (tracer != null) {
            tracer.Rf_anyDuplicated(x, fromLast);
        }
        RAbstractVector vec = (RAbstractVector) x;
        if (vec.getLength() == 0) {
            return 0;
        } else {
            return DuplicationHelper.analyze(vec, null, true, fromLast != 0).getIndex();
        }
    }

    @Override
    public Object PRINTNAME(Object x) {
        if (tracer != null) {
            tracer.PRINTNAME(x);
        }
        guaranteeInstanceOf(x, RSymbol.class);
        return CharSXPWrapper.create(((RSymbol) x).getName());
    }

    @Override
    public Object TAG(Object e) {
        if (tracer != null) {
            tracer.TAG(e);
        }
        if (e instanceof RPairList) {
            return ((RPairList) e).getTag();
        } else {
            guaranteeInstanceOf(e, RExternalPtr.class);
            // at the moment, this can only be used to null out the pointer
            return ((RExternalPtr) e).getTag();
        }
    }

    @Override
    public Object CAR(Object e) {
        if (tracer != null) {
            tracer.CAR(e);
        }
        guarantee(e != null && (RPairList.class.isInstance(e) || RLanguage.class.isInstance(e)), "CAR only works on pair lists and language objects");
        if (e instanceof RPairList) {
            return ((RPairList) e).car();
        } else {
            return ((RLanguage) e).getDataAtAsObject(0);
        }
    }

    @Override
    public Object CDR(Object e) {
        if (tracer != null) {
            tracer.CDR(e);
        }
        if (e instanceof RLanguage) {
            RLanguage lang = (RLanguage) e;
            RPairList l = lang.getPairList();
            return l.cdr();
        } else {
            guaranteeInstanceOf(e, RPairList.class);
            return ((RPairList) e).cdr();
        }
    }

    @Override
    public Object CADR(Object e) {
        if (tracer != null) {
            tracer.CADR(e);
        }
        guarantee(e != null && (RPairList.class.isInstance(e) || RLanguage.class.isInstance(e)), "CADR only works on pair lists and language objects");
        if (e instanceof RPairList) {
            return ((RPairList) e).cadr();
        } else {
            return ((RLanguage) e).getDataAtAsObject(1);
        }
    }

    @Override
    public Object CADDR(Object e) {
        if (tracer != null) {
            tracer.CADDR(e);
        }
        guarantee(e != null && (RPairList.class.isInstance(e) || RLanguage.class.isInstance(e)), "CADDR only works on pair lists and language objects");
        if (e instanceof RPairList) {
            return ((RPairList) e).caddr();
        } else {
            return ((RLanguage) e).getDataAtAsObject(2);
        }
    }

    @Override
    public Object CDDR(Object e) {
        if (tracer != null) {
            tracer.CDDR(e);
        }
        if (e instanceof RLanguage) {
            RLanguage lang = (RLanguage) e;
            RPairList l = lang.getPairList();
            return l.cddr();
        } else {
            guaranteeInstanceOf(e, RPairList.class);
            return ((RPairList) e).cddr();
        }
    }

    @Override
    public Object SET_TAG(Object x, Object y) {
        if (tracer != null) {
            tracer.SET_TAG(x, y);
        }
        if (x instanceof RPairList) {
            ((RPairList) x).setTag(y);
        } else {
            guaranteeInstanceOf(x, RExternalPtr.class);
            // at the moment, this can only be used to null out the pointer
            ((RExternalPtr) x).setTag(y);
        }
        return y;
    }

    @Override
    public Object SETCAR(Object x, Object y) {
        if (tracer != null) {
            tracer.SETCAR(x, y);
        }
        guaranteeInstanceOf(x, RPairList.class);
        ((RPairList) x).setCar(y);
        return y;
    }

    @Override
    public Object SETCDR(Object x, Object y) {
        if (tracer != null) {
            tracer.SETCDR(x, y);
        }
        guaranteeInstanceOf(x, RPairList.class);
        ((RPairList) x).setCdr(y);
        return y;
    }

    @Override
    public Object SETCADR(Object x, Object y) {
        if (tracer != null) {
            tracer.SETCADR(x, y);
        }
        SETCAR(CDR(x), y);
        return y;
    }

    @Override
    public Object SYMVALUE(Object x) {
        if (tracer != null) {
            tracer.SYMVALUE(x);
        }
        if (!(x instanceof RSymbol)) {
            throw RInternalError.shouldNotReachHere();
        }
        Object res = REnvironment.baseEnv().get(((RSymbol) x).getName());
        if (res == null) {
            return RUnboundValue.instance;
        } else {
            return res;
        }
    }

    @Override
    public void SET_SYMVALUE(Object x, Object v) {
        if (tracer != null) {
            tracer.SET_SYMVALUE(x, v);
        }
        if (!(x instanceof RSymbol)) {
            throw RInternalError.shouldNotReachHere();
        }
        REnvironment.baseEnv().safePut(((RSymbol) x).getName(), v);
    }

    @Override
    public int R_BindingIsLocked(Object sym, Object env) {
        if (tracer != null) {
            tracer.R_BindingIsLocked(sym, env);
        }
        guaranteeInstanceOf(sym, RSymbol.class);
        guaranteeInstanceOf(env, REnvironment.class);
        return ((REnvironment) env).bindingIsLocked(((RSymbol) sym).getName()) ? 1 : 0;
    }

    @Override
    public Object R_FindNamespace(Object name) {
        if (tracer != null) {
            tracer.R_FindNamespace(name);
        }
        Object result = RContext.getInstance().stateREnvironment.getNamespaceRegistry().get(RRuntime.asString(name));
        return result;
    }

    @Override
    public Object Rf_eval(Object expr, Object env) {
        if (tracer != null) {
            tracer.Rf_eval(expr, env);
        }
        guarantee(env instanceof REnvironment);
        Object result;
        if (expr instanceof RPromise) {
            result = RContext.getRRuntimeASTAccess().forcePromise(null, expr);
        } else if (expr instanceof RExpression) {
            result = RContext.getEngine().eval((RExpression) expr, (REnvironment) env, RCaller.topLevel);
        } else if (expr instanceof RLanguage) {
            result = RContext.getEngine().eval((RLanguage) expr, (REnvironment) env, RCaller.topLevel);
        } else if (expr instanceof RPairList) {
            RPairList l = (RPairList) expr;
            RFunction f = (RFunction) l.car();
            Object args = l.cdr();
            if (args == RNull.instance) {
                result = RContext.getEngine().evalFunction(f, env == REnvironment.globalEnv() ? null : ((REnvironment) env).getFrame(), RCaller.topLevel, null, new Object[0]);
            } else {
                RList argsList = ((RPairList) args).toRList();
                result = RContext.getEngine().evalFunction(f, env == REnvironment.globalEnv() ? null : ((REnvironment) env).getFrame(), RCaller.topLevel, argsList.getNames(),
                                argsList.getDataNonShared());
            }

        } else {
            // just return value
            result = expr;
        }
        return result;
    }

    @Override
    public Object Rf_findfun(Object symbolObj, Object envObj) {
        if (tracer != null) {
            tracer.Rf_findfun(symbolObj, envObj);
        }
        guarantee(envObj instanceof REnvironment);
        REnvironment env = (REnvironment) envObj;
        guarantee(symbolObj instanceof RSymbol);
        RSymbol symbol = (RSymbol) symbolObj;
        // Works but not remotely efficient
        Source source = RSource.fromTextInternal("get(\"" + symbol.getName() + "\", mode=\"function\")", RSource.Internal.RF_FINDFUN);
        try {
            Object result = RContext.getEngine().parseAndEval(source, env.getFrame(), false);
            return result;
        } catch (ParseException ex) {
            throw RInternalError.shouldNotReachHere(ex);
        }
    }

    @Override
    public Object Rf_GetOption1(Object tag) {
        if (tracer != null) {
            tracer.Rf_GetOption1(tag);
        }
        guarantee(tag instanceof RSymbol);
        Object result = RContext.getInstance().stateROptions.getValue(((RSymbol) tag).getName());
        return result;
    }

    @Override
    public void Rf_gsetVar(Object symbol, Object value, Object rho) {
        if (tracer != null) {
            tracer.Rf_gsetVar(symbol, value, rho);
        }
        guarantee(symbol instanceof RSymbol);
        REnvironment baseEnv = RContext.getInstance().stateREnvironment.getBaseEnv();
        guarantee(rho == baseEnv);
        try {
            baseEnv.put(((RSymbol) symbol).getName(), value);
        } catch (PutException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void DUPLICATE_ATTRIB(Object to, Object from) {
        if (tracer != null) {
            tracer.DUPLICATE_ATTRIB(to, from);
        }
        if (from instanceof RAttributable) {
            guaranteeInstanceOf(to, RAttributable.class);
            DynamicObject attributes = ((RAttributable) from).getAttributes();
            ((RAttributable) to).initAttributes(attributes == null ? null : RAttributesLayout.copy(attributes));
        }
        // TODO: copy OBJECT? and S4 attributes
    }

    @Override
    public int R_computeIdentical(Object x, Object y, int flags) {
        if (tracer != null) {
            tracer.R_computeIdentical(x, y, flags);
        }
        RFunction indenticalBuiltin = RContext.lookupBuiltin("identical");
        Object res = RContext.getEngine().evalFunction(indenticalBuiltin, null, null, null, x, y, RRuntime.asLogical((!((flags & 1) == 0))),
                        RRuntime.asLogical((!((flags & 2) == 0))), RRuntime.asLogical((!((flags & 4) == 0))), RRuntime.asLogical((!((flags & 8) == 0))), RRuntime.asLogical((!((flags & 16) == 0))));
        return (int) res;
    }

    @Override
    public void Rf_copyListMatrix(Object s, Object t, int byrow) {
        if (tracer != null) {
            tracer.Rf_copyListMatrix(s, t, byrow);
        }
        throw unimplemented();
    }

    @Override
    public void Rf_copyMatrix(Object s, Object t, int byrow) {
        if (tracer != null) {
            tracer.Rf_copyMatrix(s, t, byrow);
        }
        throw unimplemented();
    }

    @Override
    public Object R_tryEval(Object expr, Object env, boolean silent) {
        if (tracer != null) {
            tracer.R_tryEval(expr, env, silent);
        }
        Object handlerStack = RErrorHandling.getHandlerStack();
        Object restartStack = RErrorHandling.getRestartStack();
        try {
            // TODO handle silent
            RErrorHandling.resetStacks();
            Object result = Rf_eval(expr, env);
            return result;
        } catch (Throwable t) {
            return null;
        } finally {
            RErrorHandling.restoreStacks(handlerStack, restartStack);
        }
    }

    /**
     * Helper function for {@code R_TopLevelExec} which is similar to {@code R_TryEval} except that
     * a C function is invoked (in the native layer) instead of an R expression. assert: this is
     * ONLY called from R_TopLevelExec prior to calling C function.
     */
    @Override
    public Object R_ToplevelExec() {
        if (tracer != null) {
            tracer.R_ToplevelExec();
        }
        return RErrorHandling.resetAndGetHandlerStacks();
    }

    /**
     * Helper function for {@code R_TopLevelExec}, see {@link #R_ToplevelExec()}, called after C
     * function returns.
     */
    public void R_ToplevelExecRestoreErrorHandlerStacks(Object stacks) {
        RErrorHandling.HandlerStacks handlerStacks = guaranteeInstanceOf(stacks, RErrorHandling.HandlerStacks.class);
        RErrorHandling.restoreHandlerStacks(handlerStacks);
    }

    @Override
    public int RDEBUG(Object x) {
        if (tracer != null) {
            tracer.RDEBUG(x);
        }
        REnvironment env = guaranteeInstanceOf(x, REnvironment.class);
        if (env instanceof REnvironment.Function) {
            REnvironment.Function funcEnv = (REnvironment.Function) env;
            RFunction func = RArguments.getFunction(funcEnv.getFrame());
            return RContext.getRRuntimeASTAccess().isDebugged(func) ? 1 : 0;
        } else {
            return 0;
        }
    }

    @Override
    public void SET_RDEBUG(Object x, int v) {
        if (tracer != null) {
            tracer.SET_RDEBUG(x, v);
        }
        REnvironment env = guaranteeInstanceOf(x, REnvironment.class);
        if (env instanceof REnvironment.Function) {
            REnvironment.Function funcEnv = (REnvironment.Function) env;
            RFunction func = RArguments.getFunction(funcEnv.getFrame());
            if (v == 1) {
                RContext.getRRuntimeASTAccess().enableDebug(func, false);
            } else {
                RContext.getRRuntimeASTAccess().disableDebug(func);
            }
        }
    }

    @Override
    public int RSTEP(Object x) {
        if (tracer != null) {
            tracer.RSTEP(x);
        }
        @SuppressWarnings("unused")
        REnvironment env = guaranteeInstanceOf(x, REnvironment.class);
        throw RInternalError.unimplemented("RSTEP");
    }

    @Override
    public void SET_RSTEP(Object x, int v) {
        if (tracer != null) {
            tracer.SET_RSTEP(x, v);
        }
        @SuppressWarnings("unused")
        REnvironment env = guaranteeInstanceOf(x, REnvironment.class);
        throw RInternalError.unimplemented("SET_RSTEP");
    }

    @Override
    public Object ENCLOS(Object x) {
        if (tracer != null) {
            tracer.ENCLOS(x);
        }
        REnvironment env = guaranteeInstanceOf(x, REnvironment.class);
        Object result = env.getParent();
        if (result == null) {
            result = RNull.instance;
        }
        return result;
    }

    @Override
    public Object PRVALUE(Object x) {
        if (tracer != null) {
            tracer.PRVALUE(x);
        }
        RPromise p = guaranteeInstanceOf(x, RPromise.class);
        return p.isEvaluated() ? p.getValue() : RUnboundValue.instance;
    }

    @Override
    public Object R_ParseVector(Object text, int n, Object srcFile) {
        if (tracer != null) {
            tracer.R_ParseVector(text, n, srcFile);
        }
        // TODO general case
        assert n == 1;
        assert srcFile == RNull.instance;
        String textString = RRuntime.asString(text);
        assert textString != null;

        try {
            Source source = RSource.fromTextInternal(textString, RSource.Internal.R_PARSEVECTOR);
            RExpression exprs = RContext.getEngine().parse(source);
            return new ParseResult(ParseStatus.PARSE_OK.ordinal(), exprs);
        } catch (ParseException ex) {
            // TODO incomplete
            return new ParseResult(ParseStatus.PARSE_ERROR.ordinal(), RNull.instance);
        }

    }

    @Override
    public Object R_lsInternal3(Object envArg, int allArg, int sortedArg) {
        if (tracer != null) {
            tracer.R_lsInternal3(envArg, allArg, sortedArg);
        }
        boolean sorted = sortedArg != 0;
        boolean all = allArg != 0;
        REnvironment env = guaranteeInstanceOf(envArg, REnvironment.class);
        return env.ls(all, null, sorted);
    }

    @Override
    public String R_HomeDir() {
        if (tracer != null) {
            tracer.R_HomeDir();
        }
        return REnvVars.rHome();
    }

    @Override
    public void R_CleanUp(int sa, int status, int runlast) {
        if (tracer != null) {
            tracer.R_CleanUp(sa, status, runlast);
        }
        RCleanUp.stdCleanUp(SA_TYPE.values()[sa], status, runlast != 0);
    }

    @Override
    public Object R_GlobalContext() {
        if (tracer != null) {
            tracer.R_GlobalContext();
        }
        Utils.warn("Potential memory leak (global context object)");
        Frame frame = Utils.getActualCurrentFrame();
        if (frame == null) {
            return RCaller.topLevel;
        }
        if (RContext.getInstance().stateInstrumentation.getBrowserState().inBrowser()) {
            return RContext.getInstance().stateInstrumentation.getBrowserState().getInBrowserCaller();
        }
        RCaller rCaller = RArguments.getCall(frame);
        return rCaller == null ? RCaller.topLevel : rCaller;
    }

    @Override
    public Object R_GlobalEnv() {
        if (tracer != null) {
            tracer.R_GlobalEnv();
        }
        return RContext.getInstance().stateREnvironment.getGlobalEnv();
    }

    @Override
    public Object R_BaseEnv() {
        if (tracer != null) {
            tracer.R_BaseEnv();
        }
        return RContext.getInstance().stateREnvironment.getBaseEnv();
    }

    @Override
    public Object R_BaseNamespace() {
        if (tracer != null) {
            tracer.R_BaseNamespace();
        }
        return RContext.getInstance().stateREnvironment.getBaseNamespace();
    }

    @Override
    public Object R_NamespaceRegistry() {
        if (tracer != null) {
            tracer.R_NamespaceRegistry();
        }
        return RContext.getInstance().stateREnvironment.getNamespaceRegistry();
    }

    @Override
    public int isInteractive() {
        if (tracer != null) {
            tracer.isInteractive();
        }
        return RContext.getInstance().isInteractive() ? 1 : 0;
    }

    @Override
    public int isS4Object(Object x) {
        if (tracer != null) {
            tracer.isS4Object(x);
        }
        return x instanceof RS4Object ? 1 : 0;
    }

    @Override
    public void Rprintf(String message) {
        if (tracer != null) {
            tracer.Rprintf(message);
        }
        RContext.getInstance().getConsoleHandler().print(message);
    }

    @Override
    public void GetRNGstate() {
        if (tracer != null) {
            tracer.GetRNGstate();
        }
        RRNG.getRNGState();
    }

    @Override
    public void PutRNGstate() {
        if (tracer != null) {
            tracer.PutRNGstate();
        }
        RRNG.putRNGState();
    }

    @Override
    public double unif_rand() {
        if (tracer != null) {
            tracer.unif_rand();
        }
        return RRNG.unifRand();
    }

    // Checkstyle: stop method name check

    @Override
    public Object R_getGlobalFunctionContext() {
        if (tracer != null) {
            tracer.R_getGlobalFunctionContext();
        }
        Utils.warn("Potential memory leak (global function context object)");
        Frame frame = Utils.getActualCurrentFrame();
        if (frame == null) {
            return RNull.instance;
        }
        RCaller currentCaller = RArguments.getCall(frame);
        while (currentCaller != null) {
            if (!currentCaller.isPromise() && currentCaller.isValidCaller() && currentCaller != RContext.getInstance().stateInstrumentation.getBrowserState().getInBrowserCaller()) {
                break;
            }
            currentCaller = currentCaller.getParent();
        }
        return currentCaller == null || currentCaller == RCaller.topLevel ? RNull.instance : currentCaller;
    }

    @Override
    public Object R_getParentFunctionContext(Object c) {
        if (tracer != null) {
            tracer.R_getParentFunctionContext(c);
        }
        Utils.warn("Potential memory leak (parent function context object)");
        RCaller currentCaller = guaranteeInstanceOf(c, RCaller.class);
        while (true) {
            currentCaller = currentCaller.getParent();
            if (currentCaller == null ||
                            (!currentCaller.isPromise() && currentCaller.isValidCaller() && currentCaller != RContext.getInstance().stateInstrumentation.getBrowserState().getInBrowserCaller())) {
                break;
            }
        }
        return currentCaller == null || currentCaller == RCaller.topLevel ? RNull.instance : currentCaller;
    }

    @Override
    public Object R_getContextEnv(Object c) {
        if (tracer != null) {
            tracer.R_getContextEnv(c);
        }
        RCaller rCaller = guaranteeInstanceOf(c, RCaller.class);
        if (rCaller == RCaller.topLevel) {
            return RContext.getInstance().stateREnvironment.getGlobalEnv();
        }
        Frame frame = Utils.getActualCurrentFrame();
        if (RArguments.getCall(frame) == rCaller) {
            return REnvironment.frameToEnvironment(frame.materialize());
        } else {
            Object result = Utils.iterateRFrames(FrameAccess.READ_ONLY, new Function<Frame, Object>() {

                @Override
                public Object apply(Frame f) {
                    RCaller currentCaller = RArguments.getCall(f);
                    if (currentCaller == rCaller) {
                        return REnvironment.frameToEnvironment(f.materialize());
                    } else {
                        return null;
                    }
                }
            });
            return result;
        }
    }

    @Override
    public Object R_getContextFun(Object c) {
        if (tracer != null) {
            tracer.R_getContextFun(c);
        }
        RCaller rCaller = guaranteeInstanceOf(c, RCaller.class);
        if (rCaller == RCaller.topLevel) {
            return RNull.instance;
        }
        Frame frame = Utils.getActualCurrentFrame();
        if (RArguments.getCall(frame) == rCaller) {
            return RArguments.getFunction(frame);
        } else {
            Object result = Utils.iterateRFrames(FrameAccess.READ_ONLY, new Function<Frame, Object>() {

                @Override
                public Object apply(Frame f) {
                    RCaller currentCaller = RArguments.getCall(f);
                    if (currentCaller == rCaller) {
                        return RArguments.getFunction(f);
                    } else {
                        return null;
                    }
                }
            });
            return result;
        }
    }

    @Override
    public Object R_getContextCall(Object c) {
        if (tracer != null) {
            tracer.R_getContextCall(c);
        }
        RCaller rCaller = guaranteeInstanceOf(c, RCaller.class);
        if (rCaller == RCaller.topLevel) {
            return RNull.instance;
        }
        return RContext.getRRuntimeASTAccess().getSyntaxCaller(rCaller);
    }

    @Override
    public Object R_getContextSrcRef(Object c) {
        if (tracer != null) {
            tracer.R_getContextSrcRef(c);
        }
        Object o = R_getContextFun(c);
        if (!(o instanceof RFunction)) {
            return RNull.instance;
        } else {
            RFunction f = (RFunction) o;
            SourceSection ss = f.getRootNode().getSourceSection();
            String path = RSource.getPath(ss.getSource());
            // TODO: is it OK to pass "" if path is null?
            return RSrcref.createLloc(ss, path == null ? "" : path);
        }

    }

    @Override
    public int R_insideBrowser() {
        if (tracer != null) {
            tracer.R_insideBrowser();
        }
        return RContext.getInstance().stateInstrumentation.getBrowserState().inBrowser() ? 1 : 0;
    }

    @Override
    public int R_isGlobal(Object c) {
        if (tracer != null) {
            tracer.R_isGlobal(c);
        }
        RCaller rCaller = guaranteeInstanceOf(c, RCaller.class);

        return rCaller == RCaller.topLevel ? 1 : 0;
    }

    @Override
    public int R_isEqual(Object x, Object y) {
        if (tracer != null) {
            tracer.R_isEqual(x, y);
        }
        return x == y ? 1 : 0;
    }

    @Override
    public Object Rf_classgets(Object x, Object y) {
        if (tracer != null) {
            tracer.Rf_classgets(x, y);
        }
        RAbstractVector vector = guaranteeInstanceOf(x, RAbstractVector.class);
        vector.setClassAttr(guaranteeInstanceOf(y, RStringVector.class));
        return RNull.instance;
    }

    @Override
    public RExternalPtr R_MakeExternalPtr(long addr, Object tag, Object prot) {
        if (tracer != null) {
            tracer.R_MakeExternalPtr(addr, tag, prot);
        }
        return RDataFactory.createExternalPtr(new SymbolHandle(addr), tag, prot);
    }

    @Override
    public long R_ExternalPtrAddr(Object x) {
        if (tracer != null) {
            tracer.R_ExternalPtrAddr(x);
        }
        RExternalPtr p = guaranteeInstanceOf(x, RExternalPtr.class);
        return p.getAddr().asAddress();
    }

    @Override
    public Object R_ExternalPtrTag(Object x) {
        if (tracer != null) {
            tracer.R_ExternalPtrTag(x);
        }
        RExternalPtr p = guaranteeInstanceOf(x, RExternalPtr.class);
        return p.getTag();
    }

    @Override
    public Object R_ExternalPtrProt(Object x) {
        if (tracer != null) {
            tracer.R_ExternalPtrProt(x);
        }
        RExternalPtr p = guaranteeInstanceOf(x, RExternalPtr.class);
        return p.getProt();
    }

    @Override
    public void R_SetExternalPtrAddr(Object x, long addr) {
        if (tracer != null) {
            tracer.R_SetExternalPtrAddr(x, addr);
        }
        RExternalPtr p = guaranteeInstanceOf(x, RExternalPtr.class);
        p.setAddr(new SymbolHandle(addr));
    }

    @Override
    public void R_SetExternalPtrTag(Object x, Object tag) {
        if (tracer != null) {
            tracer.R_SetExternalPtrTag(x, tag);
        }
        RExternalPtr p = guaranteeInstanceOf(x, RExternalPtr.class);
        p.setTag(tag);
    }

    @Override
    public void R_SetExternalPtrProt(Object x, Object prot) {
        if (tracer != null) {
            tracer.R_SetExternalPtrProt(x, prot);
        }
        RExternalPtr p = guaranteeInstanceOf(x, RExternalPtr.class);
        p.setProt(prot);
    }

    @Override
    public REnvironment R_NewHashedEnv(REnvironment parent, String name, boolean hashed, int initialSize) {
        if (tracer != null) {
            tracer.R_NewHashedEnv(parent, name, hashed, initialSize);
        }
        REnvironment env = RDataFactory.createNewEnv(name, hashed, initialSize);
        RArguments.initializeEnclosingFrame(env.getFrame(), parent.getFrame());
        return env;
    }

    // Implementation specific support

    /**
     * Called to possibly update the "complete" status on {@code x}. N.B. {@code x} may not be an
     * object with a concrete {@code setComplete} method, e.g. see {@link #INTEGER(Object)}.
     */
    public void setComplete(Object x, boolean complete) {
        // only care about concrete vectors
        if (x instanceof RVector) {
            ((RVector<?>) x).setComplete(complete);
        }
    }

    /**
     * Called when a {@link CharSXPWrapper} is expected and not found.
     */
    public void logNotCharSXPWrapper(Object x) {
        System.out.println("object " + x);
        System.out.println("class " + x.getClass());
    }

}
