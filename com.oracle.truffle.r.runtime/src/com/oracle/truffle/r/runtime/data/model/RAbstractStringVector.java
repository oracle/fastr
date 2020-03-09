/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.r.runtime.data.CharSXPWrapper;
import com.oracle.truffle.r.runtime.data.MemoryCopyTracer;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RStringVector;
import java.util.Arrays;

@ExportLibrary(InteropLibrary.class)
public abstract class RAbstractStringVector extends RAbstractAtomicVector {

    public RAbstractStringVector(boolean complete) {
        super(complete);
    }

    @ExportMessage
    final boolean isNull(@Cached.Exclusive @Cached("createBinaryProfile()") ConditionProfile isScalar) {
        if (!isScalar.profile(isScalar())) {
            return false;
        }
        return RRuntime.isNA(getDataAt(0));
    }

    @ExportMessage
    final boolean isString() {
        if (!isScalar()) {
            return false;
        }
        return !RRuntime.isNA(getDataAt(0));
    }

    @ExportMessage
    final String asString(@Cached.Exclusive @Cached("createBinaryProfile()") ConditionProfile isString) throws UnsupportedMessageException {
        if (!isString.profile(isString())) {
            throw UnsupportedMessageException.create();
        }
        return getDataAt(0);
    }

    @Override
    public Object getDataAtAsObject(int index) {
        return getDataAt(index);
    }

    public String getDataAt(@SuppressWarnings("unused") Object store, int index) {
        return getDataAt(index);
    }

    @SuppressWarnings("unused")
    public void setDataAt(Object store, int index, String value) {
        throw new UnsupportedOperationException();
    }

    public abstract String getDataAt(int index);

    @Override
    public RType getRType() {
        return RType.Character;
    }

    @Override
    public RStringVector materialize() {
        RStringVector result = RDataFactory.createStringVector(getDataCopy(), isComplete());
        copyAttributes(result);
        MemoryCopyTracer.reportCopying(this, result);
        return result;
    }

    @CompilerDirectives.TruffleBoundary
    private void copyAttributes(RStringVector materialized) {
        materialized.copyAttributesFrom(this);
    }

    @Override
    protected RStringVector internalCopyResized(int size, boolean fillNA, int[] dimensions) {
        boolean isComplete = isResizedComplete(size, fillNA);
        return createStringVector(copyResizedData(size, fillNA ? RRuntime.STRING_NA : null), isComplete, dimensions);
    }

    protected RStringVector createStringVector(Object[] dataArg, boolean isComplete, int[] dims) {
        return RDataFactory.createStringVector((String[]) dataArg, isComplete, dims);
    }

    protected Object[] copyResizedData(int size, String fill) {
        Object[] localData = getReadonlyData();
        Object[] newData = Arrays.copyOf(localData, size);
        if (size > localData.length) {
            if (fill != null) {
                Object fillObj = newData instanceof String[] ? fill : CharSXPWrapper.create(fill);
                for (int i = localData.length; i < size; i++) {
                    newData[i] = fillObj;
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
    protected RStringVector internalCopy() {
        return RDataFactory.createStringVector(getDataCopy(), isComplete());
    }

    @Override
    public Object[] getReadonlyData() {
        return getDataCopy();
    }

    @Override
    public String[] getDataCopy() {
        int length = getLength();
        String[] result = new String[length];
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
    public final RStringVector createEmptySameType(int newLength, boolean newIsComplete) {
        return RDataFactory.createStringVector(new String[newLength], newIsComplete);
    }

}
