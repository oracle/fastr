/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.data;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

/**
 * Represents an {@code RRaw} value passed to the interop. This value should never appear in the
 * FastR execution, it is only passed to interop and converted back to its original RRaw value if
 * passed back to FastR.
 */
@ExportLibrary(InteropLibrary.class)
public class RInteropRaw implements RTruffleObject {

    private final RRaw value;

    public RInteropRaw(RRaw value) {
        this.value = value;
    }

    public Object getValue() {
        return value;
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public final boolean isNumber() {
        return true;
    }

    @ExportMessage
    public final boolean fitsInByte() {
        if (!isNumber()) {
            return false;
        }
        return getInterop(value).fitsInByte(value.getValue());
    }

    @ExportMessage
    public final byte asByte() throws UnsupportedMessageException {
        return getInterop(value).asByte(value.getValue());
    }

    @ExportMessage
    public final boolean fitsInShort() {
        return getInterop(value).fitsInShort(value.getValue());
    }

    @ExportMessage
    public final short asShort() throws UnsupportedMessageException {
        return getInterop(value).asShort(value.getValue());
    }

    @ExportMessage
    public final boolean fitsInInt() {
        return getInterop(value).fitsInInt(value.getValue());
    }

    @ExportMessage
    public final int asInt() throws UnsupportedMessageException {
        return getInterop(value).asInt(value.getValue());
    }

    @ExportMessage
    public final boolean fitsInLong() {
        return getInterop(value).fitsInLong(value.getValue());
    }

    @ExportMessage
    public final long asLong() throws UnsupportedMessageException {
        return getInterop(value).asLong(value.getValue());
    }

    @ExportMessage
    public final boolean fitsInFloat() {
        return getInterop(value).fitsInFloat(value.getValue());
    }

    @ExportMessage
    public final float asFloat() throws UnsupportedMessageException {
        return getInterop(value).asFloat(value.getValue());
    }

    @ExportMessage
    public final boolean fitsInDouble() {
        return getInterop(value).fitsInDouble(value.getValue());
    }

    @ExportMessage
    public final double asDouble() throws UnsupportedMessageException {
        return getInterop(value).asDouble(value.getValue());
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof RInteropRaw)) {
            return false;
        }
        return value.equals(((RInteropRaw) obj).value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    private static InteropLibrary getInterop(RRaw value) {
        return InteropLibrary.getFactory().create(value.getValue());
    }
}
