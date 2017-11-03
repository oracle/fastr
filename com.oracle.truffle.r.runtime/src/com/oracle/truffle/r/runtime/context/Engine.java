/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.TruffleException;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.vm.PolyglotEngine;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RCaller;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RSource;
import com.oracle.truffle.r.runtime.context.RContext.ConsoleIO;
import com.oracle.truffle.r.runtime.data.RExpression;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RLanguage;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.nodes.RNode;

public interface Engine {

    Source GET_CONTEXT = RSource.fromTextInternal("<<<get_context>>>", RSource.Internal.GET_CONTEXT);

    class ParseException extends RuntimeException implements TruffleException {
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

        @TruffleBoundary
        public RError throwAsRError() {
            if (source.getLineCount() == 1) {
                throw RError.error(RError.NO_CALLER, RError.Message.UNEXPECTED, token, substring);
            } else {
                throw RError.error(RError.NO_CALLER, RError.Message.UNEXPECTED_LINE, token, substring, line);
            }
        }

        @TruffleBoundary
        public void report(OutputStream output) {
            try {
                output.write(getErrorMessage().getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new RInternalError(e, "error while printing parse exception");
            }
        }

        @TruffleBoundary
        public void report(ConsoleIO console) {
            console.println(getErrorMessage());
        }

        private String getErrorMessage() {
            String msg;
            if (source.getLineCount() == 1) {
                msg = String.format(RError.Message.UNEXPECTED.message, token, substring);
            } else {
                msg = String.format(RError.Message.UNEXPECTED_LINE.message, token, substring, line);
            }
            return "Error: " + msg;
        }

        @Override
        public Node getLocation() {
            if (line <= 0 || line > source.getLineCount()) {
                return null;
            } else {
                SourceSection section = source.createSection(line);
                return new Node() {
                    @Override
                    public SourceSection getSourceSection() {
                        return section;
                    }
                };
            }
        }

        @Override
        public boolean isSyntaxError() {
            return true;
        }
    }

    final class IncompleteSourceException extends ParseException {
        private static final long serialVersionUID = -6688699706193438722L;

        public IncompleteSourceException(Throwable cause, Source source, String token, String substring, int line) {
            super(cause, source, token, substring, line);
        }

        @Override
        public boolean isIncompleteSource() {
            return true;
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
     * for the components.
     */
    RExpression parse(Source source) throws ParseException;

    /**
     * This is the external interface from {@link PolyglotEngine#eval(Source)}. It is required to
     * return a {@link CallTarget} which may be cached for future use, and the
     * {@link PolyglotEngine} is responsible for actually invoking the call target.
     */
    CallTarget parseToCallTarget(Source source, MaterializedFrame executionFrame) throws ParseException;

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
     * {@code caller} should be passed to maintain the call stack correctly. {@code names} string
     * vector describing (optional) argument names
     *
     * @param names signature of the given parameters, may be {@code null} in which case the empty
     *            signature of correct cardinality shall be used.
     * @param evalPromises whether to evaluate promises in args array before calling the function.
     */
    Object evalFunction(RFunction func, MaterializedFrame frame, RCaller caller, boolean evalPromises, ArgumentsSignature names, Object... args);

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
     * Return the "global" frame for this {@link Engine}, aka {@code globalEnv}.
     *
     */
    MaterializedFrame getGlobalFrame();
}
