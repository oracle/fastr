/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.util.ArrayList;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.java.JavaInterop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.ffi.impl.interop.NativeCharArray;
import com.oracle.truffle.r.ffi.impl.interop.base.GlobResult;
import com.oracle.truffle.r.ffi.impl.interop.base.ReadlinkResult;
import com.oracle.truffle.r.ffi.impl.interop.base.StrtolResult;
import com.oracle.truffle.r.ffi.impl.interop.base.UnameResult;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.RContext.ContextState;
import com.oracle.truffle.r.runtime.ffi.BaseRFFI;
import com.oracle.truffle.r.runtime.ffi.DLL;
import com.oracle.truffle.r.runtime.ffi.DLL.SymbolHandle;
import com.oracle.truffle.r.runtime.ffi.RFFIFactory;

public class TruffleLLVM_Base implements BaseRFFI {
    private static TruffleObject truffleBaseTruffleObject;

    TruffleLLVM_Base() {
        truffleBaseTruffleObject = JavaInterop.asTruffleObject(this);
    }

    static class ContextStateImpl implements RContext.ContextState {
        @Override
        public ContextState initialize(RContext context) {
            RFFIFactory.getRFFI().getBaseRFFI();
            context.getEnv().exportSymbol("_fastr_rffi_base", truffleBaseTruffleObject);
            return this;
        }

    }

    public static class TruffleLLVM_GetpidNode extends GetpidNode {
        @Child private Node message = LLVMFunction.getpid.createMessage();
        @CompilationFinal private SymbolHandle symbolHandle;

        @Override
        public int execute() {
            try {
                if (symbolHandle == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    symbolHandle = DLL.findSymbol(LLVMFunction.getpid.callName, null);
                }
                int result = (int) ForeignAccess.sendExecute(message, symbolHandle.asTruffleObject());
                return result;
            } catch (InteropException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }
    }

    public static class TruffleLLVM_GetwdNode extends GetwdNode {
        @Child private Node message = LLVMFunction.getwd.createMessage();
        @CompilationFinal private SymbolHandle symbolHandle;

        @Override
        public String execute() {
            byte[] buf = new byte[4096];
            NativeCharArray nativeBuf = new NativeCharArray(buf);
            try {
                if (symbolHandle == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    symbolHandle = DLL.findSymbol(LLVMFunction.getwd.callName, null);
                }
                int result = (int) ForeignAccess.sendExecute(message, symbolHandle.asTruffleObject(), nativeBuf, buf.length);
                if (result == 0) {
                    return null;
                } else {
                    byte[] mbuf = nativeBuf.getValue();
                    int i = 0;
                    while (mbuf[i] != 0 && i < mbuf.length) {
                        i++;
                    }
                    return new String(mbuf, 0, i);
                }
            } catch (InteropException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }
    }

    public static class TruffleLLVM_SetwdNode extends SetwdNode {
        @Child private Node message = LLVMFunction.setwd.createMessage();
        @CompilationFinal private SymbolHandle symbolHandle;

        @Override
        public int execute(String dir) {
            NativeCharArray nativeBuf = new NativeCharArray(dir.getBytes());
            try {
                if (symbolHandle == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    symbolHandle = DLL.findSymbol(LLVMFunction.setwd.callName, null);
                }
                int result = (int) ForeignAccess.sendExecute(message, symbolHandle.asTruffleObject(), nativeBuf);
                return result;
            } catch (InteropException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }
    }

    public static class TruffleLLVM_MkdirNode extends MkdirNode {
        @Child private Node message = LLVMFunction.mkdir.createMessage();
        @CompilationFinal private SymbolHandle symbolHandle;

        @Override
        public void execute(String dir, int mode) throws IOException {
            NativeCharArray nativeBuf = new NativeCharArray(dir.getBytes());
            try {
                if (symbolHandle == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    symbolHandle = DLL.findSymbol(LLVMFunction.mkdir.callName, null);
                }
                int result = (int) ForeignAccess.sendExecute(message, symbolHandle.asTruffleObject(), nativeBuf, mode);
                if (result != 0) {
                    throw new IOException("mkdir " + dir + " failed");
                }
            } catch (InteropException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }
    }

    public void setReadlinkResult(ReadlinkResult baseReadlinkResultCallback, NativeCharArray nativePath, int errno) {
        String path = null;
        if (nativePath != null) {
            path = new String(nativePath.getValue());
        }
        baseReadlinkResultCallback.setResult(path, errno);
    }

    public static class TruffleLLVM_ReadlinkNode extends ReadlinkNode {
        private static final int EINVAL = 22;
        @Child private Node message = LLVMFunction.readlink.createMessage();
        @CompilationFinal private SymbolHandle symbolHandle;

        @Override
        public String execute(String path) throws IOException {
            NativeCharArray nativePath = new NativeCharArray(path.getBytes());
            try {
                if (symbolHandle == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    symbolHandle = DLL.findSymbol(LLVMFunction.readlink.callName, null);
                }
                ReadlinkResult callback = new ReadlinkResult();
                ForeignAccess.sendExecute(message, symbolHandle.asTruffleObject(), callback, nativePath);
                String link = callback.getLink();
                if (link == null) {
                    if (callback.getErrno() == EINVAL) {
                        return path;
                    } else {
                        // some other error
                        throw new IOException("readlink failed: " + callback.getErrno());
                    }
                }
                return link;
            } catch (InteropException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }
    }

    public static class TruffleLLVM_MkdtempNode extends MkdtempNode {
        @Child private Node message = LLVMFunction.mkdtemp.createMessage();
        @CompilationFinal private SymbolHandle symbolHandle;

