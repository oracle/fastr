/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives.ValueType;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.r.runtime.RRuntime;

@ValueType
@ExportLibrary(InteropLibrary.class)
public final class RRaw implements RTruffleObject, ScalarWrapper {

    private final byte value;

    RRaw(byte value) {
        this.value = value;
    }

    public byte getValue() {
        return value;
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public boolean isNumber() {
        return true;
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public boolean fitsInByte() {
        return true;
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public byte asByte() {
        return getValue();
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public boolean fitsInShort() {
        return true;
    }

    @ExportMessage
    public short asShort() throws UnsupportedMessageException {
        return getInterop().asShort(getValue());
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public boolean fitsInInt() {
        return true;
    }

    @ExportMessage
    public int asInt() throws UnsupportedMessageException {
        return getInterop().asInt(getValue());
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public boolean fitsInLong() {
        return true;
    }

    @ExportMessage
    public long asLong() throws UnsupportedMessageException {
        return getInterop().asLong(getValue());
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public boolean fitsInFloat() {
        return true;
    }

    @ExportMessage
    public float asFloat() throws UnsupportedMessageException {
        return getInterop().asFloat(getValue());
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public boolean fitsInDouble() {
        return true;
    }

    @ExportMessage
    public double asDouble() throws UnsupportedMessageException {
        return getInterop().asDouble(getValue());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof RRaw) {
            return value == ((RRaw) obj).value;
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return value;
    }

    @Override
    public String toString() {
        return RRuntime.rawToHexString(value);
    }

    public static RRaw valueOf(byte value) {
        return new RRaw(value);
    }

    private InteropLibrary getInterop() {
        return InteropLibrary.getFactory().create(getValue());
    }
}
