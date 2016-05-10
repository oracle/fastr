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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RInvisibleBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.helpers.DebugHandling;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.RBuiltinKind;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RVisibility;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RNull;

public class DebugFunctions {

    protected abstract static class ErrorAdapter extends RInvisibleBuiltinNode {

        protected RError arg1Closure() throws RError {
            throw RError.error(this, RError.Message.ARG_MUST_BE_CLOSURE);
        }

        protected void doDebug(RFunction fun, Object text, Object condition, boolean once) throws RError {
            // GnuR does not generate an error for builtins, but debug (obviously) has no effect
            if (!fun.isBuiltin()) {
                if (!DebugHandling.enableDebug(fun, text, condition, once)) {
                    throw RError.error(this, RError.Message.GENERIC, "failed to attach debug handler (not instrumented?)");
                }
            }
        }
    }

    @RBuiltin(name = "debug", visibility = RVisibility.OFF, kind = RBuiltinKind.INTERNAL, parameterNames = {"fun", "text", "condition"})
    public abstract static class Debug extends ErrorAdapter {

        @SuppressWarnings("unused")
        @Fallback
        @TruffleBoundary
        protected Object doDebug(Object fun, Object text, Object condition) {
            throw arg1Closure();
        }

        @Specialization
        @TruffleBoundary
        protected RNull doDebug(RFunction fun, Object text, Object condition) {
            controlVisibility();
            doDebug(fun, text, condition, false);
            return RNull.instance;
        }
    }

    @RBuiltin(name = "debugonce", visibility = RVisibility.OFF, kind = RBuiltinKind.INTERNAL, parameterNames = {"fun", "text", "condition"})
    public abstract static class DebugOnce extends ErrorAdapter {

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
            controlVisibility();
            doDebug(fun, text, condition, true);
            return RNull.instance;
        }
    }

    @RBuiltin(name = "undebug", visibility = RVisibility.OFF, kind = RBuiltinKind.INTERNAL, parameterNames = {"fun"})
    public abstract static class UnDebug extends ErrorAdapter {

        @Fallback
        @TruffleBoundary
        protected Object doDebug(@SuppressWarnings("unused") Object fun) {
            throw arg1Closure();
        }

        @Specialization
        @TruffleBoundary
        protected RNull undebug(RFunction func) {
            controlVisibility();
            if (!DebugHandling.undebug(func)) {
                throw RError.error(this, RError.Message.NOT_DEBUGGED);
            }
            return RNull.instance;
        }
    }

    @RBuiltin(name = "isdebugged", kind = RBuiltinKind.INTERNAL, parameterNames = {"fun"})
    public abstract static class IsDebugged extends ErrorAdapter {

        @Fallback
        @TruffleBoundary
        protected Object doDebug(@SuppressWarnings("unused") Object fun) {
            throw arg1Closure();
        }

        @Specialization
        @TruffleBoundary
        protected byte isDebugged(RFunction func) {
            forceVisibility(true);
            return RRuntime.asLogical(DebugHandling.isDebugged(func));
        }
    }
}
