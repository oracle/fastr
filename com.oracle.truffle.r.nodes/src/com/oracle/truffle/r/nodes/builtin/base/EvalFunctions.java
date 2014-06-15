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

import static com.oracle.truffle.r.nodes.builtin.RBuiltinKind.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.REnvironment.PutException;
import com.oracle.truffle.r.runtime.data.*;

/**
 * {@code eval}, {@code evalq} and {@code local} are implemented as SUBSTITUTEs to ensure that the
 * expression argument is not evaluated which, currently, is only possible for builtins.
 *
 */
public class EvalFunctions {
    public abstract static class EvalAdapter extends RBuiltinNode {
        protected static final String[] PARAMETER_NAMES = new String[]{"expr", "envir", "enclosing"};

        @Override
        public Object[] getParameterNames() {
            return PARAMETER_NAMES;
        }

        @Override
        public RNode[] getParameterValues() {
            return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance)};
        }

        protected Object error(String msg) throws RError {
            CompilerDirectives.transferToInterpreter();
            throw RError.getGenericError(getEncapsulatingSourceSection(), msg);
        }

        protected Object doEvalBody(Object exprArg, REnvironment envir, @SuppressWarnings("unused") RMissing enclos) {
            Object expr = exprArg;
            if (expr instanceof RSymbol) {
                // We have to turn the string value back into a RLanguage object
                expr = RDataFactory.createLanguage(ReadVariableNode.create(((RSymbol) expr).getName(), false));
            }
            if (expr instanceof RExpression || expr instanceof RLanguage) {
                try {
                    Object result;
                    if (expr instanceof RExpression) {
                        result = REngine.eval((RExpression) expr, envir, REnvironment.emptyEnv());
                    } else {
                        result = REngine.eval((RLanguage) expr, envir, REnvironment.emptyEnv());
                    }
                    if (result == null) {
                        RContext.setVisible(false);
                        result = RNull.instance;
                    }
                    return result;
                } catch (PutException x) {
                    CompilerDirectives.transferToInterpreter();
                    throw RError.getGenericError(getEncapsulatingSourceSection(), x.getMessage());
                }
            } else {
                // just return value
                return expr;
            }
        }
    }

    @RBuiltin(name = "eval", nonEvalArgs = {0}, kind = SUBSTITUTE)
    public abstract static class Eval extends EvalAdapter {

        @Specialization
        public Object doEval(VirtualFrame frame, RPromise expr, @SuppressWarnings("unused") RMissing envir, RMissing enclos) {
            return doEval(frame, expr, REnvironment.globalEnv(), enclos);
        }

        @Specialization
        public Object doEval(VirtualFrame frame, RPromise expr, REnvironment envir, RMissing enclos) {
            /*
             * In the current hack expr is always a RPromise because we specified noEval for it in
             * the RBuiltin The spec for eval says that expr is evaluated in the current scope (aka
             * the caller's frame) so we do that first by evaluating the RPromise. Then we eval the
             * result, which could do another eval, e.g. "eval(expression(1+2))"
             * 
             * Note that builtins do not have a separate VirtualFrame, they use the frame of the
             * caller, so we can evaluate the promise using frame.
             */
            controlVisibility();
            Object exprVal = REngine.evalPromise(expr, frame);
            return doEvalBody(exprVal, envir, enclos);
        }

    }

    @RBuiltin(name = "evalq", nonEvalArgs = {0}, kind = SUBSTITUTE)
    public abstract static class EvalQuote extends EvalAdapter {
        @Specialization
        public Object doEval(RPromise expr, @SuppressWarnings("unused") RMissing envir, RMissing enclos) {
            return doEval(expr, REnvironment.globalEnv(), enclos);
        }

        @Specialization
        public Object doEval(RPromise expr, REnvironment envir, RMissing enclos) {
            /*
             * evalq does not evaluate it's first argument
             */
            controlVisibility();
            return doEvalBody(RDataFactory.createLanguage(expr.getRep()), envir, enclos);
        }

    }

    @RBuiltin(name = "local", nonEvalArgs = {0}, kind = SUBSTITUTE)
    public abstract static class Local extends EvalAdapter {
        @SuppressWarnings("hiding") protected static final String[] PARAMETER_NAMES = new String[]{"expr", "envir"};

        @Override
        public Object[] getParameterNames() {
            return PARAMETER_NAMES;
        }

        @Override
        public RNode[] getParameterValues() {
            return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance)};
        }

        @Specialization
        public Object doEval(VirtualFrame frame, RPromise expr, @SuppressWarnings("unused") RMissing envir, RMissing enclos) {
            return doEval(expr, new REnvironment.NewEnv(EnvFunctions.frameToEnvironment(frame), 0), enclos);
        }

        @Specialization
        public Object doEval(RPromise expr, REnvironment envir, RMissing enclos) {
            /*
             * local does not evaluate it's first argument
             */
            controlVisibility();
            return doEvalBody(RDataFactory.createLanguage(expr.getRep()), envir, enclos);
        }

    }
}
