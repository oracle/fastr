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
public class TestBuiltin_ascharacter extends TestBase {

    @Test
    public void testascharacter1() {
        assertEval("argv <- list('bessel_y(2,nu=181.2): precision lost in result');as.character(argv[[1]]);");
    }

    @Test
    public void testascharacter2() {
        assertEval("argv <- list(structure(c(12784, 12874, 12965, 13057, 13149, 13239, 13330, 13422, 13514, 13604, 13695, 13787, 13879, 13970, 14061, 14153, 14245), class = 'Date'));as.character(argv[[1]]);");
    }

    @Test
    public void testascharacter3() {
        assertEval("argv <- list(c(2L, 1L, NA));as.character(argv[[1]]);");
    }

    @Test
    public void testascharacter4() {
        assertEval("argv <- list(c('### Encoding: UTF-8', '', '### Name: text', '### Title: Add Text to a Plot', '### Aliases: text text.default', '### Keywords: aplot', '', '### ** Examples', '', 'plot(-1:1, -1:1, type = \\\'n\\\', xlab = \\\'Re\\\', ylab = \\\'Im\\\')', 'K <- 16; text(exp(1i * 2 * pi * (1:K) / K), col = 2)', '', '## The following two examples use latin1 characters: these may not', '## appear correctly (or be omitted entirely).', 'plot(1:10, 1:10, main = \\\'text(...) examples\\\\n~~~~~~~~~~~~~~\\\',', '     sub = \\\'R is GNU ©, but not ® ...\\\')', 'mtext(\\\'«Latin-1 accented chars»: éè øØ å<Å æ<Æ\\\', side = 3)', 'points(c(6,2), c(2,1), pch = 3, cex = 4, col = \\\'red\\\')', 'text(6, 2, \\\'the text is CENTERED around (x,y) = (6,2) by default\\\',', '     cex = .8)', 'text(2, 1, \\\'or Left/Bottom - JUSTIFIED at (2,1) by \\\'adj = c(0,0)\\\'\\\',', '     adj = c(0,0))', 'text(4, 9, expression(hat(beta) == (X^t * X)^{-1} * X^t * y))', 'text(4, 8.4, \\\'expression(hat(beta) == (X^t * X)^{-1} * X^t * y)\\\',', '     cex = .75)', 'text(4, 7, expression(bar(x) == sum(frac(x[i], n), i==1, n)))', '', '## Two more latin1 examples', 'text(5, 10.2,', '     \\\'Le français, c\\\'est façile: Règles, Liberté, Egalité, Fraternité...\\\')', 'text(5, 9.8,', '     \\\'Jetz no chli züritüütsch: (noch ein bißchen Zürcher deutsch)\\\')', '', '', ''));as.character(argv[[1]]);");
    }

    @Test
    public void testascharacter5() {
        assertEval("argv <- list(structure(1395082040.29392, class = c('POSIXct', 'POSIXt')));as.character(argv[[1]]);");
    }

    @Test
    public void testascharacter6() {
        assertEval("argv <- list(structure(2:3, .Label = c('C', 'A', 'B'), class = 'factor'));as.character(argv[[1]]);");
    }

    @Test
    public void testascharacter7() {
        assertEval("argv <- list(structure(1:255, class = 'octmode'));as.character(argv[[1]]);");
    }

    @Test
    public void testascharacter8() {
        assertEval("argv <- list(c(Inf, -Inf, NaN, NA));as.character(argv[[1]]);");
    }

    @Test
    public void testascharacter9() {
        assertEval("argv <- list(c(1, 2, NA, 2));as.character(argv[[1]]);");
    }

    @Test
    public void testascharacter10() {
        assertEval("argv <- list(structure(character(0), package = character(0), class = structure('ObjectsWithPackage', package = 'methods')));as.character(argv[[1]]);");
    }

    @Test
    public void testascharacter11() {
        assertEval("argv <- list(c(FALSE, TRUE));as.character(argv[[1]]);");
    }

