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
package com.oracle.truffle.r.nodes.access.array.write;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@SuppressWarnings("unused")
@NodeChildren({@NodeChild(value = "vectorD", type = RNode.class), @NodeChild(value = "valueD", type = RNode.class), @NodeChild(value = "positionsD", type = RNode.class)})
public abstract class MultiDimPosConverterValueNode extends RNode {

    private final BranchProfile errorProfile = BranchProfile.create();

    public abstract RIntVector executeConvert(Object vector, Object value, Object positions);

    private final boolean isSubset;

    private final ConditionProfile listProfile = ConditionProfile.createBinaryProfile();

    protected MultiDimPosConverterValueNode(boolean isSubset) {
        this.isSubset = isSubset;
    }

    protected MultiDimPosConverterValueNode(MultiDimPosConverterValueNode other) {
        this.isSubset = other.isSubset;
    }

    private static RError.Message getErrorForValueSize(Object value, RError.Message sizeOneMessage) {
        RError.Message message;
        int size = value instanceof RNull ? 0 : ((RAbstractVector) RRuntime.asAbstractVector(value)).getLength();
        if (size == 0) {
            message = RError.Message.REPLACEMENT_0;
        } else if (size == 1) {
            message = sizeOneMessage;
        } else {
            message = RError.Message.MORE_SUPPLIED_REPLACE;
        }
        return message;
    }

    // /////////////
    // RNull vectors

    @Specialization(guards = {"!singlePosNegative(positions)"})
    protected RAbstractIntVector doIntVector(RNull vector, RNull value, RAbstractIntVector positions) {
        if (isSubset || positions.getLength() <= 1) {
            return positions;
        } else {
            errorProfile.enter();
            throw RError.error(this, RError.Message.MORE_SUPPLIED_REPLACE);
        }
    }

    @Specialization(guards = {"!isPosVectorInt(positions)"})
    protected RAbstractVector doIntVector(RNull vector, Object value, RAbstractVector positions) {
        if (!isSubset || positions.getLength() <= 1) {
            return positions;
        } else {
            errorProfile.enter();
            throw RError.error(this, isEmpty(value) ? RError.Message.SELECT_MORE_1 : RError.Message.MORE_SUPPLIED_REPLACE);
        }
    }

    private static boolean isEmpty(Object value) {
        return value instanceof RNull || ((RAbstractVector) value).getLength() == 0;
    }

    @Specialization(guards = {"!emptyValue(value)"})
    protected RAbstractIntVector doIntVector(RNull vector, RAbstractVector value, RAbstractIntVector positions) {
        if (singlePosNegative(positions)) {
            errorProfile.enter();
            throw RError.error(this, RError.Message.SELECT_MORE_1);
        }
        if (!isSubset && positions.getLength() > 1) {
            errorProfile.enter();
            throw RError.error(this, RError.Message.SELECT_MORE_1);
        }
        return positions;
    }

    @Specialization(guards = {"emptyValue(value)", "!isPosVectorInt(positions)"})
    protected RAbstractVector doIntVectorEmptyValue(RNull vector, RAbstractVector value, RAbstractVector positions) {
        if (!isSubset) {
            throw RError.error(this, RError.Message.REPLACEMENT_0);
        } else {
            return positions;
        }
    }

    // //////////////////
    // RComplex positions

    @Specialization
    protected Object accessComplexEmptyValue(RAbstractContainer container, Object value, RAbstractComplexVector position) {
        if (!isSubset) {
            RError.Message message;
            if (container instanceof RList) {
                message = RError.Message.INVALID_SUBSCRIPT_TYPE;
            } else {
                message = value instanceof RNull ? RError.Message.MORE_SUPPLIED_REPLACE : getErrorForValueSize(value, RError.Message.INVALID_SUBSCRIPT_TYPE);
            }
            throw RError.error(this, message, "complex");
        } else {
            throw RError.error(this, RError.Message.INVALID_SUBSCRIPT_TYPE, "complex");
        }
    }

    // //////////////
    // RRaw positions

    @Specialization
    protected Object accessRaw(RAbstractContainer container, Object value, RAbstractRawVector position) {
        if (!isSubset) {
            RError.Message message;
            if (container instanceof RList) {
                message = RError.Message.INVALID_SUBSCRIPT_TYPE;
            } else {
                message = value instanceof RNull ? RError.Message.MORE_SUPPLIED_REPLACE : getErrorForValueSize(value, RError.Message.INVALID_SUBSCRIPT_TYPE);
            }
            throw RError.error(this, message, "raw");
        } else {
            throw RError.error(this, RError.Message.INVALID_SUBSCRIPT_TYPE, "raw");
        }
    }

