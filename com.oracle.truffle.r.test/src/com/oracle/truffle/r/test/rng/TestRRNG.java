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
package com.oracle.truffle.r.test.rng;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

public class TestRRNG extends TestBase {
    @Test
    public void testDirectSeedAssignment() {
        // changes generator to MarsagliaMulticarry and sets its 2 seeds
        assertEval(".Random.seed <- c(401L, 1L, 2L); runif(3)");
        // wrong values: not integer
        assertEval(".Random.seed <- c(401, 1, 2); invisible(runif(3))");
        // wrong values: wrong generator number
        assertEval(".Random.seed <- c(999, 1, 2); invisible(runif(3))");
    }

    @Test
    public void testGeneratorChange() {
        assertEval("invisible(runif(5)); RNGkind('Marsaglia-Multicarry'); set.seed(2); runif(5);");
        assertEval("RNGkind('Marsaglia-Multicarry'); RNGkind('Mersenne-Twister'); set.seed(2); runif(5);");
    }

    @Test
    public void testDirectReadingSeed() {
        assertEval("invisible(runif(1)); length(.Random.seed)");
        assertEval("set.seed(42); .Random.seed");
    }
}
