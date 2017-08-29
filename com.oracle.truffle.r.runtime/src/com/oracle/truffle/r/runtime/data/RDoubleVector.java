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
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

public final class RDoubleVector extends RVector<double[]> implements RAbstractDoubleVector {

    private final double[] data;

    RDoubleVector(double[] data, boolean complete, int[] dims, RStringVector names) {
        super(complete, data.length, dims, names);
        this.data = data;
        assert verify();
    }

    private RDoubleVector(double[] data, boolean complete, int[] dims) {
        this(data, complete, dims, null);
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
    protected RDoubleVector internalCopy() {
        return new RDoubleVector(Arrays.copyOf(data, data.length), this.isComplete(), null);
    }

    @Override
    public double[] getInternalStore() {
        return data;
    }

    @Override
    public void setDataAt(Object store, int index, double value) {
        assert data == store;
        ((double[]) store)[index] = value;
    }

    @Override
    public double getDataAt(Object store, int index) {
        assert data == store;
        return ((double[]) store)[index];
    }

    public RDoubleVector copyResetData(double[] newData) {
        boolean isComplete = true;
        for (int i = 0; i < newData.length; i++) {
            if (RRuntime.isNA(newData[i])) {
                isComplete = false;
                break;
            }
        }
        RDoubleVector result = new RDoubleVector(newData, isComplete, null);
        setAttributes(result);
        return result;
    }

    @Override
    public int getLength() {
        return data.length;
    }

    @Override
    public String toString() {
        return toString(i -> Double.toString(getDataAt(i)));
    }

    @Override
    public boolean verify() {
        if (isComplete()) {
            for (double d : data) {
                if (d == RRuntime.DOUBLE_NA) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public double getDataAt(int i) {
        return data[i];
    }

    @Override
    public double[] getDataCopy() {
        return Arrays.copyOf(data, data.length);
    }

    /**
     * Intended for external calls where a copy is not needed. WARNING: think carefully before using
     * this method rather than {@link #getDataCopy()}.
     */
    @Override
    public double[] getDataWithoutCopying() {
        return data;
    }

    @Override
    public RDoubleVector copyWithNewDimensions(int[] newDimensions) {
        return RDataFactory.createDoubleVector(data, isComplete(), newDimensions);
    }

    public RDoubleVector updateDataAt(int i, double right, NACheck valueNACheck) {
        assert !this.isShared();
        data[i] = right;
        if (valueNACheck.check(right)) {
            complete = false;
        }
        assert !isComplete() || !RRuntime.isNA(right);
        return this;
    }

    @Override
    public RDoubleVector updateDataAtAsObject(int i, Object o, NACheck naCheck) {
        return updateDataAt(i, (Double) o, naCheck);

    }

    static double[] resizeData(double[] newData, double[] oldData, int oldDataLength, boolean fillNA) {
        if (newData.length > oldDataLength) {
            if (fillNA) {
                for (int i = oldDataLength; i < newData.length; i++) {
                    newData[i] = RRuntime.DOUBLE_NA;
                }
            } else {
                for (int i = oldDataLength, j = 0; i < newData.length; ++i, j = Utils.incMod(j, oldDataLength)) {
                    newData[i] = oldData[j];
                }
            }
        }
        return newData;
    }

    private double[] copyResizedData(int size, boolean fillNA) {
        double[] newData = Arrays.copyOf(data, size);
        return resizeData(newData, this.data, this.getLength(), fillNA);
    }

    @Override
    protected RDoubleVector internalCopyResized(int size, boolean fillNA, int[] dimensions) {
        boolean isComplete = isComplete() && ((data.length >= size) || !fillNA);
        return RDataFactory.createDoubleVector(copyResizedData(size, fillNA), isComplete, dimensions);
    }

    @Override
    public RDoubleVector materialize() {
        return this;
    }

    @Override
    public RDoubleVector createEmptySameType(int newLength, boolean newIsComplete) {
        return RDataFactory.createDoubleVector(new double[newLength], newIsComplete);
    }

    @Override
    public void transferElementSameType(int toIndex, RAbstractVector fromVector, int fromIndex) {
        RAbstractDoubleVector other = (RAbstractDoubleVector) fromVector;
        data[toIndex] = other.getDataAt(fromIndex);
    }

    @Override
    public Object getDataAtAsObject(int index) {
        return getDataAt(index);
    }
}
