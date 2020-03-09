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

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.ValueType;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.VectorDataLibraryUtils.Iterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibraryUtils.RandomAccessIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibraryUtils.SeqIterator;

import java.util.Arrays;

@ExportLibrary(RDoubleVectorDataLibrary.class)
@ExportLibrary(VectorDataLibrary.class)
public class RDoubleSeqVectorData extends RDoubleVectorData implements RSeq {
    private final double start;
    private final double stride;
    private final int length;

    public RDoubleSeqVectorData(double start, double stride, int length) {
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

    public double getStart() {
        return start;
    }

    public double getStride() {
        return stride;
    }

    public double getEnd() {
        return start + (getLength() - 1) * stride;
    }

    @ExportMessage(library = RDoubleVectorDataLibrary.class)
    @ExportMessage(library = VectorDataLibrary.class)
    @Override
    public int getLength() {
        return length;
    }

    @ExportMessage(library = RDoubleVectorDataLibrary.class)
    public RDoubleArrayVectorData materialize() {
        return new RDoubleArrayVectorData(getReadonlyDoubleData(), isComplete());
    }

    @ExportMessage
    public RDoubleSeqVectorData copy(@SuppressWarnings("unused") boolean deep) {
        return new RDoubleSeqVectorData(start, stride, length);
    }

    @ExportMessage
    public RDoubleArrayVectorData copyResized(int newSize, @SuppressWarnings("unused") boolean deep, boolean fillNA) {
        double[] newData = getDataAsArray(newSize);
        if (fillNA) {
            Arrays.fill(newData, length, newData.length, RRuntime.DOUBLE_NA);
        }
        return new RDoubleArrayVectorData(newData, RDataFactory.INCOMPLETE_VECTOR);
    }

    // TODO: this will be message exported by the generic VectorDataLibrary
    // @ExportMessage
    public void transferElement(RVectorData destination, int index,
                    @CachedLibrary("destination") RDoubleVectorDataLibrary dataLib) {
        dataLib.setDoubleAt(destination, index, getDoubleAt(index));
    }

    @ExportMessage(library = RDoubleVectorDataLibrary.class)
    @ExportMessage(library = VectorDataLibrary.class)
    @Override
    public boolean isComplete() {
        return true;
    }

    @ExportMessage
    public boolean isSorted(boolean descending, @SuppressWarnings("unused") boolean naLast) {
        return descending ? stride < 0 : stride >= 0;
    }

    @ExportMessage
    public double[] getReadonlyDoubleData() {
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
    public double getDoubleAt(int index) {
        assert index < length;
        return start + stride * index;
    }

    @ExportMessage
    public double getNext(SeqIterator it) {
        IteratorData data = getStore(it);
        return data.start + data.stride * it.getIndex();
    }

    @ExportMessage
    public double getAt(RandomAccessIterator it, int index) {
        IteratorData data = getStore(it);
        return data.start + data.stride * index;
    }

    private static IteratorData getStore(Iterator it) {
        return (IteratorData) it.getStore();
    }

    private double[] getDataAsArray(int newLength) {
        double[] data = new double[newLength];
        for (int i = 0; i < Math.min(newLength, length); i++) {
            data[i] = getDoubleAt(i);
        }
        return data;
    }

    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        return "[" + start + " - " + getEnd() + "]";
    }

    // We use a fresh new class for the iterator data in order to help the escape analysis
    @ValueType
    private static final class IteratorData {
        public final double start;
        public final double stride;

        private IteratorData(double start, double stride) {
            this.start = start;
            this.stride = stride;
        }
    }
}