    @Test
    public void testascharacter12() {
        assertEval("argv <- list(structure(1:4, .Dim = c(1L, 4L)));as.character(argv[[1]]);");
    }

    @Test
    public void testascharacter13() {
        assertEval("argv <- list(structure('1', .Tsp = c(1, 1, 1), class = 'ts'));as.character(argv[[1]]);");
    }

    @Test
    public void testascharacter14() {
        assertEval("argv <- list(structure('Estimates a probability density function,  \\n', Rd_tag = 'TEXT'));as.character(argv[[1]]);");
    }

    @Test
    public void testascharacter15() {
        assertEval("argv <- list(c(2L, 1L, 3L, NA, 4L));as.character(argv[[1]]);");
    }

    @Test
    public void testascharacter16() {
        assertEval("argv <- list(structure(-841, class = 'Date'));as.character(argv[[1]]);");
    }

    @Test
    public void testascharacter17() {
        assertEval("argv <- list(structure(list(list(structure('x', Rd_tag = 'TEXT')), list(structure('an R object representing a hierarchical clustering.\\n', Rd_tag = 'TEXT'), structure('    For the default method, an object of class ', Rd_tag = 'TEXT'), structure(list(structure('\\\'', Rd_tag = 'RCODE'), structure(list(structure('hclust', Rd_tag = 'TEXT')), Rd_tag = '\\\\link'), structure('\\\'', Rd_tag = 'RCODE')), Rd_tag = '\\\\code'), structure(' or\\n', Rd_tag = 'TEXT'), structure('    with a method for ', Rd_tag = 'TEXT'), structure(list(    structure(list(structure('as.hclust', Rd_tag = 'TEXT')), Rd_tag = '\\\\link'), structure('()', Rd_tag = 'RCODE')), Rd_tag = '\\\\code'), structure(' such as\\n', Rd_tag = 'TEXT'), structure('    ', Rd_tag = 'TEXT'), structure(list(structure('\\\'', Rd_tag = 'RCODE'), structure(list(structure('agnes', Rd_tag = 'TEXT')), Rd_tag = '\\\\link', Rd_option = structure('cluster', Rd_tag = 'TEXT')), structure('\\\'', Rd_tag = 'RCODE')), Rd_tag = '\\\\code'), structure(' in package ', Rd_tag = 'TEXT'), structure(c('\\\\href{http://CRAN.R-project.org/package=#1}{\\\\pkg{#1}}', 'cluster'), Rd_tag = 'USERMACRO'), structure(list(list(structure('http://CRAN.R-project.org/package=cluster', Rd_tag = 'VERB')), list(structure(list(structure('cluster', Rd_tag = 'TEXT')), Rd_tag = '\\\\pkg'))), Rd_tag = '\\\\href'), structure('.', Rd_tag = 'TEXT'))), Rd_tag = '\\\\item'));as.character(argv[[1]]);");
    }

    @Test
    public void testascharacter18() {
        assertEval("argv <- list(list(epsilon = 1e-08, maxit = 25, trace = FALSE));as.character(argv[[1]]);");
    }

