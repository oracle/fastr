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
public class TestBuiltin_identical extends TestBase {

    @Test
    public void testidentical1() {
        assertEval("argv <- list('oats[-1, ]', 'newdata', TRUE, TRUE, TRUE, TRUE, FALSE); .Internal(identical(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testidentical2() {
        assertEval("argv <- list(structure(c(NA, 2, NA, 1, NA, 0), .Dim = 2:3), structure(c(NA, 2, NA, 1, NA, 0), .Dim = 2:3), TRUE, TRUE, TRUE, TRUE, FALSE); .Internal(identical(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testidentical3() {
        assertEval("argv <- list(structure(c('dgTMatrix', 'matrix.coo'), .Names = c('from', 'to'), package = c('Matrix', ''), class = structure('signature', package = 'methods')), structure(c('dgTMatrix', 'matrix.coo'), .Names = c('from', 'to'), package = c('Matrix', ''), class = structure('signature', package = 'methods')), TRUE, TRUE, TRUE, TRUE, FALSE); .Internal(identical(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testidentical4() {
        assertEval("argv <- list(structure(3.14159265358979, comment = 'Start with pi', class = structure('num1', package = '.GlobalEnv')), structure(3.14159265358979, comment = 'Start with pi', class = structure('num1', package = '.GlobalEnv')), TRUE, TRUE, TRUE, TRUE, FALSE); .Internal(identical(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testidentical5() {
        assertEval("argv <- list(structure(list(a = 1), .Dim = 1L, .Dimnames = list('a')), structure(list(a = 1), .Dim = 1L, .Dimnames = list('a')), TRUE, TRUE, TRUE, TRUE, FALSE); .Internal(identical(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testidentical6() {
        assertEval("argv <- list(structure(list(), .Names = character(0), row.names = integer(0), class = 'data.frame'), structure(list(), .Names = character(0), row.names = integer(0), class = 'data.frame'), TRUE, TRUE, TRUE, TRUE, FALSE); .Internal(identical(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testidentical7() {
        assertEval("argv <- list(c(-1.38507061859438+0.31833672642477i, 0.0383231810219-1.42379885362755i, -0.763030162361974-0.405090858049187i, 0.212306135525839+0.995386565684023i, 1.42553796686779+0.95881778764026i, 0.744479822333976+0.918087896319951i, 0.70022940298623-0.15096960188161i, -0.22935461345173-1.2230687888662i, 0.197093861895352-0.868824288637794i, 1.20715377387226-1.04248536490429i), c(-1.38507061859438+0.31833672642477i, 0.0383231810219-1.42379885362755i, -0.763030162361974-0.405090858049187i, 0.212306135525839+0.995386565684023i, 1.42553796686779+0.95881778764026i, 0.744479822333976+0.918087896319951i, 0.70022940298623-0.15096960188161i, -0.22935461345173-1.2230687888662i, 0.197093861895352-0.868824288637794i, 1.20715377387226-1.04248536490429i), TRUE, TRUE, TRUE, TRUE, FALSE); .Internal(identical(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testidentical8() {
        assertEval("argv <- list(structure(list(a = 1), .Names = 'a', .Tsp = c(1, 1, 1), class = 'ts'), structure(list(a = 1), .Names = 'a', .Tsp = c(1, 1, 1), class = 'ts'), TRUE, TRUE, TRUE, TRUE, FALSE); .Internal(identical(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testidentical9() {
        assertEval("argv <- list(c(TRUE, TRUE, FALSE, TRUE, FALSE, TRUE, TRUE, TRUE, FALSE, TRUE, TRUE, TRUE, TRUE, TRUE, FALSE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, FALSE, TRUE, TRUE, FALSE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, FALSE, FALSE, TRUE, TRUE, TRUE, TRUE, TRUE, FALSE, TRUE, FALSE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, FALSE, TRUE, TRUE, FALSE, TRUE, TRUE, TRUE, FALSE, FALSE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, FALSE, FALSE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, FALSE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, FALSE, TRUE, TRUE, TRUE, FALSE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, FALSE, TRUE, TRUE, FALSE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, FALSE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, FALSE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, FALSE, TRUE, FALSE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, FALSE, FALSE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, FALSE, TRUE, TRUE, FALSE, TRUE, FALSE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, FALSE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, FALSE, TRUE, TRUE, TRUE, TRUE), c(TRUE, TRUE, FALSE, TRUE, FALSE, TRUE, TRUE, TRUE, FALSE, TRUE, TRUE, TRUE, TRUE, TRUE, FALSE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, FALSE, TRUE, TRUE, FALSE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, FALSE, FALSE, TRUE, TRUE, TRUE, TRUE, TRUE, FALSE, TRUE, FALSE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, FALSE, TRUE, TRUE, FALSE, TRUE, TRUE, TRUE, FALSE, FALSE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, FALSE, FALSE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, FALSE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, FALSE, TRUE, TRUE, TRUE, FALSE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, FALSE, TRUE, TRUE, FALSE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, FALSE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, FALSE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, FALSE, TRUE, FALSE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, FALSE, FALSE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, FALSE, TRUE, TRUE, FALSE, TRUE, FALSE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, FALSE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, FALSE, TRUE, TRUE, TRUE, TRUE), TRUE, TRUE, TRUE, TRUE, FALSE); .Internal(identical(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testidentical10() {
        assertEval("argv <- list(complex(0), complex(0), TRUE, TRUE, TRUE, TRUE, FALSE); .Internal(identical(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testidentical11() {
        assertEval("argv <- list(NULL, '\\\\link', TRUE, TRUE, TRUE, TRUE, FALSE); .Internal(identical(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testidentical12() {
        assertEval("argv <- list(c(TRUE, TRUE, NA), c(TRUE, TRUE, NA), TRUE, TRUE, TRUE, TRUE, FALSE); .Internal(identical(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testidentical13() {
        assertEval("argv <- list(NA_complex_, NA_complex_, TRUE, TRUE, TRUE, TRUE, FALSE); .Internal(identical(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testidentical14() {
        assertEval("argv <- list(structure(c(1, 1, 1, 1, 2, 3), .Dim = c(3L, 2L), .Dimnames = list(NULL, c('I', 'a')), foo = 'bar', class = 'matrix'), structure(c(1, 1, 1, 1, 2, 3), .Dim = c(3L, 2L), class = 'matrix', foo = 'bar', .Dimnames = list(NULL, c('I', 'a'))), TRUE, TRUE, FALSE, TRUE, FALSE); .Internal(identical(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testidentical15() {
        assertEval("argv <- list(structure(list(x = 1L), .Names = 'x', row.names = c(NA, -1L), class = 'data.frame'), structure(list(x = 1L), .Names = 'x', row.names = c(NA, -1L), class = 'data.frame'), TRUE, TRUE, TRUE, TRUE, FALSE); .Internal(identical(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testidentical16() {
        assertEval("argv <- list(structure(c(1L, 1L, 1L), .Label = c('1', '2'), class = 'factor'), structure(list(f = structure(c(1L, 1L, 1L), .Label = c('1', '2'), class = 'factor'), u = structure(12:14, unit = 'kg', class = 'avector')), .Names = c('f', 'u'), row.names = 2:4, class = 'data.frame'), TRUE, TRUE, TRUE, TRUE, FALSE); .Internal(identical(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testidentical17() {
        assertEval("argv <- list(raw(0), raw(0), TRUE, TRUE, TRUE, TRUE, FALSE); .Internal(identical(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testidentical18() {
        assertEval("argv <- list(c(1, 1, 0.5, 1, 1, 1, 1, 1, 0, 0, 0.5, 1, 0, 1, 0, 1, 0.5, 1, NA, 0.75, 0.5, 0, 0.5, 0.5, 0.666666666666667, 0.666666666666667, 1, 1, 0.666666666666667, 1, 0.666666666666667, 0.666666666666667, 0.333333333333333, 0.5, 1, 0, 1, 0.5, 1, 1, 1, 0, 1, 0.5, 1, 1, 0.5, 1, 1, 1, 0.5, 1, 1, NA, 0.5), c(1, 1, 0.5, 1, 1, 1, 1, 1, 0, 0, 0.5, 1, 0, 1, 0, 1, 0.5, 1, NA, 0.75, 0.5, 0, 0.5, 0.5, 0.666666666666667, 0.666666666666667, 1, 1, 0.666666666666667, 1, 0.666666666666667, 0.666666666666667, 0.333333333333333, 0.5, 1, 0, 1, 0.5, 1, 1, 1, 0, 1, 0.5, 1, 1, 0.5, 1, 1, 1, 0.5, 1, 1, NA, 0.5), TRUE, TRUE, TRUE, TRUE, FALSE); .Internal(identical(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testidentical19() {
        assertEval("argv <- list(c(0.0804034870161223, 10.3548347412639), structure(list(loc = c(0.0804034870161223, 10.3548347412639), cov = structure(c(3.01119301965569, 6.14320559215603, 6.14320559215603, 14.7924762275451), .Dim = c(2L, 2L)), d2 = 2, wt = c(0, 0, 0, 0, 0, 1.75368801162502e-134, 0, 0, 0, 2.60477585273833e-251, 1.16485035372295e-260, 0, 1.53160350210786e-322, 0.333331382328728, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3.44161262707711e-123, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1.968811545398e-173, 0, 8.2359965384697e-150, 0, 0, 0, 0, 6.51733217171341e-10, 0, 2.36840184577368e-67, 0, 9.4348408357524e-307, 0, 1.59959906013771e-89, 0, 8.73836857865034e-286, 7.09716190970992e-54, 0, 0, 0, 1.530425353017e-274, 8.57590058044551e-14, 0.333333106397154, 0, 0, 1.36895217898448e-199, 2.0226102635783e-177, 5.50445388209462e-42, 0, 0, 0, 0, 1.07846402051283e-44, 1.88605464411243e-186, 1.09156111051203e-26, 0, 3.0702877273237e-124, 0.333333209689785, 0, 0, 0, 0, 0, 0, 3.09816093866831e-94, 0, 0, 4.7522727332095e-272, 0, 0, 2.30093251441394e-06, 0, 0, 1.27082826644707e-274, 0, 0, 0, 0, 0, 0, 0, 4.5662025456054e-65, 0, 2.77995853978268e-149, 0, 0, 0), sqdist = c(0.439364946869246, 0.0143172566761092, 0.783644692619938, 0.766252947443554, 0.346865912102713, 1.41583192825661, 0.168485512965902, 0.354299830956879, 0.0943280426627965, 1.05001058449122, 1.02875556201707, 0.229332323173361, 0.873263925064789, 2.00000009960498, 0.449304354954282, 0.155023307933165, 0.118273979375253, 0.361693898800799, 0.21462398586105, 0.155558909016629, 0.471723661454506, 0.719528696331092, 0.0738164380664225, 1.46001193111051, 0.140785322548143, 0.127761195166703, 0.048012401156175, 0.811750426884519, 0.425827709817574, 0.163016638545231, 0.557810866640707, 0.277350147637843, 0.0781399119055092, 1.29559183995835, 0.718376405567138, 1.37650242941478, 0.175087780508154, 0.233808973148729, 0.693473805463067, 0.189096604125073, 1.96893781800017, 0.4759756980592, 1.69665760380474, 0.277965749373647, 0.920525436884815, 0.57525234053591, 1.59389578665009, 0.175715364671313, 0.972045794851437, 1.75514684962809, 0.0597413185507202, 0.174340343040626, 0.143421553552865, 0.997322770596838, 1.94096736957465, 2.00000001159796, 0.367000821772989, 0.682474530588235, 1.20976163307984, 1.27031685239035, 1.79775635513363, 0.0857761902860323, 0.435578932929501, 0.214370604878221, 0.494714247412686, 1.78784623754399, 1.24216674083069, 1.87749485326709, 0.0533296334123023, 1.45588362584438, 2.00000000631459, 0.208857144738039, 0.119251291573058, 0.365303924649962, 0.690656674239668, 0.0396958405786268, 0.258262120876164, 1.57360254057537, 0.307548421049514, 0.628417063100241, 1.00647098749202, 0.297624360530352, 0.400289147351669, 1.98298426250944, 0.129127182829694, 0.0794695319493149, 0.991481735944321, 0.444068154119836, 0.206790162395106, 0.574310829851377, 0.181887577583334, 0.433872021297517, 0.802994892604009, 0.293053770941001, 1.7002969001965, 0.77984639982848, 1.36127407487932, 0.761935213110323, 0.597915313430067, 0.237134831067472), prob = NULL, tol = 1e-07, eps = 9.96049758228423e-08,     it = 898L, maxit = 5000, ierr = 0L, conv = TRUE), .Names = c('loc', 'cov', 'd2', 'wt', 'sqdist', 'prob', 'tol', 'eps', 'it', 'maxit', 'ierr', 'conv'), class = 'ellipsoid'), TRUE, TRUE, TRUE, TRUE, FALSE); .Internal(identical(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testidentical20() {
        assertEval(Ignored.Unknown, "argv <- list(NaN, NaN, TRUE, TRUE, TRUE, TRUE, FALSE); .Internal(identical(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testidentical21() {
        assertEval("argv <- list(c('«', '»', '¿', '?'), 'TeX', TRUE, TRUE, TRUE, TRUE, FALSE); .Internal(identical(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testidentical22() {
        assertEval("argv <- list(structure(3.14159265358979, class = structure('3.14159265358979', class = 'testit')), 42, TRUE, TRUE, TRUE, TRUE, FALSE); .Internal(identical(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testidentical23() {
        assertEval("argv <- list(c(3L, 3L, NA, 3L), c(3L, 3L, NA, 3L), TRUE, TRUE, TRUE, TRUE, FALSE); .Internal(identical(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testidentical24() {
        assertEval("argv <- list(list(c('r1', 'r3', 'r4', 'r5', 'r6', 'r7', 'r8', 'r9', 'r10', 'r11', 'r12', 'r13', 'r14', 'r15', 'r16', 'r17', 'r18', 'r19', 'r20', 'r21', 'r22', 'r23', 'r24', 'r25', 'r26', 'r27', 'r28', 'r29', 'r30', 'r31', 'r32', 'r33', 'r34', 'r35', 'r36', 'r37', 'r38', 'r39', 'r40'), c('c1', 'c2', 'c3', 'c4', 'c5', 'c6', 'c7', 'c8', 'c9', 'c10', 'c11', 'c12', 'c13', 'c14', 'c15', 'c16', 'c17', 'c18', 'c19', 'c20')), list(character(0), character(0)), TRUE, TRUE, TRUE, TRUE, FALSE); .Internal(identical(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testidentical25() {
        assertEval("argv <- list(c('object', NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA), c('object', NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA), TRUE, TRUE, TRUE, TRUE, FALSE); .Internal(identical(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testidentical26() {
        assertEval("argv <- list(3.04888344611714e+29, 3.04888344611714e+29, TRUE, TRUE, TRUE, TRUE, FALSE); .Internal(identical(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testidentical27() {
        assertEval("argv <- list(structure('BunchKaufman', class = structure('signature', package = 'methods'), .Names = 'x', package = 'methods'), structure('Matrix', .Names = 'x', package = 'Matrix', class = structure('signature', package = 'methods')), TRUE, TRUE, TRUE, TRUE, FALSE); .Internal(identical(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testidentical28() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(list(x = c(1, 1, 1, 1, 1, 1, 1, 1, 1, 1), y = c(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), fac = structure(c(1L, 3L, 2L, 3L, 3L, 1L, 2L, 3L, 2L, 2L), .Label = c('A', 'B', 'C'), class = 'factor')), .Names = c('x', 'y', 'fac'), row.names = c(NA, -10L), class = 'data.frame'), structure(list(x = c(1, 1, 1, 1, 1, 1, 1, 1, 1, 1), y = c(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), fac = structure(c(1L, 3L, 2L, 3L, 3L, 1L, 2L, 3L, 2L, 2L), .Label = c('A', 'B', 'C'), class = 'factor')), .Names = c('x', 'y', 'fac'), row.names = c(NA, 10L), class = 'data.frame'), TRUE, TRUE, TRUE, TRUE, FALSE); .Internal(identical(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testidentical29() {
        assertEval("argv <- list(c('1', '2', NA), c('1', '2', NA), TRUE, TRUE, TRUE, TRUE, FALSE); .Internal(identical(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testidentical30() {
        assertEval("argv <- list(c(-9.42477796076938, -6.28318530717959, -3.14159265358979, 0, 3.14159265358979, 6.28318530717959, 9.42477796076938, 12.5663706143592, 15.707963267949, 18.8495559215388), c(-9.42477796076938, -6.28318530717959, -3.14159265358979, 0, 3.14159265358979, 6.28318530717959, 9.42477796076938, 12.5663706143592, 15.707963267949, 18.8495559215388), TRUE, TRUE, TRUE, TRUE, FALSE); .Internal(identical(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testidentical31() {
        assertEval("argv <- list(structure('classGeneratorFunction', package = 'methods'), structure('classGeneratorFunction', package = 'methods'), TRUE, TRUE, TRUE, TRUE, FALSE); .Internal(identical(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testidentical32() {
        assertEval("argv <- list(structure(function (x) standardGeneric('cosh', .Primitive('cosh')), generic = structure('cosh', package = 'base'), package = 'base', group = list('Math'), valueClass = character(0), signature = 'x', default = .Primitive('cosh'), skeleton = quote(.Primitive('cosh')(x)), class = structure('standardGeneric', package = 'methods')), FALSE, TRUE, TRUE, TRUE, TRUE, FALSE); .Internal(identical(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testidentical33() {
        assertEval("argv <- list(structure(1L, match.length = 8L, useBytes = TRUE), structure(1L, match.length = 8L, useBytes = TRUE), TRUE, TRUE, TRUE, TRUE, FALSE); .Internal(identical(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testidentical35() {
        assertEval("argv <- structure(list(x = expression(exp(-0.5 * u^2)), y = expression(exp(-0.5 *     u^2))), .Names = c('x', 'y'));do.call('identical', argv)");
    }

    @Test
    public void testidentical36() {
        assertEval("argv <- structure(list(x = structure(list(X1.4 = 1:4), .Names = 'X1.4',     row.names = c(NA, -4L), class = 'data.frame'), y = structure(list(X1.4 = 1:4),     .Names = 'X1.4', row.names = c('1', '2', '3', '4'), class = 'data.frame')),     .Names = c('x', 'y'));" +
                        "do.call('identical', argv)");
    }

    @Test
    public void testidentical37() {
        assertEval(Ignored.Unknown,
                        "argv <- structure(list(x = structure(list(a = NA, b = NA_integer_,     c = NA_real_, d = NA_complex_, e = 1, f = 1L, g = 1:3, h = c(NA,         1L, 2L, 3L), i = NA_character_, j = c('foo', NA, 'bar')),     .Names = c('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j')),     y = structure(list(a = NA, b = NA_integer_, c = NA_real_,         d = NA_complex_, e = 1, f = 1L, g = 1:3, h = c(NA, 1L,             2L, 3L), i = NA_character_, j = c('foo', NA, 'bar')),         .Names = c('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i',             'j'))), .Names = c('x', 'y'));" +
                                        "do.call('identical', argv)");
    }

    @Test
    public void testAttrOrder() {
        assertEval(Ignored.ImplementationError, "x <- 1; y <- 1; attr(x, \"f\") <- 2; attr(x, \"g\") <- 1; attr(y, \"g\") <- 1; attr(y, \"f\") <- 2; identical(x, y)");
    }

    @Test
    public void testIdentical() {
        assertEval("{ identical(1,1) }");
        assertEval("{ identical(1L,1) }");
        assertEval("{ identical(1:3, c(1L,2L,3L)) }");
        assertEval("{ x <- 1 ; attr(x, \"my\") <- 10; y <- 1 ; attr(y, \"my\") <- 10 ; identical(x,y) }");
        assertEval("{ x <- 1 ; attr(x, \"hello\") <- 2 ; attr(x, \"my\") <- 10;  attr(x, \"hello\") <- NULL ; y <- 1 ; attr(y, \"my\") <- 10 ; identical(x,y) }");
        assertEval("{ identical(0/0,1[2]) }");

        assertEval("{ identical(list(1, list(2)), list(list(1), 1)) }");
        assertEval("{ identical(list(1, list(2)), list(1, list(2))) }");
        assertEval("{ x <- 1 ; attr(x, \"my\") <- 10; identical(x, 1) }");
        assertEval("{ x <- 1 ; attr(x, \"my\") <- 10; y <- 1 ; attr(y, \"my\") <- 11 ; identical(x,y) }");

        assertEval("{ identical(quote(if(x) 42), quote(if(x) 7)) }");
        assertEval("{ identical(quote(if(x) 42), quote(if(x) 42)) }");

        assertEval("{ setClass(\"foo\", representation(j=\"numeric\")); x<-new(\"foo\", j=42); y<-new(\"foo\", j=42); identical(x,y) }");
        assertEval("{ setClass(\"foo\", representation(j=\"numeric\")); x<-new(\"foo\", j=42); y<-new(\"foo\", j=7); identical(x,y) }");

        assertEval("{ x<-list(7); y<-list(7); identical(x,y) }");
        assertEval("{ x<-list(7); y<-list(42); identical(x,y) }");
        assertEval("{ x<-list(list(7)); y<-list(list(7)); identical(x,y) }");
        assertEval("{ x<-list(list(7)); y<-list(list(42)); identical(x,y) }");

        assertEval("{ f1<-function(x=1) 42; f2<-function(x=2) 7; identical(formals(f1), formals(f2)) }");
        assertEval("{ f1<-function(x=1) 42; f2<-function(x=1) 7; identical(formals(f1), formals(f2)) }");
        assertEval("{ x<-42; attr(x, \"foo\")<-\"foo\"; y<-42; attr(y, \"foo\")<-\"foo\"; identical(x, y) }");
        assertEval("{ x<-42; attr(x, \"foo\")<-\"foo\"; y<-42; attr(y, \"bar\")<-\"foo\"; identical(x, y) }");
        assertEval("{ x<-42; attr(x, \"foo\")<-\"foo\"; y<-42; attr(y, \"foo\")<-\"bar\"; identical(x, y) }");
        assertEval("{ x<-42; attr(x, \"foo\")<-\"foo\"; y<-42; attr(y, \"bar\")<-\"bar\"; identical(x, y) }");
        assertEval("{ x<-42; attr(x, \"foo\")<-\"foo\"; y<-42; identical(x, y) }");
        assertEval("{ x<-list(42); attr(x, \"foo\")<-\"foo\"; y<-list(42); attr(y, \"foo\")<-\"foo\"; identical(x, y) }");
        assertEval("{ x<-list(42); attr(x, \"foo\")<-\"foo\"; y<-list(42); attr(y, \"bar\")<-\"foo\"; identical(x, y) }");
        assertEval("{ x<-list(42); attr(x, \"foo\")<-\"foo\"; y<-list(42); attr(y, \"foo\")<-\"bar\"; identical(x, y) }");
        assertEval("{ x<-list(42); attr(x, \"foo\")<-\"foo\"; y<-list(42); attr(y, \"bar\")<-\"bar\"; identical(x, y) }");
        assertEval("{ x<-list(42); attr(x, \"foo\")<-\"foo\"; y<-list(42); identical(x, y) }");
        assertEval("{ x<-quote(f()); attr(x, \"foo\")<-\"foo\"; y<-quote(f()); attr(y, \"foo\")<-\"foo\"; identical(x, y) }");
        assertEval("{ x<-quote(f()); attr(x, \"foo\")<-\"foo\"; y<-quote(f()); attr(y, \"bar\")<-\"foo\"; identical(x, y) }");
        assertEval("{ x<-quote(f()); attr(x, \"foo\")<-\"foo\"; y<-quote(f()); attr(y, \"foo\")<-\"bar\"; identical(x, y) }");
        assertEval("{ x<-quote(f()); attr(x, \"foo\")<-\"foo\"; y<-quote(f()); attr(y, \"bar\")<-\"bar\"; identical(x, y) }");
        assertEval("{ x<-quote(f()); attr(x, \"foo\")<-\"foo\"; y<-quote(f()); identical(x, y) }");
        assertEval("{ setClass(\"c\", representation(d=\"numeric\")); x<-new(\"c\", d=42); attr(x, \"foo\")<-\"foo\"; y<-new(\"c\", d=42); attr(y, \"foo\")<-\"foo\"; identical(x, y) }");
        assertEval("{ setClass(\"c\", representation(d=\"numeric\")); x<-new(\"c\", d=42); attr(x, \"foo\")<-\"foo\"; y<-new(\"c\", d=42); attr(y, \"bar\")<-\"foo\"; identical(x, y) }");
        assertEval("{ setClass(\"c\", representation(d=\"numeric\")); x<-new(\"c\", d=42); attr(x, \"foo\")<-\"foo\"; y<-new(\"c\", d=42); attr(y, \"foo\")<-\"bar\"; identical(x, y) }");
        assertEval("{ setClass(\"c\", representation(d=\"numeric\")); x<-new(\"c\", d=42); attr(x, \"foo\")<-\"foo\"; y<-new(\"c\", d=42); attr(y, \"bar\")<-\"bar\"; identical(x, y) }");
        assertEval("{ setClass(\"c\", representation(d=\"numeric\")); x<-new(\"c\", d=42); attr(x, \"foo\")<-\"foo\"; y<-new(\"c\", d=42); identical(x, y) }");
        assertEval("{ x<-expression(1 + 0:9); y<-expression(1 + 0:9); identical(x, y) }");

        // functions

        // GnuR adds a srcref attribute, FastR does not, so we really can't do any comparative
        // tests.
        assertEval(Ignored.ImplementationError, "{ f1 <- function() {}; f2 <- function() {}; identical(f1, f2) }");
        assertEval(Ignored.ImplementationError, "{ identical(function() 42, function() 42) }");

    }

    @Test
    public void testDoubles() {
        assertEval(template("identical(%0, num.eq=%1, single.NA=%2)", new String[][]{
                        new String[]{"NA, NA", "NaN, NaN", "0/0, NaN", "0/-1, NaN"},
                        new String[]{"T", "F"},
                        new String[]{"T", "F"}
        }));
    }
}
