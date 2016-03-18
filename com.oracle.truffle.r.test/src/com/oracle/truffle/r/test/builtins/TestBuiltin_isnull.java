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
public class TestBuiltin_isnull extends TestBase {

    @Test
    public void testisnull1() {
        assertEval("argv <- list(c('a', 'b', 'c'));is.null(argv[[1]]);");
    }

    @Test
    public void testisnull2() {
        assertEval("argv <- list(structure(c(NA, NA, 159.125, 204, 221.25, 245.125, 319.75, 451.5, 561.125, 619.25, 615.625, 548, 462.125, 381.125, 316.625, 264, 228.375, 210.75, 188.375, 199, 207.125, 191, 166.875, 72, -9.25, -33.125, -36.75, 36.25, 103, 131.625, NA, NA), .Tsp = c(1951, 1958.75, 4), class = 'ts'));is.null(argv[[1]]);");
    }

    @Test
    public void testisnull3() {
        assertEval("argv <- list(structure(c(1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L), .Label = c('L', 'M', 'H'), class = 'factor'));is.null(argv[[1]]);");
    }

    @Test
    public void testisnull4() {
        assertEval("argv <- list(c('(2,5.5]', '(5.5,10]', NA));is.null(argv[[1]]);");
    }

    @Test
    public void testisnull5() {
        assertEval("argv <- list(structure(list(z = c(-2.71928906935559, -2.42170276502517, -2.09964379178171, -1.74953243478614, -1.36765437050161, -0.950481896729501, -0.49514368442691, 0, 0.534774072422106, 1.1067130528647, 1.71078417306203, 2.33938293418822, 2.98268239609615), par.vals = structure(c(0.707463571249756, 0.714694094477037, 0.725412821685713, 0.74111612512182, 0.763750498997247, 0.795678221483334, 0.839503022768249, 0.897728639347183, 0.972289000300049, 1.06404105741634, 1.1722925771844, 1.29437141627989, 1.42522018859931, -3.11497037357416, -3.12714840246737, -3.14532049441438, -3.17220876767473, -3.21154655520113, -3.26827705075488, -3.34869944608425, -3.46054428079529, -3.61294451442018, -3.81614134368036, -4.08060875057211, -4.41549521607872, -4.82702626542922, -0.0255577133668773, -0.0384397882414428, -0.0575047563177536, -0.085367554260897, -0.125387593962273, -0.181561305237101, -0.258149413255891, -0.359008117508679, -0.486728760637899, -0.641785962540215, -0.821841695092364, -1.02123122113516, -1.23065013245083, 7.95100998228548, 7.54634587182367, 7.14890399737901, 6.76196968783309, 6.39005226899545, 6.03912521056563, 5.71679838524513, 5.43240185128028, 5.19696909896364, 5.02301800418124, 4.92391121830517, 4.91279665045699, 5.00177553632184, -9.53200922191114, -8.69335766510962, -7.8547061083081, -7.01605455150657, -6.17740299470505, -5.33875143790352, -4.500099881102, -3.66144832430047, -2.82279676749895, -1.98414521069743, -1.1454936538959, -0.306842097094378, 0.531809459707146), .Dim = c(13L, 5L), .Dimnames = list(    NULL, c('(Intercept)', 'PS', 'AI', 'VS', 'PS:AI')))), .Names = c('z', 'par.vals'), row.names = c(NA, 13L), class = 'data.frame'));is.null(argv[[1]]);");
    }

    @Test
    public void testisnull6() {
        assertEval("argv <- list(structure(c(-4, -3, -2, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96), .Tsp = c(1, 101, 1), class = 'ts'));is.null(argv[[1]]);");
    }

    @Test
    public void testisnull7() {
        assertEval("argv <- list(numeric(0));is.null(argv[[1]]);");
    }

    @Test
    public void testisnull8() {
        assertEval("argv <- list(structure(c(2, 3, 4, 5, 6, 7, 8, 9, 10, 11), .Tsp = c(1920.5, 1921.25, 12), class = 'ts'));is.null(argv[[1]]);");
    }

