/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.function.opt.eval;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.nodes.access.variables.LocalReadVariableNode;
import com.oracle.truffle.r.nodes.function.RCallBaseNode;
import com.oracle.truffle.r.nodes.function.RCallNode;
import com.oracle.truffle.r.nodes.function.RCallNode.ExplicitArgs;
import com.oracle.truffle.r.nodes.profile.TruffleBoundaryNode;
import com.oracle.truffle.r.runtime.RCaller;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.env.frame.FrameSlotChangeMonitor;
import com.oracle.truffle.r.runtime.env.frame.RFrameSlot;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

/**
 * The root node for the call target called by {@code FunctionEvalCallNode}. It evaluates the
 * function call using {@code RCallBaseNode}.
 */
public final class CallInfoEvalRootNode extends RootNode {

    @Child RCallBaseNode callNode;
    @Child LocalReadVariableNode funReader;

    protected CallInfoEvalRootNode(RFrameSlot argsIdentifier, RFrameSlot funIdentifier) {
        super(RContext.getInstance().getLanguage());
        this.callNode = RCallNode.createExplicitCall(argsIdentifier);
        this.funReader = LocalReadVariableNode.create(funIdentifier, false);
        this.getCallTarget(); // Ensure call target is initialized
    }

    @Override
    public SourceSection getSourceSection() {
        return RSyntaxNode.INTERNAL;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        MaterializedFrame evalFrame = (MaterializedFrame) frame.getArguments()[0];
        RFunction fun = (RFunction) funReader.execute(evalFrame);
        return callNode.execute(evalFrame, fun);
    }

    /**
     * This is a helper intermediary node that is used by {@code FunctionEvalNode} to insert a new
     * frame on the Truffle stack.
     */
    public static final class FastPathDirectCallerNode extends Node {

        @CompilationFinal private FrameSlot argsFrameSlot;
        @CompilationFinal private FrameSlot funFrameSlot;

        private final CallInfoEvalRootNode evalRootNode = new CallInfoEvalRootNode(RFrameSlot.FunctionEvalNodeArgsIdentifier, RFrameSlot.FunctionEvalNodeFunIdentifier);
        @Child private DirectCallNode directCallNode = DirectCallNode.create(evalRootNode.getCallTarget());

        public Object execute(VirtualFrame evalFrame, RFunction function, RArgsValuesAndNames args, RCaller explicitCaller, Object callerFrame) {
            if (argsFrameSlot == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                argsFrameSlot = FrameSlotChangeMonitor.findOrAddFrameSlot(evalFrame.getFrameDescriptor(), RFrameSlot.FunctionEvalNodeArgsIdentifier, FrameSlotKind.Object);
            }
            if (funFrameSlot == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                funFrameSlot = FrameSlotChangeMonitor.findOrAddFrameSlot(evalFrame.getFrameDescriptor(), RFrameSlot.FunctionEvalNodeFunIdentifier, FrameSlotKind.Object);
            }
            // This assertion should verify that the descriptor of the current frame is the same as
            // the first descriptor for which this node created. See the guard at
            // RfEvalNode.FunctionEvalNode (FunctionInfo.isCompatible()).
            assert evalFrame.getFrameDescriptor().findFrameSlot(RFrameSlot.FunctionEvalNodeArgsIdentifier) != null &&
                            evalFrame.getFrameDescriptor().findFrameSlot(RFrameSlot.FunctionEvalNodeFunIdentifier) != null;
            try {
                // the two slots are used to pass the explicit args and the called function to the
                // FunctionEvalRootNode
                FrameSlotChangeMonitor.setObject(evalFrame, argsFrameSlot, new ExplicitArgs(args, explicitCaller, callerFrame));
                FrameSlotChangeMonitor.setObject(evalFrame, funFrameSlot, function);
                return directCallNode.call(evalFrame);
            } finally {
                FrameSlotChangeMonitor.setObject(evalFrame, argsFrameSlot, null);
                FrameSlotChangeMonitor.setObject(evalFrame, funFrameSlot, null);
            }
        }

    }

    public static final class SlowPathDirectCallerNode extends TruffleBoundaryNode {
        @Child private FastPathDirectCallerNode slowPathEvalNode;

        @TruffleBoundary
        public Object execute(MaterializedFrame evalFrame, Object callerFrame, RCaller caller, RFunction func, RArgsValuesAndNames args) {
            slowPathEvalNode = insert(new FastPathDirectCallerNode());
            return slowPathEvalNode.execute(evalFrame, func, args, caller, callerFrame);
        }
    }

}
