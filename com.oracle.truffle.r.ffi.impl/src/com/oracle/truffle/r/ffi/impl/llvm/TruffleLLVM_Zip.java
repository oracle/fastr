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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.ffi.impl.interop.NativeRawArray;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.ffi.DLL;
import com.oracle.truffle.r.runtime.ffi.DLL.SymbolHandle;
import com.oracle.truffle.r.runtime.ffi.ZipRFFI;

public class TruffleLLVM_Zip implements ZipRFFI {
    private static class TruffleLLVM_CompressNode extends ZipRFFI.CompressNode {
        @Child private Node message = LLVMFunction.compress.createMessage();
        @CompilationFinal private SymbolHandle symbolHandle;

        @Override
        public int execute(byte[] dest, byte[] source) {
            NativeRawArray nativeDest = new NativeRawArray(dest);
            NativeRawArray nativeSource = new NativeRawArray(source);
            try {
                if (symbolHandle == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    symbolHandle = DLL.findSymbol(LLVMFunction.compress.callName, null);
                }
                int result = (int) ForeignAccess.sendExecute(message, symbolHandle.asTruffleObject(),
                                nativeDest, dest.length, nativeSource, source.length);
                return result;
            } catch (InteropException e) {
                throw RInternalError.shouldNotReachHere(e);
            } finally {
                nativeDest.getValue();
            }
        }
    }

    private static class TruffleLLVM_UncompressNode extends ZipRFFI.UncompressNode {
        @Child private Node message = LLVMFunction.uncompress.createMessage();
        @CompilationFinal private SymbolHandle symbolHandle;

        @Override
        public int execute(byte[] dest, byte[] source) {
            NativeRawArray nativeDest = new NativeRawArray(dest);
            NativeRawArray nativeSource = new NativeRawArray(source);
            try {
                if (symbolHandle == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    symbolHandle = DLL.findSymbol(LLVMFunction.uncompress.callName, null);
                }
                int result = (int) ForeignAccess.sendExecute(message, symbolHandle.asTruffleObject(),
                                nativeDest, dest.length, nativeSource, source.length);
                return result;
            } catch (InteropException e) {
                throw RInternalError.shouldNotReachHere(e);
            } finally {
                nativeDest.getValue();
            }
        }
    }

    @Override
    public CompressNode createCompressNode() {
        return new TruffleLLVM_CompressNode();
    }

    @Override
    public UncompressNode createUncompressNode() {
        return new TruffleLLVM_UncompressNode();
    }
}
