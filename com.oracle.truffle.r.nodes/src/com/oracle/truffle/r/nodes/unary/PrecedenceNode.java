/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RComplexVector;
import com.oracle.truffle.r.runtime.data.RDataFrame;
import com.oracle.truffle.r.runtime.data.RDoubleSequence;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RExpression;
import com.oracle.truffle.r.runtime.data.RFactor;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RIntSequence;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RLanguage;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RRaw;
import com.oracle.truffle.r.runtime.data.RRawVector;
import com.oracle.truffle.r.runtime.data.RS4Object;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

@SuppressWarnings("unused")
@ImportStatic(RRuntime.class)
public abstract class PrecedenceNode extends RBaseNode {

    public static final int NO_PRECEDENCE = -1;
    public static final int RAW_PRECEDENCE = 0;
    public static final int LOGICAL_PRECEDENCE = 1;
    public static final int INT_PRECEDENCE = 2;
    public static final int DOUBLE_PRECEDENCE = 3;
    public static final int COMPLEX_PRECEDENCE = 4;
    public static final int STRING_PRECEDENCE = 5;
    public static final int LIST_PRECEDENCE = 6;
    public static final int EXPRESSION_PRECEDENCE = 7;

    public static final int NUMBER_OF_PRECEDENCES = 9;

    public abstract int executeInteger(Object object, byte recursive);

    @Specialization
    protected int doNull(RNull val, byte recursive) {
        return NO_PRECEDENCE;
    }

    @Specialization
    protected int doRaw(RRaw val, byte recursive) {
        return RAW_PRECEDENCE;
    }

    @Specialization
    protected int doRawVector(RRawVector val, byte recursive) {
        return RAW_PRECEDENCE;
    }

    @Specialization
    protected int doLogical(byte val, byte recursive) {
        return LOGICAL_PRECEDENCE;
    }

    @Specialization
    protected int doLogical(RLogicalVector val, byte recursive) {
        return LOGICAL_PRECEDENCE;
    }

    @Specialization
    protected int doInt(int val, byte recursive) {
        return INT_PRECEDENCE;
    }

    @Specialization
    protected int doComplex(RComplex val, byte recursive) {
        return COMPLEX_PRECEDENCE;
    }

    @Specialization
    protected int doInt(RIntVector val, byte recursive) {
        return INT_PRECEDENCE;
    }

    @Specialization
    protected int doInt(RIntSequence val, byte recursive) {
        return INT_PRECEDENCE;
    }

    @Specialization
    protected int doDouble(double val, byte recursive) {
        return DOUBLE_PRECEDENCE;
    }

    @Specialization
    protected int doDouble(RDoubleVector val, byte recursive) {
        return DOUBLE_PRECEDENCE;
    }

    @Specialization
    protected int doDouble(RDoubleSequence val, byte recursive) {
        return DOUBLE_PRECEDENCE;
    }

    @Specialization
    protected int doComplex(RComplexVector val, byte recursive) {
        return COMPLEX_PRECEDENCE;
    }

    @Specialization
    protected int doString(String val, byte recursive) {
        return STRING_PRECEDENCE;
    }

    @Specialization
    protected int doString(RStringVector val, byte recursive) {
        return STRING_PRECEDENCE;
    }

    @Specialization
    protected int doFunction(RFunction func, byte recursive) {
        return LIST_PRECEDENCE;
    }

    @Specialization
    protected int doEnvironment(REnvironment env, byte recursive) {
        return LIST_PRECEDENCE;
    }

    @Specialization(guards = "recursive == LOGICAL_TRUE")
    protected int doListRecursive(RList val, byte recursive, //
                    @Cached("createRecursive()") PrecedenceNode precedenceNode) {
        int precedence = -1;
        for (int i = 0; i < val.getLength(); i++) {
            Object data = val.getDataAt(i);
            precedence = Math.max(precedence, precedenceNode.executeInteger(val.getDataAtAsObject(i), recursive));
        }
        return precedence;
    }

    protected static PrecedenceNode createRecursive() {
        return PrecedenceNodeGen.create();
    }

    @Specialization(guards = "recursive != LOGICAL_TRUE")
    protected int doList(RList val, byte recursive) {
        return LIST_PRECEDENCE;
    }

    @Specialization(guards = "recursive != LOGICAL_TRUE")
    protected int doPairList(RPairList val, byte recursive) {
        return LIST_PRECEDENCE;
    }

    @Specialization
    protected int doDataFrame(RDataFrame val, byte recursive) {
        return LIST_PRECEDENCE;
    }

    @Specialization
    protected int doExpression(RExpression val, byte recursive) {
        return EXPRESSION_PRECEDENCE;
    }

    @Specialization
    protected int doExpression(RLanguage val, byte recursive) {
        return EXPRESSION_PRECEDENCE;
    }

    @Specialization
    protected int doFactor(RFactor val, byte recursive) {
        return INT_PRECEDENCE;
    }

    @Specialization
    protected int doS4Object(RS4Object o, byte recursive) {
        return LIST_PRECEDENCE;
    }

    @Specialization
    protected int doS4Object(RSymbol o, byte recursive) {
        return LIST_PRECEDENCE;
    }

    @Specialization(guards = {"recursive == LOGICAL_FALSE", "args.getLength() == 1"})
    protected int doArgsValuesAndNames(RArgsValuesAndNames args, byte recursive, @Cached("createRecursive()") PrecedenceNode precedenceNode) {
        return precedenceNode.executeInteger(args.getArgument(0), recursive);
    }
}
