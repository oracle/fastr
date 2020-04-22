/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.dsl.Cached.Exclusive;
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
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.RandomAccessIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.SeqIterator;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

@ExportLibrary(VectorDataLibrary.class)
class RDoubleForeignObjData implements TruffleObject {
    protected final Object foreign;

    RDoubleForeignObjData(Object foreign) {
        this.foreign = foreign;
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public final RType getType() {
        return RType.Double;
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public NACheck getNACheck() {
        return NACheck.getEnabled();
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
    public RDoubleArrayVectorData materialize(@CachedLibrary(limit = "5") InteropLibrary valueInterop,
                    @CachedLibrary("this.foreign") InteropLibrary interop,
                    @Shared("resultProfile") @Cached("createClassProfile()") ValueProfile resultProfile,
                    @Shared("unprecisseProfile") @Cached("createBinaryProfile()") ConditionProfile unprecisseDoubleProfile) {
        return copy(false, valueInterop, interop, resultProfile, unprecisseDoubleProfile);
    }

    @ExportMessage
    public boolean isWriteable() {
        return false;
    }

    @ExportMessage
    public RDoubleArrayVectorData copy(@SuppressWarnings("unused") boolean deep,
                    @CachedLibrary(limit = "5") InteropLibrary valueInterop,
                    @CachedLibrary("this.foreign") InteropLibrary interop,
                    @Shared("resultProfile") @Cached("createClassProfile()") ValueProfile resultProfile,
                    @Shared("unprecisseProfile") @Cached("createBinaryProfile()") ConditionProfile unprecisseDoubleProfile) {
        return new RDoubleArrayVectorData(getDoubleDataCopy(valueInterop, interop, resultProfile, unprecisseDoubleProfile), RDataFactory.INCOMPLETE_VECTOR);
    }

    @ExportMessage
    public double[] getDoubleDataCopy(@CachedLibrary(limit = "5") InteropLibrary valueInterop,
                    @CachedLibrary("this.foreign") InteropLibrary interop,
                    @Shared("resultProfile") @Cached("createClassProfile()") ValueProfile resultProfile,
                    @Shared("unprecisseProfile") @Cached("createBinaryProfile()") ConditionProfile unprecisseDoubleProfile) {
        int len = getLength(interop);
        return getDataAsArray(len, len, valueInterop, interop, resultProfile, unprecisseDoubleProfile);
    }

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
    @SuppressWarnings("static-method")
    public boolean nextImpl(SeqIterator it, boolean loopCondition,
                    @Shared("SeqItLoopProfile") @Cached("createCountingProfile()") LoopConditionProfile loopProfile) {
        return it.next(loopCondition, loopProfile);
    }

    @ExportMessage
    @SuppressWarnings("static-method")
    public void nextWithWrap(SeqIterator it,
                    @Exclusive @Cached("createBinaryProfile()") ConditionProfile wrapProfile) {
        it.nextWithWrap(wrapProfile);
    }

    @ExportMessage
    public RandomAccessIterator randomAccessIterator(@Shared("naCheck") @Cached() NACheck naCheck) {
        naCheck.enable(true);
        return new RandomAccessIterator(foreign);
    }

    private static double getDoubleImpl(Object foreign, int index, NACheck naCheck, InteropLibrary valueInterop, InteropLibrary interop, ValueProfile resultProfile,
                    ConditionProfile unprecisseDoubleProfile) {
        try {
            Object result = interop.readArrayElement(foreign, index);
            double rd;
            try {
                rd = ((Number) resultProfile.profile(valueInterop.asDouble(result))).doubleValue();
                naCheck.check(rd);
            } catch (UnsupportedMessageException e) {
                if (valueInterop.isNull(result)) {
                    naCheck.seenNA();
                    return RRuntime.DOUBLE_NA;
                }
                if (unprecisseDoubleProfile.profile(valueInterop.fitsInLong(result))) {
                    Long l = valueInterop.asLong(result);
                    double d = l.doubleValue();
                    RError.warning(RError.SHOW_CALLER, RError.Message.PRECISSION_LOSS_BY_CONVERSION, l, d);
                    rd = ((Long) valueInterop.asLong(result)).doubleValue();
                    naCheck.check(rd);
                    return rd;
                }
                throw RInternalError.shouldNotReachHere();
            }
            return rd;
        } catch (UnsupportedMessageException | InvalidArrayIndexException | ClassCastException e) {
            throw RInternalError.shouldNotReachHere(e);
        }
    }

    @ExportMessage
    public double getDoubleAt(int index,
                    @CachedLibrary(limit = "5") InteropLibrary valueInterop,
                    @CachedLibrary("this.foreign") InteropLibrary interop,
                    @Shared("naCheck") @Cached() NACheck naCheck,
                    @Shared("resultProfile") @Cached("createClassProfile()") ValueProfile resultProfile,
                    @Shared("unprecisseProfile") @Cached("createBinaryProfile()") ConditionProfile unprecisseDoubleProfile) {
        return getDoubleImpl(foreign, index, naCheck, valueInterop, interop, resultProfile, unprecisseDoubleProfile);
    }

    @ExportMessage
    public double getNextDouble(SeqIterator it,
                    @CachedLibrary(limit = "5") InteropLibrary valueInterop,
                    @CachedLibrary("this.foreign") InteropLibrary interop,
                    @Shared("naCheck") @Cached() NACheck naCheck,
                    @Shared("resultProfile") @Cached("createClassProfile()") ValueProfile resultProfile,
                    @Shared("unprecisseProfile") @Cached("createBinaryProfile()") ConditionProfile unprecisseDoubleProfile) {
        return getDoubleImpl(it.getStore(), it.getIndex(), naCheck, valueInterop, interop, resultProfile, unprecisseDoubleProfile);
    }

    @ExportMessage
    public double getDouble(RandomAccessIterator it, int index,
                    @CachedLibrary(limit = "5") InteropLibrary valueInterop,
                    @CachedLibrary("this.foreign") InteropLibrary interop,
                    @Shared("naCheck") @Cached() NACheck naCheck,
                    @Shared("resultProfile") @Cached("createClassProfile()") ValueProfile resultProfile,
                    @Shared("unprecisseProfile") @Cached("createBinaryProfile()") ConditionProfile unprecisseDoubleProfile) {
        return getDoubleImpl(it.getStore(), index, naCheck, valueInterop, interop, resultProfile, unprecisseDoubleProfile);
    }

    private double[] getDataAsArray(int newLength, int length, InteropLibrary valueInterop, InteropLibrary interop, ValueProfile resultProfile, ConditionProfile unprecisseDoubleProfile) {
        double[] data = new double[newLength];
        for (int i = 0; i < Math.min(newLength, length); i++) {
            data[i] = getDoubleAt(i, valueInterop, interop, NACheck.getDisabled(), resultProfile, unprecisseDoubleProfile);
        }
        return data;
    }
}
