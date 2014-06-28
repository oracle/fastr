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

public abstract class RbindBinaryNode extends CombineBinaryNode {

    private BranchProfile everSeenNotEqualCols = new BranchProfile();

    @SuppressWarnings("unused")
    @Specialization
    public RNull rbind(RNull left, RNull right) {
        return RNull.instance;
    }

    @SuppressWarnings("unused")
    @Specialization(order = 3)
    public RAbstractVector rbind(RAbstractVector left, RNull right) {
        return left.copyWithNewDimensions(getDimensions(left));
    }

    @SuppressWarnings("unused")
    @Specialization(order = 4)
    public RAbstractVector rbind(RNull left, RAbstractVector right) {
        return right.copyWithNewDimensions(getDimensions(right));
    }

    @Specialization(order = 100)
    public RAbstractVector rbind(RAbstractVector left, RAbstractVector right) {
        return genericRbind(left.materialize(), right.materialize());
    }

    public RVector genericRbind(RVector left, RVector right) {
        int leftLength = left.getLength();
        int rightLength = right.getLength();
        int[] leftDimensions = getDimensions(left);
        int[] rightDimensions = getDimensions(right);
        int[] resultDimensions = rbindDimensions(leftDimensions, rightDimensions);
        final int rows = resultDimensions[0];
        final int cols = resultDimensions[1];
        RVector result = left.createEmptySameType(rows * cols, left.isComplete() && right.isComplete());
        boolean notEqualCols = leftDimensions[1] != rightDimensions[1];

        if (cols > leftLength && cols % leftLength != 0) {
            RError.warning(getEncapsulatingSourceSection(), RError.Message.RBIND_COLUMNS_NOT_MULTIPLE);
        }

        // initial copy of left to result
        int rRow = 0;
        int rCol = 0;
        for (int iLeft = 0; iLeft < leftLength; ++iLeft) {
            result.transferElementSameType(rCol * rows + rRow, left, iLeft);
            rRow = Utils.incMod(rRow, leftDimensions[0]);
            if (rRow == 0) {
                rCol = Utils.incMod(rCol, leftDimensions[1]);
            }
        }

        // fill up from left into remaining columns if required
        if (notEqualCols) {
            everSeenNotEqualCols.enter();
            rRow = 0;
            rCol = 0;
            for (int iLeft = 0; rCol + leftDimensions[1] < cols && rRow < leftDimensions[0]; iLeft = Utils.incMod(iLeft, leftLength)) {
                result.transferElementSameType((rCol + leftDimensions[1]) * rows + rRow, left, iLeft);
                rRow = Utils.incMod(rRow, leftDimensions[0]);
                if (rRow == 0) {
                    ++rCol;
                }
            }
        }

        // initial copy of right to result
        rRow = 0;
        rCol = 0;
        for (int iRight = 0; iRight < rightLength; ++iRight) {
            result.transferElementSameType(rCol * rows + leftDimensions[0] + rRow, right, iRight);
            rRow = Utils.incMod(rRow, rightDimensions[0]);
            if (rRow == 0) {
                rCol = Utils.incMod(rCol, rightDimensions[1]);
            }
        }

        // fill up from right into remaining columns if required
        if (notEqualCols) {
            everSeenNotEqualCols.enter();
            rRow = 0;
            rCol = 0;
            for (int iRight = 0; rCol + rightDimensions[1] < cols && rRow + leftDimensions[0] < rows; iRight = Utils.incMod(iRight, rightLength)) {
                result.transferElementSameType((rCol + rightDimensions[1]) * rows + leftDimensions[0] + rRow, right, iRight);
                rRow = Utils.incMod(rRow, rightDimensions[0]);
                if (rRow == 0) {
                    ++rCol;
                }
            }
        }

        return (RVector) result.copyWithNewDimensions(resultDimensions);
    }

    private static int[] getDimensions(RAbstractVector vector) {
        int[] dimensions = vector.getDimensions();
        if (dimensions == null || dimensions.length != 2) {
            return new int[]{1, vector.getLength()};
        } else {
            assert dimensions.length == 2;
            return dimensions;
        }
    }

    private static int[] rbindDimensions(int[] leftDimensions, int[] rightDimensions) {
        assert leftDimensions != null && leftDimensions.length == 2;
        assert rightDimensions != null && rightDimensions.length == 2;
        return new int[]{effectiveDimension(leftDimensions, 0) + effectiveDimension(rightDimensions, 0), Math.max(leftDimensions[1], rightDimensions[1])};
    }

    private static int effectiveDimension(int[] dimensions, int index) {
        for (int i = 0; i < dimensions.length; i++) {
            if (dimensions[i] == 0) {
                return 0;
            }
        }
        return dimensions[index];
    }
}
