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
package com.oracle.truffle.r.engine.interop.ffi.nfi;

import java.nio.charset.StandardCharsets;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.java.JavaInterop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.data.RTruffleObject;
import com.oracle.truffle.r.runtime.ffi.PCRERFFI;

public class TruffleNFI_PCRE implements PCRERFFI {
    private enum Function {
        maketables("(): sint64", true),
        compile("((uint64, string, sint32): void, string, sint32, uint64): void", false),
        getcapturecount("(uint64, uint64): sint32", false),
        getcapturenames("((sint32, string): void, uint64, uint64): sint32", false),
        study("(uint64, sint32): void", false),
        exec("(uint64, uint64, [uint8], sint32, sint32, sint32, [sint32], sint32): sint32", true);

        private final int argCount;
        private final String signature;
        private final String callName;
        private Node executeNode;
        private TruffleObject function;

        Function(String signature, boolean direct) {
            this.argCount = TruffleNFI_Utils.getArgCount(signature);
            this.signature = signature;
            this.callName = (direct ? "pcre_" : "call_") + name();
        }

        private void initialize() {
            if (executeNode == null) {
                executeNode = Message.createExecute(argCount).createNode();
            }
            if (function == null) {
                function = TruffleNFI_Utils.lookupAndBind(callName, false, signature);
            }
        }
    }

    private static class TruffleNFI_MaketablesNode extends MaketablesNode {

        @Override
        public long execute() {
            Function.maketables.initialize();
            try {
                long result = (long) ForeignAccess.sendExecute(Function.maketables.executeNode, Function.maketables.function);
                return result;
            } catch (InteropException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }

    }

    private static class TruffleNFI_GetCaptureCountNode extends GetCaptureCountNode {

        @Override
        public int execute(long code, long extra) {
            Function.getcapturecount.initialize();
            try {
                int result = (int) ForeignAccess.sendExecute(Function.getcapturecount.executeNode, Function.getcapturecount.function, code, extra);
                return result;
            } catch (InteropException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }
    }

    public static class TruffleNFI_GetCaptureNamesNode extends GetCaptureNamesNode {
        interface CaptureNames {
            void addName(int i, String name);
        }

        public static final class CaptureNamesImpl implements CaptureNames, RTruffleObject {
            private final String[] captureNames;

            private CaptureNamesImpl(int captureCount) {
                captureNames = new String[captureCount];
            }

            @Override
            public void addName(int i, String name) {
                captureNames[i] = name;
            }

        }

        @Override
        public String[] execute(long code, long extra, int captureCount) {
            Function.getcapturenames.initialize();
            try {
                CaptureNamesImpl captureNamesImpl = new CaptureNamesImpl(captureCount);
                int result = (int) ForeignAccess.sendExecute(Function.getcapturenames.executeNode, Function.getcapturenames.function,
                                captureNamesImpl, code, extra);
                if (result < 0) {
                    CompilerDirectives.transferToInterpreter();
                    throw RError.error(RError.NO_CALLER, RError.Message.WRONG_PCRE_INFO, result);
                } else {
                    return captureNamesImpl.captureNames;
                }
            } catch (InteropException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }

    }

    public static class TruffleNFI_CompileNode extends CompileNode {
        interface MakeResult {
            void makeresult(long pcreResult, String errorMessage, int errOffset);
        }

        public static class MakeResultImpl implements MakeResult, RTruffleObject {
            private PCRERFFI.Result result;

            @Override
            public void makeresult(long pcreResult, String errorMessage, int errOffset) {
                result = new PCRERFFI.Result(pcreResult, errorMessage, errOffset);
            }
        }

        @Override
        public Result execute(String pattern, int options, long tables) {
            Function.compile.initialize();
            try {
                MakeResultImpl makeResultImpl = new MakeResultImpl();
                ForeignAccess.sendExecute(Function.compile.executeNode, Function.compile.function, makeResultImpl,
                                pattern, options, tables);
                return makeResultImpl.result;
            } catch (InteropException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }

    }

    private static class TruffleNFI_ExecNode extends ExecNode {

        @Override
        public int execute(long code, long extra, String subject, int offset, int options, int[] ovector) {
            Function.exec.initialize();
            try {

                byte[] subjectBytes = subject.getBytes(StandardCharsets.UTF_8);
                return (int) ForeignAccess.sendExecute(Function.exec.executeNode, Function.exec.function, code, extra,
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
