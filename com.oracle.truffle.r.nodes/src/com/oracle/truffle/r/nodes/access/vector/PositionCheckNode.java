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
package com.oracle.truffle.r.nodes.access.vector;

import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.nodes.access.vector.PositionsCheckNode.PositionProfile;
import com.oracle.truffle.r.nodes.control.RLengthNode;
import com.oracle.truffle.r.nodes.profile.VectorLengthProfile;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RTypedValue;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

abstract class PositionCheckNode extends Node {

    private final Class<?> positionClass;
    private final int dimensionIndex;
    protected final int numDimensions;
    private final VectorLengthProfile positionLengthProfile = VectorLengthProfile.create();
    protected final BranchProfile error = BranchProfile.create();
    protected final boolean replace;
    protected final RType containerType;
    @Child private PositionCastNode castNode;
    @Child private RLengthNode positionLengthNode = RLengthNode.create();
    @Child private PositionCharacterLookupNode characterLookup;

    PositionCheckNode(ElementAccessMode mode, RType containerType, Object positionValue, int dimensionIndex, int numDimensions, boolean exact, boolean replace) {
        this.positionClass = positionValue.getClass();
        this.dimensionIndex = dimensionIndex;
        this.numDimensions = numDimensions;
        this.replace = replace;
        this.containerType = containerType;
        this.castNode = PositionCastNode.create(mode, replace);
        if (positionValue instanceof String || positionValue instanceof RAbstractStringVector) {
            boolean useNAForNotFound = !replace && isListLike(containerType) && mode.isSubscript();
            characterLookup = new PositionCharacterLookupNode(mode, numDimensions, dimensionIndex, useNAForNotFound, exact);
        }
    }

    protected static boolean isListLike(RType type) {
        switch (type) {
            case Language:
            case DataFrame:
            case Expression:
            case PairList:
            case List:
                return true;
        }
        return false;
    }

    public boolean isIgnoreDimension() {
        return positionClass == RMissing.class;
    }

    public Class<?> getPositionClass() {
        return positionClass;
    }

    public final boolean isSupported(Object object) {
        return object.getClass() == positionClass;
    }

    public static PositionCheckNode createNode(ElementAccessMode mode, RType containerType, Object position, int positionIndex, int numDimensions, boolean exact, boolean replace, boolean recursive) {
        if (mode.isSubset()) {
            return PositionCheckSubsetNodeGen.create(mode, containerType, position, positionIndex, numDimensions, exact, replace);
        } else {
            return PositionCheckSubscriptNodeGen.create(mode, containerType, position, positionIndex, numDimensions, exact, replace, recursive);
        }
    }

    protected boolean isMultiDimension() {
        return numDimensions > 1;
    }

    public final Object execute(PositionProfile profile, RAbstractContainer vector, int[] vectorDimensions, int vectorLength, Object position) {
        Object castPosition = castNode.execute(positionClass.cast(position));

        int dimensionLength;
        if (numDimensions == 1) {
            dimensionLength = vectorLength;
        } else {
            assert vectorDimensions != null;
            assert vectorDimensions.length == numDimensions;
            dimensionLength = vectorDimensions[dimensionIndex];
        }

        if (characterLookup != null) {
            castPosition = characterLookup.execute(vector, (RAbstractStringVector) castPosition, dimensionLength);
        }

        RTypedValue positionVector = (RTypedValue) profilePosition(castPosition);

        int positionLength;
        if (positionVector instanceof RMissing) {
            positionLength = -1;
        } else {
            positionLength = positionLengthProfile.profile(((RAbstractVector) positionVector).getLength());
        }

        assert isValidCastedType(positionVector) : "result type of a position cast node must be integer or logical";

        return execute(profile, dimensionLength, positionVector, positionLength);
    }

    private final ValueProfile castedValue = ValueProfile.createClassProfile();

    private Object profilePosition(Object positionVector) {
        return castedValue.profile(positionVector);
    }

    private static boolean isValidCastedType(RTypedValue positionVector) {
        RType type = positionVector.getRType();
        return type == RType.Integer || type == RType.Logical || type == RType.Character || type == RType.Double || type == RType.Null;
    }

    public abstract Object execute(PositionProfile statistics, int dimensionLength, Object position, int positionLength);

}
