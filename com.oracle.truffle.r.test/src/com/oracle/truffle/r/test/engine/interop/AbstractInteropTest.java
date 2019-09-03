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
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.Callable;

import org.graalvm.polyglot.Context;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.r.runtime.data.RForeignObjectWrapper;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.test.generate.FastRSession;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

public abstract class AbstractInteropTest {

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

    protected boolean shouldTestKeys(@SuppressWarnings("unused") TruffleObject obj) {
        return true;
    }

    /**
     *
     * @param obj
     * @return array of keys or <code>null</code> if KEYS message not supported
     */
    protected String[] getKeys(TruffleObject obj) {
        throw new UnsupportedOperationException("override if HAS_KEYS returns true");
    }

    protected Object[] getExecuteArguments(@SuppressWarnings("unused") TruffleObject obj) {
        return new Object[]{};
    }

    protected Object getExecuteExpected(@SuppressWarnings("unused") TruffleObject obj) {
        return null;
    }

    protected boolean isNull(@SuppressWarnings("unused") TruffleObject obj) {
        return false;
    }

    protected boolean shouldTestToNative(@SuppressWarnings("unused") TruffleObject obj) {
        return true;
    }

    protected boolean canRead(@SuppressWarnings("unused") TruffleObject obj) {
        return false;
    }

    protected Class<? extends InteropException> readException(@SuppressWarnings("unused") TruffleObject obj, @SuppressWarnings("unused") int index) {
        return UnsupportedMessageException.class;
    }

    protected Class<? extends InteropException> readException(@SuppressWarnings("unused") TruffleObject obj, @SuppressWarnings("unused") String member) {
        return UnsupportedMessageException.class;
    }

    protected boolean canWrite(@SuppressWarnings("unused") TruffleObject obj) {
        return false;
    }

    protected Class<? extends InteropException> writeException(@SuppressWarnings("unused") TruffleObject obj, @SuppressWarnings("unused") int index) {
        return UnsupportedMessageException.class;
    }

    protected Class<? extends InteropException> writeException(@SuppressWarnings("unused") TruffleObject obj, @SuppressWarnings("unused") String member) {
        return UnsupportedMessageException.class;
    }

    protected int getSize(@SuppressWarnings("unused") TruffleObject obj) {
        throw new UnsupportedOperationException("override if HAS_SIZE returns true");
    }

    protected Object getUnboxed(@SuppressWarnings("unused") TruffleObject obj) {
        return null;
    }

    @Test
    public void testIsNull() throws Exception {
        for (TruffleObject obj : createTruffleObjects()) {
            assertEquals("wrong isNull value for: " + toString(obj), isNull(obj), getInterop().isNull(obj));
        }
    }

    @Test
    public void testExecutable() throws Exception {
        for (TruffleObject obj : createTruffleObjects()) {
            try {
                // TODO if the need appears, also provide for args for execute
                Object expected = getExecuteExpected(obj);
                if (expected == null) {
                    getInterop().execute(obj, getExecuteArguments(obj));
                } else {
                    Object result = getInterop().execute(obj, getExecuteArguments(obj));
                    if (result instanceof RAbstractVector) {
                        assertSingletonVector(expected, result);
                    } else {
                        assertEquals(expected, result);
                    }
                }
                assertEquals(toString(obj) + " " + obj + " IS_EXECUTABLE", true, getInterop().isExecutable(obj));
            } catch (UnsupportedTypeException | ArityException e) {
                throw e;
            } catch (UnsupportedMessageException e) {
                assertEquals(toString(obj) + " " + obj + " IS_EXECUTABLE", false, getInterop().isExecutable(obj));
            }
        }
    }

    @Test
    public void testInstantiable() throws Exception {
        for (TruffleObject obj : createTruffleObjects()) {
            try {
                // TODO if the need appears, also provide for args for new
                getInterop().instantiate(obj);
                assertEquals(toString(obj) + " IS_INSTANTIABLE", true, getInterop().isInstantiable(obj));
            } catch (UnsupportedTypeException | ArityException e) {
                throw e;
            } catch (UnsupportedMessageException e) {
                assertEquals(toString(obj) + " IS_INSTANTIABLE", false, getInterop().isInstantiable(obj));
            }
        }
    }

