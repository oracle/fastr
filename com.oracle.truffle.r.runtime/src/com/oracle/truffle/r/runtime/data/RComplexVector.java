/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.util.*;

import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.closures.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.ops.na.*;

public final class RComplexVector extends RVector implements RAbstractComplexVector {

    public static final RStringVector implicitClassHeader = RDataFactory.createStringVectorFromScalar(RType.Complex.getName());

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
                return RClosures.createComplexToStringVector(this);
            case List:
                return RClosures.createAbstractVectorToListVector(this);
            default:
                return null;
        }
    }

    public void setDataAt(Object store, int index, RComplex value) {
        assert data == store;
        double[] array = (double[]) store;
        array[index << 1] = value.getRealPart();
        array[(index << 1) + 1] = value.getImaginaryPart();
    }

    public RComplex getDataAt(Object store, int i) {
        assert data == store;
        double[] doubleStore = (double[]) store;
        int index = i << 1;
        return RDataFactory.createComplex(doubleStore[index], doubleStore[index + 1]);
    }

    public RComplex getDataAt(int i) {
        return getDataAt(data, i);
    }

    @Override
    public String toString() {
        return toString(i -> RRuntime.complexToString(getDataAt(i)));
    }

    @Override
    protected boolean internalVerify() {
        if (isComplete()) {
            for (double d : data) {
                if (d == RRuntime.DOUBLE_NA) {
                    return false;
                }
            }
        }
        return true;
    }

    public double[] getDataCopy() {
        double[] copy = new double[data.length];
        System.arraycopy(data, 0, copy, 0, data.length);
        return copy;
    }

    /**
     * Intended for external calls where a copy is not needed. WARNING: think carefully before using
     * this method rather than {@link #getDataCopy()}.
     */
    public double[] getDataWithoutCopying() {
        return data;
    }

    /**
     * Return vector data (copying if necessary) that's guaranteed not to be shared with any other
     * vector instance (but maybe non-temporary in terms of vector's sharing mode).
     *
     * @return vector data
     */
    public double[] getDataNonShared() {
        return isShared() ? getDataCopy() : getDataWithoutCopying();

    }

    /**
     * Return vector data (copying if necessary) that's guaranteed to be "fresh" (temporary in terms
     * of vector sharing mode).
     *
     * @return vector data
     */
    public double[] getDataTemp() {
        return isTemporary() ? getDataWithoutCopying() : getDataCopy();
    }

    public RComplexVector copyWithNewDimensions(int[] newDimensions) {
        return RDataFactory.createComplexVector(data, isComplete(), newDimensions);
    }

    @Override
    protected String getDataAtAsString(int index) {
        return getDataAt(index).toString();
    }

    public RComplexVector updateDataAt(int i, RComplex right, NACheck rightNACheck) {
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
    public RComplexVector copyResized(int size, boolean fillNA) {
        boolean isComplete = isComplete() && ((data.length >= size) || !fillNA);
        return RDataFactory.createComplexVector(copyResizedData(size, fillNA), isComplete);
    }

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

    @Override
    public RStringVector getImplicitClass() {
        return getClassHierarchyHelper(implicitClassHeader);
    }
}
