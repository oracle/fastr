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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleFile;
import com.oracle.truffle.api.TruffleLogger;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.r.runtime.context.FastROptions;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RLogger;
import com.oracle.truffle.r.runtime.RPlatform;
import com.oracle.truffle.r.runtime.RPlatform.OSInfo;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.RContext.ContextState;
import com.oracle.truffle.r.runtime.ffi.DLL;
import com.oracle.truffle.r.runtime.ffi.DLL.DLLInfo;
import com.oracle.truffle.r.runtime.ffi.DLL.SymbolHandle;
import com.oracle.truffle.r.runtime.ffi.DLLRFFI;
import com.oracle.truffle.r.runtime.ffi.RFFIContext;
import com.oracle.truffle.r.runtime.ffi.RFFIFactory;
import com.oracle.truffle.r.runtime.ffi.RFFIFactory.Type;
import java.util.logging.Level;

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
    /*
     * The LIBS file, which is included in the LLVM archive, enumerates native dynamic libraries to
     * be linked with the LLVM library in the archive.
     */
    private static final String LIBS = "LIBS";

    private static final Set<String> ignoredNativeLibs = new HashSet<>();
    static {
        ignoredNativeLibs.add("Rblas");
        ignoredNativeLibs.add("Rlapack");
    }

    static class ContextStateImpl implements RContext.ContextState {
        /**
         * When a new {@link RContext} is created we have to re-parse the libR modules,
         * unfortunately, as there is no way to propagate the LLVM state created in the initial
         * context. TODO when do we really need to do this? This is certainly too early for contexts
         * that will not invoke LLVM code (e.g. most unit tests)
         */
        @Override
        public ContextState initialize(RContext context) {
            // TODO: Is it really needed when using the new lookup mechanism?
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
                        for (ParsedLLVM_IR parsedIR : llvmHandle.parsedIRs) {
                            parseLLVM(llvmHandle.libName, parsedIR.ir);
                        }
                    }
                }
            }
            return this;
        }
    }

    private static TruffleLLVM_DLL truffleDLL;

    public TruffleLLVM_DLL() {
        if (truffleDLL != null) {
            libRModules = truffleDLL.libRModules;
        }
        truffleDLL = this;
    }

    static ContextStateImpl newContextState() {
        return new ContextStateImpl();
    }

    public static class ParsedLLVM_IR {
        final LLVM_IR ir;
        final Object lookupObject;

        ParsedLLVM_IR(LLVM_IR ir, Object lookupObject) {
            this.ir = ir;
            this.lookupObject = lookupObject;
        }

        Object lookup(String symbol) throws UnknownIdentifierException {
            try {
                return ForeignAccess.sendRead(Message.READ.createNode(), (TruffleObject) lookupObject, symbol);
            } catch (UnsupportedMessageException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }

    }

    public static class LLVM_Handle implements LibHandle {
        final String libName;
        final ParsedLLVM_IR[] parsedIRs;

        public LLVM_Handle(LLVM_Handle libHandle) {
            this(libHandle.libName, libHandle.parsedIRs);
        }

        public LLVM_Handle(String libName, ParsedLLVM_IR[] irs) {
            this.libName = libName;
            this.parsedIRs = irs;
        }

        @Override
        public Type getRFFIType() {
            return RFFIFactory.Type.LLVM;
        }

    }

    @FunctionalInterface
    interface ModuleNameMatch {
        boolean match(String name);
    }

    public static final class LLVMArchive {
        public final LLVM_IR[] irs;
        public final List<String> nativeLibs;

        private LLVMArchive(LLVM_IR[] irs, List<String> nativeLibs) {
            super();
            this.irs = irs;
            this.nativeLibs = nativeLibs;
        }
    }

    @TruffleBoundary
    public static LLVMArchive getZipLLVMIR(String path) throws IOException {
        List<String> nativeLibs = Collections.emptyList();
        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(path + "l")))) {
            ArrayList<LLVM_IR> irList = new ArrayList<>();
            while (true) {
                ZipEntry entry = zis.getNextEntry();
                if (entry == null) {
                    break;
                }
                int size = (int) entry.getSize();
                byte[] bytes = new byte[size];
                int n;
                int totalRead = 0;
                while (totalRead < size && (n = zis.read(bytes, totalRead, size - totalRead)) != -1) {
                    totalRead += n;
                }
                if (LIBS.equals(entry.getName())) {
                    nativeLibs = getNativeLibs(bytes);
                    continue;
                }
                Path zipName = Paths.get(entry.getName());
                String name = zipName.getFileName().toString();
                int ix = name.indexOf('.');
                if (ix > 0) {
                    name = name.substring(0, ix);
                }
                LLVM_IR.Binary ir = new LLVM_IR.Binary(name, bytes, path);
                irList.add(ir);
                // debugging
                if (System.getenv("FASTR_LLVM_DEBUG") != null) {
                    try (FileOutputStream bs = new FileOutputStream(Paths.get("tmpzip", name).toString())) {
                        bs.write(bytes);
                    }
                    try (PrintStream bs = new PrintStream(new FileOutputStream(Paths.get("tmpb64", name).toString()))) {
                        bs.print(ir.base64);
                    }
                }
            }
            LLVM_IR[] result = new LLVM_IR[irList.size()];
            irList.toArray(result);
            return new LLVMArchive(result, nativeLibs);
        }
    }

    private static List<String> getNativeLibs(byte[] bytes) throws IOException {
        List<String> libs = new LinkedList<>();
        try (BufferedReader libReader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(bytes)))) {
            String lib = null;
            while ((lib = libReader.readLine()) != null) {
                libs.add(lib);
            }
        }
        return libs;
    }

    private static final class TruffleLLVM_DLOpenNode extends Node implements DLOpenNode {
        @Child private TruffleLLVM_NativeDLL.TruffleLLVM_NativeDLOpen nativeDLLOpenNode;

        /**
         * If a library is enabled for LLVM, the IR for all the modules is retrieved and analyzed.
         * Every exported symbol in the module added to the parseStatus map for the current
         * {@link RContext}. This allows {@code dlsym} to definitively locate any symbol, even if
         * the IR has not been parsed yet.
         */
        @Override
        @TruffleBoundary
        public LibHandle execute(String path, boolean local, boolean now) {
            RFFIContext stateRFFI = RContext.getInstance().getStateRFFI();
            long before = stateRFFI.beforeDowncall(RFFIFactory.Type.LLVM);

            try {
                LLVMArchive ar = getZipLLVMIR(path);
                LLVM_IR[] irs = ar.irs;
                String libName = getLibName(path);
                if (libName.equals("libR")) {
                    // save for new RContexts
                    truffleDLL.libRModules = irs;
                } else {
                    loadNativeLibs(ar.nativeLibs);
                }
                ParsedLLVM_IR[] parsedIRs = new ParsedLLVM_IR[irs.length];
                for (int i = 0; i < irs.length; i++) {
                    LLVM_IR ir = irs[i];
                    Object irLookupObject = parseLLVM(libName, ir).call();
                    parsedIRs[i] = new ParsedLLVM_IR(ir, irLookupObject);
                }
                return new LLVM_Handle(libName, parsedIRs);
            } catch (Exception ex) {
                CompilerDirectives.transferToInterpreter();
                StringBuilder sb = new StringBuilder();
                Throwable t = ex;
                while (t != null) {
                    if (t != ex) {
                        sb.append(": ");
                    }
                    sb.append(t.getMessage());
                    t = t.getCause();
                }
                ex.printStackTrace();
                throw new UnsatisfiedLinkError(sb.toString());
            } finally {
                stateRFFI.afterDowncall(before, RFFIFactory.Type.LLVM);
            }
        }

        private void loadNativeLibs(List<String> nativeLibs) {
            OSInfo osInfo = RPlatform.getOSInfo();
            for (String nativeLib : nativeLibs) {
                if (ignoredNativeLibs.contains(nativeLib)) {
                    continue;
                }
                String nativeLibName = "lib" + nativeLib + "." + osInfo.libExt;
                tryOpenNative(nativeLibName, false, true);
            }
        }

        private long tryOpenNative(String path, boolean local, boolean now) throws UnsatisfiedLinkError {
            if (nativeDLLOpenNode == null) {
                nativeDLLOpenNode = insert(new TruffleLLVM_NativeDLL.TruffleLLVM_NativeDLOpen());
            }
            return nativeDLLOpenNode.execute(path, local, now);
        }
    }

    private static class TruffleLLVM_DLSymNode extends Node implements DLSymNode {
        @Child private Node lookupNode = Message.READ.createNode();

        @Override
        public SymbolHandle execute(Object handle, String symbol) throws UnsatisfiedLinkError {
            assert handle instanceof LLVM_Handle;
            LLVM_Handle llvmHandle = (LLVM_Handle) handle;
            Object symValue = null;
            for (int i = 0; i < llvmHandle.parsedIRs.length; i++) {
                ParsedLLVM_IR pir = llvmHandle.parsedIRs[i];
                try {
                    symValue = ForeignAccess.sendRead(lookupNode, (TruffleObject) pir.lookupObject, symbol);
                    break;
                } catch (UnknownIdentifierException e) {
                    continue;
                } catch (UnsupportedMessageException e) {
                    RInternalError.shouldNotReachHere();
                }
            }
            if (symValue == null) {
                throw new UnsatisfiedLinkError();
            }
            return new SymbolHandle(symValue);
        }

    }

    private static class TruffleLLVM_DLCloseNode extends Node implements DLCloseNode {
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

    @TruffleBoundary
    private static CallTarget parseLLVM(String libName, LLVM_IR ir) {
        if (ir instanceof LLVM_IR.Binary) {
            LLVM_IR.Binary bir = (LLVM_IR.Binary) ir;
            return parseBinary(libName, bir);
        } else {
            throw RInternalError.unimplemented("LLVM text IR");
        }
    }

    private static CallTarget parseBinary(String libName, LLVM_IR.Binary ir) {
        long start = System.nanoTime();
        RContext context = RContext.getInstance();
        long nanos = 1000 * 1000 * 1000;
        Source source;
        boolean traceBitcode = isLibTraced(libName);
        if (traceBitcode) {
            String mimeType = "application/x-llvm-ir-bitcode";
            String language = Source.findLanguage(mimeType);
            try {
                Path irFile = new File(ir.libPath).getParentFile().toPath().resolve(ir.name + ".bc");
                Path llFile = new File(ir.libPath).getParentFile().toPath().resolve(ir.name + ".ll");
                byte[] decoded = null;
                if (!irFile.toFile().exists()) {
                    decoded = Base64.getDecoder().decode(ir.base64);
                    Files.write(irFile, decoded);
                }
                if (!llFile.toFile().exists()) {
                    decoded = decoded != null ? decoded : Base64.getDecoder().decode(ir.base64);
                    disassemble(llFile.toFile(), decoded);
                }
                TruffleFile irTruffleFile = RContext.getInstance().getEnv().getTruffleFile(irFile.toUri());
                source = Source.newBuilder(language, irTruffleFile).mimeType(mimeType).build();
            } catch (IOException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        } else {
            String mimeType = "application/x-llvm-ir-bitcode-base64";
            String language = Source.findLanguage(mimeType);
            source = Source.newBuilder(language, ir.base64, ir.name).mimeType(mimeType).build();
        }
        CallTarget result = context.getEnv().parse(source);

        if (System.getenv("LLVM_PARSE_TIME") != null) {
            System.out.println("WARNING: The LLVM_PARSE_TIME env variable was discontinued.\n" +
                            "You can rerun FastR with --log.R." + TruffleLLVM_DLL.class.getName() + ".level=FINE");
        }
        TruffleLogger logger = RLogger.getLogger(TruffleLLVM_DLL.class.getName());
        if (logger.isLoggable(Level.FINE)) {
            long end = System.nanoTime();
            logger.log(Level.FINE, "parsed {0}:{1} in {2} secs", new Object[]{libName, ir.name, ((double) (end - start)) / (double) nanos});
        }
        return result;
    }

    private static boolean isLibTraced(String libName) {
        String tracedLibs = System.getenv(FastROptions.DEBUG_LLVM_LIBS);
        if (tracedLibs == null || tracedLibs.isEmpty()) {
            return false;
        }

        String[] libNames = tracedLibs.split(",");
        for (String ln : libNames) {
            if (libName.equals(ln)) {
                return true;
            }
        }

        return false;
    }

    private static void disassemble(File llFile, byte[] ir) {
        try {
            ProcessBuilder pb = new ProcessBuilder("llvm-dis");
            pb.redirectOutput(llFile);
            Process p = pb.start();
            OutputStream is = p.getOutputStream();
            is.write(ir);
            is.close();
            int rc = p.waitFor();
            if (rc != 0) {
                System.err.printf("Warning: LLVM disassembler exited with status %d. Target file %s\n", rc, llFile);
            }
        } catch (Exception e) {
            throw RInternalError.shouldNotReachHere(e);
        }
    }
}
