/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives.ValueType;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RIntVectorDataLibrary.Iterator;
import com.oracle.truffle.r.runtime.data.RIntVectorDataLibrary.RandomAccessIterator;
import com.oracle.truffle.r.runtime.data.RIntVectorDataLibrary.SeqIterator;

import java.util.Arrays;

@ExportLibrary(RIntVectorDataLibrary.class)
public class RIntSeqVectorData extends RIntVectorData implements RSeq {
    private final int start;
    private final int stride;
    private final int length;

    public RIntSeqVectorData(int start, int stride, int length) {
        this.start = start;
        this.stride = stride;
        this.length = length;
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
    public RIntArrayVectorData materialize() {
        return new RIntArrayVectorData(getReadonlyIntData(), isComplete());
    }

    @ExportMessage
    public boolean isWriteable() {
        return false;
    }

    @ExportMessage
    public RIntSeqVectorData copy(@SuppressWarnings("unused") boolean deep) {
        return new RIntSeqVectorData(start, stride, length);
    }

    @ExportMessage
    public RIntArrayVectorData copyResized(int newSize, @SuppressWarnings("unused") boolean deep, boolean fillNA) {
        int[] newData = getDataAsArray(newSize);
        if (fillNA) {
            Arrays.fill(newData, length, newData.length, RRuntime.INT_NA);
        }
        return new RIntArrayVectorData(newData, RDataFactory.INCOMPLETE_VECTOR);
    }

    // TODO: this will be message exported by the generic VectorDataLibrary
    // @ExportMessage
    public void transferElement(RVectorData destination, int index,
                    @CachedLibrary("destination") RIntVectorDataLibrary dataLib) {
        dataLib.setIntAt((RIntVectorData) destination, index, getIntAt(index));
    }

    @ExportMessage
    @Override
    public boolean isComplete() {
        return true;
    }

    @ExportMessage
    public boolean isSorted(boolean descending, @SuppressWarnings("unused") boolean naLast) {
        return descending ? stride < 0 : stride >= 0;
    }

    @ExportMessage
    public int[] getReadonlyIntData() {
        return getDataAsArray(length);
    }

    @ExportMessage
    public SeqIterator iterator() {
        return new SeqIterator(new IteratorData(start, stride), length);
    }

    @ExportMessage
    public RandomAccessIterator randomAccessIterator() {
        return new RandomAccessIterator(new IteratorData(start, stride), length);
    }

    @ExportMessage
    @Override
    public int getIntAt(int index) {
        assert index < length;
        return start + stride * index;
    }

    @ExportMessage
    public int getNext(SeqIterator it) {
        IteratorData data = getStore(it);
        return data.start + data.stride * it.getIndex();
    }

    @ExportMessage
    public int getAt(RandomAccessIterator it, int index) {
        IteratorData data = getStore(it);
        return data.start + data.stride * index;
    }

    private static IteratorData getStore(Iterator it) {
        return (IteratorData) it.getStore();
    }

    private int[] getDataAsArray(int newLength) {
        int[] data = new int[newLength];
        for (int i = 0; i < Math.min(newLength, length); i++) {
            data[i] = getIntAt(i);
        }
        return data;
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
