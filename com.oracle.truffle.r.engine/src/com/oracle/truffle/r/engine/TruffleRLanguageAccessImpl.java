/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.engine;

import com.oracle.truffle.api.TruffleLanguage.Env;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.r.nodes.RASTBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinPackages;
import com.oracle.truffle.r.nodes.function.PromiseHelperNode;
import com.oracle.truffle.r.nodes.function.RMissingHelper;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RCaller;
import com.oracle.truffle.r.runtime.RDeparse;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.conn.StdConnections.ContextStateImpl;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.TruffleRLanguageAccess;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RInteropNA;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.env.REnvironment;

public final class TruffleRLanguageAccessImpl implements TruffleRLanguageAccess {

    @Override
    public void onInitializeContext(Env env) {
        RContext.initializeGlobalState(new RASTBuilder(false), new RRuntimeASTAccessImpl(), RBuiltinPackages.getInstance());
    }

    @Override
    public String toString(RContext context, Object value) {
        // primitive values are never produced by FastR so we don't print them as R vectors
        if (value instanceof Boolean) {
            // boolean constants are capitalized like in R
            return (boolean) value ? "TRUE" : "FALSE";
        }
        if (value instanceof Number || value instanceof String || value instanceof Character) {
            return value.toString();
        }

        // special class designated to exchange NA values with the outside world
        // this value is a scalar, the only way to get it is via getArrayMember on an R vector
        if (value instanceof RInteropNA) {
            return "NA";
        }

        // the debugger also passes result of TruffleRLanguage.findMetaObject() to this method
        Object unwrapped = value;
        // print promises by other means than the "print" function to avoid evaluating them
        if (unwrapped instanceof RPromise) {
            RPromise promise = (RPromise) unwrapped;
            if (promise.isEvaluated() || promise.isOptimized()) {
                unwrapped = promise.getValue();
            } else {
                return RDeparse.deparse(unwrapped, RDeparse.MAX_CUTOFF, true, RDeparse.KEEPINTEGER, -1, 1024 * 1024);
            }
        }
        // print missing explicitly, because "print" would report missing argument
        if (RMissingHelper.isMissing(unwrapped)) {
            return "missing";
        }

        // the value unwrapped from an RPromise can be primitive Java type, but now we know that we
        // are dealing with primitive that is supposed to be treated as R vector
        unwrapped = RRuntime.asAbstractVector(unwrapped);
        if (!(unwrapped instanceof TruffleObject)) {
            throw RError.error(RError.NO_CALLER, Message.GENERIC, String.format("Printing value of type '%s' is not supported by the R language.", unwrapped.getClass().getSimpleName()));
        }
        Object printObj = REnvironment.baseEnv(context).get("print");
        if (printObj instanceof RPromise) {
            printObj = PromiseHelperNode.evaluateSlowPath((RPromise) printObj);
        }
        if (!(printObj instanceof RFunction)) {
            throw RError.error(RError.NO_CALLER, Message.GENERIC, "Cannot retrieve the 'print' function from the base package.");
        }
        MaterializedFrame callingFrame = REnvironment.globalEnv(context).getFrame();
        ContextStateImpl stateStdConnections = context.stateStdConnections;
        try {
            StringBuilder buffer = new StringBuilder();
            stateStdConnections.setBuffer(buffer);
            RContext.getEngine().evalFunction((RFunction) printObj, callingFrame, RCaller.topLevel, false, ArgumentsSignature.empty(1), unwrapped);
            // remove the last "\n", which is useful for REPL, but not here
            if (buffer.length() > 0 && buffer.charAt(buffer.length() - 1) == '\n') {
                buffer.setLength(buffer.length() - 1);
            }
            return buffer.toString();
        } finally {
            stateStdConnections.resetBuffer();
        }
    }

}
