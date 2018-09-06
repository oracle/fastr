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
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import com.oracle.truffle.r.test.TestBase;
import org.junit.Test;

public class TestBuiltin_sortfastpass extends TestBase {

    @Test
    public void testsortfastpass() {
        assertEval(".Internal(sorted_fpass(NA, FALSE, FALSE))");
        assertEval(".Internal(sorted_fpass(1, FALSE, FALSE))");
        assertEval(".Internal(sorted_fpass(1.5, FALSE, FALSE))");
        assertEval(".Internal(sorted_fpass(c(), FALSE, FALSE))");
        assertEval(".Internal(sorted_fpass(NULL, FALSE, FALSE))");
        assertEval(".Internal(sorted_fpass(1:5, FALSE, FALSE))");
        assertEval(".Internal(sorted_fpass(1:5, TRUE, FALSE))");
        assertEval(".Internal(sorted_fpass(1:5, FALSE, TRUE))");
        assertEval(".Internal(sorted_fpass(1:5, TRUE, TRUE))");
        assertEval(".Internal(sorted_fpass(1.5:5.5, FALSE, FALSE))");
        assertEval(".Internal(sorted_fpass(seq(1.5,5.5,0.5), FALSE, FALSE))");
        assertEval(".Internal(sorted_fpass(paste('hello',1:10,'.'), FALSE, FALSE))");
        assertEval("argv <- list(1:10, FALSE, FALSE); argv2 <- argv[[1]] + 1; .Internal(sorted_fpass(argv2[[1]], argv2[[2]], argv2[[3]]))");
        assertEval("argv <- list(c(1,2,3,4,5,6,7,8), FALSE, FALSE); argv2 <- argv[[1]] + 1; .Internal(sorted_fpass(argv2[[1]], argv2[[2]], argv2[[3]]))");
    }
}
