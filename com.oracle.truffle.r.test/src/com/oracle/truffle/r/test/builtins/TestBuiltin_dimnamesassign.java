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

import org.junit.*;

import com.oracle.truffle.r.test.*;

// Checkstyle: stop line length check
public class TestBuiltin_dimnamesassign extends TestBase {

    @Test
    public void testdimnamesassign1() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(c(300, 3000, 400, 4000), .Dim = c(2L, 2L, 1L), .Dimnames = list(c('happy', 'sad'), NULL, '')), value = list(c('happy', 'sad'), NULL, ''));`dimnames<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testdimnamesassign2() {
        assertEval(Ignored.Unknown, "argv <- list(structure(1:24, .Dim = 2:4, .Dimnames = list(c('A', 'B'), NULL, NULL)), value = list(c('A', 'B'), NULL, NULL));`dimnames<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testdimnamesassign3() {
        assertEval(Ignored.Unknown, "argv <- list(structure(c(NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA), .Dim = 3:4), value = NULL);`dimnames<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testdimnamesassign4() {
        assertEval(Ignored.Unknown, "argv <- list(structure(numeric(0), .Dim = c(0L, 20L)), value = NULL);`dimnames<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testdimnamesassign5() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(c(0, 0, 0, 0, 0, 7, 0, 0, 0, 0, 12, 0, 0, 0, 0, 0, 0, 0, 0, 0, 22, 0, 0, 25, 0, 0, 0, 0, 30, 0, 0, 0, 0, 0, 0, 0, 38, 39, 0, 0, 0, 0, 0, 86, 0, 0, 0, 90, 91, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 103, 0, 0, 0, 0, 0, 0, 0, 0, 0, 113, 114, 0, 0, 0, 0, 0, 0, 121, 0, 0, 0, 0, 0, 128, 129, 0, 0, 132, 133, 0, 0, 0, 0, 138, 0, 0, 141, 142, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 159, 0, 1, 0, 0, 0, 2, 0, 0, 0, 1, 171, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 187, 0, 1, 0, 0, 0, 193, 0, 0, 196, 0, 0, 0, 1, 1, 202, 0, 0, 3, 0, 208, 0, 2, 0, 212, 0, 0, 0, 0, 0, 218, 0, 220, 1, 0, 0, 0, 0, 226, 227, 0, 2, 230, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 241, 0, 0, 0, 0, 0, 0, 0, 250, 251, 0, 0, 0, 255, 256, 257, 0, 0, 0, 261, 262, 0, 264, 0, 0, 0, 268, 0, 0, 0, 0, 0, 0, 275, 276, 0, 0, 0, 0, 0, 0, 0, 0, 0, 287, 0, 0, 290, 0, 292, 293, 0, 0, 0, 0, 0, 0, 300, 0, 0, 0, 0, 305, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 319, 0, 0, 0, 324, 0, 0, 0, 328, 329, 0, 0, 0, 0, 0, 335, 0, 0, 0, 339, 340, 0, 0, 343, 0, 0, 0, 0, 0, 0, 0, 351, 0, 0, 354, 0, 0, 0, 0, 0, 360, 0, 0, 0, 0, 0, 0, 0, 0, 0, 371, 0, 0, 0, 0, 0, 0, 0, 379, 0, 0, 0, 0, 0, 385, 0, 387, 0, 389, 0, 391, 0, 393, 394, 395, 396, 0, 0, 399, 0, 0, 0, 0, 405, 1, 407, 408, 0, 2, 0, 0, 0, 414, 415, 0, 417, 0, 0, 0, 0, 0, 0, 424, 0, 0, 0, 428, 1, 0, 431, 0, 433, 0, 435, 0, 0, 0, 439, 1, 441, 0, 0, 0, 0, 0, 0, 0, 0, 0, 452, 0, 0, 0, 0, 457, 0, 0, 0, 461, 0, 463, 0, 0, 0, 0, 0, 0, 0, 0, 0, 473, 474, 475, 0, 477, 0, 0, 0, 0, 482, 484, 0, 0, 487, 0, 0, 490, 491, 492, 0, 0, 0, 0, 0, 0, 499, 0, 501, 502, 0, 0, 0, 0, 0, 0, 0, 510, 0, 0, 0, 0, 515, 516, 0, 0, 519, 0, 0, 522, 524, 0, 0, 527, 528, 529, 530, 0, 532, 533, 0, 0, 0, 0, 538, 0, 0, 0, 0, 0, 0, 0, 0, 0, 548, 0, 0, 0, 0, 553, 0, 555, 0, 0, 0, 0, 560, 561, 0, 564, 0, 566, 0, 568, 0, 570, 0, 0, 0, 0, 0, 0, 0, 0, 0, 580, 0, 0, 0, 0, 0, 586, 0, 0, 589, 0, 0, 592, 593, 594, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 608, 0, 0, 0, 0, 0, 614, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 625, 626, 0, 628, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 640, 0, 0, 0, 645, 1, 0, 648, 0, 0, 0, 0, 653, 0, 0, 0, 657, 0, 0, 0, 0, 0, 0, 0, 665, 0, 0, 0, 0, 670, 671, 0, 0, 0, 675, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 691, 0, 0, 0, 695, 0, 697, 0, 0, 700, 0, 702, 0, 0, 0, 0, 0, 708, 0, 710, 711, 0, 0, 0, 0, 716, 0, 718, 0, 0, 0, 722, 0, 0, 0, 727, 728, 729, 0, 731, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 745, 0, 747, 0, 0, 750, 0, 0, 0, 0, 0, 0, 0, 0, 0, 760, 0, 0, 764, 0, 2, 0, 0, 0, 0, 0, 772, 0, 0, 0, 776, 777, 0, 0, 0, 1, 0, 0, 784, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 796, 0, 0, 0, 1), .Dim = c(39L, 19L)), value = NULL);`dimnames<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testdimnamesassign6() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(c(-75, 0, 103, 0, 124, -1, 0, -2.77555756156289e-17, 0, -1.66533453693773e-16, 0, 0, 0, 178, 0), .Dim = c(5L, 3L)), value = NULL);`dimnames<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testdimnamesassign7() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(c(-0.0124410638457178, NA, 0.00669768951701377, NA, 0.00669754897238661, NA, 3.45036480545864, 2.52673085623929, 1, 2.64771226663238, 0.0632378108418818, 0.404928794321981), .Dim = c(2L, 6L), .Dimnames = list(c('linear', 'nonlin'), NULL)), value = list(c('linear', 'nonlin'), NULL));`dimnames<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testdimnamesassign8() {
        assertEval(Ignored.Unknown, "argv <- list(structure(c(300, 3000, 400, 4000), .Dim = c(2L, 2L)), value = NULL);`dimnames<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testdimnamesassign9() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(c(1259, 1360, 845, 1053, 719, 774, 390, 413), .Dim = c(2L, 4L), .Dimnames = list(c('a', 'b'), NULL)), value = list(c('a', 'b')));`dimnames<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testdimnamesassign10() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(1:9, .Dim = c(3L, 3L), .Dimnames = list(c('x', 'y', NA), c('1', NA, '3'))), value = list(c('x', 'y', NA), c('1', NA, '3')));`dimnames<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testdimnamesassign11() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(c(0-0.5i, 0-0.577350269189626i, 0-0.707106781186548i, 0-1i, Inf+0i, 1+0i, 0.707106781186548+0i, 0.577350269189626+0i, 0.5+0i, 0.447213595499958+0i, 0.408248290463863+0i, 0.377964473009227+0i, 0.353553390593274+0i, 0.333333333333333+0i, 0.316227766016838+0i, 0.301511344577764+0i, 0.288675134594813+0i, 1+0i, 1+0i, 1+0i, 1+0i, 1+0i, 1+0i, 1+0i, 1+0i, 1+0i, 1+0i, 1+0i, 1+0i, 1+0i, 1+0i, 1+0i, 1+0i, 1+0i, 0+2i, 0+1.73205080756888i, 0+1.41421356237309i, 0+1i, 0+0i, 1+0i, 1.41421356237309+0i, 1.73205080756888+0i, 2+0i, 2.23606797749979+0i, 2.44948974278318+0i, 2.64575131106459+0i, 2.82842712474619+0i, 3+0i, 3.16227766016838+0i, 3.3166247903554+0i, 3.46410161513775+0i, -4+0i, -3+0i, -2+0i, -1+0i, 0+0i, 1+0i, 2+0i, 3+0i, 4+0i, 5+0i, 6+0i, 7+0i, 8+0i, 9+0i, 10+0i, 11+0i, 12+0i, 0-8i, 0-5.19615242270663i, 0-2.82842712474619i, 0-1i, 0+0i, 1+0i, 2.82842712474619+0i, 5.19615242270663+0i, 8+0i, 11.1803398874989+0i, 14.6969384566991+0i, 18.5202591774521+0i, 22.6274169979695+0i, 27+0i, 31.6227766016838+0i, 36.4828726939094+0i, 41.5692193816531+0i, 16+0i, 9+0i, 4+0i, 1+0i, 0+0i, 1+0i, 4+0i, 9+0i, 16+0i, 25+0i, 36+0i, 49+0i, 64+0i, 81+0i, 100+0i, 121+0i, 144+0i), .Dim = c(17L, 6L), .Dimnames = structure(list(c('-4', '-3', '-2', '-1', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '10', '11', '12'), `^` = c('-0.5', '0', '0.5', '1', '1.5', '2')), .Names = c('', '^'))), value = structure(list(c('-4', '-3', '-2', '-1', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '10', '11', '12'), `^` = c('-0.5', '0', '0.5', '1', '1.5', '2')), .Names = c('', '^')));`dimnames<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testdimnamesassign12() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(list(fair = c(326L, 688L, 343L, 98L), red = c(38L, 116L, 84L, 48L), medium = c(241L, 584L, 909L, 403L), dark = c(110L, 188L, 412L, 681L), black = c(3L, 4L, 26L, 85L)), .Names = c('fair', 'red', 'medium', 'dark', 'black'), class = 'data.frame', row.names = c('blue', 'light', 'medium', 'dark')), value = list(c('blue', 'light', 'medium', 'dark'), c('F', 'R', 'M', 'D', 'B')));`dimnames<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testdimnamesassign13() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(c(4L, 96L, 0L, 99L, 0L, 1L, 91L, 0L, 9L, 99L, 0L, 1L, 4L, 0L, 96L, 82L, 18L, 0L, 87L, 13L, 0L, 92L, 0L, 8L, 2L, 1L, 97L, 81L, 19L, 0L, 44L, 56L, 0L, 12L, 88L, 0L, 22L, 78L, 0L, 5L, 95L, 0L, 1L, 99L, 0L, 57L, 43L, 0L, 24L, 76L, 0L, 1L, 99L, 0L, 13L, 87L, 0L, 2L, 0L, 98L, 4L, 0L, 96L, 4L, 0L, 96L, 8L, 0L, 92L, 2L, 0L, 98L), .Dim = c(3L, 24L), .Dimnames = structure(list(cluster = c('1', '2', '3'), obs = c('  30', ' 243', ' 245', ' 309', ' 562', ' 610', ' 708', ' 727', ' 770', '1038', '1081', '1120', '1248', '1289', '1430', '1610', '1644', '1683', '1922', '2070', '2380', '2662', '2821', '2983')), .Names = c('cluster', 'obs'))), value = structure(list(cluster = c('1', '2', '3'), obs = c('  30', ' 243', ' 245', ' 309', ' 562', ' 610', ' 708', ' 727', ' 770', '1038', '1081', '1120', '1248', '1289', '1430', '1610', '1644', '1683', '1922', '2070', '2380', '2662', '2821', '2983')), .Names = c('cluster', 'obs')));`dimnames<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testdimnamesassign14() {
        assertEval(Ignored.Unknown, "argv <- list(structure(c(0, 0, 0, 0), .Dim = c(2L, 2L), .Dimnames = list(NULL, c('A', 'B'))), value = list(NULL, c('A', 'B')));`dimnames<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testdimnamesassign15() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(c('NULL', 'double', 'integer', 'complex', 'list', 'list', 'pairlist', 'builtin', 'closure', 'symbol', 'symbol', 'language', 'language', 'symbol', 'symbol', 'NULL', 'double', 'integer', 'complex', 'list', 'list', 'pairlist', 'function', 'function', 'symbol', 'symbol', 'language', 'language', 'symbol', 'symbol', 'NULL', 'numeric', 'numeric', 'complex', 'list', 'list', 'pairlist', 'function', 'function', 'name', 'name', 'call', '(', 'name', 'name'), .Dim = c(15L, 3L), .Dimnames = list(    c('NULL', '1', '1:1', '1i', 'list(1)', 'data.frame(x = 1)', 'pairlist(pi)', 'c', 'lm', 'formals(lm)[[1]]', 'formals(lm)[[2]]', 'y ~ x', 'expression((1))[[1]]', '(y ~ x)[[1]]', 'expression(x <- pi)[[1]][[1]]'), c('typeof(.)', 'storage.mode(.)', 'mode(.)'))), value = list(c('NULL', '1', '1:1', '1i', 'list(1)', 'data.frame(x = 1)', 'pairlist(pi)', 'c', 'lm', 'formals(lm)[[1]]', 'formals(lm)[[2]]', 'y ~ x', 'expression((1))[[1]]', '(y ~ x)[[1]]', 'expression(x <- pi)[[1]][[1]]'), c('typeof(.)', 'storage.mode(.)', 'mode(.)')));`dimnames<-`(argv[[1]],argv[[2]]);");
    }
}
