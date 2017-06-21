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
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.ffi.impl.common.LibPaths;
import com.oracle.truffle.r.ffi.impl.interop.NativeCharArray;
import com.oracle.truffle.r.ffi.impl.interop.NativeIntegerArray;
import com.oracle.truffle.r.ffi.impl.interop.pcre.CaptureNamesResult;
import com.oracle.truffle.r.ffi.impl.interop.pcre.CompileResult;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.ffi.DLL;
import com.oracle.truffle.r.runtime.ffi.DLL.SymbolHandle;
import com.oracle.truffle.r.runtime.ffi.PCRERFFI;

public class TruffleLLVM_PCRE implements PCRERFFI {

    TruffleLLVM_PCRE() {
        // Need to ensure that the native pcre library is loaded
        String pcrePath = LibPaths.getBuiltinLibPath("pcre");
        System.load(pcrePath);
    }

    private static class TruffleLLVM_MaketablesNode extends MaketablesNode {
        @Child private Node message = LLVMFunction.maketables.createMessage();
        @CompilationFinal private SymbolHandle symbolHandle;

        @Override
        public long execute() {
            try {
                if (symbolHandle == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    symbolHandle = DLL.findSymbol(LLVMFunction.maketables.callName, null);
                }
                TruffleObject callResult = (TruffleObject) ForeignAccess.sendExecute(message, symbolHandle.asTruffleObject());
                long result = TruffleLLVM_Utils.getNativeAddress(callResult);
                return result;
            } catch (InteropException ex) {
                throw RInternalError.shouldNotReachHere(ex);
            }
        }
    }

    private static class TruffleLLVM_GetCaptureCountNode extends GetCaptureCountNode {
        @Child private Node message = LLVMFunction.getcapturecount.createMessage();
        @CompilationFinal private SymbolHandle symbolHandle;

        @Override
        public int execute(long code, long extra) {
            try {
                if (symbolHandle == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    symbolHandle = DLL.findSymbol(LLVMFunction.getcapturecount.callName, null);
                }
                int result = (int) ForeignAccess.sendExecute(message, symbolHandle.asTruffleObject(), code, extra);
                return result;
            } catch (InteropException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }
    }

    public static class TruffleLLVM_GetCaptureNamesNode extends GetCaptureNamesNode {
        @Child private Node message = LLVMFunction.getcapturenames.createMessage();
        @CompilationFinal private SymbolHandle symbolHandle;

        @Override
        public String[] execute(long code, long extra, int captureCount) {
            try {
                if (symbolHandle == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    symbolHandle = DLL.findSymbol(LLVMFunction.getcapturenames.callName, null);
                }
                CaptureNamesResult captureNamesCallback = new CaptureNamesResult(captureCount);
                int result = (int) ForeignAccess.sendExecute(message, symbolHandle.asTruffleObject(),
                                code, extra, captureNamesCallback);
                if (result < 0) {
                    CompilerDirectives.transferToInterpreter();
                    throw RError.error(RError.NO_CALLER, RError.Message.WRONG_PCRE_INFO, result);
                } else {
                    return captureNamesCallback.getCaptureNames();
                }
            } catch (InteropException ex) {
                throw RInternalError.shouldNotReachHere(ex);
            }
        }
    }

    public static class TruffleLLVM_CompileNode extends CompileNode {
        @Child private Node message = LLVMFunction.compile.createMessage();
        @CompilationFinal private SymbolHandle symbolHandle;

        @Override
        public Result execute(String pattern, int options, long tables) {
            try {
                if (symbolHandle == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    symbolHandle = DLL.findSymbol(LLVMFunction.compile.callName, null);
                }
                NativeCharArray pattenChars = new NativeCharArray(pattern.getBytes());
                CompileResult data = new CompileResult();
                ForeignAccess.sendExecute(message, symbolHandle.asTruffleObject(), data, pattenChars, options, tables);
                return data.getResult();
            } catch (InteropException ex) {
                throw RInternalError.shouldNotReachHere(ex);
            }
        }
    }

    private static class TruffleLLVM_ExecNode extends ExecNode {
        @Child private Node message = LLVMFunction.exec.createMessage();
        @CompilationFinal private SymbolHandle symbolHandle;

        @Override
        public int execute(long code, long extra, String subject, int offset, int options, int[] ovector) {
            NativeIntegerArray nativeOvector = new NativeIntegerArray(ovector);
            try {
                if (symbolHandle == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    symbolHandle = DLL.findSymbol(LLVMFunction.exec.callName, null);
                }
                byte[] subjectBytes = subject.getBytes();
                NativeCharArray subjectChars = new NativeCharArray(subjectBytes);
                int result = (int) ForeignAccess.sendExecute(message, symbolHandle.asTruffleObject(), code, extra,
                                subjectChars, subjectBytes.length,
                                offset, options, nativeOvector, ovector.length);
                return result;
            } catch (InteropException ex) {
                throw RInternalError.shouldNotReachHere(ex);
            } finally {
                nativeOvector.getValue();
            }
        }
    }

    @Override
    public MaketablesNode createMaketablesNode() {
        return new TruffleLLVM_MaketablesNode();
    }

    @Override
    public CompileNode createCompileNode() {
        return new TruffleLLVM_CompileNode();
    }

    @Override
    public GetCaptureCountNode createGetCaptureCountNode() {
        return new TruffleLLVM_GetCaptureCountNode();
    }

    @Override
    public GetCaptureNamesNode createGetCaptureNamesNode() {
        return new TruffleLLVM_GetCaptureNamesNode();
    }

    @Override
    public StudyNode createStudyNode() {
        throw RInternalError.unimplemented();
    }

    @Override
    public ExecNode createExecNode() {
        return new TruffleLLVM_ExecNode();
    }
}
