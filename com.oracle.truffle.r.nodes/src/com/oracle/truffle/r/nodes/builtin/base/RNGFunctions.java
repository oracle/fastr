/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.rng.*;
import com.oracle.truffle.r.runtime.rng.RRNG.RNGException;

public class RNGFunctions {
    @RBuiltin(name = "set.seed", kind = INTERNAL)
    public abstract static class SetSeed extends RInvisibleBuiltinNode {

        @SuppressWarnings("unused")
        @Specialization(order = 0)
        public RNull setSeed(VirtualFrame frame, double seed, RNull kind, RNull normKind) {
            controlVisibility();
            doSetSeed(frame, (int) seed, RRNG.NO_KIND_CHANGE, RRNG.NO_KIND_CHANGE);
            return RNull.instance;
        }

        @SuppressWarnings("unused")
        @Specialization(order = 1)
        public RNull setSeed(VirtualFrame frame, double seed, RAbstractIntVector kind, RNull normKind) {
            controlVisibility();
            doSetSeed(frame, (int) seed, kind.getDataAt(0), RRNG.NO_KIND_CHANGE);
            return RNull.instance;
        }

        @SuppressWarnings("unused")
        @Specialization(order = 2)
        public RNull setSeed(VirtualFrame frame, RNull seed, RNull kind, RNull normKind) {
            controlVisibility();
            doSetSeed(frame, RRNG.RESET_SEED, RRNG.NO_KIND_CHANGE, RRNG.NO_KIND_CHANGE);
            return RNull.instance;
        }

        @SuppressWarnings("unused")
        @Specialization(order = 10)
        public RNull setSeed(VirtualFrame frame, byte seed, RNull kind, RNull normKind) {
            controlVisibility();
            CompilerDirectives.transferToInterpreter();
            throw RError.getGenericError(getEncapsulatingSourceSection(), "supplied seed is not a valid integer");
        }

        private void doSetSeed(VirtualFrame frame, Integer newSeed, int kind, int normKind) {
            try {
                RRNG.doSetSeed(frame, newSeed, kind, normKind);
            } catch (RNGException ex) {
                if (ex.isError()) {
                    throw RError.getGenericError(getEncapsulatingSourceSection(), ex.getMessage());
                } else {
                    RContext.getInstance().setEvalWarning(ex.getMessage());
                }
            }
        }

    }

    @RBuiltin(name = "RNGkind", kind = INTERNAL)
    public abstract static class RNGkind extends RBuiltinNode {

        @Specialization(order = 0)
        @SuppressWarnings("unused")
        public RIntVector doRNGkind(VirtualFrame frame, RNull x, RNull y) {
            controlVisibility();
            return getCurrent();
        }

        @Specialization(order = 1)
        public RIntVector doRNGkind(VirtualFrame frame, RAbstractIntVector kind, @SuppressWarnings("unused") RNull normKind) {
            controlVisibility();
            RIntVector result = getCurrent();
            try {
                RRNG.doRNGKind(frame, kind.getDataAt(0), RRNG.NO_KIND_CHANGE);
            } catch (RNGException ex) {
                if (ex.isError()) {
                    throw RError.getGenericError(getEncapsulatingSourceSection(), ex.getMessage());
                } else {
                    RContext.getInstance().setEvalWarning(ex.getMessage());
                }
            }
            return result;
        }

        private static RIntVector getCurrent() {
            return RDataFactory.createIntVector(new int[]{RRNG.currentKindAsInt(), RRNG.currentNormKindAsInt()}, RDataFactory.COMPLETE_VECTOR);
        }
    }
}