    @Test
    public void testisnull10() {
        assertEval("argv <- list(structure(list(coefficients = numeric(0), residuals = structure(c(-68.7898369431611, -71.7713382904347, -44.0000000000001, -56.5455568546283, -29.303772984227), .Dim = c(5L, 1L), .Dimnames = list(c('2', '3', '4', '5', '6'), NULL)), fitted.values = structure(c(0, 0, 0, 0, 0), .Dim = c(5L, 1L), .Dimnames = list(c('2', '3', '4', '5', '6'), NULL)), weights = NULL, rank = 0L, df.residual = 5L), .Names = c('coefficients', 'residuals', 'fitted.values', 'weights', 'rank', 'df.residual'), class = c('aov', 'lm')));is.null(argv[[1]]);");
    }

    @Test
    public void testisnull11() {
        assertEval("argv <- list(complex(0));is.null(argv[[1]]);");
    }

    @Test
    public void testisnull12() {
        assertEval("argv <- list(1.74126257032961e-18);is.null(argv[[1]]);");
    }

    @Test
    public void testisnull13() {
        assertEval("argv <- list(structure(3.14159265358979, class = structure('3.14159265358979', class = 'testit')));is.null(argv[[1]]);");
    }

    @Test
    public void testisnull14() {
        assertEval("argv <- list(NA_complex_);do.call('is.null', argv)");
    }

    @Test
    public void testisnull15() {
        assertEval("argv <- list(complex(real = 3, imaginary = -Inf));do.call('is.null', argv)");
    }

    @Test
    public void testisnull16() {
        assertEval(Ignored.Unknown,
                        "argv <- list(function(file = ifelse(onefile, 'Rplots.pdf', 'Rplot%03d.pdf'),     width, height, onefile, family, title, fonts, version, paper,     encoding, bg, fg, pointsize, pagecentre, colormodel, useDingbats,     useKerning, fillOddEven, compress) {    initPSandPDFfonts()    new <- list()    if (!missing(width)) new$width <- width    if (!missing(height)) new$height <- height    if (!missing(onefile)) new$onefile <- onefile    if (!missing(title)) new$title <- title    if (!missing(fonts)) new$fonts <- fonts    if (!missing(version)) new$version <- version    if (!missing(paper)) new$paper <- paper    if (!missing(encoding)) new$encoding <- encoding    if (!missing(bg)) new$bg <- bg    if (!missing(fg)) new$fg <- fg    if (!missing(pointsize)) new$pointsize <- pointsize    if (!missing(pagecentre)) new$pagecentre <- pagecentre    if (!missing(colormodel)) new$colormodel <- colormodel    if (!missing(useDingbats)) new$useDingbats <- useDingbats    if (!missing(useKerning)) new$useKerning <- useKerning    if (!missing(fillOddEven)) new$fillOddEven <- fillOddEven    if (!missing(compress)) new$compress <- compress    old <- check.options(new, name.opt = '.PDF.Options', envir = .PSenv)    if (!missing(family) && (inherits(family, 'Type1Font') ||         inherits(family, 'CIDFont'))) {        enc <- family$encoding        if (inherits(family, 'Type1Font') && !is.null(enc) &&             enc != 'default' && (is.null(old$encoding) || old$encoding ==             'default')) old$encoding <- enc        family <- family$metrics    }    if (is.null(old$encoding) || old$encoding == 'default') old$encoding <- guessEncoding()    if (!missing(family)) {        if (length(family) == 4L) {            family <- c(family, 'Symbol.afm')        } else if (length(family) == 5L) {        } else if (length(family) == 1L) {            pf <- pdfFonts(family)[[1L]]            if (is.null(pf)) stop(gettextf('unknown family '%s'',                 family), domain = NA)            matchFont(pf, old$encoding)        } else stop('invalid 'family' argument')        old$family <- family    }    version <- old$version    versions <- c('1.1', '1.2', '1.3', '1.4', '1.5', '1.6', '1.7',         '2.0')    if (version %in% versions) version <- as.integer(strsplit(version,         '[.]')[[1L]]) else stop('invalid PDF version')    onefile <- old$onefile    if (!checkIntFormat(file)) stop(gettextf('invalid 'file' argument '%s'',         file), domain = NA)    .External(C_PDF, file, old$paper, old$family, old$encoding,         old$bg, old$fg, old$width, old$height, old$pointsize,         onefile, old$pagecentre, old$title, old$fonts, version[1L],         version[2L], old$colormodel, old$useDingbats, old$useKerning,         old$fillOddEven, old$compress)    invisible()});"
                                        + "do.call('is.null', argv)");
    }
}
