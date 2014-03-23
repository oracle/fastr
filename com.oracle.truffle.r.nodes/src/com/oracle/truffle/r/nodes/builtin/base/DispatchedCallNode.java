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

package com.oracle.truffle.r.nodes.builtin.base;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

public abstract class DispatchedCallNode extends RNode {

    private static final int INLINE_CACHE_SIZE = 4;

    public static DispatchedCallNode create(final String genericName, final String dispatchType) {
        return new UninitializedDispatchedCallNode(genericName, dispatchType);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        throw new UnsupportedOperationException();
    }

    public abstract Object execute(VirtualFrame frame, RStringVector type);

    private static final class UninitializedDispatchedCallNode extends DispatchedCallNode {
        protected final int depth;
        protected final String genericName;
        protected final String dispatchType;

        public UninitializedDispatchedCallNode(final String genericName, final String dispatchType) {
            this.genericName = genericName;
            this.depth = 0;
            this.dispatchType = dispatchType;
        }

        private UninitializedDispatchedCallNode(UninitializedDispatchedCallNode copy, int depth) {
            this.genericName = copy.genericName;
            this.dispatchType = copy.dispatchType;
            this.depth = depth;
        }

        @Override
        public Object execute(VirtualFrame frame, RStringVector type) {
            CompilerDirectives.transferToInterpreter();
            return specialize(type).execute(frame, type);
        }

        private DispatchedCallNode specialize(RStringVector type) {
            CompilerAsserts.neverPartOfCompilation();
            if (depth < INLINE_CACHE_SIZE) {
                final DispatchedCallNode current = createCurrentNode(type);
                final DispatchedCallNode cachedNode = new CachedNode(current, new UninitializedDispatchedCallNode(this, this.depth + 1), type);
                this.replace(cachedNode);
                return cachedNode;
            } else {
                DispatchedCallNode topMost = (DispatchedCallNode) getTopNode();
                DispatchedCallNode generic = topMost.replace(createCurrentNode(type));
                return generic;
            }
        }

        protected DispatchedCallNode createCurrentNode(RStringVector type) {
            if (this.dispatchType == RRuntime.USE_METHOD) {
                return new ResolvedDispatchedCallNode(new UseMethodDispatchNode(this.genericName, type));
            }
            // TODO: throw error
            return null;
        }

        protected Node getTopNode() {
            Node parentNode = this;
            for (int i = 0; i < depth; i++) {
                parentNode = parentNode.getParent();
            }
            return parentNode;
        }
    }

    public static final class CachedNode extends DispatchedCallNode {

        @Child protected DispatchedCallNode nextNode;
        @Child protected DispatchedCallNode currentNode;
        private final RStringVector type;

        CachedNode(final DispatchedCallNode currentNode, final DispatchedCallNode nextNode, final RStringVector type) {
            this.nextNode = insert(nextNode);
            this.currentNode = insert(currentNode);
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
    }

    private static class ResolvedDispatchedCallNode extends DispatchedCallNode {
        @Child protected RCallNode aCallNode;
        @Child protected DispatchNode aDispatchNode;

        public ResolvedDispatchedCallNode(DispatchNode dNode) {
            this.aDispatchNode = insert(dNode);
        }

        @Override
        public Object execute(VirtualFrame frame) {
            FunctionCall aFuncCall = (FunctionCall) aDispatchNode.execute(frame);
            if (aCallNode == null) {
                CompilerDirectives.transferToInterpreter();
                aCallNode = insert(RCallNode.createCall(null, aFuncCall.args));
            }
            Object result = aCallNode.execute(frame, aFuncCall.function);
            aDispatchNode.unsetEnvironment(frame);
            return result;
        }

        @Override
        public Object execute(VirtualFrame frame, RStringVector type) {
            return this.execute(frame);
        }
    }
}
