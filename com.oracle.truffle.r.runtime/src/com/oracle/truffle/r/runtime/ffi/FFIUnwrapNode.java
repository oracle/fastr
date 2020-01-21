/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.ffi;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.NativeDataAccess;
import com.oracle.truffle.r.runtime.data.NativeDataAccess.NativeMirror;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.interop.Foreign2R;

/**
 * Unwraps a value that is arriving from the native side. This unwrapping should only happen for
 * arguments and return values that represent R data structures, not for primitive values.
 */
@GenerateUncached
@ImportStatic({RRuntime.class, DSLConfig.class})
public abstract class FFIUnwrapNode extends Node {

    public abstract Object execute(Object x);

    @Specialization()
    public Object unwrap(String x) {
        return x;
    }

    @Specialization()
    public Object unwrap(NativeMirror x) {
        return x.getDelegate();
    }

    @Specialization(guards = {"!isNativeMirror(x)", "interop.isPointer(x)"}, limit = "getInteropLibraryCacheSize()")
    public Object unwrapPointer(TruffleObject x,
                    @CachedLibrary("x") InteropLibrary interop) {
        try {
            long address = interop.asPointer(x);
            if (address == 0) {
                // Users are expected to use R_NULL, but at least when embedding, GNU R
                // seems to be tolerant to NULLs.
                return RNull.instance;
            }
            return NativeDataAccess.lookup(address);
        } catch (UnsupportedMessageException e) {
            throw RInternalError.shouldNotReachHere();
        }
    }

    @Specialization(guards = {"!isNativeMirror(x)", "!interop.isPointer(x)"}, limit = "getInteropLibraryCacheSize()")
    public Object unwrapForeign(TruffleObject x,
                    @CachedLibrary("x") InteropLibrary interop) {
        try {
            Object res = Foreign2R.unbox(x, interop);
            return res != null ? res : x;
        } catch (UnsupportedMessageException e) {
            throw RInternalError.shouldNotReachHere();
        }
    }

    @Specialization(guards = "!isStringORTruffleObject(x)")
    public Object unwrapFallback(Object x) {
        CompilerDirectives.transferToInterpreter();
        throw RInternalError.shouldNotReachHere("unexpected primitive value of class " + x.getClass().getSimpleName());
    }

    protected static boolean isStringORTruffleObject(Object x) {
        return x instanceof String || x instanceof TruffleObject;
    }

    protected static boolean isNativeMirror(Object x) {
        return x instanceof NativeMirror;
    }

    public static FFIUnwrapNode create() {
        return FFIUnwrapNodeGen.create();
    }
}
