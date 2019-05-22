/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.ffi.impl.nodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess.SequentialIterator;
import com.oracle.truffle.r.runtime.interop.ConvertForeignObjectNode;
import com.oracle.truffle.r.runtime.nmath.MathFunctions;
import com.oracle.truffle.r.runtime.nmath.MathFunctions.Function2_1;
import com.oracle.truffle.r.runtime.nmath.MathFunctions.Function2_2;
import com.oracle.truffle.r.runtime.nmath.MathFunctions.Function3_1;
import com.oracle.truffle.r.runtime.nmath.MathFunctions.Function3_2;
import com.oracle.truffle.r.runtime.nmath.MathFunctions.Function4_1;
import com.oracle.truffle.r.runtime.nmath.MathFunctions.Function4_2;
import com.oracle.truffle.r.runtime.nmath.RandomFunctions.RandFunction1_Double;
import com.oracle.truffle.r.runtime.nmath.RandomFunctions.RandFunction2_Double;
import com.oracle.truffle.r.runtime.nmath.RandomFunctions.RandFunction3_Double;
import com.oracle.truffle.r.runtime.nmath.RandomFunctions.RandomNumberProvider;
import com.oracle.truffle.r.runtime.nmath.RMultinom;
import com.oracle.truffle.r.runtime.nmath.distr.Rbinom;

public final class RandFunctionsNodes {

    public abstract static class RandFunction3_2Node extends FFIUpCallNode.Arg5 {
        private final Function3_2 inner;

        protected RandFunction3_2Node(MathFunctions.Function3_2 inner) {
            this.inner = inner;
        }

        @Specialization
        protected double evaluate(double a, double b, double c, int d, int e) {
            return inner.evaluate(a, b, c, d != 0, e != 0);
        }

        public static RandFunction3_2Node create(Function3_2 inner) {
            return RandFunctionsNodesFactory.RandFunction3_2NodeGen.create(inner);
        }

    }

    public abstract static class RandFunction3_1Node extends FFIUpCallNode.Arg4 {
        private final Function3_1 inner;

        protected RandFunction3_1Node(MathFunctions.Function3_1 inner) {
            this.inner = inner;
        }

        @Specialization
        protected double evaluate(double a, double b, double c, int d) {
            return inner.evaluate(a, b, c, d != 0);
        }

        public static RandFunction3_1Node create(Function3_1 inner) {
            return RandFunctionsNodesFactory.RandFunction3_1NodeGen.create(inner);
        }
    }

    public abstract static class RandFunction3Node extends FFIUpCallNode.Arg3 {
        @Child private RandFunction3_Double inner;

        protected RandFunction3Node(RandFunction3_Double inner) {
            this.inner = inner;
        }

        @Specialization
        protected double evaluate(double a, double b, double c) {
            return inner.execute(a, b, c, RandomNumberProvider.fromCurrentRNG());
        }

        public static RandFunction3Node create(RandFunction3_Double inner) {
            return RandFunctionsNodesFactory.RandFunction3NodeGen.create(inner);
        }
    }

    public abstract static class RandFunction2Node extends FFIUpCallNode.Arg2 {
        @Child private RandFunction2_Double inner;

        protected RandFunction2Node(RandFunction2_Double inner) {
            this.inner = inner;
        }

        @Specialization
        protected double evaluate(double a, double b) {
            return inner.execute(a, b, RandomNumberProvider.fromCurrentRNG());
        }

        public static RandFunction2Node create(RandFunction2_Double inner) {
            return RandFunctionsNodesFactory.RandFunction2NodeGen.create(inner);
        }
    }

    public abstract static class RandFunction1Node extends FFIUpCallNode.Arg1 {
        @Child private RandFunction1_Double inner;

        protected RandFunction1Node(RandFunction1_Double inner) {
            this.inner = inner;
        }

        @Specialization
        protected double evaluate(double a) {
            return inner.execute(a, RandomNumberProvider.fromCurrentRNG());
        }

        public static RandFunction1Node create(RandFunction1_Double inner) {
            return RandFunctionsNodesFactory.RandFunction1NodeGen.create(inner);
        }
    }

    public abstract static class RandFunction2_1Node extends FFIUpCallNode.Arg3 {
        private final Function2_1 inner;

        protected RandFunction2_1Node(MathFunctions.Function2_1 inner) {
            this.inner = inner;
        }

        @Specialization
        protected double evaluate(double a, double b, int c) {
            return inner.evaluate(a, b, c != 0);
        }

        public static RandFunction2_1Node create(Function2_1 inner) {
            return RandFunctionsNodesFactory.RandFunction2_1NodeGen.create(inner);
        }
    }

    public abstract static class RandFunction2_2Node extends FFIUpCallNode.Arg4 {
        private final Function2_2 inner;

        protected RandFunction2_2Node(MathFunctions.Function2_2 inner) {
            this.inner = inner;
        }

        @Specialization
        protected double evaluate(double a, double b, int c, int d) {
            return inner.evaluate(a, b, c != 0, d != 0);
        }

