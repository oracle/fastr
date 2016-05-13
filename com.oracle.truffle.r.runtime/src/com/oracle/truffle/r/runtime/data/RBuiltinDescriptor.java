/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.data;

import java.util.Arrays;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.PrimitiveMethodsInfo;
import com.oracle.truffle.r.runtime.RBuiltinKind;
import com.oracle.truffle.r.runtime.RDispatch;
import com.oracle.truffle.r.runtime.RVisibility;

public abstract class RBuiltinDescriptor {

    private static int primitiveMethodCount;

    private final String name;
    private final String[] aliases;
    private final RBuiltinKind kind;
    private final ArgumentsSignature signature;
    private final int[] nonEvalArgs;
    private final boolean splitCaller;
    private final boolean alwaysSplit;
    private final RDispatch dispatch;
    private final int primitiveMethodIndex;
    private final RVisibility visibility;
    @CompilationFinal private final boolean[] evaluatesArgument;

    public RBuiltinDescriptor(String name, RVisibility visibility, String[] aliases, RBuiltinKind kind, ArgumentsSignature signature, int[] nonEvalArgs, boolean splitCaller, boolean alwaysSplit,
                    RDispatch dispatch) {
        this.name = name.intern();
        this.visibility = visibility;
        this.aliases = aliases;
        this.kind = kind;
        this.signature = signature;
        this.nonEvalArgs = nonEvalArgs;
        this.splitCaller = splitCaller;
        this.alwaysSplit = alwaysSplit;
        this.dispatch = dispatch;

        evaluatesArgument = new boolean[signature.getLength()];
        Arrays.fill(evaluatesArgument, true);
        for (int index : nonEvalArgs) {
            assert evaluatesArgument[index] : "duplicate nonEvalArgs entry " + index + " in " + this;
            evaluatesArgument[index] = false;
        }

        if (kind == RBuiltinKind.PRIMITIVE) {
            // TODO: assert that static count is only incremented in the primordial context (it's
            // currently tough to do as builtin descriptors seem to be created before the primordial
            // context is fully initialized but code inspection shows that the assertion holds)
            primitiveMethodIndex = primitiveMethodCount++;
        } else {
            primitiveMethodIndex = PrimitiveMethodsInfo.INVALID_INDEX;
        }
    }

    public String getName() {
        return name;
    }

    public String[] getAliases() {
        return aliases;
    }

    public RBuiltinKind getKind() {
        return kind;
    }

    public ArgumentsSignature getSignature() {
        return signature;
    }

    public int[] getNonEvalArgs() {
        return nonEvalArgs;
    }

    public boolean isAlwaysSplit() {
        return alwaysSplit;
    }

    public boolean isSplitCaller() {
        return splitCaller;
    }

    public RDispatch getDispatch() {
        return dispatch;
    }

    public boolean evaluatesArg(int index) {
        return evaluatesArgument[index];
    }

    public int getPrimMethodIndex() {
        return primitiveMethodIndex;
    }

    public RVisibility getVisibility() {
        return visibility;
    }
}
