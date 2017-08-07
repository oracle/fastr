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
public class TestBuiltin_ismatrix extends TestBase {

    @Test
    public void testismatrix1() {
        assertEval("argv <- list(c(-3.44, 62.44));is.matrix(argv[[1]]);");
    }

    @Test
    public void testismatrix2() {
        assertEval("argv <- list(structure(list(surname = structure(2L, .Label = c('McNeil', 'R Core', 'Ripley', 'Tierney', 'Tukey', 'Venables'), class = 'factor'), nationality = structure(NA_integer_, .Label = c('Australia', 'UK', 'US'), class = 'factor'), deceased = structure(NA_integer_, .Label = c('no', 'yes'), class = 'factor')), .Names = c('surname', 'nationality', 'deceased'), row.names = 1L, class = 'data.frame'));is.matrix(argv[[1]]);");
    }

    @Test
    public void testismatrix3() {
        assertEval("argv <- list(structure(list(visible = c(FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE), from = structure(c(1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L), .Label = 'registered S3method for summary', class = 'factor')), .Names = c('visible', 'from'), row.names = c('summary.aspell', 'summary.ecdf', 'summary.loess', 'summary.nls', 'summary.packageStatus', 'summary.PDF_Dictionary', 'summary.PDF_Stream', 'summary.ppr', 'summary.prcomp', 'summary.princomp', 'summary.stl', 'summary.tukeysmooth'), class = 'data.frame'));is.matrix(argv[[1]]);");
    }

    @Test
    public void testismatrix4() {
        assertEval("argv <- list(structure(c(0, 87, 82, 75, 63, 50, 43, 32, 35, 60, 54, 55, 36, 39, 0, 0, 69, 57, 57, 51, 45, 37, 46, 39, 36, 24, 32, 23, 25, 32, 0, 32, 59, 74, 75, 60, 71, 61, 71, 57, 71, 68, 79, 73, 76, 71, 67, 75, 79, 62, 63, 57, 60, 49, 48, 52, 57, 62, 61, 66, 71, 62, 61, 57, 72, 83, 71, 78, 79, 71, 62, 74, 76, 64, 62, 57, 80, 73, 69, 69, 71, 64, 69, 62, 63, 46, 56, 44, 44, 52, 38, 46, 36, 49, 35, 44, 59, 65, 65, 56, 66, 53, 61, 52, 51, 48, 54, 49, 49, 61, 0, 0, 68, 44, 40, 27, 28, 25, 24, 24), .Tsp = c(1945, 1974.75, 4), class = 'ts'));is.matrix(argv[[1]]);");
    }

