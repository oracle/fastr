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

import com.oracle.truffle.api.CompilerDirectives.SlowPath;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.frame.FrameInstance.FrameAccess;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.RContext.ConsoleHandler;
import com.oracle.truffle.r.runtime.data.*;

public class BrowserFunctions {

    @RBuiltin(name = "browser", kind = RBuiltinKind.PRIMITIVE)
    public abstract static class Browser extends RInvisibleBuiltinNode {

        private static final String[] PARAMETER_NAMES = new String[]{"text", "condition", "expr", "skipCalls"};

        @Override
        public Object[] getParameterNames() {
            return PARAMETER_NAMES;
        }

        @Override
        public RNode[] getParameterValues() {
            return new RNode[]{ConstantNode.create(""), ConstantNode.create(RNull.instance), ConstantNode.create(RRuntime.LOGICAL_TRUE), ConstantNode.create(0)};
        }

        @Specialization
        public RNull browser(VirtualFrame frame, @SuppressWarnings("unused") String x) {
            controlVisibility();
            doBrowser(frame);
            return RNull.instance;
        }

        @SuppressWarnings("static-method")
        @SlowPath
        private void doBrowser(VirtualFrame frame) {
            ConsoleHandler ch = RContext.getInstance().getConsoleHandler();
            REnvironment callerEnv = EnvFunctions.frameToEnvironment(frame);
            ch.println("Called from: " + (callerEnv == REnvironment.globalEnv() ? "top level" : "function"));
            String savedPrompt = ch.getPrompt();
            ch.setPrompt("Browse[1]> ");
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
                            throw RError.nyi(null, input);

                        case "where": {
                            int ix = 1;
                            Frame stackFrame;
                            while ((stackFrame = Utils.getStackFrame(FrameAccess.READ_ONLY, ix)) != null) {
                                RFunction fun = RArguments.getFunction(stackFrame);
                                if (fun != null) {
                                    ch.println("where " + ix + ": " + fun.getTarget());
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

    }

    @RBuiltin(name = "browserText", kind = RBuiltinKind.INTERNAL)
    public abstract static class BrowserText extends RBuiltinNode {
        @Specialization
        public String browserText(@SuppressWarnings("unused") int n) {
            // TODO implement
            controlVisibility();
            return null;
        }
    }

    @RBuiltin(name = "browserCondition", kind = RBuiltinKind.INTERNAL)
    public abstract static class BrowserCondition extends RBuiltinNode {
        @Specialization
        public String browserCondition(@SuppressWarnings("unused") int n) {
            // TODO implement
            controlVisibility();
            return null;
        }
    }

    @RBuiltin(name = "browserSetDebug", kind = RBuiltinKind.INTERNAL)
    public abstract static class BrowserSetDebug extends RInvisibleBuiltinNode {
        @Specialization
        public RNull browserSetDebug(@SuppressWarnings("unused") int n) {
            // TODO implement
            controlVisibility();
            return RNull.instance;
        }
    }
}
