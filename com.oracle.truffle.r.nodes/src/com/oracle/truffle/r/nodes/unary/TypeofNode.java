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

@SuppressWarnings("unused")
public abstract class TypeofNode extends UnaryNode {
    public abstract String execute(VirtualFrame frame, Object x);

    @Specialization
    public String typeof(RNull vector) {
        return "NULL";
    }

    @Specialization
    public String typeof(byte x) {
        return "logical";
    }

    @Specialization
    public String typeof(int s) {
        return "integer";
    }

    @Specialization
    public String typeof(double x) {
        return "double";
    }

    @Specialization
    public String typeof(RComplex x) {
        return "complex";
    }

    @Specialization
    public String typeof(RRaw x) {
        return "raw";
    }

    @Specialization
    public String typeof(String x) {
        return "character";
    }

    @Specialization
    public String typeof(RIntSequence vector) {
        return "integer";
    }

    @Specialization
    public String typeof(RLogicalVector vector) {
        return "logical";
    }

    @Specialization
    public String typeof(RIntVector vector) {
        return "integer";
    }

    @Specialization
    public String typeof(RDoubleVector vector) {
        return "double";
    }

    @Specialization
    public String typeof(RStringVector vector) {
        return "character";
    }

    @Specialization
    public String typeof(RComplexVector vector) {
        return "complex";
    }

    @Specialization
    public String typeof(RRawVector vector) {
        return "raw";
    }

    @Specialization
    public String typeof(RList list) {
        return "list";
    }

    @Specialization()
    public String typeof(REnvironment env) {
        return "environment";
    }

    @Specialization()
    public String typeof(RSymbol symbol) {
        return "symbol";
    }

    @Specialization()
    public String typeof(RLanguage language) {
        return "language";
    }

    @Specialization()
    public String typeof(RPromise promise) {
        return "promise";
    }

    @Specialization()
    public String typeof(RExpression symbol) {
        return "expression";
    }

    @Specialization
    public String typeof(RPairList pairlist) {
        return "pairlist";
    }

    @Specialization(order = 100, guards = "isFunctionBuiltin")
    public String typeofBuiltin(RFunction obj) {
        return "builtin";
    }

    @Specialization(order = 101, guards = "!isFunctionBuiltin")
    public String typeofClosure(RFunction obj) {
        return "closure";
    }

    @Specialization
    public String typeofFormula(RFormula f) {
        return "language";
    }

    public static boolean isFunctionBuiltin(RFunction fun) {
        return fun.isBuiltin();
    }
}