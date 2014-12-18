/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

import java.util.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.instrument.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.access.FrameSlotNode.InternalFrameSlot;
import com.oracle.truffle.r.nodes.control.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.RDeparse.State;
import com.oracle.truffle.r.runtime.env.*;

public final class FunctionDefinitionNode extends RRootNode implements RSyntaxNode {

    /**
     * Identifies the lexical scope where this function is defined, through the "parent" field.
     */
    private final REnvironment.FunctionDefinition funcEnv;
    @Child private RNode body; // typed as RNode to avoid custom instrument wrapper
    private final RNode uninitializedBody; // copy for "body" builtin
    private final String description;
    private final FunctionUID uuid;

    @Child private FrameSlotNode onExitSlot;
    @Child private InlineCacheNode<VirtualFrame, RNode> onExitExpressionCache;
    private final ConditionProfile onExitProfile = ConditionProfile.createBinaryProfile();
    private final ValueProfile enclosingFrameProfile = ValueProfile.createIdentityProfile();

    /**
     * An instance of this node may be called from with the intention to have its execution leave a
     * footprint behind in a specific frame/environment, e.g., during library loading, commands from
     * the shell, or R's {@code eval} and its friends. In that case, {@code substituteFrame} is
     * {@code true}, and the {@link #execute(VirtualFrame)} method must be invoked with one
     * argument, namely the {@link VirtualFrame} to be side-effected. Execution will then proceed in
     * the context of that frame. Note that passing only this one frame argument, strictly spoken,
     * violates the frame layout as set forth in {@link RArguments}. This is for internal use only.
     */
    private final boolean substituteFrame;

    /**
     * Profiling for catching {@link ReturnException}s.
     */
    private final BranchProfile returnProfile = BranchProfile.create();

    public FunctionDefinitionNode(SourceSection src, REnvironment.FunctionDefinition funcEnv, RNode body, FormalArguments formals, String description, boolean substituteFrame) {
        this(src, funcEnv, body, formals, description, substituteFrame, false);
    }

    // TODO skipOnExit: Temporary solution to allow onExit to be switched of; used for
    // REngine.evalPromise
    public FunctionDefinitionNode(SourceSection src, REnvironment.FunctionDefinition funcEnv, RNode body, FormalArguments formals, String description, boolean substituteFrame, boolean skipExit) {
        super(src, formals, funcEnv.getDescriptor());
        this.funcEnv = funcEnv;
        this.body = body;
        this.uninitializedBody = body;
        this.description = description;
        this.substituteFrame = substituteFrame;
        this.onExitSlot = skipExit ? null : FrameSlotNode.create(InternalFrameSlot.OnExit, false);
        this.instrumentationApplied = substituteFrame;
        this.uuid = substituteFrame ? null : FunctionUIDFactory.get().createUID();
    }

    public FunctionUID getUID() {
        return uuid;
    }

    public REnvironment.FunctionDefinition getEnv() {
        return funcEnv;
    }

    public FunctionBodyNode getBody() {
        return (FunctionBodyNode) RASTUtils.unwrap(body);
    }

    public FunctionBodyNode getUninitializedBody() {
        return (FunctionBodyNode) uninitializedBody;
    }

    /**
     * @see #substituteFrame
     */
    @Override
    public Object execute(VirtualFrame frame) {
        VirtualFrame vf = substituteFrame ? (VirtualFrame) frame.getArguments()[0] : frame;
        try {
            RArguments.setEnclosingFrame(vf, enclosingFrameProfile.profile(RArguments.getEnclosingFrame(vf)));
            return body.execute(vf);
        } catch (ReturnException ex) {
            returnProfile.enter();
            return ex.getResult();
        } finally {
            if (onExitProfile.profile(onExitSlot != null && onExitSlot.hasValue(vf))) {
                if (onExitExpressionCache == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    onExitExpressionCache = insert(InlineCacheNode.createExpression(3));
                }
                // Must preserve the visibility state as it may be changed by the on.exit expression
                boolean isVisible = RContext.isVisible();
                ArrayList<Object> current = getCurrentOnExitList(vf, onExitSlot.executeFrameSlot(vf));
                for (Object expr : current) {
                    if (!(expr instanceof RNode)) {
                        RInternalError.shouldNotReachHere("unexpected type for on.exit entry");
                    }
                    RNode node = (RNode) expr;
                    onExitExpressionCache.execute(vf, node);
                }
                RContext.setVisible(isVisible);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static ArrayList<Object> getCurrentOnExitList(VirtualFrame frame, FrameSlot slot) {
        try {
            return (ArrayList<Object>) frame.getObject(slot);
        } catch (FrameSlotTypeException e) {
            throw RInternalError.shouldNotReachHere();
        }
    }

    @Override
    public String toString() {
        return description == null ? "<no source>" : description;
    }

    public String parentToString() {
        return super.toString();
    }

    @Override
    public boolean isCloningAllowed() {
        return !substituteFrame;
    }

    @Override
    public boolean isSyntax() {
        return true;
    }

    public void deparse(State state) {
        // TODO linebreaks
        state.append("function (");
        FormalArguments formals = getFormalArguments();
        String[] formalNames = formals.getNames();
        RNode[] defaultArgs = formals.getDefaultArgs();
        for (int i = 0; i < formalNames.length; i++) {
            RNode defaultArg = defaultArgs[i];
            state.append(formalNames[i]);
            if (defaultArg != null) {
                state.append(" = ");
                defaultArg.deparse(state);
            }
            if (i != formalNames.length - 1) {
                state.append(", ");
            }
        }
        state.append(") ");
        state.writeNLOpenCurlyIncIndent();
        state.writeline();
        body.deparse(state);
        state.decIndentWriteCloseCurly();
    }

    /**
     * Since a {@link FunctionDefinitionNode} is not a subclass of an {@link RNode} we cannot just
     * override the {@link RSyntaxNode#substitute} method as that requires an {@link RNode} result.
     * So we define this custom method. N.B. The formal arguments are <b>not</b> subject to
     * substitution, just the body.
     */
    public FunctionDefinitionNode substituteFDN(REnvironment env) {
        return new FunctionDefinitionNode(null, funcEnv, body.substitute(env), getFormalArguments(), null, substituteFrame);
    }

    @Override
    public RNode substitute(REnvironment env) {
        throw RInternalError.shouldNotReachHere();
    }

    private boolean instrumentationApplied = false;

    @Override
    public void applyInstrumentation() {
        if (RContext.getEngine().instrumentingEnabled() && !instrumentationApplied) {
            Probe.applyASTProbers(body);
            instrumentationApplied = true;
        }
    }

    /**
     * A workaround for not representing left curly brace as a function call. We have to depend on
     * the source section and "parse" the start of the function definition.
     */
    @TruffleBoundary
    public boolean hasBraces() {
        SourceSection src = getSourceSection();
        if (src == null) {
            return true; // statistcally probable (must be a substituted function)
        }
        String s = src.getCode();
        int ix = s.indexOf('(') + 1;
        int bdepth = 1;
        while (ix < s.length() && bdepth > 0) {
            char ch = s.charAt(ix);
            if (ch == '(') {
                bdepth++;
            } else if (ch == ')') {
                bdepth--;
            }
            ix++;
        }
        while (ix < s.length()) {
            char ch = s.charAt(ix);
            boolean whitespace = Character.isWhitespace(ch);
            if (!whitespace) {
                return ch == '{';
            }
            ix++;
        }
        return false;
    }

}
