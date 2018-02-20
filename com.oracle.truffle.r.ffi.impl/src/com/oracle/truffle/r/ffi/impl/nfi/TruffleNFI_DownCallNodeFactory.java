/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.ffi.impl.nfi;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.java.JavaInterop;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.r.ffi.impl.interop.NativeNACheck;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.ffi.DownCallNodeFactory;
import com.oracle.truffle.r.runtime.ffi.NativeFunction;
import com.oracle.truffle.r.runtime.ffi.interop.NativeUInt8Array;

public final class TruffleNFI_DownCallNodeFactory extends DownCallNodeFactory {
    public static final TruffleNFI_DownCallNodeFactory INSTANCE = new TruffleNFI_DownCallNodeFactory();

    private TruffleNFI_DownCallNodeFactory() {
    }

    @Override
    public DownCallNode createDownCallNode(NativeFunction function) {
        return new DownCallNode(function) {
            @Override
            protected TruffleObject getTarget(NativeFunction function) {
                // TODO: this lookupNativeFunction function can exist in all FFI Contexts
                return TruffleNFI_Context.getInstance().lookupNativeFunction(function);
            }

            @SuppressWarnings("cast")
            @Override
            @ExplodeLoop
            protected long beforeCall(NativeFunction function, TruffleObject target, Object[] args) {
                for (int i = 0; i < args.length; i++) {
                    Object obj = args[i];
                    if (obj instanceof double[]) {
                        args[i] = JavaInterop.asTruffleObject(obj);
                    } else if (obj instanceof int[] || obj == null) {
                        args[i] = JavaInterop.asTruffleObject(obj);
                    } else if (obj instanceof NativeUInt8Array) {
                        // accounts for NativeCharArray and NativeRawArray
                        // the assumption is that getValue() gives us the actual backing array and
                        // NFI will transfer any changes back to this array
                        NativeUInt8Array nativeArr = (NativeUInt8Array) obj;
                        byte[] data;
                        if (nativeArr.fakesNullTermination()) {
                            data = getNullTerminatedBytes(nativeArr.getValue());
                            nativeArr.setValue(data, false);
                        } else {
                            data = nativeArr.getValue();
                        }
                        args[i] = JavaInterop.asTruffleObject(data);
                    }
                }

                if (function.hasComplexInteraction()) {
                    return ((TruffleNFI_Context) RContext.getInstance().getRFFI()).beforeDowncall();
                }
                return 0;
            }

            @TruffleBoundary
            private byte[] getNullTerminatedBytes(byte[] data) {
                byte[] newData = new byte[data.length + 1];
                System.arraycopy(data, 0, newData, 0, data.length);
                newData[data.length] = 0;
                return newData;
            }

            @Override
            @ExplodeLoop
            protected void afterCall(long before, NativeFunction function, TruffleObject target, Object[] args) {
                if (function.hasComplexInteraction()) {
                    ((TruffleNFI_Context) RContext.getInstance().getRFFI()).afterDowncall(before);
                }

                for (Object obj : args) {
                    // TODO: can this ever happen in NFI?
                    if (obj instanceof NativeNACheck<?>) {
                        ((NativeNACheck<?>) obj).close();
                    }
                }
            }
        };
    }
}