    @Test
    public void testismatrix5() {
        assertEval("argv <- list(structure(list(srcfile = c('/home/lzhao/tmp/Rtmpe5iuYI/R.INSTALL2aa854a74188/foreign/R/spss.R', '/home/lzhao/tmp/Rtmpe5iuYI/R.INSTALL2aa854a74188/foreign/R/spss.R', '/home/lzhao/tmp/Rtmpe5iuYI/R.INSTALL2aa854a74188/foreign/R/spss.R', '/home/lzhao/tmp/Rtmpe5iuYI/R.INSTALL2aa854a74188/foreign/R/spss.R', '/home/lzhao/tmp/Rtmpe5iuYI/R.INSTALL2aa854a74188/foreign/R/spss.R', '/home/lzhao/tmp/Rtmpe5iuYI/R.INSTALL2aa854a74188/foreign/R/spss.R', '/home/lzhao/tmp/Rtmpe5iuYI/R.INSTALL2aa854a74188/foreign/R/spss.R', '/home/lzhao/tmp/Rtmpe5iuYI/R.INSTALL2aa854a74188/foreign/R/spss.R', '/home/lzhao/tmp/Rtmpe5iuYI/R.INSTALL2aa854a74188/foreign/R/spss.R', '/home/lzhao/tmp/Rtmpe5iuYI/R.INSTALL2aa854a74188/foreign/R/spss.R', '/home/lzhao/tmp/Rtmpe5iuYI/R.INSTALL2aa854a74188/foreign/R/spss.R', '/home/lzhao/tmp/Rtmpe5iuYI/R.INSTALL2aa854a74188/foreign/R/spss.R', '/home/lzhao/tmp/Rtmpe5iuYI/R.INSTALL2aa854a74188/foreign/R/spss.R', '/home/lzhao/tmp/Rtmpe5iuYI/R.INSTALL2aa854a74188/foreign/R/spss.R', '/home/lzhao/tmp/Rtmpe5iuYI/R.INSTALL2aa854a74188/foreign/R/spss.R', '/home/lzhao/tmp/Rtmpe5iuYI/R.INSTALL2aa854a74188/foreign/R/spss.R', '/home/lzhao/tmp/Rtmpe5iuYI/R.INSTALL2aa854a74188/foreign/R/spss.R', '/home/lzhao/tmp/Rtmpe5iuYI/R.INSTALL2aa854a74188/foreign/R/spss.R', '/home/lzhao/tmp/Rtmpe5iuYI/R.INSTALL2aa854a74188/foreign/R/spss.R', '/home/lzhao/tmp/Rtmpe5iuYI/R.INSTALL2aa854a74188/foreign/R/spss.R', '/home/lzhao/tmp/Rtmpe5iuYI/R.INSTALL2aa854a74188/foreign/R/spss.R', '/home/lzhao/tmp/Rtmpe5iuYI/R.INSTALL2aa854a74188/foreign/R/spss.R', '/home/lzhao/tmp/Rtmpe5iuYI/R.INSTALL2aa854a74188/foreign/R/spss.R', '/home/lzhao/tmp/Rtmpe5iuYI/R.INSTALL2aa854a74188/foreign/R/spss.R', '/home/lzhao/tmp/Rtmpe5iuYI/R.INSTALL2aa854a74188/foreign/R/spss.R', '/home/lzhao/tmp/Rtmpe5iuYI/R.INSTALL2aa854a74188/foreign/R/spss.R', '/home/lzhao/tmp/Rtmpe5iuYI/R.INSTALL2aa854a74188/foreign/R/spss.R', '/home/lzhao/tmp/Rtmpe5iuYI/R.INSTALL2aa854a74188/foreign/R/spss.R', '/home/lzhao/tmp/Rtmpe5iuYI/R.INSTALL2aa854a74188/foreign/R/spss.R'), frow = c(112L, 114L, 115L, 116L, 127L, 130L, 130L, 130L, 133L, 133L, 133L, 136L, 136L, 136L, 140L, 140L, 140L, 143L, 143L, 143L, 147L, 147L, 147L, 147L, 150L, 150L, 150L, 155L, 161L), lrow = c(156L, 114L, 115L, 116L, 127L, 130L, 130L, 130L, 133L, 133L, 133L, 136L, 136L, 136L, 140L, 140L, 140L, 143L, 143L, 143L, 147L, 147L, 147L, 147L, 150L, 150L, 150L, 155L, 178L)), .Names = c('srcfile', 'frow', 'lrow'), row.names = c(NA, 29L), class = 'data.frame'));is.matrix(argv[[1]]);");
    }

