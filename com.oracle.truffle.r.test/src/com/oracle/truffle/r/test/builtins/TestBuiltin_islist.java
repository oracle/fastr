/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.*;

import com.oracle.truffle.r.test.*;

// Checkstyle: stop line length check
public class TestBuiltin_islist extends TestBase {

    @Test
    public void testislist1() {
        assertEval("argv <- list(structure(function (e1, e2) standardGeneric('/', .Primitive('/')), generic = structure('/', package = 'base'), package = 'base', group = list('Arith'), valueClass = character(0), signature = c('e1', 'e2'), default = .Primitive('/'), skeleton = quote(.Primitive('/')(e1, e2)), class = structure('standardGeneric', package = 'methods')));is.list(argv[[1]]);");
    }

    @Test
    public void testislist2() {
        assertEval("argv <- list(structure(c(2671, 6.026e+77, 3.161e+152, 3.501e+299, 2.409e+227, 1.529e+302), .Names = c('Min.', '1st Qu.', 'Median', 'Mean', '3rd Qu.', 'Max.')));is.list(argv[[1]]);");
    }

    @Test
    public void testislist3() {
        assertEval("argv <- list('• ');is.list(argv[[1]]);");
    }

    @Test
    public void testislist4() {
        assertEval("argv <- list(13186.6170826564);is.list(argv[[1]]);");
    }

    @Test
    public void testislist5() {
        assertEval("argv <- list(structure(list(object = c(0.568, 1.432, -1.08, 1.08), max.level = NA, vec.len = 4, digits.d = 3, nchar.max = 128, give.attr = TRUE, give.head = TRUE, width = 80L, envir = NULL, strict.width = 'no', formatNum = function (x, ...) format(x, trim = TRUE, drop0trailing = TRUE, ...), list.len = 99, give.length = TRUE, nest.lev = 1, indent.str = '  ..'), .Names = c('object', 'max.level', 'vec.len', 'digits.d', 'nchar.max', 'give.attr', 'give.head', 'width', 'envir', 'strict.width', 'formatNum', 'list.len', 'give.length', 'nest.lev', 'indent.str')));is.list(argv[[1]]);");
    }

    @Test
    public void testislist6() {
        assertEval("argv <- list(structure(c(2L, 1L, 3L), .Label = c('1', '2', NA), class = 'factor'));is.list(argv[[1]]);");
    }

    @Test
    public void testislist7() {
        assertEval("argv <- list(structure(list(y = c(1.08728092481538, 0.0420572471552261, 0.787502161306819, 0.512717751544676, 3.35376639535311, 0.204341510750309, -0.334930602487435, 0.80049208412789, -0.416177803375218, -0.777970346246018, 0.934996808181635, -0.678786709127108, 1.52621589791412, 0.5895781228122, -0.744496121210548, -1.99065153885627, 1.51286447692396, -0.750182409847851), A = c(0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1), U = structure(c(1L, 1L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 2L, 2L, 3L, 3L, 3L, 3L, 3L, 3L), .Label = c('a', 'b', 'c'), class = 'factor'), V = structure(c(1L, 1L, 2L, 2L, 3L, 3L, 1L, 1L, 2L, 2L, 3L, 3L, 1L, 1L, 2L, 2L, 3L, 3L), .Label = c('a', 'b', 'c'), class = 'factor')), .Names = c('y', 'A', 'U', 'V'), terms = quote(y ~ A:U + A:V - 1), row.names = c(NA, 18L), class = 'data.frame'));is.list(argv[[1]]);");
    }

    @Test
    public void testislist8() {
        assertEval("argv <- list(structure(list(title = structure(1L, .Label = c('An Introduction to R', 'Exploratory Data Analysis', 'Interactive Data Analysis', 'LISP-STAT', 'Modern Applied Statistics ...', 'Spatial Statistics', 'Stochastic Simulation'), class = 'factor'), other.author = structure(2L, .Label = c('Ripley', 'Venables & Smith'), class = 'factor')), .Names = c('title', 'other.author'), row.names = 1L, class = 'data.frame'));is.list(argv[[1]]);");
    }

