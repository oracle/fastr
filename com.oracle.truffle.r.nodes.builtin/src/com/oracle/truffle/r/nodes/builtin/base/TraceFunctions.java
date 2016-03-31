/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.RInvisibleBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.helpers.TraceHandling;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.RBuiltinKind;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RNull;

public class TraceFunctions {
    @RBuiltin(name = ".primTrace", kind = RBuiltinKind.PRIMITIVE, parameterNames = "what")
    public abstract static class PrimTrace extends RInvisibleBuiltinNode {

        @Specialization
        @TruffleBoundary
        protected RNull primTrace(RFunction func) {
            controlVisibility();
            if (!func.isBuiltin()) {
                if (!TraceHandling.enableTrace(func)) {
                    throw RError.error(this, RError.Message.GENERIC, "failed to attach trace handler");
                }
            }
            return RNull.instance;
        }
    }

    @RBuiltin(name = ".primUntrace", kind = RBuiltinKind.PRIMITIVE, parameterNames = "what")
    public abstract static class PrimUnTrace extends RInvisibleBuiltinNode {

        @Specialization
        @TruffleBoundary
        protected RNull primTrace(RFunction func) {
            controlVisibility();
            if (!func.isBuiltin()) {
                if (!TraceHandling.disableTrace(func)) {
                    throw RError.error(this, RError.Message.GENERIC, "failed to detach trace handler");
                }
            }
            return RNull.instance;
        }
    }

    @RBuiltin(name = "traceOnOff", kind = RBuiltinKind.INTERNAL, parameterNames = "state")
    public abstract static class TraceOnOff extends RBuiltinNode {
        @Specialization
        @TruffleBoundary
        protected byte traceOnOff(byte state) {
            boolean prevState = RContext.getInstance().stateTraceHandling.getTracingState();
            boolean newState = RRuntime.fromLogical(state);
            if (newState != prevState) {
                RContext.getInstance().stateTraceHandling.setTracingState(newState);
            }
            return RRuntime.asLogical(prevState);
        }

        @Specialization
        @TruffleBoundary
        protected byte traceOnOff(@SuppressWarnings("unused") RNull state) {
            return RRuntime.asLogical(RContext.getInstance().stateTraceHandling.getTracingState());
        }
    }
}
