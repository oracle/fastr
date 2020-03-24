/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RRaw;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;

/**
 * Base classes for {@link VectorAccess} implementations that are used on the slow path, i.e., that
 * are created as singletons. For implementation reasons, most of this code is mirrored in
 * {@link FastPathVectorAccess}, so that any changes need to be mirrored there.
 */
public abstract class SlowPathVectorAccess extends VectorAccess {

    protected final BranchProfile warningReportedProfile = BranchProfile.create();

    protected SlowPathVectorAccess() {
        // VectorAccess.supports has an assertion that relies on this being Object.class
        super(null, Object.class, true);
    }

    @Override
    protected Object getStore(RAbstractContainer vector) {
        return vector;
    }

    public abstract static class SlowPathFromIntAccess extends SlowPathVectorAccess {

        @Override
        public final RType getType() {
            return RType.Integer;
        }

        @Override
        protected final double getDoubleImpl(AccessIterator accessIter, int index) {
            int value = getIntImpl(accessIter, index);
            return na.check(value) ? RRuntime.DOUBLE_NA : RRuntime.int2doubleNoCheck(value);
        }

        @Override
        protected final byte getRawImpl(AccessIterator accessIter, int index) {
            int value = getIntImpl(accessIter, index);
            byte result = (byte) value;
            if ((result & 0xff) != value) {
                warningReportedProfile.enter();
                accessIter.warning(Message.OUT_OF_RANGE);
                return 0;
            }
            return result;
        }

        @Override
        protected final byte getLogicalImpl(AccessIterator accessIter, int index) {
            int value = getIntImpl(accessIter, index);
            return na.check(value) ? RRuntime.LOGICAL_NA : RRuntime.int2logicalNoCheck(value);
        }

        @Override
        protected final RComplex getComplexImpl(AccessIterator accessIter, int index) {
            int value = getIntImpl(accessIter, index);
            return na.check(value) ? RComplex.createNA() : RRuntime.int2complexNoCheck(value);
        }

        @Override
        protected final double getComplexRImpl(AccessIterator accessIter, int index) {
            int value = getIntImpl(accessIter, index);
            return na.check(value) ? RRuntime.COMPLEX_NA_REAL_PART : value;
        }

        @Override
        protected final double getComplexIImpl(AccessIterator accessIter, int index) {
            int value = getIntImpl(accessIter, index);
            return na.check(value) ? RRuntime.COMPLEX_NA_IMAGINARY_PART : 0;
        }

        @Override
        protected final String getStringImpl(AccessIterator accessIter, int index) {
            int value = getIntImpl(accessIter, index);
            return na.check(value) ? RRuntime.STRING_NA : RRuntime.intToStringNoCheck(value);
        }

        @Override
        protected final Object getListElementImpl(AccessIterator accessIter, int index) {
            return getIntImpl(accessIter, index);
        }

        @Override
        protected final void setFromSameTypeImpl(AccessIterator accessIter, int index, VectorAccess sourceAccess, SequentialIterator sourceIter) {
            setIntImpl(accessIter, index, sourceAccess.getInt(sourceIter));
        }

        @Override
        protected final void setFromSameTypeImpl(AccessIterator accessIter, int index, VectorAccess sourceAccess, RandomIterator sourceIter, int sourceIndex) {
            setIntImpl(accessIter, index, sourceAccess.getInt(sourceIter, sourceIndex));
        }

        @Override
        protected void setNAImpl(AccessIterator accessIter, int index) {
            setIntImpl(accessIter, index, RRuntime.INT_NA);
        }

        @Override
        protected boolean isNAImpl(AccessIterator accessIter, int index) {
            return na.check(getIntImpl(accessIter, index));
        }
    }

    public abstract static class SlowPathFromDoubleAccess extends SlowPathVectorAccess {

        @Override
        public final RType getType() {
            return RType.Double;
        }

