/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.anyValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.constant;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.numericValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.runtime.RError.Message.INVALID_ARGUMENT;
import static com.oracle.truffle.r.runtime.RError.Message.INVALID_NORMAL_TYPE_IN_RGNKIND;
import static com.oracle.truffle.r.runtime.RError.Message.SEED_NOT_VALID_INT;
import static com.oracle.truffle.r.runtime.RError.Message.UNIMPLEMENTED_TYPE_IN_FUNCTION;
import static com.oracle.truffle.r.runtime.RVisibility.OFF;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.MODIFIES_STATE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.NodeWithArgumentCasts.Casts;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.rng.RRNG;

public class RNGFunctions {
    @RBuiltin(name = "set.seed", visibility = OFF, kind = INTERNAL, parameterNames = {"seed", "kind", "normal.kind"}, behavior = MODIFIES_STATE)
    public abstract static class SetSeed extends RBuiltinNode.Arg3 {

        static {
            Casts casts = new Casts(SetSeed.class);
            casts.arg("seed").allowNull().mustBe(numericValue(), SEED_NOT_VALID_INT).asIntegerVector().findFirst();
            CastsHelper.kindInteger(casts, "kind", INVALID_ARGUMENT, "kind");
            // TODO: implement normal.kind specializations with String
            casts.arg("normal.kind").allowNull().mustBe(anyValue().not(), UNIMPLEMENTED_TYPE_IN_FUNCTION, "String", "set.seed").mustBe(stringValue(), INVALID_NORMAL_TYPE_IN_RGNKIND);
        }

        @SuppressWarnings("unused")
        @Specialization
        protected RNull setSeed(int seed, int kind, RNull normKind) {
            doSetSeed(seed, kind, RRNG.NO_KIND_CHANGE);
            return RNull.instance;
        }

        @SuppressWarnings("unused")
        @Specialization
        protected RNull setSeed(RNull seed, int kind, RNull normKind) {
            doSetSeed(RRNG.timeToSeed(), kind, RRNG.NO_KIND_CHANGE);
            return RNull.instance;
        }

        private static void doSetSeed(int newSeed, int kind, int normKind) {
            RRNG.doSetSeed(newSeed, kind, normKind);
        }
    }

    @RBuiltin(name = "RNGkind", kind = INTERNAL, parameterNames = {"kind", "normkind"}, behavior = MODIFIES_STATE)
    public abstract static class RNGkind extends RBuiltinNode.Arg2 {

        static {
            Casts casts = new Casts(RNGkind.class);
            CastsHelper.kindInteger(casts, "kind", INVALID_ARGUMENT, "kind");
            CastsHelper.kindInteger(casts, "normkind", INVALID_NORMAL_TYPE_IN_RGNKIND);
        }

        @Specialization
        protected RIntVector doRNGkind(int kind, int normKind) {
            RRNG.getRNGState();
            RIntVector result = getCurrent();
            RRNG.doRNGKind(kind, normKind);
            return result;
        }

        private static RIntVector getCurrent() {
            return RDataFactory.createIntVector(new int[]{RRNG.currentKindAsInt(), RRNG.currentNormKindAsInt()}, RDataFactory.COMPLETE_VECTOR);
        }
    }

    @RBuiltin(name = ".fastr.set.seed", visibility = OFF, kind = PRIMITIVE, parameterNames = {"data"}, behavior = MODIFIES_STATE)
    public abstract static class FastRSetSeed extends RBuiltinNode.Arg1 {

        static {
            Casts.noCasts(FastRSetSeed.class);
        }

        @Specialization
        @TruffleBoundary
        protected RNull setSeed(int[] data) {
            RContext.getInstance().stateRNG.currentSeeds = data;
            return RNull.instance;
        }

        @Specialization
        @TruffleBoundary
        protected RNull setSeed(RAbstractIntVector data) {
            int[] arr = new int[data.getLength()];
            for (int i = 0; i < arr.length; i++) {
                arr[i] = data.getDataAt(i);
            }
            RContext.getInstance().stateRNG.currentSeeds = arr;
            return RNull.instance;
        }

        protected boolean isSetOperation(Object param) {
            return !(param instanceof RMissing);
        }

        @Specialization(guards = {"isSetOperation(data)"})
        @TruffleBoundary
        protected RNull setSeed(Object data) {
            RContext.getInstance().stateRNG.currentSeeds = data;
            return RNull.instance;
        }

        @Specialization
        @TruffleBoundary
        protected Object getSeed(@SuppressWarnings("unused") RMissing data) {
            Object seeds = RContext.getInstance().stateRNG.currentSeeds;
            if (seeds instanceof int[]) {
                int[] seedsArr = (int[]) seeds;
                return RDataFactory.createIntVector(seedsArr, RDataFactory.INCOMPLETE_VECTOR);
            }
            return seeds;
        }
    }

    private static final class CastsHelper {
        public static void kindInteger(Casts casts, String name, Message error, Object... messageArgs) {
            casts.arg(name).mapNull(constant(RRNG.NO_KIND_CHANGE)).mustBe(numericValue(), error, messageArgs).asIntegerVector().findFirst();
        }
    }
}
