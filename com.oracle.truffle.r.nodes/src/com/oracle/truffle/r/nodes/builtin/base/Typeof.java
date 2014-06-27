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
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin(name = "typeof", kind = SUBSTITUTE)
// TODO INTERNAL
@SuppressWarnings("unused")
public abstract class Typeof extends RBuiltinNode {

    public abstract String execute(VirtualFrame frame, Object x);

    @Specialization
    public String typeof(RNull vector) {
        controlVisibility();
        return "NULL";
    }

    @Specialization
    public String typeof(byte x) {
        controlVisibility();
        return "logical";
    }

    @Specialization
    public String typeof(int s) {
        controlVisibility();
        return "integer";
    }

    @Specialization
    public String typeof(double x) {
        controlVisibility();
        return "double";
    }

    @Specialization
    public String typeof(RComplex x) {
        controlVisibility();
        return "complex";
    }

    @Specialization
    public String typeof(RRaw x) {
        controlVisibility();
        return "raw";
    }

    @Specialization
    public String typeof(String x) {
        controlVisibility();
        return "character";
    }

    @Specialization
    public String typeof(RIntSequence vector) {
        controlVisibility();
        return "integer";
    }

    @Specialization
    public String typeof(RLogicalVector vector) {
        controlVisibility();
        return "logical";
    }

    @Specialization
    public String typeof(RIntVector vector) {
        controlVisibility();
        return "integer";
    }

    @Specialization
    public String typeof(RDoubleVector vector) {
        controlVisibility();
        return "double";
    }

    @Specialization
    public String typeof(RStringVector vector) {
        controlVisibility();
        return "character";
    }

    @Specialization
    public String typeof(RComplexVector vector) {
        controlVisibility();
        return "complex";
    }

    @Specialization
    public String typeof(RRawVector vector) {
        controlVisibility();
        return "raw";
    }

    @Specialization
    public String typeof(RList list) {
        controlVisibility();
        return "list";
    }

    @Specialization()
    public String typeof(REnvironment env) {
        controlVisibility();
        return "environment";
    }

    @Specialization()
    public String typeof(RSymbol symbol) {
        controlVisibility();
        return "symbol";
    }

    @Specialization()
    public String typeof(RLanguage language) {
        controlVisibility();
        return "language";
    }

    @Specialization()
    public String typeof(RPromise promise) {
        controlVisibility();
        return "promise";
    }

    @Specialization()
    public String typeof(RExpression symbol) {
        controlVisibility();
        return "expression";
    }

    @Specialization(order = 100, guards = "isFunctionBuiltin")
    public String typeofBuiltin(RFunction obj) {
        controlVisibility();
        return "builtin";
    }

    @Specialization(order = 101, guards = "!isFunctionBuiltin")
    public String typeofClosure(RFunction obj) {
        controlVisibility();
        return "closure";
    }

    public static boolean isFunctionBuiltin(RFunction fun) {
        return fun.isBuiltin();
    }
}
