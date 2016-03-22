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
public class TestBuiltin_isfunction extends TestBase {

    @Test
    public void testisfunction1() {
        assertEval("argv <- list(function (x, y) {    c(x, y)});is.function(argv[[1]]);");
    }

    @Test
    public void testisfunction2() {
        assertEval("argv <- list(c('a', 'b', NA, NA, NA, 'f', 'g', 'h', 'i', 'j', 'k', 'l'));is.function(argv[[1]]);");
    }

    @Test
    public void testisfunction3() {
        assertEval("argv <- list(c(0, 0, 0, 0, 0, 1.75368801162502e-134, 0, 0, 0, 2.60477585273833e-251, 1.16485035372295e-260, 0, 1.53160350210786e-322, 0.333331382328728, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3.44161262707711e-123, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1.968811545398e-173, 0, 8.2359965384697e-150, 0, 0, 0, 0, 6.51733217171341e-10, 0, 2.36840184577368e-67, 0, 9.4348408357524e-307, 0, 1.59959906013771e-89, 0, 8.73836857865034e-286, 7.09716190970992e-54, 0, 0, 0, 1.530425353017e-274, 8.57590058044551e-14, 0.333333106397154, 0, 0, 1.36895217898448e-199, 2.0226102635783e-177, 5.50445388209462e-42, 0, 0, 0, 0, 1.07846402051283e-44, 1.88605464411243e-186, 1.09156111051203e-26, 0, 3.0702877273237e-124, 0.333333209689785, 0, 0, 0, 0, 0, 0, 3.09816093866831e-94, 0, 0, 4.7522727332095e-272, 0, 0, 2.30093251441394e-06, 0, 0, 1.27082826644707e-274, 0, 0, 0, 0, 0, 0, 0, 4.5662025456054e-65, 0, 2.77995853978268e-149, 0, 0, 0));is.function(argv[[1]]);");
    }

    @Test
    public void testisfunction4() {
        assertEval("argv <- list(structure(c(1, 0.341108926893721, 7.03552967282433, 0.0827010685995216, 2.31431632448833, 2.09774421189194, 17.4719768806091, 0.00710660186854106, 0.341108926893721, 6.24415190746514, 0.0686360870302571, 2.93161486304804, 1, 20.6254633582673, 0.242447681896313, 6.78468413466471, 6.14977811045541, 51.2211071100253, 0.02083381966358, 1, 18.3054485390546, 0.201214572879361, 0.142135709250525, 0.0484837592557245, 1, 0.0117547750411693, 0.328946992211217, 0.298164361383444, 2.48339182593415, 0.00101010189694619, 0.0484837592557245, 0.887516960035576, 0.00975563891022635, 12.0917421858535, 4.12460120129202, 85.071810944714, 1, 27.984116332225, 25.3653821820638, 211.266639917518, 0.0859311976119033, 4.12460120129202, 75.5026750343739, 0.829929869015672, 0.432093050296869, 0.147390796704999, 3.04000347678479, 0.0357345569939779, 1, 0.906420695258988, 7.54951978505874, 0.00307071327862333, 0.147390796704999, 2.69805464421362, 0.0296571762053451, 0.476702542822467, 0.16260749282968, 3.35385488513828, 0.0394238096955273, 1.10324047677913, 1, 8.32893580712171, 0.00338773518156042, 0.16260749282968, 2.97660309205839, 0.0327189972167077, 0.0572345079685763, 0.0195232015944512, 0.402675079122419, 0.00473335496976909, 0.132458756115733, 0.120063357811564, 1, 0.000406742861274512, 0.0195232015944512, 0.357380962104814, 0.00392835267006514, 140.714228614202, 47.998879521268, 989.999130803802, 11.6372170735519, 325.65723636963, 295.182458606281, 2458.55574912007, 1, 47.998879521268, 878.641019008853, 9.65807404155984, 2.93161486304804, 1, 20.6254633582673, 0.242447681896313, 6.78468413466471, 6.14977811045541, 51.2211071100253, 0.02083381966358, 1, 18.3054485390546, 0.201214572879361, 0.160149851384054, 0.0546285439478035, 1.12673903151092, 0.0132445638455158, 0.370637415422497, 0.335953423776254, 2.79813450081517, 0.00113812123309249, 0.0546285439478035, 1, 0.0109920591374787, 14.5695951396408, 4.96981896335886, 102.50481892598, 1.20492108711069, 33.7186518728567, 30.5632838737905, 254.559629439639, 0.103540312043258, 4.96981896335886, 90.9747652821832, 1), .Dim = c(11L, 11L), .Dimnames = list(c('ATS', 'BEF', 'DEM', 'ESP', 'FIM', 'FRF', 'IEP', 'ITL', 'LUF', 'NLG', 'PTE'), c('ATS', 'BEF', 'DEM', 'ESP', 'FIM', 'FRF', 'IEP', 'ITL', 'LUF', 'NLG', 'PTE'))));is.function(argv[[1]]);");
    }

