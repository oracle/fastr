/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.base.foreign;

import static com.oracle.truffle.r.runtime.builtins.RBehavior.COMPLEX;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.ffi.CRFFI;
import com.oracle.truffle.r.runtime.ffi.DLL;
import com.oracle.truffle.r.runtime.ffi.NativeCallInfo;
import com.oracle.truffle.r.runtime.ffi.RFFIFactory;

/**
 * {@code .C} and {@code .Fortran} functions, which share a common signature.
 *
 * TODO Completeness (more types, more error checks), Performance (copying). Especially all the
 * subtleties around copying.
 *
 * See <a href="https://stat.ethz.ch/R-manual/R-devel/library/base/html/Foreign.html">here</a>.
 */
public class FortranAndCFunctions {

    protected abstract static class CRFFIAdapter extends LookupAdapter {
        private static final int SCALAR_DOUBLE = 0;
        private static final int SCALAR_INT = 1;
        private static final int SCALAR_LOGICAL = 2;
        @SuppressWarnings("unused") private static final int SCALAR_STRING = 3;
        private static final int VECTOR_DOUBLE = 10;
        private static final int VECTOR_INT = 11;
        private static final int VECTOR_LOGICAL = 12;
        @SuppressWarnings("unused") private static final int VECTOR_STRING = 12;

        @Child private CRFFI.InvokeCNode invokeCNode = RFFIFactory.getRFFI().getCRFFI().createInvokeCNode();

        @Override
        public Object[] getDefaultParameterValues() {
            return new Object[]{RMissing.instance, RArgsValuesAndNames.EMPTY, RRuntime.LOGICAL_FALSE, RRuntime.LOGICAL_FALSE, RMissing.instance, RMissing.instance};
        }

        @TruffleBoundary
        protected RList dispatch(RBuiltinNode node, NativeCallInfo nativeCallInfo, byte naok, byte dup, RArgsValuesAndNames args) {
            @SuppressWarnings("unused")
            boolean dupArgs = RRuntime.fromLogical(dup);
            @SuppressWarnings("unused")
            boolean checkNA = RRuntime.fromLogical(naok);
            // Analyze the args, making copies (ignoring dup for now)
            Object[] array = args.getArguments();
            int[] argTypes = new int[array.length];
            Object[] nativeArgs = new Object[array.length];
            for (int i = 0; i < array.length; i++) {
                Object arg = array[i];
                if (arg instanceof RAbstractDoubleVector) {
                    argTypes[i] = VECTOR_DOUBLE;
                    nativeArgs[i] = checkNAs(node, i + 1, ((RAbstractDoubleVector) arg).materialize().getDataCopy());
                } else if (arg instanceof RAbstractIntVector) {
                    argTypes[i] = VECTOR_INT;
                    nativeArgs[i] = checkNAs(node, i + 1, ((RAbstractIntVector) arg).materialize().getDataCopy());
                } else if (arg instanceof RAbstractLogicalVector) {
                    argTypes[i] = VECTOR_LOGICAL;
                    // passed as int[]
                    byte[] data = ((RAbstractLogicalVector) arg).materialize().getDataWithoutCopying();
                    int[] dataAsInt = new int[data.length];
                    for (int j = 0; j < data.length; j++) {
                        // An NA is an error but the error handling happens in checkNAs
                        dataAsInt[j] = RRuntime.isNA(data[j]) ? RRuntime.INT_NA : data[j];
                    }
                    nativeArgs[i] = checkNAs(node, i + 1, dataAsInt);
                } else if (arg instanceof Double) {
                    argTypes[i] = SCALAR_DOUBLE;
                    nativeArgs[i] = checkNAs(node, i + 1, new double[]{(double) arg});
                } else if (arg instanceof Integer) {
                    argTypes[i] = SCALAR_INT;
                    nativeArgs[i] = checkNAs(node, i + 1, new int[]{(int) arg});
                } else if (arg instanceof Byte) {
                    argTypes[i] = SCALAR_LOGICAL;
                    nativeArgs[i] = checkNAs(node, i + 1, new int[]{RRuntime.isNA((byte) arg) ? RRuntime.INT_NA : (byte) arg});
                } else {
                    throw RError.error(node, RError.Message.UNIMPLEMENTED_ARG_TYPE, i + 1);
                }
            }
            invokeCNode.execute(nativeCallInfo, nativeArgs);
            // we have to assume that the native method updated everything
            RStringVector listNames = validateArgNames(array.length, args.getSignature());
            Object[] results = new Object[array.length];
            for (int i = 0; i < array.length; i++) {
                switch (argTypes[i]) {
                    case SCALAR_DOUBLE:
                        results[i] = RDataFactory.createDoubleVector((double[]) nativeArgs[i], RDataFactory.COMPLETE_VECTOR);
                        break;
                    case SCALAR_INT:
                        results[i] = RDataFactory.createIntVector((int[]) nativeArgs[i], RDataFactory.COMPLETE_VECTOR);
                        break;
                    case SCALAR_LOGICAL:
                        // have to convert back from int[]
                        int[] nativeIntArgs = (int[]) nativeArgs[i];
                        byte[] nativeByteArgs = new byte[nativeIntArgs.length];
                        for (int j = 0; j < nativeByteArgs.length; j++) {
                            int nativeInt = nativeIntArgs[j];
                            nativeByteArgs[j] = (byte) (nativeInt == RRuntime.INT_NA ? RRuntime.LOGICAL_NA : nativeInt & 0xFF);
                        }
                        results[i] = RDataFactory.createLogicalVector(nativeByteArgs, RDataFactory.COMPLETE_VECTOR);
                        break;
                    case VECTOR_DOUBLE:
                        results[i] = ((RAbstractDoubleVector) array[i]).materialize().copyResetData((double[]) nativeArgs[i]);
                        break;
                    case VECTOR_INT:
                        results[i] = ((RAbstractIntVector) array[i]).materialize().copyResetData((int[]) nativeArgs[i]);
                        break;
                    case VECTOR_LOGICAL: {
                        int[] intData = (int[]) nativeArgs[i];
                        byte[] byteData = new byte[intData.length];
                        for (int j = 0; j < intData.length; j++) {
                            byteData[j] = RRuntime.isNA(intData[j]) ? RRuntime.LOGICAL_NA : RRuntime.asLogical(intData[j] != 0);
                        }
                        results[i] = ((RAbstractLogicalVector) array[i]).materialize().copyResetData(byteData);
                        break;
                    }
                }
            }
            return RDataFactory.createList(results, listNames);
        }

