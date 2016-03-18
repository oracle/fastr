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
public class TestBuiltin_isS4 extends TestBase {

    @Test
    public void testisS41() {
        assertEval("argv <- list(c('time', 'status'));isS4(argv[[1]]);");
    }

    @Test
    public void testisS42() {
        assertEval("argv <- list(structure(1:10, .Tsp = c(1959.25, 1961.5, 4), class = 'ts'));isS4(argv[[1]]);");
    }

    @Test
    public void testisS43() {
        assertEval("argv <- list(1.79769313486232e+308);isS4(argv[[1]]);");
    }

    @Test
    public void testisS44() {
        assertEval("argv <- list(structure(c('a1', NA, NA, 'a4'), class = 'AsIs'));isS4(argv[[1]]);");
    }

    @Test
    public void testisS45() {
        assertEval("argv <- list(structure(list(Df = c(NA, 2L, 2L), Deviance = c(NA, 5.45230478674972, 2.66453525910038e-15), `Resid. Df` = c(8L, 6L, 4L), `Resid. Dev` = c(10.5814458637509, 5.12914107700115, 5.12914107700115)), .Names = c('Df', 'Deviance', 'Resid. Df', 'Resid. Dev'), row.names = c('NULL', 'outcome', 'treatment'), class = c('anova', 'data.frame'), heading = 'Analysis of Deviance Table\\n\\nModel: poisson, link: log\\n\\nResponse: counts\\n\\nTerms added sequentially (first to last)\\n\\n'));isS4(argv[[1]]);");
    }

    @Test
    public void testisS46() {
        assertEval("argv <- list(structure(list(f = structure(c(1L, 1L, 1L), .Label = c('1', '2'), class = 'factor'), u = structure(12:14, unit = 'kg', class = 'avector')), .Names = c('f', 'u'), row.names = 2:4, class = 'data.frame'));isS4(argv[[1]]);");
    }

    @Test
    public void testisS47() {
        assertEval("argv <- list(structure(c(1+1i, 3+1i, 2+1i, 4+1i, 5-1i, 7-1i, 6-1i, 8-1i), .Dim = c(2L, 2L, 2L)));isS4(argv[[1]]);");
    }

    @Test
    public void testisS48() {
        assertEval("argv <- list(structure(list(a = c(1L, 2L, 3L, NA), b = c(NA, 3.14159265358979, 3.14159265358979, 3.14159265358979), c = c(TRUE, NA, FALSE, TRUE), d = structure(c(1L, 2L, NA, 3L), .Label = c('aa', 'bb', 'dd'), class = 'factor'), e = structure(c(1L, NA, NA, 2L), .Label = c('a1', 'a4'), class = 'factor'), f = structure(c(11323, NA, NA, 12717), class = 'Date')), .Names = c('a', 'b', 'c', 'd', 'e', 'f'), row.names = c(NA, -4L), class = 'data.frame', data_types = c('N', 'N', 'L', 'C', 'C', 'D')));isS4(argv[[1]]);");
    }

    @Test
    public void testisS49() {
        assertEval("argv <- list(structure(3.14159265358979, class = structure('3.14159265358979', class = 'testit')));isS4(argv[[1]]);");
    }

