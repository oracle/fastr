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
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.LoopConditionProfile;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.Iterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.RandomAccessIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.RandomAccessWriteIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.SeqIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.SeqWriteIterator;
import com.oracle.truffle.r.runtime.data.altrep.AltIntegerClassDescriptor;
import com.oracle.truffle.r.runtime.data.altrep.RAltRepData;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.data.nodes.FastPathVectorAccess.FastPathFromIntAccess;
import com.oracle.truffle.r.runtime.ffi.util.NativeMemory;

@ExportLibrary(VectorDataLibrary.class)
public class RAltIntVectorData implements TruffleObject, VectorDataWithOwner {
    private final RAltRepData data;
    protected final AltIntegerClassDescriptor descriptor;
    private boolean dataptrCalled;
    private RIntVector owner;

    public RAltIntVectorData(AltIntegerClassDescriptor descriptor, RAltRepData data) {
        this.data = data;
        this.descriptor = descriptor;
        this.dataptrCalled = false;
        assert hasDescriptorRegisteredNecessaryMethods(descriptor):
                "Descriptor " + descriptor.toString() + " does not have registered all necessary methods";
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

    @Override
    public void setOwner(RAbstractVector newOwner) {
        owner = (RIntVector) newOwner;
    }

    private RIntVector getOwner() {
        assert owner != null;
        return owner;
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public final RType getType() {
        return RType.Integer;
    }

    @ExportMessage
    public int getLength(@CachedLibrary("this.descriptor.getLengthMethod()") InteropLibrary lengthMethodInterop,
                          @Cached("createBinaryProfile()") ConditionProfile hasMirrorProfile) {
        return descriptor.invokeLengthMethodCached(getOwner(), lengthMethodInterop, hasMirrorProfile);
    }

    private int getLengthUncached() {
        return getLength(InteropLibrary.getFactory().getUncached(), ConditionProfile.getUncached());
    }

    @ExportMessage
    public RIntArrayVectorData materialize() {
        // TODO: TOhle by chtelo implementovat pomoci Dataptr
        int[] newData = getDataAsArray();
        return new RIntArrayVectorData(newData, true);
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
    public boolean isComplete() {
        return true;
    }

    @ExportMessage
    public int[] getReadonlyIntData() {
        return getDataAsArray();
    }

    @ExportMessage
    public SeqIterator iterator(
            @Shared("SeqItLoopProfile") @Cached("createCountingProfile()")LoopConditionProfile loopProfile
            ) {
        SeqIterator it = new SeqIterator(descriptor, getLengthUncached());
        it.initLoopConditionProfile(loopProfile);
        return it;
    }

    @ExportMessage
    public RandomAccessIterator randomAccessIterator() {
        return new RandomAccessIterator(descriptor);
    }

    private static AltIntegerClassDescriptor getDescriptorFromIterator(Iterator it) {
        assert it.getStore() instanceof AltIntegerClassDescriptor;
        return (AltIntegerClassDescriptor) it.getStore();
    }

    @ExportMessage
    public boolean next(SeqIterator it, boolean withWrap,
                        @Shared("SeqItLoopProfile") @Cached("createCountingProfile()") LoopConditionProfile loopProfile) {
        return it.next(loopProfile, withWrap);
    }

    // TODO: Implement with copy of FastPathAccess
    @ExportMessage
    public int getIntAt(int index,
                        @Cached("createBinaryProfile()") ConditionProfile isEltMethodRegisteredProfile,
                        @Cached("createBinaryProfile()") ConditionProfile hasMirrorProfile,
                        @CachedLibrary("descriptor.getEltMethod()") InteropLibrary eltMethodInterop) {
        if (isEltMethodRegisteredProfile.profile(descriptor.isEltMethodRegistered())) {
            return descriptor.invokeEltMethodCached(getOwner(), index, eltMethodInterop, hasMirrorProfile);
        } else {
            return getIntAtUncached(index);
        }
    }

    private int getIntAtUncached(int index) {
        long address = invokeDataptrMethod();
        return NativeMemory.getInt(address, index);
    }

    @ExportMessage
    public int getNextInt(SeqIterator it,
                          @Cached("createBinaryProfile()") ConditionProfile hasMirrorProfile,
                          @CachedLibrary("descriptor.getEltMethod()") InteropLibrary eltMethodInterop) {
        int value = getDescriptorFromIterator(it).invokeEltMethodCached(
                getOwner(), it.getIndex(), eltMethodInterop, hasMirrorProfile);
        return value;
    }

    @ExportMessage
    public int getInt(RandomAccessIterator it, int index) {
        return getDescriptorFromIterator(it).invokeEltMethodUncached(owner, index);
    }

    // Write access to elements:

    private long invokeDataptrMethod() {
        // TODO: Exception handling?
        dataptrCalled = true;
        return descriptor.invokeDataptrMethodUncached(getOwner(), true);
    }

    @ExportMessage
    public SeqWriteIterator writeIterator() {
        int length = getLengthUncached();
        return new SeqWriteIterator(descriptor, length);
    }

    @ExportMessage
    public RandomAccessWriteIterator randomAccessWriteIterator() {
        return new RandomAccessWriteIterator(descriptor);
    }

    @ExportMessage
    public void commitWriteIterator(SeqWriteIterator iterator) {
        iterator.commit();
    }

    @ExportMessage
    public void commitRandomAccessWriteIterator(RandomAccessWriteIterator iterator) {
        iterator.commit();
    }

    @ExportMessage
    public void setIntAt(int index, int value) {
        long address = invokeDataptrMethod();
        NativeMemory.putInt(address, index, value);
    }

    @ExportMessage
    public void setNextInt(SeqWriteIterator it, int value) {
        // TODO: invokeCached
        long dataptrAddr = getDescriptorFromIterator(it).invokeDataptrMethodUncached(getOwner(), true);
        NativeMemory.putInt(dataptrAddr, it.getIndex(), value);
    }

    @ExportMessage
    public void setInt(RandomAccessWriteIterator it, int index, int value) {
        // TODO: invokeCached
        long dataptrAddr = getDescriptorFromIterator(it).invokeDataptrMethodUncached(getOwner(), true);
        NativeMemory.putInt(dataptrAddr, index, value);
    }

    private int[] getDataAsArray() {
        final int length = getLengthUncached();
        int[] newData = new int[length];
        for (int i = 0; i < length; i++) {
            newData[i] = getIntAtUncached(i);
        }
        return newData;
    }

    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        return "AltIntegerVector: data={" + data.toString() + "}";
    }

    /**
     * TODO: Remove it when it is integrated into RAltIntVectorData
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
