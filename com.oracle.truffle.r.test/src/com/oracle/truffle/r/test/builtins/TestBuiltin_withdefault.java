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

public class TestBuiltin_withdefault extends TestBase {

    @Test
    public void testwithdefault1() {
        assertEval(Ignored.Unknown,
                        "argv <- structure(list(data = structure(list(X = 22.1693750707316,     Y = -0.652127930273561, Z = 1.03034043827436, a = -2.66666666666667,     b = -10, c = 28), .Names = c('X', 'Y', 'Z', 'a', 'b', 'c')),     expr = expression({        dX <- a * X + Y * Z        dY <- b * (Y - Z)        dZ <- -X * Y + c * Y - Z        list(c(dX, dY, dZ))    })), .Names = c('data', 'expr'));" +
                                        "do.call('with.default', argv)");
    }
}
