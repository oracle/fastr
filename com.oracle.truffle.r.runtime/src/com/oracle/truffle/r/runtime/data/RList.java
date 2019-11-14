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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.NativeDataAccess.NativeMirror;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.data.nodes.FastPathVectorAccess.FastPathFromListAccess;
import com.oracle.truffle.r.runtime.data.nodes.SlowPathVectorAccess.SlowPathFromListAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;
import com.oracle.truffle.r.runtime.ops.na.NACheck;
import com.oracle.truffle.r.runtime.data.RSharingAttributeStorage.Shareable;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector.RMaterializedVector;

/**
 * A note on the RList complete flag {@link RAbstractVector#isComplete() } - it is always
 * initialized with {@code false} and never expected to change.
 */
public final class RList extends RAbstractListVector implements RMaterializedVector, Shareable {

    /**
     * After nativized, the data array degenerates to a reference holder.
     */
    private Object[] data;

    RList(Object[] data) {
        super(false);
        assert data.getClass().isAssignableFrom(Object[].class) : data;
        this.data = data;
        assert RAbstractVector.verifyVector(this);
    }

    RList(Object[] data, int[] dims, RStringVector names, RList dimNames) {
        this(data);
        initDimsNamesDimNames(dims, names, dimNames);
    }

    boolean isNativized() {
        return NativeDataAccess.isAllocated(this);
    }

    @Override
    public RType getRType() {
        return RType.List;
    }

    @Override
    public int getLength() {
        return NativeDataAccess.getDataLength(this, getInternalStore());
    }

    @Override
    public void setLength(int l) {
        if (!isNativized()) {
            if (l != data.length) {
                try {
                    Object[] newData = new Object[l];
                    System.arraycopy(data, 0, newData, 0, l < data.length ? l : data.length);
                    for (int i = data.length; i < l; i++) {
                        newData[i] = RNull.instance;
                    }
                    NativeDataAccess.setDataLength(this, data, l);
                } finally {
                    assert NativeDataAccess.isAllocated(this);
                    complete = false;
                }
            }
        } else {
            NativeDataAccess.setDataLength(this, null, l);
        }
    }

    @Override
    public int getTrueLength() {
        return NativeDataAccess.getTrueDataLength(this);
    }

    @Override
    public void setTrueLength(int truelength) {
        NativeDataAccess.setTrueDataLength(this, truelength);
    }

    @Override
    public Object[] getInternalStore() {
        return isNativized() ? null : data;
    }

    /**
     * Note: elements inside lists may be in inconsistent state reference counting wise. You may
     * need to put them into consistent state depending on what you use them for, consult the
     * documentation of {@code ExtractListElement}.
     */
    @Override
    public Object getDataAtAsObject(Object store, int index) {
        assert store == data;
        return ((Object[]) store)[index];
    }

    public void setDataAt(int index, Object value) {
        setDataAt(data, index, value);
    }

    @Override
    public void setDataAt(Object store, int index, Object value) {
        assert value != null : "lists must not contain nulls";
        assert isNativized() || store == getInternalStore();
        NativeDataAccess.setData(this, ((Object[]) store), index, value);
    }

    @Override
    public Object[] getInternalManagedData() {
        return getInternalStore();
    }

    @Override
    public Object[] getReadonlyData() {
        if (!isNativized()) {
            return data;
        } else {
            return NativeDataAccess.copyListNativeData(getNativeMirror());
        }
    }

    public Object[] getDataWithoutCopying() {
        return getReadonlyData();
    }

    @Override
    public Object[] getDataCopy() {
        if (!isNativized()) {
            Object[] copy = new Object[data.length];
            System.arraycopy(data, 0, copy, 0, data.length);
            return copy;
        } else {
            return NativeDataAccess.copyListNativeData(getNativeMirror());
        }
    }

    /**
     * Note: elements inside lists may be in inconsistent state reference counting wise. You may
     * need to put them into consistent state depending on what you use them for, consult the
     * documentation of {@code ExtractListElement}.
     */
    @Override
    public Object getDataAt(int i) {
        return NativeDataAccess.getData(this, getInternalStore(), i);
    }

