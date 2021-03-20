/*
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * Copyright (c) 2012-2014, Purdue University
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check
public class TestBuiltin_classassign extends TestBase {

    @Test
    public void testclassassign1() {
        // Quotes vs. apostrophes:
        // GnuR: function (x, mode = "any")
        // FastR: function (x, mode = 'any')
        assertEval(Ignored.OutputFormatting,
                        "argv <- list(structure(function (x, mode = 'any') .Internal(as.vector(x, mode)), target = structure('ANY', class = structure('signature', package = 'methods'), .Names = 'x', package = 'methods'), defined = structure('ANY', class = structure('signature', package = 'methods'), .Names = 'x', package = 'methods'), generic = character(0), class = structure('MethodDefinition', package = 'methods')), value = structure('MethodDefinition', package = 'methods'));`class<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testclassassign2() {
        // GnuR: An extra "c0" in output
        assertEval(Ignored.OutputFormatting,
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
        // FastR does not output the following:
        // attr(,"class")attr(,"package")
        // [1] "methods"
        assertEval(Ignored.OutputFormatting,
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
        // FastR does not output the following:
        // attr(,"class")attr(,"package")
        // [1] ".GlobalEnv"
        assertEval(Ignored.OutputFormatting,
                        "argv <- list(structure(c(1, 0, 0, 0, 1, 0, 0, 0, 1), .Dim = c(3L, 3L), class = structure('mmat2', package = '.GlobalEnv')), value = structure('mmat2', package = '.GlobalEnv'));`class<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testclassassign10() {
        // FastR: extra newline after date
        assertEval(Output.IgnoreWhitespace,
                        "argv <- list(structure(c('o', 'p', 'v', 'i', 'r', 'w', 'b', 'm', 'f', 's'), date = structure(1224086400, class = c('POSIXct', 'POSIXt'), tzone = ''), class = 'stamped'), value = 'stamped');`class<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testclassassign11() {
        // GnuR: extra output of 3.141... and attr(,"class")
        assertEval(Ignored.OutputFormatting,
                        "argv <- list(structure(3.14159265358979, class = structure('3.14159265358979', class = 'testit')), structure(3.14159265358979, class = structure('3.14159265358979', class = 'testit')));`class<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testclassassign12() {
        assertEval("argv <- list(structure(1, class = 'bar'), value = 'bar');`class<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testclassassign13() {
        // Quotes vs. apostrophes in output
        // Expected: "qr.fitted")attr(,"target")qr"ANY"attr(,...
        // FastR: 'qr.fitted')attr(,"target")qr"ANY"attr(,...
        assertEval(Ignored.OutputFormatting,
                        "argv <- list(structure(function (qr, y, k = qr$rank) standardGeneric('qr.fitted'), target = structure('ANY', class = structure('signature', package = 'methods'), .Names = 'qr', package = 'methods'), defined = structure('ANY', class = structure('signature', package = 'methods'), .Names = 'qr', package = 'methods'), generic = character(0), class = structure('MethodDefinition', package = 'methods')), value = structure('MethodDefinition', package = 'methods'));`class<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testclassassign14() {
        // Quotes vs. apostrophes in output
        assertEval(Ignored.OutputFormatting,
                        "argv <- list(structure(function (x = 1, nrow, ncol) standardGeneric('diag'), target = structure('ANY', class = structure('signature', package = 'methods'), .Names = 'x', package = 'methods'), defined = structure('ANY', class = structure('signature', package = 'methods'), .Names = 'x', package = 'methods'), generic = character(0), class = structure('MethodDefinition', package = 'methods')), value = structure('MethodDefinition', package = 'methods'));`class<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testclassassign15() {
        assertEval("argv <- list(structure(1:6, class = 'A'), value = 'A');`class<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testclassassign16() {
        // Quotes vs. apostrophes in output
        assertEval(Ignored.OutputFormatting,
                        "argv <- list(structure(function (x, y, ...) standardGeneric('plot'), target = structure('ANY', class = structure('signature', package = 'methods'), .Names = 'x', package = 'methods'), defined = structure('ANY', class = structure('signature', package = 'methods'), .Names = 'x', package = 'methods'), generic = character(0), class = structure('MethodDefinition', package = 'methods')), value = structure('MethodDefinition', package = 'methods'));`class<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testclassassign17() {
        // Quotes vs. apostrophes in output
        assertEval(Ignored.OutputFormatting,
                        "argv <- list(structure(function (x, logarithm = TRUE, ...) UseMethod('determinant'), target = structure('ANY', class = structure('signature', package = 'methods'), .Names = 'x', package = 'methods'), defined = structure('ANY', class = structure('signature', package = 'methods'), .Names = 'x', package = 'methods'), generic = character(0), class = structure('MethodDefinition', package = 'methods')), value = structure('MethodDefinition', package = 'methods'));`class<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testclassassign18() {
        assertEval(Output.IgnoreWhitespace,
                        "argv <- list(structure(function (x, y = NULL) .Internal(crossprod(x, y)), target = structure('ANY', class = structure('signature', package = 'methods'), .Names = 'x', package = 'methods'), defined = structure('ANY', class = structure('signature', package = 'methods'), .Names = 'x', package = 'methods'), generic = character(0), class = structure('MethodDefinition', package = 'methods')), value = structure('MethodDefinition', package = 'methods'));`class<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testclassassign19() {
        // Quotes vs. apostrophes in output
        assertEval(Ignored.OutputFormatting,
                        "argv <- list(structure(function (obj, force = FALSE) standardGeneric('unname'), target = structure('ANY', class = structure('signature', package = 'methods'), .Names = 'obj', package = 'methods'), defined = structure('ANY', class = structure('signature', package = 'methods'), .Names = 'obj', package = 'methods'), generic = character(0), class = structure('MethodDefinition', package = 'methods')), value = structure('MethodDefinition', package = 'methods'));`class<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testUpdateClass() {
        assertEval(Output.MayIgnoreErrorContext, "{`class<-`(, \"foo\") }");
        assertEval(Output.MayIgnoreErrorContext, "{`class<-`(, ) }");
        assertEval(Output.MayIgnoreErrorContext, "{x=1; `class<-`(x, ) }");
        assertEval(Output.MayIgnoreErrorContext, "{`class<-`(NULL, \"first\") }");

        assertEval("{x=1; class(x)<-\"first\"; x;}");

        assertEval("{ x=1;class(x)<-\"character\"; x}");

        assertEval("{x<-1; class(x)<-\"logical\"; x;  class(x)<-c(1,2,3); x; class(x)<-NULL; x;}");

        assertEval("{x<-1;class(x)<-c(1,2,3);class(x)<-c(); x;}");

        assertEval("{x<-1;class(x)<-c(1,2,3); x;}");

        assertEval("{x<-1;class(x)<-c(TRUE,FALSE); x;}");

        assertEval("{x<-1;class(x)<-c(2+3i,4+5i); x;}");

        assertEval("{x<-1;class(x)<-c(1,2,3);class(x)<-NULL; x;}");

        assertEval("{x<-c(1,2,3,4); dim(x)<-c(2,2); class(x)<-\"array\"; x; class(x)<-\"matrix\"; x;}");

        assertEval(Ignored.NewRVersionMigration, "{x<-c(1,2,3,4); dim(x)<-c(2,2); class(x)}");

        assertEval("{x<-c(1,2,3,4); dim(x)<-c(2,2); class(x);dim(x)<-c(2,2,1);class(x)}");

        assertEval("{x<-c(1,2,3,4); dim(x)<-c(2,2,1); class(x)}");

        assertEval("{x<-1;class(x)<-c(1,2,3);y<-unclass(x);x;y}");

        assertEval("{x<-1;class(x)<-\"a\";x}");

        assertEval("{x<-1;class(x)<-\"a\";class(x)<-\"numeric\";x;}");

        assertEval("{x<-TRUE;class(x)<-\"a\";class(x)<-\"logical\";x;}");

        assertEval("{x<-2+3i;class(x)<-\"a\";class(x)<-\"complex\";x;}");

        assertEval("{x<-c(1,2);class(x)<-\"a\";class(x)<-\"list\";x;}");

        assertEval("{x<-\"abc\";class(x)<-\"a\";class(x)<-\"character\";x;}");

        assertEval("{x<-c(2+3i,4+5i);class(x)<-\"a\";class(x)<-\"complex\";x;}");

        assertEval("{x<-1;attr(x,\"class\")<-c(\"a\",\"b\");x;}");

        // Doesn't remove the class attribute unlike class(x)<-"numeric".
        assertEval("{x<-1;attr(x,\"class\")<-c(\"a\",\"b\");attr(x,\"class\")<-\"numeric\";x}");

        assertEval("{x<-1;attr(x,\"class\")<-\"b\";x;}");

        assertEval("{x<-1;y<-\"b\";attr(x,\"class\")<-y;x;}");

        // test setting class on other types
        assertEval("{ x <- new.env(); class(x); class(x)<-\"abc\"; class(x); class(x)<-NULL; class(x) }");
        assertEval("{ x <- new.env(); class(x); class(x)<-c(\"abc\", \"xyz\"); class(x); class(x)<-NULL; class(x) }");

        assertEval("{ x <- function() { }; class(x); class(x)<-\"abc\"; class(x); class(x)<-NULL; class(x) }");
        assertEval("{ x <- function() { }; class(x); class(x)<-c(\"abc\", \"xyz\"); class(x); class(x)<-NULL; class(x) }");

        assertEval("{x<-c(1,2,3,4); class(x)<-\"array\"; class(x)<-\"matrix\";}");
        assertEval(Output.IgnoreErrorContext, "{x<-1;attr(x,\"class\")<-c(1,2,3);}");

        assertEval("{ x<-function() 42; class(x)<-\"foo\"; class(x)<-NULL; x }");

        assertEval("{ a<-1; class(a)<-c('x','y'); b<-choose(a,1); class(b)[1]<-'z'; a; }");

        // This tests specific activation order of specializations in SetFixedAttributeNode:
        assertEval("{ foo <- function(x,y) { attributes(x) <- y; x }; list(before = foo(as.pairlist(1), list(class='ahoj')), after = foo(as.pairlist(1), list(class=NULL))) }");
    }
}
