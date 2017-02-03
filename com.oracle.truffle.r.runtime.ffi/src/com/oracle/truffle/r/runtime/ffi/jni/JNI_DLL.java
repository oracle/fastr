/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.r.runtime.ffi.DLL.SymbolHandle;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.runtime.ffi.DLLRFFI;

public class JNI_DLL implements DLLRFFI {

    public static class JNI_DLOpenNode extends DLOpenNode {
        @Override
        @TruffleBoundary
        public Object execute(String path, boolean local, boolean now) throws UnsatisfiedLinkError {
            long handle = native_dlopen(path, local, now);
            return new Long(handle);
        }
    }

    public static class JNI_DLSymNode extends DLSymNode {
        @Override
        @TruffleBoundary
        public SymbolHandle execute(Object handle, String symbol) throws UnsatisfiedLinkError {
            long nativeHandle = (Long) handle;
            long symv = native_dlsym(nativeHandle, symbol);
            return new SymbolHandle(symv);
        }
    }

    public static class JNI_DLCloseNode extends DLCloseNode {
        @Override
        @TruffleBoundary
        public int execute(Object handle) {
            long nativeHandle = (Long) handle;
            return native_dlclose(nativeHandle);
        }

    }

    // Checkstyle: stop method name check

    private static native long native_dlopen(String path, boolean local, boolean now);

    private static native int native_dlclose(long handle);

    private static native String native_dlerror();

    private static native long native_dlsym(long handle, String symbol);

    @Override
    public DLOpenNode createDLOpenNode() {
        return new JNI_DLOpenNode();
    }

    @Override
    public DLSymNode createDLSymNode() {
        return new JNI_DLSymNode();
    }

    @Override
    public DLCloseNode createDLCloseNode() {
        return new JNI_DLCloseNode();
    }

}
