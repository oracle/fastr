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

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
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

public final class RStringVector extends RVector<Object[]> implements RAbstractStringVector {

    private static volatile int fence;
    private static final Assumption noWrappedStrings = Truffle.getRuntime().createAssumption();

    private long nativeContentsAddr;
    private Object[] data;

    RStringVector(Object[] data, boolean complete) {
        super(complete);
        assert data instanceof String[] || data instanceof CharSXPWrapper[];
        this.data = data;
        assert RAbstractVector.verify(this);
    }

    RStringVector(Object[] data, boolean complete, int[] dims, RStringVector names, RList dimNames) {
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
    public Object[] getInternalManagedData() {
        return data;
    }

    @Override
    public Object[] getInternalStore() {
        return data;
    }

    @Override
    public void setDataAt(Object store, int index, String value) {
        assert data == store;
        if (noWrappedStrings.isValid() || store instanceof String[]) {
            ((String[]) store)[index] = value;
        } else {
            assert store instanceof CharSXPWrapper[] : store;
            ((CharSXPWrapper[]) store)[index] = CharSXPWrapper.create(value);
        }
    }

    @Override
    public String getDataAt(Object store, int index) {
        assert data == store;
        if (noWrappedStrings.isValid() || store instanceof String[]) {
            return ((String[]) store)[index];
        }
        assert store instanceof CharSXPWrapper[] : store;
        return ((CharSXPWrapper[]) store)[index].getContents();
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

    @Override
    public Object[] getReadonlyData() {
        return data;
    }

    /**
     * Like {@link #getReadonlyData()}, but always retuns Strings. If this vectors holds Strings
     * wrapped in {@link CharSXPWrapper}, then it unwraps them to a newly allocated array.
     */
    public String[] getReadonlyStringData() {
        if (noWrappedStrings.isValid() || data instanceof String[]) {
            return (String[]) data;
        }
        assert data instanceof CharSXPWrapper[] : data;
        return createStringArray((CharSXPWrapper[]) data);
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
        if (noWrappedStrings.isValid() || data instanceof String[]) {
            return ((String[]) data)[i];
        }
        assert data instanceof CharSXPWrapper[] : data;
        return ((CharSXPWrapper[]) data)[i].getContents();
    }

    private RStringVector updateDataAt(int i, String right, NACheck rightNACheck) {
        if (this.isShared()) {
            throw RInternalError.shouldNotReachHere("update shared vector");
        }
        if (noWrappedStrings.isValid() || data instanceof String[]) {
            data[i] = right;
        } else {
            assert data instanceof CharSXPWrapper[] : data;
            ((CharSXPWrapper[]) data)[i] = CharSXPWrapper.create(right);
        }
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

    private Object[] copyResizedData(int size, String fill) {
        Object[] localData = getReadonlyData();
        Object[] newData = Arrays.copyOf(localData, size);
        if (size > localData.length) {
            if (fill != null) {
                for (int i = localData.length; i < size; i++) {
                    newData[i] = fill;
                }
            } else {
                assert localData.length > 0 : "cannot call resize on empty vector if fillNA == false";
                for (int i = localData.length, j = 0; i < size; ++i, j = Utils.incMod(j, localData.length)) {
                    newData[i] = localData[j];
                }
            }
        }
        return newData;
    }

    private Object[] createResizedData(int size, String fill) {
        return copyResizedData(size, fill);
    }

    @Override
    protected RStringVector internalCopyResized(int size, boolean fillNA, int[] dimensions) {
        boolean isComplete = isComplete() && ((data.length >= size) || !fillNA);
        return createStringVector(copyResizedData(size, fillNA ? RRuntime.STRING_NA : null), isComplete, dimensions);
    }

    public RStringVector resizeWithEmpty(int size) {
        return createStringVector(createResizedData(size, RRuntime.NAMES_ATTR_EMPTY_VALUE), isComplete(), null);
    }

    @Override
    public RStringVector createEmptySameType(int newLength, boolean newIsComplete) {
        return RDataFactory.createStringVector(new String[newLength], newIsComplete);
    }

    @Override
    public void transferElementSameType(int toIndex, RAbstractVector fromVector, int fromIndex) {
        RAbstractStringVector other = (RAbstractStringVector) fromVector;
        if (noWrappedStrings.isValid()) {
            data[toIndex] = other.getDataAt(fromIndex);
        } else {
            setDataAt(data, toIndex, other.getDataAt(fromIndex));
        }
    }

    @Override
    public RStringVector copyWithNewDimensions(int[] newDimensions) {
        return createStringVector(data, isComplete(), newDimensions);
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

    /**
     * Allocates a read-only native view on this vector data. The native array items will be
     * NativeMirror IDs pointing to {@link CharSXPWrapper} instances stored in this vector. If the
     * vector contains plain Strings, they will be first wrapped to {@link CharSXPWrapper}s.
     */
    public long allocateNativeContents() {
        if (nativeContentsAddr == 0) {
            wrapStrings();
            nativeContentsAddr = NativeDataAccess.allocateNativeContents(this, (CharSXPWrapper[]) data);
        }
        return nativeContentsAddr;
    }

    /**
     * Converts the data to {@link CharSXPWrapper} instances. Does nothing if the data are already
     * wrapped.
     */
    public void wrapStrings() {
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
        this.data = newData;
        fence = 42;
    }

    public CharSXPWrapper getWrappedDataAt(int index) {
        assert data instanceof CharSXPWrapper[] : "wrap the string vector data with wrapStrings() before using getWrappedDataAt(int)";
        return (CharSXPWrapper) data[index];
    }

    private static RStringVector createStringVector(Object[] data, boolean complete, int[] dims) {
        if (noWrappedStrings.isValid() || data instanceof String[]) {
            return RDataFactory.createStringVector((String[]) data, complete, dims);
        } else {
            return RDataFactory.createStringVector((CharSXPWrapper[]) data, complete, dims);
        }
    }

    private static final class FastPathAccess extends FastPathFromStringAccess {

        private final boolean containsWrappers;

        FastPathAccess(RAbstractContainer value) {
            super(value);
            containsWrappers = !noWrappedStrings.isValid() && value.getInternalStore() instanceof CharSXPWrapper[];
            assert containsWrappers || value.getInternalStore() instanceof String[];
        }

        @Override
        public boolean supports(Object value) {
            return super.supports(value) && ((RStringVector) value).getInternalStore() instanceof CharSXPWrapper[] == containsWrappers;
        }

        @Override
        protected String getString(Object store, int index) {
            assert hasStore;
            if (containsWrappers) {
                return ((CharSXPWrapper[]) store)[index].getContents();
            } else {
                return ((String[]) store)[index];
            }
        }

        @Override
        protected void setString(Object store, int index, String value) {
            assert hasStore;
            if (containsWrappers) {
                ((CharSXPWrapper[]) store)[index] = CharSXPWrapper.create(value);
            } else {
                ((String[]) store)[index] = value;
            }
        }
    }

    @Override
    public VectorAccess access() {
        return new FastPathAccess(this);
    }

    private static final SlowPathFromStringAccess SLOW_PATH_ACCESS = new SlowPathFromStringAccess() {
        @Override
        protected String getString(Object store, int index) {
            return ((RStringVector) store).getDataAt(index);
        }

        @Override
        protected void setString(Object store, int index, String value) {
            ((RStringVector) store).setDataAt(((RStringVector) store).getInternalStore(), index, value);
        }
    };

    @Override
    public VectorAccess slowPathAccess() {
        return SLOW_PATH_ACCESS;
    }
}
