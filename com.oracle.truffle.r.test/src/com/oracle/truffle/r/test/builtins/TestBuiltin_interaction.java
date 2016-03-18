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

public class TestBuiltin_interaction extends TestBase {

    @Test
    public void testinteraction1() {
        assertEval("argv <- list(c('a.b', 'a'), c('c', 'b.c'));do.call('interaction', argv)");
    }

    @Test
    public void testInteraction() {
        assertEval("{ a <- gl(2, 4, 8) ; b <- gl(2, 2, 8, labels = c(\"ctrl\", \"treat\")) ; interaction(a, b) }");
        assertEval("{ a <- gl(2, 4, 8) ; b <- gl(2, 2, 8, labels = c(\"ctrl\", \"treat\")) ; s <- gl(2, 1, 8, labels = c(\"M\", \"F\")) ; interaction(a, b, s, sep = \":\") }");
    }
}
