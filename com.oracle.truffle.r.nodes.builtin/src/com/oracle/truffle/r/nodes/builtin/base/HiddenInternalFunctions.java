/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import java.io.*;
import java.nio.*;
import java.util.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.builtin.base.EvalFunctions.Eval;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.env.*;
import com.oracle.truffle.r.runtime.env.REnvironment.*;
import com.oracle.truffle.r.runtime.ffi.*;

/**
 * Private, undocumented, {@code .Internal} and {@code .Primitive} functions transcribed from GnuR.
 */
public class HiddenInternalFunctions {

    /**
     * Transcribed from GnuR {@code do_makeLazy} in src/main/builtin.c.
     */
    @RBuiltin(name = "makeLazy", kind = RBuiltinKind.INTERNAL, parameterNames = {"names", "values", "expr", "eenv", "aenv"})
    public abstract static class MakeLazy extends RBuiltinNode {
        /**
         * When loading the {@code base} package we may encounter a locked binding that holds an
         * {@link RBuiltin} that is a {@link RBuiltinKind#SUBSTITUTE}. This is not an error, but is
         * used as an override mechanism.
         */
        public static boolean loadingBase;

        @Child private Eval eval;

        private void initEval() {
            if (eval == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                eval = insert(EvalFunctionsFactory.EvalNodeGen.create(new RNode[3], null, null));
            }
        }

        /**
         * {@code expr} has the value {@code lazyLoadDBfetch(key, datafile, compressed, envhook)},
         * see {@code base/lazyLoad.R}. All the arguments except {@code key} are defined in the
         * {@code eenv} environment. {@code key} is replaced in (a copy of) {@code expr} by the
         * constant 2-element vector in {@code values}, corresponding to the element of
         * {@code names}. The value for the name stored as a {@link RPromise} to evaluate the
         * modified call in the {@code eenv} environment.
         */
        @Specialization
        @TruffleBoundary
        protected RNull doMakeLazy(RAbstractStringVector names, RList values, RLanguage expr, REnvironment eenv, REnvironment aenv) {
            controlVisibility();
            initEval();
            for (int i = 0; i < names.getLength(); i++) {
                String name = names.getDataAt(i);
                RIntVector intVec = (RIntVector) values.getDataAt(i);
                // GnuR does an eval but we short cut since intVec evaluates to itself.
                // What happens next a pretty gross - we replace the "key" argument variable read
                // in expr with a constant that is the value of intVec
                RCallNode callNode = (RCallNode) RASTUtils.unwrap(expr.getRep());
                ConstantNode vecNode = ConstantNode.create(intVec);
                RCallNode expr0 = RCallNode.createCloneReplacingArgs(callNode, vecNode);
                try {
                    aenv.put(name, RDataFactory.createPromise(expr0, eenv));
                } catch (PutException ex) {
                    //
                    if (!loadingBase) {
                        throw RError.error(getEncapsulatingSourceSection(), ex);
                    }
                }
            }
            return RNull.instance;
        }
    }

    /**
     * Transcribed from {@code do_importIntoEnv} in src/main/envir.c.
     *
     * This function copies values of variables from one environment to another environment,
     * possibly with different names. Promises are not forced and active bindings are preserved.
     */
    @RBuiltin(name = "importIntoEnv", kind = INTERNAL, parameterNames = {"impEnv", "impNames", "expEnv", "expNames"})
    public abstract static class ImportIntoEnv extends RBuiltinNode {
        @Specialization
        @TruffleBoundary
        protected RNull importIntoEnv(REnvironment impEnv, RAbstractStringVector impNames, REnvironment expEnv, RAbstractStringVector expNames) {
            controlVisibility();
            int length = impNames.getLength();
            if (length != expNames.getLength()) {
                throw RError.error(getEncapsulatingSourceSection(), Message.IMP_EXP_NAMES_MATCH);
            }
            for (int i = 0; i < length; i++) {
                String impsym = impNames.getDataAt(i);
                String expsym = expNames.getDataAt(i);
                Object binding = null;
                // TODO name translation, and a bunch of other special cases
                for (REnvironment env = expEnv; env != REnvironment.emptyEnv() && binding == null; env = env.getParent()) {
                    if (env == REnvironment.baseNamespaceEnv()) {
                        assert false;
                    } else {
                        binding = env.get(expsym);
                    }
                }
                try {
                    impEnv.put(impsym, binding);
                } catch (PutException ex) {
                    throw RError.error(getEncapsulatingSourceSection(), ex);
                }

            }
            return RNull.instance;
        }
    }

    /**
     * Transcribed from {@code lazyLoaadDBFetch} in src/serialize.c.
     */
    @RBuiltin(name = "lazyLoadDBfetch", kind = PRIMITIVE, parameterNames = {"key", "dtafile", "compressed", "envhook"})
    public abstract static class LazyLoadDBFetch extends RBuiltinNode {

        @Child private CallInlineCacheNode callCache = CallInlineCacheNodeGen.create();
        @Child private CastIntegerNode castIntNode;

        private void initCast() {
            if (castIntNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                castIntNode = insert(CastIntegerNodeGen.create(null, false, false, false));
            }
        }

        private static Map<String, byte[]> dbCache = new HashMap<>();

        /**
         * No error checking here as this called by trusted library code.
         */
        @Specialization
        public Object lazyLoadDBFetch(VirtualFrame frame, RIntVector key, RStringVector datafile, RIntVector compressed, RFunction envhook) {
            return lazyLoadDBFetchInternal(frame.materialize(), key, datafile, compressed.getDataAt(0), envhook);
        }

