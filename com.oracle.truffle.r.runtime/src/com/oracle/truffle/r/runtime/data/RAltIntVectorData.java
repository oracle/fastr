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
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Exclusive;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.LoopConditionProfile;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.RandomAccessIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.RandomAccessWriteIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.SeqIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.SeqWriteIterator;
import com.oracle.truffle.r.runtime.data.altrep.AltIntegerClassDescriptor;
import com.oracle.truffle.r.runtime.data.altrep.AltRepClassDescriptor;
import com.oracle.truffle.r.runtime.data.altrep.AltrepSortedness;
import com.oracle.truffle.r.runtime.data.altrep.AltrepUtilities;
import com.oracle.truffle.r.runtime.data.altrep.RAltRepData;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.ffi.AltrepRFFI;
import com.oracle.truffle.r.runtime.ffi.util.NativeMemory;
import com.oracle.truffle.r.runtime.ffi.util.NativeMemory.ElementType;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;
import com.oracle.truffle.r.runtime.nodes.altrep.AltrepDuplicateNode;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

@ExportLibrary(VectorDataLibrary.class)
public class RAltIntVectorData implements TruffleObject, VectorDataWithOwner {
    private final RAltRepData altrepData;
    private final AltIntegerClassDescriptor descriptor;
    private boolean dataptrCalled;
    private RIntVector owner;
    @CompilationFinal private int cachedLength;

    public RAltIntVectorData(AltIntegerClassDescriptor descriptor, RAltRepData data) {
        this.altrepData = data;
        this.descriptor = descriptor;
        this.dataptrCalled = false;
        assert hasDescriptorRegisteredNecessaryMethods(descriptor):
                "Descriptor " + descriptor.toString() + " does not have registered all necessary methods";
    }

