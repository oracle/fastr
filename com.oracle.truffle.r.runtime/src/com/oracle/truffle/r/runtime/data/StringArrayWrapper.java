/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.data;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.r.runtime.ffi.interop.NativeCharArray;
import com.oracle.truffle.r.runtime.ffi.util.NativeMemory;
import com.oracle.truffle.r.runtime.ffi.util.NativeMemory.NativeMemoryWrapper;

@ExportLibrary(InteropLibrary.class)
public final class StringArrayWrapper implements TruffleObject {

    private NativeMemoryWrapper nativeMemory;
    private final RStringVector vector;
    private NativeCharArray[] nativeCharArrays = null;

    public StringArrayWrapper(RStringVector vector) {
        this.vector = vector;
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    boolean hasArrayElements() {
        return true;
    }

    @ExportMessage
    long getArraySize() {
        return getLength();
    }

    @ExportMessage
    boolean isArrayElementReadable(long idx) {
        return idx > 0 && idx < getLength();
    }

    @ExportMessage
    public Object readArrayElement(long index) {
        return getNativeCharArray((int) index);
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public boolean isPointer() {
        return nativeMemory != null;
    }

    @ExportMessage
    public void toNative() {
        assert nativeMemory == null;
        long address = NativeDataAccess.allocateNativeStringArray(vector.getReadonlyStringData());
        nativeMemory = NativeMemory.wrapNativeMemory(address, this);
    }

    @ExportMessage
    public long asPointer() {
        assert nativeMemory != null;
        return nativeMemory.getAddress();
    }

    public long getAddress() {
        return nativeMemory.getAddress();
    }

    public RStringVector copyBackFromNative() {
        if (nativeMemory == null) {
            if (nativeCharArrays != null) {
                String[] contents = new String[vector.getLength()];
                for (int i = 0; i < contents.length; i++) {
                    NativeCharArray nativeCharArray = nativeCharArrays[i];
                    if (nativeCharArray == null) {
                        contents[i] = vector.getDataAt(i);
                    } else {
                        contents[i] = nativeCharArray.getString();
                    }
                }
                RStringVector copy = new RStringVector(contents, false);
                copy.copyAttributesFrom(vector);
                return copy;
            } else {
                return vector;
            }
        } else {
            try {
                String[] contents = NativeDataAccess.copyBackNativeStringArray(nativeMemory.getAddress(), vector.getLength());
                RStringVector copy = new RStringVector(contents, false);
                copy.copyAttributesFrom(vector);
                return copy;
            } finally {
                nativeMemory.release();
            }
        }
    }

    public int getLength() {
        return vector.getLength();
    }

    public NativeCharArray getNativeCharArray(int index) {
        if (nativeCharArrays == null) {
            nativeCharArrays = new NativeCharArray[vector.getLength()];
        }
        NativeCharArray nativeCharArray = nativeCharArrays[index];
        if (nativeCharArray == null) {
            nativeCharArray = new NativeCharArray(vector.getDataAt(index));
            nativeCharArrays[index] = nativeCharArray;
        }
        return nativeCharArray;
    }
}
