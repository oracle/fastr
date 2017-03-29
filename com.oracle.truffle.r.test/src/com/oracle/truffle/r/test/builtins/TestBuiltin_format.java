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
public class TestBuiltin_format extends TestBase {

    @Test
    public void testformat1() {
        assertEval(Output.IgnoreErrorMessage,
                        "argv <- list(structure(c(0, 72.7, 56.4, 72.7, 0, 63.3, 56.4, 63.3, 0), .Dim = c(3L, 3L), .Dimnames = list(c('Girth', 'Height', 'Volume'), c('Girth', 'Height', 'Volume'))), FALSE, 7L, 0L, NULL, 3L, TRUE, NA, \".\"); .Internal(format(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]], argv[[9]], , argv[[9]]))");
    }

    @Test
    public void testformat2() {
        assertEval("argv <- list('\\\\ab\\\\c', FALSE, NULL, 0L, NULL, 3L, FALSE, NA, \".\"); .Internal(format(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]], argv[[9]]))");
    }

    @Test
    public void testformat3() {
        assertEval("argv <- list(c('Inf', '-Inf', 'NaN', 'NA'), FALSE, NULL, 0L, 4, 1L, TRUE, NA, \".\"); .Internal(format(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]], argv[[9]]))");
    }

    @Test
    public void testformat4() {
        assertEval("argv <- list(structure(c('axx', 'b', 'c', 'd', 'e', 'f', 'g', 'h'), .Dim = c(2L, 4L)), FALSE, NULL, 0L, NULL, 1L, TRUE, NA, \".\"); .Internal(format(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]], argv[[9]]))");
    }

    @Test
    public void testformat6() {
        assertEval("argv <- list(structure(c(47.97, 57.9, 74.76, 868.88), .Names = c('<none>', '- x4', '- x2', '- x1')), FALSE, 5L, 0L, NULL, 3L, TRUE, NA, \".\"); .Internal(format(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]], argv[[9]]))");
    }

    @Test
    public void testformat7() {
        assertEval("argv <- list(c('a', 'NA', NA, 'b'), FALSE, NULL, 0L, NULL, 0L, TRUE, NA, \".\"); .Internal(format(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]], argv[[9]]))");
    }

    @Test
    public void testformat8() {
        assertEval("argv <- list(NA_real_, FALSE, 4L, 0L, NULL, 3L, TRUE, NA, \".\"); .Internal(format(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]], argv[[9]]))");
    }

    @Test
    public void testformat9() {
        assertEval("argv <- list(integer(0), TRUE, NULL, 0L, NULL, 3L, TRUE, NA, \".\"); .Internal(format(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]], argv[[9]]))");
    }

    @Test
    public void testformat10() {
        assertEval("argv <- list(c(FALSE, NA, TRUE), FALSE, NULL, 0L, NULL, 3L, FALSE, NA, \".\"); .Internal(format(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]], argv[[9]]))");
    }

    @Test
    public void testformat11() {
        assertEval("argv <- list(structure(c(1L, 2L, 1L), .Dim = 3L, .Dimnames = structure(list(c('1', '2', NA)), .Names = '')), FALSE, 7L, 0L, NULL, 3L, TRUE, NA, \".\"); .Internal(format(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]], argv[[9]]))");
    }

    @Test
    public void testformat12() {
        assertEval("argv <- list(c('Min.', '1st Qu.', 'Median', 'Mean', '3rd Qu.', 'Max.'), FALSE, NULL, 0L, NULL, 0L, TRUE, NA, \".\"); .Internal(format(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]], argv[[9]]))");
    }

    @Test
    public void testformat13() {
        assertEval("argv <- list(c(1L, 2L, 3L, 4L, 5L, -1L, -2L), FALSE, NULL, 0L, NULL, 3L, FALSE, NA, \".\"); .Internal(format(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]], argv[[9]]))");
    }

    @Test
    public void testformat14() {
        assertEval("argv <- list(structure(c(NA, 1, 1, 1), .Names = c('<none>', '- x4', '- x2', '- x1')), FALSE, 5L, 0L, NULL, 3L, TRUE, NA, \".\"); .Internal(format(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]], argv[[9]]))");
    }

    @Test
    public void testformat15() {
        assertEval("argv <- list(2.22044604925031e-16, FALSE, 1, 0L, NULL, 3L, TRUE, NA, \".\"); .Internal(format(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]], argv[[9]]))");
    }

    @Test
    public void testformat16() {
        assertEval("argv <- list(structure(c(1960, 1960, 1960, 1960, 1960, 1960, 1960, 1960, 1960, 1960, 1960, 1961, 1961, 1961, 1961, 1961, 1961, 1961, 1961, 1961), .Tsp = c(1960.08333333333, 1961.66666666667, 12), class = 'ts'), FALSE, NULL, 0L, NULL, 3L, TRUE, NA, \".\"); .Internal(format(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]], argv[[9]]))");
    }

