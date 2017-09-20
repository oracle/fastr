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