    @Specialization(guards = {"noPosition(positions)"})
    protected RAbstractVector accessListEmptyPosEmptyValue(RAbstractContainer container, Object value, RAbstractVector positions) {
        if (!isSubset) {
            RError.Message message;
            if (positions instanceof RList && container instanceof RList) {
                message = RError.Message.SELECT_LESS_1;
            } else {
                message = getErrorForValueSize(value, RError.Message.SELECT_LESS_1);
            }
            throw RError.error(this, message);
        } else if (positions instanceof RList) {
            errorProfile.enter();
            throw RError.error(this, RError.Message.INVALID_SUBSCRIPT_TYPE, "list");
        } else {
            return positions;
        }
    }

    @Specialization(guards = {"onePosition(positions)", "emptyValue(value)"})
    protected RAbstractVector accessListOnePosZeroEmptyValueList(RList vector, RAbstractVector value, RAbstractIntVector positions) {
        if (!isSubset) {
            if (positions.getDataAt(0) == 0) {
                errorProfile.enter();
                throw RError.error(this, RError.Message.SELECT_LESS_1);
            }
        }
        return positions;
    }

    @Specialization(guards = {"onePosition(positions)"})
    protected RAbstractVector accessListOnePosEmptyValueList(RList vector, RNull value, RAbstractIntVector positions) {
        if (!isSubset) {
            if (positions.getDataAt(0) == 0) {
                errorProfile.enter();
                throw RError.error(this, RError.Message.SELECT_LESS_1);
            }
        }
        return positions;
    }

    @Specialization(guards = {"onePosition(positions)", "emptyValue(value)", "!isContainerList(container)"})
    protected RAbstractVector accessListOnePosEmptyValue(RAbstractContainer container, RAbstractVector value, RAbstractIntVector positions) {
        if (!isSubset) {
            throw RError.error(this, RError.Message.REPLACEMENT_0);
        } else {
            return positions;
        }
    }

    @Specialization(guards = {"onePosition(positions)", "!emptyValue(value)"})
    protected RAbstractVector accessListOnePosNonEmptyValue(RAbstractContainer container, RAbstractVector value, RAbstractIntVector positions) {
        if (!isSubset) {
            if (positions.getDataAt(0) == 0) {
                errorProfile.enter();
                throw RError.error(this, RError.Message.SELECT_LESS_1);
            }
        }
        return positions;
    }

    @Specialization(guards = {"onePosition(positions)", "valueLongerThanOne(value)", "!isContainerList(container)"})
    protected RAbstractVector accessListOnePosValueLongerThanOne(RAbstractContainer container, RAbstractVector value, RAbstractIntVector positions) {
        if (!isSubset) {
            throw RError.error(this, RError.Message.MORE_SUPPLIED_REPLACE);
        } else {
            return positions;
        }
    }

    @Specialization(guards = {"onePosition(positions)", "!isContainerList(container)"})
    protected RAbstractVector accessListOnePosValueLongerThanOne(RAbstractContainer container, RNull value, RAbstractIntVector positions) {
        if (!isSubset) {
            throw RError.error(this, RError.Message.MORE_SUPPLIED_REPLACE);
        } else {
            return positions;
        }
    }

    @Specialization(guards = {"onePosition(positions)", "emptyValue(value)", "!isPosVectorInt(positions)"})
    protected RAbstractVector accessListOnePosEmptyValueList(RList vector, RAbstractVector value, RAbstractVector positions) {
        if (positions instanceof RList) {
            errorProfile.enter();
            throw RError.error(this, RError.Message.INVALID_SUBSCRIPT_TYPE, "list");
        } else {
            return positions;
        }
    }

    @Specialization(guards = {"onePosition(positions)", "!isPosVectorInt(positions)"})
    protected RAbstractVector accessListOnePosEmptyValueList(RList vector, RNull value, RAbstractVector positions) {
        if (positions instanceof RList) {
            errorProfile.enter();
            throw RError.error(this, RError.Message.INVALID_SUBSCRIPT_TYPE, "list");
        } else {
            return positions;
        }
    }

