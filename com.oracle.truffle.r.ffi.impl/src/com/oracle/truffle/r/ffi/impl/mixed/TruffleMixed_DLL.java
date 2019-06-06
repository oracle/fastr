/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.ffi.impl.mixed;

import java.nio.file.Files;
import java.nio.file.Paths;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.ffi.impl.llvm.TruffleLLVM_DLL;
import com.oracle.truffle.r.ffi.impl.llvm.TruffleLLVM_DLL.LLVM_Handle;
import com.oracle.truffle.r.ffi.impl.mixed.TruffleMixed_DLLFactory.TruffleMixed_DLCloseNodeGen;
import com.oracle.truffle.r.ffi.impl.mixed.TruffleMixed_DLLFactory.TruffleMixed_DLSymNodeGen;
import com.oracle.truffle.r.ffi.impl.nfi.TruffleNFI_DLL;
import com.oracle.truffle.r.ffi.impl.nfi.TruffleNFI_DLL.NFIHandle;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.ffi.DLL;
import com.oracle.truffle.r.runtime.ffi.DLL.SymbolHandle;
import com.oracle.truffle.r.runtime.ffi.DLLRFFI;

public class TruffleMixed_DLL implements DLLRFFI {

    private final TruffleLLVM_DLL llvmDllRFFI = new TruffleLLVM_DLL();
    private final TruffleNFI_DLL nfiDllRFFI = new TruffleNFI_DLL();

    @Override
    public DLOpenNode createDLOpenNode() {
        return new TruffleMixed_DLOpenNode(this);
    }

    @Override
    public DLSymNode createDLSymNode() {
        return TruffleMixed_DLSymNodeGen.create(this);
    }

    @Override
    public DLCloseNode createDLCloseNode() {
        return TruffleMixed_DLCloseNodeGen.create(this);
    }

    private static final class TruffleMixed_DLOpenNode extends Node implements DLOpenNode {

        private final DLOpenNode llvmDllOpenNode;
        private final DLOpenNode nfiDllOpenNode;

        TruffleMixed_DLOpenNode(TruffleMixed_DLL dllRffi) {
            this.llvmDllOpenNode = dllRffi.llvmDllRFFI.createDLOpenNode();
            this.nfiDllOpenNode = dllRffi.nfiDllRFFI.createDLOpenNode();
        }

        @Override
        public LibHandle execute(String path, boolean local, boolean now) throws UnsatisfiedLinkError {
            if (!Files.exists(Paths.get(path))) {
                throw new UnsatisfiedLinkError(String.format("Shared library %s not found", path));
            }
            LibHandle nfiLibHandle = nfiDllOpenNode.execute(path, local, now);
            LLVM_Handle llvmLibHandle = (LLVM_Handle) llvmDllOpenNode.execute(path, local, now);
            return new MixedLLVM_Handle(llvmLibHandle, nfiLibHandle);
        }
    }

    abstract static class TruffleMixed_DLSymNode extends Node implements DLLRFFI.DLSymNode {

        private final TruffleMixed_DLL dllRffi;

        TruffleMixed_DLSymNode(TruffleMixed_DLL dllRffi) {
            this.dllRffi = dllRffi;
        }

        protected DLLRFFI.DLSymNode createLLVMDLSymNode() {
            return dllRffi.llvmDllRFFI.createDLSymNode();
        }

        protected DLLRFFI.DLSymNode createNFIDLSymNode() {
            return dllRffi.nfiDllRFFI.createDLSymNode();
        }

        static LibHandle getNFILibHandle(MixedLLVM_Handle handle) {
            return handle.nfiLibHandle;
        }

        static boolean isInitLibSymbol(String symbol) {
            return symbol.startsWith(DLL.R_INIT_PREFIX);
        }

