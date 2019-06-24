/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.data;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.closures.RClosures;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.data.nodes.FastPathVectorAccess.FastPathFromDoubleAccess;
import com.oracle.truffle.r.runtime.data.nodes.SlowPathVectorAccess.SlowPathFromDoubleAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;

public final class RDoubleSequence extends RAbstractDoubleVector implements RSequence {

    private final double start;
    private final double stride;
    private final int length;

    RDoubleSequence(double start, double stride, int length) {
        super(RDataFactory.COMPLETE_VECTOR);
        assert length >= 0;
        this.start = start;
        this.stride = stride;
        this.length = length;
    }

    @Override
    public double getDataAt(int index) {
        assert index >= 0 && index < getLength();
        return start + stride * index;
    }

    public double getStart() {
        return start;
    }

    public double getStride() {
        return stride;
    }

    @Override
    public int getLength() {
        return length;
    }

    @Override
    public Object getStartObject() {
        return getStart();
    }

    @Override
    public Object getStrideObject() {
        return getStride();
    }

    // NOTE: it does not hold that getStart() <= getEnd()!
    private double getEnd() {
        return start + (getLength() - 1) * stride;
    }

    @Override
    public RAbstractVector castSafe(RType type, ConditionProfile isNAProfile, boolean keepAttributes) {
        // TODO might be possible to implement some of these without closures
        switch (type) {
            case Integer:
                return RClosures.createToIntVector(this, keepAttributes);
            case Double:
                return this;
            case Complex:
                return RClosures.createToComplexVector(this, keepAttributes);
            case Character:
                return RClosures.createToStringVector(this, keepAttributes);
            case List:
                return RClosures.createToListVector(this, keepAttributes);
            default:
                return null;
        }
    }

    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        return "[" + start + " - " + getEnd() + "]";
    }

    private static final class FastPathAccess extends FastPathFromDoubleAccess {

        FastPathAccess(RAbstractContainer value) {
            super(value);
        }

        @Override
        protected double getDoubleImpl(AccessIterator accessIter, int index) {
            RDoubleSequence vector = (RDoubleSequence) accessIter.getStore();
            assert index >= 0 && index < vector.getLength();
            return vector.start + vector.stride * index;
        }
    }

    @Override
    public VectorAccess access() {
        return new FastPathAccess(this);
    }

    private static final SlowPathFromDoubleAccess SLOW_PATH_ACCESS = new SlowPathFromDoubleAccess() {
        @Override
        protected double getDoubleImpl(AccessIterator accessIter, int index) {
            RDoubleSequence vector = (RDoubleSequence) accessIter.getStore();
            assert index >= 0 && index < vector.getLength();
            return vector.start + vector.stride * index;
        }
    };

    @Override
    public VectorAccess slowPathAccess() {
        return SLOW_PATH_ACCESS;
    }
}
