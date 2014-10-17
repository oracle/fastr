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
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
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
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.RPromise.Closure;
import com.oracle.truffle.r.runtime.data.RPromise.PromiseProfile;
import com.oracle.truffle.r.runtime.data.model.*;

public abstract class ReadVariableNode extends RNode implements VisibilityController {

    protected final PromiseProfile promiseProfile = new PromiseProfile();
    private final ConditionProfile isPromiseProfile = ConditionProfile.createBinaryProfile();
    private final BranchProfile unexpectedMissingProfile = new BranchProfile();

    /**
     * The {@link ReadProperties} of this read node.
     */
    protected final ReadProperties props;

    private ReadVariableNode(ReadProperties props) {
        this.props = props;
    }

    private ReadVariableNode(ReadVariableNode prev) {
        this.props = prev.props;
    }

    public abstract Object execute(VirtualFrame frame, MaterializedFrame enclosingFrame);

    /**
     * Convenience constructor
     *
     * @return {@link #create(String, RType, boolean, boolean)}
     */
    public static ReadVariableNode create(String symbol, boolean shouldCopyValue) {
        return create(symbol, RType.Any, shouldCopyValue);
    }

    /**
     * Convenience constructor
     *
     * @return {@link #create(String, boolean)}
     */
    public static ReadVariableNode create(Object symbol, boolean shouldCopyValue) {
        return create(symbol.toString(), shouldCopyValue);
    }

    /**
     * Convenience constructor
     *
     * @return {@link #create(String, RType, boolean, boolean)}
     */
    public static ReadVariableNode create(SourceSection src, String symbol, RType mode, boolean shouldCopyValue) {
        ReadVariableNode rvn = create(symbol, mode, shouldCopyValue);
        rvn.assignSourceSection(src);
        return rvn;
    }

    /**
     * Convenience constructor
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
     * Creates every (regular) {@link ReadVariableNode} out there.
     *
     * @param symbolStr The symbol the {@link ReadVariableNode} is meant to resolve
     * @param mode The mode of the variable
     * @param shouldCopyValue Copy semantics
     * @param isSuper Whether the variable resides in the local frame or not
     * @param readMissing Whether the {@link ReadVariableNode} should read {@link RMissing#instance}
     *            or fail with an error
     * @param forcePromise Whether to force {@link RPromise} for type-checking or not
     * @return The appropriate implementation of {@link ReadVariableNode}
     */
    public static ReadVariableNode create(String symbolStr, RType mode, boolean shouldCopyValue, boolean isSuper, boolean readMissing, boolean forcePromise) {
        Symbol symbol = Symbol.create(symbolStr);
        ReadProperties props = new ReadProperties(symbol, mode, shouldCopyValue, isSuper, readMissing, forcePromise);

        ReadVariableNode rvn = null;
        if (isSuper) {
            rvn = new UnresolvedReadVariableNode(props);
        } else {
            rvn = new UnResolvedReadLocalVariableNode(props);
        }

        return ResolvePromiseNodeFactory.create(props, rvn);
    }

    /**
     * A simple container that holds the properties of a given {@link ReadVariableNode}. It not only
     * what to read ({@link #symbol} , {@link #mode}), but also how ({@link #copyValue}) to do so
     * and how to react on corner cases ( {@link #forcePromise}) or errors ({@link #readMissing}).
     */
    protected static final class ReadProperties {
        /**
         * The {@link Symbol} this {@link ReadVariableNode} (hierarchy) is meant to read
         */
        private final Symbol symbol;

        /**
         * The expected mode of the value to read. Checked in
         * {@link ReadVariableNode#checkType(VirtualFrame, Object)}.
         */
        private final RType mode;

        /**
         * In case this read operation is the one used to read a vector prior to updating one of its
         * elements, the vector must be copied to the local frame if it is found in an enclosing
         * frame.
         */
        private final boolean copyValue;

        /**
         * Whether to look in parent frames for the appropriate {@link FrameSlot} or not.
         */
        private final boolean isSuper;

