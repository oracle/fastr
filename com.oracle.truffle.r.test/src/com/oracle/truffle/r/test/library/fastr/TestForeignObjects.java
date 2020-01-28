/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.test.library.fastr;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import org.junit.Before;
import org.junit.Test;

import com.oracle.truffle.r.nodes.builtin.fastr.FastRInterop;
import com.oracle.truffle.r.test.TestBase;
import static com.oracle.truffle.r.test.library.fastr.Utils.errorIn;

public class TestForeignObjects extends TestBase {

    private static final String CREATE_FO = "fof <- new('com.oracle.truffle.r.test.library.fastr.TestForeignObjects$FOFactory'); fo <- fof$createInvocableNotReadable(); ";

    @Before
    public void testInit() {
        FastRInterop.testingMode();
    }

    @Test
    public void testToByte() {
        String member = InvocableNotReadable.MEMBER_NAME;

        assertEvalFastR(CREATE_FO + "names(fo)", "print('" + member + "')");
        assertEvalFastR(CREATE_FO + "fo", "cat('[polyglot value]\n$" + member + "\n[not readable value]\n\n')");
        assertEvalFastR(CREATE_FO + "fo$" + member, errorIn("fo$" + member, "invalid index/identifier during foreign access: " + member));
        assertEvalFastR(CREATE_FO + "fo$" + member + "()", errorIn("fo$" + member, "invalid index/identifier during foreign access: " + member));
        assertEvalFastR(CREATE_FO + "fo@" + member + "()", "print(42)");

        assertEvalFastR(CREATE_FO + "as.list(fo)", "cat('named list()\n')");
    }

    public static class FOFactory {
        public static TruffleObject createInvocableNotReadable() {
            return new InvocableNotReadable();
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public static class InvocableNotReadable implements TruffleObject {
        private static final String MEMBER_NAME = "invocable";
        private static ExecutableTO invocable = new ExecutableTO();

        @ExportMessage
        public boolean hasMembers() {
            return true;
        }

        @SuppressWarnings("unused")
        @ExportMessage
        public Object getMembers(boolean includeInternals) {
            return new Array(MEMBER_NAME);
        }

        @SuppressWarnings("unused")
        @ExportMessage
        public boolean isMemberReadable(String name) {
            return false;
        }

        @SuppressWarnings("unused")
        @ExportMessage
        public boolean isMemberInvocable(String name) {
            return MEMBER_NAME.equals(name);
        }

        @SuppressWarnings("unused")
        @ExportMessage
        public Object invokeMember(String name, Object... args) throws UnsupportedMessageException {
            if (!MEMBER_NAME.equals(name)) {
                throw UnsupportedMessageException.create();
            }
            return invocable.execute(args);
        }

        @SuppressWarnings("unused")
        @ExportMessage
        public Object readMember(String name) throws UnsupportedMessageException {
            throw UnsupportedMessageException.create();
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public static class ExecutableTO implements TruffleObject {
        @ExportMessage
        public boolean isExecutable() {
            return true;
        }

        @SuppressWarnings("unused")
        @ExportMessage
        public Object execute(Object... args) {
            return 42;
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public static class Array implements TruffleObject {
        private final Object[] a;

        public Array(Object... a) {
            this.a = a;
        }

        @ExportMessage
        public boolean hasArrayElements() {
            return true;
        }

        @ExportMessage
        public long getArraySize() {
            return a.length;
        }

        @ExportMessage
        public boolean isArrayElementReadable(long i) {
            return i <= a.length;
        }

        @ExportMessage
        public Object readArrayElement(long i) {
            return a[(int) i];
        }

    }

}
