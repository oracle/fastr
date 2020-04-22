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

@ExportLibrary(VectorDataLibrary.class)
class RLogicalForeignObjData implements TruffleObject {
    protected final Object foreign;

    RLogicalForeignObjData(Object foreign) {
        this.foreign = foreign;
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public final RType getType() {
        return RType.Logical;
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
    public RLogicalArrayVectorData materialize(@CachedLibrary(limit = "5") InteropLibrary valueInterop,
                    @CachedLibrary("this.foreign") InteropLibrary interop) {
        return copy(false, valueInterop, interop);
    }

    @ExportMessage
    public boolean isWriteable() {
        return false;
    }

    @ExportMessage
    public RLogicalArrayVectorData copy(@SuppressWarnings("unused") boolean deep,
                    @CachedLibrary(limit = "5") InteropLibrary valueInterop,
                    @CachedLibrary("this.foreign") InteropLibrary interop) {
        return new RLogicalArrayVectorData(getLogicalDataCopy(valueInterop, interop), RDataFactory.INCOMPLETE_VECTOR);
    }

    @ExportMessage
    public byte[] getLogicalDataCopy(@CachedLibrary(limit = "5") InteropLibrary valueInterop,
                    @CachedLibrary("this.foreign") InteropLibrary interop) {
        int len = getLength(interop);
        return getDataAsArray(len, len, valueInterop, interop);
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
                    @Cached("createBinaryProfile()") ConditionProfile wrapProfile) {
        it.nextWithWrap(wrapProfile);
    }

    @ExportMessage
    public RandomAccessIterator randomAccessIterator(@Shared("naCheck") @Cached() NACheck naCheck) {
        naCheck.enable(true);
        return new RandomAccessIterator(foreign);
    }

    private static byte getLogicalImpl(Object foreign, int index, InteropLibrary valueInterop, InteropLibrary interop) {
        try {
            Object value = interop.readArrayElement(foreign, index);
            try {
                return RRuntime.asLogical(valueInterop.asBoolean(value));
            } catch (UnsupportedMessageException ume) {
                if (valueInterop.isNull(value)) {
                    return RRuntime.LOGICAL_NA;
                }
                throw RInternalError.shouldNotReachHere(ume);
            }
        } catch (UnsupportedMessageException | InvalidArrayIndexException | ClassCastException e) {
            throw RInternalError.shouldNotReachHere(e);
        }
    }

    @ExportMessage
    public byte getLogicalAt(int index,
                    @CachedLibrary(limit = "5") InteropLibrary valueInterop,
                    @CachedLibrary("this.foreign") InteropLibrary interop) {
        return getLogicalImpl(foreign, index, valueInterop, interop);
    }

    @ExportMessage
    public byte getNextLogical(SeqIterator it,
                    @CachedLibrary(limit = "5") InteropLibrary valueInterop,
                    @CachedLibrary("this.foreign") InteropLibrary interop) {
        return getLogicalImpl(it.getStore(), it.getIndex(), valueInterop, interop);
    }

    @ExportMessage
    public byte getLogical(RandomAccessIterator it, int index,
                    @CachedLibrary(limit = "5") InteropLibrary valueInterop,
                    @CachedLibrary("this.foreign") InteropLibrary interop) {
        return getLogicalImpl(it.getStore(), index, valueInterop, interop);
    }

    private byte[] getDataAsArray(int newLength, int length, InteropLibrary valueInterop, InteropLibrary interop) {
        byte[] data = new byte[newLength];
        for (int i = 0; i < Math.min(newLength, length); i++) {
            data[i] = getLogicalAt(i, valueInterop, interop);
        }
        return data;
    }
}
