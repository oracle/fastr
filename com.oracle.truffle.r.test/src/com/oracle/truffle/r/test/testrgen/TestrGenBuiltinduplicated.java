/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 * 
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.testrgen;

import org.junit.*;

import com.oracle.truffle.r.test.*;

// Checkstyle: stop line length check
public class TestrGenBuiltinduplicated extends TestBase {

    @Test
    @Ignore
    public void testduplicated1() {
        assertEval("argv <- list(c(\'methods\', \'base\'), FALSE, FALSE, NA); .Internal(duplicated(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    @Ignore
    public void testduplicated2() {
        assertEval("argv <- list(list(\'!\', \'%%\', \'%*%\', \'%/%\', \'&\', \'*\', \'+\', \'-\', \'/\', \'Arith\', \'BunchKaufman\', \'Cholesky\', \'Compare\', \'Logic\', \'Math2\', \'Math\', \'Ops\', \'Schur\', \'Summary\', \'[\', \'[<-\', \'^\', \'all.equal\', \'all\', \'any\', \'as.array\', \'as.integer\', \'as.logical\', \'as.matrix\', \'as.numeric\', \'as.vector\', \'band\', \'cbind2\', \'chol2inv\', \'chol\', \'coerce\', \'coerce<-\', \'colMeans\', \'colSums\', \'cov2cor\', \'crossprod\', \'determinant\', \'diag\', \'diag<-\', \'diff\', \'dim\', \'dim<-\', \'dimnames\', \'dimnames<-\', \'drop\', \'expand\', \'expm\',     \'facmul\', \'forceSymmetric\', \'format\', \'head\', \'image\', \'initialize\', \'is.finite\', \'is.infinite\', \'is.na\', \'isDiagonal\', \'isSymmetric\', \'isTriangular\', \'kronecker\', \'length\', \'lu\', \'mean\', \'nnzero\', \'norm\', \'pack\', \'print\', \'prod\', \'qr.Q\', \'qr.R\', \'qr.coef\', \'qr.fitted\', \'qr.qty\', \'qr.qy\', \'qr.resid\', \'qr\', \'rbind2\', \'rcond\', \'rep\', \'rowMeans\', \'rowSums\', \'show\', \'skewpart\', \'solve\', \'sum\', \'summary\', \'symmpart\', \'t\', \'tail\', \'tcrossprod\', \'toeplitz\', \'tril\', \'triu\', \'unname\', \'unpack\', \'update\',     \'updown\', \'which\', \'writeMM\', \'zapsmall\', \'Ops\', \'[\', \'Math\'), FALSE, FALSE, NA); .Internal(duplicated(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    @Ignore
    public void testduplicated3() {
        assertEval("argv <- list(c(3L, 8L, 18L), FALSE, FALSE, NA); .Internal(duplicated(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    @Ignore
    public void testduplicated4() {
        assertEval("argv <- list(c(0, 0.700492869640978, NA), FALSE, FALSE, NA); .Internal(duplicated(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    @Ignore
    public void testduplicated5() {
        assertEval("argv <- list(1L, FALSE, TRUE, NA); .Internal(duplicated(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    @Ignore
    public void testduplicated6() {
        assertEval("argv <- list(character(0), FALSE, FALSE, NA); .Internal(duplicated(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    @Ignore
    public void testduplicated7() {
        assertEval("argv <- list(list(\'plot\', \'Ops\', \'[\', \'Math\'), FALSE, FALSE, NA); .Internal(duplicated(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    @Ignore
    public void testduplicated8() {
        assertEval("argv <- list(structure(\'lattice\', .Names = \'\'), FALSE, FALSE, NA); .Internal(duplicated(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    @Ignore
    public void testduplicated10() {
        assertEval("argv <- list(structure(list(c0 = structure(integer(0), .Label = character(0), class = \'factor\')), .Names = \'c0\', row.names = character(0), class = structure(\'integer(0)\', .Names = \'c0\')), structure(list(c0 = structure(integer(0), .Label = character(0), class = \'factor\')), .Names = \'c0\', row.names = character(0), class = structure(\'integer(0)\', .Names = \'c0\')), FALSE, NA); .Internal(duplicated(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    @Ignore
    public void testduplicated11() {
        assertEval("argv <- list(c(\'\\\\title\', \'\\\\name\', \'\\\\alias\', \'\\\\alias\', \'\\\\keyword\', \'\\\\keyword\', \'\\\\description\', \'\\\\usage\', \'\\\\arguments\', \'\\\\details\', \'\\\\value\', \'\\\\section\', \'\\\\section\', \'\\\\seealso\', \'\\\\examples\'), FALSE, FALSE, NA); .Internal(duplicated(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    @Ignore
    public void testduplicated12() {
        assertEval("argv <- list(structure(c(-0.838428742794102, 0.838428742794102, 0.838428742794102, 0.838428742794102, -0.838428742794102, -0.838428742794102), .Dim = c(6L, 1L), .Dimnames = list(c(\'1\', \'3\', \'4\', \'5\', \'6\', \'7\'), NULL)), FALSE, FALSE, NA); .Internal(duplicated(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    @Ignore
    public void testduplicated13() {
        assertEval("argv <- list(c(-1628571, -1628571, -1200000, -1200000, -1057143, -914286, -771429, -771429, -771429, -628571, -628571, -485714, -485714, -485714, -485714, -342857, -342857, -342857, -342857, -2e+05, -2e+05, -2e+05, -2e+05, -57143, -57143, -57143, 85714, 85714, 228571, 228571, 228571, 371429, 371429, 371429, 371429, 514286, 514286, 514286, 657143, 657143, 657143, 657143, 657143, 942857, 1085714, 1228571, 1228571, 1228571, 1228571, 1371429), FALSE, FALSE, NA); .Internal(duplicated(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    @Ignore
    public void testduplicated14() {
        assertEval("argv <- list(c(1, 0.778249191273129, 0.65570344192776, 0.65570344192776, 0.105668080308148, 0.0451091129154675, 0.0451091129154675, 1.49604383156071e-06, 8.3976239365668e-11, 2.13195391672632e-15, 1.4298180954663e-20, 1.47541167362595e-26, 1.09353648287987e-33, 1.6858825926109e-42, 1.6858825926109e-42, 1.6858825926109e-42), FALSE, FALSE, NA); .Internal(duplicated(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }
}

