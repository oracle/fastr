/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RArguments.DispatchArgs;
import com.oracle.truffle.r.runtime.RArguments.S3Args;
import com.oracle.truffle.r.runtime.RArguments.S4Args;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RootBodyNode;
import com.oracle.truffle.r.runtime.env.frame.FrameSlotChangeMonitor;
import com.oracle.truffle.r.runtime.nodes.RNode;

public final class FunctionBodyNode extends Node implements RootBodyNode {

    @Child private RNode body;
    @Child private SaveArgumentsNode saveArguments;
    @Child private SetupS3ArgsNode setupS3Args;
    @Child private SetupS4ArgsNode setupS4Args;

    public FunctionBodyNode(SaveArgumentsNode saveArguments, RNode body) {
        this.body = body;
        this.saveArguments = saveArguments;
    }

    @Override
    public Object visibleExecute(VirtualFrame frame) {
        setupDispatchSlots(frame);
        saveArguments.execute(frame);
        return body.visibleExecute(frame);
    }

    @Override
    public RNode getBody() {
        return body;
    }

    @Override
    public SourceSection getSourceSection() {
        return body.getSourceSection();
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
        private final FrameSlot dotGenericSlot;
        private final FrameSlot dotMethodSlot;

        final BranchProfile invalidateFrameSlotProfile = BranchProfile.create();

        SetupDispatchNode(FrameDescriptor frameDescriptor) {
            dotGenericSlot = FrameSlotChangeMonitor.findOrAddFrameSlot(frameDescriptor, RRuntime.R_DOT_GENERIC, FrameSlotKind.Object);
            dotMethodSlot = FrameSlotChangeMonitor.findOrAddFrameSlot(frameDescriptor, RRuntime.R_DOT_METHOD, FrameSlotKind.Object);
        }

        void executeDispatchArgs(VirtualFrame frame, DispatchArgs args) {
            FrameSlotChangeMonitor.setObjectAndInvalidate(frame, dotGenericSlot, args.generic, false, invalidateFrameSlotProfile);
            FrameSlotChangeMonitor.setObjectAndInvalidate(frame, dotMethodSlot, args.method, false, invalidateFrameSlotProfile);
        }
    }

    private static final class SetupS3ArgsNode extends SetupDispatchNode {
        // S3 slots
        private final FrameSlot dotClassSlot;
        private final FrameSlot dotGenericCallEnvSlot;
        private final FrameSlot dotGenericCallDefSlot;
        private final FrameSlot dotGroupSlot;

        SetupS3ArgsNode(FrameDescriptor frameDescriptor) {
            super(frameDescriptor);
            dotClassSlot = FrameSlotChangeMonitor.findOrAddFrameSlot(frameDescriptor, RRuntime.R_DOT_CLASS, FrameSlotKind.Object);
            dotGenericCallEnvSlot = FrameSlotChangeMonitor.findOrAddFrameSlot(frameDescriptor, RRuntime.R_DOT_GENERIC_CALL_ENV, FrameSlotKind.Object);
            dotGenericCallDefSlot = FrameSlotChangeMonitor.findOrAddFrameSlot(frameDescriptor, RRuntime.R_DOT_GENERIC_DEF_ENV, FrameSlotKind.Object);
            dotGroupSlot = FrameSlotChangeMonitor.findOrAddFrameSlot(frameDescriptor, RRuntime.R_DOT_GROUP, FrameSlotKind.Object);
        }

        void execute(VirtualFrame frame, S3Args args) {
            super.executeDispatchArgs(frame, args);
            FrameSlotChangeMonitor.setObjectAndInvalidate(frame, dotClassSlot, args.clazz, false, invalidateFrameSlotProfile);
            FrameSlotChangeMonitor.setObjectAndInvalidate(frame, dotGenericCallEnvSlot, args.callEnv, false, invalidateFrameSlotProfile);
            FrameSlotChangeMonitor.setObjectAndInvalidate(frame, dotGenericCallDefSlot, args.defEnv, false, invalidateFrameSlotProfile);
            FrameSlotChangeMonitor.setObjectAndInvalidate(frame, dotGroupSlot, args.group, false, invalidateFrameSlotProfile);
        }
    }

    private static final class SetupS4ArgsNode extends SetupDispatchNode {
        // S4 slots
        private final FrameSlot dotDefinedSlot;
        private final FrameSlot dotTargetSlot;
        private final FrameSlot dotMethodsSlot;

        SetupS4ArgsNode(FrameDescriptor frameDescriptor) {
            super(frameDescriptor);
            dotDefinedSlot = FrameSlotChangeMonitor.findOrAddFrameSlot(frameDescriptor, RRuntime.R_DOT_DEFINED, FrameSlotKind.Object);
            dotTargetSlot = FrameSlotChangeMonitor.findOrAddFrameSlot(frameDescriptor, RRuntime.R_DOT_TARGET, FrameSlotKind.Object);
            dotMethodsSlot = FrameSlotChangeMonitor.findOrAddFrameSlot(frameDescriptor, RRuntime.R_DOT_METHODS, FrameSlotKind.Object);
        }

        void execute(VirtualFrame frame, S4Args args) {
            super.executeDispatchArgs(frame, args);
            FrameSlotChangeMonitor.setObjectAndInvalidate(frame, dotDefinedSlot, args.defined, false, invalidateFrameSlotProfile);
            FrameSlotChangeMonitor.setObjectAndInvalidate(frame, dotTargetSlot, args.target, false, invalidateFrameSlotProfile);
            FrameSlotChangeMonitor.setObjectAndInvalidate(frame, dotMethodsSlot, args.methods, false, invalidateFrameSlotProfile);
        }
    }
}
