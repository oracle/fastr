/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.antlr.runtime.MismatchedTokenException;
import org.antlr.runtime.NoViableAltException;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.Token;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.dsl.UnsupportedSpecializationException;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.impl.FindContextNode;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.NodeUtil;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.engine.interop.RAbstractVectorAccessFactory;
import com.oracle.truffle.r.engine.interop.RFunctionAccessFactory;
import com.oracle.truffle.r.engine.interop.RListAccessFactory;
import com.oracle.truffle.r.library.graphics.RGraphics;
import com.oracle.truffle.r.nodes.RASTBuilder;
import com.oracle.truffle.r.nodes.RASTUtils;
import com.oracle.truffle.r.nodes.RRootNode;
import com.oracle.truffle.r.nodes.access.ConstantNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinPackages;
import com.oracle.truffle.r.nodes.builtin.base.PrettyPrinterNode;
import com.oracle.truffle.r.nodes.control.BreakException;
import com.oracle.truffle.r.nodes.control.NextException;
import com.oracle.truffle.r.nodes.control.SequenceNode;
import com.oracle.truffle.r.nodes.function.BodyNode;
import com.oracle.truffle.r.nodes.function.FormalArguments;
import com.oracle.truffle.r.nodes.function.FunctionBodyNode;
import com.oracle.truffle.r.nodes.function.FunctionDefinitionNode;
import com.oracle.truffle.r.nodes.function.FunctionStatementsNode;
import com.oracle.truffle.r.nodes.function.PromiseHelperNode;
import com.oracle.truffle.r.nodes.function.SaveArgumentsNode;
import com.oracle.truffle.r.nodes.runtime.RASTDeparse;
import com.oracle.truffle.r.parser.RParser;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.BrowserQuitException;
import com.oracle.truffle.r.runtime.FastROptions;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RCaller;
import com.oracle.truffle.r.runtime.RCmdOptions.RCmdOption;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RErrorHandling;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RProfile;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.ReturnException;
import com.oracle.truffle.r.runtime.ThreadTimings;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.Utils.DebugExitException;
import com.oracle.truffle.r.runtime.VirtualEvalFrame;
import com.oracle.truffle.r.runtime.context.Engine;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RExpression;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RLanguage;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.data.RPromise.Closure;
import com.oracle.truffle.r.runtime.data.RShareable;
import com.oracle.truffle.r.runtime.data.RTypedValue;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.env.frame.FrameSlotChangeMonitor;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

/**
 * The engine for the FastR implementation. Handles parsing and evaluation. There is one instance of
 * this class per {@link RContext}.
 */
