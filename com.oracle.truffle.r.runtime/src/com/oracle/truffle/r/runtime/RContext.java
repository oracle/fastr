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
import com.oracle.truffle.api.instrument.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.r.runtime.conn.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.RPromise.Closure;
import com.oracle.truffle.r.runtime.env.*;
import com.oracle.truffle.r.runtime.env.REnvironment.*;
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
 * Context-specific state for implementation classes is managed by this class (or the engine) and
 * accessed through the {@code getXXXState} methods.
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
         * Shares the set of loaded packages at the time the context is created. Only useful when
         * there is a priori knowledge on the evaluation that the context will be used for. Cannot
         * safely be used for parallel context evaluation. N.B. Evidently a {link SHARED_NOTHING}
         * context must already exist for
         */
        SHARED_PACKAGES,
        /**
         * Essentially a clean restart, modulo the basic VM-wide initialization. which does include,
         * for example, reading the external environment variables. This kind of contxt can be used
         * in a parallel computation.
         */
        SHARED_NOTHING
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
         * is initialized, in which case they can override this method.
         */
        @SuppressWarnings("unused")
        default void systemInitialized(RContext context, ContextState state) {

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
         * Evaluate a promise in the given frame, where we can use the {@link MaterializedFrame}) of
         * the caller directly). This should <b>only</b> be called by the {@link RPromise} class.
         */
        Object evalPromise(RPromise expr, MaterializedFrame frame);

        Object evalPromise(Closure closure, MaterializedFrame frame);

        /**
         * Evaluate a promise in the {@link MaterializedFrame} stored with the promise. This should
         * <b>only</b> be called by the {@link RPromise} class.
         */
        Object evalPromise(RPromise expr, SourceSection callSrc);

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
         * Returns an R-specific {@link Visualizer} for use by the instrumentation framework.
         */
        Visualizer getRVisualizer();

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
        RNG(RRNG.class, false);

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
    @CompilationFinal private ContextState[] classState;

    /**
     * Typically there is a 1-1 relationship between an {@link RContext} and the thread that is
     * performing the evaluation, so we can store the {@link RContext} in a {@link ThreadLocal}. If
     * an evaluation wishes to add more threads, it must call the {@link #addThread} method.
     */
    @CompilationFinal private static final ThreadLocal<RContext> threadLocalContext = new ThreadLocal<>();

    private static final ConcurrentLinkedDeque<RContext> activeSet = new ConcurrentLinkedDeque<>();

    /**
     * Primarily for debugging.
     */
    private static final AtomicLong ID = new AtomicLong();
    @CompilationFinal private long id;

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
    public void addThread() {
        threadLocalContext.set(this);
    }

    private RContext(Kind kind, String[] commandArgs, ConsoleHandler consoleHandler) {
        this.kind = kind;
        this.id = ID.incrementAndGet();
        addThread();
        this.commandArgs = commandArgs;
        this.consoleHandler = consoleHandler;
        this.interactive = consoleHandler.isInteractive();
        classState = new ContextState[ClassStateKind.VALUES.length];
        for (ClassStateKind classStateKind : ClassStateKind.VALUES) {
            if (!classStateKind.customCreate) {
                classState[classStateKind.ordinal()] = classStateKind.factory.newContext(this);
            }
        }
        installCustomClassState(ClassStateKind.StdConnections, new StdConnections().newContext(this, consoleHandler));
        activeSet.add(this);
    }

    public void installCustomClassState(ClassStateKind classStateKind, ContextState state) {
        assert classStateKind.customCreate;
        assert classState[classStateKind.ordinal()] == null;
        classState[classStateKind.ordinal()] = state;
    }

    public void systemInitialized() {
        for (ClassStateKind classStateKind : ClassStateKind.VALUES) {
            classStateKind.factory.systemInitialized(this, classState[classStateKind.ordinal()]);
        }
    }

    /**
     * The {@link RContext} and the {@link Engine} are bi-directionally linked hence an explicit
     * method to establish one half of the link.
     *
     * @param engine an {@link Engine} that manages parsing and evaluation, aka the <i>evaluator</i>
     */
    public void setEngine(Engine engine) {
        assert this.engine == null;
        this.engine = engine;
        this.setVisualizer(engine.getRVisualizer());
    }

    /**
     * Create a new {@link RContext} of the given {@link Kind}.
     *
     * @param kind controlshow much of the existing environment is shared
     * @param commandArgs the command line arguments passed this R session
     * @param consoleHandler a {@link ConsoleHandler} for output
     */
    public static RContext create(Kind kind, String[] commandArgs, ConsoleHandler consoleHandler) {
        validate(kind);
        return new RContext(kind, commandArgs, consoleHandler);
    }

    private static void validate(Kind kind) {
        if (kind == Kind.SHARED_PACKAGES) {
            for (RContext c : activeSet) {
                if (c.kind == kind) {
                    throw new RInternalError("can't have multiple active SHARED_PACKAGES");
                }
            }
        }
    }

    /**
     * Destroy this context.
     */
    public void destroy() {
        activeSet.remove(this);
        engine = null;
        threadLocalContext.set(null);
    }

    public GlobalAssumptions getAssumptions() {
        return globalAssumptions;
    }

    @TruffleBoundary
    public static RContext getInstance() {
        RContext result = threadLocalContext.get();
        return result;
    }

    /**
     * Access to the engine, when an {@link RContext} object is available.
     */
    public Engine getContextEngine() {
        return engine;
    }

    public boolean isVisible() {
        return resultVisible;
    }

    public void setVisible(boolean v) {
        resultVisible = v;
    }

    public boolean isInteractive() {
        return interactive;
    }

    public static boolean isIgnoringVisibility() {
        return ignoreVisibility;
    }

    public ConsoleHandler getConsoleHandler() {
        if (consoleHandler == null) {
            throw Utils.fail("no console handler set");
        }
        return consoleHandler;
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
     *
     */
    public static RFunction lookupBuiltin(String name) {
        return builtinLookup.lookup(name);
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
    public String getLanguageShortName() {
        return "R";
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

    public static ContextState getClassState(ClassStateKind kind) {
        return getInstance().classState[kind.ordinal()];
    }

    public static REnvironment.ContextState getREnvironmentState() {
        return (REnvironment.ContextState) getInstance().classState[ClassStateKind.REnvironment.ordinal()];
    }

    public static ROptions.ContextState getROptionsState() {
        return (ROptions.ContextState) getInstance().classState[ClassStateKind.ROptions.ordinal()];
    }

    public static ConnectionSupport.ContextState getRConnectionState() {
        return (ConnectionSupport.ContextState) getInstance().classState[ClassStateKind.RConnection.ordinal()];
    }

}
