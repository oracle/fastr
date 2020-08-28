/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleFile;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.r.ffi.impl.common.LibPaths;
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.REnvVars;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.ffi.AfterDownCallProfiles;
import com.oracle.truffle.r.runtime.ffi.DLL;
import com.oracle.truffle.r.runtime.ffi.DLL.SymbolHandle;
import com.oracle.truffle.r.runtime.ffi.DLLRFFI;
import com.oracle.truffle.r.runtime.ffi.RFFIContext;
import com.oracle.truffle.r.runtime.ffi.RFFIFactory;
import com.oracle.truffle.r.runtime.ffi.RFFIFactory.Type;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class TruffleLLVM_DLL implements DLLRFFI {
    // TODO: these dependencies were ignored for some reason, will it be a problem???
    private static final Set<String> ignoredNativeLibs = new HashSet<>();
    static {
        ignoredNativeLibs.add("Rblas");
        ignoredNativeLibs.add("Rlapack");
    }

    private static final class TruffleLLVM_DLOpenNode extends Node implements DLOpenNode {
        @Override
        @TruffleBoundary
        public LibHandle execute(String path, @SuppressWarnings("unused") boolean local, @SuppressWarnings("unused") boolean now) {
            return dlOpen(RContext.getInstance(), path);
        }
    }

    static LibHandle dlOpen(RContext context, String path) {
        RFFIContext stateRFFI = context.getStateRFFI();
        TruffleFile file = context.getSafeTruffleFile(path);
        String libName = DLL.libName(context, file.getPath());
        boolean isLibR = libName.equals("libR");
        if (isLibR) {
            // TODO: make it generic, if +"l" file exists, use that instead
            file = context.getSafeTruffleFile(LibPaths.normalizeLibRPath(path, "llvm"));
        }
        boolean isInitialization = isLibR;
        Object before = null;
        try {
            if (!isInitialization) {
                before = stateRFFI.beforeDowncall(null, Type.LLVM);
            }
            Source src = Source.newBuilder("llvm", file).internal(isLibR).build();
            Object lib = context.getEnv().parseInternal(src).call();
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
        } catch (Throwable e) {
            RError.warning(RError.NO_CALLER, Message.GENERIC, String.format(
                            "Loading of '%s' in LLVM mode failed. " +
                                            "You may load this package via the native mode by adding it to %s/etc/native-packages " +
                                            "or by running FastR with option --R.BackEndNative=packageName.",
                            path, REnvVars.rHome(RContext.getInstance())));
            throw e;
        } finally {
            if (!isInitialization) {
                stateRFFI.afterDowncall(before, Type.LLVM, AfterDownCallProfiles.getUncached());
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
            Object llvmHandle = ((LLVM_Handle) handle).handle;
            try {
                // Following code is disabled because of GR-21497
                // if (!interop.isMemberReadable(llvmHandle, symbol)) {
                // CompilerDirectives.transferToInterpreter();
                // throw new UnsatisfiedLinkError();
                // }
                Object result = interop.readMember(llvmHandle, symbol);
                return new SymbolHandle(result);
            } catch (UnsupportedMessageException e) {
                throw RInternalError.shouldNotReachHere("LibHandle does not support readMember operation");
            } catch (UnknownIdentifierException | IllegalStateException e) {
                // Catching IllegalStateException is a workaround for GR-21497
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
    // @SuppressWarnings("unused")
    // private static void disassemble(TruffleFile llFile, byte[] ir) {
    // try {
    // ProcessBuilder pb = new ProcessBuilder("llvm-dis");
    // File toFile = Paths.get(llFile.getAbsoluteFile().getPath()).toFile();
    // pb.redirectOutput(toFile);
    // Process p = pb.start();
    // OutputStream is = p.getOutputStream();
    // is.write(ir);
    // is.close();
    // int rc = p.waitFor();
    // if (rc != 0) {
    // System.err.printf("Warning: LLVM disassembler exited with status %d. Target file %s\n", rc,
    // llFile);
    // }
    // } catch (Exception e) {
    // throw RInternalError.shouldNotReachHere(e);
    // }
    // }
}
