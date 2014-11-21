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
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin(name = "lapply", kind = INTERNAL, parameterNames = {"X", "FUN", "..."})
public abstract class Lapply extends RBuiltinNode {

    public static final Object LAPPLY_VEC_ELEM_ID = new Object();

    @Child private CallInlineCacheNode callCache = CallInlineCacheNode.create(3);

    @Child private RCallNode callNode = null;
    @CompilationFinal private RootCallTarget callTarget = null;
    private LapplyIteratorNode iterator = null;
    @Child private WriteVariableNode writeVectorElement = null;
    private final SourceSection writeSrc = new NullSourceSection("Lapply builtin", "write vector element");
    @CompilationFinal private ReadVariableNode readVectorElement = null;
    private final SourceSection readSrc = new NullSourceSection("Lapply builtin", "read vector element");
    private VarArgsSignature oldSignature = null;

    @Child private Lapply dataFrameLapply;

    public abstract Object execute(VirtualFrame frame, RAbstractVector vec, RFunction fun, RArgsValuesAndNames optionalArgs);

    @Override
    public RNode[] getParameterValues() {
        return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance)};
    }

    @Specialization
    protected Object lapply(VirtualFrame frame, RAbstractVector vec, RFunction fun, RArgsValuesAndNames varArgs) {
        controlVisibility();
        RVector vecMat = vec.materialize();
        Object[] callResult = applyHelper(frame, vecMat, fun, varArgs);
        return RDataFactory.createList(callResult, vecMat.getNames());
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

    /**
     * @param frame
     * @param vecMat
     * @param fun
     * @param varArgsArg May be {@link RMissing#instance} to indicate empty "..."!
     * @return The results of applying fun to every vector element
     */
    protected Object[] applyHelper(VirtualFrame frame, RVector vecMat, RFunction fun, Object varArgsArg) {
        /* TODO: R switches to double if x.getLength() is greater than 2^31-1 */
        Object[] result = new Object[vecMat.getLength()];
        FormalArguments formalArgs = ((RRootNode) fun.getTarget().getRootNode()).getFormalArguments();

        // TODO Poor man's caching, change to proper cache
        VarArgsSignature signature = CallArgumentsNode.createSignature(varArgsArg, 1, true);
        RArgsValuesAndNames varArgs = varArgsArg == RMissing.instance ? null : (RArgsValuesAndNames) varArgsArg;
        if (fun.getTarget() != callTarget || signature.isNotEqualTo(oldSignature)) {
            CompilerDirectives.transferToInterpreterAndInvalidate();

            // To extract the elements from the vector in the loop, we use a dedicated node that
            // maintains an internal counter.
            // TODO Revise copy semantics here!
            readVectorElement = replace(readVectorElement, ReadVariableNode.create(LAPPLY_VEC_ELEM_ID, true));
            readVectorElement.assignSourceSection(readSrc);

            iterator = new LapplyIteratorNode();
            writeVectorElement = replace(writeVectorElement, WriteVariableNode.create(writeSrc, LAPPLY_VEC_ELEM_ID, iterator, false, false));

            // The first parameter to the function call is named as defined by the function.
            String readVectorElementName = formalArgs.getNames()[0];
            if (Arguments.VARARG_NAME.equals(readVectorElementName)) {
                // "..." is no "supplied" name, instead the argument will match by position right
                // away
                readVectorElementName = null;
            }

            // The remaining parameters are passed from {@code ...}. The call node will take care of
            // matching.
            RNode[] args;
            String[] names;
            if (varArgs == null || (varArgs.length() == 1 && varArgs.getValues()[0] == RMissing.instance)) {
                args = new RNode[]{readVectorElement};
                names = new String[]{readVectorElementName};
            } else {
                // TODO Insert more arguments
                args = new RNode[varArgs.length() + 1];
                args[0] = readVectorElement;
                for (int i = 0; i < varArgs.length(); i++) {
                    args[i + 1] = CallArgumentsNode.wrapVarArgValue(varArgs.getValues()[i]);
                }

                names = new String[varArgs.length() + 1];
                names[0] = readVectorElementName;
                System.arraycopy(varArgs.getNames(), 0, names, 1, varArgs.length());
            }
            CallArgumentsNode argsNode = CallArgumentsNode.create(false, false, args, names);
            callNode = replace(callNode, RCallNode.createStaticCall(getEncapsulatingSourceSection(), fun, argsNode));
            callTarget = fun.getTarget();
            oldSignature = signature;
        }

        iterator.reset(vecMat);

        for (int i = 0; i < result.length; ++i) {
            // Write new vector element to LAPPLY_VEC_ELEME_ID frame slot
            writeVectorElement.execute(frame);

            result[i] = callNode.execute(frame);
        }

        // LAPPLY_VEC_ELEME_ID stays inside frame
        return result;
    }

    private static class LapplyIteratorNode extends RNode {

        private RVector vector;
        private int index;

        public void reset(RVector newVector) {
            this.vector = newVector;
            this.index = 0;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            return vector.getDataAtAsObject(index++);
        }
    }

    private <O extends Node, T extends Node> T replace(O oldNode, T newNode) {
        if (oldNode == null) {
            return insert(newNode);
        } else {
            return oldNode.replace(newNode);
        }
    }
}
