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

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.ValueType;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.closures.RClosures;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

@ValueType
public final class RDouble extends RScalarVector implements RAbstractDoubleVector {

    private final double value;

    private RDouble(double value) {
        this.value = value;
    }

    public static RDouble createNA() {
        return new RDouble(RRuntime.DOUBLE_NA);
    }

    public static RDouble valueOf(double value) {
        return new RDouble(value);
    }

    public double getValue() {
        return value;
    }

    @Override
    public RAbstractVector castSafe(RType type, ConditionProfile isNAProfile) {
        switch (type) {
            case Integer:
                return this;
            case Double:
                return this;
            case Complex:
                return isNAProfile.profile(Double.isNaN(value)) ? RComplex.createNA() : RComplex.valueOf(value, 0.0);
            case Character:
                return RClosures.createDoubleToStringVector(this);
            case List:
                return RClosures.createAbstractVectorToListVector(this);
            default:
                return null;
        }
    }

    @Override
    public RType getRType() {
        return RType.Double;
    }

    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        return Double.toString(value);
    }

    @Override
    public double getDataAt(int index) {
        assert index == 0;
        return getValue();
    }

    @Override
    public RDoubleVector materialize() {
        RDoubleVector result = RDataFactory.createDoubleVectorFromScalar(getValue());
        MemoryCopyTracer.reportCopying(this, result);
        return result;
    }

    @Override
    public boolean isNA() {
        return RRuntime.isNA(getValue());
    }
}