    @Test
    public void testislist9() {
        assertEval("argv <- list(structure(list(srcfile = '/home/lzhao/tmp/RtmptukZK0/R.INSTALL2ccc3a5cba55/nlme/R/lmList.R', frow = 612L, lrow = 612L), .Names = c('srcfile', 'frow', 'lrow'), row.names = c(NA, -1L)));is.list(argv[[1]]);");
    }

    @Test
    public void testislist10() {
        assertEval("argv <- list(structure(3.14159265358979, class = structure('3.14159265358979', class = 'testit')));is.list(argv[[1]]);");
    }

    @Test
    public void testislist11() {
        assertEval("argv <- list(structure(list(a_string = c('foo', 'bar'), a_bool = FALSE, a_struct = structure(list(a = 1, b = structure(c(1, 3, 2, 4), .Dim = c(2L, 2L)), c = 'foo'), .Names = c('a', 'b', 'c')), a_cell = structure(list(1, 'foo', structure(c(1, 3, 2, 4), .Dim = c(2L, 2L)), 'bar'), .Dim = c(2L, 2L)), a_complex_scalar = 0+1i, a_list = list(1, structure(c(1, 3, 2, 4), .Dim = c(2L, 2L)), 'foo'), a_complex_matrix = structure(c(1+2i, 5+0i, 3-4i, -6+0i), .Dim = c(2L, 2L)), a_range = c(1, 2, 3, 4, 5), a_scalar = 1,     a_complex_3_d_array = structure(c(1+1i, 3+1i, 2+1i, 4+1i, 5-1i, 7-1i, 6-1i, 8-1i), .Dim = c(2L, 2L, 2L)), a_3_d_array = structure(c(1, 3, 2, 4, 5, 7, 6, 8), .Dim = c(2L, 2L, 2L)), a_matrix = structure(c(1, 3, 2, 4), .Dim = c(2L, 2L)), a_bool_matrix = structure(c(TRUE, FALSE, FALSE, TRUE), .Dim = c(2L, 2L))), .Names = c('a_string', 'a_bool', 'a_struct', 'a_cell', 'a_complex_scalar', 'a_list', 'a_complex_matrix', 'a_range', 'a_scalar', 'a_complex_3_d_array', 'a_3_d_array', 'a_matrix', 'a_bool_matrix')));is.list(argv[[1]]);");
    }

    @Test
    public void testislist12() {
        assertEval("argv <- list(5e-14);is.list(argv[[1]]);");
    }

    @Test
    public void testislist13() {
        assertEval("argv <- list(structure(c(NA, 6346.2), .Names = c('1', '2')));is.list(argv[[1]]);");
    }

    @Test
    public void testislist14() {
        assertEval("argv <- list(c(1.1+0i, NA, 3+0i));is.list(argv[[1]]);");
    }

    @Test
    public void testislist15() {
        assertEval("argv <- list(structure(list(Ozone = c(NA, NA, NA, NA, NA, NA, 29L, NA, 71L, 39L, NA, NA, 23L, NA, NA, 21L, 37L, 20L, 12L, 13L, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA), Solar.R = c(286L, 287L, 242L, 186L, 220L, 264L, 127L, 273L, 291L, 323L, 259L, 250L, 148L, 332L, 322L, 191L, 284L, 37L, 120L, 137L, 150L, 59L, 91L, 250L, 135L, 127L, 47L, 98L, 31L, 138L), Wind = c(8.6, 9.7, 16.1, 9.2, 8.6, 14.3, 9.7, 6.9, 13.8, 11.5, 10.9, 9.2, 8, 13.8, 11.5, 14.9, 20.7, 9.2, 11.5, 10.3, 6.3, 1.7, 4.6, 6.3, 8, 8, 10.3, 11.5, 14.9, 8), Temp = c(78L, 74L, 67L, 84L, 85L, 79L, 82L, 87L, 90L, 87L, 93L, 92L, 82L, 80L, 79L, 77L, 72L, 65L, 73L, 76L, 77L, 76L, 76L, 76L, 75L, 78L, 73L, 80L, 77L, 83L), Month = c(6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L), Day = 1:30), .Names = c('Ozone', 'Solar.R', 'Wind', 'Temp', 'Month', 'Day'), row.names = 32:61, class = 'data.frame'));is.list(argv[[1]]);");
    }

