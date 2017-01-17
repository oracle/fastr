/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check
public class TestBuiltin_dimassign extends TestBase {

    @Test
    public void testdimassign1() {
        assertEval("argv <- list(structure(c(300, 3000, 400, 4000), .Dim = c(2L, 2L, 1L)), value = c(2, 2, 1));`dim<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testdimassign2() {
        assertEval("argv <- list(structure(logical(0), .Dim = c(0L, 0L)), value = c(0L, 0L));`dim<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testdimassign3() {
        assertEval("argv <- list(structure(1:12, .Dim = 12L), value = 12L);`dim<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testdimassign4() {
        assertEval("argv <- list(structure(list(1:3, 4:6, 3.14159265358979, c('a', 'b', 'c')), .Dim = c(2L, 2L)), value = c(2, 2));`dim<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testdimassign5() {
        assertEval("argv <- list(structure(list(NULL, NULL, NULL, NULL, NULL, NULL), .Dim = 2:3), value = c(2, 3));`dim<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testdimassign6() {
        assertEval("argv <- list(structure(list(1L, 3.14159265358979, 3+5i, 'testit', TRUE, structure(1L, .Label = 'foo', class = 'factor')), .Dim = c(1L, 6L)), value = c(1, 6));`dim<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testdimassign7() {
        assertEval("argv <- list(structure(1:12, .Dim = 3:4), value = 3:4);`dim<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testdimassign8() {
        assertEval(Ignored.Unknown, "argv <- list(NULL, NULL);`dim<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testdimassign9() {
        assertEval("argv <- list(structure(c(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L, 12L, 13L, 14L, 15L, 16L, 17L, 1L, 2L, 3L, 4L, 5L, 6L, 7L), .Dim = c(3L, 4L, 2L)), value = c(3, 4, 2));`dim<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testdimassign10() {
        assertEval("argv <- list(structure(c(1, 0.870865388077469, -0.224541709419829, -0.331803324650025, -0.493630926048296, -0.413999601257247, 0.00943216495885559, -0.569185666759019, 0.183501080823027, -0.658299946719611, -0.563901271084431, -0.104454834691276, 0.715158727414282, -0.0805825981616209, -0.773816895694757, -0.253034783981378, -0.783775136777695, -0.439357063536005, -0.941680494841322, 0.227158249206389, -0.50752863656701, -0.0658964161620369, -0.0689244902651806, 0.185611518464636, 0.378167766177418, -0.0629003710494349, 0.487507055153686, -0.148876486655171), .Dim = c(1L, 28L)), value = c(1, 28));`dim<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testdimassign11() {
        assertEval("argv <- list(structure(NA, .Dim = c(1L, 1L)), value = c(1L, 1L));`dim<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testdimassign12() {
        assertEval("b <- c(a=1+2i,b=3+4i);dim(b) <- c(2,1);attributes(x)");
    }
}
