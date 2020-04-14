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
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.LoopConditionProfile;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.Iterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.RandomAccessIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.RandomAccessWriteIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.SeqIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.SeqWriteIterator;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

import java.util.Arrays;

import static com.oracle.truffle.r.runtime.data.model.RAbstractVector.ENABLE_COMPLETE;

@ExportLibrary(VectorDataLibrary.class)
class RStringArrayVectorData implements TruffleObject, VectorDataWithOwner {
    private final String[] data;
    private RStringVector owner;
    private boolean complete;

    RStringArrayVectorData(String[] data, boolean complete) {
        this.data = data;
        this.complete = complete && ENABLE_COMPLETE;
    }

    public CharSXPWrapper[] wrapStrings() {
        CharSXPWrapper[] result = new CharSXPWrapper[data.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = CharSXPWrapper.create(data[i]);
        }
        return result;
    }

    @Override
    public void setOwner(RAbstractVector newOwner) {
        owner = (RStringVector) newOwner;
        owner.setComplete(complete);
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public NACheck getNACheck(@Shared("naCheck") @Cached() NACheck na, @Shared("nullOwner") @Cached BranchProfile ownerIsNull) {
        na.enable(!isComplete(ownerIsNull));
        return na;
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public final RType getType() {
        return RType.Character;
    }

    @ExportMessage
    public int getLength() {
        return data.length;
    }

    @ExportMessage
    public RStringArrayVectorData materialize() {
        return this;
    }

    @ExportMessage
    public boolean isWriteable() {
        return true;
    }

    @ExportMessage
    public RStringArrayVectorData copy(@SuppressWarnings("unused") boolean deep,
                    @Shared("nullOwner") @Cached BranchProfile ownerIsNull) {
        return new RStringArrayVectorData(Arrays.copyOf(data, data.length), isComplete(ownerIsNull));
    }

    @ExportMessage
    public RStringArrayVectorData copyResized(@SuppressWarnings("unused") int newSize, @SuppressWarnings("unused") boolean deep, @SuppressWarnings("unused") boolean fillNA) {
        throw RInternalError.shouldNotReachHere("this method will be removed");
    }

    @ExportMessage
    public boolean isComplete(@Shared("nullOwner") @Cached BranchProfile ownerIsNull) {
        if (owner != null) {
            return owner.isComplete() && ENABLE_COMPLETE;
        }
        ownerIsNull.enter();
        return complete && ENABLE_COMPLETE;
    }

    @ExportMessage
    public String[] getReadonlyStringData() {
        return data;
    }

    @ExportMessage
    public String[] getStringDataCopy() {
        return Arrays.copyOf(data, data.length);
    }

    // Read access to the elements:

    @ExportMessage
    public SeqIterator iterator(
                    @Shared("naCheck") @Cached() NACheck naCheck,
                    @Shared("SeqItLoopProfile") @Cached("createCountingProfile()") LoopConditionProfile loopProfile,
                    @Shared("nullOwner") @Cached BranchProfile ownerIsNull) {
        SeqIterator it = new SeqIterator(data, data.length);
        naCheck.enable(!isComplete(ownerIsNull));
        it.initLoopConditionProfile(loopProfile);
        return it;
    }

    @ExportMessage
    public boolean next(SeqIterator it, boolean withWrap,
                    @Shared("SeqItLoopProfile") @Cached("createCountingProfile()") LoopConditionProfile loopProfile) {
        return it.next(loopProfile, withWrap);
    }

    @ExportMessage
    public RandomAccessIterator randomAccessIterator(@Shared("naCheck") @Cached() NACheck naCheck, @Shared("nullOwner") @Cached BranchProfile ownerIsNull) {
        naCheck.enable(!isComplete(ownerIsNull));
        return new RandomAccessIterator(data);
    }

    @ExportMessage
    public String getStringAt(int index,
                    @Shared("nullOwner") @Cached BranchProfile ownerIsNull,
                    @Shared("naCheck") @Cached() NACheck naCheck) {
        String value = data[index];
        naCheck.enable(!isComplete(ownerIsNull));
        naCheck.check(value);
        return value;
    }

    @ExportMessage
    public String getNextString(SeqIterator it, @Shared("naCheck") @Cached() NACheck naCheck) {
        String value = getStore(it)[it.getIndex()];
        naCheck.check(value);
        return value;
    }

    @ExportMessage
    public String getString(RandomAccessIterator it, int index, @Shared("naCheck") @Cached() NACheck naCheck) {
        String value = getStore(it)[index];
        naCheck.check(value);
        return value;
    }

    // Write access to the elements:

    @ExportMessage
    public SeqWriteIterator writeIterator() {
        return new SeqWriteIterator(data, data.length);
    }

    @ExportMessage
    public RandomAccessWriteIterator randomAccessWriteIterator() {
        return new RandomAccessWriteIterator(data);
    }

    @ExportMessage
    public void commitWriteIterator(SeqWriteIterator iterator, boolean neverSeenNA, @Shared("setCompleteProfile") @Cached BranchProfile setCompleteProfile) {
        iterator.commit();
        commitWrites(neverSeenNA, setCompleteProfile);
    }

    @ExportMessage
    public void commitRandomAccessWriteIterator(RandomAccessWriteIterator iterator, boolean neverSeenNA, @Shared("setCompleteProfile") @Cached BranchProfile setCompleteProfile) {
        iterator.commit();
        commitWrites(neverSeenNA, setCompleteProfile);
    }

    private void commitWrites(boolean neverSeenNA, @Cached BranchProfile setCompleteProfile) {
        if (!neverSeenNA) {
            setCompleteProfile.enter();
            if (owner != null) {
                owner.setComplete(false);
            }
            complete = false;
        }
    }

    @ExportMessage
    public void setStringAt(int index, String value, @Shared("setCompleteProfile") @Cached BranchProfile setCompleteProfile) {
        data[index] = value;
        if (RRuntime.isNA(value)) {
            setCompleteProfile.enter();
            if (owner != null) {
                owner.setComplete(false);
            }
            complete = false;
        }
    }

    @ExportMessage
    public void setNextString(SeqWriteIterator it, String value) {
        getStore(it)[it.getIndex()] = value;
        // complete flag will be updated in commit method
    }

    @ExportMessage
    public void setString(RandomAccessWriteIterator it, int index, String value) {
        getStore(it)[index] = value;
        // complete flag will be updated in commit method
    }

    // Utility methods:

    private static String[] getStore(Iterator it) {
        return (String[]) it.getStore();
    }
}
