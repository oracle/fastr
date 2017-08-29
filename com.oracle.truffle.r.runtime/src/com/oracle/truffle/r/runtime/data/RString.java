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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.CompilerDirectives.ValueType;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

@ValueType
public final class RString extends RScalarVector implements RAbstractStringVector {

    private final String value;

    private RString(String value) {
        this.value = value;
    }

    public static RString valueOf(String s) {
        return new RString(s);
    }

    public String getValue() {
        return value;
    }

    @Override
    public RAbstractVector castSafe(RType type, ConditionProfile isNAProfile, boolean keepAttributes) {
        switch (type) {
            case Character:
                return this;
            case List:
                return RScalarList.valueOf(this);
            default:
                return null;
        }
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public String getDataAt(int index) {
        assert index == 0;
        return value;
    }

    @Override
    public RStringVector materialize() {
        RStringVector result = RDataFactory.createStringVector(new String[]{getValue()}, isComplete());
        MemoryCopyTracer.reportCopying(this, result);
        return result;
    }

    @Override
    public boolean isNA() {
        return RRuntime.isNA(value);
    }

    /*
     * Not built for the fast path. Use appropiate nodes instead.
     */
    @TruffleBoundary
    public static String assumeSingleString(Object value) {
        if (value instanceof String) {
            return (String) value;
        } else if (value instanceof RAbstractStringVector) {
            return ((RAbstractStringVector) value).getDataAt(0);
        } else {
            throw new AssertionError();
        }
    }
}
