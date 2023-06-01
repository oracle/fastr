/*
 * Copyright (c) 2015, 2023, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.fastr;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.equalTo;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.instanceOf;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.notEmpty;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.singleElement;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.runtime.RVisibility.OFF;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.COMPLEX;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.READS_STATE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;
import static com.oracle.truffle.r.runtime.context.FastROptions.SharedContexts;

import org.graalvm.polyglot.Context;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleContext;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.r.common.RCmdOptions.Client;
import com.oracle.truffle.r.nodes.builtin.NodeWithArgumentCasts.Casts;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RChannel;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RSource;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.context.ChildContextInfo;
import com.oracle.truffle.r.runtime.context.EvalThread;
import com.oracle.truffle.r.runtime.context.FastROptions;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.RContext.ConsoleIO;
import com.oracle.truffle.r.runtime.context.RContext.ContextKind;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.env.REnvironment;

/**
 * The FastR builtins that allow multiple "virtual" R sessions potentially executing in parallel.
 */
public class FastRContext {

    private static final class CastsHelper {
        private static void exprs(Casts casts) {
            casts.arg("exprs").asStringVector().mustBe(notEmpty());
        }

        private static void kind(Casts casts) {
            casts.arg("kind").mustBe(stringValue()).asStringVector().mustBe(singleElement()).findFirst().mustNotBeNA().mustBe(
                            equalTo(RContext.ContextKind.SHARE_NOTHING.name()).or(equalTo(RContext.ContextKind.SHARE_PARENT_RW.name()).or(
                                            equalTo(RContext.ContextKind.SHARE_PARENT_RO.name()).or(equalTo(RContext.ContextKind.SHARE_ALL.name())))));
        }

        private static void key(Casts casts) {
            casts.arg("key").asIntegerVector().mustBe(notEmpty()).findFirst();
        }

        private static void id(Casts casts) {
            casts.arg("id").asIntegerVector().mustBe(notEmpty()).findFirst();
        }
    }

    @RBuiltin(name = ".fastr.context.get", kind = PRIMITIVE, parameterNames = {}, behavior = READS_STATE)
    public static final class Get extends RBuiltinNode.Arg0 {
        @Override
        public Object execute(@SuppressWarnings("unused") VirtualFrame frame) {
            return getContext();
        }

        @TruffleBoundary
        private static Object getContext() {
            RContext context = RContext.getInstance();
            return context.getEnv().asGuestValue(context);
        }
    }

    /**
     * Creates a new internal context for the unit tests. Intended to be used only for unit tests.
     */
    @RBuiltin(name = ".fastr.context.testing.new", kind = PRIMITIVE, parameterNames = {"info"}, behavior = READS_STATE)
    public static final class FastRContextNew extends RBuiltinNode.Arg1 {
        static {
            Casts.noCasts(FastRContextNew.class);
        }

        @Override
        public Object execute(@SuppressWarnings("unused") VirtualFrame frame, Object info) {
            return createNewContext(info);
        }

        @TruffleBoundary
        private static Object createNewContext(Object info) {
            RContext context = RContext.getInstance();
            ChildContextInfo ctxInfo = (ChildContextInfo) context.getEnv().asHostObject(info);
            TruffleContext truffleCtx = ctxInfo.createTruffleContext();
            Object prev = truffleCtx.enter(null);
            Context hostContext = Context.getCurrent();
            truffleCtx.leave(null, prev);
            return context.getEnv().asGuestValue(new ContextData(truffleCtx, hostContext));
        }
    }

    /**
     * Closes an internal context used for the unit test evaluation. Intended to be used only for
     * unit tests.
     */
    @RBuiltin(name = ".fastr.context.testing.close", kind = PRIMITIVE, parameterNames = {"info"}, behavior = READS_STATE)
    public static final class FastRContextClose extends RBuiltinNode.Arg1 {
        static {
            Casts.noCasts(FastRContextClose.class);
        }

        @Override
        public Object execute(VirtualFrame frame, Object info) {
            closeContext(info);
            return RNull.instance;
        }

        @TruffleBoundary
        private static void closeContext(Object info) {
            RContext context = RContext.getInstance();
            ContextData data = (ContextData) context.getEnv().asHostObject(info);
            data.truffleContext.close();
        }
    }

    public static final class ContextData {
        public final TruffleContext truffleContext;
        public final Context context;

