/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.equalTo;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.instanceOf;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.notEmpty;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.singleElement;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;
import static com.oracle.truffle.r.runtime.RVisibility.OFF;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.COMPLEX;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.READS_STATE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleContext;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.java.JavaInterop;
import com.oracle.truffle.api.vm.PolyglotEngine;
import com.oracle.truffle.r.launcher.RCmdOptions.Client;
import com.oracle.truffle.r.nodes.builtin.NodeWithArgumentCasts.Casts;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.FastROptions;
import com.oracle.truffle.r.runtime.RChannel;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RSource;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.context.ChildContextInfo;
import com.oracle.truffle.r.runtime.context.EvalThread;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.RContext.ConsoleIO;
import com.oracle.truffle.r.runtime.context.RContext.ContextKind;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
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
    public abstract static class Get extends RBuiltinNode.Arg0 {
        @Specialization
        @TruffleBoundary
        protected TruffleObject get() {
            return JavaInterop.asTruffleObject(RContext.getInstance());
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
            return new Object[]{RMissing.instance, FastROptions.SharedContexts.getBooleanValue() ? "SHARE_ALL" : "SHARE_NOTHING"};
        }

        static {
            Casts casts = new Casts(Spawn.class);
            CastsHelper.exprs(casts);
            CastsHelper.kind(casts);
        }

        @Specialization
        @TruffleBoundary
        protected RIntVector spawn(RAbstractStringVector exprs, String kind) {
            RContext.ContextKind contextKind = RContext.ContextKind.valueOf(kind);
            if (FastROptions.SharedContexts.getBooleanValue() && contextKind != ContextKind.SHARE_ALL) {
                throw RError.error(RError.NO_CALLER, RError.Message.GENERIC, "Only shared contexts are allowed");
            }
            handleSharedContexts(contextKind);

            int length = exprs.getLength();
            EvalThread[] threads = new EvalThread[length];
            int[] data = new int[length];
            int[] multiSlotIndices = new int[length];
            for (int i = 0; i < length; i++) {
                ChildContextInfo info = createContextInfo(contextKind);
                if (FastROptions.SpawnUsesPloyglot.getBooleanValue()) {
                    threads[i] = new EvalThread(info, RSource.fromTextInternalInvisible(exprs.getDataAt(i % exprs.getLength()), RSource.Internal.CONTEXT_EVAL), true);
                } else {
                    threads[i] = new EvalThread(info, RSource.fromTextInternalInvisible(exprs.getDataAt(i % exprs.getLength()), RSource.Internal.CONTEXT_EVAL));
                }
                data[i] = info.getId();
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
        protected RNull eval(RAbstractIntVector handle) {
            try {
                int[] multiSlotIndices = new int[handle.getLength()];
                for (int i = 0; i < handle.getLength(); i++) {
                    int id = handle.getDataAt(i);
                    Thread thread = EvalThread.threads.get(id);
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

    /**
     * Evaluate expressions in {@code pc} new contexts of type {@code kind}, with the expression
     * taken from the expression in the usual R repeating mode. The invoking context (thread) waits
     * for completion of all the sub-contexts. {@code args} provides the command line arguments to
     * the contexts - this is the same for all.
     *
     * Each evaluation is run in a new {@link RContext}/{@link PolyglotEngine}. The result is a list
     * of lists. The top level list has the same number of entries as the number of contexts. The
     * sublist contains the result of the evaluation with name "result". It may also have an
     * attribute "error" if the evaluation threw an exception, in which case the result will be NA.
     */
    @RBuiltin(name = ".fastr.context.eval", kind = PRIMITIVE, parameterNames = {"exprs", "kind"}, behavior = COMPLEX)
    public abstract static class Eval extends RBuiltinNode.Arg2 {
        @Override
        public Object[] getDefaultParameterValues() {
            return new Object[]{RMissing.instance, FastROptions.SharedContexts.getBooleanValue() ? "SHARE_ALL" : "SHARE_NOTHING"};
        }

        static {
            Casts casts = new Casts(Eval.class);
            CastsHelper.exprs(casts);
            CastsHelper.kind(casts);
        }

        @Specialization
        @TruffleBoundary
        protected Object eval(RAbstractStringVector exprs, String kind) {
            RContext.ContextKind contextKind = RContext.ContextKind.valueOf(kind);
            if (FastROptions.SharedContexts.getBooleanValue() && contextKind != ContextKind.SHARE_ALL) {
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
                    threads[i] = new EvalThread(info, RSource.fromTextInternalInvisible(exprs.getDataAt(i % exprs.getLength()), RSource.Internal.CONTEXT_EVAL));
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

    @RBuiltin(name = ".fastr.context.r", kind = PRIMITIVE, visibility = OFF, parameterNames = {"args", "env", "intern"}, behavior = COMPLEX)
    public abstract static class R extends RBuiltinNode.Arg3 {
        @Override
        public Object[] getDefaultParameterValues() {
            return new Object[]{RMissing.instance, RMissing.instance, RRuntime.LOGICAL_FALSE};
        }

        static {
            Casts casts = new Casts(R.class);
            casts.arg("args").allowMissing().mustBe(stringValue());
            casts.arg("env").allowMissing().mustBe(stringValue());
            casts.arg("intern").asLogicalVector().findFirst().map(toBoolean());
        }

        public abstract Object execute(VirtualFrame frame, RAbstractStringVector args, RAbstractStringVector env, boolean intern);

        @Specialization
        @TruffleBoundary
        protected Object r(RAbstractStringVector args, RAbstractStringVector env, boolean intern) {
            Object rc = RContext.getRRuntimeASTAccess().rcommandMain(prependCommand(args, "R"), env.materialize().getDataCopy(), intern);
            return rc;
        }

        @Specialization
        protected Object r(@SuppressWarnings("unused") RMissing arg, @SuppressWarnings("unused") RMissing env, boolean intern) {
            return r(RDataFactory.createEmptyStringVector(), RDataFactory.createEmptyStringVector(), intern);
        }

        @Specialization
        @TruffleBoundary
        protected Object r(@SuppressWarnings("unused") RMissing args, RAbstractStringVector env, boolean intern) {
            return r(RDataFactory.createEmptyStringVector(), env, intern);
        }
    }

    @RBuiltin(name = ".fastr.context.rscript", kind = PRIMITIVE, visibility = OFF, parameterNames = {"args", "env", "intern"}, behavior = COMPLEX)
    public abstract static class Rscript extends RBuiltinNode.Arg3 {

        public abstract Object execute(RAbstractStringVector args, RAbstractStringVector env, boolean intern);

        @Override
        public Object[] getDefaultParameterValues() {
            return new Object[]{RMissing.instance, RMissing.instance, RRuntime.LOGICAL_FALSE};
        }

        static {
            Casts casts = new Casts(Rscript.class);
            casts.arg("args").mustBe(stringValue(), RError.Message.GENERIC, "usage: /path/to/Rscript [--options] [-e expr [-e expr2 ...] | file] [args]").asStringVector();
            casts.arg("env").allowMissing().mustBe(stringValue());
            casts.arg("intern").asLogicalVector().findFirst().map(toBoolean());
        }

        @Specialization
        @TruffleBoundary
        protected Object rscript(RAbstractStringVector args, RAbstractStringVector env, boolean intern) {
            return RContext.getRRuntimeASTAccess().rscriptMain(prependCommand(args, "Rscript"), env.materialize().getDataCopy(), intern);
        }

        @Specialization
        @TruffleBoundary
        protected Object rscript(RAbstractStringVector args, @SuppressWarnings("unused") RMissing env, boolean intern) {
            return rscript(args, RDataFactory.createEmptyStringVector(), intern);
        }
    }

    private static String[] prependCommand(RAbstractStringVector argsVec, String command) {
        String[] argsVecArgs = argsVec.materialize().getDataCopy();
        String[] result = new String[argsVecArgs.length + 1];
        result[0] = command;
        System.arraycopy(argsVecArgs, 0, result, 1, argsVecArgs.length);
        return result;
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
