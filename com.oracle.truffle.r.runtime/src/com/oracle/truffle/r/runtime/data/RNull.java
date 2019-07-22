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

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.interop.RNullMRContextState;

@ExportLibrary(InteropLibrary.class)
public final class RNull extends RBaseObject implements RScalar {

    public static final RNull instance = new RNull();

    private RNull() {
        // singleton
    }

    @Override
    public String toString() {
        return RRuntime.NULL;
    }

    @Override
    public RType getRType() {
        return RType.Null;
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    boolean isNull() {
        return RContext.getInstance().stateRNullMR.isNull();
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    boolean isPointer() {
        return true;
    }

    @ExportMessage
    long asPointer() {
        return NativeDataAccess.asPointer(this);
    }

    @ExportMessage
    void toNative() {
        NativeDataAccess.asPointer(this);
    }

    /**
     * Workaround to avoid NFI converting {@link RNull} to {@code null}.
     */
    public static boolean setIsNull(boolean value) {
        RNullMRContextState state = RContext.getInstance().stateRNullMR;
        boolean prev = state.isNull();
        state.setIsNull(value);
        return prev;
    }
}
