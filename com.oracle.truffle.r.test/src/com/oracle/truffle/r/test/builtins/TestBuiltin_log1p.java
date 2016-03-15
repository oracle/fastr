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
package com.oracle.truffle.r.test.builtins;

import org.junit.*;

import com.oracle.truffle.r.test.*;

// Checkstyle: stop line length check
public class TestBuiltin_log1p extends TestBase {

    @Test
    public void testlog1p1() {
        assertEval("argv <- list(c(-0.160475096572577, -0.953101214495634, -0.329547420118877, -0.234819677566528, -0.108178529791777, -0.0994458210555148, -0.282992873965743, -0.731707656126625, -0.866467764292465, -0.76039953639421, -0.3580569675068, -0.52382260076554, -0.240530699925064, -0.236619747356161, -0.811827419307205, -0.154911720192001, -0.97472580847241, -0.464016625026599, -0.58493655376716, -0.230096919024049));log1p(argv[[1]]);");
    }

    @Test
    public void testlog1p2() {
        assertEval("argv <- list(-7e-04);log1p(argv[[1]]);");
    }

    @Test
    public void testlog1p3() {
        assertEval(Ignored.ReferenceError, "log1p(c(1+1i,-1-1i))");
    }
}
