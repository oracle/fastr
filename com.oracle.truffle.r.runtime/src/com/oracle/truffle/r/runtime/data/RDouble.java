/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.ValueType;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.closures.*;
import com.oracle.truffle.r.runtime.data.model.*;

@ValueType
public final class RDouble extends RScalarVector implements RAbstractDoubleVector {

    public static final RDouble NA = new RDouble(RRuntime.DOUBLE_NA);
    public static final RDouble DEFAULT = new RDouble(0.0);

    private final double value;

    private RDouble(double value) {
        this.value = value;
    }

    public double getValue() {
        return value;
    }

    public static RDouble valueOf(double value) {
        return new RDouble(value);
    }

    public RAbstractVector castSafe(RType type) {
        switch (type) {
            case Integer:
                return this;
            case Numeric:
            case Double:
                return this;
            case Complex:
                if (Double.isNaN(value)) {
                    return RComplex.NA;
                } else {
                    return RComplex.valueOf(value, 0.0);
                }
            case Character:
                return RClosures.createDoubleToStringVector(this);
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

    public double getDataAt(int index) {
        assert index == 0;
        return getValue();
    }

    public RDoubleVector materialize() {
        return RDataFactory.createDoubleVectorFromScalar(getValue());
    }

    @Override
    public boolean isNA() {
        return RRuntime.isNA(getValue());
    }
}
