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
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RRawVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractRawVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess.RandomIterator;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess.SequentialIterator;

/**
 * Conversion and manipulation of objects of type "raw".
 */
public class RawFunctions {

    @RBuiltin(name = "charToRaw", kind = INTERNAL, parameterNames = "x", behavior = PURE)
    public abstract static class CharToRaw extends RBuiltinNode.Arg1 {

        static {
            Casts casts = new Casts(CharToRaw.class);
            casts.arg("x").defaultError(RError.Message.ARG_MUST_BE_CHARACTER_VECTOR_LENGTH_ONE).mustBe(stringValue()).asStringVector().mustBe(notEmpty());
        }

        @Specialization(guards = "xAccess.supports(x)")
        protected RRawVector charToRaw(RAbstractStringVector x,
                        @Cached("x.access()") VectorAccess xAccess) {
            try (RandomIterator iter = xAccess.randomAccess(x)) {
                if (xAccess.getLength(iter) != 1) {
                    warning(RError.Message.ARG_SHOULD_BE_CHARACTER_VECTOR_LENGTH_ONE);
                }
                String s = xAccess.getString(iter, 0);
                byte[] data = new byte[s.length()];
                for (int i = 0; i < data.length; i++) {
                    data[i] = (byte) s.charAt(i);
                }
                return RDataFactory.createRawVector(data);
            }
        }

        @Specialization(replaces = "charToRaw")
        @TruffleBoundary
        protected RRawVector charToRawGeneric(RAbstractStringVector x) {
            return charToRaw(x, x.slowPathAccess());
        }
    }

    @RBuiltin(name = "rawToChar", kind = INTERNAL, parameterNames = {"x", "multiple"}, behavior = PURE)
    public abstract static class RawToChar extends RBuiltinNode.Arg2 {

        static {
            Casts casts = new Casts(RawToChar.class);
            casts.arg("x").mustBe(instanceOf(RAbstractRawVector.class), RError.Message.ARGUMENT_MUST_BE_RAW_VECTOR, "x");
            casts.arg("multiple").defaultError(RError.Message.INVALID_LOGICAL).asLogicalVector().findFirst().mustNotBeNA().map(toBoolean());
        }

        @TruffleBoundary
        private static String createString(int j, byte[] data) {
            return new String(data, 0, j);
        }

        @TruffleBoundary
        private static String createString(byte value) {
            return new String(new byte[]{value});
        }

        @Specialization(guards = "xAccess.supports(x)")
        protected Object rawToChar(RAbstractRawVector x, boolean multiple,
                        @Cached("x.access()") VectorAccess xAccess) {
            try (SequentialIterator iter = xAccess.access(x)) {
                if (multiple) {
                    String[] data = new String[xAccess.getLength(iter)];
                    while (xAccess.next(iter)) {
                        byte value = xAccess.getRaw(iter);
                        data[iter.getIndex()] = createString(value);
                    }
                    return RDataFactory.createStringVector(data, RDataFactory.COMPLETE_VECTOR);
                } else {
                    int j = 0;
                    byte[] data = new byte[xAccess.getLength(iter)];
                    while (xAccess.next(iter)) {
                        byte b = xAccess.getRaw(iter);
                        if (b != 0) {
                            data[j++] = b;
                        }
                    }
                    return createString(j, data);
                }
            }
        }

        @Specialization(replaces = "rawToChar")
        @TruffleBoundary
        protected Object rawToCharGeneric(RAbstractRawVector x, boolean multiple) {
            return rawToChar(x, multiple, x.slowPathAccess());
        }
    }

    @RBuiltin(name = "rawShift", kind = INTERNAL, parameterNames = {"x", "n"}, behavior = PURE)
    public abstract static class RawShift extends RBuiltinNode.Arg2 {

        static {
            Casts casts = new Casts(RawShift.class);
            casts.arg("x").mustBe(instanceOf(RAbstractRawVector.class), RError.Message.ARGUMENT_MUST_BE_RAW_VECTOR, "x");
            casts.arg("n").defaultError(RError.Message.MUST_BE_SMALL_INT, "shift").asIntegerVector().findFirst().mustNotBeNA().mustBe(gte(-8).and(lte(8)));
        }

        @Specialization(guards = "xAccess.supports(x)")
        protected RRawVector rawShift(RAbstractRawVector x, int n,
                        @Cached("createBinaryProfile()") ConditionProfile negativeShiftProfile,
                        @Cached("x.access()") VectorAccess xAccess) {
            try (SequentialIterator iter = xAccess.access(x)) {
                byte[] data = new byte[xAccess.getLength(iter)];
                if (negativeShiftProfile.profile(n < 0)) {
                    while (xAccess.next(iter)) {
                        data[iter.getIndex()] = (byte) ((xAccess.getRaw(iter) & 0xff) >> (-n));
                    }
                } else {
                    while (xAccess.next(iter)) {
                        data[iter.getIndex()] = (byte) (xAccess.getRaw(iter) << n);
                    }
                }
                return RDataFactory.createRawVector(data);
            }
        }

        @Specialization(replaces = "rawShift")
        @TruffleBoundary
        protected RRawVector rawShiftGeneric(RAbstractRawVector x, int n,
                        @Cached("createBinaryProfile()") ConditionProfile negativeShiftProfile) {
            return rawShift(x, n, negativeShiftProfile, x.slowPathAccess());
        }
    }
}
