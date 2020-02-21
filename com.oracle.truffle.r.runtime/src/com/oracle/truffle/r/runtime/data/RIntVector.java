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
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.data.closures.RClosure;
import com.oracle.truffle.r.runtime.data.closures.RClosures;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractNumericVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.data.nodes.FastPathVectorAccess.FastPathFromIntAccess;
import com.oracle.truffle.r.runtime.data.nodes.SlowPathVectorAccess.SlowPathFromIntAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;
import com.oracle.truffle.r.runtime.ops.na.InputNACheck;
import com.oracle.truffle.r.runtime.ops.na.NACheck;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

@ExportLibrary(InteropLibrary.class)
public final class RIntVector extends RAbstractNumericVector {

    private int length;

    RIntVector(int[] data, boolean complete) {
        super(complete);
        setData(new RIntArrayVectorData(data, complete), data.length);
        assert RAbstractVector.verifyVector(this);
    }

    RIntVector(int[] data, boolean complete, int[] dims, RStringVector names, RList dimNames) {
        this(data, complete);
        initDimsNamesDimNames(dims, names, dimNames);
    }

    RIntVector(Object data, int length) {
        super(false);
        setData(data, length);
    }

    private RIntVector() {
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
        shareable = data instanceof RIntArrayVectorData || data instanceof RIntNativeVectorData;
        // Only array storage strategy is handling the complete flag dynamically,
        // for other strategies, the complete flag is determined solely by the type of the strategy
        if (!(data instanceof RIntArrayVectorData)) {
            // only sequences are always complete, everything else is always incomplete
            setComplete(data instanceof RIntSeqVectorData);
        }
    }

    static RIntVector fromNative(long address, int length) {
        RIntVector result = new RIntVector();
        NativeDataAccess.toNative(result);
        NativeDataAccess.setNativeContents(result, address, length);
        result.setData(new RIntNativeVectorData(result), length);
        return result;
    }

    public static RIntVector createSequence(int start, int stride, int length) {
        return new RIntVector(new RIntSeqVectorData(start, stride, length), length);
    }

    public static RIntVector createClosure(RAbstractVector delegate, boolean keepAttrs) {
        RIntVector result = new RIntVector(new RIntVecClosureData(delegate), delegate.getLength());
        if (keepAttrs) {
            result.initAttributes(delegate.getAttributes());
        } else {
            RClosures.initRegAttributes(result, delegate);
        }
        return result;
    }

    public static RIntVector createForeignWrapper(Object foreign) {
        RIntForeignObjData data = new RIntForeignObjData(foreign);
        return new RIntVector(data, VectorDataLibrary.getFactory().getUncached().getLength(data));
    }

    @Override
    public boolean isMaterialized() {
        return VectorDataLibrary.getFactory().getUncached().isWriteable(this.data);
    }

    // TODO: method that breaks encapsulation of data

    @Override
    public boolean isSequence() {
        return data instanceof RIntSeqVectorData;
    }

    @Override
    public RIntSeqVectorData getSequence() {
        return (RIntSeqVectorData) data;
    }

    @Override
    public boolean isClosure() {
        return data instanceof RIntVecClosureData;
    }

    @Override
    public RClosure getClosure() {
        return (RIntVecClosureData) data;
    }

    @Override
    public boolean isForeignWrapper() {
        return data instanceof RIntForeignObjData;
    }

    // ---------------------

    @Override
    protected boolean isScalarNA() {
        assert getLength() == 1;
        return RRuntime.isNA(getDataAt(0));
    }

    @Override
    public RType getRType() {
        return RType.Integer;
    }

