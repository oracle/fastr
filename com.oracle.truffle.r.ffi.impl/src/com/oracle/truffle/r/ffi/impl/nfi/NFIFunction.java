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
package com.oracle.truffle.r.ffi.impl.nfi;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;

/**
 * Enumerates all the C functions that are internal to the implementation and called via Truffle
 * NFI. Some of the functions are called directly, but most are called via a wrapper, typically to
 * enable a callback with a complex result or, in the case of Fortran, to handle call by reference
 * conveniently. The wrapper functions names are all of the form {@code call_xxx_function}, where
 * {@code xxx} is the subsystem.
 *
 */
enum NFIFunction {
    // base
    getpid("(): sint32"),
    getcwd("([uint8], sint32): sint32"),
    chdir("(string): sint32"),
    mkdir("(string, sint32): sint32"),
    readlink("((string, sint32): void, string): void", "call_base_"),
    mkdtemp("([uint8]): sint32"),
    chmod("(string, sint32): sint32"),
    strtol("((sint64, sint32): void, string, sint32): void", "call_base_"),
    uname("((string, string, string, string, string): void): void", "call_base_"),
    glob("((string): void, string): void", "call_base_"),
    // PCRE, N.B. The "pcre_" prefixes are actually direct calls
    maketables("(): sint64", "pcre_"),
    compile("((uint64, string, sint32): void, string, sint32, uint64): void", "call_pcre_"),
    getcapturecount("(uint64, uint64): sint32", "call_pcre_"),
    getcapturenames("((sint32, string): void, uint64, uint64): sint32", "call_pcre_"),
    study("(uint64, sint32): void", "call_pcre_"),
    exec("(uint64, uint64, [uint8], sint32, sint32, sint32, [sint32], sint32): sint32", "pcre_"),
    // RAppl
    dqrdc2("([double], sint32, sint32, sint32, double, [sint32], [double], [sint32], [double]): void", "call_appl_"),
    dqrcf("([double], sint32, sint32, [double], [double], sint32, [double], [sint32]): void", "call_appl_"),
    dqrls("([double], sint32, sint32, [double], sint32, double, [double], [double], [double], [sint32], [sint32], [double], [double]): void", "call_appl_"),
    // zip
    compress("([uint8], [uint64], [uint8], uint64): sint32"),
    uncompress("([uint8], [uint64], [uint8], uint64): sint32"),
    // lapack
    ilaver("([sint32]): void", "call_lapack_"),
    dgeev("(uint8, uint8, sint32, [double], sint32, [double], [double], [double], sint32, [double], sint32, [double], sint32) : sint32", "call_lapack_"),
    dgeqp3("(sint32, sint32, [double], sint32, [sint32], [double], [double], sint32) : sint32", "call_lapack_"),
    dormq("(uint8, uint8, sint32, sint32, sint32, [double], sint32, [double], [double], sint32, [double], sint32) : sint32", "call_lapack_"),
    dtrtrs("(uint8, uint8, uint8, sint32, sint32, [double], sint32, [double], sint32) : sint32", "call_lapack_"),
    dgetrf("(sint32, sint32, [double], sint32, [sint32]) : sint32", "call_lapack_"),
    dpotrf("(uint8, sint32, [double], sint32) : sint32", "call_lapack_"),
    dpotri("(uint8, sint32, [double], sint32) : sint32", "call_lapack_"),
    dpstrf("uint8, sint32, [double], sint32, [sint32], [sint32], double, [double]) : sint32", "call_lapack_"),
    dgesv("(sint32, sint32, [double], sint32, [sint32], [double], sint32) : sint32", "call_lapack_"),
    dlange("(uint8, sint32, sint32, [double], sint32, [double]) : double", "call_lapack_"),
    dgecon("(uint8, sint32, [double], sint32, double, [double], [double], [sint32]) : sint32", "call_lapack_"),
    //@formatter:off
    dsyevr("(uint8, uint8, uint8, sint32, [double], sint32, double, double, sint32, sint32, double, [sint32], [double], [double], sint32, [sint32], [double], sint32, " +
                    "[sint32], sint32) : sint32", "call_lapack_");
    //@formatter:on

    private final int argCount;
    private final String signature;
    private final String callName;
    @CompilationFinal private TruffleObject function;

    NFIFunction(String signature) {
        this.argCount = TruffleNFI_Utils.getArgCount(signature);
        this.signature = signature;
        this.callName = name();
    }

    NFIFunction(String signature, String prefix) {
        this.argCount = TruffleNFI_Utils.getArgCount(signature);
        this.signature = signature;
        this.callName = prefix + name();
    }

    Node createMessage() {
        CompilerAsserts.neverPartOfCompilation();
        return Message.createExecute(argCount).createNode();
    }

    TruffleObject getFunction() {
        if (function == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            function = TruffleNFI_Utils.lookupAndBind(callName, signature);
        }
        return function;
    }
}
