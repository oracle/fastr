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

import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.VectorDataLibraryUtils.RandomAccessIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibraryUtils.SeqIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibraryUtils.Iterator;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

import java.util.Arrays;

@ExportLibrary(RIntVectorDataLibrary.class)
@ExportLibrary(VectorDataLibrary.class)
class RIntArrayVectorData extends RIntVectorData {
    private final int[] data;
    private boolean complete;

    RIntArrayVectorData(int[] data, boolean complete) {
        this.data = data;
        this.complete = complete;
    }

    @ExportMessage(library = RIntVectorDataLibrary.class)
    @ExportMessage(library = VectorDataLibrary.class)
    @Override
    public int getLength() {
        return data.length;
    }

    @ExportMessage(library = RIntVectorDataLibrary.class)
    public RIntArrayVectorData materialize() {
        return this;
    }

    @ExportMessage(library = RIntVectorDataLibrary.class)
    public boolean isWriteable() {
        return true;
    }

    @ExportMessage(library = VectorDataLibrary.class)
    public boolean isMaterialized() {
        return true;
    }

    @ExportMessage
    public RIntArrayVectorData copy(@SuppressWarnings("unused") boolean deep) {
        return new RIntArrayVectorData(Arrays.copyOf(data, data.length), complete);
    }

    @ExportMessage
    public RIntArrayVectorData copyResized(int newSize, @SuppressWarnings("unused") boolean deep, boolean fillNA) {
        int[] newData = Arrays.copyOf(data, newSize);
        if (fillNA) {
            Arrays.fill(newData, data.length, newData.length, RRuntime.INT_NA);
        }
        return new RIntArrayVectorData(newData, complete);
    }

    // TODO: this will be message exported by the generic VectorDataLibrary
    // @ExportMessage
    public void transferElement(RVectorData destination, int index,
                    @CachedLibrary("destination") RIntVectorDataLibrary dataLib) {
        dataLib.setIntAt(destination, index, data[index]);
    }

    @ExportMessage(library = RIntVectorDataLibrary.class)
    @ExportMessage(library = VectorDataLibrary.class)
    @Override
    public boolean isComplete() {
        return complete;
    }

    @ExportMessage
    public int[] getReadonlyIntData() {
        return data;
    }

    @ExportMessage
    public int[] getIntDataCopy() {
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

    @Override
    @ExportMessage
    public int getIntAt(int index) {
        return data[index];
    }

    @ExportMessage
    public int getNext(SeqIterator it) {
        return getStore(it)[it.getIndex()];
    }

    @ExportMessage
    public int getAt(RandomAccessIterator it, int index) {
        return getStore(it)[index];
    }

    @Override
    @ExportMessage
    public void setIntAt(int index, int value, NACheck naCheck) {
        updateComplete(value, naCheck);
        data[index] = value;
    }

    @ExportMessage
    public void setDataAtAsObject(int index, Object value, NACheck naCheck) {
        setIntAt(index, (int) value, naCheck);
    }

    @ExportMessage
    public void setNext(SeqIterator it, int value, NACheck naCheck) {
        updateComplete(value, naCheck);
        getStore(it)[it.getIndex()] = value;
    }

    @ExportMessage
    public void setAt(RandomAccessIterator it, int index, int value, NACheck naCheck) {
        updateComplete(value, naCheck);
        getStore(it)[index] = value;
    }

    private void updateComplete(int value, NACheck naCheck) {
        if (naCheck.check(value)) {
            this.complete = false;
        }
    }

    private static int[] getStore(Iterator it) {
        return (int[]) it.getStore();
    }
}
