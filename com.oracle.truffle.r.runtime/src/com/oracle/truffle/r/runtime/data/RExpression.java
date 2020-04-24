/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.library.ExportMessage.Ignore;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.LoopConditionProfile;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.RandomAccessIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.RandomAccessWriteIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.SeqIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.SeqWriteIterator;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractListBaseVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.data.nodes.FastPathVectorAccess.FastPathFromListAccess;
import com.oracle.truffle.r.runtime.data.nodes.SlowPathVectorAccess.SlowPathFromListAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;
import com.oracle.truffle.r.runtime.ops.na.NACheck;
import com.oracle.truffle.r.runtime.data.RSharingAttributeStorage.Shareable;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector.RMaterializedVector;

@ExportLibrary(VectorDataLibrary.class)
@ExportLibrary(AbstractContainerLibrary.class)
@ExportLibrary(InteropLibrary.class)
public final class RExpression extends RAbstractListBaseVector implements RMaterializedVector, Shareable {

    private Object[] data;

    RExpression(Object[] data, int[] dims, RStringVector names, RList dimNames) {
        super(false);
        this.data = data;
        initDimsNamesDimNames(dims, names, dimNames);
        assert RAbstractVector.verifyVector(this);
    }

    @Override
    @ExportMessage(library = VectorDataLibrary.class)
    @ExportMessage(library = AbstractContainerLibrary.class)
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
    @Ignore // VectorDataLibrary
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
    public void setDataAt(Object store, int index, Object value) {
        updateDataAtAsObject(index, value, NACheck.getEnabled());
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
    @Ignore // VectorDataLibrary
    public Object getDataAtAsObject(int index) {
        return this.getDataAt(index);
    }

    private Object[] copyResizedData(int size, boolean fillNA) {
        Object[] localData = getReadonlyData();
        Object[] newData = Arrays.copyOf(localData, size);
        return resizeData(newData, localData, this.getLength(), fillNA);
    }

    @Override
    @Ignore // VectorDataLibrary
    public void setElement(int i, Object value) {
        setDataAt(i, value);
    }

    @Override
    public RType getRType() {
        return RType.Expression;
    }

    @Override
    @ExportMessage(library = VectorDataLibrary.class)
    public RExpression materialize() {
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
            if (RRuntime.isMaterializedVector(el)) {
                Object elCopy = ((RAbstractVector) el).deepCopy();
                listCopy.updateDataAtAsObject(i, elCopy, null);
            }
        }
        return listCopy;
    }

    @Override
    @Ignore // AbstractContainerLibrary
    public RExpression createEmptySameType(int newLength, boolean newIsComplete) {
        return RDataFactory.createExpression(newLength);
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

    // -------------------------------
    // VectorDataLibrary

    @ExportMessage
    @SuppressWarnings("static-method")
    public NACheck getNACheck() {
        // we do not maintain any completeness info about lists
        // to avoid any errors we return NACheck that is enabled:
        // checks for NAs and also reports neverSeenNA() == false
        return NACheck.getEnabled();
    }

    @ExportMessage
    @SuppressWarnings("static-method")
    public RType getType() {
        return RType.Expression;
    }

    @ExportMessage(name = "isComplete", library = VectorDataLibrary.class)
    @SuppressWarnings("static-method")
    public boolean datLibIsComplete() {
        return false;
    }

    @ExportMessage
    @SuppressWarnings("static-method")
    public boolean isWriteable() {
        return true;
    }

    @ExportMessage(name = "copy", library = VectorDataLibrary.class)
    public Object dataLibCopy(@SuppressWarnings("unused") boolean deep) {
        return copy();
    }

    @ExportMessage
    @SuppressWarnings("static-method")
    public SeqIterator iterator(@Shared("SeqItLoopProfile") @Cached("createCountingProfile()") LoopConditionProfile loopProfile) {
        SeqIterator it = new SeqIterator(getInternalStore(), getLength());
        it.initLoopConditionProfile(loopProfile);
        return it;
    }

    @ExportMessage
    @SuppressWarnings("static-method")
    public boolean nextImpl(SeqIterator it, boolean loopCondition,
                    @Shared("SeqItLoopProfile") @Cached("createCountingProfile()") LoopConditionProfile loopProfile) {
        return it.next(loopCondition, loopProfile);
    }

    @ExportMessage
    @SuppressWarnings("static-method")
    public void nextWithWrap(SeqIterator it,
                    @Cached("createBinaryProfile()") ConditionProfile wrapProfile) {
        it.nextWithWrap(wrapProfile);
    }

    @ExportMessage
    public RandomAccessIterator randomAccessIterator() {
        return new RandomAccessIterator(getInternalStore());
    }

    @ExportMessage
    public SeqWriteIterator writeIterator() {
        return new SeqWriteIterator(getInternalStore(), getLength());
    }

    @ExportMessage
    public RandomAccessWriteIterator randomAccessWriteIterator() {
        return new RandomAccessWriteIterator(getInternalStore());
    }

    @ExportMessage
    public Object[] getReadonlyListData() {
        return getReadonlyData();
    }

    @ExportMessage
    public Object[] getListDataCopy() {
        return getDataCopy();
    }

    @ExportMessage
    public Object getElementAt(int index) {
        return getDataAt(index);
    }

    @ExportMessage
    public Object getNextElement(SeqIterator it) {
        return getDataAtAsObject(it.getStore(), it.getIndex());
    }

    @ExportMessage
    public Object getElement(RandomAccessIterator it, int index) {
        return getDataAtAsObject(it.getStore(), index);
    }

    @ExportMessage
    public void setElementAt(int index, Object value) {
        setDataAt(getInternalStore(), index, value);
    }

    @ExportMessage
    public void setNextElement(SeqWriteIterator it, Object value) {
        setDataAt(it.getStore(), it.getIndex(), value);
    }

    @ExportMessage
    public void setElement(RandomAccessWriteIterator it, int index, Object value) {
        setDataAt(it.getStore(), index, value);
    }
}
