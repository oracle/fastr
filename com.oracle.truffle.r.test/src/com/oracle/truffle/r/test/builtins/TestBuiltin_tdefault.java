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
public class TestBuiltin_tdefault extends TestBase {

    @Test
    public void testtdefault1() {
        assertEval("argv <- list(structure(c('D:', 'E:', 'F:', 'G:'), .Dim = c(4L, 1L))); .Internal(t.default(argv[[1]]))");
    }

    @Test
    public void testtdefault2() {
        assertEval("argv <- list(structure(c(0.63, -0.37, 0.4, -0.6, 0.85, -0.05, 1.02, -1.76, -1.62, -0.46, -0.57, 1.41, 0, -0.65, 0.57, -0.29, 1.22, 0.8, -0.5, 0.44, 1.63, -0.13, 0.17, 1.02, 0.11), .Dim = c(5L, 5L))); .Internal(t.default(argv[[1]]))");
    }

    @Test
    public void testtdefault3() {
        assertEval("argv <- list(structure(NA, .Dim = c(1L, 1L))); .Internal(t.default(argv[[1]]))");
    }

    @Test
    public void testtdefault4() {
        assertEval("argv <- list(structure(c(0, 0, -0.51, 0, 0, 0, 0.18, -0.15, 0, 2.62, -2.77555756156289e-16, 0, 8.26162055433954e-17, 0.560000000000001, 0, 0, 0, 0, 0, 0, 1.79, 0, 0.05, 0, 0, 0, 0, 0, 0, -0.18, -1.47, 0, -5.55111512312578e-17, 0, 0, 0.23, 0, 2.206351421008e-17, -2.12, 0), .Dim = c(5L, 8L))); .Internal(t.default(argv[[1]]))");
    }

    @Test
    public void testtdefault5() {
        assertEval("argv <- list(structure(c(NA, 0, NA, 0, NA, 0, NA, 0, NA, 0, NA, 0, NA, 0, NA, 0), .Dim = c(4L, 4L))); .Internal(t.default(argv[[1]]))");
    }

    @Test
    public void testtdefault6() {
        assertEval("argv <- list(structure(c(0, 10975969, 8779369, 10080625, 11148921, 7628644, 10732176, 6812100, 20115225, 8862529, 9180900, 20539024, 7579009, 15594601, 8208225, 5207524, 4748041, 9e+06, 667489, 15421329, 3964081, 0, 0, 1737124, 1758276, 1674436, 2244004, 4919524, 644809, 1373584, 4072324, 2220100, 1703025, 416025, 404496, 271441, 1028196, 1863225, 1067089, 2131600, 8225424, 3247204, 0, 0, 0, 41616, 339889, 42436, 933156, 458329, 5089536, 356409, 29584, 4343056, 476100, 2427364, 1022121, 855625, 558009, 81225, 2283121, 2611456, 1380625, 0, 0, 0, 0, 211600, 167281, 1290496, 558009, 4946176, 509796, 108900, 4210704, 546121, 2402500, 1121481, 1159929, 954529, 78400, 2762244, 3189796, 1907161, 0, 0, 0, 0, 0, 616225, 2387025, 727609, 4190209, 1243225, 534361, 3337929, 622521, 1814409, 1212201, 1461681, 1345600, 115600, 3218436, 4822416, 2521744, 0, 0, 0, 0, 0, 0, 577600, 2762244, 5934096, 211600, 72361, 5244100, 509796, 3111696, 1071225, 829921, 339889, 216225, 2241009, 1968409, 877969, 0, 0, 0, 0, 0, 0, 0, 2010724, 10214416, 211600, 72361, 8826841, 2125764, 6240004, 3161284, 2362369, 1218816, 1382976, 4202500, 422500, 2117025, 0, 0, 0, 0, 0, 0, 0, 0, 3900625, 1249924, 801025, 3748096, 24964, 2070721, 180625, 107584, 349281, 263169, 990025, 4276624, 1038361, 0, 0, 0, 0, 0, 0, 0, 0, 0, 8392609, 5895184, 456976, 3301489, 487204, 2866249, 4774225, 6579225, 3884841, 6922161, 15100996, 8844676, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 302500, 7134241, 1343281, 4831204, 2187441, 1532644, 648025, 769129, 3066001, 900601, 1334025, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 5198400, 744769, 2992900, 1399489, 1205604, 724201, 208849, 2832489, 2250000, 1452025, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1387684, 446224, 3104644, 5062500, 6285049, 3236401, 7290000, 10439361, 8625969, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1640961, 102400, 107584, 524176, 221841, 1098304, 4443664, 1338649, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1338649, 2972176, 4040100, 1620529, 4397409, 10163344, 5803281, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 381924, 1229881, 627264, 1022121, 5895184, 1857769, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 109561, 732736, 343396, 4782969, 806404, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 674041, 894916, 3076516, 183184, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2178576, 3337929, 1560001, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 7327849, 1461681, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4431025, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0), .Dim = c(21L, 21L))); .Internal(t.default(argv[[1]]))");
    }

