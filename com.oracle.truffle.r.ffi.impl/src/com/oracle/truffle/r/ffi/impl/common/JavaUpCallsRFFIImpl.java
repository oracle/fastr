/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameInstance.FrameAccess;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.CanResolve;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.ForeignAccess.StandardFactory;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.MessageResolution;
import com.oracle.truffle.api.interop.Resolve;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.ffi.impl.common.JavaUpCallsRFFIImplFactory.VectorWrapperNativePointerFactory.DispatchAllocateNodeGen;
import com.oracle.truffle.r.ffi.impl.upcalls.UpCallsRFFI;
import com.oracle.truffle.r.nodes.RASTUtils;
import com.oracle.truffle.r.nodes.function.ClassHierarchyNode;
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
import com.oracle.truffle.r.runtime.conn.RConnection;
import com.oracle.truffle.r.runtime.context.Engine.IncompleteSourceException;
import com.oracle.truffle.r.runtime.context.Engine.ParseException;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.CharSXPWrapper;
import com.oracle.truffle.r.runtime.data.NativeDataAccess;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RAttributesLayout;
import com.oracle.truffle.r.runtime.data.RComplexVector;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RExpression;
import com.oracle.truffle.r.runtime.data.RExternalPtr;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RLanguage;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RObject;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.data.RPromise.EagerPromise;
import com.oracle.truffle.r.runtime.data.RRawVector;
import com.oracle.truffle.r.runtime.data.RShareable;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.data.RTypedValue;
import com.oracle.truffle.r.runtime.data.RUnboundValue;
import com.oracle.truffle.r.runtime.data.RVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess.RandomIterator;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess.SequentialIterator;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.env.REnvironment.PutException;
import com.oracle.truffle.r.runtime.ffi.DLL;
import com.oracle.truffle.r.runtime.ffi.DLL.CEntry;
import com.oracle.truffle.r.runtime.ffi.DLL.DLLInfo;
import com.oracle.truffle.r.runtime.ffi.DLL.DotSymbol;
import com.oracle.truffle.r.runtime.ffi.DLL.SymbolHandle;
import com.oracle.truffle.r.runtime.ffi.RFFIContext;
import com.oracle.truffle.r.runtime.ffi.UnsafeAdapter;
import com.oracle.truffle.r.runtime.gnur.SA_TYPE;
import com.oracle.truffle.r.runtime.gnur.SEXPTYPE;
import com.oracle.truffle.r.runtime.nodes.DuplicationHelper;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;
import com.oracle.truffle.r.runtime.rng.RRNG;

