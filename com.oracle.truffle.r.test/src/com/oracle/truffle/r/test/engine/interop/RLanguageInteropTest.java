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

import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.junit.Assert;
import org.junit.Test;

import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.test.generate.FastRSession;

public class RLanguageInteropTest extends AbstractInteropTest {

    @Override
    protected boolean canRead(@SuppressWarnings("unused") TruffleObject obj) {
        return true;
    }

    @Test
    public void testKeysInfo() throws Exception {
        RPairList rl = (RPairList) createTruffleObjects()[0];
        assertFalse(getInterop().isMemberExisting(rl, "nnoonnee"));
        assertFalse(getInterop().isMemberInsertable(rl, "nnoonnee"));
        assertFalse(getInterop().isMemberInternal(rl, "nnoonnee"));
        assertFalse(getInterop().isMemberInvocable(rl, "nnoonnee"));
        assertFalse(getInterop().isMemberModifiable(rl, "nnoonnee"));
        assertFalse(getInterop().isMemberReadable(rl, "nnoonnee"));
        assertFalse(getInterop().isMemberRemovable(rl, "nnoonnee"));
        assertFalse(getInterop().isMemberWritable(rl, "nnoonnee"));
        assertFalse(getInterop().hasMemberReadSideEffects(rl, "nnoonnee"));
        assertFalse(getInterop().hasMemberWriteSideEffects(rl, "nnoonnee"));

        assertFalse(getInterop().isArrayElementExisting(rl, rl.getLength()));
        assertFalse(getInterop().isArrayElementInsertable(rl, rl.getLength()));
        assertFalse(getInterop().isArrayElementModifiable(rl, rl.getLength()));
        assertFalse(getInterop().isArrayElementReadable(rl, rl.getLength()));
        assertFalse(getInterop().isArrayElementRemovable(rl, rl.getLength()));
        assertFalse(getInterop().isArrayElementWritable(rl, rl.getLength()));

        assertTrue(getInterop().isArrayElementExisting(rl, 0));
        assertFalse(getInterop().isArrayElementInsertable(rl, 0));
        assertFalse(getInterop().isArrayElementModifiable(rl, 0));
        assertTrue(getInterop().isArrayElementReadable(rl, 0));
        assertFalse(getInterop().isArrayElementRemovable(rl, 0));
        assertFalse(getInterop().isArrayElementWritable(rl, 0));

        assertTrue(getInterop().isArrayElementExisting(rl, 1));
        assertFalse(getInterop().isArrayElementInsertable(rl, 1));
        assertFalse(getInterop().isArrayElementModifiable(rl, 1));
        assertTrue(getInterop().isArrayElementReadable(rl, 1));
        assertFalse(getInterop().isArrayElementRemovable(rl, 1));
        assertFalse(getInterop().isArrayElementWritable(rl, 1));

        assertTrue(getInterop().isArrayElementExisting(rl, 2));
        assertFalse(getInterop().isArrayElementInsertable(rl, 2));
        assertFalse(getInterop().isArrayElementModifiable(rl, 2));
        assertTrue(getInterop().isArrayElementReadable(rl, 2));
        assertFalse(getInterop().isArrayElementRemovable(rl, 2));
        assertFalse(getInterop().isArrayElementWritable(rl, 2));
    }

    @Test
    public void testRead() throws Exception {
        RPairList rl = (RPairList) createTruffleObjects()[0];

        assertInteropException(() -> getInterop().readMember(rl, "nnnoooonnne"), UnknownIdentifierException.class);
        assertInteropException(() -> getInterop().readArrayElement(rl, rl.getLength()), InvalidArrayIndexException.class);
        assertTrue(getInterop().readArrayElement(rl, 0) == RDataFactory.createSymbolInterned("+"));

        // TODO add some meaningful read tests
        Assert.assertNotNull(getInterop().readArrayElement(rl, 0));
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
