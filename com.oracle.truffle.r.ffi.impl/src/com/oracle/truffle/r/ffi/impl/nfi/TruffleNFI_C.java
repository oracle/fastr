/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.ffi.impl.nfi;

import java.nio.charset.StandardCharsets;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.CanResolve;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.MessageResolution;
import com.oracle.truffle.api.interop.Resolve;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.java.JavaInterop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.ffi.impl.interop.UnsafeAdapter;
import com.oracle.truffle.r.ffi.impl.nfi.TruffleNFI_CFactory.TruffleNFI_InvokeCNodeGen;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractAtomicVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.ffi.CRFFI;
import com.oracle.truffle.r.runtime.ffi.NativeCallInfo;

import sun.misc.Unsafe;

public class TruffleNFI_C implements CRFFI {

    @MessageResolution(receiverType = StringWrapper.class)
    public static class StringWrapperMR {

        @Resolve(message = "IS_POINTER")
        public abstract static class StringWrapperNativeIsPointerNode extends Node {
            protected Object access(@SuppressWarnings("unused") StringWrapper receiver) {
                return true;
            }
        }

        @Resolve(message = "AS_POINTER")
        public abstract static class StringWrapperNativeAsPointerNode extends Node {
            protected Object access(StringWrapper receiver) {
                return receiver.asPointer();
            }
        }

        @CanResolve
        public abstract static class StringWrapperCheck extends Node {

            protected static boolean test(TruffleObject receiver) {
                return receiver instanceof StringWrapper;
            }
        }
    }

    public static final class StringWrapper implements TruffleObject {

        private final RAbstractStringVector vector;
        private long address;

        public StringWrapper(RAbstractStringVector vector) {
            this.vector = vector;
        }

        @Override
        public ForeignAccess getForeignAccess() {
            return StringWrapperMRForeign.ACCESS;
        }

        public long asPointer() {
            if (address == 0) {
                address = allocate();
            }
            return address;
        }

        @TruffleBoundary
        private long allocate() {
            int length = vector.getLength();
            int size = length * 8;
            byte[][] bytes = new byte[length][];
            for (int i = 0; i < length; i++) {
                String element = vector.getDataAt(i);
                bytes[i] = element.getBytes(StandardCharsets.US_ASCII);
                size += bytes[i].length + 1;
            }
            long memory = UnsafeAdapter.UNSAFE.allocateMemory(size);
            long ptr = memory + length * 8; // start of the actual character data
            for (int i = 0; i < length; i++) {
                UnsafeAdapter.UNSAFE.putLong(memory + i * 8, ptr);
                UnsafeAdapter.UNSAFE.copyMemory(bytes[i], Unsafe.ARRAY_BYTE_BASE_OFFSET, null, ptr, bytes[i].length);
                ptr += bytes[i].length;
                UnsafeAdapter.UNSAFE.putByte(ptr++, (byte) 0);
            }
            assert ptr == memory + size : "should have filled everything";
            return memory;
        }

        public RAbstractStringVector copyBack(RAbstractStringVector original) {
            if (address == 0) {
                return original;
            } else {
                RStringVector result = original.materialize();
                String[] data = result.isTemporary() ? result.getDataWithoutCopying() : result.getDataCopy();
                for (int i = 0; i < data.length; i++) {
                    long ptr = UnsafeAdapter.UNSAFE.getLong(address + i * 8);
                    int length = 0;
                    while (UnsafeAdapter.UNSAFE.getByte(ptr + length) != 0) {
                        length++;
                    }
                    byte[] bytes = new byte[length];
                    UnsafeAdapter.UNSAFE.copyMemory(null, ptr, bytes, Unsafe.ARRAY_BYTE_BASE_OFFSET, length);
                    data[i] = new String(bytes, StandardCharsets.US_ASCII);
                }
                UnsafeAdapter.UNSAFE.freeMemory(address);
                return RDataFactory.createStringVector(data, true);
            }
        }
    }

    abstract static class TruffleNFI_InvokeCNode extends InvokeCNode {

        @Child private Node bindNode = Message.createInvoke(1).createNode();

        @Override
        protected Object getNativeArgument(int index, ArgumentType type, RAbstractAtomicVector vector) {
            if (type == ArgumentType.VECTOR_STRING) {
                return new StringWrapper((RAbstractStringVector) vector);
            } else {
                return super.getNativeArgument(index, type, vector);
            }
        }

        @Override
        protected Object postProcessArgument(ArgumentType type, RAbstractAtomicVector vector, Object nativeArgument) {
            if (type == ArgumentType.VECTOR_STRING) {
                return ((StringWrapper) nativeArgument).copyBack((RAbstractStringVector) vector);
            } else {
                return super.postProcessArgument(type, vector, nativeArgument);
            }
        }

        @Specialization(guards = "args.length == 0")
        protected void invokeCall0(NativeCallInfo nativeCallInfo, @SuppressWarnings("unused") Object[] args, @SuppressWarnings("unused") boolean hasStrings,
                        @Cached("createExecute(args.length)") Node executeNode) {
            synchronized (TruffleNFI_Call.class) {
                try {
                    TruffleObject callFunction = (TruffleObject) ForeignAccess.sendInvoke(bindNode, nativeCallInfo.address.asTruffleObject(), "bind", "(): void");
                    ForeignAccess.sendExecute(executeNode, callFunction);
                } catch (InteropException ex) {
                    throw RInternalError.shouldNotReachHere(ex);
                }
            }
        }

        @Specialization(limit = "99", guards = "args.length == cachedArgsLength")
        protected void invokeCall1(NativeCallInfo nativeCallInfo, Object[] args, @SuppressWarnings("unused") boolean hasStrings,
                        @Cached("args.length") int cachedArgsLength,
                        @Cached("createExecute(cachedArgsLength)") Node executeNode) {
            synchronized (TruffleNFI_Call.class) {
                try {
                    Object[] nargs = new Object[cachedArgsLength];
                    TruffleObject callFunction = (TruffleObject) ForeignAccess.sendInvoke(bindNode, nativeCallInfo.address.asTruffleObject(), "bind", getSignature(args, nargs));
                    ForeignAccess.sendExecute(executeNode, callFunction, nargs);
                } catch (InteropException ex) {
                    throw RInternalError.shouldNotReachHere(ex);
                }
            }
        }

        public static Node createExecute(int n) {
            return Message.createExecute(n).createNode();
        }
    }

    @TruffleBoundary
    private static String getSignature(Object[] args, Object[] nargs) {
        StringBuilder sb = new StringBuilder();
        sb.append('(');
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if (arg instanceof int[]) {
                sb.append("[sint32]");
            } else if (arg instanceof double[]) {
                sb.append("[double]");
            } else if (arg instanceof StringWrapper) {
                sb.append("pointer");
            } else {
                throw RInternalError.unimplemented(".C type: " + arg.getClass().getSimpleName());
            }
            nargs[i] = JavaInterop.asTruffleObject(arg);
            if (i < args.length - 1) {
                sb.append(", ");
            }
        }
        sb.append("): void");
        return sb.toString();
    }

    @Override
    public InvokeCNode createInvokeCNode() {
        return TruffleNFI_InvokeCNodeGen.create();
    }
}
