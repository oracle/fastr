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
public class TestBuiltin_printdefault extends TestBase {

    @Test
    public void testprintdefault1() {
        assertEval("argv <- list(structure(c('-3.001e+155', '-1.067e+107', ' -1.976e+62', '-9.961e+152', ' -2.059e+23', '  1.000e+00'), .Names = c('Min.', '1st Qu.', 'Median', 'Mean', '3rd Qu.', 'Max.')), NULL, FALSE, NULL, NULL, TRUE, NULL, TRUE, FALSE); .Internal(print.default(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]], argv[[9]]))");
    }

    @Test
    public void testprintdefault2() {
        assertEval("argv <- list(structure(c(' 1', 'NA', ' 1', '1.1', ' NA', '2.0', '1.1+0i', '    NA', '3.0+0i', 'NA', 'NA', 'NA', 'FALSE', '   NA', ' TRUE', 'abc', NA, 'def'), .Dim = c(3L, 6L), .Dimnames = list(c('1', '2', '3'), c('A', 'B', 'C', 'D', 'E', 'F'))), NULL, FALSE, NULL, NULL, TRUE, NULL, TRUE, FALSE); .Internal(print.default(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]], argv[[9]]))");
    }

    @Test
    public void testprintdefault3() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(c('1', '2', '\\\\b', '4', '5', '\\\\040', '\\\\x20', 'c:\\\\spencer\\\\tests', '\\\\t', '\\\\n', '\\\\r'), .Dim = c(11L, 1L), .Dimnames = list(c('1', '2', '3', '4', '5', '6', '7', '8', '9', '10', '11'), 'TEST')), NULL, FALSE, NULL, NULL, TRUE, NULL, TRUE, FALSE); .Internal(print.default(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]], argv[[9]]))");
    }

    @Test
    public void testprintdefault4() {
        assertEval("argv <- list(quote(~a + b:c + d + e + e:d), NULL, TRUE, NULL, NULL, FALSE, NULL, TRUE, TRUE); .Internal(print.default(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]], argv[[9]]))");
    }

    @Test
    public void testprintdefault5() {
        assertEval("argv <- list(structure(c(-1.05715, -0.48359, 0.0799, 0.44239, 1.2699), .Names = c('Min', '1Q', 'Median', '3Q', 'Max')), 4L, TRUE, NULL, NULL, FALSE, NULL, TRUE, FALSE); .Internal(print.default(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]], argv[[9]]))");
    }

    @Test
    public void testprintdefault6() {
        assertEval("argv <- list(quote(y ~ A:U + A:V - 1), NULL, TRUE, NULL, NULL, FALSE, NULL, TRUE, TRUE); .Internal(print.default(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]], argv[[9]]))");
    }

    @Test
    public void testprintdefault7() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(c(NA, NA, NA, 'a', NA, NA, 'b', 'd', NA, '10', '12', '14'), .Dim = 3:4), NULL, TRUE, '----', NULL, TRUE, NULL, TRUE, FALSE); .Internal(print.default(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]], argv[[9]]))");
    }

    @Test
    public void testprintdefault8() {
        assertEval("argv <- list(c('Alb', 'Als', 'Arz', 'Ark', 'Clf', 'Clr', 'Cn', 'Dl', 'Fl', 'Gr', 'Hw', 'Id', 'Il', 'In', 'Iw', 'Kns', 'Knt', 'Ls', 'Man', 'Mr', 'Mssc', 'Mc', 'Mnn', 'Msss', 'Mssr', 'Mnt', 'Nb', 'Nv', 'NH', 'NJ', 'NM', 'NY', 'NC', 'ND', 'Oh', 'Ok', 'Or', 'Pn', 'RI', 'SC', 'SD', 'Tn', 'Tx', 'Ut', 'Vrm', 'Vrg', 'Wsh', 'WV', 'Wsc', 'Wy'), NULL, TRUE, NULL, NULL, FALSE, NULL, TRUE, TRUE); .Internal(print.default(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]], argv[[9]]))");
    }

    @Test
    public void testprintdefault9() {
        assertEval("argv <- list(structure(c('abc', 'def\\\'gh'), .Dim = 1:2, .Dimnames = list('1', c('a', 'b'))), NULL, FALSE, NULL, NULL, TRUE, NULL, TRUE, FALSE); .Internal(print.default(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]], argv[[9]]))");
    }

    @Test
    public void testprintdefault10() {
        assertEval("argv <- list(structure(c(245L, 250L, 255L, 260L, 265L, 270L, 275L, 280L, 285L, 290L, 295L, 300L, 305L, 310L, 315L, 320L, 325L, 330L, 335L, 340L, 345L, 350L, 355L, 360L), .Dim = 2:4, .Dimnames = list(NULL, c('a', 'b', 'c'), NULL)), NULL, TRUE, NULL, NULL, FALSE, NULL, TRUE, TRUE); .Internal(print.default(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]], argv[[9]]))");
    }

    @Test
    public void testprintdefault11() {
        assertEval("argv <- list(Inf, NULL, TRUE, NULL, NULL, FALSE, NULL, TRUE, TRUE); .Internal(print.default(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]], argv[[9]]))");
    }

    @Test
    public void testprintdefault12() {
        assertEval("argv <- list(structure(c(' 79.53', '  6.00', ' 86.20', '  6.00', ' 69.77', '  5.00', ' 98.03', '  6.00', '108.03', '  6.00', ' 89.20', '  6.00', '114.20', '  6.00', '116.70', '  6.00', '110.37', '  6.00', '124.37', '  6.00', '126.37', '  6.00', '118.03', '  6.00'), .Dim = c(6L, 4L), .Dimnames = structure(list(V = c('Golden.rain', 'rep        ', 'Marvellous ', 'rep        ', 'Victory    ', 'rep        '), N = c('0.0cwt', '0.2cwt', '0.4cwt', '0.6cwt')), .Names = c('V', 'N'))), NULL, FALSE, NULL, NULL, FALSE, NULL, TRUE, FALSE); .Internal(print.default(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]], argv[[9]]))");
    }

    @Test
    public void testprintdefault13() {
        assertEval("argv <- list('2014-03-17 13:47:59 EDT', NULL, TRUE, NULL, NULL, FALSE, NULL, TRUE, TRUE); .Internal(print.default(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]], argv[[9]]))");
    }

    @Test
    public void testprintdefault14() {
        assertEval("argv <- list(structure(1:120, .Dim = 2:5, .Dimnames = list(NULL, c('a', 'b', 'c'), NULL, c('V5', 'V6', 'V7', 'V8', 'V9'))), NULL, TRUE, NULL, NULL, FALSE, NULL, TRUE, TRUE); .Internal(print.default(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]], argv[[9]]))");
    }

    @Test
    public void testprintdefault15() {
        assertEval("argv <- list(structure(c('1', '2', '1'), .Dim = 3L, .Dimnames = structure(list(c('1', '2', NA)), .Names = '')), NULL, FALSE, NULL, NULL, TRUE, NULL, TRUE, FALSE); .Internal(print.default(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]], argv[[9]]))");
    }

    @Test
    public void testprintdefault17() {
        assertEval("argv <- list(NULL, NULL, TRUE, NULL, NULL, FALSE, NULL, TRUE, TRUE); .Internal(print.default(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]], argv[[9]]))");
    }

    @Test
    public void testprintdefault18() {
        assertEval(Ignored.OutputFormatting,
                        "argv <- list(quote(breaks ~ (wool + tension) - tension), NULL, TRUE, NULL, NULL, FALSE, NULL, TRUE, TRUE); .Internal(print.default(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]], argv[[9]]))");
    }

    @Test
    public void testprintdefault19() {
        assertEval("argv <- list(c('2007-11-06', '2007-11-06'), NULL, TRUE, NULL, NULL, FALSE, 99999L, TRUE, FALSE); .Internal(print.default(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]], argv[[9]]))");
    }

    @Test
    public void testprintdefault20() {
        assertEval("argv <- list(structure(c(NA, NA, 1L, 9L), .Names = c('size', 'current', 'direction', 'eval_depth')), NULL, TRUE, NULL, NULL, FALSE, NULL, TRUE, TRUE); .Internal(print.default(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]], argv[[9]]))");
    }

    @Test
    public void testprintdefault21() {
        assertEval("argv <- list(character(0), NULL, TRUE, NULL, NULL, FALSE, NULL, TRUE, TRUE); .Internal(print.default(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]], argv[[9]]))");
    }

    @Test
    public void testprintdefault22() {
        assertEval("argv <- list(structure('0.01587', .Names = '(Intercept)'), NULL, FALSE, NULL, 2, FALSE, NULL, TRUE, FALSE); .Internal(print.default(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]], argv[[9]]))");
    }

    @Test
    public void testprintdefault23() {
        assertEval("argv <- list(quote(Y ~ X), NULL, TRUE, NULL, NULL, FALSE, NULL, TRUE, TRUE); .Internal(print.default(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]], argv[[9]]))");
    }

    @Test
    public void testprintdefault24() {
        assertEval("argv <- list(c(0.944550219923258, 0.336629745550454, 0.629688071087003, 0.591416056267917), NULL, TRUE, NULL, NULL, FALSE, NULL, TRUE, TRUE); .Internal(print.default(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]], argv[[9]]))");
    }

    @Test
    public void testprintdefault25() {
        assertEval("argv <- list(c('surname', 'nationality', 'deceased', 'title', 'other.author'), NULL, FALSE, NULL, NULL, FALSE, NULL, TRUE, FALSE); .Internal(print.default(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]], argv[[9]]))");
    }

    @Test
    public void testprintdefault26() {
        assertEval("argv <- list(structure(c('-0.91', ' 0.81', '', '-0.97'), .Dim = c(2L, 2L), .Dimnames = list(c('x1', 'x3'), c('(Intercept)', 'x1'))), NULL, FALSE, NULL, NULL, FALSE, NULL, TRUE, FALSE); .Internal(print.default(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]], argv[[9]]))");
    }

    @Test
    public void testprintdefault27() {
        assertEval("argv <- list(c(TRUE, TRUE, TRUE), NULL, TRUE, NULL, NULL, FALSE, NULL, TRUE, TRUE); .Internal(print.default(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]], argv[[9]]))");
    }

    @Test
    public void testprintdefault28() {
        assertEval("argv <- list(c(1L, 2L, 3L, 4L, 5L, 1L, 2L, 3L, 4L, 5L, 1L, 2L, 3L, 4L, 5L), NULL, FALSE, NULL, NULL, TRUE, NULL, TRUE, FALSE); .Internal(print.default(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]], argv[[9]]))");
    }

    @Test
    public void testprintdefault29() {
        assertEval("argv <- list(structure(c('', ' 1', ' 1', ' 1', '', '  9.93', ' 26.79', '820.91', ' 47.97', ' 57.90', ' 74.76', '868.88', '24.974', '25.420', '28.742', '60.629'), .Dim = c(4L, 4L), .Dimnames = list(c('<none>', '- x4', '- x2', '- x1'), c('Df', 'Sum of Sq', 'RSS', 'AIC'))), NULL, FALSE, '', NULL, TRUE, NULL, TRUE, FALSE); .Internal(print.default(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]], argv[[9]]))");
    }

    @Test
    public void testprintdefault30() {
        assertEval("argv <- list(structure(c(78.7365206866197, 17, 98.5088731171753, 18, 113.842206450509, 18, 123.008873117175, 18), .Dim = c(2L, 4L), .Dimnames = list(c('', 'rep'), c('0.0cwt', '0.2cwt', '0.4cwt', '0.6cwt'))), 4L, TRUE, NULL, NULL, FALSE, NULL, TRUE, FALSE); .Internal(print.default(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]], argv[[9]]))");
    }

    @Test
    public void testprintdefault31() {
        assertEval("argv <- list(structure(1:3, class = 'myClass'), NULL, TRUE, NULL, NULL, FALSE, NULL, TRUE, FALSE); .Internal(print.default(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]], argv[[9]]))");
    }

    @Test
    public void testprintdefault32() {
        assertEval("argv <- list(structure(c(495L, 515L, 535L, 555L, 575L, 595L, 615L, 635L, 655L, 675L, 695L, 715L), .Dim = 3:4, .Dimnames = list(c('a', 'b', 'c'), NULL)), NULL, TRUE, NULL, NULL, FALSE, NULL, TRUE, TRUE); .Internal(print.default(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]], argv[[9]]))");
    }
}
