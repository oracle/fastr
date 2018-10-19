/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.ffi.impl.nodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.r.nodes.access.variables.ReadVariableNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.env.frame.ActiveBinding;
import com.oracle.truffle.r.runtime.env.frame.FrameSlotChangeMonitor;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

public abstract class MakeActiveBindingNode extends RBaseNode {

    @CompilationFinal private BranchProfile frameSlotBranchProfile;

    public static MakeActiveBindingNode create() {
        return MakeActiveBindingNodeGen.create();
    }

    public abstract Object executeObject(Object sym, Object fun, Object env);

    @Specialization
    @TruffleBoundary
    protected Object makeActiveBinding(RSymbol sym, RFunction fun, REnvironment env) {
        if (frameSlotBranchProfile == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            frameSlotBranchProfile = BranchProfile.create();
        }

        String name = sym.getName();
        MaterializedFrame frame = env.getFrame();
        Object binding = ReadVariableNode.lookupAny(name, frame, true);
        if (binding == null) {
            if (!env.isLocked()) {
                FrameSlot slot = FrameSlotChangeMonitor.findOrAddFrameSlot(frame.getFrameDescriptor(), name, FrameSlotKind.Object);
                FrameSlotChangeMonitor.setActiveBinding(frame, slot, new ActiveBinding(sym.getRType(), fun), false, frameSlotBranchProfile);
                binding = ReadVariableNode.lookupAny(name, frame, true);
                assert binding != null;
                assert binding instanceof ActiveBinding;
            } else {
                throw error(RError.Message.CANNOT_ADD_BINDINGS);
            }
        } else if (!ActiveBinding.isActiveBinding(binding)) {
            throw error(RError.Message.SYMBOL_HAS_REGULAR_BINDING);
        } else if (env.bindingIsLocked(name)) {
            throw error(RError.Message.CANNOT_CHANGE_LOCKED_ACTIVE_BINDING);
        } else {
            // update active binding
            FrameSlot slot = frame.getFrameDescriptor().findFrameSlot(name);
            FrameSlotChangeMonitor.setActiveBinding(frame, slot, new ActiveBinding(sym.getRType(), fun), false, frameSlotBranchProfile);
        }
        return RNull.instance;
    }

}
