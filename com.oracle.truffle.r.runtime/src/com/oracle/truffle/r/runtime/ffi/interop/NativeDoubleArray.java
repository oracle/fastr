/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.runtime.ffi.interop.UnsafeAdapter.UNSAFE;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.library.ExportMessage.Ignore;
import com.oracle.truffle.llvm.spi.NativeTypeLibrary;
import com.oracle.truffle.r.runtime.RRuntime;

import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.TruffleRLanguage;
import sun.misc.Unsafe;

@ExportLibrary(InteropLibrary.class)
@ExportLibrary(NativeTypeLibrary.class)
public final class NativeDoubleArray extends NativeArray<double[]> {

    public NativeDoubleArray(double[] value) {
        super(value);
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public boolean hasNativeType() {
        return true;
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public Object getNativeType(@CachedContext(TruffleRLanguage.class) RContext ctx) {
        return ctx.getRFFI().getSulongArrayType(42.42);
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public boolean isPointer() {
        return nativeAddress() != 0;
    }

    @ExportMessage
    public long asPointer() {
        long na = nativeAddress();
        assert na != 0L : "toNative() expected to be called first";
        return na;
    }

    @ExportMessage
    public void toNative() {
        convertToNative();
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    boolean hasArrayElements() {
        return true;
    }

    @ExportMessage
    long getArraySize() {
        return array.length;
    }

    @ExportMessage
    boolean isArrayElementReadable(long index) {
        return index >= 0 && index < getArraySize();
    }

    @ExportMessage
    Object readArrayElement(long index) {
        return read(RRuntime.interopArrayIndexToInt(index, this));
    }

    @ExportMessage
    boolean isArrayElementModifiable(long index) {
        return index >= 0 && index < getArraySize();
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    boolean isArrayElementInsertable(@SuppressWarnings("unused") long index) {
        return false;
    }

    @Ignore
    void writeArrayElement(long index, Object value) throws UnsupportedMessageException, UnsupportedTypeException, InvalidArrayIndexException {
        InteropLibrary.getFactory().getUncached().writeArrayElement(this, index, value);
    }

    @ExportMessage
    static class WriteArrayElement {

        @Specialization
        static void withoutClosure(NativeDoubleArray arr, long index, Long value) {
            arr.write(RRuntime.interopArrayIndexToInt(index, arr), value);
        }

        @Specialization
        static void withClosure(NativeDoubleArray arr, long index, Integer value) {
            arr.write(RRuntime.interopArrayIndexToInt(index, arr), value);
        }

        @Specialization
        static void withClosure(NativeDoubleArray arr, long index, Double value) {
            arr.write(RRuntime.interopArrayIndexToInt(index, arr), value);
        }
    }

    double read(int index) {
        long nativeAddress = nativeAddress();
        if (nativeAddress != 0) {
            return UNSAFE.getDouble(nativeAddress + index * Unsafe.ARRAY_DOUBLE_INDEX_SCALE);
        } else {
            return array[index];
        }
    }

    void write(int index, double nv) {
        long nativeAddress = nativeAddress();
        if (nativeAddress != 0) {
            UNSAFE.putDouble(nativeAddress + index * Unsafe.ARRAY_DOUBLE_INDEX_SCALE, nv);
        } else {
            array[index] = nv;
        }
    }

    @Override
    @TruffleBoundary
    protected long allocateNative() {
        long nativeAddress = UNSAFE.allocateMemory(array.length * Unsafe.ARRAY_DOUBLE_INDEX_SCALE);
        UNSAFE.copyMemory(array, Unsafe.ARRAY_DOUBLE_BASE_OFFSET, null, nativeAddress, array.length * Unsafe.ARRAY_DOUBLE_INDEX_SCALE);
        return nativeAddress;
    }

    @Override
    @TruffleBoundary
    protected void copyBackFromNative(long nativeAddress) {
        // copy back
        UNSAFE.copyMemory(null, nativeAddress, array, Unsafe.ARRAY_DOUBLE_BASE_OFFSET, array.length * Unsafe.ARRAY_DOUBLE_INDEX_SCALE);
    }

}
