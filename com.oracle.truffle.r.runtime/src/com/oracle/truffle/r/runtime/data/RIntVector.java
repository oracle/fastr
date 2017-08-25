/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.runtime.data;

import java.util.Arrays;

import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.data.closures.RClosures;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

public final class RIntVector extends RVector<int[]> implements RAbstractIntVector {

    private int[] data;

    RIntVector(int[] data, boolean complete) {
        super(complete);
        this.data = data;
        assert verify();
    }

    RIntVector(int[] data, boolean complete, int[] dims, RStringVector names, RList dimNames) {
        this(data, complete);
        initDimsNamesDimNames(dims, names, dimNames);
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
    public int[] getInternalStore() {
        assert data != null;
        return data;
    }

    @Override
    public int getDataAt(int index) {
        return NativeDataAccess.getData(this, data, index);
    }

    @Override
    public int getDataAt(Object store, int index) {
        assert data == store;
        return NativeDataAccess.getData(this, (int[]) store, index);
    }

    @Override
    public void setDataAt(Object store, int index, int value) {
        assert data == store;
        NativeDataAccess.setData(this, (int[]) store, index, value);
    }

    @Override
    protected RIntVector internalCopy() {
        if (data != null) {
            return new RIntVector(Arrays.copyOf(data, data.length), isComplete());
        } else {
            return new RIntVector(getDataCopy(), isComplete());
        }
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
        return NativeDataAccess.getDataLength(this, data);
    }

    @Override
    public String toString() {
        return toString(i -> Double.toString(getDataAt(i)));
    }

    @Override
    public boolean verify() {
        if (isComplete()) {
            for (int i = 0; i < getLength(); i++) {
                if (RRuntime.isNA(getDataAt(i))) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public int[] getDataCopy() {
        if (data != null) {
            return Arrays.copyOf(data, data.length);
        } else {
            return NativeDataAccess.copyIntNativeData(getNativeMirror());
        }
    }

    @Override
    public int[] getInternalManagedData() {
        return data;
    }

    @Override
    public int[] getReadonlyData() {
        if (data != null) {
            return data;
        } else {
            return NativeDataAccess.copyIntNativeData(getNativeMirror());
        }
    }

    @Override
    public RIntVector copyWithNewDimensions(int[] newDimensions) {
        return RDataFactory.createIntVector(getReadonlyData(), isComplete(), newDimensions);
    }

    public RIntVector updateDataAt(int index, int value, NACheck valueNACheck) {
        assert !this.isShared();

        NativeDataAccess.setData(this, data, index, value);
        if (valueNACheck.check(value)) {
            setComplete(false);
        }
        assert !isComplete() || !RRuntime.isNA(value);
        return this;
    }

    @Override
    public RIntVector updateDataAtAsObject(int i, Object o, NACheck naCheck) {
        return updateDataAt(i, (Integer) o, naCheck);
    }

    static int[] resizeData(int[] newData, int[] oldData, int oldDataLength, boolean fillNA) {
        if (newData.length > oldDataLength) {
            if (fillNA) {
                for (int i = oldDataLength; i < newData.length; i++) {
                    newData[i] = RRuntime.INT_NA;
                }
            } else {
                for (int i = oldDataLength, j = 0; i < newData.length; ++i, j = Utils.incMod(j, oldDataLength)) {
                    newData[i] = oldData[j];
                }
            }
        }
        return newData;
    }

    private int[] copyResizedData(int size, boolean fillNA) {
        int[] newData = Arrays.copyOf(getReadonlyData(), size);
        return resizeData(newData, this.data, this.getLength(), fillNA);
    }

    @Override
    protected RIntVector internalCopyResized(int size, boolean fillNA, int[] dimensions) {
        boolean isComplete = isComplete() && ((getLength() >= size) || !fillNA);
        return RDataFactory.createIntVector(copyResizedData(size, fillNA), isComplete, dimensions);
    }

    @Override
    public RIntVector materialize() {
        return this;
    }

    @Override
    public RIntVector createEmptySameType(int newLength, boolean newIsComplete) {
        return RDataFactory.createIntVector(new int[newLength], newIsComplete);
    }

    @Override
    public void transferElementSameType(int toIndex, RAbstractVector fromVector, int fromIndex) {
        NativeDataAccess.setData(this, data, toIndex, ((RAbstractIntVector) fromVector).getDataAt(fromIndex));
    }

    @Override
    public Object getDataAtAsObject(int index) {
        return getDataAt(index);
    }

    @Override
    public void setElement(int index, Object value) {
        NativeDataAccess.setData(this, data, index, (int) value);
    }

    public long allocateNativeContents() {
        try {
            return NativeDataAccess.allocateNativeContents(this, data, getLength());
        } finally {
            data = null;
            complete = false;
        }
    }
}
