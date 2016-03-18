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
package com.oracle.truffle.r.runtime.instrument;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.r.runtime.BrowserQuitException;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RCaller;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.ReturnException;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.context.ConsoleHandler;
import com.oracle.truffle.r.runtime.context.Engine.ParseException;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RLogicalVector;

/**
 * The interactive component of the {@code browser} function.
 */
public class Browser {

    public enum ExitMode {
        STEP,
        NEXT,
        CONTINUE,
        FINISH
    }

    private static final String BROWSER_SOURCE = "<browser_input>";
    private static String lastEmptyLineCommand = "n";

    @TruffleBoundary
    public static ExitMode interact(MaterializedFrame frame) {
        ConsoleHandler ch = RContext.getInstance().getConsoleHandler();
        String savedPrompt = ch.getPrompt();
        ch.setPrompt(browserPrompt(RArguments.getDepth(frame)));
        ExitMode exitMode = ExitMode.NEXT;
        try {
            LW: while (true) {
                String input = ch.readLine().trim();
                if (input.length() == 0) {
                    RLogicalVector browserNLdisabledVec = (RLogicalVector) RContext.getInstance().stateROptions.getValue("browserNLdisabled");
                    if (!RRuntime.fromLogical(browserNLdisabledVec.getDataAt(0))) {
                        input = lastEmptyLineCommand;
                    }
                }
                switch (input) {
                    case "c":
                    case "cont":
                        exitMode = ExitMode.CONTINUE;
                        break LW;
                    case "n":
                        exitMode = ExitMode.NEXT;
                        lastEmptyLineCommand = "n";
                        break LW;
                    case "s":
                        exitMode = ExitMode.STEP;
                        lastEmptyLineCommand = "s";
                        break LW;
                    case "f":
                        exitMode = ExitMode.FINISH;
                        break LW;
                    case "Q":
                        throw new BrowserQuitException();
                    case "where": {
                        int ix = RArguments.getDepth(frame);
                        Frame stackFrame = frame;
                        do {
                            RCaller caller = RArguments.getCall(stackFrame);
                            String callString;
                            if (caller == null) {
                                // FIXME should not happen, seems S4-related
                                callString = "<no source>";
                            } else {
                                callString = RContext.getRRuntimeASTAccess().getCallerSource(caller);
                            }
                            ch.println(callString);
                            ix--;
                        } while (ix > 0 && (stackFrame = Utils.getStackFrame(FrameInstance.FrameAccess.READ_ONLY, ix)) != null);
                        ch.println("");
                        break;
                    }

                    default:
                        try {
                            RContext.getEngine().parseAndEval(Source.fromText(input, BROWSER_SOURCE), frame, true);
                        } catch (ReturnException e) {
                            exitMode = ExitMode.NEXT;
                            break LW;
                        } catch (ParseException e) {
                            throw e.throwAsRError();
                        }
                        break;
                }
            }
        } finally {
            ch.setPrompt(savedPrompt);
        }
        return exitMode;
    }

    private static String browserPrompt(int depth) {
        return "Browse[" + depth + "]> ";
    }
}
