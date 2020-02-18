/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.runtime.data.VectorDataLibrary.notWriteableError;

import java.util.Arrays;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.RandomAccessIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.SeqIterator;
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractRawVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

@ExportLibrary(value = VectorDataLibrary.class, receiverType = RAbstractVector.class)
public class DefaultRVectorDataLibrary {
    @ExportMessage
    public static boolean isComplete(RAbstractVector v) {
        return v.isComplete();
    }

    @ExportMessage
    public static int getLength(RAbstractVector v) {
        return v.getLength();
    }

    @ExportMessage
    public static boolean isWriteable(RAbstractVector v) {
        return v.isMaterialized();
    }

    @ExportMessage
    static Object materialize(RAbstractVector receiver) {
        return receiver.materialize();
    }

    @ExportMessage
    static Object copy(RAbstractVector receiver, @SuppressWarnings("unused") boolean deep) {
        return receiver.copy();
    }

    @ExportMessage
    static Object copyResized(RAbstractVector receiver, int newSize, @SuppressWarnings("unused") boolean deep, boolean fillNA) {
        return receiver.copyResized(newSize, fillNA);
    }

    @ExportMessage
    static SeqIterator iterator(RAbstractVector receiver) {
        return new SeqIterator(null, receiver.getLength());
    }

    @ExportMessage
    static RandomAccessIterator randomAccessIterator(RAbstractVector receiver) {
        return new RandomAccessIterator(null, receiver.getLength());
    }

    @ExportMessage
    public static Object getDataAtAsObject(RAbstractVector data, int index) {
        return data.getDataAtAsObject(index);
    }

    @ExportMessage
    public static void setDataAtAsObject(RAbstractVector v, int idx, Object value, NACheck naCheck) {
        v.updateDataAtAsObject(idx, value, naCheck);
    }

    // Logical

    @ExportMessage
    public static byte[] getLogicalDataCopy(RAbstractVector receiver) {
        return asLogical(receiver).getDataCopy();
    }

    @ExportMessage
    public static byte getLogicalAt(RAbstractVector receiver, int index) {
        return asLogical(receiver).getDataAt(index);
    }

    @ExportMessage
    public static byte getNextLogical(RAbstractVector receiver, SeqIterator it) {
        return asLogical(receiver).getDataAt(it.getIndex());
    }

    @ExportMessage
    public static byte getLogical(RAbstractVector receiver, RandomAccessIterator it, int index) {
        return asLogical(receiver).getDataAt(index);
    }

    @ExportMessage
    public static void setLogicalAt(RAbstractVector receiver, int index, byte value, NACheck naCheck) {
        if (!(receiver instanceof RLogicalVector)) {
            throw notWriteableError(receiver, "setLogicalAt");
        }
        RLogicalVector vec = (RLogicalVector) receiver;
        vec.setDataAt(vec.getInternalStore(), index, value);
        if (naCheck.check(value)) {
            vec.setComplete(false);
        }
    }

    @ExportMessage
    public static void setNextLogical(RAbstractVector receiver, SeqIterator it, byte value, NACheck naCheck) {
        setLogicalAt(receiver, it.getIndex(), value, naCheck);
    }

    @ExportMessage
    public static void setLogical(RAbstractVector receiver, RandomAccessIterator it, int index, byte value, NACheck naCheck) {
        setLogicalAt(receiver, index, value, naCheck);
    }

    // Raw

    @ExportMessage
    public static byte[] getRawDataCopy(RAbstractVector receiver) {
        byte[] data = asRaw(receiver).getReadonlyData();
        return Arrays.copyOf(data, data.length);
    }

    @ExportMessage
    public static byte getRawAt(RAbstractVector receiver, int index) {
        return asRaw(receiver).getRawDataAt(index);
    }

    @ExportMessage
    public static byte getNextRaw(RAbstractVector receiver, SeqIterator it) {
        return asRaw(receiver).getRawDataAt(it.getIndex());
    }

    @ExportMessage
    public static byte getRaw(RAbstractVector receiver, @SuppressWarnings("unused") RandomAccessIterator it, int index) {
        return asRaw(receiver).getRawDataAt(index);
    }

    @ExportMessage
    public static void setRawAt(RAbstractVector receiver, int index, byte value, NACheck naCheck) {
        if (!(receiver instanceof RLogicalVector)) {
            throw notWriteableError(receiver, "setRawAt");
        }
        RLogicalVector vec = (RLogicalVector) receiver;
        vec.setDataAt(vec.getInternalStore(), index, value);
        if (naCheck.check(value)) {
            vec.setComplete(false);
        }
    }

    @ExportMessage
    public static void setNextRaw(RAbstractVector receiver, SeqIterator it, byte value, NACheck naCheck) {
        setRawAt(receiver, value, value, naCheck);
    }

    @ExportMessage
    public static void setRaw(RAbstractVector receiver, RandomAccessIterator it, int index, byte value, NACheck naCheck) {
        setRawAt(receiver, value, value, naCheck);
    }

    // String

    @ExportMessage
    public static String[] getStringDataCopy(RAbstractVector receiver) {
        return asString(receiver).getDataCopy();
    }

    @ExportMessage
    public static String getStringAt(RAbstractVector receiver, int index) {
        return asString(receiver).getDataAt(index);
    }

    @ExportMessage
    public static String getNextString(RAbstractVector receiver, SeqIterator it) {
        return asString(receiver).getDataAt(it.getIndex());
    }

    @ExportMessage
    public static String getString(RAbstractVector receiver, RandomAccessIterator it, int index) {
        return asString(receiver).getDataAt(index);
    }

