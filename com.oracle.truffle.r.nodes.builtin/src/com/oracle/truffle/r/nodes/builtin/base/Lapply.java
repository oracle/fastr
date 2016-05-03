/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */

package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.nodes.access.WriteVariableNode;
import com.oracle.truffle.r.nodes.access.WriteVariableNode.Mode;
import com.oracle.truffle.r.nodes.access.variables.ReadVariableNode;
import com.oracle.truffle.r.nodes.access.vector.ElementAccessMode;
import com.oracle.truffle.r.nodes.access.vector.ExtractVectorNode;
import com.oracle.truffle.r.nodes.access.vector.ExtractVectorNodeGen;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.base.LapplyNodeGen.LapplyInternalNodeGen;
import com.oracle.truffle.r.nodes.control.RLengthNode;
import com.oracle.truffle.r.nodes.control.RLengthNodeGen;
import com.oracle.truffle.r.nodes.function.RCallNode;
import com.oracle.truffle.r.runtime.AnonymousFrameVariable;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.RAttributeProfiles;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.nodes.InternalRSyntaxNodeChildren;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

/**
 * The {@code lapply} builtin. {@code lapply} is an important implicit iterator in R. This
 * implementation handles the general case, but there are opportunities for "specializations" that
 * rewrite simple uses directly down to, e.g. explicit loops using, for example, a {@link LoopNode}.
 *
 * See the comment in {@link VApply} regarding "...".
 */
@RBuiltin(name = "lapply", kind = INTERNAL, parameterNames = {"X", "FUN"}, splitCaller = true)
public abstract class Lapply extends RBuiltinNode {

    private static final Source CALL_SOURCE = Source.fromText("FUN(X[[i]], ...)", "lapply");

    private final RAttributeProfiles attrProfiles = RAttributeProfiles.create();

    @Child private LapplyInternalNode lapply = LapplyInternalNodeGen.create();

    @Specialization
    protected Object lapply(VirtualFrame frame, RAbstractVector vec, RFunction fun) {
        Object[] result = lapply.execute(frame, vec, fun);
        // set here else it gets overridden by the iterator evaluation
        controlVisibility();
        return RDataFactory.createList(result, vec.getNames(attrProfiles));
    }

    public abstract static class LapplyInternalNode extends RBaseNode implements InternalRSyntaxNodeChildren {

        private static final String VECTOR_ELEMENT = AnonymousFrameVariable.create("LAPPLY_VEC_ELEM");

        @Child private RLengthNode lengthNode = RLengthNodeGen.create();
        @Child private WriteVariableNode writeVectorElement = WriteVariableNode.createAnonymous(VECTOR_ELEMENT, null, Mode.REGULAR);
        @Child private ExtractVectorNode extractElementNode = ExtractVectorNodeGen.create(ElementAccessMode.SUBSCRIPT, false);
        @Child private RCallNode callNode = createCallNode();

        public abstract Object[] execute(VirtualFrame frame, Object vector, RFunction function);

        @Specialization
        protected Object[] cachedLApply(VirtualFrame frame, Object vector, RFunction function) {
            // TODO: R switches to double if x.getLength() is greater than 2^31-1
            int length = lengthNode.executeInteger(frame, vector);
            Object[] result = new Object[length];
            for (int i = 1; i <= length; i++) {
                writeVectorElement.execute(frame, extractElementNode.apply(frame, vector, new Object[]{i}, RRuntime.LOGICAL_TRUE, RRuntime.LOGICAL_TRUE));
                result[i - 1] = callNode.execute(frame, function);
            }
            return result;
        }

        /**
         * Creates the {@link RCallNode} for this target and {@code varArgs}.
         */
        protected RCallNode createCallNode() {
            CompilerAsserts.neverPartOfCompilation();

            ReadVariableNode readVector = ReadVariableNode.createSilent(VECTOR_ELEMENT, RType.Any);
            ReadVariableNode readArgs = ReadVariableNode.createSilent(ArgumentsSignature.VARARG_NAME, RType.Any);

            return RCallNode.createCall(createCallSourceSection(), null, ArgumentsSignature.get(null, "..."), readVector, readArgs);
        }
    }

    static SourceSection createCallSourceSection() {
        return CALL_SOURCE.createSection("", 0, CALL_SOURCE.getLength());
    }
}