        private static int[] checkNAs(RBuiltinNode node, int argIndex, int[] data) {
            CompilerAsserts.neverPartOfCompilation();
            for (int i = 0; i < data.length; i++) {
                if (RRuntime.isNA(data[i])) {
                    throw RError.error(node, RError.Message.NA_IN_FOREIGN_FUNCTION_CALL, argIndex);
                }
            }
            return data;
        }

        private static double[] checkNAs(RBuiltinNode node, int argIndex, double[] data) {
            CompilerAsserts.neverPartOfCompilation();
            for (int i = 0; i < data.length; i++) {
                if (!RRuntime.isFinite(data[i])) {
                    throw RError.error(node, RError.Message.NA_NAN_INF_IN_FOREIGN_FUNCTION_CALL, argIndex);
                }
            }
            return data;
        }

        private static RStringVector validateArgNames(int argsLength, ArgumentsSignature signature) {
            String[] listArgNames = new String[argsLength];
            for (int i = 0; i < argsLength; i++) {
                String name = signature.getName(i);
                if (name == null) {
                    name = RRuntime.NAMES_ATTR_EMPTY_VALUE;
                }
                listArgNames[i] = name;
            }
            return RDataFactory.createStringVector(listArgNames, RDataFactory.COMPLETE_VECTOR);
        }
    }

    /**
     * Interface to .Fortran native functions. Some functions have explicit implementations in
     * FastR, otherwise the .Fortran interface uses the machinery that implements the .C interface.
     */
    @RBuiltin(name = ".Fortran", kind = PRIMITIVE, parameterNames = {".NAME", "...", "NAOK", "DUP", "PACKAGE", "ENCODING"}, behavior = COMPLEX)
    public abstract static class Fortran extends CRFFIAdapter {

        @Override
        @TruffleBoundary
        protected RExternalBuiltinNode lookupBuiltin(RList symbol) {
            switch (lookupName(symbol)) {
                case "dqrdc2":
                    return new Dqrdc2();
                case "dqrcf":
                    return new Dqrcf();
                default:
                    return null;
            }
        }

