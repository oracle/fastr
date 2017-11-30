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
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.test.generate.FastRSession;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class RArgsValuesAndNamesMRTest extends AbstractMRTest {

    @Test
    public void testReadWriteIdx() throws Exception {
        TruffleObject args = createTruffleObjects()[0];

        assertInteropException(() -> ForeignAccess.sendRead(Message.READ.createNode(), args, 1f), UnknownIdentifierException.class);
        assertInteropException(() -> ForeignAccess.sendRead(Message.READ.createNode(), args, 1d), UnknownIdentifierException.class);
        assertInteropException(() -> ForeignAccess.sendRead(Message.READ.createNode(), args, -1), UnknownIdentifierException.class);
        assertInteropException(() -> ForeignAccess.sendRead(Message.READ.createNode(), args, 100), UnknownIdentifierException.class);

        assertEquals("abc", ForeignAccess.sendRead(Message.READ.createNode(), args, 0));
        assertEquals(123, ForeignAccess.sendRead(Message.READ.createNode(), args, 1));
        assertEquals(1.1, ForeignAccess.sendRead(Message.READ.createNode(), args, 2));
        assertEquals(true, ForeignAccess.sendRead(Message.READ.createNode(), args, 3));
        assertTrue(ForeignAccess.sendRead(Message.READ.createNode(), args, 4) instanceof RFunction);
        assertTrue(ForeignAccess.sendRead(Message.READ.createNode(), args, 5) instanceof RNull);

        TruffleObject emptyargs = RArgsValuesAndNames.EMPTY;
        assertInteropException(() -> ForeignAccess.sendRead(Message.READ.createNode(), emptyargs, 0), UnknownIdentifierException.class);

        assertInteropException(() -> ForeignAccess.sendWrite(Message.WRITE.createNode(), args, 0, 321), UnsupportedMessageException.class);
    }

    @Test
    public void testReadWriteName() throws Exception {
        TruffleObject args = createTruffleObjects()[0];

        assertInteropException(() -> ForeignAccess.sendRead(Message.READ.createNode(), args, "nnnnooooonnnneee"), UnknownIdentifierException.class);

        assertEquals("abc", ForeignAccess.sendRead(Message.READ.createNode(), args, "s"));
        assertEquals(123, ForeignAccess.sendRead(Message.READ.createNode(), args, "i"));
        assertEquals(1.1, ForeignAccess.sendRead(Message.READ.createNode(), args, "d"));
        assertEquals(true, ForeignAccess.sendRead(Message.READ.createNode(), args, "b"));
        assertTrue(ForeignAccess.sendRead(Message.READ.createNode(), args, "fn") instanceof RFunction);
        assertTrue(ForeignAccess.sendRead(Message.READ.createNode(), args, "n") instanceof RNull);

        TruffleObject emptyargs = RArgsValuesAndNames.EMPTY;
        assertInteropException(() -> ForeignAccess.sendRead(Message.READ.createNode(), emptyargs, "s"), UnknownIdentifierException.class);

        assertInteropException(() -> ForeignAccess.sendWrite(Message.WRITE.createNode(), args, "s", "aaa"), UnsupportedMessageException.class);
    }

    @Test
    public void testKeysInfo() throws Exception {
        TruffleObject e = createTruffleObjects()[0];
        int info = ForeignAccess.sendKeyInfo(Message.KEY_INFO.createNode(), e, "nnoonnee");
        assertFalse(KeyInfo.isExisting(info));

        info = ForeignAccess.sendKeyInfo(Message.KEY_INFO.createNode(), e, 1f);
        assertFalse(KeyInfo.isExisting(info));

        info = ForeignAccess.sendKeyInfo(Message.KEY_INFO.createNode(), e, 0);
        assertTrue(KeyInfo.isExisting(info));
        assertTrue(KeyInfo.isReadable(info));
        assertFalse(KeyInfo.isWritable(info));
        assertFalse(KeyInfo.isInvocable(info));
        assertFalse(KeyInfo.isInternal(info));

        info = ForeignAccess.sendKeyInfo(Message.KEY_INFO.createNode(), e, "s");
        assertTrue(KeyInfo.isExisting(info));
        assertTrue(KeyInfo.isReadable(info));
        assertFalse(KeyInfo.isWritable(info));
        assertFalse(KeyInfo.isInvocable(info));
        assertFalse(KeyInfo.isInternal(info));

        info = ForeignAccess.sendKeyInfo(Message.KEY_INFO.createNode(), e, "b");
        assertTrue(KeyInfo.isExisting(info));
        assertTrue(KeyInfo.isReadable(info));
        assertFalse(KeyInfo.isWritable(info));
        assertFalse(KeyInfo.isInvocable(info));
        assertFalse(KeyInfo.isInternal(info));

        info = ForeignAccess.sendKeyInfo(Message.KEY_INFO.createNode(), e, "fn");
        assertTrue(KeyInfo.isExisting(info));
        assertTrue(KeyInfo.isReadable(info));
        assertFalse(KeyInfo.isWritable(info));
        assertTrue(KeyInfo.isInvocable(info));
        assertFalse(KeyInfo.isInternal(info));

        info = ForeignAccess.sendKeyInfo(Message.KEY_INFO.createNode(), e, "n");
        assertTrue(KeyInfo.isExisting(info));
        assertTrue(KeyInfo.isReadable(info));
        assertFalse(KeyInfo.isWritable(info));
        assertFalse(KeyInfo.isInvocable(info));
        assertFalse(KeyInfo.isInternal(info));
    }

    private String[] names = {"s", "i", "d", "b", "fn", "n"};

    @Override
    protected TruffleObject[] createTruffleObjects() throws Exception {
        Source src = Source.newBuilder("R", "f=function() {}", "<testfunction>").internal(true).buildLiteral();
        Value result = context.eval(src);
        RFunction fn = (RFunction) FastRSession.getReceiver(result);
        Object[] values = {"abc", 123, 1.1, RRuntime.asLogical(true), fn, RNull.instance};
        return new TruffleObject[]{new RArgsValuesAndNames(values, ArgumentsSignature.get(names))};
    }

    @Override
    protected String[] getKeys(TruffleObject obj) {
        if (obj == RArgsValuesAndNames.EMPTY) {
            return new String[]{};
        } else {
            return names;
        }
    }

    @Override
    protected TruffleObject createEmptyTruffleObject() throws Exception {
        return RArgsValuesAndNames.EMPTY;
    }

    @Override
    protected int getSize(TruffleObject obj) {
        return ((RArgsValuesAndNames) obj).getLength();
    }
}
