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

import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.KeyInfo;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RForeignObjectWrapper;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.test.generate.FastRSession;
import java.util.HashMap;
import java.util.Map;
import org.graalvm.polyglot.Source;
import static org.junit.Assert.assertEquals;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class RForeignObjectWrapperMRTest extends AbstractMRTest {

    private RForeignObjectWrapper array;
    private RForeignObjectWrapper rwMember;
    private RForeignObjectWrapper nulll;
    private RForeignObjectWrapper executableMember;
    private RForeignObjectWrapper invokableMember;
    private RForeignObjectWrapper str;
    private RForeignObjectWrapper b;
    private RForeignObjectWrapper by;
    private RForeignObjectWrapper d;
    private RForeignObjectWrapper f;
    private RForeignObjectWrapper i;
    private RForeignObjectWrapper l;
    private RForeignObjectWrapper s;
    private RForeignObjectWrapper e;

    @Override
    protected boolean canRead(@SuppressWarnings("unused") TruffleObject obj) {
        return true;
    }

    @Override
    protected boolean canWrite(TruffleObject obj) {
        return obj != nulll;
    }

    @Override
    protected boolean shouldTestToNative(TruffleObject obj) {
        return obj == nulll;
    }

    @Override
    protected boolean isNull(TruffleObject obj) {
        return obj == nulll;
    }

    @Override
    protected boolean shouldTestKeys(TruffleObject obj) {
        return obj != str && obj != b && obj != by && obj != d && obj != f && obj != i && obj != l && obj != s;
    }

    @Override
    protected Object getUnboxed(TruffleObject obj) {
        return boxedValues.get(obj);
    }

    @Override
    protected Object[] getExecuteArguments(TruffleObject obj) {
        return new Object[]{true};
    }

    @Override
    protected Object getExecuteExpected(TruffleObject obj) {
        return true;
    }

    @Test
    public void testReadWriteByIdx() throws Exception {
        TruffleObject obj = createArray();
        assertInteropException(() -> ForeignAccess.sendRead(Message.READ.createNode(), obj, -1), UnknownIdentifierException.class);
        assertInteropException(() -> ForeignAccess.sendRead(Message.READ.createNode(), obj, 100), UnknownIdentifierException.class);

        assertEquals(1, ForeignAccess.sendRead(Message.READ.createNode(), obj, 0));
        assertEquals(2, ForeignAccess.sendRead(Message.READ.createNode(), obj, 1));
        assertEquals(3, ForeignAccess.sendRead(Message.READ.createNode(), obj, 2));

        ForeignAccess.sendWrite(Message.WRITE.createNode(), obj, 0, 111);
        assertEquals(111, ForeignAccess.sendRead(Message.READ.createNode(), obj, 0));
    }

    @Test
    public void testReadWriteByName() throws Exception {
        TruffleObject obj = createRWMembers();
        assertInteropException(() -> ForeignAccess.sendRead(Message.READ.createNode(), obj, "nnnnooooonnnneee"), UnknownIdentifierException.class);

        assertEquals(1, ForeignAccess.sendRead(Message.READ.createNode(), obj, "a"));
        assertEquals(2.0, ForeignAccess.sendRead(Message.READ.createNode(), obj, "b"));
        assertEquals("test", ForeignAccess.sendRead(Message.READ.createNode(), obj, "s"));

        ForeignAccess.sendWrite(Message.WRITE.createNode(), obj, "a", 222);
        assertEquals(222, ForeignAccess.sendRead(Message.READ.createNode(), obj, "a"));
    }

    @Test
    public void testInvoke() throws Exception {
        TruffleObject obj = createInvokable();

        int info = ForeignAccess.sendKeyInfo(Message.KEY_INFO.createNode(), obj, "nnoonnee");
        assertFalse(KeyInfo.isExisting(info));
        info = ForeignAccess.sendKeyInfo(Message.KEY_INFO.createNode(), obj, "m");
        assertTrue(KeyInfo.isExisting(info));
        assertTrue(KeyInfo.isReadable(info)); // XXX ??? readable???
        assertTrue(KeyInfo.isInvocable(info));
        assertFalse(KeyInfo.isWritable(info));
        assertFalse(KeyInfo.isInternal(info));

        assertTrue((boolean) ForeignAccess.sendInvoke(Message.INVOKE.createNode(), obj, "m", true));
    }

    @Test
    public void testKeysInfo() throws Exception {
        TruffleObject obj = createArray();
        int info = ForeignAccess.sendKeyInfo(Message.KEY_INFO.createNode(), obj, 100);
        assertFalse(KeyInfo.isExisting(info));

        info = ForeignAccess.sendKeyInfo(Message.KEY_INFO.createNode(), obj, 0);
        assertTrue(KeyInfo.isExisting(info));
        assertTrue(KeyInfo.isReadable(info));
        assertTrue(KeyInfo.isWritable(info));
        assertFalse(KeyInfo.isInvocable(info));
        assertFalse(KeyInfo.isInternal(info));
        assertFalse(KeyInfo.hasReadSideEffects(info));
        assertFalse(KeyInfo.hasWriteSideEffects(info));

        obj = createRWMembers();
        info = ForeignAccess.sendKeyInfo(Message.KEY_INFO.createNode(), obj, "nnoonnee");
        assertFalse(KeyInfo.isExisting(info));
        info = ForeignAccess.sendKeyInfo(Message.KEY_INFO.createNode(), obj, "a");
        assertTrue(KeyInfo.isExisting(info));
        assertTrue(KeyInfo.isReadable(info));
        assertTrue(KeyInfo.isWritable(info));
        assertFalse(KeyInfo.isInvocable(info));
        assertFalse(KeyInfo.isInternal(info));
        assertFalse(KeyInfo.hasReadSideEffects(info));
        assertFalse(KeyInfo.hasWriteSideEffects(info));

        obj = createEnv();
        info = ForeignAccess.sendKeyInfo(Message.KEY_INFO.createNode(), obj, "testBinding");
        assertTrue(KeyInfo.isExisting(info));
        assertTrue(KeyInfo.isReadable(info));
        assertTrue(KeyInfo.isWritable(info));
        assertTrue(KeyInfo.hasReadSideEffects(info));
        assertTrue(KeyInfo.hasWriteSideEffects(info));
        assertFalse(KeyInfo.isInvocable(info));
        assertFalse(KeyInfo.isInternal(info));
    }

    @Override
    protected TruffleObject[] createTruffleObjects() throws Exception {
        array = createArray();
        rwMember = createRWMembers();
        invokableMember = createInvokable();
        executableMember = createExecutable();
        str = createBoxedWrapper("testString");
        b = createBoxedWrapper(true);
        by = createBoxedWrapper(Byte.MAX_VALUE);
        d = createBoxedWrapper(Double.MAX_VALUE);
        f = createBoxedWrapper(Float.MAX_VALUE);
        i = createBoxedWrapper(Integer.MAX_VALUE);
        l = createBoxedWrapper(Long.MAX_VALUE);
        s = createBoxedWrapper(Short.MAX_VALUE);
        e = createEnv();
        nulll = new RForeignObjectWrapper(RNull.instance);
        return new TruffleObject[]{
                        array, rwMember, executableMember, invokableMember, str, b, by, d, f, i, l, e, nulll};
    }

    private static RForeignObjectWrapper createRWMembers() {
        TruffleLanguage.Env env = RContext.getInstance().getEnv();
        return new RForeignObjectWrapper((TruffleObject) env.asGuestValue(new TestRWMemberClass()));
    }

    private static RForeignObjectWrapper createExecutable() throws UnsupportedMessageException, UnknownIdentifierException {
        TruffleLanguage.Env env = RContext.getInstance().getEnv();
        TruffleObject to = (TruffleObject) env.asGuestValue(new TestExecutableMemberClass());
        InteropLibrary interop = InteropLibrary.getFactory().getUncached();

        // Object ms = interop.getMembers(to, false);
        Object m = interop.readMember(to, "m");
        return new RForeignObjectWrapper((TruffleObject) m);
    }

    private static RForeignObjectWrapper createInvokable() {
        TruffleLanguage.Env env = RContext.getInstance().getEnv();
        return new RForeignObjectWrapper((TruffleObject) env.asGuestValue(new TestExecutableMemberClass()));
    }

    private static RForeignObjectWrapper createArray() {
        TruffleLanguage.Env env = RContext.getInstance().getEnv();
        return new RForeignObjectWrapper((TruffleObject) env.asGuestValue(new int[]{1, 2, 3}));
    }

    private static Map<Object, Object> boxedValues = new HashMap<>();

    private static RForeignObjectWrapper createBoxedWrapper(Object value) {
        TruffleLanguage.Env env = RContext.getInstance().getEnv();
        RForeignObjectWrapper fow = new RForeignObjectWrapper((TruffleObject) env.asBoxedGuestValue(value));
        boxedValues.put(fow, value);
        return fow;
    }

    private static RForeignObjectWrapper createEnv() {
        Source src = Source.newBuilder("R", "e <- new.env(); makeActiveBinding('testBinding', function() 'testEnvString', e); e", "<testenv>").internal(true).buildLiteral();
        return new RForeignObjectWrapper((TruffleObject) FastRSession.getReceiver(context.eval(src)));
    }

    @Override
    protected String[] getKeys(TruffleObject obj) {
        if (obj == rwMember) {
            return new String[]{"a", "b", "s"};
        } else if (obj == invokableMember) {
            return new String[]{"m"};
        } else if (obj == e) {
            return new String[]{"testBinding"};
        } else {
            return new String[]{};
        }
    }

    @Override
    protected TruffleObject createEmptyTruffleObject() throws Exception {
        return null;
    }

    @Override
    protected int getSize(TruffleObject obj) {
        if (obj == array) {
            return 3;
        }
        return 0;
    }

    public static class TestRWMemberClass {
        public int a = 1;
        public double b = 2;
        public String s = "test";
    }

    public static class TestExecutableMemberClass {
        public boolean m(boolean b) {
            return b;
        }
    }
}
