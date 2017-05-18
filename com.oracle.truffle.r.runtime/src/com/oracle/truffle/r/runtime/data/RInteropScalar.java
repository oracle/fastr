/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.runtime.data;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.ValueType;
import com.oracle.truffle.r.runtime.RType;

@ValueType
public abstract class RInteropScalar extends RScalar {

    public abstract Object getRValue();

    public abstract Class<?> getJavaType();

    @ValueType
    public static final class RInteropByte extends RInteropScalar {

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
    public static final class RInteropChar extends RInteropScalar {

        private final char value;

        private RInteropChar(char value) {
            this.value = value;
        }

        public static RInteropChar valueOf(char value) {
            return new RInteropChar(value);
        }

        public char getValue() {
            return value;
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
    public static final class RInteropFloat extends RInteropScalar {

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
    public static final class RInteropLong extends RInteropScalar {

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
    public static final class RInteropShort extends RInteropScalar {

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
}
