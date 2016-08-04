/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.RVisibility.OFF;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.COMPLEX;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.helpers.DebugHandling;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RNull;

public class DebugFunctions {

    protected abstract static class ErrorAndFunAdapter extends RBuiltinNode {

        @Override
        protected void createCasts(@SuppressWarnings("unused") CastBuilder casts) {
            casts.arg("fun").mustBe(RFunction.class, Message.ARG_MUST_BE_CLOSURE);
        }

        protected RError arg1Closure() throws RError {
            throw RError.error(this, RError.Message.ARG_MUST_BE_CLOSURE);
        }

        protected void doDebug(RFunction fun, Object text, Object condition, boolean once) throws RError {
            // GnuR does not generate an error for builtins, but debug (obviously) has no effect
            if (!fun.isBuiltin()) {
                if (!DebugHandling.enableDebug(fun, text, condition, once, false)) {
                    throw RError.error(this, RError.Message.GENERIC, "failed to attach debug handler (not instrumented?)");
                }
            }
        }
    }

    @RBuiltin(name = "debug", visibility = OFF, kind = INTERNAL, parameterNames = {"fun", "text", "condition"}, behavior = COMPLEX)
    public abstract static class Debug extends ErrorAndFunAdapter {

        @SuppressWarnings("unused")
        @Fallback
        @TruffleBoundary
        protected Object doDebug(Object fun, Object text, Object condition) {
            throw arg1Closure();
        }

        @Specialization
        @TruffleBoundary
        protected RNull doDebug(RFunction fun, Object text, Object condition) {
            doDebug(fun, text, condition, false);
            return RNull.instance;
        }
    }

    @RBuiltin(name = "debugonce", visibility = OFF, kind = INTERNAL, parameterNames = {"fun", "text", "condition"}, behavior = COMPLEX)
    public abstract static class DebugOnce extends ErrorAndFunAdapter {

        @SuppressWarnings("unused")
        @Fallback
        @TruffleBoundary
        protected Object doDebug(Object fun, Object text, Object condition) {
            throw arg1Closure();
        }

        @Specialization
        @TruffleBoundary
        protected RNull debugonce(RFunction fun, Object text, Object condition) {
            // TODO implement
            doDebug(fun, text, condition, true);
            return RNull.instance;
        }
    }

    @RBuiltin(name = "undebug", visibility = OFF, kind = INTERNAL, parameterNames = {"fun"}, behavior = COMPLEX)
    public abstract static class UnDebug extends ErrorAndFunAdapter {

        @Fallback
        @TruffleBoundary
        protected Object doDebug(@SuppressWarnings("unused") Object fun) {
            throw arg1Closure();
        }

        @Specialization
        @TruffleBoundary
        protected RNull undebug(RFunction func) {
            if (!DebugHandling.undebug(func)) {
                throw RError.error(this, RError.Message.NOT_DEBUGGED);
            }
            return RNull.instance;
        }
    }

    @RBuiltin(name = "isdebugged", kind = INTERNAL, parameterNames = {"fun"}, behavior = PURE)
    public abstract static class IsDebugged extends ErrorAndFunAdapter {

        @Fallback
        @TruffleBoundary
        protected Object doDebug(@SuppressWarnings("unused") Object fun) {
            throw arg1Closure();
        }

        @Specialization
        @TruffleBoundary
        protected byte isDebugged(RFunction func) {
            RContext.getInstance().setVisible(true);
            return RRuntime.asLogical(DebugHandling.isDebugged(func));
        }
    }
}
