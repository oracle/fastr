/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.test;

import java.util.HashSet;
import java.util.Set;

import org.junit.runner.Description;

import com.oracle.truffle.r.runtime.context.RContext.ContextKind;

public class TestContextConfig {
    public static final Set<String> BLACKLIST = new HashSet<>();

    private static final boolean IS_LLVM_ENV = "llvm".equals(System.getenv().get("FASTR_RFFI"));

    static {
        BLACKLIST.add("testMisc(com.oracle.truffle.r.test.library.base.TestSimpleAssignment)");
        BLACKLIST.add("testError(com.oracle.truffle.r.test.library.base.TestSimpleErrorHandling)");
        BLACKLIST.add("testLookup(com.oracle.truffle.r.test.library.base.TestEnvironments)");
        BLACKLIST.add("testEnvironment(com.oracle.truffle.r.test.library.base.TestEnvironments)");
        BLACKLIST.add("testScalarUpdate(com.oracle.truffle.r.test.library.base.TestSimpleVectors)");
        BLACKLIST.add("testObject(com.oracle.truffle.r.test.library.base.TestSimpleVectors)");
        BLACKLIST.add("testSimple(com.oracle.truffle.r.test.library.utils.TestInteractiveDebug)");
        BLACKLIST.add("testNestedDebugging(com.oracle.truffle.r.test.library.utils.TestInteractiveDebug)");
        BLACKLIST.add("testSetBreakpoint(com.oracle.truffle.r.test.library.utils.TestInteractiveDebug)");
        BLACKLIST.add("testLoop(com.oracle.truffle.r.test.library.utils.TestInteractiveDebug)");
        BLACKLIST.add("testConditionalBreakpoint(com.oracle.truffle.r.test.library.utils.TestInteractiveDebug)");
        BLACKLIST.add("testConditionalBreakpoint(com.oracle.truffle.r.test.library.utils.TestInteractiveDebug)");
        BLACKLIST.add("testInvalidName(com.oracle.truffle.r.test.library.utils.TestInteractiveDebug)");
        BLACKLIST.add("testmatchfun(com.oracle.truffle.r.test.builtins.TestBuiltin_matchfun)");
        BLACKLIST.add("basicTests(com.oracle.truffle.r.test.builtins.TestBuiltin_attach)");
        BLACKLIST.add("testGenericDispatch(com.oracle.truffle.r.test.builtins.TestBuiltin_rbind)");
        BLACKLIST.add("testWithNonStandardLength(com.oracle.truffle.r.test.builtins.TestBuiltin_seq_along)");
        BLACKLIST.add("testEnvVars(com.oracle.truffle.r.test.builtins.TestBuiltin_Sysgetenv)");
        BLACKLIST.add("testInternalDispatch(com.oracle.truffle.r.test.S4.TestS4)");
        BLACKLIST.add("testActiveBindings(com.oracle.truffle.r.test.S4.TestS4)");
        BLACKLIST.add("testc14(com.oracle.truffle.r.test.builtins.TestBuiltin_c)");
        BLACKLIST.add("testGenericDispatch(com.oracle.truffle.r.test.builtins.TestBuiltin_cbind)");
        BLACKLIST.add("testDoCall(com.oracle.truffle.r.test.builtins.TestBuiltin_docall)");
        BLACKLIST.add("testEval(com.oracle.truffle.r.test.builtins.TestBuiltin_eval)");
        BLACKLIST.add("testLapply(com.oracle.truffle.r.test.builtins.TestBuiltin_lapply)");
        BLACKLIST.add("testMGet(com.oracle.truffle.r.test.builtins.TestBuiltin_mget)");
        BLACKLIST.add("testArgsCast(com.oracle.truffle.r.test.builtins.TestBuiltin_RNGkind)");
        BLACKLIST.add("testwarning(com.oracle.truffle.r.test.builtins.TestBuiltin_warning)");
        BLACKLIST.add("runRSourceTests(com.oracle.truffle.r.test.library.fastr.TestChannels)");
        BLACKLIST.add("testCasts(com.oracle.truffle.r.test.builtins.TestMiscBuiltins)");
    }

    public static ContextKind getTestContextKind() {
        return IS_LLVM_ENV ? ContextKind.SHARE_NOTHING : ContextKind.SHARE_PARENT_RW;
    }

    public static boolean isTestMethodContext(Description testDesc) {
        return IS_LLVM_ENV && !BLACKLIST.contains(testDesc.getDisplayName());
    }
}
