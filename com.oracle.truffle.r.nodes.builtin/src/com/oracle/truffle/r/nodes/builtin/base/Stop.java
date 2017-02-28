/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.notEmpty;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.COMPLEX;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RNull;

@RBuiltin(name = "stop", kind = INTERNAL, parameterNames = {"call", "message"}, behavior = COMPLEX)
public abstract class Stop extends RBuiltinNode.Arg2 {

    static {
        Casts casts = new Casts(Stop.class);
        casts.arg("call").asLogicalVector().findFirst().map(toBoolean());
        casts.arg("message").allowNull().mustBe(stringValue()).asStringVector().mustBe(notEmpty(), RError.Message.INVALID_STRING_IN_STOP).findFirst();
    }

    @Specialization
    protected Object stop(boolean call, @SuppressWarnings("unused") RNull msgVec) {
        throw stop(call, "");
    }

    @Specialization
    protected RError stop(boolean call, String message) throws RError {
        CompilerDirectives.transferToInterpreter();
        throw RError.stop(call, RError.SHOW_CALLER2, RError.Message.GENERIC, message);
    }
}
