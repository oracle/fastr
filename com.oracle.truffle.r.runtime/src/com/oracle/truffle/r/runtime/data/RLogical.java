/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.data;

import com.oracle.truffle.api.CompilerDirectives.ValueType;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.data.nodes.FastPathVectorAccess.FastPathFromLogicalAccess;
import com.oracle.truffle.r.runtime.data.nodes.SlowPathVectorAccess.SlowPathFromLogicalAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;

@ValueType
public final class RLogical extends RScalarVector implements RAbstractLogicalVector {

    public static final RLogical NA = new RLogical(RRuntime.LOGICAL_NA);
    public static final RLogical TRUE = new RLogical(RRuntime.LOGICAL_TRUE);
    public static final RLogical FALSE = new RLogical(RRuntime.LOGICAL_FALSE);

    private final byte value;

    private RLogical(byte value) {
        this.value = value;
    }

    public static RLogical valueOf(byte value) {
        return new RLogical(value);
    }

    public static RLogical valueOf(boolean value) {
        return value ? TRUE : FALSE;
    }

    public byte getValue() {
        return value;
    }

    @Override
    public RAbstractVector castSafe(RType type, ConditionProfile isNAProfile, boolean keepAttributes) {
        switch (type) {
            case Logical:
                return this;
            case Integer:
                return isNAProfile.profile(isNA()) ? RInteger.createNA() : RInteger.valueOf(value);
            case Double:
                return isNAProfile.profile(isNA()) ? RDouble.createNA() : RDouble.valueOf(value);
            case Complex:
                return isNAProfile.profile(isNA()) ? RComplex.createNA() : RComplex.valueOf(value, 0.0);
            case Character:
                return RString.valueOf(RRuntime.logicalToString(value));
            case List:
                return RScalarList.valueOf(this);
            default:
                return null;
        }
    }

    @Override
    public RType getRType() {
        return RType.Logical;
    }

    @Override
    public String toString() {
        return RRuntime.logicalToString(value);
    }

    @Override
    public byte getDataAt(int index) {
        assert index == 0;
        return getValue();
    }

    @Override
    public RLogicalVector materialize() {
        RLogicalVector result = RDataFactory.createLogicalVectorFromScalar(value);
        MemoryCopyTracer.reportCopying(this, result);
        return result;
    }

    @Override
    public boolean isNA() {
        return RRuntime.isNA(value);
    }

    public static boolean isValid(byte left) {
        return left == RRuntime.LOGICAL_NA || left == RRuntime.LOGICAL_FALSE || left == RRuntime.LOGICAL_TRUE;
    }

    private static final class FastPathAccess extends FastPathFromLogicalAccess {

        FastPathAccess(RAbstractContainer value) {
            super(value);
        }

        @Override
        protected byte getLogical(Object store, int index) {
            assert index == 0;
            return ((RLogical) store).value;
        }
    }

    @Override
    public VectorAccess access() {
        return new FastPathAccess(this);
    }

    private static final SlowPathFromLogicalAccess SLOW_PATH_ACCESS = new SlowPathFromLogicalAccess() {
        @Override
        protected byte getLogical(Object store, int index) {
            assert index == 0;
            return ((RLogical) store).value;
        }
    };

    @Override
    public VectorAccess slowPathAccess() {
        return SLOW_PATH_ACCESS;
    }
}
