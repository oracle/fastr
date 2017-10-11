/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.runtime.data.nodes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.data.RVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

public abstract class GetDataStore extends Node {

    protected static final int CACHE_LIMIT = 6;

    public static GetDataStore create() {
        return GetDataStoreNodeGen.create();
    }

    public abstract Object execute(RAbstractVector vector);

    @Specialization(guards = "vector.hasNativeMemoryData()")
    protected Object doNative(RVector<?> vector) {
        return vector.getNativeMirror();
    }

    @Specialization(guards = {"noNativeMemoryData(vector)", "vector.getClass() == vectorClass"}, limit = "CACHE_LIMIT")
    protected Object doGeneric(RAbstractVector vector,
                    @Cached("vector.getClass()") Class<? extends RAbstractVector> vectorClass,
                    @Cached("getStoreClass(vector)") Class<?> storeClass) {
        Object store = vectorClass.cast(vector).getInternalStore();
        assert store.getClass() == storeClass : "every concrete implementation of RAbstractVector#getInternalStore() must always return a store object of the same type.";
        return storeClass.cast(store);
    }

    @Fallback
    protected Object doFallback(RAbstractVector vector) {
        if (noNativeMemoryData(vector)) {
            RVector<?> vec = (RVector<?>) vector;
            if (vec.hasNativeMemoryData()) {
                return vec.getNativeMirror();
            }
        }
        return vector.getInternalStore();
    }

    protected static boolean noNativeMemoryData(RAbstractVector vector) {
        return !(vector instanceof RVector<?>) || !((RVector<?>) vector).hasNativeMemoryData();
    }

    protected static Class<?> getStoreClass(RAbstractVector vector) {
        return vector.getInternalStore().getClass();
    }
}
