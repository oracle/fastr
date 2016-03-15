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

public final class RDoubleVector extends RVector implements RAbstractDoubleVector {

    public static final RStringVector implicitClassHeader = RDataFactory.createStringVectorFromScalar(RType.Numeric.getName());

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
    public RAbstractVector castSafe(RType type, ConditionProfile isNAProfile) {
        switch (type) {
            case Double:
            case Numeric:
                return this;
            case Integer:
                return RClosures.createDoubleToIntVector(this);
            case Complex:
                return RClosures.createDoubleToComplexVector(this);
            case Character:
                return RClosures.createDoubleToStringVector(this);
            case List:
                return RClosures.createAbstractVectorToListVector(this);
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

    public void setDataAt(Object store, int index, double value) {
        assert data == store;
        ((double[]) store)[index] = value;
    }

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
        return toString(i -> RRuntime.doubleToString(getDataAt(i)));
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

    public double getDataAt(int i) {
        return data[i];
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

    public RDoubleVector copyWithNewDimensions(int[] newDimensions) {
        return RDataFactory.createDoubleVector(data, isComplete(), newDimensions);
    }

    @Override
    protected String getDataAtAsString(int index) {
        return RRuntime.doubleToString(data[index]);
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
    public RDoubleVector copyResized(int size, boolean fillNA) {
        boolean isComplete = isComplete() && ((data.length >= size) || !fillNA);
        return RDataFactory.createDoubleVector(copyResizedData(size, fillNA), isComplete);
    }

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

    @Override
    public RStringVector getImplicitClass() {
        return getClassHierarchyHelper(implicitClassHeader);
    }
}
