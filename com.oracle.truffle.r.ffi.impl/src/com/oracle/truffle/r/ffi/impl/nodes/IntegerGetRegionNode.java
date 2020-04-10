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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.r.ffi.impl.llvm.AltrepLLVMDownCallNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.altrep.AltrepUtilities;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess.RandomIterator;
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

    @Specialization(limit = "3")
    public long getRegionForOtherVectors(RIntVector intVector, long fromIdx, long size, Object buffer,
                                         @CachedLibrary("buffer") InteropLibrary bufferInterop,
                                         @Cached(value = "intVector.access()", allowUncached = true) VectorAccess access) {
        assert bufferInterop.isPointer(buffer);
        if (fromIdx > Integer.MAX_VALUE || size > Integer.MAX_VALUE) {
            CompilerDirectives.transferToInterpreter();
            throw RError.error(RError.SHOW_CALLER, RError.Message.LONG_VECTORS_NOT_SUPPORTED);
        }

        int fromIdxInt = (int) fromIdx;
        int sizeInt = (int) size;
        long copied = 0;
        try (RandomIterator it = access.randomAccess(intVector)) {
            for (int idx = fromIdxInt; idx < fromIdxInt + sizeInt; idx++) {
                int value = access.getInt(it, idx);
                bufferInterop.writeArrayElement(buffer, idx, value);
                copied++;
            }
        } catch (Exception e) {
            throw RInternalError.shouldNotReachHere(e, "Some exception in IntegerGetRegionNode");
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
