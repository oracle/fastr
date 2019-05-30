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
package com.oracle.truffle.r.runtime.interop;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RInteropScalar.RInteropNA;
import com.oracle.truffle.r.runtime.data.RInteropScalar.RInteropByte;
import com.oracle.truffle.r.runtime.data.RInteropScalar.RInteropChar;
import com.oracle.truffle.r.runtime.data.RInteropScalar.RInteropFloat;
import com.oracle.truffle.r.runtime.data.RInteropScalar.RInteropLong;
import com.oracle.truffle.r.runtime.data.RInteropScalar.RInteropShort;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.RSharingAttributeStorage;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

/**
 * Normalizes the internal FastR data representation for the outside world. From the outside the
 * only values appearing in FastR are vectors, so we convert scalars to them. Moreover, there are
 * few FastR types used to explicitly choose a specific type for interop, e.g. {@link RInteropByte}.
 *
 * All the objects passed to the outside are made as shared permanent as FastR looses the control
 * over all their possible references.
 */
@GenerateUncached
public abstract class R2Foreign extends RBaseNode {

    public static R2Foreign getUncached() {
        return R2ForeignNodeGen.getUncached();
    }

    public Object convert(Object obj) {
        return execute(obj, true);
    }

    public Object convertNoBox(Object obj) {
        return execute(obj, false);
    }

    public abstract Object execute(Object obj, boolean boxPrimitives);

    @Specialization(guards = "boxPrimitives")
    public RLogicalVector doByte(byte obj, @SuppressWarnings("unused") boolean boxPrimitives) {
        RLogicalVector result = RDataFactory.createLogicalVectorFromScalar(obj);
        result.makeSharedPermanent();
        return result;
    }

    @Specialization(guards = "boxPrimitives")
    public RDoubleVector doDouble(double vec, @SuppressWarnings("unused") boolean boxPrimitives) {
        RDoubleVector result = RDataFactory.createDoubleVectorFromScalar(vec);
        result.makeSharedPermanent();
        return result;
    }

    @Specialization(guards = "boxPrimitives")
    public RIntVector doInt(int vec, @SuppressWarnings("unused") boolean boxPrimitives) {
        RIntVector result = RDataFactory.createIntVectorFromScalar(vec);
        result.makeSharedPermanent();
        return result;
    }

    @Specialization(guards = "boxPrimitives")
    public RStringVector doString(String vec, @SuppressWarnings("unused") boolean boxPrimitives) {
        RStringVector result = RDataFactory.createStringVectorFromScalar(vec);
        result.makeSharedPermanent();
        return result;
    }

    @Specialization(guards = "!boxPrimitives")
    public Object doByteNoBox(byte obj, @SuppressWarnings("unused") boolean boxPrimitives,
                    @Cached("createBinaryProfile()") ConditionProfile isNaProfile) {
        if (isNaProfile.profile(RRuntime.isNA(obj))) {
            return RInteropNA.LOGICAL;
        }
        return RRuntime.fromLogical(obj);
    }

    @Specialization(guards = "!boxPrimitives")
    public Object doDoubleNoBox(double vec, @SuppressWarnings("unused") boolean boxPrimitives,
                    @Cached("createBinaryProfile()") ConditionProfile isNaProfile) {
        if (isNaProfile.profile(RRuntime.isNA(vec))) {
            return RInteropNA.DOUBLE;
        }
        return vec;
    }

    @Specialization(guards = "!boxPrimitives")
    public Object doIntNoBox(int vec, @SuppressWarnings("unused") boolean boxPrimitives,
                    @Cached("createBinaryProfile()") ConditionProfile isNaProfile) {
        if (isNaProfile.profile(RRuntime.isNA(vec))) {
            return RInteropNA.INT;
        }
        return vec;
    }

    @Specialization(guards = "!boxPrimitives")
    public Object doStringNoBox(String vec, @SuppressWarnings("unused") boolean boxPrimitives,
                    @Cached("createBinaryProfile()") ConditionProfile isNaProfile) {
        if (isNaProfile.profile(RRuntime.isNA(vec))) {
            return RInteropNA.STRING;
        }
        return vec;
    }

    @Specialization
    public byte doInteroptByte(RInteropByte obj, @SuppressWarnings("unused") boolean boxPrimitives) {
        return obj.getValue();
    }

    @Specialization
    public char doInteroptChar(RInteropChar obj, @SuppressWarnings("unused") boolean boxPrimitives) {
        return obj.getValue();
    }

    @Specialization
    public float doInteroptFloat(RInteropFloat obj, @SuppressWarnings("unused") boolean boxPrimitives) {
        return obj.getValue();
    }

    @Specialization
    public long doInteroptLong(RInteropLong obj, @SuppressWarnings("unused") boolean boxPrimitives) {
        return obj.getValue();
    }

    @Specialization
    public short doInteroptShort(RInteropShort obj, @SuppressWarnings("unused") boolean boxPrimitives) {
        return obj.getValue();
    }

    @Specialization
    public static Object doShareable(RSharingAttributeStorage shareable, @SuppressWarnings("unused") boolean boxPrimitives) {
        return shareable.makeSharedPermanent();
    }

    @Fallback
    public static Object doOther(Object obj, @SuppressWarnings("unused") boolean boxPrimitives) {
        RSharingAttributeStorage.verify(obj);
        return obj;
    }

    public static R2Foreign create() {
        return R2ForeignNodeGen.create();
    }
}
