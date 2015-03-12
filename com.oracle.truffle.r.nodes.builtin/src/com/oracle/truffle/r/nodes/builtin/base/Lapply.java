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

package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.access.array.read.*;
import com.oracle.truffle.r.nodes.access.variables.*;
import com.oracle.truffle.r.nodes.binary.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.builtin.base.Lapply.GeneralLApplyNode.LapplyIteratorNode;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.RContext.Engine.ParseException;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.env.*;
import com.oracle.truffle.r.runtime.ops.*;

/**
 * The {@code lapply} builtin. {@code lapply} is an important implicit iterator in R. This
 * implementation handles the general case, but there are opportunities for "specializations" that
 * rewrite simple uses directly down to, e.g. explicit loops using, for example, a {@link LoopNode}.
 * <p>
 * The state associated with the iteration, namely the "vector", {@code X}, the function {@code FUN}
 * and the vector index <i>must</i> be stored in the frame. Fortunately, {@code X} and {@code FUN}
 * are already in the frame, so we only need to worry about the iterator index. In the general case,
 * this is handled by an instance of {@link LapplyIteratorNode}, which carries a piece of AST that
 * handles the indexing operation and the update of the index. Much of this is automatically
 * generated. In order to pass the result of the indexing operation, this is also stored in a unique
 * variable in the frame. This is a slight inefficiency that could be handled more directly in
 * simple cases by having the indexing operation itself be an argument to the function.
 *
 * See the comment in {@link VApply} regarding "...".
 */
@RBuiltin(name = "lapply", kind = INTERNAL, parameterNames = {"X", "FUN"})
public abstract class Lapply extends RBuiltinNode {

    private final RAttributeProfiles attrProfiles = RAttributeProfiles.create();

    /**
     * The default implementation.
     */
    @Child private GeneralLApplyNode doApply = new GeneralLApplyNode();

    @Specialization
    protected Object lapply(VirtualFrame frame, RAbstractVector vec, RFunction fun) {

        RVector vecMat = vec.materialize();
        RArgsValuesAndNames optionalArgs = (RArgsValuesAndNames) RArguments.getArgument(frame, 2);
        Object[] result = doApply.execute(frame, vecMat, fun, optionalArgs);
        // set here else it gets overridden by the iterator evaluation
        controlVisibility();
        return RDataFactory.createList(result, vecMat.getNames(attrProfiles));
    }

    public static final class GeneralLApplyNode extends RNode {
        /**
         * An anonymous variable that stores the result of the {@code X[[i]]} operation, that is
         * then passed to {@code FUN}.
         */
        private static final String LAPPLY_VEC_ELEM = AnonymousFrameVariable.create("LAPPLY_VEC_ELEM");

        /**
         * Customized inline call cache.
         */
        @Child private CachedCallNode callCache = CachedCallNode.create(this, 4);

        /**
         * These nodes are all independent of the details of a particular call.
         */
        @Child private LapplyIteratorNode iterator = new LapplyIteratorNode();
        @Child private LapplyFunctionNode functionNode = new LapplyFunctionNode();
        @Child private WriteVariableNode writeVectorElement = WriteVariableNode.create(LAPPLY_VEC_ELEM, iterator, false, false);
        @Child private ReadVariableNode readVectorElement = ReadVariableNode.create(LAPPLY_VEC_ELEM, false);

        public Object[] execute(VirtualFrame frame, RVector vecMat, RFunction fun, RArgsValuesAndNames optionalArgs) {
            // zero the iterator value in the current frame
            iterator.initialize(frame);

            Object[] result = new Object[vecMat.getLength()];
            for (int i = 0; i < result.length; ++i) {
                // Write new vector element to LAPPLY_VEC_ELEM frame slot
                writeVectorElement.execute(frame);

                result[i] = callCache.execute(frame, fun.getTarget(), optionalArgs);
            }
            return result;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            throw RInternalError.shouldNotReachHere();
        }

        /**
         * This node evaluates the indexed load and advances the iterator. All state is in the
         * {@link Frame}. It does not specialize itself but the child nodes will specialize.
         */
        protected static class LapplyIteratorNode extends RNode {
            private static final String ITER_INDEX_NAME = AnonymousFrameVariable.create("LAPPLY_ITER_INDEX");
            private static final Source ACCESS_ARRAY_SOURCE = Source.asPseudoFile("X[[i]]", "<lapply_array_access>");

