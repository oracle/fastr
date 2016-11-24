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
package com.oracle.truffle.r.nodes.binary;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.r.nodes.primitive.BinaryMapNAFunctionNode;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RLogical;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.ops.BinaryLogic.And;
import com.oracle.truffle.r.runtime.ops.BinaryLogic.Or;
import com.oracle.truffle.r.runtime.ops.BooleanOperation;
import com.oracle.truffle.r.runtime.ops.Operation;

public final class BinaryMapBooleanFunctionNode extends BinaryMapNAFunctionNode {

    @Child private BooleanOperation operation;

    public BinaryMapBooleanFunctionNode(BooleanOperation arithmetic) {
        this.operation = arithmetic;
    }

    @Override
    public boolean mayFoldConstantTime(Class<? extends RAbstractVector> left, Class<? extends RAbstractVector> right) {
        return super.mayFoldConstantTime(left, right);
    }

    @Override
    public RAbstractVector tryFoldConstantTime(RAbstractVector left, int leftLength, RAbstractVector right, int rightLength) {

        return super.tryFoldConstantTime(left, leftLength, right, rightLength);
    }

    @Override
    protected boolean resultNeedsNACheck() {
        return false;
    }

    @Override
    public byte applyLogical(byte left, byte right) {
        assert RLogical.isValid(left);
        assert RLogical.isValid(right);
        if (leftNACheck.check(left)) {
            if (operation instanceof And && right == 0) {
                return RRuntime.LOGICAL_FALSE;
            } else if (operation instanceof Or && right == RRuntime.LOGICAL_TRUE) {
                return RRuntime.LOGICAL_TRUE;
            }
            return RRuntime.LOGICAL_NA;
        }
        if (rightNACheck.check(right)) {
            if (operation instanceof And && left == 0) {
                return RRuntime.LOGICAL_FALSE;
            } else if (operation instanceof Or && left == RRuntime.LOGICAL_TRUE) {
                return RRuntime.LOGICAL_TRUE;
            }
            return RRuntime.LOGICAL_NA;
        }
        try {
            return RRuntime.asLogical(operation.opLogical(left, right));
        } catch (Throwable e) {
            CompilerDirectives.transferToInterpreter();
            throw Operation.handleException(e);
        }
    }

    @Override
    public byte applyLogical(int left, int right) {
        if (leftNACheck.check(left)) {
            if (operation instanceof And && right == 0) {
                return RRuntime.LOGICAL_FALSE;
            } else if (operation instanceof Or && !rightNACheck.check(right) && right != 0) {
                return RRuntime.LOGICAL_TRUE;
            }
            return RRuntime.LOGICAL_NA;
        }
        if (rightNACheck.check(right)) {
            if (operation instanceof And && left == 0) {
                return RRuntime.LOGICAL_FALSE;
            } else if (operation instanceof Or && left != 0) {
                return RRuntime.LOGICAL_TRUE;
            }
            return RRuntime.LOGICAL_NA;
        }
        try {
            return RRuntime.asLogical(operation.op(left, right));
        } catch (Throwable e) {
            CompilerDirectives.transferToInterpreter();
            throw Operation.handleException(e);
        }
    }

    @Override
    public byte applyLogical(double left, double right) {
        if (leftNACheck.checkNAorNaN(left)) {
            if (operation instanceof And && right == 0.0) {
                return RRuntime.LOGICAL_FALSE;
            } else if (operation instanceof Or && !rightNACheck.checkNAorNaN(right) && right != 0.0) {
                return RRuntime.LOGICAL_TRUE;
            }
            return RRuntime.LOGICAL_NA;
        }
        if (rightNACheck.checkNAorNaN(right)) {
            if (operation instanceof And && left == 0.0) {
                return RRuntime.LOGICAL_FALSE;
            } else if (operation instanceof Or && left != 0.0) {
                return RRuntime.LOGICAL_TRUE;
            }
            return RRuntime.LOGICAL_NA;
        }
        try {
            return RRuntime.asLogical(operation.op(left, right));
        } catch (Throwable e) {
            CompilerDirectives.transferToInterpreter();
            throw Operation.handleException(e);
        }
    }

    @Override
    public byte applyLogical(RComplex left, RComplex right) {
        if (leftNACheck.check(left)) {
            if (operation instanceof And && right.isZero()) {
                return RRuntime.LOGICAL_FALSE;
            } else if (operation instanceof Or && !rightNACheck.check(right) && !right.isZero()) {
                return RRuntime.LOGICAL_TRUE;
            }
            return RRuntime.LOGICAL_NA;
        }
        if (rightNACheck.check(right)) {
            if (operation instanceof And && left.isZero()) {
                return RRuntime.LOGICAL_FALSE;
            } else if (operation instanceof Or && !left.isZero()) {
                return RRuntime.LOGICAL_TRUE;
            }
            return RRuntime.LOGICAL_NA;
        }
        try {
            return RRuntime.asLogical(operation.op(left, right));
        } catch (Throwable e) {
            CompilerDirectives.transferToInterpreter();
            throw Operation.handleException(e);
        }
    }

    @Override
    public byte applyLogical(String left, String right) {
        if (leftNACheck.check(left)) {
            return RRuntime.LOGICAL_NA;
        }
        if (rightNACheck.check(right)) {
            return RRuntime.LOGICAL_NA;
        }
        try {
            return RRuntime.asLogical(operation.op(left, right));
        } catch (Throwable e) {
            CompilerDirectives.transferToInterpreter();
            throw Operation.handleException(e);
        }
    }

    @Override
    public byte applyRaw(byte left, byte right) {
        try {
            return operation.opRaw(left, right);
        } catch (Throwable e) {
            CompilerDirectives.transferToInterpreter();
            throw Operation.handleException(e);
        }
    }

    boolean requiresRightOperand(byte left) {
        if (leftNACheck.check(left)) {
            return true;
        }
        return operation.requiresRightOperand(left);
    }
}