    public RList updateDataAt(int i, Object right, @SuppressWarnings("unused") NACheck rightNACheck) {
        assert !this.isShared() : "data in shared list must not be updated, make a copy";
        assert right != null : "lists must not contain nulls";
        NativeDataAccess.setData(this, data, i, right);
        return this;
    }

    @Override
    public RList updateDataAtAsObject(int i, Object o, NACheck naCheck) {
        return updateDataAt(i, o, naCheck);

    }

    @Override
    public void transferElementSameType(int toIndex, RAbstractVector fromVector, int fromIndex) {
        RAbstractListVector other = (RAbstractListVector) fromVector;
        setDataAt(toIndex, other.getDataAt(fromIndex));
    }

    /**
     * Note: elements inside lists may be in inconsistent state reference counting wise. You may
     * need to put them into consistent state depending on what you use them for, consult the
     * documentation of {@code ExtractListElement}.
     */
    @Override
    public Object getDataAtAsObject(int index) {
        return this.getDataAt(index);
    }

    @Override
    public void setElement(int i, Object value) {
        setDataAt(i, value);
    }

    public long allocateNativeContents() {
        try {
            return NativeDataAccess.allocateNativeContents(this, getInternalStore(), data.length);
        } finally {
            assert NativeDataAccess.isAllocated(this);
            complete = false;
        }
    }

    @Override
    public RList materialize() {
        return this;
    }

    @Override
    protected RList internalCopy() {
        Object[] localData = getDataWithoutCopying();
        return new RList(Arrays.copyOf(localData, localData.length), getDimensionsInternal(), null, null);
    }

    @TruffleBoundary
    private int[] getDimensionsInternal() {
        return getDimensions();
    }

    @Override
    protected RList internalDeepCopy() {
        // TOOD: only used for nested list updates, but still could be made faster (through a
        // separate AST node?)
        Object[] localData = getDataWithoutCopying();
        RList listCopy = new RList(Arrays.copyOf(localData, localData.length), getDimensionsInternal(), null, null);
        for (int i = 0; i < listCopy.getLength(); i++) {
            Object el = listCopy.getDataAt(i);
            if (el instanceof RMaterializedVector) {
                Object elCopy = ((RAbstractVector) el).deepCopy();
                listCopy.updateDataAt(i, elCopy, null);
            }
        }
        return listCopy;
    }

    private static final class FastPathAccess extends FastPathFromListAccess {

        FastPathAccess(RAbstractContainer value) {
            super(value);
        }

        @Override
        public RType getType() {
            return RType.List;
        }

        @Override
        protected Object getListElementImpl(AccessIterator accessIter, int index) {
            if (hasStore) {
                return ((Object[]) accessIter.getStore())[index];
            } else {
                return NativeDataAccess.getListElementNativeMirrorData((NativeMirror) accessIter.getStore(), index);
            }
        }

        @Override
        protected void setListElementImpl(AccessIterator accessIter, int index, Object value) {
            if (hasStore) {
                ((Object[]) accessIter.getStore())[index] = value;
            } else {
                NativeDataAccess.setNativeMirrorListData((NativeMirror) accessIter.getStore(), index, value);
            }
        }
    }

    @Override
    public VectorAccess access() {
        return new FastPathAccess(this);
    }

    private static final SlowPathFromListAccess SLOW_PATH_ACCESS = new SlowPathFromListAccess() {
        @Override
        public RType getType() {
            return RType.List;
        }

        @Override
        protected Object getListElementImpl(AccessIterator accessIter, int index) {
            return ((RList) accessIter.getStore()).getDataAt(index);
        }

        @Override
        protected void setListElementImpl(AccessIterator accessIter, int index, Object value) {
            ((RList) accessIter.getStore()).setDataAt(index, value);
        }
    };

    @Override
    public VectorAccess slowPathAccess() {
        return SLOW_PATH_ACCESS;
    }
}
