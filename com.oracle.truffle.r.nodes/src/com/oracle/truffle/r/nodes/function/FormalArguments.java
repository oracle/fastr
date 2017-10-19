/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.function;

import java.util.Arrays;
import java.util.IdentityHashMap;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.r.nodes.access.ConstantNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.Arguments;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.data.Closure;
import com.oracle.truffle.r.runtime.data.ClosureCache;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.nodes.RNode;

/**
 * This class denotes a list of formal arguments which consist of the tuple
 * <ul>
 * <li>argument name (part of the signature)</li>
 * <li>expression ({@link RNode}, {@link #getDefaultArgument(int)})</li>
 * </ul>
 * The order is always the one defined by the function definition.
 * <p>
 * It also acts as {@link ClosureCache} for it's default arguments, so there is effectively only
 * ever one {@link RootCallTarget} for every default argument.
 * </p>
 */
public final class FormalArguments extends Arguments<RNode> implements ClosureCache {

    public static final FormalArguments NO_ARGS = new FormalArguments(new RNode[0], new Object[0], ArgumentsSignature.empty(0));

    private final IdentityHashMap<RNode, Closure> closureCache = new IdentityHashMap<>();

    /**
     * These argument constants define what will be passed along in case there is no supplied
     * argument for the given argument slot. In the case of normal functions (as opposed to
     * builtins), {@link RMissing#instance} and {@link RArgsValuesAndNames#EMPTY} will be replaced
     * with the actual default values on the callee side.
     */
    @CompilationFinal(dimensions = 1) private final Object[] internalDefaultArguments;

    private FormalArguments(RNode[] defaultArguments, Object[] internalDefaultArguments, ArgumentsSignature signature) {
        super(defaultArguments, signature);
        this.internalDefaultArguments = internalDefaultArguments;
    }

    public static FormalArguments createForFunction(RNode[] defaultArguments, ArgumentsSignature signature) {
        assert signature != null;
        assert signature.getVarArgCount() <= 1;
        RNode[] newDefaults = Arrays.copyOf(defaultArguments, signature.getLength());
        for (int i = 0; i < newDefaults.length; i++) {
            RNode defArg = newDefaults[i];
            newDefaults[i] = ConstantNode.isMissing(defArg) ? null : defArg;
        }
        Object[] internalDefaultArguments = new Object[signature.getLength()];
        for (int i = 0; i < internalDefaultArguments.length; i++) {
            internalDefaultArguments[i] = i == signature.getVarArgIndex() ? RArgsValuesAndNames.EMPTY : RMissing.instance;
        }
        return new FormalArguments(newDefaults, internalDefaultArguments, signature);
    }

    public static FormalArguments createForBuiltin(Object[] defaultArguments, ArgumentsSignature signature) {
        assert signature != null;
        assert defaultArguments.length <= signature.getLength();

        Object[] internalDefaultArguments = new Object[signature.getLength()];
        for (int i = 0; i < internalDefaultArguments.length; i++) {
            Object value;
            if (i < defaultArguments.length) {
                value = defaultArguments[i];
            } else {
                value = (i == signature.getVarArgIndex()) ? RArgsValuesAndNames.EMPTY : RMissing.instance;
            }
            internalDefaultArguments[i] = value;

            assert value != null : "null is not allowed as default value (RMissing.instance?)";
            assert value != RArgsValuesAndNames.EMPTY || i == signature.getVarArgIndex() : "RArgsValuesAndNames.EMPTY only allowed for vararg parameter";
            assert value == RArgsValuesAndNames.EMPTY || i != signature.getVarArgIndex() : "only RArgsValuesAndNames.EMPTY is allowed for vararg parameter";
            assert value != RMissing.instance || i != signature.getVarArgIndex() : "RMissing.instance not allowed for vararg parameter";
            assert isValidInternalDefaultArgument(value) : "unexpected default value " + value;
        }
        RNode[] constantDefaultParameters = new RNode[signature.getLength()];
        for (int i = 0; i < constantDefaultParameters.length; i++) {
            Object value = internalDefaultArguments[i];
            constantDefaultParameters[i] = (value == RMissing.instance || value == RArgsValuesAndNames.EMPTY) ? null : ConstantNode.create(value);
        }
        return new FormalArguments(constantDefaultParameters, internalDefaultArguments, signature);
    }

    private static boolean isValidInternalDefaultArgument(Object value) {
        return value instanceof String || value instanceof Integer || value instanceof Double || value instanceof Byte || value == RNull.instance || value == RMissing.instance ||
                        value == RArgsValuesAndNames.EMPTY;
    }

    @Override
    public IdentityHashMap<RNode, Closure> getContent() {
        return closureCache;
    }

    /**
     * The internal default value is the one to be used at the caller in case no argument is
     * supplied - this represents missing for normal functions, but may be overridden for builtin
     * functions using {@link RBuiltinNode#getDefaultParameterValues()}.
     */
    public Object getInternalDefaultArgumentAt(int index) {
        return internalDefaultArguments[index];
    }

    /**
     * @return The list of default arguments a function body specifies. 'No default value' is
     *         denoted by <code>null</code>
     */
    public RNode getDefaultArgument(int index) {
        return getArgument(index);
    }

    public boolean hasDefaultArgument(int index) {
        return getArgument(index) != null;
    }
}
