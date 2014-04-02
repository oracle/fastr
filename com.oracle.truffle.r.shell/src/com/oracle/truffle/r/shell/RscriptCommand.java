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
package com.oracle.truffle.r.shell;

import java.io.*;

import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;

/**
 * Emulates the (Gnu)Rscript command as precisely as possible.
 */
public class RscriptCommand {
    // CheckStyle: stop system..print check
    public static void main(String[] args) {
        evalFileInput(args[0], new String[0]);
    }

    private static void evalFileInput(String filePath, String[] commandArgs) {
        File file = new File(filePath);
        if (!file.exists()) {
            fail("Fatal error: cannot open file '" + filePath + "': No such file or directory");
        }
        try (BufferedInputStream is = new BufferedInputStream(new FileInputStream(file))) {
            byte[] bytes = new byte[(int) file.length()];
            is.read(bytes);
            String content = new String(bytes);
            VirtualFrame frame = REngine.initialize(commandArgs, new SysoutConsoleHandler());
            REngine.parseAndEval(content, frame, true);
        } catch (IOException ex) {
            fail("unexpected error reading file input");
        }

    }

    static void fail(String msg) {
        System.err.println(msg);
        System.exit(1);
    }

    private static class SysoutConsoleHandler implements RContext.ConsoleHandler {
        private PrintStream err = System.err;

        public void println(String s) {
            System.out.println(s);
        }

        public void print(String s) {
            System.out.print(s);
        }

        public void printErrorln(String s) {
            err.println(s);
        }

        public void printError(String s) {
            err.print(s);
        }

        public String readLine() {
            fail("input not possible");
            return null;
        }

        public boolean isInteractive() {
            return false;
        }

        public void redirectError() {
            err = System.out;
        }

        public void setPrompt(String prompt) {
        }

        public int getWidth() {
            return RContext.CONSOLE_WIDTH;
        }

    }

}
