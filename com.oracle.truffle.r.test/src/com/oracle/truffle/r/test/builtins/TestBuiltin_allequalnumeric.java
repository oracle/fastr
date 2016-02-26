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

public class TestBuiltin_allequalnumeric extends TestBase {

    @Test
    public void testallequalnumeric1() {
        assertEval("argv <- structure(list(target = -13.053274367453, current = -13.053274367453,     tolerance = 8e-16), .Names = c('target', 'current', 'tolerance'));" +
                        "do.call('all.equal.numeric', argv)");
    }

    @Test
    public void testallequalnumeric2() {
        assertEval("argv <- structure(list(target = c(0, 8, 8, 9, 10, 10, 10, 10,     10, 10, 12, 12, 12, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13,     14, 14, 14, 14, 15, 15, 15, 16, 16, 16, 16, 16, 16, 16, 16,     16, 16, 16, 16, 17, 17, 17, 18, 18, 19, 19, 20, 20, 20, 20,     21, 21, 21, 21, 22, 24, 24, 24, 24, 25, 27, 28, 28, 29, 29,     29, 29, 30, 31, 32, 32, 33, 33, 36, 36, 36, 37, 37, 39, 39,     40, 40, 41, 41, 42, 42, 42, 42, 44, 44, 46, 46, 47, 48, 48,     48, 49, 49, 51, 51, 52, 52, 52, 52, 53, 55, 57, 57, 57, 57,     57, 60, 60, 60, 60, 60, 61, 61, 61, 61, 62, 63, 66, 68, 69,     69, 69, 71, 71, 71, 72, 73, 73, 74, 74, 75, 75, 75, 76, 76,     77, 77, 77, 77, 77, 79, 79, 79, 79, 80, 80, 80, 80, 81, 82,     82, 83, 84, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85,     85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 86, 86, 86, 86, 86,     86, 86, 86, 86, 86, 86, 86, 87, 87, 88, 88, 88, 88, 88, 100,     1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17,     18, 19, 20, 21, 22, 24, 24, 25, 27, 27, 28, 29, 30, 31, 32,     33, 34, 36, 36, 37, 39, 39, 40, 41, 42, 43, 44, 46, 46, 47,     48, 49, 51, 51, 52, 53, 54, 55, 57, 57, 59, 59, 60, 61, 62,     63, 64, 66, 66, 68, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77,     79, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92,     93, 94, 95, 96, 97, 98, 99, 100), current = c(0L, 8L, 8L,     9L, 10L, 10L, 10L, 10L, 10L, 10L, 12L, 12L, 12L, 13L, 13L,     13L, 13L, 13L, 13L, 13L, 13L, 13L, 13L, 14L, 14L, 14L, 14L,     15L, 15L, 15L, 16L, 16L, 16L, 16L, 16L, 16L, 16L, 16L, 16L,     16L, 16L, 16L, 17L, 17L, 17L, 18L, 18L, 19L, 19L, 20L, 20L,     20L, 20L, 21L, 21L, 21L, 21L, 22L, 24L, 24L, 24L, 24L, 25L,     27L, 28L, 28L, 29L, 29L, 29L, 29L, 30L, 31L, 32L, 32L, 33L,     33L, 36L, 36L, 36L, 37L, 37L, 39L, 39L, 40L, 40L, 41L, 41L,     42L, 42L, 42L, 42L, 44L, 44L, 46L, 46L, 47L, 48L, 48L, 48L,     49L, 49L, 51L, 51L, 52L, 52L, 52L, 52L, 53L, 55L, 57L, 57L,     57L, 57L, 57L, 60L, 60L, 60L, 60L, 60L, 61L, 61L, 61L, 61L,     62L, 63L, 66L, 68L, 69L, 69L, 69L, 71L, 71L, 71L, 72L, 73L,     73L, 74L, 74L, 75L, 75L, 75L, 76L, 76L, 77L, 77L, 77L, 77L,     77L, 79L, 79L, 79L, 79L, 80L, 80L, 80L, 80L, 81L, 82L, 82L,     83L, 84L, 85L, 85L, 85L, 85L, 85L, 85L, 85L, 85L, 85L, 85L,     85L, 85L, 85L, 85L, 85L, 85L, 85L, 85L, 85L, 85L, 85L, 85L,     86L, 86L, 86L, 86L, 86L, 86L, 86L, 86L, 86L, 86L, 86L, 86L,     87L, 87L, 88L, 88L, 88L, 88L, 88L, 100L, 1L, 2L, 3L, 4L,     5L, 6L, 7L, 8L, 9L, 10L, 11L, 12L, 13L, 14L, 15L, 16L, 17L,     18L, 19L, 20L, 21L, 22L, 24L, 24L, 25L, 27L, 27L, 28L, 29L,     30L, 31L, 32L, 33L, 34L, 36L, 36L, 37L, 39L, 39L, 40L, 41L,     42L, 43L, 44L, 46L, 46L, 47L, 48L, 49L, 51L, 51L, 52L, 53L,     54L, 55L, 57L, 57L, 59L, 59L, 60L, 61L, 62L, 63L, 64L, 66L,     66L, 68L, 68L, 69L, 70L, 71L, 72L, 73L, 74L, 75L, 76L, 77L,     79L, 79L, 80L, 81L, 82L, 83L, 84L, 85L, 86L, 87L, 88L, 89L,     90L, 91L, 92L, 93L, 94L, 95L, 96L, 97L, 98L, 99L, 100L),     tolerance = 2.22044604925031e-14), .Names = c('target', 'current',     'tolerance'));" +
                        "do.call('all.equal.numeric', argv)");
    }

    @Test
    public void testallequalnumeric3() {
        assertEval("argv <- structure(list(target = structure(c(1L, 2L, 3L, 4L, 5L,     6L, 7L, 8L, 9L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 3L,     4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L, 4L, 5L, 6L, 7L, 8L, 9L,     10L, 11L, 12L), .Dim = c(9L, 4L), .Dimnames = list(c('c.1',     'c.2', 'c.3', 'c.4', 'c.5', 'c.6', 'c.7', 'c.8', 'c.9'),     c('A', 'B', 'C', 'D'))), current = structure(c(1, 2, 3, 4,     5, 6, 7, 8, 9, 2.00000000000001, 3, 4, 5, 6, 7, 8, 9, 10,     3.00000000000001, 4, 5, 6, 7, 8, 9, 10, 11, 4.00000000000001,     5, 6, 7, 8, 9, 10, 11, 12), .Dim = c(9L, 4L), .Dimnames = list(c('c.1',     'c.2', 'c.3', 'c.4', 'c.5', 'c.6', 'c.7', 'c.8', 'c.9'),     c('A', 'B', 'C', 'D'))), tolerance = 1e-12), .Names = c('target',     'current', 'tolerance'));" +
                        "do.call('all.equal.numeric', argv)");
    }

    @Test
    public void testallequalnumeric4() {
        assertEval("argv <- structure(list(target = 3.18309886183776e-301, current = 3.18309886183791e-301,     tolerance = 1e-15), .Names = c('target', 'current', 'tolerance'));" +
                        "do.call('all.equal.numeric', argv)");
    }

}
