/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.ffi.impl.common;

import static com.oracle.truffle.r.ffi.impl.common.RFFIUtils.guarantee;
import static com.oracle.truffle.r.ffi.impl.common.RFFIUtils.guaranteeInstanceOf;
import static com.oracle.truffle.r.ffi.impl.common.RFFIUtils.unimplemented;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameInstance.FrameAccess;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.ffi.impl.common.ParseResult.ParseStatus;
import com.oracle.truffle.r.ffi.impl.nodes.FFIUpCallRootNode;
import com.oracle.truffle.r.ffi.impl.upcalls.RFFIUpCallTable;
import com.oracle.truffle.r.ffi.impl.upcalls.UpCallsRFFI;
import com.oracle.truffle.r.nodes.RASTUtils;
import com.oracle.truffle.r.nodes.function.ClassHierarchyNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
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
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.BaseRConnection;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.InvalidConnection;
import com.oracle.truffle.r.runtime.conn.NativeConnections.NativeRConnection;
import com.oracle.truffle.r.runtime.conn.RConnection;
import com.oracle.truffle.r.runtime.context.Engine.ParseException;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RAttributesLayout;
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
import com.oracle.truffle.r.runtime.data.RSequence;
import com.oracle.truffle.r.runtime.data.RShareable;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.data.RTypedValue;
import com.oracle.truffle.r.runtime.data.RUnboundValue;
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractListBaseVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractRawVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.env.REnvironment.PutException;
import com.oracle.truffle.r.runtime.ffi.CharSXPWrapper;
import com.oracle.truffle.r.runtime.ffi.DLL.SymbolHandle;
import com.oracle.truffle.r.runtime.gnur.SA_TYPE;
import com.oracle.truffle.r.runtime.gnur.SEXPTYPE;
import com.oracle.truffle.r.runtime.nodes.DuplicationHelper;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;
import com.oracle.truffle.r.runtime.rng.RRNG;

/**
 * This class provides a simple Java-based implementation of {@link UpCallsRFFI}, where all the
 * argument values are standard Java types, i.e. no special types used by Truffle NFI or Truffle
 * LLVM.
 *
 * TODO Many of the implementations here are incomplete and/or duplicate code that exists in the
 * Truffle side of the implementation, i.e., {@link RNode} subclasses. A complete refactoring that
 * accesses the Truffle implementations (possibly somewhat refactored owing to the fact that the
 * Truffle side is driven by the builtins yet these functions don't not always map 1-1 to a builtin)
 * is desirable. In some cases it may be possible to "implement" the functions in R (which is a
 * simple way to achieve the above).
 */
public abstract class JavaUpCallsRFFIImpl implements UpCallsRFFI {

    private final Map<String, Object> nameSymbolCache = new ConcurrentHashMap<>();

    // Checkstyle: stop method name check

    @Override
    public RIntVector Rf_ScalarInteger(int value) {
        return RDataFactory.createIntVectorFromScalar(value);
    }

    @Override
    public RLogicalVector Rf_ScalarLogical(int value) {
        byte byteValue;
        if (value == RRuntime.INT_NA) {
            byteValue = RRuntime.LOGICAL_NA;
        } else {
            byteValue = (byte) (value & 0xFF);
        }
        return RDataFactory.createLogicalVectorFromScalar(byteValue);
    }

    @Override
    public RDoubleVector Rf_ScalarDouble(double value) {
        return RDataFactory.createDoubleVectorFromScalar(value);
    }

    @Override
    public RStringVector Rf_ScalarString(Object value) {
        CharSXPWrapper chars = guaranteeInstanceOf(value, CharSXPWrapper.class);
        return RDataFactory.createStringVectorFromScalar(chars.getContents());
    }

    @Override
    public int Rf_asInteger(Object x) {
        return (int) FFIUpCallRootNode.getCallTarget(RFFIUpCallTable.Rf_asInteger).call(x);
    }

    @Override
    public double Rf_asReal(Object x) {
        return (double) FFIUpCallRootNode.getCallTarget(RFFIUpCallTable.Rf_asReal).call(x);
    }

    @Override
    public int Rf_asLogical(Object x) {
        return (int) FFIUpCallRootNode.getCallTarget(RFFIUpCallTable.Rf_asLogical).call(x);
    }

    @Override
    public Object Rf_asChar(Object x) {
        return FFIUpCallRootNode.getCallTarget(RFFIUpCallTable.Rf_asChar).call(x);
    }

    @Override
    public Object Rf_coerceVector(Object x, int mode) {
        return FFIUpCallRootNode.getCallTarget(RFFIUpCallTable.Rf_coerceVector).call(x, mode);
    }