        @Specialization(guards = {"getNFILibHandle(handle) != null", "isInitLibSymbol(symbol)"})
        protected SymbolHandle handleLLVMInitLibSymbol(MixedLLVM_Handle handle, String symbol,
                        @Cached("createLLVMDLSymNode()") DLLRFFI.DLSymNode llvmDelegNode, @Cached("createNFIDLSymNode()") DLLRFFI.DLSymNode nfiDelegNode) {
            SymbolHandle llvmSym = llvmDelegNode.execute(handle, symbol);
            SymbolHandle nfiSym = nfiDelegNode.execute(handle.nfiLibHandle, symbol);
            return new SymbolHandle(RDataFactory.createList(new Object[]{llvmSym, nfiSym}));
        }

        @Specialization(guards = {"getNFILibHandle(handle) != null", "!isInitLibSymbol(symbol)"})
        protected SymbolHandle handleLLVMSymbol(MixedLLVM_Handle handle, String symbol,
                        @Cached("createLLVMDLSymNode()") DLLRFFI.DLSymNode llvmDelegNode, @Cached("createNFIDLSymNode()") DLLRFFI.DLSymNode nfiDelegNode) {
            try {
                return llvmDelegNode.execute(handle, symbol);
            } catch (UnsatisfiedLinkError e) {
                return nfiDelegNode.execute(handle.nfiLibHandle, symbol);
            }
        }

        @Specialization(guards = "getNFILibHandle(handle) == null")
        protected SymbolHandle handleLLVMSymbol(MixedLLVM_Handle handle, String symbol,
                        @Cached("createLLVMDLSymNode()") DLLRFFI.DLSymNode llvmDelegNode) {
            return llvmDelegNode.execute(handle, symbol);
        }

        @Specialization
        protected SymbolHandle handleNFISymbol(NFIHandle handle, String symbol,
                        @Cached("createNFIDLSymNode()") DLLRFFI.DLSymNode delegNode) {
            return delegNode.execute(handle, symbol);
        }
    }

    abstract static class TruffleMixed_DLCloseNode extends Node implements DLLRFFI.DLCloseNode {
        private final TruffleMixed_DLL dllRffi;

        TruffleMixed_DLCloseNode(TruffleMixed_DLL dllRffi) {
            this.dllRffi = dllRffi;

        }

        protected DLLRFFI.DLCloseNode createLLVMDLCloseNode() {
            return dllRffi.llvmDllRFFI.createDLCloseNode();
        }

        protected DLLRFFI.DLCloseNode createNFIDLCloseNode() {
            return dllRffi.nfiDllRFFI.createDLCloseNode();
        }

        static LibHandle getNFILibHandle(MixedLLVM_Handle handle) {
            return handle.nfiLibHandle;
        }

        @Specialization(guards = "getNFILibHandle(handle) != null")
        protected int handleLLVMClose(MixedLLVM_Handle handle,
                        @Cached("createLLVMDLCloseNode()") DLLRFFI.DLCloseNode llvmDelegNode,
                        @Cached("createNFIDLCloseNode()") DLLRFFI.DLCloseNode nfiDelegNode) {
            nfiDelegNode.execute(handle.nfiLibHandle);
            return llvmDelegNode.execute(handle);
        }

        @Specialization(guards = "getNFILibHandle(handle) == null")
        protected int handleLLVMClose(MixedLLVM_Handle handle,
                        @Cached("createLLVMDLCloseNode()") DLLRFFI.DLCloseNode llvmDelegNode) {
            return llvmDelegNode.execute(handle);
        }

        @Specialization
        protected int handleNFIClose(NFIHandle handle,
                        @Cached("createNFIDLCloseNode()") DLLRFFI.DLCloseNode delegNode) {
            return delegNode.execute(handle);
        }
    }

    public static final class MixedLLVM_Handle extends LLVM_Handle {
        final LibHandle nfiLibHandle;

        MixedLLVM_Handle(LLVM_Handle llvmLibHandle, LibHandle nfiLibHandle) {
            super(llvmLibHandle);
            this.nfiLibHandle = nfiLibHandle;
        }
    }

}
