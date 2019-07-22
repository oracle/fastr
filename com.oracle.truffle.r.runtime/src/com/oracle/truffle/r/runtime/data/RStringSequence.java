/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.closures.RClosures;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.data.nodes.FastPathVectorAccess.FastPathFromStringAccess;
import com.oracle.truffle.r.runtime.data.nodes.SlowPathVectorAccess.SlowPathFromStringAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;

public final class RStringSequence extends RAbstractStringVector implements RSequence {

    private final int start;
    private final int stride;
    private final String prefix;
    private final String suffix;
    private final int length;

    protected RStringSequence(String prefix, String suffix, int start, int stride, int length) {
        super(RDataFactory.COMPLETE_VECTOR);
        this.start = start;
        this.stride = stride;
        this.prefix = prefix != null ? prefix : "";
        this.suffix = suffix != null ? suffix : "";
        this.length = length;
    }

    @Override
    @TruffleBoundary
    public String getDataAt(int index) {
        assert index >= 0 && index < getLength();
        return prefix + (start + stride * index) + suffix;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return start + (getLength() - 1) * stride;
    }

    public int getStride() {
        return stride;
    }

    @Override
    public int getLength() {
        return length;
    }

    public int getIndexFor(String element) {
        if ((prefix.length() > 0 && !element.startsWith(prefix)) || (suffix.length() > 0 && !element.endsWith(suffix))) {
            return -1;
        }
        String c = element.substring(prefix.length(), element.length() - suffix.length());
        try {
            int current = Integer.parseInt(c);
            int first = Math.min(getStart(), getEnd());
            int last = Math.max(getStart(), getEnd());
            if (current < first || current > last) {
                return -1;
            }
            if ((current - getStart()) % getStride() == 0) {
                return (current - getStart()) / getStride();
            }
        } catch (NumberFormatException e) {
        }
        return -1;
    }

    @Override
    @TruffleBoundary
    public Object getStartObject() {
        return prefix + start + suffix;
    }

    @Override
    @TruffleBoundary
    public Object getStrideObject() {
        return Integer.toString(stride);
    }

    @Override
    public RAbstractVector castSafe(RType type, ConditionProfile isNAProfile, boolean keepAttributes) {
        switch (type) {
            case Character:
                return this;
            case List:
                return RClosures.createToListVector(this, keepAttributes);
            default:
                return null;
        }
    }

    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        return "[\"" + getStartObject() + "\" - \"" + prefix + getEnd() + suffix + "\"]";
    }

    private static final class FastPathAccess extends FastPathFromStringAccess {

        FastPathAccess(RAbstractContainer value) {
            super(value);
        }

        @Override
        protected String getStringImpl(AccessIterator accessIter, int index) {
            RStringSequence vector = (RStringSequence) accessIter.getStore();
            assert index >= 0 && index < vector.getLength();
            return vector.prefix + (vector.start + vector.stride * index) + vector.suffix;
        }
    }

    @Override
    public VectorAccess access() {
        return new FastPathAccess(this);
    }

    private static final SlowPathFromStringAccess SLOW_PATH_ACCESS = new SlowPathFromStringAccess() {
        @Override
        protected String getStringImpl(AccessIterator accessIter, int index) {
            RStringSequence vector = (RStringSequence) accessIter.getStore();
            assert index >= 0 && index < vector.getLength();
            return vector.prefix + (vector.start + vector.stride * index) + vector.suffix;
        }
    };

    @Override
    public VectorAccess slowPathAccess() {
        return SLOW_PATH_ACCESS;
    }
}
