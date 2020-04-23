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

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.library.ExportMessage.Ignore;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.SuppressFBWarnings;
import com.oracle.truffle.r.runtime.data.RSharingAttributeStorage.Shareable;
import com.oracle.truffle.r.runtime.data.closures.RClosure;
import com.oracle.truffle.r.runtime.data.closures.RClosures;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector.RMaterializedVector;
import com.oracle.truffle.r.runtime.data.nodes.FastPathVectorAccess.FastPathFromStringAccess;
import com.oracle.truffle.r.runtime.data.nodes.SlowPathVectorAccess.SlowPathFromStringAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

import java.util.concurrent.atomic.AtomicReference;

public final class RStringVector extends RAbstractStringVector implements RMaterializedVector, Shareable {

    private int length;

    RStringVector(Object[] data, boolean complete) {
        super(complete);
        if (data instanceof String[]) {
            setData(new RStringArrayVectorData((String[]) data, complete), data.length);
        } else if (data instanceof CharSXPWrapper[]) {
            setData(new RStringCharSXPData((CharSXPWrapper[]) data), data.length);
        } else {
            assert false : data;
        }
        assert RAbstractVector.verifyVector(this);
    }

    RStringVector(Object[] data, boolean complete, int[] dims, RStringVector names, RList dimNames) {
        this(data, complete);
        initDimsNamesDimNames(dims, names, dimNames);
    }

    public RStringVector(Object data, int length) {
        super(false);
        setData(data, length);
        assert RAbstractVector.verifyVector(this);
    }

    public static RStringVector createForeignWrapper(TruffleObject obj, int size) {
        return new RStringVector(new RStringForeignObjData(obj), size);
    }

    public static RStringVector createForeignWrapper(TruffleObject obj) {
        try {
            return new RStringVector(new RStringForeignObjData(obj), (int) InteropLibrary.getFactory().getUncached().getArraySize(obj));
        } catch (UnsupportedMessageException e) {
            throw RInternalError.shouldNotReachHere();
        }
    }

    public static RStringVector createClosure(RAbstractVector delegate, boolean keepAttrs) {
        RStringVector result = new RStringVector(VectorDataClosure.fromVector(delegate, RType.Character), delegate.getLength());
        if (keepAttrs) {
            result.initAttributes(delegate.getAttributes());
        } else {
            RClosures.initRegAttributes(result, delegate);
        }
        return result;
    }

