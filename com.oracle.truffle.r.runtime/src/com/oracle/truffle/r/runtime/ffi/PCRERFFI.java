/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.ffi;

import java.nio.charset.StandardCharsets;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.ffi.interop.NativeCharArray;
import com.oracle.truffle.r.runtime.ffi.interop.pcre.CaptureNamesResult;
import com.oracle.truffle.r.runtime.ffi.interop.pcre.CompileResult;

/**
 * An interface to the <a href="http://www.pcre.org/original/doc/html/index.html">PCRE</a> library
 * for Perl regular expressions.
 */
public final class PCRERFFI {
    public static final int NOTBOL = 0x00000080;
    public static final int CASELESS = 0x1;

    private final DownCallNodeFactory downCallNodeFactory;

    public PCRERFFI(DownCallNodeFactory downCallNodeFactory) {
        this.downCallNodeFactory = downCallNodeFactory;
    }

    /**
     * PCRE uses call by reference for error-related information, which we encapsulate and sanitize
     * in this class. The {@code result} value (which is typically an opaque pointer to an internal
     * C struct), is the actual result of the function as per the PCRE spec.
     */
    public static final class Result {
        public final long result;
        public final String errorMessage;
        public final int errOffset;

        public Result(long result, String errorMessage, int errOffset) {
            this.result = result;
            this.errorMessage = errorMessage;
            this.errOffset = errOffset;
        }
    }

    public static final class MaketablesNode extends NativeCallNode {
        @Child private InteropLibrary interop;

        private MaketablesNode(DownCallNodeFactory factory) {
            super(factory.createDownCallNode(NativeFunction.maketables));
        }

        public long execute() {
            Object result = call();
            if (result instanceof Long) {
                return (long) result;
            }
            assert result instanceof TruffleObject;
            if (interop == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                interop = insert(InteropLibrary.getFactory().createDispatched(DSLConfig.getInteropLibraryCacheSize()));
            }
            try {
                return interop.asPointer(result);
            } catch (UnsupportedMessageException e) {
                throw RInternalError.shouldNotReachHere("PCRE function maketables should return long or TruffleObject that represents a pointer.");
            }
        }

        public static MaketablesNode create() {
            return RFFIFactory.getPCRERFFI().createMaketablesNode();
        }
    }

    public static final class CompileNode extends NativeCallNode {
        private CompileNode(DownCallNodeFactory factory) {
            super(factory.createDownCallNode(NativeFunction.compile));
        }

        public Result execute(String pattern, int options, long tables) {
            CompileResult data = new CompileResult();
            call(data, pattern, options, tables);
            return data.getResult();
        }

        public static CompileNode create() {
            return RFFIFactory.getPCRERFFI().createCompileNode();
        }
    }

    public static final class GetCaptureCountNode extends NativeCallNode {
        private GetCaptureCountNode(DownCallNodeFactory factory) {
            super(factory.createDownCallNode(NativeFunction.getcapturecount));
        }

        public int execute(long code, long extra) {
            return (int) call(code, extra);
        }

        public static GetCaptureCountNode create() {
            return RFFIFactory.getPCRERFFI().createGetCaptureCountNode();
        }
    }

    public static final class GetCaptureNamesNode extends NativeCallNode {
        private GetCaptureNamesNode(DownCallNodeFactory factory) {
            super(factory.createDownCallNode(NativeFunction.getcapturenames));
        }

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

        public static GetCaptureNamesNode create() {
            return RFFIFactory.getPCRERFFI().createGetCaptureNamesNode();
        }
    }

    public static final class StudyNode extends NativeCallNode {
        private StudyNode(DownCallNodeFactory factory) {
            super(factory.createDownCallNode(NativeFunction.study));
        }

        @SuppressWarnings("unused")
        public static Result execute(long code, int options) {
            throw RInternalError.shouldNotReachHere("The factory method should throw unimplemented already");
        }

        public static StudyNode create() {
            return RFFIFactory.getPCRERFFI().createStudyNode();
        }
    }

    public static final class ExecNode extends NativeCallNode {
        private ExecNode(DownCallNodeFactory factory) {
            super(factory.createDownCallNode(NativeFunction.exec));
        }

        public int execute(long code, long extra, String subject, int offset, int options, int[] ovector) {
            byte[] subjectBytes = getBytes(subject);
            NativeCharArray subjectChars = new NativeCharArray(subjectBytes);
            return (int) call(code, extra, subjectChars, subjectBytes.length, offset, options, ovector, ovector.length);
        }

        @TruffleBoundary
        private static byte[] getBytes(String subject) {
            return subject.getBytes(StandardCharsets.UTF_8);
        }

        public static ExecNode create() {
            return RFFIFactory.getPCRERFFI().createExecNode();
        }
    }

    public MaketablesNode createMaketablesNode() {
        return new MaketablesNode(downCallNodeFactory);
    }

    public CompileNode createCompileNode() {
        return new CompileNode(downCallNodeFactory);
    }

    public GetCaptureCountNode createGetCaptureCountNode() {
        return new GetCaptureCountNode(downCallNodeFactory);
    }

    public GetCaptureNamesNode createGetCaptureNamesNode() {
        return new GetCaptureNamesNode(downCallNodeFactory);
    }

    @SuppressWarnings("static-method")
    public StudyNode createStudyNode() {
        throw RInternalError.unimplemented("study function in PCRE");
    }

    public ExecNode createExecNode() {
        return new ExecNode(downCallNodeFactory);
    }
}
