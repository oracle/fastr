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

import java.util.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.instrument.ProbeNode.WrapperNode;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.ReadVariableNodeFactory.BuiltinFunctionVariableNodeFactory;
import com.oracle.truffle.r.nodes.access.ReadVariableNodeFactory.ReadAndCopySuperVariableNodeFactory;
import com.oracle.truffle.r.nodes.access.ReadVariableNodeFactory.ReadLocalVariableNodeFactory;
import com.oracle.truffle.r.nodes.access.ReadVariableNodeFactory.ReadSuperVariableNodeFactory;
import com.oracle.truffle.r.nodes.access.ReadVariableNodeFactory.ResolvePromiseNodeFactory;
import com.oracle.truffle.r.nodes.access.ReadVariableNodeFactory.UnknownVariableNodeFactory;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.nodes.instrument.ReadVariableNodeWrapper;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.RDeparse.State;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.RPromise.Closure;
import com.oracle.truffle.r.runtime.data.RPromise.PromiseProfile;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.env.REnvironment;

public abstract class ReadVariableNode extends RNode implements VisibilityController {

    protected final PromiseProfile promiseProfile = new PromiseProfile();
    private final ConditionProfile isPromiseProfile = ConditionProfile.createBinaryProfile();
    private final BranchProfile unexpectedMissingProfile = BranchProfile.create();

    public abstract Object execute(VirtualFrame frame, MaterializedFrame enclosingFrame);

    public abstract String getName();

    /**
     * Convenience method.
     *
     * @return {@link #create(String, RType, boolean, boolean)}
     */
    public static ReadVariableNode create(String symbol, boolean shouldCopyValue) {
        return create(symbol, RType.Any, shouldCopyValue);
    }

    /**
     * Convenience method.
     *
     * @return {@link #create(String, boolean)}
     */
    public static ReadVariableNode create(Object symbol, boolean shouldCopyValue) {
        return create(symbol.toString(), shouldCopyValue);
    }

    /**
     * Convenience method.
     *
     * @return {@link #create(String, RType, boolean, boolean)}
     */
    public static ReadVariableNode create(SourceSection src, String symbol, RType mode, boolean shouldCopyValue) {
        ReadVariableNode rvn = create(symbol, mode, shouldCopyValue);
        rvn.assignSourceSection(src);
        return rvn;
    }

    /**
     * Convenience method.
     *
     * @return {@link #create(String, RType, boolean, boolean)}
     */
    public static ReadVariableNode create(String symbol, RType mode, boolean shouldCopyValue) {
        return create(symbol, mode, shouldCopyValue, true);
    }

    /**
     * @return #create(String, String, boolean, boolean, boolean, boolean), where readMissing and
     *         forcePromise are set to {@code false}
     */
    public static ReadVariableNode create(String symbolStr, RType mode, boolean shouldCopyValue, boolean isSuper) {
        return create(symbolStr, mode, shouldCopyValue, isSuper, false, false);
    }

    /**
     * Creates every {@link ReadVariableNode} out there.
     *
     * @param name The symbol the {@link ReadVariableNode} is meant to resolve
     * @param mode The mode of the variable
     * @param shouldCopyValue Copy semantics
     * @param isSuper Whether the variable resides in the local frame or not
     * @param readMissing Whether the {@link ReadVariableNode} should read {@link RMissing#instance}
     *            or fail with an error
     * @param forcePromise Whether to force {@link RPromise} for type-checking or not
     * @return The appropriate implementation of {@link ReadVariableNode}
     */
    public static ReadVariableNode create(String name, RType mode, boolean shouldCopyValue, boolean isSuper, boolean readMissing, boolean forcePromise) {
        ReadVariableNode rvn = null;
        if (isSuper) {
            rvn = new UnresolvedReadVariableNode(name, mode, readMissing, forcePromise, shouldCopyValue);
        } else {
            rvn = new UnResolvedReadLocalVariableNode(name, mode, readMissing, forcePromise);
        }

        return ResolvePromiseNodeFactory.create(rvn, name);
    }

