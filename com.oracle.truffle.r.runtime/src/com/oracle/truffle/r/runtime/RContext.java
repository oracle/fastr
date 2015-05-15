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

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.instrument.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.RPromise.Closure;
import com.oracle.truffle.r.runtime.env.*;
import com.oracle.truffle.r.runtime.env.REnvironment.*;

/**
 * Encapsulates the runtime state ("context") of an R session. All access to that state from the
 * implementation <b>must</b>go through this class. There can be multiple instances
 * (multiple-tenancy) active within a single process/Java-VM.
 *
 * The context provides two sub-interfaces {@link ConsoleHandler} and {@link Engine}that are
 * (typically) implemented elsewhere, and accessed through {@link #getConsoleHandler()} and
 * {@link #getEngine()}, respectively.
 *
 */
public final class RContext extends ExecutionContext {

    public static final int CONSOLE_WIDTH = 80;

    /**
     * The interface to the R console, which may have different implementations for different
     * environments, but only one in any given VM execution. Since I/O is involved, all methods are
     * tagged with {@link TruffleBoundary}, as should the appropriate implementation methods.
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

    public interface Engine {
        MaterializedFrame getGlobalFrame();

        REnvironment getGlobalEnv();

        REnvironment.SearchPath getSearchPath();

        void setSearchPath(REnvironment.SearchPath searchPath);

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

            public ParseException(String msg) {
                super(msg);
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

        Object INCOMPLETE_SOURCE = new Object();

        /**
         *
         * This is intended for use by the unit test environment, where a "fresh" global environment
         * is desired for each evaluation.
         */
        Object parseAndEvalTest(String rscript, boolean printResult) throws Exception;

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
     * Builtin cache. Valid across all contexts.
     */
    private static final HashMap<Object, RFunction> cachedBuiltinFunctions = new HashMap<>();

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
     * A (hopefully) temporary workaround to ignore the setting of {@link #resultVisible} for
     * benchmarks. Set across all contexts.
     */
    @CompilationFinal private static boolean ignoreVisibility;

    /*
     * Workarounds to finesse project circularities between runtime/nodes.
     */
    @CompilationFinal private static RASTHelper rASTHelper;
    @CompilationFinal private static RBuiltinLookup rBuiltinLookup;

    /**
     * Initialize VM-wide static values.
     */
    public static void initialize(RASTHelper rASTHelperArg, RBuiltinLookup rBuiltinLookupArg, boolean ignoreVisibilityArg) {
        rASTHelper = rASTHelperArg;
        rBuiltinLookup = rBuiltinLookupArg;
        ignoreVisibility = ignoreVisibilityArg;
    }

    /**
     * The initial context (used to initial the base package) that can be used for context-free
     * activities such as parsing when there is no stack.
     */
    @CompilationFinal private static RContext initialContext;

    private RContext(String[] commandArgs, ConsoleHandler consoleHandler) {
        this.commandArgs = commandArgs;
        this.consoleHandler = consoleHandler;
        this.interactive = consoleHandler.isInteractive();
        if (initialContext == null) {
            initialContext = this;
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
     *
     * @param commandArgs the command line arguments passed this R session
     * @param consoleHandler a {@link ConsoleHandler} for output
     */
    public static RContext create(String[] commandArgs, ConsoleHandler consoleHandler) {
        return new RContext(commandArgs, consoleHandler);
    }

    public GlobalAssumptions getAssumptions() {
        return globalAssumptions;
    }

    /**
     * Gets the current context with no frame to provide an efficient lookup. So we have to ask
     * Truffle for the current frame, which may be null if we are startup, result printing in the
     * shell, or shutdown. In that case {@code #noFrame(true)} must have been called. If you have a
     * frame at hand, use {@link #getInstance(Frame)} instead.
     */
    public static RContext getInstance() {
        RContext result = getInstanceNoFrame();
        return result;
    }

    @TruffleBoundary
    private static RContext getInstanceNoFrame() {
        Frame frame = Utils.getActualCurrentFrame();
        if (frame == null) {
            assert initialContext != null;
            return initialContext;
        } else {
            return RArguments.getContext(frame);
        }
    }

    /**
     * Fast path to the context through {@code frame}.
     */
    public static RContext getInstance(Frame frame) {
        RContext context = RArguments.getContext(frame);
        assert context != null;
        return context;
    }

    public Engine getEngine() {
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
    public static RASTHelper getRASTHelper() {
        return rASTHelper;
    }

    /**
     * Is {@code name} a builtin function (but not a {@link RBuiltinKind#INTERNAL}?
     */
    public static boolean isPrimitiveBuiltin(String name) {
        return rBuiltinLookup.isPrimitiveBuiltin(name);
    }

    /**
     * Return the {@link RFunction} for the builtin {@code name}.
     *
     */
    public static RFunction lookupBuiltin(String name) {
        return rBuiltinLookup.lookup(name);
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

}