    @Test
    public void testisfunction5() {
        assertEval("argv <- list(structure(c(1+0i, 5+0i, 9+0i, 13+0i, 17+0i, 21+0i, 2+0i, 6+0i, 10+0i, 14+0i, 18+0i, 22+0i, 3+0i, 7+0i, 11+0i, 15+0i, 19+0i, 23+0i, 4+0i, 8+0i, 12+0i, 16+0i, 20+0i, 24+0i), .Dim = c(6L, 4L)));is.function(argv[[1]]);");
    }

    @Test
    public void testisfunction6() {
        assertEval("argv <- list(structure(list(Df = c(1L, 7L), `Sum Sq` = c(158.407612694902, 204.202165082876), `Mean Sq` = c(158.407612694902, 29.1717378689823), `F value` = c(5.43017400630538, NA), `Pr(>F)` = c(0.052592726218915, NA)), .Names = c('Df', 'Sum Sq', 'Mean Sq', 'F value', 'Pr(>F)'), row.names = c('depression', 'Residuals'), class = c('anova', 'data.frame'), heading = c('Analysis of Variance Table\\n', 'Response: weight')));is.function(argv[[1]]);");
    }

    @Test
    public void testisfunction7() {
        assertEval("argv <- list(structure(list(GNP.deflator = c(83, 88.5, 88.2, 89.5, 96.2, 98.1, 99, 100, 101.2, 104.6, 108.4, 110.8, 112.6, 114.2, 115.7, 116.9), GNP = c(234.289, 259.426, 258.054, 284.599, 328.975, 346.999, 365.385, 363.112, 397.469, 419.18, 442.769, 444.546, 482.704, 502.601, 518.173, 554.894), Unemployed = c(235.6, 232.5, 368.2, 335.1, 209.9, 193.2, 187, 357.8, 290.4, 282.2, 293.6, 468.1, 381.3, 393.1, 480.6, 400.7), Armed.Forces = c(159, 145.6, 161.6, 165, 309.9, 359.4, 354.7, 335, 304.8, 285.7, 279.8, 263.7, 255.2, 251.4, 257.2, 282.7), Population = c(107.608, 108.632, 109.773, 110.929, 112.075, 113.27, 115.094, 116.219, 117.388, 118.734, 120.445, 121.95, 123.366, 125.368, 127.852, 130.081), Year = 1947:1962, Employed = c(60.323, 61.122, 60.171, 61.187, 63.221, 63.639, 64.989, 63.761, 66.019, 67.857, 68.169, 66.513, 68.655, 69.564, 69.331, 70.551)), .Names = c('GNP.deflator', 'GNP', 'Unemployed', 'Armed.Forces', 'Population', 'Year', 'Employed'), row.names = 1947:1962, class = 'data.frame'));is.function(argv[[1]]);");
    }

