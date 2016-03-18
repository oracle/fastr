/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.runtime.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.r.nodes.RASTUtils;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.base.EvalFunctionsFactory.EvalEnvCastNodeGen;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RAttributeProfiles;
import com.oracle.truffle.r.runtime.data.RDataFrame;
import com.oracle.truffle.r.runtime.data.RExpression;
import com.oracle.truffle.r.runtime.data.RLanguage;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RVector;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

/**
 * The {@code eval} {@code .Internal} and the {@code withVisible} {@code .Primitive}.
 */
public class EvalFunctions {
    public abstract static class EvalAdapter extends RBuiltinNode {
        @TruffleBoundary
        protected Object doEvalBody(int depth, Object exprArg, REnvironment envir) {
            Object expr = RASTUtils.checkForRSymbol(exprArg);

            if (expr instanceof RExpression) {
                return RContext.getEngine().eval((RExpression) expr, envir, depth);
            } else if (expr instanceof RLanguage) {
                return RContext.getEngine().eval((RLanguage) expr, envir, depth);
            } else {
                // just return value
                return expr;
            }
        }
    }

    public abstract static class EvalEnvCast extends RBaseNode {

        public abstract REnvironment execute(Object env, REnvironment enclos);

        private final RAttributeProfiles attributeProfiles = RAttributeProfiles.create();

        @Specialization
        protected REnvironment cast(REnvironment env, @SuppressWarnings("unused") REnvironment enclos) {
            return env;
        }

        @Specialization
        protected REnvironment cast(@SuppressWarnings("unused") RNull env, REnvironment enclos) {
            return enclos;
        }

        @Specialization
        protected REnvironment cast(RList list, REnvironment enclos) {
            return REnvironment.createFromList(attributeProfiles, list, enclos);
        }

        @Specialization
        protected REnvironment cast(RPairList list, REnvironment enclos) {
            return REnvironment.createFromList(attributeProfiles, list.toRList(), enclos);
        }

        @Specialization
        protected REnvironment cast(RDataFrame dataFrame, REnvironment enclos) {
            RVector vector = dataFrame.getVector();
            if (vector instanceof RList) {
                return REnvironment.createFromList(attributeProfiles, (RList) vector, enclos);
            } else {
                throw RError.nyi(this, "eval on non-list dataframe");
            }
        }

        @Fallback
        @TruffleBoundary
        protected REnvironment doEval(@SuppressWarnings("unused") Object env, @SuppressWarnings("unused") REnvironment enclos) {
            throw RError.error(this, RError.Message.INVALID_OR_UNIMPLEMENTED_ARGUMENTS);
        }
    }

    @RBuiltin(name = "eval", kind = INTERNAL, parameterNames = {"expr", "envir", "enclos"})
    public abstract static class Eval extends EvalAdapter {

        @Specialization
        protected Object doEval(VirtualFrame frame, Object expr, Object envir, REnvironment enclos, //
                        @Cached("createCast()") EvalEnvCast envCast) {
            controlVisibility();
            return doEvalBody(RArguments.getDepth(frame) + 1, expr, envCast.execute(envir, enclos));
        }

        protected EvalEnvCast createCast() {
            return EvalEnvCastNodeGen.create();
        }

        @SuppressWarnings("unused")
        @Fallback
        @TruffleBoundary
        protected Object doEval(Object expr, Object envir, Object enclos) {
            throw RError.error(this, RError.Message.INVALID_OR_UNIMPLEMENTED_ARGUMENTS);
        }
    }
}