    @Test
    public void testformat17() {
        assertEval("argv <- list(c(2.3e-05, 4.5e-06), FALSE, 5L, 0L, NULL, 3L, TRUE, NA, \".\"); .Internal(format(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]], argv[[9]]))");
    }

    @Test
    public void testformat18() {
        assertEval("argv <- list(c(2L, 4L), FALSE, NULL, 0L, NULL, 3L, FALSE, NA, \".\"); .Internal(format(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]], argv[[9]]))");
    }

    @Test
    public void testformat19() {
        assertEval("argv <- list(c(1L, NA, 1L), FALSE, NULL, 0L, NULL, 3L, FALSE, NA); .Internal(format(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]], argv[[9]]))");
    }

    @Test
    public void testformat20() {
        assertEval("argv <- list(c('abc', NA, 'def'), FALSE, NULL, 0L, NULL, 3L, FALSE, NA, \".\"); .Internal(format(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]], argv[[9]]))");
    }

    @Test
    public void testformat23() {
        assertEval("argv <- list(c(NA, 2L, 4L, 7L), FALSE, NULL, 0L, NULL, 3L, FALSE, NA, \".\"); .Internal(format(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]], argv[[9]]))");
    }

    @Test
    public void testformat24() {
        assertEval("argv <- list(c(1.1+0i, NA, 3+0i), FALSE, NULL, 0L, NULL, 3L, FALSE, NA, \".\"); .Internal(format(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]], argv[[9]]))");
    }

    @Test
    public void testformat25() {
        assertEval("argv <- list(c('  9 ', ' 13 ', ' 13+', ' 18 ', ' 23 ', ' 28+', ' 31 ', ' 34 ', ' 45+', ' 48 '), TRUE, NULL, 0L, NULL, 0L, TRUE, NA, \".\"); .Internal(format(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]], argv[[9]]))");
    }

    @Test
    public void testformat26() {
        assertEval("argv <- list(c(172, 88, 88, 55, 92, 92, 72, 72, 63, 63), TRUE, NULL, 0L, NULL, 3L, TRUE, NA, \".\"); .Internal(format(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]], argv[[9]]))");
    }

    @Test
    public void testformat27() {
        assertEval("argv <- list(structure(c(142L, 104L, 71L, 250L), .Dim = 4L, .Dimnames = structure(list(c('(1) Approve STRONGLY', '(2) Approve SOMEWHAT', '(3) Disapprove SOMEWHAT', '(4) Disapprove STRONGLY')), .Names = '')), FALSE, 7L, 0L, NULL, 3L, TRUE, NA, \".\"); .Internal(format(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]], argv[[9]]))");
    }

    @Test
    public void testformat28() {
        assertEval("argv <- list(structure(c('***', '*', ' ', ' ', ' '), legend = '0 ‘***’ 0.001 ‘**’ 0.01 ‘*’ 0.05 ‘.’ 0.1 ‘ ’ 1', class = 'noquote'), FALSE, NULL, 0L, NULL, 0L, TRUE, NA, \".\"); .Internal(format(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]], argv[[9]]))");
    }

    @Test
    public void testformat29() {
        assertEval("argv <- list(structure(c(0, 5, 118, 57, 0, 1, 4, 140, 0, 11, 154, 14, 0, 13, 13, 80, 35, 13, 387, 75, 17, 14, 89, 76, 0, 0, 670, 192, 0, 0, 3, 20), .Dim = c(1L, 32L), row.vars = structure(list(), .Names = character(0)), col.vars = structure(list(Class = c('1st', '2nd', '3rd', 'Crew'), Sex = c('Male', 'Female'), Age = c('Child', 'Adult'), Survived = c('No', 'Yes')), .Names = c('Class', 'Sex', 'Age', 'Survived'))), FALSE, 7L, 0L, NULL, 3L, TRUE, NA, \".\"); .Internal(format(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]], argv[[9]]))");
    }

    @Test
    public void testformat30() {
        assertEval("argv <- list(c('', '', '\\\'Adult\\\'', '\\\'No\\\'', '', '387'), FALSE, NULL, 0L, NULL, 1L, TRUE, NA, \".\"); .Internal(format(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]], argv[[9]]))");
    }

    @Test
    public void testformat31() {
        assertEval("argv <- list(2.2250738585072e-308, TRUE, NULL, 0L, NULL, 3L, TRUE, NA, \".\"); .Internal(format(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]], argv[[9]]))");
    }

