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
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.ReadVariableNodeFactory.BuiltinFunctionVariableNodeFactory;
import com.oracle.truffle.r.nodes.access.ReadVariableNodeFactory.ReadAndCopySuperVariableNodeFactory;
import com.oracle.truffle.r.nodes.access.ReadVariableNodeFactory.ReadLocalVariableNodeFactory;
import com.oracle.truffle.r.nodes.access.ReadVariableNodeFactory.ReadSuperVariableNodeFactory;
import com.oracle.truffle.r.nodes.access.ReadVariableNodeFactory.ResolvePromiseNodeFactory;
import com.oracle.truffle.r.nodes.access.ReadVariableNodeFactory.UnknownVariableNodeFactory;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

public abstract class ReadVariableNode extends RNode implements VisibilityController {

    public abstract Object execute(VirtualFrame frame, MaterializedFrame enclosingFrame);

    /**
     * Convenience method
     *
     * @return {@link #create(String, String, boolean, boolean)}
     */
    public static ReadVariableNode create(String symbol, boolean shouldCopyValue) {
        return create(symbol, RRuntime.TYPE_ANY, shouldCopyValue);
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
     * Convenience method
     *
     * @return {@link #create(String, String, boolean, boolean)}
     */
    public static ReadVariableNode create(SourceSection src, String symbol, String mode, boolean shouldCopyValue) {
        ReadVariableNode rvn = create(symbol, mode, shouldCopyValue);
        rvn.assignSourceSection(src);
        return rvn;
    }

    /**
     * Convenience method
     *
     * @return {@link #create(String, String, boolean, boolean)}
     */
    public static ReadVariableNode create(String symbol, String mode, boolean shouldCopyValue) {
        return create(symbol, mode, shouldCopyValue, true);
    }

    public static ReadVariableNode create(String symbolStr, String mode, boolean shouldCopyValue, boolean isSuper) {
        return create(symbolStr, mode, shouldCopyValue, isSuper, false);
    }

    /**
     * Creates every {@link ReadVariableNode} out there
     *
     * @param symbolStr The symbol the {@link ReadVariableNode} is meant to resolve
     * @param mode The mode of the variable
     * @param shouldCopyValue Copy semantics
     * @param isSuper Whether the variable resides in the local frame or not
     * @param readMissing Whether the {@link ReadVariableNode} should read {@link RMissing#instance}
     *            or fail with an error
     * @return The appropriate implementation of {@link ReadVariableNode}
     */
    public static ReadVariableNode create(String symbolStr, String mode, boolean shouldCopyValue, boolean isSuper, boolean readMissing) {
        Symbol symbol = Symbol.create(symbolStr);

        ReadVariableNode rvn = null;
        if (isSuper) {
            rvn = new UnresolvedReadVariableNode(symbol, mode, readMissing, shouldCopyValue);
        } else {
            rvn = new UnResolvedReadLocalVariableNode(symbol, mode, readMissing);
        }

        return ResolvePromiseNodeFactory.create(rvn, symbol);
    }

    protected boolean checkType(Object objArg, String type, boolean readMissing) {
        Object obj = objArg;
        if (obj == RMissing.instance && !readMissing && !getSymbol().isVarArg()) {
            throw RError.error(RError.Message.ARGUMENT_MISSING, getSymbol());
        }
        if (type.equals(RRuntime.TYPE_ANY)) {
            return true;
        }
        if (obj instanceof RPromise) {
            RPromise promise = (RPromise) obj;
            if (!promise.isEvaluated()) {
                // since we do not know what type the evaluates to, it may match.
                // we recover from a wrong type later
                return true;
            } else {
                obj = promise.getValue();
            }
        }
        if (type.equals(RRuntime.TYPE_FUNCTION) || type.equals(RRuntime.TYPE_CLOSURE) || type.equals(RRuntime.TYPE_BUILTIN) || type.equals(RRuntime.TYPE_SPECIAL)) {
            return obj instanceof RFunction;
        }
        if (type.equals(RRuntime.TYPE_CHARACTER)) {
            return obj instanceof String;
        }
        if (type.equals(RRuntime.TYPE_LOGICAL)) {
            return obj instanceof Byte;
        }
        if (type.equals(RRuntime.TYPE_INTEGER) || type.equals(RRuntime.TYPE_DOUBLE) || type.equals(RRuntime.TYPE_NUMERIC)) {
            return obj instanceof Integer || obj instanceof Double;
        }
        return false;
    }

    public abstract Symbol getSymbol();

    @NodeChild(value = "readNode", type = ReadVariableNode.class)
    @NodeField(name = "symbol", type = Symbol.class)
    public abstract static class ResolvePromiseNode extends ReadVariableNode {

        public abstract ReadVariableNode getReadNode();

        @Specialization
        public Object doValue(VirtualFrame frame, RPromise promise) {
            return promise.evaluate(frame);
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

        /**
         * Catch all calls to {@link #execute(VirtualFrame, MaterializedFrame)} ({@code final} so it
         * is not overridden by the annotation processor) and forward them to {@link #getReadNode()}
         * . The returned object has only to be checked for being a {@link RPromise}, then we're
         * done here.
         */
        @Override
        public final Object execute(VirtualFrame frame, MaterializedFrame enclosingFrame) {
            Object obj = getReadNode().execute(frame, enclosingFrame);
            if (isPromise(obj)) {
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
        String getMode();

        boolean readMissing();
    }

    public static final class UnresolvedReadVariableNode extends ReadVariableNode implements HasMode {

        private final Symbol symbol;
        private final String mode;
        private final boolean readMissing;

        /**
         * In case this read operation is the one used to read a vector prior to updating one of its
         * elements, the vector must be copied to the local frame if it is found in an enclosing
         * frame.
         */
        @CompilationFinal private boolean copyValue;

        public void setCopyValue(boolean c) {
            copyValue = c;
        }

        public UnresolvedReadVariableNode(Symbol symbol, String mode, boolean readMissing, boolean copyValue) {
            this.symbol = symbol;
            this.mode = mode;
            this.readMissing = readMissing;
            this.copyValue = copyValue;
        }

        @Override
        public Object execute(VirtualFrame frame, MaterializedFrame enclosingFrame) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            if (enclosingFrame != null) {
                ReadSuperVariableNode readSuper = copyValue ? ReadAndCopySuperVariableNodeFactory.create(null, new FrameSlotNode.UnresolvedFrameSlotNode(symbol.getName()), symbol)
                                : ReadSuperVariableNodeFactory.create(null, new FrameSlotNode.UnresolvedFrameSlotNode(symbol.getName()), symbol);
                ReadVariableMaterializedNode readNode = new ReadVariableMaterializedNode(readSuper, new UnresolvedReadVariableNode(symbol, mode, readMissing, copyValue), readMissing, mode);
                return replace(readNode).execute(frame, enclosingFrame);
            } else {
                return replace(resolveNonFrame()).execute(frame);
            }
        }

        private ReadVariableNode resolveNonFrame() {
            RFunction lookupResult = RContext.getEngine().lookupBuiltin(RRuntime.toString(symbol));
            if (lookupResult != null) {
                return BuiltinFunctionVariableNodeFactory.create(lookupResult, symbol);
            } else {
                return UnknownVariableNodeFactory.create(symbol, mode, readMissing);
            }
        }

        @Override
        public Object execute(VirtualFrame frame) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            ArrayList<Assumption> assumptions = allMissingAssumptions(frame);
            ReadVariableNode readNode;
            if (assumptions == null) {
                // Found variable in one of the frames; build inline cache.
                ReadLocalVariableNode actualReadNode = ReadLocalVariableNodeFactory.create(new FrameSlotNode.UnresolvedFrameSlotNode(symbol.getName()), symbol);
                readNode = new ReadVariableVirtualNode(actualReadNode, new UnresolvedReadVariableNode(symbol, mode, readMissing, copyValue), mode, readMissing);
            } else {
                // Symbol is missing in all frames; bundle assumption checks and access builtin.
                readNode = new ReadVariableNonFrameNode(assumptions, resolveNonFrame(), new UnresolvedReadVariableNode(symbol, mode, readMissing, copyValue), symbol);
            }
            return replace(readNode).execute(frame);
        }

        private ArrayList<Assumption> allMissingAssumptions(VirtualFrame frame) {
            ArrayList<Assumption> assumptions = new ArrayList<>();
            Frame currentFrame = frame;
            do {
                FrameSlot frameSlot = FrameSlotNode.findFrameSlot(currentFrame, RRuntime.toString(symbol));
                if (frameSlot != null) {
                    assumptions = null;
                    break;
                }
                assumptions.add(FrameSlotNode.getAssumption(currentFrame, symbol.getName()));
                currentFrame = RArguments.getEnclosingFrame(currentFrame);
            } while (currentFrame != null);
            return assumptions;
        }

        @Override
        public Symbol getSymbol() {
            return symbol;
        }

        @Override
        public String getMode() {
            return mode;
        }

        @Override
        public boolean readMissing() {
            return readMissing;
        }
    }

    public static final class ReadVariableNonFrameNode extends ReadVariableNode {

        @Child private ReadVariableNode readNode;
        @Child private UnresolvedReadVariableNode unresolvedNode;
        private final Assumption[] absentFrameSlotAssumptions;
        private final Symbol symbol;

        ReadVariableNonFrameNode(List<Assumption> assumptions, ReadVariableNode readNode, UnresolvedReadVariableNode unresolvedNode, Symbol symbol) {
            this.readNode = readNode;
            this.unresolvedNode = unresolvedNode;
            this.symbol = symbol;
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
        public Symbol getSymbol() {
            return symbol;
        }
    }

    public static final class ReadVariableVirtualNode extends ReadVariableNode implements HasMode {

        @Child private ReadLocalVariableNode readNode;
        @Child private ReadVariableNode nextNode;
        private final String mode;
        private final boolean readMissing;

        ReadVariableVirtualNode(ReadLocalVariableNode readNode, ReadVariableNode nextNode, String mode, boolean readMissing) {
            this.readNode = readNode;
            this.nextNode = nextNode;
            this.mode = mode;
            this.readMissing = readMissing;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            controlVisibility();
            if (readNode.getFrameSlotNode().hasValue(frame, frame)) {
                Object result = readNode.execute(frame);
                if (checkType(result, mode, readMissing)) {
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
        public Symbol getSymbol() {
            return nextNode.getSymbol();
        }

        @Override
        public String getMode() {
            return mode;
        }

        @Override
        public boolean readMissing() {
            return readMissing;
        }
    }

    public static final class UnResolvedReadLocalVariableNode extends ReadVariableNode implements HasMode {
        private final Symbol symbol;
        private final String mode;
        private final boolean readMissing;
        @Child private ReadLocalVariableNode node;

        UnResolvedReadLocalVariableNode(final Symbol symbol, final String mode, boolean readMissing) {
            this.symbol = symbol;
            this.mode = mode;
            this.readMissing = readMissing;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            controlVisibility();
            node = insert(ReadLocalVariableNodeFactory.create(new FrameSlotNode.UnresolvedFrameSlotNode(symbol.getName()), symbol));
            if (node.getFrameSlotNode().hasValue(frame, frame)) {
                Object result = node.execute(frame);
                if (checkType(result, mode, readMissing)) {
                    replace(node);
                    return result;
                }
            }
            return replace(UnknownVariableNodeFactory.create(symbol, mode, readMissing)).execute(frame);
        }

        @Override
        public Object execute(VirtualFrame frame, MaterializedFrame enclosingFrame) {
            controlVisibility();
            throw new UnsupportedOperationException();
        }

        @Override
        public Symbol getSymbol() {
            return symbol;
        }

        @Override
        public String getMode() {
            return mode;
        }

        @Override
        public boolean readMissing() {
            return readMissing;
        }
    }

    @NodeField(name = "symbol", type = Symbol.class)
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

        public static ReadVariableNode create(SourceSection src, String symbolStr, String mode) {
            Symbol symbol = Symbol.create(symbolStr);
            ReadVariableNode rvn = new UnresolvedReadVariableNode(symbol, mode, false, false);
            rvn.assignSourceSection(src);
            return ResolvePromiseNodeFactory.create(new ReadVariableSuperMaterializedNode(rvn), symbol);
        }

        @Override
        public Symbol getSymbol() {
            return readNode.getSymbol();
        }
    }

    public static final class ReadVariableMaterializedNode extends ReadVariableNode implements HasMode {

        @Child private ReadSuperVariableNode readNode;
        @Child private ReadVariableNode nextNode;
        private final String mode;
        private final boolean readMissing;

        ReadVariableMaterializedNode(ReadSuperVariableNode readNode, ReadVariableNode nextNode, boolean readMissing, String mode) {
            this.readNode = readNode;
            this.nextNode = nextNode;
            this.mode = mode;
            this.readMissing = readMissing;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            controlVisibility();
            throw new UnsupportedOperationException();
        }

        @Override
        public Object execute(VirtualFrame frame, MaterializedFrame enclosingFrame) {
            controlVisibility();
            if (readNode.getFrameSlotNode().hasValue(frame, enclosingFrame)) {
                Object result = readNode.execute(frame, enclosingFrame);
                if (checkType(result, mode, readMissing)) {
                    return result;
                }
            }
            return nextNode.execute(frame, RArguments.getEnclosingFrame(enclosingFrame));
        }

        @Override
        public Symbol getSymbol() {
            return nextNode.getSymbol();
        }

        @Override
        public String getMode() {
            return mode;
        }

        @Override
        public boolean readMissing() {
            return readMissing;
        }
    }

    @NodeChild(value = "frameSlotNode", type = FrameSlotNode.class)
    @NodeField(name = "symbol", type = Symbol.class)
    public abstract static class ReadLocalVariableNode extends ReadVariableNode {

        protected abstract FrameSlotNode getFrameSlotNode();

        @Override
        public abstract Symbol getSymbol();

        @Specialization(rewriteOn = FrameSlotTypeException.class)
        protected byte doLogical(VirtualFrame frame, FrameSlot frameSlot) throws FrameSlotTypeException {
            controlVisibility();
            return frame.getByte(frameSlot);
        }

        @Specialization(rewriteOn = FrameSlotTypeException.class)
        protected int doInteger(VirtualFrame frame, FrameSlot frameSlot) throws FrameSlotTypeException {
            controlVisibility();
            return frame.getInt(frameSlot);
        }

        @Specialization(rewriteOn = FrameSlotTypeException.class)
        protected double doDouble(VirtualFrame frame, FrameSlot frameSlot) throws FrameSlotTypeException {
            controlVisibility();
            return frame.getDouble(frameSlot);
        }

        @Specialization
        protected Object doObject(VirtualFrame frame, FrameSlot frameSlot) {
            controlVisibility();
            try {
                return frame.getObject(frameSlot);
            } catch (FrameSlotTypeException e) {
                throw new IllegalStateException();
            }
        }
    }

    @SuppressWarnings("unused")
    @NodeChildren({@NodeChild(value = "enclosingFrame", type = AccessEnclosingFrameNode.class), @NodeChild(value = "frameSlotNode", type = FrameSlotNode.class)})
    @NodeField(name = "symbol", type = Symbol.class)
    public abstract static class ReadSuperVariableNode extends ReadVariableNode {

        protected abstract FrameSlotNode getFrameSlotNode();

        @Override
        public abstract Symbol getSymbol();

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

    @NodeFields(value = {@NodeField(name = "function", type = RFunction.class), @NodeField(name = "symbol", type = Symbol.class)})
    public abstract static class BuiltinFunctionVariableNode extends ReadVariableNode {

        protected abstract RFunction getFunction();

        @Override
        public abstract Symbol getSymbol();

        @Specialization
        protected Object doObject(@SuppressWarnings("unused") VirtualFrame frame) {
            controlVisibility();
            return getFunction();
        }
    }

    @NodeFields({@NodeField(name = "symbol", type = Symbol.class), @NodeField(name = "mode", type = String.class), @NodeField(name = "readMissing", type = Boolean.class)})
    public abstract static class UnknownVariableNode extends ReadVariableNode implements HasMode {

        @Override
        public abstract Symbol getSymbol();

        @Override
        public abstract String getMode();

        public abstract boolean getReadMissing();

        @Specialization
        protected Object doObject() {
            controlVisibility();
            throw RError.error(getMode() == RRuntime.TYPE_FUNCTION ? RError.Message.UNKNOWN_FUNCTION : RError.Message.UNKNOWN_OBJECT, getSymbol());
        }

        @Override
        public boolean readMissing() {
            return getReadMissing();
        }
    }
}
