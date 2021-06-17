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
public class RStringCharSXPData implements ShareableVectorData {
    private final CharSXPWrapper[] data;

    public RStringCharSXPData(CharSXPWrapper[] data) {
        this.data = data;
    }

    public CharSXPWrapper[] getData() {
        return data;
    }

    public CharSXPWrapper getWrappedAt(int index) {
        return data[index];
    }

    public void setWrappedAt(int index, CharSXPWrapper value) {
        data[index] = value;
    }

    @ExportMessage
    public NACheck getNACheck() {
        return NACheck.getEnabled();
    }

    @ExportMessage
    public RType getType() {
        return RType.Character;
    }

    @ExportMessage
    public int getLength() {
        return data.length;
    }

    @ExportMessage
    public RStringCharSXPData materialize() {
        return this;
    }

    @ExportMessage
    public boolean isWriteable() {
        return true;
    }

    @ExportMessage
    public RStringCharSXPData copy(@SuppressWarnings("unused") boolean deep) {
        CharSXPWrapper[] result = new CharSXPWrapper[data.length];
        System.arraycopy(data, 0, result, 0, data.length);
        return new RStringCharSXPData(result);
    }

    @ExportMessage
    public String[] getStringDataCopy() {
        String[] result = new String[data.length];
        for (int i = 0; i < data.length; i++) {
            result[i] = data[i].getContents();
        }
        return result;
    }

    // Data access:

    @ExportMessage
    public SeqIterator iterator(
                    @Shared("SeqItLoopProfile") @Cached("createCountingProfile()") LoopConditionProfile loopProfile) {
        SeqIterator it = new SeqIterator(null, data.length);
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
        return new RandomAccessIterator(data);
    }

    @ExportMessage
    public String getStringAt(int index) {
        return data[index].getContents();
    }

    @ExportMessage
    public String getNextString(SeqIterator it) {
        return data[it.getIndex()].getContents();
    }

    @ExportMessage
    public String getString(@SuppressWarnings("unused") RandomAccessIterator it, int index) {
        return data[index].getContents();
    }

    // Write access to the elements:

    @ExportMessage
    public SeqWriteIterator writeIterator() {
        return new SeqWriteIterator(data, data.length);
    }

    @ExportMessage
    public RandomAccessWriteIterator randomAccessWriteIterator() {
        return new RandomAccessWriteIterator(null);
    }

    @ExportMessage
    public void setStringAt(int index, String value) {
        data[index] = CharSXPWrapper.create(value);
    }

    @ExportMessage
    public void setNextString(SeqWriteIterator it, String value) {
        data[it.getIndex()] = CharSXPWrapper.create(value);
    }

    @ExportMessage
    public void setString(@SuppressWarnings("unused") RandomAccessWriteIterator it, int index, String value) {
        data[index] = CharSXPWrapper.create(value);
    }
}
