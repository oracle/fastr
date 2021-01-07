/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Arrays;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.library.ExportMessage.Ignore;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.data.RSharingAttributeStorage.Shareable;
import com.oracle.truffle.r.runtime.data.altrep.AltRawClassDescriptor;
import com.oracle.truffle.r.runtime.data.altrep.AltrepUtilities;
import com.oracle.truffle.r.runtime.data.altrep.RAltRepData;
import com.oracle.truffle.r.runtime.data.closures.RClosures;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractNumericVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector.RMaterializedVector;
import com.oracle.truffle.r.runtime.data.nodes.FastPathVectorAccess.FastPathFromRawAccess;
import com.oracle.truffle.r.runtime.data.nodes.SlowPathVectorAccess.SlowPathFromRawAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

@ExportLibrary(AbstractContainerLibrary.class)
public final class RRawVector extends RAbstractNumericVector implements RMaterializedVector, Shareable {

    private int length;

    RRawVector(byte[] data) {
        setData(new RRawArrayVectorData(data), data.length);
        assert RAbstractVector.verifyVector(this);
    }

    RRawVector(byte[] data, int[] dims, RStringVector names, RList dimNames) {
        this(data);
        initDimsNamesDimNames(dims, names, dimNames);
    }

    RRawVector(Object data, int newLen) {
        setData(data, newLen);
        assert RAbstractVector.verifyVector(this);
    }

    private RRawVector() {
    }

    private void setData(Object data, int newLen) {
        // Temporary solution to keep getLength() fast
        // The assumption is that length of vectors can only change in infrequently used setLength
        // operation where we update the field accordingly
        length = newLen;
        super.setData(data);
    }

    static RRawVector fromNative(long address, int length) {
        RRawVector result = new RRawVector();
        NativeDataAccess.toNative(result);
        NativeDataAccess.setNativeContents(result, address, length);
        result.setData(new RRawNativeVectorData(result), length);
        return result;
    }

    @CompilerDirectives.TruffleBoundary
    public static RRawVector createAltRaw(AltRawClassDescriptor descriptor, RAltRepData altRepData) {
        RAltRawVectorData altRawVectorData = new RAltRawVectorData(descriptor, altRepData);
        RRawVector rawVector = new RRawVector();
        rawVector.setAltRep();
        rawVector.data = altRawVectorData;
        int length = AltrepUtilities.getLengthUncached(rawVector);
        rawVector.setData(altRawVectorData, length);
        return rawVector;
    }

    @Override
    public RType getRType() {
        return RType.Raw;
    }

    @Override
    protected Object getScalarValue(VectorDataLibrary dataLib) {
        assert getLength() == 1;
        return dataLib.getRawAt(getData(), 0);
    }

    @Override
    public RAbstractVector castSafe(RType type, ConditionProfile isNAProfile, boolean keepAttributes) {
        switch (type) {
            case Raw:
                return this;
            case Integer:
                return RClosures.createToIntVector(this, keepAttributes);
            case Double:
                return RClosures.createToDoubleVector(this, keepAttributes);
            case Complex:
                return RClosures.createToComplexVector(this, keepAttributes);
            case Character:
                return RClosures.createToStringVector(this, keepAttributes);
            default:
                return null;
        }
    }

    @Override
    public boolean isForeignWrapper() {
        return false;
    }

    @Override
    public byte[] getInternalManagedData() {
        if (data instanceof RRawNativeVectorData) {
            return null;
        }
        // TODO: get rid of this method
        assert data instanceof RRawArrayVectorData : data.getClass().getName();
        return ((RRawArrayVectorData) data).getReadonlyRawData();
    }

    @Override
    public Object getInternalStore() {
        return data;
    }

    @Override
    protected RRawVector internalCopyResized(int size, boolean fillNA, int[] dimensions) {
        return RDataFactory.createRawVector(copyResizedData(size, fillNA), dimensions);
    }

    private byte[] copyResizedData(int size, boolean fillNA) {
        byte[] localData = getReadonlyData();
        byte[] newData = Arrays.copyOf(localData, size);
        if (!fillNA) {
            assert localData.length > 0 : "cannot call resize on empty vector if fillNA == false";
            // NA is 00 for raw
            for (int i = localData.length, j = 0; i < size; ++i, j = Utils.incMod(j, localData.length)) {
                newData[i] = localData[j];
            }
        }
        return newData;
    }

    @Override
    public byte[] getDataTemp() {
        return (byte[]) super.getDataTemp();
    }

    @Override
    public byte[] getReadonlyData() {
        return getDataCopy();
    }

    @Override
    protected RRawVector internalCopy() {
        return RDataFactory.createRawVector(getDataCopy());
    }

    @Override
    public byte[] getDataCopy() {
        return VectorDataLibrary.getFactory().getUncached().getRawDataCopy(data);
    }

    public byte getRawDataAt(int index) {
        return VectorDataLibrary.getFactory().getUncached().getRawAt(data, index);
    }

    public byte getRawDataAt(Object store, int index) {
        assert data == store;
        return VectorDataLibrary.getFactory().getUncached().getRawAt(store, index);
    }

    public void setRawDataAt(Object store, int index, byte value) {
        assert data == store;
        VectorDataLibrary.getFactory().getUncached().setRawAt(store, index, value);
    }

    @Override
    @Ignore
    public int getLength() {
        return length;
    }

    @Override
    public int getTrueLength() {
        return NativeDataAccess.getTrueDataLength(this);
    }

    @Override
    public void setLength(int l) {
        try {
            NativeDataAccess.setDataLength(this, getArrayForNativeDataAccess(), l);
        } finally {
            RRawNativeVectorData newData = new RRawNativeVectorData(this);
            setData(newData, l);
        }
    }

