/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.test.library.base;

import org.junit.Test;

import com.oracle.truffle.r.test.ArithmeticWhiteList;
import com.oracle.truffle.r.test.TestBase;

/**
 * Testing of simple values. Most of the tests in this class are generated from templates using the
 * {@link #template} method.
 */
public class TestSimpleValues extends TestBase {

    public static final boolean VERBOSE_TEST_OUTPUT = false;
    public static final boolean GENERATE_INSTEAD_OF_CHECK = false;
    public static final String SPLIT_CHAR = "|";

    // TODO: Add when printing of double numbers is more compliant.
    // "0.1234567", "123456789000"
    private static final String[] SCALAR_NORMAL_VALUES = {"TRUE", "FALSE", "1", "as.raw(10)", "3.4", "1L", "NULL", "1i", "\"hello\""};
    private static final String[] SCALAR_SPECIAL_VALUES = {"(1+NA)", "(3.4+NA)", "(1i+NA)", "(0/0)", "((0/0)+1i)", "NULL", "(1/0)", "(-(1/0))", "(-(0/0))", "(-0.0)"};
    private static final String[] VECTOR_VALUES = template("c(%0,%0,%0)", SCALAR_NORMAL_VALUES);
    private static final String[] SCALAR_VALUES = join(SCALAR_NORMAL_VALUES, SCALAR_SPECIAL_VALUES);
    private static final String[] LIST_VALUES = {"list(1, 2, 3)"};
    private static final String[] ALL_VALUES = join(SCALAR_VALUES, VECTOR_VALUES, LIST_VALUES);
    private static final String[] ALL_ARITHMETIC_VALUES = join(SCALAR_VALUES, VECTOR_VALUES);
    private static final String[] BINARY_OPERATORS = {"+", "-", "*", "/", "^", "%%"};
    private static final String[] UNARY_BUILTINS = {"length", "abs", "rev", "names"};

    @Test
    public void testPrintValues() {
        assertEval(template("%0", ALL_VALUES));
    }

    @Test
    public void testUnaryBuiltings() {
        assertEval(Output.MayIgnoreErrorContext, template("%0(%1)", UNARY_BUILTINS, ALL_ARITHMETIC_VALUES));
    }

    private static final String[] SUBSCRIPT_SEQUENCE_VALUES = {"1:1", "2:4", "4:2"};
    private static final String[] SUBSCRIPT_SCALAR_VALUES = {"0", "2", "-2", "10", "-10", "(1+NA)"};
    private static final String[] SUBSCRIPT_MISSING_VALUES = {"2,", ",2", ","};
    private static final String[] SUBSCRIPT_VECTOR_TWO_VALUES = template("c(%0,%1)", SUBSCRIPT_SCALAR_VALUES, SUBSCRIPT_SCALAR_VALUES);
    private static final String[] SUBSCRIPT_VECTOR_THREE_VALUES = template("c(%0,%1,%2)", SUBSCRIPT_SCALAR_VALUES, SUBSCRIPT_SCALAR_VALUES, SUBSCRIPT_SCALAR_VALUES);
    private static final String[] SUBSCRIPT_ALL_VALUES = join(SUBSCRIPT_SCALAR_VALUES, SUBSCRIPT_VECTOR_TWO_VALUES, SUBSCRIPT_VECTOR_THREE_VALUES, SUBSCRIPT_SEQUENCE_VALUES);
    private static final String[] INT_UPDATE_VALUES = {"c(200L,300L)", "c(400L,500L,600L)"};
    private static final String[] TESTED_VECTORS = {"(0:4)", "c(1L, 2L, 3L, 4L, 5L)"};

    @Test
    public void testTranspose() {
        assertEval("x <- c(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L); dim(x) <- c(2L,5L); x <- t(x); dim(x) <- NULL; x");
        assertEval("x <- c(1, 2, 3, 4, 5, 6, 7, 8, 9, 10); dim(x) <- c(2L,5L); x <- t(x); dim(x) <- NULL; x");
    }

    @Test
    public void testAttributes() {
        assertEval(template("x <- 1; attr(x, \"a\") <- %0; attr(x, \"a\");", ALL_VALUES));
        assertEval(template("x <- 1; attr(x, \"a\") <- %0; names(attributes(x));", ALL_VALUES));
    }

    @Test
    public void testUnlist() {
        assertEval("x <- list(1, 2, 3); unlist(x);");
        assertEval("x <- list(1, TRUE, 3); unlist(x);");
        assertEval("x <- list(1, 2, NA); unlist(x);");
        assertEval("x <- list(1L, 2L, 3L); unlist(x);");
    }

