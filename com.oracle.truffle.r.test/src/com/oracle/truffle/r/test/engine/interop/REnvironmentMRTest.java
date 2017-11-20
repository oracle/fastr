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
import com.oracle.truffle.api.interop.java.JavaInterop;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.env.REnvironment;

public class REnvironmentMRTest extends AbstractMRTest {

    @Test
    public void testReadWrite() throws Exception {
        REnvironment e = (REnvironment) createTruffleObjects()[0];

        assertEquals("aaa", ForeignAccess.sendRead(Message.READ.createNode(), e, "s"));
        assertEquals(123, ForeignAccess.sendRead(Message.READ.createNode(), e, "i"));
        assertEquals(123.1, ForeignAccess.sendRead(Message.READ.createNode(), e, "d"));
        assertEquals(true, ForeignAccess.sendRead(Message.READ.createNode(), e, "b"));

        assertInteropException(() -> ForeignAccess.sendRead(Message.READ.createNode(), e, "nnnoooonnne"), UnknownIdentifierException.class);

        TruffleObject obj = (TruffleObject) ForeignAccess.sendWrite(Message.WRITE.createNode(), e, "s", "abc");
        Object value = ForeignAccess.sendRead(Message.READ.createNode(), obj, "s");
        assertEquals("abc", value);

        obj = (TruffleObject) ForeignAccess.sendWrite(Message.WRITE.createNode(), e, "b", false);
        REnvironment env = JavaInterop.asJavaObject(REnvironment.class, obj);
        assertEquals((byte) 0, env.get("b"));
        value = ForeignAccess.sendRead(Message.READ.createNode(), obj, "b");
        assertEquals(false, value);

        obj = (TruffleObject) ForeignAccess.sendWrite(Message.WRITE.createNode(), e, "i", (short) 1234);
        value = ForeignAccess.sendRead(Message.READ.createNode(), obj, "i");
        assertTrue(value instanceof Integer);
        assertEquals(1234, value);

        obj = (TruffleObject) ForeignAccess.sendWrite(Message.WRITE.createNode(), e, "newnew", "nneeww");
        value = ForeignAccess.sendRead(Message.READ.createNode(), obj, "newnew");
        assertEquals("nneeww", value);

        assertInteropException(() -> ForeignAccess.sendWrite(Message.WRITE.createNode(), e, "l", 667), UnsupportedMessageException.class);

        e.lock(true);
        assertInteropException(() -> ForeignAccess.sendWrite(Message.WRITE.createNode(), e, "newnew2", "nneeww"), UnsupportedMessageException.class);
        e.lock(false);

        // can't read/write via index
        assertInteropException(() -> ForeignAccess.sendRead(Message.READ.createNode(), e, 0), UnknownIdentifierException.class);
        assertInteropException(() -> ForeignAccess.sendRead(Message.READ.createNode(), e, 1f), UnknownIdentifierException.class);
        assertInteropException(() -> ForeignAccess.sendWrite(Message.WRITE.createNode(), e, 1, 321), UnknownIdentifierException.class);
    }

    @Test
    public void testKeysInfo() throws Exception {
        TruffleObject e = createTruffleObjects()[0];
        int info = ForeignAccess.sendKeyInfo(Message.KEY_INFO.createNode(), e, "nnoonnee");
        assertFalse(KeyInfo.isExisting(info));

        info = ForeignAccess.sendKeyInfo(Message.KEY_INFO.createNode(), e, 0);
        assertFalse(KeyInfo.isExisting(info));

        info = ForeignAccess.sendKeyInfo(Message.KEY_INFO.createNode(), e, 1f);
        assertFalse(KeyInfo.isExisting(info));

        info = ForeignAccess.sendKeyInfo(Message.KEY_INFO.createNode(), e, "s");
        assertTrue(KeyInfo.isExisting(info));
        assertTrue(KeyInfo.isReadable(info));
        assertTrue(KeyInfo.isWritable(info));
        assertFalse(KeyInfo.isInvocable(info));
        assertFalse(KeyInfo.isInternal(info));

        info = ForeignAccess.sendKeyInfo(Message.KEY_INFO.createNode(), e, "b");
        assertTrue(KeyInfo.isExisting(info));
        assertTrue(KeyInfo.isReadable(info));
        assertTrue(KeyInfo.isWritable(info));
        assertFalse(KeyInfo.isInvocable(info));
        assertFalse(KeyInfo.isInternal(info));

        info = ForeignAccess.sendKeyInfo(Message.KEY_INFO.createNode(), e, "fn");
        assertTrue(KeyInfo.isExisting(info));
        assertTrue(KeyInfo.isReadable(info));
        assertTrue(KeyInfo.isWritable(info));
        assertTrue(KeyInfo.isInvocable(info));
        assertFalse(KeyInfo.isInternal(info));

        info = ForeignAccess.sendKeyInfo(Message.KEY_INFO.createNode(), e, "n");
        assertTrue(KeyInfo.isExisting(info));
        assertTrue(KeyInfo.isReadable(info));
        assertTrue(KeyInfo.isWritable(info));
        assertFalse(KeyInfo.isInvocable(info));
        assertFalse(KeyInfo.isInternal(info));

        info = ForeignAccess.sendKeyInfo(Message.KEY_INFO.createNode(), e, "l");
        assertTrue(KeyInfo.isExisting(info));
        assertTrue(KeyInfo.isReadable(info));
        assertFalse(KeyInfo.isWritable(info));
        assertFalse(KeyInfo.isInvocable(info));
        assertFalse(KeyInfo.isInternal(info));
    }

    @Override
    protected TruffleObject[] createTruffleObjects() throws Exception {
        Source src = Source.newBuilder("e <- new.env(); e$s <- 'aaa'; e$i <- 123L; e$d <- 123.1; e$b <- TRUE; e$fn <- function() {}; e$n <- NULL; e$l <- 666; lockBinding('l', e); e").mimeType(
                        "text/x-r").name("test.R").build();
        return new TruffleObject[]{engine.eval(src).as(REnvironment.class)};
    }

    @Override
    protected String[] getKeys(TruffleObject obj) {
        if (((REnvironment) obj).getName().equals("R_EmptyEnv")) {
            return new String[]{};
        } else {
            return new String[]{"s", "i", "d", "b", "fn", "n", "l"};
        }
    }

    @Override
    protected TruffleObject createEmptyTruffleObject() throws Exception {
        return REnvironment.emptyEnv();
    }
}
