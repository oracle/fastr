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

import org.junit.*;

import com.oracle.truffle.r.test.*;

// Checkstyle: stop line length check
public class TestBuiltin_asvector extends TestBase {

    @Test
    public void testasvector1() {
        assertEval("argv <- list('ylog', 'list'); .Internal(as.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasvector2() {
        assertEval("argv <- list(structure(character(0), package = character(0), class = structure('ObjectsWithPackage', package = 'methods')), 'character'); .Internal(as.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasvector3() {
        assertEval(Ignored.Unknown, "argv <- list(quote(list(ya, x[rep.int(NA_integer_, nyy), nm.x, drop = FALSE])), 'list'); .Internal(as.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasvector4() {
        assertEval("argv <- list(NA_character_, 'any'); .Internal(as.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasvector5() {
        assertEval(Ignored.Unknown, "argv <- list(structure(NA_integer_, .Label = c('Australia', 'UK', 'US'), class = 'factor'), 'any'); .Internal(as.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasvector6() {
        assertEval("argv <- list(structure(list(a1 = 1:3, a2 = 4:6, a3 = 3.14159265358979, a4 = c('a', 'b', 'c')), .Names = c('a1', 'a2', 'a3', 'a4')), 'any'); .Internal(as.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasvector7() {
        assertEval("argv <- list(quote(list(ii = 1:10, xx = pi * -3:6)), 'list'); .Internal(as.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasvector8() {
        assertEval("argv <- list(c(-1L, -2L), 'any'); .Internal(as.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasvector9() {
        assertEval("argv <- list(quote(list(x = 1:100, z = 1:100 + rnorm(100, 10))), 'list'); .Internal(as.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasvector10() {
        assertEval("argv <- list(structure(c(FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE), .Names = c('#ifdef', '\\\\Sexpr', 'build', 'install', 'render', '#ifdef', '\\\\Sexpr', 'build', 'install', 'render', '#ifdef', '\\\\Sexpr', 'build', 'install', 'render', '#ifdef', '\\\\Sexpr', 'build', 'install', 'render', '#ifdef', '\\\\Sexpr', 'build', 'install', 'render', '#ifdef', '\\\\Sexpr', 'build', 'install', 'render', '#ifdef', '\\\\Sexpr', 'build', 'install', 'render', '#ifdef', '\\\\Sexpr', 'build', 'install', 'render', '#ifdef', '\\\\Sexpr', 'build', 'install', 'render', '#ifdef', '\\\\Sexpr', 'build', 'install', 'render', '#ifdef', '\\\\Sexpr', 'build', 'install', 'render', '#ifdef', '\\\\Sexpr', 'build', 'install', 'render', '#ifdef', '\\\\Sexpr', 'build', 'install', 'render', '#ifdef', '\\\\Sexpr', 'build', 'install', 'render', '#ifdef', '\\\\Sexpr', 'build', 'install', 'render', '#ifdef', '\\\\Sexpr', 'build', 'install', 'render', '#ifdef', '\\\\Sexpr', 'build', 'install', 'render', '#ifdef', '\\\\Sexpr', 'build', 'install', 'render', '#ifdef', '\\\\Sexpr', 'build', 'install', 'render', '#ifdef', '\\\\Sexpr', 'build', 'install', 'render', '#ifdef', '\\\\Sexpr', 'build', 'install', 'render', '#ifdef', '\\\\Sexpr', 'build', 'install', 'render', '#ifdef', '\\\\Sexpr', 'build', 'install', 'render', '#ifdef', '\\\\Sexpr', 'build', 'install', 'render', '#ifdef', '\\\\Sexpr', 'build', 'install', 'render', '#ifdef', '\\\\Sexpr', 'build', 'install', 'render', '#ifdef', '\\\\Sexpr', 'build', 'install', 'render', '#ifdef', '\\\\Sexpr', 'build', 'install', 'render', '#ifdef', '\\\\Sexpr', 'build', 'install', 'render', '#ifdef', '\\\\Sexpr', 'build', 'install', 'render', '#ifdef', '\\\\Sexpr', 'build', 'install', 'render', '#ifdef', '\\\\Sexpr', 'build', 'install', 'render', '#ifdef', '\\\\Sexpr', 'build', 'install', 'render', '#ifdef', '\\\\Sexpr', 'build', 'install', 'render', '#ifdef', '\\\\Sexpr', 'build', 'install', 'render', '#ifdef', '\\\\Sexpr', 'build', 'install', 'render', '#ifdef', '\\\\Sexpr', 'build', 'install', 'render', '#ifdef', '\\\\Sexpr', 'build', 'install', 'render', '#ifdef', '\\\\Sexpr', 'build', 'install', 'render', '#ifdef', '\\\\Sexpr', 'build', 'install', 'render', '#ifdef', '\\\\Sexpr', 'build', 'install', 'render', '#ifdef', '\\\\Sexpr', 'build', 'install', 'render', '#ifdef', '\\\\Sexpr', 'build', 'install', 'render', '#ifdef', '\\\\Sexpr', 'build', 'install', 'render', '#ifdef', '\\\\Sexpr', 'build', 'install', 'render', '#ifdef', '\\\\Sexpr', 'build', 'install', 'render', '#ifdef', '\\\\Sexpr', 'build', 'install', 'render', '#ifdef', '\\\\Sexpr', 'build', 'install', 'render', '#ifdef', '\\\\Sexpr', 'build', 'install', 'render', '#ifdef', '\\\\Sexpr', 'build', 'install', 'render', '#ifdef', '\\\\Sexpr', 'build', 'install', 'render', '#ifdef', '\\\\Sexpr', 'build', 'install', 'render')), 'any'); .Internal(as.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasvector11() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(c(0.00290239468554411, 0.00140705152597282, 0.00182415100508824, 0.000171517300342798, 0.0747454613066297, 0.00103234723292905, 0.000179983318697139, 0.035258608446556, 0.00336847595628205, 0.0640696486471418, 0.0132108002751951, 0.00194778778741288, 0.00351950115137134, 0.00070046832029645, 0.00252844357734999, 0.014372012195495, 0.00923422554274329, 7.64817786749709e-06, 0.00387339857745524, 0.00121246491006704, 0.00624917129689857, 0.00187753034805145, 0.000103002251547081, 0.0136703020254034, 0.000349542811339765, 0.00120367047056318, 0.00194205014408536, 0.00462815827742801, 0.000149291834133955, 0.00193441236645678, 9.00084520363788e-05, 0.0160915134527436, 0.00346675958538611, 0.00481936427422656, 3.13343033856204e-05, 0.0564685345533007, 0.00929771993193244, 0.0103876340982415, 0.0133005891226512, 0.0325989357511189, 0.00228122925969392, 0.0460976655088242, 0.00300363745967821, 0.000271060875811077, 0.0301696315261026, 4.72002631048228e-05, 0.0262321004865233, 0.00594174673473013, 0.00288915040856096, 0.00635277836091399, 0.00569342819072193, 0.0163907345734163, 0.000360581939026215, 0.00023772587191537, 0.0164062036225435, 0.0238391417439454, NaN, 0.0421542087325977, 0.00133954856768466, 0.0113421570571088, 0.0081824228772913, 0.000149291834133955, 0.00162069399881579, 0.0018026229128858, 0.0043164627226381, 0.000407784303899559, 0.00876301280354452, 0.00179253664026376, 0.000416739394150718, 0.014372012195495, 0.000179983318697139, 0.00115986529332945, 0.00377736311314377, 0.00219491136307178, 0.00070046832029645, 0.000522557531637993, 9.86336244510646e-05, 0.0216346027446621, 0.000659639144027202, 0.0137501462695058, 5.91425796335962e-08, 0.0279425064631674, 0.000170828237014775, 0.0042454690355613, 0.0114879015536739, 0.000173346990819198, 0.00138111062254461, 0.00772582941114727, 0.0277947034678616, 0.00892024547056825, 0.0618577709874562, 0.0125790610228498, 0.0277947034678616), .Names = c('1', '2', '3', '4', '5', '6', '7', '8', '9', '10', '11', '12', '13', '14', '15', '16', '17', '18', '19', '20', '21', '22', '23', '24', '25', '26', '27', '28', '29', '30', '31', '32', '33', '34', '35', '36', '37', '38', '39', '40', '41', '42', '43', '44', '45', '46', '47', '48', '49', '50', '51', '52', '53', '54', '55', '56', '57', '58', '59', '60', '61', '62', '63', '64', '65', '66', '67', '68', '69', '70', '71', '72', '73', '74', '75', '76', '77', '78', '79', '80', '81', '82', '83', '84', '85', '86', '87', '88', '89', '90', '91', '92', '93')), 'any'); .Internal(as.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasvector12() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(c(0.164593338447767, 0.182090654313858, NA, 0.484947927602608), .Names = c('(Intercept)', 'x1', 'x2', 'x3')), 'any'); .Internal(as.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasvector13() {
        assertEval("argv <- list('', 'double'); .Internal(as.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasvector14() {
        assertEval("argv <- list(structure(c('myTst', 'Package', 'What the package does (short line)', '1.0', '2014-03-17', 'Who wrote it', 'Who to complain to <yourfault@somewhere.net>', 'More about what it does (maybe more than one line)', 'What license is it under?', 'methods'), .Names = c('Package', 'Type', 'Title', 'Version', 'Date', 'Author', 'Maintainer', 'Description', 'License', 'Depends')), 'list'); .Internal(as.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasvector15() {
        assertEval(Ignored.Unknown, "argv <- list(quote(sqrt(abs(`Standardized residuals`))), 'expression'); .Internal(as.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasvector16() {
        assertEval("argv <- list(1, 'any'); .Internal(as.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasvector17() {
        assertEval("argv <- list(quote(list(X[[2L]])), 'list'); .Internal(as.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasvector18() {
        assertEval("argv <- list(NA, 'logical'); .Internal(as.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasvector19() {
        assertEval(Ignored.Unknown, "argv <- list(NULL, 'double'); .Internal(as.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasvector20() {
        assertEval(Ignored.Unknown,
                        "argv <- list(quote(list(x = c(1, 1, 1, 1, 1, 2, 2, 3, 3, 3, 3, 3, 3, 4, 4, 4, 5, 5, 5, 5, 6, 6, 6, 6, 6, 6, 7, 7, 7, 7, 7, 7, 7, 7, 7, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 9, 9, 9, 9, 9, 11, 12), y = c(73, 73, 70, 74, 75, 115, 105, 107, 124, 107, 116, 125, 102, 144, 178, 149, 177, 124, 157, 128, 169, 165, 186, 152, 181, 139, 173, 151, 138, 181, 152, 188, 173, 196, 180, 171, 188, 174, 198, 172, 176, 162, 188, 182, 182, 141, 191, 190, 159, 170, 163, 197), weight = c(1, rep(0.1, 51)))), 'list'); .Internal(as.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasvector21() {
        assertEval(Ignored.Unknown, "argv <- list(NULL, 'integer'); .Internal(as.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasvector22() {
        assertEval("argv <- list(quote(list(ff <- factor(c(1:2, NA, 2), exclude = NULL))), 'list'); .Internal(as.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasvector23() {
        assertEval("argv <- list(c(-1, 3, 1, 1, 5, 1), 'any'); .Internal(as.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasvector24() {
        assertEval("argv <- list(quote(list(y, x1, x2)), 'list'); .Internal(as.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasvector25() {
        assertEval("argv <- list(structure(c(0.005, 50, 550), .Names = c('k', 'g1', 'g2')), 'list'); .Internal(as.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasvector26() {
        assertEval(Ignored.Unknown, "argv <- list(quote(list(V1 = c('a', 'd e', 'h'), V2 = c('b'', 'f', 'i'), V3 = c('c', 'g', 'j\\nk l m'))), 'list'); .Internal(as.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasvector27() {
        assertEval("argv <- list(NA, 'integer'); .Internal(as.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasvector28() {
        assertEval("argv <- list(c(NA, NaN), 'character'); .Internal(as.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasvector29() {
        assertEval("argv <- list(c(NA, NaN), 'integer'); .Internal(as.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasvector30() {
        assertEval(Ignored.Unknown, "argv <- list(list('a', 'b', 'c'), 'pairlist'); .Internal(as.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasvector31() {
        assertEval("argv <- list(structure(1:12, .Dim = 3:4, .Dimnames = list(c('A', 'B', 'C'), c('D', 'E', 'F', 'G'))), 'any'); .Internal(as.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasvector32() {
        assertEval("argv <- list(quote(list(x = c(2:3, NA), y = c(3:4, NA))), 'list'); .Internal(as.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasvector33() {
        assertEval("argv <- list(quote(list(cut(Dtimes, '3 months'))), 'list'); .Internal(as.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasvector34() {
        assertEval("argv <- list(quote(list(a = I('abc'), b = I('def\\\'gh'))), 'list'); .Internal(as.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasvector35() {
        assertEval("argv <- list(structure(list(a = 1), .Names = 'a'), 'double'); .Internal(as.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasvector36() {
        assertEval("argv <- list(structure(c(0, 0.0123079727211562, 0.00970882237374837, 0.62883302403078, 0.689843718945119, 0.689843718944881, 0.672453157851573, 0.534493702379921, 0.171039529097608, 0.17103952909345, 0.50219835346871, 0.530975095958163, 0.0050966004562048, 0.0106639382954144, 0.811192712625201, 0.0957932531337699), .Names = c('(Intercept)', 'M.userY', 'TempHigh', 'M.userY:TempHigh', 'SoftMedium', 'SoftSoft', 'M.userY:SoftMedium', 'M.userY:SoftSoft', 'TempHigh:SoftMedium', 'TempHigh:SoftSoft', 'M.userY:TempHigh:SoftMedium', 'M.userY:TempHigh:SoftSoft', 'BrandM', 'M.userY:BrandM', 'TempHigh:BrandM', 'M.userY:TempHigh:BrandM')), 'any'); .Internal(as.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasvector37() {
        assertEval("argv <- list(c(-2, -1.95959595959596, -1.91919191919192, -1.87878787878788, -1.83838383838384, -1.7979797979798, -1.75757575757576, -1.71717171717172, -1.67676767676768, -1.63636363636364, -1.5959595959596, -1.55555555555556, -1.51515151515152, -1.47474747474747, -1.43434343434343, -1.39393939393939, -1.35353535353535, -1.31313131313131, -1.27272727272727, -1.23232323232323, -1.19191919191919, -1.15151515151515, -1.11111111111111, -1.07070707070707, -1.03030303030303, -0.98989898989899, -0.949494949494949, -0.909090909090909, -0.868686868686869, -0.828282828282828, -0.787878787878788, -0.747474747474747, -0.707070707070707, -0.666666666666667, -0.626262626262626, -0.585858585858586, -0.545454545454545, -0.505050505050505, -0.464646464646465, -0.424242424242424, -0.383838383838384, -0.343434343434343, -0.303030303030303, -0.262626262626263, -0.222222222222222, -0.181818181818182, -0.141414141414141, -0.101010101010101, -0.0606060606060606, -0.0202020202020201, 0.0202020202020203, 0.060606060606061, 0.101010101010101, 0.141414141414141, 0.181818181818182, 0.222222222222222, 0.262626262626263, 0.303030303030303, 0.343434343434343, 0.383838383838384, 0.424242424242424, 0.464646464646465, 0.505050505050505, 0.545454545454546, 0.585858585858586, 0.626262626262626, 0.666666666666667, 0.707070707070707, 0.747474747474748, 0.787878787878788, 0.828282828282829, 0.868686868686869, 0.909090909090909, 0.94949494949495, 0.98989898989899, 1.03030303030303, 1.07070707070707, 1.11111111111111, 1.15151515151515, 1.19191919191919, 1.23232323232323, 1.27272727272727, 1.31313131313131, 1.35353535353535, 1.39393939393939, 1.43434343434343, 1.47474747474747, 1.51515151515152, 1.55555555555556, 1.5959595959596, 1.63636363636364, 1.67676767676768, 1.71717171717172, 1.75757575757576, 1.7979797979798, 1.83838383838384, 1.87878787878788, 1.91919191919192, 1.95959595959596, 2), 'any'); .Internal(as.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasvector38() {
        assertEval(Ignored.Unknown, "argv <- list(integer(0), 'pairlist'); .Internal(as.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasvector39() {
        assertEval("argv <- list(structure('lightblue', .Names = 'bg'), 'list'); .Internal(as.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasvector40() {
        assertEval("argv <- list(c(NA, NaN), 'logical'); .Internal(as.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasvector41() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(list(`character(0)` = structure(integer(0), .Label = character(0), class = 'factor')), .Names = 'character(0)', row.names = character(0), class = 'data.frame'), 'pairlist'); .Internal(as.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasvector42() {
        assertEval("argv <- list(NA, 'double'); .Internal(as.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasvector43() {
        assertEval("argv <- list(list('GRID.VP.12'), 'list'); .Internal(as.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasvector44() {
        assertEval(Ignored.Unknown, "argv <- list(NULL, 'logical'); .Internal(as.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasvector45() {
        assertEval("argv <- list(structure(1:20, .Tsp = c(1, 20, 1), class = 'ts'), 'any'); .Internal(as.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasvector47() {
        assertEval("argv <- list(structure(c(0.1, 0.8, 1, 0.5, 0.8, 1, 0, 0.5, 1), .Dim = c(3L, 3L), .Dimnames = list(c('(3.59,4.5]', '(4.5,5.4]', '(5.4,6.31]'), c('ctrl', 'trt1', 'trt2'))), 'any'); .Internal(as.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasvector48() {
        assertEval("argv <- list(integer(0), 'list'); .Internal(as.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasvector49() {
        assertEval("argv <- list(structure(c(1L, 1L), .Label = 'registered S3method for $', class = 'factor'), 'any'); .Internal(as.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasvector50() {
        assertEval("argv <- list('1.3', 'double'); .Internal(as.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasvector51() {
        assertEval("argv <- list(c(8L, 11L, 14L, 16L, 19L, 4L, 6L, 9L, 15L, NA, 7L, 10L, 20L), 'any'); .Internal(as.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasvector52() {
        assertEval(Ignored.Unknown, "argv <- list(structure(c(5.4278733372119e-07, 0.000257866433233453, NA), .Names = c('x', 'm', 'Residuals')), 'any'); .Internal(as.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasvector53() {
        assertEval(Ignored.Unknown, "argv <- list('1.3', 'pairlist'); .Internal(as.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasvector54() {
        assertEval(Ignored.Unknown, "argv <- list(1L, 'pairlist'); .Internal(as.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasvector55() {
        assertEval("argv <- list(NULL, 'any'); .Internal(as.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasvector56() {
        assertEval("argv <- list(quote(list(expand.grid(Hair = lab$Hair, Eye = lab$Eye, Sex = lab$Sex, stringsAsFactors = TRUE), Fr = as.vector(HairEyeColor))), 'list'); .Internal(as.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasvector57() {
        assertEval(Ignored.Unknown, "argv <- list(FALSE, 'pairlist'); .Internal(as.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasvector59() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(list(`character(0)` = structure(integer(0), .Label = character(0), class = 'factor')), .Names = 'character(0)', row.names = character(0), class = 'data.frame'), 'character'); .Internal(as.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasvector60() {
        assertEval("argv <- list(1L, 'list'); .Internal(as.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasvector61() {
        assertEval(Ignored.Unknown,
                        "argv <- list(c('The C and R code has been reformatted for legibility.', 'The old compatibility function rpconvert() has been removed.', 'The cross-validation functions allow for user interrupt at the end\\nof evaluating each split.', 'Variable Reliability in data set car90 is corrected to be an\\nordered factor, as documented.', 'Surrogate splits are now considered only if they send two or more\\ncases _with non-zero weight_ each way.  For numeric/ordinal\\nvariables the restriction to non-zero weights is new: for\\ncategorical variables this is a new restriction.', 'Surrogate splits which improve only by rounding error over the\\ndefault split are no longer returned.  Where weights and missing\\nvalues are present, the splits component for some of these was not\\nreturned correctly.', 'A fit of class \\\'rpart\\\' now contains a component for variable\\n‘importance’, which is reported by the summary() method.', 'The text() method gains a minlength argument, like the labels()\\nmethod.  This adds finer control: the default remains pretty =\\nNULL, minlength = 1L.', 'The handling of fits with zero and fractional weights has been\\ncorrected: the results may be slightly different (or even\\nsubstantially different when the proportion of zero weights is\\nlarge).', 'Some memory leaks have been plugged.', 'There is a second vignette, longintro.Rnw, a version of the\\noriginal Mayo Tecnical Report on rpart.', 'Added dataset car90, a corrected version of the S-PLUS dataset\\ncar.all (used with permission).', 'This version does not use paste0{} and so works with R 2.14.x.', 'Merged in a set of Splus code changes that had accumulated at Mayo\\nover the course of a decade. The primary one is a change in how\\nindexing is done in the underlying C code, which leads to a major\\nspeed increase for large data sets.  Essentially, for the lower\\nleaves all our time used to be eaten up by bookkeeping, and this\\nwas replaced by a different approach.  The primary routine also\\nuses .Call{} so as to be more memory efficient.', 'The other major change was an error for asymmetric loss matrices,\\nprompted by a user query.  With L=loss asymmetric, the altered\\npriors were computed incorrectly - they were using L' instead of L.\\nUpshot - the tree would not not necessarily choose optimal splits\\nfor the given loss matrix.  Once chosen, splits were evaluated\\ncorrectly.  The printed “improvement” values are of course the\\nwrong ones as well.  It is interesting that for my little test\\ncase, with L quite asymmetric, the early splits in the tree are\\nunchanged - a good split still looks good.', 'Add the return.all argument to xpred.rpart().', 'Added a set of formal tests, i.e., cases with known answers to\\nwhich we can compare.', 'Add a usercode vignette, explaining how to add user defined\\nsplitting functions.', 'The class method now also returns the node probability.', 'Add the stagec data set, used in some tests.', 'The plot.rpart routine needs to store a value that will be visible\\nto the rpartco routine at a later time.  This is now done in an\\nenvironment in the namespace.', 'Force use of registered symbols in R >= 2.16.0', 'Update Polish translations.', 'Work on message formats.', 'Add Polish translations', 'rpart, rpart.matrix: allow backticks in formulae.', 'tests/backtick.R: regession test', 'src/xval.c: ensure unused code is not compiled in.', 'Change description of margin in ?plot.rpart as suggested by Bill\\nVenables.'), 'any'); .Internal(as.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasvector63() {
        assertEval("argv <- list(2, 'list'); .Internal(as.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasvector64() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(list(c0 = structure(integer(0), .Label = character(0), class = 'factor')), .Names = 'c0', row.names = character(0), class = structure('integer(0)', .Names = 'c0')), 'any'); .Internal(as.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasvector66() {
        assertEval(Ignored.Unknown, "argv <- list(3.18309886183776e-301, 'any'); .Internal(as.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasvector67() {
        assertEval("argv <- list(quote(list(a = 1:3, b = letters[1:3])), 'list'); .Internal(as.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasvector68() {
        assertEval("argv <- list(NA, 'list'); .Internal(as.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasvector69() {
        assertEval(Ignored.Unknown, "argv <- list(c(200, 500, 1000, 2000, 5000, 10000, 20000, 50000, 1e+05, 2e+05, 5e+05), 'any'); .Internal(as.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasvector70() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(c(-0.560475646552213+0i, 0.7424437487+0.205661411508856i, 1.39139505579429-0.26763356813179i, 0.928710764113827-0.221714979045717i, -0.46926798541295+1.18846175213664i, 0.7424437487-0.205661411508856i, 0.460916205989202+0i, -0.452623703774585+0.170604003753717i, -0.094501186832143+0.54302538277632i, -0.331818442379127+0.612232958468282i, 1.39139505579429+0.26763356813179i, -0.452623703774585-0.170604003753717i, 0.400771450594052+0i, -0.927967220342259+0.479716843914174i, -0.790922791530657+0.043092176305418i, 0.928710764113827+0.221714979045717i, -0.094501186832143-0.54302538277632i, -0.927967220342259-0.479716843914174i, 0.701355901563686+0i, -0.600841318509537+0.213998439984336i, -0.46926798541295-1.18846175213664i, -0.331818442379127-0.612232958468282i, -0.790922791530657-0.043092176305418i, -0.600841318509537-0.213998439984336i, -0.625039267849257+0i), .Dim = c(5L, 5L)), 'any'); .Internal(as.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasvector71() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(c(2.2250738585072e-308, 1.79769313486232e+308), .Names = c('double.xmin', 'double.xmax')), 'any'); .Internal(as.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasvector72() {
        assertEval("argv <- list(structure(1.6, class = 'object_size'), 'character'); .Internal(as.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasvector73() {
        assertEval(Ignored.Unknown, "argv <- list(structure(NA_integer_, .Label = c('no', 'yes'), class = 'factor'), 'any'); .Internal(as.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasvector74() {
        assertEval("argv <- list(FALSE, 'character'); .Internal(as.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasvector75() {
        assertEval(Ignored.Unknown, "argv <- list(3.14159265358979, 'pairlist'); .Internal(as.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasvector76() {
        assertEval("argv <- list(structure(list(c0 = structure(character(0), class = 'AsIs')), .Names = 'c0', row.names = character(0), class = 'data.frame'), 'character'); .Internal(as.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasvector77() {
        assertEval("argv <- list(structure(list(), .Dim = 0L), 'any'); .Internal(as.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasvector80() {
        assertEval("argv <- list(structure('1', .Tsp = c(1, 1, 1), class = 'ts'), 'character'); .Internal(as.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasvector81() {
        assertEval("argv <- list('diff', 'symbol'); .Internal(as.vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testAsVector() {
        assertEval(Output.ContainsWarning, "{ as.vector(\"foo\", \"integer\") }");
        assertEval(Output.ContainsWarning, "{ as.vector(\"foo\", \"double\") }");
        assertEval(Output.ContainsWarning, "{ as.vector(\"foo\", \"numeric\") }");
        assertEval("{ as.vector(\"foo\", \"logical\") }");
        assertEval(Output.ContainsWarning, "{ as.vector(\"foo\", \"raw\") }");
        assertEval("{ as.vector(\"foo\", \"character\") }");
        assertEval("{ as.vector(\"foo\", \"list\") }");
        assertEval("{ as.vector(\"foo\") }");
        assertEval(Output.ContainsError, "{ as.vector(\"foo\", \"bar\") }");
        assertEval(Output.ContainsWarning, "{ as.vector(c(\"foo\", \"bar\"), \"raw\") }");
        assertEval("x<-c(a=1.1, b=2.2); as.vector(x, \"raw\")");
        assertEval("x<-c(a=1L, b=2L); as.vector(x, \"complex\")");
        assertEval("{ x<-c(a=FALSE, b=TRUE); attr(x, \"foo\")<-\"foo\"; y<-as.vector(x); attributes(y) }");
        assertEval("{ x<-c(a=1, b=2); as.vector(x, \"list\") }");
        assertEval("{ x<-c(a=FALSE, b=TRUE); attr(x, \"foo\")<-\"foo\"; y<-as.vector(x, \"list\"); attributes(y) }");
        assertEval("{ x<-1:4; dim(x)<-c(2, 2); dimnames(x)<-list(c(\"a\", \"b\"), c(\"c\", \"d\")); y<-as.vector(x, \"list\"); y }");

        assertEval("{ as.vector(NULL, \"list\") }");
        assertEval("{ as.vector(NULL) }");

        assertEval("{ x<-factor(c(\"a\", \"b\", \"a\")); as.vector(x) }");
    }

    @Test
    public void testAsSymbol() {
        assertEval("{ as.symbol(\"name\") }");
        assertEval("{ as.symbol(123) }");
        assertEval("{ as.symbol(as.symbol(123)) }");
    }
}
