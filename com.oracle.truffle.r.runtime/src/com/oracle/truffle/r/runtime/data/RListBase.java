/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.data.model.RAbstractListBaseVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

/**
 * Note on sharing mode for list elements: by default the sharing state of elements in a list can be
 * inconsistent, e.g. a list referenced by one local variable may contain temporary vectors, or list
 * references by more variables (shared list) may contain non-shared elements. The sharing state of
 * the elements should be made consistent on reading from the list by the callers! When we read from
 * shared list, we make the element shared. When we read from non-shared list, we make the element
 * at least non-shared. There is no possible way, how a list can contain a non-shared element not
 * owned by it, given that any element of some other list must be first read into variable (the
 * extraction from list makes it at least non-shared, then the write makes it shared) and only then
 * it can be put inside another list. This is however not true for internal code, which may read
 * data from a list and then put it into another list, in such case it is responsibility of the code
 * to increment the refcount of such data. Consult also the documentation of
 * {@code ExtractListElement}, which is a node that can extract an element of a list or abstract
 * vector and put it in the consistent sharing state.
 */
public abstract class RListBase extends RVector<Object[]> implements RAbstractListBaseVector {

    /**
     * After nativized, the data array degenerates to a reference holder.
     */
    protected Object[] data;

    RListBase(Object[] data) {
        super(false);
        this.data = data;
        assert RAbstractVector.verify(this);
    }

    RListBase(Object[] data, int[] dims, RStringVector names, RList dimNames) {
        this(data);
        initDimsNamesDimNames(dims, names, dimNames);
    }

    boolean isNativized() {
        return NativeDataAccess.isAllocated(this);
    }

    @Override
    public final int getLength() {
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
    public void setDataAt(Object store, int index, Object valueArg) {
        assert valueArg != null : "lists must not contain nulls";
        Object value = valueArg;
        assert store == getInternalStore();
        NativeDataAccess.setData(this, ((Object[]) store), index, value);
    }

    @Override
    public Object[] getInternalManagedData() {
        return getInternalStore();
    }

    /**
     * Unlike atomic vectors, {@code RList} cannot have a native memory mirror, therefore this is a
     * fast operation.
     */
    @Override
    public final Object[] getReadonlyData() {
        if (!isNativized()) {
            return data;
        } else {
            return NativeDataAccess.copyListNativeData(getNativeMirror());
        }
    }

    public final Object[] getDataWithoutCopying() {
        return getReadonlyData();
    }

    @Override
    public final Object[] getDataCopy() {
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
    public final Object getDataAt(int i) {
        return NativeDataAccess.getData(this, getInternalStore(), i);
    }

    public final RListBase updateDataAt(int i, Object right, @SuppressWarnings("unused") NACheck rightNACheck) {
        assert !this.isShared() : "data in shared list must not be updated, make a copy";
        assert right != null : "lists must not contain nulls";
        NativeDataAccess.setData(this, data, i, right);
        return this;
    }

    @Override
    public final RListBase updateDataAtAsObject(int i, Object o, NACheck naCheck) {
        return updateDataAt(i, o, naCheck);

    }

    @Override
    public final void transferElementSameType(int toIndex, RAbstractVector fromVector, int fromIndex) {
        RAbstractListVector other = (RAbstractListVector) fromVector;
        setDataAt(toIndex, other.getDataAt(fromIndex));
    }

    /**
     * Note: elements inside lists may be in inconsistent state reference counting wise. You may
     * need to put them into consistent state depending on what you use them for, consult the
     * documentation of {@code ExtractListElement}.
     */
    @Override
    public final Object getDataAtAsObject(int index) {
        return this.getDataAt(index);
    }

    protected final Object[] copyResizedData(int size, boolean fillNA) {
        Object[] localData = getReadonlyData();
        Object[] newData = Arrays.copyOf(localData, size);
        return resizeData(newData, localData, this.getLength(), fillNA);
    }

    private static Object[] resizeData(Object[] newData, Object[] oldData, int oldDataLength, boolean fillNA) {
        if (newData.length > oldDataLength) {
            if (fillNA) {
                for (int i = oldDataLength; i < newData.length; i++) {
                    newData[i] = RNull.instance;
                }
            } else {
                assert oldDataLength > 0 : "cannot call resize on empty vector if fillNA == false";
                for (int i = oldData.length, j = 0; i < newData.length; ++i, j = Utils.incMod(j, oldData.length)) {
                    newData[i] = oldData[j];
                }
            }
        }
        return newData;
    }

    @Override
    public final void setElement(int i, Object value) {
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

}
