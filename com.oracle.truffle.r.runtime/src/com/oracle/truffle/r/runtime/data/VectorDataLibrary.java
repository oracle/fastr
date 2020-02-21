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
import com.oracle.truffle.api.profiles.LoopConditionProfile;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.Asserts;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.ops.na.InputNACheck;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

// TODO:
// 1. rewrite WriteIndexedVectorNode (done)
// 1.1: intermezzo: marker interface for data objects to catch errors when we are passing unrelated object to the library - issue, this does not allow Integer to be a data object
// 1.2: specialize on right.isComplete()
// 2. rewrite CumSum (uses NACheck from vector access)
// 3. rewrite AnyNA (uses getType from vector access)
// 3. rewrite PPsum?
// Performance compatibility: data objs update complete flag of the vector + length is kept in vectors too
// N. API for vectors materialization/reuse
// check other APIs on VectorAccess

/**
 * Truffle library for objects that represent data of {@link RAbstractVector} objects. Data objects
 * represent an array of elements of the vector. For example, {@code int} array for
 * {@link RIntVector}. {@link RAbstractVector} is a simple data holder of the data object (
 * {@link RAbstractVector#getData()}) and an object representing the attributes.
 *
 * The API defined in this Truffle library consists of type independent methods, such as
 * {@link #materialize(Object)}, and several sets of methods that depend on the vector type, for
 * example, {@link #setIntAt(Object, int, int)}. There is a set of those methods for all vector
 * types including heterogeneous vectors (lists/expressions).
 *
 * Overview of the API for getting/setting vector data elements:
 * <p>
 * Note: this documentation refers to {@code int} variants of the API, but it applies to any type.
 * <p>
 * Elements of vector data can be accessed in three ways:
 * <ul>
 * <li>Sequentially using an iterator object that encapsulates the index advancement including a
 * wrap-up if desired ({@link #getNextInt(Object, SeqIterator)})</li>
 * <li>At random positions using an iterator object (
 * {@link #getInt(Object, RandomAccessIterator, int)})</li>
 * <li>At random positions without any iterator object ({@link #getIntAt(Object, int)})</li>
 * </ul>
 * <p>
 * Using an iterator object helps to optimize memory loads of fields from the vector data object.
 * The vector data object usually cannot be escape analyzed, because its life spans over more
 * compilation units. The iterator object is usually local just to some node/specialization and can
 * be simply escape analyzed and replaced with local variables: the memory loads of fields from the
 * vector data will take place only once when constructing the iterator object. It is important that
 * the iterator objects are simple data-holders and do not have any polymorphic behavior. The
 * iterator objects should be opaque to the users of {@link VectorDataLibrary} with the exception of
 * {@link SeqIterator#getLength()}.
 * <p>
 * If you are accessing only one element, then using {@link #getIntAt(Object, int)} is sufficient
 * and using an iterator object will not improve the performance.
 * <p>
 * Elements of vector data can be written in three ways:
 * <ul>
 * <li>Sequentially using an iterator object that encapsulates the index advancement including a
 * wrap-up if desired ({@link #setNextInt(Object, SeqWriteIterator, int)})</li>
 * <li>At random positions using an iterator object (
 * {@link #setInt(Object, RandomAccessWriteIterator, int, int)})</li>
 * <li>At random positions without an iterator object (
 * {@link #setIntAt(Object, int, int, InputNACheck)})</li>
 * </ul>
 * <p>
 * Write operations require {@link InputNACheck}, which is either passed when creating the iterator
 * object or directly in case of {@link #setIntAt(Object, int, int, InputNACheck)}. The
 * {@link InputNACheck} must be enabled if the source of the "input" data may contain {@code NA}
 * values (see {@link InputNACheck#enable(boolean, boolean)}. This allows to optimize away the check
 * for {@code NA} values. Moreover, the data object usually needs to update its "complete" field
 * when {@code NA} value is written. The memory write of that "complete" field is moved to
 * {@link #commitWriteIterator(Object, SeqWriteIterator)} in case of the iterator API, so that the
 * memory write is outside of the loop that usually writes values to the vector object.
 */
@GenerateLibrary(assertions = Asserts.class)
@DefaultExport(DefaultRVectorDataLibrary.class)
public abstract class VectorDataLibrary extends Library {

    private static final boolean ENABLE_VERY_SLOW_ASSERTS = false;

    static final LibraryFactory<VectorDataLibrary> FACTORY = LibraryFactory.resolve(VectorDataLibrary.class);

    public static LibraryFactory<VectorDataLibrary> getFactory() {
        return FACTORY;
    }

    /**
     * If this method returns {@code true}, then it is guaranteed that this data does not contain
     * any {@code NA} value. If this method returns {@code false}, then this data may or may not
     * contain {@code NA} values.
     */
    @SuppressWarnings("unused")
    public boolean isComplete(Object data) {
        return false;
    }

    /**
     * If this method returns {@code true}, then it is guaranteed that this data is sorted in a way
     * specified by the arguments {@code descending} and {@code naLast}. If this method returns
     * {@code false}, then this data may or may not be sorted.
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
    public abstract int getLength(Object data);

    public abstract RType getType(Object data);

    /**
     * Returns an instance of object that implements {@link VectorDataLibrary} and it guaranteed to
     * return {@code true} from {@link #isWriteable(Object)}. The result may be the same as
     * {@code receiver} or a new fresh instance.
     */
    public abstract Object materialize(Object data);

    public abstract Object copy(Object data, boolean deep);

    public abstract Object copyResized(Object data, int newSize, boolean deep, boolean fillNA);

    /**
     * Iterator objects should be opaque to the users of {@link VectorDataLibrary} any state except
     * for {@link SeqIterator#getLength()} is intentionally package private.
     */
    public abstract static class Iterator {
        private final Object store;

