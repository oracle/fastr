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

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.ffi.interop.NativeCharArray;

@ExportLibrary(InteropLibrary.class)
public final class StringArrayWrapper implements TruffleObject {

    long address;
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
        return address != 0;
    }

    @ExportMessage
    public void toNative() {
        assert address == 0;
        address = NativeDataAccess.allocateNativeStringArray(vector.getReadonlyStringData());
    }

    @ExportMessage
    public long asPointer(@Cached("createBinaryProfile()") ConditionProfile isPointer) throws UnsupportedMessageException {
        if (isPointer.profile(address != 0)) {
            return address;
        }
        throw UnsupportedMessageException.create();
    }

    public long getAddress() {
        return address;
    }

    public RStringVector copyBackFromNative() {
        if (address == 0) {
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
                address = 0;
                RStringVector copy = new RStringVector(contents, false);
                copy.copyAttributesFrom(vector);
                return copy;
            } else {
                return vector;
            }
        } else {
            String[] contents = NativeDataAccess.releaseNativeStringArray(address, vector.getLength());
            address = 0;
            RStringVector copy = new RStringVector(contents, false);
            copy.copyAttributesFrom(vector);
            return copy;
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
