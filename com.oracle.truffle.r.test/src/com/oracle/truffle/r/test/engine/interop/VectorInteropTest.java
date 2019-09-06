/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.test.engine.interop;

import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RBaseObject;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RComplexVector;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RForeignBooleanWrapper;
import com.oracle.truffle.r.runtime.data.RForeignDoubleWrapper;
import com.oracle.truffle.r.runtime.data.RForeignIntWrapper;
import com.oracle.truffle.r.runtime.data.RForeignStringWrapper;
import com.oracle.truffle.r.runtime.data.RInteropComplex;
import com.oracle.truffle.r.runtime.data.closures.RClosures;
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractRawVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import static org.junit.Assert.fail;

public class VectorInteropTest extends AbstractInteropTest {

    @Override
    protected boolean canRead(@SuppressWarnings("unused") TruffleObject obj) {
        return true;
    }

    @Test
    public void testRead() throws Exception {
        final TruffleObject vi = RDataFactory.createIntSequence(1, 1, 10);
        assertEquals(3, getInterop().readArrayElement(vi, 2));
        assertEquals(3, getInterop().readArrayElement(vi, 2L));

        assertInteropException(() -> getInterop().readMember(vi, "a"), UnsupportedMessageException.class);
        assertInteropException(() -> getInterop().readArrayElement(vi, -1), InvalidArrayIndexException.class);
        assertInteropException(() -> getInterop().readArrayElement(vi, 100), InvalidArrayIndexException.class);

        TruffleObject vd = RDataFactory.createDoubleSequence(1.1, 1, 10);
        assertEquals(1.1, getInterop().readArrayElement(vd, 0));

        TruffleObject vb = RDataFactory.createLogicalVector(new byte[]{1, 0, 1}, true);
        assertEquals(true, getInterop().readArrayElement(vb, 0));
    }

    @Test
    public void testReadComplexMember() throws Exception {
        // scalar

        testReadComplexScalar(RDataFactory.createComplexVector(new double[]{1, 2}, true));
        testReadComplexScalar(RComplex.valueOf(1, 2));

        // scalar NA
        RComplexVector complexScalar = RDataFactory.createComplexVector(new double[]{RRuntime.COMPLEX_NA_REAL_PART, 1}, false);
        assertFalse(getInterop().hasMembers(complexScalar));
        assertTrue(getInterop().isNull(complexScalar));
        complexScalar = RDataFactory.createComplexVector(new double[]{1, RRuntime.COMPLEX_NA_IMAGINARY_PART}, false);
        assertFalse(getInterop().hasMembers(complexScalar));
        assertTrue(getInterop().isNull(complexScalar));
        complexScalar = RDataFactory.createComplexVector(new double[]{RRuntime.COMPLEX_NA_REAL_PART, RRuntime.COMPLEX_NA_IMAGINARY_PART}, false);
        assertFalse(getInterop().hasMembers(complexScalar));
        assertTrue(getInterop().isNull(complexScalar));

        // non scalar
        RComplexVector complexVector = RDataFactory.createComplexVector(new double[]{1, 2, 3, 4}, true);
        assertFalse(getInterop().hasMembers(complexVector));

        assertEquals(3.0, getInterop().readMember(getInterop().readArrayElement(complexVector, 1), "re"));
        assertEquals(4.0, getInterop().readMember(getInterop().readArrayElement(complexVector, 1), "im"));
    }

    private static void testReadComplexScalar(RAbstractComplexVector complexScalar) throws UnsupportedMessageException, UnknownIdentifierException, InvalidArrayIndexException {
        assertTrue(getInterop().hasArrayElements(complexScalar));
        testReadComplexScalarMembers(complexScalar);

        TruffleObject scalarElement = (TruffleObject) getInterop().readArrayElement(complexScalar, 0);
        assertFalse(getInterop().hasArrayElements(scalarElement));
        testReadComplexScalarMembers(scalarElement);
    }

    private static void testReadComplexScalarMembers(TruffleObject scalar) throws UnsupportedMessageException, UnknownIdentifierException {
        assertTrue(getInterop().hasMembers(scalar));
        Object members = getInterop().getMembers(scalar, false);
        assertEquals(getInterop().getArraySize(members), 2);
        assertEquals(1.0, getInterop().readMember(scalar, "re"));
        assertEquals(2.0, getInterop().readMember(scalar, "im"));
    }