    /**
     * This method checks the value a RVN just read. It is used to determine whether the value just
     * read matches the expected type or if we have to look in a frame up the lexical chain. It
     * might:
     * <ul>
     * <li>throw an {@link RError}: if 'objArg' is a missing argument and this is not allowed
     * (default, switched by 'readMissing')</li>
     * <li>return {@code true}: if the type of 'objArg' matches the description in 'type'</li>
     * <li>return {@code false}: if the type of 'objArg' does not match the description in 'type'</li>
     * </ul>
     * However, there is the special case of 'objArg' being a {@link RPromise}: Normally, it is
     * expected to match type and simply returns {@code true}. But in case of 'forcePromise' ==
     * {@code true}, the promise is evaluated and the result checked for it's type. This is only
     * used for function lookup, as we need to be sure that we read a function.
     *
     * @param frame The frame to (eventually) evaluate the {@link RPromise} in
     * @param objArg The object to check for proper type
     * @param type The type which is expected
     * @param readMissing Whether reading an instance of {@link RMissing} does not yield an error
     * @param forcePromise Whether a promise should be forced to check its type or not
     * @return see above
     */
    protected boolean checkType(VirtualFrame frame, Object objArg, RType type, boolean readMissing, boolean forcePromise) {
        Object obj = objArg;
        if (obj == RMissing.instance && !readMissing && !ArgumentsTrait.isVarArg(getName())) {
            unexpectedMissingProfile.enter();
            SourceSection callSrc = RArguments.getCallSourceSection(frame);
            throw RError.error(callSrc, RError.Message.ARGUMENT_MISSING, getName());
        }
        if (type == RType.Any) {
            return true;
        }
        if (isPromiseProfile.profile(obj instanceof RPromise)) {
            RPromise promise = (RPromise) obj;
            if (!promise.isEvaluated(promiseProfile)) {
                if (!forcePromise) {
                    // since we do not know what type the evaluates to, it may match.
                    // we recover from a wrong type later
                    return true;
                } else {
                    obj = promise.evaluate(frame, promiseProfile);
                }
            } else {
                obj = promise.getValue();
            }
        }
        if (type == RType.Function || type == RType.Closure || type == RType.Builtin || type == RType.Special) {
            return obj instanceof RFunction;
        }
        if (type == RType.Character) {
            return obj instanceof String;
        }
        if (type == RType.Logical) {
            return obj instanceof Byte;
        }
        if (type == RType.Integer || type == RType.Double || type == RType.Numeric) {
            return obj instanceof Integer || obj instanceof Double;
        }
        return false;
    }

    @Override
    public void deparse(State state) {
        state.append(getName());
    }

    @Override
    public RNode substitute(REnvironment env) {
        RNode result = RASTUtils.substituteName(getName(), env);
        if (result == null) {
            if (this instanceof ResolvePromiseNode) {
                result = ((ResolvePromiseNode) this).getReadNode();
            } else {
                result = this;
            }
            result = NodeUtil.cloneNode(result);
        }
        return result;
    }

    @NodeChild(value = "readNode", type = ReadVariableNode.class)
    @NodeField(name = "name", type = String.class)
    public abstract static class ResolvePromiseNode extends ReadVariableNode {

        private final ValueProfile promiseFrameProfile = ValueProfile.createClassProfile();

        public abstract ReadVariableNode getReadNode();

        @Override
        public abstract String getName();

        @Child private InlineCacheNode<VirtualFrame, RNode> promiseExpressionCache = InlineCacheNode.createExpression(3);
        @Child private InlineCacheNode<Frame, Closure> promiseClosureCache = InlineCacheNode.createPromise(3);

        @Specialization
        public Object doValue(VirtualFrame frame, RPromise promise) {
            if (promise.isEvaluated(promiseProfile)) {
                return promise.getValue();
            }

            if (promise.isInOriginFrame(frame, promiseProfile)) {
                return PromiseHelper.evaluate(frame, promiseExpressionCache, promise, promiseProfile);
            }

            // Check for dependency cycle
            if (promise.isUnderEvaluation(promiseProfile)) {
                SourceSection callSrc = RArguments.getCallSourceSection(frame);
                throw RError.error(callSrc, RError.Message.PROMISE_CYCLE);
            }

            Frame promiseFrame = promiseFrameProfile.profile(promise.getFrame());
            assert promiseFrame != null;
            SourceSection oldCallSource = RArguments.getCallSourceSection(promiseFrame);
            Object newValue;
            try {
                promise.setUnderEvaluation(true);
                RArguments.setCallSourceSection(promiseFrame, RArguments.getCallSourceSection(frame));

                newValue = promiseClosureCache.execute(promiseFrame, promise.getClosure());

                promise.setValue(newValue, promiseProfile);
            } finally {
                RArguments.setCallSourceSection(promiseFrame, oldCallSource);
                promise.setUnderEvaluation(false);
            }
            return newValue;
        }