        @Override
        public String execute(String template) {
            /*
             * Not only must the (C) string end in XXXXXX it must also be null-terminated. Since it
             * is modified by mkdtemp we must make a copy.
             */
            byte[] bytes = template.getBytes();
            byte[] ztbytes = new byte[bytes.length + 1];
            System.arraycopy(bytes, 0, ztbytes, 0, bytes.length);
            ztbytes[bytes.length] = 0;
            NativeCharArray nativeZtbytes = new NativeCharArray(ztbytes);
            try {
                if (symbolHandle == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    symbolHandle = DLL.findSymbol(LLVMFunction.mkdtemp.callName, null);
                }
                int result = (int) ForeignAccess.sendExecute(message, symbolHandle.asTruffleObject(), nativeZtbytes);
                if (result == 0) {
                    return null;
                } else {
                    byte[] mztBytes = nativeZtbytes.getValue();
                    String path = new String(mztBytes, 0, bytes.length);
                    return path;
                }
            } catch (InteropException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }
    }

    public static class TruffleLLVM_ChmodNode extends ChmodNode {
        @Child private Node message = LLVMFunction.chmod.createMessage();
        @CompilationFinal private SymbolHandle symbolHandle;

        @Override
        public int execute(String path, int mode) {
            NativeCharArray nativePath = new NativeCharArray(path.getBytes());
            try {
                if (symbolHandle == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    symbolHandle = DLL.findSymbol(LLVMFunction.chmod.callName, null);
                }
                int result = (int) ForeignAccess.sendExecute(message, symbolHandle.asTruffleObject(), nativePath, mode);
                return result;
            } catch (InteropException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }
    }

    public void setStrtolResult(StrtolResult callback, long value, int errno) {
        callback.setResult(value, errno);
    }

    public static class TruffleLLVM_StrolNode extends StrolNode {
        @Child private Node message = LLVMFunction.strtol.createMessage();
        @CompilationFinal private SymbolHandle symbolHandle;

        @Override
        public long execute(String s, int base) throws IllegalArgumentException {
            NativeCharArray nativeString = new NativeCharArray(s.getBytes());
            try {
                if (symbolHandle == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    symbolHandle = DLL.findSymbol(LLVMFunction.strtol.callName, null);
                }
                StrtolResult callback = new StrtolResult();
                ForeignAccess.sendExecute(message, symbolHandle.asTruffleObject(), callback, nativeString, base);
                if (callback.getErrno() != 0) {
                    throw new IllegalArgumentException("strtol failure");
                } else {
                    return callback.getResult();
                }
            } catch (InteropException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }
    }

    public void setUnameResult(UnameResult baseUnameResultCallback, NativeCharArray sysname, NativeCharArray release,
                    NativeCharArray version, NativeCharArray machine, NativeCharArray nodename) {
        baseUnameResultCallback.setResult(new String(sysname.getValue()), new String(release.getValue()), new String(version.getValue()),
                        new String(machine.getValue()), new String(nodename.getValue()));
    }

    public static class TruffleLLVM_UnameNode extends UnameNode {
        @Child private Node message = LLVMFunction.uname.createMessage();
        @CompilationFinal private SymbolHandle symbolHandle;

        @Override
        public UtsName execute() {
            try {
                if (symbolHandle == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    symbolHandle = DLL.findSymbol(LLVMFunction.uname.callName, null);
                }
                UnameResult baseUnameResultCallback = new UnameResult();
                ForeignAccess.sendExecute(message, symbolHandle.asTruffleObject(), baseUnameResultCallback);
                return baseUnameResultCallback;
            } catch (InteropException ex) {
                throw RInternalError.shouldNotReachHere(ex);
            }
        }
    }

    public void setGlobResult(GlobResult baseGlobResultCallback, NativeCharArray path) {
        baseGlobResultCallback.addPath(new String(path.getValue()));
    }

    public static class TruffleLLVM_GlobNode extends GlobNode {
        @Child private Node message = LLVMFunction.glob.createMessage();
        @CompilationFinal private SymbolHandle symbolHandle;

        @Override
        public ArrayList<String> glob(String pattern) {
            try {
                if (symbolHandle == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    symbolHandle = DLL.findSymbol(LLVMFunction.glob.callName, null);
                }
                NativeCharArray nativePattern = new NativeCharArray(pattern.getBytes());
                GlobResult baseGlobResultCallback = new GlobResult();
                ForeignAccess.sendExecute(message, symbolHandle.asTruffleObject(), baseGlobResultCallback, nativePattern);
                return baseGlobResultCallback.getPaths();
            } catch (InteropException ex) {
                throw RInternalError.shouldNotReachHere(ex);
            }
        }
    }

    @Override
    public GetpidNode createGetpidNode() {
        return new TruffleLLVM_GetpidNode();
    }

    @Override
    public GetwdNode createGetwdNode() {
        return new TruffleLLVM_GetwdNode();
    }

    @Override
    public SetwdNode createSetwdNode() {
        return new TruffleLLVM_SetwdNode();
    }

    @Override
    public MkdirNode createMkdirNode() {
        return new TruffleLLVM_MkdirNode();
    }

    @Override
    public ReadlinkNode createReadlinkNode() {
        return new TruffleLLVM_ReadlinkNode();
    }

    @Override
    public MkdtempNode createMkdtempNode() {
        return new TruffleLLVM_MkdtempNode();
    }

    @Override
    public ChmodNode createChmodNode() {
        return new TruffleLLVM_ChmodNode();
    }

    @Override
    public StrolNode createStrolNode() {
        return new TruffleLLVM_StrolNode();
    }

    @Override
    public UnameNode createUnameNode() {
        return new TruffleLLVM_UnameNode();
    }

    @Override
    public GlobNode createGlobNode() {
        return new TruffleLLVM_GlobNode();
    }
}
