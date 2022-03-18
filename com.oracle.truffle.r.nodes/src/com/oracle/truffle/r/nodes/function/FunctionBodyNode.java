/*
 * Copyright (c) 2018, 2022, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.function;

import java.util.Collection;
import java.util.Set;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RArguments.DispatchArgs;
import com.oracle.truffle.r.runtime.RArguments.S3Args;
import com.oracle.truffle.r.runtime.RArguments.S4Args;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.ReturnException;
import com.oracle.truffle.r.runtime.RootBodyNode;
import com.oracle.truffle.r.runtime.env.frame.FrameIndex;
import com.oracle.truffle.r.runtime.env.frame.FrameSlotChangeMonitor;
import com.oracle.truffle.r.runtime.nodes.RNode;

public final class FunctionBodyNode extends Node implements RootBodyNode {

    @Child private RNode body;
    @Child private SaveArgumentsNode saveArguments;
    @Child private SetupS3ArgsNode setupS3Args;
    @Child private SetupS4ArgsNode setupS4Args;

    // Profiling for catching ReturnException
    private final BranchProfile returnExceptionProfile = BranchProfile.create();
    @CompilationFinal private ConditionProfile returnTopLevelProfile;

    public FunctionBodyNode(SaveArgumentsNode saveArguments, RNode body) {
        this.body = body;
        this.saveArguments = saveArguments;
    }

    @Override
    public Object visibleExecute(VirtualFrame frame) {
        try {
            setupDispatchSlots(frame);
            saveArguments.execute(frame);
            return body.visibleExecute(frame);
        } catch (ReturnException ex) {
            returnExceptionProfile.enter();
            if (profileReturnToTopLevel(ex.getTarget() == RArguments.getCall(frame))) {
                Object result = ex.getResult();
                if (CompilerDirectives.inInterpreter() && result == null) {
                    throw RInternalError.shouldNotReachHere("invalid null from ReturnException.getResult() of " + this);
                }
                return result;
            } else {
                // re-thrown until it reaches its target
                throw ex;
            }
        }
    }

    @Override
    public RNode getBody() {
        return body;
    }

    @Override
    public SourceSection getSourceSection() {
        return body.getSourceSection();
    }

    public boolean profileReturnToTopLevel(boolean condition) {
        if (returnTopLevelProfile == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            returnTopLevelProfile = ConditionProfile.createBinaryProfile();
        }
        return returnTopLevelProfile.profile(condition);
    }

    private void setupDispatchSlots(VirtualFrame frame) {
        DispatchArgs dispatchArgs = RArguments.getDispatchArgs(frame);
        if (dispatchArgs == null) {
            return;
        }
        if (dispatchArgs instanceof S3Args) {
            S3Args s3Args = (S3Args) dispatchArgs;
            if (setupS3Args == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                setupS3Args = insert(new SetupS3ArgsNode(frame.getFrameDescriptor()));
            }
            setupS3Args.execute(frame, s3Args);
        } else {
            S4Args s4Args = (S4Args) dispatchArgs;
            if (setupS4Args == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                setupS4Args = insert(new SetupS4ArgsNode(frame.getFrameDescriptor()));
            }
            setupS4Args.execute(frame, s4Args);
        }
    }

    private abstract static class SetupDispatchNode extends Node {
        // S3/S4 slots
        private final int dotGenericFrameIndex;
        private final int dotMethodFrameIndex;

        final BranchProfile invalidateFrameSlotProfile = BranchProfile.create();

        SetupDispatchNode(FrameDescriptor frameDescriptor) {
            dotGenericFrameIndex = FrameSlotChangeMonitor.findOrAddAuxiliaryFrameSlot(frameDescriptor, RRuntime.R_DOT_GENERIC);
            dotMethodFrameIndex = FrameSlotChangeMonitor.findOrAddAuxiliaryFrameSlot(frameDescriptor, RRuntime.R_DOT_METHOD);
            assert allIndexesInitialized(dotGenericFrameIndex, dotMethodFrameIndex);
        }

        void executeDispatchArgs(VirtualFrame frame, DispatchArgs args) {
            FrameSlotChangeMonitor.setObjectAndInvalidate(frame, dotGenericFrameIndex, args.generic, false, invalidateFrameSlotProfile);
            FrameSlotChangeMonitor.setObjectAndInvalidate(frame, dotMethodFrameIndex, args.method, false, invalidateFrameSlotProfile);
        }

        protected boolean allIndexesInitialized(int... indexes) {
            CompilerAsserts.neverPartOfCompilation();
            for (int index : indexes) {
                if (FrameIndex.isUninitializedIndex(index)) {
                    return false;
                }
            }
            return true;
        }
    }

    private static final class SetupS3ArgsNode extends SetupDispatchNode {
        // S3 frame indexes
        private final int dotClassFrameIndex;
        private final int dotGenericCallEnvFrameIndex;
        private final int dotGenericCallDefFrameIndex;
        private final int dotGroupFrameIndex;

        SetupS3ArgsNode(FrameDescriptor frameDescriptor) {
            super(frameDescriptor);
            dotClassFrameIndex = FrameSlotChangeMonitor.findOrAddAuxiliaryFrameSlot(frameDescriptor, RRuntime.R_DOT_CLASS);
            dotGenericCallEnvFrameIndex = FrameSlotChangeMonitor.findOrAddAuxiliaryFrameSlot(frameDescriptor, RRuntime.R_DOT_GENERIC_CALL_ENV);
            dotGenericCallDefFrameIndex = FrameSlotChangeMonitor.findOrAddAuxiliaryFrameSlot(frameDescriptor, RRuntime.R_DOT_GENERIC_DEF_ENV);
            dotGroupFrameIndex = FrameSlotChangeMonitor.findOrAddAuxiliaryFrameSlot(frameDescriptor, RRuntime.R_DOT_GROUP);
            assert allIndexesInitialized(dotClassFrameIndex, dotGenericCallEnvFrameIndex, dotGenericCallDefFrameIndex, dotGroupFrameIndex);
        }

        void execute(VirtualFrame frame, S3Args args) {
            super.executeDispatchArgs(frame, args);
            FrameSlotChangeMonitor.setObjectAndInvalidate(frame, dotClassFrameIndex, args.clazz, false, invalidateFrameSlotProfile);
            FrameSlotChangeMonitor.setObjectAndInvalidate(frame, dotGenericCallEnvFrameIndex, args.callEnv, false, invalidateFrameSlotProfile);
            FrameSlotChangeMonitor.setObjectAndInvalidate(frame, dotGenericCallDefFrameIndex, args.defEnv, false, invalidateFrameSlotProfile);
            FrameSlotChangeMonitor.setObjectAndInvalidate(frame, dotGroupFrameIndex, args.group, false, invalidateFrameSlotProfile);
        }
    }

    private static final class SetupS4ArgsNode extends SetupDispatchNode {
        // S4 frame indexes
        private final int dotDefinedFrameIndex;
        private final int dotTargetFrameIndex;
        private final int dotMethodsFrameIndex;

        SetupS4ArgsNode(FrameDescriptor frameDescriptor) {
            super(frameDescriptor);
            dotDefinedFrameIndex = FrameSlotChangeMonitor.findOrAddAuxiliaryFrameSlot(frameDescriptor, RRuntime.R_DOT_DEFINED);
            dotTargetFrameIndex = FrameSlotChangeMonitor.findOrAddAuxiliaryFrameSlot(frameDescriptor, RRuntime.R_DOT_TARGET);
            dotMethodsFrameIndex = FrameSlotChangeMonitor.findOrAddAuxiliaryFrameSlot(frameDescriptor, RRuntime.R_DOT_METHODS);
            assert allIndexesInitialized(dotDefinedFrameIndex, dotTargetFrameIndex, dotMethodsFrameIndex);
        }

        void execute(VirtualFrame frame, S4Args args) {
            super.executeDispatchArgs(frame, args);
            FrameSlotChangeMonitor.setObjectAndInvalidate(frame, dotDefinedFrameIndex, args.defined, false, invalidateFrameSlotProfile);
            FrameSlotChangeMonitor.setObjectAndInvalidate(frame, dotTargetFrameIndex, args.target, false, invalidateFrameSlotProfile);
            FrameSlotChangeMonitor.setObjectAndInvalidate(frame, dotMethodsFrameIndex, args.methods, false, invalidateFrameSlotProfile);
        }
    }
}
