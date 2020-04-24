/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.base.fastpaths;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RIntSeqVectorData;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary;
import com.oracle.truffle.r.runtime.nodes.RFastPathNode;

@ImportStatic(DSLConfig.class)
public abstract class SetDiffFastPath extends RFastPathNode {

    @Specialization(guards = {"isSequenceStride1(x)"}, limit = "getTypedVectorDataLibraryCacheSize()")
    protected static Object cached(RIntVector x, RIntVector y,
                    @CachedLibrary("y.getData()") VectorDataLibrary yLib) {
        Object yData = y.getData();
        RIntSeqVectorData seq = x.getSequence();
        int xLength = seq.getLength();
        int xStart = seq.getStart();
        boolean[] excluded = new boolean[xLength];

        VectorDataLibrary.SeqIterator it = yLib.iterator(yData);
        while (yLib.nextLoopCondition(yData, it)) {
            int element = yLib.getNextInt(yData, it);
            int index = element - xStart;
            if (index >= 0 && index < xLength) {
                excluded[index] = true;
            }
        }
        int cnt = 0;
        for (int i = 0; i < xLength; i++) {
            if (!excluded[i]) {
                cnt++;
            }
        }
        int[] result = new int[cnt];
        int pos = 0;
        for (int i = 0; i < xLength; i++) {
            if (!excluded[i]) {
                result[pos++] = i + xStart;
            }
        }
        return RDataFactory.createIntVector(result, true);
    }

    protected static boolean isSequenceStride1(RIntVector vec) {
        return vec.isSequence() && ((RIntSeqVectorData) vec.getData()).getStride() == 1;
    }

    @Fallback
    @SuppressWarnings("unused")
    protected static Object fallback(Object x, Object y) {
        return null;
    }
}