        public ContextData(TruffleContext truffleContext, Context context) {
            this.truffleContext = truffleContext;
            this.context = context;
        }
    }

    private static void handleSharedContexts(ContextKind contextKind) {
        if (contextKind == ContextKind.SHARE_ALL && EvalThread.threadCnt.get() == 0) {
            RContext current = RContext.getInstance();
            if (EvalThread.threadCnt.get() == 0 && (current.isInitial() || current.getKind() == ContextKind.SHARE_PARENT_RW)) {
                ChildContextInfo.resetMultiSlotIndexGenerator();
            } else {
                throw RError.error(RError.NO_CALLER, RError.Message.GENERIC, "Shared contexts can be created only if no other child contexts exist");
            }
        }
    }

    /**
     * Similar to {@code .fastr.context.eval} but the invoking thread does not wait for completion,
     * which is done by {@code .fastr.context.join}. The result is a vector that should be passed to
     * {@code .fastr.context.join}.
     *
     */
    @RBuiltin(name = ".fastr.context.spawn", kind = PRIMITIVE, parameterNames = {"exprs", "kind"}, behavior = COMPLEX)
    public abstract static class Spawn extends RBuiltinNode.Arg2 {
        @Override
        public Object[] getDefaultParameterValues() {
            return new Object[]{RMissing.instance, FastROptions.sharedContextsOptionValue ? "SHARE_ALL" : "SHARE_NOTHING"};
        }

        static {
            Casts casts = new Casts(Spawn.class);
            CastsHelper.exprs(casts);
            CastsHelper.kind(casts);
        }

        @Specialization
        @TruffleBoundary
        protected RIntVector spawn(RStringVector exprs, String kind) {
            RContext.ContextKind contextKind = RContext.ContextKind.valueOf(kind);
            if (getRContext().getOption(SharedContexts) && contextKind != ContextKind.SHARE_ALL) {
                throw RError.error(RError.NO_CALLER, RError.Message.GENERIC, "Only shared contexts are allowed");
            }
            handleSharedContexts(contextKind);

            int length = exprs.getLength();
            EvalThread[] threads = new EvalThread[length];
            int[] data = new int[length];
            int[] multiSlotIndices = new int[length];

            // first, create context infos
            ChildContextInfo[] childContextInfos = new ChildContextInfo[length];
            for (int i = 0; i < length; i++) {
                childContextInfos[i] = createContextInfo(contextKind);
                data[i] = childContextInfos[i].getId();
                multiSlotIndices[i] = childContextInfos[i].getMultiSlotInd();
            }

            // convert shared slots to multi slots
            if (contextKind == ContextKind.SHARE_ALL) {
                REnvironment.convertSearchpathToMultiSlot(multiSlotIndices);
            }

            // create eval threads which may already set values to shared slots
            for (int i = 0; i < length; i++) {
                threads[i] = new EvalThread(getRContext().threads, childContextInfos[i],
                                RSource.fromTextInternalInvisible(exprs.getDataAt(i % exprs.getLength()), RSource.Internal.CONTEXT_EVAL));
            }
            for (int i = 0; i < length; i++) {
                threads[i].start();
            }
            for (int i = 0; i < length; i++) {
                threads[i].waitForInit();
            }
            return RDataFactory.createIntVector(data, RDataFactory.COMPLETE_VECTOR);
        }
    }

    @RBuiltin(name = ".fastr.context.join", visibility = OFF, kind = PRIMITIVE, parameterNames = {"handle"}, behavior = COMPLEX)
    public abstract static class Join extends RBuiltinNode.Arg1 {

        static {
            Casts casts = new Casts(Join.class);
            casts.arg("handle").asIntegerVector().mustBe(notEmpty());
        }

        @Specialization
        @TruffleBoundary
        protected RNull eval(RIntVector handle) {
            try {
                int[] multiSlotIndices = new int[handle.getLength()];
                for (int i = 0; i < handle.getLength(); i++) {
                    int id = handle.getDataAt(i);
                    Thread thread = getRContext().threads.get(id);
                    if (EvalThread.idToMultiSlotTable.containsKey(id)) {
                        multiSlotIndices[i] = EvalThread.idToMultiSlotTable.remove(id);
                    }
                    if (thread == null) {
                        // already done
                        continue;
                    } else {
                        thread.join();
                    }
                }
                // If all eval threads died, completely remove multi slot data.
                if (EvalThread.threadCnt.get() == 0) {
                    REnvironment.cleanupSearchpathFromMultiSlot();
                } else {
                    REnvironment.cleanupSearchpathFromMultiSlot(multiSlotIndices);
                }
            } catch (InterruptedException ex) {
                throw error(RError.Message.GENERIC, "error finishing eval thread");

            }
            return RNull.instance;
        }
    }