        protected Iterator(Object store) {
            this.store = store;
        }

        final Object getStore() {
            return store;
        }
    }

    public static class SeqIterator extends Iterator {
        private final int length;
        private int index;

        protected SeqIterator(Object store, int length) {
            super(store);
            index = -1;
            this.length = length;
        }

        public final int getLength() {
            return length;
        }

        public final int getIndex() {
            return index;
        }

        final void initLoopConditionProfile(LoopConditionProfile profile) {
            profile.profileCounted(length);
        }

        final boolean next(LoopConditionProfile loopConditionProfile, boolean withWrap) {
            if (withWrap) {
                index++;
                if (loopConditionProfile.inject(index == length)) {
                    index = 0;
                }
                return true;
            }
            return loopConditionProfile.inject(++index < length);
        }
    }

    public static class RandomAccessIterator extends Iterator {
        RandomAccessIterator(Object store) {
            super(store);
        }
    }

    public static final class SeqWriteIterator extends SeqIterator implements AutoCloseable {
        final boolean inputIsComplete;
        private boolean committed; // used for assertions only

        protected SeqWriteIterator(Object store, int length, boolean inputIsComplete) {
            super(store, length);
            this.inputIsComplete = inputIsComplete;
        }

        void commit() {
            assert committed = true;
        }

        @Override
        public void close() {
            // commitWriteIterator was not called or the library impl. is not setting the
            // 'committed' flag in the iterator
            assert committed;
        }
    }

    public static final class RandomAccessWriteIterator extends RandomAccessIterator implements AutoCloseable {
        final boolean inputIsComplete;
        private boolean committed; // used for assertions only

        protected RandomAccessWriteIterator(Object store, boolean inputIsComplete) {
            super(store);
            this.inputIsComplete = inputIsComplete;
        }

        void commit() {
            assert committed = true;
        }

        @Override
        public void close() {
            // commitWriteIterator was not called or the library impl. is not setting the
            // 'committed' flag in the iterator
            assert committed;
        }
    }

    static void initInputNACheck(InputNACheck naCheck, boolean inputIsComplete, boolean destIsComplete) {
        naCheck.enable(destIsComplete, inputIsComplete);
        if (!destIsComplete) {
            // No point in checking NAs if the destination is already marked as incomplete vector
            naCheck.disableChecks();
        }
    }

    /**
     * Returns an iterator object. Using an iterator object allows to better optimize the operation
     * of getting data at given index. See the documentation of {@link VectorDataLibrary} for high
     * level overview.
     *
     * The implementation should use {@link LoopConditionProfile} shared with
     * {@link #next(Object, SeqIterator, boolean)} message and it should initialize it using
     * {@link SeqIterator#initLoopConditionProfile(LoopConditionProfile)}.
     */
    public abstract SeqIterator iterator(Object receiver);

    /**
     * Advances the iterator to the next element. Prefer usage of one of the convenience overloads
     * {@link #next(Object, SeqIterator)} or {@link #nextWithWrap(Object, SeqIterator)}.
     *
     * The implementation should use {@link LoopConditionProfile} shared with
     * {@link #iterator(Object)} message and should pass the profile to
     * {@link SeqIterator#next(LoopConditionProfile,boolean)}.
     */
    public abstract boolean next(Object receiver, SeqIterator it, boolean withWrap);

    public final boolean next(Object receiver, SeqIterator it) {
        return next(receiver, it, false);
    }

    public final void nextWithWrap(Object receiver, SeqIterator it) {
        next(receiver, it, true);
    }

    /**
     * @see #iterator(Object)
     */
    public abstract RandomAccessIterator randomAccessIterator(Object receiver);

    /**
     * Returns an iterator object. Using an iterator object allows to better optimize the operation
     * of getting data at given index. See the documentation of {@link VectorDataLibrary} for high
     * level overview. The {@code receiver} object must be {@link #isWriteable(Object)}.
     * <p>
     * The {@code inputIsComplete} parameter should be {@code true} of if we know that the values
     * that will be written do not contain any {@code NA}. Usually one is writing data that
     * originate from one or more "input" vectors, the value of {@code inputIsComplete} should be
     * logical AND of {@link #isComplete(Object)} flags of those vectors. Note that if the data from
     * the "input" vectors are processed by some operation that can produce {@code NA} even if the
     * none of the inputs is {@code NA}, then {@code inputIsComplete} must be set to {@code false}.
     * <p>
     * Writes using a write iterator must be committed using
     * {@link #commitWriteIterator(Object, SeqWriteIterator)}.
     */
    public SeqWriteIterator writeIterator(Object receiver, @SuppressWarnings("unused") boolean inputIsComplete) {
        throw notWriteableError(receiver, "writeIterator");
    }

    /**
     * @see #writeIterator(Object, boolean)
     */
    public RandomAccessWriteIterator randomAccessWriteIterator(Object receiver, @SuppressWarnings("unused") boolean inputIsComplete) {
        throw notWriteableError(receiver, "randomAccessWriteIterator");
    }

    /**
     * Writes using a write iterator must be committed using this method. Moreover, the iterators
     * should be used with the try-with-resources pattern. The {@code close} method checks that
     * {@link #commitWriteIterator(Object, SeqWriteIterator)} was invoked.
     *
     * This is a place where implementations that dynamically maintain the "complete" flag will
     * check the {@code inputIsComplete} flag passed to {@link #writeIterator(Object, boolean)}.
     * They should also use a {@code @Shared @Cached} instance of {@link InputNACheck} and the
     * {@link InputNACheck#needsResettingCompleteFlag()} should be also used to determine if the
     * "complete" flag of this vector should be set to {@code false}.
     *
     * Make sure to update the {@link SeqWriteIterator#committed} flag to {@code true}. It is
     * checked with assertion in the {@link SeqWriteIterator#close()} method.
     *
     * Implementations that are not writeable, or that always return {@code false} from
     * {@link #isComplete(Object)} do not need to implement this message.
     *
     * Note: we cannot implement this in the {@link AutoCloseable#close()} method of
     * {@link SeqWriteIterator}, because we need to pass in the {@code receiver} object.
     *
     * @see #writeIterator(Object, boolean)
     */
    public void commitWriteIterator(Object receiver, SeqWriteIterator iterator) {
        iterator.commit();
    }

