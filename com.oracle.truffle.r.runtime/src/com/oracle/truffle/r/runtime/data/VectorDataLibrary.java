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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.library.GenerateLibrary;
import com.oracle.truffle.api.library.GenerateLibrary.DefaultExport;
import com.oracle.truffle.api.library.Library;
import com.oracle.truffle.api.library.LibraryFactory;
import static com.oracle.truffle.r.runtime.data.VectorDataLibrary.notWriteableError;

import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

@GenerateLibrary
@DefaultExport(DefaultRVectorDataLibrary.class)
public abstract class VectorDataLibrary extends Library {

    static final LibraryFactory<VectorDataLibrary> FACTORY = LibraryFactory.resolve(VectorDataLibrary.class);

    public static LibraryFactory<VectorDataLibrary> getFactory() {
        return FACTORY;
    }

    /**
     * If this method returns {@code true}, then it is guaranteed that this data does not contain any {@code NA} value. If this method returns {@code false}, then this data may or may not contain {@code NA} values.
     */
    @SuppressWarnings("unused")
    public boolean isComplete(Object data) {
        return false;
    }

    /**
     * If this method returns {@code true}, then it is guaranteed that this data is sorted in a way specified by the arguments {@code descending} and {@code naLast}.
     * If this method returns {@code false}, then this data may or may not be sorted.
     */
    @SuppressWarnings("unused")
    public boolean isSorted(Object receiver, boolean descending, boolean naLast) {
        return false;
    }

    /**
     * Returns {@code true} is this data object can be written to.
     */
    @SuppressWarnings("unused")
    public boolean isWriteable(Object data) {
        return false;
    }

    @SuppressWarnings("unused")
    public int getLength(Object data) {
        return 0;
    }

    public abstract Object getDataAtAsObject(Object data, int index);

    @SuppressWarnings("unused")
    public void setDataAtAsObject(Object data, int idx, Object value, NACheck naCheck) {
        CompilerDirectives.transferToInterpreter();
        throw notWriteableError(data.getClass(), "setDataAtAsObject");
    }

    /**
     * Transfers the element from receiver to the destination vector, where both vectors must be of the same type.
     */
    public static void transferElement(Object receiver, int receiverIdx, Object destination, int destinationIdx) {
        throw RInternalError.shouldNotReachHere("TODO");
    }

    public abstract Object materialize(Object receiver);

    public abstract Object copy(Object receiver, boolean deep);

    public abstract Object copyResized(Object receiver, int newSize, boolean deep, boolean fillNA);

    public abstract static class Iterator {
        private final Object store;
        private final int length;

        protected Iterator(Object store, int length) {
            this.store = store;
            this.length = length;
        }

        // Note: intentionally package private
        final Object getStore() {
            return store;
        }

        public final int getLength() {
            return length;
        }
    }

    public static final class SeqIterator extends Iterator {
        private int index;

        protected SeqIterator(Object store, int length) {
            super(store, length);
            index = -1;
        }

        public boolean next() {
            return ++index < getLength();
        }

        public void nextWithWrap() {
            // TODO
        }

        public int getIndex() {
            return index;
        }
    }

    public static final class RandomAccessIterator extends Iterator {
        protected RandomAccessIterator(Object store, int length) {
            super(store, length);
        }
    }

    public abstract SeqIterator iterator(Object receiver);

    public abstract RandomAccessIterator randomAccessIterator(Object receiver);

    // ---------------------------------------------------------------------
    // Methods specific to integer data

    /**
     * Gives a readonly Java array view on the data. The array may or may not be copy of the
     * underlying data. Note: if you need to send an array to the native code, you should use
     * {@code TODO:RAbstractVector#getDataPtr()} instead.
     */
    public int[] getReadonlyIntData(Object receiver) {
        return getIntDataCopy(receiver);
    }

    /**
     * Copies all the data into a Java array returned as the result of this method.
     */
    public int[] getIntDataCopy(Object receiver) {
        throw notImplemented(receiver);
    }

    public int getIntAt(Object receiver, int index) {
        throw notImplemented(receiver);
    }

