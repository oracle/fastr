/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.ffi.interop;

import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.llvm.spi.NativeTypeLibrary;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.TruffleRLanguage;
import com.oracle.truffle.r.runtime.data.RTruffleObject;

@ExportLibrary(NativeTypeLibrary.class)
@ExportLibrary(InteropLibrary.class)
abstract class NativeArrayExport implements RTruffleObject {

    @SuppressWarnings("static-method")
    @ExportMessage
    public boolean hasNativeType() {
        return true;
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public Object getNativeType(@CachedContext(TruffleRLanguage.class) RContext ctx) {
        return getSulongArrayType(ctx);
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public boolean isPointer() {
        return nativeAddress() != 0;
    }

    @ExportMessage
    public long asPointer() throws UnsupportedMessageException {
        long na = nativeAddress();
        if (na != 0L) {
            return na;
        }
        throw UnsupportedMessageException.create();
    }

    @ExportMessage
    public void toNative() {
        convertToNative();
    }

    protected abstract long allocateNative();

    protected abstract void copyBackFromNative(long nativeAddress);

    protected abstract Object getSulongArrayType(RContext ctx);

    protected abstract long nativeAddress();

    protected abstract long convertToNative();

}
