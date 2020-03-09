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
import com.oracle.truffle.r.runtime.data.VectorDataLibraryUtils.RandomAccessIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibraryUtils.SeqIterator;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

@ExportLibrary(RDoubleVectorDataLibrary.class)
@ExportLibrary(VectorDataLibrary.class)
public class RDoubleNativeVectorData extends RDoubleVectorData {
    // We need the vector, so that we can easily use the existing NativeDataAccess methods
    // TODO: this field should be replaced with address/length fields and
    // the address/length fields and logic should be removed from NativeMirror
    // including the releasing of the native memory
    private final RDoubleVector vec;

    public RDoubleNativeVectorData(RDoubleVector vec) {
        this.vec = vec;
    }

    @ExportMessage(library = RDoubleVectorDataLibrary.class)
    @ExportMessage(library = VectorDataLibrary.class)
    @Override
    public int getLength() {
        return NativeDataAccess.getDataLength(vec, null);
    }

    @ExportMessage(library = RDoubleVectorDataLibrary.class)
    public RDoubleNativeVectorData materialize() {
        return this;
    }

    @ExportMessage(library = RDoubleVectorDataLibrary.class)
    public boolean isWriteable() {
        return true;
    }

    @ExportMessage(library = VectorDataLibrary.class)
    public boolean isMaterialized() {
        return true;
    }

    @ExportMessage
    public RDoubleArrayVectorData copy(@SuppressWarnings("unused") boolean deep) {
        double[] data = NativeDataAccess.copyDoubleNativeData(vec.getNativeMirror());
        return new RDoubleArrayVectorData(data, RDataFactory.INCOMPLETE_VECTOR);
    }

    @ExportMessage
    public RDoubleArrayVectorData copyResized(int newSize, boolean deep, boolean fillNA) {
        return copy(deep).copyResized(newSize, deep, fillNA);
    }

    // TODO: this will be message exported by the generic VectorDataLibrary
    // @ExportMessage
    public void transferElement(RVectorData destination, int index,
                    @CachedLibrary("destination") RDoubleVectorDataLibrary dataLib) {
        dataLib.setDoubleAt(destination, index, getDoubleAt(index));
    }

    @ExportMessage
    public double[] getReadonlyDoubleData() {
        return NativeDataAccess.copyDoubleNativeData(vec.getNativeMirror());
    }

    // TODO: actually use the store in the iterator, which should be just the "address" (Long)

    @ExportMessage
    public SeqIterator iterator() {
        return new SeqIterator(vec, NativeDataAccess.getDataLength(vec, null));
    }

    @ExportMessage
    public RandomAccessIterator randomAccessIterator() {
        return new RandomAccessIterator(vec, NativeDataAccess.getDataLength(vec, null));
    }

    @ExportMessage
    @Override
    public double getDoubleAt(int index) {
        return NativeDataAccess.getData(vec, null, index);
    }

    @ExportMessage
    public double getNext(SeqIterator it) {
        return NativeDataAccess.getData(vec, null, it.getIndex());
    }

    @ExportMessage
    public double getAt(@SuppressWarnings("unused") RandomAccessIterator it, int index) {
        return NativeDataAccess.getData(vec, null, index);
    }

    @Override
    @ExportMessage
    public void setDoubleAt(int index, double value, @SuppressWarnings("unused") NACheck naCheck) {
        NativeDataAccess.setData(vec, null, index, value);
    }

    @ExportMessage
    public void setDataAtAsObject(int index, Object value, @SuppressWarnings("unused") NACheck naCheck) {
        NativeDataAccess.setData(vec, null, index, (double) value);
    }

    @ExportMessage
    public void setNext(SeqIterator it, double value, @SuppressWarnings("unused") NACheck naCheck) {
        NativeDataAccess.setData(vec, null, it.getIndex(), value);
    }

    @ExportMessage
    public void setAt(@SuppressWarnings("unused") RandomAccessIterator it, int index, double value, @SuppressWarnings("unused") NACheck naCheck) {
        NativeDataAccess.setData(vec, null, index, value);
    }
}