        /**
         * Whether a value of {@link RMissing#instance} shoudl raise an error when encountered in
         * {@link ReadVariableNode#checkType(VirtualFrame, Object)}. Defaults to <code>false</code>.
         */
        private final boolean readMissing;

        /**
         * Whether a {@link RPromise} should be force if it's encountered in
         * {@link ReadVariableNode#checkType(VirtualFrame, Object)}. Defaults to <code>false</code>.
         */
        private final boolean forcePromise;

        protected ReadProperties(Symbol symbol, RType mode, boolean shouldCopyValue, boolean isSuper, boolean readMissing, boolean forcePromise) {
            assert symbol != null;
            this.symbol = symbol;
            this.mode = mode;
            this.copyValue = shouldCopyValue;
            this.isSuper = isSuper;
            this.readMissing = readMissing;
            this.forcePromise = forcePromise;
        }
    }

    protected ReadProperties getProps() {
        return props;
    }

    /**
     * @return {@link ReadProperties#symbol}
     */
    public Symbol getSymbol() {
        return props.symbol;
    }

    /**
     * @return {@link ReadProperties#symbol}, {@link Symbol#getName()}
     */
    public String getSymbolName() {
        return props.symbol.getName();
    }

    /**
     * @return {@link ReadProperties#mode}
     */
    public RType getMode() {
        return props.mode;
    }

    /**
     * @return {@link ReadProperties#copyValue}
     */
    public boolean getCopyValue() {
        return props.copyValue;
    }

    /**
     * @return {@link ReadProperties#isSuper}
     */
    public boolean getIsSuper() {
        return props.isSuper;
    }

    /**
     * @return {@link ReadProperties#readMissing}
     */
    public boolean getReadMissing() {
        return props.readMissing;
    }

