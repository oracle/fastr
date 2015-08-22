/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.engine.repl;

import java.util.*;

import com.oracle.truffle.api.debug.*;
import com.oracle.truffle.api.instrument.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.api.vm.*;
import com.oracle.truffle.api.vm.TruffleVM.Language;
import com.oracle.truffle.r.engine.*;
import com.oracle.truffle.r.engine.shell.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.context.*;
import com.oracle.truffle.tools.debug.shell.*;
import com.oracle.truffle.tools.debug.shell.client.*;
import com.oracle.truffle.tools.debug.shell.server.*;

/**
 * A first cut at a REPL server for the FastR implementation.
 */
public final class RREPLServer extends REPLServer {

    public static void main(String[] args) {

        // Cheating for the prototype: start from R, rather than from the client.
        final RREPLServer server = new RREPLServer(args);
        final SimpleREPLClient client = new SimpleREPLClient(server.language.getShortName(), server);

        // Cheating for the prototype: allow server access to client for recursive debugging
        server.setClient(client);

        try {
            client.start();
        } catch (QuitException ex) {
        }
    }

    private final Language language;
    private TruffleVM vm;
    private Debugger db;
    private final String statusPrefix;
    private final Map<String, REPLHandler> handlerMap = new HashMap<>();
    private RREPLServerContext currentServerContext;
    private SimpleREPLClient client = null;

    private void add(REPLHandler fileHandler) {
        handlerMap.put(fileHandler.getOp(), fileHandler);
    }

    public RREPLServer(String[] args) {
        // default handlers
        add(REPLHandler.BREAK_AT_LINE_HANDLER);
        add(REPLHandler.BREAK_AT_LINE_ONCE_HANDLER);
        add(REPLHandler.BREAK_AT_THROW_HANDLER);
        add(REPLHandler.BREAK_AT_THROW_ONCE_HANDLER);
        add(REPLHandler.BREAKPOINT_INFO_HANDLER);
        add(REPLHandler.CLEAR_BREAK_HANDLER);
        add(REPLHandler.CONTINUE_HANDLER);
        add(REPLHandler.DELETE_HANDLER);
        add(REPLHandler.DISABLE_BREAK_HANDLER);
        add(REPLHandler.ENABLE_BREAK_HANDLER);
        add(REPLHandler.FILE_HANDLER);
        add(REPLHandler.KILL_HANDLER);
        add(REPLHandler.QUIT_HANDLER);
        add(REPLHandler.SET_BREAK_CONDITION_HANDLER);
        add(REPLHandler.STEP_INTO_HANDLER);
        add(REPLHandler.STEP_OUT_HANDLER);
        add(REPLHandler.STEP_OVER_HANDLER);
        add(REPLHandler.TRUFFLE_HANDLER);
        add(REPLHandler.TRUFFLE_NODE_HANDLER);
        // R specific handlers
        add(RREPLHandler.BACKTRACE_HANDLER);
        add(RREPLHandler.EVAL_HANDLER);
        add(RREPLHandler.LOAD_RUN_FILE_HANDLER);
        add(RREPLHandler.R_FRAME_HANDLER);
        add(RREPLHandler.INFO_HANDLER);

        EventConsumer<SuspendedEvent> onHalted = new EventConsumer<SuspendedEvent>(SuspendedEvent.class) {
            @Override
            protected void on(SuspendedEvent ev) {
                RREPLServer.this.haltedAt(ev);
            }
        };
        EventConsumer<ExecutionEvent> onExec = new EventConsumer<ExecutionEvent>(ExecutionEvent.class) {
            @Override
            protected void on(ExecutionEvent event) {
                event.prepareStepInto();
                db = event.getDebugger();
            }
        };

        /*
         * We call a special RCommand entry point that does most of the normal initialization but
         * returns the initial RContext which has not yet been activated, which means that the
         * TruffleVM has not yet been built, but the TruffleVM.Builder has been created.
         */

        String[] debugArgs = new String[args.length + 1];
        debugArgs[0] = "--debugger=rrepl";
        System.arraycopy(args, 0, debugArgs, 1, args.length);
        RCmdOptions options = RCmdOptions.parseArguments(RCmdOptions.Client.R, args);
        ContextInfo info = RCommand.createContextInfoFromCommandLine(options);
        RContext.tempInitializingContextInfo = info;
        this.vm = RContextFactory.create(info, builder -> builder.onEvent(onHalted).onEvent(onExec));
        this.language = vm.getLanguages().get(TruffleRLanguage.MIME);
        assert language != null;

        this.statusPrefix = language.getShortName() + " REPL:";
    }

