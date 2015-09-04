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

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.impl.*;
import com.oracle.truffle.api.instrument.*;
import com.oracle.truffle.api.interop.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.nodes.Node.Child;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.r.engine.interop.*;
import com.oracle.truffle.r.library.graphics.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.control.*;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.nodes.instrument.*;
import com.oracle.truffle.r.nodes.runtime.*;
import com.oracle.truffle.r.parser.*;
import com.oracle.truffle.r.parser.ast.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.RCmdOptions.RCmdOption;
import com.oracle.truffle.r.runtime.Utils.DebugExitException;
import com.oracle.truffle.r.runtime.context.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.RPromise.Closure;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.env.*;
import com.oracle.truffle.r.runtime.env.frame.*;
import com.oracle.truffle.r.runtime.nodes.*;

/**
 * The engine for the FastR implementation. Handles parsing and evaluation. There is one instance of
 * this class per {@link RContext}.
 */
final class REngine implements Engine {

    /**
     * Controls the behavior when an implementation errors occurs. In normal use this is fatal as
     * the system is in an undefined and likely unusable state. However, there are special
     * situations, e.g. unit tests, where we want to continue and delegate the termination to a
     * higher authority.
     */
    @CompilationFinal private boolean crashOnFatalError;
    /**
     * The system time when this engine was started.
     */
    @CompilationFinal private long startTime;
    /**
     * The accumulated time spent by child processes on behalf of this engine.
     */
    @CompilationFinal private long[] childTimes;
    /**
     * The {@link RContext} that this engine is associated with (1-1).
     */
    private final RContext context;

    /**
     * The unique frame for the global environment for this engine.
     */
    @CompilationFinal private MaterializedFrame globalFrame;

    /**
     * {@code true} iff the base package is loaded.
     */
    private static boolean loadBase;

    /**
     * A temporary mechanism for suppressing warnings while evaluating the system profile, until the
     * proper mechanism is understood.
     */
    private boolean suppressWarnings;

    public void disableCrashOnFatalError() {
        this.crashOnFatalError = false;
    }

    private REngine(RContext context) {
        this.context = context;
        this.childTimes = new long[]{0, 0};
    }

    static REngine create(RContext context) {
        REngine engine = new REngine(context);
        return engine;
    }

    public void activate(REnvironment.ContextStateImpl stateREnvironment) {
        this.globalFrame = stateREnvironment.getGlobalFrame();
        this.startTime = System.nanoTime();
        if (context.getKind() == RContext.ContextKind.SHARE_NOTHING) {
            initializeShared();
        }
    }

    private void initializeShared() {
        suppressWarnings = true;
        MaterializedFrame baseFrame = RRuntime.createNonFunctionFrame().materialize();
        REnvironment.baseInitialize(baseFrame, globalFrame);
        loadBase = FastROptions.LoadBase;
        RBuiltinPackages.loadBase(baseFrame, loadBase);
        RGraphics.initialize();
        if (loadBase) {
            /*
             * eval the system/site/user profiles. Experimentally GnuR does not report warnings
             * during system profile evaluation, but does for the site/user profiles.
             */
            try {
                parseAndEval(RProfile.systemProfile(), baseFrame, false);
            } catch (ParseException e) {
                throw new RInternalError(e, "error while parsing system profile from %s", RProfile.systemProfile().getName());
            }
            checkAndRunStartupFunction(".OptRequireMethods");

            suppressWarnings = false;
            Source siteProfile = context.stateRProfile.siteProfile();
            if (siteProfile != null) {
                try {
                    parseAndEval(siteProfile, baseFrame, false);
                } catch (ParseException e) {
                    throw new RInternalError(e, "error while parsing site profile from %s", siteProfile.getName());
                }
            }
            Source userProfile = context.stateRProfile.userProfile();
            if (userProfile != null) {
                try {
                    parseAndEval(userProfile, globalFrame, false);
                } catch (ParseException e) {
                    throw new RInternalError(e, "error while parsing user profile from %s", userProfile.getName());
                }
            }
            if (!context.getOptions().getBoolean(RCmdOption.NO_RESTORE)) {
                /*
                 * TODO This is where we would load any saved user data
                 */
            }
            checkAndRunStartupFunction(".First");
            checkAndRunStartupFunction(".First.sys");
            RBuiltinPackages.loadDefaultPackageOverrides();
        }
    }

    private void checkAndRunStartupFunction(String name) {
        Object func = REnvironment.globalEnv().findFunction(name);
        if (func instanceof RFunction) {
            /*
             * We could just invoke runCall, but that way causes problems for debugging, so we parse
             * and eval a "fake" call.
             */
            RInstrument.checkDebugRequested(name, (RFunction) func);
            String call = name + "()";
            // Should this print the result?
            try {
                parseAndEval(Source.fromText(call, "<startup>"), globalFrame, false);
            } catch (ParseException e) {
                throw new RInternalError(e, "error while parsing startup function");
            }
        }
    }

