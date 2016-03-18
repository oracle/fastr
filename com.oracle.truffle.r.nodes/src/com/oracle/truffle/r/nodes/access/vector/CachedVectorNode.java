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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.RAttributeProfiles;
import com.oracle.truffle.r.runtime.data.RDataFrame;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RTypedValue;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

abstract class CachedVectorNode extends RBaseNode {

    protected final ElementAccessMode mode;
    protected final RType vectorType;

    /**
     * Recursive indicates that the vector node is used inside {@link RecursiveReplaceSubscriptNode}
     * or {@link RecursiveExtractSubscriptNode}.
     */
    protected final boolean recursive;

    protected final BranchProfile errorBranch = BranchProfile.create();
    protected final int numberOfDimensions;
    private final int filteredPositionsLength;

    @Child private GetDataFrameDimensionNode getDataFrameDimension;

    // if this is non-null, the node needs to throw the error whenever it is executed
    @CompilationFinal protected Runnable error;

    CachedVectorNode(ElementAccessMode mode, RTypedValue vector, Object[] positions, boolean recursive) {
        this.mode = mode;
        this.vectorType = vector.getRType();
        this.recursive = recursive;
        this.filteredPositionsLength = initializeFilteredPositionsCount(positions);
        if (filteredPositionsLength == -1) {
            this.numberOfDimensions = positions.length;
        } else {
            this.numberOfDimensions = filteredPositionsLength;
        }
        if (!isSubsetable(vectorType)) {
            error = () -> {
                throw RError.error(this, RError.Message.OBJECT_NOT_SUBSETTABLE, vectorType.getName());
            };
        }
    }

    private int initializeFilteredPositionsCount(Object[] positions) {
        int dimensions = 0;
        for (int i = 0; i < positions.length; i++) {
            // for cases like RMissing the position does not contribute to the number of dimensions
            if (!isRemovePosition(positions[i])) {
                dimensions++;
            }
        }
        if (positions.length == dimensions || dimensions <= 0) {
            return -1;
        } else {
            return dimensions;
        }
    }

    protected Object[] filterPositions(Object[] positions) {
        /*
         * we assume that the positions count cannot change as the isRemovePosition check is just
         * based on types and therefore does not change per position instance.
         *
         * normally empty positions are just empty but with S3 function dispatch it may happen that
         * positions are also of type RMissing. These values should not contribute to the access
         * dimensions.
         *
         * Example test case: dimensions.x<-data.frame(c(1,2), c(3,4)); x[1]
         */
        assert initializeFilteredPositionsCount(positions) == filteredPositionsLength;
        if (filteredPositionsLength != -1) {
            Object[] newPositions = new Object[filteredPositionsLength];
            int newPositionIndex = 0;
            for (int i = 0; i < positions.length; i++) {
                Object position = positions[i];
                if (!isRemovePosition(position)) {
                    newPositions[newPositionIndex++] = position;
                }
            }
            return newPositions;
        }
        return positions;
    }

    private static boolean isRemovePosition(Object position) {
        return position instanceof RMissing;
    }

    protected static boolean logicalAsBoolean(RTypedValue cast, boolean defaultValue) {
        if (cast instanceof RMissing) {
            return defaultValue;
        } else {
            RAbstractLogicalVector logical = (RAbstractLogicalVector) cast;
            if (logical.getLength() == 0) {
                return defaultValue;
            } else {
                return logical.getDataAt(0) == RRuntime.LOGICAL_TRUE;
            }
        }
    }

    private static boolean isSubsetable(RType type) {
        if (type.isVector()) {
            return true;
        }
        switch (type) {
            case Null:
            case Language:
            case Factor:
            case PairList:
            case Environment:
            case Expression:
                return true;
            default:
                return false;
        }
    }

    protected final int[] loadVectorDimensions(RAbstractContainer vector) {
        if (vector instanceof RDataFrame) {
            // TODO (chumer) its unfortunate that we need this hack for the data frame dimensions
            // maybe we can rid of this as soon as a data frame is of the same type as a list.
            if (getDataFrameDimension == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getDataFrameDimension = insert(new GetDataFrameDimensionNode());
            }
            return getDataFrameDimension.calculateFrameDimensions((RDataFrame) vector);
        } else {
            return vector.getDimensions();
        }
    }

    public ElementAccessMode getMode() {
        return mode;
    }

    protected String tryCastSingleString(PositionsCheckNode check, Object[] positions) {
        if (numberOfDimensions > 1) {
            return null;
        }

        String positionString = null;
        Object position = check.getPositionCheckAt(0).getPositionClass().cast(positions[0]);
        if (position instanceof String) {
            positionString = (String) position;
        } else if (position instanceof RAbstractStringVector) {
            RAbstractStringVector vector = (RAbstractStringVector) position;
            if (vector.getLength() == 1) {
                positionString = vector.getDataAt(0);
            }
        }
        return positionString;
    }

    private final class GetDataFrameDimensionNode extends Node {

        private final ConditionProfile compressedProfile = ConditionProfile.createBinaryProfile();
        private final RAttributeProfiles attrProfiles = RAttributeProfiles.create();
        private final ValueProfile rowNamesProfile = ValueProfile.createClassProfile();
        private final ConditionProfile intVecProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile nameConditionProfile = ConditionProfile.createBinaryProfile();

        public int[] calculateFrameDimensions(RDataFrame container) {
            // this largely reproduces code from ShortRowNames
            Object rowNames = container.getRowNames(attrProfiles);
            if (nameConditionProfile.profile(rowNames == RNull.instance)) {
                return new int[]{0, container.getLength()};
            } else {
                return new int[]{Math.abs(calculateN((RAbstractVector) rowNames)), container.getLength()};
            }
        }

        private int calculateN(RAbstractVector rowNames) {
            RAbstractVector profiledRowNames = rowNamesProfile.profile(rowNames);
            if (intVecProfile.profile(profiledRowNames.getRType() == RType.Integer && profiledRowNames.getLength() == 2)) {
                RAbstractIntVector rowNamesIntVector = (RAbstractIntVector) profiledRowNames;
                if (compressedProfile.profile(RRuntime.isNA(rowNamesIntVector.getDataAt(0)))) {
                    return rowNamesIntVector.getDataAt(1);
                } else {
                    return profiledRowNames.getLength();
                }
            } else {
                return profiledRowNames.getLength();
            }
        }
    }
}
