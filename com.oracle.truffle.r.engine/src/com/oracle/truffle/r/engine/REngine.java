/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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
import java.util.stream.*;

import org.antlr.runtime.*;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.r.library.graphics.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.nodes.instrument.*;
import com.oracle.truffle.r.nodes.runtime.*;
import com.oracle.truffle.r.options.*;
import com.oracle.truffle.r.parser.*;
import com.oracle.truffle.r.parser.ast.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.RContext.ConsoleHandler;
import com.oracle.truffle.r.runtime.Utils.DebugExitException;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.RPromise.Closure;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.env.*;
import com.oracle.truffle.r.runtime.env.REnvironment.PutException;
import com.oracle.truffle.r.runtime.env.frame.*;
import com.oracle.truffle.r.runtime.rng.*;

/**
 * The engine for the FastR implementation. Handles parsing and evaluation. There is exactly one
 * instance of this class, stored in {link #singleton}.
 */
public final class REngine implements RContext.Engine {

    private static final REngine singleton = new REngine();

    @CompilationFinal private boolean crashOnFatalError;
    @CompilationFinal private long startTime;
    @CompilationFinal private long[] childTimes;
    @CompilationFinal private RContext context;
    @CompilationFinal private RBuiltinLookup builtinLookup;

    /**
     * Only of relevance for in-process debugging, prevents most re-initialization on repeat calls
     * to {@link #initialize}.
     */
    private static boolean initialized;

    /**
     * {@code true} iff the base package is loaded.
     */
    private static boolean loadBase;

    /**
     * A temporary mechanism for suppressing warnings while evaluating the system profile, until the
     * proper mechanism is understood.
     */
    private static boolean reportWarnings;

    private REngine() {
    }

    /**
     * Initialize the engine.
     *
     * @param commandArgs
     * @param consoleHandler for console input/output
     * @param crashOnFatalErrorArg if {@code true} any unhandled exception will terminate the
     *            process.
     * @return a {@link VirtualFrame} that can be passed to
     *         {@link #parseAndEval(Source, MaterializedFrame, REnvironment, boolean, boolean)}
     */
    public static MaterializedFrame initialize(String[] commandArgs, ConsoleHandler consoleHandler, boolean crashOnFatalErrorArg, boolean headless) {
        singleton.startTime = System.nanoTime();
        singleton.childTimes = new long[]{0, 0};
        MaterializedFrame globalFrame = RRuntime.createNonFunctionFrame().materialize();
        if (!initialized) {
            RInstrument.initialize();
            RPerfAnalysis.initialize();
            Locale.setDefault(Locale.ROOT);
            RAccuracyInfo.initialize();
            ROptions.initialize();
            singleton.crashOnFatalError = crashOnFatalErrorArg;
            singleton.builtinLookup = RBuiltinPackages.getInstance();
            singleton.context = RContext.setRuntimeState(singleton, commandArgs, consoleHandler, new RASTHelperImpl(), headless);
            MaterializedFrame baseFrame = RRuntime.createNonFunctionFrame().materialize();
            REnvironment.baseInitialize(globalFrame, baseFrame);
            loadBase = FastROptions.LoadBase.getValue();
            RBuiltinPackages.loadBase(baseFrame, loadBase);
            RVersionInfo.initialize();
            RRNG.initialize();
            TempDirPath.initialize();
            RProfile.initialize();
            RGraphics.initialize();
            if (loadBase) {
                /*
                 * eval the system/site/user profiles. Experimentally GnuR does not report warnings
                 * during system profile evaluation, but does for the site/user profiles.
                 */
                singleton.parseAndEval(RProfile.systemProfile(), baseFrame, REnvironment.baseEnv(), false, false);
                checkAndRunStartupFunction(".OptRequireMethods");

                reportWarnings = true;
                Source siteProfile = RProfile.siteProfile();
                if (siteProfile != null) {
                    singleton.parseAndEval(siteProfile, baseFrame, REnvironment.baseEnv(), false, false);
                }
                Source userProfile = RProfile.userProfile();
                if (userProfile != null) {
                    singleton.parseAndEval(userProfile, globalFrame, REnvironment.globalEnv(), false, false);
                }
                /*
                 * TODO This is where we would load any saved user data
                 */
                checkAndRunStartupFunction(".First");
                checkAndRunStartupFunction(".First.sys");
                RBuiltinPackages.loadDefaultPackageOverrides();
            }
            initialized = true;
        }
        return globalFrame;
    }

