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
public class TestBuiltin_subset extends TestBase {

    @Test
    public void testsubset1() {
        assertEval("argv <- list(structure(list(`Resid. Df` = c(4, 0), `Resid. Dev` = c(5.12914107700115, 7.54951656745095e-15), Df = c(NA, 4), Deviance = c(NA, 5.12914107700114), Rao = c(NA, 5.17320176026795)), .Names = c('Resid. Df', 'Resid. Dev', 'Df', 'Deviance', 'Rao'), row.names = c('1', '2'), class = 'data.frame'), 5L);.subset(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testsubset2() {
        assertEval("argv <- list(structure(list(y = structure(c(8.79236, 8.79137, 8.81486, 8.81301, 8.90751, 8.93673, 8.96161, 8.96044, 9.00868, 9.03049, 9.06906, 9.05871, 9.10698, 9.12685, 9.17096, 9.18665, 9.23823, 9.26487, 9.28436, 9.31378, 9.35025, 9.35835, 9.39767, 9.4215, 9.44223, 9.48721, 9.52374, 9.5398, 9.58123, 9.60048, 9.64496, 9.6439, 9.69405, 9.69958, 9.68683, 9.71774, 9.74924, 9.77536, 9.79424), .Tsp = c(1962.25, 1971.75, 4), class = 'ts'), lag.quarterly.revenue = c(8.79636, 8.79236, 8.79137, 8.81486, 8.81301, 8.90751, 8.93673, 8.96161, 8.96044, 9.00868, 9.03049, 9.06906, 9.05871, 9.10698, 9.12685, 9.17096, 9.18665, 9.23823, 9.26487, 9.28436, 9.31378, 9.35025, 9.35835, 9.39767, 9.4215, 9.44223, 9.48721, 9.52374, 9.5398, 9.58123, 9.60048, 9.64496, 9.6439, 9.69405, 9.69958, 9.68683, 9.71774, 9.74924, 9.77536), price.index = c(4.70997, 4.70217, 4.68944, 4.68558, 4.64019, 4.62553, 4.61991, 4.61654, 4.61407, 4.60766, 4.60227, 4.5896, 4.57592, 4.58661, 4.57997, 4.57176, 4.56104, 4.54906, 4.53957, 4.51018, 4.50352, 4.4936, 4.46505, 4.44924, 4.43966, 4.42025, 4.4106, 4.41151, 4.3981, 4.38513, 4.3732, 4.3277, 4.32023, 4.30909, 4.30909, 4.30552, 4.29627, 4.27839, 4.27789), income.level = c(5.8211, 5.82558, 5.83112, 5.84046, 5.85036, 5.86464, 5.87769, 5.89763, 5.92574, 5.94232, 5.95365, 5.9612, 5.97805, 6.00377, 6.02829, 6.03475, 6.03906, 6.05046, 6.05563, 6.06093, 6.07103, 6.08018, 6.08858, 6.10199, 6.11207, 6.11596, 6.12129, 6.122, 6.13119, 6.14705, 6.15336, 6.15627, 6.16274, 6.17369, 6.16135, 6.18231, 6.18768, 6.19377, 6.2003), market.potential = c(12.9699, 12.9733, 12.9774, 12.9806, 12.9831, 12.9854, 12.99, 12.9943, 12.9992, 13.0033, 13.0099, 13.0159, 13.0212, 13.0265, 13.0351, 13.0429, 13.0497, 13.0551, 13.0634, 13.0693, 13.0737, 13.077, 13.0849, 13.0918, 13.095, 13.0984, 13.1089, 13.1169, 13.1222, 13.1266, 13.1356, 13.1415, 13.1444, 13.1459, 13.152, 13.1593, 13.1579, 13.1625, 13.1664)), .Names = c('y', 'lag.quarterly.revenue', 'price.index', 'income.level', 'market.potential'), row.names = c('1962.25', '1962.5', '1962.75', '1963', '1963.25', '1963.5', '1963.75', '1964', '1964.25', '1964.5', '1964.75', '1965', '1965.25', '1965.5', '1965.75', '1966', '1966.25', '1966.5', '1966.75', '1967', '1967.25', '1967.5', '1967.75', '1968', '1968.25', '1968.5', '1968.75', '1969', '1969.25', '1969.5', '1969.75', '1970', '1970.25', '1970.5', '1970.75', '1971', '1971.25', '1971.5', '1971.75'), class = 'data.frame'), 3L);.subset(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testsubset3() {
        assertEval("argv <- list(structure(list(Var1 = c(1L, 2L, 3L, 0L, 1L, 2L, 0L, 1L, 0L), Var2 = c(0L, 0L, 0L, 1L, 1L, 1L, 2L, 2L, 3L)), .Names = c('Var1', 'Var2'), out.attrs = structure(list(dim = c(4L, 4L), dimnames = structure(list(Var1 = c('Var1=0', 'Var1=1', 'Var1=2', 'Var1=3'), Var2 = c('Var2=0', 'Var2=1', 'Var2=2', 'Var2=3')), .Names = c('Var1', 'Var2'))), .Names = c('dim', 'dimnames')), row.names = c(2L, 3L, 4L, 5L, 6L, 7L, 9L, 10L, 13L), class = 'data.frame'), 1);.subset(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testsubset4() {
        assertEval("argv <- list(structure(list(Population = c(3615, 365, 2212, 2110, 21198, 2541, 3100, 579, 8277, 4931, 868, 813, 11197, 5313, 2861, 2280, 3387, 3806, 1058, 4122, 5814, 9111, 3921, 2341, 4767, 746, 1544, 590, 812, 7333, 1144, 18076, 5441, 637, 10735, 2715, 2284, 11860, 931, 2816, 681, 4173, 12237, 1203, 472, 4981, 3559, 1799, 4589, 376), Income = c(3624, 6315, 4530, 3378, 5114, 4884, 5348, 4809, 4815, 4091, 4963, 4119, 5107, 4458, 4628, 4669, 3712, 3545, 3694, 5299, 4755, 4751, 4675, 3098, 4254, 4347, 4508, 5149, 4281, 5237, 3601, 4903, 3875, 5087, 4561, 3983, 4660, 4449, 4558, 3635, 4167, 3821, 4188, 4022, 3907, 4701, 4864, 3617, 4468, 4566), Illiteracy = c(2.1, 1.5, 1.8, 1.9, 1.1, 0.7, 1.1, 0.9, 1.3, 2, 1.9, 0.6, 0.9, 0.7, 0.5, 0.6, 1.6, 2.8, 0.7, 0.9, 1.1, 0.9, 0.6, 2.4, 0.8, 0.6, 0.6, 0.5, 0.7, 1.1, 2.2, 1.4, 1.8, 0.8, 0.8, 1.1, 0.6, 1, 1.3, 2.3, 0.5, 1.7, 2.2, 0.6, 0.6, 1.4, 0.6, 1.4, 0.7, 0.6), `Life Exp` = c(69.05, 69.31, 70.55, 70.66, 71.71, 72.06, 72.48, 70.06, 70.66, 68.54, 73.6, 71.87, 70.14, 70.88, 72.56, 72.58, 70.1, 68.76, 70.39, 70.22, 71.83, 70.63, 72.96, 68.09, 70.69, 70.56, 72.6, 69.03, 71.23, 70.93, 70.32, 70.55, 69.21, 72.78, 70.82, 71.42, 72.13, 70.43, 71.9, 67.96, 72.08, 70.11, 70.9, 72.9, 71.64, 70.08, 71.72, 69.48, 72.48, 70.29), Murder = c(15.1, 11.3, 7.8, 10.1, 10.3, 6.8, 3.1, 6.2, 10.7, 13.9, 6.2, 5.3, 10.3, 7.1, 2.3, 4.5, 10.6, 13.2, 2.7, 8.5, 3.3, 11.1, 2.3, 12.5, 9.3, 5, 2.9, 11.5, 3.3, 5.2, 9.7, 10.9, 11.1, 1.4, 7.4, 6.4, 4.2, 6.1, 2.4, 11.6, 1.7, 11, 12.2, 4.5, 5.5, 9.5, 4.3, 6.7, 3, 6.9), `HS Grad` = c(41.3, 66.7, 58.1, 39.9, 62.6, 63.9, 56, 54.6, 52.6, 40.6, 61.9, 59.5, 52.6, 52.9, 59, 59.9, 38.5, 42.2, 54.7, 52.3, 58.5, 52.8, 57.6, 41, 48.8, 59.2, 59.3, 65.2, 57.6, 52.5, 55.2, 52.7, 38.5, 50.3, 53.2, 51.6, 60, 50.2, 46.4, 37.8, 53.3, 41.8, 47.4, 67.3, 57.1, 47.8, 63.5, 41.6, 54.5, 62.9), Frost = c(20, 152, 15, 65, 20, 166, 139, 103, 11, 60, 0, 126, 127, 122, 140, 114, 95, 12, 161, 101, 103, 125, 160, 50, 108, 155, 139, 188, 174, 115, 120, 82, 80, 186, 124, 82, 44, 126, 127, 65, 172, 70, 35, 137, 168, 85, 32, 100, 149, 173), Area = c(50708, 566432, 113417, 51945, 156361, 103766, 4862, 1982, 54090, 58073, 6425, 82677, 55748, 36097, 55941, 81787, 39650, 44930, 30920, 9891, 7826, 56817, 79289, 47296, 68995, 145587, 76483, 109889, 9027, 7521, 121412, 47831, 48798, 69273, 40975, 68782, 96184, 44966, 1049, 30225, 75955, 41328, 262134, 82096, 9267, 39780, 66570, 24070, 54464, 97203)), .Names = c('Population', 'Income', 'Illiteracy', 'Life Exp', 'Murder', 'HS Grad', 'Frost', 'Area'), row.names = c('Alabama', 'Alaska', 'Arizona', 'Arkansas', 'California', 'Colorado', 'Connecticut', 'Delaware', 'Florida', 'Georgia', 'Hawaii', 'Idaho', 'Illinois', 'Indiana', 'Iowa', 'Kansas', 'Kentucky', 'Louisiana', 'Maine', 'Maryland', 'Massachusetts', 'Michigan', 'Minnesota', 'Mississippi', 'Missouri', 'Montana', 'Nebraska', 'Nevada', 'New Hampshire', 'New Jersey', 'New Mexico', 'New York', 'North Carolina', 'North Dakota', 'Ohio', 'Oklahoma', 'Oregon', 'Pennsylvania', 'Rhode Island', 'South Carolina', 'South Dakota', 'Tennessee', 'Texas', 'Utah', 'Vermont', 'Virginia', 'Washington', 'West Virginia', 'Wisconsin', 'Wyoming'), class = 'data.frame'), c(FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE));.subset(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testsubset5() {
        assertEval("argv <- list(structure(list(war = c(1, 1, 0, 1, 0, 0, 0, 0, 0, 1, 1, 1, 0, 1, 1, 0, 0, 1, 1, 0), fly = c(1, 0, 1, 1, 1, 1, 0, 0, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1), ver = c(1, 1, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0), end = c(1, 1, 1, 1, 0, 1, 1, 0, 0, 1, 0, 1, NA, 1, 1, 0, 1, 1, NA, 0), gro = c(0, 0, 1, 1, 0, 0, 0, 1, 0, 1, NA, 0, 0, 1, NA, 0, 0, NA, 1, 0), hai = c(1, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 0, 1, 1, 0, 0, 1, 0, 1)), .Names = c('war', 'fly', 'ver', 'end', 'gro', 'hai'), row.names = c('ant', 'bee', 'cat', 'cpl', 'chi', 'cow', 'duc', 'eag', 'ele', 'fly', 'fro', 'her', 'lio', 'liz', 'lob', 'man', 'rab', 'sal', 'spi', 'wha'), class = 'data.frame'), NULL);.subset(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testsubset6() {
        assertEval("argv <- list(structure(list(surname = structure(c('McNeil', 'Ripley', 'Ripley', 'Tierney', 'Tukey', 'Venables'), class = 'AsIs'), nationality = structure(c(1L, 2L, 2L, 3L, 3L, 1L), .Label = c('Australia', 'UK', 'US'), class = 'factor'), deceased = structure(c(1L, 1L, 1L, 1L, 2L, 1L), .Label = c('no', 'yes'), class = 'factor'), title = structure(c(3L, 6L, 7L, 4L, 2L, 5L), .Label = c('An Introduction to R', 'Exploratory Data Analysis', 'Interactive Data Analysis', 'LISP-STAT', 'Modern Applied Statistics ...', 'Spatial Statistics', 'Stochastic Simulation'), class = 'factor'), other.author = structure(c(NA, NA, NA, NA, NA, 1L), .Label = c('Ripley', 'Venables & Smith'), class = 'factor')), .Names = c('surname', 'nationality', 'deceased', 'title', 'other.author'), row.names = c(NA, -6L), class = 'data.frame'), -1);.subset(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testsubset7() {
        assertEval("argv <- list(structure(list(a = c(1L, 2L, 3L, NA), b = c(NA, 3.14159265358979, 3.14159265358979, 3.14159265358979), c = c(TRUE, NA, FALSE, TRUE), d = c('aa', 'bb', NA, 'dd'), e = structure(c('a1', NA, NA, 'a4'), class = 'AsIs'), f = c('20010101', NA, NA, '20041026')), .Names = c('a', 'b', 'c', 'd', 'e', 'f'), row.names = c(NA, -4L), class = 'data.frame'), 3L);.subset(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testsubset8() {
        assertEval("argv <- list(structure(list(Df = c(NA, 1, 1, 2), Deviance = c(32.825622681839, 12.2441566485997, 28.4640218366572, 32.4303239692005), AIC = c(92.5235803967766, 73.9421143635373, 90.1619795515948, 96.1282816841381)), .Names = c('Df', 'Deviance', 'AIC'), row.names = c('<none>', '+ M.user', '+ Temp', '+ Soft'), class = c('anova', 'data.frame')), 3L);.subset(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testsubset9() {
        assertEval("argv <- list(structure(list(`1` = 0:10, `2` = 10:20, `3` = 20:30), .Names = c('1', '2', '3'), row.names = c(NA, -11L), class = 'data.frame'), -2);.subset(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testsubset10() {
        assertEval("argv <- list(structure(list(surname = structure(2L, .Label = c('McNeil', 'R Core', 'Ripley', 'Tierney', 'Tukey', 'Venables'), class = 'factor'), nationality = structure(NA_integer_, .Label = c('Australia', 'UK', 'US'), class = 'factor'), deceased = structure(NA_integer_, .Label = c('no', 'yes'), class = 'factor')), .Names = c('surname', 'nationality', 'deceased'), row.names = 1L, class = 'data.frame'), 1L);.subset(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testsubset11() {
        assertEval("argv <- list(structure(list(height = numeric(0), weight = numeric(0)), .Names = c('height', 'weight'), row.names = integer(0), class = 'data.frame'), 1:2);.subset(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testsubset12() {
        assertEval("argv <- list(structure(list(srcfile = c('/home/lzhao/tmp/RtmpGUHe0I/R.INSTALL2aa51f3e9d31/lattice/R/cloud.R', '/home/lzhao/tmp/RtmpGUHe0I/R.INSTALL2aa51f3e9d31/lattice/R/cloud.R', '/home/lzhao/tmp/RtmpGUHe0I/R.INSTALL2aa51f3e9d31/lattice/R/cloud.R', '/home/lzhao/tmp/RtmpGUHe0I/R.INSTALL2aa51f3e9d31/lattice/R/cloud.R'), frow = c(32L, 33L, 33L, 36L), lrow = c(32L, 33L, 33L, 36L)), .Names = c('srcfile', 'frow', 'lrow'), row.names = c(NA, 4L), class = 'data.frame'), 'frow');.subset(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testsubset13() {
        assertEval("argv <- list(structure(list(surname = structure(c(5L, 6L, 4L, 3L, 3L, 1L, 2L), .Label = c('McNeil', 'R Core', 'Ripley', 'Tierney', 'Tukey', 'Venables'), class = 'factor'), title = structure(c(2L, 5L, 4L, 6L, 7L, 3L, 1L), .Label = c('An Introduction to R', 'Exploratory Data Analysis', 'Interactive Data Analysis', 'LISP-STAT', 'Modern Applied Statistics ...', 'Spatial Statistics', 'Stochastic Simulation'), class = 'factor'), other.author = structure(c(NA, 1L, NA, NA, NA, NA, 2L), .Label = c('Ripley', 'Venables & Smith'), class = 'factor')), .Names = c('surname', 'title', 'other.author'), row.names = c(NA, -7L), class = 'data.frame'), 1L);.subset(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testsubset14() {
        assertEval("argv <- list(structure(list(size = 1056, isdir = FALSE, mode = structure(420L, class = 'octmode'), mtime = structure(1393948130.23894, class = c('POSIXct', 'POSIXt')), ctime = structure(1393948130.23894, class = c('POSIXct', 'POSIXt')), atime = structure(1395074550.46596, class = c('POSIXct', 'POSIXt')), uid = 1001L, gid = 1001L, uname = 'roman', grname = 'roman'), .Names = c('size', 'isdir', 'mode', 'mtime', 'ctime', 'atime', 'uid', 'gid', 'uname', 'grname'), class = 'data.frame', row.names = '/home/roman/r-instrumented/library/grid/R/grid'), 'mtime');.subset(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testsubset15() {
        assertEval("argv <- list(structure(list(Sepal.Length = c(5.1, 4.9, 4.7, 4.6, 5, 5.4, 4.6, 5, 4.4, 4.9, 5.4, 4.8, 4.8, 4.3, 5.8, 5.7, 5.4, 5.1, 5.7, 5.1, 5.4, 5.1, 4.6, 5.1, 4.8, 5, 5, 5.2, 5.2, 4.7, 4.8, 5.4, 5.2, 5.5, 4.9, 5, 5.5, 4.9, 4.4, 5.1, 5, 4.5, 4.4, 5, 5.1, 4.8, 5.1, 4.6, 5.3, 5, 7, 6.4, 6.9, 5.5, 6.5, 5.7, 6.3, 4.9, 6.6, 5.2, 5, 5.9, 6, 6.1, 5.6, 6.7, 5.6, 5.8, 6.2, 5.6, 5.9, 6.1, 6.3, 6.1, 6.4, 6.6, 6.8, 6.7, 6, 5.7, 5.5, 5.5, 5.8, 6, 5.4, 6, 6.7, 6.3, 5.6, 5.5, 5.5, 6.1, 5.8, 5, 5.6, 5.7, 5.7, 6.2, 5.1, 5.7, 6.3, 5.8, 7.1, 6.3, 6.5, 7.6, 4.9, 7.3, 6.7, 7.2, 6.5, 6.4, 6.8, 5.7, 5.8, 6.4, 6.5, 7.7, 7.7, 6, 6.9, 5.6, 7.7, 6.3, 6.7, 7.2, 6.2, 6.1, 6.4, 7.2, 7.4, 7.9, 6.4, 6.3, 6.1, 7.7, 6.3, 6.4, 6, 6.9, 6.7, 6.9, 5.8, 6.8, 6.7, 6.7, 6.3, 6.5, 6.2, 5.9), Sepal.Width = c(3.5, 3, 3.2, 3.1, 3.6, 3.9, 3.4, 3.4, 2.9, 3.1, 3.7, 3.4, 3, 3, 4, 4.4, 3.9, 3.5, 3.8, 3.8, 3.4, 3.7, 3.6, 3.3, 3.4, 3, 3.4, 3.5, 3.4, 3.2, 3.1, 3.4, 4.1, 4.2, 3.1, 3.2, 3.5, 3.6, 3, 3.4, 3.5, 2.3, 3.2, 3.5, 3.8, 3, 3.8, 3.2, 3.7, 3.3, 3.2, 3.2, 3.1, 2.3, 2.8, 2.8, 3.3, 2.4, 2.9, 2.7, 2, 3, 2.2, 2.9, 2.9, 3.1, 3, 2.7, 2.2, 2.5, 3.2, 2.8, 2.5, 2.8, 2.9, 3, 2.8, 3, 2.9, 2.6, 2.4, 2.4, 2.7, 2.7, 3, 3.4, 3.1, 2.3, 3, 2.5, 2.6, 3, 2.6, 2.3, 2.7, 3, 2.9, 2.9, 2.5, 2.8, 3.3, 2.7, 3, 2.9, 3, 3, 2.5, 2.9, 2.5, 3.6, 3.2, 2.7, 3, 2.5, 2.8, 3.2, 3, 3.8, 2.6, 2.2, 3.2, 2.8, 2.8, 2.7, 3.3, 3.2, 2.8, 3, 2.8, 3, 2.8, 3.8, 2.8, 2.8, 2.6, 3, 3.4, 3.1, 3, 3.1, 3.1, 3.1, 2.7, 3.2, 3.3, 3, 2.5, 3, 3.4, 3), Petal.Length = c(1.4, 1.4, 1.3, 1.5, 1.4, 1.7, 1.4, 1.5, 1.4, 1.5, 1.5, 1.6, 1.4, 1.1, 1.2, 1.5, 1.3, 1.4, 1.7, 1.5, 1.7, 1.5, 1, 1.7, 1.9, 1.6, 1.6, 1.5, 1.4, 1.6, 1.6, 1.5, 1.5, 1.4, 1.5, 1.2, 1.3, 1.4, 1.3, 1.5, 1.3, 1.3, 1.3, 1.6, 1.9, 1.4, 1.6, 1.4, 1.5, 1.4, 4.7, 4.5, 4.9, 4, 4.6, 4.5, 4.7, 3.3, 4.6, 3.9, 3.5, 4.2, 4, 4.7, 3.6, 4.4, 4.5, 4.1, 4.5, 3.9, 4.8, 4, 4.9, 4.7, 4.3, 4.4, 4.8, 5, 4.5, 3.5, 3.8, 3.7, 3.9, 5.1, 4.5, 4.5, 4.7, 4.4, 4.1, 4, 4.4, 4.6, 4, 3.3, 4.2, 4.2, 4.2, 4.3, 3, 4.1, 6, 5.1, 5.9, 5.6, 5.8, 6.6, 4.5, 6.3, 5.8, 6.1, 5.1, 5.3, 5.5, 5, 5.1, 5.3, 5.5, 6.7, 6.9, 5, 5.7, 4.9, 6.7, 4.9, 5.7, 6, 4.8, 4.9, 5.6, 5.8, 6.1, 6.4, 5.6, 5.1, 5.6, 6.1, 5.6, 5.5, 4.8, 5.4, 5.6, 5.1, 5.1, 5.9, 5.7, 5.2, 5, 5.2, 5.4, 5.1), Petal.Width = c(0.2, 0.2, 0.2, 0.2, 0.2, 0.4, 0.3, 0.2, 0.2, 0.1, 0.2, 0.2, 0.1, 0.1, 0.2, 0.4, 0.4, 0.3, 0.3, 0.3, 0.2, 0.4, 0.2, 0.5, 0.2, 0.2, 0.4, 0.2, 0.2, 0.2, 0.2, 0.4, 0.1, 0.2, 0.2, 0.2, 0.2, 0.1, 0.2, 0.2, 0.3, 0.3, 0.2, 0.6, 0.4, 0.3, 0.2, 0.2, 0.2, 0.2, 1.4, 1.5, 1.5, 1.3, 1.5, 1.3, 1.6, 1, 1.3, 1.4, 1, 1.5, 1, 1.4, 1.3, 1.4, 1.5, 1, 1.5, 1.1, 1.8, 1.3, 1.5, 1.2, 1.3, 1.4, 1.4, 1.7, 1.5, 1, 1.1, 1, 1.2, 1.6, 1.5, 1.6, 1.5, 1.3, 1.3, 1.3, 1.2, 1.4, 1.2, 1, 1.3, 1.2, 1.3, 1.3, 1.1, 1.3, 2.5, 1.9, 2.1, 1.8, 2.2, 2.1, 1.7, 1.8, 1.8, 2.5, 2, 1.9, 2.1, 2, 2.4, 2.3, 1.8, 2.2, 2.3, 1.5, 2.3, 2, 2, 1.8, 2.1, 1.8, 1.8, 1.8, 2.1, 1.6, 1.9, 2, 2.2, 1.5, 1.4, 2.3, 2.4, 1.8, 1.8, 2.1, 2.4, 2.3, 1.9, 2.3, 2.5, 2.3, 1.9, 2, 2.3, 1.8), Species = structure(c(1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L), .Label = c('setosa', 'versicolor', 'virginica'), class = 'factor')), .Names = c('Sepal.Length', 'Sepal.Width', 'Petal.Length', 'Petal.Width', 'Species'), row.names = c(NA, -150L), class = 'data.frame'), c(FALSE, FALSE, FALSE, FALSE, TRUE));.subset(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testsubset16() {
        assertEval("argv <- list(structure(list(VAR1 = c(1, 2, 3, 4, 5), VAR2 = c(5, 4, 3, 2, 1), VAR3 = c(1, 1, 1, 1, NA)), .Names = c('VAR1', 'VAR2', 'VAR3'), row.names = c(NA, -5L), class = 'data.frame'), c(1, 3));.subset(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testsubset17() {
        assertEval("argv <- list(structure(list(ii = 1:10, xx = c(-9.42477796076938, -6.28318530717959, -3.14159265358979, 0, 3.14159265358979, 6.28318530717959, 9.42477796076938, 12.5663706143592, 15.707963267949, 18.8495559215388)), .Names = c('ii', 'xx'), row.names = c(NA, -10L), class = 'data.frame'), 'C');.subset(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testsubset18() {
        assertEval("argv <- list(structure(list(srcfile = c(NA, NA, '/home/lzhao/tmp/RtmpGUHe0I/R.INSTALL2aa51f3e9d31/lattice/R/levelplot.R', '/home/lzhao/tmp/RtmpGUHe0I/R.INSTALL2aa51f3e9d31/lattice/R/levelplot.R', '/home/lzhao/tmp/RtmpGUHe0I/R.INSTALL2aa51f3e9d31/lattice/R/levelplot.R', '/home/lzhao/tmp/RtmpGUHe0I/R.INSTALL2aa51f3e9d31/lattice/R/levelplot.R', '/home/lzhao/tmp/RtmpGUHe0I/R.INSTALL2aa51f3e9d31/lattice/R/levelplot.R', '/home/lzhao/tmp/RtmpGUHe0I/R.INSTALL2aa51f3e9d31/lattice/R/levelplot.R', '/home/lzhao/tmp/RtmpGUHe0I/R.INSTALL2aa51f3e9d31/lattice/R/levelplot.R'), frow = c(NA, NA, 427L, 427L, 432L, 434L, 434L, 438L, 438L), lrow = c(NA, NA, 428L, 428L, 432L, 437L, 437L, 441L, 441L)), .Names = c('srcfile', 'frow', 'lrow'), row.names = c(NA, 9L), class = 'data.frame'), 'lrow');.subset(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testsubset19() {
        assertEval("argv <- list(structure(list(x = 1:3, y = structure(4:6, .Dim = c(3L, 1L), class = 'AsIs'), z = structure(c('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i'), .Dim = c(3L, 3L), class = 'AsIs')), .Names = c('x', 'y', 'z'), row.names = c(NA, -3L), class = 'data.frame'), 'z');.subset(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testsubset20() {
        assertEval("argv <- list(structure(list(srcfile = c('/home/lzhao/hg/r-instrumented/library/stats/R/stats', '/home/lzhao/hg/r-instrumented/library/stats/R/stats'), frow = 21911:21912, lrow = 21911:21912), .Names = c('srcfile', 'frow', 'lrow'), row.names = 1:2, class = 'data.frame'), 'frow');.subset(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testsubset21() {
        assertEval("argv <- list(structure(list(z = c(-2.97525101631257, -2.48489962337717, -1.99157511113426, -1.4958325959814, -0.998253002608536, -0.499437269286478, 0, 0.499437269286499, 0.998253002608553, 1.49583259598141, 1.99157511113428, 2.48489962337718, 2.97525101631258), par.vals = structure(c(3.29998649934118, 3.26341935258893, 3.22450701705249, 3.18319718928165, 3.13944811066026, 3.09322935890527, 3.04452243772342, 2.99332114068265, 2.93963167421501, 2.88347253461377, 2.824874144162, 2.76387826147581, 2.70053719000543, -0.454255272277595, -0.454255272277596, -0.454255272277596, -0.454255272277598, -0.454255272277597, -0.454255272277596, -0.454255272277594, -0.454255272277597, -0.454255272277596, -0.454255272277596, -0.454255272277597, -0.454255272277596, -0.454255272277597, -0.292987124681473, -0.292987124681473, -0.292987124681474, -0.292987124681475, -0.292987124681474, -0.292987124681474, -0.292987124681473, -0.292987124681475, -0.292987124681474, -0.292987124681474, -0.292987124681474, -0.292987124681474, -0.292987124681474, -0.255464061617756, -0.218896914865511, -0.179984579329071, -0.138674751558221, -0.0949256729368359, -0.0487069211818519, 1.33790930192987e-15, 0.0512012970407721, 0.104890763508413, 0.161049903109653, 0.219648293561426, 0.280644176247611, 0.34398524771799, -0.599449309335745, -0.49954109111312, -0.399632872890496, -0.299724654667872, -0.199816436445247, -0.099908218222623, 1.42108546079721e-15, 0.0999082182226258, 0.19981643644525, 0.299724654667875, 0.399632872890499, 0.499541091113123, 0.599449309335748), .Dim = c(13L, 5L), .Dimnames = list(NULL, c('(Intercept)', 'outcome2', 'outcome3', 'treatment2', 'treatment3')))), .Names = c('z', 'par.vals'), row.names = c(NA, 13L), class = 'data.frame'), 'par.vals');.subset(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testsubset22() {
        assertEval("argv <- list(structure(3.14159265358979, class = 'testit'), structure(3.14159265358979, class = 'testit'));.subset(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testsubset23() {
        assertEval("argv <- list(structure(list(Fertility = c(80.2, 83.1, 92.5, 85.8, 76.9), Agriculture = c(17, 45.1, 39.7, 36.5, 43.5), Examination = c(15L, 6L, 5L, 12L, 17L), Education = c(12L, 9L, 5L, 7L, 15L)), .Names = c('Fertility', 'Agriculture', 'Examination', 'Education'), row.names = c('Courtelary', 'Delemont', 'Franches-Mnt', 'Moutier', 'Neuveville'), class = 'data.frame'), 'Ferti');.subset(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testsubset24() {
        assertEval("argv <- list(structure(list(size = 131, isdir = FALSE, mode = structure(436L, class = 'octmode'), mtime = structure(1386583148.91412, class = c('POSIXct', 'POSIXt')), ctime = structure(1386583148.91712, class = c('POSIXct', 'POSIXt')), atime = structure(1386583149.16512, class = c('POSIXct', 'POSIXt')), uid = 501L, gid = 501L, uname = 'lzhao', grname = 'lzhao'), .Names = c('size', 'isdir', 'mode', 'mtime', 'ctime', 'atime', 'uid', 'gid', 'uname', 'grname'), class = 'data.frame', row.names = 'startup.Rs'), 'mtime');.subset(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testsubset26() {
        assertEval("argv <- structure(list(x = structure(list(Ozone = c(41L, 36L,     12L, 18L, NA, 28L, 23L, 19L, 8L, NA, 7L, 16L, 11L, 14L, 18L,     14L, 34L, 6L, 30L, 11L, 1L, 11L, 4L, 32L, NA, NA, NA, 23L,     45L, 115L, 37L, NA, NA, NA, NA, NA, NA, 29L, NA, 71L, 39L,     NA, NA, 23L, NA, NA, 21L, 37L, 20L, 12L, 13L, NA, NA, NA,     NA, NA, NA, NA, NA, NA, NA, 135L, 49L, 32L, NA, 64L, 40L,     77L, 97L, 97L, 85L, NA, 10L, 27L, NA, 7L, 48L, 35L, 61L,     79L, 63L, 16L, NA, NA, 80L, 108L, 20L, 52L, 82L, 50L, 64L,     59L, 39L, 9L, 16L, 78L, 35L, 66L, 122L, 89L, 110L, NA, NA,     44L, 28L, 65L, NA, 22L, 59L, 23L, 31L, 44L, 21L, 9L, NA,     45L, 168L, 73L, NA, 76L, 118L, 84L, 85L, 96L, 78L, 73L, 91L,     47L, 32L, 20L, 23L, 21L, 24L, 44L, 21L, 28L, 9L, 13L, 46L,     18L, 13L, 24L, 16L, 13L, 23L, 36L, 7L, 14L, 30L, NA, 14L,     18L, 20L), Solar.R = c(190L, 118L, 149L, 313L, NA, NA, 299L,     99L, 19L, 194L, NA, 256L, 290L, 274L, 65L, 334L, 307L, 78L,     322L, 44L, 8L, 320L, 25L, 92L, 66L, 266L, NA, 13L, 252L,     223L, 279L, 286L, 287L, 242L, 186L, 220L, 264L, 127L, 273L,     291L, 323L, 259L, 250L, 148L, 332L, 322L, 191L, 284L, 37L,     120L, 137L, 150L, 59L, 91L, 250L, 135L, 127L, 47L, 98L, 31L,     138L, 269L, 248L, 236L, 101L, 175L, 314L, 276L, 267L, 272L,     175L, 139L, 264L, 175L, 291L, 48L, 260L, 274L, 285L, 187L,     220L, 7L, 258L, 295L, 294L, 223L, 81L, 82L, 213L, 275L, 253L,     254L, 83L, 24L, 77L, NA, NA, NA, 255L, 229L, 207L, 222L,     137L, 192L, 273L, 157L, 64L, 71L, 51L, 115L, 244L, 190L,     259L, 36L, 255L, 212L, 238L, 215L, 153L, 203L, 225L, 237L,     188L, 167L, 197L, 183L, 189L, 95L, 92L, 252L, 220L, 230L,     259L, 236L, 259L, 238L, 24L, 112L, 237L, 224L, 27L, 238L,     201L, 238L, 14L, 139L, 49L, 20L, 193L, 145L, 191L, 131L,     223L), Wind = c(7.4, 8, 12.6, 11.5, 14.3, 14.9, 8.6, 13.8,     20.1, 8.6, 6.9, 9.7, 9.2, 10.9, 13.2, 11.5, 12, 18.4, 11.5,     9.7, 9.7, 16.6, 9.7, 12, 16.6, 14.9, 8, 12, 14.9, 5.7, 7.4,     8.6, 9.7, 16.1, 9.2, 8.6, 14.3, 9.7, 6.9, 13.8, 11.5, 10.9,     9.2, 8, 13.8, 11.5, 14.9, 20.7, 9.2, 11.5, 10.3, 6.3, 1.7,     4.6, 6.3, 8, 8, 10.3, 11.5, 14.9, 8, 4.1, 9.2, 9.2, 10.9,     4.6, 10.9, 5.1, 6.3, 5.7, 7.4, 8.6, 14.3, 14.9, 14.9, 14.3,     6.9, 10.3, 6.3, 5.1, 11.5, 6.9, 9.7, 11.5, 8.6, 8, 8.6, 12,     7.4, 7.4, 7.4, 9.2, 6.9, 13.8, 7.4, 6.9, 7.4, 4.6, 4, 10.3,     8, 8.6, 11.5, 11.5, 11.5, 9.7, 11.5, 10.3, 6.3, 7.4, 10.9,     10.3, 15.5, 14.3, 12.6, 9.7, 3.4, 8, 5.7, 9.7, 2.3, 6.3,     6.3, 6.9, 5.1, 2.8, 4.6, 7.4, 15.5, 10.9, 10.3, 10.9, 9.7,     14.9, 15.5, 6.3, 10.9, 11.5, 6.9, 13.8, 10.3, 10.3, 8, 12.6,     9.2, 10.3, 10.3, 16.6, 6.9, 13.2, 14.3, 8, 11.5), Temp = c(67L,     72L, 74L, 62L, 56L, 66L, 65L, 59L, 61L, 69L, 74L, 69L, 66L,     68L, 58L, 64L, 66L, 57L, 68L, 62L, 59L, 73L, 61L, 61L, 57L,     58L, 57L, 67L, 81L, 79L, 76L, 78L, 74L, 67L, 84L, 85L, 79L,     82L, 87L, 90L, 87L, 93L, 92L, 82L, 80L, 79L, 77L, 72L, 65L,     73L, 76L, 77L, 76L, 76L, 76L, 75L, 78L, 73L, 80L, 77L, 83L,     84L, 85L, 81L, 84L, 83L, 83L, 88L, 92L, 92L, 89L, 82L, 73L,     81L, 91L, 80L, 81L, 82L, 84L, 87L, 85L, 74L, 81L, 82L, 86L,     85L, 82L, 86L, 88L, 86L, 83L, 81L, 81L, 81L, 82L, 86L, 85L,     87L, 89L, 90L, 90L, 92L, 86L, 86L, 82L, 80L, 79L, 77L, 79L,     76L, 78L, 78L, 77L, 72L, 75L, 79L, 81L, 86L, 88L, 97L, 94L,     96L, 94L, 91L, 92L, 93L, 93L, 87L, 84L, 80L, 78L, 75L, 73L,     81L, 76L, 77L, 71L, 71L, 78L, 67L, 76L, 68L, 82L, 64L, 71L,     81L, 69L, 63L, 70L, 77L, 75L, 76L, 68L), Month = c(5L, 5L,     5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L,     5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 6L,     6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L,     6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 7L,     7L, 7L, 7L, 7L, 7L, 7L, 7L, 7L, 7L, 7L, 7L, 7L, 7L, 7L, 7L,     7L, 7L, 7L, 7L, 7L, 7L, 7L, 7L, 7L, 7L, 7L, 7L, 7L, 7L, 7L,     8L, 8L, 8L, 8L, 8L, 8L, 8L, 8L, 8L, 8L, 8L, 8L, 8L, 8L, 8L,     8L, 8L, 8L, 8L, 8L, 8L, 8L, 8L, 8L, 8L, 8L, 8L, 8L, 8L, 8L,     8L, 9L, 9L, 9L, 9L, 9L, 9L, 9L, 9L, 9L, 9L, 9L, 9L, 9L, 9L,     9L, 9L, 9L, 9L, 9L, 9L, 9L, 9L, 9L, 9L, 9L, 9L, 9L, 9L, 9L,     9L), Day = c(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L,     12L, 13L, 14L, 15L, 16L, 17L, 18L, 19L, 20L, 21L, 22L, 23L,     24L, 25L, 26L, 27L, 28L, 29L, 30L, 31L, 1L, 2L, 3L, 4L, 5L,     6L, 7L, 8L, 9L, 10L, 11L, 12L, 13L, 14L, 15L, 16L, 17L, 18L,     19L, 20L, 21L, 22L, 23L, 24L, 25L, 26L, 27L, 28L, 29L, 30L,     1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L, 12L, 13L, 14L,     15L, 16L, 17L, 18L, 19L, 20L, 21L, 22L, 23L, 24L, 25L, 26L,     27L, 28L, 29L, 30L, 31L, 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L,     9L, 10L, 11L, 12L, 13L, 14L, 15L, 16L, 17L, 18L, 19L, 20L,     21L, 22L, 23L, 24L, 25L, 26L, 27L, 28L, 29L, 30L, 31L, 1L,     2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L, 12L, 13L, 14L,     15L, 16L, 17L, 18L, 19L, 20L, 21L, 22L, 23L, 24L, 25L, 26L,     27L, 28L, 29L, 30L)), .Names = c('Ozone', 'Solar.R', 'Wind',     'Temp', 'Month', 'Day'), class = 'data.frame', row.names = c(NA,     -153L)), c(TRUE, TRUE, TRUE, TRUE, FALSE, FALSE, TRUE, TRUE,     TRUE, FALSE, FALSE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE,     TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, FALSE, FALSE, FALSE,     TRUE, TRUE, TRUE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE,     FALSE, TRUE, FALSE, TRUE, TRUE, FALSE, FALSE, TRUE, FALSE,     FALSE, TRUE, TRUE, TRUE, TRUE, TRUE, FALSE, FALSE, FALSE,     FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, TRUE,     TRUE, FALSE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, FALSE, TRUE,     TRUE, FALSE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, FALSE,     FALSE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE,     TRUE, TRUE, FALSE, FALSE, FALSE, TRUE, TRUE, TRUE, FALSE,     FALSE, TRUE, TRUE, TRUE, FALSE, TRUE, TRUE, TRUE, TRUE, TRUE,     TRUE, TRUE, FALSE, TRUE, TRUE, TRUE, FALSE, TRUE, TRUE, TRUE,     TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE,     TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE,     TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, FALSE, TRUE, TRUE,     TRUE)), .Names = c('x', ''));" +
                        "do.call('subset', argv)");
    }
}
