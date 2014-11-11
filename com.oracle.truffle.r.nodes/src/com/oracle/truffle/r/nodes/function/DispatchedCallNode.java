/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, Oracle and/or its affiliates
 *
 * All rights reserved.
 */

package com.oracle.truffle.r.nodes.function;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.runtime.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.RDeparse.State;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.env.REnvironment;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public abstract class DispatchedCallNode extends RNode {

    private static final int INLINE_CACHE_SIZE = 4;

    public static DispatchedCallNode create(final String genericName, final String dispatchType) {
        return new UninitializedDispatchedCallNode(genericName, dispatchType);
    }

    public static DispatchedCallNode create(final String genericName, final String dispatchType, final Object[] args) {
        return new UninitializedDispatchedCallNode(genericName, dispatchType, args);
    }

    @SuppressFBWarnings(value = "ES_COMPARING_PARAMETER_STRING_WITH_EQ", justification = "RDotGroup is intended to be used as an identity")
    public static DispatchedCallNode create(final String genericName, final String dispatchType, SourceSection callSrc, final CallArgumentsNode callArgsNode) {
        if (dispatchType == RGroupGenerics.RDotGroup) {
            return new ResolvedDispatchedCallNode(GroupDispatchNode.create(genericName, callSrc, callArgsNode));
        }
        throw new AssertionError();
    }

    @Override
    public Object execute(VirtualFrame frame) {
        throw RInternalError.shouldNotReachHere();
    }

    public abstract Object execute(VirtualFrame frame, RStringVector type);

    public abstract Object executeInternal(VirtualFrame frame, RStringVector type, Object[] args);

    @Override
    public boolean isSyntax() {
        return true;
    }

    @NodeInfo(cost = NodeCost.UNINITIALIZED)
    private static final class UninitializedDispatchedCallNode extends DispatchedCallNode {
        private final int depth;
        private final String genericName;
        private final String dispatchType;
        private final Object[] args;

        public UninitializedDispatchedCallNode(final String genericName, final String dispatchType, Object[] args) {
            this.genericName = genericName;
            this.depth = 0;
            this.dispatchType = dispatchType;
            this.args = args;
        }

        public UninitializedDispatchedCallNode(final String genericName, final String dispatchType) {
            this(genericName, dispatchType, null);
        }

        private UninitializedDispatchedCallNode(final UninitializedDispatchedCallNode copy, final int depth) {
            this.depth = depth;
            this.genericName = copy.genericName;
            this.dispatchType = copy.dispatchType;
            this.args = null;
        }

        @Override
        public Object execute(VirtualFrame frame, RStringVector type) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            return specialize(type).execute(frame, type);
        }

        @Override
        public Object executeInternal(VirtualFrame frame, RStringVector type, @SuppressWarnings("hiding") Object[] args) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            return specialize(type).executeInternal(frame, type, args);
        }

        private DispatchedCallNode specialize(RStringVector type) {
            CompilerAsserts.neverPartOfCompilation();
            if (depth < INLINE_CACHE_SIZE) {
                final DispatchNode current = createCurrentNode(type);
                final DispatchedCallNode cachedNode = new CachedNode(current, new UninitializedDispatchedCallNode(this, this.depth + 1), type);
                this.replace(cachedNode);
                return cachedNode;
            }
            return this.replace(new GenericDispatchNode(createCurrentNode(type)));
        }

        protected DispatchNode createCurrentNode(RStringVector type) {
            if (this.dispatchType == RRuntime.USE_METHOD) {
                return new UseMethodDispatchNode(this.genericName, type);
            }
            if (this.dispatchType == RRuntime.NEXT_METHOD) {
                return new NextMethodDispatchNode(this.genericName, type, this.args);
            }
            throw RInternalError.shouldNotReachHere();
        }
    }

    private static final class GenericDispatchNode extends DispatchedCallNode {

        @Child private DispatchNode dcn;

        public GenericDispatchNode(DispatchNode dcn) {
            this.dcn = dcn;
        }

        @Override
        public Object execute(VirtualFrame frame, RStringVector type) {
            return dcn.execute(frame, type);
        }

        @Override
        public Object executeInternal(VirtualFrame frame, RStringVector type, Object[] args) {
            return dcn.executeInternal(frame, type, args);
        }
    }

    private static final class CachedNode extends DispatchedCallNode {

        @Child private DispatchedCallNode nextNode;
        @Child private DispatchNode currentNode;
        private final RStringVector type;

        CachedNode(final DispatchNode currentNode, final DispatchedCallNode nextNode, final RStringVector type) {
            this.nextNode = nextNode;
            this.currentNode = currentNode;
            this.type = type;
        }

        @Override
        public Object execute(VirtualFrame frame, final RStringVector aType) {
            if (isEqualType(this.type, aType)) {
                return currentNode.execute(frame);
            }
            return nextNode.execute(frame, aType);
        }

        private static boolean isEqualType(final RStringVector one, final RStringVector two) {
            if (one == null && two == null) {
                return true;
            }
            if (one == null || two == null) {
                return false;
            }

            if (one.getLength() != two.getLength()) {
                return false;
            }
            for (int i = 0; i < one.getLength(); ++i) {
                if (!one.getDataAt(i).equals(two.getDataAt(i))) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public Object executeInternal(VirtualFrame frame, RStringVector aType, Object[] args) {
            if (isEqualType(this.type, aType)) {
                return currentNode.executeInternal(frame, args);
            }
            return nextNode.executeInternal(frame, aType, args);
        }
    }

    private static final class ResolvedDispatchedCallNode extends DispatchedCallNode {
        @Child private RCallNode aCallNode;
        @Child private GroupDispatchNode aDispatchNode;

        public ResolvedDispatchedCallNode(GroupDispatchNode dNode) {
            this.aDispatchNode = dNode;
            this.assignSourceSection(dNode.getSourceSection());
        }

        @Override
        public boolean isSyntax() {
            return true;
        }

        @Override
        public void deparse(State state) {
            String name = aDispatchNode.getGenericName();
            RDeparse.Func func = RDeparse.getFunc(name);
            if (func != null) {
                // infix operator
                RASTDeparse.deparseInfixOperator(state, this, func);
            } else {
                state.append(name);
                aDispatchNode.callArgsNode.deparse(state);
            }
        }

        @Override
        public RNode substitute(REnvironment env) {
            // TODO substitute aDispatchNode
            return RASTUtils.createCall(aDispatchNode, (CallArgumentsNode) aDispatchNode.callArgsNode.substitute(env));
        }

        @Override
        public Object execute(VirtualFrame frame) {
            DispatchNode.FunctionCall aFuncCall = (DispatchNode.FunctionCall) aDispatchNode.execute(frame);
            return executeHelper(frame, aFuncCall);
        }

        @Override
        public Object execute(VirtualFrame frame, RStringVector type) {
            DispatchNode.FunctionCall aFuncCall = (DispatchNode.FunctionCall) aDispatchNode.execute(frame, type);
            return executeHelper(frame, aFuncCall);
        }

        @Override
        public Object executeInternal(VirtualFrame frame, RStringVector type, Object[] args) {
            return Utils.nyi();
        }

        // TODO Insert PIC!
        private Object executeHelper(VirtualFrame frame, DispatchNode.FunctionCall aFuncCall) {
            if (aCallNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                aCallNode = insert(RCallNode.createCall(getSourceSection(), null, aFuncCall.args));
            } else {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                aCallNode.replace(RCallNode.createCall(getSourceSection(), null, aFuncCall.args));
            }
            try {
                Object result = aCallNode.execute(frame, aFuncCall.function);
                return result;
            } finally {
                aDispatchNode.unsetEnvironment();
            }
        }
    }
}
