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
package com.oracle.truffle.r.runtime.context;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.interop.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.api.vm.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.env.*;
import com.oracle.truffle.r.runtime.env.REnvironment.PutException;

public interface Engine {
    /**
     * Make the engine ready for evaluations.
     */
    void activate(REnvironment.ContextStateImpl stateREnvironment);

    /**
     * Return the {@link TruffleVM} instance associated with this engine.
     */
    TruffleVM getTruffleVM();

    /**
     * Return the {@link com.oracle.truffle.api.vm.TruffleVM.Builder} instance associated with this
     * engine. This is only used by the command line debugger.
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
     * Parse an R expression and return an {@link RExpression} object representing the Truffle ASTs
     * for the components.
     */
    RExpression parse(Source source) throws Engine.ParseException;

    /**
     * A (perhaps temporary) interface to support {@link TruffleLanguage}.
     */
    CallTarget parseToCallTarget(Source source);

    /**
     * Parse and evaluate {@code rscript} in {@code frame}. {@code printResult == true}, the result
     * of the evaluation is printed to the console.
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
     * Variant of {@link #parseAndEval(Source, MaterializedFrame, boolean, boolean)} for evaluation
     * in the global frame.
     */
    Object parseAndEval(Source sourceDesc, boolean printResult, boolean allowIncompleteSource);

    /**
     * Variant of {@link #parseAndEval(Source, MaterializedFrame, boolean, boolean)} that does not
     * intercept errors.
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
     * Variant of {@link #eval(RExpression, REnvironment, REnvironment, int)} for a single language
     * element.
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
     * {@link RFunction} and the evaluated arguments, but do not have a frame available, and we are
     * behind a {@link TruffleBoundary}, so call inlining is not an issue. This is primarily used
     * for R callbacks from {@link RErrorHandling} and {@link RSerialize}.
     */
    Object evalFunction(RFunction func, Object... args);

    /**
     * Evaluates an {@link com.oracle.truffle.r.runtime.data.RPromise.Closure} in {@code frame}.
     */
    Object evalPromise(RPromise.Closure closure, MaterializedFrame frame);

    /**
     * Checks for the existence of {@code .Last/.Last.sys} and if present and bound to a function,
     * invokes the (parameterless) function.
     */
    void checkAndRunLast(String name);

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
    RootCallTarget makePromiseCallTarget(Object body, String funName);

    /**
     * Used by Truffle debugger; invokes the internal "print" support in R for {@code value}.
     * Essentially this is equivalent to {@link #evalFunction} using the {@code "print"} function.
     */
    void printResult(Object value);

    RFunction parseFunction(String name, Source source, MaterializedFrame enclosingFrame) throws Engine.ParseException;

    ForeignAccess getForeignAccess(RTypedValue value);

    Class<? extends TruffleLanguage<RContext>> getTruffleLanguage();

}
