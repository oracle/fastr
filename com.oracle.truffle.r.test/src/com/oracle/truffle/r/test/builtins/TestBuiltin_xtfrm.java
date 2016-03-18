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
public class TestBuiltin_xtfrm extends TestBase {

    @Test
    public void testxtfrm1() {
        assertEval("argv <- list(structure(c(4L, 5L, 3L, 2L, 1L), .Label = c('McNeil', 'Ripley', 'Tierney', 'Tukey', 'Venables'), class = 'factor'));xtfrm(argv[[1]]);");
    }

    @Test
    public void testxtfrm2() {
        assertEval("argv <- list(structure(c('Tukey', 'Venables', 'Tierney', 'Ripley', 'Ripley', 'McNeil', 'R Core'), class = 'AsIs'));xtfrm(argv[[1]]);");
    }

    @Test
    public void testxtfrm3() {
        assertEval("argv <- list(c('9', '9', '8', '7', '6', '5', '4', '3', '2', '1'));xtfrm(argv[[1]]);");
    }

    @Test
    public void testxtfrm4() {
        assertEval("argv <- list(list());xtfrm(argv[[1]]);");
    }

    @Test
    public void testxtfrm5() {
        assertEval(Ignored.Unknown, "argv <- list(NULL);xtfrm(argv[[1]]);");
    }

    @Test
    public void testxtfrm6() {
        assertEval("argv <- list(structure(c(11354, 11382, 11413), class = 'Date'));xtfrm(argv[[1]]);");
    }

    @Test
    public void testxtfrm7() {
        assertEval("argv <- list(structure(1:3, id = 'An Example', class = structure('numWithId', package = '.GlobalEnv')));xtfrm(argv[[1]]);");
    }

    @Test
    public void testxtfrm8() {
        assertEval("argv <- list(structure(1:48, .Label = c('1', '2', '3', '4', '5', '6', '7', '8', '9', '10', '11', '12', '13', '14', '15', '16', '17', '18', '19', '20', '21', '22', '23', '24', '25', '26', '27', '28', '29', '30', '31', '32', '33', '34', '35', '36', '37', '38', '39', '40', '41', '42', '43', '44', '45', '46', '47', '48'), class = 'factor'));xtfrm(argv[[1]]);");
    }

    @Test
    public void testxtfrm9() {
        assertEval("argv <- list(structure(c(0.00318983494910604, -0.0111499005186203, -0.00684577108225438, -0.0259425874127965, 0.0236273472621072, 0.0160653568112268, 0.0169512728197135, -0.0108668370208327, 0.0075620519889718, 0.000382355180538597, 0.0147085004994818, -0.0237528559595188, -0.00450253418485462, -0.00933970368616398, -0.00295998622541489, -0.0142621134100646, 0.0149334478604598, 0.0102146485133766, 0.00423575454949581, -0.00284331639280456, 0.0113535382887161, -0.00395897382574548, -0.00413390950862867, -0.0165713012838917, -0.018012405938438, 0.00225076128639717, 0.00786949110453678, 0.00890171601854386, 0.0241231688586559, 0.0104325796748375, 0.0267124035293778, -0.0242586202225146, 0.011564413201586, -0.00791916545648325, -0.020000071186273, -0.0160024870044187, 0.00243365269147765, 0.000371702019451462, 0.00543854321166064), .Names = c('1962.25', '1962.5', '1962.75', '1963', '1963.25', '1963.5', '1963.75', '1964', '1964.25', '1964.5', '1964.75', '1965', '1965.25', '1965.5', '1965.75', '1966', '1966.25', '1966.5', '1966.75', '1967', '1967.25', '1967.5', '1967.75', '1968', '1968.25', '1968.5', '1968.75', '1969', '1969.25', '1969.5', '1969.75', '1970', '1970.25', '1970.5', '1970.75', '1971', '1971.25', '1971.5', '1971.75')));xtfrm(argv[[1]]);");
    }

