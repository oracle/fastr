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
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.RandomAccessWriteIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.RandomAccessIterator;
import com.oracle.truffle.r.runtime.data.altrep.AltComplexClassDescriptor;
import com.oracle.truffle.r.runtime.data.altrep.AltrepUtilities;
import com.oracle.truffle.r.runtime.data.altrep.RAltRepData;
import com.oracle.truffle.r.runtime.ffi.AltrepRFFI;
import com.oracle.truffle.r.runtime.ffi.util.NativeMemory;

@ExportLibrary(VectorDataLibrary.class)
public class RAltComplexVectorData extends RAltrepVectorData {
    private final AltComplexClassDescriptor classDescriptor;
    
    protected RAltComplexVectorData(AltComplexClassDescriptor classDescriptor, RAltRepData altrepData) {
        super(altrepData);
        this.classDescriptor = classDescriptor;
    }

    @Override
    @ExportMessage
    public final RType getType() {
        return RType.Complex;
    }

    @ExportMessage
    public static class GetComplexRegion {
        @Specialization(guards = "hasGetRegionMethod(altComplexVecData)")
        public static int doWithNativeFunction(RAltComplexVectorData altComplexVecData, int startIdx, int size, Object buffer,
                                               @SuppressWarnings("unused") InteropLibrary bufferInterop,
                                               @Cached AltrepRFFI.GetRegionNode getRegionNode) {
            return getRegionNode.execute(altComplexVecData.owner, startIdx, size, buffer);
        }

        @Specialization(guards = "!hasGetRegionMethod(altComplexVecData)")
        public static int doWithoutNativeFunction(RAltComplexVectorData altComplexVecData, int startIdx, int size, Object buffer,
                                                  InteropLibrary bufferInterop,
                                                  @Cached.Shared("getComplexAtNode") @Cached GetComplexAtNode getComplexAtNode) {
            int bufferIdx = 0;
            for (int index = startIdx; index < startIdx + size; index++) {
                try {
                    RComplex value = altComplexVecData.getComplexAt(index, getComplexAtNode);
                    bufferInterop.writeArrayElement(buffer, bufferIdx, value);
                    bufferIdx++;
                } catch (InteropException e) {
                    throw RInternalError.shouldNotReachHere(e);
                }
            }
            return bufferIdx;
        }

        protected static boolean hasGetRegionMethod(RAltComplexVectorData vecData) {
            return vecData.classDescriptor.isGetRegionMethodRegistered();
        }
    }

    @ExportMessage
    public RComplex getComplexAt(int index,
                         @Cached.Shared("getComplexAtNode") @Cached GetComplexAtNode getComplexAtNode) {
        return getComplexAtNode.execute(owner, index);
    }

    @ExportMessage
    public RComplex getNextComplex(VectorDataLibrary.SeqIterator it,
                           @Cached.Shared("getComplexAtNode") @Cached GetComplexAtNode getComplexAtNode) {
        return getComplexAtNode.execute(owner, it.getIndex());
    }

    @ExportMessage
    public RComplex getComplex(@SuppressWarnings("unused") RandomAccessIterator it, int index,
                       @Cached.Shared("getComplexAtNode") @Cached GetComplexAtNode getComplexAtNode) {
        return getComplexAtNode.execute(owner, index);
    }

    @ExportMessage
    public void setComplexAt(int index, RComplex value,
                         @Cached.Shared("dataptrNode") @Cached AltrepRFFI.DataptrNode dataptrNode) {
        writeViaDataptrNode(dataptrNode, index, value);
    }

    @ExportMessage
    public void setNextComplex(VectorDataLibrary.SeqWriteIterator it, RComplex value,
                           @Cached.Shared("dataptrNode") @Cached AltrepRFFI.DataptrNode dataptrNode) {
        writeViaDataptrNode(dataptrNode, it.getIndex(), value);
    }

    @ExportMessage
    public void setComplex(@SuppressWarnings("unused") RandomAccessWriteIterator it, int index, RComplex value,
                       @Cached.Shared("dataptrNode") @Cached AltrepRFFI.DataptrNode dataptrNode) {
        writeViaDataptrNode(dataptrNode, index, value);
    }

    private void writeViaDataptrNode(AltrepRFFI.DataptrNode dataptrNode,
                                     int index, RComplex value) {
        long addr = dataptrNode.execute(owner, true);
        NativeMemory.putDouble(addr, index * 2L, value.getRealPart());
        NativeMemory.putDouble(addr, index * 2L + 1L, value.getImaginaryPart());
    }

    @GenerateUncached
    @ImportStatic(AltrepUtilities.class)
    public abstract static class GetComplexAtNode extends Node {
        public abstract RComplex execute(Object altComplexVec, int index);

        @Specialization(guards = "hasEltMethodRegistered(altComplexVec)")
        protected RComplex getComplexAtWithElt(RComplexVector altComplexVec, int index,
                                       @Cached AltrepRFFI.EltNode eltNode) {
            return (RComplex) eltNode.execute(altComplexVec, index);
        }

        @Specialization(guards = "!hasEltMethodRegistered(altComplexVec)")
        protected RComplex getComplexAtWithoutElt(RComplexVector altComplexVec, int index,
                                          @Cached AltrepRFFI.DataptrNode dataptrNode) {
            long dataptrAddr = dataptrNode.execute(altComplexVec, false);
            return RComplex.valueOf(
                    NativeMemory.getDouble(dataptrAddr, index * 2L),
                    NativeMemory.getDouble(dataptrAddr, index * 2L + 1L)
            );
        }
    }
}
