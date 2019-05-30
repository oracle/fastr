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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleFile;
import com.oracle.truffle.api.TruffleLanguage.Env;
import com.oracle.truffle.api.TruffleLogger;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.Source.SourceBuilder;
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RLogger;
import com.oracle.truffle.r.runtime.RPlatform;
import com.oracle.truffle.r.runtime.RPlatform.OSInfo;
import com.oracle.truffle.r.runtime.context.FastROptions;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.RContext.ContextState;
import com.oracle.truffle.r.runtime.ffi.DLL;
import com.oracle.truffle.r.runtime.ffi.DLL.DLLInfo;
import com.oracle.truffle.r.runtime.ffi.DLL.SymbolHandle;
import com.oracle.truffle.r.runtime.ffi.DLLRFFI;
import com.oracle.truffle.r.runtime.ffi.RFFIContext;
import com.oracle.truffle.r.runtime.ffi.RFFIFactory;
import com.oracle.truffle.r.runtime.ffi.RFFIFactory.Type;

public class TruffleLLVM_DLL implements DLLRFFI {
    // TODO: these dependencies were ignored for some reason, will it be a problem???
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
//                for (LLVM_IR ir : truffleDLL.libRModules) {
//                    parseLLVM(context.getEnv(), "libR", ir);
//                }
            }
            if (context.getKind() == RContext.ContextKind.SHARE_PARENT_RW) {
                // TODO: must propagate all LLVM library exports??
            }
            return this;
        }
    }

    private static final class TruffleLLVM_DLOpenNode extends Node implements DLOpenNode {
        @Override
        @TruffleBoundary
        public LibHandle execute(String path, @SuppressWarnings("unused") boolean local, @SuppressWarnings("unused") boolean now) {
            return dlOpen(RContext.getInstance(), path);
        }
    }

    static LibHandle dlOpen(RContext context, String path) {
        final Env env = context.getEnv();
        RFFIContext stateRFFI = context.getStateRFFI();
        TruffleFile file = env.getTruffleFile(path);
        String libName = DLL.libName(file.getPath());
        boolean isLibR = libName.equals("libR");
        if (isLibR) {
            file = env.getTruffleFile(path + "l"); // TODO: make it generic, if +"l" file exists, use that instead
        }
        boolean isInitialization = isLibR;
        Object before = null;
        try {
            if (!isInitialization) {
                before = stateRFFI.beforeDowncall(null, Type.LLVM);
            }
            Source src = Source.newBuilder("llvm", file).internal(true).build();
            Object lib = env.parse(src).call();
            assert lib instanceof TruffleObject;
            if (isLibR) {
                // TODO: accessing what used to be a private field, this will be refactored
                InteropLibrary interop = InteropLibrary.getFactory().getUncached();
                Object setSymbol = interop.readMember(lib, "Rdynload_setSymbol");
                context.getStateRFFI(TruffleLLVM_Context.class).callState.upCallsRFFIImpl.setSymbolHandle = (TruffleObject) setSymbol;
            }
            return new LLVM_Handle(libName, lib);
        } catch (IOException ex) {
            CompilerDirectives.transferToInterpreter();
            throw RError.error(RError.NO_CALLER, Message.GENERIC, "Cannot load bitcode from library file: " + path);
        } catch (UnsupportedMessageException e) {
            throw RInternalError.shouldNotReachHere("Loaded library does not support readMember message");
        } catch (UnknownIdentifierException e) {
            CompilerDirectives.transferToInterpreter();
            throw RError.error(RError.NO_CALLER, Message.GENERIC, "Could not find function 'Rdynload_setSymbol' in libR on path: " + path);
        } finally {
            if (!isInitialization && before != null) {
                stateRFFI.afterDowncall(before, Type.LLVM);
            }
        }
    }

    public static class LLVM_Handle implements LibHandle {
        final String libName;
        final Object handle;

        public LLVM_Handle(LLVM_Handle libHandle) {
            this(libHandle.libName, libHandle.handle);
        }

        public LLVM_Handle(String libName, Object handle) {
            this.libName = libName;
            this.handle = handle;
        }

        @Override
        public Type getRFFIType() {
            return RFFIFactory.Type.LLVM;
        }

    }

    private static class TruffleLLVM_DLSymNode extends Node implements DLSymNode {
        @Child InteropLibrary interop = InteropLibrary.getFactory().createDispatched(DSLConfig.getInteropLibraryCacheSize());

        @Override
        public SymbolHandle execute(Object handle, String symbol) throws UnsatisfiedLinkError {
            assert handle instanceof LLVM_Handle && ((LLVM_Handle) handle).getRFFIType() == Type.LLVM;
            try {
                Object result = interop.readMember(((LLVM_Handle) handle).handle, symbol);
                return new SymbolHandle(result);
            } catch (UnsupportedMessageException e) {
                throw RInternalError.shouldNotReachHere("LibHandle does not support readMember operation");
            } catch (UnknownIdentifierException e) {
                CompilerDirectives.transferToInterpreter();
                throw new UnsatisfiedLinkError();
            }
        }
    }

    private static class TruffleLLVM_DLCloseNode extends Node implements DLCloseNode {
        @Override
        public int execute(Object handle) {
            assert handle instanceof LLVM_Handle && ((LLVM_Handle) handle).getRFFIType() == Type.LLVM;
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

    // Method used only for internal debugging, not run by default
    // To be adjusted to the fact that we load bitcode embedded in native libraries
    @SuppressWarnings("unused")
    private static void disassemble(TruffleFile llFile, byte[] ir) {
        try {
            ProcessBuilder pb = new ProcessBuilder("llvm-dis");
            pb.redirectOutput(Paths.get(llFile.getAbsoluteFile().getPath()).toFile());
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
