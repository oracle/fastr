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

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.data.NativeDataAccess;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;

/**
 * Nodes contained in this class return an array that is either directly backing the vector data, or
 * copy of it if the data is not internally represented as contiguous Java array. The result must
 * not be modified, as it may result in different behaviour depending on the concrete vector
 * implementation.
 *
 * @see RVector#getDataCopy()
 */
public class GetReadonlyData {

    public abstract static class Double extends Node {
        public abstract double[] execute(RDoubleVector vector);

        @Specialization(guards = "!vec.hasNativeMemoryData()")
        protected double[] doManagedRVector(RDoubleVector vec) {
            return vec.getInternalManagedData();
        }

        @Specialization(guards = "vec.hasNativeMemoryData()")
        protected double[] doNativeDataRVector(RDoubleVector vec) {
            return NativeDataAccess.copyDoubleNativeData(vec.getNativeMirror());
        }

        public static Double create() {
            return GetReadonlyDataFactory.DoubleNodeGen.create();
        }
    }

    public abstract static class Int extends Node {
        public abstract int[] execute(RIntVector vector);

        @Specialization(guards = "!vec.hasNativeMemoryData()")
        protected int[] doManagedRVector(RIntVector vec) {
            return vec.getInternalManagedData();
        }

        @Specialization(guards = "vec.hasNativeMemoryData()")
        protected int[] doNativeDataRVector(RIntVector vec) {
            return NativeDataAccess.copyIntNativeData(vec.getNativeMirror());
        }

        public static Int create() {
            return GetReadonlyDataFactory.IntNodeGen.create();
        }
    }

    public abstract static class ListData extends Node {
        public abstract Object[] execute(RAbstractListVector vector);

        @Specialization(guards = "!vec.hasNativeMemoryData()")
        protected Object[] doManagedRVector(RList vec) {
            return vec.getInternalManagedData();
        }

        @Specialization(guards = "vec.hasNativeMemoryData()")
        protected Object[] doNativeDataRVector(@SuppressWarnings("unused") RList vec) {
            throw RInternalError.shouldNotReachHere("list cannot have native memory");
        }

        @Specialization
        protected Object[] doGeneric(RAbstractListVector vec) {
            return vec.materialize().getInternalManagedData();
        }

        public static ListData create() {
            return GetReadonlyDataFactory.ListDataNodeGen.create();
        }
    }
}
