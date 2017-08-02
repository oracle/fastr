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
package com.oracle.truffle.r.runtime.ffi;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractAtomicVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

/**
 * Support for the {.C} and {.Fortran} calls.
 */
public interface CRFFI {

    public static abstract class InvokeCNode extends RBaseNode {

        public enum ArgumentType {
            VECTOR_DOUBLE,
            VECTOR_INT,
            VECTOR_LOGICAL,
            VECTOR_STRING;
        }

        private static final Charset charset = StandardCharsets.US_ASCII;

        /**
         * Invoke the native method identified by {@code symbolInfo} passing it the arguments in
         * {@code args}. The values in {@code args} should be native types,e.g., {@code double[]}
         * not {@code RDoubleVector}. Strings are already converted to 2-dimensional byte arrays.
         *
         * @param hasStrings if {@code true}, then the {@code args} array may contain one or more
         *            values of type {@code byte[][]}, which represent arrays of strings in ASCII
         *            encoding.
         */
        protected abstract void execute(NativeCallInfo nativeCallInfo, Object[] args, boolean hasStrings);

        @TruffleBoundary
        protected Object getNativeArgument(int index, ArgumentType type, RAbstractAtomicVector vector) {
            CompilerAsserts.neverPartOfCompilation();
            switch (type) {
                case VECTOR_DOUBLE: {
                    double[] data = ((RAbstractDoubleVector) vector).materialize().getDataCopy();
                    for (int i = 0; i < data.length; i++) {
                        if (!RRuntime.isFinite(data[i])) {
                            throw error(RError.Message.NA_NAN_INF_IN_FOREIGN_FUNCTION_CALL, index + 1);
                        }
                    }
                    return data;
                }
                case VECTOR_INT: {
                    int[] data = ((RAbstractIntVector) vector).materialize().getDataCopy();
                    for (int i = 0; i < data.length; i++) {
                        if (RRuntime.isNA(data[i])) {
                            throw error(RError.Message.NA_IN_FOREIGN_FUNCTION_CALL, index + 1);
                        }
                    }
                    return data;
                }
                case VECTOR_LOGICAL: {
                    // passed as int[]
                    byte[] data = ((RAbstractLogicalVector) vector).materialize().getDataWithoutCopying();
                    int[] dataAsInt = new int[data.length];
                    for (int j = 0; j < data.length; j++) {
                        // An NA is an error but the error handling happens in checkNAs
                        dataAsInt[j] = RRuntime.isNA(data[j]) ? RRuntime.INT_NA : data[j];
                    }
                    for (int i = 0; i < dataAsInt.length; i++) {
                        if (RRuntime.isNA(dataAsInt[i])) {
                            throw error(RError.Message.NA_IN_FOREIGN_FUNCTION_CALL, index + 1);
                        }
                    }
                    return dataAsInt;
                }
                case VECTOR_STRING: {
                    RAbstractStringVector data = (RAbstractStringVector) vector;
                    for (int i = 0; i < data.getLength(); i++) {
                        if (RRuntime.isNA(data.getDataAt(i))) {
                            throw error(RError.Message.NA_IN_FOREIGN_FUNCTION_CALL, index + 1);
                        }
                    }
                    return encodeStrings((RAbstractStringVector) vector);
                }
                default:
                    throw RInternalError.shouldNotReachHere();
            }
        }

        private Object[] getNativeArguments(Object[] array, ArgumentType[] argTypes) {
            Object[] nativeArgs = new Object[array.length];
            for (int i = 0; i < array.length; i++) {
                nativeArgs[i] = getNativeArgument(i, argTypes[i], (RAbstractAtomicVector) array[i]);
            }
            return nativeArgs;
        }

