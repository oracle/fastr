/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.nodes.RASTUtils;
import com.oracle.truffle.r.nodes.RRootNode;
import com.oracle.truffle.r.nodes.function.PromiseHelperNode.PromiseDeoptimizeFrameNode;
import com.oracle.truffle.r.nodes.function.opt.EagerEvalHelper;
import com.oracle.truffle.r.nodes.function.visibility.SetVisibilityNode;
import com.oracle.truffle.r.nodes.instrumentation.RInstrumentation;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.builtins.FastPathFactory;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.env.frame.FrameSlotChangeMonitor;
import com.oracle.truffle.r.runtime.nodes.RSourceSectionNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxElement;
import com.oracle.truffle.r.runtime.nodes.RSyntaxFunction;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

public final class FunctionExpressionNode extends RSourceSectionNode implements RSyntaxNode, RSyntaxFunction {

    public static FunctionExpressionNode create(SourceSection src, RootCallTarget callTarget) {
        return new FunctionExpressionNode(src, callTarget);
    }

    @Child private SetVisibilityNode visibility = SetVisibilityNode.create();

    @CompilationFinal private RootCallTarget callTarget;
    private final PromiseDeoptimizeFrameNode deoptFrameNode;
    @CompilationFinal private FastPathFactory fastPath;

    @CompilationFinal private boolean initialized = false;

    private FunctionExpressionNode(SourceSection src, RootCallTarget callTarget) {
        super(src);
        this.callTarget = callTarget;
        this.deoptFrameNode = EagerEvalHelper.optExprs() || EagerEvalHelper.optVars() || EagerEvalHelper.optDefault() ? new PromiseDeoptimizeFrameNode() : null;
    }

    @Override
    public RFunction execute(VirtualFrame frame) {
        visibility.execute(frame, true);
        MaterializedFrame matFrame = frame.materialize();
        if (deoptFrameNode != null) {
            // Deoptimize every promise which is now in this frame, as it might leave it's stack
            deoptFrameNode.deoptimizeFrame(matFrame);
        }
        if (!initialized) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            if (!FrameSlotChangeMonitor.isEnclosingFrameDescriptor(callTarget.getRootNode().getFrameDescriptor(), frame)) {
                if (!FrameSlotChangeMonitor.isEnclosingFrameDescriptor(callTarget.getRootNode().getFrameDescriptor(), null)) {
                    RRootNode root = (RRootNode) callTarget.getRootNode();
                    callTarget = root.duplicateWithNewFrameDescriptor();
                }
                FrameSlotChangeMonitor.initializeEnclosingFrame(callTarget.getRootNode().getFrameDescriptor(), frame);
            }
            initialized = true;
        }
        RFunction func = RDataFactory.createFunction(RFunction.NO_NAME, RFunction.NO_NAME, callTarget, null, matFrame);
        RInstrumentation.checkDebugRequested(func);
        return func;
    }

    public RootCallTarget getCallTarget() {
        return callTarget;
    }

    @Override
    public RSyntaxElement[] getSyntaxArgumentDefaults() {
        return RASTUtils.asSyntaxNodes(((FunctionDefinitionNode) callTarget.getRootNode()).getFormalArguments().getArguments());
    }

    @Override
    public RSyntaxElement getSyntaxBody() {
        return ((FunctionDefinitionNode) callTarget.getRootNode()).getBody();
    }

    @Override
    public ArgumentsSignature getSyntaxSignature() {
        return ((FunctionDefinitionNode) callTarget.getRootNode()).getFormalArguments().getSignature();
    }

    @Override
    public String getSyntaxDebugName() {
        return ((RRootNode) callTarget.getRootNode()).getName();
    }
}