    @Test
    public void testReadingNAReturnsTruffleObjectThatIsNull() throws Exception {
        // logical
        testRNARTOTIN(RDataFactory.createLogicalVector(new byte[]{RRuntime.LOGICAL_TRUE, RRuntime.LOGICAL_NA}, RDataFactory.INCOMPLETE_VECTOR), true);
        testRNARTOTIN(new RForeignBooleanWrapper((TruffleObject) RContext.getInstance().getEnv().asGuestValue(new Boolean[]{true, null})), true);
        getInterop().isNull(RDataFactory.createLogicalVector(new byte[]{RRuntime.LOGICAL_NA}, RDataFactory.INCOMPLETE_VECTOR));

        // int
        testRNARTOTIN(RDataFactory.createIntVector(new int[]{42, RRuntime.INT_NA}, RDataFactory.INCOMPLETE_VECTOR), 42);
        testRNARTOTIN(RClosures.createToIntVector(RDataFactory.createDoubleVector(new double[]{42, RRuntime.DOUBLE_NA}, RDataFactory.INCOMPLETE_VECTOR), true), 42);
        testRNARTOTIN(new RForeignIntWrapper((TruffleObject) RContext.getInstance().getEnv().asGuestValue(new int[]{42, RRuntime.INT_NA})), 42);
        testRNARTOTIN(new RForeignIntWrapper((TruffleObject) RContext.getInstance().getEnv().asGuestValue(new Integer[]{42, null})), 42);
        getInterop().isNull(RDataFactory.createIntVector(new int[]{RRuntime.INT_NA}, RDataFactory.INCOMPLETE_VECTOR));

        // double
        testRNARTOTIN(RDataFactory.createDoubleVector(new double[]{42, RRuntime.DOUBLE_NA}, RDataFactory.INCOMPLETE_VECTOR), 42.0);
        testRNARTOTIN(RClosures.createToDoubleVector(RDataFactory.createIntVector(new int[]{42, RRuntime.INT_NA}, RDataFactory.INCOMPLETE_VECTOR), true), 42.0);
        testRNARTOTIN(new RForeignDoubleWrapper((TruffleObject) RContext.getInstance().getEnv().asGuestValue(new double[]{42, RRuntime.DOUBLE_NA})), 42.0);
        testRNARTOTIN(new RForeignDoubleWrapper((TruffleObject) RContext.getInstance().getEnv().asGuestValue(new Double[]{42.0, null})), 42.0);
        getInterop().isNull(RDataFactory.createDoubleVector(new double[]{RRuntime.DOUBLE_NA}, RDataFactory.INCOMPLETE_VECTOR));

        // string
        testRNARTOTIN(RDataFactory.createStringVector(new String[]{"42", RRuntime.STRING_NA}, RDataFactory.INCOMPLETE_VECTOR), "42");
        testRNARTOTIN(RClosures.createToStringVector(RDataFactory.createIntVector(new int[]{42, RRuntime.INT_NA}, RDataFactory.INCOMPLETE_VECTOR), true), "42");
        testRNARTOTIN(new RForeignStringWrapper((TruffleObject) RContext.getInstance().getEnv().asGuestValue(new String[]{"42", RRuntime.STRING_NA})), "42");
        getInterop().isNull(RDataFactory.createStringVector(new String[]{RRuntime.STRING_NA}, RDataFactory.INCOMPLETE_VECTOR));

        // complex
        testRNARTOTIN(RDataFactory.createComplexVector(new double[]{1, 1, 2, RRuntime.COMPLEX_NA_IMAGINARY_PART}, RDataFactory.INCOMPLETE_VECTOR), new RInteropComplex(RComplex.valueOf(1, 1)));
        testRNARTOTIN(RDataFactory.createComplexVector(new double[]{1, 1, RRuntime.COMPLEX_NA_REAL_PART, 2}, RDataFactory.INCOMPLETE_VECTOR), new RInteropComplex(RComplex.valueOf(1, 1)));
        testRNARTOTIN(RDataFactory.createComplexVector(new double[]{1, 1, RRuntime.COMPLEX_NA_REAL_PART, RRuntime.COMPLEX_NA_IMAGINARY_PART}, RDataFactory.INCOMPLETE_VECTOR),
                        new RInteropComplex(RComplex.valueOf(1, 1)));
        testRNARTOTIN(RClosures.createToComplexVector(RDataFactory.createIntVector(new int[]{1, RRuntime.INT_NA}, RDataFactory.INCOMPLETE_VECTOR), true), new RInteropComplex(RComplex.valueOf(1, 0)));
        getInterop().isNull(RDataFactory.createComplexVector(new double[]{RRuntime.COMPLEX_NA_REAL_PART, RRuntime.COMPLEX_NA_IMAGINARY_PART}, RDataFactory.INCOMPLETE_VECTOR));
        getInterop().isNull(RDataFactory.createComplexVector(new double[]{RRuntime.COMPLEX_NA_REAL_PART, 1}, RDataFactory.INCOMPLETE_VECTOR));
        getInterop().isNull(RDataFactory.createComplexVector(new double[]{1, RRuntime.COMPLEX_NA_IMAGINARY_PART}, RDataFactory.INCOMPLETE_VECTOR));
    }

