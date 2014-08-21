/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

public abstract class CbindBinaryNode extends CombineBinaryNode {

    private final BranchProfile everSeenNotEqualRows = new BranchProfile();

    @SuppressWarnings("unused")
    @Specialization
    protected RNull cbind(RNull left, RNull right) {
        return RNull.instance;
    }

    @SuppressWarnings("unused")
    @Specialization
    protected RAbstractVector cbind(RAbstractVector left, RNull right) {
        return left.copyWithNewDimensions(getDimensions(left));
    }

    @SuppressWarnings("unused")
    @Specialization
    protected RAbstractVector cbind(RNull left, RAbstractVector right) {
        return right.copyWithNewDimensions(getDimensions(right));
    }

    @Specialization
    protected RAbstractVector cbind(RAbstractVector left, RAbstractVector right) {
        return genericCbind(left.materialize(), right.materialize());
    }

    public RVector genericCbind(RVector left, RVector right) {
        int leftLength = left.getLength();
        int rightLength = right.getLength();
        int[] leftDimensions = getDimensions(left);
        int[] rightDimensions = getDimensions(right);
        int[] resultDimensions = cbindDimensions(leftDimensions, rightDimensions);
        RVector result = left.createEmptySameType(resultDimensions[0] * resultDimensions[1], left.isComplete() && right.isComplete());
        boolean notEqualRows = leftDimensions[0] != rightDimensions[0];
        int i = 0;
        for (; i < leftLength; ++i) {
            result.transferElementSameType(i, left, i);
        }
        int leftEnd = leftLength;
        if (notEqualRows) {
            everSeenNotEqualRows.enter();
            int j = 0;
            for (leftEnd = resultDimensions[0] * leftDimensions[1]; i < leftEnd; ++i, Utils.incMod(j, leftLength)) {
                result.transferElementSameType(i, left, j);
            }
            boolean notMultiple = j != 0;
            if (notMultiple) {
                // TODO warning
            }
        }
        for (; i < leftEnd + rightLength; ++i) {
            result.transferElementSameType(i, right, i - leftEnd);
        }
        if (notEqualRows) {
            everSeenNotEqualRows.enter();
            int j = 0;
            for (int resultEnd = resultDimensions[0] * resultDimensions[1]; i < resultEnd; ++i, Utils.incMod(j, leftLength)) {
                result.transferElementSameType(i, right, j);
            }
            boolean notMultiple = j != 0;
            if (notMultiple) {
                // TODO warning
            }
        }
        return (RVector) result.copyWithNewDimensions(resultDimensions);
    }

    private static int[] getDimensions(RAbstractVector vector) {
        int[] dimensions = vector.getDimensions();
        if (dimensions == null || dimensions.length != 2) {
            return new int[]{vector.getLength(), 1};
        } else {
            assert dimensions.length == 2;
            return dimensions;
        }
    }

    private static int[] cbindDimensions(int[] leftDimensions, int[] rightDimensions) {
        assert leftDimensions != null && leftDimensions.length == 2;
        assert rightDimensions != null && rightDimensions.length == 2;
        return new int[]{Math.max(leftDimensions[0], rightDimensions[0]), leftDimensions[1] + rightDimensions[1]};
    }
}
