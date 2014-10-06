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
package com.oracle.truffle.r.runtime;

import java.util.*;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.SlowPath;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.instrument.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.r.runtime.data.*;
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
     * tagged with {@link SlowPath}, as should the appropriate implementation methods.
     */
    public interface ConsoleHandler {
        /**
         * Normal output with a new line.
         */
        @SlowPath
        void println(String s);

        /**
         * Normal output without a newline.
         */
        @SlowPath
        void print(String s);

        /**
         * Formatted output.
         */
        @SlowPath
        void printf(String format, Object... args);

        /**
         * Error output with a newline.
         *
         * @param s
         */
        @SlowPath
        void printErrorln(String s);

        /**
         * Error output without a newline.
         */
        @SlowPath
        void printError(String s);

        /**
         * Read a line of input, newline omitted in result. Returns null if {@link #isInteractive()
         * == false}.
         */
        @SlowPath
        String readLine();

        /**
         * Return {@code true} if and only if this console is interactive.
         */
        @SlowPath
        boolean isInteractive();

        /**
         * Redirect error output to the normal output.
         */
        @SlowPath
        void redirectError();

        /**
         * Get the current prompt.
         */
        @SlowPath
        String getPrompt();

        /**
         * Set the R prompt.
         */
        @SlowPath
        void setPrompt(String prompt);

        /**
         * Get the console width.
         */
        @SlowPath
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
         * Load a package from the default set of packages identified at startup.
         *
         * @param name name of the package, e.g. {@code base}, {@code stats}.
         * @param frame for evaluating any associated R code
         * @param envForFrame the namespace environment associated with the package.
         */
        void loadDefaultPackage(String name, VirtualFrame frame, REnvironment envForFrame);

        /**
         * Return the {@link RFunction} for the builtin {@code name}.
         *
         */
        RFunction lookupBuiltin(String name);

        /**
         * Parse an R expression and return an {@link RExpression} object representing the Truffle
         * ASTs for the components.
         */
        RExpression parse(String rscript) throws ParseException;

        /**
         * Parse and evaluate {@code rscript} in {@code frame}. {@code printResult == true}, the
         * result of the evaluation is printed to the console.
         *
         * @param envForFrame the environment that {@code frame} is bound to.
         * @return the object returned by the evaluation or {@code null} if an error occurred.
         */
        Object parseAndEval(String sourceDesc, String rscript, VirtualFrame frame, REnvironment envForFrame, boolean printResult, boolean allowIncompleteSource);

        static final Object INCOMPLETE_SOURCE = new Object();

        /**
         *
         * This is intended for use by the unit test environment, where a "fresh" global environment
         * is desired for each evaluation.
         */
        Object parseAndEvalTest(String rscript, boolean printResult);

        /**
         * Support for the {@code eval} family of builtin functions.
         *
         * @param function identifies the eval variant, e.g. {@code local}, {@code eval},
         *            {@code evalq} being invoked. The value {@code null} means plain {@code eval}.
         *
         * @param enclos normally {@code null}, but see <a
         *            href="https://stat.ethz.ch/R-manual/R-devel/library/base/html/eval.html">here
         *            for details</a>.
         */
        Object eval(RFunction function, RExpression expr, REnvironment envir, REnvironment enclos) throws PutException;

        /**
         * Convenience method for common case.
         */
        default Object eval(RExpression expr, REnvironment envir) throws PutException {
            return eval(null, expr, envir, null);
        }

        /**
         * Variant of {@link #eval(RFunction, RExpression, REnvironment, REnvironment)} for a single
         * language element.
         */
        Object eval(RFunction function, RLanguage expr, REnvironment envir, REnvironment enclos) throws PutException;

        /**
         * Convenience method for common case.
         */
        default Object eval(RLanguage expr, REnvironment envir) throws PutException {
            return eval(null, expr, envir, null);
        }

        /**
         * Evaluate {@code expr} in {@code frame}.
         */
        Object eval(RExpression expr, VirtualFrame frame);

        /**
         * Variant of {@link #eval(RExpression, VirtualFrame)} for a single language element.
         */
        Object eval(RLanguage expr, VirtualFrame frame);

        /**
         * Evaluate a promise in the given frame, where we can use the {@link VirtualFrame}) of the
         * caller directly). This should <b>only</b> be called by the {@link RPromise} class.
         */
        Object evalPromise(RPromise expr, VirtualFrame frame) throws RError;

        /**
         * Evaluate a promise in the {@link MaterializedFrame} stored with the promise. This should
         * <b>only</b> be called by the {@link RPromise} class.
         */
        Object evalPromise(RPromise expr, SourceSection callSrc) throws RError;

        /**
         * Wraps the Truffle AST in {@code body} in an anonymous function and returns a
         * {@link RootCallTarget} for it. We define the
         * {@link com.oracle.truffle.r.runtime.env.REnvironment.FunctionDefinition} environment to
         * have the {@link REnvironment#emptyEnv()} as parent, so it is note scoped relative to any
         * existing environments, i.e. is truly anonymous.
         *
         * N.B. For certain expressions, there might be some value in enclosing the wrapper function
         * in a specific lexical scope. E.g., as a way to access names in the expression known to be
         * defined in that scope.
         *
         * @param body The AST for the body of the wrapper, i.e., the expression being evaluated.
         */
        RootCallTarget makeCallTarget(Object body, String funName);

        /**
         * Print 'e' and any associated warnings.
         *
         * @param e
         */
        void printRError(RError e);

    }

    private final HashMap<Object, RFunction> cachedFunctions = new HashMap<>();
    private final GlobalAssumptions globalAssumptions = new GlobalAssumptions();
    private LinkedList<String> evalWarnings;

    /**
     * Denote whether the result of an expression should be printed in the shell or not.
     */
    private boolean resultVisible = true;

    /**
     * Denote whether the FastR instance is running in headless mode ({@code true}), or in the shell
     * or test harness ({@code false}).
     */
    @CompilationFinal private boolean headless;

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
     */
    public static RContext setRuntimeState(Engine engine, String[] commandArgs, ConsoleHandler consoleHandler, RASTHelper rLanguageHelper, boolean headless) {
        singleton.engine = engine;
        singleton.commandArgs = commandArgs;
        singleton.consoleHandler = consoleHandler;
        singleton.rASTHelper = rLanguageHelper;
        singleton.headless = headless;
        return singleton;
    }

    public static boolean isVisible() {
        return singleton.resultVisible;
    }

    public static void setVisible(boolean v) {
        singleton.resultVisible = v;
    }

    public static boolean isHeadless() {
        return singleton.headless;
    }

    public ConsoleHandler getConsoleHandler() {
        if (consoleHandler == null) {
            Utils.fail("no console handler set");
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
            Utils.fail("no command args set");
        }
        return commandArgs;
    }

    public List<String> extractEvalWarnings() {
        List<String> l = evalWarnings;
        evalWarnings = null;
        return l;
    }

    public void setEvalWarning(String s) {
        if (evalWarnings == null) {
            evalWarnings = new LinkedList<>();
        }
        evalWarnings.add(s);
    }

    @Override
    public String getLanguageShortName() {
        return "R";
    }

    @Override
    protected void setSourceCallback(SourceCallback sourceCallback) {
        // TODO Auto-generated method stub
    }
}
