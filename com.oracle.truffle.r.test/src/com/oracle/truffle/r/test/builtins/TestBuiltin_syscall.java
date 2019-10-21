/*
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * Copyright (c) 2012-2014, Purdue University
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import static com.oracle.truffle.r.test.builtins.TestBuiltin_sysparent.SYS_PARENT_SETUP;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

public class TestBuiltin_syscall extends TestBase {

    private static final String DONT_KEEP_SOURCE_OPTION = "options(keep.source = FALSE);";

    @Test
    public void testSysCall() {
        // GR-18929
        assertEval(DONT_KEEP_SOURCE_OPTION + "{ bar.default<-function(a)sys.call(); bar<-function(a)UseMethod('bar'); bar(a=42); }");
        assertEval(DONT_KEEP_SOURCE_OPTION + "{ bar.default<-function(a,...,b)sys.call(); bar<-function(a,x,...)UseMethod('bar'); bar(1,x=2,b=3,c=4); }");

        // Remaining issues ignored with NewRVersionMigration: GR-18931

        assertEval("{ f <- function() sys.call() ; f() }");
        assertEval("{ f <- function(x) sys.call() ; f(x = 2) }");
        assertEval("{ f <- function() sys.call(1) ; g <- function() f() ; g() }");
        assertEval(DONT_KEEP_SOURCE_OPTION + "{ f <- function() sys.call(2) ; g <- function() f() ; h <- function() g() ; h() }");
        assertEval("{ f <- function() sys.call(1) ; g <- function() f() ; h <- function() g() ; h() }");
        assertEval(DONT_KEEP_SOURCE_OPTION + "{ f <- function() sys.call(-1) ; g <- function() f() ; h <- function() g() ; h() }");
        assertEval("{ f <- function() sys.call(-2) ; g <- function() f() ; h <- function() g() ; h() }");
        assertEval(DONT_KEEP_SOURCE_OPTION + "{ f <- function() sys.call() ; g <- function() f() ; h <- function() g() ; h() }");

        assertEval("{ f <- function() sys.call() ; typeof(f()[[1]]) }");
        assertEval("{ f <- function(x) sys.call() ; typeof(f(x = 2)[[1]]) }");
        assertEval("{ f <- function(x) sys.call() ; typeof(f(x = 2)[[2]]) }");

        assertEval("{ f <- function(x) sys.call() ; f(2) }");
        assertEval("{ f <- function(x) sys.call() ; g <- function() 23 ; f(g()) }");

        assertEval("{ f <- function(x, y) sys.call() ; f(1, 2) }");
        assertEval(DONT_KEEP_SOURCE_OPTION + "{ f <- function(x, y) sys.call() ; f(x=1, 2) }");
        assertEval(DONT_KEEP_SOURCE_OPTION + "{ f <- function(x, y) sys.call() ; f(1, y=2) }");
        assertEval(DONT_KEEP_SOURCE_OPTION + "{ f <- function(x, y) sys.call() ; f(y=1, 2) }");
        assertEval(DONT_KEEP_SOURCE_OPTION + "{ f <- function(x, y) sys.call() ; f(y=1, x=2) }");

        // fails because can't parse out the "name"
        assertEval(Output.IgnoreWhitespace, "{ (function() sys.call())() }");

        assertEval("{ foo<-function(x, z) UseMethod(\"foo\"); foo.baz<-function(x, z) NextMethod(); y<-1; class(y)<-c(\"baz\", \"bar\"); foo.bar<-function(x, z) sys.call(0); foo(y, 42) }");
        assertEval("{ foo<-function(x, ...) UseMethod(\"foo\"); foo.baz<-function(x, ...) NextMethod(); y<-1; class(y)<-c(\"baz\", \"bar\"); foo.bar<-function(x, ...) sys.call(0); foo(y, 42) }");

        // these tests look a little weird as we seem to have some printing problems with language
        // objects (we should be able to simply print x, but the outputs don't quite match)
        assertEval("{ x<-do.call(function() sys.call(0), list()); x[[1]] }");

        // whitespace in formatting of deparsed function
        assertEval(Output.IgnoreWhitespace, "{ x<-(function(f) f())(function() sys.call(1)); list(x[[1]], x[[2]][[1]], x[[2]][[2]], x[[2]][[3]]) }");

        assertEval(DONT_KEEP_SOURCE_OPTION + "{ f <- function() sys.call() ; g <- function(a=1,b=3,...) f() ; h <- function(q=33) g() ; h() }");
    }

    @Test
    public void frameAccessCommonTest() {
        // Note: we remove 4 and 7 from the result only due to different formatting. Code
        // sys.call(4) and sys.call(7) is still executed.
        assertEval(DONT_KEEP_SOURCE_OPTION + "{ foo <- function(x) lapply(1:7, function(i) sys.call(i))[c(-4,-7)];" + SYS_PARENT_SETUP + "}");
    }
}