    private static boolean hasDescriptorRegisteredNecessaryMethods(AltIntegerClassDescriptor altIntClassDescr) {
        return altIntClassDescr.isLengthMethodRegistered() && altIntClassDescr.isDataptrMethodRegistered();
        /* TODO: && altIntClassDescr.isUnserializeMethodRegistered(); */
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
    public NACheck getNACheck(@Shared("naCheck") @Cached NACheck na) {
        na.enable(false);
        return na;
    }

    @ExportMessage
    public boolean isComplete(@Exclusive @Cached ConditionProfile hasNoNAMethodProfile,
                              @Cached AltrepRFFI.NoNANode noNANode) {
        if (hasNoNAMethodProfile.profile(descriptor.isNoNAMethodRegistered())) {
            return invokeNoNA(noNANode);
        } else {
            return false;
        }
    }

    @ExportMessage
    public long asPointer(@Shared("dataptrNode") @Cached AltrepRFFI.DataptrNode dataptrNode) {
        return dataptrNode.execute(getOwner(), true);
    }

    private boolean invokeNoNA(AltrepRFFI.NoNANode noNANode) {
        assert descriptor.isNoNAMethodRegistered();
        boolean noNA = noNANode.execute(getOwner());
        if (noNA && !owner.isComplete()) {
            owner.setComplete(true);
        }
        return noNA;
    }

    private int invokeLength(AltrepRFFI.LengthNode lengthNode, ConditionProfile isLengthNotCachedProfile) {
        if (isLengthNotCachedProfile.profile(!AltRepClassDescriptor.getNoMethodRedefinedAssumption().isValid() || cachedLength == 0)) {
            cachedLength = lengthNode.execute(owner);
        }
        return cachedLength;
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public final RType getType() {
        return RType.Integer;
    }

    @ExportMessage
    public int getLength(@Shared("lengthNode") @Cached AltrepRFFI.LengthNode lengthNode,
            @Shared("isLengthNotCachedProfile") @Cached ConditionProfile isLengthNotCachedProfile) {
        return invokeLength(lengthNode, isLengthNotCachedProfile);
    }

    @ExportMessage
    public RAltIntVectorData materialize(@Shared("dataptrNode") @Cached AltrepRFFI.DataptrNode dataptrNode,
                                         @Exclusive @Cached ConditionProfile dataptrNotCalledProfile) {
        // Dataptr altrep method call forces materialization
        if (dataptrNotCalledProfile.profile(!dataptrCalled)) {
            dataptrNode.execute(getOwner(), true);
        }
        return this;
    }

    @ExportMessage
    public boolean isWriteable() {
        // TODO: if (!dataptrCalled) return Dataptr_Or_Null != NULL
        return dataptrCalled;
    }

    @ExportMessage
    public static class IsSorted {
        @Specialization(guards = "hasIsSortedMethod(altIntVectorData)")
        public static boolean doWithNativeFunction(RAltIntVectorData altIntVectorData, boolean decreasing, boolean naLast,
                                                   @Cached AltrepRFFI.IsSortedNode isSortedNode) {
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
        public static boolean doWithoutNativeFunction(@SuppressWarnings("unused") RAltIntVectorData altIntVectorData,
                                                      @SuppressWarnings("unused") boolean decreasing,
                                                      @SuppressWarnings("unused") boolean naLast) {
            return false;
        }

        protected static boolean hasIsSortedMethod(RAltIntVectorData altIntVectorData) {
            return altIntVectorData.getDescriptor().isIsSortedMethodRegistered();
        }
    }

    @ExportMessage
    public static class GetIntRegion {
        @Specialization(guards = "hasGetRegionMethod(altIntVecData)")
        public static int doWithNativeFunction(RAltIntVectorData altIntVecData, int startIdx, int size, Object buffer,
                                        @SuppressWarnings("unused") InteropLibrary bufferInterop,
                                        @Cached AltrepRFFI.GetRegionNode getRegionNode) {
            return getRegionNode.execute(altIntVecData.getOwner(), startIdx, size, buffer);
        }

        @Specialization(guards = "!hasGetRegionMethod(altIntVecData)")
        public static int doWithoutNativeFunction(RAltIntVectorData altIntVecData, int startIdx, int size, Object buffer,
                                                  InteropLibrary bufferInterop,
                                                  @Shared("getIntAtNode") @Cached GetIntAtNode getIntAtNode,
                                                  @Shared("naCheck") @Cached NACheck naCheck) {
            int bufferIdx = 0;
            for (int index = startIdx; index < startIdx + size; index++) {
                try {
                    int value = altIntVecData.getIntAt(index, getIntAtNode, naCheck);
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
     * May return either RAltIntVectorData or RIntArrayVectorData, depending whether this vector data has
     * Duplicate method registered.
     */
    @ExportMessage
    public Object copy(boolean deep,
                       @Shared("duplicateNode") @Cached AltrepDuplicateNode duplicateNode) {
        return duplicateNode.execute(getOwner(), deep);
    }

    /**
     * TODO: This method gets called from Rf_duplicate which means that it eventually should make downcall into
     *   Duplicate. The behavior may change, so the implementation is not optimized.
     */
    @ExportMessage
    public int[] getIntDataCopy(@Shared("duplicateNode") @Cached AltrepDuplicateNode duplicateNode,
                                @Shared("dataptrNode") @Cached AltrepRFFI.DataptrNode dataptrNode,
                                @Shared("lengthNode") @Cached AltrepRFFI.LengthNode lengthNode) {
        // TODO: deep?
        int length = lengthNode.execute(getOwner());
        Object duplicatedObject = duplicateNode.execute(getOwner(), false);
        assert duplicatedObject instanceof RAltIntVectorData || duplicatedObject instanceof RIntArrayVectorData;
        if (duplicatedObject instanceof RAltIntVectorData) {
            return ((RAltIntVectorData) duplicatedObject).getDataCopy(dataptrNode, length);
        } else if (duplicatedObject instanceof RIntArrayVectorData) {
            return ((RIntArrayVectorData) duplicatedObject).getIntDataCopy();
        } else {
            throw RInternalError.shouldNotReachHere(
                    "RAltIntVectorData.getIntDataCopy: Unexpected returned object = " + duplicatedObject.toString()
            );
        }
    }

    @ExportMessage
    public int[] getReadonlyIntData(@Shared("dataptrNode") @Cached AltrepRFFI.DataptrNode dataptrNode,
                                    @Shared("lengthNode") @Cached AltrepRFFI.LengthNode lengthNode,
            @Shared("isLengthNotCachedProfile") @Cached ConditionProfile isLengthNotCachedProfile) {
        int length = invokeLength(lengthNode, isLengthNotCachedProfile);
        return getDataCopy(dataptrNode, length);
    }

    private int[] getDataCopy(AltrepRFFI.DataptrNode dataptrNode, int length) {
        long dataptrAddr = dataptrNode.execute(getOwner(), false);
        int[] dataCopy = new int[length];
        NativeMemory.copyMemory(dataptrAddr, dataCopy, ElementType.INT, length);
        return dataCopy;
    }

    @ExportMessage
    public SeqIterator iterator(
            @Shared("SeqItLoopProfile") @Cached("createCountingProfile()") LoopConditionProfile loopProfile,
            @Shared("lengthNode") @Cached AltrepRFFI.LengthNode lengthNode,
            @Shared("isLengthNotCachedProfile") @Cached ConditionProfile isLengthNotCachedProfile) {
        int length = invokeLength(lengthNode, isLengthNotCachedProfile);
        SeqIterator it = new SeqIterator(this, length);
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

    @ExportMessage
    public int getIntAt(int index,
                        @Shared("getIntAtNode") @Cached GetIntAtNode getIntAtNode,
                        @Shared("naCheck") @Cached NACheck naCheck) {
        int value = getIntAtNode.execute(getOwner(), index);
        naCheck.check(value);
        return value;
    }

    @ExportMessage
    public int getNextInt(SeqIterator it,
                          @Shared("getIntAtNode") @Cached GetIntAtNode getIntAtNode,
                          @Shared("naCheck") @Cached NACheck naCheck) {
        int value = getIntAtNode.execute(getOwner(), it.getIndex());
        naCheck.check(value);
        return value;
    }

    @ExportMessage
    public int getInt(RandomAccessIterator it, int index,
                      @Shared("getIntAtNode") @Cached GetIntAtNode getIntAtNode,
                      @Shared("naCheck") @Cached NACheck naCheck) {
        int value = getIntAtNode.execute(getOwner(), index);
        naCheck.check(value);
        return value;
    }

    // Write access to elements:

    @ExportMessage
    public SeqWriteIterator writeIterator(@Shared("lengthNode") @Cached AltrepRFFI.LengthNode lengthNode,
            @Shared("isLengthNotCachedProfile") @Cached ConditionProfile isLengthNotCachedProfile) {
        int length = invokeLength(lengthNode, isLengthNotCachedProfile);
        return new SeqWriteIterator(this, length);
    }

    @ExportMessage
    public RandomAccessWriteIterator randomAccessWriteIterator() {
        return new RandomAccessWriteIterator(this);
    }

    @ExportMessage
    public void setIntAt(int index, int value,
                         @Shared("dataptrNode") @Cached AltrepRFFI.DataptrNode dataptrNode,
                         @Shared("naCheck") @Cached NACheck naCheck) {
        naCheck.check(value);
        writeViaDataptrNode(dataptrNode, getOwner(), index, value);
    }

    @ExportMessage
    public void setNextInt(SeqWriteIterator it, int value,
                           @Shared("dataptrNode") @Cached AltrepRFFI.DataptrNode dataptrNode,
                           @Shared("naCheck") @Cached NACheck naCheck) {
        naCheck.check(value);
        writeViaDataptrNode(dataptrNode, getOwner(), it.getIndex(), value);
    }

    @ExportMessage
    public void setInt(RandomAccessWriteIterator it, int index, int value,
                       @Shared("dataptrNode") @Cached AltrepRFFI.DataptrNode dataptrNode,
                       @Shared("naCheck") @Cached NACheck naCheck) {
        naCheck.check(value);
        writeViaDataptrNode(dataptrNode, getOwner(), index, value);
    }

    private static void writeViaDataptrNode(AltrepRFFI.DataptrNode altIntDataptrNode, RIntVector altIntVec,
                                            int index, int value) {
        long dataptrAddr = altIntDataptrNode.execute(altIntVec, true);
        NativeMemory.putInt(dataptrAddr, index, value);
    }

    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        return "RAltIntVectorData: altrepData={" + altrepData.toString() + "}";
    }

    @GenerateUncached
    public abstract static class GetIntAtNode extends RBaseNode {
        public abstract int execute(Object altVec, int index);

        @Specialization(guards = "hasEltMethodRegistered(altIntVector)")
        protected int doAltIntWithElt(RIntVector altIntVector, int index,
                                   @Cached("create()") AltrepRFFI.EltNode eltNode) {
            assert AltrepUtilities.isAltrep(altIntVector);
            return (int) eltNode.execute(altIntVector, index);
        }

        @Specialization(guards = "!hasEltMethodRegistered(altIntVector)")
        protected int doAltIntWithoutElt(RIntVector altIntVector, int index,
                                      @Cached("create()") AltrepRFFI.DataptrNode altIntDataptrNode) {
            assert AltrepUtilities.isAltrep(altIntVector);
            long dataptrAddr = altIntDataptrNode.execute(altIntVector, false);
            return NativeMemory.getInt(dataptrAddr, index);
        }

        protected static boolean hasEltMethodRegistered(RIntVector altIntVector) {
            return AltrepUtilities.hasEltMethodRegistered(altIntVector);
        }
    }
}
