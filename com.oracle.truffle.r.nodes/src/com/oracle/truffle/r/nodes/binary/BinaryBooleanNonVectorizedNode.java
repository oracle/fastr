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
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.Node.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.nodes.binary.BinaryArithmeticNodeFactory.*;
import com.oracle.truffle.r.nodes.binary.BinaryBooleanNonVectorizedNodeFactory.LeftOpToLogicalScalarCastFactory;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.ops.*;
import com.oracle.truffle.r.runtime.ops.na.*;

import static com.oracle.truffle.r.runtime.RRuntime.*;

@SuppressWarnings("unused")
public abstract class BinaryBooleanNonVectorizedNode extends BinaryNode {

    private final BooleanOperationFactory factory;
    @Child private BooleanOperation logic;
    @Child private LeftOpToLogicalScalarCast leftOperandScalarCast;

    private final NACheck resultNACheck = NACheck.create();

    public BinaryBooleanNonVectorizedNode(BooleanOperationFactory factory) {
        this.factory = factory;
        this.logic = adoptChild(factory.create());
    }

    public BinaryBooleanNonVectorizedNode(BinaryBooleanNonVectorizedNode op) {
        this(op.factory);
    }

    @CreateCast({"arguments"})
    public RNode[] createCastLeft(RNode[] child) {
        return new RNode[]{createCast(child[0]), child[1]};
    }

    private LeftOpToLogicalScalarCast createCast(RNode child) {
        return LeftOpToLogicalScalarCastFactory.create(child, logic.opName());
    }

    @ShortCircuit("arguments[1]")
    public boolean needsRightOperand(Object leftValue) {
        return logic.requiresRightOperand(RTypesGen.RTYPES.asByte(leftValue));
    }

    @Specialization(order = 1, guards = "needsRightOperand")
    public byte doLogical(byte left, boolean needsRightOperand, int right) {
        return logic.op(RRuntime.logical2int(left), right);
    }

    @Specialization(order = 2, guards = "!needsRightOperand")
    public byte doLogicalOnlyLeft(byte left, boolean needsRightOperand, int right) {
        return left;
    }

    @Specialization(order = 3, guards = "needsRightOperand")
    public byte doLogical(byte left, boolean needsRightOperand, double right) {
        return logic.op(RRuntime.logical2double(left), right);
    }

    @Specialization(order = 4, guards = "!needsRightOperand")
    public byte doLogicalOnlyLeft(byte left, boolean needsRightOperand, double right) {
        return left;
    }

    @Specialization(order = 5, guards = "needsRightOperand")
    public byte doLogical(byte left, boolean needsRightOperand, byte right) {
        return logic.op(RRuntime.logical2double(left), RRuntime.logical2double(right));
    }

    @Specialization(order = 6, guards = "!needsRightOperand")
    public byte doLogicalOnlyLeft(byte left, boolean needsRightOperand, byte right) {
        return left;
    }

    @Specialization(order = 7, guards = "needsRightOperand")
    public byte doLogical(byte left, boolean needsRightOperand, String right) {
        return logic.op(RRuntime.logical2int(left), right);
    }

    @Specialization(order = 8, guards = "!needsRightOperand")
    public byte doLogicalOnlyLeft(byte left, boolean needsRightOperand, String right) {
        return left;
    }

    @Specialization(order = 9, guards = "needsRightOperand")
    public byte doLogical(byte left, boolean needsRightOperand, RComplex right) {
        return logic.op(RRuntime.logical2complex(left), right);
    }

    @Specialization(order = 10, guards = "!needsRightOperand")
    public byte doLogicalOnlyLeft(byte left, boolean needsRightOperand, RComplex right) {
        return left;
    }

    @Specialization(order = 11, guards = "needsRightOperand")
    public byte doLogical(Object left, boolean needsRightOperand, RRaw right) {
        return logic.op(left, right);
    }

    @Specialization(order = 12, guards = "!needsRightOperand")
    public byte doLogicalOnlyLeft(byte left, boolean needsRightOperand, RRaw right) {
        return left;
    }

    @Specialization(order = 13, guards = "needsRightOperand")
    public byte doLogical(Object left, boolean needsRightOperand, RNull right) {
        return logic.op(left, right);
    }

    @Specialization(order = 14, guards = "!needsRightOperand")
    public byte doLogicalOnlyLeft(byte left, boolean needsRightOperand, RNull right) {
        return left;
    }

    @Specialization(order = 15, guards = {"needsRightOperand", "!isZeroLength"})
    public byte doLogical(byte left, boolean needsRightOperand, RAbstractIntVector right) {
        return logic.op(RRuntime.logical2int(left), right.getDataAt(0));
    }

    @Specialization(order = 16, guards = {"needsRightOperand", "isZeroLength"})
    public byte doLogicalEmpty(byte left, boolean needsRightOperand, RAbstractIntVector right) {
        return logic.op(RRuntime.logical2int(left), RRuntime.INT_NA);
    }

    @Specialization(order = 17, guards = "!needsRightOperand")
    public byte doLogicalOnlyLeft(byte left, boolean needsRightOperand, RAbstractIntVector right) {
        return left;
    }

    @Specialization(order = 18, guards = {"needsRightOperand", "!isZeroLength"})
    public byte doLogical(byte left, boolean needsRightOperand, RAbstractDoubleVector right) {
        return logic.op(RRuntime.logical2double(left), right.getDataAt(0));
    }

    @Specialization(order = 19, guards = {"needsRightOperand", "isZeroLength"})
    public byte doLogicalEmpty(byte left, boolean needsRightOperand, RAbstractDoubleVector right) {
        return logic.op(RRuntime.logical2double(left), RRuntime.DOUBLE_NA);
    }

