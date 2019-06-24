/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
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
import com.oracle.truffle.r.runtime.data.closures.RClosures;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.data.nodes.FastPathVectorAccess.FastPathFromLogicalAccess;
import com.oracle.truffle.r.runtime.data.nodes.SlowPathVectorAccess.SlowPathFromLogicalAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;
import com.oracle.truffle.r.runtime.ops.na.NACheck;
import com.oracle.truffle.r.runtime.data.RSharingAttributeStorage.Shareable;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector.RMaterializedVector;

public final class RLogicalVector extends RAbstractLogicalVector implements RMaterializedVector, Shareable {

    private byte[] data;

    RLogicalVector(byte[] data, boolean complete) {
        super(complete);
        this.data = data;
        assert RAbstractVector.verifyVector(this);
    }

    RLogicalVector(byte[] data, boolean complete, int[] dims, RStringVector names, RList dimNames) {
        this(data, complete);
        initDimsNamesDimNames(dims, names, dimNames);
    }

    private RLogicalVector() {
        super(false);
    }

    static RLogicalVector fromNative(long address, int length) {
        RLogicalVector result = new RLogicalVector();
        NativeDataAccess.asPointer(result);
        NativeDataAccess.setNativeContents(result, address, length);
        return result;
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
    public byte[] getInternalManagedData() {
        return data;
    }

    @Override
    public byte[] getInternalStore() {
        return data;
    }

    @Override
    public void setDataAt(Object store, int index, byte value) {
        assert data == store;
        NativeDataAccess.setData(this, (byte[]) store, index, value);
    }

    @Override
    public byte getDataAt(Object store, int index) {
        assert data == store;
        return NativeDataAccess.getData(this, (byte[]) store, index);
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
        return NativeDataAccess.getDataLength(this, data);
    }

    @Override
    public int getTrueLength() {
        return NativeDataAccess.getTrueDataLength(this);
    }

    @Override
    public void setLength(int l) {
        try {
            NativeDataAccess.setDataLength(this, data, l);
        } finally {
            data = null;
            complete = false;
        }
    }

    @Override
    public void setTrueLength(int l) {
        NativeDataAccess.setTrueDataLength(this, l);
    }

    @Override
    public byte getDataAt(int index) {
        return NativeDataAccess.getData(this, data, index);
    }

    private RLogicalVector updateDataAt(int index, byte value, NACheck valueNACheck) {
        assert !this.isShared();
        NativeDataAccess.setData(this, data, index, value);
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

    @Override
    public void transferElementSameType(int toIndex, RAbstractVector fromVector, int fromIndex) {
        NativeDataAccess.setData(this, data, toIndex, ((RAbstractLogicalVector) fromVector).getDataAt(fromIndex));
    }

    @Override
    public byte[] getDataCopy() {
        if (data != null) {
            return Arrays.copyOf(data, data.length);
        } else {
            return getNativeDataCopy();
        }
    }

    @Override
    public byte[] getReadonlyData() {
        if (data != null) {
            return data;
        } else {
            return getNativeDataCopy();
        }
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
            complete = false;
        }
    }

    private byte[] getNativeDataCopy() {
        assert data == null;
        int length = getLength();
        byte[] result = new byte[length];
        for (int i = 0; i < length; i++) {
            result[i] = getDataAt(i);
        }
        return result;
    }

    private static final class FastPathAccess extends FastPathFromLogicalAccess {

        FastPathAccess(RAbstractContainer value) {
            super(value);
        }

        @Override
        protected byte getLogicalImpl(AccessIterator accessIter, int index) {
            return hasStore ? ((byte[]) accessIter.getStore())[index] : NativeDataAccess.getLogicalNativeMirrorData(accessIter.getStore(), index);
        }

        @Override
        protected void setLogicalImpl(AccessIterator accessIter, int index, byte value) {
            if (hasStore) {
                ((byte[]) accessIter.getStore())[index] = value;
            } else {
                NativeDataAccess.setNativeMirrorLogicalData(accessIter.getStore(), index, value);
            }
        }
    }

    @Override
    public VectorAccess access() {
        return new FastPathAccess(this);
    }

    private static final SlowPathFromLogicalAccess SLOW_PATH_ACCESS = new SlowPathFromLogicalAccess() {
        @Override
        protected byte getLogicalImpl(AccessIterator accessIter, int index) {
            RLogicalVector vector = (RLogicalVector) accessIter.getStore();
            return NativeDataAccess.getData(vector, vector.data, index);
        }

        @Override
        protected void setLogicalImpl(AccessIterator accessIter, int index, byte value) {
            RLogicalVector vector = (RLogicalVector) accessIter.getStore();
            NativeDataAccess.setData(vector, vector.data, index, value);
        }
    };

    @Override
    public VectorAccess slowPathAccess() {
        return SLOW_PATH_ACCESS;
    }
}
