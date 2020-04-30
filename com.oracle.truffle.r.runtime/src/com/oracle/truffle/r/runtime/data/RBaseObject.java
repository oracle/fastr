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
package com.oracle.truffle.r.runtime.data;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.TruffleRLanguage;
import com.oracle.truffle.r.runtime.data.NativeDataAccess.NativeMirror;

/**
 * R values that are publicly flowing through the interpreter. Be aware that also the primitive
 * values {@link Integer}, {@link Double}, {@link Byte}, {@link String}, {@link RRaw} and
 * {@link RComplex} flow but are not implementing this interface.
 */
@ExportLibrary(InteropLibrary.class)
public abstract class RBaseObject extends RTruffleBaseObject {

    // mask values are the same as in GNU R
    // as is the layout of data (but it's never exposed so it does not matter for correctness)
    public static final int S4_MASK = 1 << 4;
    public static final int GP_BITS_MASK_SHIFT = 8;
    public static final int GP_BITS_MASK = 0xFFFF << GP_BITS_MASK_SHIFT;

    public static final int S4_MASK_SHIFTED = 1 << (4 + GP_BITS_MASK_SHIFT);
    public static final int ASCII_MASK_SHIFTED = 1 << 14;

    public static final int BYTES_MASK = 1 << 1;
    public static final int LATIN1_MASK = 1 << 2;
    public static final int UTF8_MASK = 1 << 3;
    public static final int CACHED_MASK = 1 << 5;
    public static final int ASCII_MASK = 1 << 6;

    private int typedValueInfo;

    private NativeMirror nativeMirror;

    public abstract RType getRType();

    final void setNativeMirror(NativeMirror mirror) {
        this.nativeMirror = mirror;
    }

    public final NativeMirror getNativeMirror() {
        return nativeMirror;
    }

    public final int getTypedValueInfo() {
        return typedValueInfo;
    }

    public final void setTypedValueInfo(int value) {
        // TODO
        // RArgsValuesAndNames can get serialized under specific circumstances (ggplot2 does that)
        // and getTypedValueInfo() must be defined for this to work,
        // but should setTypedValueInfo(RArgsValuesAndNames) work as well
        if (this instanceof RArgsValuesAndNames) {
            throw RInternalError.shouldNotReachHere();
        }

        // TODO This gets called from RSerialize, should accept a non 0 value?
        if (this instanceof RPromise) {
            // do nothing
            return;
        }
        typedValueInfo = value;
    }

    public final int getGPBits() {
        return (getTypedValueInfo() & GP_BITS_MASK) >>> GP_BITS_MASK_SHIFT;
    }

    public final void setGPBits(int gpbits) {
        setTypedValueInfo((getTypedValueInfo() & ~GP_BITS_MASK) | (gpbits << GP_BITS_MASK_SHIFT));
    }

    public final boolean isS4() {
        return (getTypedValueInfo() & S4_MASK_SHIFTED) != 0;
    }

    public final void setS4() {
        setTypedValueInfo(getTypedValueInfo() | S4_MASK_SHIFTED);
    }

    public final void unsetS4() {
        setTypedValueInfo(getTypedValueInfo() & ~S4_MASK_SHIFTED);
    }

    /**
     * Slow-path implementation of {@link #getMetaObject()} that does not use interop library. We
     * need a dedicated method for this, so that we can use it in {@code isMetaInstance} message in
     * {@link RType} without triggering an infinite recursion in the interop library assertions.
     */
    public RType getMetaType() {
        return getRType();
    }

    @ExportMessage
    @Override
    public boolean hasMetaObject() {
        return true;
    }

    @ExportMessage
    @Override
    public Object getMetaObject() {
        return getMetaType();
    }

    @ExportMessage
    @TruffleBoundary
    public Object toDisplayString(boolean allowSideEffects,
                    @CachedContext(TruffleRLanguage.class) RContext ctx) {
        return TruffleRLanguage.toDisplayString(ctx, this, allowSideEffects);
    }
}
