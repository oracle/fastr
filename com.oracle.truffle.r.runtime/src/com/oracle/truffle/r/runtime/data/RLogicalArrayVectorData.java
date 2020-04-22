/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.runtime.data.model.RAbstractVector.ENABLE_COMPLETE;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.LoopConditionProfile;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.RandomAccessIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.RandomAccessWriteIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.SeqIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.Iterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.SeqWriteIterator;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

import java.util.Arrays;

@ExportLibrary(VectorDataLibrary.class)
class RLogicalArrayVectorData implements TruffleObject, VectorDataWithOwner {
    private final byte[] data;
    private boolean complete;
    private RLogicalVector owner;

    RLogicalArrayVectorData(byte[] data, boolean complete) {
        this.data = data;
        this.complete = complete && ENABLE_COMPLETE;
    }

    @Override
    public void setOwner(RAbstractVector newOwner) {
        owner = (RLogicalVector) newOwner;
        owner.setComplete(complete);
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public NACheck getNACheck(@Shared("naCheck") @Cached() NACheck na) {
        na.enable(!isComplete());
        return na;
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public final RType getType() {
        return RType.Logical;
    }

    @ExportMessage
    public int getLength() {
        return data.length;
    }

    @ExportMessage
    public RLogicalArrayVectorData materialize() {
        return this;
    }

    @ExportMessage
    public boolean isWriteable() {
        return true;
    }

    @ExportMessage
    public RLogicalArrayVectorData copy(@SuppressWarnings("unused") boolean deep) {
        return new RLogicalArrayVectorData(Arrays.copyOf(data, data.length), isComplete());
    }

    @ExportMessage
    public RLogicalArrayVectorData copyResized(int newSize, @SuppressWarnings("unused") boolean deep, boolean fillNA) {
        byte[] newData = Arrays.copyOf(data, newSize);
        if (fillNA) {
            Arrays.fill(newData, data.length, newData.length, RRuntime.LOGICAL_NA);
        }
        return new RLogicalArrayVectorData(newData, isComplete());
    }

    @ExportMessage
    public boolean isComplete() {
        return complete && ENABLE_COMPLETE;
    }

    @ExportMessage
    public byte[] getReadonlyLogicalData() {
        return data;
    }

    @ExportMessage
    public byte[] getLogicalDataCopy() {
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
    public byte getLogicalAt(int index, @Shared("naCheck") @Cached() NACheck naCheck) {
        byte value = data[index];
        naCheck.enable(!isComplete());
        naCheck.check(value);
        return value;
    }

    @ExportMessage
    public byte getNextLogical(SeqIterator it, @Shared("naCheck") @Cached() NACheck naCheck) {
        byte value = getStore(it)[it.getIndex()];
        naCheck.check(value);
        return value;
    }

    @ExportMessage
    public byte getLogical(RandomAccessIterator it, int index, @Shared("naCheck") @Cached() NACheck naCheck) {
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
    public void commitWriteIterator(SeqWriteIterator iterator, boolean neverSeenNA, @Shared("setCompleteProfile") @Cached BranchProfile setCompleteProfile) {
        iterator.commit();
        commitWrites(neverSeenNA, setCompleteProfile);
    }

    @ExportMessage
    public void commitRandomAccessWriteIterator(RandomAccessWriteIterator iterator, boolean neverSeenNA, @Shared("setCompleteProfile") @Cached BranchProfile setCompleteProfile) {
        iterator.commit();
        commitWrites(neverSeenNA, setCompleteProfile);
    }

    private void commitWrites(boolean neverSeenNA, BranchProfile setCompleteProfile) {
        if (!neverSeenNA) {
            setCompleteProfile.enter();
            complete = false;
            owner.setComplete(false);
        }
    }

    @ExportMessage
    public void setLogicalAt(int index, byte value, @Shared("setCompleteProfile") @Cached BranchProfile setCompleteProfile) {
        data[index] = value;
        if (RRuntime.isNA(value)) {
            setCompleteProfile.enter();
            complete = false;
            owner.setComplete(false);
        }
    }

    @ExportMessage
    public void setNextLogical(SeqWriteIterator it, byte value) {
        getStore(it)[it.getIndex()] = value;
    }

    @ExportMessage
    public void setLogical(RandomAccessWriteIterator it, int index, byte value) {
        getStore(it)[index] = value;
    }

    private static byte[] getStore(Iterator it) {
        return (byte[]) it.getStore();
    }
}
