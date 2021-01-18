/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
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
import java.util.function.Function;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameInstance.FrameAccess;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.ffi.impl.javaGD.JavaGDContext;
import com.oracle.truffle.r.ffi.impl.upcalls.UpCallsRFFI;
import com.oracle.truffle.r.nodes.RASTUtils;
import com.oracle.truffle.r.nodes.function.ClassHierarchyNode;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RCaller;
import com.oracle.truffle.r.runtime.RCleanUp;
import com.oracle.truffle.r.runtime.REnvVars;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
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
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RAttributesLayout;
import com.oracle.truffle.r.runtime.data.RBaseObject;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RComplexVector;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.REmpty;
import com.oracle.truffle.r.runtime.data.RExpression;
import com.oracle.truffle.r.runtime.data.RExternalPtr;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.data.RPromise.EagerPromise;
import com.oracle.truffle.r.runtime.data.RRaw;
import com.oracle.truffle.r.runtime.data.RRawVector;
import com.oracle.truffle.r.runtime.data.RSharingAttributeStorage;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.data.RUnboundValue;
import com.oracle.truffle.r.runtime.data.RWeakRef;
import com.oracle.truffle.r.runtime.data.altrep.AltComplexClassDescriptor;
import com.oracle.truffle.r.runtime.data.altrep.AltIntegerClassDescriptor;
import com.oracle.truffle.r.runtime.data.altrep.AltLogicalClassDescriptor;
import com.oracle.truffle.r.runtime.data.altrep.AltRawClassDescriptor;
import com.oracle.truffle.r.runtime.data.altrep.AltRealClassDescriptor;
import com.oracle.truffle.r.runtime.data.altrep.AltRepClassDescriptor;
import com.oracle.truffle.r.runtime.data.altrep.AltStringClassDescriptor;
import com.oracle.truffle.r.runtime.data.altrep.AltVecClassDescriptor;
import com.oracle.truffle.r.runtime.data.altrep.AltrepMethodDescriptor;
import com.oracle.truffle.r.runtime.data.altrep.AltrepUtilities;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.data.nodes.ShareObjectNode;
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
import com.oracle.truffle.r.runtime.ffi.FFIMaterializeNode;
import com.oracle.truffle.r.runtime.ffi.RFFIFactory.Type;
import com.oracle.truffle.r.runtime.ffi.RObjectDataPtr;
import com.oracle.truffle.r.runtime.ffi.util.NativeMemory;
import com.oracle.truffle.r.runtime.ffi.util.NativeMemory.ElementType;
import com.oracle.truffle.r.runtime.gnur.SA_TYPE;
import com.oracle.truffle.r.runtime.gnur.SEXPTYPE;
import com.oracle.truffle.r.runtime.nmath.RMath;
import com.oracle.truffle.r.runtime.nmath.RandomFunctions.RandomNumberProvider;
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

    private static RuntimeException implementedAsNode() {
        // TODO: Exception handling over native boundaries is currently missing. Once this works,
        // remove the following two lines.
        System.err.println("upcall function is implemented via a node");
        System.exit(1);

        return RInternalError.shouldNotReachHere("upcall function is implemented via a node");
    }

    // Checkstyle: stop method name check

    @Override
    public RComplexVector Rf_ScalarComplex(double real, double imag) {
        return RDataFactory.createComplexVectorFromScalar(RComplex.valueOf(real, imag));
    }

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
    public RRawVector Rf_ScalarRaw(int value) {
        return RDataFactory.createRawVectorFromScalar(RRaw.valueOf((byte) value));
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
            if (value == RUnboundValue.instance) {
                env.rm(name.getName());
            } else {
                env.put(name.getName(), value);
            }
        } catch (PutException ex) {
            throw RError.error(RError.SHOW_CALLER2, ex);
        }
    }

    @Override
    @TruffleBoundary
    public Object R_getClassDef(String clazz) {
        throw implementedAsNode();
    }

    @Override
    @TruffleBoundary
    public Object R_do_MAKE_CLASS(String clazz) {
        throw implementedAsNode();
    }

    @Override
    public Object R_do_new_object(Object classDef) {
        throw implementedAsNode();
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

    @TruffleBoundary
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
            String nameKey = name.getName();
            Object value = env.get(nameKey);
            if (value != null) {
                if (value instanceof RPromise) {
                    RPromise promise = (RPromise) value;
                    // The calling site in native code should not get a promise with a null frame
                    // that could be an issue when PRENV is then called on the promise. The only
                    // exception is EagerPromise for which there is special handling in PRENV
                    // reconstructing the original frame via EagetPromise.materialize().
                    assert promise instanceof EagerPromise || promise.getFrame() != null;
                    return promise;
                }
                if (value instanceof RArgsValuesAndNames) {
                    RArgsValuesAndNames argsValsNames = (RArgsValuesAndNames) value;
                    if (argsValsNames.isEmpty()) {
                        return RSymbol.MISSING;
                    } else {
                        return argsValsNames.toPairlist();
                    }
                }
                if (value == RMissing.instance || value == REmpty.instance) {
                    return RSymbol.MISSING;
                }
                Object res = FFIMaterializeNode.uncachedMaterialize(value);
                if (res != value) {
                    env.putOverrideLock(nameKey, res);
                }
                return res;
            }
            if (!inherits) {
                // single frame lookup
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
    public void Rf_setAttrib(Object obj, Object name, Object val) {
        throw implementedAsNode();
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
        if (x == RNull.instance) {
            return RNull.instance;
        }
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
    public Object Rf_allocVector(int mode, long n) {
        throw implementedAsNode();
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

    @Override
    @TruffleBoundary
    public Object Rf_allocList(int length) {
        Object result = RNull.instance;
        for (int i = 0; i < length; i++) {
            result = RDataFactory.createPairList(RNull.instance, result);
        }
        return result;
    }

    @Override
    @TruffleBoundary
    public Object Rf_allocSExp(int mode) {
        SEXPTYPE type = SEXPTYPE.mapInt(mode);
        switch (type) {
            case ENVSXP:
                return RDataFactory.createNewEnv(null);
            case LISTSXP:
                return RDataFactory.createPairList(RNull.instance, RNull.instance);
            case LANGSXP:
                return RDataFactory.createPairList(1, type);
            case CLOSXP:
                return RDataFactory.createFunction("<unknown-from-Rf_allocSExp>", "RFFI", null, null, REnvironment.globalEnv().getFrame());
            default:
                throw unimplemented("unexpected SEXPTYPE " + type);
        }
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
            case RAWSXP:
                return RDataFactory.createRawVector(new byte[(nrow * ncol)], dims);
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
    public void SETLENGTH(Object x, int l) {
        RAbstractVector vec = (RAbstractVector) RRuntime.asAbstractVector(x);
        vec.setLength(l);
    }

    @Override
    public void SET_TRUELENGTH(Object x, int l) {
        throw implementedAsNode();
    }

    @Override
    public int TRUELENGTH(Object x) {
        throw implementedAsNode();
    }

    @Override
    public int LEVELS(Object x) {
        if (x instanceof RBaseObject) {
            return ((RBaseObject) x).getGPBits();
        }
        throw RInternalError.shouldNotReachHere();
    }

    @Override
    public void SETLEVELS(Object x, int gpbits) {
        if (x instanceof RBaseObject) {
            ((RBaseObject) x).setGPBits(gpbits);
        } else {
            throw RInternalError.shouldNotReachHere();
        }
    }

    @Override
    @TruffleBoundary
    public int Rf_isObject(Object x) {
        throw implementedAsNode();
    }

    @Override
    public void SET_STRING_ELT(Object x, long i, Object v) {
        throw implementedAsNode();
    }

    @Override
    public void SET_VECTOR_ELT(Object x, long i, Object v) {
        RList list = guaranteeInstanceOf(x, RList.class);
        list.setElement((int) i, v);
    }

    @Override
    public void SET_ATTRIB(Object target, Object attributes) {
        throw implementedAsNode();
    }

    @Override
    public Object STRING_ELT(Object x, long i) {
        throw implementedAsNode();
    }

    @Override
    public Object VECTOR_ELT(Object x, long i) {
        throw implementedAsNode();
    }

    @Override
    public int NAMED(Object x) {
        if (RSharingAttributeStorage.isShareable(x)) {
            return ((RSharingAttributeStorage) x).isTemporary() ? 0 : ((RSharingAttributeStorage) x).isShared() ? 2 : 1;
        } else {
            // Note: it may be that we need to remember this for all types, GNUR does
            return 2;
        }
    }

    @Override
    public void SET_OBJECT(Object x, int flag) {
        throw implementedAsNode();
    }

    @Override
    public void SET_NAMED_FASTR(Object x, int v) {
        // Note: In GNUR this is a macro that sets the sxpinfo.named regardless of whether it makes
        // sense to name the actual value, for compatibility we simply ignore values that are not
        // RShareable, e.g. RSymbol. However we ignore and report attempts to decrease the
        // ref-count,
        // which as it seems GNUR would just let proceed
        // Note 2: there is a hack in data.table that uses SET_NAMED(x,0) to make something mutable
        // so we allow and "support" this one specific use-case.
        if (x instanceof RSharingAttributeStorage) {
            RSharingAttributeStorage r = (RSharingAttributeStorage) x;
            if (v >= 2) {
                // we play it safe: if the caller wants this instance to be shared, they may expect
                // it to never become non-shared again, which could happen in FastR
                r.makeSharedPermanent();
            }
            if (v == 1 && r.isTemporary()) {
                r.incRefCount();
            }
            if (v == 0) {
                if (r.isSharedPermanent()) {
                    CompilerDirectives.transferToInterpreter();
                    RError.warning(RError.NO_CALLER, RError.Message.GENERIC,
                                    "Native code of some package requested that a shared permanent value is made temporary. " +
                                                    "This is a hack that may break things even on GNU-R, but for the sake of compatibility, FastR will proceed.");
                }
                r.makeTemporary();
            }
        }
    }

    @Override
    public void SET_TYPEOF(Object x, int v) {
        guaranteeInstanceOf(x, RPairList.class).setType(SEXPTYPE.mapInt(v));
    }

    @Override
    public int TYPEOF(Object x) {
        throw implementedAsNode();
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
    public Object Rf_duplicated(Object x, int fromLast) {
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
        return ((RSymbol) x).getWrappedName();
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
        guaranteeInstanceOf(x, RPairList.class).setCdr(y);
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
            throw RInternalError.shouldNotReachHere(Utils.getTypeName(x));
        }
        REnvironment baseEnv = REnvironment.baseEnv();
        String name = ((RSymbol) x).getName();
        Object res = baseEnv.get(name);
        if (res == null) {
            return RUnboundValue.instance;
        }
        Object materialized = FFIMaterializeNode.uncachedMaterialize(res);
        if (materialized != res) {
            ShareObjectNode.executeUncached(res);
            baseEnv.putOverrideLock(name, materialized);
        }
        return materialized;
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
        REnvironment registry = RContext.getInstance().stateREnvironment.getNamespaceRegistry();
        Object result = registry.get(RRuntime.asString(name));
        // otherwise we would have to FFIWrap and write-back
        assert result instanceof REnvironment : result;
        return result;
    }

    @Override
    public Object Rf_eval(Object expr, Object env) {
        throw implementedAsNode();
    }

    @Override
    public Object Rf_findFun(Object symbolObj, Object envObj) {
        throw implementedAsNode();
    }

    @Override
    @TruffleBoundary
    public Object Rf_GetOption1(Object tag) {
        guaranteeInstanceOf(tag, RSymbol.class);
        return RContext.getInstance().stateROptions.getValue(((RSymbol) tag).getName());
    }

    @Override
    @TruffleBoundary
    public void Rf_gsetVar(Object symbol, Object value, Object rho) {
        guaranteeInstanceOf(symbol, RSymbol.class);
        REnvironment baseEnv = RContext.getInstance().stateREnvironment.getBaseEnv();
        guarantee(rho == RNull.instance || rho == baseEnv);
        try {
            baseEnv.put(((RSymbol) symbol).getName(), value);
        } catch (PutException e) {
            e.printStackTrace();
        }
    }

    @Override
    @TruffleBoundary
    public void Rf_setVar(Object symbol, Object value, Object rho) {
        if (rho == RNull.instance) {
            throw RError.error(RError.SHOW_CALLER2, RError.Message.USE_NULL_ENV_DEFUNCT);
        }
        if (!(rho instanceof REnvironment)) {
            throw RError.error(RError.SHOW_CALLER2, RError.Message.ARG_NOT_AN_ENVIRONMENT, "setVar");
        }
        REnvironment env = (REnvironment) rho;
        String symName = ((RSymbol) symbol).getName();
        while (env != REnvironment.emptyEnv()) {
            if (env.get(symName) != null) {
                if (env.bindingIsLocked(symName)) {
                    throw RError.error(RError.SHOW_CALLER2, Message.ENV_CHANGE_BINDING, symName);
                }
                try {
                    env.put(symName, value);
                } catch (PutException ex) {
                    throw RError.error(RError.SHOW_CALLER2, ex);
                }
            }
            env = env.getParent();
        }
        Rf_defineVar(symbol, value, REnvironment.globalEnv());
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
    @TruffleBoundary
    public void SET_ENCLOS(Object x, Object enc) {
        REnvironment env = guaranteeInstanceOf(x, REnvironment.class);
        REnvironment enclosing = guaranteeInstanceOf(enc, REnvironment.class);
        env.setParent(enclosing);
    }

    @Override
    public Object PRVALUE(Object x) {
        RPromise p = guaranteeInstanceOf(x, RPromise.class);
        if (!p.isEvaluated()) {
            return RUnboundValue.instance;
        }
        Object val = p.getValue();
        Object materialized = FFIMaterializeNode.uncachedMaterialize(val);
        if (materialized != val) {
            ShareObjectNode.executeUncached(materialized);
            p.updateValue(materialized);
        }
        return materialized;
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
        assert n == 1 || (n < 0 && (text instanceof String || (text instanceof RStringVector && ((RStringVector) text).getLength() == 1))) : "unsupported: R_ParseVector with n != 0.";
        assert srcFile == RNull.instance : "unsupported: R_ParseVector with non-null srcFile argument.";
        String textString = RRuntime.asString(text);
        assert textString != null;

        Object[] resultData = new Object[2];
        try {
            Source source = RSource.fromTextInternal(textString, RSource.Internal.R_PARSEVECTOR);
            RExpression exprs = RContext.getEngine().parse(source, false).getExpression();
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
        // Even in GNU-R the result is a fresh vector not owned by the environment
        boolean sorted = sortedArg != 0;
        boolean all = allArg != 0;
        REnvironment env = guaranteeInstanceOf(envArg, REnvironment.class);
        return env.ls(all, null, sorted);
    }

    @Override
    @TruffleBoundary
    public String R_HomeDir() {
        // This should actually return char* on the native side and there is no documentation
        // regarding the ownership of the result. So far there has not been any issues with this
        return REnvVars.rHome(RContext.getInstance());
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
        throw implementedAsNode();
    }

    @Override
    public Object R_BaseEnv() {
        throw implementedAsNode();
    }

    @Override
    public Object R_BaseNamespace() {
        throw implementedAsNode();
    }

    @Override
    public Object R_NamespaceRegistry() {
        throw implementedAsNode();
    }

    @Override
    public int R_Interactive() {
        throw implementedAsNode();
    }

    @Override
    public int IS_S4_OBJECT(Object x) {
        return ((x instanceof RBaseObject) && ((RBaseObject) x).isS4()) ? 1 : 0;
    }

    @Override
    public Object Rf_asS4(Object x, int b, int i) {
        throw implementedAsNode();
    }

    @Override
    public void SET_S4_OBJECT(Object x) {
        guaranteeInstanceOf(x, RBaseObject.class).setS4();
    }

    @Override
    public void UNSET_S4_OBJECT(Object x) {
        guaranteeInstanceOf(x, RBaseObject.class).unsetS4();
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

    @Override
    @TruffleBoundary
    public double norm_rand() {
        return RandomNumberProvider.fromCurrentRNG().normRand();
    }

    @Override
    @TruffleBoundary
    public double exp_rand() {
        return RandomNumberProvider.fromCurrentRNG().expRand();
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
            currentCaller = currentCaller.getPrevious();
        }
        return currentCaller == null || currentCaller == RCaller.topLevel ? RNull.instance : currentCaller;
    }

    @Override
    @TruffleBoundary
    public Object R_getParentFunctionContext(Object c) {
        Utils.warn("Potential memory leak (parent function context object)");
        RCaller currentCaller = guaranteeInstanceOf(c, RCaller.class);
        while (true) {
            currentCaller = currentCaller.getPrevious();
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
        // Note: frame holds its environment in the arguments array
        RCaller rCaller = guaranteeInstanceOf(c, RCaller.class);
        if (rCaller == RCaller.topLevel) {
            return RContext.getInstance().stateREnvironment.getGlobalEnv();
        }
        Frame frame = Utils.getActualCurrentFrame();
        if (RArguments.getCall(frame) == rCaller) {
            return REnvironment.frameToEnvironment(frame.materialize());
        } else {
            return Utils.iterateRFrames(FrameAccess.READ_ONLY, new Function<Frame, Object>() {

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
            return Utils.iterateRFrames(FrameAccess.READ_ONLY, new Function<Frame, Object>() {

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
        }
    }

    @Override
    @TruffleBoundary
    public Object R_getContextCall(Object c) {
        RCaller rCaller = guaranteeInstanceOf(c, RCaller.class);
        if (rCaller == RCaller.topLevel) {
            return RNull.instance;
        }
        Utils.warn("Potential memory leak (pair-list constructed from RCaller)");
        return RContext.getRRuntimeASTAccess().getSyntaxCaller(rCaller);
    }

    @Override
    @TruffleBoundary
    public Object R_getContextSrcRef(Object c) {
        // TODO: fix ownership, used in IDE integration
        Object o = R_getContextFun(c);
        if (!(o instanceof RFunction)) {
            return RNull.instance;
        } else {
            RFunction f = (RFunction) o;
            SourceSection ss = f.getRootNode().getSourceSection();
            String path = RSource.getPath(ss.getSource());
            // TODO: is it OK to pass "" if path is null?
            return RSrcref.createLloc(RContext.getInstance(), ss, path == null ? "" : path);
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
    public int ALTREP(Object x) {
        return AltrepUtilities.isAltrep(x) ? 1 : 0;
    }

    // TODO
    @Override
    public boolean R_altrep_inherits(Object instance, Object classDescriptor) {
        return true;
    }

    @Override
    public Object R_altrep_data1(Object instance) {
        throw implementedAsNode();
    }

    @Override
    public Object R_altrep_data2(Object instance) {
        throw implementedAsNode();
    }

    @Override
    public void R_set_altrep_data1(Object instance, Object data1) {
        throw implementedAsNode();
    }

    @Override
    public void R_set_altrep_data2(Object instance, Object data2) {
        throw implementedAsNode();
    }

    @Override
    public Object R_make_altinteger_class(String className, String packageName, Object dllInfo) {
        throw implementedAsNode();
    }

    @Override
    public Object R_make_altreal_class(String className, String packageName, Object dllInfo) {
        throw implementedAsNode();
    }

    @Override
    public Object R_make_altlogical_class(String className, String packageName, Object dllInfo) {
        throw implementedAsNode();
    }

    @Override
    public Object R_make_altstring_class(String className, String packageName, Object dllInfo) {
        throw implementedAsNode();
    }

    @Override
    public Object R_make_altraw_class(String className, String packageName, Object dllInfo) {
        throw implementedAsNode();
    }

    @Override
    public Object R_make_altcomplex_class(String className, String packageName, Object dllInfo) {
        throw implementedAsNode();
    }

    /**
     * Converts the given {@code method} argument into an executable method and wraps it in
     * {@link AltrepMethodDescriptor}.
     * 
     * @param method A reference to a native function
     * @param signature Signature of the {@code method}
     */
    private static AltrepMethodDescriptor createAltrepMethodDescriptor(Object method, String signature) {
        InteropLibrary interop = InteropLibrary.getUncached();
        if (!interop.isExecutable(method)) {
            if (interop.isMemberInvocable(method, "bind")) {
                try {
                    Object boundMethod = interop.invokeMember(method, "bind", signature);
                    return new AltrepMethodDescriptor(boundMethod, Type.NFI);
                } catch (InteropException e) {
                    throw RInternalError.shouldNotReachHere(e);
                }
            } else {
                throw RInternalError.shouldNotReachHere("method is from NFI, it should have 'bind' invocable member");
            }
        }
        return new AltrepMethodDescriptor(method, Type.LLVM);
    }

    @Override
    public void R_set_altrep_Unserialize_method(Object classDescriptor, Object method) {
        AltRepClassDescriptor classDescr = guaranteeInstanceOf(classDescriptor, AltRepClassDescriptor.class);
        AltrepMethodDescriptor altrepMethodDescriptor = createAltrepMethodDescriptor(method, AltRepClassDescriptor.unserializeMethodSignature);
        classDescr.registerUnserializeMethod(altrepMethodDescriptor);
    }

    @Override
    public void R_set_altrep_Length_method(Object classDescriptor, Object method) {
        AltRepClassDescriptor classDescr = guaranteeInstanceOf(classDescriptor, AltRepClassDescriptor.class);
        AltrepMethodDescriptor altrepMethodDescriptor = createAltrepMethodDescriptor(method, AltRepClassDescriptor.lengthMethodSignature);
        classDescr.registerLengthMethod(altrepMethodDescriptor);
    }

    @Override
    public void R_set_altrep_UnserializeEX_method(Object classDescriptor, Object method) {
        AltRepClassDescriptor classDescr = guaranteeInstanceOf(classDescriptor, AltRepClassDescriptor.class);
        AltrepMethodDescriptor altrepMethodDescriptor = createAltrepMethodDescriptor(method, AltRepClassDescriptor.unserializeEXMethodSignature);
        classDescr.registerUnserializeEXMethod(altrepMethodDescriptor);
    }

    @Override
    public void R_set_altrep_Serialized_state_method(Object classDescriptor, Object method) {
        AltRepClassDescriptor classDescr = guaranteeInstanceOf(classDescriptor, AltRepClassDescriptor.class);
        AltrepMethodDescriptor altrepMethodDescriptor = createAltrepMethodDescriptor(method, AltRepClassDescriptor.serializedStateMethodSignature);
        classDescr.registerSerializedStateMethod(altrepMethodDescriptor);
    }

    @Override
    public void R_set_altrep_Duplicate_method(Object classDescriptor, Object method) {
        AltRepClassDescriptor classDescr = guaranteeInstanceOf(classDescriptor, AltRepClassDescriptor.class);
        AltrepMethodDescriptor altrepMethodDescriptor = createAltrepMethodDescriptor(method, AltRepClassDescriptor.duplicateMethodSignature);
        classDescr.registerDuplicateMethod(altrepMethodDescriptor);
    }

    @Override
    public void R_set_altrep_DuplicateEX_method(Object classDescriptor, Object method) {
        AltRepClassDescriptor classDescr = guaranteeInstanceOf(classDescriptor, AltRepClassDescriptor.class);
        AltrepMethodDescriptor altrepMethodDescriptor = createAltrepMethodDescriptor(method, AltRepClassDescriptor.duplicateEXMethodSignature);
        classDescr.registerDuplicateEXMethod(altrepMethodDescriptor);
    }

    @Override
    public void R_set_altrep_Coerce_method(Object classDescriptor, Object method) {
        AltRepClassDescriptor classDescr = guaranteeInstanceOf(classDescriptor, AltRepClassDescriptor.class);
        AltrepMethodDescriptor altrepMethodDescriptor = createAltrepMethodDescriptor(method, AltRepClassDescriptor.coerceMethodSignature);
        classDescr.registerCoerceMethod(altrepMethodDescriptor);
    }

    @Override
    public void R_set_altrep_Inspect_method(Object classDescriptor, Object method) {
        AltRepClassDescriptor classDescr = guaranteeInstanceOf(classDescriptor, AltRepClassDescriptor.class);
        AltrepMethodDescriptor altrepMethodDescriptor = createAltrepMethodDescriptor(method, AltRepClassDescriptor.inspectMethodSignature);
        classDescr.registerInspectMethod(altrepMethodDescriptor);
    }

    @Override
    public void R_set_altvec_Dataptr_method(Object classDescriptor, Object method) {
        AltVecClassDescriptor classDescr = guaranteeInstanceOf(classDescriptor, AltVecClassDescriptor.class);
        AltrepMethodDescriptor altrepMethodDescriptor = createAltrepMethodDescriptor(method, AltVecClassDescriptor.dataptrMethodSignature);
        classDescr.registerDataptrMethod(altrepMethodDescriptor);
    }

    @Override
    public void R_set_altvec_Dataptr_or_null_method(Object classDescriptor, Object method) {
        AltVecClassDescriptor classDescr = guaranteeInstanceOf(classDescriptor, AltVecClassDescriptor.class);
        AltrepMethodDescriptor altrepMethodDescriptor = createAltrepMethodDescriptor(method, AltVecClassDescriptor.dataptrOrNullMethodSignature);
        classDescr.registerDataptrOrNullMethod(altrepMethodDescriptor);
    }

    @Override
    public void R_set_altvec_Extract_subset_method(Object classDescriptor, Object method) {
        AltVecClassDescriptor classDescr = guaranteeInstanceOf(classDescriptor, AltVecClassDescriptor.class);
        AltrepMethodDescriptor altrepMethodDescriptor = createAltrepMethodDescriptor(method, AltVecClassDescriptor.extractSubsetMethodSignature);
        classDescr.registerExtractSubsetMethod(altrepMethodDescriptor);
    }

    @Override
    public void R_set_altinteger_Elt_method(Object classDescriptor, Object method) {
        AltIntegerClassDescriptor classDescr = guaranteeInstanceOf(classDescriptor, AltIntegerClassDescriptor.class);
        AltrepMethodDescriptor altrepMethodDescriptor = createAltrepMethodDescriptor(method, AltIntegerClassDescriptor.eltMethodSignature);
        classDescr.registerEltMethod(altrepMethodDescriptor);
    }

    @Override
    public void R_set_altinteger_Get_region_method(Object classDescriptor, Object method) {
        AltIntegerClassDescriptor classDescr = guaranteeInstanceOf(classDescriptor, AltIntegerClassDescriptor.class);
        AltrepMethodDescriptor altrepMethodDescriptor = createAltrepMethodDescriptor(method, AltIntegerClassDescriptor.getRegionMethodSignature);
        classDescr.registerGetRegionMethod(altrepMethodDescriptor);
    }

    @Override
    public void R_set_altinteger_Is_sorted_method(Object classDescriptor, Object method) {
        AltIntegerClassDescriptor classDescr = guaranteeInstanceOf(classDescriptor, AltIntegerClassDescriptor.class);
        AltrepMethodDescriptor altrepMethodDescriptor = createAltrepMethodDescriptor(method, AltIntegerClassDescriptor.isSortedMethodSignature);
        classDescr.registerIsSortedMethod(altrepMethodDescriptor);
    }

    @Override
    public void R_set_altinteger_No_NA_method(Object classDescriptor, Object method) {
        AltIntegerClassDescriptor classDescr = guaranteeInstanceOf(classDescriptor, AltIntegerClassDescriptor.class);
        AltrepMethodDescriptor altrepMethodDescriptor = createAltrepMethodDescriptor(method, AltIntegerClassDescriptor.noNAMethodSignature);
        classDescr.registerNoNAMethod(altrepMethodDescriptor);
    }

    @Override
    public void R_set_altinteger_Sum_method(Object classDescriptor, Object method) {
        AltIntegerClassDescriptor classDescr = guaranteeInstanceOf(classDescriptor, AltIntegerClassDescriptor.class);
        AltrepMethodDescriptor altrepMethodDescriptor = createAltrepMethodDescriptor(method, AltIntegerClassDescriptor.sumMethodSignature);
        classDescr.registerSumMethod(altrepMethodDescriptor);
    }

    @Override
    public void R_set_altinteger_Min_method(Object classDescriptor, Object method) {
        AltIntegerClassDescriptor classDescr = guaranteeInstanceOf(classDescriptor, AltIntegerClassDescriptor.class);
        AltrepMethodDescriptor altrepMethodDescriptor = createAltrepMethodDescriptor(method, AltIntegerClassDescriptor.minMethodSignature);
        classDescr.registerMinMethod(altrepMethodDescriptor);
    }

    @Override
    public void R_set_altinteger_Max_method(Object classDescriptor, Object method) {
        AltIntegerClassDescriptor classDescr = guaranteeInstanceOf(classDescriptor, AltIntegerClassDescriptor.class);
        AltrepMethodDescriptor altrepMethodDescriptor = createAltrepMethodDescriptor(method, AltIntegerClassDescriptor.maxMethodSignature);
        classDescr.registerMaxMethod(altrepMethodDescriptor);
    }

    @Override
    public void R_set_altreal_Elt_method(Object classDescriptor, Object method) {
        AltRealClassDescriptor classDescr = guaranteeInstanceOf(classDescriptor, AltRealClassDescriptor.class);
        AltrepMethodDescriptor altrepMethodDescriptor = createAltrepMethodDescriptor(method, AltRealClassDescriptor.eltMethodSignature);
        classDescr.registerEltMethod(altrepMethodDescriptor);
    }

    @Override
    public void R_set_altreal_Get_region_method(Object classDescriptor, Object method) {
        AltRealClassDescriptor classDescr = guaranteeInstanceOf(classDescriptor, AltRealClassDescriptor.class);
        AltrepMethodDescriptor altrepMethodDescriptor = createAltrepMethodDescriptor(method, AltRealClassDescriptor.getRegionMethodSignature);
        classDescr.registerGetRegionMethod(altrepMethodDescriptor);
    }

    @Override
    public void R_set_altreal_Is_sorted_method(Object classDescriptor, Object method) {
        AltRealClassDescriptor classDescr = guaranteeInstanceOf(classDescriptor, AltRealClassDescriptor.class);
        AltrepMethodDescriptor altrepMethodDescriptor = createAltrepMethodDescriptor(method, AltRealClassDescriptor.isSortedMethodSignature);
        classDescr.registerIsSortedMethod(altrepMethodDescriptor);
    }

    @Override
    public void R_set_altreal_No_NA_method(Object classDescriptor, Object method) {
        AltRealClassDescriptor classDescr = guaranteeInstanceOf(classDescriptor, AltRealClassDescriptor.class);
        AltrepMethodDescriptor altrepMethodDescriptor = createAltrepMethodDescriptor(method, AltRealClassDescriptor.noNAMethodSignature);
        classDescr.registerNoNAMethod(altrepMethodDescriptor);
    }

    @Override
    public void R_set_altreal_Sum_method(Object classDescriptor, Object method) {
        AltRealClassDescriptor classDescr = guaranteeInstanceOf(classDescriptor, AltRealClassDescriptor.class);
        AltrepMethodDescriptor altrepMethodDescriptor = createAltrepMethodDescriptor(method, AltRealClassDescriptor.sumMethodSignature);
        classDescr.registerSumMethod(altrepMethodDescriptor);
    }

    @Override
    public void R_set_altreal_Min_method(Object classDescriptor, Object method) {
        AltRealClassDescriptor classDescr = guaranteeInstanceOf(classDescriptor, AltRealClassDescriptor.class);
        AltrepMethodDescriptor altrepMethodDescriptor = createAltrepMethodDescriptor(method, AltRealClassDescriptor.minMethodSignature);
        classDescr.registerMinMethod(altrepMethodDescriptor);
    }

    @Override
    public void R_set_altreal_Max_method(Object classDescriptor, Object method) {
        AltRealClassDescriptor classDescr = guaranteeInstanceOf(classDescriptor, AltRealClassDescriptor.class);
        AltrepMethodDescriptor altrepMethodDescriptor = createAltrepMethodDescriptor(method, AltRealClassDescriptor.maxMethodSignature);
        classDescr.registerMaxMethod(altrepMethodDescriptor);
    }

    @Override
    public void R_set_altlogical_Elt_method(Object classDescriptor, Object method) {
        AltLogicalClassDescriptor classDescr = guaranteeInstanceOf(classDescriptor, AltLogicalClassDescriptor.class);
        AltrepMethodDescriptor altrepMethodDescriptor = createAltrepMethodDescriptor(method, AltLogicalClassDescriptor.eltMethodSignature);
        classDescr.registerEltMethod(altrepMethodDescriptor);
    }

    @Override
    public void R_set_altlogical_Get_region_method(Object classDescriptor, Object method) {
        AltLogicalClassDescriptor classDescr = guaranteeInstanceOf(classDescriptor, AltLogicalClassDescriptor.class);
        AltrepMethodDescriptor altrepMethodDescriptor = createAltrepMethodDescriptor(method, AltLogicalClassDescriptor.getRegionMethodSignature);
        classDescr.registerGetRegionMethod(altrepMethodDescriptor);
    }

    @Override
    public void R_set_altlogical_Is_sorted_method(Object classDescriptor, Object method) {
        AltLogicalClassDescriptor classDescr = guaranteeInstanceOf(classDescriptor, AltLogicalClassDescriptor.class);
        AltrepMethodDescriptor altrepMethodDescriptor = createAltrepMethodDescriptor(method, AltLogicalClassDescriptor.isSortedMethodSignature);
        classDescr.registerIsSortedMethod(altrepMethodDescriptor);
    }

    @Override
    public void R_set_altlogical_No_NA_method(Object classDescriptor, Object method) {
        AltLogicalClassDescriptor classDescr = guaranteeInstanceOf(classDescriptor, AltLogicalClassDescriptor.class);
        AltrepMethodDescriptor altrepMethodDescriptor = createAltrepMethodDescriptor(method, AltLogicalClassDescriptor.noNAMethodSignature);
        classDescr.registerNoNAMethod(altrepMethodDescriptor);
    }

    @Override
    public void R_set_altlogical_Sum_method(Object classDescriptor, Object method) {
        AltLogicalClassDescriptor classDescr = guaranteeInstanceOf(classDescriptor, AltLogicalClassDescriptor.class);
        AltrepMethodDescriptor altrepMethodDescriptor = createAltrepMethodDescriptor(method, AltLogicalClassDescriptor.sumMethodSignature);
        classDescr.registerSumMethod(altrepMethodDescriptor);
    }

    @Override
    public void R_set_altraw_Elt_method(Object classDescriptor, Object method) {
        AltRawClassDescriptor classDescr = guaranteeInstanceOf(classDescriptor, AltRawClassDescriptor.class);
        AltrepMethodDescriptor altrepMethodDescriptor = createAltrepMethodDescriptor(method, AltRawClassDescriptor.eltMethodSignature);
        classDescr.registerEltMethod(altrepMethodDescriptor);
    }

    @Override
    public void R_set_altraw_Get_region_method(Object classDescriptor, Object method) {
        AltRawClassDescriptor classDescr = guaranteeInstanceOf(classDescriptor, AltRawClassDescriptor.class);
        AltrepMethodDescriptor altrepMethodDescriptor = createAltrepMethodDescriptor(method, AltRawClassDescriptor.getRegionMethodSignature);
        classDescr.registerGetRegionMethod(altrepMethodDescriptor);
    }

    @Override
    public void R_set_altcomplex_Elt_method(Object classDescriptor, Object method) {
        AltComplexClassDescriptor classDescr = guaranteeInstanceOf(classDescriptor, AltComplexClassDescriptor.class);
        AltrepMethodDescriptor altrepMethodDescriptor = createAltrepMethodDescriptor(method, AltComplexClassDescriptor.eltMethodSignature);
        classDescr.registerEltMethod(altrepMethodDescriptor);
    }

    @Override
    public void R_set_altcomplex_Get_region_method(Object classDescriptor, Object method) {
        AltComplexClassDescriptor classDescr = guaranteeInstanceOf(classDescriptor, AltComplexClassDescriptor.class);
        AltrepMethodDescriptor altrepMethodDescriptor = createAltrepMethodDescriptor(method, AltComplexClassDescriptor.getRegionMethodSignature);
        classDescr.registerGetRegionMethod(altrepMethodDescriptor);
    }

    @Override
    public void R_set_altstring_Elt_method(Object classDescriptor, Object method) {
        AltStringClassDescriptor classDescr = guaranteeInstanceOf(classDescriptor, AltStringClassDescriptor.class);
        AltrepMethodDescriptor altrepMethodDescriptor = createAltrepMethodDescriptor(method, AltStringClassDescriptor.eltMethodSignature);
        classDescr.registerEltMethod(altrepMethodDescriptor);
    }

    @Override
    public void R_set_altstring_Set_elt_method(Object classDescriptor, Object method) {
        AltStringClassDescriptor classDescr = guaranteeInstanceOf(classDescriptor, AltStringClassDescriptor.class);
        AltrepMethodDescriptor altrepMethodDescriptor = createAltrepMethodDescriptor(method, AltStringClassDescriptor.setEltMethodSignature);
        classDescr.registerSetEltMethod(altrepMethodDescriptor);
    }

    @Override
    public void R_set_altstring_Is_sorted_method(Object classDescriptor, Object method) {
        AltStringClassDescriptor classDescr = guaranteeInstanceOf(classDescriptor, AltStringClassDescriptor.class);
        AltrepMethodDescriptor altrepMethodDescriptor = createAltrepMethodDescriptor(method, AltStringClassDescriptor.isSortedMethodSignature);
        classDescr.registerIsSortedMethod(altrepMethodDescriptor);
    }

    @Override
    public void R_set_altstring_No_NA_method(Object classDescriptor, Object method) {
        AltStringClassDescriptor classDescr = guaranteeInstanceOf(classDescriptor, AltStringClassDescriptor.class);
        AltrepMethodDescriptor altrepMethodDescriptor = createAltrepMethodDescriptor(method, AltStringClassDescriptor.noNAMethodSignature);
        classDescr.registerNoNAMethod(altrepMethodDescriptor);
    }

    @Override
    public Object R_new_altrep(Object classDescriptor, Object data1, Object data2) {
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
        Object result = p.getTag();
        Object materialized = FFIMaterializeNode.uncachedMaterialize(result);
        if (result != materialized) {
            ShareObjectNode.executeUncached(materialized);
            p.setTag(materialized);
        }
        return materialized;
    }

    @Override
    public Object R_ExternalPtrProtected(Object x) {
        RExternalPtr p = guaranteeInstanceOf(x, RExternalPtr.class);
        Object result = p.getProt();
        Object materialized = FFIMaterializeNode.uncachedMaterialize(result);
        if (result != materialized) {
            ShareObjectNode.executeUncached(materialized);
            p.setProt(materialized);
        }
        return materialized;
    }

    @Override
    public void R_SetExternalPtrAddr(Object x, Object addr) {
        throw implementedAsNode();
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
        return RContext.getInstance().getRFFI().getOrCreateCode(promise, p -> {
            RSyntaxNode expr = RASTUtils.unwrap(p.getRep()).asRSyntaxNode();
            return RASTUtils.createLanguageElement(expr);
        });
    }

    @Override
    @TruffleBoundary
    public Object R_new_custom_connection(String description, String mode, String className, Object connAddrObj) {
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
        NativeMemory.copyMemory(buf, bufAddress, ElementType.BYTE, Math.min(result, size));
        return result;
    }

    @Override
    @TruffleBoundary
    public int R_WriteConnection(int fd, long bufAddress, int size) {
        // Workaround using Unsafe until GR-5927 is fixed
        byte[] buf = new byte[size];
        NativeMemory.copyMemory(bufAddress, buf, ElementType.BYTE, size);
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

    private static RObjectDataPtr wrapString(String s) {
        CharSXPWrapper v = CharSXPWrapper.create(s);
        return RObjectDataPtr.getUncached(v);
    }

    @Override
    @TruffleBoundary
    public Object getSummaryDescription(Object x) {
        // FastR internal up-call: the caller makes no assumption about the ownership
        BaseRConnection conn = guaranteeInstanceOf(x, BaseRConnection.class);
        return wrapString(conn.getSummaryDescription());
    }

    @Override
    @TruffleBoundary
    public Object getConnectionClassString(Object x) {
        // FastR internal up-call: the caller makes no assumption about the ownership
        BaseRConnection conn = guaranteeInstanceOf(x, BaseRConnection.class);
        return wrapString(conn.getConnectionClassName());
    }

    @Override
    @TruffleBoundary
    public Object getOpenModeString(Object x) {
        // FastR internal up-call: the caller makes no assumption about the ownership
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

    // basic support for weak reference API - they are not actually weak and don't call finalizers

    @Override
    @TruffleBoundary
    public Object R_MakeWeakRef(Object key, Object val, Object fin, long onexit) {
        return new RWeakRef(key, val, fin, onexit != 0);
    }

    @Override
    @TruffleBoundary
    public Object R_MakeWeakRefC(Object key, Object val, Object finFunction, int onexit) {
        return new RWeakRef(key, val, finFunction, onexit != 0);
    }

    @Override
    @TruffleBoundary
    public Object R_WeakRefKey(Object w) {
        return guaranteeInstanceOf(w, RWeakRef.class).getKey();
    }

    @Override
    @TruffleBoundary
    public Object R_WeakRefValue(Object w) {
        return guaranteeInstanceOf(w, RWeakRef.class).getValue();
    }

    @Override
    public void R_PreserveObject(Object obj) {
        throw implementedAsNode();
    }

    @Override
    public void R_ReleaseObject(Object obj) {
        throw implementedAsNode();
    }

    @Override
    public Object Rf_protect(Object x) {
        throw implementedAsNode();
    }

    @Override
    public void Rf_unprotect(int x) {
        throw implementedAsNode();
    }

    @Override
    public int R_ProtectWithIndex(Object x) {
        throw implementedAsNode();
    }

    @Override
    public void R_Reprotect(Object x, int y) {
        throw implementedAsNode();
    }

    @Override
    public void Rf_unprotect_ptr(Object x) {
        throw implementedAsNode();
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
    public int Rf_str2type(String name) {
        throw implementedAsNode();
    }

    @Override
    @TruffleBoundary
    public int registerRoutines(Object dllInfoObj, int nstOrd, int num, Object routines) {
        DLLInfo dllInfo = guaranteeInstanceOf(dllInfoObj, DLLInfo.class);
        DotSymbol[] array = new DotSymbol[num];
        dllInfo.setNativeSymbols(nstOrd, array);
        for (int i = 0; i < num; i++) {
            // Calls C function to extract the DotSymbol data from the native array, which contains
            // C structures, the C function up-calls to Java function setDotSymbolValues, which
            // actually creates the DotSymbol Java object and adds it to the symbol to the DllInfo
            setSymbol(dllInfo, nstOrd, routines, i);
        }
        return 0;
    }

    @Override
    @TruffleBoundary
    public int registerCCallable(String pkgName, String functionName, Object address) {
        DLLInfo.registerCEntry(pkgName, new CEntry(functionName, new SymbolHandle(address)));
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
    public void setDotSymbolValues(Object dllInfoObj, int nstOrd, int index, String name, Object fun, int numArgs) {
        DLLInfo dllInfo = guaranteeInstanceOf(dllInfoObj, DLLInfo.class);
        dllInfo.setNativeSymbol(nstOrd, index, new DotSymbol(name, new SymbolHandle(fun), numArgs));
    }

    @Override
    public Object getEmbeddingDLLInfo() {
        return DLL.getEmbeddingDLLInfo();
    }

    protected abstract void setSymbol(DLLInfo dllInfo, int nstOrd, Object routines, int index);

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
    public void Rf_pnorm_both(double arg0, Object arg1, Object arg2, int arg3, int arg4) {
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
    public double Rf_log1pmx(double a) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_log1pexp(double a) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_lgamma1p(double a) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_logspace_add(double a, double b) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_logspace_sub(double a, double b) {
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
    public double Rf_gammafn(double a) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_lgammafn(double a) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_lgammafn_sign(double a, Object b) {
        throw implementedAsNode();
    }

    @Override
    public void Rf_dpsifn(double a, int b, int c, int d, Object e, Object f, Object g) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_psigamma(double a, double b) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_digamma(double a) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_trigamma(double a) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_tetragamma(double a) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_pentagamma(double a) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_beta(double a, double b) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_lbeta(double a, double b) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_choose(double a, double b) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_lchoose(double a, double b) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_bessel_i(double a, double b, double c) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_bessel_j(double a, double b) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_bessel_k(double a, double b, double c) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_bessel_y(double a, double b) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_bessel_i_ex(double a, double b, double c, Object d) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_bessel_j_ex(double a, double b, Object c) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_bessel_k_ex(double a, double b, double c, Object d) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_bessel_y_ex(double a, double b, Object c) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_sign(double a) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_fprec(double a, double b) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_ftrunc(double a) {
        return RMath.trunc(a);
    }

    @Override
    public double Rf_cospi(double a) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_sinpi(double a) {
        throw implementedAsNode();
    }

    @Override
    public double Rf_tanpi(double a) {
        throw implementedAsNode();
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

    @Override
    public Object match5(Object itables, Object ix, int nmatch, Object incomparables, Object env) {
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

    @Override
    public Object FASTR_DATAPTR(Object x) {
        throw implementedAsNode();
    }

    @Override
    public Object INTEGER(Object x) {
        throw implementedAsNode();
    }

    @Override
    public int INTEGER_ELT(Object x, long index) {
        throw implementedAsNode();
    }

    @Override
    public Object LOGICAL(Object x) {
        throw implementedAsNode();
    }

    @Override
    public int LOGICAL_ELT(Object x, long index) {
        throw implementedAsNode();
    }

    @Override
    public Object REAL(Object x) {
        throw implementedAsNode();
    }

    @Override
    public double REAL_ELT(Object x, long index) {
        throw implementedAsNode();
    }

    @Override
    public Object DATAPTR_OR_NULL(Object x) {
        throw implementedAsNode();
    }

    @Override
    public Object RAW(Object x) {
        throw implementedAsNode();
    }

    @Override
    public int RAW_ELT(Object x, long index) {
        throw implementedAsNode();
    }

    @Override
    public Object COMPLEX(Object x) {
        throw implementedAsNode();
    }

    @Override
    public Object COMPLEX_ELT(Object x, long index) {
        throw implementedAsNode();
    }

    @Override
    public long INTEGER_GET_REGION(Object x, long fromIdx, long size, Object buffer) {
        throw implementedAsNode();
    }

    @Override
    public int INTEGER_IS_SORTED(Object x) {
        throw implementedAsNode();
    }

    @Override
    public long REAL_GET_REGION(Object x, long fromIdx, long size, Object buffer) {
        return 0;
    }

    @Override
    public long LOGICAL_GET_REGION(Object x, long fromIdx, long size, Object buffer) {
        return 0;
    }

    @Override
    public long COMPLEX_GET_REGION(Object x, long fromIdx, long size, Object buffer) {
        return 0;
    }

    @Override
    public long RAW_GET_REGION(Object x, long fromIdx, long size, Object buffer) {
        return 0;
    }

    @Override
    public int INTEGER_NO_NA(Object x) {
        throw implementedAsNode();
    }

    @Override
    public int REAL_IS_SORTED(Object x) {
        throw implementedAsNode();
    }

    @Override
    public int REAL_NO_NA(Object x) {
        return 0;
    }

    @Override
    public int LOGICAL_IS_SORTED(Object x) {
        throw implementedAsNode();
    }

    @Override
    public int LOGICAL_NO_NA(Object x) {
        return 0;
    }

    @Override
    public int STRING_IS_SORTED(Object x) {
        throw implementedAsNode();
    }

    @Override
    public int STRING_NO_NA(Object x) {
        return 0;
    }

    @Override
    public Object R_CHAR(Object x) {
        throw implementedAsNode();
    }

    @Override
    public void Rf_PrintValue(Object value) {
        throw implementedAsNode();
    }

    @Override
    public int R_nchar(Object string, int type, int allowNA, int keepNA, String msgName) {
        throw implementedAsNode();
    }

    @Override
    public Object R_forceAndCall(Object e, Object f, int n, Object args) {
        throw implementedAsNode();
    }

    @Override
    public void R_MakeActiveBinding(Object symArg, Object funArg, Object envArg) {
        throw implementedAsNode();
    }

    private static JavaGDContext getJavaGDContext(RContext ctx) {
        assert ctx.gridContext != null;
        return (JavaGDContext) ctx.gridContext;
    }

    @Override
    @TruffleBoundary
    public void gdOpen(int gdId, String deviceName, double w, double h, RContext ctx) {
        JavaGDContext.getContext(ctx).newGD(gdId, deviceName).gdOpen(w, h);
    }

    @Override
    @TruffleBoundary
    public void gdClose(int gdId, RContext ctx) {
        getJavaGDContext(ctx).removeGD(gdId).gdClose();
    }

    @Override
    @TruffleBoundary
    public void gdActivate(int gdId, RContext ctx) {
        getJavaGDContext(ctx).getGD(gdId).gdActivate();
    }

    @Override
    @TruffleBoundary
    public void gdcSetColor(int gdId, int cc, RContext ctx) {
        getJavaGDContext(ctx).getGD(gdId).gdcSetColor(cc);
    }

    @Override
    public void gdcSetFill(int gdId, int cc, RContext ctx) {
        getJavaGDContext(ctx).getGD(gdId).gdcSetFill(cc);
    }

    @Override
    public void gdcSetLine(int gdId, double lwd, int lty, RContext ctx) {
        getJavaGDContext(ctx).getGD(gdId).gdcSetLine(lwd, lty);
    }

    @Override
    public void gdcSetFont(int gdId, double cex, double ps, double lineheight, int fontface, String fontfamily, RContext ctx) {
        getJavaGDContext(ctx).getGD(gdId).gdcSetFont(cex, ps, lineheight, fontface, fontfamily);
    }

    @Override
    @TruffleBoundary
    public void gdNewPage(int gdId, int devId, int pageNumber, RContext ctx) {
        getJavaGDContext(ctx).getGD(gdId).gdNewPage(devId, pageNumber);
    }

    @Override
    public void gdCircle(int gdId, double x, double y, double r, RContext ctx) {
        getJavaGDContext(ctx).getGD(gdId).gdCircle(x, y, r);
    }

    @Override
    public void gdClip(int gdId, double x0, double x1, double y0, double y1, RContext ctx) {
        getJavaGDContext(ctx).getGD(gdId).gdClip(x0, x1, y0, y1);
    }

    @Override
    @TruffleBoundary
    public void gdDeactivate(int gdId, RContext ctx) {
        getJavaGDContext(ctx).getGD(gdId).gdDeactivate();
    }

    @Override
    public void gdHold(int gdId, RContext ctx) {
        getJavaGDContext(ctx).getGD(gdId).gdHold();
    }

    @Override
    @TruffleBoundary
    public void gdFlush(int gdId, int flush, RContext ctx) {
        getJavaGDContext(ctx).getGD(gdId).gdFlush(flush != 0);
    }

    @Override
    @TruffleBoundary
    public Object gdLocator(int gdId, RContext ctx) {
        return getJavaGDContext(ctx).getGD(gdId).gdLocator();
    }

    @Override
    public void gdLine(int gdId, double x1, double y1, double x2, double y2, RContext ctx) {
        getJavaGDContext(ctx).getGD(gdId).gdLine(x1, y1, x2, y2);
    }

    @Override
    @TruffleBoundary
    public void gdMode(int gdId, int mode, RContext ctx) {
        getJavaGDContext(ctx).getGD(gdId).gdMode(mode);
    }

    @Override
    public void gdPath(int gdId, int npoly, Object nper, int n, Object x, Object y, int winding, RContext ctx) {
        getJavaGDContext(ctx).getGD(gdId).gdPath(npoly, (int[]) nper, (double[]) x, (double[]) y, winding != 0);
    }

    @Override
    public void gdPolygon(int gdId, int n, Object x, Object y, RContext ctx) {
        getJavaGDContext(ctx).getGD(gdId).gdPolygon(n, (double[]) x, (double[]) y);
    }

    @Override
    public void gdPolyline(int gdId, int n, Object x, Object y, RContext ctx) {
        getJavaGDContext(ctx).getGD(gdId).gdPolyline(n, (double[]) x, (double[]) y);
    }

    @Override
    public void gdRect(int gdId, double x0, double y0, double x1, double y1, RContext ctx) {
        getJavaGDContext(ctx).getGD(gdId).gdRect(x0, y0, x1, y1);
    }

    @Override
    public Object gdSize(int gdId, RContext ctx) {
        return getJavaGDContext(ctx).getGD(gdId).gdSize();
    }

    @Override
    @TruffleBoundary
    public double getStrWidth(int gdId, String str, RContext ctx) {
        return getJavaGDContext(ctx).getGD(gdId).gdStrWidth(str);
    }

    @Override
    public void gdText(int gdId, double x, double y, String str, double rot, double hadj, RContext ctx) {
        getJavaGDContext(ctx).getGD(gdId).gdText(x, y, str, rot, hadj);
    }

    @Override
    @TruffleBoundary
    public void gdRaster(int gdId, int img_w, int img_h, Object img, double x, double y, double w, double h, double rot, int interpolate, RContext ctx) {
        getJavaGDContext(ctx).getGD(gdId).gdRaster((byte[]) img, img_w, img_h, x, y, w, h, rot, interpolate != 0);
    }

    @Override
    @TruffleBoundary
    public Object gdMetricInfo(int gdId, int ch, RContext ctx) {
        return getJavaGDContext(ctx).getGD(gdId).gdMetricInfo(ch);
    }

    @Override
    public Object DispatchPRIMFUN(Object call, Object op, Object args, Object rho) {
        throw implementedAsNode();
    }

}
