/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.anyValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.constant;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.numericValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.runtime.RError.Message.INVALID_ARGUMENT;
import static com.oracle.truffle.r.runtime.RError.Message.INVALID_NORMAL_TYPE_IN_RGNKIND;
import static com.oracle.truffle.r.runtime.RError.Message.SEED_NOT_VALID_INT;
import static com.oracle.truffle.r.runtime.RError.Message.UNIMPLEMENTED_TYPE_IN_FUNCTION;
import static com.oracle.truffle.r.runtime.RVisibility.CUSTOM;
import static com.oracle.truffle.r.runtime.RVisibility.OFF;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.MODIFIES_STATE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.r.nodes.builtin.NodeWithArgumentCasts.Casts;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.function.visibility.SetVisibilityNode;
import com.oracle.truffle.r.runtime.RError.Message;
import static com.oracle.truffle.r.runtime.RError.Message.INVALID_SAMPLE_TYPE_IN_RGNKIND;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RIntVector;
import com.oracle.truffle.r.runtime.rng.RRNG;

public class RNGFunctions {
    @RBuiltin(name = "set.seed", visibility = OFF, kind = INTERNAL, parameterNames = {"seed", "kind", "normal.kind", "sample.kind"}, behavior = MODIFIES_STATE)
    public abstract static class SetSeed extends RBuiltinNode.Arg4 {

        static {
            Casts casts = new Casts(SetSeed.class);
            casts.arg("seed").allowNull().asIntegerVector().findFirst().mustNotBeNA(SEED_NOT_VALID_INT);
            CastsHelper.kindInteger(casts, "kind", INVALID_ARGUMENT, "kind");
            // TODO: implement normal.kind specializations with String
            casts.arg("normal.kind").allowNull().mustBe(anyValue().not(), UNIMPLEMENTED_TYPE_IN_FUNCTION, "normal.kind", "set.seed").mustBe(stringValue(), INVALID_NORMAL_TYPE_IN_RGNKIND);
            casts.arg("sample.kind").allowNull().asIntegerVector().findFirst();
        }

        @SuppressWarnings("unused")
        @Specialization
        @TruffleBoundary
        protected RNull setSeed(int seed, int kind, RNull normKind, RNull sampleKind) {
            doSetSeed(seed, kind, RRNG.NO_KIND_CHANGE, RRNG.NO_KIND_CHANGE);
            return RNull.instance;
        }

        @SuppressWarnings("unused")
        @Specialization
        @TruffleBoundary
        protected RNull setSeed(int seed, int kind, RNull normKind, int sampleKind) {
            doSetSeed(seed, kind, RRNG.NO_KIND_CHANGE, sampleKind);
            return RNull.instance;
        }

        @SuppressWarnings("unused")
        @Specialization
        @TruffleBoundary
        protected RNull setSeed(RNull seed, int kind, RNull normKind, RNull sampleKind) {
            doSetSeed(RRNG.timeToSeed(), kind, RRNG.NO_KIND_CHANGE, RRNG.NO_KIND_CHANGE);
            return RNull.instance;
        }

        private static void doSetSeed(int newSeed, int kind, int normKind, int sampleKind) {
            RRNG.doSetSeed(newSeed, kind, normKind, sampleKind);
        }
    }

    @RBuiltin(name = "RNGkind", kind = INTERNAL, parameterNames = {"kind", "normkind", "sample.kind"}, behavior = MODIFIES_STATE)
    public abstract static class RNGkind extends RBuiltinNode.Arg3 {

        static {
            Casts casts = new Casts(RNGkind.class);
            CastsHelper.kindInteger(casts, "kind", INVALID_ARGUMENT, "kind");
            CastsHelper.kindInteger(casts, "normkind", INVALID_NORMAL_TYPE_IN_RGNKIND);
            CastsHelper.kindInteger(casts, "sample.kind", INVALID_SAMPLE_TYPE_IN_RGNKIND);
        }

        @Specialization
        @TruffleBoundary
        protected com.oracle.truffle.r.runtime.data.RIntVector doRNGkind(int kind, int normKind, int sampleKind) {
            RRNG.getRNGState();
            com.oracle.truffle.r.runtime.data.RIntVector result = getCurrent();
            RRNG.doRNGKind(kind, normKind, sampleKind);
            return result;
        }

        private static com.oracle.truffle.r.runtime.data.RIntVector getCurrent() {
            return RDataFactory.createIntVector(new int[]{RRNG.currentKindAsInt(), RRNG.currentNormKindAsInt(), RRNG.currentSampleKindAsInt()}, RDataFactory.COMPLETE_VECTOR);
        }
    }

    @RBuiltin(name = ".fastr.set.seed", visibility = CUSTOM, kind = PRIMITIVE, parameterNames = {"data"}, behavior = MODIFIES_STATE)
    public abstract static class FastRSetSeed extends RBuiltinNode.Arg1 {

        static {
            Casts.noCasts(FastRSetSeed.class);
        }

        @Child private SetVisibilityNode visibility = SetVisibilityNode.create();

        @Specialization
        protected RNull setSeed(VirtualFrame frame, RIntVector data) {
            int[] arr = new int[data.getLength()];
            for (int i = 0; i < arr.length; i++) {
                arr[i] = data.getDataAt(i);
            }
            setCurrentSeeds(arr);
            visibility.execute(frame, false);
            return RNull.instance;
        }

        protected boolean isSetOperation(Object param) {
            return !(param instanceof RMissing);
        }

        @Specialization(guards = {"isSetOperation(data)"})
        protected RNull setSeed(VirtualFrame frame, Object data) {
            setCurrentSeeds(data);
            visibility.execute(frame, false);
            return RNull.instance;
        }

        @TruffleBoundary
        private static void setCurrentSeeds(Object data) {
            RContext.getInstance().stateRNG.setCurrentSeeds(data);
        }

        @Specialization
        protected Object getSeed(VirtualFrame frame, @SuppressWarnings("unused") RMissing data) {
            Object seeds = RContext.getInstance().stateRNG.getCurrentSeeds();
            assert seeds != RMissing.instance;
            if (seeds instanceof int[]) {
                int[] seedsArr = (int[]) seeds;
                return RDataFactory.createIntVector(seedsArr, RDataFactory.INCOMPLETE_VECTOR);
            }
            visibility.execute(frame, true);
            if (seeds == null) {
                return RNull.instance;
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
