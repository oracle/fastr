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
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;

/**
 * Base classes for {@link VectorAccess} implementations that are used on the slow path, i.e., that
 * are created as singletons. For implementation reasons, most of this code is mirrored in
 * {@link FastPathVectorAccess}, so that any changes need to be mirrored there.
 */
public abstract class SlowPathVectorAccess extends VectorAccess {

    protected boolean naReported; // TODO: move this into the iterator

    protected SlowPathVectorAccess() {
        // VectorAccess.supports has an assertion that relies on this being RAbstractContainer.class
        super(RAbstractContainer.class, true);
    }

    @Override
    protected final Object getStore(RAbstractContainer vector) {
        return vector;
    }

    protected final void warning(RError.Message message) {
        CompilerAsserts.neverPartOfCompilation();
        if (!naReported) {
            RError.warning(RError.SHOW_CALLER, message);
            naReported = true;
        }
    }

    public abstract static class SlowPathFromIntAccess extends SlowPathVectorAccess {

        @Override
        public final RType getType() {
            return RType.Integer;
        }

        @Override
        protected final double getDouble(Object store, int index) {
            int value = getInt(store, index);
            return na.check(value) ? RRuntime.DOUBLE_NA : RRuntime.int2doubleNoCheck(value);
        }

        @Override
        protected final byte getRaw(Object store, int index) {
            int value = getInt(store, index);
            byte result = (byte) value;
            if ((result & 0xff) != value) {
                warning(Message.OUT_OF_RANGE);
                return 0;
            }
            return result;
        }

        @Override
        protected final byte getLogical(Object store, int index) {
            int value = getInt(store, index);
            return na.check(value) ? RRuntime.LOGICAL_NA : RRuntime.int2logicalNoCheck(value);
        }

        @Override
        protected final RComplex getComplex(Object store, int index) {
            int value = getInt(store, index);
            return na.check(value) ? RComplex.createNA() : RRuntime.int2complexNoCheck(value);
        }

        @Override
        protected final double getComplexR(Object store, int index) {
            int value = getInt(store, index);
            return na.check(value) ? RRuntime.COMPLEX_NA_REAL_PART : value;
        }

        @Override
        protected final double getComplexI(Object store, int index) {
            int value = getInt(store, index);
            return na.check(value) ? RRuntime.COMPLEX_NA_IMAGINARY_PART : 0;
        }

        @Override
        protected final String getString(Object store, int index) {
            int value = getInt(store, index);
            return na.check(value) ? RRuntime.STRING_NA : RRuntime.intToStringNoCheck(value);
        }

        @Override
        protected final Object getListElement(Object store, int index) {
            return getInt(store, index);
        }

        @Override
        protected final void setFromSameType(Object store, int index, VectorAccess sourceAccess, SequentialIterator sourceIter) {
            setInt(store, index, sourceAccess.getInt(sourceIter));
        }

        @Override
        protected final void setFromSameType(Object store, int index, VectorAccess sourceAccess, RandomIterator sourceIter, int sourceIndex) {
            setInt(store, index, sourceAccess.getInt(sourceIter, sourceIndex));
        }

        @Override
        protected void setNA(Object store, int index) {
            setInt(store, index, RRuntime.INT_NA);
        }

        @Override
        protected boolean isNA(Object store, int index) {
            return na.check(getInt(store, index));
        }
    }

    public abstract static class SlowPathFromDoubleAccess extends SlowPathVectorAccess {

        @Override
        public final RType getType() {
            return RType.Double;
        }

        @Override
        protected final int getInt(Object store, int index) {
            double value = getDouble(store, index);
            if (Double.isNaN(value)) {
                na.enable(true);
                return RRuntime.INT_NA;
            }
            if (value > Integer.MAX_VALUE || value <= Integer.MIN_VALUE) {
                na.enable(true);
                warning(Message.NA_INTRODUCED_COERCION_INT);
                return RRuntime.INT_NA;
            }
            return (int) value;
        }

        @Override
        protected final byte getRaw(Object store, int index) {
            int value = (int) getDouble(store, index);
            byte result = (byte) value;
            if ((result & 0xff) != value) {
                warning(Message.OUT_OF_RANGE);
                return 0;
            }
            return result;
        }

