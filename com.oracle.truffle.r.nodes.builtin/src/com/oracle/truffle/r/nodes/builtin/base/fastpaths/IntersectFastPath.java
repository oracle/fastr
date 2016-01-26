/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.nodes.RFastPathNode;

public abstract class IntersectFastPath extends RFastPathNode {

    @Specialization(guards = {"x.getLength() > 0", "y.getLength() > 0"})
    protected RAbstractIntVector intersect(RAbstractIntVector x, RAbstractIntVector y, //
                    @Cached("createBinaryProfile()") ConditionProfile isSortedProfile) {
        int lastValue = x.getDataAt(0);
        boolean xSorted = true;
        for (int i = 1; i < x.getLength(); i++) {
            int value = x.getDataAt(i);
            if (value < lastValue) {
                xSorted = false;
                break;
            }
            lastValue = value;
        }
        boolean ySorted = true;
        lastValue = y.getDataAt(0);
        for (int i = 0; i < y.getLength(); i++) {
            int value = y.getDataAt(i);
            if (value < lastValue) {
                ySorted = false;
                break;
            }
            lastValue = value;
        }
        int[] result = new int[Math.min(x.getLength(), y.getLength())];
        int count = 0;
        if (isSortedProfile.profile(xSorted && ySorted)) {
            int xPos = 0;
            int yPos = 0;
            while (xPos < x.getLength() && yPos < y.getLength()) {
                int xValue = x.getDataAt(xPos);
                int yValue = y.getDataAt(yPos);
                if (xValue == yValue) {
                    result[count++] = xValue;
                    // advance over similar entries
                    while (xPos < x.getLength() - 1 && xValue == x.getDataAt(xPos + 1)) {
                        xPos++;
                        xValue = x.getDataAt(xPos);
                    }
                    xPos++;
                    yPos++;
                } else if (xValue < yValue) {
                    xPos++;
                } else {
                    yPos++;
                }
            }
        } else if (xSorted) {
            int[] temp = new int[y.getLength()];
            for (int i = 0; i < y.getLength(); i++) {
                temp[i] = y.getDataAt(i);
            }
            sort(temp);
            int xPos = 0;
            int yPos = 0;
            while (xPos < x.getLength() && yPos < y.getLength()) {
                int xValue = x.getDataAt(xPos);
                int yValue = temp[yPos];
                if (xValue == yValue) {
                    result[count++] = xValue;
                    // advance over similar entries
                    while (xPos < x.getLength() - 1 && xValue == x.getDataAt(xPos + 1)) {
                        xPos++;
                        xValue = x.getDataAt(xPos);
                    }
                    xPos++;
                    yPos++;
                } else if (xValue < yValue) {
                    xPos++;
                } else {
                    yPos++;
                }
            }
        } else {
            int[] temp = new int[y.getLength()];
            boolean[] used = new boolean[y.getLength()];
            for (int i = 0; i < y.getLength(); i++) {
                temp[i] = y.getDataAt(i);
            }
            sort(temp);

            for (int i = 0; i < x.getLength(); i++) {
                int value = x.getDataAt(i);
                int pos = Arrays.binarySearch(temp, value);
                if (pos >= 0 && !used[pos]) {
                    used[pos] = true;
                    result[count++] = value;
                }
            }
        }
        return RDataFactory.createIntVector(count == x.getLength() ? result : Arrays.copyOf(result, count), x.isComplete() || y.isComplete());
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
