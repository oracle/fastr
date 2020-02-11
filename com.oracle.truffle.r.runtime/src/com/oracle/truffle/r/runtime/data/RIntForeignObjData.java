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
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.library.ExportMessage.Ignore;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.VectorDataLibraryUtils.RandomAccessIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibraryUtils.SeqIterator;

import java.util.Arrays;

@ExportLibrary(RIntVectorDataLibrary.class)
class RIntForeignObjData extends RIntVectorData {
    protected final Object foreign;

    RIntForeignObjData(Object foreign) {
        this.foreign = foreign;
    }

    @Override
    @Ignore
    public int getIntAt(int index) {
        return getIntImpl(index, InteropLibrary.getFactory().getUncached(), InteropLibrary.getFactory().getUncached(), ValueProfile.getUncached(), ConditionProfile.getUncached(),
                        ConditionProfile.getUncached());
    }

    @Override
    @Ignore
    public int getLength() {
        return getLength(InteropLibrary.getFactory().getUncached());
    }

    @ExportMessage
    public int getLength(@CachedLibrary("this.foreign") InteropLibrary interop) {
        try {
            long result = interop.getArraySize(foreign);
            return (int) result;
        } catch (UnsupportedMessageException e) {
            throw RInternalError.shouldNotReachHere();
        }
    }

    @ExportMessage
    public RIntArrayVectorData materialize(@CachedLibrary(limit = "5") InteropLibrary valueInterop,
                    @CachedLibrary("this.foreign") InteropLibrary interop,
                    @Shared("resultProfile") @Cached("createClassProfile()") ValueProfile resultProfile,
                    @Shared("isTOProfile") @Cached("createBinaryProfile()") ConditionProfile isTruffleObjectProfile,
                    @Shared("isIntProfile") @Cached("createBinaryProfile()") ConditionProfile isIntProfile) {
        return copy(false, valueInterop, interop, resultProfile, isTruffleObjectProfile, isIntProfile);
    }

    @ExportMessage
    public boolean isWriteable() {
        return false;
    }

    @ExportMessage
    public RIntArrayVectorData copy(@SuppressWarnings("unused") boolean deep,
                    @CachedLibrary(limit = "5") InteropLibrary valueInterop,
                    @CachedLibrary("this.foreign") InteropLibrary interop,
                    @Shared("resultProfile") @Cached("createClassProfile()") ValueProfile resultProfile,
                    @Shared("isTOProfile") @Cached("createBinaryProfile()") ConditionProfile isTruffleObjectProfile,
                    @Shared("isIntProfile") @Cached("createBinaryProfile()") ConditionProfile isIntProfile) {
        return new RIntArrayVectorData(getReadonlyIntData(valueInterop, interop, resultProfile, isTruffleObjectProfile, isIntProfile), RDataFactory.INCOMPLETE_VECTOR);
    }

