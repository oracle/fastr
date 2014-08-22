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

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.env.*;

@SuppressWarnings("unused")
public abstract class TypeofNode extends UnaryNode {
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

    public static boolean isFunctionBuiltin(RFunction fun) {
        return fun.isBuiltin();
    }
}