    @Test
    public void testisfunction8() {
        assertEval("argv <- list(structure(c(112, 118, 132, 129, 121, 135, 148, 148, 136, 119, 104, 118, 115, 126, 141, 135, 125, 149, 170, 170, 158, 133, 114, 140, 145, 150, 178, 163, 172, 178, 199, 199, 184, 162, 146, 166, 171, 180, 193, 181, 183, 218, 230, 242, 209, 191, 172, 194, 196, 196, 236, 235, 229, 243, 264, 272, 237, 211, 180, 201, 204, 188, 235, 227, 234, 264, 302, 293, 259, 229, 203, 229, 242, 233, 267, 269, 270, 315, 364, 347, 312, 274, 237, 278, 284, 277, 317, 313, 318, 374, 413, 405, 355, 306, 271, 306, 315, 301, 356, 348, 355, 422, 465, 467, 404, 347, 305, 336, 340, 318, 362, 348, 363, 435, 491, 505, 404, 359, 310, 337, 360, 342, 406, 396, 420, 472, 548, 559, 463, 407, 362, 405, 417, 391, 419, 461, 472, 535, 622, 606, 508, 461, 390, 432), .Tsp = c(1949, 1960.91666666667, 12), class = 'ts'));is.function(argv[[1]]);");
    }

    @Test
    public void testisfunction9() {
        assertEval("argv <- list(structure(3.14159265358979, class = structure('3.14159265358979', class = 'testit')));is.function(argv[[1]]);");
    }

    @Test
    public void testisfunction10() {
        assertEval("argv <- list(structure(c(FALSE, TRUE, FALSE, TRUE, TRUE, FALSE, TRUE, TRUE, TRUE, FALSE, TRUE, FALSE, TRUE, TRUE, FALSE, TRUE, FALSE, FALSE, FALSE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE), .Dim = c(5L, 5L)));is.function(argv[[1]]);");
    }

    @Test
    public void testisfunction11() {
        assertEval("argv <- list(c(0.2853725+0.3927816i, -0.07283992154231+0.224178134292i, -0.10883955678256+0.035364093700981i, -0.0449501817243521-0.0326582354266614i, 8.2299281e-09-2.69753665872767e-02i, 0.0105954299973322-0.0076980245688633i, 0.00604728675391113+0.00196488543076221i, 0.00095395849586903+0.00293598723445021i, -0.00088096824266454+0.00121254736140417i, -7.27670402517897e-04-4.44010655e-10i, -2.07656947543323e-04-2.85815671682054e-04i, 5.3003554565545e-05-1.6312776087427e-04i, 7.9199339795869e-05-2.57333559721505e-05i, 3.27089023280074e-05+2.37644512768026e-05i, -1.79660253e-11+1.96291758626278e-05i, -7.70998422901389e-06+5.60161993213361e-06i, -4.4004307139296e-06-1.42979165736404e-06i, -6.9416605906477e-07-2.13643143624753e-06i, 6.4105505412914e-07-8.82334435385704e-07i, 5.29504214700362e-07+6.46186824e-13i));is.function(argv[[1]]);");
    }

    @Test
    public void testisfunction12() {
        assertEval("argv <- list(structure(c(9, 13, 13, 18, 23, 28, 31, 34, 45, 48, 161, 5, 5, 8, 8, 12, 16, 23, 27, 30, 33, 43, 45, 1, 1, 0, 1, 1, 0, 1, 1, 0, 1, 0, 1, 1, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1), .Dim = c(23L, 2L), .Dimnames = list(NULL, c('time', 'status')), type = 'right', class = 'Surv'));is.function(argv[[1]]);");
    }

    @Test
    public void testisfunction13() {
        assertEval("argv <- list(structure(list(), .Names = character(0), row.names = integer(0), class = 'data.frame'));is.function(argv[[1]]);");
    }

    @Test
    public void testisfunction14() {
        assertEval("argv <- list(structure(list(x = c(1, 1, 1, 1, 1, 1, 1, 1, 1, 1), y = c(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), fac = structure(c(1L, 3L, 2L, 3L, 3L, 1L, 2L, 3L, 2L, 2L), .Label = c('A', 'B', 'C'), class = 'factor')), .Names = c('x', 'y', 'fac'), row.names = c(NA, -10L), class = 'data.frame'));is.function(argv[[1]]);");
    }