        @Override
        protected final int getIntImpl(AccessIterator accessIter, int index) {
            double value = getDoubleImpl(accessIter, index);
            na.enable(value);
            if (na.checkNAorNaN(value)) {
                return RRuntime.INT_NA;
            }
            if (value > Integer.MAX_VALUE || value <= Integer.MIN_VALUE) {
                na.enable(true);
                warningReportedProfile.enter();
                accessIter.warning(Message.NA_INTRODUCED_COERCION_INT);
                return RRuntime.INT_NA;
            }
            return (int) value;
        }

        @Override
        protected final byte getRawImpl(AccessIterator accessIter, int index) {
            int value = (int) getDoubleImpl(accessIter, index);
            byte result = (byte) value;
            if ((result & 0xff) != value) {
                warningReportedProfile.enter();
                accessIter.warning(Message.OUT_OF_RANGE);
                return 0;
            }
            return result;
        }

        @Override
        protected final byte getLogicalImpl(AccessIterator accessIter, int index) {
            double value = getDoubleImpl(accessIter, index);
            return na.check(value) ? RRuntime.LOGICAL_NA : RRuntime.double2logicalNoCheck(value);
        }

        @Override
        protected final RComplex getComplexImpl(AccessIterator accessIter, int index) {
            double value = getDoubleImpl(accessIter, index);
            return na.check(value) ? RComplex.createNA() : RRuntime.double2complexNoCheck(value);
        }

        @Override
        protected final double getComplexRImpl(AccessIterator accessIter, int index) {
            double value = getDoubleImpl(accessIter, index);
            return na.check(value) ? RRuntime.COMPLEX_NA_REAL_PART : value;
        }

        @Override
        protected final double getComplexIImpl(AccessIterator accessIter, int index) {
            double value = getDoubleImpl(accessIter, index);
            return na.check(value) ? RRuntime.COMPLEX_NA_IMAGINARY_PART : 0;
        }

        @Override
        protected final String getStringImpl(AccessIterator accessIter, int index) {
            double value = getDoubleImpl(accessIter, index);
            return na.check(value) ? RRuntime.STRING_NA : RContext.getRRuntimeASTAccess().encodeDouble(value);
        }

        @Override
        protected final Object getListElementImpl(AccessIterator accessIter, int index) {
            return getDoubleImpl(accessIter, index);
        }

        @Override
        protected final void setFromSameTypeImpl(AccessIterator accessIter, int index, VectorAccess sourceAccess, SequentialIterator sourceIter) {
            setDoubleImpl(accessIter, index, sourceAccess.getDouble(sourceIter));
        }

        @Override
        protected final void setFromSameTypeImpl(AccessIterator accessIter, int index, VectorAccess sourceAccess, RandomIterator sourceIter, int sourceIndex) {
            setDoubleImpl(accessIter, index, sourceAccess.getDouble(sourceIter, sourceIndex));
        }

        @Override
        protected void setNAImpl(AccessIterator accessIter, int index) {
            setDoubleImpl(accessIter, index, RRuntime.DOUBLE_NA);
        }

        @Override
        protected boolean isNAImpl(AccessIterator accessIter, int index) {
            return na.check(getDoubleImpl(accessIter, index));
        }
    }

    public abstract static class SlowPathFromLogicalAccess extends SlowPathVectorAccess {

        @Override
        public final RType getType() {
            return RType.Logical;
        }

        @Override
        protected final int getIntImpl(AccessIterator accessIter, int index) {
            byte value = getLogicalImpl(accessIter, index);
            return na.check(value) ? RRuntime.INT_NA : RRuntime.logical2intNoCheck(value);
        }

        @Override
        protected final double getDoubleImpl(AccessIterator accessIter, int index) {
            byte value = getLogicalImpl(accessIter, index);
            return na.check(value) ? RRuntime.DOUBLE_NA : RRuntime.logical2doubleNoCheck(value);
        }

        @Override
        protected final byte getRawImpl(AccessIterator accessIter, int index) {
            byte value = getLogicalImpl(accessIter, index);
            if (na.check(value)) {
                warningReportedProfile.enter();
                accessIter.warning(Message.OUT_OF_RANGE);
                return 0;
            }
            return value;
        }

