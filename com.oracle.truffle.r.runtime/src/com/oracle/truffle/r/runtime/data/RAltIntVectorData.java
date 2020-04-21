/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.profiles.LoopConditionProfile;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.RandomAccessIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.RandomAccessWriteIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.SeqIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.SeqWriteIterator;
import com.oracle.truffle.r.runtime.data.altrep.AltIntegerClassDescriptor;
import com.oracle.truffle.r.runtime.data.altrep.RAltRepData;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.ffi.AltrepRFFI;
import com.oracle.truffle.r.runtime.ffi.util.NativeMemory;
import com.oracle.truffle.r.runtime.ffi.util.NativeMemory.ElementType;
import com.oracle.truffle.r.runtime.nodes.altrep.AltrepDuplicateNode;
import com.oracle.truffle.r.runtime.nodes.altrep.AltrepGetIntAtNode;
import com.oracle.truffle.r.runtime.nodes.altrep.AltrepLengthNode;
import com.oracle.truffle.r.runtime.nodes.altrep.AltrepSetElementAtNode;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

@ExportLibrary(VectorDataLibrary.class)
public class RAltIntVectorData implements TruffleObject, VectorDataWithOwner {
    protected final RAltRepData altrepData;
    protected final AltIntegerClassDescriptor descriptor;
    private boolean dataptrCalled;
    private RIntVector owner;

    public RAltIntVectorData(AltIntegerClassDescriptor descriptor, RAltRepData data) {
        this.altrepData = data;
        this.descriptor = descriptor;
        this.dataptrCalled = false;
        assert hasDescriptorRegisteredNecessaryMethods(descriptor):
                "Descriptor " + descriptor.toString() + " does not have registered all necessary methods";
    }

    private boolean hasDescriptorRegisteredNecessaryMethods(AltIntegerClassDescriptor descriptor) {
        return descriptor.isLengthMethodRegistered() && descriptor.isDataptrMethodRegistered();
        /* TODO: && descriptor.isUnserializeMethodRegistered(); */
    }

