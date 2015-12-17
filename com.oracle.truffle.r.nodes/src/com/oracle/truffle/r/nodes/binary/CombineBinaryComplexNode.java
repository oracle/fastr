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

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.LoopConditionProfile;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RComplexVector;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RNull;

@SuppressWarnings("unused")
/** Takes only RNull, RComplex or RComplexVector as arguments. Use CastComplexNode to cast the operands. */
public abstract class CombineBinaryComplexNode extends CombineBinaryNode {

    @Specialization
    protected RNull combine(RNull left, RNull right) {
        return RNull.instance;
    }

    @Specialization
    protected RComplex combine(RNull left, RComplex right) {
        return right;
    }

    @Specialization
    protected RComplex combine(RComplex left, RNull right) {
        return left;
    }

    @Specialization
    protected RComplexVector combine(RComplexVector left, RNull right) {
        return left;
    }

    @Specialization
    protected RComplexVector combine(RNull left, RComplexVector right) {
        return right;
    }

    @Specialization
    protected RComplexVector combine(RComplex left, RComplex right) {
        return RDataFactory.createComplexVector(new double[]{left.getRealPart(), left.getImaginaryPart(), right.getRealPart(), right.getImaginaryPart()}, !left.isNA() && !right.isNA());
    }

    @Specialization
    protected RComplexVector combine(RComplexVector left, RComplex right, //
                    @Cached("createCountingProfile()") LoopConditionProfile profile) {
        int dataLength = left.getLength();
        double[] result = new double[(dataLength + 1) << 1];
        profile.profileCounted(dataLength);
        for (int i = 0; profile.inject(i < dataLength); i++) {
            RComplex leftValue = left.getDataAt(i);
            int index = i << 1;
            result[index] = leftValue.getRealPart();
            result[index + 1] = leftValue.getImaginaryPart();
        }
        result[dataLength << 1] = right.getRealPart();
        result[(dataLength << 1) + 1] = right.getImaginaryPart();
        return RDataFactory.createComplexVector(result, left.isComplete() && !right.isNA(), combineNames(left, false, profile));
    }

    @Specialization
    protected RComplexVector combine(RComplex left, RComplexVector right, //
                    @Cached("createCountingProfile()") LoopConditionProfile profile) {
        int dataLength = right.getLength();
        double[] result = new double[(1 + dataLength) << 1];
        result[0] = left.getRealPart();
        result[1] = left.getImaginaryPart();
        profile.profileCounted(dataLength);
        for (int i = 0; profile.inject(i < dataLength); i++) {
            int index = (i + 1) << 1;
            RComplex rightValue = right.getDataAt(i);
            result[index] = rightValue.getRealPart();
            result[index + 1] = rightValue.getImaginaryPart();
        }
        return RDataFactory.createComplexVector(result, (!left.isNA()) && right.isComplete(), combineNames(right, true, profile));
    }

    @Specialization
    protected RComplexVector combine(RComplexVector left, RComplexVector right, //
                    @Cached("createCountingProfile()") LoopConditionProfile profileLeft, //
                    @Cached("createCountingProfile()") LoopConditionProfile profileRight) {
        return (RComplexVector) genericCombine(left, right, profileLeft, profileRight);
    }
}