    public void testRNARTOTIN(TruffleObject vec, Object expectedFirst) throws Exception {
        assertEquals(expectedFirst, getInterop().readArrayElement(vec, 0));
        Object expectedNA = getInterop().readArrayElement(vec, 1);
        assertEquals(true, getInterop().isNull(expectedNA));
    }

    @Test
    public void testKeyInfo() throws Exception {
        for (TruffleObject o : createTruffleObjects()) {
            if (o instanceof RAbstractComplexVector && ((RAbstractComplexVector) o).getLength() == 1 && !isNull(o)) {
                assertTrue(getInterop().hasMembers(o));
            } else {
                assertInteropException(() -> getInterop().getMembers(o), UnsupportedMessageException.class);
            }

            for (int i = 0; i < getSize(o); i++) {
                assertTrue(getInterop().isArrayElementExisting(o, i));
                assertFalse(getInterop().isArrayElementInsertable(o, i));
                assertFalse(getInterop().isArrayElementModifiable(o, i));
                assertTrue(getInterop().isArrayElementReadable(o, i));
                assertFalse(getInterop().isArrayElementRemovable(o, i));
                assertFalse(getInterop().isArrayElementWritable(o, i));
            }
            assertFalse(getInterop().isArrayElementExisting(o, getSize(o)));
        }
    }

