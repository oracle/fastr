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

import org.junit.*;

import com.oracle.truffle.r.test.*;

public class TestSimpleMatrix extends TestBase {

    @Test
    public void testAccessDim() {
        assertEval("{ x<-1:10; dim(x) }");
        assertEval("{ x<-FALSE; dim(x) }");
        assertEval("{ x<-TRUE; dim(x) }");
        assertEval("{ x<-1; dim(x) }");
        assertEval("{ x<-1L; dim(x) }");
        assertEval("{ x<-c(1L, 2L, 3L); dim(x) }");
        assertEval("{ x<-c(1, 2, 3); dim(x) }");
    }

    @Test
    public void testUpdateDim() {
        assertEval("{ x<-c(1L,2L,3L,4L,5L,6L); dim(x) <- c(2.1,3.9); dim(x) }");
        assertEval("{ x<-c(1L,2L,3L,4L,5L,6L); dim(x) <- c(2L,3L); dim(x) }");
        assertEval("{ x<-c(1L,2L,3L,4L,5L,6L); dim(x) <- c(2L,3L); dim(x) <- NULL; dim(x) }");
        assertEval("{ x<-c(1,2,3,4,5,6); dim(x) <- c(2L,3L); dim(x) }");
        assertEval("{ x<-c(1,2,3,4,5,6); dim(x) <- c(2L,3L); dim(x) <- NULL; dim(x) }");
    }

    private static final String[] TESTED_4L_VECTORS = new String[]{"(1:4)", "c(1.1, 2.2, 3.3, 4.4)", "c(1+1i, 2+2i, 3+3i, 4+4i)", "c(\"a\", \"b\", \"c\", \"d\")", "c(TRUE, FALSE, TRUE, FALSE)",
                    "c(as.raw(1),as.raw(2),as.raw(3),as.raw(4))", "list(TRUE, \"a\", 42, 1.1)"};
    private static final String[] TESTED_8L_VECTORS = new String[]{"(1:8)", "c(1.1, 2.2, 3.3, 4.4, 5.5, 6.6, 7.7, 8.8)", "c(1+1i, 2+2i, 3+3i, 4+4i, 5+5i, 6+6i, 7+7i, 8+8i)"};

