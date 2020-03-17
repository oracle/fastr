/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.data.nodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.ErrorContext;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.AbstractContainerLibrary;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RComplexVector;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RExpression;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RRawVector;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;
import com.oracle.truffle.r.runtime.ops.na.NACheck;
import java.util.HashSet;
import java.util.Set;

/**
 * This class is the main access point for reading and writing vectors. Every implementation of
 * {@link RAbstractContainer} needs to be able to supply an instance of {@link VectorAccess} via the
 * {@link RAbstractContainer#access()} function. These instances can be asked, via the
 * {@link #supports(Object)} function, whether they support a specific object.<br/>
 *
 * The usual interaction with vectors looks like this:
 * <ul>
 * <li>{@link Specialization} {@code foo} has a vector as a parameter, and adds a {@link Cached}
 * parameter of type {@link VectorAccess} that is initialized by {@link RAbstractContainer#access()}
 * .</li>
 * <li>The specialization guards with {@code access.supports(vector)} so that it only handles cases
 * that it actually supports.</li>
 * <li>try-with-resources is used to open an access (either sequential or random) on the vector:
 * <br/>
 * {@code try (SequentialIterator iter = access.access(vector))...}</li>
 * <li>Inside the try-with-resources, individual elements can be accessed using the
 * {@link VectorAccess#getInt(SequentialIterator)}, etc. functions</li>
 * <li>A fallback specialization, which {@link Specialization#replaces()} the original one and
 * creates the {@link VectorAccess} via {@link RAbstractContainer#slowPathAccess()}, calls the first
 * specialization with the slow path vector access.</li>
 * </ul>
 */
public abstract class VectorAccess extends RBaseNode {

    public final NACheck na = NACheck.create();

    protected final Class<?> clazz;
    protected final boolean hasStore;

    public VectorAccess(Class<?> clazz, boolean hasStore) {
        CompilerAsserts.neverPartOfCompilation();
        this.clazz = clazz;
        this.hasStore = hasStore;
    }

    public abstract static class AccessIterator {
        private Set<RError.Message> reportedWarnings;

        protected final Object store; // internal store, native mirror or vector
        protected final int length;
        private final RBaseNode warningContext;

        private AccessIterator(Object store, int length, RBaseNode warningContext) {
            this.store = store;
            this.length = length;
            this.warningContext = warningContext != null ? warningContext : RError.SHOW_CALLER;
        }

        public final Object getStore() {
            return store;
        }

        /**
         * Generates a warning once for a AccessIterator instance.
         * 
         * @param message
         * @return <code>true</code> if a warning was generated, otherwise <code>false</code>
         */
        @TruffleBoundary
        public final boolean warning(RError.Message message) {
            if (reportedWarnings == null) {
                reportedWarnings = new HashSet<>();
            }
            if (reportedWarnings.add(message)) {
                RError.warning(warningContext instanceof ErrorContext ? warningContext : warningContext.getErrorContext(), message);
                return true;
            }
            return false;
        }
    }

    public static final class SequentialIterator extends AccessIterator implements AutoCloseable {

        protected int index;

        private SequentialIterator(Object store, int length, RBaseNode warningContext) {
            super(store, length, warningContext);
            this.index = -1;
        }

        @Override
        public void close() {
            // nothing to do
        }

        public int getIndex() {
            return index;
        }

        @Override
        public String toString() {
            return String.format("<iterator %d of %d, %s>", index, length, store == null ? "null" : store.getClass().getSimpleName());
        }
    }

    public static final class RandomIterator extends AccessIterator implements AutoCloseable {

        private RandomIterator(Object store, int length, RBaseNode warningContext) {
            super(store, length, warningContext);
        }

        @Override
        public void close() {
            // nothing to do
        }

        @Override
        public String toString() {
            return String.format("<random access %s>", store == null ? "null" : store.getClass().getSimpleName());
        }
    }

    protected abstract int getIntImpl(AccessIterator accessIter, int index);

    protected abstract double getDoubleImpl(AccessIterator accessIter, int index);

