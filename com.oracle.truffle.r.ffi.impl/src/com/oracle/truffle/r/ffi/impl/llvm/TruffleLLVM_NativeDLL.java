/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.ffi.impl.llvm;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.CanResolve;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.MessageResolution;
import com.oracle.truffle.api.interop.Resolve;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.ffi.impl.interop.NativeCharArray;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.data.RTruffleObject;
import com.oracle.truffle.r.runtime.ffi.DLL;
import com.oracle.truffle.r.runtime.ffi.DLL.SymbolHandle;
import com.oracle.truffle.r.runtime.ffi.RFFIRootNode;

/**
 * Direct access to native {@code dlopen} for libraries for which no LLVM code is available.
 */
class TruffleLLVM_NativeDLL {
    enum Function {
        dlopen(3),
        dlclose(1);

        private final Node executeNode;
        private final String callName;

        Function(int argCount) {
            executeNode = Message.createExecute(argCount).createNode();
            callName = "call_" + name();
        }
    }

    public interface ErrorCallback {
        void setResult(String errorMessage);

    }

    private static class ErrorCallbackImpl implements ErrorCallback, RTruffleObject {
        private String errorMessage;

        @Override
        public void setResult(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        @Override
        public ForeignAccess getForeignAccess() {
            return ErrorCallbackCallbackMRForeign.ACCESS;
        }
    }

    @MessageResolution(receiverType = ErrorCallback.class)
    public static class ErrorCallbackCallbackMR {
        @CanResolve
        public abstract static class ErrorCallbackCheckNode extends Node {

            protected static boolean test(TruffleObject receiver) {
                return receiver instanceof ErrorCallback;
            }
        }

        @Resolve(message = "IS_EXECUTABLE")
        public abstract static class ErrorCallbackIsExecutableNode extends Node {
            protected Object access(@SuppressWarnings("unused") ErrorCallback receiver) {
                return true;
            }
        }

        @Resolve(message = "EXECUTE")
        public abstract static class ErrorCallbackExecuteNode extends Node {
            protected Object access(@SuppressWarnings("unused") VirtualFrame frame, ErrorCallback receiver, Object[] arguments) {
                receiver.setResult((String) arguments[0]);
                return receiver;
            }
        }
    }

    static class TruffleLLVM_NativeDLOpen extends Node {
        @CompilationFinal private SymbolHandle symbolHandle;

        public long execute(String path, boolean local, boolean now) throws UnsatisfiedLinkError {
            try {
                if (symbolHandle == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    symbolHandle = DLL.findSymbol(Function.dlopen.callName, null);
                }
                ErrorCallbackImpl errorCallbackImpl = new ErrorCallbackImpl();
                long result = (long) ForeignAccess.sendExecute(Function.dlopen.executeNode, symbolHandle.asTruffleObject(), errorCallbackImpl, new NativeCharArray(path.getBytes()), local ? 1 : 0,
                                now ? 1 : 0);
                if (result == 0) {
                    throw new UnsatisfiedLinkError(errorCallbackImpl.errorMessage);
                }
                return result;
            } catch (InteropException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }
    }

    static class TruffleLLVM_NativeDLClose extends Node {
        @CompilationFinal private SymbolHandle symbolHandle;

        public int execute(long handle) {
            try {
                if (symbolHandle == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    symbolHandle = DLL.findSymbol(Function.dlclose.callName, null);
                }
                int result = (int) ForeignAccess.sendExecute(Function.dlclose.executeNode, symbolHandle.asTruffleObject(), handle);
                return result;
            } catch (InteropException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }
    }

    public static final class NativeDLOpenRootNode extends RFFIRootNode<TruffleLLVM_NativeDLOpen> {
        private static NativeDLOpenRootNode nativeDLOpenRootNode;

        private NativeDLOpenRootNode() {
            super(new TruffleLLVM_NativeDLOpen());
        }

        @Override
        public Object execute(VirtualFrame frame) {
            Object[] args = frame.getArguments();
            return rffiNode.execute((String) args[0], (boolean) args[1], (boolean) args[2]);
        }

        public static NativeDLOpenRootNode create() {
            if (nativeDLOpenRootNode == null) {
                nativeDLOpenRootNode = new NativeDLOpenRootNode();
            }
            return nativeDLOpenRootNode;
        }
    }
}
