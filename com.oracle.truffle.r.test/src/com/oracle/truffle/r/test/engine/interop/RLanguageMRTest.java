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

import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.junit.Assert;
import org.junit.Test;

import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.KeyInfo;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.test.generate.FastRSession;

public class RLanguageMRTest extends AbstractMRTest {

    @Override
    protected boolean canRead(@SuppressWarnings("unused") TruffleObject obj) {
        return true;
    }

    @Test
    public void testKeysInfo() throws Exception {
        RPairList rl = (RPairList) createTruffleObjects()[0];
        int info = ForeignAccess.sendKeyInfo(Message.KEY_INFO.createNode(), rl, "nnoonnee");
        assertFalse(KeyInfo.isExisting(info));

        info = ForeignAccess.sendKeyInfo(Message.KEY_INFO.createNode(), rl, 1f);
        assertFalse(KeyInfo.isExisting(info));

        info = ForeignAccess.sendKeyInfo(Message.KEY_INFO.createNode(), rl, rl.getLength());
        assertFalse(KeyInfo.isExisting(info));

        info = ForeignAccess.sendKeyInfo(Message.KEY_INFO.createNode(), rl, 0);
        assertTrue(KeyInfo.isExisting(info));
        assertTrue(KeyInfo.isReadable(info));
        assertFalse(KeyInfo.isWritable(info));
        assertFalse(KeyInfo.isInvocable(info));
        assertFalse(KeyInfo.isInternal(info));

        info = ForeignAccess.sendKeyInfo(Message.KEY_INFO.createNode(), rl, 1);
        assertTrue(KeyInfo.isExisting(info));
        assertTrue(KeyInfo.isReadable(info));
        assertFalse(KeyInfo.isWritable(info));
        assertFalse(KeyInfo.isInvocable(info));
        assertFalse(KeyInfo.isInternal(info));

        info = ForeignAccess.sendKeyInfo(Message.KEY_INFO.createNode(), rl, 2);
        assertTrue(KeyInfo.isExisting(info));
        assertTrue(KeyInfo.isReadable(info));
        assertFalse(KeyInfo.isWritable(info));
        assertFalse(KeyInfo.isInvocable(info));
        assertFalse(KeyInfo.isInternal(info));
    }

    @Test
    public void testRead() throws Exception {
        RPairList rl = (RPairList) createTruffleObjects()[0];

        assertInteropException(() -> ForeignAccess.sendRead(Message.READ.createNode(), rl, "nnnoooonnne"), UnknownIdentifierException.class);
        assertInteropException(() -> ForeignAccess.sendRead(Message.READ.createNode(), rl, rl.getLength()), UnknownIdentifierException.class);
        assertTrue(ForeignAccess.sendRead(Message.READ.createNode(), rl, 0d) == RDataFactory.createSymbolInterned("+"));
        assertInteropException(() -> ForeignAccess.sendRead(Message.READ.createNode(), rl, 0f), UnknownIdentifierException.class);

        // TODO add some meaningful read tests
        Assert.assertNotNull(ForeignAccess.sendRead(Message.READ.createNode(), rl, 0));
    }

    @Override
    protected TruffleObject[] createTruffleObjects() {
        String srcTxt = "quote(1+2)";
        Source src = Source.newBuilder("R", srcTxt, "<testrlanguage>").internal(true).buildLiteral();
        Value result = context.eval(src);
        return new TruffleObject[]{(TruffleObject) FastRSession.getReceiver(result)};
    }

    @Override
    protected String[] getKeys(TruffleObject obj) {
        return new String[0];
    }

    @Override
    protected TruffleObject createEmptyTruffleObject() throws Exception {
        return null;
    }

    @Override
    protected int getSize(TruffleObject obj) {
        return ((RPairList) obj).getLength();
    }
}
