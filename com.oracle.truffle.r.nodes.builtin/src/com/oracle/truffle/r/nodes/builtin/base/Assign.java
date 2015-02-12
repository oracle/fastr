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

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.utilities.BranchProfile;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.env.*;
import com.oracle.truffle.r.runtime.env.REnvironment.*;
import com.oracle.truffle.r.runtime.env.frame.*;

/**
 * The {@code assign} builtin. There are two special cases worth optimnizing:
 * <ul>
 * <li>{@code inherits == FALSE}. No need to search environment hierarchy.</li>
 * <li>{@code envir} corresponds to the currently active frame. Unfortunately this is masked
 * somewhat by the signature of the {@code .Internal}, which only provides an environment. So this
 * is currently disabled,</li>
 * </ul>
 *
 */
@RBuiltin(name = "assign", kind = INTERNAL, parameterNames = {"x", "value", "envir", "inherits"})
public abstract class Assign extends RInvisibleBuiltinNode {

    @Child private WriteVariableNode writeVariableNode;

    @CompilationFinal private String lastName;

    @CompilationFinal private final BranchProfile[] slotFoundOnIteration = {BranchProfile.create(), BranchProfile.create(), BranchProfile.create()};
    private final BranchProfile invalidateProfile = BranchProfile.create();

    private void ensureWrite(String x) {
        if (writeVariableNode == null || !x.equals(lastName)) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            lastName = x;
            // must write to super (effect must be visible in caller frame of the assign builtin)
            WriteVariableNode wvn = WriteVariableNode.create(lastName, null, false, false);
            writeVariableNode = writeVariableNode == null ? insert(wvn) : writeVariableNode.replace(wvn);
        }
    }

    private String checkVariable(RAbstractStringVector xVec) throws RError {
        int len = xVec.getLength();
        if (len == 1) {
            return xVec.getDataAt(0);
        } else if (len == 0) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_FIRST_ARGUMENT);
        } else {
            RContext.getInstance().setEvalWarning("only the first element is used as variable name");
            return xVec.getDataAt(0);
        }
    }

    /**
     * The general case that requires searching the environment hierarchy.
     */
    @Specialization(guards = {"inheritsIsTrue", "!currentFrame"})
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
            throw RError.error(getEncapsulatingSourceSection(), ex);
        }
        return value;
    }

    @SuppressWarnings("unused")
    @ExplodeLoop
    @Specialization(guards = {"inheritsIsTrue", "currentFrame"})
    protected Object assignInherit(VirtualFrame virtualFrame, RAbstractStringVector xVec, Object value, REnvironment envir, byte inherits) {
        String x = checkVariable(xVec);
        controlVisibility();
        MaterializedFrame materializedFrame = virtualFrame.materialize();
        FrameSlot slot = materializedFrame.getFrameDescriptor().findFrameSlot(x);
        int iterationsAmount = CompilerAsserts.compilationConstant(slotFoundOnIteration.length);
        for (int i = 0; i < iterationsAmount; i++) {
            if (isAppropriateFrameSlot(slot, materializedFrame)) {
                addValueToFrame(x, value, materializedFrame, slot);
                return value;
            }
            slotFoundOnIteration[i].enter();
            materializedFrame = RArguments.getEnclosingFrame(materializedFrame);
            slot = materializedFrame.getFrameDescriptor().findFrameSlot(x);
        }
        assignInheritGenericCase(materializedFrame, x, value);
        return value;
    }

    @Specialization(guards = {"!inheritsIsTrue", "currentFrame"})
    @SuppressWarnings("unused")
    protected Object assignNoInherit(VirtualFrame frame, RAbstractStringVector xVec, Object value, REnvironment envir, byte inherits) {
        String x = checkVariable(xVec);
        controlVisibility();
        ensureWrite(x);
        writeVariableNode.execute(frame, value);
        return value;
    }

    @Specialization(guards = {"!inheritsIsTrue", "!currentFrame"})
    @SuppressWarnings("unused")
    protected Object assignNoInherit(RAbstractStringVector xVec, Object value, REnvironment envir, byte inherits) {
        String x = checkVariable(xVec);
        controlVisibility();
        if (envir == REnvironment.emptyEnv()) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.CANNOT_ASSIGN_IN_EMPTY_ENV);
        }
        try {
            envir.put(x, value);
        } catch (PutException ex) {
            throw RError.error(getEncapsulatingSourceSection(), ex);
        }
        return value;
    }

    private Object assignInheritGenericCase(MaterializedFrame startFrame, String x, Object value) {
        MaterializedFrame materializedFrame = startFrame;
        FrameSlot frameSlot = materializedFrame.getFrameDescriptor().findFrameSlot(x);
        while (!isAppropriateFrameSlot(frameSlot, materializedFrame)) {
            materializedFrame = RArguments.getEnclosingFrame(materializedFrame);
            frameSlot = materializedFrame.getFrameDescriptor().findFrameSlot(x);
        }
        addValueToFrame(x, value, materializedFrame, frameSlot);
        return value;
    }

    private void addValueToFrame(String x, Object value, Frame frame, FrameSlot frameSlot) {
        FrameSlot fs = frameSlot;
        if (fs == null) {
            fs = FrameSlotChangeMonitor.addFrameSlot(frame.getFrameDescriptor(), x, FrameSlotKind.Illegal);
        }
        FrameSlotChangeMonitor.setObjectAndInvalidate(frame, fs, value, true, invalidateProfile);
    }

    private static boolean isAppropriateFrameSlot(FrameSlot frameSlot, MaterializedFrame materializedFrame) {
        return frameSlot != null || REnvironment.isGlobalEnvFrame(materializedFrame);
    }

    @SuppressWarnings("unused")
    protected static boolean inheritsIsTrue(VirtualFrame frame, RAbstractStringVector x, Object value, REnvironment envir, byte inherits) {
        return inherits == RRuntime.LOGICAL_TRUE;
    }

    @SuppressWarnings("unused")
    protected static boolean currentFrame(VirtualFrame frame, RAbstractStringVector x, Object value, REnvironment envir, byte inherits) {
        /*
         * TODO how to determine this efficiently, remembering that "frame" is for the assign
         * closure, not the function that called it, which is where we want to do the assign iff
         * that matches "envir"
         */
        return false;
    }
}