    public static RStringVector createFactorClosure(RIntVector factor, RAbstractStringVector levels, boolean keepAttrs) {
        RStringVector result = new RStringVector(new RStringFactorClosure(factor, levels), factor.getLength());
        if (keepAttrs) {
            result.initAttributes(factor.getAttributes());
        } else {
            RClosures.initRegAttributes(result, factor);
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

    @Override
    @Ignore // AbstractContainerLibrary
    public boolean isMaterialized() {
        return getUncachedDataLib().isWriteable(data);
    }

    boolean isNativized() {
        return NativeDataAccess.isAllocated(this);
    }

    @Override
    public boolean isForeignWrapper() {
        return data instanceof RStringForeignObjData;
    }

    @Override
    public boolean isSequence() {
        return data instanceof RStringSeqVectorData;
    }

    @Override
    public RStringSeqVectorData getSequence() {
        return (RStringSeqVectorData) data;
    }

    @Override
    public boolean isClosure() {
        return data instanceof RClosure;
    }

    @Override
    public RClosure getClosure() {
        return (RClosure) data;
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
        if (data instanceof RStringVecNativeData) {
            return null;
        }
        // TODO: get rid of this method
        return getUncachedDataLib().getReadonlyStringData(data);
    }

    @Override
    public Object getInternalStore() {
        return data;
    }

    @Override
    public void setDataAt(Object store, int index, String value) {
        getUncachedDataLib().setStringAt(data, index, value);
    }

    @Override
    public String getDataAt(Object store, int index) {
        return getUncachedDataLib().getStringAt(data, index);
    }

    @Override
    @Ignore
    public int getLength() {
        return length;
    }

    @Override
    public void setLength(int l) {
        wrapStrings();
        assert data instanceof RStringCharSXPData || data instanceof RStringVecNativeData;
        CharSXPWrapper[] dataForNativeAccess = data instanceof RStringCharSXPData ? ((RStringCharSXPData) data).getData() : null;
        try {
            NativeDataAccess.setDataLength(this, dataForNativeAccess, l);
        } finally {
            if (!(data instanceof RStringVecNativeData)) {
                setData(new RStringVecNativeData(this, dataForNativeAccess), l);
            } else {
                length = l;
            }
        }
        verifyData();
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
        return getUncachedDataLib().getStringDataCopy(data);
    }

    @Override
    public Object[] getReadonlyData() {
        return getUncachedDataLib().getReadonlyStringData(data);
    }

    /**
     * Like {@link #getReadonlyData()}, but always returns Strings. If this vectors holds Strings
     * wrapped in {@link CharSXPWrapper}, then it unwraps them to a newly allocated array.
     */
    @Ignore // VectorDataLibrary
    public String[] getReadonlyStringData() {
        return getUncachedDataLib().getReadonlyStringData(data);
    }

    @Override
    public String getDataAt(int i) {
        return getUncachedDataLib().getStringAt(data, i);
    }

    private RStringVector updateDataAt(int i, String right, NACheck rightNACheck) {
        if (this.isShared()) {
            throw RInternalError.shouldNotReachHere("update shared vector");
        }
        getUncachedDataLib().setStringAt(data, i, right);
        rightNACheck.check(right);
        assert !isComplete() || !RRuntime.isNA(right);
        return this;
    }

    @Override
    public RStringVector updateDataAtAsObject(int i, Object o, NACheck naCheck) {
        return updateDataAt(i, (String) o, naCheck);
    }

    @Override
    protected boolean isResizedComplete(int newSize, boolean filledNAs) {
        return !isNativized() && isComplete() && ((getLength() >= newSize) || !filledNAs);
    }

    @Override
    public void transferElementSameType(int toIndex, RAbstractVector fromVector, int fromIndex) {
        Object[] localData = getReadonlyData();
        RAbstractStringVector other = (RAbstractStringVector) fromVector;
        setDataAt(localData, toIndex, other.getDataAt(fromIndex));
    }

    @Override
    @Ignore
    public RStringVector materialize() {
        return containerLibMaterialize(VectorDataLibrary.getFactory().getUncached(data));
    }

    @ExportMessage(library = AbstractContainerLibrary.class)
    public void materializeData(@CachedLibrary(limit = DATA_LIB_LIMIT) VectorDataLibrary dataLib) {
        setData(dataLib.materialize(data), getLength());
    }

    @ExportMessage(name = "materialize", library = AbstractContainerLibrary.class)
    RStringVector containerLibMaterialize(@CachedLibrary(limit = DATA_LIB_LIMIT) VectorDataLibrary dataLib) {
        if (dataLib.isWriteable(data)) {
            return this;
        }
        // To retain the semantics of the original materialize, for sequences and such we return new
        // vector
        return new RStringVector(dataLib.getStringDataCopy(data), isComplete());
    }

    @ExportMessage(name = "copy", library = AbstractContainerLibrary.class)
    RStringVector containerLibCopy(@CachedLibrary(limit = DATA_LIB_LIMIT) VectorDataLibrary dataLib) {
        RStringVector result = new RStringVector(dataLib.copy(data, false), dataLib.getLength(data));
        MemoryCopyTracer.reportCopying(this, result);
        return result;
    }

    private AtomicReference<RStringVector> materialized = new AtomicReference<>();

    public Object cachedMaterialize() {
        if (materialized.get() == null) {
            materialized.compareAndSet(null, materialize());
        }
        return materialized.get();
    }

    @Override
    @Ignore // VectorDataLibrary
    public Object getDataAtAsObject(int index) {
        return getDataAt(index);
    }

    @Override
    @Ignore // VectorDataLibrary
    public void setElement(int i, Object value) {
        assert value instanceof CharSXPWrapper;
        wrapStrings();
        setWrappedDataAt(i, (CharSXPWrapper) value);
    }

    /**
     * Allocates a read-only native view on this vector data. The native array items will be
     * NativeMirror IDs pointing to {@link CharSXPWrapper} instances stored in this vector. If the
     * vector contains plain Strings, they will be first wrapped to {@link CharSXPWrapper}s.
     */
    public long allocateNativeContents() {
        wrapStrings();
        assert data instanceof RStringCharSXPData || data instanceof RStringVecNativeData;
        CharSXPWrapper[] dataForNativeAccess = data instanceof RStringCharSXPData ? ((RStringCharSXPData) data).getData() : null;
        int len = getLength();
        try {
            NativeDataAccess.allocateNativeContents(this, dataForNativeAccess, len);
        } finally {
            if (!(data instanceof RStringVecNativeData)) {
                setData(new RStringVecNativeData(this, dataForNativeAccess), len);
            }
            assert NativeDataAccess.isAllocated(this);
        }
        verifyData();
        return NativeDataAccess.getNativeDataAddress(this);
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
            Object oldData = data;
            if (needsWrapping.profile(oldData instanceof RStringCharSXPData)) {
                return;
            }
            oldData = getUncachedDataLib().materialize(oldData);
            Object newData = ((RStringArrayVectorData) oldData).wrapStrings();
            fence = 42; // make sure the array is really initialized before we set it to this.data
            setData(newData, getUncachedDataLib().getLength(newData));
        }
        verifyData();
    }

    public CharSXPWrapper getWrappedDataAt(int index) {
        if (!isNativized()) {
            assert data instanceof RStringCharSXPData : "wrap the string vector data with wrapStrings() before using getWrappedDataAt(int)";
            return ((RStringCharSXPData) data).getWrappedAt(index);
        } else {
            return NativeDataAccess.getStringNativeMirrorData(getNativeMirror(), index);
        }
    }

    public void setWrappedDataAt(int index, CharSXPWrapper elem) {
        if (!isNativized()) {
            wrapStrings();
            assert data instanceof RStringCharSXPData : "wrap the string vector data with wrapStrings() before using getWrappedDataAt(int)";
            ((RStringCharSXPData) data).setWrappedAt(index, elem);
        } else {
            ((RStringVecNativeData) data).setWrappedStringAt(index, elem);
        }
    }

    @Override
    protected RStringVector createStringVector(Object[] vecData, boolean isComplete, int[] dims) {
        if (vecData instanceof String[]) {
            return RDataFactory.createStringVector((String[]) vecData, isComplete, dims);
        } else {
            return RDataFactory.createStringVector((CharSXPWrapper[]) vecData, isComplete, dims);
        }
    }

    public boolean isWrapped() {
        return data instanceof RStringCharSXPData;
    }

    private static final class FastPathAccess extends FastPathFromStringAccess {
        FastPathAccess(RAbstractContainer value) {
            super(value);
        }

        @Override
        public boolean supports(Object value) {
            return super.supports(value) && dataLib.accepts(((RStringVector) value).getData());
        }

        @Override
        public String getStringImpl(AccessIterator accessIter, int index) {
            String value = dataLib.getStringAt(accessIter.getStore(), index);
            na.check(value);
            return value;
        }

        @Override
        protected void setStringImpl(AccessIterator accessIter, int index, String value) {
            dataLib.setStringAt(accessIter.getStore(), index, value);
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

    @Override
    protected void verifyData() {
        super.verifyData();
        assert getUncachedDataLib().getLength(data) == length;
    }
}
