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

import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.ops.na.*;

public abstract class CombineBinaryNode extends BinaryNode {

    private final NACheck naCheck = NACheck.create();
    private final BranchProfile hasNamesProfile = BranchProfile.create();

    public abstract Object executeCombine(VirtualFrame frame, Object left, Object right);

    protected RStringVector combineNames(RAbstractVector orgVector, boolean prependEmpty) {
        if (orgVector.getNames() == null || orgVector.getNames() == RNull.instance) {
            return null;
        }
        hasNamesProfile.enter();
        RStringVector orgNames = (RStringVector) orgVector.getNames();
        int orgLength = orgVector.getLength();
        String[] namesData = new String[orgLength + 1];
        int i = 0;
        int to = namesData.length - 1;
        if (prependEmpty) {
            namesData[i++] = RRuntime.NAMES_ATTR_EMPTY_VALUE;
            to++;
        }
        naCheck.enable(orgNames);
        for (int j = 0; i < to; i++, j++) {
            namesData[i] = orgNames.getDataAt(j);
            naCheck.check(namesData[i]);
        }
        if (!prependEmpty) {
            namesData[i] = RRuntime.NAMES_ATTR_EMPTY_VALUE;
        }
        return RDataFactory.createStringVector(namesData, naCheck.neverSeenNA());
    }

    protected RStringVector combineNames(RAbstractVector left, RAbstractVector right) {
        Object leftNames = left.getNames();
        Object rightNames = right.getNames();
        if ((leftNames == null || leftNames == RNull.instance) && (rightNames == null || rightNames == RNull.instance)) {
            return null;
        }
        hasNamesProfile.enter();
        int leftLength = left.getLength();
        int rightLength = right.getLength();
        String[] namesData = new String[leftLength + rightLength];
        naCheck.enable((leftNames != null && leftNames != RNull.instance && !((RStringVector) leftNames).isComplete()) ||
                        (rightNames != null && rightNames != RNull.instance && !((RStringVector) rightNames).isComplete()));
        if (leftNames == null || leftNames == RNull.instance) {
            for (int i = 0; i < leftLength; ++i) {
                namesData[i] = RRuntime.NAMES_ATTR_EMPTY_VALUE;
            }
        } else {
            for (int i = 0; i < leftLength; ++i) {
                namesData[i] = ((RStringVector) leftNames).getDataAt(i);
                naCheck.check(namesData[i]);
            }
        }
        if (rightNames == null || rightNames == RNull.instance) {
            for (int i = 0, j = leftLength; i < rightLength; ++i, ++j) {
                namesData[j] = RRuntime.NAMES_ATTR_EMPTY_VALUE;
            }
        } else {
            for (int i = 0, j = leftLength; i < rightLength; ++i, ++j) {
                namesData[j] = ((RStringVector) rightNames).getDataAt(i);
                naCheck.check(namesData[j]);
            }
        }
        return RDataFactory.createStringVector(namesData, naCheck.neverSeenNA());
    }

    public RVector genericCombine(RVector left, RVector right) {
        int leftLength = left.getLength();
        int rightLength = right.getLength();
        RVector result = left.createEmptySameType(leftLength + rightLength, left.isComplete() && right.isComplete());
        int i = 0;
        for (; i < leftLength; ++i) {
            result.transferElementSameType(i, left, i);
        }
        for (; i < leftLength + rightLength; ++i) {
            result.transferElementSameType(i, right, i - leftLength);
        }
        RStringVector names = combineNames(left, right);
        if (names != null) {
            result.setNames(names);
        }
        return result;
    }
}
