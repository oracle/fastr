/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2012-2014, Purdue University
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check
public class TestBuiltin_namesassign extends TestBase {

    @Test
    public void testnamesassign1() {
        assertEval("argv <- list(structure(list(happy = c('a', 'b', 'c', 'd'), sad = c('A', 'B', 'C', 'D', 'E', 'F')), .Names = c('happy', 'sad')), value = c('happy', 'sad'));`names<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testnamesassign2() {
        assertEval("argv <- list(structure(list(population = c(3615, 365, 2212, 2110, 21198, 2541, 3100, 579, 8277, 4931, 868, 813, 11197, 5313, 2861, 2280, 3387, 3806, 1058, 4122, 5814, 9111, 3921, 2341, 4767, 746, 1544, 590, 812, 7333, 1144, 18076, 5441, 637, 10735, 2715, 2284, 11860, 931, 2816, 681, 4173, 12237, 1203, 472, 4981, 3559, 1799, 4589, 376), income = c(3624, 6315, 4530, 3378, 5114, 4884, 5348, 4809, 4815, 4091, 4963, 4119, 5107, 4458, 4628, 4669, 3712, 3545, 3694, 5299, 4755, 4751, 4675, 3098, 4254, 4347, 4508, 5149, 4281, 5237, 3601, 4903, 3875, 5087, 4561, 3983, 4660, 4449, 4558, 3635, 4167, 3821, 4188, 4022, 3907, 4701, 4864, 3617, 4468, 4566), illiteracy = c(2.1, 1.5, 1.8, 1.9, 1.1, 0.7, 1.1, 0.9, 1.3, 2, 1.9, 0.6, 0.9, 0.7, 0.5, 0.6, 1.6, 2.8, 0.7, 0.9, 1.1, 0.9, 0.6, 2.4, 0.8, 0.6, 0.6, 0.5, 0.7, 1.1, 2.2, 1.4, 1.8, 0.8, 0.8, 1.1, 0.6, 1, 1.3, 2.3, 0.5, 1.7, 2.2, 0.6, 0.6, 1.4, 0.6, 1.4, 0.7, 0.6), life.exp = c(69.05, 69.31, 70.55, 70.66, 71.71, 72.06, 72.48, 70.06, 70.66, 68.54, 73.6, 71.87, 70.14, 70.88, 72.56, 72.58, 70.1, 68.76, 70.39, 70.22, 71.83, 70.63, 72.96, 68.09, 70.69, 70.56, 72.6, 69.03, 71.23, 70.93, 70.32, 70.55, 69.21, 72.78, 70.82, 71.42, 72.13, 70.43, 71.9, 67.96, 72.08, 70.11, 70.9, 72.9, 71.64, 70.08, 71.72, 69.48, 72.48, 70.29), murder = c(15.1, 11.3, 7.8, 10.1, 10.3, 6.8, 3.1, 6.2, 10.7, 13.9, 6.2, 5.3, 10.3, 7.1, 2.3, 4.5, 10.6, 13.2, 2.7, 8.5, 3.3, 11.1, 2.3, 12.5, 9.3, 5, 2.9, 11.5, 3.3, 5.2, 9.7, 10.9, 11.1, 1.4, 7.4, 6.4, 4.2, 6.1, 2.4, 11.6, 1.7, 11, 12.2, 4.5, 5.5, 9.5, 4.3, 6.7, 3, 6.9), hs.grad = c(41.3, 66.7, 58.1, 39.9, 62.6, 63.9, 56, 54.6, 52.6, 40.6, 61.9, 59.5, 52.6, 52.9, 59, 59.9, 38.5, 42.2, 54.7, 52.3, 58.5, 52.8, 57.6, 41, 48.8, 59.2, 59.3, 65.2, 57.6, 52.5, 55.2, 52.7, 38.5, 50.3, 53.2, 51.6, 60, 50.2, 46.4, 37.8, 53.3, 41.8, 47.4, 67.3, 57.1, 47.8, 63.5, 41.6, 54.5, 62.9), frost = c(20, 152, 15, 65, 20, 166, 139, 103, 11, 60, 0, 126, 127, 122, 140, 114, 95, 12, 161, 101, 103, 125, 160, 50, 108, 155, 139, 188, 174, 115, 120, 82, 80, 186, 124, 82, 44, 126, 127, 65, 172, 70, 35, 137, 168, 85, 32, 100, 149, 173), area = c(50708, 566432, 113417, 51945, 156361, 103766, 4862, 1982, 54090, 58073, 6425, 82677, 55748, 36097, 55941, 81787, 39650, 44930, 30920, 9891, 7826, 56817, 79289, 47296, 68995, 145587, 76483, 109889, 9027, 7521, 121412, 47831, 48798, 69273, 40975, 68782, 96184, 44966, 1049, 30225, 75955, 41328, 262134, 82096, 9267, 39780, 66570, 24070, 54464, 97203), region = structure(c(2L, 4L, 4L, 2L, 4L, 4L, 1L, 2L, 2L, 2L, 4L, 4L, 3L, 3L, 3L, 3L, 2L, 2L, 1L, 2L, 1L, 3L, 3L, 2L, 3L, 4L, 3L, 4L, 1L, 1L, 4L, 1L, 2L, 3L, 3L, 2L, 4L, 1L, 1L, 2L, 3L, 2L, 2L, 4L, 1L, 2L, 4L, 2L, 3L, 4L), .Label = c('Northeast', 'South', 'North Central', 'West'), class = 'factor')), .Names = c('population', 'income', 'illiteracy', 'life.exp', 'murder', 'hs.grad', 'frost', 'area', 'region'), row.names = c('Alabama', 'Alaska', 'Arizona', 'Arkansas', 'California', 'Colorado', 'Connecticut', 'Delaware', 'Florida', 'Georgia', 'Hawaii', 'Idaho', 'Illinois', 'Indiana', 'Iowa', 'Kansas', 'Kentucky', 'Louisiana', 'Maine', 'Maryland', 'Massachusetts', 'Michigan', 'Minnesota', 'Mississippi', 'Missouri', 'Montana', 'Nebraska', 'Nevada', 'New Hampshire', 'New Jersey', 'New Mexico', 'New York', 'North Carolina', 'North Dakota', 'Ohio', 'Oklahoma', 'Oregon', 'Pennsylvania', 'Rhode Island', 'South Carolina', 'South Dakota', 'Tennessee', 'Texas', 'Utah', 'Vermont', 'Virginia', 'Washington', 'West Virginia', 'Wisconsin', 'Wyoming'), class = 'data.frame'), value = c('population', 'income', 'illiteracy', 'life.exp', 'murder', 'hs.grad', 'frost', 'area', 'region'));`names<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testnamesassign3() {
        assertEval("argv <- list(structure(list(`Sepal Length` = c(5.1, 4.9, 4.7, 4.6, 5, 5.4, 4.6, 5, 4.4, 4.9, 5.4, 4.8, 4.8, 4.3, 5.8, 5.7, 5.4, 5.1, 5.7, 5.1, 5.4, 5.1, 4.6, 5.1, 4.8, 5, 5, 5.2, 5.2, 4.7, 4.8, 5.4, 5.2, 5.5, 4.9, 5, 5.5, 4.9, 4.4, 5.1, 5, 4.5, 4.4, 5, 5.1, 4.8, 5.1, 4.6, 5.3, 5, 7, 6.4, 6.9, 5.5, 6.5, 5.7, 6.3, 4.9, 6.6, 5.2, 5, 5.9, 6, 6.1, 5.6, 6.7, 5.6, 5.8, 6.2, 5.6, 5.9, 6.1, 6.3, 6.1, 6.4, 6.6, 6.8, 6.7, 6, 5.7, 5.5, 5.5, 5.8, 6, 5.4, 6, 6.7, 6.3, 5.6, 5.5, 5.5, 6.1, 5.8, 5, 5.6, 5.7, 5.7, 6.2, 5.1, 5.7, 6.3, 5.8, 7.1, 6.3, 6.5, 7.6, 4.9, 7.3, 6.7, 7.2, 6.5, 6.4, 6.8, 5.7, 5.8, 6.4, 6.5, 7.7, 7.7, 6, 6.9, 5.6, 7.7, 6.3, 6.7, 7.2, 6.2, 6.1, 6.4, 7.2, 7.4, 7.9, 6.4, 6.3, 6.1, 7.7, 6.3, 6.4, 6, 6.9, 6.7, 6.9, 5.8, 6.8, 6.7, 6.7, 6.3, 6.5, 6.2, 5.9), `Sepal Width` = c(3.5, 3, 3.2, 3.1, 3.6, 3.9, 3.4, 3.4, 2.9, 3.1, 3.7, 3.4, 3, 3, 4, 4.4, 3.9, 3.5, 3.8, 3.8, 3.4, 3.7, 3.6, 3.3, 3.4, 3, 3.4, 3.5, 3.4, 3.2, 3.1, 3.4, 4.1, 4.2, 3.1, 3.2, 3.5, 3.6, 3, 3.4, 3.5, 2.3, 3.2, 3.5, 3.8, 3, 3.8, 3.2, 3.7, 3.3, 3.2, 3.2, 3.1, 2.3, 2.8, 2.8, 3.3, 2.4, 2.9, 2.7, 2, 3, 2.2, 2.9, 2.9, 3.1, 3, 2.7, 2.2, 2.5, 3.2, 2.8, 2.5, 2.8, 2.9, 3, 2.8, 3, 2.9, 2.6, 2.4, 2.4, 2.7, 2.7, 3, 3.4, 3.1, 2.3, 3, 2.5, 2.6, 3, 2.6, 2.3, 2.7, 3, 2.9, 2.9, 2.5, 2.8, 3.3, 2.7, 3, 2.9, 3, 3, 2.5, 2.9, 2.5, 3.6, 3.2, 2.7, 3, 2.5, 2.8, 3.2, 3, 3.8, 2.6, 2.2, 3.2, 2.8, 2.8, 2.7, 3.3, 3.2, 2.8, 3, 2.8, 3, 2.8, 3.8, 2.8, 2.8, 2.6, 3, 3.4, 3.1, 3, 3.1, 3.1, 3.1, 2.7, 3.2, 3.3, 3, 2.5, 3, 3.4, 3), `Petal Length` = c(1.4, 1.4, 1.3, 1.5, 1.4, 1.7, 1.4, 1.5, 1.4, 1.5, 1.5, 1.6, 1.4, 1.1, 1.2, 1.5, 1.3, 1.4, 1.7, 1.5, 1.7, 1.5, 1, 1.7, 1.9, 1.6, 1.6, 1.5, 1.4, 1.6, 1.6, 1.5, 1.5, 1.4, 1.5, 1.2, 1.3, 1.4, 1.3, 1.5, 1.3, 1.3, 1.3, 1.6, 1.9, 1.4, 1.6, 1.4, 1.5, 1.4, 4.7, 4.5, 4.9, 4, 4.6, 4.5, 4.7, 3.3, 4.6, 3.9, 3.5, 4.2, 4, 4.7, 3.6, 4.4, 4.5, 4.1, 4.5, 3.9, 4.8, 4, 4.9, 4.7, 4.3, 4.4, 4.8, 5, 4.5, 3.5, 3.8, 3.7, 3.9, 5.1, 4.5, 4.5, 4.7, 4.4, 4.1, 4, 4.4, 4.6, 4, 3.3, 4.2, 4.2, 4.2, 4.3, 3, 4.1, 6, 5.1, 5.9, 5.6, 5.8, 6.6, 4.5, 6.3, 5.8, 6.1, 5.1, 5.3, 5.5, 5, 5.1, 5.3, 5.5, 6.7, 6.9, 5, 5.7, 4.9, 6.7, 4.9, 5.7, 6, 4.8, 4.9, 5.6, 5.8, 6.1, 6.4, 5.6, 5.1, 5.6, 6.1, 5.6, 5.5, 4.8, 5.4, 5.6, 5.1, 5.1, 5.9, 5.7, 5.2, 5, 5.2, 5.4, 5.1), `Petal Width` = c(0.2, 0.2, 0.2, 0.2, 0.2, 0.4, 0.3, 0.2, 0.2, 0.1, 0.2, 0.2, 0.1, 0.1, 0.2, 0.4, 0.4, 0.3, 0.3, 0.3, 0.2, 0.4, 0.2, 0.5, 0.2, 0.2, 0.4, 0.2, 0.2, 0.2, 0.2, 0.4, 0.1, 0.2, 0.2, 0.2, 0.2, 0.1, 0.2, 0.2, 0.3, 0.3, 0.2, 0.6, 0.4, 0.3, 0.2, 0.2, 0.2, 0.2, 1.4, 1.5, 1.5, 1.3, 1.5, 1.3, 1.6, 1, 1.3, 1.4, 1, 1.5, 1, 1.4, 1.3, 1.4, 1.5, 1, 1.5, 1.1, 1.8, 1.3, 1.5, 1.2, 1.3, 1.4, 1.4, 1.7, 1.5, 1, 1.1, 1, 1.2, 1.6, 1.5, 1.6, 1.5, 1.3, 1.3, 1.3, 1.2, 1.4, 1.2, 1, 1.3, 1.2, 1.3, 1.3, 1.1, 1.3, 2.5, 1.9, 2.1, 1.8, 2.2, 2.1, 1.7, 1.8, 1.8, 2.5, 2, 1.9, 2.1, 2, 2.4, 2.3, 1.8, 2.2, 2.3, 1.5, 2.3, 2, 2, 1.8, 2.1, 1.8, 1.8, 1.8, 2.1, 1.6, 1.9, 2, 2.2, 1.5, 1.4, 2.3, 2.4, 1.8, 1.8, 2.1, 2.4, 2.3, 1.9, 2.3, 2.5, 2.3, 1.9, 2, 2.3, 1.8), Species = structure(c(1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L), .Label = c('setosa', 'versicolor', 'virginica'), class = 'factor')), .Names = c('Sepal Length', 'Sepal Width', 'Petal Length', 'Petal Width', 'Species'), row.names = c(NA, -150L), class = 'data.frame'), value = c('Sepal Length', 'Sepal Width', 'Petal Length', 'Petal Width', 'Species'));`names<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testnamesassign4() {
        assertEval("argv <- list(structure(list(y = c(83, 88.5, 88.2, 89.5, 96.2, 98.1, 99, 100, 101.2, 104.6, 108.4, 110.8, 112.6, 114.2, 115.7, 116.9), GNP = c(234.289, 259.426, 258.054, 284.599, 328.975, 346.999, 365.385, 363.112, 397.469, 419.18, 442.769, 444.546, 482.704, 502.601, 518.173, 554.894), Unemployed = c(235.6, 232.5, 368.2, 335.1, 209.9, 193.2, 187, 357.8, 290.4, 282.2, 293.6, 468.1, 381.3, 393.1, 480.6, 400.7), Armed.Forces = c(159, 145.6, 161.6, 165, 309.9, 359.4, 354.7, 335, 304.8, 285.7, 279.8, 263.7, 255.2, 251.4, 257.2, 282.7), Population = c(107.608, 108.632, 109.773, 110.929, 112.075, 113.27, 115.094, 116.219, 117.388, 118.734, 120.445, 121.95, 123.366, 125.368, 127.852, 130.081), Year = 1947:1962, Employed = c(60.323, 61.122, 60.171, 61.187, 63.221, 63.639, 64.989, 63.761, 66.019, 67.857, 68.169, 66.513, 68.655, 69.564, 69.331, 70.551)), .Names = c('y', 'GNP', 'Unemployed', 'Armed.Forces', 'Population', 'Year', 'Employed'), row.names = 1947:1962, class = 'data.frame'), value = c('y', 'GNP', 'Unemployed', 'Armed.Forces', 'Population', 'Year', 'Employed'));`names<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testnamesassign5() {
        assertEval("argv <- list(c(-3.21402130636699, 101.08748330158, -8.50234284659562), value = NULL);`names<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testnamesassign6() {
        assertEval("argv <- list(structure(1:3, .Names = c(NA, 'b', NA)), value = c(NA, 'b'));`names<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testnamesassign7() {
        assertEval("argv <- list(structure(c(3.14159265358979e-10, 0.0314159265358979, 3.14159265358979, 31.4159265358979, 314.159265358979, 314159265.358979, 3.14159265358979e+20), .Names = c('3.14e-10', '0.0314', '3.14', '31.4', '314', '3.14e+08', '3.14e+20')), value = c('3.14e-10', '0.0314', '3.14', '31.4', '314', '3.14e+08', '3.14e+20'));`names<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testnamesassign8() {
        assertEval("argv <- list(structure(c('variable1', 'variable2'), .Names = c('variable1', 'variable2')), value = c('variable1', 'variable2'));`names<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testnamesassign9() {
        assertEval("argv <- list(structure(c(NA, FALSE, TRUE), .Names = c(NA, 'FALSE', 'TRUE')), value = c(NA, 'FALSE', 'TRUE'));`names<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testnamesassign10() {
        assertEval("argv <- list(structure(list(), .Names = character(0)), character(0));`names<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testnamesassign11() {
        assertEval("argv <- list(structure(list(save.object = NULL, panel.error = NULL, drop.unused.levels = NULL, default.theme = NULL, legend.bbox = NULL, banking = NULL, default.args = NULL, axis.padding = NULL, skip.boundary.labels = NULL, interaction.sep = NULL, panel.contourplot = NULL, panel.levelplot = NULL, panel.levelplot.raster = NULL, panel.parallel = NULL, panel.densityplot = NULL, panel.splom = NULL, panel.wireframe = NULL, panel.dotplot = NULL, panel.qq = NULL, panel.stripplot = NULL, panel.xyplot = NULL, panel.qqmath = NULL,     panel.barchart = NULL, panel.bwplot = NULL, panel.histogram = NULL, panel.cloud = NULL, panel.pairs = NULL, prepanel.default.bwplot = NULL, prepanel.default.cloud = NULL, prepanel.default.densityplot = NULL, prepanel.default.histogram = NULL, prepanel.default.levelplot = NULL, prepanel.default.parallel = NULL, prepanel.default.qq = NULL, prepanel.default.qqmath = NULL, prepanel.default.splom = NULL, prepanel.default.xyplot = NULL, prepanel.default.dotplot = NULL, prepanel.default.barchart = NULL,     prepanel.default.wireframe = NULL, prepanel.default.contourplot = NULL, axis.units = NULL, layout.heights = NULL, layout.widths = NULL, highlight.gpar = NULL), .Names = c('save.object', 'panel.error', 'drop.unused.levels', 'default.theme', 'legend.bbox', 'banking', 'default.args', 'axis.padding', 'skip.boundary.labels', 'interaction.sep', 'panel.contourplot', 'panel.levelplot', 'panel.levelplot.raster', 'panel.parallel', 'panel.densityplot', 'panel.splom', 'panel.wireframe', 'panel.dotplot', 'panel.qq', 'panel.stripplot', 'panel.xyplot', 'panel.qqmath', 'panel.barchart', 'panel.bwplot', 'panel.histogram', 'panel.cloud', 'panel.pairs', 'prepanel.default.bwplot', 'prepanel.default.cloud', 'prepanel.default.densityplot', 'prepanel.default.histogram', 'prepanel.default.levelplot', 'prepanel.default.parallel', 'prepanel.default.qq', 'prepanel.default.qqmath', 'prepanel.default.splom', 'prepanel.default.xyplot', 'prepanel.default.dotplot', 'prepanel.default.barchart', 'prepanel.default.wireframe', 'prepanel.default.contourplot', 'axis.units', 'layout.heights', 'layout.widths', 'highlight.gpar')), value = c('save.object', 'panel.error', 'drop.unused.levels', 'default.theme', 'legend.bbox', 'banking', 'default.args', 'axis.padding', 'skip.boundary.labels', 'interaction.sep', 'panel.contourplot', 'panel.levelplot', 'panel.levelplot.raster', 'panel.parallel', 'panel.densityplot', 'panel.splom', 'panel.wireframe', 'panel.dotplot', 'panel.qq', 'panel.stripplot', 'panel.xyplot', 'panel.qqmath', 'panel.barchart', 'panel.bwplot', 'panel.histogram', 'panel.cloud', 'panel.pairs', 'prepanel.default.bwplot', 'prepanel.default.cloud', 'prepanel.default.densityplot', 'prepanel.default.histogram', 'prepanel.default.levelplot', 'prepanel.default.parallel', 'prepanel.default.qq', 'prepanel.default.qqmath', 'prepanel.default.splom', 'prepanel.default.xyplot', 'prepanel.default.dotplot', 'prepanel.default.barchart', 'prepanel.default.wireframe', 'prepanel.default.contourplot', 'axis.units', 'layout.heights', 'layout.widths', 'highlight.gpar'));`names<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testnamesassign12() {
        assertEval("argv <- list(structure(c(100, -1e-13, Inf, -Inf, NaN, 3.14159265358979, NA), .Names = c(' 100', '-1e-13', ' Inf', '-Inf', ' NaN', '3.14', '  NA')), value = c(' 100', '-1e-13', ' Inf', '-Inf', ' NaN', '3.14', '  NA'));`names<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testnamesassign13() {
        assertEval("argv <- list(structure(list(A = 0:10, B = 10:20, `NA` = 20:30), .Names = c('A', 'B', NA), row.names = c(NA, -11L), class = 'data.frame'), value = c('A', 'B', NA));`names<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testnamesassign15() {
        assertEval("argv <- list(structure(list(surname = structure(c(5L, 6L, 4L, 3L, 3L, 1L, 2L), .Label = c('McNeil', 'R Core', 'Ripley', 'Tierney', 'Tukey', 'Venables'), class = 'factor'), title = structure(c(2L, 5L, 4L, 6L, 7L, 3L, 1L), .Label = c('An Introduction to R', 'Exploratory Data Analysis', 'Interactive Data Analysis', 'LISP-STAT', 'Modern Applied Statistics ...', 'Spatial Statistics', 'Stochastic Simulation'), class = 'factor'), other.author = structure(c(NA, 1L, NA, NA, NA, NA, 2L), .Label = c('Ripley', 'Venables & Smith'), class = 'factor')), .Names = c('surname', 'title', 'other.author'), row.names = c(NA, -7L), class = 'data.frame'), value = c('surname', 'title', 'other.author'));`names<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testnamesassign16() {
        assertEval("argv <- list(structure(1:3, .Names = c('foo', 'bar', 'baz')), value = c('foo', 'bar', 'baz'));`names<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testnamesassign17() {
        assertEval("argv <- list(structure(c(1+1i, 1.2+10i), .Names = c('a', 'b')), value = c('a', 'b'));`names<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testnamesassign18() {
        assertEval("argv <- list(structure(c(67L, 34L), .Dim = 2L, .Dimnames = list(c('\\\'actual\\\'', 'virtual')), class = 'table'), value = c('\\\'actual\\\'', 'virtual'));`names<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testUpdateNames() {
        assertEval("{ x <- c(1,2) ; names(x) <- c(\"hello\", \"hi\"); names(x) } ");

        assertEval("{ x <- 1:2 ; names(x) <- c(\"hello\", \"hi\"); names(x) } ");
        assertEval("{ x<-c(1, 2); attr(x, \"names\")<-c(\"a\", \"b\"); x }");
        assertEval("{ x<-c(1, 2); attr(x, \"names\")<-c(\"a\", \"b\"); names(x)<-NULL; attributes(x) }");
        assertEval("{ x<-c(1, 2); names(x)<-c(\"a\", \"b\"); attr(x, \"names\")<-NULL; x }");
        assertEval("{ x<-c(1, 2); names(x)<-42; x }");
        assertEval("{ x<-c(1, 2); names(x)<-c(TRUE, FALSE); x }");
        assertEval(Output.ContainsError, "{ x<-c(1,2); names(x) <- 42:44; x }");
        assertEval(Output.ContainsError, "{ x<-c(1,2); attr(x, \"names\") <- 42:45; x }");
        assertEval("{ x<-list(1,2); names(x)<-c(\"a\",NA); x }");
        assertEval("{ x<-list(1,2); names(x)<-c(\"a\",\"$\"); x }");
        assertEval("{ x<-list(1,2); names(x)<-c(\"a\",\"b\"); x }");
        assertEval("{ x<-list(1,2); names(x)<-42:43; x }");
        assertEval("{ x<-7; attr(x, \"foo\")<-\"a\"; attr(x, \"bar\")<-42; attributes(x) }");
        assertEval("{ x<-c(\"a\", \"\", \"bbb\", \"\", \"c\"); names(x)<-1:4; x }");

        assertEval("{ x <- c(1,2); names(x) <- c(\"hello\", \"hi\") ; x }");
        assertEval("{ x <- 1:2; names(x) <- c(\"hello\", \"hi\") ; x }");

        assertEval("{ x <- c(1,9); names(x) <- c(\"hello\",\"hi\") ; sqrt(x) }");
        assertEval("{ x <- c(1,9); names(x) <- c(\"hello\",\"hi\") ; is.na(x) }");
        assertEval("{ x <- c(1,NA); names(x) <- c(\"hello\",\"hi\") ; cumsum(x) }");
        assertEval("{ x <- c(1,NA); names(x) <- c(NA,\"hi\") ; cumsum(x) }");

        assertEval("{ x <- 1:2; names(x) <- c(\"A\", \"B\") ; abs(x) }");
        assertEval("{ z <- c(a=1, b=2) ; names(z) <- NULL ; z }");
        assertEval("{ x <- c(1,2) ; names(x) <- c(\"hello\"); names(x) }");
        assertEval("{ x <- 1:2 ; names(x) <- c(\"hello\"); names(x) }");

        assertEval("{ x <- c(1,2); names(x) <- c(\"A\", \"B\") ; x + 1 }");
        assertEval("{ x <- 1:2; names(x) <- c(\"A\", \"B\") ; y <- c(1,2,3,4) ; names(y) <- c(\"X\", \"Y\", \"Z\") ; x + y }");

        assertEval(Output.ContainsError, "{ x <- quote(plot(x = age, y = weight)); names(x)<- c(\"\", \"a\", \"b\", \"d\")}");
        assertEval("{ x <- quote(plot(x = age, y = weight)); names(x)<- c(\"\", \"a\", \"b\"); x}");
        assertEval("{ x <- quote(plot(x = age, y = weight)); x$x <- \"random\"; x}");
    }
}
