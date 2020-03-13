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
package com.oracle.truffle.r.ffi.impl.nodes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.ffi.impl.llvm.AltrepLLVMDownCallNode;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RIntVectorData;
import com.oracle.truffle.r.runtime.data.RIntVectorDataLibrary;
import com.oracle.truffle.r.runtime.data.RAltIntVectorData;
import com.oracle.truffle.r.runtime.data.altrep.AltrepUtilities;
import com.oracle.truffle.r.runtime.ffi.NativeFunction;

@GenerateUncached
public abstract class IntegerGetRegionNode extends FFIUpCallNode.Arg4 {

    public static IntegerGetRegionNode create() {
        return IntegerGetRegionNodeGen.create();
    }

    @Specialization(guards = {"isAltrep(altIntVec)", "hasGetRegionMethodRegistered(altIntVec)"}, limit = "3")
    public long getRegionForAltIntegerVec(RIntVector altIntVec, long fromIdx, long size, Object buffer,
                                          @Cached(value = "create()", allowUncached = true) AltrepLLVMDownCallNode downCallNode) {
        Object count = downCallNode.call(NativeFunction.AltInteger_Get_region, altIntVec, fromIdx, size, buffer);
        // TODO: Better return
        assert count instanceof Integer || count instanceof Long;
        if (count instanceof Integer) {
            return ((Integer) count).longValue();
        } else {
            return (long) count;
        }
    }

    @Specialization(limit = "3", guards = {"isAltrep(altIntVec)", "!hasGetRegionMethodRegistered(altIntVec)"})
    public long getRegionForAltIntegerVecWithoutRegisteredMethod(RIntVector altIntVec, long fromIdx, long size, Object buffer,
                                                                 @CachedLibrary("buffer") InteropLibrary bufferInterop,
                                                                 @CachedLibrary("altIntVec.getData()") RIntVectorDataLibrary altIntVecDataLibrary) {
        assert bufferInterop.isPointer(buffer);
        long copied = 0;
        for (long idx = fromIdx; idx < fromIdx + size; idx++) {
            try {
                int value = altIntVecDataLibrary.getIntAt(altIntVec.getData(), (int) idx);
                bufferInterop.writeArrayElement(buffer, idx, value);
                copied++;
            } catch (Exception e) {
                throw RInternalError.shouldNotReachHere(e, "Some exception");
            }
        }
        return copied;
    }

    @Specialization(guards = "!isAltrep(intVector)", limit = "3")
    public long getRegionForNormalIntVector(RIntVector intVector, long fromIdx, long size, Object buffer,
                                            @CachedLibrary("buffer") InteropLibrary bufferInterop,
                                            @CachedLibrary("intVector.getData()") RIntVectorDataLibrary dataLibrary) {
        assert bufferInterop.isPointer(buffer);
        long copied = 0;
        for (long idx = fromIdx; idx < fromIdx + size; idx++) {
            try {
                int value = dataLibrary.getIntAt(intVector.getData(), (int) fromIdx);
                bufferInterop.writeArrayElement(buffer, idx, value);
                copied++;
            } catch (Exception e) {
                throw RInternalError.shouldNotReachHere(e, "Some exception");
            }
        }
        return copied;
    }

    @Fallback
    public long getRegionFallback(Object x, Object fromIdx, Object size, Object buffer) {
        throw RInternalError.shouldNotReachHere("Type error: Unexpected type:" + x.getClass());
    }

    protected static boolean isAltrep(Object object) {
        return AltrepUtilities.isAltrep(object);
    }

    protected static boolean hasGetRegionMethodRegistered(RIntVector altIntVec) {
        return AltrepUtilities.hasGetRegionMethodRegistered(altIntVec);
    }
}
