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

import com.oracle.truffle.api.TruffleFile;
import com.oracle.truffle.api.TruffleLanguage;
import java.util.HashSet;
import java.util.Set;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.ffi.impl.llvm.TruffleLLVM_DLL;
import com.oracle.truffle.r.ffi.impl.llvm.TruffleLLVM_DLL.LLVM_Handle;
import com.oracle.truffle.r.ffi.impl.mixed.TruffleMixed_DLLFactory.TruffleMixed_DLCloseNodeGen;
import com.oracle.truffle.r.ffi.impl.mixed.TruffleMixed_DLLFactory.TruffleMixed_DLOpenNodeGen;
import com.oracle.truffle.r.ffi.impl.mixed.TruffleMixed_DLLFactory.TruffleMixed_DLSymNodeGen;
import com.oracle.truffle.r.ffi.impl.nfi.TruffleNFI_DLL;
import com.oracle.truffle.r.ffi.impl.nfi.TruffleNFI_DLL.NFIHandle;
import com.oracle.truffle.r.runtime.context.FastROptions;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.TruffleRLanguage;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.ffi.DLL;
import com.oracle.truffle.r.runtime.ffi.DLL.SymbolHandle;
import com.oracle.truffle.r.runtime.ffi.DLLRFFI;

public class TruffleMixed_DLL implements DLLRFFI {

    private final TruffleLLVM_DLL llvmDllRFFI = new TruffleLLVM_DLL();
    private final TruffleNFI_DLL nfiDllRFFI = new TruffleNFI_DLL();

    private final Set<String> explicitPackages;
    private final boolean isLLVMDefault;

    TruffleMixed_DLL() {
        if ("llvm".equals(System.getenv().get("FASTR_RFFI"))) {
            isLLVMDefault = true;
            explicitPackages = java.util.Collections.emptySet();
        } else {
            String backendOpt = RContext.getInstance().getOption(FastROptions.BackEnd);
            String explicitPkgsOpt;
            if ("native".equals(backendOpt)) {
                isLLVMDefault = false;
                explicitPkgsOpt = RContext.getInstance().getOption(FastROptions.BackEndLLVM);
            } else {
                // llvm
                isLLVMDefault = true;
                explicitPkgsOpt = RContext.getInstance().getOption(FastROptions.BackEndNative);
            }

            String[] explicitPkgsOptSplit = explicitPkgsOpt == null ? null : explicitPkgsOpt.split(",");
            if (explicitPkgsOptSplit == null || explicitPkgsOptSplit.length == 0) {
                explicitPackages = java.util.Collections.emptySet();
            } else {
                explicitPackages = new HashSet<>();
                for (String pkg : explicitPkgsOptSplit) {
                    explicitPackages.add(pkg);
                }
            }
        }
    }

    boolean isLLVMPackage(TruffleFile libPath) {
        if (explicitPackages.isEmpty()) {
            return isLLVMDefault;
        }

        assert libPath != null;
        String libName = libPath.getName();
        libName = libName.substring(0, libName.lastIndexOf('.'));
        boolean isExplicitPkg = explicitPackages.contains(libName);
        return isExplicitPkg ^ isLLVMDefault;
    }

    @Override
    public DLOpenNode createDLOpenNode() {
        return TruffleMixed_DLOpenNodeGen.create(this);
    }

    @Override
    public DLSymNode createDLSymNode() {
        return TruffleMixed_DLSymNodeGen.create(this);
    }

    @Override
    public DLCloseNode createDLCloseNode() {
        return TruffleMixed_DLCloseNodeGen.create(this);
    }

    abstract static class TruffleMixed_DLOpenNode extends Node implements DLOpenNode {

        private final DLOpenNode llvmDllOpenNode;
        private final DLOpenNode nfiDllOpenNode;
        private final TruffleMixed_DLL dllRffi;

        TruffleMixed_DLOpenNode(TruffleMixed_DLL dllRffi) {
            this.dllRffi = dllRffi;
            this.llvmDllOpenNode = dllRffi.llvmDllRFFI.createDLOpenNode();
            this.nfiDllOpenNode = dllRffi.nfiDllRFFI.createDLOpenNode();
        }

        @Specialization
        public LibHandle exec(String path, boolean local, boolean now,
                        @CachedContext(TruffleRLanguage.class) TruffleLanguage.ContextReference<RContext> ctxRef) throws UnsatisfiedLinkError {
            TruffleFile libPath = ctxRef.get().getSafeTruffleFile(path);
            if (!libPath.exists()) {
                throw new UnsatisfiedLinkError(String.format("Shared library %s not found", path));
            }

            boolean useLLVM = dllRffi.isLLVMPackage(libPath);

            LibHandle nfiLibHandle = nfiDllOpenNode.execute(path, local, now);
            if (useLLVM) {
                LLVM_Handle llvmLibHandle = (LLVM_Handle) llvmDllOpenNode.execute(path, local, now);
                return new MixedLLVM_Handle(llvmLibHandle, nfiLibHandle);
            } else {
                return nfiLibHandle;
            }
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
        public final LibHandle nfiLibHandle;

        MixedLLVM_Handle(LLVM_Handle llvmLibHandle, LibHandle nfiLibHandle) {
            super(llvmLibHandle);
            this.nfiLibHandle = nfiLibHandle;
        }
    }

}
