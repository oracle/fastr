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

import static com.oracle.truffle.r.runtime.builtins.RBehavior.COMPLEX;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.function.signature.MissingNode;
import com.oracle.truffle.r.nodes.function.signature.MissingNode.MissingCheckCache;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RPromise;

/**
 * This builtin will not normally be used, since the RASTBuilder will immediately create instances
 * of {@link MissingNode}. This "proper" builtin differs in that it does not handle the "..." case
 * correctly, because the varargs are already handled by the argument matching.
 */
@RBuiltin(name = "missing", kind = PRIMITIVE, nonEvalArgs = 0, parameterNames = {"x"}, behavior = COMPLEX)
public abstract class Missing extends RBuiltinNode {

    static {
        Casts casts = new Casts(Missing.class);
        casts.arg("x").allowMissing();
    }

    @Specialization
    protected byte missing(VirtualFrame frame, RPromise promise,
                    @Cached("create(0)") MissingCheckCache cache) {
        String symbol = promise.getClosure().asSymbol();
        if (symbol == null) {
            CompilerDirectives.transferToInterpreter();
            throw error(Message.INVALID_USE, "missing");
        }
        return RRuntime.asLogical(cache.execute(frame, symbol));
    }

    @Specialization
    protected byte missing(@SuppressWarnings("unused") RMissing missing) {
        return RRuntime.LOGICAL_TRUE;
    }
}
