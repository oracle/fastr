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

import java.util.Arrays;

import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.data.closures.RClosures;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

public final class RLogicalVector extends RVector implements RAbstractLogicalVector {

    public static final RStringVector implicitClassHeader = RDataFactory.createStringVectorFromScalar(RType.Logical.getClazz());

    private final byte[] data;

    RLogicalVector(byte[] data, boolean complete, int[] dims, RStringVector names) {
        super(complete, data.length, dims, names);
        this.data = data;
        assert verify();
    }

    private RLogicalVector(byte[] data, boolean complete, int[] dims) {
        this(data, complete, dims, null);
    }

    @Override
    public RAbstractVector castSafe(RType type, ConditionProfile isNAProfile) {
        switch (type) {
            case Logical:
                return this;
            case Double:
                return RClosures.createLogicalToDoubleVector(this);
            case Integer:
                return RClosures.createLogicalToIntVector(this);
            case Complex:
                return RClosures.createLogicalToComplexVector(this);
            case Character:
                return RClosures.createLogicalToStringVector(this);
            case List:
                return RClosures.createAbstractVectorToListVector(this);
            default:
                return null;
        }
    }

    @Override
    public byte[] getInternalStore() {
        return data;
    }

    @Override
    public void setDataAt(Object store, int index, byte value) {
        assert data == store;
        ((byte[]) store)[index] = value;
    }

    @Override
    public byte getDataAt(Object store, int index) {
        assert data == store;
        return ((byte[]) store)[index];
    }

    @Override
    protected RLogicalVector internalCopy() {
        return new RLogicalVector(Arrays.copyOf(data, data.length), isComplete(), null);
    }

    public RLogicalVector copyResetData(byte[] newData) {
        boolean isComplete = true;
        for (int i = 0; i < newData.length; i++) {
            if (RRuntime.isNA(newData[i])) {
                isComplete = false;
                break;
            }
        }
        RLogicalVector result = new RLogicalVector(newData, isComplete, null);
        setAttributes(result);
        return result;
    }

    @Override
    public int getLength() {
        return data.length;
    }

    @Override
    public String toString() {
        return toString(i -> RRuntime.logicalToString(getDataAt(i)));
    }

    @Override
    protected boolean internalVerify() {
        if (isComplete()) {
            for (byte b : data) {
                if (b == RRuntime.LOGICAL_NA) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public byte getDataAt(int i) {
        return data[i];
    }

    @Override
    protected String getDataAtAsString(int index) {
        return RRuntime.logicalToString(this.getDataAt(index));
    }

    private RLogicalVector updateDataAt(int index, byte right, NACheck valueNACheck) {
        assert !this.isShared();
        data[index] = right;
        if (valueNACheck.check(right)) {
            setComplete(false);
        }
        assert !isComplete() || !RRuntime.isNA(right);
        return this;
    }

    @Override
    public RLogicalVector updateDataAtAsObject(int i, Object o, NACheck naCheck) {
        return updateDataAt(i, (Byte) o, naCheck);

    }

    private byte[] copyResizedData(int size, boolean fillNA) {
        byte[] newData = Arrays.copyOf(data, size);
        if (size > this.getLength()) {
            if (fillNA) {
                for (int i = data.length; i < size; i++) {
                    newData[i] = RRuntime.LOGICAL_NA;
                }
            } else {
                for (int i = data.length, j = 0; i < size; ++i, j = Utils.incMod(j, data.length)) {
                    newData[i] = data[j];
                }
            }
        }
        return newData;
    }

    @Override
    public RLogicalVector copyResized(int size, boolean fillNA) {
        boolean isComplete = isComplete() && ((data.length >= size) || !fillNA);
        return RDataFactory.createLogicalVector(copyResizedData(size, fillNA), isComplete);
    }

    @Override
    public RLogicalVector createEmptySameType(int newLength, boolean newIsComplete) {
        return RDataFactory.createLogicalVector(new byte[newLength], newIsComplete);
    }

    @Override
    public void transferElementSameType(int toIndex, RAbstractVector fromVector, int fromIndex) {
        RAbstractLogicalVector other = (RAbstractLogicalVector) fromVector;
        data[toIndex] = other.getDataAt(fromIndex);
    }

    public byte[] getDataCopy() {
        return Arrays.copyOf(data, data.length);
    }

    /**
     * Intended for external calls where a copy is not needed. WARNING: think carefully before using
     * this method rather than {@link #getDataCopy()}.
     */
    public byte[] getDataWithoutCopying() {
        return data;
    }

    /**
     * Return vector data (copying if necessary) that's guaranteed not to be shared with any other
     * vector instance (but maybe non-temporary in terms of vector's sharing mode).
     *
     * @return vector data
     */
    public byte[] getDataNonShared() {
        return isShared() ? getDataCopy() : getDataWithoutCopying();

    }

    /**
     * Return vector data (copying if necessary) that's guaranteed to be "fresh" (temporary in terms
     * of vector sharing mode).
     *
     * @return vector data
     */
    public byte[] getDataTemp() {
        return isTemporary() ? getDataWithoutCopying() : getDataCopy();
    }

    @Override
    public RLogicalVector copyWithNewDimensions(int[] newDimensions) {
        return RDataFactory.createLogicalVector(data, isComplete(), newDimensions);
    }

    @Override
    public RLogicalVector materialize() {
        return this;
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
