/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.shell;

import java.io.*;
import java.util.*;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.runtime.*;

public class StringConsoleHandler implements RContext.ConsoleHandler {
    private final PrintStream output;
    private final List<String> lines;
    private String prompt;
    private int currentLine;

    public StringConsoleHandler(List<String> lines, PrintStream output) {
        this.lines = lines;
        this.output = output;
    }

    @TruffleBoundary
    public void println(String s) {
        output.println(s);
        output.flush();
    }

    @TruffleBoundary
    public void print(String s) {
        output.print(s);
        output.flush();
    }

    @TruffleBoundary
    public String readLine() {
        if (currentLine < lines.size()) {
            if (prompt != null) {
                output.print(prompt);
                output.println(lines.get(currentLine));
            }
            return lines.get(currentLine++) + "\n";
        } else {
            return null;
        }
    }

    public boolean isInteractive() {
        return false;
    }

    @TruffleBoundary
    public void printErrorln(String s) {
        println(s);
    }

    @TruffleBoundary
    public void printError(String s) {
        print(s);
    }

    public void redirectError() {
    }

    @TruffleBoundary
    public String getPrompt() {
        return prompt;
    }

    @TruffleBoundary
    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public int getWidth() {
        return RContext.CONSOLE_WIDTH;
    }
}
