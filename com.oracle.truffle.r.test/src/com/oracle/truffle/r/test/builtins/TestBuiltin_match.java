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
public class TestBuiltin_match extends TestBase {

    @Test
    public void testmatch1() {
        assertEval("argv <- list('corMatrix', c('dpoMatrix', 'dsyMatrix', 'ddenseMatrix', 'symmetricMatrix', 'dMatrix', 'denseMatrix', 'compMatrix', 'Matrix', 'mMatrix'), NA_integer_, NULL); .Internal(match(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testmatch2() {
        assertEval("argv <- list(c('ANY', 'abIndex', 'ddenseMatrix', 'diagonalMatrix', 'dsparseMatrix', 'lMatrix', 'nMatrix', 'nsparseVector', 'pMatrix', 'sparseVector'), 'ANY', NA_integer_, NULL); .Internal(match(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testmatch3() {
        assertEval("argv <- list(character(0), NA_integer_, NA_integer_, NULL); .Internal(match(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testmatch4() {
        assertEval("argv <- list(c('1', '2', NA), NA_real_, NA_integer_, NULL); .Internal(match(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testmatch5() {
        assertEval("argv <- list(c(0.00711247435174189, 0.251292124343149, -0.319172743733056, 5.75733114833721e-05, -0.35788385867217, -0.423873493915367, -0.440922191441033, 0.454737405613056, -0.337349081024889, -0.340540089756868, 0.0142999714851724, -0.337349081024889, 0.16929974943645, 0.0119141094780619, 0.0237947544260095, 0.481799107922823, -0.398620160881439, 0.112296211162227, 0.124500575635478, -0.423873493915367, 0.476631055345105, -0.201544176575946, 0.0504435384277691, 0.0142999714851724, 0.0859627732681778, -0.402191440217491, 0.0237947544260095, -0.35788385867217, 0.131606068222389, -0.328335725283617, -0.366873527650917, 0.855944113774621, 0.0506448607016037, -0.540294711232517, 0.365377890605673, 0.122315677921641, 0.122315677921641, 0.476631055345105, 0.0859627732681778, 0.028962807003728, 0.130710526672205, 0.704128425262244, 0.0119141094780619, 0.0506448607016037, 0.0859627732681778, 0.131606068222389, 0.122315677921641, -0.429041546493085, 0.0506448607016037, -0.35788385867217, 0.746844979419744, -0.158827622418446, -0.340540089756868, 0.130710526672205, -0.429041546493085, 0.126579318324608, 0.0119141094780619, 0.251292124343149, -0.283536551482645, 0.107466982896435, 0.586499858105134, -0.402392762491326, -0.85437461044313, 0.133663557186039, -0.328335725283617, 0.124500575635478, 0.0237947544260095, 0.133663557186039, 0.133663557186039, 0.656149860060726, 0.579415619243703, 0.107466982896435, -0.599127482939288, -0.326256982594487, 0.746844979419744, -0.452778727607612, -0.328335725283617, 0.0119141094780619, -0.340540089756868, -0.319172743733056, -0.725390113737062, 0.503481161620698, -0.661275243349858, -0.402392762491326, 0.476631055345105, 0.126579318324608, 0.251292124343149, -0.0874584103134217, 0.107466982896435, -0.201544176575946, 0.0734191385691725), c(-0.85437461044313, -0.725390113737062, -0.661275243349858, -0.599127482939288, -0.540294711232517, -0.452778727607612, -0.440922191441033, -0.429041546493085, -0.423873493915367, -0.402392762491326, -0.402191440217491, -0.398620160881439, -0.366873527650917, -0.35788385867217, -0.340540089756868, -0.337349081024889, -0.328335725283617, -0.326256982594487, -0.319172743733056, -0.283536551482645, -0.201544176575946, -0.158827622418446, -0.0874584103134217, 5.75733114833721e-05, 0.00711247435174189, 0.0119141094780619, 0.0142999714851724, 0.0237947544260095, 0.028962807003728, 0.0504435384277691, 0.0506448607016037, 0.0734191385691725, 0.0859627732681778, 0.107466982896435, 0.112296211162227, 0.122315677921641, 0.124500575635478, 0.126579318324608, 0.130710526672205, 0.131606068222389, 0.133663557186039, 0.16929974943645, 0.251292124343149, 0.365377890605673, 0.454737405613056, 0.476631055345105, 0.481799107922823, 0.503481161620698, 0.579415619243703, 0.586499858105134, 0.656149860060726, 0.704128425262244, 0.746844979419744, 0.855944113774621), NA_integer_, NULL); .Internal(match(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testmatch6() {
        assertEval("argv <- list(c('1008', '1011', '1013', '1014', '1015', '1016', '1027', '1028', '1030', '1032', '1051', '1052', '1083', '1093', '1095', '1096', '110', '1102', '111', '1117', '112', '113', '116', '117', '1219', '125', '1250', '1251', '126', '127', '128', '1291', '1292', '1293', '1298', '1299', '130', '1308', '135', '1376', '1377', '1383', '1408', '1409', '141', '1410', '1411', '1413', '1418', '1422', '1438', '1445', '1456', '1492', '2001', '2316', '262', '266', '269', '270', '2708', '2714', '2715', '272', '2728', '2734', '280', '283', '286', '290', '3501', '411', '412', '475', '5028', '5042', '5043', '5044', '5045', '5047', '5049', '5050', '5051', '5052', '5053', '5054', '5055', '5056', '5057', '5058', '5059', '5060', '5061', '5062', '5066', '5067', '5068', '5069', '5070', '5072', '5073', '5115', '5160', '5165', '655', '724', '885', '931', '942', '952', '955', '958', 'c118', 'c168', 'c203', 'c204', 'c266'), NA_integer_, NA_integer_, NULL); .Internal(match(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testmatch7() {
        assertEval("argv <- list(character(0), c('methods', 'utils', 'XML', 'RCurl'), 0L, NULL); .Internal(match(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testmatch8() {
        assertEval("argv <- list(c('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '10', '11', '12', '13', '14', '15'), NA_real_, NA_integer_, NULL); .Internal(match(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testmatch9() {
        assertEval("argv <- list(c(-1628571, -1628571, -1200000, -1200000, -1057143, -914286, -771429, -771429, -771429, -628571, -628571, -485714, -485714, -485714, -485714, -342857, -342857, -342857, -342857, -2e+05, -2e+05, -2e+05, -2e+05, -57143, -57143, -57143, 85714, 85714, 228571, 228571, 228571, 371429, 371429, 371429, 371429, 514286, 514286, 514286, 657143, 657143, 657143, 657143, 657143, 942857, 1085714, 1228571, 1228571, 1228571, 1228571, 1371429), c(-1628571, -1200000, -1057143, -914286, -771429, -628571, -485714, -342857, -2e+05, -57143, 85714, 228571, 371429, 514286, 657143, 942857, 1085714, 1228571, 1371429), NA_integer_, NULL); .Internal(match(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testmatch10() {
        assertEval("argv <- list(structure(1:27, .Label = c('M16', 'M05', 'M02', 'M11', 'M07', 'M08', 'M03', 'M12', 'M13', 'M14', 'M09', 'M15', 'M06', 'M04', 'M01', 'M10', 'F10', 'F09', 'F06', 'F01', 'F05', 'F07', 'F02', 'F08', 'F03', 'F04', 'F11'), class = c('ordered', 'factor')), structure(c(1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 3L, 3L, 3L, 3L, 4L, 4L, 4L, 4L, 5L, 5L, 5L, 5L, 6L, 6L, 6L, 6L, 7L, 7L, 7L, 7L, 8L, 8L, 8L, 8L, 9L, 9L, 9L, 9L, 10L, 10L, 10L, 10L, 11L, 11L, 11L, 11L, 12L, 12L, 12L, 12L, 13L, 13L, 13L, 13L, 14L, 14L, 14L, 14L, 15L, 15L, 15L, 15L, 16L, 16L, 16L, 16L, 17L, 17L, 17L, 17L, 18L, 18L, 18L, 18L, 19L, 19L, 19L, 19L, 20L, 20L, 20L, 20L, 21L, 21L, 21L, 21L, 22L, 22L, 22L, 22L, 23L, 23L, 23L, 23L, 24L, 24L, 24L, 24L, 25L, 25L, 25L, 25L, 26L, 26L, 26L, 26L, 27L, 27L, 27L, 27L), .Label = c('M16', 'M05', 'M02', 'M11', 'M07', 'M08', 'M03', 'M12', 'M13', 'M14', 'M09', 'M15', 'M06', 'M04', 'M01', 'M10', 'F10', 'F09', 'F06', 'F01', 'F05', 'F07', 'F02', 'F08', 'F03', 'F04', 'F11'), class = c('ordered', 'factor')), NA_integer_, NULL); .Internal(match(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testmatch11() {
        assertEval("argv <- list('g', 'l', NA_character_, NULL); .Internal(match(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testmatch12() {
        assertEval("argv <- list(1:4, 3L, 0L, NULL); .Internal(match(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testmatch13() {
        assertEval("argv <- list(c('0.5', '0.5', '0.5', '0.5', '0.5'), 0.5, NA_integer_, NULL); .Internal(match(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testmatch14() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(list(c0 = structure(integer(0), .Label = character(0), class = 'factor')), .Names = 'c0', row.names = character(0), class = 'data.frame'), structure(list(c0 = structure(integer(0), .Label = character(0), class = 'factor')), .Names = 'c0', row.names = character(0), class = 'data.frame'), 0L, NULL); .Internal(match(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testmatch15() {
        assertEval("argv <- list(c('May', 'Jun', 'Jul', 'Aug', 'Sep'), c(NA, NaN), 0L, NULL); .Internal(match(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testmatch16() {
        assertEval("argv <- list(c(1L, 2L, 4L, 13L, 14L, 15L, 16L, 17L, 18L, 23L), c(23L, 28L), 0L, NULL); .Internal(match(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testmatch17() {
        assertEval("argv <- list(c('dMatrix', 'nonStructure', 'structure'), c('nonStructure', 'structure'), NA_integer_, NULL); .Internal(match(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testmatch18() {
        assertEval("argv <- list(structure(c(0, 1), .Names = c('Domestic', 'Foreign')), NA_integer_, NA_integer_, NULL); .Internal(match(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testmatch19() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(list(col = 1, cellvp = structure(list(structure(list(x = structure(0.5, unit = 'npc', valid.unit = 0L, class = 'unit'), y = structure(0.5, unit = 'npc', valid.unit = 0L, class = 'unit'), width = structure(1, unit = 'npc', valid.unit = 0L, class = 'unit'), height = structure(1, unit = 'npc', valid.unit = 0L, class = 'unit'), justification = 'centre', gp = structure(list(), class = 'gpar'), clip = FALSE, xscale = c(0, 1), yscale = c(0, 1), angle = 0, layout = NULL, layout.pos.row = c(1L, 1L), layout.pos.col = c(1L, 1L), valid.just = c(0.5, 0.5), valid.pos.row = c(1L, 1L), valid.pos.col = c(1L, 1L), name = 'GRID.VP.8'), .Names = c('x', 'y', 'width', 'height', 'justification', 'gp', 'clip', 'xscale', 'yscale', 'angle', 'layout', 'layout.pos.row', 'layout.pos.col', 'valid.just', 'valid.pos.row', 'valid.pos.col', 'name'), class = 'viewport'), structure(list(x = structure(1, unit = 'lines', valid.unit = 3L, data = list(NULL), class = 'unit'), y = structure(1, unit = 'lines', valid.unit = 3L, data = list(    NULL), class = 'unit'), width = structure(list(fname = '-', arg1 = structure(1, unit = 'npc', valid.unit = 0L, class = 'unit'), arg2 = structure(list(fname = 'sum', arg1 = structure(c(1, 1), unit = c('lines', 'lines'), valid.unit = c(3L, 3L), data = list(NULL, NULL), class = 'unit'), arg2 = NULL), .Names = c('fname', 'arg1', 'arg2'), class = c('unit.arithmetic', 'unit'))), .Names = c('fname', 'arg1', 'arg2'), class = c('unit.arithmetic', 'unit')), height = structure(list(fname = '-', arg1 = structure(1, unit = 'npc', valid.unit = 0L, class = 'unit'),     arg2 = structure(list(fname = 'sum', arg1 = structure(c(1, 1), unit = c('lines', 'lines'), valid.unit = c(3L, 3L), data = list(NULL, NULL), class = 'unit'), arg2 = NULL), .Names = c('fname', 'arg1', 'arg2'), class = c('unit.arithmetic', 'unit'))), .Names = c('fname', 'arg1', 'arg2'), class = c('unit.arithmetic', 'unit')), justification = c('left', 'bottom'), gp = structure(list(), class = 'gpar'), clip = FALSE, xscale = c(0, 1), yscale = c(0, 1), angle = 0, layout = NULL, layout.pos.row = NULL,     layout.pos.col = NULL, valid.just = c(0, 0), valid.pos.row = NULL, valid.pos.col = NULL, name = 'GRID.VP.9'), .Names = c('x', 'y', 'width', 'height', 'justification', 'gp', 'clip', 'xscale', 'yscale', 'angle', 'layout', 'layout.pos.row', 'layout.pos.col', 'valid.just', 'valid.pos.row', 'valid.pos.col', 'name'), class = 'viewport')), class = c('vpStack', 'viewport'))), .Names = c('col', 'cellvp')), c('children', 'childrenOrder'), 0L, NULL); .Internal(match(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testmatch20() {
        assertEval("argv <- list(structure(c(1, 1, 6, 2, 2, 7, 3, 3, 7, 3, 3, 8, 4, 4, 4, 5), .Dim = c(16L, 1L), .Dimnames = list(c('1', '2', '3', '4', '5', '6', '7', '8', '9', '10', '11', '12', '13', '14', '15', '16'), 'y')), c(1, 2, 3, 4, 5, 6, 7, 8), NA_integer_, NULL); .Internal(match(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testmatch21() {
        assertEval("argv <- list(structure(c(0, 0, 0, 1, 1, 1, 2, 2, 2, 3, 3, 3, 0, 0, 0, 1, 1, 1, 2, 2, 2, 3, 3, 3, 0, 0, 0, 1, 1, 1, 2, 2, 2, 3, 3, 3, 0, 0, 0, 1, 1, 1, 2, 2, 2, 3, 3, 3, 0, 0, 0, 1, 1, 1, 2, 2, 2, 3, 3, 3, 0, 0, 0, 1, 1, 1, 2, 2, 2, 3, 3, 3, 0, 0, 0, 1, 1, 1, 2, 2, 2, 3, 3, 3, 0, 0, 0, 1, 1, 1, 2, 2, 2, 3, 3, 3, 0, 0, 0, 1, 1, 1, 2, 2, 2, 3, 3, 3, 0, 0, 0, 1, 1, 1, 2, 2, 2, 3, 3, 3, 0, 0, 0, 1, 1, 1, 2, 2, 2, 3, 3, 3, 0, 0, 0, 1, 1, 1, 2, 2, 2, 3, 3, 3, 0, 0, 0, 1, 1, 1, 2, 2, 2, 3, 3, 3, 0, 0, 0, 1, 1, 1, 2, 2, 2, 3, 3, 3, 0, 0, 0, 1, 1, 1, 2, 2, 2, 3, 3, 3, 0, 0, 0, 1, 1, 1, 2, 2, 2, 3, 3, 3, 0, 0, 0, 1, 1, 1, 2, 2, 2, 3, 3, 3, 0, 0, 0, 1, 1, 1, 2, 2, 2, 3, 3, 3, 0, 0, 0, 1, 1, 1, 2, 2, 2, 3, 3, 3, 0, 0, 0, 1, 1, 1, 2, 2, 2, 3, 3, 3, 0, 0, 0, 1, 1, 1, 2, 2, 2, 3, 3, 3, 0, 0, 0, 1, 1, 1, 2, 2, 2, 3, 3, 3, 0, 0, 0, 1, 1, 1, 2, 2, 2, 3, 3, 3, 0, 0, 0, 1, 1, 1, 2, 2, 2, 3, 3, 3, 0, 0, 0, 1, 1, 1, 2, 2, 2, 3, 3, 3, 0, 0, 0, 1, 1, 1, 2, 2, 2, 3, 3, 3, 0, 0, 0, 1, 1, 1, 2, 2, 2, 3, 3, 3, 0, 0, 0, 1, 1, 1, 2, 2, 2, 3, 3, 3, 0, 0, 0, 1, 1, 1, 2, 2, 2, 3, 3, 3, 0, 0, 0, 1, 1, 1, 2, 2, 2, 3, 3, 3, 0, 0, 0, 1, 1, 1, 2, 2, 2, 3, 3, 3, 0, 0, 0, 1, 1, 1, 2, 2, 2, 3, 3, 3, 0, 0, 0, 1, 1, 1, 2, 2, 2, 3, 3, 3, 0, 0, 0, 1, 1, 1, 2, 2, 2, 3, 3, 3, 0, 0, 0, 1, 1, 1, 2, 2, 2, 3, 3, 3, 0, 0, 0, 1, 1, 1, 2, 2, 2, 3, 3, 3, 0, 0, 0, 1, 1, 1, 2, 2, 2, 3, 3, 3, 0, 0, 0, 1, 1, 1, 2, 2, 2, 3, 3, 3, 0, 0, 0, 1, 1, 1, 2, 2, 2, 3, 3, 3), .Tsp = c(1959, 1997.91666667, 12), class = 'ts'), c(0, 1, 2, 3), NA_integer_, NULL); .Internal(match(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testmatch22() {
        assertEval(Ignored.Unknown, "argv <- list(c(NA, NA, 3, 4, 5), c(NA, NA, 4, 5), 0L, NA); .Internal(match(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testmatch23() {
        assertEval("argv <- list(structure('tools', .Names = 'name'), c('base', 'utils'), 0L, NULL); .Internal(match(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testmatch24() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(list(framevp = structure(list(x = structure(0.5, unit = 'npc', valid.unit = 0L, class = 'unit'), y = structure(0.5, unit = 'npc', valid.unit = 0L, class = 'unit'), width = structure(1, unit = 'npc', valid.unit = 0L, class = 'unit'), height = structure(1, unit = 'npc', valid.unit = 0L, class = 'unit'), justification = 'centre', gp = structure(list(), class = 'gpar'), clip = FALSE, xscale = c(0, 1), yscale = c(0, 1), angle = 0, layout = structure(list(nrow = 1L, ncol = 1L, widths = structure(list(    fname = 'sum', arg1 = structure(c(1, 1, 1), unit = c('lines', 'lines', 'lines'), valid.unit = c(3L, 3L, 3L), data = list(NULL, NULL, NULL), class = 'unit'), arg2 = NULL), .Names = c('fname', 'arg1', 'arg2'), class = c('unit.arithmetic', 'unit')), heights = structure(list(fname = 'sum', arg1 = structure(c(1, 1, 1), unit = c('lines', 'lines', 'lines'), valid.unit = c(3L, 3L, 3L), data = list(NULL, NULL, NULL), class = 'unit'), arg2 = NULL), .Names = c('fname', 'arg1', 'arg2'), class = c('unit.arithmetic', 'unit')), respect = FALSE, valid.respect = 0L, respect.mat = structure(0L, .Dim = c(1L, 1L)), just = 'centre', valid.just = c(0.5, 0.5)), .Names = c('nrow', 'ncol', 'widths', 'heights', 'respect', 'valid.respect', 'respect.mat', 'just', 'valid.just'), class = 'layout'), layout.pos.row = NULL, layout.pos.col = NULL, valid.just = c(0.5, 0.5), valid.pos.row = NULL, valid.pos.col = NULL, name = 'GRID.VP.33'), .Names = c('x', 'y', 'width', 'height', 'justification', 'gp', 'clip', 'xscale', 'yscale', 'angle', 'layout', 'layout.pos.row', 'layout.pos.col', 'valid.just', 'valid.pos.row', 'valid.pos.col', 'name'), class = 'viewport')), .Names = 'framevp'), c('children', 'childrenOrder'), 0L, NULL); .Internal(match(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testmatch25() {
        assertEval(Ignored.Unknown,
                        "argv <- list(' *** Run successfully completed ***', c('', '> ### R code from vignette source 'Design-issues.Rnw'', '> ', '> ###################################################', '> ### code chunk number 1: preliminarie .... [TRUNCATED] ', '', '> ###################################################', '> ### code chunk number 2: diag-class', '> ###################################################', '> li .... [TRUNCATED] ', 'Loading required package: lattice', '', 'Attaching package: ‘Matrix’', '', 'The following object is masked from ‘package:base’:', '', '    det', '', '', '> (D4 <- Diagonal(4, 10*(1:4)))', '4 x 4 diagonal matrix of class \\\'ddiMatrix\\\'', '     [,1] [,2] [,3] [,4]', '[1,]   10    .    .    .', '[2,]    .   20    .    .', '[3,]    .    .   30    .', '[4,]    .    .    .   40', '', '> str(D4)', 'Formal class 'ddiMatrix' [package \\\'Matrix\\\'] with 4 slots', '  ..@ diag    : chr \\\'N\\\'', '  ..@ Dim     : int [1:2] 4 4', '  ..@ Dimnames:List of 2', '  .. ..$ : NULL', '  .. ..$ : NULL', '  ..@ x       : num [1:4] 10 20 30 40', '', '> diag(D4)', '[1] 10 20 30 40', '', '> ###################################################', '> ### code chunk number 3: diag-2', '> ###################################################', '> diag(D .... [TRUNCATED] ', '', '> D4', '4 x 4 diagonal matrix of class \\\'ddiMatrix\\\'', '     [,1] [,2] [,3] [,4]', '[1,]   11    .    .    .', '[2,]    .   22    .    .', '[3,]    .    .   33    .', '[4,]    .    .    .   44', '', '> ###################################################', '> ### code chunk number 4: unit-diag', '> ###################################################', '> str .... [TRUNCATED] ', 'Formal class 'ddiMatrix' [package \\\'Matrix\\\'] with 4 slots', '  ..@ diag    : chr \\\'U\\\'', '  ..@ Dim     : int [1:2] 3 3', '  ..@ Dimnames:List of 2', '  .. ..$ : NULL', '  .. ..$ : NULL', '  ..@ x       : num(0) ', '', '> getClass(\\\'diagonalMatrix\\\') ## extending \\\'denseMatrix\\\'', 'Virtual Class \\\'diagonalMatrix\\\' [package \\\'Matrix\\\']', '', 'Slots:', '                                    ', 'Name:       diag       Dim  Dimnames', 'Class: character   integer      list', '', 'Extends: ', 'Class \\\'sparseMatrix\\\', directly', 'Class \\\'Matrix\\\', by class \\\'sparseMatrix\\\', distance 2', 'Class \\\'mMatrix\\\', by class \\\'Matrix\\\', distance 3', '', 'Known Subclasses: \\\'ddiMatrix\\\', \\\'ldiMatrix\\\'', '', '> ###################################################', '> ### code chunk number 5: Matrix-ex', '> ###################################################', '> (M  .... [TRUNCATED] ', '4 x 4 sparse Matrix of class \\\'dgTMatrix\\\'', '            ', '[1,] . . 4 .', '[2,] . 1 . .', '[3,] 4 . . .', '[4,] . . . 8', '', '> m <- as(M, \\\'matrix\\\')', '', '> (M. <- Matrix(m)) # dsCMatrix (i.e. *symmetric*)', '4 x 4 sparse Matrix of class \\\'dsCMatrix\\\'', '            ', '[1,] . . 4 .', '[2,] . 1 . .', '[3,] 4 . . .', '[4,] . . . 8', '', '> ###################################################', '> ### code chunk number 6: sessionInfo', '> ###################################################', '> t .... [TRUNCATED] ', '\\\\begin{itemize}\\\\raggedright', '  \\\\item R version 3.0.1 (2013-05-16), \\\\verb|x86_64-unknown-linux-gnu|', '  \\\\item Locale: \\\\verb|LC_CTYPE=en_US.UTF-8|, \\\\verb|LC_NUMERIC=C|, \\\\verb|LC_TIME=en_US.UTF-8|, \\\\verb|LC_COLLATE=C|, \\\\verb|LC_MONETARY=en_US.UTF-8|, \\\\verb|LC_MESSAGES=en_US.UTF-8|, \\\\verb|LC_PAPER=C|, \\\\verb|LC_NAME=C|, \\\\verb|LC_ADDRESS=C|, \\\\verb|LC_TELEPHONE=C|, \\\\verb|LC_MEASUREMENT=en_US.UTF-8|, \\\\verb|LC_IDENTIFICATION=C|', '  \\\\item Base packages: base, datasets, grDevices, graphics,', '    methods, stats, utils', '  \\\\item Other packages: Matrix~1.0-12, lattice~0.20-15', '  \\\\item Loaded via a namespace (and not attached): grid~3.0.1,', '    tools~3.0.1', '\\\\end{itemize}', '', ' *** Run successfully completed ***', '> proc.time()', '   user  system elapsed ', '157.417   4.183 161.773 '), 0L, NULL); .Internal(match(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testmatch26() {
        assertEval("argv <- list(c(NA, NA, NA, NA, NA, NA, NA, NA), c('real', 'double'), 0L, NULL); .Internal(match(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testmatch27() {
        assertEval("argv <- list(c('2005-01-01', '2006-01-01', '2007-01-01', '2008-01-01', '2009-01-01'), c(NA, NaN), 0L, NULL); .Internal(match(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testmatch28() {
        assertEval("argv <- list(c(NA, NA), c('real', 'double'), 0L, NULL); .Internal(match(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testmatch29() {
        assertEval("argv <- list(c('TRUE', 'FALSE', 'TRUE', 'FALSE', 'TRUE', 'FALSE', 'TRUE', 'FALSE', 'TRUE', 'FALSE'), c(FALSE, TRUE), NA_integer_, NULL); .Internal(match(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testmatch30() {
        assertEval("argv <- list(c('2005-01-01', '2005-02-01', '2005-03-01', '2005-04-01', '2005-05-01', '2005-06-01', '2005-07-01', '2005-08-01', '2005-09-01', '2005-10-01', '2005-11-01', '2005-12-01', '2006-01-01', '2006-02-01', '2006-03-01', '2006-04-01', '2006-05-01', '2006-06-01', '2006-07-01', '2006-08-01', '2006-09-01', '2006-10-01', '2006-11-01', '2006-12-01', '2007-01-01', '2007-02-01', '2007-03-01', '2007-04-01', '2007-05-01', '2007-06-01', '2007-07-01', '2007-08-01', '2007-09-01', '2007-10-01', '2007-11-01', '2007-12-01', '2008-01-01', '2008-02-01', '2008-03-01', '2008-04-01', '2008-05-01', '2008-06-01', '2008-07-01', '2008-08-01', '2008-09-01', '2008-10-01', '2008-11-01', '2008-12-01', '2009-01-01'), NA_integer_, NA_integer_, NULL); .Internal(match(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testmatch31() {
        assertEval("argv <- list(c(1, 2, 3, 4, 8, 12), c(1, 2, 3, 4, 8, 12), NA_integer_, NULL); .Internal(match(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testmatch32() {
        assertEval("argv <- list(c('.__C__classA', '.__T__$:base', '.__T__$<-:base', '.__T__[:base', '.__T__plot:graphics', 'plot'), c('.__NAMESPACE__.', '.__S3MethodsTable__.', '.packageName', '.First.lib', '.Last.lib', '.onLoad', '.onAttach', '.onDetach', '.conflicts.OK', '.noGenerics'), 0L, NULL); .Internal(match(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testMatch() {
        assertEval("{ match(2,c(1,2,3)) }");
        assertEval("{ match(c(1,2,3,4,5),c(1,2,1,2)) }");
        assertEval("{ match(\"hello\",c(\"I\", \"say\", \"hello\", \"world\")) }");
        assertEval("{ match(c(\"hello\", \"say\"),c(\"I\", \"say\", \"hello\", \"world\")) }");
        assertEval("{ match(\"abc\", c(\"xyz\")) }");
        assertEval("{ match(\"abc\", c(\"xyz\"), nomatch=-1) }");

        assertEval("{ match(c(1,2,3,\"NA\",NA), c(NA,\"NA\",1,2,3,4,5,6,7,8,9,10)) }");
        assertEval("{ match(c(1L,2L,3L,1L,NA), c(NA,1L,1L,2L,3L,4L,5L,6L,7L,8L,9L,10L)) }");
        assertEval("{ match(c(1,2,3,NaN,NA,1), c(1,NA,NaN,1,2,3,4,5,6,7,8,9,10)) }");
        assertEval("{ match(c(0,1,2,3,4,5,6,7,8,9,0,1,2,3,4,5,6,7,8,9,0,1,2,3,4,5,6,7,8,9,0,1,2,3,4,5,6,7,8,9,0,1,2,3,4,5,6,7,8,9,\"NA\",NA), c(NA,\"NA\",1,2,3,4,5,6,7,8,9,10,0,1,2,3,4,5,6,7,8,9,0,1,2,3,4,5,6,7,8,9)) }");
        assertEval("{ match(c(0,1,2,3,4,5,6,7,8,9,0,1,2,3,4,5,6,7,8,9,0,1,2,3,4,5,6,7,8,9,0,1,2,3,4,5,6,7,8,9,0,1,2,3,4,5,6,7,8,9,1L,NA), c(NA,1L,1L,2L,3L,4L,5L,6L,7L,8L,9L,10L,0L,1L,1L,2L,3L,4L,5L,6L,7L,8L,9L,10L,0L,1L,1L,2L,3L,4L,5L,6L)) }");
        assertEval("{ match(c(0,1,2,3,4,5,6,7,8,9,0,1,2,3,4,5,6,7,8,9,0,1,2,3,4,5,6,7,8,9,0,1,2,3,4,5,6,7,8,9,0,1,2,3,4,5,6,7,8,9,NaN,NA,1), c(1,NA,NaN,1,2,3,4,5,6,7,8,9,10,0,1,2,3,4,5,6,7,8,9,0,1,2,3,4,5,6,7,8,9)) }");

        assertEval("{ match(factor(c(\"a\", \"b\")), factor(c(\"c\", \"b\", \"a\", \"b\", \"c\", \"a\"))) }");

        assertEval("{ match(\"a\", factor(c(\"a\", \"b\", \"a\"))) }");
        assertEval("{ match(factor(c(\"a\", \"b\", \"a\")), \"a\") }");

        assertEval("{ match(42, NULL) }");
        assertEval("{ match(c(7, 42), NULL }");
        assertEval("{ match(c(7, 42), NULL, integer() }");
        assertEval("{ match(c(7, 42), NULL, 1L }");
        assertEval("{ match(NULL, NULL) }");
    }
}