    @Test
    public void testAccessScalarIndex() {
        assertEval("{ x<-1:8; dim(x)<-c(2,2,2); x[1, 2] }");
        assertEval("{ x<-1:8; x[1, 2] }");

        assertEval("{ x<-c(1L,2L,3L,4L,5L,6L); dim(x) <- c(2,3); x[1,2] }");
        assertEval("{ x<-c(1L,2L,3L,4L,5L,6L,7L,8L,9L,10L); dim(x) <- c(2,5); x[2,4] }");
        assertEval("{ x<-c(1, 2, 3, 4); dim(x)<-c(2, 2); x[1] }");

        assertEval("{ x<-c(1+1i, 2+2i, 3+3i, 4+4i); dim(x)<-c(2, 2); x[1, 1] }");
        assertEval("{ x<-c(FALSE, TRUE, TRUE, FALSE); dim(x)<-c(2, 2); x[1, 1] }");
        assertEval("{ x<-c(\"a\", \"b\", \"c\", \"d\"); dim(x)<-c(2, 2); x[1, 1] }");

        assertEval(Output.ContainsError, "{ x<-1:4; dim(x)<-c(2,2); x[1,3] }");
        assertEval(Output.ContainsError, "{ x<-1:8; dim(x)<-c(2, 4); x[c(-1, -2),c(5)] }");

        assertEval(template("{ x<-%0; dim(x)<-c(2,2); x[0,] }", TESTED_4L_VECTORS));
        assertEval(template("{ x<-%0; dim(x)<-c(2,2); x[,0] }", TESTED_4L_VECTORS));
        assertEval(template("{ x<-%0; dim(x)<-c(2,2); x[1,1] }", TESTED_4L_VECTORS));
        assertEval(template("{ x<-%0; dim(x)<-c(2,2); x[1,2] }", TESTED_4L_VECTORS));
        assertEval(template("{ x<-%0; dim(x)<-c(2, 2); x[-1, ] }", TESTED_4L_VECTORS));
        assertEval(template("{ x<-%0; dim(x)<-c(2, 2); x[, -1] }", TESTED_4L_VECTORS));
        assertEval(template("{ x<-%0; dim(x)<-c(2, 2); x[-3, ] }", TESTED_4L_VECTORS));
        assertEval(template("{ x<-%0; dim(x)<-c(2, 2); x[, -3] }", TESTED_4L_VECTORS));
        assertEval(template("{ x<-%0; dim(x)<-c(2,2); x[-1, -1] }", TESTED_4L_VECTORS));
        assertEval(template("{ x<-%0; dim(x)<-c(1,4); dimnames(x)<-list(\"z\", c(\"a\", \"b\", \"c\", \"d\")); x[1, ] }", TESTED_4L_VECTORS));
        assertEval(template("{ x<-%0; dim(x)<-c(1,4); dimnames(x)<-list(\"z\", c(\"a\", \"b\", \"c\", \"d\")); x[1, c(1,3)] }", TESTED_4L_VECTORS));
        assertEval(template("{ x<-%0; dim(x)<-c(1,4); dimnames(x)<-list(\"z\", c(\"a\", \"b\", \"d\", \"e\")); x[1, c(1,3)] }", TESTED_4L_VECTORS));
        assertEval(template("{ x<-%0; dim(x)<-c(2,2); x[c(1, NA), ] }", TESTED_4L_VECTORS));
        assertEval(template("{ x<-%0; dim(x)<-c(2,2); x[c(TRUE, NA), ] }", TESTED_4L_VECTORS));
        assertEval(template("{ x<-%0; x<-1:4; dim(x)<-c(2,2); x[NA, ] }", TESTED_4L_VECTORS));
        // A misalignment error similar to those in TestSimpleVector (testIgnored1-3)
        assertEval(Ignored.ReferenceError, template("{ x<-%0; dim(x)<-c(2,2); dimnames(x)<-list(c(\"a\", \"b\"), c(\"c\", \"d\")); x[c(1,NA), 1] }", TESTED_4L_VECTORS));
        assertEval(Ignored.ReferenceError, template("{ x<-%0; dim(x)<-c(2,2); dimnames(x)<-list(c(\"a\", \"b\"), c(\"c\", \"d\")); x[1, c(1,NA)] }", TESTED_4L_VECTORS));
        assertEval(template("{ x<-%0; dim(x)<-c(2,2); dimnames(x)<-list(c(\"a\", \"b\"), c(\"c\", \"d\")); x[c(1, NA), c(1,NA)] }", TESTED_4L_VECTORS));
        assertEval(template("{ x<-%0; dim(x)<-c(2,2); dimnames(x)<-list(c(\"a\", \"b\"), c(\"c\", \"d\")); x[c(1, 1), c(1,NA)] }", TESTED_4L_VECTORS));
        assertEval(template("{ x<-%0; dim(x)<-c(1,4); dimnames(x)<-list(\"z\", c(\"a\", \"b\", \"c\", \"d\")); x[1, 0] }", TESTED_4L_VECTORS));
        assertEval(template("{ x<-%0; dim(x)<-c(1,4); dimnames(x)<-list(\"z\", c(\"a\", \"b\", \"c\", \"d\")); x[0, 1] }", TESTED_4L_VECTORS));
        assertEval(template("{ x<-%0; dim(x)<-c(1,4); dimnames(x)<-list(\"z\", c(\"a\", \"b\", \"c\", \"d\")); x[-1, 0] }", TESTED_4L_VECTORS));
        assertEval(template("{ x<-%0; dim(x)<-c(1,4); dimnames(x)<-list(\"z\", c(\"a\", \"b\", \"c\", \"d\")); x[0, -1] }", TESTED_4L_VECTORS));
        assertEval(template("{ x<-%0; dim(x)<-c(1,4); dimnames(x)<-list(\"z\", c(\"a\", \"b\", \"c\", \"d\")); x[, 0] }", TESTED_4L_VECTORS));
        assertEval(template("{ x<-%0; dim(x)<-c(1,4); dimnames(x)<-list(\"z\", c(\"a\", \"b\", \"c\", \"d\")); x[0, ] }", TESTED_4L_VECTORS));
        assertEval(template("{ x<-%0; dim(x)<-c(1,4); dimnames(x)<-list(\"z\", c(\"a\", \"b\", \"c\", \"d\")); x[c(1,1,1), 0] }", TESTED_4L_VECTORS));
        assertEval(template("{ x<-%0; dim(x)<-c(1,4); dimnames(x)<-list(\"z\", c(\"a\", \"b\", \"c\", \"d\")); x[0, c(1,1,1)] }", TESTED_4L_VECTORS));

        assertEval(template("{ x<-%0; dim(x)<-c(2,4); x[0,0] }", TESTED_8L_VECTORS));
        assertEval(template("{ x<-%0; dim(x)<-c(2,4); x[,0] }", TESTED_8L_VECTORS));
        assertEval(template("{ x<-%0; dim(x)<-c(2,4); x[0,] }", TESTED_8L_VECTORS));
        assertEval(template("{ x<-%0; dim(x)<-c(2,4); x[1,0] }", TESTED_8L_VECTORS));
        assertEval(template("{ x<-%0; dim(x)<-c(4,2); x[1,0] }", TESTED_8L_VECTORS));
        assertEval(template("{ x<-%0; dim(x)<-c(4,2); x[2,0] }", TESTED_8L_VECTORS));
        assertEval(template("{ x<-%0; dim(x)<-c(2,4); x[-1,0] }", TESTED_8L_VECTORS));
        assertEval(template("{ x<-%0; dim(x)<-c(2,4); x[-2,0] }", TESTED_8L_VECTORS));
        assertEval(template("{ x<-%0; dim(x)<-c(2,4); x[0,1] }", TESTED_8L_VECTORS));
        assertEval(template("{ x<-%0; dim(x)<-c(4,2); x[0,1] }", TESTED_8L_VECTORS));
        assertEval(template("{ x<-%0; dim(x)<-c(4,2); x[0,2] }", TESTED_8L_VECTORS));
        assertEval(template("{ x<-%0; dim(x)<-c(2,4); x[0,-1] }", TESTED_8L_VECTORS));
        assertEval(template("{ x<-%0; dim(x)<-c(2,4); x[0,-2] }", TESTED_8L_VECTORS));
        assertEval(template("{ x<-%0; dim(x)<-c(4,2); x[1, -1] }", TESTED_8L_VECTORS));
        assertEval(template("{ x<-%0; dim(x)<-c(4,2); x[-1, 1] }", TESTED_8L_VECTORS));
        assertEval(template("{ x<-%0; dim(x)<-c(2, 4); x[c(-1, -2),c(-1)] }", TESTED_8L_VECTORS));
        assertEval(template("{ x<-%0; dim(x)<-c(2, 4); x[c(-1, -2),c(-1, -2)] }", TESTED_8L_VECTORS));
        assertEval(template("{ x<-%0; dim(x)<-c(2, 4); x[c(-1, -2),c(-1, -2, -3)] }", TESTED_8L_VECTORS));
        assertEval(template("x<-%0; dim(x)<-c(2, 4); x[c(-1, -2),c(-1, -2, -3, -4)]", TESTED_8L_VECTORS));
        assertEval(template("{ x<-%0; dim(x)<-c(2, 4); x[c(-1, -2),c(0)] }", TESTED_8L_VECTORS));
        assertEval(template("{ x<-%0; dim(x)<-c(2, 4); x[c(-1, -2),c(1)] }", TESTED_8L_VECTORS));
        assertEval(template("{ x<-%0; dim(x)<-c(2, 4); x[c(-1, -2),c(1, 2)] }", TESTED_8L_VECTORS));

        assertEval("{ x<-1:8; dim(x)<-c(2,4);  x[1, TRUE] }");
        assertEval("{ x<-1:8; dim(x)<-c(2,4);  x[1, FALSE] }");
        assertEval("{ x<-1:8; dim(x)<-c(2,4);  x[1, c(TRUE, FALSE, TRUE, FALSE)] }");
        assertEval("{ x<-1:8; dim(x)<-c(2,4);  x[1, c(TRUE, FALSE, TRUE)] }");
        assertEval("{ x<-1:8; dim(x)<-c(2,4);  x[1, c(TRUE, FALSE, TRUE, TRUE, TRUE)] }");
        assertEval("{ x<-(1:8); dim(x)<-c(2,4); x[1, c(NA, NA)] }");
        assertEval("{ x<-(1:8); dim(x)<-c(2,4); x[1, c(1, NA)] }");
        assertEval("{ x<-(1:4); dim(x)<-c(2,2); dimnames(x)<-list(c(\"a\", \"b\"), c(\"c\", \"d\")); x[1, NA] }");
        assertEval("{ x<-(1:4); dim(x)<-c(2,2); dimnames(x)<-list(c(\"a\", \"b\"), c(\"c\", \"d\")); x[NA, 1] }");

        assertEval("{ x<-1:16; dim(x)<-c(4,4); x[-1,-2] }");
        assertEval("{ x<-1:16; dim(x)<-c(4,4); x[-1,c(1,1,2,3)] }");

        assertEval("{ x<-1:4; dim(x)<-c(4,1); dimnames(x)<-list(c(\"a\", \"b\", \"c\", \"d\"), \"z\"); x[, 1] }");
        assertEval("{ x<-1:4; dim(x)<-c(4,1); dimnames(x)<-list(c(\"a\", \"b\", \"c\", \"d\"), \"z\"); x[c(2,4), 1] }");
        assertEval("{ x<-1:4; dim(x)<-c(2,2); dimnames(x)<-list(c(\"a\", \"b\"), c(\"d\", \"e\")); x[1, 1] }");
        assertEval("{ x<-1:4; dim(x)<-c(2,2); dimnames(x)<-list(c(\"a\", \"b\"), c(\"d\", \"e\")); x[c(1,1), 1] }");
        assertEval("{ x<-1:4; dim(x)<-c(2,2); dimnames(x)<-list(c(\"a\", \"b\"), c(\"d\", \"e\")); x[c(1,2), 1] }");
        assertEval("{ x<-1:4; dim(x)<-c(2,2); dimnames(x)<-list(c(\"a\", \"b\"), c(\"d\", \"e\")); x[c(1,2,1), 1] }");

        assertEval("{ x<-1:4; dim(x)<-c(2,2); dimnames(x)<-list(c(\"a\", \"b\"), c(\"d\", \"e\")); x[\"b\", 1] }");
        assertEval("{ x<-1:4; dim(x)<-c(2,2); dimnames(x)<-list(c(\"a\", \"b\"), c(\"d\", \"e\")); x[\"d\", 1] }");
        assertEval("{ x<-1:4; dim(x)<-c(2,2); dimnames(x)<-list(c(\"a\", \"b\"), c(\"d\", \"e\")); x[as.character(NA), 1] }");
        assertEval("{ x<-1:4; dim(x)<-c(2,2); dimnames(x)<-list(c(\"a\", \"b\"), c(\"d\", \"e\")); x[c(\"b\"), 1] }");
        assertEval("{ x<-1:4; dim(x)<-c(2,2); dimnames(x)<-list(c(\"a\", \"b\"), c(\"d\", \"e\")); x[c(\"a\", \"b\"), 1] }");
        assertEval("{ x<-1:4; dim(x)<-c(2,2); dimnames(x)<-list(c(\"a\", \"b\"), c(\"d\", \"e\")); x[c(\"a\", \"a\"), 1] }");
        assertEval("{ x<-1:2; dim(x)<-c(1:2); dimnames(x)<-list(\"z\", c(\"a\", \"b\")); x[\"z\", 1] }");
        assertEval("{ x<-1:2; dim(x)<-c(1:2); dimnames(x)<-list(\"z\", c(\"a\", \"b\")); x[c(\"z\", \"z\"), 1] }");
    }

