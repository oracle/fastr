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
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check

public class TestBuiltin_signif extends TestBase {

    @Test
    public void testsignif1() {
        // FIXME RInternalError: java.lang.NumberFormatException: Infinite or NaN
        assertEval(Ignored.ImplementationError, "argv <- list(structure(c(0, NaN, 0, 4.94065645841247e-324), class = 'integer64'));do.call('signif', argv)");
    }

    @Test
    public void testSignif() {
        assertEval("{ signif(8.175, 3) }");
        assertEval("{ signif(8.125, 3) }");
        assertEval("{ signif(0.555, 2) }");
        assertEval("{ signif(0.5549, 2) }");
        assertEval("{ signif(0.5551, 2) }");
        assertEval("{ signif(0.555, 0) }");
        assertEval("{ signif(0.555, -1) }");
        assertEval("{ signif(0.0005551, 2) }");

        assertEval("{ signif(42.1234, \"2\") }");
        assertEval("{ signif(42.1234, as.raw(2)) }");
        assertEval("{ signif(42.1234, 42+7i) }");
        assertEval(Output.IgnoreErrorMessage, "{ signif(42.1234, character()) }");
        assertEval("{ signif(\"42.1234\", 2) }");

        assertEval("{ signif(c(42.1234, 7.1234), 1:2) }");
        assertEval("{ signif(42.1234, 1:2) }");
        assertEval("{ signif(c(42.1234, 7.1234), 1) }");
        assertEval("{ signif(c(42.1234, 7.1234, 42.1234), c(1,2)) }");
        assertEval("{ x<-42.1234; attr(x, \"foo\")<-\"foo\"; signif(x, 2) }");
        assertEval("{ x<-FALSE; attr(x, \"foo\")<-\"foo\"; signif(x, 2) }");
        assertEval("{ signif(42.1234, matrix(1:2, nrow=1)) }");

        assertEval("{ signif(c(1.234, 2.345, 3.456, 4.567), c(1, NA)); }");
        assertEval("{ signif(c(1.234, NA, 3.456, 4.567), c(1, NA)); }");

        assertEval("{ signif(1.234+2.345i, 2) }");
        assertEval("{ signif(Inf+2.345i, 2) }");
        assertEval("{ signif(complex(re=1.234, im=2.345), 2) }");
    }
}
