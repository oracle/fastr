/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2012-2014, Purdue University
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check
public class TestBuiltin_gsub extends TestBase {

    @Test
    public void testgsub1() {
        assertEval("argv <- list('([[:alnum:]])--([[:alnum:]])', '\\\\1-\\\\2', 'Date-Time Classes', FALSE, FALSE, FALSE, FALSE); .Internal(gsub(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testgsub2() {
        assertEval("argv <- list('\\\\\\\\(link|var)\\\\{([^}]+)\\\\}', '\\\\2', structure('     \\\'Jetz no chli züritüütsch: (noch ein bißchen Zürcher deutsch)\\\')\\n', Rd_tag = 'RCODE'), FALSE, TRUE, FALSE, TRUE); .Internal(gsub(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testgsub3() {
        assertEval("argv <- list('\\\\bsl', '\\\\bsl{}', structure('     knots).\\n', Rd_tag = 'TEXT'), FALSE, FALSE, TRUE, TRUE); .Internal(gsub(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testgsub4() {
        assertEval("argv <- list('\\\\bsl', '\\\\bsl{}', structure('  ', Rd_tag = 'TEXT'), FALSE, FALSE, TRUE, TRUE); .Internal(gsub(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testgsub5() {
        assertEval("argv <- list('([{}$#_])', '\\\\\\\\\\\\1', structure('2013-03-19 13:18:58', .Names = 'Date/Publication'), FALSE, FALSE, FALSE, TRUE); .Internal(gsub(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testgsub6() {
        assertEval("argv <- list('é', 'gh', '«Latin-1 accented chars»: éè øØ å<Å æ<Æ é éè', FALSE, FALSE, TRUE, FALSE); .Internal(gsub(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testgsub7() {
        assertEval("argv <- list('([&$%_#])', '\\\\\\\\\\\\1', structure(', then ', Rd_tag = 'TEXT'), FALSE, TRUE, FALSE, TRUE); .Internal(gsub(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testgsub8() {
        assertEval("argv <- list(\''([^']*)'', '‘\\\\1’', '‘/home/lzhao/hg/r-instrumented/tests/rpart.Rcheck’', FALSE, FALSE, FALSE, FALSE); .Internal(gsub(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testgsub9() {
        assertEval("argv <- list('\\\\\', '\\\\bsl', structure('range specified by ', Rd_tag = 'TEXT'), FALSE, FALSE, TRUE, TRUE); .Internal(gsub(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testgsub10() {
        // FIXME GnuR result appears to be more logical:
        // Expected output: [1] "xbxcx"
        // FastR output: [1] "xbxxcx"
        assertEval(Ignored.ImplementationError,
                        "argv <- list('a*', 'x', 'baaac', FALSE, FALSE, FALSE, FALSE); .Internal(gsub(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testgsub11() {
        assertEval("argv <- list('é', 'gh', '«Latin-1 accented chars»: éè øØ å<Å æ<Æ é éè', FALSE, FALSE, FALSE, FALSE); .Internal(gsub(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testgsub12() {
        assertEval("argv <- list('([[:digit:]]+[.-]){1,}[[:digit:]]+', '', 'pkgB_1.0.tar.gz', FALSE, FALSE, FALSE, FALSE); .Internal(gsub(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testgsub13() {
        // FIXME FastR does not recognize word boundary properly:
        // Expected output: [1] "|The| |quick| |brown| |èé|"
        // FastR output: [1] "|The| |quick| |brown| èé"
        assertEval(Ignored.ImplementationError,
                        "argv <- list('\\\\b', '|', 'The quick brown èé', FALSE, TRUE, FALSE, FALSE); .Internal(gsub(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testgsub14() {
        assertEval("argv <- list('/home/lzhao/hg/r-instrumented/src/library/utils', '', '/home/lzhao/hg/r-instrumented/src/library/utils/vignettes', FALSE, FALSE, TRUE, FALSE); .Internal(gsub(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testgsub15() {
        assertEval("argv <- list('(‘|’)', '\\'', c('', '', '> library(survival)', 'Loading required package: splines', '> options(na.action=na.exclude) # preserve missings', '> options(contrasts=c(\\'contr.treatment\\', \\'contr.poly\\')) #ensure constrast type', '> ', '> #', '> # Simple tests of concordance.  These numbers were derived in multiple', '> #   codes.', '> #', '> aeq <- function(x,y, ...) all.equal(as.vector(x), as.vector(y), ...)', '> ', '> grank <- function(x, time, grp, wt) ', '+     unlist(tapply(x, grp, rank))', '> grank2 <- function(x, time, grp, wt) {  #for case weights', '+     if (length(wt)==0) wt <- rep(1, length(x))', '+     z <- double(length(x))', '+     for (i in unique(grp)) {', '+         indx <- which(grp==i)', '+         temp <- tapply(wt[indx], x[indx], sum)', '+         temp <- temp/2  + c(0, cumsum(temp)[-length(temp)])', '+         z[indx] <- temp[match(x[indx], names(temp))]', '+     }', '+     z', '+ }', '> ', '> ', '> tdata <- aml[aml$x==\\'Maintained\\',]', '> tdata$y <- c(1,6,2,7,3,7,3,8,4,4,5)', '> tdata$wt <- c(1,2,3,2,1,2,3,4,3,2,1)', '> fit <- survConcordance(Surv(time, status) ~y, tdata)', '> aeq(fit$stats[1:4], c(14,24,2,0))', '[1] TRUE', '> cfit <- coxph(Surv(time, status) ~ tt(y), tdata, tt=grank, method=\\'breslow\\',', '+               iter=0, x=T)', '> cdt <- coxph.detail(cfit)', '> aeq(4*sum(cdt$imat),fit$stats[5]^2) ', '[1] TRUE', '> aeq(2*sum(cdt$score), diff(fit$stats[2:1]))', '[1] TRUE', '> ', '> ', '> # Lots of ties', '> tempx <- Surv(c(1,2,2,2,3,4,4,4,5,2), c(1,0,1,0,1,0,1,1,0,1))', '> tempy <- c(5,5,4,4,3,3,7,6,5,4)', '> fit2 <- survConcordance(tempx ~ tempy)', '> aeq(fit2$stats[1:4], c(13,13,5,2))', '[1] TRUE', '> cfit2 <-  coxph(tempx ~ tt(tempy), tt=grank, method=\\'breslow\\', iter=0)', '> aeq(4/cfit2$var, fit2$stats[5]^2)', '[1] TRUE', '> ', '> # Bigger data', '> fit3 <- survConcordance(Surv(time, status) ~ age, lung)', '> aeq(fit3$stats[1:4], c(10717, 8706, 591, 28))', '[1] TRUE', '> cfit3 <- coxph(Surv(time, status) ~ tt(age), lung, ', '+                iter=0, method=\\'breslow\\', tt=grank, x=T)', '> cdt <- coxph.detail(cfit3)', '> aeq(4*sum(cdt$imat),fit3$stats[5]^2) ', '[1] TRUE', '> aeq(2*sum(cdt$score), diff(fit3$stats[2:1]))', '[1] TRUE', '> ', '> ', '> # More ties', '> fit4 <- survConcordance(Surv(time, status) ~ ph.ecog, lung)', '> aeq(fit4$stats[1:4], c(8392, 4258, 7137, 28))', '[1] TRUE', '> cfit4 <- coxph(Surv(time, status) ~ tt(ph.ecog), lung, ', '+                iter=0, method=\\'breslow\\', tt=grank)', '> aeq(4/cfit4$var, fit4$stats[5]^2)', '[1] TRUE', '> ', '> # Case weights', '> fit5 <- survConcordance(Surv(time, status) ~ y, tdata, weight=wt)', '> fit6 <- survConcordance(Surv(time, status) ~y, tdata[rep(1:11,tdata$wt),])', '> aeq(fit5$stats[1:4], c(70, 91, 7, 0))  # checked by hand', '[1] TRUE', '> aeq(fit5$stats[1:3], fit6$stats[1:3])  #spurious \\\'tied on time\\\' value, ignore', '[1] TRUE', '> aeq(fit5$std, fit6$std)', '[1] TRUE', '> cfit5 <- coxph(Surv(time, status) ~ tt(y), tdata, weight=wt, ', '+                iter=0, method=\\'breslow\\', tt=grank2)', '> cfit6 <- coxph(Surv(time, status) ~ tt(y), tdata[rep(1:11,tdata$wt),], ', '+                iter=0, method=\\'breslow\\', tt=grank)', '> aeq(4/cfit6$var, fit6$stats[5]^2)', '[1] TRUE', '> aeq(cfit5$var, cfit6$var)', '[1] TRUE', '> ', '> # Start, stop simplest cases', '> fit7 <- survConcordance(Surv(rep(0,11), time, status) ~ y, tdata)', '> aeq(fit7$stats, fit$stats)', '[1] TRUE', '> aeq(fit7$std.err, fit$std.err)', '[1] TRUE', '> fit7 <- survConcordance(Surv(rep(0,11), time, status) ~ y, tdata, weight=wt)', '> aeq(fit5$stats, fit7$stats)', '[1] TRUE', '> ', '> # Multiple intervals for some, but same risk sets as tdata', '> tdata2 <- data.frame(time1=c(0,3, 5,  6,7,   0,  4,17,  7,  0,16,  2,  0, ', '+                              0,9, 5),', '+                      time2=c(3,9, 13, 7,13, 18, 17,23, 28, 16,31, 34, 45, ', '+                              9,48, 60),', '+                      status=c(0,1, 1, 0,0,  1,  0,1, 0, 0,1, 1, 0, 0,1, 0),', '+                      y = c(1,1, 6, 2,2, 7, 3,3, 7, 3,3, 8, 4, 4,4, 5),', '+                      wt= c(1,1, 2, 3,3, 2, 1,1, 2, 3,3, 4, 3, 2,2, 1))', '> fit8 <- survConcordance(Surv(time1, time2, status) ~y, tdata2, weight=wt)', '> aeq(fit5$stats, fit8$stats)', '[1] TRUE', '> aeq(fit5$std.err, fit8$std.err)', '[1] TRUE', '> cfit8 <- coxph(Surv(time1, time2, status) ~ tt(y), tdata2, weight=wt, ', '+                iter=0, method=\\'breslow\\', tt=grank2)', '> aeq(4/cfit8$var, fit8$stats[5]^2)', '[1] TRUE', '> aeq(fit8$stats[5]/(2*sum(fit8$stats[1:3])), fit8$std.err)', '[1] TRUE', '> ', '> # Stratified', '> tdata3 <- data.frame(time1=c(tdata2$time1, rep(0, nrow(lung))),', '+                      time2=c(tdata2$time2, lung$time),', '+                      status = c(tdata2$status, lung$status -1),', '+                      x = c(tdata2$y, lung$ph.ecog),', '+                      wt= c(tdata2$wt, rep(1, nrow(lung))),', '+                      grp=rep(1:2, c(nrow(tdata2), nrow(lung))))', '> fit9 <- survConcordance(Surv(time1, time2, status) ~x + strata(grp),', '+                         data=tdata3, weight=wt)', '> aeq(fit9$stats[1,], fit5$stats)', '[1] TRUE', '> aeq(fit9$stats[2,], fit4$stats)', '[1] TRUE', '> '), FALSE, TRUE, FALSE, TRUE); .Internal(gsub(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testgsub16() {
        assertEval("argv <- list('[[:blank:][:cntrl:]]*', '', structure(' unix\\n', Rd_tag = 'TEXT'), FALSE, TRUE, FALSE, TRUE); .Internal(gsub(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testgsub17() {
        assertEval("argv <- list('(\\\\w)(\\\\w*)', '\\\\U\\\\1\\\\L\\\\2', 'a test of capitalizing', FALSE, TRUE, FALSE, FALSE); .Internal(gsub(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testgsub18() {
        assertEval("argv <- list('\\\\.', '\\\\\\\\.', '^*.t??$', FALSE, FALSE, FALSE, FALSE); .Internal(gsub(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testgsub19() {
        assertEval("argv <- list('(\\\\w)(\\\\w*)(\\\\w)', '\\\\U\\\\1\\\\E\\\\2\\\\U\\\\3', 'useRs may fly into JFK or laGuardia', FALSE, TRUE, FALSE, FALSE); .Internal(gsub(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testgsub20() {
        assertEval("argv <- list('([&$%_#])', '\\\\\\\\\\\\1', structure('with 5% of the range added to each end.\\n', Rd_tag = 'TEXT'), FALSE, TRUE, FALSE, TRUE); .Internal(gsub(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testgsub21() {
        assertEval("argv <- list('[[:space:]]*%+[[:space:]]*\\\\\\\\VignetteEngine\\\\{([^}]*)\\\\}', '\\\\1', character(0), FALSE, FALSE, FALSE, FALSE); .Internal(gsub(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testgsub22() {
        assertEval("argv <- list('^\\\\s+', '', ' utilities ', FALSE, TRUE, FALSE, TRUE); .Internal(gsub(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testgsub23() {
        assertEval("argv <- list('([^\\\\])\\\\[', '\\\\1\\\\\\\\[', '^.*[.*$', FALSE, FALSE, FALSE, FALSE); .Internal(gsub(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testgsub24() {
        assertEval("argv <- list('.__M__(.*):([^:]+)', '\\\\1', character(0), FALSE, FALSE, FALSE, FALSE); .Internal(gsub(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testgsub25() {
        assertEval("argv <- list('%', '\\\\\\\\%', structure('foo', .Names = 'object'), FALSE, FALSE, FALSE, FALSE); .Internal(gsub(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testgsub26() {
        assertEval("argv <- list('\\\\\\\\(l|)dots', '...', structure('plot(1:10, 1:10, main = \\\'text(...) examples\\\\n~~~~~~~~~~~~~~\\\',\\n', Rd_tag = 'RCODE'), FALSE, TRUE, FALSE, TRUE); .Internal(gsub(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testgsub27() {
        assertEval("argv <- list('.__T__(.*):([^:]+(.*))', '\\\\2', c('.__T__!:base', '.__T__%%:base', '.__T__%*%:base', '.__T__%/%:base', '.__T__&:base', '.__T__*:base', '.__T__+:base', '.__T__-:base', '.__T__/:base', '.__T__Arith:base', '.__T__BunchKaufman:Matrix', '.__T__Cholesky:Matrix', '.__T__Compare:methods', '.__T__Logic:base', '.__T__Math2:methods', '.__T__Math:base', '.__T__Ops:base', '.__T__Schur:Matrix', '.__T__Summary:base', '.__T__[:base', '.__T__[<-:base', '.__T__^:base', '.__T__all.equal:base', '.__T__all:base', '.__T__any:base', '.__T__as.array:base', '.__T__as.integer:base', '.__T__as.logical:base', '.__T__as.matrix:base', '.__T__as.numeric:base', '.__T__as.vector:base', '.__T__band:Matrix', '.__T__cbind2:methods', '.__T__chol2inv:base', '.__T__chol:base', '.__T__coerce:methods', '.__T__coerce<-:methods', '.__T__colMeans:base', '.__T__colSums:base', '.__T__cov2cor:stats', '.__T__crossprod:base', '.__T__determinant:base', '.__T__diag:base', '.__T__diag<-:base', '.__T__diff:base', '.__T__dim:base', '.__T__dim<-:base', '.__T__dimnames:base', '.__T__dimnames<-:base', '.__T__drop:base', '.__T__expand:Matrix', '.__T__expm:Matrix', '.__T__facmul:Matrix', '.__T__forceSymmetric:Matrix', '.__T__format:base', '.__T__head:utils', '.__T__image:graphics', '.__T__initialize:methods', '.__T__is.finite:base', '.__T__is.infinite:base', '.__T__is.na:base', '.__T__isDiagonal:Matrix', '.__T__isSymmetric:base', '.__T__isTriangular:Matrix', '.__T__kronecker:base', '.__T__length:base', '.__T__lu:Matrix', '.__T__mean:base', '.__T__nnzero:Matrix', '.__T__norm:base', '.__T__pack:Matrix', '.__T__print:base', '.__T__prod:base', '.__T__qr.Q:base', '.__T__qr.R:base', '.__T__qr.coef:base', '.__T__qr.fitted:base', '.__T__qr.qty:base', '.__T__qr.qy:base', '.__T__qr.resid:base', '.__T__qr:base', '.__T__rbind2:methods', '.__T__rcond:base', '.__T__rep:base', '.__T__rowMeans:base', '.__T__rowSums:base', '.__T__show:methods', '.__T__skewpart:Matrix', '.__T__solve:base', '.__T__sum:base', '.__T__summary:base', '.__T__symmpart:Matrix', '.__T__t:base', '.__T__tail:utils', '.__T__tcrossprod:base', '.__T__toeplitz:stats', '.__T__tril:Matrix', '.__T__triu:Matrix', '.__T__unname:base', '.__T__unpack:Matrix', '.__T__update:stats', '.__T__updown:Matrix', '.__T__which:base', '.__T__writeMM:Matrix', '.__T__zapsmall:base'), FALSE, FALSE, FALSE, FALSE); .Internal(gsub(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testgsub28() {
        assertEval("argv <- list('([^\\\\])\\\\(', '\\\\1\\\\\\\\(', '^.*{n.*$', FALSE, FALSE, FALSE, FALSE); .Internal(gsub(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testgsub30() {
        assertEval("argv <- structure(list(pattern = 'a*', replacement = 'x', x = 'baaaac',     perl = TRUE), .Names = c('pattern', 'replacement', 'x', 'perl'));do.call('gsub', argv)");
    }

    @Test
    public void testgsub31() {
        // FIXME GnuR result appears to be more logical:
        // Expected output: [1] "xbxcx"
        // FastR output: [1] "xbxxcx"
        assertEval(Ignored.ImplementationError, "argv <- structure(list(pattern = 'a*', replacement = 'x', x = 'baaaac'),     .Names = c('pattern', 'replacement', 'x'));do.call('gsub', argv)");
    }

    @Test
    public void testGsub() {
        assertEval("{ gsub(\"a\",\"aa\", \"prague alley\") }");
        assertEval("{ gsub(\"a\",\"aa\", \"prAgue alley\") }");
        assertEval("{ gsub(\"a\",\"aa\", \"prague alley\", fixed=TRUE) }");
        assertEval("{ gsub(\"a\",\"aa\", \"prAgue alley\", fixed=TRUE) }");
        assertEval("{ gsub(\"a\",\"aa\", \"prAgue alley\", fixed=TRUE, ignore.case=TRUE) }");
        assertEval("{ gsub(\"([a-e])\",\"\\\\1\\\\1\", \"prague alley\") }");
        assertEval("{ gsub(\"h\",\"\", c(\"hello\", \"hi\", \"bye\")) }");
        assertEval("{ gsub(\"h\",\"\", c(\"hello\", \"hi\", \"bye\"), fixed=TRUE) }");
        // FIXME not yet implemented: ignoreCase == true
        assertEval(Ignored.Unimplemented, "{ gsub(\"a\",\"aa\", \"prAgue alley\", ignore.case=TRUE) }");

        assertEval("{ .Internal(gsub(7, \"42\", \"7\", F, F, F, F)) }");
        assertEval("{ .Internal(gsub(character(), \"42\", \"7\", F, F, F, F)) }");
        assertEval("{ .Internal(gsub(\"7\", 42, \"7\", F, F, F, F)) }");
        assertEval("{ .Internal(gsub(\"7\", character(), \"7\", F, F, F, F)) }");
        assertEval("{ .Internal(gsub(\"7\", \"42\", 7, F, F, F, F)) }");

        assertEval("{ gsub(pattern = 'a*', replacement = 'x', x = 'ÄaÄ', perl = TRUE) }");
        assertEval("{ gsub(pattern = 'a*', replacement = 'x', x = 'ÄaaaaÄ', perl = TRUE) }");

        // Expected output: [1] "xaxbx"
        // FastR output: [1] "axxxxxb"
        assertEval(Ignored.ImplementationError, "{ gsub(pattern = 'Ä*', replacement = 'x', x = 'aÄÄÄÄÄb', perl = TRUE) }");
    }
}