    /**
     * @return {@link ReadProperties#forcePromise}
     */
    public boolean getForcePromise() {
        return props.forcePromise;
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
     * @return see above
     */
    protected boolean checkType(VirtualFrame frame, Object objArg) {
        final RType type = props.mode;
        final boolean readMissing = props.readMissing;
        final boolean forcePromise = props.forcePromise;

        Object obj = objArg;
        if (obj == RMissing.instance && !readMissing && !getSymbol().isVarArg()) {
            unexpectedMissingProfile.enter();
            SourceSection callSrc = RArguments.getCallSourceSection(frame);
            throw RError.error(callSrc, RError.Message.ARGUMENT_MISSING, getSymbol());
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

    @NodeChild(value = "readNode", type = ReadVariableNode.class)
    public abstract static class ResolvePromiseNode extends ReadVariableNode {

        private final ValueProfile promiseFrameProfile = ValueProfile.createClassProfile();

        @Child private InlineCacheNode<VirtualFrame, RNode> promiseExpressionCache = InlineCacheNode.createExpression(3);
        @Child private InlineCacheNode<Frame, Closure> promiseClosureCache = InlineCacheNode.createPromise(3);

        protected ResolvePromiseNode(ReadProperties props) {
            super(props);
        }

        protected ResolvePromiseNode(ResolvePromiseNode prev) {
            this(prev.getProps());
        }

        public abstract ReadVariableNode getReadNode();

        @Specialization
        public Object doValue(VirtualFrame frame, RPromise promise) {
            if (promise.isEvaluated(promiseProfile)) {
                return promise.getValue();
            }

            if (promise.isEagerPromise(promiseProfile)) {
                return promise.evaluate(frame, promiseProfile);
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

    public static final class UnresolvedReadVariableNode extends ReadVariableNode {
        public UnresolvedReadVariableNode(ReadProperties props) {
            super(props);
        }

        @Override
        public Object execute(VirtualFrame frame, MaterializedFrame enclosingFrame) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            if (enclosingFrame != null) {
                ReadSuperVariableNode readSuper = props.copyValue ? ReadAndCopySuperVariableNodeFactory.create(props, (AccessEnclosingFrameNode) null, FrameSlotNode.create(getSymbolName()))
                                : ReadSuperVariableNodeFactory.create(props, (AccessEnclosingFrameNode) null, FrameSlotNode.create(getSymbolName()));
                ReadVariableMaterializedNode readNode = new ReadVariableMaterializedNode(readSuper, new UnresolvedReadVariableNode(props));
                return replace(readNode).execute(frame, enclosingFrame);
            } else {
                return replace(resolveNonFrame()).execute(frame);
            }
        }

        private ReadVariableNode resolveNonFrame() {
            RFunction lookupResult = RContext.getEngine().lookupBuiltin(RRuntime.toString(getSymbol()));
            if (lookupResult != null) {
                return BuiltinFunctionVariableNodeFactory.create(props, lookupResult);
            } else {
                return UnknownVariableNodeFactory.create(props);
            }
        }

        @Override
        public Object execute(VirtualFrame frame) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            ArrayList<Assumption> assumptions = allMissingAssumptions(frame);
            ReadVariableNode readNode;
            if (assumptions == null) {
                // Found variable in one of the frames; build inline cache.
                ReadLocalVariableNode actualReadNode = ReadLocalVariableNodeFactory.create(props, FrameSlotNode.create(getSymbolName()));
                readNode = new ReadVariableVirtualNode(actualReadNode, new UnresolvedReadVariableNode(props));
            } else {
                // Symbol is missing in all frames; bundle assumption checks and access builtin.
                readNode = new ReadVariableNonFrameNode(assumptions, resolveNonFrame(), new UnresolvedReadVariableNode(props));
            }
            return replace(readNode).execute(frame);
        }

        private ArrayList<Assumption> allMissingAssumptions(VirtualFrame frame) {
            ArrayList<Assumption> assumptions = new ArrayList<>();
            Frame currentFrame = frame;
            do {
                FrameSlot frameSlot = FrameSlotNode.findFrameSlot(currentFrame, RRuntime.toString(getSymbol()));
                if (frameSlot != null) {
                    assumptions = null;
                    break;
                }
                assumptions.add(FrameSlotNode.getAssumption(currentFrame, getSymbolName()));
                currentFrame = RArguments.getEnclosingFrame(currentFrame);
            } while (currentFrame != null);
            return assumptions;
        }
    }

    public static final class ReadVariableNonFrameNode extends ReadVariableNode {

        @Child private ReadVariableNode readNode;
        @Child private UnresolvedReadVariableNode unresolvedNode;
        private final Assumption[] absentFrameSlotAssumptions;

        private ReadVariableNonFrameNode(List<Assumption> assumptions, ReadVariableNode readNode, UnresolvedReadVariableNode unresolvedNode) {
            super(readNode.props);
            this.readNode = readNode;
            this.unresolvedNode = unresolvedNode;
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
            throw RInternalError.shouldNotReachHere();
        }
    }

    public static final class ReadVariableVirtualNode extends ReadVariableNode {

        @Child private ReadLocalVariableNode readNode;
        @Child private ReadVariableNode nextNode;

        private final BranchProfile hasValueProfile = new BranchProfile();

        ReadVariableVirtualNode(ReadLocalVariableNode readNode, ReadVariableNode nextNode) {
            super(readNode.props);
            this.readNode = readNode;
            this.nextNode = nextNode;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            controlVisibility();
            if (readNode.getFrameSlotNode().hasValue(frame)) {
                hasValueProfile.enter();
                Object result = readNode.execute(frame);
                if (checkType(frame, result)) {
                    return result;
                }
            }
            return nextNode.execute(frame, RArguments.getEnclosingFrame(frame));
        }

        @Override
        public Object execute(VirtualFrame frame, MaterializedFrame enclosingFrame) {
            throw RInternalError.shouldNotReachHere();
        }
    }

    public static final class UnResolvedReadLocalVariableNode extends ReadVariableNode {
        @Child private ReadLocalVariableNode node;

        UnResolvedReadLocalVariableNode(ReadProperties props) {
            super(props);
        }

        public static ReadVariableNode create(Symbol symbol, RType mode, boolean shouldCopyValue, boolean readMissing) {
            ReadProperties props = new ReadProperties(symbol, mode, shouldCopyValue, false, readMissing, false);
            return new UnResolvedReadLocalVariableNode(props);
        }

        @Override
        public Object execute(VirtualFrame frame) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            controlVisibility();
            node = insert(ReadLocalVariableNodeFactory.create(props, FrameSlotNode.create(getSymbolName())));
            if (node.getFrameSlotNode().hasValue(frame)) {
                Object result = node.execute(frame);
                if (checkType(frame, result)) {
                    replace(node);
                    return result;
                }
            }
            return replace(UnknownVariableNodeFactory.create(props)).execute(frame);
        }

        @Override
        public Object execute(VirtualFrame frame, MaterializedFrame enclosingFrame) {
            throw RInternalError.shouldNotReachHere();
        }
    }

    public static final class ReadVariableSuperMaterializedNode extends ReadVariableNode {
        @Child private ReadVariableNode readNode;

        public ReadVariableSuperMaterializedNode(ReadVariableNode readNode) {
            super(readNode.getProps());
            this.readNode = readNode;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            controlVisibility();
            return readNode.execute(frame, RArguments.getEnclosingFrame(frame));
        }

        @Override
        public Object execute(VirtualFrame frame, MaterializedFrame enclosingFrame) {
            throw RInternalError.shouldNotReachHere();
        }

        public static ReadVariableNode create(SourceSection src, String symbolStr, RType mode) {
            Symbol symbol = Symbol.create(symbolStr);
            ReadProperties props = new ReadProperties(symbol, mode, false, true, false, false);
            ReadVariableNode rvn = new UnresolvedReadVariableNode(props);
            rvn.assignSourceSection(src);
            return ResolvePromiseNodeFactory.create(props, new ReadVariableSuperMaterializedNode(rvn));
        }
    }

    public static final class ReadVariableMaterializedNode extends ReadVariableNode {

        @Child private ReadSuperVariableNode readNode;
        @Child private ReadVariableNode nextNode;

        private final ValueProfile frameTypeProfile = ValueProfile.createClassProfile();
        private final BranchProfile hasValueProfile = new BranchProfile();

        protected ReadVariableMaterializedNode(ReadSuperVariableNode readNode, ReadVariableNode nextNode) {
            super(readNode.getProps());
            this.readNode = readNode;
            this.nextNode = nextNode;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            throw RInternalError.shouldNotReachHere();
        }

        @Override
        public Object execute(VirtualFrame frame, MaterializedFrame enclosingFrame) {
            controlVisibility();
            MaterializedFrame typedEnclosingFrame = frameTypeProfile.profile(enclosingFrame);
            if (readNode.getFrameSlotNode().hasValue(typedEnclosingFrame)) {
                hasValueProfile.enter();
                Object result = readNode.execute(frame, typedEnclosingFrame);
                if (checkType(frame, result)) {
                    return result;
                }
            }
            return nextNode.execute(frame, RArguments.getEnclosingFrame(typedEnclosingFrame));
        }
    }

    @NodeChild(value = "frameSlotNode", type = FrameSlotNode.class)
    public abstract static class ReadLocalVariableNode extends ReadVariableNode {

        private final ValueProfile frameProfile = ValueProfile.createClassProfile();

        protected ReadLocalVariableNode(ReadProperties props) {
            super(props);
        }

        protected ReadLocalVariableNode(ReadLocalVariableNode prev) {
            super(prev.props);
        }

        protected abstract FrameSlotNode getFrameSlotNode();

        @Specialization(rewriteOn = FrameSlotTypeException.class)
        protected byte doLogical(VirtualFrame frame, FrameSlot frameSlot) throws FrameSlotTypeException {
            controlVisibility();
            return frameProfile.profile(frame).getByte(frameSlot);
        }

        @Specialization(rewriteOn = FrameSlotTypeException.class)
        protected int doInteger(VirtualFrame frame, FrameSlot frameSlot) throws FrameSlotTypeException {
            controlVisibility();
            return frameProfile.profile(frame).getInt(frameSlot);
        }

        @Specialization(rewriteOn = FrameSlotTypeException.class)
        protected double doDouble(VirtualFrame frame, FrameSlot frameSlot) throws FrameSlotTypeException {
            controlVisibility();
            return frameProfile.profile(frame).getDouble(frameSlot);
        }

        @Specialization
        protected Object doObject(VirtualFrame frame, FrameSlot frameSlot) {
            controlVisibility();
            try {
                return frameProfile.profile(frame).getObject(frameSlot);
            } catch (FrameSlotTypeException e) {
                throw new IllegalStateException();
            }
        }
    }

    @SuppressWarnings("unused")
    @NodeChildren({@NodeChild(value = "enclosingFrame", type = AccessEnclosingFrameNode.class), @NodeChild(value = "frameSlotNode", type = FrameSlotNode.class)})
    public abstract static class ReadSuperVariableNode extends ReadVariableNode {

        protected ReadSuperVariableNode(ReadProperties props) {
            super(props);
        }

        protected ReadSuperVariableNode(ReadSuperVariableNode prev) {
            super(prev.getProps());
        }

        protected abstract FrameSlotNode getFrameSlotNode();

        @Specialization(rewriteOn = FrameSlotTypeException.class)
        protected byte doLogical(VirtualFrame frame, MaterializedFrame enclosingFrame, FrameSlot frameSlot) throws FrameSlotTypeException {
            controlVisibility();
            return enclosingFrame.getByte(frameSlot);
        }

        @Specialization(rewriteOn = FrameSlotTypeException.class)
        protected int doInteger(VirtualFrame frame, MaterializedFrame enclosingFrame, FrameSlot frameSlot) throws FrameSlotTypeException {
            controlVisibility();
            return enclosingFrame.getInt(frameSlot);
        }

        @Specialization(rewriteOn = FrameSlotTypeException.class)
        protected double doDouble(VirtualFrame frame, MaterializedFrame enclosingFrame, FrameSlot frameSlot) throws FrameSlotTypeException {
            controlVisibility();
            return enclosingFrame.getDouble(frameSlot);
        }

        @Specialization
        protected Object doObject(VirtualFrame frame, MaterializedFrame enclosingFrame, FrameSlot frameSlot) {
            controlVisibility();
            try {
                return enclosingFrame.getObject(frameSlot);
            } catch (FrameSlotTypeException e) {
                throw new IllegalStateException();
            }
        }
    }

    public abstract static class ReadAndCopySuperVariableNode extends ReadSuperVariableNode {

        protected ReadAndCopySuperVariableNode(ReadProperties props) {
            super(props);
        }

        protected ReadAndCopySuperVariableNode(ReadAndCopySuperVariableNode prev) {
            super(prev.getProps());
        }

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
                throw new IllegalStateException();
            }
        }

    }

    @NodeField(name = "function", type = RFunction.class)
    public abstract static class BuiltinFunctionVariableNode extends ReadVariableNode {

        protected BuiltinFunctionVariableNode(ReadProperties props) {
            super(props);
        }

        public static BuiltinFunctionVariableNode create(RFunction function, Symbol symbol) {
            ReadProperties props = new ReadProperties(symbol, RType.Function, false, false, false, false);
            return BuiltinFunctionVariableNodeFactory.create(props, function);
        }

        public abstract RFunction getFunction();

        @Specialization
        protected Object doObject(@SuppressWarnings("unused") VirtualFrame frame) {
            controlVisibility();
            return getFunction();
        }
    }

    public abstract static class UnknownVariableNode extends ReadVariableNode {

        protected UnknownVariableNode(ReadProperties props) {
            super(props);
        }

        @Specialization
        protected Object doObject() {
            controlVisibility();
            throw RError.error(getMode() == RType.Function ? RError.Message.UNKNOWN_FUNCTION : RError.Message.UNKNOWN_OBJECT, getSymbol());
        }
    }
}
