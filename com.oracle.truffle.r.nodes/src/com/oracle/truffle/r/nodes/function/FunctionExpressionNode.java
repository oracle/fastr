/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.nodes.NodeUtil.NodeCountFilter;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.variables.*;
import com.oracle.truffle.r.nodes.function.PromiseHelperNode.*;
import com.oracle.truffle.r.nodes.function.opt.*;
import com.oracle.truffle.r.nodes.instrument.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.env.*;
import com.oracle.truffle.r.runtime.env.frame.*;
import com.oracle.truffle.r.runtime.gnur.*;

public final class FunctionExpressionNode extends RNode implements RSyntaxNode {

    public static FunctionExpressionNode create(SourceSection src, RootCallTarget callTarget) {
        return new FunctionExpressionNode(src, callTarget);
    }

    private final RootCallTarget callTarget;
    private final PromiseDeoptimizeFrameNode deoptFrameNode;
    private final boolean containsDispatch;

    @CompilationFinal private StableValue<MaterializedFrame> enclosingFrameAssumption;
    @CompilationFinal private StableValue<FrameDescriptor> enclosingFrameDescriptorAssumption;

    private FunctionExpressionNode(SourceSection src, RootCallTarget callTarget) {
        assignSourceSection(src);
        this.callTarget = callTarget;
        this.deoptFrameNode = EagerEvalHelper.optExprs() || EagerEvalHelper.optVars() || EagerEvalHelper.optDefault() ? new PromiseDeoptimizeFrameNode() : null;

        NodeCountFilter dispatchingMethodsFilter = node -> {
            if (node instanceof ReadVariableNode) {
                String identifier = ((ReadVariableNode) node).getIdentifier();
                return "UseMethod".equals(identifier) /* || "NextMethod".equals(identifier) */;
            }
            return false;
        };
        this.containsDispatch = NodeUtil.countNodes(callTarget.getRootNode(), dispatchingMethodsFilter) > 0;

        this.enclosingFrameAssumption = FrameSlotChangeMonitor.getEnclosingFrameAssumption(callTarget.getRootNode().getFrameDescriptor());
        this.enclosingFrameDescriptorAssumption = FrameSlotChangeMonitor.getEnclosingFrameDescriptorAssumption(callTarget.getRootNode().getFrameDescriptor());
    }

    @Override
    public RFunction execute(VirtualFrame frame) {
        return executeFunction(frame);
    }

    protected void verifyEnclosingAssumptions(VirtualFrame enclosingFrame, FrameDescriptor descriptor) {
        if (enclosingFrameAssumption != null) {
            try {
                enclosingFrameAssumption.getAssumption().check();
            } catch (InvalidAssumptionException e) {
                enclosingFrameAssumption = FrameSlotChangeMonitor.getEnclosingFrameAssumption(descriptor);
            }
            if (enclosingFrameAssumption != null) {
                if (enclosingFrameAssumption.getValue() != enclosingFrame) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    enclosingFrameAssumption = FrameSlotChangeMonitor.getOrInitializeEnclosingFrameAssumption(null, descriptor, enclosingFrameAssumption, enclosingFrame.materialize());
                }
            }
        }
        if (enclosingFrameDescriptorAssumption != null) {
            try {
                enclosingFrameDescriptorAssumption.getAssumption().check();
            } catch (InvalidAssumptionException e) {
                enclosingFrameDescriptorAssumption = FrameSlotChangeMonitor.getEnclosingFrameDescriptorAssumption(descriptor);
            }
            if (enclosingFrameDescriptorAssumption != null) {
                FrameDescriptor enclosingFrameDescriptor = enclosingFrame.getFrameDescriptor();
                if (enclosingFrameDescriptorAssumption.getValue() != enclosingFrameDescriptor) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    enclosingFrameDescriptorAssumption = FrameSlotChangeMonitor.getOrInitializeEnclosingFrameDescriptorAssumption(null, descriptor, enclosingFrameDescriptorAssumption,
                                    enclosingFrameDescriptor);
                }
            }
        }
    }

    @Override
    public RFunction executeFunction(VirtualFrame frame) {
        MaterializedFrame matFrame = frame.materialize();
        if (deoptFrameNode != null) {
            // Deoptimize every promise which is now in this frame, as it might leave it's stack
            deoptFrameNode.deoptimizeFrame(matFrame);
        }
        verifyEnclosingAssumptions(frame, callTarget.getRootNode().getFrameDescriptor());
        RFunction func = RDataFactory.createFunction("", callTarget, null, matFrame, containsDispatch);
        if (RInstrument.instrumentingEnabled()) {
            RInstrument.checkDebugRequested(callTarget.toString(), func);
        }
        return func;
    }

    public RootCallTarget getCallTarget() {
        return callTarget;
    }

    @Override
    public void deparse(RDeparse.State state) {
        ((FunctionDefinitionNode) callTarget.getRootNode()).deparse(state);
    }

    @Override
    public void serialize(RSerialize.State state) {
        state.setAsBuiltin("function");
        state.openPairList(SEXPTYPE.LISTSXP);
        FunctionDefinitionNode fdn = (FunctionDefinitionNode) callTarget.getRootNode();
        // Cannot just serialize fdn, as this needs to generate slightly different output
        fdn.serializeFormals(state);
        state.openPairList(SEXPTYPE.LISTSXP);
        fdn.getBody().serialize(state);
        state.setCdr(state.closePairList());
        state.setCdr(state.closePairList());
    }

    @Override
    public RSyntaxNode substitute(REnvironment env) {
        FunctionDefinitionNode thisFdn = (FunctionDefinitionNode) callTarget.getRootNode();
        FunctionDefinitionNode fdn = (FunctionDefinitionNode) thisFdn.substitute(env);
        return new FunctionExpressionNode(null, Truffle.getRuntime().createCallTarget(fdn));
    }
}
