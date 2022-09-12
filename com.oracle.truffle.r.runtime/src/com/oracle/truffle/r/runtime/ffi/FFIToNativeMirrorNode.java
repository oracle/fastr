/*
 * Copyright (c) 2019, 2022, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.data.NativeDataAccess;
import com.oracle.truffle.r.runtime.data.RBaseObject;
import com.oracle.truffle.r.runtime.ffi.interop.NativeArray;

@GenerateUncached
public abstract class FFIToNativeMirrorNode extends Node {

    public abstract Object execute(Object value);

    @Specialization
    protected static Object wrap(RBaseObject value,
                    @Cached("createBinaryProfile()") ConditionProfile hasMirror) {
        NativeDataAccess.NativeMirror mirror = value.getNativeMirror();
        if (hasMirror.profile(mirror != null)) {
            return mirror;
        }
        return NativeDataAccess.createNativeMirror(value);
    }

    @Specialization
    protected static Object wrap(NativeArray value) {
        return value;
    }

    @Specialization
    protected static Object wrap(RObjectDataPtr value) {
        return value;
    }

    @Specialization
    protected static Object wrap(long value) {
        // e.g in the CAR upcall (CARCall.java) for NFISymbol
        return value;
    }

    @Fallback
    protected static Object wrap(Object value) {
        CompilerDirectives.transferToInterpreter();
        throw RInternalError.shouldNotReachHere("Cannot create NativeMirror for " + value.getClass().getSimpleName());
    }

    public static FFIToNativeMirrorNode[] create(int count) {
        FFIToNativeMirrorNode[] result = new FFIToNativeMirrorNode[count];
        for (int i = 0; i < count; i++) {
            result[i] = create();
        }
        return result;
    }

    public static FFIToNativeMirrorNode create() {
        return FFIToNativeMirrorNodeGen.create();
    }

    public static FFIToNativeMirrorNode getUncached() {
        return FFIToNativeMirrorNodeGen.getUncached();
    }
}
