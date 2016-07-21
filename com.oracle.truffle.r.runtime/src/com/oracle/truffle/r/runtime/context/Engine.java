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
package com.oracle.truffle.r.runtime.context;

import java.io.IOException;
import java.util.Map;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.vm.PolyglotEngine;
import com.oracle.truffle.r.runtime.RCaller;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.data.RExpression;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RLanguage;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.nodes.RNode;

public interface Engine {

    class ParseException extends IOException {
        private static final long serialVersionUID = 1L;

        private final Source source;
        private final String token;
        private final String substring;
        private final int line;

        public ParseException(Throwable cause, Source source, String token, String substring, int line) {
            super("parse exception", cause);
            this.source = source;
            this.token = token;
            this.substring = substring;
            this.line = line;
        }

        public RError throwAsRError() {
            if (source.getLineCount() == 1) {
                throw RError.error(RError.NO_CALLER, RError.Message.UNEXPECTED, token, substring);
            } else {
                throw RError.error(RError.NO_CALLER, RError.Message.UNEXPECTED_LINE, token, substring, line);
            }
        }

        public void report(ConsoleHandler consoleHandler) {
            String msg;
            if (source.getLineCount() == 1) {
                msg = String.format(RError.Message.UNEXPECTED.message, token, substring);
            } else {
                msg = String.format(RError.Message.UNEXPECTED_LINE.message, token, substring, line);
            }
            consoleHandler.println("Error: " + msg);
        }
    }

    final class IncompleteSourceException extends ParseException {
        private static final long serialVersionUID = -6688699706193438722L;

        public IncompleteSourceException(Throwable cause, Source source, String token, String substring, int line) {
            super(cause, source, token, substring, line);
        }
    }

    /**
     * Make the engine ready for evaluations.
     */
    void activate(REnvironment.ContextStateImpl stateREnvironment);

    public interface Timings {
        /**
         * Elapsed time of runtime.
         *
         * @return elapsed time in nanosecs.
         */
        long elapsedTimeInNanos();

        /**
         * Return user and system times for any spawned child processes in nanosecs, {@code < 0}
         * means not available.
         */
        long[] childTimesInNanos();

        /**
         * Return user/sys time for this engine, {@code < 0} means not available..
         */
        long[] userSysTimeInNanos();

    }

    /**
     * Return the timing information for this engine.
     */
    Timings getTimings();

    /**
     * Parse an R expression and return an {@link RExpression} object representing the Truffle ASTs
     * for the components. The {Code constants} map can be used to replace some names in the source
     * code with specific constants, which can be used to parse code that has no proper textual
     * representation.
     */
    RExpression parse(Map<String, Object> constants, Source source) throws ParseException;

    /**
     * This is the external interface from {@link PolyglotEngine#eval(Source)}. It is required to
     * return a {@link CallTarget} which may be cached for future use, and the
     * {@link PolyglotEngine} is responsible for actually invoking the call target.
     */
    CallTarget parseToCallTarget(Source source) throws ParseException;

    /**
     * Parse and evaluate {@code rscript} in {@code frame}. {@code printResult == true}, the result
     * of the evaluation is printed to the console.
     *
     * @param sourceDesc a {@link Source} object that describes the input to be parsed
     * @param frame the frame in which to evaluate the input
     * @param printResult {@code true} iff the result of the evaluation should be printed to the
     *            console
     * @return the object returned by the evaluation or {@code null} if an error occurred.
     */
    Object parseAndEval(Source sourceDesc, MaterializedFrame frame, boolean printResult) throws ParseException;

    /**
     * Support for the {@code eval} {@code .Internal}.
     */
    Object eval(RExpression expr, REnvironment envir, RCaller caller);

    /**
     * Variant of {@link #eval(RExpression, REnvironment, RCaller)} for a single language element.
     */
    Object eval(RLanguage expr, REnvironment envir, RCaller caller);

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
     * {@link RFunction} and the evaluated arguments. {@code frame} may be {@code null} in which
     * case the current frame is used). In many cases {@code frame} may not represent the current
     * call stack, for example many S4-related evaluations set {@code frame} to the {@code methods}
     * namespace, but the current stack is not empty. So when {@code frame} is not {@code null} a
     * {@code caller} should be passed to maintain the call stack correctly.
     */
    Object evalFunction(RFunction func, MaterializedFrame frame, RCaller caller, Object... args);

    /**
     * Checks for the existence of (startup/shutdown) function {@code name} and, if present, invokes
     * the function with the given {@code args}.
     */
    void checkAndRunStartupShutdownFunction(String name, String... args);

    /**
     * Wraps the Truffle AST in {@code body} in an anonymous function and returns a
     * {@link RootCallTarget} for it.
     *
     * N.B. For certain expressions, there might be some value in enclosing the wrapper function in
     * a specific lexical scope. E.g., as a way to access names in the expression known to be
     * defined in that scope.
     *
     * @param body The AST for the body of the wrapper, i.e., the expression being evaluated.
     */
    RootCallTarget makePromiseCallTarget(RNode body, String funName);

    /**
     * Used by Truffle debugger; invokes the internal "print" support in R for {@code value}.
     * Essentially this is equivalent to {@link #evalFunction} using the {@code "print"} function.
     */
    void printResult(Object value);

    /**
     * This function a special fast path to create functions from code directly, without executing
     * the intermediate "function" expression.
     */
    RFunction parseFunction(Map<String, Object> constants, String name, Source source, MaterializedFrame enclosingFrame) throws ParseException;
}
