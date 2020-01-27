/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.test.runtime.ffi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.ffi.interop.NativeCharArray;
import com.oracle.truffle.r.runtime.ffi.interop.NativeDoubleArray;
import com.oracle.truffle.r.runtime.ffi.interop.NativeIntegerArray;
import com.oracle.truffle.r.runtime.ffi.interop.StringArrayWrapper;
import com.oracle.truffle.r.runtime.ffi.util.NativeMemory;
import org.junit.Test;

public class NativeArrayTests {
    private static final double DELTA = 0.0000000001;
    private final InteropLibrary interop = InteropLibrary.getFactory().getUncached();

    @Test
    public void testIntArray() throws Throwable {
        int[] array = new int[]{1, 2, 3};
        NativeIntegerArray wrapper = new NativeIntegerArray(array);

        // interop array operations
        assertEquals(1, interop.readArrayElement(wrapper, 0));
        interop.writeArrayElement(wrapper, 0, 42);
        assertEquals(42, interop.readArrayElement(wrapper, 0));

        // pointers
        assertFalse(interop.isPointer(wrapper));
        interop.toNative(wrapper);
        long address = interop.asPointer(wrapper);
        NativeMemory.putInt(address, 0, 11);
        NativeMemory.putInt(address, 2, 13);
        assertEquals(11, interop.readArrayElement(wrapper, 0));
        assertEquals(13, interop.readArrayElement(wrapper, 2));

        // transfer back to managed memory
        int[] result = (int[]) wrapper.refresh();
        assertEquals(11, result[0]);
        assertEquals(2, result[1]);
        assertEquals(13, result[2]);
    }

    @Test
    public void testDoubleArray() throws Throwable {
        double[] array = new double[]{1.5, 2.5, 3.25};
        NativeDoubleArray wrapper = new NativeDoubleArray(array);

        // interop array operations
        assertEquals(1.5, interop.readArrayElement(wrapper, 0));
        interop.writeArrayElement(wrapper, 0, 4.5);
        assertEquals(4.5, interop.readArrayElement(wrapper, 0));

        // pointers
        assertFalse(interop.isPointer(wrapper));
        interop.toNative(wrapper);
        long address = interop.asPointer(wrapper);
        NativeMemory.putDouble(address, 0, 5.5);
        NativeMemory.putDouble(address, 2, 6.5);
        assertEquals(5.5, interop.readArrayElement(wrapper, 0));
        assertEquals(6.5, interop.readArrayElement(wrapper, 2));

        // transfer back to managed memory
        double[] result = (double[]) wrapper.refresh();
        assertEquals(5.5, result[0], DELTA);
        assertEquals(2.5, result[1], DELTA);
        assertEquals(6.5, result[2], DELTA);
    }

    @Test
    public void testCharArray() throws Throwable {
        byte[] bytes = "test".getBytes();
        NativeCharArray wrapper = new NativeCharArray(bytes);

        // interop array operations
        assertEquals((byte) 't', interop.readArrayElement(wrapper, 0));
        assertEquals((byte) 0, interop.readArrayElement(wrapper, 4));
        interop.writeArrayElement(wrapper, 0, (byte) 'c');
        assertEquals((byte) 'c', interop.readArrayElement(wrapper, 0));

        // pointers
        assertFalse(interop.isPointer(wrapper));
        interop.toNative(wrapper);
        long address = interop.asPointer(wrapper);
        NativeMemory.putByte(address, 0, (byte) 'n');
        NativeMemory.putByte(address, 2, (byte) 'e');
        assertEquals((byte) 'n', interop.readArrayElement(wrapper, 0));
        assertEquals((byte) 'e', interop.readArrayElement(wrapper, 2));
        assertEquals((byte) 0, interop.readArrayElement(wrapper, 4));

        // transfer back to native memory
        byte[] result = (byte[]) wrapper.refresh();
        assertEquals((byte) 'n', result[0]);
        assertEquals((byte) 'e', result[1]);
        assertEquals((byte) 'e', result[2]);
        assertEquals((byte) 't', result[3]);
    }

    @Test
    public void testStringArray() throws Throwable {
        RStringVector vector = RDataFactory.createStringVector(new String[]{"a", "bcde"}, true);
        StringArrayWrapper wrapper = new StringArrayWrapper(vector);

        // interop array operations
        NativeCharArray item0 = (NativeCharArray) interop.readArrayElement(wrapper, 0);
        assertEquals("a", item0.getString());

        NativeCharArray item1 = (NativeCharArray) interop.readArrayElement(wrapper, 1);
        assertEquals("bcde", item1.getString());

        // no support for writing via interop, TODO: issue?

        // pointers
        assertFalse(interop.isPointer(wrapper));
        interop.toNative(wrapper);
        long address = interop.asPointer(wrapper);

        long item0Addr = NativeMemory.getLong(address);
        assertEquals((byte) 'a', NativeMemory.getByte(item0Addr, 0));
        assertEquals((byte) 0, NativeMemory.getByte(item0Addr, 1));

        long item1Addr = NativeMemory.getLong(address, 1);
        assertEquals((byte) 'b', NativeMemory.getByte(item1Addr, 0));
        assertEquals((byte) 'c', NativeMemory.getByte(item1Addr, 1));
        assertEquals((byte) 'd', NativeMemory.getByte(item1Addr, 2));
        assertEquals((byte) 'e', NativeMemory.getByte(item1Addr, 3));
        assertEquals((byte) 0, NativeMemory.getByte(item1Addr, 4));

        NativeMemory.putByte(item0Addr, 0, (byte) 'q');

        // transfer back to native memory
        RStringVector result = wrapper.copyBackFromNative();
        assertEquals("q", result.getDataAt(0));
        assertEquals("bcde", result.getDataAt(1));
    }
}
