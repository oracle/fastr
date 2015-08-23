/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.context.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.env.*;
import com.oracle.truffle.r.runtime.nodes.*;

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

    @RBuiltin(name = "eval", kind = INTERNAL, parameterNames = {"expr", "envir", "enclos"})
    public abstract static class Eval extends EvalAdapter {

        public abstract Object execute(VirtualFrame frame, Object expr, REnvironment envir, REnvironment enclos);

        private final RAttributeProfiles attributeProfiles = RAttributeProfiles.create();

        @Specialization
        protected Object doEval(VirtualFrame frame, Object expr, REnvironment envir, @SuppressWarnings("unused") REnvironment enclos) {
            controlVisibility();
            return doEvalBody(RArguments.getDepth(frame) + 1, expr, envir);
        }

        @Specialization
        protected Object doEval(VirtualFrame frame, Object expr, @SuppressWarnings("unused") RNull envir, REnvironment enclos) {
            controlVisibility();
            return doEvalBody(RArguments.getDepth(frame) + 1, expr, enclos);
        }

        @Specialization
        protected Object doEval(VirtualFrame frame, Object expr, RList list, REnvironment enclos) {
            controlVisibility();
            return doEvalBody(RArguments.getDepth(frame) + 1, expr, REnvironment.createFromList(attributeProfiles, list, enclos));
        }

        @Specialization
        protected Object doEval(VirtualFrame frame, Object expr, RPairList list, REnvironment enclos) {
            controlVisibility();
            return doEvalBody(RArguments.getDepth(frame) + 1, expr, REnvironment.createFromList(attributeProfiles, list.toRList(), enclos));
        }

        @Specialization
        protected Object doEval(VirtualFrame frame, Object expr, RDataFrame dataFrame, REnvironment enclos) {
            controlVisibility();
            RVector vector = dataFrame.getVector();
            if (vector instanceof RList) {
                return doEvalBody(RArguments.getDepth(frame) + 1, expr, REnvironment.createFromList(attributeProfiles, (RList) vector, enclos));
            } else {
                throw RError.nyi(this, "eval on non-list dataframe");
            }
        }

        @SuppressWarnings("unused")
        @Fallback
        @TruffleBoundary
        protected Object doEval(Object expr, Object envir, Object enclos) {
            throw RError.error(this, RError.Message.INVALID_OR_UNIMPLEMENTED_ARGUMENTS);
        }
    }

    @RBuiltin(name = "withVisible", kind = RBuiltinKind.PRIMITIVE, parameterNames = "x", nonEvalArgs = 0)
    public abstract static class WithVisible extends EvalAdapter {
        private static final RStringVector LISTNAMES = RDataFactory.createStringVector(new String[]{"value", "visible"}, RDataFactory.COMPLETE_VECTOR);

        @Specialization
        protected RList withVisible(VirtualFrame frame, RPromise expr) {
            controlVisibility();
            Object result = doEvalBody(RArguments.getDepth(frame) + 1, RDataFactory.createLanguage((RNode) expr.getRep()), REnvironment.frameToEnvironment(frame.materialize()));
            Object[] data = new Object[]{result, RRuntime.asLogical(RContext.getInstance().isVisible())};
            // Visibility is changed by the evaluation (else this code would not work),
            // so we have to force it back on.
            RContext.getInstance().setVisible(true);
            return RDataFactory.createList(data, LISTNAMES);
        }
    }
}
