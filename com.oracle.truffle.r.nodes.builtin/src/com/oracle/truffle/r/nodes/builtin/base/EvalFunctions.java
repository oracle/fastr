/*
 * Copyright (c) 2014, 2014, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import com.oracle.truffle.api.CompilerDirectives.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.env.*;
import com.oracle.truffle.r.runtime.env.REnvironment.*;

/**
 * The {@code eval} {@code .Internal} and the {@code withVisible} {@code .Primitive}.
 *
 * TODO the {@code list} variants and the interpretation of the {@code enclos} argument.
 */
public class EvalFunctions {
    public abstract static class EvalAdapter extends RBuiltinNode {
        protected Object doEvalBody(VirtualFrame frame, Object exprArg, REnvironment envir, REnvironment enclos) {
            Object expr = RASTUtils.checkForRSymbol(exprArg);

            if (RASTUtils.isLanguageOrExpression(expr)) {
                try {
                    Object result;
                    int depth = RArguments.getDepth(frame) + 1;
                    if (expr instanceof RExpression) {
                        result = RContext.getEngine().eval((RExpression) expr, envir, enclos, depth);
                    } else {
                        result = RContext.getEngine().eval((RLanguage) expr, envir, enclos, depth);
                    }
                    return result;
                } catch (PutException ex) {
                    throw RError.error(getEncapsulatingSourceSection(), ex);
                }
            } else {
                // just return value
                return expr;
            }
        }

    }

    @RBuiltin(name = "eval", kind = INTERNAL, parameterNames = {"expr", "envir", "enclos"})
    @GenerateNodeFactory
    public abstract static class Eval extends EvalAdapter {

        public abstract Object execute(VirtualFrame frame, Object expr, REnvironment envir, REnvironment enclos);

        @Specialization
        @TruffleBoundary
        protected Object doEval(VirtualFrame frame, Object expr, REnvironment envir, REnvironment enclos) {
            controlVisibility();
            return doEvalBody(frame, expr, envir, enclos);
        }

        @Specialization
        @TruffleBoundary
        protected Object doEval(VirtualFrame frame, Object expr, @SuppressWarnings("unused") RNull envir, REnvironment enclos) {
            controlVisibility();
            return doEvalBody(frame, expr, REnvironment.emptyEnv(), enclos);
        }

        @SuppressWarnings("unused")
        @Fallback
        @TruffleBoundary
        protected Object doEval(VirtualFrame frame, Object expr, Object envir, Object enclos) {
            throw RError.nyi(getEncapsulatingSourceSection(), " eval arg type");
        }

    }

    @RBuiltin(name = "withVisible", kind = RBuiltinKind.PRIMITIVE, parameterNames = "x", nonEvalArgs = -1)
    public abstract static class WithVisible extends EvalAdapter {
        private static final RStringVector LISTNAMES = RDataFactory.createStringVector(new String[]{"value", "visible"}, RDataFactory.COMPLETE_VECTOR);

        @TruffleBoundary
        @Specialization
        protected RList withVisible(VirtualFrame frame, RPromise expr) {
            controlVisibility();
            Object result = doEvalBody(frame, RDataFactory.createLanguage(expr.getRep()), REnvironment.frameToEnvironment(frame.materialize()), REnvironment.emptyEnv());
            Object[] data = new Object[]{result, RRuntime.asLogical(RContext.isVisible())};
            // Visibility is changed by the evaluation (else this code would not work),
            // so we have to force it back on.
            RContext.setVisible(true);
            return RDataFactory.createList(data, LISTNAMES);
        }

    }
}
