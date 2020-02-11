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

import com.oracle.truffle.api.library.GenerateLibrary;
import com.oracle.truffle.api.library.Library;
import com.oracle.truffle.api.library.LibraryFactory;
import com.oracle.truffle.r.runtime.data.VectorDataLibraryUtils.RandomAccessIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibraryUtils.SeqIterator;
import static com.oracle.truffle.r.runtime.data.VectorDataLibraryUtils.notWriteableError;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

// TODO: add "assertions" library, possible checks:
//  - isComplete after write corresponds to the written value
//  - something similar for isSorted?
@GenerateLibrary
public abstract class RDoubleVectorDataLibrary extends Library {

    static final LibraryFactory<RDoubleVectorDataLibrary> FACTORY = LibraryFactory.resolve(RDoubleVectorDataLibrary.class);

    public static LibraryFactory<RDoubleVectorDataLibrary> getFactory() {
        return FACTORY;
    }

    // ---------------------------------------------------------------------
    // Methods copy & pasted from VectorDataLibrary, specialized for double

    public abstract RDoubleVectorData materialize(RDoubleVectorData receiver);

    @SuppressWarnings("unused")
    public boolean isWriteable(RDoubleVectorData receiver) {
        return false;
    }

    public abstract RDoubleVectorData copy(RDoubleVectorData receiver, boolean deep);

    public abstract RDoubleVectorData copyResized(RDoubleVectorData receiver, int newSize, boolean deep, boolean fillNA);

    public abstract int getLength(RDoubleVectorData receiver);

    @SuppressWarnings("unused")
    public boolean isComplete(RDoubleVectorData receiver) {
        return false;
    }

    @SuppressWarnings("unused")
    public boolean isSorted(RDoubleVectorData receiver, boolean descending, boolean naLast) {
        return false;
    }

    public abstract SeqIterator iterator(RDoubleVectorData receiver);

    public abstract RandomAccessIterator randomAccessIterator(RDoubleVectorData receiver);

    // ---------------------------------------------------------------------
    // Methods specific to integer data

    /**
     * Gives a readonly Java array view on the data. The array may or may not be copy of the
     * underlying data. Note: if you need to send an array to the native code, you should use
     * {@code TODO:RDoubleVector#getDataPtr()} instead.
     */
    public abstract double[] getReadonlyDoubleData(RDoubleVectorData receiver);

    // TODO: switch this: implement getReadonlyDoubleData using getDoubleDataCopy

    public double[] getDoubleDataCopy(RDoubleVectorData receiver) {
        return getReadonlyDoubleData(receiver);
    }

    public abstract double getDoubleAt(RDoubleVectorData receiver, int index);

    public abstract double getNext(RDoubleVectorData receiver, SeqIterator it);

    public abstract double getAt(RDoubleVectorData receiver, RandomAccessIterator it, int index);

    /**
     * Sets the value under given index. The vector must be writeable (see
     * {@link #isWriteable(RDoubleVectorData)}. The {@code naCheck} is used to determine if it is
     * necessary to check whether {@code value} is {@code NA}. The {@code naCheck} must be "enabled"
     * on the source of the input data, i.e., the {@code value} argument. Using this overload makes
     * sense if this method is called multiple times with the same {@code naCheck} instance,
     * otherwise use the overload without the {@code naCheck}.
     */
    @SuppressWarnings("unused")
    public void setDoubleAt(RDoubleVectorData receiver, int index, double value, NACheck naCheck) {
        throw notWriteableError(RDoubleVectorData.class, "setDoubleAt");
    }

    @SuppressWarnings("unused")
    public void setNext(RDoubleVectorData receiver, SeqIterator it, double value, NACheck naCheck) {
        throw notWriteableError(RDoubleVectorData.class, "setDoubleAt");
    }

    @SuppressWarnings("unused")
    public void setAt(RDoubleVectorData receiver, RandomAccessIterator it, int index, double value, NACheck naCheck) {
        throw notWriteableError(RDoubleVectorData.class, "setDoubleAt");
    }

    public final void setDoubleAt(RDoubleVectorData receiver, int index, double value) {
        setDoubleAt(receiver, index, value, NACheck.getEnabled());
    }

    public final void setDoubleAt(RDoubleVectorData receiver, SeqIterator it, double value) {
        setNext(receiver, it, value, NACheck.getEnabled());
    }

    public final void setDoubleAt(RDoubleVectorData receiver, RandomAccessIterator it, int index, double value) {
        setAt(receiver, it, index, value, NACheck.getEnabled());
    }
}
