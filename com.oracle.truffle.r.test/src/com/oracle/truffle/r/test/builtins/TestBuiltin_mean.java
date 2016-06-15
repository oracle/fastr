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
public class TestBuiltin_mean extends TestBase {

    @Test
    public void testmean1() {
        assertEval("argv <- list(c(95.4489970123773, 98.5489970123773, 98.5489970123773, 98.5489970123773, 98.5489970123773, 98.5489970123773)); .Internal(mean(argv[[1]]))");
    }

    @Test
    public void testmean2() {
        assertEval(Ignored.Unknown, "argv <- list(c(0.104166666666667, 0.285714285714286, 0.285714285714286, NA)); .Internal(mean(argv[[1]]))");
    }

    @Test
    public void testmean3() {
        assertEval("argv <- list(structure(c(59.8164361195774, 16.3642182644146, 111.747292631572, 33.1308121255244, 111.087966260681, 17.8530570409338, 109.920202428016, 21.131775457295, 0, 0, 16.3642182644146, 161.535939255833, 37.6748802609012, 67.2883398244609, 38.6252764993654, 76.8303935707398, 28.4778348377214, 75.3935967432183, 0, 0, 111.747292631572, 37.6748802609012, 398.433064545232, 41.228743361535, 56.6580728922266, 34.5026155985806, 59.292325604515, 36.0412835987832, 0, 0, 33.1308121255244, 67.2883398244609, 41.228743361535, 1176.45954558834, 42.4165944769534, 69.928075668575, 32.9974365646273, 68.4061187132491, 0, 0, 111.087966260681, 38.6252764993654, 56.6580728922266, 42.4165944769534, 1738.19143232074, 35.3995546471346, 61.6125843485971, 37.0026062612778, 0, 0, 17.8530570409338, 76.8303935707398, 34.5026155985806, 69.928075668575, 35.3995546471346, 3334.81773597237, 25.0719711616328, 77.7527739510622, 0, 0, 109.920202428016, 28.4778348377214, 59.292325604515, 32.9974365646273, 61.6125843485971, 25.0719711616328, 3310.21623403309, 26.7939833992556, 0, 0, 21.131775457295, 75.3935967432183, 36.0412835987832, 68.4061187132491, 37.0026062612778, 77.7527739510622, 26.7939833992556, 6145.64636329227, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0), .Dim = c(10L, 10L))); .Internal(mean(argv[[1]]))");
    }

    @Test
    public void testmean4() {
        assertEval("argv <- list(structure(c(36, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 33, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 40, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 28, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 50, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 47, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 31, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 55, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 38, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 42), .Dim = c(10L, 10L), .Dimnames = list(c('a1', 'a2', 'a3', 'a4', 'a5', 'a6', 'a7', 'a8', 'a9', 'a10'), c('a1', 'a2', 'a3', 'a4', 'a5', 'a6', 'a7', 'a8', 'a9', 'a10')))); .Internal(mean(argv[[1]]))");
    }

    @Test
    public void testmean5() {
        assertEval("argv <- list(c(FALSE, FALSE, FALSE, TRUE, TRUE, FALSE, FALSE, TRUE, TRUE, TRUE, FALSE, TRUE, FALSE, FALSE, FALSE, TRUE, FALSE, TRUE, TRUE, TRUE, FALSE, TRUE, TRUE, FALSE, TRUE, TRUE, FALSE, FALSE, TRUE, FALSE, TRUE, TRUE, TRUE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, TRUE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE, TRUE, TRUE, FALSE, TRUE, FALSE, FALSE, TRUE, FALSE, TRUE, TRUE, FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE, TRUE, FALSE, TRUE, TRUE, FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, TRUE, TRUE, FALSE, TRUE, TRUE, FALSE, TRUE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, TRUE, TRUE, TRUE, TRUE, FALSE, FALSE, TRUE, TRUE, TRUE, TRUE, FALSE, FALSE, FALSE, TRUE, TRUE, FALSE, TRUE, TRUE, TRUE, TRUE, FALSE, FALSE, FALSE, TRUE, FALSE, TRUE, FALSE, FALSE, FALSE, TRUE, FALSE, TRUE, TRUE, FALSE, FALSE, TRUE, TRUE, TRUE, TRUE, FALSE, FALSE, FALSE, FALSE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, FALSE, TRUE, FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, TRUE, TRUE, TRUE, FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, TRUE, FALSE, FALSE, TRUE, TRUE, FALSE, FALSE, FALSE, FALSE, TRUE, TRUE, FALSE, TRUE, FALSE, TRUE, TRUE, FALSE, TRUE, FALSE, FALSE, FALSE, TRUE)); .Internal(mean(argv[[1]]))");
    }

