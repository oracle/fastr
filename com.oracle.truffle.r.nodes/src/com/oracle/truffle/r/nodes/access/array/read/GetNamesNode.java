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
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.ops.na.*;

abstract class GetNamesNode extends BaseRNode {

    public abstract RStringVector executeNamesGet(RAbstractVector vector, Object[] positions, int currentDimLevel, Object names);

    private final NACheck namesNACheck;

    @Child private GetNamesNode getNamesNodeRecursive;

    private RStringVector getNamesRecursive(RAbstractVector vector, Object[] positions, int currentDimLevel, Object names, NACheck namesCheck) {
        if (getNamesNodeRecursive == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            getNamesNodeRecursive = insert(GetNamesNodeGen.create(namesCheck));
        }
        return getNamesNodeRecursive.executeNamesGet(vector, positions, currentDimLevel, names);
    }

    protected GetNamesNode(NACheck namesNACheck) {
        this.namesNACheck = namesNACheck;
    }

    @Specialization
    protected RStringVector getNames(RAbstractVector vector, Object[] positions, int currentDimLevel, RStringVector names) {
        return getNamesInternal(vector, positions, currentDimLevel, names);
    }

    @Specialization
    protected RStringVector getNamesNull(RAbstractVector vector, Object[] positions, int currentDimLevel, @SuppressWarnings("unused") RNull names) {
        return getNamesInternal(vector, positions, currentDimLevel, null);
    }

    private final ConditionProfile oneDimLevelProfile = ConditionProfile.createBinaryProfile();
    private final BranchProfile nonEmptyPos = BranchProfile.create();
    private final BranchProfile nullNames = BranchProfile.create();
    private final BranchProfile nonNullSrcNames = BranchProfile.create();
    private final ConditionProfile multiPosProfile = ConditionProfile.createBinaryProfile();
    private final RAttributeProfiles attrProfiles = RAttributeProfiles.create();

    RStringVector getNamesInternal(RAbstractVector vector, Object[] positions, int currentDimLevel, RStringVector names) {
        RIntVector p = (RIntVector) positions[currentDimLevel - 1];
        int numPositions = p.getLength();
        RList dimNames = vector.getDimNames(attrProfiles);
        Object srcNames = dimNames == null ? RNull.instance : (dimNames.getDataAt(currentDimLevel - 1) == RNull.instance ? RNull.instance : dimNames.getDataAt(currentDimLevel - 1));
        RStringVector newNames = null;
        if (numPositions > 0) {
            nonEmptyPos.enter();
            if (numPositions == 1 && p.getDataAt(0) == 0) {
                nullNames.enter();
                return null;
            } else if (srcNames != RNull.instance) {
                nonNullSrcNames.enter();
                newNames = AccessArrayNode.getNamesVector(srcNames, p, numPositions, namesNACheck);
            }
        }
        if (multiPosProfile.profile(numPositions > 1)) {
            return newNames;
        } else {
            if (newNames != null) {
                // the idea here is that you can get names from dimnames only if the name of of an
                // item can be unambiguously identified (there can be only one matching name in all
                // dimensions - if "name" has already been set, we might as well return null
                // already)
                if (names != null) {
                    return null;
                }
            } else {
                newNames = names;
            }
            if (oneDimLevelProfile.profile(currentDimLevel == 1)) {
                return newNames != null ? newNames : names;
            } else {
                return getNamesRecursive(vector, positions, currentDimLevel - 1, newNames == null ? RNull.instance : newNames, namesNACheck);
            }
        }
    }
}
