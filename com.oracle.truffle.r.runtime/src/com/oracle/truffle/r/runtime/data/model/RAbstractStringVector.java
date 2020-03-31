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
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.library.ExportMessage.Ignore;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.LoopConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.data.CharSXPWrapper;
import com.oracle.truffle.r.runtime.data.MemoryCopyTracer;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.RandomAccessIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.RandomAccessWriteIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.SeqIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.SeqWriteIterator;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

import java.util.Arrays;

@ExportLibrary(InteropLibrary.class)
@ExportLibrary(VectorDataLibrary.class)
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
    @Ignore
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
    @Ignore
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

    // ------------------------------
    // VectorDataLibrary

    @ExportMessage
    @SuppressWarnings("static")
    public NACheck getNACheck(@Shared("naCheck") @Cached NACheck na) {
        na.enable(!isComplete());
        return na;
    }

    @ExportMessage
    @SuppressWarnings("static")
    public RType getType() {
        return RType.Character;
    }

    @ExportMessage(name = "isComplete", library = VectorDataLibrary.class)
    public boolean datLibIsComplete() {
        return this.isComplete();
    }

    @ExportMessage(name = "getLength", library = VectorDataLibrary.class)
    public int dataLibGetLength() {
        return getLength();
    }

    @ExportMessage
    public boolean isWriteable() {
        return isMaterialized();
    }

    @ExportMessage(name = "materialize", library = VectorDataLibrary.class)
    public Object dataLibMaterialize() {
        return materialize();
    }

    @ExportMessage(name = "copy", library = VectorDataLibrary.class)
    public Object dataLibCopy(@SuppressWarnings("unused") boolean deep) {
        return copy();
    }

    @ExportMessage(name = "copyResized", library = VectorDataLibrary.class)
    public Object dataLibCopyResized(int newSize, @SuppressWarnings("unused") boolean deep, boolean fillNA) {
        return this.copyResized(newSize, fillNA);
    }

    @ExportMessage
    public SeqIterator iterator(@Shared("naCheck") @Cached() NACheck naCheck,
                    @Shared("SeqItLoopProfile") @Cached("createCountingProfile()") LoopConditionProfile loopProfile) {
        SeqIterator it = new SeqIterator(getInternalStore(), getLength());
        naCheck.enable(!isComplete());
        it.initLoopConditionProfile(loopProfile);
        return it;
    }

    @ExportMessage
    @SuppressWarnings("static")
    public boolean next(SeqIterator it, boolean withWrap,
                    @Shared("SeqItLoopProfile") @Cached("createCountingProfile()") LoopConditionProfile loopProfile) {
        return it.next(loopProfile, withWrap);
    }

    @ExportMessage
    public RandomAccessIterator randomAccessIterator(@Shared("naCheck") @Cached() NACheck naCheck) {
        naCheck.enable(!isComplete());
        return new RandomAccessIterator(getInternalStore());
    }

    @ExportMessage
    public SeqWriteIterator writeIterator() {
        return new SeqWriteIterator(getInternalStore(), getLength());
    }

    @ExportMessage
    public RandomAccessWriteIterator randomAccessWriteIterator() {
        return new RandomAccessWriteIterator(getInternalStore());
    }

    @ExportMessage
    public void commitWriteIterator(SeqWriteIterator iterator, boolean neverSeenNA) {
        iterator.commit();
        commitWrites(neverSeenNA);
    }

    @ExportMessage
    public void commitRandomAccessWriteIterator(RandomAccessWriteIterator iterator, boolean neverSeenNA) {
        iterator.commit();
        commitWrites(neverSeenNA);
    }

    private void commitWrites(boolean neverSeenNA) {
        if (!neverSeenNA) {
            setComplete(false);
        }
    }

    @ExportMessage
    public String[] getStringDataCopy() {
        return getDataCopy();
    }

    @ExportMessage
    public String getStringAt(int index,
                    @Shared("storeProfile") @Cached("createClassProfile()") ValueProfile storeProfile,
                    @Shared("naCheck") @Cached NACheck na) {
        String result = getDataAt(storeProfile.profile(getInternalStore()), index);
        na.enable(!isComplete());
        na.check(result);
        return result;
    }

    @ExportMessage
    public String getNextString(SeqIterator it,
                    @Shared("storeProfile") @Cached("createClassProfile()") ValueProfile storeProfile,
                    @Shared("naCheck") @Cached NACheck na) {
        String result = getDataAt(storeProfile.profile(it.getStore()), it.getIndex());
        na.check(result);
        return result;
    }

    @ExportMessage
    public String getString(RandomAccessIterator it, int index,
                    @Shared("storeProfile") @Cached("createClassProfile()") ValueProfile storeProfile,
                    @Shared("naCheck") @Cached NACheck na) {
        String result = getDataAt(storeProfile.profile(it.getStore()), index);
        na.check(result);
        return result;
    }

    @ExportMessage
    public void setStringAt(int index, String value,
                    @Shared("storeProfile") @Cached("createClassProfile()") ValueProfile storeProfile) {
        setDataAt(storeProfile.profile(getInternalStore()), index, value);
        if (RRuntime.isNA(value)) {
            setComplete(false);
        }
    }

    @ExportMessage
    public void setNextString(SeqWriteIterator it, String value,
                    @Shared("storeProfile") @Cached("createClassProfile()") ValueProfile storeProfile) {
        setDataAt(storeProfile.profile(it.getStore()), it.getIndex(), value);
    }

    @ExportMessage
    public void setString(RandomAccessWriteIterator it, int index, String value,
                    @Shared("storeProfile") @Cached("createClassProfile()") ValueProfile storeProfile) {
        setDataAt(storeProfile.profile(it.getStore()), index, value);
    }
}
