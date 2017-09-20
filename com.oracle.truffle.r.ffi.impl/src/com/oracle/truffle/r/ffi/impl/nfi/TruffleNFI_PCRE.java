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
package com.oracle.truffle.r.ffi.impl.nfi;

import java.nio.charset.StandardCharsets;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.interop.java.JavaInterop;
import com.oracle.truffle.r.ffi.impl.interop.pcre.CaptureNamesResult;
import com.oracle.truffle.r.ffi.impl.interop.pcre.CompileResult;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.ffi.PCRERFFI;

public class TruffleNFI_PCRE implements PCRERFFI {

    private static class TruffleNFI_MaketablesNode extends TruffleNFI_DownCallNode implements MaketablesNode {
        @Override
        protected NativeFunction getFunction() {
            return NativeFunction.maketables;
        }

        @Override
        public long execute() {
            return (long) call();
        }
    }

    private static class TruffleNFI_GetCaptureCountNode extends TruffleNFI_DownCallNode implements GetCaptureCountNode {
        @Override
        protected NativeFunction getFunction() {
            return NativeFunction.getcapturecount;
        }

        @Override
        public int execute(long code, long extra) {
            return (int) call(code, extra);
        }
    }

    private static class TruffleNFI_GetCaptureNamesNode extends TruffleNFI_DownCallNode implements GetCaptureNamesNode {
        @Override
        protected NativeFunction getFunction() {
            return NativeFunction.getcapturenames;
        }

        @Override
        public String[] execute(long code, long extra, int captureCount) {
            CaptureNamesResult data = new CaptureNamesResult(captureCount);
            int result = (int) call(data, code, extra);
            if (result < 0) {
                CompilerDirectives.transferToInterpreter();
                throw RError.error(RError.NO_CALLER, RError.Message.WRONG_PCRE_INFO, result);
            } else {
                return data.getCaptureNames();
            }
        }
    }

    private static class TruffleNFI_CompileNode extends TruffleNFI_DownCallNode implements CompileNode {
        @Override
        protected NativeFunction getFunction() {
            return NativeFunction.compile;
        }

        @Override
        public Result execute(String pattern, int options, long tables) {
            CompileResult data = new CompileResult();
            call(data, pattern, options, tables);
            return data.getResult();
        }
    }

    private static class TruffleNFI_ExecNode extends TruffleNFI_DownCallNode implements ExecNode {
        @Override
        protected NativeFunction getFunction() {
            return NativeFunction.exec;
        }

        @Override
        public int execute(long code, long extra, String subject, int offset, int options, int[] ovector) {
            byte[] subjectBytes = subject.getBytes(StandardCharsets.UTF_8);
            return (int) call(code, extra, JavaInterop.asTruffleObject(subjectBytes), subjectBytes.length, offset, options, ovector, ovector.length);
        }
    }

    @Override
    public MaketablesNode createMaketablesNode() {
        return new TruffleNFI_MaketablesNode();
    }

    @Override
    public CompileNode createCompileNode() {
        return new TruffleNFI_CompileNode();
    }

    @Override
    public GetCaptureCountNode createGetCaptureCountNode() {
        return new TruffleNFI_GetCaptureCountNode();
    }

    @Override
    public GetCaptureNamesNode createGetCaptureNamesNode() {
        return new TruffleNFI_GetCaptureNamesNode();
    }

    @Override
    public StudyNode createStudyNode() {
        throw RInternalError.unimplemented();
    }

    @Override
    public ExecNode createExecNode() {
        return new TruffleNFI_ExecNode();
    }
}
