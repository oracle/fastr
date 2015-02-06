/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.ops.na.*;

public final class RIntVector extends RVector implements RAbstractIntVector {

    private int[] data;

    @CompilationFinal private static final String[] implicitClassHr = RRuntime.CLASS_INTEGER;
    @CompilationFinal private static final String[] implicitClassHrDyn;

    static {
        implicitClassHrDyn = new String[implicitClassHr.length + 1];
        System.arraycopy(implicitClassHr, 0, implicitClassHrDyn, 1, implicitClassHr.length);
    }

    RIntVector(int[] data, boolean complete, int[] dims, RStringVector names) {
        super(complete, data.length, dims, names);
        this.data = data;
    }

    private RIntVector(int[] data, boolean complete, int[] dims) {
        this(data, complete, dims, null);
    }

    public int getDataAt(int index) {
        return data[index];
    }

    @Override
    protected RIntVector internalCopy() {
        return new RIntVector(Arrays.copyOf(data, data.length), isComplete(), null);
    }

    public RIntVector copyResetData(int[] newData) {
        boolean isComplete = true;
        for (int i = 0; i < newData.length; ++i) {
            if (RRuntime.isNA(newData[i])) {
                isComplete = false;
                break;
            }
        }
        RIntVector result = new RIntVector(newData, isComplete, null);
        setAttributes(result);
        return result;
    }

    @Override
    public int getLength() {
        return data.length;
    }

    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        return Arrays.toString(Arrays.stream(data).mapToObj(v -> RRuntime.intToString(v, false)).toArray(String[]::new));
    }

    @Override
    protected boolean internalVerify() {
        if (isComplete()) {
            for (int x : data) {
                if (x == RRuntime.INT_NA) {
                    return false;
                }
            }
        }
        return true;
    }

    public RIntVector removeLast() {
        assert getLength() > 0;
        return RDataFactory.createIntVector(Arrays.copyOf(data, getLength() - 1), isComplete());
    }

    public RIntVector removeFirst() {
        assert getLength() > 0;
        return RDataFactory.createIntVector(Arrays.copyOfRange(data, 1, getLength()), isComplete());
    }

    public int[] getDataCopy() {
        return Arrays.copyOf(data, data.length);
    }

    /**
     * Intended for external calls where a copy is not needed. WARNING: think carefully before using
     * this method rather than {@link #getDataCopy()}.
     */
    public int[] getDataWithoutCopying() {
        return data;
    }

    /**
     * Return vector data (copying if necessary) that's guaranteed not to be shared with any other
     * vector instance (but maybe non-temporary in terms of vector's sharing mode).
     *
     * @return vector data
     */
    public int[] getDataNonShared() {
        return isShared() ? getDataCopy() : getDataWithoutCopying();

    }

    /**
     * Return vector data (copying if necessary) that's guaranteed to be "fresh" (temporary in terms
     * of vector sharing mode).
     *
     * @return vector data
     */
    public int[] getDataTemp() {
        return isTemporary() ? getDataWithoutCopying() : getDataCopy();
    }

    public RIntVector copyWithNewDimensions(int[] newDimensions) {
        return RDataFactory.createIntVector(data, isComplete(), newDimensions);
    }

    @Override
    protected String getDataAtAsString(int index) {
        return RRuntime.intToString(this.getDataAt(index), false);
    }

    public RIntVector updateDataAt(int i, int right, NACheck valueNACheck) {
        assert !this.isShared();
        data[i] = right;
        if (valueNACheck.check(right)) {
            complete = false;
        }
        return this;
    }

    @Override
    public RIntVector updateDataAtAsObject(int i, Object o, NACheck naCheck) {
        return updateDataAt(i, (Integer) o, naCheck);

    }

    public static int[] resizeData(int[] newData, int[] oldData, int oldDataLength, boolean fillNA) {
        if (newData.length > oldDataLength) {
            if (fillNA) {
                for (int i = oldDataLength; i < newData.length; ++i) {
                    newData[i] = RRuntime.INT_NA;
                }
            } else {
                for (int i = oldDataLength, j = 0; i < newData.length; ++i, j = Utils.incMod(j, oldDataLength)) {
                    newData[i] = oldData[j];
                }
            }
        }
        return newData;
    }

    private int[] copyResizedData(int size, boolean fillNA) {
        int[] newData = Arrays.copyOf(data, size);
        return resizeData(newData, this.data, this.getLength(), fillNA);
    }

    private int[] createResizedData(int size, boolean fillNA) {
        assert !this.isShared();
        return copyResizedData(size, fillNA);
    }

    @Override
    public RIntVector copyResized(int size, boolean fillNA) {
        boolean isComplete = isComplete() && ((data.length >= size) || !fillNA);
        return RDataFactory.createIntVector(copyResizedData(size, fillNA), isComplete);
    }

    @Override
    protected void resizeInternal(int size) {
        this.data = createResizedData(size, true);
    }

    public RIntVector materialize() {
        return this;
    }

    @Override
    public RIntVector createEmptySameType(int newLength, boolean newIsComplete) {
        return RDataFactory.createIntVector(new int[newLength], newIsComplete);
    }

    @Override
    public void transferElementSameType(int toIndex, RAbstractVector fromVector, int fromIndex) {
        RAbstractIntVector other = (RAbstractIntVector) fromVector;
        data[toIndex] = other.getDataAt(fromIndex);
    }

    public Class<?> getElementClass() {
        return RInt.class;
    }

    @Override
    public Object getDataAtAsObject(int index) {
        return getDataAt(index);
    }

    @Override
    protected RStringVector getImplicitClassHr() {
        return getClassHierarchyHelper(implicitClassHr, implicitClassHrDyn);
    }
}
