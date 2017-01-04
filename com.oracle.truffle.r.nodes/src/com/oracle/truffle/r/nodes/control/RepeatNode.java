/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RepeatingNode;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.nodes.function.visibility.SetVisibilityNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxCall;
import com.oracle.truffle.r.runtime.nodes.RSyntaxElement;
import com.oracle.truffle.r.runtime.nodes.RSyntaxLookup;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

public final class RepeatNode extends AbstractLoopNode implements RSyntaxNode, RSyntaxCall {

    @Child private LoopNode loop;
    @Child private SetVisibilityNode visibility = SetVisibilityNode.create();

    public RepeatNode(SourceSection src, RSyntaxLookup operator, RSyntaxNode body) {
        super(src, operator);
        this.loop = Truffle.getRuntime().createLoopNode(new WhileRepeatingNode(this, body.asRNode()));
    }

    @Override
    public Object execute(VirtualFrame frame) {
        loop.executeLoop(frame);
        visibility.execute(frame, false);
        return RNull.instance;
    }

    private static final class WhileRepeatingNode extends Node implements RepeatingNode {

        @Child private RNode body;

        private final BranchProfile normalBlock = BranchProfile.create();
        private final BranchProfile breakBlock = BranchProfile.create();
        private final BranchProfile nextBlock = BranchProfile.create();

        private final RepeatNode whileNode;

        WhileRepeatingNode(RepeatNode whileNode, RNode body) {
            this.whileNode = whileNode;
            this.body = body;
        }

        @Override
        public boolean executeRepeating(VirtualFrame frame) {
            try {
                body.voidExecute(frame);
                normalBlock.enter();
                return true;
            } catch (BreakException e) {
                breakBlock.enter();
                return false;
            } catch (NextException e) {
                nextBlock.enter();
                return true;
            }
        }

        @Override
        public String toString() {
            return whileNode.toString();
        }
    }

    @Override
    public RSyntaxElement[] getSyntaxArguments() {
        return new RSyntaxElement[]{((WhileRepeatingNode) loop.getRepeatingNode()).body.asRSyntaxNode()};
    }

    @Override
    public ArgumentsSignature getSyntaxSignature() {
        return ArgumentsSignature.empty(1);
    }
}
