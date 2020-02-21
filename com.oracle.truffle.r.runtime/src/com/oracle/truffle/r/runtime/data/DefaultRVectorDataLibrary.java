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

import static com.oracle.truffle.r.runtime.data.VectorDataLibrary.initInputNACheck;
import static com.oracle.truffle.r.runtime.data.VectorDataLibrary.notWriteableError;

import java.util.Arrays;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.profiles.LoopConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.runtime.RInternalError;
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
import com.oracle.truffle.r.runtime.ops.na.InputNACheck;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

@ExportLibrary(value = VectorDataLibrary.class, receiverType = RAbstractContainer.class)
public class DefaultRVectorDataLibrary {
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
                    @Shared("SeqItLoopProfile") @Cached("createCountingProfile()") LoopConditionProfile loopProfile) {
        SeqIterator it = new SeqIterator(null, receiver.getLength());
        it.initLoopConditionProfile(loopProfile);
        return it;
    }

    @ExportMessage
    public static boolean next(@SuppressWarnings("unused") RAbstractContainer receiver, SeqIterator it, boolean withWrap,
                    @Shared("SeqItLoopProfile") @Cached("createCountingProfile()") LoopConditionProfile loopProfile) {
        return it.next(loopProfile, withWrap);
    }

    @ExportMessage
    public static RandomAccessIterator randomAccessIterator(RAbstractContainer receiver) {
        return new RandomAccessIterator(null);
    }

    @ExportMessage
    public static SeqWriteIterator writeIterator(RAbstractContainer receiver, boolean inputIsComplete, @Shared("inputNACheck") @Cached InputNACheck naCheck) {
        initInputNACheck(naCheck, inputIsComplete, isComplete(receiver));
        return new SeqWriteIterator(null, receiver.getLength(), inputIsComplete);
    }

    @ExportMessage
    public static RandomAccessWriteIterator randomAccessWriteIterator(@SuppressWarnings("unused") RAbstractContainer receiver, boolean inputIsComplete,
                    @Shared("inputNACheck") @Cached InputNACheck naCheck) {
        initInputNACheck(naCheck, inputIsComplete, isComplete(receiver));
        return new RandomAccessWriteIterator(null, inputIsComplete);
    }

    @ExportMessage
    public static void commitWriteIterator(RAbstractContainer receiver, SeqWriteIterator iterator, @Shared("inputNACheck") @Cached InputNACheck naCheck) {
        iterator.commit();
        commitWrites(receiver, naCheck, iterator.inputIsComplete);
    }

    @ExportMessage
    public static void commitRandomAccessWriteIterator(RAbstractContainer receiver, RandomAccessWriteIterator iterator, @Shared("inputNACheck") @Cached InputNACheck naCheck) {
        iterator.commit();
        commitWrites(receiver, naCheck, iterator.inputIsComplete);
    }

    private static void commitWrites(RAbstractContainer receiver, InputNACheck naCheck, boolean inputIsComlete) {
        if (receiver instanceof RAbstractVector && naCheck.needsResettingCompleteFlag() && !inputIsComlete) {
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
    public static byte getNextLogical(RAbstractContainer receiver, SeqIterator it) {
        return asLogical(receiver).getDataAt(it.getIndex());
    }

    @ExportMessage
    public static byte getLogical(RAbstractContainer receiver, @SuppressWarnings("unused") RandomAccessIterator it, int index) {
        return asLogical(receiver).getDataAt(index);
    }

    @ExportMessage
    public static void setLogicalAt(RAbstractContainer receiver, int index, byte value, InputNACheck naCheck) {
        setLogicalImpl(receiver, index, value, naCheck);
        if (naCheck.needsResettingCompleteFlag()) {
            ((RAbstractVector) receiver).setComplete(false);
        }
    }

    @ExportMessage
    public static void setNextLogical(RAbstractContainer receiver, SeqWriteIterator it, byte value, @Shared("inputNACheck") @Cached InputNACheck naCheck) {
        setLogicalImpl(receiver, it.getIndex(), value, naCheck);
    }

    @ExportMessage
    public static void setLogical(RAbstractContainer receiver, RandomAccessWriteIterator it, int index, byte value, @Shared("inputNACheck") @Cached InputNACheck naCheck) {
        setLogicalImpl(receiver, index, value, naCheck);
    }

    private static void setLogicalImpl(RAbstractContainer receiver, int index, byte value, InputNACheck naCheck) {
        if (!(receiver instanceof RLogicalVector)) {
            throw notWriteableError(receiver, "setLogicalAt");
        }
        RLogicalVector vec = (RLogicalVector) receiver;
        vec.setDataAt(vec.getInternalStore(), index, value);
        naCheck.check(value);
    }

    // Raw

    @ExportMessage
    public static byte[] getRawDataCopy(RAbstractContainer receiver) {
        byte[] data = asRaw(receiver).getReadonlyData();
        return Arrays.copyOf(data, data.length);
    }

    @ExportMessage
    public static byte getRawAt(RAbstractContainer receiver, int index) {
        return asRaw(receiver).getRawDataAt(index);
    }

    @ExportMessage
    public static byte getNextRaw(RAbstractContainer receiver, SeqIterator it) {
        return asRaw(receiver).getRawDataAt(it.getIndex());
    }

    @ExportMessage
    public static byte getRaw(RAbstractContainer receiver, @SuppressWarnings("unused") RandomAccessIterator it, int index) {
        return asRaw(receiver).getRawDataAt(index);
    }

    @ExportMessage
    public static void setRawAt(RAbstractContainer receiver, int index, byte value, InputNACheck naCheck) {
        setRawImpl(receiver, index, value, naCheck);
        if (naCheck.needsResettingCompleteFlag()) {
            ((RAbstractVector) receiver).setComplete(false);
        }
    }

    @ExportMessage
    public static void setNextRaw(RAbstractContainer receiver, SeqWriteIterator it, byte value, @Shared("inputNACheck") @Cached InputNACheck naCheck) {
        setRawImpl(receiver, value, value, naCheck);
    }

    @ExportMessage
    public static void setRaw(RAbstractContainer receiver, RandomAccessWriteIterator it, int index, byte value, @Shared("inputNACheck") @Cached InputNACheck naCheck) {
        setRawImpl(receiver, value, value, naCheck);
    }

    private static void setRawImpl(RAbstractContainer receiver, int index, byte value, InputNACheck naCheck) {
        if (!(receiver instanceof RLogicalVector)) {
            throw notWriteableError(receiver, "setRawAt");
        }
        RLogicalVector vec = (RLogicalVector) receiver;
        vec.setDataAt(vec.getInternalStore(), index, value);
        naCheck.check(value);
    }

    // String

    @ExportMessage
    public static String[] getStringDataCopy(RAbstractContainer receiver) {
        return asString(receiver).getDataCopy();
    }

    @ExportMessage
    public static String getStringAt(RAbstractContainer receiver, int index) {
        return asString(receiver).getDataAt(index);
    }

    @ExportMessage
    public static String getNextString(RAbstractContainer receiver, SeqIterator it) {
        return asString(receiver).getDataAt(it.getIndex());
    }

    @ExportMessage
    public static String getString(RAbstractContainer receiver, @SuppressWarnings("unused") RandomAccessIterator it, int index) {
        return asString(receiver).getDataAt(index);
    }

    @ExportMessage
    public static void setStringAt(RAbstractContainer receiver, int index, String value, InputNACheck naCheck) {
        setStringImpl(receiver, index, value, naCheck);
        if (naCheck.needsResettingCompleteFlag()) {
            ((RAbstractVector) receiver).setComplete(false);
        }
    }

    @ExportMessage
    public static void setNextString(RAbstractContainer receiver, SeqWriteIterator it, String value, @Shared("inputNACheck") @Cached InputNACheck naCheck) {
        setStringImpl(receiver, it.getIndex(), value, naCheck);
    }

    @ExportMessage
    public static void setString(RAbstractContainer receiver, RandomAccessWriteIterator it, int index, String value, @Shared("inputNACheck") @Cached InputNACheck naCheck) {
        setStringImpl(receiver, index, value, naCheck);
    }

    private static void setStringImpl(RAbstractContainer receiver, int index, String value, InputNACheck naCheck) {
        if (!(receiver instanceof RStringVector)) {
            throw notWriteableError(receiver, "setStringAt");
        }
        RStringVector vec = (RStringVector) receiver;
        vec.setDataAt(vec.getInternalStore(), index, value);
        naCheck.check(value);
    }

    // Complex

    @ExportMessage
    public static double[] getComplexDataCopy(RAbstractContainer receiver) {
        return asComplex(receiver).getDataCopy();
    }

    @ExportMessage
    public static RComplex getComplexAt(RAbstractContainer receiver, int index) {
        return asComplex(receiver).getDataAt(index);
    }

    @ExportMessage
    public static RComplex getNextComplex(RAbstractContainer receiver, SeqIterator it) {
        return asComplex(receiver).getDataAt(it.getIndex());
    }

    @ExportMessage
    public static RComplex getComplex(RAbstractContainer receiver, @SuppressWarnings("unused") RandomAccessIterator it, int index) {
        return asComplex(receiver).getDataAt(index);
    }

    @ExportMessage
    public static void setComplexAt(RAbstractContainer receiver, int index, RComplex value, InputNACheck naCheck) {
        setComplexImpl(receiver, index, value, naCheck);
        if (naCheck.needsResettingCompleteFlag()) {
            ((RAbstractVector) receiver).setComplete(false);
        }
    }

    @ExportMessage
    public static void setNextComplex(RAbstractContainer receiver, SeqWriteIterator it, RComplex value, @Shared("inputNACheck") @Cached InputNACheck naCheck) {
        setComplexImpl(receiver, it.getIndex(), value, naCheck);
    }

    @ExportMessage
    public static void setComplex(RAbstractContainer receiver, RandomAccessWriteIterator it, int index, RComplex value, @Shared("inputNACheck") @Cached InputNACheck naCheck) {
        setComplexImpl(receiver, index, value, naCheck);
    }

    private static void setComplexImpl(RAbstractContainer receiver, int index, RComplex value, InputNACheck naCheck) {
        if (!(receiver instanceof RComplexVector)) {
            throw notWriteableError(receiver, "setStringAt");
        }
        RComplexVector vec = (RComplexVector) receiver;
        vec.setDataAt(vec.getInternalStore(), index, value);
        naCheck.check(value);
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

    @ExportMessage
    public static int getIntAt(RAbstractContainer receiver, @SuppressWarnings("unused") int index) {
        throw shouldHaveSpecializedLib(receiver);
    }

    @ExportMessage
    public static int getNextInt(RAbstractContainer receiver, @SuppressWarnings("unused") SeqIterator it) {
        throw shouldHaveSpecializedLib(receiver);
    }

    @ExportMessage
    public static int getInt(RAbstractContainer receiver, @SuppressWarnings("unused") RandomAccessIterator it, @SuppressWarnings("unused") int index) {
        throw shouldHaveSpecializedLib(receiver);
    }

    // Doubles: should already have specialized library

    @ExportMessage
    public static double[] getDoubleDataCopy(RAbstractContainer receiver) {
        throw shouldHaveSpecializedLib(receiver);
    }

    @ExportMessage
    public static double getDoubleAt(RAbstractContainer receiver, @SuppressWarnings("unused") int index) {
        throw shouldHaveSpecializedLib(receiver);
    }

    @ExportMessage
    public static double getNextDouble(RAbstractContainer receiver, @SuppressWarnings("unused") SeqIterator it) {
        throw shouldHaveSpecializedLib(receiver);
    }

    @ExportMessage
    public static double getDouble(RAbstractContainer receiver, @SuppressWarnings("unused") RandomAccessIterator it, @SuppressWarnings("unused") int index) {
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
