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
public class TestBuiltin_grep extends TestBase {

    @Test
    public void testgrep1() {
        assertEval("argv <- list('|', 'wool', FALSE, FALSE, FALSE, TRUE, FALSE, FALSE); .Internal(grep(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]]))");
    }

    @Test
    public void testgrep2() {
        assertEval("argv <- list('éè', '«Latin-1 accented chars»: éè øØ å<Å æ<Æ é éè', TRUE, FALSE, TRUE, FALSE, FALSE, FALSE); .Internal(grep(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]]))");
    }

    @Test
    public void testgrep3() {
        assertEval("argv <- list('[', '^\\\\.__[MT]', FALSE, FALSE, FALSE, TRUE, FALSE, FALSE); .Internal(grep(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]]))");
    }

    @Test
    public void testgrep4() {
        assertEval("argv <- list('éè', '«Latin-1 accented chars»: éè øØ å<Å æ<Æ é éè', FALSE, FALSE, FALSE, TRUE, FALSE, FALSE); .Internal(grep(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]]))");
    }

    @Test
    public void testgrep5() {
        assertEval("argv <- list('[', '^[[:alpha:]]+', FALSE, FALSE, FALSE, TRUE, FALSE, FALSE); .Internal(grep(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]]))");
    }

    @Test
    public void testgrep6() {
        assertEval("argv <- list('.__T__[:', c('.__T__plot:graphics', '.__T__$:base', '.__T__$<-:base', '.__T__Arith:base', '.__T__Compare:methods', '.__T__Complex:base', '.__T__Logic:base', '.__T__Math2:methods', '.__T__Math:base', '.__T__Ops:base', '.__T__Summary:base', '.__T__[:base', '.__T__addNextMethod:methods', '.__T__body<-:base', '.__T__cbind2:methods', '.__T__coerce:methods', '.__T__coerce<-:methods', '.__T__initialize:methods', '.__T__kronecker:base', '.__T__loadMethod:methods', '.__T__rbind2:methods', '.__T__show:methods', '.__T__slotsFromS3:methods'), FALSE, FALSE, FALSE, TRUE, FALSE, FALSE); .Internal(grep(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]]))");
    }

    @Test
    public void testgrep7() {
        assertEval(Ignored.ParserErrorFormatting,
                        Output.IgnoreErrorContext,
                        "argv <- list(''', structure('exNSS4_1.0.tar.gz', .Names = ''), FALSE, FALSE, FALSE, FALSE, FALSE, FALSE); .Internal(grep(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]]))");
        assertEval("argv <- list('\\'', structure('exNSS4_1.0.tar.gz', .Names = ''), FALSE, FALSE, FALSE, FALSE, FALSE, FALSE); .Internal(grep(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]]))");
        assertEval("argv <- list('', structure('exNSS4_1.0.tar.gz', .Names = ''), FALSE, FALSE, FALSE, FALSE, FALSE, FALSE); .Internal(grep(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]]))");
    }

    @Test
    public void testgrep9() {
        assertEval("argv <- list('^[ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789]', 'all.R', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE); .Internal(grep(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]]))");
    }

    @Test
    public void testgrep10() {
        assertEval("argv <- list('-package$', structure(c('bkde', 'bkde2D', 'bkfe', 'dpih', 'dpik', 'dpill', 'locpoly'), .Names = c('/home/lzhao/tmp/RtmphvE7Uy/ltxf49c4960bf/bkde.tex', '/home/lzhao/tmp/RtmphvE7Uy/ltxf49c4960bf/bkde2D.tex', '/home/lzhao/tmp/RtmphvE7Uy/ltxf49c4960bf/bkfe.tex', '/home/lzhao/tmp/RtmphvE7Uy/ltxf49c4960bf/dpih.tex', '/home/lzhao/tmp/RtmphvE7Uy/ltxf49c4960bf/dpik.tex', '/home/lzhao/tmp/RtmphvE7Uy/ltxf49c4960bf/dpill.tex', '/home/lzhao/tmp/RtmphvE7Uy/ltxf49c4960bf/locpoly.tex')), FALSE, FALSE, TRUE, FALSE, FALSE, FALSE); .Internal(grep(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]]))");
    }

