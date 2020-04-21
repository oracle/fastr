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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.library.ExportMessage.Ignore;

import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.data.closures.RClosures;
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.data.nodes.FastPathVectorAccess.FastPathFromComplexAccess;
import com.oracle.truffle.r.runtime.data.nodes.SlowPathVectorAccess.SlowPathFromComplexAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;
import com.oracle.truffle.r.runtime.ops.na.NACheck;
import com.oracle.truffle.r.runtime.data.RSharingAttributeStorage.Shareable;
import com.oracle.truffle.r.runtime.data.closures.RClosure;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector.RMaterializedVector;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

@ExportLibrary(InteropLibrary.class)
public final class RComplexVector extends RAbstractComplexVector implements RMaterializedVector, Shareable {

    public static final String MEMBER_RE = "re";
    public static final String MEMBER_IM = "im";

    private int length;

    RComplexVector(double[] data, boolean complete) {
        super(complete);
        assert data.length % 2 == 0;
        setData(new RComplexArrayVectorData(data, complete), data.length >> 1);
        assert RAbstractVector.verifyVector(this);
    }

    RComplexVector(double[] data, boolean complete, int[] dims, RStringVector names, RList dimNames) {
        this(data, complete);
        initDimsNamesDimNames(dims, names, dimNames);
    }

    RComplexVector(Object data, int newLen) {
        super(false);
        setData(data, newLen);
        assert RAbstractVector.verifyVector(this);
    }

    private RComplexVector() {
        super(false);
    }

    private void setData(Object data, int newLen) {
        this.data = data;
        if (data instanceof VectorDataWithOwner) {
            ((VectorDataWithOwner) data).setOwner(this);
        }
        // Temporary solution to keep getLength(), isComplete(), and isShareable be fast-path
        // operations (they only read a field, no polymorphism).
        // The assumption is that length of vectors can only change in infrequently used setLength
        // operation where we update the field accordingly
        length = newLen;
        shareable = data instanceof RComplexArrayVectorData || data instanceof RComplexNativeVectorData;
        // Only array storage strategy is handling the complete flag dynamically,
        // for other strategies, the complete flag is determined solely by the type of the strategy
        if (!(data instanceof RComplexArrayVectorData)) {
            // only sequences are always complete, everything else is always incomplete
            setComplete(false);
        }
    }

    public static RComplexVector createClosure(RAbstractVector delegate, boolean keepAttrs) {
        RComplexVector result = new RComplexVector(VectorDataClosure.fromVector(delegate, RType.Complex), delegate.getLength());
        if (keepAttrs) {
            result.initAttributes(delegate.getAttributes());
        } else {
            RClosures.initRegAttributes(result, delegate);
        }
        return result;
    }

    static RComplexVector fromNative(long address, int length) {
        RComplexVector result = new RComplexVector();
        NativeDataAccess.toNative(result);
        NativeDataAccess.setNativeContents(result, address, length);
        return result;
    }

    @ExportMessage
    public boolean isNull(
                    @CachedLibrary(limit = DATA_LIB_LIMIT) VectorDataLibrary dataLib,
                    @Cached.Exclusive @Cached("createBinaryProfile()") ConditionProfile isScalar) {
        if (!isScalar.profile(isScalar(dataLib))) {
            return false;
        }
        return RRuntime.isNA(dataLib.getComplexAt(getData(), 0));
    }

    @ExportMessage
    boolean hasMembers(@CachedLibrary(limit = DATA_LIB_LIMIT) VectorDataLibrary dataLib) {
        return isScalar(dataLib) && !RRuntime.isNA(dataLib.getComplexAt(getData(), 0));
    }

    @ExportMessage
    Object getMembers(@SuppressWarnings("unused") boolean includeInternal,
                    @CachedLibrary(limit = DATA_LIB_LIMIT) VectorDataLibrary dataLib,
                    @Cached.Shared("noMembers") @Cached("createBinaryProfile()") ConditionProfile noMembers) throws UnsupportedMessageException {
        if (noMembers.profile(!hasMembers(dataLib))) {
            throw UnsupportedMessageException.create();
        }
        return RDataFactory.createStringVector(new String[]{MEMBER_RE, MEMBER_IM}, RDataFactory.COMPLETE_VECTOR);
    }

    @ExportMessage
    boolean isMemberReadable(String member,
                    @CachedLibrary(limit = DATA_LIB_LIMIT) VectorDataLibrary dataLib,
                    @Cached.Shared("noMembers") @Cached("createBinaryProfile()") ConditionProfile noMembers) {
        if (noMembers.profile(!hasMembers(dataLib))) {
            return false;
        }
        return MEMBER_RE.equals(member) || MEMBER_IM.equals(member);
    }