    @Test
    public void testformat32() {
        assertEval("argv <- list(c(-0.318309886183791+0i, 0-0.564189583547756i, 1+0i, 0+1.77245385090552i, -3.14159265358979+0i), TRUE, 2, 0L, NULL, 3L, TRUE, NA, \".\"); .Internal(format(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]], argv[[9]]))");
    }

    @Test
    public void testformat33() {
        assertEval("argv <- list(0+1i, TRUE, NULL, 0L, NULL, 3L, TRUE, NA, \".\"); .Internal(format(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]], argv[[9]]))");
    }

    @Test
    public void testformat34() {
        assertEval("argv <- list(structure(c(-Inf, -Inf, -2.248e+263, -Inf, -3.777e+116, -1), .Names = c('Min.', '1st Qu.', 'Median', 'Mean', '3rd Qu.', 'Max.')), FALSE, 7L, 0L, NULL, 3L, TRUE, NA, \".\"); .Internal(format(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]], argv[[9]]))");
    }

    @Test
    public void testformat35() {
        assertEval("argv <- list(c(-41L, -36L, -12L, -18L, NA, -28L, -23L, -19L, -8L, NA, -7L, -16L, -11L, -14L, -18L, -14L, -34L, -6L, -30L, -11L, -1L, -11L, -4L, -32L, NA, NA, NA, -23L, -45L, -115L, -37L, NA, NA, NA, NA, NA, NA, -29L, NA, -71L, -39L, NA, NA, -23L, NA, NA, -21L, -37L, -20L, -12L, -13L, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, -135L, -49L, -32L, NA, -64L, -40L, -77L, -97L, -97L, -85L, NA, -10L, -27L, NA, -7L, -48L, -35L, -61L, -79L, -63L, -16L, NA, NA, -80L, -108L, -20L, -52L, -82L, -50L, -64L, -59L, -39L, -9L, -16L, -78L, -35L, -66L, -122L, -89L, -110L, NA, NA, -44L, -28L, -65L, NA, -22L, -59L, -23L, -31L, -44L, -21L, -9L, NA, -45L, -168L, -73L, NA, -76L, -118L, -84L, -85L, -96L, -78L, -73L, -91L, -47L, -32L, -20L, -23L, -21L, -24L, -44L, -21L, -28L, -9L, -13L, -46L, -18L, -13L, -24L, -16L, -13L, -23L, -36L, -7L, -14L, -30L, NA, -14L, -18L, -20L), FALSE, NULL, 0L, NULL, 3L, FALSE, NA, \".\"); .Internal(format(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]], argv[[9]]))");
    }

    @Test
    public void testformat36() {
        assertEval("argv <- list(structure(integer(0), .Label = character(0), class = 'factor'), TRUE, NULL, 0L, NULL, 3L, FALSE, NA, \".\"); .Internal(format(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]], argv[[9]]))");
    }

    @Test
    public void testformat37() {
        assertEval("argv <- list(structure(c(213198964, 652424.52183908), .Names = c('null.deviance', 'deviance')), FALSE, 5L, 0L, NULL, 3L, TRUE, NA, \".\"); .Internal(format(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]], argv[[9]]))");
    }

    @Test
    public void testformat38() {
        assertEval("argv <- list(structure(integer(0), .Dim = c(1L, 0L), row.vars = structure(list(), .Names = character(0)), col.vars = structure(list(df0 = NULL), .Names = 'df0')), FALSE, 7L, 0L, NULL, 3L, TRUE, NA, \".\"); .Internal(format(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]], argv[[9]]))");
    }

    @Test
    public void testformat39() {
        assertEval("argv <- list(FALSE, FALSE, NULL, 0L, NULL, 3L, FALSE, NA, \".\"); .Internal(format(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]], argv[[9]]))");
    }

    @Test
    public void testformat40() {
        assertEval("argv <- list(1e-07, TRUE, NULL, 0L, NULL, 3L, TRUE, NA, \".\"); .Internal(format(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]], argv[[9]]))");
    }

    @Test
    public void testformat41() {
        assertEval("argv <- list(structure(c(3035, 2552, 2704, 2554, 2014, 1655, 1721, 1524, 1596, 2074, 2199, 2512, 2933, 2889, 2938, 2497, 1870, 1726, 1607, 1545, 1396, 1787, 2076, 2837, 2787, 3891, 3179, 2011, 1636, 1580, 1489, 1300, 1356, 1653, 2013, 2823, 3102, 2294, 2385, 2444, 1748, 1554, 1498, 1361, 1346, 1564, 1640, 2293, 2815, 3137, 2679, 1969, 1870, 1633, 1529, 1366, 1357, 1570, 1535, 2491, 3084, 2605, 2573, 2143, 1693, 1504, 1461, 1354, 1333, 1492, 1781, 1915), .Tsp = c(1973, 1978.91666666667, 12), class = 'ts'), FALSE, NULL, 0L, NULL, 3L, TRUE, NA, \".\"); .Internal(format(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]], argv[[9]]))");
    }

