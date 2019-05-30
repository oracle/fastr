/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractRawVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.data.nodes.FastPathVectorAccess.FastPathFromRawAccess;
import com.oracle.truffle.r.runtime.data.nodes.SlowPathVectorAccess.SlowPathFromRawAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;

@ValueType
@ExportLibrary(InteropLibrary.class)
public final class RRaw extends RScalarVector implements RAbstractRawVector {

    protected final byte value;

    RRaw(byte value) {
        this.value = value;
    }

    @ExportMessage
    @SuppressWarnings("static-method")
    boolean isNumber() {
        return true;
    }

    @ExportMessage
    boolean fitsInByte(@CachedLibrary("this.value") InteropLibrary interop) {
        return interop.fitsInByte(value);
    }

    @ExportMessage
    byte asByte(@CachedLibrary("this.value") InteropLibrary interop) throws UnsupportedMessageException {
        return interop.asByte(value);
    }

    @ExportMessage
    boolean fitsInShort(@CachedLibrary("this.value") InteropLibrary interop) {
        return interop.fitsInShort(value);
    }

    @ExportMessage
    short asShort(@CachedLibrary("this.value") InteropLibrary interop) throws UnsupportedMessageException {
        return interop.asShort(value);
    }

    @ExportMessage
    boolean fitsInInt(@CachedLibrary("this.value") InteropLibrary interop) {
        return interop.fitsInShort(value);
    }

    @ExportMessage
    int asInt(@CachedLibrary("this.value") InteropLibrary interop) throws UnsupportedMessageException {
        return interop.asInt(value);
    }

    @ExportMessage
    boolean fitsInLong(@CachedLibrary("this.value") InteropLibrary interop) {
        return interop.fitsInLong(value);
    }

    @ExportMessage
    long asLong(@CachedLibrary("this.value") InteropLibrary interop) throws UnsupportedMessageException {
        return interop.asLong(value);
    }

    @ExportMessage
    boolean fitsInFloat(@CachedLibrary("this.value") InteropLibrary interop) {
        return interop.fitsInFloat(value);
    }

    @ExportMessage
    float asFloat(@CachedLibrary("this.value") InteropLibrary interop) throws UnsupportedMessageException {
        return interop.asFloat(value);
    }

    @ExportMessage
    boolean fitsInDouble(@CachedLibrary("this.value") InteropLibrary interop) {
        return interop.fitsInDouble(value);
    }

    @ExportMessage
    double asDouble(@CachedLibrary("this.value") InteropLibrary interop) throws UnsupportedMessageException {
        return interop.asDouble(value);
    }

    @ExportMessage()
    void toNative() {
        NativeDataAccess.asPointer(this);
    }

    @Override
    public boolean isNA() {
        return false;
    }

    @Override
    public RAbstractVector castSafe(RType type, ConditionProfile isNAProfile, boolean keepAttributes) {
        switch (type) {
            case Raw:
                return this;
            case Integer:
                return RInteger.valueOf(value);
            case Double:
                return RDouble.valueOf(value);
            case Complex:
                return RComplex.valueOf(value, 0.0);
            case Character:
                return RString.valueOf(RRuntime.rawToHexString(value));
            default:
                return null;
        }
    }

    @Override
    public RRawVector materialize() {
        RRawVector result = RDataFactory.createRawVector(new byte[]{value});
        MemoryCopyTracer.reportCopying(this, result);
        return result;
    }

    @Override
    public byte getRawDataAt(int index) {
        assert index == 0;
        return value;
    }

    public byte getValue() {
        return value;
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

    private static final class FastPathAccess extends FastPathFromRawAccess {

        FastPathAccess(RAbstractContainer value) {
            super(value);
        }

        @Override
        protected byte getRawImpl(AccessIterator accessIter, int index) {
            assert index == 0;
            return ((RRaw) accessIter.getStore()).value;
        }
    }

    @Override
    public VectorAccess access() {
        return new FastPathAccess(this);
    }

    private static final SlowPathFromRawAccess SLOW_PATH_ACCESS = new SlowPathFromRawAccess() {
        @Override
        protected byte getRawImpl(AccessIterator accessIter, int index) {
            assert index == 0;
            return ((RRaw) accessIter.getStore()).value;
        }
    };

    @Override
    public VectorAccess slowPathAccess() {
        return SLOW_PATH_ACCESS;
    }
}