    /**
     * @see #commitWriteIterator(Object, SeqWriteIterator)
     */
    public void commitRandomAccessWriteIterator(Object receiver, RandomAccessWriteIterator iterator) {
        iterator.commit();
    }

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

    /**
     * Returns the value at given position. See the documentation of {@link #iterator(Object)} for
     * details.
     */
    public int getIntAt(Object receiver, int index) {
        throw notImplemented(receiver);
    }

    /**
     * Returns the value at position given by the iterator which should be constructed by calling
     * {@link #iterator(Object)} on the same {@code receiver} object. See the documentation of
     * {@link VectorDataLibrary} for a high level overview.
     */
    public int getNextInt(Object receiver, SeqIterator it) {
        throw notImplemented(receiver);
    }

    /**
     * Returns the value at given position. The iterator which should be constructed by calling
     * {@link #randomAccessIterator(Object)} on the same {@code receiver} object. See the
     * documentation of {@link VectorDataLibrary} for a high level overview.
     */
    public int getInt(Object receiver, RandomAccessIterator it, int index) {
        throw notImplemented(receiver);
    }

    /**
     * Sets the value under given index. The vector must be writeable (see
     * {@link #isWriteable(Object)}. The {@code naCheck} is used to determine if it is necessary to
     * check whether {@code value} is {@code NA}. The {@code naCheck} must be "enabled" on the
     * source of the input data, i.e., the {@code value} argument. Using this overload makes sense
     * if this method is called multiple times with the same {@code naCheck} instance stored as a
     * filed in AST or a cached parameter of specialization, otherwise use the overload without the
     * {@code naCheck}.
     *
     * See the documentation of {@link VectorDataLibrary} for a high level overview.
     */
    @SuppressWarnings("unused")
    public void setIntAt(Object receiver, int index, int value, InputNACheck naCheck) {
        throw notWriteableError(receiver, "setIntAt");
    }

    /**
     * Sets the value under the index given by the iterator. See the documentation of
     * {@link VectorDataLibrary} for a high level overview.
     *
     * @see #iterator(Object)
     */
    @SuppressWarnings("unused")
    public void setNextInt(Object receiver, SeqWriteIterator it, int value) {
        throw notWriteableError(receiver, "setNextInt");
    }

    /**
     * Sets the value under the given index using the given iterator to optimize some operations.
     * See the documentation of {@link VectorDataLibrary} for a high level overview.
     *
     * @see #iterator(Object)
     */
    @SuppressWarnings("unused")
    public void setInt(Object receiver, RandomAccessWriteIterator it, int index, int value) {
        throw notWriteableError(receiver, "setInt");
    }

    /**
     * Convenience overload of {@link #setIntAt(Object, int, int, InputNACheck)}, which uses enabled
     * {@link InputNACheck}. It is recommended to use the other overload and actual
     * {@link InputNACheck} to enable more optimizations.
     */
    public final void setIntAt(Object receiver, int index, int value) {
        setIntAt(receiver, index, value, InputNACheck.SEEN_NA);
    }

    // ---------------------------------------------------------------------
    // Methods specific to double data

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

    @SuppressWarnings("unused")
    public void setDoubleAt(Object receiver, int index, double value, InputNACheck naCheck) {
        throw notWriteableError(receiver, "setDoubleAt");
    }

    @SuppressWarnings("unused")
    public void setNextDouble(Object receiver, SeqWriteIterator it, double value) {
        throw notWriteableError(receiver, "setNextDouble");
    }

    @SuppressWarnings("unused")
    public void setDouble(Object receiver, RandomAccessWriteIterator it, int index, double value) {
        throw notWriteableError(receiver, "setDouble");
    }

    public final void setDoubleAt(Object receiver, int index, double value) {
        setDoubleAt(receiver, index, value, InputNACheck.SEEN_NA);
    }

    // ---------------------------------------------------------------------
    // Methods specific to logical data

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

    @SuppressWarnings("unused")
    public void setLogicalAt(Object receiver, int index, byte value, InputNACheck naCheck) {
        throw notWriteableError(receiver, "setLogicalAt");
    }

    @SuppressWarnings("unused")
    public void setNextLogical(Object receiver, SeqWriteIterator it, byte value) {
        throw notWriteableError(receiver, "setLogicalAt");
    }

    @SuppressWarnings("unused")
    public void setLogical(Object receiver, RandomAccessWriteIterator it, int index, byte value) {
        throw notWriteableError(receiver, "setLogicalAt");
    }

    public final void setLogicalAt(Object receiver, int index, byte value) {
        setLogicalAt(receiver, index, value, InputNACheck.SEEN_NA);
    }

    // ---------------------------------------------------------------------
    // Methods specific to raw data

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

    @SuppressWarnings("unused")
    public void setRawAt(Object receiver, int index, byte value, InputNACheck naCheck) {
        throw notWriteableError(receiver, "setRawAt");
    }

    @SuppressWarnings("unused")
    public void setNextRaw(Object receiver, SeqWriteIterator it, byte value) {
        throw notWriteableError(receiver, "setNextRaw");
    }

    @SuppressWarnings("unused")
    public void setRaw(Object receiver, RandomAccessWriteIterator it, int index, byte value) {
        throw notWriteableError(receiver, "setRaw");
    }

