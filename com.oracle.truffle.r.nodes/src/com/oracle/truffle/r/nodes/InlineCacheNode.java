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
package com.oracle.truffle.r.nodes;

import java.util.function.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.RContext.Engine;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.RPromise.Closure;

/**
 * This node reifies a runtime object into the AST by creating nodes for frequently encountered
 * values. This can be used to bridge the gap between code as runtime data and executed code.
 */
@TypeSystemReference(RTypes.class)
public abstract class InlineCacheNode<F extends Frame, T> extends Node {

    public abstract Object execute(F frame, T value);

    /**
     * Creates an inline cache.
     *
     * @param maxPicDepth maximum number of entries in the polymorphic inline cache
     * @param reify a function that turns the runtime value into an RNode
     * @param generic a function that will be used to evaluate the given value after the polymorphic
     *            inline cache has reached its maximum size
     */
    public static <F extends Frame, T> InlineCacheNode<F, T> create(int maxPicDepth, Function<T, RNode> reify, BiFunction<F, T, Object> generic) {
        return new UninitializedInlineCacheNode<>(maxPicDepth, reify, generic);
    }

    /**
     * Creates an inline cache that will execute runtime expression given to it as RNodes by using a
     * PIC and falling back to {@link Engine#eval(RLanguage, MaterializedFrame)}.
     *
     * @param maxPicDepth maximum number of entries in the polymorphic inline cache
     */
    public static <F extends Frame> InlineCacheNode<F, RNode> createExpression(int maxPicDepth) {
        return create(maxPicDepth, value -> value, (frame, value) -> RContext.getEngine().eval(RDataFactory.createLanguage(value), frame.materialize()));
    }

    /**
     * Creates an inline cache that will execute promises closures by using a PIC and falling back
     * to {@link Engine#evalPromise(Closure, MaterializedFrame)}.
     *
     * @param maxPicDepth maximum number of entries in the polymorphic inline cache
     */
    public static <F extends Frame> InlineCacheNode<F, Closure> createPromise(int maxPicDepth) {
        return create(maxPicDepth, closure -> (RNode) closure.getExpr(), InlineCacheNode::evalPromise);
    }

    @TruffleBoundary
    private static <F extends Frame> Object evalPromise(F frame, Closure closure) {
        return RContext.getEngine().evalPromise(closure, frame.materialize());
    }

    @NodeInfo(cost = NodeCost.UNINITIALIZED)
    private static final class UninitializedInlineCacheNode<F extends Frame, T> extends InlineCacheNode<F, T> {

        private final int maxPicDepth;
        private final Function<T, RNode> reify;
        private final BiFunction<F, T, Object> generic;

        /** The current depth of the inline cache. */
        private int picDepth = 0;

        public UninitializedInlineCacheNode(int maxPicDepth, Function<T, RNode> reify, BiFunction<F, T, Object> generic) {
            this.maxPicDepth = maxPicDepth;
            this.reify = reify;
            this.generic = generic;
        }

        @Override
        public Object execute(F frame, T value) {
            CompilerDirectives.transferToInterpreterAndInvalidate();

            // Specialize below
            InlineCacheNode<F, T> replacement;
            if (picDepth < maxPicDepth) {
                picDepth += 1;
                replacement = new DirectInlineCacheNode(value, this);
            } else {
                replacement = new GenericInlineCacheNode();
            }
            return replace(replacement).execute(frame, value);
        }

        private final class DirectInlineCacheNode extends InlineCacheNode<F, T> {

            private final ConditionProfile isVirtualFrameProfile = ConditionProfile.createBinaryProfile();
            private final T originalValue;
            @Child private RNode reified;
            @Child private InlineCacheNode<F, T> next;

            protected DirectInlineCacheNode(T originalValue, InlineCacheNode<F, T> next) {
                /*
                 * The expression needs to be cloned in order to be inserted as a child (which is
                 * required for it to be executed). But at the same time the PIC relies on the
                 * identity of the original expression object, so both need to be kept.
                 */
                this.originalValue = originalValue;
                this.reified = NodeUtil.cloneNode(reify.apply(originalValue));
                this.next = next;
            }

            @Override
            public Object execute(F frame, T value) {
                VirtualFrame vf = isVirtualFrameProfile.profile(frame instanceof VirtualFrame) ? (VirtualFrame) frame : new SubstituteVirtualFrame(frame.materialize());
                return value == originalValue ? reified.execute(vf) : next.execute(frame, value);
            }
        }

        private final class GenericInlineCacheNode extends InlineCacheNode<F, T> {

            @Override
            public Object execute(F frame, T value) {
                return generic.apply(frame, value);
            }
        }
    }
}