    public static void checkAndRunStartupFunction(String name) {
        Object func = REnvironment.globalEnv().findFunction(name);
        if (func != null) {
            /*
             * We could just invoke runCall, but that way causes problems for debugging, so we parse
             * and eval a "fake" call.
             */
            RInstrument.checkDebugRequested(name, (RFunction) func);
            String call = name + "()";
            // Should this print the result?
            singleton.parseAndEval(Source.asPseudoFile(call, "<startup>"), REnvironment.globalEnv().getFrame(), REnvironment.globalEnv(), false, false);
        }
    }

    public void checkAndRunLast(String name) {
        checkAndRunStartupFunction(name);
    }

    public static REngine getInstance() {
        return singleton;
    }

    public boolean isPrimitiveBuiltin(String name) {
        return builtinLookup.isPrimitiveBuiltin(name);
    }

    public RFunction lookupBuiltin(String name) {
        return builtinLookup.lookup(name);
    }

    public long elapsedTimeInNanos() {
        return System.nanoTime() - startTime;
    }

    public long[] childTimesInNanos() {
        return childTimes;
    }

    public Object parseAndEval(Source sourceDesc, MaterializedFrame frame, REnvironment envForFrame, boolean printResult, boolean allowIncompleteSource) {
        return parseAndEvalImpl(new ANTLRStringStream(sourceDesc.getCode()), sourceDesc, frame, printResult, allowIncompleteSource);
    }

    public Object parseAndEvalTest(String rscript, boolean printResult) {
        // We first remove all the definitions from the previous test
        String rm = "rm(list = ls())";
        parseAndEvalImpl(new ANTLRStringStream(rm), Source.fromText(rm, "<test_reset>"), REnvironment.globalEnv().getFrame(), printResult, false);
        return parseAndEvalImpl(new ANTLRStringStream(rscript), Source.fromText(rscript, "<test_input>"), REnvironment.globalEnv().getFrame(), printResult, false);
    }

    public class ParseException extends Exception {
        private static final long serialVersionUID = 1L;

        public ParseException(String msg) {
            super(msg);
        }
    }

    public Node parseSingle(String singleExpression) {
        try {
            Sequence seq = (Sequence) ParseUtil.parseAST(new ANTLRStringStream(singleExpression), Source.asPseudoFile(singleExpression, "<parse_input>"));
            return transform(seq.getExpressions()[0]);
        } catch (RecognitionException ex) {
            throw Utils.fatalError("parseSingle failed");
        }
    }

    public RExpression parse(String rscript) throws RContext.Engine.ParseException {
        try {
            Sequence seq = (Sequence) ParseUtil.parseAST(new ANTLRStringStream(rscript), Source.asPseudoFile(rscript, "<parse_input>"));
            ASTNode[] exprs = seq.getExpressions();
            Object[] data = new Object[exprs.length];
            for (int i = 0; i < exprs.length; i++) {
                data[i] = RDataFactory.createLanguage(transform(exprs[i]));
            }
            return RDataFactory.createExpression(RDataFactory.createList(data));
        } catch (RecognitionException ex) {
            throw new RContext.Engine.ParseException(ex.getMessage());
        }
    }

    public Object eval(RExpression exprs, REnvironment envir, REnvironment enclos, int depth) throws PutException {
        Object result = RNull.instance;
        for (int i = 0; i < exprs.getLength(); i++) {
            Object obj = RASTUtils.checkForRSymbol(exprs.getDataAt(i));
            if (obj instanceof RLanguage) {
                result = evalNode((RNode) ((RLanguage) obj).getRep(), envir, enclos, depth);
            } else {
                result = obj;
            }
        }
        return result;
    }

    public Object eval(RLanguage expr, REnvironment envir, REnvironment enclos, int depth) throws PutException {
        return evalNode((RNode) expr.getRep(), envir, enclos, depth);
    }

    public Object eval(RExpression expr, MaterializedFrame frame) {
        Object result = null;
        for (int i = 0; i < expr.getLength(); i++) {
            result = expr.getDataAt(i);
            if (result instanceof RLanguage) {
                RLanguage lang = (RLanguage) result;
                result = eval(lang, frame);
            }
        }
        return result;
    }

    private static final String EVAL_FUNCTION_NAME = "<eval wrapper>";

    public Object eval(RLanguage expr, MaterializedFrame frame) {
        RNode n = (RNode) expr.getRep();
        RootCallTarget callTarget = doMakeCallTarget(n, EVAL_FUNCTION_NAME);
        return runCall(callTarget, frame, false, false);
    }