    public final void setRawAt(Object receiver, int index, byte value) {
        setRawAt(receiver, index, value, InputNACheck.SEEN_NA);
    }

    // ---------------------------------------------------------------------
    // Methods specific to String data
    // TODO: support for CharSXP

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

    @SuppressWarnings("unused")
    public void setStringAt(Object receiver, int index, String value, InputNACheck naCheck) {
        throw notWriteableError(receiver, "setStringAt");
    }

    @SuppressWarnings("unused")
    public void setNextString(Object receiver, SeqWriteIterator it, String value) {
        throw notWriteableError(receiver, "setNextString");
    }

    @SuppressWarnings("unused")
    public void setString(Object receiver, RandomAccessWriteIterator it, int index, String value) {
        throw notWriteableError(receiver, "setRaw");
    }

    public final void setStringAt(Object receiver, int index, String value) {
        setStringAt(receiver, index, value, InputNACheck.SEEN_NA);
    }

    // ---------------------------------------------------------------------
    // Methods specific to complex data

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

    @SuppressWarnings("unused")
    public void setComplexAt(Object receiver, int index, RComplex value, InputNACheck naCheck) {
        throw notWriteableError(receiver, "setStringAt");
    }

    @SuppressWarnings("unused")
    public void setNextComplex(Object receiver, SeqWriteIterator it, RComplex value) {
        throw notWriteableError(receiver, "setNextString");
    }

    @SuppressWarnings("unused")
    public void setComplex(Object receiver, RandomAccessWriteIterator it, int index, RComplex value) {
        throw notWriteableError(receiver, "setRaw");
    }

    public final void setComplexAt(Object receiver, int index, RComplex value) {
        setComplexAt(receiver, index, value, InputNACheck.SEEN_NA);
    }

    // ---------------------------------------------------------------------
    // Methods for accessing heterogeneous vectors (list, expression, ...) and for accessing all
    // vectors in a generic way via Object, which causes unnecessary boxing/unboxing.
    // Note: the switches over VectorDataLibrary.getType(...) should be partially evaluated to just
    // a single branch, because getType(...) should be a compilation constant unless the Truffle
    // library cache overflows

    public Object[] getReadonlyListData(Object receiver) {
        return getListDataCopy(receiver);
    }

    public Object[] getListDataCopy(Object receiver) {
        throw notImplemented(receiver);
    }

    public Object getElementAt(Object receiver, int index) {
        RType type = getType(receiver);
        switch (type) {
            case Integer:
                return getIntAt(receiver, index);
            case Double:
                return getDoubleAt(receiver, index);
            case Logical:
                return getLogicalAt(receiver, index);
            case Raw:
                return getRawAt(receiver, index);
            case Complex:
                return getComplexAt(receiver, index);
            case Character:
                return getStringAt(receiver, index);
            default:
                CompilerDirectives.transferToInterpreter();
                throw notImplemented(receiver);
        }
    }

    public Object getNextElement(Object receiver, SeqIterator it) {
        RType type = getType(receiver);
        switch (type) {
            case Integer:
                return getNextInt(receiver, it);
            case Double:
                return getNextDouble(receiver, it);
            case Logical:
                return getNextLogical(receiver, it);
            case Raw:
                return getNextRaw(receiver, it);
            case Complex:
                return getNextComplex(receiver, it);
            case Character:
                return getNextString(receiver, it);
            default:
                CompilerDirectives.transferToInterpreter();
                throw notImplemented(receiver);
        }
    }

    public Object getElement(Object receiver, RandomAccessIterator it, int index) {
        RType type = getType(receiver);
        switch (type) {
            case Integer:
                return getInt(receiver, it, index);
            case Double:
                return getDouble(receiver, it, index);
            case Logical:
                return getLogical(receiver, it, index);
            case Raw:
                return getRaw(receiver, it, index);
            case Complex:
                return getComplex(receiver, it, index);
            case Character:
                return getString(receiver, it, index);
            default:
                CompilerDirectives.transferToInterpreter();
                throw notImplemented(receiver);
        }
    }

    @SuppressWarnings("unused")
    public void setElementAt(Object receiver, int index, Object value) {
        RType type = getType(receiver);
        switch (type) {
            case Integer:
                setIntAt(receiver, index, (Integer) value, InputNACheck.SEEN_NA);
                break;
            case Double:
                setDoubleAt(receiver, index, (Double) value, InputNACheck.SEEN_NA);
                break;
            case Logical:
                setLogicalAt(receiver, index, (Byte) value, InputNACheck.SEEN_NA);
                break;
            case Raw:
                setRawAt(receiver, index, (Byte) value, InputNACheck.SEEN_NA);
                break;
            case Complex:
                setComplexAt(receiver, index, (RComplex) value, InputNACheck.SEEN_NA);
                break;
            case Character:
                setStringAt(receiver, index, (String) value, InputNACheck.SEEN_NA);
                break;
            default:
                CompilerDirectives.transferToInterpreter();
                throw notImplemented(receiver);
        }
    }

    public void setNextElement(Object receiver, SeqWriteIterator it, Object value) {
        setElementAt(receiver, it.getIndex(), value);
    }

    public void setElement(Object receiver, @SuppressWarnings("unused") RandomAccessWriteIterator it, int index, Object value) {
        setElementAt(receiver, index, value);
    }

    // Utility methods. The instance utility methods are not intended for overriding although they
    // may be overridden if the need is.

