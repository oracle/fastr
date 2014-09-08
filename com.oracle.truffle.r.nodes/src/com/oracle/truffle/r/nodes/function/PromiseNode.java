/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.function;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.expressions.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.RPromise.Closure;
import com.oracle.truffle.r.runtime.data.RPromise.EvalPolicy;
import com.oracle.truffle.r.runtime.data.RPromise.PromiseType;
import com.oracle.truffle.r.runtime.data.RPromise.RPromiseFactory;
import com.oracle.truffle.r.runtime.env.*;

/**
 * This {@link RNode} implementations are used as a factory-nodes for {@link RPromise}s OR direct
 * evaluation of these {@link RPromise}s, depending on the promises {@link EvalPolicy}.<br/>
 * All these classes are created during/after argument matching and get cached afterwards, so they
 * get (and need to get) called every repeated call to a function with the same arguments.
 */
public class PromiseNode extends RNode {
    /**
     * The {@link RPromiseFactory} which holds all information necessary to construct a proper
     * {@link RPromise} for every case that might occur.
     */
    protected final RPromiseFactory factory;

    /**
     * {@link EnvProvider} needed to construct a proper {@link REnvironment} for the promises being
     * created here.
     */
    protected final EnvProvider envProvider;

    /**
     * @param factory {@link #factory}
     * @param envProvider {@link #envProvider}
     */
    protected PromiseNode(RPromiseFactory factory, EnvProvider envProvider) {
        this.factory = factory;
        this.envProvider = envProvider;
    }

    /**
     * @param src The {@link SourceSection} of the argument for debugging purposes
     * @param factory {@link #factory}
     * @param envProvider {@link #envProvider}
     * @return Depending on {@link RPromiseFactory#getEvalPolicy()} and
     *         {@link RPromiseFactory#getType()} the proper {@link PromiseNode} implementation
     */
    public static PromiseNode create(SourceSection src, RPromiseFactory factory, EnvProvider envProvider) {
        PromiseNode pn = null;
        assert factory.getType() != PromiseType.NO_ARG;
        switch (factory.getEvalPolicy()) {
            case INLINED:
                if (factory.getType() == PromiseType.ARG_SUPPLIED) {
                    pn = new InlinedSuppliedPromiseNode(factory, envProvider);
                } else {
                    pn = new InlinedPromiseNode(factory, envProvider);
                }
                break;

            case PROMISED:
                pn = new PromiseNode(factory, envProvider);
                break;

            default:
                throw new AssertionError();
        }

        pn.assignSourceSection(src);
        return pn;
    }

    /**
     * @param promise
     * @return Creates a {@link VarArgPromiseNode} for the given {@link RPromise}
     */
    public static VarArgPromiseNode createVarArg(RPromise promise) {
        VarArgPromiseNode result = new VarArgPromiseNode(promise);
        result.assignSourceSection(((RNode) promise.getRep()).getSourceSection());
        return result;
    }

    /**
     * Creates a new {@link RPromise} every time.
     */
    @Override
    public Object execute(VirtualFrame frame) {
        return factory.createPromise(factory.getType() == PromiseType.ARG_DEFAULT ? null : envProvider.getREnvironmentFor(frame));
    }

    /**
     * This class is meant for supplied arguments (which have to be evaluated in the caller frame)
     * which are supposed to be evaluated {@link EvalPolicy#INLINED}: This means we can simply
     * evaluate it here, and as it's {@link EvalPolicy#INLINED}, return its value and not the
     * {@link RPromise} itself! {@link EvalPolicy#INLINED} {@link PromiseType#ARG_SUPPLIED}
     */
    private final static class InlinedSuppliedPromiseNode extends PromiseNode {
        @Child private RNode expr;
        @Child private ExpressionExecutorNode exprExecNode = ExpressionExecutorNode.create();

        public InlinedSuppliedPromiseNode(RPromiseFactory factory, EnvProvider envProvider) {
            super(factory, envProvider);
            this.expr = (RNode) factory.getExpr();
        }

        @Override
        public Object execute(VirtualFrame frame) {
            // builtin.inline: We do re-evaluation every execute inside the caller frame, as we
            // know that the evaluation of default values has no side effects (has to be assured by
            // builtin implementations)
            Object obj = expr.execute(frame);
            if (obj == RMissing.instance) {
                if (factory.getDefaultExpr() == null) {
                    return RMissing.instance;
                }
                RPromise promise = factory.createPromiseDefault();
                return PromiseHelper.evaluate(frame, exprExecNode, promise);
            } else if (obj instanceof RArgsValuesAndNames) {
                return ((RArgsValuesAndNames) obj).evaluate(frame);
            } else {
                return RPromise.checkEvaluate(frame, obj);
            }
        }
    }

    /**
     * This class is meant for default arguments which have to be evaluated in the callee frame -
     * usually. But as this is for {@link EvalPolicy#INLINED}, arguments are simply evaluated inside
     * the caller frame: This means we can simply evaluate it here, and as it's
     * {@link EvalPolicy#INLINED}, return its value and not the {@link RPromise} itself!
     */
    private final static class InlinedPromiseNode extends PromiseNode {
        @Child private RNode defaultExpr;

        public InlinedPromiseNode(RPromiseFactory factory, EnvProvider envProvider) {
            super(factory, envProvider);
            // defaultExpr and expr are identical here!
            this.defaultExpr = (RNode) factory.getDefaultExpr();
        }