    @ExportMessage
    public RIntArrayVectorData copyResized(int newSize, @SuppressWarnings("unused") boolean deep, boolean fillNA,
                    @CachedLibrary(limit = "5") InteropLibrary valueInterop,
                    @CachedLibrary("this.foreign") InteropLibrary interop,
                    @Shared("resultProfile") @Cached("createClassProfile()") ValueProfile resultProfile,
                    @Shared("isTOProfile") @Cached("createBinaryProfile()") ConditionProfile isTruffleObjectProfile,
                    @Shared("isIntProfile") @Cached("createBinaryProfile()") ConditionProfile isIntProfile) {
        int length = getLength(interop);
        int[] newData = getDataAsArray(newSize, length, interop, valueInterop, resultProfile, isTruffleObjectProfile, isIntProfile);
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
    public int[] getReadonlyIntData(@CachedLibrary(limit = "5") InteropLibrary valueInterop,
                    @CachedLibrary("this.foreign") InteropLibrary interop,
                    @Shared("resultProfile") @Cached("createClassProfile()") ValueProfile resultProfile,
                    @Shared("isTOProfile") @Cached("createBinaryProfile()") ConditionProfile isTruffleObjectProfile,
                    @Shared("isIntProfile") @Cached("createBinaryProfile()") ConditionProfile isIntProfile) {
        int len = getLength(interop);
        return getDataAsArray(len, len, valueInterop, interop, resultProfile, isTruffleObjectProfile, isIntProfile);
    }

    @ExportMessage
    public SeqIterator iterator(@CachedLibrary("this.foreign") InteropLibrary interop) {
        return new SeqIterator(foreign, getLength(interop));
    }

    @ExportMessage
    public RandomAccessIterator randomAccessIterator(@CachedLibrary("this.foreign") InteropLibrary interop) {
        return new RandomAccessIterator(foreign, getLength(interop));
    }

    private int getIntImpl(int index, InteropLibrary valueInterop, InteropLibrary interop, ValueProfile resultProfile, ConditionProfile isTruffleObjectProfile, ConditionProfile isIntProfile) {
        try {
            Object result = interop.readArrayElement(foreign, index);
            if (isTruffleObjectProfile.profile(result instanceof TruffleObject)) {
                if (valueInterop.isNumber(result)) {
                    result = valueInterop.asInt(result);
                } else if (valueInterop.isNull(result)) {
                    return RRuntime.INT_NA;
                } else {
                    result = valueInterop.asString(result).charAt(0);
                }
            }

            if (isIntProfile.profile(result instanceof Number)) {
                return ((Number) resultProfile.profile(result)).intValue();
            } else {
                return (Character) resultProfile.profile(result);
            }
        } catch (UnsupportedMessageException | InvalidArrayIndexException e) {
            throw RInternalError.shouldNotReachHere(e);
        }
    }

    @ExportMessage
    public int getIntAt(int index,
                    @CachedLibrary(limit = "5") InteropLibrary valueInterop,
                    @CachedLibrary("this.foreign") InteropLibrary interop,
                    @Shared("resultProfile") @Cached("createClassProfile()") ValueProfile resultProfile,
                    @Shared("isTOProfile") @Cached("createBinaryProfile()") ConditionProfile isTruffleObjectProfile,
                    @Shared("isIntProfile") @Cached("createBinaryProfile()") ConditionProfile isIntProfile) {
        return getIntImpl(index, valueInterop, interop, resultProfile, isTruffleObjectProfile, isIntProfile);
    }

    @ExportMessage
    public int getNext(SeqIterator it,
                    @CachedLibrary(limit = "5") InteropLibrary valueInterop,
                    @CachedLibrary("this.foreign") InteropLibrary interop,
                    @Shared("resultProfile") @Cached("createClassProfile()") ValueProfile resultProfile,
                    @Shared("isTOProfile") @Cached("createBinaryProfile()") ConditionProfile isTruffleObjectProfile,
                    @Shared("isIntProfile") @Cached("createBinaryProfile()") ConditionProfile isIntProfile) {
        return getIntImpl(it.getIndex(), valueInterop, interop, resultProfile, isTruffleObjectProfile, isIntProfile);
    }

    @ExportMessage
    public int getAt(@SuppressWarnings("unused") RandomAccessIterator it, int index,
                    @CachedLibrary(limit = "5") InteropLibrary valueInterop,
                    @CachedLibrary("this.foreign") InteropLibrary interop,
                    @Shared("resultProfile") @Cached("createClassProfile()") ValueProfile resultProfile,
                    @Shared("isTOProfile") @Cached("createBinaryProfile()") ConditionProfile isTruffleObjectProfile,
                    @Shared("isIntProfile") @Cached("createBinaryProfile()") ConditionProfile isIntProfile) {
        return getIntImpl(index, valueInterop, interop, resultProfile, isTruffleObjectProfile, isIntProfile);
    }

    private int[] getDataAsArray(int newLength, int length, InteropLibrary valueInterop, InteropLibrary interop, ValueProfile resultProfile, ConditionProfile isTruffleObjectProfile,
                    ConditionProfile isIntProfile) {
        int[] data = new int[newLength];
        for (int i = 0; i < Math.min(newLength, length); i++) {
            data[i] = getIntAt(i, valueInterop, interop, resultProfile, isTruffleObjectProfile, isIntProfile);
        }
        return data;
    }
}