        @Override
        protected final RComplex getComplexImpl(AccessIterator accessIter, int index) {
            byte value = getLogicalImpl(accessIter, index);
            return na.check(value) ? RComplex.createNA() : RRuntime.logical2complexNoCheck(value);
        }

        @Override
        protected final double getComplexRImpl(AccessIterator accessIter, int index) {
            byte value = getLogicalImpl(accessIter, index);
            return na.check(value) ? RRuntime.COMPLEX_NA_REAL_PART : value;
        }

        @Override
        protected final double getComplexIImpl(AccessIterator accessIter, int index) {
            byte value = getLogicalImpl(accessIter, index);
            return na.check(value) ? RRuntime.COMPLEX_NA_IMAGINARY_PART : 0;
        }

        @Override
        protected final String getStringImpl(AccessIterator accessIter, int index) {
            byte value = getLogicalImpl(accessIter, index);
            return na.check(value) ? RRuntime.STRING_NA : RRuntime.logicalToStringNoCheck(value);
        }

        @Override
        protected final Object getListElementImpl(AccessIterator accessIter, int index) {
            return getLogicalImpl(accessIter, index);
        }

        @Override
        protected final void setFromSameTypeImpl(AccessIterator accessIter, int index, VectorAccess sourceAccess, SequentialIterator sourceIter) {
            setLogicalImpl(accessIter, index, sourceAccess.getLogical(sourceIter));
        }

        @Override
        protected final void setFromSameTypeImpl(AccessIterator accessIter, int index, VectorAccess sourceAccess, RandomIterator sourceIter, int sourceIndex) {
            setLogicalImpl(accessIter, index, sourceAccess.getLogical(sourceIter, sourceIndex));
        }

        @Override
        protected void setNAImpl(AccessIterator accessIter, int index) {
            setLogicalImpl(accessIter, index, RRuntime.LOGICAL_NA);
        }

        @Override
        protected boolean isNAImpl(AccessIterator accessIter, int index) {
            return na.check(getLogicalImpl(accessIter, index));
        }
    }

    public abstract static class SlowPathFromRawAccess extends SlowPathVectorAccess {

        @Override
        public final RType getType() {
            return RType.Raw;
        }

        @Override
        protected final int getIntImpl(AccessIterator accessIter, int index) {
            return getRawImpl(accessIter, index) & 0xff;
        }

        @Override
        protected final double getDoubleImpl(AccessIterator accessIter, int index) {
            return getRawImpl(accessIter, index) & 0xff;
        }

        @Override
        protected final byte getLogicalImpl(AccessIterator accessIter, int index) {
            return getRawImpl(accessIter, index) == 0 ? RRuntime.LOGICAL_FALSE : RRuntime.LOGICAL_TRUE;
        }

        @Override
        protected final RComplex getComplexImpl(AccessIterator accessIter, int index) {
            return RComplex.valueOf(getRawImpl(accessIter, index) & 0xff, 0);
        }

        @Override
        protected final double getComplexRImpl(AccessIterator accessIter, int index) {
            return getRawImpl(accessIter, index) & 0xff;
        }

        @Override
        protected final double getComplexIImpl(AccessIterator accessIter, int index) {
            return 0;
        }

        @Override
        protected final String getStringImpl(AccessIterator accessIter, int index) {
            return RRuntime.rawToHexString(getRawImpl(accessIter, index));
        }

        @Override
        protected final Object getListElementImpl(AccessIterator accessIter, int index) {
            return RRaw.valueOf(getRawImpl(accessIter, index));
        }

        @Override
        protected final void setFromSameTypeImpl(AccessIterator accessIter, int index, VectorAccess sourceAccess, SequentialIterator sourceIter) {
            setRawImpl(accessIter, index, sourceAccess.getRaw(sourceIter));
        }

