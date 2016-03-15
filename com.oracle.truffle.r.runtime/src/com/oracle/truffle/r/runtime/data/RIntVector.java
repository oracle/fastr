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

public final class RIntVector extends RVector implements RAbstractIntVector {

    public static final RStringVector implicitClassHeader = RDataFactory.createStringVectorFromScalar(RType.Integer.getName());

    private final int[] data;

    RIntVector(int[] data, boolean complete, int[] dims, RStringVector names) {
        super(complete, data.length, dims, names);
        this.data = data;
        assert verify();
    }

    private RIntVector(int[] data, boolean complete, int[] dims) {
        this(data, complete, dims, null);
    }

    @Override
    public RAbstractVector castSafe(RType type, ConditionProfile isNAProfile) {
        switch (type) {
            case Double:
            case Numeric:
                return RClosures.createIntToDoubleVector(this);
            case Integer:
                return this;
            case Complex:
                return RClosures.createIntToComplexVector(this);
            case Character:
                return RClosures.createIntToStringVector(this);
            case List:
                return RClosures.createAbstractVectorToListVector(this);
            default:
                return null;
        }
    }

    @Override
    public int[] getInternalStore() {
        return data;
    }

    public int getDataAt(int index) {
        return data[index];
    }

    public int getDataAt(Object store, int index) {
        assert data == store;
        return ((int[]) store)[index];
    }

    @Override
    public void setDataAt(Object store, int index, int value) {
        assert data == store;
        ((int[]) store)[index] = value;
    }

    @Override
    protected RIntVector internalCopy() {
        return new RIntVector(Arrays.copyOf(data, data.length), isComplete(), null);
    }

    public RIntVector copyResetData(int[] newData) {
        boolean isComplete = true;
        for (int i = 0; i < newData.length; i++) {
            if (RRuntime.isNA(newData[i])) {
                isComplete = false;
                break;
            }
        }
        RIntVector result = new RIntVector(newData, isComplete, null);
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
            for (int x : data) {
                if (x == RRuntime.INT_NA) {
                    return false;
                }
            }
        }
        return true;
    }

    public int[] getDataCopy() {
        return Arrays.copyOf(data, data.length);
    }

    /**
     * Intended for external calls where a copy is not needed. WARNING: think carefully before using
     * this method rather than {@link #getDataCopy()}.
     */
    public int[] getDataWithoutCopying() {
        return data;
    }

    /**
     * Return vector data (copying if necessary) that's guaranteed not to be shared with any other
     * vector instance (but maybe non-temporary in terms of vector's sharing mode).
     *
     * @return vector data
     */
    public int[] getDataNonShared() {
        return isShared() ? getDataCopy() : getDataWithoutCopying();

    }

    /**
     * Return vector data (copying if necessary) that's guaranteed to be "fresh" (temporary in terms
     * of vector sharing mode).
     *
     * @return vector data
     */
    public int[] getDataTemp() {
        return isTemporary() ? getDataWithoutCopying() : getDataCopy();
    }

    public RIntVector copyWithNewDimensions(int[] newDimensions) {
        return RDataFactory.createIntVector(data, isComplete(), newDimensions);
    }

    @Override
    protected String getDataAtAsString(int index) {
        return RRuntime.intToString(this.getDataAt(index));
    }

    public RIntVector updateDataAt(int i, int right, NACheck valueNACheck) {
        assert !this.isShared();
        data[i] = right;
        if (valueNACheck.check(right)) {
            setComplete(false);
        }
        assert !isComplete() || !RRuntime.isNA(right);
        return this;
    }

    @Override
    public RIntVector updateDataAtAsObject(int i, Object o, NACheck naCheck) {
        return updateDataAt(i, (Integer) o, naCheck);
    }

    public static int[] resizeData(int[] newData, int[] oldData, int oldDataLength, boolean fillNA) {
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
        int[] newData = Arrays.copyOf(data, size);
        return resizeData(newData, this.data, this.getLength(), fillNA);
    }

    @Override
    public RIntVector copyResized(int size, boolean fillNA) {
        boolean isComplete = isComplete() && ((data.length >= size) || !fillNA);
        return RDataFactory.createIntVector(copyResizedData(size, fillNA), isComplete);
    }

    public RIntVector materialize() {
        return this;
    }

    @Override
    public RIntVector createEmptySameType(int newLength, boolean newIsComplete) {
        return RDataFactory.createIntVector(new int[newLength], newIsComplete);
    }

    @Override
    public void transferElementSameType(int toIndex, RAbstractVector fromVector, int fromIndex) {
        RAbstractIntVector other = (RAbstractIntVector) fromVector;
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

    @Override
    public void setElement(int i, Object value) {
        data[i] = (int) value;
    }
}
