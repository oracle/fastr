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

import org.junit.*;

import com.oracle.truffle.r.test.*;

// Checkstyle: stop line length check

public class TestBuiltin_asmatrix extends TestBase {

    private final String expectedOut1 = ""
                    + "      [,1] [,2] [,3] [,4] [,5] [,6] [,7] [,8] [,9] [,10]\n"
                    + " [1,] 0+1i 0+1i 0+1i 0+1i 0+1i 0+1i 0+1i 0+1i 0+1i  0+1i\n"
                    + " [2,] 0+1i 0+1i 0+1i 0+1i 0+1i 0+1i 0+1i 0+1i 0+1i  0+1i\n"
                    + " [3,] 0+1i 0+1i 0+1i 0+1i 0+1i 0+1i 0+1i 0+1i 0+1i  0+1i\n"
                    + " [4,] 0+1i 0+1i 0+1i 0+1i 0+1i 0+1i 0+1i 0+1i 0+1i  0+1i\n"
                    + " [5,] 0+1i 0+1i 0+1i 0+1i 0+1i 0+1i 0+1i 0+1i 0+1i  0+1i\n"
                    + " [6,] 0+1i 0+1i 0+1i 0+1i 0+1i 0+1i 0+1i 0+1i 0+1i  0+1i\n"
                    + " [7,] 0+1i 0+1i 0+1i 0+1i 0+1i 0+1i 0+1i 0+1i 0+1i  0+1i\n"
                    + " [8,] 0+1i 0+1i 0+1i 0+1i 0+1i 0+1i 0+1i 0+1i 0+1i  0+1i\n"
                    + " [9,] 0+1i 0+1i 0+1i 0+1i 0+1i 0+1i 0+1i 0+1i 0+1i  0+1i\n"
                    + "[10,] 0+1i 0+1i 0+1i 0+1i 0+1i 0+1i 0+1i 0+1i 0+1i  0+1i\n";

    private final String expectedOut2 = ""
                    + "      [,1] [,2] [,3] [,4] [,5] [,6] [,7] [,8] [,9] [,10]\n"
                    + " [1,] 0+1i 0+1i 0+1i 0+1i 0+1i 0+1i 0+1i 0+1i 0+1i  0+1i\n"
                    + " [2,]   NA   NA   NA   NA   NA   NA   NA   NA   NA    NA\n"
                    + " [3,] 0+1i 0+1i 0+1i 0+1i 0+1i 0+1i 0+1i 0+1i 0+1i  0+1i\n"
                    + " [4,]   NA   NA   NA   NA   NA   NA   NA   NA   NA    NA\n"
                    + " [5,] 0+1i 0+1i 0+1i 0+1i 0+1i 0+1i 0+1i 0+1i 0+1i  0+1i\n"
                    + " [6,]   NA   NA   NA   NA   NA   NA   NA   NA   NA    NA\n"
                    + " [7,] 0+1i 0+1i 0+1i 0+1i 0+1i 0+1i 0+1i 0+1i 0+1i  0+1i\n"
                    + " [8,]   NA   NA   NA   NA   NA   NA   NA   NA   NA    NA\n"
                    + " [9,] 0+1i 0+1i 0+1i 0+1i 0+1i 0+1i 0+1i 0+1i 0+1i  0+1i\n"
                    + "[10,]   NA   NA   NA   NA   NA   NA   NA   NA   NA    NA\n";

    @Test
    public void testasmatrix1() {
        assertEval("argv <- structure(list(x = structure(c(9L, 27L, 27L, 27L, 27L,     3L, 3L, 3L, 3L, 9L, 9L, 9L, 9L, 9L, 9L), .Names = c('Blocks',     'A', 'B', 'C', 'D', 'Blocks:A', 'Blocks:B', 'Blocks:C', 'Blocks:D',     'A:B', 'A:C', 'A:D', 'B:C', 'B:D', 'C:D'))), .Names = 'x');"
                        + "do.call('as.matrix', argv)");
    }

    @Test
    public void testIgnoredMatrixExpression1() {
        Assert.assertEquals(expectedOut1, fastREval("{ matrix(1i,10,10) }"));
    }

    @Test
    public void testIgnoredMatrixExpression2() {
        Assert.assertEquals(expectedOut2, fastREval("{ matrix(c(1i,NA),10,10) }"));
    }

    @Test
    public void testMatrix() {
        assertEval("{ matrix(c(1,2,3,4),2,2) }");
        assertEval("{ matrix(as.double(NA),2,2) }");
        assertEval("{ matrix(\"a\",10,10) }");
        assertEval("{ matrix(c(\"a\",NA),10,10) }");
        assertEval("{ matrix(1:4, nrow=2) }");
        assertEval("{ matrix(c(1,2,3,4), nrow=2) }");
        assertEval("{ matrix(c(1+1i,2+2i,3+3i,4+4i),2) }");
        assertEval("{ matrix(nrow=2,ncol=2) }");
        assertEval("{ matrix(1:4,2,2) }");
        assertEval(Ignored.ReferenceError, "{ matrix(1i,10,10) }");
        assertEval(Ignored.ReferenceError, "{ matrix(c(1i,NA),10,10) }");
        assertEval("{ matrix(c(10+10i,5+5i,6+6i,20-20i),2) }");
        assertEval("{ matrix(c(1i,100i),10,10) }");
        assertEval("{ matrix(1:6, nrow=3,byrow=TRUE)}");
        assertEval("{ matrix(1:6, nrow=3,byrow=1)}");
        assertEval("{ matrix(1:6, nrow=c(3,4,5),byrow=TRUE)}");
        assertEval("{ matrix(1:6)}");
        assertEval("{ matrix(1:6, ncol=3:5,byrow=TRUE)}");

        assertEval("{ matrix(TRUE,FALSE,FALSE,TRUE)}");
        assertEval("{ matrix(c(NaN,4+5i,2+0i,5+10i)} ");

        // FIXME missing warning
        assertEval(Ignored.Unknown, Output.ContainsWarning, "{ matrix(c(1,2,3,4),3,2) }");
        assertEval(Ignored.Unknown, Output.ContainsWarning, "{ matrix(1:4,3,2) }");

        assertEval("{ x<-matrix(integer(), ncol=2) }");
        assertEval("{ x<-matrix(integer(), nrow=2) }");
        assertEval("{ x<-matrix(integer(), ncol=2, nrow=3) }");
        assertEval("{ x<-matrix(integer()) }");

        assertEval("{ x<-matrix(list(), nrow=2, ncol=2); x }");
        assertEval("{ x<-matrix(list(), nrow=2, ncol=2, dimnames=list(c(\"a\", \"b\"), c(\"c\", \"d\"))); x }");
    }
}