    public void checkAndRunLast(String name) {
        checkAndRunStartupFunction(name);
    }

    public long elapsedTimeInNanos() {
        return System.nanoTime() - startTime;
    }

    public long[] childTimesInNanos() {
        return childTimes;
    }

    @Override
    public Object parseAndEval(Source source, MaterializedFrame frame, boolean printResult) throws ParseException {
        RSyntaxNode node = transform(parseImpl(source));
        RootCallTarget callTarget = doMakeCallTarget(node.asRNode(), "<repl wrapper>");
        try {
            return runCall(callTarget, frame, printResult, true);
        } catch (ReturnException ex) {
            return ex.getResult();
        } catch (DebugExitException | QuitException | BrowserQuitException e) {
            throw e;
        } catch (RError e) {
            // RError prints the correct result on the console during construction
            RInternalError.reportError(e);
            return null;
        } catch (UnsupportedSpecializationException use) {
            String message = "FastR internal error: Unsupported specialization in node " + use.getNode().getClass().getSimpleName() + " - supplied values: " +
                            Arrays.asList(use.getSuppliedValues()).stream().map(v -> v == null ? "null" : v.getClass().getSimpleName()).collect(Collectors.toList());
            context.getConsoleHandler().printErrorln(message);
            RInternalError.reportError(use);
            return null;
        } catch (Throwable t) {
            context.getConsoleHandler().printErrorln("FastR internal error: " + t.getMessage());
            RInternalError.reportError(t);
            return null;
        }
    }

    @Override
    public Object parseAndEval(Source source, boolean printResult) throws ParseException {
        return parseAndEval(source, globalFrame, printResult);
    }

    private static ASTNode parseImpl(Source source) throws ParseException {
        try {
            return ParseUtil.parseAST(new ANTLRStringStream(source.getCode()), source);
        } catch (RecognitionException e) {
            String line = e.line <= source.getLineCount() ? source.getCode(e.line) : "";
            String substring = line.substring(0, Math.min(line.length(), e.charPositionInLine + 1));
            String token = e.token.getText();
            if (e.token.getType() == Token.EOF && (e instanceof NoViableAltException || e instanceof MismatchedTokenException)) {
                // the parser got stuck at the eof, request another line
                throw new IncompleteSourceException(e, source, token, substring, e.line);
            } else {
                throw new ParseException(e, source, token, substring, e.line);
            }
        }
    }

    public RExpression parse(Source source) throws ParseException {
        Sequence seq = (Sequence) parseImpl(source);
        Object[] data = Arrays.stream(seq.getExpressions()).map(expr -> RDataFactory.createLanguage(transform(expr).asRNode())).toArray();
        return RDataFactory.createExpression(RDataFactory.createList(data));
    }

    public RFunction parseFunction(String name, Source source, MaterializedFrame enclosingFrame) throws ParseException {
        Sequence seq = (Sequence) parseImpl(source);
        ASTNode[] exprs = seq.getExpressions();
        assert exprs.length == 1;

        return new RTruffleVisitor().transformFunction(name, (Function) exprs[0], enclosingFrame);
    }

    @Override
    public CallTarget parseToCallTarget(Source source, boolean printResult) throws ParseException {
        ASTNode ast = parseImpl(source);
        return new TVMCallTarget(ast, printResult);
    }

    private static class TVMCallTarget implements RootCallTarget {

        private final ASTNode ast;
        private final boolean printResult;

        @SuppressWarnings("unchecked") @Child private FindContextNode<RContext> findContext = (FindContextNode<RContext>) TruffleRLanguage.INSTANCE.actuallyCreateFindContextNode();

        TVMCallTarget(ASTNode ast, boolean printResult) {
            this.ast = ast;
            this.printResult = printResult;
        }

        @Override
        public Object call(Object... arguments) {
            RSyntaxNode node = transform(ast);
            RootCallTarget callTarget = doMakeCallTarget(node.asRNode(), "<repl wrapper>");

            RContext oldContext = RContext.threadLocalContext.get();
            RContext context = findContext.executeFindContext();
            RContext.threadLocalContext.set(context);
            try {
                return ((REngine) context.getThisEngine()).runCall(callTarget, context.stateREnvironment.getGlobalFrame(), printResult, true);
            } finally {
                RContext.threadLocalContext.set(oldContext);
            }
        }

        public RootNode getRootNode() {
            return null;
        }
    }

