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
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.binary.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.ops.*;

@RBuiltin(name = "lapply", kind = INTERNAL, parameterNames = {"X", "FUN", "..."})
public abstract class Lapply extends RBuiltinNode {

    @Child private CallInlineCacheNode callCache = CallInlineCacheNode.create(3);

    @Child private Lapply dataFrameLapply;
    @Child private DoApplyNode doApply = new DoApplyNode();

    public abstract Object execute(VirtualFrame frame, RAbstractVector vec, RFunction fun, RArgsValuesAndNames optionalArgs);

    @Override
    public RNode[] getParameterValues() {
        return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance)};
    }

    @Specialization
    protected Object lapply(VirtualFrame frame, RAbstractVector vec, RFunction fun, RArgsValuesAndNames varArgs) {
        controlVisibility();
        RVector vecMat = vec.materialize();
        Object[] result = doApply.execute(frame, vecMat, fun, varArgs);
        return RDataFactory.createList(result, vecMat.getNames());
    }

    @Specialization
    protected Object lapply(VirtualFrame frame, RDataFrame x, RFunction fun, RArgsValuesAndNames optionalArgs) {
        controlVisibility();
        if (dataFrameLapply == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            dataFrameLapply = insert(LapplyFactory.create(new RNode[3], getBuiltin(), getSuppliedArgsNames()));
        }
        return dataFrameLapply.execute(frame, x.getVector(), fun, optionalArgs);
    }

    public static final class DoApplyNode extends RNode {
        private static final AnonymousFrameVariable LAPPLY_VEC_ELEM = AnonymousFrameVariable.create("LAPPLY_VEC_ELEM");

        @Child protected WriteVariableNode writeVectorElement = null;
        @CompilationFinal private ReadVariableNode readVectorElement = null;

        protected LapplyIteratorNode iterator = null;
        private LapplyFunctionNode functionNode = null;
        @Child protected RCallNode callNode = null;

        @CompilationFinal private RootCallTarget callTarget = null;
        @CompilationFinal private VarArgsSignature oldSignature = null;

        public Object[] execute(VirtualFrame frame, RVector vecMat, RFunction fun, RArgsValuesAndNames varArgs) {
            checkFunction(frame, vecMat, fun, varArgs);

            Object[] result = new Object[vecMat.getLength()];
            for (int i = 0; i < result.length; ++i) {
                // Write new vector element to LAPPLY_VEC_ELEM frame slot
                writeVectorElement.execute(frame);

                result[i] = callNode.execute(frame);
            }
            return result;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            throw RInternalError.shouldNotReachHere();
        }

        /**
         * @param vecMat
         * @param fun
         * @param varArgs May be {@link RMissing#instance} to indicate empty "..."!
         */
        private void checkFunction(VirtualFrame frame, RVector vecMat, RFunction fun, RArgsValuesAndNames varArgs) {
            /* TODO: R switches to double if x.getLength() is greater than 2^31-1 */
            FormalArguments formalArgs = ((RRootNode) fun.getTarget().getRootNode()).getFormalArguments();

            // TODO Poor man's caching, change to proper cache
            VarArgsSignature signature = CallArgumentsNode.createSignature(varArgs, 1, true);
            if (fun.getTarget() != callTarget || signature.isNotEqualTo(oldSignature)) {
                CompilerDirectives.transferToInterpreterAndInvalidate();

                // To extract the elements from the vector in the loop, we use a dedicated node that
                // maintains an internal counter.
                // TODO Revise copy semantics here!
                readVectorElement = replace(readVectorElement, ReadVariableNode.create(LAPPLY_VEC_ELEM, true));

                iterator = new LapplyIteratorNode();
                writeVectorElement = replace(writeVectorElement, WriteVariableNode.create(LAPPLY_VEC_ELEM, iterator, false, false));

                // The first parameter to the function call is named as defined by the function.
                String readVectorElementName = formalArgs.getNames()[0];
                if (Arguments.VARARG_NAME.equals(readVectorElementName)) {
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
                    args = new RNode[]{readVectorElement};
                    names = new String[]{readVectorElementName};
                } else {
                    // Insert expressions found inside "..." as arguments
                    args = new RNode[varArgs.length() + 1];
                    args[0] = readVectorElement;
                    for (int i = 0; i < varArgs.length(); i++) {
                        args[i + 1] = CallArgumentsNode.wrapVarArgValue(varArgs.getValues()[i]);
                    }

                    names = new String[varArgs.length() + 1];
                    names[0] = readVectorElementName;
                    System.arraycopy(varArgs.getNames(), 0, names, 1, varArgs.length());
                }
                functionNode = new LapplyFunctionNode();
                CallArgumentsNode argsNode = CallArgumentsNode.create(false, false, args, names);
                callNode = replace(callNode, RCallNode.createCall(null, functionNode, argsNode));
                callTarget = fun.getTarget();
                oldSignature = signature;
            }

            // zero the iteratoe value in the current frame
            iterator.initialize(frame);
        }

        private <O extends Node, T extends Node> T replace(O oldNode, T newNode) {
            if (oldNode == null) {
                return insert(newNode);
            } else {
                return oldNode.replace(newNode);
            }
        }

        protected static class LapplyIteratorNode extends RNode {
            private static final String LAPPLY_VEC = new String("X");
            private static final String LAPPLY_ITER_INDEX = new String("LAPPLY_ITER_INDEX");

            @Child ReadVariableNode readVector;
            @Child ReadVariableNode readIndex;
            @Child WriteVariableNode writeIndex;
            @Child WriteVariableNode zeroIndex;

            LapplyIteratorNode() {
                readVector = insert(ReadVariableNode.create(LAPPLY_VEC, false));
                readIndex = insert(ReadVariableNode.create(LAPPLY_ITER_INDEX, false));
                RNode[] incArgs = new RNode[]{ReadVariableNode.create(LAPPLY_ITER_INDEX, false), ConstantNode.create(1)};
                BinaryArithmeticNode incIndex = BinaryArithmeticNodeFactory.create(BinaryArithmetic.ADD, null, incArgs, null, null);
                writeIndex = insert(WriteVariableNode.create(null, LAPPLY_ITER_INDEX, incIndex, false, false));
                zeroIndex = insert(WriteVariableNode.create(null, LAPPLY_ITER_INDEX, ConstantNode.create(0), false, false));
            }

            LapplyIteratorNode initialize(VirtualFrame frame) {
                zeroIndex.execute(frame);
                return this;
            }

            @Override
            public Object execute(VirtualFrame frame) {
                Object result = readVector.execute(frame);
                if (result instanceof RAbstractVector) {
                    RAbstractVector vector = (RAbstractVector) result;
                    int index = (int) readIndex.execute(frame);
                    result = vector.getDataAtAsObject(index);
                } else {
                    // single element vector as scalar, e.g. Double
                }
                writeIndex.execute(frame);
                return result;
            }
        }

        protected static class LapplyFunctionNode extends RNode {
            private static final String LAPPLY_FUN = new String("FUN");

            @Child ReadVariableNode readFun;

            public LapplyFunctionNode() {
                readFun = insert(ReadVariableNode.create(LAPPLY_FUN, false));
            }

            @Override
            public Object execute(VirtualFrame frame) {
                return readFun.execute(frame);
            }

        }
    }
}
