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
package com.oracle.truffle.r.runtime.ffi.util;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.RInternalError;

/**
 * Encapsulates the operation of reading a double from a source object, which should represent a
 * double pointer. This node tries to read the data using array messages of the interop protocol and
 * if that fails it uses the {@code toNative} and {@code asPointer} messages to get the raw pointer
 * and reads from it using unsafe access.
 */
@GenerateUncached
@ImportStatic(DSLConfig.class)
public abstract class ReadDoublePointerNode extends Node {
    public final double read(Object source) {
        return execute(source, 0);
    }

    public abstract double execute(Object source, int index);

    @Specialization(guards = "targetLib.isArrayElementReadable(target, index)", limit = "getInteropLibraryCacheSize()")
    protected static double getFromArray(Object target, int index,
                    @Cached AsDoubleNode asDoubleNode,
                    @CachedLibrary("target") InteropLibrary targetLib) {
        try {
            return asDoubleNode.execute(targetLib.readArrayElement(target, index));
        } catch (UnsupportedMessageException | InvalidArrayIndexException e) {
            throw RInternalError.shouldNotReachHere();
        }
    }

    @Specialization(guards = {"!targetLib.isArrayElementReadable(target, index)"}, limit = "getInteropLibraryCacheSize()")
    protected static double getFromNative(Object target, int index,
                    @CachedLibrary("target") InteropLibrary targetLib) {
        try {
            targetLib.toNative(target);
            long ptr = targetLib.asPointer(target);
            return NativeMemory.getDouble(ptr, index);
        } catch (UnsupportedMessageException e) {
            throw RInternalError.shouldNotReachHere();
        }
    }

    @GenerateUncached
    @ImportStatic(DSLConfig.class)
    public abstract static class AsDoubleNode extends Node {
        public abstract double execute(Object value);

        @Specialization
        static double doDouble(double i) {
            return i;
        }

        @Specialization(replaces = "doDouble", limit = "getInteropLibraryCacheSize()")
        static double doOthers(Object value,
                        @CachedLibrary("value") InteropLibrary lib) {
            try {
                return lib.asDouble(value);
            } catch (UnsupportedMessageException e) {
                throw RInternalError.shouldNotReachHere();
            }
        }
    }
}
