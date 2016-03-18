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

public class TestBuiltin_signif extends TestBase {

    @Test
    public void testsignif1() {
        assertEval(Ignored.Unknown, "argv <- list(structure(c(0, NaN, 0, 4.94065645841247e-324), class = 'integer64'));do.call('signif', argv)");
    }

    @Test
    public void testSignif() {
        assertEval("{ signif(0.555, 2) }");
        assertEval("{ signif(0.5549, 2) }");
        assertEval("{ signif(0.5551, 2) }");
        assertEval("{ signif(0.555, 0) }");
        assertEval("{ signif(0.555, -1) }");
        assertEval("{ signif(0.0005551, 2) }");
    }
}
