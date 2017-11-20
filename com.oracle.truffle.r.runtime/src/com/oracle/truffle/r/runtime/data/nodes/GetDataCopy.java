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

import java.util.Arrays;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.data.NativeDataAccess;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.nodes.GetDataCopyFactory.DoubleNodeGen;
import com.oracle.truffle.r.runtime.data.nodes.GetDataCopyFactory.StringNodeGen;

/**
 * Nodes contained in this class materialize given vector into an array of corresponding type. The
 * array is always a copy of the original data and can be modified freely.
 *
 * @see RVector#getDataCopy()
 */
public abstract class GetDataCopy {

    public abstract static class Double extends Node {
        public abstract double[] execute(RAbstractDoubleVector vector);

        public static Double create() {
            return DoubleNodeGen.create();
        }

        @Specialization(guards = "!vec.hasNativeMemoryData()")
        protected double[] doManagedRVector(RDoubleVector vec) {
            double[] data = vec.getInternalManagedData();
            return Arrays.copyOf(data, data.length);
        }

        @Specialization(guards = "vec.hasNativeMemoryData()")
        protected double[] doNativeDataRVector(RDoubleVector vec) {
            return NativeDataAccess.copyDoubleNativeData(vec.getNativeMirror());
        }

        @Fallback
        protected double[] doOthers(RAbstractDoubleVector vec) {
            int len = vec.getLength();
            double[] result = new double[len];
            Object store = vec.getInternalStore();
            for (int i = 0; i < len; i++) {
                result[i] = vec.getDataAt(store, i);
            }
            return result;
        }
    }

    public abstract static class String extends Node {
        public abstract java.lang.String[] execute(RAbstractStringVector vector);

        public static String create() {
            return StringNodeGen.create();
        }

        @Specialization(guards = "vec.hasNativeMemoryData()")
        protected java.lang.String[] doManagedRVector(@SuppressWarnings("unused") RStringVector vec) {
            throw RInternalError.unimplemented("string vectors backed by native memory");
        }

        @Specialization(guards = "!vec.hasNativeMemoryData()")
        protected java.lang.String[] doNativeDataRVector(RStringVector vec) {
            java.lang.String[] data = vec.getInternalManagedData();
            return Arrays.copyOf(data, data.length);
        }
    }
}
