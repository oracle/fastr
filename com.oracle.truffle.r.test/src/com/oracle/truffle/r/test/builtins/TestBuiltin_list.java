/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2012-2014, Purdue University
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check
public class TestBuiltin_list extends TestBase {

    @Test
    public void testlist1() {
        assertEval("argv <- list(surname = structure(c('McNeil', 'Ripley', 'Ripley', 'Tierney', 'Tukey', 'Venables', 'R Core'), class = 'AsIs'), nationality = structure(c('Australia', 'UK', 'UK', 'US', 'US', 'Australia', NA), class = 'AsIs'), deceased = structure(c('no', 'no', 'no', 'no', 'yes', 'no', NA), class = 'AsIs'), title = structure(c('Interactive Data Analysis', 'Spatial Statistics', 'Stochastic Simulation', 'LISP-STAT', 'Exploratory Data Analysis', 'Modern Applied Statistics ...', 'An Introduction to R'), class = 'AsIs'), other.author = structure(c(NA, NA, NA, NA, NA, 'Ripley', 'Venables & Smith'), class = 'AsIs'));list(argv[[1]],argv[[2]],argv[[3]],argv[[4]],argv[[5]]);");
    }

    @Test
    public void testlist2() {
        assertEval("argv <- list(`_R_NS_LOAD_` = structure('survival', .Names = 'name'));list(argv[[1]]);");
    }

    @Test
    public void testlist3() {
        assertEval("argv <- list(x = c(9.5367431640625e-07, 1.9073486328125e-06, 3.814697265625e-06, 7.62939453125e-06, 1.52587890625e-05, 3.0517578125e-05, 6.103515625e-05, 0.0001220703125, 0.000244140625, 0.00048828125, 0.0009765625, 0.001953125, 0.00390625, 0.0078125, 0.015625, 0.03125, 0.0625, 0.125, 0.25, 0.5, 1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024), y = c(3.69420518444359e+25, 2.30887824027777e+24, 1.44304890017492e+23, 9.01905562612606e+21, 5.63690976641081e+20, 35230686042118275072, 2201917878145066496, 137619867512235136, 8601241751556820, 537577617482832, 33598603095309.8, 2099913194115.17, 131244699796.888, 8202825028.58974, 512684387.219832, 32044730.0464007, 2003284.70114408, 125327.674230857, 7863.68742857025, 499.272560819512, 33.2784230289721, 2.7659432263306, 0.488936768533843, -0.282943224311172, 7.32218543045282e-05, -0.00636442868227041, -0.0483709204009262, -0.0704795507649514, 0.0349437746169591, -0.0264830837608839, 0.0200901469411759), xlab = NULL, ylab = NULL);list(argv[[1]],argv[[2]],argv[[3]],argv[[4]]);");
    }

