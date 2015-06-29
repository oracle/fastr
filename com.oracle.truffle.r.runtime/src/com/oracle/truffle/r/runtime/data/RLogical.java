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

import com.oracle.truffle.api.CompilerDirectives.ValueType;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.closures.*;
import com.oracle.truffle.r.runtime.data.model.*;

@ValueType
public final class RLogical extends RScalarVector implements RAbstractLogicalVector {

    public static final RLogical NA = new RLogical(RRuntime.LOGICAL_NA);
    public static final RLogical TRUE = new RLogical(RRuntime.LOGICAL_TRUE);
    public static final RLogical FALSE = new RLogical(RRuntime.LOGICAL_FALSE);
    public static final RLogical DEFAULT = FALSE;

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

    public RAbstractVector castSafe(RType type) {
        switch (type) {
            case Logical:
                return this;
            case Integer:
                if (isNA()) {
                    return RInteger.NA;
                } else {
                    return RInteger.valueOf(value);
                }
            case Numeric:
            case Double:
                if (isNA()) {
                    return RDouble.NA;
                } else {
                    return RDouble.valueOf(value);
                }
            case Complex:
                if (isNA()) {
                    return RComplex.NA;
                } else {
                    return RComplex.valueOf(value, 0.0);
                }
            case Character:
                return RClosures.createLogicalToStringVector(this);
            case List:
                return RClosures.createAbstractVectorToListVector(this);
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

    public byte getDataAt(int index) {
        assert index == 0;
        return getValue();
    }

    public RLogicalVector materialize() {
        return RDataFactory.createLogicalVectorFromScalar(value);
    }

    @Override
    public boolean isNA() {
        return RRuntime.isNA(value);
    }

    public static boolean isValid(byte left) {
        return left == RRuntime.LOGICAL_NA || left == RRuntime.LOGICAL_FALSE || left == RRuntime.LOGICAL_TRUE;
    }
}
