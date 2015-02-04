/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates
 *
 * All rights reserved.
 */

package com.oracle.truffle.r.nodes.function;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

public abstract class DispatchedCallNode extends RNode {

    private static final int INLINE_CACHE_SIZE = 4;

    public static DispatchedCallNode create(final String genericName, final String dispatchType) {
        return new UninitializedDispatchedCallNode(genericName, dispatchType);
    }

    public static DispatchedCallNode create(final String genericName, final String dispatchType, final Object[] args) {
        return new UninitializedDispatchedCallNode(genericName, null, dispatchType, args, null);
    }

    public static DispatchedCallNode create(final String genericName, final String enclosingName, final String dispatchType, final Object[] args, final String[] argNames) {
        return new UninitializedDispatchedCallNode(genericName, enclosingName, dispatchType, args, argNames);
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
        private final String enclosingName;
        private final String dispatchType;
        @CompilationFinal private final Object[] args;
        @CompilationFinal private final String[] argNames;

        public UninitializedDispatchedCallNode(final String genericName, final String enclosingName, final String dispatchType, Object[] args, String[] argNames) {
            this.genericName = genericName;
            this.enclosingName = enclosingName;
            this.depth = 0;
            this.dispatchType = dispatchType;
            this.args = args;
            this.argNames = argNames;
        }

        public UninitializedDispatchedCallNode(final String genericName, final String dispatchType) {
            this(genericName, dispatchType, null, null, null);
        }

        private UninitializedDispatchedCallNode(final UninitializedDispatchedCallNode copy, final int depth) {
            this.depth = depth;
            this.genericName = copy.genericName;
            this.enclosingName = copy.enclosingName;
            this.dispatchType = copy.dispatchType;
            this.args = null;
            this.argNames = null; // TODO: is it OK to nullify these two?
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
                return new NextMethodDispatchNode(this.genericName, type, this.args, this.argNames, enclosingName);
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
            if (S3DispatchNode.isEqualType(this.type, aType)) {
                return currentNode.execute(frame);
            }
            return nextNode.execute(frame, aType);
        }

        @Override
        public Object executeInternal(VirtualFrame frame, RStringVector aType, Object[] args) {
            if (S3DispatchNode.isEqualType(this.type, aType)) {
                return currentNode.executeInternal(frame, args);
            }
            return nextNode.executeInternal(frame, aType, args);
        }
    }
}
