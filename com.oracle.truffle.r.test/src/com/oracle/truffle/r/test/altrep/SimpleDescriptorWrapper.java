/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.test.altrep;

import com.oracle.truffle.r.runtime.data.RAltIntVectorData;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.altrep.AltIntegerClassDescriptor;
import com.oracle.truffle.r.runtime.data.altrep.RAltRepData;
import com.oracle.truffle.r.runtime.ffi.util.NativeMemory;
import com.oracle.truffle.r.runtime.ffi.util.NativeMemory.ElementType;

public class SimpleDescriptorWrapper {
    private static final int LENGTH = 10;
    private DataptrMethod dataptrMethod;
    private Length lengthMethod;
    private Elt eltMethod;
    private Sum sumMethod;
    private AltIntegerClassDescriptor descriptor;
    private RIntVector altIntVector;
    private RAltIntVectorData altIntVectorData;
    private long nativeMemPtr;

    public SimpleDescriptorWrapper() {
        nativeMemPtr = allocateNativeMem(LENGTH);
        dataptrMethod = new DataptrMethod(nativeMemPtr);
        eltMethod = new Elt(nativeMemPtr);
        lengthMethod = new Length();
        sumMethod = new Sum();
        descriptor = createDescriptor();
        altIntVector = RDataFactory.createAltIntVector(descriptor, new RAltRepData(RNull.instance, RNull.instance));
        altIntVectorData = (RAltIntVectorData) altIntVector.getData();
    }

    private AltIntegerClassDescriptor createDescriptor() {
        AltIntegerClassDescriptor descriptor = new AltIntegerClassDescriptor(SimpleDescriptorWrapper.class.getSimpleName(),
                        "packageName", null);
        // descriptor.registerDataptrMethod(dataptrMethod);
        // descriptor.registerLengthMethod(lengthMethod);
        // descriptor.registerEltMethod(eltMethod);
        // descriptor.registerSumMethod(sumMethod);
        return descriptor;
    }

    private static long allocateNativeMem(long size) {
        return NativeMemory.allocate(ElementType.INT, size, null);
    }

    public AltIntegerClassDescriptor getDescriptor() {
        return descriptor;
    }

    public DataptrMethod getDataptrMethod() {
        return dataptrMethod;
    }

    public Length getLengthMethod() {
        return lengthMethod;
    }

    public Elt getEltMethod() {
        return eltMethod;
    }

    public Sum getSumMethod() {
        return sumMethod;
    }

    public RAltIntVectorData getAltIntVectorData() {
        return altIntVectorData;
    }

    public RIntVector getAltIntVector() {
        return altIntVector;
    }

    public int getExpectedLength() {
        return LENGTH;
    }

    static final class Sum extends SumFunctionMock {
        @Override
        protected int sum(RIntVector instance, boolean naRm) {
            return 42;
        }
    }

    static final class Elt extends EltFunctionMock {
        private long nativeMemAddr;

        Elt(long nativeMemAddr) {
            this.nativeMemAddr = nativeMemAddr;
        }

        @Override
        protected int elt(RIntVector instance, int idx) {
            return NativeMemory.getInt(nativeMemAddr, idx);
        }
    }

    static final class DataptrMethod extends DataptrFunctionMock {
        private long nativeMemAddr;

        DataptrMethod(long nativeMemAddr) {
            this.nativeMemAddr = nativeMemAddr;
        }

        @Override
        protected Dataptr dataptr(RIntVector instance, boolean writeabble) {
            return new Dataptr(nativeMemAddr);
        }
    }

    static final class Length extends LengthFunctionMock {
        @Override
        protected int length(RIntVector instance) {
            return LENGTH;
        }
    }
}
