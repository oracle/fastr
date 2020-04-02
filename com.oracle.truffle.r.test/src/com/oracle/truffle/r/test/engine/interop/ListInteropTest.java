/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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

import org.graalvm.polyglot.Value;
import org.junit.Test;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.r.runtime.data.RExpression;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.test.generate.FastRSession;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ListInteropTest extends AbstractInteropTest {

    private static final String testValues = "i=1L, r=as.raw(1), d=2.1, b=TRUE, fn=function(s) {s}, n=NULL, 4";

    @Override
    protected boolean canRead(@SuppressWarnings("unused") TruffleObject obj) {
        return true;
    }

    @Test
    public void testKeysRead() throws Exception {
        ListInteropTest.this.testKeysRead("list");
        ListInteropTest.this.testKeysRead("pairlist");
    }

    private void testKeysRead(String createFun) throws Exception {
        RAbstractContainer l = create(createFun, testValues);

        assertSingletonVector(1, getInterop().readMember(l, "i"));
        assertSingletonVector(2.1, getInterop().readMember(l, "d"));
        assertSingletonVector(true, getInterop().readMember(l, "b"));
        assertTrue(getInterop().readMember(l, "n") instanceof RNull);

        assertSingletonVector(1, getInterop().readArrayElement(l, 0));
        assertSingletonVector((byte) 1, getInterop().readArrayElement(l, 1));
        assertSingletonVector(2.1, getInterop().readArrayElement(l, 2));
        assertSingletonVector(4d, getInterop().readArrayElement(l, 6));
        assertSingletonVector(true, getInterop().readArrayElement(l, 3));
        assertTrue(getInterop().readArrayElement(l, 5) instanceof RNull);

        assertInteropException(() -> getInterop().readArrayElement(l, -1), InvalidArrayIndexException.class);

        assertInteropException(() -> getInterop().readMember(l, "nnnoooonnne"), UnknownIdentifierException.class);
        assertInteropException(() -> getInterop().readArrayElement(l, 100), InvalidArrayIndexException.class);
    }

    @Test
    public void testKeysInfo() {
        testKeysInfo("list");
        testKeysInfo("pairlist");
    }

    @Test
    public void testInvokeMember() throws Exception {
        assertSingletonVector(true, InteropLibrary.getFactory().getUncached().invokeMember(create("list", testValues), "fn", true));
        assertSingletonVector(true, InteropLibrary.getFactory().getUncached().invokeMember(create("pairlist", testValues), "fn", true));
    }

    public void testKeysInfo(String createFun) {
        RAbstractContainer l = create(createFun, testValues);

        assertFalse(getInterop().isMemberExisting(l, "nnoonnee"));
        assertFalse(getInterop().isMemberInsertable(l, "nnoonnee"));
        assertFalse(getInterop().isMemberInternal(l, "nnoonnee"));
        assertFalse(getInterop().isMemberInvocable(l, "nnoonnee"));
        assertFalse(getInterop().isMemberModifiable(l, "nnoonnee"));
        assertFalse(getInterop().isMemberReadable(l, "nnoonnee"));
        assertFalse(getInterop().isMemberRemovable(l, "nnoonnee"));
        assertFalse(getInterop().isMemberWritable(l, "nnoonnee"));
        assertFalse(getInterop().hasMemberReadSideEffects(l, "nnoonnee"));
        assertFalse(getInterop().hasMemberWriteSideEffects(l, "nnoonnee"));

        assertTrue(getInterop().isMemberExisting(l, "d"));
        assertFalse(getInterop().isMemberInsertable(l, "d"));
        assertFalse(getInterop().isMemberInternal(l, "d"));
        assertFalse(getInterop().isMemberInvocable(l, "d"));
        assertFalse(getInterop().isMemberModifiable(l, "d"));
        assertTrue(getInterop().isMemberReadable(l, "d"));
        assertFalse(getInterop().isMemberRemovable(l, "d"));
        assertFalse(getInterop().isMemberWritable(l, "d"));
        assertFalse(getInterop().hasMemberReadSideEffects(l, "d"));
        assertFalse(getInterop().hasMemberWriteSideEffects(l, "d"));

        assertTrue(getInterop().isMemberExisting(l, "fn"));
        assertFalse(getInterop().isMemberInsertable(l, "fn"));
        assertFalse(getInterop().isMemberInternal(l, "fn"));
        assertTrue(getInterop().isMemberInvocable(l, "fn"));
        assertFalse(getInterop().isMemberModifiable(l, "fn"));
        assertTrue(getInterop().isMemberReadable(l, "fn"));
        assertFalse(getInterop().isMemberRemovable(l, "fn"));
        assertFalse(getInterop().isMemberWritable(l, "fn"));
        assertFalse(getInterop().hasMemberReadSideEffects(l, "fn"));
        assertFalse(getInterop().hasMemberWriteSideEffects(l, "fn"));

        assertFalse(getInterop().isArrayElementExisting(l, -1));
        assertFalse(getInterop().isArrayElementInsertable(l, -1));
        assertFalse(getInterop().isArrayElementModifiable(l, -1));
        assertFalse(getInterop().isArrayElementReadable(l, -1));
        assertFalse(getInterop().isArrayElementRemovable(l, -1));
        assertFalse(getInterop().isArrayElementWritable(l, -1));

        assertFalse(getInterop().isArrayElementExisting(l, l.getLength()));
        assertFalse(getInterop().isArrayElementInsertable(l, l.getLength()));
        assertFalse(getInterop().isArrayElementModifiable(l, l.getLength()));
        assertFalse(getInterop().isArrayElementReadable(l, l.getLength()));
        assertFalse(getInterop().isArrayElementRemovable(l, l.getLength()));
        assertFalse(getInterop().isArrayElementWritable(l, l.getLength()));

        assertTrue(getInterop().isArrayElementExisting(l, 0));
        assertFalse(getInterop().isArrayElementInsertable(l, 0));
        assertFalse(getInterop().isArrayElementModifiable(l, 0));
        assertTrue(getInterop().isArrayElementReadable(l, 0));
        assertFalse(getInterop().isArrayElementRemovable(l, 0));
        assertFalse(getInterop().isArrayElementWritable(l, 0));

        assertTrue(getInterop().isArrayElementExisting(l, 1));
        assertFalse(getInterop().isArrayElementInsertable(l, 1));
        assertFalse(getInterop().isArrayElementModifiable(l, 1));
        assertTrue(getInterop().isArrayElementReadable(l, 1));
        assertFalse(getInterop().isArrayElementRemovable(l, 1));
        assertFalse(getInterop().isArrayElementWritable(l, 1));
    }

    private static RAbstractContainer create(String createFun, String values) {
        String create = createFun + "(" + values + ")";
        org.graalvm.polyglot.Source src = org.graalvm.polyglot.Source.newBuilder("R", create, "<testrlist>").internal(true).buildLiteral();
        Value result = context.eval(src);
        return (RAbstractContainer) FastRSession.getReceiver(result);
    }

    @Override
    protected String[] getKeys(TruffleObject obj) {
        if (((RAbstractContainer) obj).getLength() > 0) {
            return new String[]{"i", "r", "d", "b", "fn", "n", ""};
        }
        return new String[]{};
    }

    @Override
    protected TruffleObject[] createTruffleObjects() throws Exception {
        return new TruffleObject[]{create("list", testValues), create("pairlist", testValues), create("expression", testValues)};
    }

    @Override
    protected TruffleObject createEmptyTruffleObject() throws Exception {
        // cant have an emtpy pair list
        return create("list", "");
    }

    @Override
    protected int getSize(TruffleObject obj) {
        if (obj instanceof RList) {
            return ((RList) obj).getLength();
        } else if (obj instanceof RPairList) {
            return ((RPairList) obj).getLength();
        } else if (obj instanceof RExpression) {
            return ((RExpression) obj).getLength();
        }
        fail("unexpected list type " + obj.getClass().getName());
        return -1;
    }
}
