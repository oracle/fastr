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
import java.util.Map;
import java.util.stream.Collectors;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.UnsupportedSpecializationException;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.library.graphics.RGraphics;
import com.oracle.truffle.r.nodes.RASTBuilder;
import com.oracle.truffle.r.nodes.RASTUtils;
import com.oracle.truffle.r.nodes.access.ConstantNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinPackages;
import com.oracle.truffle.r.nodes.builtin.base.printer.ValuePrinterNode;
import com.oracle.truffle.r.nodes.control.BreakException;
import com.oracle.truffle.r.nodes.control.NextException;
import com.oracle.truffle.r.nodes.function.CallMatcherNode.CallMatcherGenericNode;
import com.oracle.truffle.r.nodes.function.PromiseHelperNode;
import com.oracle.truffle.r.nodes.function.call.CallRFunctionNode;
import com.oracle.truffle.r.nodes.function.visibility.GetVisibilityNode;
import com.oracle.truffle.r.nodes.function.visibility.SetVisibilityNode;
import com.oracle.truffle.r.nodes.instrumentation.RInstrumentation;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.ExitException;
import com.oracle.truffle.r.runtime.FastROptions;
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
import com.oracle.truffle.r.runtime.RStartParams.SA_TYPE;
import com.oracle.truffle.r.runtime.ReturnException;
import com.oracle.truffle.r.runtime.SubstituteVirtualFrame;
import com.oracle.truffle.r.runtime.ThreadTimings;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.Utils.DebugExitException;
import com.oracle.truffle.r.runtime.VirtualEvalFrame;
import com.oracle.truffle.r.runtime.context.Engine;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RExpression;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RLanguage;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.data.RShareable;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RTypedValue;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.env.frame.FrameSlotChangeMonitor;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxElement;
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

    @Override
    public void activate(REnvironment.ContextStateImpl stateREnvironment) {
        RInstrumentation.activate(context);
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
        if (FastROptions.LoadBase.getBooleanValue()) {
            /*
             * eval the system/site/user profiles. Experimentally GnuR does not report warnings
             * during system profile evaluation, but does for the site/user profiles.
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
            if (!(context.getStartParams().getRestoreAction() == SA_TYPE.NORESTORE)) {
                // call sys.load.image(".RData", RCmdOption.QUIET
                checkAndRunStartupShutdownFunction("sys.load.image", new String[]{"\".RData\"", context.getStartParams().getQuiet() ? "TRUE" : "FALSE"});
                context.getConsoleHandler().setHistoryFrom(new File("./.Rhistory"));
            }
            checkAndRunStartupShutdownFunction(".First");
            checkAndRunStartupShutdownFunction(".First.sys");
        }
    }

    @Override
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
                parseAndEval(RSource.fromTextInternal(call, RSource.Internal.STARTUP_SHUTDOWN), globalFrame, false);
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
        List<RSyntaxNode> list = parseImpl(null, source);
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

    private static List<RSyntaxNode> parseImpl(Map<String, Object> constants, Source source) throws ParseException {
        RParserFactory.Parser<RSyntaxNode> parser = RParserFactory.getParser();
        return parser.script(source, new RASTBuilder(constants));
    }

    @Override
    public RExpression parse(Map<String, Object> constants, Source source) throws ParseException {
        List<RSyntaxNode> list = parseImpl(constants, source);
        Object[] data = list.stream().map(node -> RASTUtils.createLanguageElement(node)).toArray();
        return RDataFactory.createExpression(RDataFactory.createList(data));
    }

    @Override
    public RFunction parseFunction(Map<String, Object> constants, String name, Source source, MaterializedFrame enclosingFrame) throws ParseException {
        RParserFactory.Parser<RSyntaxNode> parser = RParserFactory.getParser();
        RootCallTarget callTarget = parser.rootFunction(source, name, new RASTBuilder(constants));
        FrameSlotChangeMonitor.initializeEnclosingFrame(callTarget.getRootNode().getFrameDescriptor(), enclosingFrame);
        RFunction func = RDataFactory.createFunction(name, callTarget, null, enclosingFrame);
        RInstrumentation.checkDebugRequested(func);
        return func;
    }

    @Override
    public CallTarget parseToCallTarget(Source source) throws ParseException {
        List<RSyntaxNode> statements = parseImpl(null, source);
        return Truffle.getRuntime().createCallTarget(new PolyglotEngineRootNode(statements));
    }

    private final class PolyglotEngineRootNode extends RootNode {

        private final List<RSyntaxNode> statements;

        @Child private Node findContext = TruffleRLanguage.INSTANCE.actuallyCreateFindContextNode();

        PolyglotEngineRootNode(List<RSyntaxNode> statements) {
            super(TruffleRLanguage.class, SourceSection.createUnavailable("repl", RSource.Internal.REPL_WRAPPER.string), new FrameDescriptor());
            this.statements = statements;
        }

        /**
         * The normal {@link #doMakeCallTarget} happens first, then we actually run the call using
         * the standard FastR machinery, saving and restoring the {@link RContext}, since we have no
         * control over what that might be when the call is initiated.
         */
        @Override
        public Object execute(VirtualFrame frame) {
            RContext oldContext = RContext.getThreadLocalInstance();
            RContext newContext = TruffleRLanguage.INSTANCE.actuallyFindContext0(findContext);
            RContext.setThreadLocalInstance(newContext);
            try {
                Object lastValue = RNull.instance;
                for (int i = 0; i < statements.size(); i++) {
                    RSyntaxNode node = statements.get(i);
                    RootCallTarget callTarget = doMakeCallTarget(node.asRNode(), RSource.Internal.REPL_WRAPPER.string, true, true);
                    lastValue = callTarget.call(newContext.stateREnvironment.getGlobalFrame());
                }
                return lastValue;
            } catch (ReturnException ex) {
                return ex.getResult();
            } catch (DebugExitException | JumpToTopLevelException | ExitException | ThreadDeath e) {
                throw e;
            } catch (RError e) {
                CompilerDirectives.transferToInterpreter();
                throw e;
            } catch (Throwable t) {
                throw t;
            } finally {
                RContext.setThreadLocalInstance(oldContext);
            }
        }
    }

    @Override
    @TruffleBoundary
    public Object eval(RExpression exprs, REnvironment envir, RCaller caller) {
        Object result = RNull.instance;
        for (int i = 0; i < exprs.getLength(); i++) {
            Object obj = RASTUtils.checkForRSymbol(exprs.getDataAt(i));
            if (obj instanceof RLanguage) {
                result = evalNode(((RLanguage) obj).getRep().asRSyntaxNode(), envir, caller);
            } else {
                result = obj;
            }
        }
        return result;
    }

    @Override
    @TruffleBoundary
    public Object eval(RLanguage expr, REnvironment envir, RCaller caller) {
        return evalNode(expr.getRep().asRSyntaxNode(), envir, caller);
    }

    @Override
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

    @Override
    public Object eval(RLanguage expr, MaterializedFrame frame) {
        RNode n = (RNode) expr.getRep();
        // TODO perhaps this ought to be being checked earlier
        if (n instanceof ConstantNode) {
            return ((ConstantNode) n).getValue();
        }
        RootCallTarget callTarget = doMakeCallTarget(n, RSource.Internal.EVAL_WRAPPER.string, false, false);
        return callTarget.call(frame);
    }

    @Override
    @TruffleBoundary
    public Object evalFunction(RFunction func, MaterializedFrame frame, RCaller caller, RStringVector names, Object... args) {
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
        RArgsValuesAndNames reorderedArgs = CallMatcherGenericNode.reorderArguments(args, func,
                        names == null ? ArgumentsSignature.empty(args.length) : ArgumentsSignature.get(names.getDataWithoutCopying()), false,
                        RError.NO_CALLER);
        Object[] newArgs = reorderedArgs.getArguments();
        for (int i = 0; i < newArgs.length; i++) {
            Object arg = newArgs[i];
            if (arg instanceof RPromise) {
                newArgs[i] = PromiseHelperNode.evaluateSlowPath(null, (RPromise) arg);
            }
        }
        return CallRFunctionNode.executeSlowpath(func, caller == null ? RArguments.getCall(actualFrame) : caller, actualFrame, newArgs, null);
    }

    private Object evalNode(RSyntaxElement exprRep, REnvironment envir, RCaller caller) {
        // we need to copy the node, otherwise it (and its children) will specialized to a specific
        // frame descriptor and will fail on subsequent re-executions
        RSyntaxNode n = RContext.getASTBuilder().process(exprRep);
        RootCallTarget callTarget = doMakeCallTarget(n.asRNode(), RSource.Internal.EVAL_WRAPPER.string, false, false);
        return evalTarget(callTarget, caller, envir);
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
    private static Object evalTarget(RootCallTarget callTarget, RCaller call, REnvironment envir) {
        // Here we create fake frame that wraps the original frame's context and has an only
        // slightly changed arguments array (function and callSrc).
        MaterializedFrame vFrame = VirtualEvalFrame.create(envir.getFrame(), (RFunction) null, call);
        return callTarget.call(vFrame);
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
    private RootCallTarget doMakeCallTarget(RNode body, String description, boolean printResult, boolean topLevel) {
        return Truffle.getRuntime().createCallTarget(new AnonymousRootNode(body, description, printResult, topLevel));
    }

    /**
     * An instance of this node is called with the intention to have its execution leave a footprint
     * behind in a specific frame/environment, e.g., during library loading, commands from the
     * shell, or R's {@code eval} and its friends. The call target must be invoked with one
     * argument, namely the {@link Frame} to be side-effected. Execution will then proceed in the
     * context of that frame. Note that passing only this one frame argument, strictly spoken,
     * violates the frame layout as set forth in {@link RArguments}. This is for internal use only.
     */
    private final class AnonymousRootNode extends RootNode {

        private final ValueProfile frameTypeProfile = ValueProfile.createClassProfile();
        private final ConditionProfile isVirtualFrameProfile = ConditionProfile.createBinaryProfile();

        private final String description;
        private final boolean printResult;
        private final boolean topLevel;

        @Child private RNode body;
        @Child private GetVisibilityNode visibility = GetVisibilityNode.create();
        @Child private SetVisibilityNode setVisibility = SetVisibilityNode.create();

        protected AnonymousRootNode(RNode body, String description, boolean printResult, boolean topLevel) {
            super(TruffleRLanguage.class, null, new FrameDescriptor());
            this.body = body;
            this.description = description;
            this.printResult = printResult;
            this.topLevel = topLevel;
        }

        private VirtualFrame prepareFrame(VirtualFrame frame) {
            VirtualFrame vf;
            MaterializedFrame originalFrame = (MaterializedFrame) frameTypeProfile.profile(frame.getArguments()[0]);
            if (isVirtualFrameProfile.profile(originalFrame instanceof VirtualFrame)) {
                vf = (VirtualFrame) originalFrame;
            } else {
                vf = SubstituteVirtualFrame.create(originalFrame);
            }
            return vf;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            assert frame.getArguments().length == 1;
            VirtualFrame vf = prepareFrame(frame);
            Object result = null;
            try {
                result = body.execute(vf);
                assert checkResult(result);
                if (printResult && result != null) {
                    assert topLevel;
                    if (visibility.execute(vf, context)) {
                        printResult(result);
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
        public String toString() {
            return description;
        }

        @Override
        public boolean isCloningAllowed() {
            return false;
        }
    }

    @TruffleBoundary
    private static boolean checkResult(Object result) {
        if (FastROptions.CheckResultCompleteness.getBooleanValue() && result instanceof RAbstractVector && ((RAbstractVector) result).isComplete()) {
            assert ((RAbstractVector) result).checkCompleteness() : "vector: " + result + " is not complete, but isComplete flag is true";
        }
        return true;
    }

    @Override
    @TruffleBoundary
    public void printResult(Object originalResult) {
        Object result = evaluatePromise(originalResult);
        result = RRuntime.asAbstractVector(result);
        if (result instanceof RTypedValue) {
            Object resultValue = evaluatePromise(result);
            Object printMethod = REnvironment.globalEnv().findFunction("print");
            RFunction function = (RFunction) evaluatePromise(printMethod);
            if (resultValue instanceof RShareable && !((RShareable) resultValue).isSharedPermanent()) {
                ((RShareable) resultValue).incRefCount();
            }
            MaterializedFrame callingFrame = REnvironment.globalEnv().getFrame();
            CallRFunctionNode.executeSlowpath(function, RCaller.createInvalid(callingFrame), callingFrame, new Object[]{resultValue, RMissing.instance}, null);
            if (resultValue instanceof RShareable && !((RShareable) resultValue).isSharedPermanent()) {
                ((RShareable) resultValue).decRefCount();
            }
        } else {
            // this supports printing of non-R values (via toString for now)
            RContext.getInstance().getConsoleHandler().println(toString(result));
        }
    }

    private static String toString(Object originalResult) {
        Object result = evaluatePromise(originalResult);
        result = RRuntime.asAbstractVector(result);
        // this supports printing of non-R values (via toString for now)
        if (result instanceof RTypedValue) {
            return ValuePrinterNode.prettyPrint(result);
        } else if (result == null) {
            return "[external object (null)]";
        } else if (result instanceof TruffleObject) {
            assert !(result instanceof RTypedValue);
            return "[external object]";
        } else if (result instanceof CharSequence) {
            return "[1] \"" + String.valueOf(result) + "\"";
        } else {
            return String.valueOf(result);
        }
    }

    private static Object evaluatePromise(Object value) {
        return value instanceof RPromise ? PromiseHelperNode.evaluateSlowPath(null, (RPromise) value) : value;
    }
}
