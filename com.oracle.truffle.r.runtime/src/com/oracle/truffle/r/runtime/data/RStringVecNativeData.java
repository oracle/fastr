/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.data;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.LoopConditionProfile;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.RandomAccessIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.RandomAccessWriteIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.SeqIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.SeqWriteIterator;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

@ExportLibrary(VectorDataLibrary.class)
public class RStringVecNativeData implements TruffleObject {
    // We need the vector, so that we can easily use the existing NativeDataAccess methods
    // TODO: this field should be replaced with address/length fields and
    // the address/length fields and logic should be removed from NativeMirror
    // including the releasing of the native memory
    private final RStringVector vec;

    /**
     * After nativized, the data array degenerates to a reference holder, which prevents the objects
     * from being GC'ed. If the native code copies data of one string vector to another without
     * using the proper API, we are doomed.
     */
    private final CharSXPWrapper[] data;

    public RStringVecNativeData(RStringVector vec, CharSXPWrapper[] oldData) {
        this.vec = vec;
        this.data = oldData;
    }

    public void setWrappedStringAt(int index, CharSXPWrapper value) {
        data[index] = value;
        NativeDataAccess.setNativeMirrorStringData(vec.getNativeMirror(), index, value);
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public NACheck getNACheck() {
        return NACheck.getEnabled();
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public final RType getType() {
        return RType.Character;
    }

    @ExportMessage
    public int getLength() {
        return NativeDataAccess.getDataLength(vec, null);
    }

    @ExportMessage
    public RStringVecNativeData materialize() {
        return this;
    }

    @ExportMessage
    public RStringVecNativeData materializeCharSXPStorage() {
        return this;
    }

    @ExportMessage
    public boolean isWriteable() {
        return true;
    }

    @ExportMessage
    public RStringArrayVectorData copy(@SuppressWarnings("unused") boolean deep) {
        String[] dataCopy = NativeDataAccess.copyStringNativeData(vec.getNativeMirror());
        return new RStringArrayVectorData(dataCopy, RDataFactory.INCOMPLETE_VECTOR);
    }

    @ExportMessage
    public long asPointer() {
        return NativeDataAccess.getNativeDataAddress(vec);
    }

    @ExportMessage
    public String[] getStringDataCopy() {
        return NativeDataAccess.copyStringNativeData(vec.getNativeMirror());
    }

    // Access to the elements:

    @ExportMessage
    public SeqIterator iterator(
                    @Shared("SeqItLoopProfile") @Cached("createCountingProfile()") LoopConditionProfile loopProfile) {
        SeqIterator it = new SeqIterator(data, getLength());
        it.initLoopConditionProfile(loopProfile);
        return it;
    }

    @ExportMessage
    @SuppressWarnings("static-method")
    public boolean nextImpl(SeqIterator it, boolean loopCondition,
                    @Shared("SeqItLoopProfile") @Cached("createCountingProfile()") LoopConditionProfile loopProfile) {
        return it.next(loopCondition, loopProfile);
    }

    @ExportMessage
    @SuppressWarnings("static-method")
    public void nextWithWrap(SeqIterator it,
                    @Cached("createBinaryProfile()") ConditionProfile wrapProfile) {
        it.nextWithWrap(wrapProfile);
    }

    @ExportMessage
    public RandomAccessIterator randomAccessIterator() {
        return new RandomAccessIterator(null);
    }

    @ExportMessage
    public String getStringAt(int index) {
        return NativeDataAccess.getData(vec, null, index);
    }

    @ExportMessage
    public String getNextString(SeqIterator it) {
        return NativeDataAccess.getData(vec, null, it.getIndex());
    }

    @ExportMessage
    public String getString(@SuppressWarnings("unused") RandomAccessIterator it, int index) {
        return NativeDataAccess.getData(vec, null, index);
    }

    @ExportMessage
    public CharSXPWrapper getCharSXPAt(int index) {
        return data[index];
    }

    @ExportMessage
    public CharSXPWrapper getNextCharSXP(SeqIterator it) {
        return data[it.getIndex()];
    }

    @ExportMessage
    public CharSXPWrapper getCharSXP(@SuppressWarnings("unused") RandomAccessIterator it, int index) {
        return data[index];
    }

    // Write access to the elements:

    @ExportMessage
    public SeqWriteIterator writeIterator() {
        return new SeqWriteIterator(null, data.length);
    }

    @ExportMessage
    public RandomAccessWriteIterator randomAccessWriteIterator() {
        return new RandomAccessWriteIterator(null);
    }

    @ExportMessage
    public void setStringAt(int index, String value) {
        NativeDataAccess.setData(vec, data, index, CharSXPWrapper.create(value));
    }

    @ExportMessage
    public void setNextString(SeqWriteIterator it, String value) {
        NativeDataAccess.setData(vec, data, it.getIndex(), CharSXPWrapper.create(value));
    }

    @ExportMessage
    public void setString(@SuppressWarnings("unused") RandomAccessWriteIterator it, int index, String value) {
        NativeDataAccess.setData(vec, data, index, CharSXPWrapper.create(value));
    }

    @ExportMessage
    public void setCharSXPAt(int index, CharSXPWrapper value) {
        data[index] = value;
    }

    @ExportMessage
    public void setNextCharSXP(SeqWriteIterator it, CharSXPWrapper value) {
        data[it.getIndex()] = value;
    }

    @ExportMessage
    public void setCharSXP(@SuppressWarnings("unused") RandomAccessWriteIterator it, int index, CharSXPWrapper value) {
        data[index] = value;
    }
}
