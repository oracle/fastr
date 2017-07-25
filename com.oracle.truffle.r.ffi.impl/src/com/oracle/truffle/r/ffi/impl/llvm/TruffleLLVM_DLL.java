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
package com.oracle.truffle.r.ffi.impl.llvm;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.naming.NameAlreadyBoundException;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.RContext.ContextState;
import com.oracle.truffle.r.runtime.ffi.DLL;
import com.oracle.truffle.r.runtime.ffi.DLL.DLLInfo;
import com.oracle.truffle.r.runtime.ffi.DLL.SymbolHandle;
import com.oracle.truffle.r.runtime.ffi.DLLRFFI;

/**
 * The Truffle version of {@link DLLRFFI}. {@code dlopen} expects to find the LLVM IR in a "library"
 * that is actually a zip file of LLVM bitcode files.
 *
 * There is one major difference between native and LLVM libraries. There is a single global
 * instance of a native library and the symbols are, therefore, accessible from any {@link RContext}
 * instance . However, the (Truffle) function descriptors for an LLVM library are specific to the
 * {@link RContext} they are created (parsed) in. This has two important consequences:
 * <ol>
 * <li>It is theoretically possible to have different versions of libraries in different contexts.
 * </li>
 * <li>The {@code libR} library function descriptors must be made available in every context. At the
 * present time this can only be done by re-parsing the library contents.</li>
 * </ol>
 *
 */
public class TruffleLLVM_DLL implements DLLRFFI {
    static class ContextStateImpl implements RContext.ContextState {
        /**
         * When a new {@link RContext} is created we have to re-parse the libR modules,
         * unfortunately, as there is no way to propagate the LLVM state created in the initial
         * context. TODO when do we really need to do this? This is certainly too early for contexts
         * that will not invoke LLVM code (e.g. most unit tests)
         */
        @Override
        public ContextState initialize(RContext context) {
            if (!context.isInitial()) {
                for (LLVM_IR ir : truffleDLL.libRModules) {
                    parseLLVM("libR", ir);
                }
            }
            if (context.getKind() == RContext.ContextKind.SHARE_PARENT_RW) {
                // must propagate all LLVM library exports
                ArrayList<DLLInfo> loadedDLLs = DLL.getLoadedDLLs();
                for (DLLInfo dllInfo : loadedDLLs) {
                    if (dllInfo.handle instanceof LLVM_Handle) {
                        LLVM_Handle llvmHandle = (LLVM_Handle) dllInfo.handle;
                        for (LLVM_IR ir : llvmHandle.irs) {
                            parseLLVM(llvmHandle.libName, ir);
                        }
                    }
                }
            }
            return this;
        }
    }

    private static TruffleLLVM_DLL truffleDLL;

    TruffleLLVM_DLL() {
        assert truffleDLL == null;
        truffleDLL = this;
    }

    static ContextStateImpl newContextState() {
        return new ContextStateImpl();
    }

    static class LLVM_Handle {
        private final String libName;
        private final LLVM_IR[] irs;

        LLVM_Handle(String libName, LLVM_IR[] irs) {
            this.libName = libName;
            this.irs = irs;
        }
    }

    @FunctionalInterface
    interface ModuleNameMatch {
        boolean match(String name);
    }

