/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.ffi.impl.jni;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.ffi.PCRERFFI;

public class JNI_PCRE implements PCRERFFI {
    private static class JNI_MaketablesNode extends Node implements MaketablesNode {
        @Override
        public long execute() {
            return nativeMaketables();
        }
    }

    private static class JNI_CompileNode extends Node implements CompileNode {
        @Override
        public Result execute(String pattern, int options, long tables) {
            return nativeCompile(pattern, options, tables);
        }
    }

    private static class JNI_GetCaptureCountNode extends Node implements GetCaptureCountNode {
        @Override
        public int execute(long code, long extra) {
            int res = nativeGetCaptureCount(code, extra);
            if (res < 0) {
                CompilerDirectives.transferToInterpreter();
                throw RError.error(RError.NO_CALLER, RError.Message.WRONG_PCRE_INFO, res);
            }
            return res;
        }
    }

    private static class JNI_GetCaptureNamesNode extends Node implements GetCaptureNamesNode {
        @Override
        public String[] execute(long code, long extra, int captureCount) {
            String[] ret = new String[captureCount];
            int res = nativeGetCaptureNames(code, extra, ret);
            if (res < 0) {
                CompilerDirectives.transferToInterpreter();
                throw RError.error(RError.NO_CALLER, RError.Message.WRONG_PCRE_INFO, res);
            }
            return ret;
        }
    }

    private static class JNI_StudyNode extends Node implements StudyNode {
        @Override
        public Result execute(long code, int options) {
            throw RInternalError.unimplemented("pcre_study");
        }
    }

    private static class JNI_ExecNode extends Node implements ExecNode {
        @Override
        public int execute(long code, long extra, String subject, int offset, int options, int[] ovector) {
            return nativeExec(code, extra, subject, offset, options, ovector, ovector.length);
        }
    }

    private static native long nativeMaketables();

    private static native Result nativeCompile(String pattern, int options, long tables);

    private static native int nativeGetCaptureCount(long code, long extra);

    private static native int nativeGetCaptureNames(long code, long extra, String[] res);

    private static native int nativeExec(long code, long extra, String subject, int offset,
                    int options, int[] ovector, int ovectorLen);

    @Override
    public MaketablesNode createMaketablesNode() {
        return new JNI_MaketablesNode();
    }

    @Override
    public CompileNode createCompileNode() {
        return new JNI_CompileNode();
    }

    @Override
    public GetCaptureCountNode createGetCaptureCountNode() {
        return new JNI_GetCaptureCountNode();
    }

    @Override
    public GetCaptureNamesNode createGetCaptureNamesNode() {
        return new JNI_GetCaptureNamesNode();
    }

    @Override
    public StudyNode createStudyNode() {
        return new JNI_StudyNode();
    }

    @Override
    public ExecNode createExecNode() {
        return new JNI_ExecNode();
    }
}
