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
package com.oracle.truffle.r.runtime.ffi;

import com.oracle.truffle.r.runtime.RInternalError;

/**
 * Enumerates all the C functions that are internal to the implementation and called via Truffle
 * NFI. Some of the functions are called directly, but most are called via a wrapper, typically to
 * enable a callback with a complex result or, in the case of Fortran, to handle call by reference
 * conveniently. The wrapper functions names are all of the form {@code call_xxx_function}, where
 * {@code xxx} is the subsystem.
 */
public enum NativeFunction {
    // base
    getpid("(): sint32", "call_base_"),
    getcwd("([uint8], sint32): sint32", "call_base_"),
    chdir("(string): sint32", "call_base_"),
    mkdir("(string, sint32): sint32", "call_base_"),
    readlink("((string, sint32): void, string): void", "call_base_"),
    mkdtemp("([uint8]): sint32", "call_base_"),
    chmod("(string, sint32): sint32", "call_base_"),
    strtol("((sint64, sint32): void, string, sint32): void", "call_base_"),
    uname("((string, string, string, string, string): void): void", "call_base_"),
    glob("((string): void, string): void", "call_base_"),
    // PCRE
    maketables("(): sint64", "call_pcre_"),
    compile("((uint64, string, sint32): void, string, sint32, uint64): void", "call_pcre_"),
    getcapturecount("(uint64, uint64): sint32", "call_pcre_"),
    getcapturenames("((sint32, string): void, uint64, uint64): sint32", "call_pcre_"),
    study("(uint64, sint32): void", "call_pcre_"),
    exec("(uint64, uint64, [uint8], sint32, sint32, sint32, [sint32], sint32): sint32", "call_pcre_"),
    // zip
    compress("([uint8], uint64, [uint8], uint64): sint32", "call_zip_"),
    uncompress("([uint8], uint64, [uint8], uint64): sint32", "call_zip_"),
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
    dgesdd("(uint8, sint32, sint32, [double], sint32, [double], [double], sint32, [double], sint32, [double], sint32, [sint32]) : sint32", "call_lapack_"),
    dlange("(uint8, sint32, sint32, [double], sint32, [double]) : double", "call_lapack_"),
    dgecon("(uint8, sint32, [double], sint32, double, [double], [double], [sint32]) : sint32", "call_lapack_"),
    dsyevr(
                    "(uint8, uint8, uint8, sint32, [double], sint32, double, double, sint32, sint32, double, [sint32], [double], [double], sint32, [sint32], [double], sint32, [sint32], sint32) : sint32",
                    "call_lapack_"),
    // misc
    exactSumFunc("([double], sint32, sint32, sint32): double", "call_misc_"),
    dqrls("([double], sint32, sint32, [double], sint32, double, [double], [double], [double], [sint32], [sint32], [double], [double]): void", "call_misc_"),
    // stats
    fft_factor("(sint32, [sint32], [sint32]): void", "", "stats"),
    fft_work("([double], sint32, sint32, sint32, sint32, [double], [sint32]): sint32", "", "stats"),
    lminfl("([double], sint32, sint32, sint32, sint32, [double], [double], [double], [double], [double], double): void", "call_stats_", "stats"),
    // FastR helpers
    set_exception_flag("(): void"),
    // user-defined RNG
    unif_init("(sint32): void", "user_", anyLibrary()),
    norm_rand("(): pointer", "user_", anyLibrary()),
    unif_rand("(): pointer", "user_", anyLibrary()),
    unif_nseed("(): pointer", "user_", anyLibrary()),
    unif_seedloc("(): pointer", "user_", anyLibrary()),
    // memory access helper functions
    read_pointer_int("(pointer): sint32", "caccess_"),
    read_array_int("(pointer, sint64): sint32", "caccess_"),
    read_pointer_double("(pointer): double", "caccess_"),
    read_array_double("(pointer, sint32): double", "caccess_"),
    // initialization helpers
    Rdynload_setSymbol("(pointer, sint32, pointer, sint32): pointer"),
    initvar_obj("(env, sint32, pointer) : void", "Call_"),
    initvar_double("(sint32, double): void", "Call_"),
    initvar_string("(sint32, string): void", "Call_"),
    initvar_int("(sint32, sint32) : void", "Call_");

    private final String callName;
    private final String signature;
    private final int argumentCount;
    private final String library;

    NativeFunction(String signature, String prefix, String library) {
        this.callName = prefix + name();
        this.signature = signature;
        this.argumentCount = getArgCount(signature);
        this.library = library;
    }

    NativeFunction(String signature, String prefix) {
        this(signature, prefix, baseLibrary());
    }

    NativeFunction(String signature) {
        this(signature, "", baseLibrary());
    }

    public String getCallName() {
        return callName;
    }

    public String getSignature() {
        return signature;
    }

    public int getArgumentCount() {
        return argumentCount;
    }

    public String getLibrary() {
        return library;
    }

    // Functions because the enum constants cannot refer to static final fields:

    public static String anyLibrary() {
        return "any";
    }

    public static String baseLibrary() {
        return "base";
    }

    /**
     * Returns the number of arguments in an NFI signature.
     */
    public static int getArgCount(String signature) {
        int argCount = 0;
        int nestCount = -1;
        boolean type = false;
        for (int i = 0; i < signature.length(); i++) {
            char ch = signature.charAt(i);
            if (ch == '(') {
                nestCount++;
            } else if (ch == ')') {
                if (nestCount > 0) {
                    nestCount--;
                } else {
                    return type ? argCount + 1 : 0;
                }
            } else if (ch == ',') {
                if (nestCount == 0) {
                    argCount++;
                }
            } else {
                type = true;
            }
        }
        throw RInternalError.shouldNotReachHere();
    }
}
