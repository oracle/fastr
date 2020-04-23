/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.LoopConditionProfile;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.RandomAccessIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.RandomAccessWriteIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.SeqIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.Iterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.SeqWriteIterator;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

import java.util.Arrays;

@ExportLibrary(VectorDataLibrary.class)
class RRawArrayVectorData implements TruffleObject, VectorDataWithOwner {
    private final byte[] data;

    RRawArrayVectorData(byte[] data) {
        this.data = data;
    }

    @Override
    public void setOwner(RAbstractContainer newOwner) {

    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public NACheck getNACheck() {
        return NACheck.getDisabled();
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public final RType getType() {
        return RType.Raw;
    }

    @ExportMessage
    public int getLength() {
        return data.length;
    }

    @ExportMessage
    public RRawArrayVectorData materialize() {
        return this;
    }

    @ExportMessage
    public boolean isWriteable() {
        return true;
    }

    @ExportMessage
    public RRawArrayVectorData copy(@SuppressWarnings("unused") boolean deep) {
        return new RRawArrayVectorData(Arrays.copyOf(data, data.length));
    }

    @ExportMessage
    public RRawArrayVectorData copyResized(int newSize, @SuppressWarnings("unused") boolean deep, boolean fillNA) {
        byte[] newData = Arrays.copyOf(data, newSize);
        if (!fillNA) {
            assert data.length > 0 : "cannot call resize on empty vector if fillNA == false";
            // NA is 00 for raw
            for (int i = data.length, j = 0; i < newSize; ++i, j = Utils.incMod(j, data.length)) {
                newData[i] = data[j];
            }
        }
        return new RRawArrayVectorData(newData);
    }

    @ExportMessage
    public boolean isComplete() {
        return true;
    }

    @ExportMessage
    public byte[] getReadonlyRawData() {
        return data;
    }

    @ExportMessage
    public byte[] getRawDataCopy() {
        return Arrays.copyOf(data, data.length);
    }

    // Read access to the elements:

    @ExportMessage
    public SeqIterator iterator(
                    @Shared("naCheck") @Cached() NACheck naCheck,
                    @Shared("SeqItLoopProfile") @Cached("createCountingProfile()") LoopConditionProfile loopProfile) {
        SeqIterator it = new SeqIterator(data, data.length);
        naCheck.enable(!isComplete());
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
    public RandomAccessIterator randomAccessIterator(@Shared("naCheck") @Cached() NACheck naCheck) {
        naCheck.enable(!isComplete());
        return new RandomAccessIterator(data);
    }

    @ExportMessage
    public byte getRawAt(int index, @Shared("naCheck") @Cached() NACheck naCheck) {
        byte value = data[index];
        naCheck.enable(!isComplete());
        naCheck.check(value);
        return value;
    }

    @ExportMessage
    public byte getNextRaw(SeqIterator it, @Shared("naCheck") @Cached() NACheck naCheck) {
        byte value = getStore(it)[it.getIndex()];
        naCheck.check(value);
        return value;
    }

    @ExportMessage
    public byte getRaw(RandomAccessIterator it, int index, @Shared("naCheck") @Cached() NACheck naCheck) {
        byte value = getStore(it)[index];
        naCheck.check(value);
        return value;
    }

    // Write access to the elements:

    @ExportMessage
    public SeqWriteIterator writeIterator() {
        return new SeqWriteIterator(data, data.length);
    }

    @ExportMessage
    public RandomAccessWriteIterator randomAccessWriteIterator() {
        return new RandomAccessWriteIterator(data);
    }

    @ExportMessage
    public void commitWriteIterator(SeqWriteIterator iterator, @SuppressWarnings("unused") boolean neverSeenNA) {
        iterator.commit();
    }

    @ExportMessage
    public void commitRandomAccessWriteIterator(RandomAccessWriteIterator iterator, @SuppressWarnings("unused") boolean neverSeenNA) {
        iterator.commit();
    }

    @ExportMessage
    public void setRawAt(int index, byte value) {
        data[index] = value;
    }

    @ExportMessage
    public void setNextRaw(SeqWriteIterator it, byte value) {
        getStore(it)[it.getIndex()] = value;
    }

    @ExportMessage
    public void setRaw(RandomAccessWriteIterator it, int index, byte value) {
        getStore(it)[index] = value;
    }

    private static byte[] getStore(Iterator it) {
        return (byte[]) it.getStore();
    }
}
