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

import org.junit.*;

import com.oracle.truffle.r.test.*;

// Checkstyle: stop line length check

public class TestBuiltin_levels extends TestBase {

    @Test
    public void testlevels1() {
        assertEval("argv <- structure(list(x = structure(c(1L, 1L, 1L, 1L, 1L, 1L,     1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L),     .Label = c('1', '2'), class = 'factor')), .Names = 'x');"
                        + "do.call('levels', argv)");
    }

    @Test
    public void testLevels() {
        assertEval("{ x <- 1 ; levels(x)<-\"a\"; levels(x);}");
        assertEval("{ x <- 5 ; levels(x)<-\"catdog\"; levels(x);}");
        assertEval("{ x <- 1 ; levels(x)<-NULL; levels(x)}");
        assertEval("{ x <- 1 ; levels(x)<-1; levels(x);}");
        assertEval("{ x <- 1 ; levels(x)<-4.5; levels(x);}");
        assertEval("{ x <- 1 ; levels(x)<-c(1); levels(x);}");
        assertEval("{ x <- 5 ; levels(x)<-c(1,2,3); levels(x);}");
        assertEval("{ x <- 1 ; levels(x)<-c(\"cat\", \"dog\"); levels(x)}");
        assertEval("{ x <- 1 ; levels(x)<-c(3, \"cat\"); levels(x);}");
        assertEval("{ x <- 1 ; levels(x)<-c(1, \"cat\", 4.5, \"3\"); levels(x);}");
        assertEval("{ x <- 1 ; levels(x)<-NULL; levels(notx)}");
        assertEval(Ignored.Unknown, "{ x <- NULL; levels(x)<-\"dog\"; levels(x)}");
    }
}
