/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.runtime.RVisibility.OFF;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.MODIFIES_STATE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.rng.RRNG;

public class RNGFunctions {
    @RBuiltin(name = "set.seed", visibility = OFF, kind = INTERNAL, parameterNames = {"seed", "kind", "normal.kind"}, behavior = MODIFIES_STATE)
    public abstract static class SetSeed extends RBuiltinNode {

        @SuppressWarnings("unused")
        @Specialization
        protected RNull setSeed(double seed, RNull kind, RNull normKind) {
            doSetSeed((int) seed, RRNG.NO_KIND_CHANGE, RRNG.NO_KIND_CHANGE);
            return RNull.instance;
        }

        @SuppressWarnings("unused")
        @Specialization
        protected RNull setSeed(double seed, RAbstractIntVector kind, RNull normKind) {
            doSetSeed((int) seed, kind.getDataAt(0), RRNG.NO_KIND_CHANGE);
            return RNull.instance;
        }

        @SuppressWarnings("unused")
        @Specialization
        protected RNull setSeed(RNull seed, RNull kind, RNull normKind) {
            doSetSeed(RRNG.timeToSeed(), RRNG.NO_KIND_CHANGE, RRNG.NO_KIND_CHANGE);
            return RNull.instance;
        }

        @SuppressWarnings("unused")
        @Specialization
        protected RNull setSeed(byte seed, RNull kind, RNull normKind) {
            CompilerDirectives.transferToInterpreter();
            throw RError.error(this, RError.Message.SEED_NOT_VALID_INT);
        }

        private static void doSetSeed(int newSeed, int kind, int normKind) {
            RRNG.doSetSeed(newSeed, kind, normKind);
        }
    }

    @RBuiltin(name = "RNGkind", kind = INTERNAL, parameterNames = {"kind", "normkind"}, behavior = MODIFIES_STATE)
    public abstract static class RNGkind extends RBuiltinNode {

        @Specialization
        protected RIntVector doRNGkind(Object kind, Object normKind) {
            RRNG.getRNGState();
            RIntVector result = getCurrent();
            int kindChange = checkType(kind, "kind");
            int normKindChange = checkType(normKind, "normkind");
            RRNG.doRNGKind(kindChange, normKindChange);
            return result;
        }

        private int checkType(Object kind, String name) {
            if (!(kind == RNull.instance || kind instanceof RIntVector || kind instanceof Integer)) {
                throw RError.error(this, RError.Message.INVALID_ARGUMENT, name);
            }
            return kind == RNull.instance ? RRNG.NO_KIND_CHANGE : kind instanceof Integer ? (Integer) kind : ((RAbstractIntVector) kind).getDataAt(0);
        }

        private static RIntVector getCurrent() {
            return RDataFactory.createIntVector(new int[]{RRNG.currentKindAsInt(), RRNG.currentNormKindAsInt()}, RDataFactory.COMPLETE_VECTOR);
        }
    }
}