    @Test
    public void testtdefault7() {
        assertEval("argv <- list(structure(logical(0), .Dim = c(0L, 0L))); .Internal(t.default(argv[[1]]))");
    }

    @Test
    public void testtdefault8() {
        assertEval("argv <- list(structure(c(TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE), .Dim = c(4L, 4L), .Dimnames = list(NULL, NULL))); .Internal(t.default(argv[[1]]))");
    }

    @Test
    public void testtdefault9() {
        assertEval("argv <- list(structure(c(0.589872882227945+0i, 0.193623477236295+0i, 0.66266867945261+0i, 0.140441698505598+0i, -0.394596353845825+0i, 0.168331537203598+0i, 0.293129347035038+0i, -0.481237717889449+0i, 0.7985227152757+0i, -0.128496737541326+0i, -0.0231518691888815+0i, -0.892171028872675+0i, 0.158252886617681+0i, 0.418477841524233+0i, -0.0576815934568704+0i, 0.471807942431513+0i, -0.00978429568549377+0i, 0.0825499722933953+0i, 0.0943143868799564+0i, 0.872692289136496+0i, -0.632910525118973+0i, 0.283760916561723+0i, 0.545364104158516+0i, 0.398269626120626+0i, 0.25072556357658+0i), .Dim = c(5L, 5L))); .Internal(t.default(argv[[1]]))");
    }

