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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.nodes.RASTBuilder;
import com.oracle.truffle.r.nodes.RRootNode;
import com.oracle.truffle.r.nodes.access.ConstantNode;
import com.oracle.truffle.r.nodes.access.WriteVariableNode;
import com.oracle.truffle.r.nodes.access.WriteVariableNode.Mode;
import com.oracle.truffle.r.nodes.access.variables.ReadVariableNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinRootNode;
import com.oracle.truffle.r.nodes.builtin.base.LapplyNodeGen.LapplyInternalNodeGen;
import com.oracle.truffle.r.nodes.function.PromiseNode;
import com.oracle.truffle.r.nodes.function.RCallNode;
import com.oracle.truffle.r.runtime.AnonymousFrameVariable;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.RDeparse;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RAttributeProfiles;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.nodes.InternalRSyntaxNodeChildren;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

/**
 * The {@code lapply} builtin. {@code lapply} is an important implicit iterator in R. This
 * implementation handles the general case, but there are opportunities for "specializations" that
 * rewrite simple uses directly down to, e.g. explicit loops using, for example, a {@link LoopNode}.
 *
 * See the comment in {@link VApply} regarding "...".
 */
@RBuiltin(name = "lapply", kind = INTERNAL, parameterNames = {"X", "FUN"}, splitCaller = true)
public abstract class Lapply extends RBuiltinNode {

    private final RAttributeProfiles attrProfiles = RAttributeProfiles.create();

    @Child private LapplyInternalNode lapply = LapplyInternalNodeGen.create(null, null, null);

    @Specialization
    protected Object lapply(VirtualFrame frame, RAbstractVector vec, RFunction fun) {
        RArgsValuesAndNames optionalArgs = (RArgsValuesAndNames) RArguments.getArgument(frame, 2);
        Object[] result = lapply.execute(frame, vec, fun, optionalArgs);
        // set here else it gets overridden by the iterator evaluation
        controlVisibility();
        return RDataFactory.createList(result, vec.getNames(attrProfiles));
    }

    @NodeChildren({@NodeChild(type = RNode.class), @NodeChild(type = RNode.class), @NodeChild(type = RNode.class)})
    public abstract static class LapplyInternalNode extends RNode implements InternalRSyntaxNodeChildren {

        private static final String INDEX_NAME = AnonymousFrameVariable.create("LAPPLY_ITER_INDEX");
        private static final String VECTOR_ELEMENT = AnonymousFrameVariable.create("LAPPLY_VEC_ELEM");

        @Child private Length lengthNode = LengthNodeGen.create(null, null, null);
        @Child private WriteVariableNode writeVectorElement = WriteVariableNode.createAnonymous(VECTOR_ELEMENT, null, Mode.REGULAR);
        @Child private WriteVariableNode writeIndex = WriteVariableNode.createAnonymous(INDEX_NAME, null, Mode.REGULAR);
        @Child private RNode indexedLoadNode = createIndexedLoad();

        public abstract Object[] execute(VirtualFrame frame, Object vector, RFunction function, RArgsValuesAndNames additionalArguments);

        private Object[] lApplyInternal(VirtualFrame frame, Object vector, RFunction function, RCallNode callNode) {
            int length = lengthNode.executeInt(frame, vector);
            Object[] result = new Object[length];
            for (int i = 1; i <= length; i++) {
                writeIndex.execute(frame, i);
                writeVectorElement.execute(frame, indexedLoadNode.execute(frame));
                result[i - 1] = callNode.execute(frame, function);
            }
            return result;
        }

        @SuppressWarnings("unused")
        @Specialization(limit = "5", guards = {"function.getTarget() == cachedTarget", "additionalArguments.getSignature() == cachedSignature"})
        protected Object[] cachedLApply(VirtualFrame frame, Object vector, RFunction function, RArgsValuesAndNames additionalArguments, //
                        @Cached("function.getTarget()") RootCallTarget cachedTarget, //
                        @Cached("additionalArguments.getSignature()") ArgumentsSignature cachedSignature, //
                        @Cached("createCallNode(cachedTarget, additionalArguments)") RCallNode callNode) {
            return lApplyInternal(frame, vector, function, callNode);
        }

