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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.singleElement;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.objects.AsS4;
import com.oracle.truffle.r.nodes.objects.AsS4NodeGen;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RSequence;

@RBuiltin(name = "setS4Object", kind = INTERNAL, parameterNames = {"object", "flag", "complete"}, behavior = PURE)
public abstract class SetS4Object extends RBuiltinNode.Arg3 {

    @Child private AsS4 asS4 = AsS4NodeGen.create();

    static {
        Casts casts = new Casts(SetS4Object.class);
        casts.arg("object").mustNotBeMissing().asAttributable(true, true, true);
        casts.arg("flag").asLogicalVector().mustBe(singleElement(), RError.Message.INVALID_ARGUMENT, "flag").findFirst().map(toBoolean());
        // "complete" can be a vector, unlike "flag"
        casts.arg("complete").asIntegerVector().findFirst(RError.Message.INVALID_ARGUMENT, "complete");
    }

    @Specialization
    @TruffleBoundary
    protected RNull asS4(RNull object, boolean flag, @SuppressWarnings("unused") int complete) {
        if (flag) {
            RContext.getInstance().setNullS4Object(true);
        } else {
            boolean wasS4 = RContext.getInstance().isNullS4Object();
            RContext.getInstance().setNullS4Object(false);
            if (wasS4) {
                throw error(RError.Message.GENERIC, "object of class \"NULL\" does not correspond to a valid S3 object");
            }
        }
        return object;
    }

    @Specialization(guards = "!isSequence(object)")
    protected Object asS4(RAttributable object, boolean flag, int complete) {
        return asS4.executeObject(object, flag, complete);
    }

    @Specialization
    protected Object asS4(RSequence seq, boolean flag, int complete) {
        return asS4(seq.materialize(), flag, complete);
    }

    protected boolean isSequence(Object o) {
        return o instanceof RSequence;
    }
}
