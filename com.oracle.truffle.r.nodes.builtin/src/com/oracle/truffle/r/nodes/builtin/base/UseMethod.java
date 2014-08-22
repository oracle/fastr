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

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.control.*;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

@RBuiltin(name = "UseMethod", kind = PRIMITIVE, parameterNames = {"generic", "object"})
public abstract class UseMethod extends RBuiltinNode {

    private static final int INLINE_CACHE_SIZE = 4;

    /*
     * TODO: If more than two parameters are passed to UseMethod the extra parameters are ignored
     * and a warning is generated.
     */
    @Child UseMethodNode useMethodNode;

    public UseMethod() {
        this.useMethodNode = new UninitializedUseMethodNode(0, getSuppliedArgsNames());
    }

    @Override
    public RNode[] getParameterValues() {
        return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance)};
    }

    @Specialization
    protected Object execute(VirtualFrame frame, String generic, Object arg) {
        controlVisibility();
        throw new ReturnException(useMethodNode.execute(frame, generic, arg));
    }

    private abstract static class UseMethodNode extends RNode {

        @Child protected ClassHierarchyNode classHierarchyNode = ClassHierarchyNodeFactory.create(null);
        protected final String[] suppliedArgsNames;

        public UseMethodNode(String[] suppliedArgsNames) {
            this.suppliedArgsNames = suppliedArgsNames;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            throw new AssertionError();
        }

        public abstract Object execute(VirtualFrame frame, final String generic, final Object o);
    }

    private static final class UninitializedUseMethodNode extends UseMethodNode {

        protected final int depth;

        protected UninitializedUseMethodNode(int depth, String[] suppliedArgsNames) {
            super(suppliedArgsNames);
            this.depth = depth;
        }

        @Override
        public Object execute(VirtualFrame frame, String generic, Object o) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            return specialize(generic, o).execute(frame, generic, o);
        }

        private UseMethodNode specialize(String generic, Object o) {
            CompilerAsserts.neverPartOfCompilation();
            if (depth < INLINE_CACHE_SIZE) {
                if (o == RMissing.instance) {
                    return replace(new UseMethodGenericOnlyNode(generic, depth, suppliedArgsNames));
                } else {
                    return replace(new UseMethodGenericAndObjectNode(generic, depth, suppliedArgsNames));
                }
            }
            return replace(new UseMethodFallbackNode(suppliedArgsNames));
        }

    }

    private abstract static class UseMethodCachedNode extends UseMethodNode {

        @Child protected UseMethodNode nextNode;
        @Child protected DispatchedCallNode currentNode;

        private final String generic;

        protected UseMethodCachedNode(String generic, int depth, String[] suppliedArgsNames) {
            super(suppliedArgsNames);
            this.generic = generic;
            nextNode = new UninitializedUseMethodNode(depth + 1, suppliedArgsNames);
            currentNode = DispatchedCallNode.create(generic, RRuntime.USE_METHOD, suppliedArgsNames);
        }

        protected abstract Object executeDispatch(VirtualFrame frame, String gen, Object o);

        @Override
        public final Object execute(VirtualFrame frame, String gen, Object o) {
            if (generic.equals(gen)) {
                return executeDispatch(frame, gen, o);
            } else {
                return nextNode.execute(frame, gen, o);
            }
        }

    }

    /*
     * If only one argument is passed to UseMethod, the first argument of enclosing function is used
     * to resolve the generic.
     */
    private static final class UseMethodGenericOnlyNode extends UseMethodCachedNode {

        protected UseMethodGenericOnlyNode(String generic, int depth, String[] suppliedArgsNames) {
            super(generic, depth, suppliedArgsNames);
        }

        @Override
        public Object executeDispatch(VirtualFrame frame, final String gen, Object obj) {
            if (RArguments.getArgumentsLength(frame) == 0 || RArguments.getArgument(frame, 0) == null) {
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.UNKNOWN_FUNCTION_USE_METHOD, gen, RRuntime.toString(RNull.instance));
            }
            Object enclosingArg = RArguments.getArgument(frame, 0);
            if (enclosingArg instanceof RPromise) {
                enclosingArg = ((RPromise) enclosingArg).evaluate(frame);
            }
            return currentNode.execute(frame, classHierarchyNode.execute(frame, enclosingArg));
        }
    }

    private static final class UseMethodGenericAndObjectNode extends UseMethodCachedNode {

        protected UseMethodGenericAndObjectNode(String generic, int depth, String[] suppliedArgsNames) {
            super(generic, depth, suppliedArgsNames);
        }

        @Override
        public Object executeDispatch(VirtualFrame frame, final String gen, Object obj) {
            return currentNode.execute(frame, classHierarchyNode.execute(frame, obj));
        }
    }

    private static final class UseMethodFallbackNode extends UseMethodNode {

        public UseMethodFallbackNode(String[] suppliedArgsNames) {
            super(suppliedArgsNames);
        }

        @Override
        public Object execute(VirtualFrame frame, String generic, Object o) {
            // TODO restructure UseMethodDispatchNode to expose generic case
            // (This will prevent this fallback from being uncompilable.)
            CompilerAsserts.neverPartOfCompilation();
            if (o == RMissing.instance) {
                if (RArguments.getArgumentsLength(frame) == 0 || RArguments.getArgument(frame, 0) == null) {
                    throw RError.error(getEncapsulatingSourceSection(), RError.Message.UNKNOWN_FUNCTION_USE_METHOD, generic, RRuntime.toString(RNull.instance));
                }
                Object enclosingArg = RArguments.getArgument(frame, 0);
                if (enclosingArg instanceof RPromise) {
                    enclosingArg = ((RPromise) enclosingArg).evaluate(frame);
                }
                DispatchedCallNode dcn = DispatchedCallNode.create(generic, RRuntime.USE_METHOD, suppliedArgsNames);
                return dcn.execute(frame, classHierarchyNode.execute(frame, enclosingArg));
            } else {
                DispatchedCallNode dcn = DispatchedCallNode.create(generic, RRuntime.USE_METHOD, suppliedArgsNames);
                return dcn.execute(frame, classHierarchyNode.execute(frame, o));
            }
        }

    }

}