    @Test
    public void testismatrix6() {
        assertEval("argv <- list(structure(list(a_string = c('foo', 'bar'), a_bool = FALSE, a_struct = structure(list(a = 1, b = structure(c(1, 3, 2, 4), .Dim = c(2L, 2L)), c = 'foo'), .Names = c('a', 'b', 'c')), a_cell = structure(list(1, 'foo', structure(c(1, 3, 2, 4), .Dim = c(2L, 2L)), 'bar'), .Dim = c(2L, 2L)), a_complex_scalar = 0+1i, a_list = list(1, structure(c(1, 3, 2, 4), .Dim = c(2L, 2L)), 'foo'), a_complex_matrix = structure(c(1+2i, 5+0i, 3-4i, -6+0i), .Dim = c(2L, 2L)), a_range = c(1, 2, 3, 4, 5), a_scalar = 1,     a_complex_3_d_array = structure(c(1+1i, 3+1i, 2+1i, 4+1i, 5-1i, 7-1i, 6-1i, 8-1i), .Dim = c(2L, 2L, 2L)), a_3_d_array = structure(c(1, 3, 2, 4, 5, 7, 6, 8), .Dim = c(2L, 2L, 2L)), a_matrix = structure(c(1, 3, 2, 4), .Dim = c(2L, 2L)), a_bool_matrix = structure(c(TRUE, FALSE, FALSE, TRUE), .Dim = c(2L, 2L))), .Names = c('a_string', 'a_bool', 'a_struct', 'a_cell', 'a_complex_scalar', 'a_list', 'a_complex_matrix', 'a_range', 'a_scalar', 'a_complex_3_d_array', 'a_3_d_array', 'a_matrix', 'a_bool_matrix')));is.matrix(argv[[1]]);");
    }

    @Test
    public void testismatrix7() {
        assertEval("argv <- list(c(1.2e+100, 1.3e+100));is.matrix(argv[[1]]);");
    }

    @Test
    public void testismatrix8() {
        assertEval("argv <- list(structure(c(1.46658790096676e-05, -0.00015671726259936, -4.04552045434325e-05, 0.00255024941225984, -0.00162786181391528, 8.23090637551149e-05, -0.00015671726259936, 3.72287793770631e-05, 0.000886372801540247, -0.0567316142279179, 0.0349990628241952, -0.00175223081612341, -4.04552045434325e-05, 0.000886372801540247, 2.56091878967357e-05, -0.000729189497559513, -0.000975857105361189, 4.86109322531125e-05, 0.00255024941225984, -0.0567316142279179, -0.000729189497559513, 0.000230331183246113, 0.0612339887096517, -0.00297447704687248, -0.00162786181391528, 0.0349990628241952, -0.000975857105361189, 0.0612339887096517, -1.91064691608123e-05, -0.000246257006748074, 8.23090637551149e-05, -0.00175223081612341, 4.86109322531125e-05, -0.00297447704687248, -0.000246257006748074, 2.51870808007926e-05), .Dim = c(6L, 6L), .Dimnames = list(c('v1', 'v2', 'v3', 'v4', 'v5', 'v6'), c('v1', 'v2', 'v3', 'v4', 'v5', 'v6'))));is.matrix(argv[[1]]);");
    }

    @Test
    public void testismatrix9() {
        assertEval("argv <- list(structure(list(Topic = c('myTst-package', 'foo-class', 'myTst', 'show,foo-method', 'show,foo-method', 'show-methods'), File = c('myTst-package', 'foo-class', 'myTst-package', 'foo-class', 'show-methods', 'show-methods')), .Names = c('Topic', 'File'), row.names = c(3L, 1L, 4L, 2L, 6L, 5L), class = 'data.frame'));is.matrix(argv[[1]]);");
    }

    @Test
    public void testismatrix10() {
        assertEval("argv <- list(structure(numeric(0), .Dim = c(25L, 0L)));is.matrix(argv[[1]]);");
    }

    @Test
    public void testismatrix11() {
        assertEval("argv <- list(structure(list(srcfile = c('/home/lzhao/tmp/Rtmpe5iuYI/R.INSTALL2aa854a74188/foreign/R/arff.R', '/home/lzhao/tmp/Rtmpe5iuYI/R.INSTALL2aa854a74188/foreign/R/arff.R'), frow = c(86L, 86L), lrow = c(88L, 88L)), .Names = c('srcfile', 'frow', 'lrow'), row.names = 1:2, class = 'data.frame'));is.matrix(argv[[1]]);");
    }

