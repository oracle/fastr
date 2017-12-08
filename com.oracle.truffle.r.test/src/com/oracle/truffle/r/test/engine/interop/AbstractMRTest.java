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

import com.oracle.truffle.api.interop.ArityException;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;

import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.r.ffi.impl.interop.NativePointer;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.test.generate.FastRSession;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.graalvm.polyglot.Context;
import org.junit.Test;
import static com.oracle.truffle.r.test.generate.FastRSession.execInContext;
import java.util.concurrent.Callable;

public abstract class AbstractMRTest {

    protected static Context context;

    @BeforeClass
    public static void before() {
        context = Context.newBuilder("R").build();
    }

    @AfterClass
    public static void after() {
        context.close();
    }

    protected void execInContext(Callable<Object> c) {
        FastRSession.execInContext(context, c);
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

    /**
     * 
     * @param obj
     * @return array of keys or <code>null</code> if KEYS message not supported
     */
    protected String[] getKeys(TruffleObject obj) {
        throw new UnsupportedOperationException("override if HAS_KEYS returns true");
    }

    protected boolean isNull(@SuppressWarnings("unused") TruffleObject obj) {
        return false;
    }

    protected boolean testToNative(@SuppressWarnings("unused") TruffleObject obj) {
        return true;
    }

    protected int getSize(@SuppressWarnings("unused") TruffleObject obj) {
        throw new UnsupportedOperationException("override if HAS_SIZE returns true");
    }

    protected Object getUnboxed(@SuppressWarnings("unused") TruffleObject obj) {
        throw new UnsupportedOperationException("override if IS_BOXED returns true");
    }

    @Test
    public void testIsNull() throws Exception {
        execInContext(() -> {
            for (TruffleObject obj : createTruffleObjects()) {
                assertEquals(isNull(obj), ForeignAccess.sendIsNull(Message.IS_NULL.createNode(), obj));
            }
            return null;
        });
    }

    @Test
    public void testExecutable() throws Exception {
        execInContext(() -> {
            for (TruffleObject obj : createTruffleObjects()) {
                try {
                    // TODO if the need appears, also provide for args for execute
                    ForeignAccess.sendExecute(Message.createExecute(0).createNode(), obj);
                    assertEquals(obj.getClass().getSimpleName() + " " + obj + " IS_EXECUTABLE", true, ForeignAccess.sendIsExecutable(Message.IS_EXECUTABLE.createNode(), obj));
                } catch (UnsupportedTypeException | ArityException e) {
                    throw e;
                } catch (UnsupportedMessageException e) {
                    assertEquals(obj.getClass().getSimpleName() + " " + obj + " IS_EXECUTABLE", false, ForeignAccess.sendIsExecutable(Message.IS_EXECUTABLE.createNode(), obj));
                }
            }
            return null;
        });
    }

    @Test
    public void testInstantiable() throws Exception {
        execInContext(() -> {
            for (TruffleObject obj : createTruffleObjects()) {
                try {
                    // TODO if the need appears, also provide for args for new
                    ForeignAccess.sendNew(Message.createNew(0).createNode(), obj);
                    assertEquals(obj.getClass().getSimpleName() + " " + obj + " IS_INSTANTIABLE", true, ForeignAccess.sendIsInstantiable(Message.IS_INSTANTIABLE.createNode(), obj));
                } catch (UnsupportedTypeException | ArityException e) {
                    throw e;
                } catch (UnsupportedMessageException e) {
                    assertEquals(obj.getClass().getSimpleName() + " " + obj + " IS_INSTANTIABLE", false, ForeignAccess.sendIsInstantiable(Message.IS_INSTANTIABLE.createNode(), obj));
                }
            }
            return null;
        });
    }

    @Test
    public void testAsNativePointer() throws Exception {
        execInContext(() -> {
            for (TruffleObject obj : createTruffleObjects()) {
                try {
                    assertNotNull(obj.getClass().getSimpleName(), ForeignAccess.sendToNative(Message.AS_POINTER.createNode(), obj));
                    assertEquals(obj.getClass().getSimpleName() + " " + obj + " IS_POINTER", true, ForeignAccess.sendIsPointer(Message.IS_POINTER.createNode(), obj));
                } catch (UnsupportedMessageException e) {
                    assertEquals(obj.getClass().getSimpleName() + " " + obj + " IS_POINTER", false, ForeignAccess.sendIsPointer(Message.IS_POINTER.createNode(), obj));
                }
            }
            return null;
        });
    }

    @Test
    public void testNativePointer() throws Exception {
        execInContext(() -> {
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
                } catch (UnsupportedMessageException e) {
                }
            }
            return null;
        });
    }

    @Test
    public void testSize() throws Exception {
        execInContext(() -> {
            for (TruffleObject obj : createTruffleObjects()) {
                testSize(obj);
            }
            TruffleObject empty = createEmptyTruffleObject();
            if (empty != null) {
                testSize(empty);
            }
            return null;
        });
    }

    private void testSize(TruffleObject obj) {
        try {
            Object size = ForeignAccess.sendGetSize(Message.GET_SIZE.createNode(), obj);
            assertEquals(getSize(obj), size);
            assertEquals(obj.getClass().getSimpleName() + " " + obj + " HAS_SIZE", true, ForeignAccess.sendHasSize(Message.HAS_SIZE.createNode(), obj));
        } catch (UnsupportedMessageException e) {
            assertEquals(obj.getClass().getSimpleName() + " " + obj + " HAS_SIZE", false, ForeignAccess.sendHasSize(Message.HAS_SIZE.createNode(), obj));
        }
    }

    @Test
    public void testBoxed() throws Exception {
        execInContext(() -> {
            for (TruffleObject obj : createTruffleObjects()) {
                testUnboxed(obj);
            }
            TruffleObject empty = createEmptyTruffleObject();
            if (empty != null) {
                testUnboxed(empty);
            }
            return null;
        });
    }

    private void testUnboxed(TruffleObject obj) {
        try {
            Object unboxed = ForeignAccess.sendUnbox(Message.UNBOX.createNode(), obj);
            assertEquals(getUnboxed(obj), unboxed);
            assertEquals(obj.getClass().getSimpleName() + " " + obj + " IS_BOXED", true, ForeignAccess.sendIsBoxed(Message.IS_BOXED.createNode(), obj));
        } catch (UnsupportedMessageException e) {
            assertEquals(obj.getClass().getSimpleName() + " " + obj + " IS_BOXED", false, ForeignAccess.sendIsBoxed(Message.IS_BOXED.createNode(), obj));
        }
    }

    @Test
    public void testKeys() throws Exception {
        execInContext(() -> {
            for (TruffleObject obj : createTruffleObjects()) {
                testKeys(obj);
            }
            TruffleObject empty = createEmptyTruffleObject();
            if (empty != null) {
                testKeys(empty);
            }
            return null;
        });
    }

    private void testKeys(TruffleObject obj) throws UnknownIdentifierException, UnsupportedMessageException {
        try {
            TruffleObject keysObj = ForeignAccess.sendKeys(Message.KEYS.createNode(), obj);

            int size = (int) ForeignAccess.sendGetSize(Message.GET_SIZE.createNode(), keysObj);
            String[] keys = getKeys(obj);
            assertEquals(keys.length, size);

            Set<Object> set = new HashSet<>();
            for (int i = 0; i < size; i++) {
                set.add(ForeignAccess.sendRead(Message.READ.createNode(), keysObj, i));
            }
            for (String key : keys) {
                assertTrue(set.contains(key));
            }

            assertEquals(obj.getClass().getSimpleName() + " " + obj + " HAS_KEYS", true, ForeignAccess.sendHasKeys(Message.HAS_KEYS.createNode(), obj));
        } catch (UnsupportedMessageException e) {
            assertEquals(obj.getClass().getSimpleName() + " " + obj + " HAS_KEYS", false, ForeignAccess.sendHasKeys(Message.HAS_KEYS.createNode(), obj));
        }
    }

    protected void assertInteropException(Callable<Object> c, Class<? extends InteropException> expectedClazz) {
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