        @Override
        public Object execute(VirtualFrame frame) {
            // builtin.inline: We do re-evaluation every execute inside the caller frame, based on
            // the assumption that the evaluation of default values should have no side effects
            return defaultExpr.execute(frame);
        }
    }

    /**
     * This {@link RNode} is used to evaluate the expression given in a {@link RPromise} formerly
     * wrapped into a "..." after unrolling.
     */
    public final static class VarArgPromiseNode extends RNode {
        private final RPromise promise;
        @CompilationFinal private boolean isEvaluated = false;

        private VarArgPromiseNode(RPromise promise) {
            this.promise = promise;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            // ExpressionExecutorNode would be overkill, as this is only executed once, and not in
            // the correct frame anyway
            if (!isEvaluated) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                Object result = promise.evaluate(frame);
                isEvaluated = promise.isEvaluated();
                return result;
            }

            // Should be easily compile to constant
            return promise.getValue();
        }

        public RPromise getPromise() {
            return promise;
        }
    }

    /**
     * @param src
     * @param evalPolicy {@link EvalPolicy}
     * @param envProvider {@link EnvProvider}
     * @param nodes The argument {@link RNode}s that got wrapped into this "..."
     * @param names The argument's names
     * @param callSrc The {@link SourceSection} of the call this "..." belongs to
     * @return Creates either a {@link InlineVarArgsPromiseNode} or a {@link VarArgsPromiseNode},
     *         depending on the {@link EvalPolicy}
     */
    public static RNode createVarArgs(SourceSection src, EvalPolicy evalPolicy, EnvProvider envProvider, RNode[] nodes, String[] names, ClosureCache closureCache, SourceSection callSrc) {
        RNode node;
        switch (evalPolicy) {
            case INLINED:
                node = new InlineVarArgsPromiseNode(nodes, names);
                break;

            case PROMISED:
                node = new VarArgsPromiseNode(envProvider, nodes, names, closureCache);
                break;

            default:
                throw new AssertionError();
        }

        node.assignSourceSection(src);
        return node;
    }

    /**
     * This class is used for wrapping arguments into "..." ({@link RArgsValuesAndNames}).
     */
    private final static class VarArgsPromiseNode extends RNode {
        protected final RNode[] nodes;
        protected final String[] names;
        protected final EnvProvider envProvider;
        protected final ClosureCache closureCache;

        public VarArgsPromiseNode(EnvProvider envProvider, RNode[] nodes, String[] names, ClosureCache closureCache) {
            this.envProvider = envProvider;
            this.nodes = nodes;
            this.names = names;
            this.closureCache = closureCache;
        }

        @Override
        @ExplodeLoop
        public Object execute(VirtualFrame frame) {
            Object[] promises = new Object[nodes.length];
            for (int i = 0; i < nodes.length; i++) {
                Closure closure = closureCache.getOrCreateClosure(nodes[i]);
                promises[i] = RPromise.create(EvalPolicy.PROMISED, PromiseType.ARG_SUPPLIED, envProvider.getREnvironmentFor(frame), closure);
            }
            return new RArgsValuesAndNames(promises, names);
        }
    }

    /**
     * The {@link EvalPolicy#INLINED} counterpart of {@link VarArgsPromiseNode}: This gets a bit
     * more complicated, as "..." might also values from an outer "...", which might resolve to an
     * empty argument list.
     */
    public final static class InlineVarArgsPromiseNode extends RNode {
        @Children private final RNode[] varargs;
        protected final String[] names;

        public InlineVarArgsPromiseNode(RNode[] nodes, String[] names) {
            this.varargs = nodes;
            this.names = names;
        }

        @Override
        @ExplodeLoop
        public Object execute(VirtualFrame frame) {
            Object[] evaluatedArgs = new Object[varargs.length];
            String[] evaluatedNames = names;
            int index = 0;
            for (int i = 0; i < varargs.length; i++) {
                Object argValue = varargs[i].execute(frame);
                if (argValue instanceof RArgsValuesAndNames) {
                    // this can happen if ... is simply passed around (in particular when the call
                    // chain contains two functions with just the ... argument)
                    RArgsValuesAndNames argsValuesAndNames = (RArgsValuesAndNames) argValue;
                    int newLength = evaluatedArgs.length + argsValuesAndNames.length() - 1;
                    if (newLength == 0) {
                        // Corner case: "f <- function(...) g(...); g <- function(...)"
                        // In this case, "..." gets evaluated, and its only content is "...", which
                        // itself is missing. Result: Both disappear!
                        evaluatedArgs[index++] = RMissing.instance;
                        continue;
                    }
                    evaluatedArgs = Utils.resizeArray(evaluatedArgs, newLength);
                    evaluatedNames = Utils.resizeArray(evaluatedNames, newLength);
                    Object[] varargValues = argsValuesAndNames.getValues();
                    for (int j = 0; j < argsValuesAndNames.length(); j++) {
                        evaluatedArgs[index] = RPromise.checkEvaluate(frame, varargValues[j]);
                        evaluatedNames[index] = argsValuesAndNames.getNames()[j];
                        index++;
                    }
                } else {
                    evaluatedArgs[index++] = RPromise.checkEvaluate(frame, argValue);
                }
            }
            return new RArgsValuesAndNames(evaluatedArgs, evaluatedNames);
        }
    }
}
