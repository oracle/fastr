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

import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetDimAttributeNode;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RTypedValue;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

/**
 * Implements common logic for accessing element of a vector which is used in RW operations:
 * {@link CachedExtractVectorNode} and {@link CachedReplaceVectorNode}.
 *
 * One of the more significant parts is getting dimensions: the built-in function dim has a
 * specialization for data.frame, i.e. data.frames have different way of getting dimensions, which
 * is reflected in {@link #loadVectorDimensions(RAbstractContainer)} method.
 */
abstract class CachedVectorNode extends RBaseNode {

    protected final ElementAccessMode mode;
    protected final RType vectorType;

    /**
     * Recursive indicates that the vector node is used inside {@link RecursiveReplaceSubscriptNode}
     * or {@link RecursiveExtractSubscriptNode}.
     */
    protected final boolean recursive;

    protected final int numberOfDimensions;
    private final int filteredPositionsLength;

    @Child private GetDimAttributeNode getDimNode = GetDimAttributeNode.create();

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
    }

    private static int initializeFilteredPositionsCount(Object[] positions) {
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

    protected final int[] loadVectorDimensions(RAbstractContainer vector) {
        // N.B. (stepan) this method used to be instance method and have special handling for
        // RDataFrame, which was removed and any test case, which would require this special
        // handling was not found (see TestBuiltin_extract_dataframe for tests used and further
        // explanation). This method and note will remain here for a while in case this behavior
        // crops up somewhere
        return getDimNode.getDimensions(vector);
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
}
