/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

import jline.console.*;

import com.oracle.truffle.api.CompilerDirectives.*;
import com.oracle.truffle.r.runtime.*;

public class JLineConsoleHandler implements RContext.ConsoleHandler {
    final ConsoleReader console;
    final boolean isInteractive;
    final PrintWriter printWriter;

    JLineConsoleHandler(boolean isInteractive, ConsoleReader console) {
        this.console = console;
        printWriter = new PrintWriter(console.getOutput());
        this.isInteractive = isInteractive;
    }

    @SlowPath
    public void println(String s) {
        printWriter.println(s);
        printWriter.flush();
    }

    @SlowPath
    public void print(String s) {
        printWriter.print(s);
        printWriter.flush();
    }

    @SlowPath
    public void printf(String format, Object... args) {
        printWriter.format(format, args);
    }

    @SlowPath
    public String readLine() {
        try {
            return console.readLine();
        } catch (IOException ex) {
            Utils.fail("unexpected error reading console input");
            return null;
        }
    }

    public boolean isInteractive() {
        return isInteractive;
    }

    @SlowPath
    public void printErrorln(String s) {
        println(s);
    }

    @SlowPath
    public void printError(String s) {
        print(s);
    }

    public void redirectError() {
    }

    @SlowPath
    public String getPrompt() {
        return console.getPrompt();
    }

    @SlowPath
    public void setPrompt(String prompt) {
        console.setPrompt(prompt);
    }

    public int getWidth() {
        return RContext.CONSOLE_WIDTH;
    }

}
