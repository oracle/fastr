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
public class TestBuiltin_POSIXlt2Date extends TestBase {

    @Test
    public void testPOSIXlt2Date1() {
        assertEval("argv <- list(structure(list(sec = c(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0), min = c(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L), hour = c(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L), mday = c(1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L), mon = c(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L), year = c(37L, 16L, 13L, 27L, 47L, 13L, 17L, 23L, 21L, 26L, 20L, 15L, 14L, 14L, 14L, 19L, 48L, 11L, 9L, 13L, 25L, 26L, 10L, 17L, 36L, 38L, 60L, 15L, 19L, 24L, 14L, 5L, 21L, 29L, 26L, 21L, 8L, 28L, 19L, 21L, 25L, 34L, 27L, 28L, 34L, 22L, 23L, 15L, 34L, 25L, 22L, 30L, 24L, 23L, 19L, 32L, 30L, 23L, 30L, 22L, 19L, 32L, 39L, 23L, 20L, 19L, 52L, 27L, 24L, 19L, 25L, 45L, 16L, 43L, 20L, 20L, 31L, 24L, 19L, 26L, 20L, 42L, 19L, 30L, 25L, 24L, 26L, 18L, 22L, 21L, 25L, 28L, 25L, 29L, 33L, 47L, 50L, 45L, 24L, 39L, 24L, 33L, 28L), wday = c(5L, 6L, 3L, 6L, 3L, 3L, 1L, 1L, 6L, 5L, 4L, 5L, 4L, 4L, 4L, 3L, 4L, 0L, 5L, 3L, 4L, 5L, 6L, 1L, 3L, 6L, 5L, 5L, 3L, 2L, 4L, 0L, 6L, 2L, 5L, 6L, 3L, 0L, 3L, 6L, 4L, 1L, 6L, 0L, 1L, 0L, 1L, 5L, 1L, 4L, 0L, 3L, 2L, 1L, 3L, 5L, 3L, 1L, 3L, 0L, 3L, 5L, 0L, 1L, 4L, 3L, 2L, 6L, 2L, 3L, 4L, 1L, 6L, 5L, 4L, 4L, 4L, 2L, 3L, 5L, 4L, 4L, 3L, 3L, 4L, 2L, 5L, 2L, 0L, 6L, 4L, 0L, 4L, 2L, 0L, 3L, 0L, 1L, 2L, 0L, 2L, 0L, 0L), yday = c(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L), isdst = c(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 1L, 0L, 1L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 1L, 0L, 0L, 0L, 0L, 0L)), .Names = c('sec', 'min', 'hour', 'mday', 'mon', 'year', 'wday', 'yday', 'isdst'), class = c('POSIXlt', 'POSIXt'))); .Internal(POSIXlt2Date(argv[[1]]))");
    }

    @Test
    public void testPOSIXlt2Date2() {
        assertEval("argv <- list(structure(list(sec = c(0, NA), min = c(0L, NA), hour = c(0L, NA), mday = c(6L, NA), mon = c(10L, NA), year = c(107L, NA), wday = c(2L, NA), yday = c(309L, NA), isdst = c(0L, -1L)), .Names = c('sec', 'min', 'hour', 'mday', 'mon', 'year', 'wday', 'yday', 'isdst'), class = c('POSIXlt', 'POSIXt'))); .Internal(POSIXlt2Date(argv[[1]]))");
    }

    @Test
    public void testPOSIXlt2Date3() {
        assertEval("argv <- list(structure(list(sec = numeric(0), min = integer(0), hour = integer(0), mday = integer(0), mon = integer(0), year = integer(0), wday = integer(0), yday = integer(0), isdst = integer(0)), .Names = c('sec', 'min', 'hour', 'mday', 'mon', 'year', 'wday', 'yday', 'isdst'), class = c('POSIXlt', 'POSIXt'))); .Internal(POSIXlt2Date(argv[[1]]))");
    }

    @Test
    public void testPOSIXlt2Date4() {
        assertEval("argv <- list(structure(list(sec = 0, min = 0L, hour = 0L, mday = 1L, mon = 0L, year = 79L, wday = 1L, yday = 0L, isdst = 0L), .Names = c('sec', 'min', 'hour', 'mday', 'mon', 'year', 'wday', 'yday', 'isdst'), class = c('POSIXlt', 'POSIXt'))); .Internal(POSIXlt2Date(argv[[1]]))");
    }