    @Test
    public void testisS410() {
        assertEval("argv <- list(structure(list(coefficients = structure(c(62.4053692999179, 1.55110264750845, 0.510167579684914, 0.101909403579661, -0.144061029071015), .Names = c('(Intercept)', 'x1', 'x2', 'x3', 'x4')), residuals = structure(c(0.00476041849820263, 1.51120069970905, -1.67093753208295, -1.72710025504269, 0.250755561773019, 3.92544270216433, -1.44866908650026, -3.17498851728652, 1.3783494772083, 0.281547998741553, 1.99098357125943, 0.972989034920119, -2.2943340733616), .Names = c('1', '2', '3', '4', '5', '6', '7', '8', '9', '10', '11', '12', '13')), effects = structure(c(-344.052796708698, 38.0798677535417, -34.7531619513928, -3.12951579377076, 0.496965514049109, 4.50219669010871, -0.53327716269669, -2.71488989732451, 1.79317596396333, 1.57288365899254, 2.82474425399188, 1.8967418325489, -1.63480882826157), .Names = c('(Intercept)', 'x1', 'x2', 'x3', 'x4', '', '', '', '', '', '', '', '')), rank = 5L, fitted.values = structure(c(78.4952395815018, 72.7887993002909, 105.970937532083, 89.3271002550427, 95.649244438227, 105.274557297836, 104.1486690865, 75.6749885172865, 91.7216505227917, 115.618452001258, 81.8090164287406, 112.32701096508, 111.694334073362), .Names = c('1', '2', '3', '4', '5', '6', '7', '8', '9', '10', '11', '12', '13')), assign = 0:4, qr = structure(list(qr = structure(c(-3.60555127546399, 0.277350098112615, 0.277350098112615, 0.277350098112615, 0.277350098112615, 0.277350098112615, 0.277350098112615, 0.277350098112615, 0.277350098112615, 0.277350098112615, 0.277350098112615, 0.277350098112615, 0.277350098112615, -26.9029595169236, 20.3772120082893, -0.178565892506596, -0.178565892506596, 0.0177318148722558, -0.178565892506596, 0.214029522251108, 0.312178375940534, 0.263103949095821, -0.669310160953726, 0.312178375940534, -0.178565892506596, -0.129491465661883, -173.621161418497, 12.3214560939341, -52.4773668110126, -0.304364982610169, 0.171821393780133, 0.152975037639542, 0.609896101816292, -0.114330335930658, 0.304950385474031, -0.189506282456539, 0.0571721716629835, 0.362589213587327, 0.41970434660942, -42.43456501123, -18.2858864335223, -1.11991681104158, -12.5171816310368, -0.405342735734607, 0.108637576500954, 0.150506108798058, 0.497910771855039, 0.197741319088291, 0.429225499683342, 0.557905444893665, 0.0843208353807417, -0.0702259750833564, -108.16653826392, -14.2315837849668, 54.6072781350954, 12.8688326829848, -3.44968738078449, -0.0383654655076831, 0.50336264362848, 0.326250451511037, -0.0404173233188265, 0.0147578414456289, 0.526049642157631, 0.437713824243366, 0.410519010978314), .Dim = c(13L, 5L), .Dimnames = list(c('1', '2', '3', '4', '5', '6', '7', '8', '9', '10', '11', '12', '13'), c('(Intercept)', 'x1', 'x2', 'x3', 'x4')), assign = 0:4), qraux = c(1.27735009811261, 1.31217837594053, 1.17203087181661, 1.08180209589898, 1.00399408483144), pivot = 1:5, tol = 1e-07, rank = 5L), .Names = c('qr', 'qraux', 'pivot', 'tol', 'rank'), class = 'qr'), df.residual = 8L, xlevels = structure(list(), .Names = character(0)), call = quote(lm(formula = y ~ x1 + x2 + x3 + x4, data = d2)),     terms = quote(y ~ x1 + x2 + x3 + x4), model = structure(list(y = c(78.5, 74.3, 104.3, 87.6, 95.9, 109.2, 102.7, 72.5, 93.1, 115.9, 83.8, 113.3, 109.4), x1 = c(7, 1, 11, 11, 7, 11, 3, 1, 2, 21, 1, 11, 10), x2 = c(26, 29, 56, 31, 52, 55, 71, 31, 54, 47, 40, 66, 68), x3 = c(6, 15, 8, 8, 6, 9, 17, 22, 18, 4, 23, 9, 8), x4 = c(60, 52, 20, 47, 33, 22, 6, 44, 22, 26, 34, 12, 12)), .Names = c('y', 'x1', 'x2', 'x3', 'x4'), terms = quote(y ~ x1 + x2 + x3 + x4), row.names = c(NA, 13L), class = 'data.frame'),     formula = quote(y ~ x1 + x2 + x3 + x4)), .Names = c('coefficients', 'residuals', 'effects', 'rank', 'fitted.values', 'assign', 'qr', 'df.residual', 'xlevels', 'call', 'terms', 'model', 'formula'), class = 'lm'));isS4(argv[[1]]);");
    }

