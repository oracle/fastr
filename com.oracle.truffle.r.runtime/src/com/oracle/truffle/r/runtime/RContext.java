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
package com.oracle.truffle.r.runtime;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.api.vm.*;
import com.oracle.truffle.r.runtime.conn.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.env.*;
import com.oracle.truffle.r.runtime.env.REnvironment.*;
import com.oracle.truffle.r.runtime.ffi.*;
import com.oracle.truffle.r.runtime.rng.*;

/**
 * Encapsulates the runtime state ("context") of an R session. All access to that state from the
 * implementation <b>must</b> go through this class. There can be multiple instances
 * (multiple-tenancy) active within a single process/Java-VM.
 *
 * The context provides two sub-interfaces {@link ConsoleHandler} and {@link Engine}that are
 * (typically) implemented elsewhere, and accessed through {@link #getConsoleHandler()} and
 * {@link #getEngine()}, respectively.
 *
 * Context-specific state for implementation classes is managed by this class (or the associated
 * engine) and accessed through the {@code getXXXState} methods.
 *
 * The life-cycle of a {@link RContext} is:
 * <ol>
 * <li>created: {@link #createShareNothing(RContext, String[], ConsoleHandler)} or
 * {@link #createShareParentReadOnly(RContext, String[], ConsoleHandler)}</li>
 * <li>activated: {@link #activate()}</li>
 * <li>destroyed: {@link #destroy()}</li>
 * </ol>
 *
 * Evaluations are only possible on an active context.
 *
 */
public final class RContext extends ExecutionContext {

    public static final int CONSOLE_WIDTH = 80;

    /**
     * The interface to a source of input/output for the context, which may have different
     * implementations for different contexts. Since I/O is involved, all methods are tagged with
     * {@link TruffleBoundary} as a hint that so should the associated implementation methods.
     */
    public interface ConsoleHandler {
        /**
         * Normal output with a new line.
         */
        @TruffleBoundary
        void println(String s);

        /**
         * Normal output without a newline.
         */
        @TruffleBoundary
        void print(String s);

        /**
         * Formatted output.
         */
        @TruffleBoundary
        default void printf(String format, Object... args) {
            print(String.format(format, args));
        }

        /**
         * Error output with a newline.
         *
         * @param s
         */
        @TruffleBoundary
        void printErrorln(String s);

        /**
         * Error output without a newline.
         */
        @TruffleBoundary
        void printError(String s);

        /**
         * Read a line of input, newline included in result. Returns null if
         * {@link #isInteractive() == false}. The rationale for including the readline is to ensure
         * that the accumulated input, whether it be from a file or the console accurately reflects
         * the the source. TODO worry about "\r\n"?
         */
        @TruffleBoundary
        String readLine();

        /**
         * Return {@code true} if and only if this console is interactive.
         */
        @TruffleBoundary
        boolean isInteractive();

        /**
         * Redirect error output to the normal output.
         */
        @TruffleBoundary
        void redirectError();

        /**
         * Get the current prompt.
         */
        @TruffleBoundary
        String getPrompt();

        /**
         * Set the R prompt.
         */
        @TruffleBoundary
        void setPrompt(String prompt);

        /**
         * Get the console width.
         */
        @TruffleBoundary
        int getWidth();
    }

    public enum Kind {
        /**
         * Essentially a clean restart, modulo the basic VM-wide initialization. which does include,
         * for example, reading the external environment variables. I.e., it is not a goal to create
         * a multi-user environment with different external inputs. This kind of context can be used
         * in a parallel computation. The initial context is always of this kind. The intent is that
         * all mutable state is localized to the context, which may not be completely achievable.
         * For example, shared native libraries are assumed to contain no mutable state and be
         * re-entrant.
         */
        SHARE_NOTHING,

