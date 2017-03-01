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
public class TestBuiltin_substr extends TestBase {

    @Test
    public void testsubstr1() {
        assertEval("argv <- list('weight', 1L, 2L); .Internal(substr(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testsubstr2() {
        assertEval("argv <- list(c('        ', '        '), 1L, c(4L, -16L)); .Internal(substr(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testsubstr3() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(c('as.formula', 'coef', 'makepredictcall', 'na.fail', 'predict'), .Names = c('as.formula', 'coef', 'makepredictcall', 'na.fail', 'predict')), 1L, 6L); .Internal(substr(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testsubstr4() {
        assertEval("argv <- list(character(0), 7L, 1000000L); .Internal(substr(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testsubstr5() {
        assertEval(Ignored.Unknown, "argv <- list(structure('to be supported).', Rd_tag = 'TEXT'), 17L, 17L); .Internal(substr(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testsubstr6() {
        assertEval("argv <- list(character(0), 1L, 5L); .Internal(substr(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testsubstr7() {
        assertEval("argv <- list('', 1L, 2L); .Internal(substr(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testsubstr8() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(c('model.frame', 'predict', 'residuals'), .Names = c('model.frame', 'predict', 'residuals')), 1L, 6L); .Internal(substr(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testsubstr9() {
        assertEval("argv <- list('> ### R code from vignette source \\'parallel.Rnw\\'\\n> \\n> ###################################################\\n> ### code chunk number 1: parallel.Rnw:474-475 (eval = FALSE)\\n> ###################################################\\n> ## library(parallel)\\n> \\n> \\n> ###################################################\\n> ### code chunk number 2: parallel.Rnw:500-507 (eval = FALSE)\\n> ###################################################\\n> ## library(boot)\\n> ## cd4.rg <- function(data, mle) MASS::mvrnorm(nrow(data), mle$m, mle$v)\\n> ## cd4.mle <- list(m = colMeans(cd4), v = var(cd4))\\n> ## cd4.boot <- boot(cd4, corr, R = 999, sim = \\\'parametric\\\',\\n> ##                  ran.gen = cd4.rg, mle = cd4.mle)\\n> ## boot.ci(cd4.boot,  type = c(\\\'norm\\\', \\\'basic\\\', \\\'perc\\\'),\\n> ##         conf = 0.9, h = atanh, hinv = tanh)\\n> \\n> \\n> ###################################################\\n> ### code chunk number 3: parallel.Rnw:512-522 (eval = FALSE)\\n> ###################################################\\n> ## cd4.rg <- function(data, mle) MASS::mvrnorm(nrow(data), mle$m, mle$v)\\n> ## cd4.mle <- list(m = colMeans(cd4), v = var(cd4))\\n> ## run1 <- function(...) boot(cd4, corr, R = 500, sim = \\\'parametric\\\',\\n> ##                            ran.gen = cd4.rg, mle = cd4.mle)\\n> ## mc <- 2 # set as appropriate for your hardware\\n> ## ## To make this reproducible:\\n> ## set.seed(123, \\\'L\\'Ecuyer\\\')\\n> ## cd4.boot <- do.call(c, mclapply(seq_len(mc), run1) )\\n> ## boot.ci(cd4.boot,  type = c(\\\'norm\\\', \\\'basic\\\', \\\'perc\\\'),\\n> ##         conf = 0.9, h = atanh, hinv = tanh)\\n> \\n> \\n> ###################################################\\n> ### code chunk number 4: parallel.Rnw:527-528 (eval = FALSE)\\n> ###################################################\\n> ## do.call(c, lapply(seq_len(mc), run1))\\n> \\n> \\n> ###################################################\\n> ### code chunk number 5: parallel.Rnw:532-547 (eval = FALSE)\\n> ###################################################\\n> ## run1 <- function(...) {\\n> ##    library(boot)\\n> ##    cd4.rg <- function(data, mle) MASS::mvrnorm(nrow(data), mle$m, mle$v)\\n> ##    cd4.mle <- list(m = colMeans(cd4), v = var(cd4))\\n> ##    boot(cd4, corr, R = 500, sim = \\\'parametric\\\',\\n> ##         ran.gen = cd4.rg, mle = cd4.mle)\\n> ## }\\n> ## cl <- makeCluster(mc)\\n> ## ## make this reproducible\\n> ## clusterSetRNGStream(cl, 123)\\n> ## library(boot) # needed for c() method on master\\n> ## cd4.boot <- do.call(c, parLapply(cl, seq_len(mc), run1) )\\n> ## boot.ci(cd4.boot,  type = c(\\\'norm\\\', \\\'basic\\\', \\\'perc\\\'),\\n> ##         conf = 0.9, h = atanh, hinv = tanh)\\n> ## stopCluster(cl)\\n> \\n> \\n> ###################################################\\n> ### code chunk number 6: parallel.Rnw:557-570 (eval = FALSE)\\n> ###################################################\\n> ## cl <- makeCluster(mc)\\n> ## cd4.rg <- function(data, mle) MASS::mvrnorm(nrow(data), mle$m, mle$v)\\n> ## cd4.mle <- list(m = colMeans(cd4), v = var(cd4))\\n> ## clusterExport(cl, c(\\\'cd4.rg\\\', \\\'cd4.mle\\\'))\\n> ## junk <- clusterEvalQ(cl, library(boot)) # discard result\\n> ## clusterSetRNGStream(cl, 123)\\n> ## res <- clusterEvalQ(cl, boot(cd4, corr, R = 500,\\n> ##                     sim = \\\'parametric\\\', ran.gen = cd4.rg, mle = cd4.mle))\\n> ## library(boot) # needed for c() method on master\\n> ## cd4.boot <- do.call(c, res)\\n> ## boot.ci(cd4.boot,  type = c(\\\'norm\\\', \\\'basic\\\', \\\'perc\\\'),\\n> ##         conf = 0.9, h = atanh, hinv = tanh)\\n> ## stopCluster(cl)\\n> \\n> \\n> ###################################################\\n> ### code chunk number 7: parallel.Rnw:575-589 (eval = FALSE)\\n> ###################################################\\n> ## R <- 999; M <- 999 ## we would like at least 999 each\\n> ## cd4.nest <- boot(cd4, nested.corr, R=R, stype=\\\'w\\\', t0=corr(cd4), M=M)\\n> ## ## nested.corr is a function in package boot\\n> ## op <- par(pty = \\\'s\\\', xaxs = \\\'i\\\', yaxs = \\\'i\\\')\\n> ## qqplot((1:R)/(R+1), cd4.nest$t[, 2], pch = \\\'.\\\', asp = 1,\\n> ##         xlab = \\\'nominal\\\', ylab = \\\'estimated\\\')\\n> ## abline(a = 0, b = 1, col = \\\'grey\\\')\\n> ## abline(h = 0.05, col = \\\'grey\\\')\\n> ## abline(h = 0.95, col = \\\'grey\\\')\\n> ## par(op)\\n> ## \\n> ## nominal <- (1:R)/(R+1)\\n> ## actual <- cd4.nest$t[, 2]\\n> ## 100*nominal[c(sum(actual <= 0.05), sum(actual < 0.95))]\\n> \\n> \\n> ###################################################\\n> ### code chunk number 8: parallel.Rnw:594-602 (eval = FALSE)\\n> ###################################################\\n> ## mc <- 9\\n> ## R <- 999; M <- 999; RR <- floor(R/mc)\\n> ## run2 <- function(...)\\n> ##     cd4.nest <- boot(cd4, nested.corr, R=RR, stype=\\\'w\\\', t0=corr(cd4), M=M)\\n> ## cd4.nest <- do.call(c, mclapply(seq_len(mc), run2, mc.cores = mc) )\\n> ## nominal <- (1:R)/(R+1)\\n> ## actual <- cd4.nest$t[, 2]\\n> ## 100*nominal[c(sum(actual <= 0.05), sum(actual < 0.95))]\\n> \\n> \\n> ###################################################\\n> ### code chunk number 9: parallel.Rnw:616-627 (eval = FALSE)\\n> ###################################################\\n> ## library(spatial)\\n> ## towns <- ppinit(\\\'towns.dat\\\')\\n> ## tget <- function(x, r=3.5) sum(dist(cbind(x$x, x$y)) < r)\\n> ## t0 <- tget(towns)\\n> ## R <- 1000\\n> ## c <- seq(0, 1, 0.1)\\n> ## ## res[1] = 0\\n> ## res <- c(0, sapply(c[-1], function(c)\\n> ##     mean(replicate(R, tget(Strauss(69, c=c, r=3.5))))))\\n> ## plot(c, res, type=\\\'l\\\', ylab=\\\'E t\\\')\\n> ## abline(h=t0, col=\\\'grey\\\')\\n> \\n> \\n> ###################################################\\n> ### code chunk number 10: parallel.Rnw:631-640 (eval = FALSE)\\n> ###################################################\\n> ## run3 <- function(c) {\\n> ##     library(spatial)\\n> ##     towns <- ppinit(\\\'towns.dat\\\') # has side effects\\n> ##     mean(replicate(R, tget(Strauss(69, c=c, r=3.5))))\\n> ## }\\n> ## cl <- makeCluster(10, methods = FALSE)\\n> ## clusterExport(cl, c(\\\'R\\\', \\\'towns\\\', \\\'tget\\\'))\\n> ## res <- c(0, parSapply(cl, c[-1], run3)) # 10 tasks\\n> ## stopCluster(cl)\\n> \\n> \\n> ###################################################\\n> ### code chunk number 11: parallel.Rnw:644-648 (eval = FALSE)\\n> ###################################################\\n> ## cl <- makeForkCluster(10)  # fork after the variables have been set up\\n> ## run4 <- function(c)  mean(replicate(R, tget(Strauss(69, c=c, r=3.5))))\\n> ## res <- c(0, parSapply(cl, c[-1], run4))\\n> ## stopCluster(cl)\\n> \\n> \\n> ###################################################\\n> ### code chunk number 12: parallel.Rnw:651-653 (eval = FALSE)\\n> ###################################################\\n> ## run4 <- function(c)  mean(replicate(R, tget(Strauss(69, c=c, r=3.5))))\\n> ## res <- c(0, unlist(mclapply(c[-1], run4, mc.cores = 10)))\\n> \\n> \\n> ###################################################\\n> ### code chunk number 13: parallel.Rnw:684-718 (eval = FALSE)\\n> ###################################################\\n> ## pkgs <- \\\'<names of packages to be installed>\\\'\\n> ## M <- 20 # number of parallel installs\\n> ## M <- min(M, length(pkgs))\\n> ## library(parallel)\\n> ## unlink(\\\'install_log\\\')\\n> ## cl <- makeCluster(M, outfile = \\\'install_log\\\')\\n> ## clusterExport(cl, c(\\\'tars\\\', \\\'fakes\\\', \\\'gcc\\\')) # variables needed by do_one\\n> ## \\n> ## ## set up available via a call to available.packages() for\\n> ## ## repositories containing all the packages involved and all their\\n> ## ## dependencies.\\n> ## DL <- utils:::.make_dependency_list(pkgs, available, recursive = TRUE)\\n> ## DL <- lapply(DL, function(x) x[x %in% pkgs])\\n> ## lens <- sapply(DL, length)\\n> ## ready <- names(DL[lens == 0L])\\n> ## done <- character() # packages already installed\\n> ## n <- length(ready)\\n> ## submit <- function(node, pkg)\\n> ##     parallel:::sendCall(cl[[node]], do_one, list(pkg), tag = pkg)\\n> ## for (i in 1:min(n, M)) submit(i, ready[i])\\n> ## DL <- DL[!names(DL) %in% ready[1:min(n, M)]]\\n> ## av <- if(n < M) (n+1L):M else integer() # available workers\\n> ## while(length(done) < length(pkgs)) {\\n> ##     d <- parallel:::recvOneResult(cl)\\n> ##     av <- c(av, d$node)\\n> ##     done <- c(done, d$tag)\\n> ##     OK <- unlist(lapply(DL, function(x) all(x %in% done) ))\\n> ##     if (!any(OK)) next\\n> ##     p <- names(DL)[OK]\\n> ##     m <- min(length(p), length(av)) # >= 1\\n> ##     for (i in 1:m) submit(av[i], p[i])\\n> ##     av <- av[-(1:m)]\\n> ##     DL <- DL[!names(DL) %in% p[1:m]]\\n> ## }\\n> \\n> \\n> ###################################################\\n> ### code chunk number 14: parallel.Rnw:731-748 (eval = FALSE)\\n> ###################################################\\n> ##     fn <- function(r) statistic(data, i[r, ], ...)\\n> ##     RR <- sum(R)\\n> ##     res <- if (ncpus > 1L && (have_mc || have_snow)) {\\n> ##         if (have_mc) {\\n> ##             parallel::mclapply(seq_len(RR), fn, mc.cores = ncpus)\\n> ##         } else if (have_snow) {\\n> ##             list(...) # evaluate any promises\\n> ##             if (is.null(cl)) {\\n> ##                 cl <- parallel::makePSOCKcluster(rep(\\\'localhost\\\', ncpus))\\n> ##                 if(RNGkind()[1L] == \\\'L\\'Ecuyer-CMRG\\\')\\n> ##                     parallel::clusterSetRNGStream(cl)\\n> ##                 res <- parallel::parLapply(cl, seq_len(RR), fn)\\n> ##                 parallel::stopCluster(cl)\\n> ##                 res\\n> ##             } else parallel::parLapply(cl, seq_len(RR), fn)\\n> ##         }\\n> ##     } else lapply(seq_len(RR), fn)\\n> \\n> \\n> ###################################################\\n> ### code chunk number 15: parallel.Rnw:751-752 (eval = FALSE)\\n> ###################################################\\n> ##             list(...) # evaluate any promises\\n> \\n> ', 1L, 150L); .Internal(substr(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testSubstring() {
        assertEval("{ substr(\"123456\", 2L, 4L) }");
        assertEval("{ substr(\"123456\", 2, 4) }");
        assertEval("{ substr(\"123456\", 4, 2) }");
        assertEval("{ substr(\"123456\", 7, 8) }");
        assertEval("{ substr(\"123456\", 4, 8) }");
        assertEval("{ substr(\"123456\", -1, 3) }");
        assertEval("{ substr(\"123456\", -5, -1) }");
        assertEval("{ substr(\"123456\", -20, -100) }");
        assertEval("{ substr(\"123456\", 2.8, 4) }");
        assertEval("{ substr(c(\"hello\", \"bye\"), 1, 2) }");
        assertEval("{ substr(c(\"hello\", \"bye\"), c(1,2,3), 4) }");
        assertEval("{ substr(c(\"hello\", \"bye\"), 1, c(1,2,3)) }");
        assertEval("{ substr(c(\"hello\", \"bye\"), c(1,2), c(2,3)) }");
        assertEval("{ substr(1234L,2,3) }");
        assertEval("{ substr(1234,2,3) }");
        assertEval("{ substr(\"abcdef\",c(1,2),c(3L,5L)) }");
        assertEval("{ substr(c(\"abcdef\", \"aa\"), integer(), 2) }");
        assertEval("{ substr(c(\"abcdef\", \"aa\"), 2, integer()) }");
        assertEval("{ substr(character(), integer(), integer()) }");
        assertEval("{ substr(c(\"abcdef\", \"aa\"), NA, 4) }");
        assertEval("{ substr(c(\"abcdef\", \"aa\"), 3, NA) }");
        assertEval("{ substr(c(\"abcdef\", \"aa\"), c(NA,8), 4) }");
        assertEval("{ substr(c(\"abcdef\", \"aa\"), c(1,NA), 4) }");

        assertEval("{ substr(NA,1,2) }");
        assertEval("{ substr(\"fastr\", NA, 2) }");
        assertEval("{ substr(\"fastr\", 1, NA) }");

        assertEval("{ x<-\"abcdef\"; substr(x,1,4)<-\"0000\"; x }");
        assertEval("{ x<-\"abcdef\"; substr(x,1,3)<-\"0000\"; x }");
        assertEval("{ x<-\"abcdef\"; substr(x,1,3)<-\"0\"; x }");
        assertEval("{ x<-\"abcdef\"; substr(x,NA,3)<-\"0\"; x }");
        assertEval("{ x<-\"abcdef\"; substr(x,1,NA)<-\"0\"; x }");
        assertEval("{ x<-character(); substr(x,1,3)<-\"0\"; x }");
        assertEval("{ x<-c(\"abcdef\", \"ghijklm\"); substr(x, c(1,NA), 4)<-\"0\"; x }");

        assertEval(Output.ImprovedErrorContext, "{ x<-\"abcdef\"; substr(x,3,1)<-0; x }");
        assertEval(Output.ImprovedErrorContext, "{ x<-\"abcdef\"; substr(x,1,3)<-character(); x }");
        assertEval(Output.ImprovedErrorContext, "{ x<-\"abcdef\"; substr(x,1,3)<-NULL; x }");
        assertEval(Output.ImprovedErrorContext, "{ x<-\"abcdef\"; substr(x,integer(),3)<-NULL; x }");

        assertEval("{ x<-character(); substr(x,1,3)<-0; x }");
        assertEval("{ x<-character(); substr(x,1,3)<-NULL; x }");
        assertEval("{ x<-character(); substr(x,integer(),3)<-NULL; x }");

        assertEval("{ x<-c(\"abcdef\"); substr(x[1], 2, 3)<-\"0\"; x }");
    }
}
