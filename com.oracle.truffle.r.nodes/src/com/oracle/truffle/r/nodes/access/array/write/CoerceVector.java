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

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.control.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.nodes.*;

@SuppressWarnings("unused")
@NodeChildren({@NodeChild(value = "newValue", type = RNode.class), @NodeChild(value = "vector", type = RNode.class), @NodeChild(value = "operand", type = RNode.class)})
public abstract class CoerceVector extends RNode {

    public abstract Object executeEvaluated(Object value, Object vector, Object operand);

    @Child private CastComplexNode castComplex;
    @Child private CastDoubleNode castDouble;
    @Child private CastIntegerNode castInteger;
    @Child private CastStringNode castString;
    @Child private CastListNode castList;
    @Child private CoerceVector coerceRecursive;

    public abstract RNode getVector();

    private Object coerceRecursive(Object value, Object vector, Object operand) {
        if (coerceRecursive == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            coerceRecursive = insert(CoerceVectorNodeGen.create(null, null, null));
        }
        return coerceRecursive.executeEvaluated(value, vector, operand);
    }

    private Object castComplex(Object vector) {
        if (castComplex == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castComplex = insert(CastComplexNodeGen.create(true, true, true));
        }
        return castComplex.execute(vector);
    }

    private Object castDouble(Object vector) {
        if (castDouble == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castDouble = insert(CastDoubleNodeGen.create(true, true, true));
        }
        return castDouble.execute(vector);
    }

    private Object castInteger(Object vector) {
        if (castInteger == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castInteger = insert(CastIntegerNodeGen.create(true, true, true));
        }
        return castInteger.execute(vector);
    }

    private Object castString(Object vector) {
        if (castString == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castString = insert(CastStringNodeGen.create(true, true, true, false));
        }
        return castString.execute(vector);
    }

    private Object castList(Object vector) {
        if (castList == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castList = insert(CastListNodeGen.create(true, false, true));
        }
        return castList.execute(vector);
    }

    @Specialization
    protected RFunction coerce(Object value, RFunction vector, Object operand) {
        return vector;
    }

    @Specialization
    protected RFactor coerce(Object value, RFactor vector, Object operand) {
        return vector;
    }

    // int vector value

    @Specialization
    protected RAbstractIntVector coerce(RAbstractIntVector value, RAbstractIntVector vector, Object operand) {
        return vector;
    }

    @Specialization
    protected RAbstractDoubleVector coerce(RAbstractIntVector value, RAbstractDoubleVector vector, Object operand) {
        return vector;
    }

    @Specialization
    protected RAbstractIntVector coerce(RAbstractIntVector value, RAbstractLogicalVector vector, Object operand) {
        return (RIntVector) castInteger(vector);
    }

    @Specialization
    protected RAbstractStringVector coerce(RAbstractIntVector value, RAbstractStringVector vector, Object operand) {
        return vector;
    }

    @Specialization
    protected RAbstractComplexVector coerce(RAbstractIntVector value, RAbstractComplexVector vector, Object operand) {
        return vector;
    }

    @Specialization
    protected RIntVector coerce(RAbstractIntVector value, RAbstractRawVector vector, Object operand) {
        throw RError.error(this, RError.Message.SUBASSIGN_TYPE_FIX, "integer", "raw");
    }

    @Specialization(guards = "isVectorListOrDataFrame(vector)")
    protected RAbstractContainer coerce(RAbstractIntVector value, RAbstractContainer vector, Object operand) {
        return vector;
    }

    // double vector value

    @Specialization
    protected RDoubleVector coerce(RAbstractDoubleVector value, RAbstractIntVector vector, Object operand) {
        return (RDoubleVector) castDouble(vector);
    }

    @Specialization
    protected RAbstractDoubleVector coerce(RAbstractDoubleVector value, RAbstractDoubleVector vector, Object operand) {
        return vector;
    }

    @Specialization
    protected RDoubleVector coerce(RAbstractDoubleVector value, RAbstractLogicalVector vector, Object operand) {
        return (RDoubleVector) castDouble(vector);
    }

    @Specialization
    protected RAbstractStringVector coerce(RAbstractDoubleVector value, RAbstractStringVector vector, Object operand) {
        return vector;
    }

    @Specialization
    protected RAbstractComplexVector coerce(RAbstractDoubleVector value, RAbstractComplexVector vector, Object operand) {
        return vector;
    }

    @Specialization
    protected RDoubleVector coerce(RAbstractDoubleVector value, RAbstractRawVector vector, Object operand) {
        throw RError.error(this, RError.Message.SUBASSIGN_TYPE_FIX, "double", "raw");
    }

    @Specialization
    protected RAbstractContainer coerce(RAbstractDoubleVector value, RAbstractContainer vector, Object operand) {
        return vector;
    }

    // logical vector value

    @Specialization
    protected RAbstractIntVector coerce(RAbstractLogicalVector value, RAbstractIntVector vector, Object operand) {
        return vector;
    }

    @Specialization
    protected RAbstractDoubleVector coerce(RAbstractLogicalVector value, RAbstractDoubleVector vector, Object operand) {
        return vector;
    }

    @Specialization
    protected RAbstractLogicalVector coerce(RAbstractLogicalVector value, RAbstractLogicalVector vector, Object operand) {
        return vector;
    }

    @Specialization
    protected RAbstractStringVector coerce(RAbstractLogicalVector value, RAbstractStringVector vector, Object operand) {
        return vector;
    }

