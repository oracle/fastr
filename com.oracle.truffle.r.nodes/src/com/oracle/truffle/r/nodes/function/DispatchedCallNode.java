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
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public abstract class DispatchedCallNode extends RNode {

    private static final int INLINE_CACHE_SIZE = 4;
    protected Object[] args;
    protected RNode[] argNodes;

    public static DispatchedCallNode create(final String genericName, final String dispatchType, String[] suppliedArgsNames) {
        return new UninitializedDispatchedCallNode(genericName, dispatchType, suppliedArgsNames);
    }

    public static DispatchedCallNode create(final String genericName, final String dispatchType, final Object[] args, String[] suppliedArgsNames) {
        return new UninitializedDispatchedCallNode(genericName, dispatchType, args, suppliedArgsNames);
    }

    @SuppressFBWarnings(value = "ES_COMPARING_PARAMETER_STRING_WITH_EQ", justification = "RDotGroup is intended to be used as an identity")
    public static DispatchedCallNode create(final String genericName, final String dispatchType, final CallArgumentsNode callArgsNode) {
        if (dispatchType == RGroupGenerics.RDotGroup) {
            return new ResolvedDispatchedCallNode(GroupDispatchNode.create(genericName, callArgsNode));
        }
        throw new AssertionError();
    }

    @Override
    public Object execute(VirtualFrame frame) {
        throw new UnsupportedOperationException();
    }

    public abstract Object execute(VirtualFrame frame, RStringVector type);

    public abstract Object executeInternal(VirtualFrame frame, RStringVector type, @SuppressWarnings("hiding") Object[] args);

    private static final class UninitializedDispatchedCallNode extends DispatchedCallNode {
        protected final int depth;
        protected final String genericName;
        protected final String dispatchType;
        protected final String[] suppliedArgsNames;

        public UninitializedDispatchedCallNode(final String genericName, final String dispatchType, String[] suppliedArgsNames) {
            this.genericName = genericName;
            this.depth = 0;
            this.dispatchType = dispatchType;
            this.suppliedArgsNames = suppliedArgsNames;
        }

        private UninitializedDispatchedCallNode(final UninitializedDispatchedCallNode copy, final int depth) {
            this.genericName = copy.genericName;
            this.dispatchType = copy.dispatchType;
            this.depth = depth;
            this.suppliedArgsNames = copy.suppliedArgsNames;
        }

        public UninitializedDispatchedCallNode(final String genericName, final String dispatchType, final Object[] args, String[] suppliedArgsNames) {
            this(genericName, dispatchType, suppliedArgsNames);
            this.args = args;
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
                return new UseMethodDispatchNode(this.genericName, type, this.suppliedArgsNames);
            }
            if (this.dispatchType == RRuntime.NEXT_METHOD) {
                return new NextMethodDispatchNode(this.genericName, type, this.args);
            }
            throw new AssertionError();
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
        public Object executeInternal(VirtualFrame frame, RStringVector type, @SuppressWarnings("hiding") Object[] args) {
            return dcn.executeInternal(frame, type, args);
        }
    }

    private static final class CachedNode extends DispatchedCallNode {

        @Child protected DispatchedCallNode nextNode;
        @Child protected DispatchNode currentNode;
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
        public Object executeInternal(VirtualFrame frame, RStringVector aType, @SuppressWarnings("hiding") Object[] args) {
            if (isEqualType(this.type, aType)) {
                return currentNode.executeInternal(frame, args);
            }
            return nextNode.executeInternal(frame, aType, args);
        }
    }

    private static final class ResolvedDispatchedCallNode extends DispatchedCallNode {
        @Child protected RCallNode aCallNode;
        @Child protected GroupDispatchNode aDispatchNode;

        public ResolvedDispatchedCallNode(GroupDispatchNode dNode) {
            this.aDispatchNode = dNode;
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
        public Object executeInternal(VirtualFrame frame, RStringVector type, @SuppressWarnings("hiding") Object[] args) {
            return Utils.nyi();
        }

        private Object executeHelper(VirtualFrame frame, DispatchNode.FunctionCall aFuncCall) {
            if (aCallNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                aCallNode = insert(RCallNode.createCall(null, aFuncCall.args));
            } else {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                aCallNode.replace(RCallNode.createCall(null, aFuncCall.args));
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
