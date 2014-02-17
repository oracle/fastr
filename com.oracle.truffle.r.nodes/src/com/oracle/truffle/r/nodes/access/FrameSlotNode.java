/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.access;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.runtime.*;

@TypeSystemReference(RTypes.class)
public abstract class FrameSlotNode extends Node {

    public abstract boolean hasValue(VirtualFrame virtualFrame, Frame frame);

    public FrameSlot executeFrameSlot(@SuppressWarnings("unused") VirtualFrame frame) {
        throw new UnsupportedOperationException();
    }

    static FrameSlot findFrameSlot(Frame frame, Object symbol) {
        return frame.getFrameDescriptor().findFrameSlot(RRuntime.toString(symbol));
    }

    static Assumption getAssumption(Frame frame, Object symbol) {
        return frame.getFrameDescriptor().getNotInFrameAssumption(RRuntime.toString(symbol));
    }

    public static final class UnresolvedFrameSlotNode extends FrameSlotNode {

        private final Object symbol;

        public UnresolvedFrameSlotNode(Object symbol) {
            this.symbol = symbol;
        }

        @Override
        public boolean hasValue(VirtualFrame virtualFrame, Frame frame) {
            CompilerDirectives.transferToInterpreter();
            return resolveFrameSlot(frame).hasValue(virtualFrame, frame);
        }

        private FrameSlotNode resolveFrameSlot(Frame frame) {
            final FrameSlotNode newNode;
            final FrameSlot frameSlot = findFrameSlot(frame, symbol);
            if (frameSlot != null) {
                newNode = new PresentFrameSlotNode(frameSlot);
            } else {
                newNode = new AbsentFrameSlotNode(getAssumption(frame, symbol), symbol);
            }
            return replace(newNode);
        }
    }

    public static final class AbsentFrameSlotNode extends FrameSlotNode {

        @CompilationFinal private Assumption assumption;
        private final Object symbol;

        public AbsentFrameSlotNode(Assumption assumption, Object symbol) {
            this.assumption = assumption;
            this.symbol = symbol;
        }

        @Override
        public boolean hasValue(VirtualFrame virtualFrame, Frame frame) {
            try {
                assumption.check();
            } catch (InvalidAssumptionException e) {
                final FrameSlot frameSlot = findFrameSlot(frame, symbol);
                if (frameSlot != null) {
                    return replace(new PresentFrameSlotNode(frameSlot)).hasValue(virtualFrame, frame);
                } else {
                    assumption = frame.getFrameDescriptor().getVersion();
                }
            }
            return false;
        }

        public Assumption getAssumption() {
            return assumption;
        }
    }

    public abstract static class AbstractPresentFrameSlotNode extends FrameSlotNode {

        protected final FrameSlot frameSlot;

        public AbstractPresentFrameSlotNode(FrameSlot frameSlot) {
            this.frameSlot = frameSlot;
        }

        @Override
        public final FrameSlot executeFrameSlot(VirtualFrame frame) {
            return frameSlot;
        }

        protected static boolean isInitialized(Frame frame, FrameSlot frameSlot) {
            try {
                return !frame.isObject(frameSlot) || frame.getObject(frameSlot) != null;
            } catch (FrameSlotTypeException e) {
                throw new IllegalStateException();
            }
        }
    }

    public static final class PresentFrameSlotNode extends AbstractPresentFrameSlotNode {

        public PresentFrameSlotNode(FrameSlot frameSlot) {
            super(frameSlot);
        }

        @Override
        public boolean hasValue(VirtualFrame virtualFrame, Frame frame) {
            CompilerAsserts.neverPartOfCompilation();
            boolean initialized = isInitialized(frame, frameSlot);
            if (initialized) {
                return replace(new InitializedFrameSlotNode(frameSlot)).hasValue(virtualFrame, frame);
            } else {
                return replace(new NotInitializedFrameSlotNode(frameSlot)).hasValue(virtualFrame, frame);
            }
        }
    }

    public static final class NotInitializedFrameSlotNode extends AbstractPresentFrameSlotNode {

        public NotInitializedFrameSlotNode(FrameSlot frameSlot) {
            super(frameSlot);
        }

        @Override
        public boolean hasValue(VirtualFrame virtualFrame, Frame frame) {
            boolean initialized = isInitialized(frame, frameSlot);
            if (!initialized) {
                return false;
            } else {
                CompilerDirectives.transferToInterpreter();
                return replace(new MaybeInitializedFrameSlotNode(frameSlot)).hasValue(virtualFrame, frame);
            }
        }
    }

    public static final class InitializedFrameSlotNode extends AbstractPresentFrameSlotNode {

        public InitializedFrameSlotNode(FrameSlot frameSlot) {
            super(frameSlot);
        }

        @Override
        public boolean hasValue(VirtualFrame virtualFrame, Frame frame) {
            boolean initialized = isInitialized(frame, frameSlot);
            if (initialized) {
                return true;
            } else {
                CompilerDirectives.transferToInterpreter();
                return replace(new MaybeInitializedFrameSlotNode(frameSlot)).hasValue(virtualFrame, frame);
            }
        }
    }

    public static final class MaybeInitializedFrameSlotNode extends AbstractPresentFrameSlotNode {

        public MaybeInitializedFrameSlotNode(FrameSlot frameSlot) {
            super(frameSlot);
        }

        @Override
        public boolean hasValue(VirtualFrame virtualFrame, Frame frame) {
            return isInitialized(frame, frameSlot);
        }
    }
}
