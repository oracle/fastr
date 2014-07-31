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

import com.oracle.truffle.api.CompilerDirectives.SlowPath;
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

public class BrowserFunctions {

    private static final class HelperState {
        String text;
        Object condition;

        HelperState(String text, Object condition) {
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
        public RNull browser(VirtualFrame frame, String text, RNull condition, byte expr, int skipCalls) {
            controlVisibility();
            if (RRuntime.fromLogical(expr)) {
                try {
                    helperState.add(new HelperState(text, condition));
                    doBrowser(frame);
                } finally {
                    helperState.remove(helperState.size() - 1);
                }
            }
            return RNull.instance;
        }

        private static void doBrowser(VirtualFrame frame) {
            ConsoleHandler ch = RContext.getInstance().getConsoleHandler();
            REnvironment callerEnv = REnvironment.frameToEnvironment(frame.materialize());
            ch.printf("Called from: %s%n", callerEnv == REnvironment.globalEnv() ? "top level" : RArguments.getFunction(frame).getTarget());
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
                            break LW;

                        case "s":
                        case "f":
                        case "n":
                            throw RError.nyi(null, notImplemented(input));

                        case "where": {
                            int ix = 1;
                            Frame stackFrame;
                            while ((stackFrame = Utils.getStackFrame(FrameAccess.READ_ONLY, ix)) != null) {
                                RFunction fun = RArguments.getFunction(stackFrame);
                                if (fun != null) {
                                    ch.printf("where %d: %s%n", ix, fun.getTarget());
                                }
                                ix++;
                            }
                            ch.println("");
                            break;
                        }

                        default:
                            RContext.getEngine().parseAndEval(input, frame, callerEnv, true);
                            break;
                    }
                }
            } finally {
                ch.setPrompt(savedPrompt);
            }
        }

        @SlowPath
        private static String browserPrompt() {
            return "Browse[" + (helperState.size()) + "]> ";
        }

        @SlowPath
        private static String notImplemented(String command) {
            return "browser command: '" + command + "'";
        }
    }

    private abstract static class RetrieveAdapter extends RBuiltinNode {
        private static final String[] PARAMETER_NAMES = new String[]{"n"};

        @Override
        public Object[] getParameterNames() {
            return PARAMETER_NAMES;
        }

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

    @RBuiltin(name = "browserText", kind = RBuiltinKind.INTERNAL)
    public abstract static class BrowserText extends RetrieveAdapter {
        @Specialization
        public String browserText(int n) {
            controlVisibility();
            return getHelperState(n).text;
        }

        @Specialization
        public String browserText(double n) {
            controlVisibility();
            return getHelperState((int) n).text;
        }
    }

    @RBuiltin(name = "browserCondition", kind = RBuiltinKind.INTERNAL)
    public abstract static class BrowserCondition extends RetrieveAdapter {
        @Specialization
        public Object browserCondition(int n) {
            controlVisibility();
            return getHelperState(n).condition;
        }

        @Specialization
        public Object browserCondition(double n) {
            controlVisibility();
            return getHelperState((int) n).condition;
        }
    }

    @RBuiltin(name = "browserSetDebug", kind = RBuiltinKind.INTERNAL)
    public abstract static class BrowserSetDebug extends RetrieveAdapter {
        @Specialization
        public RNull browserSetDebug(@SuppressWarnings("unused") int n) {
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
