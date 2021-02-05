/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.library.ExportMessage.Ignore;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.RSharingAttributeStorage.Shareable;
import com.oracle.truffle.r.runtime.data.closures.RClosures;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector.RMaterializedVector;
import com.oracle.truffle.r.runtime.data.nodes.FastPathVectorAccess.FastPathFromListAccess;
import com.oracle.truffle.r.runtime.data.nodes.SlowPathVectorAccess.SlowPathFromListAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

/**
 * A note on the RList complete flag {@link RAbstractVector#isComplete() } - it is always
 * initialized with {@code false} and never expected to change.
 */
@ExportLibrary(AbstractContainerLibrary.class)
public final class RList extends RAbstractListVector implements RMaterializedVector, Shareable {
    private int length;

    RList(Object[] data) {
        assert data.getClass().isAssignableFrom(Object[].class) : data;
        setData(data, data.length);
        assert RAbstractVector.verifyVector(this);
    }

    RList(Object[] data, int[] dims, RStringVector names, RList dimNames) {
        this(data);
        initDimsNamesDimNames(dims, names, dimNames);
    }

    private RList(Object data, int length) {
        // if data is array => it must be Object array
        assert !data.getClass().isArray() || data.getClass().isAssignableFrom(Object[].class) : data;
        setData(data, length);
        assert RAbstractVector.verifyVector(this);
    }

    public static RList createForeignWrapper(TruffleObject obj, int size) {
        return new RList(new RListForeignObjData(obj), size);
    }

    public static RList createClosure(RAbstractVector delegate, boolean keepAttrs) {
        RList result = new RList(VectorDataClosure.fromVector(delegate, RType.List), delegate.getLength());
        if (keepAttrs) {
            result.initAttributes(delegate.getAttributes());
        } else {
            RClosures.initRegAttributes(result, delegate);
        }
        return result;
    }

    private void setData(Object data, int newLen) {
        // Temporary solution to keep getLength() fast
        // The assumption is that length of vectors can only change in infrequently used setLength
        // operation where we update the field accordingly
        length = newLen;
        super.setData(data);
    }

    boolean isNativized() {
        return NativeDataAccess.isAllocated(this);
    }

    @Override
    public RType getRType() {
        return RType.List;
    }

    @Override
    @Ignore // VectorDataLibrary
    public int getLength() {
        return length;
    }

    @Override
    public void setLength(int l) {
        allocateNativeContents();
        NativeDataAccess.setDataLength(this, null, l);
        length = l;
    }

    @Override
    public int getTrueLength() {
        return NativeDataAccess.getTrueDataLength(this);
    }

    @Override
    public void setTrueLength(int truelength) {
        allocateNativeContents();
        NativeDataAccess.setTrueDataLength(this, truelength);
    }

    @Override
    public Object getInternalStore() {
        return data;
    }

    /**
     * Note: elements inside lists may be in inconsistent state reference counting wise. You may
     * need to put them into consistent state depending on what you use them for, consult the
     * documentation of {@code ExtractListElement}.
     */
    @Override
    @Ignore // VectorDataLibrary
    public Object getDataAtAsObject(Object store, int index) {
        assert store == getInternalStore();
        return VectorDataLibrary.getFactory().getUncached().getElementAt(data, index);
    }

    public void setDataAt(int index, Object value) {
        setDataAt(data, index, value);
    }

    @Override
    public void setDataAt(Object store, int index, Object value) {
        assert value != null : "lists must not contain nulls";
        assert isNativized() || store == getInternalStore();
        VectorDataLibrary.getFactory().getUncached().setElementAt(data, index, value);
    }

    @Override
    public Object[] getInternalManagedData() {
        return (Object[]) data;
    }

    @Override
    public Object[] getReadonlyData() {
        return VectorDataLibrary.getFactory().getUncached().getReadonlyListData(data);
    }

    @Override
    public Object[] getDataCopy() {
        return VectorDataLibrary.getFactory().getUncached().getListDataCopy(data);
    }

