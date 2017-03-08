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
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.data.model.RAbstractRawVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

@ValueType
public final class RRaw extends RScalarVector implements RAbstractRawVector {

    private final byte value;

    RRaw(byte value) {
        this.value = value;
    }

    @Override
    public boolean isNA() {
        return false;
    }

    @Override
    public RAbstractVector castSafe(RType type, ConditionProfile isNAProfile) {
        switch (type) {
            case Raw:
                return this;
            case Integer:
                return RInteger.valueOf(value);
            case Double:
                return RDouble.valueOf(value);
            case Complex:
                return RComplex.valueOf(value, 0.0);
            case Character:
                return RString.valueOf(RRuntime.rawToString(this));
            default:
                return null;
        }
    }

    @Override
    public RRawVector materialize() {
        RRawVector result = RDataFactory.createRawVector(new byte[]{value});
        MemoryCopyTracer.reportCopying(this, result);
        return result;
    }

    @Override
    public byte getRawDataAt(int index) {
        assert index == 0;
        return value;
    }

    @Override
    public RRaw getDataAt(int index) {
        assert index == 0;
        return this;
    }

    public byte getValue() {
        return value;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof RRaw) {
            return value == ((RRaw) obj).value;
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return value;
    }

    @Override
    public String toString() {
        return Utils.stringFormat("%02x", value);
    }

    public static RRaw valueOf(byte value) {
        return new RRaw(value);
    }
}