    @Test
    public void testNativePointer() throws Exception {
        for (TruffleObject obj : createTruffleObjects()) {
            if (!shouldTestToNative(obj)) {
                continue;
            }
            getInterop().toNative(obj);
            assertTrue(toString(obj), getInterop().isPointer(obj));
        }
    }

    private static String toString(TruffleObject obj) {
        TruffleObject o = obj;
        if (o instanceof RForeignObjectWrapper) {
            o = ((RForeignObjectWrapper) o).getDelegate();
        }
        return o.getClass().getSimpleName() + " " + obj;
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
            Object size = getInterop().getArraySize(obj);
            assertEquals((long) getSize(obj), size);
            assertEquals(toString(obj) + " HAS_SIZE", true, getInterop().hasArrayElements(obj));
        } catch (UnsupportedMessageException e) {
            assertEquals(toString(obj) + " HAS_SIZE", false, getInterop().hasArrayElements(obj));
        }
    }

    @Test
    public void testBoxed() throws Exception {
        for (TruffleObject obj : createTruffleObjects()) {
            testBoxed(obj);
        }
        TruffleObject empty = createEmptyTruffleObject();
        if (empty != null) {
            testBoxed(empty);
        }
    }

    private void testBoxed(TruffleObject obj) throws UnsupportedMessageException {
        InteropLibrary interop = getInterop();
        Object expectedUnboxed = getUnboxed(obj);
        if (expectedUnboxed != null) {
            assertEquals(toString(obj), interop.isBoolean(expectedUnboxed), interop.isBoolean(obj));
            assertEquals(toString(obj), interop.isString(expectedUnboxed), interop.isString(obj));
            assertEquals(toString(obj), interop.isNumber(expectedUnboxed), interop.isNumber(obj));

            checkFitsInAs(obj, expectedUnboxed, (o) -> interop.fitsInByte(o), (o) -> interop.asByte(o));
            checkFitsInAs(obj, expectedUnboxed, (o) -> interop.fitsInShort(o), (o) -> interop.asShort(o));
            checkFitsInAs(obj, expectedUnboxed, (o) -> interop.fitsInInt(o), (o) -> interop.asInt(o));
            checkFitsInAs(obj, expectedUnboxed, (o) -> interop.fitsInLong(o), (o) -> interop.asLong(o));
            checkFitsInAs(obj, expectedUnboxed, (o) -> interop.fitsInFloat(o), (o) -> interop.asFloat(o));
            checkFitsInAs(obj, expectedUnboxed, (o) -> interop.fitsInDouble(o), (o) -> interop.asDouble(o));
        } else {
            assertFalse(toString(obj), interop.isBoolean(obj));
            assertFalse(toString(obj), interop.isString(obj));
            assertFalse(toString(obj), interop.isNumber(obj));

            checkFitsInAs(obj, (o) -> interop.fitsInByte(o), (o) -> interop.asByte(o));
            checkFitsInAs(obj, (o) -> interop.fitsInShort(o), (o) -> interop.asShort(o));
            checkFitsInAs(obj, (o) -> interop.fitsInInt(o), (o) -> interop.asInt(o));
            checkFitsInAs(obj, (o) -> interop.fitsInLong(o), (o) -> interop.asLong(o));
            checkFitsInAs(obj, (o) -> interop.fitsInFloat(o), (o) -> interop.asFloat(o));
            checkFitsInAs(obj, (o) -> interop.fitsInDouble(o), (o) -> interop.asDouble(o));
        }
    }

    private interface As<T, R> {
        R apply(T t) throws UnsupportedMessageException;
    }

    private void checkFitsInAs(Object value, Object unboxedValue, Function<Object, Boolean> fitsIn, As<Object, Object> as) throws UnsupportedMessageException {
        if (fitsIn.apply(unboxedValue)) {
            assertTrue(fitsIn.apply(value));
            assertEquals(as.apply(unboxedValue), as.apply(value));
        } else {
            assertFalse(fitsIn.apply(value));
            assertInteropException(() -> as.apply(value), UnsupportedMessageException.class);
        }
    }

    private void checkFitsInAs(Object value, Function<Object, Boolean> fitsIn, As<Object, Object> as) {
        assertFalse(fitsIn.apply(value));
        assertInteropException(() -> as.apply(value), UnsupportedMessageException.class);
    }

    @Test
    public void testCannotRead() throws Exception {
        for (TruffleObject obj : createTruffleObjects()) {
            if (!canRead(obj)) {
                testCannotRead(obj);
            }
        }
    }

    private void testCannotRead(TruffleObject obj) throws Exception {
        if (getInterop().hasArrayElements(obj)) {
            int size = (int) getInterop().getArraySize(obj);
            for (int i = 0; i < size; i++) {
                assertFalse(getInterop().isArrayElementReadable(obj, i));
            }
        }
        int index = 100000000;
        assertInteropException(() -> getInterop().readArrayElement(obj, index), readException(obj, index));
        String doesnotexist = "doesnotexist";
        assertFalse(getInterop().isMemberReadable(obj, doesnotexist));
        assertInteropException(() -> getInterop().readMember(obj, doesnotexist), readException(obj, doesnotexist));
    }

    @Test
    public void testCannotWrite() throws Exception {
        for (TruffleObject obj : createTruffleObjects()) {
            if (!canWrite(obj)) {
                testCannotWrite(obj);
            }
        }
    }

    private void testCannotWrite(TruffleObject obj) throws Exception {
        if (getInterop().hasArrayElements(obj)) {
            int size = (int) getInterop().getArraySize(obj);
            for (int i = 0; i < size; i++) {
                assertFalse(getInterop().isArrayElementWritable(obj, i));
            }
        }
        int index = 100000000;
        assertInteropException(() -> {
            getInterop().writeArrayElement(obj, index, 1);
            return null;
        }, writeException(obj, index));
        String doesnotexist = "doesnotexist";
        assertFalse(getInterop().isMemberWritable(obj, doesnotexist));
        assertInteropException(() -> {
            getInterop().writeMember(obj, doesnotexist, 1);
            return null;
        }, writeException(obj, doesnotexist));
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

    private void testKeys(TruffleObject obj) throws InvalidArrayIndexException {
        if (!shouldTestKeys(obj)) {
            return;
        }
        try {
            Object keysObj = getInterop().getMembers(obj);

            int size = (int) getInterop().getArraySize(keysObj);
            String[] keys = getKeys(obj);
            if (keys.length != size) {
                StringBuilder sbExpected = new StringBuilder();
                for (int i = 0; i < keys.length; i++) {
                    sbExpected.append(keys[i]);
                    if (i < keys.length - 1) {
                        sbExpected.append(", ");
                    }
                }
                StringBuilder sbActual = new StringBuilder();
                for (int i = 0; i < size; i++) {
                    sbActual.append(getInterop().readArrayElement(keysObj, i));
                    if (i < keys.length - 1) {
                        sbActual.append(", ");
                    }
                }
                fail("wrong keys lenght : expected:" + sbExpected + " <" + keys.length + "> but was:" + sbActual + " <" + size + "> " + toString(obj));
            }

            Set<Object> set = new HashSet<>();
            for (int i = 0; i < size; i++) {
                set.add(getInterop().readArrayElement(keysObj, i));
            }
            for (String key : keys) {
                assertTrue(set.contains(key));
            }

            assertEquals(toString(obj) + " HAS_KEYS", true, getInterop().hasMembers(obj));
        } catch (UnsupportedMessageException e) {
            assertEquals(toString(obj) + " HAS_KEYS", false, getInterop().hasMembers(obj));
        }
    }

    protected void assertInteropException(Callable<Object> c, Class<? extends Exception> expectedClazz) {
        assert expectedClazz != null;
        try {
            c.call();
        } catch (InteropException ex) {
            if (expectedClazz != null && ex.getClass() != expectedClazz) {
                ex.printStackTrace();
                fail(this.getClass().getName() + " : " + expectedClazz + " was expected but got instead: " + ex);
            }
            return;
        } catch (Exception ex) {
            if (ex.getClass() != expectedClazz) {
                ex.printStackTrace();
                fail(this.getClass().getName() + " : " + expectedClazz + " was expected but got instead: " + ex);
            }
            return;
        }
        fail(expectedClazz + " was expected");
    }

    protected void assertSingletonVector(Object expected, Object vector) throws UnsupportedMessageException, InvalidArrayIndexException {
        assertThat(vector, instanceOf(RAbstractVector.class));
        RAbstractVector vec = (RAbstractVector) vector;
        assertTrue(vec.getLength() == 1);
        Object actual = getInterop().readArrayElement(vec, 0);
        assertEquals("Value returned from vector " + vec, expected, actual);
    }

    protected static InteropLibrary getInterop() {
        return InteropLibrary.getFactory().getUncached();
    }
}
