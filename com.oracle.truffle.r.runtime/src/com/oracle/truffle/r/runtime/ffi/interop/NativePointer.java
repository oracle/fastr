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

import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RTruffleObject;

/**
 * Created when a {@link RTruffleObject} subclass has no meaningful native representation,
 * nevertheless a {@code Message#TO_NATIVE} message is sent to it.
 */
public abstract class NativePointer implements TruffleObject {

    /**
     * This is used when an {@link RNull} is stored in memory (LLVM).
     */
    private static final class NullNativePointer extends NativePointer {
        private NullNativePointer() {
            super(RNull.instance);
        }

        @Override
        protected long asPointerImpl() {
            return 0;
        }
    }

    public static final NullNativePointer NULL_NATIVEPOINTER = new NullNativePointer();

    final RTruffleObject object;

    protected NativePointer(RTruffleObject object) {
        this.object = object;
    }

    @Override
    public ForeignAccess getForeignAccess() {
        return NativePointerMRForeign.ACCESS;
    }

    public static boolean isInstance(TruffleObject obj) {
        return obj instanceof NativePointer;
    }

    final long asPointer() {
        return asPointerImpl();
    }

    protected abstract long asPointerImpl();
}
