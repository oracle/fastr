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

import org.junit.Test;

import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.KeyInfo;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.test.generate.FastRSession;
import org.graalvm.polyglot.Source;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class REnvironmentMRTest extends AbstractMRTest {

    @Override
    protected boolean canRead(@SuppressWarnings("unused") TruffleObject obj) {
        return true;
    }

    @Override
    protected boolean canWrite(@SuppressWarnings("unused") TruffleObject obj) {
        return true;
    }

    @Test
    public void testReadWrite() throws Exception {
        REnvironment e = (REnvironment) createTruffleObjects()[0];

        assertSingletonVector("aaa", ForeignAccess.sendRead(Message.READ.createNode(), e, "s"));
        assertSingletonVector(123, ForeignAccess.sendRead(Message.READ.createNode(), e, "i"));
        assertSingletonVector(123.1, ForeignAccess.sendRead(Message.READ.createNode(), e, "d"));
        assertSingletonVector(true, ForeignAccess.sendRead(Message.READ.createNode(), e, "b"));

        assertInteropException(() -> ForeignAccess.sendRead(Message.READ.createNode(), e, "nnnoooonnne"), UnknownIdentifierException.class);

        ForeignAccess.sendWrite(Message.WRITE.createNode(), e, "s", "abc");
        Object value = ForeignAccess.sendRead(Message.READ.createNode(), e, "s");
        assertSingletonVector("abc", value);

        ForeignAccess.sendWrite(Message.WRITE.createNode(), e, "b", false);
        value = ForeignAccess.sendRead(Message.READ.createNode(), e, "b");
        assertSingletonVector(false, value);

        ForeignAccess.sendWrite(Message.WRITE.createNode(), e, "i", (short) 1234);
        value = ForeignAccess.sendRead(Message.READ.createNode(), e, "i");
        assertSingletonVector(1234, value);

        ForeignAccess.sendWrite(Message.WRITE.createNode(), e, "newnew", "nneeww");
        value = ForeignAccess.sendRead(Message.READ.createNode(), e, "newnew");
        assertSingletonVector("nneeww", value);

        assertInteropException(() -> ForeignAccess.sendWrite(Message.WRITE.createNode(), e, "l", 667), UnsupportedMessageException.class);

        e.lock(true);
        assertInteropException(() -> ForeignAccess.sendWrite(Message.WRITE.createNode(), e, "newnew2", "nneeww"), UnsupportedMessageException.class);
        e.lock(false);

        // can't read/write via index
        assertInteropException(() -> ForeignAccess.sendRead(Message.READ.createNode(), e, 0), UnsupportedMessageException.class);
        assertInteropException(() -> ForeignAccess.sendRead(Message.READ.createNode(), e, 1f), UnsupportedMessageException.class);
        assertInteropException(() -> ForeignAccess.sendWrite(Message.WRITE.createNode(), e, 1, 321), UnsupportedMessageException.class);
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

    @Test
    public void testRemove() throws Exception {
        assertInteropException(() -> ForeignAccess.sendRemove(Message.REMOVE.createNode(), createEmptyTruffleObject(), "nnoonnee"), UnknownIdentifierException.class);

        REnvironment e = (REnvironment) createTruffleObjects()[0];
        assertInteropException(() -> ForeignAccess.sendRemove(Message.REMOVE.createNode(), e, "nnoonnee"), UnknownIdentifierException.class);

        assertSingletonVector("aaa", ForeignAccess.sendRead(Message.READ.createNode(), e, "s"));
        assertTrue(ForeignAccess.sendRemove(Message.REMOVE.createNode(), e, "s"));
        assertInteropException(() -> ForeignAccess.sendRead(Message.READ.createNode(), e, "s"), UnknownIdentifierException.class);

        e.lock(true);
        assertInteropException(() -> ForeignAccess.sendRemove(Message.REMOVE.createNode(), e, "i"), UnsupportedMessageException.class);
    }

    @Test
    public void testInvokeMember() throws Exception {
        TruffleObject o = createTruffleObjects()[0];

        int info = ForeignAccess.sendKeyInfo(Message.KEY_INFO.createNode(), o, "fn");
        assertTrue(KeyInfo.isExisting(info));
        assertTrue(KeyInfo.isReadable(info));
        assertTrue(KeyInfo.isWritable(info));
        assertTrue(KeyInfo.isInvocable(info));
        assertFalse(KeyInfo.isInternal(info));

        assertSingletonVector(true, InteropLibrary.getFactory().getUncached().invokeMember(o, "fn", true));
    }

    @Test
    public void testReadingNAAndWritingNABackKeepsNA() throws Exception {
        final TruffleObject e = createEnv("e <- new.env(); e$i <- 42; e$na <- NA_integer_; e")[0];
        Object naInteropValue = ForeignAccess.sendRead(Message.READ.createNode(), e, "na");
        ForeignAccess.sendWrite(Message.WRITE.createNode(), e, "i", naInteropValue);
        Object result = ForeignAccess.sendRead(Message.READ.createNode(), e, "i");
        assertTrue("index 0 is NA in the updated vector", RRuntime.isNA(getEnvIntValue(result)));
    }

    private static int getEnvIntValue(Object value) {
        if (value instanceof Integer) {
            return (int) value;
        } else if (value instanceof RAbstractIntVector) {
            return ((RAbstractIntVector) value).getDataAt(0);
        }
        fail("unexpected value " + value);
        return -1;
    }

    @Override
    protected TruffleObject[] createTruffleObjects() throws Exception {
        return createEnv("e <- new.env(); e$s <- 'aaa'; e$i <- 123L; e$d <- 123.1; e$b <- TRUE; e$fn <- function(s) {s}; e$n <- NULL; e$l <- 666; lockBinding('l', e); e");
    }

    private static TruffleObject[] createEnv(String txt) {
        Source src = Source.newBuilder("R", txt, "<testenv>").internal(true).buildLiteral();
        return new TruffleObject[]{(TruffleObject) FastRSession.getReceiver(context.eval(src))};
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
