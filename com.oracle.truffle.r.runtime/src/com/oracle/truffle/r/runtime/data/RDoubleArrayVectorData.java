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

import static com.oracle.truffle.r.runtime.data.VectorDataLibrary.initInputNAChecks;
import static com.oracle.truffle.r.runtime.data.model.RAbstractVector.ENABLE_COMPLETE;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.profiles.LoopConditionProfile;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.RandomAccessIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.RandomAccessWriteIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.SeqIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.Iterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.SeqWriteIterator;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.ops.na.InputNACheck;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

import java.util.Arrays;

@ExportLibrary(VectorDataLibrary.class)
class RDoubleArrayVectorData implements TruffleObject, VectorDataWithOwner {
    private final double[] data;
    // this flag is used only to initialize the complete flag in the owner,
    // from then on, we read/write the owner's complete flag
    private final boolean dataInitiallyComplete;
    private RDoubleVector owner;

    RDoubleArrayVectorData(double[] data, boolean complete) {
        this.data = data;
        this.dataInitiallyComplete = complete && ENABLE_COMPLETE;
    }

    @Override
    public void setOwner(RAbstractVector newOwner) {
        boolean firstOwner = owner == null;
        owner = (RDoubleVector) newOwner;
        if (firstOwner) {
            owner.setComplete(dataInitiallyComplete);
        }
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public NACheck getNACheck(@Shared("naCheck") @Cached() NACheck na) {
        return na;
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public InputNACheck getInputNACheck(@Shared("inputNACheck") @Cached() InputNACheck na) {
        return na;
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public final RType getType() {
        return RType.Double;
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
        return new RDoubleArrayVectorData(Arrays.copyOf(data, data.length), isComplete());
    }

    @ExportMessage
    public RDoubleArrayVectorData copyResized(int newSize, @SuppressWarnings("unused") boolean deep, boolean fillNA) {
        double[] newData = Arrays.copyOf(data, newSize);
        if (fillNA) {
            Arrays.fill(newData, data.length, newData.length, RRuntime.DOUBLE_NA);
        }
        return new RDoubleArrayVectorData(newData, isComplete());
    }

    @ExportMessage
    public boolean isComplete() {
        return owner.isComplete() && ENABLE_COMPLETE;
    }

    @ExportMessage
    public double[] getReadonlyDoubleData() {
        return data;
    }

    @ExportMessage
    public double[] getDoubleDataCopy() {
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
    public boolean next(SeqIterator it, boolean withWrap,
                    @Shared("SeqItLoopProfile") @Cached("createCountingProfile()") LoopConditionProfile loopProfile) {
        return it.next(loopProfile, withWrap);
    }

    @ExportMessage
    public RandomAccessIterator randomAccessIterator(@Shared("naCheck") @Cached() NACheck naCheck) {
        naCheck.enable(!isComplete());
        return new RandomAccessIterator(data);
    }

    @ExportMessage
    public double getDoubleAt(int index, @Shared("naCheck") @Cached() NACheck naCheck) {
        double value = data[index];
        naCheck.check(value);
        return value;
    }

    @ExportMessage
    public double getNextDouble(SeqIterator it, @Shared("naCheck") @Cached() NACheck naCheck) {
        double value = getStore(it)[it.getIndex()];
        naCheck.check(value);
        return value;
    }

    @ExportMessage
    public double getDouble(RandomAccessIterator it, int index, @Shared("naCheck") @Cached() NACheck naCheck) {
        double value = getStore(it)[index];
        naCheck.check(value);
        return value;
    }

    // Write access to the elements:

    @ExportMessage
    public SeqWriteIterator writeIterator(boolean inputIsComplete,
                    @Shared("naCheck") @Cached() NACheck naCheck,
                    @Shared("inputNACheck") @Cached() InputNACheck inputNACheck) {
        initInputNAChecks(inputNACheck, naCheck, inputIsComplete, isComplete());
        return new SeqWriteIterator(data, data.length, inputIsComplete);
    }

    @ExportMessage
    public RandomAccessWriteIterator randomAccessWriteIterator(boolean inputIsComplete,
                    @Shared("naCheck") @Cached() NACheck naCheck,
                    @Shared("inputNACheck") @Cached() InputNACheck inputNACheck) {
        initInputNAChecks(inputNACheck, naCheck, inputIsComplete, isComplete());
        return new RandomAccessWriteIterator(data, inputIsComplete);
    }

    @ExportMessage
    public void commitWriteIterator(SeqWriteIterator iterator, @Shared("inputNACheck") @Cached() InputNACheck na) {
        iterator.commit();
        commitWrites(na, iterator.inputIsComplete);
    }

    @ExportMessage
    public void commitRandomAccessWriteIterator(RandomAccessWriteIterator iterator, @Shared("inputNACheck") @Cached() InputNACheck na) {
        iterator.commit();
        commitWrites(na, iterator.inputIsComplete);
    }

    private void commitWrites(InputNACheck na, boolean inputIsComplete) {
        if (na.needsResettingCompleteFlag() && !inputIsComplete) {
            owner.setComplete(false);
        }
    }

    @ExportMessage
    public void setDoubleAt(int index, double value, InputNACheck inputNACheck) {
        inputNACheck.check(value);
        data[index] = value;
        if (inputNACheck.needsResettingCompleteFlag()) {
            owner.setComplete(false);
        }
    }

    @ExportMessage
    public void setNextDouble(SeqWriteIterator it, double value,
                    @Shared("inputNACheck") @Cached() InputNACheck inputNACheck) {
        inputNACheck.check(value);
        getStore(it)[it.getIndex()] = value;
    }

    @ExportMessage
    public void setDouble(RandomAccessWriteIterator it, int index, double value,
                    @Shared("inputNACheck") @Cached() InputNACheck inputNACheck) {
        inputNACheck.check(value);
        getStore(it)[index] = value;
    }

    private static double[] getStore(Iterator it) {
        return (double[]) it.getStore();
    }
}
