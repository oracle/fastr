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
import static com.oracle.truffle.r.ffi.impl.common.RFFIUtils.traceDownCallReturn;
import static com.oracle.truffle.r.ffi.impl.common.RFFIUtils.traceEnabled;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.ffi.impl.common.RFFIUtils;
import com.oracle.truffle.r.ffi.impl.upcalls.UpCallsRFFI;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.ffi.CallRFFI;
import com.oracle.truffle.r.runtime.ffi.NativeCallInfo;
import com.oracle.truffle.r.runtime.ffi.RFFIVariables;

/**
 * The only variety in the signatures for {@code .Call} is the number of arguments. GnuR supports a
 * maximum number of args (64). This implementation passes up to 9 arguments explicitly; beyond 9
 * they are passed as an array and the JNI code has to call back to get the args (not very
 * efficient).
 *
 * The JNI layer is not (currently) MT safe, so all calls are single threaded. N.B. Since the calls
 * take place from nodes, and these may be duplicated in separate contexts, we must synchronize on
 * the class.
 */
public class JNI_Call implements CallRFFI {

    private static class JNI_InvokeCallNode extends Node implements InvokeCallNode {

        @Override
        @TruffleBoundary
        public Object execute(NativeCallInfo nativeCallInfo, Object[] args) {
            long address = nativeCallInfo.address.asAddress();
            Object result = null;
            if (traceEnabled()) {
                traceDownCall(nativeCallInfo.name, args);
            }
            try {
                switch (args.length) {
                    case 0:
                        result = call0(address);
                        break;
                    case 1:
                        result = call1(address, args[0]);
                        break;
                    case 2:
                        result = call2(address, args[0], args[1]);
                        break;
                    case 3:
                        result = call3(address, args[0], args[1], args[2]);
                        break;
                    case 4:
                        result = call4(address, args[0], args[1], args[2], args[3]);
                        break;
                    case 5:
                        result = call5(address, args[0], args[1], args[2], args[3], args[4]);
                        break;
                    case 6:
                        result = call6(address, args[0], args[1], args[2], args[3], args[4], args[5]);
                        break;
                    case 7:
                        result = call7(address, args[0], args[1], args[2], args[3], args[4], args[5], args[6]);
                        break;
                    case 8:
                        result = call8(address, args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7]);
                        break;
                    case 9:
                        result = call9(address, args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8]);
                        break;
                    default:
                        result = call(address, args);
                        break;
                }
                return result;
            } finally {
                if (traceEnabled()) {
                    traceDownCallReturn(nativeCallInfo.name, result);
                }
            }
        }
    }

    private static class JNI_InvokeVoidCallNode extends Node implements InvokeVoidCallNode {

        @Override
        @TruffleBoundary
        public void execute(NativeCallInfo nativeCallInfo, Object[] args) {
            synchronized (JNI_Call.class) {
                if (traceEnabled()) {
                    traceDownCall(nativeCallInfo.name, args);
                }
                long address = nativeCallInfo.address.asAddress();
                try {
                    switch (args.length) {
                        case 0:
                            callVoid0(address);
                            break;
                        case 1:
                            callVoid1(address, args[0]);
                            break;
                        default:
                            throw RInternalError.shouldNotReachHere();
                    }
                } finally {
                    if (traceEnabled()) {
                        traceDownCallReturn(nativeCallInfo.name, null);
                    }
                }
            }
        }
    }

    public JNI_Call() {
        initialize();
    }

    @TruffleBoundary
    private static void initialize() {
        UpCallsRFFI upCallsRFFIImpl = RFFIUtils.initialize(new JNIUpCallsRFFIImpl());

        if (traceEnabled()) {
            traceDownCall("initialize");
        }
        try {
            initialize(upCallsRFFIImpl, RFFIVariables.initialize());
        } finally {
            if (traceEnabled()) {
                traceDownCallReturn("initialize", null);
            }
        }
    }

    private static native void initialize(UpCallsRFFI upCallRFFI, RFFIVariables[] variables);

    private static native void nativeSetTempDir(String tempDir);

    private static native void nativeSetInteractive(boolean interactive);

    private static native Object call(long address, Object[] args);

    private static native Object call0(long address);

    private static native Object call1(long address, Object arg1);

    private static native Object call2(long address, Object arg1, Object arg2);

    private static native Object call3(long address, Object arg1, Object arg2, Object arg3);

    private static native Object call4(long address, Object arg1, Object arg2, Object arg3, Object arg4);

    private static native Object call5(long address, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5);

    private static native Object call6(long address, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6);

    private static native Object call7(long address, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7);

    private static native Object call8(long address, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8);

    private static native Object call9(long address, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8, Object arg9);

    private static native void callVoid0(long address);

    private static native void callVoid1(long address, Object arg1);

    @Override
    public InvokeCallNode createInvokeCallNode() {
        return new JNI_InvokeCallNode();
    }

    @Override
    public InvokeVoidCallNode createInvokeVoidCallNode() {
        return new JNI_InvokeVoidCallNode();
    }
}