        /**
         * Shares the set of loaded packages of the parent context at the time the context is
         * created. Only useful when there is a priori knowledge on the evaluation that the context
         * will be used for. Cannot safely be used for parallel context evaluation. Must be created
         * as a child of an existing parent context of type {@link #SHARE_NOTHING} or
         * {@link #SHARE_PARENT_RO} and only one such child is allowed. (Strictly speaking the
         * invariant should be only one active child, but the implementation enforces it at creation
         * time). Evidently any changes made to the shared environment, e.g., loading a package,
         * affect the parent.
         */
        SHARE_PARENT_RW,

        /**
         * Intermediate between {@link #SHARE_NOTHING} and {@link #SHARE_PARENT_RW}, this is similar
         * to the standard shared code/copied data model provided by operating systems, although the
         * code/data distinction isn't completely applicable to a language like R. Unlike
         * {@link #SHARE_NOTHING}, where the ASTs for the functions in the default packages are
         * distinct copies in each context, in this kind of context, they are shared. Strictly
         * speaking, the bindings of R functions are shared, and this is achieved by creating a
         * shallow copy of the environments associated with the default packages of the parent
         * context at the time the context is created.
         */
        SHARE_PARENT_RO,
    }

    /**
     * Denotes a class that has context-specific state, e.g. {@link REnvironment}. Such a class must
     * implement the {@link #newContext} method.
     */
    public interface StateFactory {
        /**
         * Create the class-specific state for a new context.
         *
         * @param context the context
         * @param objects additional arguments for custom initialization, typically empty
         */
        ContextState newContext(RContext context, Object... objects);

        /**
         * A state factory may want to snapshot the state of the context just after the basic system
         * is initialized, in which case they can override this method. N.B. This is only invoked
         * for {@link Kind#SHARE_NOTHING} contexts. The definition of "system initialized" is that
         * the default packages have been loaded, profiles evaluated and {@code .First, First.Sys}
         * executed.
         */
        @SuppressWarnings("unused")
        default void systemInitialized(RContext context, ContextState state) {

        }

        /**
         * Called in response to the {@link RContext#destroy} method. Provides a hook for finalizing
         * any state before the context is destroyed.
         */
        @SuppressWarnings("unused")
        default void beforeDestroy(RContext context, ContextState state) {

        }
    }

    /**
     * Tagging interface denoting a class that carries the context-specific state for a class that
     * implements {@link StateFactory}. The class specific state must implement this interface.
     */
    public interface ContextState {

    }

    public interface Engine {
        /**
         * Make the engine ready for evaluations.
         */
        void activate();

        /**
         * Return the {@link TruffleVM} instance associated with this engine.
         */
        TruffleVM getTruffleVM();

        /**
         * Return the {@link com.oracle.truffle.api.vm.TruffleVM.Builder} instance associated with
         * this engine. This is only used by the command line debugger.
         */
        TruffleVM.Builder getTruffleVMBuilder();

        /**
         * Elapsed time of runtime.
         *
         * @return elapsed time in nanosecs.
         */
        long elapsedTimeInNanos();

        /**
         * Return user and system times for any spawned child processes in nanosecs, < 0 means not
         * available (Windows).
         */
        long[] childTimesInNanos();

        public static class ParseException extends Exception {
            private static final long serialVersionUID = 1L;

            public ParseException(Throwable cause, String msg) {
                super(msg, cause);
            }
        }

        /**
         * Parse an R expression and return an {@link RExpression} object representing the Truffle
         * ASTs for the components.
         */
        RExpression parse(Source source) throws ParseException;

        /**
         * A (perhaps temporary) interface to support {@link TruffleLanguage}.
         */
        CallTarget parseToCallTarget(Source source);

        /**
         * Parse and evaluate {@code rscript} in {@code frame}. {@code printResult == true}, the
         * result of the evaluation is printed to the console.
         *
         * @param sourceDesc a {@link Source} object that describes the input to be parsed
         * @param frame the frame in which to evaluate the input
         * @param printResult {@code true} iff the result of the evaluation should be printed to the
         *            console
         * @param allowIncompleteSource {@code true} if partial input is acceptable
         * @return the object returned by the evaluation or {@code null} if an error occurred.
         */
        Object parseAndEval(Source sourceDesc, MaterializedFrame frame, boolean printResult, boolean allowIncompleteSource);

