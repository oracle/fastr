/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.engine;

import java.io.*;
import java.util.*;

import org.antlr.runtime.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.impl.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.parser.*;
import com.oracle.truffle.r.parser.ast.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.RContext.*;
import com.oracle.truffle.r.runtime.data.*;

/**
 * The engine for the FastR implementation. Handles parsing and evaluation. There is exactly one
 * instance of this class, stored in {link #singleton}.
 */
public final class REngine {

    private final RContext context;

    private static REngine singleton;

    private REngine(String[] commandArgs, ConsoleHandler consoleHandler) {
        RDefaultPackages defaultPackages = RDefaultPackages.getInstance();
        this.context = RContext.instantiate(defaultPackages, commandArgs, consoleHandler);
        defaultPackages.load();
    }

    /**
     * Get the singleton engine instance, creating it if necessary.
     */
    public static REngine getInstance(String[] commandArgs, ConsoleHandler consoleHandler) {
        if (singleton == null) {
            singleton = new REngine(commandArgs, consoleHandler);
        }
        return singleton;
    }

    public RContext getContext() {
        return context;
    }

    /**
     * Create a {@link VirtualFrame} for use in {@link #parseAndEval(String, VirtualFrame, boolean)}
     * , for accumulating the results fromevaluatring expressions in an interactive context. Such a
     * value cannot be stored in an object field, so must be passed as an argument.
     */
    public static VirtualFrame createVirtualFrame() {
        return Truffle.getRuntime().createVirtualFrame(null, RArguments.create(), new FrameDescriptor());
    }

    public Object parseAndEval(File file, boolean printResult) throws IOException {
        String path = file.getAbsolutePath();
        return parseAndEvalImpl(new ANTLRFileStream(path), context.getSourceManager().get(path), null, printResult);
    }

    /**
     * Parse and evaluate {@code rscript}. Value of {@code globalFrame} may be null. If
     * {@code printResult == true}, the result of the evaluation is printed to the console.
     */
    public Object parseAndEval(String rscript, VirtualFrame globalFrame, boolean printResult) {
        return parseAndEvalImpl(new ANTLRStringStream(rscript), context.getSourceManager().getFakeFile("<shell_input>", rscript), globalFrame, printResult);
    }

    public Object parseAndEval(String rscript, boolean printResult) {
        return parseAndEvalImpl(new ANTLRStringStream(rscript), context.getSourceManager().getFakeFile("<shell_input>", rscript), null, printResult);
    }

    private Object parseAndEvalImpl(ANTLRStringStream stream, Source source, VirtualFrame globalFrame, boolean printResult) {
        try {
            RTruffleVisitor transform = new RTruffleVisitor();
            RNode node = transform.transform(parseAST(stream, source));
            FunctionDefinitionNode rootNode = new FunctionDefinitionNode(null, transform.getEnvironment(), node, RArguments.EMPTY_OBJECT_ARRAY, "<main>");
            CallTarget callTarget = Truffle.getRuntime().createCallTarget(rootNode);
            if (globalFrame == null) {
                return run(callTarget, printResult);
            } else {
                return runGlobal(callTarget, globalFrame, printResult);
            }
        } catch (RecognitionException | RuntimeException e) {
            context.getConsoleHandler().println("Exception while parsing: " + e);
            e.printStackTrace();
            return null;
        }
    }

    private static ASTNode parseAST(ANTLRStringStream stream, Source source) throws RecognitionException {
        CommonTokenStream tokens = new CommonTokenStream();
        RLexer lexer = new RLexer(stream);
        tokens.setTokenSource(lexer);
        RParser parser = new RParser(tokens);
        parser.setSource(source);
        return parser.script().v;
    }

    private Object run(CallTarget callTarget, boolean printResult) {
        Object result = null;
        try {
            try {
                result = callTarget.call(null, RArguments.create());
            } catch (ControlFlowException cfe) {
                throw RError.getNoLoopForBreakNext(null);
            }
            if (printResult) {
                printResult(result);
            }
            reportWarnings(false);
        } catch (RError e) {
            reportRError(e);
        } catch (Throwable e) {
            reportImplementationError(e);
        }
        return result;
    }

    private Object runGlobal(CallTarget callTarget, VirtualFrame globalFrame, boolean printResult) {
        Object result = null;
        try {
            try {
                result = ((DefaultCallTarget) callTarget).getRootNode().execute(globalFrame);
            } catch (ControlFlowException cfe) {
                throw RError.getNoLoopForBreakNext(null);
            }
            if (printResult) {
                printResult(result);
            }
            reportWarnings(false);
        } catch (RError e) {
            reportRError(e);
        } catch (Throwable e) {
            reportImplementationError(e);
        }
        return result;
    }

    private void printResult(Object result) {
        if (!(result instanceof RInvisible)) {
            RFunction function = context.getLookup().lookup("print");
            RRuntime.toString(function.call(null, RArguments.create(function, new Object[]{result})));
        }
    }

    private void reportRError(RError e) {
        context.getConsoleHandler().printErrorln("Error: " + e.getMessage());
        reportWarnings(true);
    }

    private void reportImplementationError(Throwable e) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        e.printStackTrace(new PrintStream(out));
        context.getConsoleHandler().printErrorln(RRuntime.toString(out));

    }

    private void reportWarnings(boolean inAddition) {
        List<String> evalWarnings = context.extractEvalWarnings();
        ConsoleHandler consoleHandler = context.getConsoleHandler();
        // GnuR outputs warnings to the stderr, so we do too
        if (evalWarnings != null && evalWarnings.size() > 0) {
            if (inAddition) {
                consoleHandler.printError("In addition: ");
            }
            if (evalWarnings.size() == 1) {
                consoleHandler.printErrorln("Warning message:");
                consoleHandler.printErrorln(evalWarnings.get(0));
            } else {
                consoleHandler.printErrorln("Warning messages:");
                for (int i = 0; i < evalWarnings.size(); i++) {
                    consoleHandler.printErrorln((i + 1) + ":");
                    consoleHandler.printErrorln("  " + evalWarnings.get(i));
                }
            }
        }
    }

}