        @Specialization
        public int doValue(int value) {
            return value;
        }

        @Specialization
        public double doValue(double value) {
            return value;
        }

        @Specialization
        public byte doValue(byte value) {
            return value;
        }

        private final ConditionProfile isPromiseProfile = ConditionProfile.createBinaryProfile();

        /**
         * Catch all calls to {@link #execute(VirtualFrame, MaterializedFrame)} ({@code final} so it
         * is not overridden by the annotation processor) and forward them to {@link #getReadNode()}
         * . The returned object has only to be checked for being a {@link RPromise}, then we're
         * done here.
         */
        @Override
        public final Object execute(VirtualFrame frame, MaterializedFrame enclosingFrame) {
            Object obj = getReadNode().execute(frame, enclosingFrame);
            if (isPromiseProfile.profile(isPromise(obj))) {
                return doValue(frame, (RPromise) obj);
            }
            return obj;
        }

        @Specialization(guards = "!isPromise")
        public Object doValue(Object obj) {
            return obj;
        }

        public boolean isPromise(Object obj) {
            return obj instanceof RPromise;
        }

    }

    private interface HasMode {
        RType getMode();
    }

    public static final class UnresolvedReadVariableNode extends ReadVariableNode implements HasMode {

        // TODO It seems a refactoring would be appropriate to encapsulate all fields (symbol, mode,
        // readMissing, forcePromise, copyValue) into a single class to reduce clutter and
        // repetition throughout RVN hierarchy
        private final String name;
        private final RType mode;
        private final boolean readMissing;
        private final boolean forcePromise;

        /**
         * In case this read operation is the one used to read a vector prior to updating one of its
         * elements, the vector must be copied to the local frame if it is found in an enclosing
         * frame.
         */
        @CompilationFinal private boolean copyValue;

        @Override
        public boolean isSyntax() {
            return true;
        }

        public void setCopyValue(boolean c) {
            copyValue = c;
        }

        public UnresolvedReadVariableNode(String name, RType mode, boolean readMissing, boolean forcePromise, boolean copyValue) {
            this.name = name;
            this.mode = mode;
            this.readMissing = readMissing;
            this.forcePromise = forcePromise;
            this.copyValue = copyValue;
        }

        @Override
        public Object execute(VirtualFrame frame, MaterializedFrame enclosingFrame) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            if (enclosingFrame != null) {
                ReadSuperVariableNode readSuper = copyValue ? ReadAndCopySuperVariableNodeFactory.create(null, FrameSlotNode.create(name), name) : ReadSuperVariableNodeFactory.create(null,
                                FrameSlotNode.create(name), name);
                ReadVariableMaterializedNode readNode = new ReadVariableMaterializedNode(readSuper, new UnresolvedReadVariableNode(name, mode, readMissing, forcePromise, copyValue), readMissing,
                                forcePromise, mode);
                return replace(readNode).execute(frame, enclosingFrame);
            } else {
                return replace(resolveNonFrame()).execute(frame);
            }
        }

        private ReadVariableNode resolveNonFrame() {
            RFunction lookupResult = RContext.getEngine().lookupBuiltin(RRuntime.toString(name));
            if (lookupResult != null) {
                return BuiltinFunctionVariableNodeFactory.create(lookupResult, name);
            } else {
                return UnknownVariableNodeFactory.create(name, mode, readMissing, forcePromise);
            }
        }