    @Override
    public RAbstractVector castSafe(RType type, ConditionProfile isNAProfile, boolean keepAttributes) {
        switch (type) {
            case Integer:
                return this;
            case Double:
                if (isSequence()) {
                    RIntSeqVectorData seq = getSequence();
                    return RDataFactory.createDoubleSequence(seq.getStart(), seq.getStride(), getLength());
                } else {
                    return RClosures.createToDoubleVector(this, keepAttributes);
                }
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
    public Object getInternalStore() {
        return data;
    }

    public int getDataAt(int index) {
        return VectorDataLibrary.getFactory().getUncached().getIntAt(data, index);
    }

    public int getDataAt(Object store, int index) {
        assert data == store;
        return VectorDataLibrary.getFactory().getUncached().getIntAt(getData(), index);
    }

    public void setDataAt(Object store, int index, int value) {
        assert data == store;
        VectorDataLibrary.getFactory().getUncached().setIntAt(getData(), index, value);
    }

    public RIntVector copyResetData(int[] newData) {
        boolean isComplete = true;
        for (int i = 0; i < newData.length; i++) {
            if (RRuntime.isNA(newData[i])) {
                isComplete = false;
                break;
            }
        }
        RIntVector result = new RIntVector(newData, isComplete);
        setAttributes(result);
        return result;
    }

    @Override
    @ExportMessage.Ignore
    public int getLength() {
        return length;
    }

    @Override
    public void setLength(int l) {
        try {
            NativeDataAccess.setDataLength(this, getArrayForNativeDataAccess(), l);
        } finally {
            setData(new RIntNativeVectorData(this), l);
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
            setData(new RIntNativeVectorData(this), getLength());
            setComplete(false);
        }
    }

    @Override
    public int[] getDataCopy() {
        return VectorDataLibrary.getFactory().getUncached().getIntDataCopy(data);
    }

    @Override
    public int[] getInternalManagedData() {
        if (data instanceof RIntNativeVectorData) {
            return null;
        }
        // TODO: get rid of this method
        assert data instanceof RIntArrayVectorData : data.getClass().getName();
        return ((RIntArrayVectorData) data).getReadonlyIntData();
    }

    @Override
    public int[] getReadonlyData() {
        return VectorDataLibrary.getFactory().getUncached().getReadonlyIntData(data);
    }

    private RIntVector updateDataAt(int index, int value, NACheck valueNACheck) {
        assert !this.isShared();
        assert !RRuntime.isNA(value) || valueNACheck.isEnabled();
        VectorDataLibrary.getFactory().getUncached().setIntAt(data, index, value, InputNACheck.fromNACheck(valueNACheck));
        assert !isComplete() || !RRuntime.isNA(value);
        return this;
    }

    @Override
    public RIntVector updateDataAtAsObject(int i, Object o, NACheck naCheck) {
        return updateDataAt(i, (Integer) o, naCheck);
    }

    @Override
    @ExportMessage.Ignore
    public RIntVector materialize() {
        if (VectorDataLibrary.getFactory().getUncached().isWriteable(data)) {
            return this;
        }
        // To retain the semantics of the original materialize, for sequences and such we return new
        // vector
        return new RIntVector(getDataCopy(), isComplete());
    }

    public void materializeData(VectorDataLibrary dataLib) {
        setData(dataLib.materialize(data), getLength());
    }

    @Override
    public RIntVector createEmptySameType(int newLength, boolean newIsComplete) {
        return new RIntVector(new int[newLength], newIsComplete);
    }

    @Override
    public void transferElementSameType(int toIndex, RAbstractVector fromVector, int fromIndex) {
        VectorDataLibrary lib = VectorDataLibrary.getFactory().getUncached();
        int value = lib.getIntAt(((RIntVector) fromVector).data, fromIndex);
        lib.setIntAt(data, toIndex, value);
    }

    @CompilerDirectives.TruffleBoundary
    protected void copyAttributes(RIntVector materializedVec) {
        materializedVec.copyAttributesFrom(this);
    }

    @Override
    protected RIntVector internalCopy() {
        return RDataFactory.createIntVector(getDataCopy(), isComplete());
    }

    // XXX HACK taken from RAbstractVector
    RIntVector copy(VectorDataLibrary dataLib) {
        RIntVector result = RDataFactory.createIntVector(dataLib.getIntDataCopy(data), dataLib.isComplete(data));
        MemoryCopyTracer.reportCopying(this, result);
        setAttributes(result);
        result.setTypedValueInfo(getTypedValueInfo());
        return result;
    }

    @Override
    protected RIntVector internalCopyResized(int size, boolean fillNA, int[] dimensions) {
        int[] localData = getReadonlyData();
        int[] newData = Arrays.copyOf(localData, size);
        newData = resizeData(newData, localData, localData.length, fillNA);
        return RDataFactory.createIntVector(newData, isResizedComplete(size, fillNA), dimensions);
    }

    protected static int[] resizeData(int[] newData, int[] oldData, int oldDataLength, boolean fillNA) {
        if (newData.length > oldDataLength) {
            if (fillNA) {
                for (int i = oldDataLength; i < newData.length; i++) {
                    newData[i] = RRuntime.INT_NA;
                }
            } else {
                assert oldDataLength > 0 : "cannot call resize on empty vector if fillNA == false";
                for (int i = oldDataLength, j = 0; i < newData.length; ++i, j = Utils.incMod(j, oldDataLength)) {
                    newData[i] = oldData[j];
                }
            }
        }
        return newData;
    }

    @Override
    public int[] getDataTemp() {
        return (int[]) super.getDataTemp();
    }

    @Override
    public Object getDataAtAsObject(int index) {
        return getDataAt(index);
    }

    @Override
    public void setElement(int index, Object value) {
        setDataAt(getData(), index, (Integer) value);
    }

    public long allocateNativeContents() {
        try {
            setData(VectorDataLibrary.getFactory().getUncached().materialize(data), getLength());
            long result = NativeDataAccess.allocateNativeContents(this, getArrayForNativeDataAccess(), getLength());
            setData(new RIntNativeVectorData(this), getLength());
            return result;
        } finally {
            setComplete(false);
        }
    }

    private AtomicReference<RIntVector> materialized = new AtomicReference<>();

    public Object cachedMaterialize() {
        if (materialized.get() == null) {
            materialized.compareAndSet(null, materialize());
        }
        return materialized.get();
    }

    private static final class FastPathAccess extends FastPathFromIntAccess {

        @Child VectorDataLibrary dataLib;

        FastPathAccess(RAbstractContainer value) {
            super(value);
            dataLib = VectorDataLibrary.getFactory().create(((RIntVector) value).data);
        }

        @Override
        public boolean supports(Object value) {
            return super.supports(value) && dataLib.accepts(((RIntVector) value).getData());
        }

        @Override
        public int getIntImpl(AccessIterator accessIter, int index) {
            return dataLib.getIntAt(accessIter.getStore(), index);
        }

        @Override
        protected void setIntImpl(AccessIterator accessIter, int index, int value) {
            dataLib.setIntAt(accessIter.getStore(), index, value);
        }

        @Override
        protected int getLength(RAbstractContainer vector) {
            return dataLib.getLength(((RIntVector) vector).getData());
        }
    }

    @Override
    public VectorAccess access() {
        return new FastPathAccess(this);
    }

    private static final SlowPathFromIntAccess SLOW_PATH_ACCESS = new SlowPathFromIntAccess() {
        @Override
        public int getIntImpl(AccessIterator accessIter, int index) {
            RIntVector vector = (RIntVector) accessIter.getStore();
            return vector.getDataAt(index);
        }

        @Override
        protected void setIntImpl(AccessIterator accessIter, int index, int value) {
            RIntVector vector = (RIntVector) accessIter.getStore();
            vector.setDataAt(vector.getInternalStore(), index, value);
        }
    };

    @Override
    public VectorAccess slowPathAccess() {
        return SLOW_PATH_ACCESS;
    }

    // TODO: Hack: we make sure the vector is either array or native, so that we can call
    // NativeDataAccess methods
    private int[] getArrayForNativeDataAccess() {
        materializeData(VectorDataLibrary.getFactory().getUncached());
        return data instanceof RIntArrayVectorData ? ((RIntArrayVectorData) data).getReadonlyIntData() : null;
    }
}