        @Override
        protected final byte getLogical(Object store, int index) {
            double value = getDouble(store, index);
            return na.check(value) ? RRuntime.LOGICAL_NA : RRuntime.double2logicalNoCheck(value);
        }

        @Override
        protected final RComplex getComplex(Object store, int index) {
            double value = getDouble(store, index);
            return na.check(value) ? RComplex.createNA() : RRuntime.double2complexNoCheck(value);
        }

        @Override
        protected final double getComplexR(Object store, int index) {
            double value = getDouble(store, index);
            return na.check(value) ? RRuntime.COMPLEX_NA_REAL_PART : value;
        }

        @Override
        protected final double getComplexI(Object store, int index) {
            double value = getDouble(store, index);
            return na.check(value) ? RRuntime.COMPLEX_NA_IMAGINARY_PART : 0;
        }

        @Override
        protected final String getString(Object store, int index) {
            double value = getDouble(store, index);
            return na.check(value) ? RRuntime.STRING_NA : RContext.getRRuntimeASTAccess().encodeDouble(value);
        }

        @Override
        protected final Object getListElement(Object store, int index) {
            return getDouble(store, index);
        }

        @Override
        protected final void setFromSameType(Object store, int index, VectorAccess sourceAccess, SequentialIterator sourceIter) {
            setDouble(store, index, sourceAccess.getDouble(sourceIter));
        }

        @Override
        protected final void setFromSameType(Object store, int index, VectorAccess sourceAccess, RandomIterator sourceIter, int sourceIndex) {
            setDouble(store, index, sourceAccess.getDouble(sourceIter, sourceIndex));
        }

        @Override
        protected void setNA(Object store, int index) {
            setDouble(store, index, RRuntime.DOUBLE_NA);
        }

        @Override
        protected boolean isNA(Object store, int index) {
            return na.check(getDouble(store, index));
        }
    }

    public abstract static class SlowPathFromLogicalAccess extends SlowPathVectorAccess {

        @Override
        public final RType getType() {
            return RType.Logical;
        }

        @Override
        protected final int getInt(Object store, int index) {
            byte value = getLogical(store, index);
            return na.check(value) ? RRuntime.INT_NA : RRuntime.logical2intNoCheck(value);
        }

        @Override
        protected final double getDouble(Object store, int index) {
            byte value = getLogical(store, index);
            return na.check(value) ? RRuntime.DOUBLE_NA : RRuntime.logical2doubleNoCheck(value);
        }

        @Override
        protected final byte getRaw(Object store, int index) {
            byte value = getLogical(store, index);
            if (na.check(value)) {
                warning(Message.OUT_OF_RANGE);
                return 0;
            }
            return value;
        }

        @Override
        protected final RComplex getComplex(Object store, int index) {
            byte value = getLogical(store, index);
            return na.check(value) ? RComplex.createNA() : RRuntime.logical2complexNoCheck(value);
        }

        @Override
        protected final double getComplexR(Object store, int index) {
            byte value = getLogical(store, index);
            return na.check(value) ? RRuntime.COMPLEX_NA_REAL_PART : value;
        }

        @Override
        protected final double getComplexI(Object store, int index) {
            byte value = getLogical(store, index);
            return na.check(value) ? RRuntime.COMPLEX_NA_IMAGINARY_PART : 0;
        }

        @Override
        protected final String getString(Object store, int index) {
            byte value = getLogical(store, index);
            return na.check(value) ? RRuntime.STRING_NA : RRuntime.logicalToStringNoCheck(value);
        }

        @Override
        protected final Object getListElement(Object store, int index) {
            return getLogical(store, index);
        }

        @Override
        protected final void setFromSameType(Object store, int index, VectorAccess sourceAccess, SequentialIterator sourceIter) {
            setLogical(store, index, sourceAccess.getLogical(sourceIter));
        }

        @Override
        protected final void setFromSameType(Object store, int index, VectorAccess sourceAccess, RandomIterator sourceIter, int sourceIndex) {
            setLogical(store, index, sourceAccess.getLogical(sourceIter, sourceIndex));
        }

        @Override
        protected void setNA(Object store, int index) {
            setLogical(store, index, RRuntime.LOGICAL_NA);
        }

        @Override
        protected boolean isNA(Object store, int index) {
            return na.check(getLogical(store, index));
        }
    }

    public abstract static class SlowPathFromRawAccess extends SlowPathVectorAccess {

