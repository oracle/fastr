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

import java.nio.charset.Charset;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.SetNamesAttributeNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.base.foreign.FortranAndCFunctionsFactory.FortranResultNamesSetterNodeGen;
import com.oracle.truffle.r.nodes.builtin.base.foreign.LookupAdapter.ExtractNativeCallInfoNode;
import com.oracle.truffle.r.nodes.builtin.base.foreign.LookupAdapterFactory.ExtractNativeCallInfoNodeGen;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RString;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.ffi.CRFFI;
import com.oracle.truffle.r.runtime.ffi.DLL;
import com.oracle.truffle.r.runtime.ffi.NativeCallInfo;
import com.oracle.truffle.r.runtime.ffi.RFFIFactory;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

/**
 * {@code .C} and {@code .Fortran} functions, which share a common signature.
 *
 * TODO Completeness (more types, more error checks), Performance (copying). Especially all the
 * subtleties around copying.
 *
 * See <a href="https://stat.ethz.ch/R-manual/R-devel/library/base/html/Foreign.html">here</a>.
 */
public class FortranAndCFunctions {

    protected abstract static class CRFFIAdapter extends RBuiltinNode.Arg6 {
        private static final int SCALAR_DOUBLE = 0;
        private static final int SCALAR_INT = 1;
        private static final int SCALAR_LOGICAL = 2;
        private static final int SCALAR_STRING = 3;
        private static final int VECTOR_DOUBLE = 10;
        private static final int VECTOR_INT = 11;
        private static final int VECTOR_LOGICAL = 12;
        private static final int VECTOR_STRING = 13;

        private static final Charset charset = Charset.forName("US-ASCII");

