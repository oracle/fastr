/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.runtime.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.r.nodes.RASTUtils;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.function.PromiseHelperNode;
import com.oracle.truffle.r.nodes.function.RCallNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

@RBuiltin(name = "forceAndCall", kind = PRIMITIVE, parameterNames = {"n", "FUN", "..."}, nonEvalArgs = 2)
public abstract class ForceAndCall extends RBuiltinNode {

    @Child DoCall doCallNode;
    @Child PromiseHelperNode promiseHelper;

    protected PromiseHelperNode initPromiseHelper() {
        if (promiseHelper == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            promiseHelper = insert(new PromiseHelperNode());
        }
        return promiseHelper;
    }

    @Override
    protected void createCasts(CastBuilder casts) {
        casts.toInteger(0);
    }

    private DoCall getDoCallNode() {
        if (doCallNode == null) {
            doCallNode = insert(DoCallNodeGen.create(new RNode[3], null, null));
        }
        return doCallNode;
    }

    @Specialization(guards = "isBuiltin(fun)")
    protected Object forceAndCallBuiltin(VirtualFrame frame, @SuppressWarnings("unused") int n, RFunction fun, RArgsValuesAndNames args) {
        return getDoCallNode().execute(frame, fun, args, RNull.instance);
    }

    @Specialization(guards = "!isBuiltin(fun)")
    protected Object forceAndCall(VirtualFrame frame, int n, RFunction fun, RArgsValuesAndNames args) {
        initPromiseHelper();
        // In GnuR there appears to be no error checks on n > args.length
        int an = args.getLength();
        Object[] newArgValues = new Object[an];
        for (int i = 0; i < an; i++) {
            RPromise arg = (RPromise) args.getArgument(i);
            Object newArg;
            if (i < n) {
                newArg = initPromiseHelper().evaluate(frame, arg);
            } else {
                newArg = arg;
            }
            newArgValues[i] = newArg;
        }
        RArgsValuesAndNames newArgs = new RArgsValuesAndNames(newArgValues, args.getSignature());
        return createCallNode(fun, newArgs).execute(frame, fun);
    }

    @TruffleBoundary
    protected RCallNode createCallNode(RFunction fun, RArgsValuesAndNames args) {
        RSyntaxNode[] synArgs = new RSyntaxNode[args.getLength()];
        Object[] argValues = args.getArguments();
        String[] names = new String[synArgs.length];
        for (int i = 0; i < synArgs.length; i++) {
            synArgs[i] = (RSyntaxNode) RASTUtils.createNodeForValue(argValues[i]);
            String name = args.getSignature().getName(i);
            if (name != null && !name.isEmpty()) {
                names[i] = name;
            }
        }
        ArgumentsSignature argsSig = ArgumentsSignature.get(names);
        return (RCallNode) RASTUtils.createCall(fun, true, argsSig, synArgs);
    }

    @SuppressWarnings("unused")
    @Fallback
    protected Object forceAndCall(Object n, Object fun, RArgsValuesAndNames args) {
        throw RError.error(this, RError.Message.INVALID_OR_UNIMPLEMENTED_ARGUMENTS);
    }

    public static boolean isBuiltin(RFunction fun) {
        return fun.isBuiltin();
    }

}
