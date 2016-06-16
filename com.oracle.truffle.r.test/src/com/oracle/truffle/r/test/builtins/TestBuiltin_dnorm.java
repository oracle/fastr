/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

public class TestBuiltin_dnorm extends TestBase {
    @Test
    public void testDnorm() {
        assertEval("dnorm(c(0,1,-1,10,-10,3.14,0.1), log=FALSE)");
        assertEval("dnorm(c(0,1,-1,10,-10,3.14,0.1), log=TRUE)");
        assertEval("dnorm(c(0,1,-1,10,-10,3.14,0.1), mean=3)");
    }

    @Test
    public void testDnormSigma() {
        assertEval("dnorm(c(0,1,-1,10,-10,3.14,0.1), sd=0.5)");
        assertEval("dnorm(c(0,1,-1,10,-10,3.14,0.1), sd=0)");
        assertEval("dnorm(c(0,1,-1,10,-10,3.14,0.1), sd=-1)");
        assertEval("dnorm(42, mean=42, sd=-1)");
    }

    @Test
    public void testDnormWithInfinity() {
        assertEval("dnorm(1/0)");
        assertEval("dnorm(10, mean=1/0)");
        assertEval("dnorm(10, sd=1/0)");
    }
}
