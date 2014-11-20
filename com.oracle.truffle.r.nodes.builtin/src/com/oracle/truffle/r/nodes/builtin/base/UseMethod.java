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
import com.oracle.truffle.api.CompilerDirectives.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.control.*;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.RPromise.*;

@RBuiltin(name = "UseMethod", kind = PRIMITIVE, parameterNames = {"generic", "object"})
public abstract class UseMethod extends RBuiltinNode {

    private static final int INLINE_CACHE_SIZE = 4;

    /*
     * TODO: If more than two parameters are passed to UseMethod the extra parameters are ignored
     * and a warning is generated.
     */
    @Child private UseMethodNode useMethodNode;

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
        @CompilationFinal protected final String[] suppliedArgsNames;

        private final PromiseProfile promiseProfile = new PromiseProfile();

        public UseMethodNode(String[] suppliedArgsNames) {
            this.suppliedArgsNames = suppliedArgsNames;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            throw new AssertionError();
        }

        public abstract Object execute(VirtualFrame frame, final String generic, final Object o);

        /**
         * @param frame
         * @return The 1. (logical) argument in the frame, handles {@link RPromise}s and
         *         {@link RArgsValuesAndNames}
         */
        protected Object getEnclosingArg(VirtualFrame frame) {
            // For S3Dispatch, we have to evaluate the the first argument
            Object enclosingArg = RArguments.getArgument(frame, 0);
            if (enclosingArg instanceof RArgsValuesAndNames) {
                // The GnuR "1. argument" might be hidden inside a "..."! Unwrap for proper dispatch
                RArgsValuesAndNames varArgs = (RArgsValuesAndNames) enclosingArg;
                enclosingArg = varArgs.getValues()[0];
            }

            enclosingArg = RPromise.checkEvaluate(frame, enclosingArg, promiseProfile);
            return enclosingArg;
        }
    }

    @NodeInfo(cost = NodeCost.UNINITIALIZED)
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

        @Child private UseMethodNode nextNode;
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

            // For S3Dispatch, we have to evaluate the the first argument
            Object enclosingArg = getEnclosingArg(frame);
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

                // For S3Dispatch, we have to evaluate the the first argument
                Object enclosingArg = getEnclosingArg(frame);
                DispatchedCallNode dcn = DispatchedCallNode.create(generic, RRuntime.USE_METHOD, suppliedArgsNames);
                return dcn.execute(frame, classHierarchyNode.execute(frame, enclosingArg));
            } else {
                DispatchedCallNode dcn = DispatchedCallNode.create(generic, RRuntime.USE_METHOD, suppliedArgsNames);
                return dcn.execute(frame, classHierarchyNode.execute(frame, o));
            }
        }

    }

}
