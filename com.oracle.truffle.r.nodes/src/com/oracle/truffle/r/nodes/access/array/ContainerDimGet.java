/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.access.array;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

// TODO: this should not be necessary once data frame access operators are implemented in R
// which likely makes potential refactoring of this code redundant
@NodeChild(value = "op")
public abstract class ContainerDimGet extends RNode {

    public abstract Object execute(VirtualFrame frame, RAbstractContainer container);

    private final ConditionProfile nameConditionProfile = ConditionProfile.createBinaryProfile();
    private final BranchProfile naValueMet = BranchProfile.create();
    private final BranchProfile intVectorMet = BranchProfile.create();

    @Specialization(guards = "!isDataFrame")
    Object getDim(RAbstractContainer container) {
        return container.getDimensions();
    }

    @Specialization
    Object getDimDataFrame(RDataFrame container) {
        // this largely reproduces code from ShortRowNames
        Object rowNames = container.getRowNames();
        if (nameConditionProfile.profile(rowNames == RNull.instance)) {
            return new int[]{0, container.getLength()};
        } else {
            return new int[]{Math.abs(calculateN((RAbstractVector) rowNames)), container.getLength()};
        }
    }

    private int calculateN(RAbstractVector rowNames) {
        if (rowNames.getElementClass() == RInt.class && rowNames.getLength() == 2) {
            RAbstractIntVector rowNamesIntVector = (RAbstractIntVector) rowNames;
            intVectorMet.enter();
            if (RRuntime.isNA(rowNamesIntVector.getDataAt(0))) {
                naValueMet.enter();
                return rowNamesIntVector.getDataAt(1);
            }
        }
        return rowNames.getLength();
    }

    protected boolean isDataFrame(RAbstractContainer container) {
        return container.getElementClass() == RDataFrame.class;
    }

}
