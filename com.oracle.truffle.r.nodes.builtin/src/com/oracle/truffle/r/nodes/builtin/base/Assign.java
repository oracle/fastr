/*
 * Copyright (c) 2013, 2022, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.singleElement;
import static com.oracle.truffle.r.runtime.RVisibility.OFF;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.COMPLEX;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.nodes.access.FrameIndexNode;
import com.oracle.truffle.r.nodes.access.WriteSuperFrameVariableNode.ResolvedWriteSuperFrameVariableNode;
import com.oracle.truffle.r.nodes.access.WriteSuperFrameVariableNodeFactory.ResolvedWriteSuperFrameVariableNodeGen;
import com.oracle.truffle.r.nodes.access.WriteVariableNode.Mode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.base.AssignNodeGen.AssignInternalNodeGen;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.nodes.ShareObjectNode;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.env.REnvironment.PutException;
import com.oracle.truffle.r.runtime.env.frame.FrameSlotChangeMonitor;
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
public abstract class Assign extends RBuiltinNode.Arg4 {

    private final boolean direct;

    protected Assign() {
        this(false);
    }

    protected Assign(boolean direct) {
        this.direct = direct;
    }

    public abstract Object execute(VirtualFrame frame, RStringVector x, Object value, REnvironment pos, byte inherits);

    @Override
    public RBaseNode getErrorContext() {
        return direct ? this : super.getErrorContext();
    }

    /**
     * TODO: This method becomes obsolete when Assign and AssignFastPaths are modified to have the
     * (String, Object, REnvironment, boolean) signature.
     */
    private String checkVariable(RStringVector xVec) {
        int len = xVec.getLength();
        if (len == 1) {
            return xVec.getDataAt(0);
        } else if (len == 0) {
            throw error(RError.Message.INVALID_FIRST_ARGUMENT);
        } else {
            warning(RError.Message.ONLY_FIRST_VARIABLE_NAME);
            return xVec.getDataAt(0);
        }
    }

    static {
        Casts casts = new Casts(Assign.class);
        casts.arg("x").asStringVector().shouldBe(singleElement(), RError.Message.ONLY_FIRST_VARIABLE_NAME).findFirst(RError.Message.INVALID_FIRST_ARGUMENT);

        casts.arg("envir").mustNotBeNull(RError.Message.USE_NULL_ENV_DEFUNCT).mustBe(REnvironment.class, RError.Message.INVALID_ARGUMENT, "envir");

        // this argument could be made Boolean unless there were AssignFastPath relying upon the
        // byte argument
        casts.arg("inherits").asLogicalVector().findFirst().mustNotBeNA();
    }

    /**
     * The general case that requires searching the environment hierarchy.
     */
    @Specialization
    protected Object assign(VirtualFrame frame, RStringVector xVec, Object value, REnvironment envir, byte inherits,
                    @Cached("createBinaryProfile()") ConditionProfile inheritsProfile,
                    @Cached("create()") ShareObjectNode share,
                    @Cached("create()") AssignInternalNode assign) {
        String x = checkVariable(xVec);
        REnvironment env = envir;
        RContext ctx = getRContext();
        if (inheritsProfile.profile(RRuntime.fromLogical(inherits))) {
            while (env != REnvironment.emptyEnv()) {
                if (env.get(x) != null) {
                    break;
                }
                env = env.getParent();
            }
            if (env == REnvironment.emptyEnv()) {
                env = REnvironment.globalEnv(ctx);
            }
        } else {
            if (env == REnvironment.emptyEnv()) {
                throw error(RError.Message.CANNOT_ASSIGN_IN_EMPTY_ENV);
            }
        }
        assign.execute(frame, env, x, share.execute(value));
        return value;
    }

    protected abstract static class AssignInternalNode extends RBaseNode {

        public static AssignInternalNode create() {
            return AssignInternalNodeGen.create();
        }

        protected final ValueProfile frameAccessProfile = ValueProfile.createClassProfile();
        protected final ValueProfile frameProfile = ValueProfile.createClassProfile();

        public abstract void execute(VirtualFrame frame, REnvironment env, String name, Object value);

        protected static ResolvedWriteSuperFrameVariableNode createWrite(String name, FrameDescriptor envDesc) {
            int frameIndex;
            if (!FrameSlotChangeMonitor.containsIdentifier(envDesc, name)) {
                frameIndex = FrameSlotChangeMonitor.findOrAddAuxiliaryFrameSlot(envDesc, name);
            } else {
                frameIndex = FrameSlotChangeMonitor.getIndexOfIdentifier(envDesc, name);
            }
            FrameIndexNode frameIndexNode = FrameIndexNode.createInitializedWithIndex(envDesc, frameIndex);
            return ResolvedWriteSuperFrameVariableNodeGen.create(name, Mode.REGULAR, null, null, frameIndexNode);
        }

        protected FrameDescriptor getFrameDescriptor(REnvironment env) {
            return frameProfile.profile(env.getFrame(frameAccessProfile)).getFrameDescriptor();
        }

        @Specialization(guards = {"getFrameDescriptor(env) == envDesc", "write.getName().equals(name)"})
        protected void assignCached(VirtualFrame frame, REnvironment env, @SuppressWarnings("unused") String name, Object value,
                        @Cached("env.getFrame().getFrameDescriptor()") @SuppressWarnings("unused") FrameDescriptor envDesc,
                        @Cached("createWrite(name, envDesc)") ResolvedWriteSuperFrameVariableNode write) {
            write.execute(frame, value, frameProfile.profile(env.getFrame(frameAccessProfile)));
        }

        @Specialization(replaces = "assignCached")
        @TruffleBoundary
        protected void assign(REnvironment env, String name, Object value) {
            try {
                env.put(name, value);
            } catch (PutException ex) {
                throw error(ex);
            }
        }
    }
}
