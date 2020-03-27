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
import com.oracle.truffle.api.interop.InteropLibrary;
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
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.data.nodes.SlowPathVectorAccess.SlowPathFromDoubleAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;
import com.oracle.truffle.r.runtime.ops.na.NACheck;
import com.oracle.truffle.r.runtime.data.RSharingAttributeStorage.Shareable;
import com.oracle.truffle.r.runtime.data.closures.RClosure;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector.RMaterializedVector;
import com.oracle.truffle.r.runtime.data.nodes.FastPathVectorAccess.FastPathFromDoubleAccess;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

@ExportLibrary(InteropLibrary.class)
public final class RDoubleVector extends RAbstractDoubleVector implements RMaterializedVector, Shareable {

    private int length;

    RDoubleVector(double[] data, boolean complete) {
        super(complete);
        setData(new RDoubleArrayVectorData(data, complete), data.length);
        assert RAbstractVector.verifyVector(this);
    }

    RDoubleVector(double[] data, boolean complete, int[] dims, RStringVector names, RList dimNames) {
        this(data, complete);
        initDimsNamesDimNames(dims, names, dimNames);
    }

    RDoubleVector(Object data, int newLen) {
        super(false);
        setData(data, newLen);
        assert RAbstractVector.verifyVector(this);
    }

    private RDoubleVector() {
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
        shareable = data instanceof RDoubleArrayVectorData || data instanceof RDoubleNativeVectorData;
        // Only array storage strategy is handling the complete flag dynamically,
        // for other strategies, the complete flag is determined solely by the type of the strategy
        if (!(data instanceof RDoubleArrayVectorData)) {
            // only sequences are always complete, everything else is always incomplete
            setComplete(data instanceof RDoubleSeqVectorData);
        }
    }

    public static RDoubleVector createForeignWrapper(Object foreign) {
        RDoubleForeignObjData data = new RDoubleForeignObjData(foreign);
        return new RDoubleVector(data, VectorDataLibrary.getFactory().getUncached().getLength(data));
    }

    public static RDoubleVector createSequence(double start, double stride, int length) {
        return new RDoubleVector(new RDoubleSeqVectorData(start, stride, length), length);
    }

    public static RDoubleVector createClosure(RAbstractVector delegate, boolean keepAttrs) {
        RDoubleVector result = new RDoubleVector(VectorDataClosure.fromVector(delegate, RType.Double), delegate.getLength());
        if (keepAttrs) {
            result.initAttributes(delegate.getAttributes());
        } else {
            RClosures.initRegAttributes(result, delegate);
        }
        return result;
    }

    static RDoubleVector fromNative(long address, int length) {
        RDoubleVector result = new RDoubleVector();
        NativeDataAccess.toNative(result);
        NativeDataAccess.setNativeContents(result, address, length);
        result.setData(new RDoubleNativeVectorData(result), length);
        return result;
    }

    @Override
    public boolean isMaterialized() {
        return VectorDataLibrary.getFactory().getUncached().isWriteable(this.data);
    }

    @Override
    protected boolean isScalarNA() {
        assert getLength() == 1;
        return RRuntime.isNA(getDataAt(0));
    }

    @Override
    public boolean isForeignWrapper() {
        return data instanceof RDoubleForeignObjData;
    }

    @Override
    public boolean isSequence() {
        return data instanceof RDoubleSeqVectorData;
    }

    @Override
    public RDoubleSeqVectorData getSequence() {
        return (RDoubleSeqVectorData) data;
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
        return RType.Double;
    }

