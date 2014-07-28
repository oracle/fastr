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
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.RPromise.EvalPolicy;
import com.oracle.truffle.r.runtime.data.RPromise.RPromiseArgFactory;

public class PromiseNode extends RNode {
    protected final RPromiseArgFactory factory;

    protected PromiseNode(RPromiseArgFactory factory) {
        this.factory = factory;
    }

    public static PromiseNode create(SourceSection src, RPromiseArgFactory factory) {
        PromiseNode pn = null;
        switch (factory.getEvalPolicy()) {
            case RAW:
                pn = new RawPromiseNode(factory);
                break;

            case STRICT:
                pn = new StrictPromiseNode(factory);
                break;

            case PROMISED:
                pn = new PromiseNode(factory);
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
        return factory.createPromiseArg();
    }

    /**
     * TODO Gero: comment.
     *
     */
    private static class StrictPromiseNode extends PromiseNode {
        @Child private RNode suppliedArg;

        public StrictPromiseNode(RPromiseArgFactory factory) {
            super(factory);
            this.suppliedArg = (RNode) factory.getArgument();
        }

        @Override
        public Object execute(VirtualFrame frame) {
            // Evaluate the supplied argument here in the caller frame and create the Promise with
            // this value so it can finally be evaluated on the callee side
            Object obj = suppliedArg != null ? suppliedArg.execute(frame) : RMissing.instance;
            return factory.createPromiseArgEvaluated(obj);
        }
    }

    /**
     * TODO Gero: comment.
     *
     * @see EvalPolicy#RAW
     */
    private static class RawPromiseNode extends PromiseNode {
        @Child private RNode suppliedArg;
        @Child private RNode defaultArg;

        public RawPromiseNode(RPromiseArgFactory factory) {
            super(factory);
            this.suppliedArg = (RNode) factory.getArgument();
            this.defaultArg = (RNode) factory.getDefaultArg();
        }

        @Override
        public Object execute(VirtualFrame frame) {
            // TODO builtin.inline: We do re-evaluation every execute inside the caller frame, as we
            // know that the evaluation of default values has no side effects (hopefully!)
            Object obj = suppliedArg != null ? suppliedArg.execute(frame) : RMissing.instance;
            RPromise promise = factory.createPromiseArgEvaluated(obj);
            return promise.evaluate(frame);
        }
    }
}
