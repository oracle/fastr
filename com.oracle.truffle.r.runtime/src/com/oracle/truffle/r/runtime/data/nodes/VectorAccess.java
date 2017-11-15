/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.runtime.data.nodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RComplexVector;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RExpression;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.RRawVector;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

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
public abstract class VectorAccess extends Node {

    public final NACheck na = NACheck.create();

    protected final Class<? extends RAbstractContainer> clazz;
    protected final boolean hasStore;

    public VectorAccess(Class<? extends RAbstractContainer> clazz, boolean hasStore) {
        CompilerAsserts.neverPartOfCompilation();
        this.clazz = clazz;
        this.hasStore = hasStore;
    }

    public static final class SequentialIterator implements AutoCloseable {

        protected final Object store; // internal store, native mirror or vector
        protected final int length;
        protected int index;

        private SequentialIterator(Object store, int length) {
            this.store = store;
            this.length = length;
            this.index = -1;
        }

        @Override
        public void close() {
            // nothing to do
        }

        public int getIndex() {
            return index;
        }

        Object getStore() {
            return store;
        }

        @Override
        public String toString() {
            return String.format("<iterator %d of %d, %s>", index, length, store == null ? "null" : store.getClass().getSimpleName());
        }
    }

    public static final class RandomIterator implements AutoCloseable {

        protected final Object store; // internal store, native mirror or vector
        protected final int length;

        private RandomIterator(Object store, int length) {
            this.store = store;
            this.length = length;
        }

        @Override
        public void close() {
            // nothing to do
        }

        Object getStore() {
            return store;
        }

        @Override
        public String toString() {
            return String.format("<random access %s>", store == null ? "null" : store.getClass().getSimpleName());
        }
    }

    protected abstract int getInt(Object store, int index);

    protected abstract double getDouble(Object store, int index);

    protected abstract RComplex getComplex(Object store, int index);

    protected abstract double getComplexR(Object store, int index);

    protected abstract double getComplexI(Object store, int index);

    protected abstract byte getRaw(Object store, int index);

    protected abstract byte getLogical(Object store, int index);

    protected abstract String getString(Object store, int index);

    protected abstract Object getListElement(Object store, int index);

    @SuppressWarnings("unused")
    protected void setInt(Object store, int index, int value) {
        throw RInternalError.shouldNotReachHere();
    }

    @SuppressWarnings("unused")
    protected void setDouble(Object store, int index, double value) {
        throw RInternalError.shouldNotReachHere();
    }

    @SuppressWarnings("unused")
    protected void setComplex(Object store, int index, double real, double imaginary) {
        throw RInternalError.shouldNotReachHere();
    }

    @SuppressWarnings("unused")
    protected void setRaw(Object store, int index, byte value) {
        throw RInternalError.shouldNotReachHere();
    }

    @SuppressWarnings("unused")
    protected void setLogical(Object store, int index, byte value) {
        throw RInternalError.shouldNotReachHere();
    }

    @SuppressWarnings("unused")
    protected void setString(Object store, int index, String value) {
        throw RInternalError.shouldNotReachHere();
    }

    @SuppressWarnings("unused")
    protected void setListElement(Object store, int index, Object value) {
        throw RInternalError.shouldNotReachHere();
    }

    @SuppressWarnings("unused")
    protected void setFromSameType(Object store, int index, VectorAccess sourceAccess, SequentialIterator sourceIter) {
        throw RInternalError.shouldNotReachHere();
    }

    @SuppressWarnings("unused")
    protected void setFromSameType(Object store, int index, VectorAccess sourceAccess, RandomIterator sourceIter, int sourceIndex) {
        throw RInternalError.shouldNotReachHere();
    }

    @SuppressWarnings("unused")
    protected void setNA(Object store, int index) {
        throw RInternalError.shouldNotReachHere();
    }

    protected abstract boolean isNA(Object store, int index);

    public final RAbstractContainer cast(Object value) {
        return clazz.cast(value);
    }

    public final boolean supports(Object value) {
        assert clazz != RAbstractContainer.class : "cannot call 'supports' on slow path vector access";
        return value.getClass() == clazz && (cast(value).getInternalStore() != null) == hasStore;
    }

    protected abstract Object getStore(RAbstractContainer vector);