    @Override
    public RAbstractVector castSafe(RType type, ConditionProfile isNAProfile, boolean keepAttributes) {
        switch (type) {
            case Integer:
                return RClosures.createToIntVector(this, keepAttributes);
            case Double:
                return this;
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
    public double[] getInternalManagedData() {
        if (data instanceof RDoubleNativeVectorData) {
            return null;
        }
        // TODO: get rid of this method
        assert data instanceof RDoubleArrayVectorData : data.getClass().getName();
        return ((RDoubleArrayVectorData) data).getReadonlyDoubleData();
    }

    @Override
    public Object getInternalStore() {
        return data;
    }

    @Override
    public void setDataAt(Object store, int index, double value) {
        assert data == store;
        VectorDataLibrary.getFactory().getUncached().setDoubleAt(store, index, value);
    }

    @Override
    public double getDataAt(Object store, int index) {
        assert data == store;
        return VectorDataLibrary.getFactory().getUncached().getDoubleAt(store, index);
    }

    @Override
    @Ignore
    public int getLength() {
        return length;
    }

    @Override
    public void setLength(int l) {
        try {
            NativeDataAccess.setDataLength(this, getArrayForNativeDataAccess(), l);
        } finally {
            RDoubleNativeVectorData newData = new RDoubleNativeVectorData(this);
            setData(newData, l);
            setComplete(false);
        }
    }

    @Override
    public int getTrueLength() {
        return NativeDataAccess.getTrueDataLength(this);
    }

    @Override
    public void setTrueLength(int l) {
        try {
            NativeDataAccess.setTrueDataLength(this, l);
        } finally {
            RDoubleNativeVectorData newData = new RDoubleNativeVectorData(this);
            setData(newData, VectorDataLibrary.getFactory().getUncached().getLength(newData));
            setComplete(false);
        }
    }

    @Override
    public double getDataAt(int index) {
        return getDataAt(getData(), index);
    }

    @Override
    public double[] getDataCopy() {
        return VectorDataLibrary.getFactory().getUncached().getDoubleDataCopy(data);
    }

    // HACK copy and paste from RAbstractVector
    @Ignore // AbstractContainerLibrary
    RDoubleVector copy(VectorDataLibrary dataLib) {
        RDoubleVector result = RDataFactory.createDoubleVector(dataLib.getDoubleDataCopy(data), dataLib.isComplete(data));
        MemoryCopyTracer.reportCopying(this, result);
        setAttributes(result);
        result.setTypedValueInfo(getTypedValueInfo());
        return result;
    }

    @Override
    public double[] getReadonlyData() {
        return VectorDataLibrary.getFactory().getUncached().getReadonlyDoubleData(data);
    }

    private RDoubleVector updateDataAt(int index, double value, NACheck naCheck) {
        assert !this.isShared();
        assert !RRuntime.isNA(value) || naCheck.isEnabled();
        VectorDataLibrary.getFactory().getUncached().setDoubleAt(data, index, value);
        assert !isComplete() || !RRuntime.isNA(value);
        return this;
    }

    @Override
    public RDoubleVector updateDataAtAsObject(int i, Object o, NACheck naCheck) {
        return updateDataAt(i, (Double) o, naCheck);
    }

    @Override
    @Ignore // AbstractContainerLibrary
    public RDoubleVector materialize() {
        if (VectorDataLibrary.getFactory().getUncached().isWriteable(data)) {
            return this;
        }
        // To retain the semantics of the original materialize, for sequences and such we return new
        // vector
        return new RDoubleVector(getDataCopy(), isComplete());
    }

    @ExportMessage(library = AbstractContainerLibrary.class)
    public void materializeData(@CachedLibrary(limit = DATA_LIB_LIMIT) VectorDataLibrary dataLib) {
        setData(dataLib.materialize(data), getLength());
    }

    @Override
    public RDoubleVector createEmptySameType(int newLength, boolean newIsComplete) {
        return new RDoubleVector(new double[newLength], newIsComplete);
    }

    @Override
    public void transferElementSameType(int toIndex, RAbstractVector fromVector, int fromIndex) {
        VectorDataLibrary lib = VectorDataLibrary.getFactory().getUncached();
        double value = lib.getDoubleAt(((RDoubleVector) fromVector).data, fromIndex);
        lib.setDoubleAt(data, toIndex, value);
    }

    @CompilerDirectives.TruffleBoundary
    protected void copyAttributes(RIntVector materializedVec) {
        materializedVec.copyAttributesFrom(this);
    }

    @Override
    protected RDoubleVector internalCopy() {
        return RDataFactory.createDoubleVector(getDataCopy(), isComplete());
    }

    @Override
    protected RDoubleVector internalCopyResized(int size, boolean fillNA, int[] dimensions) {
        double[] localData = getReadonlyData();
        double[] newData = Arrays.copyOf(localData, size);
        newData = resizeData(newData, localData, localData.length, fillNA);
        return RDataFactory.createDoubleVector(newData, isResizedComplete(size, fillNA), dimensions);
    }

    protected static double[] resizeData(double[] newData, double[] oldData, int oldDataLength, boolean fillNA) {
        if (newData.length > oldDataLength) {
            if (fillNA) {
                for (int i = oldDataLength; i < newData.length; i++) {
                    newData[i] = RRuntime.DOUBLE_NA;
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
    public double[] getDataTemp() {
        return super.getDataTemp();
    }

    @Override
    public Object getDataAtAsObject(int index) {
        return getDataAt(index);
    }

    @Override
    public void setElement(int index, Object value) {
        setDataAt(getData(), index, (Double) value);
    }

    public long allocateNativeContents() {
        try {
            data = VectorDataLibrary.getFactory().getUncached().materialize(data);
            long result = NativeDataAccess.allocateNativeContents(this, getArrayForNativeDataAccess(), getLength());
            setData(new RDoubleNativeVectorData(this), getLength());
            return result;
        } finally {
            setComplete(false);
        }
    }

    private AtomicReference<RDoubleVector> materialized = new AtomicReference<>();

    public Object cachedMaterialize() {
        if (materialized.get() == null) {
            materialized.compareAndSet(null, materialize());
        }
        return materialized.get();
    }

    private static final class FastPathAccess extends FastPathFromDoubleAccess {
        FastPathAccess(RAbstractContainer value) {
            super(value);
        }

        @Override
        public double getDoubleImpl(AccessIterator accessIter, int index) {
            return dataLib.getDoubleAt(accessIter.getStore(), index);
        }

        @Override
        protected void setDoubleImpl(AccessIterator accessIter, int index, double value) {
            dataLib.setDoubleAt(accessIter.getStore(), index, value);
        }
    }

    @Override
    public VectorAccess access() {
        return new FastPathAccess(this);
    }

    private static final SlowPathFromDoubleAccess SLOW_PATH_ACCESS = new SlowPathFromDoubleAccess() {
        @Override
        public double getDoubleImpl(AccessIterator accessIter, int index) {
            RDoubleVector vector = (RDoubleVector) accessIter.getStore();
            return vector.getDataAt(index);
        }

        @Override
        protected void setDoubleImpl(AccessIterator accessIter, int index, double value) {
            RDoubleVector vector = (RDoubleVector) accessIter.getStore();
            vector.setDataAt(vector.getInternalStore(), index, value);
        }
    };

    @Override
    public VectorAccess slowPathAccess() {
        return SLOW_PATH_ACCESS;
    }

    // TODO: Hack: we make sure the vector is either array or native, so that we can call
    // NativeDataAccess methods
    private double[] getArrayForNativeDataAccess() {
        materializeData(VectorDataLibrary.getFactory().getUncached());
        return data instanceof RDoubleArrayVectorData ? ((RDoubleArrayVectorData) data).getReadonlyDoubleData() : null;
    }

    @ExportMessage(name = "getLength", library = AbstractContainerLibrary.class)
    public int containerLibGetLength(@CachedLibrary(limit = DATA_LIB_LIMIT) VectorDataLibrary dataLib) {
        return dataLib.getLength(data);
    }
}