    /**
     * @return @see
     *         {@link #evalTarget(RootCallTarget, SourceSection, REnvironment, REnvironment, int)}
     */
    private static Object evalNode(RNode exprRep, REnvironment envir, REnvironment enclos, int depth) {
        RootCallTarget callTarget = doMakeCallTarget(exprRep, EVAL_FUNCTION_NAME);
        SourceSection callSrc = RArguments.getCallSourceSection(envir.getFrame());
        return evalTarget(callTarget, callSrc, envir, enclos, depth);
    }

    /**
     * This is tricky because the {@link Frame} "f" associated with {@code envir} has been
     * materialized so we can't evaluate in it directly. Instead we create a new
     * {@link VirtualEvalFrame} that behaves like "f" (delegates most calls to it) but has a
     * slightly changed arguments array.
     *
     * N.B. The implementation should do its utmost to avoid calling this method as it is inherently
     * inefficient. In particular, in the case where a {@link VirtualFrame} is available, then the
     * {@code eval} methods that take such a {@link VirtualFrame} should be used in preference.
     */
    private static Object evalTarget(RootCallTarget callTarget, SourceSection callSrc, REnvironment envir, @SuppressWarnings("unused") REnvironment enclos, int depth) {
        MaterializedFrame envFrame = envir.getFrame();
        // Here we create fake frame that wraps the original frame's context and has an only
        // slightly changed arguments array (function and callSrc).
        MaterializedFrame vFrame = VirtualEvalFrame.create(envFrame, (RFunction) null, callSrc, depth);
        return runCall(callTarget, vFrame, false, false);
    }

    public Object evalPromise(RPromise promise, MaterializedFrame frame) {
        return runCall(promise.getClosure().getCallTarget(), frame, false, false);
    }

    public Object evalPromise(Closure closure, MaterializedFrame frame) {
        return runCall(closure.getCallTarget(), frame, false, false);
    }

    public Object evalPromise(RPromise promise, SourceSection callSrc) {
        // have to do the full out eval
        MaterializedFrame frame = promise.getFrame().materialize();
        REnvironment env = REnvironment.frameToEnvironment(frame);
        assert env != null;
        Closure closure = promise.getClosure();
        return evalTarget(closure.getCallTarget(), callSrc, env, null, RArguments.getDepth(frame));
    }

