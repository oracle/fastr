/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.ffi.impl.nfi.TruffleNFI_Context;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.ffi.AfterDownCallProfiles;
import com.oracle.truffle.r.runtime.ffi.NativeFunction;
import com.oracle.truffle.r.runtime.ffi.RFFIContext;
import com.oracle.truffle.r.runtime.ffi.RFFIFactory;
import com.oracle.truffle.r.runtime.ffi.UserRngRFFI;

public class TruffleLLVM_UserRng implements UserRngRFFI {

    private abstract static class RNGNode extends Node {

        @Child protected InteropLibrary userFunctionInterop;
        @Child protected InteropLibrary readPointerInterop;
        @CompilationFinal protected TruffleObject userFunctionTarget;
        @CompilationFinal protected TruffleObject readPointerTarget;
        @CompilationFinal protected AfterDownCallProfiles afterDownCallProfiles;

        protected RNGNode(NativeFunction userFunction, NativeFunction readFunction) {
            userFunctionTarget = TruffleNFI_Context.getInstance().lookupNativeFunction(userFunction);
            userFunctionInterop = InteropLibrary.getFactory().create(userFunctionTarget);
            if (readFunction != null) {
                readPointerTarget = TruffleNFI_Context.getInstance().lookupNativeFunction(readFunction);
                readPointerInterop = InteropLibrary.getFactory().create(readPointerTarget);
            }
            afterDownCallProfiles = AfterDownCallProfiles.create();
        }
    }

    private static final class TruffleLLVMInitNode extends RNGNode implements InitNode {

        TruffleLLVMInitNode() {
            super(NativeFunction.unif_init, null);
        }

        @Override
        public void execute(VirtualFrame frame, int seed) {
            RFFIContext stateRFFI = RContext.getInstance().getStateRFFI();
            Object before = stateRFFI.beforeDowncall(frame.materialize(), RFFIFactory.Type.LLVM);
            try {
                userFunctionInterop.execute(userFunctionTarget, seed);
            } catch (InteropException ex) {
                throw RInternalError.shouldNotReachHere(ex);
            } finally {
                stateRFFI.afterDowncall(before, RFFIFactory.Type.LLVM, afterDownCallProfiles);
            }
        }
    }

    private static final class TruffleLLVM_RandNode extends RNGNode implements RandNode {

        TruffleLLVM_RandNode() {
            super(NativeFunction.unif_rand, NativeFunction.read_pointer_double);
        }

        @Override
        public double execute(VirtualFrame frame) {
            RFFIContext stateRFFI = RContext.getInstance().getStateRFFI();
            Object before = stateRFFI.beforeDowncall(frame.materialize(), RFFIFactory.Type.LLVM);
            try {
                Object address = userFunctionInterop.execute(userFunctionTarget);
                return (double) readPointerInterop.execute(readPointerTarget, address);
            } catch (InteropException ex) {
                throw RInternalError.shouldNotReachHere(ex);
            } finally {
                stateRFFI.afterDowncall(before, RFFIFactory.Type.LLVM, afterDownCallProfiles);
            }
        }
    }

    private static final class TruffleLLVM_NSeedNode extends RNGNode implements NSeedNode {

        TruffleLLVM_NSeedNode() {
            super(NativeFunction.unif_nseed, NativeFunction.read_pointer_int);
        }

        @Override
        public int execute(VirtualFrame frame) {
            RFFIContext stateRFFI = RContext.getInstance().getStateRFFI();
            Object before = stateRFFI.beforeDowncall(frame.materialize(), RFFIFactory.Type.LLVM);
            try {
                Object address = userFunctionInterop.execute(userFunctionTarget);
                return (int) readPointerInterop.execute(readPointerTarget, address);
            } catch (InteropException ex) {
                throw RInternalError.shouldNotReachHere(ex);
            } finally {
                stateRFFI.afterDowncall(before, RFFIFactory.Type.LLVM, afterDownCallProfiles);
            }
        }
    }

    private static final class TruffleLLVM_SeedsNode extends RNGNode implements SeedsNode {

        TruffleLLVM_SeedsNode() {
            super(NativeFunction.unif_seedloc, NativeFunction.read_array_int);
        }

        @Override
        public void execute(VirtualFrame frame, int[] n) {
            RFFIContext stateRFFI = RContext.getInstance().getStateRFFI();
            Object before = stateRFFI.beforeDowncall(frame.materialize(), RFFIFactory.Type.LLVM);
            try {
                Object address = userFunctionInterop.execute(userFunctionTarget);
                for (int i = 0; i < n.length; i++) {
                    n[i] = (int) readPointerInterop.execute(readPointerTarget, address, i);
                }
            } catch (InteropException ex) {
                throw RInternalError.shouldNotReachHere(ex);
            } finally {
                stateRFFI.afterDowncall(before, RFFIFactory.Type.LLVM, afterDownCallProfiles);
            }
        }
    }

    @Override
    public InitNode createInitNode() {
        return new TruffleLLVMInitNode();
    }

    @Override
    public RandNode createRandNode() {
        return new TruffleLLVM_RandNode();
    }

    @Override
    public NSeedNode createNSeedNode() {
        return new TruffleLLVM_NSeedNode();
    }

    @Override
    public SeedsNode createSeedsNode() {
        return new TruffleLLVM_SeedsNode();
    }
}
