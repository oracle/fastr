/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.*;

import com.oracle.truffle.r.test.*;

// Checkstyle: stop line length check

public class TestBuiltin_isnaPOSIXlt extends TestBase {

    @Test
    public void testisnaPOSIXlt1() {
        assertEval("argv <- structure(list(x = structure(list(sec = 0, min = 0L,     hour = 0L, mday = 11L, mon = 7L, year = 3L, wday = 2L, yday = 222L,     isdst = 0L, zone = 'EST', gmtoff = NA_integer_), .Names = c('sec',     'min', 'hour', 'mday', 'mon', 'year', 'wday', 'yday', 'isdst',     'zone', 'gmtoff'), class = c('POSIXlt', 'POSIXt'), tzone = c('EST5EDT',     'EST', 'EDT'))), .Names = 'x');" +
                        "do.call('is.na.POSIXlt', argv)");
    }
}
