/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.library.ExportMessage.Ignore;

import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.data.closures.RClosures;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.data.nodes.FastPathVectorAccess.FastPathFromLogicalAccess;
import com.oracle.truffle.r.runtime.data.nodes.SlowPathVectorAccess.SlowPathFromLogicalAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;
import com.oracle.truffle.r.runtime.ops.na.NACheck;
import com.oracle.truffle.r.runtime.data.RSharingAttributeStorage.Shareable;
import com.oracle.truffle.r.runtime.data.closures.RClosure;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector.RMaterializedVector;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

@ExportLibrary(InteropLibrary.class)
public final class RLogicalVector extends RAbstractLogicalVector implements RMaterializedVector, Shareable {

    private int length;

    RLogicalVector(byte[] data, boolean complete) {
        super(complete);
        setData(new RLogicalArrayVectorData(data, complete), data.length);
        assert RAbstractVector.verifyVector(this);
    }

    RLogicalVector(byte[] data, boolean complete, int[] dims, RStringVector names, RList dimNames) {
        this(data, complete);
        initDimsNamesDimNames(dims, names, dimNames);
    }

    RLogicalVector(Object data, int newLen) {
        super(false);
        setData(data, newLen);
        assert RAbstractVector.verifyVector(this);
    }

    private RLogicalVector() {
        super(false);
    }

    private void setData(Object data, int newLen) {
        this.data = data;
        if (data instanceof VectorDataWithOwner) {
            ((VectorDataWithOwner) data).setOwner(this);
        }
        // Temporary solution to keep getLength(), isComplete(), and isShareable be fast-path
        // operations (they only read a field, no polymorphism).
        // The assumption is that length of vectors can only change in infrequently used setLength
        // operation where we update the field accordingly
        length = newLen;
        shareable = data instanceof RLogicalArrayVectorData || data instanceof RLogicalNativeVectorData;
        // Only array storage strategy is handling the complete flag dynamically,
        // for other strategies, the complete flag is determined solely by the type of the strategy
        if (!(data instanceof RLogicalArrayVectorData)) {
            setComplete(false);
        }
    }

    public static RLogicalVector createForeignWrapper(Object foreign) {
        RLogicalForeignObjData data = new RLogicalForeignObjData(foreign);
        return new RLogicalVector(data, VectorDataLibrary.getFactory().getUncached().getLength(data));
    }

    static RLogicalVector fromNative(long address, int length) {
        RLogicalVector result = new RLogicalVector();
        NativeDataAccess.toNative(result);
        NativeDataAccess.setNativeContents(result, address, length);
        result.setData(new RLogicalNativeVectorData(result), length);
        return result;
    }

    @ExportMessage
    boolean isNull(@Cached.Exclusive @Cached("createBinaryProfile()") ConditionProfile isScalar) {
        if (!isScalar.profile(isScalar())) {
            return false;
        }
        return RRuntime.isNA(getDataAt(0));
    }

    @ExportMessage
    boolean isBoolean() {
        if (!isScalar()) {
            return false;
        }
        return !RRuntime.isNA(getDataAt(0));
    }

    @ExportMessage
    boolean asBoolean(@Cached.Exclusive @Cached("createBinaryProfile()") ConditionProfile isBoolean) throws UnsupportedMessageException {
        if (isBoolean.profile(!isBoolean())) {
            throw UnsupportedMessageException.create();
        }
        return RRuntime.fromLogical(getDataAt(0));
    }

    @Override
    @Ignore // AbstractContainerLibrary
    public boolean isMaterialized() {
        return VectorDataLibrary.getFactory().getUncached().isWriteable(this.data);
    }

    @Override
    public boolean isForeignWrapper() {
        return data instanceof RLogicalForeignObjData;
    }

    @Override
    public boolean isClosure() {
        return data instanceof RClosure;
    }

    @Override
    public RClosure getClosure() {
        return (RClosure) data;
    }

    @Override
    public RType getRType() {
        return RType.Logical;
    }