    @Test
    public void testislist16() {
        assertEval("argv <- list(structure(c(1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4), .Tsp = c(1945, 1974.75, 4), class = 'ts'));is.list(argv[[1]]);");
    }

    @Test
    public void testislist17() {
        assertEval("argv <- list(structure(c('***', '***', '*', '*'), legend = '0 ‘***’ 0.001 ‘**’ 0.01 ‘*’ 0.05 ‘.’ 0.1 ‘ ’ 1', class = 'noquote'));is.list(argv[[1]]);");
    }

    @Test
    public void testislist18() {
        assertEval("argv <- list(structure(list(`/home/lzhao/tmp/Rtmpe5iuYI/R.INSTALL2aa854a74188/foreign/man/lookup.xport.Rd` = structure(c('read.xport', ''), .Dim = 1:2, .Dimnames = list(NULL, c('Target', 'Anchor'))), `/home/lzhao/tmp/Rtmpe5iuYI/R.INSTALL2aa854a74188/foreign/man/read.S.Rd` = structure(character(0), .Dim = c(0L, 2L), .Dimnames = list(NULL, c('Target', 'Anchor'))), `/home/lzhao/tmp/Rtmpe5iuYI/R.INSTALL2aa854a74188/foreign/man/read.arff.Rd` = structure(c('connection', 'write.arff', '', ''), .Dim = c(2L, 2L), .Dimnames = list(    NULL, c('Target', 'Anchor'))), `/home/lzhao/tmp/Rtmpe5iuYI/R.INSTALL2aa854a74188/foreign/man/read.dbf.Rd` = structure(c('make.names', 'write.dbf', '', ''), .Dim = c(2L, 2L), .Dimnames = list(NULL, c('Target', 'Anchor'))), `/home/lzhao/tmp/Rtmpe5iuYI/R.INSTALL2aa854a74188/foreign/man/read.dta.Rd` = structure(c('write.dta', 'attributes', 'Date', 'factor', '', '', '', ''), .Dim = c(4L, 2L), .Dimnames = list(NULL, c('Target', 'Anchor'))), `/home/lzhao/tmp/Rtmpe5iuYI/R.INSTALL2aa854a74188/foreign/man/read.epiinfo.Rd` = structure(c('Date', 'DateTimeClasses', '', ''), .Dim = c(2L, 2L), .Dimnames = list(NULL, c('Target', 'Anchor'))), `/home/lzhao/tmp/Rtmpe5iuYI/R.INSTALL2aa854a74188/foreign/man/read.mtp.Rd` = structure(character(0), .Dim = c(0L, 2L), .Dimnames = list(NULL, c('Target', 'Anchor'))), `/home/lzhao/tmp/Rtmpe5iuYI/R.INSTALL2aa854a74188/foreign/man/read.octave.Rd` = structure(character(0), .Dim = c(0L, 2L), .Dimnames = list(NULL, c('Target', 'Anchor'))), `/home/lzhao/tmp/Rtmpe5iuYI/R.INSTALL2aa854a74188/foreign/man/read.spss.Rd` = structure(c('sub', 'iconv', 'iconvlist', '', '', ''), .Dim = c(3L, 2L), .Dimnames = list(NULL, c('Target', 'Anchor'))), `/home/lzhao/tmp/Rtmpe5iuYI/R.INSTALL2aa854a74188/foreign/man/read.ssd.Rd` = structure(c('read.xport', ''), .Dim = 1:2, .Dimnames = list(NULL, c('Target', 'Anchor'))), `/home/lzhao/tmp/Rtmpe5iuYI/R.INSTALL2aa854a74188/foreign/man/read.systat.Rd` = structure(character(0), .Dim = c(0L, 2L), .Dimnames = list(NULL, c('Target', 'Anchor'))), `/home/lzhao/tmp/Rtmpe5iuYI/R.INSTALL2aa854a74188/foreign/man/read.xport.Rd` = structure(c('lookup.xport', ''), .Dim = 1:2, .Dimnames = list(NULL, c('Target', 'Anchor'))), `/home/lzhao/tmp/Rtmpe5iuYI/R.INSTALL2aa854a74188/foreign/man/write.arff.Rd` = structure(c('make.names', 'read.arff', '', ''), .Dim = c(2L, 2L), .Dimnames = list(NULL, c('Target', 'Anchor'))), `/home/lzhao/tmp/Rtmpe5iuYI/R.INSTALL2aa854a74188/foreign/man/write.dbf.Rd` = structure(c('read.dbf', ''), .Dim = 1:2, .Dimnames = list(NULL, c('Target', 'Anchor'))), `/home/lzhao/tmp/Rtmpe5iuYI/R.INSTALL2aa854a74188/foreign/man/write.dta.Rd` = structure(c('drop', 'read.dta', 'attributes', 'DateTimeClasses', 'abbreviate', '', '', '', '', ''), .Dim = c(5L, 2L), .Dimnames = list(NULL, c('Target', 'Anchor'))), `/home/lzhao/tmp/Rtmpe5iuYI/R.INSTALL2aa854a74188/foreign/man/write.foreign.Rd` = structure(character(0), .Dim = c(0L, 2L), .Dimnames = list(NULL, c('Target', 'Anchor')))), .Names = c('/home/lzhao/tmp/Rtmpe5iuYI/R.INSTALL2aa854a74188/foreign/man/lookup.xport.Rd', '/home/lzhao/tmp/Rtmpe5iuYI/R.INSTALL2aa854a74188/foreign/man/read.S.Rd', '/home/lzhao/tmp/Rtmpe5iuYI/R.INSTALL2aa854a74188/foreign/man/read.arff.Rd', '/home/lzhao/tmp/Rtmpe5iuYI/R.INSTALL2aa854a74188/foreign/man/read.dbf.Rd', '/home/lzhao/tmp/Rtmpe5iuYI/R.INSTALL2aa854a74188/foreign/man/read.dta.Rd', '/home/lzhao/tmp/Rtmpe5iuYI/R.INSTALL2aa854a74188/foreign/man/read.epiinfo.Rd', '/home/lzhao/tmp/Rtmpe5iuYI/R.INSTALL2aa854a74188/foreign/man/read.mtp.Rd', '/home/lzhao/tmp/Rtmpe5iuYI/R.INSTALL2aa854a74188/foreign/man/read.octave.Rd', '/home/lzhao/tmp/Rtmpe5iuYI/R.INSTALL2aa854a74188/foreign/man/read.spss.Rd', '/home/lzhao/tmp/Rtmpe5iuYI/R.INSTALL2aa854a74188/foreign/man/read.ssd.Rd', '/home/lzhao/tmp/Rtmpe5iuYI/R.INSTALL2aa854a74188/foreign/man/read.systat.Rd', '/home/lzhao/tmp/Rtmpe5iuYI/R.INSTALL2aa854a74188/foreign/man/read.xport.Rd', '/home/lzhao/tmp/Rtmpe5iuYI/R.INSTALL2aa854a74188/foreign/man/write.arff.Rd', '/home/lzhao/tmp/Rtmpe5iuYI/R.INSTALL2aa854a74188/foreign/man/write.dbf.Rd', '/home/lzhao/tmp/Rtmpe5iuYI/R.INSTALL2aa854a74188/foreign/man/write.dta.Rd', '/home/lzhao/tmp/Rtmpe5iuYI/R.INSTALL2aa854a74188/foreign/man/write.foreign.Rd')));is.list(argv[[1]]);");
    }

