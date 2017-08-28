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
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

public final class RComplexVector extends RVector<double[]> implements RAbstractComplexVector {

    private double[] data;

    RComplexVector(double[] data, boolean complete) {
        super(complete);
        assert data.length % 2 == 0;
        this.data = data;
        assert verify();
    }

    RComplexVector(double[] data, boolean complete, int[] dims, RStringVector names, RList dimNames) {
        this(data, complete);
        initDimsNamesDimNames(dims, names, dimNames);
    }

    @Override
    protected RComplexVector internalCopy() {
        assert data != null;
        return new RComplexVector(Arrays.copyOf(data, data.length), this.isComplete());
    }

    @Override
    public double[] getInternalManagedData() {
        return data;
    }

    @Override
    public double[] getInternalStore() {
        return data;
    }

    @Override
    public int getLength() {
        return NativeDataAccess.getDataLength(this, data);
    }

    @Override
    public RAbstractVector castSafe(RType type, ConditionProfile isNAProfile, boolean keepAttributes) {
        switch (type) {
            case Complex:
                return this;
            case Character:
                return RClosures.createToStringVector(this, keepAttributes);
            case List:
                return RClosures.createToListVector(this, keepAttributes);
            default:
                return null;
        }
    }

    @Override
    public void setDataAt(Object store, int index, RComplex value) {
        assert data == store;
        NativeDataAccess.setData(this, (double[]) store, index, value.getRealPart(), value.getImaginaryPart());
    }

    @Override
    public RComplex getDataAt(Object store, int index) {
        assert data == store;
        return NativeDataAccess.getData(this, (double[]) store, index);
    }

    @Override
    public RComplex getDataAt(int index) {
        return NativeDataAccess.getData(this, data, index);
    }

    @Override
    public String toString() {
        return toString(i -> getDataAt(i).toString());
    }

    @Override
    public boolean verify() {
        if (isComplete()) {
            for (int i = 0; i < getLength(); i++) {
                if (getDataAt(i).isNA()) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public double[] getDataCopy() {
        assert data != null;
        return Arrays.copyOf(data, data.length);
    }

    @Override
    public double[] getReadonlyData() {
        assert data != null;
        return data;
    }

    @Override
    public RComplexVector copyWithNewDimensions(int[] newDimensions) {
        assert data != null;
        return RDataFactory.createComplexVector(data, isComplete(), newDimensions);
    }

    private RComplexVector updateDataAt(int index, RComplex value, NACheck rightNACheck) {
        assert !this.isShared();
        NativeDataAccess.setData(this, data, index, value.getRealPart(), value.getImaginaryPart());
        if (rightNACheck.check(value)) {
            setComplete(false);
        }
        assert !isComplete() || !RRuntime.isNA(value);
        return this;
    }

    @Override
    public RComplexVector updateDataAtAsObject(int i, Object o, NACheck naCheck) {
        return updateDataAt(i, (RComplex) o, naCheck);
    }

    private double[] copyResizedData(int size, boolean fillNA) {
        assert data != null;
        int csize = size << 1;
        double[] newData = Arrays.copyOf(data, csize);
        if (csize > this.getLength()) {
            if (fillNA) {
                for (int i = data.length; i < size; i++) {
                    newData[i] = RRuntime.DOUBLE_NA;
                }
            } else {
                for (int i = data.length, j = 0; i <= csize - 2; i += 2, j = Utils.incMod(j + 1, data.length)) {
                    newData[i] = data[j];
                    newData[i + 1] = data[j + 1];
                }
            }
        }
        return newData;
    }

    @Override
    protected RComplexVector internalCopyResized(int size, boolean fillNA, int[] dimensions) {
        assert data != null;
        boolean isComplete = isComplete() && ((data.length >= size) || !fillNA);
        return RDataFactory.createComplexVector(copyResizedData(size, fillNA), isComplete, dimensions);
    }

    @Override
    public RComplexVector materialize() {
        return this;
    }

    @Override
    public RComplexVector createEmptySameType(int newLength, boolean newIsComplete) {
        return RDataFactory.createComplexVector(new double[newLength << 1], newIsComplete);
    }

    @Override
    public void transferElementSameType(int toIndex, RAbstractVector fromVector, int fromIndex) {
        RAbstractComplexVector other = (RAbstractComplexVector) fromVector;
        RComplex value = other.getDataAt(fromIndex);
        NativeDataAccess.setData(this, data, toIndex, value.getRealPart(), value.getImaginaryPart());
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
