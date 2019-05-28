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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.KeyInfo;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RForeignBooleanWrapper;
import com.oracle.truffle.r.runtime.data.RForeignDoubleWrapper;
import com.oracle.truffle.r.runtime.data.RForeignIntWrapper;
import com.oracle.truffle.r.runtime.data.RForeignStringWrapper;
import com.oracle.truffle.r.runtime.data.RObject;
import com.oracle.truffle.r.runtime.data.closures.RClosures;
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractRawVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

public class VectorMRTest extends AbstractMRTest {

    @Override
    protected boolean canRead(@SuppressWarnings("unused") TruffleObject obj) {
        return true;
    }

    @Test
    public void testRead() throws Exception {
        final TruffleObject vi = RDataFactory.createIntSequence(1, 1, 10);
        assertEquals(3, ForeignAccess.sendRead(Message.READ.createNode(), vi, 2));
        assertEquals(3, ForeignAccess.sendRead(Message.READ.createNode(), vi, 2L));

        assertInteropException(() -> ForeignAccess.sendRead(Message.READ.createNode(), vi, "a"), UnsupportedMessageException.class);
        assertInteropException(() -> ForeignAccess.sendRead(Message.READ.createNode(), vi, 100), UnknownIdentifierException.class);

        TruffleObject vd = RDataFactory.createDoubleSequence(1.1, 1, 10);
        assertEquals(1.1, ForeignAccess.sendRead(Message.READ.createNode(), vd, 0));

        TruffleObject vb = RDataFactory.createLogicalVector(new byte[]{1, 0, 1}, true);
        assertEquals(true, ForeignAccess.sendRead(Message.READ.createNode(), vb, 0));
    }

    @Test
    public void testReadingNAReturnsTruffleObjectThatIsNull() throws Exception {
        // logical
        testRNARTOTIN(RDataFactory.createLogicalVector(new byte[]{RRuntime.LOGICAL_TRUE, RRuntime.LOGICAL_NA}, RDataFactory.INCOMPLETE_VECTOR), true);
        testRNARTOTIN(new RForeignBooleanWrapper((TruffleObject) RContext.getInstance().getEnv().asGuestValue(new Boolean[]{true, null})), true);

        // int
        testRNARTOTIN(RDataFactory.createIntVector(new int[]{42, RRuntime.INT_NA}, RDataFactory.INCOMPLETE_VECTOR), 42);
        testRNARTOTIN(RClosures.createToIntVector(RDataFactory.createDoubleVector(new double[]{42, RRuntime.DOUBLE_NA}, RDataFactory.INCOMPLETE_VECTOR), true), 42);
        testRNARTOTIN(new RForeignIntWrapper((TruffleObject) RContext.getInstance().getEnv().asGuestValue(new int[]{42, RRuntime.INT_NA})), 42);
        testRNARTOTIN(new RForeignIntWrapper((TruffleObject) RContext.getInstance().getEnv().asGuestValue(new Integer[]{42, null})), 42);

        // double
        testRNARTOTIN(RDataFactory.createDoubleVector(new double[]{42, RRuntime.DOUBLE_NA}, RDataFactory.INCOMPLETE_VECTOR), 42.0);
        testRNARTOTIN(RClosures.createToDoubleVector(RDataFactory.createIntVector(new int[]{42, RRuntime.INT_NA}, RDataFactory.INCOMPLETE_VECTOR), true), 42.0);
        testRNARTOTIN(new RForeignDoubleWrapper((TruffleObject) RContext.getInstance().getEnv().asGuestValue(new double[]{42, RRuntime.DOUBLE_NA})), 42.0);
        testRNARTOTIN(new RForeignDoubleWrapper((TruffleObject) RContext.getInstance().getEnv().asGuestValue(new Double[]{42.0, null})), 42.0);

        // string
        testRNARTOTIN(RDataFactory.createStringVector(new String[]{"42", RRuntime.STRING_NA}, RDataFactory.INCOMPLETE_VECTOR), "42");
        testRNARTOTIN(RClosures.createToStringVector(RDataFactory.createIntVector(new int[]{42, RRuntime.INT_NA}, RDataFactory.INCOMPLETE_VECTOR), true), "42");
        testRNARTOTIN(new RForeignStringWrapper((TruffleObject) RContext.getInstance().getEnv().asGuestValue(new String[]{"42", RRuntime.STRING_NA})), "42");
    }

    public void testRNARTOTIN(TruffleObject vec, Object expectedFirst) throws Exception {
        assertEquals(expectedFirst, ForeignAccess.sendRead(Message.READ.createNode(), vec, 0));
        Object expectedNA = ForeignAccess.sendRead(Message.READ.createNode(), vec, 1);
        assertEquals(true, ForeignAccess.sendIsNull(Message.IS_NULL.createNode(), (TruffleObject) expectedNA));
    }

