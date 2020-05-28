package com.oracle.truffle.r.runtime.ffi;
/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RRaw;
import com.oracle.truffle.r.runtime.ffi.interop.NativeDoubleArray;

/**
 * This node represents "standard" way of materializing values - it has all the specializations
 * inherited from FFIMaterializeNode plus scalar-value wrapping.
 */
@GenerateUncached
public abstract class FFIMaterializeNodeImpl extends FFIMaterializeNode {
    @Override
    protected abstract Object execute(Object value, boolean protect);

    // Scalar values:

    @Specialization
    protected static Object wrap(int value, @SuppressWarnings("unused") boolean protect) {
        return RDataFactory.createIntVectorFromScalar(value);
    }

    @Specialization
    protected static Object wrap(double value, @SuppressWarnings("unused") boolean protect) {
        return RDataFactory.createDoubleVectorFromScalar(value);
    }

    @Specialization
    protected static Object wrap(double[] value, @SuppressWarnings("unused") boolean protect) {
        return new NativeDoubleArray(value);
    }

    @Specialization
    protected static Object wrap(byte value, @SuppressWarnings("unused") boolean protect) {
        return RDataFactory.createLogicalVectorFromScalar(value);
    }

    @Specialization
    protected static Object wrap(String value, @SuppressWarnings("unused") boolean protect) {
        return RDataFactory.createStringVectorFromScalar(value);
    }

    @Specialization
    protected static Object wrap(RRaw value, @SuppressWarnings("unused") boolean protect) {
        return RDataFactory.createRawVectorFromScalar(value);
    }

    @Specialization
    protected static Object wrap(RComplex value, @SuppressWarnings("unused") boolean protect) {
        return RDataFactory.createComplexVectorFromScalar(value);
    }
}
