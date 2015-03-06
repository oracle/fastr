/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.binary;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.Node.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.nodes.binary.BinaryArithmeticNodeFactory.*;
import com.oracle.truffle.r.nodes.binary.BinaryBooleanNonVectorizedNodeFactory.LeftOpToLogicalScalarCastNodeGen;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.ops.*;
import com.oracle.truffle.r.runtime.ops.na.*;

import static com.oracle.truffle.r.runtime.RRuntime.*;

@SuppressWarnings("unused")
public abstract class BinaryBooleanNonVectorizedNode extends RBuiltinNode {

    private final BooleanOperationFactory factory;
    @Child private BooleanOperation logic;
    @Child private LeftOpToLogicalScalarCast leftOperandScalarCast;

    private final NACheck resultNACheck = NACheck.create();

    public BinaryBooleanNonVectorizedNode(BooleanOperationFactory factory) {
        this.factory = factory;
        this.logic = factory.create();
    }

    public BinaryBooleanNonVectorizedNode(BinaryBooleanNonVectorizedNode op) {
        this(op.factory);
    }

    @CreateCast({"arguments"})
    public RNode[] createCastLeft(RNode[] child) {
        return new RNode[]{createCast(child[0]), child[1]};
    }

    private LeftOpToLogicalScalarCast createCast(RNode child) {
        return LeftOpToLogicalScalarCastNodeGen.create(child, logic.opName());
    }

    @ShortCircuit("arguments[1]")
    protected boolean needsRightOperand(Object leftValue) {
        return logic.requiresRightOperand(RTypesGen.asByte(leftValue));
    }

    @Specialization(guards = "needsRightOperand")
    protected byte doLogical(byte left, boolean needsRightOperand, int right) {
        return logic.op(RRuntime.logical2int(left), right);
    }

    @Specialization(guards = "!needsRightOperand")
    protected byte doLogicalOnlyLeft(byte left, boolean needsRightOperand, int right) {
        return left;
    }

    @Specialization(guards = "needsRightOperand")
    protected byte doLogical(byte left, boolean needsRightOperand, double right) {
        return logic.op(RRuntime.logical2double(left), right);
    }

    @Specialization(guards = "!needsRightOperand")
    protected byte doLogicalOnlyLeft(byte left, boolean needsRightOperand, double right) {
        return left;
    }

    @Specialization(guards = "needsRightOperand")
    protected byte doLogical(byte left, boolean needsRightOperand, byte right) {
        return logic.op(RRuntime.logical2int(left), RRuntime.logical2int(right));
    }

    @Specialization(guards = "!needsRightOperand")
    protected byte doLogicalOnlyLeft(byte left, boolean needsRightOperand, byte right) {
        return left;
    }

    @Specialization(guards = "needsRightOperand")
    protected byte doLogical(byte left, boolean needsRightOperand, String right) {
        return logic.op(RRuntime.logical2int(left), right);
    }

    @Specialization(guards = "!needsRightOperand")
    protected byte doLogicalOnlyLeft(byte left, boolean needsRightOperand, String right) {
        return left;
    }

    @Specialization(guards = "needsRightOperand")
    protected byte doLogical(byte left, boolean needsRightOperand, RComplex right) {
        return logic.op(RRuntime.logical2complex(left), right);
    }

    @Specialization(guards = "!needsRightOperand")
    protected byte doLogicalOnlyLeft(byte left, boolean needsRightOperand, RComplex right) {
        return left;
    }

    @Specialization(guards = "needsRightOperand")
    protected byte doLogical(Object left, boolean needsRightOperand, RRaw right) {
        return logic.op(left, right);
    }

    @Specialization(guards = "!needsRightOperand")
    protected byte doLogicalOnlyLeft(byte left, boolean needsRightOperand, RRaw right) {
        return left;
    }

    @Specialization(guards = "needsRightOperand")
    protected byte doLogical(Object left, boolean needsRightOperand, RNull right) {
        return logic.op(left, right);
    }

    @Specialization(guards = "!needsRightOperand")
    protected byte doLogicalOnlyLeft(byte left, boolean needsRightOperand, RNull right) {
        return left;
    }

    @Specialization(guards = {"needsRightOperand", "right.getLength() != 0"})
    protected byte doLogical(byte left, boolean needsRightOperand, RAbstractIntVector right) {
        return logic.op(RRuntime.logical2int(left), right.getDataAt(0));
    }

    @Specialization(guards = {"needsRightOperand", "right.getLength() == 0"})
    protected byte doLogicalEmpty(byte left, boolean needsRightOperand, RAbstractIntVector right) {
        return logic.op(RRuntime.logical2int(left), RRuntime.INT_NA);
    }

    @Specialization(guards = "!needsRightOperand")
    protected byte doLogicalOnlyLeft(byte left, boolean needsRightOperand, RAbstractIntVector right) {
        return left;
    }

    @Specialization(guards = {"needsRightOperand", "right.getLength() != 0"})
    protected byte doLogical(byte left, boolean needsRightOperand, RAbstractDoubleVector right) {
        return logic.op(RRuntime.logical2double(left), right.getDataAt(0));
    }

    @Specialization(guards = {"needsRightOperand", "right.getLength() == 0"})
    protected byte doLogicalEmpty(byte left, boolean needsRightOperand, RAbstractDoubleVector right) {
        return logic.op(RRuntime.logical2double(left), RRuntime.DOUBLE_NA);
    }

    @Specialization(guards = "!needsRightOperand")
    protected byte doLogicalOnlyLeft(byte left, boolean needsRightOperand, RAbstractDoubleVector right) {
        return left;
    }

