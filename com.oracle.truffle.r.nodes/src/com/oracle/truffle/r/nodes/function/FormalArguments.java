/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.util.*;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.access.ConstantNode.ConstantMissingNode;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.RPromise.Closure;

/**
 * This class denotes a list of formal arguments which consist of the tuple
 * <ul>
 * <li>argument name (part of the signature)</li>
 * <li>expression ({@link RNode}, {@link #getDefaultArguments()})</li>
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

    @CompilationFinal private final Object[] internalDefaultArguments;

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
            newDefaults[i] = defArg instanceof ConstantMissingNode ? null : defArg;
        }
        Object[] internalDefaultArguments = new Object[signature.getLength()];
        for (int i = 0; i < internalDefaultArguments.length; i++) {
            internalDefaultArguments[i] = i == signature.getVarArgIndex() ? RArgsValuesAndNames.EMPTY : RMissing.instance;
        }
        return new FormalArguments(newDefaults, internalDefaultArguments, signature);
    }

    public static FormalArguments createForBuiltin(Object[] defaultArguments, ArgumentsSignature signature) {
        assert signature != null;

        Object[] internalDefaultArguments = Arrays.copyOf(defaultArguments, signature.getLength());
        for (int i = 0; i < internalDefaultArguments.length; i++) {
            if (internalDefaultArguments[i] == null) {
                internalDefaultArguments[i] = i == signature.getVarArgIndex() ? RArgsValuesAndNames.EMPTY : RMissing.instance;
            }
        }
        RNode[] constantDefaultParameters = new RNode[signature.getLength()];
        for (int i = 0; i < constantDefaultParameters.length; i++) {
            Object value = internalDefaultArguments[i];
            assert !(value instanceof Node) && !(value instanceof Boolean) : "unexpected default value " + value;
            constantDefaultParameters[i] = (value == RMissing.instance || value == RArgsValuesAndNames.EMPTY) ? null : ConstantNode.create(value);
        }
        return new FormalArguments(constantDefaultParameters, internalDefaultArguments, signature);
    }

    @Override
    public IdentityHashMap<RNode, Closure> getContent() {
        return closureCache;
    }

    /**
     * @return The list of default arguments a function body specifies. 'No default value' is
     *         denoted by <code>null</code>
     */
    public RNode[] getDefaultArguments() {
        return arguments;
    }

    public boolean hasDefaultArgumentAt(int index) {
        return arguments[index] != null;
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
     * This works as a direct accessor to one of the {@link #getDefaultArguments()}.
     *
     * @param index
     * @return The default argument for the given index.
     */
    public RNode getDefaultArgumentAt(int index) {
        return arguments[index];
    }
}
