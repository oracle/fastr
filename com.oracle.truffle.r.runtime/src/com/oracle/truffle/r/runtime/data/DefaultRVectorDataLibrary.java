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

import java.util.Arrays;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.profiles.LoopConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.RandomAccessIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.RandomAccessWriteIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.SeqIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.SeqWriteIterator;
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractListBaseVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractRawVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

@ExportLibrary(value = VectorDataLibrary.class, receiverType = RAbstractContainer.class)
public class DefaultRVectorDataLibrary {

    @ExportMessage
    public static NACheck getNACheck(@SuppressWarnings("unused") RAbstractContainer v,
                    @Shared("naCheck") @Cached() NACheck na) {
        return na;
    }

    @ExportMessage
    public static RType getType(RAbstractContainer v,
                    @Cached("createIdentityProfile()") ValueProfile valueProfile) {
        return valueProfile.profile(v.getRType());
    }

    @ExportMessage
    public static boolean isComplete(RAbstractContainer v) {
        return v.isComplete();
    }

    @ExportMessage
    public static int getLength(RAbstractContainer v) {
        return v.getLength();
    }

    @ExportMessage
    public static boolean isWriteable(RAbstractContainer v) {
        if (v instanceof RAbstractVector) {
            return ((RAbstractVector) v).isMaterialized();
        }
        return true;
    }

    @ExportMessage
    public static Object materialize(RAbstractContainer receiver) {
        return receiver.materialize();
    }

    @ExportMessage
    public static Object copy(RAbstractContainer receiver, @SuppressWarnings("unused") boolean deep) {
        return receiver.copy();
    }

    @ExportMessage
    public static Object copyResized(RAbstractContainer receiver, int newSize, @SuppressWarnings("unused") boolean deep, boolean fillNA) {
        if (receiver instanceof RAbstractVector) {
            return ((RAbstractVector) receiver).copyResized(newSize, fillNA);
        }
        throw RInternalError.unimplemented();
    }

    @ExportMessage
    public static SeqIterator iterator(RAbstractContainer receiver,
                    @Shared("naCheck") @Cached() NACheck naCheck,
                    @Shared("SeqItLoopProfile") @Cached("createCountingProfile()") LoopConditionProfile loopProfile) {
        SeqIterator it = new SeqIterator(null, receiver.getLength());
        naCheck.enable(!isComplete(receiver));
        it.initLoopConditionProfile(loopProfile);
        return it;
    }

    @ExportMessage
    public static boolean next(@SuppressWarnings("unused") RAbstractContainer receiver, SeqIterator it, boolean withWrap,
                    @Shared("SeqItLoopProfile") @Cached("createCountingProfile()") LoopConditionProfile loopProfile) {
        return it.next(loopProfile, withWrap);
    }

    @ExportMessage
    public static RandomAccessIterator randomAccessIterator(@SuppressWarnings("unused") RAbstractContainer receiver,
                    @Shared("naCheck") @Cached() NACheck naCheck) {
        naCheck.enable(!isComplete(receiver));
        return new RandomAccessIterator(null);
    }

    @ExportMessage
    public static SeqWriteIterator writeIterator(RAbstractContainer receiver) {
        return new SeqWriteIterator(null, receiver.getLength());
    }

    @ExportMessage
    public static RandomAccessWriteIterator randomAccessWriteIterator(@SuppressWarnings("unused") RAbstractContainer receiver) {
        return new RandomAccessWriteIterator(null);
    }

    @ExportMessage
    public static void commitWriteIterator(RAbstractContainer receiver, SeqWriteIterator iterator, boolean neverSeenNA) {
        iterator.commit();
        commitWrites(receiver, neverSeenNA);
    }

    @ExportMessage
    public static void commitRandomAccessWriteIterator(RAbstractContainer receiver, RandomAccessWriteIterator iterator, boolean neverSeenNA) {
        iterator.commit();
        commitWrites(receiver, neverSeenNA);
    }

    private static void commitWrites(RAbstractContainer receiver, boolean neverSeenNA) {
        if (!neverSeenNA) {
            ((RAbstractVector) receiver).setComplete(false);
        }
    }

    // Logical

    @ExportMessage
    public static byte[] getLogicalDataCopy(RAbstractContainer receiver) {
        return asLogical(receiver).getDataCopy();
    }

    @ExportMessage
    public static byte getLogicalAt(RAbstractContainer receiver, int index) {
        return asLogical(receiver).getDataAt(index);
    }

    @ExportMessage
    public static void setLogicalAt(RAbstractContainer receiver, int index, byte value) {
        setLogicalImpl(receiver, index, value);
        if (RRuntime.isNA(value)) {
            ((RAbstractVector) receiver).setComplete(false);
        }
    }

    @ExportMessage
    public static void setNextLogical(RAbstractContainer receiver, SeqWriteIterator it, byte value) {
        setLogicalImpl(receiver, it.getIndex(), value);
    }

    @ExportMessage
    public static void setLogical(RAbstractContainer receiver, @SuppressWarnings("unused") RandomAccessWriteIterator it, int index, byte value) {
        setLogicalImpl(receiver, index, value);
    }

    private static void setLogicalImpl(RAbstractContainer receiver, int index, byte value) {
        RLogicalVector vec = (RLogicalVector) receiver;
        vec.setDataAt(vec.getInternalStore(), index, value);
    }

    // Raw

    @ExportMessage
    public static byte[] getRawDataCopy(RAbstractContainer receiver) {
        byte[] data = asRaw(receiver).getReadonlyData();
        return Arrays.copyOf(data, data.length);
    }

    @ExportMessage
    public static void setRawAt(RAbstractContainer receiver, int index, byte value) {
        setRawImpl(receiver, index, value);
        if (RRuntime.isNA(value)) {
            ((RAbstractVector) receiver).setComplete(false);
        }
    }

