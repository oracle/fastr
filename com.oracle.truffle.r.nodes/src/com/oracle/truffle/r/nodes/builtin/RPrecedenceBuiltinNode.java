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
package com.oracle.truffle.r.nodes.builtin;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

public abstract class RPrecedenceBuiltinNode extends RBuiltinNode {

    @Child private PrecedenceNode precedenceNode = PrecedenceNodeGen.create(null, null);

    @Child private CastComplexNode castComplex;
    @Child private CastDoubleNode castDouble;
    @Child private CastIntegerNode castInteger;
    @Child private CastLogicalNode castLogical;
    @Child private CastStringNode castString;
    @Child private CastRawNode castRaw;
    @Child private CastListNode castList;

    protected Object castComplex(VirtualFrame frame, Object operand, boolean preserveAllAttr) {
        if (castComplex == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castComplex = insert(CastComplexNodeGen.create(null, true, preserveAllAttr, preserveAllAttr));
        }
        return castComplex.executeCast(frame, operand);
    }

    protected Object castDouble(VirtualFrame frame, Object operand, boolean preserveAllAttr) {
        if (castDouble == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castDouble = insert(CastDoubleNodeGen.create(null, true, preserveAllAttr, preserveAllAttr));
        }
        return castDouble.executeCast(frame, operand);
    }

    protected Object castInteger(VirtualFrame frame, Object operand, boolean preserveAllAttr) {
        if (castInteger == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castInteger = insert(CastIntegerNodeGen.create(null, true, preserveAllAttr, preserveAllAttr));
        }
        return castInteger.executeCast(frame, operand);
    }

    protected Object castLogical(VirtualFrame frame, Object operand, boolean preserveAllAttr) {
        if (castLogical == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castLogical = insert(CastLogicalNodeGen.create(null, true, preserveAllAttr, preserveAllAttr));
        }
        return castLogical.executeCast(frame, operand);
    }

    protected Object castString(VirtualFrame frame, Object operand, boolean preserveAllAttr) {
        if (castString == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castString = insert(CastStringNodeGen.create(null, false, true, preserveAllAttr, preserveAllAttr));
        }
        return castString.executeCast(frame, operand);
    }

    protected Object castRaw(VirtualFrame frame, Object operand, boolean preserveAllAttr) {
        if (castRaw == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castRaw = insert(CastRawNodeGen.create(null, true, preserveAllAttr, preserveAllAttr));
        }
        return castRaw.executeCast(frame, operand);
    }

    protected RList castList(VirtualFrame frame, Object operand, boolean preserveAllAttr) {
        if (castList == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castList = insert(CastListNodeGen.create(null, true, preserveAllAttr, preserveAllAttr));
        }
        return castList.executeList(frame, operand);
    }

    private int precedence(VirtualFrame frame, RArgsValuesAndNames args) {
        int precedence = -1;
        Object[] array = args.getValues();
        for (int i = 0; i < array.length; i++) {
            precedence = Math.max(precedence, precedenceNode.executeInteger(frame, array[i], RRuntime.LOGICAL_FALSE));
        }
        return precedence;
    }

    protected boolean isIntegerPrecedence(VirtualFrame frame, RArgsValuesAndNames args) {
        return precedence(frame, args) == PrecedenceNode.INT_PRECEDENCE;
    }

    protected boolean isLogicalPrecedence(VirtualFrame frame, RArgsValuesAndNames args) {
        return precedence(frame, args) == PrecedenceNode.LOGICAL_PRECEDENCE;
    }

    protected boolean isDoublePrecedence(VirtualFrame frame, RArgsValuesAndNames args) {
        return precedence(frame, args) == PrecedenceNode.DOUBLE_PRECEDENCE;
    }

    protected boolean isComplexPrecedence(VirtualFrame frame, RArgsValuesAndNames args) {
        return precedence(frame, args) == PrecedenceNode.COMPLEX_PRECEDENCE;
    }

    protected boolean isStringPrecedence(VirtualFrame frame, RArgsValuesAndNames args) {
        return precedence(frame, args) == PrecedenceNode.STRING_PRECEDENCE;
    }

    protected boolean isRawPrecedence(VirtualFrame frame, RArgsValuesAndNames args) {
        return precedence(frame, args) == PrecedenceNode.RAW_PRECEDENCE;
    }

    protected boolean isNullPrecedence(VirtualFrame frame, RArgsValuesAndNames args) {
        return precedence(frame, args) == PrecedenceNode.NO_PRECEDENCE;
    }

    protected boolean isListPrecedence(VirtualFrame frame, RArgsValuesAndNames args) {
        return precedence(frame, args) == PrecedenceNode.LIST_PRECEDENCE;
    }

    protected boolean isExprPrecedence(VirtualFrame frame, RArgsValuesAndNames args) {
        return precedence(frame, args) == PrecedenceNode.EXPRESSION_PRECEDENCE;
    }

}