    @Override
    public Object Rf_mkCharLenCE(Object bytes, int len, int encoding) {
        // TODO: handle encoding properly
        return CharSXPWrapper.create(new String((byte[]) bytes, StandardCharsets.UTF_8));
    }

    @Override
    public Object Rf_cons(Object car, Object cdr) {
        return RDataFactory.createPairList(car, cdr);
    }

    @Override
    public int Rf_defineVar(Object symbolArg, Object value, Object envArg) {
        REnvironment env = (REnvironment) envArg;
        RSymbol name = (RSymbol) symbolArg;
        try {
            env.put(name.getName(), value);
        } catch (PutException ex) {
            throw RError.error(RError.SHOW_CALLER2, ex);
        }
        return 0;
    }

    @Override
    public Object R_do_MAKE_CLASS(Object clazz) {
        String name = "getClass";
        RFunction getClass = (RFunction) RContext.getRRuntimeASTAccess().forcePromise(name, REnvironment.getRegisteredNamespace("methods").get(name));
        return RContext.getEngine().evalFunction(getClass, null, RCaller.createInvalid(null), true, null, clazz);
    }

    @Override
    public Object R_do_new_object(Object classDef) {
        return FFIUpCallRootNode.getCallTarget(RFFIUpCallTable.R_do_new_object).call(classDef);
    }

    @Override
    public Object Rf_findVar(Object symbolArg, Object envArg) {
        return findVarInFrameHelper(envArg, symbolArg, true);
    }

    @Override
    public Object Rf_findVarInFrame(Object envArg, Object symbolArg) {
        return findVarInFrameHelper(envArg, symbolArg, false);
    }

    @Override
    public Object Rf_findVarInFrame3(Object envArg, Object symbolArg, int doGet) {
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
    public Object ATTRIB(Object obj) {
        if (obj instanceof RAttributable) {
            return ((RAttributable) obj).getAttributes();
        }
        return RNull.instance;
    }

    @Override
    public Object Rf_getAttrib(Object obj, Object name) {
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
    @TruffleBoundary
    public int Rf_setAttrib(Object obj, Object name, Object val) {
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
                removeAttr(attrObj, nameAsString);
            } else if ("class" == nameAsString) {
                attrObj.initAttributes().define(nameAsString, val);
            } else {
                attrObj.setAttr(nameAsString, val);
            }
        } else {
            throw RInternalError.shouldNotReachHere();
        }
        return 0;
    }

    @TruffleBoundary
    private static void removeAttr(RAttributable a, String name) {
        a.removeAttr(name);
    }

    public static RStringVector getClassHr(Object v) {
        return ClassHierarchyNode.getClassHierarchy(v);
    }