    @Test
    public void testislist19() {
        assertEval("argv <- list(3.14159265358979e+20);is.list(argv[[1]]);");
    }

    @Test
    public void testislist20() {
        assertEval("argv <- list(structure(list(srcfile = c(NA, '/home/lzhao/tmp/RtmpYl9n1I/R.INSTALL2aa24b6697e5/MASS/R/rlm.R', '/home/lzhao/tmp/RtmpYl9n1I/R.INSTALL2aa24b6697e5/MASS/R/rlm.R', '/home/lzhao/tmp/RtmpYl9n1I/R.INSTALL2aa24b6697e5/MASS/R/rlm.R', '/home/lzhao/tmp/RtmpYl9n1I/R.INSTALL2aa24b6697e5/MASS/R/rlm.R', '/home/lzhao/tmp/RtmpYl9n1I/R.INSTALL2aa24b6697e5/MASS/R/rlm.R', '/home/lzhao/tmp/RtmpYl9n1I/R.INSTALL2aa24b6697e5/MASS/R/rlm.R', '/home/lzhao/tmp/RtmpYl9n1I/R.INSTALL2aa24b6697e5/MASS/R/rlm.R', '/home/lzhao/tmp/RtmpYl9n1I/R.INSTALL2aa24b6697e5/MASS/R/rlm.R', '/home/lzhao/tmp/RtmpYl9n1I/R.INSTALL2aa24b6697e5/MASS/R/rlm.R', '/home/lzhao/tmp/RtmpYl9n1I/R.INSTALL2aa24b6697e5/MASS/R/rlm.R', '/home/lzhao/tmp/RtmpYl9n1I/R.INSTALL2aa24b6697e5/MASS/R/rlm.R', '/home/lzhao/tmp/RtmpYl9n1I/R.INSTALL2aa24b6697e5/MASS/R/rlm.R', '/home/lzhao/tmp/RtmpYl9n1I/R.INSTALL2aa24b6697e5/MASS/R/rlm.R', '/home/lzhao/tmp/RtmpYl9n1I/R.INSTALL2aa24b6697e5/MASS/R/rlm.R', '/home/lzhao/tmp/RtmpYl9n1I/R.INSTALL2aa24b6697e5/MASS/R/rlm.R', '/home/lzhao/tmp/RtmpYl9n1I/R.INSTALL2aa24b6697e5/MASS/R/rlm.R', '/home/lzhao/tmp/RtmpYl9n1I/R.INSTALL2aa24b6697e5/MASS/R/rlm.R', '/home/lzhao/tmp/RtmpYl9n1I/R.INSTALL2aa24b6697e5/MASS/R/rlm.R', '/home/lzhao/tmp/RtmpYl9n1I/R.INSTALL2aa24b6697e5/MASS/R/rlm.R', '/home/lzhao/tmp/RtmpYl9n1I/R.INSTALL2aa24b6697e5/MASS/R/rlm.R', '/home/lzhao/tmp/RtmpYl9n1I/R.INSTALL2aa24b6697e5/MASS/R/rlm.R', '/home/lzhao/tmp/RtmpYl9n1I/R.INSTALL2aa24b6697e5/MASS/R/rlm.R', '/home/lzhao/tmp/RtmpYl9n1I/R.INSTALL2aa24b6697e5/MASS/R/rlm.R', '/home/lzhao/tmp/RtmpYl9n1I/R.INSTALL2aa24b6697e5/MASS/R/rlm.R', '/home/lzhao/tmp/RtmpYl9n1I/R.INSTALL2aa24b6697e5/MASS/R/rlm.R'), frow = c(NA, 88L, 89L, 89L, 90L, 90L, 90L, 88L, 88L, 92L, 92L, 92L, 92L, 92L, 94L, 94L, 100L, 103L, 108L, 108L, 128L, 131L, 138L, 142L, 160L, 160L), lrow = c(NA, 91L, 89L, 89L, 90L, 90L, 90L, 91L, 91L, 93L, 93L, 93L, 93L, 93L, 95L, 95L, 100L, 104L, 108L, 108L, 132L, 131L, 138L, 144L, 160L, 160L)), .Names = c('srcfile', 'frow', 'lrow'), row.names = c(NA, 26L)));is.list(argv[[1]]);");
    }

