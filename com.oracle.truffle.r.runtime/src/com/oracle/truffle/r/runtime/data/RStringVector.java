/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.util.*;

import com.oracle.truffle.api.CompilerDirectives.SlowPath;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.ops.na.*;

public final class RStringVector extends RVector implements RAbstractStringVector {

    private String[] data;
    private static final String[] implicitClassHrDyn = new String[]{"", RRuntime.TYPE_CHARACTER};

    RStringVector(String[] data, boolean complete, int[] dims, Object names) {
        super(complete, data.length, dims, names);
        this.data = data;
    }

    RStringVector(String[] data, boolean complete, int[] dims) {
        this(data, complete, dims, null);
    }

    @Override
    protected RStringVector internalCopy() {
        return new RStringVector(Arrays.copyOf(data, data.length), isComplete(), null);
    }

    @Override
    protected int internalGetLength() {
        return data.length;
    }

    public String[] getDataCopy() {
        String[] copy = new String[data.length];
        System.arraycopy(data, 0, copy, 0, data.length);
        return copy;
    }

    /**
     * Intended for external calls where a copy is not needed. WARNING: think carefully before using
     * this method rather than {@link #getDataCopy()}.
     */
    public String[] getDataWithoutCopying() {
        return data;
    }

    @Override
    @SlowPath
    public String toString() {
        return Arrays.toString(data);
    }

    @Override
    protected boolean internalVerify() {
        // TODO: Implement String + NA
        return true;
    }

    public String getDataAt(int i) {
        return data[i];
    }

    @Override
    protected String getDataAtAsString(int index) {
        return getDataAt(index);
    }

    public RStringVector updateDataAt(int i, String right, NACheck rightNACheck) {
        assert !this.isShared();
        data[i] = right;
        if (rightNACheck.check(right)) {
            complete = false;
        }
        return this;
    }

    private String[] copyResizedData(int size, String fill) {
        String[] newData = Arrays.copyOf(data, size);
        if (size > this.getLength()) {
            if (fill != null) {
                for (int i = data.length; i < size; ++i) {
                    newData[i] = fill;
                }
            } else {
                for (int i = data.length, j = 0; i < size; ++i, j = Utils.incMod(j, data.length)) {
                    newData[i] = data[j];
                }
            }
        }
        return newData;
    }

    private String[] createResizedData(int size, String fill) {
        assert !this.isShared();
        return copyResizedData(size, fill);
    }

    @Override
    public RStringVector copyResized(int size, boolean fillNA) {
        boolean isComplete = isComplete() && ((data.length <= size) || !fillNA);
        return RDataFactory.createStringVector(copyResizedData(size, fillNA ? RRuntime.STRING_NA : null), isComplete);
    }

    @Override
    protected void resizeInternal(int size) {
        this.data = createResizedData(size, RRuntime.STRING_NA);
    }

    public void resizeWithEmpty(int size) {
        this.data = createResizedData(size, RRuntime.NAMES_ATTR_EMPTY_VALUE);
    }

    @Override
    public RStringVector createEmptySameType(int newLength, boolean newIsComplete) {
        return RDataFactory.createStringVector(new String[newLength], newIsComplete);
    }

    @Override
    public void transferElementSameType(int toIndex, RVector fromVector, int fromIndex) {
        RStringVector other = (RStringVector) fromVector;
        data[toIndex] = other.data[fromIndex];
    }

    public Class<?> getElementClass() {
        return RString.class;
    }

    @Override
    public RStringVector copyWithNewDimensions(int[] newDimensions) {
        return RDataFactory.createStringVector(data, isComplete(), newDimensions);
    }

    public RStringVector materialize() {
        return this;
    }

    @Override
    public Object getDataAtAsObject(int index) {
        return getDataAt(index);
    }

    @Override
    protected RStringVector getImplicitClassHr() {
        return getClassHierarchyHelper(new String[]{RRuntime.TYPE_CHARACTER}, implicitClassHrDyn);
    }
}
