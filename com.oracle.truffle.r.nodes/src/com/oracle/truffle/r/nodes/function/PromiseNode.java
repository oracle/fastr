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
import com.oracle.truffle.api.CompilerDirectives.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.RPromise.EvalPolicy;

public class PromiseNode extends RNode {
    protected final RPromise promise;

    private PromiseNode(RPromise promise) {
        this.promise = promise;
    }

    public static PromiseNode create(SourceSection src, RPromise promise) {
        PromiseNode pn = null;
        switch (promise.getEvalPolicy()) {
            case RAW:
                pn = new RawPromiseNode(promise);
                break;

            case STRICT:
                pn = new StrictPromiseNode(promise);
                break;

            case PROMISED:
                pn = new PromiseNode(promise);
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
        return promise;
    }

    private static class StrictPromiseNode extends PromiseNode {
        @Child private RNode suppliedArg;
        @CompilationFinal private boolean hasArgValue;

        public StrictPromiseNode(RPromise promise) {
            super(promise);
            this.suppliedArg = (RNode) promise.getRep();
            this.hasArgValue = promise.isArgumentEvaluated();
        }

        @Override
        public Object execute(VirtualFrame frame) {
            if (!hasArgValue) {
                Object obj = suppliedArg.execute(frame);
                promise.setRawValue(obj);

                hasArgValue = true;
                CompilerDirectives.transferToInterpreterAndInvalidate();
            }
            return promise;
        }
    }

    /**
     * TODO Gero: comment
     *
     * @see EvalPolicy#RAW
     */
    private static class RawPromiseNode extends PromiseNode {
        public RawPromiseNode(RPromise promise) {
            super(promise);
        }

        @Override
        public Object execute(VirtualFrame frame) {
            // TODO Gero, WRONG! Normally take (null, frame), but env seems to be messes up... :-/
            return promise.evaluate(frame, frame);
        }
    }
}
