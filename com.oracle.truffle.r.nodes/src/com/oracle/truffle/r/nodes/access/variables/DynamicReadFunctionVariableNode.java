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
package com.oracle.truffle.r.nodes.access.variables;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.nodes.access.variables.DynamicReadFunctionVariableNodeGen.GenericDynamicReadFunctionVariableNodeGen;
import com.oracle.truffle.r.nodes.profile.TruffleBoundaryNode;
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.SuppressFBWarnings;

/**
 * Version of {@link ReadVariableNode} that re-specializes on frame descriptor changes. It should be
 * used in places where the descriptor may change, e.g., when reading a variable from caller frame.
 *
 * The read is always silent and always forces the result to type check that it is a function.
 */
@ImportStatic(DSLConfig.class)
public abstract class DynamicReadFunctionVariableNode extends Node {

    protected final String identifier;
    protected final boolean local;

    public DynamicReadFunctionVariableNode(String identifier, boolean local) {
        this.identifier = identifier;
        this.local = local;
    }

    public static DynamicReadFunctionVariableNode create(String identifier) {
        return DynamicReadFunctionVariableNodeGen.create(identifier, false);
    }

    public static DynamicReadFunctionVariableNode createLocal(String identifier) {
        return DynamicReadFunctionVariableNodeGen.create(identifier, true);
    }

    // Note: DSL gets crazy if the second argument is of type "Frame"
    public abstract Object execute(VirtualFrame frame, Object varFrame);

    @Specialization(guards = {"getDescriptor(variableFrame) == descriptor", "local"}, limit = "getCacheSize(3)")
    protected Object cachedLocalRead(VirtualFrame frame, Object variableFrame,
                    @Cached("getDescriptor(variableFrame)") @SuppressWarnings("unused") FrameDescriptor descriptor,
                    @Cached("createLocalRead(identifier)") LocalReadVariableNode readNode) {
        return readNode.execute(frame, (Frame) variableFrame);
    }

    @Specialization(guards = {"getDescriptor(variableFrame) == descriptor", "!local"}, limit = "getCacheSize(3)")
    protected Object cachedRead(VirtualFrame frame, Object variableFrame,
                    @Cached("getDescriptor(variableFrame)") @SuppressWarnings("unused") FrameDescriptor descriptor,
                    @Cached("createRead(identifier)") ReadVariableNode readNode) {
        return readNode.execute(frame, (Frame) variableFrame);
    }

    @Specialization(replaces = {"cachedRead", "cachedLocalRead"})
    protected Object genericRead(VirtualFrame frame, Object variableFrame,
                    @Cached("create(identifier, local)") GenericDynamicReadFunctionVariableNode generiRead) {
        return executeGenericRead(frame.materialize(), variableFrame, generiRead);
    }

    @TruffleBoundary
    private static Object executeGenericRead(MaterializedFrame frame, Object variableFrame, GenericDynamicReadFunctionVariableNode generiRead) {
        return generiRead.execute(frame, variableFrame);
    }

    protected static FrameDescriptor getDescriptor(Object frame) {
        return ((Frame) frame).getFrameDescriptor();
    }

    protected static ReadVariableNode createRead(String identifier) {
        return ReadVariableNode.createSilent(identifier, RType.Function);
    }

    protected static LocalReadVariableNode createLocalRead(String identifier) {
        return LocalReadVariableNode.create(identifier, true);
    }

    protected abstract static class GenericDynamicReadFunctionVariableNode extends TruffleBoundaryNode {
        @Child private LocalReadVariableNode cachedLocalRead;
        @Child private ReadVariableNode cachedRead;

        private final String identifier;
        private FrameDescriptor lastDescriptor;
        protected final boolean local;

        public GenericDynamicReadFunctionVariableNode(String identifier, boolean local) {
            this.identifier = identifier;
            this.local = local;
        }

        public static GenericDynamicReadFunctionVariableNode create(String identifier, boolean local) {
            return GenericDynamicReadFunctionVariableNodeGen.create(identifier, local);
        }

        // Note: DSL gets crazy if the second argument is of type "Frame"
        public abstract Object execute(VirtualFrame frame, Object varFrame);

        @Specialization
        @SuppressFBWarnings(value = "UWF_UNWRITTEN_FIELD", justification = "incomplete implementation")
        protected Object cachedLocalRead(VirtualFrame frame, Object variableFrame) {
            Frame varFrame = (Frame) variableFrame;
            if (local) {
                LocalReadVariableNode read = cachedLocalRead;
                if (lastDescriptor != varFrame.getFrameDescriptor()) {
                    cachedLocalRead = read = insert(createLocalRead(identifier));
                }
                return read.execute(frame, varFrame);
            } else {
                ReadVariableNode read = cachedRead;
                if (lastDescriptor != varFrame.getFrameDescriptor()) {
                    cachedRead = read = insert(createRead(identifier));
                }
                return read.execute(frame, varFrame);
            }
        }
    }
}
