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

import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.ffi.util.NativeMemory;
import com.oracle.truffle.r.runtime.ffi.util.NativeMemory.ElementType;
import com.oracle.truffle.r.runtime.ffi.util.NativeMemory.NativeMemoryWrapper;

public final class NativeDoubleArray extends NativeArray {

    private final double[] array;

    public NativeDoubleArray(double[] value) {
        this.array = value;
    }

    @Override
    protected Object getArray() {
        return array;
    }

    @Override
    protected int getArrayLength() {
        return array.length;
    }

    @Override
    protected void writeToNative(NativeMemoryWrapper nativeAddress, int index, Object value) {
        NativeMemory.putDouble(nativeAddress, index, (Double) value);
    }

    @Override
    protected void writeToArray(int index, Object value) {
        array[index] = (double) value;
    }

    @Override
    protected Object readFromNative(NativeMemoryWrapper nativeAddress, int index) {
        return NativeMemory.getDouble(nativeAddress, index);
    }

    @Override
    protected Object readFromArray(int index) {
        return array[index];
    }

    @Override
    protected NativeMemoryWrapper allocateNative() {
        long ptr = NativeMemory.allocate(array.length * Double.BYTES, "NativeDoubleArray");
        NativeMemoryWrapper nativeAddress = NativeMemory.wrapNativeMemory(ptr, this);
        NativeMemory.copyMemory(array, nativeAddress, ElementType.DOUBLE, array.length);
        return nativeAddress;
    }

    @Override
    protected void copyBackFromNative(NativeMemoryWrapper nativeAddress) {
        NativeMemory.copyMemory(nativeAddress, array, ElementType.DOUBLE, array.length);
    }

    @Override
    protected Object getSulongArrayType(RContext ctx) {
        return ctx.getRFFI().getSulongArrayType(42.42);
    }
}
