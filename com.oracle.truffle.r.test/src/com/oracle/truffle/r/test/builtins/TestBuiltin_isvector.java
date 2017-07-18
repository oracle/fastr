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
public class TestBuiltin_isvector extends TestBase {

    @Test
    public void testisvector1() {
        assertEval("argv <- list(list(structure(0, class = c('POSIXct', 'POSIXt'), tzone = 'GMT'), 1262304000), 'any'); .Internal(is.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testisvector2() {
        assertEval("argv <- list(list(c(' 1', 'NA', ' 1'), c('1.1', ' NA', '2.0'), c('1.1+0i', '    NA', '3.0+0i'), c('NA', 'NA', 'NA'), c('FALSE', '   NA', ' TRUE'), c('abc', NA, 'def')), 'any'); .Internal(is.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testisvector3() {
        assertEval("argv <- list(structure(list(A = c(1, NA, 1), B = c(1.1, NA, 2), C = c(1.1+0i, NA, 3+0i), D = c(NA_integer_, NA_integer_, NA_integer_), E = c(FALSE, NA, TRUE), F = c('abc', NA, 'def')), .Names = c('A', 'B', 'C', 'D', 'E', 'F'), class = 'data.frame', row.names = c('1', '2', '3')), 'any'); .Internal(is.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testisvector4() {
        assertEval("argv <- list(structure(list(character = character(0), numeric = numeric(0), numeric = numeric(0), complex = complex(0), integer = integer(0), logical = logical(0), character = character(0)), .Names = c('character', 'numeric', 'numeric', 'complex', 'integer', 'logical', 'character')), 'any'); .Internal(is.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testisvector5() {
        assertEval("argv <- list(structure(list(group = structure(c(1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L), .Label = c('Ctl', 'Trt'), class = 'factor')), .Names = 'group', class = 'data.frame', row.names = c(NA, 20L)), 'any'); .Internal(is.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testisvector6() {
        assertEval("argv <- list(structure(list(y = c(1.08728092481538, 0.0420572471552261, 0.787502161306819, 0.512717751544676, 3.35376639535311, 0.204341510750309, -0.334930602487435, 0.80049208412789, -0.416177803375218, -0.777970346246018, 0.934996808181635, -0.678786709127108, 1.52621589791412, 0.5895781228122, -0.744496121210548, -1.99065153885627, 1.51286447692396, -0.750182409847851), A = c(0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1), U = structure(c(1L, 1L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 2L, 2L, 3L, 3L, 3L, 3L, 3L, 3L), .Label = c('a', 'b', 'c'), class = 'factor'), V = structure(c(1L, 1L, 2L, 2L, 3L, 3L, 1L, 1L, 2L, 2L, 3L, 3L, 1L, 1L, 2L, 2L, 3L, 3L), .Label = c('a', 'b', 'c'), class = 'factor')), .Names = c('y', 'A', 'U', 'V'), terms = quote(y ~ A:U + A:V - 1), row.names = c(NA, 18L), class = 'data.frame'), 'any'); .Internal(is.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testisvector7() {
        assertEval("argv <- list(list(integer(0)), 'any'); .Internal(is.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testisvector8() {
        assertEval("argv <- list(list(structure(0:100, .Tsp = c(1, 101, 1), class = 'ts'), structure(c(0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 110, 120, 130, 140, 150, 160, 170, 180, 190, 200, 210, 220, 230, 240, 250, 260, 270, 280, 290, 300, 310, 320, 330, 340, 350, 360, 370, 380, 390, 400, 410, 420, 430, 440, 450, 460, 470, 480, 490, 500, 510, 520, 530, 540, 550, 560, 570, 580, 590, 600, 610, 620, 630, 640, 650, 660, 670, 680, 690, 700, 710, 720, 730, 740, 750, 760, 770, 780, 790, 800, 810, 820, 830, 840, 850, 860, 870, 880, 890, 900, 910, 920, 930, 940, 950, 960, 970, 980, 990, 1000), .Tsp = c(1, 101, 1), class = 'ts'), structure(c(-4, -3, -2, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96), .Tsp = c(1, 101, 1), class = 'ts')), 'any'); .Internal(is.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testisvector9() {
        assertEval("argv <- list(structure(list(Plant = structure(c(5L, 5L, 5L, 5L, 5L, 5L, 5L), .Label = c('Qn1', 'Qn2', 'Qn3', 'Qc1', 'Qc3', 'Qc2', 'Mn3', 'Mn2', 'Mn1', 'Mc2', 'Mc3', 'Mc1'), class = c('ordered', 'factor')), Type = structure(c(1L, 1L, 1L, 1L, 1L, 1L, 1L), .Label = c('Quebec', 'Mississippi'), class = 'factor'), Treatment = structure(c(2L, 2L, 2L, 2L, 2L, 2L, 2L), .Label = c('nonchilled', 'chilled'), class = 'factor')), .Names = c('Plant', 'Type', 'Treatment'), class = 'data.frame', row.names = 36:42), 'any'); .Internal(is.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testisvector10() {
        assertEval("argv <- list(structure(list(`1` = c(0, 0, 0, 0, 0, 0, 2.96439387504748e-323, 0, 0, 0, 0, 0)), .Names = '1'), 'any'); .Internal(is.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testisvector11() {
        assertEval("argv <- list(list(structure(list(srcfile = c('/home/lzhao/hg/r-instrumented/library/methods/R/methods', '/home/lzhao/hg/r-instrumented/library/methods/R/methods'), frow = c(6030L, 6032L), lrow = c(6031L, 6063L)), .Names = c('srcfile', 'frow', 'lrow'), row.names = 1:2, class = 'data.frame'), structure(list(srcfile = '/home/lzhao/hg/r-instrumented/library/methods/R/methods', frow = 6036L, lrow = 6055L), .Names = c('srcfile', 'frow', 'lrow'), row.names = c(NA, -1L), class = 'data.frame')), 'any'); .Internal(is.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testisvector12() {
        assertEval("argv <- list(structure(list(B = structure(c(1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 4L, 4L, 4L, 4L, 4L, 4L, 4L, 4L, 4L, 4L, 4L, 4L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L), .Label = c('I', 'II', 'III', 'IV', 'V', 'VI'), class = 'factor'), V = structure(c(3L, 3L, 3L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 3L, 3L, 3L, 3L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 3L, 3L, 3L, 3L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 3L, 3L, 3L, 3L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 3L, 3L, 3L, 3L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 3L, 3L, 3L, 3L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L), .Label = c('Golden.rain', 'Marvellous', 'Victory'), class = 'factor'), N = structure(c(2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L), .Label = c('0.0cwt', '0.2cwt', '0.4cwt', '0.6cwt'), class = 'factor')), .Names = c('B', 'V', 'N'), class = 'data.frame', row.names = 2:72), 'any'); .Internal(is.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testisvector13() {
        assertEval("argv <- list(list('‘FUN’ is found by a call to ‘match.fun’ and typically   is specified as a function or a symbol (e.g. a backquoted name) or a   character string specifying a function to be searched for from the   environment of the call to ‘lapply’.'), 'any'); .Internal(is.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testisvector14() {
        assertEval("argv <- list(list(c(35, 232, 232, 355, 1041, 1510, 1525, 1548, 1560, 1560, 1563, 1641, 1648, 1652, 1654, 1654, 1690, 1690, 1710, 1710, 1710, 1710, 1779, 1779, 1779, 1779, 1787, 1804, 1812, 1836, 1854, 1864, 1899, 1919, 1920, 1958, 1963, 2007, 2011, 2024, 2024, 2024, 2028, 2061, 2061, 2061, 2062, 2062, 2075, 2085, 2103, 2156, 2227, 2264, 2339, 2339, 2361, 2361, 2387, 2387, 2388, 2426, 2431, 2460, 2493, 2493, 2542, 2559, 2559, 2570, 2676, 2738, 2782, 2984, 3067, 3144, 3154, 3199, 3228, 3297, 3328, 3328, 3330, 3383, 3384, 3384, 3402, 3402, 3441, 3458, 3459, 3459, 3476, 3476, 3695, 3695, 3776, 3776, 3776, 3830, 3856, 3909, 3909, 3968, 3968, 4001, 4119, 4124, 4207, 4207, 4310, 4390, 4479, 4479, 4688), c(0, 0, 0, 0, -1, 0, 0, -1, -1, -1, 0, 0, 0, 0, 0, 0, -1, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, -1, -1, -1, -1, 0, 0, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)), 'any'); .Internal(is.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testisvector15() {
        assertEval("argv <- list(structure(c(1+2i, 5+0i, 3-4i, -6+0i), .Dim = c(2L, 2L)), 'any'); .Internal(is.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testisvector16() {
        assertEval("argv <- list(structure(list(row.names = character(0), A = numeric(0), B = numeric(0), C = complex(0), D = integer(0), E = logical(0), F = character(0)), .Names = c('row.names', 'A', 'B', 'C', 'D', 'E', 'F')), 'any'); .Internal(is.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testisvector17() {
        assertEval("argv <- list(structure(c(315.42, 316.32, 316.49, 317.56, 318.13, 318, 316.39, 314.66, 313.68, 313.18, 314.66, 315.43, 316.27, 316.81, 317.42, 318.87, 319.87, 319.43, 318.01, 315.75, 314, 313.68, 314.84, 316.03, 316.73, 317.54, 318.38, 319.31, 320.42, 319.61, 318.42, 316.64, 314.83, 315.15, 315.95, 316.85, 317.78, 318.4, 319.53, 320.41, 320.85, 320.45, 319.44, 317.25, 316.12, 315.27, 316.53, 317.53, 318.58, 318.92, 319.7, 321.22, 322.08, 321.31, 319.58, 317.61, 316.05, 315.83, 316.91, 318.2, 319.41, 320.07, 320.74, 321.4, 322.06, 321.73, 320.27, 318.54, 316.54, 316.71, 317.53, 318.55, 319.27, 320.28, 320.73, 321.97, 322, 321.71, 321.05, 318.71, 317.65, 317.14, 318.71, 319.25, 320.46, 321.43, 322.22, 323.54, 323.91, 323.59, 322.26, 320.21, 318.48, 317.94, 319.63, 320.87, 322.17, 322.34, 322.88, 324.25, 324.83, 323.93, 322.39, 320.76, 319.1, 319.23, 320.56, 321.8, 322.4, 322.99, 323.73, 324.86, 325.41, 325.19, 323.97, 321.92, 320.1, 319.96, 320.97, 322.48, 323.52, 323.89, 325.04, 326.01, 326.67, 325.96, 325.13, 322.9, 321.61, 321.01, 322.08, 323.37, 324.34, 325.3, 326.29, 327.54, 327.54, 327.21, 325.98, 324.42, 322.91, 322.9, 323.85, 324.96, 326.01, 326.51, 327.01, 327.62, 328.76, 328.4, 327.2, 325.28, 323.2, 323.4, 324.64, 325.85, 326.6, 327.47, 327.58, 329.56, 329.9, 328.92, 327.89, 326.17, 324.68, 325.04, 326.34, 327.39, 328.37, 329.4, 330.14, 331.33, 332.31, 331.9, 330.7, 329.15, 327.34, 327.02, 327.99, 328.48, 329.18, 330.55, 331.32, 332.48, 332.92, 332.08, 331.02, 329.24, 327.28, 327.21, 328.29, 329.41, 330.23, 331.24, 331.87, 333.14, 333.8, 333.42, 331.73, 329.9, 328.4, 328.17, 329.32, 330.59, 331.58, 332.39, 333.33, 334.41, 334.71, 334.17, 332.88, 330.77, 329.14, 328.77, 330.14, 331.52, 332.75, 333.25, 334.53, 335.9, 336.57, 336.1, 334.76, 332.59, 331.41, 330.98, 332.24, 333.68, 334.8, 335.22, 336.47, 337.59, 337.84, 337.72, 336.37, 334.51, 332.6, 332.37, 333.75, 334.79, 336.05, 336.59, 337.79, 338.71, 339.3, 339.12, 337.56, 335.92, 333.74, 333.7, 335.13, 336.56, 337.84, 338.19, 339.9, 340.6, 341.29, 341, 339.39, 337.43, 335.72, 335.84, 336.93, 338.04, 339.06, 340.3, 341.21, 342.33, 342.74, 342.07, 340.32, 338.27, 336.52, 336.68, 338.19, 339.44, 340.57, 341.44, 342.53, 343.39, 343.96, 343.18, 341.88, 339.65, 337.8, 337.69, 339.09, 340.32, 341.2, 342.35, 342.93, 344.77, 345.58, 345.14, 343.81, 342.22, 339.69, 339.82, 340.98, 342.82, 343.52, 344.33, 345.11, 346.88, 347.25, 346.61, 345.22, 343.11, 340.9, 341.17, 342.8, 344.04, 344.79, 345.82, 347.25, 348.17, 348.75, 348.07, 346.38, 344.52, 342.92, 342.63, 344.06, 345.38, 346.12, 346.79, 347.69, 349.38, 350.04, 349.38, 347.78, 345.75, 344.7, 344.01, 345.5, 346.75, 347.86, 348.32, 349.26, 350.84, 351.7, 351.11, 349.37, 347.97, 346.31, 346.22, 347.68, 348.82, 350.29, 351.58, 352.08, 353.45, 354.08, 353.66, 352.25, 350.3, 348.58, 348.74, 349.93, 351.21, 352.62, 352.93, 353.54, 355.27, 355.52, 354.97, 353.74, 351.51, 349.63, 349.82, 351.12, 352.35, 353.47, 354.51, 355.18, 355.98, 356.94, 355.99, 354.58, 352.68, 350.72, 350.92, 352.55, 353.91), .Tsp = c(1959, 1990.91666666667, 12), class = 'ts'), 'symbol'); .Internal(is.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testisvector18() {
        assertEval("argv <- list(structure(list(age = 60), .Names = 'age', row.names = c(NA, -1L), class = 'data.frame'), 'numeric'); .Internal(is.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testisvector19() {
        assertEval("argv <- list(c(1.00000001+1.00000001i, 1.00000002+1.00000002i, 1.00000003+1.00000003i, 1.00000004+1.00000004i, 1.00000005+1.00000005i, 1.00000006+1.00000006i, 1.00000007+1.00000007i, 1.00000008+1.00000008i, 1.00000009+1.00000009i, 1.0000001+1.0000001i), 'any'); .Internal(is.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testisvector20() {
        assertEval("argv <- list(list(), 'list'); .Internal(is.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testisvector21() {
        assertEval("argv <- list(structure(list(`1` = c(256.266652076228, 529.998452486383, 655.612271403493, 31.5607377310524, 10.1780771257452, 0.82654086298349, 0.192588149393303, 0.27340160887417, 0.420761091220242, 0.212073424883136, 6006.37649011526, 8.9782737548589e+42)), .Names = '1'), 'any'); .Internal(is.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testisvector22() {
        assertEval("argv <- list(structure(list(Subject = structure(c(40L, 40L, 40L, 40L, 40L, 40L, 40L, 40L), .Label = c('42', '28', '30', '56', '46', '5', '55', '32', '43', '29', '3', '11', '45', '22', '40', '47', '31', '14', '7', '41', '33', '44', '23', '57', '34', '18', '36', '21', '15', '38', '10', '1', '58', '51', '4', '6', '19', '2', '27', '53', '37', '20', '12', '9', '17', '26', '8', '49', '39', '54', '25', '35', '52', '13', '16', '59', '48', '24', '50'), class = c('ordered', 'factor')), Wt = c(1.7, 1.7, 1.7, 1.7, 1.7, 1.7, 1.7, 1.7), Apgar = structure(c(8L, 8L, 8L, 8L, 8L, 8L, 8L, 8L), .Label = c('1', '2', '3', '4', '5', '6', '7', '8', '9', '10'), class = c('ordered', 'factor')), ApgarInd = structure(c(2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L), .Label = c('< 5', '>= 5'), class = 'factor'), time = c(0, 4, 6, 23.8, 27, 28, 39.5, 47), dose = c(17, 17, NA, 4, 7.5, 4, 4, NA), conc = c(NA, NA, 19.1, NA, NA, NA, NA, 33.3)), .Names = c('Subject', 'Wt', 'Apgar', 'ApgarInd', 'time', 'dose', 'conc'), row.names = c('669', '670', '671', '672', '673', '674', '675', '676'), class = 'data.frame'), 'any'); .Internal(is.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testisvector23() {
        assertEval("argv <- list(list(structure(c(0.445983387275159, 0.0291424961297979, 0.305722673636889, 0.0640910333172597, 6.1841587262516e-05, 0.000608774190997193, 0.00533346072966287, 1.87468589092225, 0.00776943250876635, 0.00695873604736988), .Names = c('1', '2', '3', '4', '5', '6', '7', '8', '9', '10')), 0), 'any'); .Internal(is.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testisvector24() {
        assertEval("argv <- list(structure(list(`1` = structure(list(stats = c(-9.90250128905181, -7.70852699427806, -5.13496062122677, -3.95948091921295, 1.46970778502994), n = 29L, conf = c(-6.23492561965266, -4.03499562280088), out = numeric(0)), .Names = c('stats', 'n', 'conf', 'out')), `2` = structure(list(stats = c(-11.1332994435188, -6.39592651878103, -4.21825647639093, -2.4710346135438, 0.142332232638518), n = 27L, conf = c(-5.41170283935282, -3.02481011342905), out = c(5.49660997093232, 3.55716552721441)), .Names = c('stats', 'n', 'conf', 'out')), `3` = structure(list(stats = c(-6.21825647639093, -4.89504044270307, -3.50339002906768, -2.12460329098075, 1.68674122392151), n = 27L, conf = c(-4.34580001700278, -2.66098004113257), out = 3.49961675599928), .Names = c('stats', 'n', 'conf', 'out')), `4` = structure(list(stats = c(-5.85766776736148, -2.17391966458661, -1.21825647639093, 0.620760276498144, 4.06758688195534), n = 27L, conf = c(-2.06803799696724, -0.368474955814626), out = numeric(0)), .Names = c('stats', 'n', 'conf', 'out')), `5` = structure(list(stats = c(-7.47307099543129, -3.31679346391683, -0.571500134198763, 0.883997101871453, 5.68320653608317), n = 29L, conf = c(-1.8040063492643, 0.661006080866776), out = numeric(0)), .Names = c('stats', 'n', 'conf', 'out')), `6` = structure(list(stats = c(-5.74371144541934, -1.32005454439779, -0.0691719185754582, 1.68918858100201, 5.88399710187145), n = 25L, conf = c(-1.02009274620179, 0.881748909050878), out = numeric(0)), .Names = c('stats', 'n', 'conf', 'out')), `7` = structure(list(    stats = c(-0.959725366614089, 1.04051908078705, 2.68320653608317, 4.68320653608317, 6.86503937877323), n = 29L, conf = c(1.61444701144563, 3.7519660607207), out = c(10.4020441274641, 11.9338597320297)), .Names = c('stats', 'n', 'conf', 'out')), `8` = structure(list(stats = c(-4.31872184443094, -0.341901367712618, 2.09749871094819, 4.42849305489153, 9.86504029478776), n = 29L, conf = c(0.69787150218962, 3.49712591970676), out = numeric(0)), .Names = c('stats', 'n', 'conf', 'out')), `9` = structure(list(    stats = c(-4.47307099543129, 1.46970778502994, 3.27573063223268, 6.09749871094819, 8.5266833961141), n = 29L, conf = c(1.91794309465097, 4.63351816981439), out = numeric(0)), .Names = c('stats', 'n', 'conf', 'out')), `10` = structure(list(stats = c(-5.31679346391683, -1.04725655125673, 2.61440793064106, 7.68320653608317, 14.5985362650564), n = 26L, conf = c(-0.0908438616349159, 5.31965972291703), out = numeric(0)), .Names = c('stats', 'n', 'conf', 'out')), `11` = structure(list(stats = c(-4.26322883423527, -1.27884444060771, 1.37070166144218, 5.85051662661329, 10.1854674121229), n = 31L, conf = c(-0.65244259398095, 3.39384591686531), out = numeric(0)), .Names = c('stats', 'n', 'conf', 'out'))), .Dim = 11L, .Dimnames = list(c('1', '2', '3', '4', '5', '6', '7', '8', '9', '10', '11'))), 'any'); .Internal(is.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testisvector25() {
        assertEval("argv <- list(structure(list(A = c(1L, NA, 1L), B = c(1.1, NA, 2), C = c(1.1+0i, NA, 3+0i), D = c(NA, NA, NA), E = c(FALSE, NA, TRUE), F = structure(c(1L, NA, 2L), .Label = c('abc', 'def'), class = 'factor')), .Names = c('A', 'B', 'C', 'D', 'E', 'F'), class = 'data.frame', row.names = c('1', '2', '3')), 'any'); .Internal(is.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testisvector26() {
        assertEval("argv <- list(list(structure(list(srcfile = c(NA, '/home/lzhao/hg/r-instrumented/library/graphics/R/graphics'), frow = c(NA, 3990L), lrow = c(NA, 3991L)), .Names = c('srcfile', 'frow', 'lrow'), row.names = 1:2, class = 'data.frame'), structure(list(srcfile = '/home/lzhao/hg/r-instrumented/library/graphics/R/graphics', frow = 3998L, lrow = 4009L), .Names = c('srcfile', 'frow', 'lrow'), row.names = c(NA, -1L), class = 'data.frame')), 'any'); .Internal(is.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testisvector27() {
        assertEval("argv <- list(structure(list(y = c(-0.0561287395290008, -0.155795506705329, -1.47075238389927, -0.47815005510862, 0.417941560199702, 1.35867955152904, -0.102787727342996, 0.387671611559369, -0.0538050405829051, -1.37705955682861), x = c(TRUE, FALSE, TRUE, FALSE, TRUE, FALSE, TRUE, FALSE, TRUE, FALSE), z = 1:10), .Names = c('y', 'x', 'z'), terms = quote(y ~ x * z), row.names = c(NA, 10L), class = 'data.frame'), 'any'); .Internal(is.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testisvector28() {
        assertEval("argv <- list(list(structure(c(-1L, -2L, -3L, -4L, -5L, -6L, -7L, -8L, -9L, -10L), .Dim = c(2L, 5L)), structure(list(V1 = 1:5, V2 = 6:10, V3 = 11:15, V4 = 16:20, V5 = 21:25), .Names = c('V1', 'V2', 'V3', 'V4', 'V5'), row.names = c(NA, -5L), class = 'data.frame')), 'any'); .Internal(is.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testisvector29() {
        assertEval("argv <- list(structure(list(group = structure(c(1L, 1L), .Label = c('Ctl', 'Trt'), class = 'factor', contrasts = 'contr.treatment')), .Names = 'group', class = 'data.frame', row.names = 1:2), 'any'); .Internal(is.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testisvector30() {
        assertEval("argv <- list(structure(c(12L, 120L, 116L), .Dim = 3L, .Dimnames = structure(list(c('0-5yrs', '6-11yrs', '12+ yrs')), .Names = ''), class = 'table'), 'any'); .Internal(is.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testisvector31() {
        assertEval("argv <- list(structure(c(1L, 3L, 2L, 3L, 3L, 1L, 2L, 3L, 2L, 2L), .Label = c('A', 'B', 'C'), class = 'factor'), 'symbol'); .Internal(is.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testisvector32() {
        assertEval("argv <- list(structure(list(Df = c(NA, 0L), Deviance = c(NA, 0), `Resid. Df` = c(10L, 10L), `Resid. Dev` = c(2.74035772634541, 2.74035772634541)), .Names = c('Df', 'Deviance', 'Resid. Df', 'Resid. Dev'), row.names = c('NULL', 'x'), class = c('anova', 'data.frame'), heading = 'Analysis of Deviance Table\\n\\nModel: gaussian, link: identity\\n\\nResponse: y\\n\\nTerms added sequentially (first to last)\\n\\n'), 'any'); .Internal(is.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testisvector34() {
        assertEval("argv <- structure(list(x = 3), .Names = 'x');do.call('is.vector', argv)");
    }

    @Test
    public void testIsVector() {
        assertEval("{ is.vector(1) }");
        assertEval("{ is.vector(1:3) }");
        assertEval("{ is.vector(NULL) }");
        assertEval("{ x<-c(1,3); is.vector(x, \"double\"); }");
        assertEval("{ x<-c(1,3); is.vector(x, \"integer\"); }");
        assertEval("{ x<-c(1:3); is.vector(x, \"double\"); }");
        assertEval("{ x<-c(1:3); is.vector(x, \"integer\"); }");
        assertEval("{ x<-c(1,3); is.vector(x, \"d\"); }");
        assertEval("{ x<-list(1,3); }");
        assertEval("{ x<-c(1); attr(x, \"foo\")<-\"foo\"; is.vector(x) }");
        assertEval("{ x<-list(1); attr(x, \"foo\")<-\"foo\"; is.vector(x) }");
        assertEval("{is.vector(c(TRUE,FALSE),\"numeric\");}");
        assertEval("{is.vector(c(TRUE,FALSE),\"logical\");}");
        assertEval("{x<-1;class(x)<-\"a\";is.vector(x);}");
        assertEval("{x<-1;names(x)<-\"a\";is.vector(x);}");
        assertEval("is.vector(1L, 'numeric');");
        assertEval("{is.vector(c(1,2), c(\"sss\", \"dddd\"));}");
        assertEval("{is.vector(c(1,2), TRUE);}");
    }
}
