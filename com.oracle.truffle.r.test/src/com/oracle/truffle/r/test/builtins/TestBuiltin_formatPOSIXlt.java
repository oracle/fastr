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
public class TestBuiltin_formatPOSIXlt extends TestBase {

    @Test
    public void testformatPOSIXlt1() {
        assertEval("argv <- list(structure(list(sec = c(0, 0, 0, 0, 0), min = c(0L, 0L, 0L, 0L, 0L), hour = c(0L, 0L, 0L, 0L, 0L), mday = c(1L, 1L, 1L, 1L, 1L), mon = c(0L, 0L, 0L, 0L, 0L), year = 105:109, wday = c(6L, 0L, 1L, 2L, 4L), yday = c(0L, 0L, 0L, 0L, 0L), isdst = c(0L, 0L, 0L, 0L, 0L)), .Names = c('sec', 'min', 'hour', 'mday', 'mon', 'year', 'wday', 'yday', 'isdst'), class = c('POSIXlt', 'POSIXt'), tzone = 'UTC'), '%Y-%m-%d', FALSE); .Internal(format.POSIXlt(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testformatPOSIXlt2() {
        assertEval("argv <- list(structure(list(sec = 10.7712235450745, min = 48L, hour = 14L, mday = 17L, mon = 2L, year = 114L, wday = 1L, yday = 75L, isdst = 1L), .Names = c('sec', 'min', 'hour', 'mday', 'mon', 'year', 'wday', 'yday', 'isdst'), class = c('POSIXlt', 'POSIXt'), tzone = c('', 'EST', 'EDT')), '%Y-%m-%d', FALSE); .Internal(format.POSIXlt(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testformatPOSIXlt3() {
        assertEval("argv <- list(structure(list(sec = 59.7693939208984, min = 47L, hour = 18L, mday = 17L, mon = 2L, year = 114L, wday = 1L, yday = 75L, isdst = 0L), .Names = c('sec', 'min', 'hour', 'mday', 'mon', 'year', 'wday', 'yday', 'isdst'), class = c('POSIXlt', 'POSIXt'), tzone = 'GMT'), '%Y-%m-%d %H:%M:%S', TRUE); .Internal(format.POSIXlt(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testformatPOSIXlt4() {
        assertEval(IgnoredJdk.LaterThanJdk8,
                        "argv <- list(structure(list(sec = c(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0), min = c(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L), hour = c(20L, 19L, 19L, 19L, 19L, 19L, 19L, 19L, 19L, 20L, 20L, 20L, 20L, 19L, 19L, 19L, 20L, 20L, 20L, 19L, 20L, 19L, 19L, 19L, 20L), mday = c(30L, 31L, 31L, 31L, 31L, 31L, 31L, 31L, 31L, 30L, 30L, 30L, 30L, 31L, 31L, 31L, 30L, 30L, 30L, 31L, 30L, 31L, 31L, 31L, 30L), mon = c(5L, 11L, 11L, 11L, 11L, 11L, 11L, 11L, 11L, 5L, 5L, 5L, 5L, 11L, 11L, 11L, 5L, 5L, 5L, 11L, 5L, 11L, 11L, 11L, 5L), year = c(72L, 72L, 73L, 74L, 75L, 76L, 77L, 78L, 79L, 81L, 82L, 83L, 85L, 87L, 89L, 90L, 92L, 93L, 94L, 95L, 97L, 98L, 105L, 108L, 112L), wday = c(5L, 0L, 1L, 2L, 3L, 5L, 6L, 0L, 1L, 2L, 3L, 4L, 0L, 4L, 0L, 1L, 2L, 3L, 4L, 0L, 1L, 4L, 6L, 3L, 6L), yday = c(181L, 365L, 364L, 364L, 364L, 365L, 364L, 364L, 364L, 180L, 180L, 180L, 180L, 364L, 364L, 364L, 181L, 180L, 180L, 364L, 180L, 364L, 364L, 365L, 181L), isdst = c(1L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 1L, 1L, 1L, 1L, 0L, 0L, 0L, 1L, 1L, 1L, 0L, 1L, 0L, 0L, 0L, 1L)), .Names = c('sec', 'min', 'hour', 'mday', 'mon', 'year', 'wday', 'yday', 'isdst'), class = c('POSIXlt', 'POSIXt'), tzone = c('', 'EST', 'EDT')), '%Y-%m-%d %H:%M:%S', TRUE); .Internal(format.POSIXlt(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testformatPOSIXlt5() {
        assertEval(IgnoredJdk.LaterThanJdk8,
                        "argv <- list(structure(list(sec = 0, min = 0L, hour = 19L, mday = 31L, mon = 11L, year = 69L, wday = 3L, yday = 364L, isdst = 0L), .Names = c('sec', 'min', 'hour', 'mday', 'mon', 'year', 'wday', 'yday', 'isdst'), class = c('POSIXlt', 'POSIXt'), tzone = c('', 'EST', 'EDT')), '%Y-%m-%d %H:%M:%S', TRUE); .Internal(format.POSIXlt(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testformatPOSIXlt6() {
        assertEval("argv <- list(structure(list(sec = c(0, 0, 0, 0, 0, 0), min = c(0L, 0L, 0L, 0L, 0L, 0L), hour = c(0L, 0L, 0L, 0L, 0L, 0L), mday = 6:11, mon = 0:5, year = -10:-5, wday = c(1L, 6L, 2L, 0L, 4L, 2L), yday = c(5L, 37L, 67L, 98L, 129L, 161L), isdst = c(0L, 0L, 0L, 0L, 0L, 0L)), .Names = c('sec', 'min', 'hour', 'mday', 'mon', 'year', 'wday', 'yday', 'isdst'), class = c('POSIXlt', 'POSIXt'), tzone = 'UTC'), '%Y', FALSE); .Internal(format.POSIXlt(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testformatPOSIXlt7() {
        assertEval("argv <- list(structure(list(sec = c(0, NA, NA, 0), min = c(0L, NA, NA, 0L), hour = c(0L, NA, NA, 0L), mday = c(1L, NA, NA, 26L), mon = c(0L, NA, NA, 9L), year = c(101L, NA, NA, 104L), wday = c(1L, NA, NA, 2L), yday = c(0L, NA, NA, 299L), isdst = c(0L, -1L, -1L, 0L)), .Names = c('sec', 'min', 'hour', 'mday', 'mon', 'year', 'wday', 'yday', 'isdst'), class = c('POSIXlt', 'POSIXt'), tzone = 'UTC'), '%Y-%m-%d', FALSE); .Internal(format.POSIXlt(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testformatPOSIXlt8() {
        assertEval("argv <- list(structure(list(sec = 11.3034093379974, min = 37L, hour = 7L, mday = 7L, mon = 11L, year = 113L, wday = 6L, yday = 340L, isdst = 0L), .Names = c('sec', 'min', 'hour', 'mday', 'mon', 'year', 'wday', 'yday', 'isdst'), class = c('POSIXlt', 'POSIXt'), tzone = c('', 'EST', 'EDT')), '%H:%M:%OS3', FALSE); .Internal(format.POSIXlt(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testformatPOSIXlt9() {
        assertEval("argv <- list(structure(list(sec = numeric(0), min = integer(0), hour = integer(0), mday = integer(0), mon = integer(0), year = integer(0), wday = integer(0), yday = integer(0), isdst = integer(0)), .Names = c('sec', 'min', 'hour', 'mday', 'mon', 'year', 'wday', 'yday', 'isdst'), class = c('POSIXlt', 'POSIXt'), tzone = 'UTC'), '%Y-%m-%d', TRUE); .Internal(format.POSIXlt(argv[[1]], argv[[2]], argv[[3]]))");
    }

    private static final String[] FORMAT_STRINGS = new String[]{
                    "%Y-%m-%d %H:%M:%S %Z",
                    "%Y-%m-%d %H:%M:%S %z",
    };

    // These formats seems to be locale specific and Java is somewhat incompatible to the lib that
    // GNU-R uses, so we only test that some output is produced
    private static final String[] FORMAT_STRINGS_NO_CRASH = new String[]{
                    "%x",
                    "%X",
    };

    @Test
    public void testFormatPOSIXlt() {
        assertEval(template("strftime(as.POSIXct(1547211595, origin='1970-01-01', tz='GMT'), '%0')", FORMAT_STRINGS));
        assertEval(template("nchar(strftime(as.POSIXct(1547211595, origin='1970-01-01', tz='GMT'), '%0')) > 2", FORMAT_STRINGS_NO_CRASH));
    }
}