    @RBuiltin(name = ".fastr.context.interrupt", visibility = OFF, kind = PRIMITIVE, parameterNames = {"handle"}, behavior = COMPLEX)
    public abstract static class Interrupt extends RBuiltinNode.Arg1 {

        static {
            Casts casts = new Casts(Interrupt.class);
            casts.arg("handle").asIntegerVector().mustBe(notEmpty());
        }

        @Specialization
        @TruffleBoundary
        protected RNull eval(RIntVector handle) {
            for (int i = 0; i < handle.getLength(); i++) {
                int id = handle.getDataAt(i);
                Thread thread = getRContext().threads.get(id);
                if (thread == null) {
                    // already done
                    continue;
                } else {
                    thread.interrupt();
                }
            }
            return RNull.instance;
        }
    }

    /**
     * Evaluate expressions in {@code pc} new contexts of type {@code kind}, with the expression
     * taken from the expression in the usual R repeating mode. The invoking context (thread) waits
     * for completion of all the sub-contexts. {@code args} provides the command line arguments to
     * the contexts - this is the same for all.
     *
     * Each evaluation is run in a new {@link RContext}. The result is a list of lists. The top
     * level list has the same number of entries as the number of contexts. The sublist contains the
     * result of the evaluation with name "result". It may also have an attribute "error" if the
     * evaluation threw an exception, in which case the result will be NA.
     */
    @RBuiltin(name = ".fastr.context.eval", kind = PRIMITIVE, parameterNames = {"exprs", "kind"}, behavior = COMPLEX)
    public abstract static class Eval extends RBuiltinNode.Arg2 {
        @Override
        public Object[] getDefaultParameterValues() {
            return new Object[]{RMissing.instance, getRContext().getOption(SharedContexts) ? "SHARE_ALL" : "SHARE_NOTHING"};
        }

        static {
            Casts casts = new Casts(Eval.class);
            CastsHelper.exprs(casts);
            CastsHelper.kind(casts);
        }

        @Specialization
        @TruffleBoundary
        protected Object eval(RStringVector exprs, String kind) {
            RContext.ContextKind contextKind = RContext.ContextKind.valueOf(kind);
            if (getRContext().getOption(SharedContexts) && contextKind != ContextKind.SHARE_ALL) {
                throw RError.error(RError.NO_CALLER, RError.Message.GENERIC, "Only shared contexts are allowed");
            }
            handleSharedContexts(contextKind);

            int length = exprs.getLength();
            Object[] results = new Object[length];
            if (length == 1) {
                ChildContextInfo info = createContextInfo(contextKind);
                TruffleContext truffleContext = info.createTruffleContext();
                results[0] = EvalThread.run(truffleContext, info, RSource.fromTextInternalInvisible(exprs.getDataAt(0), RSource.Internal.CONTEXT_EVAL));
            } else {
                // separate threads that run in parallel; invoking thread waits for completion
                EvalThread[] threads = new EvalThread[length];
                int[] multiSlotIndices = new int[length];
                for (int i = 0; i < length; i++) {
                    ChildContextInfo info = createContextInfo(contextKind);
                    threads[i] = new EvalThread(getRContext().threads, info, RSource.fromTextInternalInvisible(exprs.getDataAt(i % exprs.getLength()), RSource.Internal.CONTEXT_EVAL));
                    multiSlotIndices[i] = info.getMultiSlotInd();
                }
                if (contextKind == ContextKind.SHARE_ALL) {
                    REnvironment.convertSearchpathToMultiSlot(multiSlotIndices);
                }
                for (int i = 0; i < length; i++) {
                    threads[i].start();
                }
                for (int i = 0; i < length; i++) {
                    threads[i].waitForInit();
                }
                try {
                    for (int i = 0; i < length; i++) {
                        threads[i].join();
                        results[i] = threads[i].getEvalResult();
                    }
                } catch (InterruptedException ex) {
                    throw error(RError.Message.GENERIC, "error finishing eval thread");
                }
            }
            return RDataFactory.createList(results);
        }
    }