            /**
             * Increments the iterator index, updating the {@link #ITER_INDEX_NAME} variable. Always
             * specializes to the same tree.
             */
            @Child private WriteVariableNode incIndex;
            /**
             * Initializes the {@link #ITER_INDEX_NAME} variable to 1 (R indexing rules). Always
             * specializes to the same tree.
             */
            @Child private WriteVariableNode zeroIndex;
            /**
             * Loads the element of the vector based on the {@code X} variable and the current
             * {@link #ITER_INDEX_NAME} variable. Specialized based on the type of {@code X}.
             */
            @Child private RNode indexedLoad;

            LapplyIteratorNode() {
                RNode[] incArgs = new RNode[]{ReadVariableNode.create(ITER_INDEX_NAME, false), ConstantNode.create(1)};
                BinaryArithmeticNode inc = BinaryArithmeticNodeFactory.create(BinaryArithmetic.ADD, null, incArgs, null, null);
                incIndex = insert(WriteVariableNode.create(ITER_INDEX_NAME, inc, false, false));
                zeroIndex = insert(WriteVariableNode.create(ITER_INDEX_NAME, ConstantNode.create(1), false, false));
                getIndexedLoad();
            }

            LapplyIteratorNode initialize(VirtualFrame frame) {
                zeroIndex.execute(frame);
                return this;
            }

            RNode getIndexedLoad() {
                if (indexedLoad == null) {
                    AccessArrayNode indexNode;
                    try {
                        indexNode = (AccessArrayNode) ((RLanguage) RContext.getEngine().parse(ACCESS_ARRAY_SOURCE).getDataAt(0)).getRep();
                    } catch (ParseException ex) {
                        throw RInternalError.shouldNotReachHere();
                    }
                    REnvironment env = RDataFactory.createNewEnv("dummy");
                    env.safePut("i", RDataFactory.createLanguage(ReadVariableNode.create(ITER_INDEX_NAME, false)));
                    indexedLoad = indexNode.substitute(env);
                }
                return indexedLoad;
            }

            @Override
            public Object execute(VirtualFrame frame) {
                Object result = indexedLoad.execute(frame);
                incIndex.execute(frame);
                return result;
            }
        }

        /**
         * Loads the value of the {@code FUN} variable, which represents the function to be called.
         * It does not specialize itself but the {@code readFun} node will specialize, but always to
         * the same tree. It is used as the {@code functionNode} field in the generated
         * {@link RCallNode}.
         *
         */
        protected static class LapplyFunctionNode extends RNode {
            private static final String LAPPLY_FUN = new String("FUN");

            @Child private ReadVariableNode readFun;

            public LapplyFunctionNode() {
                readFun = insert(ReadVariableNode.create(LAPPLY_FUN, false));
            }

            @Override
            public Object execute(VirtualFrame frame) {
                return readFun.execute(frame);
            }

        }

        @TypeSystemReference(RTypes.class)
        private abstract static class CachedCallNode extends Node {
            protected final GeneralLApplyNode owner;

            protected CachedCallNode(GeneralLApplyNode owner) {
                this.owner = owner;
            }

            public abstract Object execute(VirtualFrame frame, RootCallTarget callTarget, RArgsValuesAndNames varArgs);

            CachedCallNode next() {
                throw RInternalError.shouldNotReachHere();
            }

            /**
             * Creates the inline cache.
             *
             * @param maxPicDepth maximum number of entries in the polymorphic inline cache
             */
            public static CachedCallNode create(GeneralLApplyNode owner, int maxPicDepth) {
                return new UninitializedCachedCallNode(owner, maxPicDepth);
            }

            @NodeInfo(cost = NodeCost.UNINITIALIZED)
            private static final class UninitializedCachedCallNode extends CachedCallNode {

                private final int maxPicDepth;

                /** The current depth of the inline cache. */
                private int picDepth = 0;

                public UninitializedCachedCallNode(GeneralLApplyNode owner, int maxPicDepth) {
                    super(owner);
                    this.maxPicDepth = maxPicDepth;
                }