    public static void transferElementSameType(VectorDataLibrary destLib, RandomAccessWriteIterator destIt, Object dest, int destIdx, VectorDataLibrary sourceLib, RandomAccessIterator sourceIt,
                    Object source, int sourceIdx) {
        RType type = sourceLib.getType(source);
        assert type == destLib.getType(dest);
        switch (type) {
            case Integer:
                destLib.setInt(dest, destIt, destIdx, sourceLib.getInt(source, sourceIt, sourceIdx));
                break;
            case Double:
                destLib.setDouble(dest, destIt, destIdx, sourceLib.getDouble(source, sourceIt, sourceIdx));
                break;
            case Logical:
                destLib.setLogical(dest, destIt, destIdx, sourceLib.getLogical(source, sourceIt, sourceIdx));
                break;
            case Raw:
                destLib.setRaw(dest, destIt, destIdx, sourceLib.getRaw(source, sourceIt, sourceIdx));
                break;
            case Complex:
                destLib.setComplex(dest, destIt, destIdx, sourceLib.getComplex(source, sourceIt, sourceIdx));
                break;
            case Character:
                destLib.setString(dest, destIt, destIdx, sourceLib.getString(source, sourceIt, sourceIdx));
                break;
            case List:
            case PairList:
            case Language:
            case Expression:
                destLib.setElement(dest, destIt, destIdx, sourceLib.getElement(source, sourceIt, sourceIdx));
                break;
            default:
                CompilerDirectives.transferToInterpreter();
                throw RInternalError.unimplemented(type.toString());
        }
    }

    public Object getDataAtAsObject(Object data, int index) {
        RType type = getType(data);
        switch (type) {
            case Integer:
                return getIntAt(data, index);
            case Double:
                return getDoubleAt(data, index);
            case Logical:
                return getLogicalAt(data, index);
            case Raw:
                return getRawAt(data, index);
            case Complex:
                return getComplexAt(data, index);
            case Character:
                return getStringAt(data, index);
            case List:
            case PairList:
            case Language:
            case Expression:
                return getElementAt(data, index);
            default:
                CompilerDirectives.transferToInterpreter();
                throw RInternalError.shouldNotReachHere(type.toString());
        }
    }

    public void setDataAtAsObject(Object data, int index, Object value, InputNACheck naCheck) {
        RType type = getType(data);
        switch (type) {
            case Integer:
                setIntAt(data, index, (Integer) value, naCheck);
                break;
            case Double:
                setDoubleAt(data, index, (Double) value, naCheck);
                break;
            case Logical:
                setLogicalAt(data, index, (Byte) value, naCheck);
                break;
            case Raw:
                setRawAt(data, index, (Byte) value, naCheck);
                break;
            case Complex:
                setComplexAt(data, index, (RComplex) value, naCheck);
                break;
            case Character:
                setStringAt(data, index, (String) value, naCheck);
                break;
            case List:
            case PairList:
            case Language:
            case Expression:
                setElementAt(data, index, value);
                break;
            default:
                CompilerDirectives.transferToInterpreter();
                throw RInternalError.shouldNotReachHere(type.toString());
        }
    }

    public void setNA(Object data, RandomAccessWriteIterator it, int index) {
        RType type = getType(data);
        switch (type) {
            case Integer:
                setInt(data, it, index, RRuntime.INT_NA);
                break;
            case Double:
                setDouble(data, it, index, RRuntime.DOUBLE_NA);
                break;
            case Logical:
                setLogical(data, it, index, RRuntime.LOGICAL_NA);
                break;
            case Raw:
                throw RInternalError.shouldNotReachHere("raw vectors do not have NA value");
            case Complex:
                setComplex(data, it, index, RComplex.createNA());
                break;
            case Character:
                setString(data, it, index, RRuntime.STRING_NA);
                break;
            case List:
            case PairList:
            case Language:
            case Expression:
                // To be compatible with the VectorAccess API, NAs are treated as NULLs...
                setElement(data, it, index, RNull.instance);
                break;
            default:
                CompilerDirectives.transferToInterpreter();
                throw RInternalError.unimplemented(type.toString());
        }
    }

    public boolean isNextNA(Object data, SeqIterator it, NACheck naCheck) {
        RType type = getType(data);
        switch (type) {
            case Integer:
                return naCheck.check(getNextInt(data, it));
            case Double:
                return naCheck.check(getNextDouble(data, it));
            case Logical:
                return naCheck.check(getNextLogical(data, it));
            case Raw:
                return false;
            case Complex:
                return naCheck.check(getNextComplex(data, it));
            case Character:
                return naCheck.check(getNextString(data, it));
            case List:
            case PairList:
            case Language:
            case Expression:
                // To be compatible with the VectorAccess API, checkListElement checks for NULLs not
                // NAs...
                return naCheck.checkListElement(getNextElement(data, it));
            default:
                CompilerDirectives.transferToInterpreter();
                throw RInternalError.unimplemented(type.toString());
        }
    }

    public boolean isNAAt(Object data, int index, NACheck naCheck) {
        RType type = getType(data);
        switch (type) {
            case Integer:
                return naCheck.check(getIntAt(data, index));
            case Double:
                return naCheck.check(getDoubleAt(data, index));
            case Logical:
                return naCheck.check(getLogicalAt(data, index));
            case Raw:
                return false;
            case Complex:
                return naCheck.check(getComplexAt(data, index));
            case Character:
                return naCheck.check(getStringAt(data, index));
            case List:
            case PairList:
            case Language:
            case Expression:
                // To be compatible with the VectorAccess API, checkListElement checks for NULLs not
                // NAs...
                return naCheck.checkListElement(getElementAt(data, index));
            default:
                CompilerDirectives.transferToInterpreter();
                throw RInternalError.unimplemented(type.toString());
        }
    }

    // Assertions:

    static class Asserts extends VectorDataLibrary {
        @Child private VectorDataLibrary delegate;

        Asserts(VectorDataLibrary delegate) {
            this.delegate = delegate;
        }

