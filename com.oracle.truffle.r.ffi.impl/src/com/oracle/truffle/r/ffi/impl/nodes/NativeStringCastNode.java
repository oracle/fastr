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
package com.oracle.truffle.r.ffi.impl.nodes;

import static com.oracle.truffle.r.runtime.data.NativeDataAccess.readNativeString;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
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

@ImportStatic(DSLConfig.class)
@GenerateUncached
public abstract class NativeStringCastNode extends Node {

    public static NativeStringCastNode create() {
        return NativeStringCastNodeGen.create();
    }

    public static NativeStringCastNode getUncached() {
        return NativeStringCastNodeGen.getUncached();
    }

    public abstract String executeObject(Object s);

    @Specialization
    @TruffleBoundary
    String handleString(String s) {
        return s;
    }

    @Specialization(guards = "interop.isPointer(s)", limit = "getInteropLibraryCacheSize()")
    String handlePointerAddress(TruffleObject s,
                    @CachedLibrary("s") InteropLibrary interop) {
        try {
            return readNativeString(interop.asPointer(s));
        } catch (UnsupportedMessageException e) {
            throw RInternalError.shouldNotReachHere(e);
        }
    }

    @Specialization(guards = "!interop.isPointer(s)", limit = "getInteropLibraryCacheSize()")
    String handleNonPointerAddress(TruffleObject s,
                    @CachedLibrary("s") InteropLibrary interop) {
        try {
            interop.toNative(s);
            return readNativeString(interop.asPointer(s));
        } catch (UnsupportedMessageException e) {
            throw RInternalError.shouldNotReachHere(e);
        }
    }
}