    /**
     * Note: elements inside lists may be in inconsistent state reference counting wise. You may
     * need to put them into consistent state depending on what you use them for, consult the
     * documentation of {@code ExtractListElement}.
     */
    @Override
    public Object getDataAt(int i) {
        return VectorDataLibrary.getFactory().getUncached().getElementAt(data, i);
    }

    public RList updateDataAt(int i, Object right, @SuppressWarnings("unused") NACheck rightNACheck) {
        assert !this.isShared() : "data in shared list must not be updated, make a copy";
        assert right != null : "lists must not contain nulls";
        setDataAt(getInternalStore(), i, right);
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
    @Ignore // VectorDataLibrary
    public Object getDataAtAsObject(int index) {
        return this.getDataAt(index);
    }

    @Override
    @Ignore // VectorDataLibrary
    public void setElement(int i, Object value) {
        setDataAt(i, value);
    }

    public long allocateNativeContents() {
        if (!NativeDataAccess.isAllocated(this)) {
            assert data instanceof Object[]; // this assumes only two impls. list data: array and
                                             // native memory
            Object[] arr = (Object[]) data;
            NativeDataAccess.allocateNativeContents(this, arr, arr.length);
            setData(new RListNativeData(this, arr), arr.length);
            assert NativeDataAccess.isAllocated(this);
        }
        return NativeDataAccess.getNativeDataAddress(this);
    }

    @Override
    @Ignore // VectorDataLibrary
    public RList materialize() {
        return containerLibMaterialize(VectorDataLibrary.getFactory().getUncached(data));
    }

    @Override
    protected RList internalCopy() {
        Object[] localData = getReadonlyData();
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
        Object[] localData = getReadonlyData();
        RList listCopy = new RList(Arrays.copyOf(localData, localData.length), getDimensionsInternal(), null, null);
        for (int i = 0; i < listCopy.getLength(); i++) {
            Object el = listCopy.getDataAt(i);
            if (RRuntime.isMaterializedVector(el)) {
                Object elCopy = ((RAbstractVector) el).deepCopy();
                listCopy.updateDataAt(i, elCopy, null);
            }
        }
        return listCopy;
    }

    @ExportMessage(library = AbstractContainerLibrary.class)
    public void materializeData(@CachedLibrary(limit = DATA_LIB_LIMIT) VectorDataLibrary dataLib) {
        setData(dataLib.materialize(data), dataLib.getLength(data));
    }

    @ExportMessage(name = "materialize", library = AbstractContainerLibrary.class)
    RList containerLibMaterialize(@CachedLibrary(limit = DATA_LIB_LIMIT) VectorDataLibrary dataLib) {
        return dataLib.isWriteable(data) ? this : new RList(dataLib.materialize(data), dataLib.getLength(data));
    }

    @ExportMessage(name = "toNative", library = AbstractContainerLibrary.class)
    public void containerLibToNative(
                    @Cached("createBinaryProfile()") ConditionProfile isAllocatedProfile) {
        if (isAllocatedProfile.profile(!NativeDataAccess.isAllocated(this))) {
            // this assumes only two impls. list data: array and native memory
            assert data instanceof Object[];
            Object[] arr = (Object[]) data;
            NativeDataAccess.allocateNativeContents(this, arr, arr.length);
            setData(new RListNativeData(this, arr), arr.length);
            assert NativeDataAccess.isAllocated(this);
        }
        NativeDataAccess.getNativeDataAddress(this);
    }

    @ExportMessage(name = "duplicate", library = AbstractContainerLibrary.class)
    RList containerLibDuplicate(boolean deep, @CachedLibrary(limit = DATA_LIB_LIMIT) VectorDataLibrary dataLib) {
        RList result = new RList(dataLib.copy(data, deep), dataLib.getLength(data));
        setAttributes(result);
        MemoryCopyTracer.reportCopying(this, result);
        return result;
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
            return dataLib.getElementAt(accessIter.getStore(), index);
        }

        @Override
        protected void setListElementImpl(AccessIterator accessIter, int index, Object value) {
            dataLib.setElementAt(accessIter.getStore(), index, value);
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
