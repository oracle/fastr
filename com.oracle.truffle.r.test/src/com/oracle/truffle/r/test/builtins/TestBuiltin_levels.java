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

public class TestBuiltin_levels extends TestBase {

    @Test
    public void testlevels1() {
        assertEval("argv <- structure(list(x = structure(c(1L, 1L, 1L, 1L, 1L, 1L,     1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L),     .Label = c('1', '2'), class = 'factor')), .Names = 'x');" +
                        "do.call('levels', argv)");
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
        assertEval(Output.MayIgnoreErrorContext, "{ x <- 1 ; levels(x)<-NULL; levels(notx)}");
        assertEval("{ x <- NULL; levels(x)<-\"dog\"; levels(x)}");
    }
}