        public static RandFunction2_2Node create(Function2_2 inner) {
            return RandFunctionsNodesFactory.RandFunction2_2NodeGen.create(inner);
        }
    }

    public abstract static class RandFunction4_1Node extends FFIUpCallNode.Arg5 {
        private final Function4_1 inner;

        protected RandFunction4_1Node(MathFunctions.Function4_1 inner) {
            this.inner = inner;
        }

        @Specialization
        protected double evaluate(double a, double b, double c, double d, int e) {
            return inner.evaluate(a, b, c, d, e != 0);
        }

        public static RandFunction4_1Node create(Function4_1 inner) {
            return RandFunctionsNodesFactory.RandFunction4_1NodeGen.create(inner);
        }
    }

    public abstract static class RandFunction4_2Node extends FFIUpCallNode.Arg6 {
        private final Function4_2 inner;

        protected RandFunction4_2Node(MathFunctions.Function4_2 inner) {
            this.inner = inner;
        }

        @Specialization
        protected double evaluate(double a, double b, double c, double d, int e, int f) {
            return inner.evaluate(a, b, c, d, e != 0, f != 0);
        }

        public static RandFunction4_2Node create(Function4_2 inner) {
            return RandFunctionsNodesFactory.RandFunction4_2NodeGen.create(inner);
        }
    }

    @ImportStatic(DSLConfig.class)
    public abstract static class RfRMultinomNode extends FFIUpCallNode.Arg4 {

        @Child private ConvertForeignObjectNode probConvertForeign;
        @Child private ConvertForeignObjectNode rNConvertForeign;
        @Child private DoRMultinomNode doRMultinomNode = RandFunctionsNodesFactory.DoRMultinomNodeGen.create();

        @Specialization(limit = "getInteropLibraryCacheSize()")
        protected Object evaluate(int n, Object prob, int k, Object rN,
                        @CachedLibrary("prob") InteropLibrary probInterop,
                        @CachedLibrary("rN") InteropLibrary rNInterop) {
            // prob is double* and rN is int*
            // Return a vector data in rN rN[1:K] {K := length(prob)}
            RAbstractDoubleVector probVector;
            if (probInterop.isPointer(prob)) {
                long addr;
                try {
                    addr = probInterop.asPointer(prob);
                    probVector = RDataFactory.createDoubleVectorFromNative(addr, k);
                } catch (UnsupportedMessageException e) {
                    throw RInternalError.shouldNotReachHere("IS_POINTER message returned true, AS_POINTER should not fail");
                }
            } else {
                if (probConvertForeign == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    probConvertForeign = insert(ConvertForeignObjectNode.create());
                }
                probVector = (RAbstractDoubleVector) probConvertForeign.convert((TruffleObject) prob);
            }

            RAbstractIntVector rNVector;
            if (rNInterop.isPointer(rN)) {
                long addr;
                try {
                    addr = rNInterop.asPointer(rN);
                    rNVector = RDataFactory.createIntVectorFromNative(addr, k);
                } catch (UnsupportedMessageException e) {
                    throw RInternalError.shouldNotReachHere("IS_POINTER message returned true, AS_POINTER should not fail");
                }
            } else {
                if (rNConvertForeign == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    rNConvertForeign = insert(ConvertForeignObjectNode.create());
                }
                rNVector = (RAbstractIntVector) rNConvertForeign.convert((TruffleObject) rN);
            }

            doRMultinomNode.execute(n, probVector, k, rNVector);
            return RNull.instance;
        }

        public static RfRMultinomNode create() {
            return RandFunctionsNodesFactory.RfRMultinomNodeGen.create();
        }
    }

    abstract static class DoRMultinomNode extends Node {

        public abstract void execute(int n, RAbstractDoubleVector prob, int k, RAbstractIntVector rN);

        @Specialization(guards = {"probAccess.supports(prob)", "rNAccess.supports(rN)"})
        protected void doRMultinom(int n, RAbstractDoubleVector prob, int k, RAbstractIntVector rN,
                        @Cached("prob.access()") VectorAccess probAccess,
                        @Cached("rN.access()") VectorAccess rNAccess,
                        @Cached("new()") Rbinom rbinom) {
            int[] rNArr = new int[k];
            RMultinom.rmultinom(n, probAccess.access(prob), probAccess, 1d, rNArr, 0, RandomNumberProvider.fromCurrentRNG(), rbinom);
            int i = 0;
            for (SequentialIterator rNIter = rNAccess.access(rN); rNAccess.next(rNIter);) {
                rNAccess.setInt(rNIter, rNArr[i++]);
            }
        }

        @Specialization(replaces = "doRMultinom")
        protected void doGeneric(int n, RAbstractDoubleVector prob, int k, RAbstractIntVector rN,
                        @Cached("new()") Rbinom rbinom) {
            doRMultinom(n, prob, k, rN, prob.slowPathAccess(), rN.slowPathAccess(), rbinom);
        }

    }
}
