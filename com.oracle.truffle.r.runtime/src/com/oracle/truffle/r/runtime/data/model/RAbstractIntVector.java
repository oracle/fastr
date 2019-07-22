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
package com.oracle.truffle.r.runtime.data.model;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.data.MemoryCopyTracer;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RIntVector;
import java.util.Arrays;

public abstract class RAbstractIntVector extends RAbstractAtomicVector {

    public RAbstractIntVector(boolean complete) {
        super(complete);
    }

    @Override
    public Object getDataAtAsObject(int index) {
        return getDataAt(index);
    }

    public int getDataAt(@SuppressWarnings("unused") Object store, int index) {
        return getDataAt(index);
    }

    public abstract int getDataAt(int index);

    @SuppressWarnings("unused")
    public void setDataAt(Object store, int index, int value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public RIntVector materialize() {
        RIntVector result = RDataFactory.createIntVector(getDataCopy(), isComplete());
        copyAttributes(result);
        MemoryCopyTracer.reportCopying(this, result);
        return result;
    }

    @TruffleBoundary
    protected void copyAttributes(RIntVector materialized) {
        materialized.copyAttributesFrom(this);
    }

    @Override
    protected RIntVector internalCopy() {
        return RDataFactory.createIntVector(getDataCopy(), isComplete());
    }

    @Override
    protected RIntVector internalCopyResized(int size, boolean fillNA, int[] dimensions) {
        int[] localData = getReadonlyData();
        int[] newData = Arrays.copyOf(localData, size);
        newData = resizeData(newData, localData, localData.length, fillNA);
        return RDataFactory.createIntVector(newData, isResizedComplete(size, fillNA), dimensions);
    }

    protected static int[] resizeData(int[] newData, int[] oldData, int oldDataLength, boolean fillNA) {
        if (newData.length > oldDataLength) {
            if (fillNA) {
                for (int i = oldDataLength; i < newData.length; i++) {
                    newData[i] = RRuntime.INT_NA;
                }
            } else {
                assert oldDataLength > 0 : "cannot call resize on empty vector if fillNA == false";
                for (int i = oldDataLength, j = 0; i < newData.length; ++i, j = Utils.incMod(j, oldDataLength)) {
                    newData[i] = oldData[j];
                }
            }
        }
        return newData;
    }

    @Override
    public RType getRType() {
        return RType.Integer;
    }

    @Override
    public int[] getDataTemp() {
        return (int[]) super.getDataTemp();
    }

    @Override
    public int[] getReadonlyData() {
        return getDataCopy();
    }

    @Override
    public int[] getDataCopy() {
        int length = getLength();
        int[] result = new int[length];
        for (int i = 0; i < length; i++) {
            result[i] = getDataAt(i);
        }
        return result;
    }

    @Override
    public Object getInternalManagedData() {
        return null;
    }

    @Override
    public RIntVector createEmptySameType(int newLength, boolean newIsComplete) {
        return RDataFactory.createIntVector(new int[newLength], newIsComplete);
    }

}