    protected abstract RComplex getComplexImpl(AccessIterator accessIter, int index);

    protected abstract double getComplexRImpl(AccessIterator accessIter, int index);

    protected abstract double getComplexIImpl(AccessIterator accessIter, int index);

    protected abstract byte getRawImpl(AccessIterator accessIter, int index);

    protected abstract byte getLogicalImpl(AccessIterator accessIter, int index);

    protected abstract String getStringImpl(AccessIterator accessIter, int index);

    protected abstract Object getListElementImpl(AccessIterator accessIter, int index);

    @SuppressWarnings("unused")
    protected void setIntImpl(AccessIterator accessIter, int index, int value) {
        throw RInternalError.shouldNotReachHere();
    }

    @SuppressWarnings("unused")
    protected void setDoubleImpl(AccessIterator accessIter, int index, double value) {
        throw RInternalError.shouldNotReachHere();
    }

    @SuppressWarnings("unused")
    protected void setComplexImpl(AccessIterator accessIter, int index, double real, double imaginary) {
        throw RInternalError.shouldNotReachHere();
    }

    @SuppressWarnings("unused")
    protected void setRawImpl(AccessIterator accessIter, int index, byte value) {
        throw RInternalError.shouldNotReachHere();
    }

    @SuppressWarnings("unused")
    protected void setLogicalImpl(AccessIterator accessIter, int index, byte value) {
        throw RInternalError.shouldNotReachHere();
    }

    @SuppressWarnings("unused")
    protected void setStringImpl(AccessIterator accessIter, int index, String value) {
        throw RInternalError.shouldNotReachHere();
    }

    @SuppressWarnings("unused")
    protected void setListElementImpl(AccessIterator accessIter, int index, Object value) {
        throw RInternalError.shouldNotReachHere();
    }

    @SuppressWarnings("unused")
    protected void setFromSameTypeImpl(AccessIterator accessIter, int index, VectorAccess sourceAccess, SequentialIterator sourceIter) {
        throw RInternalError.shouldNotReachHere();
    }

    @SuppressWarnings("unused")
    protected void setFromSameTypeImpl(AccessIterator accessIter, int index, VectorAccess sourceAccess, RandomIterator sourceIter, int sourceIndex) {
        throw RInternalError.shouldNotReachHere();
    }

    @SuppressWarnings("unused")
    protected void setNAImpl(AccessIterator accessIter, int index) {
        throw RInternalError.shouldNotReachHere();
    }

    protected abstract boolean isNAImpl(AccessIterator accessIter, int index);

    public final Object cast(Object value) {
        return clazz.cast(value);
    }

    public boolean supports(Object value) {
        assert clazz != Object.class : "cannot call 'supports' on slow path vector access";
        if (value.getClass() != clazz) {
            return false;
        }
        Object castVector = cast(value);
        return !(castVector instanceof RAbstractContainer) || (((RAbstractContainer) castVector).getInternalStore() != null) == hasStore;
    }

    protected abstract Object getStore(RAbstractContainer vector);

    protected int getLength(RAbstractContainer vector) {
        return vector.getLength();
    }

    protected int getLength(@SuppressWarnings("unused") Object vector) {
        return 1;
    }

    /**
     * Creates a new iterator that will point to before the beginning of the vector, so that
     * {@link #next(SequentialIterator)} will move it to the first element. <br>
     * In case a warning is generated while accessing data from the vector then
     * {@link RError#SHOW_CALLER} will be used to determine the warning message caller context.
     * 
     * @param vector
     * 
     */
    public final SequentialIterator access(Object vector) {
        return access(AbstractContainerLibrary.getFactory().getUncached(), vector, null);
    }

    public final SequentialIterator access(AbstractContainerLibrary library, Object vector) {
        return access(library, vector, null);
    }