    @Override
    public RAbstractVector castSafe(RType type, ConditionProfile isNAProfile, boolean keepAttributes) {
        switch (type) {
            case Logical:
                return this;
            case Integer:
                return RClosures.createToIntVector(this, keepAttributes);
            case Double:
                return RClosures.createToDoubleVector(this, keepAttributes);
            case Complex:
                return RClosures.createToComplexVector(this, keepAttributes);
            case Character:
                return RClosures.createToStringVector(this, keepAttributes);
            case List:
                return RClosures.createToListVector(this, keepAttributes);
            default:
                return null;
        }
    }

    @Override
    public byte[] getInternalManagedData() {
        if (data instanceof RLogicalNativeVectorData) {
            return null;
        }
        // TODO: get rid of this method
        assert data instanceof RLogicalArrayVectorData : data.getClass().getName();
        return ((RLogicalArrayVectorData) data).getReadonlyLogicalData();
    }

    @Override
    public Object getInternalStore() {
        return data;
    }

    @Override
    public void setDataAt(Object store, int index, byte value) {
        assert data == store;
        VectorDataLibrary.getFactory().getUncached().setLogicalAt(store, index, value);
    }

    @Override
    public byte getDataAt(Object store, int index) {
        assert data == store;
        return VectorDataLibrary.getFactory().getUncached().getLogicalAt(store, index);
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
            RLogicalNativeVectorData newData = new RLogicalNativeVectorData(this);
            setData(newData, l);
            setComplete(false);
        }
    }

    @Override
    public void setTrueLength(int l) {
        try {
            NativeDataAccess.setTrueDataLength(this, l);
        } finally {
            RLogicalNativeVectorData newData = new RLogicalNativeVectorData(this);
            setData(newData, VectorDataLibrary.getFactory().getUncached().getLength(newData));
            setComplete(false);
        }
    }

    @Override
    public byte getDataAt(int index) {
        return getDataAt(getData(), index);
    }

    @Override
    public Object getDataAtAsObject(int index) {
        return getDataAt(index);
    }

    private RLogicalVector updateDataAt(int index, byte value, NACheck valueNACheck) {
        assert !this.isShared();
        assert !RRuntime.isNA(value) || valueNACheck.isEnabled();
        VectorDataLibrary.getFactory().getUncached().setLogicalAt(data, index, value);
        assert !isComplete() || !RRuntime.isNA(value);
        return this;
    }

    @Override
    public RLogicalVector updateDataAtAsObject(int i, Object o, NACheck naCheck) {
        return updateDataAt(i, (Byte) o, naCheck);
    }

    @Override
    public void transferElementSameType(int toIndex, RAbstractVector fromVector, int fromIndex) {
        VectorDataLibrary lib = VectorDataLibrary.getFactory().getUncached();
        byte value = lib.getLogicalAt(((RLogicalVector) fromVector).data, fromIndex);
        lib.setLogicalAt(data, toIndex, value);
    }

    @Override
    public byte[] getDataCopy() {
        return VectorDataLibrary.getFactory().getUncached().getLogicalDataCopy(data);
    }

    @Override
    public byte[] getReadonlyData() {
        return VectorDataLibrary.getFactory().getUncached().getReadonlyLogicalData(data);
    }

    @Override
    @Ignore
    public RLogicalVector materialize() {
        return containerLibMaterialize(VectorDataLibrary.getFactory().getUncached(data));
    }

    @ExportMessage(library = AbstractContainerLibrary.class)
    public void materializeData(@CachedLibrary(limit = DATA_LIB_LIMIT) VectorDataLibrary dataLib) {
        setData(dataLib.materialize(data), getLength());
    }

    @ExportMessage(name = "materialize", library = AbstractContainerLibrary.class)
    RLogicalVector containerLibMaterialize(@CachedLibrary(limit = DATA_LIB_LIMIT) VectorDataLibrary dataLib) {
        if (dataLib.isWriteable(data)) {
            return this;
        }
        // To retain the semantics of the original materialize, for sequences and such we return new
        // vector
        return new RLogicalVector(dataLib.getLogicalDataCopy(data), isComplete());
    }

    @ExportMessage(name = "copy", library = AbstractContainerLibrary.class)
    RLogicalVector containerLibCopy(@CachedLibrary(limit = DATA_LIB_LIMIT) VectorDataLibrary dataLib) {
        RLogicalVector result = new RLogicalVector(dataLib.copy(data, false), dataLib.getLength(data));
        MemoryCopyTracer.reportCopying(this, result);
        return result;
    }

    @Override
    @Ignore // AbstractContainerLibrary
    public RLogicalVector createEmptySameType(int newLength, boolean newIsComplete) {
        return new RLogicalVector(new byte[newLength], newIsComplete);
    }

    @CompilerDirectives.TruffleBoundary
    protected void copyAttributes(RIntVector materializedVec) {
        materializedVec.copyAttributesFrom(this);
    }

    @Override
    protected RLogicalVector internalCopy() {
        return RDataFactory.createLogicalVector(getDataCopy(), isComplete());
    }

    @Override
    protected RLogicalVector internalCopyResized(int size, boolean fillNA, int[] dimensions) {
        boolean isComplete = isResizedComplete(size, fillNA);
        return RDataFactory.createLogicalVector(copyResizedData(size, fillNA), isComplete, dimensions);
    }

    private byte[] copyResizedData(int size, boolean fillNA) {
        byte[] localData = getReadonlyData();
        byte[] newData = Arrays.copyOf(localData, size);
        if (size > localData.length) {
            if (fillNA) {
                for (int i = localData.length; i < size; i++) {
                    newData[i] = RRuntime.LOGICAL_NA;
                }
            } else {
                assert localData.length > 0 : "cannot call resize on empty vector if fillNA == false";
                for (int i = localData.length, j = 0; i < size; ++i, j = Utils.incMod(j, localData.length)) {
                    newData[i] = localData[j];
                }
            }
        }
        return newData;
    }

    protected static byte[] resizeData(byte[] newData, byte[] oldData, int oldDataLength, boolean fillNA) {
        if (newData.length > oldDataLength) {
            if (fillNA) {
                for (int i = oldDataLength; i < newData.length; i++) {
                    newData[i] = RRuntime.LOGICAL_NA;
                }
            } else {
                assert oldData.length > 0 : "cannot call resize on empty vector if fillNA == false";
                for (int i = oldDataLength, j = 0; i < newData.length; ++i, j = Utils.incMod(j, oldDataLength)) {
                    newData[i] = oldData[j];
                }
            }
        }
        return newData;
    }

    @Override
    public byte[] getDataTemp() {
        return (byte[]) super.getDataTemp();
    }

    @Override
    public void setElement(int index, Object value) {
        setDataAt(getData(), index, (Byte) value);
    }

    public long allocateNativeContents() {
        try {
            data = VectorDataLibrary.getFactory().getUncached().materialize(data);
            long result = NativeDataAccess.allocateNativeContents(this, getArrayForNativeDataAccess(), getLength());
            setData(new RLogicalNativeVectorData(this), getLength());
            return result;
        } finally {
            setComplete(false);
        }
    }

    private final AtomicReference<RLogicalVector> materialized = new AtomicReference<>();

    public Object cachedMaterialize() {
        if (materialized.get() == null) {
            materialized.compareAndSet(null, materialize());
        }
        return materialized.get();
    }

    private static final class FastPathAccess extends FastPathFromLogicalAccess {

        FastPathAccess(RAbstractContainer value) {
            super(value);
        }

        @Override
        protected byte getLogicalImpl(AccessIterator accessIter, int index) {
            return dataLib.getLogicalAt(accessIter.getStore(), index);
        }

        @Override
        protected void setLogicalImpl(AccessIterator accessIter, int index, byte value) {
            dataLib.setLogicalAt(accessIter.getStore(), index, value);
        }
    }

    @Override
    public VectorAccess access() {
        return new FastPathAccess(this);
    }

    private static final SlowPathFromLogicalAccess SLOW_PATH_ACCESS = new SlowPathFromLogicalAccess() {
        @Override
        protected byte getLogicalImpl(AccessIterator accessIter, int index) {
            RLogicalVector vector = (RLogicalVector) accessIter.getStore();
            return vector.getDataAt(index);
        }

        @Override
        protected void setLogicalImpl(AccessIterator accessIter, int index, byte value) {
            RLogicalVector vector = (RLogicalVector) accessIter.getStore();
            vector.setDataAt(vector.getInternalStore(), index, value);
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
        return data instanceof RLogicalArrayVectorData ? ((RLogicalArrayVectorData) data).getReadonlyLogicalData() : null;
    }
}
