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
package com.oracle.truffle.r.nodes.builtin.fastr;

import java.io.IOException;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.vm.PolyglotEngine;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.RBuiltinKind;
import com.oracle.truffle.r.runtime.RChannel;
import com.oracle.truffle.r.runtime.RCmdOptions;
import com.oracle.truffle.r.runtime.RCmdOptions.Client;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RInternalSourceDescriptions;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RVisibility;
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

    @RBuiltin(name = ".fastr.context.create", kind = RBuiltinKind.PRIMITIVE, parameterNames = {"args", "kind"})
    public abstract static class Create extends RBuiltinNode {

        @Override
        public Object[] getDefaultParameterValues() {
            return new Object[]{"", "SHARE_NOTHING"};
        }

        @Specialization
        @TruffleBoundary
        protected int create(RAbstractStringVector args, RAbstractStringVector kindVec) {
            controlVisibility();
            try {
                RContext.ContextKind kind = RContext.ContextKind.valueOf(kindVec.getDataAt(0));
                RCmdOptions options = RCmdOptions.parseArguments(Client.RSCRIPT, args.materialize().getDataCopy());
                return ContextInfo.createDeferred(options, kind, RContext.getInstance(), RContext.getInstance().getConsoleHandler());
            } catch (IllegalArgumentException ex) {
                throw RError.error(this, RError.Message.GENERIC, "invalid kind argument");
            }
        }

        @SuppressWarnings("unused")
        @Fallback
        protected int create(Object args, Object kindVec) {
            throw RError.error(this, RError.Message.INVALID_OR_UNIMPLEMENTED_ARGUMENTS);
        }
    }

    @RBuiltin(name = ".fastr.context.get", kind = RBuiltinKind.PRIMITIVE, parameterNames = {})
    public abstract static class Get extends RBuiltinNode {
        @Specialization
        @TruffleBoundary
        protected Object get() {
            controlVisibility();
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

    @RBuiltin(name = ".fastr.context.spawn", visibility = RVisibility.OFF, kind = RBuiltinKind.PRIMITIVE, parameterNames = {"contexts", "exprs"})
    public abstract static class Spawn extends RBuiltinNode {
        @Specialization
        @TruffleBoundary
        protected RNull spawn(RAbstractIntVector contexts, RAbstractStringVector exprs) {
            controlVisibility();
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

        @SuppressWarnings("unused")
        @Fallback
        protected RNull spawn(Object contexts, Object exprs) {
            throw RError.error(this, RError.Message.INVALID_OR_UNIMPLEMENTED_ARGUMENTS);
        }
    }

    @RBuiltin(name = ".fastr.context.join", visibility = RVisibility.OFF, kind = RBuiltinKind.PRIMITIVE, parameterNames = {"contexts"})
    public abstract static class Join extends RBuiltinNode {
        @Specialization
        protected RNull eval(RAbstractIntVector contexts) {
            controlVisibility();
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

        @SuppressWarnings("unused")
        @Fallback
        protected RNull join(Object contexts) {
            throw RError.error(this, RError.Message.INVALID_OR_UNIMPLEMENTED_ARGUMENTS);
        }
    }

    /**
     * The result of eval is a list of lists. The top level list has the same number of entries as
     * the number of contexts. The sublist contains the result of the evaluation with name "result".
     * It may also have an attribute "error" if the evaluation threw an exception, in which case the
     * result will be NA.
     */
    @RBuiltin(name = ".fastr.context.eval", kind = RBuiltinKind.PRIMITIVE, parameterNames = {"contexts", "exprs", "par"})
    public abstract static class Eval extends RBuiltinNode {
        @Specialization
        @TruffleBoundary
        protected Object eval(RAbstractIntVector contexts, RAbstractStringVector exprs, byte par) {
            controlVisibility();
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

        @SuppressWarnings("unused")
        @Fallback
        protected RNull eval(Object contexts, Object exprs, Object par) {
            throw RError.error(this, RError.Message.INVALID_OR_UNIMPLEMENTED_ARGUMENTS);
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

    @RBuiltin(name = ".fastr.channel.create", kind = RBuiltinKind.PRIMITIVE, parameterNames = {"key"})
    public abstract static class CreateChannel extends RBuiltinNode {
        @Specialization(guards = "key.getLength() == 1")
        @TruffleBoundary
        protected int createChannel(RAbstractIntVector key) {
            controlVisibility();
            return RChannel.createChannel(key.getDataAt(0));
        }

        @Fallback
        protected int error(Object key) {
            return wrongChannelArg(this, key, "key");
        }
    }

    @RBuiltin(name = ".fastr.channel.get", kind = RBuiltinKind.PRIMITIVE, parameterNames = {"key"})
    public abstract static class GetChannel extends RBuiltinNode {
        @Specialization(guards = "key.getLength() == 1")
        @TruffleBoundary
        protected int getChannel(RAbstractIntVector key) {
            controlVisibility();
            return RChannel.getChannel(key.getDataAt(0));
        }

        @Fallback
        protected int error(Object key) {
            return wrongChannelArg(this, key, "key");
        }
    }

    @RBuiltin(name = ".fastr.channel.close", visibility = RVisibility.OFF, kind = RBuiltinKind.PRIMITIVE, parameterNames = {"id"})
    public abstract static class CloseChannel extends RBuiltinNode {
        @Specialization(guards = "id.getLength() == 1")
        @TruffleBoundary
        protected RNull getChannel(RAbstractIntVector id) {
            controlVisibility();
            RChannel.closeChannel(id.getDataAt(0));
            return RNull.instance;
        }

        @Fallback
        protected int error(Object id) {
            return wrongChannelArg(this, id, "id");
        }
    }

    @RBuiltin(name = ".fastr.channel.send", visibility = RVisibility.OFF, kind = RBuiltinKind.PRIMITIVE, parameterNames = {"id", "data"})
    public abstract static class ChannelSend extends RBuiltinNode {
        @Specialization(guards = "id.getLength() == 1")
        @TruffleBoundary
        protected RNull send(RAbstractIntVector id, Object data) {
            controlVisibility();
            RChannel.send(id.getDataAt(0), data);
            return RNull.instance;
        }

        @Fallback
        protected int error(Object id, @SuppressWarnings("unused") Object data) {
            return wrongChannelArg(this, id, "id");
        }
    }

    @RBuiltin(name = ".fastr.channel.receive", kind = RBuiltinKind.PRIMITIVE, parameterNames = {"id"})
    public abstract static class ChannelReceive extends RBuiltinNode {
        @Specialization(guards = "id.getLength() == 1")
        @TruffleBoundary
        protected Object receive(RAbstractIntVector id) {
            controlVisibility();
            return RChannel.receive(id.getDataAt(0));
        }

        @Fallback
        protected int error(Object id) {
            return wrongChannelArg(this, id, "id");
        }
    }

    @RBuiltin(name = ".fastr.channel.poll", kind = RBuiltinKind.PRIMITIVE, parameterNames = {"id"})
    public abstract static class ChannelPoll extends RBuiltinNode {
        @Specialization(guards = "id.getLength() == 1")
        @TruffleBoundary
        protected Object poll(RAbstractIntVector id) {
            controlVisibility();
            return RChannel.poll(id.getDataAt(0));
        }

        @Fallback
        protected int error(Object id) {
            return wrongChannelArg(this, id, "id");
        }
    }

    @RBuiltin(name = ".fastr.channel.select", kind = RBuiltinKind.PRIMITIVE, parameterNames = {"ids"})
    public abstract static class ChannelSelect extends RBuiltinNode {
        @Specialization
        @TruffleBoundary
        protected RList select(RList nodes) {
            controlVisibility();
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