        @Override
        public Object execute(VirtualFrame frame) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            ArrayList<Assumption> assumptions = allMissingAssumptions(frame);
            ReadVariableNode readNode;
            if (assumptions == null) {
                // Found variable in one of the frames; build inline cache.
                ReadLocalVariableNode actualReadNode = ReadLocalVariableNodeFactory.create(FrameSlotNode.create(name), name);
                readNode = new ReadVariableVirtualNode(actualReadNode, new UnresolvedReadVariableNode(name, mode, readMissing, forcePromise, copyValue), mode, readMissing, forcePromise);
            } else {
                // Symbol is missing in all frames; bundle assumption checks and access builtin.
                readNode = new ReadVariableNonFrameNode(assumptions, resolveNonFrame(), new UnresolvedReadVariableNode(name, mode, readMissing, forcePromise, copyValue), name);
            }
            return replace(readNode).execute(frame);
        }

        private ArrayList<Assumption> allMissingAssumptions(VirtualFrame frame) {
            ArrayList<Assumption> assumptions = new ArrayList<>();
            Frame currentFrame = frame;
            do {
                FrameSlot frameSlot = FrameSlotNode.findFrameSlot(currentFrame, RRuntime.toString(name));
                if (frameSlot != null) {
                    assumptions = null;
                    break;
                }
                assumptions.add(FrameSlotNode.getAssumption(currentFrame, name));
                currentFrame = RArguments.getEnclosingFrame(currentFrame);
            } while (currentFrame != null);
            return assumptions;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public RType getMode() {
            return mode;
        }

    }

    public static final class ReadVariableNonFrameNode extends ReadVariableNode {

        @Child private ReadVariableNode readNode;
        @Child private UnresolvedReadVariableNode unresolvedNode;
        private final Assumption[] absentFrameSlotAssumptions;
        private final String name;

        ReadVariableNonFrameNode(List<Assumption> assumptions, ReadVariableNode readNode, UnresolvedReadVariableNode unresolvedNode, String name) {
            this.readNode = readNode;
            this.unresolvedNode = unresolvedNode;
            this.name = name;
            this.absentFrameSlotAssumptions = assumptions.toArray(new Assumption[assumptions.size()]);
        }

        @ExplodeLoop
        @Override
        public Object execute(VirtualFrame frame) {
            controlVisibility();
            try {
                for (Assumption assumption : absentFrameSlotAssumptions) {
                    assumption.check();
                }
            } catch (InvalidAssumptionException e) {
                return replace(unresolvedNode).execute(frame);
            }
            return readNode.execute(frame);
        }

        @Override
        public Object execute(VirtualFrame frame, MaterializedFrame enclosingFrame) {
            controlVisibility();
            throw new UnsupportedOperationException();
        }

        @Override
        public String getName() {
            return name;
        }

    }

    public static final class ReadVariableVirtualNode extends ReadVariableNode implements HasMode {

        @Child private ReadLocalVariableNode readNode;
        @Child private ReadVariableNode nextNode;
        private final RType mode;
        private final boolean readMissing;
        private final boolean forcePromise;

        private final BranchProfile hasValueProfile = BranchProfile.create();

        ReadVariableVirtualNode(ReadLocalVariableNode readNode, ReadVariableNode nextNode, RType mode, boolean readMissing, boolean forcePromise) {
            this.readNode = readNode;
            this.nextNode = nextNode;
            this.mode = mode;
            this.readMissing = readMissing;
            this.forcePromise = forcePromise;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            controlVisibility();
            if (readNode.getFrameSlotNode().hasValue(frame)) {
                hasValueProfile.enter();
                Object result = readNode.execute(frame);
                if (checkType(frame, result, mode, readMissing, forcePromise)) {
                    return result;
                }
            }
            return nextNode.execute(frame, RArguments.getEnclosingFrame(frame));
        }

        @Override
        public Object execute(VirtualFrame frame, MaterializedFrame enclosingFrame) {
            controlVisibility();
            throw new UnsupportedOperationException();
        }

        @Override
        public String getName() {
            return readNode.getName();
        }

        @Override
        public RType getMode() {
            return mode;
        }
    }

    public static final class UnResolvedReadLocalVariableNode extends ReadVariableNode implements HasMode {
        private final String name;
        private final RType mode;
        private final boolean readMissing;
        private final boolean forcePromise;
        @Child private ReadLocalVariableNode node;

        UnResolvedReadLocalVariableNode(String name, RType mode, boolean readMissing, boolean forcePromise) {
            this.name = name;
            this.mode = mode;
            this.readMissing = readMissing;
            this.forcePromise = forcePromise;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            controlVisibility();
            node = insert(ReadLocalVariableNodeFactory.create(FrameSlotNode.create(name), name));
            if (node.getFrameSlotNode().hasValue(frame)) {
                Object result = node.execute(frame);
                if (checkType(frame, result, mode, readMissing, forcePromise)) {
                    replace(node);
                    return result;
                }
            }
            return replace(UnknownVariableNodeFactory.create(name, mode, readMissing, forcePromise)).execute(frame);
        }

        @Override
        public Object execute(VirtualFrame frame, MaterializedFrame enclosingFrame) {
            controlVisibility();
            throw new UnsupportedOperationException();
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public RType getMode() {
            return mode;
        }
    }

    public static final class ReadVariableSuperMaterializedNode extends ReadVariableNode {
        @Child private ReadVariableNode readNode;

        public ReadVariableSuperMaterializedNode(ReadVariableNode readNode) {
            this.readNode = readNode;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            controlVisibility();
            return readNode.execute(frame, RArguments.getEnclosingFrame(frame));
        }

        @Override
        public Object execute(VirtualFrame frame, MaterializedFrame enclosingFrame) {
            controlVisibility();
            throw new UnsupportedOperationException();
        }

        public static ReadVariableNode create(SourceSection src, String name, RType mode) {
            ReadVariableNode rvn = new UnresolvedReadVariableNode(name, mode, false, false, false);
            rvn.assignSourceSection(src);
            return ResolvePromiseNodeFactory.create(new ReadVariableSuperMaterializedNode(rvn), name);
        }

        @Override
        public String getName() {
            return readNode.getName();
        }

    }

    public static final class ReadVariableMaterializedNode extends ReadVariableNode implements HasMode {

        @Child private ReadSuperVariableNode readNode;
        @Child private ReadVariableNode nextNode;
        private final RType mode;
        private final boolean readMissing;
        private final boolean forcePromise;

        private final ValueProfile frameTypeProfile = ValueProfile.createClassProfile();
        private final BranchProfile hasValueProfile = BranchProfile.create();

        ReadVariableMaterializedNode(ReadSuperVariableNode readNode, ReadVariableNode nextNode, boolean readMissing, boolean forcePromise, RType mode) {
            this.readNode = readNode;
            this.nextNode = nextNode;
            this.mode = mode;
            this.readMissing = readMissing;
            this.forcePromise = forcePromise;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            controlVisibility();
            throw new UnsupportedOperationException();
        }

        @Override
        public Object execute(VirtualFrame frame, MaterializedFrame enclosingFrame) {
            controlVisibility();
            MaterializedFrame typedEnclosingFrame = frameTypeProfile.profile(enclosingFrame);
            if (readNode.getFrameSlotNode().hasValue(typedEnclosingFrame)) {
                hasValueProfile.enter();
                Object result = readNode.execute(frame, typedEnclosingFrame);
                if (checkType(frame, result, mode, readMissing, forcePromise)) {
                    return result;
                }
            }
            return nextNode.execute(frame, RArguments.getEnclosingFrame(typedEnclosingFrame));
        }

        @Override
        public String getName() {
            return readNode.getName();
        }

        @Override
        public RType getMode() {
            return mode;
        }
    }

    @NodeChild(value = "frameSlotNode", type = FrameSlotNode.class)
    @NodeField(name = "name", type = String.class)
    public abstract static class ReadLocalVariableNode extends ReadVariableNode {

        private final ValueProfile frameProfile = ValueProfile.createClassProfile();

        protected abstract FrameSlotNode getFrameSlotNode();

        @Override
        public abstract String getName();

        @Specialization(guards = "isByte")
        protected byte doLogical(VirtualFrame frame, FrameSlot frameSlot) {
            controlVisibility();
            try {
                return frameProfile.profile(frame).getByte(frameSlot);
            } catch (FrameSlotTypeException e) {
                throw RInternalError.shouldNotReachHere();
            }
        }

        @Specialization(guards = "isInt")
        protected int doInteger(VirtualFrame frame, FrameSlot frameSlot) {
            controlVisibility();
            try {
                return frameProfile.profile(frame).getInt(frameSlot);
            } catch (FrameSlotTypeException e) {
                throw RInternalError.shouldNotReachHere();
            }
        }

        @Specialization(guards = "isDouble")
        protected double doDouble(VirtualFrame frame, FrameSlot frameSlot) {
            controlVisibility();
            try {
                return frameProfile.profile(frame).getDouble(frameSlot);
            } catch (FrameSlotTypeException e) {
                throw RInternalError.shouldNotReachHere();
            }
        }

        @Fallback
        protected Object doObject(VirtualFrame frame, FrameSlot frameSlot) {
            controlVisibility();
            try {
                return frameProfile.profile(frame).getObject(frameSlot);
            } catch (FrameSlotTypeException e) {
                throw RInternalError.shouldNotReachHere();
            }
        }

        protected static boolean isByte(VirtualFrame frame, FrameSlot frameSlot) {
            return frame.isByte(frameSlot);
        }

        protected static boolean isInt(VirtualFrame frame, FrameSlot frameSlot) {
            return frame.isInt(frameSlot);
        }

        protected static boolean isDouble(VirtualFrame frame, FrameSlot frameSlot) {
            return frame.isDouble(frameSlot);
        }

    }

    @SuppressWarnings("unused")
    @NodeChildren({@NodeChild(value = "enclosingFrame", type = AccessEnclosingFrameNode.class), @NodeChild(value = "frameSlotNode", type = FrameSlotNode.class)})
    @NodeField(name = "name", type = String.class)
    public abstract static class ReadSuperVariableNode extends ReadVariableNode {

        protected abstract FrameSlotNode getFrameSlotNode();

        @Override
        public abstract String getName();

        @Specialization(guards = "isByte")
        protected byte doLogical(VirtualFrame frame, MaterializedFrame enclosingFrame, FrameSlot frameSlot) {
            controlVisibility();
            try {
                return enclosingFrame.getByte(frameSlot);
            } catch (FrameSlotTypeException e) {
                throw RInternalError.shouldNotReachHere();
            }
        }

        @Specialization(guards = "isInt")
        protected int doInteger(VirtualFrame frame, MaterializedFrame enclosingFrame, FrameSlot frameSlot) {
            controlVisibility();
            try {
                return enclosingFrame.getInt(frameSlot);
            } catch (FrameSlotTypeException e) {
                throw RInternalError.shouldNotReachHere();
            }
        }

        @Specialization(guards = "isDouble")
        protected double doDouble(VirtualFrame frame, MaterializedFrame enclosingFrame, FrameSlot frameSlot) {
            controlVisibility();
            try {
                return enclosingFrame.getDouble(frameSlot);
            } catch (FrameSlotTypeException e) {
                throw RInternalError.shouldNotReachHere();
            }
        }

        @Fallback
        protected Object doObject(VirtualFrame frame, MaterializedFrame enclosingFrame, FrameSlot frameSlot) {
            controlVisibility();
            try {
                return enclosingFrame.getObject(frameSlot);
            } catch (FrameSlotTypeException e) {
                throw RInternalError.shouldNotReachHere();
            }
        }

        protected static boolean isByte(VirtualFrame frame, MaterializedFrame enclosingFrame, FrameSlot frameSlot) {
            return enclosingFrame.isByte(frameSlot);
        }

        protected static boolean isInt(VirtualFrame frame, MaterializedFrame enclosingFrame, FrameSlot frameSlot) {
            return enclosingFrame.isInt(frameSlot);
        }

        protected static boolean isDouble(VirtualFrame frame, MaterializedFrame enclosingFrame, FrameSlot frameSlot) {
            return enclosingFrame.isDouble(frameSlot);
        }

    }

    public abstract static class ReadAndCopySuperVariableNode extends ReadSuperVariableNode {

        @Override
        @Specialization
        protected Object doObject(VirtualFrame frame, MaterializedFrame enclosingFrame, FrameSlot frameSlot) {
            controlVisibility();
            try {
                Object result = enclosingFrame.getObject(frameSlot);
                if (result instanceof RAbstractVector) {
                    return ((RAbstractVector) result).copy();
                }
                return result;
            } catch (FrameSlotTypeException e) {
                throw RInternalError.shouldNotReachHere();
            }
        }

    }

    @NodeFields(value = {@NodeField(name = "function", type = RFunction.class), @NodeField(name = "name", type = String.class)})
    public abstract static class BuiltinFunctionVariableNode extends ReadVariableNode {

        public abstract RFunction getFunction();

        @Override
        public abstract String getName();

        @Specialization
        protected Object doObject(@SuppressWarnings("unused") VirtualFrame frame) {
            controlVisibility();
            return getFunction();
        }
    }

    @NodeFields({@NodeField(name = "name", type = String.class), @NodeField(name = "mode", type = RType.class), @NodeField(name = "readMissing", type = Boolean.class),
                    @NodeField(name = "forcePromise", type = Boolean.class)})
    public abstract static class UnknownVariableNode extends ReadVariableNode implements HasMode {

        @Override
        public abstract String getName();

        @Override
        public abstract RType getMode();

        public abstract boolean getReadMissing();

        public abstract boolean getForcePromise();

        @Specialization
        protected Object doObject() {
            controlVisibility();
            throw RError.error(getMode() == RType.Function ? RError.Message.UNKNOWN_FUNCTION : RError.Message.UNKNOWN_OBJECT, getName());
        }
    }

    @Override
    public WrapperNode createWrapperNode(RNode node) {
        return new ReadVariableNodeWrapper((ReadVariableNode) node);
    }
}
