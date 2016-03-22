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

import java.util.Arrays;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check
public class TestBuiltin_c extends TestBase {

    @Test
    public void testc1() {
        assertEval("argv <- list(character(0), 'myLib/myTst');c(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testc2() {
        assertEval("argv <- list(structure(list(names = c('x', 'z')), .Names = 'names'), structure(list(class = 'data.frame', row.names = c(NA, 10L)), .Names = c('class', 'row.names')));c(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testc3() {
        assertEval(Ignored.Unknown, "argv <- list(0.1, 1e+60);c(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testc4() {
        assertEval("argv <- list(1, 1, 1, 1, NA);c(argv[[1]],argv[[2]],argv[[3]],argv[[4]],argv[[5]]);");
    }

    @Test
    public void testc5() {
        assertEval("argv <- list(`difference in location` = -30);c(argv[[1]]);");
    }

    @Test
    public void testc6() {
        assertEval("argv <- list(TRUE, TRUE, NA);c(argv[[1]],argv[[2]],argv[[3]]);");
    }

    @Test
    public void testc7() {
        assertEval("argv <- list(expression(data.frame), list(), check.names = TRUE, stringsAsFactors = TRUE);c(argv[[1]],argv[[2]],argv[[3]],argv[[4]]);");
    }

    @Test
    public void testc8() {
        assertEval("argv <- list(-0.1, 0.1);c(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testc9() {
        assertEval(Ignored.Unknown,
                        "argv <- list(NULL, structure(list(class = 'try-error', condition = structure(list(message = 'more columns than column names', call = quote(read.table('foo6', header = TRUE))), .Names = c('message', 'call'), class = c('simpleError', 'error', 'condition'))), .Names = c('class', 'condition')));c(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testc10() {
        assertEval(Ignored.Unknown,
                        "argv <- list(NULL, structure(list(class = 'try-error', condition = structure(list(message = 'supply both 'x' and 'y' or a matrix-like 'x'', call = quote(cor(rnorm(10), NULL))), .Names = c('message', 'call'), class = c('simpleError', 'error', 'condition'))), .Names = c('class', 'condition')));c(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testc11() {
        assertEval("argv <- list(`(Intercept)` = '(Intercept)', structure(list(B = 'B', V = 'V', N = 'N', `V:N` = c('V', 'N'), Residuals = c('B', 'V', 'N', 'Within')), .Names = c('B', 'V', 'N', 'V:N', 'Residuals')));c(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testc12() {
        assertEval("argv <- list(structure(c(512, 313, 89, 19, 353, 207, 17, 8, 120, 205, 202, 391, 138, 279, 131, 244, 53, 138, 94, 299, 22, 351, 24, 317), .Dim = c(2L, 2L, 6L), .Dimnames = structure(list(Admit = c('Admitted', 'Rejected'), Gender = c('Male', 'Female'), Dept = c('A', 'B', 'C', 'D', 'E', 'F')), .Names = c('Admit', 'Gender', 'Dept')), class = 'table'));c(argv[[1]]);");
    }

    @Test
    public void testc13() {
        assertEval("argv <- list(structure(1208822400, class = c('POSIXct', 'POSIXt')), structure(1209168000, class = c('POSIXct', 'POSIXt')));c(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testc14() {
        assertEval(Ignored.Unknown,
                        "argv <- list(`Grand mean` = structure(103.87323943662, class = 'mtable'), structure(list(N = structure(c(78.7365206866197, 98.5088731171753, 113.842206450509, 123.008873117175), .Dim = 4L, .Dimnames = structure(list(N = c('0.0cwt', '0.2cwt', '0.4cwt', '0.6cwt')), .Names = 'N'), class = 'mtable'), `V:N` = structure(c(79.5323303457107, 86.1989970123773, 69.7732394366197, 98.0323303457106, 108.032330345711, 89.1989970123773, 114.198997012377, 116.698997012377, 110.365663679044, 124.365663679044, 126.365663679044, 118.032330345711), .Dim = 3:4, .Dimnames = structure(list(V = c('Golden.rain', 'Marvellous', 'Victory'), N = c('0.0cwt', '0.2cwt', '0.4cwt', '0.6cwt')), .Names = c('V', 'N')), class = 'mtable')), .Names = c('N', 'V:N')));c(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testc15() {
        assertEval(Ignored.Unknown,
                        "argv <- list(NULL, structure(list(class = 'try-error', condition = structure(list(message = '(converted from warning) NAs produced', call = quote(rexp(2, numeric()))), .Names = c('message', 'call'), class = c('simpleError', 'error', 'condition'))), .Names = c('class', 'condition')));c(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testc16() {
        assertEval("argv <- list(NULL, structure(list(other = structure(1:3, .Label = c('A', 'B', 'C'), class = 'factor')), .Names = 'other'));c(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testc17() {
        assertEval("argv <- list(logical(0), structure(1:10, .Tsp = c(1920.5, 1921.25, 12), class = 'ts'), logical(0));c(argv[[1]],argv[[2]],argv[[3]]);");
    }

    @Test
    public void testc18() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(list(V1 = c(1L, 1L, 2L, 3L), V2 = structure(c(1L, 1L, 2L, 3L), .Label = c('A', 'D', 'E'), class = 'factor'), V3 = c(6, 6, 9, 10)), .Names = c('V1', 'V2', 'V3'), row.names = c(NA, 4L), class = 'data.frame'), sep = '\\r');c(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testc19() {
        assertEval("argv <- list(10L, NULL, 10);c(argv[[1]],argv[[2]],argv[[3]]);");
    }

    @Test
    public void testc20() {
        assertEval(Ignored.Unknown,
                        "argv <- list(NULL, structure(list(class = 'try-error', condition = structure(list(message = 'non-numeric argument to mathematical function', call = quote(log('a'))), .Names = c('message', 'call'), class = c('simpleError', 'error', 'condition'))), .Names = c('class', 'condition')));c(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testc21() {
        assertEval(Ignored.Unknown,
                        "argv <- list(NULL, structure(list(class = 'try-error', condition = structure(list(message = \''x' is empty', call = quote(cor(Z[, FALSE], use = 'pairwise.complete.obs', method = 'kendall'))), .Names = c('message', 'call'), class = c('simpleError', 'error', 'condition'))), .Names = c('class', 'condition')));c(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testc22() {
        assertEval("argv <- list(structure(list(N = structure(c(17, 18, 18, 18), .Dim = 4L, .Dimnames = structure(list(N = c('0.0cwt', '0.2cwt', '0.4cwt', '0.6cwt')), .Names = 'N'))), .Names = 'N'), structure(list(`V:N` = structure(c(6, 6, 5, 6, 6, 6, 6, 6, 6, 6, 6, 6), .Dim = 3:4, .Dimnames = structure(list(V = c('Golden.rain', 'Marvellous', 'Victory'), N = c('0.0cwt', '0.2cwt', '0.4cwt', '0.6cwt')), .Names = c('V', 'N')))), .Names = 'V:N'));c(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testc23() {
        assertEval("argv <- list(list(NULL), list(NULL, c('a', 'b', 'c'), NULL, c('V5', 'V6', 'V7', 'V8', 'V9')));c(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testc24() {
        assertEval(Ignored.Unknown,
                        "argv <- list(NULL, structure(list(class = 'try-error', condition = structure(list(message = 'undefined columns selected', call = quote(`[.data.frame`(dd, , 'C'))), .Names = c('message', 'call'), class = c('simpleError', 'error', 'condition'))), .Names = c('class', 'condition')));c(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testc25() {
        assertEval(Ignored.Unknown,
                        "argv <- list(NULL, structure(list(class = 'try-error', condition = structure(list(message = 'line 1 did not have 4 elements', call = quote(scan(file, what, nmax, sep, dec, quote, skip, nlines, na.strings, flush, fill, strip.white, quiet, blank.lines.skip, multi.line, comment.char, allowEscapes, encoding))), .Names = c('message', 'call'), class = c('simpleError', 'error', 'condition'))), .Names = c('class', 'condition')));c(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testc26() {
        assertEval("argv <- list(0, c(FALSE, FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE, FALSE));c(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testc27() {
        assertEval("argv <- list(character(0));c(argv[[1]]);");
    }

    @Test
    public void testc28() {
        assertEval(Ignored.Unknown,
                        "argv <- list(NULL, structure(list(class = 'try-error', condition = structure(list(message = '(converted from warning) NAs produced', call = quote(rnorm(1, sd = Inf))), .Names = c('message', 'call'), class = c('simpleError', 'error', 'condition'))), .Names = c('class', 'condition')));c(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testc29() {
        assertEval("argv <- list(1944, 1944.75, 4);c(argv[[1]],argv[[2]],argv[[3]]);");
    }

    @Test
    public void testc30() {
        assertEval("argv <- list(structure(c(2.8709968773466e-06, -0.000158359165766342, 0.00727428858396739, -0.000819679205658397, -0.000777694946526408, -0.00356554678621684, 0.000131355545630207, 0.0114265093267527, -0.000158359165766342, 5.43254707707774e-06, -0.000158630865337517, 9.73709585506688e-05, 0.000111529300368063, 5.13485783500411e-05, -6.33871099330885e-05, -0.000383481109923256, 0.00727428858396739, -0.000158630865337517, -1.56486391901245e-05, -0.00236056684784514, -0.00652700637569598, 0.00050199030070891, 0.00218994696407579, 0.0203300594009954, -0.000819679205658397, 9.73709585506688e-05, -0.00236056684784514, 7.93209373295412e-07, 0.00187235412049774, 0.00143329638746881, -3.6749249077872e-05, -0.0118829190788863, -0.000777694946526408, 0.000111529300368063, -0.00652700637569598, 0.00187235412049774, 4.25289264915918e-06, 0.00235407805712873, -0.000833270910443051, -0.00229252218256459, -0.00356554678621684, 5.13485783500411e-05, 0.00050199030070891, 0.00143329638746881, 0.00235407805712873, -3.00860514170775e-05, -0.00105162168837414, -0.00640852176345075, 0.000131355545630207, -6.33871099330885e-05, 0.00218994696407579, -3.6749249077872e-05, -0.000833270910443051, -0.00105162168837414, 2.63610545947479e-06, 0.00637158302982355, 0.0114265093267527, -0.000383481109923256, 0.0203300594009954, -0.0118829190788863, -0.00229252218256459, -0.00640852176345075, 0.00637158302982355, -9.55643771360926e-05), .Dim = c(8L, 8L), .Dimnames = list(c('height', 'arm.span', 'forearm', 'lower.leg', 'weight', 'bitro.diameter', 'chest.girth', 'chest.width'), c('height', 'arm.span', 'forearm', 'lower.leg', 'weight', 'bitro.diameter', 'chest.girth', 'chest.width'))));c(argv[[1]]);");
    }

    @Test
    public void testc31() {
        assertEval("argv <- list(c(-Inf, 2.17292368994844e-311, 4.34584737989688e-311, 8.69169475979376e-311, 1.73833895195875e-310, 3.4766779039175e-310, 6.953355807835e-310, 1.390671161567e-309, 2.781342323134e-309, 5.562684646268e-309, 1.1125369292536e-308, 2.2250738585072e-308, 4.4501477170144e-308, 8.90029543402881e-308, 1.78005908680576e-307, 2.2250738585072e-303, 2.2250738585072e-298, 1.79769313486232e+298, 1.79769313486232e+303, 2.24711641857789e+307, 4.49423283715579e+307, 8.98846567431158e+307, 1.79769313486232e+308, Inf, Inf), c(NaN, NA));c(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testc32() {
        assertEval(Ignored.Unknown,
                        "argv <- list(list(structure(list(Ozone = c(NA, NA, NA, NA, NA, NA, 29L, NA, 71L, 39L, NA, NA, 23L, NA, NA, 21L, 37L, 20L, 12L, 13L, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA), Solar.R = c(286L, 287L, 242L, 186L, 220L, 264L, 127L, 273L, 291L, 323L, 259L, 250L, 148L, 332L, 322L, 191L, 284L, 37L, 120L, 137L, 150L, 59L, 91L, 250L, 135L, 127L, 47L, 98L, 31L, 138L), Wind = c(8.6, 9.7, 16.1, 9.2, 8.6, 14.3, 9.7, 6.9, 13.8, 11.5, 10.9, 9.2, 8, 13.8, 11.5, 14.9, 20.7, 9.2, 11.5, 10.3, 6.3, 1.7, 4.6, 6.3, 8, 8, 10.3, 11.5, 14.9, 8), Temp = c(78L, 74L, 67L, 84L, 85L, 79L, 82L, 87L, 90L, 87L, 93L, 92L, 82L, 80L, 79L, 77L, 72L, 65L, 73L, 76L, 77L, 76L, 76L, 76L, 75L, 78L, 73L, 80L, 77L, 83L), Month = c(6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L), Day = 1:30), .Names = c('Ozone', 'Solar.R', 'Wind', 'Temp', 'Month', 'Day'), row.names = 32:61, class = 'data.frame')), structure(list(Oz.Z = structure(c(NA, NA, NA, NA, NA, NA, -0.0244094233987339, NA, 2.28228108778162, 0.52480260307278, NA, NA, -0.353936639281642, NA, NA, -0.463779044575945, 0.414960197778477, -0.518700247223096, -0.958069868400307, -0.903148665753156, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA), .Dim = c(30L, 1L), '`scaled:center`' = 29.4444444444444, '`scaled:scale`' = 18.2079042664931)), .Names = 'Oz.Z'));c(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testc33() {
        assertEval("argv <- list(list('*', ' ', 'skipping installation test', '\\n'), sep = '');c(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testc34() {
        assertEval("argv <- list('exNSS4', 'myLib', structure(c('1.0', NA, 'methods', NA, NA, NA, NA, 'GPL (>= 2)', NA, NA, NA, NA, NA, NA, '3.0.1'), .Names = c('Version', NA, 'Depends', NA, NA, NA, NA, 'License', NA, NA, NA, NA, NA, NA, 'Built')));c(argv[[1]],argv[[2]],argv[[3]]);");
    }

    @Test
    public void testc35() {
        assertEval("argv <- list(c('‘?’ for shortcuts to help topics.', '', '  ‘help.search()’ or ‘??’ for finding help pages', '  on a vague topic;', '  ‘help.start()’ which opens the HTML version of the R', '  help pages;', '  ‘library()’ for listing available packages and the', '  help objects they contain;', '  ‘data()’ for listing available data sets;', '  ‘methods()’.', '', '  Use ‘prompt'), character(0));c(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testc36() {
        assertEval("argv <- list(-1, 0+1i);c(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testc37() {
        assertEval("argv <- list(c(0, 1, 1.3, 1.8, 2.4), 4.6, NULL);c(argv[[1]],argv[[2]],argv[[3]]);");
    }

    @Test
    public void testc38() {
        assertEval("argv <- list(raw(0), 61);c(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testc39() {
        assertEval("argv <- list(structure(list(c(1L, 2L, 4L), 1:3, c(2L, 1L)), class = c('package_version', 'numeric_version')));c(argv[[1]]);");
    }

    @Test
    public void testc40() {
        assertEval("argv <- list(structure(list(A = 1, c = 'C'), .Names = c('A', 'c')), d = 1:3);c(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testc41() {
        assertEval("argv <- list(structure(list(1:3), class = c('package_version', 'numeric_version')), structure(list(c(2L, 1L)), class = c('package_version', 'numeric_version')));c(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testc42() {
        assertEval("argv <- list(structure(list(Ozone = c(41L, 36L, 12L, 18L, NA, 28L, 23L, 19L, 8L, NA, 7L, 16L, 11L, 14L, 18L, 14L, 34L, 6L, 30L, 11L, 1L, 11L, 4L, 32L, NA, NA, NA, 23L, 45L, 115L, 37L, NA, NA, NA, NA, NA, NA, 29L, NA, 71L, 39L, NA, NA, 23L, NA, NA, 21L, 37L, 20L, 12L, 13L, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, 135L, 49L, 32L, NA, 64L, 40L, 77L, 97L, 97L, 85L, NA, 10L, 27L, NA, 7L, 48L, 35L, 61L, 79L, 63L, 16L, NA, NA, 80L, 108L, 20L, 52L, 82L, 50L, 64L, 59L, 39L, 9L, 16L, 78L, 35L, 66L, 122L, 89L, 110L, NA, NA, 44L, 28L, 65L, NA, 22L, 59L, 23L, 31L, 44L, 21L, 9L, NA, 45L, 168L, 73L, NA, 76L, 118L, 84L, 85L, 96L, 78L, 73L, 91L, 47L, 32L, 20L, 23L, 21L, 24L, 44L, 21L, 28L, 9L, 13L, 46L, 18L, 13L, 24L, 16L, 13L, 23L, 36L, 7L, 14L, 30L, NA, 14L, 18L, 20L), Solar.R = c(190L, 118L, 149L, 313L, NA, NA, 299L, 99L, 19L, 194L, NA, 256L, 290L, 274L, 65L, 334L, 307L, 78L, 322L, 44L, 8L, 320L, 25L, 92L, 66L, 266L, NA, 13L, 252L, 223L, 279L, 286L, 287L, 242L, 186L, 220L, 264L, 127L, 273L, 291L, 323L, 259L, 250L, 148L, 332L, 322L, 191L, 284L, 37L, 120L, 137L, 150L, 59L, 91L, 250L, 135L, 127L, 47L, 98L, 31L, 138L, 269L, 248L, 236L, 101L, 175L, 314L, 276L, 267L, 272L, 175L, 139L, 264L, 175L, 291L, 48L, 260L, 274L, 285L, 187L, 220L, 7L, 258L, 295L, 294L, 223L, 81L, 82L, 213L, 275L, 253L, 254L, 83L, 24L, 77L, NA, NA, NA, 255L, 229L, 207L, 222L, 137L, 192L, 273L, 157L, 64L, 71L, 51L, 115L, 244L, 190L, 259L, 36L, 255L, 212L, 238L, 215L, 153L, 203L, 225L, 237L, 188L, 167L, 197L, 183L, 189L, 95L, 92L, 252L, 220L, 230L, 259L, 236L, 259L, 238L, 24L, 112L, 237L, 224L, 27L, 238L, 201L, 238L, 14L, 139L, 49L, 20L, 193L, 145L, 191L, 131L, 223L), Wind = c(7.4, 8, 12.6, 11.5, 14.3, 14.9, 8.6, 13.8, 20.1, 8.6, 6.9, 9.7, 9.2, 10.9, 13.2, 11.5, 12, 18.4, 11.5, 9.7, 9.7, 16.6, 9.7, 12, 16.6, 14.9, 8, 12, 14.9, 5.7, 7.4, 8.6, 9.7, 16.1, 9.2, 8.6, 14.3, 9.7, 6.9, 13.8, 11.5, 10.9, 9.2, 8, 13.8, 11.5, 14.9, 20.7, 9.2, 11.5, 10.3, 6.3, 1.7, 4.6, 6.3, 8, 8, 10.3, 11.5, 14.9, 8, 4.1, 9.2, 9.2, 10.9, 4.6, 10.9, 5.1, 6.3, 5.7, 7.4, 8.6, 14.3, 14.9, 14.9, 14.3, 6.9, 10.3, 6.3, 5.1, 11.5, 6.9, 9.7, 11.5, 8.6, 8, 8.6, 12, 7.4, 7.4, 7.4, 9.2, 6.9, 13.8, 7.4, 6.9, 7.4, 4.6, 4, 10.3, 8, 8.6, 11.5, 11.5, 11.5, 9.7, 11.5, 10.3, 6.3, 7.4, 10.9, 10.3, 15.5, 14.3, 12.6, 9.7, 3.4, 8, 5.7, 9.7, 2.3, 6.3, 6.3, 6.9, 5.1, 2.8, 4.6, 7.4, 15.5, 10.9, 10.3, 10.9, 9.7, 14.9, 15.5, 6.3, 10.9, 11.5, 6.9, 13.8, 10.3, 10.3, 8, 12.6, 9.2, 10.3, 10.3, 16.6, 6.9, 13.2, 14.3, 8, 11.5), Temp = c(67L, 72L, 74L, 62L, 56L, 66L, 65L, 59L, 61L, 69L, 74L, 69L, 66L, 68L, 58L, 64L, 66L, 57L, 68L, 62L, 59L, 73L, 61L, 61L, 57L, 58L, 57L, 67L, 81L, 79L, 76L, 78L, 74L, 67L, 84L, 85L, 79L, 82L, 87L, 90L, 87L, 93L, 92L, 82L, 80L, 79L, 77L, 72L, 65L, 73L, 76L, 77L, 76L, 76L, 76L, 75L, 78L, 73L, 80L, 77L, 83L, 84L, 85L, 81L, 84L, 83L, 83L, 88L, 92L, 92L, 89L, 82L, 73L, 81L, 91L, 80L, 81L, 82L, 84L, 87L, 85L, 74L, 81L, 82L, 86L, 85L, 82L, 86L, 88L, 86L, 83L, 81L, 81L, 81L, 82L, 86L, 85L, 87L, 89L, 90L, 90L, 92L, 86L, 86L, 82L, 80L, 79L, 77L, 79L, 76L, 78L, 78L, 77L, 72L, 75L, 79L, 81L, 86L, 88L, 97L, 94L, 96L, 94L, 91L, 92L, 93L, 93L, 87L, 84L, 80L, 78L, 75L, 73L, 81L, 76L, 77L, 71L, 71L, 78L, 67L, 76L, 68L, 82L, 64L, 71L, 81L, 69L, 63L, 70L, 77L, 75L, 76L, 68L), Month = c(5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 7L, 7L, 7L, 7L, 7L, 7L, 7L, 7L, 7L, 7L, 7L, 7L, 7L, 7L, 7L, 7L, 7L, 7L, 7L, 7L, 7L, 7L, 7L, 7L, 7L, 7L, 7L, 7L, 7L, 7L, 7L, 8L, 8L, 8L, 8L, 8L, 8L, 8L, 8L, 8L, 8L, 8L, 8L, 8L, 8L, 8L, 8L, 8L, 8L, 8L, 8L, 8L, 8L, 8L, 8L, 8L, 8L, 8L, 8L, 8L, 8L, 8L, 9L, 9L, 9L, 9L, 9L, 9L, 9L, 9L, 9L, 9L, 9L, 9L, 9L, 9L, 9L, 9L, 9L, 9L, 9L, 9L, 9L, 9L, 9L, 9L, 9L, 9L, 9L, 9L, 9L, 9L), Day = c(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L, 12L, 13L, 14L, 15L, 16L, 17L, 18L, 19L, 20L, 21L, 22L, 23L, 24L, 25L, 26L, 27L, 28L, 29L, 30L, 31L, 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L, 12L, 13L, 14L, 15L, 16L, 17L, 18L, 19L, 20L, 21L, 22L, 23L, 24L, 25L, 26L, 27L, 28L, 29L, 30L, 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L, 12L, 13L, 14L, 15L, 16L, 17L, 18L, 19L, 20L, 21L, 22L, 23L, 24L, 25L, 26L, 27L, 28L, 29L, 30L, 31L, 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L, 12L, 13L, 14L, 15L, 16L, 17L, 18L, 19L, 20L, 21L, 22L, 23L, 24L, 25L, 26L, 27L, 28L, 29L, 30L, 31L, 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L, 12L, 13L, 14L, 15L, 16L, 17L, 18L, 19L, 20L, 21L, 22L, 23L, 24L, 25L, 26L, 27L, 28L, 29L, 30L)), .Names = c('Ozone', 'Solar.R', 'Wind', 'Temp', 'Month', 'Day'), row.names = c(NA, -153L)), list(NULL, NULL));c(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testc43() {
        assertEval(Ignored.Unknown,
                        "argv <- list(list(structure(list(u = c(5, 10, 15, 20, 30, 40, 60, 80, 100), lot1 = c(118, 58, 42, 35, 27, 25, 21, 19, 18), lot2 = c(69, 35, 26, 21, 18, 16, 13, 12, 12)), .Names = c('u', 'lot1', 'lot2'), row.names = c(NA, -9L), class = 'data.frame')), structure(list(max.level = 0, give.attr = FALSE, digits = 3), .Names = c('max.level', 'give.attr', 'digits')));c(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testc44() {
        assertEval(Ignored.Unknown,
                        "argv <- list(list(structure(list(structure('vpl1', class = c('vpListing', 'gridVectorListing', 'gridListing')), structure('1', class = c('vpUpListing', 'gridVectorListing', 'gridListing'))), class = c('gridListListing', 'gridListing'))), list(structure('vpl2', class = c('vpListing', 'gridVectorListing', 'gridListing'))));c(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testc45() {
        assertEval("argv <- list(1, c(2, 3, 4, 6, 7, 8, 9, 10, 11), c(3, 4, 5, 7, 8, 9, 10, 11), 11L);c(argv[[1]],argv[[2]],argv[[3]],argv[[4]]);");
    }

    @Test
    public void testc46() {
        assertEval("argv <- list(TRUE, NULL);c(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testc47() {
        assertEval("argv <- list(NULL, structure(list(names = structure('stats', .Names = 'name')), .Names = 'names'));c(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testc48() {
        assertEval("argv <- list(list(c('foo', 'bar'), FALSE, structure(list(a = 1, b = structure(c(1, 3, 2, 4), .Dim = c(2L, 2L)), c = 'foo'), .Names = c('a', 'b', 'c')), structure(list(1, 'foo', structure(c(1, 3, 2, 4), .Dim = c(2L, 2L)), 'bar'), .Dim = c(2L, 2L)), 0+1i, list(1, structure(c(1, 3, 2, 4), .Dim = c(2L, 2L)), 'foo'), structure(c(1+2i, 5+0i, 3-4i, -6+0i), .Dim = c(2L, 2L)), c(1, 2, 3, 4, 5), 1, structure(c(1+1i, 3+1i, 2+1i, 4+1i, 5-1i, 7-1i, 6-1i, 8-1i), .Dim = c(2L, 2L, 2L)), structure(c(1, 3, 2, 4, 5, 7, 6, 8), .Dim = c(2L, 2L, 2L)), structure(c(1, 3, 2, 4), .Dim = c(2L, 2L))), list(structure(c(TRUE, FALSE, FALSE, TRUE), .Dim = c(2L, 2L))));c(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testc49() {
        assertEval("argv <- list(c(NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA), structure(c(3.3032627879465, 3.28768675817403, 3.28198500972868, 3.26064954685429, 3.28230636466286, 3.29427556805693, 3.28140319515598, 3.31501132729969, 3.29996451963546, 3.3405648068776, 3.3615463372345, 3.37238152179651, 3.32652089130696, 3.31449399159178, 3.31051950313397, 3.29704421073007, 3.31063284281209, 3.31814807478072, 3.3100622663054, 3.33117177869743, 3.32172069914554, 3.34722215914612, 3.36040087649739, 3.36720656884446), .Tsp = c(1983, 1984.91666666667, 12), class = 'ts'), logical(0));c(argv[[1]],argv[[2]],argv[[3]]);");
    }

    @Test
    public void testc50() {
        assertEval("argv <- list(1, 1, 1, list());c(argv[[1]],argv[[2]],argv[[3]],argv[[4]]);");
    }

    @Test
    public void testc51() {
        assertEval("argv <- list(list('1: In matrix(1:7, 3, 4) :\\n  data length [7] is not a sub-multiple or multiple of the number of rows [3]'), list(), fill = TRUE);c(argv[[1]],argv[[2]],argv[[3]]);");
    }

    @Test
    public void testc52() {
        assertEval("argv <- list(structure(list(names = c('freq', 'score')), .Names = 'names'), structure(list(class = 'data.frame', row.names = integer(0)), .Names = c('class', 'row.names')));c(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testc53() {
        assertEval("argv <- list(1, FALSE, c(0, 0));c(argv[[1]],argv[[2]],argv[[3]]);");
    }

    @Test
    public void testc54() {
        assertEval("argv <- list(structure(list(x = structure(1:8, .Dim = structure(8L, .Names = 'voice.part'))), .Names = 'x'), list(4));c(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testc55() {
        assertEval("argv <- list(c(1L, 2L, 3L, NA), c(-1, 0, 1, NA));c(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testc56() {
        assertEval("argv <- list(369.430769230769, 4.99999999999983);c(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testc57() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(list(structure(list(title = 'boot: Bootstrap R (S-PLUS) Functions', author = structure(list(structure(list(given = 'Angelo', family = 'Canty', role = 'aut', email = NULL, comment = 'S original'), .Names = c('given', 'family', 'role', 'email', 'comment')), structure(list(given = c('Brian', 'D.'), family = 'Ripley', role = c('aut', 'trl', 'cre'), email = 'ripley@stats.ox.ac.uk', comment = 'R port, author of parallel support'), .Names = c('given', 'family', 'role', 'email', 'comment'))), class = 'person'),     year = '2012', note = 'R package version 1.3-4', url = 'http://CRAN.R-project.org/package=boot'), .Names = c('title', 'author', 'year', 'note', 'url'), bibtype = 'Manual', key = 'boot-package')), class = 'bibentry'), structure(list(structure(list(title = 'Bootstrap Methods and Their Applications', author = structure(list(structure(list(given = c('Anthony', 'C.'), family = 'Davison', role = 'aut', email = NULL, comment = NULL), .Names = c('given', 'family', 'role', 'email', 'comment')), structure(list(    given = c('David', 'V.'), family = 'Hinkley', role = 'aut', email = NULL, comment = NULL), .Names = c('given', 'family', 'role', 'email', 'comment'))), class = 'person'), year = '1997', publisher = 'Cambridge University Press', address = 'Cambridge', isbn = '0-521-57391-2', url = 'http://statwww.epfl.ch/davison/BMA/'), .Names = c('title', 'author', 'year', 'publisher', 'address', 'isbn', 'url'), bibtype = 'Book', key = 'boot-book')), class = 'bibentry'));c(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testc58() {
        assertEval("argv <- list(FALSE, 'More testing :', 12321, 'B2');c(argv[[1]],argv[[2]],argv[[3]],argv[[4]]);");
    }

    @Test
    public void testc59() {
        assertEval("argv <- list(1:10, 1+1i, TRUE);c(argv[[1]],argv[[2]],argv[[3]]);");
    }

    @Test
    public void testc60() {
        assertEval("argv <- list('ArgMethod', 1.10714871779409);c(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testc61() {
        assertEval(Ignored.Unknown, "argv <- list(structure(list(`ANY#ANY` = .Primitive('==')), .Names = 'ANY#ANY'), list());c(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testc62() {
        assertEval("argv <- list(list(), list());c(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testc63() {
        assertEval("argv <- list(recursive = TRUE);c(argv[[1]]);");
    }

    @Test
    public void testc64() {
        assertEval("argv <- list(structure(1386393974.25184, class = c('POSIXct', 'POSIXt')), structure(1386393974.25184, class = c('POSIXct', 'POSIXt')));c(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testc65() {
        assertEval("argv <- list('BiocInstaller', '/home/lzhao/R/x86_64-unknown-linux-gnu-library/3.0', structure(c('1.12.0', NA, 'R (>= 3.0.0)', NA, NA, 'RUnit, BiocGenerics', NA, 'Artistic-2.0', NA, NA, NA, NA, NA, NA, '3.0.1'), .Names = c('Version', NA, 'Depends', NA, NA, 'Suggests', NA, 'License', NA, NA, NA, NA, NA, NA, 'Built')));c(argv[[1]],argv[[2]],argv[[3]]);");
    }

    @Test
    public void testc66() {
        assertEval("argv <- list(NA, 1+2i);c(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testc67() {
        assertEval("argv <- list(structure(c(0.06, 0.32, 0.63), .Names = c('0%', '25%', '50%')), 909.591818181818, structure(c(0.905, 10000), .Names = c('75%', '100%')));c(argv[[1]],argv[[2]],argv[[3]]);");
    }

    @Test
    public void testc68() {
        assertEval("argv <- list(structure(list(ctrl = c(4.17, 5.58, 5.18, 6.11, 4.5, 4.61, 5.17, 4.53, 5.33, 5.14), trt1 = c(4.81, 4.17, 4.41, 3.59, 5.87, 3.83, 6.03, 4.89, 4.32, 4.69), trt2 = c(6.31, 5.12, 5.54, 5.5, 5.37, 5.29, 4.92, 6.15, 5.8, 5.26)), .Dim = 3L, .Dimnames = list(c('ctrl', 'trt1', 'trt2'))));c(argv[[1]]);");
    }

    @Test
    public void testc69() {
        assertEval("argv <- list(list(NA, FALSE), structure(list(na.rm = TRUE), .Names = 'na.rm'));c(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testc70() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(list(Topic = character(0), File = character(0)), .Names = c('Topic', 'File'), class = 'data.frame', row.names = integer(0)), sep = '\\r');c(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testc71() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(list(Subject = structure(c(1L, 3L, 6L, 2L, 4L, 5L), .Label = c('1', '4', '2', '5', '6', '3'), class = c('ordered', 'factor')), conc.0.25 = c(1.5, 2.03, 2.72, 1.85, 2.05, 2.31), conc.0.5 = c(0.94, 1.63, 1.49, 1.39, 1.04, 1.44), conc.0.75 = c(0.78, 0.71, 1.16, 1.02, 0.81, 1.03), conc.1 = c(0.48, 0.7, 0.8, 0.89, 0.39, 0.84), conc.1.25 = c(0.37, 0.64, 0.8, 0.59, 0.3, 0.64), conc.2 = c(0.19, 0.36, 0.39, 0.4, 0.23, 0.42)), row.names = c(1L, 12L, 23L, 34L, 45L, 56L), .Names = c('Subject', 'conc.0.25', 'conc.0.5', 'conc.0.75', 'conc.1', 'conc.1.25', 'conc.2')), list(NULL));c(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testc72() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(list(x.limits = c(-2.46408176011189, 2.92512533057276), y.limits = structure(c(1386479490.57927, 1387608090.57927), class = c('POSIXct', 'POSIXt')), x.used.at = NULL, y.used.at = NULL, x.num.limit = NULL, y.num.limit = NULL, aspect.ratio = 1, prepanel.default = 'prepanel.default.xyplot', prepanel = NULL), .Names = c('x.limits', 'y.limits', 'x.used.at', 'y.used.at', 'x.num.limit', 'y.num.limit', 'aspect.ratio', 'prepanel.default', 'prepanel')), structure(list(index.cond = list(1:3),     perm.cond = 1L), .Names = c('index.cond', 'perm.cond')));c(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testc73() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(list(coefficients = structure(c(-0.0529307911108286, -0.200175675120066), .Names = c('(Intercept)', 'xTRUE')), residuals = structure(c(0.196977726701894, -0.102864715594501, -1.21764591766838, -0.425219263997792, 0.671048026430597, 1.41161034263987, 0.150318738887899, 0.440602402670198, 0.19930142564799, -1.32412876571778), .Names = c('1', '2', '3', '4', '5', '6', '7', '8', '9', '10')), effects = structure(c(0.483887391035467, -0.316505532770654, -1.29088865053614, -0.430233412486575, 0.597805293562832, 1.40659619415109, 0.0770760060201344, 0.435588254181415, 0.126058692780225, -1.32914291420656), .Names = c('(Intercept)', 'xTRUE', '', '', '', '', '', '', '', '')), rank = 2L), .Names = c('coefficients', 'residuals', 'effects', 'rank')), structure(list(fitted.values = structure(c(-0.253106466230895, -0.0529307911108286, -0.253106466230895, -0.0529307911108285, -0.253106466230895, -0.0529307911108285, -0.253106466230895, -0.0529307911108285, -0.253106466230895, -0.0529307911108285), .Names = c('1', '2', '3', '4', '5', '6', '7', '8', '9', '10')), assign = 0:1, qr = structure(list(qr = structure(c(-3.16227766016838, 0.316227766016838, 0.316227766016838, 0.316227766016838, 0.316227766016838, 0.316227766016838, 0.316227766016838, 0.316227766016838, 0.316227766016838, 0.316227766016838, -1.58113883008419, 1.58113883008419, -0.240253073352042, 0.392202458681634, -0.240253073352042, 0.392202458681634, -0.240253073352042, 0.392202458681634, -0.240253073352042, 0.392202458681634), .Dim = c(10L, 2L), .Dimnames = list(c('1', '2', '3', '4', '5', '6', '7', '8', '9', '10'), c('(Intercept)', 'xTRUE')), assign = 0:1, contrasts = structure(list(x = 'contr.treatment'), .Names = 'x')), qraux = c(1.31622776601684, 1.39220245868163), pivot = 1:2, tol = 1e-07, rank = 2L), .Names = c('qr', 'qraux', 'pivot', 'tol', 'rank'), class = 'qr'), df.residual = 8L), .Names = c('fitted.values', 'assign', 'qr', 'df.residual')));c(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testc74() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(list(object = c('time', 'status')), .Names = 'object'), structure(list(max.level = NA, vec.len = 4, digits.d = 3, nchar.max = 128, give.attr = TRUE, give.head = TRUE, width = 80L, envir = NULL, strict.width = 'no', formatNum = function (x, ...) format(x, trim = TRUE, drop0trailing = TRUE, ...), list.len = 99), .Names = c('max.level', 'vec.len', 'digits.d', 'nchar.max', 'give.attr', 'give.head', 'width', 'envir', 'strict.width', 'formatNum', 'list.len')), structure(list(give.length = TRUE, nest.lev = 2, indent.str = '  .. ..'), .Names = c('give.length', 'nest.lev', 'indent.str')));c(argv[[1]],argv[[2]],argv[[3]]);");
    }

    private static String[] repeat(int size, String[] array) {
        if (size == array.length) {
            return array;
        } else if (size <= array.length) {
            return Arrays.copyOf(array, size);
        } else {
            String[] result = new String[size];
            for (int i = 0; i < size; i++) {
                result[i] = array[i % array.length];
            }
            return result;
        }
    }

    private void genTest(String test, int size, String[] testValues) {
        String genTest = String.format(test, (Object[]) repeat(size, testValues));
        assertEval(genTest);
    }

    public void testCombine(String[] testValues) {
        genTest("{ c(%s) }", 1, testValues);
        genTest("{ c(%s, %s) }", 2, testValues);
        genTest("{ c(c(%s,%s), %s) }", 3, testValues);
        genTest("{ c(%s, c(%s,%s)) }", 3, testValues);
        genTest("{ c(NULL, c(%s,%s)) }", 2, testValues);
        genTest("{ c(c(%s,%s), NULL) }", 2, testValues);
        genTest("{ c(NULL, %s, NULL) }", 1, testValues);
        genTest("{ c(NULL, %s) }", 1, testValues);
        genTest("{ c(%s, NULL) }", 1, testValues);

        genTest("{ c(c(%s,%s), c(%s,%s)) }", 4, testValues);
        genTest("{ c(c(%s,%s), %s, c(%s,%s)) }", 5, testValues);
        genTest("{ c(c(%s,%s), c(%s,%s), c(%s,%s)) }", 6, testValues);
    }

    @Test
    public void testCombine() {
        assertEval("{ c(\"1.2\",\"3.4\") }");
        assertEval("{ c(\"a\",\"b\",\"c\") }");
        assertEval("{ c(\"1\",\"b\") }");
        assertEval("{ c(\"1.00\",\"2.00\") }");
        assertEval("{ c(\"1.00\",\"b\") }");

        assertEval("{ c(1.0,1L) }");
        assertEval("{ c(1L,1.0) }");
        assertEval("{ c( 1:3 ) }");
        assertEval("{ c( 1L:3L ) }");
        assertEval("{ c( 100, 1:3, 200 ) }");
        assertEval("{ c( 1:3, 7:9 ) }");
        assertEval("{ c( 1:3, 5, 7:9 ) }");

        assertEval("{ c() }");
        assertEval("{ c(NULL) }");
        assertEval("{ c(NULL,NULL) }");
        assertEval("{ c(NULL,1,2,3) }");

        assertEval("{ c(1+1i,2-3i,4+5i) }");

        assertEval("{ c(\"hello\", \"hi\") }");

        assertEval("{ c(1+1i, as.raw(10)) }");
        assertEval("{ c(as.raw(10), as.raw(20)) }");
        assertEval("{ c(as.raw(10),  \"test\") }");

        assertEval("{ f <- function(x,y) { c(x,y) } ; f(1,1) ; f(1, TRUE) }");
        assertEval("{ f <- function(x,y) { c(x,y) } ; f(1,1) ; f(1, TRUE) ; f(NULL, NULL) }");

        testCombine(new String[]{"1", "2", "3"});
        testCombine(new String[]{"1L", "2L", "3L"});
        testCombine(new String[]{"TRUE", "FALSE", "FALSE"});
        testCombine(new String[]{"\"a\"", "\"b\"", "\"c\""});
        testCombine(new String[]{"\"d\"", "2L", "\"f\""});
        testCombine(new String[]{"\"g\"", "2", "2"});
        testCombine(new String[]{"\"j\"", "TRUE", "TRUE"});

        // test propagation of the "names" attribute
        assertEval("{ x<-1:2; names(x)<-7:8; y<-3:4; names(y)<-9:10; z<-c(x, y); z }");
        assertEval("{ x<-1:2; names(x)<-7:8; y<-3:4; z<-c(x, y); z }");
        assertEval("{ x<-1:2; names(x)<-7:8;  z<-c(x, integer()); z }");
        assertEval("{ x<-1:2; names(x)<-7:8; z<-c(x, 3L); z }");
        assertEval("{ x<-1:2; names(x)<-7:8; z<-c(3L, x); z }");
        assertEval("{ x<-1:2; names(x)<-7:8; y<-double(0);  z<-c(x, y); z }");
        assertEval("{ x<-1:2; names(x)<-7:8; z<-c(x, 3); z }");
        assertEval("{ x<-1:2; names(x)<-7:8; z<-c(x, 3); attributes(z) }");

        assertEval("{ c(a=42) }");
        assertEval("{ c(a=FALSE) }");
        assertEval("{ c(a=as.raw(7)) }");
        assertEval("{ c(a=\"foo\") }");
        assertEval("{ c(a=7i) }");

        assertEval("{ c(a=1, b=2) }");
        assertEval("{ c(a=FALSE, b=TRUE) }");
        assertEval("{ c(a=as.raw(1), b=as.raw(2)) }");
        assertEval("{ c(a=\"bar\", b=\"baz\") }");
        assertEval("{ c(a=1, 2) }");
        assertEval("{ c(1, b=2) }");

        assertEval("{ c(a=1i, b=2i) }");
        assertEval("{ c(a=7i, a=1:2) }");
        assertEval("{ c(a=1:2, 42) }");
        assertEval("{ c(a=1:2, b=c(42)) }");
        assertEval("{ c(a=1:2, b=double()) }");
        assertEval("{ c(a=c(z=1), 42) }");
        assertEval("{ x<-c(z=1); names(x)=c(\"\"); c(a=x, 42) }");
        assertEval("{ x<-c(y=1, z=2); names(x)=c(\"\", \"\"); c(a=x, 42) }");
        assertEval("{ x<-c(y=1, z=2);  c(a=x, 42) }");
        assertEval("{ x<-c(y=1);  c(x, 42) }");
        assertEval("{ x<-c(1);  c(z=x, 42) }");
        assertEval("{ x<-c(y=1, 2);  c(a=x, 42) }");

        assertEval("{ c(TRUE,1L,1.0,list(3,4)) }");
        assertEval("{ c(TRUE,1L,1.0,list(3,list(4,5))) }");

        assertEval("{ c(x=1,y=2) }");
        assertEval("{ c(x=1,2) }");
        assertEval("{ x <- 1:2 ; names(x) <- c(\"A\",NA) ; c(x,test=x) }");
        assertEval("{ c(a=1,b=2:3,list(x=FALSE))  }");
        assertEval("{ c(1,z=list(1,b=22,3)) }");

        assertEval("{ is.matrix(c(matrix(1:4,2))) }");

        assertEval("{ x<-expression(1); c(x) }");
        assertEval("{ x<-expression(1); c(x,2) }");

        // print output for a function in a list doesn't match GnuR,
        // which seems to invoke deparse, so we just check the c didn't fail.
        assertEval("{ f <- function() { }; length(c(f, 2)) == 2 }");

        assertEval("{ e1 <- new.env(), e2 <- new.env(); c(e1, e2) }");
        assertEval("{ e1 <- new.env(), c(e1, 3) }");

        assertEval("{ setClass(\"foo\", representation(d=\"numeric\")); x<-new(\"foo\", d=42); y<-c(x, 7); y[[1]] }");
    }

    @Test
    public void testCombineBroken() {
        assertEval(Ignored.Unknown, "{ c(1i,0/0) }"); // yes, this is done by GNU-R, note
        // inconsistency with as.complex(0/0)
    }
}
