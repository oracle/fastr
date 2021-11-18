/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.test.library.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check
public class TestInteractiveDebug extends TestBase {
    // See GR-23700, the time it takes to instrument all nodes has significantly increased, which
    // means that we may reach the default timeout frequently
    private static final long TIMEOUT = 30000;

    private static Path debugFile;

    @BeforeClass
    public static void setup() throws IOException {
        Path testDir = TestBase.createTestDir("com.oracle.truffle.r.test.library.utils.rsrc");
        String content = "bar <- function(x) print(x)\n\nfun <- function(x) {\nprint('Hello')\nfor(i in seq(3)) print(i)\nbar('World')\nprint(x)\n}";
        debugFile = testDir.resolve("debug.r");
        Files.write(debugFile, content.getBytes(), StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    @Test
    public void testSimple() {
        assertEval(TIMEOUT, Context.NonShared, "f <- function(x) {\n  t <- x + 1\n  print(t)\n  t}\ndebug(f)\nf(5)\nx\nn\nn\nt\nn\nn\nundebug(f)");
        assertEval(TIMEOUT, Context.NonShared, "f <- function(x) {\n  t <- x + 1\n  print(t)\n  t}\ndebug(f)\nf(5)\nx\nn\nn\nt\nn\nn\nundebug(f)\nf(3)\ndebug(f)\nf(5)\nx\nn\nn\nt\nn\nn");
    }

    @Test
    public void testInvalidName() {
        assertEval(TIMEOUT, Context.NonShared, "f <- function(x) {\n  `123t` <- x + 1\n  print(`123t`)\n  `123t`}\ndebug(f)\nf(5)\nx\nn\nn\n`123t`\nn\nn\nundebug(f)");
    }

    @Test
    public void testNoBracket() {
        assertEval(TIMEOUT, Context.NonShared, Ignored.OutputFormatting, "f <- function(x) print(x)\ndebug(f)\nf(5)\nx\nn\nundebug(f)");
    }

    @Test
    public void testLoop() {
        assertEval(TIMEOUT, Context.NonShared, "fun <- function(x) { for(i in seq(x)) cat(i); cat(5) }; debug(fun); fun(3)\n\n\n\n\n\n\nundebug(fun)");
        assertEval(TIMEOUT, Context.NonShared, "fun <- function(x) { for(i in seq(x)) { cat(i) }; cat(5) }; debug(fun); fun(3)\n\n\n\n\n\n\nundebug(fun)");
        assertEval(TIMEOUT, Context.NonShared, "fun <- function(x) { for(i in seq(x)) { cat(i) }; cat(5) }; debug(fun); fun(3)\n\n\n\nf\n\nundebug(fun)");
        assertEval(TIMEOUT, Context.NonShared, "fun <- function(x) { for(j in seq(2)) for(i in seq(x)) cat(i); cat(5) }; debug(fun); fun(3)\n\n\n\n\n\n\n\n\n\n\n\nundebug(fun)");
        assertEval(TIMEOUT, Context.NonShared, "fun <- function(x) { for(j in seq(2)) for(i in seq(x)) cat(i); cat(5) }; debug(fun); fun(3)\n\n\n\nf\nf\n\nundebug(fun)");
    }

    @Test
    public void testStepInto() {
        assertEval(TIMEOUT, Context.NonShared, Ignored.OutputFormatting,
                        "bar <- function(x) { cat(x); cat('\\n') }; foo <- function(x) { cat('foo entry\\n'); bar(x); cat('foo exit\\n') }; debug(foo); foo(3)\n\n\ns\nn\n\n\n\n");
        assertEval(TIMEOUT, Context.NonShared, "bar <- function(x) { cat(x); cat('\\n') }; foo <- function(x) { cat('foo entry\\n'); bar(x); cat('foo exit\\n') }; debug(foo); foo(3)\n\n\ns\nn\nQ\n");
        assertEval(TIMEOUT, Context.NonShared, Ignored.OutputFormatting,
                        "bar <- function(x) { for(i in seq(x)) print(x) }; foo <- function(x) { cat('foo entry\\n'); bar(x); cat('foo exit\\n') }; debug(foo); foo(5)\n\n\ns\nn\n\n\nf\n\n");
    }

    @Test
    public void testPromise() {
        assertEval(TIMEOUT, Context.NonShared, "fun <- function(x) { cat({ cat(x); cat('\n') }) }; debug(fun); fun(3)\n\n\n\n\nundebug(fun)");
    }

    /**
     * FIXME: Ignored, because the shared context between unit tests does not handle debugging more functions at once
     * correctly.
     */
    @Test
    @Ignore
    public void testNestedDebugging() {
        assertEval(TIMEOUT, Context.NonShared, Output.IgnoreDebugPath,
                        "foo <- function(rCode) { eval(parse(text=rCode)); print('foo done') }; debug(foo); foo(\"bar <- function() { print('bar') }; debug(bar); bar()\")\n\n\n\n\n\n");
        assertEval(TIMEOUT, Context.NonShared, Output.IgnoreDebugPath,
                        "foo <- function(rCode) { eval(parse(text=rCode)); print('foo done') }; debug(foo); foo(\"bar <- function() { print('bar') }; debug(bar); bar()\")\n\n\nQ\n");
    }

    @Test
    public void testConditionalBreakpoint() {
        assertEval(TIMEOUT, Context.NonShared, "fun <- function(x) { cat('x='); cat(x); cat('\\n') }; trace(fun, quote(if (x > 10) browser())); fun(10)\n; fun(11)\n\n\n\n\n\n");
    }

    @Test
    public void testContinue() {
        assertEval(TIMEOUT, Context.NonShared, Ignored.OutputFormatting,
                        "fun0 <- function() { print('fun0') }; fun1 <- function() { print('enter fun1'); fun0(); print('exit fun1') }; fun2 <- function() { print('enter fun2'); fun1(); print('exit fun2') }; debug(fun2); fun2()\n\n\ns\nn\n\ns\nc\nc\nc\n");
    }

    @Test
    public void testDebugOnce() {
        assertEval(TIMEOUT, Context.NonShared, Ignored.OutputFormatting, "fun0 <- function() { print('fun0') }; fun1 <- function() { print('en'); fun0(); fun0(); print('ex') }; debugonce(fun0); fun1()\nc\n");
        assertEval(TIMEOUT, Context.NonShared, Ignored.OutputFormatting, "fun0 <- function() { print('fun0') }; debugonce(fun0); fun0()\nc\n");
    }

    @Test
    public void testBrowser() {
        assertEval(TIMEOUT, Context.NonShared, "foo <- function() { stop('error msg') }; tryCatch(foo(), error=browser)\nprint(msg)\nc\n");
        assertEval(TIMEOUT, Context.NonShared, "do.call('browser', list())\nc\n");
        assertEval(TIMEOUT, Context.NonShared, "browser()\nwhere\nc\n");
        assertEval(TIMEOUT, Context.NonShared, "options(error=browser); prod('a')\nwhere\nc\n");
        assertEval(TIMEOUT, Context.NonShared,
                        "{ bar <- function(vbar) do.call(browser, list(), envir = parent.frame(2));" +
                                        "baz <- function(vbaz) bar(vbaz);" +
                                        "start <- function(vstart) baz(vstart);" +
                                        "start(42); }\nls()\nc");
        assertEval(TIMEOUT, Context.NonShared, "{ foo <- function(x) browser(\"hello\", condition=4+x, expr=x>3); foo(1); }");
        assertEval(TIMEOUT, Context.NonShared, "{ foo <- function(x) browser(\"hello\", condition=4+x, expr=x>3); foo(5); }\nbrowserText()\nbrowserCondition()\nc");
    }

    @Test
    public void testSetBreakpoint() {
        assertEval(TIMEOUT, Context.NonShared, Output.IgnoreDebugCallString, Output.IgnoreDebugPath, String.format("source('%s'); setBreakpoint('%s', 4, verbose=F); fun(10)\n\n\n\n\n\n\n\n", debugFile, debugFile));
        assertEval(TIMEOUT, Context.NonShared, String.format("source('%s'); setBreakpoint('%s', 4, verbose=F); setBreakpoint('%s', 4, verbose=F, clear=T); fun(10)\n", debugFile, debugFile, debugFile));
        assertEval(TIMEOUT, Context.NonShared, Output.IgnoreDebugCallString, Output.IgnoreDebugPath, String.format(
                        "source('%s'); setBreakpoint('%s', 4, verbose=F); fun(10)\n\n\n\n\n\n\n\n\nsetBreakpoint('%s', 4, verbose=F, clear=T); fun(10)\nsetBreakpoint('%s', 4, verbose=F); invisible(fun(10))\n\n\n\n\n\n\n\n\n",
                        debugFile, debugFile, debugFile, debugFile));
    }
}
