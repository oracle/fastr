/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2012-2014, Purdue University
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.*;

import com.oracle.truffle.r.test.*;

// Checkstyle: stop line length check
public class TestBuiltin_diag extends TestBase {

    @Test
    public void testdiag1() {
        assertEval(Ignored.Unknown, "argv <- list(structure(c(1L, 2L, 3L, 4L, 1L), .Dim = 5L), 5L, 5L); .Internal(diag(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testdiag2() {
        assertEval(Ignored.Unknown, "argv <- list(NULL, 0L, 0L); .Internal(diag(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testdiag3() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(c(0.00258017518312032, 0.00371592854270272, 4.74358130918145e-05, 0.00490111130607204, 0.000101990092933588, 0.00674107947251412, 0.000239828967315095, 0.00980847069198632, 0.000617541923597065, 0.0155189333862593, 0.00178497855501229, 0.0281274123275302, 0.00633033372222146, 0.0642581517771313, 0.0351608933185668, 0.151097171670205, 0.967636582993474, 0.0809667077153405), .Names = c('Xr1', 'Xr2', 'Xr3', 'Xr4', 'Xr5', 'Xr6', 'Xr7', 'Xr8', 'Xr9', 'Xr10', 'Xr11', 'Xr12', 'Xr13', 'Xr14', 'Xr15', 'Xr16', 'Xr17', 'Xr18')), 18L, 18L); .Internal(diag(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testdiag4() {
        assertEval(Ignored.Unknown, "argv <- list(c(FALSE, TRUE, TRUE, TRUE), 4L, 4L); .Internal(diag(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testdiag5() {
        assertEval(Ignored.Unknown,
                        "argv <- list(c(-2.80063713410797-0i, 2.40432724210166-0i, -1.40502612938985+0i, 1.39344241164891+0i, 0.785422253492721+0i), 5L, 5L); .Internal(diag(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testdiag6() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(c(0.662193594830517, 0.883082096514931, 0.80211645621425, 0.806993241239092, 0.593615611337433, 0.55837479933202, 0.531727869384763, 0.696607367099979, 0.506321785494117, 0.489681023915914, 0.742249020738322, 0.65965217395585, 0.700437655250271, 0.80388520340336, 0.834325796707322, 0.741083805719802, 0.77320911661894, 0.76968452857621, 0.872531808824412, 0.769100148773066, 0.763385216756006, 0.775173380089108, 0.705125971098107, 0.706916424657676), .Names = c('VisualPerception', 'Cubes', 'PaperFormBoard', 'Flags', 'GeneralInformation', 'PargraphComprehension', 'SentenceCompletion', 'WordClassification', 'WordMeaning', 'Addition', 'Code', 'CountingDots', 'StraightCurvedCapitals', 'WordRecognition', 'NumberRecognition', 'FigureRecognition', 'ObjectNumber', 'NumberFigure', 'FigureWord', 'Deduction', 'NumericalPuzzles', 'ProblemReasoning', 'SeriesCompletion', 'ArithmeticProblems')), 24L, 24L); .Internal(diag(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testdiag7() {
        assertEval(Ignored.Unknown, "argv <- list(1, 0L, 0L); .Internal(diag(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testdiag8() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(c(0.553622032575332, 1.83583330034692, 0.540309168173204, 0.347171956892285), .Names = c('A', 'B', 'C', 'D')), 4L, 4L); .Internal(diag(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testdiag9() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(c(-875.251472917967, 12.8319913648351, -28.2155558559225, -27.6015982680416, -70.4377976184188, -98.9293825275015, 32.8291346503008, -20.6544753576079, 26.3486263439148, -42.5376299218819, -131.164911564755, -12.7775395276621, 3.34207338870892, -6.39516049903921, 5.97199502480298, 9.16451921253422, 4.70193189358059), .Names = c('(Intercept)', 'BII', 'BIII', 'BIV', 'BV', 'BVI', 'VMarvellous', 'VVictory', 'N0.2cwt', 'N0.4cwt', 'N0.6cwt', 'VMarvellous:N0.2cwt', 'VVictory:N0.2cwt', 'VMarvellous:N0.4cwt', 'VVictory:N0.4cwt', 'VMarvellous:N0.6cwt', 'VVictory:N0.6cwt')), 71L, 17L); .Internal(diag(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testdiag10() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(c(-268.831499270454, 5.6415423423032, 14.3443760756611, -6.07661158322081, -7.61383061715105, 3.28804653251744, 13.7579673886322, 2.89856286229343, -9.75713414208632, 4.61320568224165), .Names = c('(Intercept)', 'block2', 'block3', 'block4', 'block5', 'block6', 'N1', 'P1', 'K1', 'N1:P1')), 24L, 10L); .Internal(diag(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testdiag11() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(c(0.00284900284900285, 0.00284900284900285, 0.00284900284900285, 0.00284900284900285, 0.00284900284900285, 0.00284900284900285, 0.00284900284900285, 0.00284900284900285, 0.00284900284900285, 0.00284900284900285), .Dim = 10L, .Dimnames = list(c('1', '2', '3', '4', '5', '6', '7', '8', '9', '10'))), 10L, 10L); .Internal(diag(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testdiag12() {
        assertEval(Ignored.Unknown, "argv <- list(list(1, 1, 1), 3L, 3L); .Internal(diag(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testdiag13() {
        assertEval(Ignored.Unknown, "argv <- list(list(), 0L, 0L); .Internal(diag(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testdiag14() {
        assertEval(Ignored.Unknown, "argv <- list(character(0), 0L, 0L); .Internal(diag(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testdiag16() {
        assertEval(Ignored.Unknown,
                        "argv <- structure(list(x = structure(c(25707905.8534307, -1396341.94003231,     107590.673887047, 1282255.68405509, 990152.618275206, -1396341.94003231,     23207928.6679172, -602948.854263649, -750498.277752946, -97557.914173682,     107590.673887047, -602948.854263649, 25224155.0868383, -1446668.75346658,     3085225.85187065, 1282255.68405509, -750498.277752946, -1446668.75346658,     22221045.9258222, -1069907.07413189, 990152.618275206, -97557.914173682,     3085225.85187065, -1069907.07413189, 27302989.4318488), .Dim = c(5L,     5L))), .Names = 'x');" +
                                        "do.call('diag', argv)");
    }

    @Test
    public void testDiagonal() {
        assertEval("{ m <- matrix(1:6, nrow=3) ; diag(m) }");
        assertEval("{ m <- matrix(1:6, nrow=2) ; diag(m) }");
        assertEval("{ m <- matrix(1:9, nrow=3) ; diag(m) }");

        assertEval("{ diag(1, 7) }");
        assertEval("{ diag(1, 7, 2) }");
        assertEval("{ diag(1, 2, 7) }");
    }
}