    @Specialization(guards = {"onePosition(positions)", "emptyValue(value)", "!isContainerList(container)", "!isPosVectorInt(positions)"})
    protected RAbstractVector accessListOnePosEmptyValue(RAbstractContainer container, RAbstractVector value, RAbstractVector positions) {
        if (!isSubset) {
            errorProfile.enter();
            throw RError.error(this, RError.Message.REPLACEMENT_0);
        } else if (positions instanceof RList) {
            errorProfile.enter();
            throw RError.error(this, RError.Message.INVALID_SUBSCRIPT_TYPE, "list");
        } else {
            return positions;
        }
    }

    @Specialization(guards = {"onePosition(positions)", "valueLengthOne(value)", "!isPosVectorInt(positions)"})
    protected RAbstractVector accessListOnePosValueLengthOne(RAbstractContainer container, RAbstractVector value, RAbstractVector positions) {
        if (positions instanceof RList) {
            errorProfile.enter();
            throw RError.error(this, RError.Message.INVALID_SUBSCRIPT_TYPE, "list");
        } else {
            return positions;
        }
    }

    @Specialization(guards = {"onePosition(positions)", "valueLongerThanOne(value)", "!isPosVectorInt(positions)"})
    protected RAbstractVector accessListOnePosValueLongerThanOneList(RList vector, RAbstractVector value, RAbstractVector positions) {
        if (positions instanceof RList) {
            errorProfile.enter();
            throw RError.error(this, RError.Message.INVALID_SUBSCRIPT_TYPE, "list");
        } else {
            return positions;
        }
    }

    @Specialization(guards = {"onePosition(positions)", "valueLongerThanOne(value)", "!isPosVectorInt(positions)", "!isContainerList(container)"})
    protected RAbstractVector accessListOnePosValueLongerThanOne(RAbstractContainer container, RAbstractVector value, RAbstractVector positions) {
        if (!isSubset) {
            throw RError.error(this, RError.Message.MORE_SUPPLIED_REPLACE);
        } else if (positions instanceof RList) {
            throw RError.error(this, RError.Message.INVALID_SUBSCRIPT_TYPE, "list");
        } else {
            return positions;
        }
    }

    @Specialization(guards = {"onePosition(positions)", "!isContainerList(container)", "!isPosVectorInt(positions)"})
    protected RAbstractVector accessListOnePosValueLongerThanOne(RAbstractContainer container, RNull value, RAbstractVector positions) {
        if (!isSubset) {
            throw RError.error(this, RError.Message.MORE_SUPPLIED_REPLACE);
        } else if (positions instanceof RList) {
            errorProfile.enter();
            throw RError.error(this, RError.Message.INVALID_SUBSCRIPT_TYPE, "list");
        } else {
            return positions;
        }
    }

    @Specialization(guards = "multiPos(positions)")
    protected RAbstractVector accessListMultiPos(RAbstractContainer container, Object value, RAbstractVector positions) {
        if (!isSubset) {
            RError.Message message;
            if (positions instanceof RList && container instanceof RList) {
                message = RError.Message.SELECT_MORE_1;
            } else {
                message = getErrorForValueSize(value, RError.Message.SELECT_MORE_1);
            }
            throw RError.error(this, message);
        } else if (positions instanceof RList) {
            errorProfile.enter();
            throw RError.error(this, RError.Message.INVALID_SUBSCRIPT_TYPE, "list");
        } else {
            return positions;
        }
    }

    protected static boolean singlePosNegative(RAbstractIntVector p) {
        return p.getLength() == 1 && p.getDataAt(0) < 0 && !RRuntime.isNA(p.getDataAt(0));
    }

    protected boolean firstPosZero(RAbstractIntVector p) {
        return p.getDataAt(0) == 0;
    }

    protected boolean isContainerList(RAbstractContainer container) {
        return container instanceof RList;
    }

    protected boolean isPosVectorInt(RAbstractVector p) {
        return p instanceof RAbstractIntVector;
    }

    protected boolean noPosition(RAbstractVector p) {
        return p.getLength() == 0;
    }

    protected boolean onePosition(RAbstractVector p) {
        return p.getLength() == 1;
    }

    protected boolean multiPos(RAbstractVector p) {
        return p.getLength() > 1;
    }

    protected boolean emptyValue(RAbstractVector value) {
        return value.getLength() == 0;
    }

    protected boolean valueLengthOne(RAbstractVector value) {
        return value.getLength() == 1;
    }

    protected boolean valueLongerThanOne(RAbstractVector value) {
        return value.getLength() > 1;
    }
}
