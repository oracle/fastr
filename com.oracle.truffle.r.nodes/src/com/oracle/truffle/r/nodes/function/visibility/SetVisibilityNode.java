/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.function.visibility;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RCaller;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RVisibility;
import com.oracle.truffle.r.runtime.env.frame.RFrameSlot;

/**
 * See {@link RFrameSlot#Visibility}.
 */
@NodeInfo(cost = NodeCost.NONE)
public final class SetVisibilityNode extends Node {

    @CompilationFinal private FrameSlot frameSlot;

    private SetVisibilityNode() {
    }

    public static SetVisibilityNode create() {
        return new SetVisibilityNode();
    }

    private void ensureFrameSlot(Frame frame) {
        if (frameSlot == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            frameSlot = frame.getFrameDescriptor().findOrAddFrameSlot(RFrameSlot.Visibility, FrameSlotKind.Boolean);
        }
    }

    public void execute(Frame frame, boolean value) {
        ensureFrameSlot(frame);
        frame.setBoolean(frameSlot, value);
    }

    public void execute(VirtualFrame frame, RVisibility visibility) {
        if (visibility == RVisibility.ON) {
            execute(frame, true);
        } else if (visibility == RVisibility.OFF) {
            execute(frame, false);
        }
    }

    /**
     * Needs to be called after each call site, so that the visibility is transferred from the
     * {@link RCaller} to the current frame.
     */
    public void executeAfterCall(VirtualFrame frame, RCaller caller) {
        ensureFrameSlot(frame);
        frame.setBoolean(frameSlot, caller.getVisibility());
    }

    /**
     * Needs to be called at the end of each function, so that the visibility is transferred from
     * the current frame into the {@link RCaller}.
     */
    public void executeEndOfFunction(VirtualFrame frame, RootNode root) {
        ensureFrameSlot(frame);
        try {
            if (frame.isBoolean(frameSlot)) {
                RArguments.getCall(frame).setVisibility(frame.getBoolean(frameSlot) == Boolean.TRUE);
            } else {
                CompilerDirectives.transferToInterpreter();
                /*
                 * Most likely the (only) builtin call in the function was configured to
                 * RVisibility.CUSTOM and didn't actually set the visibility. Another possible
                 * problem is a node that is created by RASTBuilder that does not set visibility.
                 */
                throw RInternalError.shouldNotReachHere("visibility not set at the end of " + root.getName());
            }
        } catch (FrameSlotTypeException e) {
            throw RInternalError.shouldNotReachHere(e);
        }
    }

    /**
     * Slow-path version of {@link #executeAfterCall(VirtualFrame, RCaller)}.
     */
    public static void executeAfterCallSlowPath(Frame frame, RCaller caller) {
        CompilerAsserts.neverPartOfCompilation();
        frame.setBoolean(frame.getFrameDescriptor().findOrAddFrameSlot(RFrameSlot.Visibility, FrameSlotKind.Boolean), caller.getVisibility());
    }

    /**
     * Slow-path version of {@link #execute(Frame, boolean)}.
     */
    public static void executeSlowPath(Frame frame, boolean visibility) {
        CompilerAsserts.neverPartOfCompilation();
        frame.setBoolean(frame.getFrameDescriptor().findOrAddFrameSlot(RFrameSlot.Visibility, FrameSlotKind.Boolean), visibility);
    }
}
