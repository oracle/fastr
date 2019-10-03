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

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.SuppressFBWarnings;
import com.oracle.truffle.r.runtime.data.closures.RClosures;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.data.nodes.FastPathVectorAccess.FastPathFromStringAccess;
import com.oracle.truffle.r.runtime.data.nodes.SlowPathVectorAccess.SlowPathFromStringAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;
import com.oracle.truffle.r.runtime.ops.na.NACheck;
import com.oracle.truffle.r.runtime.data.RSharingAttributeStorage.Shareable;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector.RMaterializedVector;

public final class RStringVector extends RAbstractStringVector implements RMaterializedVector, Shareable {

    static final Assumption noWrappedStrings = Truffle.getRuntime().createAssumption("noWrappedStrings");

    /**
     * After nativized, the data array degenerates to a reference holder.
     */
    private Object[] data;

    private RStringVector() {
        super(false);
    }

    RStringVector(Object[] data, boolean complete) {
        super(complete);
        assert data instanceof String[] || data instanceof CharSXPWrapper[];
        if (noWrappedStrings.isValid() && data instanceof CharSXPWrapper[]) {
            noWrappedStrings.invalidate();
        }
        this.data = data;
        assert RAbstractVector.verifyVector(this);
    }

    RStringVector(Object[] data, boolean complete, int[] dims, RStringVector names, RList dimNames) {
        this(data, complete);
        initDimsNamesDimNames(dims, names, dimNames);
    }

    @Override
    public boolean isMaterialized() {
        return true;
    }