        @SuppressWarnings("unused")
        @Specialization(limit = "1", guards = {"cached == symbol", "builtin != null"})
        protected Object doExternal(VirtualFrame frame, RList symbol, RArgsValuesAndNames args, byte naok, byte dup, Object rPackage, RMissing encoding,
                        @Cached("symbol") RList cached,
                        @Cached("lookupBuiltin(symbol)") RExternalBuiltinNode builtin) {
            return builtin.call(frame, args);
        }

        @Specialization(guards = "lookupBuiltin(symbol) == null")
        protected RList c(VirtualFrame frame, RList symbol, RArgsValuesAndNames args, byte naok, byte dup, @SuppressWarnings("unused") Object rPackage,
                        @SuppressWarnings("unused") RMissing encoding) {
            NativeCallInfo nativeCallInfo = extractSymbolInfo(frame, symbol);
            return dispatch(this, nativeCallInfo, naok, dup, args);
        }

        @Specialization
        protected RList c(RAbstractStringVector symbol, RArgsValuesAndNames args, byte naok, byte dup, Object rPackage, @SuppressWarnings("unused") RMissing encoding,
                        @Cached("create()") DLL.RFindSymbolNode findSymbolNode,
                        @Cached("create()") BranchProfile errorProfile) {
            String libName = checkPackageArg(rPackage, errorProfile);
            DLL.RegisteredNativeSymbol rns = new DLL.RegisteredNativeSymbol(DLL.NativeSymbolType.Fortran, null, null);
            DLL.SymbolHandle func = findSymbolNode.execute(symbol.getDataAt(0), libName, rns);
            if (func == DLL.SYMBOL_NOT_FOUND) {
                errorProfile.enter();
                throw RError.error(this, RError.Message.C_SYMBOL_NOT_IN_TABLE, symbol);
            }
            return dispatch(this, new NativeCallInfo(symbol.getDataAt(0), func, rns.getDllInfo()), naok, dup, args);
        }

        @SuppressWarnings("unused")
        @Fallback
        protected Object fallback(Object symbol, Object args, Object naok, Object dup, Object rPackage, Object encoding) {
            throw fallback(symbol);
        }
    }

    @RBuiltin(name = ".C", kind = PRIMITIVE, parameterNames = {".NAME", "...", "NAOK", "DUP", "PACKAGE", "ENCODING"}, behavior = COMPLEX)
    public abstract static class DotC extends CRFFIAdapter {

        @Override
        @TruffleBoundary
        protected RExternalBuiltinNode lookupBuiltin(RList symbol) {
            throw RInternalError.shouldNotReachHere();
        }

        @SuppressWarnings("unused")
        @Specialization
        protected RList c(VirtualFrame frame, RList symbol, RArgsValuesAndNames args, byte naok, byte dup, Object rPackage, RMissing encoding) {
            NativeCallInfo nativeCallInfo = extractSymbolInfo(frame, symbol);
            return dispatch(this, nativeCallInfo, naok, dup, args);
        }

        @SuppressWarnings("unused")
        @Specialization
        protected RList c(RAbstractStringVector symbol, RArgsValuesAndNames args, byte naok, byte dup, Object rPackage, RMissing encoding,
                        @Cached("create()") DLL.RFindSymbolNode findSymbolNode,
                        @Cached("create()") BranchProfile errorProfile) {
            String libName = null;
            if (!(rPackage instanceof RMissing)) {
                libName = RRuntime.asString(rPackage);
                if (libName == null) {
                    errorProfile.enter();
                    throw RError.error(this, RError.Message.ARGUMENT_MUST_BE_STRING, "PACKAGE");
                }
            }
            DLL.RegisteredNativeSymbol rns = new DLL.RegisteredNativeSymbol(DLL.NativeSymbolType.C, null, null);
            DLL.SymbolHandle func = findSymbolNode.execute(symbol.getDataAt(0), libName, rns);
            if (func == DLL.SYMBOL_NOT_FOUND) {
                errorProfile.enter();
                throw RError.error(this, RError.Message.C_SYMBOL_NOT_IN_TABLE, symbol);
            }
            return dispatch(this, new NativeCallInfo(symbol.getDataAt(0), func, rns.getDllInfo()), naok, dup, args);
        }
    }
}