        private static VectorDataLibrary getUncachedLib() {
            return VectorDataLibrary.getFactory().getUncached();
        }

        @Override
        public boolean accepts(Object receiver) {
            // check whether someone is passing a vector that is already converted to vector-data
            // pattern
            // this could be when you do @CachedLibrary("vec") instead of
            // @CachedLibrary("vec.getData()")
            assert !(receiver instanceof RIntVector);
            assert !(receiver instanceof RDoubleVector);
            return delegate.accepts(receiver);
        }

        @Override
        public int getLength(Object data) {
            verifyIfSlowAssertsEnabled(data);
            int result = delegate.getLength(data);
            assert result >= 0;
            return result;
        }

        @Override
        public RType getType(Object data) {
            verifyIfSlowAssertsEnabled(data);
            RType result = delegate.getType(data);
            assert result != null;
            return result;
        }

        @Override
        public Object materialize(Object data) {
            verifyIfSlowAssertsEnabled(data);
            Object result = delegate.materialize(data);
            assert result != null;
            assert getUncachedLib().isWriteable(result);
            // data is not complete => result is not complete
            assert delegate.isComplete(data) || !getUncachedLib().isComplete(result);
            return result;
        }

        @Override
        public Object copy(Object data, boolean deep) {
            verifyIfSlowAssertsEnabled(data);
            return delegate.copy(data, deep);
        }

        @Override
        public Object copyResized(Object data, int newSize, boolean deep, boolean fillNA) {
            verifyIfSlowAssertsEnabled(data);
            assert newSize >= 0;
            Object result = delegate.copyResized(data, newSize, deep, fillNA);
            assert getUncachedLib().getLength(result) == newSize;
            // data is not complete => result is not complete
            assert delegate.isComplete(data) || !getUncachedLib().isComplete(result);
            // we filled the tail with NAs => result is not complete + there should be NAs at the
            // end
            if (fillNA && newSize > delegate.getLength(data)) {
                assert !getUncachedLib().isComplete(result);
                assert getUncachedLib().isNAAt(result, getUncachedLib().getLength(result) - 1, NACheck.getEnabled());
            }
            return result;
        }

        @Override
        public SeqIterator iterator(Object receiver) {
            verifyIfSlowAssertsEnabled(receiver);
            SeqIterator it = delegate.iterator(receiver);
            assert it != null;
            assert it.getLength() == delegate.getLength(receiver);
            return it;
        }

        @Override
        public RandomAccessIterator randomAccessIterator(Object receiver) {
            verifyIfSlowAssertsEnabled(receiver);
            RandomAccessIterator it = delegate.randomAccessIterator(receiver);
            assert it != null;
            return it;
        }

        @Override
        public void setDataAtAsObject(Object data, int index, Object value, InputNACheck naCheck) {
            assert index >= 0 && index < delegate.getLength(data);
            delegate.setDataAtAsObject(data, index, value, naCheck);
            assert delegate.getDataAtAsObject(data, index) == value;
            // NA written -> complete must be false
            assert !delegate.isNAAt(data, index, NACheck.getEnabled()) || !delegate.isComplete(data);
        }

        @Override
        public void setIntAt(Object receiver, int index, int value, InputNACheck naCheck) {
            assert index >= 0 && index < delegate.getLength(receiver);
            naCheck.assertInputValue(value);
            delegate.setIntAt(receiver, index, value, naCheck);
            assert delegate.getIntAt(receiver, index) == value;
            // NA written -> complete must be false
            assert !RRuntime.isNA(delegate.getIntAt(receiver, index)) || !delegate.isComplete(receiver);
        }

        @Override
        public void setDoubleAt(Object receiver, int index, double value, InputNACheck naCheck) {
            assert index >= 0 && index < delegate.getLength(receiver);
            naCheck.assertInputValue(value);
            delegate.setDoubleAt(receiver, index, value, naCheck);
            assert delegate.getDoubleAt(receiver, index) == value;
            // NA written -> complete must be false
            assert !RRuntime.isNA(delegate.getDoubleAt(receiver, index)) || !delegate.isComplete(receiver);
        }

        @Override
        public void setLogicalAt(Object receiver, int index, byte value, InputNACheck naCheck) {
            assert index >= 0 && index < delegate.getLength(receiver);
            naCheck.assertInputValue(value);
            delegate.setLogicalAt(receiver, index, value, naCheck);
            assert delegate.getLogicalAt(receiver, index) == value;
            // NA written -> complete must be false
            assert !RRuntime.isNA(delegate.getLogicalAt(receiver, index)) || !delegate.isComplete(receiver);
        }

        @Override
        public void commitWriteIterator(Object receiver, SeqWriteIterator iterator) {
            delegate.commitWriteIterator(receiver, iterator);
            assert verify(receiver);
        }

        @Override
        public void commitRandomAccessWriteIterator(Object receiver, RandomAccessWriteIterator iterator) {
            delegate.commitRandomAccessWriteIterator(receiver, iterator);
            assert verify(receiver);
        }

        private void verifyIfSlowAssertsEnabled(Object data) {
            if (ENABLE_VERY_SLOW_ASSERTS) {
                assert verify(data);
            }
        }

