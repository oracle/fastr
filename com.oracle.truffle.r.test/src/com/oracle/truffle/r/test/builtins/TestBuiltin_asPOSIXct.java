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
public class TestBuiltin_asPOSIXct extends TestBase {

    @Test
    public void testasPOSIXct1() {
        assertEval("argv <- list(structure(list(sec = 0, min = 0L, hour = 0L, mday = 1L, mon = 0L, year = 109L, wday = 4L, yday = 0L, isdst = 0L), .Names = c('sec', 'min', 'hour', 'mday', 'mon', 'year', 'wday', 'yday', 'isdst'), class = c('POSIXlt', 'POSIXt'), tzone = 'UTC'), 'UTC'); .Internal(as.POSIXct(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasPOSIXct2() {
        assertEval("argv <- list(structure(list(sec = 0, min = 0L, hour = 0L, mday = 1, mon = c(11, 12, 13, 14), year = 100L, wday = 0L, yday = 365L, isdst = -1L), .Names = c('sec', 'min', 'hour', 'mday', 'mon', 'year', 'wday', 'yday', 'isdst'), class = c('POSIXlt', 'POSIXt'), tzone = 'GMT'), 'GMT'); .Internal(as.POSIXct(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasPOSIXct3() {
        assertEval("argv <- list(structure(list(sec = 0, min = 0L, hour = 0L, mday = -3L, mon = 1L, year = 102L, wday = 6L, yday = 32L, isdst = -1L), .Names = c('sec', 'min', 'hour', 'mday', 'mon', 'year', 'wday', 'yday', 'isdst'), class = c('POSIXlt', 'POSIXt'), tzone = c('', 'EST', 'EDT')), ''); .Internal(as.POSIXct(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasPOSIXct4() {
        assertEval("argv <- list(structure(list(sec = 0, min = 0L, hour = 12L, mday = 1L, mon = 0L, year = c(70L, 75L, 80L, 85L, 90L, 95L, 100L, 105L, 110L, 115L), wday = 4L, yday = 0L, isdst = -1L), .Names = c('sec', 'min', 'hour', 'mday', 'mon', 'year', 'wday', 'yday', 'isdst'), class = c('POSIXlt', 'POSIXt'), tzone = 'GMT'), 'GMT'); .Internal(as.POSIXct(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasPOSIXct5() {
        assertEval("argv <- list(structure(list(sec = numeric(0), min = integer(0), hour = integer(0), mday = integer(0), mon = integer(0), year = integer(0), wday = integer(0), yday = integer(0), isdst = integer(0)), .Names = c('sec', 'min', 'hour', 'mday', 'mon', 'year', 'wday', 'yday', 'isdst'), class = c('POSIXlt', 'POSIXt')), ''); .Internal(as.POSIXct(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasPOSIXct6() {
        assertEval("argv <- list(structure(list(sec = 0, min = 0L, hour = 0L, mday = 1L, mon = 0L, year = -5L, wday = 2L, yday = 0L, isdst = 0L), .Names = c('sec', 'min', 'hour', 'mday', 'mon', 'year', 'wday', 'yday', 'isdst'), class = c('POSIXlt', 'POSIXt'), tzone = 'GMT'), 'GMT'); .Internal(as.POSIXct(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasPOSIXct7() {
        assertEval("argv <- list(structure(list(sec = NA_real_, min = NA_integer_, hour = NA_integer_, mday = NA_integer_, mon = NA_integer_, year = NA_integer_, wday = NA_integer_, yday = NA_integer_, isdst = -1L), .Names = c('sec', 'min', 'hour', 'mday', 'mon', 'year', 'wday', 'yday', 'isdst'), class = c('POSIXlt', 'POSIXt'), tzone = 'GMT'), 'GMT'); .Internal(as.POSIXct(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasPOSIXct8() {
        assertEval("argv <- list(structure(list(sec = 0, min = 0L, hour = 0L, mday = 1L, mon = 0L, year = 70L, wday = 4L, yday = 0L, isdst = 0L), .Names = c('sec', 'min', 'hour', 'mday', 'mon', 'year', 'wday', 'yday', 'isdst'), class = c('POSIXlt', 'POSIXt'), tzone = 'GMT'), 'GMT'); .Internal(as.POSIXct(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasPOSIXct9() {
        assertEval("argv <- list(structure(list(sec = 0, min = 0L, hour = 0L, mday = 22:27, mon = 3L, year = 108L, wday = 2L, yday = 112L, isdst = -1L), .Names = c('sec', 'min', 'hour', 'mday', 'mon', 'year', 'wday', 'yday', 'isdst'), class = c('POSIXlt', 'POSIXt'), tzone = 'GMT'), 'GMT'); .Internal(as.POSIXct(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasPOSIXct10() {
        assertEval("argv <- list(structure(list(sec = 0, min = 0L, hour = 0L, mday = 1, mon = 1L, year = 109L, wday = 0L, yday = 31L, isdst = -1), .Names = c('sec', 'min', 'hour', 'mday', 'mon', 'year', 'wday', 'yday', 'isdst'), class = c('POSIXlt', 'POSIXt'), tzone = 'UTC'), 'UTC'); .Internal(as.POSIXct(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasPOSIXct11() {
        assertEval("argv <- list(structure(list(sec = 0, min = 0L, hour = 0L, mday = 1L, mon = c(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49), year = 105L, wday = 6L, yday = 0L, isdst = -1L), .Names = c('sec', 'min', 'hour', 'mday', 'mon', 'year', 'wday', 'yday', 'isdst'), class = c('POSIXlt', 'POSIXt'), tzone = 'UTC'), 'UTC'); .Internal(as.POSIXct(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasPOSIXct12() {
        assertEval("argv <- list(structure(list(sec = 0, min = 2L, hour = 2L, mday = 2L, mon = 1L, year = c(102L, 1102L), wday = 6L, yday = 32L, isdst = -1L), .Names = c('sec', 'min', 'hour', 'mday', 'mon', 'year', 'wday', 'yday', 'isdst'), class = c('POSIXlt', 'POSIXt'), tzone = c('', 'EST', 'EDT')), ''); .Internal(as.POSIXct(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasPOSIXct13() {
        assertEval("argv <- list(structure(list(sec = 0, min = 0L, hour = 0L, mday = c(2L, 4L, 6L, 8L, 10L, 12L, 14L, 16L, 18L, 20L, 22L, 24L, 26L, 28L, 30L, 32L), mon = 1L, year = 102L, wday = 6L, yday = 32L, isdst = -1L), .Names = c('sec', 'min', 'hour', 'mday', 'mon', 'year', 'wday', 'yday', 'isdst'), class = c('POSIXlt', 'POSIXt'), tzone = c('', 'EST', 'EDT')), ''); .Internal(as.POSIXct(argv[[1]], argv[[2]]))");
    }
}
