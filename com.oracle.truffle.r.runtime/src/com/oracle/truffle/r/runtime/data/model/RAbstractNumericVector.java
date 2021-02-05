/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.data.model;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RRawVector;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary;

/**
 * Provides interop messages for numeric vectors. <br>
 * <ul>
 * <li>{@link RIntVector}</li>
 * <li>{@link RDoubleVector}</li>
 * <li>{@link RRawVector}</li>
 * </ul>
 * 
 */
@ExportLibrary(InteropLibrary.class)
public abstract class RAbstractNumericVector extends RAbstractAtomicVector {

    protected Object getScalarValue(VectorDataLibrary dataLib) {
        assert dataLib.getLength(getData()) == 1 && getLength() == 1;
        return dataLib.getDataAtAsObject(getData(), 0);
    }

    @ExportMessage
    public final boolean isNull(
                    @CachedLibrary(limit = DATA_LIB_LIMIT) VectorDataLibrary dataLib,
                    @Cached.Exclusive @Cached("createBinaryProfile()") ConditionProfile isScalar) {
        if (!isScalar.profile(isScalar(dataLib))) {
            return false;
        }
        return dataLib.isNAAt(getData(), 0);
    }

    @ExportMessage
    public final boolean isNumber(@CachedLibrary(limit = DATA_LIB_LIMIT) VectorDataLibrary dataLib) {
        if (!isScalar(dataLib)) {
            return false;
        }
        return !dataLib.isNAAt(getData(), 0);
    }

    @ExportMessage
    public final boolean fitsInByte(
                    @CachedLibrary(limit = DATA_LIB_LIMIT) VectorDataLibrary dataLib,
                    @CachedLibrary(limit = "1") InteropLibrary valueInterop) {
        if (!isNumber(dataLib)) {
            return false;
        }
        Object value = getScalarValue(dataLib);
        return valueInterop.fitsInByte(value);
    }

    @ExportMessage
    public final byte asByte(
                    @CachedLibrary(limit = DATA_LIB_LIMIT) VectorDataLibrary dataLib,
                    @CachedLibrary(limit = "1") InteropLibrary valueInterop,
                    @Cached.Shared("isNumber") @Cached("createBinaryProfile()") ConditionProfile isNumber) throws UnsupportedMessageException {
        if (!isNumber.profile(isNumber(dataLib))) {
            throw UnsupportedMessageException.create();
        }
        Object value = getScalarValue(dataLib);
        return valueInterop.asByte(value);
    }

    @ExportMessage
    public final boolean fitsInShort(
                    @CachedLibrary(limit = DATA_LIB_LIMIT) VectorDataLibrary dataLib,
                    @CachedLibrary(limit = "1") InteropLibrary valueInterop) {
        if (!isNumber(dataLib)) {
            return false;
        }
        Object value = getScalarValue(dataLib);
        return valueInterop.fitsInShort(value);
    }

    @ExportMessage
    public final short asShort(
                    @CachedLibrary(limit = DATA_LIB_LIMIT) VectorDataLibrary dataLib,
                    @CachedLibrary(limit = "1") InteropLibrary valueInterop,
                    @Cached.Shared("isNumber") @Cached("createBinaryProfile()") ConditionProfile isNumber) throws UnsupportedMessageException {
        if (!isNumber.profile(isNumber(dataLib))) {
            throw UnsupportedMessageException.create();
        }
        Object value = getScalarValue(dataLib);
        return valueInterop.asShort(value);
    }

    @ExportMessage
    public final boolean fitsInInt(
                    @CachedLibrary(limit = DATA_LIB_LIMIT) VectorDataLibrary dataLib,
                    @CachedLibrary(limit = "1") InteropLibrary valueInterop) {
        if (!isNumber(dataLib)) {
            return false;
        }
        Object value = getScalarValue(dataLib);
        return valueInterop.fitsInInt(value);
    }

    @ExportMessage
    public final int asInt(
                    @CachedLibrary(limit = DATA_LIB_LIMIT) VectorDataLibrary dataLib,
                    @CachedLibrary(limit = "1") InteropLibrary valueInterop,
                    @Cached.Shared("isNumber") @Cached("createBinaryProfile()") ConditionProfile isNumber) throws UnsupportedMessageException {
        if (!isNumber.profile(isNumber(dataLib))) {
            throw UnsupportedMessageException.create();
        }
        Object value = getScalarValue(dataLib);
        return valueInterop.asInt(value);
    }

    @ExportMessage
    public final boolean fitsInLong(
                    @CachedLibrary(limit = DATA_LIB_LIMIT) VectorDataLibrary dataLib,
                    @CachedLibrary(limit = "1") InteropLibrary valueInterop) {
        if (!isNumber(dataLib)) {
            return false;
        }
        Object value = getScalarValue(dataLib);
        return valueInterop.fitsInLong(value);
    }

    @ExportMessage
    public final long asLong(
                    @CachedLibrary(limit = DATA_LIB_LIMIT) VectorDataLibrary dataLib,
                    @CachedLibrary(limit = "1") InteropLibrary valueInterop,
                    @Cached.Shared("isNumber") @Cached("createBinaryProfile()") ConditionProfile isNumber) throws UnsupportedMessageException {
        if (!isNumber.profile(isNumber(dataLib))) {
            throw UnsupportedMessageException.create();
        }
        Object value = getScalarValue(dataLib);
        return valueInterop.asLong(value);
    }

    @ExportMessage
    public final boolean fitsInFloat(
                    @CachedLibrary(limit = DATA_LIB_LIMIT) VectorDataLibrary dataLib,
                    @CachedLibrary(limit = "1") InteropLibrary valueInterop) {
        if (!isNumber(dataLib)) {
            return false;
        }
        Object value = getScalarValue(dataLib);
        return valueInterop.fitsInFloat(value);
    }

    @ExportMessage
    public final float asFloat(
                    @CachedLibrary(limit = DATA_LIB_LIMIT) VectorDataLibrary dataLib,
                    @CachedLibrary(limit = "1") InteropLibrary valueInterop,
                    @Cached.Shared("isNumber") @Cached("createBinaryProfile()") ConditionProfile isNumber) throws UnsupportedMessageException {
        if (!isNumber.profile(isNumber(dataLib))) {
            throw UnsupportedMessageException.create();
        }
        Object value = getScalarValue(dataLib);
        return valueInterop.asFloat(value);
    }

    @ExportMessage
    public final boolean fitsInDouble(
                    @CachedLibrary(limit = DATA_LIB_LIMIT) VectorDataLibrary dataLib,
                    @CachedLibrary(limit = "1") InteropLibrary valueInterop) {
        if (!isNumber(dataLib)) {
            return false;
        }
        Object value = getScalarValue(dataLib);
        return valueInterop.fitsInDouble(value);
    }

    @ExportMessage
    public final double asDouble(
                    @CachedLibrary(limit = DATA_LIB_LIMIT) VectorDataLibrary dataLib,
                    @CachedLibrary(limit = "1") InteropLibrary valueInterop,
                    @Cached.Shared("isNumber") @Cached("createBinaryProfile()") ConditionProfile isNumber) throws UnsupportedMessageException {
        if (!isNumber.profile(isNumber(dataLib))) {
            throw UnsupportedMessageException.create();
        }
        Object value = getScalarValue(dataLib);
        return valueInterop.asDouble(value);
    }
}