    @ExportMessage
    public static void setStringAt(RAbstractVector receiver, int index, String value, NACheck naCheck) {
        if (!(receiver instanceof RStringVector)) {
            throw notWriteableError(receiver, "setStringAt");
        }
        RStringVector vec = (RStringVector) receiver;
        vec.setDataAt(vec.getInternalStore(), index, value);
        if (naCheck.check(value)) {
            vec.setComplete(false);
        }
    }

    @ExportMessage
    public static void setNextString(RAbstractVector receiver, SeqIterator it, String value, NACheck naCheck) {
        setStringAt(receiver, it.getIndex(), value, naCheck);
    }

    @ExportMessage
    public static void setString(RAbstractVector receiver, RandomAccessIterator it, int index, String value, NACheck naCheck) {
        setStringAt(receiver, index, value, naCheck);
    }

    // Complex

    @ExportMessage
    public static double[] getComplexDataCopy(RAbstractVector receiver) {
        return asComplex(receiver).getDataCopy();
    }

    @ExportMessage
    public static RComplex getComplexAt(RAbstractVector receiver, int index) {
        return asComplex(receiver).getDataAt(index);
    }

    @ExportMessage
    public static RComplex getNextComplex(RAbstractVector receiver, SeqIterator it) {
        return asComplex(receiver).getDataAt(it.getIndex());
    }

    @ExportMessage
    public static RComplex getComplex(RAbstractVector receiver, RandomAccessIterator it, int index) {
        return asComplex(receiver).getDataAt(index);
    }

    @ExportMessage
    public static void setComplexAt(RAbstractVector receiver, int index, RComplex value, NACheck naCheck) {
        if (!(receiver instanceof RComplexVector)) {
            throw notWriteableError(receiver, "setStringAt");
        }
        RComplexVector vec = (RComplexVector) receiver;
        vec.setDataAt(vec.getInternalStore(), index, value);
        if (naCheck.check(value)) {
            vec.setComplete(false);
        }
    }

    @ExportMessage
    public static void setNextComplex(RAbstractVector receiver, SeqIterator it, RComplex value, NACheck naCheck) {
        setComplexAt(receiver, it.getIndex(), value, naCheck);
    }

    @ExportMessage
    public static void setComplex(RAbstractVector receiver, RandomAccessIterator it, int index, RComplex value, NACheck naCheck) {
        setComplexAt(receiver, index, value, naCheck);
    }

    // Lists/expressions

    @ExportMessage
    public static Object[] getListDataCopy(RAbstractVector receiver) {
        return asList(receiver).getDataCopy();
    }

    @ExportMessage
    public static Object getListElementAt(RAbstractVector receiver, int index) {
        return asList(receiver).getDataAt(index);
    }

    @ExportMessage
    public static Object getNextListElement(RAbstractVector receiver, SeqIterator it) {
        return asList(receiver).getDataAt(it.getIndex());
    }

    @ExportMessage
    public static Object getListElement(RAbstractVector receiver, RandomAccessIterator it, int index) {
        return asList(receiver).getDataAt(index);
    }

    @ExportMessage
    public static void setListElementAt(RAbstractVector receiver, int index, Object value) {
        RAbstractListVector list = asList(receiver);
        list.setDataAt(list, index, value);
    }

    @ExportMessage
    public static void setNextListElement(RAbstractVector receiver, SeqIterator it, Object value) {
        setListElementAt(receiver, it.getIndex(), value);
    }

    @ExportMessage
    public static void setListElement(RAbstractVector receiver, RandomAccessIterator it, int index, Object value) {
        setListElementAt(receiver, index, value);
    }

    // Integers: should already have specialized library

    @ExportMessage
    public static int[] getIntDataCopy(RAbstractVector receiver) {
        throw shouldHaveSpecializedLib(receiver);
    }

    @ExportMessage
    public static int getIntAt(RAbstractVector receiver, int index) {
        throw shouldHaveSpecializedLib(receiver);
    }

    @ExportMessage
    public static int getNextInt(RAbstractVector receiver, SeqIterator it) {
        throw shouldHaveSpecializedLib(receiver);
    }

    @ExportMessage
    public static int getInt(RAbstractVector receiver, RandomAccessIterator it, int index) {
        throw shouldHaveSpecializedLib(receiver);
    }

    // Doubles: should already have specialized library

    @ExportMessage
    public static double[] getDoubleDataCopy(RAbstractVector receiver) {
        throw shouldHaveSpecializedLib(receiver);
    }

    @ExportMessage
    public static double getDoubleAt(RAbstractVector receiver, int index) {
        throw shouldHaveSpecializedLib(receiver);
    }

    @ExportMessage
    public static double getNextDouble(RAbstractVector receiver, SeqIterator it) {
        throw shouldHaveSpecializedLib(receiver);
    }

    @ExportMessage
    public static double getDouble(RAbstractVector receiver, RandomAccessIterator it, int index) {
        throw shouldHaveSpecializedLib(receiver);
    }

    // Utility methods

    private static RAbstractLogicalVector asLogical(RAbstractVector obj) {
        return (RAbstractLogicalVector) obj;
    }

    private static RAbstractRawVector asRaw(RAbstractVector obj) {
        return (RAbstractRawVector) obj;
    }

    private static RAbstractStringVector asString(RAbstractVector obj) {
        return (RAbstractStringVector) obj;
    }

    private static RAbstractComplexVector asComplex(RAbstractVector obj) {
        return (RAbstractComplexVector) obj;
    }

    private static RAbstractListVector asList(RAbstractVector obj) {
        return (RAbstractListVector) obj;
    }

    private static RInternalError shouldHaveSpecializedLib(Object receiver) {
        CompilerDirectives.transferToInterpreter();
        String className = receiver != null ? receiver.getClass().getSimpleName() : "null";
        throw RInternalError.shouldNotReachHere(String.format("Object of type '%s' should already have specialized VectorDataLibrary implementation.", className));
    }
}
