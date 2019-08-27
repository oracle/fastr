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
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check
public class TestBuiltin_printfunction extends TestBase {

    @Test
    public void testprintfunction1() {
        // FIXME maybe we could show arg names
        // Expected output: function (e1, e2) .Primitive("+")
        // FastR output: function (null, null) .Primitive("+")
        assertEval(Ignored.OutputFormatting, "argv <- list(.Primitive('+'), TRUE); .Internal(print.function(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testprintfunction2() {
        // Expected output: .Primitive("if")
        // FastR output: function(x).Primitive("if")
        assertEval(Ignored.OutputFormatting, "argv <- list(.Primitive('if'), TRUE); .Internal(print.function(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testprintfunction3() {
        // ?c shows 'recursive' arg so marked as ReferenceError
        // Expected output: function (...) .Primitive("c")
        // FastR output: function (..., recursive) .Primitive("c")
        assertEval(Ignored.ReferenceError, "argv <- list(.Primitive('c'), TRUE); .Internal(print.function(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testprintfunction4() {
        assertEval(Ignored.NewRVersionMigration, "argv <- list(.Primitive('.Internal'), TRUE); .Internal(print.function(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testprintfunction5() {
        // FIXME exp(1) sligthly better than 2.718282 but definitely a minor issue
        // Expected output: function (x, base = exp(1)) .Primitive("log")
        // FastR output: function (x, base = 2.718282) .Primitive("log")
        assertEval(Ignored.OutputFormatting, "argv <- list(.Primitive('log'), TRUE); .Internal(print.function(argv[[1]], argv[[2]]))");
    }
}