    @Test
    public void testisfunction15() {
        assertEval("argv <- list(structure(function (x) standardGeneric('dim', .Primitive('dim')), generic = structure('dim', package = 'base'), package = 'base', group = list(), valueClass = character(0), signature = 'x', default = .Primitive('dim'), skeleton = quote(.Primitive('dim')(x)), class = structure('standardGeneric', package = 'methods')));is.function(argv[[1]]);");
    }

    @Test
    public void testisfunction16() {
        assertEval("argv <- list(structure(function (x, na.rm = FALSE, dims = 1, ...) standardGeneric('rowMeans'), generic = structure('rowMeans', package = 'base'), package = 'base', group = list(), valueClass = character(0), signature = c('x', 'na.rm', 'dims'), default = structure(function (x, na.rm = FALSE, dims = 1, ...) base::rowMeans(x, na.rm = na.rm, dims = dims, ...), target = structure('ANY', class = structure('signature', package = 'methods'), .Names = 'x', package = 'methods'), defined = structure('ANY', class = structure('signature', package = 'methods'), .Names = 'x', package = 'methods'), generic = structure('rowMeans', package = 'base'), class = structure('derivedDefaultMethod', package = 'methods')), skeleton = quote((function (x, na.rm = FALSE, dims = 1, ...) base::rowMeans(x, na.rm = na.rm, dims = dims, ...))(x, na.rm, dims, ...)), class = structure('standardGeneric', package = 'methods')));is.function(argv[[1]]);");
    }

    @Test
    public void testisfunction17() {
        assertEval("argv <- list(structure(c(2L, 1L, 3L), .Label = c('1', '2', NA), class = c('ordered', 'factor')));is.function(argv[[1]]);");
    }

    @Test
    public void testisfunction18() {
        assertEval("argv <- list(structure(list(usr = c(0.568, 1.432, -1.08, 1.08), xaxp = c(0.6, 1.4, 4), yaxp = c(-1, 1, 4)), .Names = c('usr', 'xaxp', 'yaxp')));is.function(argv[[1]]);");
    }

    @Test
    public void testisfunction19() {
        assertEval("argv <- list(structure(list(double.eps = 2.22044604925031e-16, double.neg.eps = 1.11022302462516e-16, double.xmin = 2.2250738585072e-308, double.xmax = 1.79769313486232e+308, double.base = 2L, double.digits = 53L, double.rounding = 5L, double.guard = 0L, double.ulp.digits = -52L, double.neg.ulp.digits = -53L, double.exponent = 11L, double.min.exp = -1022L, double.max.exp = 1024L, integer.max = 2147483647L, sizeof.long = 8L, sizeof.longlong = 8L, sizeof.longdouble = 16L, sizeof.pointer = 8L), .Names = c('double.eps', 'double.neg.eps', 'double.xmin', 'double.xmax', 'double.base', 'double.digits', 'double.rounding', 'double.guard', 'double.ulp.digits', 'double.neg.ulp.digits', 'double.exponent', 'double.min.exp', 'double.max.exp', 'integer.max', 'sizeof.long', 'sizeof.longlong', 'sizeof.longdouble', 'sizeof.pointer')));is.function(argv[[1]]);");
    }

    @Test
    public void testisfunction20() {
        assertEval("argv <- list(structure(list(extra = c(0.7, -1.6, -0.2, -1.2, -0.1, 3.4, 3.7, 0.8, 0, 2, 1.9, 0.8, 1.1, 0.1, -0.1, 4.4, 5.5, 1.6, 4.6, 3.4), group = structure(c(1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L), .Label = c('1', '2'), class = 'factor'), ID = structure(c(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L), .Label = c('1', '2', '3', '4', '5', '6', '7', '8', '9', '10'), class = 'factor')), .Names = c('extra', 'group', 'ID'), row.names = c(NA, -20L), class = 'data.frame'));is.function(argv[[1]]);");
    }