    public int getNextInt(Object receiver, SeqIterator it) {
        throw notImplemented(receiver);
    }

    public int getInt(Object receiver, RandomAccessIterator it, int index) {
        throw notImplemented(receiver);
    }

    /**
     * Sets the value under given index. The vector must be writeable (see
     * {@link #isWriteable(Object)}. The {@code naCheck} is used to determine if it is necessary to
     * check whether {@code value} is {@code NA}. The {@code naCheck} must be "enabled" on the
     * source of the input data, i.e., the {@code value} argument. Using this overload makes sense
     * if this method is called multiple times with the same {@code naCheck} instance, otherwise use
     * the overload without the {@code naCheck}.
     */
    @SuppressWarnings("unused")
    public void setIntAt(Object receiver, int index, int value, NACheck naCheck) {
        throw notWriteableError(receiver, "setIntAt");
    }

    @SuppressWarnings("unused")
    public void setNextInt(Object receiver, SeqIterator it, int value, NACheck naCheck) {
        throw notWriteableError(receiver, "setNextInt");
    }

    @SuppressWarnings("unused")
    public void setInt(Object receiver, RandomAccessIterator it, int index, int value, NACheck naCheck) {
        throw notWriteableError(receiver, "setInt");
    }

    public final void setIntAt(Object receiver, int index, int value) {
        setIntAt(receiver, index, value, NACheck.getEnabled());
    }

    public final void setNextInt(Object receiver, SeqIterator it, int value) {
        setNextInt(receiver, it, value, NACheck.getEnabled());
    }

    public final void setInt(Object receiver, RandomAccessIterator it, int index, int value) {
        setInt(receiver, it, index, value, NACheck.getEnabled());
    }

    // ---------------------------------------------------------------------
    // Methods specific to double data

    /**
     * Gives a readonly Java array view on the data. The array may or may not be copy of the
     * underlying data. Note: if you need to send an array to the native code, you should use
     * {@code TODO:RAbstractVector#getDataPtr()} instead.
     */
    public double[] getReadonlyDoubleData(Object receiver) {
        return getDoubleDataCopy(receiver);
    }

    public double[] getDoubleDataCopy(Object receiver) {
        throw notImplemented(receiver);
    }

    public double getDoubleAt(Object receiver, int index) {
        throw notImplemented(receiver);
    }

    public double getNextDouble(Object receiver, SeqIterator it) {
        throw notImplemented(receiver);
    }

    public double getDouble(Object receiver, RandomAccessIterator it, int index) {
        throw notImplemented(receiver);
    }

    /**
     * Sets the value under given index. The vector must be writeable (see
     * {@link #isWriteable(Object)}. The {@code naCheck} is used to determine if it is necessary to
     * check whether {@code value} is {@code NA}. The {@code naCheck} must be "enabled" on the
     * source of the input data, i.e., the {@code value} argument. Using this overload makes sense
     * if this method is called multiple times with the same {@code naCheck} instance, otherwise use
     * the overload without the {@code naCheck}.
     */
    @SuppressWarnings("unused")
    public void setDoubleAt(Object receiver, int index, double value, NACheck naCheck) {
        throw notWriteableError(receiver, "setDoubleAt");
    }

    @SuppressWarnings("unused")
    public void setNextDouble(Object receiver, SeqIterator it, double value, NACheck naCheck) {
        throw notWriteableError(receiver, "setNextDouble");
    }

    @SuppressWarnings("unused")
    public void setDouble(Object receiver, RandomAccessIterator it, int index, double value, NACheck naCheck) {
        throw notWriteableError(receiver, "setDouble");
    }

    public final void setDoubleAt(Object receiver, int index, double value) {
        setDoubleAt(receiver, index, value, NACheck.getEnabled());
    }

    public final void setNextDouble(Object receiver, SeqIterator it, double value) {
        setNextDouble(receiver, it, value, NACheck.getEnabled());
    }

    public final void setDouble(Object receiver, RandomAccessIterator it, int index, double value) {
        setDouble(receiver, it, index, value, NACheck.getEnabled());
    }

