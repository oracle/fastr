/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.gte;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.instanceOf;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.lte;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.notEmpty;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RRawVector;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractRawVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;

/**
 * Conversion and manipulation of objects of type "raw".
 */
public class RawFunctions {

    @RBuiltin(name = "charToRaw", kind = INTERNAL, parameterNames = "x", behavior = PURE)
    public abstract static class CharToRaw extends RBuiltinNode {

        @Override
        protected void createCasts(CastBuilder casts) {
            casts.arg("x").defaultError(RError.Message.ARG_MUST_BE_CHARACTER_VECTOR_LENGTH_ONE).mustBe(stringValue()).asStringVector().mustBe(notEmpty());
        }

        @Specialization
        protected RRawVector charToRaw(RAbstractStringVector x) {
            if (x.getLength() > 1) {
                RError.warning(this, RError.Message.ARG_SHOULD_BE_CHARACTER_VECTOR_LENGTH_ONE);
            }
            String s = x.getDataAt(0);
            byte[] data = new byte[s.length()];
            for (int i = 0; i < data.length; i++) {
                data[i] = (byte) s.charAt(i);
            }
            return RDataFactory.createRawVector(data);
        }
    }

    @RBuiltin(name = "rawToChar", kind = INTERNAL, parameterNames = {"x", "multiple"}, behavior = PURE)
    public abstract static class RawToChar extends RBuiltinNode {
        @Override
        protected void createCasts(CastBuilder casts) {
            casts.arg("x").mustBe(instanceOf(RAbstractRawVector.class), RError.SHOW_CALLER, RError.Message.ARGUMENT_MUST_BE_RAW_VECTOR, "x");
            casts.arg("multiple").defaultError(RError.Message.INVALID_LOGICAL).asLogicalVector().findFirst().notNA().map(toBoolean());
        }

        @Specialization
        @TruffleBoundary
        protected RStringVector rawToChar(RAbstractRawVector x, boolean multiple) {
            RStringVector result;
            if (multiple) {
                String[] data = new String[x.getLength()];
                for (int i = 0; i < data.length; i++) {
                    data[i] = new String(new byte[]{x.getDataAt(i).getValue()});
                }
                result = RDataFactory.createStringVector(data, RDataFactory.COMPLETE_VECTOR);
            } else {
                int j = 0;
                byte[] data = new byte[x.getLength()];
                for (int i = 0; i < data.length; i++) {
                    byte b = x.getDataAt(i).getValue();
                    if (b != 0) {
                        data[j++] = b;
                    }
                }
                result = RDataFactory.createStringVectorFromScalar(new String(data, 0, j));
            }
            return result;
        }
    }

    @RBuiltin(name = "rawShift", kind = INTERNAL, parameterNames = {"x", "n"}, behavior = PURE)
    public abstract static class RawShift extends RBuiltinNode {
        @Override
        protected void createCasts(CastBuilder casts) {
            casts.arg("x").mustBe(instanceOf(RAbstractRawVector.class), RError.SHOW_CALLER, RError.Message.ARGUMENT_MUST_BE_RAW_VECTOR, "x");
            casts.arg("n").defaultError(RError.Message.MUST_BE_SMALL_INT, "shift").asIntegerVector().findFirst().notNA().mustBe(gte(-8).and(lte(8)));
        }

        @Specialization
        protected RRawVector rawShift(RAbstractRawVector x, int n,
                        @Cached("createBinaryProfile()") ConditionProfile negativeShiftProfile) {
            byte[] data = new byte[x.getLength()];
            if (negativeShiftProfile.profile(n < 0)) {
                for (int i = 0; i < data.length; i++) {
                    data[i] = (byte) ((x.getDataAt(i).getValue() & 0xff) >> (-n));
                }
            } else {
                for (int i = 0; i < data.length; i++) {
                    data[i] = (byte) (x.getDataAt(i).getValue() << n);
                }
            }
            return RDataFactory.createRawVector(data);
        }
    }

    // TODO the rest of the functions

}