    @Test
    public void testtdefault10() {
        assertEval("argv <- list(structure(c(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 74, 68, 56, 57, 60, 74, 53, 61, 57, 57, 67, 70, 63, 57, 67, 50, 58, 72, 60, 70, 53, 74, 73, 48, 61, 65, 74, 70, 68, 74, 63, 74, 63, 68, 58, 59, 62, 57, 48, 73, 69, 68, 68, 67, 63, 74, 40, 81, 73, 59, 55, 42, 44, 71, 61, 72, 63, 70, 66, 72, 69, 71, 64, 56, 63, 59, 66, 67, 55, 69, 44, 80, 76, 49, 68, 66, 80, 75, 72, 70, 66, 50, 64, 53, 47, 67, 56, 54, 56, 74, 76, 57, 71, 54, 82, 70, 60, 55, 69, 62, 63, 69, 63, 64, 46, 61, 65, 61, 56, 53, 56, 60, 39, 58, 64, 53, 72, 52, 50, 64, 71, 70, 64, 60, 73, 62, 69, 67, 69, 65, 65, 76, 67, 76, 77, 39, 66, 1, 0, 0, 1, 0, 1, 1, 2, 1, 1, 1, 1, 2, 1, 1, 1, 0, 0, 0, 3, 1, 2, 2, 2, 2, 2, 2, 1, 1, 0, 1, 0, 0, 0, 0, 1, 0, 2, 1, 1, 1, 2, 0, 1, 0, 2, 1, 0, 0, 0, 1, 1, 1, 1, 0, 2, 0, 1, 1, 2, 1, 1, 1, 1, 1, 2, 1, 1, 1, 0, 1, 1, 2, 2, 2, 2, 1, 0, 2, 0, 1, 1, 1, 1, 0, 0, 2, 1, 0, 1, 2, 1, 0, 1, 0, 1, 0, 2, 2, 2, 1, 2, 1, 1, 0, 1, 0, 1, 1, 1, 2, 0, 0, 0, 1, 0, 1, 2, 1, 1, 1, 0, 1, 1, 1, 2, 1, 1, 1, 1, 1, 2, 1, 1, 1, 0, 1), .Dim = c(137L, 3L), .Dimnames = list(c('1', '2', '3', '4', '5', '6', '9', '10', '11', '15', '16', '17', '18', '20', '21', '23', '24', '25', '27', '28', '29', '30', '32', '33', '35', '37', '39', '41', '45', '47', '48', '49', '52', '53', '54', '55', '56', '58', '62', '63', '65', '66', '69', '70', '71', '73', '74', '79', '80', '81', '82', '83', '85', '86', '88', '90', '91', '92', '93', '96', '97', '98', '99', '103', '104', '105', '106', '108', '109', '111', '112', '113', '116', '117', '118', '119', '120', '121', '124', '125', '126', '127', '128', '132', '133', '135', '138', '139', '140', '142', '143', '145', '147', '148', '149', '151', '152', '155', '156', '158', '159', '163', '164', '165', '168', '169', '170', '171', '173', '175', '177', '181', '182', '188', '189', '190', '191', '192', '193', '194', '195', '196', '198', '200', '202', '206', '209', '212', '213', '215', '216', '218', '221', '223', '224', '225', '227'), c('(Intercept)', 'age', 'ph.ecog')), assign = 0:2)); .Internal(t.default(argv[[1]]))");
    }

    @Test
    public void testtdefault11() {
        assertEval("argv <- list(structure(c(NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_, NA_real_), .Dim = c(20L, 20L), .Dimnames = list(c(NA_character_, NA_character_, NA_character_, NA_character_, NA_character_, NA_character_, NA_character_, NA_character_, NA_character_, NA_character_, NA_character_, NA_character_, NA_character_, NA_character_, NA_character_, NA_character_, NA_character_, NA_character_, NA_character_, NA_character_), c(NA_character_, NA_character_, NA_character_, NA_character_, NA_character_, NA_character_, NA_character_, NA_character_, NA_character_, NA_character_, NA_character_, NA_character_, NA_character_, NA_character_, NA_character_, NA_character_, NA_character_, NA_character_, NA_character_, NA_character_)))); .Internal(t.default(argv[[1]]))");
    }

    @Test
    public void testtdefault12() {
        assertEval("argv <- list(structure('foo', .Dim = c(1L, 1L), .Dimnames = list(structure('object', simpleOnly = TRUE), NULL))); .Internal(t.default(argv[[1]]))");
    }

    @Test
    public void testtdefault13() {
        assertEval(Ignored.Unknown, "argv <- list(structure(c(0, 0, 0, 0, 0, 0, 3.95252516672997e-323, 0, 0, 0, 0, 0), .Dim = c(12L, 1L))); .Internal(t.default(argv[[1]]))");
    }

