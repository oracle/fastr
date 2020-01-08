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

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.data.altrep.AltIntegerClassDescriptor;
import com.oracle.truffle.r.runtime.data.altrep.RAltIntegerVec;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;

@GenerateUncached
public abstract class IntegerGetRegionNode extends FFIUpCallNode.Arg4 {

    public static IntegerGetRegionNode create() {
        return IntegerGetRegionNodeGen.create();
    }

    @Specialization(guards = {"hasGetRegionMethodRegistered(altIntVec)"}, limit = "3")
    public long getRegionForAltIntegerVec(RAltIntegerVec altIntVec, long fromIdx, long size, Object buffer,
                                          @CachedLibrary("buffer") InteropLibrary bufferInterop,
                                          @CachedLibrary("altIntVec.getDescriptor().getGetRegionMethod()") InteropLibrary methodInterop) {
        assert bufferInterop.isPointer(buffer);
        return altIntVec.getDescriptor().invokeGetRegionMethod(altIntVec, methodInterop, fromIdx, size, buffer);
    }

    @Specialization(limit = "3")
    public long getRegionForAltIntegerVecWithoutRegisteredMethod(RAltIntegerVec altIntVec, long fromIdx, long size,
                                                                 Object buffer,
                                                                 @CachedLibrary("buffer") InteropLibrary bufferInterop,
                                                                 @CachedLibrary("altIntVec.getDescriptor().getEltMethod()") InteropLibrary eltMethodInterop) {
        assert bufferInterop.isPointer(buffer);
        AltIntegerClassDescriptor descriptor = altIntVec.getDescriptor();
        long copied = 0;
        for (long idx = fromIdx; idx < fromIdx + size; idx++) {
            try {
                int value = descriptor.invokeEltMethod(altIntVec, eltMethodInterop, (int) idx);
                bufferInterop.writeArrayElement(buffer, idx, value);
                copied++;
            } catch (Exception e) {
                throw RInternalError.shouldNotReachHere(e, "Some exception");
            }
        }
        return copied;
    }

    @Specialization(limit = "3")
    public long getRegionForNormalIntVector(RAbstractIntVector intVector, long fromIdx, long size, Object buffer,
                                         @CachedLibrary("buffer") InteropLibrary bufferInterop) {
        assert bufferInterop.isPointer(buffer);
        long copied = 0;
        for (long idx = fromIdx; idx < fromIdx + size; idx++) {
            try {
                int value = intVector.getDataAt((int) idx);
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

    protected static boolean hasGetRegionMethodRegistered(RAltIntegerVec altIntVec) {
        return altIntVec.getDescriptor().isGetRegionMethodRegistered();
    }
}
