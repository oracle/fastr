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
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

public final class RLogicalVector extends RVector<byte[]> implements RAbstractLogicalVector {

    private byte[] data;

    RLogicalVector(byte[] data, boolean complete) {
        super(complete);
        this.data = data;
        assert verify();
    }

    RLogicalVector(byte[] data, boolean complete, int[] dims, RStringVector names, RList dimNames) {
        this(data, complete);
        initDimsNamesDimNames(dims, names, dimNames);
    }

    @Override
    public RAbstractVector castSafe(RType type, ConditionProfile isNAProfile, boolean keepAttributes) {
        switch (type) {
            case Logical:
                return this;
            case Integer:
                return RClosures.createToIntVector(this, keepAttributes);
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
    public byte[] getInternalStore() {
        assert data != null;
        return data;
    }

    @Override
    public void setDataAt(Object store, int index, byte value) {
        assert data == store;
        if (store == null) {
            NativeDataAccess.setIntData(this, index, RRuntime.logical2int(value));
        } else {
            ((byte[]) store)[index] = value;
        }
    }

    @Override
    public byte getDataAt(Object store, int index) {
        assert data == store;
        return data == null ? (byte) NativeDataAccess.getIntData(this, index) : data[index];
    }

    @Override
    protected RLogicalVector internalCopy() {
        assert data != null;
        return new RLogicalVector(Arrays.copyOf(data, data.length), isComplete());
    }

    public RLogicalVector copyResetData(byte[] newData) {
        boolean isComplete = true;
        for (int i = 0; i < newData.length; i++) {
            if (RRuntime.isNA(newData[i])) {
                isComplete = false;
                break;
            }
        }
        RLogicalVector result = new RLogicalVector(newData, isComplete);
        setAttributes(result);
        return result;
    }

    @Override
    public int getLength() {
        return data == null ? (int) NativeDataAccess.getDataLength(this) : data.length;
    }

    @Override
    public String toString() {
        return toString(i -> RRuntime.logicalToString(getDataAt(i)));
    }

    @Override
    public boolean verify() {
        if (isComplete()) {
            for (int i = 0; i < getLength(); i++) {
                if (getDataAt(i) == RRuntime.LOGICAL_NA) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public byte getDataAt(int index) {
        return data == null ? (byte) NativeDataAccess.getIntData(this, index) : data[index];
    }

    private RLogicalVector updateDataAt(int index, byte value, NACheck valueNACheck) {
        assert !this.isShared();
        if (data == null) {
            NativeDataAccess.setIntData(this, index, RRuntime.logical2int(value));
        } else {
            data[index] = value;
        }
        if (valueNACheck.check(value)) {
            setComplete(false);
        }
        assert !isComplete() || !RRuntime.isNA(value);
        return this;
    }

    @Override
    public RLogicalVector updateDataAtAsObject(int i, Object o, NACheck naCheck) {
        return updateDataAt(i, (Byte) o, naCheck);

    }

    private byte[] copyResizedData(int size, boolean fillNA) {
        assert data != null;
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
    protected RLogicalVector internalCopyResized(int size, boolean fillNA, int[] dimensions) {
        boolean isComplete = isComplete() && ((getLength() >= size) || !fillNA);
        return RDataFactory.createLogicalVector(copyResizedData(size, fillNA), isComplete, dimensions);
    }

    @Override
    public RLogicalVector createEmptySameType(int newLength, boolean newIsComplete) {
        return RDataFactory.createLogicalVector(new byte[newLength], newIsComplete);
    }

    @Override
    public void transferElementSameType(int toIndex, RAbstractVector fromVector, int fromIndex) {
        RAbstractLogicalVector other = (RAbstractLogicalVector) fromVector;
        if (data == null) {
            NativeDataAccess.setIntData(this, toIndex, RRuntime.logical2int(other.getDataAt(fromIndex)));
        } else {
            data[toIndex] = other.getDataAt(fromIndex);
        }
    }

    @Override
    public byte[] getDataCopy() {
        assert data != null;
        return Arrays.copyOf(data, data.length);
    }

    /**
     * Intended for external calls where a copy is not needed. WARNING: think carefully before using
     * this method rather than {@link #getDataCopy()}.
     */
    @Override
    public byte[] getDataWithoutCopying() {
        assert data != null;
        return data;
    }

    @Override
    public RLogicalVector copyWithNewDimensions(int[] newDimensions) {
        assert data != null;
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

    public long allocateNativeContents() {
        try {
            return NativeDataAccess.allocateNativeContents(this, data, getLength());
        } finally {
            data = null;
        }
    }
}
