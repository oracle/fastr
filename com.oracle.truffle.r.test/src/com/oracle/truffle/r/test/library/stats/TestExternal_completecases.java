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

public class TestExternal_completecases extends TestBase {
    @Test
    public void testCompleteCases() {
        assertEval("stats::complete.cases(data.frame(col1=c(1,2,NA), col2=c(1,2,3)))");
        assertEval("stats::complete.cases(data.frame(col1=c(1,2,NA), col2=c(1,2,3)), data.frame(col1=c(1,NA,3), col2=c(1,2,3)))");
        assertEval("stats::complete.cases(data.frame(col1=c(1,2,NA), col2=c(1,2,3)), c(1,NA,2))");
        assertEval("stats::complete.cases(data.frame(col1=c(1,NA), col2=c(2,3)), matrix(c(1,NA,2,NA), nrow=2))");
    }

    @Test
    public void testCompleteCasesArgsValidation() {
        assertEval(Output.IgnoreErrorContext, "stats::complete.cases(data.frame(col1=c(1,2,NA), col2=c(1,2,3)), list(NA,2,2))");
    }
}
