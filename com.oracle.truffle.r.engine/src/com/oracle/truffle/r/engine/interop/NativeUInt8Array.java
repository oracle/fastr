/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.engine.interop;

import static com.oracle.truffle.r.engine.interop.UnsafeAdapter.UNSAFE;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.r.runtime.data.RTruffleObject;

import sun.misc.Unsafe;

/**
 * Parent class of {@link NativeRawArray} and {@link NativeCharArray}, that holds the common logic
 * for a C type {@code uint8*}, that may or may not be {@code NULL} terminated (in the C domain),
 * and may escape into the native domain via an UNBOX message.
 *
 * N.B. Java never stores a {@code NULL} value in a String or the byte array from
 * {@link String#getBytes}.
 *
 * If {@link #fakeNull()} is {@code true}, then {@link #read} returns 0, else it is an error;
 * similar for {@link #write}.
 */
public abstract class NativeUInt8Array implements RTruffleObject {
    public final byte[] bytes;

    /**
     * If the array escapes the Truffle world via {@link #convertToNative()}, this value will be
     * non-zero and is used exclusively thereafter.
     */
    @CompilationFinal protected long nativeAddress;
    private final int effectiveLength;

    protected NativeUInt8Array(byte[] bytes, boolean nullTerminate) {
        this.bytes = bytes;
        this.effectiveLength = bytes.length + (nullTerminate ? 1 : 0);
    }

    private boolean fakeNull() {
        return bytes.length != effectiveLength;
    }

    private void checkNativeIndex(int index) {
        if (index < 0 || index >= effectiveLength) {
            throw new ArrayIndexOutOfBoundsException(index);
        }
    }

    void write(int index, byte value) {
        if (nativeAddress != 0) {
            checkNativeIndex(index);
            UNSAFE.putByte(nativeAddress + index, value);
        } else {
            if (index == bytes.length && fakeNull()) {
                // ignore
            } else {
                bytes[index] = value;
            }
        }
    }

    byte read(int index) {
        if (nativeAddress != 0) {
            checkNativeIndex(index);
            return UNSAFE.getByte(nativeAddress + index);
        } else {
            if (index == bytes.length && fakeNull()) {
                return (byte) 0;
            }
            return bytes[index];
        }
    }

    int getSize() {
        return bytes.length;
    }

    long convertToNative() {
        if (nativeAddress == 0) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            nativeAddress = UNSAFE.allocateMemory(effectiveLength);
            UNSAFE.copyMemory(bytes, Unsafe.ARRAY_BYTE_BASE_OFFSET, null, nativeAddress, bytes.length);
            if (fakeNull()) {
                UNSAFE.putByte(nativeAddress + bytes.length, (byte) 0);
            }
        }
        return nativeAddress;
    }

    public byte[] getBytes() {
        if (nativeAddress != 0) {
            // copy back
            UNSAFE.copyMemory(null, nativeAddress, bytes, Unsafe.ARRAY_BYTE_BASE_OFFSET, bytes.length);
        }
        return bytes;
    }
}
