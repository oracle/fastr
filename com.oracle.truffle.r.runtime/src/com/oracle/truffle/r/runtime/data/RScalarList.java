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
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.data.nodes.FastPathVectorAccess.FastPathFromListAccess;
import com.oracle.truffle.r.runtime.data.nodes.SlowPathVectorAccess.SlowPathFromListAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;

@ValueType
public final class RScalarList extends RScalarVector implements RAbstractListVector {

    private final Object value;

    private RScalarList(Object value) {
        this.value = value;
    }

    public static RScalarList valueOf(Object value) {
        return new RScalarList(value);
    }

    @Override
    public Object getDataAt(int index) {
        assert index == 0;
        return value;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public RType getRType() {
        return RType.List;
    }

    @Override
    public RAbstractVector castSafe(RType type, ConditionProfile isNAProfile, boolean keepAttributes) {
        switch (type) {
            case List:
                return this;
            default:
                return null;
        }
    }

    @Override
    public String toString() {
        return value.toString();
    }

    @Override
    public RList materialize() {
        RList result = RDataFactory.createList(new Object[]{value});
        MemoryCopyTracer.reportCopying(this, result);
        return result;
    }

    @Override
    public Object getDataAtAsObject(int index) {
        return getDataAt(index);
    }

    @Override
    public boolean isNA() {
        return false;
    }

    private static final class FastPathAccess extends FastPathFromListAccess {

        FastPathAccess(RAbstractContainer value) {
            super(value);
        }

        @Override
        public RType getType() {
            return RType.List;
        }

        @Override
        protected Object getListElement(Object store, int index) {
            assert index == 0;
            return ((RScalarList) store).value;
        }
    }

    @Override
    public VectorAccess access() {
        return new FastPathAccess(this);
    }

    private static final SlowPathFromListAccess SLOW_PATH_ACCESS = new SlowPathFromListAccess() {
        @Override
        public RType getType() {
            return RType.List;
        }

        @Override
        protected Object getListElement(Object store, int index) {
            assert index == 0;
            return ((RScalarList) store).value;
        }
    };

    @Override
    public VectorAccess slowPathAccess() {
        return SLOW_PATH_ACCESS;
    }
}
