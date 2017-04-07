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

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.closures.RClosures;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

public final class RIntSequence extends RSequence implements RAbstractIntVector {

    private final int start;
    private final int stride;

    RIntSequence(int start, int stride, int length) {
        super(length);
        assert length >= 0;
        this.start = start;
        this.stride = stride;
    }

    @Override
    public int getDataAt(int index) {
        assert index >= 0 && index < getLength();
        return start + stride * index;
    }

    @Override
    public RAbstractVector castSafe(RType type, ConditionProfile isNAProfile) {
        switch (type) {
            case Integer:
                return this;
            case Double:
                return RDataFactory.createDoubleSequence(getStart(), getStride(), getLength());
            case Complex:
                return RClosures.createToComplexVector(this);
            case Character:
                return RClosures.createToStringVector(this);
            case List:
                return RClosures.createToListVector(this);
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

    private RIntVector populateVectorData(int[] result) {
        int current = start;
        for (int i = 0; i < result.length && i < getLength(); i++) {
            result[i] = current;
            current += stride;
        }
        return RDataFactory.createIntVector(result, RDataFactory.COMPLETE_VECTOR);
    }

    @Override
    protected RIntVector internalCreateVector() {
        return populateVectorData(new int[getLength()]);
    }

    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        return "[" + start + " - " + getEnd() + "]";
    }

    public int getEnd() {
        return start + (getLength() - 1) * stride;
    }

    @Override
    public RIntVector materialize() {
        return this.internalCreateVector();
    }

    @Override
    public RStringVector getImplicitClass() {
        return RIntVector.implicitClassHeader;
    }

    @Override
    public RIntVector copyResized(int size, boolean fillNA) {
        int[] data = new int[size];
        populateVectorData(data);
        RIntVector.resizeData(data, data, getLength(), fillNA);
        return RDataFactory.createIntVector(data, !(fillNA && size > getLength()));
    }

    @Override
    public RVector<?> copyResizedWithDimensions(int[] newDimensions, boolean fillNA) {
        int size = newDimensions[0] * newDimensions[1];
        int[] data = new int[size];
        populateVectorData(data);
        RIntVector.resizeData(data, data, getLength(), fillNA);
        return RDataFactory.createIntVector(data, !(fillNA && size > getLength()), newDimensions);
    }

    @Override
    public RIntVector createEmptySameType(int newLength, boolean newIsComplete) {
        return RDataFactory.createIntVector(new int[newLength], newIsComplete);
    }
}
