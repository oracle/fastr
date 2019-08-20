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

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.test.generate.FastRSession;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class RArgsValuesAndNamesInteropTest extends AbstractInteropTest {

    @Override
    protected boolean canRead(@SuppressWarnings("unused") TruffleObject obj) {
        return true;
    }

    @Test
    public void testReadIdx() throws Exception {
        TruffleObject args = createTruffleObjects()[0];

        assertInteropException(() -> getInterop().readArrayElement(args, -1), InvalidArrayIndexException.class);
        assertInteropException(() -> getInterop().readArrayElement(args, 100), InvalidArrayIndexException.class);

        assertSingletonVector("abc", getInterop().readArrayElement(args, 0));
        assertSingletonVector(123, getInterop().readArrayElement(args, 1));
        assertSingletonVector(1.1, getInterop().readArrayElement(args, 2));
        assertSingletonVector(true, getInterop().readArrayElement(args, 3));
        assertTrue(getInterop().readArrayElement(args, 4) instanceof RFunction);
        assertTrue(getInterop().readArrayElement(args, 5) instanceof RNull);

        TruffleObject emptyargs = RArgsValuesAndNames.EMPTY;
        assertInteropException(() -> getInterop().readArrayElement(emptyargs, 0), InvalidArrayIndexException.class);
    }

    @Test
    public void testReadName() throws Exception {
        TruffleObject args = createTruffleObjects()[0];

        assertInteropException(() -> getInterop().readMember(args, "nnnnooooonnnneee"), UnknownIdentifierException.class);

        assertSingletonVector("abc", getInterop().readMember(args, "s"));
        assertSingletonVector(123, getInterop().readMember(args, "i"));
        assertSingletonVector(1.1, getInterop().readMember(args, "d"));
        assertSingletonVector(true, getInterop().readMember(args, "b"));
        assertTrue(getInterop().readMember(args, "fn") instanceof RFunction);
        assertTrue(getInterop().readMember(args, "n") instanceof RNull);

        TruffleObject emptyargs = RArgsValuesAndNames.EMPTY;
        assertInteropException(() -> getInterop().readMember(emptyargs, "s"), UnknownIdentifierException.class);
    }

    @Test
    public void testKeysInfo() throws Exception {
        TruffleObject e = createTruffleObjects()[0];
        assertFalse(getInterop().isMemberExisting(e, "nnoonnee"));
        assertFalse(getInterop().isMemberInsertable(e, "nnoonnee"));
        assertFalse(getInterop().isMemberInternal(e, "nnoonnee"));
        assertFalse(getInterop().isMemberInvocable(e, "nnoonnee"));
        assertFalse(getInterop().isMemberModifiable(e, "nnoonnee"));
        assertFalse(getInterop().isMemberReadable(e, "nnoonnee"));
        assertFalse(getInterop().isMemberRemovable(e, "nnoonnee"));
        assertFalse(getInterop().isMemberWritable(e, "nnoonnee"));
        assertFalse(getInterop().hasMemberReadSideEffects(e, "nnoonnee"));
        assertFalse(getInterop().hasMemberWriteSideEffects(e, "nnoonnee"));

        assertTrue(getInterop().isArrayElementExisting(e, 0));
        assertFalse(getInterop().isArrayElementInsertable(e, 0));
        assertFalse(getInterop().isArrayElementModifiable(e, 0));
        assertTrue(getInterop().isArrayElementReadable(e, 0));
        assertFalse(getInterop().isArrayElementRemovable(e, 0));
        assertFalse(getInterop().isArrayElementWritable(e, 0));

        assertTrue(getInterop().isMemberExisting(e, "s"));
        assertFalse(getInterop().isMemberInsertable(e, "s"));
        assertFalse(getInterop().isMemberInternal(e, "s"));
        assertFalse(getInterop().isMemberInvocable(e, "s"));
        assertFalse(getInterop().isMemberModifiable(e, "s"));
        assertTrue(getInterop().isMemberReadable(e, "s"));
        assertFalse(getInterop().isMemberRemovable(e, "s"));
        assertFalse(getInterop().isMemberWritable(e, "s"));
        assertFalse(getInterop().hasMemberReadSideEffects(e, "s"));
        assertFalse(getInterop().hasMemberWriteSideEffects(e, "s"));

        assertTrue(getInterop().isMemberExisting(e, "b"));
        assertFalse(getInterop().isMemberInsertable(e, "b"));
        assertFalse(getInterop().isMemberInternal(e, "b"));
        assertFalse(getInterop().isMemberInvocable(e, "b"));
        assertFalse(getInterop().isMemberModifiable(e, "b"));
        assertTrue(getInterop().isMemberReadable(e, "b"));
        assertFalse(getInterop().isMemberRemovable(e, "b"));
        assertFalse(getInterop().isMemberWritable(e, "b"));
        assertFalse(getInterop().hasMemberReadSideEffects(e, "b"));
        assertFalse(getInterop().hasMemberWriteSideEffects(e, "b"));

        assertTrue(getInterop().isMemberExisting(e, "fn"));
        assertFalse(getInterop().isMemberInsertable(e, "fn"));
        assertFalse(getInterop().isMemberInternal(e, "fn"));
        assertTrue(getInterop().isMemberInvocable(e, "fn"));
        assertFalse(getInterop().isMemberModifiable(e, "fn"));
        assertTrue(getInterop().isMemberReadable(e, "fn"));
        assertFalse(getInterop().isMemberRemovable(e, "fn"));
        assertFalse(getInterop().isMemberWritable(e, "fn"));
        assertFalse(getInterop().hasMemberReadSideEffects(e, "fn"));
        assertFalse(getInterop().hasMemberWriteSideEffects(e, "fn"));

        assertTrue(getInterop().isMemberExisting(e, "n"));
        assertFalse(getInterop().isMemberInsertable(e, "n"));
        assertFalse(getInterop().isMemberInternal(e, "n"));
        assertFalse(getInterop().isMemberInvocable(e, "n"));
        assertFalse(getInterop().isMemberModifiable(e, "n"));
        assertTrue(getInterop().isMemberReadable(e, "n"));
        assertFalse(getInterop().isMemberRemovable(e, "n"));
        assertFalse(getInterop().isMemberWritable(e, "n"));
        assertFalse(getInterop().hasMemberReadSideEffects(e, "n"));
        assertFalse(getInterop().hasMemberWriteSideEffects(e, "n"));
    }

    @Test
    public void testInvokeMember() throws Exception {
        TruffleObject o = createTruffleObjects()[0];
        assertSingletonVector(true, InteropLibrary.getFactory().getUncached().invokeMember(o, "fn", true));
    }

    private String[] names = {"s", "i", "d", "b", "fn", "n"};

    @Override
    protected TruffleObject[] createTruffleObjects() throws Exception {
        Source src = Source.newBuilder("R", "f=function(s) {s}", "<testfunction>").internal(true).buildLiteral();
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
