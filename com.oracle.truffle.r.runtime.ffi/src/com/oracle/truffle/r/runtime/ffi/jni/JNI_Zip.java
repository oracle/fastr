/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.runtime.ffi.jni;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.runtime.ffi.LibPaths;
import com.oracle.truffle.r.runtime.ffi.ZipRFFI;

/**
 * Zip support using JNI.
 */
public class JNI_Zip implements ZipRFFI {

    JNI_Zip() {
        System.load(LibPaths.getBuiltinLibPath("z"));
    }

    @Override
    @TruffleBoundary
    public int compress(byte[] dest, byte[] source) {
        int rc = native_compress(dest, dest.length, source, source.length);
        return rc;
    }

    @Override
    @TruffleBoundary
    public int uncompress(byte[] dest, byte[] source) {
        int rc = native_uncompress(dest, dest.length, source, source.length);
        return rc;
    }

    // Checkstyle: stop method name

    private static native int native_compress(byte[] dest, long destlen, byte[] source, long sourcelen);

    private static native int native_uncompress(byte[] dest, long destlen, byte[] source, long sourcelen);
}
