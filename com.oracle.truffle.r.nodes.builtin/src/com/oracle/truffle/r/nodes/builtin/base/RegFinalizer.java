/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.instanceOf;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.COMPLEX;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RExternalPtr;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.env.REnvironment;

@RBuiltin(name = "reg.finalizer", kind = INTERNAL, parameterNames = {"e", "f", "onexit"}, behavior = COMPLEX)
public abstract class RegFinalizer extends RBuiltinNode {
    @Override
    protected void createCasts(CastBuilder casts) {
        casts.arg("e").mustBe(instanceOf(REnvironment.class).or(instanceOf(RExternalPtr.class)), RError.Message.REG_FINALIZER_FIRST);
        casts.arg("f").mustBe(instanceOf(RFunction.class), RError.Message.REG_FINALIZER_SECOND);
        casts.arg("onexit").asLogicalVector().findFirst().notNA(RError.Message.REG_FINALIZER_THIRD).map(toBoolean());
    }

    @Specialization
    protected RNull doRegFinalizer(RExternalPtr ext, RFunction fun, boolean onexit) {
        return doRegFinalizerEither(ext, fun, onexit);
    }

    @Specialization
    protected RNull doRegFinalizer(REnvironment env, RFunction fun, boolean onexit) {
        return doRegFinalizerEither(env, fun, onexit);
    }

    @SuppressWarnings("unused")
    private static RNull doRegFinalizerEither(Object env, RFunction fun, boolean onexit) {
        // TODO the actual work
        return RNull.instance;
    }
}
