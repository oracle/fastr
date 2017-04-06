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

public class TestBuiltin_ISOdatetime extends TestBase {

    @Test
    public void testISOdatetime1() {
        // FIXME FastR returns NA
        assertEval(Ignored.ImplementationError,
                        "argv <- structure(list(year = 1970, month = 1, day = 1, hour = 0,     min = 0, sec = 0, tz = 'GMT'), .Names = c('year', 'month',     'day', 'hour', 'min', 'sec', 'tz'));" +
                                        "do.call('ISOdatetime', argv)");
    }

    @Test
    public void testISOdatetime2() {
        // FIXME FastR returns NA
        assertEval(Ignored.ImplementationError,
                        "argv <- structure(list(year = 2002, month = 6, day = 24, hour = 0,     min = 0, sec = 10), .Names = c('year', 'month', 'day', 'hour',     'min', 'sec'));" +
                                        "do.call('ISOdatetime', argv)");
    }
}
