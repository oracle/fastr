/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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

package com.oracle.truffle.r.runtime.env.frame;

/**
 * A helper class for frame slot indexing. Auxiliary and normal frame slots are both indexed from 0,
 * therefore, we have to differentiate between these two. In our case, auxiliary indexes are represented
 * by negative integers, i.e., everything that is returned as an integer index from {@link FrameSlotChangeMonitor}
 * is either negative for aux slot or positive for normal slot.
 *
 * Note that it is not possible to refactor this class to a normal class that would wrap the index itself
 * and the type of the index because of the performance - any instance of that class could potentially escape from
 * any compilation unit, assuming that they would be cached as a field in a node in some AST. That means that for
 * every read of such an instance, we would have to generate a {@code LoadField} instruction.
 *
 * TODO: Refactor indexes to longs and convert them to real indexes with bit arithmetics, so that
 * there is a clear distinction between auxiliary slot index and normal slot index.
 */
public class FrameIndex {
    public static final int UNITIALIZED_INDEX = Integer.MIN_VALUE;

    public static boolean isUninitializedIndex(int index) {
        return index == UNITIALIZED_INDEX;
    }

    public static boolean isInitializedIndex(int index) {
        return !isUninitializedIndex(index);
    }

    public static boolean representsAuxiliaryIndex(int index) {
        return index < 0;
    }

    public static boolean representsNormalIndex(int index) {
        return index >= 0;
    }

    public static int transformIndex(int index) {
        return (-index) - 1;
    }

    /**
     * Returns an integer that can be used to index an auxiliary slot in a real frame.
     */
    public static int toAuxiliaryIndex(int index) {
        if (representsAuxiliaryIndex(index)) {
            return (-index) - 1;
        } else {
            assert representsNormalIndex(index);
            return index;
        }
    }

    public static int toNormalIndex(int index) {
        if (representsNormalIndex(index)) {
            return index;
        } else {
            assert representsAuxiliaryIndex(index);
            return (-index) - 1;
        }
    }
}
