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
package com.oracle.truffle.r.nodes.access;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.access.WriteVariableNodeFactory.ResolvedWriteLocalVariableNodeGen;
import com.oracle.truffle.r.nodes.access.WriteVariableNodeFactory.UnresolvedWriteLocalVariableNodeGen;
import com.oracle.truffle.r.nodes.access.WriteVariableNodeFactory.WriteSuperVariableNodeGen;
import com.oracle.truffle.r.nodes.instrument.CreateWrapper;
import com.oracle.truffle.api.instrument.ProbeNode;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.RDeparse.State;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.env.*;
import com.oracle.truffle.r.runtime.env.frame.*;

import static com.oracle.truffle.r.runtime.env.frame.FrameSlotChangeMonitor.findOrAddFrameSlot;

@NodeChild(value = "rhs", type = RNode.class)
@NodeFields({@NodeField(name = "argWrite", type = boolean.class), @NodeField(name = "name", type = String.class)})
@CreateWrapper
public abstract class WriteVariableNode extends RNode implements VisibilityController {

    public enum Mode {

        REGULAR,
        COPY,
        INVISIBLE,
        TEMP
    }

    public abstract boolean isArgWrite();

    public abstract String getName();

    public abstract RNode getRhs();

    private final ConditionProfile isCurrentProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile isShareableProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile isTemporaryProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile isSharedProfile = ConditionProfile.createBinaryProfile();

    private final BranchProfile initialSetKindProfile = BranchProfile.create();

    @Override
    public boolean isSyntax() {
        return !isArgWrite();
    }

    @Override
    public final boolean getVisibility() {
        return false;
    }

    // setting value of the mode parameter to COPY is meant to induce creation of a copy
    // of the RHS; this needed for the implementation of the replacement forms of
    // builtin functions whose last argument can be mutated; for example, in
    // "dimnames(x)<-list(1)", the assigned value list(1) must become list("1"), with the latter
    // value returned as a result of the call;
    // TODO: is there a better way than to eagerly create a copy of RHS?
    // the above, however, is not necessary for vector updates, which never coerces RHS to a
    // different type; in this case we set the mode parameter to INVISIBLE is meant to prevent
    // changing state altogether
    // setting value of the mode parameter to TEMP is meant to modify how the state is changed; this
    // is needed for the replacement forms of vector updates where a vector is assigned to a
    // temporary (visible) variable and then, again, to the original variable (which would cause the
    // vector to be copied each time);
    protected final Object shareObjectValue(Frame frame, FrameSlot frameSlot, Object value, Mode mode, boolean isSuper) {
        Object newValue = value;
        if (!isArgWrite()) {
            // for the meaning of INVISIBLE mode see the comment preceding the current method
            if (mode != Mode.INVISIBLE && !isCurrentProfile.profile(isCurrentValue(frame, frameSlot, value))) {
                if (isShareableProfile.profile(value instanceof RShareable)) {
                    RShareable rShareable = (RShareable) value;
                    if (isTemporaryProfile.profile(rShareable.isTemporary())) {
                        if (mode == Mode.COPY) {
                            RShareable shareableCopy = rShareable.copy();
                            newValue = shareableCopy;
                        } else {
                            rShareable.markNonTemporary();
                        }
                    } else if (isSharedProfile.profile(rShareable.isShared())) {
                        RShareable shareableCopy = rShareable.copy();
                        if (mode != Mode.COPY) {
                            shareableCopy.markNonTemporary();
                        }
                        newValue = shareableCopy;
                    } else {
                        if (mode == Mode.COPY) {
                            RShareable shareableCopy = rShareable.copy();
                            newValue = shareableCopy;
                        } else if (mode != Mode.TEMP || isSuper) {
                            // mark shared when assigning to the enclosing frame as there must
                            // be a distinction between variables with the same name defined in
                            // different scopes, for example to correctly support:
                            // x<-1:3; f<-function() { x[2]<-10; x[2]<<-100; x[2]<-1000 } ; f()

                            rShareable.makeShared();
                        }
                    }
                }
            }
        }
        return newValue;
    }

    protected void deparseHelper(State state, String op) {
        if (!isArgWrite()) {
            state.append(getName());
            RNode rhs = getRhs();
            if (rhs != null) {
                state.append(op);
                getRhs().deparse(state);
            }
        }
    }

