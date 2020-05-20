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
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.LoopConditionProfile;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.RandomAccessIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.RandomAccessWriteIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.SeqIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.SeqWriteIterator;
import com.oracle.truffle.r.runtime.data.altrep.AltIntegerClassDescriptor;
import com.oracle.truffle.r.runtime.data.altrep.AltrepSortedness;
import com.oracle.truffle.r.runtime.data.altrep.RAltRepData;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
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
    public void setOwner(RAbstractContainer newOwner) {
        owner = (RIntVector) newOwner;
    }

    private RIntVector getOwner() {
        assert owner != null;
        return owner;
    }

    public void setDataptrCalled() {
        dataptrCalled = true;
    }

    public boolean wasDataptrCalled() {
        return dataptrCalled;
    }

    @ExportMessage
    public NACheck getNACheck(@Cached() NACheck na) {
        na.enable(false);
        return na;
    }

    @ExportMessage
    public boolean noNA(@Cached("createBinaryProfile()") ConditionProfile hasNoNAMethodProfile,
                        @Cached AltrepRFFI.AltIntNoNANode noNANode,
                        @Cached AltrepRFFI.AltIntGetRegionNode getRegionNode,
                        @Cached AltrepLengthNode lengthNode) {
        if (hasNoNAMethodProfile.profile(descriptor.isNoNAMethodRegistered())) {
            return noNANode.execute(owner);
        } else {
            // Get whole region
            int length = getLength(lengthNode);
            int[] buffer = new int[length];
            int copied = getRegionNode.execute(owner, 0, getLength(lengthNode), buffer);
            if (copied != length) {
                throw RInternalError.shouldNotReachHere();
            }
            for (int item : buffer) {
                if (RRuntime.isNA(item)) {
                    return false;
                }
            }
            return true;
        }
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
        return false;
    }

    @ExportMessage
    public static class IsSorted {
        @Specialization(guards = "hasIsSortedMethod(altIntVectorData)")
        public static boolean doWithNativeFunction(RAltIntVectorData altIntVectorData, boolean decreasing, boolean naLast,
                                            @Cached AltrepRFFI.AltIntIsSortedNode isSortedNode) {
            AltrepSortedness sortedness = isSortedNode.execute(altIntVectorData.getOwner());
            if (decreasing) {
                if (naLast && sortedness == AltrepSortedness.SORTED_DECR) {
                    return true;
                } else {
                    return !naLast && sortedness == AltrepSortedness.SORTED_DECR_NA_1ST;
                }
            } else {
                if (naLast && sortedness == AltrepSortedness.SORTED_INCR) {
                    return true;
                } else {
                    return !naLast && sortedness == AltrepSortedness.SORTED_INCR_NA_1ST;
                }
            }
        }

        @Specialization(guards = "!hasIsSortedMethod(altIntVectorData)")
        public static boolean doWithoutNativeFunction(RAltIntVectorData altIntVectorData, boolean decreasing, boolean naLast) {
            return false;
        }

        protected static boolean hasIsSortedMethod(RAltIntVectorData altIntVectorData) {
            return altIntVectorData.getDescriptor().isIsSortedMethodRegistered();
        }
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
    public static class GetIntRegion {
        @Specialization(guards = "hasGetRegionMethod(altIntVecData)")
        public static int doWithNativeFunction(RAltIntVectorData altIntVecData, int startIdx, int size, Object buffer,
                                        @SuppressWarnings("unused") InteropLibrary bufferInterop,
                                        @Cached AltrepRFFI.AltIntGetRegionNode getRegionNode) {
            return getRegionNode.execute(altIntVecData.getOwner(), startIdx, size, buffer);
        }

        @Specialization(guards = "!hasGetRegionMethod(altIntVecData)")
        public static int doWithoutNativeFunction(RAltIntVectorData altIntVecData, int startIdx, int size, Object buffer,
                                                  InteropLibrary bufferInterop,
                                                  @Cached AltrepGetIntAtNode getIntAtNode) {
            int bufferIdx = 0;
            for (int index = startIdx; index < startIdx + size; index++) {
                try {
                    int value = altIntVecData.getIntAt(index, getIntAtNode);
                    bufferInterop.writeArrayElement(buffer, bufferIdx, value);
                    bufferIdx++;
                } catch (InteropException e) {
                    throw RInternalError.shouldNotReachHere(e);
                }
            }
            return bufferIdx;
        }

        protected static boolean hasGetRegionMethod(RAltIntVectorData vecData) {
            return vecData.getDescriptor().isGetRegionMethodRegistered();
        }
    }

    /**
     * TODO: This method gets called from Rf_duplicate which means that it eventually should make downcall into
     *   Duplicate. The behavior may change, so the implementation is not optimized.
     */
    @ExportMessage
    public int[] getIntDataCopy(@Cached AltrepDuplicateNode duplicateNode,
                                @Cached AltrepRFFI.AltIntDataptrNode dataptrNode,
                                @Cached AltrepLengthNode lengthNode) {
        // TODO: deep?
        Object duplicatedObject = duplicateNode.execute(getOwner(), false);
        assert duplicatedObject instanceof RAltIntVectorData || duplicatedObject instanceof RIntArrayVectorData;
        if (duplicatedObject instanceof RAltIntVectorData) {
            return ((RAltIntVectorData) duplicatedObject).getDataCopy(dataptrNode, lengthNode);
        } else if (duplicatedObject instanceof RIntArrayVectorData) {
            return ((RIntArrayVectorData) duplicatedObject).getIntDataCopy();
        } else {
            throw RInternalError.shouldNotReachHere(
                    "RAltIntVectorData.getIntDataCopy: Unexpected returned object = " + duplicatedObject.toString()
            );
        }
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
    public boolean nextImpl(SeqIterator it, boolean loopCondition,
                        @Shared("SeqItLoopProfile") @Cached("createCountingProfile()") LoopConditionProfile loopProfile) {
        return it.next(loopCondition, loopProfile);
    }

    @ExportMessage
    public void nextWithWrap(SeqIterator it,
                             @Shared("SeqItLoopProfile") @Cached("createCountingProfile()") LoopConditionProfile loopProfile) {
        it.nextWithWrap(loopProfile);
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
