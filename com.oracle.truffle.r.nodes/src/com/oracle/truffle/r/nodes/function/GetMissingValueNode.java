/*
 * Copyright (c) 2014, 2014, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;

/**
 * This is a node abstraction for the functionality defined in
 * {@link RMissingHelper#getMissingValue(Frame,Symbol)}.
 */
public abstract class GetMissingValueNode extends RNode {

    public static GetMissingValueNode create(Symbol sym) {
        return new UninitializedGetMissingValueNode(sym);
    }

    private static final class UninitializedGetMissingValueNode extends GetMissingValueNode {

        private final Symbol sym;

        private UninitializedGetMissingValueNode(Symbol sym) {
            this.sym = sym;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            FrameSlot slot = frame.getFrameDescriptor().findFrameSlot(sym.getName());
            GetMissingValueNode gmvn = new ResolvedGetMissingValueNode(slot);
            return replace(gmvn).execute(frame);
        }

    }

    private static final class ResolvedGetMissingValueNode extends GetMissingValueNode {

        private final FrameSlot slot;

        private ResolvedGetMissingValueNode(FrameSlot slot) {
            this.slot = slot;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            if (slot == null) {
                return null;
            }
            try {
                return frame.getObject(slot);
            } catch (FrameSlotTypeException e) {
                return null;
            }
        }
    }
}