    @Test
    public void testisfunction22() {
        assertEval(Ignored.Unknown,
                        "argv <- list(function(e1, e2) {    ok <- switch(.Generic, `<` = , `>` = , `<=` = , `>=` = ,         `==` = , `!=` = TRUE, FALSE)    if (!ok) {        warning(sprintf(\''%s' is not meaningful for ordered factors',             .Generic))        return(rep.int(NA, max(length(e1), if (!missing(e2)) length(e2))))    }    if (.Generic %in% c('==', '!=')) return(NextMethod(.Generic))    nas <- is.na(e1) | is.na(e2)    ord1 <- FALSE    ord2 <- FALSE    if (nzchar(.Method[1L])) {        l1 <- levels(e1)        ord1 <- TRUE    }    if (nzchar(.Method[2L])) {        l2 <- levels(e2)        ord2 <- TRUE    }    if (all(nzchar(.Method)) && (length(l1) != length(l2) ||         !all(l2 == l1))) stop('level sets of factors are different')    if (ord1 && ord2) {        e1 <- as.integer(e1)        e2 <- as.integer(e2)    } else if (!ord1) {        e1 <- match(e1, l2)        e2 <- as.integer(e2)    } else if (!ord2) {        e2 <- match(e2, l1)        e1 <- as.integer(e1)    }    value <- get(.Generic, mode = 'function')(e1, e2)    value[nas] <- NA    value});"
                                        + "do.call('is.function', argv)");
    }

    @Test
    public void testisfunction23() {
        assertEval(Ignored.Unknown,
                        "argv <- list(function(x, width = 0.9 * getOption('width'), indent = 0,     exdent = 0, prefix = '', simplify = TRUE, initial = prefix) {    if (!is.character(x)) x <- as.character(x)    indentString <- paste(rep.int(' ', indent), collapse = '')    exdentString <- paste(rep.int(' ', exdent), collapse = '')    y <- list()    UB <- TRUE    if (all(Encoding(x) == 'UTF-8')) UB <- FALSE else {        enc <- Encoding(x) %in% c('latin1', 'UTF-8')        if (length(enc)) x[enc] <- enc2native(x[enc])    }    z <- lapply(strsplit(x, '\\n[ \\t\\n]*\\n', perl = TRUE, useBytes = UB),         strsplit, '[ \\t\\n]', perl = TRUE, useBytes = UB)    for (i in seq_along(z)) {        yi <- character()        for (j in seq_along(z[[i]])) {            words <- z[[i]][[j]]            nc <- nchar(words, type = 'w')            if (anyNA(nc)) {                nc0 <- nchar(words, type = 'b')                nc[is.na(nc)] <- nc0[is.na(nc)]            }            if (any(nc == 0L)) {                zLenInd <- which(nc == 0L)                zLenInd <- zLenInd[!(zLenInd %in% (grep('[.?!][)\\'']{0,1}$',                   words, perl = TRUE, useBytes = TRUE) + 1L))]                if (length(zLenInd)) {                  words <- words[-zLenInd]                  nc <- nc[-zLenInd]                }            }            if (!length(words)) {                yi <- c(yi, '', initial)                next            }            currentIndex <- 0L            lowerBlockIndex <- 1L            upperBlockIndex <- integer()            lens <- cumsum(nc + 1L)            first <- TRUE            maxLength <- width - nchar(initial, type = 'w') -                 indent            while (length(lens)) {                k <- max(sum(lens <= maxLength), 1L)                if (first) {                  first <- FALSE                  maxLength <- width - nchar(prefix, type = 'w') -                     exdent                }                currentIndex <- currentIndex + k                if (nc[currentIndex] == 0L) upperBlockIndex <- c(upperBlockIndex,                   currentIndex - 1L) else upperBlockIndex <- c(upperBlockIndex,                   currentIndex)                if (length(lens) > k) {                  if (nc[currentIndex + 1L] == 0L) {                    currentIndex <- currentIndex + 1L                    k <- k + 1L                  }                  lowerBlockIndex <- c(lowerBlockIndex, currentIndex +                     1L)                }                if (length(lens) > k) lens <- lens[-seq_len(k)] -                   lens[k] else lens <- NULL            }            nBlocks <- length(upperBlockIndex)            s <- paste0(c(initial, rep.int(prefix, nBlocks -                 1L)), c(indentString, rep.int(exdentString, nBlocks -                 1L)))            initial <- prefix            for (k in seq_len(nBlocks)) s[k] <- paste0(s[k],                 paste(words[lowerBlockIndex[k]:upperBlockIndex[k]],                   collapse = ' '))            yi <- c(yi, s, prefix)        }        y <- if (length(yi)) c(y, list(yi[-length(yi)])) else c(y,             '')    }    if (simplify) y <- as.character(unlist(y))    y});"
                                        + "do.call('is.function', argv)");
    }

