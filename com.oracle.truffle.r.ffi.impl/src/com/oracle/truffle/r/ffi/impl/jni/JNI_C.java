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
package com.oracle.truffle.r.ffi.impl.jni;

import static com.oracle.truffle.r.ffi.impl.common.RFFIUtils.traceDownCall;
import static com.oracle.truffle.r.ffi.impl.common.RFFIUtils.traceEnabled;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.ffi.CRFFI;
import com.oracle.truffle.r.runtime.ffi.NativeCallInfo;

public class JNI_C implements CRFFI {
    private static class JNI_InvokeCNode extends Node implements InvokeCNode {
        /**
         * This is rather similar to {@link JNI_Call}, except the objects are guaranteed to be
         * native array types, no upcalls are possible, and no result is returned. However, the
         * receiving function expects actual native arrays (not SEXPs), so these have to be handled
         * on the JNI side.
         */
        @Override
        @TruffleBoundary
        public void execute(NativeCallInfo nativeCallInfo, Object[] args, boolean hasStrings) {
            if (traceEnabled()) {
                traceDownCall(nativeCallInfo.name, args);
            }
            c(nativeCallInfo.address.asAddress(), args, hasStrings);
        }
    }

    private static native void c(long address, Object[] args, boolean hasStrings);

    @Override
    public InvokeCNode createInvokeCNode() {
        return new JNI_InvokeCNode();
    }
}
