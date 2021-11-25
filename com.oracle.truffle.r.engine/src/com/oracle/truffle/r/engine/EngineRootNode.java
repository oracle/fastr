/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.engine;

import java.util.List;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLanguage.ContextReference;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.ProbeNode;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.nodes.REntryPointRootNode;
import com.oracle.truffle.r.runtime.ExitException;
import com.oracle.truffle.r.runtime.JumpToTopLevelException;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RSource;
import com.oracle.truffle.r.runtime.ReturnException;
import com.oracle.truffle.r.runtime.Utils.DebugExitException;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.TruffleRLanguage;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.interop.R2Foreign;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

class EngineRootNode extends RootNode implements REntryPointRootNode {
    private final SourceSection sourceSection;

    private final MaterializedFrame executionFrame;
    private final ContextReference<RContext> contextReference;

    @Child private EngineBodyNode bodyNode;
    @Child private R2Foreign r2Foreign = R2Foreign.create();
    @Child private InteropLibrary exceptionInterop;

    EngineRootNode(EngineBodyNode bodyNode, RContext context, SourceSection sourceSection, MaterializedFrame executionFrame) {
        super(context.getLanguage());
        this.sourceSection = sourceSection;
        this.bodyNode = bodyNode;
        this.executionFrame = executionFrame;
        this.contextReference = lookupContextReference(TruffleRLanguage.class);
    }

    public static EngineRootNode createEngineRoot(REngine engine, RContext context, List<RSyntaxNode> statements, SourceSection sourceSection, MaterializedFrame executionFrame,
                    boolean forcePrintResult) {
        return new EngineRootNode(new EngineBodyNode(engine, statements, forcePrintResult || getPrintResult(sourceSection)), context, sourceSection, executionFrame);
    }

    @Override
    public Frame getActualExecutionFrame(@SuppressWarnings("unused") Frame currentFrame) {
        return executionFrame != null ? executionFrame : contextReference.get().stateREnvironment.getGlobalFrame();
    }

    /**
     * The normal {@link REngine#doMakeCallTarget} happens first, then we actually run the call
     * using the standard FastR machinery, saving and restoring the {@link RContext}, since we have
     * no control over what that might be when the call is initiated.
     */
    @Override
    public Object execute(VirtualFrame frame) {
        Object actualFrame = getActualExecutionFrame(frame);
        try {
            return r2Foreign.convert(this.bodyNode.execute(actualFrame));
        } catch (ReturnException ex) {
            return ex.getResult();
        } catch (DebugExitException | JumpToTopLevelException | ExitException | ThreadDeath | RError e) {
            CompilerDirectives.transferToInterpreter();
            throw e;
        } catch (Throwable t) {
            CompilerDirectives.transferToInterpreter();
            if (getExceptionInterop().isException(t)) {
                try {
                    throw getExceptionInterop().throwException(t);
                } catch (UnsupportedMessageException ex) {
                    throw CompilerDirectives.shouldNotReachHere(ex);
                }
            }
            // other errors didn't produce an output yet
            RInternalError.reportError(t);
            throw t;
        }
    }

    @Override
    public SourceSection getSourceSection() {
        return sourceSection;
    }

    public static boolean isEngineBody(Object node) {
        return node instanceof EngineBodyNode;
    }

    private static boolean getPrintResult(SourceSection sourceSection) {
        // can't print if initializing the system in embedded mode (no builtins yet)
        return !sourceSection.getSource().getName().equals(RSource.Internal.INIT_EMBEDDED.string) && sourceSection.getSource().isInteractive();
    }

    public InteropLibrary getExceptionInterop() {
        if (exceptionInterop == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            exceptionInterop = insert(InteropLibrary.getFactory().createDispatched(3));
        }
        return exceptionInterop;
    }

    private static final class EngineBodyNode extends Node implements InstrumentableNode {

        private final REngine engine;
        private final List<RSyntaxNode> statements;
        @Children protected final DirectCallNode[] calls;
        private final boolean printResult;

        EngineBodyNode(REngine engine, List<RSyntaxNode> statements, boolean printResult) {
            this.engine = engine;
            this.statements = statements;
            this.calls = new DirectCallNode[statements.size()];
            this.printResult = printResult;
            createNodes();
        }

        @ExplodeLoop
        Object execute(Object actualFrame) {
            Object lastValue = RNull.instance;
            for (int i = 0; i < calls.length; i++) {
                lastValue = calls[i].call(new Object[]{actualFrame});
            }
            return lastValue;
        }

        @Override
        public boolean isInstrumentable() {
            return false;
        }

        @Override
        public WrapperNode createWrapper(ProbeNode probe) {
            return null;
        }

        @Override
        public boolean hasTag(Class<? extends Tag> tag) {
            return false;
        }

        private void createNodes() {
            for (int i = 0; i < calls.length; i++) {
                RNode node = statements.get(i).asRNode();
                calls[i] = insert(Truffle.getRuntime().createDirectCallNode(engine.doMakeCallTarget(node, RSource.Internal.REPL_WRAPPER.string, printResult, true)));
            }
        }
    }
}
