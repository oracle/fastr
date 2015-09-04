/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.binary;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.profile.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@SuppressWarnings("unused")
/** Takes only RNull, int or RIntVector as arguments. Use CastIntegerNode to cast the operands. */
public abstract class CombineBinaryIntegerNode extends CombineBinaryNode {

    @Specialization
    protected RNull combine(RNull left, RNull right) {
        return RNull.instance;
    }

    @Specialization
    protected RAbstractIntVector combine(RNull left, RAbstractIntVector right) {
        return right;
    }

    @Specialization
    protected RAbstractIntVector combine(RAbstractIntVector left, RNull right) {
        return left;
    }

    @Specialization
    protected RIntVector combine(int left, int right) {
        return RDataFactory.createIntVector(new int[]{left, right}, RRuntime.isComplete(left) && RRuntime.isComplete(right));
    }

    @Specialization
    protected RIntVector performAbstractIntVectorInt(RAbstractIntVector left, int right, //
                    @Cached("create()") CountedLoopConditionProfile profile) {
        int dataLength = left.getLength();
        int[] result = new int[dataLength + 1];
        profile.profileLength(dataLength);
        for (int i = 0; profile.inject(i < dataLength); i++) {
            result[i] = left.getDataAt(i);
        }
        result[dataLength] = right;
        return RDataFactory.createIntVector(result, left.isComplete() && RRuntime.isComplete(right), combineNames(left, false, profile));
    }

    @Specialization
    protected RIntVector performIntAbstractIntVector(int left, RAbstractIntVector right, //
                    @Cached("create()") CountedLoopConditionProfile profile) {
        int dataLength = right.getLength();
        int[] result = new int[dataLength + 1];
        profile.profileLength(dataLength);
        for (int i = 0; profile.inject(i < dataLength); i++) {
            result[i + 1] = right.getDataAt(i);
        }
        result[0] = left;
        return RDataFactory.createIntVector(result, RRuntime.isComplete(left) && right.isComplete(), combineNames(right, true, profile));
    }

    @Specialization
    protected RIntVector combine(RAbstractIntVector left, RAbstractIntVector right, //
                    @Cached("create()") CountedLoopConditionProfile leftProfile, //
                    @Cached("create()") CountedLoopConditionProfile rightProfile) {
        int leftLength = left.getLength();
        int rightLength = right.getLength();
        int[] result = new int[leftLength + rightLength];
        int i = 0;
        leftProfile.profileLength(leftLength);
        for (; leftProfile.inject(i < leftLength); i++) {
            result[i] = left.getDataAt(i);
        }
        rightProfile.profileLength(rightLength);
        for (; rightProfile.inject(i < leftLength + rightLength); i++) {
            result[i] = right.getDataAt(i - leftLength);
        }
        return RDataFactory.createIntVector(result, left.isComplete() && right.isComplete(), combineNames(left, right, leftProfile, rightProfile));
    }
}
