/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.oracle.truffle.r.runtime.data.nodes;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.data.NativeDataAccess;
import com.oracle.truffle.r.runtime.data.RComplexVector;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RList;
import java.util.Arrays;

/**
 * Nodes contained in this class allow to reuse an internal data array of a vector which is
 * temporary (has zero refCount) and its data are not natively held. Otherwise a copy of the data
 * array is returned.
 */
public class VectorDataReuse {

    public abstract static class Int extends Node {

        public abstract int[] execute(RIntVector vector);

        @Specialization(guards = {"!vec.hasNativeMemoryData()", "vec.isTemporary()"})
        protected int[] doManagedTempRVector(RIntVector vec) {
            return vec.getInternalManagedData();
        }

        @Specialization(guards = {"!vec.hasNativeMemoryData()", "!vec.isTemporary()"})
        protected int[] doManagedRVector(RIntVector vec) {
            int[] data = vec.getInternalManagedData();
            return Arrays.copyOf(data, data.length);
        }

        @Specialization(guards = "vec.hasNativeMemoryData()")
        protected int[] doNativeDataRVector(RIntVector vec) {
            return NativeDataAccess.copyIntNativeData(vec.getNativeMirror());
        }

        public static Int create() {
            return VectorDataReuseFactory.IntNodeGen.create();
        }
    }

    public abstract static class Double extends Node {

        public abstract double[] execute(RDoubleVector vector);

        @Specialization(guards = {"!vec.hasNativeMemoryData()", "vec.isTemporary()"})
        protected double[] doManagedTempRVector(RDoubleVector vec) {
            return vec.getInternalManagedData();
        }

        @Specialization(guards = {"!vec.hasNativeMemoryData()", "!vec.isTemporary()"})
        protected double[] doManagedRVector(RDoubleVector vec) {
            double[] data = vec.getInternalManagedData();
            return Arrays.copyOf(data, data.length);
        }

        @Specialization(guards = "vec.hasNativeMemoryData()")
        protected double[] doNativeDataRVector(RDoubleVector vec) {
            return NativeDataAccess.copyDoubleNativeData(vec.getNativeMirror());
        }

        public static Double create() {
            return VectorDataReuseFactory.DoubleNodeGen.create();
        }
    }

    public abstract static class Complex extends Node {

        public abstract double[] execute(RComplexVector vector);

        @Specialization(guards = {"!vec.hasNativeMemoryData()", "vec.isTemporary()"})
        protected double[] doManagedTempRVector(RComplexVector vec) {
            return vec.getInternalManagedData();
        }

        @Specialization(guards = {"!vec.hasNativeMemoryData()", "!vec.isTemporary()"})
        protected double[] doManagedRVector(RComplexVector vec) {
            double[] data = vec.getInternalManagedData();
            return Arrays.copyOf(data, data.length);
        }

        @Specialization(guards = "vec.hasNativeMemoryData()")
        protected double[] doNativeDataRVector(RComplexVector vec) {
            return NativeDataAccess.copyComplexNativeData(vec.getNativeMirror());
        }

        public static Complex create() {
            return VectorDataReuseFactory.ComplexNodeGen.create();
        }
    }

    public abstract static class ListData extends Node {

        public abstract Object[] execute(RList vector);

        @Specialization(guards = {"!vec.hasNativeMemoryData()", "vec.isTemporary()"})
        protected Object[] doManagedTempRVector(RList vec) {
            return vec.getInternalManagedData();
        }

        @Specialization(guards = {"!vec.hasNativeMemoryData()", "!vec.isTemporary()"})
        protected Object[] doManagedRVector(RList vec) {
            Object[] data = vec.getInternalManagedData();
            return Arrays.copyOf(data, data.length);
        }

        @Specialization(guards = "vec.hasNativeMemoryData()")
        protected Object[] doNativeDataRVector(@SuppressWarnings("unused") RList vec) {
            throw RInternalError.shouldNotReachHere("list cannot have native memory");
        }

        public static ListData create() {
            return VectorDataReuseFactory.ListDataNodeGen.create();
        }
    }
}
