/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.ffi.util;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

/**
 * A wrapper for a native array ie. a pointer to a valid address.
 */
@ExportLibrary(InteropLibrary.class)
public abstract class NativeArrayWrapper implements TruffleObject {
    private final int size;
    private final long arrayPtr;

    private NativeArrayWrapper(long arrayPtr, int size) {
        this.size = size;
        this.arrayPtr = arrayPtr;
    }

    /**
     * Creates a wrapper for integer native array.
     * 
     * @param arrayPtr Address to the array.
     */
    public static NativeArrayWrapper createIntWrapper(long arrayPtr, int size) {
        return new NativeIntArrayWrapper(arrayPtr, size);
    }

    /**
     * Creates a wrapper for double native array.
     * 
     * @param arrayPtr Address to the array.
     */
    public static NativeArrayWrapper createDoubleWrapper(long arrayPtr, int size) {
        return new NativeDoubleArrayWrapper(arrayPtr, size);
    }

    public abstract void writeElement(long arrayAddr, long index, Object value);

    public abstract Object readElement(long arrayAddr, long index);

    @ExportMessage
    public boolean hasArrayElements() {
        return true;
    }

    @ExportMessage
    public boolean isPointer() {
        return true;
    }

    @ExportMessage
    public long asPointer() {
        return arrayPtr;
    }

    @ExportMessage
    public void writeArrayElement(long index, Object value) {
        writeElement(arrayPtr, index, value);
    }

    @ExportMessage
    public Object readArrayElement(long index) {
        return readElement(arrayPtr, index);
    }

    @ExportMessage
    public void removeArrayElement(@SuppressWarnings("unused") long index) throws UnsupportedMessageException {
        throw UnsupportedMessageException.create();
    }

    @ExportMessage
    public long getArraySize() {
        return size;
    }

    @ExportMessage(name = "isArrayElementModifiable")
    @ExportMessage(name = "isArrayElementReadable")
    public boolean isArrayElementModifiable(long index) {
        return 0 <= index && index < size;
    }

    @ExportMessage
    public boolean isArrayElementInsertable(@SuppressWarnings("unused") long index) {
        return false;
    }

    @ExportMessage
    public boolean isArrayElementRemovable(@SuppressWarnings("unused") long index) {
        return false;
    }

    private static final class NativeIntArrayWrapper extends NativeArrayWrapper {
        protected NativeIntArrayWrapper(long arrayPtr, int size) {
            super(arrayPtr, size);
        }

        @Override
        public void writeElement(long arrayPtr, long index, Object value) {
            NativeMemory.putInt(arrayPtr, index, (int) value);
        }

        @Override
        public Object readElement(long arrayPtr, long index) {
            return NativeMemory.getInt(arrayPtr, index);
        }
    }

    private static final class NativeDoubleArrayWrapper extends NativeArrayWrapper {
        protected NativeDoubleArrayWrapper(long arrayPtr, int size) {
            super(arrayPtr, size);
        }

        @Override
        public void writeElement(long arrayAddr, long index, Object value) {
            NativeMemory.putDouble(arrayAddr, index, (double) value);
        }

        @Override
        public Object readElement(long arrayAddr, long index) {
            return NativeMemory.getDouble(arrayAddr, index);
        }
    }
}
