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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.vm.PolyglotEngine;
import com.oracle.truffle.r.ffi.impl.interop.NativePointer;
import com.oracle.truffle.r.runtime.data.RNull;

public abstract class AbstractMRTest {

    protected static PolyglotEngine engine;

    @BeforeClass
    public static void before() {
        engine = PolyglotEngine.newBuilder().build();
    }

    @AfterClass
    public static void after() {
        engine.dispose();
    }

    /**
     * Create TruffleObject-s to be rudimentary tested for IS_NULL, IS_BOXED/UNBOX, IS_EXECUTABLE,
     * IS_POINTER, HAS_SIZE/GET_SIZE/KEYS behavior.
     *
     * @throws Exception
     */
    protected abstract TruffleObject[] createTruffleObjects() throws Exception;

    /**
     * Create a test TruffleObject having size == 0.
     */
    protected abstract TruffleObject createEmptyTruffleObject() throws Exception;

    protected String[] getKeys() {
        return null;
    }

    protected boolean isNull(@SuppressWarnings("unused") TruffleObject obj) {
        return false;
    }

    protected boolean isExecutable(@SuppressWarnings("unused") TruffleObject obj) {
        return false;
    }

    protected boolean isPointer(@SuppressWarnings("unused") TruffleObject obj) {
        return true;
    }

    protected boolean isBoxed(@SuppressWarnings("unused") TruffleObject obj) {
        return false;
    }

    protected boolean hasSize(@SuppressWarnings("unused") TruffleObject obj) {
        return false;
    }

    protected boolean testToNative(@SuppressWarnings("unused") TruffleObject obj) {
        return true;
    }

    protected int getSize(@SuppressWarnings("unused") TruffleObject obj) {
        throw new UnsupportedOperationException("override if hasSize returns true");
    }

    protected Object getUnboxed(@SuppressWarnings("unused") TruffleObject obj) {
        throw new UnsupportedOperationException("override if isBoxed returns true");
    }

    @Test
    public void testIsNull() throws Exception {
        for (TruffleObject obj : createTruffleObjects()) {
            assertEquals(isNull(obj), ForeignAccess.sendIsNull(Message.IS_NULL.createNode(), obj));
        }
    }

    @Test
    public void testIsExecutable() throws Exception {
        for (TruffleObject obj : createTruffleObjects()) {
            assertEquals(isExecutable(obj), ForeignAccess.sendIsExecutable(Message.IS_EXECUTABLE.createNode(), obj));
        }
    }

    @Test
    public void testIsPointer() throws Exception {
        for (TruffleObject obj : createTruffleObjects()) {
            assertEquals(obj.getClass().getSimpleName(), isPointer(obj), ForeignAccess.sendIsPointer(Message.IS_POINTER.createNode(), obj));
        }
    }

    @Test
    public void testNativePointer() throws Exception {
        for (TruffleObject obj : createTruffleObjects()) {
            if (!testToNative(obj)) {
                continue;
            }
            try {
                if (obj == RNull.instance) {
                    assertTrue(obj.getClass().getSimpleName(), ForeignAccess.sendToNative(Message.TO_NATIVE.createNode(), obj) == NativePointer.NULL_NATIVEPOINTER);
                } else {
                    assertTrue(obj.getClass().getSimpleName(), ForeignAccess.sendToNative(Message.TO_NATIVE.createNode(), obj) == obj);
                }
            } catch (UnsupportedMessageException unsupportedMessageException) {
            }
        }
    }

    @Test
    public void testSize() throws Exception {
        for (TruffleObject obj : createTruffleObjects()) {
            boolean hasSize = ForeignAccess.sendHasSize(Message.HAS_SIZE.createNode(), obj);
            assertEquals("" + obj.getClass().getSimpleName() + " " + obj + " hasSize", hasSize(obj), hasSize);
            if (hasSize) {
                assertEquals(getSize(obj), ForeignAccess.sendGetSize(Message.GET_SIZE.createNode(), obj));
            } else {
                assertInteropException(() -> ForeignAccess.sendGetSize(Message.GET_SIZE.createNode(), obj), UnsupportedMessageException.class);
            }
        }
    }

    @Test
    public void testBoxed() throws Exception {
        for (TruffleObject obj : createTruffleObjects()) {
            boolean isBoxed = ForeignAccess.sendIsBoxed(Message.IS_BOXED.createNode(), obj);
            assertEquals("" + obj.getClass().getSimpleName() + " " + obj + " isBoxed", isBoxed(obj), isBoxed);
            if (isBoxed) {
                assertEquals(getUnboxed(obj), ForeignAccess.sendUnbox(Message.UNBOX.createNode(), obj));
            } else {
                assertInteropException(() -> ForeignAccess.sendUnbox(Message.UNBOX.createNode(), obj), UnsupportedMessageException.class);
            }
        }
    }

    @Test
    public void testKeys() throws Exception {
        String[] keys = getKeys();
        if (keys == null) {
            return;
        }
        for (TruffleObject obj : createTruffleObjects()) {
            TruffleObject keysObj = ForeignAccess.sendKeys(Message.KEYS.createNode(), obj);
            int size = (int) ForeignAccess.sendGetSize(Message.GET_SIZE.createNode(), keysObj);
            assertEquals(keys.length, size);

            Set<Object> set = new HashSet<>();
            for (int i = 0; i < size; i++) {
                set.add(ForeignAccess.sendRead(Message.READ.createNode(), keysObj, i));
            }
            for (String key : keys) {
                assertTrue(set.contains(key));
            }
        }
    }

    @Test
    public void testEmpty() throws Exception {

        TruffleObject obj = createEmptyTruffleObject();
        if (obj != null) {
            if (hasSize(obj)) {
                int size = (int) ForeignAccess.sendGetSize(Message.GET_SIZE.createNode(), obj);
                assertEquals(0, size);
            }

            TruffleObject keys = null;
            try {
                keys = ForeignAccess.sendKeys(Message.KEYS.createNode(), obj);
            } catch (UnsupportedMessageException ex) {
            }
            if (keys != null) {
                boolean keysHasSize = ForeignAccess.sendHasSize(Message.HAS_SIZE.createNode(), keys);
                if (keysHasSize) {
                    int keysSize = (int) ForeignAccess.sendGetSize(Message.GET_SIZE.createNode(), keys);
                    assertEquals(0, keysSize);
                }
            }
        }
    }

    protected interface ForeignCall {
        void call() throws Exception;
    }

    protected void assertInteropException(ForeignCall c, Class<? extends InteropException> expectedClazz) {
        boolean ie = false;
        try {
            c.call();
        } catch (InteropException ex) {
            if (expectedClazz != null && ex.getClass() != expectedClazz) {
                Assert.fail(expectedClazz + " was expected but got instead: " + ex);
            }
            ie = true;
        } catch (Exception ex) {
            if (expectedClazz != null && ex.getClass() != expectedClazz) {
                Assert.fail(expectedClazz + " was expected but got instead: " + ex);
            } else {
                Assert.fail("InteropException was expected but got insteat: " + ex);
            }
        }
        if (!ie) {
            if (expectedClazz != null) {
                Assert.fail(expectedClazz + " was expected");
            } else {
                Assert.fail("InteropException was expected");
            }
        }
    }

}
