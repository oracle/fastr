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
package com.oracle.truffle.r.nodes.expressions;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.RContext.Engine;
import com.oracle.truffle.r.runtime.data.*;

/**
 * This node reifies an expression into the AST, i.e., it executes an expression given to it as an
 * {@link RNode}. This in essence bridges the gap between code as runtime data and executed code. To
 * make this as efficient as possible, it creates a PIC (polymorphic inline cache) for a certain
 * number of known expressions before delegating to a generic version which uses the
 * {@link Engine#eval(RLanguage, VirtualFrame)} functionality.
 */
public abstract class ExpressionExecutorNode extends Node {

    private static final int INLINE_CACHE_SIZE = 3;

    public abstract Object execute(VirtualFrame frame, RNode node);

    public static ExpressionExecutorNode create() {
        return new UninitializedExpressionExecutorNode();
    }

    private static final class UninitializedExpressionExecutorNode extends ExpressionExecutorNode {

        @Override
        public Object execute(VirtualFrame frame, RNode node) {
            // determine the current depth of the inline cache
            int depth = 0;
            Node parent = getParent();
            while (parent instanceof ExpressionExecutorNode) {
                depth++;
                parent = parent.getParent();
            }

            // Guard node creation/cloning. No
            // invalidate, as we are being replaced anyway
            CompilerDirectives.transferToInterpreter();

            ExpressionExecutorNode replacement;
            if (depth < INLINE_CACHE_SIZE) {
                replacement = new DirectExpressionExecutorNode(node, this);
            } else {
                replacement = new GenericExpressionExecutorNode();
            }
            return replace(replacement).execute(frame, node);
        }
    }

    private static final class DirectExpressionExecutorNode extends ExpressionExecutorNode {
        /*
         * The expression needs to be cloned in order to be inserted as a child (which is required
         * for it to be executed). But at the same time the PIC relies on the identity of the
         * original expression object, so both need to be kept.
         */
        private final RNode originalExpression;
        @Child private RNode reifiedExpression;
        @Child private ExpressionExecutorNode next;

        public DirectExpressionExecutorNode(RNode expression, ExpressionExecutorNode next) {
            this.originalExpression = expression;
            this.reifiedExpression = NodeUtil.cloneNode(expression);
            this.next = next;
        }

        @Override
        public Object execute(VirtualFrame frame, RNode node) {
            return node == originalExpression ? reifiedExpression.execute(frame) : next.execute(frame, node);
        }
    }

    private static final class GenericExpressionExecutorNode extends ExpressionExecutorNode {
        @Override
        public Object execute(VirtualFrame frame, RNode node) {
            return RContext.getEngine().eval(new RLanguage(node), frame);
        }
    }
}
