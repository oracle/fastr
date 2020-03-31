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
package com.oracle.truffle.r.runtime.data.model;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.library.ExportMessage.Ignore;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.LoopConditionProfile;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.data.MemoryCopyTracer;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RComplexVector;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RInteropNA.RInteropComplexNA;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.RandomAccessIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.RandomAccessWriteIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.SeqIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.SeqWriteIterator;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

import java.util.Arrays;

@ExportLibrary(InteropLibrary.class)
@ExportLibrary(VectorDataLibrary.class)
public abstract class RAbstractComplexVector extends RAbstractAtomicVector {

    public static final String MEMBER_RE = "re";
    public static final String MEMBER_IM = "im";

    public RAbstractComplexVector(boolean complete) {
        super(complete);
    }

    @ExportMessage
    public final boolean isNull(@Cached.Exclusive @Cached("createBinaryProfile()") ConditionProfile isScalar) {
        if (!isScalar.profile(isScalar())) {
            return false;
        }
        return RRuntime.isNA(getDataAt(0));
    }

    @ExportMessage
    boolean hasMembers() {
        return isScalar() && !RRuntime.isNA(getDataAt(0));
    }

    @ExportMessage
    Object getMembers(@SuppressWarnings("unused") boolean includeInternal,
                    @Cached.Shared("noMembers") @Cached("createBinaryProfile()") ConditionProfile noMembers) throws UnsupportedMessageException {
        if (noMembers.profile(!hasMembers())) {
            throw UnsupportedMessageException.create();
        }
        return RDataFactory.createStringVector(new String[]{MEMBER_RE, MEMBER_IM}, RDataFactory.COMPLETE_VECTOR);
    }

    @ExportMessage
    boolean isMemberReadable(String member,
                    @Cached.Shared("noMembers") @Cached("createBinaryProfile()") ConditionProfile noMembers) {
        if (noMembers.profile(!hasMembers())) {
            return false;
        }
        return MEMBER_RE.equals(member) || MEMBER_IM.equals(member);
    }

    @ExportMessage
    Object readMember(String member,
                    @Cached.Shared("noMembers") @Cached("createBinaryProfile()") ConditionProfile noMembers,
                    @Cached.Exclusive @Cached("createBinaryProfile()") ConditionProfile unknownIdentifier,
                    @Cached.Exclusive @Cached("createBinaryProfile()") ConditionProfile isNullIdentifier,
                    @Cached.Exclusive @Cached("createBinaryProfile()") ConditionProfile isNA) throws UnknownIdentifierException, UnsupportedMessageException {
        if (noMembers.profile(!hasMembers())) {
            throw UnsupportedMessageException.create();
        }
        if (unknownIdentifier.profile(!isMemberReadable(member, noMembers))) {
            throw UnknownIdentifierException.create(member);
        }
        if (isNullIdentifier.profile(isNull(isNA))) {
            return new RInteropComplexNA(getDataAt(0));
        }
        if (MEMBER_RE.equals(member)) {
            return getDataAt(0).getRealPart();
        } else {
            return getDataAt(0).getImaginaryPart();
        }
    }

    @Ignore
    @Override
    public Object getDataAtAsObject(int index) {
        return getDataAt(index);
    }

    public abstract RComplex getDataAt(int index);

    public RComplex getDataAt(@SuppressWarnings("unused") Object store, int index) {
        return getDataAt(index);
    }

    @Ignore
    @Override
    public RComplexVector materialize() {
        RComplexVector result = RDataFactory.createComplexVector(getDataCopy(), isComplete());
        copyAttributes(result);
        MemoryCopyTracer.reportCopying(this, result);
        return result;
    }

