/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2012-2014, Purdue University
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check
public class TestBuiltin_unique extends TestBase {

    @Test
    public void testunique1() {
        assertEval("argv <- list(character(0), FALSE, FALSE, NA); .Internal(unique(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testunique2() {
        // FIXME FastR wrongly considers 'NA' and NA equal
        assertEval(Ignored.ImplementationError, "argv <- list(c('a', 'b', 'c', 'c', 'b', 'a', 'NA', 'd', 'd', NA), FALSE, FALSE, NA); .Internal(unique(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testunique3() {
        assertEval("argv <- list(c(1, 2, 4, 6, 8, 9, 11, 13, 14, 16, 3, 5, 7, 9, 10, 12, 14, 15, 17, 17), FALSE, FALSE, NA); .Internal(unique(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testunique4() {
        assertEval("argv <- list(list(c(9L, 9L), c(9L, 9L), c(9L, 9L), c(9L, 9L), c(9L, 9L), c(9L, 9L)), FALSE, FALSE, NA); .Internal(unique(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testunique5() {
        assertEval("argv <- list(structure(c(1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 3L, 3L, 3L, 3L, 4L, 4L, 4L, 4L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 3L, 3L, 3L, 3L, 4L, 4L, 4L, 4L), .Label = c('Brown', 'Blue', 'Hazel', 'Green'), class = 'factor'), FALSE, FALSE, 5L); .Internal(unique(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testunique6() {
        assertEval("argv <- list(c('colors', 'colours'), FALSE, FALSE, NA); .Internal(unique(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testunique7() {
        assertEval("argv <- list(structure(list(a = 1), .Names = 'a'), FALSE, FALSE, NA); .Internal(unique(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testunique8() {
        assertEval("argv <- list(c(1, 258, 516, 774, 1032, 1290, 1548, 1806, 2064, 2322, 2580, 2838, 3096, 3354, 3612, 3870, 4128, 4386, 4644, 4902, 5160, 1, 259, 517, 775, 1033, 1291, 1549, 1807, 2065, 2323, 2581, 2839, 3097, 3355, 3613, 3871, 4129, 4387, 4645, 4903, 5160), FALSE, FALSE, NA); .Internal(unique(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testunique9() {
        assertEval("argv <- list(list(FALSE), FALSE, FALSE, NA); .Internal(unique(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testunique10() {
        assertEval("argv <- list(c(1L, 2L, 2L, 2L, 1L, 1L, 2L, 1L, 2L, 2L, 2L, 2L, 1L, 2L, 1L, 1L, 2L, 1L, 1L, 2L, 1L, 1L, 1L, 2L, 1L, 1L, 2L, 1L, 2L, 2L, 1L, 1L, 2L, 1L, 1L, 1L, 2L, 1L, 2L, 1L, 1L, 1L, 1L, 2L, 1L, 2L, 2L, 1L, 1L, 2L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 1L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 1L, 2L, 2L, 2L, 1L, 1L, 2L, 1L, 1L, 2L), FALSE, FALSE, NA); .Internal(unique(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testunique11() {
        // FIXME ordering wrong
        assertEval(Ignored.ImplementationError,
                        "argv <- list(structure(c(20.65, NA, NA, 40.25, 61.9, 55.27, 58.13, 54.04, 21.15, 18.32, NA, 65.84, 58.77, 53.99, 63.37, 64.81, 47.11, 9.65, 67.1, 48.83, 57.92, 69.5, 73.95, 5.46, 49.92, 54.21, 61.38, 56.66, 60.14, 56.68, NA, 53.13, 39.7, 74.83, 59.73, NA, 67.06, 67.99, 60.6, 4.63, 71.09, 43.4, 21.9, 61.45, 77.98, 36.67, 69.95, 55.26, 63.24, NA), .Names = c('Alabama', 'Alaska', 'Arizona', 'Arkansas', 'California', 'Colorado', 'Connecticut', 'Delaware', 'Florida', 'Georgia', 'Hawaii', 'Idaho', 'Illinois', 'Indiana', 'Iowa', 'Kansas', 'Kentucky', 'Louisiana', 'Maine', 'Maryland', 'Massachusetts', 'Michigan', 'Minnesota', 'Mississippi', 'Missouri', 'Montana', 'Nebraska', 'Nevada', 'New Hampshire', 'New Jersey', 'New Mexico', 'New York', 'North Carolina', 'North Dakota', 'Ohio', 'Oklahoma', 'Oregon', 'Pennsylvania', 'Rhode Island', 'South Carolina', 'South Dakota', 'Tennessee', 'Texas', 'Utah', 'Vermont', 'Virginia', 'Washington', 'West Virginia', 'Wisconsin', 'Wyoming')), FALSE, FALSE, NA); .Internal(unique(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testunique12() {
        assertEval("argv <- list(c(1.5, 1.5, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60), FALSE, FALSE, NA); .Internal(unique(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testunique13() {
        assertEval("argv <- list(list(NULL, NULL, NULL, NULL, NULL), FALSE, FALSE, NA); .Internal(unique(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testunique14() {
        assertEval("argv <- list(c(9.18429112061858e-05, 0.0238094009226188, 0.0498038685764186), FALSE, FALSE, NA); .Internal(unique(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testunique15() {
        assertEval("argv <- list(structure(c(1L, 1L, 1L, 1L, 3L, 3L, 3L, 3L, 2L, 2L, 2L, 2L, 2L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 3L, 3L, 3L, 3L, 3L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 3L, 3L, 1L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 2L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 2L, 3L, 3L, 3L, 3L, 3L, 1L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 2L, 1L, 1L, 1L, 1L, 1L, 3L, 3L, 3L, 3L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 3L, 3L, 3L, 3L, 3L, 2L, 2L, 2L, 2L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 2L, 3L, 3L, 3L, 3L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 2L, 1L, 1L, 1L, 1L, 1L, 3L, 3L, 2L, 2L, 2L, 2L, 1L, 1L, 1L, 2L, 2L, 2L, 1L, 1L, 1L, 1L, 1L, 3L, 3L, 3L, 3L, 3L, 1L, 1L, 1L, 1L, 1L, 3L, 3L, 2L, 2L, 2L, 2L, 2L, 3L, 3L, 3L, 3L, 1L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 2L, 3L, 3L, 3L, 3L, 3L, 2L, 2L, 2L, 3L, 3L, 3L, 3L, 3L, 2L, 2L, 2L, 2L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L), contrasts = structure(c(-0.666666666666667, 0.333333333333333, 0.333333333333333, -0.333333333333333, -0.333333333333333, 0.666666666666667), .Dim = c(3L, 2L), .Dimnames = list(c('placebo', 'drug', 'drug+'), c('drug', 'encourage'))), .Label = c('placebo', 'drug', 'drug+'), class = 'factor'), FALSE, FALSE, 4L); .Internal(unique(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testunique16() {
        assertEval("argv <- list(c(TRUE, FALSE, FALSE, TRUE, TRUE, TRUE, TRUE, FALSE, FALSE, TRUE, TRUE, FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, FALSE, FALSE, FALSE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, FALSE, FALSE, FALSE, TRUE, TRUE, TRUE, TRUE, TRUE, FALSE, TRUE, TRUE, TRUE, TRUE, FALSE, FALSE, TRUE, TRUE, FALSE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, FALSE, TRUE, FALSE, TRUE, FALSE, TRUE, TRUE, TRUE, TRUE, TRUE, NA, TRUE, NA, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, NA, TRUE, NA, TRUE, TRUE, NA, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, NA, NA, TRUE, TRUE, TRUE, TRUE, TRUE, NA, TRUE, NA, TRUE, NA, TRUE, TRUE, TRUE, TRUE, NA, TRUE, TRUE, NA, TRUE, NA, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, FALSE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, NA, NA, FALSE, TRUE, TRUE, NA, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE), FALSE, FALSE, NA); .Internal(unique(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testunique17() {
        assertEval("argv <- list(c('2.21', '7.6', '18.19', '19.78', '20.23', '27.01', '28.06', '29.28', '32.79', '37.06', '39.72', '41.26', '41.76', '42.5', '42.82', '43.59', '45.29', '47.09', '47.12', '47.68', '48.52', '48.93', '49.26', '49.45', '49.58', '49.69', '51.01', '51.18', '52.24', '52.39', '55.06', '55.25', '55.76', '57.02', '57.21', '57.71', '58.33', '58.84', '59.63', '59.83', '61.54', '62.16', '62.26', '65.35', '72.03', '75.37', '78.22', NA), FALSE, FALSE, NA); .Internal(unique(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testunique18() {
        // FIXME ordering wrong
        assertEval(Ignored.ImplementationError, "argv <- list(c(3, 4, 5, 11, 10, 9, 8, 8, 9, 10, 11, 12, 13), FALSE, TRUE, NA); .Internal(unique(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testunique19() {
        assertEval("argv <- list(structure(c(1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4), .Tsp = c(1945, 1974.75, 4), class = 'ts'), FALSE, FALSE, NA); .Internal(unique(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testunique20() {
        assertEval("argv <- list(c(4L, 6L, 9L, 15L, NA), FALSE, FALSE, NA); .Internal(unique(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testunique21() {
        assertEval("argv <- list(structure(list(A = c(3L, 5L), B = c(3L, 5L), C = c(3L, 5L), D = c(3L, 5L)), .Names = c('A', 'B', 'C', 'D')), FALSE, FALSE, NA); .Internal(unique(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testunique22() {
        assertEval("argv <- list(c(25, 50, 100, 250, 500, 1e+05), FALSE, FALSE, NA); .Internal(unique(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testunique23() {
        assertEval("argv <- list(c(1, 2, NA, 2), FALSE, FALSE, NA); .Internal(unique(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testunique24() {
        assertEval("argv <- list(list('numeric_version', 'numeric_version'), FALSE, FALSE, NA); .Internal(unique(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testunique25() {
        assertEval("argv <- list(structure(list(c0 = structure(integer(0), .Label = character(0), class = 'factor')), .Names = 'c0', row.names = character(0), class = structure('integer(0)', .Names = 'c0')), FALSE, FALSE, NA); .Internal(unique(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testunique26() {
        assertEval("argv <- list(list('Math2', 'round', 'signif'), FALSE, FALSE, NA); .Internal(unique(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testunique27() {
        assertEval("argv <- list(list(structure('Math2', package = 'methods'), 'round', 'signif'), FALSE, FALSE, NA); .Internal(unique(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testunique28() {
        assertEval("argv <- list(structure(c(1L, 1L, 1L, 1L), .Names = c('vector', 'data.frameRowLabels', 'SuperClassMethod', 'atomicVector')), FALSE, FALSE, NA); .Internal(unique(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testunique29() {
        assertEval("argv <- list(NULL, FALSE, FALSE, NA); .Internal(unique(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testunique30() {
        assertEval("argv <- list(c(TRUE, FALSE, TRUE, TRUE), FALSE, FALSE, NA); .Internal(unique(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testunique31() {
        assertEval("argv <- list(c(2L, 1L, NA), FALSE, FALSE, NA); .Internal(unique(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testunique33() {
        assertEval("argv <- structure(list(x = structure(c(15, 37, 30, 18, 8, 20,     42.7, 29.3), .Dim = c(4L, 2L), .Dimnames = structure(list(Evaluation = c('very good',     'good', 'bad', 'very bad'), Location = c('city centre', 'suburbs')),     .Names = c('Evaluation', 'Location')))), .Names = 'x');" +
                        "do.call('unique', argv)");
    }

    @Test
    public void testUnique() {
        assertEval("{x<-factor(c(\"a\", \"b\", \"a\")); unique(x) }");

        assertEval("{ x<-quote(f(7, 42)); unique(x) }");
        assertEval("{ x<-function() 42; unique(x) }");
        // FastR msg "invalid 'incomparables' argument"
        // seems a bit better than GnuR's "cannot coerce type 'closure' to vector of type 'double'"
        assertEval(Ignored.ReferenceError, "{ unique(c(1,2,1), incomparables=function() 42) }");

    }
}