        @TruffleBoundary
        protected Object postProcessArgument(ArgumentType type, RAbstractAtomicVector vector, Object nativeArgument) {
            switch (type) {
                case VECTOR_STRING:
                    return ((RAbstractStringVector) vector).materialize().copyResetData(decodeStrings((byte[][]) nativeArgument));
                case VECTOR_DOUBLE:
                    return ((RAbstractDoubleVector) vector).materialize().copyResetData((double[]) nativeArgument);
                case VECTOR_INT:
                    return ((RAbstractIntVector) vector).materialize().copyResetData((int[]) nativeArgument);
                case VECTOR_LOGICAL: {
                    int[] intData = (int[]) nativeArgument;
                    byte[] byteData = new byte[intData.length];
                    for (int j = 0; j < intData.length; j++) {
                        byteData[j] = RRuntime.isNA(intData[j]) ? RRuntime.LOGICAL_NA : RRuntime.asLogical(intData[j] != 0);
                    }
                    return ((RAbstractLogicalVector) vector).materialize().copyResetData(byteData);
                }
                default:
                    throw RInternalError.shouldNotReachHere();
            }
        }

        private Object[] postProcessArguments(Object[] array, ArgumentType[] argTypes, Object[] nativeArgs) {
            Object[] results = new Object[array.length];
            for (int i = 0; i < array.length; i++) {
                results[i] = postProcessArgument(argTypes[i], (RAbstractAtomicVector) array[i], nativeArgs[i]);
            }
            return results;
        }

        @TruffleBoundary
        public final RList dispatch(NativeCallInfo nativeCallInfo, byte naok, byte dup, RArgsValuesAndNames args) {
            @SuppressWarnings("unused")
            boolean dupArgs = RRuntime.fromLogical(dup);
            @SuppressWarnings("unused")
            boolean checkNA = RRuntime.fromLogical(naok);
            // Analyze the args, making copies (ignoring dup for now)
            Object[] array = new Object[args.getLength()];
            ArgumentType[] argTypes = new ArgumentType[array.length];
            boolean hasStrings = false;
            for (int i = 0; i < array.length; i++) {
                Object arg = args.getArgument(i);
                ArgumentType type;
                RAbstractAtomicVector vector;
                if (arg instanceof RAbstractDoubleVector) {
                    vector = (RAbstractDoubleVector) arg;
                    type = ArgumentType.VECTOR_DOUBLE;
                } else if (arg instanceof RAbstractIntVector) {
                    vector = (RAbstractIntVector) arg;
                    type = ArgumentType.VECTOR_INT;
                } else if (arg instanceof RAbstractLogicalVector) {
                    vector = (RAbstractLogicalVector) arg;
                    type = ArgumentType.VECTOR_LOGICAL;
                } else if (arg instanceof RAbstractStringVector) {
                    hasStrings = true;
                    vector = (RAbstractStringVector) arg;
                    type = ArgumentType.VECTOR_STRING;
                } else if (arg instanceof String) {
                    hasStrings = true;
                    vector = RDataFactory.createStringVectorFromScalar((String) arg);
                    type = ArgumentType.VECTOR_STRING;
                } else if (arg instanceof Double) {
                    vector = RDataFactory.createDoubleVectorFromScalar((double) arg);
                    type = ArgumentType.VECTOR_DOUBLE;
                } else if (arg instanceof Integer) {
                    vector = RDataFactory.createIntVectorFromScalar((int) arg);
                    type = ArgumentType.VECTOR_INT;
                } else if (arg instanceof Byte) {
                    vector = RDataFactory.createLogicalVectorFromScalar((byte) arg);
                    type = ArgumentType.VECTOR_LOGICAL;
                } else {
                    throw error(RError.Message.UNIMPLEMENTED_ARG_TYPE, i + 1);
                }
                argTypes[i] = type;
                array[i] = vector;
            }
            Object[] nativeArgs = getNativeArguments(array, argTypes);
            execute(nativeCallInfo, nativeArgs, hasStrings);
            // we have to assume that the native method updated everything
            RStringVector listNames = validateArgNames(array.length, args.getSignature());
            Object[] results = postProcessArguments(array, argTypes, nativeArgs);
            return RDataFactory.createList(results, listNames);
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
                int length = 0;
                while (length < bytes[i].length && bytes[i][length] != 0) {
                    length++;
                }
                result[i] = new String(bytes[i], 0, length, charset);
            }
            return result;
        }

    }

    InvokeCNode createInvokeCNode();
}
