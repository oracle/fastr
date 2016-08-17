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
public class TestBuiltin_duplicated extends TestBase {

    @Test
    public void testduplicated1() {
        assertEval("argv <- list(c('methods', 'base'), FALSE, FALSE, NA); .Internal(duplicated(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testduplicated2() {
        assertEval("argv <- list(list('!', '%%', '%*%', '%/%', '&', '*', '+', '-', '/', 'Arith', 'BunchKaufman', 'Cholesky', 'Compare', 'Logic', 'Math2', 'Math', 'Ops', 'Schur', 'Summary', '[', '[<-', '^', 'all.equal', 'all', 'any', 'as.array', 'as.integer', 'as.logical', 'as.matrix', 'as.numeric', 'as.vector', 'band', 'cbind2', 'chol2inv', 'chol', 'coerce', 'coerce<-', 'colMeans', 'colSums', 'cov2cor', 'crossprod', 'determinant', 'diag', 'diag<-', 'diff', 'dim', 'dim<-', 'dimnames', 'dimnames<-', 'drop', 'expand', 'expm',     'facmul', 'forceSymmetric', 'format', 'head', 'image', 'initialize', 'is.finite', 'is.infinite', 'is.na', 'isDiagonal', 'isSymmetric', 'isTriangular', 'kronecker', 'length', 'lu', 'mean', 'nnzero', 'norm', 'pack', 'print', 'prod', 'qr.Q', 'qr.R', 'qr.coef', 'qr.fitted', 'qr.qty', 'qr.qy', 'qr.resid', 'qr', 'rbind2', 'rcond', 'rep', 'rowMeans', 'rowSums', 'show', 'skewpart', 'solve', 'sum', 'summary', 'symmpart', 't', 'tail', 'tcrossprod', 'toeplitz', 'tril', 'triu', 'unname', 'unpack', 'update',     'updown', 'which', 'writeMM', 'zapsmall', 'Ops', '[', 'Math'), FALSE, FALSE, NA); .Internal(duplicated(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testduplicated3() {
        assertEval("argv <- list(c(3L, 8L, 18L), FALSE, FALSE, NA); .Internal(duplicated(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testduplicated4() {
        assertEval("argv <- list(c(0, 0.700492869640978, NA), FALSE, FALSE, NA); .Internal(duplicated(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testduplicated5() {
        assertEval("argv <- list(1L, FALSE, TRUE, NA); .Internal(duplicated(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testduplicated6() {
        assertEval("argv <- list(character(0), FALSE, FALSE, NA); .Internal(duplicated(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testduplicated7() {
        assertEval("argv <- list(list('plot', 'Ops', '[', 'Math'), FALSE, FALSE, NA); .Internal(duplicated(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testduplicated8() {
        assertEval("argv <- list(structure('lattice', .Names = ''), FALSE, FALSE, NA); .Internal(duplicated(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testduplicated10() {
        assertEval("argv <- list(structure(list(c0 = structure(integer(0), .Label = character(0), class = 'factor')), .Names = 'c0', row.names = character(0), class = structure('integer(0)', .Names = 'c0')), structure(list(c0 = structure(integer(0), .Label = character(0), class = 'factor')), .Names = 'c0', row.names = character(0), class = structure('integer(0)', .Names = 'c0')), FALSE, NA); .Internal(duplicated(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testduplicated11() {
        assertEval("argv <- list(c('\\\\title', '\\\\name', '\\\\alias', '\\\\alias', '\\\\keyword', '\\\\keyword', '\\\\description', '\\\\usage', '\\\\arguments', '\\\\details', '\\\\value', '\\\\section', '\\\\section', '\\\\seealso', '\\\\examples'), FALSE, FALSE, NA); .Internal(duplicated(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testduplicated12() {
        assertEval("argv <- list(structure(c(-0.838428742794102, 0.838428742794102, 0.838428742794102, 0.838428742794102, -0.838428742794102, -0.838428742794102), .Dim = c(6L, 1L), .Dimnames = list(c('1', '3', '4', '5', '6', '7'), NULL)), FALSE, FALSE, NA); .Internal(duplicated(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testduplicated13() {
        assertEval("argv <- list(c(-1628571, -1628571, -1200000, -1200000, -1057143, -914286, -771429, -771429, -771429, -628571, -628571, -485714, -485714, -485714, -485714, -342857, -342857, -342857, -342857, -2e+05, -2e+05, -2e+05, -2e+05, -57143, -57143, -57143, 85714, 85714, 228571, 228571, 228571, 371429, 371429, 371429, 371429, 514286, 514286, 514286, 657143, 657143, 657143, 657143, 657143, 942857, 1085714, 1228571, 1228571, 1228571, 1228571, 1371429), FALSE, FALSE, NA); .Internal(duplicated(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testduplicated14() {
        assertEval("argv <- list(c(1, 0.778249191273129, 0.65570344192776, 0.65570344192776, 0.105668080308148, 0.0451091129154675, 0.0451091129154675, 1.49604383156071e-06, 8.3976239365668e-11, 2.13195391672632e-15, 1.4298180954663e-20, 1.47541167362595e-26, 1.09353648287987e-33, 1.6858825926109e-42, 1.6858825926109e-42, 1.6858825926109e-42), FALSE, FALSE, NA); .Internal(duplicated(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testDuplicated() {
        assertEval("{ duplicated(c(1L, 2L, 3L, 4L, 2L, 3L)) }");
        assertEval("{ duplicated(c(1L, 2L, 3L, 4L, 2L, 3L), incomparables = TRUE )}");
        assertEval("{ duplicated(c(1L, 2L, 3L, 4L, 2L, 3L), fromLast = TRUE) }");

        // strings
        assertEval("{duplicated(c(\"abc\"))}");
        assertEval("{duplicated(c(\"abc\", \"def\", \"abc\"))}");
        assertEval("{duplicated(c(\"abc\", \"def\", \"ghi\", \"jkl\"))}");

        // boolean
        assertEval("{duplicated(c(FALSE))}");
        assertEval("{duplicated(c(FALSE, TRUE))}");
        assertEval("{duplicated(c(FALSE, TRUE, FALSE))}");

        // complex
        assertEval("{duplicated(c(2+2i)) }");
        assertEval("{duplicated(c(2+2i, 3+3i, 2+2i)) }");
        assertEval("{duplicated(c(2+2i, 3+3i, 4+4i, 5+5i)) }");

        // Double Vector
        assertEval("{ duplicated(c(27.2, 68.4, 94.3, 22.2)) }");
        assertEval("{ duplicated(c(1, 1, 4, 5, 4), TRUE, TRUE) }");
        assertEval("{ duplicated(c(1,2,1)) }");
        assertEval("{ duplicated(c(1)) }");
        assertEval("{ duplicated(c(1,2,3,4)) }");
        assertEval("{ duplicated(list(76.5, 5L, 5L, 76.5, 5, 5), incomparables = c(5L, 76.5)) }");

        // Logical Vector
        assertEval("{ duplicated(c(TRUE, FALSE, TRUE), TRUE) }");
        assertEval("{ duplicated(c(TRUE, FALSE, TRUE), TRUE, fromLast = 1) }");

        // String Vector
        assertEval("{ duplicated(c(\"abc\", \"good\", \"hello\", \"hello\", \"abc\")) }");
        assertEval("{ duplicated(c(\"TRUE\", \"TRUE\", \"FALSE\", \"FALSE\"), FALSE) }");
        assertEval("{ duplicated(c(\"TRUE\", \"TRUE\", \"FALSE\", \"FALSE\"), TRUE) }");
        assertEval("{ duplicated(c(\"TRUE\", \"TRUE\", \"FALSE\", \"FALSE\"), 1) }");

        // Complex Vector
        assertEval("{ duplicated(c(1+0i, 6+7i, 1+0i), TRUE)}");
        assertEval("{ duplicated(c(1+1i, 4-6i, 4-6i, 6+7i)) }");
        assertEval("{ duplicated(c(1, 4+6i, 7+7i, 1), incomparables = c(1, 2)) }");

        assertEval(Output.IgnoreWarningContext, "{ duplicated(c(1L, 2L, 1L, 1L, 3L, 2L), incomparables = \"cat\") }");
        assertEval(Output.IgnoreWarningContext, "{ duplicated(c(1,2,3,2), incomparables = c(2+6i)) }");

        assertEval("{ duplicated(NULL, 0); }");

        assertEval("{ x<-quote(f(7, 42)); duplicated(x) }");
        assertEval("{ x<-function() 42; duplicated(x) }");
        assertEval(Output.IgnoreErrorMessage, "{ duplicated(c(1,2,1), incomparables=function() 42) }");

    }
}
