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

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@SuppressWarnings("unused")
@NodeChildren({@NodeChild(value = "newValue", type = RNode.class), @NodeChild(value = "vector", type = RNode.class), @NodeChild(value = "operand", type = RNode.class)})
public abstract class CoerceVector extends RNode {

    public abstract Object executeEvaluated(VirtualFrame frame, Object value, Object vector, Object operand);

    @Child private CastComplexNode castComplex;
    @Child private CastDoubleNode castDouble;
    @Child private CastIntegerNode castInteger;
    @Child private CastStringNode castString;
    @Child private CastListNode castList;

    private Object castComplex(VirtualFrame frame, Object vector) {
        if (castComplex == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castComplex = insert(CastComplexNodeFactory.create(null, true, true, true));
        }
        return castComplex.executeCast(frame, vector);
    }

    private Object castDouble(VirtualFrame frame, Object vector) {
        if (castDouble == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castDouble = insert(CastDoubleNodeFactory.create(null, true, true, true));
        }
        return castDouble.executeCast(frame, vector);
    }

    private Object castInteger(VirtualFrame frame, Object vector) {
        if (castInteger == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castInteger = insert(CastIntegerNodeFactory.create(null, true, true, true));
        }
        return castInteger.executeCast(frame, vector);
    }

    private Object castString(VirtualFrame frame, Object vector) {
        if (castString == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castString = insert(CastStringNodeFactory.create(null, true, true, true, false));
        }
        return castString.executeCast(frame, vector);
    }

    private Object castList(VirtualFrame frame, Object vector) {
        if (castList == null) {
            CompilerDirectives.transferToInterpreter();
            castList = insert(CastListNodeFactory.create(null, true, false, true));
        }
        return castList.executeCast(frame, vector);
    }

    @Specialization
    protected RFunction coerce(VirtualFrame frame, Object value, RFunction vector, Object operand) {
        return vector;
    }

    // int vector value

    @Specialization
    protected RAbstractIntVector coerce(VirtualFrame frame, RAbstractIntVector value, RAbstractIntVector vector, Object operand) {
        return vector;
    }

    @Specialization
    protected RAbstractDoubleVector coerce(VirtualFrame frame, RAbstractIntVector value, RAbstractDoubleVector vector, Object operand) {
        return vector;
    }

    @Specialization
    protected RAbstractIntVector coerce(VirtualFrame frame, RAbstractIntVector value, RAbstractLogicalVector vector, Object operand) {
        return (RIntVector) castInteger(frame, vector);
    }

    @Specialization
    protected RAbstractStringVector coerce(VirtualFrame frame, RAbstractIntVector value, RAbstractStringVector vector, Object operand) {
        return vector;
    }

    @Specialization
    protected RAbstractComplexVector coerce(VirtualFrame frame, RAbstractIntVector value, RAbstractComplexVector vector, Object operand) {
        return vector;
    }

    @Specialization
    protected RIntVector coerce(VirtualFrame frame, RAbstractIntVector value, RAbstractRawVector vector, Object operand) {
        throw RError.error(frame, getEncapsulatingSourceSection(), RError.Message.SUBASSIGN_TYPE_FIX, "integer", "raw");
    }

    @Specialization
    protected RList coerce(VirtualFrame frame, RAbstractIntVector value, RList vector, Object operand) {
        return vector;
    }

    // double vector value

    @Specialization
    protected RDoubleVector coerce(VirtualFrame frame, RAbstractDoubleVector value, RAbstractIntVector vector, Object operand) {
        return (RDoubleVector) castDouble(frame, vector);
    }

    @Specialization
    protected RAbstractDoubleVector coerce(VirtualFrame frame, RAbstractDoubleVector value, RAbstractDoubleVector vector, Object operand) {
        return vector;
    }

    @Specialization
    protected RDoubleVector coerce(VirtualFrame frame, RAbstractDoubleVector value, RAbstractLogicalVector vector, Object operand) {
        return (RDoubleVector) castDouble(frame, vector);
    }

    @Specialization
    protected RAbstractStringVector coerce(VirtualFrame frame, RAbstractDoubleVector value, RAbstractStringVector vector, Object operand) {
        return vector;
    }

    @Specialization
    protected RAbstractComplexVector coerce(VirtualFrame frame, RAbstractDoubleVector value, RAbstractComplexVector vector, Object operand) {
        return vector;
    }

    @Specialization
    protected RDoubleVector coerce(VirtualFrame frame, RAbstractDoubleVector value, RAbstractRawVector vector, Object operand) {
        throw RError.error(frame, getEncapsulatingSourceSection(), RError.Message.SUBASSIGN_TYPE_FIX, "double", "raw");
    }

    @Specialization
    protected RList coerce(VirtualFrame frame, RAbstractDoubleVector value, RList vector, Object operand) {
        return vector;
    }

    // logical vector value

    @Specialization
    protected RAbstractIntVector coerce(VirtualFrame frame, RAbstractLogicalVector value, RAbstractIntVector vector, Object operand) {
        return vector;
    }

    @Specialization
    protected RAbstractDoubleVector coerce(VirtualFrame frame, RAbstractLogicalVector value, RAbstractDoubleVector vector, Object operand) {
        return vector;
    }

    @Specialization
    protected RAbstractLogicalVector coerce(VirtualFrame frame, RAbstractLogicalVector value, RAbstractLogicalVector vector, Object operand) {
        return vector;
    }

    @Specialization
    protected RAbstractStringVector coerce(VirtualFrame frame, RAbstractLogicalVector value, RAbstractStringVector vector, Object operand) {
        return vector;
    }

    @Specialization
    protected RAbstractComplexVector coerce(VirtualFrame frame, RAbstractLogicalVector value, RAbstractComplexVector vector, Object operand) {
        return vector;
    }

