/*
 * Copyright (c) 2019, 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.ffi;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.data.RBaseObject;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDouble;
import com.oracle.truffle.r.runtime.data.RInteger;
import com.oracle.truffle.r.runtime.data.RLogical;
import com.oracle.truffle.r.runtime.data.RRaw;
import com.oracle.truffle.r.runtime.data.RScalarList;
import com.oracle.truffle.r.runtime.data.RScalarVector;
import com.oracle.truffle.r.runtime.data.RSequence;
import com.oracle.truffle.r.runtime.data.RString;

/**
 * Converts values that can live on the FastR side to values that can be sent to the native code,
 * i.e., anything that is not {@link RBaseObject} and doesn't have a native mirror needs to be
 * wrapped to such object. Moreover, we need to convert compact read-only vectors to materialized
 * ones, since user may expect to be able to take their data-pointer and write into them.
 *
 * See documentation/dev/ffi.md for more details.
 */
@GenerateUncached
public abstract class FFIMaterializeNode extends Node {

    public abstract Object execute(Object value);

    public static Object executeUncached(Object value) {
        return FFIWrapNodeGen.getUncached().execute(value);
    }

    // Scalar values:

    @Specialization
    protected static Object wrap(int value) {
        return RDataFactory.createIntVectorFromScalar(value);
    }

    @Specialization
    protected static Object wrap(double value) {
        return RDataFactory.createDoubleVectorFromScalar(value);
    }

    @Specialization
    protected static Object wrap(byte value) {
        return RDataFactory.createLogicalVectorFromScalar(value);
    }

    @Specialization
    protected static Object wrap(String value) {
        return RDataFactory.createStringVectorFromScalar(value);
    }

    @Specialization
    protected static Object wrap(RString value) {
        return RDataFactory.createStringVectorFromScalar(value.getValue());
    }

    @Specialization
    protected static Object wrap(RRaw value) {
        return RDataFactory.createRawVectorFromScalar(value);
    }

    @Specialization
    protected static Object wrap(RComplex value) {
        return RDataFactory.createComplexVectorFromScalar(value);
    }

    // Scalar vectors: so far protected via weak hash map, should not occur here often
    // Maybe we should get rid of the scalar vectors altogether

    @Specialization
    protected static Object wrap(RInteger value) {
        return value.materialize();
    }

    @Specialization
    protected static Object wrap(RDouble value) {
        return value.materialize();
    }

    @Specialization
    protected static Object wrap(RLogical value) {
        return value.materialize();
    }

    @Specialization
    protected static Object wrap(RScalarList value) {
        return value.materialize();
    }

    // Sequences: life-cycle of the materialized vector is cached and tied with the sequence via a
    // field inside the sequence

    @Specialization
    protected static Object wrap(RSequence seq) {
        return seq.cachedMaterialize();
    }

    // No need to wrap other RObjects than sequences or scalars

    @Specialization(guards = "!isRScalarVectorOrSequence(value)")
    protected static Object wrap(RBaseObject value) {
        return value;
    }

    @Fallback
    protected static Object wrap(Object value) {
        return value;
    }

    protected static boolean isRScalarVectorOrSequence(RBaseObject value) {
        return value instanceof RScalarVector || value instanceof RSequence;
    }

    public static FFIMaterializeNode create() {
        return FFIMaterializeNodeGen.create();
    }

    public static FFIMaterializeNode[] create(int count) {
        FFIMaterializeNode[] result = new FFIMaterializeNode[count];
        for (int i = 0; i < count; i++) {
            result[i] = FFIMaterializeNode.create();
        }
        return result;
    }
}
