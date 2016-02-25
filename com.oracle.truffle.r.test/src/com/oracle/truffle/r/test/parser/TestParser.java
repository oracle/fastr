/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.test.parser;

import java.io.File;

import org.antlr.runtime.RecognitionException;
import org.junit.Test;

import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.r.nodes.RASTBuilder;
import com.oracle.truffle.r.parser.RParser;
import com.oracle.truffle.r.test.TestBase;

public class TestParser extends TestBase {

    @Test
    public void testOpName() {
        assertEval("{ \"%??%\" <- function(x,y) x + y; 7 %??% 42 }");
    }

    @Test
    public void testNegativePow() {
        assertEval("10^-2");
        assertEval("10^+2");
        assertEval("10^1");
        assertEval("10^1.5");
        assertEval("10^(1+1)");
        assertEval("10^1+1");
        assertEval("10^2^2");
    }

    @Test
    public void testSpaceEscapeSequence() {
        assertEval("\"\\ \" == \" \"");
        assertEval("'\\ ' == ' '");
    }

    /**
     * Recursively look for .r source files in the args[0] directory and parse them.
     */
    public static void main(String[] args) {
        recurse(new File(args[0]));
        System.out.println("errors: " + errorCount);
    }

    static int errorCount;

    @SuppressWarnings("deprecation")
    private static void recurse(File file) {
        assert file.exists();
        if (file.isDirectory()) {
            for (File sub : file.listFiles()) {
                recurse(sub);
            }
        } else {
            String name = file.getName();
            if (name.endsWith(".r") || name.endsWith(".R")) {
                Source source = null;
                try {
                    source = Source.fromURL(file.toURL(), file.getName());
                    new RParser<>(source, new RASTBuilder()).script();
                } catch (Throwable e) {
                    errorCount++;
                    Throwable t = e;
                    while (t.getCause() != null && t.getCause() != t) {
                        t = t.getCause();
                    }
                    System.out.println("Error while parsing " + file.getAbsolutePath());
                    if (t instanceof RecognitionException) {
                        RecognitionException rec = (RecognitionException) t;
                        System.out.println(source.getCode(rec.line));
                        System.out.printf("%" + rec.charPositionInLine + "s^%n", "");
                    }
                    System.out.println(t);
                    if (!t.getStackTrace()[0].getMethodName().equals("unimplemented")) {
                        System.out.println(t.getStackTrace()[0]);
                    } else {
                        System.out.println(t.getStackTrace()[1]);
                    }
                    // e.printStackTrace();
                    System.out.println();
                }
            }
        }
    }
}
