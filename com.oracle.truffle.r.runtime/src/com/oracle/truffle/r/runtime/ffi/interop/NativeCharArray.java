/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.charset.StandardCharsets;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

/**
 * A {@link TruffleObject} that represents an array of {@code unsigned char} values, that is
 * {@code NULL} terminated in the C domain.
 *
 * Note that {@link #getArrayLength()} returns effective length, which means that the length also
 * includes the terminating null.
 *
 * Beware of using {@code strlen} on instances of this class, as it will return different results on
 * LLVM and on NFI (on LLVM, the return value will include the terminating null). If you plan to use
 * an instance of this class as string in native code, pass along the length of the string to the
 * native, do not compute it in the native code via {@code strlen}.
 */
@ExportLibrary(InteropLibrary.class)
public final class NativeCharArray extends NativeUInt8Array {

    public NativeCharArray(byte[] bytes) {
        super(bytes, true);
    }

    @TruffleBoundary
    public NativeCharArray(String value) {
        super(value.getBytes(StandardCharsets.UTF_8), true);
    }

    private NativeCharArray(byte[] bytes, boolean nullTerminate) {
        super(bytes, nullTerminate);
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    boolean isString() {
        return true;
    }

    @ExportMessage
    @TruffleBoundary
    String asString() {
        return new String(getValue());
    }

    /**
     * Creates {@link NativeCharArray} of given length that can be used for output parameters, it is
     * not null terminated.
     */
    public static NativeCharArray crateOutputBuffer(int length) {
        return new NativeCharArray(new byte[length], false);
    }

    /**
     * Finds the null terminator and creates the Java String accordingly.
     */
    @TruffleBoundary
    public String getStringFromOutputBuffer() {
        assert !fakesNullTermination() : "create the buffer string via createOutputBuffer()";
        byte[] mbuf = getValue();
        int i = 0;
        while (i < mbuf.length && mbuf[i] != 0) {
            i++;
        }
        return new String(mbuf, 0, i);
    }

    @TruffleBoundary
    public String getString() {
        byte[] val = getValue();
        return new String(val, 0, fakesNullTermination() ? val.length : val.length - 1);
    }
}
