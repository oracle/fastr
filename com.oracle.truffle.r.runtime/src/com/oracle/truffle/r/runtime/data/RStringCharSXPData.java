/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.profiles.LoopConditionProfile;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.RandomAccessIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.RandomAccessWriteIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.SeqIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.SeqWriteIterator;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

@ExportLibrary(value = VectorDataLibrary.class, receiverType = CharSXPWrapper[].class)
public class RStringCharSXPData {
    @ExportMessage
    public static NACheck getNACheck(@SuppressWarnings("unused") CharSXPWrapper[] receiver) {
        return NACheck.getEnabled();
    }

    @ExportMessage
    public static RType getType(@SuppressWarnings("unused") CharSXPWrapper[] receiver) {
        return RType.List;
    }

    @ExportMessage
    public static int getLength(CharSXPWrapper[] receiver) {
        return receiver.length;
    }

    @ExportMessage
    public static CharSXPWrapper[] materialize(CharSXPWrapper[] receiver) {
        return receiver;
    }

    @ExportMessage
    public static boolean isWriteable(@SuppressWarnings("unused") CharSXPWrapper[] receiver) {
        return true;
    }

    @ExportMessage
    public static CharSXPWrapper[] copy(CharSXPWrapper[] receiver, @SuppressWarnings("unused") boolean deep) {
        if (deep) {
            throw RInternalError.unimplemented("list data deep copy");
        }
        CharSXPWrapper[] result = new CharSXPWrapper[receiver.length];
        System.arraycopy(receiver, 0, result, 0, receiver.length);
        return result;
    }

    @ExportMessage
    @SuppressWarnings("unused")
    public static Object copyResized(CharSXPWrapper[] receiver, int newSize, boolean deep, boolean fillNA) {
        throw RInternalError.unimplemented("this method should be removed");
    }

    @ExportMessage
    public static String[] getStringDataCopy(CharSXPWrapper[] receiver) {
        String[] result = new String[receiver.length];
        for (int i = 0; i < receiver.length; i++) {
            result[i] = receiver[i].getContents();
        }
        return result;
    }

    // Data access:

    @ExportMessage
    public static SeqIterator iterator(CharSXPWrapper[] receiver,
                    @Shared("SeqItLoopProfile") @Cached("createCountingProfile()") LoopConditionProfile loopProfile) {
        SeqIterator it = new SeqIterator(null, receiver.length);
        it.initLoopConditionProfile(loopProfile);
        return it;
    }

    @ExportMessage
    public static boolean next(@SuppressWarnings("unused") CharSXPWrapper[] receiver, SeqIterator it, boolean withWrap,
                    @Shared("SeqItLoopProfile") @Cached("createCountingProfile()") LoopConditionProfile loopProfile) {
        return it.next(loopProfile, withWrap);
    }

    @ExportMessage
    public static RandomAccessIterator randomAccessIterator(CharSXPWrapper[] receiver) {
        return new RandomAccessIterator(receiver);
    }

    @ExportMessage
    public static String getStringAt(CharSXPWrapper[] data, int index) {
        return data[index].getContents();
    }

    @ExportMessage
    public static String getNextString(CharSXPWrapper[] data, SeqIterator it) {
        return data[it.getIndex()].getContents();
    }

    @ExportMessage
    public static String getString(CharSXPWrapper[] data, @SuppressWarnings("unused") RandomAccessIterator it, int index) {
        return data[index].getContents();
    }

    // Write access to the elements:

    @ExportMessage
    public static SeqWriteIterator writeIterator(CharSXPWrapper[] receiver) {
        return new SeqWriteIterator(receiver, receiver.length);
    }

    @ExportMessage
    public static RandomAccessWriteIterator randomAccessWriteIterator(@SuppressWarnings("unused") CharSXPWrapper[] receiver) {
        return new RandomAccessWriteIterator(null);
    }

    @ExportMessage
    public static void setStringAt(CharSXPWrapper[] receiver, int index, String value) {
        receiver[index] = CharSXPWrapper.create(value);
    }

    @ExportMessage
    public static void setNextString(CharSXPWrapper[] receiver, SeqWriteIterator it, String value) {
        receiver[it.getIndex()] = CharSXPWrapper.create(value);
    }

    @ExportMessage
    public static void setString(CharSXPWrapper[] receiver, @SuppressWarnings("unused") RandomAccessWriteIterator it, int index, String value) {
        receiver[index] = CharSXPWrapper.create(value);
    }
}