    public AltIntegerClassDescriptor getDescriptor() {
        return descriptor;
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
    public void setOwner(RAbstractVector newOwner) {
        owner = (RIntVector) newOwner;
    }

    private RIntVector getOwner() {
        assert owner != null;
        return owner;
    }

    @ExportMessage
    public NACheck getNACheck(@Cached() NACheck na) {
        na.enable(false);
        return na;
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public final RType getType() {
        return RType.Integer;
    }

    @ExportMessage(limit = "1")
    public int getLength(@Cached("create()") AltrepLengthNode lengthNode) {
        return (int) lengthNode.execute(getOwner());
    }

    @ExportMessage(limit = "1")
    public RIntArrayVectorData materialize(@Cached("create()") AltrepRFFI.AltIntDataptrNode dataptrNode,
                                           @Cached AltrepLengthNode lengthNode) {
        int length = (int) lengthNode.execute(getOwner());
        long dataptrAddr = dataptrNode.execute(getOwner(), true);
        int[] newData = new int[length];
        NativeMemory.copyMemory(dataptrAddr, newData, ElementType.INT, length);
        // TODO: complete=true?
        return new RIntArrayVectorData(newData, true);
    }

    @ExportMessage
    public boolean isWriteable() {
        return dataptrCalled;
    }

    @ExportMessage
    public boolean isComplete() {
        return true;
    }

    /**
     * May return either RAltIntVectorData or RIntArrayVectorData, depending whether this vector data has
     * Duplicate method registered.
     */
    @ExportMessage
    public Object copy(boolean deep,
                       @Cached AltrepDuplicateNode duplicateNode) {
        return duplicateNode.execute(getOwner(), deep);
    }

    @ExportMessage
    public RIntArrayVectorData copyResized(int newSize, boolean deep, boolean fillNA) {
        throw RInternalError.unimplemented("RAltIntVectorData.copyResized");
    }

    @ExportMessage
    public int[] getIntDataCopy(@Cached AltrepRFFI.AltIntDataptrNode dataptrNode,
                                @Cached AltrepLengthNode lengthNode) {
        return getDataCopy(dataptrNode, lengthNode);
    }

    @ExportMessage
    public int[] getReadonlyIntData(@Cached AltrepRFFI.AltIntDataptrNode dataptrNode,
                                    @Cached AltrepLengthNode lengthNode) {
        return getDataCopy(dataptrNode, lengthNode);
    }

    private int[] getDataCopy(AltrepRFFI.AltIntDataptrNode dataptrNode, AltrepLengthNode lengthNode) {
        long dataptrAddr = dataptrNode.execute(getOwner(), false);
        int length = (int) lengthNode.execute(getOwner());
        int[] dataCopy = new int[length];
        NativeMemory.copyMemory(dataptrAddr, dataCopy, ElementType.INT, length);
        return dataCopy;
    }

    @ExportMessage
    public SeqIterator iterator(
            @Shared("SeqItLoopProfile") @Cached("createCountingProfile()") LoopConditionProfile loopProfile,
            @Cached AltrepLengthNode lengthnode) {
        SeqIterator it = new SeqIterator(this, (int) lengthnode.execute(getOwner()));
        it.initLoopConditionProfile(loopProfile);
        return it;
    }

    @ExportMessage
    public RandomAccessIterator randomAccessIterator() {
        return new RandomAccessIterator(this);
    }

    @ExportMessage
    public boolean next(SeqIterator it, boolean withWrap,
                        @Shared("SeqItLoopProfile") @Cached("createCountingProfile()") LoopConditionProfile loopProfile) {
        return it.next(loopProfile, withWrap);
    }

    @ExportMessage(limit = "1")
    public int getIntAt(int index,
                        @Cached(value = "create()") AltrepGetIntAtNode getIntAtNode) {
        return (int) getIntAtNode.execute(getOwner(), index);
    }

    @ExportMessage(limit = "1")
    public int getNextInt(SeqIterator it,
                          @Cached(value = "create()") AltrepGetIntAtNode getIntAtNode) {
        return (int) getIntAtNode.execute(getOwner(), it.getIndex());
    }

    @ExportMessage(limit = "1")
    public int getInt(RandomAccessIterator it, int index,
                      @Cached(value = "create()") AltrepGetIntAtNode getIntAtNode) {
        return (int) getIntAtNode.execute(getOwner(), index);
    }

    // Write access to elements:

    private long invokeDataptrMethodUncached() {
        // TODO: Exception handling?
        dataptrCalled = true;
        return descriptor.invokeDataptrMethodUncached(getOwner(), true);
    }

    @ExportMessage
    public SeqWriteIterator writeIterator(@Cached AltrepLengthNode lengthNode) {
        int length = (int) lengthNode.execute(getOwner());
        return new SeqWriteIterator(this, length);
    }

    @ExportMessage
    public RandomAccessWriteIterator randomAccessWriteIterator() {
        return new RandomAccessWriteIterator(this);
    }

    @ExportMessage
    public void commitWriteIterator(SeqWriteIterator iterator, @SuppressWarnings("unused") boolean neverSeenNA) {
        iterator.commit();
    }

    @ExportMessage
    public void commitRandomAccessWriteIterator(RandomAccessWriteIterator iterator,
                                                @SuppressWarnings("unused") boolean neverSeenNA) {
        iterator.commit();
    }

    @ExportMessage(limit = "1")
    public void setIntAt(int index, int value,
                         @Cached("create()") @Shared("setElementAtNode") AltrepSetElementAtNode setElementAtNode) {
        setElementAtNode.execute(getOwner(), value, index);
    }

    @ExportMessage(limit = "1")
    public void setNextInt(SeqWriteIterator it, int value,
                           @Cached("create()") @Shared("setElementAtNode") AltrepSetElementAtNode setElementAtNode) {
        setElementAtNode.execute(getOwner(), value, it.getIndex());
    }

    @ExportMessage(limit = "1")
    public void setInt(RandomAccessWriteIterator it, int index, int value,
                       @Cached("create()") @Shared("setElementAtNode") AltrepSetElementAtNode setElementAtNode) {
        setElementAtNode.execute(getOwner(), value, index);
    }

    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        return "RAltIntVectorData: altrepData={" + altrepData.toString() + "}";
    }
}