                @Override
                public Object execute(VirtualFrame frame, RootCallTarget callTarget, RArgsValuesAndNames varArgs) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();

                    CachedCallNode next = this;
                    /*
                     * Node that will be the subject of the replace operation, which will change its
                     * parent to "replacement". Normally "this" unless the cache is full, in which
                     * it is the head of the chain.
                     */
                    CachedCallNode replacee = this;
                    if (picDepth < maxPicDepth) {
                        picDepth += 1;
                        // insert prior to this node
                    } else {
                        // Discard the oldest
                        next = owner.callCache.next();
                        replacee = owner.callCache;
                    }
                    CachedCallNode replacement = new ResolvedCachedCallNode(callTarget, varArgs, next);
                    return replacee.replace(replacement).execute(frame, callTarget, varArgs);
                }
            }

            private static final class ResolvedCachedCallNode extends CachedCallNode {

                private final RootCallTarget originalTarget;
                private final VarArgsSignature originalSignature;
                @Child private RCallNode callNode;
                @Child private CachedCallNode next;

                protected ResolvedCachedCallNode(RootCallTarget originalTarget, RArgsValuesAndNames varArgs, CachedCallNode next) {
                    super(next.owner);
                    this.originalTarget = originalTarget;
                    this.originalSignature = CallArgumentsNode.createSignature(varArgs, 1);
                    this.callNode = checkFunction(originalTarget, varArgs);
                    this.next = next;
                }

                @Override
                CachedCallNode next() {
                    return next;
                }

                @Override
                public Object execute(VirtualFrame frame, RootCallTarget target, RArgsValuesAndNames varArgs) {
                    VarArgsSignature signature = CallArgumentsNode.createSignature(varArgs, 1);
                    if (target != originalTarget || signature.isNotEqualTo(originalSignature)) {
                        return next.execute(frame, target, varArgs);
                    } else {
                        return callNode.execute(frame);
                    }
                }

                /**
                 * Creates the {@link RCallNode} for this target and {@code varArgs}.
                 *
                 * @param varArgs may be {@link RMissing#instance} to indicate empty "..."!
                 */
                private RCallNode checkFunction(RootCallTarget callTarget, RArgsValuesAndNames varArgs) {
                    /* TODO: R switches to double if x.getLength() is greater than 2^31-1 */
                    FormalArguments formalArgs = ((RRootNode) callTarget.getRootNode()).getFormalArguments();

                    // The first parameter to the function call is named as defined by the function.
                    String readVectorElementName = formalArgs.getSignature().getName(0);
                    if (ArgumentsSignature.VARARG_NAME.equals(readVectorElementName)) {
                        // "..." is no "supplied" name, instead the argument will match by position
                        // right away
                        readVectorElementName = null;
                    }

                    // The remaining parameters are passed from {@code ...}. The call node will take
                    // care of matching.
                    RNode[] args;
                    String[] names;
                    if (varArgs.isEmpty()) {    // == null || (varArgs.length() == 1 &&
                        // varArgs.getValues()[0]
                        // == RMissing.instance)) {
                        args = new RNode[]{owner.readVectorElement};
                        names = new String[]{readVectorElementName};
                    } else {
                        // Insert expressions found inside "..." as arguments
                        args = new RNode[varArgs.length() + 1];
                        args[0] = owner.readVectorElement;
                        Object[] varArgsValues = varArgs.getValues();
                        for (int i = 0; i < varArgs.length(); i++) {
                            args[i + 1] = CallArgumentsNode.wrapVarArgValue(varArgsValues[i], i);

                        }

                        names = new String[varArgs.length() + 1];
                        names[0] = readVectorElementName;
                        for (int i = 0; i < varArgs.length(); i++) {
                            String name = varArgs.getSignature().getName(i);
                            if (name != null && !name.isEmpty()) {
                                // change "" to null
                                names[i + 1] = name;
                            }
                        }
                    }
                    ArgumentsSignature callSignature = ArgumentsSignature.get(names);

                    CallArgumentsNode argsNode = CallArgumentsNode.create(false, false, args, callSignature);
                    return RCallNode.createCall(null, owner.functionNode, argsNode, null);
                }
            }
        }
    }

}
