/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.testrgen;

import org.junit.*;

import com.oracle.truffle.r.test.*;

// Checkstyle: stop line length check
public class TestrGenBuiltinclassassign extends TestBase {

    @Test
    public void testclassassign1() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(function (x, mode = 'any') .Internal(as.vector(x, mode)), target = structure('ANY', class = structure('signature', package = 'methods'), .Names = 'x', package = 'methods'), defined = structure('ANY', class = structure('signature', package = 'methods'), .Names = 'x', package = 'methods'), generic = character(0), class = structure('MethodDefinition', package = 'methods')), value = structure('MethodDefinition', package = 'methods'));`class<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testclassassign2() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(list(c0 = structure(integer(0), .Label = character(0), class = 'factor')), .Names = 'c0', row.names = character(0), class = structure('integer(0)', .Names = 'c0')), structure(list(c0 = structure(integer(0), .Label = character(0), class = 'factor')), .Names = 'c0', row.names = character(0), class = structure('integer(0)', .Names = 'c0')));`class<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testclassassign3() {
        assertEval("argv <- list(character(0), character(0));`class<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testclassassign4() {
        assertEval("argv <- list(structure(c(8, 10, 12, 14, 8, 10, 12, 14, 8, 10, 12, 14, 8, 10, 12, 14, 8, 10, 12, 14, 8, 10, 12, 14, 8, 10, 12, 14, 8, 10, 12, 14, 8, 10, 12, 14, 8, 10, 12, 14, 8, 10, 12, 14, 8, 10, 12, 14, 8, 10, 12, 14, 8, 10, 12, 14, 8, 10, 12, 14, 8, 10, 12, 14, 8, 10, 12, 14, 8, 10, 12, 14, 8, 10, 12, 14, 8, 10, 12, 14, 8, 10, 12, 14, 8, 10, 12, 14, 8, 10, 12, 14, 8, 10, 12, 14, 8, 10, 12, 14, 8, 10, 12, 14, 8, 10, 12, 14), class = 'anyC'), value = 'anyC');`class<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testclassassign5() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(character(0), .Names = character(0), package = character(0), class = structure('signature', package = 'methods')), value = structure('signature', package = 'methods'));`class<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testclassassign6() {
        assertEval("argv <- list(structure(list(par = 5.5, loglik = 0.970661978016996), .Names = c('par', 'loglik'), class = 'pfit'), value = 'pfit');`class<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testclassassign7() {
        assertEval("argv <- list(structure(FALSE, class = 'FALSE'), FALSE);`class<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testclassassign8() {
        assertEval("argv <- list(1:3, value = 'numeric');`class<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testclassassign9() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(c(1, 0, 0, 0, 1, 0, 0, 0, 1), .Dim = c(3L, 3L), class = structure('mmat2', package = '.GlobalEnv')), value = structure('mmat2', package = '.GlobalEnv'));`class<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testclassassign10() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(c('o', 'p', 'v', 'i', 'r', 'w', 'b', 'm', 'f', 's'), date = structure(1224086400, class = c('POSIXct', 'POSIXt'), tzone = ''), class = 'stamped'), value = 'stamped');`class<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testclassassign11() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(3.14159265358979, class = structure('3.14159265358979', class = 'testit')), structure(3.14159265358979, class = structure('3.14159265358979', class = 'testit')));`class<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testclassassign12() {
        assertEval("argv <- list(structure(1, class = 'bar'), value = 'bar');`class<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testclassassign13() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(function (qr, y, k = qr$rank) standardGeneric('qr.fitted'), target = structure('ANY', class = structure('signature', package = 'methods'), .Names = 'qr', package = 'methods'), defined = structure('ANY', class = structure('signature', package = 'methods'), .Names = 'qr', package = 'methods'), generic = character(0), class = structure('MethodDefinition', package = 'methods')), value = structure('MethodDefinition', package = 'methods'));`class<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testclassassign14() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(function (x = 1, nrow, ncol) standardGeneric('diag'), target = structure('ANY', class = structure('signature', package = 'methods'), .Names = 'x', package = 'methods'), defined = structure('ANY', class = structure('signature', package = 'methods'), .Names = 'x', package = 'methods'), generic = character(0), class = structure('MethodDefinition', package = 'methods')), value = structure('MethodDefinition', package = 'methods'));`class<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testclassassign15() {
        assertEval("argv <- list(structure(1:6, class = 'A'), value = 'A');`class<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testclassassign16() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(function (x, y, ...) standardGeneric('plot'), target = structure('ANY', class = structure('signature', package = 'methods'), .Names = 'x', package = 'methods'), defined = structure('ANY', class = structure('signature', package = 'methods'), .Names = 'x', package = 'methods'), generic = character(0), class = structure('MethodDefinition', package = 'methods')), value = structure('MethodDefinition', package = 'methods'));`class<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testclassassign17() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(function (x, logarithm = TRUE, ...) UseMethod('determinant'), target = structure('ANY', class = structure('signature', package = 'methods'), .Names = 'x', package = 'methods'), defined = structure('ANY', class = structure('signature', package = 'methods'), .Names = 'x', package = 'methods'), generic = character(0), class = structure('MethodDefinition', package = 'methods')), value = structure('MethodDefinition', package = 'methods'));`class<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testclassassign18() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(function (x, y = NULL) .Internal(crossprod(x, y)), target = structure('ANY', class = structure('signature', package = 'methods'), .Names = 'x', package = 'methods'), defined = structure('ANY', class = structure('signature', package = 'methods'), .Names = 'x', package = 'methods'), generic = character(0), class = structure('MethodDefinition', package = 'methods')), value = structure('MethodDefinition', package = 'methods'));`class<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testclassassign19() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(function (obj, force = FALSE) standardGeneric('unname'), target = structure('ANY', class = structure('signature', package = 'methods'), .Names = 'obj', package = 'methods'), defined = structure('ANY', class = structure('signature', package = 'methods'), .Names = 'obj', package = 'methods'), generic = character(0), class = structure('MethodDefinition', package = 'methods')), value = structure('MethodDefinition', package = 'methods'));`class<-`(argv[[1]],argv[[2]]);");
    }
}
