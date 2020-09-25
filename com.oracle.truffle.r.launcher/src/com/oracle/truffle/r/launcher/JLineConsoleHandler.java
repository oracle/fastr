/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.launcher;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;

import org.graalvm.polyglot.Context;
import org.graalvm.shadowed.org.jline.reader.EndOfFileException;
import org.graalvm.shadowed.org.jline.reader.History;
import org.graalvm.shadowed.org.jline.reader.LineReader;
import org.graalvm.shadowed.org.jline.reader.LineReaderBuilder;
import org.graalvm.shadowed.org.jline.reader.UserInterruptException;
import org.graalvm.shadowed.org.jline.reader.impl.DefaultParser;
import org.graalvm.shadowed.org.jline.reader.impl.history.DefaultHistory;
import org.graalvm.shadowed.org.jline.terminal.Terminal;
import org.graalvm.shadowed.org.jline.terminal.TerminalBuilder;

public class JLineConsoleHandler extends ConsoleHandler {
    private final History history = new DefaultHistory();
    private final InputStream in;
    private final OutputStream out;
    private final Terminal terminal;
    private LineReader reader;

    private final boolean noPrompt;
    private String prompt;
    private int currentLine;

    public JLineConsoleHandler(InputStream inStream, OutputStream outStream, boolean noPrompt) throws IOException {
        this.noPrompt = noPrompt;
        this.in = inStream;
        this.out = outStream;
        this.terminal = terminal();
    }

    @Override
    public void setContext(Context context) {
        reader = LineReaderBuilder.builder().terminal(terminal).history(history).completer(new JLineConsoleCompleter(context)).parser(new ParserWithCustomDelimiters()).build();
    }

    @Override
    public String readLine() {
        try {
            currentLine++;
            return reader.readLine(prompt);
        } catch (UserInterruptException ex) {
            // interrupted by ctrl-c
            return "";
        } catch (EndOfFileException ex) {
            // interrupted by ctrl-d
            return null;
        } catch (Throwable ex) {
            if (ex.getCause() instanceof InterruptedIOException || ex.getCause() instanceof InterruptedException) {
                // seen this with ctrl-c
                return "";
            }
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void setPrompt(String prompt) {
        this.prompt = noPrompt ? "" : prompt != null ? prompt : "";
    }

    @Override
    public String getPrompt() {
        return prompt;
    }

    public void clearHistory() {
        try {
            history.purge();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void addToHistory(String input) {
        history.add(input);
    }

    public String[] getHistory() {
        String[] result = new String[history.size()];
        for (int i = 0; i < history.size(); i++) {
            result[i] = history.get(i);
        }
        return result;
    }

    @Override
    public int getCurrentLineIndex() {
        return currentLine;
    }

    private Terminal terminal() throws IOException {
        return TerminalBuilder.builder().jna(false).streams(in, out).system(true).signalHandler(Terminal.SignalHandler.SIG_IGN).build();
    }

    public class ParserWithCustomDelimiters extends DefaultParser {

        private char[] delimiters = {'(', ','};

        public String getDelimiters() {
            return new String(delimiters);
        }

        public void setDelimiters(String delimiters) {
            this.delimiters = delimiters.toCharArray();
        }

        @Override
        public boolean isDelimiterChar(CharSequence buffer, int pos) {
            char c = buffer.charAt(pos);
            if (Character.isWhitespace(c)) {
                return true;
            }
            for (char delimiter : delimiters) {
                if (c == delimiter) {
                    return true;
                }
            }
            return false;
        }
    }
}