    /**
     * Creates a new iterator that will point to before the beginning of the vector, so that
     * {@link #next(SequentialIterator)} will move it to the first element.
     * 
     * @param vector
     * @param warningContext determines the caller context in a warning message in case a warning is
     *            generated while accessing data from the vector. <br>
     *            Possible values are either an instance {@link ErrorContext} or the actual
     *            accessing node, in which case {@link RBaseNode#getErrorContext()} will be used.
     */
    public final SequentialIterator access(Object vector, RBaseNode warningContext) {
        return access(AbstractContainerLibrary.getFactory().getUncached(), vector, warningContext);
    }

    public final SequentialIterator access(AbstractContainerLibrary library, Object vector, RBaseNode warningContext) {
        Object castVector = cast(vector);
        if (castVector instanceof RAbstractContainer) {
            RAbstractContainer container = (RAbstractContainer) castVector;
            int length = getLength(container);
            RBaseNode.reportWork(this, length);
            na.enable(library, container);
            return new SequentialIterator(getStore(container), length, warningContext);
        } else {
            na.enable(true);
            return new SequentialIterator(castVector, getLength(castVector), warningContext);
        }
    }

    @SuppressWarnings("static-method")
    public final boolean next(SequentialIterator iter) {
        return ++iter.index < iter.length;
    }

    @SuppressWarnings("static-method")
    public final void nextWithWrap(SequentialIterator iter) {
        assert iter.length > 0;
        if (++iter.index >= iter.length) {
            iter.index = 0;
        }
    }

    @SuppressWarnings("static-method")
    public final int getLength(SequentialIterator iter) {
        return iter.length;
    }

    /**
     * Resets the iterator to point to before the first element, calling
     * {@link #next(SequentialIterator)} will move it to the first element.
     */
    @SuppressWarnings("static-method")
    public final void reset(SequentialIterator iter) {
        iter.index = -1;
    }

    public abstract RType getType();

    public int getInt(SequentialIterator iter) {
        return getIntImpl(iter, iter.index);
    }

    public double getDouble(SequentialIterator iter) {
        return getDoubleImpl(iter, iter.index);
    }

    public RComplex getComplex(SequentialIterator iter) {
        return getComplexImpl(iter, iter.index);
    }

    public double getComplexR(SequentialIterator iter) {
        return getComplexRImpl(iter, iter.index);
    }

    public double getComplexI(SequentialIterator iter) {
        return getComplexIImpl(iter, iter.index);
    }

    public final byte getRaw(SequentialIterator iter) {
        return getRawImpl(iter, iter.index);
    }

    public final byte getLogical(SequentialIterator iter) {
        return getLogicalImpl(iter, iter.index);
    }

    public String getString(SequentialIterator iter) {
        return getStringImpl(iter, iter.index);
    }

    public final Object getListElement(SequentialIterator iter) {
        return getListElementImpl(iter, iter.index);
    }

    public final void setInt(SequentialIterator iter, int value) {
        setIntImpl(iter, iter.index, value);
    }

    public final void setDouble(SequentialIterator iter, double value) {
        setDoubleImpl(iter, iter.index, value);
    }

    public final void setComplex(SequentialIterator iter, double real, double imaginary) {
        setComplexImpl(iter, iter.index, real, imaginary);
    }

    public final void setRaw(SequentialIterator iter, byte value) {
        setRawImpl(iter, iter.index, value);
    }

    public final void setLogical(SequentialIterator iter, byte value) {
        setLogicalImpl(iter, iter.index, value);
    }

    public final void setString(SequentialIterator iter, String value) {
        setStringImpl(iter, iter.index, value);
    }

    public final void setListElement(SequentialIterator iter, Object value) {
        setListElementImpl(iter, iter.index, value);
    }

    public final void setFromSameType(SequentialIterator iter, VectorAccess sourceAccess,
                    SequentialIterator sourceIter) {
        setFromSameTypeImpl(iter, iter.index, sourceAccess, sourceIter);
    }

    public final void setFromSameType(SequentialIterator iter, VectorAccess sourceAccess,
                    RandomIterator sourceIter, int sourceIndex) {
        setFromSameTypeImpl(iter, iter.index, sourceAccess, sourceIter, sourceIndex);
    }

    public final void setNA(SequentialIterator iter) {
        setNAImpl(iter, iter.index);
    }

