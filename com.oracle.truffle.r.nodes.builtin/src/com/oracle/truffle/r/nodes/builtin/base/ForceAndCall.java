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

import java.util.ArrayList;

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

    @Child private DoCall doCallNode;
    @Child private PromiseHelperNode promiseHelper;

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
        RArgsValuesAndNames flattened = flatten(args);
        // In GnuR there appears to be no error checks on n > args.length
        int cnt = Math.min(flattened.getLength(), n);
        for (int i = 0; i < cnt; i++) {
            RPromise arg = (RPromise) args.getArgument(i);
            initPromiseHelper().evaluate(frame, arg);
        }
        return getDoCallNode().execute(frame, fun, flattened, RNull.instance);
    }

    @TruffleBoundary
    private static RArgsValuesAndNames flatten(RArgsValuesAndNames args) {
        boolean hasVarArgs = false;
        int finalSize = args.getLength();
        for (Object arg : args.getArguments()) {
            if (arg instanceof RArgsValuesAndNames) {
                hasVarArgs = true;
                finalSize += ((RArgsValuesAndNames) arg).getLength() - 1;
            }
        }
        if (!hasVarArgs) {
            return args;
        }
        Object[] values = new Object[finalSize];
        String[] names = new String[finalSize];
        int pos = 0;
        Object[] arguments = args.getArguments();
        for (int i = 0; i < arguments.length; i++) {
            Object arg = arguments[i];
            if (arg instanceof RArgsValuesAndNames) {
                RArgsValuesAndNames varArgs = (RArgsValuesAndNames) arg;
                for (int j = 0; j < varArgs.getLength(); j++) {
                    values[pos] = varArgs.getArgument(j);
                    names[pos++] = varArgs.getSignature().getName(j);
                }
            } else {
                values[pos] = arg;
                names[pos++] = args.getSignature().getName(i);
            }
        }
        return new RArgsValuesAndNames(values, ArgumentsSignature.get(names));
    }

    @SuppressWarnings("unused")
    @Fallback
    protected Object forceAndCall(Object n, Object fun, RArgsValuesAndNames args) {
        throw RError.error(this, RError.Message.INVALID_OR_UNIMPLEMENTED_ARGUMENTS);
    }

    protected static boolean isBuiltin(RFunction fun) {
        return fun.isBuiltin();
    }
}