    @Override
    public void setTrueLength(int l) {
        try {
            NativeDataAccess.setTrueDataLength(this, l);
        } finally {
            RRawNativeVectorData newData = new RRawNativeVectorData(this);
            setData(newData, VectorDataLibrary.getFactory().getUncached().getLength(newData));
        }
    }

    @Override
    @Ignore // AbstractContainerLibrary
    public RRawVector materialize() {
        return containerLibMaterialize(VectorDataLibrary.getFactory().getUncached(data));
    }

    @ExportMessage(library = AbstractContainerLibrary.class)
    public void materializeData(@CachedLibrary(limit = DATA_LIB_LIMIT) VectorDataLibrary dataLib) {
        setData(dataLib.materialize(data), getLength());
    }

    @ExportMessage(name = "materialize", library = AbstractContainerLibrary.class)
    RRawVector containerLibMaterialize(@CachedLibrary(limit = DATA_LIB_LIMIT) VectorDataLibrary dataLib) {
        if (dataLib.isWriteable(data)) {
            return this;
        }
        // To retain the semantics of the original materialize, for sequences and such we return new
        // vector
        return new RRawVector(dataLib.getRawDataCopy(data));
    }

    @ExportMessage(name = "toNative", library = AbstractContainerLibrary.class)
    public void containerLibToNative(
                    @Cached("createBinaryProfile()") ConditionProfile alreadyNativeProfile,
                    @CachedLibrary(limit = DATA_LIB_LIMIT) VectorDataLibrary dataLib) {
        if (alreadyNativeProfile.profile(data instanceof RRawNativeVectorData)) {
            return;
        }
        byte[] arr = dataLib.getReadonlyRawData(this.data);
        NativeDataAccess.allocateNativeContents(this, arr, getLength());
        setData(new RRawNativeVectorData(this), getLength());
    }

    @ExportMessage(name = "duplicate", library = AbstractContainerLibrary.class)
    RRawVector containerLibDuplicate(boolean deep, @CachedLibrary(limit = DATA_LIB_LIMIT) VectorDataLibrary dataLib) {
        RRawVector result = new RRawVector(dataLib.copy(data, deep), dataLib.getLength(data));
        setAttributes(result);
        MemoryCopyTracer.reportCopying(this, result);
        return result;
    }

    @Override
    @Ignore // AbstractContainerLibrary
    public boolean isMaterialized() {
        return VectorDataLibrary.getFactory().getUncached().isWriteable(this.data);
    }

    private RRawVector updateDataAt(int index, RRaw value) {
        assert !this.isShared();
        VectorDataLibrary.getFactory().getUncached().setRawAt(data, index, value.getValue());
        return this;
    }

    @Override
    public RRawVector updateDataAtAsObject(int i, Object o, NACheck naCheck) {
        return updateDataAt(i, (RRaw) o);
    }

    @Override
    @Ignore
    public Object getDataAtAsObject(int index) {
        return RRaw.valueOf(getRawDataAt(index));
    }

    @Override
    public void transferElementSameType(int toIndex, RAbstractVector fromVector, int fromIndex) {
        VectorDataLibrary lib = VectorDataLibrary.getFactory().getUncached();
        byte value = lib.getRawAt(((RRawVector) fromVector).data, fromIndex);
        lib.setRawAt(data, toIndex, value);
    }

    @Override
    @Ignore
    public RRawVector createEmptySameType(int newLength, @SuppressWarnings("unused") boolean newIsComplete) {
        return RDataFactory.createRawVector(new byte[newLength]);
    }

    public long allocateNativeContents() {
        setData(VectorDataLibrary.getFactory().getUncached().materialize(data));
        long result = NativeDataAccess.allocateNativeContents(this, getArrayForNativeDataAccess(), getLength());
        setData(new RRawNativeVectorData(this), getLength());
        return result;
    }

    private static final class FastPathAccess extends FastPathFromRawAccess {

        FastPathAccess(RAbstractContainer value) {
            super(value);
        }

        @Override
        protected byte getRawImpl(AccessIterator accessIter, int index) {
            return dataLib.getRawAt(accessIter.getStore(), index);
        }

        @Override
        protected void setRawImpl(AccessIterator accessIter, int index, byte value) {
            dataLib.setRawAt(accessIter.getStore(), index, value);
        }
    }

    @Override
    public VectorAccess access() {
        return new FastPathAccess(this);
    }

    private static final SlowPathFromRawAccess SLOW_PATH_ACCESS = new SlowPathFromRawAccess() {
        @Override
        protected byte getRawImpl(AccessIterator accessIter, int index) {
            RRawVector vector = (RRawVector) accessIter.getStore();
            return vector.getRawDataAt(index);
        }

        @Override
        protected void setRawImpl(AccessIterator accessIter, int index, byte value) {
            RRawVector vector = (RRawVector) accessIter.getStore();
            vector.setRawDataAt(vector.getInternalStore(), index, value);
        }
    };

    @Override
    public VectorAccess slowPathAccess() {
        return SLOW_PATH_ACCESS;
    }

    // TODO: Hack: we make sure the vector is either array or native, so that we can call
    // NativeDataAccess methods
    private byte[] getArrayForNativeDataAccess() {
        materializeData(VectorDataLibrary.getFactory().getUncached());
        return data instanceof RRawArrayVectorData ? ((RRawArrayVectorData) data).getReadonlyRawData() : null;
    }

    @ExportMessage(name = "getLength", library = AbstractContainerLibrary.class)
    @Override
    public int containerLibGetLength(@CachedLibrary(limit = DATA_LIB_LIMIT) VectorDataLibrary dataLib) {
        return dataLib.getLength(data);
    }
}
