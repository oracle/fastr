/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.runtime.data.*;

/**
 * This class should allow caching for calls to function taking "..." arguments.<br/>
 * The idea is the following: We want to cache processed (matched and wrapped) arguments (and thus
 * argument processing) per call site.<br/>
 * The problem: If a function takes "...", arguments may change each call - even on the same call
 * site! <br/>
 * Example:
 *
 * <pre>
 * > f <- function(...) g(...)      # ONE callsite, but may take different arguments!
 * > f(1,2)                         # 1,2
 * > f(a=3,b=4)                     # a=3, b=4
 * </pre>
 *
 * To enable us to cache anyway, we have to be able to decide whether two function calls were issued
 * with the same arguments - which we do in this class.<br/>
 * The basic assumption is that the expressions used for two different calls on the same callsite
 * are identical ({@code ==} returns <code>true</code>). So by comparing two {@link RNode}s by
 * identity we can decide whether the call was issued using the same arguments or not and thus
 * whether the call originated from the same call site as before.
 */
public final class VarArgsSignature {
    /**
     * Important to allow identity checks on values of {@link RMissing} in
     * {@link #isEqualTo(VarArgsSignature)}. Never to be executed!
     */
    public static final RNode NO_VARARGS = ConstantNode.create(RArgsValuesAndNames.EMPTY);
    public static final VarArgsSignature TAKES_NO_VARARGS = new VarArgsSignature(null, 0);

    @CompilationFinal private final Object[] expressions;
    private final int times;

    private VarArgsSignature(Object[] expressions, int times) {
        this.expressions = expressions;
        this.times = times;
    }

    /**
     * @param expressions Must be non-<code>null</code> and longer then 0
     * @param times Must be <code>>= 1</code>
     * @return a fresh {@link VarArgsSignature}
     */
    public static VarArgsSignature create(Object[] expressions, int times) {
        assert expressions != null && expressions.length > 0;
        assert times >= 1;
        return new VarArgsSignature(expressions, times);
    }

    public boolean isEqualTo(final VarArgsSignature other) {
        if (other == null) {
            return false;
        }
        if (this == other || this.expressions == null) {
            return true;
        }

        // Check expressions for identity of each element
        if (expressions.length != other.expressions.length || times != other.times) {
            return false;
        }
        return checkExpressionsIdentity(other);
    }

    @ExplodeLoop
    private boolean checkExpressionsIdentity(final VarArgsSignature other) {
        for (int i = 0; i < expressions.length; i++) {
            if (expressions[i] != other.expressions[i]) {
                return false;
            }
        }
        return true;
    }

    public boolean isNotEqualTo(VarArgsSignature other) {
        return !isEqualTo(other);
    }
}
