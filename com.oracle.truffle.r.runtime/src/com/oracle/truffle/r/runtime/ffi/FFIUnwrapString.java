/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.charset.StandardCharsets;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.ffi.util.NativeMemory;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

/**
 * Converts to {@code java.lang.String} an object coming from the native code which is supposed to
 * represent a string on the native side. Can unwrap string like objects coming from both NFI and
 * Sulong.
 *
 * TODO: in Sulong, we should convert the native objects proactively by calling polyglot_from_string
 * in order to get polyglot object (that supports asString message) and pass that as the argument to
 * the actual up-call. However, in any case, we want to use this unwrap node.
 */
@GenerateUncached
public abstract class FFIUnwrapString extends RBaseNode {

    public abstract String execute(Object obj);

    @Specialization
    String doString(String value) {
        return value;
    }

    // Note: this specialization should cover also VectorRFFIWrapper of CharSXP and NativeCharArray
    @Specialization(guards = {"!isString(value)", "interopLib.isString(value)"}, limit = "getInteropLibraryCacheSize()")
    String doInteropString(Object value,
                    @CachedLibrary("value") InteropLibrary interopLib) {
        try {
            return interopLib.asString(value);
        } catch (UnsupportedMessageException e) {
            throw RInternalError.shouldNotReachHere(e);
        }
    }

    @Specialization(guards = {"!isString(value)", "!interopLib.isString(value)", "!interopLib.isPointer(value)"}, limit = "getInteropLibraryCacheSize()")
    String doToNativeAndPointer(Object value,
                    @CachedLibrary("value") InteropLibrary interopLib) {
        interopLib.toNative(value);
        if (!interopLib.isPointer(value)) {
            CompilerDirectives.transferToInterpreter();
            throw RInternalError.shouldNotReachHere(value.toString());
        }
        return doPointer(value, interopLib);
    }

    @Specialization(guards = {"!isString(value)", "!interopLib.isString(value)", "interopLib.isPointer(value)"}, limit = "getInteropLibraryCacheSize()")
    String doPointer(Object value,
                    @CachedLibrary("value") InteropLibrary interopLib) {
        try {
            long ptr = interopLib.asPointer(value);
            return copyCString(ptr);
        } catch (UnsupportedMessageException e) {
            throw RInternalError.shouldNotReachHere(e);
        }
    }

    @TruffleBoundary
    private static String copyCString(long ptr) {
        return NativeMemory.copyCString(ptr, StandardCharsets.US_ASCII);
    }

    static boolean isString(Object obj) {
        return obj instanceof String;
    }
}