import sun.misc.Unsafe;

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

    private static RuntimeException implementedAsNode() {
        // TODO: Exception handling over native boundaries is currently missing. Once this works,
        // remove the following two lines.
        System.err.println("upcall function is implemented via a node");
        System.exit(1);

        return RInternalError.shouldNotReachHere("upcall function is implemented via a node");
    }

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
    public RDoubleVector Rf_ScalarReal(double value) {
        return RDataFactory.createDoubleVectorFromScalar(value);
    }

    @Override
    public RStringVector Rf_ScalarString(Object value) {
        CharSXPWrapper chars = guaranteeInstanceOf(value, CharSXPWrapper.class);
        return RDataFactory.createStringVectorFromScalar(chars.getContents());
    }

    @Override
    public int Rf_asInteger(Object x) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_asReal(Object x) {
        throw implementedAsNode();
    }

    @Override
    public int Rf_asLogical(Object x) {
        throw implementedAsNode();
    }

    @Override
    public Object Rf_asChar(Object x) {
        throw implementedAsNode();
    }

    @Override
    public Object Rf_coerceVector(Object x, int mode) {
        throw implementedAsNode();
    }

    @Override
    @TruffleBoundary
    public Object Rf_mkCharLenCE(Object bytes, int len, int encoding) {
        // TODO: handle encoding properly
        return CharSXPWrapper.create(new String((byte[]) bytes, StandardCharsets.UTF_8));
    }

    @Override
    public Object Rf_cons(Object car, Object cdr) {
        return RDataFactory.createPairList(car, cdr);
    }

    @Override
    @TruffleBoundary
    public void Rf_defineVar(Object symbolArg, Object value, Object envArg) {
        REnvironment env = (REnvironment) envArg;
        RSymbol name = (RSymbol) symbolArg;
        try {
            env.put(name.getName(), value);
        } catch (PutException ex) {
            throw RError.error(RError.SHOW_CALLER2, ex);
        }
    }

    @Override
    @TruffleBoundary
    public Object R_getClassDef(Object clazz) {
        throw implementedAsNode();
    }

    @Override
    @TruffleBoundary
    public Object R_do_MAKE_CLASS(Object clazz) {
        throw implementedAsNode();
    }

    @Override
    public Object R_do_new_object(Object classDef) {
        throw implementedAsNode();
    }

    @Override
    @TruffleBoundary
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
                if (value instanceof RPromise && ((RPromise) value).isOptimized()) {
                    // From the point of view of RFFI, optimized promises (i.e. promises with null
                    // env) should not show up
                    return ((RPromise) value).getRawValue();
                }
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
        throw implementedAsNode();
    }

    @Override
    public Object Rf_getAttrib(Object obj, Object name) {
        throw implementedAsNode();
    }

    @Override
    @TruffleBoundary
    public void Rf_setAttrib(Object obj, Object name, Object val) {
        if (obj instanceof RAttributable) {
            RAttributable attrObj = (RAttributable) obj;
            String nameAsString;
            if (name instanceof RSymbol) {
                nameAsString = ((RSymbol) name).getName();
            } else {
                nameAsString = RRuntime.asString(name);
                assert nameAsString != null;
            }
            nameAsString = Utils.intern(nameAsString);
            if (val == RNull.instance) {
                if ("class" == nameAsString) {
                    removeClassAttr(attrObj);
                } else {
                    removeAttr(attrObj, nameAsString);
                }
            } else if ("class" == nameAsString) {
                attrObj.initAttributes().define(nameAsString, val);
            } else {
                attrObj.setAttr(nameAsString, val);
            }
        } else {
            throw RInternalError.shouldNotReachHere();
        }
    }

    @TruffleBoundary
    private static void removeAttr(RAttributable a, String name) {
        a.removeAttr(name);
    }

    @TruffleBoundary
    private static void removeClassAttr(RAttributable a) {
        a.setClassAttr(null);
    }

    public static RStringVector getClassHr(Object v) {
        return ClassHierarchyNode.getClassHierarchy(v);
    }

    @Override
    @TruffleBoundary
    public int Rf_inherits(Object x, String clazz) {
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
    @TruffleBoundary
    public Object Rf_install(String name) {
        return RDataFactory.createSymbolInterned(name);
    }

    @Override
    @TruffleBoundary
    public Object Rf_installChar(Object name) {
        CharSXPWrapper charSXP = guaranteeInstanceOf(name, CharSXPWrapper.class);
        return RDataFactory.createSymbolInterned(charSXP.getContents());
    }

    @Override
    @TruffleBoundary
    public Object Rf_lengthgets(Object x, int newSize) {
        RAbstractVector vec = (RAbstractVector) RRuntime.asAbstractVector(x);
        return vec.resize(newSize);
    }

    @Override
    @TruffleBoundary
    public int Rf_isString(Object x) {
        return RRuntime.checkType(x, RType.Character) ? 1 : 0;
    }

    @Override
    public int Rf_isNull(Object x) {
        return x == RNull.instance ? 1 : 0;
    }

    @Override
    @TruffleBoundary
    public Object Rf_PairToVectorList(Object x) {
        if (x == RNull.instance) {
            return RDataFactory.createList();
        }
        RPairList pl = (RPairList) x;
        return pl.toRList();
    }

    @Override
    @TruffleBoundary
    public void Rf_error(String msg) {
        RError.error(RError.SHOW_CALLER, RError.Message.GENERIC, msg);
    }

    @Override
    @TruffleBoundary
    public void Rf_warning(String msg) {
        RError.warning(RError.SHOW_CALLER, RError.Message.GENERIC, msg);
    }

    @Override
    @TruffleBoundary
    public void Rf_warningcall(Object call, String msg) {
        RErrorHandling.warningcallRFFI(call, msg);
    }

    @Override
    @TruffleBoundary
    public void Rf_errorcall(Object call, String msg) {
        RErrorHandling.errorcallRFFI(call, msg);
    }

    @Override
    @TruffleBoundary
    public Object Rf_allocVector(int mode, long n) {
        SEXPTYPE type = SEXPTYPE.mapInt(mode);
        if (n > Integer.MAX_VALUE) {
            throw RError.error(RError.SHOW_CALLER, RError.Message.LONG_VECTORS_NOT_SUPPORTED);
            // TODO check long vector
        }
        int ni = (int) n;
        switch (type) {
            case INTSXP:
                return RDataFactory.createIntVector(new int[ni], RDataFactory.COMPLETE_VECTOR);
            case REALSXP:
                return RDataFactory.createDoubleVector(new double[ni], RDataFactory.COMPLETE_VECTOR);
            case LGLSXP:
                return RDataFactory.createLogicalVector(new byte[ni], RDataFactory.COMPLETE_VECTOR);
            case STRSXP:
                // fill list with empty strings
                String[] data = new String[ni];
                Arrays.fill(data, "");
                return RDataFactory.createStringVector(data, RDataFactory.COMPLETE_VECTOR);
            case CPLXSXP:
                return RDataFactory.createComplexVector(new double[2 * ni], RDataFactory.COMPLETE_VECTOR);
            case RAWSXP:
                return RDataFactory.createRawVector(new byte[ni]);
            case VECSXP:
                return RDataFactory.createList(ni);
            case LANGSXP:
                return RDataFactory.createLangPairList(ni);
            default:
                throw unimplemented("unexpected SEXPTYPE " + type);
        }
    }

    @Override
    @TruffleBoundary
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
    @TruffleBoundary
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
    @TruffleBoundary
    public int Rf_nrows(Object x) {
        return RRuntime.nrows(x);
    }

    @Override
    @TruffleBoundary
    public int Rf_ncols(Object x) {
        return RRuntime.ncols(x);
    }

    @Override
    public int LENGTH(Object x) {
        throw implementedAsNode();
    }

    @Override
    public void SET_STRING_ELT(Object x, long i, Object v) {
        RStringVector vector = guaranteeInstanceOf(x, RStringVector.class);
        CharSXPWrapper element = guaranteeInstanceOf(v, CharSXPWrapper.class);
        String value = element.getContents();
        if (RRuntime.isNA(value)) {
            vector.setComplete(false);
        }
        vector.setElement((int) i, value);
    }

    @Override
    public void SET_VECTOR_ELT(Object x, long i, Object v) {
        RList list = guaranteeInstanceOf(x, RList.class);
        list.setElement((int) i, v);
    }

    @Override
    public Object STRING_ELT(Object x, long i) {
        RAbstractStringVector vector = guaranteeInstanceOf(RRuntime.asAbstractVector(x), RAbstractStringVector.class);
        return CharSXPWrapper.create(vector.getDataAt((int) i));
    }

    @Override
    public Object VECTOR_ELT(Object x, long i) {
        Object vec = x;
        if (vec instanceof RExpression) {
            return ((RExpression) vec).getDataAt((int) i);
        }
        RAbstractListVector list = guaranteeInstanceOf(RRuntime.asAbstractVector(vec), RAbstractListVector.class);
        return list.getDataAt((int) i);
    }

    @Override
    public int NAMED(Object x) {
        if (x instanceof RShareable) {
            return getNamed((RShareable) x);
        } else {
            // Note: it may be that we need to remember this for all types, GNUR does
            return 2;
        }
    }

    @Override
    public void SET_NAMED_FASTR(Object x, int v) {
        // Note: In GNUR this is a macro that sets the sxpinfo.named regardless of whether it makes
        // sense to name the actual value, for compatibilty we simply ignore values that are not
        // RShareable, e.g. RSymbol. However we ignore and report attemps to decrease the ref-count,
        // which as it seems GNUR would just let proceede
        if (x instanceof RShareable) {
            RShareable r = (RShareable) x;
            int actual = getNamed(r);
            if (v < actual) {
                RError.warning(RError.NO_CALLER, RError.Message.GENERIC, "Native code attempted to decrease the reference count. This operation is ignored.");
            }
            if (v == 2) {
                // we play it safe: if the caller wants this instance to be shared, they may expect
                // it to never become non-shared again, which could happen in FastR
                r.makeSharedPermanent();
            }
            if (v == 1 && r.isTemporary()) {
                r.incRefCount();
            }
        }
    }

    private static int getNamed(RShareable r) {
        return r.isTemporary() ? 0 : r.isShared() ? 2 : 1;
    }

    @Override
    @TruffleBoundary
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
    @TruffleBoundary
    public Object Rf_duplicate(Object x, int deep) {
        throw implementedAsNode();
    }

    @Override
    public long Rf_any_duplicated(Object x, int fromLast) {
        throw implementedAsNode();
    }

    @Override
    public long Rf_any_duplicated3(Object x, Object incomparables, int fromLast) {
        throw implementedAsNode();
    }

    @Override
    public Object PRINTNAME(Object x) {
        if (x == RNull.instance) {
            return x;
        }
        guaranteeInstanceOf(x, RSymbol.class);
        return CharSXPWrapper.create(((RSymbol) x).getName());
    }

    @Override
    public Object TAG(Object e) {
        throw implementedAsNode();
    }

    @Override
    public Object CAR(Object e) {
        throw implementedAsNode();
    }

    @Override
    public Object CAAR(Object e) {
        throw implementedAsNode();
    }

    @Override
    public Object CDR(Object e) {
        throw implementedAsNode();
    }

    @Override
    public Object CADR(Object e) {
        throw implementedAsNode();
    }

    @Override
    public Object CDAR(Object e) {
        throw implementedAsNode();
    }

    @Override
    public Object CADDR(Object e) {
        throw implementedAsNode();
    }

    @Override
    public Object CADDDR(Object e) {
        throw implementedAsNode();
    }

    @Override
    public Object CAD4R(Object e) {
        throw implementedAsNode();
    }

    @Override
    public Object CDDR(Object e) {
        throw implementedAsNode();
    }

    @Override
    public Object CDDDR(Object e) {
        throw implementedAsNode();
    }

    @Override
    public void SET_TAG(Object x, Object y) {
        if (x instanceof RPairList) {
            ((RPairList) x).setTag(y);
        } else {
            guaranteeInstanceOf(x, RExternalPtr.class);
            // at the moment, this can only be used to null out the pointer
            ((RExternalPtr) x).setTag(y);
        }
    }

    @Override
    public Object SETCAR(Object x, Object y) {
        throw implementedAsNode();
    }

    @Override
    public Object SETCDR(Object x, Object y) {
        guaranteeInstanceOf(x, RPairList.class);
        ((RPairList) x).setCdr(y);
        return y;
    }

    @Override
    public Object SETCADR(Object x, Object y) {
        throw implementedAsNode();
    }

    @Override
    public Object SETCADDR(Object x, Object y) {
        throw implementedAsNode();
    }

    @Override
    public Object SETCADDDR(Object x, Object y) {
        throw implementedAsNode();
    }

    @Override
    public Object SETCAD4R(Object x, Object y) {
        throw implementedAsNode();
    }

    @Override
    @TruffleBoundary
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
    @TruffleBoundary
    public void SET_SYMVALUE(Object x, Object v) {
        if (!(x instanceof RSymbol)) {
            throw RInternalError.shouldNotReachHere();
        }
        REnvironment.baseEnv().safePut(((RSymbol) x).getName(), v);
    }

    @Override
    @TruffleBoundary
    public int R_BindingIsLocked(Object sym, Object env) {
        guaranteeInstanceOf(sym, RSymbol.class);
        guaranteeInstanceOf(env, REnvironment.class);
        return ((REnvironment) env).bindingIsLocked(((RSymbol) sym).getName()) ? 1 : 0;
    }

    @Override
    public void R_LockBinding(Object sym, Object env) {
        throw implementedAsNode();
    }

    @Override
    public void R_unLockBinding(Object sym, Object env) {
        throw implementedAsNode();
    }

    @Override
    @TruffleBoundary
    public Object R_FindNamespace(Object name) {
        Object result = RContext.getInstance().stateREnvironment.getNamespaceRegistry().get(RRuntime.asString(name));
        return result;
    }

    @Override
    public Object Rf_eval(Object expr, Object env) {
        throw implementedAsNode();
    }

    @Override
    @TruffleBoundary
    public Object Rf_findFun(Object symbolObj, Object envObj) {
        guaranteeInstanceOf(envObj, REnvironment.class);
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
    @TruffleBoundary
    public Object Rf_GetOption1(Object tag) {
        guaranteeInstanceOf(tag, RSymbol.class);
        Object result = RContext.getInstance().stateROptions.getValue(((RSymbol) tag).getName());
        return result;
    }

    @Override
    @TruffleBoundary
    public void Rf_gsetVar(Object symbol, Object value, Object rho) {
        guaranteeInstanceOf(symbol, RSymbol.class);
        REnvironment baseEnv = RContext.getInstance().stateREnvironment.getBaseEnv();
        guarantee(rho == baseEnv);
        try {
            baseEnv.put(((RSymbol) symbol).getName(), value);
        } catch (PutException e) {
            e.printStackTrace();
        }
    }

    @Override
    @TruffleBoundary
    public void DUPLICATE_ATTRIB(Object to, Object from) {
        if (from instanceof RAttributable) {
            guaranteeInstanceOf(to, RAttributable.class);
            DynamicObject attributes = ((RAttributable) from).getAttributes();
            ((RAttributable) to).initAttributes(attributes == null ? null : RAttributesLayout.copy(attributes));
        }
        // TODO: copy OBJECT? and S4 attributes
    }

    @Override
    @TruffleBoundary
    public int R_compute_identical(Object x, Object y, int flags) {
        RFunction indenticalBuiltin = RContext.getInstance().lookupBuiltin("identical");
        Object res = RContext.getEngine().evalFunction(indenticalBuiltin, null, null, true, null, x, y, RRuntime.asLogical((!((flags & 1) == 0))),
                        RRuntime.asLogical((!((flags & 2) == 0))), RRuntime.asLogical((!((flags & 4) == 0))), RRuntime.asLogical((!((flags & 8) == 0))), RRuntime.asLogical((!((flags & 16) == 0))));
        return RRuntime.logical2int((byte) res);
    }

    @Override
    public void Rf_copyListMatrix(Object t, Object s, int byrow) {
        throw unimplemented();
    }

    @Override
    @TruffleBoundary
    public void Rf_copyMatrix(Object t, Object s, int byRow) {
        RAbstractVector target = guaranteeInstanceOf(t, RAbstractVector.class);
        RAbstractVector source = guaranteeInstanceOf(RRuntime.asAbstractVector(s), RAbstractVector.class);

        VectorAccess targetAccess = target.slowPathAccess();
        VectorAccess sourceAccess = source.slowPathAccess();

        try (SequentialIterator sourceIter = sourceAccess.access(source)) {
            if (byRow != 0) { // copy by row
                int tRows = RRuntime.nrows(target);
                int tCols = RRuntime.ncols(target);
                try (RandomIterator targetIter = targetAccess.randomAccess(target)) {
                    for (int i = 0; i < tRows; i++) {
                        int tIdx = i;
                        for (int j = 0; j < tCols; j++) {
                            sourceAccess.nextWithWrap(sourceIter);
                            targetAccess.setFromSameType(targetIter, tIdx, sourceAccess, sourceIter);
                            tIdx += tRows;
                        }
                    }
                }
            } else { // copy by column
                try (SequentialIterator targetIter = targetAccess.access(target)) {
                    while (targetAccess.next(targetIter)) {
                        sourceAccess.nextWithWrap(sourceIter);
                        targetAccess.setFromSameType(targetIter, sourceAccess, sourceIter);
                    }
                }
            }
        }
    }

    @Override
    public Object R_tryEval(Object expr, Object env, Object errorFlag, int silent) {
        throw implementedAsNode();
    }

    /**
     * Helper function for {@code R_TopLevelExec} which is similar to {@code R_TryEval} except that
     * a C function is invoked (in the native layer) instead of an R expression. assert: this is
     * ONLY called from R_TopLevelExec prior to calling C function.
     */
    @Override
    @TruffleBoundary
    public Object R_ToplevelExec() {
        return RErrorHandling.resetAndGetHandlerStacks().handlerStack;
    }

    @Override
    public void restoreHandlerStacks(Object savedHandlerStack) {
        RErrorHandling.restoreHandlerStack(savedHandlerStack);
    }

    @Override
    @TruffleBoundary
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
    @TruffleBoundary
    public void SET_RDEBUG(Object x, int v) {
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
    @TruffleBoundary
    public int RSTEP(Object x) {
        @SuppressWarnings("unused")
        REnvironment env = guaranteeInstanceOf(x, REnvironment.class);
        throw RInternalError.unimplemented("RSTEP");
    }

    @Override
    @TruffleBoundary
    public void SET_RSTEP(Object x, int v) {
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

    private enum ParseStatus {
        PARSE_NULL,
        PARSE_OK,
        PARSE_INCOMPLETE,
        PARSE_ERROR,
        PARSE_EOF
    }

    @Override
    @TruffleBoundary
    public Object R_ParseVector(Object text, int n, Object srcFile) {
        // TODO general case + all statuses
        assert n == 1 : "unsupported: R_ParseVector with n != 0.";
        assert srcFile == RNull.instance : "unsupported: R_ParseVector with non-null srcFile argument.";
        String textString = RRuntime.asString(text);
        assert textString != null;

        Object[] resultData = new Object[2];
        try {
            Source source = RSource.fromTextInternal(textString, RSource.Internal.R_PARSEVECTOR);
            RExpression exprs = RContext.getEngine().parse(source);
            resultData[0] = RDataFactory.createIntVectorFromScalar(ParseStatus.PARSE_OK.ordinal());
            resultData[1] = exprs;
        } catch (IncompleteSourceException ex) {
            resultData[0] = RDataFactory.createIntVectorFromScalar(ParseStatus.PARSE_INCOMPLETE.ordinal());
            resultData[1] = RNull.instance;
        } catch (ParseException ex) {
            resultData[0] = RDataFactory.createIntVectorFromScalar(ParseStatus.PARSE_ERROR.ordinal());
            resultData[1] = RNull.instance;
        }
        return RDataFactory.createList(resultData);
    }

    @Override
    @TruffleBoundary
    public Object R_lsInternal3(Object envArg, int allArg, int sortedArg) {
        boolean sorted = sortedArg != 0;
        boolean all = allArg != 0;
        REnvironment env = guaranteeInstanceOf(envArg, REnvironment.class);
        return env.ls(all, null, sorted);
    }

    @Override
    @TruffleBoundary
    public String R_HomeDir() {
        return REnvVars.rHome();
    }

    @Override
    @TruffleBoundary
    public void R_CleanUp(int sa, int status, int runlast) {
        RCleanUp.stdCleanUp(SA_TYPE.values()[sa], status, runlast != 0);
    }

    @Override
    @TruffleBoundary
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
    public void SET_S4_OBJECT(Object x) {
        guaranteeInstanceOf(x, RTypedValue.class).setS4();
    }

    @Override
    public void UNSET_S4_OBJECT(Object x) {
        guaranteeInstanceOf(x, RTypedValue.class).unsetS4();
    }

    @Override
    @TruffleBoundary
    public void Rprintf(String message) {
        RContext.getInstance().getConsole().print(message);
    }

    @Override
    @TruffleBoundary
    public void GetRNGstate() {
        RRNG.getRNGState();
    }

    @Override
    @TruffleBoundary
    public void PutRNGstate() {
        RRNG.putRNGState();
    }

    @Override
    @TruffleBoundary
    public double unif_rand() {
        return RRNG.unifRand();
    }

    // Checkstyle: stop method name check
    @Override
    @TruffleBoundary
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
    @TruffleBoundary
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
    @TruffleBoundary
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
    @TruffleBoundary
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
    @TruffleBoundary
    public Object R_getContextCall(Object c) {
        RCaller rCaller = guaranteeInstanceOf(c, RCaller.class);
        if (rCaller == RCaller.topLevel) {
            return RNull.instance;
        }
        return RContext.getRRuntimeASTAccess().getSyntaxCaller(rCaller);
    }

    @Override
    @TruffleBoundary
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
    @TruffleBoundary
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
    public RExternalPtr R_MakeExternalPtr(Object addr, Object tag, Object prot) {
        throw implementedAsNode();
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
    public void R_SetExternalPtrAddr(Object x, long addr) {
        RExternalPtr p = guaranteeInstanceOf(x, RExternalPtr.class);
        p.setAddr(new SymbolHandle(addr));
    }

    @Override
    public void R_SetExternalPtrTag(Object x, Object tag) {
        RExternalPtr p = guaranteeInstanceOf(x, RExternalPtr.class);
        p.setTag(tag);
    }

    @Override
    public void R_SetExternalPtrProtected(Object x, Object prot) {
        RExternalPtr p = guaranteeInstanceOf(x, RExternalPtr.class);
        p.setProt(prot);
    }

    @Override
    @TruffleBoundary
    public REnvironment R_NewHashedEnv(Object parent, Object initialSize) {
        // We know this is an RIntVector from use site in gramRd.c
        REnvironment env = RDataFactory.createNewEnv(REnvironment.UNNAMED, true, ((RIntVector) initialSize).getDataAt(0));
        RArguments.initializeEnclosingFrame(env.getFrame(), guaranteeInstanceOf(parent, REnvironment.class).getFrame());
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
        if (promise instanceof EagerPromise) {
            ((EagerPromise) promise).materialize();
        }
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
    @TruffleBoundary
    public Object PRCODE(Object x) {
        RPromise promise = RFFIUtils.guaranteeInstanceOf(x, RPromise.class);
        RSyntaxNode expr = RASTUtils.unwrap(promise.getRep()).asRSyntaxNode();
        return RASTUtils.createLanguageElement(expr);
    }

    @Override
    @TruffleBoundary
    public Object R_new_custom_connection(Object description, Object mode, Object className, Object connAddrObj) {
        throw implementedAsNode();
    }

    @Override
    @TruffleBoundary
    public int R_ReadConnection(int fd, long bufAddress, int size) {
        // Workaround using Unsafe until GR-5927 is fixed
        byte[] buf = new byte[size];
        int result = 0;
        try (BaseRConnection fromIndex = RConnection.fromIndex(fd)) {
            Arrays.fill(buf, (byte) 0);
            result = fromIndex.readBin(ByteBuffer.wrap(buf));
        } catch (IOException e) {
            throw RError.error(RError.SHOW_CALLER, RError.Message.ERROR_READING_CONNECTION, e.getMessage());
        }
        UnsafeAdapter.UNSAFE.copyMemory(buf, Unsafe.ARRAY_BYTE_BASE_OFFSET, null, bufAddress, Math.min(result, size));
        return result;
    }

    @Override
    @TruffleBoundary
    public int R_WriteConnection(int fd, long bufAddress, int size) {
        // Workaround using Unsafe until GR-5927 is fixed
        byte[] buf = new byte[size];
        UnsafeAdapter.UNSAFE.copyMemory(null, bufAddress, buf, Unsafe.ARRAY_BYTE_BASE_OFFSET, size);
        try (BaseRConnection fromIndex = RConnection.fromIndex(fd)) {
            final ByteBuffer wrapped = ByteBuffer.wrap(buf);
            fromIndex.writeBin(wrapped);
            return wrapped.position();
        } catch (IOException e) {
            throw RError.error(RError.SHOW_CALLER, RError.Message.ERROR_WRITING_CONNECTION, e.getMessage());
        }
    }

    @Override
    @TruffleBoundary
    public Object R_GetConnection(int fd) {
        return RConnection.fromIndex(fd);
    }

    private static VectorWrapper wrapString(String s) {
        CharSXPWrapper v = CharSXPWrapper.create(s);
        NativeDataAccess.asPointer(v);
        return new VectorWrapper(v);
    }

    @Override
    @TruffleBoundary
    public Object getSummaryDescription(Object x) {
        BaseRConnection conn = guaranteeInstanceOf(x, BaseRConnection.class);
        return wrapString(conn.getSummaryDescription());
    }

    @Override
    @TruffleBoundary
    public Object getConnectionClassString(Object x) {
        BaseRConnection conn = guaranteeInstanceOf(x, BaseRConnection.class);
        return wrapString(conn.getConnectionClass());
    }

    @Override
    @TruffleBoundary
    public Object getOpenModeString(Object x) {
        BaseRConnection conn = guaranteeInstanceOf(x, BaseRConnection.class);
        return wrapString(conn.getOpenMode().toString());
    }

    @Override
    @TruffleBoundary
    public boolean isSeekable(Object x) {
        BaseRConnection conn = guaranteeInstanceOf(x, BaseRConnection.class);
        return conn.isSeekable();
    }

    @Override
    public Object R_do_slot(Object o, Object name) {
        throw implementedAsNode();
    }

    @Override
    public Object R_do_slot_assign(Object o, Object name, Object value) {
        throw implementedAsNode();
    }

    @Override
    @TruffleBoundary
    public Object R_MethodsNamespace() {
        return REnvironment.getRegisteredNamespace("methods");
    }

    @Override
    @TruffleBoundary
    public void R_PreserveObject(Object obj) {
        guaranteeInstanceOf(obj, RObject.class);
        IdentityHashMap<RObject, AtomicInteger> preserveList = getContext().preserveList;
        AtomicInteger prevCnt = preserveList.putIfAbsent((RObject) obj, new AtomicInteger(1));
        if (prevCnt != null) {
            prevCnt.incrementAndGet();
        }
    }

    @Override
    @TruffleBoundary
    public void R_ReleaseObject(Object obj) {
        guaranteeInstanceOf(obj, RObject.class);
        RFFIContext context = getContext();
        IdentityHashMap<RObject, AtomicInteger> preserveList = context.preserveList;
        AtomicInteger atomicInteger = preserveList.get(obj);
        if (atomicInteger != null) {
            int decrementAndGet = atomicInteger.decrementAndGet();
            if (decrementAndGet == 0) {
                // remove from "list"
                preserveList.remove(obj);
                context.registerReferenceUsedInNative(obj);
            }
        } else {
            // TODO report ?
        }
    }

    @Override
    @TruffleBoundary
    public Object Rf_protect(Object x) {
        getContext().protectStack.add(guaranteeInstanceOf(x, RObject.class));
        return x;
    }

    @Override
    @TruffleBoundary
    public void Rf_unprotect(int x) {
        RFFIContext context = getContext();
        ArrayList<RObject> stack = context.protectStack;
        for (int i = 0; i < x; i++) {
            context.registerReferenceUsedInNative(stack.remove(stack.size() - 1));
        }
    }

    @Override
    @TruffleBoundary
    public int R_ProtectWithIndex(Object x) {
        ArrayList<RObject> stack = getContext().protectStack;
        stack.add(guaranteeInstanceOf(x, RObject.class));
        return stack.size() - 1;
    }

    @Override
    @TruffleBoundary
    public void R_Reprotect(Object x, int y) {
        ArrayList<RObject> stack = getContext().protectStack;
        stack.set(y, guaranteeInstanceOf(x, RObject.class));
    }

    @Override
    @TruffleBoundary
    public void Rf_unprotect_ptr(Object x) {
        RFFIContext context = getContext();
        ArrayList<RObject> stack = context.protectStack;
        for (int i = stack.size() - 1; i >= 0; i--) {
            if (stack.get(i) == x) {
                context.registerReferenceUsedInNative(stack.remove(i));
                return;
            }
        }
    }

    @Override
    @TruffleBoundary
    public int FASTR_getConnectionChar(Object obj) {
        try {
            return guaranteeInstanceOf(obj, RConnection.class).getc();
        } catch (IOException e) {
            return -1;
        }
    }

    @Override
    @TruffleBoundary
    public int Rf_str2type(Object name) {
        throw implementedAsNode();
    }

    @Override
    @TruffleBoundary
    public int registerRoutines(Object dllInfoObj, int nstOrd, int num, Object routines) {
        DLLInfo dllInfo = guaranteeInstanceOf(dllInfoObj, DLLInfo.class);
        DotSymbol[] array = new DotSymbol[num];
        for (int i = 0; i < num; i++) {
            Object sym = setSymbol(dllInfo, nstOrd, routines, i);
            array[i] = (DotSymbol) sym;
        }
        dllInfo.setNativeSymbols(nstOrd, array);
        return 0;
    }

    @Override
    @TruffleBoundary
    public int registerCCallable(String pkgName, String functionName, Object address) {
        DLLInfo lib = DLL.safeFindLibrary(pkgName);
        lib.registerCEntry(new CEntry(functionName, new SymbolHandle(address)));
        return 0;
    }

    @Override
    @TruffleBoundary
    public int useDynamicSymbols(Object dllInfoObj, int value) {
        DLLInfo dllInfo = guaranteeInstanceOf(dllInfoObj, DLLInfo.class);
        return DLL.useDynamicSymbols(dllInfo, value);
    }

    @Override
    @TruffleBoundary
    public int forceSymbols(Object dllInfoObj, int value) {
        DLLInfo dllInfo = guaranteeInstanceOf(dllInfoObj, DLLInfo.class);
        return DLL.forceSymbols(dllInfo, value);
    }

    @Override
    @TruffleBoundary
    public DotSymbol setDotSymbolValues(Object dllInfoObj, String name, Object fun, int numArgs) {
        @SuppressWarnings("unused")
        DLLInfo dllInfo = guaranteeInstanceOf(dllInfoObj, DLLInfo.class);
        return new DotSymbol(name, new SymbolHandle(fun), numArgs);
    }

    @Override
    public Object getEmbeddingDLLInfo() {
        return DLL.getEmbeddingDLLInfo();
    }

    protected abstract Object setSymbol(DLLInfo dllInfo, int nstOrd, Object routines, int index);

    @Override
    public double Rf_dunif(double a, double b, double c, int d) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_qunif(double a, double b, double c, int d, int e) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_punif(double a, double b, double c, int d, int e) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_runif(double a, double b) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_dchisq(double a, double b, int c) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_pchisq(double a, double b, int c, int d) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_qchisq(double a, double b, int c, int d) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_rchisq(double a) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_dnchisq(double a, double b, double c, int d) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_pnchisq(double a, double b, double c, int d, int e) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_qnchisq(double a, double b, double c, int d, int e) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_rnchisq(double a, double b) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_dnorm4(double a, double b, double c, int d) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_pnorm5(double a, double b, double c, int d, int e) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_qnorm5(double a, double b, double c, int d, int e) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_rnorm(double a, double b) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_dlnorm(double a, double b, double c, int d) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_plnorm(double a, double b, double c, int d, int e) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_qlnorm(double a, double b, double c, int d, int e) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_rlnorm(double a, double b) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_dgamma(double a, double b, double c, int d) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_pgamma(double a, double b, double c, int d, int e) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_qgamma(double a, double b, double c, int d, int e) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_rgamma(double a, double b) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_dbeta(double a, double b, double c, int d) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_pbeta(double a, double b, double c, int d, int e) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_qbeta(double a, double b, double c, int d, int e) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_rbeta(double a, double b) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_df(double a, double b, double c, int d) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_pf(double a, double b, double c, int d, int e) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_qf(double a, double b, double c, int d, int e) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_rf(double a, double b) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_dt(double a, double b, int c) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_pt(double a, double b, int c, int d) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_qt(double a, double b, int c, int d) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_rt(double a) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_dbinom(double a, double b, double c, int d) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_pbinom(double a, double b, double c, int d, int e) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_qbinom(double a, double b, double c, int d, int e) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_rbinom(double a, double b) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_dcauchy(double a, double b, double c, int d) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_pcauchy(double a, double b, double c, int d, int e) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_qcauchy(double a, double b, double c, int d, int e) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_rcauchy(double a, double b) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_dexp(double a, double b, int c) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_pexp(double a, double b, int c, int d) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_qexp(double a, double b, int c, int d) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_rexp(double a) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_dgeom(double a, double b, int c) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_pgeom(double a, double b, int c, int d) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_qgeom(double a, double b, int c, int d) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_rgeom(double a) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_dhyper(double a, double b, double c, double d, int e) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_phyper(double a, double b, double c, double d, int e, int f) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_qhyper(double a, double b, double c, double d, int e, int f) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_rhyper(double a, double b, double c) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_dnbinom(double a, double b, double c, int d) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_pnbinom(double a, double b, double c, int d, int e) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_qnbinom(double a, double b, double c, int d, int e) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_rnbinom(double a, double b) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_dnbinom_mu(double a, double b, double c, int d) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_pnbinom_mu(double a, double b, double c, int d, int e) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_qnbinom_mu(double a, double b, double c, int d, int e) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_rnbinom_mu(double a, double b) {
        throw implementedAsNode();
    }

    @Override
    public void Rf_rmultinom(int a, Object b, int c, Object d) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_dpois(double a, double b, int c) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_ppois(double a, double b, int c, int d) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_qpois(double a, double b, int c, int d) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_rpois(double a) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_dweibull(double a, double b, double c, int d) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_pweibull(double a, double b, double c, int d, int e) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_qweibull(double a, double b, double c, int d, int e) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_rweibull(double a, double b) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_dlogis(double a, double b, double c, int d) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_plogis(double a, double b, double c, int d, int e) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_qlogis(double a, double b, double c, int d, int e) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_rlogis(double a, double b) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_dnbeta(double a, double b, double c, double d, int e) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_pnbeta(double a, double b, double c, double d, int e, int f) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_qnbeta(double a, double b, double c, double d, int e, int f) {
        throw implementedAsNode();
    }

    // @Override
    // public double Rf_rnbeta(double a, double b, double c) {
    // throw implementedAsNode();
    // }

    @Override
    public double Rf_dnf(double a, double b, double c, double d, int e) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_pnf(double a, double b, double c, double d, int e, int f) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_qnf(double a, double b, double c, double d, int e, int f) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_dnt(double a, double b, double c, int d) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_pnt(double a, double b, double c, int d, int e) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_qnt(double a, double b, double c, int d, int e) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_ptukey(double a, double b, double c, double d, int e, int f) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_qtukey(double a, double b, double c, double d, int e, int f) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_dwilcox(double a, double b, double c, int d) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_pwilcox(double a, double b, double c, int d, int e) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_qwilcox(double a, double b, double c, int d, int e) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_rwilcox(double a, double b) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_dsignrank(double a, double b, int c) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_psignrank(double a, double b, int c, int d) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_qsignrank(double a, double b, int c, int d) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_rsignrank(double a) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_ftrunc(double a) {
        if (a > 0) {
            return Math.floor(a);
        } else {
            return Math.ceil(a);
        }
    }

    @Override
    public Object Rf_namesgets(Object x, Object y) {
        throw implementedAsNode();
    }

    @Override
    public void Rf_copyMostAttrib(Object x, Object y) {
        throw implementedAsNode();
    }

    @Override
    public Object Rf_VectorToPairList(Object x) {
        throw implementedAsNode();
    }

    @Override
    public Object Rf_asCharacterFactor(Object x) {
        throw implementedAsNode();
    }

    private static RFFIContext getContext() {
        return RContext.getInstance().getStateRFFI();
    }

    @Override
    public Object Rf_match(Object itables, Object ix, int nmatch) {
        throw implementedAsNode();
    }

    @Override
    public boolean Rf_NonNullStringMatch(Object s, Object t) {
        throw implementedAsNode();
    }

    @Override
    public int R_has_slot(Object container, Object name) {
        throw implementedAsNode();
    }

    @Override
    public Object FORMALS(Object x) {
        throw implementedAsNode();
    }

    @Override
    public Object BODY(Object x) {
        throw implementedAsNode();
    }

    @Override
    public Object CLOENV(Object x) {
        throw implementedAsNode();
    }

    @Override
    public void SET_FORMALS(Object x, Object y) {
        throw implementedAsNode();
    }

    @Override
    public void SET_BODY(Object x, Object y) {
        throw implementedAsNode();
    }

    @Override
    public void SET_CLOENV(Object x, Object y) {
        throw implementedAsNode();
    }

    @Override
    public Object octsize(Object size) {
        throw implementedAsNode();
    }

    public static class VectorWrapperNativePointer implements TruffleObject {

        private final TruffleObject vector;

        VectorWrapperNativePointer(TruffleObject vector) {
            this.vector = vector;
            assert vector instanceof RObject;
            NativeDataAccess.asPointer(vector); // initialize the native mirror in the vector
        }

        abstract static class InteropRootNode extends RootNode {
            InteropRootNode() {
                super(/* TruffleRLanguageImpl.getCurrentLanguage() */null);
            }

            @Override
            public final SourceSection getSourceSection() {
                return RSyntaxNode.INTERNAL;
            }
        }

        // TODO: with separate version of this for the different types, it would be more efficient
        // and not need the dispatch
        public abstract static class DispatchAllocate extends Node {
            private static final long EMPTY_DATA_ADDRESS = 0x1BAD;

            public abstract long execute(Object vector);

            @Specialization
            @TruffleBoundary
            protected static long get(RIntVector vector) {
                return vector.allocateNativeContents();
            }

            @Specialization
            @TruffleBoundary
            protected static long get(RLogicalVector vector) {
                return vector.allocateNativeContents();
            }

            @Specialization
            @TruffleBoundary
            protected static long get(RRawVector vector) {
                return vector.allocateNativeContents();
            }

            @Specialization
            @TruffleBoundary
            protected static long get(RDoubleVector vector) {
                return vector.allocateNativeContents();
            }

            @Specialization
            @TruffleBoundary
            protected static long get(RComplexVector vector) {
                return vector.allocateNativeContents();
            }

            @Specialization
            @TruffleBoundary
            protected static long get(CharSXPWrapper vector) {
                return vector.allocateNativeContents();
            }

            @Specialization
            protected static long get(@SuppressWarnings("unused") RNull nullValue) {
                // Note: GnuR is OK with, e.g., INTEGER(NULL), but it's illegal to read from or
                // write to the resulting address.
                return EMPTY_DATA_ADDRESS;
            }

            @Fallback
            protected static long get(Object vector) {
                throw RInternalError.shouldNotReachHere("invalid wrapped object " + vector.getClass().getSimpleName());
            }
        }

        @Override
        public ForeignAccess getForeignAccess() {
            return ForeignAccess.create(VectorWrapperNativePointer.class, new StandardFactory() {
                @Override
                public CallTarget accessIsNull() {
                    return Truffle.getRuntime().createCallTarget(new InteropRootNode() {
                        @Override
                        public Object execute(VirtualFrame frame) {
                            return false;
                        }
                    });
                }

                @Override
                public CallTarget accessIsPointer() {
                    return Truffle.getRuntime().createCallTarget(new InteropRootNode() {
                        @Override
                        public Object execute(VirtualFrame frame) {
                            return true;
                        }
                    });
                }

                @Override
                public CallTarget accessAsPointer() {
                    return Truffle.getRuntime().createCallTarget(new InteropRootNode() {
                        @Child private DispatchAllocate dispatch = DispatchAllocateNodeGen.create();

                        @Override
                        public Object execute(VirtualFrame frame) {
                            VectorWrapperNativePointer receiver = (VectorWrapperNativePointer) ForeignAccess.getReceiver(frame);
                            return dispatch.execute(receiver.vector);
                        }
                    });
                }

                @Override
                public CallTarget accessToNative() {
                    return Truffle.getRuntime().createCallTarget(new InteropRootNode() {
                        @Override
                        public Object execute(VirtualFrame frame) {
                            return ForeignAccess.getReceiver(frame);
                        }
                    });
                }
            });
        }
    }

    @MessageResolution(receiverType = VectorWrapper.class)
    public static class VectorWrapperMR {

        @Resolve(message = "IS_POINTER")
        public abstract static class IntVectorWrapperNativeIsPointerNode extends Node {
            protected Object access(@SuppressWarnings("unused") VectorWrapper receiver) {
                return false;
            }
        }

        @Resolve(message = "TO_NATIVE")
        public abstract static class IntVectorWrapperNativeAsPointerNode extends Node {
            protected Object access(VectorWrapper receiver) {
                return new VectorWrapperNativePointer(receiver.vector);
            }
        }

        @Resolve(message = "HAS_SIZE")
        public abstract static class VectorWrapperHasSizeNode extends Node {
            protected Object access(@SuppressWarnings("unused") VectorWrapper receiver) {
                return true;
            }
        }

        @Resolve(message = "GET_SIZE")
        public abstract static class VectorWrapperGetSizeNode extends Node {
            @Child private Node getSizeMsg = Message.GET_SIZE.createNode();

            protected Object access(VectorWrapper receiver) {
                try {
                    return ForeignAccess.sendGetSize(getSizeMsg, receiver.vector);
                } catch (UnsupportedMessageException e) {
                    throw RInternalError.shouldNotReachHere(e);
                }
            }
        }

        @Resolve(message = "READ")
        abstract static class VectorWrapperReadNode extends Node {
            @Child private Node readMsg = Message.READ.createNode();

            public Object access(VectorWrapper receiver, Object index) {
                try {
                    return ForeignAccess.sendRead(readMsg, receiver.vector, index);
                } catch (UnsupportedMessageException | UnknownIdentifierException e) {
                    throw RInternalError.shouldNotReachHere(e);
                }
            }
        }

        @Resolve(message = "WRITE")
        abstract static class VectorWrapperWriteNode extends Node {
            @Child private Node writeMsg = Message.WRITE.createNode();

            public Object access(VectorWrapper receiver, Object index, Object value) {
                try {
                    return ForeignAccess.sendWrite(writeMsg, receiver.vector, index, value);
                } catch (UnsupportedMessageException | UnknownIdentifierException | UnsupportedTypeException e) {
                    throw RInternalError.shouldNotReachHere(e);
                }
            }
        }

        @Resolve(message = "IS_EXECUTABLE")
        abstract static class VectorWrapperIsExecutableNode extends Node {
            @Child private Node isExecMsg = Message.IS_EXECUTABLE.createNode();

            public Object access(VectorWrapper receiver) {
                return ForeignAccess.sendIsExecutable(isExecMsg, receiver.vector);
            }
        }

        @Resolve(message = "EXECUTE")
        abstract static class VectorWrapperExecuteNode extends Node {
            @Child private Node execMsg = Message.createExecute(0).createNode();

            protected Object access(VectorWrapper receiver, Object[] arguments) {
                try {
                    // Currently, there is only one "executable" object, which is
                    // CharSXPWrapper.
                    // See CharSXPWrapperMR for the EXECUTABLE message handler.
                    assert arguments.length == 0 && receiver.vector instanceof CharSXPWrapper;
                    return ForeignAccess.sendExecute(execMsg, receiver.vector);
                } catch (UnsupportedMessageException | UnsupportedTypeException | ArityException e) {
                    throw RInternalError.shouldNotReachHere(e);
                }
            }
        }

        @CanResolve
        public abstract static class VectorWrapperCheck extends Node {
            protected static boolean test(TruffleObject receiver) {
                return receiver instanceof VectorWrapper;
            }
        }
    }

    public static final class VectorWrapper implements TruffleObject {

        private final TruffleObject vector;

        public VectorWrapper(TruffleObject vector) {
            assert vector instanceof RObject;
            this.vector = vector;
            NativeDataAccess.setNativeWrapper((RObject) vector, this);
        }

        public static Object get(TruffleObject x) {
            assert x instanceof RObject;
            Object wrapper = NativeDataAccess.getNativeWrapper((RObject) x);
            if (wrapper != null) {
                return wrapper;
            } else {
                wrapper = new VectorWrapper(x);
                // Establish the 1-1 relationship between the object and its native wrapper
                NativeDataAccess.setNativeWrapper((RObject) x, wrapper);
                return wrapper;
            }
        }

        public TruffleObject getVector() {
            return vector;
        }

        @Override
        public ForeignAccess getForeignAccess() {
            return VectorWrapperMRForeign.ACCESS;
        }

        @Override
        public int hashCode() {
            return vector.hashCode();
        }

    }

    @Override
    public Object INTEGER(Object x) {
        // also handles LOGICAL
        assert x instanceof RIntVector || x instanceof RLogicalVector || x == RNull.instance;
        return VectorWrapper.get(guaranteeVectorOrNull(x, RVector.class));
    }

    @Override
    public Object LOGICAL(Object x) {
        return VectorWrapper.get(guaranteeVectorOrNull(x, RLogicalVector.class));
    }

    @Override
    public Object REAL(Object x) {
        return VectorWrapper.get(guaranteeVectorOrNull(x, RDoubleVector.class));
    }

    @Override
    public Object RAW(Object x) {
        return VectorWrapper.get(guaranteeVectorOrNull(x, RRawVector.class));
    }

    @Override
    public Object COMPLEX(Object x) {
        return VectorWrapper.get(guaranteeVectorOrNull(x, RComplexVector.class));
    }

    @Override
    public Object R_CHAR(Object x) {
        return VectorWrapper.get(guaranteeVectorOrNull(x, CharSXPWrapper.class));
    }

    @Override
    public void Rf_PrintValue(Object value) {
        throw implementedAsNode();
    }

    private static TruffleObject guaranteeVectorOrNull(Object obj, Class<? extends TruffleObject> clazz) {
        if (obj == RNull.instance) {
            return RNull.instance;
        }
        return guaranteeInstanceOf(obj, clazz);
    }
}
