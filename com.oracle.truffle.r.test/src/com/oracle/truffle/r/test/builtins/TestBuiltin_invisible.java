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
public class TestBuiltin_invisible extends TestBase {

    @Test
    public void testinvisible1() {
        assertEval("argv <- list(c(3.14159265358977, 3.14159265358981));invisible(argv[[1]]);");
    }

    @Test
    public void testinvisible2() {
        assertEval(Output.ContainsError,
                        "argv <- list(structure('Error in cov(rnorm(10), NULL) : \\n  supply both 'x' and 'y' or a matrix-like 'x'\\n', class = 'try-error', condition = structure(list(message = 'supply both 'x' and 'y' or a matrix-like 'x'', call = quote(cov(rnorm(10), NULL))), .Names = c('message', 'call'), class = c('simpleError', 'error', 'condition'))));invisible(argv[[1]]);");
    }

    @Test
    public void testinvisible3() {
        assertEval("argv <- list(quote(Y ~ X));invisible(argv[[1]]);");
    }

    @Test
    public void testinvisible4() {
        assertEval("argv <- list(structure(list(height = numeric(0), weight = numeric(0)), .Names = c('height', 'weight'), row.names = integer(0), class = 'data.frame'));invisible(argv[[1]]);");
    }

    @Test
    public void testinvisible5() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(c('Min.   : 1.000  ', '1st Qu.: 9.000  ', 'Median :18.000  ', 'Mean   :14.742  ', '3rd Qu.:20.000  ', 'Max.   :23.000  ', NA, 'Min.   :5.0000  ', '1st Qu.:5.3000  ', 'Median :6.1000  ', 'Mean   :6.0841  ', '3rd Qu.:6.6000  ', 'Max.   :7.7000  ', NA, 'Min.   :  1.000  ', '1st Qu.: 24.250  ', 'Median : 56.500  ', 'Mean   : 56.928  ', '3rd Qu.: 86.750  ', 'Max.   :117.000  ', 'NA's   :16  ', 'Min.   :  0.500  ', '1st Qu.: 11.325  ', 'Median : 23.400  ', 'Mean   : 45.603  ', '3rd Qu.: 47.550  ', 'Max.   :370.000  ', NA, 'Min.   :0.00300  ', '1st Qu.:0.04425  ', 'Median :0.11300  ', 'Mean   :0.15422  ', '3rd Qu.:0.21925  ', 'Max.   :0.81000  ', NA), .Dim = c(7L, 5L), .Dimnames = list(c('', '', '', '', '', '', ''), c('    event', '     mag', '   station', '     dist', '    accel')), class = 'table'));invisible(argv[[1]]);");
    }

    @Test
    public void testinvisible6() {
        assertEval("argv <- list(structure(list(call = quote(lm(formula = y ~ x1 + x2 + x3)), terms = quote(y ~ x1 + x2 + x3), residuals = structure(c(0.224762433374997, 0.4813346401898, -0.548705796690786, -0.873306430909872, 0.3255545927283, -0.288240908441576, 0.530823516045489, -0.0649703574297026, 1.2699009772491, -1.05715266611575), .Names = c('1', '2', '3', '4', '5', '6', '7', '8', '9', '10')), coefficients = structure(c(1.47191076131574, 0.586694550701453, 0.258706725324317, 0.948371836939988, 0.396080061109718, 0.350912037541581, 1.55203972111298, 1.48125242421363, 0.737240953991673, 0.164593338447767, 0.182090654313858, 0.484947927602608), .Dim = 3:4, .Dimnames = list(c('(Intercept)', 'x1', 'x3'), c('Estimate', 'Std. Error', 't value', 'Pr(>|t|)'))), aliased = structure(c(FALSE, FALSE, TRUE, FALSE), .Names = c('(Intercept)', 'x1', 'x2', 'x3')), sigma = 0.806334473232766, df = c(3L, 7L, 4L), r.squared = 0.932605950232242, adj.r.squared = 0.913350507441455, fstatistic = structure(c(48.4333681840033, 2, 7), .Names = c('value', 'numdf', 'dendf')), cov.unscaled = structure(c(1.38333333333333, -0.525000000000001, 0.416666666666667, -0.525000000000001, 0.241287878787879, -0.208333333333334, 0.416666666666667, -0.208333333333334, 0.18939393939394), .Dim = c(3L, 3L), .Dimnames = list(c('(Intercept)', 'x1', 'x3'), c('(Intercept)', 'x1', 'x3'))), correlation = structure(c(1, -0.908715905467124, 0.814033538872717, -0.908715905467124, 1, -0.974558628915209, 0.814033538872717, -0.974558628915209, 1), .Dim = c(3L, 3L), .Dimnames = list(    c('(Intercept)', 'x1', 'x3'), c('(Intercept)', 'x1', 'x3'))), symbolic.cor = FALSE), .Names = c('call', 'terms', 'residuals', 'coefficients', 'aliased', 'sigma', 'df', 'r.squared', 'adj.r.squared', 'fstatistic', 'cov.unscaled', 'correlation', 'symbolic.cor'), class = 'summary.lm'));invisible(argv[[1]]);");
    }

    @Test
    public void testinvisible7() {
        assertEval("argv <- list(structure(list(call = quote(lm(formula = y ~ 0)), terms = quote(y ~ 0), aliased = logical(0), residuals = structure(c(-0.667819876370237, 0.170711734013213, 0.552921941721332, -0.253162069270378, -0.00786394222146348, 0.0246733498130512, 0.0730305465518564, -1.36919169254062, 0.0881443844426084, -0.0834190388782434), .Names = c('1', '2', '3', '4', '5', '6', '7', '8', '9', '10')), df = c(0L, 10L, 0L), coefficients = structure(logical(0), .Dim = c(0L, 4L), .Dimnames = list(NULL, c('Estimate', 'Std. Error', 't value', 'Pr(>|t|)'))), sigma = 0.523484262069588, adj.r.squared = 0, r.squared = 0), .Names = c('call', 'terms', 'aliased', 'residuals', 'df', 'coefficients', 'sigma', 'adj.r.squared', 'r.squared'), class = 'summary.lm'));invisible(argv[[1]]);");
    }

    @Test
    public void testinvisible8() {
        assertEval("argv <- list(structure(list(width = 80L, minIndent = 10L, extraIndent = 4L, sectionIndent = 5L, sectionExtra = 2L, itemBullet = '• ', enumFormat = function (n) sprintf('%d. ', n), showURLs = FALSE, code_quote = TRUE, underline_titles = FALSE), .Names = c('width', 'minIndent', 'extraIndent', 'sectionIndent', 'sectionExtra', 'itemBullet', 'enumFormat', 'showURLs', 'code_quote', 'underline_titles')));invisible(argv[[1]]);");
    }

    @Test
    public void testinvisible9() {
        assertEval("argv <- list(quote(breaks ~ (wool + tension)^2));invisible(argv[[1]]);");
    }

    @Test
    public void testinvisible10() {
        assertEval("argv <- list(structure(list(surname = structure(1:5, .Label = c('McNeil', 'Ripley', 'Tierney', 'Tukey', 'Venables'), class = 'factor'), nationality = structure(c(1L, 2L, 3L, 3L, 1L), .Label = c('Australia', 'UK', 'US'), class = 'factor'), deceased = structure(c(1L, 1L, 1L, 2L, 1L), .Label = c('no', 'yes'), class = 'factor'), title = structure(c(NA_integer_, NA_integer_, NA_integer_, NA_integer_, NA_integer_), .Label = c('An Introduction to R', 'Exploratory Data Analysis', 'Interactive Data Analysis', 'LISP-STAT', 'Modern Applied Statistics ...', 'Spatial Statistics', 'Stochastic Simulation'), class = 'factor'), other.author = structure(c(NA_integer_, NA_integer_, NA_integer_, NA_integer_, NA_integer_), .Label = c('Ripley', 'Venables & Smith'), class = 'factor')), .Names = c('surname', 'nationality', 'deceased', 'title', 'other.author'), row.names = c(NA, -5L), class = 'data.frame'));invisible(argv[[1]]);");
    }

    @Test
    public void testinvisible11() {
        assertEval("argv <- list(structure('Error in `[.data.frame`(dd, , \\\'x\\\') : undefined columns selected\\n', class = 'try-error', condition = structure(list(message = 'undefined columns selected', call = quote(`[.data.frame`(dd, , 'x'))), .Names = c('message', 'call'), class = c('simpleError', 'error', 'condition'))));invisible(argv[[1]]);");
    }

    @Test
    public void testinvisible12() {
        assertEval("argv <- list(structure(list(value = structure(c(NA, NA, 1L, 9L), .Names = c('size', 'current', 'direction', 'eval_depth')), visible = TRUE), .Names = c('value', 'visible')));invisible(argv[[1]]);");
    }

    @Test
    public void testinvisible13() {
        assertEval("argv <- list(structure(function (...) new('test1', ...), className = structure('test1', package = '.GlobalEnv'), package = '.GlobalEnv', class = structure('classGeneratorFunction', package = 'methods')));invisible(argv[[1]]);");
    }

    @Test
    public void testinvisible14() {
        assertEval("argv <- list(structure(list(coefficients = structure(c(-0.0880891704401362, -0.508170309402877, -0.00510235947825228, 0.0737329622006759), .Names = c('(Intercept)', 'x1', 'z', 'x1:z')), residuals = structure(c(0.471500137591588, -0.418206002310214, -1.08038471222353, -0.582889907355648, 0.671048026430597, 1.41161034263987, 0.0130575334430522, 0.598273046028054, -0.0752209852417045, -1.00878747900206), .Names = c('1', '2', '3', '4', '5', '6', '7', '8', '9', '10')), effects = structure(c(0.483887391035467, -0.316505532770654, -0.0456368905560498, -0.659487662652535, 0.502868792132386, 1.20242722895332, -0.301792379913696, 0.0429789614006214, -0.536741577656989, -1.91019253457038), .Names = c('(Intercept)', 'x1', 'z', 'x1:z', '', '', '', '', '', '')), rank = 4L, fitted.values = structure(c(-0.527628877120589, 0.262410495604884, -0.390367671675741, 0.104739852247028, -0.253106466230895, -0.0529307911108283, -0.115845260786048, -0.210601434468685, 0.0214159446587994, -0.368272077826542), .Names = c('1', '2', '3', '4', '5', '6', '7', '8', '9', '10')), assign = 0:3, qr = structure(list(qr = structure(c(-3.16227766016838, 0.316227766016838, 0.316227766016838, 0.316227766016838, 0.316227766016838, 0.316227766016838, 0.316227766016838, 0.316227766016838, 0.316227766016838, 0.316227766016838, 0, 3.16227766016838, -0.240253073352042, 0.392202458681634, -0.240253073352042, 0.392202458681634, -0.240253073352042, 0.392202458681634, -0.240253073352042, 0.392202458681634, -17.3925271309261, -1.58113883008419, 8.94427190999916, 0.0204447427551466, -0.048810308101025, -0.203162054994832, -0.272417105851004, -0.426768852744811, -0.496023903600983, -0.65037565049479, 1.58113883008419, 17.3925271309261, 2.77555756156289e-17, -8.94427190999916, 0.202312619197469, -0.0523458957441388, 0.422028033632482, -0.279844076809084, 0.641743448067495, -0.507342257874029), .Dim = c(10L, 4L), .Dimnames = list(c('1', '2', '3', '4', '5', '6', '7', '8', '9', '10'), c('(Intercept)', 'x1', 'z', 'x1:z')), assign = 0:3, contrasts = structure(list(    x = 'contr.helmert'), .Names = 'x')), qraux = c(1.31622776601684, 1.39220245868163, 1.17479648964895, 1.17515228532081), pivot = 1:4, tol = 1e-07, rank = 4L), .Names = c('qr', 'qraux', 'pivot', 'tol', 'rank'), class = 'qr'), df.residual = 6L, contrasts = structure(list(x = 'contr.helmert'), .Names = 'x'), xlevels = structure(list(), .Names = character(0)), call = quote(lm(formula = y ~ x * z)), terms = quote(y ~ x * z), model = structure(list(y = c(-0.0561287395290008, -0.155795506705329, -1.47075238389927, -0.47815005510862, 0.417941560199702, 1.35867955152904, -0.102787727342996, 0.387671611559369, -0.0538050405829051, -1.37705955682861), x = c(TRUE, FALSE, TRUE, FALSE, TRUE, FALSE, TRUE, FALSE, TRUE, FALSE), z = 1:10), .Names = c('y', 'x', 'z'), terms = quote(y ~ x * z), row.names = c(NA, 10L), class = 'data.frame')), .Names = c('coefficients', 'residuals', 'effects', 'rank', 'fitted.values', 'assign', 'qr', 'df.residual', 'contrasts', 'xlevels', 'call', 'terms', 'model'), class = 'lm'));invisible(argv[[1]]);");
    }

    @Test
    public void testinvisible15() {
        assertEval("argv <- list(structure('Error in scan(file, what, nmax, sep, dec, quote, skip, nlines, na.strings,  : \\n  line 1 did not have 4 elements\\n', class = 'try-error', condition = structure(list(message = 'line 1 did not have 4 elements', call = quote(scan(file, what, nmax, sep, dec, quote, skip, nlines, na.strings, flush, fill, strip.white, quiet, blank.lines.skip, multi.line, comment.char, allowEscapes, encoding))), .Names = c('message', 'call'), class = c('simpleError', 'error', 'condition'))));invisible(argv[[1]]);");
    }

    @Test
    public void testinvisible16() {
        assertEval(Output.ContainsError,
                        "argv <- list(structure('Error in cor(Z[, FALSE], use = \\\'pairwise.complete.obs\\\', method = \\\'kendall\\\') : \\n  'x' is empty\\n', class = 'try-error', condition = structure(list(message = \''x' is empty', call = quote(cor(Z[, FALSE], use = 'pairwise.complete.obs', method = 'kendall'))), .Names = c('message', 'call'), class = c('simpleError', 'error', 'condition'))));invisible(argv[[1]]);");
    }

    @Test
    public void testinvisible17() {
        assertEval("argv <- list(structure(list(A = c(1L, NA, 1L), B = c(1.1, NA, 2), C = c(1.1+0i, NA, 3+0i), D = c(NA, NA, NA), E = c(FALSE, NA, TRUE), F = structure(c(1L, NA, 2L), .Label = c('abc', 'def'), class = 'factor')), .Names = c('A', 'B', 'C', 'D', 'E', 'F'), class = 'data.frame', row.names = c('1', '2', '3')));invisible(argv[[1]]);");
    }

    @Test
    public void testinvisible18() {
        assertEval("argv <- list(structure(c(3, 8), .Dim = 2L, .Dimnames = structure(list(g = c('1', '2')), .Names = 'g'), call = quote(by.data.frame(data = X, INDICES = g, FUN = colMeans)), class = 'by'));invisible(argv[[1]]);");
    }

    @Test
    public void testinvisible19() {
        assertEval("argv <- list(structure('Error in rnorm(2, c(1, NA)) : (converted from warning) NAs produced\\n', class = 'try-error', condition = structure(list(message = '(converted from warning) NAs produced', call = quote(rnorm(2, c(1, NA)))), .Names = c('message', 'call'), class = c('simpleError', 'error', 'condition'))));invisible(argv[[1]]);");
    }

    @Test
    public void testinvisible20() {
        assertEval("argv <- list(structure(list(z = structure(c(1395082040.29392, 1395082040.29392, 1395082040.29392, 1395082040.29392, 1395082040.29392), class = c('AsIs', 'POSIXct', 'POSIXt'))), .Names = 'z', row.names = c(NA, -5L), class = 'data.frame'));invisible(argv[[1]]);");
    }

    @Test
    public void testinvisible21() {
        assertEval("argv <- list(quote(~a + b:c + d + e + e:d));invisible(argv[[1]]);");
    }

    @Test
    public void testinvisible22() {
        assertEval("argv <- list(structure(list(tables = structure(list(`Grand mean` = 103.87323943662, N = structure(c(78.7365206866197, 98.5088731171753, 113.842206450509, 123.008873117175), .Dim = 4L, .Dimnames = structure(list(N = c('0.0cwt', '0.2cwt', '0.4cwt', '0.6cwt')), .Names = 'N'), class = 'mtable'), `V:N` = structure(c(79.5323303457107, 86.1989970123773, 69.7732394366197, 98.0323303457106, 108.032330345711, 89.1989970123773, 114.198997012377, 116.698997012377, 110.365663679044, 124.365663679044, 126.365663679044, 118.032330345711), .Dim = 3:4, .Dimnames = structure(list(V = c('Golden.rain', 'Marvellous', 'Victory'), N = c('0.0cwt', '0.2cwt', '0.4cwt', '0.6cwt')), .Names = c('V', 'N')), class = 'mtable')), .Names = c('Grand mean', 'N', 'V:N')), n = structure(list(N = structure(c(17, 18, 18, 18), .Dim = 4L, .Dimnames = structure(list(N = c('0.0cwt', '0.2cwt', '0.4cwt', '0.6cwt')), .Names = 'N')), `V:N` = structure(c(6, 6, 5, 6, 6, 6, 6, 6, 6, 6, 6, 6), .Dim = 3:4, .Dimnames = structure(list(V = c('Golden.rain', 'Marvellous', 'Victory'), N = c('0.0cwt', '0.2cwt', '0.4cwt', '0.6cwt')), .Names = c('V', 'N')))), .Names = c('N', 'V:N'))), .Names = c('tables', 'n'), type = 'means', class = c('tables_aov', 'list.of')));invisible(argv[[1]]);");
    }

    @Test
    public void testinvisible23() {
        assertEval("argv <- list(structure(list(A = 0:10, `NA` = 20:30), .Names = c('A', NA), class = 'data.frame', row.names = c(NA, -11L)));invisible(argv[[1]]);");
    }

    @Test
    public void testinvisible24() {
        assertEval("argv <- list(structure(c(-Inf, -Inf, -2.248e+263, -Inf, -3.777e+116, -1), .Names = c('Min.', '1st Qu.', 'Median', 'Mean', '3rd Qu.', 'Max.'), class = 'table'));invisible(argv[[1]]);");
    }

    @Test
    public void testinvisible25() {
        assertEval("argv <- list(structure(list(Df = c(1, 1, NA, 2), Deviance = c(12.2441566485997, 28.4640218366572, 32.825622681839, 32.4303239692005), AIC = c(73.9421143635373, 90.1619795515948, 92.5235803967766, 96.1282816841381)), .Names = c('Df', 'Deviance', 'AIC'), row.names = c('+ M.user', '+ Temp', '<none>', '+ Soft'), class = c('anova', 'data.frame')));invisible(argv[[1]]);");
    }

    @Test
    public void testinvisible26() {
        assertEval("argv <- list(c(-1, -0.5, 0, 0.5, 1));invisible(argv[[1]]);");
    }

    @Test
    public void testinvisible27() {
        assertEval("argv <- list(structure(c('Min.   :  5.00  ', '1st Qu.: 12.50  ', 'Median : 23.00  ', 'Mean   : 29.48  ', '3rd Qu.: 33.50  ', 'Max.   :161.00  ', 'Min.   :0.0000  ', '1st Qu.:1.0000  ', 'Median :1.0000  ', 'Mean   :0.7826  ', '3rd Qu.:1.0000  ', 'Max.   :1.0000  ', 'Maintained   :11  ', 'Nonmaintained:12  ', NA, NA, NA, NA), .Dim = c(6L, 3L), .Dimnames = list(c('', '', '', '', '', ''), c('     time', '    status', '            x')), class = 'table'));invisible(argv[[1]]);");
    }

    @Test
    public void testinvisible28() {
        assertEval("argv <- list(structure(list(latin1 = 0L, utf8 = 0L, bytes = 0L, unknown = structure(character(0), .Dim = c(0L, 2L), .Dimnames = list(NULL, c('non_ASCII', 'where')))), .Names = c('latin1', 'utf8', 'bytes', 'unknown'), class = 'check_package_datasets'));invisible(argv[[1]]);");
    }

    @Test
    public void testinvisible29() {
        assertEval("argv <- list(structure(NA, .Tsp = c(1, 1, 1), class = 'ts'));invisible(argv[[1]]);");
    }

    @Test
    public void testinvisible30() {
        assertEval("argv <- list(structure(list(), class = 'formula'));invisible(argv[[1]]);");
    }

    @Test
    public void testinvisible31() {
        assertEval("argv <- list(structure(list(strip.vp = structure(list(x = structure(0.5, unit = 'npc', valid.unit = 0L, class = 'unit'), y = structure(0.5, unit = 'npc', valid.unit = 0L, class = 'unit'), width = structure(1, unit = 'npc', valid.unit = 0L, class = 'unit'), height = structure(1, unit = 'npc', valid.unit = 0L, class = 'unit'), justification = 'centre', gp = structure(list(), class = 'gpar'), clip = FALSE, xscale = c(-0.0330971105140634, 1.03229244338581), yscale = c(0, 1), angle = 0, layout = NULL, layout.pos.row = c(1L, 1L), layout.pos.col = c(1L, 1L), valid.just = c(0.5, 0.5), valid.pos.row = c(1L, 1L), valid.pos.col = c(1L, 1L), name = 'GRID.VP.40'), .Names = c('x', 'y', 'width', 'height', 'justification', 'gp', 'clip', 'xscale', 'yscale', 'angle', 'layout', 'layout.pos.row', 'layout.pos.col', 'valid.just', 'valid.pos.row', 'valid.pos.col', 'name'), class = 'viewport'), plot.vp = structure(list(x = structure(0.5, unit = 'npc', valid.unit = 0L, class = 'unit'), y = structure(0.5, unit = 'npc', valid.unit = 0L, class = 'unit'),     width = structure(1, unit = 'npc', valid.unit = 0L, class = 'unit'), height = structure(1, unit = 'npc', valid.unit = 0L, class = 'unit'), justification = 'centre', gp = structure(list(), class = 'gpar'), clip = FALSE, xscale = c(-0.0330971105140634, 1.03229244338581), yscale = c(-0.0353837383445352, 1.04704589419998), angle = 0, layout = NULL, layout.pos.row = c(2L, 2L), layout.pos.col = c(1L, 1L), valid.just = c(0.5, 0.5), valid.pos.row = c(2L, 2L), valid.pos.col = c(1L, 1L), name = 'GRID.VP.41'), .Names = c('x', 'y', 'width', 'height', 'justification', 'gp', 'clip', 'xscale', 'yscale', 'angle', 'layout', 'layout.pos.row', 'layout.pos.col', 'valid.just', 'valid.pos.row', 'valid.pos.col', 'name'), class = 'viewport')), .Names = c('strip.vp', 'plot.vp')));invisible(argv[[1]]);");
    }

    @Test
    public void testinvisible32() {
        assertEval("argv <- list(structure(list(name = 'list', objs = structure(list(`package:base` = .Primitive('list'), .Primitive('list')), .Names = c('package:base', '')), where = c('package:base', 'namespace:base'), visible = c(TRUE, FALSE), dups = c(FALSE, TRUE)), .Names = c('name', 'objs', 'where', 'visible', 'dups'), class = 'getAnywhere'));invisible(argv[[1]]);");
    }

    @Test
    public void testinvisible33() {
        assertEval("argv <- list(structure(list(GNP.deflator = c(83, 88.5, 88.2, 89.5, 96.2, 98.1, 99, 100, 101.2, 104.6, 108.4, 110.8, 112.6, 114.2, 115.7, 116.9), GNP = c(234.289, 259.426, 258.054, 284.599, 328.975, 346.999, 365.385, 363.112, 397.469, 419.18, 442.769, 444.546, 482.704, 502.601, 518.173, 554.894), Unemployed = c(235.6, 232.5, 368.2, 335.1, 209.9, 193.2, 187, 357.8, 290.4, 282.2, 293.6, 468.1, 381.3, 393.1, 480.6, 400.7), Armed.Forces = c(159, 145.6, 161.6, 165, 309.9, 359.4, 354.7, 335, 304.8, 285.7, 279.8, 263.7, 255.2, 251.4, 257.2, 282.7), Population = c(107.608, 108.632, 109.773, 110.929, 112.075, 113.27, 115.094, 116.219, 117.388, 118.734, 120.445, 121.95, 123.366, 125.368, 127.852, 130.081), Year = 1947:1962, Employed = c(60.323, 61.122, 60.171, 61.187, 63.221, 63.639, 64.989, 63.761, 66.019, 67.857, 68.169, 66.513, 68.655, 69.564, 69.331, 70.551)), .Names = c('GNP.deflator', 'GNP', 'Unemployed', 'Armed.Forces', 'Population', 'Year', 'Employed'), row.names = 1947:1962, class = 'data.frame'));invisible(argv[[1]]);");
    }

    @Test
    public void testinvisible34() {
        assertEval("argv <- list(structure(list(sec = 59.7693939208984, min = 47L, hour = 18L, mday = 17L, mon = 2L, year = 114L, wday = 1L, yday = 75L, isdst = 0L), .Names = c('sec', 'min', 'hour', 'mday', 'mon', 'year', 'wday', 'yday', 'isdst'), class = c('POSIXlt', 'POSIXt'), tzone = 'GMT'));invisible(argv[[1]]);");
    }

    @Test
    public void testinvisible35() {
        assertEval("argv <- list(structure(list(x = structure(c(63.8079770211941, 64.1015289795127, 64.3950809378313, 64.6886328961499, 64.9821848544685, 65.2757368127871, 65.5692887711057, 65.8628407294243, 66.1563926877429, 66.4499446460616, 66.7434966043802, 67.0370485626988, 67.3306005210174, 67.624152479336, 67.9177044376546, 68.2112563959732, 68.5048083542918, 68.7983603126104, 69.091912270929, 69.3854642292476, 69.6790161875662, 69.9725681458849, 70.2661201042035, 70.5596720625221, 70.8532240208407, 71.1467759791593, 71.4403279374779, 71.7338798957965, 72.0274318541151, 72.3209838124337, 72.6145357707524, 72.908087729071, 73.2016396873896, 73.4951916457082, 73.7887436040268, 74.0822955623454, 74.375847520664, 74.6693994789826, 74.9629514373012, 75.2565033956198, 75.5500553539384, 75.843607312257, 76.1371592705757, 76.4307112288943, 76.7242631872129, 77.0178151455315, 77.3113671038501, 77.6049190621687, 77.8984710204873, 78.1920229788059), unit = 'native', valid.unit = 4L, class = 'unit'), y = structure(c(0.000292389503184205, 0.000897790147984954, 0.00234624782100963, 0.00521720896677798, 0.00989423163518025, 0.015999825469344, 0.0221693602680603, 0.0266484406702544, 0.0287592128884921, 0.0302032637184832, 0.0349150884986298, 0.0473117449499264, 0.069811568153779, 0.101849712371392, 0.14014558800306, 0.179532924691013, 0.213121481011927, 0.233373692723354, 0.235396372946243, 0.221556776074102, 0.201658872746641, 0.187397555681655, 0.184299939839784, 0.187901304936084, 0.186879499085897, 0.171534710980926, 0.140953197828419, 0.103411084284294, 0.0700968149951466, 0.0478115464491638, 0.0363916682131507, 0.0310202066683672, 0.0267344490723088, 0.0212112857883806, 0.0149149265224817, 0.00956339674119522, 0.00665150505587597, 0.00689835920722663, 0.010231338259878, 0.0157315524205489, 0.0215689799990253, 0.0254154063025622, 0.0255363521874538, 0.0218531199052928, 0.0159232922023665, 0.00987834564939972, 0.00521442208935573, 0.00234582757042574, 0.000897736459776011, 0.000292383673435392), unit = 'native', valid.unit = 4L, class = 'unit'),     arrow = NULL, name = 'plot_02.density.lines.panel.3.1', gp = structure(list(lty = 1, col = '#0080ff', lwd = 1, alpha = 1), .Names = c('lty', 'col', 'lwd', 'alpha'), class = 'gpar'), vp = NULL), .Names = c('x', 'y', 'arrow', 'name', 'gp', 'vp'), class = c('lines', 'grob', 'gDesc')));invisible(argv[[1]]);");
    }

    @Test
    public void testinvisible37() {
        assertEval("argv <- list(structure(list(sec = numeric(0), min = integer(0), hour = integer(0), mday = integer(0), mon = integer(0), year = integer(0), wday = integer(0), yday = integer(0), isdst = integer(0)), .Names = c('sec', 'min', 'hour', 'mday', 'mon', 'year', 'wday', 'yday', 'isdst'), class = c('POSIXlt', 'POSIXt'), tzone = 'UTC'));invisible(argv[[1]]);");
    }

    @Test
    public void testinvisible38() {
        assertEval("argv <- list(structure(list(value = 4.94065645841247e-324, visible = TRUE), .Names = c('value', 'visible')));invisible(argv[[1]]);");
    }

    @Test
    public void testinvisible39() {
        assertEval("argv <- list(structure(list(c0 = structure(integer(0), .Label = character(0), class = 'factor'), c0 = structure(integer(0), .Label = character(0), class = 'factor')), .Names = c('c0', 'c0'), row.names = integer(0), class = 'data.frame'));invisible(argv[[1]]);");
    }

    @Test
    public void testinvisible40() {
        assertEval("argv <- list(structure(3.14159265358979, class = structure('3.14159265358979', class = 'testit')));invisible(argv[[1]]);");
    }

    @Test
    public void testinvisible41() {
        assertEval("argv <- list(c(1e-10, 1e+49, 1e+108, 1e+167, 1e+226));invisible(argv[[1]]);");
    }

    @Test
    public void testinvisible42() {
        assertEval("argv <- list(structure('checkRd: (-3) Surv.Rd:90: Unnecessary braces at ‘{time2}’', class = 'checkRd'));invisible(argv[[1]]);");
    }

    @Test
    public void testinvisible43() {
        assertEval("argv <- list(structure(list(raster = structure('#000000', .Dim = c(1L, 1L), class = 'raster'), x = structure(0, unit = 'npc', valid.unit = 0L, class = 'unit'), y = structure(0.5, unit = 'npc', valid.unit = 0L, class = 'unit'), width = NULL, height = NULL, just = 'centre', hjust = NULL, vjust = NULL, interpolate = TRUE, name = 'GRID.rastergrob.785', gp = structure(list(), class = 'gpar'), vp = NULL), .Names = c('raster', 'x', 'y', 'width', 'height', 'just', 'hjust', 'vjust', 'interpolate', 'name', 'gp', 'vp'), class = c('rastergrob', 'grob', 'gDesc')));invisible(argv[[1]]);");
    }

    @Test
    public void testinvisible44() {
        assertEval("argv <- list(structure(c('0', 'NULL', 'NULL'), .Names = c('Length', 'Class', 'Mode'), class = c('summaryDefault', 'table')));invisible(argv[[1]]);");
    }

    @Test
    public void testinvisible45() {
        assertEval("argv <- list(structure(list(TEST = structure(c(1L, 2L, 6L, 3L, 4L, 5L, 10L, 11L, 9L, 7L, 8L), .Label = c('1', '2', '4', '5', '\\\\040', '\\\\b', '\\\\n', '\\\\r', '\\\\t', '\\\\x20', 'c:\\\\spencer\\\\tests'), class = 'factor')), .Names = 'TEST', class = 'data.frame', row.names = c(NA, -11L)));invisible(argv[[1]]);");
    }

    @Test
    public void testinvisible46() {
        assertEval("argv <- list(structure(list(size = numeric(0), isdir = logical(0), mode = structure(integer(0), class = 'octmode'), mtime = structure(numeric(0), class = c('POSIXct', 'POSIXt')), ctime = structure(numeric(0), class = c('POSIXct', 'POSIXt')), atime = structure(numeric(0), class = c('POSIXct', 'POSIXt')), uid = integer(0), gid = integer(0), uname = character(0), grname = character(0)), .Names = c('size', 'isdir', 'mode', 'mtime', 'ctime', 'atime', 'uid', 'gid', 'uname', 'grname'), class = 'data.frame', row.names = character(0)));invisible(argv[[1]]);");
    }

    @Test
    public void testinvisible47() {
        assertEval("argv <- list(structure(list(a = c(1L, 4L, 7L), b = c(2L, 5L, 8L), c = c(3L, 6L, 9L)), .Names = c('a', 'b', 'c'), class = 'data.frame', row.names = c(NA, -3L)));invisible(argv[[1]]);");
    }

    @Test
    public void testinvisible49() {
        assertEval("argv <- list(structure('Error in rnorm(1, sd = Inf) : (converted from warning) NAs produced\\n', class = 'try-error', condition = structure(list(message = '(converted from warning) NAs produced', call = quote(rnorm(1, sd = Inf))), .Names = c('message', 'call'), class = c('simpleError', 'error', 'condition'))));invisible(argv[[1]]);");
    }

    @Test
    public void testinvisible50() {
        assertEval("argv <- list(structure(1395078479.75887, class = c('POSIXct', 'POSIXt')));invisible(argv[[1]]);");
    }

    @Test
    public void testinvisible51() {
        assertEval("argv <- list(structure('Error in read.table(\\\'foo1\\\') : no lines available in input\\n', class = 'try-error', condition = structure(list(message = 'no lines available in input', call = quote(read.table('foo1'))), .Names = c('message', 'call'), class = c('simpleError', 'error', 'condition'))));invisible(argv[[1]]);");
    }

    @Test
    public void testinvisible52() {
        assertEval("argv <- list(structure(c(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11), .Dim = c(10L, 2L), .Dimnames = list(NULL, c('tt', 'tt + 1')), .Tsp = c(1920.5, 1921.25, 12), class = c('mts', 'ts', 'matrix')));invisible(argv[[1]]);");
    }

    @Test
    public void testInvisible() {
        assertEval("{ f <- function() { invisible(23) } ; f() }");
        assertEval("{ f <- function() { invisible(23) } ; toString(f()) }");
        assertEval("{ f <- function(x, r) { if (x) invisible(r) else r }; f(FALSE, 1) }");
        assertEval("{ f <- function(x, r) { if (x) invisible(r) else r }; f(TRUE, 1) }");
        assertEval("{ f <- function(x, r) { if (x) return(invisible(r)) else return(r) }; f(FALSE, 1) }");
        assertEval("{ f <- function(x, r) { if (x) return(invisible(r)) else return(r) }; f(TRUE, 1) }");
    }
}
