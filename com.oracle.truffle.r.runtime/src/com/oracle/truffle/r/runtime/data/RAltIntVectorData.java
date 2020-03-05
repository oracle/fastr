/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.library.ExportMessage.Ignore;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.data.VectorDataLibraryUtils.RandomAccessIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibraryUtils.SeqIterator;
import com.oracle.truffle.r.runtime.data.altrep.AltIntegerClassDescriptor;
import com.oracle.truffle.r.runtime.data.altrep.RAltRepData;
import com.oracle.truffle.r.runtime.data.nodes.FastPathVectorAccess.FastPathFromIntAccess;
import com.oracle.truffle.r.runtime.ffi.util.NativeMemory;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

@ExportLibrary(RIntVectorDataLibrary.class)
public class RAltIntVectorData extends RIntVectorData {
    private final RAltRepData data;
    protected final AltIntegerClassDescriptor descriptor;
    private RIntVector vector;
    private boolean dataptrCalled;

    public RAltIntVectorData(AltIntegerClassDescriptor descriptor, RAltRepData data, RIntVector vector) {
        this.data = data;
        this.descriptor = descriptor;
        this.vector = vector;
        this.dataptrCalled = false;
        assert hasDescriptorRegisteredNecessaryMethods(descriptor):
                "Descriptor " + descriptor.toString() + " does not have registered all necessary methods";
    }

    public RAltIntVectorData(AltIntegerClassDescriptor descriptor, RAltRepData data) {
        this(descriptor, data, null);
    }

    private boolean hasDescriptorRegisteredNecessaryMethods(AltIntegerClassDescriptor descriptor) {
        return descriptor.isLengthMethodRegistered() && descriptor.isDataptrMethodRegistered();
                /* TODO: && descriptor.isUnserializeMethodRegistered(); */
    }

    public AltIntegerClassDescriptor getDescriptor() {
        return descriptor;
    }

    public RAltRepData getData() {
        return data;
    }

    public Object getData1() {
        return data.getData1();
    }

    public Object getData2() {
        return data.getData2();
    }

    public void setData1(Object data1) {
        data.setData1(data1);
    }

    public void setData2(Object data2) {
        data.setData2(data2);
    }

