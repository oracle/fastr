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
    }
}
