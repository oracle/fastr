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

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.env.*;
import com.oracle.truffle.r.runtime.env.REnvironment.*;

/**
 * {@code eval}, {@code evalq} and {@code local} are implemented as SUBSTITUTEs to ensure that the
 * expression/envir arguments are not evaluated which, absent full promises, is only possible for
 * builtins.
 *
 * N.B. {@code eval}, in particular, can accept an argument of any type so, with the current issue
 * of not being able to mix specializations that take arguments of type {@code T <: Object} and
 * {@code Object}, we don't specialize on {@code RExpression} and {@code RLanguage} but do runtime
 * instance checks instead.
 *
 * The general case where the "envir" argument is an arbitrary environment cannot be optimized by
 * Truffle as it necessarily involves a {@link MaterializedFrame}. However, we break out the special
 * case of the "current" frame, instead than routinely converting it to an environment and following
 * the slow path.
 *
 * TODO handle the special {@code enclos} argument special case.
 */
public class EvalFunctions {
    public abstract static class EvalAdapter extends RBuiltinNode {
        protected static final String[] PARAMETER_NAMES = new String[]{"expr", "envir", "enclosing"};

        @CompilationFinal private RFunction function;

        @Override
        public String[] getParameterNames() {
            return PARAMETER_NAMES;
        }

        @Override
        public RNode[] getParameterValues() {
            return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance)};
        }

        private static Object checkConvertSymbol(Object expr) {
            if (expr instanceof RSymbol) {
                String symbolName = ((RSymbol) expr).getName();
                // We have to turn the string value back into a RLanguage object
                return RDataFactory.createLanguage(ReadVariableNode.create(symbolName, false), RLanguage.TYPE.RNODE);
            } else {
                return expr;
            }
        }

        /**
         * Slow path variant, eval in arbitrary environment/frame.
         */
        protected Object doEvalBody(Object exprArg, REnvironment envir, @SuppressWarnings("unused") RMissing enclos) {
            Object expr = checkConvertSymbol(exprArg);

            if (expr instanceof RExpression || expr instanceof RLanguage) {
                try {
                    Object result;
                    if (expr instanceof RExpression) {
                        result = RContext.getEngine().eval(getFunction(), (RExpression) expr, envir, REnvironment.emptyEnv());
                    } else {
                        result = RContext.getEngine().eval(getFunction(), (RLanguage) expr, envir, REnvironment.emptyEnv());
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

        /**
         * Fast path variant, eval in the caller frame.
         */
        protected Object doEvalBodyInCallerFrame(VirtualFrame frame, Object exprArg) {
            Object expr = checkConvertSymbol(exprArg);

            if (expr instanceof RExpression) {
                return RContext.getEngine().eval((RExpression) exprArg, frame);
            } else if (expr instanceof RLanguage) {
                return RContext.getEngine().eval((RLanguage) exprArg, frame);
            } else {
                // just return value
                return expr;
            }
        }

        protected RFunction getFunction() {
            if (function == null) {
                function = RContext.getEngine().lookupBuiltin(getRBuiltin().name());
            }
            return function;
        }
    }

    @RBuiltin(name = "eval", kind = SUBSTITUTE, parameterNames = {"expr", "envir", "enclosing"})
    public abstract static class Eval extends EvalAdapter {

        @Specialization
        protected Object doEval(VirtualFrame frame, Object expr, @SuppressWarnings("unused") RMissing envir, @SuppressWarnings("unused") RMissing enclos) {
            return doEvalBodyInCallerFrame(frame, expr);
        }

        @Specialization
        protected Object doEval(Object expr, REnvironment envir, RMissing enclos) {
            controlVisibility();
            return doEvalBody(expr, envir, enclos);
        }

    }

    @RBuiltin(name = "evalq", nonEvalArgs = {0}, kind = SUBSTITUTE, parameterNames = {"expr", "envir", "enclosing"})
    public abstract static class EvalQuote extends EvalAdapter {

        @Specialization
        protected Object doEval(VirtualFrame frame, RPromise expr, @SuppressWarnings("unused") RMissing envir, @SuppressWarnings("unused") RMissing enclos) {
            return doEvalBodyInCallerFrame(frame, RDataFactory.createLanguage(expr.getRep(), RLanguage.TYPE.RNODE));
        }

        @Specialization
        protected Object doEval(RPromise expr, REnvironment envir, RMissing enclos) {
            /*
             * evalq does not evaluate it's first argument
             */
            controlVisibility();
            return doEvalBody(RDataFactory.createLanguage(expr.getRep(), RLanguage.TYPE.RNODE), envir, enclos);
        }

    }

    @RBuiltin(name = "local", nonEvalArgs = {0}, kind = SUBSTITUTE, parameterNames = {"expr", "envir"})
    public abstract static class Local extends EvalAdapter {
        @SuppressWarnings("hiding") protected static final String[] PARAMETER_NAMES = new String[]{"expr", "envir"};

        @Override
        public String[] getParameterNames() {
            return PARAMETER_NAMES;
        }

        @Override
        public RNode[] getParameterValues() {
            return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance)};
        }

        @Specialization
        protected Object doEval(VirtualFrame frame, RPromise expr, @SuppressWarnings("unused") RMissing envir, RMissing enclos) {
            return doEval(expr, new REnvironment.NewEnv(REnvironment.frameToEnvironment(frame.materialize()), 0), enclos);
        }

        @Specialization
        protected Object doEval(RPromise expr, REnvironment envir, RMissing enclos) {
            /*
             * local does not evaluate it's first argument
             */
            controlVisibility();
            return doEvalBody(RDataFactory.createLanguage(expr.getRep(), RLanguage.TYPE.RNODE), envir, enclos);
        }

    }
}
