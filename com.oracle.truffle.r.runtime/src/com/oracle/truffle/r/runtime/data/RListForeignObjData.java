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
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.RandomAccessIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.SeqIterator;
import com.oracle.truffle.r.runtime.interop.Foreign2R;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

@ExportLibrary(VectorDataLibrary.class)
class RListForeignObjData implements TruffleObject {
    protected final Object foreign;

    RListForeignObjData(Object foreign) {
        this.foreign = foreign;
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public final RType getType() {
        return RType.List;
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
    public Object[] materialize(@CachedLibrary("this.foreign") InteropLibrary interop,
                    @Shared("foreign2R") @Cached Foreign2R foreign2R) {
        return getListDataCopy(interop, foreign2R);
    }

    @ExportMessage
    public boolean isWriteable() {
        return false;
    }

    @ExportMessage
    public RListForeignObjData copy(@SuppressWarnings("unused") boolean deep) {
        return new RListForeignObjData(foreign);
    }

    @ExportMessage
    public Object[] getListDataCopy(@CachedLibrary("this.foreign") InteropLibrary interop,
                    @Shared("foreign2R") @Cached Foreign2R foreign2R) {
        int length = getLength(interop);
        Object[] result = new Object[length];
        Object foreignObj = foreign;
        for (int i = 0; i < length; i++) {
            result[i] = getElementAtImpl(foreignObj, i, interop, foreign2R);
        }
        return result;
    }

    @ExportMessage
    public SeqIterator iterator(@Shared("SeqItLoopProfile") @Cached("createCountingProfile()") LoopConditionProfile loopProfile,
                    @CachedLibrary("this.foreign") InteropLibrary interop) {
        SeqIterator it = new SeqIterator(foreign, getLength(interop));
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
    public RandomAccessIterator randomAccessIterator() {
        return new RandomAccessIterator(foreign);
    }

    @ExportMessage
    public Object getElementAt(int index,
                    @CachedLibrary("this.foreign") InteropLibrary interop,
                    @Shared("foreign2R") @Cached Foreign2R foreign2R) {
        return getElementAtImpl(foreign, index, interop, foreign2R);
    }

    @ExportMessage
    public Object getNextElement(SeqIterator it,
                    @CachedLibrary("this.foreign") InteropLibrary interop,
                    @Shared("foreign2R") @Cached Foreign2R foreign2R) {
        return getElementAtImpl(it.getStore(), it.getIndex(), interop, foreign2R);
    }

    @ExportMessage
    public Object getElement(@SuppressWarnings("unused") RandomAccessIterator it, int index,
                    @CachedLibrary("this.foreign") InteropLibrary interop,
                    @Shared("foreign2R") @Cached Foreign2R foreign2R) {
        return getElementAtImpl(it.getStore(), index, interop, foreign2R);
    }

    private static Object getElementAtImpl(Object foreign, int index, InteropLibrary interop, Foreign2R foreign2R) {
        try {
            return foreign2R.convert(interop.readArrayElement(foreign, index));
        } catch (UnsupportedMessageException | InvalidArrayIndexException e) {
            throw RInternalError.shouldNotReachHere(e);
        }
    }
}
