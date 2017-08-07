/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check
public class TestBuiltin_isarray extends TestBase {

    @Test
    public void testisarray1() {
        assertEval("argv <- list(structure(list(weight = c(4.17, 5.58), group = structure(c(1L, 1L), .Label = c('Ctl', 'Trt'), class = 'factor')), .Names = c('weight', 'group'), row.names = 1:2, class = 'data.frame'));is.array(argv[[1]]);");
    }

    @Test
    public void testisarray2() {
        assertEval("argv <- list(structure(list(weight = c(4.17, 5.58, 5.18, 6.11, 4.5, 4.61, 5.17, 4.53, 5.33, 5.14, 4.81, 4.17, 4.41, 3.59, 5.87, 3.83, 6.03, 4.89, 4.32, 4.69), group = structure(c(1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L), .Label = c('Ctl', 'Trt'), class = 'factor')), .Names = c('weight', 'group'), row.names = c(NA, -20L), class = 'data.frame'));is.array(argv[[1]]);");
    }

    @Test
    public void testisarray3() {
        assertEval("argv <- list(structure(c(3.3675190249981e-55, 0.765191717864009, 1.84321904447013e-13, 0.270563224172485, 1.09996038197079, 1.31584681249013e-26, 0.00018392029356426, 0.515909871821833, 3.2666146281237e-45, -9.79475754683005e-56, -0.139604410987981, -1.56689901864133e-13, -0.285096750996398, -0.00590762252543826, -1.87837727043588e-27, -1.95765906855729e-05, -0.587847588896037, -1.0000270983218e-45), .Dim = c(9L, 2L), .Dimnames = list(NULL, c('Comp.1', 'Comp.2'))));is.array(argv[[1]]);");
    }

    @Test
    public void testisarray4() {
        assertEval("argv <- list(c(0, 0, 0, 0, 0, 1.75368801162502e-134, 0, 0, 0, 2.60477585273833e-251, 1.16485035372295e-260, 0, 1.53160350210786e-322, 0.333331382328728, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3.44161262707711e-123, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1.968811545398e-173, 0, 8.2359965384697e-150, 0, 0, 0, 0, 6.51733217171341e-10, 0, 2.36840184577368e-67, 0, 9.4348408357524e-307, 0, 1.59959906013771e-89, 0, 8.73836857865034e-286, 7.09716190970992e-54, 0, 0, 0, 1.530425353017e-274, 8.57590058044551e-14, 0.333333106397154, 0, 0, 1.36895217898448e-199, 2.0226102635783e-177, 5.50445388209462e-42, 0, 0, 0, 0, 1.07846402051283e-44, 1.88605464411243e-186, 1.09156111051203e-26, 0, 3.0702877273237e-124, 0.333333209689785, 0, 0, 0, 0, 0, 0, 3.09816093866831e-94, 0, 0, 4.7522727332095e-272, 0, 0, 2.30093251441394e-06, 0, 0, 1.27082826644707e-274, 0, 0, 0, 0, 0, 0, 0, 4.5662025456054e-65, 0, 2.77995853978268e-149, 0, 0, 0));is.array(argv[[1]]);");
    }

    @Test
    public void testisarray5() {
        assertEval("argv <- list(structure(c(TRUE, TRUE, FALSE, FALSE, TRUE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, FALSE, TRUE, TRUE, FALSE, FALSE), .Dim = c(11L, 2L)));is.array(argv[[1]]);");
    }

    @Test
    public void testisarray6() {
        assertEval("argv <- list(structure(c(3+2i, 3+2i, NA, 3+2i, 3+2i, 3+2i, 3+2i, 3+2i, 4-5i, 3-5i, NA, NA, 2-5i, 3-5i, 4-5i, 5-5i), .Dim = c(8L, 2L), .Dimnames = list(NULL, c('x1', 'x2'))));is.array(argv[[1]]);");
    }

    @Test
    public void testisarray7() {
        assertEval("argv <- list(c('2001-01-01', NA, NA, '2004-10-26'));is.array(argv[[1]]);");
    }

    @Test
    public void testisarray8() {
        assertEval("argv <- list(structure(list(breaks = c(26, 30, 54, 25, 70, 52, 51, 26, 67, 27, 14, 29, 19, 29, 31, 41, 20, 44), wool = structure(c(1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L), .Label = c('A', 'B'), class = 'factor'), tension = structure(c(1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L), .Label = c('L', 'M', 'H'), class = 'factor')), .Names = c('breaks', 'wool', 'tension'), row.names = c(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 28L, 29L, 30L, 31L, 32L, 33L, 34L, 35L, 36L), class = 'data.frame'));is.array(argv[[1]]);");
    }