    @Specialization(order = 20, guards = "!needsRightOperand")
    public byte doLogicalOnlyLeft(byte left, boolean needsRightOperand, RAbstractDoubleVector right) {
        return left;
    }

    @Specialization(order = 21, guards = {"needsRightOperand", "!isZeroLength"})
    public byte doLogical(byte left, boolean needsRightOperand, RAbstractLogicalVector right) {
        return logic.op(RRuntime.logical2int(left), RRuntime.logical2int(right.getDataAt(0)));
    }

    @Specialization(order = 22, guards = {"needsRightOperand", "isZeroLength"})
    public byte doLogicalEmpty(byte left, boolean needsRightOperand, RAbstractLogicalVector right) {
        return logic.op(RRuntime.logical2int(left), RRuntime.INT_NA);
    }

    @Specialization(order = 23, guards = "!needsRightOperand")
    public byte doLogicalOnlyLeft(byte left, boolean needsRightOperand, RAbstractLogicalVector right) {
        return left;
    }

    @Specialization(order = 24, guards = {"needsRightOperand", "!isZeroLength"})
    public byte doLogical(byte left, boolean needsRightOperand, RAbstractStringVector right) {
        return logic.op(RRuntime.logical2int(left), right.getDataAt(0));
    }

    @Specialization(order = 25, guards = {"needsRightOperand", "isZeroLength"})
    public byte doLogicalEmpty(byte left, boolean needsRightOperand, RAbstractStringVector right) {
        return logic.op(RRuntime.logical2int(left), RRuntime.STRING_NA);
    }

    @Specialization(order = 26, guards = "!needsRightOperand")
    public byte doLogicalOnlyLeft(byte left, boolean needsRightOperand, RAbstractStringVector right) {
        return left;
    }

    @Specialization(order = 27, guards = {"needsRightOperand", "!isZeroLength"})
    public byte doLogical(byte left, boolean needsRightOperand, RAbstractComplexVector right) {
        return logic.op(RRuntime.logical2complex(left), right.getDataAt(0));
    }

    @Specialization(order = 28, guards = {"needsRightOperand", "isZeroLength"})
    public byte doLogicalEmpty(byte left, boolean needsRightOperand, RAbstractComplexVector right) {
        return logic.op(RRuntime.logical2complex(left), RRuntime.createComplexNA());
    }

    @Specialization(order = 29, guards = "!needsRightOperand")
    public byte doLogicalOnlyLeft(byte left, boolean needsRightOperand, RAbstractComplexVector right) {
        return left;
    }

    @Specialization(order = 30, guards = {"needsRightOperand", "!isZeroLength"})
    public byte doLogical(Object left, boolean needsRightOperand, RAbstractRawVector right) {
        return logic.op(left, right.getDataAt(0));
    }

    @Specialization(order = 31, guards = {"needsRightOperand", "isZeroLength"})
    public byte doLogicalEmpty(Object left, boolean needsRightOperand, RAbstractRawVector right) {
        throw RError.getInvalidTypeIn(getEncapsulatingSourceSection(), "y", logic.opName());
    }

    @Specialization(order = 32, guards = "!needsRightOperand")
    public byte doLogicalOnlyLeft(byte left, boolean needsRightOperand, RAbstractRawVector right) {
        return left;
    }

    boolean isZeroLength(byte left, boolean needsRightOperand, RAbstractVector operand) {
        return operand.getLength() == 0;
    }

    boolean isZeroLength(Object left, boolean needsRightOperand, RAbstractVector operand) {
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
        public byte doLogical(int operand) {
            return RRuntime.int2logical(operand);
        }

        @Specialization
        public byte doLogical(double operand) {
            return RRuntime.double2logical(operand);
        }

        @Specialization
        public byte doLogical(RComplex operand) {
            return RRuntime.complex2logical(operand);
        }

        @Specialization
        public byte doLogical(byte operand) {
            return operand;
        }

        @Specialization
        public byte doLogical(String operand) {
            throw RError.getInvalidTypeIn(getEncapsulatingSourceSection(), "x", getOpName());
        }

        @Specialization
        public byte doLogical(RRaw operand) {
            throw RError.getInvalidTypeIn(getEncapsulatingSourceSection(), "x", getOpName());
        }

        @Specialization
        public byte doLogical(RNull operand) {
            throw RError.getInvalidTypeIn(getEncapsulatingSourceSection(), "x", getOpName());
        }

        @Specialization(guards = "isZeroLength")
        public byte doLogical(RAbstractVector operand) {
            return RRuntime.LOGICAL_NA;
        }

        @Specialization(guards = "!isZeroLength")
        public byte doLogical(RAbstractDoubleVector operand) {
            return RRuntime.double2logical(operand.getDataAt(0));
        }

        @Specialization(guards = "!isZeroLength")
        public byte doLogical(RAbstractComplexVector operand) {
            return RRuntime.complex2logical(operand.getDataAt(0));
        }

        @Specialization(guards = "!isZeroLength")
        public byte doLogical(RAbstractLogicalVector operand) {
            return operand.getDataAt(0);
        }

        @Specialization
        public byte doLogical(RAbstractStringVector operand) {
            throw RError.getInvalidTypeIn(getEncapsulatingSourceSection(), "x", getOpName());
        }

        @Specialization
        public byte doLogical(RAbstractRawVector operand) {
            throw RError.getInvalidTypeIn(getEncapsulatingSourceSection(), "x", getOpName());
        }

        @Specialization(guards = "!isZeroLength")
        public byte doLogical(RAbstractIntVector operand) {
            return RRuntime.int2logical(operand.getDataAt(0));
        }

        boolean isZeroLength(RAbstractVector operand) {
            return operand.getLength() == 0;
        }
    }
}
