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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.*;
import static com.oracle.truffle.r.runtime.RVisibility.OFF;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.COMPLEX;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.helpers.BrowserInteractNode;
import com.oracle.truffle.r.nodes.builtin.helpers.BrowserInteractNodeGen;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RCaller;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.instrument.InstrumentationState.BrowserState;
import com.oracle.truffle.r.runtime.instrument.InstrumentationState.BrowserState.HelperState;

public class BrowserFunctions {

    @RBuiltin(name = "browser", visibility = OFF, kind = PRIMITIVE, parameterNames = {"text", "condition", "expr", "skipCalls"}, behavior = COMPLEX)
    public abstract static class BrowserNode extends RBuiltinNode {

        @Child private BrowserInteractNode browserInteractNode = BrowserInteractNodeGen.create();

        @Override
        public Object[] getDefaultParameterValues() {
            return new Object[]{"", RNull.instance, RRuntime.LOGICAL_TRUE, 0};
        }

        @Override
        protected void createCasts(CastBuilder casts) {
            // TODO: add support for conditions conditions
            casts.arg("condition").mustBe(nullValue(), RError.Message.GENERIC, "Only NULL conditions currently supported in browser");
            casts.arg("expr").asLogicalVector().findFirst().map(toBoolean());
            casts.arg("skipCalls").asIntegerVector().findFirst();
        }

        @SuppressWarnings("unused")
        @Specialization
        protected RNull browser(VirtualFrame frame, Object text, RNull condition, boolean expr, int skipCalls) {
            if (expr) {
                BrowserState browserState = RContext.getInstance().stateInstrumentation.getBrowserState();
                try {
                    browserState.push(new HelperState(text, condition));
                    MaterializedFrame mFrame = frame.materialize();
                    RCaller caller = RArguments.getCall(mFrame);
                    doPrint(caller);
                    browserInteractNode.execute(frame);
                } finally {
                    browserState.pop();
                }
            }
            RContext.getInstance().setVisible(false);
            return RNull.instance;
        }

        @TruffleBoundary
        private static void doPrint(RCaller caller) {
            String callerString;
            if (caller == null || (!caller.isValidCaller() && caller.getDepth() == 0 && caller.getParent() == null)) {
                callerString = "top level";
            } else {
                callerString = RContext.getRRuntimeASTAccess().getCallerSource(caller);
            }
            RContext.getInstance().getConsoleHandler().printf("Called from: %s%n", callerString);

        }
    }

    private abstract static class RetrieveAdapter extends RBuiltinNode {

        @Override
        protected void createCasts(CastBuilder casts) {
            casts.arg("n").asIntegerVector().findFirst().mustBe(gt(0), Message.POSITIVE_CONTEXTS);
        }

        /**
         * GnuR objects to indices <= 0 but allows positive indices that are out of range.
         */
        protected HelperState getHelperState(int n) {
            BrowserState helperState = RContext.getInstance().stateInstrumentation.getBrowserState();
            return helperState.get(n);
        }
    }

    @RBuiltin(name = "browserText", kind = INTERNAL, parameterNames = {"n"}, behavior = COMPLEX)
    public abstract static class BrowserText extends RetrieveAdapter {

        @Specialization
        @TruffleBoundary
        protected Object browserText(int n) {
            return getHelperState(n).text;
        }

    }

    @RBuiltin(name = "browserCondition", kind = INTERNAL, parameterNames = {"n"}, behavior = COMPLEX)
    public abstract static class BrowserCondition extends RetrieveAdapter {

        @Specialization
        @TruffleBoundary
        protected Object browserCondition(int n) {
            return getHelperState(n).condition;
        }

    }

    @RBuiltin(name = "browserSetDebug", visibility = OFF, kind = INTERNAL, parameterNames = {"n"}, behavior = COMPLEX)
    public abstract static class BrowserSetDebug extends RetrieveAdapter {

        @Specialization
        @TruffleBoundary
        protected RNull browserSetDebug(@SuppressWarnings("unused") int n) {
            // TODO implement
            RContext.getInstance().setVisible(false);
            return RNull.instance;
        }
    }
}