    @Override
    protected TruffleObject[] createTruffleObjects() throws Exception {
        // Note: single value vectors are unboxable, unless they contain NA
        return new TruffleObject[]{
                        // int array
                        RDataFactory.createIntVector(new int[]{1}, true),
                        RDataFactory.createIntVector(new int[]{Byte.MAX_VALUE, Short.MAX_VALUE, Integer.MAX_VALUE}, true),
                        RDataFactory.createIntVector(new int[]{1, 2, 3}, true),
                        RDataFactory.createIntVector(new int[]{RRuntime.INT_NA}, RDataFactory.INCOMPLETE_VECTOR),
                        RDataFactory.createIntVector(new int[]{1, RRuntime.INT_NA}, RDataFactory.INCOMPLETE_VECTOR),
                        RDataFactory.createEmptyIntVector(),
                        // int seq
                        RDataFactory.createIntSequence(1, 1, 3),
                        RDataFactory.createIntSequence(1, 1, 1),
                        // to int closure
                        RClosures.createToIntVector(RDataFactory.createDoubleVector(new double[]{1}, true), true),
                        RClosures.createToIntVector(RDataFactory.createDoubleVector(new double[]{Byte.MAX_VALUE, Short.MAX_VALUE, Integer.MAX_VALUE, Float.MAX_VALUE}, true), true),
                        RClosures.createToIntVector(RDataFactory.createDoubleVector(new double[]{1}, true), true),
                        RClosures.createToIntVector(RDataFactory.createDoubleVector(new double[]{1, 2, 3}, true), true),
                        RClosures.createToIntVector(RDataFactory.createDoubleVector(new double[]{RRuntime.DOUBLE_NA}, RDataFactory.INCOMPLETE_VECTOR), true),
                        RClosures.createToIntVector(RDataFactory.createDoubleVector(new double[]{1, RRuntime.DOUBLE_NA}, RDataFactory.INCOMPLETE_VECTOR), true),
                        RClosures.createToIntVector(RDataFactory.createEmptyDoubleVector(), true),
                        // int foreign wrapper
                        new RForeignIntWrapper((TruffleObject) RContext.getInstance().getEnv().asGuestValue(new int[]{1})),
                        new RForeignIntWrapper((TruffleObject) RContext.getInstance().getEnv().asGuestValue(new int[]{1, 2, 3})),
                        new RForeignIntWrapper((TruffleObject) RContext.getInstance().getEnv().asGuestValue(new int[]{Byte.MAX_VALUE, Short.MAX_VALUE, Integer.MAX_VALUE})),
                        new RForeignIntWrapper((TruffleObject) RContext.getInstance().getEnv().asGuestValue(new int[]{RRuntime.INT_NA})),
                        new RForeignIntWrapper((TruffleObject) RContext.getInstance().getEnv().asGuestValue(new int[]{1, RRuntime.INT_NA})),
                        new RForeignIntWrapper((TruffleObject) RContext.getInstance().getEnv().asGuestValue(new int[]{})),

                        // double array
                        RDataFactory.createDoubleVector(new double[]{1}, true),
                        RDataFactory.createDoubleVector(new double[]{Byte.MAX_VALUE, Short.MAX_VALUE, Integer.MAX_VALUE, Float.MAX_VALUE, Double.MAX_VALUE, Long.MAX_VALUE}, true),
                        RDataFactory.createDoubleVector(new double[]{1, 2, 3}, true),
                        RDataFactory.createDoubleVector(new double[]{RRuntime.DOUBLE_NA}, RDataFactory.INCOMPLETE_VECTOR),
                        RDataFactory.createDoubleVector(new double[]{1, RRuntime.DOUBLE_NA}, RDataFactory.INCOMPLETE_VECTOR),
                        RDataFactory.createEmptyDoubleVector(),
                        // double seq
                        RDataFactory.createDoubleSequence(1, 1, 10),
                        RDataFactory.createDoubleSequence(1, 1, 1),
                        // to double closure
                        RClosures.createToDoubleVector(RDataFactory.createIntVector(new int[]{1}, true), true),
                        RClosures.createToDoubleVector(RDataFactory.createIntVector(new int[]{1, 2, 3}, true), true),
                        RClosures.createToDoubleVector(RDataFactory.createIntVector(new int[]{Byte.MAX_VALUE, Short.MAX_VALUE, Integer.MAX_VALUE}, true), true),
                        RClosures.createToDoubleVector(RDataFactory.createIntVector(new int[]{RRuntime.INT_NA}, RDataFactory.INCOMPLETE_VECTOR), true),
                        RClosures.createToDoubleVector(RDataFactory.createIntVector(new int[]{1, RRuntime.INT_NA}, RDataFactory.INCOMPLETE_VECTOR), true),
                        RClosures.createToDoubleVector(RDataFactory.createEmptyIntVector(), true),
                        // double foreign wrapper
                        new RForeignDoubleWrapper((TruffleObject) RContext.getInstance().getEnv().asGuestValue(new double[]{1})),
                        new RForeignDoubleWrapper((TruffleObject) RContext.getInstance().getEnv().asGuestValue(new double[]{1, 2, 3})),
                        new RForeignDoubleWrapper((TruffleObject) RContext.getInstance().getEnv().asGuestValue(
                                        new double[]{Byte.MAX_VALUE, Short.MAX_VALUE, Integer.MAX_VALUE, Float.MAX_VALUE, Double.MAX_VALUE, Long.MAX_VALUE})),
                        new RForeignDoubleWrapper((TruffleObject) RContext.getInstance().getEnv().asGuestValue(new double[]{RRuntime.DOUBLE_NA})),
                        new RForeignDoubleWrapper((TruffleObject) RContext.getInstance().getEnv().asGuestValue(new double[]{1, RRuntime.DOUBLE_NA})),
                        new RForeignDoubleWrapper((TruffleObject) RContext.getInstance().getEnv().asGuestValue(new double[]{})),

                        // string array
                        RDataFactory.createStringVector(new String[]{"test1"}, true),
                        RDataFactory.createStringVector(new String[]{"test1", "test2", "test3"}, true),
                        RDataFactory.createStringVector(new String[]{RRuntime.STRING_NA}, RDataFactory.INCOMPLETE_VECTOR),
                        RDataFactory.createStringVector(new String[]{"test1", RRuntime.STRING_NA}, RDataFactory.INCOMPLETE_VECTOR),
                        RDataFactory.createEmptyStringVector(),
                        // double seq
                        RDataFactory.createStringSequence("pref", "suf", 1, 1, 10),
                        RDataFactory.createStringSequence("pref", "suf", 1, 1, 1),
                        // to double closure
                        RClosures.createToStringVector(RDataFactory.createIntVector(new int[]{1}, true), true),
                        RClosures.createToStringVector(RDataFactory.createIntVector(new int[]{1, 2, 3}, true), true),
                        RClosures.createToStringVector(RDataFactory.createIntVector(new int[]{RRuntime.INT_NA}, RDataFactory.INCOMPLETE_VECTOR), true),
                        RClosures.createToStringVector(RDataFactory.createIntVector(new int[]{1, RRuntime.INT_NA}, RDataFactory.INCOMPLETE_VECTOR), true),
                        RClosures.createToStringVector(RDataFactory.createEmptyIntVector(), true),
                        // string foreign wrapper
                        new RForeignStringWrapper((TruffleObject) RContext.getInstance().getEnv().asGuestValue(new String[]{"test1"})),
                        new RForeignStringWrapper((TruffleObject) RContext.getInstance().getEnv().asGuestValue(new String[]{"test2", "test2", "test2"})),
                        new RForeignStringWrapper((TruffleObject) RContext.getInstance().getEnv().asGuestValue(new String[]{RRuntime.STRING_NA})),
                        new RForeignStringWrapper((TruffleObject) RContext.getInstance().getEnv().asGuestValue(new String[]{"test1", RRuntime.STRING_NA})),
                        new RForeignStringWrapper((TruffleObject) RContext.getInstance().getEnv().asGuestValue(new String[]{})),

                        // complex array
                        RDataFactory.createComplexVector(new double[]{1, 1}, true),
                        RDataFactory.createComplexVector(new double[]{1, 1, 2, 2, 3, 3}, true),
                        RDataFactory.createComplexVector(new double[]{RRuntime.COMPLEX_NA_REAL_PART, RRuntime.COMPLEX_NA_IMAGINARY_PART}, RDataFactory.INCOMPLETE_VECTOR),
                        RDataFactory.createComplexVector(new double[]{1, RRuntime.COMPLEX_NA_IMAGINARY_PART}, RDataFactory.INCOMPLETE_VECTOR),
                        RDataFactory.createComplexVector(new double[]{RRuntime.COMPLEX_NA_REAL_PART, 1}, RDataFactory.INCOMPLETE_VECTOR),
                        RDataFactory.createComplexVector(new double[]{1, 1, RRuntime.COMPLEX_NA_REAL_PART, RRuntime.COMPLEX_NA_IMAGINARY_PART}, RDataFactory.INCOMPLETE_VECTOR),
                        RDataFactory.createEmptyComplexVector(),
                        // complex closure
                        RClosures.createToComplexVector(RDataFactory.createDoubleVector(new double[]{1}, true), true),
                        RClosures.createToComplexVector(RDataFactory.createDoubleVector(new double[]{1, 2, 3}, true), true),
                        RClosures.createToComplexVector(RDataFactory.createDoubleVector(new double[]{RRuntime.DOUBLE_NA}, RDataFactory.INCOMPLETE_VECTOR), true),
                        RClosures.createToComplexVector(RDataFactory.createDoubleVector(new double[]{1, RRuntime.DOUBLE_NA}, RDataFactory.INCOMPLETE_VECTOR), true),
                        RClosures.createToComplexVector(RDataFactory.createEmptyDoubleVector(), true),

                        // logical array
                        RDataFactory.createLogicalVector(new byte[]{1, 0}, RDataFactory.COMPLETE_VECTOR),
                        RDataFactory.createLogicalVector(new byte[]{1, RRuntime.LOGICAL_NA}, RDataFactory.INCOMPLETE_VECTOR),
                        RDataFactory.createLogicalVector(new byte[]{RRuntime.LOGICAL_NA}, RDataFactory.INCOMPLETE_VECTOR),
                        RDataFactory.createLogicalVector(new byte[]{RRuntime.LOGICAL_TRUE, RRuntime.LOGICAL_NA}, RDataFactory.INCOMPLETE_VECTOR),
                        RDataFactory.createLogicalVector(new byte[]{RRuntime.LOGICAL_NA, RRuntime.LOGICAL_TRUE}, RDataFactory.INCOMPLETE_VECTOR),
                        RDataFactory.createEmptyLogicalVector(),
                        // logical foreign wrapper
                        new RForeignBooleanWrapper((TruffleObject) RContext.getInstance().getEnv().asGuestValue(new boolean[]{true})),
                        new RForeignBooleanWrapper((TruffleObject) RContext.getInstance().getEnv().asGuestValue(new boolean[]{true, false})),
                        new RForeignBooleanWrapper((TruffleObject) RContext.getInstance().getEnv().asGuestValue(new boolean[]{})),

                        // raw array
                        RDataFactory.createRawVector(new byte[]{1}),
                        RDataFactory.createRawVector(new byte[]{Byte.MIN_VALUE, 0, Byte.MAX_VALUE}),
                        RDataFactory.createEmptyRawVector(),

                        createEmptyTruffleObject()
        };
    }

