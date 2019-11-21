/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.closures.RClosures;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.data.nodes.FastPathVectorAccess.FastPathFromIntAccess;
import com.oracle.truffle.r.runtime.data.nodes.SlowPathVectorAccess.SlowPathFromIntAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

public final class RIntVector extends RAbstractIntVector {

    private RIntVectorData data;

    RIntVector(int[] data, boolean complete) {
        super(complete);
        this.data = new RIntArrayVectorData(data, complete);
        assert RAbstractVector.verifyVector(this);
    }

    RIntVector(int[] data, boolean complete, int[] dims, RStringVector names, RList dimNames) {
        this(data, complete);
        initDimsNamesDimNames(dims, names, dimNames);
    }

    private RIntVector(RIntVectorData data) {
        super(false);
        this.data = data;
    }

    private RIntVector() {
        super(false);
    }

    static RIntVector fromNative(long address, int length) {
        RIntVector result = new RIntVector();
        NativeDataAccess.toNative(result);
        NativeDataAccess.setNativeContents(result, address, length);
        return result;
    }

    public static RIntVector createSequence(int start, int stride, int length) {
        return new RIntVector(new RIntSeqVectorData(start, stride, length));
    }

    public RIntVectorData getData() {
        return data;
    }

    // To be removed

    @Override
    public boolean isSequence() {
        return data instanceof RIntSeqVectorData;
    }

    @Override
    public RIntSeqVectorData getSequence() {
        return (RIntSeqVectorData) data;
    }

    // End: to be removed

    @Override
    protected boolean isScalarNA() {
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
    public Object getInternalStore() {
        return data;
    }

    public int getDataAt(int index) {
        return data.getIntAt(index);
    }

    public int getDataAt(Object store, int index) {
        assert data == store;
        return ((RIntVectorData) store).getIntAt(index);
    }

    public void setDataAt(Object store, int index, int value) {
        assert data == store;
        ((RIntVectorData) store).setIntAt(index, value);
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
    public int getLength() {
        return data.getLength();
    }

    @Override
    public void setLength(int l) {
        try {
            NativeDataAccess.setDataLength(this, getArrayForNativeDataAccess(), l);
        } finally {
            data = null;
            complete = false;
        }
    }

    @Override
    public int getTrueLength() {
        return NativeDataAccess.getTrueDataLength(this);
    }

    @Override
    public void setTrueLength(int l) {
        NativeDataAccess.setTrueDataLength(this, l);
    }

    @Override
    public int[] getDataCopy() {
        return RIntVectorDataLibrary.getFactory().getUncached().getIntDataCopy(data);
    }

    @Override
    public int[] getInternalManagedData() {
        // TODO: get rid of this method
        assert data instanceof RIntArrayVectorData;
        return ((RIntArrayVectorData) data).getReadonlyIntData();
    }

    @Override
    public int[] getReadonlyData() {
        return RIntVectorDataLibrary.getFactory().getUncached().getReadonlyIntData(data);
    }

    private RIntVector updateDataAt(int index, int value, NACheck valueNACheck) {
        assert !this.isShared();
        RIntVectorDataLibrary.getFactory().getUncached().setIntAt(data, index, value, valueNACheck);
        assert !isComplete() || !RRuntime.isNA(value);
        return this;
    }

    @Override
    public RIntVector updateDataAtAsObject(int i, Object o, NACheck naCheck) {
        return updateDataAt(i, (Integer) o, naCheck);
    }

    @Override
    public RIntVector materialize() {
        if (RIntVectorDataLibrary.getFactory().getUncached().isWriteable(data)) {
            return this;
        }
        // To retain the semantics of the original materialize, for sequences and such we return new vector
        return new RIntVector(getDataCopy(), isComplete());
    }

    public void materializeData(RIntVectorDataLibrary dataLib) {
        data = dataLib.materialize(data);
    }

    @Override
    public RIntVector createEmptySameType(int newLength, boolean newIsComplete) {
        return new RIntVector(new int[newLength], newIsComplete);
    }

    @Override
    public void transferElementSameType(int toIndex, RAbstractVector fromVector, int fromIndex) {
        RIntVectorDataLibrary lib = RIntVectorDataLibrary.getFactory().getUncached();
        int value = lib.getIntAt(data, fromIndex);
        lib.setIntAt(((RIntVector) fromVector).data, toIndex, value);
    }

    @Override
    public Object getDataAtAsObject(int index) {
        return getDataAt(index);
    }

    @Override
    public void setElement(int index, Object value) {
        data.setIntAt(index, (Integer) value);
    }

    public long allocateNativeContents() {
        try {
            data = RIntVectorDataLibrary.getFactory().getUncached().materialize(data);
            long result = NativeDataAccess.allocateNativeContents(this, getArrayForNativeDataAccess(), getLength());
            data = new RIntNativeVectorData(this);
            return result;
        } finally {
            complete = false;
        }
    }

    private static final class FastPathAccess extends FastPathFromIntAccess {

        @Child RIntVectorDataLibrary dataLib;

        FastPathAccess(RAbstractContainer value) {
            super(value);
            dataLib = RIntVectorDataLibrary.getFactory().create(((RIntVector) value).data);
        }

        @Override
        public boolean supports(Object value) {
            return super.supports(value) && dataLib.accepts(value);
        }

        @Override
        public int getIntImpl(AccessIterator accessIter, int index) {
            return dataLib.getIntAt((RIntVectorData) accessIter.getStore(), index);
        }

        @Override
        protected void setIntImpl(AccessIterator accessIter, int index, int value) {
            dataLib.setIntAt((RIntVectorData) accessIter.getStore(), index, value);
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

    // TODO: Hack: we make sure the vector is either array or native, so that we can call NativeDataAccess methods
    private int[] getArrayForNativeDataAccess() {
        materializeData(RIntVectorDataLibrary.getFactory().getUncached());
        return data instanceof RIntArrayVectorData ? ((RIntArrayVectorData) data).getReadonlyIntData() : null;
    }
}
