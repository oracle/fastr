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
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.profiles.ConditionProfile;
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
import com.oracle.truffle.r.runtime.interop.R2Foreign;
import com.oracle.truffle.r.runtime.ops.na.NACheck;
import com.oracle.truffle.r.runtime.data.RSharingAttributeStorage.Shareable;

@ExportLibrary(InteropLibrary.class)
public final class RExpression extends RAbstractListBaseVector implements RMaterializedVector, Shareable {

    private Object[] data;

    RExpression(Object[] data, int[] dims, RStringVector names, RList dimNames) {
        super(false);
        this.data = data;
        initDimsNamesDimNames(dims, names, dimNames);
        assert RAbstractVector.verifyVector(this);
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    boolean hasArrayElements() {
        return true;
    }

    @ExportMessage
    long getArraySize() {
        return getLength();
    }

    @ExportMessage
    boolean isArrayElementReadable(long index) {
        return index >= 0 && index < getLength();
    }

    @ExportMessage
    Object readArrayElement(long index,
                    @Cached.Shared("r2Foreign") @Cached() R2Foreign r2Foreign,
                    @Cached.Shared("unknownIdentifier") @Cached("createBinaryProfile()") ConditionProfile invalidIndex) throws InvalidArrayIndexException {
        if (!invalidIndex.profile(isArrayElementReadable(index))) {
            throw InvalidArrayIndexException.create(index);
        }
        return r2Foreign.convert(getDataAtAsObject((int) index));
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    boolean hasMembers() {
        return true;
    }

    @ExportMessage
    Object getMembers(@SuppressWarnings("unused") boolean includeInternal) {
        RStringVector names = getNames();
        return names != null ? names : RDataFactory.createEmptyStringVector();
    }

    @ExportMessage
    boolean isMemberReadable(String member) {
        int idx = getElementIndexByName(member);
        return isArrayElementReadable(idx);
    }

    @ExportMessage
    boolean isMemberInvocable(String member) {
        int idx = getElementIndexByName(member);
        return isArrayElementReadable(idx) && getDataAtAsObject(idx) instanceof RFunction;
    }

    @ExportMessage
    Object readMember(String member,
                    @Cached.Shared("r2Foreign") @Cached() R2Foreign r2Foreign,
                    @Cached.Shared("unknownIdentifier") @Cached("createBinaryProfile()") ConditionProfile unknownIdentifier) throws UnknownIdentifierException {
        int idx = getElementIndexByName(member);
        if (unknownIdentifier.profile(!isArrayElementReadable(idx))) {
            throw UnknownIdentifierException.create(member);
        }
        return r2Foreign.convert(getDataAtAsObject(idx));
    }

    @ExportMessage
    Object invokeMember(String member, Object[] arguments,
                    @Cached.Shared("unknownIdentifier") @Cached("createBinaryProfile()") ConditionProfile unknownIdentifier,
                    @Cached() RFunction.ExplicitCall c) throws UnknownIdentifierException, UnsupportedMessageException {
        int idx = getElementIndexByName(member);
        if (unknownIdentifier.profile(!isArrayElementReadable(idx))) {
            throw UnknownIdentifierException.create(member);
        }
        Object f = getDataAtAsObject(idx);
        if (f instanceof RFunction) {
            return c.execute((RFunction) f, arguments);
        }
        throw UnsupportedMessageException.create();
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    boolean isPointer() {
        return true;
    }

    @ExportMessage
    long asPointer() {
        return NativeDataAccess.asPointer(this);
    }

    @ExportMessage
    void toNative() {
        NativeDataAccess.asPointer(this);
    }

    @Override
    public boolean isMaterialized() {
        return true;
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
            if (el instanceof RMaterializedVector) {
                Object elCopy = ((RAbstractVector) el).deepCopy();
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
