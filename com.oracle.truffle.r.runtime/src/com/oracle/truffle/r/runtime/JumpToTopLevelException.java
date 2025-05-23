/*
 * Copyright (c) 2013, 2023, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.exception.AbstractTruffleException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

/**
 * Thrown whenever the system wants to return to the top level, e.g. "Q" in browser, "c" in the
 * {@code quit} builtin.
 */
@ExportLibrary(InteropLibrary.class)
public final class JumpToTopLevelException extends AbstractTruffleException {

    private static final String CANCEL_QUITTING_MEMBER_NAME = "FastR_error_cancelQuitting";

    private static final long serialVersionUID = 1L;

    @ExportMessage
    @SuppressWarnings("static-method")
    boolean hasMembers() {
        return true;
    }

    @ExportMessage
    @SuppressWarnings("static-method")
    Object getMembers(@SuppressWarnings("unused") boolean includeInternal) {
        return new String[]{CANCEL_QUITTING_MEMBER_NAME};
    }

    @ExportMessage
    @SuppressWarnings("static-method")
    boolean isMemberReadable(String name) {
        return name.equals(CANCEL_QUITTING_MEMBER_NAME);
    }

    @ExportMessage
    @SuppressWarnings("static-method")
    Object readMember(String name) throws UnknownIdentifierException {
        if (name.equals(CANCEL_QUITTING_MEMBER_NAME)) {
            return true;
        }
        CompilerDirectives.transferToInterpreterAndInvalidate();
        throw UnknownIdentifierException.create(name);
    }
}
