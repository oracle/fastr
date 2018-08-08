/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.ffi.impl.nodes;

import java.util.LinkedList;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.nodes.access.variables.ReadVariableNode;
import com.oracle.truffle.r.nodes.function.PromiseHelperNode;
import com.oracle.truffle.r.nodes.function.RCallerHelper;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RCaller;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.data.RPromise.PromiseState;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.gnur.SEXPTYPE;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

public abstract class RForceAndCallNode extends RBaseNode {

    public static RForceAndCallNode create() {
        return RForceAndCallNodeGen.create();
    }

    @Child private PromiseHelperNode promiseHelper = new PromiseHelperNode();

    public abstract Object executeObject(Object e, Object f, int n, Object env);

    @Specialization
    Object forceAndCall(Object e, RFunction fun, int n, REnvironment env,
                    @Cached("createClassProfile()") ValueProfile accessProfile) {
        Object el = ((RPairList) e).cdr();
        List<Object> argValues = new LinkedList<>();
        RArgsValuesAndNames dotArgs = null;
        while (el != RNull.instance) {
            assert el instanceof RPairList;
            Object arg = ((RPairList) el).car();
            Object argVal = arg;
            if (arg instanceof RSymbol) {
                Object a = ReadVariableNode.lookupAny(((RSymbol) arg).getName(), env.getFrame(accessProfile), false);
                if (a instanceof RArgsValuesAndNames) {
                    dotArgs = (RArgsValuesAndNames) a;
                } else {
                    argVal = a;
                }

            } else if (arg instanceof RPairList) {
                RPairList argPL = (RPairList) arg;
                argPL = RDataFactory.createPairList(argPL.car(), argPL.cdr(), argPL.getTag(), SEXPTYPE.LANGSXP);
                argVal = RDataFactory.createPromise(PromiseState.Supplied, argPL.getClosure(), env.getFrame());
            }
            argValues.add(argVal);
            el = ((RPairList) el).cdr();
        }

        final RArgsValuesAndNames argsAndNames;
        if (dotArgs == null) {
            argsAndNames = new RArgsValuesAndNames(argValues.toArray(), ArgumentsSignature.empty(argValues.size()));
        } else {
            argsAndNames = createArgsAndNames(argValues, dotArgs);
        }

        if (!fun.isBuiltin()) {
            flattenFirstArgs(env.getFrame(), n, argsAndNames);
        }

        RCaller rCaller = RCaller.create(env.getFrame(), RCallerHelper.createFromArguments(fun, argsAndNames));
        return RContext.getEngine().evalFunction(fun, env.getFrame(), rCaller, false, argsAndNames.getSignature(), argsAndNames.getArguments());
    }

    private static RArgsValuesAndNames createArgsAndNames(List<Object> argValues, RArgsValuesAndNames dotArgs) {
        final RArgsValuesAndNames argsAndNames;
        Object[] argValuesEx = new Object[argValues.size() + dotArgs.getLength() - 1];
        String[] argNamesEx = new String[argValues.size() + dotArgs.getLength() - 1];
        System.arraycopy(argValues.toArray(), 0, argValuesEx, 0, argValues.size() - 1);
        System.arraycopy(dotArgs.getArguments(), 0, argValuesEx, argValues.size() - 1, dotArgs.getLength());
        final String[] argNames = dotArgs.getSignature().getNames();
        if (argNames != null) {
            System.arraycopy(argNames, 0, argNamesEx, argValues.size(), dotArgs.getLength());
        }
        argsAndNames = new RArgsValuesAndNames(argValuesEx, ArgumentsSignature.get(argNamesEx));
        return argsAndNames;
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
            throw RError.nyi(this, "forceAndCall with insufficient arguments");
        }
        for (int i = 0; i < n; i++) {
            Object arg = args.getArgument(i);
            if (arg instanceof RArgsValuesAndNames) {
                CompilerDirectives.transferToInterpreter();
                throw RError.nyi(this, "forceAndCall trying to force varargs");
            }
            if (arg instanceof RPromise) {
                promiseHelper.evaluate(frame, (RPromise) arg);
            }
        }
    }

}