        @Override
        public final RType getType() {
            return RType.Raw;
        }

        @Override
        protected final int getInt(Object store, int index) {
            return getRaw(store, index) & 0xff;
        }

        @Override
        protected final double getDouble(Object store, int index) {
            return getRaw(store, index) & 0xff;
        }

        @Override
        protected final byte getLogical(Object store, int index) {
            return getRaw(store, index) == 0 ? RRuntime.LOGICAL_FALSE : RRuntime.LOGICAL_TRUE;
        }

        @Override
        protected final RComplex getComplex(Object store, int index) {
            return RComplex.valueOf(getRaw(store, index) & 0xff, 0);
        }

        @Override
        protected final double getComplexR(Object store, int index) {
            return getRaw(store, index) & 0xff;
        }

        @Override
        protected final double getComplexI(Object store, int index) {
            return 0;
        }

        @Override
        protected final String getString(Object store, int index) {
            return RRuntime.rawToHexString(getRaw(store, index));
        }

        @Override
        protected final Object getListElement(Object store, int index) {
            return getRaw(store, index);
        }

        @Override
        protected final void setFromSameType(Object store, int index, VectorAccess sourceAccess, SequentialIterator sourceIter) {
            setRaw(store, index, sourceAccess.getRaw(sourceIter));
        }

        @Override
        protected final void setFromSameType(Object store, int index, VectorAccess sourceAccess, RandomIterator sourceIter, int sourceIndex) {
            setRaw(store, index, sourceAccess.getRaw(sourceIter, sourceIndex));
        }

        @Override
        protected void setNA(Object store, int index) {
            /*
             * There is no raw NA, but places that write NA for other types usually write 0 for raw.
             */
            setRaw(store, index, (byte) 0);
        }

        @Override
        protected boolean isNA(Object store, int index) {
            return false;
        }
    }

    public abstract static class SlowPathFromComplexAccess extends SlowPathVectorAccess {

        @Override
        public final RType getType() {
            return RType.Complex;
        }

        @Override
        protected final int getInt(Object store, int index) {
            double value = getComplexR(store, index);
            if (Double.isNaN(value)) {
                na.enable(true);
                return RRuntime.INT_NA;
            }
            if (value > Integer.MAX_VALUE || value <= Integer.MIN_VALUE) {
                na.enable(true);
                warning(Message.NA_INTRODUCED_COERCION_INT);
                return RRuntime.INT_NA;
            }
            if (getComplexI(store, index) != 0) {
                warning(Message.IMAGINARY_PARTS_DISCARDED_IN_COERCION);
            }
            return (int) value;
        }

        @Override
        protected final double getDouble(Object store, int index) {
            double value = getComplexR(store, index);
            if (Double.isNaN(value)) {
                na.enable(true);
                return RRuntime.DOUBLE_NA;
            }
            if (getComplexI(store, index) != 0) {
                warning(Message.IMAGINARY_PARTS_DISCARDED_IN_COERCION);
            }
            return value;
        }

        @Override
        protected final byte getRaw(Object store, int index) {
            double value = getComplexR(store, index);
            if (Double.isNaN(value) || value < 0 || value >= 256) {
                warning(Message.OUT_OF_RANGE);
                return 0;
            }
            if (getComplexI(store, index) != 0) {
                warning(Message.IMAGINARY_PARTS_DISCARDED_IN_COERCION);
            }
            return (byte) value;
        }

        @Override
        protected final byte getLogical(Object store, int index) {
            RComplex value = getComplex(store, index);
            return na.check(value) ? RRuntime.LOGICAL_NA : RRuntime.complex2logicalNoCheck(value);
        }

        @Override
        protected final String getString(Object store, int index) {
            RComplex value = getComplex(store, index);
            return na.check(value) ? RRuntime.STRING_NA : RContext.getRRuntimeASTAccess().encodeComplex(value);
        }

        @Override
        protected final Object getListElement(Object store, int index) {
            return getComplex(store, index);
        }

        @Override
        protected final void setFromSameType(Object store, int index, VectorAccess sourceAccess, SequentialIterator sourceIter) {
            setComplex(store, index, sourceAccess.getComplexR(sourceIter), sourceAccess.getComplexI(sourceIter));
        }

