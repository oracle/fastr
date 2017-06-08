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
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.java.JavaInterop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.ffi.impl.interop.pcre.CaptureNamesResult;
import com.oracle.truffle.r.ffi.impl.interop.pcre.CompileResult;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.ffi.PCRERFFI;

public class TruffleNFI_PCRE implements PCRERFFI {

    private static class TruffleNFI_MaketablesNode extends MaketablesNode {
        @Child private Node message = NFIFunction.maketables.createMessage();

        @Override
        public long execute() {
            try {
                long result = (long) ForeignAccess.sendExecute(message, NFIFunction.maketables.getFunction());
                return result;
            } catch (InteropException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }
    }

    private static class TruffleNFI_GetCaptureCountNode extends GetCaptureCountNode {
        @Child private Node message = NFIFunction.getcapturecount.createMessage();

        @Override
        public int execute(long code, long extra) {
            try {
                int result = (int) ForeignAccess.sendExecute(message, NFIFunction.getcapturecount.getFunction(), code, extra);
                return result;
            } catch (InteropException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }
    }

    public static class TruffleNFI_GetCaptureNamesNode extends GetCaptureNamesNode {

        @Child private Node message = NFIFunction.getcapturenames.createMessage();

        @Override
        public String[] execute(long code, long extra, int captureCount) {
            try {
                CaptureNamesResult data = new CaptureNamesResult(captureCount);
                int result = (int) ForeignAccess.sendExecute(message, NFIFunction.getcapturenames.getFunction(), data, code, extra);
                if (result < 0) {
                    CompilerDirectives.transferToInterpreter();
                    throw RError.error(RError.NO_CALLER, RError.Message.WRONG_PCRE_INFO, result);
                } else {
                    return data.getCaptureNames();
                }
            } catch (InteropException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }
    }

    public static class TruffleNFI_CompileNode extends CompileNode {

        @Child private Node message = NFIFunction.compile.createMessage();

        @Override
        public Result execute(String pattern, int options, long tables) {
            try {
                CompileResult data = new CompileResult();
                ForeignAccess.sendExecute(message, NFIFunction.compile.getFunction(), data, pattern, options, tables);
                return data.getResult();
            } catch (InteropException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }
    }

    private static class TruffleNFI_ExecNode extends ExecNode {
        @Child private Node message = NFIFunction.exec.createMessage();

        @Override
        public int execute(long code, long extra, String subject, int offset, int options, int[] ovector) {
            try {
                byte[] subjectBytes = subject.getBytes(StandardCharsets.UTF_8);
                return (int) ForeignAccess.sendExecute(message, NFIFunction.exec.getFunction(), code, extra,
                                JavaInterop.asTruffleObject(subjectBytes), subjectBytes.length,
                                offset, options, JavaInterop.asTruffleObject(ovector), ovector.length);
            } catch (InteropException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
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
