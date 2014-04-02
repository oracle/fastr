/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.test.generate;

import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;

public class FastRSession implements RSession {

    /**
     * A (virtual) console handler that collects the output in a {@link StringBuilder} for
     * comparison. It does not separate error output as the test analysis doesn't need it.
     */
    private static class ConsoleHandler implements RContext.ConsoleHandler {
        private StringBuilder buffer = new StringBuilder();

        public void println(String s) {
            buffer.append(s);
            buffer.append('\n');
        }

        public void print(String s) {
            buffer.append(s);
        }

        public String readLine() {
            return null;
        }

        public boolean isInteractive() {
            return false;
        }

        public void printErrorln(String s) {
            println(s);
        }

        public void printError(String s) {
            print(s);
        }

        public void redirectError() {
            // always
        }

        public void setPrompt(String prompt) {
            // ignore
        }

        void reset() {
            buffer.delete(0, buffer.length());
        }

        public int getWidth() {
            return RContext.CONSOLE_WIDTH;
        }

    }

    private final ConsoleHandler consoleHandler;

    public FastRSession() {
        consoleHandler = new ConsoleHandler();
        REngine.setRuntimeState(new String[0], consoleHandler, false, false);
    }

    public String eval(String expression) {
        consoleHandler.reset();
        REngine.parseAndEval(expression, true);
        return consoleHandler.buffer.toString();
    }

    public String name() {
        return "FastR";
    }

    public void quit() {
    }

}
