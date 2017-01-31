/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.size;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;

@RBuiltin(name = "iconv", kind = INTERNAL, parameterNames = {"x", "from", "to", "sub", "mark", "toRaw"}, behavior = PURE)
public abstract class IConv extends RBuiltinNode {

    static {
        Casts casts = new Casts(IConv.class);
        casts.arg("x").mustBe(stringValue(), RError.NO_CALLER, RError.Message.NOT_CHARACTER_VECTOR, "x");
        // with default error message, NO_CALLER does not work
        casts.arg("from").defaultError(RError.NO_CALLER, RError.Message.INVALID_ARGUMENT, "from").mustBe(stringValue()).asStringVector().mustBe(size(1));
        casts.arg("to").defaultError(RError.NO_CALLER, RError.Message.INVALID_ARGUMENT, "to").mustBe(stringValue()).asStringVector().mustBe(size(1));
        casts.arg("sub").defaultError(RError.NO_CALLER, RError.Message.INVALID_ARGUMENT, "sub").mustBe(stringValue()).asStringVector().mustBe(size(1));
        casts.arg("mark").asLogicalVector().findFirst(RRuntime.LOGICAL_FALSE);
        casts.arg("toRaw").asLogicalVector().findFirst(RRuntime.LOGICAL_FALSE);

    }

    @SuppressWarnings("unused")
    @Specialization
    protected RStringVector doIConv(RAbstractStringVector x, Object from, Object to, Object sub, byte mark, byte toRaw) {
        // TODO implement
        RStringVector xv = x.materialize();
        return RDataFactory.createStringVector(xv.getDataCopy(), RDataFactory.COMPLETE_VECTOR);
    }
}