    @Override
    public RNode substitute(REnvironment env) {
        String name = getName();
        RNode nameSub = RASTUtils.substituteName(name, env);
        if (nameSub != null) {
            name = RASTUtils.expectName(nameSub);
        }
        RNode rhsSub = null;
        if (getRhs() != null) {
            rhsSub = getRhs().substitute(env);
        }
        return create(name, rhsSub, false, this instanceof WriteSuperVariableNode);
    }

    private static boolean isCurrentValue(Frame frame, FrameSlot frameSlot, Object value) {
        try {
            return frame.isObject(frameSlot) && frame.getObject(frameSlot) == value;
        } catch (FrameSlotTypeException ex) {
            throw RInternalError.shouldNotReachHere();
        }
    }

    public static WriteVariableNode create(String name, RNode rhs, boolean isArgWrite, boolean isSuper, Mode mode) {
        if (!isSuper) {
            return UnresolvedWriteLocalVariableNodeGen.create(rhs, isArgWrite, name, mode);
        } else {
            assert !isArgWrite;
            return new UnresolvedWriteSuperVariableNode(rhs, name, mode);
        }
    }

    public static WriteVariableNode create(String name, RNode rhs, boolean isArgWrite, boolean isSuper) {
        return create(name, rhs, isArgWrite, isSuper, Mode.REGULAR);
    }

    public static WriteVariableNode create(SourceSection src, String name, RNode rhs, boolean isArgWrite, boolean isSuper) {
        WriteVariableNode wvn = create(name, rhs, isArgWrite, isSuper, Mode.REGULAR);
        wvn.assignSourceSection(src);
        return wvn;
    }

    public abstract void execute(VirtualFrame frame, Object value);

    @NodeField(name = "mode", type = Mode.class)
    @NodeInfo(cost = NodeCost.UNINITIALIZED)
    public abstract static class UnresolvedWriteLocalVariableNode extends WriteVariableNode {

        public abstract Mode getMode();

        @Specialization
        protected byte doLogical(VirtualFrame frame, byte value) {
            resolveAndSet(frame, value, FrameSlotKind.Byte);
            return value;
        }

        @Specialization
        protected int doInteger(VirtualFrame frame, int value) {
            resolveAndSet(frame, value, FrameSlotKind.Int);
            return value;
        }

        @Specialization
        protected double doDouble(VirtualFrame frame, double value) {
            resolveAndSet(frame, value, FrameSlotKind.Double);
            return value;
        }

        @Specialization
        protected Object doObject(VirtualFrame frame, Object value) {
            resolveAndSet(frame, value, FrameSlotKind.Object);
            return value;
        }

        private void resolveAndSet(VirtualFrame frame, Object value, FrameSlotKind initialKind) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            if (getName().isEmpty()) {
                throw RError.error(RError.Message.ZERO_LENGTH_VARIABLE);
            }
            FrameSlot frameSlot = findOrAddFrameSlot(frame.getFrameDescriptor(), getName(), initialKind);
            replace(ResolvedWriteLocalVariableNode.create(getRhs(), this.isArgWrite(), getName(), frameSlot, getMode())).execute(frame, value);
        }

