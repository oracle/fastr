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

public class TestBuiltin_docall extends TestBase {

    @Test
    public void testDoCall() {
        assertEval("{ x<-list(c(1,2)); do.call(\"as.matrix\", x) }");
        assertEval("{ do.call(quote, list(quote(1)))}");
        assertEval("{ do.call(quote, list(quote(x)))}");
        assertEval("{ do.call(quote, list(quote(x+1)))}");
        assertEval(Output.IgnoreErrorContext, "{ f <- function(x) x; do.call(f, list(quote(y)))}");
        assertEval(Output.IgnoreErrorContext, "{ f <- function(x) x; do.call(f, list(quote(y + 1)))}");
        assertEval("{ do.call(\"+\", list(quote(1), 2))}");
        assertEval("v1 <- as.numeric_version('3.0.0'); v2 <- as.numeric_version('3.1.0'); do.call('<', list(v1, v2))");
        assertEval("v1 <- as.numeric_version('3.0.0'); v2 <- as.numeric_version('3.1.0'); do.call('<', list(quote(v1), quote(v2)))");
    }
}
