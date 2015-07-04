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
package com.oracle.truffle.r.repl;

import java.util.*;

import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.instrument.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.tools.debug.engine.*;
import com.oracle.truffle.tools.debug.shell.*;
import com.oracle.truffle.tools.debug.shell.server.*;

/**
 * Request handlers for the {@link RREPLServer}; these should be stateless.
 */
public abstract class RREPLHandler extends REPLHandler {

    public RREPLHandler(String op) {
        super(op);
    }

    public static final RREPLHandler EVAL_HANDLER = new RREPLHandler(REPLMessage.EVAL) {

        @Override
        public REPLMessage[] receive(REPLMessage request, REPLServerContext serverContext) {
            final String sourceName = request.get(REPLMessage.SOURCE_NAME);
            final REPLMessage message = new REPLMessage(REPLMessage.OP, REPLMessage.EVAL);
            message.put(REPLMessage.SOURCE_NAME, sourceName);
            message.put(REPLMessage.DEBUG_LEVEL, Integer.toString(serverContext.getLevel()));

            final Source source = Source.fromText(request.get(REPLMessage.CODE), sourceName);
            final MaterializedFrame frame = serverContext.getFrame();
            try {
                if (frame == null) { // Top Level
                    return finishReplyFailed(message, "no active engine");
                } else {
                    final Object returnValue = serverContext.getDebugEngine().eval(source, serverContext.getNode(), frame);
                    return finishReplySucceeded(message, serverContext.getLanguage().getToolSupport().getVisualizer().displayValue(returnValue, 0));
                }
            } catch (QuitException ex) {
                throw ex;
            } catch (KillException ex) {
                return finishReplySucceeded(message, "eval (" + sourceName + ") killed");
            } catch (Exception ex) {
                return finishReplyFailed(message, ex.toString());
            }
        }
    };

    public static final RREPLHandler INFO_HANDLER = new RREPLHandler(REPLMessage.INFO) {

        @Override
        public REPLMessage[] receive(REPLMessage request, REPLServerContext serverContext) {
            final String topic = request.get(REPLMessage.TOPIC);

            if (topic == null || topic.isEmpty()) {
                final REPLMessage message = new REPLMessage(REPLMessage.OP, REPLMessage.INFO);
                return finishReplyFailed(message, "No info topic specified");
            }

            switch (topic) {
                case REPLMessage.LANGUAGE:
                    return createLanguageInfoReply();

                default:
                    final REPLMessage message = new REPLMessage(REPLMessage.OP, REPLMessage.INFO);
                    return finishReplyFailed(message, "No info about topic \"" + topic + "\"");
            }
        }
    };

    private static REPLMessage[] createLanguageInfoReply() {
        final ArrayList<REPLMessage> langMessages = new ArrayList<>();

        return langMessages.toArray(new REPLMessage[0]);
    }

    /**
     * Returns a general description of the frame, plus a textual summary of the slot values: one
     * per line. Custiom version for FastR that does not show anonymous frame slots.
     */
    public static final REPLHandler R_FRAME_HANDLER = new REPLHandler(REPLMessage.FRAME) {

        @Override
        public REPLMessage[] receive(REPLMessage request, REPLServerContext serverContext) {
            final REPLMessage reply = createReply();
            final Integer frameNumber = request.getIntValue(REPLMessage.FRAME_NUMBER);
            if (frameNumber == null) {
                return finishReplyFailed(reply, "no frame number specified");
            }
            final List<FrameDebugDescription> stack = serverContext.getDebugEngine().getStack();
            if (frameNumber < 0 || frameNumber >= stack.size()) {
                return finishReplyFailed(reply, "frame number " + frameNumber + " out of range");
            }
            final FrameDebugDescription frameDescription = stack.get(frameNumber);
            final REPLMessage frameMessage = createFrameInfoMessage(serverContext, frameDescription);
            final Frame frame = RArguments.unwrap(frameDescription.frameInstance().getFrame(FrameInstance.FrameAccess.READ_ONLY, true));
            final FrameDescriptor frameDescriptor = frame.getFrameDescriptor();
            Visualizer visualizer = serverContext.getLanguage().getToolSupport().getVisualizer();
            try {
                final StringBuilder sb = new StringBuilder();
                for (FrameSlot slot : frameDescriptor.getSlots()) {
                    String slotName = slot.getIdentifier().toString();
                    if (!AnonymousFrameVariable.isAnonymous(slotName)) {
                        sb.append(Integer.toString(slot.getIndex()) + ": " + visualizer.displayIdentifier(slot) + " = ");
                        try {
                            final Object value = frame.getValue(slot);
                            sb.append(visualizer.displayValue(value, 0));
                        } catch (Exception ex) {
                            sb.append("???");
                        }
                        sb.append("\n");
                    }
                }
                return finishReplySucceeded(frameMessage, sb.toString());
            } catch (Exception ex) {
                return finishReplyFailed(frameMessage, ex.toString());
            }
        }
    };

    public static final RREPLHandler LOAD_RUN_FILE_HANDLER = new RREPLHandler(REPLMessage.LOAD_RUN) {

        @Override
        public REPLMessage[] receive(REPLMessage request, REPLServerContext serverContext) {
            final REPLMessage message = new REPLMessage(REPLMessage.OP, REPLMessage.LOAD_RUN);
            final String fileName = request.get(REPLMessage.SOURCE_NAME);

            try {
                Source source = null;
                if (fileName.endsWith("rshell")) {
                    // workaround as there is no "shell" command
                    source = Source.fromText("", "<shell>");
                } else {
                    source = Source.fromFileName(fileName, true);
                    if (source == null) {
                        return finishReplyFailed(message, "can't find file \"" + fileName + "\"");
                    }
                }
                serverContext.getDebugEngine().run(source, false);
                message.put(REPLMessage.FILE_PATH, source.getPath());
                return finishReplySucceeded(message, source.getName() + "  exited");
            } catch (QuitException ex) {
                throw ex;
            } catch (KillException ex) {
                return finishReplySucceeded(message, fileName + " killed");
            } catch (Exception ex) {
                return finishReplyFailed(message, "error loading file \"" + fileName + "\": " + ex.getMessage());
            }
        }
    };

}
