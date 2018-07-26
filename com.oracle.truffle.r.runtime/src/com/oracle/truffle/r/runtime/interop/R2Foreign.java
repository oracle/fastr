/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * All the objects passed to the outside are maked as shared permanent as FastR looses the control
 * over all their possible references.
 */
public abstract class R2Foreign extends RBaseNode {

    protected final boolean boxPrimitives;

    protected R2Foreign(boolean boxPrimitives) {
        this.boxPrimitives = boxPrimitives;
    }

    public abstract Object execute(Object obj);

    @Specialization(guards = "boxPrimitives")
    public RLogicalVector doByte(byte obj) {
        RLogicalVector result = RDataFactory.createLogicalVectorFromScalar(obj);
        result.makeSharedPermanent();
        return result;
    }

    @Specialization(guards = "boxPrimitives")
    public RDoubleVector doDouble(double vec) {
        RDoubleVector result = RDataFactory.createDoubleVectorFromScalar(vec);
        result.makeSharedPermanent();
        return result;
    }

    @Specialization(guards = "boxPrimitives")
    public RIntVector doInt(int vec) {
        RIntVector result = RDataFactory.createIntVectorFromScalar(vec);
        result.makeSharedPermanent();
        return result;
    }

    @Specialization(guards = "boxPrimitives")
    public RStringVector doString(String vec) {
        RStringVector result = RDataFactory.createStringVectorFromScalar(vec);
        result.makeSharedPermanent();
        return result;
    }

    @Specialization(guards = "!boxPrimitives")
    public Object doByteNoBox(byte obj,
                    @Cached("createBinaryProfile()") ConditionProfile isNaProfile) {
        if (isNaProfile.profile(RRuntime.isNA(obj))) {
            return RInteropNA.LOGICAL;
        }
        return RRuntime.fromLogical(obj);
    }

    @Specialization(guards = "!boxPrimitives")
    public Object doDoubleNoBox(double vec,
                    @Cached("createBinaryProfile()") ConditionProfile isNaProfile) {
        if (isNaProfile.profile(RRuntime.isNA(vec))) {
            return RInteropNA.DOUBLE;
        }
        return vec;
    }

    @Specialization(guards = "!boxPrimitives")
    public Object doIntNoBox(int vec,
                    @Cached("createBinaryProfile()") ConditionProfile isNaProfile) {
        if (isNaProfile.profile(RRuntime.isNA(vec))) {
            return RInteropNA.INT;
        }
        return vec;
    }

    @Specialization(guards = "!boxPrimitives")
    public Object doStringNoBox(String vec,
                    @Cached("createBinaryProfile()") ConditionProfile isNaProfile) {
        if (isNaProfile.profile(RRuntime.isNA(vec))) {
            return RInteropNA.STRING;
        }
        return vec;
    }

    @Specialization
    public byte doInteroptByte(RInteropByte obj) {
        return obj.getValue();
    }

    @Specialization
    public char doInteroptChar(RInteropChar obj) {
        return obj.getValue();
    }

    @Specialization
    public float doInteroptFloat(RInteropFloat obj) {
        return obj.getValue();
    }

    @Specialization
    public long doInteroptLong(RInteropLong obj) {
        return obj.getValue();
    }

    @Specialization
    public short doInteroptShort(RInteropShort obj) {
        return obj.getValue();
    }

    @Specialization
    public static Object doShareable(RSharingAttributeStorage shareable) {
        return shareable.makeSharedPermanent();
    }

    @Fallback
    public static Object doOther(Object obj) {
        RSharingAttributeStorage.verify(obj);
        return obj;
    }

    public static R2Foreign create() {
        return R2ForeignNodeGen.create(true);
    }

    /**
     * Primitive values not wrapped in
     * {@link com.oracle.truffle.r.runtime.data.model.RAbstractAtomicVector} are only returned from
     * {@code READ} on a primitive vector.
     */
    public static R2Foreign createNoBox() {
        return R2ForeignNodeGen.create(false);
    }
}
