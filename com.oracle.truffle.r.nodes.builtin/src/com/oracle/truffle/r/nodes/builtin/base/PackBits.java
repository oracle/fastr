/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.integerValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.logicalValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.rawValue;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import java.util.BitSet;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractRawVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

@RBuiltin(name = "packBits", kind = INTERNAL, parameterNames = {"x", "type"}, behavior = PURE)
public abstract class PackBits extends RBuiltinNode.Arg2 {

    static {
        Casts casts = new Casts(PackBits.class);
        casts.arg("x").mustNotBeMissing().mustBe(integerValue().or(rawValue()).or(logicalValue())).asVector();
        casts.arg("type").asStringVector().findFirst();
    }

    private static final String RESULT_RAW = "raw";
    private static final String RESULT_INTEGER = "integer";

    private static void checkLength(int length, String resultType) {
        String tp = Utils.intern(resultType);
        if (tp == RESULT_INTEGER) {
            if (length % 32 != 0) {
                throw RError.error(RError.SHOW_CALLER, RError.Message.MUST_BE_MULTIPLE, "x", 32);
            }
        } else if (tp == RESULT_RAW) {
            if (length % 8 != 0) {
                throw RError.error(RError.SHOW_CALLER, RError.Message.MUST_BE_MULTIPLE, "x", 8);
            }
        } else {
            throw RError.error(RError.SHOW_CALLER, RError.Message.ARG_ONE_OF, "\"raw\", \"integer\"");
        }
    }

    private static int[] toIntArray(byte[] data, int[] ints) {
        for (int i = 0; i < ints.length; i++) {
            int j = i * 4;
            byte d3 = j < data.length ? data[j] : 0;
            j++;
            byte d2 = j < data.length ? data[1] : 0;
            j++;
            byte d1 = j < data.length ? data[2] : 0;
            j++;
            byte d0 = j < data.length ? data[3] : 0;
            j++;

            ints[i] = (0xff & d0) << 24 |
                            (0xff & d1) << 16 |
                            (0xff & d2) << 8 |
                            (0xff & d3) << 0;

            if (j >= data.length) {
                break;
            }
        }
        return ints;
    }

    private static RAbstractVector bitSetToVector(BitSet bitSet, String type, int origVecLen) {
        byte[] byteArray = bitSet.toByteArray();

        if (Utils.intern(type) == RESULT_INTEGER) {
            int[] packed = toIntArray(byteArray, new int[origVecLen / 32]);
            return RDataFactory.createIntVector(packed, true);
        } else {
            return RDataFactory.createRawVector(byteArray);
        }
    }

    @Specialization
    @TruffleBoundary
    protected RAbstractVector packBits(RAbstractIntVector x, String type) {
        checkLength(x.getLength(), type);

        BitSet bitSet = new BitSet();
        for (int i = 0; i < x.getLength(); i++) {
            int elem = x.getDataAt(i);
            if (RRuntime.isNA(elem)) {
                throw RError.error(RError.SHOW_CALLER, RError.Message.MUSTNOT_CONTAIN_NAS, "x");
            }
            if ((elem & 1) == 1) {
                bitSet.set(i);
            }
        }
        return bitSetToVector(bitSet, type, x.getLength());
    }

    @Specialization
    @TruffleBoundary
    protected RAbstractVector packBits(RAbstractLogicalVector x, String type) {
        checkLength(x.getLength(), type);

        BitSet bitSet = new BitSet();
        for (int i = 0; i < x.getLength(); i++) {
            byte elem = x.getDataAt(i);
            if (RRuntime.isNA(elem)) {
                throw RError.error(RError.SHOW_CALLER, RError.Message.MUSTNOT_CONTAIN_NAS, "x");
            }
            if (RRuntime.LOGICAL_TRUE == elem) {
                bitSet.set(i);
            }
        }
        return bitSetToVector(bitSet, type, x.getLength());
    }

    @Specialization
    @TruffleBoundary
    protected RAbstractVector packBits(RAbstractRawVector x, String type) {
        checkLength(x.getLength(), type);

        BitSet bitSet = new BitSet();
        for (int i = 0; i < x.getLength(); i++) {
            byte elem = x.getRawDataAt(i);
            if ((elem & 1) == 1) {
                bitSet.set(i);
            }
        }
        return bitSetToVector(bitSet, type, x.getLength());
    }

}
