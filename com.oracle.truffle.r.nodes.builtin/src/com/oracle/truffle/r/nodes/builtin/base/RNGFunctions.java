/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.rng.*;
import com.oracle.truffle.r.runtime.rng.RRNG.RNGException;

public class RNGFunctions {
    @RBuiltin(name = "set.seed", kind = INTERNAL, parameterNames = {"seed", "kind", "normal.kind"})
    public abstract static class SetSeed extends RInvisibleBuiltinNode {

        @SuppressWarnings("unused")
        @Specialization
        protected RNull setSeed(VirtualFrame frame, double seed, RNull kind, RNull normKind) {
            controlVisibility();
            doSetSeed(frame, (int) seed, RRNG.NO_KIND_CHANGE, RRNG.NO_KIND_CHANGE);
            return RNull.instance;
        }

        @SuppressWarnings("unused")
        @Specialization
        protected RNull setSeed(VirtualFrame frame, double seed, RAbstractIntVector kind, RNull normKind) {
            controlVisibility();
            doSetSeed(frame, (int) seed, kind.getDataAt(0), RRNG.NO_KIND_CHANGE);
            return RNull.instance;
        }

        @SuppressWarnings("unused")
        @Specialization
        protected RNull setSeed(VirtualFrame frame, RNull seed, RNull kind, RNull normKind) {
            controlVisibility();
            doSetSeed(frame, RRNG.RESET_SEED, RRNG.NO_KIND_CHANGE, RRNG.NO_KIND_CHANGE);
            return RNull.instance;
        }

        @SuppressWarnings("unused")
        @Specialization
        protected RNull setSeed(byte seed, RNull kind, RNull normKind) {
            controlVisibility();
            CompilerDirectives.transferToInterpreter();
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.SEED_NOT_VALID_INT);
        }

        private void doSetSeed(VirtualFrame frame, Integer newSeed, int kind, int normKind) {
            try {
                RRNG.doSetSeed(frame, newSeed, kind, normKind);
            } catch (RNGException ex) {
                if (ex.isError()) {
                    throw RError.error(getEncapsulatingSourceSection(), ex);
                } else {
                    RError.warning(RError.Message.GENERIC, ex.getMessage());
                }
            }
        }

    }

    @RBuiltin(name = "RNGkind", kind = INTERNAL, parameterNames = {"kind", "normkind"})
    public abstract static class RNGkind extends RBuiltinNode {

        @Specialization
        @SuppressWarnings("unused")
        protected RIntVector doRNGkind(VirtualFrame frame, RNull x, RNull y) {
            controlVisibility();
            return getCurrent();
        }

        @Specialization
        protected RIntVector doRNGkind(VirtualFrame frame, RAbstractIntVector kind, @SuppressWarnings("unused") RNull normKind) {
            controlVisibility();
            RIntVector result = getCurrent();
            try {
                RRNG.doRNGKind(frame, kind.getDataAt(0), RRNG.NO_KIND_CHANGE);
            } catch (RNGException ex) {
                if (ex.isError()) {
                    throw RError.error(getEncapsulatingSourceSection(), ex);
                } else {
                    RError.warning(RError.Message.GENERIC, ex.getMessage());
                }
            }
            return result;
        }

        private static RIntVector getCurrent() {
            return RDataFactory.createIntVector(new int[]{RRNG.currentKindAsInt(), RRNG.currentNormKindAsInt()}, RDataFactory.COMPLETE_VECTOR);
        }
    }
}
