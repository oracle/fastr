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
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.altrep.AltRawClassDescriptor;
import com.oracle.truffle.r.runtime.data.altrep.AltrepUtilities;
import com.oracle.truffle.r.runtime.data.altrep.RAltRepData;
import com.oracle.truffle.r.runtime.ffi.AltrepRFFI;
import com.oracle.truffle.r.runtime.ffi.util.NativeMemory;

@ExportLibrary(VectorDataLibrary.class)
public class RAltRawVectorData extends RAltrepVectorData {
    private final AltRawClassDescriptor classDescriptor;

    protected RAltRawVectorData(AltRawClassDescriptor classDescriptor, RAltRepData altrepData) {
        super(altrepData);
        this.classDescriptor = classDescriptor;
    }

    @Override
    @ExportMessage
    public final RType getType() {
        return RType.Raw;
    }

    public AltRawClassDescriptor getDescriptor() {
        return classDescriptor;
    }

    @ExportMessage
    public static class GetRawRegion {
        @Specialization(guards = "hasGetRegionMethod(altRawVectorData)")
        public static int doWithNativeFunction(RAltRawVectorData altRawVectorData, int startIdx, int size, Object buffer,
                        @SuppressWarnings("unused") InteropLibrary bufferInterop,
                        @Cached AltrepRFFI.GetRegionNode getRegionNode) {
            return getRegionNode.execute(altRawVectorData.owner, startIdx, size, buffer);
        }

        @Specialization(guards = "!hasGetRegionMethod(altRawVectorData)")
        public static int doWithoutNativeFunction(RAltRawVectorData altRawVectorData, int startIdx, int size, Object buffer,
                        InteropLibrary bufferInterop,
                        @Shared("getRawAtNode") @Cached GetRawAtNode getRawAtNode) {
            int bufferIdx = 0;
            for (int index = startIdx; index < startIdx + size; index++) {
                try {
                    int value = altRawVectorData.getRawAt(index, getRawAtNode);
                    bufferInterop.writeArrayElement(buffer, bufferIdx, value);
                    bufferIdx++;
                } catch (InteropException e) {
                    throw RInternalError.shouldNotReachHere(e);
                }
            }
            return bufferIdx;
        }

        protected static boolean hasGetRegionMethod(RAltRawVectorData vecData) {
            return vecData.classDescriptor.isGetRegionMethodRegistered();
        }
    }

    @ExportMessage
    public byte getRawAt(int index,
                    @Shared("getRawAtNode") @Cached GetRawAtNode getRawAtNode) {
        return getRawAtNode.execute(owner, index);
    }

    @ExportMessage
    public byte getNextRaw(VectorDataLibrary.SeqIterator it,
                    @Shared("getRawAtNode") @Cached GetRawAtNode getRawAtNode) {
        return getRawAtNode.execute(owner, it.getIndex());
    }

    @ExportMessage
    public byte getRaw(@SuppressWarnings("unused") VectorDataLibrary.RandomAccessIterator it, int index,
                    @Shared("getRawAtNode") @Cached GetRawAtNode getRawAtNode) {
        return getRawAtNode.execute(owner, index);
    }

    @ExportMessage
    public void setRawAt(int index, byte value,
                    @Shared("dataptrNode") @Cached AltrepRFFI.DataptrNode dataptrNode) {
        writeViaDataptrNode(dataptrNode, index, value);
    }

    @ExportMessage
    public void setNextRaw(VectorDataLibrary.SeqWriteIterator it, byte value,
                    @Shared("dataptrNode") @Cached AltrepRFFI.DataptrNode dataptrNode) {
        writeViaDataptrNode(dataptrNode, it.getIndex(), value);
    }

    @ExportMessage
    public void setRaw(@SuppressWarnings("unused") VectorDataLibrary.RandomAccessWriteIterator it, int index, byte value,
                    @Shared("dataptrNode") @Cached AltrepRFFI.DataptrNode dataptrNode) {
        writeViaDataptrNode(dataptrNode, index, value);
    }

    private void writeViaDataptrNode(AltrepRFFI.DataptrNode dataptrNode,
                    int index, byte value) {
        long addr = dataptrNode.execute(owner, true);
        NativeMemory.putByte(addr, index, value);
    }

    @GenerateUncached
    @ImportStatic(AltrepUtilities.class)
    public abstract static class GetRawAtNode extends Node {
        public abstract byte execute(Object altRawVec, int index);

        @Specialization(guards = "hasEltMethodRegistered(altRawVec)")
        protected byte getRawAtWithElt(RRawVector altRawVec, int index,
                        @Cached AltrepRFFI.EltNode eltNode) {
            return (byte) eltNode.execute(altRawVec, index);
        }

        @Specialization(guards = "!hasEltMethodRegistered(altRawVec)")
        protected byte getRawAtWithoutElt(RRawVector altRawVec, int index,
                        @Cached AltrepRFFI.DataptrNode dataptrNode) {
            long dataptrAddr = dataptrNode.execute(altRawVec, false);
            return NativeMemory.getByte(dataptrAddr, index);
        }
    }
}
