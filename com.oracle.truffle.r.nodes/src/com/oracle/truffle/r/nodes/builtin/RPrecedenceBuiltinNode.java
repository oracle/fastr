/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

public abstract class RPrecedenceBuiltinNode extends RCastingBuiltinNode {

    @Child private PrecedenceNode precedenceNode = PrecedenceNodeGen.create(null, null);

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

    protected boolean isIntegerPrecedence(VirtualFrame frame, Object arg) {
        return precedenceNode.executeInteger(frame, arg, RRuntime.LOGICAL_FALSE) == PrecedenceNode.INT_PRECEDENCE;
    }

    protected boolean isLogicalPrecedence(VirtualFrame frame, Object arg) {
        return precedenceNode.executeInteger(frame, arg, RRuntime.LOGICAL_FALSE) == PrecedenceNode.LOGICAL_PRECEDENCE;
    }

    protected boolean isDoublePrecedence(VirtualFrame frame, Object arg) {
        return precedenceNode.executeInteger(frame, arg, RRuntime.LOGICAL_FALSE) == PrecedenceNode.DOUBLE_PRECEDENCE;
    }

    protected boolean isComplexPrecedence(VirtualFrame frame, Object arg) {
        return precedenceNode.executeInteger(frame, arg, RRuntime.LOGICAL_FALSE) == PrecedenceNode.COMPLEX_PRECEDENCE;
    }

    protected boolean isStringPrecedence(VirtualFrame frame, Object arg) {
        return precedenceNode.executeInteger(frame, arg, RRuntime.LOGICAL_FALSE) == PrecedenceNode.STRING_PRECEDENCE;
    }

    protected boolean isRawPrecedence(VirtualFrame frame, Object arg) {
        return precedenceNode.executeInteger(frame, arg, RRuntime.LOGICAL_FALSE) == PrecedenceNode.RAW_PRECEDENCE;
    }

    protected boolean isNullPrecedence(VirtualFrame frame, Object arg) {
        return precedenceNode.executeInteger(frame, arg, RRuntime.LOGICAL_FALSE) == PrecedenceNode.NO_PRECEDENCE;
    }

    protected boolean isListPrecedence(VirtualFrame frame, Object arg) {
        return precedenceNode.executeInteger(frame, arg, RRuntime.LOGICAL_FALSE) == PrecedenceNode.LIST_PRECEDENCE;
    }

    protected boolean isExprPrecedence(VirtualFrame frame, Object arg) {
        return precedenceNode.executeInteger(frame, arg, RRuntime.LOGICAL_FALSE) == PrecedenceNode.EXPRESSION_PRECEDENCE;
    }

}
