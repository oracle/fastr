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
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.r.ffi.impl.common.LibPaths;
import com.oracle.truffle.r.ffi.impl.interop.NativeCharArray;
import com.oracle.truffle.r.ffi.impl.interop.pcre.CaptureNamesResult;
import com.oracle.truffle.r.ffi.impl.interop.pcre.CompileResult;
import com.oracle.truffle.r.ffi.impl.llvm.TruffleLLVM_Utils.AsPointerNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.ffi.PCRERFFI;

public class TruffleLLVM_PCRE implements PCRERFFI {

    TruffleLLVM_PCRE() {
        // Need to ensure that the native pcre library is loaded
        String pcrePath = LibPaths.getBuiltinLibPath("pcre");
        TruffleLLVM_NativeDLL.NativeDLOpenRootNode.create().getCallTarget().call(pcrePath, false, true);
    }

    private static class TruffleLLVM_MaketablesNode extends TruffleLLVM_DownCallNode implements MaketablesNode {

        @Child private AsPointerNode asPointer = new AsPointerNode();

        @Override
        protected LLVMFunction getFunction() {
            return LLVMFunction.maketables;
        }

        @Override
        public long execute() {
            return asPointer.execute((TruffleObject) call());
        }
    }

    private static class TruffleLLVM_GetCaptureCountNode extends TruffleLLVM_DownCallNode implements GetCaptureCountNode {
        @Override
        protected LLVMFunction getFunction() {
            return LLVMFunction.getcapturecount;
        }

        @Override
        public int execute(long code, long extra) {
            return (int) call(code, extra);
        }
    }

    private static class TruffleLLVM_GetCaptureNamesNode extends TruffleLLVM_DownCallNode implements GetCaptureNamesNode {
        @Override
        protected LLVMFunction getFunction() {
            return LLVMFunction.getcapturenames;
        }

        @Override
        public String[] execute(long code, long extra, int captureCount) {
            CaptureNamesResult captureNamesCallback = new CaptureNamesResult(captureCount);
            int result = (int) call(captureNamesCallback, code, extra);
            if (result < 0) {
                CompilerDirectives.transferToInterpreter();
                throw RError.error(RError.NO_CALLER, RError.Message.WRONG_PCRE_INFO, result);
            } else {
                return captureNamesCallback.getCaptureNames();
            }
        }
    }

    private static class TruffleLLVM_CompileNode extends TruffleLLVM_DownCallNode implements CompileNode {
        @Override
        protected LLVMFunction getFunction() {
            return LLVMFunction.compile;
        }

        @Override
        public Result execute(String pattern, int options, long tables) {
            NativeCharArray pattenChars = new NativeCharArray(pattern.getBytes());
            CompileResult data = new CompileResult();
            call(data, pattenChars, options, tables);
            return data.getResult();
        }
    }

    private static class TruffleLLVM_ExecNode extends TruffleLLVM_DownCallNode implements ExecNode {
        @Override
        protected LLVMFunction getFunction() {
            return LLVMFunction.exec;
        }

        @Override
        public int execute(long code, long extra, String subject, int offset, int options, int[] ovector) {
            byte[] subjectBytes = subject.getBytes();
            NativeCharArray subjectChars = new NativeCharArray(subjectBytes);
            return (int) call(code, extra, subjectChars, subjectBytes.length, offset, options, ovector, ovector.length);
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
