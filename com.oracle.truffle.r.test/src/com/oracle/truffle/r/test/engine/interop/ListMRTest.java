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
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.interop.java.JavaInterop;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.vm.PolyglotEngine;
import com.oracle.truffle.r.ffi.impl.interop.NativePointer;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;

public class ListMRTest extends AbstractMRTest {

    private String testValues = "i=1L, d=2.1, b=TRUE, fn=function() {}, n=NULL, 4";

    @Override
    @Test
    public void testNativePointer() throws UnsupportedMessageException, UnknownIdentifierException, UnsupportedTypeException {
        assertTrue(ForeignAccess.sendToNative(Message.TO_NATIVE.createNode(), create("list", testValues)) instanceof NativePointer);
        assertTrue(ForeignAccess.sendToNative(Message.TO_NATIVE.createNode(), create("pairlist", testValues)) instanceof NativePointer);
    }

    @Test
    public void testKeysReadWrite() throws UnsupportedMessageException, UnknownIdentifierException, UnsupportedTypeException {
        testKeysReadWrite("list");
        testKeysReadWrite("pairlist");
    }

    private void testKeysReadWrite(String createFun) throws UnsupportedMessageException, UnknownIdentifierException, UnsupportedTypeException {

        RAbstractContainer l = create(createFun, testValues);

        assertEquals(1, ForeignAccess.sendRead(Message.READ.createNode(), l, "i"));
        assertEquals(2.1, ForeignAccess.sendRead(Message.READ.createNode(), l, "d"));
        assertEquals(true, ForeignAccess.sendRead(Message.READ.createNode(), l, "b"));
        assertTrue(ForeignAccess.sendRead(Message.READ.createNode(), l, "n") instanceof RNull);

        assertEquals(1, ForeignAccess.sendRead(Message.READ.createNode(), l, 0));
        assertEquals(2.1, ForeignAccess.sendRead(Message.READ.createNode(), l, 1));
        assertEquals(4d, ForeignAccess.sendRead(Message.READ.createNode(), l, 5d));
        assertEquals(true, ForeignAccess.sendRead(Message.READ.createNode(), l, 2));
        assertTrue(ForeignAccess.sendRead(Message.READ.createNode(), l, 4) instanceof RNull);

        assertInteropException(() -> ForeignAccess.sendRead(Message.READ.createNode(), l, -1), UnknownIdentifierException.class);
        assertInteropException(() -> ForeignAccess.sendRead(Message.READ.createNode(), l, 0f), UnknownIdentifierException.class);

        assertInteropException(() -> ForeignAccess.sendRead(Message.READ.createNode(), l, "nnnoooonnne"), UnknownIdentifierException.class);
        assertInteropException(() -> ForeignAccess.sendRead(Message.READ.createNode(), l, 100), UnknownIdentifierException.class);

        TruffleObject obj = (TruffleObject) ForeignAccess.sendWrite(Message.WRITE.createNode(), l, "d", 123.1);
        assertEquals(123.1, ForeignAccess.sendRead(Message.READ.createNode(), obj, "d"));

        obj = (TruffleObject) ForeignAccess.sendWrite(Message.WRITE.createNode(), l, 2, false);
        RAbstractContainer returnedList = JavaInterop.asJavaObject(RAbstractContainer.class, obj);
        assertEquals((byte) 0, returnedList.getDataAtAsObject(2));
        assertEquals(false, ForeignAccess.sendRead(Message.READ.createNode(), obj, "b"));

        obj = (TruffleObject) ForeignAccess.sendWrite(Message.WRITE.createNode(), l, "newnew", "nneeww");
        assertEquals("nneeww", ForeignAccess.sendRead(Message.READ.createNode(), obj, "newnew"));

        assertInteropException(() -> ForeignAccess.sendWrite(Message.WRITE.createNode(), l, 0f, false), UnknownIdentifierException.class);
        assertInteropException(() -> ForeignAccess.sendWrite(Message.WRITE.createNode(), l, 0d, false), UnknownIdentifierException.class);
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
        assertTrue(KeyInfo.isWritable(info));
        assertFalse(KeyInfo.isInvocable(info));
        assertFalse(KeyInfo.isInternal(info));

        info = ForeignAccess.sendKeyInfo(Message.KEY_INFO.createNode(), l, "fn");
        assertTrue(KeyInfo.isExisting(info));
        assertTrue(KeyInfo.isReadable(info));
        assertTrue(KeyInfo.isWritable(info));
        assertTrue(KeyInfo.isInvocable(info));
        assertFalse(KeyInfo.isInternal(info));

        info = ForeignAccess.sendKeyInfo(Message.KEY_INFO.createNode(), l, -1);
        assertFalse(KeyInfo.isExisting(info));

        info = ForeignAccess.sendKeyInfo(Message.KEY_INFO.createNode(), l, l.getLength());
        assertFalse(KeyInfo.isExisting(info));

        info = ForeignAccess.sendKeyInfo(Message.KEY_INFO.createNode(), l, 1f);
        assertFalse(KeyInfo.isExisting(info));

        info = ForeignAccess.sendKeyInfo(Message.KEY_INFO.createNode(), l, 0);
        assertTrue(KeyInfo.isExisting(info));
        assertTrue(KeyInfo.isReadable(info));
        assertTrue(KeyInfo.isWritable(info));
        assertFalse(KeyInfo.isInvocable(info));
        assertFalse(KeyInfo.isInternal(info));

        info = ForeignAccess.sendKeyInfo(Message.KEY_INFO.createNode(), l, 1d);
        assertTrue(KeyInfo.isExisting(info));
        assertTrue(KeyInfo.isReadable(info));
        assertTrue(KeyInfo.isWritable(info));
        assertFalse(KeyInfo.isInvocable(info));
        assertFalse(KeyInfo.isInternal(info));
    }

    private static RAbstractContainer create(String createFun, String values) {
        Source src = Source.newBuilder(createFun + "(" + values + ")").mimeType("text/x-r").name("test.R").build();
        PolyglotEngine.Value result = engine.eval(src);
        return result.as(RAbstractContainer.class);
    }

    @Override
    protected String[] getKeys() {
        return new String[]{"i", "d", "b", "fn", "n", ""};
    }

    @Override
    protected TruffleObject[] createTruffleObjects() throws Exception {
        return new TruffleObject[]{create("list", testValues), create("pairlist", testValues)};
    }

    @Override
    protected TruffleObject createEmptyTruffleObject() throws Exception {
        // cant have an emtpy pair list
        return create("list", "");
    }

    @Override
    protected boolean hasSize(TruffleObject arg0) {
        return true;
    }

    @Override
    protected int getSize(TruffleObject obj) {
        return obj instanceof RList ? ((RList) obj).getLength() : ((RPairList) obj).getLength();
    }
}