        @Specialization(contains = "cachedLApply")
        protected Object[] genericLApply(VirtualFrame frame, Object vector, RFunction function, RArgsValuesAndNames additionalArguments) {

            // TODO: implement more efficiently (how much does it matter considering that there is
            // cached version?); previous comment here implied that having RCallNode executing with
            // an evaluated RArgsValuesAndNames would help
            return lApplyInternal(frame, vector, function, createCallNode(function.getTarget(), additionalArguments));
        }

        private static RNode createIndexedLoad() {
            RASTBuilder builder = new RASTBuilder();
            RSyntaxNode receiver = builder.lookup(RSyntaxNode.INTERNAL, "X", false);
            RSyntaxNode index = builder.lookup(RSyntaxNode.INTERNAL, INDEX_NAME, false);
            RSyntaxNode access = builder.lookup(RSyntaxNode.INTERNAL, "[[", true);
            return builder.call(RSyntaxNode.INTERNAL, access, receiver, index).asRNode();
        }

        /**
         * Creates the {@link RCallNode} for this target and {@code varArgs}.
         *
         * @param additionalArguments may be {@link RMissing#instance} to indicate empty "..."!
         */
        @TruffleBoundary
        protected RCallNode createCallNode(RootCallTarget callTarget, RArgsValuesAndNames additionalArguments) {
            /* TODO: R switches to double if x.getLength() is greater than 2^31-1 */

            ReadVariableNode readVector = ReadVariableNode.create(VECTOR_ELEMENT);

            // The remaining parameters are passed from {@code ...}. The call node will take
            // care of matching.
            RSyntaxNode[] args;
            String[] names;
            if (additionalArguments.isEmpty()) {    // == null || (varArgs.length() == 1 &&
                // varArgs.getValue(0)
                // == RMissing.instance)) {
                args = new RSyntaxNode[]{readVector};
                names = new String[]{null};
            } else {
                // Insert expressions found inside "..." as arguments
                args = new RSyntaxNode[additionalArguments.getLength() + 1];
                args[0] = readVector;
                Object[] varArgsValues = additionalArguments.getArguments();
                for (int i = 0; i < additionalArguments.getLength(); i++) {
                    args[i + 1] = (RSyntaxNode) wrapVarArgValue(varArgsValues[i], i);

                }
                names = new String[additionalArguments.getLength() + 1];
                names[0] = null;
                for (int i = 0; i < additionalArguments.getLength(); i++) {
                    String name = additionalArguments.getSignature().getName(i);
                    if (name != null && !name.isEmpty()) {
                        // change "" to null
                        names[i + 1] = name;
                    }
                }
            }
            ArgumentsSignature argsSig = ArgumentsSignature.get(names);
            // Errors can be thrown from the modified call so a SourceSection is required
            SourceSection ss = createCallSourceSection(callTarget, argsSig, args);
            return RCallNode.createCall(ss, null, argsSig, args);
        }
    }

    static SourceSection createCallSourceSection(RootCallTarget callTarget, ArgumentsSignature argsSig, RSyntaxNode[] args) {
        RDeparse.State state = RDeparse.State.createPrintableState();
        RCallNode.deparseArguments(state, args, argsSig);
        // The call is (function)(args)
        String callName;
        RRootNode rrn = (RRootNode) callTarget.getRootNode();
        if (rrn instanceof RBuiltinRootNode) {
            RBuiltinRootNode rbrn = (RBuiltinRootNode) rrn;
            callName = rbrn.getBuiltin().getBuiltin().getName();
        } else {
            callName = "(" + rrn.getSourceCode() + ")";
        }
        String callString = callName + state.toString();
        Source callSource = Source.fromText(callString, "lapply");
        SourceSection ss = callSource.createSection("", 0, callString.length());
        return ss;
    }

    private static RNode wrapVarArgValue(Object varArgValue, int varArgIndex) {
        if (varArgValue instanceof RPromise) {
            return PromiseNode.createVarArg(varArgIndex);
        } else {
            return ConstantNode.create(varArgValue);
        }
    }
}