        @Override
        protected final void setFromSameTypeImpl(AccessIterator accessIter, int index, VectorAccess sourceAccess, RandomIterator sourceIter, int sourceIndex) {
            setRawImpl(accessIter, index, sourceAccess.getRaw(sourceIter, sourceIndex));
        }

        @Override
        protected void setNAImpl(AccessIterator accessIter, int index) {
            /*
             * There is no raw NA, but places that write NA for other types usually write 0 for raw.
             */
            setRawImpl(accessIter, index, (byte) 0);
        }

        @Override
        protected boolean isNAImpl(AccessIterator accessIter, int index) {
            return false;
        }
    }

    public abstract static class SlowPathFromComplexAccess extends SlowPathVectorAccess {

        @Override
        public final RType getType() {
            return RType.Complex;
        }

        @Override
        protected final int getIntImpl(AccessIterator accessIter, int index) {
            double value = getComplexRImpl(accessIter, index);
            if (Double.isNaN(value)) {
                na.enable(true);
                return RRuntime.INT_NA;
            }
            if (value > Integer.MAX_VALUE || value <= Integer.MIN_VALUE) {
                na.enable(true);
                warningReportedProfile.enter();
                accessIter.warning(Message.NA_INTRODUCED_COERCION_INT);
                return RRuntime.INT_NA;
            }
            if (getComplexIImpl(accessIter, index) != 0) {
                warningReportedProfile.enter();
                accessIter.warning(Message.IMAGINARY_PARTS_DISCARDED_IN_COERCION);
            }
            return (int) value;
        }

        @Override
        protected final double getDoubleImpl(AccessIterator accessIter, int index) {
            double value = getComplexRImpl(accessIter, index);
            if (Double.isNaN(value)) {
                na.enable(true);
                return RRuntime.DOUBLE_NA;
            }
            if (getComplexIImpl(accessIter, index) != 0) {
                warningReportedProfile.enter();
                accessIter.warning(Message.IMAGINARY_PARTS_DISCARDED_IN_COERCION);
            }
            return value;
        }

        @Override
        protected final byte getRawImpl(AccessIterator accessIter, int index) {
            RComplex value = getComplexImpl(accessIter, index);

            double realPart = value.getRealPart();
            double realResult = realPart;

            if (realPart > Integer.MAX_VALUE || realPart <= Integer.MIN_VALUE) {
                warningReportedProfile.enter();
                accessIter.warning(Message.NA_INTRODUCED_COERCION_INT);
                realResult = 0;
            }

            if (getComplexIImpl(accessIter, index) != 0) {
                warningReportedProfile.enter();
                accessIter.warning(Message.IMAGINARY_PARTS_DISCARDED_IN_COERCION);
            }

            if (Double.isNaN(realPart) || realPart < 0 || realPart >= 256) {
                warningReportedProfile.enter();
                accessIter.warning(Message.OUT_OF_RANGE);
                realResult = 0;
            }
            return (byte) RRuntime.double2rawIntValue(realResult);
        }

        @Override
        protected final byte getLogicalImpl(AccessIterator accessIter, int index) {
            RComplex value = getComplexImpl(accessIter, index);
            return na.check(value) ? RRuntime.LOGICAL_NA : RRuntime.complex2logicalNoCheck(value);
        }

        @Override
        protected final String getStringImpl(AccessIterator accessIter, int index) {
            RComplex value = getComplexImpl(accessIter, index);
            return na.check(value) ? RRuntime.STRING_NA : RContext.getRRuntimeASTAccess().encodeComplex(value);
        }

        @Override
        protected final Object getListElementImpl(AccessIterator accessIter, int index) {
            return getComplexImpl(accessIter, index);
        }

        @Override
        protected final void setFromSameTypeImpl(AccessIterator accessIter, int index, VectorAccess sourceAccess, SequentialIterator sourceIter) {
            setComplexImpl(accessIter, index, sourceAccess.getComplexR(sourceIter), sourceAccess.getComplexI(sourceIter));
        }

