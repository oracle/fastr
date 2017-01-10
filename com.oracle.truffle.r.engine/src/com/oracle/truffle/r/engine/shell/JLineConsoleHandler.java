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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.runtime.RSource;
import com.oracle.truffle.r.runtime.RStartParams;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.context.ConsoleHandler;

import jline.console.ConsoleReader;
import jline.console.UserInterruptException;
import jline.console.history.FileHistory;
import jline.console.history.History;

class JLineConsoleHandler implements ConsoleHandler {
    private final ConsoleReader console;
    private final boolean isInteractive;
    private final PrintWriter printWriter;

    JLineConsoleHandler(RStartParams startParams, InputStream inStream, OutputStream outStream) {
        try {
            console = new ConsoleReader(inStream, outStream);
            console.setHandleUserInterrupt(true);
            console.setExpandEvents(false);
        } catch (IOException ex) {
            throw Utils.rSuicide("unexpected error opening console reader");
        }
        // long start = System.currentTimeMillis();
        printWriter = new PrintWriter(console.getOutput());
        this.isInteractive = startParams.getInteractive();
    }

    @Override
    @TruffleBoundary
    public void println(String s) {
        printWriter.println(s);
        printWriter.flush();
    }

    @Override
    @TruffleBoundary
    public void print(String s) {
        printWriter.print(s);
        printWriter.flush();
    }

    @Override
    @TruffleBoundary
    public String readLine() {
        try {
            console.getTerminal().init();
            String line = console.readLine();
            if (line != null) {
                line += "\n";
            }
            return line;
        } catch (UserInterruptException e) {
            throw e;
        } catch (Exception ex) {
            throw Utils.rSuicide("unexpected error reading console input: " + ex);
        }
    }

    @Override
    public boolean isInteractive() {
        return isInteractive;
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
        return console.getPrompt();
    }

    @Override
    @TruffleBoundary
    public void setPrompt(String prompt) {
        console.setPrompt(prompt);
    }

    @Override
    public String getInputDescription() {
        return RSource.Internal.SHELL_INPUT.string;
    }

    @Override
    public void setHistoryFrom(File file) {
        try {
            console.setHistory(new FileHistory(file));
        } catch (IOException x) {
            // silent - there will simply be no history
        }
    }

    @Override
    public void flushHistory() {
        History history = console.getHistory();
        if (history instanceof FileHistory) {
            try {
                history.removeLast();
                ((FileHistory) history).flush();
            } catch (IOException x) {
                // silent - no history appended
            }
        }
    }
}
