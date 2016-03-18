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
package com.oracle.truffle.r.nodes.builtin;

import com.oracle.truffle.r.nodes.unary.PrecedenceNode;
import com.oracle.truffle.r.nodes.unary.PrecedenceNodeGen;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;

public abstract class RPrecedenceBuiltinNode extends RBuiltinNode {

    @Child private PrecedenceNode precedenceNode = PrecedenceNodeGen.create();

    private int precedence(RArgsValuesAndNames args) {
        int precedence = -1;
        Object[] array = args.getArguments();
        for (int i = 0; i < array.length; i++) {
            precedence = Math.max(precedence, precedenceNode.executeInteger(array[i], RRuntime.LOGICAL_FALSE));
        }
        return precedence;
    }

    protected boolean isIntegerPrecedence(RArgsValuesAndNames args) {
        return precedence(args) == PrecedenceNode.INT_PRECEDENCE;
    }

    protected boolean isLogicalPrecedence(RArgsValuesAndNames args) {
        return precedence(args) == PrecedenceNode.LOGICAL_PRECEDENCE;
    }

    protected boolean isDoublePrecedence(RArgsValuesAndNames args) {
        return precedence(args) == PrecedenceNode.DOUBLE_PRECEDENCE;
    }

    protected boolean isComplexPrecedence(RArgsValuesAndNames args) {
        return precedence(args) == PrecedenceNode.COMPLEX_PRECEDENCE;
    }

    protected boolean isStringPrecedence(RArgsValuesAndNames args) {
        return precedence(args) == PrecedenceNode.STRING_PRECEDENCE;
    }

    protected boolean isRawPrecedence(RArgsValuesAndNames args) {
        return precedence(args) == PrecedenceNode.RAW_PRECEDENCE;
    }

    protected boolean isNullPrecedence(RArgsValuesAndNames args) {
        return precedence(args) == PrecedenceNode.NO_PRECEDENCE;
    }

    protected boolean isListPrecedence(RArgsValuesAndNames args) {
        return precedence(args) == PrecedenceNode.LIST_PRECEDENCE;
    }

    protected boolean isExprPrecedence(RArgsValuesAndNames args) {
        return precedence(args) == PrecedenceNode.EXPRESSION_PRECEDENCE;
    }

    protected boolean isIntegerPrecedence(Object arg) {
        return precedenceNode.executeInteger(arg, RRuntime.LOGICAL_FALSE) == PrecedenceNode.INT_PRECEDENCE;
    }

    protected boolean isLogicalPrecedence(Object arg) {
        return precedenceNode.executeInteger(arg, RRuntime.LOGICAL_FALSE) == PrecedenceNode.LOGICAL_PRECEDENCE;
    }

    protected boolean isDoublePrecedence(Object arg) {
        return precedenceNode.executeInteger(arg, RRuntime.LOGICAL_FALSE) == PrecedenceNode.DOUBLE_PRECEDENCE;
    }

    protected boolean isComplexPrecedence(Object arg) {
        return precedenceNode.executeInteger(arg, RRuntime.LOGICAL_FALSE) == PrecedenceNode.COMPLEX_PRECEDENCE;
    }

    protected boolean isStringPrecedence(Object arg) {
        return precedenceNode.executeInteger(arg, RRuntime.LOGICAL_FALSE) == PrecedenceNode.STRING_PRECEDENCE;
    }

    protected boolean isRawPrecedence(Object arg) {
        return precedenceNode.executeInteger(arg, RRuntime.LOGICAL_FALSE) == PrecedenceNode.RAW_PRECEDENCE;
    }

    protected boolean isNullPrecedence(Object arg) {
        return precedenceNode.executeInteger(arg, RRuntime.LOGICAL_FALSE) == PrecedenceNode.NO_PRECEDENCE;
    }

    protected boolean isListPrecedence(Object arg) {
        return precedenceNode.executeInteger(arg, RRuntime.LOGICAL_FALSE) == PrecedenceNode.LIST_PRECEDENCE;
    }

    protected boolean isExprPrecedence(Object arg) {
        return precedenceNode.executeInteger(arg, RRuntime.LOGICAL_FALSE) == PrecedenceNode.EXPRESSION_PRECEDENCE;
    }
}