    @Specialization(guards = {"needsRightOperand", "right.getLength() != 0"})
    protected byte doLogical(byte left, boolean needsRightOperand, RAbstractLogicalVector right) {
        return logic.op(RRuntime.logical2int(left), RRuntime.logical2int(right.getDataAt(0)));
    }

    @Specialization(guards = {"needsRightOperand", "right.getLength() == 0"})
    protected byte doLogicalEmpty(byte left, boolean needsRightOperand, RAbstractLogicalVector right) {
        return logic.op(RRuntime.logical2int(left), RRuntime.INT_NA);
    }

    @Specialization(guards = "!needsRightOperand")
    protected byte doLogicalOnlyLeft(byte left, boolean needsRightOperand, RAbstractLogicalVector right) {
        return left;
    }

    @Specialization(guards = {"needsRightOperand", "right.getLength() != 0"})
    protected byte doLogical(byte left, boolean needsRightOperand, RAbstractStringVector right) {
        return logic.op(RRuntime.logical2int(left), right.getDataAt(0));
    }

    @Specialization(guards = {"needsRightOperand", "right.getLength() == 0"})
    protected byte doLogicalEmpty(byte left, boolean needsRightOperand, RAbstractStringVector right) {
        return logic.op(RRuntime.logical2int(left), RRuntime.STRING_NA);
    }

    @Specialization(guards = "!needsRightOperand")
    protected byte doLogicalOnlyLeft(byte left, boolean needsRightOperand, RAbstractStringVector right) {
        return left;
    }

    @Specialization(guards = {"needsRightOperand", "right.getLength() != 0"})
    protected byte doLogical(byte left, boolean needsRightOperand, RAbstractComplexVector right) {
        return logic.op(RRuntime.logical2complex(left), right.getDataAt(0));
    }

    @Specialization(guards = {"needsRightOperand", "right.getLength() == 0"})
    protected byte doLogicalEmpty(byte left, boolean needsRightOperand, RAbstractComplexVector right) {
        return logic.op(RRuntime.logical2complex(left), RRuntime.createComplexNA());
    }

    @Specialization(guards = "!needsRightOperand")
    protected byte doLogicalOnlyLeft(byte left, boolean needsRightOperand, RAbstractComplexVector right) {
        return left;
    }

    @Specialization(guards = {"needsRightOperand", "right.getLength() != 0"})
    protected byte doLogical(Object left, boolean needsRightOperand, RAbstractRawVector right) {
        return logic.op(left, right.getDataAt(0));
    }

    @Specialization(guards = {"needsRightOperand", "right.getLength() == 0"})
    protected byte doLogicalEmpty(Object left, boolean needsRightOperand, RAbstractRawVector right) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_TYPE_IN, "y", logic.opName());
    }

    @Specialization(guards = "!needsRightOperand")
    protected byte doLogicalOnlyLeft(byte left, boolean needsRightOperand, RAbstractRawVector right) {
        return left;
    }

    protected boolean isZeroLength(byte left, boolean needsRightOperand, RAbstractVector operand) {
        return operand.getLength() == 0;
    }

    protected boolean isZeroLength(Object left, boolean needsRightOperand, RAbstractVector operand) {
        return operand.getLength() == 0;
    }

    protected boolean needsRightOperand(Object left, boolean needsRightOperand, Object right) {
        return needsRightOperand;
    }

    @NodeField(name = "opName", type = String.class)
    @NodeChild("operand")
    public abstract static class LeftOpToLogicalScalarCast extends RNode {

        public abstract byte executeCast(VirtualFrame frame, Object o);

        public abstract String getOpName();

        @Specialization
        protected byte doLogical(int operand) {
            return RRuntime.int2logical(operand);
        }

        @Specialization
        protected byte doLogical(double operand) {
            return RRuntime.double2logical(operand);
        }

        @Specialization
        protected byte doLogical(RComplex operand) {
            return RRuntime.complex2logical(operand);
        }

        @Specialization
        protected byte doLogical(byte operand) {
            return operand;
        }

        @Specialization
        protected byte doLogical(String operand) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_TYPE_IN, "x", getOpName());
        }

        @Specialization
        protected byte doLogical(RRaw operand) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_TYPE_IN, "x", getOpName());
        }

        @Specialization
        protected byte doLogical(RNull operand) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_TYPE_IN, "x", getOpName());
        }

        @Specialization(guards = {"operand.getLength() == 0", "!isStringVector(operand)", "!isRawVector(operand)"})
        protected byte doLogical(RAbstractVector operand) {
            return RRuntime.LOGICAL_NA;
        }

        @Specialization(guards = "operand.getLength() != 0")
        protected byte doLogical(RAbstractDoubleVector operand) {
            return RRuntime.double2logical(operand.getDataAt(0));
        }

        @Specialization(guards = "operand.getLength() != 0")
        protected byte doLogical(RAbstractComplexVector operand) {
            return RRuntime.complex2logical(operand.getDataAt(0));
        }

        @Specialization(guards = "operand.getLength() != 0")
        protected byte doLogical(RAbstractLogicalVector operand) {
            return operand.getDataAt(0);
        }

        @Specialization
        protected byte doLogical(RAbstractStringVector operand) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_TYPE_IN, "x", getOpName());
        }

        @Specialization
        protected byte doLogical(RAbstractRawVector operand) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_TYPE_IN, "x", getOpName());
        }

        @Specialization(guards = "operand.getLength() != 0")
        protected byte doLogical(RAbstractIntVector operand) {
            return RRuntime.int2logical(operand.getDataAt(0));
        }

        protected static boolean isStringVector(RAbstractVector vector) {
            return vector.getElementClass() == RString.class;
        }

        protected static boolean isRawVector(RAbstractVector vector) {
            return vector.getElementClass() == RRaw.class;
        }
    }
}
