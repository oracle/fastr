/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.base.fastpaths;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RIntSequence;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.nodes.RFastPathNode;

public abstract class SetDiffFastPath extends RFastPathNode {

    @Specialization(guards = {"x.getStride() == 1", "y.getClass() == yClass"})
    protected Object setdiff(RIntSequence x, RAbstractIntVector y,
                    @Cached("y.getClass()") Class<? extends RAbstractIntVector> yClass) {
        RAbstractIntVector profiledY = yClass.cast(y);
        int xLength = x.getLength();
        int xStart = x.getStart();
        int yLength = profiledY.getLength();
        boolean[] excluded = new boolean[xLength];

        for (int i = 0; i < yLength; i++) {
            int element = profiledY.getDataAt(i);
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

    @Fallback
    @SuppressWarnings("unused")
    protected Object fallback(Object x, Object y) {
        return null;
    }
}
