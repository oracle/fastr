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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.graalvm.polyglot.Value;
import org.junit.Test;

import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.KeyInfo;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.r.runtime.data.RExpression;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.test.generate.FastRSession;
import static org.junit.Assert.fail;

public class ListMRTest extends AbstractMRTest {

    private static final String testValues = "i=1L, d=2.1, b=TRUE, fn=function() {}, n=NULL, 4";

    @Override
    @Test
    public void testNativePointer() throws UnsupportedMessageException, UnknownIdentifierException, UnsupportedTypeException {
        for (TruffleObject obj : new TruffleObject[]{create("list", testValues), create("pairlist", testValues)}) {
            assertTrue(ForeignAccess.sendToNative(Message.TO_NATIVE.createNode(), obj) == obj);
        }
    }

    @Override
    protected boolean canRead(@SuppressWarnings("unused") TruffleObject obj) {
        return true;
    }

    @Test
    public void testKeysRead() throws Exception {
        ListMRTest.this.testKeysRead("list");
        ListMRTest.this.testKeysRead("pairlist");
    }

    private void testKeysRead(String createFun) throws Exception {
        RAbstractContainer l = create(createFun, testValues);

        assertSingletonVector(1, ForeignAccess.sendRead(Message.READ.createNode(), l, "i"));
        assertSingletonVector(2.1, ForeignAccess.sendRead(Message.READ.createNode(), l, "d"));
        assertSingletonVector(true, ForeignAccess.sendRead(Message.READ.createNode(), l, "b"));
        assertTrue(ForeignAccess.sendRead(Message.READ.createNode(), l, "n") instanceof RNull);

        assertSingletonVector(1, ForeignAccess.sendRead(Message.READ.createNode(), l, 0));
        assertSingletonVector(1, ForeignAccess.sendRead(Message.READ.createNode(), l, 0f));
        assertSingletonVector(2.1, ForeignAccess.sendRead(Message.READ.createNode(), l, 1));
        assertSingletonVector(4d, ForeignAccess.sendRead(Message.READ.createNode(), l, 5d));
        assertSingletonVector(true, ForeignAccess.sendRead(Message.READ.createNode(), l, 2));
        assertTrue(ForeignAccess.sendRead(Message.READ.createNode(), l, 4) instanceof RNull);

        assertInteropException(() -> ForeignAccess.sendRead(Message.READ.createNode(), l, -1), UnknownIdentifierException.class);

        assertInteropException(() -> ForeignAccess.sendRead(Message.READ.createNode(), l, "nnnoooonnne"), UnknownIdentifierException.class);
        assertInteropException(() -> ForeignAccess.sendRead(Message.READ.createNode(), l, 100), UnknownIdentifierException.class);
    }

    @Test
    public void testKeysInfo() {
        testKeysInfo("list");
        testKeysInfo("pairlist");
    }

    public void testKeysInfo(String createFun) {
        RAbstractContainer l = create(createFun, testValues);

        int info = ForeignAccess.sendKeyInfo(Message.KEY_INFO.createNode(), l, "nnoonnee");
        assertFalse(KeyInfo.isExisting(info));
        assertFalse(KeyInfo.isReadable(info));
        assertFalse(KeyInfo.isWritable(info));
        assertFalse(KeyInfo.isInvocable(info));
        assertFalse(KeyInfo.isInternal(info));

        info = ForeignAccess.sendKeyInfo(Message.KEY_INFO.createNode(), l, "d");
        assertTrue(KeyInfo.isExisting(info));
        assertTrue(KeyInfo.isReadable(info));
        assertFalse(KeyInfo.isWritable(info));
        assertFalse(KeyInfo.isInvocable(info));
        assertFalse(KeyInfo.isInternal(info));

        info = ForeignAccess.sendKeyInfo(Message.KEY_INFO.createNode(), l, "fn");
        assertTrue(KeyInfo.isExisting(info));
        assertTrue(KeyInfo.isReadable(info));
        assertFalse(KeyInfo.isWritable(info));
        assertTrue(KeyInfo.isInvocable(info));
        assertFalse(KeyInfo.isInternal(info));

        info = ForeignAccess.sendKeyInfo(Message.KEY_INFO.createNode(), l, -1);
        assertFalse(KeyInfo.isExisting(info));

        info = ForeignAccess.sendKeyInfo(Message.KEY_INFO.createNode(), l, l.getLength());
        assertFalse(KeyInfo.isExisting(info));

        info = ForeignAccess.sendKeyInfo(Message.KEY_INFO.createNode(), l, 1f);
        assertTrue(KeyInfo.isExisting(info));

        info = ForeignAccess.sendKeyInfo(Message.KEY_INFO.createNode(), l, 0);
        assertTrue(KeyInfo.isExisting(info));
        assertTrue(KeyInfo.isReadable(info));
        assertFalse(KeyInfo.isWritable(info));
        assertFalse(KeyInfo.isInvocable(info));
        assertFalse(KeyInfo.isInternal(info));

        info = ForeignAccess.sendKeyInfo(Message.KEY_INFO.createNode(), l, 1d);
        assertTrue(KeyInfo.isExisting(info));
        assertTrue(KeyInfo.isReadable(info));
        assertFalse(KeyInfo.isWritable(info));
        assertFalse(KeyInfo.isInvocable(info));
        assertFalse(KeyInfo.isInternal(info));
    }

    private static RAbstractContainer create(String createFun, String values) {
        String create = createFun + "(" + values + ")";
        org.graalvm.polyglot.Source src = org.graalvm.polyglot.Source.newBuilder("R", create, "<testrlist>").internal(true).buildLiteral();
        Value result = context.eval(src);
        return (RAbstractContainer) FastRSession.getReceiver(result);
    }

    @Override
    protected String[] getKeys(TruffleObject obj) {
        if (((RAbstractContainer) obj).getLength() > 0) {
            return new String[]{"i", "d", "b", "fn", "n", ""};
        }
        return new String[]{};
    }

    @Override
    protected TruffleObject[] createTruffleObjects() throws Exception {
        return new TruffleObject[]{create("list", testValues), create("pairlist", testValues), create("expression", testValues)};
    }

    @Override
    protected TruffleObject createEmptyTruffleObject() throws Exception {
        // cant have an emtpy pair list
        return create("list", "");
    }

    @Override
    protected int getSize(TruffleObject obj) {
        if (obj instanceof RList) {
            return ((RList) obj).getLength();
        } else if (obj instanceof RPairList) {
            return ((RPairList) obj).getLength();
        } else if (obj instanceof RExpression) {
            return ((RExpression) obj).getLength();
        }
        fail("unexpected list type " + obj.getClass().getName());
        return -1;
    }
}
