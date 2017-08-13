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
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.vm.PolyglotEngine;
import com.oracle.truffle.r.runtime.data.RS4Object;

public class RS4ObjectMRTest extends AbstractMRTest {

    @Test
    public void testKeysInfo() throws Exception {
        TruffleObject s4 = createTruffleObjects()[0];
        int info = ForeignAccess.sendKeyInfo(Message.KEY_INFO.createNode(), s4, "nnoonnee");
        assertFalse(KeyInfo.isExisting(info));
        info = ForeignAccess.sendKeyInfo(Message.KEY_INFO.createNode(), s4, 0);
        assertFalse(KeyInfo.isExisting(info));

        info = ForeignAccess.sendKeyInfo(Message.KEY_INFO.createNode(), s4, 1f);
        assertFalse(KeyInfo.isExisting(info));

        info = ForeignAccess.sendKeyInfo(Message.KEY_INFO.createNode(), s4, "s");
        assertTrue(KeyInfo.isExisting(info));
        assertTrue(KeyInfo.isReadable(info));
        assertTrue(KeyInfo.isWritable(info));
        assertFalse(KeyInfo.isInvocable(info));
        assertFalse(KeyInfo.isInternal(info));

        info = ForeignAccess.sendKeyInfo(Message.KEY_INFO.createNode(), s4, "b");
        assertTrue(KeyInfo.isExisting(info));
        assertTrue(KeyInfo.isReadable(info));
        assertTrue(KeyInfo.isWritable(info));
        assertFalse(KeyInfo.isInvocable(info));
        assertFalse(KeyInfo.isInternal(info));

        info = ForeignAccess.sendKeyInfo(Message.KEY_INFO.createNode(), s4, "fn");
        assertTrue(KeyInfo.isExisting(info));
        assertTrue(KeyInfo.isReadable(info));
        assertTrue(KeyInfo.isWritable(info));
        assertTrue(KeyInfo.isInvocable(info));
        assertFalse(KeyInfo.isInternal(info));

        info = ForeignAccess.sendKeyInfo(Message.KEY_INFO.createNode(), s4, "class");
        assertTrue(KeyInfo.isExisting(info));
        assertTrue(KeyInfo.isReadable(info));
        assertFalse(KeyInfo.isWritable(info));
        assertFalse(KeyInfo.isInvocable(info));
        assertFalse(KeyInfo.isInternal(info));
    }

    @Test
    public void testReadWrite() throws Exception {
        TruffleObject s4 = createTruffleObjects()[0];

        assertEquals("aaa", ForeignAccess.sendRead(Message.READ.createNode(), s4, "s"));
        assertEquals(123, ForeignAccess.sendRead(Message.READ.createNode(), s4, "i"));
        assertEquals(1.1, ForeignAccess.sendRead(Message.READ.createNode(), s4, "d"));
        assertEquals(true, ForeignAccess.sendRead(Message.READ.createNode(), s4, "b"));

        assertInteropException(() -> ForeignAccess.sendRead(Message.READ.createNode(), s4, "nnnoooonnne"), UnknownIdentifierException.class);
        assertInteropException(() -> ForeignAccess.sendRead(Message.READ.createNode(), s4, 0), UnknownIdentifierException.class);
        assertInteropException(() -> ForeignAccess.sendRead(Message.READ.createNode(), s4, 1d), UnknownIdentifierException.class);
        assertInteropException(() -> ForeignAccess.sendRead(Message.READ.createNode(), s4, 1f), UnknownIdentifierException.class);

        assertInteropException(() -> ForeignAccess.sendWrite(Message.WRITE.createNode(), s4, "class", "cantchangeclass"), UnsupportedMessageException.class);
        // TODO this should fail !!!
        // assertInteropException(() -> ForeignAccess.sendWrite(Message.WRITE.createNode(), s4, "i",
        // "cant write string into int slot"), UnsupportedMessageException.class);

        ForeignAccess.sendWrite(Message.WRITE.createNode(), s4, "s", "abc");
        Object value = ForeignAccess.sendRead(Message.READ.createNode(), s4, "s");
        assertEquals("abc", value);

        ForeignAccess.sendWrite(Message.WRITE.createNode(), s4, "b", false);
        value = ForeignAccess.sendRead(Message.READ.createNode(), s4, "b");
        assertEquals(false, value);

        ForeignAccess.sendWrite(Message.WRITE.createNode(), s4, "i", (short) 1234);
        value = ForeignAccess.sendRead(Message.READ.createNode(), s4, "i");
        assertTrue(value instanceof Integer);
        assertEquals(1234, value);

    }

    @Override
    protected TruffleObject[] createTruffleObjects() {
        String srcTxt = "setClass('test', representation(s = 'character', d = 'numeric', i = 'integer', b = 'logical', fn = 'function'));" +
                        "new('test', s = 'aaa', d = 1.1, i=123L, b = TRUE, fn = function() {})";
        Source src = Source.newBuilder(srcTxt).mimeType("text/x-r").name("test.R").build();
        PolyglotEngine.Value result = engine.eval(src);
        RS4Object s4 = result.as(RS4Object.class);
        return new TruffleObject[]{s4};
    }

    @Override
    protected String[] getKeys() {
        return new String[]{"s", "d", "i", "b", "fn", "class"};
    }

    @Override
    protected TruffleObject createEmptyTruffleObject() throws Exception {
        return null;
    }
}
