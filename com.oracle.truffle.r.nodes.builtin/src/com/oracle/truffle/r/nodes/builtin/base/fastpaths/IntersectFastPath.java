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

import java.util.Arrays;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;
import com.oracle.truffle.r.runtime.nodes.RFastPathNode;

public abstract class IntersectFastPath extends RFastPathNode {

    private static final int[] EMPTY_INT_ARRAY = new int[0];

    protected static final class IntersectSortedNode extends Node {

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

        private int[] execute(RAbstractIntVector x, int xLength, int yLength, RAbstractIntVector y) {
            int[] result = EMPTY_INT_ARRAY;
            int maxResultLength = Math.min(xLength, yLength);
            int count = 0;
            int xPos = 0;
            int yPos = 0;
            int xValue = x.getDataAt(xPos);
            int yValue = y.getDataAt(yPos);
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
                        int nextValue = x.getDataAt(xPos + 1);
                        if (exit2Profile.profile(xValue != nextValue)) {
                            break;
                        }
                        xPos++;
                        xValue = nextValue;
                    }
                    if (exit3Profile.profile(++xPos >= xLength) || exit4Profile.profile(++yPos >= yLength)) {
                        break;
                    }
                    xValue = getNextValue(x, xPos, xValue);
                    yValue = getNextValue(y, yPos, yValue);
                } else if (valueSmallerProfile.profile(xValue < yValue)) {
                    if (exit5Profile.profile(++xPos >= xLength)) {
                        break;
                    }
                    xValue = getNextValue(x, xPos, xValue);
                } else {
                    if (exit6Profile.profile(++yPos >= yLength)) {
                        break;
                    }
                    yValue = getNextValue(y, yPos, yValue);
                }
            }
            if (!isSorted) {
                // check remaining array entries
                while (++xPos < xLength) {
                    xValue = getNextValue(x, xPos, xValue);
                }
                while (++yPos < yLength) {
                    yValue = getNextValue(y, yPos, yValue);
                }
            }
            return resultLengthMatchProfile.profile(count == result.length) ? result : Arrays.copyOf(result, count);
        }

        private int getNextValue(RAbstractIntVector vector, int pos, int oldValue) {
            int newValue = vector.getDataAt(pos);
            if (!isSorted && newValue < oldValue) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                throw new IllegalArgumentException();
            }
            return newValue;
        }
    }

    protected static int length(RAbstractIntVector v, Class<? extends RAbstractIntVector> clazz) {
        return clazz.cast(v).getLength();
    }

    protected static IntersectSortedNode createMaybeSorted() {
        return new IntersectSortedNode(false);
    }

    @Specialization(limit = "1", guards = {"x.getClass() == cached.xClass", "y.getClass() == cached.yClass", "length(x, cached.xClass) > 0",
                    "length(y, cached.yClass) > 0"}, rewriteOn = IllegalArgumentException.class)
    protected RAbstractIntVector intersectMaybeSorted(RAbstractIntVector x, RAbstractIntVector y,
                    @Cached("new(x.getClass(), y.getClass())") IntersectMaybeSortedNode cached) {
        // apply the type profiles:
        RAbstractIntVector profiledX = cached.xClass.cast(x);
        RAbstractIntVector profiledY = cached.yClass.cast(y);

        int xLength = profiledX.getLength();
        int yLength = profiledY.getLength();
        RBaseNode.reportWork(this, xLength + yLength);

        int[] result = cached.intersect.execute(profiledX, xLength, yLength, profiledY);
        return RDataFactory.createIntVector(result, profiledX.isComplete() | profiledY.isComplete());
    }

    public static class IntersectMaybeSortedNode extends Node {
        public final Class<? extends RAbstractIntVector> xClass;
        public final Class<? extends RAbstractIntVector> yClass;
        @Child IntersectSortedNode intersect;

        public IntersectMaybeSortedNode(Class<? extends RAbstractIntVector> xClass, Class<? extends RAbstractIntVector> yClass) {
            this.xClass = xClass;
            this.yClass = yClass;
            this.intersect = createMaybeSorted();
        }
    }

    protected static IntersectSortedNode createSorted() {
        return new IntersectSortedNode(true);
    }

    @Specialization(limit = "1", guards = {"x.getClass() == cached.xClass", "y.getClass() == cached.yClass", "length(x, cached.xClass) > 0", "length(y, cached.yClass) > 0"})
    protected RAbstractIntVector intersect(RAbstractIntVector x, RAbstractIntVector y,
                    @Cached("new(x.getClass(), y.getClass())") IntersectNode cached) {
        // apply the type profiles:
        RAbstractIntVector profiledX = cached.xClass.cast(x);
        RAbstractIntVector profiledY = cached.yClass.cast(y);

        int xLength = profiledX.getLength();
        int yLength = profiledY.getLength();
        RBaseNode.reportWork(this, xLength + yLength);

        int[] result;
        if (cached.isXSortedProfile.profile(isSorted(profiledX))) {
            RAbstractIntVector tempY;
            if (cached.isYSortedProfile.profile(isSorted(profiledY))) {
                tempY = profiledY;
            } else {
                int[] temp = new int[yLength];
                for (int i = 0; i < yLength; i++) {
                    temp[i] = profiledY.getDataAt(i);
                }
                sort(temp);
                tempY = RDataFactory.createIntVector(temp, profiledY.isComplete());
            }
            result = cached.intersect.execute(profiledX, xLength, yLength, tempY);
        } else {
            result = EMPTY_INT_ARRAY;
            int maxResultLength = Math.min(xLength, yLength);
            int[] temp = new int[yLength];
            boolean[] used = new boolean[yLength];
            for (int i = 0; i < yLength; i++) {
                temp[i] = profiledY.getDataAt(i);
            }
            sort(temp);

            int count = 0;
            for (int i = 0; i < xLength; i++) {
                int value = profiledX.getDataAt(i);
                int pos = Arrays.binarySearch(temp, value);
                if (pos >= 0 && !used[pos]) {
                    used[pos] = true;
                    if (count >= result.length) {
                        result = Arrays.copyOf(result, Math.min(maxResultLength, Math.max(result.length * 2, 8)));
                    }
                    result[count++] = value;
                }
            }
            result = cached.resultLengthMatchProfile.profile(count == result.length) ? result : Arrays.copyOf(result, count);
        }
        return RDataFactory.createIntVector(result, profiledX.isComplete() | profiledY.isComplete());
    }

    public static class IntersectNode extends Node {
        public final Class<? extends RAbstractIntVector> xClass;
        public final Class<? extends RAbstractIntVector> yClass;
        final ConditionProfile isXSortedProfile = ConditionProfile.createBinaryProfile();
        final ConditionProfile isYSortedProfile = ConditionProfile.createBinaryProfile();
        final ConditionProfile resultLengthMatchProfile = ConditionProfile.createBinaryProfile();
        @Child IntersectSortedNode intersect;

        public IntersectNode(Class<? extends RAbstractIntVector> xClass, Class<? extends RAbstractIntVector> yClass) {
            this.xClass = xClass;
            this.yClass = yClass;
            this.intersect = createMaybeSorted();
        }
    }

    private static boolean isSorted(RAbstractIntVector vector) {
        int length = vector.getLength();
        int lastValue = vector.getDataAt(0);
        for (int i = 1; i < length; i++) {
            int value = vector.getDataAt(i);
            if (value < lastValue) {
                return false;
            }
            lastValue = value;
        }
        return true;
    }

    @TruffleBoundary
    private static void sort(int[] temp) {
        Arrays.sort(temp);
    }

    @Fallback
    protected Object fallback(@SuppressWarnings("unused") Object x, @SuppressWarnings("unused") Object y) {
        return null;
    }

}