    @Test
    public void testlist4() {
        assertEval("argv <- list(class = 'try-error', condition = structure(list(message = 'line 1 did not have 4 elements', call = quote(scan(file, what, nmax, sep, dec, quote, skip, nlines, na.strings, flush, fill, strip.white, quiet, blank.lines.skip, multi.line, comment.char, allowEscapes, encoding))), .Names = c('message', 'call'), class = c('simpleError', 'error', 'condition')));list(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testlist5() {
        assertEval("argv <- list(structure(list(Hair = structure(c(1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L), .Label = c('Black', 'Brown', 'Red', 'Blond'), class = 'factor'), Eye = structure(c(1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 3L, 3L, 3L, 3L, 4L, 4L, 4L, 4L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 3L, 3L, 3L, 3L, 4L, 4L, 4L, 4L), .Label = c('Brown', 'Blue', 'Hazel', 'Green'), class = 'factor'), Sex = structure(c(1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L), .Label = c('Male', 'Female'), class = 'factor')), .Names = c('Hair', 'Eye', 'Sex'), out.attrs = structure(list(dim = structure(c(4L, 4L, 2L), .Names = c('Hair', 'Eye', 'Sex')), dimnames = structure(list(Hair = c('Hair=Black', 'Hair=Brown', 'Hair=Red', 'Hair=Blond'), Eye = c('Eye=Brown', 'Eye=Blue', 'Eye=Hazel', 'Eye=Green'), Sex = c('Sex=Male', 'Sex=Female')), .Names = c('Hair', 'Eye', 'Sex'))), .Names = c('dim', 'dimnames')), class = 'data.frame', row.names = c(NA, -32L)), Fr = c(32, 53, 10, 3, 11, 50, 10, 30, 10, 25, 7, 5, 3, 15, 7, 8, 36, 66, 16, 4, 9, 34, 7, 64, 5, 29, 7, 5, 2, 14, 7, 8));list(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testlist6() {
        assertEval("argv <- list(deviance.resid = structure(c(0.979005810350303, 0.190415231217834, -0.262041786489909, -1.18856115310823, -0.0713326116251696, 0.258231444611709, 0.637403312181204, -1.72855514890285, -0.632723785156881, -0.819071604478243, 2.23780874325045, -0.472376375886729), .Names = c('1', '3', '5', '7', '9', '11', '13', '15', '17', '19', '21', '23')), coefficients = structure(c(0.291009862544455, -0.575062166945441, 0.0881289026086606, 0.127412648101879, 3.30209334202984, -4.5133836829576, 0.00095966129066828, 6.38014475989249e-06), .Dim = c(2L, 4L), .Dimnames = list(c('(Intercept)', 'M.userY'), c('Estimate', 'Std. Error', 'z value', 'Pr(>|z|)'))), aliased = structure(c(FALSE, FALSE), .Names = c('(Intercept)', 'M.userY')), dispersion = 1, df = c(2L, 10L, 2L), cov.unscaled = structure(c(0.00776670347500679, -0.00776670347500679, -0.00776670347500679, 0.0162339828963334), .Dim = c(2L, 2L), .Dimnames = list(c('(Intercept)', 'M.userY'), c('(Intercept)', 'M.userY'))), cov.scaled = structure(c(0.00776670347500679, -0.00776670347500679, -0.00776670347500679, 0.0162339828963334), .Dim = c(2L, 2L), .Dimnames = list(c('(Intercept)', 'M.userY'), c('(Intercept)', 'M.userY'))));list(argv[[1]],argv[[2]],argv[[3]],argv[[4]],argv[[5]],argv[[6]],argv[[7]]);");
    }

    @Test
    public void testlist7() {
        assertEval("argv <- list(class = 'try-error', condition = structure(list(message = '(converted from warning) NAs produced', call = quote(rnorm(1, sd = Inf))), .Names = c('message', 'call'), class = c('simpleError', 'error', 'condition')));list(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testlist8() {
        assertEval("argv <- list(upper = quote(~M.user * Temp * Soft));list(argv[[1]]);");
    }

    @Test
    public void testlist9() {
        assertEval("argv <- list(label = '', x = structure(0.5, unit = 'npc', valid.unit = 0L, class = 'r_unit'), y = structure(0.5, unit = 'npc', valid.unit = 0L, class = 'r_unit'), just = 'centre', hjust = NULL, vjust = NULL, rot = 0, check.overlap = FALSE, name = NULL, gp = structure(list(), class = 'r_gpar'), vp = NULL);list(argv[[1]],argv[[2]],argv[[3]],argv[[4]],argv[[5]],argv[[6]],argv[[7]],argv[[8]],argv[[9]],argv[[10]],argv[[11]]);");
    }

    @Test
    public void testlist10() {
        assertEval(Output.IgnoreWhitespace,
                        "argv <- list(linkfun = function (mu) .Call(C_logit_link, mu), linkinv = function (eta) .Call(C_logit_linkinv, eta), mu.eta = function (eta) .Call(C_logit_mu_eta, eta), valideta = function (eta) TRUE, name = 'logit');list(argv[[1]],argv[[2]],argv[[3]],argv[[4]],argv[[5]]);");
    }

    @Test
    public void testlist11() {
        assertEval(Output.IgnoreWhitespace,
                        "argv <- list(linkfun = function (mu) log(mu), linkinv = function (eta) pmax(exp(eta), .Machine$double.eps), mu.eta = function (eta) pmax(exp(eta), .Machine$double.eps), valideta = function (eta) TRUE, name = 'log');list(argv[[1]],argv[[2]],argv[[3]],argv[[4]],argv[[5]]);");
    }

    @Test
    public void testlist12() {
        assertEval("argv <- list(class = 'try-error', condition = structure(list(message = '(converted from warning) NAs produced', call = quote(rnorm(2, numeric()))), .Names = c('message', 'call'), class = c('simpleError', 'error', 'condition')));list(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testlist13() {
        assertEval("argv <- list(class = 'try-error', condition = structure(list(message = 'undefined columns selected', call = quote(`[.data.frame`(dd, , 'x'))), .Names = c('message', 'call'), class = c('simpleError', 'error', 'condition')));list(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testlist14() {
        assertEval(Output.IgnoreWhitespace, "argv <- list(error = function (e) -1);list(argv[[1]]);");
    }

    @Test
    public void testlist15() {
        assertEval(Ignored.OutputFormatting,
                        "argv <- list(error = function (e) warning(gettextf('%s namespace cannot be unloaded:\\n  ', sQuote(pkgname)), conditionMessage(e), call. = FALSE, domain = NA));list(argv[[1]]);");
    }

    @Test
    public void testlist16() {
        assertEval("argv <- list(aa = structure(c('1', '2', '3'), class = 'AsIs'), ..dfd.row.names = structure(c('4', '5', '6', '7', '8', '9'), .Dim = c(3L, 2L), .Dimnames = list(NULL, c('a', 'b'))));list(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testlist17() {
        assertEval("argv <- list(deviance.resid = structure(c(-0.667819876370237, 0.170711734013213, 0.552921941721332, -0.253162069270378, -0.00786394222146348, 0.0246733498130512, 0.0730305465518564, -1.36919169254062, 0.0881443844426084, -0.0834190388782434), .Names = c('1', '2', '3', '4', '5', '6', '7', '8', '9', '10')), coefficients = structure(logical(0), .Dim = c(0L, 4L), .Dimnames = list(NULL, c('Estimate', 'Std. Error', 't value', 'Pr(>|t|)'))), aliased = structure(TRUE, .Names = 'x'), dispersion = 0.274035772634541, df = c(0L, 10L, 1L), cov.unscaled = structure(logical(0), .Dim = c(0L, 0L)), cov.scaled = structure(logical(0), .Dim = c(0L, 0L)));list(argv[[1]],argv[[2]],argv[[3]],argv[[4]],argv[[5]],argv[[6]],argv[[7]]);");
    }

    @Test
    public void testlist18() {
        assertEval("argv <- list(date = structure(1065672000, class = c('POSIXct', 'POSIXt'), tzone = ''));list(argv[[1]]);");
    }

    @Test
    public void testlist19() {
        assertEval(Ignored.ImplementationError,
                        "argv <- list(arguments = structure('object', simpleOnly = TRUE), generic = structure(function (object) standardGeneric('show'), generic = structure('show', package = 'methods'), package = 'methods', group = list(), valueClass = character(0), signature = structure('object', simpleOnly = TRUE), default = structure(function (object) showDefault(object, FALSE), target = structure('ANY', class = structure('signature', package = 'methods'), .Names = 'object', package = 'methods'), defined = structure('ANY', class = structure('signature', package = 'methods'), .Names = 'object', package = 'methods'), generic = structure('show', package = 'methods'), class = structure('derivedDefaultMethod', package = 'methods')), skeleton = quote((function (object) showDefault(object, FALSE))(object)), class = structure('standardGeneric', package = 'methods')));list(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testlist20() {
        assertEval(Output.IgnoreWhitespace,
                        "argv <- list('Residuals vs Fitted', 'Normal Q-Q', 'Scale-Location', 'Cooks distance', 'Residuals vs Leverage', expression('Cooks dist vs Leverage  ' * h[ii]/(1 - h[ii])));list(argv[[1]],argv[[2]],argv[[3]],argv[[4]],argv[[5]],argv[[6]]);");
    }

    @Test
    public void testlist21() {
        assertEval("argv <- list(Df = structure(c(NA, 2, 1), .Names = c('<none>', 'Soft', 'M.user:Temp')), Deviance = structure(c(8.44399377410362, 8.2279889309135, 5.65604443125997), .Names = c('<none>', 'Soft', 'M.user:Temp')), AIC = structure(c(72.1419514890413, 75.9259466458512, 71.3540021461976), .Names = c('<none>', 'Soft', 'M.user:Temp')));list(argv[[1]],argv[[2]],argv[[3]]);");
    }

    @Test
    public void testlist22() {
        assertEval("argv <- list(structure(TRUE, .Dim = c(1L, 1L)));list(argv[[1]]);");
    }

    @Test
    public void testlist23() {
        assertEval("argv <- list(V1 = c(1L, 1L, 2L, 3L), V2 = structure(c(1L, 1L, 2L, 3L), .Label = c('A', 'D', 'E'), class = 'factor'), V3 = c(6, 6, 9, 10));list(argv[[1]],argv[[2]],argv[[3]]);");
    }

    @Test
    public void testlist24() {
        assertEval("argv <- list(structure(1:5, .Tsp = c(-1, 3, 1), class = 'ts'), structure(1:5, .Tsp = c(1, 5, 1), class = 'ts'));list(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testlist25() {
        assertEval("argv <- list(structure(list(x = 1L, y = structure(1L, .Label = c('A', 'D', 'E'), class = 'factor'), z = 6), .Names = c('x', 'y', 'z'), row.names = 1L, class = 'data.frame'), structure(list(), .Names = character(0), row.names = 1L, class = 'data.frame'));list(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testlist26() {
        assertEval("argv <- list(1L, 3.14159265358979, 3+5i, 'testit', TRUE, structure(1L, .Label = 'foo', class = 'factor'));list(argv[[1]],argv[[2]],argv[[3]],argv[[4]],argv[[5]],argv[[6]]);");
    }

    @Test
    public void testlist27() {
        assertEval("argv <- list(structure(c(24L, 13L, 15L, 68L, 39L, 74L, 22L, 1L, 8L, 55L, 24L, 20L, 51L, 13L, 3L, 4L, 5L, 6L, 15L, 2L, 8L, 60L, 67L, 23L, 58L, 24L, 22L, 21L, 37L, 74L, 59L, 39L, 14L, 14L, 19L, 23L, 70L, 21L, 22L, 31L, 29L, 30L, 45L, 58L, 17L, 7L, 19L, 26L, 39L, 74L, 57L, 59L, 12L, 72L, 70L, 37L, 64L, 16L, 18L, 21L, 22L, 8L, 62L, 61L, 63L, 71L, 105L, 64L, 10L, 41L, 8L, 27L, 11L, 34L, 32L, 33L, 68L, 107L, NA, 66L, NA, 65L, 48L, 52L, 43L, 47L, 46L, 44L, 41L, 54L, 28L, 50L, 40L, NA, 69L, NA, 75L, 109L, NA, 86L, 112L, 110L, 104L, 24L, 111L, 87L, NA, NA, 92L, 73L, 85L, 90L, 89L, NA, 83L, NA, 102L, NA, 108L, 88L, 91L, 93L, NA, 94L, 84L, NA, 106L, NA, 95L, 82L, 56L, 87L, 109L, 75L, 104L, 110L, 112L, 111L, 24L, 73L, 85L, 86L, 90L, 89L, 102L, 88L, 92L, 9L, 49L, 42L, 38L, 35L, 36L, 25L, NA, NA, 9L, 49L, 42L, NA, 36L, 38L, 25L, 53L, 79L, 78L, 103L, 77L, 80L, 114L, 97L, 113L, 76L, 96L, 81L, 116L, 99L, 117L, 115L, 98L, 101L, 100L), .Label = c('1008', '1011', '1013', '1014', '1015', '1016', '1027', '1028', '1030', '1032', '1051', '1052', '1083', '1093', '1095', '1096', '110', '1102', '111', '1117', '112', '113', '116', '117', '1219', '125', '1250', '1251', '126', '127', '128', '1291', '1292', '1293', '1298', '1299', '130', '1308', '135', '1376', '1377', '1383', '1408', '1409', '141', '1410', '1411', '1413', '1418', '1422', '1438', '1445', '1456', '1492', '2001', '2316', '262', '266', '269', '270', '2708', '2714', '2715', '272', '2728', '2734', '280', '283', '286', '290', '3501', '411', '412', '475', '5028', '5042', '5043', '5044', '5045', '5047', '5049', '5050', '5051', '5052', '5053', '5054', '5055', '5056', '5057', '5058', '5059', '5060', '5061', '5062', '5066', '5067', '5068', '5069', '5070', '5072', '5073', '5115', '5160', '5165', '655', '724', '885', '931', '942', '952', '955', '958', 'c118', 'c168', 'c203', 'c204', 'c266'), class = 'factor'));list(argv[[1]]);");
    }

    @Test
    public void testlist28() {
        assertEval("argv <- list(3.14159265358979, 'C', NaN, Inf, 1:3, c(0, NA), NA);list(argv[[1]],argv[[2]],argv[[3]],argv[[4]],argv[[5]],argv[[6]],argv[[7]]);");
    }

    @Test
    public void testlist29() {
        assertEval("argv <- list(assign = c(0L, 1L, 1L, 1L), qr = structure(list(qr = structure(c(-28.8270706107991, 0.273146306828071, 0.312206540911182, 0.247733407426682, 0.216636580341913, 0.0849718577324175, 0.298411357268471, 0.294351149612123, 0.247733407426682, 0.308328048219576, 0.125075187976724, 0.138758462627192, 0.190002850064127, 0.1835601922086, 0.232705016165824, 0.069379231313596, 0.120168353625222, 0.222121918799273, 0.190002850064127, 0.247733407426682, 0.0917800961043001, -10.2334366187554, 13.7940847818881, 0.190374922931528, 0.151060987411652, 0.132099001405849, -0.125761881229701, -0.441661211981173, -0.435651935890569, -0.366655739827817, -0.45633832676795, -0.185116476853374, 0.084611076858457, 0.115858488525451, 0.111929933764425, 0.141897089628727, 0.0423055384292285, 0.0732753420009814, 0.13544380924692, 0.115858488525451, 0.151060987411652, 0.0559649668822123, -4.26682272578616, -3.16543363464969, 9.7352069177467, 0.118607830555703, 0.10371953900067, 0.00616533725634264, 0.0216519528674631, 0.0213573547475655, 0.0179748924786157, 0.0223714822011986, 0.00907513071804667, -0.344446140042991, -0.471652301867824, -0.45565941330494, -0.577653737792655, -0.172223070021495, 0.0575332486360618, 0.106345765721762, 0.0909680534393656, 0.118607830555703, 0.0439417444752447, -4.89123580760852, -3.62866782508622, -3.32364207119197, 9.63649238427318, 0.135617489972887, 0.00806142768852949, 0.0283108036266689, 0.0279256046761512, 0.0235028985277947, 0.0292516173165799, 0.0118661002643811, 0.0254562434016423, 0.0348573968510539, 0.0336754446773372, 0.0426914180233895, 0.0127281217008212, -0.284250391934964, -0.525414891452651, -0.449439332155022, -0.585997195035538, -0.217099822893807), assign = c(0L, 1L, 1L, 1L), contrasts = structure(list(trt = 'contr.treatment'), .Names = 'trt'), .Dim = c(21L, 4L), .Dimnames = list(c('1', '2', '3', '4', '5', '6', '7', '8', '9', '10', '11', '12', '13', '14', '15', '16', '17', '18', '19', '20', '21'), c('(Intercept)', 'trt2', 'trt3', 'trt4'))), qraux = c(1.21663658034191, 1.16655707135303, 1.14947576464323, 1.15508453302121), pivot = 1:4, tol = 1e-07, rank = 4L), .Names = c('qr', 'qraux', 'pivot', 'tol', 'rank'), class = 'qr'), df.residual = 17L);list(argv[[1]],argv[[2]],argv[[3]]);");
    }

    @Test
    public void testlist30() {
        assertEval(Output.IgnoreWhitespace, "argv <- list(function (x, i, j, ...) x@aa[[i]]);list(argv[[1]]);");
    }

    @Test
    public void testlist31() {
        assertEval("argv <- list(structure(c(TRUE, TRUE, TRUE, TRUE, TRUE, TRUE), .Dim = 2:3, .Dimnames = list(NULL, c('a', 'b', 'c'))), structure(c(TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE), .Dim = c(2L, 2L, 5L)), TRUE);list(argv[[1]],argv[[2]],argv[[3]]);");
    }

    @Test
    public void testlist32() {
        assertEval(Ignored.ImplementationError,
                        "argv <- list(structure(function (e1, e2) standardGeneric('Ops'), generic = structure('Ops', package = 'base'), package = 'base', group = list(), valueClass = character(0), signature = c('e1', 'e2'), default = quote(`\\001NULL\\001`), skeleton = quote((function (e1, e2) stop('invalid call in method dispatch to Ops (no default method)', domain = NA))(e1, e2)), groupMembers = list('Arith', 'Compare', 'Logic'), class = structure('groupGenericFunction', package = 'methods')));list(argv[[1]]);");
    }

    @Test
    public void testlist33() {
        assertEval(Ignored.OutputFormatting,
                        "argv <- list(tables = structure(list(`Grand mean` = 103.87323943662, N = structure(c(78.7365206866197, 98.5088731171753, 113.842206450509, 123.008873117175), .Dim = 4L, .Dimnames = structure(list(N = c('0.0cwt', '0.2cwt', '0.4cwt', '0.6cwt')), .Names = 'N'), class = 'mtable'), `V:N` = structure(c(79.5323303457107, 86.1989970123773, 69.7732394366197, 98.0323303457106, 108.032330345711, 89.1989970123773, 114.198997012377, 116.698997012377, 110.365663679044, 124.365663679044, 126.365663679044, 118.032330345711), .Dim = 3:4, .Dimnames = structure(list(V = c('Golden.rain', 'Marvellous', 'Victory'), N = c('0.0cwt', '0.2cwt', '0.4cwt', '0.6cwt')), .Names = c('V', 'N')), class = 'mtable')), .Names = c('Grand mean', 'N', 'V:N')), n = structure(list(N = structure(c(17, 18, 18, 18), .Dim = 4L, .Dimnames = structure(list(N = c('0.0cwt', '0.2cwt', '0.4cwt', '0.6cwt')), .Names = 'N')), `V:N` = structure(c(6, 6, 5, 6, 6, 6, 6, 6, 6, 6, 6, 6), .Dim = 3:4, .Dimnames = structure(list(V = c('Golden.rain', 'Marvellous', 'Victory'), N = c('0.0cwt', '0.2cwt', '0.4cwt', '0.6cwt')), .Names = c('V', 'N')))), .Names = c('N', 'V:N')));list(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testlist34() {
        assertEval("argv <- list(itemBullet = '• ');list(argv[[1]]);");
    }

    @Test
    public void testlist35() {
        assertEval("argv <- list(class = 'try-error', condition = structure(list(message = 'more columns than column names', call = quote(read.table('foo6', header = TRUE))), .Names = c('message', 'call'), class = c('simpleError', 'error', 'condition')));list(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testlist36() {
        assertEval("argv <- list(name = 'list', objs = structure(list(`package:base` = .Primitive('list'), .Primitive('list')), .Names = c('package:base', '')), where = c('package:base', 'namespace:base'), visible = c(TRUE, FALSE), dups = c(FALSE, TRUE));list(argv[[1]],argv[[2]],argv[[3]],argv[[4]],argv[[5]]);");
    }

    @Test
    public void testlist37() {
        assertEval("argv <- list(structure(-1.81670076485116, .Names = '5%'), structure(c(-0.569903669351869, -3.58817618394987, 1.7002237104195, 0.247262299686774, -1.6099565644337, -0.117004990933267, 2.26201852051082, 1.27765184061634, -0.585159452768219, 0.777745165779344, -0.299055554574658, -0.10613560158199, -0.96347850905908, 2.01298478288055, -0.65898967614864, 0.497719980170775, 0.113843920033269, -0.766958149949393, 3.9222560854539, -0.936533336103743, 0.287536526568389, -1.36853788163704, 0.875060974238616, 6.63795852562496, -1.7181964535622, -1.84566355665129, -2.51563250429738, -0.197885450775488, 0.343408036526242, 0.0203380393884578, 0.207160904400713, 0.869565410777187, -0.815315222368209, -0.0998963343276999, 0.656114271672876, 1.27566552196184, 0.0658788246994603, -1.69200573781689, -0.0369929356350034, -0.342061734014624, 0.31798622848054, -1.52242182038666, -1.33617654990952, 0.0175687049379899, -0.093090859182165, -0.0507330478224399, -0.431715933999334, 0.37428759377223, -1.51710077889452, 0.148230661369186, 0.214909263767934, 0.178494903424769, -2.69339417614172, 0.644025806665703, -0.287978582462478, 3.36345700350871, 1.39656784449323, -0.344866954524567, -0.270662469024608, -1.32424067954204), .Dim = 60L, .Dimnames = list(c('1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1'))), c(1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L));list(argv[[1]],argv[[2]],argv[[3]]);");
    }

    @Test
    public void testlist38() {
        assertEval("argv <- list(srcfile = '/home/lzhao/hg/r-instrumented/library/utils/R/utils', frow = 1271L, lrow = 1273L);list(argv[[1]],argv[[2]],argv[[3]]);");
    }

    @Test
    public void testlist39() {
        assertEval("argv <- list(structure(list(srcfile = c('/home/lzhao/hg/r-instrumented/library/utils/R/utils', '/home/lzhao/hg/r-instrumented/library/utils/R/utils', '/home/lzhao/hg/r-instrumented/library/utils/R/utils', '/home/lzhao/hg/r-instrumented/library/utils/R/utils', '/home/lzhao/hg/r-instrumented/library/utils/R/utils', '/home/lzhao/hg/r-instrumented/library/utils/R/utils', '/home/lzhao/hg/r-instrumented/library/utils/R/utils', '/home/lzhao/hg/r-instrumented/library/utils/R/utils', '/home/lzhao/hg/r-instrumented/library/utils/R/utils', '/home/lzhao/hg/r-instrumented/library/utils/R/utils', '/home/lzhao/hg/r-instrumented/library/utils/R/utils', '/home/lzhao/hg/r-instrumented/library/utils/R/utils', '/home/lzhao/hg/r-instrumented/library/utils/R/utils'), frow = c(6889L, 6893L, 6897L, 6901L, 6902L, 6903L, 6903L, 6917L, 6918L, 6919L, 6919L, 6927L, 6928L), lrow = c(6889L, 6893L, 6900L, 6901L, 6902L, 6903L, 6903L, 6917L, 6918L, 6919L, 6919L, 6927L, 6928L)), .Names = c('srcfile', 'frow', 'lrow'), row.names = c(NA, 13L), class = 'data.frame'), structure(list(    srcfile = '/home/lzhao/hg/r-instrumented/library/utils/R/utils', frow = 6928L, lrow = 6928L), .Names = c('srcfile', 'frow', 'lrow'), row.names = c(NA, -1L), class = 'data.frame'));list(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testlist40() {
        assertEval("argv <- list(x = c(190, 118, 149, 313, NA, NA, 299, 99, 19, 194, NA, 256, 290, 274, 65, 334, 307, 78, 322, 44, 8, 320, 25, 92, 66, 266, NA, 13, 252, 223, 279, 286, 287, 242, 186, 220, 264, 127, 273, 291, 323, 259, 250, 148, 332, 322, 191, 284, 37, 120, 137, 150, 59, 91, 250, 135, 127, 47, 98, 31, 138, 269, 248, 236, 101, 175, 314, 276, 267, 272, 175, 139, 264, 175, 291, 48, 260, 274, 285, 187, 220, 7, 258, 295, 294, 223, 81, 82, 213, 275, 253, 254, 83, 24, 77, NA, NA, NA, 255, 229, 207, 222, 137, 192, 273, 157, 64, 71, 51, 115, 244, 190, 259, 36, 255, 212, 238, 215, 153, 203, 225, 237, 188, 167, 197, 183, 189, 95, 92, 252, 220, 230, 259, 236, 259, 238, 24, 112, 237, 224, 27, 238, 201, 238, 14, 139, 49, 20, 193, 145, 191, 131, 223), y = c(7.4, 8, 12.6, 11.5, 14.3, 14.9, 8.6, 13.8, 20.1, 8.6, 6.9, 9.7, 9.2, 10.9, 13.2, 11.5, 12, 18.4, 11.5, 9.7, 9.7, 16.6, 9.7, 12, 16.6, 14.9, 8, 12, 14.9, 5.7, 7.4, 8.6, 9.7, 16.1, 9.2, 8.6, 14.3, 9.7, 6.9, 13.8, 11.5, 10.9, 9.2, 8, 13.8, 11.5, 14.9, 20.7, 9.2, 11.5, 10.3, 6.3, 1.7, 4.6, 6.3, 8, 8, 10.3, 11.5, 14.9, 8, 4.1, 9.2, 9.2, 10.9, 4.6, 10.9, 5.1, 6.3, 5.7, 7.4, 8.6, 14.3, 14.9, 14.9, 14.3, 6.9, 10.3, 6.3, 5.1, 11.5, 6.9, 9.7, 11.5, 8.6, 8, 8.6, 12, 7.4, 7.4, 7.4, 9.2, 6.9, 13.8, 7.4, 6.9, 7.4, 4.6, 4, 10.3, 8, 8.6, 11.5, 11.5, 11.5, 9.7, 11.5, 10.3, 6.3, 7.4, 10.9, 10.3, 15.5, 14.3, 12.6, 9.7, 3.4, 8, 5.7, 9.7, 2.3, 6.3, 6.3, 6.9, 5.1, 2.8, 4.6, 7.4, 15.5, 10.9, 10.3, 10.9, 9.7, 14.9, 15.5, 6.3, 10.9, 11.5, 6.9, 13.8, 10.3, 10.3, 8, 12.6, 9.2, 10.3, 10.3, 16.6, 6.9, 13.2, 14.3, 8, 11.5), xlab = NULL, ylab = NULL);list(argv[[1]],argv[[2]],argv[[3]],argv[[4]]);");
    }

    @Test
    public void testlist41() {
        assertEval("argv <- list(properties = structure(list(.Data = 'numeric', comment = 'character'), .Names = c('.Data', 'comment')), prototype = structure(3.14159265358979, comment = 'Start with pi'));list(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testlist42() {
        assertEval("argv <- list(structure(c(NA, NA, FALSE), .Names = c('perm', 'LDL', 'super')));list(argv[[1]]);");
    }

    @Test
    public void testlist43() {
        assertEval("argv <- list(raster = structure('#000000', .Dim = c(1L, 1L), class = 'r_raster'), x = structure(0, unit = 'npc', valid.unit = 0L, class = 'r_unit'), y = structure(0.5, unit = 'npc', valid.unit = 0L, class = 'r_unit'), width = NULL, height = NULL, just = 'centre', hjust = NULL, vjust = NULL, interpolate = TRUE, name = NULL, gp = structure(list(), class = 'r_gpar'), vp = NULL);list(argv[[1]],argv[[2]],argv[[3]],argv[[4]],argv[[5]],argv[[6]],argv[[7]],argv[[8]],argv[[9]],argv[[10]],argv[[11]],argv[[12]]);");
    }

    @Test
    public void testlist44() {
        assertEval("argv <- list(trace = 0, fnscale = 1, parscale = 1, ndeps = 0.001, maxit = 100L, abstol = -Inf, reltol = 1.49011611938477e-08, alpha = 1, beta = 0.5, gamma = 2, REPORT = 10, type = 1, lmm = 5, factr = 1e+07, pgtol = 0, tmax = 10, temp = 10);list(argv[[1]],argv[[2]],argv[[3]],argv[[4]],argv[[5]],argv[[6]],argv[[7]],argv[[8]],argv[[9]],argv[[10]],argv[[11]],argv[[12]],argv[[13]],argv[[14]],argv[[15]],argv[[16]],argv[[17]]);");
    }

    @Test
    public void testlist45() {
        assertEval(Output.IgnoreWhitespace, "argv <- list(error = function (...) {});list(argv[[1]]);");
    }

    @Test
    public void testlist46() {
        assertEval("argv <- list(structure(list(srcfile = c(NA, '/home/lzhao/tmp/RtmptukZK0/R.INSTALL2ccc3a5cba55/nlme/R/lmList.R', '/home/lzhao/tmp/RtmptukZK0/R.INSTALL2ccc3a5cba55/nlme/R/lmList.R', '/home/lzhao/tmp/RtmptukZK0/R.INSTALL2ccc3a5cba55/nlme/R/lmList.R', '/home/lzhao/tmp/RtmptukZK0/R.INSTALL2ccc3a5cba55/nlme/R/lmList.R', '/home/lzhao/tmp/RtmptukZK0/R.INSTALL2ccc3a5cba55/nlme/R/lmList.R', '/home/lzhao/tmp/RtmptukZK0/R.INSTALL2ccc3a5cba55/nlme/R/lmList.R', '/home/lzhao/tmp/RtmptukZK0/R.INSTALL2ccc3a5cba55/nlme/R/lmList.R', '/home/lzhao/tmp/RtmptukZK0/R.INSTALL2ccc3a5cba55/nlme/R/lmList.R', '/home/lzhao/tmp/RtmptukZK0/R.INSTALL2ccc3a5cba55/nlme/R/lmList.R', '/home/lzhao/tmp/RtmptukZK0/R.INSTALL2ccc3a5cba55/nlme/R/lmList.R', '/home/lzhao/tmp/RtmptukZK0/R.INSTALL2ccc3a5cba55/nlme/R/lmList.R'), frow = c(NA, 832L, 833L, 834L, 842L, 845L, 845L, 849L, 858L, 860L, 862L, 863L), lrow = c(NA, 832L, 833L, 834L, 842L, 846L, 846L, 851L, 859L, 860L, 862L, 863L)), .Names = c('srcfile', 'frow', 'lrow'), row.names = c(NA, 12L), class = 'data.frame'), structure(list(    srcfile = '/home/lzhao/tmp/RtmptukZK0/R.INSTALL2ccc3a5cba55/nlme/R/lmList.R', frow = 863L, lrow = 863L), .Names = c('srcfile', 'frow', 'lrow'), row.names = c(NA, -1L), class = 'data.frame'));list(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testlist48() {
        assertEval("argv <- list(structure(list(Month = c(5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 7L, 7L, 7L, 7L, 7L, 7L, 7L, 7L, 7L, 7L, 7L, 7L, 7L, 7L, 7L, 7L, 7L, 7L, 7L, 7L, 7L, 7L, 7L, 7L, 7L, 7L, 8L, 8L, 8L, 8L, 8L, 8L, 8L, 8L, 8L, 8L, 8L, 8L, 8L, 8L, 8L, 8L, 8L, 8L, 8L, 8L, 8L, 8L, 8L, 8L, 8L, 8L, 9L, 9L, 9L, 9L, 9L, 9L, 9L, 9L, 9L, 9L, 9L, 9L, 9L, 9L, 9L, 9L, 9L, 9L, 9L, 9L, 9L, 9L, 9L, 9L, 9L, 9L, 9L, 9L, 9L)), .Names = 'Month', class = 'data.frame', row.names = c(1L, 2L, 3L, 4L, 6L, 7L, 8L, 9L, 11L, 12L, 13L, 14L, 15L, 16L, 17L, 18L, 19L, 20L, 21L, 22L, 23L, 24L, 28L, 29L, 30L, 31L, 38L, 40L, 41L, 44L, 47L, 48L, 49L, 50L, 51L, 62L, 63L, 64L, 66L, 67L, 68L, 69L, 70L, 71L, 73L, 74L, 76L, 77L, 78L, 79L, 80L, 81L, 82L, 85L, 86L, 87L, 88L, 89L, 90L, 91L, 92L, 93L, 94L, 95L, 96L, 97L, 98L, 99L, 100L, 101L, 104L, 105L, 106L, 108L, 109L, 110L, 111L, 112L, 113L, 114L, 116L, 117L, 118L, 120L, 121L, 122L, 123L, 124L, 125L, 126L, 127L, 128L, 129L, 130L, 131L, 132L, 133L, 134L, 135L, 136L, 137L, 138L, 139L, 140L, 141L, 142L, 143L, 144L, 145L, 146L, 147L, 148L, 149L, 151L, 152L, 153L)));list(argv[[1]]);");
    }

    @Test
    public void testlist49() {
        assertEval("argv <- list(structure(list(stats = c(7, 35, 60, 80, 135), n = 26L, conf = c(46.0561427916751, 73.9438572083249), out = integer(0)), .Names = c('stats', 'n', 'conf', 'out')));list(argv[[1]]);");
    }

    @Test
    public void testlist50() {
        assertEval("argv <- list('‘', 'Matrix', '’');list(argv[[1]],argv[[2]],argv[[3]]);");
    }

    @Test
    public void testlist51() {
        assertEval(Output.IgnoreWhitespace, "argv <- list(structure(3.14159265358979, class = structure('3.14159265358979', class = 'testit')));list(argv[[1]]);");
    }

    @Test
    public void testlist52() {
        assertEval("argv <- list(structure(1386392034.50546, class = c('POSIXct', 'POSIXt')));list(argv[[1]]);");
    }

    @Test
    public void testlist53() {
        assertEval("argv <- list(structure(list(sec = 54.5054557323456, min = 53L, hour = 23L, mday = 6L, mon = 11L, year = 113L, wday = 5L, yday = 339L, isdst = 0L), .Names = c('sec', 'min', 'hour', 'mday', 'mon', 'year', 'wday', 'yday', 'isdst'), class = c('POSIXlt', 'POSIXt'), tzone = c('', 'EST', 'EDT')));list(argv[[1]]);");
    }

    @Test
    public void testlist54() {
        assertEval("argv <- list(values = c(0.266917355843816, 0.00557315714782281, 0.00229578896250102, 0.000615239459643172, 8.19421206363694e-05), vectors = structure(c(-0.452472222108953, -0.386550651250976, -0.453293999783174, -0.439775552409852, -0.496960255453506, -0.157430201026812, 0.910628681750865, -0.204120426456847, -0.072367418669335, -0.314752194584169, 0.437863914035591, 0.0975567326834968, -0.370843683888789, -0.67178336056532, 0.458192050652246, 0.752103796361061, -0.0893037594956476, 0.0198033027727173, 0.0214021063919376, -0.652314722415877, 0.114453887261006, -0.0619800003080987, -0.784182499538679, 0.591277842073673, 0.136040832629847), .Dim = c(5L, 5L)));list(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testlist55() {
        assertEval("argv <- list(object = c(0, 0, 0, 0, 0, 1.75368801162502e-134, 0, 0, 0, 2.60477585273833e-251, 1.16485035372295e-260, 0, 1.53160350210786e-322, 0.333331382328728, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3.44161262707711e-123, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1.968811545398e-173, 0, 8.2359965384697e-150, 0, 0, 0, 0, 6.51733217171341e-10, 0, 2.36840184577368e-67, 0, 9.4348408357524e-307, 0, 1.59959906013771e-89, 0, 8.73836857865034e-286, 7.09716190970992e-54, 0, 0, 0, 1.530425353017e-274, 8.57590058044551e-14, 0.333333106397154, 0, 0, 1.36895217898448e-199, 2.0226102635783e-177, 5.50445388209462e-42, 0, 0, 0, 0, 1.07846402051283e-44, 1.88605464411243e-186, 1.09156111051203e-26, 0, 3.0702877273237e-124, 0.333333209689785, 0, 0, 0, 0, 0, 0, 3.09816093866831e-94, 0, 0, 4.7522727332095e-272, 0, 0, 2.30093251441394e-06, 0, 0, 1.27082826644707e-274, 0, 0, 0, 0, 0, 0, 0, 4.5662025456054e-65, 0, 2.77995853978268e-149, 0, 0, 0));list(argv[[1]]);");
    }

    @Test
    public void testlist56() {
        assertEval("argv <- list(class = 'data.frame', row.names = c(NA, 32L));list(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testlist57() {
        assertEval("argv <- list(structure(list(srcfile = c('/home/lzhao/tmp/Rtmpe5iuYI/R.INSTALL2aa854a74188/foreign/R/spss.R', '/home/lzhao/tmp/Rtmpe5iuYI/R.INSTALL2aa854a74188/foreign/R/spss.R'), frow = 164:165, lrow = c(164L, 169L)), .Names = c('srcfile', 'frow', 'lrow'), row.names = 1:2, class = 'data.frame'), structure(list(srcfile = '/home/lzhao/tmp/Rtmpe5iuYI/R.INSTALL2aa854a74188/foreign/R/spss.R', frow = 170L, lrow = 177L), .Names = c('srcfile', 'frow', 'lrow'), row.names = c(NA, -1L), class = 'data.frame'));list(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testlist58() {
        assertEval("argv <- list(structure(FALSE, .Names = 'Series 1', .Tsp = c(0, 0, 1), class = 'ts'), structure(FALSE, .Names = 'Series 1', .Tsp = c(1, 1, 1), class = 'ts'));list(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testlist59() {
        assertEval("argv <- list(structure(FALSE, .Tsp = c(1, 1, 1), class = 'ts'), structure(FALSE, .Tsp = c(1, 1, 1), class = 'ts'));list(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testlist60() {
        assertEval("argv <- list(Depends = structure(logical(0), .Dim = c(0L, 3L)), Installed = structure(logical(0), .Dim = c(0L, 3L)), R = structure(logical(0), .Dim = c(0L, 3L)));list(argv[[1]],argv[[2]],argv[[3]]);");
    }

    @Test
    public void testlist61() {
        assertEval("argv <- list(structure(c(TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE), .Dim = c(12L, 1L), .Dimnames = structure(list(`  p L s` = c('. . .', '| . .', '. | .', '| | .', '. . |', '| . |', '. | |', '| | |', '. . ?', '| . ?', '. | ?', '| | ?'), NULL), .Names = c('  p L s', ''))));list(argv[[1]]);");
    }

    @Test
    public void testlist62() {
        assertEval("argv <- list(values = c(-2572.90550008339+0i, -915.064609071159+0i, -456.632018115023+0i, 419.980933101553+0i, -366.745362912885+0i, -308.301779528581+0i, -258.104614655539+0i, -208.43876984087+0i, -174.152165416129+0i, 166.131403923756+0i, -153.932564395224+31.248756965275i, -153.932564395224-31.248756965275i, -145.261798316303+0i, -140.969649074553+0i, -109.026224585292+37.27313202252i, -109.026224585292-37.27313202252i, -95.4200045428049+0i, -94.2845517186135+0i, 93.6287479850051+0i, -83.7083948970612+39.7221174209657i, -83.7083948970612-39.7221174209657i, -89.7405335285911+14.6972603541884i, -89.7405335285911-14.6972603541884i, -90.4677652619726+0i, 80.9227484547009+0i, -79.2808369338756+0i, -67.7641499054793+34.4882180369511i, -67.7641499054793-34.4882180369511i, -74.7131802385517+0i, -72.7892236613541+0i, -70.8748882290923+0i, -65.326216345093+24.6325729497989i, -65.326216345093-24.6325729497989i, -65.6613463045206+12.2012477360608i, -65.6613463045206-12.2012477360608i, -64.009437139127+0i, -53.8555784147338+28.3814233344012i, -53.8555784147338-28.3814233344012i, -60.372612826631+0i, -55.598407412763+0i, -53.8337490558365+13.1765372798343i, -53.8337490558365-13.1765372798343i, -48.7010835501729+24.5244827641945i, -48.7010835501729-24.5244827641945i, -51.620171425175+0i, -49.1047272072286+7.0804434036442i, -49.1047272072286-7.0804434036442i, -44.0755122578262+21.8965512206582i, -44.0755122578262-21.8965512206582i, -47.6686025497685+0i, -47.0350049752776+0i, 43.2054741656531+0i, -42.0546965543942+0i, -41.4311176038551+0i, -36.4574226401686+16.1634950480082i, -36.4574226401686-16.1634950480082i, -39.2901755793811+0i, -36.5376333751307+11.2152902727145i, -36.5376333751307-11.2152902727145i, -38.0398197891428+0i, -32.9946255929378+12.9867445602001i, -32.9946255929378-12.9867445602001i, -34.7321001383969+0i, -32.0667502593492+12.342590095597i, -32.0667502593492-12.342590095597i, -27.2830437098322+11.6992356475951i, -27.2830437098322-11.6992356475951i, -29.1247671355682+2.0847233845627i, -29.1247671355682-2.0847233845627i, -28.1216021055426+0i, -27.0745572919711+0i, 26.1565478253913+0i, -23.4210302095847+1.8723763695687i, -23.4210302095847-1.8723763695687i, 20.782836979896+0i, 16.5058357149619+0i, -15.9316203363047+0i, 13.2377600042936+0i, -11.9119569568831+0i, -11.1832867499603+0i, 8.99100195370794+0i, 7.62805946796798+0i, -7.44159556589097+0i, -6.46699019595805+0i, 5.57838460483725+0i, 5.07382264677001+0i, -4.77172378340461+0i, 4.21976444063592+0i, -2.86123099075901+0i, -2.69814683135512+0i, -2.29820560404041+0i, 2.05951624519943+0i, -1.8306332549612+0i, 1.66021670517454+0i, 1.03505989993491+0i, -0.773887754953459+0i, -0.416100454072758+0i, 0.213086170361661+0i, -3.42336062193255e-06+0i, 3.42336057523814e-06+0i), vectors = NULL);list(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testlist63() {
        assertEval("argv <- list(x = 2.28125, y = 1.70580465116279, xlab = NULL, ylab = NULL);list(argv[[1]],argv[[2]],argv[[3]],argv[[4]]);");
    }

    @Test
    public void testlist64() {
        assertEval("argv <- list(fit = structure(numeric(0), .Dim = c(10L, 0L), constant = 0), se.fit = structure(numeric(0), .Dim = c(10L, 0L)), df = 10L, residual.scale = 0.523484262069588);list(argv[[1]],argv[[2]],argv[[3]],argv[[4]]);");
    }

    @Test
    public void testlist65() {
        // FIXME Besides whitespace diff in "function (x, y..." output there's
        // at several places e.g. after '[1] "signature"' GnuR outputs
        // attr(,"class")attr(,"package")
        // while FastR outputs just
        // attr(,"package")
        assertEval(Output.IgnoreWhitespace, Ignored.OutputFormatting,
                        "argv <- list(ANY = structure(function (x, y = NULL) .Internal(crossprod(x, y)), target = structure('ANY', class = structure('signature', package = 'methods'), .Names = 'x', package = 'methods'), defined = structure('ANY', class = structure('signature', package = 'methods'), .Names = 'x', package = 'methods'), generic = structure('crossprod', package = 'base'), class = structure('derivedDefaultMethod', package = 'methods')));list(argv[[1]]);");
    }

    @Test
    public void testRefCount() {
        assertEval("{ l <- list(a=c(1,2)); l2 <- l; l$a[[1]] <- 3; l2 }");
    }

    @Test
    public void testList() {
        assertEval("{ list(a=1, b=2) }");
        assertEval("{ list(a=1, 2) }");
        assertEval("{ list(1, b=2) }");
        assertEval("{ x<-c(y=1, 2);  list(a=x, 42) }");
        assertEval("{ x<-list(y=1, 2);  c(a=x, 42) }");
        assertEval("{ x<-list(y=1, 2);  c(42, a=x) }");
        assertEval("{ x<-list(y=1, 2);  c(a=x, c(z=7,42)) }");
        assertEval("{ x<-list(y=1, 2);  c(a=x, c(y=7,z=42)) }");
    }
}
