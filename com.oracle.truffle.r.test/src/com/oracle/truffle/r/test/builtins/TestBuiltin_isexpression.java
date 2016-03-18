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
public class TestBuiltin_isexpression extends TestBase {

    @Test
    public void testisexpression1() {
        assertEval("argv <- list(c(20L, 19L, 19L, 19L, 19L, 19L, 19L, 19L, 19L, 20L, 20L, 20L, 20L, 19L, 19L, 19L, 20L, 20L, 20L, 19L, 20L, 19L, 19L, 19L, 20L));is.expression(argv[[1]]);");
    }

    @Test
    public void testisexpression2() {
        assertEval("argv <- list(structure(list(class = c('ordered', 'factor'), levels = character(0)), .Names = c('class', 'levels')));is.expression(argv[[1]]);");
    }

    @Test
    public void testisexpression3() {
        assertEval("argv <- list(c(TRUE, FALSE, TRUE, NA, FALSE, FALSE, TRUE));is.expression(argv[[1]]);");
    }

    @Test
    public void testisexpression4() {
        assertEval("argv <- list(structure(list(nationality = structure(c(1L, 2L, 2L, 3L, 3L, 1L), .Label = c('Australia', 'UK', 'US'), class = 'factor'), deceased = structure(c(1L, 1L, 1L, 1L, 2L, 1L), .Label = c('no', 'yes'), class = 'factor'), title = structure(c(3L, 6L, 7L, 4L, 2L, 5L), .Label = c('An Introduction to R', 'Exploratory Data Analysis', 'Interactive Data Analysis', 'LISP-STAT', 'Modern Applied Statistics ...', 'Spatial Statistics', 'Stochastic Simulation'), class = 'factor'), other.author = structure(c(NA, NA, NA, NA, NA, 1L), .Label = c('Ripley', 'Venables & Smith'), class = 'factor')), .Names = c('nationality', 'deceased', 'title', 'other.author'), class = 'data.frame', row.names = c(NA, -6L)));is.expression(argv[[1]]);");
    }

    @Test
    public void testisexpression5() {
        assertEval("argv <- list(structure(c('Min.   :10.00  ', '1st Qu.:15.25  ', 'Median :20.50  ', 'Mean   :21.67  ', '3rd Qu.:25.50  ', 'Max.   :43.00  ', 'A:9  ', 'B:9  ', NA, NA, NA, NA), .Dim = c(6L, 2L), .Dimnames = list(c('', '', '', '', '', ''), c('    breaks', 'wool'))));is.expression(argv[[1]]);");
    }

    @Test
    public void testisexpression6() {
        assertEval("argv <- list(structure(c(1+2i, 5+0i, 3-4i, -6+0i), .Dim = c(2L, 2L)));is.expression(argv[[1]]);");
    }

    @Test
    public void testisexpression7() {
        assertEval("argv <- list(structure(list(), .Names = character(0), row.names = integer(0), class = 'data.frame'));is.expression(argv[[1]]);");
    }

    @Test
    public void testisexpression8() {
        assertEval("argv <- list(structure(3.14159265358979, class = structure('3.14159265358979', class = 'testit')));is.expression(argv[[1]]);");
    }

    @Test
    public void testisexpression9() {
        assertEval("argv <- list(structure(3.14159265358979, .Tsp = c(1, 1, 1), class = 'ts'));is.expression(argv[[1]]);");
    }

    @Test
    public void testisexpression10() {
        assertEval("argv <- list(structure(list(a = 1), .Dim = 1L, .Dimnames = list('a')));is.expression(argv[[1]]);");
    }

