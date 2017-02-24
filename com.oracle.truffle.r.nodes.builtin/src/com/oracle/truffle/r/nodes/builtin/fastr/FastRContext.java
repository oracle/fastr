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
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.gt;
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
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.vm.PolyglotEngine;
import com.oracle.truffle.r.nodes.builtin.NodeWithArgumentCasts.Casts;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RChannel;
import com.oracle.truffle.r.runtime.RCmdOptions.Client;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RSource;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.context.ContextInfo;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;

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
                            equalTo(RContext.ContextKind.SHARE_NOTHING.name()).or(equalTo(RContext.ContextKind.SHARE_PARENT_RW.name()).or(equalTo(RContext.ContextKind.SHARE_PARENT_RO.name()))));
        }

        private static void pc(Casts casts) {
            casts.arg("pc").asIntegerVector().findFirst().mustNotBeNA().mustBe(gt(0));
        }

        private static void key(Casts casts) {
            casts.arg("key").asIntegerVector().mustBe(notEmpty()).findFirst();
        }

        private static void id(Casts casts) {
            casts.arg("id").asIntegerVector().mustBe(notEmpty()).findFirst();
        }
    }

    @RBuiltin(name = ".fastr.context.get", kind = PRIMITIVE, parameterNames = {}, behavior = READS_STATE)
    public abstract static class Get extends RBuiltinNode {
        @Specialization
        @TruffleBoundary
        protected Object get() {
            return RContext.getInstance();
        }
    }

    /**
     * Similar to {@code .fastr.context.eval} but the invoking thread does not wait for completion,
     * which is done by {@code .fastr.context.join}. The result is a vector that should be passed to
     * {@code .fastr.context.join}.
     *
     */
    @RBuiltin(name = ".fastr.context.spawn", kind = PRIMITIVE, parameterNames = {"exprs", "pc", "kind"}, behavior = COMPLEX)
    public abstract static class Spawn extends RBuiltinNode {
        @Override
        public Object[] getDefaultParameterValues() {
            return new Object[]{RMissing.instance, 1, "SHARE_NOTHING"};
        }

        static {
            Casts casts = new Casts(Spawn.class);
            CastsHelper.exprs(casts);
            CastsHelper.pc(casts);
            CastsHelper.kind(casts);
        }

        @Specialization
        @TruffleBoundary
        protected RIntVector spawn(RAbstractStringVector exprs, int pc, String kind) {
            RContext.ContextKind contextKind = RContext.ContextKind.valueOf(kind);
            RContext.EvalThread[] threads = new RContext.EvalThread[pc];
            int[] data = new int[pc];
            for (int i = 0; i < pc; i++) {
                ContextInfo info = createContextInfo(contextKind);
                threads[i] = new RContext.EvalThread(info, RSource.fromTextInternal(exprs.getDataAt(i % exprs.getLength()), RSource.Internal.CONTEXT_EVAL));
                data[i] = info.getId();
            }
            for (int i = 0; i < pc; i++) {
                threads[i].start();
            }
            return RDataFactory.createIntVector(data, RDataFactory.COMPLETE_VECTOR);
        }
    }

    @RBuiltin(name = ".fastr.context.join", visibility = OFF, kind = PRIMITIVE, parameterNames = {"handle"}, behavior = COMPLEX)
    public abstract static class Join extends RBuiltinNode {

        static {
            Casts casts = new Casts(Join.class);
            casts.arg("handle").asIntegerVector().mustBe(notEmpty());
        }

        @Specialization
        @TruffleBoundary
        protected RNull eval(RAbstractIntVector handle) {
            try {
                for (int i = 0; i < handle.getLength(); i++) {
                    Thread thread = RContext.EvalThread.threads.get(handle.getDataAt(i));
                    if (thread == null) {
                        // already done
                        continue;
                    } else {
                        thread.join();
                    }
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
    @RBuiltin(name = ".fastr.context.eval", kind = PRIMITIVE, parameterNames = {"exprs", "pc", "kind"}, behavior = COMPLEX)
    public abstract static class Eval extends RBuiltinNode {
        @Override
        public Object[] getDefaultParameterValues() {
            return new Object[]{RMissing.instance, 1, "SHARE_NOTHING"};
        }

        static {
            Casts casts = new Casts(Eval.class);
            CastsHelper.exprs(casts);
            CastsHelper.pc(casts);
            CastsHelper.kind(casts);
        }

        @Specialization
        @TruffleBoundary
        protected Object eval(RAbstractStringVector exprs, int pc, String kind) {
            RContext.ContextKind contextKind = RContext.ContextKind.valueOf(kind);

            Object[] results = new Object[pc];
            if (pc == 1) {
                ContextInfo info = createContextInfo(contextKind);
                PolyglotEngine vm = info.createVM();
                try {
                    results[0] = RContext.EvalThread.run(vm, info, RSource.fromTextInternal(exprs.getDataAt(0), RSource.Internal.CONTEXT_EVAL));
                } finally {
                    vm.dispose();
                }
            } else {
                // separate threads that run in parallel; invoking thread waits for completion
                RContext.EvalThread[] threads = new RContext.EvalThread[pc];
                for (int i = 0; i < pc; i++) {
                    ContextInfo info = createContextInfo(contextKind);
                    threads[i] = new RContext.EvalThread(info, RSource.fromTextInternal(exprs.getDataAt(i % exprs.getLength()), RSource.Internal.CONTEXT_EVAL));
                }
                for (int i = 0; i < pc; i++) {
                    threads[i].start();
                }
                try {
                    for (int i = 0; i < pc; i++) {
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
    public abstract static class R extends RBuiltinNode {
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

        @Specialization
        @TruffleBoundary
        protected Object r(RAbstractStringVector args, RAbstractStringVector env, boolean intern) {
            Object rc = RContext.getRRuntimeASTAccess().rcommandMain(args.materialize().getDataCopy(), env.materialize().getDataCopy(), intern);
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
    public abstract static class Rscript extends RBuiltinNode {

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
            return RContext.getRRuntimeASTAccess().rscriptMain(args.materialize().getDataCopy(), env.materialize().getDataCopy(), intern);
        }

        @Specialization
        @TruffleBoundary
        protected Object rscript(RAbstractStringVector args, @SuppressWarnings("unused") RMissing env, boolean intern) {
            return rscript(args, RDataFactory.createEmptyStringVector(), intern);
        }
    }

    private static ContextInfo createContextInfo(RContext.ContextKind contextKind) {
        return ContextInfo.createNoRestore(Client.RSCRIPT, null, contextKind, RContext.getInstance(), RContext.getInstance().getConsoleHandler());
    }

    @RBuiltin(name = ".fastr.channel.create", kind = PRIMITIVE, parameterNames = {"key"}, behavior = COMPLEX)
    public abstract static class CreateChannel extends RBuiltinNode {

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
    public abstract static class GetChannel extends RBuiltinNode {

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
    public abstract static class CloseChannel extends RBuiltinNode {

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
    public abstract static class ChannelSend extends RBuiltinNode {

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
    public abstract static class ChannelReceive extends RBuiltinNode {

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
    public abstract static class ChannelPoll extends RBuiltinNode {

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
    public abstract static class ChannelSelect extends RBuiltinNode {

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
