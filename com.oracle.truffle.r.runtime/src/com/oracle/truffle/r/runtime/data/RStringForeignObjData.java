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
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.LoopConditionProfile;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.RandomAccessIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.SeqIterator;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

import java.util.Arrays;

@ExportLibrary(VectorDataLibrary.class)
class RStringForeignObjData implements TruffleObject {
    protected final Object foreign;

    RStringForeignObjData(Object foreign) {
        this.foreign = foreign;
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public NACheck getNACheck() {
        return NACheck.getEnabled();
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public final RType getType() {
        return RType.Character;
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
    public RStringArrayVectorData materialize(@CachedLibrary(limit = "5") InteropLibrary valueInterop,
                    @CachedLibrary("this.foreign") InteropLibrary interop,
                    @Shared("isNullCheck") @Cached("createBinaryProfile()") ConditionProfile isNullProfile) {
        return copy(false, valueInterop, interop, isNullProfile);
    }

    @ExportMessage
    public boolean isWriteable() {
        return false;
    }

    @ExportMessage
    public RStringArrayVectorData copy(@SuppressWarnings("unused") boolean deep,
                    @CachedLibrary(limit = "5") InteropLibrary valueInterop,
                    @CachedLibrary("this.foreign") InteropLibrary interop,
                    @Shared("isNullCheck") @Cached("createBinaryProfile()") ConditionProfile isNullProfile) {
        return new RStringArrayVectorData(getStringDataCopy(valueInterop, interop, isNullProfile), RDataFactory.INCOMPLETE_VECTOR);
    }

    @ExportMessage
    public RStringArrayVectorData copyResized(int newSize, @SuppressWarnings("unused") boolean deep, boolean fillNA,
                    @CachedLibrary(limit = "5") InteropLibrary valueInterop,
                    @Shared("isNullCheck") @Cached("createBinaryProfile()") ConditionProfile isNullProfile,
                    @CachedLibrary("this.foreign") InteropLibrary interop) {
        int length = getLength(interop);
        String[] newData = getDataAsArray(newSize, length, interop, valueInterop, isNullProfile);
        if (fillNA) {
            Arrays.fill(newData, length, newData.length, RRuntime.INT_NA);
        }
        return new RStringArrayVectorData(newData, RDataFactory.INCOMPLETE_VECTOR);
    }

    @ExportMessage
    public String[] getStringDataCopy(@CachedLibrary(limit = "5") InteropLibrary valueInterop,
                    @CachedLibrary("this.foreign") InteropLibrary interop,
                    @Shared("isNullCheck") @Cached("createBinaryProfile()") ConditionProfile isNullProfile) {
        int len = getLength(interop);
        return getDataAsArray(len, len, valueInterop, interop, isNullProfile);
    }

    // Read access to the elements:

    @ExportMessage
    public SeqIterator iterator(@Shared("naCheck") @Cached() NACheck naCheck,
                    @Shared("SeqItLoopProfile") @Cached("createCountingProfile()") LoopConditionProfile loopProfile,
                    @CachedLibrary("this.foreign") InteropLibrary interop) {
        SeqIterator it = new SeqIterator(foreign, getLength(interop));
        naCheck.enable(true);
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
        naCheck.enable(true);
        return new RandomAccessIterator(foreign);
    }

    private static String getStringImpl(Object foreign, int index, NACheck naCheck, InteropLibrary valueInterop, InteropLibrary interop, ConditionProfile isNullProfile) {
        try {
            Object elem = interop.readArrayElement(foreign, index);
            String result;
            try {
                if (isNullProfile.profile(valueInterop.isNull(elem))) {
                    result = RRuntime.STRING_NA;
                } else {
                    result = valueInterop.asString(elem);
                }
            } catch (UnsupportedMessageException ex) {
                throw RInternalError.shouldNotReachHere(ex);
            }
            naCheck.check(result);
            return result;
        } catch (UnsupportedMessageException | InvalidArrayIndexException e) {
            throw RInternalError.shouldNotReachHere(e);
        }
    }

    @ExportMessage
    public String getStringAt(int index,
                    @CachedLibrary(limit = "5") InteropLibrary valueInterop,
                    @CachedLibrary("this.foreign") InteropLibrary interop,
                    @Shared("isNullCheck") @Cached("createBinaryProfile()") ConditionProfile isNullProfile,
                    @Shared("naCheck") @Cached() NACheck naCheck) {
        return getStringImpl(foreign, index, naCheck, valueInterop, interop, isNullProfile);
    }

    @ExportMessage
    public String getNextString(SeqIterator it,
                    @CachedLibrary(limit = "5") InteropLibrary valueInterop,
                    @CachedLibrary("this.foreign") InteropLibrary interop,
                    @Shared("isNullCheck") @Cached("createBinaryProfile()") ConditionProfile isNullProfile,
                    @Shared("naCheck") @Cached() NACheck naCheck) {
        return getStringImpl(it.getStore(), it.getIndex(), naCheck, valueInterop, interop, isNullProfile);
    }

    @ExportMessage
    public String getString(RandomAccessIterator it, int index,
                    @CachedLibrary(limit = "5") InteropLibrary valueInterop,
                    @CachedLibrary("this.foreign") InteropLibrary interop,
                    @Shared("isNullCheck") @Cached("createBinaryProfile()") ConditionProfile isNullProfile,
                    @Shared("naCheck") @Cached() NACheck naCheck) {
        return getStringImpl(it.getStore(), index, naCheck, valueInterop, interop, isNullProfile);
    }

    // Utility methods:

    private String[] getDataAsArray(int newLength, int length, InteropLibrary valueInterop, InteropLibrary interop, ConditionProfile isNullProfile) {
        String[] data = new String[newLength];
        for (int i = 0; i < Math.min(newLength, length); i++) {
            data[i] = getStringAt(i, valueInterop, interop, isNullProfile, NACheck.getDisabled());
        }
        return data;
    }
}
