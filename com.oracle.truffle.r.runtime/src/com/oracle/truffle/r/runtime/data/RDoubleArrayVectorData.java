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

import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.RandomAccessIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.SeqIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.Iterator;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

import java.util.Arrays;

@ExportLibrary(VectorDataLibrary.class)
class RDoubleArrayVectorData implements TruffleObject {
    private final double[] data;
    private boolean complete;

    RDoubleArrayVectorData(double[] data, boolean complete) {
        this.data = data;
        this.complete = complete;
    }

    @ExportMessage
    public int getLength() {
        return data.length;
    }

    @ExportMessage
    public RDoubleArrayVectorData materialize() {
        return this;
    }

    @ExportMessage
    public boolean isWriteable() {
        return true;
    }

    @ExportMessage
    public RDoubleArrayVectorData copy(@SuppressWarnings("unused") boolean deep) {
        return new RDoubleArrayVectorData(Arrays.copyOf(data, data.length), complete);
    }

    @ExportMessage
    public RDoubleArrayVectorData copyResized(int newSize, @SuppressWarnings("unused") boolean deep, boolean fillNA) {
        double[] newData = Arrays.copyOf(data, newSize);
        if (fillNA) {
            Arrays.fill(newData, data.length, newData.length, RRuntime.DOUBLE_NA);
        }
        return new RDoubleArrayVectorData(newData, complete);
    }

    @ExportMessage
    public boolean isComplete() {
        return complete;
    }

    @ExportMessage
    public double[] getReadonlyDoubleData() {
        return data;
    }

    @ExportMessage
    public double[] getDoubleDataCopy() {
        return Arrays.copyOf(data, data.length);
    }

    @ExportMessage
    public SeqIterator iterator() {
        return new SeqIterator(data, data.length);
    }

    @ExportMessage
    public RandomAccessIterator randomAccessIterator() {
        return new RandomAccessIterator(data, data.length);
    }

    @ExportMessage
    public Object getDataAtAsObject(int index) {
        return getDoubleAt(index);
    }

    @ExportMessage
    public double getDoubleAt(int index) {
        return data[index];
    }

    @ExportMessage
    public double getNextDouble(SeqIterator it) {
        return getStore(it)[it.getIndex()];
    }

    @ExportMessage
    public double getDouble(RandomAccessIterator it, int index) {
        return getStore(it)[index];
    }

    @ExportMessage
    public void setDoubleAt(int index, double value, NACheck naCheck) {
        updateComplete(value, naCheck);
        data[index] = value;
    }

    @ExportMessage
    public void setDataAtAsObject(int index, Object value, NACheck naCheck) {
        setDoubleAt(index, (double) value, naCheck);
    }

    @ExportMessage
    public void setNextDouble(SeqIterator it, double value, NACheck naCheck) {
        updateComplete(value, naCheck);
        getStore(it)[it.getIndex()] = value;
    }

    @ExportMessage
    public void setDouble(RandomAccessIterator it, int index, double value, NACheck naCheck) {
        updateComplete(value, naCheck);
        getStore(it)[index] = value;
    }

    private void updateComplete(double value, NACheck naCheck) {
        if (naCheck.check(value)) {
            this.complete = false;
        }
    }

    private static double[] getStore(Iterator it) {
        return (double[]) it.getStore();
    }
}
