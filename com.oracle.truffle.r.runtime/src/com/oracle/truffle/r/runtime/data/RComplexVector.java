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

    private final double[] data;

    RComplexVector(double[] data, boolean complete, int[] dims, RStringVector names) {
        super(complete, data.length >> 1, dims, names);
        assert data.length % 2 == 0;
        this.data = data;
        assert verify();
    }

    private RComplexVector(double[] data, boolean complete, int[] dims) {
        this(data, complete, dims, null);
    }

    @Override
    protected RComplexVector internalCopy() {
        return new RComplexVector(Arrays.copyOf(data, data.length), this.isComplete(), null);
    }

    @Override
    public double[] getInternalStore() {
        return data;
    }

    @Override
    public int getLength() {
        return data.length >> 1;
    }

    @Override
    public RAbstractVector castSafe(RType type, ConditionProfile isNAProfile) {
        switch (type) {
            case Complex:
                return this;
            case Character:
                return RClosures.createToStringVector(this);
            case List:
                return RClosures.createToListVector(this);
            default:
                return null;
        }
    }

    @Override
    public void setDataAt(Object store, int index, RComplex value) {
        assert data == store;
        double[] array = (double[]) store;
        array[index << 1] = value.getRealPart();
        array[(index << 1) + 1] = value.getImaginaryPart();
    }

    @Override
    public RComplex getDataAt(Object store, int i) {
        assert data == store;
        double[] doubleStore = (double[]) store;
        int index = i << 1;
        return RDataFactory.createComplex(doubleStore[index], doubleStore[index + 1]);
    }

    @Override
    public RComplex getDataAt(int i) {
        return getDataAt(data, i);
    }

    @Override
    public String toString() {
        return toString(i -> getDataAt(i).toString());
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
    public RComplexVector copyWithNewDimensions(int[] newDimensions) {
        return RDataFactory.createComplexVector(data, isComplete(), newDimensions);
    }

    private RComplexVector updateDataAt(int i, RComplex right, NACheck rightNACheck) {
        assert !this.isShared();
        int index = i << 1;
        data[index] = right.getRealPart();
        data[index + 1] = right.getImaginaryPart();
        if (rightNACheck.check(right)) {
            setComplete(false);
        }
        assert !isComplete() || !RRuntime.isNA(right);
        return this;
    }

    @Override
    public RComplexVector updateDataAtAsObject(int i, Object o, NACheck naCheck) {
        return updateDataAt(i, (RComplex) o, naCheck);
    }

    private double[] copyResizedData(int size, boolean fillNA) {
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
        int toIndex2 = toIndex << 1;
        RComplex value = other.getDataAt(fromIndex);
        data[toIndex2] = value.getRealPart();
        data[toIndex2 + 1] = value.getImaginaryPart();
    }
}
