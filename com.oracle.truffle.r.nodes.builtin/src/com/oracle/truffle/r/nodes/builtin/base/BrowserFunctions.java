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

import java.util.ArrayList;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.RInvisibleBuiltinNode;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.RBuiltinKind;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.instrument.Browser;

public class BrowserFunctions {

    private static final class HelperState {

        private final String text;
        private final Object condition;

        private HelperState(String text, Object condition) {
            this.text = text;
            this.condition = condition;
        }
    }

    private static final ArrayList<HelperState> helperState = new ArrayList<>();

    @RBuiltin(name = "browser", kind = RBuiltinKind.PRIMITIVE, parameterNames = {"text", "condition", "expr", "skipCalls"})
    public abstract static class BrowserNode extends RInvisibleBuiltinNode {

        @Override
        public Object[] getDefaultParameterValues() {
            return new Object[]{"", RNull.instance, RRuntime.LOGICAL_TRUE, 0};
        }

        @SuppressWarnings("unused")
        @Specialization
        protected RNull browser(VirtualFrame frame, String text, RNull condition, byte expr, int skipCalls) {
            controlVisibility();
            if (RRuntime.fromLogical(expr)) {
                try {
                    helperState.add(new HelperState(text, condition));
                    MaterializedFrame mFrame = frame.materialize();
                    String callString = RContext.getRRuntimeASTAccess().getCallerSource(RArguments.getCall(mFrame));
                    RContext.getInstance().getConsoleHandler().printf("Called from: %s%n", REnvironment.isGlobalEnvFrame(frame) ? "top level" : callString);
                    Browser.interact(mFrame);
                } finally {
                    helperState.remove(helperState.size() - 1);
                }
            }
            return RNull.instance;
        }
    }

    private abstract static class RetrieveAdapter extends RBuiltinNode {

        /**
         * GnuR objects to indices <= 0 but allows positive indices that are out of range.
         */
        protected HelperState getHelperState(int n) {
            if (n <= 0) {
                throw RError.error(this, Message.POSITIVE_CONTEXTS);
            }
            int nn = n;
            if (nn > helperState.size()) {
                nn = helperState.size();
            }
            return helperState.get(nn - 1);
        }
    }

    @RBuiltin(name = "browserText", kind = RBuiltinKind.INTERNAL, parameterNames = {"n"})
    public abstract static class BrowserText extends RetrieveAdapter {

        @Specialization
        @TruffleBoundary
        protected String browserText(int n) {
            controlVisibility();
            return getHelperState(n).text;
        }

        @Specialization
        @TruffleBoundary
        protected String browserText(double n) {
            controlVisibility();
            return getHelperState((int) n).text;
        }
    }

    @RBuiltin(name = "browserCondition", kind = RBuiltinKind.INTERNAL, parameterNames = {"n"})
    public abstract static class BrowserCondition extends RetrieveAdapter {

        @Specialization
        @TruffleBoundary
        protected Object browserCondition(int n) {
            controlVisibility();
            return getHelperState(n).condition;
        }

        @Specialization
        @TruffleBoundary
        protected Object browserCondition(double n) {
            controlVisibility();
            return getHelperState((int) n).condition;
        }
    }

    @RBuiltin(name = "browserSetDebug", kind = RBuiltinKind.INTERNAL, parameterNames = {"n"})
    public abstract static class BrowserSetDebug extends RetrieveAdapter {

        @Specialization
        @TruffleBoundary
        protected RNull browserSetDebug(@SuppressWarnings("unused") int n) {
            // TODO implement
            controlVisibility();
            return RNull.instance;
        }

        @Override
        public final boolean getVisibility() {
            return false;
        }
    }
}
