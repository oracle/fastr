/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.ffi.impl.nodes;

import static com.oracle.truffle.r.runtime.RError.Message.ARGUMENT_NOT_ENVIRONMENT;
import static com.oracle.truffle.r.runtime.RError.Message.ARGUMENT_NOT_FUNCTION;
import static com.oracle.truffle.r.runtime.RError.Message.UNKNOWN_OBJECT;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.access.variables.ReadVariableNode;
import com.oracle.truffle.r.nodes.function.PromiseHelperNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RCaller;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RExpression;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RLanguage;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.env.REnvironment;

public abstract class RfEvalNode extends FFIUpCallNode.Arg2 {

    @Child private PromiseHelperNode promiseHelper;

    public static RfEvalNode create() {
        return RfEvalNodeGen.create();
    }

    @Specialization
    @TruffleBoundary
    Object handlePromise(RPromise expr, @SuppressWarnings("unused") REnvironment env) {
        return getPromiseHelper().evaluate(null, expr);
    }

    @Specialization
    @TruffleBoundary
    Object handleExpression(RExpression expr, REnvironment env) {
        return RContext.getEngine().eval(expr, env, RCaller.topLevel);
    }

    @Specialization
    @TruffleBoundary
    Object handleLanguage(RLanguage expr, REnvironment env) {
        return RContext.getEngine().eval(expr, env, RCaller.topLevel);
    }

    @Specialization
    @TruffleBoundary
    Object handleSymbol(RSymbol expr, REnvironment env) {
        Object result = ReadVariableNode.lookupAny(expr.getName(), env.getFrame(), false);
        if (result == null) {
            throw RError.error(RError.NO_CALLER, UNKNOWN_OBJECT, expr.getName());
        }
        return result;
    }

    @Specialization
    Object handlePairList(RPairList l, REnvironment env,
                    @Cached("createBinaryProfile()") ConditionProfile isPromiseProfile,
                    @Cached("createBinaryProfile()") ConditionProfile noArgsProfile) {
        Object car = l.car();
        RFunction f;
        if (isPromiseProfile.profile(car instanceof RPromise)) {
            car = getPromiseHelper().evaluate(null, (RPromise) car);
        }

        if (car instanceof RFunction) {
            f = (RFunction) car;
        } else {
            throw RError.error(RError.NO_CALLER, ARGUMENT_NOT_FUNCTION);
        }

        Object args = l.cdr();
        if (noArgsProfile.profile(args == RNull.instance)) {
            return evalFunction(f, env, null);
        } else {
            RList argsList = ((RPairList) args).toRList();
            return evalFunction(f, env, ArgumentsSignature.fromNamesAttribute(argsList.getNames()), argsList.getDataTemp());
        }
    }

    @TruffleBoundary
    private Object evalFunction(RFunction f, REnvironment env, ArgumentsSignature argsNames, Object... args) {
        return RContext.getEngine().evalFunction(f, env == REnvironment.globalEnv() ? null : env.getFrame(), RCaller.topLevel, true, argsNames, args);
    }

    @Fallback
    Object handleOthers(Object expr, Object env) {
        if (env instanceof REnvironment) {
            return expr;
        } else {
            throw RError.error(RError.NO_CALLER, ARGUMENT_NOT_ENVIRONMENT);
        }
    }

    private PromiseHelperNode getPromiseHelper() {
        if (promiseHelper == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            promiseHelper = insert(new PromiseHelperNode());
        }
        return promiseHelper;
    }
}
