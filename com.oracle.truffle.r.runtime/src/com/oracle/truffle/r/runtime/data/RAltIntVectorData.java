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

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.RandomAccessIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.RandomAccessWriteIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.SeqIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.SeqWriteIterator;
import com.oracle.truffle.r.runtime.data.altrep.AltIntegerClassDescriptor;
import com.oracle.truffle.r.runtime.data.altrep.AltrepUtilities;
import com.oracle.truffle.r.runtime.data.altrep.RAltRepData;
import com.oracle.truffle.r.runtime.ffi.AltrepRFFI;
import com.oracle.truffle.r.runtime.ffi.util.NativeMemory;
import com.oracle.truffle.r.runtime.ffi.util.NativeMemory.ElementType;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;
import com.oracle.truffle.r.runtime.nodes.altrep.AltrepDuplicateNode;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

@ExportLibrary(VectorDataLibrary.class)
public class RAltIntVectorData extends RAltrepNumericVectorData {

    public RAltIntVectorData(AltIntegerClassDescriptor descriptor, RAltRepData data) {
        super(descriptor, data);
    }

    public AltIntegerClassDescriptor getDescriptor() {
        return (AltIntegerClassDescriptor) descriptor;
    }

    @SuppressWarnings("static-method")
    @Override
    @ExportMessage
    public final RType getType() {
        return RType.Integer;
    }

    @ExportMessage
    public static class GetIntRegion {
        @Specialization(guards = "hasGetRegionMethod(altIntVecData)")
        public static int doWithNativeFunction(RAltIntVectorData altIntVecData, int startIdx, int size, Object buffer,
                        @SuppressWarnings("unused") InteropLibrary bufferInterop,
                        @Cached AltrepRFFI.GetRegionNode getRegionNode) {
            return getRegionNode.execute(altIntVecData.owner, startIdx, size, buffer);
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

    @ExportMessage
    public int[] getIntDataCopy(@Cached AltrepDuplicateNode duplicateNode,
                    @Shared("dataptrNode") @Cached AltrepRFFI.DataptrNode dataptrNode,
                    @Shared("lengthNode") @Cached AltrepRFFI.LengthNode lengthNode,
                    @Shared("naCheck") @Cached NACheck naCheck) {
        // TODO: deep?
        int length = lengthNode.execute(owner);
        Object duplicatedObject = duplicateNode.execute(owner, false);
        if (duplicatedObject instanceof RAltIntVectorData) {
            return ((RAltIntVectorData) duplicatedObject).getDataCopy(dataptrNode, length);
        } else if (duplicatedObject instanceof RIntArrayVectorData) {
            return ((RIntArrayVectorData) duplicatedObject).getIntDataCopy();
        } else if (duplicatedObject instanceof RIntSeqVectorData) {
            return ((RIntSeqVectorData) duplicatedObject).getIntDataCopy(naCheck);
        } else {
            CompilerDirectives.transferToInterpreter();
            throw RInternalError.shouldNotReachHere(
                            "RAltIntVectorData.getIntDataCopy: Unexpected returned object = " + duplicatedObject.toString());
        }
    }

    @ExportMessage
    public int[] getReadonlyIntData(@Shared("dataptrNode") @Cached AltrepRFFI.DataptrNode dataptrNode,
                    @Shared("lengthNode") @Cached AltrepRFFI.LengthNode lengthNode) {
        int length = lengthNode.execute(owner);
        return getDataCopy(dataptrNode, length);
    }

    private int[] getDataCopy(AltrepRFFI.DataptrNode dataptrNode, int length) {
        long addr = dataptrNode.execute(owner, false);
        int[] dataCopy = new int[length];
        NativeMemory.copyMemory(addr, dataCopy, ElementType.INT, length);
        return dataCopy;
    }

    @ExportMessage
    public int getIntAt(int index,
                    @Shared("getIntAtNode") @Cached GetIntAtNode getIntAtNode,
                    @Shared("naCheck") @Cached NACheck naCheck) {
        int value = getIntAtNode.execute(owner, index);
        naCheck.check(value);
        return value;
    }

    @ExportMessage
    public int getNextInt(SeqIterator it,
                    @Shared("getIntAtNode") @Cached GetIntAtNode getIntAtNode,
                    @Shared("naCheck") @Cached NACheck naCheck) {
        int value = getIntAtNode.execute(owner, it.getIndex());
        naCheck.check(value);
        return value;
    }

    @ExportMessage
    public int getInt(@SuppressWarnings("unused") RandomAccessIterator it, int index,
                    @Shared("getIntAtNode") @Cached GetIntAtNode getIntAtNode,
                    @Shared("naCheck") @Cached NACheck naCheck) {
        int value = getIntAtNode.execute(owner, index);
        naCheck.check(value);
        return value;
    }

    // Write access to elements:

    @ExportMessage
    public void setIntAt(int index, int value,
                    @Shared("dataptrNode") @Cached AltrepRFFI.DataptrNode dataptrNode,
                    @Shared("naCheck") @Cached NACheck naCheck) {
        naCheck.check(value);
        writeViaDataptrNode(dataptrNode, index, value);
    }

    @ExportMessage
    public void setNextInt(SeqWriteIterator it, int value,
                    @Shared("dataptrNode") @Cached AltrepRFFI.DataptrNode dataptrNode,
                    @Shared("naCheck") @Cached NACheck naCheck) {
        naCheck.check(value);
        writeViaDataptrNode(dataptrNode, it.getIndex(), value);
    }

    @ExportMessage
    public void setInt(@SuppressWarnings("unused") RandomAccessWriteIterator it, int index, int value,
                    @Shared("dataptrNode") @Cached AltrepRFFI.DataptrNode dataptrNode,
                    @Shared("naCheck") @Cached NACheck naCheck) {
        naCheck.check(value);
        writeViaDataptrNode(dataptrNode, index, value);
    }

    private void writeViaDataptrNode(AltrepRFFI.DataptrNode dataptrNode,
                    int index, int value) {
        long addr = dataptrNode.execute(owner, true);
        NativeMemory.putInt(addr, index, value);
    }

    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        return String.format("RAltIntVectorData{hash=%d, altrepData=%s}", this.hashCode(), altrepData.toString());
    }

    @GenerateUncached
    public abstract static class GetIntAtNode extends RBaseNode {
        public abstract int execute(Object altVec, int index);

        @Specialization(guards = "hasEltMethodRegistered(altIntVector)")
        protected int doAltIntWithElt(RIntVector altIntVector, int index,
                        @Cached AltrepRFFI.EltNode eltNode) {
            assert AltrepUtilities.isAltrep(altIntVector);
            return (int) eltNode.execute(altIntVector, index);
        }

        @Specialization(guards = "!hasEltMethodRegistered(altIntVector)")
        protected int doAltIntWithoutElt(RIntVector altIntVector, int index,
                        @Cached AltrepRFFI.DataptrNode dataptrNode) {
            assert AltrepUtilities.isAltrep(altIntVector);
            long dataptrAddr = dataptrNode.execute(altIntVector, false);
            return NativeMemory.getInt(dataptrAddr, index);
        }

        protected static boolean hasEltMethodRegistered(RIntVector altIntVector) {
            return AltrepUtilities.hasEltMethodRegistered(altIntVector);
        }
    }
}
