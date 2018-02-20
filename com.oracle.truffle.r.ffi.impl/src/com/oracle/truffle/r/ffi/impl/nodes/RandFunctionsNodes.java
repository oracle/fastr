/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.r.runtime.nmath.MathFunctions.Function2_1;
import com.oracle.truffle.r.runtime.nmath.MathFunctions.Function2_2;
import com.oracle.truffle.r.runtime.nmath.MathFunctions.Function3_1;
import com.oracle.truffle.r.runtime.nmath.MathFunctions.Function3_2;
import com.oracle.truffle.r.runtime.nmath.RandomFunctions;
import com.oracle.truffle.r.runtime.nmath.RandomFunctions.RandFunction1_Double;
import com.oracle.truffle.r.runtime.nmath.RandomFunctions.RandFunction2_Double;
import com.oracle.truffle.r.runtime.nmath.RandomFunctions.RandomNumberProvider;
import com.oracle.truffle.r.runtime.nmath.distr.Chisq;
import com.oracle.truffle.r.runtime.nmath.distr.DNChisq;
import com.oracle.truffle.r.runtime.nmath.distr.PNChisq;
import com.oracle.truffle.r.runtime.nmath.distr.QNChisq;
import com.oracle.truffle.r.runtime.nmath.distr.RNchisq;
import com.oracle.truffle.r.runtime.nmath.distr.Unif;

public final class RandFunctionsNodes {

    abstract static class RandFunction3_2Node extends FFIUpCallNode.Arg5 {
        private final MathFunctions.Function3_2 inner;

        protected RandFunction3_2Node(MathFunctions.Function3_2 inner) {
            this.inner = inner;
        }

        @Specialization
        protected double evaluate(double a, double b, double c, int d, int e) {
            return inner.evaluate(a, b, c, d != 0, e != 0);
        }
    }

    abstract static class RandFunction3_1Node extends FFIUpCallNode.Arg4 {
        private final MathFunctions.Function3_1 inner;

        protected RandFunction3_1Node(MathFunctions.Function3_1 inner) {
            this.inner = inner;
        }

        @Specialization
        protected double evaluate(double a, double b, double c, int d) {
            return inner.evaluate(a, b, c, d != 0);
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

    abstract static class RandFunction1Node extends FFIUpCallNode.Arg1 {
        @Child private RandFunction1_Double inner;

        protected RandFunction1Node(RandFunction1_Double inner) {
            this.inner = inner;
        }

        @Specialization
        protected double evaluate(double a) {
            return inner.execute(a, RandomNumberProvider.fromCurrentRNG());
        }
    }

    abstract static class RandFunction2_1Node extends FFIUpCallNode.Arg3 {
        private final MathFunctions.Function2_1 inner;

        protected RandFunction2_1Node(MathFunctions.Function2_1 inner) {
            this.inner = inner;
        }

        @Specialization
        protected double evaluate(double a, double b, int c) {
            return inner.evaluate(a, b, c != 0);
        }
    }

    abstract static class RandFunction2_2Node extends FFIUpCallNode.Arg4 {
        private final MathFunctions.Function2_2 inner;

        protected RandFunction2_2Node(MathFunctions.Function2_2 inner) {
            this.inner = inner;
        }

        @Specialization
        protected double evaluate(double a, double b, int c, int d) {
            return inner.evaluate(a, b, c != 0, d != 0);
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

    public abstract static class DChisqNode extends RandFunction2_1Node {

        protected DChisqNode(Function2_1 inner) {
            super(inner);
        }

        public static DChisqNode create() {
            return RandFunctionsNodesFactory.DChisqNodeGen.create(new Chisq.DChisq());
        }

    }

    public abstract static class PChisqNode extends RandFunction2_2Node {

        protected PChisqNode(Function2_2 inner) {
            super(inner);
        }

        public static PChisqNode create() {
            return RandFunctionsNodesFactory.PChisqNodeGen.create(new Chisq.PChisq());
        }

    }

    public abstract static class QChisqNode extends RandFunction2_2Node {

        protected QChisqNode(Function2_2 inner) {
            super(inner);
        }

        public static QChisqNode create() {
            return RandFunctionsNodesFactory.QChisqNodeGen.create(new Chisq.QChisq());
        }

    }

    public abstract static class RChisqNode extends RandFunction1Node {

        protected RChisqNode(RandFunction1_Double inner) {
            super(inner);
        }

        public static RChisqNode create() {
            return RandFunctionsNodesFactory.RChisqNodeGen.create(new Chisq.RChisq());
        }

    }

    public abstract static class DNChisqNode extends RandFunction3_1Node {

        protected DNChisqNode(Function3_1 inner) {
            super(inner);
        }

        public static DNChisqNode create() {
            return RandFunctionsNodesFactory.DNChisqNodeGen.create(new DNChisq());
        }

    }

    public abstract static class PNChisqNode extends RandFunction3_2Node {

        protected PNChisqNode(Function3_2 inner) {
            super(inner);
        }

        public static PNChisqNode create() {
            return RandFunctionsNodesFactory.PNChisqNodeGen.create(new PNChisq());
        }

    }

    public abstract static class QNChisqNode extends RandFunction3_2Node {

        protected QNChisqNode(Function3_2 inner) {
            super(inner);
        }

        public static QNChisqNode create() {
            return RandFunctionsNodesFactory.QNChisqNodeGen.create(new QNChisq());
        }

    }

    public abstract static class RNChisqNode extends RandFunction2Node {

        protected RNChisqNode(RandFunction2_Double inner) {
            super(inner);
        }

        public static RNChisqNode create() {
            return RandFunctionsNodesFactory.RNChisqNodeGen.create(new RNchisq());
        }

    }

}
