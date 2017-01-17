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
package com.oracle.truffle.r.nodes.access.vector;

import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetDimNamesAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetNamesAttributeNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;

final class PositionCharacterLookupNode extends Node {

    private final ElementAccessMode mode;
    private final int numDimensions;
    private final int dimensionIndex;
    private final BranchProfile emptyProfile = BranchProfile.create();

    @Child private SearchFirstStringNode searchNode;
    @Child private GetDimNamesAttributeNode getDimNamesNode = GetDimNamesAttributeNode.create();
    @Child private GetNamesAttributeNode getNamesNode = GetNamesAttributeNode.create();

    PositionCharacterLookupNode(ElementAccessMode mode, int numDimensions, int dimensionIndex, boolean useNAForNotFound, boolean exact) {
        this.numDimensions = numDimensions;
        this.dimensionIndex = dimensionIndex;
        this.searchNode = SearchFirstStringNode.createNode(exact, useNAForNotFound);
        this.mode = mode;
    }

    public RAbstractIntVector execute(RAbstractContainer target, RAbstractStringVector position, int notFoundStartIndex) {
        // lookup names for single dimension case
        RAbstractIntVector result;
        if (numDimensions <= 1) {
            RStringVector names = getNamesNode.getNames(target);
            if (names == null) {
                emptyProfile.enter();
                names = RDataFactory.createEmptyStringVector();
            }
            result = searchNode.apply(names, position, notFoundStartIndex, position.materialize());
        } else {
            RList dimNames = getDimNamesNode.getDimNames(target);
            if (dimNames != null) {
                Object dataAt = dimNames.getDataAt(dimensionIndex);
                if (dataAt != RNull.instance) {
                    RStringVector dimName = (RStringVector) dataAt;
                    result = searchNode.apply(dimName, position, notFoundStartIndex, null);
                } else {
                    emptyProfile.enter();
                    throw RError.error(this, Message.SUBSCRIPT_BOUNDS);
                }
            } else {
                emptyProfile.enter();
                throw noDimNames();
            }
        }
        return result;
    }

    private RError noDimNames() {
        if (mode.isSubset()) {
            return RError.error(this, Message.NO_ARRAY_DIMNAMES);
        } else {
            return RError.error(this, Message.SUBSCRIPT_BOUNDS);
        }
    }
}
