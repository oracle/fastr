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
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

public abstract class DispatchedCallNode extends RNode {

    public static final class NoGenericMethodException extends ControlFlowException {
        private static final long serialVersionUID = 344198853147758435L;
    }

    public static enum DispatchType {
        UseMethod,
        NextMethod
    }

    private static final int INLINE_CACHE_SIZE = 4;

    public static DispatchedCallNode create(String genericName, DispatchType dispatchType, ArgumentsSignature signature) {
        return new UninitializedDispatchedCallNode(genericName, dispatchType, signature);
    }

    public static DispatchedCallNode create(String genericName, String enclosingName, DispatchType dispatchType, Object[] args, ArgumentsSignature signature) {
        return new UninitializedDispatchedCallNode(genericName, enclosingName, dispatchType, args, signature);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        throw RInternalError.shouldNotReachHere();
    }

    public abstract Object execute(VirtualFrame frame, RStringVector type);

    public abstract Object executeInternal(VirtualFrame frame, RStringVector type, Object[] args) throws NoGenericMethodException;

    @Override
    public boolean isSyntax() {
        return true;
    }

    @NodeInfo(cost = NodeCost.UNINITIALIZED)
    private static final class UninitializedDispatchedCallNode extends DispatchedCallNode {
        private final int depth;
        private final String genericName;
        private final String enclosingName;
        private final DispatchType dispatchType;
        @CompilationFinal private final Object[] args;
        private final ArgumentsSignature signature;

        private UninitializedDispatchedCallNode(String genericName, String enclosingName, DispatchType dispatchType, Object[] args, ArgumentsSignature signature) {
            this.genericName = genericName;
            this.enclosingName = enclosingName;
            this.signature = signature;
            this.depth = 0;
            this.dispatchType = dispatchType;
            this.args = args;
        }

        public UninitializedDispatchedCallNode(String genericName, DispatchType dispatchType, ArgumentsSignature signature) {
            this(genericName, null, dispatchType, null, signature);
        }

        private UninitializedDispatchedCallNode(UninitializedDispatchedCallNode copy, int depth) {
            this.depth = depth;
            this.genericName = copy.genericName;
            this.enclosingName = copy.enclosingName;
            this.dispatchType = copy.dispatchType;
            this.signature = copy.signature;
            this.args = null;
        }

        @Override
        public Object execute(VirtualFrame frame, RStringVector type) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            return specialize(type).execute(frame, type);
        }

        @Override
        public Object executeInternal(VirtualFrame frame, RStringVector type, @SuppressWarnings("hiding") Object[] args) throws NoGenericMethodException {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            return specialize(type).executeInternal(frame, type, args);
        }

        private DispatchedCallNode specialize(RStringVector type) {
            CompilerAsserts.neverPartOfCompilation();
            if (depth < INLINE_CACHE_SIZE) {
                DispatchNode current = createCurrentNode(type, true);
                return replace(new CachedNode(current, new UninitializedDispatchedCallNode(this, depth + 1), type));
            }
            RError.performanceWarning("S3 method dispatch fallback to generic");
            return this.replace(new GenericDispatchNode(createCurrentNode(type, false)));
        }

        private DispatchNode createCurrentNode(RStringVector type, boolean cached) {
            switch (dispatchType) {
                case NextMethod:
                    return new NextMethodDispatchNode(genericName, type, args, signature, enclosingName);
                case UseMethod:
                    return cached ? UseMethodDispatchNode.createCached(genericName, type, signature) : UseMethodDispatchNode.createGeneric(genericName, signature);
                default:
                    throw RInternalError.shouldNotReachHere();
            }
        }
    }

    private static final class GenericDispatchNode extends DispatchedCallNode {

        @Child private DispatchNode dcn;

        public GenericDispatchNode(DispatchNode dcn) {
            this.dcn = dcn;
        }

        @Override
        public Object execute(VirtualFrame frame, RStringVector type) {
            return dcn.executeGeneric(frame, type);
        }

        @Override
        public Object executeInternal(VirtualFrame frame, RStringVector type, Object[] args) throws NoGenericMethodException {
            return dcn.executeInternalGeneric(frame, type, args);
        }
    }

    private static final class CachedNode extends DispatchedCallNode {

        private final ConditionProfile sameIdentityProfile = ConditionProfile.createBinaryProfile();
        private final BranchProfile nullTypeProfile = BranchProfile.create();
        private final BranchProfile lengthMismatch = BranchProfile.create();
        private final BranchProfile notIdentityEqualElements = BranchProfile.create();

        @Child private DispatchedCallNode nextNode;
        @Child private DispatchNode currentNode;
        private final RStringVector cachedType;
        @CompilationFinal private final String[] cachedTypeElements;

        CachedNode(DispatchNode currentNode, DispatchedCallNode nextNode, RStringVector type) {
            this.nextNode = nextNode;
            this.currentNode = currentNode;
            this.cachedType = type;
            this.cachedTypeElements = type.getDataCopy();
        }

        private boolean isEqualType(RStringVector type) {
            if (sameIdentityProfile.profile(type == cachedType)) {
                return true;
            }
            if (cachedType == null) {
                return false;
            }
            if (type == null) {
                nullTypeProfile.enter();
                return false;
            }
            if (type.getLength() != cachedTypeElements.length) {
                lengthMismatch.enter();
                return false;
            }
            return compareLoop(type);
        }

        @ExplodeLoop
        private boolean compareLoop(RStringVector type) {
            for (int i = 0; i < cachedTypeElements.length; i++) {
                String elementOne = cachedTypeElements[i];
                String elementTwo = type.getDataAt(i);
                if (elementOne != elementTwo) {
                    notIdentityEqualElements.enter();
                    if (!elementOne.equals(elementTwo)) {
                        return false;
                    }
                }
            }
            return true;
        }

        @Override
        public Object execute(VirtualFrame frame, RStringVector type) {
            if (isEqualType(type)) {
                return currentNode.execute(frame);
            }
            return nextNode.execute(frame, type);
        }

        @Override
        public Object executeInternal(VirtualFrame frame, RStringVector type, Object[] args) throws NoGenericMethodException {
            if (isEqualType(type)) {
                return currentNode.executeInternal(frame, args);
            }
            return nextNode.executeInternal(frame, type, args);
        }
    }
}
