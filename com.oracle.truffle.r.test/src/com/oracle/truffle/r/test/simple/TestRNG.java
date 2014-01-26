/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.test.simple;

import org.junit.*;

import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.test.*;

public class TestRNG extends TestBase {

    private static final double DELTA = 1e-10;

    @Test
    public void testRNG1() {
        // set.seed(0); sum(runif(10000))
        Assert.assertEquals(0.896697200136259, sum(runifWithSeed(1, 0)), DELTA);
        Assert.assertEquals(5002.52002784028, sum(runifWithSeed(10000, 0)), DELTA);
        Assert.assertEquals(4955.99995856802, sum(runifWithSeed(10000, -3)), DELTA);
        Assert.assertEquals(50308.2911445985, sum(runifWithSeed(100500, 100500)), DELTA);
    }

    private static double[] runifWithSeed(int n, int seed) {
        RRandomNumberGenerator rng = new RRandomNumberGenerator();
        rng.setSeed(seed);
        double[] result = new double[n];
        for (int i = 0; i < n; i++) {
            result[i] = rng.genrandDouble();
        }
        return result;
    }

    private static double sum(double[] values) {
        double sum = 0;
        for (int i = 0; i < values.length; i++) {
            sum += values[i];
        }
        return sum;
    }

    @Test
    public void testDefault() {
        // .Random.seed[2] = 625L; runif(10)
        RRandomNumberGenerator rng = new RRandomNumberGenerator();
        int n = 10;
        double[] result = new double[n];
        for (int i = 0; i < n; i++) {
            result[i] = rng.genrandDouble();
        }
        Assert.assertEquals(0.957454058108851, result[n - 1], DELTA);
    }
}