final class REngine implements Engine, Engine.Timings {

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
     * A temporary mechanism for suppressing warnings while evaluating the system profile, until the
     * proper mechanism is understood.
     */
    private boolean suppressWarnings;

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
        MaterializedFrame baseFrame = RRuntime.createNonFunctionFrame("base");
        REnvironment.baseInitialize(baseFrame, globalFrame);
        RBuiltinPackages.loadBase(baseFrame);
        RGraphics.initialize();
        /*
         * eval the system/site/user profiles. Experimentally GnuR does not report warnings during
         * system profile evaluation, but does for the site/user profiles.
         */
        try {
            parseAndEval(RProfile.systemProfile(), baseFrame, false);
        } catch (ParseException e) {
            throw new RInternalError(e, "error while parsing system profile from %s", RProfile.systemProfile().getName());
        }
        checkAndRunStartupShutdownFunction(".OptRequireMethods");

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
        if (!(context.getOptions().getBoolean(RCmdOption.NO_RESTORE) || context.getOptions().getBoolean(RCmdOption.NO_RESTORE_DATA))) {
            // call sys.load.image(".RData", RCmdOption.QUIET
            checkAndRunStartupShutdownFunction("sys.load.image", new String[]{"\".RData\"", context.getOptions().getBoolean(RCmdOption.QUIET) ? "TRUE" : "FALSE"});
            context.getConsoleHandler().setHistoryFrom(new File("./.Rhistory"));
        }
        checkAndRunStartupShutdownFunction(".First");
        checkAndRunStartupShutdownFunction(".First.sys");
        RBuiltinPackages.loadDefaultPackageOverrides();
    }

    public void checkAndRunStartupShutdownFunction(String name, String... args) {
        Object func = REnvironment.globalEnv().findFunction(name);
        if (func != null) {
            String call = name;
            if (args.length == 0) {
                call += "()";
            } else {
                call += "(";
                if (args.length > 0) {
                    for (int i = 0; i < args.length; i++) {
                        call += args[i];
                        if (i != args.length - 1) {
                            call += ", ";
                        }
                    }
                }
                call += ")";
            }
            // Should this print the result?
            try {
                parseAndEval(Source.fromText(call, "<startup/shutdown>"), globalFrame, false);
            } catch (ParseException e) {
                throw new RInternalError(e, "error while parsing startup function");
            }
        }
    }

    public Timings getTimings() {
        return this;
    }

    public long elapsedTimeInNanos() {
        return System.nanoTime() - startTime;
    }

    public long[] childTimesInNanos() {
        return childTimes;
    }

    public long[] userSysTimeInNanos() {
        return ThreadTimings.userSysTimeInNanos();
    }

    @Override
    public Object parseAndEval(Source source, MaterializedFrame frame, boolean printResult) throws ParseException {
        List<RSyntaxNode> list = parseImplDirect(source);
        SequenceNode sequence = new SequenceNode(list.toArray(new RNode[list.size()]));
        RootCallTarget callTarget = doMakeCallTarget(sequence, "<repl wrapper>");
        try {
            return runCall(callTarget, frame, printResult, true);
        } catch (ReturnException ex) {
            return ex.getResult();
        } catch (DebugExitException | BrowserQuitException e) {
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

    private static List<RSyntaxNode> parseImplDirect(Source source) throws ParseException {
        try {
            try {
                RParser<RSyntaxNode> parser = new RParser<>(source, new RASTBuilder());
                return parser.script();
            } catch (IllegalArgumentException e) {
                // the lexer will wrap exceptions in IllegalArgumentExceptions
                if (e.getCause() instanceof RecognitionException) {
                    throw (RecognitionException) e.getCause();
                } else {
                    throw e;
                }
            }
        } catch (RecognitionException e) {
            throw handleRecognitionException(source, e);
        }
    }

    private static ParseException handleRecognitionException(Source source, RecognitionException e) throws IncompleteSourceException, ParseException {
        String line = e.line <= source.getLineCount() ? source.getCode(e.line) : "";
        String substring = line.substring(0, Math.min(line.length(), e.charPositionInLine + 1));
        String token = e.token == null ? (substring.length() == 0 ? "" : substring.substring(substring.length() - 1)) : e.token.getText();
        if (e.token != null && e.token.getType() == Token.EOF && (e instanceof NoViableAltException || e instanceof MismatchedTokenException)) {
            // the parser got stuck at the eof, request another line
            throw new IncompleteSourceException(e, source, token, substring, e.line);
        } else {
            throw new ParseException(e, source, token, substring, e.line);
        }
    }

    public RExpression parse(Source source) throws ParseException {
        List<RSyntaxNode> list = parseImplDirect(source);
        Object[] data = list.stream().map(node -> RDataFactory.createLanguage(node.asRNode())).toArray();
        return RDataFactory.createExpression(RDataFactory.createList(data));
    }

    public RFunction parseFunction(String name, Source source, MaterializedFrame enclosingFrame) throws ParseException {
        RParser<RSyntaxNode> parser = new RParser<>(source, new RASTBuilder());
        try {
            return parser.root_function(name, enclosingFrame);
        } catch (RecognitionException e) {
            throw handleRecognitionException(source, e);
        }
    }

    @Override
    public CallTarget parseToCallTarget(Source source) throws ParseException {
        List<RSyntaxNode> list = parseImplDirect(source);
        SequenceNode sequence = new SequenceNode(list.toArray(new RNode[list.size()]));

        return Truffle.getRuntime().createCallTarget(new PolyglotEngineRootNode(sequence));
    }

    private static class PolyglotEngineRootNode extends RootNode {

        private final SequenceNode sequence;

        @SuppressWarnings("unchecked") @Child private FindContextNode<RContext> findContext = (FindContextNode<RContext>) TruffleRLanguage.INSTANCE.actuallyCreateFindContextNode();

        PolyglotEngineRootNode(SequenceNode sequence) {
            super(TruffleRLanguage.class, SourceSection.createUnavailable("repl", "<repl wrapper>"), new FrameDescriptor());
            this.sequence = sequence;
        }

        /**
         * The normal {@link #doMakeCallTarget} happens first, then we actually run the call using
         * the standard FastR machinery, saving and restoring the {@link RContext}, since we have no
         * control over what that might be when the call is initiated.
         */
        @Override
        public Object execute(VirtualFrame frame) {
            RootCallTarget callTarget = doMakeCallTarget(sequence, "<repl wrapper>");

            RContext oldContext = RContext.threadLocalContext.get();
            RContext context = findContext.executeFindContext();
            RContext.threadLocalContext.set(context);
            try {
                return ((REngine) context.getThisEngine()).runCall(callTarget, context.stateREnvironment.getGlobalFrame(), true, true);
            } catch (ReturnException ex) {
                return ex.getResult();
            } catch (DebugExitException | BrowserQuitException e) {
                throw e;
            } catch (RError e) {
                // TODO normal error reporting is done by the runtime
                RInternalError.reportError(e);
                return null;
            } catch (Throwable t) {
                throw t;
            } finally {
                RContext.threadLocalContext.set(oldContext);
            }
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

    public Object eval(RLanguage expr, MaterializedFrame frame) {
        RNode n = (RNode) expr.getRep();
        // TODO perhaps this ought to be being checked earlier
        if (n instanceof ConstantNode) {
            return ((ConstantNode) n).getValue();
        }
        RootCallTarget callTarget = doMakeCallTarget(n, EVAL_FUNCTION_NAME);
        return runCall(callTarget, frame, false, false);
    }

    public Object evalFunction(RFunction func, MaterializedFrame frame, Object... args) {
        ArgumentsSignature argsSig = ((RRootNode) func.getRootNode()).getSignature();
        MaterializedFrame actualFrame = frame == null ? Utils.getActualCurrentFrame().materialize() : frame;
        Object[] rArgs = RArguments.create(func, actualFrame == null ? null : RArguments.getCall(actualFrame), actualFrame, actualFrame == null ? 1 : RArguments.getDepth(actualFrame) + 1, args,
                        argsSig, null);
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

    public Object evalGeneric(RFunction func, MaterializedFrame frame) {
        FrameDescriptor descriptor = frame.getFrameDescriptor();
        FunctionDefinitionNode fdn = (FunctionDefinitionNode) func.getRootNode();
        FormalArguments formals = ((RRootNode) func.getRootNode()).getFormalArguments();
        FunctionDefinitionNode rootNode = new FunctionDefinitionNode(fdn.getSourceSection(), descriptor, NodeUtil.cloneNode(fdn.getBody()), formals, "GENERIC EVAL", true, true, null);
        RootCallTarget callTarget = Truffle.getRuntime().createCallTarget(rootNode);
        MaterializedFrame vFrame = VirtualEvalFrame.create(frame, func, RArguments.getCall(frame), RArguments.getDepth(frame));
        return runCall(callTarget, vFrame, false, false);
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
    @TruffleBoundary
    private static RootCallTarget doMakeCallTarget(RNode body, String description) {
        BodyNode fbn;
        SourceSection sourceSection = null;
        if (RBaseNode.isRSyntaxNode(body)) {
            RSyntaxNode synBody = (RSyntaxNode) body;
            RASTDeparse.ensureSourceSection(synBody);
            fbn = new FunctionBodyNode(SaveArgumentsNode.NO_ARGS, new FunctionStatementsNode(synBody.getSourceSection(), synBody));
            // SourceSection might be "unavailable", which has no code
            sourceSection = synBody.getSourceSection();
            if (sourceSection.getSource() != null) {
                String funPlusBody = "function() " + sourceSection.getCode();
                sourceSection = Source.fromText(funPlusBody, description).createSection("", 0, funPlusBody.length());
            }
        } else {
            fbn = new BodyNode(body);
        }
        FrameDescriptor descriptor = new FrameDescriptor();
        FrameSlotChangeMonitor.initializeFunctionFrameDescriptor("<eval>", descriptor);
        FunctionDefinitionNode rootNode = new FunctionDefinitionNode(sourceSection, descriptor, fbn, FormalArguments.NO_ARGS, description, true, true, null);
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
    private Object runCall(RootCallTarget callTarget, MaterializedFrame frame, boolean printResult, boolean topLevel) {
        Object result = null;
        try {
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
                throw RError.error(RError.SHOW_CALLER2, RError.Message.NO_LOOP_FOR_BREAK_NEXT);
            } else {
                // there can be an outer loop
                throw cfe;
            }
        } catch (DebugExitException | BrowserQuitException e) {
            throw e;
        } catch (Throwable e) {
            if (e instanceof Error) {
                throw (Error) e;
            } else if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else {
                /* This should never happen given the logic in FunctionDefinitionNode.execute */
                assert false;
            }
        }
        return result;
    }

    @TruffleBoundary
    private static boolean checkResult(Object result) {
        if (FastROptions.CheckResultCompleteness.getBooleanValue() && result instanceof RAbstractVector && ((RAbstractVector) result).isComplete()) {
            assert ((RAbstractVector) result).checkCompleteness() : "vector: " + result + " is not complete, but isComplete flag is true";
        }
        return true;
    }

    private static final ArgumentsSignature PRINT_SIGNATURE = ArgumentsSignature.get("x", "...");

    @TruffleBoundary
    public void printResult(Object result) {
        // this supports printing of non-R values (via toString for now)
        if (result == null || result instanceof TruffleObject && !(result instanceof RTypedValue)) {
            RContext.getInstance().getConsoleHandler().println(toString(result));
        } else if (result instanceof CharSequence && !(result instanceof String)) {
            RContext.getInstance().getConsoleHandler().println(toString(result));
        } else {
            Object resultValue = evaluatePromise(result);
            Object printMethod = REnvironment.globalEnv().findFunction("print");
            RFunction function = (RFunction) evaluatePromise(printMethod);
            if (FastROptions.NewStateTransition.getBooleanValue() && resultValue instanceof RShareable && !((RShareable) resultValue).isSharedPermanent()) {
                ((RShareable) resultValue).incRefCount();
            }
            function.getTarget().call(RArguments.create(function, null, REnvironment.globalEnv().getFrame(), 1, new Object[]{resultValue, RMissing.instance}, PRINT_SIGNATURE, null));
            if (FastROptions.NewStateTransition.getBooleanValue() && resultValue instanceof RShareable && !((RShareable) resultValue).isSharedPermanent()) {
                ((RShareable) resultValue).decRefCount();
            }
        }
    }

    public String toString(Object result) {
        // this supports printing of non-R values (via toString for now)
        if (result == null || (result instanceof TruffleObject && !(result instanceof RTypedValue))) {
            return "foreign()";
        } else if (result instanceof CharSequence && !(result instanceof String)) {
            return "[1] \"" + String.valueOf(result) + "\"";
        } else {
            Object resultValue = evaluatePromise(result);
            return PrettyPrinterNode.prettyPrintDefault(resultValue);
        }
    }

    private static Object evaluatePromise(Object value) {
        return value instanceof RPromise ? PromiseHelperNode.evaluateSlowPath(null, (RPromise) value) : value;
    }

    public Class<? extends TruffleLanguage<RContext>> getTruffleLanguage() {
        return TruffleRLanguage.class;
    }

    public ForeignAccess getForeignAccess(RTypedValue value) {
        if (value instanceof RList) {
            return ForeignAccess.create(RList.class, new RListAccessFactory());
        } else if (value instanceof RAbstractVector) {
            return ForeignAccess.create(RAbstractVector.class, new RAbstractVectorAccessFactory());
        } else if (value instanceof RFunction) {
            return ForeignAccess.create(RFunction.class, new RFunctionAccessFactory());
        } else {
            throw RInternalError.shouldNotReachHere("cannot create ForeignAccess for " + value);
        }
    }

}
