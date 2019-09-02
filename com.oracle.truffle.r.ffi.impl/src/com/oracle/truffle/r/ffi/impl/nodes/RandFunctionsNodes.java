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
import com.oracle.truffle.api.dsl.GenerateUncached;
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
import com.oracle.truffle.r.runtime.nmath.MathFunctions.Function2_1;
import com.oracle.truffle.r.runtime.nmath.MathFunctions.Function2_2;
import com.oracle.truffle.r.runtime.nmath.MathFunctions.Function3_1;
import com.oracle.truffle.r.runtime.nmath.MathFunctions.Function3_2;
import com.oracle.truffle.r.runtime.nmath.MathFunctions.Function4_1;
import com.oracle.truffle.r.runtime.nmath.MathFunctions.Function4_2;
import com.oracle.truffle.r.runtime.nmath.RandomFunctions.RandFunction1_Double;
import com.oracle.truffle.r.runtime.nmath.RandomFunctions.RandFunction2_Double;
import com.oracle.truffle.r.runtime.nmath.RandomFunctions.RandFunction3_DoubleBase;
import com.oracle.truffle.r.runtime.nmath.RandomFunctions.RandomNumberProvider;
import com.oracle.truffle.r.runtime.nmath.RMultinom;
import com.oracle.truffle.r.runtime.nmath.distr.Rbinom;

public final class RandFunctionsNodes {

    @GenerateUncached
    public abstract static class RandFunction3_2Node extends FFIUpCallNode.Arg6 {
        @Specialization
        protected double evaluate(Function3_2 delegate, double a, double b, double c, int d, int e) {
            return delegate.evaluate(a, b, c, d != 0, e != 0);
        }

        public static RandFunction3_2Node create() {
            return RandFunctionsNodesFactory.RandFunction3_2NodeGen.create();
        }
    }

    @GenerateUncached
    public abstract static class RandFunction3_1Node extends FFIUpCallNode.Arg5 {
        @Specialization
        protected double evaluate(Function3_1 delegate, double a, double b, double c, int d) {
            return delegate.evaluate(a, b, c, d != 0);
        }

        public static RandFunction3_1Node create() {
            return RandFunctionsNodesFactory.RandFunction3_1NodeGen.create();
        }
    }

    @GenerateUncached
    public abstract static class RandFunction3Node extends FFIUpCallNode.Arg4 {
        @Specialization
        protected double evaluate(RandFunction3_DoubleBase delegate, double a, double b, double c) {
            return delegate.execute(a, b, c, RandomNumberProvider.fromCurrentRNG());
        }

        public static RandFunction3Node create() {
            return RandFunctionsNodesFactory.RandFunction3NodeGen.create();
        }
    }

    @GenerateUncached
    public abstract static class RandFunction2Node extends FFIUpCallNode.Arg3 {

        @Specialization
        protected double evaluate(RandFunction2_Double delegate, double a, double b) {
            return delegate.execute(a, b, RandomNumberProvider.fromCurrentRNG());
        }

        public static RandFunction2Node create() {
            return RandFunctionsNodesFactory.RandFunction2NodeGen.create();
        }
    }

    @GenerateUncached
    public abstract static class RandFunction1Node extends FFIUpCallNode.Arg2 {
        @Specialization
        protected double evaluate(RandFunction1_Double delegate, double a) {
            return delegate.execute(a, RandomNumberProvider.fromCurrentRNG());
        }

        public static RandFunction1Node create() {
            return RandFunctionsNodesFactory.RandFunction1NodeGen.create();
        }
    }

    @GenerateUncached
    public abstract static class RandFunction2_1Node extends FFIUpCallNode.Arg4 {
        @Specialization
        protected double evaluate(Function2_1 delegate, double a, double b, int c) {
            return delegate.evaluate(a, b, c != 0);
        }

        public static RandFunction2_1Node create() {
            return RandFunctionsNodesFactory.RandFunction2_1NodeGen.create();
        }
    }

    @GenerateUncached
    public abstract static class RandFunction2_2Node extends FFIUpCallNode.Arg5 {
        @Specialization
        protected double evaluate(Function2_2 delegate, double a, double b, int c, int d) {
            return delegate.evaluate(a, b, c != 0, d != 0);
        }

        public static RandFunction2_2Node create() {
            return RandFunctionsNodesFactory.RandFunction2_2NodeGen.create();
        }
    }

    @GenerateUncached
    public abstract static class RandFunction4_1Node extends FFIUpCallNode.Arg6 {
        @Specialization
        protected double evaluate(Function4_1 delegate, double a, double b, double c, double d, int e) {
            return delegate.evaluate(a, b, c, d, e != 0);
        }

        public static RandFunction4_1Node create() {
            return RandFunctionsNodesFactory.RandFunction4_1NodeGen.create();
        }
    }

    @GenerateUncached
    public abstract static class RandFunction4_2Node extends FFIUpCallNode.Arg7 {
        @Specialization
        protected double evaluate(Function4_2 delegate, double a, double b, double c, double d, int e, int f) {
            return delegate.evaluate(a, b, c, d, e != 0, f != 0);
        }

        public static RandFunction4_2Node create() {
            return RandFunctionsNodesFactory.RandFunction4_2NodeGen.create();
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
            if (probInterop.hasArrayElements(prob)) {
                if (probConvertForeign == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    probConvertForeign = insert(ConvertForeignObjectNode.create());
                }
                probVector = (RAbstractDoubleVector) probConvertForeign.convert((TruffleObject) prob);
            } else {
                if (!probInterop.isPointer(prob)) {
                    probInterop.toNative(prob);
                }
                long addr;
                try {
                    addr = probInterop.asPointer(prob);
                    probVector = RDataFactory.createDoubleVectorFromNative(addr, k);
                } catch (UnsupportedMessageException e) {
                    throw RInternalError.shouldNotReachHere("IS_POINTER message returned true, AS_POINTER should not fail");
                }
            }

            RAbstractIntVector rNVector;
            if (rNInterop.hasArrayElements(rN)) {
                if (rNConvertForeign == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    rNConvertForeign = insert(ConvertForeignObjectNode.create());
                }
                rNVector = (RAbstractIntVector) rNConvertForeign.convert((TruffleObject) rN);
            } else {
                if (!rNInterop.isPointer(rN)) {
                    rNInterop.toNative(rN);
                }
                long addr;
                try {
                    addr = rNInterop.asPointer(rN);
                    rNVector = RDataFactory.createIntVectorFromNative(addr, k);
                } catch (UnsupportedMessageException e) {
                    throw RInternalError.shouldNotReachHere("IS_POINTER message returned true, AS_POINTER should not fail");
                }
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
                        @Cached() Rbinom rbinom) {
            int[] rNArr = new int[k];
            RMultinom.rmultinom(n, probAccess.access(prob), probAccess, 1d, rNArr, 0, RandomNumberProvider.fromCurrentRNG(), rbinom);
            int i = 0;
            for (SequentialIterator rNIter = rNAccess.access(rN); rNAccess.next(rNIter);) {
                rNAccess.setInt(rNIter, rNArr[i++]);
            }
        }

        @Specialization(replaces = "doRMultinom")
        protected void doGeneric(int n, RAbstractDoubleVector prob, int k, RAbstractIntVector rN,
                        @Cached() Rbinom rbinom) {
            doRMultinom(n, prob, k, rN, prob.slowPathAccess(), rN.slowPathAccess(), rbinom);
        }

    }
}
