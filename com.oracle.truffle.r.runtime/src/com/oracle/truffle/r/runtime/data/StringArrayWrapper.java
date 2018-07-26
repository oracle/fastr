/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.interop.CanResolve;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.MessageResolution;
import com.oracle.truffle.api.interop.Resolve;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.ffi.interop.NativeCharArray;

@MessageResolution(receiverType = StringArrayWrapper.class)
final class StringArrayWrapperMR {

    @Resolve(message = "READ")
    public abstract static class StringArrayWrapperReadNode extends Node {
        protected Object access(StringArrayWrapper receiver, int index) {
            return receiver.getNativeCharArray(index);
        }

        protected Object access(StringArrayWrapper receiver, long index) {
            return receiver.getNativeCharArray((int) index);
        }
    }

    @Resolve(message = "HAS_SIZE")
    public abstract static class StringArrayWrapperHasSizeNode extends Node {
        protected boolean access(@SuppressWarnings("unused") StringArrayWrapper receiver) {
            return true;
        }
    }

    @Resolve(message = "GET_SIZE")
    public abstract static class StringArrayWrapperGetSizeNode extends Node {
        protected int access(StringArrayWrapper receiver) {
            return receiver.getLength();
        }
    }

    @Resolve(message = "IS_POINTER")
    public abstract static class StringArrayWrapperIsPointerNode extends Node {
        protected Object access(@SuppressWarnings("unused") StringArrayWrapper receiver) {
            return true;
        }
    }

    @Resolve(message = "AS_POINTER")
    public abstract static class StringArrayWrapperAsPointerNode extends Node {
        private final ConditionProfile profile = ConditionProfile.createBinaryProfile();

        protected Object access(StringArrayWrapper receiver) {
            long address = receiver.address;
            if (profile.profile(address == 0)) {
                return receiver.asPointer();
            }
            return address;
        }
    }

    @CanResolve
    public abstract static class StringArrayWrapperCheck extends Node {
        protected static boolean test(TruffleObject receiver) {
            return receiver instanceof StringArrayWrapper;
        }
    }
}

public final class StringArrayWrapper implements TruffleObject {

    long address;
    private final RStringVector vector;
    private NativeCharArray[] nativeCharArrays = null;

    public StringArrayWrapper(RStringVector vector) {
        this.vector = vector;
    }

    @Override
    public ForeignAccess getForeignAccess() {
        return StringArrayWrapperMRForeign.ACCESS;
    }

    public long asPointer() {
        address = NativeDataAccess.allocateNativeStringArray(vector.getReadonlyStringData());
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
