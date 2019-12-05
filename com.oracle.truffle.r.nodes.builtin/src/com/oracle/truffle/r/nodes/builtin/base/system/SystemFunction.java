/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.nodes.builtin.base.system;

import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.dsl.CachedContext;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.gte;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;
import static com.oracle.truffle.r.runtime.RVisibility.CUSTOM;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.COMPLEX;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.function.visibility.SetVisibilityNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.TruffleRLanguage;

@RBuiltin(name = "system", visibility = CUSTOM, kind = INTERNAL, parameterNames = {"command", "intern", "timeout"}, behavior = COMPLEX)
public abstract class SystemFunction extends RBuiltinNode.Arg3 {
    @Child private SetVisibilityNode visibility = SetVisibilityNode.create();

    static {
        Casts casts = new Casts(SystemFunction.class);
        casts.arg("command").mustBe(stringValue(), RError.Message.SYSTEM_CHAR_ARG).asStringVector().findFirst();
        casts.arg("intern").asLogicalVector().findFirst().mustNotBeNA(RError.Message.SYSTEM_INTERN_NOT_NA).map(toBoolean());
        casts.arg("timeout").asIntegerVector().findFirst().mustNotBeNA().mustBe(gte(0));
    }

    @Specialization
    protected Object system(VirtualFrame frame, String command, boolean intern, int timeout,
                    @CachedContext(TruffleRLanguage.class) TruffleLanguage.ContextReference<RContext> ctxRef) {
        Object result = SystemFunctionFactory.getInstance().execute(frame, command.trim(), intern, timeout, ctxRef.get());
        visibility.execute(frame, intern);
        return result;
    }
}
