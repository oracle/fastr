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

import java.util.Arrays;

import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.data.closures.RClosures;
import com.oracle.truffle.r.runtime.data.model.RAbstractRawVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

public final class RRawVector extends RVector<byte[]> implements RAbstractRawVector {

    public static final RStringVector implicitClassHeader = RDataFactory.createStringVectorFromScalar(RType.Raw.getClazz());

    private final byte[] data;

    RRawVector(byte[] data, int[] dims, RStringVector names) {
        super(true, data.length, dims, names);
        this.data = data;
        assert verify();
    }

    private RRawVector(byte[] data, int[] dims) {
        this(data, dims, null);
    }

    @Override
    public RAbstractVector castSafe(RType type, ConditionProfile isNAProfile) {
        switch (type) {
            case Raw:
                return this;
            case Integer:
                return RClosures.createToIntVector(this);
            case Double:
                return RClosures.createToDoubleVector(this);
            case Complex:
                return RClosures.createToComplexVector(this);
            case Character:
                return RClosures.createToStringVector(this);
            default:
                return null;
        }
    }

    @Override
    public byte getRawDataAt(int index) {
        return data[index];
    }

    @Override
    public byte getRawDataAt(Object store, int index) {
        assert data == store;
        return ((byte[]) store)[index];
    }

    @Override
    public byte[] getInternalStore() {
        return data;
    }

    @Override
    public void setRawDataAt(Object store, int index, byte value) {
        assert data == store;
        ((byte[]) store)[index] = value;
    }

    @Override
    protected RRawVector internalCopy() {
        return new RRawVector(Arrays.copyOf(data, data.length), null);
    }

    @Override
    public int getLength() {
        return data.length;
    }

    @Override
    public String toString() {
        return toString(i -> RRuntime.rawToString(getDataAt(i)));
    }

    @Override
    public boolean verify() {
        return true;
    }

    @Override
    public RRaw getDataAt(int i) {
        return RDataFactory.createRaw(data[i]);
    }

    @Override
    public byte[] getDataCopy() {
        return Arrays.copyOf(data, data.length);
    }

    /**
     * Intended for external calls where a copy is not needed. WARNING: think carefully before using
     * this method rather than {@link #getDataCopy()}.
     */
    @Override
    public byte[] getDataWithoutCopying() {
        return data;
    }

    @Override
    public RRawVector copyWithNewDimensions(int[] newDimensions) {
        return RDataFactory.createRawVector(data, newDimensions);
    }

    @Override
    public RRawVector materialize() {
        return this;
    }

    public RRawVector updateDataAt(int i, RRaw right) {
        assert !this.isShared();
        data[i] = right.getValue();
        return this;
    }

    @Override
    public RRawVector updateDataAtAsObject(int i, Object o, NACheck naCheck) {
        return updateDataAt(i, (RRaw) o);
    }

    private byte[] copyResizedData(int size, boolean fillNA) {
        byte[] newData = Arrays.copyOf(data, size);
        if (!fillNA) {
            // NA is 00 for raw
            for (int i = data.length, j = 0; i < size; ++i, j = Utils.incMod(j, data.length)) {
                newData[i] = data[j];
            }
        }
        return newData;
    }

    @Override
    protected RRawVector internalCopyResized(int size, boolean fillNA, int[] dimensions) {
        return RDataFactory.createRawVector(copyResizedData(size, fillNA), dimensions);
    }

    @Override
    public RRawVector createEmptySameType(int newLength, boolean newIsComplete) {
        assert newIsComplete == true;
        return RDataFactory.createRawVector(new byte[newLength]);
    }

    @Override
    public void transferElementSameType(int toIndex, RAbstractVector fromVector, int fromIndex) {
        RAbstractRawVector other = (RAbstractRawVector) fromVector;
        data[toIndex] = other.getRawDataAt(fromIndex);
    }

    @Override
    public Object getDataAtAsObject(int index) {
        return getDataAt(index);
    }

    @Override
    public RStringVector getImplicitClass() {
        return getClassHierarchyHelper(implicitClassHeader);
    }
}