    private static Object parseAndEvalImpl(ANTLRStringStream stream, Source source, MaterializedFrame frame, boolean printResult, boolean allowIncompleteSource) {
        try {
            RootCallTarget callTarget = doMakeCallTarget(parseToRNode(stream, source), "<repl wrapper>");
            return runCall(callTarget, frame, printResult, true);
        } catch (NoViableAltException | MismatchedTokenException e) {
            if (e.token.getType() == Token.EOF && allowIncompleteSource) {
                // the parser got stuck at the eof, request another line
                return INCOMPLETE_SOURCE;
            }
            String line = source.getCode(e.line);
            String message = "Error: unexpected '" + e.token.getText() + "' in \"" + line.substring(0, Math.min(line.length(), e.charPositionInLine + 1)) + "\"";
            singleton.context.getConsoleHandler().println(source.getLineCount() == 1 ? message : (message + " (line " + e.line + ")"));
            return null;
        } catch (RError e) {
            singleton.context.getConsoleHandler().println(e.getMessage());
            return null;
        } catch (UnsupportedSpecializationException use) {
            ConsoleHandler ch = singleton.context.getConsoleHandler();
            ch.println("Unsupported specialization in node " + use.getNode().getClass().getSimpleName() + " - supplied values: " +
                            Arrays.asList(use.getSuppliedValues()).stream().map(v -> v == null ? "null" : v.getClass().getSimpleName()).collect(Collectors.toList()));
            throw use;
        } catch (DebugExitException | BrowserQuitException e) {
            throw e;
        } catch (RecognitionException | RuntimeException e) {
            singleton.context.getConsoleHandler().println("Exception while parsing: " + e);
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
        return transform(ParseUtil.parseAST(stream, source));
    }

    /**
     * Transforms an AST produced by the parser into a Truffle AST.
     *
     * @param astNode parser AST instance
     * @return the root node of the Truffle AST
     */
    private static RNode transform(ASTNode astNode) {
        RTruffleVisitor transform = new RTruffleVisitor();
        RNode result = transform.transform(astNode);
        return result;
    }

    /**
     * Wraps the Truffle AST in {@code node} in an anonymous function and returns a
     * {@link RootCallTarget} for it.
     *
     * N.B. For certain expressions, there might be some value in enclosing the wrapper function in
     * a specific lexical scope. E.g., as a way to access names in the expression known to be
     * defined in that scope.
     *
     * @param body The AST for the body of the wrapper, i.e., the expression being evaluated.
     */
    @Override
    public RootCallTarget makeCallTarget(Object body, String funName) {
        assert body instanceof RNode;
        return doMakeCallTarget((RNode) body, funName);
    }

    /**
     * Creates an anonymous function, with no arguments, whose {@link FunctionStatementsNode} is
     * {@code body}.
     *
     * @param body
     * @return {@link #makeCallTarget(Object, String)}
     */
    @TruffleBoundary
    private static RootCallTarget doMakeCallTarget(RNode body, String funName) {
        FunctionBodyNode fbn = new FunctionBodyNode(SaveArgumentsNode.NO_ARGS, new FunctionStatementsNode(body));
        FrameDescriptor descriptor = new FrameDescriptor();
        FrameSlotChangeMonitor.initializeFrameDescriptor(descriptor, false);
        FunctionDefinitionNode rootNode = new FunctionDefinitionNode(null, descriptor, fbn, FormalArguments.NO_ARGS, funName, true, true);
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
    private static Object runCall(RootCallTarget callTarget, MaterializedFrame frame, boolean printResult, boolean topLevel) {
        Object result = null;
        try {
            try {
                // FIXME: callTargets should only be called via Direct/IndirectCallNode
                result = callTarget.call(frame);
            } catch (ControlFlowException cfe) {
                throw RError.error(RError.Message.NO_LOOP_FOR_BREAK_NEXT);
            }
            assert checkResult(result);
            if (printResult) {
                printResult(result);
            }
            reportWarnings(false);
        } catch (RError e) {
            if (topLevel) {
                singleton.printRError(e);
            } else {
                throw e;
            }
        } catch (UnsupportedSpecializationException use) {
            throw use;
        } catch (DebugExitException | BrowserQuitException e) {
            throw e;
        } catch (Throwable e) {
            reportImplementationError(e);
        }
        return result;
    }

    @TruffleBoundary
    private static boolean checkResult(Object result) {
        if (FastROptions.CheckResultCompleteness.getValue() && result instanceof RAbstractVector && ((RAbstractVector) result).isComplete()) {
            assert ((RAbstractVector) result).checkCompleteness() : "vector: " + result + " is not complete, but isComplete flag is true";
        }
        return true;
    }

    @TruffleBoundary
    private static void printResult(Object result) {
        if (RContext.isVisible()) {
            Object resultValue = result instanceof RPromise ? PromiseHelperNode.evaluateSlowPath(null, (RPromise) result) : result;
            if (loadBase) {
                Object printMethod = REnvironment.globalEnv().findFunction("print");
                RFunction function = (RFunction) (printMethod instanceof RPromise ? PromiseHelperNode.evaluateSlowPath(null, (RPromise) printMethod) : printMethod);
                function.getTarget().call(RArguments.create(function, null, REnvironment.baseEnv().getFrame(), 1, new Object[]{resultValue, RMissing.instance}));
            } else {
                // we only have the .Internal print.default method available
                getPrintInternal().getTarget().call(RArguments.create(printInternal, null, REnvironment.baseEnv().getFrame(), 1, new Object[]{resultValue}));
            }
        }
    }

    // Only relevant when running without base package loaded
    private static final String INTERNAL_PRINT = ".print.internal <- function(x) { .Internal(print.default(x, NULL, TRUE, NULL, NULL, FALSE, NULL, TRUE))" + "}";
    @CompilationFinal private static RFunction printInternal;

    private static RFunction getPrintInternal() {
        if (printInternal == null) {
            try {
                RExpression funDef = singleton.parse(INTERNAL_PRINT);
                printInternal = (RFunction) singleton.eval(funDef, REnvironment.baseEnv().getFrame());
            } catch (RContext.Engine.ParseException ex) {
                Utils.fail("failed to parse print.internal");
            }
        }
        return printInternal;

    }

    @TruffleBoundary
    public void printRError(RError e) {
        String es = e.toString();
        if (!es.isEmpty()) {
            context.getConsoleHandler().printErrorln(e.toString());
        }
        reportWarnings(true);
    }

    @TruffleBoundary
    private static void reportImplementationError(Throwable e) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        e.printStackTrace(new PrintStream(out));
        singleton.context.getConsoleHandler().printErrorln(out.toString());
        // R suicide, unless, e.g., we are running units tests.
        // We don't call quit as the system is broken.
        if (singleton.crashOnFatalError) {
            Utils.exit(2);
        }
    }

    @TruffleBoundary
    private static void reportWarnings(boolean inAddition) {
        List<String> evalWarnings = singleton.context.extractEvalWarnings();
        if (!reportWarnings) {
            return;
        }
        ConsoleHandler consoleHandler = singleton.context.getConsoleHandler();
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
