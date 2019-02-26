/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.engine;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.dsl.UnsupportedSpecializationException;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.ExecutableNode;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.launcher.StartupTiming;
import com.oracle.truffle.r.library.graphics.RGraphics;
import com.oracle.truffle.r.nodes.RASTBuilder;
import com.oracle.truffle.r.nodes.RASTUtils;
import com.oracle.truffle.r.nodes.access.variables.ReadVariableNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinPackages;
import com.oracle.truffle.r.nodes.control.BreakException;
import com.oracle.truffle.r.nodes.control.NextException;
import com.oracle.truffle.r.nodes.function.CallMatcherNode.CallMatcherGenericNode;
import com.oracle.truffle.r.nodes.function.PromiseHelperNode;
import com.oracle.truffle.r.nodes.function.RCallerHelper;
import com.oracle.truffle.r.nodes.function.call.CallRFunctionNode;
import com.oracle.truffle.r.nodes.function.opt.ShareObjectNode;
import com.oracle.truffle.r.nodes.function.opt.UnShareObjectNode;
import com.oracle.truffle.r.nodes.function.visibility.GetVisibilityNode;
import com.oracle.truffle.r.nodes.function.visibility.SetVisibilityNode;
import com.oracle.truffle.r.nodes.instrumentation.RInstrumentation;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.ExitException;
import static com.oracle.truffle.r.runtime.context.FastROptions.LoadProfiles;
import com.oracle.truffle.r.runtime.JumpToTopLevelException;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RCaller;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RErrorHandling;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RParserFactory;
import com.oracle.truffle.r.runtime.RProfile;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RSource;
import com.oracle.truffle.r.runtime.ReturnException;
import com.oracle.truffle.r.runtime.RootBodyNode;
import com.oracle.truffle.r.runtime.RootWithBody;
import com.oracle.truffle.r.runtime.ThreadTimings;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.Utils.DebugExitException;
import com.oracle.truffle.r.runtime.context.Engine;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RExpression;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.data.RTypedValue;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.interop.R2Foreign;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
    private long[] childTimes;
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
        return new REngine(context);
    }

    @Override
    public void activate(REnvironment.ContextStateImpl stateREnvironment) {
        RInstrumentation.activate(context);
        this.globalFrame = stateREnvironment.getGlobalFrame();
        this.startTime = System.nanoTime();
        if (context.getKind() == RContext.ContextKind.SHARE_NOTHING) {
            initializeNonShared();
        }
        context.stateRNG.initializeDotRandomSeed(context);
    }

    private void initializeNonShared() {
        suppressWarnings = true;
        MaterializedFrame baseFrame = RRuntime.createNonFunctionFrame("base");
        REnvironment.baseInitialize(baseFrame, globalFrame);
        context.getStateRFFI().initializeVariables(context);
        RBuiltinPackages.loadBase(context.getLanguage(), baseFrame);
        RGraphics.initialize(context);
        if (context.getOption(LoadProfiles)) {
            StartupTiming.timestamp("Before Profiles Loaded");
            /*
             * eval the system/site/user profiles. Experimentally GnuR does not report warnings
             * during system profile evaluation, but does for the site/user profiles.
             */
            try {
                parseAndEval(RProfile.systemProfile(), baseFrame, false);
            } catch (ParseException e) {
                throw new RInternalError(e, "error while parsing system profile from %s", RProfile.systemProfile().getName());
            }
            checkAndRunStartupShutdownFunction(".OptRequireMethods", ".OptRequireMethods()");

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
            if (context.getStartParams().restore()) {
                // call sys.load.image(".RData", RCmdOption.QUIET
                checkAndRunStartupShutdownFunction("sys.load.image", "sys.load.image('.RData'," + (context.getStartParams().isQuiet() ? "TRUE" : "FALSE") + ')');
            }
            checkAndRunStartupShutdownFunction(".First", ".First()");
            checkAndRunStartupShutdownFunction(".First.sys", ".First.sys()");

            StartupTiming.timestamp("After Profiles Loaded");
        }
    }

    @Override
    public void checkAndRunStartupShutdownFunction(String name, String code) {
        // sanity check: code should be invocation of the function, so it should contain
        // "{name}(some-args)"
        assert code.contains("(") && code.contains(name);
        Object func = REnvironment.globalEnv().findFunction(name);
        if (func != null) {
            // Should this print the result?
            try {
                parseAndEval(RSource.fromTextInternal(code, RSource.Internal.STARTUP_SHUTDOWN), globalFrame, false);
            } catch (ParseException e) {
                throw new RInternalError(e, "error while parsing startup function");
            }
        }
    }

    @Override
    public Timings getTimings() {
        return this;
    }

    @Override
    public MaterializedFrame getGlobalFrame() {
        return globalFrame;
    }

    @Override
    public long elapsedTimeInNanos() {
        return System.nanoTime() - startTime;
    }

    @Override
    public long[] childTimesInNanos() {
        return childTimes;
    }

    @Override
    public long[] userSysTimeInNanos() {
        return ThreadTimings.userSysTimeInNanos();
    }

    @Override
    public Object parseAndEval(Source source, MaterializedFrame frame, boolean printResult) throws ParseException {
        List<RSyntaxNode> list = parseSource(source);
        try {
            Object lastValue = RNull.instance;
            for (RSyntaxNode node : list) {
                RootCallTarget callTarget = doMakeCallTarget(node.asRNode(), RSource.Internal.REPL_WRAPPER.string, printResult, true);
                lastValue = callTarget.call(frame);
            }
            return lastValue;
        } catch (ReturnException ex) {
            return ex.getResult();
        } catch (DebugExitException | JumpToTopLevelException | ExitException e) {
            throw e;
        } catch (RError e) {
            // RError prints the correct result on the console during construction
            return null;
        } catch (UnsupportedSpecializationException use) {
            String message = "FastR internal error: Unsupported specialization in node " + use.getNode().getClass().getSimpleName() + " - supplied values: " +
                            Arrays.asList(use.getSuppliedValues()).stream().map(v -> v == null ? "null" : v.getClass().getSimpleName()).collect(Collectors.toList());
            context.getConsole().printErrorln(message);
            RInternalError.reportError(use);
            return null;
        } catch (Throwable t) {
            context.getConsole().printErrorln("FastR internal error: " + t.getMessage());
            RInternalError.reportError(t);
            return null;
        }
    }

    private List<RSyntaxNode> parseSource(Source source) throws ParseException {
        RParserFactory.Parser<RSyntaxNode> parser = RParserFactory.getParser();
        return parser.script(source, new RASTBuilder(true), context.getLanguage());
    }

    @Override
    public ParsedExpression parse(Source source, boolean keepSource) throws ParseException {
        RParserFactory.Parser<RSyntaxNode> parser = RParserFactory.getParser();
        RASTBuilder builder = new RASTBuilder(true);
        List<RSyntaxNode> script = parser.script(source, builder, context.getLanguage());
        Object[] data = new Object[script.size()];
        for (int i = 0; i < script.size(); i++) {
            data[i] = RASTUtils.createLanguageElement(script.get(i));
        }
        return new ParsedExpression(RDataFactory.createExpression(data), builder.getParseData());
    }

    @Override
    public CallTarget parseToCallTarget(Source source, MaterializedFrame executionFrame) throws ParseException {
        if (source.getPath() != null && !source.isInteractive()) {
            // Use RScript semantics (delay syntax errors) for non-interactive sources from file
            return Truffle.getRuntime().createCallTarget(createRScriptRoot(source, executionFrame));
        } else if (source == Engine.GET_CONTEXT) {
            /*
             * The "get context" operations should be executed with as little influence on the
             * actual engine as possible, therefore this special case takes care of it explicitly.
             */
            return Truffle.getRuntime().createCallTarget(new RootNode(context.getLanguage()) {
                @Override
                public SourceSection getSourceSection() {
                    return source.createUnavailableSection();
                }

                @Override
                public Object execute(VirtualFrame frame) {
                    return context.getEnv().asGuestValue(context);
                }
            });
        } else {
            List<RSyntaxNode> statements = parseSource(source);
            EngineRootNode rootNode = EngineRootNode.createEngineRoot(this, context, statements, createSourceSection(source, statements), executionFrame, false);
            return Truffle.getRuntime().createCallTarget(rootNode);
        }
    }

    @Override
    public ExecutableNode parseToExecutableNode(Source source) throws ParseException {
        List<RSyntaxNode> list = parseSource(source);
        return new ExecutableNodeImpl(context.getLanguage(), list);
    }

    private final class ExecutableNodeImpl extends ExecutableNode {

        @Child R2Foreign toForeignNode = R2Foreign.create();
        @Children final RNode[] statements;

        private ExecutableNodeImpl(TruffleLanguage<?> language, List<RSyntaxNode> list) {
            super(language);
            statements = new RNode[list.size()];
            for (int i = 0; i < statements.length; i++) {
                statements[i] = list.get(i).asRNode();
            }
        }

        @Override
        @ExplodeLoop
        public Object execute(VirtualFrame frame) {
            if (statements.length == 0) {
                return RNull.instance;
            }
            for (int i = 0; i < statements.length - 1; i++) {
                statements[i].execute(frame);
            }
            return toForeignNode.execute(statements[statements.length - 1].execute(frame));
        }
    }

    private static SourceSection createSourceSection(Source source, List<RSyntaxNode> statements) {
        // All statements come from the same "Source"
        if (statements.isEmpty()) {
            return source.createSection(0, source.getLength());
        } else if (statements.size() == 1) {
            return statements.get(0).getSourceSection();
        } else {
            Source newSource = statements.get(0).getSourceSection().getSource();
            return newSource.createSection(0, statements.get(statements.size() - 1).getSourceSection().getCharEndIndex());
        }
    }

    private EngineRootNode createRScriptRoot(Source fullSource, MaterializedFrame frame) {
        URI uri = fullSource.getURI();
        String file = fullSource.getPath();
        ArrayList<RSyntaxNode> statements = new ArrayList<>(128);
        try {
            try (BufferedReader br = new BufferedReader(fullSource.getReader())) {
                int lineIndex = 1;
                int startLine = lineIndex;
                StringBuilder sb = new StringBuilder();
                String nextLineInput = br.readLine();
                ParseException lastParseException = null;
                while (true) {
                    String input = nextLineInput;
                    if (input == null) {
                        if (sb.length() != 0) {
                            // end of file, but not end of statement => error
                            statements.add(new SyntaxErrorNode(lastParseException, fullSource.createSection(startLine, 1, sb.length())));
                        }
                        break;
                    }
                    nextLineInput = br.readLine();
                    sb.append(input);
                    Source src = Source.newBuilder(RRuntime.R_LANGUAGE_ID, sb.toString(), file + "#" + startLine + "-" + lineIndex).uri(uri).build();
                    lineIndex++;
                    List<RSyntaxNode> currentStmts = null;
                    try {
                        RParserFactory.Parser<RSyntaxNode> parser = RParserFactory.getParser();
                        currentStmts = parser.statements(src, fullSource, startLine, new RASTBuilder(true), context.getLanguage());
                    } catch (IncompleteSourceException e) {
                        lastParseException = e;
                        if (nextLineInput != null) {
                            sb.append('\n');
                        }
                        continue;
                    } catch (ParseException e) {
                        statements.add(new SyntaxErrorNode(e, fullSource.createSection(startLine, 1, sb.length())));
                    }
                    if (currentStmts != null) {
                        statements.addAll(currentStmts);
                    }
                    // we did not continue on incomplete source exception
                    sb.setLength(0);
                    startLine = lineIndex;
                }
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        return EngineRootNode.createEngineRoot(this, context, statements, createSourceSection(fullSource, statements), frame, true);
    }

    @Override
    @TruffleBoundary
    public Object eval(RExpression exprs, REnvironment envir, Object callerFrame, RCaller caller, RFunction function) {
        Object result = RNull.instance;
        for (int i = 0; i < exprs.getLength(); i++) {
            Object obj = exprs.getDataAt(i);
            if (obj instanceof RSymbol) {
                String identifier = ((RSymbol) obj).getName();
                result = ReadVariableNode.lookupAny(identifier, envir.getFrame(), false);
                caller.setVisibility(true);
                if (result == null) {
                    throw RError.error(RError.SHOW_CALLER, RError.Message.ARGUMENT_MISSING, identifier);
                }
            } else if ((obj instanceof RPairList && ((RPairList) obj).isLanguage())) {
                result = eval((RPairList) obj, envir, callerFrame, caller, function);
            } else {
                result = obj;
            }
        }
        return result;
    }

    @Override
    @TruffleBoundary
    public Object eval(RPairList expr, REnvironment envir, Object callerFrame, RCaller caller, RFunction function) {
        assert expr.isLanguage();
        return expr.getClosure().eval(envir, callerFrame, caller, function);
    }

    @Override
    public Object eval(RExpression expr, MaterializedFrame frame) {
        CompilerAsserts.neverPartOfCompilation();
        Object result = null;
        for (int i = 0; i < expr.getLength(); i++) {
            result = expr.getDataAt(i);
            if ((result instanceof RPairList && ((RPairList) result).isLanguage())) {
                RPairList lang = (RPairList) result;
                result = eval(lang, frame);
            }
        }
        return result;
    }

    @Override
    public Object eval(RPairList expr, MaterializedFrame frame) {
        assert expr.isLanguage();
        CompilerAsserts.neverPartOfCompilation();
        return expr.getClosure().eval(frame);
    }

    @Override
    @TruffleBoundary
    public Object evalFunction(RFunction func, MaterializedFrame frame, RCaller caller, boolean evalPromises, ArgumentsSignature names, Object... args) {
        assert frame == null || caller != null;
        MaterializedFrame actualFrame = frame;
        if (actualFrame == null) {
            Frame current = Utils.getActualCurrentFrame();
            if (current == null || !RArguments.isRFrame(current)) {
                // special case, e.g. in parser and an error is thrown
                actualFrame = REnvironment.globalEnv().getFrame();
            } else {
                actualFrame = current.materialize();
            }
        }
        ArgumentsSignature argsSignature = names == null ? ArgumentsSignature.empty(args.length) : names;
        RArgsValuesAndNames reorderedArgs = CallMatcherGenericNode.reorderArguments(args, func,
                        argsSignature, RError.NO_CALLER);
        Object[] newArgs = reorderedArgs.getArguments();
        if (evalPromises) {
            for (int i = 0; i < newArgs.length; i++) {
                Object arg = newArgs[i];
                if (arg instanceof RPromise) {
                    newArgs[i] = PromiseHelperNode.evaluateSlowPath((RPromise) arg);
                }
            }
        }
        RCaller rCaller = caller == null ? RCaller.create(actualFrame, RCallerHelper.createFromArguments(func, new RArgsValuesAndNames(args, argsSignature))) : caller;
        return CallRFunctionNode.executeSlowpath(func, rCaller, actualFrame, newArgs, reorderedArgs.getSignature(), null);
    }

    @Override
    public RootCallTarget makePromiseCallTarget(RNode body, String funName) {
        return doMakeCallTarget(body, funName, false, false);
    }

    /**
     * Creates an anonymous function, with no arguments to evaluate {@code body}, optionally
     * printing any result. The {@code callTarget} expects exactly one argument: the {@code frame}
     * that the body should be executed in.
     */
    @TruffleBoundary
    RootCallTarget doMakeCallTarget(RNode body, String description, boolean printResult, boolean topLevel) {
        return Truffle.getRuntime().createCallTarget(new AnonymousRootNode(this, body, description, printResult, topLevel));
    }

    /**
     * An instance of this node is called with the intention to have its execution leave a footprint
     * behind in a specific frame/environment, e.g., during library loading, commands from the
     * shell, or R's {@code eval} and its friends. The call target must be invoked with one
     * argument, namely the {@link Frame} to be side-effected. Execution will then proceed in the
     * context of that frame. Note that passing only this one frame argument, strictly spoken,
     * violates the frame layout as set forth in {@link RArguments}. This is for internal use only.
     */
    private static final class AnonymousRootNode extends RootNode implements RootWithBody {

        private final ValueProfile frameTypeProfile = ValueProfile.createClassProfile();

        private final String description;
        private final boolean printResult;
        private final boolean topLevel;
        private final boolean suppressWarnings;

        @Child private RootBodyNode body;
        @Child private GetVisibilityNode visibility = GetVisibilityNode.create();
        @Child private SetVisibilityNode setVisibility = SetVisibilityNode.create();

        protected AnonymousRootNode(REngine engine, RNode body, String description, boolean printResult, boolean topLevel) {
            super(engine.context.getLanguage());
            this.suppressWarnings = engine.suppressWarnings;
            this.body = new AnonymousBodyNode(body);
            this.description = description;
            this.printResult = printResult;
            this.topLevel = topLevel;
        }

        @Override
        public SourceSection getSourceSection() {
            return getBody().getSourceSection();
        }

        @Override
        public boolean isInternal() {
            return RSyntaxNode.isInternal(getBody().getLazySourceSection());
        }

        private VirtualFrame prepareFrame(VirtualFrame frame) {
            return (MaterializedFrame) frameTypeProfile.profile(frame.getArguments()[0]);
        }

        @Override
        public Object execute(VirtualFrame frame) {
            assert frame.getArguments().length == 1;
            VirtualFrame vf = prepareFrame(frame);
            Object result = null;
            try {
                result = body.visibleExecute(vf);
                assert checkResult(result);
                if (printResult && result != null) {
                    assert topLevel;
                    if (visibility.execute(vf)) {
                        printResultImpl(RContext.getInstance(), result);
                    }
                }
                if (topLevel) {
                    RErrorHandling.printWarnings(suppressWarnings);
                }
                setVisibility.executeEndOfFunction(vf);
            } catch (RError e) {
                CompilerDirectives.transferToInterpreter();
                throw e;
            } catch (ReturnException ex) {
                CompilerDirectives.transferToInterpreter();
                // condition handling can cause a "return" that needs to skip over this call
                throw ex;
            } catch (BreakException | NextException cfe) {
                if (topLevel) {
                    CompilerDirectives.transferToInterpreter();
                    throw RError.error(RError.SHOW_CALLER2, RError.Message.NO_LOOP_FOR_BREAK_NEXT);
                } else {
                    // there can be an outer loop
                    throw cfe;
                }
            } catch (DebugExitException | JumpToTopLevelException | ExitException e) {
                CompilerDirectives.transferToInterpreter();
                throw e;
            } catch (Throwable e) {
                CompilerDirectives.transferToInterpreter();
                if (e instanceof Error) {
                    throw (Error) e;
                } else if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                } else {
                    assert false : "unexpected exception: " + e;
                }
            }
            return result;
        }

        @Override
        public String getName() {
            return description;
        }

        @Override
        public String toString() {
            return description;
        }

        @Override
        public boolean isCloningAllowed() {
            return false;
        }

        @Override
        public RSyntaxNode getBody() {
            return body.getBody().asRSyntaxNode();
        }
    }

    private static final class AnonymousBodyNode extends Node implements RootBodyNode {
        @Child private RNode body;

        AnonymousBodyNode(RNode body) {
            this.body = body;
        }

        @Override
        public Object visibleExecute(VirtualFrame frame) {
            return body.visibleExecute(frame);
        }

        @Override
        public SourceSection getSourceSection() {
            return body.getSourceSection();
        }

        @Override
        public RNode getBody() {
            return body;
        }
    }

    @TruffleBoundary
    private static boolean checkResult(Object result) {
        if (result instanceof RAbstractVector) {
            return RAbstractVector.verify((RAbstractVector) result);
        }
        return true;
    }

    @Override
    public void printResult(RContext ctx, Object originalResult) {
        printResultImpl(ctx, originalResult);
    }

    @TruffleBoundary
    static void printResultImpl(RContext ctx, Object originalResult) {
        Object result = evaluatePromise(originalResult);
        result = RRuntime.asAbstractVector(result);
        MaterializedFrame callingFrame = REnvironment.globalEnv(ctx).getFrame();
        printValue(ctx, callingFrame, result);
    }

    private static void printValue(RContext ctx, MaterializedFrame callingFrame, Object result) {
        if (result instanceof RTypedValue || result instanceof TruffleObject) {
            Object resultValue = ShareObjectNode.share(evaluatePromise(result));
            if (result instanceof RAttributable && ((RAttributable) result).isS4()) {
                Object printMethod = REnvironment.getRegisteredNamespace(ctx, "methods").get("show");
                RFunction function = (RFunction) evaluatePromise(printMethod);
                CallRFunctionNode.executeSlowpath(function, RCaller.createInvalid(callingFrame), callingFrame, new Object[]{resultValue}, null);
            } else {
                Object printMethod = REnvironment.globalEnv().findFunction("print");
                RFunction function = (RFunction) evaluatePromise(printMethod);
                CallRFunctionNode.executeSlowpath(function, RCaller.createInvalid(callingFrame), callingFrame, new Object[]{resultValue, RArgsValuesAndNames.EMPTY}, null);
            }
            UnShareObjectNode.unshare(resultValue);
        } else {
            // this supports printing of non-R values (via toString for now)
            String str;
            if (result == null) {
                str = "[polyglot value (null)]";
            } else if (result instanceof CharSequence) {
                str = "[1] \"" + String.valueOf(result) + "\"";
            } else {
                str = String.valueOf(result);
            }
            RContext.getInstance().getConsole().println(str);
        }
    }

    private static Object evaluatePromise(Object value) {
        return value instanceof RPromise ? PromiseHelperNode.evaluateSlowPath((RPromise) value) : value;
    }
}
