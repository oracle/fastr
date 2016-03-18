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
public class TestBuiltin_grepl extends TestBase {

    @Test
    public void testgrepl1() {
        assertEval("argv <- list('([[:digit:]]+[.-]){1,}[[:digit:]]+', c('1.0', '1.0'), FALSE, FALSE, FALSE, FALSE, FALSE, FALSE); .Internal(grepl(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]]))");
    }

    @Test
    public void testgrepl3() {
        assertEval("argv <- list('\\n', structure('c(person(\\\'José\\\', \\\'Pinheiro\\\', role = \\\'aut\\\',\\n                    comment = \\\'S version\\\'),\\n             person(\\\'Douglas\\\', \\\'Bates\\\', role = \\\'aut\\\',\\n                    comment = \\\'up to 2007\\\'),\\n             person(\\\'Saikat\\\', \\\'DebRoy\\\', role = \\\'ctb\\\',\\n                    comment = \\\'up to 2002\\\'),\\n             person(\\\'Deepayan\\\', \\\'Sarkar\\\', role = \\\'ctb\\\',\\n                    comment = \\\'up to 2005\\\'),\\n\\t     person(\\\'R-core\\\', email = \\\'R-core@R-project.org\\\',\\n                    role = c(\\\'aut\\\', \\\'cre\\\')),\\n             person(\\\'EISPACK authors\\\', role = \\\'ctb\\\'))', .Names = 'Authors@R'), FALSE, FALSE, FALSE, TRUE, FALSE, FALSE); .Internal(grepl(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]]))");
    }

    @Test
    public void testgrepl4() {
        assertEval("argv <- list('([[:digit:]]+[.-]){1,}[[:digit:]]+', structure('7.3-26', .Names = 'Version'), FALSE, FALSE, FALSE, FALSE, FALSE, FALSE); .Internal(grepl(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]]))");
    }

    @Test
    public void testgrepl5() {
        assertEval("argv <- list('\\n', c('### Encoding: UTF-8', '', '### Name: text', '### Title: Add Text to a Plot', '### Aliases: text text.default', '### Keywords: aplot', '', '### ** Examples', '', 'plot(-1:1, -1:1, type = \\'n\\', xlab = \\'Re\\', ylab = \\'Im\\')', 'K <- 16; text(exp(1i * 2 * pi * (1:K) / K), col = 2)', '', '## The following two examples use latin1 characters: these may not', '## appear correctly (or be omitted entirely).', 'plot(1:10, 1:10, main = \\'text(...) examples\\\\n~~~~~~~~~~~~~~\\',', '     sub = \\'R is GNU ©, but not ® ...\\')', 'mtext(\\'«Latin-1 accented chars»: éè øØ å<Å æ<Æ\\', side = 3)', 'points(c(6,2), c(2,1), pch = 3, cex = 4, col = \\'red\\')', 'text(6, 2, \\'the text is CENTERED around (x,y) = (6,2) by default\\',', '     cex = .8)', 'text(2, 1, \\'or Left/Bottom - JUSTIFIED at (2,1) by \\'adj = c(0,0)\\'\\',', '     adj = c(0,0))', 'text(4, 9, expression(hat(beta) == (X^t * X)^{-1} * X^t * y))', 'text(4, 8.4, \\'expression(hat(beta) == (X^t * X)^{-1} * X^t * y)\\',', '     cex = .75)', 'text(4, 7, expression(bar(x) == sum(frac(x[i], n), i==1, n)))', '', '## Two more latin1 examples', 'text(5, 10.2,', '     \\'Le français, c\\'est façile: Règles, Liberté, Egalité, Fraternité...\\')', 'text(5, 9.8,', '     \\'Jetz no chli züritüütsch: (noch ein bißchen Zürcher deutsch)\\')', '', '', ''), FALSE, FALSE, FALSE, TRUE, FALSE, FALSE); .Internal(grepl(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]]))");
    }

    @Test
    public void testgrepl6() {
        assertEval("argv <- list('x', 'x', FALSE, FALSE, FALSE, TRUE, FALSE, FALSE); .Internal(grepl(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]]))");
    }

