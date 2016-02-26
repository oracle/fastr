/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2012-2014, Purdue University
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.*;

import com.oracle.truffle.r.test.*;

// Checkstyle: stop line length check
public class TestBuiltin_inherits extends TestBase {

    @Test
    public void testinherits1() {
        assertEval("argv <- list(structure(c(1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L), .Names = c('dtrMatrix', 'MatrixFactorization', 'ddenseMatrix', 'triangularMatrix', 'dMatrix', 'denseMatrix', 'Matrix', 'mMatrix')), 'factor', FALSE); .Internal(inherits(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testinherits2() {
        assertEval("argv <- list(structure(list(x = numeric(0), y = numeric(0), fac = structure(integer(0), .Label = c('A', 'B', 'C'), class = 'factor')), .Names = c('x', 'y', 'fac'), row.names = integer(0), class = 'data.frame'), 'data.frame', FALSE); .Internal(inherits(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testinherits3() {
        assertEval("argv <- list(structure(c(2, 3, 4, 5, 6, 7, 8, 9, 10, 11), .Tsp = c(2, 11, 1)), 'data.frame', FALSE); .Internal(inherits(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testinherits4() {
        assertEval("argv <- list(structure(list(Sepal.Length = c(4.7, 4.8, 5.4, 5.2, 5.5, 4.9, 5, 5.5, 4.9, 4.4, 5.1, 5, 4.5, 4.4, 5, 5.1, 4.8, 5.1, 4.6, 5.3, 5, 7, 6.4, 6.9, 5.5, 6.5, 5.7, 6.3, 4.9, 6.6, 5.2, 5, 5.9, 6, 6.1, 5.6, 6.7, 5.6, 5.8, 6.2, 5.6, 5.9, 6.1, 6.3, 6.1, 6.4, 6.6, 6.8, 6.7, 6, 5.7, 5.5, 5.5, 5.8, 6, 5.4, 6, 6.7, 6.3, 5.6, 5.5, 5.5, 6.1, 5.8, 5, 5.6, 5.7, 5.7, 6.2, 5.1, 5.7, 6.3, 5.8, 7.1, 6.3, 6.5, 7.6, 4.9, 7.3, 6.7, 7.2, 6.5, 6.4, 6.8, 5.7, 5.8, 6.4, 6.5, 7.7, 7.7, 6, 6.9, 5.6, 7.7, 6.3, 6.7, 7.2, 6.2, 6.1, 6.4, 7.2), Sepal.Width = c(3.2, 3.1, 3.4, 4.1, 4.2, 3.1, 3.2, 3.5, 3.6, 3, 3.4, 3.5, 2.3, 3.2, 3.5, 3.8, 3, 3.8, 3.2, 3.7, 3.3, 3.2, 3.2, 3.1, 2.3, 2.8, 2.8, 3.3, 2.4, 2.9, 2.7, 2, 3, 2.2, 2.9, 2.9, 3.1, 3, 2.7, 2.2, 2.5, 3.2, 2.8, 2.5, 2.8, 2.9, 3, 2.8, 3, 2.9, 2.6, 2.4, 2.4, 2.7, 2.7, 3, 3.4, 3.1, 2.3, 3, 2.5, 2.6, 3, 2.6, 2.3, 2.7, 3, 2.9, 2.9, 2.5, 2.8, 3.3, 2.7, 3, 2.9, 3, 3, 2.5, 2.9, 2.5, 3.6, 3.2, 2.7, 3, 2.5, 2.8, 3.2, 3, 3.8, 2.6, 2.2, 3.2, 2.8, 2.8, 2.7, 3.3, 3.2, 2.8, 3, 2.8, 3),     Petal.Length = c(1.6, 1.6, 1.5, 1.5, 1.4, 1.5, 1.2, 1.3, 1.4, 1.3, 1.5, 1.3, 1.3, 1.3, 1.6, 1.9, 1.4, 1.6, 1.4, 1.5, 1.4, 4.7, 4.5, 4.9, 4, 4.6, 4.5, 4.7, 3.3, 4.6, 3.9, 3.5, 4.2, 4, 4.7, 3.6, 4.4, 4.5, 4.1, 4.5, 3.9, 4.8, 4, 4.9, 4.7, 4.3, 4.4, 4.8, 5, 4.5, 3.5, 3.8, 3.7, 3.9, 5.1, 4.5, 4.5, 4.7, 4.4, 4.1, 4, 4.4, 4.6, 4, 3.3, 4.2, 4.2, 4.2, 4.3, 3, 4.1, 6, 5.1, 5.9, 5.6, 5.8, 6.6, 4.5, 6.3, 5.8, 6.1, 5.1, 5.3, 5.5, 5, 5.1, 5.3, 5.5, 6.7, 6.9, 5, 5.7, 4.9, 6.7, 4.9, 5.7, 6, 4.8, 4.9, 5.6, 5.8    ), Petal.Width = c(0.2, 0.2, 0.4, 0.1, 0.2, 0.2, 0.2, 0.2, 0.1, 0.2, 0.2, 0.3, 0.3, 0.2, 0.6, 0.4, 0.3, 0.2, 0.2, 0.2, 0.2, 1.4, 1.5, 1.5, 1.3, 1.5, 1.3, 1.6, 1, 1.3, 1.4, 1, 1.5, 1, 1.4, 1.3, 1.4, 1.5, 1, 1.5, 1.1, 1.8, 1.3, 1.5, 1.2, 1.3, 1.4, 1.4, 1.7, 1.5, 1, 1.1, 1, 1.2, 1.6, 1.5, 1.6, 1.5, 1.3, 1.3, 1.3, 1.2, 1.4, 1.2, 1, 1.3, 1.2, 1.3, 1.3, 1.1, 1.3, 2.5, 1.9, 2.1, 1.8, 2.2, 2.1, 1.7, 1.8, 1.8, 2.5, 2, 1.9, 2.1, 2, 2.4, 2.3, 1.8, 2.2, 2.3, 1.5, 2.3, 2, 2, 1.8, 2.1, 1.8, 1.8, 1.8, 2.1,     1.6)), .Names = c('Sepal.Length', 'Sepal.Width', 'Petal.Length', 'Petal.Width'), row.names = 30:130, class = 'data.frame'), 'data.frame', FALSE); .Internal(inherits(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testinherits5() {
        assertEval("argv <- list(structure(1L, .Dim = 1L), 'try-error', FALSE); .Internal(inherits(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testinherits6() {
        assertEval("argv <- list(c(0, 0, 0, 0, 0, 1.75368801162502e-134, 0, 0, 0, 2.60477585273833e-251, 1.16485035372295e-260, 0, 1.53160350210786e-322, 0.333331382328728, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3.44161262707711e-123, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1.968811545398e-173, 0, 8.2359965384697e-150, 0, 0, 0, 0, 6.51733217171341e-10, 0, 2.36840184577368e-67, 0, 9.4348408357524e-307, 0, 1.59959906013771e-89, 0, 8.73836857865034e-286, 7.09716190970992e-54, 0, 0, 0, 1.530425353017e-274, 8.57590058044551e-14, 0.333333106397154, 0, 0, 1.36895217898448e-199, 2.0226102635783e-177, 5.50445388209462e-42, 0, 0, 0, 0, 1.07846402051283e-44, 1.88605464411243e-186, 1.09156111051203e-26, 0, 3.0702877273237e-124, 0.333333209689785, 0, 0, 0, 0, 0, 0, 3.09816093866831e-94, 0, 0, 4.7522727332095e-272, 0, 0, 2.30093251441394e-06, 0, 0, 1.27082826644707e-274, 0, 0, 0, 0, 0, 0, 0, 4.5662025456054e-65, 0, 2.77995853978268e-149, 0, 0, 0), 'Surv', FALSE); .Internal(inherits(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testinherits7() {
        assertEval("argv <- list(structure(c(1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L), .Label = c('Ctl', 'A', 'B'), class = 'factor', contrasts = 'contr.treatment'), 'factor', FALSE); .Internal(inherits(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testinherits8() {
        assertEval("argv <- list(c(NA, NA, '/home/lzhao/tmp/RtmptukZK0/R.INSTALL2ccc3a5cba55/nlme/R/groupedData.R', '/home/lzhao/tmp/RtmptukZK0/R.INSTALL2ccc3a5cba55/nlme/R/groupedData.R', '/home/lzhao/tmp/RtmptukZK0/R.INSTALL2ccc3a5cba55/nlme/R/groupedData.R', '/home/lzhao/tmp/RtmptukZK0/R.INSTALL2ccc3a5cba55/nlme/R/groupedData.R'), 'ordered', FALSE); .Internal(inherits(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testinherits9() {
        assertEval("argv <- list(list(structure(3.14159265358979, class = structure('3.14159265358979', class = 'testit'))), 'try-error', FALSE); .Internal(inherits(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testinherits10() {
        assertEval("argv <- list(structure(c(3+2i, 3+2i, NA, 3+2i, 3+2i, 3+2i, 3+2i, 3+2i, 4-5i, 3-5i, NA, NA, 2-5i, 3-5i, 4-5i, 5-5i), .Dim = c(8L, 2L), .Dimnames = list(NULL, c('x1', 'x2'))), 'data.frame', FALSE); .Internal(inherits(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testinherits11() {
        assertEval("argv <- list(structure(list(srcfile = '/home/lzhao/hg/r-instrumented/library/stats/R/stats', frow = 853L, lrow = 853L), .Names = c('srcfile', 'frow', 'lrow'), row.names = c(NA, -1L), class = 'data.frame'), 'data.frame', FALSE); .Internal(inherits(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testinherits12() {
        assertEval("argv <- list(quote(y ~ a + b:c + d + e + e:d), 'formula', FALSE); .Internal(inherits(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testinherits13() {
        assertEval("argv <- list(structure(10, class = c('a', 'b')), c('a', 'b', 'c'), TRUE); .Internal(inherits(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testinherits14() {
        assertEval("argv <- list(complex(0), 'try-error', FALSE); .Internal(inherits(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testinherits15() {
        assertEval("argv <- list(structure(FALSE, .Tsp = c(0, 0, 1), class = 'ts'), 'ts', FALSE); .Internal(inherits(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testinherits16() {
        assertEval("argv <- list(structure(list(a = 1), .Dim = 1L, .Dimnames = list('a')), 'try-error', FALSE); .Internal(inherits(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testinherits17() {
        assertEval("argv <- list(raw(0), 'try-error', FALSE); .Internal(inherits(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testinherits18() {
        assertEval("argv <- list(structure(c(-1.5116089581734, 0.200010507218348, 0.266001075015567, 0.339550302820724, 0.425045882083188, 0.496549005782181, 0.576998440511346, -0.909988579721932, -1.06576984591386, 0.174059431391812, -0.0372498129362256, -0.282881300668478, -0.488312023557303, -0.719445779363312), gradient = structure(c(0.160743714207466, 0.251172444221428, 0.307513919261763, 0.350467096622222, 0.367731527586793, 0.346345778958899, 0.262925702855199, -0.160743714207466, -0.251172444221428, -0.307513919261763, -0.350467096622222, -0.367731526984617, -0.346345778958899, -0.262925703156287), .Dim = c(7L, 2L, 1L)), .Dim = c(7L, 2L)), 'data.frame', FALSE); .Internal(inherits(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testinherits19() {
        assertEval("argv <- list(structure(3.14159265358979, comment = 'Start with pi'), 'try-error', FALSE); .Internal(inherits(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testinherits20() {
        assertEval("argv <- list(c(TRUE, NA, FALSE, TRUE), 'Date', FALSE); .Internal(inherits(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testinherits21() {
        assertEval("argv <- list(c(-1, 1, -1, 2, 1, NA, -1, 1, 4, 1, NA, 4, 1, 3, NA, 4, 2, 2, NA, 4, 4, 2, 4, 4, 2, 1, 4, 4, 3, 1, 1, 4, 1, 4, NA, 1, 4, 4, 2, 2, 4, 4, 3, 4, 2, 2, 3, 3, 4, 1, 1, 1, 4, 1, 4, 4, 4, 4, NA, 4, 4, 4, NA, 1, 2, 3, 4, 3, 4, 2, 4, 4, 1, 4, 1, 4, NA, 4, 2, 1, 4, 1, 1, 1, 4, 4, 2, 4, 1, 1, 1, 4, 1, 1, 1, 4, 3, 1, 4, 3, 2, 4, 3, 1, 4, 2, 4, NA, 4, 4, 4, 2, 1, 4, 4, NA, 2, 4, 4, 1, 1, 1, 1, 4, 1, 2, 3, 2, 1, 4, 4, 4, 1, NA, 4, 2, 2, 2, 4, 4, 3, 3, 4, 2, 4, 3, 1, 1, 4, 2, 4, 3, 1, 4, 3, 4, 4, 1, 1, 4, 4, 3, 1, 1, 2, 1, 3, 4, 2, 2, 2, 4, 4, 3, 2, 1, 1, 4, 1, 1, 2, NA, 2, 3, 3, 2, 1, 1, 1, 1, 4, 4, 4, 4, 4, 4, 2, 2, 1, 4, 1, 4, 3, 4, 2, 3, 1, 3, 1, 4, 1, 4, 1, 4, 3, 3, 4, 4, 1, NA, 3, 4, 4, 4, 4, 4, 4, 3, 4, 3, 4, 2, 4, 4, 1, 2, NA, 4, 4, 4, 4, 1, 2, 1, 1, 2, 1, 4, 2, 3, 1, 4, 4, 4, 1, 2, 1, 4, 2, 1, 3, 1, 2, 2, 1, 2, 1, NA, 3, 2, 2, 4, 1, 4, 4, 2, 4, 4, 4, 2, 1, 4, 2, 4, 4, 4, 4, 4, 1, 3, 4, 3, 4, 1, NA, 4, NA, 1, 1, 1, 4, 4, 4, 4, 2, 4, 3, 2, NA, 1, 4, 4, 3, 4, 4, 4, 2, 4, 2, 1, 4, 4, NA, 4, 4, 3, 3, 4, 2, 2, 4, 1, 4, 4, 4, 3, 4, 4, 4, 3, 2, 1, 3, 1, 4, 1, 4, 2, NA, 1, 4, 4, 3, 1, 4, 1, 4, 1, 4, 4, 1, 2, 2, 1, 4, 1, 1, 4, NA, 4, NA, 4, 4, 4, 1, 4, 2, 1, 2, 2, 2, 2, 1, 1, 2, 1, 4, 2, 3, 3, 1, 3, 1, 4, 1, 3, 2, 2, 4, 1, NA, 3, 4, 2, 4, 4, 4, 4, 4, 4, 3, 4, 4, 3, 2, 1, 4, 4, 2, 4, 2, 1, 2, 1, 1, 1, 1, 4, 4, 1, 1, 4, 1, 4, 4, 4, 1, 1, NA, 3, 2, 4, 4, 4, 4, 2, 3, 3, 2, NA, 4, 2, 4, 4, 1, 1, 4, 4, 1, 1, 4, 1, 2, 2, 2, 2, 1, 4, 4, 1, 2, 2, 2, 3, 4, 4, 3, 4, 1, 1, 4, 4, NA, 4, 1, 4, 4, 4, 1, 4, 4, 1, 2, 4, 4, 4, 4, 1, 2, 4, 4, 2, 1, 4, 2, 4, 2, 2, 4, 1, 3, 3, 2, 4, 1, 4, 4, 4, 1, NA, 4, 4, 2, 4, 4, 4, 4, 4, 2, NA, 4, 2, 4, 3, 1, 4, 4, 3, 4, 2, 4, 4, 1, 2, 1, 4, 1, 3, 3, 1, 4, 4, 2, 4, 4, 4, 4, 3, 2, 3, 3, 2, NA, 3, 4, 4, 3, 3, 4, 4, 4, 1, 4, 4, 4, 4, 4, 4, 4, 2, 4, 2, 3, 4, 1, 3, 1, NA, 4, 1, 2, 2, 1, 4, 3, 3, 4, 1, 1, 3), 'Date', FALSE); .Internal(inherits(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testinherits22() {
        assertEval("argv <- list(.Primitive('['), 'try-error', FALSE); .Internal(inherits(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testinherits23() {
        assertEval("argv <- list(structure(c(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 110, 120, 130, 140, 150, 160, 170, 180, 190, 200, 210, 220, 230, 240, 250, 260, 270, 280, 290, 300, 310, 320, 330, 340, 350, 360, 370, 380, 390, 400, 410, 420, 430, 440, 450, 460, 470, 480, 490, 500, 510, 520, 530, 540, 550, 560, 570, 580, 590, 600, 610, 620, 630, 640, 650, 660, 670, 680, 690, 700, 710, 720, 730, 740, 750, 760, 770, 780, 790, 800, 810, 820, 830, 840, 850, 860, 870, 880, 890, 900, 910, 920, 930, 940, 950, 960, 970, 980, 990, 1000, -4, -3, -2, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96), .Dim = c(101L, 3L), .Dimnames = list(NULL, c('t1', '10 * t1', 't1 - 4')), .Tsp = c(1, 101, 1), class = c('mts', 'ts', 'matrix')), 'ts', FALSE); .Internal(inherits(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testinherits24() {
        assertEval("argv <- list(c(10000, 20000, 30000, 40000, 50000, 60000, 70000, 80000, 90000, 1e+05, 110000, 120000, 130000, 140000, 150000, 160000, 170000, 180000, 190000, 2e+05, 210000, 220000, 230000, 240000, 250000, 260000, 270000, 280000, 290000, 3e+05, 310000, 320000, 330000, 340000, 350000, 360000, 370000, 380000, 390000, 4e+05, 410000, 420000, 430000, 440000, 450000, 460000, 470000, 480000, 490000, 5e+05, 510000, 520000, 530000, 540000, 550000, 560000, 570000, 580000, 590000, 6e+05, 610000, 620000, 630000, 640000, 650000, 660000, 670000, 680000, 690000, 7e+05, 710000, 720000, 730000, 740000, 750000, 760000, 770000, 780000, 790000, 8e+05, 810000, 820000, 830000, 840000, 850000, 860000, 870000, 880000, 890000, 9e+05, 910000, 920000, 930000, 940000, 950000, 960000, 970000, 980000, 990000, 1e+06), 'POSIXlt', FALSE); .Internal(inherits(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testinherits25() {
        assertEval("argv <- list(structure(list(srcfile = c('/home/lzhao/hg/r-instrumented/library/grid/R/grid', '/home/lzhao/hg/r-instrumented/library/grid/R/grid'), frow = 3581:3582, lrow = c(3581L, 3590L)), .Names = c('srcfile', 'frow', 'lrow'), row.names = 1:2, class = 'data.frame'), 'data.frame', FALSE); .Internal(inherits(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testinherits26() {
        assertEval("argv <- list('  Running ‘scales.R’', 'condition', FALSE); .Internal(inherits(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testinherits27() {
        assertEval("argv <- list(c(0.923879532511287+0.38268343236509i, 0.707106781186548+0.707106781186547i, 0.38268343236509+0.923879532511287i, 0+1i, -0.38268343236509+0.923879532511287i, -0.707106781186547+0.707106781186548i, -0.923879532511287+0.38268343236509i, -1+0i, -0.923879532511287-0.38268343236509i, -0.707106781186548-0.707106781186547i, -0.38268343236509-0.923879532511287i, 0-1i, 0.38268343236509-0.923879532511287i, 0.707106781186547-0.707106781186548i, 0.923879532511287-0.38268343236509i, 1-0i), 'ts', FALSE); .Internal(inherits(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testinherits28() {
        assertEval("argv <- list(structure(character(0), .Names = character(0), package = character(0), class = structure('signature', package = 'methods')), 'try-error', FALSE); .Internal(inherits(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testinherits29() {
        assertEval("argv <- list(structure(c(11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30), .Tsp = c(1960.08333333333, 1961.66666666667, 12), class = 'ts'), 'ts', FALSE); .Internal(inherits(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testinherits30() {
        assertEval("argv <- list(structure(list(coefficients = numeric(0), residuals = structure(c(-0.667819876370237, 0.170711734013213, 0.552921941721332, -0.253162069270378, -0.00786394222146348, 0.0246733498130512, 0.0730305465518564, -1.36919169254062, 0.0881443844426084, -0.0834190388782434), .Names = c('1', '2', '3', '4', '5', '6', '7', '8', '9', '10')), fitted.values = structure(c(0, 0, 0, 0, 0, 0, 0, 0, 0, 0), .Names = c('1', '2', '3', '4', '5', '6', '7', '8', '9', '10')), weights = NULL, rank = 0L, df.residual = 10L,     call = quote(lm(formula = y ~ 0)), terms = quote(y ~ 0), model = structure(list(y = c(-0.667819876370237, 0.170711734013213, 0.552921941721332, -0.253162069270378, -0.00786394222146348, 0.0246733498130512, 0.0730305465518564, -1.36919169254062, 0.0881443844426084, -0.0834190388782434)), .Names = 'y', terms = quote(y ~ 0), row.names = c(NA, 10L), class = 'data.frame')), .Names = c('coefficients', 'residuals', 'fitted.values', 'weights', 'rank', 'df.residual', 'call', 'terms', 'model'), class = 'lm'), 'lm', FALSE); .Internal(inherits(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testinherits31() {
        assertEval("argv <- list(structure(c(-0.562441486309934, -0.588967592535822, 0.0277608937997097, 0.568074124752969, 3.89980510825846, -0.428174866497729, -0.343990813420242, -0.260996370058754, -2.31774610938305, 0.314764947225063, -0.455124436264437, -0.0444006414474544, -0.27748974692001, -0.303134023269405, -0.670168347915028, 2.92643313367, -0.749546667806845, -0.410394401887929, -0.203261263063707, 0.1847365997012, 0.128559671155683, 0.313558179929332, -0.0668425264405297, -0.106427678524531, -0.523747793519006, -0.402585404761851, 0.0642079595716389, -0.779859286629166, 0.356484381211739, -0.625053119472271, 1.31547628490512, -0.21959878152752, -0.102402088986461), .Names = c('Craig Dunain', 'Ben Rha', 'Ben Lomond', 'Goatfell', 'Bens of Jura', 'Cairnpapple', 'Scolty', 'Traprain', 'Lairig Ghru', 'Dollar', 'Lomonds', 'Cairn Table', 'Eildon Two', 'Cairngorm', 'Seven Hills', 'Knock Hill', 'Black Hill', 'Creag Beag', 'Kildcon Hill', 'Meall Ant-Suidhe', 'Half Ben Nevis', 'Cow Hill', 'N Berwick Law', 'Creag Dubh', 'Burnswark', 'Largo Law', 'Criffel', 'Acmony', 'Ben Nevis', 'Knockfarrel', 'Two Breweries', 'Cockleroi', 'Moffat Chase')), 'factor', FALSE); .Internal(inherits(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testinherits32() {
        assertEval("argv <- list(quote(breaks ~ (wool + tension) - tension), 'formula', FALSE); .Internal(inherits(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testinherits34() {
        assertEval("argv <- structure(list(x = structure(c(1412799280.04908, 1412799280.04908),     class = c('POSIXct', 'POSIXt')), what = 'POSIXt'), .Names = c('x',     'what'));"
                        + "do.call('inherits', argv)");
    }

    @Test
    public void testInherits() {
        assertEval("{x <- 10; inherits(x, \"a\") ;}");
        assertEval("{x <- 10;class(x) <- c(\"a\", \"b\"); inherits(x,\"a\") ;}");
        assertEval("{x <- 10;class(x) <- c(\"a\", \"b\");inherits(x, \"a\", TRUE) ;}");
        assertEval("{x <- 10;class(x) <- c(\"a\", \"b\");inherits(x, c(\"a\", \"b\", \"c\"), TRUE) ;}");
        assertEval("{x <- 10;class(x) <- c(\"a\");inherits(x, c(\"a\", \"b\", \"a\"), TRUE) ;}");
        assertEval("{x <- 10;class(x) <- c(\"a\", \"b\");inherits(x, c(\"c\", \"q\", \"b\"), TRUE) ;}");
        assertEval("{x <- 10;class(x) <- c(\"a\", \"b\");inherits(x, c(\"c\", \"q\", \"b\")) ;}");
        assertEval("{x <- 10;class(x) <- c(\"a\", \"b\");inherits(x, \"a\", c(TRUE)) ;}");
        assertEval("{ inherits(NULL, \"try-error\") }");
        assertEval("{ inherits(new.env(), \"try-error\") }");

        assertEval("{ x<-data.frame(c(1,2)); inherits(x, \"data.frame\") }");
        assertEval("{ x<-factor(\"a\", \"b\", \"a\"); inherits(x, \"factor\") }");
        assertEval("{ inherits(textConnection(\"abc\"), \"connection\") }");

        assertEval("{ e <- new.env(); inherits(e, \"environment\") }");
        assertEval("{ e <- new.env(); inherits(e, \"abc\") }");
        assertEval("{ e <- new.env(); class(e)<-\"abc\"; inherits(e, \"abc\") }");
        assertEval("{ f <- function() { }; inherits(f, \"function\") }");
        assertEval("{ f <- function() { }; inherits(f, \"abc\") }");
        assertEval("{ f <- function() { }; class(f)<-\"abc\"; inherits(f, \"abc\") }");

        assertEval("{ inherits(getClass(\"ClassUnionRepresentation\"), \"classRepresentation\") }");

        // Fails because of exact string matching in error message.
        assertEval(Ignored.Unknown, "{x <- 10;class(x) <- c(\"a\", \"b\");inherits(x, 2, c(TRUE)) ;}");
        assertEval(Ignored.Unknown, "{x <- 10;class(x) <- c(\"a\", \"b\");inherits(x, \"a\", 1) ;}");
    }
}
