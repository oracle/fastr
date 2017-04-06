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
public class TestBuiltin_paste0 extends TestBase {

    @Test
    public void testpaste01() {
        assertEval("argv <- list(list('2', ': '), NULL); .Internal(paste0(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testpaste02() {
        assertEval("argv <- list(list(structure(c('-0.20', ' 0.07', ' 0.16', ' 0.55', ' 0.13', '-0.07', '-0.08', '-0.48', ' 0.22', ' 0.04', '-0.34', '-0.38', '-0.02', '-0.23', ' 0.09', '-0.02', ' 0.12', '-0.03', ' 0.23', '-1.02', '-0.46', '-0.25', ' 0.75', '-1.16', ' 0.65', ' 1.66', ' 0.51', ' 2.09', ' 0.04', ' 0.01', ' 0.10', ' 0.27', ' 0.04', ' 0.33', ' 0.06', ' 0.53'), .Dim = c(4L, 9L), .Dimnames = list(c('Chile', 'United States', 'Zambia', 'Libya'), c('dfb.1_', 'dfb.pp15', 'dfb.pp75', 'dfb.dpi', 'dfb.ddpi', 'dffit', 'cov.r', 'cook.d', 'hat'))), c('', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '_*', '', '', '', '_*', '_*', '_*', '_*', '_*', '', '', '', '', '', '_*', '', '_*')), NULL); .Internal(paste0(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testpaste03() {
        assertEval("argv <- list(list(c('\\\'1\\\'', '\\\'2\\\'', NA)), ','); .Internal(paste0(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testpaste04() {
        assertEval("argv <- list(list('  ‘help.search()’ or ‘', '??'), NULL); .Internal(paste0(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testpaste05() {
        assertEval("argv <- list(list(structure(c('coef.aov', 'extractAIC.aov', 'model.tables.aov', 'print.aov', 'proj.aov', 'se.contrast.aov', 'summary.aov', 'TukeyHSD.aov'), class = 'MethodsFunction', info = structure(list(visible = c(FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, TRUE), from = structure(c(9L, 9L, 9L, 9L, 9L, 9L, 7L, 7L), .Label = c('CheckExEnv', 'package:base', 'package:datasets', 'package:graphics', 'package:grDevices', 'package:methods', 'package:stats', 'package:utils', 'registered S3method'), class = 'factor')), .Names = c('visible', 'from'), row.names = c('coef.aov', 'extractAIC.aov', 'model.tables.aov', 'print.aov', 'proj.aov', 'se.contrast.aov', 'summary.aov', 'TukeyHSD.aov'), class = 'data.frame')), c('*', '*', '*', '*', '*', '*', '', '')), NULL); .Internal(paste0(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testpaste06() {
        assertEval("argv <- list(list(character(0), character(0)), '\\n'); .Internal(paste0(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testpaste07() {
        assertEval("argv <- list(list(), NULL); .Internal(paste0(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testpaste010() {
        assertEval("argv <- list(list(structure(c('-0.16', '-0.03', ' 0.11', '-0.19', ' 0.12', ' 0.00', '-0.03', ' 0.18', ' 0.01', '-0.03', '-0.11', '-0.11', ' 0.00', ' 0.09', '-0.15', ' 0.05', '-0.04', ' 0.00', ' 0.04', ' 0.00', ' 0.01', ' 0.04', ' 0.11', ' 0.11', '-0.10', '-0.01', ' 0.04', ' 0.34', '-0.30', ' 0.00', '-0.02', '-0.31', '-0.04', '-0.01', '-0.05', '-0.05', '-0.07', '-0.03', ' 0.02', ' 0.21', '-0.17', ' 0.00', '-0.03', '-0.17', '-0.03', '-0.04', ' 0.28', '-0.45', '-0.24', ' 0.00', ' 0.02', ' 0.25', '-0.28', ' 0.00', '-0.01', '-0.24', '-0.04', '-0.04', '-0.06', '-0.06', '-0.44', '-0.01', ' 0.02', ' 0.18', '-0.26', ' 0.00', ' 0.00', '-0.19', '-0.03', '-0.03', '-0.04', '-0.04', ' 0.01', '-0.03', ' 0.04', ' 0.18', '-0.11', ' 0.00', '-0.02', '-0.15', '-0.02', '-0.02', '-0.06', '-0.06', ' 0.12', ' 0.08', ' 0.07', '-0.01', '-0.03', ' 0.00', '-0.01', ' 0.00', ' 0.00', ' 0.04', '-0.01', '-0.01', ' 0.00', ' 0.01', ' 0.08', '-0.01', ' 0.01', ' 0.00', '-0.02', ' 0.01', ' 0.00', ' 0.03', '-0.07', '-0.07', ' 0.21', ' 0.00', '-0.08', ' 0.04', '-0.42', ' 0.00', ' 0.01', ' 0.01', ' 0.00', ' 0.02', ' 0.05', ' 0.05', ' 0.06', '-0.04', '-0.09', ' 0.01', ' 0.01', ' 0.00', ' 0.01', '-0.02', ' 0.00', ' 0.03', ' 0.04', ' 0.04', '-0.04', '-0.01', ' 0.16', '-0.06', '-0.03', ' 0.00', ' 0.02', '-0.07', ' 0.00', ' 0.06', ' 0.08', ' 0.08', ' 0.47', '-0.15', ' 0.06', ' 0.01', ' 0.18', ' 0.00', '-0.03', ' 0.01', ' 0.00', '-0.02', '-0.06', '-0.06', '-0.04', ' 0.01', ' 0.09', '-0.08', '-0.10', ' 0.00', ' 0.05', '-0.18', ' 0.01', '-0.03', ' 0.12', ' 0.12', ' 0.03', ' 0.02', ' 0.12', '-0.08', '-0.11', ' 0.00', ' 0.03', '-0.14', ' 0.01', '-0.02', ' 0.10', ' 0.10', ' 0.73', '-0.24', '-0.26', '-0.44', '-0.89', '  NaN', ' 0.08', ' 0.45', ' 0.05', ' 0.14', ' 0.64', '-0.64', ' 1.71', ' 2.09', ' 1.86', ' 1.76', ' 0.13', '  NaN', ' 1.63', ' 1.92', ' 1.88', ' 1.60', ' 2.68', ' 2.68', ' 0.04', ' 0.00', ' 0.00', ' 0.01', ' 0.05', '  NaN', ' 0.00', ' 0.01', ' 0.00', ' 0.00', ' 0.03', ' 0.03', ' 0.39', ' 0.43', ' 0.36', ' 0.36', ' 0.06', ' 1.00', ' 0.26', ' 0.40', ' 0.36', ' 0.25', ' 0.57', ' 0.57'), .Dim = c(12L, 19L), .Dimnames = list(c('8', '19', '28', '39', '42', '57', '66', '80', '83', '87', '89', '93'), c('dfb.1_', 'dfb.Wght', 'dfb.Cyl4', 'dfb.Cyl5', 'dfb.Cyl6', 'dfb.Cyl8', 'dfb.Cyln', 'dfb.TypL', 'dfb.TypM', 'dfb.TypSm', 'dfb.TypSp', 'dfb.TypV', 'dfb.EngS', 'dfb.DrTF', 'dfb.DrTR', 'dffit', 'cov.r', 'cook.d', 'hat'))), c('', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '_*', '_*', '_*', '_*', '_*', '', '_*', '_*', '_*', '_*', '_*', '_*', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '_*', '', '', '', '', '_*', '_*')), NULL); .Internal(paste0(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testpaste011() {
        // FIXME:
        // FastR output: '[1] ""'
        // GnuR output: 'character(0)'
        assertEval(Ignored.ImplementationError, "argv <- list(list(character(0), character(0), character(0)), NULL); .Internal(paste0(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testpaste012() {
        assertEval("argv <- list(list(c('Package:', 'Type:', 'Version:', 'Date:', 'License:', 'Depends:'), ' \\\\tab ', structure(c('myTst2', 'Package', '1.0', '2014-03-17', 'What license is it under?', 'methods'), .Names = c('Package', 'Type', 'Version', 'Date', 'License', 'Depends')), '\\\\cr'), NULL); .Internal(paste0(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testpaste013() {
        assertEval("argv <- list(list(character(0), '$y'), NULL); .Internal(paste0(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testpaste014() {
        assertEval("argv <- list(list(c('text> ', 'text> ', 'text> ', 'text+ '), c('## The following two examples use latin1 characters: these may not', '## appear correctly (or be omitted entirely).', 'plot(1:10, 1:10, main = \\\'text(...) examples\\\\n~~~~~~~~~~~~~~\\\',', '     sub = \\\'R is GNU ©, but not ® ...\\\')')), '\\n'); .Internal(paste0(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testpaste015() {
        assertEval("argv <- list(list('cnstrO> ', 'constrOptim(c(2,-1,-1), fQP, gQP, ui = t(Amat), ci = bvec)'), '\\n'); .Internal(paste0(argv[[1]], argv[[2]]))");
    }
}
