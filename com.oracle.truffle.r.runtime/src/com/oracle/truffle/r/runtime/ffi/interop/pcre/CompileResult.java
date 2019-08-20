/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.ffi.interop.pcre;

import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.data.RTruffleObject;
import com.oracle.truffle.r.runtime.ffi.PCRERFFI;
import com.oracle.truffle.r.runtime.ffi.PCRERFFI.Result;
import com.oracle.truffle.r.runtime.interop.Foreign2R;

@ImportStatic(DSLConfig.class)
@ExportLibrary(InteropLibrary.class)
public final class CompileResult implements RTruffleObject {
    private PCRERFFI.Result result;

    @SuppressWarnings("static-method")
    @ExportMessage
    boolean isExecutable() {
        return true;
    }

    @ExportMessage
    Object execute(Object[] arguments,
                    @CachedLibrary(limit = "getInteropLibraryCacheSize()") InteropLibrary interop) {
        try {
            Object arg1 = arguments[1];
            if (arg1 instanceof TruffleObject) {
                if (interop.isNull(arg1)) {
                    arg1 = null;
                } else {
                    arg1 = Foreign2R.unbox(arg1, interop);
                }
            }
            set((long) arguments[0], (String) arg1, (int) arguments[2]);
            return this;
        } catch (InteropException e) {
            throw RInternalError.shouldNotReachHere(e);
        }
    }

    public void set(long pcreResult, String errorMessage, int errOffset) {
        result = new Result(pcreResult, errorMessage, errOffset);
    }

    public PCRERFFI.Result getResult() {
        return result;
    }

}
