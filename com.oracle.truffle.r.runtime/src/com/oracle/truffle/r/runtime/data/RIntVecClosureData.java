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
import com.oracle.truffle.r.runtime.data.RIntVectorDataLibrary.RandomAccessIterator;
import com.oracle.truffle.r.runtime.data.RIntVectorDataLibrary.SeqIterator;
import com.oracle.truffle.r.runtime.data.closures.RClosure;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess.RandomIterator;

@ExportLibrary(RIntVectorDataLibrary.class)
public class RIntVecClosureData extends RIntVectorData implements RClosure {
    private final RAbstractVector vector;

    public RIntVecClosureData(RAbstractVector vector) {
        this.vector = vector;
    }

    @ExportMessage
    @Override
    public int getLength() {
        return vector.getLength();
    }

    @ExportMessage
    public RIntArrayVectorData materialize() {
        throw new RuntimeException("TODO?");
    }

    @ExportMessage
    public boolean isWriteable() {
        return false;
    }

    @SuppressWarnings("unused")
    @ExportMessage
    public RIntArrayVectorData copy(@SuppressWarnings("unused") boolean deep) {
        throw new RuntimeException("TODO?");
    }

    @SuppressWarnings("unused")
    @ExportMessage
    public RIntArrayVectorData copyResized(int newSize, boolean deep, boolean fillNA) {
        throw new RuntimeException("TODO?");
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
        return false;
    }

    @ExportMessage
    public int[] getReadonlyIntData() {
        // XXX TODO throw new RuntimeException("TODO?");
        return getIntDataCopy();
    }

    @ExportMessage
    public int[] getIntDataCopy() {
        // XXX TODO throw new RuntimeException("TODO?");
        int[] res = new int[getLength()];
        for (int i = 0; i < res.length; i++) {
            res[i] = getIntAt(i);
        }
        return res;
    }

    // TODO: the accesses may be done more efficiently with nodes and actually using the "store" in
    // the iterator object

    @ExportMessage
    public SeqIterator iterator() {
        return new SeqIterator(null, getLength());
    }

    @ExportMessage
    public RandomAccessIterator randomAccessIterator() {
        return new RandomAccessIterator(null, getLength());
    }

    @ExportMessage
    @Override
    public int getIntAt(int index) {
        VectorAccess access = vector.slowPathAccess();
        RandomIterator it = access.randomAccess(vector);
        return access.getInt(it, index);
    }

    @ExportMessage
    public int getNext(SeqIterator it) {
        return getIntAt(it.getIndex());
    }

    @ExportMessage
    public int getAt(@SuppressWarnings("unused") RandomAccessIterator it, int index) {
        return getIntAt(index);
    }

    // RClosure overrides:

    @Override
    public Object getDelegateDataAt(int idx) {
        return vector.getDataAtAsObject(idx);
    }

    @Override
    public RAbstractVector getDelegate() {
        return vector;
    }
}
