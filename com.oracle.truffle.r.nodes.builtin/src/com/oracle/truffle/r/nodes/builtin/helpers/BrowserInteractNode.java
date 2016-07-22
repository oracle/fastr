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
package com.oracle.truffle.r.nodes.builtin.helpers;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.r.runtime.JumpToTopLevelException;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RSource;
import com.oracle.truffle.r.runtime.RSrcref;
import com.oracle.truffle.r.runtime.ReturnException;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.context.ConsoleHandler;
import com.oracle.truffle.r.runtime.context.Engine.ParseException;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.nodes.RNode;

/**
 * The interactive component of the {@code browser} function.
 *
 * TODO GnuR does not allow quit() from the browser. This really needs to be checked in the quit
 * builtin somehow.
 */
public abstract class BrowserInteractNode extends RNode {

    public static final int STEP = 0;
    public static final int NEXT = 1;
    public static final int CONTINUE = 2;
    public static final int FINISH = 3;

    private static String lastEmptyLineCommand = "n";

    @Specialization
    protected int interact(VirtualFrame frame) {
        CompilerDirectives.transferToInterpreter();
        MaterializedFrame mFrame = frame.materialize();
        ConsoleHandler ch = RContext.getInstance().getConsoleHandler();
        String savedPrompt = ch.getPrompt();
        ch.setPrompt(browserPrompt(RArguments.getDepth(frame)));
        int exitMode = NEXT;
        try {
            RContext.getInstance().setInBrowser(true);
            LW: while (true) {
                String input = ch.readLine();
                if (input != null) {
                    input = input.trim();
                }
                if (input == null || input.length() == 0) {
                    byte browserNLdisabledVec = RRuntime.asLogicalObject(RContext.getInstance().stateROptions.getValue("browserNLdisabled"));
                    if (!RRuntime.fromLogical(browserNLdisabledVec)) {
                        input = lastEmptyLineCommand;
                    }
                }
                switch (input) {
                    case "c":
                    case "cont":
                        exitMode = CONTINUE;
                        break LW;
                    case "n":
                        exitMode = NEXT;
                        lastEmptyLineCommand = "n";
                        break LW;
                    case "s":
                        exitMode = STEP;
                        lastEmptyLineCommand = "s";
                        break LW;
                    case "f":
                        exitMode = FINISH;
                        break LW;
                    case "Q":
                        throw new JumpToTopLevelException();
                    case "where": {
                        if (RArguments.getDepth(mFrame) > 1) {
                            Object stack = Utils.createTraceback(0);
                            // browser inverts frame depth
                            int idepth = 1;
                            while (stack != RNull.instance) {
                                RPairList pl = (RPairList) stack;
                                RStringVector element = (RStringVector) pl.car();
                                ch.printf("where %d%s: %s%n", idepth, getSrcinfo(element), element.getDataAt(0));
                                idepth++;
                                stack = pl.cdr();
                            }
                        }
                        ch.println("");
                        break;
                    }

                    default:
                        try {
                            RContext.getEngine().parseAndEval(RSource.fromTextInternal(input, RSource.Internal.BROWSER_INPUT), mFrame, true);
                        } catch (ReturnException e) {
                            exitMode = NEXT;
                            break LW;
                        } catch (ParseException e) {
                            throw e.throwAsRError();
                        }
                        break;
                }
            }
        } finally {
            ch.setPrompt(savedPrompt);
            RContext.getInstance().setInBrowser(false);
        }
        return exitMode;
    }

    private static String getSrcinfo(RStringVector element) {
        Object srcref = element.getAttr(RRuntime.R_SRCREF);
        if (srcref != null) {
            RIntVector lloc = (RIntVector) srcref;
            Object srcfile = lloc.getAttr(RRuntime.R_SRCFILE);
            if (srcfile != null) {
                REnvironment env = (REnvironment) srcfile;
                return " at " + RRuntime.asString(env.get(RSrcref.SrcrefFields.filename.name())) + "#" + lloc.getDataAt(0);
            }
        }
        return "";
    }

    private static String browserPrompt(int depth) {
        return "Browse[" + depth + "]> ";
    }
}
