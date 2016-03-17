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
public class TestBuiltin_subset2 extends TestBase {

    @Test
    public void testsubset21() {
        assertEval("argv <- list(structure(list(par.vals = structure(c(43.6690361821048, 35.0518890362789, 30.2558850234373, 27.1664611723591, 24.9930921115624, 23.3776455926353, 22.122313646246, 21.1173217554787, 20.293145402391, 19.6041024133034, 19.0188803067124, 18.5152545044936, 18.0769941360739, 17.6919540860845, 17.3508558987268, 17.0464826391527, 0.924696372559026, 1.4577878275186, 1.99087928247818, 2.52397073743776, 3.05706219239734, 3.59015364735691, 4.12324510231649, 4.65633655727607, 5.18942801223565, 5.72251946719522, 6.2556109221548, 6.78870237711438, 7.32179383207396, 7.85488528703353, 8.38797674199311, 8.92106819695269), .Dim = c(16L, 2L), .Dimnames = list(NULL, c('ymax', 'xhalf')))), .Names = 'par.vals'), 1L);.subset2(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testsubset22() {
        assertEval("argv <- list(structure(list(frow = c(NA, 2467L, 2468L, 2470L, 2470L, 2477L, 2478L, 2478L, 2480L, 2480L, 2482L, 2482L, 2482L, 2484L, 2484L, 2486L, 2486L, 2486L, 2490L, 2491L)), .Names = 'frow'), 1L);.subset2(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testsubset23() {
        assertEval("argv <- list(structure(list(x = c(0, 0, 1, 1), y = c(2, 2, 9, 9), z = c(0, 0, -3, -3), u = c(34, 35, 19, 37)), .Names = c('x', 'y', 'z', 'u'), row.names = c(2L, 90L, 25L, 50L), class = 'data.frame'), 4L);.subset2(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testsubset24() {
        assertEval("argv <- list(NULL, NULL);.subset2(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testsubset25() {
        assertEval("argv <- list(structure(list(V1 = c(1L, 9L), V2 = c(NA, NA), V3 = c(23L, 87L), V4 = c(NA, 654L)), .Names = c('V1', 'V2', 'V3', 'V4'), class = 'data.frame', row.names = c(NA, -2L)), 2L);.subset2(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testsubset26() {
        assertEval("argv <- list(structure(list(Df = c(NA, 2L, 2L), Deviance = c(NA, 5.45230478674972, 2.66453525910038e-15), `Resid. Df` = c(8L, 6L, 4L), `Resid. Dev` = c(10.5814458637509, 5.12914107700115, 5.12914107700115)), .Names = c('Df', 'Deviance', 'Resid. Df', 'Resid. Dev'), row.names = c('NULL', 'outcome', 'treatment'), class = c('anova', 'data.frame'), heading = 'Analysis of Deviance Table\\n\\nModel: poisson, link: log\\n\\nResponse: counts\\n\\nTerms added sequentially (first to last)\\n\\n'), 4L);.subset2(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testsubset27() {
        assertEval("argv <- list(structure(list(surname = structure(c('Tukey', 'Venables', 'Tierney', 'Ripley', 'McNeil'), class = 'AsIs')), .Names = 'surname'), 1L);.subset2(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testsubset28() {
        assertEval("argv <- list(structure(list(surname = structure('R Core', class = 'AsIs'), nationality = structure(NA_integer_, .Label = c('Australia', 'UK', 'US'), class = 'factor'), deceased = structure(NA_integer_, .Label = c('no', 'yes'), class = 'factor')), .Names = c('surname', 'nationality', 'deceased'), row.names = 7L, class = 'data.frame'), 1L);.subset2(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testsubset29() {
        assertEval("argv <- list(structure(list(z = structure(c('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i'), .Dim = c(3L, 3L), class = 'AsIs')), .Names = 'z'), 1L);.subset2(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testsubset211() {
        assertEval("argv <- list(structure(list(V1 = structure(c(4L, 1L, 2L, 3L), .Label = c('1', '3', '6', 'head'), class = 'factor'), V2 = c(NA, 2L, 4L, 7L), V3 = c(NA, NA, 5L, 8L), V4 = c(NA, NA, NA, 9L)), .Names = c('V1', 'V2', 'V3', 'V4'), class = 'data.frame', row.names = c(NA, -4L)), 2L);.subset2(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testsubset212() {
        assertEval("argv <- list(structure(list(Res.Df = c(20, 21), RSS = c(652424.52183908, 658770.746755654), Df = c(NA, -1), `Sum of Sq` = c(NA, -6346.22491657443), F = c(NA, 0.194542807762205), `Pr(>F)` = c(NA, 0.663893424608742)), .Names = c('Res.Df', 'RSS', 'Df', 'Sum of Sq', 'F', 'Pr(>F)'), row.names = c('1', '2'), class = c('anova', 'data.frame'), heading = c('Analysis of Variance Table\\n', 'Model 1: birthw ~ sex + sex:age - 1\\nModel 2: birthw ~ sex + age - 1')), 6L);.subset2(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testsubset213() {
        assertEval("argv <- list(structure(list(a = structure('abc', class = 'AsIs'), b = structure('def\\\'gh', class = 'AsIs')), .Names = c('a', 'b'), row.names = '1', class = 'data.frame'), 1L);.subset2(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testsubset214() {
        assertEval("argv <- list(structure(list(mtime = structure(1395082258.61787, class = c('POSIXct', 'POSIXt'))), .Names = 'mtime'), 1L);.subset2(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testsubset215() {
        assertEval("argv <- list(structure(list(A = 0:10, `NA` = 20:30), .Names = c('A', NA), class = 'data.frame', row.names = c(NA, -11L)), 2L);.subset2(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testsubset216() {
        assertEval("argv <- list(structure(list(Employed = c(60.323, 61.122, 60.171, 61.187, 63.221, 63.639, 64.989, 63.761, 66.019, 67.857, 68.169, 66.513, 68.655, 69.564, 69.331, 70.551), GNP.deflator = c(83, 88.5, 88.2, 89.5, 96.2, 98.1, 99, 100, 101.2, 104.6, 108.4, 110.8, 112.6, 114.2, 115.7, 116.9), GNP = c(234.289, 259.426, 258.054, 284.599, 328.975, 346.999, 365.385, 363.112, 397.469, 419.18, 442.769, 444.546, 482.704, 502.601, 518.173, 554.894), Unemployed = c(235.6, 232.5, 368.2, 335.1, 209.9, 193.2, 187, 357.8, 290.4, 282.2, 293.6, 468.1, 381.3, 393.1, 480.6, 400.7), Armed.Forces = c(159, 145.6, 161.6, 165, 309.9, 359.4, 354.7, 335, 304.8, 285.7, 279.8, 263.7, 255.2, 251.4, 257.2, 282.7), Population = c(107.608, 108.632, 109.773, 110.929, 112.075, 113.27, 115.094, 116.219, 117.388, 118.734, 120.445, 121.95, 123.366, 125.368, 127.852, 130.081), Year = 1947:1962), .Names = c('Employed', 'GNP.deflator', 'GNP', 'Unemployed', 'Armed.Forces', 'Population', 'Year'), class = 'data.frame', row.names = 1947:1962, terms = quote(Employed ~     GNP.deflator + GNP + Unemployed + Armed.Forces + Population + Year)), 3L);.subset2(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testsubset217() {
        assertEval("argv <- list(structure(list(y = c(1.08728092481538, 0.0420572471552261, 0.787502161306819, 0.512717751544676, 3.35376639535311, 0.204341510750309, -0.334930602487435, 0.80049208412789, -0.416177803375218, -0.777970346246018, 0.934996808181635, -0.678786709127108, 1.52621589791412, 0.5895781228122, -0.744496121210548, -1.99065153885627, 1.51286447692396, -0.750182409847851), A = c(0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1), B = c(1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0), U = structure(c(1L, 1L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 2L, 2L, 3L, 3L, 3L, 3L, 3L, 3L), .Label = c('a', 'b', 'c'), class = 'factor'), V = structure(c(1L, 1L, 2L, 2L, 3L, 3L, 1L, 1L, 2L, 2L, 3L, 3L, 1L, 1L, 2L, 2L, 3L, 3L), .Label = c('a', 'b', 'c'), class = 'factor')), .Names = c('y', 'A', 'B', 'U', 'V'), class = 'data.frame', row.names = c(NA, 18L), terms = quote(y ~ (A + B):(U + V) - 1)), 4L);.subset2(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testsubset218() {
        assertEval("argv <- list(structure(list(y = c(-0.0561287395290008, -0.155795506705329, -1.47075238389927, -0.47815005510862, 0.417941560199702, 1.35867955152904, -0.102787727342996, 0.387671611559369, -0.0538050405829051, -1.37705955682861), x = c(TRUE, FALSE, TRUE, FALSE, TRUE, FALSE, TRUE, FALSE, TRUE, FALSE)), .Names = c('y', 'x'), class = 'data.frame', row.names = c(NA, 10L), terms = quote(y ~ x)), 1L);.subset2(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testsubset219() {
        assertEval("argv <- list(structure(list(surname = structure(2L, .Label = c('McNeil', 'R Core', 'Ripley', 'Tierney', 'Tukey', 'Venables'), class = 'factor'), nationality = structure(NA_integer_, .Label = c('Australia', 'UK', 'US'), class = 'factor'), deceased = structure(NA_integer_, .Label = c('no', 'yes'), class = 'factor'), title = structure(1L, .Label = c('An Introduction to R', 'Exploratory Data Analysis', 'Interactive Data Analysis', 'LISP-STAT', 'Modern Applied Statistics ...', 'Spatial Statistics', 'Stochastic Simulation'), class = 'factor'), other.author = structure(2L, .Label = c('Ripley', 'Venables & Smith'), class = 'factor')), .Names = c('surname', 'nationality', 'deceased', 'title', 'other.author'), row.names = 1L, class = 'data.frame'), 4L);.subset2(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testsubset220() {
        assertEval("argv <- list(structure(list(surname = structure(integer(0), .Label = c('McNeil', 'Ripley', 'Tierney', 'Tukey', 'Venables'), class = 'factor'), nationality = structure(integer(0), .Label = c('Australia', 'UK', 'US'), class = 'factor'), deceased = structure(integer(0), .Label = c('no', 'yes'), class = 'factor'), title = structure(integer(0), .Label = c('An Introduction to R', 'Exploratory Data Analysis', 'Interactive Data Analysis', 'LISP-STAT', 'Modern Applied Statistics ...', 'Spatial Statistics', 'Stochastic Simulation'), class = 'factor'), other.author = structure(integer(0), .Label = c('Ripley', 'Venables & Smith'), class = 'factor')), .Names = c('surname', 'nationality', 'deceased', 'title', 'other.author'), row.names = integer(0), class = 'data.frame'), 4L);.subset2(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testsubset221() {
        assertEval("argv <- list(structure(list(A = c(1, NA, 1), B = c(1.1, NA, 2), C = c(1.1+0i, NA, 3+0i), D = c(NA_integer_, NA_integer_, NA_integer_), E = c(FALSE, NA, TRUE), F = c('abc', NA, 'def')), .Names = c('A', 'B', 'C', 'D', 'E', 'F'), class = 'data.frame', row.names = c('1', '2', '3')), 3L);.subset2(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testsubset222() {
        assertEval("argv <- list(structure(list(A = c(1L, NA, 1L), B = c(1.1, NA, 2), C = c(1.1+0i, NA, 3+0i), D = c(NA, NA, NA), E = c(FALSE, NA, TRUE), F = structure(c(1L, NA, 2L), .Label = c('abc', 'def'), class = 'factor')), .Names = c('A', 'B', 'C', 'D', 'E', 'F'), class = 'data.frame', row.names = c('1', '2', '3')), 5L);.subset2(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testsubset223() {
        assertEval("argv <- list(structure(list(srcfile = '/home/lzhao/tmp/RtmpS45wYI/R.INSTALL2aa62411bcd3/rpart/R/rpart.R', frow = 187L, lrow = 187L), .Names = c('srcfile', 'frow', 'lrow'), row.names = c(NA, -1L), class = 'data.frame'), 2L);.subset2(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testsubset224() {
        assertEval("argv <- list(structure(list(y = c(78.5, 74.3, 104.3, 87.6, 95.9, 109.2, 102.7, 72.5, 93.1, 115.9, 83.8, 113.3, 109.4), x1 = c(7, 1, 11, 11, 7, 11, 3, 1, 2, 21, 1, 11, 10), x2 = c(26, 29, 56, 31, 52, 55, 71, 31, 54, 47, 40, 66, 68), x4 = c(60, 52, 20, 47, 33, 22, 6, 44, 22, 26, 34, 12, 12)), .Names = c('y', 'x1', 'x2', 'x4'), class = 'data.frame', row.names = c(NA, 13L), terms = quote(y ~ x1 + x2 + x4)), 3L);.subset2(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testsubset225() {
        assertEval("argv <- list(structure(list(`cbind(X, M)` = structure(c(68, 42, 37, 24, 66, 33, 47, 23, 63, 29, 57, 19, 42, 30, 52, 43, 50, 23, 55, 47, 53, 27, 49, 29), .Dim = c(12L, 2L), .Dimnames = list(NULL, c('X', 'M'))), M.user = structure(c(1L, 1L, 2L, 2L, 1L, 1L, 2L, 2L, 1L, 1L, 2L, 2L), .Label = c('N', 'Y'), class = 'factor'), Temp = structure(c(2L, 1L, 2L, 1L, 2L, 1L, 2L, 1L, 2L, 1L, 2L, 1L), .Label = c('High', 'Low'), class = 'factor'), Soft = structure(c(1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 3L, 3L, 3L, 3L), .Label = c('Hard', 'Medium', 'Soft'), class = 'factor')), .Names = c('cbind(X, M)', 'M.user', 'Temp', 'Soft'), terms = quote(cbind(X, M) ~ M.user + Temp + Soft + M.user:Temp), row.names = c('1', '3', '5', '7', '9', '11', '13', '15', '17', '19', '21', '23'), class = 'data.frame'), 'cbind(X, M)');.subset2(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testsubset226() {
        assertEval("argv <- list(structure(list(Df = 10L, `Sum Sq` = 2.74035772634541, `Mean Sq` = 0.274035772634541, `F value` = NA_real_, `Pr(>F)` = NA_real_), .Names = c('Df', 'Sum Sq', 'Mean Sq', 'F value', 'Pr(>F)'), row.names = 'Residuals', class = c('anova', 'data.frame'), heading = c('Analysis of Variance Table\\n', 'Response: y')), 5L);.subset2(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testsubset227() {
        assertEval("argv <- list(structure(list(surname = structure(1:5, .Label = c('McNeil', 'Ripley', 'Tierney', 'Tukey', 'Venables'), class = 'factor'), nationality = structure(c(1L, 2L, 3L, 3L, 1L), .Label = c('Australia', 'UK', 'US'), class = 'factor'), deceased = structure(c(1L, 1L, 1L, 2L, 1L), .Label = c('no', 'yes'), class = 'factor'), title = structure(c(NA_integer_, NA_integer_, NA_integer_, NA_integer_, NA_integer_), .Label = c('An Introduction to R', 'Exploratory Data Analysis', 'Interactive Data Analysis', 'LISP-STAT', 'Modern Applied Statistics ...', 'Spatial Statistics', 'Stochastic Simulation'), class = 'factor'), other.author = structure(c(NA_integer_, NA_integer_, NA_integer_, NA_integer_, NA_integer_), .Label = c('Ripley', 'Venables & Smith'), class = 'factor')), .Names = c('surname', 'nationality', 'deceased', 'title', 'other.author'), row.names = c(NA, -5L), class = 'data.frame'), 4L);.subset2(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testsubset228() {
        assertEval("argv <- list(structure(list(Df = c(NA, 1, 1, 1, 1), `Sum of Sq` = c(NA, 25.9509113775335, 2.97247824113524, 0.109090049888117, 0.246974722154086), RSS = c(47.863639350499, 73.8145507280325, 50.8361175916342, 47.9727294003871, 48.1106140726531), AIC = c(26.9442879283302, 30.5758847476115, 25.7275503692601, 24.9738836085411, 25.0111950072736)), .Names = c('Df', 'Sum of Sq', 'RSS', 'AIC'), row.names = c('<none>', '- x1', '- x2', '- x3', '- x4'), class = c('anova', 'data.frame')), 2L);.subset2(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testsubset229() {
        assertEval("argv <- list(structure(list(mtime = structure(1342423171, class = c('POSIXct', 'POSIXt'))), .Names = 'mtime'), 1L);.subset2(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testsubset230() {
        assertEval("argv <- list(structure(list(df0 = structure(list(structure(integer(0), .Label = character(0), class = 'factor')), row.names = character(0), class = 'data.frame')), .Names = 'df0', row.names = 'c0', class = 'data.frame'), 1L);.subset2(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testsubset231() {
        assertEval("argv <- list(structure(list(y = structure(c(8.79236, 8.79137, 8.81486, 8.81301, 8.90751, 8.93673, 8.96161, 8.96044, 9.00868, 9.03049, 9.06906, 9.05871, 9.10698, 9.12685, 9.17096, 9.18665, 9.23823, 9.26487, 9.28436, 9.31378, 9.35025, 9.35835, 9.39767, 9.4215, 9.44223, 9.48721, 9.52374, 9.5398, 9.58123, 9.60048, 9.64496, 9.6439, 9.69405, 9.69958, 9.68683, 9.71774, 9.74924, 9.77536, 9.79424), .Tsp = c(1962.25, 1971.75, 4), class = 'ts'), lag.quarterly.revenue = c(8.79636, 8.79236, 8.79137, 8.81486, 8.81301, 8.90751, 8.93673, 8.96161, 8.96044, 9.00868, 9.03049, 9.06906, 9.05871, 9.10698, 9.12685, 9.17096, 9.18665, 9.23823, 9.26487, 9.28436, 9.31378, 9.35025, 9.35835, 9.39767, 9.4215, 9.44223, 9.48721, 9.52374, 9.5398, 9.58123, 9.60048, 9.64496, 9.6439, 9.69405, 9.69958, 9.68683, 9.71774, 9.74924, 9.77536), price.index = c(4.70997, 4.70217, 4.68944, 4.68558, 4.64019, 4.62553, 4.61991, 4.61654, 4.61407, 4.60766, 4.60227, 4.5896, 4.57592, 4.58661, 4.57997, 4.57176, 4.56104, 4.54906, 4.53957, 4.51018, 4.50352, 4.4936, 4.46505, 4.44924, 4.43966, 4.42025, 4.4106, 4.41151, 4.3981, 4.38513, 4.3732, 4.3277, 4.32023, 4.30909, 4.30909, 4.30552, 4.29627, 4.27839, 4.27789), income.level = c(5.8211, 5.82558, 5.83112, 5.84046, 5.85036, 5.86464, 5.87769, 5.89763, 5.92574, 5.94232, 5.95365, 5.9612, 5.97805, 6.00377, 6.02829, 6.03475, 6.03906, 6.05046, 6.05563, 6.06093, 6.07103, 6.08018, 6.08858, 6.10199, 6.11207, 6.11596, 6.12129, 6.122, 6.13119, 6.14705, 6.15336, 6.15627, 6.16274, 6.17369, 6.16135, 6.18231, 6.18768, 6.19377, 6.2003), market.potential = c(12.9699, 12.9733, 12.9774, 12.9806, 12.9831, 12.9854, 12.99, 12.9943, 12.9992, 13.0033, 13.0099, 13.0159, 13.0212, 13.0265, 13.0351, 13.0429, 13.0497, 13.0551, 13.0634, 13.0693, 13.0737, 13.077, 13.0849, 13.0918, 13.095, 13.0984, 13.1089, 13.1169, 13.1222, 13.1266, 13.1356, 13.1415, 13.1444, 13.1459, 13.152, 13.1593, 13.1579, 13.1625, 13.1664)), .Names = c('y', 'lag.quarterly.revenue', 'price.index', 'income.level', 'market.potential'), row.names = c('1962.25', '1962.5', '1962.75', '1963', '1963.25', '1963.5', '1963.75', '1964', '1964.25', '1964.5', '1964.75', '1965', '1965.25', '1965.5', '1965.75', '1966', '1966.25', '1966.5', '1966.75', '1967', '1967.25', '1967.5', '1967.75', '1968', '1968.25', '1968.5', '1968.75', '1969', '1969.25', '1969.5', '1969.75', '1970', '1970.25', '1970.5', '1970.75', '1971', '1971.25', '1971.5', '1971.75'), class = 'data.frame'), 4L);.subset2(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testsubset233() {
        assertEval("argv <- list(structure(list(A = c(1L, NA, 1L), B = c(1.1, NA, 2), C = c(1.1+0i, NA, 3+0i), D = c(NA, NA, NA), E = c(FALSE, NA, TRUE), F = structure(c(1L, NA, 2L), .Label = c('abc', 'def'), class = 'factor')), .Names = c('A', 'B', 'C', 'D', 'E', 'F'), class = 'data.frame', row.names = c('1', '2', '3')), 6L);.subset2(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testsubset234() {
        assertEval("argv <- list(structure(list(variog = c(0.00723952158228125, 0.014584633605134, 0.0142079356273193, 0.0184422668389517, 0.0111285046171491, 0.0199100817701382, 0.0270723108677323, 0.0341403794476899, 0.0283206569034573, 0.03752550654923), dist = c(1, 6, 7, 8, 13, 14, 15, 20, 21, 22), n.pairs = structure(c(16L, 16L, 144L, 16L, 16L, 128L, 16L, 16L, 112L, 16L), .Dim = 10L, .Dimnames = structure(list(c('1', '6', '7', '8', '13', '14', '15', '20', '21', '22')), .Names = ''))), .Names = c('variog', 'dist', 'n.pairs'), collapse = TRUE, row.names = c(NA, 10L), class = c('Variogram', 'data.frame')), 3L);.subset2(argv[[1]],argv[[2]]);");
    }
}
