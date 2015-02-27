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

import com.oracle.truffle.api.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.ConstantNode.ConstantMissingNode;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.RPromise.Closure;

/**
 * This class denotes a list of formal arguments which consist of the tuple
 * <ul>
 * <li>argument name (part of the signature)</li>
 * <li>expression ({@link RNode}, {@link #getDefaultArgs()})</li>
 * </ul>
 * The order is always the one defined by the function definition.
 * <p>
 * It also acts as {@link ClosureCache} for it's default arguments, so there is effectively only
 * ever one {@link RootCallTarget} for every default argument.
 * </p>
 */
public final class FormalArguments extends Arguments<RNode> implements ClosureCache {

    public static final FormalArguments NO_ARGS = new FormalArguments(new RNode[0], ArgumentsSignature.empty(0));

    private final IdentityHashMap<RNode, Closure> closureCache = new IdentityHashMap<>();

    private FormalArguments(RNode[] defaultArguments, ArgumentsSignature signature) {
        super(defaultArguments, signature);
    }

    public static FormalArguments create(RNode[] defaultArguments, ArgumentsSignature signature) {
        assert signature != null;
        RNode[] newDefaults = new RNode[defaultArguments.length];
        for (int i = 0; i < newDefaults.length; i++) {
            RNode defArg = defaultArguments[i];
            newDefaults[i] = defArg instanceof ConstantMissingNode ? null : defArg;
        }
        return new FormalArguments(newDefaults, signature);
    }

    @Override
    public IdentityHashMap<RNode, Closure> getContent() {
        return closureCache;
    }

    /**
     * @return The list of default arguments a function body specifies. 'No default value' is
     *         denoted by <code>null</code>
     */
    public RNode[] getDefaultArgs() {
        return arguments;
    }

    /**
     * This works as a direct accessor to one of the {@link #getDefaultArgs()}.
     *
     * @param index
     * @return The default argument for the given index.
     */
    public RNode getDefaultArg(int index) {
        assert index >= 0;
        return index < arguments.length ? arguments[index] : null;
    }

    /**
     * Retrieve one of the {@link #getDefaultArgs()}. If it does not exist, return {@code null}.
     *
     * @param index
     * @return The default argument for the given index, or <code>null</code> if there is none.
     */
    public RNode getDefaultArgOrNull(int index) {
        if (index < 0 || index >= arguments.length) {
            return null;
        } else {
            return arguments[index];
        }
    }

    /**
     * @return The length of the argument array
     */
    public int getArgsCount() {
        return arguments.length;
    }
}