    public static LLVM_IR[] getZipLLVMIR(String path) {
        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(path)))) {
            ArrayList<LLVM_IR> irList = new ArrayList<>();
            while (true) {
                ZipEntry entry = zis.getNextEntry();
                if (entry == null) {
                    break;
                }
                int size = (int) entry.getSize();
                byte[] bc = new byte[size];
                int n;
                int totalRead = 0;
                while (totalRead < size && (n = zis.read(bc, totalRead, size - totalRead)) != -1) {
                    totalRead += n;
                }
                Path zipName = Paths.get(entry.getName());
                String name = zipName.getFileName().toString();
                int ix = name.indexOf('.');
                if (ix > 0) {
                    name = name.substring(0, ix);
                }
                LLVM_IR.Binary ir = new LLVM_IR.Binary(name, bc);
                irList.add(ir);
                // debugging
                if (System.getenv("FASTR_LLVM_DEBUG") != null) {
                    try (FileOutputStream bs = new FileOutputStream(Paths.get("tmpzip", name).toString())) {
                        bs.write(bc);
                    }
                    try (PrintStream bs = new PrintStream(new FileOutputStream(Paths.get("tmpb64", name).toString()))) {
                        bs.print(ir.base64);
                    }
                }
            }
            LLVM_IR[] result = new LLVM_IR[irList.size()];
            irList.toArray(result);
            return result;
        } catch (IOException ex) {
            // not a zip file
            return null;
        }
    }

    private static class TruffleLLVM_DLOpenNode extends DLOpenNode {
        @Child private TruffleLLVM_NativeDLL.TruffleLLVM_NativeDLOpen nativeDLLOpenNode;

        /**
         * If a library is enabled for LLVM, the IR for all the modules is retrieved and analyzed.
         * Every exported symbol in the module added to the parseStatus map for the current
         * {@link RContext}. This allows {@code dlsym} to definitively locate any symbol, even if
         * the IR has not been parsed yet.
         */
        @Override
        public Object execute(String path, boolean local, boolean now) {
            try {
                LLVM_IR[] irs = getZipLLVMIR(path);
                if (irs == null) {
                    return tryOpenNative(path, local, now);
                }
                String libName = getLibName(path);
                if (libName.equals("libR")) {
                    // save for new RContexts
                    truffleDLL.libRModules = irs;
                }
                for (LLVM_IR ir : irs) {
                    parseLLVM(libName, ir);
                }
                return new LLVM_Handle(libName, irs);
            } catch (

            Exception ex) {
                throw new UnsatisfiedLinkError(ex.getMessage());
            }
        }

        private long tryOpenNative(String path, boolean local, boolean now) throws UnsatisfiedLinkError {
            if (nativeDLLOpenNode == null) {
                nativeDLLOpenNode = insert(new TruffleLLVM_NativeDLL.TruffleLLVM_NativeDLOpen());
            }
            return nativeDLLOpenNode.execute(path, local, now);
        }
    }

    private static class TruffleLLVM_DLSymNode extends DLSymNode {
        @Override
        public SymbolHandle execute(Object handle, String symbol) throws UnsatisfiedLinkError {
            assert handle instanceof LLVM_Handle;
            Object symValue = RContext.getInstance().getEnv().importSymbol("@" + symbol);
            if (symValue == null) {
                throw new UnsatisfiedLinkError();
            }
            return new SymbolHandle(symValue);
        }
    }

    private static class TruffleLLVM_DLCloseNode extends DLCloseNode {
        @Override
        public int execute(Object handle) {
            assert handle instanceof LLVM_Handle;
            return 0;
        }
    }

    @Override
    public DLOpenNode createDLOpenNode() {
        return new TruffleLLVM_DLOpenNode();
    }

    @Override
    public DLSymNode createDLSymNode() {
        return new TruffleLLVM_DLSymNode();
    }

    @Override
    public DLCloseNode createDLCloseNode() {
        return new TruffleLLVM_DLCloseNode();
    }

    private static String getLibName(String path) {
        String fileName = FileSystems.getDefault().getPath(path).getFileName().toString();
        int ix = fileName.lastIndexOf(".");
        return fileName.substring(0, ix);
    }

    /**
     * Record of the libR modules for subsequent parsing.
     */
    private LLVM_IR[] libRModules;

    private static final String[] PARSE_ERRORS = new String[0];

    private static boolean parseFails(String libName, LLVM_IR ir) {
        for (int i = 0; i < PARSE_ERRORS.length / 2; i++) {
            String plibName = PARSE_ERRORS[i * 2];
            String pModule = PARSE_ERRORS[i * 2 + 1];
            if (libName.equals(plibName) && ir.name.equals(pModule)) {
                return true;
            }
        }
        return false;
    }

    private static void parseLLVM(String libName, LLVM_IR ir) {
        if (ir instanceof LLVM_IR.Binary) {
            LLVM_IR.Binary bir = (LLVM_IR.Binary) ir;
            if (!parseFails(libName, ir)) {
                parseBinary(libName, bir);
            }
        } else {
            throw RInternalError.unimplemented("LLVM text IR");
        }
    }

    private static CallTarget parseBinary(String libName, LLVM_IR.Binary ir) {
        long start = System.nanoTime();
        RContext context = RContext.getInstance();
        long nanos = 1000 * 1000 * 1000;
        Source source = Source.newBuilder(ir.base64).name(ir.name).mimeType("application/x-llvm-ir-bitcode-base64").build();
        CallTarget result = context.getEnv().parse(source);
        if (System.getenv("LLVM_PARSE_TIME") != null) {
            long end = System.nanoTime();
            System.out.printf("parsed %s:%s in %f secs%n", libName, ir.name, ((double) (end - start)) / (double) nanos);
        }
        return result;
    }
}
