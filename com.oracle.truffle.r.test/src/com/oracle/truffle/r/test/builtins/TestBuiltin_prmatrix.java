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
public class TestBuiltin_prmatrix extends TestBase {

    @Test
    public void testprmatrix1() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(c(1, 6, 9, 6, 4, 1, 1, 2, 1, 0.305397625390859, 0.00170825768891124, 8.51556634078892e-12, 0.64987756971621, 0.0197968749793939, 5.28672163823767e-10, 0.00471555351643001, 2.33367394341443e-13, 1.21630438148624e-64, 1, 1, 1), .Dim = c(3L, 7L), .Dimnames = list(NULL, c('time', 'n.risk', 'n.event', 'survival', 'std.err', 'lower 95% CI', 'upper 95% CI'))), c('', '', ''), c('time', 'n.risk', 'n.event', 'survival', 'std.err', 'lower 95% CI', 'upper 95% CI'), TRUE, FALSE, NULL); .Internal(prmatrix(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]]))");
    }

    @Test
    public void testprmatrix2() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(FALSE, .Dim = c(1L, 1L)), NULL, NULL, TRUE, FALSE, NULL); .Internal(prmatrix(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]]))");
    }

    @Test
    public void testprmatrix3() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(c(1, 2, 3, 4, 5, 8, 11, 12, 15, 17, 22, 23, 21, 19, 17, 16, 14, 12, 8, 6, 4, 3, 2, 1, 2, 2, 1, 2, 2, 4, 2, 2, 1, 1, 1, 1, 0.904761904761905, 0.80952380952381, 0.761904761904762, 0.666666666666667, 0.571428571428571, 0.380952380952381, 0.285714285714286, 0.19047619047619, 0.142857142857143, 0.0952380952380952, 0.0476190476190476, 0, 0.0640564484890047, 0.0856890867468988, 0.0929428640903365, 0.102868899974728, 0.107989849431208, 0.105971169574131, 0.0985807941917649, 0.0856890867468988, 0.0763603548321212, 0.0640564484890047, 0.0464714320451682, NaN, 0.670045882235034, 0.568905059924173, 0.519391415328429, 0.425350435565247, 0.337976953859493, 0.183066548820394, 0.116561326436765, 0.0594817013611753, 0.0356573551906667, 0.016259260212247, 0.00332446304253118, NA, 0.975294149038113, 0.923888828559295, 0.893257109782487, 0.82504400879734, 0.749240709943216, 0.577788677745831, 0.481819648009025, 0.37743489058515, 0.321161574680869, 0.261249981968687, 0.197044905698946, NA), .Dim = c(12L, 7L), .Dimnames = list(NULL, c('time', 'n.risk', 'n.event', 'survival', 'std.err', 'lower 95% CI', 'upper 95% CI'))), c('', '', '', '', '', '', '', '', '', '', '', ''), c('time', 'n.risk', 'n.event', 'survival', 'std.err', 'lower 95% CI', 'upper 95% CI'), TRUE, FALSE, NULL); .Internal(prmatrix(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]]))");
    }

    @Test
    public void testprmatrix4() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(c(952L, 3622L, 202L, 406L), .Dim = c(2L, 2L), .Dimnames = list(c('subcohort', 'cohort'), c('1', '2'))), c('subcohort', 'cohort'), c('1', '2'), FALSE, FALSE, NULL); .Internal(prmatrix(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]]))");
    }

    @Test
    public void testprmatrix5() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(c(-1.89646294299258, 1.16675019914746, -8.10054918052941, -5.02922966334328, -0.332284173685658, -0.370285767776029, -0.422218802914528, 0.27824687717147, NA, 0.150098588668891, 3.21153879975245, 0.000303372486059531, 0.00654384959664698, 0.717283460804982, 0.690536969224971, 0.655590578569862, 1.32081223535046, NA, 1.38078223740269, 0.565646487676971, 0.698724423746393, 0.770086232143856, 0.0568682431416458, 0.116409970657657, 0.0584328435912827, 0.0510212342180821, 0, -1.37346997348395, 2.06268442316178, -11.5933390979754, -6.53073571948212, -5.84305326362929, -3.1808767383421, -7.22571035337252, 5.45355049590036, NA, 0.17, 0.039, 0, 6.5e-11, 5.1e-09, 0.0015, 5e-13, 4.9e-08, NA), .Dim = c(9L, 5L), .Dimnames = list(c('toccfarm', 'toccoperatives', 'toccprofessional', 'toccsales', 'tocccraftsmen:education', 'toccfarm:education', 'toccoperatives:education', 'toccprofessional:education', 'toccsales:education'), c('coef', 'exp(coef)', 'se(coef)', 'z', 'p'))), c('toccfarm', 'toccoperatives', 'toccprofessional', 'toccsales', 'tocccraftsmen:education', 'toccfarm:education', 'toccoperatives:education', 'toccprofessional:education', 'toccsales:education'), c('coef', 'exp(coef)', 'se(coef)', 'z', 'p'), TRUE, FALSE, NULL); .Internal(prmatrix(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]]))");
    }

    @Test
    public void testprmatrix6() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(c('splines-package', 'as.polySpline', 'asVector', 'backSpline', 'bs', 'interpSpline', 'ns', 'periodicSpline', 'polySpline', 'predict.bs', 'predict.bSpline', 'predict.nbSpline', 'predict.npolySpline', 'predict.ns', 'predict.pbSpline', 'predict.ppolySpline', 'spline.des', 'splineDesign', 'splineKnots', 'splineOrder', 'splines', 'xyVector', 'splines-package', 'polySpline', 'asVector', 'backSpline', 'bs', 'interpSpline', 'ns', 'periodicSpline', 'polySpline', 'predict.bs', 'predict.bSpline', 'predict.bSpline', 'predict.bSpline', 'predict.bs', 'predict.bSpline', 'predict.bSpline', 'splineDesign', 'splineDesign', 'splineKnots', 'splineOrder', 'splines-package', 'xyVector'), .Dim = c(22L, 2L)), NULL, c('Column 1', 'Column 2', 'Column 3'), FALSE, TRUE, NULL); .Internal(prmatrix(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]]))");
    }

    @Test
    public void testprmatrix7() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(c(' 0.228763', '-0.000666', '', '0.08909', '0.00426', '', '0.08899', '0.00426', '', '6.59', '0.02', '6.02', '1.00', '1.00', '3.06', '0.01', '0.88', '0.12'), .Dim = c(3L, 6L), .Dimnames = list(c('male', 'tt(agechf), linear', 'tt(agechf), nonlin'), c('coef', 'se(coef)', 'se2', 'Chisq', 'DF', 'p'))), c('male', 'tt(agechf), linear', 'tt(agechf), nonlin'), c('coef', 'se(coef)', 'se2', 'Chisq', 'DF', 'p'), FALSE, FALSE, NULL); .Internal(prmatrix(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]]))");
    }

    @Test
    public void testprmatrix8() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(c(' 0.00561', '-1.65487', '', '0.012', '0.483', '', '0.00872', '0.38527', '', ' 0.22', '11.74', '20.33', ' 1.0', ' 1.0', '13.9', '0.64000', '0.00061', '0.12000'), .Dim = c(3L, 6L), .Dimnames = list(c('age', 'sex', 'frailty(id, dist = \\\'t\\\', c'), c('coef', 'se(coef)', 'se2', 'Chisq', 'DF', 'p'))), c('age', 'sex', 'frailty(id, dist = \\\'t\\\', c'), c('coef', 'se(coef)', 'se2', 'Chisq', 'DF', 'p'), FALSE, FALSE, NULL); .Internal(prmatrix(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]]))");
    }
}
