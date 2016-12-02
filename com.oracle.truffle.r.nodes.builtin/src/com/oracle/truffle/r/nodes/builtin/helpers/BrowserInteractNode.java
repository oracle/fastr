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
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.r.nodes.attributes.GetFixedAttributeNode;
import com.oracle.truffle.r.runtime.JumpToTopLevelException;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RCaller;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RSource;
import com.oracle.truffle.r.runtime.RSrcref;
import com.oracle.truffle.r.runtime.ReturnException;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.context.ConsoleHandler;
import com.oracle.truffle.r.runtime.context.Engine.IncompleteSourceException;
import com.oracle.truffle.r.runtime.context.Engine.ParseException;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.instrument.InstrumentationState.BrowserState;
import com.oracle.truffle.r.runtime.nodes.RCodeBuilder;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

/**
 * The interactive component of the {@code browser} function.
 *
 * This is called in two ways:
 * <ol>
 * <li>implicitly when a function has had {@code debug} called</li>
 * <li>explicitly by a call in the source code. N.B. in this case we must enable debugging
 * (instrumentation) because a {@code n} command must stop at the next statement.</li>
 * </ol>
 *
 */
public abstract class BrowserInteractNode extends RNode {

    public static final int STEP = 0;
    public static final int NEXT = 1;
    public static final int CONTINUE = 2;
    public static final int FINISH = 3;

    @Child private GetFixedAttributeNode getSrcRefAttrNode;
    @Child private GetFixedAttributeNode getSrcFileAttrNode;

    @Specialization
    protected int interact(VirtualFrame frame) {
        CompilerDirectives.transferToInterpreter();
        MaterializedFrame mFrame = frame.materialize();
        ConsoleHandler ch = RContext.getInstance().getConsoleHandler();
        BrowserState browserState = RContext.getInstance().stateInstrumentation.getBrowserState();
        String savedPrompt = ch.getPrompt();
        RFunction callerFunction = RArguments.getFunction(frame);
        // we may be at top level where there is not caller
        boolean callerIsDebugged = callerFunction == null ? false : DebugHandling.isDebugged(callerFunction);
        int exitMode = NEXT;
        RCaller currentCaller = RArguments.getCall(mFrame);
        if (currentCaller == null) {
            currentCaller = RCaller.topLevel;
        }
        RCaller browserCaller = createCaller(currentCaller);
        try {
            browserState.setInBrowser(browserCaller);
            LW: while (true) {
                ch.setPrompt(browserPrompt(RArguments.getDepth(frame)));
                String input = ch.readLine();
                if (input != null) {
                    input = input.trim();
                }
                if (input == null || input.length() == 0) {
                    byte browserNLdisabledVec = RRuntime.asLogicalObject(RContext.getInstance().stateROptions.getValue("browserNLdisabled"));
                    if (!RRuntime.fromLogical(browserNLdisabledVec)) {
                        input = browserState.lastEmptyLineCommand();
                    }
                }
                switch (input) {
                    case "c":
                    case "cont":
                        exitMode = CONTINUE;
                        break LW;
                    case "n":
                        exitMode = NEXT;
                        // don't enable debugging if at top level
                        if (!callerIsDebugged) {
                            DebugHandling.enableDebug(callerFunction, "", "", true, true);
                        }
                        browserState.setLastEmptyLineCommand("n");
                        break LW;
                    case "s":
                        exitMode = STEP;
                        // don't enable debugging if at top level
                        if (!callerIsDebugged) {
                            DebugHandling.enableDebug(callerFunction, "", "", true, true);
                        }
                        browserState.setLastEmptyLineCommand("s");
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
                        StringBuffer sb = new StringBuffer(input);
                        while (true) {
                            try {
                                RContext.getEngine().parseAndEval(RSource.fromTextInternal(sb.toString(), RSource.Internal.BROWSER_INPUT), mFrame, true);
                            } catch (IncompleteSourceException e) {
                                // read another line of input
                                ch.setPrompt("+ ");
                                sb.append(ch.readLine());
                                // The only continuation in the while loop
                                continue;
                            } catch (ParseException e) {
                                e.report(ch);
                                continue LW;
                            } catch (RError e) {
                                continue LW;
                            } catch (ReturnException e) {
                                exitMode = NEXT;
                                break LW;
                            }
                            continue LW;
                        }
                }
            }
        } finally {
            ch.setPrompt(savedPrompt);
            browserState.setInBrowser(null);
        }
        return exitMode;
    }

    @TruffleBoundary
    private static RCaller createCaller(RCaller currentCaller) {
        RCodeBuilder<RSyntaxNode> builder = RContext.getASTBuilder();
        return RCaller.create(null, currentCaller, builder.call(RSyntaxNode.INTERNAL, builder.lookup(RSyntaxNode.INTERNAL, "browser", true)));
    }

    private String getSrcinfo(RStringVector element) {
        if (getSrcRefAttrNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            getSrcRefAttrNode = insert(GetFixedAttributeNode.create(RRuntime.R_SRCREF));
        }

        Object srcref = getSrcRefAttrNode.execute(element);
        if (srcref != null) {
            if (getSrcFileAttrNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getSrcFileAttrNode = insert(GetFixedAttributeNode.create(RRuntime.R_SRCFILE));
            }
            RIntVector lloc = (RIntVector) srcref;
            Object srcfile = getSrcFileAttrNode.execute(lloc);
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
