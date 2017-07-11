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
package com.oracle.truffle.r.launcher;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.graalvm.polyglot.Context;

import jline.console.ConsoleReader;
import jline.console.UserInterruptException;
import jline.console.completer.CandidateListCompletionHandler;
import jline.console.completer.CompletionHandler;
import jline.console.history.MemoryHistory;

public class JLineConsoleHandler extends ConsoleHandler {
    private final ConsoleReader console;
    private final MemoryHistory history;
    private final boolean noPrompt;

    public JLineConsoleHandler(InputStream inStream, OutputStream outStream, boolean noPrompt) {
        this.noPrompt = noPrompt;
        try {
            console = new ConsoleReader(inStream, outStream);
            history = new MemoryHistory();
            console.setHistory(history);
            console.setHandleUserInterrupt(true);
            console.setExpandEvents(false);
        } catch (IOException ex) {
            throw RCommand.fatal(ex, "unexpected error opening console reader");
        }
    }

    @Override
    public void setContext(Context context) {
        console.addCompleter(new JLineConsoleCompleter(context));
        CompletionHandler completionHandler = console.getCompletionHandler();
        if (completionHandler instanceof CandidateListCompletionHandler) {
            ((CandidateListCompletionHandler) completionHandler).setPrintSpaceAfterFullCompletion(false);
        }
    }

    @Override
    public String readLine() {
        try {
            console.getTerminal().init();
            return console.readLine();
        } catch (UserInterruptException e) {
            throw e;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void setPrompt(String prompt) {
        console.setPrompt(noPrompt ? "" : prompt);
    }

    public void clearHistory() {
        history.clear();
    }

    public void addToHistory(String input) {
        history.add(input);
    }

    public String[] getHistory() {
        String[] result = new String[history.size()];
        for (int i = 0; i < history.size(); i++) {
            result[i] = history.get(i).toString();
        }
        return result;
    }
}
