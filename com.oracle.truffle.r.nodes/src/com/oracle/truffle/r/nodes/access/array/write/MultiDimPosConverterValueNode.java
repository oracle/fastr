/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@SuppressWarnings("unused")
@NodeChildren({@NodeChild(value = "vector", type = RNode.class), @NodeChild(value = "newValue", type = RNode.class), @NodeChild(value = "operand", type = RNode.class)})
public abstract class MultiDimPosConverterValueNode extends RNode {

    public abstract RIntVector executeConvert(VirtualFrame frame, Object vector, Object value, Object p);

    private final boolean isSubset;

    protected MultiDimPosConverterValueNode(boolean isSubset) {
        this.isSubset = isSubset;
    }

    protected MultiDimPosConverterValueNode(MultiDimPosConverterValueNode other) {
        this.isSubset = other.isSubset;
    }

    @Specialization(guards = {"!singlePosNegative", "!multiPos"})
    protected RAbstractIntVector doIntVector(RNull vector, RNull value, RAbstractIntVector positions) {
        return positions;
    }

    @Specialization(guards = {"!isPosVectorInt", "!multiPos"})
    protected RAbstractVector doIntVector(RNull vector, RNull value, RAbstractVector positions) {
        return positions;
    }

    @Specialization(guards = {"!singlePosNegative", "multiPos"})
    protected RAbstractIntVector doIntVectorMultiPos(RNull vector, RNull value, RAbstractIntVector positions) {
        if (isSubset) {
            return positions;
        } else {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.MORE_SUPPLIED_REPLACE);
        }
    }

    @Specialization(guards = {"!isPosVectorInt", "multiPos"})
    protected RAbstractVector doIntVectorMultiPos(RNull vector, RNull value, RAbstractVector positions) {
        if (isSubset) {
            return positions;
        } else {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.MORE_SUPPLIED_REPLACE);
        }
    }

    @Specialization(guards = {"!emptyValue", "!singlePosNegative", "!multiPos"})
    protected RAbstractIntVector doIntVector(RNull vector, RAbstractVector value, RAbstractIntVector positions) {
        return positions;
    }

    @Specialization(guards = {"!emptyValue", "!isPosVectorInt", "!multiPos"})
    protected RAbstractVector doIntVector(RNull vector, RAbstractVector value, RAbstractVector positions) {
        return positions;
    }

    @Specialization(guards = {"!emptyValue", "!singlePosNegative", "multiPos"})
    protected RAbstractIntVector doIntVectorMultiPos(RNull vector, RAbstractVector value, RAbstractIntVector positions) {
        if (isSubset) {
            return positions;
        } else {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.SELECT_MORE_1);
        }
    }

    @Specialization(guards = {"!emptyValue", "!isPosVectorInt", "multiPos"})
    protected RAbstractVector doIntVectorMultiPos(RNull vector, RAbstractVector value, RAbstractVector positions) {
        if (isSubset) {
            return positions;
        } else {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.SELECT_MORE_1);
        }
    }

    @Specialization(guards = {"!emptyValue", "singlePosNegative"})
    protected RAbstractIntVector doIntVectorNegative(RNull vector, RAbstractVector value, RAbstractIntVector positions) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.SELECT_MORE_1);
    }

    @Specialization(guards = "emptyValue")
    protected RAbstractVector doIntVectorEmptyValue(RNull vector, RAbstractVector value, RAbstractVector positions) {
        if (!isSubset) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.REPLACEMENT_0);
        } else {
            return positions;
        }
    }

    @Specialization(guards = {"emptyValue", "!isVectorList"})
    protected Object accessComplexEmptyValue(RAbstractVector vector, RAbstractVector value, RComplex position) {
        if (!isSubset) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.REPLACEMENT_0);
        } else {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "complex");
        }
    }

    @Specialization(guards = {"valueLongerThanOne", "!isVectorList"})
    protected Object accessComplexValueLongerThanOne(RAbstractVector vector, RAbstractVector value, RComplex position) {
        if (!isSubset) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.MORE_SUPPLIED_REPLACE);
        } else {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "complex");
        }
    }

    @Specialization(guards = {"!valueLongerThanOne", "!emptyValue", "!isVectorList"})
    protected Object accessComplex(RAbstractVector vector, RAbstractVector value, RComplex position) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "complex");
    }

    @Specialization
    protected Object accessComplexList(RList vector, RAbstractVector value, RComplex position) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "complex");
    }

    @Specialization
    protected Object accessComplexList(RList vector, RNull value, RComplex position) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "complex");
    }

    @Specialization(guards = "!isVectorList")
    protected Object accessComplex(RAbstractVector vector, RNull value, RComplex position) {
        if (!isSubset) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.MORE_SUPPLIED_REPLACE);
        } else {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "complex");
        }
    }

    @Specialization(guards = {"emptyValue", "!isVectorList"})
    protected Object accessRawEmptyValue(RAbstractVector vector, RAbstractVector value, RRaw position) {
        if (!isSubset) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.REPLACEMENT_0);
        } else {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "raw");
        }
    }

    @Specialization(guards = {"valueLongerThanOne", "!isVectorList"})
    protected Object accessRawValueLongerThanOne(RAbstractVector vector, RAbstractVector value, RRaw position) {
        if (!isSubset) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.MORE_SUPPLIED_REPLACE);
        } else {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "raw");
        }
    }

    @Specialization(guards = {"!valueLongerThanOne", "!emptyValue", "!isVectorList"})
    protected Object accessRaw(RAbstractVector vector, RAbstractVector value, RRaw position) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "raw");
    }

    @Specialization
    protected Object accessRawList(RList vector, RAbstractVector value, RRaw position) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "raw");
    }

    @Specialization
    protected Object accessRawList(RList vector, RNull value, RRaw position) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "raw");
    }

    @Specialization(guards = "!isVectorList")
    protected Object accessRaw(RAbstractVector vector, RNull value, RRaw position) {
        if (!isSubset) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.MORE_SUPPLIED_REPLACE);
        } else {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "raw");
        }
    }

    @Specialization(guards = {"noPosition", "emptyValue"})
    protected RAbstractVector accessListEmptyPosEmptyValueList(RList vector, RAbstractVector value, RAbstractVector positions) {
        if (!isSubset) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.SELECT_LESS_1);
        } else if (positions.getElementClass() == Object.class) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "list");
        } else {
            return positions;
        }
    }

    @Specialization(guards = {"noPosition", "emptyValue", "!isVectorList"})
    protected RAbstractVector accessListEmptyPosEmptyValue(RAbstractVector vector, RAbstractVector value, RAbstractVector positions) {
        if (!isSubset) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.REPLACEMENT_0);
        } else if (positions.getElementClass() == Object.class) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "list");
        } else {
            return positions;
        }
    }

    @Specialization(guards = {"noPosition", "valueLengthOne"})
    protected RAbstractVector accessListEmptyPosValueLengthOne(RAbstractVector vector, RAbstractVector value, RAbstractVector positions) {
        if (!isSubset) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.SELECT_LESS_1);
        } else if (positions.getElementClass() == Object.class) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "list");
        } else {
            return positions;
        }
    }

    @Specialization(guards = {"noPosition", "valueLongerThanOne"})
    protected RAbstractVector accessListEmptyPosValueLongerThanOneList(RList vector, RAbstractVector value, RAbstractVector positions) {
        if (!isSubset) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.SELECT_LESS_1);
        } else if (positions.getElementClass() == Object.class) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "list");
        } else {
            return positions;
        }
    }

    @Specialization(guards = {"noPosition", "valueLongerThanOne", "!isVectorList"})
    protected RAbstractVector accessListEmptyPosValueLongerThanOne(RAbstractVector vector, RAbstractVector value, RAbstractVector positions) {
        if (!isSubset) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.MORE_SUPPLIED_REPLACE);
        } else if (positions.getElementClass() == Object.class) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "list");
        } else {
            return positions;
        }
    }

    @Specialization(guards = "noPosition")
    protected RAbstractVector accessListEmptyPosEmptyValueList(RList vector, RNull value, RAbstractVector positions) {
        if (!isSubset) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.SELECT_LESS_1);
        } else if (positions.getElementClass() == Object.class) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "list");
        } else {
            return positions;
        }
    }

    @Specialization(guards = {"noPosition", "!isVectorList"})
    protected RAbstractVector accessListEmptyPosEmptyValue(RAbstractVector vector, RNull value, RAbstractVector positions) {
        if (!isSubset) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.MORE_SUPPLIED_REPLACE);
        } else if (positions.getElementClass() == Object.class) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "list");
        } else {
            return positions;
        }
    }

    @Specialization(guards = {"onePosition", "emptyValue", "!isPosVectorInt"})
    protected RAbstractVector accessListOnePosEmptyValueList(RList vector, RAbstractVector value, RAbstractVector positions) {
        if (positions.getElementClass() == Object.class) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "list");
        } else {
            return positions;
        }
    }

    @Specialization(guards = {"onePosition", "emptyValue", "!firstPosZero"})
    protected RAbstractVector accessListOnePosEmptyValueList(RList vector, RAbstractVector value, RAbstractIntVector positions) {
        if (positions.getElementClass() == Object.class) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "list");
        } else {
            return positions;
        }
    }

    @Specialization(guards = {"onePosition", "emptyValue", "firstPosZero"})
    protected RAbstractVector accessListOnePosZeroEmptyValueList(RList vector, RAbstractVector value, RAbstractIntVector positions) {
        if (!isSubset) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.SELECT_LESS_1);
        } else {
            return positions;
        }
    }

    @Specialization(guards = {"onePosition", "emptyValue", "!isVectorList", "!isPosVectorInt"})
    protected RAbstractVector accessListOnePosEmptyValue(RAbstractVector vector, RAbstractVector value, RAbstractVector positions) {
        if (!isSubset) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.REPLACEMENT_0);
        } else if (positions.getElementClass() == Object.class) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "list");
        } else {
            return positions;
        }
    }

    @Specialization(guards = {"onePosition", "emptyValue", "!isVectorList", "!firstPosZero"})
    protected RAbstractVector accessListOnePosEmptyValue(RAbstractVector vector, RAbstractVector value, RAbstractIntVector positions) {
        if (!isSubset) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.REPLACEMENT_0);
        } else {
            return positions;
        }
    }

    @Specialization(guards = {"onePosition", "emptyValue", "!isVectorList", "firstPosZero"})
    protected RAbstractVector accessListOnePosZeroEmptyValue(RAbstractVector vector, RAbstractVector value, RAbstractIntVector positions) {
        if (!isSubset) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.REPLACEMENT_0);
        } else {
            return positions;
        }
    }

    @Specialization(guards = {"onePosition", "valueLengthOne", "!isPosVectorInt"})
    protected RAbstractVector accessListOnePosValueLengthOne(RAbstractVector vector, RAbstractVector value, RAbstractVector positions) {
        if (positions.getElementClass() == Object.class) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "list");
        } else {
            return positions;
        }
    }

    @Specialization(guards = {"onePosition", "valueLengthOne", "!firstPosZero"})
    protected RAbstractVector accessListOnePosValueLengthOne(RAbstractVector vector, RAbstractVector value, RAbstractIntVector positions) {
        return positions;
    }

    @Specialization(guards = {"onePosition", "valueLengthOne", "firstPosZero"})
    protected RAbstractVector accessListOnePosZeroValueLengthOne(RAbstractVector vector, RAbstractVector value, RAbstractIntVector positions) {
        if (!isSubset) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.SELECT_LESS_1);
        } else {
            return positions;
        }
    }

    @Specialization(guards = {"onePosition", "valueLongerThanOne", "!isPosVectorInt"})
    protected RAbstractVector accessListOnePosValueLongerThanOneList(RList vector, RAbstractVector value, RAbstractVector positions) {
        if (positions.getElementClass() == Object.class) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "list");
        } else {
            return positions;
        }
    }

    @Specialization(guards = {"onePosition", "valueLongerThanOne", "!firstPosZero"})
    protected RAbstractVector accessListOnePosValueLongerThanOneList(RList vector, RAbstractVector value, RAbstractIntVector positions) {
        if (positions.getElementClass() == Object.class) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "list");
        } else {
            return positions;
        }
    }

    @Specialization(guards = {"onePosition", "valueLongerThanOne", "firstPosZero"})
    protected RAbstractVector accessListOnePosZeroValueLongerThanOneList(RList vector, RAbstractVector value, RAbstractIntVector positions) {
        if (!isSubset) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.SELECT_LESS_1);
        } else {
            return positions;
        }
    }

    @Specialization(guards = {"onePosition", "valueLongerThanOne", "!isVectorList", "!isPosVectorInt"})
    protected RAbstractVector accessListOnePosValueLongerThanOne(RAbstractVector vector, RAbstractVector value, RAbstractVector positions) {
        if (!isSubset) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.MORE_SUPPLIED_REPLACE);
        } else if (positions.getElementClass() == Object.class) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "list");
        } else {
            return positions;
        }
    }

    @Specialization(guards = {"onePosition", "valueLongerThanOne", "!isVectorList", "!firstPosZero"})
    protected RAbstractVector accessListOnePosValueLongerThanOne(RAbstractVector vector, RAbstractVector value, RAbstractIntVector positions) {
        if (!isSubset) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.MORE_SUPPLIED_REPLACE);
        } else {
            return positions;
        }
    }

    @Specialization(guards = {"onePosition", "valueLongerThanOne", "!isVectorList", "firstPosZero"})
    protected RAbstractVector accessListOnePosZeroValueLongerThanOne(RAbstractVector vector, RAbstractVector value, RAbstractIntVector positions) {
        if (!isSubset) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.MORE_SUPPLIED_REPLACE);
        } else {
            return positions;
        }
    }

    @Specialization(guards = {"onePosition", "!isPosVectorInt"})
    protected RAbstractVector accessListOnePosEmptyValueList(RList vector, RNull value, RAbstractVector positions) {
        if (positions.getElementClass() == Object.class) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "list");
        } else {
            return positions;
        }
    }

    @Specialization(guards = {"onePosition", "!firstPosZero"})
    protected RAbstractVector accessListOnePosEmptyValueList(RList vector, RNull value, RAbstractIntVector positions) {
        if (positions.getElementClass() == Object.class) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "list");
        } else {
            return positions;
        }
    }

    @Specialization(guards = {"onePosition", "firstPosZero"})
    protected RAbstractVector accessListOnePosZeroEmptyValueList(RList vector, RNull value, RAbstractIntVector positions) {
        if (!isSubset) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.SELECT_LESS_1);
        } else {
            return positions;
        }
    }

    @Specialization(guards = {"onePosition", "!isVectorList", "!isPosVectorInt"})
    protected RAbstractVector accessListOnePosValueLongerThanOne(RAbstractVector vector, RNull value, RAbstractVector positions) {
        if (!isSubset) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.MORE_SUPPLIED_REPLACE);
        } else if (positions.getElementClass() == Object.class) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "list");
        } else {
            return positions;
        }
    }

    @Specialization(guards = {"onePosition", "!isVectorList", "!firstPosZero"})
    protected RAbstractVector accessListOnePosValueLongerThanOne(RAbstractVector vector, RNull value, RAbstractIntVector positions) {
        if (!isSubset) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.MORE_SUPPLIED_REPLACE);
        } else {
            return positions;
        }
    }

    @Specialization(guards = {"onePosition", "!isVectorList", "firstPosZero"})
    protected RAbstractVector accessListOnePosZeroValueLongerThanOne(RAbstractVector vector, RNull value, RAbstractIntVector positions) {
        if (!isSubset) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.MORE_SUPPLIED_REPLACE);
        } else {
            return positions;
        }
    }

    @Specialization(guards = "multiPos")
    protected RAbstractVector accessListTwoPosEmptyValueList(RList vector, RAbstractVector value, RAbstractVector positions) {
        if (!isSubset) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.SELECT_MORE_1);
        } else if (positions.getElementClass() == Object.class) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "list");
        } else {
            return positions;
        }
    }

    @Specialization(guards = {"multiPos", "emptyValue", "!isVectorList"})
    protected RAbstractVector accessListTwoPosEmptyValue(RAbstractVector vector, RAbstractVector value, RAbstractVector positions) {
        if (!isSubset) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.REPLACEMENT_0);
        } else if (positions.getElementClass() == Object.class) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "list");
        } else {
            return positions;
        }
    }

    @Specialization(guards = {"multiPos", "valueLengthOne", "!isVectorList"})
    protected RAbstractVector accessListTwoPosValueLengthOne(RAbstractVector vector, RAbstractVector value, RAbstractVector positions) {
        if (!isSubset) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.SELECT_MORE_1);
        } else if (positions.getElementClass() == Object.class) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "list");
        } else {
            return positions;
        }
    }

    @Specialization(guards = {"multiPos", "valueLongerThanOne", "!isVectorList"})
    protected RAbstractVector accessListTwoPosValueLongerThanOne(RAbstractVector vector, RAbstractVector value, RAbstractVector positions) {
        if (!isSubset) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.MORE_SUPPLIED_REPLACE);
        } else if (positions.getElementClass() == Object.class) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "list");
        } else {
            return positions;
        }
    }

    @Specialization(guards = "multiPos")
    protected RAbstractVector accessListTwoPosEmptyValueList(RList vector, RNull value, RAbstractVector positions) {
        if (!isSubset) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.SELECT_MORE_1);
        } else if (positions.getElementClass() == Object.class) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "list");
        } else {
            return positions;
        }
    }

    @Specialization(guards = {"multiPos", "!isVectorList"})
    protected RAbstractVector accessListTwoPosValueLongerThanOne(RAbstractVector vector, RNull value, RAbstractVector positions) {
        if (!isSubset) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.MORE_SUPPLIED_REPLACE);
        } else if (positions.getElementClass() == Object.class) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "list");
        } else {
            return positions;
        }
    }

    protected boolean singlePosNegative(Object vector, RNull value, RAbstractIntVector p) {
        return p.getLength() == 1 && p.getDataAt(0) < 0 && !RRuntime.isNA(p.getDataAt(0));
    }

    protected boolean singlePosNegative(Object vector, RAbstractVector value, RAbstractIntVector p) {
        return p.getLength() == 1 && p.getDataAt(0) < 0 && !RRuntime.isNA(p.getDataAt(0));
    }

    protected boolean firstPosZero(RAbstractVector vector, RNull value, RAbstractIntVector p) {
        return p.getDataAt(0) == 0;
    }

    protected boolean firstPosZero(RAbstractVector vector, RAbstractVector value, RAbstractIntVector p) {
        return p.getDataAt(0) == 0;
    }

    protected boolean isVectorList(RAbstractVector vector) {
        return vector.getElementClass() == Object.class;
    }

    protected boolean isPosVectorInt(RAbstractVector vector, RAbstractVector value, RAbstractVector p) {
        return p.getElementClass() == RInt.class;
    }

    protected boolean isPosVectorInt(RAbstractVector vector, RNull value, RAbstractVector p) {
        return p.getElementClass() == RInt.class;
    }

    protected boolean isPosVectorInt(RNull vector, RNull value, RAbstractVector p) {
        return p.getElementClass() == RInt.class;
    }

    protected boolean isPosVectorInt(RNull vector, RAbstractVector value, RAbstractVector p) {
        return p.getElementClass() == RInt.class;
    }

    // Truffle DSL bug (?) - guards should work with just RAbstractVector as the vector
    // parameter
    protected boolean onePosition(RList vector, RAbstractVector value, RAbstractVector p) {
        return p.getLength() == 1;
    }

    protected boolean onePosition(RList vector, RNull value, RAbstractVector p) {
        return p.getLength() == 1;
    }

    protected boolean noPosition(RList vector, RAbstractVector value, RAbstractVector p) {
        return p.getLength() == 0;
    }

    protected boolean noPosition(RList vector, RNull value, RAbstractVector p) {
        return p.getLength() == 0;
    }

    protected boolean multiPos(RList vector, RAbstractVector value, RAbstractVector p) {
        return p.getLength() > 1;
    }

    protected boolean multiPos(RList vector, RNull value, RAbstractVector p) {
        return p.getLength() > 1;
    }

    protected boolean onePosition(RAbstractVector vector, RAbstractVector value, RAbstractVector p) {
        return p.getLength() == 1;
    }

    protected boolean onePosition(RAbstractVector vector, RNull value, RAbstractVector p) {
        return p.getLength() == 1;
    }

    protected boolean noPosition(RAbstractVector vector, RAbstractVector value, RAbstractVector p) {
        return p.getLength() == 0;
    }

    protected boolean noPosition(RAbstractVector vector, RNull value, RAbstractVector p) {
        return p.getLength() == 0;
    }

    protected boolean multiPos(RAbstractVector vector, RAbstractVector value, RAbstractVector p) {
        return p.getLength() > 1;
    }

    protected boolean multiPos(RAbstractVector vector, RNull value, RAbstractVector p) {
        return p.getLength() > 1;
    }

    protected boolean multiPos(RNull vector, RNull value, RAbstractVector p) {
        return p.getLength() > 1;
    }

    protected boolean multiPos(RNull vector, RAbstractVector value, RAbstractVector p) {
        return p.getLength() > 1;
    }

    protected boolean emptyValue(RNull vector, RAbstractVector value) {
        return value.getLength() == 0;
    }

    protected boolean emptyValue(RAbstractVector vector, RAbstractVector value) {
        return value.getLength() == 0;
    }

    protected boolean valueLengthOne(RAbstractVector vector, RAbstractVector value) {
        return value.getLength() == 1;
    }

    protected boolean valueLongerThanOne(RAbstractVector vector, RAbstractVector value) {
        return value.getLength() > 1;
    }

}