        /**
         * Variant of {@link #parseAndEval(Source, MaterializedFrame, boolean, boolean)} for
         * evaluation in the global frame.
         */
        Object parseAndEval(Source sourceDesc, boolean printResult, boolean allowIncompleteSource);

        /**
         * Variant of {@link #parseAndEval(Source, MaterializedFrame, boolean, boolean)} that does
         * not intercept errors.
         */
        Object parseAndEvalDirect(Source sourceDesc, boolean printResult, boolean allowIncompleteSource);

        Object INCOMPLETE_SOURCE = new Object();

        /**
         * Support for the {@code eval} {@code .Internal}.
         */
        Object eval(RExpression expr, REnvironment envir, REnvironment enclos, int depth) throws PutException;

        /**
         * Convenience method for common case.
         */
        default Object eval(RExpression expr, REnvironment envir, int depth) throws PutException {
            return eval(expr, envir, null, depth);
        }

        /**
         * Variant of {@link #eval(RExpression, REnvironment, REnvironment, int)} for a single
         * language element.
         */
        Object eval(RLanguage expr, REnvironment envir, REnvironment enclos, int depth) throws PutException;

        /**
         * Convenience method for common case.
         */
        default Object eval(RLanguage expr, REnvironment envir, int depth) throws PutException {
            return eval(expr, envir, null, depth);
        }

        /**
         * Evaluate {@code expr} in {@code frame}.
         */
        Object eval(RExpression expr, MaterializedFrame frame);

        /**
         * Variant of {@link #eval(RExpression, MaterializedFrame)} for a single language element.
         */
        Object eval(RLanguage expr, MaterializedFrame frame);

        /**
         * Variant of {@link #eval(RLanguage, MaterializedFrame)} where we already have the
         * {@link RFunction} and the evaluated arguments, but do not have a frame available, and we
         * are behind a {@link TruffleBoundary}, so call inlining is not an issue. This is primarily
         * used for R callbacks from {@link RErrorHandling} and {@link RSerialize}.
         */
        Object evalFunction(RFunction func, Object... args);

        /**
         * Evaluates an {@link com.oracle.truffle.r.runtime.data.RPromise.Closure} in {@code frame}.
         */
        Object evalPromise(RPromise.Closure closure, MaterializedFrame frame);

        /**
         * Checks for the existence of {@code .Last/.Last.sys} and if present and bound to a
         * function, invokes the (parameterless) function.
         */
        void checkAndRunLast(String name);

        /**
         * Wraps the Truffle AST in {@code body} in an anonymous function and returns a
         * {@link RootCallTarget} for it.
         *
         * N.B. For certain expressions, there might be some value in enclosing the wrapper function
         * in a specific lexical scope. E.g., as a way to access names in the expression known to be
         * defined in that scope.
         *
         * @param body The AST for the body of the wrapper, i.e., the expression being evaluated.
         */
        RootCallTarget makePromiseCallTarget(Object body, String funName);

        /**
         * Used by Truffle debugger; invokes the internal "print" support in R for {@code value}.
         * Essentially this is equivalent to {@link #evalFunction} using the {@code "print"}
         * function.
         */
        void printResult(Object value);

        RFunction parseFunction(String name, Source source, MaterializedFrame enclosingFrame) throws ParseException;

    }

    /**
     * The set of classes for which the context manages context-specific state, and their state. We
     * could do this more dynamically with a registration process, perhaps driven by an annotation
     * processor, but the set is relatively small, so we just enumerate them here.
     */
    public static enum ClassStateKind {
        ROptions(ROptions.class, false),
        REnvironment(REnvironment.ClassStateFactory.class, true),
        RErrorHandling(RErrorHandling.class, false),
        RConnection(ConnectionSupport.class, false),
        StdConnections(StdConnections.class, true),
        RNG(RRNG.class, false),
        RFFI(RFFIContextStateFactory.class, false),
        RSerialize(RSerialize.class, false),
        REnvVars(REnvVars.class, false);

