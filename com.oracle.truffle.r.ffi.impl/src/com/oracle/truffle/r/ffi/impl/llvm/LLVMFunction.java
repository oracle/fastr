/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.ffi.impl.common.NativeFunction;
import com.oracle.truffle.r.runtime.ffi.DLL;
import com.oracle.truffle.r.runtime.ffi.DLL.SymbolHandle;

/**
 * Enumerates all the C functions that are internal to the implementation and called via Truffle
 * LLVM. All of the functions are called indirectly via a wrapper, typically to enable a callback
 * with a complex result or, in the case of Fortran, to handle call by reference conveniently, or to
 * just have LLVM handle the underlying native call. The wrapper functions names are all of the form
 * {@code call_xxx_function}, where {@code xxx} is the subsystem.
 */
public enum LLVMFunction implements NativeFunction {
    // base
    getpid(0, "call_base_"),
    getwd(2, "call_base_"),
    setwd(1, "call_base_"),
    mkdir(2, "call_base_"),
    readlink(2, "call_base_"),
    mkdtemp(1, "call_base_"),
    chmod(2, "call_base_"),
    strtol(3, "call_base_"),
    uname(1, "call_base_"),
    glob(2, "call_base_"),
    // PCRE
    maketables(0, "call_pcre_"),
    compile(4, "call_pcre_"),
    getcapturecount(2, "call_pcre_"),
    getcapturenames(3, "call_pcre_"),
    study(2, "call_pcre_"),
    exec(8, "call_pcre_"),
    // RAppl
    dqrdc2(9, "call_appl_"),
    dqrcf(8, "call_appl_"),
    dqrls(13, "call_appl_"),
    // zip
    compress(4, "call_zip_"),
    uncompress(4, "call_zip_"),
    // lapack
    ilaver(1, "call_lapack_"),
    dgeev(13, "call_lapack_"),
    dgeqp3(8, "call_lapack_"),
    dormq(12, "call_lapack_"),
    dtrtrs(9, "call_lapack_"),
    dgetrf(5, "call_lapack_"),
    dpotrf(4, "call_lapack_"),
    dpotri(4, "call_lapack_"),
    dpstrf(9, "call_lapack_"),
    dgesv(7, "call_lapack_"),
    dlange(6, "call_lapack_"),
    dgecon(8, "call_lapack_"),
    dsyevr(20, "call_lapack_"),
    // misc
    exactSumFunc(4, "");

    private final int argumentCount;
    private final String callName;

    LLVMFunction(int argCount, String prefix) {
        this.argumentCount = argCount;
        this.callName = prefix + name();
    }

    Node createMessage() {
        CompilerAsserts.neverPartOfCompilation();
        return Message.createExecute(argumentCount).createNode();
    }

    SymbolHandle createSymbol() {
        return DLL.findSymbol(callName, null);
    }

    @Override
    public int getArgumentCount() {
        return argumentCount;
    }
}