    // ---------------------------------------------------------------------
    // Methods specific to logical data

    /**
     * Gives a readonly Java array view on the data. The array may or may not be copy of the
     * underlying data. Note: if you need to send an array to the native code, you should use
     * {@code TODO:RAbstractVector#getDataPtr()} instead.
     */
    public byte[] getReadonlyLogicalData(Object receiver) {
        return getLogicalDataCopy(receiver);
    }

    public byte[] getLogicalDataCopy(Object receiver) {
        throw notImplemented(receiver);
    }

    public byte getLogicalAt(Object receiver, int index) {
        throw notImplemented(receiver);
    }

    public byte getNextLogical(Object receiver, SeqIterator it) {
        throw notImplemented(receiver);
    }

    public byte getLogical(Object receiver, RandomAccessIterator it, int index) {
        throw notImplemented(receiver);
    }

    /**
     * Sets the value under given index. The vector must be writeable (see
     * {@link #isWriteable(Object)}. The {@code naCheck} is used to determine if it is necessary to
     * check whether {@code value} is {@code NA}. The {@code naCheck} must be "enabled" on the
     * source of the input data, i.e., the {@code value} argument. Using this overload makes sense
     * if this method is called multiple times with the same {@code naCheck} instance, otherwise use
     * the overload without the {@code naCheck}.
     */
    @SuppressWarnings("unused")
    public void setLogicalAt(Object receiver, int index, byte value, NACheck naCheck) {
        throw notWriteableError(receiver, "setLogicalAt");
    }

    @SuppressWarnings("unused")
    public void setNextLogical(Object receiver, SeqIterator it, byte value, NACheck naCheck) {
        throw notWriteableError(receiver, "setLogicalAt");
    }

    @SuppressWarnings("unused")
    public void setLogical(Object receiver, RandomAccessIterator it, int index, byte value, NACheck naCheck) {
        throw notWriteableError(receiver, "setLogicalAt");
    }

    public final void setLogicalAt(Object receiver, int index, byte value) {
        setLogicalAt(receiver, index, value, NACheck.getEnabled());
    }

    public final void setNextLogical(Object receiver, SeqIterator it, byte value) {
        setNextLogical(receiver, it, value, NACheck.getEnabled());
    }

    public final void setLogical(Object receiver, RandomAccessIterator it, int index, byte value) {
        setLogical(receiver, it, index, value, NACheck.getEnabled());
    }

    // ---------------------------------------------------------------------
    // Methods specific to raw data

    /**
     * Gives a readonly Java array view on the data. The array may or may not be copy of the
     * underlying data. Note: if you need to send an array to the native code, you should use
     * {@code TODO:RAbstractVector#getDataPtr()} instead.
     */
    public byte[] getReadonlyRawData(Object receiver) {
        return getLogicalDataCopy(receiver);
    }

    public byte[] getRawDataCopy(Object receiver) {
        throw notImplemented(receiver);
    }

    public byte getRawAt(Object receiver, int index) {
        throw notImplemented(receiver);
    }

    public byte getNextRaw(Object receiver, SeqIterator it) {
        throw notImplemented(receiver);
    }

    public byte getRaw(Object receiver, RandomAccessIterator it, int index) {
        throw notImplemented(receiver);
    }

    /**
     * Sets the value under given index. The vector must be writeable (see
     * {@link #isWriteable(Object)}. The {@code naCheck} is used to determine if it is necessary to
     * check whether {@code value} is {@code NA}. The {@code naCheck} must be "enabled" on the
     * source of the input data, i.e., the {@code value} argument. Using this overload makes sense
     * if this method is called multiple times with the same {@code naCheck} instance, otherwise use
     * the overload without the {@code naCheck}.
     */
    @SuppressWarnings("unused")
    public void setRawAt(Object receiver, int index, byte value, NACheck naCheck) {
        throw notWriteableError(receiver, "setRawAt");
    }

    @SuppressWarnings("unused")
    public void setNextRaw(Object receiver, SeqIterator it, byte value, NACheck naCheck) {
        throw notWriteableError(receiver, "setNextRaw");
    }

