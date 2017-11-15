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
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.data.closures.RClosures;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.data.nodes.FastPathVectorAccess.FastPathFromStringAccess;
import com.oracle.truffle.r.runtime.data.nodes.SlowPathVectorAccess.SlowPathFromStringAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

public final class RStringVector extends RVector<String[]> implements RAbstractStringVector {

    private final String[] data;

    RStringVector(String[] data, boolean complete) {
        super(complete);
        this.data = data;
        assert verify();
    }

    RStringVector(String[] data, boolean complete, int[] dims, RStringVector names, RList dimNames) {
        this(data, complete);
        initDimsNamesDimNames(dims, names, dimNames);
    }

    @Override
    public RAbstractVector castSafe(RType type, ConditionProfile isNAProfile, boolean keepAttributes) {
        switch (type) {
            case Character:
                return this;
            case List:
                return RClosures.createToListVector(this, keepAttributes);
            default:
                return null;
        }
    }

    @Override
    public String[] getInternalManagedData() {
        return data;
    }

    @Override
    public String[] getInternalStore() {
        assert data != null : "support for native memory backed vectors is not complete";
        return data;
    }

    @Override
    public void setDataAt(Object store, int index, String value) {
        assert data == store;
        ((String[]) store)[index] = value;
    }

    public void setDataAt(int index, String value) {
        data[index] = value;
    }

    @Override
    public String getDataAt(Object store, int index) {
        assert data == store;
        return ((String[]) store)[index];
    }

    @Override
    protected RStringVector internalCopy() {
        return new RStringVector(Arrays.copyOf(data, data.length), isComplete());
    }

    @Override
    public int getLength() {
        return data.length;
    }

    @Override
    public String[] getDataCopy() {
        String[] copy = new String[data.length];
        System.arraycopy(data, 0, copy, 0, data.length);
        return copy;
    }

    public RStringVector copyResetData(String[] newData) {
        boolean isComplete = true;
        for (int i = 0; i < newData.length; i++) {
            if (RRuntime.isNA(newData[i])) {
                isComplete = false;
                break;
            }
        }
        RStringVector result = new RStringVector(newData, isComplete);
        setAttributes(result);
        return result;
    }

    @Override
    public String[] getReadonlyData() {
        return data;
    }

    @Override
    public boolean verify() {
        if (isComplete()) {
            for (String b : data) {
                if (b == RRuntime.STRING_NA) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public String getDataAt(int i) {
        return data[i];
    }

    public RStringVector updateDataAt(int i, String right, NACheck rightNACheck) {
        if (this.isShared()) {
            throw RInternalError.shouldNotReachHere("update shared vector");
        }
        data[i] = right;
        if (rightNACheck.check(right)) {
            setComplete(false);
        }
        assert !isComplete() || !RRuntime.isNA(right);
        return this;
    }

    @Override
    public RStringVector updateDataAtAsObject(int i, Object o, NACheck naCheck) {
        return updateDataAt(i, (String) o, naCheck);

    }

    private String[] copyResizedData(int size, String fill) {
        String[] newData = Arrays.copyOf(data, size);
        if (size > this.getLength()) {
            if (fill != null) {
                for (int i = data.length; i < size; i++) {
                    newData[i] = fill;
                }
            } else {
                for (int i = data.length, j = 0; i < size; ++i, j = Utils.incMod(j, data.length)) {
                    newData[i] = data[j];
                }
            }
        }
        return newData;
    }

    private String[] createResizedData(int size, String fill) {
        return copyResizedData(size, fill);
    }

    @Override
    protected RStringVector internalCopyResized(int size, boolean fillNA, int[] dimensions) {
        boolean isComplete = isComplete() && ((data.length >= size) || !fillNA);
        return RDataFactory.createStringVector(copyResizedData(size, fillNA ? RRuntime.STRING_NA : null), isComplete, dimensions);
    }

    public RStringVector resizeWithEmpty(int size) {
        return RDataFactory.createStringVector(createResizedData(size, RRuntime.NAMES_ATTR_EMPTY_VALUE), isComplete());
    }

    @Override
    public RStringVector createEmptySameType(int newLength, boolean newIsComplete) {
        return RDataFactory.createStringVector(new String[newLength], newIsComplete);
    }

    @Override
    public void transferElementSameType(int toIndex, RAbstractVector fromVector, int fromIndex) {
        RAbstractStringVector other = (RAbstractStringVector) fromVector;
        data[toIndex] = other.getDataAt(fromIndex);
    }

    @Override
    public RStringVector copyWithNewDimensions(int[] newDimensions) {
        return RDataFactory.createStringVector(data, isComplete(), newDimensions);
    }

    @Override
    public RStringVector materialize() {
        return this;
    }

    @Override
    public Object getDataAtAsObject(int index) {
        return getDataAt(index);
    }

    @Override
    public void setElement(int i, Object value) {
        data[i] = (String) value;
    }

    private static final class FastPathAccess extends FastPathFromStringAccess {

        FastPathAccess(RAbstractContainer value) {
            super(value);
        }

        @Override
        protected String getString(Object store, int index) {
            assert hasStore;
            return ((String[]) store)[index];
        }

        @Override
        protected void setString(Object store, int index, String value) {
            assert hasStore;
            ((String[]) store)[index] = value;
        }
    }

    @Override
    public VectorAccess access() {
        return new FastPathAccess(this);
    }

    private static final SlowPathFromStringAccess SLOW_PATH_ACCESS = new SlowPathFromStringAccess() {
        @Override
        protected String getString(Object store, int index) {
            return ((RStringVector) store).data[index];
        }

        @Override
        protected void setString(Object store, int index, String value) {
            ((RStringVector) store).data[index] = value;
        }
    };

    @Override
    public VectorAccess slowPathAccess() {
        return SLOW_PATH_ACCESS;
    }
}