    @Test
    public void testgrepl7() {
        assertEval("argv <- list('^[[:space:]]*## No test:', 'Diagonal(3)', FALSE, FALSE, TRUE, FALSE, TRUE, FALSE); .Internal(grepl(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]]))");
    }

    @Test
    public void testgrepl8() {
        assertEval("argv <- list('\\\\\\\\keyword\\\\{\\\\s*internal\\\\s*\\\\}', c('\\\\inputencoding{utf8}', '\\\\HeaderA{condest}{Compute Approximate CONDition number and 1-Norm of (Large) Matrices}{condest}', '\\\\aliasA{onenormest}{condest}{onenormest}', '%', '\\\\begin{Description}\\\\relax', '``Estimate\\'\\', i.e. compute approximately the CONDition number of', 'a (potentially large, often sparse) matrix \\\\code{A}.', 'It works by apply a fast approximation of the 1-norm,', '\\\\code{norm(A,\\'1\\')}, through \\\\code{onenormest(.)}.', '\\\\end{Description}', '%', '\\\\begin{Usage}', '\\\\begin{verbatim}', 'condest(A, t = min(n, 5), normA = norm(A, \\'1\\'),', '        silent = FALSE, quiet = TRUE)', '', 'onenormest(A, t = min(n, 5), A.x, At.x, n,', '           silent = FALSE, quiet = silent,', '           iter.max = 10, eps = 4 * .Machine$double.eps)', '\\\\end{verbatim}', '\\\\end{Usage}', '%', '\\\\begin{Arguments}', '\\\\begin{ldescription}', '\\\\item[\\\\code{A}] a square matrix, optional for \\\\code{onenormest()}, where', 'instead of \\\\code{A}, \\\\code{A.x} and \\\\code{At.x} can be specified,', 'see there.', '\\\\item[\\\\code{t}] number of columns to use in the iterations.', '\\\\item[\\\\code{normA}] number; (an estimate of) the 1-norm of \\\\code{A}, by', 'default \\\\code{\\\\LinkA{norm}{norm}(A, \\'1\\')}; may be replaced by an estimate.', '\\\\item[\\\\code{silent}] logical indicating if warning and (by default)', 'convergence messages should be displayed.', '\\\\item[\\\\code{quiet}] logical indicating if convergence messages should be', 'displayed.', '\\\\item[\\\\code{A.x, At.x}] when \\\\code{A} is missing, these two must be given as', 'functions which compute \\\\code{A \\\\%\\\\% x}, or \\\\code{t(A) \\\\%\\\\% x},', 'respectively.', '\\\\item[\\\\code{n}] \\\\code{ == nrow(A)}, only needed when \\\\code{A} is not specified.', '\\\\item[\\\\code{iter.max}] maximal number of iterations for the 1-norm estimator.', '\\\\item[\\\\code{eps}] the relaive change that is deemed irrelevant.', '\\\\end{ldescription}', '\\\\end{Arguments}', '%', '\\\\begin{Value}', 'Both functions return a \\\\code{\\\\LinkA{list}{list}};', '\\\\code{onenormest()} with components,', '\\\\begin{ldescription}', '\\\\item[\\\\code{est}] a number \\\\eqn{> 0}{}, the estimated \\\\code{norm(A, \\'1\\')}.', '\\\\item[\\\\code{v}] the maximal \\\\eqn{A X}{} column.', '', '\\\\end{ldescription}', 'The function \\\\code{condest()} returns a list with components,', '\\\\begin{ldescription}', '\\\\item[\\\\code{est}] a number \\\\eqn{> 0}{}, the estimated condition number', '\\\\eqn{\\\\hat\\\\kappa}{}; when \\\\eqn{r :=}{}\\\\code{rcond(A)},', '\\\\eqn{1/\\\\hat\\\\kappa \\\\approx r}{}.', '\\\\item[\\\\code{v}] integer vector length \\\\code{n}, with an \\\\code{1} at the index', '\\\\code{j} with maximal column \\\\code{A[,j]} in \\\\eqn{A}{}.', '\\\\item[\\\\code{w}] numeric vector, the largest \\\\eqn{A x}{} found.', '\\\\item[\\\\code{iter}] the number of iterations used.', '\\\\end{ldescription}', '\\\\end{Value}', '%', '\\\\begin{Author}\\\\relax', 'This is based on octave\\'s \\\\code{condest()} and', '\\\\code{onenormest()} implementations with original author', 'Jason Riedy, U Berkeley; translation to \\\\R{} and', 'adaption by Martin Maechler.', '\\\\end{Author}', '%', '\\\\begin{References}\\\\relax', '', 'Nicholas J. Higham and Fran\\\\303\\\\247oise Tisseur (2000).', 'A Block Algorithm for Matrix 1-Norm Estimation, with an Application to 1-Norm', 'Pseudospectra.', '\\\\emph{SIAM J. Matrix Anal. Appl.} \\\\bold{21}, 4, 1185--1201.', '\\\\\\\\url{http://dx.doi.org/10.1137/S0895479899356080}', '', '', 'William W. Hager (1984).', 'Condition Estimates.', '\\\\emph{SIAM J. Sci. Stat. Comput.} \\\\bold{5}, 311--316.', '\\\\end{References}', '%', '\\\\begin{SeeAlso}\\\\relax', '\\\\code{\\\\LinkA{norm}{norm}}, \\\\code{\\\\LinkA{rcond}{rcond}}.', '\\\\end{SeeAlso}', '%', '\\\\begin{Examples}', '\\\\begin{ExampleCode}', 'data(KNex)', 'mtm <- with(KNex, crossprod(mm))', 'system.time(ce <- condest(mtm))', '## reciprocal', '1 / ce$est', 'system.time(rc <- rcond(mtm)) # takes ca  3 x  longer', 'rc', 'all.equal(rc, 1/ce$est) # TRUE -- the approxmation was good', '\\\\end{ExampleCode}', '\\\\end{Examples}'), FALSE, FALSE, TRUE, FALSE, FALSE, FALSE); .Internal(grepl(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]]))");
    }