    @Test
    public void testisarray9() {
        assertEval("argv <- list(structure(list(carb = c(33, 40, 37, 27, 30, 43, 34, 48, 30, 38, 50, 51, 30, 36, 41, 42, 46, 24, 35, 37), age = c(33, 47, 49, 35, 46, 52, 62, 23, 32, 42, 31, 61, 63, 40, 50, 64, 56, 61, 48, 28), wgt = c(100, 92, 135, 144, 140, 101, 95, 101, 98, 105, 108, 85, 130, 127, 109, 107, 117, 100, 118, 102), prot = c(14, 15, 18, 12, 15, 15, 14, 17, 15, 14, 17, 19, 19, 20, 15, 16, 18, 13, 18, 14)), .Names = c('carb', 'age', 'wgt', 'prot'), row.names = c(NA, -20L), class = 'data.frame'));is.array(argv[[1]]);");
    }

    @Test
    public void testisarray10() {
        assertEval("argv <- list(structure(integer(0), .Names = character(0)));is.array(argv[[1]]);");
    }

    @Test
    public void testisarray11() {
        assertEval("argv <- list(structure(list(c0 = logical(0)), .Names = 'c0', row.names = integer(0), class = 'difftime'));is.array(argv[[1]]);");
    }

    @Test
    public void testisarray12() {
        assertEval("argv <- list(structure(list(B = structure(c(1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 4L, 4L, 4L, 4L, 4L, 4L, 4L, 4L, 4L, 4L, 4L, 4L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L), .Label = c('I', 'II', 'III', 'IV', 'V', 'VI'), class = 'factor'), V = structure(c(3L, 3L, 3L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 3L, 3L, 3L, 3L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 3L, 3L, 3L, 3L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 3L, 3L, 3L, 3L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 3L, 3L, 3L, 3L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 3L, 3L, 3L, 3L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L), .Label = c('Golden.rain', 'Marvellous', 'Victory'), class = 'factor'), N = structure(c(2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L), .Label = c('0.0cwt', '0.2cwt', '0.4cwt', '0.6cwt'), class = 'factor'), Y = c(130L, 157L, 174L, 117L, 114L, 161L, 141L, 105L, 140L, 118L, 156L, 61L, 91L, 97L, 100L, 70L, 108L, 126L, 149L, 96L, 124L, 121L, 144L, 68L, 64L, 112L, 86L, 60L, 102L, 89L, 96L, 89L, 129L, 132L, 124L, 74L, 89L, 81L, 122L, 64L, 103L, 132L, 133L, 70L, 89L, 104L, 117L, 62L, 90L, 100L, 116L, 80L, 82L, 94L, 126L, 63L, 70L, 109L, 99L, 53L, 74L, 118L, 113L, 89L, 82L, 86L, 104L, 97L, 99L, 119L, 121L)), .Names = c('B', 'V', 'N', 'Y'), row.names = 2:72, class = 'data.frame'));is.array(argv[[1]]);");
    }

    @Test
    public void testisarray13() {
        assertEval("argv <- list(1.79769313486232e+308);is.array(argv[[1]]);");
    }

    @Test
    public void testisarray14() {
        assertEval("argv <- list(structure(list(a = 1), .Dim = 1L, .Dimnames = list('a')));is.array(argv[[1]]);");
    }

    @Test
    public void testisarray15() {
        assertEval("argv <- list(structure(c(-0.826728474083517, -0.154469781470927, 0.390336546510923, 1.36531474451071, 2.06722571939869, 1.96521311125433, 1.45602298338166, 0.730191404067138, -0.941608750081938, -0.839732558506723, -0.0905911085922035, 0.450465973953452, 1.12065714554563, 1.45760317860118, 1.25109677501882, 1.2162018587134, 1.45496813096317, -0.271923056200992, -2.39070401244086, -2.40312653243942, 1.3490211050246, 0.723634003520978, -0.525571703048375, -2.20018568844273, -1.57021740950431, -1.15394138529193, -0.771438011070496, 0.948968136215006, 1.11028283982967, 1.3490211050246, 0.723634003520978, -0.771438011070496, -1.76787333029837, -2.97849067734298, -2.56564546193719, -1.42386100140387, -0.482534191368393, 0.15506930200634, 0.878254497780181, 1.05319014844382, 0.0786502243396558, -0.896275208780418, -0.852907979665288, -1.36905276490888, -0.852907979665288, 0.378793517229249, 1.51661659600387, 1.37393548755461, -1.19044178751146, -1.01371204398328, -0.413541319881442, -0.0155111902607956, 0.511260101660621, 1.1596935438887, 1.49073250236106, 1.90481616825336, 1.72198968844944, 1.06922804353907, -0.525571703048375, -2.20018568844273, -1.57021740950431, -1.15394138529193, -0.771438011070496, 0.948968136215006, 1.11028283982967, 1.3490211050246, 0.723634003520978, -0.771438011070496, -1.76787333029837, 1.90481616825336, -1.15394138529193, -0.771438011070496, 0.948968136215006, 1.11028283982967, 1.3490211050246, 0.723634003520978, -0.771438011070496, -1.76787333029837, 1.17726251300777, 0.873546391155001, -0.257195249490748, -1.08470959372261, -1.32132136208769, -1.28389495656857, -0.471605120836204, 0.606878400401293, 1.31985237043395, 2.02783906485667, 1.57046182864688, -0.252818874890949, -1.24388962195487, -0.626057778621366, 1.49073250236106, 1.90481616825336, 1.72198968844944, 1.06922804353907, -0.525571703048375, -2.20018568844273, -1.57021740950431, -1.15394138529193, -0.771438011070496, 0.948968136215006, 1.11028283982967, 1.45760317860118, 1.25109677501882, 1.2162018587134, 1.45496813096317, -0.271923056200992, -2.39070401244086, -2.40312653243942, -2.10302193998908, -1.35143116355906, -0.796191750223435, 0.24658809164983), .Dim = c(114L, 1L), .Dimnames = list(NULL, 'Series 1'), .Tsp = c(1, 114, 1), class = 'ts'));is.array(argv[[1]]);");
    }