    @Test
    public void testismatrix12() {
        assertEval("argv <- list(structure(c(-15.8396536770559, 0.267020886067525, -10.0516337591148, 7.62751967585832), .Dim = c(2L, 2L), .Dimnames = list(c('1', '3'), c('(Intercept)', 'TempLow'))));is.matrix(argv[[1]]);");
    }

    @Test
    public void testismatrix13() {
        assertEval("argv <- list(structure(list(srcfile = c(NA, '/home/lzhao/hg/r-instrumented/library/stats/R/stats', '/home/lzhao/hg/r-instrumented/library/stats/R/stats'), frow = c(NA, 16987L, 16991L), lrow = c(NA, 16987L, 16991L)), .Names = c('srcfile', 'frow', 'lrow'), row.names = c(NA, 3L), class = 'data.frame'));is.matrix(argv[[1]]);");
    }

    @Test
    public void testismatrix14() {
        assertEval("argv <- list(structure(list(V1 = c(NA, 2, NA, 4, 5), V2 = c(NA, NA, 3, 4, 5)), .Names = c('V1', 'V2'), class = 'data.frame', row.names = c(NA, -5L)));is.matrix(argv[[1]]);");
    }

    @Test
    public void testismatrix15() {
        assertEval("argv <- list(structure(c(0, 0, 0, 0, 0, 56.989995924654, 56.989995924654, 94.3649041101607, 94.3649041101607, 94.3649041101607, 94.3649041101607, 94.3649041101607, 94.3649041101607, 109.608811230383, 109.608811230383, 109.608811230383, 107.478028232287, 107.478028232287, 107.478028232287, 107.478028232287, 94.6057793667664, 94.6057793667664, 94.6057793667664, 94.6057793667664, 94.6057793667664, 94.6057793667664, 76.6771074226725, 76.6771074226725, 76.6771074226725, 76.6771074226725, 76.6771074226725, 76.6771074226725, 76.6771074226725, 76.6771074226725, 76.6771074226725, 57.5975949121373, 57.5975949121373, 57.5975949121373, 57.5975949121373, 57.5975949121373, 57.5975949121373, 57.5975949121373, 57.5975949121373, 57.5975949121373, 57.5975949121373, 39.6403646307366, 39.6403646307366, 39.6403646307366, 39.6403646307366, 39.6403646307366, 10.7055301785859, 0, 1.00000000551046, 1.00000000551046, 1.00000000551046, 1.00000000551046, 1.00000000551046, 0.914597467778369, 0.914597467778369, 0.764820801027804, 0.764820801027804, 0.764820801027804, 0.764820801027804, 0.764820801027804, 0.764820801027804, 0.599195286063472, 0.599195286063472, 0.599195286063472, 0.446659102876937, 0.446659102876937, 0.446659102876937, 0.446659102876937, 0.319471715663991, 0.319471715663991, 0.319471715663991, 0.319471715663991, 0.319471715663991, 0.319471715663991, 0.21965732107982, 0.21965732107982, 0.21965732107982, 0.21965732107982, 0.21965732107982, 0.21965732107982, 0.21965732107982, 0.21965732107982, 0.21965732107982, 0.144322069921372, 0.144322069921372, 0.144322069921372, 0.144322069921372, 0.144322069921372, 0.144322069921372, 0.144322069921372, 0.144322069921372, 0.144322069921372, 0.144322069921372, 0.0889140940358009, 0.0889140940358009, 0.0889140940358009, 0.0889140940358009, 0.0889140940358009, 0.0202635232425103, 2.60032456603692e-08, 0, 0, 0, 0, 0, 0.165626203544259, 0.165626203544259, 0.341691261149167, 0.341691261149167, 0.341691261149167, 0.341691261149167, 0.341691261149167, 0.341691261149167, 0.503396799290371, 0.503396799290371, 0.503396799290371, 0.638987326722699, 0.638987326722699, 0.638987326722699, 0.638987326722699, 0.746106779008021, 0.746106779008021, 0.746106779008021, 0.746106779008021, 0.746106779008021, 0.746106779008021, 0.827421615259225, 0.827421615259225, 0.827421615259225, 0.827421615259225, 0.827421615259225, 0.827421615259225, 0.827421615259225, 0.827421615259225, 0.827421615259225, 0.887496120452751, 0.887496120452751, 0.887496120452751, 0.887496120452751, 0.887496120452751, 0.887496120452751, 0.887496120452751, 0.887496120452751, 0.887496120452751, 0.887496120452751, 0.931061257482989, 0.931061257482989, 0.931061257482989, 0.931061257482989, 0.931061257482989, 0.984387422945875, 0.999999996451695), .Dim = c(52L, 3L)));is.matrix(argv[[1]]);");
    }

