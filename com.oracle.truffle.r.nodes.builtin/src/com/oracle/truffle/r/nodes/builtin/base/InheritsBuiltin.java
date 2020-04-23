/*
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates
 *
 * All rights reserved.
 */

package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.logicalValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;
import static com.oracle.truffle.r.runtime.RError.Message.NOT_CHARACTER_VECTOR;
import static com.oracle.truffle.r.runtime.RError.Message.NOT_LEN_ONE_LOGICAL_VECTOR;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.unary.InheritsNode;
import com.oracle.truffle.r.nodes.unary.InheritsNodeGen;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RStringVector;

@RBuiltin(name = "inherits", kind = INTERNAL, parameterNames = {"x", "what", "which"}, behavior = PURE)
public abstract class InheritsBuiltin extends RBuiltinNode.Arg3 {

    @Child private InheritsNode inheritsNode = InheritsNodeGen.create();

    public abstract Object execute(Object x, Object what, Object which);

    static {
        Casts casts = new Casts(InheritsBuiltin.class);
        casts.arg("what").mustBe(stringValue(), NOT_CHARACTER_VECTOR, "what");
        casts.arg("which").mustBe(logicalValue(), NOT_LEN_ONE_LOGICAL_VECTOR, "which").asLogicalVector().findFirst().map(toBoolean());
    }

    @Specialization
    protected Object doesInherit(Object x, RStringVector what, boolean which) {
        return inheritsNode.execute(x, what, which);
    }
}