    @Override
    protected TruffleObject createEmptyTruffleObject() throws Exception {
        return RDataFactory.createDoubleVector(new double[]{}, true);
    }

    @Override
    protected boolean shouldTestToNative(TruffleObject obj) {
        return obj instanceof RBaseObject;
    }

    @Override
    protected Object getUnboxed(TruffleObject obj) {
        RAbstractVector vec = (RAbstractVector) obj;
        if ((vec.getLength() != 1 || isNA(vec))) {
            return null;
        }
        if (vec instanceof RAbstractLogicalVector) {
            return RRuntime.fromLogical(((RAbstractLogicalVector) vec).getDataAt(0));
        }
        if (vec instanceof RAbstractRawVector) {
            return ((RAbstractRawVector) vec).getRawDataAt(0);
        }
        return vec.getDataAtAsObject(0);
    }

    private static boolean isNA(RAbstractVector vec) {
        if (vec instanceof RAbstractDoubleVector) {
            return RRuntime.isNA(((RAbstractDoubleVector) vec).getDataAt(0));
        } else if (vec instanceof RAbstractIntVector) {
            return RRuntime.isNA(((RAbstractIntVector) vec).getDataAt(0));
        } else if (vec instanceof RAbstractLogicalVector) {
            return RRuntime.isNA(((RAbstractLogicalVector) vec).getDataAt(0));
        } else if (vec instanceof RAbstractStringVector) {
            return RRuntime.isNA(((RAbstractStringVector) vec).getDataAt(0));
        } else if (vec instanceof RAbstractComplexVector || vec instanceof RAbstractRawVector) {
            return false;
        }
        assertTrue("unexpected type of RAbstractVector " + vec != null ? vec.getClass().getSimpleName() : "null", false);
        return false;
    }

