/*
 * Copyright (c) 2015, 2023, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Arrays;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.RandomAccessIterator;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;
import com.oracle.truffle.r.runtime.nodes.RFastPathNode;

public abstract class IntersectFastPath extends RFastPathNode {

    private static final int[] EMPTY_INT_ARRAY = new int[0];

    @ImportStatic(DSLConfig.class)
    protected abstract static class IntersectSortedNode extends Node {

        private final boolean isSorted;

        private final ConditionProfile resizeProfile = ConditionProfile.createCountingProfile();
        private final ConditionProfile valueEqualsProfile = ConditionProfile.createCountingProfile();
        private final ConditionProfile valueSmallerProfile = ConditionProfile.createCountingProfile();
        private final ConditionProfile exit1Profile = ConditionProfile.createCountingProfile();
        private final ConditionProfile exit2Profile = ConditionProfile.createCountingProfile();
        private final ConditionProfile exit3Profile = ConditionProfile.createCountingProfile();
        private final ConditionProfile exit4Profile = ConditionProfile.createCountingProfile();
        private final ConditionProfile exit5Profile = ConditionProfile.createCountingProfile();
        private final ConditionProfile exit6Profile = ConditionProfile.createCountingProfile();
        private final ConditionProfile resultLengthMatchProfile = ConditionProfile.createBinaryProfile();

        protected IntersectSortedNode(boolean isSorted) {
            this.isSorted = isSorted;
        }

        @Override
        public NodeCost getCost() {
            return NodeCost.NONE;
        }

        abstract int[] execute(RIntVector x, RIntVector y);

        @Specialization(limit = "getTypedVectorDataLibraryCacheSize()")
        protected int[] exec(RIntVector x, RIntVector y,
                        @CachedLibrary("x.getData()") VectorDataLibrary xLib,
                        @CachedLibrary("y.getData()") VectorDataLibrary yLib) {
            Object xData = x.getData();
            Object yData = y.getData();
            int xLength = xLib.getLength(xData);
            int yLength = yLib.getLength(yData);
            RandomAccessIterator xrit = xLib.randomAccessIterator(xData);
            RandomAccessIterator yrit = yLib.randomAccessIterator(yData);

            int[] result = EMPTY_INT_ARRAY;
            int maxResultLength = Math.min(xLength, yLength);
            int count = 0;
            int xPos = 0;
            int yPos = 0;
            int xValue = xLib.getInt(xData, xrit, xPos);
            int yValue = yLib.getInt(yData, yrit, yPos);
            while (true) {
                if (valueEqualsProfile.profile(xValue == yValue)) {
                    if (resizeProfile.profile(count >= result.length)) {
                        result = Arrays.copyOf(result, Math.min(maxResultLength, Math.max(result.length * 2, 8)));
                    }
                    result[count++] = xValue;
                    // advance over similar entries
                    while (true) {
                        if (exit1Profile.profile(xPos >= xLength - 1)) {
                            break;
                        }
                        int nextValue = xLib.getInt(xData, xrit, xPos + 1);
                        if (exit2Profile.profile(xValue != nextValue)) {
                            break;
                        }
                        xPos++;
                        xValue = nextValue;
                    }
                    if (exit3Profile.profile(++xPos >= xLength) || exit4Profile.profile(++yPos >= yLength)) {
                        break;
                    }
                    xValue = getNextValue(xLib, xData, xrit, xPos, xValue);
                    yValue = getNextValue(yLib, yData, yrit, yPos, yValue);
                } else if (valueSmallerProfile.profile(xValue < yValue)) {
                    if (exit5Profile.profile(++xPos >= xLength)) {
                        break;
                    }
                    xValue = getNextValue(xLib, xData, xrit, xPos, xValue);
                } else {
                    if (exit6Profile.profile(++yPos >= yLength)) {
                        break;
                    }
                    yValue = getNextValue(yLib, yData, yrit, yPos, yValue);
                }
            }
            if (!isSorted) {
                // check remaining array entries
                while (++xPos < xLength) {
                    xValue = getNextValue(xLib, xData, xrit, xPos, xValue);
                }
                while (++yPos < yLength) {
                    yValue = getNextValue(yLib, yData, yrit, yPos, yValue);
                }
            }
            return resultLengthMatchProfile.profile(count == result.length) ? result : Arrays.copyOf(result, count);
        }

        private int getNextValue(VectorDataLibrary lib, Object data, RandomAccessIterator rit, int pos, int oldValue) {
            int newValue = lib.getInt(data, rit, pos);
            if (!isSorted && newValue < oldValue) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                throw new IllegalArgumentException();
            }
            return newValue;
        }
    }

    @Specialization(limit = "1", guards = {"xLib.getLength(x.getData()) > 0", "yLib.getLength(y.getData()) > 0", "getLimit1Guard()"}, rewriteOn = IllegalArgumentException.class)
    protected RIntVector intersectMaybeSorted(RIntVector x, RIntVector y,
                    @CachedLibrary(value = "x.getData()") VectorDataLibrary xLib,
                    @CachedLibrary(value = "y.getData()") VectorDataLibrary yLib,
                    @Cached("create(false)") IntersectSortedNode sortSortedNode) {
        RBaseNode.reportWork(this, xLib.getLength(x.getData()) + yLib.getLength(y.getData()));

        int[] result = sortSortedNode.execute(x, y);
        return RDataFactory.createIntVector(result, xLib.isComplete(x.getData()) | yLib.isComplete(y.getData()));
    }

    @Specialization(replaces = "intersectMaybeSorted", limit = "1", guards = {"xLib.getLength(x.getData()) > 0", "yLib.getLength(y.getData()) > 0", "getLimit1Guard()"})
    protected RIntVector intersect(RIntVector x, RIntVector y,
                    @CachedLibrary(value = "x.getData()") VectorDataLibrary xLib,
                    @CachedLibrary(value = "y.getData()") VectorDataLibrary yLib,
                    @Cached("createBinaryProfile()") ConditionProfile isXSortedProfile,
                    @Cached("createBinaryProfile()") ConditionProfile isYSortedProfile,
                    @Cached("create(false)") IntersectSortedNode intersectSortedNode) {
        Object xData = x.getData();
        Object yData = y.getData();
        int xLength = xLib.getLength(xData);
        int yLength = yLib.getLength(yData);
        RandomAccessIterator xrit = xLib.randomAccessIterator(xData);
        RandomAccessIterator yrit = yLib.randomAccessIterator(yData);

        RBaseNode.reportWork(this, xLength + yLength);

        int[] result;
        if (isXSortedProfile.profile(isSorted(xLib, xData))) {
            RIntVector tempY;
            if (isYSortedProfile.profile(isSorted(yLib, yData))) {
                tempY = y;
            } else {
                int[] temp = new int[yLength];
                for (int i = 0; i < yLength; i++) {
                    temp[i] = yLib.getInt(yData, yrit, i);
                }
                sort(temp);
                tempY = RDataFactory.createIntVector(temp, yLib.isComplete(yData));
            }
            result = intersectSortedNode.execute(x, tempY);
        } else {
            result = EMPTY_INT_ARRAY;
            int maxResultLength = Math.min(xLength, yLength);
            int[] temp = new int[yLength];
            boolean[] used = new boolean[yLength];
            for (int i = 0; i < yLength; i++) {
                temp[i] = yLib.getInt(yData, yrit, i);
            }
            sort(temp);

            int count = 0;
            for (int i = 0; i < xLength; i++) {
                int value = xLib.getInt(xData, xrit, i);
                int pos = Arrays.binarySearch(temp, value);
                if (pos >= 0 && !used[pos]) {
                    used[pos] = true;
                    if (count >= result.length) {
                        result = Arrays.copyOf(result, Math.min(maxResultLength, Math.max(result.length * 2, 8)));
                    }
                    result[count++] = value;
                }
            }
            result = intersectSortedNode.resultLengthMatchProfile.profile(count == result.length) ? result : Arrays.copyOf(result, count);
        }
        return RDataFactory.createIntVector(result, xLib.isComplete(xData) | yLib.isComplete(yData));
    }

    private static boolean isSorted(VectorDataLibrary lib, Object data) {
        VectorDataLibrary.SeqIterator it = lib.iterator(data);
        if (lib.nextLoopCondition(data, it)) {
            int lastValue = lib.getNextInt(data, it);
            while (lib.nextLoopCondition(data, it)) {
                int value = lib.getNextInt(data, it);
                if (value < lastValue) {
                    return false;
                }
                lastValue = value;
            }
        }
        return true;
    }

    @TruffleBoundary
    private static void sort(int[] temp) {
        Arrays.sort(temp);
    }

    @Specialization(replaces = "intersect")
    protected Object fallback(@SuppressWarnings("unused") Object x, @SuppressWarnings("unused") Object y) {
        return null;
    }

}
