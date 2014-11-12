/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.oracle.truffle.r.nodes.instrument.debug;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.r.runtime.BrowserQuitException;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RContext;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.ROptions;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.env.REnvironment;

/**
 * The interactive component of the {@code browser} function.
 */
public class Browser {

    public enum ExitMode {

        STEP,
        NEXT,
        CONTINUE,
    }

    @CompilerDirectives.TruffleBoundary
    public static ExitMode interact(MaterializedFrame frame) {
        RContext.ConsoleHandler ch = RContext.getInstance().getConsoleHandler();
        REnvironment callerEnv = REnvironment.frameToEnvironment(frame);
        String savedPrompt = ch.getPrompt();
        ch.setPrompt(browserPrompt(RArguments.getDepth(frame)));
        ExitMode exitMode = ExitMode.NEXT;
        try {
            LW:
            while (true) {
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
                        exitMode = ExitMode.CONTINUE;
                        break LW;
                    case "n":
                        exitMode = ExitMode.NEXT;
                        break LW;

                    case "s":
                        exitMode = ExitMode.STEP;
                        break LW;
                    case "f":
                        throw RError.nyi(null, notImplemented(input));
                    case "Q":
                        throw new BrowserQuitException();

                    case "where": {
                        int ix = RArguments.getDepth(frame);
                        Frame stackFrame;
                        while (ix >= 0 && (stackFrame = Utils.getStackFrame(FrameInstance.FrameAccess.READ_ONLY, ix)) != null) {
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
                        RContext.getEngine().parseAndEval("<browser_input>", input, frame, callerEnv, true, false);
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

    private static String notImplemented(String command) {
        return "browser command: '" + command + "'";
    }

}
