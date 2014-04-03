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
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.WriteVariableNodeFactory.ResolvedWriteLocalVariableNodeFactory;
import com.oracle.truffle.r.nodes.access.WriteVariableNodeFactory.UnresolvedWriteLocalVariableNodeFactory;
import com.oracle.truffle.r.nodes.access.WriteVariableNodeFactory.WriteSuperVariableNodeFactory;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

@NodeChild(value = "rhs", type = RNode.class)
@NodeField(name = "argWrite", type = boolean.class)
public abstract class WriteVariableNode extends RNode implements VisibilityController {

    public abstract boolean isArgWrite();

    public abstract RNode getRhs();

    @com.oracle.truffle.api.CompilerDirectives.CompilationFinal boolean everSeenNonEqual;
    @com.oracle.truffle.api.CompilerDirectives.CompilationFinal boolean everSeenVector;
    @com.oracle.truffle.api.CompilerDirectives.CompilationFinal boolean everSeenNonShared;
    @com.oracle.truffle.api.CompilerDirectives.CompilationFinal boolean everSeenShared;
    @com.oracle.truffle.api.CompilerDirectives.CompilationFinal boolean everSeenTemporary;
    @com.oracle.truffle.api.CompilerDirectives.CompilationFinal boolean everSeenSequence;

    @Override
    public final boolean getVisibility() {
        return false;
    }

    // the toBeCopied parameter is meant to prevent creation of the shared/non-temp vector; this
    // needed for the implementation of the replacement forms of builtin functions as their last
    // argument can be mutated; for example, in "dimnames(x)<-list(1)", the assigned value list(1)
    // must become list("1"), with the latter value returned as a result of the call
    protected void writeObjectValue(@SuppressWarnings("unused") VirtualFrame virtualFrame, Frame frame, FrameSlot frameSlot, Object value, boolean toBeCopied) {
        Object newValue = value;
        if (!isArgWrite()) {
            Object frameValue;
            try {
                frameValue = frame.getObject(frameSlot);
            } catch (FrameSlotTypeException ex) {
                throw new IllegalStateException();
            }
            if (frameValue != value) {
                if (!everSeenNonEqual) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    everSeenNonEqual = true;
                }
                if (value instanceof RVector) {
                    if (!everSeenVector) {
                        CompilerDirectives.transferToInterpreterAndInvalidate();
                        everSeenVector = true;
                    }
                    RVector rVector = (RVector) value;
                    if (rVector.isTemporary()) {
                        if (!everSeenTemporary) {
                            CompilerDirectives.transferToInterpreterAndInvalidate();
                            everSeenTemporary = true;
                        }
                        if (toBeCopied) {
                            RVector vectorCopy = rVector.copy();
                            newValue = vectorCopy;
                        } else {
                            rVector.markNonTemporary();
                        }
                    } else if (rVector.isShared()) {
                        if (!everSeenShared) {
                            CompilerDirectives.transferToInterpreterAndInvalidate();
                            everSeenShared = true;
                        }
                        RVector vectorCopy = rVector.copy();
                        if (!toBeCopied) {
                            vectorCopy.markNonTemporary();
                        }
                        newValue = vectorCopy;
                    } else {
                        if (!everSeenNonShared) {
                            CompilerDirectives.transferToInterpreterAndInvalidate();
                            everSeenNonShared = true;
                        }
                        if (toBeCopied) {
                            RVector vectorCopy = rVector.copy();
                            newValue = vectorCopy;
                        } else {
                            rVector.makeShared();
                        }
                    }
                }
            }
        }

