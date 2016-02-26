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

import org.junit.*;

import com.oracle.truffle.r.test.*;

// Checkstyle: stop line length check
public class TestBuiltin_matrix extends TestBase {

    @Test
    public void testmatrix1() {
        assertEval("argv <- list(NA_real_, 1L, 5L, FALSE, list('Residuals', c('Df', 'Sum Sq', 'Mean Sq', 'F value', 'Pr(>F)')), FALSE, FALSE); .Internal(matrix(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testmatrix2() {
        assertEval("argv <- list(c(NA, 'a', 'b', '10', NA, NA, 'd', '12', NA, NA, NA, '14'), 1, 4, TRUE, NULL, TRUE, FALSE); .Internal(matrix(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testmatrix3() {
        assertEval("argv <- list(NA, 0L, 0L, FALSE, NULL, FALSE, FALSE); .Internal(matrix(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testmatrix4() {
        assertEval("argv <- list('foo', 1L, 1, FALSE, list(structure('object', simpleOnly = TRUE), NULL), FALSE, TRUE); .Internal(matrix(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testmatrix5() {
        assertEval("argv <- list(c(1, 2, 3, 0, 10, NA), 3, 2, FALSE, NULL, FALSE, FALSE); .Internal(matrix(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testmatrix6() {
        assertEval("argv <- list(1:25, 5, 5, FALSE, NULL, FALSE, FALSE); .Internal(matrix(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testmatrix7() {
        assertEval("argv <- list('ANY', 2L, 3L, FALSE, NULL, FALSE, FALSE); .Internal(matrix(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testmatrix8() {
        assertEval("argv <- list(0L, 1L, 1L, TRUE, NULL, FALSE, FALSE); .Internal(matrix(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testmatrix9() {
        assertEval("argv <- list(0L, 0L, 0L, FALSE, NULL, FALSE, FALSE); .Internal(matrix(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testmatrix10() {
        assertEval("argv <- list(c(1, 1, 1, 1, 1, 1), 1, 1, FALSE, NULL, TRUE, TRUE); .Internal(matrix(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testmatrix11() {
        assertEval("argv <- list(c(420.223016031624, 290.609964753365, 290.609964753365, 200), 2, 2, FALSE, NULL, FALSE, FALSE); .Internal(matrix(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testmatrix12() {
        assertEval("argv <- list(1:4, 1, 2, FALSE, NULL, TRUE, FALSE); .Internal(matrix(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testmatrix13() {
        assertEval(Ignored.Unknown, "argv <- list(c(0, 0, 0, 0), 4L, 0L, FALSE, NULL, FALSE, FALSE); .Internal(matrix(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testmatrix14() {
        assertEval("argv <- list(0+0i, 7L, 2L, FALSE, NULL, FALSE, FALSE); .Internal(matrix(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testmatrix15() {
        assertEval("argv <- list(0L, 1L, 2L, FALSE, NULL, FALSE, FALSE); .Internal(matrix(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testmatrix16() {
        assertEval("argv <- list(0, 12L, 12L, FALSE, NULL, FALSE, FALSE); .Internal(matrix(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testmatrix17() {
        assertEval("argv <- list(NA, 240, 4L, FALSE, NULL, FALSE, FALSE); .Internal(matrix(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testmatrix18() {
        assertEval("argv <- list(c(1448665, 18316380, 77.4, 139.8, 2251281, 26424236, 120.3, 201.7, NA, NA, 1835812, 22608335, 98.1, 172.5), 2L, 7L, FALSE, list(c('Ncells', 'Vcells'), c('used', '(Mb)', 'gc trigger', '(Mb)', 'limit (Mb)', 'max used', '(Mb)')), FALSE, FALSE); .Internal(matrix(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testmatrix19() {
        assertEval("argv <- list(c('315.45', '363.01', '405.02', '443.06', '478.09', '510.72'), 1L, 1, TRUE, list('1979', c('Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun')), FALSE, TRUE); .Internal(matrix(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testmatrix20() {
        assertEval("argv <- list('', 0L, 0L, FALSE, NULL, FALSE, FALSE); .Internal(matrix(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testmatrix21() {
        assertEval("argv <- list(character(0), 0L, 2L, FALSE, NULL, FALSE, FALSE); .Internal(matrix(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testmatrix22() {
        assertEval("argv <- list(raw(0), 0L, 0L, FALSE, NULL, FALSE, FALSE); .Internal(matrix(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testmatrix23() {
        assertEval("argv <- list(character(0), 0L, 17L, FALSE, NULL, FALSE, FALSE); .Internal(matrix(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testmatrix24() {
        assertEval(Ignored.Unknown,
                        "argv <- list(c(TRUE, FALSE, FALSE, TRUE), 2L, 2L, TRUE, NULL, FALSE, FALSE); .Internal(matrix(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testmatrix25() {
        assertEval("argv <- list(c(0.342020143325669, 0, -0.939692620785908, 0, 1, 0, 0.939692620785908, 0, 0.342020143325669), 3, 3, FALSE, NULL, FALSE, FALSE); .Internal(matrix(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testmatrix26() {
        assertEval("argv <- list(NA_integer_, 1L, 1L, FALSE, NULL, FALSE, FALSE); .Internal(matrix(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testmatrix27() {
        assertEval("argv <- list(numeric(0), 1, 1L, FALSE, NULL, TRUE, FALSE); .Internal(matrix(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testmatrix28() {
        assertEval("argv <- list(NA_complex_, 5L, 1L, FALSE, NULL, FALSE, FALSE); .Internal(matrix(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testmatrix29() {
        assertEval("argv <- list(NA_character_, 4L, 17L, FALSE, NULL, FALSE, FALSE); .Internal(matrix(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testmatrix30() {
        assertEval("argv <- list(c(2.8421709430404e-14, 0, 0, 0, 0, 0, 0, -4.44089209850063e-16, 0, 0, 0, 0, 0, 0, 4.44089209850063e-16, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 7.105427357601e-15), 6L, 6L, FALSE, NULL, FALSE, FALSE); .Internal(matrix(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testmatrix31() {
        assertEval("argv <- list(structure(integer(0), .Dim = c(0L, 2L), .Dimnames = list(NULL, c('row', 'col'))), 0, 2, FALSE, NULL, FALSE, FALSE); .Internal(matrix(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testmatrix32() {
        assertEval("argv <- list(c(24.2407407407407, 22.3953488372093, 24.4761904761905, 25.625, 24.3518518518519, 30.8333333333333, 30.8333333333333, 30.8333333333333, 24.2037037037037, 30.5, 30.5, 30.5, 24.4444444444444, 30.8571428571429, 30.8571428571429, 30.8571428571429, 24.2037037037037, 30.5, 30.5, 30.5, 24.2407407407407, 31.4545454545455, 31.4545454545455, 31.4545454545455, 24.8333333333333, 30.7857142857143, 30.7857142857143, 30.7857142857143, 24.7777777777778, 31.1428571428571, 31.1428571428571, 31.1428571428571, 24.2407407407407, 31.4545454545455, 31.4545454545455, 31.4545454545455, 24.2407407407407, 31.4545454545455, 31.4545454545455, 31.4545454545455, 24.2037037037037, 30.5, 30.5, 30.5, 24.5185185185185, 30.6428571428571, 30.6428571428571, 30.6428571428571, 24.8333333333333, 31.2857142857143, 31.2857142857143, 31.2857142857143, 24.8333333333333, 22.75, 20.5789473684211, 20.5789473684211, 24.5185185185185, 22.375, 24.4, 23.5833333333333, 24.8148148148148, 22.4615384615385, 20.2222222222222, 20.2222222222222, 24.4444444444444, 22.2, 24.05, 24.05, 24.3518518518519, 30.8333333333333, 30.8333333333333, 30.8333333333333, 24.8148148148148, 22.4615384615385, 24.3809523809524, 25.5714285714286, 24.8333333333333, 22.575, 24.3333333333333, 23.5384615384615, 24.4444444444444, 22.2, 24.05, 24.05, 24.3518518518519, 30.8333333333333, 30.8333333333333, 30.8333333333333, 24.2037037037037, 22.4047619047619, 24.4761904761905, 24.4761904761905, 24.8148148148148, 22.4615384615385, 24.3809523809524, 25.5714285714286, 24.8148148148148, 22.4615384615385, 24.3809523809524, 25.5714285714286, 24.3518518518519, 22.5, 20.695652173913, 20.695652173913, 24.2407407407407, 22.3953488372093, 24.4761904761905, 25.625, 24.5185185185185, 22.375, 24.4, 23.5833333333333, 24.2407407407407, 22.3953488372093, 24.4761904761905, 23.7692307692308, 24.4444444444444, 22.2, 24.05, 24.05, 24.5185185185185, 22.375, 20.35, 20.35, 24.5185185185185, 22.375, 24.4, 23.5833333333333, 24.7777777777778, 22.55, 24.65, 26, 24.8148148148148, 30.9333333333333, 30.9333333333333, 30.9333333333333, 24.7777777777778, 22.55, 24.65, 23.9230769230769, 24.8333333333333, 22.575, 24.3333333333333, 23.5384615384615, 24.7777777777778, 22.55, 24.65, 23.9230769230769, 24.8333333333333, 22.575, 20.6315789473684, 20.6315789473684, 24.8333333333333, 22.75, 24.7142857142857, 24.7142857142857, 24.8333333333333, 22.75, 24.7142857142857, 24.7142857142857, 24.8148148148148, 22.4615384615385, 20.2222222222222, 20.2222222222222, 24.2037037037037, 22.4047619047619, 20.3333333333333, 20.3333333333333, 24.7777777777778, 22.55, 20.45, 20.45, 24.8148148148148, 22.4615384615385, 20.2222222222222, 20.2222222222222, 24.3518518518519, 22.5, 24.6842105263158, 24.6842105263158, 24.8148148148148, 22.4615384615385, 20.2222222222222, 20.2222222222222, 24.4444444444444, 22.2, 20.35, 20.35, 24.2037037037037, 22.4047619047619, 24.4761904761905, 24.4761904761905, 24.5185185185185, 22.375, 20.35, 20.35, 24.8148148148148, 22.4615384615385, 20.2222222222222, 20.2222222222222, 24.8148148148148, 22.4615384615385, 20.2222222222222, 20.2222222222222, 24.3518518518519, 22.5, 20.695652173913, 20.695652173913, 24.8148148148148, 22.4615384615385, 20.2222222222222, 20.2222222222222, 24.8333333333333, 22.575, 20.6315789473684, 20.6315789473684, 24.7777777777778, 22.55, 20.45, 20.45, 24.8333333333333, 22.75, 20.5789473684211, 20.5789473684211, 24.8333333333333, 22.575, 20.6315789473684, 20.6315789473684, 24.8333333333333, 22.75, 20.5789473684211, 20.5789473684211, 24.4444444444444, 22.2, 20.35, 20.35, 24.8148148148148, 22.4615384615385, 20.2222222222222, 20.2222222222222), 60L, 1, TRUE, list(c('Eagle Summit 4', 'Ford Escort   4', 'Ford Festiva 4', 'Honda Civic 4', 'Mazda Protege 4', 'Mercury Tracer 4', 'Nissan Sentra 4', 'Pontiac LeMans 4', 'Subaru Loyale 4', 'Subaru Justy 3', 'Toyota Corolla 4', 'Toyota Tercel 4', 'Volkswagen Jetta 4', 'Chevrolet Camaro V8', 'Dodge Daytona', 'Ford Mustang V8', 'Ford Probe', 'Honda Civic CRX Si 4', 'Honda Prelude Si 4WS 4', 'Nissan 240SX 4', 'Plymouth Laser', 'Subaru XT 4', 'Audi 80 4', 'Buick Skylark 4', 'Chevrolet Beretta 4', 'Chrysler Le Baron V6', 'Ford Tempo 4', 'Honda Accord 4', 'Mazda 626 4', 'Mitsubishi Galant 4', 'Mitsubishi Sigma V6', 'Nissan Stanza 4', 'Oldsmobile Calais 4', 'Peugeot 405 4', 'Subaru Legacy 4', 'Toyota Camry 4', 'Volvo 240 4', 'Acura Legend V6', 'Buick Century 4', 'Chrysler Le Baron Coupe', 'Chrysler New Yorker V6', 'Eagle Premier V6', 'Ford Taurus V6', 'Ford Thunderbird V6', 'Hyundai Sonata 4', 'Mazda 929 V6', 'Nissan Maxima V6', 'Oldsmobile Cutlass Ciera 4', 'Oldsmobile Cutlass Supreme V6', 'Toyota Cressida 6', 'Buick Le Sabre V6', 'Chevrolet Caprice V8', 'Ford LTD Crown Victoria V8', 'Chevrolet Lumina APV V6', 'Dodge Grand Caravan V6', 'Ford Aerostar V6', 'Mazda MPV V6', 'Mitsubishi Wagon 4', 'Nissan Axxess 4', 'Nissan Van 4'), structure(c('0.79767456', '0.28300396', '0.04154257', '0.01132626'), .Names = c('1', '2', '3', '4'))), FALSE, TRUE); .Internal(matrix(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testmatrix33() {
        assertEval(Ignored.Unknown,
                        "argv <- list(c(1+2i, 3-4i, 5+0i, -6+0i), 2L, 2L, TRUE, NULL, FALSE, FALSE); .Internal(matrix(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testmatrix34() {
        assertEval("argv <- list(NA, 2, 5, FALSE, NULL, FALSE, FALSE); .Internal(matrix(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testmatrix35() {
        assertEval("argv <- list(structure(logical(0), .Dim = c(1L, 0L), .Dimnames = list('r', NULL)), 0L, 0L, FALSE, NULL, FALSE, FALSE); .Internal(matrix(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testmatrix36() {
        assertEval(Ignored.Unknown,
                        "argv <- list(c(0, 0, 0, 0, 0, 0, 4.94065645841247e-324, 0, 0, 0, 0, 0), structure(12L, .Names = '1'), 1L, FALSE, NULL, FALSE, FALSE); .Internal(matrix(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testmatrix37() {
        assertEval(Ignored.Unknown, "argv <- list(1:7, 3, 4, FALSE, NULL, FALSE, FALSE); .Internal(matrix(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testmatrix38() {
        assertEval("argv <- list(c(1, 3, 2, -2, 16, 4, 3, 3, 1, 32, -3, 2, 3, 2, 5, 1, 3, 4, 9, 0, 4, 16, 3, 5, 1, -1, 3, 2, -3, 4, 1, 2, 8, 0, 8, 5, 4, 2, 6, 5, 3, 1, 3, 1), 4L, 4L, FALSE, NULL, FALSE, FALSE); .Internal(matrix(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testmatrix39() {
        assertEval(Ignored.Unknown,
                        "argv <- list(c(-1, 4, 4, 9, 5, 1, 4, 8, 8, 2, 6, 0, 2, 3, 8, 8, 4, 4, 2, 3, 4, 0, -1, 7, 2, 4, 2, 3, 5, 6, 6, 5, 4, 3, 7, -1, 3, 1, -1, 2, 32, 1, 4, 4), 2L, 5L, FALSE, NULL, FALSE, FALSE); .Internal(matrix(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testmatrix40() {
        assertEval("argv <- list(c(1259, 845, 719, 390, 1360, 1053, 774, 413), 2, 1, TRUE, NULL, FALSE, TRUE); .Internal(matrix(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testmatrix41() {
        assertEval("argv <- list(1:2, 2, 12, TRUE, NULL, FALSE, FALSE); .Internal(matrix(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testmatrix42() {
        assertEval("argv <- list(NA_character_, 1L, 17L, FALSE, NULL, FALSE, FALSE); .Internal(matrix(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testmatrix43() {
        assertEval("argv <- list(c('', '', '', '', '', '', ' 1', ' 2', ' 3', ' 4', ' 5', ' 6', ' 7', ' 8', ' 9', '10', '', '', '', '', '', '', '', ''), 1, 12, TRUE, list(c('1920', '1921'), c('Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec')), TRUE, FALSE); .Internal(matrix(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testmatrix44() {
        assertEval("argv <- list(structure(list(a1 = 1:3, a2 = 4:6, a3 = 3.14159265358979, a4 = c('a', 'b', 'c')), .Names = c('a1', 'a2', 'a3', 'a4')), 2, 2, FALSE, NULL, FALSE, FALSE); .Internal(matrix(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }
}
