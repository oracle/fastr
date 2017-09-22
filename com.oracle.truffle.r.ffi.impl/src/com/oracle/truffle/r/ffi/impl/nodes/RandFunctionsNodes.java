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
package com.oracle.truffle.r.ffi.impl.nodes;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.runtime.nmath.MathFunctions;
import com.oracle.truffle.r.runtime.nmath.MathFunctions.Function3_1;
import com.oracle.truffle.r.runtime.nmath.MathFunctions.Function3_2;
import com.oracle.truffle.r.runtime.nmath.RandomFunctions;
import com.oracle.truffle.r.runtime.nmath.RandomFunctions.RandFunction2_Double;
import com.oracle.truffle.r.runtime.nmath.RandomFunctions.RandomNumberProvider;
import com.oracle.truffle.r.runtime.nmath.distr.Unif;

public final class RandFunctionsNodes {

    abstract static class RandFunction3_2Node extends FFIUpCallNode.Arg5 {
        private final MathFunctions.Function3_2 inner;

        protected RandFunction3_2Node(MathFunctions.Function3_2 inner) {
            this.inner = inner;
        }

        @Specialization
        protected double evaluate(double a, double b, double c, boolean d, boolean e) {
            return inner.evaluate(a, b, c, d, e);
        }
    }

    abstract static class RandFunction3_1Node extends FFIUpCallNode.Arg4 {
        private final MathFunctions.Function3_1 inner;

        protected RandFunction3_1Node(MathFunctions.Function3_1 inner) {
            this.inner = inner;
        }

        @Specialization
        protected double evaluate(double a, double b, double c, boolean d) {
            return inner.evaluate(a, b, c, d);
        }
    }

    abstract static class RandFunction2Node extends FFIUpCallNode.Arg2 {
        @Child private RandomFunctions.RandFunction2_Double inner;

        protected RandFunction2Node(RandFunction2_Double inner) {
            this.inner = inner;
        }

        @Specialization
        protected double evaluate(double a, double b) {
            return inner.execute(a, b, RandomNumberProvider.fromCurrentRNG());
        }
    }

    public abstract static class RunifNode extends RandFunction2Node {

        protected RunifNode(RandFunction2_Double inner) {
            super(inner);
        }

        public static RunifNode create() {
            return RandFunctionsNodesFactory.RunifNodeGen.create(new Unif.Runif());
        }

    }

    public abstract static class DunifNode extends RandFunction3_1Node {

        protected DunifNode(Function3_1 inner) {
            super(inner);
        }

        public static DunifNode create() {
            return RandFunctionsNodesFactory.DunifNodeGen.create(new Unif.DUnif());
        }

    }

    public abstract static class QunifNode extends RandFunction3_2Node {

        protected QunifNode(Function3_2 inner) {
            super(inner);
        }

        public static QunifNode create() {
            return RandFunctionsNodesFactory.QunifNodeGen.create(new Unif.QUnif());
        }

    }

    public abstract static class PunifNode extends RandFunction3_2Node {

        protected PunifNode(Function3_2 inner) {
            super(inner);
        }

        public static PunifNode create() {
            return RandFunctionsNodesFactory.PunifNodeGen.create(new Unif.PUnif());
        }

    }
}
