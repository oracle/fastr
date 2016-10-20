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
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.singleElement;
import static com.oracle.truffle.r.runtime.RVisibility.OFF;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.COMPLEX;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.function.opt.ShareObjectNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.env.REnvironment.PutException;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

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
@RBuiltin(name = "assign", visibility = OFF, kind = INTERNAL, parameterNames = {"x", "value", "envir", "inherits"}, behavior = COMPLEX)
public abstract class Assign extends RBuiltinNode {

    private final BranchProfile errorProfile = BranchProfile.create();
    private final BranchProfile warningProfile = BranchProfile.create();
    private final boolean direct;

    protected Assign() {
        this(false);
    }

    protected Assign(boolean direct) {
        this.direct = direct;
    }

    private RBaseNode errorContext() {
        return direct ? this : RError.SHOW_CALLER;
    }

    /**
     * TODO: This method becomes obsolete when Assign and AssignFastPaths are modified to have the
     * (String, Object, REnvironment, boolean) signature.
     */
    private String checkVariable(RAbstractStringVector xVec) {
        int len = xVec.getLength();
        if (len == 1) {
            return xVec.getDataAt(0);
        } else if (len == 0) {
            errorProfile.enter();
            throw RError.error(errorContext(), RError.Message.INVALID_FIRST_ARGUMENT);
        } else {
            warningProfile.enter();
            RError.warning(errorContext(), RError.Message.ONLY_FIRST_VARIABLE_NAME);
            return xVec.getDataAt(0);
        }
    }

    @Override
    protected void createCasts(CastBuilder casts) {
        casts.arg("x").asStringVector().shouldBe(singleElement(), RError.Message.ONLY_FIRST_VARIABLE_NAME).findFirst(RError.Message.INVALID_FIRST_ARGUMENT);

        casts.arg("envir").mustNotBeNull(RError.Message.USE_NULL_ENV_DEFUNCT).mustBe(REnvironment.class, RError.Message.INVALID_ARGUMENT, "envir");

        // this argument could be made Boolean unless there were AssignFastPath relying upon the
        // byte argument
        casts.arg("inherits").asLogicalVector().findFirst().notNA();
    }

    /**
     * The general case that requires searching the environment hierarchy.
     */
    @Specialization
    protected Object assign(RAbstractStringVector xVec, Object value, REnvironment envir, byte inherits, //
                    @Cached("createBinaryProfile()") ConditionProfile inheritsProfile,
                    @Cached("create()") ShareObjectNode share) {
        String x = checkVariable(xVec);
        REnvironment env = envir;
        if (inheritsProfile.profile(RRuntime.fromLogical(inherits))) {
            while (env != REnvironment.emptyEnv()) {
                if (env.get(x) != null) {
                    break;
                }
                env = env.getParent();
            }
            if (env == REnvironment.emptyEnv()) {
                env = REnvironment.globalEnv();
            }
        } else {
            if (CompilerDirectives.inInterpreter()) {
                LoopNode.reportLoopCount(this, -1);
            }
            if (env == REnvironment.emptyEnv()) {
                errorProfile.enter();
                throw RError.error(errorContext(), RError.Message.CANNOT_ASSIGN_IN_EMPTY_ENV);
            }
        }
        try {
            env.put(x, share.execute(value));
        } catch (PutException ex) {
            errorProfile.enter();
            throw RError.error(errorContext(), ex);
        }
        return value;
    }
}