    @Test
    public void testislist21() {
        assertEval("argv <- list(structure(list(srcfile = c('/home/lzhao/tmp/Rtmpe5iuYI/R.INSTALL2aa854a74188/foreign/R/R_systat.R', '/home/lzhao/tmp/Rtmpe5iuYI/R.INSTALL2aa854a74188/foreign/R/R_systat.R'), frow = 21:22, lrow = 21:22), .Names = c('srcfile', 'frow', 'lrow'), row.names = 1:2));is.list(argv[[1]]);");
    }

    @Test
    public void testislist22() {
        assertEval("argv <- list(structure(c(1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 4L, 4L, 4L, 4L, 4L, 4L, 4L, 4L, NA, NA, NA, NA, NA), .Label = c('[0,2)', '[2,4)', '[4,6)', '[6,8)'), class = 'factor'));is.list(argv[[1]]);");
    }

    @Test
    public void testislist23() {
        assertEval("argv <- list(structure(c(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10), .Dim = c(10L, 2L), .Dimnames = list(NULL, c('x', 'y'))));is.list(argv[[1]]);");
    }

    @Test
    public void testislist24() {
        assertEval("argv <- list(structure(list(loc = c(0.0804034870161223, 10.3548347412639), cov = structure(c(3.01119301965569, 6.14320559215603, 6.14320559215603, 14.7924762275451), .Dim = c(2L, 2L)), d2 = 2, wt = c(0, 0, 0, 0, 0, 1.75368801162502e-134, 0, 0, 0, 2.60477585273833e-251, 1.16485035372295e-260, 0, 1.53160350210786e-322, 0.333331382328728, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3.44161262707711e-123, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1.968811545398e-173, 0, 8.2359965384697e-150, 0, 0, 0, 0, 6.51733217171341e-10, 0, 2.36840184577368e-67, 0, 9.4348408357524e-307, 0, 1.59959906013771e-89, 0, 8.73836857865034e-286, 7.09716190970992e-54, 0, 0, 0, 1.530425353017e-274, 8.57590058044551e-14, 0.333333106397154, 0, 0, 1.36895217898448e-199, 2.0226102635783e-177, 5.50445388209462e-42, 0, 0, 0, 0, 1.07846402051283e-44, 1.88605464411243e-186, 1.09156111051203e-26, 0, 3.0702877273237e-124, 0.333333209689785, 0, 0, 0, 0, 0, 0, 3.09816093866831e-94, 0, 0, 4.7522727332095e-272, 0, 0, 2.30093251441394e-06, 0, 0, 1.27082826644707e-274, 0, 0, 0, 0, 0, 0, 0, 4.5662025456054e-65, 0, 2.77995853978268e-149, 0, 0, 0), sqdist = c(0.439364946869246, 0.0143172566761092, 0.783644692619938, 0.766252947443554, 0.346865912102713, 1.41583192825661, 0.168485512965902, 0.354299830956879, 0.0943280426627965, 1.05001058449122, 1.02875556201707, 0.229332323173361, 0.873263925064789, 2.00000009960498, 0.449304354954282, 0.155023307933165, 0.118273979375253, 0.361693898800799, 0.21462398586105, 0.155558909016629, 0.471723661454506, 0.719528696331092, 0.0738164380664225, 1.46001193111051, 0.140785322548143, 0.127761195166703, 0.048012401156175, 0.811750426884519, 0.425827709817574, 0.163016638545231, 0.557810866640707, 0.277350147637843, 0.0781399119055092, 1.29559183995835, 0.718376405567138, 1.37650242941478, 0.175087780508154, 0.233808973148729, 0.693473805463067, 0.189096604125073, 1.96893781800017, 0.4759756980592, 1.69665760380474, 0.277965749373647, 0.920525436884815, 0.57525234053591, 1.59389578665009, 0.175715364671313, 0.972045794851437, 1.75514684962809, 0.0597413185507202, 0.174340343040626, 0.143421553552865, 0.997322770596838, 1.94096736957465, 2.00000001159796, 0.367000821772989, 0.682474530588235, 1.20976163307984, 1.27031685239035, 1.79775635513363, 0.0857761902860323, 0.435578932929501, 0.214370604878221, 0.494714247412686, 1.78784623754399, 1.24216674083069, 1.87749485326709, 0.0533296334123023, 1.45588362584438, 2.00000000631459, 0.208857144738039, 0.119251291573058, 0.365303924649962, 0.690656674239668, 0.0396958405786268, 0.258262120876164, 1.57360254057537, 0.307548421049514, 0.628417063100241, 1.00647098749202, 0.297624360530352, 0.400289147351669, 1.98298426250944, 0.129127182829694, 0.0794695319493149, 0.991481735944321, 0.444068154119836, 0.206790162395106, 0.574310829851377, 0.181887577583334, 0.433872021297517, 0.802994892604009, 0.293053770941001, 1.7002969001965, 0.77984639982848, 1.36127407487932, 0.761935213110323, 0.597915313430067, 0.237134831067472), prob = NULL, tol = 1e-07, eps = 9.96049758228423e-08, it = 898L, maxit = 5000,     ierr = 0L, conv = TRUE), .Names = c('loc', 'cov', 'd2', 'wt', 'sqdist', 'prob', 'tol', 'eps', 'it', 'maxit', 'ierr', 'conv'), class = 'ellipsoid'));is.list(argv[[1]]);");
    }

