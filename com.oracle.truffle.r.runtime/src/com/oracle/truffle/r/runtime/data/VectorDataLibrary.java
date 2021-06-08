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

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.GenerateLibrary;
import com.oracle.truffle.api.library.GenerateLibrary.DefaultExport;
import com.oracle.truffle.api.library.Library;
import com.oracle.truffle.api.library.LibraryFactory;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.LoopConditionProfile;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.Asserts;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

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
 * <li>At random positions without an iterator object ( {@link #setIntAt(Object, int, int)}). This
 * is the least efficient way and should be used only outside of loops.</li>
 * </ul>
 * <p>
 * Write operations also provide overloads with and without iterators. The overload without iterator
 * ({@link #setIntAt(Object, int, int)}), always checks if the value is NA and updates the complete
 * flag immediately if necessary. This is more efficient if you are writing single value. With the
 * iterators usage, the memory write of the "complete" field is moved to
 * {@link #commitWriteIterator(Object, SeqWriteIterator, boolean)}, so that the memory write is
 * outside of the loop that usually writes values to the vector object.
 */
@GenerateLibrary(assertions = Asserts.class)
@DefaultExport(RListArrayDataLibrary.class)
public abstract class VectorDataLibrary extends Library {

    public static final boolean ENABLE_VERY_SLOW_ASSERTS = "true".equals(System.getenv().get("FASTR_TEST_VERY_SLOW_ASSERTS"));

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
     * Returns an instance of object that implements {@link VectorDataLibrary} and is guaranteed to
     * return {@code true} from {@link #isWriteable(Object)}. The result may be the same as
     * {@code receiver} or a new fresh instance.
     */
    public abstract Object materialize(Object data);

    /**
     * Similarly to {@link #materialize(Object)}, returns an instance of object that implements
     * {@link VectorDataLibrary}. Moreover, the receiver should transform into an object containing
     * {@link CharSXPWrapper} data, if possible.
     * 
     * @see #materialize(Object)
     */
    public Object materializeCharSXPStorage(Object data) {
        return null;
    }

    /**
     * Transforms the data to representation that is backed by native memory.
     */
    public long asPointer(@SuppressWarnings("unused") Object data) throws UnsupportedMessageException {
        throw UnsupportedMessageException.create();
    }

    public abstract Object copy(Object data, boolean deep);

    /**
     * Iterator objects should be opaque to the users of {@link VectorDataLibrary} any state except
     * for {@link SeqIterator#getLength()} is intentionally package private.
     */
    public abstract static class Iterator {
        private final Object store;

        private final WarningInfo warningInfo = new WarningInfo();

        protected Iterator(Object store) {
            this.store = store;
        }

        public final Object getStore() {
            return store;
        }

        public final WarningInfo getWarningInfo() {
            return warningInfo;
        }
    }

    public static class SeqIterator extends Iterator {
        private final int length;
        private int index;

        public SeqIterator(Object store, int length) {
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

        public final void initLoopConditionProfile(LoopConditionProfile profile) {
            profile.profileCounted(length);
        }

        public final boolean next(boolean loopCondition, LoopConditionProfile loopConditionProfile) {
            CompilerAsserts.partialEvaluationConstant(loopCondition);
            if (loopCondition) {
                return loopConditionProfile.inject(++index < length);
            } else {
                return ++index < length;
            }
        }

        public final void nextWithWrap(ConditionProfile wrapConditionProfile) {
            index++;
            if (wrapConditionProfile.profile(index == length)) {
                index = 0;
            }
        }

        public final void reset() {
            index = -1;
        }
    }

    public static class RandomAccessIterator extends Iterator {
        public RandomAccessIterator(Object store) {
            super(store);
        }
    }

    public static final class SeqWriteIterator extends SeqIterator {
        public SeqWriteIterator(Object store, int length) {
            super(store, length);
        }
    }

    public static final class RandomAccessWriteIterator extends RandomAccessIterator {
        public RandomAccessWriteIterator(Object store) {
            super(store);
        }
    }

    /**
     * Returns an iterator object. Using an iterator object allows to better optimize the operation
     * of getting data at given index. See the documentation of {@link VectorDataLibrary} for high
     * level overview.
     *
     * The implementation should use {@link LoopConditionProfile} shared with
     * {@link #nextLoopCondition(Object, SeqIterator)} message and it should initialize it using
     * {@link SeqIterator#initLoopConditionProfile(LoopConditionProfile)}.
     */
    public abstract SeqIterator iterator(Object receiver);

    /**
     * Single point for implementation of {@link #next(Object, SeqIterator)} and
     * {@link #nextLoopCondition(Object, SeqIterator)}. The users of this library should not use
     * this method directly!
     */
    public abstract boolean nextImpl(Object receiver, SeqIterator it, boolean loopCondition);

    /**
     * Advances the iterator to the next element. There are several overloads of this method:
     * <ul>
     * <li>{@link #nextLoopCondition(Object, SeqIterator)} should be used whenever the resulting
     * {@code boolean} is used in a loop condition. This must not be used anywhere else!</li>
     * <li>{@link #next(Object, SeqIterator)} variant of the previous that is safe to use in other
     * contexts than loop conditions.</li>
     * <li>{@link #nextWithWrap(Object, SeqIterator)} variant which wraps the index back to 0 when
     * reaching the end of the vector.</li>
     * <li>{@link #nextImpl(Object, SeqIterator, boolean)} simplifies the implementation, but should
     * not be used directly.</li>
     * </ul>
     */
    public final boolean next(Object receiver, SeqIterator it) {
        return nextImpl(receiver, it, false);
    }

    /**
     * Prefer usage of {@link #nextLoopCondition(Object, SeqIterator)} over
     * {@link #next(Object, SeqIterator)} when used in a loop condition. This overload cannot be
     * used with dispatched libraries (only with cached and uncached libraries), nor in other
     * contexts than loop conditions, otherwise the compilation will fail!
     *
     * The implementation should use {@link LoopConditionProfile} shared with
     * {@link #iterator(Object)} message and should pass the profile to
     * {@link SeqIterator#next(boolean, LoopConditionProfile)} in
     * {@link #nextImpl(Object, SeqIterator, boolean)}.
     *
     * @see #next(Object, SeqIterator)
     */
    public final boolean nextLoopCondition(Object receiver, SeqIterator it) {
        return nextImpl(receiver, it, true);
    }

    /**
     * Advances the iterator to the next element with a wrap from last element to the first.
     *
     * @see #next(Object, SeqIterator)
     */
    public abstract void nextWithWrap(Object receiver, SeqIterator it);

    /**
     * @see #iterator(Object)
     */
    public abstract RandomAccessIterator randomAccessIterator(Object receiver);

    /**
     * Returns an iterator object. Using an iterator object allows to better optimize the operation
     * of writing data at given index. See the documentation of {@link VectorDataLibrary} for high
     * level overview. The {@code receiver} object must be {@link #isWriteable(Object)}.
     * <p>
     * Writes using a write iterator must be committed using
     * {@link #commitWriteIterator(Object, SeqWriteIterator, boolean)}.
     */
    public SeqWriteIterator writeIterator(Object receiver) {
        throw notWriteableError(receiver, "writeIterator");
    }

    /**
     * @see #writeIterator(Object)
     */
    public RandomAccessWriteIterator randomAccessWriteIterator(Object receiver) {
        throw notWriteableError(receiver, "randomAccessWriteIterator");
    }

    /**
     * Writes using a write iterator must be committed using this method. Moreover, the iterators
     * should be used with the try-finally pattern where the finally block should call
     * {@link #commitWriteIterator(Object, SeqWriteIterator, boolean)}.
     *
     * {@code neverSeenNA} set to {@code true} indicates that no {@code NA} value was written to the
     * vector. {@code neverSeenNA} set to {@code false} indicates that an {@code NA} value may or
     * may not have been written. Some implementations need this information in order to update
     * their {@link #isComplete(Object)} state. It is left to the user to track the {@code NA}
     * values, for example, using {@link NACheck}.
     *
     * One pattern is to pass the {@link NACheck#neverSeenNA()} of the {@link #getNACheck(Object)}
     * of the input vector (one we took that data from) as the {@code neverSeenNA} parameter. On top
     * of that, if we know that there was no conversion that could have introduced {@code NA} values
     * (e.g. , in
     * {@link #transfer(Object, RandomAccessWriteIterator, int, VectorDataLibrary, RandomAccessIterator, Object, int)}
     * ), we can use the following pattern:
     *
     * <pre>
     * inputDataLib.isComplete(inputData) || dataLib.getNACheck(inputData).neverSeenNA()
     * </pre>
     *
     * Notes for implementors:
     * 
     * Implementations that are not writeable, or that always return {@code false} from
     * {@link #isComplete(Object)} do not need to implement this message.
     */
    public void commitWriteIterator(@SuppressWarnings("unused") Object receiver, @SuppressWarnings("unused") SeqWriteIterator iterator, @SuppressWarnings("unused") boolean neverSeenNA) {
    }

    /**
     * @see #commitWriteIterator(Object, SeqWriteIterator, boolean)
     */
    public void commitRandomAccessWriteIterator(@SuppressWarnings("unused") Object receiver, @SuppressWarnings("unused") RandomAccessWriteIterator iterator,
                    @SuppressWarnings("unused") boolean neverSeenNA) {
    }

    /**
     * Gives an {@link NACheck} that is enabled depending on the {@link #isComplete(Object)} flag of
     * the data and also checks every value returned from any method for accessing single elements
     * of this data object. Applies only to atomic vectors, not to lists! I.e., not to
     * {@link #getElementAt(Object, int)}.
     *
     * In other words: {@link NACheck#neverSeenNA()} implies that no method such as
     * {@link #getIntAt(Object, int)} ever returned {@code NA} value. If
     * {@link NACheck#neverSeenNA()} is {@code false}, then we do not know anything about the data
     * and must assume that {@code NA} value could have been returned.
     */
    public abstract NACheck getNACheck(Object receiver);

    /**
     * Converts the given data to another data object, for which
     * {@code getType(newObj) == targetType}. The new data object can convert data on the fly or use
     * other strategy. {@code targetType} must be a compilation constant if used in partially
     * evaluated code.
     *
     * For every combination of the implementation of {@link VectorDataLibrary} and
     * {@code targetType}, the result must always be the same implementation of
     * {@link VectorDataLibrary}. I.e., it is allowed to create a specialized library for the result
     * if the type of the {@code receiver} and {@code targetType} are guaranteed to be the same.
     */
    public Object cast(Object receiver, RType targetType) {
        CompilerAsserts.partialEvaluationConstant(targetType);
        return VectorDataClosure.fromData(receiver, getType(receiver), targetType);
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
     * Gets a region of integers from the data. It is a Java equivalent for INTEGER_GET_REGION C
     * function.
     *
     * @param startIndex Starting index of the region.
     * @param size Size of the required region
     * @param buffer Buffer into which the data will be copied, in C equivalent it is pointer to
     *            integer.
     * @param bufferInterop InteropLibrary for the buffer through which the elements will be set
     *            into the buffer.
     * @return count of elements that were actually copied.
     */
    public int getIntRegion(Object receiver, int startIndex, int size, Object buffer, InteropLibrary bufferInterop) {
        return getRegion(receiver, startIndex, size, buffer, bufferInterop, RType.Integer);
    }

    private int getRegion(Object receiver, int startIndex, int size, Object buffer, InteropLibrary bufferInterop, RType bufferType) {
        RandomAccessIterator it = randomAccessIterator(receiver);
        int bufferIdx = 0;
        for (int idx = startIndex; idx < startIndex + size; idx++) {
            try {
                switch (bufferType) {
                    case Integer:
                        bufferInterop.writeArrayElement(buffer, bufferIdx, getInt(receiver, it, idx));
                        break;
                    case Double:
                        bufferInterop.writeArrayElement(buffer, bufferIdx, getDouble(receiver, it, idx));
                        break;
                    case Logical:
                        bufferInterop.writeArrayElement(buffer, bufferIdx, getLogical(receiver, it, idx));
                        break;
                    case Raw:
                        bufferInterop.writeArrayElement(buffer, bufferIdx, getRaw(receiver, it, idx));
                        break;
                    case Complex:
                        bufferInterop.writeArrayElement(buffer, bufferIdx, getComplex(receiver, it, idx));
                        break;
                    default:
                        CompilerDirectives.transferToInterpreter();
                        throw RInternalError.shouldNotReachHere(bufferType.toString());
                }
                bufferIdx++;
            } catch (InteropException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }
        return bufferIdx;
    }

    /**
     * Returns the value at given position. See the documentation of {@link #iterator(Object)} for
     * details.
     */
    public int getIntAt(Object receiver, @SuppressWarnings("unused") int index) {
        RType type = getType(receiver);
        switch (type) {
            case Integer:
                CompilerDirectives.transferToInterpreter();
                throw RInternalError.shouldNotReachHere("should be exported elsewhere " + receiver);
            case Double:
                return double2int(getNACheck(receiver), getDoubleAt(receiver, index));
            case Logical:
                return logical2int(getNACheck(receiver), getLogicalAt(receiver, index));
            case Raw:
                return raw2int(getRawAt(receiver, index));
            case Complex:
                return complex2int(getNACheck(receiver), getComplexAt(receiver, index));
            case Character:
                return string2int(getNACheck(receiver), getStringAt(receiver, index));
            default:
                CompilerDirectives.transferToInterpreter();
                throw RInternalError.shouldNotReachHere(type.toString());
        }
    }

    /**
     * Returns the value at position given by the iterator which should be constructed by calling
     * {@link #iterator(Object)} on the same {@code receiver} object. See the documentation of
     * {@link VectorDataLibrary} for a high level overview.
     */
    public int getNextInt(Object receiver, @SuppressWarnings("unused") SeqIterator it) {
        RType type = getType(receiver);
        switch (type) {
            case Integer:
                CompilerDirectives.transferToInterpreter();
                throw RInternalError.shouldNotReachHere("should be exported elsewhere " + receiver);
            case Double:
                return double2int(getNACheck(receiver), getNextDouble(receiver, it));
            case Logical:
                return logical2int(getNACheck(receiver), getNextLogical(receiver, it));
            case Raw:
                return raw2int(getNextRaw(receiver, it));
            case Complex:
                return complex2int(getNACheck(receiver), getNextComplex(receiver, it));
            case Character:
                return string2int(getNACheck(receiver), getNextString(receiver, it));
            default:
                CompilerDirectives.transferToInterpreter();
                throw RInternalError.shouldNotReachHere(type.toString());
        }
    }

    /**
     * Returns the value at given position. The iterator which should be constructed by calling
     * {@link #randomAccessIterator(Object)} on the same {@code receiver} object. See the
     * documentation of {@link VectorDataLibrary} for a high level overview.
     */
    public int getInt(Object receiver, @SuppressWarnings("unused") RandomAccessIterator it, @SuppressWarnings("unused") int index) {
        RType type = getType(receiver);
        switch (type) {
            case Integer:
                CompilerDirectives.transferToInterpreter();
                throw RInternalError.shouldNotReachHere("should be exported elsewhere " + receiver);
            case Double:
                return double2int(getNACheck(receiver), getDouble(receiver, it, index));
            case Logical:
                return logical2int(getNACheck(receiver), getLogical(receiver, it, index));
            case Raw:
                return raw2int(getRaw(receiver, it, index));
            case Complex:
                return complex2int(getNACheck(receiver), getComplex(receiver, it, index));
            case Character:
                return string2int(getNACheck(receiver), getString(receiver, it, index));
            default:
                CompilerDirectives.transferToInterpreter();
                throw RInternalError.shouldNotReachHere(type.toString());
        }
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
    public void setIntAt(Object receiver, int index, int value) {
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

    // ---------------------------------------------------------------------
    // Methods specific to double data

    public double[] getReadonlyDoubleData(Object receiver) {
        return getDoubleDataCopy(receiver);
    }

    public double[] getDoubleDataCopy(Object receiver) {
        throw notImplemented(receiver);
    }

    /**
     * See {@link #getIntRegion}.
     */
    public int getDoubleRegion(Object receiver, int startIndex, int size, Object buffer, InteropLibrary bufferInterop) {
        return getRegion(receiver, startIndex, size, buffer, bufferInterop, RType.Double);
    }

    public double getDoubleAt(Object receiver, @SuppressWarnings("unused") int index) {
        RType type = getType(receiver);
        switch (type) {
            case Integer:
                return int2double(getNACheck(receiver), getIntAt(receiver, index));
            case Double:
                CompilerDirectives.transferToInterpreter();
                throw RInternalError.shouldNotReachHere("should be exported elsewhere " + receiver);
            case Logical:
                return logical2double(getNACheck(receiver), getLogicalAt(receiver, index));
            case Raw:
                return raw2double(getRawAt(receiver, index));
            case Complex:
                return complex2double(getNACheck(receiver), getComplexAt(receiver, index));
            case Character:
                return string2double(getNACheck(receiver), getStringAt(receiver, index));
            default:
                CompilerDirectives.transferToInterpreter();
                throw RInternalError.shouldNotReachHere(type.toString());
        }
    }

    public double getNextDouble(Object receiver, @SuppressWarnings("unused") SeqIterator it) {
        RType type = getType(receiver);
        switch (type) {
            case Integer:
                return int2double(getNACheck(receiver), getNextInt(receiver, it));
            case Double:
                CompilerDirectives.transferToInterpreter();
                throw RInternalError.shouldNotReachHere("should be exported elsewhere " + receiver);
            case Logical:
                return logical2double(getNACheck(receiver), getNextLogical(receiver, it));
            case Raw:
                return raw2double(getNextRaw(receiver, it));
            case Complex:
                return complex2double(getNACheck(receiver), getNextComplex(receiver, it));
            case Character:
                return string2double(getNACheck(receiver), getNextString(receiver, it));
            default:
                CompilerDirectives.transferToInterpreter();
                throw RInternalError.shouldNotReachHere(type.toString());
        }
    }

    public double getDouble(Object receiver, @SuppressWarnings("unused") RandomAccessIterator it, @SuppressWarnings("unused") int index) {
        RType type = getType(receiver);
        switch (type) {
            case Integer:
                return int2double(getNACheck(receiver), getInt(receiver, it, index));
            case Double:
                CompilerDirectives.transferToInterpreter();
                throw RInternalError.shouldNotReachHere("should be exported elsewhere " + receiver);
            case Logical:
                return logical2double(getNACheck(receiver), getLogical(receiver, it, index));
            case Raw:
                return raw2double(getRaw(receiver, it, index));
            case Complex:
                return complex2double(getNACheck(receiver), getComplex(receiver, it, index));
            case Character:
                return string2double(getNACheck(receiver), getString(receiver, it, index));
            default:
                CompilerDirectives.transferToInterpreter();
                throw RInternalError.shouldNotReachHere(type.toString());
        }
    }

    @SuppressWarnings("unused")
    public void setDoubleAt(Object receiver, int index, double value) {
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

    // ---------------------------------------------------------------------
    // Methods specific to logical data

    public byte[] getReadonlyLogicalData(Object receiver) {
        return getLogicalDataCopy(receiver);
    }

    public byte[] getLogicalDataCopy(Object receiver) {
        throw notImplemented(receiver);
    }

    /**
     * See {@link #getIntRegion}.
     */
    public int getLogicalRegion(Object receiver, int startIndex, int size, Object buffer, InteropLibrary bufferInterop) {
        return getRegion(receiver, startIndex, size, buffer, bufferInterop, RType.Logical);
    }

    public byte getLogicalAt(Object receiver, @SuppressWarnings("unused") int index) {
        RType type = getType(receiver);
        switch (type) {
            case Integer:
                return int2logical(getNACheck(receiver), getIntAt(receiver, index));
            case Double:
                return double2logical(getNACheck(receiver), getDoubleAt(receiver, index));
            case Logical:
                CompilerDirectives.transferToInterpreter();
                throw RInternalError.shouldNotReachHere("should be exported elsewhere " + receiver);
            case Raw:
                return raw2logical(getRawAt(receiver, index));
            case Complex:
                return complex2logical(getNACheck(receiver), getComplexAt(receiver, index));
            case Character:
                return string2logical(getNACheck(receiver), getStringAt(receiver, index));
            default:
                CompilerDirectives.transferToInterpreter();
                throw RInternalError.shouldNotReachHere(type.toString());
        }
    }

    public byte getNextLogical(Object receiver, @SuppressWarnings("unused") SeqIterator it) {
        RType type = getType(receiver);
        switch (type) {
            case Integer:
                return int2logical(getNACheck(receiver), getNextInt(receiver, it));
            case Double:
                return double2logical(getNACheck(receiver), getNextDouble(receiver, it));
            case Logical:
                CompilerDirectives.transferToInterpreter();
                throw RInternalError.shouldNotReachHere("should be exported elsewhere " + receiver);
            case Raw:
                return raw2logical(getNextRaw(receiver, it));
            case Complex:
                return complex2logical(getNACheck(receiver), getNextComplex(receiver, it));
            case Character:
                return string2logical(getNACheck(receiver), getNextString(receiver, it));
            default:
                CompilerDirectives.transferToInterpreter();
                throw RInternalError.shouldNotReachHere(type.toString());
        }
    }

    public byte getLogical(Object receiver, @SuppressWarnings("unused") RandomAccessIterator it, @SuppressWarnings("unused") int index) {
        RType type = getType(receiver);
        switch (type) {
            case Integer:
                return int2logical(getNACheck(receiver), getInt(receiver, it, index));
            case Double:
                return double2logical(getNACheck(receiver), getDouble(receiver, it, index));
            case Logical:
                CompilerDirectives.transferToInterpreter();
                throw RInternalError.shouldNotReachHere("should be exported elsewhere " + receiver);
            case Raw:
                return raw2logical(getRaw(receiver, it, index));
            case Complex:
                return complex2logical(getNACheck(receiver), getComplex(receiver, it, index));
            case Character:
                return string2logical(getNACheck(receiver), getString(receiver, it, index));
            default:
                CompilerDirectives.transferToInterpreter();
                throw RInternalError.shouldNotReachHere(type.toString());
        }
    }

    @SuppressWarnings("unused")
    public void setLogicalAt(Object receiver, int index, byte value) {
        throw notWriteableError(receiver, "setLogicalAt");
    }

    @SuppressWarnings("unused")
    public void setNextLogical(Object receiver, SeqWriteIterator it, byte value) {
        throw notWriteableError(receiver, "setNextLogical");
    }

    @SuppressWarnings("unused")
    public void setLogical(Object receiver, RandomAccessWriteIterator it, int index, byte value) {
        throw notWriteableError(receiver, "setLogical");
    }

    // ---------------------------------------------------------------------
    // Methods specific to raw data

    public byte[] getReadonlyRawData(Object receiver) {
        return getLogicalDataCopy(receiver);
    }

    public byte[] getRawDataCopy(Object receiver) {
        throw notImplemented(receiver);
    }

    /**
     * See {@link #getIntRegion}.
     */
    public int getRawRegion(Object receiver, int startIndex, int size, Object buffer, InteropLibrary bufferInterop) {
        return getRegion(receiver, startIndex, size, buffer, bufferInterop, RType.Raw);
    }

    public byte getRawAt(Object receiver, @SuppressWarnings("unused") int index) {
        RType type = getType(receiver);
        switch (type) {
            case Integer:
                return int2raw(getNACheck(receiver), getIntAt(receiver, index));
            case Double:
                return double2raw(getNACheck(receiver), getDoubleAt(receiver, index));
            case Logical:
                return logical2raw(getNACheck(receiver), getLogicalAt(receiver, index));
            case Raw:
                CompilerDirectives.transferToInterpreter();
                throw RInternalError.shouldNotReachHere("should be exported elsewhere " + receiver);
            case Complex:
                return complex2raw(getNACheck(receiver), getComplexAt(receiver, index));
            case Character:
                return string2raw(getNACheck(receiver), getStringAt(receiver, index));
            default:
                CompilerDirectives.transferToInterpreter();
                throw RInternalError.shouldNotReachHere(type.toString());
        }
    }

    public byte getNextRaw(Object receiver, @SuppressWarnings("unused") SeqIterator it) {
        RType type = getType(receiver);
        switch (type) {
            case Integer:
                return int2raw(getNACheck(receiver), getNextInt(receiver, it));
            case Double:
                return double2raw(getNACheck(receiver), getNextDouble(receiver, it));
            case Logical:
                return logical2raw(getNACheck(receiver), getNextLogical(receiver, it));
            case Raw:
                CompilerDirectives.transferToInterpreter();
                throw RInternalError.shouldNotReachHere("should be exported elsewhere " + receiver);
            case Complex:
                return complex2raw(getNACheck(receiver), getNextComplex(receiver, it));
            case Character:
                return string2raw(getNACheck(receiver), getNextString(receiver, it));
            default:
                CompilerDirectives.transferToInterpreter();
                throw RInternalError.shouldNotReachHere(type.toString());
        }
    }

    public byte getRaw(Object receiver, @SuppressWarnings("unused") RandomAccessIterator it, @SuppressWarnings("unused") int index) {
        RType type = getType(receiver);
        switch (type) {
            case Integer:
                return int2raw(getNACheck(receiver), getInt(receiver, it, index));
            case Double:
                return double2raw(getNACheck(receiver), getDouble(receiver, it, index));
            case Logical:
                return logical2raw(getNACheck(receiver), getLogical(receiver, it, index));
            case Raw:
                CompilerDirectives.transferToInterpreter();
                throw RInternalError.shouldNotReachHere("should be exported elsewhere " + receiver);
            case Complex:
                return complex2raw(getNACheck(receiver), getComplex(receiver, it, index));
            case Character:
                return string2raw(getNACheck(receiver), getString(receiver, it, index));
            default:
                CompilerDirectives.transferToInterpreter();
                throw RInternalError.shouldNotReachHere(type.toString());
        }
    }

    @SuppressWarnings("unused")
    public void setRawAt(Object receiver, int index, byte value) {
        throw notWriteableError(receiver, "setRawAt");
    }

    @SuppressWarnings("unused")
    public void setNextRaw(Object receiver, SeqWriteIterator it, byte value) {
        throw notWriteableError(receiver, "setNextRaw");
    }

    @SuppressWarnings("unused")
    public void setRaw(Object receiver, RandomAccessWriteIterator it, int index, byte value) {
        throw notWriteableError(receiver, "setLogical");
    }

    // ---------------------------------------------------------------------
    // Methods specific to String data

    public String[] getReadonlyStringData(Object receiver) {
        return getStringDataCopy(receiver);
    }

    public String[] getStringDataCopy(Object receiver) {
        throw notImplemented(receiver);
    }

    public String getStringAt(Object receiver, int index) {
        RType type = getType(receiver);
        switch (type) {
            case Integer:
                return int2string(getNACheck(receiver), getIntAt(receiver, index));
            case Double:
                return double2string(getNACheck(receiver), getDoubleAt(receiver, index));
            case Logical:
                return logical2string(getNACheck(receiver), getLogicalAt(receiver, index));
            case Raw:
                return raw2string(getRawAt(receiver, index));
            case Complex:
                return complex2string(getNACheck(receiver), getComplexAt(receiver, index));
            case Character:
                CompilerDirectives.transferToInterpreter();
                throw RInternalError.shouldNotReachHere("should be exported elsewhere " + receiver);
            default:
                CompilerDirectives.transferToInterpreter();
                throw RInternalError.shouldNotReachHere(type.toString());
        }
    }

    public String getNextString(Object receiver, SeqIterator it) {
        RType type = getType(receiver);
        switch (type) {
            case Integer:
                return int2string(getNACheck(receiver), getNextInt(receiver, it));
            case Double:
                return double2string(getNACheck(receiver), getNextDouble(receiver, it));
            case Logical:
                return logical2string(getNACheck(receiver), getNextLogical(receiver, it));
            case Raw:
                return raw2string(getNextRaw(receiver, it));
            case Complex:
                return complex2string(getNACheck(receiver), getNextComplex(receiver, it));
            case Character:
                CompilerDirectives.transferToInterpreter();
                throw RInternalError.shouldNotReachHere("should be exported elsewhere " + receiver);
            default:
                CompilerDirectives.transferToInterpreter();
                throw RInternalError.shouldNotReachHere(type.toString());
        }
    }

    public String getString(Object receiver, RandomAccessIterator it, int index) {
        RType type = getType(receiver);
        return getStringImpl(receiver, type, it, index);
    }

    public final String getStringImpl(Object receiver, RType type, RandomAccessIterator it, int index) {
        switch (type) {
            case Integer:
                return int2string(getNACheck(receiver), getInt(receiver, it, index));
            case Double:
                return double2string(getNACheck(receiver), getDouble(receiver, it, index));
            case Logical:
                return logical2string(getNACheck(receiver), getLogical(receiver, it, index));
            case Raw:
                return raw2string(getRaw(receiver, it, index));
            case Complex:
                return complex2string(getNACheck(receiver), getComplex(receiver, it, index));
            case Character:
                CompilerDirectives.transferToInterpreter();
                throw RInternalError.shouldNotReachHere("should be exported elsewhere " + receiver);
            default:
                CompilerDirectives.transferToInterpreter();
                throw RInternalError.shouldNotReachHere(type.toString());
        }
    }

    // ---------------------------------------------------------------------
    // Methods specific to CharSXP data

    public CharSXPWrapper[] getReadonlyCharSXPData(Object receiver) {
        return getCharSXPDataCopy(receiver);
    }

    public CharSXPWrapper[] getCharSXPDataCopy(Object receiver) {
        throw notImplemented(receiver);
    }

    public CharSXPWrapper getCharSXPAt(Object receiver, int index) {
        RType type = getType(receiver);
        switch (type) {
            case Integer:
                return int2charSXP(getNACheck(receiver), getIntAt(receiver, index));
            case Double:
                return double2charSXP(getNACheck(receiver), getDoubleAt(receiver, index));
            case Logical:
                return logical2charSXP(getNACheck(receiver), getLogicalAt(receiver, index));
            case Raw:
                return raw2charSXP(getRawAt(receiver, index));
            case Complex:
                return complex2charSXP(getNACheck(receiver), getComplexAt(receiver, index));
            case Character:
                CompilerDirectives.transferToInterpreter();
                throw RInternalError.shouldNotReachHere("should be exported elsewhere " + receiver);
            default:
                CompilerDirectives.transferToInterpreter();
                throw RInternalError.shouldNotReachHere(type.toString());
        }
    }

    public CharSXPWrapper getNextCharSXP(Object receiver, SeqIterator it) {
        RType type = getType(receiver);
        switch (type) {
            case Integer:
                return int2charSXP(getNACheck(receiver), getNextInt(receiver, it));
            case Double:
                return double2charSXP(getNACheck(receiver), getNextDouble(receiver, it));
            case Logical:
                return logical2charSXP(getNACheck(receiver), getNextLogical(receiver, it));
            case Raw:
                return raw2charSXP(getNextRaw(receiver, it));
            case Complex:
                return complex2charSXP(getNACheck(receiver), getNextComplex(receiver, it));
            case Character:
                CompilerDirectives.transferToInterpreter();
                throw RInternalError.shouldNotReachHere("should be exported elsewhere " + receiver);
            default:
                CompilerDirectives.transferToInterpreter();
                throw RInternalError.shouldNotReachHere(type.toString());
        }
    }

    public CharSXPWrapper getCharSXP(Object receiver, RandomAccessIterator it, int index) {
        RType type = getType(receiver);
        switch (type) {
            case Integer:
                return int2charSXP(getNACheck(receiver), getInt(receiver, it, index));
            case Double:
                return double2charSXP(getNACheck(receiver), getDouble(receiver, it, index));
            case Logical:
                return logical2charSXP(getNACheck(receiver), getLogical(receiver, it, index));
            case Raw:
                return raw2charSXP(getRaw(receiver, it, index));
            case Complex:
                return complex2charSXP(getNACheck(receiver), getComplex(receiver, it, index));
            case Character:
                CompilerDirectives.transferToInterpreter();
                throw RInternalError.shouldNotReachHere("should be exported elsewhere " + receiver);
            default:
                CompilerDirectives.transferToInterpreter();
                throw RInternalError.shouldNotReachHere(type.toString());
        }
    }

    @SuppressWarnings("unused")
    public void setStringAt(Object receiver, int index, String value) {
        throw notWriteableError(receiver, "setStringAt");
    }

    @SuppressWarnings("unused")
    public void setNextString(Object receiver, SeqWriteIterator it, String value) {
        throw notWriteableError(receiver, "setNextString");
    }

    @SuppressWarnings("unused")
    public void setString(Object receiver, RandomAccessWriteIterator it, int index, String value) {
        throw notWriteableError(receiver, "setString");
    }

    public void setCharSXPAt(Object receiver, int index, CharSXPWrapper value) {
        throw notWriteableError(receiver, "setStringAt");
    }

    public void setNextCharSXP(Object receiver, SeqWriteIterator it, CharSXPWrapper value) {
        throw notWriteableError(receiver, "setNextString");
    }

    public void setCharSXP(Object receiver, RandomAccessWriteIterator it, int index, CharSXPWrapper value) {
        throw notWriteableError(receiver, "setString");
    }

    private final ConditionProfile emptyStringProfile = ConditionProfile.createBinaryProfile();

    // ---------------------------------------------------------------------
    // Methods specific to complex data

    public double[] getReadonlyComplexData(Object receiver) {
        return getComplexDataCopy(receiver);
    }

    public double[] getComplexDataCopy(Object receiver) {
        throw notImplemented(receiver);
    }

    /**
     * See {@link #getIntRegion}.
     */
    public int getComplexRegion(Object receiver, int startIndex, int size, Object buffer, InteropLibrary bufferInterop) {
        return getRegion(receiver, startIndex, size, buffer, bufferInterop, RType.Complex);
    }

    /**
     * Provides view on the complex data as a flat array of doubles.
     */
    public double getComplexComponentAt(Object receiver, @SuppressWarnings("unused") int index) {
        CompilerDirectives.transferToInterpreter();
        throw RInternalError.shouldNotReachHere(getType(receiver).toString());
    }

    /**
     * Provides view on the complex data as a flat array of doubles.
     */
    public double setComplexComponentAt(Object receiver, @SuppressWarnings("unused") int index, @SuppressWarnings("unused") double value) {
        CompilerDirectives.transferToInterpreter();
        throw RInternalError.shouldNotReachHere(getType(receiver).toString());
    }

    public RComplex getComplexAt(Object receiver, @SuppressWarnings("unused") int index) {
        RType type = getType(receiver);
        switch (type) {
            case Integer:
                return int2complex(getNACheck(receiver), getIntAt(receiver, index));
            case Double:
                return double2complex(getNACheck(receiver), getDoubleAt(receiver, index));
            case Logical:
                return logical2complex(getNACheck(receiver), getLogicalAt(receiver, index));
            case Raw:
                return raw2complex(getRawAt(getNACheck(receiver), index));
            case Complex:
                CompilerDirectives.transferToInterpreter();
                throw RInternalError.shouldNotReachHere("should be exported elsewhere " + receiver);
            case Character:
                return string2complex(getNACheck(receiver), getStringAt(receiver, index));
            default:
                CompilerDirectives.transferToInterpreter();
                throw RInternalError.shouldNotReachHere(type.toString());
        }
    }

    public RComplex getNextComplex(Object receiver, @SuppressWarnings("unused") SeqIterator it) {
        RType type = getType(receiver);
        switch (type) {
            case Integer:
                return int2complex(getNACheck(receiver), getNextInt(receiver, it));
            case Double:
                return double2complex(getNACheck(receiver), getNextDouble(receiver, it));
            case Logical:
                return logical2complex(getNACheck(receiver), getNextLogical(receiver, it));
            case Raw:
                return raw2complex(getNextRaw(receiver, it));
            case Complex:
                CompilerDirectives.transferToInterpreter();
                throw RInternalError.shouldNotReachHere("should be exported elsewhere " + receiver);
            case Character:
                return string2complex(getNACheck(receiver), getNextString(receiver, it));
            default:
                CompilerDirectives.transferToInterpreter();
                throw RInternalError.shouldNotReachHere(type.toString());
        }
    }

    public RComplex getComplex(Object receiver, @SuppressWarnings("unused") RandomAccessIterator it, @SuppressWarnings("unused") int index) {
        RType type = getType(receiver);
        return getComplexImpl(receiver, type, it, index);
    }

    private RComplex getComplexImpl(Object receiver, RType type, RandomAccessIterator it, int index) {
        switch (type) {
            case Integer:
                return int2complex(getNACheck(receiver), getInt(receiver, it, index));
            case Double:
                return double2complex(getNACheck(receiver), getDouble(receiver, it, index));
            case Logical:
                return logical2complex(getNACheck(receiver), getLogical(receiver, it, index));
            case Raw:
                return raw2complex(getRaw(receiver, it, index));
            case Complex:
                CompilerDirectives.transferToInterpreter();
                throw RInternalError.shouldNotReachHere("should be exported elsewhere " + receiver);
            case Character:
                return string2complex(getNACheck(receiver), getString(receiver, it, index));
            default:
                CompilerDirectives.transferToInterpreter();
                throw RInternalError.shouldNotReachHere(type.toString());
        }
    }

    @SuppressWarnings("unused")
    public void setComplexAt(Object receiver, int index, RComplex value) {
        throw notWriteableError(receiver, "setComplexAt");
    }

    @SuppressWarnings("unused")
    public void setNextComplex(Object receiver, SeqWriteIterator it, RComplex value) {
        throw notWriteableError(receiver, "setNextComplex");
    }

    @SuppressWarnings("unused")
    public void setComplex(Object receiver, RandomAccessWriteIterator it, int index, RComplex value) {
        throw notWriteableError(receiver, "setComplex");
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
                return RRaw.valueOf(getRaw(receiver, it, index));
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
                setIntAt(receiver, index, (Integer) value);
                break;
            case Double:
                setDoubleAt(receiver, index, (Double) value);
                break;
            case Logical:
                setLogicalAt(receiver, index, (Byte) value);
                break;
            case Raw:
                byte rawVal = value instanceof RRaw ? ((RRaw) value).getValue() : (Byte) value;
                setRawAt(receiver, index, rawVal);
                break;
            case Complex:
                setComplexAt(receiver, index, (RComplex) value);
                break;
            case Character:
                setStringAt(receiver, index, (String) value);
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

    /**
     * Transfers an element from source to destination. The element will be coerced to the type of
     * the destination data object.
     */
    public void transferNext(Object dest, SeqWriteIterator destIt, VectorDataLibrary sourceLib, SeqIterator sourceIt, Object source) {
        RType destType = getType(dest);
        switch (destType) {
            case Integer:
                setNextInt(dest, destIt, sourceLib.getNextInt(source, sourceIt));
                break;
            case Double:
                setNextDouble(dest, destIt, sourceLib.getNextDouble(source, sourceIt));
                break;
            case Logical:
                setNextLogical(dest, destIt, sourceLib.getNextLogical(source, sourceIt));
                break;
            case Raw:
                setNextRaw(dest, destIt, sourceLib.getNextRaw(source, sourceIt));
                break;
            case Complex:
                setNextComplex(dest, destIt, sourceLib.getNextComplex(source, sourceIt));
                break;
            case Character:
                setNextString(dest, destIt, sourceLib.getNextString(source, sourceIt));
                break;
            case List:
            case PairList:
            case Language:
            case Expression:
                setNextElement(dest, destIt, sourceLib.getNextElement(source, sourceIt));
                break;
            default:
                CompilerDirectives.transferToInterpreter();
                throw RInternalError.unimplemented(destType.toString());
        }
    }

    /**
     * Transfers an element from source to destination. The element will be coerced to the type of
     * the destination data object.
     */
    public void transferNextToRandom(Object dest, RandomAccessWriteIterator destIt, int destIdx, VectorDataLibrary sourceLib, SeqIterator sourceIt, Object source) {
        RType destType = getType(dest);
        switch (destType) {
            case Integer:
                setInt(dest, destIt, destIdx, sourceLib.getNextInt(source, sourceIt));
                break;
            case Double:
                setDouble(dest, destIt, destIdx, sourceLib.getNextDouble(source, sourceIt));
                break;
            case Logical:
                setLogical(dest, destIt, destIdx, sourceLib.getNextLogical(source, sourceIt));
                break;
            case Raw:
                setRaw(dest, destIt, destIdx, sourceLib.getNextRaw(source, sourceIt));
                break;
            case Complex:
                setComplex(dest, destIt, destIdx, sourceLib.getNextComplex(source, sourceIt));
                break;
            case Character:
                setString(dest, destIt, destIdx, sourceLib.getNextString(source, sourceIt));
                break;
            case List:
            case PairList:
            case Language:
            case Expression:
                setElement(dest, destIt, destIdx, sourceLib.getNextElement(source, sourceIt));
                break;
            default:
                CompilerDirectives.transferToInterpreter();
                throw RInternalError.unimplemented(destType.toString());
        }
    }

    /**
     * Variant of
     * {@link #transferNext(Object, SeqWriteIterator, VectorDataLibrary, SeqIterator, Object)} that
     * supports random access.
     */
    public void transfer(Object dest, RandomAccessWriteIterator destIt, int destIdx, VectorDataLibrary sourceLib,
                    RandomAccessIterator sourceIt,
                    Object source, int sourceIdx) {
        RType destType = getType(dest);
        switch (destType) {
            case Integer:
                setInt(dest, destIt, destIdx, sourceLib.getInt(source, sourceIt, sourceIdx));
                break;
            case Double:
                setDouble(dest, destIt, destIdx, sourceLib.getDouble(source, sourceIt, sourceIdx));
                break;
            case Logical:
                setLogical(dest, destIt, destIdx, sourceLib.getLogical(source, sourceIt, sourceIdx));
                break;
            case Raw:
                setRaw(dest, destIt, destIdx, sourceLib.getRaw(source, sourceIt, sourceIdx));
                break;
            case Complex:
                setComplex(dest, destIt, destIdx, sourceLib.getComplex(source, sourceIt, sourceIdx));
                break;
            case Character:
                setString(dest, destIt, destIdx, sourceLib.getString(source, sourceIt, sourceIdx));
                break;
            case List:
            case PairList:
            case Language:
            case Expression:
                setElement(dest, destIt, destIdx, sourceLib.getElement(source, sourceIt, sourceIdx));
                break;
            default:
                CompilerDirectives.transferToInterpreter();
                throw RInternalError.unimplemented(destType.toString());
        }
    }

    /**
     * Returns given element converted to given type. Unlike {@link #getDataAtAsObject(Object, int)}
     * , which keeps the type of the underlying data.
     */
    public Object getDataAtAs(Object data, RType type, int index) {
        CompilerAsserts.compilationConstant(type);
        switch (type) {
            case Integer:
                return getIntAt(data, index);
            case Double:
                return getDoubleAt(data, index);
            case Logical:
                return getLogicalAt(data, index);
            case Raw:
                return RRaw.valueOf(getRawAt(data, index));
            case Complex:
                return getComplexAt(data, index);
            case Character:
                return getStringAt(data, index);
            default:
                CompilerDirectives.transferToInterpreter();
                throw RInternalError.shouldNotReachHere(type.toString());
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
                return RRaw.valueOf(getRawAt(data, index));
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

    public void setDataAtAsObject(Object data, int index, Object value) {
        RType type = getType(data);
        switch (type) {
            case Integer:
                setIntAt(data, index, (Integer) value);
                break;
            case Double:
                setDoubleAt(data, index, (Double) value);
                break;
            case Logical:
                setLogicalAt(data, index, (Byte) value);
                break;
            case Raw:
                byte rawVal = value instanceof RRaw ? ((RRaw) value).getValue() : (Byte) value;
                setRawAt(data, index, rawVal);
                break;
            case Complex:
                setComplexAt(data, index, (RComplex) value);
                break;
            case Character:
                setStringAt(data, index, (String) value);
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

    /**
     * Sets the right NA value (according to the type of the data) at given position. Note: for raw
     * vectors, this sets value {@code 0x00}.
     */
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
                setRaw(data, it, index, (byte) 0);
                break;
            case Complex:
                setComplex(data, it, index, RRuntime.COMPLEX_NA);
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

    /**
     * @see #setNA(Object, RandomAccessWriteIterator, int)
     */
    public void setNextNA(Object data, SeqWriteIterator it) {
        RType type = getType(data);
        switch (type) {
            case Integer:
                setNextInt(data, it, RRuntime.INT_NA);
                break;
            case Double:
                setNextDouble(data, it, RRuntime.DOUBLE_NA);
                break;
            case Logical:
                setNextLogical(data, it, RRuntime.LOGICAL_NA);
                break;
            case Raw:
                setNextRaw(data, it, (byte) 0);
                break;
            case Complex:
                setNextComplex(data, it, RRuntime.COMPLEX_NA);
                break;
            case Character:
                setNextString(data, it, RRuntime.STRING_NA);
                break;
            case List:
            case PairList:
            case Language:
            case Expression:
                // To be compatible with the VectorAccess API, NAs are treated as NULLs...
                setNextElement(data, it, RNull.instance);
                break;
            default:
                CompilerDirectives.transferToInterpreter();
                throw RInternalError.unimplemented(type.toString());
        }
    }

    public boolean isNextNA(Object data, SeqIterator it) {
        RType type = getType(data);
        switch (type) {
            case Integer:
                return getNACheck(data).check(getNextInt(data, it));
            case Double:
                return getNACheck(data).check(getNextDouble(data, it));
            case Logical:
                return getNACheck(data).check(getNextLogical(data, it));
            case Raw:
                return false;
            case Complex:
                return getNACheck(data).check(getNextComplex(data, it));
            case Character:
                return getNACheck(data).check(getNextString(data, it));
            case List:
            case PairList:
            case Language:
            case Expression:
                // To be compatible with the VectorAccess API, checkListElement checks for NULLs not
                // NAs...
                return getNACheck(data).checkListElement(getNextElement(data, it));
            default:
                CompilerDirectives.transferToInterpreter();
                throw RInternalError.unimplemented(type.toString());
        }
    }

    public boolean isNAAt(Object data, int index) {
        RType type = getType(data);
        switch (type) {
            case Integer:
                return getNACheck(data).check(getIntAt(data, index));
            case Double:
                return getNACheck(data).check(getDoubleAt(data, index));
            case Logical:
                return getNACheck(data).check(getLogicalAt(data, index));
            case Raw:
                return false;
            case Complex:
                return getNACheck(data).check(getComplexAt(data, index));
            case Character:
                return getNACheck(data).check(getStringAt(data, index));
            case List:
            case PairList:
            case Language:
            case Expression:
                // To be compatible with the VectorAccess API, checkListElement checks for NULLs not
                // NAs...
                return getNACheck(data).checkListElement(getElementAt(data, index));
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
            return delegate.accepts(receiver);
        }

        @Override
        public int getLength(Object data) {
            int result = delegate.getLength(data);
            assert result >= 0;
            return result;
        }

        @Override
        public NACheck getNACheck(Object receiver) {
            return delegate.getNACheck(receiver);
        }

        @Override
        public RType getType(Object data) {
            RType result = delegate.getType(data);
            assert result != null;
            return result;
        }

        @Override
        public Object cast(Object receiver, RType targetType) {
            CompilerAsserts.partialEvaluationConstant(targetType);
            Object result = delegate.cast(receiver, targetType);
            assert VectorDataLibrary.getFactory().getUncached().getType(result) == targetType;
            return result;
        }

        @Override
        public Object materialize(Object data) {
            verifyIfSlowAssertsEnabled(data);
            Object result = delegate.materialize(data);
            assert result != null;
            assert getUncachedLib().isWriteable(result);
            return result;
        }

        @Override
        public Object materializeCharSXPStorage(Object data) {
            verifyIfSlowAssertsEnabled(data);
            Object result = delegate.materializeCharSXPStorage(data);
            if (result == null) {
                throw RInternalError.shouldNotReachHere("materializeCharSXPStorage message not implemented for " + delegate.toString());
            }
            assert result != null;
            return result;
        }

        @Override
        public Object copy(Object data, boolean deep) {
            verifyIfSlowAssertsEnabled(data);
            return delegate.copy(data, deep);
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
        public void setDataAtAsObject(Object data, int index, Object value) {
            assert index >= 0 && index < delegate.getLength(data);
            delegate.setDataAtAsObject(data, index, value);
            assert isSame(delegate.getDataAtAsObject(data, index), value) : "data: " + data + ", delegate: " + delegate +
                            ", index: " + index + ", value: " + value + ", dataAt: " +
                            delegate.getDataAtAsObject(data, index);
            // NA written -> complete must be false
            assert !delegate.isNAAt(data, index) || !delegate.isComplete(data) : "data: " + data + ", delegate: " + delegate + ", value: " + value + " " +
                            delegate.isNAAt(data, index) + " " +
                            delegate.isComplete(data);
        }

        private static boolean isSame(Object o1, Object o2) {
            if (o1 instanceof Double) {
                if (RRuntime.isNAorNaN((double) o1) && !RRuntime.isNAorNaN((double) o2)) {
                    return false;
                }
                if (!RRuntime.isNAorNaN((double) o1) && RRuntime.isNAorNaN((double) o2)) {
                    return false;
                }
                // if (o1 == (Double) o2) {
                // return true;
                // }
                return true;
            }
            if (o1.equals(o2)) {
                return true;
            }
            return false;
        }

        @Override
        public void setIntAt(Object receiver, int index, int value) {
            assert index >= 0 && index < delegate.getLength(receiver);
            delegate.setIntAt(receiver, index, value);
            assert delegate.getIntAt(receiver, index) == value;
            // NA written -> complete must be false
            assert !RRuntime.isNA(delegate.getIntAt(receiver, index)) || !delegate.isComplete(receiver);
        }

        @Override
        public void setDoubleAt(Object receiver, int index, double value) {
            assert index >= 0 && index < delegate.getLength(receiver);
            delegate.setDoubleAt(receiver, index, value);
            assert isSame(delegate.getDoubleAt(receiver, index), value);
            // NA written -> complete must be false
            assert !RRuntime.isNA(delegate.getDoubleAt(receiver, index)) || !delegate.isComplete(receiver) : delegate.getDoubleAt(receiver, index) + " " + delegate.isComplete(receiver);
        }

        @Override
        public void setLogicalAt(Object receiver, int index, byte value) {
            assert index >= 0 && index < delegate.getLength(receiver);
            delegate.setLogicalAt(receiver, index, value);
            assert delegate.getLogicalAt(receiver, index) == value;
            // NA written -> complete must be false
            assert !RRuntime.isNA(delegate.getLogicalAt(receiver, index)) || !delegate.isComplete(receiver);
        }

        @Override
        public void commitWriteIterator(Object receiver, SeqWriteIterator iterator, boolean neverSeenNA) {
            delegate.commitWriteIterator(receiver, iterator, neverSeenNA);
            verifyIfSlowAssertsEnabled(receiver);
        }

        @Override
        public void commitRandomAccessWriteIterator(Object receiver, RandomAccessWriteIterator iterator, boolean neverSeenNA) {
            delegate.commitRandomAccessWriteIterator(receiver, iterator, neverSeenNA);
            verifyIfSlowAssertsEnabled(receiver);
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
                        assert !isComplete || !RRuntime.isNA(doubleVal) : isComplete + " " + doubleVal;
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
                        assert ((RRaw) lib.getDataAtAsObject(data, i)).getValue() == rawVal;
                    }
                    break;
                case Character:
                    for (int i = 0; i < len; i++) {
                        String stringVal = lib.getStringAt(data, i);
                        assert !isComplete || !RRuntime.isNA(stringVal);
                        assert stringVal == null && lib.getDataAtAsObject(data, i) == null || lib.getDataAtAsObject(data, i).equals(stringVal);
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

        private void verifyBufferSize(Object receiver, int startIndex, int size, Object buffer, InteropLibrary bufferInterop) {
            assert buffer != null;
            assert bufferInterop.hasArrayElements(buffer);
            assert 0 <= startIndex && startIndex < delegate.getLength(receiver);
            long bufSize = 0;
            try {
                bufSize = bufferInterop.getArraySize(buffer);
            } catch (UnsupportedMessageException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
            assert bufSize >= size;
            verifyIfSlowAssertsEnabled(receiver);
        }

        // TODO: there methods simply delegate, but may be enhanced with assertions

        @Override
        public long asPointer(Object data) throws UnsupportedMessageException {
            return delegate.asPointer(data);
        }

        @Override
        public boolean nextImpl(Object receiver, SeqIterator it, boolean loopCondition) {
            return delegate.nextImpl(receiver, it, loopCondition);
        }

        @Override
        public void nextWithWrap(Object receiver, SeqIterator it) {
            delegate.nextWithWrap(receiver, it);
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
        public SeqWriteIterator writeIterator(Object receiver) {
            verifyIfSlowAssertsEnabled(receiver);
            return delegate.writeIterator(receiver);
        }

        @Override
        public RandomAccessWriteIterator randomAccessWriteIterator(Object receiver) {
            verifyIfSlowAssertsEnabled(receiver);
            return delegate.randomAccessWriteIterator(receiver);
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
        public int getIntRegion(Object receiver, int startIndex, int size, Object buffer, InteropLibrary bufferInterop) {
            verifyBufferSize(receiver, startIndex, size, buffer, bufferInterop);
            return delegate.getIntRegion(receiver, startIndex, size, buffer, bufferInterop);
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
        public boolean isNextNA(Object data, SeqIterator it) {
            return delegate.isNextNA(data, it);
        }

        @Override
        public boolean isNAAt(Object data, int index) {
            return delegate.isNAAt(data, index);
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
        public int getDoubleRegion(Object receiver, int startIndex, int size, Object buffer, InteropLibrary bufferInterop) {
            verifyBufferSize(receiver, startIndex, size, buffer, bufferInterop);
            return delegate.getDoubleRegion(receiver, startIndex, size, buffer, bufferInterop);
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
        public int getLogicalRegion(Object receiver, int startIndex, int size, Object buffer, InteropLibrary bufferInterop) {
            verifyBufferSize(receiver, startIndex, size, buffer, bufferInterop);
            return delegate.getLogicalRegion(receiver, startIndex, size, buffer, bufferInterop);
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
        public int getRawRegion(Object receiver, int startIndex, int size, Object buffer, InteropLibrary bufferInterop) {
            verifyBufferSize(receiver, startIndex, size, buffer, bufferInterop);
            return delegate.getRawRegion(receiver, startIndex, size, buffer, bufferInterop);
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
        public void setRawAt(Object receiver, int index, byte value) {
            delegate.setRawAt(receiver, index, value);
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
        public CharSXPWrapper[] getReadonlyCharSXPData(Object receiver) {
            verifyIfSlowAssertsEnabled(receiver);
            return delegate.getReadonlyCharSXPData(receiver);
        }

        @Override
        public CharSXPWrapper[] getCharSXPDataCopy(Object receiver) {
            verifyIfSlowAssertsEnabled(receiver);
            return delegate.getCharSXPDataCopy(receiver);
        }

        @Override
        public CharSXPWrapper getCharSXPAt(Object receiver, int index) {
            return delegate.getCharSXPAt(receiver, index);
        }

        @Override
        public CharSXPWrapper getNextCharSXP(Object receiver, SeqIterator it) {
            return delegate.getNextCharSXP(receiver, it);
        }

        @Override
        public CharSXPWrapper getCharSXP(Object receiver, RandomAccessIterator it, int index) {
            return delegate.getCharSXP(receiver, it, index);
        }

        @Override
        public void setStringAt(Object receiver, int index, String value) {
            delegate.setStringAt(receiver, index, value);
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
        public void setCharSXPAt(Object receiver, int index, CharSXPWrapper value) {
            delegate.setCharSXPAt(receiver, index, value);
        }

        @Override
        public void setNextCharSXP(Object receiver, SeqWriteIterator it, CharSXPWrapper value) {
            delegate.setNextCharSXP(receiver, it, value);
        }

        @Override
        public void setCharSXP(Object receiver, RandomAccessWriteIterator it, int index, CharSXPWrapper value) {
            delegate.setCharSXP(receiver, it, index, value);
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
        public int getComplexRegion(Object receiver, int startIndex, int size, Object buffer, InteropLibrary bufferInterop) {
            verifyBufferSize(receiver, startIndex, size, buffer, bufferInterop);
            return delegate.getComplexRegion(receiver, startIndex, size, buffer, bufferInterop);
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
        public void setComplexAt(Object receiver, int index, RComplex value) {
            delegate.setComplexAt(receiver, index, value);
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

    private static byte complex2logical(NACheck naCheck, RComplex value) {
        return naCheck.check(value) ? RRuntime.LOGICAL_NA : RRuntime.complex2logicalNoCheck(value);
    }

    private static String complex2string(NACheck naCheck, RComplex value) {
        return naCheck.check(value) ? RRuntime.STRING_NA : RContext.getRRuntimeASTAccess().encodeComplex(value);
    }

    private static CharSXPWrapper complex2charSXP(NACheck naCheck, RComplex value) {
        return CharSXPWrapper.create(complex2string(naCheck, value));
    }

    private static byte complex2raw(NACheck naCheck, RComplex value) {
        naCheck.check(value);
        double realPart = value.getRealPart();
        double realResult = realPart;

        if (realPart > Integer.MAX_VALUE || realPart <= Integer.MIN_VALUE) {
            // warningReportedProfile.enter();
            // accessIter.warning(RError.Message.NA_INTRODUCED_COERCION_INT);
            realResult = 0;
        }

        if (value.getImaginaryPart() != 0) {
            // warningReportedProfile.enter();
            // accessIter.warning(RError.Message.IMAGINARY_PARTS_DISCARDED_IN_COERCION);
        }

        if (Double.isNaN(realPart) || realPart < 0 || realPart >= 256) {
            // warningReportedProfile.enter();
            // accessIter.warning(RError.Message.OUT_OF_RANGE);
            realResult = 0;
        }
        return (byte) RRuntime.double2rawIntValue(realResult);
    }

    private static double complex2double(NACheck naCheck, RComplex cpl) {
        double value = cpl.getRealPart();
        if (Double.isNaN(value)) {
            naCheck.enable(true);
            return RRuntime.DOUBLE_NA;
        }
        // if (cpl.getImaginaryPart() != 0) {
        // warningReportedProfile.enter();
        // accessIter.warning(RError.Message.IMAGINARY_PARTS_DISCARDED_IN_COERCION);
        // }
        return value;
    }

    private static int complex2int(NACheck naCheck, RComplex cpl) {
        double value = cpl.getRealPart();
        if (Double.isNaN(value)) {
            naCheck.enable(true);
            return RRuntime.INT_NA;
        }
        if (value > Integer.MAX_VALUE || value <= Integer.MIN_VALUE) {
            naCheck.enable(true);
            // warningReportedProfile.enter();
            // accessIter.warning(RError.Message.NA_INTRODUCED_COERCION_INT);
            return RRuntime.INT_NA;
        }
        if (cpl.getImaginaryPart() != 0) {
            // warningReportedProfile.enter();
            // accessIter.warning(RError.Message.IMAGINARY_PARTS_DISCARDED_IN_COERCION);
        }
        return (int) value;
    }

    private static byte double2raw(NACheck naCheck, double value) {
        naCheck.check(value);
        byte result = (byte) (int) value;
        if ((result & 0xff) != value) {
            // warningReportedProfile.enter();
            // accessIter.warning(RError.Message.OUT_OF_RANGE);
            return 0;
        }
        return result;
    }

    private static String double2string(NACheck naCheck, double value) {
        return naCheck.check(value) ? RRuntime.STRING_NA : RContext.getRRuntimeASTAccess().encodeDouble(value);
    }

    private static CharSXPWrapper double2charSXP(NACheck naCheck, double value) {
        return CharSXPWrapper.create(double2string(naCheck, value));
    }

    private static RComplex double2complex(NACheck naCheck, double value) {
        return naCheck.check(value) ? RRuntime.COMPLEX_NA : RRuntime.double2complexNoCheck(value);
    }

    private static int double2int(NACheck naCheck, double value) {
        naCheck.enable(value);
        if (naCheck.checkNAorNaN(value)) {
            return RRuntime.INT_NA;
        }
        if (value > Integer.MAX_VALUE || value <= Integer.MIN_VALUE) {
            // conversionOverflowReached.enter();
            naCheck.enable(true);
            // warningReportedProfile.enter();
            // accessIter.warning(RError.Message.NA_INTRODUCED_COERCION_INT);
            return RRuntime.INT_NA;
        }
        return (int) value;
    }

    private static byte double2logical(NACheck naCheck, double value) {
        return naCheck.check(value) ? RRuntime.LOGICAL_NA : RRuntime.double2logicalNoCheck(value);
    }

    private static byte int2raw(NACheck naCheck, int value) {
        naCheck.check(value);
        byte result = (byte) value;
        if ((result & 0xff) != value) {
            // warningReportedProfile.enter();
            // accessIter.warning(RError.Message.OUT_OF_RANGE);
            return 0;
        }
        return result;
    }

    private static String int2string(NACheck naCheck, int value) {
        return naCheck.check(value) ? RRuntime.STRING_NA : RRuntime.intToStringNoCheck(value);
    }

    private static CharSXPWrapper int2charSXP(NACheck naCheck, int value) {
        return CharSXPWrapper.create(int2string(naCheck, value));
    }

    private static RComplex int2complex(NACheck naCheck, int value) {
        return naCheck.check(value) ? RRuntime.COMPLEX_NA : RRuntime.int2complexNoCheck(value);
    }

    private static double int2double(NACheck naCheck, int value) {
        return naCheck.check(value) ? RRuntime.DOUBLE_NA : RRuntime.int2doubleNoCheck(value);
    }

    private static byte int2logical(NACheck naCheck, int value) {
        return naCheck.check(value) ? RRuntime.LOGICAL_NA : RRuntime.int2logicalNoCheck(value);
    }

    private static byte logical2raw(NACheck naCheck, byte value) {
        if (naCheck.check(value)) {
            // warningReportedProfile.enter();
            // accessIter.warning(RError.Message.OUT_OF_RANGE);
            return 0;
        } else {
            return value;
        }
    }

    private static String logical2string(NACheck naCheck, byte value) {
        return naCheck.check(value) ? RRuntime.STRING_NA : RRuntime.logicalToStringNoCheck(value);
    }

    private static CharSXPWrapper logical2charSXP(NACheck naCheck, byte value) {
        return CharSXPWrapper.create(logical2string(naCheck, value));
    }

    private static RComplex logical2complex(NACheck naCheck, byte value) {
        return naCheck.check(value) ? RRuntime.COMPLEX_NA : RRuntime.logical2complexNoCheck(value);
    }

    private static double logical2double(NACheck naCheck, byte value) {
        return naCheck.check(value) ? RRuntime.DOUBLE_NA : RRuntime.logical2doubleNoCheck(value);
    }

    private static int logical2int(NACheck naCheck, byte value) {
        return naCheck.check(value) ? RRuntime.INT_NA : RRuntime.logical2intNoCheck(value);
    }

    private static byte raw2logical(byte value) {
        return value == 0 ? RRuntime.LOGICAL_FALSE : RRuntime.LOGICAL_TRUE;
    }

    private static String raw2string(byte value) {
        return RRuntime.rawToHexString(value);
    }

    private static CharSXPWrapper raw2charSXP(byte value) {
        return CharSXPWrapper.create(raw2string(value));
    }

    private static RComplex raw2complex(byte value) {
        return RComplex.valueOf(value & 0xff, 0);
    }

    private static double raw2double(byte value) {
        return value & 0xff;
    }

    private static int raw2int(byte value) {
        return value & 0xff;
    }

    private static byte string2logical(NACheck naCheck, String value) {
        return naCheck.convertStringToLogical(value);
    }

    private byte string2raw(NACheck naCheck, String value) {
        int intValue;
        if (naCheck.check(value) || emptyStringProfile.profile(value.isEmpty())) {
            intValue = RRuntime.INT_NA;
        } else {
            double d = naCheck.convertStringToDouble(value);
            naCheck.enable(d);
            if (naCheck.checkNAorNaN(d)) {
                // if (naCheck.check(d) && !value.isEmpty()) {
                // warningReportedProfile.enter();
                // accessIter.warning(RError.Message.NA_INTRODUCED_COERCION);
                // }
                intValue = RRuntime.INT_NA;
            } else {
                intValue = naCheck.convertDoubleToInt(d);
                // naCheck.enable(intValue);
                // if (naCheck.check(intValue) && !value.isEmpty()) {
                // warningReportedProfile.enter();
                // accessIter.warning(RError.Message.NA_INTRODUCED_COERCION_INT);
                // }
            }
            int intRawValue = RRuntime.int2rawIntValue(intValue);
            if (intValue != intRawValue) {
                // warningReportedProfile.enter();
                // accessIter.warning(RError.Message.OUT_OF_RANGE);
                intValue = 0;
            }
        }
        return intValue >= 0 && intValue <= 255 ? (byte) intValue : 0;
    }

    private RComplex string2complex(NACheck naCheck, String value) {
        RComplex complexValue;
        if (naCheck.check(value) || emptyStringProfile.profile(value.isEmpty())) {
            complexValue = RRuntime.COMPLEX_NA;
        } else {
            complexValue = RRuntime.string2complexNoCheck(value);
            if (complexValue.isNA()) {
                // warningReportedProfile.enter();
                naCheck.enable(true);
                // accessIter.warning(RError.Message.NA_INTRODUCED_COERCION);
            }
        }
        return complexValue;
    }

    private double string2double(NACheck naCheck, String str) {
        if (naCheck.check(str) || emptyStringProfile.profile(str.isEmpty())) {
            naCheck.enable(true);
            return RRuntime.DOUBLE_NA;
        }
        double value = naCheck.convertStringToDouble(str);
        if (RRuntime.isNA(value)) {
            naCheck.enable(true);
            // warningReportedProfile.enter();
            // accessIter.warning(RError.Message.NA_INTRODUCED_COERCION);
            return RRuntime.DOUBLE_NA;
        }
        return value;
    }

    private int string2int(NACheck naCheck, String str) {
        if (naCheck.check(str) || emptyStringProfile.profile(str.isEmpty())) {
            naCheck.enable(true);
            return RRuntime.INT_NA;
        }
        double d = naCheck.convertStringToDouble(str);
        naCheck.enable(d);
        if (naCheck.checkNAorNaN(d)) {
            if (naCheck.check(d)) {
                // warningReportedProfile.enter();
                // accessIter.warning(RError.Message.NA_INTRODUCED_COERCION);
                return RRuntime.INT_NA;
            }
            return RRuntime.INT_NA;
        }
        int value = naCheck.convertDoubleToInt(d);
        naCheck.enable(value);
        if (naCheck.check(value)) {
            // warningReportedProfile.enter();
            // accessIter.warning(RError.Message.NA_INTRODUCED_COERCION_INT);
            return RRuntime.INT_NA;
        }
        return value;
    }

    // Private utility methods

    private static RInternalError notImplemented(Object receiver) {
        CompilerDirectives.transferToInterpreter();
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
