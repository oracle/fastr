/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.RBuiltinKind;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RRawVector;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractRawVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;

/**
 * Conversion and manipulation of objects of type "raw".
 */
public class RawFunctions {

    @RBuiltin(name = "charToRaw", kind = RBuiltinKind.INTERNAL, parameterNames = "x")
    public abstract static class CharToRaw extends RBuiltinNode {
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

        @Fallback
        protected Object charToRaw(@SuppressWarnings("unused") Object x) {
            throw RError.error(this, RError.Message.ARG_MUST_BE_CHARACTER_VECTOR_LENGTH_ONE);
        }
    }

    @RBuiltin(name = "rawToChar", kind = RBuiltinKind.INTERNAL, parameterNames = {"x", "multiple"})
    public abstract static class RawToChar extends RBuiltinNode {
        @Specialization
        protected RStringVector rawToChar(RAbstractRawVector x, byte multiple) {
            if (RRuntime.isNA(multiple)) {
                throw RError.error(this, RError.Message.INVALID_LOGICAL, "multiple");
            }
            RStringVector result;
            if (RRuntime.fromLogical(multiple)) {
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

        @SuppressWarnings("unused")
        @Fallback
        protected Object rawToChar(Object x, Object multiple) {
            throw RError.error(this, RError.Message.ARGUMENT_MUST_BE_RAW_VECTOR, "x");
        }
    }

    @RBuiltin(name = "rawShift", kind = RBuiltinKind.INTERNAL, parameterNames = {"x", "n"})
    public abstract static class RawShift extends RBuiltinNode {
        @Override
        protected void createCasts(CastBuilder casts) {
            casts.firstIntegerWithError(1, null, null);
        }

        @Specialization
        protected RRawVector rawShift(RAbstractRawVector x, int n, //
                        @Cached("createBinaryProfile()") ConditionProfile negativeShiftProfile, //
                        @Cached("create()") BranchProfile errorProfile) {
            if (n < -8 || n > 8) {
                errorProfile.enter();
                throw RError.error(this, RError.Message.MUST_BE_SMALL_INT, "shift");
            }
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

        @SuppressWarnings("unused")
        @Fallback
        protected Object charToRaw(Object x, Object n) {
            throw RError.error(this, RError.Message.ARGUMENT_MUST_BE_RAW_VECTOR);
        }
    }

    // TODO the rest of the functions

}
