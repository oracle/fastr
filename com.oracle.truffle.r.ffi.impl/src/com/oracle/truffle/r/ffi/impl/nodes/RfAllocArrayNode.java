/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.nodes.attributes.SpecialAttributesFunctions.SetDimAttributeNode;
import com.oracle.truffle.r.runtime.gnur.SEXPTYPE;

public abstract class RfAllocArrayNode extends FFIUpCallNode.Arg2 {
    public static RfAllocArrayNode create() {
        return RfAllocArrayNodeGen.create();
    }

    @Specialization(guards = "mode == type.code", limit = "getGenericDataLibraryCacheSize()")
    protected static Object allocArrayCached(@SuppressWarnings("unused") int mode, RIntVector dims,
                    @Cached(value = "getType(mode)", allowUncached = true) SEXPTYPE type,
                    @CachedLibrary("dims.getData()") VectorDataLibrary dimsDataLib,
                    @Cached SetDimAttributeNode setDimNode) {
        CompilerAsserts.compilationConstant(type);
        return allocArray(dims, dimsDataLib, type, setDimNode);
    }

    @Specialization(replaces = "allocArrayCached")
    @TruffleBoundary
    protected static Object allocArrayUncached(int mode, RIntVector dims, @Cached SetDimAttributeNode setDimNode) {
        return allocArray(dims, VectorDataLibrary.getFactory().getUncached(), getType(mode), setDimNode);
    }

    private static Object allocArray(RIntVector dims, VectorDataLibrary dimsDataLib, SEXPTYPE type, SetDimAttributeNode setDimNode) {
        Object dimsData = dims.getData();
        int dimsLen = dimsDataLib.getLength(dimsData);
        int[] newDims = new int[dimsLen];
        int totalLen = 1;
        for (int i = 0; i < dimsLen; i++) {
            int dimLen = dimsDataLib.getIntAt(dimsData, i);
            newDims[i] = dimLen;
            totalLen *= dimLen;
        }
        Object array = RDataFactory.createEmptyVectorFromSEXPType(type, totalLen);
        if (!(array instanceof RAbstractContainer)) {
            throw RInternalError.shouldNotReachHere("Type not implemented for Rf_allocArray");
        } else {
            setDimNode.setDimensions((RAbstractContainer) array, newDims);
            return array;
        }
    }

    protected static SEXPTYPE getType(int mode) {
        return SEXPTYPE.mapInt(mode);
    }
}
