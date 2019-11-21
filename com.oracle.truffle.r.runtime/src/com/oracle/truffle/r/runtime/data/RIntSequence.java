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
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.data.nodes.FastPathVectorAccess.FastPathFromIntAccess;
import com.oracle.truffle.r.runtime.data.nodes.SlowPathVectorAccess.SlowPathFromIntAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;

import java.util.concurrent.atomic.AtomicReference;

public final class RIntSequence extends RIntVector implements RSequence {

    private final int start;
    private final int stride;
    private final int length;
    private AtomicReference<com.oracle.truffle.r.runtime.data.RIntVector> materialized = new AtomicReference<>();

    RIntSequence(int start, int stride, int length) {
        super(RDataFactory.COMPLETE_VECTOR);
        assert length >= 0;
        this.start = start;
        this.stride = stride;
        this.length = length;
    }

    @Override
    public com.oracle.truffle.r.runtime.data.RIntVector cachedMaterialize() {
        if (materialized.get() == null) {
            materialized.compareAndSet(null, materialize());
        }
        return materialized.get();
    }

    @Override
    public int getDataAt(int index) {
        assert index >= 0 && index < getLength();
        return start + stride * index;
    }

    @Override
    public RAbstractVector castSafe(RType type, ConditionProfile isNAProfile, boolean keepAttributes) {
        switch (type) {
            case Integer:
                return this;
            case Double:
                return RDataFactory.createDoubleSequence(getStart(), getStride(), getLength());
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
    public Object getStartObject() {
        return getStart();
    }

    @Override
    public Object getStrideObject() {
        return getStride();
    }

    public int getStart() {
        return start;
    }

    public int getStride() {
        return stride;
    }

    @Override
    public int getLength() {
        return length;
    }

    public int getIndexFor(int element) {
        int first = Math.min(getStart(), getEnd());
        int last = Math.max(getStart(), getEnd());
        if (element < first || element > last) {
            return -1;
        }
        if ((element - getStart()) % getStride() == 0) {
            return (element - getStart()) / getStride();
        }
        return -1;
    }

    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        return "[" + start + " - " + getEnd() + "]";
    }

    // NOTE: it does not hold that getStart() <= getEnd()!
    public int getEnd() {
        return start + (getLength() - 1) * stride;
    }

    private static final class FastPathAccess extends FastPathFromIntAccess {

        FastPathAccess(RAbstractContainer value) {
            super(value);
        }

        @Override
        public int getIntImpl(AccessIterator accessIter, int index) {
            RIntSequence vector = (RIntSequence) accessIter.getStore();
            assert index >= 0 && index < vector.getLength();
            return vector.start + vector.stride * index;
        }
    }

    @Override
    public VectorAccess access() {
        return new FastPathAccess(this);
    }

    private static final SlowPathFromIntAccess SLOW_PATH_ACCESS = new SlowPathFromIntAccess() {
        @Override
        public int getIntImpl(AccessIterator accessIter, int index) {
            RIntSequence vector = (RIntSequence) accessIter.getStore();
            assert index >= 0 && index < vector.getLength();
            return vector.start + vector.stride * index;
        }
    };

    @Override
    public VectorAccess slowPathAccess() {
        return SLOW_PATH_ACCESS;
    }
}