    protected int getLength(RAbstractContainer vector) {
        return vector.getLength();
    }

    /**
     * Creates a new iterator that will point to before the beginning of the vector, so that
     * {@link #next(SequentialIterator)} will move it to the first element.
     */
    public final SequentialIterator access(RAbstractContainer vector) {
        RAbstractContainer castVector = cast(vector);
        int length = getLength(castVector);
        RBaseNode.reportWork(this, length);
        na.enable(castVector);
        return new SequentialIterator(getStore(castVector), length);
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

    public final int getInt(SequentialIterator iter) {
        return getInt(iter.store, iter.index);
    }

    public final double getDouble(SequentialIterator iter) {
        return getDouble(iter.store, iter.index);
    }

    public final RComplex getComplex(SequentialIterator iter) {
        return getComplex(iter.store, iter.index);
    }

    public final double getComplexR(SequentialIterator iter) {
        return getComplexR(iter.store, iter.index);
    }

    public final double getComplexI(SequentialIterator iter) {
        return getComplexI(iter.store, iter.index);
    }

    public final byte getRaw(SequentialIterator iter) {
        return getRaw(iter.store, iter.index);
    }

    public final byte getLogical(SequentialIterator iter) {
        return getLogical(iter.store, iter.index);
    }

    public final String getString(SequentialIterator iter) {
        return getString(iter.store, iter.index);
    }

    public final Object getListElement(SequentialIterator iter) {
        return getListElement(iter.store, iter.index);
    }

    public final void setInt(SequentialIterator iter, int value) {
        setInt(iter.store, iter.index, value);
    }

    public final void setDouble(SequentialIterator iter, double value) {
        setDouble(iter.store, iter.index, value);
    }

    public final void setComplex(SequentialIterator iter, double real, double imaginary) {
        setComplex(iter.store, iter.index, real, imaginary);
    }

    public final void setRaw(SequentialIterator iter, byte value) {
        setRaw(iter.store, iter.index, value);
    }

    public final void setLogical(SequentialIterator iter, byte value) {
        setLogical(iter.store, iter.index, value);
    }

    public final void setString(SequentialIterator iter, String value) {
        setString(iter.store, iter.index, value);
    }

    public final void setListElement(SequentialIterator iter, Object value) {
        setListElement(iter.store, iter.index, value);
    }

    public final void setFromSameType(SequentialIterator iter, VectorAccess sourceAccess, SequentialIterator sourceIter) {
        setFromSameType(iter.store, iter.index, sourceAccess, sourceIter);
    }

    public final void setFromSameType(SequentialIterator iter, VectorAccess sourceAccess, RandomIterator sourceIter, int sourceIndex) {
        setFromSameType(iter.store, iter.index, sourceAccess, sourceIter, sourceIndex);
    }

    public final void setNA(SequentialIterator iter) {
        setNA(iter.store, iter.index);
    }

    public final boolean isNA(SequentialIterator iter) {
        return isNA(iter.store, iter.index);
    }

    /**
     * Creates a new random access on the given vector.
     */
    public final RandomIterator randomAccess(RAbstractContainer vector) {
        RAbstractContainer castVector = cast(vector);
        int length = getLength(castVector);
        RBaseNode.reportWork(this, length);
        na.enable(castVector);
        return new RandomIterator(getStore(castVector), length);
    }

    @SuppressWarnings("static-method")
    public final int getLength(RandomIterator iter) {
        return iter.length;
    }

    public final int getInt(RandomIterator iter, int index) {
        return getInt(iter.store, index);
    }

    public final double getDouble(RandomIterator iter, int index) {
        return getDouble(iter.store, index);
    }

    public final RComplex getComplex(RandomIterator iter, int index) {
        return getComplex(iter.store, index);
    }

    public final double getComplexR(RandomIterator iter, int index) {
        return getComplexR(iter.store, index);
    }

    public final double getComplexI(RandomIterator iter, int index) {
        return getComplexI(iter.store, index);
    }

    public final byte getRaw(RandomIterator iter, int index) {
        return getRaw(iter.store, index);
    }

    public final byte getLogical(RandomIterator iter, int index) {
        return getLogical(iter.store, index);
    }

    public final String getString(RandomIterator iter, int index) {
        return getString(iter.store, index);
    }

    public final Object getListElement(RandomIterator iter, int index) {
        return getListElement(iter.store, index);
    }

    public final void setInt(RandomIterator iter, int index, int value) {
        setInt(iter.store, index, value);
    }

    public final void setDouble(RandomIterator iter, int index, double value) {
        setDouble(iter.store, index, value);
    }

    public final void setComplex(RandomIterator iter, int index, double real, double imaginary) {
        setComplex(iter.store, index, real, imaginary);
    }

    public final void setRaw(RandomIterator iter, int index, byte value) {
        setRaw(iter.store, index, value);
    }

    public final void setLogical(RandomIterator iter, int index, byte value) {
        setLogical(iter.store, index, value);
    }

    public final void setString(RandomIterator iter, int index, String value) {
        setString(iter.store, index, value);
    }

    public final void setListElement(RandomIterator iter, int index, Object value) {
        setListElement(iter.store, index, value);
    }

    public final void setFromSameType(RandomIterator iter, int index, VectorAccess sourceAccess, SequentialIterator sourceIter) {
        setFromSameType(iter.store, index, sourceAccess, sourceIter);
    }

    public final void setFromSameType(RandomIterator iter, int index, VectorAccess sourceAccess, RandomIterator sourceIter, int sourceIndex) {
        setFromSameType(iter.store, index, sourceAccess, sourceIter, sourceIndex);
    }

    public final void setNA(RandomIterator iter, int index) {
        setNA(iter.store, index);
    }

    public final boolean isNA(RandomIterator iter, int index) {
        return isNA(iter.store, index);
    }

    private static final RStringVector TEMPLATE_CHARACTER = RDataFactory.getPermanent().createStringVector(4);
    private static final RComplexVector TEMPLATE_COMPLEX = RDataFactory.getPermanent().createComplexVector(4);
    private static final RDoubleVector TEMPLATE_DOUBLE = RDataFactory.getPermanent().createDoubleVector(4);
    private static final RIntVector TEMPLATE_INTEGER = RDataFactory.getPermanent().createIntVector(4);
    private static final RList TEMPLATE_LIST = RDataFactory.getPermanent().createList(4);
    private static final RExpression TEMPLATE_EXPRESSION = RDataFactory.createExpression(4);
    private static final RLogicalVector TEMPLATE_LOGICAL = RDataFactory.getPermanent().createLogicalVector(4);
    private static final RRawVector TEMPLATE_RAW = RDataFactory.getPermanent().createRawVector(4);

    public static VectorAccess createNew(RType type) {
        switch (type) {
            case Character:
                return TEMPLATE_CHARACTER.access();
            case Complex:
                return TEMPLATE_COMPLEX.access();
            case Double:
                return TEMPLATE_DOUBLE.access();
            case Integer:
                return TEMPLATE_INTEGER.access();
            case List:
                return TEMPLATE_LIST.access();
            case Expression:
                return TEMPLATE_EXPRESSION.access();
            case Logical:
                return TEMPLATE_LOGICAL.access();
            case Raw:
                return TEMPLATE_RAW.access();
            case RInteropChar:
            case RInteropFloat:
            case RInteropLong:
            case RInteropShort:
            default:
                throw RInternalError.shouldNotReachHere();
        }
    }

    public static VectorAccess createSlowPathNew(RType type) {
        switch (type) {
            case Character:
                return TEMPLATE_CHARACTER.slowPathAccess();
            case Complex:
                return TEMPLATE_COMPLEX.slowPathAccess();
            case Double:
                return TEMPLATE_DOUBLE.slowPathAccess();
            case Integer:
                return TEMPLATE_INTEGER.slowPathAccess();
            case List:
                return TEMPLATE_LIST.slowPathAccess();
            case Expression:
                return TEMPLATE_EXPRESSION.slowPathAccess();
            case Logical:
                return TEMPLATE_LOGICAL.slowPathAccess();
            case Raw:
                return TEMPLATE_RAW.slowPathAccess();
            case RInteropChar:
            case RInteropFloat:
            case RInteropLong:
            case RInteropShort:
            default:
                throw RInternalError.shouldNotReachHere();
        }
    }
}
