/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.ffi.impl.interop;

import static com.oracle.truffle.r.ffi.impl.interop.UnsafeAdapter.UNSAFE;
import com.oracle.truffle.r.runtime.data.RVector;

/**
 * Handles the {@code complete} flag in an {@link RVector} when an {@code NA} value is assigned in
 * native code.
 *
 */
public abstract class NativeNACheck<T> implements AutoCloseable {

    private final RVector<?> vec;

    /**
     * If the array escapes the Truffle world via {@link #convertToNative()}, this value will be
     * non-zero and is used exclusively thereafter.
     */
    protected long nativeAddress;

    protected NativeNACheck(Object x) {
        if (x instanceof RVector<?>) {
            vec = (RVector<?>) x;
        } else {
            // scalar (length 1) vector or no associated R object
            vec = null;
        }
    }

    public void setIncomplete() {
        if (vec != null) {
            vec.setComplete(false);
        }
    }

    protected abstract void allocateNative();

    protected abstract void copyBackFromNative();

    final long convertToNative() {
        if (nativeAddress == 0) {
            allocateNative();
        }
        return nativeAddress;
    }

    @Override
    public final void close() {
        if (nativeAddress != 0) {
            copyBackFromNative();
            UNSAFE.freeMemory(nativeAddress);
        }
    }
}