        @Override
        protected final void setFromSameType(Object store, int index, VectorAccess sourceAccess, RandomIterator sourceIter, int sourceIndex) {
            setComplex(store, index, sourceAccess.getComplexR(sourceIter, sourceIndex), sourceAccess.getComplexI(sourceIter, sourceIndex));
        }

        @Override
        protected void setNA(Object store, int index) {
            setComplex(store, index, RRuntime.COMPLEX_NA_REAL_PART, RRuntime.COMPLEX_NA_IMAGINARY_PART);
        }

        @Override
        protected boolean isNA(Object store, int index) {
            return na.check(getComplexR(store, index), getComplexI(store, index));
        }
    }

    public abstract static class SlowPathFromStringAccess extends SlowPathVectorAccess {

        @Override
        public final RType getType() {
            return RType.Character;
        }

        @Override
        protected final int getInt(Object store, int index) {
            return na.convertStringToInt(getString(store, index));
        }

        @Override
        protected final double getDouble(Object store, int index) {
            return na.convertStringToDouble(getString(store, index));
        }

        @Override
        protected final byte getRaw(Object store, int index) {
            int value = na.convertStringToInt(getString(store, index));
            return value >= 0 && value <= 255 ? (byte) value : 0;
        }

        @Override
        protected final byte getLogical(Object store, int index) {
            return na.convertStringToLogical(getString(store, index));
        }

        @Override
        protected final RComplex getComplex(Object store, int index) {
            return na.convertStringToComplex(getString(store, index));
        }

        @Override
        protected final double getComplexR(Object store, int index) {
            return na.convertStringToComplex(getString(store, index)).getRealPart();
        }

        @Override
        protected final double getComplexI(Object store, int index) {
            return na.convertStringToComplex(getString(store, index)).getImaginaryPart();
        }

        @Override
        protected final Object getListElement(Object store, int index) {
            return getString(store, index);
        }

        @Override
        protected final void setFromSameType(Object store, int index, VectorAccess sourceAccess, SequentialIterator sourceIter) {
            setString(store, index, sourceAccess.getString(sourceIter));
        }

        @Override
        protected final void setFromSameType(Object store, int index, VectorAccess sourceAccess, RandomIterator sourceIter, int sourceIndex) {
            setString(store, index, sourceAccess.getString(sourceIter, sourceIndex));
        }

        @Override
        protected void setNA(Object store, int index) {
            setString(store, index, RRuntime.STRING_NA);
        }

        @Override
        protected boolean isNA(Object store, int index) {
            return na.check(getString(store, index));
        }
    }

    public abstract static class SlowPathFromListAccess extends SlowPathVectorAccess {

        @Override
        protected final int getInt(Object store, int index) {
            throw RInternalError.shouldNotReachHere();
        }

        @Override
        protected final double getDouble(Object store, int index) {
            throw RInternalError.shouldNotReachHere();
        }

        @Override
        protected final byte getRaw(Object store, int index) {
            throw RInternalError.shouldNotReachHere();
        }

        @Override
        protected final byte getLogical(Object store, int index) {
            throw RInternalError.shouldNotReachHere();
        }

        @Override
        protected final RComplex getComplex(Object store, int index) {
            throw RInternalError.shouldNotReachHere();
        }

        @Override
        protected final double getComplexR(Object store, int index) {
            throw RInternalError.shouldNotReachHere();
        }

        @Override
        protected final double getComplexI(Object store, int index) {
            throw RInternalError.shouldNotReachHere();
        }

        @Override
        protected final String getString(Object store, int index) {
            throw RInternalError.shouldNotReachHere();
        }

        @Override
        protected final void setFromSameType(Object store, int index, VectorAccess sourceAccess, SequentialIterator sourceIter) {
            setListElement(store, index, sourceAccess.getListElement(sourceIter));
        }

        @Override
        protected final void setFromSameType(Object store, int index, VectorAccess sourceAccess, RandomIterator sourceIter, int sourceIndex) {
            setListElement(store, index, sourceAccess.getListElement(sourceIter, sourceIndex));
        }

        @Override
        protected void setNA(Object store, int index) {
            /*
             * There is no list NA, but places that write NA for other types usually write NULL for
             * lists.
             */
            setListElement(store, index, RNull.instance);
        }

        @Override
        protected boolean isNA(Object store, int index) {
            return na.checkListElement(getListElement(store, index));
        }
    }
}
