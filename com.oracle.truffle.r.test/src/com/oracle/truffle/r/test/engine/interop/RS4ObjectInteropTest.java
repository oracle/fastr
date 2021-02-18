/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.junit.Ignore;
import org.junit.Test;

import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.data.RS4Object;
import com.oracle.truffle.r.test.generate.FastRSession;

public class RS4ObjectInteropTest extends AbstractInteropTest {

    // TODO: NewRVersionMigration
    @Ignore
    @Test
    public void testKeysInfo() throws Exception {
        TruffleObject s4 = createTruffleObjects()[0];
        assertFalse(getInterop().isMemberExisting(s4, "nnoonnee"));
        assertFalse(getInterop().isMemberReadable(s4, "nnoonnee"));
        assertFalse(getInterop().isMemberInsertable(s4, "nnoonnee"));
        assertFalse(getInterop().isMemberInternal(s4, "nnoonnee"));
        assertFalse(getInterop().isMemberInvocable(s4, "nnoonnee"));
        assertFalse(getInterop().isMemberModifiable(s4, "nnoonnee"));
        assertFalse(getInterop().isMemberRemovable(s4, "nnoonnee"));
        assertFalse(getInterop().isMemberWritable(s4, "nnoonnee"));
        assertFalse(getInterop().hasMemberReadSideEffects(s4, "nnoonnee"));
        assertFalse(getInterop().hasMemberWriteSideEffects(s4, "nnoonnee"));

        assertFalse(getInterop().isArrayElementExisting(s4, 0));
        assertFalse(getInterop().isArrayElementInsertable(s4, 0));
        assertFalse(getInterop().isArrayElementModifiable(s4, 0));
        assertFalse(getInterop().isArrayElementReadable(s4, 0));
        assertFalse(getInterop().isArrayElementRemovable(s4, 0));
        assertFalse(getInterop().isArrayElementWritable(s4, 0));

        assertTrue(getInterop().isMemberExisting(s4, "s"));
        assertFalse(getInterop().isMemberInsertable(s4, "s"));
        assertFalse(getInterop().isMemberInternal(s4, "s"));
        assertFalse(getInterop().isMemberInvocable(s4, "s"));
        assertTrue(getInterop().isMemberModifiable(s4, "s"));
        assertTrue(getInterop().isMemberReadable(s4, "s"));
        assertFalse(getInterop().isMemberRemovable(s4, "s"));
        assertTrue(getInterop().isMemberWritable(s4, "s"));
        assertFalse(getInterop().hasMemberReadSideEffects(s4, "s"));
        assertFalse(getInterop().hasMemberWriteSideEffects(s4, "s"));

        assertTrue(getInterop().isMemberExisting(s4, "b"));
        assertFalse(getInterop().isMemberInsertable(s4, "b"));
        assertFalse(getInterop().isMemberInvocable(s4, "b"));
        assertFalse(getInterop().isMemberInternal(s4, "b"));
        assertTrue(getInterop().isMemberModifiable(s4, "b"));
        assertTrue(getInterop().isMemberReadable(s4, "b"));
        assertFalse(getInterop().isMemberRemovable(s4, "b"));
        assertTrue(getInterop().isMemberWritable(s4, "b"));
        assertFalse(getInterop().hasMemberReadSideEffects(s4, "b"));
        assertFalse(getInterop().hasMemberWriteSideEffects(s4, "b"));

        assertTrue(getInterop().isMemberExisting(s4, "fn"));
        assertFalse(getInterop().isMemberInsertable(s4, "fn"));
        assertFalse(getInterop().isMemberInternal(s4, "fn"));
        assertTrue(getInterop().isMemberInvocable(s4, "fn"));
        assertTrue(getInterop().isMemberModifiable(s4, "fn"));
        assertTrue(getInterop().isMemberReadable(s4, "fn"));
        assertFalse(getInterop().isMemberRemovable(s4, "fn"));
        assertTrue(getInterop().isMemberWritable(s4, "fn"));
        assertFalse(getInterop().hasMemberReadSideEffects(s4, "fn"));
        assertFalse(getInterop().hasMemberWriteSideEffects(s4, "fn"));

        assertTrue(getInterop().isMemberExisting(s4, "class"));
        assertFalse(getInterop().isMemberInsertable(s4, "class"));
        assertFalse(getInterop().isMemberInternal(s4, "class"));
        assertFalse(getInterop().isMemberInvocable(s4, "class"));
        assertFalse(getInterop().isMemberModifiable(s4, "class"));
        assertTrue(getInterop().isMemberReadable(s4, "class"));
        assertFalse(getInterop().isMemberRemovable(s4, "class"));
        assertFalse(getInterop().isMemberWritable(s4, "class"));
        assertFalse(getInterop().hasMemberReadSideEffects(s4, "class"));
        assertFalse(getInterop().hasMemberWriteSideEffects(s4, "class"));
    }

