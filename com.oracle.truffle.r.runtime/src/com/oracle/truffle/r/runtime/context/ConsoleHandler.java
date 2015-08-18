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

import com.oracle.truffle.api.CompilerDirectives.*;

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
     * Read a line of input, newline included in result. Returns null if {@link #isInteractive() ==
     * false}. The rationale for including the readline is to ensure that the accumulated input,
     * whether it be from a file or the console accurately reflects the the source. TODO worry about
     * "\r\n"?
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
