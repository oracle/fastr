/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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

package com.oracle.truffle.r.test.library.stats;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

public class TestExternal_covcor extends TestBase {
    @Test
    public void testCovcor() {
        assertEval(".Call(stats:::C_cov, 1:5, 1:5, 4, FALSE)");
        assertEval(".Call(stats:::C_cov, 1:5, c(1,5,1,5,10), 4, FALSE)");
    }

    @Test
    public void testCovcorArgsCasts() {
        assertEval(".Call(stats:::C_cov, c('1','2','3','4','5'), 1:5, 4, FALSE)");
        assertEval(".Call(stats:::C_cov, NULL, 1:5, 4, FALSE)");
        assertEval(".Call(stats:::C_cov, 1:3, 1:5, 4, FALSE)");
    }

    @Test
    public void testCombinations() {
        String[] useCor = new String[]{"e", "a", "c", "n", "p"};
        String[] useCov = new String[]{"e", "a", "c", "n"};
        String[] methods = new String[]{"p", "k", "s"};
        assertEval(template("cor(mtcars[,1:4], use='%0', method='%1')", useCor, methods));
        assertEval(template("cor(1:4, c(1,7,1,-4), use='%0', method='%1')", useCor, methods));
        assertEval(template("cov(mtcars[,1:4], use='%0', method='%1')", useCov, methods));
        assertEval(template("cov(1:4, c(1,7,1,-4), use='%0', method='%1')", useCov, methods));
    }
}
