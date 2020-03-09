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

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.library.ExportMessage.Ignore;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.VectorDataLibraryUtils.RandomAccessIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibraryUtils.SeqIterator;

import java.util.Arrays;

@ExportLibrary(RDoubleVectorDataLibrary.class)
@ExportLibrary(VectorDataLibrary.class)
class RDoubleForeignObjData extends RDoubleVectorData {
    protected final Object foreign;

    RDoubleForeignObjData(Object foreign) {
        this.foreign = foreign;
    }

    @Override
    @Ignore
    public double getDoubleAt(int index) {
        return getDoubleImpl(index, InteropLibrary.getFactory().getUncached(), InteropLibrary.getFactory().getUncached(), ValueProfile.getUncached(), ConditionProfile.getUncached());
    }

    @Override
    @Ignore
    public int getLength() {
        return getLength(InteropLibrary.getFactory().getUncached());
    }

    @ExportMessage(library = RDoubleVectorDataLibrary.class)
    @ExportMessage(library = VectorDataLibrary.class)
    public int getLength(@CachedLibrary("this.foreign") InteropLibrary interop) {
        try {
            long result = interop.getArraySize(foreign);
            return (int) result;
        } catch (UnsupportedMessageException e) {
            throw RInternalError.shouldNotReachHere();
        }
    }

    @ExportMessage(library = RDoubleVectorDataLibrary.class)
    public RDoubleArrayVectorData materialize(@CachedLibrary(limit = "5") InteropLibrary valueInterop,
                    @CachedLibrary("this.foreign") InteropLibrary interop,
                    @Shared("resultProfile") @Cached("createClassProfile()") ValueProfile resultProfile,
                    @Shared("unprecisseProfile") @Cached("createBinaryProfile()") ConditionProfile unprecisseDoubleProfile) {
        return copy(false, valueInterop, interop, resultProfile, unprecisseDoubleProfile);
    }

    @ExportMessage(library = RDoubleVectorDataLibrary.class)
    public boolean isWriteable() {
        return false;
    }

    @ExportMessage
    public RDoubleArrayVectorData copy(@SuppressWarnings("unused") boolean deep,
                    @CachedLibrary(limit = "5") InteropLibrary valueInterop,
                    @CachedLibrary("this.foreign") InteropLibrary interop,
                    @Shared("resultProfile") @Cached("createClassProfile()") ValueProfile resultProfile,
                    @Shared("unprecisseProfile") @Cached("createBinaryProfile()") ConditionProfile unprecisseDoubleProfile) {
        return new RDoubleArrayVectorData(getReadonlyDoubleData(valueInterop, interop, resultProfile, unprecisseDoubleProfile), RDataFactory.INCOMPLETE_VECTOR);
    }

    @ExportMessage
    public RDoubleArrayVectorData copyResized(int newSize, @SuppressWarnings("unused") boolean deep, boolean fillNA,
                    @CachedLibrary(limit = "5") InteropLibrary valueInterop,
                    @CachedLibrary("this.foreign") InteropLibrary interop,
                    @Shared("resultProfile") @Cached("createClassProfile()") ValueProfile resultProfile,
                    @Shared("unprecisseProfile") @Cached("createBinaryProfile()") ConditionProfile unprecisseDoubleProfile) {
        int length = getLength(interop);
        double[] newData = getDataAsArray(newSize, length, interop, valueInterop, resultProfile, unprecisseDoubleProfile);
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

    @ExportMessage
    public double[] getReadonlyDoubleData(@CachedLibrary(limit = "5") InteropLibrary valueInterop,
                    @CachedLibrary("this.foreign") InteropLibrary interop,
                    @Shared("resultProfile") @Cached("createClassProfile()") ValueProfile resultProfile,
                    @Shared("unprecisseProfile") @Cached("createBinaryProfile()") ConditionProfile unprecisseDoubleProfile) {
        int len = getLength(interop);
        return getDataAsArray(len, len, valueInterop, interop, resultProfile, unprecisseDoubleProfile);
    }

    @ExportMessage
    public SeqIterator iterator(@CachedLibrary("this.foreign") InteropLibrary interop) {
        return new SeqIterator(foreign, getLength(interop));
    }

    @ExportMessage
    public RandomAccessIterator randomAccessIterator(@CachedLibrary("this.foreign") InteropLibrary interop) {
        return new RandomAccessIterator(foreign, getLength(interop));
    }

    private double getDoubleImpl(int index, InteropLibrary valueInterop, InteropLibrary interop, ValueProfile resultProfile, ConditionProfile unprecisseDoubleProfile) {
        try {
            Object result = interop.readArrayElement(foreign, index);
            try {
                return ((Number) resultProfile.profile(valueInterop.asDouble(result))).doubleValue();
            } catch (UnsupportedMessageException e) {
                if (interop.isNull(result)) {
                    return RRuntime.DOUBLE_NA;
                }
                if (unprecisseDoubleProfile.profile(valueInterop.fitsInLong(result))) {
                    Long l = valueInterop.asLong(result);
                    double d = l.doubleValue();
                    RError.warning(RError.SHOW_CALLER, RError.Message.PRECISSION_LOSS_BY_CONVERSION, l, d);
                    return ((Long) valueInterop.asLong(result)).doubleValue();
                }
                throw RInternalError.shouldNotReachHere();
            }
        } catch (UnsupportedMessageException | InvalidArrayIndexException | ClassCastException e) {
            throw RInternalError.shouldNotReachHere(e);
        }
    }

    @ExportMessage
    public double getDoubleAt(int index,
                    @CachedLibrary(limit = "5") InteropLibrary valueInterop,
                    @CachedLibrary("this.foreign") InteropLibrary interop,
                    @Shared("resultProfile") @Cached("createClassProfile()") ValueProfile resultProfile,
                    @Shared("unprecisseProfile") @Cached("createBinaryProfile()") ConditionProfile unprecisseDoubleProfile) {
        return getDoubleImpl(index, valueInterop, interop, resultProfile, unprecisseDoubleProfile);
    }

    @ExportMessage
    public double getNext(SeqIterator it,
                    @CachedLibrary(limit = "5") InteropLibrary valueInterop,
                    @CachedLibrary("this.foreign") InteropLibrary interop,
                    @Shared("resultProfile") @Cached("createClassProfile()") ValueProfile resultProfile,
                    @Shared("unprecisseProfile") @Cached("createBinaryProfile()") ConditionProfile unprecisseDoubleProfile) {
        return getDoubleImpl(it.getIndex(), valueInterop, interop, resultProfile, unprecisseDoubleProfile);
    }

    @ExportMessage
    public double getAt(@SuppressWarnings("unused") RandomAccessIterator it, int index,
                    @CachedLibrary(limit = "5") InteropLibrary valueInterop,
                    @CachedLibrary("this.foreign") InteropLibrary interop,
                    @Shared("resultProfile") @Cached("createClassProfile()") ValueProfile resultProfile,
                    @Shared("unprecisseProfile") @Cached("createBinaryProfile()") ConditionProfile unprecisseDoubleProfile) {
        return getDoubleImpl(index, valueInterop, interop, resultProfile, unprecisseDoubleProfile);
    }

    private double[] getDataAsArray(int newLength, int length, InteropLibrary valueInterop, InteropLibrary interop, ValueProfile resultProfile, ConditionProfile unprecisseDoubleProfile) {
        double[] data = new double[newLength];
        for (int i = 0; i < Math.min(newLength, length); i++) {
            data[i] = getDoubleAt(i, valueInterop, interop, resultProfile, unprecisseDoubleProfile);
        }
        return data;
    }
}