    @Specialization
    protected RLogicalVector coerce(VirtualFrame frame, RAbstractLogicalVector value, RAbstractRawVector vector, Object operand) {
        throw RError.error(frame, getEncapsulatingSourceSection(), RError.Message.SUBASSIGN_TYPE_FIX, "logical", "raw");
    }

    @Specialization
    protected RList coerce(VirtualFrame frame, RAbstractLogicalVector value, RList vector, Object operand) {
        return vector;
    }

    // string vector value

    @Specialization
    protected RStringVector coerce(VirtualFrame frame, RAbstractStringVector value, RAbstractIntVector vector, Object operand) {
        return (RStringVector) castString(frame, vector);
    }

    @Specialization
    protected RStringVector coerce(VirtualFrame frame, RAbstractStringVector value, RAbstractDoubleVector vector, Object operand) {
        return (RStringVector) castString(frame, vector);
    }

    @Specialization
    protected RStringVector coerce(VirtualFrame frame, RAbstractStringVector value, RAbstractLogicalVector vector, Object operand) {
        return (RStringVector) castString(frame, vector);
    }

    @Specialization
    protected RAbstractStringVector coerce(VirtualFrame frame, RAbstractStringVector value, RAbstractStringVector vector, Object operand) {
        return vector;
    }

    @Specialization
    protected RStringVector coerce(VirtualFrame frame, RAbstractStringVector value, RAbstractComplexVector vector, Object operand) {
        return (RStringVector) castString(frame, vector);
    }

    @Specialization
    protected RStringVector coerce(VirtualFrame frame, RAbstractStringVector value, RAbstractRawVector vector, Object operand) {
        throw RError.error(frame, getEncapsulatingSourceSection(), RError.Message.SUBASSIGN_TYPE_FIX, "character", "raw");
    }

    @Specialization
    protected RList coerce(VirtualFrame frame, RAbstractStringVector value, RList vector, Object operand) {
        return vector;
    }

    // complex vector value

    @Specialization
    protected RComplexVector coerce(VirtualFrame frame, RAbstractComplexVector value, RAbstractIntVector vector, Object operand) {
        return (RComplexVector) castComplex(frame, vector);
    }

    @Specialization
    protected RComplexVector coerce(VirtualFrame frame, RAbstractComplexVector value, RAbstractDoubleVector vector, Object operand) {
        return (RComplexVector) castComplex(frame, vector);
    }

    @Specialization
    protected RComplexVector coerce(VirtualFrame frame, RAbstractComplexVector value, RAbstractLogicalVector vector, Object operand) {
        return (RComplexVector) castComplex(frame, vector);
    }

    @Specialization
    protected RAbstractStringVector coerce(VirtualFrame frame, RAbstractComplexVector value, RAbstractStringVector vector, Object operand) {
        return vector;
    }

    @Specialization
    protected RAbstractComplexVector coerce(VirtualFrame frame, RAbstractComplexVector value, RAbstractComplexVector vector, Object operand) {
        return vector;
    }

    @Specialization
    protected RComplexVector coerce(VirtualFrame frame, RAbstractComplexVector value, RAbstractRawVector vector, Object operand) {
        throw RError.error(frame, getEncapsulatingSourceSection(), RError.Message.SUBASSIGN_TYPE_FIX, "complex", "raw");
    }

    @Specialization
    protected RList coerce(VirtualFrame frame, RAbstractComplexVector value, RList vector, Object operand) {
        return vector;
    }

    // raw vector value

    @Specialization
    protected RAbstractRawVector coerce(VirtualFrame frame, RAbstractRawVector value, RAbstractRawVector vector, Object operand) {
        return vector;
    }

    @Specialization(guards = "!isVectorList")
    protected RRawVector coerce(VirtualFrame frame, RAbstractRawVector value, RAbstractVector vector, Object operand) {
        throw RError.error(frame, getEncapsulatingSourceSection(), RError.Message.SUBASSIGN_TYPE_FIX, "raw", RRuntime.classToString(vector.getElementClass(), false));
    }

    @Specialization
    protected RList coerce(VirtualFrame frame, RAbstractRawVector value, RList vector, Object operand) {
        return vector;
    }

    // list vector value

    @Specialization
    protected RList coerce(VirtualFrame frame, RList value, RList vector, Object operand) {
        return vector;
    }

    @Specialization(guards = "!isVectorList")
    protected RList coerce(VirtualFrame frame, RList value, RAbstractVector vector, Object operand) {
        return (RList) castList(frame, vector);
    }

    // data frame value

    @Specialization
    protected RList coerce(VirtualFrame frame, RDataFrame value, RAbstractVector vector, Object operand) {
        return (RList) castList(frame, vector);
    }

    // function vector value

    @Specialization
    protected RFunction coerce(VirtualFrame frame, RFunction value, RAbstractVector vector, Object operand) {
        throw RError.error(frame, getEncapsulatingSourceSection(), RError.Message.SUBASSIGN_TYPE_FIX, "closure", RRuntime.classToString(vector.getElementClass(), false));
    }

    // in all other cases, simply return the vector (no coercion)

    @Specialization
    protected RNull coerce(RNull value, RNull vector, Object operand) {
        return vector;
    }

    @Specialization
    protected RNull coerce(RAbstractVector value, RNull vector, Object operand) {
        return vector;
    }

    @Specialization
    protected RAbstractVector coerce(RNull value, RAbstractVector vector, Object operand) {
        return vector;
    }

    @Specialization
    protected RAbstractVector coerce(RList value, RAbstractVector vector, Object operand) {
        return vector;
    }

    protected boolean isVectorList(RAbstractVector value, RAbstractVector vector) {
        return vector.getElementClass() == Object.class;
    }

}