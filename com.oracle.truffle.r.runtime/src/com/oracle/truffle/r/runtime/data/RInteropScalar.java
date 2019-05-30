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

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.ValueType;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;

@ValueType
public abstract class RInteropScalar extends RScalar {

    public abstract Object getRValue();

    public abstract Class<?> getJavaType();

    protected abstract Object getValueObject();

    @ImportStatic(DSLConfig.class)
    @ExportLibrary(InteropLibrary.class)
    protected abstract static class RNumericInteropScalar extends RInteropScalar {

        @ExportMessage
        @SuppressWarnings("static-method")
        boolean isNumber() {
            return true;
        }

        @ExportMessage(limit = "getInteropLibraryCacheSize()")
        boolean fitsInByte(@CachedLibrary("this.getValueObject()") InteropLibrary interop) {
            return interop.fitsInByte(getValueObject());
        }

        @ExportMessage(limit = "getInteropLibraryCacheSize()")
        byte asByte(@CachedLibrary("this.getValueObject()") InteropLibrary interop) throws UnsupportedMessageException {
            return interop.asByte(getValueObject());
        }

        @ExportMessage(limit = "getInteropLibraryCacheSize()")
        boolean fitsInShort(@CachedLibrary("this.getValueObject()") InteropLibrary interop) {
            return interop.fitsInShort(getValueObject());
        }

        @ExportMessage(limit = "getInteropLibraryCacheSize()")
        short asShort(@CachedLibrary("this.getValueObject()") InteropLibrary interop) throws UnsupportedMessageException {
            return interop.asShort(getValueObject());
        }

        @ExportMessage(limit = "getInteropLibraryCacheSize()")
        boolean fitsInInt(@CachedLibrary("this.getValueObject()") InteropLibrary interop) {
            return interop.fitsInShort(getValueObject());
        }

        @ExportMessage(limit = "getInteropLibraryCacheSize()")
        int asInt(@CachedLibrary("this.getValueObject()") InteropLibrary interop) throws UnsupportedMessageException {
            return interop.asInt(getValueObject());
        }

        @ExportMessage(limit = "getInteropLibraryCacheSize()")
        boolean fitsInLong(@CachedLibrary("this.getValueObject()") InteropLibrary interop) {
            return interop.fitsInLong(getValueObject());
        }

        @ExportMessage(limit = "getInteropLibraryCacheSize()")
        long asLong(@CachedLibrary("this.getValueObject()") InteropLibrary interop) throws UnsupportedMessageException {
            return interop.asLong(getValueObject());
        }

        @ExportMessage(limit = "getInteropLibraryCacheSize()")
        boolean fitsInFloat(@CachedLibrary("this.getValueObject()") InteropLibrary interop) {
            return interop.fitsInFloat(getValueObject());
        }

        @ExportMessage(limit = "getInteropLibraryCacheSize()")
        float asFloat(@CachedLibrary("this.getValueObject()") InteropLibrary interop) throws UnsupportedMessageException {
            return interop.asFloat(getValueObject());
        }

        @ExportMessage(limit = "getInteropLibraryCacheSize()")
        boolean fitsInDouble(@CachedLibrary("this.getValueObject()") InteropLibrary interop) {
            return interop.fitsInDouble(getValueObject());
        }

        @ExportMessage(limit = "getInteropLibraryCacheSize()")
        double asDouble(@CachedLibrary("this.getValueObject()") InteropLibrary interop) throws UnsupportedMessageException {
            return interop.asDouble(getValueObject());
        }
    }

    @ValueType
    public static final class RInteropByte extends RNumericInteropScalar {

        private final byte value;

        private RInteropByte(byte value) {
            this.value = value;
        }

        public static RInteropByte valueOf(byte value) {
            return new RInteropByte(value);
        }

        public byte getValue() {
            return value;
        }

        @Override
        protected Object getValueObject() {
            return getValue();
        }

        @Override
        public Object getRValue() {
            return ((Byte) value).intValue();
        }

