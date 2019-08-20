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

import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.data.MemoryCopyTracer;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RRaw;
import com.oracle.truffle.r.runtime.data.RRawVector;
import java.util.Arrays;

public abstract class RAbstractRawVector extends RAbstractNumericVector {

    public RAbstractRawVector(boolean complete) {
        super(complete);
    }

    @Override
    protected boolean isScalarNA() {
        assert getLength() == 1;
        return false;
    }

    @Override
    protected Object getScalarValue() {
        assert getLength() == 1;
        return getRawDataAt(0);
    }

    @Override
    public Object getDataAtAsObject(int index) {
        return RRaw.valueOf(getRawDataAt(index));
    }

    @SuppressWarnings("unused")
    public void setRawDataAt(Object store, int index, byte value) {
        throw new UnsupportedOperationException();
    }

    public abstract byte getRawDataAt(int index);

    @Override
    public RRawVector materialize() {
        RRawVector result = RDataFactory.createRawVector(getDataCopy());
        MemoryCopyTracer.reportCopying(this, result);
        return result;
    }

    private byte[] copyResizedData(int size, boolean fillNA) {
        byte[] localData = getReadonlyData();
        byte[] newData = Arrays.copyOf(localData, size);
        if (!fillNA) {
            assert localData.length > 0 : "cannot call resize on empty vector if fillNA == false";
            // NA is 00 for raw
            for (int i = localData.length, j = 0; i < size; ++i, j = Utils.incMod(j, localData.length)) {
                newData[i] = localData[j];
            }
        }
        return newData;
    }

    @Override
    protected RRawVector internalCopyResized(int size, boolean fillNA, int[] dimensions) {
        return RDataFactory.createRawVector(copyResizedData(size, fillNA), dimensions);
    }

    @Override
    protected RRawVector internalCopy() {
        return RDataFactory.createRawVector(getDataCopy());
    }

    @Override
    public RType getRType() {
        return RType.Raw;
    }

    @Override
    public byte[] getDataTemp() {
        return (byte[]) super.getDataTemp();
    }

    @Override
    public byte[] getReadonlyData() {
        return getDataCopy();
    }

    @Override
    public byte[] getDataCopy() {
        int length = getLength();
        byte[] result = new byte[length];
        for (int i = 0; i < length; i++) {
            result[i] = getRawDataAt(i);
        }
        return result;
    }

    @Override
    public Object getInternalManagedData() {
        return null;
    }

    @Override
    public final RRawVector createEmptySameType(int newLength, boolean newIsComplete) {
        assert newIsComplete;
        return RDataFactory.createRawVector(new byte[newLength]);
    }

}
