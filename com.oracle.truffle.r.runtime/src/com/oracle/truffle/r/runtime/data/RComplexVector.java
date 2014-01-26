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

import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.ops.na.*;

public final class RComplexVector extends RVector implements RAbstractComplexVector {

    private double[] data;

    RComplexVector(double[] data, boolean complete, int[] dims, Object names) {
        super(complete, data.length >> 1, dims, names);
        assert data.length % 2 == 0;
        this.data = data;
    }

    RComplexVector(double[] data, boolean complete, int[] dims) {
        this(data, complete, dims, null);
    }

    @Override
    protected RComplexVector internalCopy() {
        return new RComplexVector(Arrays.copyOf(data, data.length), this.isComplete(), null);
    }

    @Override
    protected int internalGetLength() {
        return data.length >> 1;
    }

    @Override
    public String toString() {
        return Arrays.toString(data);
    }

    @Override
    protected boolean internalVerify() {
        if (isComplete()) {
            for (double d : data) {
                if (d == RRuntime.DOUBLE_NA) {
                    return false;
                }
            }
        }
        return true;
    }

    public RComplex getDataAt(int i) {
        int index = i << 1;
        return new RComplex(data[index], data[index + 1]);
    }

    public RComplexVector copyWithNewDimensions(int[] newDimensions) {
        this.makeShared();
        return RDataFactory.createComplexVector(data, isComplete(), newDimensions);
    }

    public RComplexVector removeLast() {
        assert getLength() > 0;
        return RDataFactory.createComplexVector(Arrays.copyOf(data, getLength() - 1), isComplete());
    }

    @Override
    protected String getDataAtAsString(int index) {
        return getDataAt(index).toString();
    }

    public RComplexVector updateDataAt(int i, RComplex right, NACheck rightNACheck) {
        assert !this.isShared();
        int index = i << 1;
        data[index] = right.getRealPart();
        data[index + 1] = right.getImaginaryPart();
        if (rightNACheck.check(right)) {
            complete = false;
        }
        return this;
    }

    private double[] createResizedData(int size, boolean fillNA) {
        assert !this.isShared();
        int csize = size << 1;
        double[] newData = Arrays.copyOf(data, csize);
        if (csize > this.getLength()) {
            if (fillNA) {
                for (int i = data.length; i < size; ++i) {
                    newData[i] = RRuntime.DOUBLE_NA;
                }
            } else {
                for (int i = data.length, j = 0; i < size; ++i, j = Utils.incMod(j, data.length)) {
                    newData[i] = data[j];
                    newData[i + 1] = data[j + 1];
                }
            }
        }
        return newData;
    }

    @Override
    public RComplexVector copyResized(int size, boolean fillNA) {
        boolean isComplete = isComplete() && ((data.length <= size) || !fillNA);
        return RDataFactory.createComplexVector(createResizedData(size, fillNA), isComplete);
    }

    @Override
    protected void resizeInternal(int size) {
        this.data = createResizedData(size, true);
    }

    public RComplexVector materialize() {
        return this;
    }

    @Override
    public RComplexVector createEmptySameType(int newLength, boolean newIsComplete) {
        return RDataFactory.createComplexVector(new double[newLength << 1], newIsComplete);
    }

    @Override
    public void transferElementSameType(int toIndex, RVector fromVector, int fromIndex) {
        RComplexVector other = (RComplexVector) fromVector;
        int toIndex2 = toIndex << 1;
        int fromIndex2 = fromIndex << 1;
        data[toIndex2] = other.data[fromIndex2];
        data[toIndex2 + 1] = other.data[fromIndex2 + 1];
    }

    public Class<?> getElementClass() {
        return RComplex.class;
    }

    @Override
    public Object getDataAtAsObject(int index) {
        return getDataAt(index);
    }

}