    @Test
    public void testUpdateScalarIndex() {
        assertEval("{ x<-c(1L,2L,3L,4L,5L,6L); dim(x) <- c(2,3); x[1,2] <- 100L; x[1,2] }");
        assertEval("{ x<-c(1L,2L,3L,4L,5L,6L,7L,8L,9L,10L); dim(x) <- c(2,5); x[2,4] <- 100L; x[2,4] }");
        assertEval("{ x<-c(1.1, 2.2, 3.3, 4.4); dim(x)<-c(2,2); x[, c(1)] }");
        assertEval("{ x<-c(1.1, 2.2, 3.3, 4.4); dim(x)<-c(2,2); x[c(1), ] }");
        assertEval("{ x<-c(1.1, 2.2, 3.3, 4.4); dim(x)<-c(2,2); x[, c(1,2)] }");
        assertEval("{ x<-c(1.1, 2.2, 3.3, 4.4); dim(x)<-c(2,2); x[c(1,2), ] }");

        assertEval("{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[,][1]<-42; x }");
        assertEval("{  x<-c(1,2,3,4); dim(x)<-c(2,2); x[3][1]<-42; x }");
        assertEval("{  x<-c(1,2,3,4); dim(x)<-c(2,2); x[3][1][1]<-42; x }");
        assertEval("{ x<-c(1L,2L,3L,4L); dim(x)<-c(2,2); f<-function() { x[3][1]<-42; x }; f() }");
        assertEval("{ x<-c(1L,2L,3L,4L); dim(x)<-c(2,2); f<-function() { x[3][1]<-42; }; f(); x }");
    }

    @Test
    public void testMatrixAccessWithScalarAndVector() {
        assertEval("{ i <- c(1L,3L,5L) ; m <- 1:10 ; dim(m) <- c(2,5) ; m[2,i] }");
        assertEval("{ i <- c(1L,3L,5L) ; m <- c(\"a\",\"b\",\"c\",\"d\",\"e\",\"f\",\"g\",\"h\",\"i\",\"j\") ; dim(m) <- c(2,5) ; m[2,i] }");
    }

}
