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
package com.oracle.truffle.r.nodes.builtin.stats;

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.rng.*;

/**
 * TODO GnuR checks/updates {@code .Random.seed} across this call.
 */
@RBuiltin(name = "runif", kind = SUBSTITUTE, parameterNames = {"n", "min", "max"})
public abstract class Runif extends RBuiltinNode {

    @Override
    public RNode[] getParameterValues() {
        // n, min = 0, max = 1
        return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(0), ConstantNode.create(1)};
    }

    @Specialization
    public RDoubleVector runif(int n) {
        controlVisibility();
        double[] result = new double[n];
        for (int i = 0; i < n; i++) {
            result[i] = RRNG.unifRand();
        }
        return RDataFactory.createDoubleVector(result, RDataFactory.COMPLETE_VECTOR);
    }

    @Specialization
    public RDoubleVector runif(double d) {
        controlVisibility();
        return runif((int) d);
    }
}