    @Test
    public void testgrepl9() {
        assertEval("argv <- list('\\n', '\\nqr(x, ...)\\nqrR(qr, complete=FALSE, backPermute=TRUE)\\n', FALSE, FALSE, FALSE, TRUE, FALSE, FALSE); .Internal(grepl(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]]))");
    }

    @Test
    public void testgrepl10() {
        assertEval("argv <- list('{refObject}', c('\\\\section{Extends}{', 'Class \\\\code{\\\'\\\\linkS4class{refClassA}\\\'}, directly.', 'Class \\\\code{\\\'\\\\linkS4class{envRefClass}\\\'}, by class \\\'refClassA\\\', distance 2.', 'Class \\\\code{\\\'\\\\linkS4class{.environment}\\\'}, by class \\\'refClassA\\\', distance 3.', 'Class \\\\code{\\\'\\\\linkS4class{refClass}\\\'}, by class \\\'refClassA\\\', distance 3.', 'Class \\\\code{\\\'\\\\linkS4class{environment}\\\'}, by class \\\'refClassA\\\', distance 4, with explicit coerce.', 'Class \\\\code{\\\'\\\\linkS4class{refObject}\\\'}, by class \\\'refClassA\\\', distance 4.', '}'), FALSE, FALSE, FALSE, TRUE, FALSE, FALSE); .Internal(grepl(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]]))");
    }

    @Test
    public void testgrepl11() {
        assertEval("argv <- list('^prepare_Rd', structure(character(0), class = 'checkRd'), FALSE, FALSE, FALSE, FALSE, FALSE, FALSE); .Internal(grepl(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]]))");
    }

    @Test
    public void testgrepl12() {
        assertEval("argv <- structure(list(pattern = 'length', x = 'Lengths: 0, 1',     ignore.case = TRUE), .Names = c('pattern', 'x', 'ignore.case'));do.call('grepl', argv)");
    }

    @Test
    public void testGrep() {
        assertEval("{ txt<-c(\"arm\",\"foot\",\"lefroo\", \"bafoobar\"); grepl(\"foo\", txt) }");
    }
}