    @Override
    protected boolean canRead(@SuppressWarnings("unused") TruffleObject obj) {
        return true;
    }

    @Override
    protected boolean canWrite(@SuppressWarnings("unused") TruffleObject obj) {
        return true;
    }

    // TODO: NewRVersionMigration
    @Ignore
    @Test
    public void testReadWrite() throws Exception {
        TruffleObject s4 = createTruffleObjects()[0];

        assertSingletonVector("aaa", getInterop().readMember(s4, "s"));
        assertSingletonVector(123, getInterop().readMember(s4, "i"));
        assertSingletonVector(1.1, getInterop().readMember(s4, "d"));
        assertSingletonVector(true, getInterop().readMember(s4, "b"));

        assertInteropException(() -> getInterop().readMember(s4, "nnnoooonnne"), UnknownIdentifierException.class);
        assertInteropException(() -> getInterop().readArrayElement(s4, 0), UnsupportedMessageException.class);
        assertInteropException(() -> getInterop().readArrayElement(s4, 1), UnsupportedMessageException.class);

        assertInteropException(() -> {
            getInterop().writeMember(s4, "class", "cantchangeclass");
            return null;
        }, UnsupportedMessageException.class);

        assertInteropException(() -> {
            getInterop().writeMember(s4, "nnnoooonnne", "newvalue");
            return null;
        }, UnsupportedMessageException.class);

        // TODO this should fail !!!
        assertInteropException(() -> {
            getInterop().writeMember(s4, "i", "cant write string into int slot");
            return null;
        }, RError.class);

        getInterop().writeMember(s4, "s", "abc");
        Object value = getInterop().readMember(s4, "s");
        assertSingletonVector("abc", value);
        assertEquals("abc", getInterop().readArrayElement(value, 0));

        getInterop().writeMember(s4, "b", false);
        value = getInterop().readMember(s4, "b");
        assertSingletonVector(false, value);
        assertEquals(false, getInterop().readArrayElement(value, 0));

        getInterop().writeMember(s4, "i", (short) 1234);
        value = getInterop().readMember(s4, "i");
        assertSingletonVector(1234, value);
        value = getInterop().readArrayElement(value, 0);
        assertTrue(value instanceof Integer);
        assertEquals(1234, value);

    }

    @Override
    protected TruffleObject[] createTruffleObjects() {
        String srcTxt = "setClass('test', representation(s = 'character', d = 'numeric', i = 'integer', b = 'logical', fn = 'function'));" +
                        "new('test', s = 'aaa', d = 1.1, i=123L, b = TRUE, fn = function() {})";
        Source src = Source.newBuilder("R", srcTxt, "<testS4object>").internal(true).buildLiteral();
        Value result = context.eval(src);
        Object s = FastRSession.getReceiver(result);
        RS4Object s4 = (RS4Object) s;
        return new TruffleObject[]{s4};
    }

    @Override
    protected String[] getKeys(TruffleObject obj) {
        return new String[]{"s", "d", "i", "b", "fn", "class"};
    }

    @Override
    protected TruffleObject createEmptyTruffleObject() throws Exception {
        return null;
    }
}
