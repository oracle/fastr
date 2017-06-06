/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates
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
        assertEval("argv <- list(.Primitive('.Internal'), TRUE); .Internal(print.function(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testprintfunction5() {
        // FIXME exp(1) sligthly better than 2.718282 but definitely a minor issue
        // Expected output: function (x, base = exp(1)) .Primitive("log")
        // FastR output: function (x, base = 2.718282) .Primitive("log")
        assertEval(Ignored.OutputFormatting, "argv <- list(.Primitive('log'), TRUE); .Internal(print.function(argv[[1]], argv[[2]]))");
    }
}