    @Test
    public void testPOSIXlt2Date5() {
        assertEval("argv <- list(structure(list(sec = c(0, NA, NA, 0), min = c(0L, NA, NA, 0L), hour = c(0L, NA, NA, 0L), mday = c(1L, NA, NA, 26L), mon = c(0L, NA, NA, 9L), year = c(101L, NA, NA, 104L), wday = c(1L, NA, NA, 2L), yday = c(0L, NA, NA, 299L), isdst = c(0L, -1L, -1L, 0L)), .Names = c('sec', 'min', 'hour', 'mday', 'mon', 'year', 'wday', 'yday', 'isdst'), class = c('POSIXlt', 'POSIXt'), tzone = 'GMT')); .Internal(POSIXlt2Date(argv[[1]]))");
    }

    @Test
    public void testPOSIXlt2Date6() {
        assertEval("argv <- list(structure(list(sec = 0, min = 0L, hour = 0L, mday = 1L, mon = c(0, 3, 6, 9, 12, 15, 18, 21, 24, 27, 30, 33, 36), year = 100L, wday = 6L, yday = 0L, isdst = 0L), .Names = c('sec', 'min', 'hour', 'mday', 'mon', 'year', 'wday', 'yday', 'isdst'), class = c('POSIXlt', 'POSIXt'), tzone = 'UTC')); .Internal(POSIXlt2Date(argv[[1]]))");
    }

    @Test
    public void testPOSIXlt2Date7() {
        assertEval("argv <- list(structure(list(sec = c(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0), min = c(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L), hour = c(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L), mday = 1, mon = c(0, 0.5, 1, 1.5, 2, 2.5, 3), year = c(101, 101, 101, 101, 101, 101, 101, 102, 102, 102, 102, 102, 102, 102), wday = c(3L, 0L, 6L, 4L, 2L, 0L, 5L, 3L, 1L, 6L, 4L), yday = c(9L, 111L, 12L, 24L, 36L, 48L, 60L, 72L, 84L, 96L, 108L), isdst = c(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L)), .Names = c('sec', 'min', 'hour', 'mday', 'mon', 'year', 'wday', 'yday', 'isdst'), class = c('POSIXlt', 'POSIXt'), tzone = 'UTC')); .Internal(POSIXlt2Date(argv[[1]]))");
    }

    @Test
    public void testPOSIXlt2Date8() {
        assertEval("argv <- list(structure(list(sec = 0, min = 0L, hour = 0L, mday = 1L, mon = 0L, year = 109L, wday = 4L, yday = 0L, isdst = 0L), .Names = c('sec', 'min', 'hour', 'mday', 'mon', 'year', 'wday', 'yday', 'isdst'), class = c('POSIXlt', 'POSIXt'))); .Internal(POSIXlt2Date(argv[[1]]))");
    }

    @Test
    public void testPOSIXlt2Date9() {
        assertEval("argv <- list(structure(list(sec = c(0, 0, 0, 0, 0), min = c(0L, 0L, 0L, 0L, 0L), hour = c(0L, 0L, 0L, 0L, 0L), mday = 22:26, mon = c(3L, 3L, 3L, 3L, 3L), year = c(108L, 108L, 108L, 108L, 108L), wday = 2:6, yday = 112:116, isdst = c(-1L, -1L, -1L, -1L, -1L)), .Names = c('sec', 'min', 'hour', 'mday', 'mon', 'year', 'wday', 'yday', 'isdst'), class = c('POSIXlt', 'POSIXt'), tzone = 'GMT')); .Internal(POSIXlt2Date(argv[[1]]))");
    }

    @Test
    public void testPOSIXlt2Date10() {
        assertEval("argv <- list(structure(list(sec = 33.1798663139343, min = 47L, hour = 14L, mday = 17L, mon = 2L, year = 114L, wday = 1L, yday = 75L, isdst = 1L), .Names = c('sec', 'min', 'hour', 'mday', 'mon', 'year', 'wday', 'yday', 'isdst'), class = c('POSIXlt', 'POSIXt'), tzone = c('', 'EST', 'EDT'))); .Internal(POSIXlt2Date(argv[[1]]))");
    }
}
