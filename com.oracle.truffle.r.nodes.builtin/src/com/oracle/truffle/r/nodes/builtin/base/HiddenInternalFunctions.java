/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import java.io.*;
import java.nio.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.access.variables.ReadVariableNode;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.builtin.base.EvalFunctions.Eval;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.nodes.runtime.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.context.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.env.*;
import com.oracle.truffle.r.runtime.env.REnvironment.PutException;
import com.oracle.truffle.r.runtime.ffi.*;
import com.oracle.truffle.r.runtime.nodes.*;

/**
 * Private, undocumented, {@code .Internal} and {@code .Primitive} functions transcribed from GnuR,
 * but also those that need to be defined as builtins in FastR.
 */
public class HiddenInternalFunctions {

    /**
     * Transcribed from GnuR {@code do_makeLazy} in src/main/builtin.c.
     */
    @RBuiltin(name = "makeLazy", kind = RBuiltinKind.INTERNAL, parameterNames = {"names", "values", "expr", "eenv", "aenv"})
    public abstract static class MakeLazy extends RBuiltinNode {
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
                    // We want this call to have a SourceSection
                    RASTDeparse.ensureSourceSection(expr0);
                    aenv.put(name, RDataFactory.createPromise(expr0, eenv));
                } catch (PutException ex) {
                    /*
                     * When loading the {@code base} package we may encounter a locked binding that
                     * holds an {@link RBuiltin} that is a {@link RBuiltinKind#SUBSTITUTE}. This is
                     * not an error, but is used as an override mechanism.
                     */
                    if (!RContext.getInstance().getLoadingBase()) {
                        throw RError.error(this, ex);
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
                throw RError.error(this, Message.IMP_EXP_NAMES_MATCH);
            }
            for (int i = 0; i < length; i++) {
                String impsym = impNames.getDataAt(i);
                String expsym = expNames.getDataAt(i);
                Object binding = null;
                // TODO name translation, and a bunch of other special cases
                for (REnvironment env = expEnv; env != REnvironment.emptyEnv(); env = env.getParent()) {
                    if (env == REnvironment.baseNamespaceEnv()) {
                        assert false;
                    } else {
                        binding = env.get(expsym);
                        if (binding != null) {
                            break;
                        }
                    }
                }
                try {
                    impEnv.put(impsym, binding);
                } catch (PutException ex) {
                    throw RError.error(this, ex);
                }

            }
            return RNull.instance;
        }
    }

    /**
     * Transcribed from {@code lazyLoaadDBFetch} in src/serialize.c.
     */
    @RBuiltin(name = "lazyLoadDBfetch", kind = PRIMITIVE, parameterNames = {"key", "datafile", "compressed", "envhook"})
    public abstract static class LazyLoadDBFetch extends RBuiltinNode {

        @Child private CallInlineCacheNode callCache = CallInlineCacheNodeGen.create();
        @Child private CastIntegerNode castIntNode;

        private final RCaller caller = RDataFactory.createCaller(this);

        private void initCast() {
            if (castIntNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                castIntNode = insert(CastIntegerNodeGen.create(false, false, false));
            }
        }

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

        @Specialization
        protected Object lazyLoadDBFetch(VirtualFrame frame, RIntVector key, RStringVector datafile, RLogicalVector compressed, RFunction envhook) {
            initCast();
            return lazyLoadDBFetch(frame, key, datafile, castIntNode.doLogicalVector(compressed), envhook);
        }

        private static final ArgumentsSignature SIGNATURE = ArgumentsSignature.get("n");

        @TruffleBoundary
        public Object lazyLoadDBFetchInternal(MaterializedFrame frame, RIntVector key, RStringVector datafile, int compression, RFunction envhook) {
            if (CompilerDirectives.inInterpreter()) {
                LoopNode.reportLoopCount(this, -5);
            }
            String dbPath = datafile.getDataAt(0);
            String packageName = new File(dbPath).getName();
            byte[] dbData = RContext.getInstance().stateLazyDBCache.getData(dbPath);
            int dotIndex;
            if ((dotIndex = packageName.lastIndexOf('.')) > 0) {
                packageName = packageName.substring(0, dotIndex);
            }
            int offset = key.getDataAt(0);
            int length = key.getDataAt(1);
            int outlen = getOutlen(dbData, offset); // length of uncompressed data
            byte[] udata = null;
            boolean rc = true;
            /*
             * compression may have value 0, 1, 2 or 3. Value 1 is gzip and the data starts at
             * "offset + 4". Values 2 and 3 have a "type" field at "offset +
             * 4" and the data starts at "offset + 5". The type field is 'Z' for lzma, '2' for bzip,
             * '1' for zip and '0' for no compression. From GnuR code, the only difference between
             * compression=2 and compression=3 is that type='Z' is only possible for the latter.
             */
            if (compression == 0) {
                udata = new byte[length];
                System.arraycopy(dbData, offset, udata, 0, length);
            } else {
                udata = new byte[outlen];
                if (compression == 2 || compression == 3) {
                    RCompression.Type type = RCompression.Type.fromTypeChar(dbData[4]);
                    if (type == null) {
                        RError.warning(this, RError.Message.GENERIC, "unknown compression type");
                        return RNull.instance;
                    }
                    byte[] data = new byte[length - 5];
                    System.arraycopy(dbData, offset + 5, data, 0, data.length);
                    rc = RCompression.uncompress(type, udata, data);
                } else {
                    // GnuR treats any other value as 1
                    byte[] data = new byte[length - 4];
                    System.arraycopy(dbData, offset + 4, data, 0, data.length);
                    rc = RCompression.uncompress(RCompression.Type.GZIP, udata, data);
                }
            }
            if (!rc) {
                throw RError.error(this, RError.Message.LAZY_LOAD_DB_CORRUPT, dbPath);
            }
            try {
                RSerialize.CallHook callHook = new RSerialize.CallHook() {
                    public Object eval(Object arg) {
                        Object[] callArgs = RArguments.create(envhook, caller, null, RArguments.getDepth(frame) + 1, new Object[]{arg}, SIGNATURE, null);
                        return callCache.execute(new SubstituteVirtualFrame(frame), envhook.getTarget(), callArgs);
                    }
                };
                String functionName = ReadVariableNode.getSlowPathEvaluationName();
                Object result = RSerialize.unserialize(udata, callHook, packageName, functionName);
                return result;
            } catch (IOException ex) {
                // unexpected
                throw RInternalError.shouldNotReachHere(ex);
            }
        }

        private static int getOutlen(byte[] dbData, int offset) {
            ByteBuffer dataLengthBuf = ByteBuffer.allocate(4);
            dataLengthBuf.put(dbData, offset, 4);
            dataLengthBuf.position(0);
            return dataLengthBuf.getInt();
        }

    }

    @RBuiltin(name = "getRegisteredRoutines", kind = INTERNAL, parameterNames = "info")
    public abstract static class GetRegisteredRoutines extends RBuiltinNode {
        private static final RStringVector NAMES = RDataFactory.createStringVector(new String[]{".C", ".Call", ".Fortran", ".External"}, RDataFactory.COMPLETE_VECTOR);
        private static final RStringVector NATIVE_ROUTINE_LIST = RDataFactory.createStringVectorFromScalar("NativeRoutineList");

        @Specialization
        protected RList getRegisteredRoutines(@SuppressWarnings("unused") RNull info) {
            throw RError.error(this, RError.Message.NULL_DLLINFO);
        }

        @Specialization(guards = "isDLLInfo(externalPtr)")
        @TruffleBoundary
        protected RList getRegisteredRoutines(RExternalPtr externalPtr) {
            Object[] data = new Object[NAMES.getLength()];
            DLL.DLLInfo dllInfo = DLL.getDLLInfoForId((int) externalPtr.getAddr());
            RInternalError.guarantee(dllInfo != null);
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
            throw RError.error(this, RError.Message.REQUIRES_DLLINFO);
        }

        public static boolean isDLLInfo(RExternalPtr externalPtr) {
            return DLL.isDLLInfo(externalPtr);
        }

    }

    @RBuiltin(name = "getVarsFromFrame", kind = INTERNAL, parameterNames = {"vars", "e", "force"})
    public abstract static class GetVarsFromFrame extends RBuiltinNode {
        @Child private PromiseHelperNode promiseHelper;

        @Specialization
        protected RList getVarsFromFrame(VirtualFrame frame, RAbstractStringVector varsVec, REnvironment env, byte forceArg) {
            boolean force = RRuntime.fromLogical(forceArg);
            Object[] data = new Object[varsVec.getLength()];
            for (int i = 0; i < data.length; i++) {
                String var = varsVec.getDataAt(i);
                Object value = env.get(var);
                if (value == null) {
                    throw RError.error(this, RError.Message.UNKNOWN_OBJECT, var);
                }
                if (force && value instanceof RPromise) {
                    if (promiseHelper == null) {
                        CompilerDirectives.transferToInterpreterAndInvalidate();
                        promiseHelper = insert(new PromiseHelperNode());
                    }
                    value = promiseHelper.evaluate(frame, (RPromise) value);
                }
                data[i] = value;
            }
            return RDataFactory.createList(data, (RStringVector) varsVec);
        }

        @SuppressWarnings("unused")
        @Fallback
        protected RList getVarsFromFrame(Object varsVec, Object env, Object forceArg) {
            throw RError.error(this, RError.Message.INVALID_OR_UNIMPLEMENTED_ARGUMENTS);
        }
    }

    @RBuiltin(name = "lazyLoadDBinsertValue", kind = INTERNAL, parameterNames = {"value", "file", "ascii", "compsxp", "hook"})
    public abstract static class LazyLoadDBinsertValue extends RBuiltinNode {

        private static final ArgumentsSignature SIGNATURE = ArgumentsSignature.get("e");
        @Child private CallInlineCacheNode callCache = CallInlineCacheNodeGen.create();

        private final RCaller caller = RDataFactory.createCaller(this);

        @Override
        protected void createCasts(CastBuilder casts) {
            casts.toInteger(2).toInteger(3);
        }

        @Specialization
        protected RIntVector lazyLoadDBinsertValue(VirtualFrame frame, Object value, RAbstractStringVector file, int asciiL, int compression, RFunction hook) {
            return lazyLoadDBinsertValueInternal(frame.materialize(), value, file, asciiL, compression, hook);
        }

        @TruffleBoundary
        private RIntVector lazyLoadDBinsertValueInternal(MaterializedFrame frame, Object value, RAbstractStringVector file, int type, int compression, RFunction hook) {
            if (!(compression == 1 || compression == 3)) {
                throw RError.error(this, Message.GENERIC, "unsupported compression");
            }

            RSerialize.CallHook callHook = new RSerialize.CallHook() {
                public Object eval(Object arg) {
                    Object[] callArgs = RArguments.create(hook, caller, null, RArguments.getDepth(frame) + 1, new Object[]{arg}, SIGNATURE, null);
                    return callCache.execute(new SubstituteVirtualFrame(frame), hook.getTarget(), callArgs);
                }
            };

            try {
                byte[] data = RSerialize.serialize(value, type, RSerialize.DEFAULT_VERSION, callHook);
                // See comment in LazyLoadDBFetch for format
                int outLen;
                int offset;
                RCompression.Type ctype;
                byte[] cdata;
                if (compression == 1) {
                    ctype = RCompression.Type.GZIP;
                    offset = 4;
                    outLen = (int) (1.001 * data.length) + 20;
                    cdata = new byte[outLen];
                    boolean rc = RCompression.compress(ctype, data, cdata);
                    if (!rc) {
                        throw RError.error(this, Message.GENERIC, "zlib compress error");
                    }
                } else if (compression == 3) {
                    ctype = RCompression.Type.LZMA;
                    offset = 5;
                    outLen = data.length;
                    cdata = new byte[outLen];
                    boolean rc = RCompression.compress(ctype, data, cdata);
                    if (!rc) {
                        throw RError.error(this, Message.GENERIC, "lzma compress error");
                    }
                } else {
                    throw RInternalError.shouldNotReachHere();
                }
                int[] intData = new int[2];
                intData[1] = outLen + offset; // include length + type (compression == 3)
                intData[0] = appendFile(file.getDataAt(0), cdata, data.length, ctype);
                return RDataFactory.createIntVector(intData, RDataFactory.COMPLETE_VECTOR);
            } catch (Throwable ex) {
                // Exceptions have been observed that were masked and very hard to find
                ex.printStackTrace();
                throw RInternalError.shouldNotReachHere("lazyLoadDBinsertValue exception");
            }
        }

        @SuppressWarnings("unused")
        @Fallback
        protected Object lazyLoadDBinsertValue(Object value, Object file, Object ascii, Object compsxp, Object hook) {
            throw RError.error(this, RError.Message.INVALID_OR_UNIMPLEMENTED_ARGUMENTS);
        }

        /**
         * Append the compressed data to {@code path}. N.B The uncompressed length is stored as an
         * int in the first four bytes of the data. See {@link LazyLoadDBFetch}.
         *
         * @param path path of file
         * @param cdata the compressed data
         * @param ulen length of uncompressed data
         * @return offset in file of appended data
         */
        private int appendFile(String path, byte[] cdata, int ulen, RCompression.Type type) {
            File file = new File(path);
            try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file, true))) {
                int result = (int) file.length();
                ByteBuffer dataLengthBuf = ByteBuffer.allocate(4);
                dataLengthBuf.putInt(ulen);
                dataLengthBuf.position(0);
                byte[] ulenData = new byte[4];
                dataLengthBuf.get(ulenData);
                out.write(ulenData);
                if (type == RCompression.Type.LZMA) {
                    out.write(RCompression.Type.LZMA.typeByte);
                }
                out.write(cdata);
                return result;
            } catch (IOException ex) {
                throw RError.error(this, Message.GENERIC, "lazyLoadDBinsertValue file append error");
            }
        }

    }

    @RBuiltin(name = "lazyLoadDBflush", kind = INTERNAL, parameterNames = "path")
    public abstract static class LazyLoadDBFlush extends RBuiltinNode {
        @Specialization
        protected RNull doLazyLoadDBFlush(RAbstractStringVector dbPath) {
            RContext.getInstance().stateLazyDBCache.remove(dbPath.getDataAt(0));
            return RNull.instance;
        }
    }

    /*
     * Created as primitive function to avoid incrementing reference count for the argument.
     * 
     * returns -1 for non-shareable, 0 for private, 1 for temp, 2 for shared and
     * SHARED_PERMANENT_VAL for permanent shared
     */
    @RBuiltin(name = "fastr.refcountinfo", kind = PRIMITIVE, parameterNames = {""})
    public abstract static class RefCountInfo extends RBuiltinNode {
        @Specialization
        protected int refcount(Object x) {
            if (x instanceof RShareable) {
                RShareable s = (RShareable) x;
                if (s.isTemporary()) {
                    return 0;
                } else if (s.isSharedPermanent()) {
                    return RShareable.SHARED_PERMANENT_VAL;
                } else if (s.isShared()) {
                    return 2;
                } else {
                    return 1;
                }
            } else {
                return -1;
            }
        }
    }

    /*
     * Created as primitive function to avoid incrementing reference count for the argument (used in
     * reference count tests).
     */
    @RBuiltin(name = "fastr.identity", kind = PRIMITIVE, parameterNames = {""})
    public abstract static class Identity extends RBuiltinNode {
        @Specialization
        @TruffleBoundary
        protected int typeof(Object x) {
            return System.identityHashCode(x);
        }
    }
}