    private void setClient(SimpleREPLClient client) {
        this.client = client;
    }

    @Override
    public REPLMessage start() {

        // Complete initialization of instrumentation & debugging contexts.

        this.currentServerContext = new RREPLServerContext(null, null);

        final REPLMessage reply = new REPLMessage();
        reply.put(REPLMessage.STATUS, REPLMessage.SUCCEEDED);
        reply.put(REPLMessage.DISPLAY_MSG, language.getShortName() + " started");
        return reply;
    }

    @Override
    public REPLMessage[] receive(REPLMessage request) {
        if (currentServerContext == null) {
            final REPLMessage message = new REPLMessage();
            message.put(REPLMessage.STATUS, REPLMessage.FAILED);
            message.put(REPLMessage.DISPLAY_MSG, "server not started");
            final REPLMessage[] reply = new REPLMessage[]{message};
            return reply;
        }
        return currentServerContext.receive(request);
    }

    /**
     * Execution context of a halted R program.
     */
    public final class RREPLServerContext extends REPLServerContext {

        private final RREPLServerContext predecessor;

        public RREPLServerContext(RREPLServerContext predecessor, SuspendedEvent event) {
            super(predecessor == null ? 0 : predecessor.getLevel() + 1, event);
            this.predecessor = predecessor;
        }

        @Override
        public REPLMessage[] receive(REPLMessage request) {
            final String command = request.get(REPLMessage.OP);
            final REPLHandler handler = handlerMap.get(command);

            if (handler == null) {
                final REPLMessage message = new REPLMessage();
                message.put(REPLMessage.OP, command);
                message.put(REPLMessage.STATUS, REPLMessage.FAILED);
                message.put(REPLMessage.DISPLAY_MSG, statusPrefix + " op \"" + command + "\" not supported");
                final REPLMessage[] reply = new REPLMessage[]{message};
                return reply;
            }
            return handler.receive(request, currentServerContext);
        }

        @Override
        public Language getLanguage() {
            return language;
        }

        @Override
        public TruffleVM vm() {
            return vm;
        }

        @Override
        protected Debugger db() {
            return db;
        }

        @Override
        public void registerBreakpoint(Breakpoint breakpoint) {
            RREPLServer.this.registerBreakpoint(breakpoint);

        }

        @Override
        public Breakpoint findBreakpoint(int id) {
            return RREPLServer.this.findBreakpoint(id);
        }

        @Override
        public int getBreakpointID(Breakpoint breakpoint) {
            return RREPLServer.this.getBreakpointID(breakpoint);
        }

    }

    void haltedAt(SuspendedEvent event) {
        // Create and push a new debug context where execution is halted
        currentServerContext = new RREPLServerContext(currentServerContext, event);

        // Message the client that execution is halted and is in a new debugging context
        final REPLMessage message = new REPLMessage();
        message.put(REPLMessage.OP, REPLMessage.STOPPED);
        final SourceSection src = event.getNode().getSourceSection();
        final Source source = src.getSource();
        message.put(REPLMessage.SOURCE_NAME, source.getName());
        message.put(REPLMessage.FILE_PATH, source.getPath());
        message.put(REPLMessage.LINE_NUMBER, Integer.toString(src.getStartLine()));
        message.put(REPLMessage.STATUS, REPLMessage.SUCCEEDED);
        message.put(REPLMessage.DEBUG_LEVEL, Integer.toString(currentServerContext.getLevel()));

        try {
            // Cheat with synchrony: call client directly about entering a nested debugging
            // context.
            if (client != null) {
                client.halted(message);
            }
        } finally {
            // Returns when "continue" is called in the new debugging context

            // Pop the debug context, and return so that the old context will continue
            currentServerContext = currentServerContext.predecessor;
        }
    }

}
