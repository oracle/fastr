/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;

import com.oracle.truffle.api.TruffleLanguage.Env;

public class DefaultConsoleHandler implements ConsoleHandler {

    private final BufferedReader in;
    private final OutputStreamWriter out;

    public DefaultConsoleHandler(Env env) {
        in = new BufferedReader(new InputStreamReader(env.in()));
        out = new OutputStreamWriter(env.out());
    }

    public void println(String s) {
        try {
            out.append(s).append('\n');
            out.flush();
        } catch (IOException e) {
            // TODO: handle this error
        }
    }

    public void print(String s) {
        try {
            out.append(s).append('\n');
            out.flush();
        } catch (IOException e) {
            // TODO: handle this error
        }
    }

    public void printErrorln(String s) {
        try {
            out.append(s).append('\n');
            out.flush();
        } catch (IOException e) {
            // TODO: handle this error
        }
    }

    public void printError(String s) {
        try {
            out.append(s).append('\n');
            out.flush();
        } catch (IOException e) {
            // TODO: handle this error
        }
    }

    public String readLine() {
        try {
            return in.readLine();
        } catch (IOException e) {
            // TODO: handle this error
            return null;
        }
    }

    public boolean isInteractive() {
        return true;
    }

    public void redirectError() {
        // ?
    }

    public String getPrompt() {
        return "> ";
    }

    public void setPrompt(String prompt) {
        // ?
    }

    public int getWidth() {
        return 80;
    }

    public String getInputDescription() {
        return "<PolyglotEngine env input>";
    }
}