    @SuppressWarnings("unused")
    public void setRaw(Object receiver, RandomAccessIterator it, int index, byte value, NACheck naCheck) {
        throw notWriteableError(receiver, "setRaw");
    }

    public final void setRawAt(Object receiver, int index, byte value) {
        setRawAt(receiver, index, value, NACheck.getEnabled());
    }

    public final void setNextRaw(Object receiver, SeqIterator it, byte value) {
        setNextRaw(receiver, it, value, NACheck.getEnabled());
    }

    public final void setRaw(Object receiver, RandomAccessIterator it, int index, byte value) {
        setRaw(receiver, it, index, value, NACheck.getEnabled());
    }

    // ---------------------------------------------------------------------
    // Methods specific to String data
    // TODO: support for CharSXP

    /**
     * Gives a readonly Java array view on the data. The array may or may not be copy of the
     * underlying data. Note: if you need to send an array to the native code, you should use
     * {@code TODO:RAbstractVector#getDataPtr()} instead.
     */
    public String[] getReadonlyStringData(Object receiver) {
        return getStringDataCopy(receiver);
    }

    public String[] getStringDataCopy(Object receiver) {
        throw notImplemented(receiver);
    }

    public String getStringAt(Object receiver, int index) {
        throw notImplemented(receiver);
    }

    public String getNextString(Object receiver, SeqIterator it) {
        throw notImplemented(receiver);
    }

    public String getString(Object receiver, RandomAccessIterator it, int index) {
        throw notImplemented(receiver);
    }

    /**
     * Sets the value under given index. The vector must be writeable (see
     * {@link #isWriteable(Object)}. The {@code naCheck} is used to determine if it is necessary to
     * check whether {@code value} is {@code NA}. The {@code naCheck} must be "enabled" on the
     * source of the input data, i.e., the {@code value} argument. Using this overload makes sense
     * if this method is called multiple times with the same {@code naCheck} instance, otherwise use
     * the overload without the {@code naCheck}.
     */
    @SuppressWarnings("unused")
    public void setStringAt(Object receiver, int index, String value, NACheck naCheck) {
        throw notWriteableError(receiver, "setStringAt");
    }

    @SuppressWarnings("unused")
    public void setNextString(Object receiver, SeqIterator it, String value, NACheck naCheck) {
        throw notWriteableError(receiver, "setNextString");
    }

    @SuppressWarnings("unused")
    public void setString(Object receiver, RandomAccessIterator it, int index, String value, NACheck naCheck) {
        throw notWriteableError(receiver, "setRaw");
    }

    public final void setStringAt(Object receiver, int index, String value) {
        setStringAt(receiver, index, value, NACheck.getEnabled());
    }

    public final void setNextString(Object receiver, SeqIterator it, String value) {
        setNextString(receiver, it, value, NACheck.getEnabled());
    }

    public final void setRaw(Object receiver, RandomAccessIterator it, int index, String value) {
        setString(receiver, it, index, value, NACheck.getEnabled());
    }

    // ---------------------------------------------------------------------
    // Methods specific to complex data

    /**
     * Gives a readonly Java array view on the data. The array may or may not be copy of the
     * underlying data. Note: if you need to send an array to the native code, you should use
     * {@code TODO:RAbstractVector#getDataPtr()} instead.
     */
    public double[] getReadonlyComplexData(Object receiver) {
        return getComplexDataCopy(receiver);
    }

    public double[] getComplexDataCopy(Object receiver) {
        throw notImplemented(receiver);
    }

    public RComplex getComplexAt(Object receiver, int index) {
        throw notImplemented(receiver);
    }

    public RComplex getNextComplex(Object receiver, SeqIterator it) {
        throw notImplemented(receiver);
    }

    public RComplex getComplex(Object receiver, RandomAccessIterator it, int index) {
        throw notImplemented(receiver);
    }