        @Override
        public RType getRType() {
            return RType.RInteropByte;
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return Byte.toString(value);
        }

        @Override
        public Class<?> getJavaType() {
            return Byte.TYPE;
        }
    }

    @ValueType
    @ExportLibrary(InteropLibrary.class)
    public static final class RInteropChar extends RInteropScalar {

        private final char value;

        private RInteropChar(char value) {
            this.value = value;
        }

        @SuppressWarnings("static-method")
        @ExportMessage
        boolean isString() {
            return true;
        }

        @ExportMessage
        String asString() {
            return Character.toString(value);
        }

        public static RInteropChar valueOf(char value) {
            return new RInteropChar(value);
        }

        public char getValue() {
            return value;
        }

        @Override
        protected Object getValueObject() {
            return getValue();
        }

        @Override
        public Object getRValue() {
            return Character.toString(value);
        }

        @Override
        public RType getRType() {
            return RType.RInteropChar;
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return Character.toString(value);
        }

        @Override
        public Class<?> getJavaType() {
            return Character.TYPE;
        }
    }

    @ValueType
    public static final class RInteropFloat extends RNumericInteropScalar {

        private final float value;

        private RInteropFloat(float value) {
            this.value = value;
        }

        public static RInteropFloat valueOf(float value) {
            return new RInteropFloat(value);
        }

        public float getValue() {
            return value;
        }

        @Override
        protected Object getValueObject() {
            return getValue();
        }

        @Override
        public Object getRValue() {
            return ((Float) value).doubleValue();
        }

        @Override
        public RType getRType() {
            return RType.RInteropFloat;
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return Float.toString(value);
        }

        @Override
        public Class<?> getJavaType() {
            return Float.TYPE;
        }
    }

    @ValueType
    public static final class RInteropLong extends RNumericInteropScalar {

        private final long value;

        private RInteropLong(long value) {
            this.value = value;
        }

        public static RInteropLong valueOf(long value) {
            return new RInteropLong(value);
        }

        public long getValue() {
            return value;
        }

        @Override
        protected Object getValueObject() {
            return getValue();
        }

        @Override
        public Object getRValue() {
            return ((Long) value).doubleValue();
        }

        @Override
        public RType getRType() {
            return RType.RInteropLong;
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return Long.toString(value);
        }

        @Override
        public Class<?> getJavaType() {
            return Long.TYPE;
        }
    }

    @ValueType
    public static final class RInteropShort extends RNumericInteropScalar {

        private final short value;

        private RInteropShort(short value) {
            this.value = value;
        }

        public static RInteropShort valueOf(short value) {
            return new RInteropShort(value);
        }

        public short getValue() {
            return value;
        }

        @Override
        protected Object getValueObject() {
            return getValue();
        }

        @Override
        public Object getRValue() {
            return ((Short) value).intValue();
        }

        @Override
        public RType getRType() {
            return RType.RInteropShort;
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return Short.toString(value);
        }

        @Override
        public Class<?> getJavaType() {
            return Short.TYPE;
        }
    }

    /**
     * Represents an {@code NA} value passed to the interop. This value should never appear in the
     * FastR execution, it is only passed to interop and converted back to primitive value if passed
     * back to FastR.
     */
    @ExportLibrary(InteropLibrary.class)
    public static final class RInteropNA implements RTruffleObject {
        public static final RInteropNA INT = new RInteropNA(RRuntime.INT_NA);
        public static final RInteropNA DOUBLE = new RInteropNA(RRuntime.DOUBLE_NA);
        public static final RInteropNA STRING = new RInteropNA(RRuntime.STRING_NA);
        public static final RInteropNA LOGICAL = new RInteropNA(RRuntime.LOGICAL_NA);

        private final Object value;

        private RInteropNA(Object value) {
            this.value = value;
        }

        public Object getValue() {
            return value;
        }

        @SuppressWarnings("static-method")
        @ExportMessage
        boolean isNull() {
            return true;
        }
    }
}