        @Override
        protected final void setFromSameTypeImpl(AccessIterator accessIter, int index, VectorAccess sourceAccess, RandomIterator sourceIter, int sourceIndex) {
            setComplexImpl(accessIter, index, sourceAccess.getComplexR(sourceIter, sourceIndex), sourceAccess.getComplexI(sourceIter, sourceIndex));
        }

        @Override
        protected void setNAImpl(AccessIterator accessIter, int index) {
            setComplexImpl(accessIter, index, RRuntime.COMPLEX_NA_REAL_PART, RRuntime.COMPLEX_NA_IMAGINARY_PART);
        }

        @Override
        protected boolean isNAImpl(AccessIterator accessIter, int index) {
            return na.check(getComplexRImpl(accessIter, index), getComplexIImpl(accessIter, index));
        }
    }

    public abstract static class SlowPathFromStringAccess extends SlowPathVectorAccess {

        @Override
        public final RType getType() {
            return RType.Character;
        }

        @Override
        protected final int getIntImpl(AccessIterator accessIter, int index) {
            String str = getStringImpl(accessIter, index);
            if (na.check(str) || str.isEmpty()) {
                na.enable(true);
                return RRuntime.INT_NA;
            }
            double d = na.convertStringToDouble(str);
            na.enable(d);
            if (na.checkNAorNaN(d)) {
                if (na.check(d)) {
                    warningReportedProfile.enter();
                    accessIter.warning(Message.NA_INTRODUCED_COERCION);
                    return RRuntime.INT_NA;
                }
                return RRuntime.INT_NA;
            }
            int value = na.convertDoubleToInt(d);
            na.enable(value);
            if (na.check(value)) {
                warningReportedProfile.enter();
                accessIter.warning(Message.NA_INTRODUCED_COERCION_INT);
                return RRuntime.INT_NA;
            }
            return value;
        }

        @Override
        protected final double getDoubleImpl(AccessIterator accessIter, int index) {
            String str = getStringImpl(accessIter, index);
            if (na.check(str) || str.isEmpty()) {
                na.enable(true);
                return RRuntime.DOUBLE_NA;
            }
            double value = na.convertStringToDouble(str);
            if (RRuntime.isNA(value)) {
                na.enable(true);
                warningReportedProfile.enter();
                accessIter.warning(Message.NA_INTRODUCED_COERCION);
                return RRuntime.DOUBLE_NA;
            }
            return value;
        }

        @Override
        protected final byte getRawImpl(AccessIterator accessIter, int index) {
            String value = getStringImpl(accessIter, index);
            int intValue;
            if (na.check(value) || value.isEmpty()) {
                intValue = RRuntime.INT_NA;
            } else {
                double d = na.convertStringToDouble(value);
                na.enable(d);
                if (na.checkNAorNaN(d)) {
                    if (na.check(d) && !value.isEmpty()) {
                        warningReportedProfile.enter();
                        accessIter.warning(Message.NA_INTRODUCED_COERCION);
                    }
                    intValue = RRuntime.INT_NA;
                } else {
                    intValue = na.convertDoubleToInt(d);
                    na.enable(intValue);
                    if (na.check(intValue) && !value.isEmpty()) {
                        warningReportedProfile.enter();
                        accessIter.warning(Message.NA_INTRODUCED_COERCION_INT);
                    }
                }
                int intRawValue = RRuntime.int2rawIntValue(intValue);
                if (intValue != intRawValue) {
                    warningReportedProfile.enter();
                    accessIter.warning(Message.OUT_OF_RANGE);
                    intValue = 0;
                }
            }
            return intValue >= 0 && intValue <= 255 ? (byte) intValue : 0;
        }

        @Override
        protected final byte getLogicalImpl(AccessIterator accessIter, int index) {
            return na.convertStringToLogical(getStringImpl(accessIter, index));
        }

