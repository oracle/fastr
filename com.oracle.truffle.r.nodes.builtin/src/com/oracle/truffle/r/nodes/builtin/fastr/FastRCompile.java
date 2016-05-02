/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.fastr;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.Future;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.RBuiltinKind;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RMissing;

@RBuiltin(name = ".fastr.compile", kind = RBuiltinKind.PRIMITIVE, parameterNames = {"func", "background"})
public abstract class FastRCompile extends RBuiltinNode {

    private static final class Compiler {
        private final Class<?> optimizedCallTarget;
        private final Class<?> graalTruffleRuntime;
        private final Method submitForCompilationMethod;
        private final Method finishCompilationMethod;

        private Compiler() {
            try {
                optimizedCallTarget = Class.forName("com.oracle.graal.truffle.OptimizedCallTarget", false, Truffle.getRuntime().getClass().getClassLoader());
                graalTruffleRuntime = Class.forName("com.oracle.graal.truffle.GraalTruffleRuntime", false, Truffle.getRuntime().getClass().getClassLoader());
                submitForCompilationMethod = graalTruffleRuntime.getDeclaredMethod("submitForCompilation", optimizedCallTarget);
                finishCompilationMethod = graalTruffleRuntime.getDeclaredMethod("finishCompilation", optimizedCallTarget, Future.class, boolean.class);
            } catch (ClassNotFoundException | IllegalArgumentException | NoSuchMethodException | SecurityException e) {
                throw Utils.fail("fastr.compile: failed to find 'compile' method");
            }
        }

        static Compiler getCompiler() {
            if (System.getProperty("fastr.truffle.compile", "true").equals("true") && Truffle.getRuntime().getName().contains("Graal")) {
                return new Compiler();
            } else {
                return null;
            }
        }

        boolean compile(CallTarget callTarget, boolean background) throws InvocationTargetException, IllegalAccessException {
            if (optimizedCallTarget.isInstance(callTarget)) {
                Future<?> submitted = (Future<?>) submitForCompilationMethod.invoke(Truffle.getRuntime(), callTarget);
                finishCompilationMethod.invoke(Truffle.getRuntime(), callTarget, submitted, background);
                return true;
            } else {
                return false;
            }
        }
    }

    private static final Compiler compiler = Compiler.getCompiler();

    @Override
    public Object[] getDefaultParameterValues() {
        return new Object[]{RMissing.instance, RRuntime.LOGICAL_FALSE};
    }

    @Specialization
    protected byte compileFunction(RFunction function, byte background) {
        controlVisibility();
        if (compiler != null) {
            try {
                if (compiler.compile(function.getTarget(), background == RRuntime.LOGICAL_TRUE)) {
                    return RRuntime.LOGICAL_TRUE;
                }
            } catch (InvocationTargetException | IllegalAccessException e) {
                throw RError.error(this, RError.Message.GENERIC, e.toString());
            }
        } else {
            throw RError.error(this, RError.Message.GENERIC, "fastr.compile not supported in this environment");
        }
        return RRuntime.LOGICAL_FALSE;
    }

    @SuppressWarnings("unused")
    @Fallback
    protected Object fallback(Object a1, Object a2) {
        throw RError.error(this, RError.Message.INVALID_OR_UNIMPLEMENTED_ARGUMENTS);
    }
}