    @Test
    public void testismatrix16() {
        assertEval("argv <- list(structure(list(Df = c(NA, 1), Deviance = c(12.2441566485997, 32.825622681839), AIC = c(73.9421143635373, 92.5235803967766)), .Names = c('Df', 'Deviance', 'AIC'), row.names = c('<none>', '- M.user'), class = c('anova', 'data.frame'), heading = c('Single term deletions', '\\nModel:', 'cbind(X, M) ~ M.user')));is.matrix(argv[[1]]);");
    }

    @Test
    public void testismatrix17() {
        assertEval("argv <- list(structure(list(V1 = 1L, V2 = structure(1L, .Label = c('A', 'D', 'E'), class = 'factor'), V3 = 6), .Names = c('V1', 'V2', 'V3'), class = 'data.frame', row.names = c(NA, -1L)));is.matrix(argv[[1]]);");
    }

    @Test
    public void testismatrix18() {
        assertEval("argv <- list(structure(logical(0), .Dim = c(10L, 0L)));is.matrix(argv[[1]]);");
    }

    @Test
    public void testismatrix19() {
        assertEval("argv <- list(c(-1.12778377684043, -12820.0784261145, -21650982809.6744, -473300382255715392, -6.08456909882282e+25, -3.04622557026196e+34, -4.60125024792566e+43, -1.76183826972506e+53, -1.5069799345972e+63, -2.61556777274611e+73, -8.54170618068872e+83, -4.9383857330861e+94, -4.80716085942859e+105, -7.55412056676629e+116, -1.84898368353639e+128, -6.83535188151783e+139, -3.71562599613334e+151, -2.90089508183654e+163, -3.18582547396557e+175, -4.83110332887119e+187, -9.94902790498679e+199, -2.74100158340596e+212, -9.96611412047338e+224, -4.72336572671053e+237, -2.88514442494869e+250, -2.24780296109123e+263, -2.21240023126594e+276, -2.72671165723473e+289, -4.17369555651928e+302, -Inf, -Inf, -Inf, -Inf, -Inf, -Inf, -Inf, -Inf, -Inf, -Inf, -Inf, -Inf, -Inf, -Inf, -Inf, -Inf, -Inf, -Inf, -Inf, -Inf, -Inf, -Inf));is.matrix(argv[[1]]);");
    }

    @Test
    public void testismatrix20() {
        assertEval("argv <- list(structure(c(-15.7116658409483, 0.267197739695975, -7.51681521806951, 7.8485143735526), .Dim = c(2L, 2L), .Dimnames = list(c('1', '3'), c('(Intercept)', 'M.userY'))));is.matrix(argv[[1]]);");
    }

    @Test
    public void testismatrix21() {
        assertEval("argv <- list(structure(c(NA, NA, NA, NA), .Dim = c(1L, 4L), .Dimnames = list('x', c('Estimate', 'Std. Error', 't value', 'Pr(>|t|)'))));is.matrix(argv[[1]]);");
    }

    @Test
    public void testismatrix23() {
        assertEval("argv <- list(0.0597289453377495);do.call('is.matrix', argv)");
    }

    @Test
    public void testismatrixGenericDispatch() {
        assertEval("{ is.matrix.cls <- function(x) 42; is.matrix(structure(c(1,2), class='cls')); }");
    }
}
