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
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check
public class TestBuiltin_mget extends TestBase {

    @Test
    public void testMGet() {
        assertEval("{ a<- 1; b <- 2; mget(c(\"a\", \"b\")) }");
        assertEval("{ a<- 1; b <- 2; mget(c('a', 'b'), envir=1) }");
        assertEval("{ a<- 1; b <- 2; f <- function() { mget(c(\"a\", \"b\"), inherits=TRUE)}; f() }");
        assertEval("{ a<- 1; mget(c(\"a\", \"b\"), ifnotfound=list(100)) }");
        assertEval("{ b <- 2; f <- function() { mget(c(\"a\", \"b\"), ifnotfound=list(100), inherits=TRUE)}; f() }");
        assertEval("{ mget(c(\"a\", \"b\"), ifnotfound=list(100, 200)) }");
        assertEval("{ a<- 1; b <- 2; mget(c(\"a\", \"b\"), mode=\"numeric\") }");
        assertEval("{ a<- 1; b <- \"2\"; mget(c(\"a\", \"b\"), mode=c(\"numeric\", \"character\")) }");

        assertEval("{ mget(\"_foo_\", ifnotfound=list(function(x) \"bar\")) }");
        // these tests look a little weird as we seem to have some printing problems with language
        // objects (we should be able to simply print x, but the outputs don't quite match)
        assertEval("{ x<-mget(\"_foo_\", ifnotfound=list(function(x) sys.call(0))); print(x[[1]][[1]]); print(x[[1]][[2]]) }");
        assertEval("{ x<-mget(\"_foo_\", ifnotfound=list(function(x) sys.call(1))); list(x[[1]][[1]], x[[1]][[2]], x[[1]][[3]][[1]], x[[1]][[3]][[2]][[1]], x[[1]][[3]][[2]][[2]], x[[1]][[3]][[2]][[3]]) }");
    }

    @Test
    public void testMGetWithVarArgs() {
        assertEval(template("{ find_args <- function(...) !vapply(mget('...', envir = parent.frame()), function(x) identical(x, quote(expr = )), logical(1));" +
                        "func <- function(...) find_args(...);" +
                        "%0; }", new String[]{"func()", "func(a=3)"}));
    }

    @Test
    public void testWithIfnotfound() {
        assertEval("mget('abc', ifnotfound = list(`[`))");
        assertEval("mget('abc', ifnotfound = list(function(x) cat('NOT FOUND', x, '\\n')))");
        assertEval("mget('abc', ifnotfound = list(function(x, y = 'default value') cat('NOT FOUND', x, ',', y, '\\n')))");

        assertEval("mget('abc', ifnotfound = NA)");
        assertEval("mget('abc', ifnotfound = NA_complex_)");
        assertEval("mget('abc', ifnotfound = 'a')");
        assertEval("mget('abc', ifnotfound = c('a1', 'b1'))");
        assertEval("mget(c('a', 'b'), ifnotfound = c('a1', 'b1'))");
        assertEval("mget(c('a', 'b'), ifnotfound = new.env())");

    }
}
