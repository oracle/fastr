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

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.library.ExportMessage.Ignore;
import com.oracle.truffle.api.profiles.LoopConditionProfile;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.data.MemoryCopyTracer;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RRaw;
import com.oracle.truffle.r.runtime.data.RRawVector;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.RandomAccessIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.RandomAccessWriteIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.SeqIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.SeqWriteIterator;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

import java.util.Arrays;

@ExportLibrary(VectorDataLibrary.class)
public abstract class RAbstractRawVector extends RAbstractNumericVector {

    public RAbstractRawVector(boolean complete) {
        super(complete);
    }

    @Override
    protected boolean isScalarNA() {
        assert getLength() == 1;
        return false;
    }

    @Override
    protected Object getScalarValue() {
        assert getLength() == 1;
        return getRawDataAt(0);
    }

    @Override
    @Ignore
    public Object getDataAtAsObject(int index) {
        return RRaw.valueOf(getRawDataAt(index));
    }

    @SuppressWarnings("unused")
    public void setRawDataAt(Object store, int index, byte value) {
        throw new UnsupportedOperationException();
    }

    public abstract byte getRawDataAt(int index);

    public byte getRawDataAt(@SuppressWarnings("unused") Object store, int index) {
        return getRawDataAt(index);
    }

    @Override
    @Ignore
    public RRawVector materialize() {
        RRawVector result = RDataFactory.createRawVector(getDataCopy());
        MemoryCopyTracer.reportCopying(this, result);
        return result;
    }

    private byte[] copyResizedData(int size, boolean fillNA) {
        byte[] localData = getReadonlyData();
        byte[] newData = Arrays.copyOf(localData, size);
        if (!fillNA) {
            assert localData.length > 0 : "cannot call resize on empty vector if fillNA == false";
            // NA is 00 for raw
            for (int i = localData.length, j = 0; i < size; ++i, j = Utils.incMod(j, localData.length)) {
                newData[i] = localData[j];
            }
        }
        return newData;
    }

    @Override
    protected RRawVector internalCopyResized(int size, boolean fillNA, int[] dimensions) {
        return RDataFactory.createRawVector(copyResizedData(size, fillNA), dimensions);
    }

    @Override
    protected RRawVector internalCopy() {
        return RDataFactory.createRawVector(getDataCopy());
    }

    @Override
    public RType getRType() {
        return RType.Raw;
    }

    @Override
    public byte[] getDataTemp() {
        return (byte[]) super.getDataTemp();
    }

    @Override
    public byte[] getReadonlyData() {
        return getDataCopy();
    }

    @Override
    public byte[] getDataCopy() {
        int length = getLength();
        byte[] result = new byte[length];
        for (int i = 0; i < length; i++) {
            result[i] = getRawDataAt(i);
        }
        return result;
    }

    @Override
    public Object getInternalManagedData() {
        return null;
    }

    @Override
    public final RRawVector createEmptySameType(int newLength, boolean newIsComplete) {
        return RDataFactory.createRawVector(new byte[newLength]);
    }

    // ------------------------------
    // VectorDataLibrary

    @ExportMessage
    @SuppressWarnings("static")
    public NACheck getNACheck() {
        return NACheck.getDisabled();
    }

    @ExportMessage
    @SuppressWarnings("static")
    public RType getType() {
        return RType.Raw;
    }

    @ExportMessage(name = "isComplete", library = VectorDataLibrary.class)
    public boolean datLibIsComplete() {
        return true;
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
    public SeqIterator iterator(@Shared("SeqItLoopProfile") @Cached("createCountingProfile()") LoopConditionProfile loopProfile) {
        SeqIterator it = new SeqIterator(getInternalStore(), getLength());
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
    public byte[] getRawDataCopy() {
        return getDataCopy();
    }

    @ExportMessage
    public byte[] getReadonlyRawData() {
        return getReadonlyData();
    }

    @ExportMessage
    public byte getRawAt(int index) {
        return getRawDataAt(index);
    }

    @ExportMessage
    public byte getNextRaw(SeqIterator it) {
        return getRawDataAt(it.getStore(), it.getIndex());
    }

    @ExportMessage
    public byte getRaw(RandomAccessIterator it, int index) {
        return getRawDataAt(it.getStore(), index);
    }

    @ExportMessage
    public void setRawAt(int index, byte value) {
        setRawDataAt(getInternalStore(), index, value);
    }

    @ExportMessage
    public void setNextRaw(SeqWriteIterator it, byte value) {
        setRawDataAt(it.getStore(), it.getIndex(), value);
    }

    @ExportMessage
    public void setRaw(RandomAccessWriteIterator it, int index, byte value) {
        setRawDataAt(it.getStore(), index, value);
    }
}