    @Specialization
    protected RAbstractComplexVector coerce(RAbstractLogicalVector value, RAbstractComplexVector vector, Object operand) {
        return vector;
    }

    @Specialization
    protected RLogicalVector coerce(RAbstractLogicalVector value, RAbstractRawVector vector, Object operand) {
        throw RError.error(this, RError.Message.SUBASSIGN_TYPE_FIX, "logical", "raw");
    }

    @Specialization(guards = "isVectorListOrDataFrame(vector)")
    protected RAbstractContainer coerce(RAbstractLogicalVector value, RAbstractContainer vector, Object operand) {
        return vector;
    }

    // string vector value

    @Specialization
    protected RStringVector coerce(RAbstractStringVector value, RAbstractIntVector vector, Object operand) {
        return (RStringVector) castString(vector);
    }

    @Specialization
    protected RStringVector coerce(RAbstractStringVector value, RAbstractDoubleVector vector, Object operand) {
        return (RStringVector) castString(vector);
    }

    @Specialization
    protected RStringVector coerce(RAbstractStringVector value, RAbstractLogicalVector vector, Object operand) {
        return (RStringVector) castString(vector);
    }

    @Specialization
    protected RAbstractStringVector coerce(RAbstractStringVector value, RAbstractStringVector vector, Object operand) {
        return vector;
    }

    @Specialization
    protected RStringVector coerce(RAbstractStringVector value, RAbstractComplexVector vector, Object operand) {
        return (RStringVector) castString(vector);
    }

    @Specialization
    protected RStringVector coerce(RAbstractStringVector value, RAbstractRawVector vector, Object operand) {
        throw RError.error(this, RError.Message.SUBASSIGN_TYPE_FIX, "character", "raw");
    }

    @Specialization(guards = "isVectorListOrDataFrame(vector)")
    protected RAbstractContainer coerce(RAbstractStringVector value, RAbstractContainer vector, Object operand) {
        return vector;
    }

    // complex vector value

    @Specialization
    protected RComplexVector coerce(RAbstractComplexVector value, RAbstractIntVector vector, Object operand) {
        return (RComplexVector) castComplex(vector);
    }

    @Specialization
    protected RComplexVector coerce(RAbstractComplexVector value, RAbstractDoubleVector vector, Object operand) {
        return (RComplexVector) castComplex(vector);
    }

    @Specialization
    protected RComplexVector coerce(RAbstractComplexVector value, RAbstractLogicalVector vector, Object operand) {
        return (RComplexVector) castComplex(vector);
    }

    @Specialization
    protected RAbstractStringVector coerce(RAbstractComplexVector value, RAbstractStringVector vector, Object operand) {
        return vector;
    }

    @Specialization
    protected RAbstractComplexVector coerce(RAbstractComplexVector value, RAbstractComplexVector vector, Object operand) {
        return vector;
    }

    @Specialization
    protected RComplexVector coerce(RAbstractComplexVector value, RAbstractRawVector vector, Object operand) {
        throw RError.error(this, RError.Message.SUBASSIGN_TYPE_FIX, "complex", "raw");
    }

    @Specialization(guards = "isVectorListOrDataFrame(vector)")
    protected RAbstractContainer coerce(RAbstractComplexVector value, RAbstractContainer vector, Object operand) {
        return vector;
    }

    // raw vector value

    @Specialization
    protected RAbstractRawVector coerce(RAbstractRawVector value, RAbstractRawVector vector, Object operand) {
        return vector;
    }

    @Specialization(guards = "!isVectorList(vector)")
    protected RRawVector coerce(RAbstractRawVector value, RAbstractVector vector, Object operand) {
        throw RError.error(this, RError.Message.SUBASSIGN_TYPE_FIX, "raw", RRuntime.classToString(vector.getElementClass(), false));
    }

    @Specialization(guards = "isVectorListOrDataFrame(vector)")
    protected RAbstractContainer coerce(RAbstractRawVector value, RAbstractContainer vector, Object operand) {
        return vector;
    }

    // list vector value

    @Specialization(guards = "isVectorListOrDataFrame(vector)")
    protected RAbstractContainer coerce(RList value, RAbstractContainer vector, Object operand) {
        return vector;
    }

    @Specialization(guards = "!isVectorList(vector)")
    protected RList coerce(RList value, RAbstractVector vector, Object operand) {
        return (RList) castList(vector);
    }

    // data frame value

    @Specialization
    protected RList coerce(RDataFrame value, RAbstractContainer vector, Object operand) {
        return (RList) castList(vector);
    }

    // factor value

    @Specialization
    protected Object coerce(RFactor value, RAbstractContainer vector, Object operand) {
        return coerceRecursive(value.getVector(), vector, operand);
    }

    // function vector value

    @Specialization
    protected RFunction coerce(RFunction value, RAbstractContainer vector, Object operand) {
        throw RError.error(this, RError.Message.SUBASSIGN_TYPE_FIX, "closure", RRuntime.classToString(vector.getElementClass(), false));
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

    @Specialization(guards = "isVectorList(vector)")
    protected RAbstractVector coerceList(RList value, RAbstractVector vector, Object operand) {
        return vector;
    }

    protected boolean isVectorList(RAbstractVector vector) {
        return vector instanceof RList;
    }

    protected boolean isVectorListOrDataFrame(RAbstractContainer vector) {
        return vector instanceof RList || vector.getElementClass() == RDataFrame.class;
    }

    @Override
    public RSyntaxNode getRSyntaxNode() {
        return getVector().asRSyntaxNode();
    }

}
