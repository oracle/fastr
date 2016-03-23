/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.library.fastr;

import java.io.IOException;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.vm.PolyglotEngine;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.RChannel;
import com.oracle.truffle.r.runtime.RCmdOptions;
import com.oracle.truffle.r.runtime.RCmdOptions.Client;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RInternalSourceDescriptions;
import com.oracle.truffle.r.runtime.conn.StdConnections;
import com.oracle.truffle.r.runtime.context.ContextInfo;
import com.oracle.truffle.r.runtime.context.Engine.ParseException;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

public class FastRContext {

    public abstract static class Create extends RExternalBuiltinNode.Arg2 {
        @Specialization
        @TruffleBoundary
        protected int create(RAbstractStringVector args, RAbstractIntVector kindVec) {
            RContext.ContextKind kind = RContext.ContextKind.VALUES[kindVec.getDataAt(0) - 1];
            RCmdOptions options = RCmdOptions.parseArguments(Client.RSCRIPT, args.materialize().getDataCopy());
            return ContextInfo.createDeferred(options, kind, RContext.getInstance(), RContext.getInstance().getConsoleHandler());
        }
    }

    public abstract static class Get extends RExternalBuiltinNode.Arg0 {
        @Specialization
        @TruffleBoundary
        protected Object get() {
            return RContext.getInstance();
        }
    }

    public abstract static class Print extends RExternalBuiltinNode.Arg1 {
        @Specialization
        @TruffleBoundary
        protected RNull print(RAbstractIntVector ctxt) {
            if (ctxt.getLength() != 1) {
                throw RError.error(this, RError.Message.INVALID_ARGUMENT, "context");
            }
            int contextId = ctxt.getDataAt(0);
            ContextInfo info = ContextInfo.get(contextId);
            try {
                if (info == null) {
                    StdConnections.getStdout().writeString("obsolete context: " + contextId, true);
                } else {
                    StdConnections.getStdout().writeString("context: " + contextId, true);
                }
                return RNull.instance;
            } catch (IOException ex) {
                throw RError.error(this, RError.Message.GENERIC, ex.getMessage());
            }
        }
    }

    public abstract static class Spawn extends RExternalBuiltinNode.Arg2 {
        @Specialization
        @TruffleBoundary
        protected RNull eval(RAbstractIntVector contexts, RAbstractStringVector exprs) {
            RContext.EvalThread[] threads = new RContext.EvalThread[contexts.getLength()];
            for (int i = 0; i < threads.length; i++) {
                ContextInfo info = checkContext(contexts.getDataAt(i), this);
                threads[i] = new RContext.EvalThread(info, Source.fromText(exprs.getDataAt(i % threads.length), RInternalSourceDescriptions.CONTEXT_EVAL).withMimeType(RRuntime.R_APP_MIME));
            }
            for (int i = 0; i < threads.length; i++) {
                threads[i].start();
            }
            return RNull.instance;
        }
    }

    public abstract static class Join extends RExternalBuiltinNode.Arg1 {
        @Specialization
        protected RNull eval(RAbstractIntVector contexts) {
            try {
                for (int i = 0; i < contexts.getLength(); i++) {
                    Thread thread = RContext.EvalThread.threads.get(contexts.getDataAt(i));
                    if (thread == null) {
                        // already done
                        continue;
                    } else {
                        thread.join();
                    }
                }
            } catch (InterruptedException ex) {
                throw RError.error(this, RError.Message.GENERIC, "error finishing eval thread");

            }
            return RNull.instance;
        }
    }

    /**
     * The result of eval is a list of lists. The top level list has the same number of entries as
     * the number of contexts. The sublist contains the result of the evaluation with name "result".
     * It may also have an attribute "error" if the evaluation threw an exception, in which case the
     * result will be NA.
     */
    public abstract static class Eval extends RExternalBuiltinNode.Arg3 {
        @Specialization
        @TruffleBoundary
        protected Object eval(RAbstractIntVector contexts, RAbstractStringVector exprs, byte par) {
            Object[] results = new Object[contexts.getLength()];
            if (RRuntime.fromLogical(par)) {
                RContext.EvalThread[] threads = new RContext.EvalThread[contexts.getLength()];
                for (int i = 0; i < threads.length; i++) {
                    ContextInfo info = checkContext(contexts.getDataAt(i), this);
                    threads[i] = new RContext.EvalThread(info, Source.fromText(exprs.getDataAt(i % threads.length), RInternalSourceDescriptions.CONTEXT_EVAL).withMimeType(RRuntime.R_APP_MIME));
                }
                for (int i = 0; i < threads.length; i++) {
                    threads[i].start();
                }
                try {
                    for (int i = 0; i < threads.length; i++) {
                        threads[i].join();
                        results[i] = threads[i].getEvalResult();
                    }
                } catch (InterruptedException ex) {
                    throw RError.error(this, RError.Message.GENERIC, "error finishing eval thread");
                }
            } else {
                for (int i = 0; i < contexts.getLength(); i++) {
                    ContextInfo info = checkContext(contexts.getDataAt(i), this);
                    PolyglotEngine vm = info.apply(PolyglotEngine.newBuilder()).build();
                    try {
                        Source source = Source.fromText(exprs.getDataAt(i % exprs.getLength()), RInternalSourceDescriptions.CONTEXT_EVAL).withMimeType(RRuntime.R_APP_MIME);
                        PolyglotEngine.Value resultValue = vm.eval(source);
                        results[i] = RContext.EvalThread.createEvalResult(resultValue);
                    } catch (ParseException e) {
                        e.report(info.getConsoleHandler());
                        results[i] = RContext.EvalThread.createErrorResult(e.getMessage());
                    } catch (IOException e) {
                        // This is an unhandled exception, e.g. RInternalError
                        Throwable cause = e.getCause();
                        if (cause instanceof RInternalError) {
                            info.getConsoleHandler().println("internal error: " + e.getMessage() + " (see fastr_errors.log)");
                            RInternalError.reportError(e);
                        }
                        results[i] = RContext.EvalThread.createErrorResult(e.getCause().getMessage());
                    } finally {
                        vm.dispose();
                    }
                }
            }
            return RDataFactory.createList(results);
        }
    }

