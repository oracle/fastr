/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.closures.*;
import com.oracle.truffle.r.runtime.data.model.*;

@ValueType
public final class RInteger extends RScalarVector implements RAbstractIntVector {

    private final int value;

    private RInteger(int value) {
        this.value = value;
    }

    public static RInteger createNA() {
        return new RInteger(RRuntime.INT_NA);
    }

    public static RInteger valueOf(int value) {
        return new RInteger(value);
    }

    public int getDataAt(int index) {
        assert index == 0;
        return value;
    }

    public int getValue() {
        return value;
    }

    @Override
    public RType getRType() {
        return RType.Integer;
    }

    @Override
    public RAbstractVector castSafe(RType type, ConditionProfile isNAProfile) {
        switch (type) {
            case Integer:
                return this;
            case Numeric:
            case Double:
                return isNAProfile.profile(isNA()) ? RDouble.createNA() : RDouble.valueOf(value);
            case Complex:
                return isNAProfile.profile(isNA()) ? RComplex.createNA() : RComplex.valueOf(value, 0.0);
            case Character:
                return RClosures.createIntToStringVector(this);
            case List:
                return RClosures.createAbstractVectorToListVector(this);
            default:
                return null;
        }
    }

    @Override
    public String toString() {
        return RRuntime.intToString(value);
    }

    public RIntVector materialize() {
        return RDataFactory.createIntVectorFromScalar(value);
    }

    @Override
    public boolean isNA() {
        return RRuntime.isNA(value);
    }
}