    @Test
    public void testmean6() {
        assertEval("argv <- list(c(-0.207917278019599, -0.0833690664718293, 0.878133487533042, 0.070508391424576, 0.460916205989202, 0.497850478229239, 0.400771450594052, 0.400771450594052, -0.380471001012383, -0.686852851893526, 1.25381492106993, 0.821581081637487, -0.402884835299076, 0.821581081637487, 0.11068271594512, -0.560475646552213, 1.55870831414912, -0.686852851893526, -1.26539635156826, 1.55870831414912, 0.11068271594512, 1.20796199830499, 0.153373117836515, -0.694706978920513, -0.466655353623219, 0.821581081637487, -1.06782370598685, 0.779965118336318, -0.402884835299076, -1.68669331074241, 0.460916205989202, -0.295071482992271, -0.207917278019599, 0.460916205989202, 1.25381492106993, -0.0833690664718293, 0.359813827057364, -1.06782370598685, 1.71506498688328, 0.11068271594512, 0.837787044494525, 1.78691313680308, 0.426464221476814, -0.0833690664718293, 0.426464221476814, -1.26506123460653, 0.688640254100091, 0.878133487533042, 0.497850478229239, -0.217974914658295)); .Internal(mean(argv[[1]]))");
    }

    @Test
    public void testmean7() {
        assertEval("argv <- list(1.47130567537631e-314); .Internal(mean(argv[[1]]))");
    }

    @Test
    public void testmean8() {
        assertEval("argv <- list(4.9306115419259e+108); .Internal(mean(argv[[1]]))");
    }

    @Test
    public void testmean9() {
        assertEval("argv <- list(structure(c(2134, 1863, 1877, 1877, 1492, 1249, 1280, 1131, 1209, 1492, 1621, 1846, 2103, 2137, 2153, 1833, 1403, 1288, 1186, 1133, 1053, 1347, 1545, 2066, 2020, 2750, 2283, 1479, 1189, 1160, 1113, 970, 999, 1208, 1467, 2059, 2240, 1634, 1722, 1801, 1246, 1162, 1087, 1013, 959, 1179, 1229, 1655, 2019, 2284, 1942, 1423, 1340, 1187, 1098, 1004, 970, 1140, 1110, 1812, 2263, 1820, 1846, 1531, 1215, 1075, 1056, 975), .Tsp = c(1974, 1979.58333333333, 12), class = 'ts')); .Internal(mean(argv[[1]]))");
    }

    @Test
    public void testmean10() {
        assertEval("argv <- list(c(TRUE, FALSE, TRUE, TRUE)); .Internal(mean(argv[[1]]))");
    }

    @Test
    public void testmean11() {
        assertEval("argv <- list(structure(c(103.87323943662, 103.87323943662, 103.87323943662, 103.87323943662, 103.87323943662, 103.87323943662, 103.87323943662, 103.87323943662, 103.87323943662, 103.87323943662, 103.87323943662, 103.87323943662, 103.87323943662, 103.87323943662, 103.87323943662, 103.87323943662, 103.87323943662, 103.87323943662, 103.87323943662, 103.87323943662, 103.87323943662, 103.87323943662, 103.87323943662, 103.87323943662, 103.87323943662, 103.87323943662, 103.87323943662, 103.87323943662, 103.87323943662, 103.87323943662, 103.87323943662, 103.87323943662, 103.87323943662, 103.87323943662, 103.87323943662, 103.87323943662, 103.87323943662, 103.87323943662, 103.87323943662, 103.87323943662, 103.87323943662, 103.87323943662, 103.87323943662, 103.87323943662, 103.87323943662, 103.87323943662, 103.87323943662, 103.87323943662, 103.87323943662, 103.87323943662, 103.87323943662, 103.87323943662, 103.87323943662, 103.87323943662, 103.87323943662, 103.87323943662, 103.87323943662, 103.87323943662, 103.87323943662, 103.87323943662, 103.87323943662, 103.87323943662, 103.87323943662, 103.87323943662, 103.87323943662, 103.87323943662, 103.87323943662, 103.87323943662, 103.87323943662, 103.87323943662, 103.87323943662), .Names = c('2', '3', '4', '5', '6', '7', '8', '9', '10', '11', '12', '13', '14', '15', '16', '17', '18', '19', '20', '21', '22', '23', '24', '25', '26', '27', '28', '29', '30', '31', '32', '33', '34', '35', '36', '37', '38', '39', '40', '41', '42', '43', '44', '45', '46', '47', '48', '49', '50', '51', '52', '53', '54', '55', '56', '57', '58', '59', '60', '61', '62', '63', '64', '65', '66', '67', '68', '69', '70', '71', '72'))); .Internal(mean(argv[[1]]))");
    }

