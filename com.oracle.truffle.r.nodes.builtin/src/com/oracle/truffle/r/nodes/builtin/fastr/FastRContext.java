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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.*;
import static com.oracle.truffle.r.runtime.RVisibility.OFF;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.COMPLEX;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.READS_STATE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.vm.PolyglotEngine;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RChannel;
import com.oracle.truffle.r.runtime.RCmdOptions;
import com.oracle.truffle.r.runtime.RCmdOptions.Client;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RSource;
import com.oracle.truffle.r.runtime.RStartParams;
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

    private abstract static class CastHelper extends RBuiltinNode {
        protected void exprs(CastBuilder casts) {
            casts.arg("exprs").asStringVector().mustBe(nullValue().not().and(notEmpty()));
        }

        protected void kind(CastBuilder casts) {
            casts.arg("kind").mustBe(stringValue()).asStringVector().mustBe(singleElement()).findFirst().notNA().mustBe(
                            equalTo(RContext.ContextKind.SHARE_NOTHING.name()).or(equalTo(RContext.ContextKind.SHARE_PARENT_RW.name()).or(equalTo(RContext.ContextKind.SHARE_PARENT_RO.name()))));
        }

        protected void args(CastBuilder casts) {
            casts.arg("args").asStringVector().mustBe(nullValue().not().and(notEmpty()));
        }

        protected void pc(CastBuilder casts) {
            casts.arg("pc").asIntegerVector().findFirst().notNA().mustBe(gt(0));
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
    @RBuiltin(name = ".fastr.context.spawn", kind = PRIMITIVE, parameterNames = {"exprs", "pc", "kind", "args"}, behavior = COMPLEX)
    public abstract static class Spawn extends CastHelper {
        @Override
        public Object[] getDefaultParameterValues() {
            return new Object[]{RMissing.instance, 1, "SHARE_NOTHING", ""};
        }

        @Override
        protected void createCasts(CastBuilder casts) {
            exprs(casts);
            pc(casts);
            kind(casts);
            args(casts);
        }

        @Specialization
        @TruffleBoundary
        protected RIntVector spawn(RAbstractStringVector exprs, int pc, String kind, RAbstractStringVector args) {
            RContext.ContextKind contextKind = RContext.ContextKind.valueOf(kind);
            RContext.EvalThread[] threads = new RContext.EvalThread[pc];
            int[] data = new int[pc];
            for (int i = 0; i < pc; i++) {
                ContextInfo info = createContextInfo(contextKind, args);
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
        @Override
        protected void createCasts(CastBuilder casts) {
            casts.arg("handle").asIntegerVector().mustBe(nullValue().not().and(notEmpty()));
        }

        @Specialization
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
                throw RError.error(this, RError.Message.GENERIC, "error finishing eval thread");

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
    @RBuiltin(name = ".fastr.context.eval", kind = PRIMITIVE, parameterNames = {"exprs", "pc", "kind", "args"}, behavior = COMPLEX)
    public abstract static class Eval extends CastHelper {
        @Override
        public Object[] getDefaultParameterValues() {
            return new Object[]{RMissing.instance, 1, "SHARE_NOTHING", ""};
        }

        @Override
        protected void createCasts(CastBuilder casts) {
            exprs(casts);
            pc(casts);
            kind(casts);
            args(casts);
        }

        @Specialization
        @TruffleBoundary
        protected Object eval(RAbstractStringVector exprs, int pc, String kind, RAbstractStringVector args) {
            RContext.ContextKind contextKind = RContext.ContextKind.valueOf(kind);
            Object[] results = new Object[pc];
            // separate threads that run in parallel; invoking thread waits for completion
            RContext.EvalThread[] threads = new RContext.EvalThread[pc];
            for (int i = 0; i < pc; i++) {
                ContextInfo info = createContextInfo(contextKind, args);
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
                throw RError.error(this, RError.Message.GENERIC, "error finishing eval thread");
            }
            return RDataFactory.createList(results);
        }

    }

    private static ContextInfo createContextInfo(RContext.ContextKind contextKind, RAbstractStringVector args) {
        RStartParams startParams = new RStartParams(RCmdOptions.parseArguments(Client.RSCRIPT, args.materialize().getDataCopy(), false), false);
        ContextInfo info = ContextInfo.create(startParams, contextKind, RContext.getInstance(), RContext.getInstance().getConsoleHandler());
        return info;
    }

    private abstract static class ChannelCastAdapter extends RBuiltinNode {
        protected void key(CastBuilder casts) {
            casts.arg("key").asIntegerVector().mustBe(nullValue().not().and(notEmpty())).findFirst();
        }

        protected void id(CastBuilder casts) {
            casts.arg("id").asIntegerVector().mustBe(nullValue().not().and(notEmpty())).findFirst();
        }
    }

    @RBuiltin(name = ".fastr.channel.create", kind = PRIMITIVE, parameterNames = {"key"}, behavior = COMPLEX)
    public abstract static class CreateChannel extends ChannelCastAdapter {

        @Override
        protected void createCasts(CastBuilder casts) {
            key(casts);
        }

        @Specialization
        @TruffleBoundary
        protected int createChannel(int key) {
            return RChannel.createChannel(key);
        }

    }

    @RBuiltin(name = ".fastr.channel.get", kind = PRIMITIVE, parameterNames = {"key"}, behavior = COMPLEX)
    public abstract static class GetChannel extends ChannelCastAdapter {
        @Override
        protected void createCasts(CastBuilder casts) {
            key(casts);
        }

        @Specialization
        @TruffleBoundary
        protected int getChannel(int key) {
            return RChannel.getChannel(key);
        }

    }

    @RBuiltin(name = ".fastr.channel.close", visibility = OFF, kind = PRIMITIVE, parameterNames = {"id"}, behavior = COMPLEX)
    public abstract static class CloseChannel extends ChannelCastAdapter {
        @Override
        protected void createCasts(CastBuilder casts) {
            id(casts);
        }

        @Specialization
        @TruffleBoundary
        protected RNull getChannel(int id) {
            RChannel.closeChannel(id);
            return RNull.instance;
        }

    }

    @RBuiltin(name = ".fastr.channel.send", visibility = OFF, kind = PRIMITIVE, parameterNames = {"id", "data"}, behavior = COMPLEX)
    public abstract static class ChannelSend extends ChannelCastAdapter {
        @Override
        protected void createCasts(CastBuilder casts) {
            id(casts);
        }

        @Specialization
        @TruffleBoundary
        protected RNull send(int id, Object data) {
            RChannel.send(id, data);
            return RNull.instance;
        }

    }

    @RBuiltin(name = ".fastr.channel.receive", kind = PRIMITIVE, parameterNames = {"id"}, behavior = COMPLEX)
    public abstract static class ChannelReceive extends ChannelCastAdapter {
        @Override
        protected void createCasts(CastBuilder casts) {
            id(casts);
        }

        @Specialization
        @TruffleBoundary
        protected Object receive(int id) {
            return RChannel.receive(id);
        }

    }

    @RBuiltin(name = ".fastr.channel.poll", kind = PRIMITIVE, parameterNames = {"id"}, behavior = COMPLEX)
    public abstract static class ChannelPoll extends ChannelCastAdapter {
        @Override
        protected void createCasts(CastBuilder casts) {
            id(casts);
        }

        @Specialization
        @TruffleBoundary
        protected Object poll(int id) {
            return RChannel.poll(id);
        }

    }

    @RBuiltin(name = ".fastr.channel.select", kind = PRIMITIVE, parameterNames = {"ids"}, behavior = COMPLEX)
    public abstract static class ChannelSelect extends RBuiltinNode {
        @Override
        protected void createCasts(CastBuilder casts) {
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
