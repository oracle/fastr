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
package com.oracle.truffle.r.nodes.builtin;

import java.io.*;
import java.util.*;

import org.antlr.runtime.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.parser.*;
import com.oracle.truffle.r.parser.ast.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.RContext.ConsoleHandler;
import com.oracle.truffle.r.runtime.REnvironment.PutException;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.rng.*;

/**
 * The engine for the FastR implementation. Handles parsing and evaluation. There is exactly one
 * instance of this class, stored in {link #singleton}.
 */
public final class REngine implements RBuiltinLookupProvider {

    private static REngine singleton = new REngine();
    private static final RContext context = RContext.linkRBuiltinLookupProvider(singleton);
    private static boolean crashOnFatalError;
    private static long startTime;
    private static long[] childTimes;

    private REngine() {
    }

    public RBuiltinLookup getRBuiltinLookup() {
        return RDefaultBuiltinPackages.getInstance();
    }

    /**
     * Initialize the engine.
     *
     * @param commandArgs
     * @param consoleHandler for console input/output
     * @param crashOnFatalErrorArg if {@code true} any unhandled exception will terminate the
     *            process.
     * @return a {@link VirtualFrame} that can be passed to
     *         {@link #parseAndEval(String, VirtualFrame, REnvironment, boolean)}
     */
    public static VirtualFrame initialize(String[] commandArgs, ConsoleHandler consoleHandler, boolean crashOnFatalErrorArg, boolean headless) {
        startTime = System.nanoTime();
        childTimes = new long[]{0, 0};
        Locale.setDefault(Locale.ROOT);
        RPerfAnalysis.initialize();
        crashOnFatalError = crashOnFatalErrorArg;
        RContext.setRuntimeState(commandArgs, consoleHandler, headless);
        VirtualFrame globalFrame = RRuntime.createVirtualFrame();
        VirtualFrame baseFrame = RRuntime.createVirtualFrame();
        REnvironment.baseInitialize(globalFrame, baseFrame);
        RVersionInfo.initialize();
        RAccuracyInfo.initialize();
        RRNG.initialize();
        TempDirPath.initialize();
        LibPaths.initialize();
        ROptions.initialize();
        RProfile.initialize();
        // eval the system profile
        REngine.parseAndEval(RProfile.systemProfile(), baseFrame, REnvironment.baseEnv(), false);
        REnvironment.packagesInitialize(RPackages.initialize());
        RPackageVariables.initialize(); // TODO replace with R code
        String siteProfile = RProfile.siteProfile();
        if (siteProfile != null) {
            REngine.parseAndEval(siteProfile, baseFrame, REnvironment.baseEnv(), false);
        }
        String userProfile = RProfile.userProfile();
        if (userProfile != null) {
            REngine.parseAndEval(userProfile, globalFrame, REnvironment.globalEnv(), false);
        }
        return globalFrame;
    }

    /**
     * Elapsed time of process.
     *
     * @return elapsed time in nanosecs.
     */
    public static long elapsedTimeInNanos() {
        return System.nanoTime() - startTime;
    }

    /**
     * Return user and system times for any spawned child processes in nanosecs, < 0 means not
     * available (Windows).
     */
    public static long[] childTimesInNanos() {
        return childTimes;
    }

    public static RContext getContext() {
        return context;
    }

    /**
     * Parse and evaluate {@code rscript} in {@code frame}. {@code printResult == true}, the result
     * of the evaluation is printed to the console.
     *
     * @param envForFrame the environment that {@code frame} is bound to.
     * @return the object returned by the evaluation or {@code null} if an error occurred.
     */
    public static Object parseAndEval(String rscript, VirtualFrame frame, REnvironment envForFrame, boolean printResult) {
        return parseAndEvalImpl(new ANTLRStringStream(rscript), Source.asPseudoFile(rscript, "<shell_input>"), frame, envForFrame, printResult);
    }

    /**
     *
     * This is intended for use by the unit test environment, where a "fresh" global environment is
     * desired for each evaluation.
     */
    public static Object parseAndEvalTest(String rscript, boolean printResult) {
        VirtualFrame frame = RRuntime.createVirtualFrame();
        REnvironment.resetForTest(frame);
        return parseAndEvalImpl(new ANTLRStringStream(rscript), Source.asPseudoFile(rscript, "<test_input>"), frame, REnvironment.globalEnv(), printResult);
    }

