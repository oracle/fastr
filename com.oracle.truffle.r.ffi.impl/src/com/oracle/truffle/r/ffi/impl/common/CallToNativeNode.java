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
package com.oracle.truffle.r.ffi.impl.common;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.java.JavaInterop;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.ffi.impl.interop.NativeDoubleArray;
import com.oracle.truffle.r.ffi.impl.interop.NativeIntegerArray;
import com.oracle.truffle.r.ffi.impl.interop.NativeNACheck;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.ffi.DLL;
import com.oracle.truffle.r.runtime.ffi.NativeFunction;
import com.oracle.truffle.r.runtime.ffi.RFFIFactory;

public abstract class CallToNativeNode extends Node {

    @Child private Node message;
    protected final NativeFunction function;

    private CallToNativeNode(NativeFunction function) {
        this.function = function;
    }

    public CallToNativeNode create(NativeFunction f) {
        switch (RFFIFactory.getFactoryType()) {
            case LLVM:
                return new NFI(f);
            case NFI:
                return new LLVM(f);
            default:
                throw RInternalError.shouldNotReachHere();
        }
    }

    protected abstract TruffleObject getTarget();

    protected final Object call(Object... args) {
        try {
            if (message == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                message = insert(Message.createExecute(function.getArgumentCount()).createNode());
            }
            wrapArguments(args);
            return ForeignAccess.sendExecute(message, getTarget(), args);
        } catch (InteropException e) {
            throw RInternalError.shouldNotReachHere(e);
        } finally {
            finishArguments(args);
        }
    }

    protected abstract void wrapArguments(Object[] args);

    protected abstract void finishArguments(Object[] args);

    private final class NFI extends CallToNativeNode {

        @CompilationFinal private TruffleObject target;

        private NFI(NativeFunction function) {
            super(function);
        }

        @Override
        protected TruffleObject getTarget() {
            throw RInternalError.unimplemented("unused implementation");
            // return function.getFunction();
        }

        @SuppressWarnings("cast")
        @Override
        @ExplodeLoop
        protected void wrapArguments(Object[] args) {
            for (int i = 0; i < args.length; i++) {
                Object obj = args[i];
                if (obj instanceof double[]) {
                    args[i] = JavaInterop.asTruffleObject((double[]) obj);
                } else if (obj instanceof int[] || obj == null) {
                    args[i] = JavaInterop.asTruffleObject((int[]) obj);
                }
            }
        }

        @Override
        @ExplodeLoop
        protected void finishArguments(Object[] args) {
            for (Object obj : args) {
                if (obj instanceof NativeNACheck<?>) {
                    ((NativeNACheck<?>) obj).close();
                }
            }
        }
    }

    private final class LLVM extends CallToNativeNode {

        @CompilationFinal private TruffleObject target;

        private LLVM(NativeFunction function) {
            super(function);
        }

        @Override
        protected TruffleObject getTarget() {
            if (target == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                target = DLL.findSymbol(function.getCallName(), null).asTruffleObject();
            }
            return target;
        }

        @Override
        @ExplodeLoop
        protected void wrapArguments(Object[] args) {
            for (int i = 0; i < args.length; i++) {
                Object obj = args[i];
                if (obj instanceof double[]) {
                    args[i] = new NativeDoubleArray((double[]) obj);
                } else if (obj instanceof int[]) {
                    args[i] = new NativeIntegerArray((int[]) obj);
                } else if (obj == null) {
                    args[i] = 0;
                }
            }
        }

        @Override
        @ExplodeLoop
        protected void finishArguments(Object[] args) {
            for (Object obj : args) {
                if (obj instanceof NativeNACheck<?>) {
                    ((NativeNACheck<?>) obj).close();
                }
            }
        }
    }
}