    public Object eval(RExpression exprs, REnvironment envir, int depth) {
        Object result = RNull.instance;
        for (int i = 0; i < exprs.getLength(); i++) {
            Object obj = RASTUtils.checkForRSymbol(exprs.getDataAt(i));
            if (obj instanceof RLanguage) {
                result = evalNode((RNode) ((RLanguage) obj).getRep(), envir, depth);
            } else {
                result = obj;
            }
        }
        return result;
    }

    public Object eval(RLanguage expr, REnvironment envir, int depth) {
        return evalNode((RNode) expr.getRep(), envir, depth);
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
        // TODO perhaps this ought to be being checked earlier
        if (n instanceof ConstantNode) {
            return ((ConstantNode) n).getValue();
        }
        RootCallTarget callTarget = doMakeCallTarget(n, EVAL_FUNCTION_NAME);
        return runCall(callTarget, frame, false, false);
    }

    public Object evalFunction(RFunction func, Object... args) {
        ArgumentsSignature argsSig = ((RRootNode) func.getRootNode()).getSignature();
        MaterializedFrame frame = Utils.getActualCurrentFrame().materialize();
        Object[] rArgs = RArguments.create(func, frame == null ? null : RArguments.getCall(frame), frame, frame == null ? 1 : RArguments.getDepth(frame) + 1, args, argsSig, null);
        return func.getTarget().call(rArgs);
    }