    @Test
    public void testisexpression11() {
        assertEval("argv <- list(structure(list(var = structure(c(3L, 5L, 3L, 1L, 1L, 1L, 3L, 4L, 1L, 2L, 1L, 1L, 1L), .Label = c('<leaf>', 'frost', 'life.exp', 'population', 'region'), class = 'factor'), n = c(50L, 21L, 13L, 10L, 3L, 8L, 29L, 21L, 4L, 17L, 9L, 8L, 8L), wt = c(50, 21, 13, 10, 3, 8, 29, 21, 4, 17, 9, 8, 8), dev = c(667.7458, 87.3866666666667, 18.8523076923077, 6.989, 2.84666666666667, 28.2, 222.311724137931, 116.909523809524, 10.18, 50.8823529411765, 24.24, 11.62, 14.415), yval = c(7.378, 4.23333333333333, 3.14615384615385, 2.69, 4.66666666666667, 6, 9.6551724137931, 8.56190476190476, 5.2, 9.35294117647059, 8.46666666666667, 10.35, 12.525), complexity = c(0.536203161735203, 0.0604037628905475, 0.0135031040639133, 0.00508384221318095, 0.01, 0.01, 0.136260236048519, 0.0836353757198433, 0.01, 0.0224971133344103, 0.01, 0.01, 0.01), ncompete = c(4L, 4L, 4L, 0L, 0L, 0L, 4L, 4L, 0L, 4L, 0L, 0L, 0L), nsurrogate = c(5L, 4L, 1L, 0L, 0L, 0L, 4L, 3L, 0L, 5L, 0L, 0L, 0L)), .Names = c('var', 'n', 'wt', 'dev', 'yval', 'complexity', 'ncompete', 'nsurrogate'), row.names = c(1L, 2L, 4L, 8L, 9L, 5L, 3L, 6L, 12L, 13L, 26L, 27L, 7L), class = 'data.frame'));is.expression(argv[[1]]);");
    }

    @Test
    public void testisexpression12() {
        assertEval("argv <- list(structure(c(1, 24.25, 56.5, 56.92771, 86.75, 117), .Names = c('Min.', '1st Qu.', 'Median', 'Mean', '3rd Qu.', 'Max.')));is.expression(argv[[1]]);");
    }

    @Test
    public void testisexpression13() {
        assertEval("argv <- list(1.79769313486232e+308);is.expression(argv[[1]]);");
    }

    @Test
    public void testisexpression14() {
        assertEval("argv <- list(c(1.1+0i, NA, 3+0i));is.expression(argv[[1]]);");
    }

    @Test
    public void testisexpression15() {
        assertEval("argv <- list(c(0, 0, 0, 0, 0, 1.75368801162502e-134, 0, 0, 0, 2.60477585273833e-251, 1.16485035372295e-260, 0, 1.53160350210786e-322, 0.333331382328728, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3.44161262707711e-123, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1.968811545398e-173, 0, 8.2359965384697e-150, 0, 0, 0, 0, 6.51733217171341e-10, 0, 2.36840184577368e-67, 0, 9.4348408357524e-307, 0, 1.59959906013771e-89, 0, 8.73836857865034e-286, 7.09716190970992e-54, 0, 0, 0, 1.530425353017e-274, 8.57590058044551e-14, 0.333333106397154, 0, 0, 1.36895217898448e-199, 2.0226102635783e-177, 5.50445388209462e-42, 0, 0, 0, 0, 1.07846402051283e-44, 1.88605464411243e-186, 1.09156111051203e-26, 0, 3.0702877273237e-124, 0.333333209689785, 0, 0, 0, 0, 0, 0, 3.09816093866831e-94, 0, 0, 4.7522727332095e-272, 0, 0, 2.30093251441394e-06, 0, 0, 1.27082826644707e-274, 0, 0, 0, 0, 0, 0, 0, 4.5662025456054e-65, 0, 2.77995853978268e-149, 0, 0, 0));is.expression(argv[[1]]);");
    }

    @Test
    public void testisexpression16() {
        assertEval("argv <- list(structure(c('***', '***', '*', '*'), legend = '0 ‘***’ 0.001 ‘**’ 0.01 ‘*’ 0.05 ‘.’ 0.1 ‘ ’ 1', class = 'noquote'));is.expression(argv[[1]]);");
    }

