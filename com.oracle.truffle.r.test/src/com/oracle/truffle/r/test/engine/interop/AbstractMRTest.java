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

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;

import org.graalvm.polyglot.Context;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.KeyInfo;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.r.runtime.data.RForeignObjectWrapper;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.test.generate.FastRSession;
import java.util.function.Function;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

public abstract class AbstractMRTest {

    // XXX check if exceptions are raised properly
    // XXX test all keyinfos whereever they are tested

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
            assertEquals(isNull(obj), ForeignAccess.sendIsNull(Message.IS_NULL.createNode(), obj));
        }
    }

    @Test
    public void testExecutable() throws Exception {
        for (TruffleObject obj : createTruffleObjects()) {
            try {
                // TODO if the need appears, also provide for args for execute
                Object expected = getExecuteExpected(obj);
                if (expected == null) {
                    ForeignAccess.sendExecute(Message.EXECUTE.createNode(), obj, getExecuteArguments(obj));
                } else {
                    Object result = ForeignAccess.sendExecute(Message.EXECUTE.createNode(), obj, getExecuteArguments(obj));
                    if (result instanceof RAbstractVector) {
                        assertSingletonVector(expected, result);
                    } else {
                        assertEquals(expected, result);
                    }
                }
                assertEquals(toString(obj) + " " + obj + " IS_EXECUTABLE", true, ForeignAccess.sendIsExecutable(Message.IS_EXECUTABLE.createNode(), obj));
            } catch (UnsupportedTypeException | ArityException e) {
                throw e;
            } catch (UnsupportedMessageException e) {
                assertEquals(toString(obj) + " " + obj + " IS_EXECUTABLE", false, ForeignAccess.sendIsExecutable(Message.IS_EXECUTABLE.createNode(), obj));
            }
        }
    }

    @Test
    public void testInstantiable() throws Exception {
        for (TruffleObject obj : createTruffleObjects()) {
            try {
                // TODO if the need appears, also provide for args for new
                ForeignAccess.sendNew(Message.NEW.createNode(), obj);
                assertEquals(toString(obj) + " IS_INSTANTIABLE", true, ForeignAccess.sendIsInstantiable(Message.IS_INSTANTIABLE.createNode(), obj));
            } catch (UnsupportedTypeException | ArityException e) {
                throw e;
            } catch (UnsupportedMessageException e) {
                assertEquals(toString(obj) + " IS_INSTANTIABLE", false, ForeignAccess.sendIsInstantiable(Message.IS_INSTANTIABLE.createNode(), obj));
            }
        }
    }

    @Test
    public void testNativePointer() throws Exception {
        for (TruffleObject obj : createTruffleObjects()) {
            if (!shouldTestToNative(obj)) {
                continue;
            }
            try {
                ForeignAccess.sendToNative(Message.TO_NATIVE.createNode(), obj);
                assertTrue(toString(obj), ForeignAccess.sendIsPointer(Message.IS_POINTER.createNode(), obj));
            } catch (UnsupportedMessageException e) {
                // XXX if not supported, this should throw UnsupportedMessageException, but isnt
                // always the case
            }
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
            Object size = ForeignAccess.sendGetSize(Message.GET_SIZE.createNode(), obj);
            assertEquals(getSize(obj), size);
            assertEquals(toString(obj) + " HAS_SIZE", true, ForeignAccess.sendHasSize(Message.HAS_SIZE.createNode(), obj));
        } catch (UnsupportedMessageException e) {
            assertEquals(toString(obj) + " HAS_SIZE", false, ForeignAccess.sendHasSize(Message.HAS_SIZE.createNode(), obj));
        }
    }

    @Test
    public void testBoxed() throws Exception {
        for (TruffleObject obj : createTruffleObjects()) {
            testBoxed(obj);
            testBoxedLegacy(obj);
        }
        TruffleObject empty = createEmptyTruffleObject();
        if (empty != null) {
            testBoxed(empty);
            testBoxedLegacy(empty);
        }
    }

    private void testBoxed(TruffleObject obj) throws UnsupportedMessageException {
        InteropLibrary interop = InteropLibrary.getFactory().getUncached();
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

    private void testBoxedLegacy(TruffleObject obj) {
        try {
            Object unboxed = ForeignAccess.sendUnbox(Message.UNBOX.createNode(), obj);
            Object expectedUnboxed = getUnboxed(obj);
            if (expectedUnboxed instanceof Number) {
                assertTrue(((Number) expectedUnboxed).doubleValue() == ((Number) unboxed).doubleValue());
            } else {
                assertEquals(expectedUnboxed, unboxed);
            }
            assertTrue(toString(obj) + " IS_BOXED returns false but UNBOX returns " + unboxed, ForeignAccess.sendIsBoxed(Message.IS_BOXED.createNode(), obj));
        } catch (UnsupportedMessageException e) {
            assertFalse(toString(obj) + " IS_BOXED returns true but UBOX unsupported", ForeignAccess.sendIsBoxed(Message.IS_BOXED.createNode(), obj));
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
        if (ForeignAccess.sendHasSize(Message.HAS_SIZE.createNode(), obj)) {
            int size = (int) ForeignAccess.sendGetSize(Message.GET_SIZE.createNode(), obj);
            for (int i = 0; i < size; i++) {
                assertFalse(KeyInfo.isReadable(ForeignAccess.sendKeyInfo(Message.KEY_INFO.createNode(), obj, i)));
            }
        }
        int index = 100000000;
        assertInteropException(() -> ForeignAccess.sendRead(Message.READ.createNode(), obj, index), readException(obj, index));
        String doesnotexist = "doesnotexist";
        assertInteropException(() -> ForeignAccess.sendRead(Message.READ.createNode(), obj, doesnotexist), readException(obj, doesnotexist));
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
        if (ForeignAccess.sendHasSize(Message.HAS_SIZE.createNode(), obj)) {
            int size = (int) ForeignAccess.sendGetSize(Message.GET_SIZE.createNode(), obj);
            for (int i = 0; i < size; i++) {
                assertFalse(KeyInfo.isWritable(ForeignAccess.sendKeyInfo(Message.KEY_INFO.createNode(), obj, i)));
            }
        }
        int index = 100000000;
        assertInteropException(() -> ForeignAccess.sendWrite(Message.WRITE.createNode(), obj, index, 1), writeException(obj, index));
        String doesnotexist = "doesnotexist";
        assertInteropException(() -> ForeignAccess.sendWrite(Message.WRITE.createNode(), obj, doesnotexist, 1), writeException(obj, doesnotexist));
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
        if (!shouldTestKeys(obj)) {
            return;
        }
        try {
            TruffleObject keysObj = ForeignAccess.sendKeys(Message.KEYS.createNode(), obj);

            int size = (int) ForeignAccess.sendGetSize(Message.GET_SIZE.createNode(), keysObj);
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
                    sbActual.append(ForeignAccess.sendRead(Message.READ.createNode(), keysObj, i));
                    if (i < keys.length - 1) {
                        sbActual.append(", ");
                    }
                }
                fail("wrong keys lenght : expected:" + sbExpected + " <" + keys.length + "> but was:" + sbActual + " <" + size + "> " + toString(obj));
            }

            Set<Object> set = new HashSet<>();
            for (int i = 0; i < size; i++) {
                set.add(ForeignAccess.sendRead(Message.READ.createNode(), keysObj, i));
            }
            for (String key : keys) {
                assertTrue(set.contains(key));
            }

            assertEquals(toString(obj) + " HAS_KEYS", true, ForeignAccess.sendHasKeys(Message.HAS_KEYS.createNode(), obj));
        } catch (UnsupportedMessageException e) {
            assertEquals(toString(obj) + " HAS_KEYS", false, ForeignAccess.sendHasKeys(Message.HAS_KEYS.createNode(), obj));
        }
    }

    protected void assertInteropException(Callable<Object> c, Class<? extends InteropException> expectedClazz) {
        try {
            c.call();
        } catch (InteropException ex) {
            if (expectedClazz != null && ex.getClass() != expectedClazz) {
                ex.printStackTrace();
                fail(this.getClass().getName() + " : " + expectedClazz + " was expected but got instead: " + ex);
            }
            return;
        } catch (Exception ex) {
            if (expectedClazz != null && ex.getClass() != expectedClazz) {
                ex.printStackTrace();
                fail(this.getClass().getName() + " : " + expectedClazz + " was expected but got instead: " + ex);
            } else {
                ex.printStackTrace();
                fail(this.getClass().getName() + " : " + "InteropException was expected but got instead: " + ex);
            }
        }
        if (expectedClazz != null) {
            fail(expectedClazz + " was expected");
        } else {
            fail(this.getClass().getName() + " : " + "InteropException was expected");
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
