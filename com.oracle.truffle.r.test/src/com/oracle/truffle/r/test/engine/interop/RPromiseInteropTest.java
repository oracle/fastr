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

import com.oracle.truffle.api.interop.InteropException;
import org.graalvm.polyglot.Value;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RExpression;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.test.generate.FastRSession;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;

public class RPromiseInteropTest extends AbstractInteropTest {

    // c&p from RPromise
    private static final String MEMBER_VALUE = "value";
    private static final String MEMBER_IS_EVALUATED = "isEvaluated";
    private static final String MEMBER_EXPR = "expression";

    @Override
    protected boolean canRead(@SuppressWarnings("unused") TruffleObject obj) {
        return false;
    }

    @Override
    protected Class<? extends InteropException> readException(TruffleObject obj, String member) {
        return UnknownIdentifierException.class;
    }

    @Override
    protected Class<? extends InteropException> writeException(TruffleObject obj, String member) {
        return UnknownIdentifierException.class;
    }

    @Test
    public void testReadWrite() throws Exception {

        TruffleObject p = createRPromise("i<-1L");

        Object obj = getInterop().readMember(p, MEMBER_EXPR);
        assertTrue(((RPairList) obj).isLanguage());

        assertFalse(RRuntime.fromLogical((byte) getInterop().readMember(p, MEMBER_IS_EVALUATED)));
        assertEquals(RNull.instance, getInterop().readMember(p, MEMBER_VALUE));

        getInterop().writeMember(p, MEMBER_IS_EVALUATED, true);
        assertTrue(RRuntime.fromLogical((byte) getInterop().readMember(p, MEMBER_IS_EVALUATED)));
        assertEquals(1, getInterop().readMember(p, MEMBER_VALUE));
    }

    @Test
    public void testKeyInfo() throws Exception {
        TruffleObject p = createRPromise("i<-1L");

        assertTrue(getInterop().isMemberExisting(p, MEMBER_VALUE));
        assertTrue(getInterop().isMemberReadable(p, MEMBER_VALUE));
        assertFalse(getInterop().isMemberWritable(p, MEMBER_VALUE));
        assertFalse(getInterop().isMemberModifiable(p, MEMBER_VALUE));
        assertFalse(getInterop().isMemberInsertable(p, MEMBER_VALUE));
        assertFalse(getInterop().isMemberRemovable(p, MEMBER_VALUE));
        assertFalse(getInterop().isMemberInvocable(p, MEMBER_VALUE));
        assertFalse(getInterop().isMemberInternal(p, MEMBER_VALUE));
        assertFalse(getInterop().hasMemberReadSideEffects(p, MEMBER_VALUE));
        assertFalse(getInterop().hasMemberWriteSideEffects(p, MEMBER_VALUE));

        assertTrue(getInterop().isMemberExisting(p, MEMBER_EXPR));
        assertTrue(getInterop().isMemberReadable(p, MEMBER_EXPR));
        assertFalse(getInterop().isMemberWritable(p, MEMBER_EXPR));
        assertFalse(getInterop().isMemberModifiable(p, MEMBER_EXPR));
        assertFalse(getInterop().isMemberInsertable(p, MEMBER_EXPR));
        assertFalse(getInterop().isMemberRemovable(p, MEMBER_EXPR));
        assertFalse(getInterop().isMemberInvocable(p, MEMBER_EXPR));
        assertFalse(getInterop().isMemberInternal(p, MEMBER_EXPR));
        assertFalse(getInterop().hasMemberReadSideEffects(p, MEMBER_EXPR));
        assertFalse(getInterop().hasMemberWriteSideEffects(p, MEMBER_EXPR));

        assertTrue(getInterop().isMemberExisting(p, MEMBER_IS_EVALUATED));
        assertTrue(getInterop().isMemberReadable(p, MEMBER_IS_EVALUATED));
        assertTrue(getInterop().isMemberWritable(p, MEMBER_IS_EVALUATED));
        assertTrue(getInterop().isMemberModifiable(p, MEMBER_IS_EVALUATED));
        assertFalse(getInterop().isMemberInsertable(p, MEMBER_IS_EVALUATED));
        assertFalse(getInterop().isMemberRemovable(p, MEMBER_IS_EVALUATED));
        assertFalse(getInterop().isMemberInvocable(p, MEMBER_IS_EVALUATED));
        assertFalse(getInterop().isMemberInternal(p, MEMBER_IS_EVALUATED));
        assertFalse(getInterop().hasMemberReadSideEffects(p, MEMBER_IS_EVALUATED));
        assertFalse(getInterop().hasMemberWriteSideEffects(p, MEMBER_IS_EVALUATED));
    }

    @Override
    protected String[] getKeys(TruffleObject obj) {
        return new String[]{MEMBER_VALUE, MEMBER_EXPR, MEMBER_IS_EVALUATED};
    }

    @Override
    protected TruffleObject[] createTruffleObjects() throws Exception {
        return new TruffleObject[]{createRPromise("i<-1L")};
    }

    protected TruffleObject createRPromise(String value) throws Exception {
        RPairList expr = createExpression(value);
        return RDataFactory.createPromise(RPromise.PromiseState.Default, expr.getClosure(), REnvironment.globalEnv().getFrame());
    }

    protected RPairList createExpression(String value) throws Exception {
        String create = "expression(" + value + ")";
        org.graalvm.polyglot.Source src = org.graalvm.polyglot.Source.newBuilder("R", create, "<testrlist>").internal(true).buildLiteral();
        Value result = context.eval(src);
        return (RPairList) ((RExpression) FastRSession.getReceiver(result)).getDataAt(0);
    }

    @Override
    protected TruffleObject createEmptyTruffleObject() throws Exception {
        return null;
    }

    @Override
    protected int getSize(TruffleObject obj) {
        if (obj instanceof RExpression) {
            return ((RExpression) obj).getLength();
        }
        fail("unexpected type " + obj.getClass().getName());
        return -1;
    }
}