    @ExportMessage
    Object readMember(String member,
                    @CachedLibrary(limit = DATA_LIB_LIMIT) VectorDataLibrary dataLib,
                    @Cached.Shared("noMembers") @Cached("createBinaryProfile()") ConditionProfile noMembers,
                    @Cached.Exclusive @Cached("createBinaryProfile()") ConditionProfile unknownIdentifier) throws UnknownIdentifierException, UnsupportedMessageException {
        if (noMembers.profile(!hasMembers(dataLib))) {
            throw UnsupportedMessageException.create();
        }
        if (unknownIdentifier.profile(!isMemberReadable(member, dataLib, noMembers))) {
            throw UnknownIdentifierException.create(member);
        }
        RComplex singleValue = dataLib.getComplexAt(getData(), 0);
        if (MEMBER_RE.equals(member)) {
            return singleValue.getRealPart();
        } else {
            return singleValue.getImaginaryPart();
        }
    }

    @Override
    @ExportMessage.Ignore // AbstractContainerLibrary
    public boolean isMaterialized() {
        return VectorDataLibrary.getFactory().getUncached().isWriteable(this.data);
    }

    @Override
    public RType getRType() {
        return RType.Complex;
    }

    @Override
    public boolean isForeignWrapper() {
        return false;
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
    public double[] getInternalManagedData() {
        if (data instanceof RComplexNativeVectorData) {
            return null;
        }
        // TODO: get rid of this method
        assert data instanceof RComplexArrayVectorData : data.getClass().getName();
        return ((RComplexArrayVectorData) data).getReadonlyComplexData();
    }

    @Override
    public Object getInternalStore() {
        return data;
    }

    @Override
    @ExportMessage.Ignore
    public int getLength() {
        return length;
    }

    @Override
    public void setLength(int l) {
        try {
            NativeDataAccess.setDataLength(this, getArrayForNativeDataAccess(), l);
        } finally {
            RComplexNativeVectorData newData = new RComplexNativeVectorData(this);
            setData(newData, l);
            setComplete(false);
        }
    }

    @Override
    public int getTrueLength() {
        return NativeDataAccess.getTrueDataLength(this);
    }

    @Override
    public void setTrueLength(int l) {
        try {
            NativeDataAccess.setTrueDataLength(this, l);
        } finally {
            RComplexNativeVectorData newData = new RComplexNativeVectorData(this);
            setData(newData, VectorDataLibrary.getFactory().getUncached().getLength(newData));
            setComplete(false);
        }
    }

    @Override
    public RAbstractVector castSafe(RType type, ConditionProfile isNAProfile, boolean keepAttributes) {
        switch (type) {
            case Complex:
                return this;
            case Character:
                return RClosures.createToStringVector(this, keepAttributes);
            case List:
                return RClosures.createToListVector(this, keepAttributes);
            default:
                return null;
        }
    }

    @Override
    public void setDataAt(Object store, int index, RComplex value) {
        assert data == store;
        VectorDataLibrary.getFactory().getUncached().setComplexAt(store, index, value);
    }

    @Override
    public void setDataAt(Object store, int index, double value) {
        assert data == store;
        NativeDataAccess.setData(this, null, index * 2, value);
    }

    @Override
    public RComplex getDataAt(int index) {
        return getDataAt(getData(), index);
    }

    @Override
    public RComplex getDataAt(Object store, int index) {
        assert data == store;
        return VectorDataLibrary.getFactory().getUncached().getComplexAt(store, index);
    }

    /**
     * Returns the double value under specified index assuming a representation where the complex
     * numbers are flattened to an array: real part followed by the imaginary part.
     */
    public double getRawDataAt(int index) {
        return NativeDataAccess.getRawComplexData(this, null, index);
    }

    @Override
    public double[] getDataCopy() {
        return VectorDataLibrary.getFactory().getUncached().getComplexDataCopy(data);
    }

    @Override
    public double[] getReadonlyData() {
        return VectorDataLibrary.getFactory().getUncached().getReadonlyComplexData(data);
    }

    private RComplexVector updateDataAt(int index, RComplex value, NACheck naCheck) {
        assert !this.isShared();
        assert !RRuntime.isNA(value) || naCheck.isEnabled();
        VectorDataLibrary.getFactory().getUncached().setComplexAt(data, index, value);
        assert !isComplete() || !RRuntime.isNA(value);
        return this;
    }

    @Override
    public RComplexVector updateDataAtAsObject(int i, Object o, NACheck naCheck) {
        return updateDataAt(i, (RComplex) o, naCheck);
    }

    @Override
    @Ignore // AbstractContainerLibrary
    public RComplexVector materialize() {
        return containerLibMaterialize(VectorDataLibrary.getFactory().getUncached(data));
    }

    @ExportMessage(library = AbstractContainerLibrary.class)
    public void materializeData(@CachedLibrary(limit = DATA_LIB_LIMIT) VectorDataLibrary dataLib) {
        setData(dataLib.materialize(data), getLength());
    }

    @ExportMessage(name = "materialize", library = AbstractContainerLibrary.class)
    RComplexVector containerLibMaterialize(@CachedLibrary(limit = DATA_LIB_LIMIT) VectorDataLibrary dataLib) {
        if (dataLib.isWriteable(data)) {
            return this;
        }
        // To retain the semantics of the original materialize, for sequences and such we return new
        // vector
        return new RComplexVector(dataLib.getComplexDataCopy(data), isComplete());
    }

    @Override
    @Ignore // AbstractContainerLibrary
    public RComplexVector createEmptySameType(int newLength, boolean newIsComplete) {
        return new RComplexVector(new double[newLength << 1], newIsComplete);
    }

    @Override
    public void transferElementSameType(int toIndex, RAbstractVector fromVector, int fromIndex) {
        VectorDataLibrary lib = VectorDataLibrary.getFactory().getUncached();
        RComplex value = lib.getComplexAt(((RComplexVector) fromVector).data, fromIndex);
        lib.setComplexAt(data, toIndex, value);
    }

    @TruffleBoundary
    protected void copyAttributes(RIntVector materializedVec) {
        materializedVec.copyAttributesFrom(this);
    }

    @Override
    protected RComplexVector internalCopyResized(int size, boolean fillNA, int[] dimensions) {
        boolean isComplete = isResizedComplete(size, fillNA);
        return RDataFactory.createComplexVector(copyResizedData(size, fillNA), isComplete, dimensions);
    }

    private double[] copyResizedData(int size, boolean fillNA) {
        int csize = size << 1;
        double[] localData = getReadonlyData();
        double[] newData = Arrays.copyOf(localData, csize);
        if (csize > localData.length) {
            if (fillNA) {
                for (int i = localData.length; i < csize; i++) {
                    newData[i] = RRuntime.DOUBLE_NA;
                }
            } else {
                assert localData.length > 0 : "cannot call resize on empty vector if fillNA == false";
                for (int i = localData.length, j = 0; i <= csize - 2; i += 2, j = Utils.incMod(j + 1, localData.length)) {
                    newData[i] = localData[j];
                    newData[i + 1] = localData[j + 1];
                }
            }
        }
        return newData;
    }

    @Override
    protected RComplexVector internalCopy() {
        return RDataFactory.createComplexVector(getDataCopy(), isComplete());
    }

    @Override
    public double[] getDataTemp() {
        return (double[]) super.getDataTemp();
    }

    @Override
    public Object getDataAtAsObject(int index) {
        return getDataAt(index);
    }

    @Override
    public void setElement(int index, Object value) {
        setDataAt(getData(), index, (Double) value);
    }

    public long allocateNativeContents() {
        try {
            data = VectorDataLibrary.getFactory().getUncached().materialize(data);
            long result = NativeDataAccess.allocateNativeContents(this, getArrayForNativeDataAccess(), getLength());
            setData(new RComplexNativeVectorData(this), getLength());
            return result;
        } finally {
            setComplete(false);
        }
    }

    private AtomicReference<RComplexVector> materialized = new AtomicReference<>();

    public Object cachedMaterialize() {
        if (materialized.get() == null) {
            materialized.compareAndSet(null, materialize());
        }
        return materialized.get();
    }

    private static final class FastPathAccess extends FastPathFromComplexAccess {

        FastPathAccess(RAbstractContainer value) {
            super(value);
        }

        @Override
        protected RComplex getComplexImpl(AccessIterator accessIter, int index) {
            return dataLib.getComplexAt(accessIter.getStore(), index);
        }

        @Override
        protected double getComplexRImpl(AccessIterator accessIter, int index) {
            return dataLib.getComplexAt(accessIter.getStore(), index).getRealPart();
        }

        @Override
        protected double getComplexIImpl(AccessIterator accessIter, int index) {
            return dataLib.getComplexAt(accessIter.getStore(), index).getImaginaryPart();
        }

        @Override
        protected void setComplexImpl(AccessIterator accessIter, int index, double real, double imaginary) {
            dataLib.setComplexAt(accessIter.getStore(), index, RComplex.valueOf(real, imaginary));
        }
    }

    @Override
    public VectorAccess access() {
        return new FastPathAccess(this);
    }

    private static final SlowPathFromComplexAccess SLOW_PATH_ACCESS = new SlowPathFromComplexAccess() {
        @Override
        protected RComplex getComplexImpl(AccessIterator accessIter, int index) {
            RComplexVector vector = (RComplexVector) accessIter.getStore();
            return vector.getDataAt(index);
        }

        @Override
        protected double getComplexRImpl(AccessIterator accessIter, int index) {
            RComplexVector vector = (RComplexVector) accessIter.getStore();
            return vector.getDataAt(index).getRealPart();
        }

        @Override
        protected double getComplexIImpl(AccessIterator accessIter, int index) {
            RComplexVector vector = (RComplexVector) accessIter.getStore();
            return vector.getDataAt(index).getImaginaryPart();
        }

        @Override
        protected void setComplexImpl(AccessIterator accessIter, int index, double real, double imaginary) {
            RComplexVector vector = (RComplexVector) accessIter.getStore();
            vector.setDataAt(vector.data, index, RComplex.valueOf(real, imaginary));
        }
    };

    @Override
    public VectorAccess slowPathAccess() {
        return SLOW_PATH_ACCESS;
    }

    // TODO: Hack: we make sure the vector is either array or native, so that we can call
    // NativeDataAccess methods
    private double[] getArrayForNativeDataAccess() {
        materializeData(VectorDataLibrary.getFactory().getUncached());
        return data instanceof RComplexArrayVectorData ? ((RComplexArrayVectorData) data).getReadonlyComplexData() : null;
    }
}