    public final boolean isNA(SequentialIterator iter) {
        return isNAImpl(iter, iter.index);
    }

    /**
     * Creates a new random access on the given vector. <br>
     * In case a warning is generated while accessing data from the vector then
     * {@link RError#SHOW_CALLER} will be used to determine the warning message caller context.
     */
    public final RandomIterator randomAccess(RAbstractContainer vector) {
        return randomAccess(AbstractContainerLibrary.getFactory().getUncached(), vector, null);
    }

    public final RandomIterator randomAccess(AbstractContainerLibrary library, RAbstractContainer vector) {
        return randomAccess(library, vector, null);
    }

    /**
     * Creates a new random access on the given vector.
     * 
     * @param vector
     * @param warningContext determines the caller context in a warning message in case a warning is
     *            generated while accessing data from the vector. <br>
     *            Possible values are either an instance {@link ErrorContext} or the actual
     *            accessing node, in which case {@link RBaseNode#getErrorContext()} will be used.
     */
    public final RandomIterator randomAccess(RAbstractContainer vector, RBaseNode warningContext) {
        return randomAccess(AbstractContainerLibrary.getFactory().getUncached(), vector, warningContext);
    }

    public final RandomIterator randomAccess(AbstractContainerLibrary library, RAbstractContainer vector, RBaseNode warningContext) {
        Object castVector = cast(vector);
        if (castVector instanceof RAbstractContainer) {
            RAbstractContainer container = (RAbstractContainer) castVector;
            int length = getLength(container);
            na.enable(library, container);
            return new RandomIterator(getStore(container), length, warningContext);
        } else {
            na.enable(true);
            return new RandomIterator(castVector, getLength(castVector), warningContext);
        }
    }

    @SuppressWarnings("static-method")
    public final int getLength(RandomIterator iter) {
        return iter.length;
    }

    public int getInt(RandomIterator iter, int index) {
        return getIntImpl(iter, index);
    }

    public double getDouble(RandomIterator iter, int index) {
        return getDoubleImpl(iter, index);
    }

    public RComplex getComplex(RandomIterator iter, int index) {
        return getComplexImpl(iter, index);
    }

    public double getComplexR(RandomIterator iter, int index) {
        return getComplexRImpl(iter, index);
    }

    public double getComplexI(RandomIterator iter, int index) {
        return getComplexIImpl(iter, index);
    }

    public final byte getRaw(RandomIterator iter, int index) {
        return getRawImpl(iter, index);
    }

    public final byte getLogical(RandomIterator iter, int index) {
        return getLogicalImpl(iter, index);
    }

    public String getString(RandomIterator iter, int index) {
        return getStringImpl(iter, index);
    }

    public final Object getListElement(RandomIterator iter, int index) {
        return getListElementImpl(iter, index);
    }

    public final void setInt(RandomIterator iter, int index, int value) {
        setIntImpl(iter, index, value);
    }

    public final void setDouble(RandomIterator iter, int index, double value) {
        setDoubleImpl(iter, index, value);
    }

    public final void setComplex(RandomIterator iter, int index, double real, double imaginary) {
        setComplexImpl(iter, index, real, imaginary);
    }

    public final void setRaw(RandomIterator iter, int index, byte value) {
        setRawImpl(iter, index, value);
    }

    public final void setLogical(RandomIterator iter, int index, byte value) {
        setLogicalImpl(iter, index, value);
    }

    public final void setString(RandomIterator iter, int index, String value) {
        setStringImpl(iter, index, value);
    }

    public final void setListElement(RandomIterator iter, int index, Object value) {
        setListElementImpl(iter, index, value);
    }

    public final void setFromSameType(RandomIterator iter, int index, VectorAccess sourceAccess, SequentialIterator sourceIter) {
        setFromSameTypeImpl(iter, index, sourceAccess, sourceIter);
    }

    public final void setFromSameType(RandomIterator iter, int index, VectorAccess sourceAccess, RandomIterator sourceIter, int sourceIndex) {
        setFromSameTypeImpl(iter, index, sourceAccess, sourceIter, sourceIndex);
    }

