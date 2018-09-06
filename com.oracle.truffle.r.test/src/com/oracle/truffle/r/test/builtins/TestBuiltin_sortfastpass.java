/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
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
