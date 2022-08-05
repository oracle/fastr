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
import com.oracle.truffle.api.interop.InteropLibrary;
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
import org.junit.Test;

public class RForeignObjectWrapperInteropTest extends AbstractInteropTest {

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
    private RForeignObjectWrapper e;

    @Test
    public void dummyTest() {
        // nop, just to force unit testing of this class
    }

    @Override
    protected boolean shouldTestToNative(TruffleObject obj) {
        return obj == nulll;
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
