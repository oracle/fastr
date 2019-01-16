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
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractListBaseVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.data.nodes.FastPathVectorAccess.FastPathFromListAccess;
import com.oracle.truffle.r.runtime.data.nodes.SlowPathVectorAccess.SlowPathFromListAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

public final class RExpression extends RVector<Object[]> implements RAbstractListBaseVector {

    private Object[] data;

    RExpression(Object[] data, int[] dims, RStringVector names, RList dimNames) {
        super(false);
        this.data = data;
        initDimsNamesDimNames(dims, names, dimNames);
        assert RAbstractVector.verify(this);
    }

    @Override
    public int getLength() {
        return data.length;
    }

    @Override
    public void setLength(int l) {
        if (l == data.length) {
            return;
        }
        Object[] newData = new Object[l];
        System.arraycopy(data, 0, newData, 0, l < data.length ? l : data.length);
        for (int i = data.length; i < l; i++) {
            newData[i] = RNull.instance;
        }
        data = newData;
    }

    @Override
    public int getTrueLength() {
        return data.length;
    }

    @Override
    public void setTrueLength(int truelength) {
        // should not be called on an expression
        throw RInternalError.shouldNotReachHere();
    }

    @Override
    public Object[] getInternalStore() {
        return data;
    }

    @Override
    public Object getDataAtAsObject(Object store, int index) {
        assert store == data;
        return ((Object[]) store)[index];
    }

    public void setDataAt(int index, Object value) {
        data[index] = value;
    }

    @Override
    public Object[] getInternalManagedData() {
        return getInternalStore();
    }

    @Override
    public Object[] getReadonlyData() {
        return data;
    }

    @Override
    public Object[] getDataCopy() {
        Object[] copy = new Object[data.length];
        System.arraycopy(data, 0, copy, 0, data.length);
        return copy;
    }

    /**
     * Note: elements inside lists may be in inconsistent state reference counting wise. You may
     * need to put them into consistent state depending on what you use them for, consult the
     * documentation of {@code ExtractListElement}.
     */
    @Override
    public Object getDataAt(int i) {
        return data[i];
    }

    @Override
    public RExpression updateDataAtAsObject(int i, Object o, NACheck naCheck) {
        assert !this.isShared() : "data in shared list must not be updated, make a copy";
        assert o != null : "expressions must not contain nulls";
        data[i] = o;
        return this;

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

    private Object[] copyResizedData(int size, boolean fillNA) {
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
    public void setElement(int i, Object value) {
        setDataAt(i, value);
    }

    @Override
    public RType getRType() {
        return RType.Expression;
    }

    @Override
    public RVector<?> materialize() {
        return this;
    }

    @Override
    @TruffleBoundary
    protected RExpression internalCopy() {
        return new RExpression(Arrays.copyOf(data, data.length), getDimensions(), null, null);
    }

    @Override
    @TruffleBoundary
    protected RExpression internalDeepCopy() {
        // TOOD: only used for nested list updates, but still could be made faster (through a
        // separate AST node?)
        RExpression listCopy = new RExpression(Arrays.copyOf(data, data.length), getDimensions(), null, null);
        for (int i = 0; i < listCopy.getLength(); i++) {
            Object el = listCopy.getDataAt(i);
            if (el instanceof RVector) {
                Object elCopy = ((RVector<?>) el).deepCopy();
                listCopy.updateDataAtAsObject(i, elCopy, null);
            }
        }
        return listCopy;
    }

    @Override
    public RExpression createEmptySameType(int newLength, boolean newIsComplete) {
        return RDataFactory.createExpression(newLength);
    }

    @Override
    public RExpression copyWithNewDimensions(int[] newDimensions) {
        return RDataFactory.createExpression(data, newDimensions);
    }

    @Override
    protected RExpression internalCopyResized(int size, boolean fillNA, int[] dimensions) {
        return RDataFactory.createExpression(copyResizedData(size, fillNA), dimensions);
    }

    private static final class FastPathAccess extends FastPathFromListAccess {

        FastPathAccess(RAbstractContainer value) {
            super(value);
        }

        @Override
        public RType getType() {
            return RType.Expression;
        }

        @Override
        protected Object getListElementImpl(AccessIterator accessIter, int index) {
            return ((Object[]) accessIter.getStore())[index];
        }

        @Override
        protected void setListElementImpl(AccessIterator accessIter, int index, Object value) {
            ((Object[]) accessIter.getStore())[index] = value;
        }
    }

    @Override
    public VectorAccess access() {
        return new FastPathAccess(this);
    }

    private static final SlowPathFromListAccess SLOW_PATH_ACCESS = new SlowPathFromListAccess() {
        @Override
        public RType getType() {
            return RType.Expression;
        }

        @Override
        protected Object getListElementImpl(AccessIterator accessIter, int index) {
            return ((RExpression) accessIter.getStore()).data[index];
        }

        @Override
        protected void setListElementImpl(AccessIterator accessIter, int index, Object value) {
            ((RExpression) accessIter.getStore()).data[index] = value;
        }
    };

    @Override
    public VectorAccess slowPathAccess() {
        return SLOW_PATH_ACCESS;
    }
}
