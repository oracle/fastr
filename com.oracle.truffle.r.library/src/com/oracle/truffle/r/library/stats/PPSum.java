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

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.library.stats.PPSumFactory.IntgrtVecNodeGen;
import com.oracle.truffle.r.library.stats.PPSumFactory.PPSumExternalNodeGen;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDataFactory.VectorFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess.RandomIterator;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess.SequentialIterator;

public abstract class PPSum {

    public abstract static class PPSumExternal extends RExternalBuiltinNode.Arg2 {
        static {
            Casts casts = new Casts(PPSumExternal.class);
            casts.arg(0).asDoubleVector();
            casts.arg(1).asIntegerVector().findFirst();
        }

        @Specialization(guards = "uAccess.supports(u)")
        protected RDoubleVector doPPSum(RAbstractDoubleVector u, int sl,
                        @Cached("create()") VectorFactory factory,
                        @Cached("u.access()") VectorAccess uAccess) {

            RandomIterator uIter = uAccess.randomAccess(u);
            int n = uAccess.getLength(uIter);
            double tmp1 = 0.0;
            for (int i = 1; i <= sl; i++) {
                double tmp2 = 0.0;
                for (int j = i; j < n; j++) {
                    tmp2 += uAccess.getDouble(uIter, j) * uAccess.getDouble(uIter, j - i);
                }
                tmp2 *= 1.0 - i / (sl + 1.0);
                tmp1 += tmp2;
            }
            return factory.createDoubleVectorFromScalar(2.0 * tmp1 / n);
        }

        @Specialization(replaces = "doPPSum")
        protected RDoubleVector doPPSumGeneric(RAbstractDoubleVector u, int sl,
                        @Cached("create()") VectorFactory factory) {
            return doPPSum(u, sl, factory, u.slowPathAccess());
        }

        public static PPSumExternal create() {
            return PPSumExternalNodeGen.create();
        }
    }

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
