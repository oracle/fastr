/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.r.runtime.RRuntime;

/**
 * Represents an {@code NA} value passed to the interop. This value should never appear in the FastR
 * execution, it is only passed to interop and converted back to primitive value if passed back to
 * FastR.
 */
@ExportLibrary(InteropLibrary.class)
public class RInteropNA implements RTruffleObject {
    public static final RInteropNA INT = new RInteropNA(RRuntime.INT_NA);
    public static final RInteropNA DOUBLE = new RInteropNA(RRuntime.DOUBLE_NA);
    public static final RInteropNA STRING = new RInteropNA(RRuntime.STRING_NA);
    public static final RInteropNA LOGICAL = new RInteropNA(RRuntime.LOGICAL_NA, RRuntime.INT_NA);

    private final Object value;
    private final Object nativeValue;

    protected RInteropNA(Object value) {
        this(value, value);
    }

    protected RInteropNA(Object value, Object nativeValue) {
        this.value = value;
        this.nativeValue = nativeValue;
    }

    public Object getValue() {
        return value;
    }

    public Object getNativeValue() {
        return nativeValue;
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    boolean isNull() {
        return true;
    }

    public static final class RInteropComplexNA extends RInteropNA {
        public RInteropComplexNA(RComplex value) {
            super(value);
            assert RRuntime.isNA(value);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof RInteropComplexNA)) {
                return false;
            }
            return getValue().equals(((RInteropComplexNA) obj).getValue());
        }

        @Override
        public int hashCode() {
            return getValue().hashCode();
        }
    }
}
