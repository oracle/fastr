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
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import java.util.Arrays;

public abstract class RAbstractDoubleVector extends RAbstractAtomicVector {

    public RAbstractDoubleVector(boolean complete) {
        super(complete);
    }

    @Override
    public Object getDataAtAsObject(int index) {
        return getDataAt(index);
    }

    public double getDataAt(@SuppressWarnings("unused") Object store, int index) {
        return getDataAt(index);
    }

    public abstract double getDataAt(int index);

    @SuppressWarnings("unused")
    public void setDataAt(Object store, int index, double value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public RDoubleVector materialize() {
        RDoubleVector result = RDataFactory.createDoubleVector(getDataCopy(), isComplete());
        copyAttributes(result);
        MemoryCopyTracer.reportCopying(this, result);
        return result;
    }

    @TruffleBoundary
    protected void copyAttributes(RDoubleVector materialized) {
        materialized.copyAttributesFrom(this);
    }

    @Override
    protected RDoubleVector internalCopyResized(int size, boolean fillNA, int[] dimensions) {
        double[] localData = getReadonlyData();
        double[] newData = Arrays.copyOf(localData, size);
        newData = resizeData(newData, localData, localData.length, fillNA);
        return RDataFactory.createDoubleVector(newData, isResizedComplete(size, fillNA), dimensions);
    }

    protected static double[] resizeData(double[] newData, double[] oldData, int oldDataLength, boolean fillNA) {
        if (newData.length > oldDataLength) {
            if (fillNA) {
                for (int i = oldDataLength; i < newData.length; i++) {
                    newData[i] = RRuntime.DOUBLE_NA;
                }
            } else {
                assert oldData.length > 0 : "cannot call resize on empty vector if fillNA == false";
                for (int i = oldDataLength, j = 0; i < newData.length; ++i, j = Utils.incMod(j, oldDataLength)) {
                    newData[i] = oldData[j];
                }
            }
        }
        return newData;
    }

    @Override
    protected RDoubleVector internalCopy() {
        return RDataFactory.createDoubleVector(getDataCopy(), isComplete());
    }

    @Override
    public RType getRType() {
        return RType.Double;
    }

    @Override
    public double[] getDataTemp() {
        return (double[]) super.getDataTemp();
    }

    @Override
    public double[] getReadonlyData() {
        return getDataCopy();
    }

    @Override
    public double[] getDataCopy() {
        int length = getLength();
        double[] result = new double[length];
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
    public final RDoubleVector createEmptySameType(int newLength, boolean newIsComplete) {
        return RDataFactory.createDoubleVector(new double[newLength], newIsComplete);
    }

}
