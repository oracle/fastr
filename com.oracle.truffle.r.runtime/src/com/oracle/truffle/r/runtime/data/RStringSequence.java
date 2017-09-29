/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.data.closures.RClosures;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

public class RStringSequence extends RSequence implements RAbstractStringVector {

    private final int start;
    private final int stride;
    private final String prefix;
    private final String suffix;

    protected RStringSequence(String prefix, String suffix, int start, int stride, int length) {
        super(length);
        this.start = start;
        this.stride = stride;
        this.prefix = prefix != null ? prefix : "";
        this.suffix = suffix != null ? suffix : "";
    }

    private static void resizeData(String[] newData, String[] data, int oldDataLength, String fill) {
        if (newData.length > oldDataLength) {
            if (fill != null) {
                for (int i = data.length; i < oldDataLength; i++) {
                    newData[i] = fill;
                }
            } else {
                for (int i = oldDataLength, j = 0; i < newData.length; ++i, j = Utils.incMod(j, oldDataLength)) {
                    newData[i] = data[j];
                }
            }
        }
    }

    @Override
    public RStringVector copyResized(int size, boolean fillNA) {
        String[] data = new String[size];
        populateVectorData(data);
        resizeData(data, data, getLength(), fillNA ? RRuntime.STRING_NA : null);
        return RDataFactory.createStringVector(data, !(fillNA && size > getLength()));
    }

    @Override
    public RVector<?> copyResizedWithDimensions(int[] newDimensions, boolean fillNA) {
        int size = newDimensions[0] * newDimensions[1];
        String[] data = new String[size];
        populateVectorData(data);
        resizeData(data, data, getLength(), fillNA ? RRuntime.STRING_NA : null);
        return RDataFactory.createStringVector(data, !(fillNA && size > getLength()), newDimensions);
    }

    @Override
    public RDoubleVector createEmptySameType(int newLength, boolean newIsComplete) {
        return RDataFactory.createDoubleVector(new double[newLength], newIsComplete);
    }

    @Override
    @TruffleBoundary
    public String getDataAt(int index) {
        assert index >= 0 && index < getLength();
        return prefix + (start + stride * index) + suffix;
    }

    @TruffleBoundary
    private void populateVectorData(String[] result) {
        int current = start;
        for (int i = 0; i < result.length && i < getLength(); i++) {
            result[i] = prefix + current + suffix;
            current += stride;
        }
    }

    @Override
    public RStringVector materialize() {
        return internalCreateVector();
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
    protected RStringVector internalCreateVector() {
        return copyResized(getLength(), false);
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
}