    @Test
    public void testKeyInfo() throws Exception {
        for (TruffleObject o : createTruffleObjects()) {
            assertInteropException(() -> ForeignAccess.sendKeys(Message.KEYS.createNode(), o), UnsupportedMessageException.class);

            for (int i = 0; i < getSize(o); i++) {
                int keyInfo = ForeignAccess.sendKeyInfo(Message.KEY_INFO.createNode(), o, i);
                assertTrue(KeyInfo.isExisting(keyInfo));
                assertFalse(KeyInfo.isInsertable(keyInfo));
                assertFalse(KeyInfo.isInternal(keyInfo));
                assertFalse(KeyInfo.isInvocable(keyInfo));
                assertFalse(KeyInfo.isModifiable(keyInfo));
                assertTrue(KeyInfo.isReadable(keyInfo));
                assertFalse(KeyInfo.isRemovable(keyInfo));
                assertFalse(KeyInfo.isWritable(keyInfo));
                assertFalse(KeyInfo.hasReadSideEffects(keyInfo));
                assertFalse(KeyInfo.hasWriteSideEffects(keyInfo));
            }
            assertFalse(KeyInfo.isExisting(ForeignAccess.sendKeyInfo(Message.KEY_INFO.createNode(), o, getSize(o))));
        }
    }

    @Override
    protected TruffleObject[] createTruffleObjects() throws Exception {
        // Note: single value vectors are unboxable, unless they contain NA
        return new TruffleObject[]{
                        // int array
                        RDataFactory.createIntVector(new int[]{1}, true),
                        RDataFactory.createIntVector(new int[]{1, 2, 3}, true),
                        RDataFactory.createIntVector(new int[]{RRuntime.INT_NA}, RDataFactory.INCOMPLETE_VECTOR),
                        RDataFactory.createIntVector(new int[]{1, RRuntime.INT_NA}, RDataFactory.INCOMPLETE_VECTOR),
                        RDataFactory.createEmptyIntVector(),
                        // int seq
                        RDataFactory.createIntSequence(1, 1, 3),
                        RDataFactory.createIntSequence(1, 1, 1),
                        // to int closure
                        RClosures.createToIntVector(RDataFactory.createDoubleVector(new double[]{1}, true), true),
                        RClosures.createToIntVector(RDataFactory.createDoubleVector(new double[]{1, 2, 3}, true), true),
                        RClosures.createToIntVector(RDataFactory.createDoubleVector(new double[]{RRuntime.DOUBLE_NA}, RDataFactory.INCOMPLETE_VECTOR), true),
                        RClosures.createToIntVector(RDataFactory.createDoubleVector(new double[]{1, RRuntime.DOUBLE_NA}, RDataFactory.INCOMPLETE_VECTOR), true),
                        RClosures.createToIntVector(RDataFactory.createEmptyDoubleVector(), true),
                        // int foreign wrapper
                        // XXX foreign NAs vs null
                        new RForeignIntWrapper((TruffleObject) RContext.getInstance().getEnv().asGuestValue(new int[]{1})),
                        new RForeignIntWrapper((TruffleObject) RContext.getInstance().getEnv().asGuestValue(new int[]{1, 2, 3})),
                        new RForeignIntWrapper((TruffleObject) RContext.getInstance().getEnv().asGuestValue(new int[]{RRuntime.INT_NA})),
                        new RForeignIntWrapper((TruffleObject) RContext.getInstance().getEnv().asGuestValue(new int[]{1, RRuntime.INT_NA})),
                        new RForeignIntWrapper((TruffleObject) RContext.getInstance().getEnv().asGuestValue(new int[]{})),

                        // double array
                        RDataFactory.createDoubleVector(new double[]{1}, true),
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
                        RClosures.createToDoubleVector(RDataFactory.createIntVector(new int[]{RRuntime.INT_NA}, RDataFactory.INCOMPLETE_VECTOR), true),
                        RClosures.createToDoubleVector(RDataFactory.createIntVector(new int[]{1, RRuntime.INT_NA}, RDataFactory.INCOMPLETE_VECTOR), true),
                        RClosures.createToDoubleVector(RDataFactory.createEmptyIntVector(), true),
                        // double foreign wrapper
                        new RForeignDoubleWrapper((TruffleObject) RContext.getInstance().getEnv().asGuestValue(new double[]{1})),
                        new RForeignDoubleWrapper((TruffleObject) RContext.getInstance().getEnv().asGuestValue(new double[]{1, 2, 3})),
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
        return obj instanceof RObject;
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

}
