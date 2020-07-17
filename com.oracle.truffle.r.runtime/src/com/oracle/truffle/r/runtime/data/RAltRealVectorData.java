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

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.altrep.AltRealClassDescriptor;
import com.oracle.truffle.r.runtime.data.altrep.AltrepUtilities;
import com.oracle.truffle.r.runtime.data.altrep.RAltRepData;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.SeqIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.RandomAccessIterator;
import com.oracle.truffle.r.runtime.ffi.AltrepRFFI;
import com.oracle.truffle.r.runtime.ffi.util.NativeMemory;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

@ExportLibrary(VectorDataLibrary.class)
public class RAltRealVectorData extends RAltrepNumericVectorData {

    public RAltRealVectorData(AltRealClassDescriptor descriptor, RAltRepData altrepData) {
        super(descriptor, altrepData);
    }

    public AltRealClassDescriptor getDescriptor() {
        return (AltRealClassDescriptor) descriptor;
    }

    @SuppressWarnings("static-method")
    @Override
    @ExportMessage
    public final RType getType() {
        return RType.Double;
    }

    @ExportMessage
    public static class GetDoubleRegion {
        @Specialization(guards = "hasGetRegionMethod(altRealVecData)")
        public static int doWithNativeFunction(RAltRealVectorData altRealVecData, int startIdx, int size, Object buffer,
                                               @SuppressWarnings("unused") InteropLibrary bufferInterop,
                                               @Cached AltrepRFFI.GetRegionNode getRegionNode) {
            return getRegionNode.execute(altRealVecData.owner, startIdx, size, buffer);
        }

        @Specialization(guards = "!hasGetRegionMethod(altRealVecData)")
        public static int doWithoutNativeFunction(RAltRealVectorData altRealVecData, int startIdx, int size, Object buffer,
                                                  InteropLibrary bufferInterop,
                                                  @Shared("getDoubleAtNode") @Cached GetDoubleAtNode getDoubleAtNode,
                                                  @Shared("naCheck") @Cached NACheck naCheck) {
            int bufferIdx = 0;
            for (int index = startIdx; index < startIdx + size; index++) {
                try {
                    double value = altRealVecData.getDoubleAt(index, getDoubleAtNode, naCheck);
                    bufferInterop.writeArrayElement(buffer, bufferIdx, value);
                    bufferIdx++;
                } catch (InteropException e) {
                    throw RInternalError.shouldNotReachHere(e);
                }
            }
            return bufferIdx;
        }

        protected static boolean hasGetRegionMethod(RAltRealVectorData vecData) {
            return vecData.getDescriptor().isGetRegionMethodRegistered();
        }
    }

    @ExportMessage
    public double[] getDoubleDataCopy() {
        throw RInternalError.unimplemented();
    }

    @ExportMessage
    public double[] getReadonlyDoubleData() {
        throw RInternalError.unimplemented();
    }

    @ExportMessage
    public double getDoubleAt(int index,
                              @Shared("getDoubleAtNode") @Cached GetDoubleAtNode getDoubleAtNode,
                              @Shared("naCheck") @Cached NACheck naCheck) {
        double value = getDoubleAtNode.execute(owner, index);
        naCheck.check(value);
        return value;
    }

    @ExportMessage
    public double getNextDouble(SeqIterator it,
                                @Shared("getDoubleAtNode") @Cached GetDoubleAtNode getDoubleAtNode,
                                @Shared("naCheck") @Cached NACheck naCheck) {
        double value = getDoubleAtNode.execute(owner, it.getIndex());
        naCheck.check(value);
        return value;
    }

    @ExportMessage
    public double getDouble(RandomAccessIterator it, int index,
                            @Shared("getDoubleAtNode") @Cached GetDoubleAtNode getDoubleAtNode,
                            @Shared("naCheck") @Cached NACheck naCheck) {
        double value = getDoubleAtNode.execute(owner, index);
        naCheck.check(value);
        return value;
    }

    // Write access to elements:

    @ExportMessage
    public void setDoubleAt(int index, double value,
                            @Shared("dataptrNode") @Cached AltrepRFFI.DataptrNode dataptrNode,
                            @Shared("naCheck") @Cached NACheck naCheck) {
        naCheck.check(value);
        writeViaDataptrNode(dataptrNode, index, value);
    }

    @ExportMessage
    public void setNextDouble(VectorDataLibrary.SeqWriteIterator it, double value,
                              @Shared("dataptrNode") @Cached AltrepRFFI.DataptrNode dataptrNode,
                              @Shared("naCheck") @Cached NACheck naCheck) {
        naCheck.check(value);
        writeViaDataptrNode(dataptrNode, it.getIndex(), value);
    }

    @ExportMessage
    public void setDouble(VectorDataLibrary.RandomAccessWriteIterator it, int index, double value,
                          @Shared("dataptrNode") @Cached AltrepRFFI.DataptrNode dataptrNode,
                          @Shared("naCheck") @Cached NACheck naCheck) {
        naCheck.check(value);
        writeViaDataptrNode(dataptrNode, index, value);
    }

    private void writeViaDataptrNode(AltrepRFFI.DataptrNode dataptrNode, int index, double value) {
        long addr = dataptrNode.execute(owner, true);
        NativeMemory.putDouble(addr, index, value);
    }

    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        return String.format("RAltRealVectorData{hash=%d, altrepData=%s}", this.hashCode(), altrepData.toString());
    }

    @GenerateUncached
    public abstract static class GetDoubleAtNode extends Node {
        public abstract double execute(Object altVec, int index);

        @Specialization(guards = "hasEltMethodRegistered(altRealVec)")
        protected double getDoubleWithElt(RDoubleVector altRealVec, int index,
                                      @Cached AltrepRFFI.EltNode eltNode) {
            assert AltrepUtilities.isAltrep(altRealVec);
            return (double) eltNode.execute(altRealVec, index);
        }

        @Specialization(guards = "!hasEltMethodRegistered(altRealVec)")
        protected double getDoubleWithoutElt(RDoubleVector altRealVec, int index,
                                         @Cached AltrepRFFI.DataptrNode dataptrNode) {
            assert AltrepUtilities.isAltrep(altRealVec);
            long dataptrAddr = dataptrNode.execute(altRealVec, false);
            return NativeMemory.getDouble(dataptrAddr, index);
        }

        protected static boolean hasEltMethodRegistered(RDoubleVector altRealVec) {
            return AltrepUtilities.hasEltMethodRegistered(altRealVec);
        }
    }
}
