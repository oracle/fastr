/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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
import static com.oracle.truffle.r.runtime.builtins.RBehavior.COMPLEX;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.function.PromiseHelperNode;
import com.oracle.truffle.r.nodes.function.call.RExplicitCallNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RPromise;

@RBuiltin(name = "forceAndCall", kind = PRIMITIVE, parameterNames = {"n", "FUN", "..."}, nonEvalArgs = 2, behavior = COMPLEX)
public abstract class ForceAndCall extends RBuiltinNode.Arg3 {

    @Child private RExplicitCallNode call = RExplicitCallNode.create();

    @Child private PromiseHelperNode promiseHelper;

    static {
        Casts casts = new Casts(ForceAndCall.class);
        casts.arg("n").asIntegerVector().findFirst();
        // TODO other types are possible for FUN that we don't yet handle
        casts.arg("FUN").mustBe(instanceOf(RFunction.class), RError.Message.INVALID_OR_UNIMPLEMENTED_ARGUMENTS);
    }

    @Specialization(guards = "cachedN == n")
    protected Object forceAndCall(VirtualFrame frame, @SuppressWarnings("unused") int n, RFunction fun, RArgsValuesAndNames args,
                    @Cached("n") int cachedN) {
        if (!fun.isBuiltin()) {
            flattenFirstArgs(frame, cachedN, args);
        }
        return call.execute(frame, fun, args);
    }

    @ExplodeLoop
    private void flattenFirstArgs(VirtualFrame frame, int n, RArgsValuesAndNames args) {
        if (promiseHelper == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            promiseHelper = insert(new PromiseHelperNode());
        }
        // In GnuR there appears to be no error checks on n > args.length
        if (args.getLength() < n) {
            CompilerDirectives.transferToInterpreter();
            throw RError.nyi(this, "callAndForce with insufficient arguments");
        }
        for (int i = 0; i < n; i++) {
            Object arg = args.getArgument(i);
            if (arg instanceof RArgsValuesAndNames) {
                CompilerDirectives.transferToInterpreter();
                throw RError.nyi(this, "callAndForce trying to force varargs");
            }
            if (arg instanceof RPromise) {
                promiseHelper.evaluate(frame, (RPromise) arg);
            }
        }
    }

    @SuppressWarnings("unused")
    @Specialization
    protected Object forceAndCallGeneric(VirtualFrame frame, int n, RFunction fun, RArgsValuesAndNames args) {
        CompilerDirectives.transferToInterpreter();
        throw RError.nyi(this, "generic case of forceAndCall");
    }
}
