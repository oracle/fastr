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
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.profiles.LoopConditionProfile;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.altrep.RAltRepData;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.ffi.AltrepRFFI;
import com.oracle.truffle.r.runtime.nodes.altrep.AltrepDuplicateNode;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

/**
 * Base class for all ALTREP numeric vectors - altinteger, altreal, altlogical, altcomplex and altraw.
 */
@ExportLibrary(VectorDataLibrary.class)
public class RAltrepNumericVectorData implements TruffleObject, VectorDataWithOwner {
    protected RAbstractContainer owner;
    protected final RAltRepData altrepData;
    private boolean dataptrCalled;

    protected RAltrepNumericVectorData(RAltRepData altrepData) {
        this.altrepData = altrepData;
    }

    public RAltRepData getAltrepData() {
        return altrepData;
    }

    public Object getData1() {
        return altrepData.getData1();
    }

    public Object getData2() {
        return altrepData.getData2();
    }

    public void setData1(Object data1) {
        altrepData.setData1(data1);
    }

    public void setData2(Object data2) {
        altrepData.setData2(data2);
    }

    @Override
    public void setOwner(RAbstractContainer owner) {
        this.owner = owner;
    }

    @ExportMessage
    public RType getType() {
        // TODO
        return RType.Null;
    }

    @ExportMessage
    public NACheck getNACheck() {
        return NACheck.getDisabled();
    }

    @ExportMessage
    public long asPointer(@Shared("dataptrNode") @Cached AltrepRFFI.DataptrNode dataptrNode) {
        return dataptrNode.execute(owner, true);
    }

    @ExportMessage
    public int getLength(@Shared("lengthNode") @Cached AltrepRFFI.LengthNode lengthNode) {
        return lengthNode.execute(owner);
    }

    @ExportMessage
    public RAltrepNumericVectorData materialize(@Shared("dataptrNode") @Cached AltrepRFFI.DataptrNode dataptrNode) {
        // Dataptr altrep method call forces materialization
        dataptrNode.execute(owner, true);
        return this;
    }

    @ExportMessage
    public boolean isWriteable() {
        // TODO: if (!dataptrCalled) return Dataptr_Or_Null != NULL
        return dataptrCalled;
    }

    public void setDataptrCalled() {
        dataptrCalled = true;
    }

    @ExportMessage
    public Object copy(boolean deep,
                       @Cached AltrepDuplicateNode duplicateNode) {
        return duplicateNode.execute(owner, deep);
    }

    @ExportMessage
    public VectorDataLibrary.SeqIterator iterator(
            @Shared("SeqItLoopProfile") @Cached("createCountingProfile()") LoopConditionProfile loopProfile,
            @Shared("lengthNode") @Cached AltrepRFFI.LengthNode lengthNode) {
        int length = lengthNode.execute(owner);
        VectorDataLibrary.SeqIterator it = new VectorDataLibrary.SeqIterator(this, length);
        it.initLoopConditionProfile(loopProfile);
        return it;
    }

    @ExportMessage
    public VectorDataLibrary.RandomAccessIterator randomAccessIterator() {
        return new VectorDataLibrary.RandomAccessIterator(this);
    }

    @ExportMessage
    public boolean nextImpl(VectorDataLibrary.SeqIterator it, boolean loopCondition,
                            @Shared("SeqItLoopProfile") @Cached("createCountingProfile()") LoopConditionProfile loopProfile) {
        return it.next(loopCondition, loopProfile);
    }

    @ExportMessage
    public void nextWithWrap(VectorDataLibrary.SeqIterator it,
                             @Shared("SeqItLoopProfile") @Cached("createCountingProfile()") LoopConditionProfile loopProfile) {
        it.nextWithWrap(loopProfile);
    }

    @ExportMessage
    public VectorDataLibrary.SeqWriteIterator writeIterator(@Shared("lengthNode") @Cached AltrepRFFI.LengthNode lengthNode) {
        int length = lengthNode.execute(owner);
        return new VectorDataLibrary.SeqWriteIterator(this, length);
    }

    @ExportMessage
    public VectorDataLibrary.RandomAccessWriteIterator randomAccessWriteIterator() {
        return new VectorDataLibrary.RandomAccessWriteIterator(this);
    }
}