    public final void setNA(RandomIterator iter, int index) {
        setNAImpl(iter, index);
    }

    public final boolean isNA(RandomIterator iter, int index) {
        return isNAImpl(iter, index);
    }

    /**
     * Placed in a separate class to avoid circular dependencies during class initialization.
     */
    private static final class Lazy {
        private static final RStringVector TEMPLATE_CHARACTER = RDataFactory.getPermanent().createStringVector(4);
        private static final RComplexVector TEMPLATE_COMPLEX = RDataFactory.getPermanent().createComplexVector(4);
        private static final RDoubleVector TEMPLATE_DOUBLE = RDataFactory.getPermanent().createDoubleVector(4);
        private static final RIntVector TEMPLATE_INTEGER = RDataFactory.getPermanent().createIntVector(4);
        private static final RList TEMPLATE_LIST = RDataFactory.getPermanent().createList(4);
        private static final RExpression TEMPLATE_EXPRESSION = RDataFactory.createExpression(4);
        private static final RLogicalVector TEMPLATE_LOGICAL = RDataFactory.getPermanent().createLogicalVector(4);
        private static final RRawVector TEMPLATE_RAW = RDataFactory.getPermanent().createRawVector(4);
        private static final RPairList TEMPLATE_PAIRLIST = RDataFactory.getPermanent().createPairList();
    }

    public static VectorAccess createNew(RType type) {
        CompilerAsserts.neverPartOfCompilation();
        switch (type) {
            case Character:
                return Lazy.TEMPLATE_CHARACTER.access();
            case Complex:
                return Lazy.TEMPLATE_COMPLEX.access();
            case Double:
                return Lazy.TEMPLATE_DOUBLE.access();
            case Integer:
                return Lazy.TEMPLATE_INTEGER.access();
            case List:
                return Lazy.TEMPLATE_LIST.access();
            case Expression:
                return Lazy.TEMPLATE_EXPRESSION.access();
            case Logical:
                return Lazy.TEMPLATE_LOGICAL.access();
            case Raw:
                return Lazy.TEMPLATE_RAW.access();
            case PairList:
            case Language:
                return Lazy.TEMPLATE_PAIRLIST.access();
            case RInteropChar:
            case RInteropFloat:
            case RInteropLong:
            case RInteropShort:
            default:
                throw RInternalError.shouldNotReachHere();
        }
    }

    @TruffleBoundary
    public static VectorAccess createSlowPathNew(RType type) {
        switch (type) {
            case Character:
                return Lazy.TEMPLATE_CHARACTER.slowPathAccess();
            case Complex:
                return Lazy.TEMPLATE_COMPLEX.slowPathAccess();
            case Double:
                return Lazy.TEMPLATE_DOUBLE.slowPathAccess();
            case Integer:
                return Lazy.TEMPLATE_INTEGER.slowPathAccess();
            case List:
                return Lazy.TEMPLATE_LIST.slowPathAccess();
            case Expression:
                return Lazy.TEMPLATE_EXPRESSION.slowPathAccess();
            case Logical:
                return Lazy.TEMPLATE_LOGICAL.slowPathAccess();
            case Raw:
                return Lazy.TEMPLATE_RAW.slowPathAccess();
            case RInteropChar:
            case RInteropFloat:
            case RInteropLong:
            case RInteropShort:
            default:
                throw RInternalError.shouldNotReachHere();
        }
    }

    public static VectorAccess create(Object value) {
        CompilerAsserts.neverPartOfCompilation();
        if (value instanceof RAbstractContainer) {
            return ((RAbstractContainer) value).access();
        } else {
            return PrimitiveVectorAccess.create(value);
        }
    }

    @TruffleBoundary
    public static VectorAccess createSlowPath(Object value) {
        if (value instanceof RAbstractContainer) {
            return ((RAbstractContainer) value).slowPathAccess();
        } else {
            return PrimitiveVectorAccess.createSlowPath(value);
        }
    }
}