        @Child protected ExtractNativeCallInfoNode extractSymbolInfo = ExtractNativeCallInfoNodeGen.create();
        @Child private CRFFI.InvokeCNode invokeCNode = RFFIFactory.getCRFFI().createInvokeCNode();

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
            boolean hasStrings = false;
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
                } else if (arg instanceof RAbstractStringVector) {
                    hasStrings = true;
                    argTypes[i] = VECTOR_STRING;
                    checkNAs(node, i + 1, (RAbstractStringVector) arg);
                    nativeArgs[i] = encodeStrings((RAbstractStringVector) arg);
                } else if (arg instanceof String) {
                    hasStrings = true;
                    argTypes[i] = SCALAR_STRING;
                    checkNAs(node, i + 1, RString.valueOf((String) arg));
                    nativeArgs[i] = new byte[][]{encodeString((String) arg)};
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
                    throw node.error(RError.Message.UNIMPLEMENTED_ARG_TYPE, i + 1);
                }
            }
            invokeCNode.execute(nativeCallInfo, nativeArgs, hasStrings);
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
                    case SCALAR_STRING:
                        results[i] = RDataFactory.createStringVector(decodeStrings((byte[][]) nativeArgs[i]), RDataFactory.COMPLETE_VECTOR);
                        break;
                    case VECTOR_STRING:
                        results[i] = ((RAbstractStringVector) array[i]).materialize().copyResetData(decodeStrings((byte[][]) nativeArgs[i]));
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
                    throw node.error(RError.Message.NA_IN_FOREIGN_FUNCTION_CALL, argIndex);
                }
            }
            return data;
        }

        private static void checkNAs(RBuiltinNode node, int argIndex, RAbstractStringVector data) {
            CompilerAsserts.neverPartOfCompilation();
            for (int i = 0; i < data.getLength(); i++) {
                if (RRuntime.isNA(data.getDataAt(i))) {
                    throw node.error(RError.Message.NA_IN_FOREIGN_FUNCTION_CALL, argIndex);
                }
            }
        }

        private static double[] checkNAs(RBuiltinNode node, int argIndex, double[] data) {
            CompilerAsserts.neverPartOfCompilation();
            for (int i = 0; i < data.length; i++) {
                if (!RRuntime.isFinite(data[i])) {
                    throw node.error(RError.Message.NA_NAN_INF_IN_FOREIGN_FUNCTION_CALL, argIndex);
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

        private static Object encodeStrings(RAbstractStringVector vector) {
            byte[][] result = new byte[vector.getLength()][];
            for (int i = 0; i < vector.getLength(); i++) {
                result[i] = encodeString(vector.getDataAt(i));
            }
            return result;
        }

        private static byte[] encodeString(String str) {
            byte[] bytes = str.getBytes(charset);
            byte[] result = new byte[bytes.length + 1];
            System.arraycopy(bytes, 0, result, 0, bytes.length);
            return result;
        }

        private static String[] decodeStrings(byte[][] bytes) {
            String[] result = new String[bytes.length];
            for (int i = 0; i < bytes.length; i++) {
                result[i] = new String(bytes[i], charset);
            }
            return result;
        }
    }

    /**
     * Interface to .Fortran native functions. Some functions have explicit implementations in
     * FastR, otherwise the .Fortran interface uses the machinery that implements the .C interface.
     */
    @RBuiltin(name = ".Fortran", kind = PRIMITIVE, parameterNames = {".NAME", "...", "NAOK", "DUP", "PACKAGE", "ENCODING"}, behavior = COMPLEX)
    public abstract static class Fortran extends CRFFIAdapter implements Lookup {

        static {
            Casts.noCasts(Fortran.class);
        }

        @Child private FortranResultNamesSetter resNamesSetter = FortranResultNamesSetterNodeGen.create();

        @Override
        @TruffleBoundary
        public RExternalBuiltinNode lookupBuiltin(RList symbol) {
            switch (LookupAdapter.lookupName(symbol)) {
                case "dqrdc2":
                    return Dqrdc2.create();
                case "dqrcf":
                    return DqrcfNodeGen.create();
                case "dqrqty":
                    return DqrqtyNodeGen.create();
                case "dqrqy":
                    return DqrqyNodeGen.create();
                case "dqrrsd":
                    return DqrrsdNodeGen.create();
                case "dqrxb":
                    return DqrxbNodeGen.create();
                default:
                    return null;
            }
        }

        @SuppressWarnings("unused")
        @Specialization(limit = "1", guards = {"cached == symbol", "builtin != null"})
        protected Object doExternal(VirtualFrame frame, RList symbol, RArgsValuesAndNames args, byte naok, byte dup, Object rPackage, RMissing encoding,
                        @Cached("symbol") RList cached,
                        @Cached("lookupBuiltin(symbol)") RExternalBuiltinNode builtin) {
            return resNamesSetter.execute(builtin.call(frame, args), args);
        }

        @Specialization(guards = "lookupBuiltin(symbol) == null")
        protected RList c(VirtualFrame frame, RList symbol, RArgsValuesAndNames args, byte naok, byte dup, @SuppressWarnings("unused") Object rPackage,
                        @SuppressWarnings("unused") RMissing encoding) {
            NativeCallInfo nativeCallInfo = extractSymbolInfo.execute(frame, symbol);
            return dispatch(this, nativeCallInfo, naok, dup, args);
        }

        @Specialization
        protected RList c(RAbstractStringVector symbol, RArgsValuesAndNames args, byte naok, byte dup, Object rPackage, @SuppressWarnings("unused") RMissing encoding,
                        @Cached("create()") DLL.RFindSymbolNode findSymbolNode) {
            String libName = LookupAdapter.checkPackageArg(rPackage);
            DLL.RegisteredNativeSymbol rns = new DLL.RegisteredNativeSymbol(DLL.NativeSymbolType.Fortran, null, null);
            DLL.SymbolHandle func = findSymbolNode.execute(symbol.getDataAt(0), libName, rns);
            if (func == DLL.SYMBOL_NOT_FOUND) {
                throw error(RError.Message.C_SYMBOL_NOT_IN_TABLE, symbol);
            }
            return dispatch(this, new NativeCallInfo(symbol.getDataAt(0), func, rns.getDllInfo()), naok, dup, args);
        }

        @SuppressWarnings("unused")
        @Fallback
        protected Object fallback(Object symbol, Object args, Object naok, Object dup, Object rPackage, Object encoding) {
            throw LookupAdapter.fallback(this, symbol);
        }
    }

    public abstract static class FortranResultNamesSetter extends RBaseNode {

        public abstract Object execute(Object result, RArgsValuesAndNames argNames);

        @Specialization
        public Object handleArgNames(RAttributable result, RArgsValuesAndNames argValNames,
                        @Cached("create()") SetNamesAttributeNode namesSetter,
                        @Cached("create()") BranchProfile namesProfile) {
            ArgumentsSignature sig = argValNames.getSignature();
            if (sig.getNonNullCount() > 0) {
                namesProfile.enter();
                String[] argNames = sig.getNames();
                String[] names = new String[sig.getLength()];
                for (int i = 0; i < sig.getLength(); i++) {
                    String argName = argNames[i];
                    if (argName == null) {
                        names[i] = "";
                    } else {
                        names[i] = argName;
                    }
                }
                namesSetter.execute(result, RDataFactory.createStringVector(names, true));
            }

            return result;
        }

        @Fallback
        public Object handleOthers(Object result, @SuppressWarnings("unused") RArgsValuesAndNames argNames) {
            // do nothing
            return result;
        }
    }

    @RBuiltin(name = ".C", kind = PRIMITIVE, parameterNames = {".NAME", "...", "NAOK", "DUP", "PACKAGE", "ENCODING"}, behavior = COMPLEX)
    public abstract static class DotC extends CRFFIAdapter {

        static {
            Casts.noCasts(DotC.class);
        }

        @Specialization
        protected RList c(VirtualFrame frame, RList symbol, RArgsValuesAndNames args, byte naok, byte dup, @SuppressWarnings("unused") Object rPackage, @SuppressWarnings("unused") RMissing encoding) {
            NativeCallInfo nativeCallInfo = extractSymbolInfo.execute(frame, symbol);
            return dispatch(this, nativeCallInfo, naok, dup, args);
        }

        @Specialization
        protected RList c(RAbstractStringVector symbol, RArgsValuesAndNames args, byte naok, byte dup, Object rPackage, @SuppressWarnings("unused") RMissing encoding,
                        @Cached("create()") DLL.RFindSymbolNode findSymbolNode) {
            String libName = null;
            if (!(rPackage instanceof RMissing)) {
                libName = RRuntime.asString(rPackage);
                if (libName == null) {
                    throw error(RError.Message.ARGUMENT_MUST_BE_STRING, "PACKAGE");
                }
            }
            DLL.RegisteredNativeSymbol rns = new DLL.RegisteredNativeSymbol(DLL.NativeSymbolType.C, null, null);
            DLL.SymbolHandle func = findSymbolNode.execute(symbol.getDataAt(0), libName, rns);
            if (func == DLL.SYMBOL_NOT_FOUND) {
                throw error(RError.Message.C_SYMBOL_NOT_IN_TABLE, symbol);
            }
            return dispatch(this, new NativeCallInfo(symbol.getDataAt(0), func, rns.getDllInfo()), naok, dup, args);
        }
    }
}
