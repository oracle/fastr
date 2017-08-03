package com.oracle.truffle.r.ffi.impl.nodes;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.runtime.nmath.MathFunctions;
import com.oracle.truffle.r.runtime.nmath.RandomFunctions;
import com.oracle.truffle.r.runtime.nmath.RandomFunctions.RandFunction2_Double;
import com.oracle.truffle.r.runtime.nmath.RandomFunctions.RandomNumberProvider;

public final class RandFunctionsNodes {

    public abstract static class RandFunction3_2Node extends FFIUpCallNode.Arg5 {
        @Child private MathFunctions.Function3_2 inner;

        protected RandFunction3_2Node(MathFunctions.Function3_2 inner) {
            this.inner = inner;
        }

        @Specialization
        protected double evaluate(double a, double b, double c, boolean d, boolean e) {
            return inner.evaluate(a, b, c, d, e);
        }
    }

    public abstract static class RandFunction3_1Node extends FFIUpCallNode.Arg4 {
        @Child private MathFunctions.Function3_1 inner;

        protected RandFunction3_1Node(MathFunctions.Function3_1 inner) {
            this.inner = inner;
        }

        @Specialization
        protected double evaluate(double a, double b, double c, boolean d) {
            return inner.evaluate(a, b, c, d);
        }
    }

    public abstract static class RandFunction2Node extends FFIUpCallNode.Arg2 {
        @Child private RandomFunctions.RandFunction2_Double inner;

        protected RandFunction2Node(RandFunction2_Double inner) {
            this.inner = inner;
        }

        @Specialization
        protected double evaluate(double a, double b) {
            return inner.execute(a, b, RandomNumberProvider.fromCurrentRNG());
        }
    }

}
