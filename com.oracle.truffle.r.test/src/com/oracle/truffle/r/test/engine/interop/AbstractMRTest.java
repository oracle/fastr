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

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;

import org.graalvm.polyglot.Context;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.test.generate.FastRSession;

public abstract class AbstractMRTest {

    protected static Context context;

    @BeforeClass
    public static void before() {
        context = FastRSession.getContextBuilder("R", "llvm").build();
        context.eval("R", "1"); // initialize context
        context.enter();
    }

    @AfterClass
    public static void after() {
        context.leave();
        context.close();
    }

    @Before
    public void beforeMethod() {
        /**
         * Currently, the tests derived from this abstract test class fail when running in the LLVM
         * mode due to interferences between the engine created in TestBase.RunListener and the one
         * created in this class. To run the tests in the LLVM mode there must be only one engine in
         * the VM.
         */
        org.junit.Assume.assumeTrue(!"llvm".equals(System.getenv().get("FASTR_RFFI")));
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
        for (TruffleObject obj : createTruffleObjects()) {
            assertEquals(isNull(obj), ForeignAccess.sendIsNull(Message.IS_NULL.createNode(), obj));
        }
    }

    @Test
    public void testExecutable() throws Exception {
        for (TruffleObject obj : createTruffleObjects()) {
            try {
                // TODO if the need appears, also provide for args for execute
                ForeignAccess.sendExecute(Message.EXECUTE.createNode(), obj);
                assertEquals(obj.getClass().getSimpleName() + " " + obj + " IS_EXECUTABLE", true, ForeignAccess.sendIsExecutable(Message.IS_EXECUTABLE.createNode(), obj));
            } catch (UnsupportedTypeException | ArityException e) {
                throw e;
            } catch (UnsupportedMessageException e) {
                assertEquals(obj.getClass().getSimpleName() + " " + obj + " IS_EXECUTABLE", false, ForeignAccess.sendIsExecutable(Message.IS_EXECUTABLE.createNode(), obj));
            }
        }
    }

    @Test
    public void testInstantiable() throws Exception {
        for (TruffleObject obj : createTruffleObjects()) {
            try {
                // TODO if the need appears, also provide for args for new
                ForeignAccess.sendNew(Message.NEW.createNode(), obj);
                assertEquals(obj.getClass().getSimpleName() + " " + obj + " IS_INSTANTIABLE", true, ForeignAccess.sendIsInstantiable(Message.IS_INSTANTIABLE.createNode(), obj));
            } catch (UnsupportedTypeException | ArityException e) {
                throw e;
            } catch (UnsupportedMessageException e) {
                assertEquals(obj.getClass().getSimpleName() + " " + obj + " IS_INSTANTIABLE", false, ForeignAccess.sendIsInstantiable(Message.IS_INSTANTIABLE.createNode(), obj));
            }
        }
    }

    @Test
    public void testAsNativePointer() throws Exception {
        for (TruffleObject obj : createTruffleObjects()) {
            try {
                assertNotNull(obj.getClass().getSimpleName(), ForeignAccess.sendToNative(Message.TO_NATIVE.createNode(), obj));
                assertEquals(obj.getClass().getSimpleName() + " " + obj + " IS_POINTER", true, ForeignAccess.sendIsPointer(Message.IS_POINTER.createNode(), obj));
            } catch (UnsupportedMessageException e) {
                assertEquals(obj.getClass().getSimpleName() + " " + obj + " IS_POINTER", false, ForeignAccess.sendIsPointer(Message.IS_POINTER.createNode(), obj));
            }
        }
    }

    @Test
    public void testNativePointer() throws Exception {
        for (TruffleObject obj : createTruffleObjects()) {
            if (!testToNative(obj)) {
                continue;
            }
            try {
                assertTrue(obj.getClass().getSimpleName(), ForeignAccess.sendToNative(Message.TO_NATIVE.createNode(), obj) == obj);
            } catch (UnsupportedMessageException e) {
            }
        }
    }

    @Test
    public void testSize() throws Exception {
        for (TruffleObject obj : createTruffleObjects()) {
            testSize(obj);
        }
        TruffleObject empty = createEmptyTruffleObject();
        if (empty != null) {
            testSize(empty);
        }
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
        for (TruffleObject obj : createTruffleObjects()) {
            testUnboxed(obj);
        }
        TruffleObject empty = createEmptyTruffleObject();
        if (empty != null) {
            testUnboxed(empty);
        }
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
        for (TruffleObject obj : createTruffleObjects()) {
            testKeys(obj);
        }
        TruffleObject empty = createEmptyTruffleObject();
        if (empty != null) {
            testKeys(empty);
        }
    }

    private void testKeys(TruffleObject obj) throws UnknownIdentifierException {
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
        try {
            c.call();
        } catch (InteropException ex) {
            if (expectedClazz != null && ex.getClass() != expectedClazz) {
                ex.printStackTrace();
                Assert.fail(expectedClazz + " was expected but got instead: " + ex);
            }
            return;
        } catch (Exception ex) {
            if (expectedClazz != null && ex.getClass() != expectedClazz) {
                ex.printStackTrace();
                Assert.fail(expectedClazz + " was expected but got instead: " + ex);
            } else {
                ex.printStackTrace();
                Assert.fail("InteropException was expected but got instead: " + ex);
            }
        }
        if (expectedClazz != null) {
            Assert.fail(expectedClazz + " was expected");
        } else {
            Assert.fail("InteropException was expected");
        }
    }

    protected void assertSingletonVector(Object expected, Object vector) throws UnsupportedMessageException, UnknownIdentifierException {
        assertThat(vector, instanceOf(RAbstractVector.class));
        RAbstractVector vec = (RAbstractVector) vector;
        assertTrue(vec.getLength() == 1);
        Object actual = ForeignAccess.sendRead(Message.READ.createNode(), vec, 0);
        assertEquals("Value returned from vector " + vec, expected, actual);
    }
}