    @Test
    public void testtdefault14() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(c(794, 86, 150, 570), .Dim = c(2L, 2L), .Dimnames = structure(list(`1st Survey` = c('Approve', 'Disapprove'), `2nd Survey` = c('Approve', 'Disapprove')), .Names = c('1st Survey', '2nd Survey')))); .Internal(t.default(argv[[1]]))");
    }

    @Test
    public void testtdefault15() {
        assertEval("argv <- list(structure(list(3, 3, 3, 3, 3, 'fred'), .Dim = 2:3)); .Internal(t.default(argv[[1]]))");
    }

    @Test
    public void testtdefault16() {
        assertEval("argv <- list(1.28578345790245); .Internal(t.default(argv[[1]]))");
    }

    @Test
    public void testtdefault17() {
        assertEval("argv <- list(structure(c(-0.560475646552213+0i, 0.7424437487+0.205661411508856i, 1.39139505579429-0.26763356813179i, 0.928710764113827-0.221714979045717i, -0.46926798541295+1.18846175213664i, 0.7424437487-0.205661411508856i, 0.460916205989202+0i, -0.452623703774585+0.170604003753717i, -0.094501186832143+0.54302538277632i, -0.331818442379127+0.612232958468282i, 1.39139505579429+0.26763356813179i, -0.452623703774585-0.170604003753717i, 0.400771450594052+0i, -0.927967220342259+0.479716843914174i, -0.790922791530657+0.043092176305418i, 0.928710764113827+0.221714979045717i, -0.094501186832143-0.54302538277632i, -0.927967220342259-0.479716843914174i, 0.701355901563686+0i, -0.600841318509537+0.213998439984336i, -0.46926798541295-1.18846175213664i, -0.331818442379127-0.612232958468282i, -0.790922791530657-0.043092176305418i, -0.600841318509537-0.213998439984336i, -0.625039267849257+0i), .Dim = c(5L, 5L))); .Internal(t.default(argv[[1]]))");
    }

    @Test
    public void testtdefault18() {
        assertEval("argv <- list(structure(c(0, 1954.88214285714, 557.144827586207, 0, 0, 1392.34285714286, 0, 0, 0), .Dim = c(3L, 3L))); .Internal(t.default(argv[[1]]))");
    }

    @Test
    public void testtdefault19() {
        assertEval("argv <- list(c(3, 4)); .Internal(t.default(argv[[1]]))");
    }

    @Test
    public void testtdefault20() {
        assertEval("argv <- list(structure(c(0L, 1L, 0L, 0L, 0L, 0L, 1L, 0L, 0L, 0L, 0L, 1L, 0L, 0L, 1L, 1L), .Dim = c(4L, 4L), .Dimnames = list(c('Y', 'B', 'V', 'N'), c('B', 'V', 'N', 'V:N')))); .Internal(t.default(argv[[1]]))");
    }

    @Test
    public void testtdefault21() {
        assertEval("argv <- list(-3:5); .Internal(t.default(argv[[1]]))");
    }

    @Test
    public void testtdefault22() {
        assertEval("argv <- list(structure(c(8.3, 8.6, 8.8, 10.5, 10.7, 10.8, 11, 11, 11.1, 11.2, 11.3, 11.4, 11.4, 11.7, 12, 12.9, 12.9, 13.3, 13.7, 13.8, 14, 14.2, 14.5, 16, 16.3, 17.3, 17.5, 17.9, 18, 18, 20.6, 70, 65, 63, 72, 81, 83, 66, 75, 80, 75, 79, 76, 76, 69, 75, 74, 85, 86, 71, 64, 78, 80, 74, 72, 77, 81, 82, 80, 80, 80, 87, 10.3, 10.3, 10.2, 16.4, 18.8, 19.7, 15.6, 18.2, 22.6, 19.9, 24.2, 21, 21.4, 21.3, 19.1, 22.2, 33.8, 27.4, 25.7, 24.9, 34.5, 31.7, 36.3, 38.3, 42.6, 55.4, 55.7, 58.3, 51.5, 51, 77), .Dim = c(31L, 3L), .Dimnames = list(NULL, c('Girth', 'Height', 'Volume')))); .Internal(t.default(argv[[1]]))");
    }

    @Test
    public void testtdefault23() {
        assertEval("argv <- list(structure(list(), .Dim = 0L)); .Internal(t.default(argv[[1]]))");
    }

    @Test
    public void testtdefault24() {
        assertEval("argv <- list(structure('Seed', .Dim = c(1L, 1L))); .Internal(t.default(argv[[1]]))");
    }
}
