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
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.env.*;
import com.oracle.truffle.r.runtime.env.REnvironment.PutException;

/**
 * The {@code assign} builtin. There are two special cases worth optimizing:
 * <ul>
 * <li>{@code inherits == FALSE}. No need to search environment hierarchy.</li>
 * <li>{@code envir} corresponds to the currently active frame. Unfortunately this is masked
 * somewhat by the signature of the {@code .Internal}, which adds an frame. Not worth optimizing
 * anyway as using {@code assign} for the current frame is highly unlikely.</li>
 * </ul>
 *
 */
@RBuiltin(name = "assign", kind = INTERNAL, parameterNames = {"x", "value", "envir", "inherits"})
public abstract class Assign extends RInvisibleBuiltinNode {

    @CompilationFinal private final BranchProfile[] slotFoundOnIteration = {BranchProfile.create(), BranchProfile.create(), BranchProfile.create()};
    private final BranchProfile errorProfile = BranchProfile.create();
    private final BranchProfile warningProfile = BranchProfile.create();

    private String checkVariable(RAbstractStringVector xVec) {
        int len = xVec.getLength();
        if (len == 1) {
            return xVec.getDataAt(0);
        } else if (len == 0) {
            errorProfile.enter();
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_FIRST_ARGUMENT);
        } else {
            warningProfile.enter();
            RError.warning(getEncapsulatingSourceSection(), RError.Message.ONLY_FIRST_VARIABLE_NAME);
            return xVec.getDataAt(0);
        }
    }

    @Override
    protected void createCasts(CastBuilder casts) {
        casts.toLogical(3);
    }

    /**
     * The general case that requires searching the environment hierarchy.
     */
    @Specialization(guards = {"inheritsIsTrue(inherits)"})
    protected Object assignInherit(RAbstractStringVector xVec, Object value, REnvironment envir, @SuppressWarnings("unused") byte inherits) {
        controlVisibility();
        String x = checkVariable(xVec);
        REnvironment env = envir;
        while (env != null) {
            if (env.get(x) != null) {
                break;
            }
            env = env.getParent();
        }
        try {
            if (env != null) {
                env.put(x, value);
            } else {
                REnvironment.globalEnv().put(x, value);
            }
        } catch (PutException ex) {
            errorProfile.enter();
            throw RError.error(getEncapsulatingSourceSection(), ex);
        }
        return value;
    }

    @Specialization(guards = {"!inheritsIsTrue(inherits)"})
    @SuppressWarnings("unused")
    protected Object assignNoInherit(RAbstractStringVector xVec, Object value, REnvironment envir, byte inherits) {
        String x = checkVariable(xVec);
        controlVisibility();
        if (envir == REnvironment.emptyEnv()) {
            errorProfile.enter();
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.CANNOT_ASSIGN_IN_EMPTY_ENV);
        }
        try {
            envir.put(x, value);
        } catch (PutException ex) {
            errorProfile.enter();
            throw RError.error(getEncapsulatingSourceSection(), ex);
        }
        return value;
    }

    @SuppressWarnings("unused")
    @Fallback
    protected Object assignFallback(Object xVec, Object value, Object envir, Object inherits) {
        errorProfile.enter();
        if (RRuntime.asString(xVec) == null) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_FIRST_ARGUMENT);
        } else if (!(envir instanceof REnvironment)) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_ARGUMENT, "pos");
        } else {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_ARGUMENT, "inherits");
        }
    }

    protected static boolean inheritsIsTrue(byte inherits) {
        return inherits == RRuntime.LOGICAL_TRUE;
    }

}