    @Test
    public void testisfunction24() {
        assertEval(Ignored.Unknown,
                        "argv <- list(function(..., na.rm) {    coerceTimeUnit <- function(x) {        as.vector(switch(attr(x, 'units'), secs = x, mins = 60 *             x, hours = 60 * 60 * x, days = 60 * 60 * 24 * x,             weeks = 60 * 60 * 24 * 7 * x))    }    ok <- switch(.Generic, max = , min = , sum = , range = TRUE,         FALSE)    if (!ok) stop(gettextf(\''%s' not defined for \\'difftime\\' objects',         .Generic), domain = NA)    x <- list(...)    Nargs <- length(x)    if (Nargs == 0) {        .difftime(do.call(.Generic), 'secs')    } else {        units <- sapply(x, function(x) attr(x, 'units'))        if (all(units == units[1L])) {            args <- c(lapply(x, as.vector), na.rm = na.rm)        } else {            args <- c(lapply(x, coerceTimeUnit), na.rm = na.rm)            units <- 'secs'        }        .difftime(do.call(.Generic, args), units[[1L]])    }});"
                                        + "do.call('is.function', argv)");
    }

    @Test
    public void testisfunction25() {
        assertEval("argv <- list(structure(list(platform = 'x86_64-unknown-linux-gnu',     arch = 'x86_64', os = 'linux-gnu', system = 'x86_64, linux-gnu',     status = '', major = '3', minor = '1.1', year = '2014', month = '07',     day = '10', `svn rev` = '66115', language = 'R', version.string = 'R version 3.1.1 (2014-07-10)',     nickname = 'Sock it to Me'), .Names = c('platform', 'arch',     'os', 'system', 'status', 'major', 'minor', 'year', 'month',     'day', 'svn rev', 'language', 'version.string', 'nickname'),     class = 'simple.list'));"
                        + "do.call('is.function', argv)");
    }

    @Test
    public void testisfunction26() {
        assertEval(Ignored.Unknown,
                        "argv <- list(function(x, format = '', usetz = FALSE, ...) {    if (!inherits(x, 'POSIXlt')) stop('wrong class')    if (format == '') {        times <- unlist(unclass(x)[1L:3L])        secs <- x$sec        secs <- secs[!is.na(secs)]        np <- getOption('digits.secs')        if (is.null(np)) np <- 0L else np <- min(6L, np)        if (np >= 1L) for (i in seq_len(np) - 1L) if (all(abs(secs -             round(secs, i)) < 1e-06)) {            np <- i            break        }        format <- if (all(times[!is.na(times)] == 0)) '%Y-%m-%d' else if (np ==             0L) '%Y-%m-%d %H:%M:%S' else paste0('%Y-%m-%d %H:%M:%OS',             np)    }    y <- .Internal(format.POSIXlt(x, format, usetz))    names(y) <- names(x$year)    y});"
                                        + "do.call('is.function', argv)");
    }

