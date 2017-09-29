/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.control;

import com.oracle.truffle.api.instrumentation.InstrumentableFactory.WrapperNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RepeatingNode;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.nodes.RRootNode;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxCall;
import com.oracle.truffle.r.runtime.nodes.RSyntaxElement;
import com.oracle.truffle.r.runtime.nodes.RSyntaxLookup;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

/** Marker class for loops. */
public abstract class AbstractLoopNode extends OperatorNode {

    protected AbstractLoopNode(SourceSection sourceSection, RSyntaxLookup operator) {
        super(sourceSection, operator);
    }

    @Override
    public String toString() {
        RootNode rootNode = getRootNode();
        String function = "?";
        if (rootNode instanceof RRootNode) {
            function = rootNode.toString();
        }
        SourceSection sourceSection = getLazySourceSection();
        int startLine = -1;
        if (sourceSection != null && sourceSection != RSyntaxNode.LAZY_DEPARSE && sourceSection != RSyntaxNode.INTERNAL && sourceSection != RSyntaxNode.SOURCE_UNAVAILABLE) {
            startLine = sourceSection.getStartLine();
        }
        RSyntaxElement call = ((RSyntaxCall) this).getSyntaxLHS();
        String name = ((RSyntaxLookup) call).getIdentifier();
        return String.format("%s-<%s:%d>", name, function, startLine);
    }

    protected abstract static class AbstractRepeatingNode extends Node implements RepeatingNode {

        @Child protected RNode body;

        public AbstractRepeatingNode(RNode body) {
            this.body = body;
        }
    }

    /**
     * Tests if the provided node is a loop-body node (also considering wrappers).
     */
    public static boolean isLoopBody(Node n) {
        Node parent = n.getParent();
        if (parent instanceof WrapperNode) {
            Node grandparent = parent.getParent();
            return grandparent instanceof AbstractRepeatingNode && ((AbstractRepeatingNode) grandparent).body == parent;
        }
        return (parent instanceof AbstractRepeatingNode && ((AbstractRepeatingNode) parent).body == n) ||
                        (parent instanceof ForNode && ((ForNode) parent).body == n);
    }
}
