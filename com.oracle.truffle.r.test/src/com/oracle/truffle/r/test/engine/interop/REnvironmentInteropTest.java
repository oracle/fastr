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

import static org.junit.Assert.assertFalse;

import com.oracle.truffle.r.runtime.data.model.RIntVector;
import org.junit.Test;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.test.generate.FastRSession;
import org.graalvm.polyglot.Source;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class REnvironmentInteropTest extends AbstractInteropTest {

    @Override
    protected boolean canRead(@SuppressWarnings("unused") TruffleObject obj) {
        return true;
    }

    @Override
    protected boolean canWrite(@SuppressWarnings("unused") TruffleObject obj) {
        return true;
    }

    @Test
    public void testReadWrite() throws Exception {
        REnvironment e = (REnvironment) createTruffleObjects()[0];

        assertSingletonVector("aaa", getInterop().readMember(e, "s"));
        assertSingletonVector(123, getInterop().readMember(e, "i"));
        assertSingletonVector(123.1, getInterop().readMember(e, "d"));
        assertSingletonVector(true, getInterop().readMember(e, "b"));

        assertInteropException(() -> getInterop().readMember(e, "nnnoooonnne"), UnknownIdentifierException.class);

        getInterop().writeMember(e, "s", "abc");
        Object value = getInterop().readMember(e, "s");
        assertSingletonVector("abc", value);

        getInterop().writeMember(e, "b", false);
        value = getInterop().readMember(e, "b");
        assertSingletonVector(false, value);

        getInterop().writeMember(e, "i", (short) 1234);
        value = getInterop().readMember(e, "i");
        assertSingletonVector(1234, value);

        getInterop().writeMember(e, "newnew", "nneeww");
        value = getInterop().readMember(e, "newnew");
        assertSingletonVector("nneeww", value);

        assertInteropException(() -> {
            getInterop().writeMember(e, "l", 667);
            return null;
        }, UnsupportedMessageException.class);

        e.lock(false);
        // writing the value for an existing binding is ok
        getInterop().writeMember(e, "i", 1235);
        value = getInterop().readMember(e, "i");
        assertSingletonVector(1235, value);
        // but addind a new is not possible
        assertInteropException(() -> {
            getInterop().writeMember(e, "newnew2", "nneeww");
            return null;
        }, UnsupportedMessageException.class);

        // can't read/write via index
        assertInteropException(() -> getInterop().readArrayElement(e, 0), UnsupportedMessageException.class);
        assertInteropException(() -> getInterop().readArrayElement(e, 1), UnsupportedMessageException.class);
        assertInteropException(() -> {
            getInterop().writeArrayElement(e, 1, 321);
            return null;
        }, UnsupportedMessageException.class);
    }

    @Test
    public void testKeysInfo() throws Exception {
        REnvironment e = (REnvironment) createTruffleObjects()[0];
        assertFalse(getInterop().isMemberExisting(e, "nnoonnee"));
        assertTrue(getInterop().isMemberInsertable(e, "nnoonnee"));
        assertFalse(getInterop().isMemberInternal(e, "nnoonnee"));
        assertFalse(getInterop().isMemberInvocable(e, "nnoonnee"));
        assertFalse(getInterop().isMemberModifiable(e, "nnoonnee"));
        assertFalse(getInterop().isMemberReadable(e, "nnoonnee"));
        assertFalse(getInterop().isMemberRemovable(e, "nnoonnee"));
        assertTrue(getInterop().isMemberWritable(e, "nnoonnee"));
        assertFalse(getInterop().hasMemberReadSideEffects(e, "nnoonnee"));
        assertFalse(getInterop().hasMemberWriteSideEffects(e, "nnoonnee"));

        assertFalse(getInterop().isArrayElementExisting(e, 0));
        assertFalse(getInterop().isArrayElementInsertable(e, 0));
        assertFalse(getInterop().isArrayElementModifiable(e, 0));
        assertFalse(getInterop().isArrayElementReadable(e, 0));
        assertFalse(getInterop().isArrayElementRemovable(e, 0));
        assertFalse(getInterop().isArrayElementWritable(e, 0));

        assertFalse(getInterop().isArrayElementExisting(e, 1));
        assertFalse(getInterop().isArrayElementInsertable(e, 1));
        assertFalse(getInterop().isArrayElementModifiable(e, 1));
        assertFalse(getInterop().isArrayElementReadable(e, 1));
        assertFalse(getInterop().isArrayElementRemovable(e, 1));
        assertFalse(getInterop().isArrayElementWritable(e, 1));

        assertTrue(getInterop().isMemberExisting(e, "s"));
        assertFalse(getInterop().isMemberInsertable(e, "s"));
        assertFalse(getInterop().isMemberInternal(e, "s"));
        assertFalse(getInterop().isMemberInvocable(e, "s"));
        assertTrue(getInterop().isMemberModifiable(e, "s"));
        assertTrue(getInterop().isMemberReadable(e, "s"));
        assertTrue(getInterop().isMemberRemovable(e, "s"));
        assertTrue(getInterop().isMemberWritable(e, "s"));
        assertFalse(getInterop().hasMemberReadSideEffects(e, "s"));
        assertFalse(getInterop().hasMemberWriteSideEffects(e, "s"));

        assertTrue(getInterop().isMemberExisting(e, "b"));
        assertFalse(getInterop().isMemberInsertable(e, "b"));
        assertFalse(getInterop().isMemberInternal(e, "b"));
        assertFalse(getInterop().isMemberInvocable(e, "b"));
        assertTrue(getInterop().isMemberModifiable(e, "b"));
        assertTrue(getInterop().isMemberReadable(e, "b"));
        assertTrue(getInterop().isMemberRemovable(e, "b"));
        assertTrue(getInterop().isMemberWritable(e, "b"));
        assertFalse(getInterop().hasMemberReadSideEffects(e, "b"));
        assertFalse(getInterop().hasMemberWriteSideEffects(e, "b"));

        assertTrue(getInterop().isMemberExisting(e, "fn"));
        assertFalse(getInterop().isMemberInsertable(e, "fn"));
        assertFalse(getInterop().isMemberInternal(e, "fn"));
        assertTrue(getInterop().isMemberInvocable(e, "fn"));
        assertTrue(getInterop().isMemberModifiable(e, "fn"));
        assertTrue(getInterop().isMemberReadable(e, "fn"));
        assertTrue(getInterop().isMemberRemovable(e, "fn"));
        assertTrue(getInterop().isMemberWritable(e, "fn"));
        assertFalse(getInterop().hasMemberReadSideEffects(e, "fn"));
        assertFalse(getInterop().hasMemberWriteSideEffects(e, "fn"));

        assertTrue(getInterop().isMemberExisting(e, "n"));
        assertFalse(getInterop().isMemberInsertable(e, "n"));
        assertFalse(getInterop().isMemberInternal(e, "n"));
        assertFalse(getInterop().isMemberInvocable(e, "n"));
        assertTrue(getInterop().isMemberModifiable(e, "n"));
        assertTrue(getInterop().isMemberReadable(e, "n"));
        assertTrue(getInterop().isMemberRemovable(e, "n"));
        assertTrue(getInterop().isMemberWritable(e, "n"));
        assertFalse(getInterop().hasMemberReadSideEffects(e, "n"));
        assertFalse(getInterop().hasMemberWriteSideEffects(e, "n"));

        e.lock(false);
        assertFalse(getInterop().isMemberExisting(e, "nneeww"));
        assertFalse(getInterop().isMemberInsertable(e, "nneeww"));
        assertFalse(getInterop().isMemberModifiable(e, "nneeww"));
        assertFalse(getInterop().isMemberReadable(e, "nneeww"));
        assertFalse(getInterop().isMemberRemovable(e, "nneeww"));
        assertFalse(getInterop().isMemberWritable(e, "nneeww"));
        assertFalse(getInterop().hasMemberReadSideEffects(e, "nneeww"));
        assertFalse(getInterop().hasMemberWriteSideEffects(e, "nneeww"));

        assertTrue(getInterop().isMemberModifiable(e, "i"));
        assertTrue(getInterop().isMemberReadable(e, "i"));
        assertFalse(getInterop().isMemberRemovable(e, "i"));
        assertTrue(getInterop().isMemberWritable(e, "i"));
        assertFalse(getInterop().hasMemberReadSideEffects(e, "i"));
        assertFalse(getInterop().hasMemberWriteSideEffects(e, "i"));
    }

    @Test
    public void testRemove() throws Exception {
        assertInteropException(() -> {
            getInterop().removeMember(createEmptyTruffleObject(), "nnoonnee");
            return null;
        }, UnknownIdentifierException.class);

        REnvironment e = (REnvironment) createTruffleObjects()[0];
        assertInteropException(() -> {
            getInterop().removeMember(e, "nnoonnee");
            return null;
        }, UnknownIdentifierException.class);

        assertSingletonVector("aaa", getInterop().readMember(e, "s"));
        assertTrue(getInterop().isMemberRemovable(e, "s"));
        getInterop().removeMember(e, "s");
        assertInteropException(() -> getInterop().readMember(e, "s"), UnknownIdentifierException.class);

        assertTrue(getInterop().isMemberRemovable(e, "i"));
        e.lock(true);
        assertFalse(getInterop().isMemberRemovable(e, "i"));
        assertInteropException(() -> {
            getInterop().removeMember(e, "i");
            return null;
        }, UnsupportedMessageException.class);
    }

    @Test
    public void testInvokeMember() throws Exception {
        TruffleObject o = createTruffleObjects()[0];

        assertTrue(getInterop().isMemberExisting(o, "fn"));
        assertFalse(getInterop().isMemberInsertable(o, "fn"));
        assertFalse(getInterop().isMemberInternal(o, "fn"));
        assertTrue(getInterop().isMemberInvocable(o, "fn"));
        assertTrue(getInterop().isMemberModifiable(o, "fn"));
        assertTrue(getInterop().isMemberReadable(o, "fn"));
        assertTrue(getInterop().isMemberRemovable(o, "fn"));
        assertTrue(getInterop().isMemberWritable(o, "fn"));

        assertSingletonVector(true, InteropLibrary.getFactory().getUncached().invokeMember(o, "fn", true));
    }

    @Test
    public void testReadingNAAndWritingNABackKeepsNA() throws Exception {
        final TruffleObject e = createEnv("e <- new.env(); e$i <- 42; e$na <- NA_integer_; e")[0];
        Object naInteropValue = getInterop().readMember(e, "na");
        getInterop().writeMember(e, "i", naInteropValue);
        Object result = getInterop().readMember(e, "i");
        assertTrue("index 0 is NA in the updated vector", RRuntime.isNA(getEnvIntValue(result)));
    }

    private static int getEnvIntValue(Object value) {
        if (value instanceof Integer) {
            return (int) value;
        } else if (value instanceof RIntVector) {
            return ((RIntVector) value).getDataAt(0);
        }
        fail("unexpected value " + value);
        return -1;
    }

    @Override
    protected TruffleObject[] createTruffleObjects() throws Exception {
        return createEnv("e <- new.env(); e$s <- 'aaa'; e$i <- 123L; e$d <- 123.1; e$b <- TRUE; e$fn <- function(s) {s}; e$n <- NULL; e$l <- 666; lockBinding('l', e); e");
    }

    private static TruffleObject[] createEnv(String txt) {
        Source src = Source.newBuilder("R", txt, "<testenv>").internal(true).buildLiteral();
        return new TruffleObject[]{(TruffleObject) FastRSession.getReceiver(context.eval(src))};
    }

    @Override
    protected String[] getKeys(TruffleObject obj) {
        if (((REnvironment) obj).getName().equals("R_EmptyEnv")) {
            return new String[]{};
        } else {
            return new String[]{"s", "i", "d", "b", "fn", "n", "l"};
        }
    }

    @Override
    protected TruffleObject createEmptyTruffleObject() throws Exception {
        return REnvironment.emptyEnv();
    }
}
