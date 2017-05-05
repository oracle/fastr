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

import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.KeyInfo;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;

import org.junit.Test;

import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.vm.PolyglotEngine;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RListMRTest {

    @Test
    public void testKeysReadWrite() throws UnsupportedMessageException, UnknownIdentifierException, UnsupportedTypeException {
        TruffleObject l = createRListTruffleObject("n1=1L, n2=2L, 3, 4");

        TruffleObject keys = ForeignAccess.sendKeys(Message.KEYS.createNode(), l);
        assertEquals("n1", ForeignAccess.sendRead(Message.READ.createNode(), keys, 0));
        assertEquals("n2", ForeignAccess.sendRead(Message.READ.createNode(), keys, 1));
        assertEquals("", ForeignAccess.sendRead(Message.READ.createNode(), keys, 2));
        assertEquals("", ForeignAccess.sendRead(Message.READ.createNode(), keys, 3));

        assertEquals(4, ForeignAccess.sendGetSize(Message.GET_SIZE.createNode(), keys));
        assertEquals(1, ForeignAccess.sendRead(Message.READ.createNode(), l, "n1"));
        assertEquals(2, ForeignAccess.sendRead(Message.READ.createNode(), l, "n2"));

        // TODO fails - should this return NULL as in l[""] or 3 as in l$""
        // ElementAccessMode.FIELD_SUBSCRIPT vs .SUBSCRIPT doesn't seem to have any effect
        // assertEquals(3, ForeignAccess.sendRead(Message.READ.createNode(), l, ""));

        // TODO - more tests for NA, ... ?

        l = (TruffleObject) ForeignAccess.sendWrite(Message.WRITE.createNode(), l, "n2", 123);
        assertEquals(123, ForeignAccess.sendRead(Message.READ.createNode(), l, "n2"));
    }

    @Test
    public void testKeysInfo() throws UnsupportedMessageException, UnknownIdentifierException, UnsupportedTypeException {
        TruffleObject l = createRListTruffleObject("n1=1, n2=2");
        int info = ForeignAccess.sendKeyInfo(Message.KEY_INFO.createNode(), l, "nnoonnee");
        assertFalse(KeyInfo.isExisting(info));
        assertFalse(KeyInfo.isReadable(info));
        assertFalse(KeyInfo.isWritable(info));
        assertFalse(KeyInfo.isInvocable(info));
        assertFalse(KeyInfo.isInternal(info));

        info = ForeignAccess.sendKeyInfo(Message.KEY_INFO.createNode(), l, "n2");
        assertTrue(KeyInfo.isExisting(info));
        assertTrue(KeyInfo.isReadable(info));
        assertTrue(KeyInfo.isWritable(info));
        assertFalse(KeyInfo.isInvocable(info));
        assertFalse(KeyInfo.isInternal(info));

        l = createRListTruffleObject("f=function() {}");
        info = ForeignAccess.sendKeyInfo(Message.KEY_INFO.createNode(), l, "f");
        assertTrue(KeyInfo.isExisting(info));
        assertTrue(KeyInfo.isReadable(info));
        assertTrue(KeyInfo.isWritable(info));
        assertTrue(KeyInfo.isInvocable(info));
        assertFalse(KeyInfo.isInternal(info));
    }

    private TruffleObject createRListTruffleObject(String values) {
        PolyglotEngine engine = PolyglotEngine.newBuilder().build();
        Source src = Source.newBuilder("list(" + values + ")").mimeType("text/x-r").name("test.R").build();
        PolyglotEngine.Value result = engine.eval(src);
        return result.as(TruffleObject.class);
    }

}