    private static ContextInfo checkContext(int contextId, RBaseNode invokingNode) throws RError {
        ContextInfo info = ContextInfo.get(contextId);
        if (info == null) {
            throw RError.error(invokingNode, RError.Message.GENERIC, "no context: " + contextId);
        } else {
            return info;
        }
    }

    private static int wrongChannelArg(RBaseNode baseNode, Object arg, String argName) {
        if (!(arg instanceof RAbstractIntVector)) {
            throw RError.error(baseNode, RError.Message.INVALID_ARG_TYPE);
        } else {
            // guard failed
            throw RError.error(baseNode, RError.Message.WRONG_LENGTH_ARG, argName);
        }
    }

    public abstract static class CreateChannel extends RExternalBuiltinNode.Arg1 {
        @Specialization(guards = "key.getLength() == 1")
        @TruffleBoundary
        protected int createChannel(RAbstractIntVector key) {
            return RChannel.createChannel(key.getDataAt(0));
        }

        @Fallback
        protected int error(Object key) {
            return wrongChannelArg(this, key, "key");
        }
    }

    public abstract static class GetChannel extends RExternalBuiltinNode.Arg1 {
        @Specialization(guards = "key.getLength() == 1")
        @TruffleBoundary
        protected int getChannel(RAbstractIntVector key) {
            return RChannel.getChannel(key.getDataAt(0));
        }

        @Fallback
        protected int error(Object key) {
            return wrongChannelArg(this, key, "key");
        }
    }

    public abstract static class CloseChannel extends RExternalBuiltinNode.Arg1 {
        @Specialization(guards = "id.getLength() == 1")
        @TruffleBoundary
        protected RNull getChannel(RAbstractIntVector id) {
            RChannel.closeChannel(id.getDataAt(0));
            return RNull.instance;
        }

        @Fallback
        protected int error(Object id) {
            return wrongChannelArg(this, id, "id");
        }
    }

    public abstract static class ChannelSend extends RExternalBuiltinNode.Arg2 {
        @Specialization(guards = "id.getLength() == 1")
        @TruffleBoundary
        protected RNull send(RAbstractIntVector id, Object data) {
            RChannel.send(id.getDataAt(0), data);
            return RNull.instance;
        }

        @Fallback
        protected int error(Object id, @SuppressWarnings("unused") Object data) {
            return wrongChannelArg(this, id, "id");
        }
    }

    public abstract static class ChannelReceive extends RExternalBuiltinNode.Arg1 {
        @Specialization(guards = "id.getLength() == 1")
        @TruffleBoundary
        protected Object receive(RAbstractIntVector id) {
            return RChannel.receive(id.getDataAt(0));
        }

        @Fallback
        protected int error(Object id) {
            return wrongChannelArg(this, id, "id");
        }
    }

    public abstract static class ChannelPoll extends RExternalBuiltinNode.Arg1 {
        @Specialization(guards = "id.getLength() == 1")
        @TruffleBoundary
        protected Object poll(RAbstractIntVector id) {
            return RChannel.poll(id.getDataAt(0));
        }

        @Fallback
        protected int error(Object id) {
            return wrongChannelArg(this, id, "id");
        }
    }

    public abstract static class ChannelSelect extends RExternalBuiltinNode.Arg1 {
        @Specialization
        @TruffleBoundary
        protected RList select(RList nodes) {
            int ind = 0;
            int length = nodes.getLength();
            while (true) {
                Object o = nodes.getDataAt(ind);
                ind = (ind + 1) % length;
                int id;
                if (o instanceof Integer) {
                    id = (int) o;
                } else {
                    id = ((RIntVector) o).getDataAt(0);
                }
                Object res = RChannel.poll(id);
                if (res != null) {
                    return RDataFactory.createList(new Object[]{id, res});
                }
            }
        }
    }
}
