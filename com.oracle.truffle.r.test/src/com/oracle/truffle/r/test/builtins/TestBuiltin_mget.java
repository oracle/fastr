/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2012-2014, Purdue University
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates
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
}
