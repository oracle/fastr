/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.instanceOf;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.runtime.RVisibility.OFF;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.COMPLEX;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.r.nodes.RASTUtils;
import com.oracle.truffle.r.nodes.access.ConstantNode;
import com.oracle.truffle.r.nodes.access.variables.ReadVariableNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.SetClassAttributeNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.function.PromiseHelperNode;
import com.oracle.truffle.r.nodes.function.RCallNode;
import com.oracle.truffle.r.nodes.function.call.CallRFunctionCachedNode;
import com.oracle.truffle.r.nodes.function.call.CallRFunctionCachedNodeGen;
import com.oracle.truffle.r.runtime.RCaller;
import com.oracle.truffle.r.runtime.RCompression;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RSerialize;
import com.oracle.truffle.r.runtime.SubstituteVirtualFrame;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.Closure;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RExternalPtr;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RLanguage;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.data.RPromise.PromiseState;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.env.REnvironment.PutException;
import com.oracle.truffle.r.runtime.ffi.DLL;
import com.oracle.truffle.r.runtime.ffi.DLL.DLLInfo;

/**
 * Private, undocumented, {@code .Internal} and {@code .Primitive} functions transcribed from GnuR,
 * but also those that need to be defined as builtins in FastR.
 */
public class HiddenInternalFunctions {

    /**
     * Transcribed from GnuR {@code do_makeLazy} in src/main/builtin.c.
     */
    @RBuiltin(name = "makeLazy", visibility = OFF, kind = INTERNAL, parameterNames = {"names", "values", "expr", "eval.env", "assign.env"}, behavior = COMPLEX)
    public abstract static class MakeLazy extends RBuiltinNode.Arg5 {
        @Child private Eval eval;

        private void initEval() {
            if (eval == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                eval = insert(EvalNodeGen.create());
            }
        }