    @ExportMessage
    public static void setNextRaw(RAbstractContainer receiver, SeqWriteIterator it, byte value) {
        setRawImpl(receiver, it.getIndex(), value);
    }

    @ExportMessage
    public static void setRaw(RAbstractContainer receiver, @SuppressWarnings("unused") RandomAccessWriteIterator it, int index, byte value) {
        setRawImpl(receiver, index, value);
    }

    private static void setRawImpl(RAbstractContainer receiver, int index, byte value) {
        RRawVector vec = (RRawVector) receiver;
        vec.setRawDataAt(vec.getInternalStore(), index, value);
    }

    // String

    @ExportMessage
    public static String[] getStringDataCopy(RAbstractContainer receiver) {
        return asString(receiver).getDataCopy();
    }

    @ExportMessage
    public static void setStringAt(RAbstractContainer receiver, int index, String value) {
        setStringImpl(receiver, index, value);
        if (RRuntime.isNA(value)) {
            ((RAbstractVector) receiver).setComplete(false);
        }
    }

    @ExportMessage
    public static void setNextString(RAbstractContainer receiver, SeqWriteIterator it, String value) {
        setStringImpl(receiver, it.getIndex(), value);
    }

    @ExportMessage
    public static void setString(RAbstractContainer receiver, @SuppressWarnings("unused") RandomAccessWriteIterator it, int index, String value) {
        setStringImpl(receiver, index, value);
    }

    private static void setStringImpl(RAbstractContainer receiver, int index, String value) {
        RStringVector vec = (RStringVector) receiver;
        vec.setDataAt(vec.getInternalStore(), index, value);
    }

    // Complex

    @ExportMessage
    public static double[] getComplexDataCopy(RAbstractContainer receiver) {
        return asComplex(receiver).getDataCopy();
    }

    @ExportMessage
    public static void setComplexAt(RAbstractContainer receiver, int index, RComplex value) {
        setComplexImpl(receiver, index, value);
        if (RRuntime.isNA(value)) {
            ((RAbstractVector) receiver).setComplete(false);
        }
    }

    @ExportMessage
    public static void setNextComplex(RAbstractContainer receiver, SeqWriteIterator it, RComplex value) {
        setComplexImpl(receiver, it.getIndex(), value);
    }

    @ExportMessage
    public static void setComplex(RAbstractContainer receiver, @SuppressWarnings("unused") RandomAccessWriteIterator it, int index, RComplex value) {
        setComplexImpl(receiver, index, value);
    }

    private static void setComplexImpl(RAbstractContainer receiver, int index, RComplex value) {
        RComplexVector vec = (RComplexVector) receiver;
        vec.setDataAt(vec.getInternalStore(), index, value);
    }

    // Lists/expressions

    @ExportMessage
    public static Object[] getListDataCopy(RAbstractContainer receiver) {
        return asList(receiver).getDataCopy();
    }

    @ExportMessage
    public static Object getElementAt(RAbstractContainer receiver, int index) {
        return receiver.getDataAtAsObject(index);
    }

    @ExportMessage
    public static Object getNextElement(RAbstractContainer receiver, SeqIterator it) {
        return receiver.getDataAtAsObject(it.getIndex());
    }

    @ExportMessage
    public static Object getElement(RAbstractContainer receiver, @SuppressWarnings("unused") RandomAccessIterator it, int index) {
        return receiver.getDataAtAsObject(index);
    }

    @ExportMessage
    public static void setElementAt(RAbstractContainer receiver, int index, Object value) {
        if (receiver instanceof RAbstractVector) {
            ((RAbstractVector) receiver).updateDataAtAsObject(index, value, NACheck.getEnabled());
        } else {
            throw RInternalError.unimplemented();
        }
    }

    @ExportMessage
    public static void setNextElement(RAbstractContainer receiver, SeqWriteIterator it, Object value) {
        if (receiver instanceof RAbstractVector) {
            ((RAbstractVector) receiver).updateDataAtAsObject(it.getIndex(), value, NACheck.getEnabled());
        } else {
            throw RInternalError.unimplemented();
        }
    }

    @ExportMessage
    public static void setElement(RAbstractContainer receiver, @SuppressWarnings("unused") RandomAccessWriteIterator it, int index, Object value) {
        if (receiver instanceof RAbstractVector) {
            ((RAbstractVector) receiver).updateDataAtAsObject(index, value, NACheck.getEnabled());
        } else {
            throw RInternalError.unimplemented();
        }
    }

    // Integers: should already have specialized library

    @ExportMessage
    public static int[] getIntDataCopy(RAbstractContainer receiver) {
        throw shouldHaveSpecializedLib(receiver);
    }

    // Utility methods

    private static RAbstractLogicalVector asLogical(RAbstractContainer obj) {
        return (RAbstractLogicalVector) obj;
    }

    private static RAbstractRawVector asRaw(RAbstractContainer obj) {
        return (RAbstractRawVector) obj;
    }

    private static RAbstractStringVector asString(RAbstractContainer obj) {
        return (RAbstractStringVector) obj;
    }

    private static RAbstractComplexVector asComplex(RAbstractContainer obj) {
        return (RAbstractComplexVector) obj;
    }

    private static RAbstractListBaseVector asList(RAbstractContainer obj) {
        return (RAbstractListBaseVector) obj;
    }

    private static RInternalError shouldHaveSpecializedLib(Object receiver) {
        CompilerDirectives.transferToInterpreter();
        String className = receiver != null ? receiver.getClass().getSimpleName() : "null";
        throw RInternalError.shouldNotReachHere(String.format("Object of type '%s' should already have specialized VectorDataLibrary implementation.", className));
    }
}