        @Override
        public void deparse(State state) {
            deparseHelper(state, " <- ");
        }
    }

    @NodeFields({@NodeField(name = "frameSlot", type = FrameSlot.class), @NodeField(name = "mode", type = Mode.class)})
    public abstract static class ResolvedWriteLocalVariableNode extends WriteVariableNode {

        private final ValueProfile storedObjectProfile = ValueProfile.createClassProfile();
        private final BranchProfile invalidateProfile = BranchProfile.create();

        public abstract Mode getMode();

        public static ResolvedWriteLocalVariableNode create(RNode rhs, boolean isArgWrite, String name, FrameSlot frameSlot, Mode mode) {
            return ResolvedWriteLocalVariableNodeGen.create(rhs, isArgWrite, name, frameSlot, mode);
        }

        @Specialization(guards = "isFrameLogicalKind")
        protected byte doLogical(VirtualFrame frame, FrameSlot frameSlot, byte value) {
            controlVisibility();
            FrameSlotChangeMonitor.setByteAndInvalidate(frame, frameSlot, value, false, invalidateProfile);
            return value;
        }

        @Specialization(guards = "isFrameIntegerKind")
        protected int doInteger(VirtualFrame frame, FrameSlot frameSlot, int value) {
            controlVisibility();
            FrameSlotChangeMonitor.setIntAndInvalidate(frame, frameSlot, value, false, invalidateProfile);
            return value;
        }

        @Specialization(guards = "isFrameDoubleKind")
        protected double doDouble(VirtualFrame frame, FrameSlot frameSlot, double value) {
            controlVisibility();
            FrameSlotChangeMonitor.setDoubleAndInvalidate(frame, frameSlot, value, false, invalidateProfile);
            return value;
        }

        @Specialization
        protected Object doObject(VirtualFrame frame, FrameSlot frameSlot, Object value) {
            controlVisibility();
            Object newValue = shareObjectValue(frame, frameSlot, storedObjectProfile.profile(value), getMode(), false);
            FrameSlotChangeMonitor.setObjectAndInvalidate(frame, frameSlot, newValue, false, invalidateProfile);
            return value;
        }

        protected boolean isFrameLogicalKind(FrameSlot frameSlot, @SuppressWarnings("unused") byte value) {
            return isLogicalKind(frameSlot);
        }

        protected boolean isFrameIntegerKind(FrameSlot frameSlot, @SuppressWarnings("unused") int value) {
            return isIntegerKind(frameSlot);
        }

        protected boolean isFrameDoubleKind(FrameSlot frameSlot, @SuppressWarnings("unused") double value) {
            return isDoubleKind(frameSlot);
        }

        @Override
        public void deparse(State state) {
            deparseHelper(state, " <- ");
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

        @Override
        public void deparse(State state) {
            deparseHelper(state, " <<- ");
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
        public String getName() {
            return writeNode.getName();
        }

        @Override
        public RNode getRhs() {
            return rhs;
        }

        @Override
        public void execute(VirtualFrame frame, Object value, MaterializedFrame enclosingFrame) {
            controlVisibility();
            if (writeNode.getFrameSlotNode().hasValue(enclosingFrame)) {
                writeNode.execute(frame, value, enclosingFrame);
            } else {
                MaterializedFrame superFrame = RArguments.getEnclosingFrame(enclosingFrame);
                if (superFrame == null) {
                    // Might be the case if "{ x <<- 42 }": This is in glovalEnv!
                    superFrame = REnvironment.globalEnv().getFrame();
                }
                nextNode.execute(frame, value, superFrame);
            }
        }

        @Override
        public void execute(VirtualFrame frame, Object value) {
            controlVisibility();
            assert RArguments.getEnclosingFrame(frame) != null;
            execute(frame, value, RArguments.getEnclosingFrame(frame));
        }

        @Override
        public boolean isArgWrite() {
            return false;
        }
    }

    public static final class UnresolvedWriteSuperVariableNode extends AbstractWriteSuperVariableNode {

        @Child private RNode rhs;
        private final String symbol;
        private final WriteVariableNode.Mode mode;

        public UnresolvedWriteSuperVariableNode(RNode rhs, String symbol, WriteVariableNode.Mode mode) {
            this.rhs = rhs;
            this.symbol = symbol;
            this.mode = mode;
        }

        @Override
        public String getName() {
            return symbol;
        }

        @Override
        public RNode getRhs() {
            return rhs;
        }

        @Override
        public void execute(VirtualFrame frame, Object value, MaterializedFrame enclosingFrame) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            if (getName().isEmpty()) {
                throw RError.error(RError.Message.ZERO_LENGTH_VARIABLE);
            }
            final AbstractWriteSuperVariableNode writeNode;
            if (REnvironment.isGlobalEnvFrame(enclosingFrame)) {
                // we've reached the global scope, do unconditional write
                // if this is the first node in the chain, needs the rhs and enclosingFrame nodes
                AccessEnclosingFrameNode enclosingFrameNode = RArguments.getEnclosingFrame(frame) == enclosingFrame ? AccessEnclosingFrameNodeGen.create(1) : null;
                writeNode = WriteSuperVariableNodeGen.create(getRhs(), enclosingFrameNode, FrameSlotNode.create(findOrAddFrameSlot(enclosingFrame.getFrameDescriptor(), symbol)), this.isArgWrite(),
                                getName(), mode);
            } else {
                WriteSuperVariableNode actualWriteNode = WriteSuperVariableNodeGen.create(null, null, FrameSlotNode.create(symbol), this.isArgWrite(), this.getName(), mode);
                writeNode = new WriteSuperVariableConditionalNode(actualWriteNode, new UnresolvedWriteSuperVariableNode(null, symbol, mode), getRhs());
            }
            replace(writeNode).execute(frame, value, enclosingFrame);
        }

        @Override
        public void execute(VirtualFrame frame, Object value) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            MaterializedFrame enclosingFrame = RArguments.getEnclosingFrame(frame);
            if (enclosingFrame != null) {
                execute(frame, value, enclosingFrame);
            } else {
                // we're in global scope, do a local write instead
                replace(UnresolvedWriteLocalVariableNodeGen.create(getRhs(), this.isArgWrite(), symbol, mode)).execute(frame, value);
            }
        }

        @Override
        public boolean isArgWrite() {
            return false;
        }

    }

    @SuppressWarnings("unused")
    @NodeChildren({@NodeChild(value = "enclosingFrame", type = AccessEnclosingFrameNode.class), @NodeChild(value = "frameSlotNode", type = FrameSlotNode.class)})
    @NodeField(name = "mode", type = Mode.class)
    public abstract static class WriteSuperVariableNode extends AbstractWriteSuperVariableNode {

        private final ValueProfile storedObjectProfile = ValueProfile.createClassProfile();
        private final BranchProfile invalidateProfile = BranchProfile.create();
        private final ValueProfile enclosingFrameProfile = ValueProfile.createClassProfile();

        protected abstract FrameSlotNode getFrameSlotNode();

        public abstract Mode getMode();

        @Specialization(guards = "isFrameLogicalKind")
        protected byte doLogical(VirtualFrame frame, byte value, MaterializedFrame enclosingFrame, FrameSlot frameSlot) {
            controlVisibility();
            FrameSlotChangeMonitor.setByteAndInvalidate(enclosingFrameProfile.profile(enclosingFrame), frameSlot, value, true, invalidateProfile);
            return value;
        }

        @Specialization(guards = "isFrameIntegerKind")
        protected int doInteger(VirtualFrame frame, int value, MaterializedFrame enclosingFrame, FrameSlot frameSlot) {
            controlVisibility();
            FrameSlotChangeMonitor.setIntAndInvalidate(enclosingFrameProfile.profile(enclosingFrame), frameSlot, value, true, invalidateProfile);
            return value;
        }

        @Specialization(guards = "isFrameDoubleKind")
        protected double doDouble(VirtualFrame frame, double value, MaterializedFrame enclosingFrame, FrameSlot frameSlot) {
            controlVisibility();
            FrameSlotChangeMonitor.setDoubleAndInvalidate(enclosingFrameProfile.profile(enclosingFrame), frameSlot, value, true, invalidateProfile);
            return value;
        }

        @Specialization
        protected Object doObject(VirtualFrame frame, Object value, MaterializedFrame enclosingFrame, FrameSlot frameSlot) {
            controlVisibility();
            MaterializedFrame profiledFrame = enclosingFrameProfile.profile(enclosingFrame);
            Object newValue = shareObjectValue(profiledFrame, frameSlot, storedObjectProfile.profile(value), getMode(), true);
            FrameSlotChangeMonitor.setObjectAndInvalidate(profiledFrame, frameSlot, newValue, true, invalidateProfile);
            return value;
        }

        protected boolean isFrameLogicalKind(byte arg0, MaterializedFrame arg1, FrameSlot frameSlot) {
            return isLogicalKind(frameSlot);
        }

        protected boolean isFrameIntegerKind(int arg0, MaterializedFrame arg1, FrameSlot frameSlot) {
            return isIntegerKind(frameSlot);
        }

        protected boolean isFrameDoubleKind(double arg0, MaterializedFrame arg1, FrameSlot frameSlot) {
            return isDoubleKind(frameSlot);
        }
    }

    protected boolean isLogicalKind(FrameSlot frameSlot) {
        return isKind(frameSlot, FrameSlotKind.Boolean);
    }

    protected boolean isIntegerKind(FrameSlot frameSlot) {
        return isKind(frameSlot, FrameSlotKind.Int);
    }

    protected boolean isDoubleKind(FrameSlot frameSlot) {
        return isKind(frameSlot, FrameSlotKind.Double);
    }

    private boolean isKind(FrameSlot frameSlot, FrameSlotKind kind) {
        if (frameSlot.getKind() == kind) {
            return true;
        } else {
            initialSetKindProfile.enter();
            return initialSetKind(frameSlot, kind);
        }
    }

    private static boolean initialSetKind(FrameSlot frameSlot, FrameSlotKind kind) {
        if (frameSlot.getKind() == FrameSlotKind.Illegal) {
            frameSlot.setKind(kind);
            return true;
        }
        return false;
    }

    @Override
    public ProbeNode.WrapperNode createWrapperNode() {
        return new WriteVariableNodeWrapper(this);
    }

}
