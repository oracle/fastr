/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.r.runtime.ffi.DLLRFFI;

public class JNI_DLL implements DLLRFFI {

    @Override
    public Object dlopen(String path, boolean local, boolean now) {
        long handle = native_dlopen(path, local, now);
        if (handle == 0) {
            return null;
        } else {
            return new Long(handle);
        }
    }

    @Override
    public SymbolHandle dlsym(Object handle, String symbol) {
        long nativeHandle = (Long) handle;
        long symv = native_dlsym(nativeHandle, symbol);
        if (symv == 0) {
            // symbol might actually be zero
            if (dlerror() != null) {
                return null;
            }
        }
        return new SymbolHandle(symv);
    }

    @Override
    public int dlclose(Object handle) {
        long nativeHandle = (Long) handle;
        return native_dlclose(nativeHandle);
    }

    @Override
    public String dlerror() {
        return native_dlerror();
    }

    // Checkstyle: stop method name check

    private static native long native_dlopen(String path, boolean local, boolean now);

    private static native int native_dlclose(long handle);

    private static native String native_dlerror();

    private static native long native_dlsym(long handle, String symbol);

}
