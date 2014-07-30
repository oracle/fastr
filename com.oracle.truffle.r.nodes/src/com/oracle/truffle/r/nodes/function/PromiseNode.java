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

import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.RPromise.EvalPolicy;
import com.oracle.truffle.r.runtime.data.RPromise.PromiseType;
import com.oracle.truffle.r.runtime.data.RPromise.RPromiseFactory;

public class PromiseNode extends RNode {
    protected final RPromiseFactory factory;
    protected final IEnvironmentProvider envProvider;

    protected PromiseNode(RPromiseFactory factory, IEnvironmentProvider envProvider) {
        this.factory = factory;
        this.envProvider = envProvider;
    }

    public static PromiseNode create(SourceSection src, RPromiseFactory factory, IEnvironmentProvider envProvider) {
        PromiseNode pn = null;
        assert factory.getType() != PromiseType.NO_ARG;
        switch (factory.getEvalPolicy()) {
            case RAW:
                if (factory.getType() == PromiseType.ARG_SUPPLIED) {
                    pn = new RawSuppliedPromiseNode(factory, envProvider);
                } else {
                    pn = new RawPromiseNode(factory, envProvider);
                }
                break;

            case STRICT:
                if (factory.getType() == PromiseType.ARG_SUPPLIED) {
                    pn = new StrictSuppliedPromiseNode(factory, envProvider);
                } else {
                    pn = new PromiseNode(factory, envProvider);
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
     * {@link RPromise} values are assign once.
     */
    @Override
    public Object execute(VirtualFrame frame) {
        return factory.createPromise(factory.getType() == PromiseType.ARG_DEFAULT ? null : envProvider.getREnvironmentFor(frame));
    }

    /**
     * TODO Gero: comment.
     *
     */
    private static class StrictSuppliedPromiseNode extends PromiseNode {
        @Child private RNode suppliedArg;

        public StrictSuppliedPromiseNode(RPromiseFactory factory, IEnvironmentProvider envProvider) {
            super(factory, envProvider);
            this.suppliedArg = (RNode) factory.getExpr();
        }

        @Override
        public Object execute(VirtualFrame frame) {
            // Evaluate the supplied argument here in the caller frame and create the Promise with
            // this value so it can finally be evaluated on the callee side
            Object obj = suppliedArg.execute(frame);
            RPromise promise = null;
            if (obj == RMissing.instance) {
                if (factory.getDefaultExpr() == null) {
                    return RMissing.instance;
                }
                promise = factory.createPromiseDefault();
            } else {
                promise = factory.createPromiseArgEvaluated(obj);
            }
            return promise;
        }
    }

    /**
     * TODO Gero: comment.
     *
     * @see EvalPolicy#RAW
     */
    private static class RawSuppliedPromiseNode extends PromiseNode {
        @Child private RNode expr;

        public RawSuppliedPromiseNode(RPromiseFactory factory, IEnvironmentProvider envProvider) {
            super(factory, envProvider);
            this.expr = (RNode) factory.getExpr();
        }

        @Override
        public Object execute(VirtualFrame frame) {
            // TODO builtin.inline: We do re-evaluation every execute inside the caller frame, as we
            // know that the evaluation of default values has no side effects (hopefully!)
            Object obj = expr.execute(frame);
            RPromise promise = null;
            if (obj == RMissing.instance) {
                if (factory.getDefaultExpr() == null) {
                    return RMissing.instance;
                }
                promise = factory.createPromiseDefault();
            } else {
                promise = factory.createPromiseArgEvaluated(obj);
            }
            return promise.evaluate(frame);
        }
    }

    /**
     * TODO Gero: comment.
     *
     * @see EvalPolicy#RAW
     */
    private static class RawPromiseNode extends PromiseNode {
        public RawPromiseNode(RPromiseFactory factory, IEnvironmentProvider envProvider) {
            super(factory, envProvider);
        }

        @Override
        public Object execute(VirtualFrame frame) {
            // TODO builtin.inline: We do re-evaluation every execute inside the caller frame, as we
            // know that the evaluation of default values has no side effects (hopefully!)
            // This is ARG_DEFAULT but use the same frame/environment!!!
            RPromise promise = factory.createPromise(null); // envProvider.getREnvironmentFor(frame));
            return promise.evaluate(frame);
        }
    }

    public static interface IEnvironmentProvider {
        REnvironment getREnvironmentFor(VirtualFrame frame);
    }

    public static class DefaultEnvProvider implements IEnvironmentProvider {
        private REnvironment callerEnv = null;

        public DefaultEnvProvider() {
        }

        public REnvironment getREnvironmentFor(VirtualFrame frame) {
            if (callerEnv == null || callerEnv.getFrame() != frame) {
                callerEnv = REnvironment.frameToEnvironment(frame);
            }

            return callerEnv;
        }
    }

}