    @Test
    public void testisarray16() {
        assertEval("argv <- list(structure(c(1.82608695652174, 1.17391304347826, 1.17391304347826, 4.17391304347826, 1.82608695652174, 1.82608695652174, 2.17391304347826, 0.173913043478262, 4.17391304347826, 0.826086956521738, 2.17391304347826, 2.17391304347826, 2.17391304347826, 4.17391304347826, 2.82608695652174, 2.17391304347826, 2.17391304347826, 0.826086956521738, 3.82608695652174, 1.82608695652174, 4.82608695652174, 0.173913043478262, 7.82608695652174, 1.15, 5.15, 0.85, 1.15, 2.85, 2.85, 1.85, NA, 6.15, 2.85, 0.15, 3.15, 0.15, NA, NA, 1.15, 1.85, 0.15, 0.85, 0.85, 2.15, 2.85, 2.85, 32.2608695652174, 54.2608695652174, 36.2608695652174, 45.2608695652174, 194.739130434783, 130.739130434783, 35.2608695652174, 59.2608695652174, 63.2608695652174, 25.7391304347826, 25.2608695652174, 44.2608695652174, 16.2608695652174, 63.2608695652174, 53.2608695652174, 56.2608695652174, 19.2608695652174, 35.2608695652174, 39.2608695652174, 7.26086956521739, 38.2608695652174, 213.739130434783, 158.739130434783, 8.09999999999999, 94.9, 59.9, 49.9, 176.1, 11.1, 59.9, NA, 100.1, 15.1, 21.1, 84.1, 65.1, NA, NA, 63.9, 37.9, 26.9, 128.9, 42.1, 87.9, 118.1, 30.9), .Dim = c(23L, 4L), .Dimnames = list(NULL, c('V1', 'V2', 'V3', 'V4')), '`scaled:center`' = structure(c(10.8260869565217, 3.85, 95.2608695652174, 137.9), .Names = c('V1', 'V2', 'V3', 'V4'))));is.array(argv[[1]]);");
    }

    @Test
    public void testisarray17() {
        assertEval("argv <- list(c('1', '2', NA));is.array(argv[[1]]);");
    }

    @Test
    public void testisarray18() {
        assertEval("argv <- list(structure(list(y = c(0.219628047744843, 0.360454661130887, NA, 0.114681204747219, -1.14267533343616, 0.772374419482067, 0.681741904304867, 0.171869265068012, 2.08409180391906, 0.367547276775469), x1 = c(1L, 2L, 3L, NA, 5L, 6L, 7L, 8L, 9L, 10L), x2 = 1:10, x3 = c(0, 0, 0, 0, 0, 0, 0, 0, 0, 0), wt = c(0, 1, 1, 1, 1, 1, 1, 1, 1, 1)), .Names = c('y', 'x1', 'x2', 'x3', 'wt'), row.names = c('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j'), class = 'data.frame'));is.array(argv[[1]]);");
    }

    @Test
    public void testisarray19() {
        assertEval("argv <- list(structure(list(x = c(TRUE, FALSE, TRUE, FALSE, TRUE, FALSE, TRUE, FALSE, TRUE, FALSE), y = c(-0.0561287395290008, -0.155795506705329, -1.47075238389927, -0.47815005510862, 0.417941560199702, 1.35867955152904, -0.102787727342996, 0.387671611559369, -0.0538050405829051, -1.37705955682861)), .Names = c('x', 'y'), row.names = c(NA, -10L), class = 'data.frame'));is.array(argv[[1]]);");
    }

    @Test
    public void testisarray20() {
        assertEval("argv <- list(structure(c(365, 365, 365, 366, 1, 0), .Dim = c(3L, 2L)));is.array(argv[[1]]);");
    }

    @Test
    public void testisarray21() {
        assertEval("argv <- list(integer(0));is.array(argv[[1]]);");
    }

    @Test
    public void testisarray22() {
        assertEval("argv <- list(structure(1:12, .Dim = 3:4, .Dimnames = list(c('A', 'B', 'C'), c('D', 'E', 'F', 'G'))));is.array(argv[[1]]);");
    }

    @Test
    public void testisarray23() {
        assertEval("argv <- list(c(0.568, 1.432, -1.08, 1.08));is.array(argv[[1]]);");
    }

    @Test
    public void testisarrayGenericDispatch() {
        assertEval("{ is.array.cls <- function(x) 42; is.array(structure(c(1,2), class='cls')); }");
    }
}