    @Test
    public void testxtfrm10() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(c(-0.00456054471709705, -0.0386783362736307, -0.0164476694679408, -0.191054486056936, -0.648560736432306, -0.0674820314025517, -0.0740415039370447, -0.0243389397466521, -0.00751319716764208, -2.290078352999e-05, -0.0207911295071267, -0.0697881087827301, -0.00348607275095251, -0.00951045408299201, -0.00166106374745221, -0.0259659490166321, -0.0293880116898911, -0.0140288480262381, -0.0027358575950958, -0.000491817024731849, -0.00823790481253382, -0.00148071888751321, -0.00122448210109329, -0.0168483392795904, -0.0206872529040578, -0.000471241810355829, -0.00239800266383851, -0.00875711097940079, -0.0461679706262251, -0.0100810672498937, -0.0758745277496017, -0.0537304828043233, -0.0171367489531612, -0.01057973675541, -0.0676085282986778, -0.069201293818924, -0.00065957876422003, -1.97617272327839e-05, -0.00439039022584134), .Names = c('1962.25', '1962.5', '1962.75', '1963', '1963.25', '1963.5', '1963.75', '1964', '1964.25', '1964.5', '1964.75', '1965', '1965.25', '1965.5', '1965.75', '1966', '1966.25', '1966.5', '1966.75', '1967', '1967.25', '1967.5', '1967.75', '1968', '1968.25', '1968.5', '1968.75', '1969', '1969.25', '1969.5', '1969.75', '1970', '1970.25', '1970.5', '1970.75', '1971', '1971.25', '1971.5', '1971.75')));xtfrm(argv[[1]]);");
    }

    @Test
    public void testxtfrm11() {
        assertEval("argv <- list(structure(list(c(2L, 10L, 0L), c(2L, 10L, 1L), c(2L, 10L, 1L, 1L), c(2L, 11L, 0L), c(2L, 11L, 1L), c(2L, 11L, 1L, 1L), c(2L, 12L, 0L), c(2L, 12L, 1L), c(2L, 12L, 2L), c(2L, 12L, 2L, 1L), c(2L, 13L, 0L), c(2L, 13L, 1L), c(2L, 13L, 2L), c(2L, 14L, 0L), c(2L, 14L, 1L), c(2L, 14L, 2L), c(2L, 15L, 0L), c(2L, 15L, 1L), c(2L, 15L, 2L), c(2L, 15L, 3L), c(3L, 0L, 0L), c(3L, 0L, 1L)), class = 'numeric_version'));xtfrm(argv[[1]]);");
    }

    @Test
    public void testxtfrm12() {
        assertEval("argv <- list(structure(list(c(2L, 11L, 0L)), class = 'numeric_version'));xtfrm(argv[[1]]);");
    }

    @Test
    public void testxtfrm13() {
        assertEval("argv <- list(structure(c(607L, 30L, 3L, 11L, 44L, 67L, 17L, 16L, 67L, 6L, 1L, 13L, 672L, 46L, 8L, 18L, 10L, 22L, 16L, 5L, 55L, 2L), .Dim = 22L, .Dimnames = structure(list(c('BUG FIXES', 'C-LEVEL FACILITIES', 'CODE MIGRATION', 'COMPRESSION', 'DEPRECATED & DEFUNCT', 'DEPRECATED AND DEFUNCT', 'GRAPHICS DEVICES', 'HELP & Rd FILE CHANGES', 'INSTALLATION', 'INTERNATIONALIZATION', 'LICENCE', 'LONG VECTORS', 'NEW FEATURES', 'PACKAGE INSTALLATION', 'PACKAGE parallel', 'PERFORMANCE IMPROVEMENTS', 'REGULAR EXPRESSIONS', 'SIGNIFICANT USER-VISIBLE CHANGES', 'SWEAVE & VIGNETTES', 'SWEAVE CHANGES', 'UTILITIES', 'WINDOWS-SPECIFIC CHANGES')), .Names = ''), class = 'table'));xtfrm(argv[[1]]);");
    }

    @Test
    public void testxtfrm15() {
        assertEval("argv <- list(structure(1:3, id = 'An Example', class = structure('numWithId',     package = '.GlobalEnv')));do.call('xtfrm', argv)");
    }
}