        @Specialization
        public Object lazyLoadDBFetch(VirtualFrame frame, RIntVector key, RStringVector datafile, RDoubleVector compressed, RFunction envhook) {
            return lazyLoadDBFetchInternal(frame.materialize(), key, datafile, (int) compressed.getDataAt(0), envhook);
        }

        private static final ArgumentsSignature SIGNATURE = ArgumentsSignature.get("n");

        @TruffleBoundary
        public Object lazyLoadDBFetchInternal(MaterializedFrame frame, RIntVector key, RStringVector datafile, int compression, RFunction envhook) {
            String dbPath = datafile.getDataAt(0);
            File dbPathFile = new File(dbPath);
            byte[] dbData = dbCache.get(dbPath);
            if (dbData == null) {
                assert dbPathFile.exists();
                dbData = new byte[(int) dbPathFile.length()];
                try (BufferedInputStream bs = new BufferedInputStream(new FileInputStream(dbPathFile))) {
                    bs.read(dbData);
                } catch (IOException ex) {
                    // unexpected
                    throw RError.error(Message.GENERIC, ex.getMessage());
                }
                dbCache.put(dbPath, dbData);
            }
            String packageName = dbPathFile.getName();
            int dotIndex;
            if ((dotIndex = packageName.lastIndexOf('.')) > 0) {
                packageName = packageName.substring(0, dotIndex);
            }
            int offset = key.getDataAt(0);
            int length = key.getDataAt(1);
            ByteBuffer dataLengthBuf = ByteBuffer.allocate(4);
            dataLengthBuf.put(dbData, offset, 4);
            dataLengthBuf.position(0);
            byte[] data = new byte[length - 4];
            System.arraycopy(dbData, offset + 4, data, 0, data.length);
            if (compression == 1) {
                int outlen = dataLengthBuf.getInt();
                byte[] udata = new byte[outlen];
                long[] destlen = new long[1];
                destlen[0] = udata.length;
                int rc = RFFIFactory.getRFFI().getBaseRFFI().uncompress(udata, destlen, data);
                if (rc != 0) {
                    throw RError.error(Message.GENERIC, "zlib uncompress error");
                }
                try {
                    RSerialize.CallHook callHook = new RSerialize.CallHook() {

                        public Object eval(Object arg) {
                            Object[] callArgs = RArguments.create(envhook, callCache.getSourceSection(), null, RArguments.getDepth(frame) + 1, new Object[]{arg}, SIGNATURE);
                            // TODO this cast is problematic
                            return callCache.execute(new SubstituteVirtualFrame(frame), envhook.getTarget(), callArgs);
                        }

                    };
                    Object result = RSerialize.unserialize(udata, callHook, RArguments.getDepth(frame), packageName);
                    return result;
                } catch (IOException ex) {
                    // unexpected
                    throw RError.error(Message.GENERIC, ex.getMessage());
                }
            } else {
                throw RError.error(Message.GENERIC, "unsupported compression");
            }
        }

        @Specialization
        protected Object lazyLoadDBFetch(VirtualFrame frame, RIntVector key, RStringVector datafile, RLogicalVector compressed, RFunction envhook) {
            initCast();
            return lazyLoadDBFetch(frame, key, datafile, castIntNode.doLogicalVector(compressed), envhook);
        }
    }

    @RBuiltin(name = "getRegisteredRoutines", kind = INTERNAL, parameterNames = "info")
    public abstract static class GetRegisteredRoutines extends RBuiltinNode {
        private static final RStringVector NAMES = RDataFactory.createStringVector(new String[]{".C", ".Call", ".Fortran", ".External"}, RDataFactory.COMPLETE_VECTOR);
        private static final RStringVector NATIVE_ROUTINE_LIST = RDataFactory.createStringVectorFromScalar("NativeRoutineList");

        @Specialization
        protected RList getRegisteredRoutines(@SuppressWarnings("unused") RNull info) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.NULL_DLLINFO);
        }

        @Specialization(guards = "isDLLInfo(externalPtr)")
        @TruffleBoundary
        protected RList getRegisteredRoutines(RExternalPtr externalPtr) {
            Object[] data = new Object[NAMES.getLength()];
            DLL.DLLInfo dllInfo = DLL.getDLLInfoForId((int) externalPtr.value);
            if (dllInfo == null) {
                throw RInternalError.shouldNotReachHere();
            }
            for (DLL.NativeSymbolType nst : DLL.NativeSymbolType.values()) {
                DLL.DotSymbol[] symbols = dllInfo.getNativeSymbols(nst);
                if (symbols == null) {
                    symbols = new DLL.DotSymbol[0];
                }
                Object[] symbolData = new Object[symbols.length];
                for (int i = 0; i < symbols.length; i++) {
                    DLL.DotSymbol symbol = symbols[i];
                    DLL.RegisteredNativeType rnt = new DLL.RegisteredNativeType(nst, symbol, dllInfo);
                    DLL.SymbolInfo symbolInfo = new DLL.SymbolInfo(dllInfo, symbol.name, symbol.fun);
                    symbolData[i] = symbolInfo.createRSymbolObject(rnt, true);
                }
                RList symbolDataList = RDataFactory.createList(symbolData);
                symbolDataList.setClassAttr(NATIVE_ROUTINE_LIST, false);
                data[nst.ordinal()] = symbolDataList;
            }
            return RDataFactory.createList(data, NAMES);
        }

        @Fallback
        protected RList getRegisteredRoutines(@SuppressWarnings("unused") Object info) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.REQUIRES_DLLINFO);
        }

        public static boolean isDLLInfo(RExternalPtr externalPtr) {
            return DLL.isDLLInfo(externalPtr);
        }

    }

}