    public static class ParseException extends Exception {
        private static final long serialVersionUID = 1L;

        public ParseException(String msg) {
            super(msg);
        }
    }

    /**
     * Parse an R expression and return an {@link RExpression} object representing the Truffle ASTs
     * for the components.
     */
    public static RExpression parse(String rscript) throws ParseException {
        try {
            Sequence seq = (Sequence) ParseUtil.parseAST(new ANTLRStringStream(rscript), Source.asPseudoFile(rscript, "<parse_input>"));
            ASTNode[] exprs = seq.getExprs();
            Object[] data = new Object[exprs.length];
            for (int i = 0; i < exprs.length; i++) {
                data[i] = RDataFactory.createLanguage(transform(exprs[i], REnvironment.emptyEnv()));
            }
            return RDataFactory.createExpression(RDataFactory.createList(data));
        } catch (RecognitionException ex) {
            throw new ParseException(ex.getMessage());
        }
    }

    /**
     * Support for the {@code eval} builtin using an {@link RExpression}.
     *
     * @param function TODO
     */
    public static Object eval(RFunction function, RExpression expr, REnvironment envir, REnvironment enclos) throws PutException {
        Object result = null;
        for (int i = 0; i < expr.getLength(); i++) {
            result = eval(function, (RLanguage) expr.getDataAt(i), envir, enclos);
        }
        return result;
    }

    /**
     * Support for the {@code eval} builtin. This is tricky because the {@link Frame} "f" associated
     * with {@code envir} has been materialized so we can't evaluate in it directly. Instead we
     * create a new {@link VirtualFrame}, that is a logical clone of "f", evaluate in that, and then
     * update "f" on return.
     *
     * @param function the actual function that invoked the "eval", e.g. {@code eval}, {@code evalq}
     *            , {@code local}.
     */
    public static Object eval(RFunction function, RLanguage expr, REnvironment envir, @SuppressWarnings("unused") REnvironment enclos) throws PutException {
        RootCallTarget callTarget = makeCallTarget((RNode) expr.getRep(), REnvironment.globalEnv());
        MaterializedFrame envFrame = envir.getFrame();
        VirtualFrame vFrame = RRuntime.createVirtualFrame();
        // We make the new frame look like it was a real call to "function".
        RArguments.setEnclosingFrame(vFrame, function.getEnclosingFrame());
        RArguments.setFunction(vFrame, function);
        FrameDescriptor envfd = envFrame.getFrameDescriptor();
        FrameDescriptor vfd = vFrame.getFrameDescriptor();
        // Copy existing bindings
        for (FrameSlot slot : envfd.getSlots()) {
            FrameSlotKind slotKind = slot.getKind();
            FrameSlot vFrameSlot = vfd.addFrameSlot(slot.getIdentifier(), slotKind);
            try {
                switch (slotKind) {
                    case Byte:
                        vFrame.setByte(vFrameSlot, envFrame.getByte(slot));
                        break;
                    case Int:
                        vFrame.setInt(vFrameSlot, envFrame.getInt(slot));
                        break;
                    case Double:
                        vFrame.setDouble(vFrameSlot, envFrame.getDouble(slot));
                        break;
                    case Object:
                        vFrame.setObject(vFrameSlot, envFrame.getObject(slot));
                        break;
                    case Illegal:
                        break;
                    default:
                        throw new FrameSlotTypeException();
                }
            } catch (FrameSlotTypeException ex) {
                throw new RuntimeException("unexpected FrameSlot exception", ex);
            }

        }
        Object result = runCall(callTarget, vFrame, false, false);
        if (result != null) {
            FrameDescriptor fd = vFrame.getFrameDescriptor();
            for (FrameSlot slot : fd.getSlots()) {
                envir.put(slot.getIdentifier().toString(), vFrame.getValue(slot));
            }
        }
        return result;
    }

