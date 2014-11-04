/*
 * Copyright (c) 2014, 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.util.*;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.frame.FrameInstance.FrameAccess;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.RContext.ConsoleHandler;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.env.*;

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
    public abstract static class Browser extends RInvisibleBuiltinNode {

        @Override
        public RNode[] getParameterValues() {
            return new RNode[]{ConstantNode.create(""), ConstantNode.create(RNull.instance), ConstantNode.create(RRuntime.LOGICAL_TRUE), ConstantNode.create(0)};
        }

        @SuppressWarnings("unused")
        @Specialization
        protected RNull browser(VirtualFrame frame, String text, RNull condition, byte expr, int skipCalls) {
            controlVisibility();
            if (RRuntime.fromLogical(expr)) {
                try {
                    helperState.add(new HelperState(text, condition));
                    MaterializedFrame mFrame = frame.materialize();
                    RContext.getInstance().getConsoleHandler().printf("Called from: %s%n", REnvironment.isGlobalEnvFrame(frame) ? "top level" : RArguments.getFunction(frame).getTarget());
                    doBrowser(mFrame);
                } finally {
                    helperState.remove(helperState.size() - 1);
                }
            }
            return RNull.instance;
        }

        @TruffleBoundary
        public static void doBrowser(MaterializedFrame frame) {
            ConsoleHandler ch = RContext.getInstance().getConsoleHandler();
            REnvironment callerEnv = REnvironment.frameToEnvironment(frame);
            String savedPrompt = ch.getPrompt();
            ch.setPrompt(browserPrompt());
            try {
                LW: while (true) {
                    String input = ch.readLine();
                    if (input.length() == 0) {
                        RLogicalVector browserNLdisabledVec = (RLogicalVector) ROptions.getValue("browserNLdisabled");
                        if (!RRuntime.fromLogical(browserNLdisabledVec.getDataAt(0))) {
                            break;
                        }
                    } else {
                        input = input.trim();
                    }
                    switch (input) {
                        case "c":
                        case "cont":
                        case "n":
                            break LW;

                        case "s":
                        case "f":
                            throw RError.nyi(null, notImplemented(input));

                        case "where": {
                            int ix = RArguments.getDepth(frame);
                            Frame stackFrame;
                            while (ix >= 0 && (stackFrame = Utils.getStackFrame(FrameAccess.READ_ONLY, ix)) != null) {
                                RFunction fun = RArguments.getFunction(stackFrame);
                                if (fun != null) {
                                    ch.printf("where %d: %s%n", ix, fun.getTarget());
                                }
                                ix--;
                            }
                            ch.println("");
                            break;
                        }

                        default:
                            RContext.getEngine().parseAndEval("<browser_input>", input, frame.materialize(), callerEnv, true, false);
                            break;
                    }
                }
            } finally {
                ch.setPrompt(savedPrompt);
            }
        }

        private static String browserPrompt() {
            return "Browse[" + (helperState.size()) + "]> ";
        }

        private static String notImplemented(String command) {
            return "browser command: '" + command + "'";
        }
    }

    private abstract static class RetrieveAdapter extends RBuiltinNode {

        @Override
        public RNode[] getParameterValues() {
            return new RNode[]{ConstantNode.create(1)};
        }

        /**
         * GnuR objects to indices <= 0 but allows positive indices that are out of range.
         */
        protected HelperState getHelperState(int n) {
            if (n <= 0) {
                throw RError.error(getEncapsulatingSourceSection(), Message.POSITIVE_CONTEXTS);
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