        private final Class<? extends StateFactory> klass;
        private StateFactory factory;
        private final boolean customCreate;

        private ClassStateKind(Class<? extends StateFactory> klass, boolean customCreate) {
            this.klass = klass;
            this.customCreate = customCreate;
        }

        private static final ClassStateKind[] VALUES = values();
        // Avoid reflection at runtime (AOT VM).
        static {
            for (ClassStateKind css : VALUES) {
                try {
                    css.factory = css.klass.newInstance();
                } catch (IllegalAccessException | InstantiationException ex) {
                    throw Utils.fail("failed to instantiate ClassStateFactory: " + css.klass.getSimpleName());
                }
            }
        }

    }

    /**
     * A thread that is explicitly associated with a context for efficient lookup.
     */
    public static class ContextThread extends Thread {
        protected RContext context;

        public ContextThread(RContext context) {
            this.context = context;
        }

        protected ContextThread() {

        }

        public void setContext(RContext context) {
            this.context = context;
        }

    }

    /**
     * A thread for performing an evaluation (used by {@code fastr} package.
     */
    public static class EvalThread extends ContextThread {
        private final Source source;

        public EvalThread(RContext context, Source source) {
            super(context);
            this.source = source;
            context.evalThread = this;
        }

        @Override
        public void run() {
            try {
                context.activate();
                context.engine.parseAndEval(source, true, false);
            } finally {
                context.destroy();
            }
        }

    }

    /**
     * Builtin cache. Valid across all contexts.
     */
    private static final HashMap<Object, RFunction> cachedBuiltinFunctions = new HashMap<>();

    private final Kind kind;

    private final GlobalAssumptions globalAssumptions = new GlobalAssumptions();

    /**
     * Denote whether the result of an expression should be printed in the shell or not.
     */
    private boolean resultVisible = true;

    /**
     * A context-specific value that is checked in {@code HiddenInternalFunctions} to avoid an error
     * report on a {@code SUBSTITUTE} builtin. Not worth promoting to a {@link ContextState}.
     */
    private boolean loadingBase;

    /**
     * Denote whether the FastR instance is running in 'interactive' mode. This can be set in a
     * number of ways and is <b>not</> simply equivalent to taking input from a file. However, it is
     * final once set.
     */
    @CompilationFinal private boolean interactive;

    @CompilationFinal private ConsoleHandler consoleHandler;
    @CompilationFinal private String[] commandArgs;
    @CompilationFinal private Engine engine;

    /**
     * The array is indexed by {@link ClassStateKind#ordinal()}.
     */
    @CompilationFinal private ContextState[] contextState;

    /**
     * Any context created by another has a parent. When such a context is destroyed we must reset
     * the {@link #threadLocalContext} to the parent.
     */
    private final RContext parent;

    /**
     * At most one shared child.
     */
    private RContext sharedChild;

    /**
     * Back pointer to the evalThread.
     */
    private EvalThread evalThread;

    /**
     * Typically there is a 1-1 relationship between an {@link RContext} and the thread that is
     * performing the evaluation, so we can store the {@link RContext} in a {@link ThreadLocal}.
     *
     * When a context is first created no threads are attached, to allow contexts to be used as
     * values in the experimental {@code fastr.createcontext} function. The {@link #engine} must
     * call the {@link #activate} method once the context becomes active. Additional threads can be
     * added by the {@link #attachThread} method.
     */
    @CompilationFinal private static final ThreadLocal<RContext> threadLocalContext = new ThreadLocal<>();

    /**
     * Used by the MethodListDispatch class.
     */

    private boolean methodTableDispatchOn = true;

    /*
     * Primarily for debugging.
     */
    private static final AtomicLong ID = new AtomicLong();
    @CompilationFinal private long id;
    private boolean active;

    private static final Deque<RContext> allContexts = new ConcurrentLinkedDeque<>();

    private static final Semaphore allContextsSemaphore = new Semaphore(1, true);

