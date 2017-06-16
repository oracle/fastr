/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.nullValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;
import static com.oracle.truffle.r.runtime.RVisibility.OFF;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.COMPLEX;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import java.io.IOException;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.r.nodes.builtin.NodeWithArgumentCasts.Casts;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode.Arg3;
import com.oracle.truffle.r.nodes.builtin.casts.fluent.HeadPhaseBuilder;
import com.oracle.truffle.r.nodes.builtin.helpers.DebugHandling;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RSource;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

public class DebugFunctions {

    protected static Casts createCasts(Class<? extends RBuiltinNode> extCls) {
        Casts casts = new Casts(extCls);
        casts.arg("fun").mustBe(RFunction.class, Message.ARG_MUST_BE_CLOSURE);
        return casts;
    }

    protected static HeadPhaseBuilder<Boolean> flag(Casts casts, String parName) {
        return casts.arg(parName).asLogicalVector().findFirst().map(toBoolean());
    }

    protected static void doDebug(RBaseNode node, RFunction fun, Object text, Object condition, boolean once) throws RError {
        // GnuR does not generate an error for builtins, but debug (obviously) has no effect
        if (!fun.isBuiltin()) {
            if (!DebugHandling.enableDebug(fun, text, condition, once, false)) {
                throw node.error(RError.Message.GENERIC, "failed to attach debug handler (not instrumented?)");
            }
        }
    }

    @RBuiltin(name = "debug", visibility = OFF, kind = INTERNAL, parameterNames = {"fun", "text", "condition"}, behavior = COMPLEX)
    public abstract static class Debug extends RBuiltinNode.Arg3 {

        static {
            createCasts(Debug.class);
        }

        @Specialization
        @TruffleBoundary
        protected RNull debug(RFunction fun, Object text, Object condition) {
            doDebug(this, fun, text, condition, false);
            return RNull.instance;
        }
    }

    @RBuiltin(name = "debugonce", visibility = OFF, kind = INTERNAL, parameterNames = {"fun", "text", "condition"}, behavior = COMPLEX)
    public abstract static class DebugOnce extends RBuiltinNode.Arg3 {

        static {
            createCasts(DebugOnce.class);
        }

        @Specialization
        @TruffleBoundary
        protected RNull debugonce(RFunction fun, Object text, Object condition) {
            doDebug(this, fun, text, condition, true);
            return RNull.instance;
        }
    }

    @RBuiltin(name = "undebug", visibility = OFF, kind = INTERNAL, parameterNames = {"fun"}, behavior = COMPLEX)
    public abstract static class UnDebug extends RBuiltinNode.Arg1 {

        static {
            createCasts(UnDebug.class);
        }

        @Specialization
        @TruffleBoundary
        protected RNull undebug(RFunction func) {
            if (!DebugHandling.undebug(func)) {
                throw error(RError.Message.NOT_DEBUGGED);
            }
            return RNull.instance;
        }
    }

    @RBuiltin(name = "isdebugged", kind = INTERNAL, parameterNames = {"fun"}, behavior = PURE)
    public abstract static class IsDebugged extends RBuiltinNode.Arg1 {

        static {
            createCasts(IsDebugged.class);
        }

        @Specialization
        @TruffleBoundary
        protected byte isDebugged(RFunction func) {
            return RRuntime.asLogical(DebugHandling.isDebugged(func));
        }
    }

    @RBuiltin(name = ".fastr.setBreakpoint", visibility = OFF, kind = PRIMITIVE, parameterNames = {"srcfile", "line", "clear"}, behavior = COMPLEX)
    public abstract static class FastRSetBreakpoint extends Arg3 {

        static {
            Casts casts = new Casts(FastRSetBreakpoint.class);
            casts.arg("srcfile").mustNotBeMissing().mustBe(nullValue().not()).mustBe(stringValue()).asStringVector().findFirst();
            casts.arg("line").allowMissing().asIntegerVector().findFirst();
            casts.arg("clear").asLogicalVector().findFirst().map(toBoolean());
        }

        @SuppressWarnings("unused")
        @Specialization
        protected Object setBreakpoint(String fileLine, RMissing lineNr, boolean clear) {

            if (!fileLine.contains("#")) {
                throw error(RError.Message.GENERIC, "Line number missing");
            }

            int lastIndexOf = fileLine.lastIndexOf('#');
            assert lastIndexOf != -1;

            String fileName = fileLine.substring(0, lastIndexOf);
            int lnr = Integer.parseInt(fileLine.substring(lastIndexOf));

            return setBreakpoint(fileName, lnr, clear);
        }

        @SuppressWarnings("unused")
        @Specialization
        protected Object setBreakpoint(String fileName, int lineNr, boolean clear) {

            Source fromSrcfile;
            try {
                fromSrcfile = RSource.fromFileName(fileName, false);
                DebugHandling.enableLineDebug(fromSrcfile, lineNr);
                return RDataFactory.createStringVectorFromScalar(fileName + "#" + lineNr);
            } catch (IOException e) {
                return RNull.instance;
            }
        }
    }
}