    @Test
    public void testisfunction27() {
        assertEval(Ignored.Unknown,
                        "argv <- list(function(name, cond = NULL) {    i <- 1L    repeat {        r <- .Internal(.getRestart(i))        if (is.null(r)) return(NULL) else if (name == r[[1L]] &&             (is.null(cond) || is.null(r$test) || r$test(cond))) return(r) else i <- i +             1L    }});"
                                        + "do.call('is.function', argv)");
    }

    @Test
    public void testisfunction28() {
        assertEval(Ignored.Unknown,
                        "argv <- list(function(x, width = 12, ...) {    if (is.character(x)) return(format.default(x, ...))    if (is.null(width)) width = 12L    n <- length(x)    rvec <- rep.int(NA_character_, n)    for (i in seq_len(n)) {        y <- x[[i]]        cl <- oldClass(y)        if (m <- match('AsIs', cl, 0L)) oldClass(y) <- cl[-m]        rvec[i] <- toString(y, width = width, ...)    }    dim(rvec) <- dim(x)    dimnames(rvec) <- dimnames(x)    format.default(rvec, justify = 'right')});"
                                        + "do.call('is.function', argv)");
    }

    @Test
    public void testisfunction29() {
        assertEval("argv <- list(function(x, i, ...) structure(NextMethod('['), class = class(x)));do.call('is.function', argv)");
    }

    @Test
    public void testisfunction31() {
        assertEval(Ignored.Unknown,
                        "argv <- list(function(qr, y) {    if (!is.qr(qr)) stop('first argument must be a QR decomposition')    n <- as.integer(nrow(qr$qr))    if (is.na(n)) stop('invalid nrow(qr$qr)')    p <- as.integer(ncol(qr$qr))    if (is.na(p)) stop('invalid ncol(qr$qr)')    k <- as.integer(qr$rank)    if (is.na(k)) stop('invalid ncol(qr$rank)')    im <- is.matrix(y)    if (!im) y <- as.matrix(y)    ny <- as.integer(ncol(y))    if (is.na(ny)) stop('invalid ncol(y)')    if (p == 0L) return(if (im) matrix(0, p, ny) else numeric())    ix <- if (p > n) c(seq_len(n), rep(NA, p - n)) else seq_len(p)    if (is.complex(qr$qr)) {        coef <- matrix(NA_complex_, nrow = p, ncol = ny)        coef[qr$pivot, ] <- .Internal(qr_coef_cmplx(qr, y))[ix,             ]        return(if (im) coef else c(coef))    }    if (isTRUE(attr(qr, 'useLAPACK'))) {        coef <- matrix(NA_real_, nrow = p, ncol = ny)        coef[qr$pivot, ] <- .Internal(qr_coef_real(qr, y))[ix,             ]        return(if (im) coef else c(coef))    }    if (k == 0L) return(if (im) matrix(NA, p, ny) else rep.int(NA,         p))    storage.mode(y) <- 'double'    if (nrow(y) != n) stop(\''qr' and 'y' must have the same number of rows')    z <- .Fortran(.F_dqrcf, as.double(qr$qr), n, k, as.double(qr$qraux),         y, ny, coef = matrix(0, nrow = k, ncol = ny), info = integer(1L),         NAOK = TRUE)[c('coef', 'info')]    if (z$info) stop('exact singularity in 'qr.coef'')    if (k < p) {        coef <- matrix(NA_real_, nrow = p, ncol = ny)        coef[qr$pivot[seq_len(k)], ] <- z$coef    } else coef <- z$coef    if (!is.null(nam <- colnames(qr$qr))) if (k < p) rownames(coef)[qr$pivot] <- nam else rownames(coef) <- nam    if (im && !is.null(nam <- colnames(y))) colnames(coef) <- nam    if (im) coef else drop(coef)});"
                                        + "do.call('is.function', argv)");
    }

    @Test
    public void testisfunction32() {
        assertEval("argv <- list(function(cpu = Inf, elapsed = Inf, transient = FALSE) .Internal(setTimeLimit(cpu,     elapsed, transient)));do.call('is.function', argv)");
    }
}
