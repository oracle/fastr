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
package com.oracle.truffle.r.runtime.ffi.interop;

import com.oracle.truffle.r.runtime.data.NativeDataAccess;
import com.oracle.truffle.r.runtime.data.RTruffleObject;
import static com.oracle.truffle.r.runtime.ffi.interop.UnsafeAdapter.UNSAFE;

import java.lang.ref.WeakReference;

public abstract class NativeArray<T> implements RTruffleObject {

    protected T array;
    @SuppressWarnings("unused") private NativeMirror<T> nativeMirror;

    protected NativeArray(T array) {
        this.array = array;
    }

    protected abstract long allocateNative();

    protected abstract void copyBackFromNative(long nativeAddress);

    protected long nativeAddress() {
        return (nativeMirror != null) ? nativeMirror.nativeAddress : 0L;
    }

    final long convertToNative() {
        if (nativeMirror == null) {
            nativeMirror = new NativeMirror<>(this);
        }
        return nativeMirror.nativeAddress;
    }

    public final T refresh() {
        if (nativeMirror != null) {
            copyBackFromNative(nativeMirror.nativeAddress);
        }
        return array;
    }

    private static final class NativeMirror<T> extends WeakReference<NativeArray<T>> implements NativeDataAccess.Releasable {

        /**
         * If the array escapes the Truffle world via {@link #convertToNative()}, this value will be
         * non-zero and is used exclusively thereafter.
         */
        final long nativeAddress;

        NativeMirror(NativeArray<T> owner) {
            super(owner, NativeDataAccess.nativeReferenceQueue());
            nativeAddress = owner.allocateNative();
            assert nativeAddress != 0;
        }

        @Override
        public void release() {
            UNSAFE.freeMemory(nativeAddress);
        }
    }
}
