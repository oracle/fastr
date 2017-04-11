/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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

import org.junit.Test;

import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.r.nodes.RASTBuilder;
import com.oracle.truffle.r.runtime.RParserFactory;
import com.oracle.truffle.r.runtime.RSource;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;
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
    public void testDoubleLiterals() {
        assertEval("0x1.1p2");
        assertEval("0x1p2");
        assertEval("0x0p0");
        assertEval("0x1.aP2");
        assertEval("0xa.p2");
        assertEval("0xa.bp1i");
    }

    private static final String[] HEX_VALUES = new String[]{"0xFFF", "0xFFFFFFFFFFF"};

    @Test
    public void testHexLiterals() {
        for (String sign : new String[]{"", "-", "+"}) {
            for (String suffix : new String[]{"", "L"}) {
                String l = sign + "%0" + suffix;
                assertEval(template(l, HEX_VALUES));
                assertEval(template("typeof(" + l + ")", HEX_VALUES));
            }
        }
    }

    @Test
    public void testNonIntegerQualifiedWithLWarning() {
        assertEval(Ignored.OutputFormatting, "12345678909876543212L; 12345678909876543212L; 12345678909876543212L");
        assertEval(Ignored.OutputFormatting, "123456789098765432121L\n123456789098765432121L");
        assertEval(Ignored.OutputFormatting, "0xFFFFFFFFFFFL; 0xFFFFFFFFFFFL; 0xFFFFFFFFFFFL");
        assertEval(Ignored.OutputFormatting, "0xFFFFFFFFFFFL\n0xFFFFFFFFFFFL");
    }

    @Test
    public void testSpaceEscapeSequence() {
        assertEval("\"\\ \" == \" \"");
        assertEval("'\\ ' == ' '");
    }

    @Test
    public void testNewLinesNesting() {
        assertEval("y <- 2; z <- 5; x <- (y +\n  z)");
        assertEval("y <- 2; z <- 5; x <- (y \n + z)");
        assertEval("y <- 2; z <- 5; x <- ({y +\n  z})");
        assertEval("y <- 2; z <- 5; x <- ({y \n + z})");
        assertEval("y <- 2; z <- 5; x <- (y *\n  z)");
        assertEval("y <- 2; z <- 5; x <- (y \n * z)");
        assertEval("y <- 2; z <- 5; x <- ({y *\n  z})");
        assertEval(Output.IgnoreErrorMessage, "y <- 2; z <- 5; x <- ({y \n * z})");
        assertEval("y <- 2; z <- 5; x <- ({(y *\n  z)})");
        assertEval("y <- 2; z <- 5; x <- ({(y \n * z)})");
        assertEval("a <- 1:100; y <- 2; z <- 5; x <- ({(a[y *\n  z])})");
        assertEval("a <- 1:100; y <- 2; z <- 5; x <- ({(a[[y \n * z]])})");
        assertEval(Output.IgnoreErrorMessage, "a <- 1:100; y <- 2; z <- 5; x <- (a[[{y \n * z}]])");
    }

    @Test
    public void testNewLinesInLiterals() {
        assertEval("`asdf\nasdf` <- 1; `asdf\nasdf`");
        assertEval("'foo\nbar\baz'");
        assertEval("\"foo\nbar\baz\"");
    }

    @Test
    public void testEmptySymbols() {
        assertEval("names(e)");
        assertEval("e <- quote(b(f=1,foo)); names(e) <- c('','f',''); e");
        assertEval("e <- quote(b(f=1,foo)); names(e) <- c('','',''); e");
        assertEval("f <- function(){ function(``=1) 1 }");
        assertEval("f <- function(){ function(``=1) cat(``=1) }");
        assertEval("f <- function(){ function() cat(``=1) }");
        assertEval("f <- function(){ function() cat(asdf=1) }");
        assertEval("f <- function(){ function(``) cat(asdf=1) }");
        assertEval(Output.IgnoreErrorMessage, "f <- function(){ function('') cat(asdf=1) }");
        assertEval("``(1)");
        assertEval("quote(``(1))");
        assertEval("quote(a$b)");
        assertEval("quote(a$``)");
        assertEval("quote(a$'')");
        assertEval("quote(``:::a)");
        assertEval("quote(a:::a)");
        assertEval("quote(a:::``)");
        assertEval("quote(x <- 1)");
        assertEval(Output.IgnoreErrorMessage, "e <- quote(x <- 1); e[[2]] <- as.symbol(''); ");
        assertEval("as.symbol(''))");
    }

    @Test
    public void testLexerError() {
        // FastR provides a more accurate error message
        assertEval(Output.IgnoreErrorMessage, "%0");
    }

    /**
     * Recursively look for .r source files in the args[0] directory and parse them.
     */
    public static void main(String[] args) {
        recurse(new File(args[0]));
        System.out.println("errors: " + errorCount);
    }

    static int errorCount;

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
                RParserFactory.Parser<RSyntaxNode> parser = RParserFactory.getParser();
                try {
                    source = RSource.fromFile(file);
                    parser.script(source, new RASTBuilder());
                } catch (Throwable e) {
                    errorCount++;
                    Throwable t = e;
                    while (t.getCause() != null && t.getCause() != t) {
                        t = t.getCause();
                    }
                    System.out.println("Error while parsing " + file.getAbsolutePath());
                    if (parser.isRecognitionException(t)) {
                        System.out.println(source.getCode(parser.line(t)));
                        System.out.printf("%" + parser.charPositionInLine(t) + "s^%n", "");
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