    @TruffleBoundary
    protected void copyAttributes(RDoubleVector materialized) {
        materialized.copyAttributesFrom(this);
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

    @TruffleBoundary
    protected void copyAttributes(RComplexVector materialized) {
        materialized.copyAttributesFrom(this);
    }

    @Override
    protected RComplexVector internalCopy() {
        return RDataFactory.createComplexVector(getDataCopy(), isComplete());
    }

    @SuppressWarnings("unused")
    public void setDataAt(Object store, int index, RComplex value) {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unused")
    public void setDataAt(Object store, int index, double value) {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unused")
    public double getComplexPartAt(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public RType getRType() {
        return RType.Complex;
    }

    @Override
    public double[] getDataTemp() {
        return (double[]) super.getDataTemp();
    }

    @Override
    public double[] getReadonlyData() {
        return getDataCopy();
    }

    @Override
    public double[] getDataCopy() {
        int length = getLength();
        double[] result = new double[length << 1];
        for (int i = 0; i < length; i++) {
            RComplex c = getDataAt(i);
            result[i * 2] = c.getRealPart();
            result[i * 2 + 1] = c.getImaginaryPart();
        }
        return result;
    }

    @Override
    public Object getInternalManagedData() {
        return null;
    }

    @Override
    public final RComplexVector createEmptySameType(int newLength, boolean newIsComplete) {
        return RDataFactory.createComplexVector(new double[newLength << 1], newIsComplete);
    }

    // ------------------------------
    // VectorDataLibrary

    @ExportMessage
    @SuppressWarnings("static")
    public NACheck getNACheck(@Shared("naCheck") @Cached NACheck na) {
        na.enable(!isComplete());
        return na;
    }

    @ExportMessage
    @SuppressWarnings("static")
    public RType getType() {
        return RType.Complex;
    }

    @ExportMessage(name = "isComplete", library = VectorDataLibrary.class)
    public boolean datLibIsComplete() {
        return this.isComplete();
    }

    @ExportMessage(name = "getLength", library = VectorDataLibrary.class)
    public int dataLibGetLength() {
        return getLength();
    }

    @ExportMessage
    public boolean isWriteable() {
        return isMaterialized();
    }

    @ExportMessage(name = "materialize", library = VectorDataLibrary.class)
    public Object dataLibMaterialize() {
        return materialize();
    }

    @ExportMessage(name = "copy", library = VectorDataLibrary.class)
    public Object dataLibCopy(@SuppressWarnings("unused") boolean deep) {
        return copy();
    }

    @ExportMessage(name = "copyResized", library = VectorDataLibrary.class)
    public Object dataLibCopyResized(int newSize, @SuppressWarnings("unused") boolean deep, boolean fillNA) {
        return this.copyResized(newSize, fillNA);
    }

    @ExportMessage
    public SeqIterator iterator(@Shared("naCheck") @Cached() NACheck naCheck,
                    @Shared("SeqItLoopProfile") @Cached("createCountingProfile()") LoopConditionProfile loopProfile) {
        SeqIterator it = new SeqIterator(getInternalStore(), getLength());
        naCheck.enable(!isComplete());
        it.initLoopConditionProfile(loopProfile);
        return it;
    }

    @ExportMessage
    @SuppressWarnings("static")
    public boolean next(SeqIterator it, boolean withWrap,
                    @Shared("SeqItLoopProfile") @Cached("createCountingProfile()") LoopConditionProfile loopProfile) {
        return it.next(loopProfile, withWrap);
    }

    @ExportMessage
    public RandomAccessIterator randomAccessIterator(@Shared("naCheck") @Cached() NACheck naCheck) {
        naCheck.enable(!isComplete());
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
    public void commitWriteIterator(SeqWriteIterator iterator, boolean neverSeenNA) {
        iterator.commit();
        commitWrites(neverSeenNA);
    }

    @ExportMessage
    public void commitRandomAccessWriteIterator(RandomAccessWriteIterator iterator, boolean neverSeenNA) {
        iterator.commit();
        commitWrites(neverSeenNA);
    }

    private void commitWrites(boolean neverSeenNA) {
        if (!neverSeenNA) {
            setComplete(false);
        }
    }

    @ExportMessage
    public double[] getComplexDataCopy() {
        return getDataCopy();
    }

    @ExportMessage
    public double[] getReadonlyComplexData() {
        return getReadonlyData();
    }

    @ExportMessage
    public RComplex getComplexAt(int index,
                    @Shared("naCheck") @Cached NACheck na) {
        RComplex result = getDataAt(index);
        na.enable(!isComplete());
        na.check(result);
        return result;
    }

    @ExportMessage
    public RComplex getNextComplex(SeqIterator it,
                    @Shared("naCheck") @Cached NACheck na) {
        RComplex result = getDataAt(it.getStore(), it.getIndex());
        na.check(result);
        return result;
    }

    @ExportMessage
    public RComplex getComplex(RandomAccessIterator it, int index,
                    @Shared("naCheck") @Cached NACheck na) {
        RComplex result = getDataAt(it.getStore(), index);
        na.check(result);
        return result;
    }

    @ExportMessage
    public void setComplexAt(int index, RComplex value) {
        setDataAt(getInternalStore(), index, value);
        if (RRuntime.isNA(value)) {
            setComplete(false);
        }
    }

    @ExportMessage
    public void setNextComplex(SeqWriteIterator it, RComplex value) {
        setDataAt(it.getStore(), it.getIndex(), value);
    }

    @ExportMessage
    public void setComplex(RandomAccessWriteIterator it, int index, RComplex value) {
        setDataAt(it.getStore(), index, value);
    }
}