    /**
     * A (hopefully) temporary workaround to ignore the setting of {@link #resultVisible} for
     * benchmarks. Set across all contexts.
     */
    @CompilationFinal private static boolean ignoreVisibility;

    /*
     * Workarounds to finesse project circularities between runtime/nodes.
     */
    @CompilationFinal private static RRuntimeASTAccess runtimeASTAccess;
    @CompilationFinal private static RBuiltinLookup builtinLookup;

    /**
     * Initialize VM-wide static values.
     */
    public static void initialize(RRuntimeASTAccess rASTHelperArg, RBuiltinLookup rBuiltinLookupArg, boolean ignoreVisibilityArg) {
        runtimeASTAccess = rASTHelperArg;
        builtinLookup = rBuiltinLookupArg;
        ignoreVisibility = ignoreVisibilityArg;
    }

    /**
     * Associates this {@link RContext} with the current thread.
     */
    public void attachThread() {
        Thread current = Thread.currentThread();
        if (current instanceof ContextThread) {
            ((ContextThread) current).setContext(this);
        } else {
            threadLocalContext.set(this);
        }
    }

    /**
     * Waits for the associated EvalThread to finish.
     *
     * @throws InterruptedException
     */
    public void joinThread() throws InterruptedException {
        EvalThread t = this.evalThread;
        if (t == null) {
            throw RError.error(RError.NO_NODE, RError.Message.GENERIC, "no eval thread in a given context");
        }
        this.evalThread = null;
        t.join();
    }

    private static final Assumption singleContextAssumption = Truffle.getRuntime().createAssumption("single RContext");
    @CompilationFinal private static RContext singleContext;

    private RContext(Kind kind, RContext parent, String[] commandArgs, ConsoleHandler consoleHandler) {
        if (kind == Kind.SHARE_PARENT_RW) {
            if (parent.sharedChild != null) {
                throw RError.error(RError.NO_NODE, RError.Message.GENERIC, "can't have multiple active SHARED_PARENT_RW contexts");
            }
            parent.sharedChild = this;
        }
        this.kind = kind;
        this.parent = parent;
        this.id = ID.getAndIncrement();
        this.commandArgs = commandArgs;
        if (consoleHandler == null) {
            throw Utils.fail("no console handler set");
        }
        this.consoleHandler = consoleHandler;
        this.interactive = consoleHandler.isInteractive();
        try {
            allContextsSemaphore.acquire();
            allContexts.add(this);
            allContextsSemaphore.release();
        } catch (InterruptedException x) {
            throw RError.error(RError.NO_NODE, RError.Message.GENERIC, "error destroying context");
        }

        if (singleContextAssumption.isValid()) {
            if (singleContext == null) {
                singleContext = this;
            } else {
                singleContext = null;
                singleContextAssumption.invalidate();
            }
        }
    }

    public void installCustomClassState(ClassStateKind classStateKind, ContextState state) {
        assert classStateKind.customCreate;
        assert contextState[classStateKind.ordinal()] == null;
        contextState[classStateKind.ordinal()] = state;
    }

    /**
     * Inform state factories that the system is initialized.
     */
    public void systemInitialized() {
        for (ClassStateKind classStateKind : ClassStateKind.VALUES) {
            classStateKind.factory.systemInitialized(this, contextState[classStateKind.ordinal()]);
        }
    }

    /**
     * Create a {@link Kind#SHARE_NOTHING} {@link RContext}.
     *
     * @param parent if non-null {@code null} the parent creating the context
     * @param commandArgs the command line arguments passed this R session
     * @param consoleHandler a {@link ConsoleHandler} for output
     */
    public static RContext createShareNothing(RContext parent, String[] commandArgs, ConsoleHandler consoleHandler) {
        RContext result = create(parent, Kind.SHARE_NOTHING, commandArgs, consoleHandler);
        return result;
    }

