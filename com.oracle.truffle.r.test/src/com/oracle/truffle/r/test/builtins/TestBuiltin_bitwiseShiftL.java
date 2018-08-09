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
public class TestBuiltin_bitwiseShiftL extends TestBase {

    @Test
    public void generated() {
        assertEval("argv <- list(-1, 1:31); .Internal(bitwiseShiftL(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testBitwiseFunctions() {
        assertEval(Output.MayIgnoreErrorContext, "{ .Internal(bitwiseShiftL(, 1))}");
        assertEval(Output.MayIgnoreErrorContext, "{ .Internal(bitwiseShiftL(200, ))}");
        assertEval("{ bitwShiftL(c(10,11,12,13,14,15), c(1,1,1,1,1,1)) }");
        assertEval("{ bitwShiftL(c(100,200,300), 1) }");
        assertEval("{ bitwShiftL(c(25,57,66), c(10,20,30,40,50,60)) }");
        assertEval("{ bitwShiftL(c(8,4,2), NULL) }");

        assertEval(Ignored.NewRVersionMigration, "{ bitwShiftL(TRUE, c(TRUE, FALSE)) }");

        assertEval(Ignored.NewRVersionMigration, "{ bitwShiftL(c(3+3i), c(3,2,4)) }");
        assertEval("{ bitwShiftL(c(3,2,4), c(3+3i)) }");
        assertEval("{ bitwShiftL(c(1,2,3,4), c(\"a\")) }");
    }
}