    @Test
    public void testisS411() {
        assertEval("argv <- list(3.14159265358979);isS4(argv[[1]]);");
    }

    @Test
    public void testisS412() {
        assertEval("argv <- list(structure(1, .Dim = 1L));isS4(argv[[1]]);");
    }

    @Test
    public void testisS413() {
        assertEval("argv <- list(structure(c(2L, 1L, 3L), .Label = c('1', '2', NA), class = 'factor'));isS4(argv[[1]]);");
    }

    @Test
    public void testisS414() {
        assertEval("argv <- list(structure(list(usr = c(0.568, 1.432, -1.08, 1.08), xaxp = c(0.6, 1.4, 4), yaxp = c(-1, 1, 4)), .Names = c('usr', 'xaxp', 'yaxp')));isS4(argv[[1]]);");
    }

    @Test
    public void testisS415() {
        assertEval("argv <- list(structure(list(row.names = c('rate', 'additive', 'rate:additive', 'Residuals'), SS = structure(list(rate = structure(c(1.7405, -1.5045, 0.855500000000001, -1.5045, 1.3005, -0.739500000000001, 0.855500000000001, -0.739500000000001, 0.420500000000001), .Dim = c(3L, 3L), .Dimnames = list(c('tear', 'gloss', 'opacity'), c('tear', 'gloss', 'opacity'))), additive = structure(c(0.760499999999999, 0.682499999999998, 1.9305, 0.682499999999998, 0.612499999999998, 1.7325, 1.9305, 1.7325, 4.90050000000001), .Dim = c(3L, 3L), .Dimnames = list(c('tear', 'gloss', 'opacity'), c('tear', 'gloss', 'opacity'))), `rate:additive` = structure(c(0.000500000000000012, 0.0165000000000002, 0.0445000000000006, 0.0165000000000002, 0.5445, 1.4685, 0.0445000000000006, 1.4685, 3.9605), .Dim = c(3L, 3L), .Dimnames = list(c('tear', 'gloss', 'opacity'), c('tear', 'gloss', 'opacity'))), Residuals = structure(c(1.764, 0.0200000000000005, -3.07, 0.0200000000000005, 2.628, -0.551999999999994, -3.07, -0.551999999999994, 64.924), .Dim = c(3L, 3L), .Dimnames = list(c('tear', 'gloss', 'opacity'), c('tear', 'gloss', 'opacity')))), .Names = c('rate', 'additive', 'rate:additive', 'Residuals')), Eigenvalues = structure(c(1.61877188028067, 0.911918322770912, 0.286826136427727, -8.75998844614162e-17, -6.73817551294033e-18, 8.58370095630716e-18, 1.36263996836868e-17, -6.73817551294033e-18, -2.24871081520413e-19), .Dim = c(3L, 3L), .Dimnames = list(c('rate', 'additive', 'rate:additive'), NULL)), stats = structure(c(1, 1, 1, 16, 0.618141615338857, 0.476965104581081, 0.222894242126575, NA, 7.55426877464313, 4.25561883959759, 1.33852196999606, NA, 3, 3, 3, NA, 14, 14, 14, NA, 0.00303404516026092, 0.0247452809990207, 0.301781645099671, NA), .Dim = c(4L, 6L), .Dimnames = list(c('rate', 'additive', 'rate:additive', 'Residuals'), c('Df', 'Pillai', 'approx F', 'num Df', 'den Df', 'Pr(>F)')))), .Names = c('row.names', 'SS', 'Eigenvalues', 'stats'), class = 'summary.manova'));isS4(argv[[1]]);");
    }

    @Test
    public void testisS416() {
        assertEval("argv <- list(4.94065645841247e-324);isS4(argv[[1]]);");
    }

    @Test
    public void testisS417() {
        assertEval("argv <- list(structure(list(), .Names = character(0), row.names = integer(0), class = 'data.frame'));isS4(argv[[1]]);");
    }
}
