/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.ffi.impl.llvm;

import static com.oracle.truffle.r.runtime.ffi.NativeFunction.anyLibrary;
import static com.oracle.truffle.r.runtime.ffi.NativeFunction.baseLibrary;

import java.nio.charset.StandardCharsets;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.r.ffi.impl.llvm.TruffleLLVM_DownCallNodeFactoryFactory.LLVMDownCallNodeGen;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.ffi.DLL;
import com.oracle.truffle.r.runtime.ffi.DLL.DLLInfo;
import com.oracle.truffle.r.runtime.ffi.DLL.SymbolHandle;
import com.oracle.truffle.r.runtime.ffi.DownCallNodeFactory;
import com.oracle.truffle.r.runtime.ffi.NativeFunction;
import com.oracle.truffle.r.runtime.ffi.interop.NativeArray;
import com.oracle.truffle.r.runtime.ffi.interop.NativeCharArray;
import com.oracle.truffle.r.runtime.ffi.interop.NativeDoubleArray;
import com.oracle.truffle.r.runtime.ffi.interop.NativeIntegerArray;
import com.oracle.truffle.r.runtime.ffi.interop.NativePointer;

public final class TruffleLLVM_DownCallNodeFactory extends DownCallNodeFactory {

    public static final TruffleLLVM_DownCallNodeFactory INSTANCE = new TruffleLLVM_DownCallNodeFactory();

    private TruffleLLVM_DownCallNodeFactory() {
    }

    @Override
    public DownCallNode createDownCallNode() {
        return LLVMDownCallNodeGen.create();
    }

    @GenerateUncached
    protected abstract static class LLVMDownCallNode extends DownCallNode {
        public LLVMDownCallNode() {
            super();
        }

        @Specialization
        protected Object call(NativeFunction f, Object[] args,
                        @Cached(value = "createTarget(f)", allowUncached = true) TruffleObject target,
                        @CachedLibrary("target") InteropLibrary interop) {
            return callInternal(f, args, target, interop);
        }

        @Override
        protected TruffleObject createTarget(NativeFunction fn) {
            if (fn == NativeFunction.initEventLoop) {
                return new InitEventLoop();
            }

            CompilerAsserts.neverPartOfCompilation();
            String library = fn.getLibrary();
            DLLInfo dllInfo = null;
            if (Utils.identityEquals(library, baseLibrary())) {
                dllInfo = DLL.getRdllInfo();
            } else if (!Utils.identityEquals(library, anyLibrary())) {
                dllInfo = DLL.findLibrary(library);
            }
            SymbolHandle result = DLL.findSymbol(fn.getCallName(), dllInfo);
            if (result == DLL.SYMBOL_NOT_FOUND) {
                throw RInternalError.shouldNotReachHere("Could not find function " + fn.getCallName() + " in library " + library);
            }
            return result.asTruffleObject();
        }

        @Override
        @ExplodeLoop
        protected Object beforeCall(NativeFunction nativeFunction, TruffleObject fn, Object[] args) {
            assert !(fn instanceof RFunction);

            for (int i = 0; i < args.length; i++) {
                Object obj = args[i];
                if (obj instanceof double[]) {
                    args[i] = new NativeDoubleArray((double[]) obj);
                } else if (obj instanceof int[]) {
                    args[i] = new NativeIntegerArray((int[]) obj);
                } else if (obj == null) {
                    args[i] = NativePointer.NULL_NATIVEPOINTER;
                } else if (obj instanceof String) {
                    args[i] = new NativeCharArray(getStringBytes((String) obj));
                }
            }
            return 0;
        }

        @TruffleBoundary
        private static byte[] getStringBytes(String obj) {
            return obj.getBytes(StandardCharsets.UTF_8);
        }

        @Override
        @ExplodeLoop
        protected void afterCall(Object before, NativeFunction fn, TruffleObject target, Object[] args) {
            assert !(target instanceof RFunction);

            for (int i = 0; i < args.length; i++) {
                Object obj = args[i];
                if (obj instanceof NativeArray<?>) {
                    ((NativeArray<?>) obj).refresh();
                }
            }
        }
    }

    @ExportLibrary(InteropLibrary.class)
    static final class InitEventLoop implements TruffleObject {

        @SuppressWarnings("static-method")
        @ExportMessage
        boolean isExecutable() {
            return true;
        }

        @SuppressWarnings("static-method")
        @ExportMessage
        Object execute(@SuppressWarnings("unused") Object... args) {
            // TODO:
            // by returning -1 we indicate that the native handlers loop is not available
            return -1;
        }
    }

}
