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

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

@RBuiltin("typeof")
@SuppressWarnings("unused")
public abstract class Typeof extends RBuiltinNode {

    @Specialization
    public RStringVector typeof(RNull vector) {
        controlVisibility();
        return RDataFactory.createStringVector("NULL");
    }

    @Specialization
    public RStringVector typeof(byte x) {
        controlVisibility();
        return RDataFactory.createStringVector("logical");
    }

    @Specialization
    public RStringVector typeof(int s) {
        controlVisibility();
        return RDataFactory.createStringVector("integer");
    }

    @Specialization
    public RStringVector typeof(double x) {
        controlVisibility();
        return RDataFactory.createStringVector("double");
    }

    @Specialization
    public RStringVector typeof(RComplex x) {
        controlVisibility();
        return RDataFactory.createStringVector("complex");
    }

    @Specialization
    public RStringVector typeof(RRaw x) {
        controlVisibility();
        return RDataFactory.createStringVector("raw");
    }

    @Specialization
    public RStringVector typeof(String x) {
        controlVisibility();
        return RDataFactory.createStringVector("character");
    }

    @Specialization
    public RStringVector typeof(RIntSequence vector) {
        controlVisibility();
        return RDataFactory.createStringVector("integer");
    }

    @Specialization
    public RStringVector typeof(RLogicalVector vector) {
        controlVisibility();
        return RDataFactory.createStringVector("logical");
    }

    @Specialization
    public RStringVector typeof(RIntVector vector) {
        controlVisibility();
        return RDataFactory.createStringVector("integer");
    }

    @Specialization
    public RStringVector typeof(RDoubleVector vector) {
        controlVisibility();
        return RDataFactory.createStringVector("double");
    }

    @Specialization
    public RStringVector typeof(RStringVector vector) {
        controlVisibility();
        return RDataFactory.createStringVector("character");
    }

    @Specialization
    public RStringVector typeof(RComplexVector vector) {
        controlVisibility();
        return RDataFactory.createStringVector("complex");
    }

    @Specialization
    public RStringVector typeof(RRawVector vector) {
        controlVisibility();
        return RDataFactory.createStringVector("raw");
    }

    @Specialization
    public RStringVector typeof(RList list) {
        controlVisibility();
        return RDataFactory.createStringVector("list");
    }

    @Specialization()
    public RStringVector typeof(REnvironment env) {
        controlVisibility();
        return RDataFactory.createStringVector("environment");
    }

    @Specialization(order = 100, guards = "isFunctionBuiltin")
    public RStringVector typeofBuiltin(RFunction obj) {
        controlVisibility();
        return RDataFactory.createStringVector("builtin");
    }

    @Specialization(order = 101, guards = "!isFunctionBuiltin")
    public RStringVector typeofClosure(RFunction obj) {
        controlVisibility();
        return RDataFactory.createStringVector("closure");
    }

    public static boolean isFunctionBuiltin(RFunction fun) {
        return fun.isBuiltin();
    }
}