    private static ChildContextInfo createContextInfo(RContext.ContextKind contextKind) {
        RContext context = RContext.getInstance();
        ConsoleIO console = context.getConsole();
        return ChildContextInfo.createNoRestore(Client.RSCRIPT, null, contextKind, context, console.getStdin(), console.getStdout(), console.getStderr());
    }

    @RBuiltin(name = ".fastr.channel.create", kind = PRIMITIVE, parameterNames = {"key"}, behavior = COMPLEX)
    public abstract static class CreateChannel extends RBuiltinNode.Arg1 {

        static {
            Casts casts = new Casts(CreateChannel.class);
            CastsHelper.key(casts);
        }

        @Specialization
        @TruffleBoundary
        protected int createChannel(int key) {
            return RChannel.createChannel(key);
        }
    }

    @RBuiltin(name = ".fastr.channel.createForkChannel", kind = PRIMITIVE, parameterNames = {"portBaseNumber"}, behavior = COMPLEX)
    public abstract static class CreateForkChannel extends RBuiltinNode.Arg1 {

        static {
            Casts casts = new Casts(CreateForkChannel.class);
            casts.arg("portBaseNumber").asIntegerVector().mustBe(notEmpty()).findFirst();
        }

        @Specialization
        @TruffleBoundary
        protected RAbstractListVector createForkChannel(int portBaseNumber) {
            int[] res = RChannel.createForkChannel(portBaseNumber);
            return RDataFactory.createList(new Object[]{res[0], res[1]}, RDataFactory.createStringVector(new String[]{"channelId", "port"}, true));
        }
    }

    @RBuiltin(name = ".fastr.channel.get", kind = PRIMITIVE, parameterNames = {"key"}, behavior = COMPLEX)
    public abstract static class GetChannel extends RBuiltinNode.Arg1 {

        static {
            Casts casts = new Casts(GetChannel.class);
            CastsHelper.key(casts);
        }

        @Specialization
        @TruffleBoundary
        protected int getChannel(int key) {
            return RChannel.getChannel(key);
        }
    }

    @RBuiltin(name = ".fastr.channel.close", visibility = OFF, kind = PRIMITIVE, parameterNames = {"id"}, behavior = COMPLEX)
    public abstract static class CloseChannel extends RBuiltinNode.Arg1 {

        static {
            Casts casts = new Casts(CloseChannel.class);
            CastsHelper.id(casts);
        }

        @Specialization
        @TruffleBoundary
        protected RNull getChannel(int id) {
            RChannel.closeChannel(id);
            return RNull.instance;
        }
    }

    @RBuiltin(name = ".fastr.channel.send", visibility = OFF, kind = PRIMITIVE, parameterNames = {"id", "data"}, behavior = COMPLEX)
    public abstract static class ChannelSend extends RBuiltinNode.Arg2 {

        static {
            Casts casts = new Casts(ChannelSend.class);
            CastsHelper.id(casts);
        }

        @Specialization
        @TruffleBoundary
        protected RNull send(int id, Object data) {
            RChannel.send(id, data);
            return RNull.instance;
        }
    }

    @RBuiltin(name = ".fastr.channel.receive", kind = PRIMITIVE, parameterNames = {"id"}, behavior = COMPLEX)
    public abstract static class ChannelReceive extends RBuiltinNode.Arg1 {

        static {
            Casts casts = new Casts(ChannelReceive.class);
            CastsHelper.id(casts);
        }

        @Specialization
        @TruffleBoundary
        protected Object receive(int id) {
            return RChannel.receive(id);
        }
    }

    @RBuiltin(name = ".fastr.channel.poll", kind = PRIMITIVE, parameterNames = {"id"}, behavior = COMPLEX)
    public abstract static class ChannelPoll extends RBuiltinNode.Arg1 {

        static {
            Casts casts = new Casts(ChannelPoll.class);
            CastsHelper.id(casts);
        }

        @Specialization
        @TruffleBoundary
        protected Object poll(int id) {
            return RChannel.poll(id);
        }
    }

    @RBuiltin(name = ".fastr.channel.select", kind = PRIMITIVE, parameterNames = {"ids"}, behavior = COMPLEX)
    public abstract static class ChannelSelect extends RBuiltinNode.Arg1 {

        static {
            Casts casts = new Casts(ChannelSelect.class);
            casts.arg("ids").mustBe(instanceOf(RList.class));
        }

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
