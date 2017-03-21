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
package com.oracle.truffle.r.nodes.access.variables;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.InvalidAssumptionException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.nodes.function.PromiseHelperNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.env.frame.ActiveBinding;
import com.oracle.truffle.r.runtime.env.frame.FrameSlotChangeMonitor;

public final class LocalReadVariableNode extends Node {

    @Child private PromiseHelperNode promiseHelper;

    private final Object identifier;
    private final boolean forceResult;

    @CompilationFinal private boolean[] seenValueKinds;
    @CompilationFinal private ValueProfile valueProfile;
    @CompilationFinal private ConditionProfile isNullProfile;
    @CompilationFinal private ConditionProfile isMissingProfile;
    @CompilationFinal private ConditionProfile isPromiseProfile;

    @CompilationFinal private FrameSlot frameSlot;
    @CompilationFinal private Assumption notInFrame;
    @CompilationFinal private Assumption containsNoActiveBindingAssumption;

    private final ValueProfile frameProfile = ValueProfile.createClassProfile();

    public static LocalReadVariableNode create(Object identifier, boolean forceResult) {
        return new LocalReadVariableNode(identifier, forceResult);
    }

    private LocalReadVariableNode(Object identifier, boolean forceResult) {
        assert identifier != null : "LocalReadVariableNode identifier is null";
        this.identifier = identifier;
        this.forceResult = forceResult;
    }

    public Object getIdentifier() {
        return identifier;
    }

    public Object execute(VirtualFrame frame) {
        return execute(frame, frame);
    }

    public Object execute(VirtualFrame frame, Frame variableFrame) {
        Frame profiledVariableFrame = frameProfile.profile(variableFrame);
        if (frameSlot == null && notInFrame == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            if (identifier.toString().isEmpty()) {
                throw RError.error(RError.NO_CALLER, RError.Message.ZERO_LENGTH_VARIABLE);
            }
            frameSlot = profiledVariableFrame.getFrameDescriptor().findFrameSlot(identifier);
            notInFrame = frameSlot == null ? profiledVariableFrame.getFrameDescriptor().getNotInFrameAssumption(identifier) : null;
        }
        // check if the slot is missing / wrong type in current frame
        if (frameSlot == null) {
            try {
                notInFrame.check();
            } catch (InvalidAssumptionException e) {
                frameSlot = profiledVariableFrame.getFrameDescriptor().findFrameSlot(identifier);
                notInFrame = frameSlot == null ? profiledVariableFrame.getFrameDescriptor().getNotInFrameAssumption(identifier) : null;
            }
        }
        if (frameSlot == null) {
            return null;
        }
        Object result = null;
        if (isMissingProfile == null) {
            seenValueKinds = new boolean[FrameSlotKind.values().length];
            valueProfile = ValueProfile.createClassProfile();
            isNullProfile = ConditionProfile.createBinaryProfile();
            isMissingProfile = ConditionProfile.createBinaryProfile();
        }
        result = valueProfile.profile(ReadVariableNode.profiledGetValue(seenValueKinds, profiledVariableFrame, frameSlot));
        if (isNullProfile.profile(result == null) || isMissingProfile.profile(result == RMissing.instance)) {
            return null;
        }

        if (containsNoActiveBindingAssumption == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            containsNoActiveBindingAssumption = FrameSlotChangeMonitor.getContainsNoActiveBindingAssumption(profiledVariableFrame.getFrameDescriptor());
        }
        // special treatment for active binding: call bound function
        if (!containsNoActiveBindingAssumption.isValid() && ActiveBinding.isActiveBinding(result)) {
            return ((ActiveBinding) result).readValue();
        }

        if (forceResult) {
            if (isPromiseProfile == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                isPromiseProfile = ConditionProfile.createBinaryProfile();
            }
            if (isPromiseProfile.profile(result instanceof RPromise)) {
                if (promiseHelper == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    promiseHelper = insert(new PromiseHelperNode());
                }
                result = promiseHelper.evaluate(frame, (RPromise) result);
            }
        }
        return result;
    }
}
