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
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.ffi.impl.interop.NativeCharArray;
import com.oracle.truffle.r.ffi.impl.interop.base.GlobResult;
import com.oracle.truffle.r.ffi.impl.interop.base.ReadlinkResult;
import com.oracle.truffle.r.ffi.impl.interop.base.StrtolResult;
import com.oracle.truffle.r.ffi.impl.interop.base.UnameResult;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.ffi.BaseRFFI;
import com.oracle.truffle.r.runtime.ffi.DLL.SymbolHandle;

public class TruffleLLVM_Base implements BaseRFFI {
    private static class TruffleLLVM_GetpidNode extends Node implements GetpidNode {

        @Child private Node message = LLVMFunction.getpid.createMessage();
        @CompilationFinal private SymbolHandle symbolHandle;

        @Override
        public int execute() {
            try {
                if (symbolHandle == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    symbolHandle = LLVMFunction.getpid.createSymbol();
                }
                int result = (int) ForeignAccess.sendExecute(message, symbolHandle.asTruffleObject());
                return result;
            } catch (InteropException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }
    }

    private static class TruffleLLVM_GetwdNode extends Node implements GetwdNode {

        @Child private Node message = LLVMFunction.getwd.createMessage();
        @CompilationFinal private SymbolHandle symbolHandle;

        @Override
        public String execute() {
            byte[] buf = new byte[4096];
            NativeCharArray nativeBuf = new NativeCharArray(buf);
            try {
                if (symbolHandle == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    symbolHandle = LLVMFunction.getwd.createSymbol();
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

    private static class TruffleLLVM_SetwdNode extends Node implements SetwdNode {
        @Child private Node message = LLVMFunction.setwd.createMessage();
        @CompilationFinal private SymbolHandle symbolHandle;

        @Override
        public int execute(String dir) {
            NativeCharArray nativeBuf = new NativeCharArray(dir.getBytes());
            try {
                if (symbolHandle == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    symbolHandle = LLVMFunction.setwd.createSymbol();
                }
                int result = (int) ForeignAccess.sendExecute(message, symbolHandle.asTruffleObject(), nativeBuf);
                return result;
            } catch (InteropException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }
    }

    private static class TruffleLLVM_MkdirNode extends Node implements MkdirNode {
        @Child private Node message = LLVMFunction.mkdir.createMessage();
        @CompilationFinal private SymbolHandle symbolHandle;

        @Override
        public void execute(String dir, int mode) throws IOException {
            NativeCharArray nativeBuf = new NativeCharArray(dir.getBytes());
            try {
                if (symbolHandle == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    symbolHandle = LLVMFunction.mkdir.createSymbol();
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

    private static class TruffleLLVM_ReadlinkNode extends Node implements ReadlinkNode {
        private static final int EINVAL = 22;
        @Child private Node message = LLVMFunction.readlink.createMessage();
        @CompilationFinal private SymbolHandle symbolHandle;

        @Override
        public String execute(String path) throws IOException {
            NativeCharArray nativePath = new NativeCharArray(path.getBytes());
            try {
                if (symbolHandle == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    symbolHandle = LLVMFunction.readlink.createSymbol();
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

    private static class TruffleLLVM_MkdtempNode extends Node implements MkdtempNode {
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
                    symbolHandle = LLVMFunction.mkdtemp.createSymbol();
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

    private static class TruffleLLVM_ChmodNode extends Node implements ChmodNode {
        @Child private Node message = LLVMFunction.chmod.createMessage();
        @CompilationFinal private SymbolHandle symbolHandle;

        @Override
        public int execute(String path, int mode) {
            NativeCharArray nativePath = new NativeCharArray(path.getBytes());
            try {
                if (symbolHandle == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    symbolHandle = LLVMFunction.chmod.createSymbol();
                }
                int result = (int) ForeignAccess.sendExecute(message, symbolHandle.asTruffleObject(), nativePath, mode);
                return result;
            } catch (InteropException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }
    }

    private static class TruffleLLVM_StrolNode extends Node implements StrolNode {
        @Child private Node message = LLVMFunction.strtol.createMessage();
        @CompilationFinal private SymbolHandle symbolHandle;

        @Override
        public long execute(String s, int base) throws IllegalArgumentException {
            NativeCharArray nativeString = new NativeCharArray(s.getBytes());
            try {
                if (symbolHandle == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    symbolHandle = LLVMFunction.strtol.createSymbol();
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

    private static class TruffleLLVM_UnameNode extends Node implements UnameNode {
        @Child private Node message = LLVMFunction.uname.createMessage();
        @CompilationFinal private SymbolHandle symbolHandle;

        @Override
        public UtsName execute() {
            try {
                if (symbolHandle == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    symbolHandle = LLVMFunction.uname.createSymbol();
                }
                UnameResult baseUnameResultCallback = new UnameResult();
                ForeignAccess.sendExecute(message, symbolHandle.asTruffleObject(), baseUnameResultCallback);
                return baseUnameResultCallback;
            } catch (InteropException ex) {
                throw RInternalError.shouldNotReachHere(ex);
            }
        }
    }

    private static class TruffleLLVM_GlobNode extends Node implements GlobNode {
        @Child private Node message = LLVMFunction.glob.createMessage();
        @CompilationFinal private SymbolHandle symbolHandle;

        @Override
        public ArrayList<String> glob(String pattern) {
            try {
                if (symbolHandle == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    symbolHandle = LLVMFunction.glob.createSymbol();
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