    @Test
    public void testmean12() {
        assertEval("argv <- list(c(-1.12778377684043, -12820.0784261145, -21650982809.6744, -473300382255715392, -6.08456909882282e+25, -3.04622557026196e+34, -4.60125024792566e+43, -1.76183826972506e+53, -1.5069799345972e+63, -2.61556777274611e+73, -8.54170618068872e+83, -4.9383857330861e+94, -4.80716085942859e+105, -7.55412056676629e+116, -1.84898368353639e+128, -6.83535188151783e+139, -3.71562599613334e+151, -2.90089508183654e+163, -3.18582547396557e+175, -4.83110332887119e+187, -9.94902790498679e+199, -2.74100158340596e+212, -9.96611412047338e+224, -4.72336572671053e+237, -2.88514442494869e+250, -2.24780296109123e+263, -2.21240023126594e+276, -2.72671165723473e+289, -4.17369555651928e+302, -Inf, -Inf, -Inf, -Inf, -Inf, -Inf, -Inf, -Inf, -Inf, -Inf, -Inf, -Inf, -Inf, -Inf, -Inf, -Inf, -Inf, -Inf, -Inf, -Inf, -Inf, -Inf)); .Internal(mean(argv[[1]]))");
    }

    @Test
    public void testmean13() {
        assertEval("argv <- list(1:10); .Internal(mean(argv[[1]]))");
    }

    @Test
    public void testmean14() {
        assertEval("argv <- list(c(-2.16610675289233, 2.16610675289233)); .Internal(mean(argv[[1]]))");
    }

    @Test
    public void testmean15() {
        assertEval("argv <- list(c(-Inf, Inf)); .Internal(mean(argv[[1]]))");
    }

    @Test
    public void testmean16() {
        assertEval("argv <- list(numeric(0)); .Internal(mean(argv[[1]]))");
    }

    @Test
    public void testmean17() {
        assertEval("argv <- list(c(1.77635683940025e-15, 7.105427357601e-14, 4.54747350886464e-13, 4.54747350886464e-13, 1.81898940354586e-12, 7.27595761418343e-12, 7.27595761418343e-12, 1.45519152283669e-11, 2.91038304567337e-11, 5.82076609134674e-11)); .Internal(mean(argv[[1]]))");
    }

    @Test
    public void testmean19() {
        assertEval("argv <- structure(list(x = structure(c(31, NA, NA, 31), units = 'days',     class = 'difftime'), na.rm = TRUE), .Names = c('x', 'na.rm'));do.call('mean', argv)");
    }

    @Test
    public void testmean20() {
        assertEval("argv <- structure(list(x = c(TRUE, FALSE, TRUE, TRUE)), .Names = 'x');do.call('mean', argv)");
    }

    @Test
    public void testmean21() {
        assertEval("argv <- structure(list(x = c(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10,     50), trim = 0.5), .Names = c('x', 'trim'));do.call('mean', argv)");
    }

    @Test
    public void testmean22() {
        assertEval(Output.IgnoreWarningContext, "argv <- structure(list(x = structure(c(2L, 1L, 2L, 2L), .Label = c('FALSE',     'TRUE'), class = 'factor')), .Names = 'x');do.call('mean', argv)");
    }

    @Test
    public void testmean23() {
        assertEval("argv <- structure(list(x = c(83.7010937038573, 61.9895951152624,     259.87035947113, 58.4906618904788, 24.7573173158259, 27.3459081536165,     286.404145870861, 31.5386609266279, 11.4645558243349, 48.261763556938,     24.118141168773, 25.3079966732443)), .Names = 'x');" +
                        "do.call('mean', argv)");
    }

    @Test
    public void testMean() {
        assertEval("{ mean(c(5,5,5,5,5)) }");
        assertEval("{ mean(c(1,2,3,4,5)) }");
        assertEval("{ mean(c(2,4))}");
        assertEval("{ mean(c(2L,4L,3L))}");
        assertEval("{ mean(c(1,2,3,4,5))}");
        assertEval("{ mean(c(1+2i))}");
        assertEval("{ mean(c(1+2i, 2+3i))}");
        assertEval("{ mean(c(1+2i,1+3i,1+45i))}");
        assertEval("{ mean(c(TRUE, TRUE))}");
        assertEval("{ mean(c(TRUE, FALSE))}");
    }
}
