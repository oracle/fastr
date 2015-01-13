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
package com.oracle.truffle.r.nodes.access.array.read;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.ops.na.*;

@NodeChildren({@NodeChild(value = "dimNames", type = RNode.class), @NodeChild(value = "vec", type = RNode.class), @NodeChild(value = "pos", type = RNode.class),
                @NodeChild(value = "srcDimLevel", type = RNode.class), @NodeChild(value = "dstDimLevel", type = RNode.class)})
abstract class GetDimNamesNode extends RNode {

    public abstract Object executeDimNamesGet(VirtualFrame frame, RList dstDimNames, RAbstractVector vector, Object[] positions, int currentSrcDimLevel, int currentDstDimLevel);

    private final NACheck namesNACheck;

    @Child private GetDimNamesNode getDimNamesNodeRecursive;

    private RStringVector getDimNamesRecursive(VirtualFrame frame, RList dstDimNames, RAbstractVector vector, Object[] positions, int currentSrcDimLevel, int currentDstDimLevel, NACheck namesCheck) {
        if (getDimNamesNodeRecursive == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            getDimNamesNodeRecursive = insert(GetDimNamesNodeGen.create(namesCheck, null, null, null, null, null));
        }
        return (RStringVector) getDimNamesNodeRecursive.executeDimNamesGet(frame, dstDimNames, vector, positions, currentSrcDimLevel, currentDstDimLevel);
    }

    protected GetDimNamesNode(NACheck namesNACheck) {
        this.namesNACheck = namesNACheck;
    }

    protected GetDimNamesNode(GetDimNamesNode other) {
        this.namesNACheck = other.namesNACheck;
    }

    private final ConditionProfile multiPosProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile srcNamesNullProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile emptyPosProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile diffDimLenProfile = ConditionProfile.createBinaryProfile();

    @Specialization
    protected Object getDimNames(VirtualFrame frame, RList dstDimNames, RAbstractVector vector, Object[] positions, int currentSrcDimLevel, int currentDstDimLevel) {
        if (currentSrcDimLevel == 0) {
            return null;
        }
        RIntVector p = (RIntVector) positions[currentSrcDimLevel - 1];
        int numPositions = p.getLength();
        if (multiPosProfile.profile(numPositions > 1)) {
            RList srcDimNames = vector.getDimNames();
            RStringVector srcNames = srcDimNames == null ? null : (srcDimNames.getDataAt(currentSrcDimLevel - 1) == RNull.instance ? null
                            : (RStringVector) srcDimNames.getDataAt(currentSrcDimLevel - 1));
            if (srcNamesNullProfile.profile(srcNames == null)) {
                dstDimNames.updateDataAt(currentDstDimLevel - 1, RNull.instance, null);
            } else {
                namesNACheck.enable(!srcNames.isComplete() || !p.isComplete());
                String[] namesData = new String[numPositions];
                for (int i = 0; i < p.getLength(); i++) {
                    int pos = p.getDataAt(i);
                    if (namesNACheck.check(pos)) {
                        namesData[i] = RRuntime.STRING_NA;
                    } else {
                        namesData[i] = srcNames.getDataAt(pos - 1);
                        namesNACheck.check(namesData[i]);
                    }
                }
                RStringVector dstNames = RDataFactory.createStringVector(namesData, namesNACheck.neverSeenNA());
                dstDimNames.updateDataAt(currentDstDimLevel - 1, dstNames, null);
            }
            getDimNamesRecursive(frame, dstDimNames, vector, positions, currentSrcDimLevel - 1, currentDstDimLevel - 1, namesNACheck);
        } else {
            if (emptyPosProfile.profile(p.getDataAt(0) == 0)) {
                dstDimNames.updateDataAt(currentDstDimLevel - 1, RNull.instance, null);
                getDimNamesRecursive(frame, dstDimNames, vector, positions, currentSrcDimLevel - 1, currentDstDimLevel - 1, namesNACheck);
            } else {
                if (diffDimLenProfile.profile(currentSrcDimLevel > currentDstDimLevel)) {
                    // skip source dimensions
                    getDimNamesRecursive(frame, dstDimNames, vector, positions, currentSrcDimLevel - 1, currentDstDimLevel, namesNACheck);
                } else {
                    RList srcDimNames = vector.getDimNames();
                    RStringVector srcNames = srcDimNames == null ? null : (srcDimNames.getDataAt(currentSrcDimLevel - 1) == RNull.instance ? null
                                    : (RStringVector) srcDimNames.getDataAt(currentSrcDimLevel - 1));
                    if (srcNamesNullProfile.profile(srcNames == null)) {
                        dstDimNames.updateDataAt(currentDstDimLevel - 1, RNull.instance, null);
                    } else {
                        namesNACheck.enable(!srcNames.isComplete() || !p.isComplete());
                        String name;
                        int pos = p.getDataAt(0);
                        if (namesNACheck.check(pos)) {
                            name = RRuntime.STRING_NA;
                        } else {
                            name = srcNames.getDataAt(pos - 1);
                            namesNACheck.check(name);
                        }
                        dstDimNames.updateDataAt(currentDstDimLevel - 1, RDataFactory.createStringVector(new String[]{name}, namesNACheck.neverSeenNA()), null);
                    }
                    getDimNamesRecursive(frame, dstDimNames, vector, positions, currentSrcDimLevel - 1, currentDstDimLevel - 1, namesNACheck);
                }
            }
        }
        return null;
    }
}
