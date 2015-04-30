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
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.RPromise.Closure;
import com.oracle.truffle.r.runtime.env.*;
import com.oracle.truffle.r.runtime.env.REnvironment.*;

/**
 * Provides access to the runtime state ("context") without being tied statically to a particular
 * implementation of the runtime.
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
        void printf(String format, Object... args);

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
         * Read a line of input, newline omitted in result. Returns null if {@link #isInteractive()
         * == false}.
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
         * Is {@code name} a builtin function (but not a {@link RBuiltinKind#INTERNAL}?
         */
        boolean isPrimitiveBuiltin(String name);

        /**
         * Return the {@link RFunction} for the builtin {@code name}.
         *
         */
        RFunction lookupBuiltin(String name);

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
         * @param envForFrame the environment that {@code frame} is bound to.
         * @param printResult {@code true} iff the result of the evaluation should be printed to the
         *            console
         * @param allowIncompleteSource {@code true} if partial input is acceptable
         * @return the object returned by the evaluation or {@code null} if an error occurred.
         */
        Object parseAndEval(Source sourceDesc, MaterializedFrame frame, REnvironment envForFrame, boolean printResult, boolean allowIncompleteSource);

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
        RootCallTarget makeCallTarget(Object body, String funName);

    }

    private final HashMap<Object, RFunction> cachedFunctions = new HashMap<>();
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

    /**
     * A (hopefully) temporary workaround to ignore the setting of {@link #resultVisible} for
     * benchmarks.
     */
    @CompilationFinal private boolean ignoreVisibility;

    @CompilationFinal private ConsoleHandler consoleHandler;
    @CompilationFinal private String[] commandArgs;
    @CompilationFinal private Engine engine;
    @CompilationFinal private RASTHelper rASTHelper;

    private static final RContext singleton = new RContext();

    private RContext() {
    }

    public GlobalAssumptions getAssumptions() {
        return globalAssumptions;
    }

    public static RContext getInstance() {
        return singleton;
    }

    public static Engine getEngine() {
        return singleton.engine;
    }

    /**
     * Although there is only ever one instance of a {@code RContext}, the following state fields
     * are runtime specific and must be set explicitly.
     * 
     * @param ignoreVisibility TODO
     */
    public static RContext setRuntimeState(Engine engine, String[] commandArgs, ConsoleHandler consoleHandler, RASTHelper rLanguageHelper, boolean interactive, boolean ignoreVisibility) {
        singleton.engine = engine;
        singleton.commandArgs = commandArgs;
        singleton.consoleHandler = consoleHandler;
        singleton.rASTHelper = rLanguageHelper;
        singleton.interactive = interactive;
        singleton.ignoreVisibility = ignoreVisibility;
        return singleton;
    }

    public static boolean isVisible() {
        return singleton.resultVisible;
    }

    public static void setVisible(boolean v) {
        singleton.resultVisible = v;
    }

    public static boolean isInteractive() {
        return singleton.interactive;
    }

    public static boolean isIgnoringVisibility() {
        return singleton.ignoreVisibility;
    }

    public ConsoleHandler getConsoleHandler() {
        if (consoleHandler == null) {
            throw Utils.fail("no console handler set");
        }
        return consoleHandler;
    }

    public static RASTHelper getRASTHelper() {
        return singleton.rASTHelper;
    }

    public RFunction putCachedFunction(Object key, RFunction function) {
        cachedFunctions.put(key, function);
        return function;
    }

    public RFunction getCachedFunction(Object key) {
        return cachedFunctions.get(key);
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