    /**
     * Sets the value under given index. The vector must be writeable (see
     * {@link #isWriteable(Object)}. The {@code naCheck} is used to determine if it is necessary to
     * check whether {@code value} is {@code NA}. The {@code naCheck} must be "enabled" on the
     * source of the input data, i.e., the {@code value} argument. Using this overload makes sense
     * if this method is called multiple times with the same {@code naCheck} instance, otherwise use
     * the overload without the {@code naCheck}.
     */
    @SuppressWarnings("unused")
    public void setComplexAt(Object receiver, int index, RComplex value, NACheck naCheck) {
        throw notWriteableError(receiver, "setStringAt");
    }

    @SuppressWarnings("unused")
    public void setNextComplex(Object receiver, SeqIterator it, RComplex value, NACheck naCheck) {
        throw notWriteableError(receiver, "setNextString");
    }

    @SuppressWarnings("unused")
    public void setComplex(Object receiver, RandomAccessIterator it, int index, RComplex value, NACheck naCheck) {
        throw notWriteableError(receiver, "setRaw");
    }

    public final void setComplexAt(Object receiver, int index, RComplex value) {
        setComplexAt(receiver, index, value, NACheck.getEnabled());
    }

    public final void setNextComplex(Object receiver, SeqIterator it, RComplex value) {
        setNextComplex(receiver, it, value, NACheck.getEnabled());
    }

    public final void setComplex(Object receiver, RandomAccessIterator it, int index, RComplex value) {
        setComplex(receiver, it, index, value, NACheck.getEnabled());
    }

    // ---------------------------------------------------------------------
    // Methods specific to lists/expressions

    /**
     * Gives a readonly Java array view on the data. The array may or may not be copy of the
     * underlying data. Note: if you need to send an array to the native code, you should use
     * {@code TODO:RAbstractVector#getDataPtr()} instead.
     */
    public Object[] getReadonlyListData(Object receiver) {
        return getListDataCopy(receiver);
    }

    public Object[] getListDataCopy(Object receiver) {
        throw notImplemented(receiver);
    }

    public Object getListElementAt(Object receiver, int index) {
        throw notImplemented(receiver);
    }

    public Object getNextListElement(Object receiver, SeqIterator it) {
        throw notImplemented(receiver);
    }

    public Object getListElement(Object receiver, RandomAccessIterator it, int index) {
        throw notImplemented(receiver);
    }

    /**
     * Sets the value under given index. The vector must be writeable (see
     * {@link #isWriteable(Object)}. The {@code naCheck} is used to determine if it is necessary to
     * check whether {@code value} is {@code NA}. The {@code naCheck} must be "enabled" on the
     * source of the input data, i.e., the {@code value} argument. Using this overload makes sense
     * if this method is called multiple times with the same {@code naCheck} instance, otherwise use
     * the overload without the {@code naCheck}.
     */
    @SuppressWarnings("unused")
    public void setListElementAt(Object receiver, int index, Object value) {
        throw notWriteableError(receiver, "setListElementAt");
    }

    @SuppressWarnings("unused")
    public void setNextListElement(Object receiver, SeqIterator it, Object value) {
        throw notWriteableError(receiver, "setNextListElement");
    }

    @SuppressWarnings("unused")
    public void setListElement(Object receiver, RandomAccessIterator it, int index, Object value) {
        throw notWriteableError(receiver, "setListElement");
    }

    // Utility methods

    private static RInternalError notImplemented(Object receiver) {
        throw RInternalError.unimplemented(receiver == null ? "null" : receiver.getClass().getSimpleName());
    }

    public static RInternalError notWriteableError(Class<?> dataClass, String method) {
        CompilerDirectives.transferToInterpreter();
        throw RInternalError.shouldNotReachHere(String.format("RVectorData class '%s' is not writeable, it must be materialized before writing. Method: '%s'", dataClass.getSimpleName(), method));
    }

    public static RInternalError notWriteableError(Object data, String method) {
        CompilerDirectives.transferToInterpreter();
        String className = data != null ? data.getClass().getSimpleName() : "null";
        throw RInternalError.shouldNotReachHere(String.format("RVectorData class '%s' is not writeable, it must be materialized before writing. Method: '%s'", className, method));
    }
}
