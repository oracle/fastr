/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.engine.shell;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.runtime.context.ConsoleHandler;
import com.oracle.truffle.r.runtime.context.RContext;

class StringConsoleHandler implements ConsoleHandler {
    private final PrintStream output;
    private final List<String> lines;
    private final String inputDescription;
    private String prompt;
    private int currentLine;
    private RContext ctx;

    StringConsoleHandler(List<String> lines, OutputStream output, String inputDescription) {
        this.lines = lines;
        this.output = output instanceof PrintStream ? (PrintStream) output : new PrintStream(output);
        this.inputDescription = inputDescription;
    }

    @Override
    @TruffleBoundary
    public void println(String s) {
        output.println(s);
        output.flush();
    }

    @Override
    @TruffleBoundary
    public void print(String s) {
        output.print(s);
        output.flush();
    }

    @Override
    @TruffleBoundary
    public String readLine() {
        if (currentLine < lines.size()) {
            if (prompt != null) {
                output.print(prompt);
                output.println(lines.get(currentLine));
            }
            return lines.get(currentLine++);
        } else {
            return null;
        }
    }

    @Override
    public boolean isInteractive() {
        return false;
    }

    @Override
    @TruffleBoundary
    public void printErrorln(String s) {
        println(s);
    }

    @Override
    @TruffleBoundary
    public void printError(String s) {
        print(s);
    }

    @Override
    @TruffleBoundary
    public String getPrompt() {
        return prompt;
    }

    @Override
    @TruffleBoundary
    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    @Override
    public String getInputDescription() {
        return inputDescription;
    }

    @Override
    public void setContext(RContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public RContext getContext() {
        return ctx;
    }
}
