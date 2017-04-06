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
public class TestBuiltin_besselK extends TestBase {

    @Test
    public void testbesselK1() {
        // FIXME RInternalError: not implemented: .Internal besselK
        assertEval(Ignored.Unimplemented, "argv <- list(FALSE, FALSE, 1); .Internal(besselK(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testbesselK2() {
        // FIXME RInternalError: not implemented: .Internal besselK
        assertEval(Ignored.Unimplemented, "argv <- list(logical(0), logical(0), 1); .Internal(besselK(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testbesselK3() {
        // FIXME RInternalError: not implemented: .Internal besselK
        assertEval(Ignored.Unimplemented,
                        "argv <- list(c(9.5367431640625e-07, 1.9073486328125e-06, 3.814697265625e-06, 7.62939453125e-06, 1.52587890625e-05, 3.0517578125e-05, 6.103515625e-05, 0.0001220703125, 0.000244140625, 0.00048828125, 0.0009765625, 0.001953125, 0.00390625, 0.0078125, 0.015625, 0.03125, 0.0625, 0.125, 0.25, 0.5, 1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024), 3, 1); .Internal(besselK(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testbesselK4() {
        // FIXME RInternalError: not implemented: .Internal besselK
        assertEval(Ignored.Unimplemented,
                        "argv <- list(c(9.5367431640625e-07, 1.9073486328125e-06, 3.814697265625e-06, 7.62939453125e-06, 1.52587890625e-05, 3.0517578125e-05, 6.103515625e-05, 0.0001220703125, 0.000244140625, 0.00048828125, 0.0009765625, 0.001953125, 0.00390625, 0.0078125, 0.015625, 0.03125, 0.0625, 0.125, 0.25, 0.5, 1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024), 3.5, 1); .Internal(besselK(argv[[1]], argv[[2]], argv[[3]]))");
    }
}
