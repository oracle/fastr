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

import org.junit.Assert;
import org.junit.Test;

import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.KeyInfo;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.vm.PolyglotEngine;
import com.oracle.truffle.r.runtime.data.RLanguage;

public class RLanguageMRTest extends AbstractMRTest {

    @Test
    public void testKeysInfo() throws Exception {
        RLanguage rl = (RLanguage) createTruffleObjects()[0];
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
    public void testReadWrite() throws Exception {
        RLanguage rl = (RLanguage) createTruffleObjects()[0];

        assertInteropException(() -> ForeignAccess.sendRead(Message.READ.createNode(), rl, "nnnoooonnne"), UnknownIdentifierException.class);
        assertInteropException(() -> ForeignAccess.sendRead(Message.READ.createNode(), rl, rl.getLength()), UnknownIdentifierException.class);
        assertInteropException(() -> ForeignAccess.sendRead(Message.READ.createNode(), rl, 0d), UnknownIdentifierException.class);
        assertInteropException(() -> ForeignAccess.sendRead(Message.READ.createNode(), rl, 0f), UnknownIdentifierException.class);

        // TODO add some meaningfull read tests
        Assert.assertNotNull(ForeignAccess.sendRead(Message.READ.createNode(), rl, 0));

        assertInteropException(() -> ForeignAccess.sendWrite(Message.WRITE.createNode(), rl, "aaa", "abc"), UnsupportedMessageException.class);
        assertInteropException(() -> ForeignAccess.sendWrite(Message.WRITE.createNode(), rl, 0, "abc"), UnsupportedMessageException.class);

    }

    @Override
    protected TruffleObject[] createTruffleObjects() {
        // TODO any simpler way to create a RLanguage ?
        String srcTxt = "ne <- new.env(); delayedAssign('x', 1 + 2, assign.env = ne); substitute(x, ne)";
        Source src = Source.newBuilder(srcTxt).mimeType("text/x-r").name("test.R").build();
        PolyglotEngine.Value result = engine.eval(src);
        return new TruffleObject[]{result.as(RLanguage.class)};
    }

    @Override
    protected TruffleObject createEmptyTruffleObject() throws Exception {
        return null;
    }

    @Override
    protected boolean hasSize(TruffleObject obj) {
        return true;
    }

    @Override
    protected int getSize(TruffleObject obj) {
        return ((RLanguage) obj).getLength();
    }
}
