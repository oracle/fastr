/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.runtime.ffi.UnsafeAdapter.UNSAFE;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

import sun.misc.Unsafe;

/**
 * Parent class of {@link NativeRawArray} and {@link NativeCharArray}, that holds the common logic
 * for a C type {@code uint8*}, that may or may not be {@code NULL} terminated (in the C domain),
 * and may escape into the native domain via an UNBOX message.
 *
 * N.B. Java never stores a {@code NULL} value in a String or the byte array from
 * {@link String#getBytes}.
 *
 * If {@link #fakesNullTermination()} is {@code true}, then {@link #read} returns 0, else it is an
 * error; similar for {@link #write}.
 */
public abstract class NativeUInt8Array extends NativeArray<byte[]> {

    private int effectiveLength;

    protected NativeUInt8Array(byte[] array, boolean nullTerminate) {
        super(array);
        this.effectiveLength = array.length + (nullTerminate ? 1 : 0);
    }

    public boolean fakesNullTermination() {
        return array.length != effectiveLength;
    }

    private void checkNativeIndex(int index) {
        if (index < 0 || index >= effectiveLength) {
            throw new ArrayIndexOutOfBoundsException(index);
        }
    }

    void write(int index, byte value) {
        long nativeAddress = nativeAddress();
        if (nativeAddress != 0) {
            checkNativeIndex(index);
            UNSAFE.putByte(nativeAddress + index, value);
        } else {
            if (index == array.length && fakesNullTermination()) {
                // ignore
            } else {
                array[index] = value;
            }
        }
    }

    byte read(int index) {
        long nativeAddress = nativeAddress();
        if (nativeAddress != 0) {
            checkNativeIndex(index);
            return UNSAFE.getByte(nativeAddress + index);
        } else {
            if (index == array.length && fakesNullTermination()) {
                return (byte) 0;
            }
            return array[index];
        }
    }

    int getSize() {
        return array.length;
    }

    @TruffleBoundary
    @Override
    protected final long allocateNative() {
        long nativeAddress = UNSAFE.allocateMemory(effectiveLength);
        UNSAFE.copyMemory(array, Unsafe.ARRAY_BYTE_BASE_OFFSET, null, nativeAddress, array.length);
        if (fakesNullTermination()) {
            UNSAFE.putByte(nativeAddress + array.length, (byte) 0);
        }
        return nativeAddress;
    }

    public byte[] getValue() {
        return refresh();
    }

    public void setValue(byte[] newBytes, boolean isNullTerminated) {
        array = newBytes;
        effectiveLength = isNullTerminated ? array.length + 1 : array.length;
    }

    @TruffleBoundary
    @Override
    protected void copyBackFromNative(long nativeAddress) {
        // copy back
        UNSAFE.copyMemory(null, nativeAddress, array, Unsafe.ARRAY_BYTE_BASE_OFFSET, array.length);
    }

}
