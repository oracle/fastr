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

import static com.oracle.truffle.r.runtime.data.model.RAbstractVector.ENABLE_COMPLETE;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.ValueType;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.profiles.LoopConditionProfile;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.Iterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.RandomAccessIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.SeqIterator;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

import java.util.Arrays;

@ExportLibrary(VectorDataLibrary.class)
public class RIntSeqVectorData implements RSeq, TruffleObject {
    private final int start;
    private final int stride;
    private final int length;

    public RIntSeqVectorData(int start, int stride, int length) {
        this.start = start;
        this.stride = stride;
        this.length = length;
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public NACheck getNACheck() {
        return NACheck.getDisabled();
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public final RType getType() {
        return RType.Integer;
    }

    @Override
    public Object getStartObject() {
        return start;
    }

    @Override
    public Object getStrideObject() {
        return stride;
    }

    public int getStart() {
        return start;
    }

    public int getStride() {
        return stride;
    }

    public int getEnd() {
        return start + (getLength() - 1) * stride;
    }

    public int getIndexFor(int element) {
        int first = Math.min(getStart(), getEnd());
        int last = Math.max(getStart(), getEnd());
        if (element < first || element > last) {
            return -1;
        }
        if ((element - getStart()) % getStride() == 0) {
            return (element - getStart()) / getStride();
        }
        return -1;
    }

    @ExportMessage
    @Override
    public int getLength() {
        return length;
    }

    @ExportMessage
    public RIntArrayVectorData materialize(@Shared("naCheck") @Cached() NACheck naCheck) {
        return new RIntArrayVectorData(getIntDataCopy(naCheck), isComplete());
    }

    @ExportMessage
    public RIntSeqVectorData copy(@SuppressWarnings("unused") boolean deep) {
        return new RIntSeqVectorData(start, stride, length);
    }

    @ExportMessage
    public RIntArrayVectorData copyResized(int newSize, @SuppressWarnings("unused") boolean deep, boolean fillNA,
                    @Shared("naCheck") @Cached() NACheck naCheck) {
        int[] newData = getDataAsArray(newSize, naCheck);
        if (fillNA) {
            Arrays.fill(newData, length, newData.length, RRuntime.INT_NA);
        }
        return new RIntArrayVectorData(newData, RDataFactory.INCOMPLETE_VECTOR);
    }

    @ExportMessage
    public boolean isComplete() {
        return ENABLE_COMPLETE;
    }

    @ExportMessage
    public boolean isSorted(boolean descending, @SuppressWarnings("unused") boolean naLast) {
        return descending ? stride < 0 : stride >= 0;
    }

    @ExportMessage
    public int[] getIntDataCopy(@Shared("naCheck") @Cached() NACheck naCheck) {
        return getDataAsArray(length, naCheck);
    }

    // Read access to the elements:

    @ExportMessage
    public SeqIterator iterator(@Shared("naCheck") @Cached() NACheck naCheck,
                    @Shared("SeqItLoopProfile") @Cached("createCountingProfile()") LoopConditionProfile loopProfile) {
        SeqIterator it = new SeqIterator(new IteratorData(start, stride), length);
        naCheck.enable(false);
        it.initLoopConditionProfile(loopProfile);
        return it;
    }

    @ExportMessage
    public boolean next(SeqIterator it, boolean withWrap,
                    @Shared("SeqItLoopProfile") @Cached("createCountingProfile()") LoopConditionProfile loopProfile) {
        return it.next(loopProfile, withWrap);
    }

    @ExportMessage
    public RandomAccessIterator randomAccessIterator(@Shared("naCheck") @Cached() NACheck naCheck) {
        naCheck.enable(false);
        return new RandomAccessIterator(new IteratorData(start, stride));
    }

    @ExportMessage
    public int getIntAt(int index,
                    @Shared("naCheck") @Cached() NACheck naCheck) {
        assert index < length;
        int value = start + stride * index;
        naCheck.check(value);
        return value;
    }

    @ExportMessage
    public int getNextInt(SeqIterator it, @Shared("naCheck") @Cached() NACheck naCheck) {
        IteratorData data = getStore(it);
        int value = data.start + data.stride * it.getIndex();
        naCheck.check(value);
        return value;
    }

    @ExportMessage
    public int getInt(RandomAccessIterator it, int index, @Shared("naCheck") @Cached() NACheck naCheck) {
        IteratorData data = getStore(it);
        int value = data.start + data.stride * index;
        naCheck.check(value);
        return value;
    }

    // Utility methods:

    private int[] getDataAsArray(int newLength, @SuppressWarnings("unused") NACheck naCheck) {
        int[] data = new int[newLength];
        int startLocal = start;
        int strideLocal = stride;
        for (int i = 0; i < Math.min(newLength, length); i++) {
            data[i] = startLocal + strideLocal * i;
        }
        return data;
    }

    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        return "[" + start + " - " + getEnd() + "]";
    }

    private static IteratorData getStore(Iterator it) {
        return (IteratorData) it.getStore();
    }

    // We use a fresh new class for the iterator data in order to help the escape analysis
    @ValueType
    private static final class IteratorData {
        public final int start;
        public final int stride;

        private IteratorData(int start, int stride) {
            this.start = start;
            this.stride = stride;
        }
    }
}