    /**
     * Create a {@link Kind#SHARE_PARENT_RO} {@link RContext}.
     *
     * @param parent parent context with which to shgre
     * @param commandArgs the command line arguments passed this R session
     * @param consoleHandler a {@link ConsoleHandler} for output
     */
    public static RContext createShareParentReadOnly(RContext parent, String[] commandArgs, ConsoleHandler consoleHandler) {
        RContext result = create(parent, Kind.SHARE_PARENT_RO, commandArgs, consoleHandler);
        return result;

    }

    /**
     * Create a {@link Kind#SHARE_PARENT_RW} {@link RContext}.
     *
     * @param parent parent context with which to shgre
     * @param commandArgs the command line arguments passed this R session
     * @param consoleHandler a {@link ConsoleHandler} for output
     */
    public static RContext createShareParentReadWrite(RContext parent, String[] commandArgs, ConsoleHandler consoleHandler) {
        RContext result = create(parent, Kind.SHARE_PARENT_RW, commandArgs, consoleHandler);
        return result;

    }

    /**
     * Create a context of a given kind.
     */
    public static RContext create(RContext parent, Kind kind, String[] commandArgs, ConsoleHandler consoleHandler) {
        RContext result = new RContext(kind, parent, commandArgs, consoleHandler);
        result.engine = RContext.getRRuntimeASTAccess().createEngine(result);
        return result;
    }

    /**
     * Activate the context by attaching the current thread and initializing the
     * {@link StateFactory} objects. Note that we attach the thread before creating the new context
     * state. This means that code that accesses the state through this interface will receive a
     * {@code null} value. Access to the parent state is available through the {@link RContext}
     * argument passed to the {@link StateFactory#newContext(RContext, Object...)} method. It might
     * be better to attach the thread after state creation but it is a finely balanced decision and
     * risks incorrectly accessing the parent state.
     */
    public RContext activate() {
        assert !active;
        active = true;
        attachThread();
        contextState = new ContextState[ClassStateKind.VALUES.length];
        for (ClassStateKind classStateKind : ClassStateKind.VALUES) {
            if (!classStateKind.customCreate) {
                contextState[classStateKind.ordinal()] = classStateKind.factory.newContext(this);
            }
        }
        installCustomClassState(ClassStateKind.StdConnections, new StdConnections().newContext(this, consoleHandler));
        if (kind == Kind.SHARE_PARENT_RW) {
            parent.sharedChild = this;
        }
        // The environment state installation is handled by the engine
        engine.activate();
        return this;
    }

    /**
     * Destroy this context.
     */
    public void destroy() {
        for (ClassStateKind classStateKind : ClassStateKind.VALUES) {
            classStateKind.factory.beforeDestroy(this, contextState[classStateKind.ordinal()]);
        }
        if (kind == Kind.SHARE_PARENT_RW) {
            parent.sharedChild = null;
        }
        engine = null;
        try {
            allContextsSemaphore.acquire();
            allContexts.remove(this);
            allContextsSemaphore.release();
        } catch (InterruptedException x) {
            throw RError.error(RError.NO_NODE, RError.Message.GENERIC, "error destroying context");
        }
        if (parent == null) {
            threadLocalContext.set(null);
        } else {
            threadLocalContext.set(parent);
        }
    }

    public RContext getParent() {
        return parent;
    }

    public Kind getKind() {
        return kind;
    }

    public long getId() {
        return id;
    }

    public static RContext find(int id) {
        try {
            allContextsSemaphore.acquire();
            for (RContext context : allContexts) {
                if (context.id == id) {
                    allContextsSemaphore.release();
                    return context;
                }
            }
            allContextsSemaphore.release();
        } catch (InterruptedException x) {
            throw RError.error(RError.NO_NODE, RError.Message.GENERIC, "error destroying context");
        }
        return null;
    }

    public GlobalAssumptions getAssumptions() {
        return globalAssumptions;
    }

    @TruffleBoundary
    private static RContext getInstanceInternal() {
        RContext result = threadLocalContext.get();
        assert result != null;
        assert result.active;
        return result;
    }

