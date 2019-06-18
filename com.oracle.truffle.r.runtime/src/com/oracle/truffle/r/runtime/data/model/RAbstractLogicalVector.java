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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.data.MemoryCopyTracer;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import java.util.Arrays;

@ExportLibrary(InteropLibrary.class)
public abstract class RAbstractLogicalVector extends RAbstractAtomicVector {

    public RAbstractLogicalVector(boolean complete) {
        super(complete);
    }

    @ExportMessage
    final boolean isBoolean() {
        if (!isScalar()) {
            return false;
        }
        return !RRuntime.isNA(getDataAt(0));
    }

    @ExportMessage
    final boolean asBoolean(@Cached.Exclusive @Cached("createBinaryProfile()") ConditionProfile isBoolean) throws UnsupportedMessageException {
        if (isBoolean.profile(!isBoolean())) {
            throw UnsupportedMessageException.create();
        }
        return RRuntime.fromLogical(getDataAt(0));
    }

    @Override
    public Object getDataAtAsObject(int index) {
        return getDataAt(index);
    }

    public byte getDataAt(@SuppressWarnings("unused") Object store, int index) {
        return getDataAt(index);
    }

    public abstract byte getDataAt(int index);

    @SuppressWarnings("unused")
    public void setDataAt(Object store, int index, byte value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public RType getRType() {
        return RType.Logical;
    }

    @Override
    public RLogicalVector materialize() {
        RLogicalVector result = RDataFactory.createLogicalVector(getDataCopy(), isComplete());
        copyAttributes(result);
        MemoryCopyTracer.reportCopying(this, result);
        return result;
    }

    @CompilerDirectives.TruffleBoundary
    private void copyAttributes(RLogicalVector materialized) {
        materialized.copyAttributesFrom(this);
    }

    private byte[] copyResizedData(int size, boolean fillNA) {
        byte[] localData = getReadonlyData();
        byte[] newData = Arrays.copyOf(localData, size);
        if (size > localData.length) {
            if (fillNA) {
                for (int i = localData.length; i < size; i++) {
                    newData[i] = RRuntime.LOGICAL_NA;
                }
            } else {
                assert localData.length > 0 : "cannot call resize on empty vector if fillNA == false";
                for (int i = localData.length, j = 0; i < size; ++i, j = Utils.incMod(j, localData.length)) {
                    newData[i] = localData[j];
                }
            }
        }
        return newData;
    }

    @Override
    protected RLogicalVector internalCopyResized(int size, boolean fillNA, int[] dimensions) {
        boolean isComplete = isResizedComplete(size, fillNA);
        return RDataFactory.createLogicalVector(copyResizedData(size, fillNA), isComplete, dimensions);
    }

    @Override
    protected RLogicalVector internalCopy() {
        return RDataFactory.createLogicalVector(getDataCopy(), isComplete());
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
            result[i] = getDataAt(i);
        }
        return result;
    }

    @Override
    public Object getInternalManagedData() {
        return null;
    }

    @Override
    public final RLogicalVector createEmptySameType(int newLength, boolean newIsComplete) {
        return RDataFactory.createLogicalVector(new byte[newLength], newIsComplete);
    }

}