    public void setVector(RIntVector vector) {
        this.vector = vector;
    }

    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        return "AltIntegerVector: data={" + data.toString() + "}";
    }

    @Override
    @Ignore
    public int getIntAt(int index) {
        ConditionProfile isEltMethodRegisteredProfile = ConditionProfile.getUncached();
        return getIntAt(index, isEltMethodRegisteredProfile);
    }

    // TODO: Implement with copy of FastPathAccess
    @ExportMessage
    public int getIntAt(int index, @Cached("createBinaryProfile()") ConditionProfile isEltMethodRegisteredProfile) {
        if (isEltMethodRegisteredProfile.profile(descriptor.isEltMethodRegistered())) {
            return descriptor.invokeEltMethodUncached(vector, index);
        } else {
            // Invoke uncached dataptr method
            long address = invokeDataptrMethod();
            return NativeMemory.getInt(address, index);
        }
    }

    private long invokeDataptrMethod() {
        // TODO: Exception handling?
        dataptrCalled = true;
        return descriptor.invokeDataptrMethodUncached(vector, true);
    }

    @ExportMessage
    @Override
    public void setIntAt(int index, int value, @SuppressWarnings("unused") NACheck naCheck) {
        long address = invokeDataptrMethod();
        NativeMemory.putInt(address, index, value);
    }

    @ExportMessage
    public RIntArrayVectorData materialize() {
        // TODO: TOhle by chtelo implementovat pomoci Dataptr
        int[] newData = new int[getLength()];
        for (int i = 0; i < getLength(); i++) {
            newData[i] = getIntAt(i);
        }
        return new RIntArrayVectorData(newData, true);
    }

    @Override
    @Ignore
    public int getLength() {
        return getLength(InteropLibrary.getFactory().getUncached(), ConditionProfile.getUncached());
    }

    @ExportMessage(limit = "3")
    public int getLength(@CachedLibrary("this.descriptor.getLengthMethod()") InteropLibrary lengthMethodInterop,
                         @Cached("createBinaryProfile()") ConditionProfile hasMirrorProfile) {
        assert vector != null;
        return descriptor.invokeLengthMethodCached(vector, lengthMethodInterop, hasMirrorProfile);
    }

    @ExportMessage
    public boolean isWriteable() {
        return dataptrCalled;
    }

    @ExportMessage
    public RAltIntVectorData copy(boolean deep) {
        throw RInternalError.unimplemented("RAltIntVectorData.copy");
    }

    @ExportMessage
    public RIntArrayVectorData copyResized(int newSize, boolean deep, boolean fillNA) {
        throw RInternalError.unimplemented("RAltIntVectorData.copyResized");
    }

    @ExportMessage
    @Override
    public boolean isComplete() {
        return true;
    }

    @ExportMessage
    public int[] getReadonlyIntData() {
        throw RInternalError.unimplemented("RAltIntVectorData.getReadonlyIntData");
    }

    @ExportMessage
    public SeqIterator iterator() {
        // TODO: Use different store.
        return new SeqIterator(this, getLength());
    }

    @ExportMessage
    public RandomAccessIterator randomAccessIterator() {
        // TODO: Use different store.
        return new RandomAccessIterator(this, getLength());
    }

    @ExportMessage
    public int getNext(SeqIterator it) {
        assert this == it.getStore();
        return getIntAt(it.getIndex());
    }

    @ExportMessage
    public int getAt(RandomAccessIterator it, int index) {
        assert this == it.getStore();
        return getIntAt(index);
    }

    /**
     * Specializes on every separate instance. Note that we cannot have one FastPathAccess for two instances with
     * same descriptor, because this descriptor may return different Dataptr or Elt for both instances (Dataptr or
     * Elt methods may be dependent on instance data).
     */
    private static final class FastPathAccess extends FastPathFromIntAccess {
        private final boolean hasEltMethod;
        private final int instanceId;
        private final ConditionProfile hasMirrorProfile = ConditionProfile.createBinaryProfile();
        @Child private InteropLibrary eltMethodInterop;
        private final long dataptrAddr;

        FastPathAccess(RAltIntVectorData value) {
            super(value);
            this.hasEltMethod = value.getDescriptor().isEltMethodRegistered();
            this.instanceId = value.hashCode();
            this.eltMethodInterop = hasEltMethod ? InteropLibrary.getFactory().create(value.getDescriptor().getEltMethod()) : null;
            this.dataptrAddr = hasEltMethod ? 0 : value.getDescriptor().invokeDataptrMethodUncached(value, true);
        }

        @Override
        public boolean supports(Object value) {
            if (!(value instanceof RAltIntVectorData)) {
                return false;
            }
            return instanceId == value.hashCode();
        }

        @Override
        public int getIntImpl(AccessIterator accessIter, int index) {
            RAltIntVectorData instance = getInstanceFromIterator(accessIter);

            if (hasEltMethod) {
                return instance.getDescriptor().invokeEltMethodCached(instance, index, eltMethodInterop, hasMirrorProfile);
            } else {
                return NativeMemory.getInt(dataptrAddr, index);
            }
        }

        @Override
        protected void setIntImpl(AccessIterator accessIter, int index, int value) {
            RAltIntVectorData instance = getInstanceFromIterator(accessIter);
            if (dataptrAddr != 0) {
                NativeMemory.putInt(dataptrAddr, index, value);
            } else {
                throw RInternalError.shouldNotReachHere();
            }
        }

        private RAltIntVectorData getInstanceFromIterator(AccessIterator accessIterator) {
            Object store = accessIterator.getStore();
            assert store instanceof RAltIntVectorData;
            return (RAltIntVectorData) store;
        }
    }
}