        private boolean verify(Object data) {
            VectorDataLibrary lib = delegate;
            boolean isComplete = lib.isComplete(data);
            int len = lib.getLength(data);
            assert len >= 0;
            switch (lib.getType(data)) {
                case Integer:
                    for (int i = 0; i < len; i++) {
                        int intVal = lib.getIntAt(data, i);
                        assert !isComplete || !RRuntime.isNA(intVal);
                        assert lib.getDataAtAsObject(data, i).equals(intVal);
                    }
                    break;
                case Double:
                    for (int i = 0; i < len; i++) {
                        double doubleVal = lib.getDoubleAt(data, i);
                        assert !isComplete || !RRuntime.isNA(doubleVal);
                        assert lib.getDataAtAsObject(data, i).equals(doubleVal);
                    }
                    break;
                case Logical:
                    for (int i = 0; i < len; i++) {
                        byte logicalVal = lib.getLogicalAt(data, i);
                        assert !isComplete || !RRuntime.isNA(logicalVal);
                        assert lib.getDataAtAsObject(data, i).equals(logicalVal);
                    }
                    break;
                case Raw:
                    for (int i = 0; i < len; i++) {
                        byte rawVal = lib.getRawAt(data, i);
                        assert lib.getDataAtAsObject(data, i).equals(rawVal);
                    }
                    break;
                case Character:
                    for (int i = 0; i < len; i++) {
                        String stringVal = lib.getStringAt(data, i);
                        assert !isComplete || !RRuntime.isNA(stringVal);
                        assert lib.getDataAtAsObject(data, i).equals(stringVal);
                    }
                    break;
                case Complex:
                    for (int i = 0; i < len; i++) {
                        RComplex complexVal = lib.getComplexAt(data, i);
                        assert !isComplete || !RRuntime.isNA(complexVal);
                        assert lib.getDataAtAsObject(data, i).equals(complexVal);
                    }
                    break;
                default:
                    // no vector can contain any Java null value
                    for (int i = 0; i < len; i++) {
                        assert lib.getDataAtAsObject(data, i) != null;
                    }
                    break;
            }
            return true;
        }

        // TODO: there methods simply delegate, but may be enhanced with assertions

        @Override
        public boolean next(Object receiver, SeqIterator it, boolean withWrap) {
            return delegate.next(receiver, it, withWrap);
        }

        @Override
        public boolean isComplete(Object data) {
            verifyIfSlowAssertsEnabled(data);
            return delegate.isComplete(data);
        }

        @Override
        public boolean isSorted(Object receiver, boolean descending, boolean naLast) {
            verifyIfSlowAssertsEnabled(receiver);
            return delegate.isSorted(receiver, descending, naLast);
        }

        @Override
        public boolean isWriteable(Object data) {
            verifyIfSlowAssertsEnabled(data);
            return delegate.isWriteable(data);
        }

        @Override
        public SeqWriteIterator writeIterator(Object receiver, boolean inputIsComplete) {
            verifyIfSlowAssertsEnabled(receiver);
            return delegate.writeIterator(receiver, inputIsComplete);
        }

        @Override
        public RandomAccessWriteIterator randomAccessWriteIterator(Object receiver, boolean inputIsComplete) {
            verifyIfSlowAssertsEnabled(receiver);
            return delegate.randomAccessWriteIterator(receiver, inputIsComplete);
        }

        @Override
        public int[] getReadonlyIntData(Object receiver) {
            verifyIfSlowAssertsEnabled(receiver);
            return delegate.getReadonlyIntData(receiver);
        }

        @Override
        public int[] getIntDataCopy(Object receiver) {
            verifyIfSlowAssertsEnabled(receiver);
            return delegate.getIntDataCopy(receiver);
        }

        @Override
        public Object getDataAtAsObject(Object data, int index) {
            return delegate.getDataAtAsObject(data, index);
        }

        @Override
        public void setNA(Object data, RandomAccessWriteIterator it, int index) {
            delegate.setNA(data, it, index);
        }

        @Override
        public boolean isNextNA(Object data, SeqIterator it, NACheck naCheck) {
            return delegate.isNextNA(data, it, naCheck);
        }

        @Override
        public boolean isNAAt(Object data, int index, NACheck naCheck) {
            return delegate.isNAAt(data, index, naCheck);
        }

        @Override
        public int getIntAt(Object receiver, int index) {
            return delegate.getIntAt(receiver, index);
        }

        @Override
        public int getNextInt(Object receiver, SeqIterator it) {
            return delegate.getNextInt(receiver, it);
        }

        @Override
        public int getInt(Object receiver, RandomAccessIterator it, int index) {
            return delegate.getInt(receiver, it, index);
        }

        @Override
        public void setNextInt(Object receiver, SeqWriteIterator it, int value) {
            delegate.setNextInt(receiver, it, value);
        }

        @Override
        public void setInt(Object receiver, RandomAccessWriteIterator it, int index, int value) {
            delegate.setInt(receiver, it, index, value);
        }

        @Override
        public double[] getReadonlyDoubleData(Object receiver) {
            verifyIfSlowAssertsEnabled(receiver);
            return delegate.getReadonlyDoubleData(receiver);
        }

        @Override
        public double[] getDoubleDataCopy(Object receiver) {
            verifyIfSlowAssertsEnabled(receiver);
            return delegate.getDoubleDataCopy(receiver);
        }

        @Override
        public double getDoubleAt(Object receiver, int index) {
            return delegate.getDoubleAt(receiver, index);
        }

        @Override
        public double getNextDouble(Object receiver, SeqIterator it) {
            return delegate.getNextDouble(receiver, it);
        }

        @Override
        public double getDouble(Object receiver, RandomAccessIterator it, int index) {
            return delegate.getDouble(receiver, it, index);
        }

        @Override
        public void setNextDouble(Object receiver, SeqWriteIterator it, double value) {
            delegate.setNextDouble(receiver, it, value);
        }

        @Override
        public void setDouble(Object receiver, RandomAccessWriteIterator it, int index, double value) {
            delegate.setDouble(receiver, it, index, value);
        }

        @Override
        public byte[] getReadonlyLogicalData(Object receiver) {
            verifyIfSlowAssertsEnabled(receiver);
            return delegate.getReadonlyLogicalData(receiver);
        }