    @Test
    public void testislist25() {
        assertEval("argv <- list(structure(c(NA, 1, 1, 2), .Names = c('<none>', 'M.user', 'Temp', 'Soft')));is.list(argv[[1]]);");
    }

    @Test
    public void testislist26() {
        assertEval("argv <- list(structure(list(y = c(-0.0561287395290008, -0.155795506705329, -1.47075238389927, -0.47815005510862, 0.417941560199702, 1.35867955152904, -0.102787727342996, 0.387671611559369, -0.0538050405829051, -1.37705955682861), x = c(TRUE, FALSE, TRUE, FALSE, TRUE, FALSE, TRUE, FALSE, TRUE, FALSE), z = 1:10), .Names = c('y', 'x', 'z'), terms = quote(y ~ x * z), row.names = c(NA, 10L), class = 'data.frame'));is.list(argv[[1]]);");
    }

    @Test
    public void testislist27() {
        assertEval("argv <- list(structure(c(1920, 1920, 1920, 1920, 1920, 1920, 1921, 1921, 1921, 1921), .Tsp = c(1920.5, 1921.25, 12), class = 'ts'));is.list(argv[[1]]);");
    }

    @Test
    public void testislist29() {
        assertEval("argv <- list(structure(1, .Dim = 1L));is.list(argv[[1]]);");
    }

