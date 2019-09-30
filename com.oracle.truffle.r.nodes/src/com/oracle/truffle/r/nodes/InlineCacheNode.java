/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.TruffleRuntime;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.Closure;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;
import com.oracle.truffle.r.runtime.nodes.RNode;

/**
 * This node reifies a runtime object into the AST by creating nodes for frequently encountered
 * values. This can be used to bridge the gap between code as runtime data and executed code.
 */
@ReportPolymorphism
public abstract class InlineCacheNode extends RBaseNode {

    protected final int maxPicDepth;

    public abstract Object execute(Frame frame, Object value);

    protected InlineCacheNode(int maxPicDepth) {
        this.maxPicDepth = maxPicDepth;
    }

    /**
     * Creates an inline cache that will execute promises closures by using a polymorphic inline
     * cache (PIC) and falling back to
     * {@link InlineCacheNode#evalPromise(MaterializedFrame, Closure)}.
     *
     * @param maxPicDepth maximum number of entries in the PIC
     */
    public static InlineCacheNode create(int maxPicDepth) {
        return InlineCacheNodeGen.create(maxPicDepth);
    }

    // Cached case: create a root node and direct call node
    // This should allow Truffle PE to inline the promise code if it deems it is beneficial

    protected DirectCallNode createInlinableCall(Closure value) {
        InlineCacheRootNode rootNode = new InlineCacheRootNode(RContext.getInstance().getLanguage(), RASTUtils.cloneNode((RNode) value.getExpr()));
        TruffleRuntime runtime = Truffle.getRuntime();
        runtime.createCallTarget(rootNode);
        return runtime.createDirectCallNode(rootNode.getCallTarget());
    }

    private static final class InlineCacheRootNode extends RootNode {
        @Child private RNode node;

        protected InlineCacheRootNode(TruffleLanguage<?> language, RNode node) {
            super(language);
            this.node = node;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            VirtualFrame execFrame = (VirtualFrame) frame.getArguments()[0];
            return node.visibleExecute(execFrame);
        }
    }

    @Specialization(limit = "maxPicDepth", guards = "value == cachedValue")
    protected Object doCached(Frame frame, @SuppressWarnings("unused") Closure value,
                    @SuppressWarnings("unused") @Cached("value") Closure cachedValue,
                    @Cached("createClassProfile()") ValueProfile frameClassProfile,
                    @Cached("createInlinableCall(value)") DirectCallNode callNode) {
        return callNode.call(frameClassProfile.profile(frame).materialize());
    }

    // Generic case: execute call target cached in the Closure
    // We do not go though call node, so not Truffle inlining can take place,
    // but nothing is inserted into this AST, so it doesn't grow without limits

    @Specialization(replaces = "doCached")
    protected Object doGeneric(Frame frame, Closure value) {
        return evalPromise(frame.materialize(), value);
    }

    @TruffleBoundary
    protected static Object evalPromise(MaterializedFrame frame, Closure closure) {
        return closure.eval(frame);
    }
}
