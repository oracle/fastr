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
package com.oracle.truffle.r.nodes.unary;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.env.*;

@SuppressWarnings("unused")
public abstract class TypeofNode extends UnaryNode {
    public static final String ARG_NAME = "x";

    private static final int MORE_THEN_2 = -1;

    @Child private ToStringNode toStringNode = null;

    public abstract String execute(VirtualFrame frame, Object x);

    @Specialization
    protected String typeof(RNull vector) {
        return "NULL";
    }

    @Specialization
    protected String typeof(byte x) {
        return "logical";
    }

    @Specialization
    protected String typeof(int s) {
        return "integer";
    }

    @Specialization
    protected String typeof(double x) {
        return "double";
    }

    @Specialization
    protected String typeof(RComplex x) {
        return "complex";
    }

    @Specialization
    protected String typeof(RRaw x) {
        return "raw";
    }

    @Specialization
    protected String typeof(String x) {
        return "character";
    }

    @Specialization
    protected String typeof(RIntSequence vector) {
        return "integer";
    }

    @Specialization
    protected String typeof(RLogicalVector vector) {
        return "logical";
    }

    @Specialization
    protected String typeof(RIntVector vector) {
        return "integer";
    }

    @Specialization
    protected String typeof(RDoubleVector vector) {
        return "double";
    }

    @Specialization
    protected String typeof(RStringVector vector) {
        return "character";
    }

    @Specialization
    protected String typeof(RComplexVector vector) {
        return "complex";
    }

    @Specialization
    protected String typeof(RRawVector vector) {
        return "raw";
    }

    @Specialization
    protected String typeof(RList list) {
        return "list";
    }

    @Specialization
    protected String typeof(REnvironment env) {
        return RRuntime.TYPE_ENVIRONMENT;
    }

    @Specialization
    protected String typeof(RSymbol symbol) {
        return "symbol";
    }

    @Specialization
    protected String typeof(RLanguage language) {
        return "language";
    }

    @Specialization
    protected String typeof(RPromise promise) {
        return "promise";
    }

    @Specialization
    protected String typeof(RExpression symbol) {
        return "expression";
    }

    @Specialization
    protected String typeof(RPairList pairlist) {
        return RRuntime.TYPE_PAIR_LIST;
    }

    @Specialization
    protected String typeofVarArgs0(RMissing args) {
        // RArgsValuesAndNames/"..." with length 0 is RMissing
        throw RError.error(getSourceSection(), RError.Message.ARGUMENT_MISSING, ARG_NAME);
    }

    @Specialization(guards = "isRArgsValuesAndNamesOfLength1")
    protected String typeofVarArgs1(VirtualFrame frame, RArgsValuesAndNames args) {
        // TODO Does Truffle allow recursive calling of nodes??
        return this.execute(frame, args.getValues()[0]);
    }

    @Specialization(guards = "isRArgsValuesAndNamesOfLength2")
    protected String typeofVarArgs2(VirtualFrame frame, RArgsValuesAndNames args) {
        throw RError.error(getSourceSection(), RError.Message.UNUSED_ARGUMENT, args.getNames()[1]);
    }

    @Specialization(guards = "isRArgsValuesAndNamesOfLengthGT2")
    @SlowPath
    protected String typeofVarArgsGT2(VirtualFrame frame, RArgsValuesAndNames args) {
        // More then 1 unused argument: Create argument string
        setupToString();
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < args.getNames().length; i++) {
            Object value = args.getValues()[i];
            if (value instanceof RPromise) {
                RPromise promise = (RPromise) value;
                RNode expr = (RNode) promise.getRep();
                b.append(expr.getSourceSection().toString());
            } else {
                b.append(toStringNode.execute(frame, value));
            }
            if (i < args.getNames().length - 1) {
                b.append(ToStringNode.DEFAULT_SEPARATOR);
            }
        }

        throw RError.error(getSourceSection(), RError.Message.UNUSED_ARGUMENTS, b.toString());
    }

    @Specialization(guards = "isFunctionBuiltin")
    protected String typeofBuiltin(RFunction obj) {
        return "builtin";
    }

    @Specialization(guards = "!isFunctionBuiltin")
    protected String typeofClosure(RFunction obj) {
        return "closure";
    }

    @Specialization
    protected String typeofFormula(RFormula f) {
        return "language";
    }

    private void setupToString() {
        if (toStringNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            toStringNode = insert(ToStringNodeFactory.create(null));
            toStringNode.setSeparator(ToStringNode.DEFAULT_SEPARATOR);
        }
    }

    public static boolean isFunctionBuiltin(RFunction fun) {
        return fun.isBuiltin();
    }

    public static boolean isRArgsValuesAndNamesOfLength1(RArgsValuesAndNames args) {
        return isRArgsValuesAndNamesOfLength(args, 1);
    }

    public static boolean isRArgsValuesAndNamesOfLength2(RArgsValuesAndNames args) {
        return isRArgsValuesAndNamesOfLength(args, 2);
    }

    public static boolean isRArgsValuesAndNamesOfLengthGT2(RArgsValuesAndNames args) {
        return isRArgsValuesAndNamesOfLength(args, MORE_THEN_2);
    }

    public static boolean isRArgsValuesAndNamesOfLength(RArgsValuesAndNames args, int len) {
        int actLength = args.length();
        return len == MORE_THEN_2 || actLength == len;
    }
}