    @Override
    protected int getSize(TruffleObject obj) {
        return ((RAbstractVector) obj).getLength();
    }

    @Override
    protected boolean isNull(TruffleObject obj) {
        assert obj instanceof RAbstractVector;
        RAbstractVector vec = (RAbstractVector) obj;
        if (vec.getLength() != 1) {
            return false;
        }
        if (vec instanceof RAbstractLogicalVector) {
            return RRuntime.isNA(((RAbstractLogicalVector) vec).getDataAt(0));
        }
        if (vec instanceof RAbstractStringVector) {
            return RRuntime.isNA(((RAbstractStringVector) vec).getDataAt(0));
        }
        if (vec instanceof RAbstractIntVector) {
            return RRuntime.isNA(((RAbstractIntVector) vec).getDataAt(0));
        }
        if (vec instanceof RAbstractDoubleVector) {
            return RRuntime.isNA(((RAbstractDoubleVector) vec).getDataAt(0));
        }
        if (vec instanceof RAbstractComplexVector) {
            return RRuntime.isNA(((RAbstractComplexVector) vec).getDataAt(0));
        }
        if (vec instanceof RAbstractRawVector) {
            return false;
        }
        fail();
        return false;
    }

    @Override
    protected String[] getKeys(TruffleObject obj) {
        if ((obj instanceof RAbstractComplexVector) && ((RAbstractComplexVector) obj).getLength() == 1 && !isNull(obj)) {
            return new String[]{"re", "im"};
        }
        return super.getKeys(obj);
    }

}