    private Object evalNode(RNode exprRep, REnvironment envir, int depth) {
        RNode n = exprRep;
        RootCallTarget callTarget = doMakeCallTarget(n, EVAL_FUNCTION_NAME);
        RCaller call = RArguments.getCall(envir.getFrame());
        return evalTarget(callTarget, call, envir, depth);
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
    private Object evalTarget(RootCallTarget callTarget, RCaller call, REnvironment envir, int depth) {
        MaterializedFrame envFrame = envir.getFrame();
        // Here we create fake frame that wraps the original frame's context and has an only
        // slightly changed arguments array (function and callSrc).
        MaterializedFrame vFrame = VirtualEvalFrame.create(envFrame, (RFunction) null, call, depth);
        return runCall(callTarget, vFrame, false, false);
    }

    public Object evalPromise(Closure closure, MaterializedFrame frame) {
        return runCall(closure.getCallTarget(), frame, false, false);
    }

    /**
     * Transforms an AST produced by the parser into a Truffle AST.
     *
     * @param astNode parser AST instance
     * @return the root node of the Truffle AST
     */
    private static RSyntaxNode transform(ASTNode astNode) {
        return new RTruffleVisitor().transform(astNode);
    }

    @Override
    public RootCallTarget makePromiseCallTarget(Object bodyArg, String funName) {
        RNode body = (RNode) bodyArg;
        return doMakeCallTarget(body, funName);
    }

    /**
     * Creates an anonymous function, with no arguments to evaluate {@code body}. If {@body}
     * is a not a syntax node, uses a simple {@link BodyNode} with no source information. Otherwise
     * creates a {@link FunctionStatementsNode} using {@code body}. and ensures that the
     * {@link FunctionBodyNode} has a {@link SourceSection}, for instrumentation, although the
     * anonymous {@link FunctionDefinitionNode} itself does not need one.
     */
    private static RootCallTarget doMakeCallTarget(RNode body, String description) {
        BodyNode fbn;
        if (RBaseNode.isRSyntaxNode(body)) {
            RSyntaxNode synBody = (RSyntaxNode) body;
            RASTDeparse.ensureSourceSection(synBody);
            fbn = new FunctionBodyNode(SaveArgumentsNode.NO_ARGS, new FunctionStatementsNode(synBody.getSourceSection(), synBody));
        } else {
            fbn = new BodyNode(body);
        }
        FrameDescriptor descriptor = new FrameDescriptor();
        FrameSlotChangeMonitor.initializeFunctionFrameDescriptor(descriptor);
        FunctionDefinitionNode rootNode = new FunctionDefinitionNode(null, descriptor, fbn, FormalArguments.NO_ARGS, description, true, true, null);
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
     *
     * TODO This method is perhaps too generic for the different cases it handles, e.g. promise
     * evaluation, so the exception handling in particular is overly complex. Maybe it should be
     * refactored into separate methods to reflect the usages more precisely.
     */
    private Object runCall(RootCallTarget callTarget, MaterializedFrame frame, boolean printResult, boolean topLevel) {
        Object result = null;
        try {
            // FIXME: callTargets should only be called via Direct/IndirectCallNode
            result = callTarget.call(frame);
            assert checkResult(result);
            if (printResult && result != null) {
                assert topLevel;
                if (context.isVisible()) {
                    printResult(result);
                }
            }
            if (topLevel) {
                RErrorHandling.printWarnings(suppressWarnings);
            }
        } catch (RError e) {
            throw e;
        } catch (ReturnException ex) {
            // condition handling can cause a "return" that needs to skip over this call
            throw ex;
        } catch (BreakException | NextException cfe) {
            if (topLevel) {
                throw RError.error(RError.NO_NODE, RError.Message.NO_LOOP_FOR_BREAK_NEXT);
            } else {
                // there can be an outer loop
                throw cfe;
            }
        } catch (UnsupportedSpecializationException use) {
            throw use;
        } catch (DebugExitException | QuitException | BrowserQuitException e) {
            throw e;
        } catch (Throwable e) {
            reportImplementationError(e);
            if (e instanceof Error) {
                throw (Error) e;
            } else if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else {
                throw new RInternalError(e, "throwable caught while evaluating");
            }
        }
        return result;
    }

    @TruffleBoundary
    private static boolean checkResult(Object result) {
        if (FastROptions.CheckResultCompleteness && result instanceof RAbstractVector && ((RAbstractVector) result).isComplete()) {
            assert ((RAbstractVector) result).checkCompleteness() : "vector: " + result + " is not complete, but isComplete flag is true";
        }
        return true;
    }

    private static final ArgumentsSignature PRINT_SIGNATURE = ArgumentsSignature.get("x", "...");
    private static final ArgumentsSignature PRINT_INTERNAL_SIGNATURE = ArgumentsSignature.get("x");

    @TruffleBoundary
    public void printResult(Object result) {
        // this supports printing of non-R values (via toString for now)
        if (result instanceof TruffleObject && !(result instanceof RTypedValue)) {
            RContext.getInstance().getConsoleHandler().println(String.valueOf(result));
        } else if (result instanceof CharSequence && !(result instanceof String)) {
            RContext.getInstance().getConsoleHandler().println("\"" + String.valueOf(result) + "\"");
        } else {
            Object resultValue = result instanceof RPromise ? PromiseHelperNode.evaluateSlowPath(null, (RPromise) result) : result;
            if (loadBase) {
                Object printMethod = REnvironment.globalEnv().findFunction("print");
                RFunction function = (RFunction) (printMethod instanceof RPromise ? PromiseHelperNode.evaluateSlowPath(null, (RPromise) printMethod) : printMethod);
                if (FastROptions.NewStateTransition && resultValue instanceof RShareable && !((RShareable) resultValue).isSharedPermanent()) {
                    ((RShareable) resultValue).incRefCount();
                }
                function.getTarget().call(RArguments.create(function, null, REnvironment.globalEnv().getFrame(), 1, new Object[]{resultValue, RMissing.instance}, PRINT_SIGNATURE, null));
                if (FastROptions.NewStateTransition && resultValue instanceof RShareable && !((RShareable) resultValue).isSharedPermanent()) {
                    ((RShareable) resultValue).decRefCount();
                }
            } else {
                // we only have the .Internal print.default method available
                getPrintInternal().getTarget().call(RArguments.create(printInternal, null, REnvironment.globalEnv().getFrame(), 1, new Object[]{resultValue}, PRINT_INTERNAL_SIGNATURE, null));
            }
        }
    }

    // Only relevant when running without base package loaded
    private static final Source INTERNAL_PRINT = Source.fromText(".print.internal <- function(x) { .Internal(print.default(x, NULL, TRUE, NULL, NULL, FALSE, NULL, TRUE))}", "<internal_print>");
    @CompilationFinal private static RFunction printInternal;

    private RFunction getPrintInternal() {
        if (printInternal == null) {
            try {
                RExpression funDef = parse(INTERNAL_PRINT);
                printInternal = (RFunction) eval(funDef, REnvironment.baseEnv().getFrame());
            } catch (Engine.ParseException ex) {
                Utils.fail("failed to parse print.internal");
            }
        }
        return printInternal;

    }

    public Class<? extends TruffleLanguage<RContext>> getTruffleLanguage() {
        return TruffleRLanguage.class;
    }

    @TruffleBoundary
    private void reportImplementationError(Throwable e) {
        // R suicide, unless, e.g., we are running units tests.
        // We also don't call quit as the system is broken.
        if (crashOnFatalError) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            e.printStackTrace(new PrintStream(out));
            // We don't call writeStdErr as that may exercise the (broken) implementation
            context.getConsoleHandler().printErrorln(out.toString());
            Utils.exit(2);
        }
    }

    public ForeignAccess getForeignAccess(RTypedValue value) {
        if (value instanceof RAbstractVector) {
            return ForeignAccess.create(RAbstractVector.class, new RAbstractVectorAccessFactory());
        } else if (value instanceof RFunction) {
            return ForeignAccess.create(RFunction.class, new RFunctionAccessFactory());
        } else {
            throw RInternalError.shouldNotReachHere("cannot create ForeignAccess for " + value);
        }
    }
}
