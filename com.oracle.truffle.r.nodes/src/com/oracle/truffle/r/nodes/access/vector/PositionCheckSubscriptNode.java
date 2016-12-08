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

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.access.vector.PositionsCheckNode.PositionProfile;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetNamesAttributeNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.RAttributeProfiles;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RInteger;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

abstract class PositionCheckSubscriptNode extends PositionCheckNode {

    private final NACheck positionNACheck = NACheck.create();
    private final ConditionProfile greaterZero = ConditionProfile.createBinaryProfile();

    private final boolean recursive;

    private final RAttributeProfiles attributeProfile = RAttributeProfiles.create();

    PositionCheckSubscriptNode(ElementAccessMode mode, RType containerType, Object positionValue, int dimensionIndex, int numDimensions, boolean exact, boolean assignment, boolean recursive) {
        super(mode, containerType, positionValue, dimensionIndex, numDimensions, exact, assignment);
        this.recursive = recursive;
    }

    @SuppressWarnings("unused")
    @Specialization
    protected Object doMissing(PositionProfile statistics, int dimSize, RMissing position, int positionLength) {
        statistics.selectedPositionsCount = dimSize;
        return position;
    }

    @Specialization
    protected RAbstractVector doLogical(PositionProfile statistics, int dimSize, RAbstractLogicalVector position, int positionLength,
                    @Cached("create()") GetNamesAttributeNode getNamesNode) {
        positionNACheck.enable(position);
        byte value = position.getDataAt(0);
        if (positionLength != 1) {
            error.enter();
            if (positionLength >= 3) {
                throw RError.error(this, RError.Message.SELECT_MORE_1);
            } else {
                if (value == RRuntime.LOGICAL_TRUE) {
                    throw RError.error(this, RError.Message.SELECT_MORE_1);
                } else {
                    throw RError.error(this, RError.Message.SELECT_LESS_1);
                }
            }
        }

        return doIntegerImpl(statistics, dimSize, positionNACheck.convertLogicalToInt(value), position, getNamesNode);
    }

    @Specialization
    protected RAbstractVector doInteger(PositionProfile profile, int dimSize, RAbstractIntVector position, int positionLength,
                    @Cached("create()") GetNamesAttributeNode getNamesNode) {
        if (positionLength != 1) {
            error.enter();
            Message message;
            if (positionLength > 1) {
                /* This is a very specific check. But it just did not fit into other checks. */
                if (positionLength == 2 && position.getDataAt(0) == 0) {
                    message = RError.Message.SELECT_LESS_1;
                } else {
                    message = RError.Message.SELECT_MORE_1;
                }
            } else {
                message = RError.Message.SELECT_LESS_1;
            }
            throw RError.error(this, message);
        }
        assert positionLength == 1;
        positionNACheck.enable(position);
        RAbstractVector result = doIntegerImpl(profile, dimSize, position.getDataAt(0), position, getNamesNode);
        return result;

    }

    private RAbstractVector doIntegerImpl(PositionProfile profile, int dimSize, int value, RAbstractVector originalVector, GetNamesAttributeNode getNamesNode) {
        int result;
        if (greaterZero.profile(value > 0)) {
            // fast path
            assert RRuntime.INT_NA <= 0;
            result = value;
            if (!replace && result > dimSize) {
                error.enter();
                throwBoundsError();
            }
            profile.maxOutOfBoundsIndex = result;
        } else {
            // slow path
            result = doIntegerSlowPath(profile, dimSize, value);
        }
        profile.selectedPositionsCount = 1;

        RStringVector names = getNamesNode.getNames(originalVector);
        if (names != null) {
            return RDataFactory.createIntVector(new int[]{result}, !profile.containsNA, names);
        } else {
            return RInteger.valueOf(result);
        }
    }

    private int doIntegerSlowPath(PositionProfile profile, int dimSize, int value) {
        positionNACheck.enable(value);
        if (positionNACheck.check(value)) {
            handleNA(dimSize);
            profile.containsNA = true;
            return value;
        } else {
            if (dimSize == 2) {
                if (value == -2) {
                    return 1;
                } else if (value == -1) {
                    return 2;
                }
            }
            error.enter();
            int selected = 0;
            if (value > 0) {
                selected = 1;
            } else if (value < 0) {
                if (-value <= dimSize) {
                    selected = dimSize - 1;
                } else {
                    selected = dimSize;
                }
            }
            if (selected <= 1) {
                throw RError.error(this, RError.Message.SELECT_LESS_1);
            } else {
                throw RError.error(this, RError.Message.SELECT_MORE_1);
            }

        }
    }

    private void handleNA(int dimSize) {
        if (replace) {
            error.enter();
            Message message;
            if (isMultiDimension()) {
                message = RError.Message.SUBSCRIPT_BOUNDS_SUB;
            } else {
                if (dimSize < 2) {
                    message = RError.Message.SELECT_LESS_1;
                } else {
                    message = RError.Message.SELECT_MORE_1;
                }
            }
            throw RError.error(this, message);
        } else {
            if (numDimensions == 1 && isListLike(containerType) && !recursive) {
                // lists pass on the NA value
            } else {
                error.enter();
                throwBoundsError();
            }
        }
    }

    private void throwBoundsError() {
        if (recursive) {
            throw new RecursiveIndexNotFoundError();
        } else {
            throw RError.error(this, RError.Message.SUBSCRIPT_BOUNDS);
        }
    }
}