        static {
            Casts casts = new Casts(MakeLazy.class);
            casts.arg("names").mustBe(stringValue()).asStringVector();
            casts.arg("eval.env").mustBe(instanceOf(REnvironment.class));
            casts.arg("assign.env").mustBe(instanceOf(REnvironment.class));
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
                    aenv.put(name, RDataFactory.createPromise(PromiseState.Explicit, Closure.createPromiseClosure(expr0), eenv.getFrame()));
                } catch (PutException ex) {
                    /*
                     * When loading the {@code base} package we may encounter a locked binding that
                     * holds an {@link RBuiltin} that is a {@link RBuiltinKind#SUBSTITUTE}. This is
                     * not an error, but is used as an override mechanism.
                     */
                    if (!RContext.getInstance().getLoadingBase()) {
                        throw error(ex);
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
    @RBuiltin(name = "importIntoEnv", kind = INTERNAL, parameterNames = {"impenv", "impnames", "expenv", "expnames"}, behavior = COMPLEX)
    public abstract static class ImportIntoEnv extends RBuiltinNode.Arg4 {

        static {
            Casts casts = new Casts(ImportIntoEnv.class);
            casts.arg("impenv").mustNotBeNull(RError.Message.USE_NULL_ENV_DEFUNCT).mustBe(instanceOf(REnvironment.class), RError.Message.BAD_ENVIRONMENT, "import");
            casts.arg("impnames").defaultError(RError.Message.INVALID_ARGUMENT, "names").mustBe(stringValue()).asStringVector();
            casts.arg("expenv").mustNotBeNull(RError.Message.USE_NULL_ENV_DEFUNCT).mustBe(instanceOf(REnvironment.class), RError.Message.BAD_ENVIRONMENT, "import");
            casts.arg("expnames").defaultError(RError.Message.INVALID_ARGUMENT, "names").mustBe(stringValue(), RError.Message.INVALID_ARGUMENT, "names").asStringVector();
        }

        @Specialization
        @TruffleBoundary
        protected RNull importIntoEnv(REnvironment impEnv, RAbstractStringVector impNames, REnvironment expEnv, RAbstractStringVector expNames) {
            int length = impNames.getLength();
            if (length != expNames.getLength()) {
                throw error(Message.IMP_EXP_NAMES_MATCH);
            }
            for (int i = 0; i < length; i++) {
                String impsym = impNames.getDataAt(i);
                String expsym = expNames.getDataAt(i);
                Object binding = null;
                // TODO name translation, and a bunch of other special cases
                for (REnvironment env = expEnv; env != REnvironment.emptyEnv(); env = env.getParent()) {
                    binding = env.get(expsym);
                    if (binding != null) {
                        break;
                    }
                }
                try {
                    impEnv.put(impsym, binding);
                } catch (PutException ex) {
                    throw error(ex);
                }
            }
            return RNull.instance;
        }
    }

    /**
     * Transcribed from {@code lazyLoaadDBFetch} in src/serialize.c.
     */
    @RBuiltin(name = "lazyLoadDBfetch", kind = PRIMITIVE, parameterNames = {"key", "datafile", "compressed", "envhook"}, behavior = PURE)
    public abstract static class LazyLoadDBFetch extends RBuiltinNode.Arg4 {

        @Child private CallRFunctionCachedNode callCache = CallRFunctionCachedNodeGen.create(2);

        static {
            Casts casts = new Casts(LazyLoadDBFetch.class);
            casts.arg("compressed").asIntegerVector().findFirst();
        }

        /**
         * No error checking here as this called by trusted library code.
         */
        @Specialization
        protected Object lazyLoadDBFetch(VirtualFrame frame, RIntVector key, RStringVector datafile, int compressed, RFunction envhook) {
            return lazyLoadDBFetchInternal(frame.materialize(), key, datafile, compressed, envhook);
        }

        @TruffleBoundary
        private Object lazyLoadDBFetchInternal(MaterializedFrame frame, RIntVector key, RStringVector datafile, int compression, RFunction envhook) {
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
             * "offset + 4". Values 2 and 3 have a "type" field at "offset + 4
             * " and the data starts at "offset + 5". The type field is 'Z' for lzma, '2' for bzip,
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
                        warning(RError.Message.GENERIC, "unknown compression type");
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
                throw error(RError.Message.LAZY_LOAD_DB_CORRUPT, dbPath);
            }
            try {
                RSerialize.CallHook callHook = new RSerialize.CallHook() {
                    @Override
                    public Object eval(Object arg) {
                        return callCache.execute(SubstituteVirtualFrame.create(frame), envhook, RCaller.create(frame, getOriginalCall()), new Object[]{arg}, null);
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

    @RBuiltin(name = "getRegisteredRoutines", kind = INTERNAL, parameterNames = "info", behavior = COMPLEX)
    public abstract static class GetRegisteredRoutines extends RBuiltinNode.Arg1 {
        private static final RStringVector NAMES = RDataFactory.createStringVector(new String[]{".C", ".Call", ".Fortran", ".External"}, RDataFactory.COMPLETE_VECTOR);
        private static final RStringVector NATIVE_ROUTINE_LIST = RDataFactory.createStringVectorFromScalar("NativeRoutineList");

        static {
            Casts.noCasts(GetRegisteredRoutines.class);
        }

        @Specialization
        protected RList getRegisteredRoutines(@SuppressWarnings("unused") RNull info) {
            throw error(RError.Message.NULL_DLLINFO);
        }

        @Specialization(guards = "isDLLInfo(externalPtr)")
        @TruffleBoundary
        protected RList getRegisteredRoutines(RExternalPtr externalPtr,
                        @Cached("create()") SetClassAttributeNode setClassAttrNode) {
            Object[] data = new Object[NAMES.getLength()];
            DLL.DLLInfo dllInfo = (DLLInfo) externalPtr.getExternalObject();
            RInternalError.guarantee(dllInfo != null);
            for (DLL.NativeSymbolType nst : DLL.NativeSymbolType.values()) {
                DLL.DotSymbol[] symbols = dllInfo.getNativeSymbols(nst);
                if (symbols == null) {
                    symbols = new DLL.DotSymbol[0];
                }
                Object[] symbolData = new Object[symbols.length];
                for (int i = 0; i < symbols.length; i++) {
                    DLL.DotSymbol symbol = symbols[i];
                    DLL.RegisteredNativeSymbol rnt = new DLL.RegisteredNativeSymbol(nst, symbol, dllInfo);
                    DLL.SymbolInfo symbolInfo = new DLL.SymbolInfo(dllInfo, symbol.name, symbol.fun);
                    symbolData[i] = symbolInfo.createRSymbolObject(rnt, true);
                }
                RList symbolDataList = RDataFactory.createList(symbolData);
                setClassAttrNode.execute(symbolDataList, NATIVE_ROUTINE_LIST);
                data[nst.ordinal()] = symbolDataList;
            }
            return RDataFactory.createList(data, NAMES);
        }

        @Fallback
        protected RList getRegisteredRoutines(@SuppressWarnings("unused") Object info) {
            throw error(RError.Message.REQUIRES_DLLINFO);
        }

        protected static boolean isDLLInfo(RExternalPtr externalPtr) {
            return DLL.isDLLInfo(externalPtr);
        }
    }

    @RBuiltin(name = "getVarsFromFrame", kind = INTERNAL, parameterNames = {"vars", "e", "force"}, behavior = COMPLEX)
    public abstract static class GetVarsFromFrame extends RBuiltinNode.Arg3 {
        @Child private PromiseHelperNode promiseHelper;

        static {
            Casts.noCasts(GetVarsFromFrame.class);
        }

        @Specialization
        protected RList getVarsFromFrame(VirtualFrame frame, RAbstractStringVector varsVec, REnvironment env, byte forceArg) {
            boolean force = RRuntime.fromLogical(forceArg);
            Object[] data = new Object[varsVec.getLength()];
            for (int i = 0; i < data.length; i++) {
                String var = varsVec.getDataAt(i);
                Object value = env.get(var);
                if (value == null) {
                    throw error(RError.Message.UNKNOWN_OBJECT, var);
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
            throw error(RError.Message.INVALID_OR_UNIMPLEMENTED_ARGUMENTS);
        }
    }

    @RBuiltin(name = "lazyLoadDBinsertValue", kind = INTERNAL, parameterNames = {"value", "file", "ascii", "compsxp", "hook"}, behavior = COMPLEX)
    public abstract static class LazyLoadDBinsertValue extends RBuiltinNode.Arg5 {

        @Child private CallRFunctionCachedNode callCache = CallRFunctionCachedNodeGen.create(2);

        static {
            Casts casts = new Casts(LazyLoadDBinsertValue.class);
            casts.arg("ascii").asIntegerVector().findFirst();
            casts.arg("compsxp").asIntegerVector().findFirst();
        }

        @Specialization
        protected RIntVector lazyLoadDBinsertValue(VirtualFrame frame, Object value, RAbstractStringVector file, int asciiL, int compression, RFunction hook) {
            return lazyLoadDBinsertValueInternal(frame.materialize(), value, file, asciiL, compression, hook);
        }

        @TruffleBoundary
        private RIntVector lazyLoadDBinsertValueInternal(MaterializedFrame frame, Object value, RAbstractStringVector file, int type, int compression, RFunction hook) {
            if (!(compression == 1 || compression == 3)) {
                throw error(Message.GENERIC, "unsupported compression");
            }

            RSerialize.CallHook callHook = new RSerialize.CallHook() {
                @Override
                public Object eval(Object arg) {
                    return callCache.execute(SubstituteVirtualFrame.create(frame), hook, RCaller.create(frame, getOriginalCall()), new Object[]{arg}, null);
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
                        throw error(Message.GENERIC, "zlib compress error");
                    }
                } else if (compression == 3) {
                    ctype = RCompression.Type.XZ;
                    offset = 5;
                    outLen = data.length;
                    cdata = new byte[outLen];
                    boolean rc = RCompression.compress(ctype, data, cdata);
                    if (!rc) {
                        throw error(Message.GENERIC, "lzma compress error");
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
                throw RInternalError.shouldNotReachHere(ex, "lazyLoadDBinsertValue exception");
            }
        }

        @SuppressWarnings("unused")
        @Fallback
        protected Object lazyLoadDBinsertValue(Object value, Object file, Object ascii, Object compsxp, Object hook) {
            throw error(RError.Message.INVALID_OR_UNIMPLEMENTED_ARGUMENTS);
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
                if (type == RCompression.Type.XZ) {
                    out.write(RCompression.Type.XZ.typeByte);
                }
                out.write(cdata);
                return result;
            } catch (IOException ex) {
                throw RError.ioError(this, ex);
            }
        }
    }

    @RBuiltin(name = "lazyLoadDBflush", kind = INTERNAL, parameterNames = "path", behavior = COMPLEX)
    public abstract static class LazyLoadDBFlush extends RBuiltinNode.Arg1 {

        static {
            Casts.noCasts(LazyLoadDBFlush.class);
        }

        @Specialization
        @TruffleBoundary
        protected RNull doLazyLoadDBFlush(RAbstractStringVector dbPath) {
            RContext.getInstance().stateLazyDBCache.remove(dbPath.getDataAt(0));
            return RNull.instance;
        }
    }
}