    boolean isNativized() {
        return NativeDataAccess.isAllocated(this);
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
    public Object[] getInternalManagedData() {
        return getInternalStore();
    }

    @Override
    public Object[] getInternalStore() {
        return isNativized() ? null : data;
    }

    @Override
    public void setDataAt(Object store, int index, String value) {
        assert canBeValidStore(store, getInternalStore());
        NativeDataAccess.setData(this, data, index, value);
    }

    @Override
    public String getDataAt(Object store, int index) {
        assert canBeValidStore(store, getInternalStore());
        return NativeDataAccess.getData(this, store, index);
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
                    wrapStrings();
                    Object[] newData = new CharSXPWrapper[l];
                    System.arraycopy(data, 0, newData, 0, l < data.length ? l : data.length);
                    for (int i = data.length; i < l; i++) {
                        newData[i] = CharSXPWrapper.create("");
                    }
                    NativeDataAccess.setDataLength(this, (CharSXPWrapper[]) data, l);
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
    public String[] getDataCopy() {
        if (!isNativized()) {
            Object[] localData = data;
            String[] copy = new String[localData.length];
            if (noWrappedStrings.isValid() || localData instanceof String[]) {
                System.arraycopy(localData, 0, copy, 0, localData.length);
            } else {
                CharSXPWrapper[] wrappers = (CharSXPWrapper[]) localData;
                for (int i = 0; i < localData.length; i++) {
                    copy[i] = wrappers[i].getContents();
                }
            }
            return copy;
        } else {
            return NativeDataAccess.copyStringNativeData(getNativeMirror());
        }
    }

    @Override
    public Object[] getReadonlyData() {
        if (!isNativized()) {
            return data;
        } else {
            return NativeDataAccess.copyStringNativeData(getNativeMirror());
        }
    }

    /**
     * Like {@link #getReadonlyData()}, but always returns Strings. If this vectors holds Strings
     * wrapped in {@link CharSXPWrapper}, then it unwraps them to a newly allocated array.
     */
    public String[] getReadonlyStringData() {
        if (!isNativized()) {
            Object[] localData = data;
            if (noWrappedStrings.isValid() || localData instanceof String[]) {
                return (String[]) localData;
            }
            assert localData instanceof CharSXPWrapper[] : localData;
            return createStringArray((CharSXPWrapper[]) localData);
        } else {
            return NativeDataAccess.copyStringNativeData(getNativeMirror());
        }
    }

    @TruffleBoundary
    private static String[] createStringArray(CharSXPWrapper[] data) {
        String[] result = new String[data.length];
        for (int i = 0; i < data.length; i++) {
            result[i] = data[i].getContents();
        }
        return result;
    }

    @Override
    public String getDataAt(int i) {
        return NativeDataAccess.getData(this, getInternalStore(), i);
    }

    private RStringVector updateDataAt(int i, String right, NACheck rightNACheck) {
        if (this.isShared()) {
            throw RInternalError.shouldNotReachHere("update shared vector");
        }
        NativeDataAccess.setData(this, data, i, right);
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

    @Override
    protected boolean isResizedComplete(int newSize, boolean filledNAs) {
        return !isNativized() && isComplete() && ((data.length >= newSize) || !filledNAs);
    }

    public RStringVector resizeWithEmpty(int size) {
        return createStringVector(copyResizedData(size, RRuntime.NAMES_ATTR_EMPTY_VALUE), isComplete(), null);
    }

    @Override
    public void transferElementSameType(int toIndex, RAbstractVector fromVector, int fromIndex) {
        Object[] localData = getReadonlyData();
        RAbstractStringVector other = (RAbstractStringVector) fromVector;
        if (noWrappedStrings.isValid()) {
            localData[toIndex] = other.getDataAt(fromIndex);
        } else {
            setDataAt(localData, toIndex, other.getDataAt(fromIndex));
        }
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
        assert value instanceof CharSXPWrapper;
        wrapStrings();
        NativeDataAccess.setData(this, (CharSXPWrapper[]) data, i, (CharSXPWrapper) value);
    }

    /**
     * Allocates a read-only native view on this vector data. The native array items will be
     * NativeMirror IDs pointing to {@link CharSXPWrapper} instances stored in this vector. If the
     * vector contains plain Strings, they will be first wrapped to {@link CharSXPWrapper}s.
     */
    public long allocateNativeContents() {
        try {
            wrapStrings();
            return NativeDataAccess.allocateNativeContents(this, (CharSXPWrapper[]) getInternalStore(), getLength());
        } finally {
            assert NativeDataAccess.isAllocated(this);
            complete = false;
        }
    }

    static RStringVector fromNative(long address, int length) {
        RStringVector result = new RStringVector();
        NativeDataAccess.toNative(result);
        NativeDataAccess.setNativeContents(result, address, length);
        return result;
    }

    public void wrapStrings() {
        wrapStrings(ConditionProfile.getUncached(), ConditionProfile.getUncached());
    }

    /**
     * Converts the data to {@link CharSXPWrapper} instances. Does nothing if the data are already
     * wrapped.
     */
    @SuppressFBWarnings(value = "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD", justification = "intentional")
    public void wrapStrings(ConditionProfile isNativized, ConditionProfile needsWrapping) {
        if (isNativized.profile(!isNativized())) {
            Object[] oldData = data;
            if (needsWrapping.profile(oldData instanceof CharSXPWrapper[])) {
                return;
            }
            noWrappedStrings.invalidate();
            String[] oldStrings = (String[]) oldData;
            CharSXPWrapper[] newData = new CharSXPWrapper[oldStrings.length];
            for (int i = 0; i < oldData.length; i++) {
                newData[i] = CharSXPWrapper.create(oldStrings[i]);
            }
            fence = 42; // make sure the array is really initialized before we set it to this.data
            this.data = newData;
        }
    }

    @SuppressFBWarnings(value = "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD", justification = "intentional")
    public void wrapStrings(ConditionProfile profile) {
        if (profile.profile(!isNativized())) {
            Object[] oldData = data;
            if (oldData instanceof CharSXPWrapper[]) {
                return;
            }
            noWrappedStrings.invalidate();
            String[] oldStrings = (String[]) oldData;
            CharSXPWrapper[] newData = new CharSXPWrapper[oldStrings.length];
            for (int i = 0; i < oldData.length; i++) {
                newData[i] = CharSXPWrapper.create(oldStrings[i]);
            }
            fence = 42; // make sure the array is really initialized before we set it to this.data
            this.data = newData;
        }
    }

    public CharSXPWrapper getWrappedDataAt(int index) {
        if (!isNativized()) {
            assert data instanceof CharSXPWrapper[] : "wrap the string vector data with wrapStrings() before using getWrappedDataAt(int)";
            return (CharSXPWrapper) data[index];
        } else {
            return NativeDataAccess.getStringNativeMirrorData(getNativeMirror(), index);
        }
    }

    public void setWrappedDataAt(int index, CharSXPWrapper elem) {
        if (!isNativized()) {
            wrapStrings();
            assert data instanceof CharSXPWrapper[] : "wrap the string vector data with wrapStrings() before using getWrappedDataAt(int)";
            data[index] = elem;
        } else {
            data[index] = elem;
            NativeDataAccess.setNativeMirrorStringData(getNativeMirror(), index, elem);
        }
    }

    @Override
    protected RStringVector createStringVector(Object[] vecData, boolean isComplete, int[] dims) {
        if (noWrappedStrings.isValid() || vecData instanceof String[]) {
            return RDataFactory.createStringVector((String[]) vecData, isComplete, dims);
        } else {
            return RDataFactory.createStringVector((CharSXPWrapper[]) vecData, isComplete, dims);
        }
    }

    private static final class FastPathAccess extends FastPathFromStringAccess {

        private final boolean containsWrappers;

        FastPathAccess(RAbstractContainer value) {
            super(value);
            containsWrappers = !noWrappedStrings.isValid() && value.getInternalStore() instanceof CharSXPWrapper[];
            assert !hasStore || containsWrappers || value.getInternalStore() instanceof String[];
        }

        @Override
        public boolean supports(Object value) {
            return super.supports(value) && (!hasStore || ((RStringVector) value).getInternalStore() instanceof CharSXPWrapper[] == containsWrappers);
        }

        @Override
        protected String getStringImpl(AccessIterator accessIter, int index) {
            if (hasStore) {
                if (containsWrappers) {
                    return ((CharSXPWrapper[]) accessIter.getStore())[index].getContents();
                } else {
                    return ((String[]) accessIter.getStore())[index];
                }
            } else {
                return NativeDataAccess.getStringNativeMirrorData(accessIter.getStore(), index).getContents();
            }
        }

        @Override
        protected void setStringImpl(AccessIterator accessIter, int index, String value) {
            if (hasStore) {
                if (containsWrappers) {
                    ((CharSXPWrapper[]) accessIter.getStore())[index] = CharSXPWrapper.create(value);
                } else {
                    ((String[]) accessIter.getStore())[index] = value;
                }
            } else {
                NativeDataAccess.setNativeMirrorStringData(accessIter.getStore(), index, CharSXPWrapper.create(value));
            }
        }
    }

    @Override
    public VectorAccess access() {
        return new FastPathAccess(this);
    }

    private static final SlowPathFromStringAccess SLOW_PATH_ACCESS = new SlowPathFromStringAccess() {
        @Override
        protected String getStringImpl(AccessIterator accessIter, int index) {
            return ((RStringVector) accessIter.getStore()).getDataAt(index);
        }

        @Override
        protected void setStringImpl(AccessIterator accessIter, int index, String value) {
            Object store = accessIter.getStore();
            ((RStringVector) store).setDataAt(((RStringVector) store).getInternalStore(), index, value);
        }
    };

    @Override
    public VectorAccess slowPathAccess() {
        return SLOW_PATH_ACCESS;
    }
}
