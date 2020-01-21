/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.library.ExportMessage.Ignore;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.context.RContext;

import com.oracle.truffle.r.runtime.ffi.util.NativeMemory;
import sun.misc.Unsafe;

@ExportLibrary(InteropLibrary.class)
public final class NativeIntegerArray extends NativeArray<int[]> {

    public NativeIntegerArray(int[] value) {
        super(value);
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
        static void withoutClosure(NativeIntegerArray arr, long index, Long value) {
            arr.write(RRuntime.interopArrayIndexToInt(index, arr), value.intValue());
        }

        @Specialization
        static void withClosure(NativeIntegerArray arr, long index, Integer value) {
            arr.write(RRuntime.interopArrayIndexToInt(index, arr), value);
        }

        @Specialization
        static void withClosure(NativeIntegerArray arr, long index, Double value) {
            arr.write(RRuntime.interopArrayIndexToInt(index, arr), value.intValue());
        }
    }

    int read(int index) {
        long nativeAddress = nativeAddress();
        if (nativeAddress != 0) {
            return UNSAFE.getInt(nativeAddress + index * Unsafe.ARRAY_INT_INDEX_SCALE);
        } else {
            return array[index];
        }
    }

    void write(int index, int nv) {
        long nativeAddress = nativeAddress();
        if (nativeAddress != 0) {
            UNSAFE.putInt(nativeAddress + index * Unsafe.ARRAY_INT_INDEX_SCALE, nv);
        } else {
            array[index] = nv;
        }
    }

    @Override
    @TruffleBoundary
    protected long allocateNative() {
        long nativeAddress = NativeMemory.allocate(array.length * Unsafe.ARRAY_INT_INDEX_SCALE, "NativeIntegerArray");
        UNSAFE.copyMemory(array, Unsafe.ARRAY_INT_BASE_OFFSET, null, nativeAddress, array.length * Unsafe.ARRAY_INT_INDEX_SCALE);
        return nativeAddress;
    }

    @Override
    @TruffleBoundary
    protected void copyBackFromNative(long nativeAddress) {
        // copy back
        UNSAFE.copyMemory(null, nativeAddress, array, Unsafe.ARRAY_INT_BASE_OFFSET, array.length * Unsafe.ARRAY_INT_INDEX_SCALE);
    }

    @Override
    protected Object getSulongArrayType(RContext ctx) {
        return ctx.getRFFI().getSulongArrayType(42);
    }

}
