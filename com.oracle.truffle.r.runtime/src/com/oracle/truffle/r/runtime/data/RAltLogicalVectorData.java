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
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.SeqIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.SeqWriteIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.RandomAccessIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.RandomAccessWriteIterator;
import com.oracle.truffle.r.runtime.data.altrep.AltLogicalClassDescriptor;
import com.oracle.truffle.r.runtime.data.altrep.AltrepUtilities;
import com.oracle.truffle.r.runtime.data.altrep.RAltRepData;
import com.oracle.truffle.r.runtime.ffi.AltrepRFFI;
import com.oracle.truffle.r.runtime.ffi.util.NativeMemory;
import com.oracle.truffle.r.runtime.ops.na.NACheck;


@ExportLibrary(VectorDataLibrary.class)
public class RAltLogicalVectorData extends RAltrepNumericVectorData {
    public RAltLogicalVectorData(AltLogicalClassDescriptor descriptor, RAltRepData altrepData) {
        super(descriptor, altrepData);
    }

    public AltLogicalClassDescriptor getDescriptor() {
        return (AltLogicalClassDescriptor) descriptor;
    }

    @Override
    @ExportMessage
    public final RType getType() {
        return RType.Logical;
    }

    @ExportMessage
    public static class GetLogicalRegion {
        @Specialization(guards = "hasGetRegionMethod(altLogicalVecData)")
        public static int doWithNativeFunction(RAltLogicalVectorData altLogicalVecData, int startIdx, int size, Object buffer,
                                               @SuppressWarnings("unused") InteropLibrary bufferInterop,
                                               @Cached AltrepRFFI.GetRegionNode getRegionNode) {
            return getRegionNode.execute(altLogicalVecData.owner, startIdx, size, buffer);
        }

        @Specialization(guards = "!hasGetRegionMethod(altLogicalVecData)")
        public static int doWithoutNativeFunction(RAltLogicalVectorData altLogicalVecData, int startIdx, int size, Object buffer,
                                                  InteropLibrary bufferInterop,
                                                  @Shared("getLogicalAtNode") @Cached GetLogicalAtNode getLogicalAtNode,
                                                  @Shared("naCheck") @Cached NACheck naCheck) {
            int bufferIdx = 0;
            for (int index = startIdx; index < startIdx + size; index++) {
                try {
                    int value = altLogicalVecData.getLogicalAt(index, getLogicalAtNode, naCheck);
                    bufferInterop.writeArrayElement(buffer, bufferIdx, value);
                    bufferIdx++;
                } catch (InteropException e) {
                    throw RInternalError.shouldNotReachHere(e);
                }
            }
            return bufferIdx;
        }

        protected static boolean hasGetRegionMethod(RAltLogicalVectorData vecData) {
            return vecData.getDescriptor().isGetRegionMethodRegistered();
        }
    }

    @ExportMessage
    public byte getLogicalAt(int index,
                             @Shared("getLogicalAtNode") @Cached GetLogicalAtNode getLogicalAtNode,
                             @Shared("naCheck") @Cached NACheck naCheck) {
        byte value = getLogicalAtNode.execute(owner, index);
        naCheck.check(value);
        return value;
    }

    @ExportMessage
    public byte getNextLogical(SeqIterator it,
                               @Shared("getLogicalAtNode") @Cached GetLogicalAtNode getLogicalAtNode,
                               @Shared("naCheck") @Cached NACheck naCheck) {
        byte value = getLogicalAtNode.execute(owner, it.getIndex());
        naCheck.check(value);
        return value;
    }

    @ExportMessage
    public byte getLogical(@SuppressWarnings("unused") RandomAccessIterator it, int index,
                           @Shared("getLogicalAtNode") @Cached GetLogicalAtNode getLogicalAtNode,
                           @Shared("naCheck") @Cached NACheck naCheck) {
        byte value = getLogicalAtNode.execute(owner, index);
        naCheck.check(value);
        return value;
    }

    // Write access to elements:

    @ExportMessage
    public void setLogicalAt(int index, byte value,
                             @Shared("dataptrNode") @Cached AltrepRFFI.DataptrNode dataptrNode,
                             @Shared("naCheck") @Cached NACheck naCheck) {
        naCheck.check(value);
        writeViaDataptrNode(dataptrNode, index, value);
    }

    @ExportMessage
    public void setNextLogical(SeqWriteIterator it, byte value,
                               @Shared("dataptrNode") @Cached AltrepRFFI.DataptrNode dataptrNode,
                               @Shared("naCheck") @Cached NACheck naCheck) {
        naCheck.check(value);
        writeViaDataptrNode(dataptrNode, it.getIndex(), value);
    }

    @ExportMessage
    public void setLogical(@SuppressWarnings("unused") RandomAccessWriteIterator it, int index, byte value,
                           @Shared("dataptrNode") @Cached AltrepRFFI.DataptrNode dataptrNode,
                           @Shared("naCheck") @Cached NACheck naCheck) {
        naCheck.check(value);
        writeViaDataptrNode(dataptrNode, index, value);
    }

    private void writeViaDataptrNode(AltrepRFFI.DataptrNode dataptrNode,
                                     int index, byte value) {
        long addr = dataptrNode.execute(owner, true);
        NativeMemory.putInt(addr, index, RRuntime.logical2int(value));
    }

    @GenerateUncached
    @ImportStatic(AltrepUtilities.class)
    public abstract static class GetLogicalAtNode extends Node {
        public abstract byte execute(Object altLogicalVec, int index);

        @Specialization(guards = "hasEltMethodRegistered(altLogicalVec)")
        protected byte getLogicalAtWithElt(RLogicalVector altLogicalVec, int index,
                          @Cached AltrepRFFI.EltNode eltNode) {
            return (byte) eltNode.execute(altLogicalVec, index);
        }

        @Specialization(guards = "!hasEltMethodRegistered(altLogicalVec)")
        protected byte getLogicalAtWithoutElt(RLogicalVector altLogicalVec, int index,
                             @Cached AltrepRFFI.DataptrNode dataptrNode) {
            long dataptrAddr = dataptrNode.execute(altLogicalVec, false);
            int intValue = NativeMemory.getInt(dataptrAddr, index);
            return RRuntime.int2logical(intValue);
        }
    }
}