        frame.setObject(frameSlot, newValue);
    }

    public static WriteVariableNode create(Object symbol, RNode rhs, boolean isArgWrite, boolean isSuper, boolean toBeCopied) {
        if (!isSuper) {
            return UnresolvedWriteLocalVariableNodeFactory.create(rhs, isArgWrite, RRuntime.toString(symbol), toBeCopied);
        } else {
            assert !isArgWrite;
            return new UnresolvedWriteSuperVariableNode(rhs, RRuntime.toString(symbol), toBeCopied);
        }
    }

    public static WriteVariableNode create(Object symbol, RNode rhs, boolean isArgWrite, boolean isSuper) {
        return create(symbol, rhs, isArgWrite, isSuper, false);
    }

    public static WriteVariableNode create(SourceSection src, Object symbol, RNode rhs, boolean isArgWrite, boolean isSuper) {
        WriteVariableNode wvn = create(symbol, rhs, isArgWrite, isSuper, false);
        wvn.assignSourceSection(src);
        return wvn;
    }

    public abstract void execute(VirtualFrame frame, Object value);

    @NodeFields({@NodeField(name = "symbol", type = Object.class), @NodeField(name = "toBeCopied", type = boolean.class)})
    public abstract static class UnresolvedWriteLocalVariableNode extends WriteVariableNode {

        public abstract Object getSymbol();

        public abstract boolean isToBeCopied();

        @Specialization
        public byte doLogical(VirtualFrame frame, byte value) {
            resolveAndSet(frame, value, FrameSlotKind.Byte);
            return value;
        }

        @Specialization
        public int doInteger(VirtualFrame frame, int value) {
            resolveAndSet(frame, value, FrameSlotKind.Int);
            return value;
        }

        @Specialization
        public double doDouble(VirtualFrame frame, double value) {
            resolveAndSet(frame, value, FrameSlotKind.Double);
            return value;
        }

        @Specialization
        public Object doObject(VirtualFrame frame, Object value) {
            resolveAndSet(frame, value, FrameSlotKind.Object);
            return value;
        }

        private void resolveAndSet(VirtualFrame frame, Object value, FrameSlotKind initialKind) {
            CompilerAsserts.neverPartOfCompilation();
            FrameSlot frameSlot = frame.getFrameDescriptor().findOrAddFrameSlot(getSymbol(), initialKind);
            replace(ResolvedWriteLocalVariableNode.create(getRhs(), this.isArgWrite(), frameSlot, isToBeCopied())).execute(frame, value);
        }
    }

    @NodeFields({@NodeField(name = "frameSlot", type = FrameSlot.class), @NodeField(name = "toBeCopied", type = boolean.class)})
    public abstract static class ResolvedWriteLocalVariableNode extends WriteVariableNode {

        public abstract boolean isToBeCopied();

        public static ResolvedWriteLocalVariableNode create(RNode rhs, boolean isArgWrite, FrameSlot frameSlot, boolean toBeCopied) {
            return ResolvedWriteLocalVariableNodeFactory.create(rhs, isArgWrite, frameSlot, toBeCopied);
        }

        @Specialization(guards = "isBooleanKind")
        public byte doLogical(VirtualFrame frame, FrameSlot frameSlot, byte value) {
            controlVisibility();
            frame.setByte(frameSlot, value);
            return value;
        }

        @Specialization(guards = "isIntegerKind")
        public int doInteger(VirtualFrame frame, FrameSlot frameSlot, int value) {
            controlVisibility();
            frame.setInt(frameSlot, value);
            return value;
        }

        @Specialization(guards = "isDoubleKind")
        public double doDouble(VirtualFrame frame, FrameSlot frameSlot, double value) {
            controlVisibility();
            frame.setDouble(frameSlot, value);
            return value;
        }

        @Specialization(guards = "isObjectKind")
        public Object doObject(VirtualFrame frame, FrameSlot frameSlot, Object value) {
            controlVisibility();
            super.writeObjectValue(frame, frame, frameSlot, value, isToBeCopied());
            return value;
        }
    }

    public abstract static class AbstractWriteSuperVariableNode extends WriteVariableNode {

        public abstract void execute(VirtualFrame frame, Object value, MaterializedFrame enclosingFrame);

        @Override
        public final Object execute(VirtualFrame frame) {
            Object value = getRhs().execute(frame);
            execute(frame, value);
            return value;
        }
    }

    public static final class WriteSuperVariableConditionalNode extends AbstractWriteSuperVariableNode {

        @Child private WriteSuperVariableNode writeNode;
        @Child private AbstractWriteSuperVariableNode nextNode;
        @Child private RNode rhs;

        WriteSuperVariableConditionalNode(WriteSuperVariableNode writeNode, AbstractWriteSuperVariableNode nextNode, RNode rhs) {
            this.writeNode = writeNode;
            this.nextNode = nextNode;
            this.rhs = rhs;
        }

        @Override
        public RNode getRhs() {
            return rhs;
        }

        @Override
        public void execute(VirtualFrame frame, Object value, MaterializedFrame enclosingFrame) {
            controlVisibility();
            if (writeNode.getFrameSlotNode().hasValue(frame, enclosingFrame)) {
                writeNode.execute(frame, value, enclosingFrame);
            } else {
                nextNode.execute(frame, value, RArguments.get(enclosingFrame).getEnclosingFrame());
            }
        }

        @Override
        public void execute(VirtualFrame frame, Object value) {
            controlVisibility();
            assert RArguments.get(frame).getEnclosingFrame() != null;
            execute(frame, value, RArguments.get(frame).getEnclosingFrame());
        }

        @Override
        public boolean isArgWrite() {
            return false;
        }
    }

    public static final class UnresolvedWriteSuperVariableNode extends AbstractWriteSuperVariableNode {

        @Child private RNode rhs;
        private final Object symbol;
        private final boolean toBeCopied;

        public UnresolvedWriteSuperVariableNode(RNode rhs, Object symbol, boolean toBeCopied) {
            this.rhs = rhs;
            this.symbol = symbol;
            this.toBeCopied = toBeCopied;
        }

        @Override
        public RNode getRhs() {
            return rhs;
        }

        @Override
        public void execute(VirtualFrame frame, Object value, MaterializedFrame enclosingFrame) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            final AbstractWriteSuperVariableNode writeNode;
            if (RArguments.get(enclosingFrame).getEnclosingFrame() == null) {
                // we've reached the global scope, do unconditional write
                // if this is the first node in the chain, needs the rhs and enclosingFrame nodes
                AccessEnclosingFrameNode enclosingFrameNode = RArguments.get(frame).getEnclosingFrame() == enclosingFrame ? AccessEnclosingFrameNodeFactory.create(1) : null;
                writeNode = WriteSuperVariableNodeFactory.create(getRhs(), enclosingFrameNode, new FrameSlotNode.PresentFrameSlotNode(enclosingFrame.getFrameDescriptor().findOrAddFrameSlot(symbol)),
                                this.isArgWrite(), toBeCopied);
            } else {
                WriteSuperVariableNode actualWriteNode = WriteSuperVariableNodeFactory.create(null, null, new FrameSlotNode.UnresolvedFrameSlotNode(symbol), this.isArgWrite(), toBeCopied);
                writeNode = new WriteSuperVariableConditionalNode(actualWriteNode, new UnresolvedWriteSuperVariableNode(null, symbol, toBeCopied), getRhs());
            }
            replace(writeNode).execute(frame, value, enclosingFrame);
        }

        @Override
        public void execute(VirtualFrame frame, Object value) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            MaterializedFrame enclosingFrame = RArguments.get(frame).getEnclosingFrame();
            if (enclosingFrame != null) {
                execute(frame, value, enclosingFrame);
            } else {
                // we're in global scope, do a local write instead
                replace(UnresolvedWriteLocalVariableNodeFactory.create(getRhs(), this.isArgWrite(), RRuntime.toString(symbol), toBeCopied)).execute(frame, value);
            }
        }

        @Override
        public boolean isArgWrite() {
            return false;
        }
    }

    @SuppressWarnings("unused")
    @NodeChildren({@NodeChild(value = "enclosingFrame", type = AccessEnclosingFrameNode.class), @NodeChild(value = "frameSlotNode", type = FrameSlotNode.class)})
    @NodeField(name = "toBeCopied", type = boolean.class)
    public abstract static class WriteSuperVariableNode extends AbstractWriteSuperVariableNode {

        protected abstract FrameSlotNode getFrameSlotNode();

        public abstract boolean isToBeCopied();

        @Specialization(guards = "isBooleanKind")
        public byte doBoolean(VirtualFrame frame, byte value, MaterializedFrame enclosingFrame, FrameSlot frameSlot) {
            controlVisibility();
            enclosingFrame.setByte(frameSlot, value);
            return value;
        }

        @Specialization(guards = "isIntegerKind")
        public int doInteger(VirtualFrame frame, int value, MaterializedFrame enclosingFrame, FrameSlot frameSlot) {
            controlVisibility();
            enclosingFrame.setInt(frameSlot, value);
            return value;
        }

        @Specialization(guards = "isDoubleKind")
        public double doDouble(VirtualFrame frame, double value, MaterializedFrame enclosingFrame, FrameSlot frameSlot) {
            controlVisibility();
            enclosingFrame.setDouble(frameSlot, value);
            return value;
        }

        @Specialization(guards = "isObjectKind")
        public Object doObject(VirtualFrame frame, Object value, MaterializedFrame enclosingFrame, FrameSlot frameSlot) {
            controlVisibility();
            super.writeObjectValue(frame, enclosingFrame, frameSlot, value, isToBeCopied());
            return value;
        }

        protected static boolean isBooleanKind(Object arg0, MaterializedFrame arg1, FrameSlot frameSlot) {
            return isBooleanKind(frameSlot);
        }

        protected static boolean isIntegerKind(Object arg0, MaterializedFrame arg1, FrameSlot frameSlot) {
            return isIntegerKind(frameSlot);
        }

        protected static boolean isDoubleKind(Object arg0, MaterializedFrame arg1, FrameSlot frameSlot) {
            return isDoubleKind(frameSlot);
        }

        protected static boolean isObjectKind(Object arg0, MaterializedFrame arg1, FrameSlot frameSlot) {
            return isObjectKind(frameSlot);
        }
    }

    protected static boolean isBooleanKind(FrameSlot frameSlot) {
        return isKind(frameSlot, FrameSlotKind.Boolean);
    }

    protected static boolean isIntegerKind(FrameSlot frameSlot) {
        return isKind(frameSlot, FrameSlotKind.Int);
    }

    protected static boolean isDoubleKind(FrameSlot frameSlot) {
        return isKind(frameSlot, FrameSlotKind.Double);
    }

    protected static boolean isObjectKind(FrameSlot frameSlot) {
        if (frameSlot.getKind() != FrameSlotKind.Object) {
            CompilerDirectives.transferToInterpreter();
            frameSlot.setKind(FrameSlotKind.Object);
        }
        return true;
    }

    private static boolean isKind(FrameSlot frameSlot, FrameSlotKind kind) {
        return frameSlot.getKind() == kind || initialSetKind(frameSlot, kind);
    }

    private static boolean initialSetKind(FrameSlot frameSlot, FrameSlotKind kind) {
        if (frameSlot.getKind() == FrameSlotKind.Illegal) {
            CompilerDirectives.transferToInterpreter();
            frameSlot.setKind(kind);
            return true;
        }
        return false;
    }
}