    @Test
    public void testascharacter19() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(list(structure(list(given = c('George', 'E.', 'P.'), family = 'Box', role = NULL, email = NULL, comment = NULL), .Names = c('given', 'family', 'role', 'email', 'comment')), structure(list(given = c('David', 'R.'), family = 'Cox', role = NULL, email = NULL, comment = NULL), .Names = c('given', 'family', 'role', 'email', 'comment'))), class = 'person'));as.character(argv[[1]]);");
    }

    @Test
    public void testascharacter20() {
        assertEval("argv <- list(structure(list(structure('\\n', Rd_tag = 'TEXT'), structure('    ', Rd_tag = 'TEXT'), structure(list(list(structure('coerce', Rd_tag = 'TEXT')), list(structure(list(structure('signature(from = \\\'dgCMatrix\\\', to =\\n', Rd_tag = 'RCODE'), structure('\\t\\\'ngCMatrix\\\')', Rd_tag = 'RCODE')), Rd_tag = '\\\\code'), structure(', and many similar ones; typically you should\\n', Rd_tag = 'TEXT'), structure('      coerce to ', Rd_tag = 'TEXT'), structure(list(structure('\\\'nsparseMatrix\\\'', Rd_tag = 'RCODE')), Rd_tag = '\\\\code'),     structure(' (or ', Rd_tag = 'TEXT'), structure(list(structure('\\\'nMatrix\\\'', Rd_tag = 'RCODE')), Rd_tag = '\\\\code'), structure(').  Note that\\n', Rd_tag = 'TEXT'), structure('      coercion to a sparse pattern matrix records all the potential\\n', Rd_tag = 'TEXT'), structure('      non-zero entries, i.e., explicit (', Rd_tag = 'TEXT'), structure(list(structure('non-structural', Rd_tag = 'TEXT')), Rd_tag = '\\\\dQuote'), structure(') zeroes\\n', Rd_tag = 'TEXT'), structure('      are coerced to ', Rd_tag = 'TEXT'),     structure(list(structure('TRUE', Rd_tag = 'RCODE')), Rd_tag = '\\\\code'), structure(', not ', Rd_tag = 'TEXT'), structure(list(structure('FALSE', Rd_tag = 'RCODE')), Rd_tag = '\\\\code'), structure(', see the example.\\n', Rd_tag = 'TEXT'), structure('    ', Rd_tag = 'TEXT'))), Rd_tag = '\\\\item'), structure('\\n', Rd_tag = 'TEXT'), structure('    ', Rd_tag = 'TEXT'), structure(list(list(structure('t', Rd_tag = 'TEXT')), list(structure(list(structure('signature(x = \\\'ngCMatrix\\\')', Rd_tag = 'RCODE')), Rd_tag = '\\\\code'),     structure(': returns the transpose\\n', Rd_tag = 'TEXT'), structure('      of ', Rd_tag = 'TEXT'), structure(list(structure('x', Rd_tag = 'RCODE')), Rd_tag = '\\\\code'))), Rd_tag = '\\\\item'), structure('\\n', Rd_tag = 'TEXT'), structure('\\n', Rd_tag = 'TEXT'), structure('    ', Rd_tag = 'TEXT'), structure(list(list(structure('which', Rd_tag = 'TEXT')), list(structure(list(structure('signature(x = \\\'lsparseMatrix\\\')', Rd_tag = 'RCODE')), Rd_tag = '\\\\code'), structure(', semantically\\n', Rd_tag = 'TEXT'),     structure('      equivalent to ', Rd_tag = 'TEXT'), structure(list(structure('base', Rd_tag = 'TEXT')), Rd_tag = '\\\\pkg'), structure(' function ', Rd_tag = 'TEXT'), structure(list(structure(list(structure('which', Rd_tag = 'TEXT')), Rd_tag = '\\\\link'), structure('(x, arr.ind)', Rd_tag = 'RCODE')), Rd_tag = '\\\\code'), structure(';\\n', Rd_tag = 'TEXT'), structure('      for details, see the ', Rd_tag = 'TEXT'), structure(list(structure(list(structure('lMatrix', Rd_tag = 'TEXT')), Rd_tag = '\\\\linkS4class')), Rd_tag = '\\\\code'),     structure(' class documentation.', Rd_tag = 'TEXT'))), Rd_tag = '\\\\item'), structure('\\n', Rd_tag = 'TEXT'), structure('  ', Rd_tag = 'TEXT')), Rd_tag = '\\\\describe'));as.character(argv[[1]]);");
    }

    @Test
    public void testascharacter21() {
        assertEval("argv <- list(c(3, 3, NA, NA, NA, NA, 4, 3, 4, NA, NA, 2, 3, 3, NA, NA, 2, 4, NA, 2, 5, 2, 2, 4, 3, NA, 2, NA, 3, 3));as.character(argv[[1]]);");
    }

    @Test
    public void testascharacter22() {
        assertEval("argv <- list(structure(list(structure('\\n', Rd_tag = 'TEXT'), structure('    ', Rd_tag = 'TEXT'), structure(list(list(structure(list(structure('languageEl', Rd_tag = 'RCODE')), Rd_tag = '\\\\code'), structure(':', Rd_tag = 'TEXT')), list(structure('\\n', Rd_tag = 'TEXT'), structure('      extract an element of a language object, consistently for\\n', Rd_tag = 'TEXT'), structure('      different kinds of objects.\\n', Rd_tag = 'TEXT'), structure('\\n', Rd_tag = 'TEXT'), structure('      The 1st., etc. elements of a function are the corresponding formal\\n', Rd_tag = 'TEXT'),     structure('      arguments, with the default expression if any as value.\\n', Rd_tag = 'TEXT'), structure('\\n', Rd_tag = 'TEXT'), structure('      The first element of a call is the name or the function object\\n', Rd_tag = 'TEXT'), structure('      being called.\\n', Rd_tag = 'TEXT'), structure('\\n', Rd_tag = 'TEXT'), structure('      The 2nd, 3rd, etc. elements are the 1st, 2nd, etc. arguments\\n', Rd_tag = 'TEXT'), structure('      expressions.  Note that the form of the extracted name is\\n', Rd_tag = 'TEXT'),     structure('      different for R and S-Plus.  When the name (the first element) of\\n', Rd_tag = 'TEXT'), structure('      a call is replaced, the languageEl replacement function coerces a\\n', Rd_tag = 'TEXT'), structure('      character string to the internal form for each system.\\n', Rd_tag = 'TEXT'), structure('\\n', Rd_tag = 'TEXT'), structure('      The 1st, 2nd, 3rd elements of an ', Rd_tag = 'TEXT'), structure(list(structure('if', Rd_tag = 'RCODE')), Rd_tag = '\\\\code'), structure(' expression are the\\n', Rd_tag = 'TEXT'),     structure('      test, first, and second branch.\\n', Rd_tag = 'TEXT'), structure('\\n', Rd_tag = 'TEXT'), structure('      The 1st element of a ', Rd_tag = 'TEXT'), structure(list(structure('for', Rd_tag = 'RCODE')), Rd_tag = '\\\\code'), structure(' object is the name (symbol) being\\n', Rd_tag = 'TEXT'), structure('      used in the loop, the second is the expression for the range of\\n', Rd_tag = 'TEXT'), structure('      the loop, the third is the body of the loop.\\n', Rd_tag = 'TEXT'), structure('\\n', Rd_tag = 'TEXT'),     structure('      The first element of a ', Rd_tag = 'TEXT'), structure(list(structure('while', Rd_tag = 'RCODE')), Rd_tag = '\\\\code'), structure(' object is the loop test, and\\n', Rd_tag = 'TEXT'), structure('      the second the body of the loop.\\n', Rd_tag = 'TEXT'), structure('    ', Rd_tag = 'TEXT'))), Rd_tag = '\\\\item'), structure('\\n', Rd_tag = 'TEXT'), structure('\\n', Rd_tag = 'TEXT'), structure('    ', Rd_tag = 'TEXT'), structure(list(list(structure(list(structure('isGrammarSymbol', Rd_tag = 'RCODE')), Rd_tag = '\\\\code'),     structure(':', Rd_tag = 'TEXT')), list(structure('\\n', Rd_tag = 'TEXT'), structure('      Checks whether the symbol is part of the grammar.\\n', Rd_tag = 'TEXT'), structure('      Don\\'t use this function directly.\\n', Rd_tag = 'TEXT'), structure('    ', Rd_tag = 'TEXT'))), Rd_tag = '\\\\item'), structure('\\n', Rd_tag = 'TEXT'), structure('  ', Rd_tag = 'TEXT')), Rd_tag = '\\\\describe'));as.character(argv[[1]]);");
    }

    @Test
    public void testascharacter23() {
        assertEval("argv <- list(structure(c(1L, 2L, 2L, 3L, 3L, 1L, NA), .Label = c('Australia', 'UK', 'US'), class = 'factor'));as.character(argv[[1]]);");
    }

    @Test
    public void testascharacter24() {
        assertEval("argv <- list(structure(list(4L), class = c('package_version', 'numeric_version')));as.character(argv[[1]]);");
    }

    @Test
    public void testascharacter25() {
        assertEval("argv <- list(c(-Inf, NaN, Inf));as.character(argv[[1]]);");
    }

    @Test
    public void testascharacter26() {
        assertEval("argv <- list(FALSE, useSource = TRUE);as.character(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testascharacter27() {
        assertEval("argv <- list(structure(c(1L, 1L, 1L, 1L, 1L, 1L, 2L, 1L, 1L, 1L, 2L, 2L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 2L, 1L, 1L, 1L, 1L, NA, 1L, 1L, 1L, 2L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 2L, 1L, 1L, 1L, 2L, 1L, 1L, 1L, 2L, 2L, 1L, 1L, 2L, 1L, 2L, 1L, 1L, 1L, 1L, 1L, 1L, NA, 1L, 1L, 1L, 2L, 2L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 2L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 2L, 1L, 1L, 1L, NA, 1L, 1L, 2L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 2L, 1L, 1L, 1L, 1L, 2L, NA, 1L, 1L, 1L, 2L, 1L, 1L, 1L, 2L, 1L, 1L, 1L, 1L, 1L, 1L), .Label = c('0', '1'), class = 'factor'));as.character(argv[[1]]);");
    }

    @Test
    public void testascharacter28() {
        assertEval("argv <- list(structure(c(11323, 11330, 11337, 11344, 11351, 11358, 11365, 11372, 11379, 11386), class = 'Date'));as.character(argv[[1]]);");
    }

    @Test
    public void testascharacter29() {
        assertEval("argv <- list(structure(c(1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L), .Dim = c(10L, 2L)));as.character(argv[[1]]);");
    }

    @Test
    public void testascharacter30() {
        assertEval("argv <- list(c(FALSE, FALSE, FALSE, FALSE, NA, FALSE, FALSE, FALSE, FALSE, NA, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, NA, NA, NA, FALSE, FALSE, TRUE, FALSE, NA, NA, NA, NA, NA, NA, FALSE, NA, FALSE, FALSE, NA, NA, FALSE, NA, NA, FALSE, FALSE, FALSE, FALSE, FALSE, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, TRUE, FALSE, FALSE, NA, FALSE, FALSE, FALSE, TRUE, TRUE, TRUE, NA, FALSE, FALSE, NA, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, NA, NA, FALSE, TRUE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, TRUE, TRUE, NA, NA, FALSE, FALSE, FALSE, NA, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, NA, FALSE, TRUE, FALSE, NA, FALSE, TRUE, TRUE, TRUE, TRUE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, NA, FALSE, FALSE, FALSE));as.character(argv[[1]]);");
    }

    @Test
    public void testascharacter31() {
        assertEval("argv <- list(structure(c(1338523200, 1338609600, 1338696000, 1338782400, 1338868800, 1338955200, 1339041600), class = c('POSIXct', 'POSIXt'), tzone = ''));as.character(argv[[1]]);");
    }

    @Test
    public void testascharacter32() {
        assertEval(Ignored.Unknown, "argv <- list(structure(1:4, class = 'roman'));as.character(argv[[1]]);");
    }

    @Test
    public void testascharacter33() {
        assertEval("argv <- list(logical(0));as.character(argv[[1]]);");
    }

    @Test
    public void testascharacter34() {
        assertEval("argv <- list(structure(c(1104537600, 1107216000, 1109635200, 1112313600, 1114905600, 1117584000, 1120176000, 1122854400, 1125532800, 1128124800, 1130803200, 1133395200, 1136073600, 1138752000, 1141171200, 1143849600, 1146441600, 1149120000, 1151712000, 1154390400, 1157068800, 1159660800, 1162339200, 1164931200, 1167609600, 1170288000, 1172707200, 1175385600, 1177977600, 1180656000, 1183248000, 1185926400, 1188604800, 1191196800, 1193875200, 1196467200, 1199145600, 1201824000, 1204329600, 1207008000, 1209600000, 1212278400, 1214870400, 1217548800, 1220227200, 1222819200, 1225497600, 1228089600, 1230768000), class = c('POSIXct', 'POSIXt'), tzone = 'UTC'));as.character(argv[[1]]);");
    }

    @Test
    public void testascharacter35() {
        assertEval("argv <- list(c(-4, 4, 3.99, -1, -3.01));as.character(argv[[1]]);");
    }

    @Test
    public void testascharacter36() {
        assertEval("argv <- list(list(exit.code = 0L, send = NULL));as.character(argv[[1]]);");
    }

    @Test
    public void testascharacter37() {
        assertEval("argv <- list(c(34L, -45L));as.character(argv[[1]]);");
    }

    @Test
    public void testascharacter38() {
        assertEval("argv <- list(structure(c(978307200, 978912000, 979516800, 980121600, 980726400, 981331200, 981936000, 982540800, 983145600, 983750400), class = c('POSIXct', 'POSIXt'), tzone = 'GMT'));as.character(argv[[1]]);");
    }

    @Test
    public void testascharacter39() {
        assertEval("argv <- list(structure(list(c0 = structure(integer(0), .Label = character(0), class = 'factor')), .Names = 'c0', row.names = character(0), class = 'data.frame'), structure(list(c0 = structure(integer(0), .Label = character(0), class = 'factor')), .Names = 'c0', row.names = character(0), class = 'data.frame'));as.character(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testascharacter40() {
        assertEval("argv <- list(structure(list(), class = 'numeric_version'));as.character(argv[[1]]);");
    }

    @Test
    public void testascharacter41() {
        assertEval("argv <- list(structure(list(c0 = structure(character(0), class = 'AsIs')), .Names = 'c0', row.names = character(0), class = 'data.frame'));as.character(argv[[1]]);");
    }

    @Test
    public void testascharacter42() {
        assertEval("argv <- list(structure(c(12784, 13879), class = 'Date'));as.character(argv[[1]]);");
    }

    @Test
    public void testascharacter43() {
        assertEval("argv <- list(NaN);as.character(argv[[1]]);");
    }

    @Test
    public void testascharacter44() {
        assertEval("argv <- list(Inf);as.character(argv[[1]]);");
    }

    @Test
    public void testascharacter45() {
        assertEval("argv <- list(c('class', 'names', 'package'));as.character(argv[[1]]);");
    }

    @Test
    public void testascharacter46() {
        assertEval("argv <- list(c(59.5, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59.5));as.character(argv[[1]]);");
    }

    @Test
    public void testAsCharacter() {
        assertEval("{ as.character(1) }");
        assertEval("{ as.character(1L) }");
        assertEval("{ as.character(TRUE) }");
        assertEval("{ as.character(1:3) }");
        assertEval("{ as.character(NULL) }");

        assertEval("{ as.character(list(1,2,3)) }");
        assertEval("{ as.character(list(c(\"hello\", \"hi\"))) }");
        assertEval("{ as.character(list(list(c(\"hello\", \"hi\")))) }");
        assertEval("{ as.character(list(c(2L, 3L))) }");
        assertEval("{ as.character(list(c(2L, 3L, 5L))) }");

        assertEval("{ x<-as.character(Sys.time()) }");
        assertEval("{ f<-function(x) { sys.call() }; as.character(f(7)) }");

        assertEval("{ f1<-function() 7; f2<-function(x) { sys.call() }; as.character(f2(f1())) }");
        assertEval("{ f1<-function(x) 7; f2<-function(y) { sys.call() }; as.character(f2(f1(42))) }");
    }
}