        @Override
        public byte[] getLogicalDataCopy(Object receiver) {
            verifyIfSlowAssertsEnabled(receiver);
            return delegate.getLogicalDataCopy(receiver);
        }

        @Override
        public byte getLogicalAt(Object receiver, int index) {
            return delegate.getLogicalAt(receiver, index);
        }

        @Override
        public byte getNextLogical(Object receiver, SeqIterator it) {
            return delegate.getNextLogical(receiver, it);
        }

        @Override
        public byte getLogical(Object receiver, RandomAccessIterator it, int index) {
            return delegate.getLogical(receiver, it, index);
        }

        @Override
        public void setNextLogical(Object receiver, SeqWriteIterator it, byte value) {
            delegate.setNextLogical(receiver, it, value);
        }

        @Override
        public void setLogical(Object receiver, RandomAccessWriteIterator it, int index, byte value) {
            delegate.setLogical(receiver, it, index, value);
        }

        @Override
        public byte[] getReadonlyRawData(Object receiver) {
            return delegate.getReadonlyRawData(receiver);
        }

        @Override
        public byte[] getRawDataCopy(Object receiver) {
            return delegate.getRawDataCopy(receiver);
        }

        @Override
        public byte getRawAt(Object receiver, int index) {
            return delegate.getRawAt(receiver, index);
        }

        @Override
        public byte getNextRaw(Object receiver, SeqIterator it) {
            return delegate.getNextRaw(receiver, it);
        }

        @Override
        public byte getRaw(Object receiver, RandomAccessIterator it, int index) {
            return delegate.getRaw(receiver, it, index);
        }

        @Override
        public void setRawAt(Object receiver, int index, byte value, InputNACheck naCheck) {
            delegate.setRawAt(receiver, index, value, naCheck);
        }

        @Override
        public void setNextRaw(Object receiver, SeqWriteIterator it, byte value) {
            delegate.setNextRaw(receiver, it, value);
        }

        @Override
        public void setRaw(Object receiver, RandomAccessWriteIterator it, int index, byte value) {
            delegate.setRaw(receiver, it, index, value);
        }

        @Override
        public String[] getReadonlyStringData(Object receiver) {
            verifyIfSlowAssertsEnabled(receiver);
            return delegate.getReadonlyStringData(receiver);
        }

        @Override
        public String[] getStringDataCopy(Object receiver) {
            verifyIfSlowAssertsEnabled(receiver);
            return delegate.getStringDataCopy(receiver);
        }

        @Override
        public String getStringAt(Object receiver, int index) {
            return delegate.getStringAt(receiver, index);
        }

        @Override
        public String getNextString(Object receiver, SeqIterator it) {
            return delegate.getNextString(receiver, it);
        }

        @Override
        public String getString(Object receiver, RandomAccessIterator it, int index) {
            return delegate.getString(receiver, it, index);
        }

        @Override
        public void setStringAt(Object receiver, int index, String value, InputNACheck naCheck) {
            delegate.setStringAt(receiver, index, value, naCheck);
        }

        @Override
        public void setNextString(Object receiver, SeqWriteIterator it, String value) {
            delegate.setNextString(receiver, it, value);
        }

        @Override
        public void setString(Object receiver, RandomAccessWriteIterator it, int index, String value) {
            delegate.setString(receiver, it, index, value);
        }

        @Override
        public double[] getReadonlyComplexData(Object receiver) {
            verifyIfSlowAssertsEnabled(receiver);
            return delegate.getReadonlyComplexData(receiver);
        }

        @Override
        public double[] getComplexDataCopy(Object receiver) {
            verifyIfSlowAssertsEnabled(receiver);
            return delegate.getComplexDataCopy(receiver);
        }

        @Override
        public RComplex getComplexAt(Object receiver, int index) {
            return delegate.getComplexAt(receiver, index);
        }

        @Override
        public RComplex getNextComplex(Object receiver, SeqIterator it) {
            return delegate.getNextComplex(receiver, it);
        }

        @Override
        public RComplex getComplex(Object receiver, RandomAccessIterator it, int index) {
            return delegate.getComplex(receiver, it, index);
        }

        @Override
        public void setComplexAt(Object receiver, int index, RComplex value, InputNACheck naCheck) {
            delegate.setComplexAt(receiver, index, value, naCheck);
        }

        @Override
        public void setNextComplex(Object receiver, SeqWriteIterator it, RComplex value) {
            delegate.setNextComplex(receiver, it, value);
        }

        @Override
        public void setComplex(Object receiver, RandomAccessWriteIterator it, int index, RComplex value) {
            delegate.setComplex(receiver, it, index, value);
        }

        @Override
        public Object[] getReadonlyListData(Object receiver) {
            verifyIfSlowAssertsEnabled(receiver);
            return delegate.getReadonlyListData(receiver);
        }

        @Override
        public Object[] getListDataCopy(Object receiver) {
            verifyIfSlowAssertsEnabled(receiver);
            return delegate.getListDataCopy(receiver);
        }

        @Override
        public Object getElementAt(Object receiver, int index) {
            return delegate.getElementAt(receiver, index);
        }

        @Override
        public Object getNextElement(Object receiver, SeqIterator it) {
            return delegate.getNextElement(receiver, it);
        }

        @Override
        public Object getElement(Object receiver, RandomAccessIterator it, int index) {
            return delegate.getElement(receiver, it, index);
        }

        @Override
        public void setElementAt(Object receiver, int index, Object value) {
            delegate.setElementAt(receiver, index, value);
        }

        @Override
        public void setNextElement(Object receiver, SeqWriteIterator it, Object value) {
            delegate.setNextElement(receiver, it, value);
        }

        @Override
        public void setElement(Object receiver, RandomAccessWriteIterator it, int index, Object value) {
            delegate.setElement(receiver, it, index, value);
        }
    }

    // Private utility methods

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