    @Test
    public void testVectorStringAccess() {
        assertEval("x <- NULL; x[c(\"A\", \"A\", \"B\")] <- 1; names(x)");
        assertEval("y <- NULL; y[c(\"A\", \"A\", \"B\")] <- 1; y <- NULL; names(y)");
        assertEval("x <- NULL; names(x)");
        assertEval("x <- NULL; x[c(\"A\", \"A\", \"B\")] <- \"x\"; names(x[\"A\"])");
        assertEval("x <- NULL; x[c(\"A\", \"A\", \"B\")] <- \"x\"; as.vector(x[\"A\"])");
        assertEval("x <- NULL; x[c(\"A\", \"B\", \"C\")] <- c(\"x\", \"y\", \"z\"); as.vector(x[\"B\"])");
        assertEval("x <- NULL; x[c(\"A\", \"B\", \"C\")] <- c(\"x\", \"y\", \"z\"); as.vector(x[\"C\"])");
        assertEval("x <- NULL; codes <- c(\"A\", \"C\", \"G\"); complements <- c(\"T\", \"G\", \"C\"); x[codes] <- complements; as.vector(x);");
        assertEval("x <- NULL; codes <- c(\"A\", \"C\", \"G\"); complements <- c(\"T\", \"G\", \"C\"); x[codes] <- complements; names(x);");
        assertEval("x <- NULL; codes <- c(\"A\", \"C\", \"G\"); complements <- c(\"T\", \"G\", \"C\"); x[codes] <- complements; x[tolower(codes)] <- complements; as.vector(x);");
        assertEval("x <- NULL; codes <- c(\"A\", \"C\", \"G\"); complements <- c(\"T\", \"G\", \"C\"); x[codes] <- complements; x[tolower(codes)] <- complements; names(x);");
    }

    @Test
    public void testVectorAccess() {
        assertEval(Output.MayIgnoreErrorContext, template("x <- %1; x[%0]", SUBSCRIPT_ALL_VALUES, TESTED_VECTORS));
    }

    @Test
    public void testVectorUpdate() {
        assertEval("x <- c(1, 2, 3); y <- x; x[1] <- 100; y;");
        assertEval("x <- 1:10; for (i in 1:2) { x[[1]] <- x[[1]]; x <- c(1, 2, 3) }; x");
        assertEval("v <- double(5) ; v[[3]] <- c(1) ; v");
        assertEval("v <- double(5) ; v[[3]] <- matrix(c(1)) ; v");
        // TODO(tw): Expand this test.
        assertEval(Output.MayIgnoreErrorContext, Output.MayIgnoreWarningContext,
                        template("{ x <- %2; x[%0] <- %1; x }", join(SUBSCRIPT_VECTOR_THREE_VALUES, SUBSCRIPT_SEQUENCE_VALUES), INT_UPDATE_VALUES, TESTED_VECTORS));
    }

    @Test
    public void testMatrixAccess() {
        assertEval(template("x <- matrix(c(1,2,3,4),2) ; x[%0]", SUBSCRIPT_MISSING_VALUES));
    }

    @Test
    public void testTypeofValues() {
        assertEval(template("typeof(%0)", ALL_VALUES));
    }

    @Test
    public void testFunctionCall() {
        assertEval(template("f <- function(x) { x[1] = 2; }; x <- %0; f(x); x[1]", new String[]{"1:3", "(1:3)+0.1", "c(1, 2, 3)", "1", "c(1L, 2L, 3L)", "1L"}));
    }

    private static final String[] TESTED_EXPRESSIONS = {"a <- b", "a[1] <- 7", "b <- a", "b[2] <- 8", "a[3] <- 9", "b <- a + 0"};

    private static final String[] TESTED_EXPRESSION_SEQS = template("%0;%1;%2;%3", TESTED_EXPRESSIONS, TESTED_EXPRESSIONS, TESTED_EXPRESSIONS, TESTED_EXPRESSIONS);

    @Test
    public void testVectorCopySemantics() {
        assertEval(template("a <- c(1, 2, 3); b <- c(4, 5, 6); %0; c(toString(a),toString(b))", TESTED_EXPRESSION_SEQS));
    }

    @Test
    public void testBinaryArithmetic() {
        assertEval("FALSE^(-3)");
        assertEval(Output.MayIgnoreErrorContext, ArithmeticWhiteList.WHITELIST, template("%0%1%2", ALL_ARITHMETIC_VALUES, BINARY_OPERATORS, ALL_ARITHMETIC_VALUES));
    }

    @Test
    public void testAmbiguousExpression() {
        assertEval(ArithmeticWhiteList.WHITELIST, new String[]{"exp(-abs((0+1i)/(0+0i)))"});
    }

    @Test
    public void testStrings() {
        assertEval("{ \"hello\" }");
    }

    @Test
    public void testComplex() {
        assertEval("{ 1i }");
    }

    @Test
    public void testSpecial() {
        assertEval("{ NULL }");
        assertEval("{ NA }");
        assertEval("{ Inf }");
        assertEval("{ NaN }");
    }

    @Test
    public void testDefaultVariables() {
        assertEval("{ .Platform$endian }");
    }
}