    @Test
    public void testisexpression17() {
        assertEval("argv <- list(structure(c(-0.0880891704401362, -0.508170309402877, -0.00510235947825228, 0.0737329622006759), .Names = c('(Intercept)', 'x1', 'z', 'x1:z')));is.expression(argv[[1]]);");
    }

    @Test
    public void testisexpression18() {
        assertEval("argv <- list(structure(c(-Inf, -Inf, -2.248e+263, -Inf, -3.777e+116, -1), .Names = c('Min.', '1st Qu.', 'Median', 'Mean', '3rd Qu.', 'Max.')));is.expression(argv[[1]]);");
    }

    @Test
    public void testisexpression19() {
        assertEval("argv <- list(quote(print(.leap.seconds, tz = 'PST8PDT')));is.expression(argv[[1]]);");
    }

    @Test
    public void testisexpression20() {
        assertEval("argv <- list(c(-0.5, -0.47979797979798, -0.45959595959596, -0.439393939393939, -0.419191919191919, -0.398989898989899, -0.378787878787879, -0.358585858585859, -0.338383838383838, -0.318181818181818, -0.297979797979798, -0.277777777777778, -0.257575757575758, -0.237373737373737, -0.217171717171717, -0.196969696969697, -0.176767676767677, -0.156565656565657, -0.136363636363636, -0.116161616161616, -0.0959595959595959, -0.0757575757575757, -0.0555555555555555, -0.0353535353535353, -0.0151515151515151, 0.00505050505050508, 0.0252525252525253, 0.0454545454545455, 0.0656565656565657, 0.0858585858585859, 0.106060606060606, 0.126262626262626, 0.146464646464647, 0.166666666666667, 0.186868686868687, 0.207070707070707, 0.227272727272727, 0.247474747474748, 0.267676767676768, 0.287878787878788, 0.308080808080808, 0.328282828282828, 0.348484848484849, 0.368686868686869, 0.388888888888889, 0.409090909090909, 0.429292929292929, 0.44949494949495, 0.46969696969697, 0.48989898989899, 0.51010101010101, 0.53030303030303, 0.550505050505051, 0.570707070707071, 0.590909090909091, 0.611111111111111, 0.631313131313131, 0.651515151515152, 0.671717171717172, 0.691919191919192, 0.712121212121212, 0.732323232323232, 0.752525252525253, 0.772727272727273, 0.792929292929293, 0.813131313131313, 0.833333333333333, 0.853535353535354, 0.873737373737374, 0.893939393939394, 0.914141414141414, 0.934343434343434, 0.954545454545455, 0.974747474747475, 0.994949494949495, 1.01515151515152, 1.03535353535354, 1.05555555555556, 1.07575757575758, 1.0959595959596, 1.11616161616162, 1.13636363636364, 1.15656565656566, 1.17676767676768, 1.1969696969697, 1.21717171717172, 1.23737373737374, 1.25757575757576, 1.27777777777778, 1.2979797979798, 1.31818181818182, 1.33838383838384, 1.35858585858586, 1.37878787878788, 1.3989898989899, 1.41919191919192, 1.43939393939394, 1.45959595959596, 1.47979797979798, 1.5));is.expression(argv[[1]]);");
    }

    @Test
    public void testisexpression21() {
        assertEval("argv <- list(structure(list(dim = 1L, dimnames = list('a')), .Names = c('dim', 'dimnames')));is.expression(argv[[1]]);");
    }

    @Test
    public void testisexpression22() {
        assertEval("argv <- list(structure(c(NA, 6346.2), .Names = c('1', '2')));is.expression(argv[[1]]);");
    }

    @Test
    public void testisexpression23() {
        assertEval("argv <- list(3.97376540705816e-12);is.expression(argv[[1]]);");
    }

    @Test
    public void testisexpression25() {
        assertEval("argv <- list(expression(quote(expression(b = pi^3))));do.call('is.expression', argv)");
    }
}
