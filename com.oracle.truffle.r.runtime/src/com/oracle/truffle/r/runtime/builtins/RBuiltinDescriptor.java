/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.builtins;

import java.util.Arrays;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.PrimitiveMethodsInfo;
import com.oracle.truffle.r.runtime.RDispatch;
import com.oracle.truffle.r.runtime.RVisibility;
import com.oracle.truffle.r.runtime.Utils;

public abstract class RBuiltinDescriptor {

    private static int primitiveMethodCount;

    private final String name;
    private final Class<?> builtinMetaClass;
    private final Class<?> builtinNodeClass;
    private final RVisibility visibility;
    private final String[] aliases;
    private final RBuiltinKind kind;
    private final ArgumentsSignature signature;
    private final int[] nonEvalArgs;
    private final boolean allowMissingInVarArgs;
    private final boolean splitCaller;
    private final boolean alwaysSplit;
    private final RDispatch dispatch;
    private final String genericName;
    private final RBehavior behavior;
    private final boolean lookupVarArgs;
    private final RSpecialFactory specialCall;

    private final int primitiveMethodIndex;
    @CompilationFinal(dimensions = 1) private final boolean[] evaluatesArgument;
    private final boolean isFieldAccess;

    public RBuiltinDescriptor(RBuiltin annotation, Class<?> builtinMetaClass, Class<?> builtinNodeClass, ArgumentsSignature signature, RSpecialFactory specialCall) {
        // Note: we use own final fields to avoid using the RBuiltin interface
        this.lookupVarArgs = annotation.lookupVarArgs();
        this.specialCall = specialCall;
        this.name = Utils.intern(annotation.name());
        this.builtinMetaClass = builtinMetaClass;
        this.builtinNodeClass = builtinNodeClass;
        this.visibility = annotation.visibility();
        this.aliases = annotation.aliases();
        this.kind = annotation.kind();
        this.signature = signature;
        this.nonEvalArgs = annotation.nonEvalArgs();
        this.allowMissingInVarArgs = annotation.allowMissingInVarArgs();
        this.splitCaller = annotation.splitCaller();
        this.alwaysSplit = annotation.alwaysSplit();
        this.dispatch = annotation.dispatch();
        this.genericName = Utils.intern(annotation.genericName());
        this.behavior = annotation.behavior();
        this.isFieldAccess = annotation.isFieldAccess();

        evaluatesArgument = new boolean[signature.getLength()];
        Arrays.fill(evaluatesArgument, true);
        for (int index : nonEvalArgs) {
            assert evaluatesArgument[index] : "duplicate nonEvalArgs entry " + index + " in " + this;
            evaluatesArgument[index] = false;
        }

        if (kind == RBuiltinKind.PRIMITIVE || (kind == RBuiltinKind.INTERNAL && dispatch == RDispatch.INTERNAL_GENERIC)) {
            // TODO: assert that static count is only incremented in the primordial context (it's
            // currently tough to do as builtin descriptors seem to be created before the primordial
            // context is fully initialized but code inspection shows that the assertion holds)
            primitiveMethodIndex = primitiveMethodCount++;
        } else {
            primitiveMethodIndex = PrimitiveMethodsInfo.INVALID_INDEX;
        }
    }

    public final String getName() {
        return name;
    }

    public final String getGenericName() {
        if (genericName.isEmpty()) {
            return name;
        } else {
            return genericName;
        }
    }

    public final String[] getAliases() {
        return aliases;
    }

    public final RBuiltinKind getKind() {
        return kind;
    }

    public final ArgumentsSignature getSignature() {
        return signature;
    }

    public final boolean isAlwaysSplit() {
        return alwaysSplit;
    }

    public final boolean isSplitCaller() {
        return splitCaller;
    }

    public final RDispatch getDispatch() {
        return dispatch;
    }

    public final boolean evaluatesArg(int index) {
        return evaluatesArgument[index];
    }

    public final boolean allowMissingInVarArgs() {
        return allowMissingInVarArgs;
    }

    public final int getPrimMethodIndex() {
        return primitiveMethodIndex;
    }

    public final RVisibility getVisibility() {
        return visibility;
    }

    public final Class<?> getBuiltinMetaClass() {
        return builtinMetaClass;
    }

    public final Class<?> getBuiltinNodeClass() {
        return builtinNodeClass;
    }

    public final RBehavior getBehavior() {
        return behavior;
    }

    public final RSpecialFactory getSpecialCall() {
        return specialCall;
    }

    @Override
    public final String toString() {
        return "RBuiltinFactory [name=" + getName() + ", aliases=" + Arrays.toString(getAliases()) + ", kind=" + getKind() + ", siagnature=" + getSignature() + ", nonEvaledArgs=" +
                        Arrays.toString(nonEvalArgs) + ", splitCaller=" + isSplitCaller() + ", dispatch=" + getDispatch() + ", behavior=" + getBehavior() + "]";
    }

    public final boolean isFieldAccess() {
        return isFieldAccess;
    }

    public final boolean lookupVarArgs() {
        return lookupVarArgs;
    }

    public static boolean lookupVarArgs(RBuiltinDescriptor builtin) {
        return builtin == null || builtin.lookupVarArgs;
    }
}