    @Override
    public int Rf_inherits(Object x, Object clazz) {
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
    public Object Rf_install(Object name) {
        String nameStr = (String) name;
        Object ret = nameSymbolCache.get(nameStr);
        if (ret == null) {
            ret = RDataFactory.createSymbolInterned(nameStr);
            nameSymbolCache.put(nameStr, ret);
        }
        return ret;
    }

    @Override
    public Object Rf_installChar(Object name) {
        CharSXPWrapper charSXP = guaranteeInstanceOf(name, CharSXPWrapper.class);
        return RDataFactory.createSymbolInterned(charSXP.getContents());
    }

    @Override
    public Object Rf_lengthgets(Object x, int newSize) {
        RAbstractVector vec = (RAbstractVector) RRuntime.asAbstractVector(x);
        return vec.resize(newSize);
    }

    @Override
    public int Rf_isString(Object x) {
        return RRuntime.checkType(x, RType.Character) ? 1 : 0;
    }

    @Override
    public int Rf_isNull(Object x) {
        return x == RNull.instance ? 1 : 0;
    }

    @Override
    public Object Rf_PairToVectorList(Object x) {
        if (x == RNull.instance) {
            return RDataFactory.createList();
        }
        RPairList pl = (RPairList) x;
        return pl.toRList();
    }

    @Override
    public int Rf_error(Object msg) {
        throw RError.error(RError.SHOW_CALLER2, RError.Message.GENERIC, msg);
    }

    @Override
    public int Rf_warning(Object msg) {
        RError.warning(RError.SHOW_CALLER2, RError.Message.GENERIC, msg);
        return 0;
    }

    @Override
    public int Rf_warningcall(Object call, Object msg) {
        RErrorHandling.warningcallRFFI(call, (String) msg);
        return 0;
    }

    @Override
    public Object Rf_allocVector(int mode, int n) {
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
                // fill list with empty strings
                String[] data = new String[n];
                Arrays.fill(data, "");
                return RDataFactory.createStringVector(data, RDataFactory.COMPLETE_VECTOR);
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
    public Object Rf_allocArray(int mode, Object dimsObj) {
        RIntVector dims = (RIntVector) dimsObj;
        int n = 1;
        int[] newDims = new int[dims.getLength()];
        // TODO check long vector
        for (int i = 0; i < newDims.length; i++) {
            newDims[i] = dims.getDataAt(i);
            n *= newDims[i];
        }
        RAbstractVector result = (RAbstractVector) Rf_allocVector(mode, n);
        setDims(newDims, result);
        return result;

    }

    @TruffleBoundary
    private static void setDims(int[] newDims, RAbstractVector result) {
        result.setDimensions(newDims);
    }

    @Override
    public Object Rf_allocMatrix(int mode, int nrow, int ncol) {
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
                String[] data = new String[nrow * ncol];
                Arrays.fill(data, "");
                return RDataFactory.createStringVector(data, RDataFactory.COMPLETE_VECTOR, dims);
            case CPLXSXP:
                return RDataFactory.createComplexVector(new double[2 * (nrow * ncol)], RDataFactory.COMPLETE_VECTOR, dims);
            default:
                throw unimplemented();
        }
    }

    @Override
    public int Rf_nrows(Object x) {
        return RRuntime.nrows(x);
    }

    @Override
    public int Rf_ncols(Object x) {
        return RRuntime.ncols(x);
    }

    @Override
    public int LENGTH(Object x) {
        return (int) FFIUpCallRootNode.getCallTarget(RFFIUpCallTable.LENGTH).call(x);
    }

    @Override
    public int SET_STRING_ELT(Object x, int i, Object v) {
        RStringVector vector = guaranteeInstanceOf(x, RStringVector.class);
        CharSXPWrapper element = guaranteeInstanceOf(v, CharSXPWrapper.class);
        String value = element.getContents();
        if (RRuntime.isNA(value)) {
            vector.setComplete(false);
        }
        vector.setElement(i, value);
        return 0;
    }

    @Override
    public int SET_VECTOR_ELT(Object x, int i, Object v) {
        RList list = guaranteeInstanceOf(x, RList.class);
        list.setElement(i, v);
        return 0;
    }

    @Override
    public Object RAW(Object x) {
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
        RAbstractStringVector vector = guaranteeInstanceOf(RRuntime.asAbstractVector(x), RAbstractStringVector.class);
        return CharSXPWrapper.create(vector.getDataAt(i));
    }

    @Override
    public Object VECTOR_ELT(Object x, int i) {
        Object vec = x;
        if (vec instanceof RExpression) {
            return ((RExpression) vec).getDataAt(i);
        }
        RAbstractListVector list = guaranteeInstanceOf(RRuntime.asAbstractVector(vec), RAbstractListVector.class);
        return list.getDataAt(i);
    }

    @Override
    public int NAMED(Object x) {
        if (x instanceof RShareable) {
            return ((RShareable) x).isShared() ? 1 : 0;
        } else {
            throw unimplemented();
        }
    }

    @Override
    public Object SET_TYPEOF_FASTR(Object x, int v) {
        int code = SEXPTYPE.gnuRCodeForObject(x);
        if (code == SEXPTYPE.LISTSXP.code && v == SEXPTYPE.LANGSXP.code) {
            return RLanguage.fromList(x, RLanguage.RepType.CALL);
        } else {
            throw unimplemented();
        }
    }

    @Override
    public int TYPEOF(Object x) {
        if (x instanceof CharSXPWrapper) {
            return SEXPTYPE.CHARSXP.code;
        } else {
            return SEXPTYPE.gnuRCodeForObject(x);
        }
    }

    @Override
    @TruffleBoundary
    public int OBJECT(Object x) {
        if (x instanceof RAttributable) {
            return ((RAttributable) x).getAttr(RRuntime.CLASS_ATTR_KEY) == null ? 0 : 1;
        } else {
            return 0;
        }
    }

    @Override
    public Object Rf_duplicate(Object x, int deep) {
        guarantee(x != null, "unexpected type: null instead of " + x.getClass().getSimpleName());
        guarantee(x instanceof RShareable || x instanceof RSequence || x instanceof RExternalPtr,
                        "unexpected type: " + x + " is " + x.getClass().getSimpleName() + " instead of RShareable or RExternalPtr");
        if (x instanceof RShareable) {
            return deep == 1 ? ((RShareable) x).deepCopy() : ((RShareable) x).copy();
        } else if (x instanceof RSequence) {
            return ((RSequence) x).materialize();
        } else {
            return ((RExternalPtr) x).copy();
        }
    }

    @Override
    public int Rf_any_duplicated(Object x, int fromLast) {
        RAbstractVector vec = (RAbstractVector) x;
        if (vec.getLength() == 0) {
            return 0;
        } else {
            return DuplicationHelper.analyze(vec, null, true, fromLast != 0).getIndex();
        }
    }

    @Override
    public Object PRINTNAME(Object x) {
        guaranteeInstanceOf(x, RSymbol.class);
        return CharSXPWrapper.create(((RSymbol) x).getName());
    }

    @Override
    public Object TAG(Object e) {
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
        return FFIUpCallRootNode.getCallTarget(RFFIUpCallTable.CAR).call(e);
    }

    @Override
    public Object CDR(Object e) {
        Object result = FFIUpCallRootNode.getCallTarget(RFFIUpCallTable.CDR).call(e);
        return result;
    }

    @Override
    public Object CADR(Object e) {
        return FFIUpCallRootNode.getCallTarget(RFFIUpCallTable.CADR).call(e);
    }

    @Override
    public Object CADDR(Object e) {
        return FFIUpCallRootNode.getCallTarget(RFFIUpCallTable.CADDR).call(e);
    }

    @Override
    public Object CDDR(Object e) {
        return FFIUpCallRootNode.getCallTarget(RFFIUpCallTable.CDDR).call(e);
    }

    @Override
    public Object SET_TAG(Object x, Object y) {
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
        guaranteeInstanceOf(x, RPairList.class);
        ((RPairList) x).setCar(y);
        return y;
    }

    @Override
    public Object SETCDR(Object x, Object y) {
        guaranteeInstanceOf(x, RPairList.class);
        ((RPairList) x).setCdr(y);
        return y;
    }

    @Override
    public Object SETCADR(Object x, Object y) {
        SETCAR(CDR(x), y);
        return y;
    }

    @Override
    public Object SYMVALUE(Object x) {
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
    public int SET_SYMVALUE(Object x, Object v) {
        if (!(x instanceof RSymbol)) {
            throw RInternalError.shouldNotReachHere();
        }
        REnvironment.baseEnv().safePut(((RSymbol) x).getName(), v);
        return 0;
    }

    @Override
    public int R_BindingIsLocked(Object sym, Object env) {
        guaranteeInstanceOf(sym, RSymbol.class);
        guaranteeInstanceOf(env, REnvironment.class);
        return ((REnvironment) env).bindingIsLocked(((RSymbol) sym).getName()) ? 1 : 0;
    }

    @Override
    public Object R_FindNamespace(Object name) {
        Object result = RContext.getInstance().stateREnvironment.getNamespaceRegistry().get(RRuntime.asString(name));
        return result;
    }

    @Override
    public Object Rf_eval(Object expr, Object env) {
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
                result = RContext.getEngine().evalFunction(f, env == REnvironment.globalEnv() ? null : ((REnvironment) env).getFrame(), RCaller.topLevel, true, null, new Object[0]);
            } else {
                RList argsList = ((RPairList) args).toRList();
                result = RContext.getEngine().evalFunction(f, env == REnvironment.globalEnv() ? null : ((REnvironment) env).getFrame(), RCaller.topLevel, true,
                                ArgumentsSignature.fromNamesAttribute(argsList.getNames()),
                                argsList.getDataNonShared());
            }
        } else {
            // just return value
            result = expr;
        }
        return result;
    }

