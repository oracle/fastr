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
package com.oracle.truffle.r.test.engine.interop;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.KeyInfo;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.java.JavaInterop;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RObject;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import static org.junit.Assert.assertEquals;

public class VectorMRTest extends AbstractMRTest {

    @Test
    public void testReadWrite() throws Exception {
        execInContext(() -> {
            final TruffleObject vi = RDataFactory.createIntSequence(1, 1, 10);
            assertEquals(3, ForeignAccess.sendRead(Message.READ.createNode(), vi, 2));
            assertEquals(3, ForeignAccess.sendRead(Message.READ.createNode(), vi, 2L));

            assertInteropException(() -> ForeignAccess.sendRead(Message.READ.createNode(), vi, "a"), UnknownIdentifierException.class);
            assertInteropException(() -> ForeignAccess.sendRead(Message.READ.createNode(), vi, 100), UnknownIdentifierException.class);
            assertInteropException(() -> ForeignAccess.sendWrite(Message.WRITE.createNode(), vi, "s", "abc"), UnknownIdentifierException.class);

            TruffleObject vd = RDataFactory.createDoubleSequence(1.1, 1, 10);
            assertEquals(1.1, ForeignAccess.sendRead(Message.READ.createNode(), vd, 0));

            TruffleObject vb = RDataFactory.createLogicalVector(new byte[]{1, 0, 1}, true);
            assertEquals(true, ForeignAccess.sendRead(Message.READ.createNode(), vb, 0));

            TruffleObject nvi = (TruffleObject) ForeignAccess.sendWrite(Message.WRITE.createNode(), vi, 0, 123);
            RAbstractIntVector returnedVec = JavaInterop.asJavaObject(RAbstractIntVector.class, nvi);
            assertEquals(123, returnedVec.getDataAt(0));
            assertEquals(123, ForeignAccess.sendRead(Message.READ.createNode(), nvi, 0));

            assertEquals(10, ForeignAccess.sendGetSize(Message.GET_SIZE.createNode(), nvi));
            nvi = (TruffleObject) ForeignAccess.sendWrite(Message.WRITE.createNode(), nvi, 100, 321);
            assertEquals(101, ForeignAccess.sendGetSize(Message.GET_SIZE.createNode(), nvi));
            assertEquals(321, ForeignAccess.sendRead(Message.READ.createNode(), nvi, 100));

            nvi = (TruffleObject) ForeignAccess.sendWrite(Message.WRITE.createNode(), nvi, 0, "abc");
            RAbstractVector vec = JavaInterop.asJavaObject(RAbstractVector.class, nvi);
            assertTrue(vec instanceof RAbstractStringVector);
            assertEquals("abc", ForeignAccess.sendRead(Message.READ.createNode(), nvi, 0));
            return null;
        });
    }

    @Test
    public void testKeyInfo() throws Exception {
        execInContext(() -> {
            TruffleObject v = RDataFactory.createLogicalVector(new byte[]{1, 0, 1}, true);
            assertInteropException(() -> ForeignAccess.sendKeys(Message.KEYS.createNode(), v), UnsupportedMessageException.class);

            int keyInfo = ForeignAccess.sendKeyInfo(Message.KEY_INFO.createNode(), v, 0);
            assertTrue(KeyInfo.isExisting(keyInfo));
            assertTrue(KeyInfo.isReadable(keyInfo));
            assertTrue(KeyInfo.isWritable(keyInfo));
            assertFalse(KeyInfo.isInvocable(keyInfo));
            assertFalse(KeyInfo.isInternal(keyInfo));

            keyInfo = ForeignAccess.sendKeyInfo(Message.KEY_INFO.createNode(), v, 100);
            assertFalse(KeyInfo.isExisting(keyInfo));
            return null;
        });
    }

    @Override
    protected TruffleObject[] createTruffleObjects() throws Exception {
        return new TruffleObject[]{RDataFactory.createDoubleVector(new double[]{1}, true), RDataFactory.createDoubleSequence(1, 1, 10), createEmptyTruffleObject()};
    }

    @Override
    protected TruffleObject createEmptyTruffleObject() throws Exception {
        return RDataFactory.createDoubleVector(new double[]{}, true);
    }

    @Override
    protected boolean testToNative(TruffleObject obj) {
        return obj instanceof RObject;
    }

    @Override
    protected Object getUnboxed(TruffleObject obj) {
        assertTrue(((RAbstractVector) obj).getLength() == 1);
        return ((RAbstractVector) obj).getDataAtAsObject(0);
    }

    @Override
    protected int getSize(TruffleObject obj) {
        return ((RAbstractVector) obj).getLength();
    }

}