    @Test
    public void testgrep11() {
        assertEval("argv <- list('.__T__unname:', c('.__T__!:base', '.__T__%%:base', '.__T__%*%:base', '.__T__%/%:base', '.__T__&:base', '.__T__*:base', '.__T__+:base', '.__T__-:base', '.__T__/:base', '.__T__Arith:base', '.__T__BunchKaufman:Matrix', '.__T__Cholesky:Matrix', '.__T__Compare:methods', '.__T__Logic:base', '.__T__Math2:methods', '.__T__Math:base', '.__T__Ops:base', '.__T__Schur:Matrix', '.__T__Summary:base', '.__T__[:base', '.__T__[<-:base', '.__T__^:base', '.__T__all.equal:base', '.__T__all:base', '.__T__any:base', '.__T__as.array:base', '.__T__as.integer:base', '.__T__as.logical:base', '.__T__as.matrix:base', '.__T__as.numeric:base', '.__T__as.vector:base', '.__T__band:Matrix', '.__T__cbind2:methods', '.__T__chol2inv:base', '.__T__chol:base', '.__T__coerce:methods', '.__T__coerce<-:methods', '.__T__colMeans:base', '.__T__colSums:base', '.__T__cov2cor:stats', '.__T__crossprod:base', '.__T__determinant:base', '.__T__diag:base', '.__T__diag<-:base', '.__T__diff:base', '.__T__dim:base', '.__T__dim<-:base', '.__T__dimnames:base', '.__T__dimnames<-:base', '.__T__drop:base', '.__T__expand:Matrix', '.__T__expm:Matrix', '.__T__facmul:Matrix', '.__T__forceSymmetric:Matrix', '.__T__format:base', '.__T__head:utils', '.__T__image:graphics', '.__T__initialize:methods', '.__T__is.finite:base', '.__T__is.infinite:base', '.__T__is.na:base', '.__T__isDiagonal:Matrix', '.__T__isSymmetric:base', '.__T__isTriangular:Matrix', '.__T__kronecker:base', '.__T__length:base', '.__T__lu:Matrix', '.__T__mean:base', '.__T__nnzero:Matrix', '.__T__norm:base', '.__T__pack:Matrix', '.__T__print:base', '.__T__prod:base', '.__T__qr.Q:base', '.__T__qr.R:base', '.__T__qr.coef:base', '.__T__qr.fitted:base', '.__T__qr.qty:base', '.__T__qr.qy:base', '.__T__qr.resid:base', '.__T__qr:base', '.__T__rbind2:methods', '.__T__rcond:base', '.__T__rep:base', '.__T__rowMeans:base', '.__T__rowSums:base', '.__T__show:methods', '.__T__skewpart:Matrix', '.__T__solve:base', '.__T__sum:base', '.__T__summary:base', '.__T__symmpart:Matrix', '.__T__t:base', '.__T__tail:utils', '.__T__tcrossprod:base', '.__T__toeplitz:stats', '.__T__tril:Matrix', '.__T__triu:Matrix', '.__T__unname:base', '.__T__unpack:Matrix', '.__T__update:stats', '.__T__updown:Matrix', '.__T__which:base', '.__T__writeMM:Matrix', '.__T__zapsmall:base'), FALSE, FALSE, FALSE, TRUE, FALSE, FALSE); .Internal(grep(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]]))");
    }

    @Test
    public void testgrep12() {
        assertEval("argv <- list('^[[:blank:]]*$', 'mtext(\\\'«Latin-1 accented chars»: éè øØ å<Å æ<Æ\\\', side = 3)', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE); .Internal(grep(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]]))");
    }

    @Test
    public void testGrep() {
        assertEval("{ txt<-c(\"arm\",\"foot\",\"lefroo\", \"bafoobar\"); grep(\"foo\", txt) }");
        assertEval("{ txt<-c(\"is\", \"intended\", \"to\", \"guarantee\", \"your\", \"freedom\"); grep(\"[gu]\", txt) }");
        assertEval("{ txt<-c(\"1+1i\", \"7\", \"42.1\", \"7+42i\"); grep(\"[0-9].*[-+][0-9].*i$\", txt) }");
        assertEval("{ txt<-c(\"rai\", \"ira\", \"iri\"); grep(\"i$\", txt) }");
    }

    @Test
    public void testLsRegExp() {
        assertEval("{ abc <- 1; ls(pattern=\"a.*\")}");
        assertEval("{ .abc <- 1; ls(pattern=\"\\\\.a.*\")}");
        assertEval("{ .abc <- 1; ls(all.names=TRUE, pattern=\"\\\\.a.*\")}");
        assertEval("{ abc <- 1; ls(pattern=\"[[:alpha:]]*\")}");
        assertEval("{ f <- function(abc) { ls(pattern=\"[a-z]*\") }; f(1) }");
    }
}
