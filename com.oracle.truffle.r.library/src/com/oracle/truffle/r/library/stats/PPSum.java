/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.library.stats;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.library.stats.PPSumFactory.IntgrtVecNodeGen;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;

public abstract class PPSum {

    /**
     * Implementation of function 'intgrt_vec'.
     */
    public abstract static class IntgrtVecNode extends RExternalBuiltinNode.Arg3 {

        private final ValueProfile profileArgX = ValueProfile.createClassProfile();
        private final ValueProfile profileArgXi = ValueProfile.createClassProfile();

        static {
            Casts casts = new Casts(IntgrtVecNode.class);
            casts.arg(0).asDoubleVector();
            casts.arg(1).asDoubleVector();
            casts.arg(2).asIntegerVector().findFirst();
        }

        @Specialization
        protected RAbstractDoubleVector doIntegrateVector(RAbstractDoubleVector x, RAbstractDoubleVector xi, int lag) {
            RAbstractDoubleVector profiledX = profileArgX.profile(x);
            RAbstractDoubleVector profiledXi = profileArgXi.profile(xi);

            int n = profiledX.getLength();
            int nResult = n + lag;

            RDoubleVector result = (RDoubleVector) profiledXi.copyResized(nResult, false);

            double[] store = result.getInternalStore();
            for (int i = 0; i < n; i++) {
                store[i + lag] = profiledX.getDataAt(i) + store[i];
            }
            return result;
        }

        public static IntgrtVecNode create() {
            return IntgrtVecNodeGen.create();
        }
    }

}
