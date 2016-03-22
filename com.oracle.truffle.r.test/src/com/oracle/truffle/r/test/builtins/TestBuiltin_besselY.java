/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check
public class TestBuiltin_besselY extends TestBase {

    @Test
    public void testbesselY1() {
        assertEval(Ignored.Unknown,
                        "argv <- list(c(9.5367431640625e-07, 1.9073486328125e-06, 3.814697265625e-06, 7.62939453125e-06, 1.52587890625e-05, 3.0517578125e-05, 6.103515625e-05, 0.0001220703125, 0.000244140625, 0.00048828125, 0.0009765625, 0.001953125, 0.00390625, 0.0078125, 0.015625, 0.03125, 0.0625, 0.125, 0.25, 0.5, 1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024), 20.5); .Internal(besselY(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testbesselY2() {
        assertEval(Ignored.Unknown,
                        "argv <- list(2, c(3, 8.94, 14.88, 20.82, 26.76, 32.7, 38.64, 44.58, 50.52, 56.46, 62.4, 68.34, 74.28, 80.22, 86.16, 92.1, 98.04, 103.98, 109.92, 115.86, 121.8, 127.74, 133.68, 139.62, 145.56, 151.5, 157.44, 163.38, 169.32, 175.26, 181.2, 187.14, 193.08, 199.02, 204.96, 210.9, 216.84, 222.78, 228.72, 234.66, 240.6, 246.54, 252.48, 258.42, 264.36, 270.3, 276.24, 282.18, 288.12, 294.06, 300)); .Internal(besselY(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testbesselY3() {
        assertEval(Ignored.Unknown,
                        "argv <- list(c(0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1, 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 1.9, 2, 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8, 2.9, 3, 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 3.8, 3.9, 4, 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7, 4.8, 4.9, 5, 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.7, 5.8, 5.9, 6, 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 6.7, 6.8, 6.9, 7, 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 7.7, 7.8, 7.9, 8, 8.1, 8.2, 8.3, 8.4, 8.5, 8.6, 8.7, 8.8, 8.9, 9, 9.1, 9.2, 9.3, 9.4, 9.5, 9.6, 9.7, 9.8, 9.9, 10), -0.2); .Internal(besselY(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testbesselY4() {
        assertEval(Ignored.Unknown, "argv <- list(logical(0), logical(0)); .Internal(besselY(argv[[1]], argv[[2]]))");
    }
}
