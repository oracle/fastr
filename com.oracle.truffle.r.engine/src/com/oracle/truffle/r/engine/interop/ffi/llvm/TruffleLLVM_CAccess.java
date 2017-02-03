/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.engine.interop.ffi.llvm;

import com.oracle.truffle.r.runtime.ffi.DLL;
import com.oracle.truffle.r.runtime.ffi.DLLRFFI;
import com.oracle.truffle.r.runtime.ffi.jni.JNI_DLL;
import com.oracle.truffle.r.runtime.rng.user.UserRNG;

/**
 * Access to some primitive C operations. This is required by the {@link UserRNG} API which works
 * with {@code double *}.
 *
 * N.B. When {@code libR} is not completely in LLVM mode (as now), we have to look up the symbols
 * using an explicitly created {@link TruffleLLVM_DLL.LLVM_Handle} and not go via generic lookup in
 * {@link DLL} as that would use a {@link JNI_DLL} handle.
 */
public class TruffleLLVM_CAccess {
    private static final TruffleLLVM_DLL.LLVM_Handle handle = new TruffleLLVM_DLL.LLVM_Handle("libR", null);

    public enum Function {
        READ_POINTER_INT,
        READ_ARRAY_INT,
        READ_POINTER_DOUBLE,
        READ_ARRAY_DOUBLE;

        private DLL.SymbolHandle symbolHandle;

        public DLL.SymbolHandle getSymbolHandle() {
            if (symbolHandle == null) {
                // This would be the generic path
                // symbolHandle = DLL.findSymbol(cName(), null);
                symbolHandle = (DLL.SymbolHandle) DLLRFFI.DLSymRootNode.create().getCallTarget().call(handle, cName());
                assert symbolHandle != null;
            }
            return symbolHandle;
        }

        public String cName() {
            return "caccess_" + name().toLowerCase();
        }
    }
}