    @Test
    public void testformat42() {
        assertEval("argv <- list(c(2.5, 97.5), TRUE, 3, 0L, NULL, 3L, TRUE, FALSE, \".\"); .Internal(format(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]], argv[[9]]))");
    }

    @Test
    public void testformat43() {
        assertEval("argv <- list(structure(c(9.4, 10.2, 9.2, 4.4, 3.5, 2.7), .Dim = c(3L, 2L), .Dimnames = list(NULL, c('Estimate', 'Std.Err'))), FALSE, 2, 0L, NULL, 3L, TRUE, NA, \".\"); .Internal(format(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]], argv[[9]]))");
    }

    @Test
    public void testformat44() {
        assertEval("argv <- list(95, 2, NULL, 0L, NULL, 3L, TRUE, NA, \".\"); .Internal(format(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]], argv[[9]]))");
    }

    @Test
    public void testformat46() {
        assertEval("argv <- list(1.2e+07, FALSE, NULL, 9L, NULL, 3L, TRUE, NA, \".\"); .Internal(format(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]], argv[[9]]))");
    }

    @Test
    public void testformat47() {
        assertEval("argv <- list(-0.01234+3.14159265358979i, FALSE, NULL, 14L, NULL, 3L, TRUE, NA, \".\"); .Internal(format(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]], argv[[9]]))");
    }

    @Test
    public void testformat48() {
        assertEval("argv <- list(c(TRUE, FALSE, TRUE, FALSE, FALSE, FALSE), FALSE, NULL, 0L, NULL, 3L, TRUE, NA, \".\"); .Internal(format(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]], argv[[9]]))");
    }

    @Test
    public void testformat49() {
        assertEval("argv <- list(3.141, FALSE, NULL, 13L, NULL, 3L, TRUE, NA, \".\"); .Internal(format(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]], argv[[9]]))");
    }

    @Test
    public void testformat50() {
        assertEval("argv <- list(c(Inf, -Inf), FALSE, NULL, 0L, NULL, 3L, TRUE, NA, \".\"); .Internal(format(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]], argv[[9]]))");
    }

    @Test
    public void testformat51() {
        assertEval("argv <- list(structure(c(2, NA), .Names = c('N:P:K', 'Residuals')), FALSE, 5L, 0L, NULL, 3L, TRUE, NA, \".\"); .Internal(format(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]], argv[[9]]))");
    }

    @Test
    public void testformat52() {
        assertEval("argv <- list(structure('def\\\'gh', class = 'AsIs'), FALSE, NULL, 0L, NULL, 3L, TRUE, NA, \".\"); .Internal(format(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]], argv[[9]]))");
    }

    @Test
    public void testformat53() {
        assertEval("argv <- list(structure(4:9, .Dim = c(3L, 2L), .Dimnames = list(NULL, c('a', 'b'))), FALSE, NULL, 0L, NULL, 3L, FALSE, NA, \".\"); .Internal(format(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]], argv[[9]]))");
    }

    @Test
    public void testformat54() {
        assertEval("argv <- list(c(NA, NA, NA, NA, NA, 'Ripley', 'Venables & Smith'), FALSE, NULL, 0L, NULL, 3L, FALSE, NA, \".\"); .Internal(format(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]], argv[[9]]))");
    }

    @Test
    public void testformat55() {
        assertEval("argv <- list(1e-11, FALSE, NULL, 0L, NULL, 3L, TRUE, NA, \".\"); .Internal(format(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]], argv[[9]]))");
    }

    @Test
    public void testformat56() {
        assertEval("argv <- structure(list(x = 0.04, digits = 3, nsmall = 3), .Names = c('x',     'digits', 'nsmall'));do.call('format', argv)");
    }

    /**
     * This test checks whether the names of double values in a vector are present in the formatted
     * output.
     */
    @Test
    public void testformat57() {
        assertEval("x <- c(1.0,2.0);names(x) <- c(\"x\",\"y\");argv <- list(x, FALSE, NULL, 0L, NULL, 0L, FALSE, FALSE, \".\");names(.Internal(format(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]], argv[[9]])))");
    }

    public void testFormat() {
        assertEval("{ format(7) }");
        assertEval("{ format(7.42) }");
        assertEval("{ format(c(7,42)) }");
        assertEval("{ format(c(7.42,42.7)) }");
        assertEval("{ format(c(7.42,42.7,NA)) }");
        assertEval("{ .Internal(format(.GlobalEnv,FALSE,NA,0,0,3,TRUE,NA,'.')) }");
    }
}
