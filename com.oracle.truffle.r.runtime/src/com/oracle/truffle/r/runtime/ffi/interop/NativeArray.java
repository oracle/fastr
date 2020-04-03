/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.ffi.interop;

import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.llvm.spi.NativeTypeLibrary;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.TruffleRLanguage;
import com.oracle.truffle.r.runtime.data.RTruffleObject;
import com.oracle.truffle.r.runtime.ffi.util.NativeMemory.NativeMemoryWrapper;

/**
 * Wraps a Java array to make it look like an interop and native array. These objects can be sent to
 * LLVM/NFI and they will mimic corresponding native array type. To make any changes made to this
 * object either via interop or via pointer visible in the Java array, call {@link #refresh()}.
 * Note: if you do not call {@link #refresh()} the changes may or may not be visible in the Java
 * array.
 */
@ExportLibrary(InteropLibrary.class)
@ExportLibrary(NativeTypeLibrary.class)
public abstract class NativeArray implements RTruffleObject {
    protected NativeMemoryWrapper nativeMirror;

    @SuppressWarnings("static-method")
    @ExportMessage
    public final boolean hasNativeType() {
        return true;
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public final Object getNativeType(@CachedContext(TruffleRLanguage.class) RContext ctx) {
        return getSulongArrayType(ctx);
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public final boolean isPointer() {
        return nativeAddress() != null;
    }

    @ExportMessage
    public final long asPointer() throws UnsupportedMessageException {
        if (nativeMirror != null) {
            return nativeMirror.getAddress();
        }
        throw UnsupportedMessageException.create();
    }

    @ExportMessage
    protected static class ToNative {
        @Specialization(guards = "receiver.hasNativeAddress()")
        static void doNothing(@SuppressWarnings("unused") NativeArray receiver) {
        }

        @Specialization(guards = "!receiver.hasNativeAddress()")
        static void doToNative(NativeArray receiver) {
            receiver.nativeMirror = receiver.allocateNative();
        }
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public final boolean hasArrayElements() {
        return true;
    }

    @ExportMessage
    public final long getArraySize() {
        return getArrayLength();
    }

    @ExportMessage
    public final boolean isArrayElementReadable(long index) {
        return index >= 0 && index < getArraySize();
    }

    @ExportMessage
    public final boolean isArrayElementModifiable(long index) {
        return index >= 0 && index < getArraySize();
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public final boolean isArrayElementInsertable(@SuppressWarnings("unused") long index) {
        return false;
    }

    @ExportMessage
    protected static class ReadArrayElement {
        @Specialization(guards = "receiver.hasNativeAddress()")
        static Object readFromNative(NativeArray receiver, long index) {
            return receiver.readFromNative(receiver.nativeMirror, RRuntime.interopArrayIndexToInt(index, receiver));
        }

        @Specialization(guards = "!receiver.hasNativeAddress()")
        static Object readFromArray(NativeArray receiver, long index) {
            return receiver.readFromArray(RRuntime.interopArrayIndexToInt(index, receiver));
        }
    }

    @ExportMessage
    protected static class WriteArrayElement {
        @Specialization(guards = "receiver.hasNativeAddress()")
        static void writeToNative(NativeArray receiver, long index, Object value) {
            receiver.writeToNative(receiver.nativeMirror, RRuntime.interopArrayIndexToInt(index, receiver), value);
        }

        @Specialization(guards = "!receiver.hasNativeAddress()")
        static void writeToArray(NativeArray receiver, long index, Object value) {
            receiver.writeToArray(RRuntime.interopArrayIndexToInt(index, receiver), value);
        }
    }

    protected abstract int getArrayLength();

    protected abstract Object getArray();

    protected abstract Object readFromNative(NativeMemoryWrapper nativeAddress, int index);

    protected abstract Object readFromArray(int index);

    protected abstract void writeToNative(NativeMemoryWrapper nativeAddress, int index, Object value);

    protected abstract void writeToArray(int index, Object value);

    protected abstract NativeMemoryWrapper allocateNative();

    protected abstract void copyBackFromNative(NativeMemoryWrapper nativeAddress);

    protected abstract Object getSulongArrayType(RContext ctx);

    protected final boolean hasNativeAddress() {
        return nativeMirror != null;
    }

    protected final NativeMemoryWrapper nativeAddress() {
        return nativeMirror;
    }

    public final Object refresh() {
        if (nativeMirror != null) {
            copyBackFromNative(nativeMirror);
        }
        return getArray();
    }
}
