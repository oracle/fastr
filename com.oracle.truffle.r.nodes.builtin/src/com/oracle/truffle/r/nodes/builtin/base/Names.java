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
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin(name = "names", kind = PRIMITIVE, parameterNames = {"x"})
@SuppressWarnings("unused")
public abstract class Names extends RBuiltinNode {

    @Specialization
    public RNull getNames(RNull vector) {
        controlVisibility();
        return RNull.instance;
    }

    @Specialization
    public RNull getNames(byte operand) {
        controlVisibility();
        return RNull.instance;
    }

    @Specialization
    public RNull getNames(int operand) {
        controlVisibility();
        return RNull.instance;
    }

    @Specialization
    public RNull getNames(double operand) {
        controlVisibility();
        return RNull.instance;
    }

    @Specialization
    public RNull getNames(RComplex operand) {
        controlVisibility();
        return RNull.instance;
    }

    @Specialization
    public RNull getNames(String operand) {
        controlVisibility();
        return RNull.instance;
    }

    @Specialization
    public RNull getNames(RRaw operand) {
        controlVisibility();
        return RNull.instance;
    }

    @Specialization
    public RNull getNames(RFunction function) {
        controlVisibility();
        return RNull.instance;
    }

    @Specialization(guards = "!hasNames")
    public RNull getEmptyNames(RAbstractContainer container) {
        controlVisibility();
        return RNull.instance;
    }

    @Specialization(guards = "hasNames")
    public RStringVector getNames(RAbstractContainer container) {
        controlVisibility();
        return (RStringVector) container.getNames();
    }

    public static boolean hasNames(RAbstractContainer container) {
        return container.getNames() != null && container.getNames() != RNull.instance;
    }

}
