/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.base.foreign;

import java.util.Arrays;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.r.nodes.access.vector.ElementAccessMode;
import com.oracle.truffle.r.nodes.access.vector.ExtractVectorNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.FastROptions;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.RBuiltinKind;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RExternalPtr;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.ffi.DLL;
import com.oracle.truffle.r.runtime.ffi.RFFIFactory;

/**
 * {@code .C} functions.
 *
 * TODO Completeness (more types, more error checks), Performance (copying). Especially all the
 * subtleties around copying.
 *
 * See <a href="https://stat.ethz.ch/R-manual/R-devel/library/base/html/Foreign.html">here</a>.
 */
@RBuiltin(name = ".C", kind = RBuiltinKind.PRIMITIVE, parameterNames = {".NAME", "...", "NAOK", "DUP", "PACKAGE", "ENCODING"})
public abstract class DotC extends RBuiltinNode {

    private static final int SCALAR_DOUBLE = 0;
    private static final int SCALAR_INT = 1;
    private static final int SCALAR_LOGICAL = 2;
    @SuppressWarnings("unused") private static final int SCALAR_STRING = 3;
    private static final int VECTOR_DOUBLE = 10;
    private static final int VECTOR_INT = 11;
    private static final int VECTOR_LOGICAL = 12;
    @SuppressWarnings("unused") private static final int VECTOR_STRING = 12;

    @Child private ExtractVectorNode nameExtract = ExtractVectorNode.create(ElementAccessMode.SUBSCRIPT, true);
    @Child private ExtractVectorNode addressExtract = ExtractVectorNode.create(ElementAccessMode.SUBSCRIPT, true);

    @Override
    public Object[] getDefaultParameterValues() {
        return new Object[]{RMissing.instance, RArgsValuesAndNames.EMPTY, RRuntime.LOGICAL_FALSE, RRuntime.LOGICAL_FALSE, RMissing.instance, RMissing.instance};
    }

    @SuppressWarnings("unused")
    @Specialization
    protected RList c(VirtualFrame frame, RList symbol, RArgsValuesAndNames args, byte naok, byte dup, Object rPackage, RMissing encoding) {
        long address = getAddressFromSymbolInfo(frame, symbol);
        String name = getNameFromSymbolInfo(frame, symbol);
        return dispatch(this, address, name, naok, dup, args);
    }

    @SuppressWarnings("unused")
    @Specialization
    protected RList c(RAbstractStringVector f, RArgsValuesAndNames args, byte naok, byte dup, Object rPackage, RMissing encoding, //
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
        long func = DLL.findSymbol(f.getDataAt(0), libName, rns);
        if (func == -1) {
            errorProfile.enter();
            throw RError.error(this, RError.Message.C_SYMBOL_NOT_IN_TABLE, f);
        }
        return dispatch(this, func, f.getDataAt(0), naok, dup, args);
    }

    private String getNameFromSymbolInfo(VirtualFrame frame, RList symbol) {
        if (nameExtract == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            nameExtract = ExtractVectorNode.create(ElementAccessMode.SUBSCRIPT, true);
        }
        return RRuntime.asString(nameExtract.applyAccessField(frame, symbol, "name"));
    }

    private long getAddressFromSymbolInfo(VirtualFrame frame, RList symbol) {
        if (addressExtract == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            addressExtract = ExtractVectorNode.create(ElementAccessMode.SUBSCRIPT, true);
        }
        return ((RExternalPtr) addressExtract.applyAccessField(frame, symbol, "address")).getAddr();
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

    @TruffleBoundary
    protected static RList dispatch(RBuiltinNode node, long address, String name, byte naok, byte dup, RArgsValuesAndNames args) {
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
        if (FastROptions.TraceNativeCalls.getBooleanValue()) {
            trace(name, nativeArgs);
        }
        RFFIFactory.getRFFI().getCRFFI().invoke(address, nativeArgs);
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
                    results[i] = RDataFactory.createLogicalVector((byte[]) nativeArgs[i], RDataFactory.COMPLETE_VECTOR);
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

    @TruffleBoundary
    private static void trace(String name, Object[] nativeArgs) {
        System.out.println("calling " + name + ": " + Arrays.toString(nativeArgs));
    }
}