    /**
     * Evaluate a promise in the given frame (for a builtin, where we can use the
     * {@link VirtualFrame}) of the caller directly).
     */
    public static Object evalPromise(RPromise expr, VirtualFrame frame) throws RError {
        RootCallTarget callTarget = makeCallTarget((RNode) expr.getRep(), REnvironment.emptyEnv());
        return expr.setValue(runCall(callTarget, frame, false, false));
    }

    private static Object parseAndEvalImpl(ANTLRStringStream stream, Source source, VirtualFrame frame, REnvironment envForFrame, boolean printResult) {
        try {
            return runCall(makeCallTarget(parseToRNode(stream, source), envForFrame), frame, printResult, true);
        } catch (RecognitionException | RuntimeException e) {
            context.getConsoleHandler().println("Exception while parsing: " + e);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Parses a text stream into a Truffle AST.
     *
     * @param stream
     * @param source
     * @return the root node of the Truffle AST
     * @throws RecognitionException on parse error
     */
    private static RNode parseToRNode(ANTLRStringStream stream, Source source) throws RecognitionException {
        return transform(ParseUtil.parseAST(stream, source), REnvironment.globalEnv());
    }

    /**
     * Transforms an AST produced by the parser into a Truffle AST.
     *
     * @param astNode parser AST instance
     * @param environment the lexically enclosing environment that will be associated with top-level
     *            function definitions in {@code astNode}
     * @return the root node of the Truffle AST
     */
    private static RNode transform(ASTNode astNode, REnvironment environment) {
        RTruffleVisitor transform = new RTruffleVisitor(environment);
        return transform.transform(astNode);
    }

    /**
     * Wraps the Truffle AST in {@code node} in an anonymous function and returns a
     * {@link RootCallTarget} for it.
     *
     * @param node
     * @param enclosing the enclosing environment to use for the anonymous function (value probably
     *            does not matter)
     */
    private static RootCallTarget makeCallTarget(RNode node, REnvironment enclosing) {
        REnvironment.FunctionDefinition rootNodeEnvironment = new REnvironment.FunctionDefinition(enclosing);
        FunctionDefinitionNode rootNode = new FunctionDefinitionNode(null, rootNodeEnvironment, node, RArguments.EMPTY_OBJECT_ARRAY, "<main>", true);
        RootCallTarget callTarget = Truffle.getRuntime().createCallTarget(rootNode);
        return callTarget;
    }

    /**
     * Execute {@code callTarget} in {@code frame}, optionally printing any result. N.B.
     * {@code callTarget.call} will create a new {@link VirtualFrame} called, say, {@code newFrame},
     * in which to execute the (anonymous) {@link FunctionDefinitionNode} associated with
     * {@code callTarget}. When execution reaches {@link FunctionDefinitionNode#execute},
     * {@code frame} will be accessible via {@code newFrame.getArguments()[0]}, and the execution
     * will continue using {@code frame}.
     */
    private static Object runCall(RootCallTarget callTarget, VirtualFrame frame, boolean printResult, boolean topLevel) {
        Object result = null;
        try {
            try {
                result = callTarget.call(frame);
            } catch (ControlFlowException cfe) {
                throw RError.getNoLoopForBreakNext(null);
            }
            if (printResult) {
                printResult(result);
            }
            reportWarnings(false);
        } catch (RError e) {
            if (topLevel) {
                reportRError(e);
            } else {
                throw e;
            }
        } catch (Throwable e) {
            reportImplementationError(e);
        }
        return result;
    }

    private static void printResult(Object result) {
        if (RContext.isVisible()) {
            RFunction function = RContext.getLookup().lookup("print");
            function.getTarget().call(RArguments.create(function, new Object[]{result}));
        }
    }

    private static void reportRError(RError e) {
        context.getConsoleHandler().printErrorln(e.toString());
        reportWarnings(true);
    }

    private static void reportImplementationError(Throwable e) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        e.printStackTrace(new PrintStream(out));
        context.getConsoleHandler().printErrorln(RRuntime.toString(out));
        // R suicide, unless, e.g., we are running units tests.
        // We don't call quit as the system is broken.
        if (crashOnFatalError) {
            Utils.exit(2);
        }
    }

    private static void reportWarnings(boolean inAddition) {
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