    public static RContext getInstance() {
        RContext context = singleContext;
        if (context != null) {
            try {
                singleContextAssumption.check();
                return context;
            } catch (InvalidAssumptionException e) {
                // fallback to slow case
            }
        }

        Thread current = Thread.currentThread();
        if (current instanceof ContextThread) {
            context = ((ContextThread) current).context;
            assert context != null;
            return context;
        } else {
            return getInstanceInternal();
        }

    }

    /**
     * Access to the engine, when an {@link RContext} object is available, and/or when {@code this}
     * context is not active.
     */
    public Engine getThisEngine() {
        return engine;
    }

    /**
     * Access to the {@link ContextState}, when an {@link RContext} object is available, and/or when
     * {@code this} context is not active.
     */
    public ContextState getThisContextState(ClassStateKind classStateKind) {
        return contextState[classStateKind.ordinal()];
    }

    public boolean isVisible() {
        return resultVisible;
    }

    public void setVisible(boolean v) {
        resultVisible = v;
    }

    public boolean isMethodTableDispatchOn() {
        return methodTableDispatchOn;
    }

    public void setMethodTableDispatchOn(boolean on) {
        methodTableDispatchOn = on;
    }

    public boolean isInteractive() {
        return interactive;
    }

    public static boolean isIgnoringVisibility() {
        return ignoreVisibility;
    }

    public ConsoleHandler getConsoleHandler() {
        return consoleHandler;
    }

    private TimeZone timeZone = TimeZone.getDefault();

    public TimeZone getSystemTimeZone() {
        return timeZone;
    }

    public void setSystemTimeZone(TimeZone timeZone) {
        this.timeZone = timeZone;
    }

    /**
     * This is a static property of the implementation and not context-specific.
     */
    public static RRuntimeASTAccess getRRuntimeASTAccess() {
        return runtimeASTAccess;
    }

    /**
     * Is {@code name} a builtin function (but not a {@link RBuiltinKind#INTERNAL}?
     */
    public static boolean isPrimitiveBuiltin(String name) {
        return builtinLookup.isPrimitiveBuiltin(name);
    }

    /**
     * Return the {@link RFunction} for the builtin {@code name}.
     */
    public static RFunction lookupBuiltin(String name) {
        return builtinLookup.lookupBuiltin(name);
    }

    /**
     * Returns the descriptor for the builtin with the given name. This does not cause an RFunction
     * to be created.
     */
    public static RBuiltinDescriptor lookupBuiltinDescriptor(String name) {
        return builtinLookup.lookupBuiltinDescriptor(name);
    }

    public static RFunction cacheBuiltin(Object key, RFunction function) {
        cachedBuiltinFunctions.put(key, function);
        return function;
    }

    public static RFunction getCachedBuiltin(Object key) {
        return cachedBuiltinFunctions.get(key);
    }

    public String[] getCommandArgs() {
        if (commandArgs == null) {
            throw Utils.fail("no command args set");
        }
        return commandArgs;
    }

    @Override
    public String toString() {
        return "context: " + id;
    }

    /*
     * static functions necessary in code where the context is only implicit in the thread(s)
     * running an evaluation
     */

    public static Engine getEngine() {
        return RContext.getInstance().engine;
    }

    public static ContextState getContextState(ClassStateKind kind) {
        return getInstance().contextState[kind.ordinal()];
    }

    public static REnvironment.ContextState getREnvironmentState() {
        return (REnvironment.ContextState) getInstance().contextState[ClassStateKind.REnvironment.ordinal()];
    }

    public static ROptions.ContextState getROptionsState() {
        return (ROptions.ContextState) getInstance().contextState[ClassStateKind.ROptions.ordinal()];
    }

    public static ConnectionSupport.ContextState getRConnectionState() {
        return (ConnectionSupport.ContextState) getInstance().contextState[ClassStateKind.RConnection.ordinal()];
    }

    public void setLoadingBase(boolean b) {
        loadingBase = b;
    }

    public boolean getLoadingBase() {
        return loadingBase;
    }

}
