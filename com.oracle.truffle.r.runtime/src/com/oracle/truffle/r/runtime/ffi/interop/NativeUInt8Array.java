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

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.llvm.spi.NativeTypeLibrary;

import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.ffi.util.NativeMemory;
import com.oracle.truffle.r.runtime.ffi.util.NativeMemory.ElementType;
import com.oracle.truffle.r.runtime.ffi.util.NativeMemory.NativeMemoryWrapper;

/**
 * Parent class of {@link NativeRawArray} and {@link NativeCharArray}, that holds the common logic
 * for a C type {@code uint8*}, that may or may not be {@code NULL} terminated (in the C domain).
 *
 * The null termination is faked for Java arrays. If this object escapes to native code and we
 * allocate native memory for it, then the native memory will be null terminated (and one byte
 * longer).
 */
@ExportLibrary(InteropLibrary.class)
@ExportLibrary(NativeTypeLibrary.class)
public abstract class NativeUInt8Array extends NativeArray {

    private byte[] array;
    private int effectiveLength;

    public NativeUInt8Array(long address, int length) {
        this.array = new byte[length];
        this.effectiveLength = length;
        this.nativeMirror = NativeMemory.wrapExternalNativeMemory(address, this);
        refresh();
    }

    protected NativeUInt8Array(byte[] array, boolean nullTerminate) {
        this.array = array;
        this.effectiveLength = array.length + (nullTerminate ? 1 : 0);
    }

    public boolean fakesNullTermination() {
        return array.length != effectiveLength;
    }

    @Override
    protected Object getArray() {
        return array;
    }

    public byte[] getByteArray() {
        return array;
    }

    @Override
    protected int getArrayLength() {
        return effectiveLength;
    }

    @Override
    protected void writeToNative(NativeMemoryWrapper nativeAddress, int index, Object value) {
        NativeMemory.putByte(nativeAddress, index, (Byte) value);
    }

    @Override
    protected void writeToArray(int index, Object value) {
        if (!(index == array.length && fakesNullTermination())) {
            array[index] = (byte) value;
        }
        // otherwise ignore overwrite of the terminating zero, maybe warn?
    }

    @Override
    protected Object readFromNative(NativeMemoryWrapper nativeAddress, int index) {
        return NativeMemory.getByte(nativeAddress, index);
    }

    @Override
    protected Object readFromArray(int index) {
        if (index == array.length && fakesNullTermination()) {
            return (byte) 0;
        }
        return array[index];
    }

    @Override
    protected final NativeMemoryWrapper allocateNative() {
        long ptr = NativeMemory.allocate(effectiveLength, "NativeUInt8Array");
        NativeMemoryWrapper nativeAddress = NativeMemory.wrapNativeMemory(ptr, this);
        NativeMemory.copyMemory(array, nativeAddress, ElementType.BYTE, array.length);
        if (fakesNullTermination()) {
            NativeMemory.putByte(nativeAddress, array.length, (byte) 0);
        }
        return nativeAddress;
    }

    public byte[] getValue() {
        refresh();
        return array;
    }

    public void setValue(byte[] newBytes, boolean isNullTerminated) {
        array = newBytes;
        effectiveLength = isNullTerminated ? array.length + 1 : array.length;
    }

    @Override
    protected void copyBackFromNative(NativeMemoryWrapper nativeAddress) {
        NativeMemory.copyMemory(nativeAddress, array, ElementType.BYTE, array.length);
    }

    @Override
    protected Object getSulongArrayType(RContext ctx) {
        return ctx.getRFFI().getSulongArrayType((byte) 42);
    }
}