    @Test
    public void testislist30() {
        assertEval("argv <- list(structure(list(Employed = c(60.323, 61.122, 60.171, 61.187, 63.221, 63.639, 64.989, 63.761, 66.019, 67.857, 68.169, 66.513, 68.655, 69.564, 69.331, 70.551), GNP.deflator = c(83, 88.5, 88.2, 89.5, 96.2, 98.1, 99, 100, 101.2, 104.6, 108.4, 110.8, 112.6, 114.2, 115.7, 116.9), GNP = c(234.289, 259.426, 258.054, 284.599, 328.975, 346.999, 365.385, 363.112, 397.469, 419.18, 442.769, 444.546, 482.704, 502.601, 518.173, 554.894), Unemployed = c(235.6, 232.5, 368.2, 335.1, 209.9, 193.2, 187, 357.8, 290.4, 282.2, 293.6, 468.1, 381.3, 393.1, 480.6, 400.7), Armed.Forces = c(159, 145.6, 161.6, 165, 309.9, 359.4, 354.7, 335, 304.8, 285.7, 279.8, 263.7, 255.2, 251.4, 257.2, 282.7), Population = c(107.608, 108.632, 109.773, 110.929, 112.075, 113.27, 115.094, 116.219, 117.388, 118.734, 120.445, 121.95, 123.366, 125.368, 127.852, 130.081), Year = 1947:1962), .Names = c('Employed', 'GNP.deflator', 'GNP', 'Unemployed', 'Armed.Forces', 'Population', 'Year'), terms = quote(Employed ~ GNP.deflator + GNP + Unemployed +     Armed.Forces + Population + Year), row.names = 1947:1962, class = 'data.frame'));is.list(argv[[1]]);");
    }

    @Test
    public void testislist31() {
        assertEval("argv <- list(structure(list(onefile = TRUE, family = 'Helvetica',     title = 'R Graphics Output', fonts = NULL, encoding = 'default',     bg = 'transparent', fg = 'black', width = 0, height = 0,     horizontal = TRUE, pointsize = 12, paper = 'default', pagecentre = TRUE,     print.it = FALSE, command = 'default', colormodel = 'srgb',     useKerning = TRUE, fillOddEven = FALSE), .Names = c('onefile',     'family', 'title', 'fonts', 'encoding', 'bg', 'fg', 'width',     'height', 'horizontal', 'pointsize', 'paper', 'pagecentre',     'print.it', 'command', 'colormodel', 'useKerning', 'fillOddEven')));"
                        + "do.call('is.list', argv)");
    }

}