    @Override
    public Object Rf_findFun(Object symbolObj, Object envObj) {
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
        guarantee(tag instanceof RSymbol);
        Object result = RContext.getInstance().stateROptions.getValue(((RSymbol) tag).getName());
        return result;
    }

    @Override
    public int Rf_gsetVar(Object symbol, Object value, Object rho) {
        guarantee(symbol instanceof RSymbol);
        REnvironment baseEnv = RContext.getInstance().stateREnvironment.getBaseEnv();
        guarantee(rho == baseEnv);
        try {
            baseEnv.put(((RSymbol) symbol).getName(), value);
        } catch (PutException e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public int DUPLICATE_ATTRIB(Object to, Object from) {
        if (from instanceof RAttributable) {
            guaranteeInstanceOf(to, RAttributable.class);
            DynamicObject attributes = ((RAttributable) from).getAttributes();
            ((RAttributable) to).initAttributes(attributes == null ? null : RAttributesLayout.copy(attributes));
        }
        // TODO: copy OBJECT? and S4 attributes
        return 0;
    }

    @Override
    @TruffleBoundary
    public int R_compute_identical(Object x, Object y, int flags) {
        RFunction indenticalBuiltin = RContext.getInstance().lookupBuiltin("identical");
        Object res = RContext.getEngine().evalFunction(indenticalBuiltin, null, null, true, null, x, y, RRuntime.asLogical((!((flags & 1) == 0))),
                        RRuntime.asLogical((!((flags & 2) == 0))), RRuntime.asLogical((!((flags & 4) == 0))), RRuntime.asLogical((!((flags & 8) == 0))), RRuntime.asLogical((!((flags & 16) == 0))));
        return (int) res;
    }

    @Override
    public int Rf_copyListMatrix(Object t, Object s, int byrow) {
        throw unimplemented();
    }

    @Override
    public int Rf_copyMatrix(Object t, Object s, int byRow) {
        int tRows = RRuntime.nrows(t);
        int tCols = RRuntime.ncols(t);
        final Object sav = RRuntime.asAbstractVector(s);
        ContainerItemCopier c;
        if (sav instanceof RAbstractContainer) {
            int sLen = ((RAbstractContainer) sav).getLength();
            if (sav instanceof RAbstractIntVector) {
                c = new ContainerItemCopier() {
                    private final RAbstractIntVector sv = (RAbstractIntVector) sav;
                    private final RAbstractIntVector tv = (RAbstractIntVector) t;
                    private final Object tvStore = tv.getInternalStore();

                    @Override
                    public void copy(int sIdx, int tIdx) {
                        tv.setDataAt(tvStore, tIdx, sv.getDataAt(sIdx));
                    }
                };
            } else if (sav instanceof RAbstractDoubleVector) {
                c = new ContainerItemCopier() {
                    private final RAbstractDoubleVector sv = (RAbstractDoubleVector) sav;
                    private final RAbstractDoubleVector tv = (RAbstractDoubleVector) t;
                    private final Object tvStore = tv.getInternalStore();

                    @Override
                    public void copy(int sIdx, int tIdx) {
                        tv.setDataAt(tvStore, tIdx, sv.getDataAt(sIdx));
                    }
                };
            } else if (sav instanceof RAbstractLogicalVector) {
                c = new ContainerItemCopier() {
                    private final RAbstractLogicalVector sv = (RAbstractLogicalVector) sav;
                    private final RAbstractLogicalVector tv = (RAbstractLogicalVector) t;
                    private final Object tvStore = tv.getInternalStore();

                    @Override
                    public void copy(int sIdx, int tIdx) {
                        tv.setDataAt(tvStore, tIdx, sv.getDataAt(sIdx));
                    }
                };
            } else if (sav instanceof RAbstractComplexVector) {
                c = new ContainerItemCopier() {
                    private final RAbstractComplexVector sv = (RAbstractComplexVector) sav;
                    private final RAbstractComplexVector tv = (RAbstractComplexVector) t;
                    private final Object tvStore = tv.getInternalStore();

                    @Override
                    public void copy(int sIdx, int tIdx) {
                        tv.setDataAt(tvStore, tIdx, sv.getDataAt(sIdx));
                    }
                };
            } else if (sav instanceof RAbstractStringVector) {
                c = new ContainerItemCopier() {
                    private final RAbstractStringVector sv = (RAbstractStringVector) sav;
                    private final RAbstractStringVector tv = (RAbstractStringVector) t;
                    private final Object tvStore = tv.getInternalStore();

                    @Override
                    public void copy(int sIdx, int tIdx) {
                        tv.setDataAt(tvStore, tIdx, sv.getDataAt(sIdx));
                    }
                };
            } else if (sav instanceof RAbstractRawVector) {
                c = new ContainerItemCopier() {
                    private final RAbstractRawVector sv = (RAbstractRawVector) sav;
                    private final RAbstractRawVector tv = (RAbstractRawVector) t;
                    private final Object tvStore = tv.getInternalStore();

                    @Override
                    public void copy(int sIdx, int tIdx) {
                        tv.setRawDataAt(tvStore, tIdx, sv.getRawDataAt(sIdx));
                    }
                };
            } else if (sav instanceof RAbstractListBaseVector) {
                c = new ContainerItemCopier() {
                    private final RAbstractListBaseVector sv = (RAbstractListBaseVector) sav;
                    private final RAbstractListBaseVector tv = (RAbstractListBaseVector) t;
                    private final Object tvStore = tv.getInternalStore();

                    @Override
                    public void copy(int sIdx, int tIdx) {
                        tv.setDataAt(tvStore, tIdx, sv.getDataAt(sIdx));
                    }
                };
            } else {
                throw unimplemented();
            }
            if (byRow != 0) {
                int sIdx = 0;
                for (int i = 0; i < tRows; i++) {
                    int tIdx = i;
                    for (int j = 0; j < tCols; j++) {
                        c.copy(sIdx, tIdx);
                        sIdx++;
                        if (sIdx >= sLen) {
                            sIdx -= sLen;
                        }
                        tIdx += tRows;
                    }
                }
            } else { // Copy by column
                int tLen = ((RAbstractContainer) t).getLength();
                if (sLen >= tLen) {
                    for (int i = 0; i < tLen; i++) {
                        c.copy(i, i);
                    }
                } else { // Recycle needed
                    int sIdx = 0;
                    for (int i = 0; i < tLen; i++) {
                        c.copy(sIdx, i);
                        if (sIdx >= sLen) {
                            sIdx -= sLen;
                        }
                    }
                }
            }

        } else { // source is non-RAbstractContainer
            throw unimplemented();
        }
        return 0;
    }

    /**
     * Helper interface for {@link #Rf_copyMatrix(java.lang.Object, java.lang.Object, int)} that
     * copies from source index in an (internally held) source container into target index in a
     * target container.
     */
    interface ContainerItemCopier {
        void copy(int sIdx, int tIdx);
    }

    @Override
    public Object R_tryEval(Object expr, Object env, boolean silent) {
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
        return RErrorHandling.resetAndGetHandlerStacks();
    }

    @Override
    public int RDEBUG(Object x) {
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
    public int SET_RDEBUG(Object x, int v) {
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
        return 0;
    }

    @Override
    public int RSTEP(Object x) {
        @SuppressWarnings("unused")
        REnvironment env = guaranteeInstanceOf(x, REnvironment.class);
        throw RInternalError.unimplemented("RSTEP");
    }

    @Override
    public int SET_RSTEP(Object x, int v) {
        @SuppressWarnings("unused")
        REnvironment env = guaranteeInstanceOf(x, REnvironment.class);
        throw RInternalError.unimplemented("SET_RSTEP");
    }

    @Override
    public Object ENCLOS(Object x) {
        REnvironment env = guaranteeInstanceOf(x, REnvironment.class);
        Object result = env.getParent();
        if (result == null) {
            result = RNull.instance;
        }
        return result;
    }

    @Override
    public Object PRVALUE(Object x) {
        RPromise p = guaranteeInstanceOf(x, RPromise.class);
        return p.isEvaluated() ? p.getValue() : RUnboundValue.instance;
    }

    @Override
    public Object R_ParseVector(Object text, int n, Object srcFile) {
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
        boolean sorted = sortedArg != 0;
        boolean all = allArg != 0;
        REnvironment env = guaranteeInstanceOf(envArg, REnvironment.class);
        return env.ls(all, null, sorted);
    }

    @Override
    public String R_HomeDir() {
        return REnvVars.rHome();
    }

    @Override
    public int R_CleanUp(int sa, int status, int runlast) {
        RCleanUp.stdCleanUp(SA_TYPE.values()[sa], status, runlast != 0);
        return 0;
    }

    @Override
    public Object R_GlobalContext() {
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
        return RContext.getInstance().stateREnvironment.getGlobalEnv();
    }

    @Override
    public Object R_BaseEnv() {
        return RContext.getInstance().stateREnvironment.getBaseEnv();
    }

    @Override
    public Object R_BaseNamespace() {
        return RContext.getInstance().stateREnvironment.getBaseNamespace();
    }

    @Override
    public Object R_NamespaceRegistry() {
        return RContext.getInstance().stateREnvironment.getNamespaceRegistry();
    }

    @Override
    public int R_Interactive() {
        return RContext.getInstance().isInteractive() ? 1 : 0;
    }

    @Override
    public int IS_S4_OBJECT(Object x) {
        return ((x instanceof RTypedValue) && ((RTypedValue) x).isS4()) ? 1 : 0;
    }

    @Override
    public int SET_S4_OBJECT(Object x) {
        guaranteeInstanceOf(x, RTypedValue.class).setS4();
        return 0;
    }

    @Override
    public int UNSET_S4_OBJECT(Object x) {
        guaranteeInstanceOf(x, RTypedValue.class).unsetS4();
        return 0;
    }

    @Override
    public int Rprintf(Object message) {
        RContext.getInstance().getConsole().print((String) message);
        return 0;
    }

    @Override
    public int GetRNGstate() {
        RRNG.getRNGState();
        return 0;
    }

    @Override
    public int PutRNGstate() {
        RRNG.putRNGState();
        return 0;
    }

    @Override
    public double unif_rand() {
        return RRNG.unifRand();
    }

    // Checkstyle: stop method name check

    @Override
    public Object R_getGlobalFunctionContext() {
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
        RCaller rCaller = guaranteeInstanceOf(c, RCaller.class);
        if (rCaller == RCaller.topLevel) {
            return RNull.instance;
        }
        return RContext.getRRuntimeASTAccess().getSyntaxCaller(rCaller);
    }

    @Override
    public Object R_getContextSrcRef(Object c) {
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
        return RContext.getInstance().stateInstrumentation.getBrowserState().inBrowser() ? 1 : 0;
    }

    @Override
    public int R_isGlobal(Object c) {
        RCaller rCaller = guaranteeInstanceOf(c, RCaller.class);

        return rCaller == RCaller.topLevel ? 1 : 0;
    }

    @Override
    public int R_isEqual(Object x, Object y) {
        return x == y ? 1 : 0;
    }

    @Override
    @TruffleBoundary
    public Object Rf_classgets(Object x, Object y) {
        RAbstractVector vector = guaranteeInstanceOf(x, RAbstractVector.class);
        vector.setClassAttr(guaranteeInstanceOf(y, RStringVector.class));
        return RNull.instance;
    }

    @Override
    public RExternalPtr R_MakeExternalPtr(long addr, Object tag, Object prot) {
        return RDataFactory.createExternalPtr(new SymbolHandle(addr), tag, prot);
    }

    @Override
    public long R_ExternalPtrAddr(Object x) {
        RExternalPtr p = guaranteeInstanceOf(x, RExternalPtr.class);
        return p.getAddr().asAddress();
    }

    @Override
    public Object R_ExternalPtrTag(Object x) {
        RExternalPtr p = guaranteeInstanceOf(x, RExternalPtr.class);
        return p.getTag();
    }

    @Override
    public Object R_ExternalPtrProtected(Object x) {
        RExternalPtr p = guaranteeInstanceOf(x, RExternalPtr.class);
        return p.getProt();
    }

    @Override
    public int R_SetExternalPtrAddr(Object x, long addr) {
        RExternalPtr p = guaranteeInstanceOf(x, RExternalPtr.class);
        p.setAddr(new SymbolHandle(addr));
        return 0;
    }

    @Override
    public int R_SetExternalPtrTag(Object x, Object tag) {
        RExternalPtr p = guaranteeInstanceOf(x, RExternalPtr.class);
        p.setTag(tag);
        return 0;
    }

    @Override
    public int R_SetExternalPtrProtected(Object x, Object prot) {
        RExternalPtr p = guaranteeInstanceOf(x, RExternalPtr.class);
        p.setProt(prot);
        return 0;
    }

    @Override
    public REnvironment R_NewHashedEnv(REnvironment parent, Object initialSize) {
        // We know this is an RIntVector from use site in gramRd.c
        REnvironment env = RDataFactory.createNewEnv(REnvironment.UNNAMED, true, ((RIntVector) initialSize).getDataAt(0));
        RArguments.initializeEnclosingFrame(env.getFrame(), parent.getFrame());
        return env;
    }

    @Override
    public int PRSEEN(Object x) {
        RPromise promise = RFFIUtils.guaranteeInstanceOf(x, RPromise.class);
        return promise.getGPBits();
    }

    @Override
    public Object PRENV(Object x) {
        RPromise promise = RFFIUtils.guaranteeInstanceOf(x, RPromise.class);
        final MaterializedFrame frame = promise.getFrame();
        return frame != null ? REnvironment.frameToEnvironment(frame) : RNull.instance;
    }

    @Override
    public Object R_PromiseExpr(Object x) {
        // R_PromiseExpr usually checks, if 'x' is a byte code object. This is not possible in
        // FastR, so simply call PRCODE.
        return PRCODE(x);
    }

    @Override
    public Object PRCODE(Object x) {
        RPromise promise = RFFIUtils.guaranteeInstanceOf(x, RPromise.class);
        RSyntaxNode expr = RASTUtils.unwrap(promise.getRep()).asRSyntaxNode();
        return RASTUtils.createLanguageElement(expr);
    }

    @Override
    public Object R_new_custom_connection(Object description, Object mode, Object className, Object connAddrObj) {
        // TODO handle encoding properly !
        String strDescription = (String) description;
        String strMode = (String) mode;
        String strClassName = (String) className;
        RExternalPtr connAddr = guaranteeInstanceOf(connAddrObj, RExternalPtr.class);
        try {
            return new NativeRConnection(strDescription, strMode, strClassName, connAddr).asVector();
        } catch (IOException e) {
            return InvalidConnection.instance.asVector();
        }
    }

    @Override
    public int R_ReadConnection(int fd, Object bufObj) {
        byte[] buf = (byte[]) bufObj;
        try (BaseRConnection fromIndex = RConnection.fromIndex(fd)) {
            Arrays.fill(buf, (byte) 0);
            return fromIndex.readBin(ByteBuffer.wrap(buf));
        } catch (IOException e) {
            throw RError.error(RError.SHOW_CALLER, RError.Message.ERROR_READING_CONNECTION, e.getMessage());
        }
    }

    @Override
    public int R_WriteConnection(int fd, Object bufObj) {
        byte[] buf = (byte[]) bufObj;
        try (BaseRConnection fromIndex = RConnection.fromIndex(fd)) {
            Arrays.fill(buf, (byte) 0);
            final ByteBuffer wrapped = ByteBuffer.wrap(buf);
            fromIndex.writeBin(wrapped);
            return wrapped.position();
        } catch (IOException e) {
            throw RError.error(RError.SHOW_CALLER, RError.Message.ERROR_WRITING_CONNECTION, e.getMessage());
        }
    }

    @Override
    public Object R_GetConnection(int fd) {
        return RConnection.fromIndex(fd);
    }

    @Override
    public String getSummaryDescription(Object x) {
        BaseRConnection conn = guaranteeInstanceOf(x, BaseRConnection.class);
        return conn.getSummaryDescription();
    }

    @Override
    public String getConnectionClassString(Object x) {
        BaseRConnection conn = guaranteeInstanceOf(x, BaseRConnection.class);
        return conn.getConnectionClass();
    }

    @Override
    public String getOpenModeString(Object x) {
        BaseRConnection conn = guaranteeInstanceOf(x, BaseRConnection.class);
        return conn.getOpenMode().toString();
    }

    @Override
    public boolean isSeekable(Object x) {
        BaseRConnection conn = guaranteeInstanceOf(x, BaseRConnection.class);
        return conn.isSeekable();
    }

    @Override
    public Object R_do_slot(Object o, Object name) {
        return FFIUpCallRootNode.getCallTarget(RFFIUpCallTable.R_do_slot).call(o, name);
    }

    @Override
    public Object R_do_slot_assign(Object o, Object name, Object value) {
        return FFIUpCallRootNode.getCallTarget(RFFIUpCallTable.R_do_slot_assign).call(o, name, value);
    }

    @Override
    public Object R_MethodsNamespace() {
        return REnvironment.getRegisteredNamespace("methods");
    }

    private HashMap<String, Integer> name2typeTable;

    @Override
    @TruffleBoundary
    public int Rf_str2type(Object name) {
        if (name == null) {
            return -1;
        }
        initName2typeTable();
        Integer result = name2typeTable.get(name);
        return result != null ? result : -1;
    }

    private void initName2typeTable() {
        if (name2typeTable != null) {
            return;
        }
        name2typeTable = new HashMap<>(26);
        name2typeTable.put("NULL", SEXPTYPE.NILSXP.code); /* real types */
        name2typeTable.put("symbol", SEXPTYPE.SYMSXP.code);
        name2typeTable.put("pairlist", SEXPTYPE.LISTSXP.code);
        name2typeTable.put("closure", SEXPTYPE.CLOSXP.code);
        name2typeTable.put("environment", SEXPTYPE.ENVSXP.code);
        name2typeTable.put("promise", SEXPTYPE.PROMSXP.code);
        name2typeTable.put("language", SEXPTYPE.LANGSXP.code);
        name2typeTable.put("special", SEXPTYPE.SPECIALSXP.code);
        name2typeTable.put("builtin", SEXPTYPE.BUILTINSXP.code);
        name2typeTable.put("char", SEXPTYPE.CHARSXP.code);
        name2typeTable.put("logical", SEXPTYPE.LGLSXP.code);
        name2typeTable.put("integer", SEXPTYPE.INTSXP.code);
        name2typeTable.put("double", SEXPTYPE.REALSXP.code); /*-  "real", for R <= 0.61.x */
        name2typeTable.put("complex", SEXPTYPE.CPLXSXP.code);
        name2typeTable.put("character", SEXPTYPE.STRSXP.code);
        name2typeTable.put("...", SEXPTYPE.DOTSXP.code);
        name2typeTable.put("any", SEXPTYPE.ANYSXP.code);
        name2typeTable.put("expression", SEXPTYPE.EXPRSXP.code);
        name2typeTable.put("list", SEXPTYPE.VECSXP.code);
        name2typeTable.put("externalptr", SEXPTYPE.EXTPTRSXP.code);
        name2typeTable.put("bytecode", SEXPTYPE.BCODESXP.code);
        name2typeTable.put("weakref", SEXPTYPE.WEAKREFSXP.code);
        name2typeTable.put("raw", SEXPTYPE.RAWSXP.code);
        name2typeTable.put("S4", SEXPTYPE.S4SXP.code);
        name2typeTable.put("numeric", SEXPTYPE.REALSXP.code);
        name2typeTable.put("name", SEXPTYPE.SYMSXP.code);
    }
}