        @Override
        protected final RComplex getComplexImpl(AccessIterator accessIter, int index) {
            String value = getStringImpl(accessIter, index);
            RComplex complexValue;
            if (na.check(value) || value.isEmpty()) {
                complexValue = RComplex.createNA();
            } else {
                complexValue = RRuntime.string2complexNoCheck(value);
                if (complexValue.isNA()) {
                    warningReportedProfile.enter();
                    na.enable(true);
                    accessIter.warning(Message.NA_INTRODUCED_COERCION);
                }
            }
            return complexValue;
        }

        @Override
        protected final double getComplexRImpl(AccessIterator accessIter, int index) {
            return na.convertStringToComplex(getStringImpl(accessIter, index)).getRealPart();
        }

        @Override
        protected final double getComplexIImpl(AccessIterator accessIter, int index) {
            return na.convertStringToComplex(getStringImpl(accessIter, index)).getImaginaryPart();
        }

        @Override
        protected final Object getListElementImpl(AccessIterator accessIter, int index) {
            return getStringImpl(accessIter, index);
        }

        @Override
        protected final void setFromSameTypeImpl(AccessIterator accessIter, int index, VectorAccess sourceAccess, SequentialIterator sourceIter) {
            setStringImpl(accessIter, index, sourceAccess.getString(sourceIter));
        }

        @Override
        protected final void setFromSameTypeImpl(AccessIterator accessIter, int index, VectorAccess sourceAccess, RandomIterator sourceIter, int sourceIndex) {
            setStringImpl(accessIter, index, sourceAccess.getString(sourceIter, sourceIndex));
        }

        @Override
        protected void setNAImpl(AccessIterator accessIter, int index) {
            setStringImpl(accessIter, index, RRuntime.STRING_NA);
        }

        @Override
        protected boolean isNAImpl(AccessIterator accessIter, int index) {
            return na.check(getStringImpl(accessIter, index));
        }
    }

    public abstract static class SlowPathFromListAccess extends SlowPathVectorAccess {

        @Override
        protected final int getIntImpl(AccessIterator accessIter, int index) {
            throw RInternalError.shouldNotReachHere();
        }

        @Override
        protected final double getDoubleImpl(AccessIterator accessIter, int index) {
            throw RInternalError.shouldNotReachHere();
        }

        @Override
        protected final byte getRawImpl(AccessIterator accessIter, int index) {
            throw RInternalError.shouldNotReachHere();
        }

        @Override
        protected final byte getLogicalImpl(AccessIterator accessIter, int index) {
            throw RInternalError.shouldNotReachHere();
        }

        @Override
        protected final RComplex getComplexImpl(AccessIterator accessIter, int index) {
            throw RInternalError.shouldNotReachHere();
        }

        @Override
        protected final double getComplexRImpl(AccessIterator accessIter, int index) {
            throw RInternalError.shouldNotReachHere();
        }

        @Override
        protected final double getComplexIImpl(AccessIterator accessIter, int index) {
            throw RInternalError.shouldNotReachHere();
        }

        @Override
        protected final String getStringImpl(AccessIterator accessIter, int index) {
            throw RInternalError.shouldNotReachHere();
        }

        @Override
        protected final void setFromSameTypeImpl(AccessIterator accessIter, int index, VectorAccess sourceAccess, SequentialIterator sourceIter) {
            setListElementImpl(accessIter, index, sourceAccess.getListElement(sourceIter));
        }

        @Override
        protected final void setFromSameTypeImpl(AccessIterator accessIter, int index, VectorAccess sourceAccess, RandomIterator sourceIter, int sourceIndex) {
            setListElementImpl(accessIter, index, sourceAccess.getListElement(sourceIter, sourceIndex));
        }

        @Override
        protected void setNAImpl(AccessIterator accessIter, int index) {
            /*
             * There is no list NA, but places that write NA for other types usually write NULL for
             * lists.
             */
            setListElementImpl(accessIter, index, RNull.instance);
        }

        @Override
        protected boolean isNAImpl(AccessIterator accessIter, int index) {
            return na.checkListElement(getListElementImpl(accessIter, index));
        }
    }
}
